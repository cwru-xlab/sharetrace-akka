package sharetrace.experiment.data;

import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well44497a;

public final class Well44497aRandomGeneratorFactory implements RandomGeneratorFactory {

  @Override
  public RandomGenerator getRandom(long seed) {
    return new Well44497a(seed);
  }
}
