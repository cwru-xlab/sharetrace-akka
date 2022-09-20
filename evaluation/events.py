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
from typing import Any

import numpy as np
from scipy import sparse

Predicate = Callable[[Any], bool]
EventRecord = dict[str, Any]

ONE = np.int8(1)


class Event(str, enum.Enum):
    CONTACT = "ContactEvent"
    CONTACTS_REFRESH = "ContactsRefreshEvent"
    CURRENT_REFRESH = "CurrentRefreshEvent"
    PROPAGATE = "PropagateEvent"
    RECEIVE = "ReceiveEvent"
    SEND_CACHED = "SendCachedEvent"
    SEND_CURRENT = "SendCurrentEvent"
    UPDATE = "UpdateEvent"

    def __repr__(self):
        return self.value

    def __int__(self):
        return _EVENTS[self.value]

    @classmethod
    def of(cls, record: EventRecord) -> Event:
        return Event(record["type"])


_EVENTS = {v: i for i, v in enumerate(Event)}


def event_stream(logdir: os.PathLike | str) -> Iterable[EventRecord]:
    logdir = pathlib.Path(logdir).absolute()
    zipped, unzipped = _split(logdir.iterdir(), _is_zipped)
    # Iterate over zipped first! Unzipped events occur last.
    with tempfile.TemporaryDirectory() as tmpdir:
        tmpdir = pathlib.Path(tmpdir).absolute()
        _unzip(zipped, tmpdir)
        for event in _stream(tmpdir.iterdir()):
            yield event
    for event in _stream(unzipped):
        yield event


def _stream(filenames: Iterable[pathlib.Path]) -> Iterable[EventRecord]:
    for filename in sorted(filenames):
        with filename.open() as log:
            for line in log:
                yield json.loads(line)["event"]


def _is_zipped(path: pathlib.Path) -> bool:
    return path.name.endswith(".zip")


def _unzip(zipped: Iterable[os.PathLike], dest: os.PathLike) -> None:
    for filename in zipped:
        with zipfile.ZipFile(filename) as f:
            f.extractall(dest)


def _split(it: Iterable, predicate: Predicate) -> tuple[Iterable, Iterable]:
    it = list(it)
    true = (e for e in it if predicate(e))
    false = (e for e in it if not predicate(e))
    return true, false


class EventCounter(collections.Counter):

    def __setitem__(self, key, value):
        if key in _EVENTS:
            super().__setitem__(key, value)
        else:
            raise ValueError("'key' must be an Event instance")

    @property
    def num_sent_to_contacts(self) -> int:
        return self.num_curr_sent + self.num_cached_sent

    @property
    def num_received(self) -> int:
        return self[Event.RECEIVE]

    @property
    def num_curr_sent(self) -> int:
        return self[Event.SEND_CURRENT]

    @property
    def num_cached_sent(self) -> int:
        return self[Event.SEND_CACHED]

    @property
    def num_propagated(self) -> int:
        return self[Event.PROPAGATE]

    @property
    def num_updates(self) -> int:
        return self[Event.UPDATE]

    @property
    def num_contacts(self) -> int:
        # Each user logs a contact, so each contact is double counted.
        return int(self[Event.CONTACT] / 2)

    @property
    def num_contact_refreshes(self) -> int:
        return self[Event.CONTACTS_REFRESH]

    @property
    def num_curr_refreshes(self) -> int:
        return self[Event.CURRENT_REFRESH]

    @property
    def num_not_to_contacts(self) -> int:
        # Each user sends a message, so double the number of contacts.
        return 2 * self.num_contacts - self.num_sent_to_contacts


# Callbacks

class EventCallback(Callable):

    @abc.abstractmethod
    def __call__(self, record: EventRecord, **kwargs) -> None:
        pass

    def on_complete(self) -> None:
        pass


class EventCounterCallback(EventCounter, EventCallback):

    def __call__(self, record: EventRecord, **kwargs) -> None:
        self.update((Event.of(record),))


@dataclasses.dataclass(slots=True)
class UserUpdates:
    initial: np.float32
    final: np.float32 | None = None
    n: np.uint16 = np.uint16(0)

    def __post_init__(self) -> None:
        if self.final is None:
            self.final = self.initial


