import numpy as np
import polars as pl
import polars.selectors as cs

from numpy.typing import NDArray

DataFrame = pl.DataFrame | pl.LazyFrame


def load_runtime_dataset(name: str) -> pl.LazyFrame:
    return load_dataset(name)


def load_parameter_dataset(name: str) -> pl.LazyFrame:
    df = (
        load_dataset(name)
        .with_columns(user_id=pl.int_ranges("nodes"))
        .explode(cs.by_dtype(pl.List(pl.Int64), pl.List(pl.Float64)))
    )
    return check_parameter_dataset(df)


def check_parameter_dataset(df: DataFrame) -> DataFrame:
    invalid = (
        df.lazy()
        # Check that the number of network nodes matches the number of network users
        .filter(pl.col("nodes").first().ne(pl.len()).over("key", "network_id"))
        # Check that the number of network edges matches the number of contacts.
        .filter(
            pl.col("edges")
            .first()
            .ne(pl.col("n_contacts").sum().truediv(2))
            .over("key", "network_id")
        ).collect()
    )
    if len(invalid):
        raise ValueError("Invalid dataset")
    return df


def load_dataset(name: str) -> pl.LazyFrame:
    return pl.scan_parquet(f"./data/{name}/experiment.parquet")


def get_network_types(df: DataFrame) -> list[str]:
    return (
        df.lazy().select("network_type").unique().collect().to_series().sort().to_list()
    )


def compute_runtime_results(df: DataFrame, network_type: str) -> NDArray:
    return np.array(
        df.lazy()
        # Remove the "burn in" iteration for JVM class loading.
        .filter(pl.col("key").ne("1"))
        .filter(pl.col("network_type").eq(network_type))
        .group_by("ct_random_type", "sv_random_type", "st_random_type")
        .agg("msg_runtime")
        .select("msg_runtime")
        .collect()
        .to_series()
        .to_list()
    )


def compute_accuracy_results(df: DataFrame) -> pl.LazyFrame:
    return (
        df.lazy()
        # Use the max exposure diff of each network user to compute accuracy.
        .with_columns(
            pl.col("exposure_diffs")
            .max()
            .over("network_id", "user_id", "network_type", "send_coefficient")
            .alias("max_exposure_diff")
        )
        # Only consider network users that updated their exposure score.
        .filter(pl.col("max_exposure_diff").ne(0))
        # Compute the exposure diff rank for each network user over all send coefficients.
        .with_columns(
            pl.col("max_exposure_diff")
            .rank("dense")
            .over("network_id", "user_id")
            .alias("max_exposure_diff_rank")
        )
        # Retain send coefficients associated with the max exposure diff of each network user.
        .filter(
            pl.col("max_exposure_diff_rank")
            .eq(pl.col("max_exposure_diff_rank").max())
            .over("network_id", "user_id")
        )
        # Retain the max send coefficient for each network user.
        .filter(
            pl.col("send_coefficient")
            .eq(pl.col("send_coefficient").max())
            .over("network_id", "user_id")
        )
        # Compute count statistics of send coefficients for each network type.
        .group_by("network_type", "send_coefficient").agg(count=pl.len())
        # Sort for plotting and accuracy calculation.
        .sort("network_type", "send_coefficient")
        # Accuracy is defined as the normalized cumulative counts of send coefficients.
        .with_columns(
            pl.col("count")
            .truediv(pl.col("count").sum())
            .cum_sum(reverse=True)
            .over("network_type")
            .alias("accuracy")
        )
    )


"""
User metrics (p0, p25, p50, p75, p100).over('network_id', 'send_coefficient'):
- n_influenced
- n_influences
- msg_reachability

Network metrics:
- n_updates
- msg_runtime
"""


def compute_efficiency_results(
    df: DataFrame, min_send_coefficient: float
) -> pl.LazyFrame:
    return (
        df.lazy()
        .filter(pl.col("send_coefficient") >= min_send_coefficient)
        .with_columns(pl.col("n_influenced").median().over("key", "network_id"))
        .group_by("network_id", "network_type", "send_coefficient")
        .agg(pl.col("n_receives").sum())
        # .with_columns(
        #     n_receives=(pl.col.n_receives / pl.col.n_receives.max()).over("network_id"),
        #     **{n_influenced_p0: pl.col.n_influenced.quantile(0, "midpoint")},
        # )
        .filter(pl.col("send_coefficient") > min_send_coefficient)
        .sort("network_type", "send_coefficient")
    )


def get_efficiency_box_plot(
    df: pl.DataFrame, abs_metric: str, rel_metric: str, group_by: str = None
):
    box_kwargs = {"by": "send_coefficient", "groupby": group_by}
    abs_box = df.plot.box(y=abs_metric, **box_kwargs)
    rel_box = df.plot.box(y=rel_metric, **box_kwargs)
    scatter = df.plot.scatter(y=abs_metric, x="send_coefficient", c="orange", size=2)
    scatter = scatter.opts(jitter=0.5)
    return (abs_box * rel_box * scatter).opts(multi_y=True, show_legend=False)


__all__ = [
    "get_efficiency_box_plot",
    "compute_efficiency_results",
    "compute_accuracy_results",
    "compute_runtime_results",
    "load_runtime_dataset",
    "load_parameter_dataset",
]
