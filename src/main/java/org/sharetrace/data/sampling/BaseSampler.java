package org.sharetrace.data.sampling;

import static org.sharetrace.util.Preconditions.checkArgument;
import java.util.Random;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well44497a;
import org.immutables.value.Value;

abstract class BaseSampler {

  private static final String UNBOUNDED_MESSAGE =
      "Distribution must have finite lower and upper support bounds";

  protected static double normalizedSample(RealDistribution distribution) {
    double max = distribution.getSupportUpperBound();
    double min = distribution.getSupportLowerBound();
    return (distribution.sample() - min) / (max - min);
  }

  protected static void checkBoundedness(RealDistribution distribution) {
    checkArgument(
        distribution.getSupportLowerBound() != Double.NEGATIVE_INFINITY, () -> UNBOUNDED_MESSAGE);
    checkArgument(
        distribution.getSupportUpperBound() != Double.POSITIVE_INFINITY, () -> UNBOUNDED_MESSAGE);
  }

  /**
   * Returns a pseudo-random number generator. If overriding this method, it is recommended to also
   * override {@link #seed()}.
   */
  @Value.Default
  protected RandomGenerator randomGenerator() {
    return new Well44497a(seed());
  }

  /**
   * Returns a seed that can be used by {@link #randomGenerator()} to create a pseudo-random number
   * generator.
   */
  @Value.Default
  protected long seed() {
    return new Random().nextLong();
  }
}