class UserUpdatesCallback(EventCallback, Sized):
    __slots__ = "updates", "initials", "finals", "_n"

    def __init__(self) -> None:
        self.updates: dict[np.uint32, UserUpdates] | np.ndarray = {}
        self.initials: np.ndarray | None = None
        self.finals: np.ndarray | None = None
        self._n = 0

    def __call__(self, record: EventRecord, **kwargs) -> None:
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
        updates = np.zeros(n_users, dtype=np.int16)
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


class TimelineCallback(EventCallback):
    __slots__ = "_events", "_repeats", "idx"

    def __init__(self):
        self._events: list[Event | None] | np.ndarray = [None]
        self._repeats: list[int] | np.ndarray = [0]
        self.idx: dict | np.ndarray = dict()

    def __call__(self, record: EventRecord, **kwargs) -> None:
        if (name := Event.of(record)) not in self.idx:
            self.idx[name] = len(self.idx)
        if (label := self.idx[name]) == self._events[-1]:
            self._repeats[-1] += 1
        else:
            self._events.append(label)
            self._repeats.append(1)

    def on_complete(self) -> None:
        self._events = np.array(self._events[1:])
        self._repeats = np.array(self._repeats[1:])
        self.idx = np.array(list(self.idx))

    def flatten(self, labeled: bool = False) -> np.ndarray:
        return np.repeat(self._get_events(labeled), self._repeats)

    def run_length_encoded(self, labeled: bool = False) -> np.ndarray:
        return np.array(list(zip(self._get_events(labeled), self._repeats)))

    def _get_events(self, labeled: bool = False) -> np.ndarray:
        return self.idx[self._events] if labeled else self._events


class ReachabilityCallback(EventCallback):
    __slots__ = (
        "adj",
        "msg_idx",
        "msgs",
        "_msg_reach",
        "_reach_ratio",
        "_influence",
        "_source_size")

    def __init__(self):
        super().__init__()
        self.adj: sparse.csr_matrix | None = None
        self.msg_idx: dict[str, np.uint32] = {}
        self.msgs: dict | list = collections.defaultdict(list)
        self._msg_reach: np.ndarray | None = None
        self._reach_ratio: float | None = None
        self._influence: np.ndarray | None = None
        self._source_size: np.ndarray | None = None

    def __call__(self, record: dict, **kwargs) -> None:
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

    def _to_numpy(self) -> None:
        # msgs[i] = (to, from) pairs that sent message starting from user i.
        self.msgs = [np.array(self.msgs[v]) for v in range(len(self.msgs))]

    def influence(self, user: int | None = None) -> int | np.ndarray:
        """Returns the cardinality of the influence set for a given user."""
        return self._influence if user is None else self._influence[user]

    def source_size(self, user: int | None = None) -> int | np.ndarray:
        """Returns the cardinality of the source set for a given user."""
        return self._source_size if user is None else self._source_size[user]

    def reach_ratio(self) -> float:
        """Returns the reachability ratio."""
        return self._reach_ratio

    def msg_reach(self, user: int | None = None) -> int | np.ndarray:
        """Returns the message reachability for a given user's initial score."""
        return self._msg_reach if user is None else self._msg_reach[user]

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
        # adj[i][j] = 1: user j is reachable from user i
        self.adj = sparse.csr_matrix((data, (row, col)))

    def _compute_msg_reaches(self) -> None:
        shortest_path = sparse.csgraph.shortest_path
        longest = np.max
        nan2num = np.nan_to_num
        msg_reach = np.zeros(len(self.msgs), dtype=np.int8)
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


def edges_to_adj(edges: set[tuple]) -> tuple[dict, sparse.spmatrix]:
    idx = index_edges(edges)
    adj = np.zeros((len(idx), len(idx)), dtype=np.int8)
    for v1, v2 in edges:
        adj[idx[v1], idx[v2]] = ONE
    return idx, sparse.csr_matrix(adj)


def index_edges(edges: set[tuple]) -> dict[int, int]:
    return {v: i for i, v in enumerate(set(itertools.chain(*edges)))}


def analyze_events(logdir: str, *callbacks: EventCallback) -> None:
    for event, callback in itertools.product(event_stream(logdir), callbacks):
        callback(event)
    for callback in callbacks:
        callback.on_complete()
