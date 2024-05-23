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
        .with_columns(pl.int_ranges("n_nodes").alias("user_id"))
        .explode(cs.by_dtype(pl.List(pl.Int64), pl.List(pl.Float64)))
    )
    return check_parameter_dataset(df)


def check_parameter_dataset(df: DataFrame) -> DataFrame:
    invalid = (
        df.lazy()
        .filter(pl.col("n_nodes").first().ne(pl.len()).over("key", "network_id"))
        .collect()
    )
    if len(invalid):
        raise ValueError("Invalid dataset: number of nodes != number of users")
    invalid = (
        df.lazy()
        .filter(
            pl.col("n_edges")
            .first()
            .ne(pl.col("n_contacts").sum().truediv(2))
            .over("key", "network_id")
        )
        .collect()
    )
    if len(invalid):
        raise ValueError("Invalid dataset: number of edges != number of contacts")
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


def eq_max(col_name: str) -> pl.Expr:
    return pl.col(col_name).eq(pl.col(col_name).max())


def normalized(col_name: str, by: str) -> pl.Expr:
    num = pl.col(col_name)
    den = num.max() if by == "max" else num.sum()
    return num.truediv(den)


def compute_accuracy_results(
    df: DataFrame, by_network_type: bool = True
) -> pl.DataFrame:
    accuracy_key = ["send_coefficient"]
    if by_network_type:
        accuracy_key.insert(0, "network_type")
    percentiles = [0, 0.01, 0.1, *range(1, 6)]
    network_user_key = ["network_id", "user_id"]
    parametrized_network_user_key = network_user_key + ["send_coefficient"]
    return (
        df.lazy()
        .filter(pl.col("exposure_diff").ne(0))
        .filter(eq_max("exposure_diff").over(parametrized_network_user_key))
        .with_columns(
            pl.col("exposure_diff")
            .sub(pl.col("exposure_diff").max())
            .over(network_user_key)
            .alias("exposure_diff2")
        )
        .with_columns(
            *[
                pl.col("exposure_diff2")
                .quantile(p / 100, interpolation="midpoint")
                .alias(f"exposure_diff2_p{p}")
                .over(accuracy_key)
                for p in percentiles
            ]
        )
        .with_columns(
            pl.col("exposure_diff")
            .rank("dense")
            .over(network_user_key)
            .alias("exposure_diff_rank")
        )
        .filter(eq_max("exposure_diff_rank").over(network_user_key))
        .filter(eq_max("send_coefficient").over(network_user_key))
        .group_by(accuracy_key)
        .agg(
            pl.len().alias("count"),
            *[pl.col(f"exposure_diff2_p{p}").first() for p in percentiles]
        )
        .sort(accuracy_key)
        # Accuracy is defined as the normalized cumulative counts of send coefficients.
        .with_columns(
            normalized("count", by="sum")
            .cum_sum(reverse=True)
            .over("network_type" if by_network_type else None)
            .alias("accuracy")
        )
        .collect()
    )


def compute_reachability_results(df: DataFrame) -> pl.DataFrame:
    return df.lazy().collect()


def compute_efficiency_results(
    df: DataFrame, min_send_coefficient: float
) -> pl.DataFrame:
    return (
        df.lazy()
        .filter(pl.col("send_coefficient").ge(min_send_coefficient))
        .group_by("network_id", "network_type", "send_coefficient")
        .agg(pl.col("n_receives").sum(), pl.col("n_updates").sum())
        .with_columns(
            normalized("n_receives", by="max")
            .over("network_id", "send_coefficient")
            .alias("n_receives_percent"),
            normalized("n_updates", by="max")
            .over("network_id", "send_coefficient")
            .alias("n_updates_percent"),
        )
        .filter(pl.col("send_coefficient").gt(min_send_coefficient))
        .sort("network_type", "send_coefficient")
        .collect()
    )


def collect(df: DataFrame) -> pl.DataFrame:
    return df.collect() if isinstance(df, pl.LazyFrame) else df


def get_efficiency_box_plot(df: DataFrame, metric: str, group_by: str = None):
    df = collect(df)
    box_kwargs = {"by": "send_coefficient", "groupby": group_by}
    abs_box = df.plot.box(y=metric, **box_kwargs)
    rel_box = df.plot.box(y=f"{metric}_percent", **box_kwargs)
    scatter = df.plot.scatter(y=metric, x="send_coefficient", c="orange", size=2)
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
