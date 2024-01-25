package sharetrace.util;

import org.apache.commons.math3.distribution.IntegerDistribution;
import org.apache.commons.math3.distribution.RealDistribution;

@FunctionalInterface
public interface DistributedRandom {

  static DistributedRandom from(RealDistribution distribution) {
    return () -> distribution.cumulativeProbability(distribution.sample());
  }

  static DistributedRandom from(IntegerDistribution distribution) {
    return () -> distribution.cumulativeProbability(distribution.sample());
  }

  double nextDouble();

  default double nextDouble(double bound) {
    return nextDouble() * bound;
  }

  default double nextDouble(double origin, double bound) {
    return nextDouble() * (bound - origin) + origin;
  }

  default long nextLong(double bound) {
    return Math.round(nextDouble(bound));
  }
}
