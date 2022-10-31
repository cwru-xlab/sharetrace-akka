from __future__ import annotations

from numbers import Real
from typing import Union

import numpy as np
import scipy as sp

from hints import ArrayLike

ArrayOrScalar = Union[Real, np.ndarray[Real]]


def kl_div(x: ArrayLike, y: ArrayLike, **kwargs) -> ArrayOrScalar:
    """Returns the KL divergence between x and y."""
    return sp.special.kl_div(x, y, **kwargs)


def js_div(x: ArrayLike, y: ArrayLike, **kwargs) -> ArrayOrScalar:
    """Returns the Jensen-Shannon divergence between x and y."""
    return sp.spatial.distance.jensenshannon(x, y, **kwargs)


def wasserstein(x: ArrayLike, y: ArrayLike, **kwargs) -> ArrayOrScalar:
    """Returns the Wasserstein distance between x and y."""
    return sp.stats.wasserstein_distance(x, y, **kwargs)
