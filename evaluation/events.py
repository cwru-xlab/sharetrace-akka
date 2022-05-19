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
from json import loads
from typing import Any, AnyStr, Callable, Iterable, Tuple

Predicate = Callable[[Any], bool]


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


def event_stream(logdir: os.PathLike | AnyStr) -> Iterable[dict]:
    logdir = pathlib.Path(logdir).absolute()
    zipped, unzipped = _split(logdir.iterdir(), _is_zipped)
    for event in _stream(unzipped):
        yield event
    with tempfile.TemporaryDirectory() as tmp_dir:
        tmp_dir = pathlib.Path(tmp_dir).absolute()
        _unzip(zipped, tmp_dir)
        for event in _stream(tmp_dir.iterdir()):
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


def _split(it: Iterable, predicate: Predicate) -> Tuple[Iterable, Iterable]:
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

    @property
    def n_users(self) -> int:
        return self.n_received - self.n_sent

    @property
    def n_sent(self) -> int:
        return self.n_to_contacts + self.n_propagated

    @property
    def n_to_contacts(self) -> int:
        return self.n_current_sent + self.n_cached_sent

    @property
    def n_received(self) -> int:
        return self[Event.RECEIVE]

    @property
    def n_current_sent(self) -> int:
        return self[Event.SEND_CURRENT]

    @property
    def n_cached_sent(self) -> int:
        return self[Event.SEND_CACHED]

    @property
    def n_propagated(self) -> int:
        return self[Event.PROPAGATE]

    @property
    def n_updates(self) -> int:
        # Disregard the initialization update of each user.
        return self[Event.UPDATE] - self.n_users

    @property
    def n_contacts(self) -> int:
        # Each user logs a contact, so each contact is double counted.
        return int(self[Event.CONTACT] / 2)

    @property
    def n_contact_refreshes(self) -> int:
        return self[Event.CONTACTS_REFRESH]

    @property
    def n_current_refreshes(self) -> int:
        return self[Event.CURRENT_REFRESH]

    @property
    def n_not_to_contacts(self) -> int:
        # Each user sends a message, so double the number of contacts.
        return 2 * self.n_contacts - self.n_to_contacts


# Callbacks

class EventCallback(Callable):
    __slots__ = ()

    def __init__(self):
        super().__init__()

    @abc.abstractmethod
    def __call__(self, event: dict, **kwargs) -> None:
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


def analyze_events(logdir: str, *callbacks: EventCallback):
    for event, callback in itertools.product(event_stream(logdir), callbacks):
        callback(event)


if __name__ == '__main__':
    path = "..//logs//20220519010415//events"
    counter = EventCounterCallback()
    updates = UserUpdatesCallback()
    timeline = TimelineCallback()
    analyze_events(path, counter, updates, timeline)
