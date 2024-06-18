import functools
from numbers import Real
from typing import Self, Sequence

import numpy as np
import polars as pl
import polars.selectors as cs
from numpy.typing import NDArray


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
        self, percentiles: Sequence[Real], by_network_type: bool = False
    ) -> Self:
        accuracy_key = [self.parameter]
        if by_network_type:
            # Prefer to sort first by parameter value and then by network type.
            accuracy_key.insert(0, "network_type")
        network_user_key = ["network_id", "user_id"]
        return (
            # Only consider users whose exposure score was updated.
            self.filter(pl.col("exposure_diff").ne(0))
            # Use the maximum change in exposure score as the baseline for accuracy.
            .filter(eq_max("exposure_diff").over(network_user_key + [self.parameter]))
            # Get the accuracy across parameter values for each user.
            .with_columns(
                (1 - pl.col("exposure_diff").max().sub(pl.col("exposure_diff")))
                .over(network_user_key)
                .alias("accuracy")
            )
            # Compute accuracy per parameter value (and network type).
            .with_columns(
                [percentile("accuracy", p).over(accuracy_key) for p in percentiles]
            )
            # For each user, rank in ascending order the exposure score updates across parameter values...
            .with_columns(
                pl.col("exposure_diff")
                .rank("dense")
                .over(network_user_key)
                .alias("exposure_diff_rank")
            )
            # and keep the row(s) associated with the most accurate update...
            .filter(eq_max("exposure_diff_rank").over(network_user_key))
            # in order to determine the parameter value that offers the most accuracy and efficiency.
            .filter(eq_max(self.parameter).over(network_user_key))
            # Group by the parameter value (and network type) to get the frequency normalization constants.
            .group_by(accuracy_key)
            .agg(
                pl.len().alias("frequency"),
                *[pl.col(f"accuracy_p{p}").first() for p in percentiles],
            )
            .sort(accuracy_key)
            # Frequency accumulates in descending order by parameter value, so use cumulative sum.
            .with_columns(
                normalized("frequency", by="sum")
                .cum_sum(reverse=True)
                .over("network_type" if by_network_type else None)
                .alias("normalized_frequency")
            )
        )

    def efficiency_results(
        self,
        percentiles: Sequence[Real] | None = None,
        by_network_type: bool = False,
        min_parameter: Real = 0,
        aggregate: bool = False
    ) -> Self:
        percentiles = percentiles or [0, 10, 25, 50, 75, 90, 100]
        group_key = [self.parameter]
        if by_network_type:
            # Prefer to sort first by parameter value and then by network type.
            group_key.insert(0, "network_type")
        result = (
            self.filter(pl.col(self.parameter).ge(min_parameter))
            .group_by("key", "network_id", *group_key)
            .agg(pl.col("n_receives").sum())
            .with_columns(normalized("n_receives", by="max").over("network_id"))
            .filter(pl.col(self.parameter).gt(min_parameter))
        )
        if aggregate:
            return (
                result
                .group_by(group_key)
                .agg([percentile("n_receives", p) for p in percentiles])
                .sort(group_key)
            )
        return result.sort(group_key)



class RuntimeDataset(Dataset):
    def __init__(self, data: pl.DataFrame | None = None):
        super().__init__(data)

    @classmethod
    def load(cls, name: str) -> Self:
        return RuntimeDataset(super().load(name))

    # def runtime_results(self) -> Self:
    #     (
    #         self
    #         # Remove the "burn in" iteration for JVM class loading.
    #         .filter(pl.col("key").ne("1"))
    #     )
    #     np.array(
    #         self
    #         # Remove the "burn in" iteration for JVM class loading.
    #         .filter(pl.col("key").ne("1"))
    #         .filter(pl.col("network_type").eq(network_type))
    #         .group_by("ct_random_type", "sv_random_type", "st_random_type")
    #         .agg("msg_runtime")
    #         .select("msg_runtime")
    #         .collect()
    #         .to_series()
    #         .to_list()
    #     )


def get_network_types(df: pl.DataFrame) -> list[str]:
    return df.select("network_type").unique().to_series().sort().to_list()


def compute_runtime_results(df: pl.DataFrame, network_type: str) -> NDArray:
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


def get_efficiency_box_plot(
    df: pl.DataFrame, parameter: str, metric: str, group_by: str = None
):
    box_kwargs = {"by": parameter, "groupby": group_by}
    abs_box = df.plot.box(y=metric, **box_kwargs)
    rel_box = df.plot.box(y=f"{metric}_percent", **box_kwargs)
    scatter = df.plot.scatter(y=metric, x=parameter, c="orange", size=2)
    scatter = scatter.opts(jitter=0.5)
    return (abs_box * rel_box * scatter).opts(multi_y=True, show_legend=False)


__all__ = ["ParameterDataset", "RuntimeDataset"]
