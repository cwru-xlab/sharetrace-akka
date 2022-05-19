from __future__ import annotations

import os
import pprint
from json import loads
from typing import AnyStr


def load(path: os.PathLike | AnyStr) -> dict:
    metrics = {}
    with open(path) as f:
        for line in f:
            metric = loads(line)["metric"]
            metrics[metric.pop("name")] = metric
    return metrics


if __name__ == '__main__':
    pprint.pprint(load("..//logs//20220518144513//metrics.log"))
