package io.sharetrace.util;

import org.apache.commons.math3.distribution.IntegerDistribution;
import org.apache.commons.math3.distribution.RealDistribution;

@FunctionalInterface
public interface DistributedRandom {

  double MIN_FINITE = Math.nextUp(Double.NEGATIVE_INFINITY);
  double MAX_FINITE = Math.nextDown(Double.POSITIVE_INFINITY);

  static DistributedRandom from(RealDistribution distribution) {
    double min = finite(distribution.getSupportLowerBound());
    double max = finite(distribution.getSupportUpperBound());
    return () -> normalized(finite(distribution.sample()), min, max);
  }

  static DistributedRandom from(IntegerDistribution distribution) {
    double min = finite(distribution.getSupportLowerBound());
    double max = finite(distribution.getSupportUpperBound());
    return () -> normalized(finite(distribution.sample()), min, max);
  }

  private static double finite(double value) {
    return Math.max(MIN_FINITE, Math.min(MAX_FINITE, value));
  }

  private static double normalized(double value, double min, double max) {
    return (value - min) / (max - min);
  }

  double nextDouble();

  default double nextDouble(double scale) {
    return nextDouble() * scale;
  }

  default float nextFloat(float scale) {
    return (float) nextDouble(scale);
  }

  default long nextLong(double scale) {
    return Math.round(nextDouble(scale));
  }
}
