package org.sharetrace.data.sampling;

import org.apache.commons.math3.random.RandomGenerator;

@FunctionalInterface
public interface GeneratorFactory {

  RandomGenerator create(long seed);
}
