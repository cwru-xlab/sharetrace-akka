#!/usr/bin/env python3

import collections
import itertools
import string

randoms = ["standard-normal", "uniform"]


def network_configs():
    for p in frange(25, 76, 5):
        base = {
            "nodes": (nodes := 10_000),
            # Random regular
            "degree": round(0.1 * p * nodes),
            # Gnm random
            "edges": round(p * nodes * (nodes - 1) / 2),
            # Barabasi Albert
            "initial_nodes": (initial_nodes := round(nodes * 0.1)),
            "new_edges": round(p * initial_nodes),
            # Watts Strogatz
            "rewiring_probability": 0.2,
            "nearest_neighbors": round(0.1 * p * nodes),
        }
        yield {
            **base,
            "network_type": "gnm-random",
            "qualifier": base["edges"]
        }
        yield {
            **base,
            "network_type": "barabasi-albert",
            "qualifier": base["new_edges"]
        }
        yield {
            **base,
            "network_type": "random-regular",
            "qualifier": base["degree"]
        }
        yield {
            **base,
            "network_type": "watts-strogatz",
            "qualifier": base["nearest_neighbors"]
        }


def frange(start, stop, step=1, scale=0.01):
    return map(lambda n: n * scale, range(start, stop, step))


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
