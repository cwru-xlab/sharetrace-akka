from numbers import Real
from typing import Self, Sequence

import numpy as np
import polars as pl
import polars.selectors as cs
from numpy.typing import NDArray

DataFrame = pl.DataFrame | pl.LazyFrame


class InvalidDatasetError(Exception):
    pass


class Dataset(pl.DataFrame):

    def __init__(self, data: pl.DataFrame | None = None):
        super().__init__(data)

    @classmethod
    def load(cls, name: str) -> Self:
        return Dataset(pl.read_parquet(f"data/{name}/experiment.parquet"))


class ParameterDataset(Dataset):

    def __init__(self, parameter: str, data: pl.DataFrame | None = None):
        super().__init__(data)
        self._check_users()
        self._check_contacts()
        self.parameter = parameter

    @classmethod
    def load(cls, name: str, parameter: str | None = None) -> Self:
        data = (
            super()
            .load(name)
            .with_columns(pl.int_ranges("n_nodes").alias("user_id"))
            .explode(cs.by_dtype(pl.List(pl.Int64), pl.List(pl.Float64)))
        )
        return ParameterDataset(parameter or name, data)

    def _check_users(self) -> None:
        invalid = self.filter(
            pl.col("n_nodes").first().ne(pl.len()).over("key", "network_id")
        )
        if not invalid.is_empty():
            raise InvalidDatasetError("Number of nodes != number of users")

    def _check_contacts(self) -> None:
        invalid = self.filter(
            pl.col("n_edges")
            .first()
            .ne(pl.col("n_contacts").sum().truediv(2))
            .over("key", "network_id")
        )
        if not invalid.is_empty():
            raise InvalidDatasetError("Number of edges != number of contacts")

    def accuracy_results(
        self, percentiles: Sequence[Real] | None = None, by_network_type: bool = True
    ) -> Self:
        accuracy_key = [self.parameter]
        if by_network_type:
            accuracy_key.insert(0, "network_type")
        percentiles = percentiles or [0, 0.01, 0.1, *range(1, 6)]
        network_user_key = ["network_id", "user_id"]
        return (
            self.filter(pl.col("exposure_diff").ne(0))
            .filter(eq_max("exposure_diff").over(network_user_key + [self.parameter]))
            .with_columns(
                pl.col("exposure_diff")
                .sub(pl.col("exposure_diff").max())
                .over(network_user_key)
                .alias("exposure_diff2")
            )
            .with_columns(
                *[
                    percentile("exposure_diff2", p).over(accuracy_key)
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
            .filter(eq_max(self.parameter).over(network_user_key))
            .group_by(accuracy_key)
            .agg(
                pl.len().alias("count"),
                *[pl.col(f"exposure_diff2_p{p}").first() for p in percentiles],
            )
            .sort(accuracy_key)
            .with_columns(
                normalized("count", by="sum")
                .cum_sum(reverse=True)
                .over("network_type" if by_network_type else None)
                .alias("accuracy")
            )
        )

    def efficiency_results(
        self,
        min_parameter: Real = 0,
        percentiles: Sequence[Real] | None = None,
        by_network_type: bool = True,
    ) -> Self:
        metrics = ["n_receives", "n_updates"]
        percentiles = percentiles or [0, 10, 25, 50, 75, 90, 100]
        group_key = [self.parameter]
        if by_network_type:
            group_key.insert(0, "network_type")
        return (
            self.filter(pl.col(self.parameter).ge(min_parameter))
            .group_by("key", "network_id", "network_type", self.parameter)
            .agg([pl.col(m).sum() for m in metrics])
            .with_columns(
                [
                    normalized(m, by="max").over("network_id", "network_type")
                    for m in metrics
                ]
            )
            .filter(pl.col(self.parameter).gt(min_parameter))
            .group_by(group_key)
            .agg([percentile(m, p) for m in metrics for p in percentiles])
            .sort(group_key)
        )


def load_runtime_dataset(name: str) -> pl.LazyFrame:
    return load_dataset(name)


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


def percentile(col_name: str, value: Real) -> pl.Expr:
    return (
        pl.col(col_name)
        .quantile(value / 100, interpolation="midpoint")
        .alias(f"{col_name}_p{value}")
    )


def compute_reachability_results(df: DataFrame) -> pl.DataFrame:
    return df.lazy().collect()


def collect(df: DataFrame) -> pl.DataFrame:
    return df.collect() if isinstance(df, pl.LazyFrame) else df


def get_efficiency_box_plot(
    df: DataFrame, parameter: str, metric: str, group_by: str = None
):
    df = collect(df)
    box_kwargs = {"by": parameter, "groupby": group_by}
    abs_box = df.plot.box(y=metric, **box_kwargs)
    rel_box = df.plot.box(y=f"{metric}_percent", **box_kwargs)
    scatter = df.plot.scatter(y=metric, x=parameter, c="orange", size=2)
    scatter = scatter.opts(jitter=0.5)
    return (abs_box * rel_box * scatter).opts(multi_y=True, show_legend=False)


__all__ = [
    "ParameterDataset",
]
