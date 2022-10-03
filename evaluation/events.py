from __future__ import annotations

import abc
import collections
import dataclasses
import fileinput
import gzip
import itertools
import json
import shutil
import sys
from collections.abc import Callable, Iterable
from enum import Enum
from os import PathLike
from pathlib import Path
from tempfile import TemporaryDirectory
from typing import Iterator, final, ContextManager, AnyStr

import numpy as np
from scipy import sparse
from tqdm.notebook import tqdm

from hints import Record

_ONE_8 = np.uint8(1)
_ONE_16 = np.uint16(1)
_ONE_64 = np.int64(1)
_ZERO_16 = np.uint16(0)


class Event(str, Enum):
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

    def __new__(cls, name: str) -> Event:
        ordinal = len(cls.__members__)
        event = str.__new__(cls, name)
        event.ordinal = ordinal
        return event

    def __repr__(self) -> str:
        return self.value

    def __str__(self) -> str:
        return self.value

    def __int__(self) -> int:
        return self.ordinal

    @classmethod
    def of(cls, record: Record) -> Event:
        return Event(record["type"])


_EVENT_WIDTH = f"<U{max(len(e) for e in Event)}"

_READ_MODE = "rb"
_WRITE_MODE = "wb"
_ZIP_EXT = ".log.gz"
_LOG_EXT = ".log"


class LogStream(Callable, ContextManager):
    __slots__ = ("logdir", "bytes", "_tmp_dir", "_files")

    def __init__(self, logdir: PathLike | AnyStr):
        self.logdir = Path(logdir).absolute()
        self.bytes = 0
        self._tmp_dir: TemporaryDirectory | None = None
        self._files: list[PathLike] | None = None

    def __enter__(self) -> LogStream:
        zipped, unzipped = self._logfiles()
        self._tmp_dir = TemporaryDirectory()
        tmp_path = Path(self._tmp_dir.name).absolute()
        _unzip(zipped, tmp_path)
        self.bytes = _sizeof(unzipped) + _sizeof(tmp_path.iterdir())
        self._files = [*sorted(tmp_path.iterdir()), *sorted(unzipped)]
        return self

    def __exit__(self, *args) -> None:
        self._tmp_dir.cleanup()
        self._tmp_dir = None
        self._files = None
        self.bytes = 0

    def __call__(self) -> Iterable[str]:
        with fileinput.input(self._files, mode=_READ_MODE) as lines:
            yield from lines

    def _logfiles(self) -> tuple[list[Path], list[Path]]:
        logs = self.logdir.iterdir()
        files = [f for f in logs if f.name.endswith((_LOG_EXT, _ZIP_EXT))]
        unzipped = _endswith_split(files, _LOG_EXT)
        zipped = _endswith_split(files, _ZIP_EXT)
        zipped = (path for name, path in zipped.items() if name not in unzipped)
        return list(zipped), list(unzipped.values())


def _endswith_split(files: Iterable[Path], ext: str) -> dict:
    return {f.name.split(ext)[0]: f for f in files if f.name.endswith(ext)}


def _sizeof(files: Iterable[PathLike]) -> int:
    with fileinput.input(files, mode=_READ_MODE) as lines:
        return sum(map(sys.getsizeof, lines))


def _unzip(zipped: Iterable[Path], dst: Path) -> None:
    for filename in zipped:
        with gzip.open(filename, _READ_MODE) as f_in:
            logfile = f_in.name.split("/")[-1].split(_ZIP_EXT)[0]
            with dst.joinpath(logfile).open(_WRITE_MODE) as f_out:
                shutil.copyfileobj(f_in, f_out)


class EventCounter(collections.Counter):
    """Counts the number of occurrences of each event type."""

    def __setitem__(self, key: Event, value: int) -> None:
        if isinstance(key, Event):
            super().__setitem__(key, value)
        else:
            raise TypeError(f"'key' must be an {Event.__name__} instance")

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
    __slots__ = ("_states",)

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
    symptom: np.float32
    exposure: np.float32 | None = None
    updates: np.uint16 = _ZERO_16

    def __post_init__(self):
        if self.exposure is None:
            self.exposure = self.symptom


