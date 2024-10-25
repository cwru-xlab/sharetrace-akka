import dataclasses
from typing import Any, Callable

import numpy as np
import polars as pl
import polars.selectors as cs
from numpy.typing import NDArray

DF = pl.DataFrame
Expr = pl.Expr


class InvalidDatasetError(Exception):
    pass


@dataclasses.dataclass
class AccuracyResults:
    tabular: DF
    aggregated: DF


def load_dataset(name: str = None, path: str = None) -> DF:
    return pl.read_parquet(path or f"data/{name}/dataset.parquet")


def process_runtime_dataset(df: DF) -> DF:
    # Remove the "burn in" iteration for JVM class loading.
    return df.filter(pl.col("run_id") != "1")


def process_parameter_dataset(df: DF) -> DF:
    invalid = df.filter(pl.col("n_nodes") != pl.col("n_contacts").list.len())
    if len(invalid) > 0:
        raise InvalidDatasetError("Number of nodes != number of users")
    invalid = df.filter(pl.col("n_edges") != pl.col("n_contacts").list.sum() / 2)
    if len(invalid) > 0:
        raise InvalidDatasetError("Number of edges != number of contacts")
    df = df.with_columns(user_id=pl.int_ranges("n_nodes"))
    df = df.explode(cs.by_dtype(pl.List(pl.Int64), pl.List(pl.Float64)))
    return df


def compute_accuracy_results(
    df: DF,
    parameter: str,
    percentiles: list[float],
    precision: int = 3,
    by_network_type: bool = False,
) -> AccuracyResults:
    # Include the data sources to account for multiple iterations with the same
    # parameters, but different contact networks and risk scores.
    user_key = ["dataset_id", "network_source", "score_source", "user_id"]
    tabular = (
        df
        # Only consider users whose exposure score was updated at least once.
        # Users whose exposure score is never updated are not informative of accuracy.
        .filter((pl.col("score_diff").abs().sum() > 0).over(user_key))
        # Get the accuracy across parameter values for each network user.
        .with_columns(
            accuracy=(1 - maximum("score_diff") + pl.col("score_diff")).over(user_key)
        )
    )
    axes = [parameter]
    if by_network_type:
        axes.insert(0, "network_type")
    aggregated = (
        tabular
        # Per the previous step, accuracy may vary across network users, for a given
        # parameter value. This step captures that variation by quantifying how the
        # accuracy associated with a given parameter value shifts as it changes.
        .with_columns(
            percentile("accuracy", p).round(precision).over(axes).alias(f"{p}")
            for p in percentiles
        )
        # For each user, rank in ascending order the accuracy across parameter values...
        .with_columns(rank=pl.col("accuracy").rank("dense").over(user_key))
        # and keep the row(s) associated with the highest accuracy...
        .filter(equals_max("rank").over(user_key))
        # in order to determine the parameter value that offers the highest accuracy.
        .filter(equals_max(parameter).over(user_key))
        .group_by(axes)
        .agg(freq=pl.len(), *[first(f"{p}") for p in percentiles])
        .sort(axes)
        # Frequency accumulates in descending order by parameter value, so use cumulative sum.
        .with_columns(
            norm_freq=normalized("freq")
            .cum_sum(reverse=True)
            .round(precision)
            .over(set(axes) - {parameter} or None)
        )
    )
    return AccuracyResults(tabular=tabular, aggregated=aggregated)


def compute_efficiency_results(
    df: DF,
    parameter: str,
    min_parameter_value: float = 0.0,
    normalize: bool = False,
    aggregate: bool = False,
    by_network_type: bool = False,
) -> DF:
    axes = [parameter]
    metrics = ["n_receives", "n_influenced", "n_influences", "msg_reachability"]
    if by_network_type:
        axes.insert(0, "network_type")
    result = (
        df.filter(pl.col(parameter) >= min_parameter_value)
        .group_by(axes + ["dataset_id", "network_source"])
        .agg(summation(m) for m in metrics)
    )
    if normalize:
        result = result.with_columns(
            normalized(m, by_max=True).over("network_source") for m in metrics
        )
    result = result.filter(pl.col(parameter) > min_parameter_value)
    if aggregate:
        percentiles = [0, 10, 25, 50, 75, 90, 100]
        result = result.group_by(axes).agg(
            percentile(m, p).alias(f"{m}_p{p}") for m in metrics for p in percentiles
        )
    return result.sort(axes)


def get_network_types(df: DF) -> list[str]:
    return df["network_type"].unique().sort().to_list()


def apply_hypothesis_test(
    df: DF,
    test: Callable,
    by_network_type: bool = False,
    by_distributions: bool = False,
) -> Any:
    # noinspection PyBroadException
    def apply_test(network_type=None):
        runtimes = get_runtimes(
            df,
            network_type=network_type,
            by_distributions=by_distributions,
        )
        try:
            return test(*runtimes)
        except:
            return test(runtimes)

    if by_network_type:
        return {nt: apply_test(nt) for nt in get_network_types(df)}
    else:
        return apply_test()


def get_runtimes(
    df: pl.DataFrame,
    network_type: str = None,
    by_distributions: bool = False,
) -> NDArray:
    if network_type is not None:
        df = df.filter(network_type=network_type)
    if by_distributions:
        df = df.group_by(
            "contact_time_dist", "score_value_dist", "score_time_dist"
        ).agg("*")
    return df["msg_runtime"].to_numpy()


def get_efficiency_box_plot(
    df: pl.DataFrame, parameter: str, metric: str, group_by: str = None
):
    box_kwargs = {"by": parameter, "groupby": group_by}
    abs_box = df.plot.box(y=metric, **box_kwargs)
    rel_box = df.plot.box(y=f"{metric}_percent", **box_kwargs)
    scatter = df.plot.scatter(y=metric, x=parameter, c="orange", size=2)
    scatter = scatter.opts(jitter=0.5)
    return (abs_box * rel_box * scatter).opts(multi_y=True, show_legend=False)


def percentile(name: str, value: float, interpolation: str = "midpoint") -> Expr:
    return pl.col(name).quantile(value / 100, interpolation=interpolation)


def first(name: str) -> Expr:
    return pl.col(name).first()


def equals_max(name: str) -> Expr:
    return pl.col(name).eq(maximum(name))


def normalized(name: str, by_max: bool = False) -> Expr:
    return pl.col(name) / (maximum(name) if by_max else summation(name))


def maximum(name: str) -> Expr:
    return pl.col(name).max()


def summation(name: str) -> Expr:
    return pl.col(name).sum()


__all__ = [
    "apply_hypothesis_test",
    "compute_accuracy_results",
    "compute_efficiency_results",
    "get_network_types",
    "get_runtimes",
    "InvalidDatasetError",
    "AccuracyResults",
    "load_dataset",
    "process_parameter_dataset",
    "process_runtime_dataset",
]
