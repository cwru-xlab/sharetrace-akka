package sharetrace.util;

import org.apache.commons.math3.distribution.IntegerDistribution;
import org.apache.commons.math3.distribution.RealDistribution;

@FunctionalInterface
public interface DistributedRandom {

  static DistributedRandom from(RealDistribution distribution) {
    double min = distribution.getSupportLowerBound();
    double max = distribution.getSupportUpperBound();
    return () -> Ranges.normalized(distribution.sample(), min, max);
  }

  static DistributedRandom from(IntegerDistribution distribution) {
    double min = distribution.getSupportLowerBound();
    double max = distribution.getSupportUpperBound();
    return () -> Ranges.normalized(distribution.sample(), min, max);
  }

  double nextDouble();

  default double nextDouble(double scale) {
    return nextDouble() * scale;
  }

  default float nextFloat(double scale) {
    return Ranges.finiteFloat(nextDouble(scale));
  }

  default long nextLong(double scale) {
    return Math.round(nextDouble(scale));
  }
}