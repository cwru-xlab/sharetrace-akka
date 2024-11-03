import dataclasses
import math
from itertools import count
from typing import Callable, Literal

import numpy as np
import polars as pl
import polars.selectors as cs
import seaborn as sns
from matplotlib import pyplot as plt
from numpy.ma.core import append
from numpy.typing import NDArray

DF = pl.DataFrame
Expr = pl.Expr

lists_selector = cs.by_dtype(pl.List(int), pl.List(float))


class InvalidDatasetError(Exception):
    pass


@dataclasses.dataclass(slots=True, frozen=True)
class PercentileResults:
    aggregate: DF
    network_type: DF


@dataclasses.dataclass(slots=True, frozen=True)
class AccuracyResults:
    tabular: DF
    tabular_percentiles: PercentileResults
    counts: DF
    count_percentiles: PercentileResults


def load_dataset(name: str) -> DF:
    return pl.read_parquet(f"data/{name}/dataset.parquet")


def process_runtime_dataset(df: DF) -> DF:
    # Remove the "burn in" iteration for JVM class loading.
    return df.filter(pl.col("run_id") != "1")


def validate_parameter_dataset(df: DF) -> None:
    def find_invalid_rows(expected, actual) -> list[int]:
        return (
            df.select(values=pl.concat_list(expected, actual))
            .select(pl.arg_where(pl.col("values").list.n_unique() > 1))
            .to_series()
            .to_list()
        )

    invalid = find_invalid_rows(pl.col("n_nodes"), lists_selector.list.len())
    if (n_invalid := len(invalid)) > 0:
        raise InvalidDatasetError(
            f"{n_invalid} rows contain a list whose length is not equal to "
            f"'n_nodes': {invalid}"
        )

    invalid = find_invalid_rows(pl.col("n_edges"), pl.col("n_contacts").list.sum() / 2)
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


def compute_accuracy_results(
    df: DF,
    parameter: str,
    tabular_percentiles: list[float],
    count_percentiles: list[float],
    precision: int = 3,
) -> AccuracyResults:
    run_key = ("dataset_id", "network_id", "score_source")
    user_key = (*run_key, "user_id")

    def get_percentiles(
        __df: DF, col: str, percentiles: list[float], by_network_type: bool
    ) -> DF:
        axes = ("network_type", parameter) if by_network_type else (parameter,)
        return (
            __df.select(
                *axes,
                *(
                    percentile(col, p).round(precision).over(axes).alias(f"{p}")
                    for p in percentiles
                ),
            )
            .unique()
            .sort(axes)
        )

    tabular = (
        # Only consider users whose exposure score was updated at least once.
        # Users whose exposure score is never updated are not informative of accuracy.
        df.filter((pl.col("score_diff").abs().sum() > 0).over(user_key))
        # Get the accuracy across parameter values for each network user.
        .with_columns(
            (1 - pl.col("score_diff").max() + pl.col("score_diff"))
            .over(user_key)
            .alias("accuracy")
        )
    )

    counts = (
        tabular
        # Keep the row(s) associated with the highest accuracy...
        .filter(is_max("accuracy").over(user_key))
        # to determine the parameter value that offers the highest accuracy.
        .filter(is_max(parameter).over(user_key))
        # Group by the axes of interest to calculate their frequency.
        .group_by(*run_key, "network_type", parameter)
        .agg(count=pl.len())
        # The parameter column must be last to properly compute proportions.
        .sort(*run_key, "network_type", parameter)
        .with_columns(
            normalized("count", by="sum")
            # Frequency accumulates in descending order by parameter value.
            .cum_sum(reverse=True)
            .round(precision)
            .over(run_key)
            .alias("proportion")
        )
    )

    return AccuracyResults(
        tabular=tabular,
        tabular_percentiles=PercentileResults(
            aggregate=get_percentiles(
                tabular,
                col="accuracy",
                percentiles=tabular_percentiles,
                by_network_type=False,
            ),
            network_type=get_percentiles(
                tabular,
                col="accuracy",
                percentiles=tabular_percentiles,
                by_network_type=True,
            ),
        ),
        counts=counts,
        count_percentiles=PercentileResults(
            aggregate=get_percentiles(
                counts,
                col="proportion",
                percentiles=count_percentiles,
                by_network_type=False,
            ),
            network_type=get_percentiles(
                counts,
                col="proportion",
                percentiles=count_percentiles,
                by_network_type=True,
            ),
        ),
    )


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
        .group_by(axes + ["dataset_id", "network_id", "score_source"])
        .agg(pl.col(m).sum() for m in metrics)
    )
    if normalize:
        result = result.with_columns(
            normalized(m, by="max").over("network_id") for m in metrics
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


def percentile(name: str, value: float) -> Expr:
    return pl.col(name).quantile(value / 100, interpolation="midpoint")


def is_max(name: str) -> Expr:
    return pl.col(name).eq(pl.col(name).max())


def normalized(name: str, by: Literal["sum", "max"]) -> Expr:
    if by == "sum":
        den = pl.col(name).sum()
    else:
        den = pl.col(name).max()
    return pl.col(name) / den


def make_boxplot(df: DF, by_network_type: bool = False, **kwargs) -> sns.FacetGrid:
    if by_network_type:
        df = format_network_types(df)
        network_types = get_network_types(df)
        kwargs["col"] = "network_type"
        kwargs["col_order"] = network_types
        kwargs["col_wrap"] = math.floor(math.sqrt(len(network_types)))
    g = sns.catplot(df, kind="box", fill=None, color="black", linewidth=1, **kwargs)
    g.set_titles("{col_name}")
    return g


def set_legend_line_width(g: sns.FacetGrid, line_width: int) -> None:
    for line in g.legend.get_lines():
        line.set_linewidth(line_width)


def save_figure(name: str, **kwargs) -> None:
    plt.savefig(f"{name}.png", dpi=500, bbox_inches="tight", **kwargs)


def save_table(df: DF, name: str, **kwargs) -> None:
    df.write_csv(f"{name}.csv", **kwargs)


__all__ = [
    "AccuracyResults",
    "apply_hypothesis_test",
    "compute_accuracy_results",
    "compute_efficiency_results",
    "format_network_types",
    "get_network_types",
    "get_runtimes",
    "InvalidDatasetError",
    "lists_selector",
    "load_dataset",
    "normalized",
    "make_boxplot",
    "process_parameter_dataset",
    "process_runtime_dataset",
    "save_figure",
    "save_table",
    "set_legend_line_width",
    "validate_parameter_dataset",
]
