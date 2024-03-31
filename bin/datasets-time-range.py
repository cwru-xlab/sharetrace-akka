#!/bin/python

import pathlib
import math

for path in pathlib.Path("./lib/src/main/resources/datasets").iterdir():
    with path.open() as dataset:
        min_time = math.inf
        max_time = -math.inf
        for contact in dataset:
            t, *_ = contact.split()
            min_time = min(min_time, int(t))
            max_time = max(max_time, int(t))
        range_days = round((max_time - min_time) / 86400, 2)
        print(f"{path.name}: {range_days} days")