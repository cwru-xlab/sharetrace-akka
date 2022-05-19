from __future__ import annotations

import os
from json import loads
from typing import AnyStr


def load(path: os.PathLike | AnyStr) -> dict:
    with open(path) as f:
        return loads(f.readline())["setting"]
