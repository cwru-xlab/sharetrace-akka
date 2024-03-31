package sharetrace.model.random;

import org.apache.commons.math3.distribution.RealDistribution;

final class RandomSupport {

  private RandomSupport() {}

  public static double nextDouble(RealDistribution distribution) {
    return distribution.cumulativeProbability(distribution.sample());
  }
}
