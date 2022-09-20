from __future__ import annotations

import collections
import json
import os
from typing import AnyStr, Any


def load(path: os.PathLike | AnyStr) -> dict:
    with open(path) as f:
        settings = collections.defaultdict(dict)
        for line in f:
            record: dict[str, Any] = json.loads(line)
            info: dict[str, Any] = record["setting"]
            info.pop("type")
            settings[record["sid"]] = info
        return dict(settings)
