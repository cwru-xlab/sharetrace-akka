import dataclasses
from typing import Callable

import numpy as np
import polars as pl
import polars.selectors as cs
import seaborn as sns
from numpy.typing import NDArray

DF = pl.DataFrame
Expr = pl.Expr

user_key = ("dataset_id", "network_source", "score_source", "user_id")

lists_selector = cs.by_dtype(pl.List(int), pl.List(float))


class InvalidDatasetError(Exception):
    pass


@dataclasses.dataclass
class AccuracyResults:
    tabular: DF
    aggregated: DF


def load_dataset(name: str) -> DF:
    return pl.read_parquet(f"data/{name}/dataset.parquet")


def process_runtime_dataset(df: DF) -> DF:
    # Remove the "burn in" iteration for JVM class loading.
    return df.filter(pl.col("run_id") != "1")


def validate_parameter_dataset(df: DF) -> None:
    def find_invalid_rows(expected, actual) -> list[int]:
        return (
            df.select(expected, actual)
            .select(pl.arg_where(pl.all_horizontal("*") == False))
            .to_series()
            .to_list()
        )

    invalid = find_invalid_rows("n_nodes", lists_selector.list.len())
    if (n_invalid := len(invalid)) > 0:
        raise InvalidDatasetError(
            f"{n_invalid} rows contain a list whose length is not equal to "
            f"'n_nodes': {invalid}"
        )

    invalid = find_invalid_rows("n_edges", pl.col("n_contacts").list.sum() / 2)
    if (n_invalid := len(invalid)) > 0:
        raise InvalidDatasetError(
            f"{n_invalid} rows contain a 'n_contacts' list whose half sum is "
            f"not equal to 'n_edges': {invalid}"
        )


def process_parameter_dataset(df: DF, validate: bool = True) -> DF:
    if validate:
        validate_parameter_dataset(df)
    return df.with_columns(user_id=pl.int_ranges("n_nodes")).explode(lists_selector)


def format_network_types(df: DF) -> DF:
    return df.with_columns(
        pl.col("network_type").replace(
            {
                "BarabasiAlbert": "Barabasi-Albert",
                "GnmRandom": "Erdös–Rényi",
                "RandomRegular": "Random regular",
                "WattsStrogatz": "Watts-Strogatz",
            }
        ),
    )


def remove_non_updates(df: DF) -> DF:
    return df.filter((pl.col("score_diff").abs().sum() > 0).over(user_key))


def compute_accuracy_results(
    df: DF,
    parameter: str,
    percentiles: list[float],
    precision: int = 3,
    by_network_type: bool = False,
) -> AccuracyResults:
    # Include the data sources to account for multiple iterations with the same
    # parameters, but different contact networks and risk scores.
    tabular = (
        # Only consider users whose exposure score was updated at least once.
        # Users whose exposure score is never updated are not informative of
        # accuracy.
        remove_non_updates(df)
        # Get the accuracy across parameter values for each network user.
        .with_columns(
            (1 - pl.col("score_diff").max() + pl.col("score_diff"))
            .over(user_key)
            .alias("accuracy")
        )
    )
    axes = [parameter]
    if by_network_type:
        # Prefer to sort first by the network type, then by the parameter value.
        axes.insert(0, "network_type")
    aggregated = (
        tabular
        # For a given parameter value, accuracy may vary across network users.
        # Quantify this variation by computing various percentiles.
        .with_columns(
            percentile("accuracy", p).round(precision).over(axes).alias(f"{p}")
            for p in percentiles
        )
        # Rank in ascending order the accuracy across parameter values...
        .with_columns(rank=pl.col("accuracy").rank("dense").over(user_key))
        # and keep the row(s) associated with the highest accuracy...
        .filter(is_max("rank").over(user_key))
        # to determine the parameter value that offers the highest accuracy.
        .filter(is_max(parameter).over(user_key))
        # Finally, group by the axes of interest to calculate their frequency.
        # Keep the previously calculated percentiles as well.
        .group_by(axes)
        .agg(freq=pl.len(), *[pl.col(f"{p}").first() for p in percentiles])
        .sort(axes)
        .with_columns(
            normalized("freq")
            # Frequency accumulates in descending order by parameter value.
            .cum_sum(reverse=True)
            .round(precision)
            .over(set(axes) - {parameter} or None)
            .alias("norm_freq")
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
        .group_by(axes + ["dataset_id", "network_source", "score_source"])
        .agg(pl.col(m).sum() for m in metrics)
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
):
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
    return np.array(df["msg_runtime"].to_list())


def get_efficiency_box_plot(
    df: pl.DataFrame, parameter: str, metric: str, group_by: str = None
):
    box_kwargs = {"by": parameter, "groupby": group_by}
    abs_box = df.plot.box(y=metric, **box_kwargs)
    rel_box = df.plot.box(y=f"{metric}_percent", **box_kwargs)
    scatter = df.plot.scatter(y=metric, x=parameter, c="orange", size=2)
    scatter = scatter.opts(jitter=0.5)
    return (abs_box * rel_box * scatter).opts(multi_y=True, show_legend=False)


def percentile(name: str, value: float) -> Expr:
    return pl.col(name).quantile(value / 100, interpolation="midpoint")


def is_max(name: str) -> Expr:
    return pl.col(name).eq(pl.col(name).max())


def normalized(name: str, by_max: bool = False) -> Expr:
    return pl.col(name) / (pl.col(name).max() if by_max else pl.col(name).sum())


def save_fig(g: sns.FacetGrid, name: str, **kwargs) -> None:
    g.savefig(f"{name}.png", dpi=500, **kwargs)


__all__ = [
    "AccuracyResults",
    "apply_hypothesis_test",
    "compute_accuracy_results",
    "compute_efficiency_results",
    "format_network_types",
    "get_network_types",
    "get_runtimes",
    "InvalidDatasetError",
    "load_dataset",
    "process_parameter_dataset",
    "process_runtime_dataset",
    "remove_non_updates",
    "save_fig",
    "validate_parameter_dataset"
]
