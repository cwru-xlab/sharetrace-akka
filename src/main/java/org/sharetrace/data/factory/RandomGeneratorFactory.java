package org.sharetrace.data.factory;

import org.apache.commons.math3.random.RandomGenerator;

@FunctionalInterface
public interface RandomGeneratorFactory {

  RandomGenerator getGenerator(long seed);
}
