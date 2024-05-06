#!/usr/bin/env python3
import json
import sys
from pathlib import Path
from typing import Iterable

import polars as pl


def save_experiment(path: Path, experiment_type: str) -> None:
    datasets = _load_mapped_datasets(path, experiment_type)
    df = pl.DataFrame(datasets)
    df.write_parquet(path / "dataset.parquet")


def _load_mapped_datasets(path: Path, experiment_type: str) -> Iterable[dict]:
    datasets = _load_datasets(path)
    if experiment_type == "runtime":
        return map(_to_runtime_dataset, datasets)
    else:
        raise ValueError(experiment_type)


def _to_runtime_dataset(dataset: dict) -> dict:
    return {
        "id": dataset["id"],
        "key": dataset["key"],
        "net_id": dataset["network"]["id"],
        "net_type": dataset["networkFactory"]["type"],
        "nodes": dataset["network"]["nodes"],
        "edges": dataset["network"]["edges"],
        "ctr_type": dataset["networkFactory"]["timeFactory"]["random"]["type"],
        "svr_type": dataset["scoreFactory"]["random"]["type"],
        "str_type": dataset["scoreFactory"]["timeFactory"]["random"]["type"],
        "mp_runtime": dataset["results"]["runtime"]["MessagePassing"],
        "rp_runtime": dataset["results"]["runtime"]["RiskPropagation"],
        "tr": dataset["parameters"]["transmissionRate"],
        "sc": dataset["parameters"]["sendCoefficient"]
    }


def _load_datasets(path: Path) -> Iterable[dict]:
    for dataset_path in _iterdir(path):
        yield from _load_dataset(dataset_path)


def _iterdir(path: Path) -> Iterable[Path]:
    return (p for p in path.iterdir() if p.is_dir())


def _load_dataset(path: Path) -> Iterable[dict]:
    with (path / "results.json").open() as f:
        results = json.load(f)
    with (path / "properties.log").open() as f:
        for props in map(json.loads, f):
            yield {
                "id": path.name,
                "key": (key := props["k"]),
                "results": results[key],
                **props["p"]
            }


if __name__ == '__main__':
    save_experiment(path=Path(sys.argv[1]), experiment_type=sys.argv[2])
