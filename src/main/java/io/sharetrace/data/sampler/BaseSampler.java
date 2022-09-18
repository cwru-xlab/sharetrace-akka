package io.sharetrace.data.sampler;

import org.apache.commons.math3.distribution.RealDistribution;

abstract class BaseSampler<T> implements Sampler<T> {

  protected static float normalizedSample(RealDistribution distribution) {
    double max = Math.min(Double.MAX_VALUE, distribution.getSupportUpperBound());
    double min = Math.max(Double.MIN_VALUE, distribution.getSupportLowerBound());
    double sample = Math.max(Double.MIN_VALUE, Math.min(Double.MAX_VALUE, distribution.sample()));
    return (float) ((sample - min) / (max - min));
  }
}