class UpdatesData(CallbackData):
    """An event callback for tracking user updates.

    Attributes:
       updates (ndarray): i-th entry is the number of updates of user i.
       symptoms (ndarray): i-th entry is the symptom score of user i.
       exposures (ndarray): i-th entry is the exposure score of user i.
       num_updated (int): Number of users that were updated.
       num_updates (int): Number of updates for all users.
    """
    __slots__ = (
        "updates", "symptoms", "exposures", "num_updated", "num_updates")

    def __init__(self) -> None:
        self.updates: dict | np.ndarray = {}
        self.symptoms: np.ndarray | None = None
        self.exposures: np.ndarray | None = None
        self.num_updated = 0
        self.num_updates = 0

    def __call__(self, record: Record, **kwargs) -> None:
        if Event.of(record) == Event.UPDATE:
            score = np.float32(record["newScore"]["value"])
            if (name := np.uint32(record["to"])) in self.updates:
                user = self.updates[name]
                user.updates += _ONE_16
                user.exposure = score
            else:
                self.updates[name] = UserUpdates(symptom=score)

    def on_complete(self) -> None:
        num_users = len(self.updates)
        updates = np.zeros(num_users, dtype=np.uint16)
        self.symptoms = np.zeros(num_users, dtype=np.float32)
        self.exposures = np.zeros(num_users, dtype=np.float32)
        for u, user in self.updates.items():
            user = dataclasses.astuple(user)
            self.symptoms[u], self.exposures[u], updates[u] = user
        self.updates = updates
        self.num_updates = np.sum(updates)
        self.num_updated = np.count_nonzero(updates)


class ReceivedData(CallbackData):
    __slots__ = ("values",)

    def __init__(self):
        self.values: list | np.ndarray = []

    def __call__(self, record: Record, **kwargs) -> None:
        if Event.of(record) == Event.RECEIVE:
            self.values.append(np.float32(record["score"]["value"]))

    def on_complete(self) -> None:
        self.values = np.array(self.values)


class TimelineData(CallbackData):
    """An event callback for recording the order of events.

    Attributes:
        e2i: An dictionary that maps an Event to its encoded integer.
        i2e: An array where the i-th entry is the Event value encoded as i.
    """
    __slots__ = ("e2i", "i2e", "timestamps", "_events", "_repeats")

    def __init__(self):
        self.e2i = {}
        self.i2e: np.ndarray | None = None
        self.timestamps: list | np.ndarray = []
        self._events: list | np.ndarray = [None]
        self._repeats: list | np.ndarray = [0]

    def __call__(self, record: Record, **kwargs) -> None:
        self.timestamps.append(np.uint64(record["timestamp"]))
        if (event := Event.of(record)) not in self.e2i:
            self.e2i[event] = len(self.e2i)
        if (label := self.e2i[event]) == self._events[-1]:
            self._repeats[-1] += _ONE_64
        else:
            self._events.append(label)
            self._repeats.append(_ONE_64)

    def on_complete(self) -> None:
        t0 = np.min(times := self.timestamps)
        self.timestamps = np.array([t - t0 for t in times], dtype=np.uint32)
        self._events = np.array(self._events[1:], dtype=np.uint8)
        self._repeats = np.array(self._repeats[1:], dtype=np.int64)
        self.i2e = np.array([e.value for e in self.e2i], dtype=_EVENT_WIDTH)

    def flatten(self, decoded: bool = False) -> np.ndarray:
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
                dtype=[("event", _EVENT_WIDTH), ("count", np.uint64)])
        else:
            encoded = np.column_stack((events, repeats))
        return encoded

    def _get_events(self, decoded: bool) -> np.ndarray[np.str | np.uint8]:
        return self.i2e[self._events] if decoded else self._events


