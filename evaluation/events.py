from __future__ import annotations

import abc
import collections
import dataclasses
import enum
import itertools
import os
import pathlib
import tempfile
import zipfile
from collections.abc import Callable, Iterable, Collection
from json import loads
from typing import Any

import numpy as np
from scipy import sparse
from scipy.sparse import csgraph

Predicate = Callable[[Any], bool]

ONE = np.int8(1)


class Event(str, enum.Enum):
    SEND_CURRENT = "SendCurrentEvent"
    SEND_CACHED = "SendCachedEvent"
    UPDATE = "UpdateEvent"
    PROPAGATE = "PropagateEvent"
    CONTACT = "ContactEvent"
    RECEIVE = "ReceiveEvent"
    CONTACTS_REFRESH = "ContactsRefreshEvent"
    CURRENT_REFRESH = "CurrentRefreshEvent"

    def __repr__(self):
        return self.value


def event_stream(logdir: os.PathLike | str) -> Iterable[dict]:
    logdir = pathlib.Path(logdir).absolute()
    zipped, unzipped = _split(logdir.iterdir(), _is_zipped)
    for event in _stream(unzipped):
        yield event
    with tempfile.TemporaryDirectory() as tmpdir:
        tmpdir = pathlib.Path(tmpdir).absolute()
        _unzip(zipped, tmpdir)
        for event in _stream(tmpdir.iterdir()):
            yield event


def _stream(filenames: Iterable[pathlib.Path]) -> Iterable[dict]:
    for filename in filenames:
        with filename.open() as log:
            for line in log:
                yield loads(line)["event"]


def _is_zipped(path: pathlib.Path) -> bool:
    return path.name.endswith(".zip")


def _unzip(zipped: Iterable[os.PathLike], dst: os.PathLike) -> None:
    for filename in zipped:
        with zipfile.ZipFile(filename) as f:
            f.extractall(dst)


def _split(it: Iterable, predicate: Predicate) -> tuple[Iterable, Iterable]:
    it = list(it)
    true = (e for e in it if predicate(e))
    false = (e for e in it if not predicate(e))
    return true, false


class EventCounter(collections.Counter):
    _EVENTS = set(Event)

    def __setitem__(self, key, value):
        if key in EventCounter._EVENTS:
            super().__setitem__(key, value)
        else:
            raise ValueError("'key' must be an Event instance")

    def n_users(self) -> int:
        return self.n_received() - self.n_sent()

    def n_sent(self) -> int:
        return self.n_to_contacts() + self.n_propagated()

    def n_to_contacts(self) -> int:
        return self.n_current_sent() + self.n_cached_sent()

    def n_received(self) -> int:
        return self[Event.RECEIVE]

    def n_current_sent(self) -> int:
        return self[Event.SEND_CURRENT]

    def n_cached_sent(self) -> int:
        return self[Event.SEND_CACHED]

    def n_propagated(self) -> int:
        return self[Event.PROPAGATE]

    def n_updates(self) -> int:
        # Disregard the initialization update of each user.
        return self[Event.UPDATE] - self.n_users

    def n_contacts(self) -> int:
        # Each user logs a contact, so each contact is double counted.
        return int(self[Event.CONTACT] / 2)

    def n_contact_refreshes(self) -> int:
        return self[Event.CONTACTS_REFRESH]

    def n_current_refreshes(self) -> int:
        return self[Event.CURRENT_REFRESH]

    def n_not_to_contacts(self) -> int:
        # Each user sends a message, so double the number of contacts.
        return 2 * self.n_contacts() - self.n_to_contacts()


# Callbacks

class EventCallback(Callable[[dict], None]):
    __slots__ = ()

    def __init__(self):
        super().__init__()

    @abc.abstractmethod
    def __call__(self, event: dict, **kwargs) -> None:
        pass

    def on_complete(self) -> None:
        pass


