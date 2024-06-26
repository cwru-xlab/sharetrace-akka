#!/usr/bin/env python3

import math
import pathlib


def print_dataset_time_ranges():
    for path in pathlib.Path("./lib/src/main/resources/datasets").iterdir():
        with path.open() as dataset:
            min_time = math.inf
            max_time = -math.inf
            for contact in dataset:
                t, *_ = contact.split()
                min_time = min(min_time, int(t))
                max_time = max(max_time, int(t))
            range_days = round((max_time - min_time) / 86400, 2)
            print(f"{path.stem}: {range_days} days")


if __name__ == "__main__":
    print_dataset_time_ranges()
