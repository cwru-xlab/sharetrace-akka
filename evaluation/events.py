from __future__ import annotations

import abc
import collections
import dataclasses
import enum
import itertools
import json
import os
import pathlib
import tempfile
import zipfile
from collections.abc import Callable, Iterable, Sized
from typing import Any, Iterator, final

import numpy as np
from scipy import sparse

from hints import Record


class Event(str, enum.Enum):
    """Types of events logged by user actors."""
    CONTACT = "ContactEvent"
    CONTACTS_REFRESH = "ContactsRefreshEvent"
    CURRENT_REFRESH = "CurrentRefreshEvent"
    PROPAGATE = "PropagateEvent"
    RECEIVE = "ReceiveEvent"
    SEND_CACHED = "SendCachedEvent"
    SEND_CURRENT = "SendCurrentEvent"
    UPDATE = "UpdateEvent"
    TIMEOUT = "TimeoutEvent"

    def __repr__(self) -> str:
        return self.value

    def __int__(self) -> int:
        return _EVENTS[self.value]

    @classmethod
    def of(cls, record: Record) -> Event:
        return Event(record["type"])


_EVENT_WIDTH = f"<U{max(len(e) for e in Event)}"
_EVENTS = {v: i for i, v in enumerate(Event)}


def stream(logdir: os.PathLike | str) -> Iterable[Record]:
    """Returns a stream of event records from the logging directory."""
    logdir = pathlib.Path(logdir).absolute()
    zipped, unzipped = _split(logdir.iterdir(), _is_zipped)
    # Iterate over zipped first! Unzipped events occur last.
    with tempfile.TemporaryDirectory() as tmpdir:
        tmpdir = pathlib.Path(tmpdir).absolute()
        _unzip(zipped, tmpdir)
        for record in _stream(tmpdir.iterdir()):
            yield record
    for record in _stream(unzipped):
        yield record


def _stream(filenames: Iterable[pathlib.Path]) -> Iterable[Record]:
    for filename in sorted(filenames):
        with filename.open() as log:
            for line in log:
                yield json.loads(line)


def _is_zipped(path: pathlib.Path) -> bool:
    return path.name.endswith(".zip")


def _unzip(zipped: Iterable[os.PathLike], dest: os.PathLike) -> None:
    for filename in zipped:
        with zipfile.ZipFile(filename) as f:
            f.extractall(dest)


Predicate = Callable[[Any], bool]


def _split(it: Iterable, predicate: Predicate) -> tuple[Iterable, Iterable]:
    it = list(it)
    true = (e for e in it if predicate(e))
    false = (e for e in it if not predicate(e))
    return true, false


class EventCounter(collections.Counter):
    """Counts the number of occurrences of each event type."""

    def __setitem__(self, key, value) -> None:
        if key in _EVENTS:
            super().__setitem__(key, value)
        else:
            raise ValueError(
                f"'key' must be an {Event.__name__} instance")

    @property
    def num_sent_to_contacts(self) -> int:
        """Returns the number of messages sent to a contact."""
        return self.num_curr_sent + self.num_cached_sent

    @property
    def num_received(self) -> int:
        """Returns the number of messages received."""
        return self[Event.RECEIVE]

    @property
    def num_curr_sent(self) -> int:
        """Returns the number of current messages."""
        return self[Event.SEND_CURRENT]

    @property
    def num_cached_sent(self) -> int:
        """Returns the number of cached messages."""
        return self[Event.SEND_CACHED]

    @property
    def num_propagated(self) -> int:
        """Returns the number of propagated messages."""
        return self[Event.PROPAGATE]

    @property
    def num_updates(self) -> int:
        """Returns the number of user actor updates."""
        return self[Event.UPDATE]

    @property
    def num_contacts(self) -> int:
        """Returns the number of contacts."""
        # Each user logs a contact, so each contact is double counted.
        return int(self[Event.CONTACT] / 2)

    @property
    def num_contact_refreshes(self) -> int:
        """Returns the number of contact refreshes."""
        return self[Event.CONTACTS_REFRESH]

    @property
    def num_curr_refreshes(self) -> int:
        """Returns the number of current message refreshes."""
        return self[Event.CURRENT_REFRESH]

    @property
    def num_not_sent_to_contacts(self) -> int:
        """Returns the number of messages not sent to a contact."""
        # Each user sends a message, so double the number of contacts.
        return 2 * self.num_contacts - self.num_sent_to_contacts


class CallbackData(Callable):

    @abc.abstractmethod
    def __call__(self, record: Record, **kwargs) -> None:
        pass

    def on_complete(self) -> None:
        pass


@final
class EventCallback(CallbackData):
    """A callable that processes event records."""
    __slots__ = "_states"

    def __init__(self, factory: Callable[[], CallbackData]):
        self._states = collections.defaultdict(factory)

    def __call__(self, record: Record, **kwargs) -> None:
        """Processes the record."""
        return self[record["sid"]](record["event"], **kwargs)

    def __getitem__(self, key: str | int | slice) -> CallbackData:
        return self._states[key]

    def __len__(self) -> int:
        return len(self._states)

    def __iter__(self) -> Iterator[EventCallback]:
        return iter(self._states)

    def on_complete(self) -> None:
        """Finalizes the callback for inspection after processing records."""
        self._states = list(self._states.values())
        for state in self._states:
            state.on_complete()


