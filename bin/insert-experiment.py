#!/usr/bin/env python3

import json
import os
import pathlib
import sys
from typing import Iterable

from pymongo import MongoClient

PathLike = str | os.PathLike[str]


def insert_experiment(client: MongoClient, experiment: pathlib.Path):
    db = client[experiment.name]
    for dataset in _iterdir(experiment):
        db["properties"].insert_many(_load_properties(dataset))
        db["results"].insert_many(_load_results(dataset))


def _iterdir(path: pathlib.Path) -> Iterable[pathlib.Path]:
    return (p for p in path.iterdir() if p.is_dir())


def _load_properties(dataset: pathlib.Path) -> Iterable[dict]:
    with dataset.joinpath("properties.log").open() as f:
        for line in f:
            props = json.loads(line)
            props.update(key=props.pop("k"), dataset=dataset.name, **props.pop("p"))
            yield props


def _load_results(dataset: pathlib.Path) -> Iterable[dict]:
    with dataset.joinpath("results.json").open() as f:
        results = json.load(f)
        for key, value in results.items():
            value.update(key=key, dataset=dataset.name)
            yield value


if __name__ == '__main__':
    insert_experiment(MongoClient(), pathlib.Path(sys.argv[1]))
