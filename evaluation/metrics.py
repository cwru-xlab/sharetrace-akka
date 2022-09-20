from __future__ import annotations

import collections
import enum
import json
import os
from typing import AnyStr, Any


class Metric(str, enum.Enum):
    CREATE_USERS_RUNTIME = "CreateUsersRuntime"
    GRAPH_CYCLES = "GraphCycles"
    GRAPH_ECCENTRICITY = "GraphEccentricity"
    GRAPH_SCORES = "GraphScores"
    GRAPH_SIZE = "GraphSize"
    GRAPH_TOPOLOGY = "GraphTopology"
    MSG_PASSING_RUNTIME = "MsgPassingRuntime"
    RISK_PROP_RUNTIME = "RiskPropRuntime"
    SEND_CONTACTS_RUNTIME = "SendContactsRuntime"
    SEND_SCORES_RUNTIME = "SendScoresRuntime"

    def __repr__(self) -> str:
        return self.value


def load(path: os.PathLike | AnyStr) -> dict[str, Any]:
    metrics = collections.defaultdict(dict)
    with open(path) as f:
        for line in f:
            record: dict[str, Any] = json.loads(line)
            metric: dict[str, Any] = record["metric"]
            label = Metric(metric.pop("type"))
            metrics[record["sid"]][label] = metric
    return dict(metric)
