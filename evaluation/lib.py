import dataclasses
import math
from typing import Callable, Literal

import numpy as np
import polars as pl
import polars.selectors as cs
import seaborn as sns
from matplotlib import pyplot as plt
from numpy.typing import NDArray
from sklearn import linear_model, model_selection, metrics

DF = pl.DataFrame
Expr = pl.Expr

lists_selector = cs.by_dtype(pl.List(int), pl.List(float))

immutable = dataclasses.dataclass(slots=True, frozen=True)


class InvalidDatasetError(Exception):
    pass


@immutable
class PercentileResults:
    aggregate: DF
    network_type: DF


@immutable
class AccuracyResults:
    tabular: DF
    tabular_percentiles: PercentileResults
    counts: DF
    count_percentiles: PercentileResults


@immutable
class RuntimeValidityResults:
    total_runtime: DF
    msg_runtime: DF


@immutable
class RuntimeModel:
    estimator: linear_model.QuantileRegressor
    params_and_scores: DF


def load_dataset(name: str) -> DF:
    return pl.read_parquet(f"data/{name}/dataset.parquet")


def process_runtime_dataset(df: DF, keep_burn_in: bool = False) -> DF:
    # Remove the "burn in" iteration for JVM class loading.
    if not keep_burn_in:
        df = df.filter(pl.col("run_id") != "1")
    return (
        df.with_columns(
            total_runtime_validity=pl.col("total_runtime") > 0,
            msg_runtime_validity=pl.col("msg_runtime") > 0,
        )
        # Scale runtimes to seconds.
        .with_columns(cs.ends_with("_runtime") / 1000)
    )


def compute_runtime_validity_results(df: DF) -> RuntimeValidityResults:
    def compute(runtime_type: str) -> DF:
        validity_col = f"{runtime_type}_validity"
        return (
            df.group_by("network_type", validity_col)
            .agg(count=pl.len())
            .with_columns(proportion=normalized("count", by="sum").over("network_type"))
            .filter(pl.col(validity_col))
            .select("network_type", "count", "proportion")
            .sort("network_type")
        )

    return RuntimeValidityResults(
        total_runtime=compute("total_runtime"), msg_runtime=compute("msg_runtime")
    )


# noinspection PyPep8Naming
def get_runtime_model_data(df: DF, flatten: bool = False) -> (NDArray, NDArray):
    X = df["n_edges"].to_numpy()
    if not flatten:
        X = X.reshape(-1, 1)
    y = df["msg_runtime"].to_numpy()
    return X, y


def fit_runtime_model(df: DF, seed: int) -> (dict, DF):
    # noinspection PyPep8Naming
    X, y = get_runtime_model_data(df)

    def make_cross_validation():
        # Ref: https://scikit-learn.org/stable/common_pitfalls.html#controlling-randomness
        return model_selection.KFold(n_splits=5, shuffle=True, random_state=seed)

    def make_scorer(metric: Callable, _quantile: float) -> Callable:
        # Based on the below references, 'alpha' appears to be intended for the quantile
        # value, rather than the 'alpha' regularization term that is specified for
        # QuantileRegressor.
        # Ref: https://scikit-learn.org/stable/modules/generated/sklearn.metrics.mean_pinball_loss.html
        # Ref: https://scikit-learn.org/stable/modules/generated/sklearn.metrics.d2_pinball_score.html
        # Ref: https://scikit-learn.org/stable/auto_examples/ensemble/plot_gradient_boosting_quantile.html
        return metrics.make_scorer(
            metric,
            alpha=_quantile,
            greater_is_better=metric.__name__.endswith("_score"),
        )

    def fit_quantile(_quantile: float) -> tuple:
        # Ref: https://scikit-learn.org/stable/auto_examples/model_selection/plot_nested_cross_validation_iris.html
        # Ref: https://machinelearningmastery.com/nested-cross-validation-for-machine-learning-with-python/
        cv_outer = make_cross_validation()
        cv_inner = make_cross_validation()

        # Ref: https://scikit-learn.org/stable/modules/linear_model.html#quantile-regression
        _estimator = linear_model.QuantileRegressor(
            quantile=_quantile, fit_intercept=True, solver="highs"
        )

        common_cv_kwargs = {
            # Ref: https://scikit-learn.org/stable/modules/model_evaluation.html
            "scoring": {
                "d2_pinball_score": make_scorer(metrics.d2_pinball_score, _quantile),
                "mean_pinball_loss": make_scorer(metrics.mean_pinball_loss, _quantile),
            },
            "n_jobs": -1,
            "error_score": "raise",
        }

        # Ref: https://scikit-learn.org/stable/modules/grid_search.html
        grid_search = model_selection.GridSearchCV(
            estimator=_estimator,
            param_grid={"alpha": [10**-p for p in range(1, 6)]},
            refit="mean_pinball_loss",
            cv=cv_inner,
            **common_cv_kwargs,
        )
        grid_search.fit(X, y)

        # Ref: https://scikit-learn.org/stable/modules/cross_validation.html
        _scores = model_selection.cross_validate(
            estimator=grid_search, X=X, y=y, cv=cv_outer, **common_cv_kwargs
        )
        return grid_search.best_estimator_, _scores

    estimators = {}
    results = []
    for quantile in (0.05, 0.5, 0.95):
        estimator, scores = fit_quantile(quantile)
        estimators[quantile] = estimator
        mean_pinball_loss = scores["test_mean_pinball_loss"]
        d2_pinball_score = scores["test_d2_pinball_score"]
        result = pl.DataFrame(
            {
                "quantile": quantile,
                "intercept": estimator.intercept_,
                "coefficient": estimator.coef_,
                "alpha": estimator.alpha,
                "pinball_loss_mean": np.mean(mean_pinball_loss),
                "pinball_loss_std_err": std_err(mean_pinball_loss),
                "d2_pinball_score_mean": np.mean(d2_pinball_score),
                "d2_pinball_score_std_err": std_err(d2_pinball_score),
            }
        )
        results.append(result)
    results = pl.concat(results)

    return estimators, results


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


