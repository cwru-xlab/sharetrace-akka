package sharetrace.util;

import org.apache.commons.math3.distribution.IntegerDistribution;
import org.apache.commons.math3.distribution.RealDistribution;

@FunctionalInterface
public interface DistributedRandom {

  static DistributedRandom from(RealDistribution distribution) {
    double min = distribution.getSupportLowerBound();
    double max = distribution.getSupportUpperBound();
    return () -> normalized(distribution.sample(), min, max);
  }

  static DistributedRandom from(IntegerDistribution distribution) {
    double min = distribution.getSupportLowerBound();
    double max = distribution.getSupportUpperBound();
    return () -> normalized(distribution.sample(), min, max);
  }

  private static double normalized(double value, double min, double max) {
    return finite(value - min) / finite(max - min);
  }

  private static double finite(double value) {
    return bound(value, -Double.MAX_VALUE, Double.MAX_VALUE);
  }

  private static double bound(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }

  double nextDouble();

  default double nextDouble(double scale) {
    return nextDouble() * scale;
  }

  default float nextFloat(float scale) {
    return (float) bound(nextDouble(scale), -Float.MAX_VALUE, Float.MAX_VALUE);
  }

  default long nextLong(double scale) {
    return Math.round(nextDouble(scale));
  }
}
