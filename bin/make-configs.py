#!/usr/bin/env python3

import collections
import itertools
import math
import string
import sys

randoms = ["standard-normal", "uniform"]


def best_barabasi_albert_config(n, m):
    result = None
    min_frac = math.inf
    for n0 in range(1, n):
        m0 = (m - (n0 * (n0 - 1) / 2)) / (n - n0)
        if 0 < m0 <= n0 and (frac := m0 % 1) < min_frac:
            result = (n0, math.floor(m0))
            min_frac = frac
    return result


def network_configs(network_sizes):
    for i, (n, m) in enumerate(network_sizes, 1):
        n0, m0 = best_barabasi_albert_config(n, m)
        base = {
            "qualifier": i,
            "nodes": n,
            # Gnm random
            "edges": m,
            # Random regular
            "degree": (k := math.floor(2 * m / n)),
            # Barabasi Albert
            "initial_nodes": n0,
            "new_edges": m0,
            # Watts Strogatz
            "rewiring_probability": 0.2,
            "nearest_neighbors": k if k % 2 == 0 else k + 1,
        }
        yield base | {"network_type": "gnm-random"}
        yield base | {"network_type": "barabasi-albert"}
        yield base | {"network_type": "random-regular"}
        yield base | {"network_type": "watts-strogatz"}


def contact_time_factory_configs():
    for ct_random in randoms:
        yield {"contact_time_random": ct_random}


def score_factory_configs():
    for sv_random, st_random in itertools.product(randoms, repeat=2):
        yield {"score_value_random": sv_random, "score_time_random": st_random}


def send_coefficient_experiment_configs():
    return parameter_experiment_configs(
        send_coefficients=[c / 10 for c in range(8, 21)]
    )


def tolerance_experiment_configs():
    return parameter_experiment_configs(tolerances=[t / 1000 for t in range(1, 11)])


def parameter_experiment_configs(**parameter_config):
    yield from merge_configs(
        network_configs([(5_000, 50_000)]),
        contact_time_factory_configs(),
        score_factory_configs(),
        [parameter_config],
    )


def runtime_baseline_experiment_configs():
    yield from merge_configs(
        network_configs([(10_000, 100_000)]),
        contact_time_factory_configs(),
        score_factory_configs(),
    )


def runtime_experiment_configs():
    def runtime_network_sizes():
        for n, m in itertools.product(range(1, 11), repeat=2):
            yield n * 10_000, m * 1_000_000

    random_configs = [
        {
            "score_value_random": "uniform",
            "score_time_random": "uniform",
            "contact_time_random": "uniform",
        }
    ]

    yield from merge_configs(network_configs(runtime_network_sizes()), random_configs)


def merge_configs(*configs):
    for configs in itertools.product(*configs):
        yield collections.ChainMap(*configs)


def make_configs(experiment_type):
    base_filename = experiment_type
    if experiment_type == "send-coefficient":
        template_values = send_coefficient_experiment_configs()
    elif experiment_type == "tolerance":
        template_values = tolerance_experiment_configs()
    elif experiment_type == "runtime":
        template_values = runtime_experiment_configs()
    elif experiment_type == "runtime-baseline":
        template_values = runtime_baseline_experiment_configs()
        base_filename = "runtime"
    else:
        raise ValueError(f"Invalid experiment type: {experiment_type}")

    base_dir = "./app/src/main/resources"
    with open(f"{base_dir}/{base_filename}-experiment-config.template") as f:
        template = string.Template(f.read())

    for values in template_values:
        network = values["network_type"]
        qualifier = values["qualifier"]
        sv_random = values["score_value_random"]
        st_random = values["score_time_random"]
        ct_random = values["contact_time_random"]
        filename = f"{experiment_type}_{network}_{qualifier}_{sv_random}_{st_random}_{ct_random}.conf"
        with open(f"{base_dir}/{filename}", "w") as f:
            f.write(template.substitute(values))


if __name__ == "__main__":
    make_configs(sys.argv[1])
