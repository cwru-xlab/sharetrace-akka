from __future__ import annotations

import collections
import json
import os
from typing import AnyStr

from hints import Record, Records


def load(path: os.PathLike | AnyStr) -> Records:
    with open(path) as f:
        settings = collections.defaultdict(dict)
        for line in f:
            record: Record = json.loads(line)
            info: Record = record["setting"]
            info.pop("type")
            settings[record["sid"]] = info
        return dict(settings)
