#!/bin/python

import collections
import itertools
import string

distributions = [
    "standard-normal",
    "uniform"
]


def network_configs():
    defaults = {
        "degree": -1,
        "edges": -1,
        "initial_nodes": -1,
        "nearest_neighbors": -1,
        "network_factory": "missing",
        "new_edges": -1,
        "nodes": 5000,
        "rewiring_probability": -1,
    }
    nodes = defaults["nodes"]
    return [
        {
            **defaults,
            "edges": int(0.005 * nodes * (nodes - 1) / 2),
            "network_factory": "gnm-random"
        },
        {
            **defaults,
            "initial_nodes": 10,
            "new_edges": 5,
            "network_factory": "barabasi-albert"
        },
        {
            **defaults,
            "degree": 20,
            "network_factory": "random-regular"
        },
        {
            **defaults,
            "nearest_neighbors": 20,
            "rewiring_probability": 0.2,
            "network_factory": "watts-strogatz"
        },
        {
            **defaults,
            "network_factory": "scale-free"
        }
    ]


def contact_time_factory_configs():
    for dist in distributions:
        yield {
            "contact_time_distribution": dist
        }


def score_factory_configs():
    for value_dist, time_dist in itertools.product(distributions, repeat=2):
        yield {
            "score_value_distribution": value_dist,
            "score_time_distribution": time_dist,
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
        factory = values["network_factory"]
        sv_dist = values["score_value_distribution"]
        st_dist = values["score_time_distribution"]
        ct_dist = values["contact_time_distribution"]
        filename = "_".join(("send-coefficient", factory, sv_dist, st_dist, ct_dist))
        with open(f"./app/src/main/resources/{filename}.conf", "w") as f:
            f.write(template.substitute(values))


if __name__ == '__main__':
    generate_configs()