class EventCounterData(EventCounter, CallbackData):
    """An event callback for counting the occurrences of events."""

    def __call__(self, record: Record, **kwargs) -> None:
        self.update((Event.of(record),))


@dataclasses.dataclass(slots=True)
class UserUpdates:
    _DEFAULT_N = np.uint16(0)

    initial: np.float32
    final: np.float32 | None = None
    n: np.uint16 = _DEFAULT_N

    def __post_init__(self) -> None:
        if self.final is None:
            self.final = self.initial


class UpdatesData(CallbackData, Sized):
    """An event callback for tracking user updates.

    Attributes:
       updates: An array where the i-th entry is number of updates of user i.
       initials: An array where the i-th entry is the initial value of user i.
       finals: An array where the i-th entry is the final value of user i.
    """
    __slots__ = "updates", "initials", "finals", "_n"

    def __init__(self) -> None:
        self.updates: dict[np.uint32, UserUpdates] | np.ndarray[np.uint16] = {}
        self.initials: np.ndarray[np.float32] | None = None
        self.finals: np.ndarray[np.float32] | None = None
        self._n: int = 0

    def __call__(self, record: Record, **kwargs) -> None:
        if Event.of(record) == Event.UPDATE:
            if (name := np.uint32(record["to"])) in self.updates:
                user = self.updates[name]
                user.n += 1
                user.final = record["newScore"]["value"]
                self._n += 1  # Only count non-initial updates
            else:
                initial = np.float32(record["newScore"]["value"])
                self.updates[name] = UserUpdates(initial=initial)

    def on_complete(self) -> None:
        n_users = len(self.updates)
        updates = np.zeros(n_users, dtype=np.uint16)
        initials = np.zeros(n_users, dtype=np.float32)
        finals = np.zeros(n_users, dtype=np.float32)
        for u, user in self.updates.items():
            updates[u] = user.n
            initials[u] = user.initial
            finals[u] = user.final
        self.updates = updates
        self.initials = initials
        self.finals = finals

    def __len__(self) -> int:
        return self._n


class ReceivedData(CallbackData):
    __slots__ = "values"

    def __init__(self):
        self.values: list[float] | np.ndarray[np.float32] = []

    def __call__(self, record: Record, **kwargs) -> None:
        if Event.of(record) == Event.RECEIVE:
            self.values.append(record["score"]["value"])

    def on_complete(self) -> None:
        self.values = np.array(self.values, dtype=np.float32)


class TimelineData(CallbackData):
    """An event callback for recording the order of events.

    Attributes:
        e2i: An dictionary that maps an Event to its encoded integer.
        i2e: An array where the i-th entry is the Event value encoded as i.
    """
    __slots__ = "e2i", "i2e", "timestamps", "_events", "_repeats"

    def __init__(self):
        self.e2i: dict[Event, int] = {}
        self.i2e: np.ndarray[np.str] | None = None
        self.timestamps: list[int | None] | np.ndarray[np.uint64] = []
        self._events: list[int | None] | np.ndarray[np.uint8] = [None]
        self._repeats: list[int] | np.ndarray[np.uint16] = [0]

    def __call__(self, record: Record, **kwargs) -> None:
        if (event := Event.of(record)) not in self.e2i:
            # Encode the event as an integer.
            self.e2i[event] = len(self.e2i)
        self.timestamps.append(record["timestamp"])
        # If the previous event was also this type...
        if (label := self.e2i[event]) == self._events[-1]:
            # ...increment its count.
            self._repeats[-1] += 1
        else:
            self._events.append(label)
            self._repeats.append(1)

    def on_complete(self) -> None:
        t0 = min(times := self.timestamps)
        self.timestamps = np.array([t - t0 for t in times], dtype=np.uint64)
        self._events = np.array(self._events[1:], dtype=np.uint8)
        self._repeats = np.array(self._repeats[1:], dtype=np.uint16)
        # Use an array to use numpy indexing to map back to decoded values.
        self.i2e = np.array([e.value for e in self.e2i], dtype=_EVENT_WIDTH)

    def flatten(self, decoded: bool = False) -> np.ndarray[np.uint8]:
        """Returns a 1-D array of events.

        Args:
            decoded: If true, entries are the names of the events. Otherwise,
                entries are encoded integers.
        """
        return np.repeat(self._get_events(decoded), self._repeats)

    def run_length_encoded(self, decoded: bool = False) -> np.ndarray:
        """Returns a 2-D array of run-length encoded events.

        An entry (`e`, `n`) corresponds to the event `e` and the number `n` of
        times it occurred consecutively.

        Args:
            decoded: If true, entries are the names of the events. Otherwise,
                entries are encoded integers.
        """
        events = self._get_events(decoded)
        repeats = self._repeats
        if decoded:
            encoded = np.array(
                list(zip(events, repeats)),
                dtype=[("event", _EVENT_WIDTH), ("count", np.uint16)])
        else:
            encoded = np.column_stack((events, repeats))
        return encoded

    def _get_events(self, decoded: bool) -> np.ndarray[np.str | np.uint8]:
        return self.i2e[self._events] if decoded else self._events


