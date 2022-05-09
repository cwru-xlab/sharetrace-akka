package org.sharetrace.data.sampling;

import java.util.Random;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well44497a;
import org.immutables.value.Value;

abstract class BaseSampler {

  @Value.Default
  protected RandomGenerator randomGenerator() {
    return new Well44497a(seed());
  }

  @Value.Default
  protected long seed() {
    return new Random().nextLong();
  }
}
