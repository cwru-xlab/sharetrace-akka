#!/usr/bin/env python3

import json
import os
import pathlib
import sys
from typing import Iterable, Any

import humps
from pymongo import MongoClient

PathLike = str | os.PathLike[str]


def insert_experiment(client: MongoClient, path: pathlib.Path):
    experiment = client["experiments"][humps.camelize(path.name)]
    for dataset in _iterdir(path):
        experiment.insert_many(_load_dataset(dataset))


def _iterdir(path: pathlib.Path) -> Iterable[pathlib.Path]:
    return (p for p in path.iterdir() if p.is_dir())


def _load_dataset(path: pathlib.Path) -> Iterable[dict[str, Any]]:
    results = _load_results(path)
    for props in _load_properties(path):
        yield {"results": results[props["key"]], **props}


def _load_results(path: pathlib.Path) -> dict[str, Any]:
    with path.joinpath("results.json").open() as f:
        return json.load(f)


def _load_properties(path: pathlib.Path) -> Iterable[dict[str, Any]]:
    with path.joinpath("properties.log").open() as f:
        for line in f:
            props = json.loads(line)
            props.update(key=props.pop("k"), dataset=path.name, **props.pop("p"))
            yield props


if __name__ == '__main__':
    insert_experiment(MongoClient(), pathlib.Path(sys.argv[1]))
