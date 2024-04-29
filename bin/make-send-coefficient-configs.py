#!/usr/bin/env python3

import collections
import itertools
import string

randoms = [
    "standard-normal",
    "uniform"
]


def network_configs():
    defaults = {
        "degree": -1,
        "edges": -1,
        "initial_nodes": -1,
        "nearest_neighbors": -1,
        "network_type": "missing",
        "new_edges": -1,
        "nodes": (nodes := 5000),
        "rewiring_probability": -1,
    }
    return [
        {
            **defaults,
            "edges": int(0.005 * nodes * (nodes - 1) / 2),
            "network_type": "gnm-random"
        },
        {
            **defaults,
            "initial_nodes": 10,
            "new_edges": 5,
            "network_type": "barabasi-albert"
        },
        {
            **defaults,
            "degree": 20,
            "network_type": "random-regular"
        },
        {
            **defaults,
            "nearest_neighbors": 20,
            "rewiring_probability": 0.2,
            "network_type": "watts-strogatz"
        },
        {
            **defaults,
            "network_type": "scale-free"
        }
    ]


def contact_time_factory_configs():
    for random in randoms:
        yield {
            "contact_time_random": random
        }


def score_factory_configs():
    for sv_random, st_random in itertools.product(randoms, repeat=2):
        yield {
            "score_value_random": sv_random,
            "score_time_random": st_random,
        }


def parameter_configs():
    return [{
        "send_coefficients": [0.01 * c for c in range(80, 121, 10)]
    }]


def template_values():
    for configs in itertools.product(
            network_configs(),
            contact_time_factory_configs(),
            score_factory_configs(),
            parameter_configs()):
        yield collections.ChainMap(*configs)


def generate_configs():
    base_dir = "./app/src/main/resources"
    with open(f"{base_dir}/send-coefficient.template") as f:
        template = string.Template(f.read())
    for values in template_values():
        network = values["network_type"]
        sv_random = values["score_value_random"]
        st_random = values["score_time_random"]
        ct_random = values["contact_time_random"]
        filename = f"send-coefficient_{network}_{sv_random}_{st_random}_{ct_random}.conf"
        with open(f"{base_dir}/{filename}", "w") as f:
            f.write(template.substitute(values))


if __name__ == '__main__':
    generate_configs()
