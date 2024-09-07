from numbers import Real
from typing import Self

import numpy as np
import polars as pl
import polars.selectors as cs
from numpy.typing import NDArray

DF = pl.DataFrame
Expr = pl.Expr

_network_instance_key = ["key", "network_id"]


class InvalidDatasetError(Exception):
    pass


def _first(name: str) -> Expr:
    return pl.col(name).first()


def _eq_max(name: str) -> Expr:
    return pl.col(name).eq(pl.col(name).max())


def _normalized(name: str, by_max: bool = False) -> Expr:
    num = pl.col(name)
    den = num.max() if by_max else num.sum()
    return num.truediv(den)


def _percentile(name: str, value: Real) -> Expr:
    return (
        pl.col(name)
        .quantile(value / 100, interpolation="midpoint")
        .alias(f"{name}_p{value}")
    )


def load_dataset(name: str = None, path: str = None) -> DF:
    return pl.read_parquet(path or f"data/{name}/experiment.parquet")


def process_runtime_dataset(df: DF, simple: bool = False) -> DF:
    # Remove the "burn in" iteration for JVM class loading.
    df = df.filter(pl.col("key").ne("1"))
    if simple:
        return df.select("network_type", "n_nodes", "n_edges", "msg_runtime")
    return df


def process_parameter_dataset(df: DF) -> DF:
    df = _add_user_id_column(df)
    df = _explode_numeric_columns(df)
    _check_parameter_dataset_users(df)
    _check_parameter_dataset_contacts(df)
    return df


def accuracy_results(
    df: DF, parameter: str, percentiles: list[float], by_network_type: bool = False
) -> DF:
    accuracy_key = [parameter]
    if by_network_type:
        # Prefer to sort first by parameter value and then by network type.
        accuracy_key.insert(0, "network_type")
    network_user_key = ["network_id", "user_id"]
    return (
        # Only consider users whose exposure score was updated.
        df.filter(pl.col("exposure_diff").ne(0))
        # Use the maximum change in exposure score as the baseline for accuracy.
        .filter(_eq_max("exposure_diff").over(network_user_key + [parameter]))
        # Get the accuracy across parameter values for each user.
        .with_columns(
            (1 - pl.col("exposure_diff").max().sub(pl.col("exposure_diff")))
            .over(network_user_key)
            .alias("accuracy")
        )
        # Compute accuracy per parameter value (and network type).
        .with_columns(
            [_percentile("accuracy", p).over(accuracy_key) for p in percentiles]
        )
        # For each user, rank in ascending order the exposure score updates across parameter values...
        .with_columns(
            pl.col("exposure_diff")
            .rank("dense")
            .over(network_user_key)
            .alias("exposure_diff_rank")
        )
        # and keep the row(s) associated with the most accurate update...
        .filter(_eq_max("exposure_diff_rank").over(network_user_key))
        # in order to determine the parameter value that offers the most accuracy and efficiency.
        .filter(_eq_max(parameter).over(network_user_key))
        # Group by the parameter value (and network type) to get the frequency normalization constants.
        .group_by(accuracy_key)
        .agg(
            pl.len().alias("frequency"),
            *[pl.col(f"accuracy_p{p}").first() for p in percentiles],
        )
        .sort(accuracy_key)
        # Frequency accumulates in descending order by parameter value, so use cumulative sum.
        .with_columns(
            _normalized("frequency", by_max=False)
            .cum_sum(reverse=True)
            .over("network_type" if by_network_type else None)
            .alias("normalized_frequency")
        )
    )


def compute_efficiency_results(
    df: DF,
    parameter: str,
    min_parameter_value: float = 0.0,
    normalize: bool = False,
    aggregate: bool = False,
    by_network_type: bool = False,
) -> DF:
    group_key = [parameter]
    if by_network_type:
        group_key.insert(0, "network_type")
    result = (
        df.filter(pl.col(parameter).ge(min_parameter_value))
        .group_by(_network_instance_key + group_key)
        .agg(pl.col("n_updates").sum())
    )
    if normalize:
        result = result.with_columns(
            _normalized("n_updates", by_max=True).over("network_id")
        )
    result = result.filter(pl.col(parameter).gt(min_parameter_value))
    if aggregate:
        percentiles = [0, 10, 25, 50, 75, 90, 100]
        result = result.group_by(group_key).agg(
            [_percentile("n_updates", p) for p in percentiles]
        )
    return result.sort(group_key)


def get_network_types(df: DF) -> list[str]:
    return df.select("network_type").unique().to_series().sort().to_list()


def _add_user_id_column(df: DF) -> DF:
    return df.with_columns(pl.int_ranges("n_nodes").alias("user_id"))


def _explode_numeric_columns(df: DF) -> DF:
    return df.explode(cs.by_dtype(pl.List(pl.Int64), pl.List(pl.Float64)))


def _check_parameter_dataset_users(df: DF) -> None:
    invalid = df.filter(_first("n_nodes").ne(pl.len()).over(_network_instance_key))
    if len(invalid) > 0:
        raise InvalidDatasetError("Number of nodes != number of users")


def _check_parameter_dataset_contacts(df: DF) -> None:
    invalid = df.filter(
        _first("n_edges")
        .ne(pl.col("n_contacts").sum().truediv(2))
        .over(_network_instance_key)
    )
    if len(invalid) > 0:
        raise InvalidDatasetError("Number of edges != number of contacts")


def get_message_passing_runtimes(df: pl.DataFrame, network_type: str = None) -> NDArray:
    if network_type is not None:
        df = df.filter(pl.col("network_type").eq(network_type))
    return df.select("msg_runtime").to_numpy().flatten()


def get_efficiency_box_plot(
    df: pl.DataFrame, parameter: str, metric: str, group_by: str = None
):
    box_kwargs = {"by": parameter, "groupby": group_by}
    abs_box = df.plot.box(y=metric, **box_kwargs)
    rel_box = df.plot.box(y=f"{metric}_percent", **box_kwargs)
    scatter = df.plot.scatter(y=metric, x=parameter, c="orange", size=2)
    scatter = scatter.opts(jitter=0.5)
    return (abs_box * rel_box * scatter).opts(multi_y=True, show_legend=False)
