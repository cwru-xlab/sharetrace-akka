package sharetrace.model.factory;

import org.apache.commons.math3.random.RandomGenerator;

@FunctionalInterface
public interface RandomGeneratorFactory {

  RandomGenerator getRandomGenerator(long seed);
}
