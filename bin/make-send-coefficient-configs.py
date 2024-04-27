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
        "nodes": 5000,
        "rewiring_probability": -1,
    }
    nodes = defaults["nodes"]
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
    for rand in randoms:
        yield {
            "contact_time_random": rand
        }


def score_factory_configs():
    for value_rand, time_rand in itertools.product(randoms, repeat=2):
        yield {
            "score_value_random": value_rand,
            "score_time_random": time_rand,
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
    with open("./app/src/main/resources/send-coefficient.template") as f:
        template = string.Template(f.read())
    for values in template_values():
        factory = values["network_type"]
        sv_rand = values["score_value_random"]
        st_rand = values["score_time_random"]
        ct_rand = values["contact_time_random"]
        filename = "_".join(("send-coefficient", factory, sv_rand, st_rand, ct_rand))
        with open(f"./app/src/main/resources/{filename}.conf", "w") as f:
            f.write(template.substitute(values))


if __name__ == '__main__':
    generate_configs()