class ReachabilityData(CallbackData):
    """An event callback for computing reachability metrics.

    Attributes:
        adj (ndarray): Message-reachability adjacency matrix. Entry `ij` is
            1 if the initial message of user `i` reached user `j`, and 0
            otherwise.
        msg_idx (dict): Maps a message ID to the origin user.
        msgs (list): Source-destination pairs where the `i`-th entry
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
        self.msg_idx: dict = {}
        self.msgs: dict | list = collections.defaultdict(list)
        self._msg_reach: np.ndarray | None = None
        self._reach_ratio: float | None = None
        self._influence: np.ndarray | None = None
        self._source_size: np.ndarray | None = None

    def __call__(self, record: Record, **kwargs) -> None:
        if Event.of(record) == Event.RECEIVE:
            src = np.uint32(record["from"])
            dst = np.uint32(record["to"])
            uid = record["id"]
            if src == dst:
                self.msg_idx[uid] = src
            else:
                origin = self.msg_idx[uid]
                edge = np.array([src, dst])
                self.msgs[origin].append(edge)

    def on_complete(self) -> None:
        self._to_numpy()
        self._sparsify()
        self._compute_source_size()
        self._compute_influence()
        self._compute_reach_ratio()
        self._compute_msg_reaches()

    def influence(self, user: int | None = None) -> int | np.ndarray:
        """Returns the cardinality of the user's influence set.

        A user's influence set is the set of users that received the
        user's symptom score. In other words, the influence set is the set of
        users that are message-reachable per the user's symptom score.
        """
        return self._influence if user is None else self._influence[user]

    def source_size(self, user: int | None = None) -> int | np.ndarray:
        """Returns the cardinality of the user's source set.

        A user's source set is the set of users whose symptom score was
        received by the user. In other words, the source set is the set of
        users whose symptom scores are message-reachable to the user.
        """
        return self._source_size if user is None else self._source_size[user]

    def reach_ratio(self) -> float:
        """Returns the reachability ratio.

        The reachability ratio is the average size of a user's influence set.
        """
        return self._reach_ratio

    def msg_reach(self, user: int | None = None) -> int | np.ndarray:
        """Returns the message reachability for a user's symptom score."""
        return self._msg_reach if user is None else self._msg_reach[user]

    def _to_numpy(self) -> None:
        # Assumes users are 0-based enumerated.
        self.msgs = [np.array(self.msgs[v]) for v in range(max(self.msgs) + 1)]

    def _sparsify(self) -> None:
        row, col, data = [], [], []
        for user, edges in enumerate(self.msgs):
            # Keep user as a destination to show loops in the network.
            dsts = {dst for _, dst in edges}
            row.extend(itertools.repeat(user, len(dsts)))
            col.extend(dsts)
            data.extend(itertools.repeat(_ONE_8, len(dsts)))
        # adj[i][j] = 1: user `j` is message-reachable from user `i`
        self.adj = sparse.csr_matrix((data, (row, col)))

    def _compute_msg_reaches(self) -> None:
        msg_reach = np.zeros(len(self.msgs), dtype=np.uint8)
        for user, edges in enumerate(self.msgs):
            if len(edges) > 0:
                idx, adj = edges_to_adj(edges)
                sp = sparse.csgraph.shortest_path(adj, indices=idx[user])
                # Longest shortest path starting from the user
                reach = np.max(np.nan_to_num(sp, copy=False, posinf=0))
            else:
                reach = 0
            msg_reach[user] = reach
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
        adj[idx[v1], idx[v2]] = _ONE_8
    return idx, sparse.csr_matrix(adj)


def index_edges(edges: Edges) -> Index:
    """Encodes the vertices of the edges as 0-based integers."""
    return {v: i for i, v in enumerate(set(itertools.chain(*edges)))}


def analyze(logdir: str, *callbacks: EventCallback) -> None:
    """Analyze the event logs in the logging directory."""
    with LogStream(logdir) as stream:
        size = (stream.bytes + 1) * len(callbacks)
        with tqdm(total=size, unit_scale=True) as t:
            for event, callback in itertools.product(stream(), callbacks):
                callback(json.loads(event))
                t.update(sys.getsizeof(event))
            for callback in callbacks:
                callback.on_complete()
                t.update()
