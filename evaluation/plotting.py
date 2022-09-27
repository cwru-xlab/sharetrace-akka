from __future__ import annotations

from collections import namedtuple
from numbers import Real

import numpy as np

from events import TimelineData

EventPlotPositions = namedtuple("EventPlotPositions", "positions n_events")


def event_plot_positions(
        timeline: TimelineData,
        x_axis: str = "time",
        timescale: Real = 1e3
) -> EventPlotPositions:
    if x_axis not in (valid := ("message", "time")):
        raise ValueError(f"'x_axis' must be one of {valid}")
    n_events = len(timeline.e2i)
    idx = timeline.timestamps.argsort()
    events = timeline.flatten()[idx]
    masks = ((events == e) for e in range(n_events))
    if x_axis == "message":
        positions = [np.flatnonzero(mask) for mask in masks]
    else:
        times = timeline.timestamps[idx]
        positions = [times[mask] / timescale for mask in masks]
    return EventPlotPositions(positions=positions, n_events=n_events)
