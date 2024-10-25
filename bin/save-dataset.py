#!/usr/bin/env python3
import functools
import itertools
import json
import sys
from pathlib import Path
from typing import Iterable, Callable, Mapping

import polars as pl


def dig(data: dict, *keys):
    if len(keys) == 0:
        return data
    elif len(keys) == 1:
        return data[keys[0]]
    else:
        return dig(data[keys[0]], *keys[1:])


def provider(*keys) -> Callable:
    def decorator(wrapped: Callable[[dict], dict]):
        @functools.wraps(wrapped)
        def provide(data: dict) -> dict:
            try:
                data = dig(data, *keys)
            except KeyError:
                return {}
            return wrapped(data)

        return provide

    return decorator


@provider("context")
def context(data: dict) -> dict:
    return {"seed": data["seed"]}


@provider("parameters")
def parameters(data: dict) -> dict:
    return {
        "transmission_rate": data["transmissionRate"],
        "send_coefficient": data["sendCoefficient"],
        "tolerance": data["tolerance"],
        "time_buffer": data["timeBuffer"],
        "score_expiry": data["scoreExpiry"],
        "contact_expiry": data["contactExpiry"],
        "flush_timeout": data["flushTimeout"],
        "idle_timeout": data["idleTimeout"],
    }


def dist_type(data: Mapping) -> str:
    return data["random"]["type"]


@provider()
def network_properties(data: dict) -> dict:
    network = data["network"]
    factory = data["networkFactory"]
    return {
        "network_source": network["id"],
        "network_type": factory["type"],
        "n_nodes": network["nodes"],
        "n_edges": network["edges"],
        "contact_time_dist": dist_type(factory["timeFactory"]),
    }


@provider("scoreFactory")
def score_properties(data: dict) -> dict:
    return {
        "score_source": data["id"],
        "score_value_dist": dist_type(data),
        "score_time_dist": dist_type(data["timeFactory"]),
    }


@provider("reachability")
def reachability_results(data: dict) -> dict:
    return {
        "n_influenced": data["influence"],
        "n_influences": data["source"],
        "msg_reachability": data["message"],
    }


@provider("user", "counts")
def event_count_results(data: dict) -> dict:
    return {
        "n_receives": data["ReceiveEvent"],
        "n_updates": data["UpdateEvent"],
        "n_contacts": data["ContactEvent"],
    }


@provider("user", "updates")
def score_results(data: dict) -> dict:
    return {
        "score_diff": data["difference"],
        "exposure_score": data["exposure"],
    }


@provider("runtime")
def runtime_results(data: dict) -> dict:
    return {
        "msg_runtime": data["MessagePassing"],
        "total_runtime": data["RiskPropagation"],
    }


def map_record(record: dict, results: dict, directory: Path) -> dict:
    base = {
        "dataset_id": directory.name,
        "run_id": (run_id := record["k"]),
    }
    properties = record["p"]
    results = results[run_id]
    return (
        base
        | context(properties)
        | parameters(properties)
        | network_properties(properties)
        | score_properties(properties)
        | reachability_results(results)
        | runtime_results(results)
        | score_results(results)
        | event_count_results(results)
    )


def load_records(directory: Path) -> Iterable[dict]:
    with (directory / "results.json").open() as f:
        results = json.load(f)
    with (directory / "properties.log").open() as f:
        for record in map(json.loads, f):
            yield map_record(record, results, directory)


def load_all_records(directory: Path) -> Iterable[dict]:
    return itertools.chain.from_iterable(
        load_records(p) for p in directory.iterdir() if p.is_dir()
    )


def save_dataset(directory: Path) -> None:
    records = load_all_records(directory)
    df = pl.DataFrame(records)
    df.write_parquet(directory / "dataset.parquet")


if __name__ == "__main__":
    save_dataset(Path(sys.argv[1]))