ONE = np.int8(1)


class ReachabilityData(CallbackData):
    """An event callback for computing reachability metrics.

    Attributes:
        adj: The message-reachability adjacency matrix. Entry `ij` is 1 if
            the initial message of user `i` reached user `j`,
            and 0 otherwise.
        msg_idx: A dictionary that maps a message ID to the origin user.
        msgs: A list of source-destination pairs where the `i`-th entry
        are the edges along which the initial score of user `i` was passed.
    """

    __slots__ = (
        "adj",
        "msg_idx",
        "msgs",
        "_msg_reach",
        "_reach_ratio",
        "_influence",
        "_source_size")

    def __init__(self) -> None:
        self.adj: sparse.csr_matrix | None = None
        self.msg_idx: dict[str, np.uint32] = {}
        self.msgs: dict[str, list] | list = collections.defaultdict(list)
        self._msg_reach: np.ndarray[np.uint8] | None = None
        self._reach_ratio: float | None = None
        self._influence: np.ndarray[int] | None = None
        self._source_size: np.ndarray[int] | None = None

    def __call__(self, record: Record, **kwargs) -> None:
        if Event.of(record) == Event.RECEIVE:
            source = np.uint32(record["from"])
            dest = np.uint32(record["to"])
            uid = record["id"]
            if source == dest:
                self.msg_idx[uid] = source
            else:
                origin = self.msg_idx[uid]
                edge = np.array([source, dest])
                self.msgs[origin].append(edge)

    def on_complete(self) -> None:
        self._to_numpy()
        self._sparsify()
        self._compute_source_size()
        self._compute_influence()
        self._compute_reach_ratio()
        self._compute_msg_reaches()

    def influence(self, user: int | None = None) -> int | np.ndarray[int]:
        """Returns the cardinality of the influence set for a given user."""
        return self._influence if user is None else self._influence[user]

    def source_size(self, user: int | None = None) -> int | np.ndarray[int]:
        """Returns the cardinality of the source set for a given user."""
        return self._source_size if user is None else self._source_size[user]

    def reach_ratio(self) -> float:
        """Returns the reachability ratio."""
        return self._reach_ratio

    def msg_reach(self, user: int | None = None) -> int | np.ndarray:
        """Returns the message reachability for a given user's initial score."""
        return self._msg_reach if user is None else self._msg_reach[user]

    def _to_numpy(self) -> None:
        # Assumes users are 0-based enumerated.
        self.msgs = [np.array(self.msgs[v]) for v in range(len(self.msgs))]

    def _sparsify(self) -> None:
        row, col, data = [], [], []
        row_add = row.extend
        col_add = col.extend
        data_add = data.extend
        repeat = itertools.repeat
        for user, edges in enumerate(self.msgs):
            # Keep user as a destination to show loops in the network.
            destinations = {dest for _, dest in edges}
            row_add(repeat(user, len(destinations)))
            col_add(destinations)
            data_add(repeat(ONE, len(destinations)))
        # adj[i][j] = 1: user `j` is reachable from user `i`
        self.adj = sparse.csr_matrix((data, (row, col)))

    def _compute_msg_reaches(self) -> None:
        shortest_path = sparse.csgraph.shortest_path
        longest = np.max
        nan2num = np.nan_to_num
        msg_reach = np.zeros(len(self.msgs), dtype=np.uint8)
        for user, edges in enumerate(self.msgs):
            if len(edges) > 0:
                idx, adj = edges_to_adj(edges)
                sp = shortest_path(adj, indices=idx[user])
                # Longest shortest path starting from the user
                msg_reach[user] = longest(nan2num(sp, copy=False, posinf=0))
        self._msg_reach = msg_reach

    def _compute_reach_ratio(self) -> None:
        self._reach_ratio = np.mean(self._influence) / self.adj.shape[0]

    def _compute_influence(self) -> None:
        self._influence = np.count_nonzero(self.adj.toarray(), axis=0)

    def _compute_source_size(self) -> None:
        self._source_size = np.count_nonzero(self.adj.toarray(), axis=1)


Edges = set[tuple[int, int]]
Index = dict[int, int]


def edges_to_adj(edges: Edges) -> tuple[Index, sparse.spmatrix]:
    """Encodes a set of edges as an adjacency matrix."""
    idx = index_edges(edges)
    adj = np.zeros((len(idx), len(idx)), dtype=np.uint8)
    for v1, v2 in edges:
        adj[idx[v1], idx[v2]] = ONE
    return idx, sparse.csr_matrix(adj)


def index_edges(edges: Edges) -> Index:
    """Encodes the vertices of the edges as 0-based integers."""
    return {v: i for i, v in enumerate(set(itertools.chain(*edges)))}


def analyze(logdir: str, *callbacks: EventCallback) -> None:
    """Analyze the event logs in the logging directory."""
    for event, callback in itertools.product(stream(logdir), callbacks):
        callback(event)
    for callback in callbacks:
        callback.on_complete()
