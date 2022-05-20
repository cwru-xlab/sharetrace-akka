from __future__ import annotations

import os
from json import loads
from typing import AnyStr


def load_metrics(path: os.PathLike | AnyStr) -> dict:
    metrics = {}
    with open(path) as f:
        for line in f:
            metric = loads(line)["metric"]
            metrics[metric.pop("name")] = metric
    return metrics
