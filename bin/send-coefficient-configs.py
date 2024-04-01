#!/bin/python

import collections
import itertools
import string

common_network_config = {
    "nodes": 10_000,
    "cache_network": "false"
}

distributions = [
    "standard-normal",
    "uniform",
    "right-skew-beta",
    "left-skew-beta"
]


def gnm_random_configs():
    nodes = common_network_config["nodes"]
    max_nodes = nodes * (nodes - 1) / 2
    for edges in (int(0.1 * p * max_nodes) for p in range(2, 11, 2)):
        yield {
            "edges": edges,
            "network_factory": "gnm-random",
            **common_network_config
        }


def barabasi_albert_configs():
    initial_nodes = [10, 15, 20, 25]
    new_edges = [5, 10, 15, 20]
    for nodes, edges in zip(initial_nodes, new_edges):
        yield {
            "initial_nodes": nodes,
            "new_edges": edges,
            "network_factory": "barabasi-albert",
            **common_network_config
        }


def random_regular_configs():
    for degree in [20, 40, 80]:
        yield {
            "degree": degree,
            "network_factory": "random-regular",
            **common_network_config
        }


def watts_strogatz_configs():
    nearest_neighbors = [20, 40, 80]
    rewiring_probability = [0.2, 0.4, 0.8]
    for nn, rp in itertools.product(nearest_neighbors, rewiring_probability):
        yield {
            "nearest_neighbors": nn,
            "rewiring_probability": rp,
            "network_factory": "watts-strogatz",
            **common_network_config
        }


def scale_free_configs():
    yield {
        "nodes": common_network_config["nodes"],
        "network_factory": "scale-free"
    }


def network_configs():
    return itertools.chain(
        gnm_random_configs(),
        barabasi_albert_configs(),
        random_regular_configs(),
        watts_strogatz_configs(),
        scale_free_configs()
    )


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
        score_value_dist = values["score_value_distribution"]
        score_time_dist = values["score_time_distribution"]
        contact_time_dist = values["contact_time_distribution"]
        config_name = "_".join(("send-coefficient", sc, factory, score_value_dist, score_time_dist, contact_time_dist))
        with open(f"./app/src/main/resources/{config_name}.conf", "w") as f:
            f.write(template.safe_substitute(values))


if __name__ == '__main__':
    generate_configs()
