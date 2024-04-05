import copy
import json
import os
import pathlib
from typing import Iterable, Callable

import dotmap

PathLike = str | os.PathLike[str]
Query = Callable[[dotmap.DotMap], bool]
DotMaps = Iterable[dotmap.DotMap]


def iterdir(path: PathLike) -> Iterable[pathlib.Path]:
    return (p for p in pathlib.Path(path).iterdir() if p.is_dir())


def parse_properties(string: str) -> dotmap.DotMap:
    props = dotmap.DotMap(json.loads(string))
    props.key = props.pop("k")
    props.update(props.pop("p"))
    return props


def query_properties(path: PathLike, query: Query) -> Iterable[tuple[pathlib.Path, DotMaps]]:
    for dataset in iterdir(path):
        with dataset.joinpath("properties.log").open() as f:
            if properties := list(filter(query, map(parse_properties, f))):
                yield dataset, properties


def query_results(path: PathLike, query: Query) -> DotMaps:
    for dataset, properties in query_properties(path, query):
        with dataset.joinpath("results.json").open() as f:
            results = dotmap.DotMap(json.load(f))
            for props in properties:
                results = copy.copy(results)
                results.properties = props
                yield results