class EventCounterCallback(EventCounter, EventCallback):

    def __init__(self):
        super().__init__()

    def __call__(self, event: dict, **kwargs) -> None:
        self.update((event["name"],))


@dataclasses.dataclass(slots=True)
class UserUpdates:
    initial: float
    final: float
    n: int = 0


class UserUpdatesCallback(EventCallback):
    __slots__ = "updates"

    def __init__(self):
        super().__init__()
        self.updates = {}

    def __call__(self, event: dict, **kwargs) -> None:
        if event["name"] == Event.UPDATE:
            if (name := event["to"]) in self.updates:
                user = self.updates[name]
                user.n += 1
                user.final = event["newScore"]["value"]
            else:
                value = event["newScore"]["value"]
                self.updates[name] = UserUpdates(initial=value, final=value)


class TimelineCallback(EventCallback):
    __slots__ = "timeline", "n"

    def __init__(self):
        super().__init__()
        self.timeline = collections.defaultdict(list)
        self.n = 0

    def __call__(self, event: dict, **kwargs) -> None:
        self.timeline[event["name"]].append(self.n)
        self.n += 1


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
        self.adj = None
        self.msg_idx = {}
        self.msgs = collections.defaultdict(list)
        self._msg_reach = None
        self._reach_ratio = None
        self._influence = None
        self._source_size = None

    def __call__(self, event: dict, **kwargs) -> None:
        if event["name"] == Event.RECEIVE:
            src, dst = np.int32(event["from"]), np.int32(event["to"])
            uuid = event["uuid"]
            if src == dst:
                self.msg_idx[uuid] = src
            else:
                origin = self.msg_idx[uuid]
                self.msgs[origin].append(np.array([src, dst]))

    def on_complete(self) -> None:
        self._sparsify()
        self._compute_source_size()
        self._compute_influence()
        self._compute_reach_ratio()
        self._compute_msg_reaches()

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
        row_add, col_add, data_add = row.extend, col.extend, data.extend
        repeat = itertools.repeat
        for user, edges in self.msgs.items():
            dst = set(dst for _, dst in edges)
            row_add(repeat(user, len(dst)))
            col_add(dst)
            data_add(repeat(ONE, len(dst)))
        self.adj = sparse.csr_matrix((data, (row, col)))

    # noinspection PyTypeChecker
    def _compute_msg_reaches(self):
        shortest_path = csgraph.shortest_path
        longest, nan2num = np.max, np.nan_to_num
        msg_reach = np.zeros(len(self.msgs), dtype=np.int8)
        for user, edges in self.msgs.items():
            idx, adj = edges_to_adj(edges)
            sp = shortest_path(adj, indices=idx[user])
            msg_reach[user] = longest(nan2num(sp, copy=False, posinf=0))
        self._msg_reach = msg_reach

    def _compute_reach_ratio(self) -> None:
        reached = self.adj.count_nonzero()
        n = self.adj.shape[0]
        max_possible = n ** 2 - n  # Exclude sending to self
        self._reach_ratio = reached / max_possible

    def _compute_influence(self) -> None:
        self._influence = np.count_nonzero(self.adj.toarray(), axis=0)

    def _compute_source_size(self) -> None:
        self._source_size = np.count_nonzero(self.adj.toarray(), axis=1)


def edges_to_adj(edges: Collection[tuple]) -> tuple[dict, sparse.spmatrix]:
    idx = index_edges(edges)
    adj = np.zeros((len(idx), len(idx)), dtype=np.int8)
    for v1, v2 in edges:
        adj[idx[v1], idx[v2]] = 1
    return idx, sparse.csr_matrix(adj)


def index_edges(edges: Collection[tuple]) -> dict[int, int]:
    return {v: i for i, v in enumerate(set(itertools.chain(*edges)))}


def analyze_events(logdir: str, *callbacks: EventCallback) -> None:
    for event, callback in itertools.product(event_stream(logdir), callbacks):
        callback(event)
    for callback in callbacks:
        callback.on_complete()
