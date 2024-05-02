#!/usr/bin/env python3

import collections
import itertools
import math
import string

randoms = ["standard-normal", "uniform"]


def network_configs():
    for i, (n, m) in enumerate(itertools.product(range(1, 11), repeat=2)):
        base = {
            "nodes": (n := 10_000 * n),
            # Gnm random
            "edges": (m := 1_000_000 * m),
            # Random regular
            "degree": (k := math.floor(m / n)),
            # Barabasi Albert
            "initial_nodes": (n0 := 10),
            "new_edges": math.floor((m - (n0 * (n0 - 1) / 2)) / (n - n0)),
            # Watts Strogatz
            "rewiring_probability": 0.2,
            "nearest_neighbors": k,
            "qualifier": i + 1
        }
        yield {
            **base,
            "network_type": "gnm-random",
        }
        yield {
            **base,
            "network_type": "barabasi-albert",
        }
        yield {
            **base,
            "network_type": "random-regular",
        }
        yield {
            **base,
            "network_type": "watts-strogatz",
        }


def contact_time_factory_configs():
    for ct_random in randoms:
        yield {"contact_time_random": ct_random}


def score_factory_configs():
    for sv_random, st_random in itertools.product(randoms, repeat=2):
        yield {
            "score_value_random": sv_random,
            "score_time_random": st_random
        }


def template_values():
    for configs in itertools.product(
            network_configs(),
            contact_time_factory_configs(),
            score_factory_configs()):
        yield collections.ChainMap(*configs)


def generate_configs():
    base_dir = "./app/src/main/resources"
    with open(f"{base_dir}/runtime.template") as f:
        template = string.Template(f.read())
    for values in template_values():
        network = values["network_type"]
        qualifier = values["qualifier"]
        sv_random = values["score_value_random"]
        st_random = values["score_time_random"]
        ct_random = values["contact_time_random"]
        filename = f"runtime_{network}_{qualifier}_{sv_random}_{st_random}_{ct_random}.conf"
        with open(f"{base_dir}/{filename}", "w") as f:
            f.write(template.substitute(values))


if __name__ == '__main__':
    generate_configs()
