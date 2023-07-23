package sharetrace.model.factory;

import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well44497a;

public record Well44497aRandomGeneratorFactory() implements RandomGeneratorFactory {

  @Override
  public RandomGenerator getRandomGenerator(long seed) {
    return new Well44497a(seed);
  }
}
