package sharetrace.util;

import org.apache.commons.math3.distribution.IntegerDistribution;
import org.apache.commons.math3.distribution.RealDistribution;

@FunctionalInterface
public interface DistributedRandom {

  static DistributedRandom from(RealDistribution distribution) {
    var min = distribution.getSupportLowerBound();
    var max = distribution.getSupportUpperBound();
    return () -> Doubles.normalized(distribution.sample(), min, max);
  }

  static DistributedRandom from(IntegerDistribution distribution) {
    var min = distribution.getSupportLowerBound();
    var max = distribution.getSupportUpperBound();
    return () -> Doubles.normalized(distribution.sample(), min, max);
  }

  double nextDouble();

  default double nextDouble(double scale) {
    return nextDouble() * scale;
  }

  default long nextLong(double scale) {
    return Math.round(nextDouble(scale));
  }
}
