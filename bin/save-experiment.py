#!/usr/bin/env python3
import json
import sys
from pathlib import Path
from typing import Iterable

import polars as pl


def save_experiment(path: Path, kind: str) -> None:
    datasets = _load_datasets(path, kind)
    df = pl.DataFrame(datasets)
    df.write_parquet(path / "experiment.parquet")


def _load_datasets(path: Path, kind: str) -> Iterable[dict]:
    datasets = _load_raw_datasets(path)
    if kind == "runtime":
        return map(_to_runtime_dataset, datasets)
    elif kind == "parameter":
        return map(_to_parameter_dataset, datasets)
    else:
        raise ValueError(kind)


def _to_runtime_dataset(dataset: dict) -> dict:
    return _common_properties(dataset) | _runtime_results(dataset)


def _to_parameter_dataset(dataset: dict) -> dict:
    return (
        _common_properties(dataset)
        | _reachability_results(dataset)
        | _runtime_results(dataset)
        | _updates_results(dataset)
        | _event_counts_results(dataset)
    )


def _reachability_results(dataset: dict) -> dict:
    namespace = dataset["results"]["reachability"]
    return {
        "n_influenced": namespace["influence"],
        "n_influences": namespace["source"],
        "msg_reachability": namespace["message"],
    }


def _event_counts_results(dataset: dict) -> dict:
    namespace = dataset["results"]["user"]["counts"]
    return {
        "n_receives": namespace["ReceiveEvent"],
        "n_updates": namespace["UpdateEvent"],
        "n_contacts": namespace["ContactEvent"],
    }


def _updates_results(dataset: dict) -> dict:
    namespace = dataset["results"]["user"]["updates"]
    return {
        "exposure_diffs": namespace["difference"],
        "exposure_scores": namespace["exposure"],
    }


def _runtime_results(dataset: dict) -> dict:
    namespace = dataset["results"]["runtime"]
    return {
        "msg_runtime": namespace["MessagePassing"],
        "total_runtime": namespace["RiskPropagation"],
    }


def _common_properties(dataset: dict) -> dict:
    return {
        "id": dataset["id"],
        "key": dataset["key"],
        "network_id": dataset["network"]["id"],
        "network_type": dataset["networkFactory"]["type"],
        "nodes": dataset["network"]["nodes"],
        "edges": dataset["network"]["edges"],
        "ct_random_type": dataset["networkFactory"]["timeFactory"]["random"]["type"],
        "sv_random_type": dataset["scoreFactory"]["random"]["type"],
        "st_random_type": dataset["scoreFactory"]["timeFactory"]["random"]["type"],
        "transmission_rate": dataset["parameters"]["transmissionRate"],
        "send_coefficient": dataset["parameters"]["sendCoefficient"],
    }


def _load_raw_datasets(path: Path) -> Iterable[dict]:
    for dataset_path in _iterdir(path):
        yield from _load_raw_dataset(dataset_path)


def _iterdir(path: Path) -> Iterable[Path]:
    return (p for p in path.iterdir() if p.is_dir())


def _load_raw_dataset(path: Path) -> Iterable[dict]:
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


if __name__ == "__main__":
    save_experiment(path=Path(sys.argv[1]), kind=sys.argv[2])
