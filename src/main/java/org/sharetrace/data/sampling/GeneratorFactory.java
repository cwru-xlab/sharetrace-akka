package org.sharetrace.data.sampling;

import org.apache.commons.math3.random.RandomGenerator;

@FunctionalInterface
public interface GeneratorFactory {

  default RandomGenerator create(int seed) {
    return create((long) seed);
  }

  RandomGenerator create(long seed);
}
