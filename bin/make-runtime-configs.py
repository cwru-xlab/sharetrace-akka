#!/usr/bin/env python3

import collections
import itertools
import string

randoms = ["standard-normal", "uniform"]


def network_configs():
    for nodes in range(100_000, 1_000_001, 100_000):
        defaults = {
            "nodes": nodes,
            "degree": -1,
            "edges": -1,
            "initial_nodes": -1,
            "nearest_neighbors": -1,
            "network_type": "missing",
            "new_edges": -1,
            "rewiring_probability": -1,
        }
        yield {
            **defaults,
            "edges": int(0.0025 * nodes * (nodes - 1) / 2),
            "network_type": "gnm-random"
        }
        yield {
            **defaults,
            "initial_nodes": 10,
            "new_edges": 5,
            "network_type": "barabasi-albert"
        }
        yield {
            **defaults,
            "degree": 20,
            "network_type": "random-regular"
        }
        yield {
            **defaults,
            "nearest_neighbors": 20,
            "rewiring_probability": 0.2,
            "network_type": "watts-strogatz"
        }
        yield {
            **defaults,
            "network_type": "scale-free"
        }


def contact_time_factory_configs():
    for ct_random in randoms:
        yield {"contact_time_random": ct_random}


def score_factory_configs():
    for sv_random, st_random in itertools.product(randoms, repeat=2):
        yield {
            "score_value_random": sv_random,
            "score_time_random": st_random,
        }


def template_values():
    for configs in itertools.product(
            network_configs(),
            contact_time_factory_configs(),
            score_factory_configs()):
        yield collections.ChainMap(*configs)


def generate_configs():
    with open("./app/src/main/resources/runtime.template") as f:
        template = string.Template(f.read())
    for values in template_values():
        nodes = values["nodes"]
        factory = values["network_type"]
        sv_random = values["score_value_random"]
        st_random = values["score_time_random"]
        ct_random = values["contact_time_random"]
        filename = "_".join(("runtime", factory, f"{nodes}", sv_random, st_random, ct_random))
        with open(f"./app/src/main/resources/{filename}.conf", "w") as f:
            f.write(template.substitute(values))


if __name__ == '__main__':
    generate_configs()
