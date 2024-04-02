#!/bin/python

import collections
import itertools
import string

distributions = [
    "standard-normal",
    "uniform",
    "right-skew-beta",
    "left-skew-beta"
]


def network_configs():
    defaults = {
        "cache_network": "false",
        "degree": -1,
        "edges": -1,
        "initial_nodes": -1,
        "nearest_neighbors": -1,
        "network_factory": "missing",
        "new_edges": -1,
        "nodes": 10_000,
        "rewiring_probability": -1,
    }
    nodes = defaults["nodes"]
    return [
        {
            **defaults,
            "edges": 0.04 * nodes * (nodes - 1) / 2,
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
            "degree": 40,
            "network_factory": "random-regular"
        },
        {
            **defaults,
            "nearest_neighbors": 40,
            "rewiring_probability": 0.4,
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
            "cache_scores": "false"
        }


def parameter_configs():
    for sc in (0.01 * c for c in range(50, 151, 25)):
        yield {
            "send_coefficient": sc
        }


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
        sc = format(int(values["send_coefficient"] * 100), "03d")
        factory = values["network_factory"]
        sv_dist = values["score_value_distribution"]
        st_dist = values["score_time_distribution"]
        ct_dist = values["contact_time_distribution"]
        filename = "_".join(("send-coefficient", sc, factory, sv_dist, st_dist, ct_dist))
        with open(f"./app/src/main/resources/{filename}.conf", "w") as f:
            f.write(template.substitute(values))


if __name__ == '__main__':
    generate_configs()