def std_err(arr: NDArray) -> NDArray:
    return arr.std() / np.sqrt(arr.size)


def process_parameter_dataset(df: DF, validate: bool = True) -> DF:
    if validate:
        validate_parameter_dataset(df)
    return df.with_columns(user_id=pl.int_ranges("n_nodes")).explode(lists_selector)


def format_network_types(df: DF) -> DF:
    return df.with_columns(
        pl.col("network_type").replace(
            {
                "BarabasiAlbert": "Barabasi-Albert",
                "GnmRandom": "Erdős-Rényi",
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
    def get_percentiles(
        __df: DF, col: str, percentiles: list[float], by_network_type: bool
    ) -> DF:
        axes = ("network_type", parameter) if by_network_type else (parameter,)
        return (
            __df.select(
                *axes,
                *(
                    percentile(col, p).round(precision).over(axes).alias(f"$P_{p}$")
                    for p in percentiles
                ),
            )
            .unique()
            .sort(axes)
        )

    run_key = ("dataset_id", "network_id", "score_source")
    user_key = (*run_key, "user_id")

    tabular = (
        # Only consider users whose exposure score was updated at least once.
        # Users whose exposure score is never updated are not informative of accuracy.
        df.filter((pl.col("score_diff").abs().sum() > 0).over(user_key))
        # Get the accuracy across parameter values for each network user.
        .with_columns(
            (1 - pl.max("score_diff") + pl.col("score_diff"))
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


def binned(df: DF, col: str, scale: float, rescale: bool | int | float = True) -> DF:
    if isinstance(rescale, (int, float)) and rescale != 0:
        new_scale = rescale
    elif rescale:
        new_scale = scale
    else:
        new_scale = 1
    return df.with_columns(
        ((pl.col(col) / scale).round() * new_scale)
        .cast(df[col].dtype)
        .alias(f"binned_{col}")
    )


def get_runtimes(
    df: pl.DataFrame,
    network_type: str = None,
    by_distributions: bool = False,
) -> NDArray:
    if network_type is not None:
        df = df.filter(network_type=network_type)
    if by_distributions:
        df = df.group_by(cs.ends_with("_dist")).agg("*")
    return np.array(df["msg_runtime"].to_list())


def percentile(name: str, value: float) -> Expr:
    return pl.col(name).quantile(value / 100, interpolation="midpoint")


# noinspection PyTypeChecker
def is_max(name: str) -> Expr:
    return pl.col(name) == pl.max(name)


def normalized(name: str, by: Literal["sum", "max"]) -> Expr:
    return pl.col(name) / (pl.sum(name) if by == "sum" else pl.max(name))


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
    "binned",
    "compute_accuracy_results",
    "compute_runtime_validity_results",
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
    "fit_runtime_model",
    "get_runtime_model_data",
]
