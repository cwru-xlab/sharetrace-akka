package sharetrace.experiment.data;

import org.apache.commons.math3.random.RandomGenerator;

public interface RandomGeneratorFactory {

  RandomGenerator getRandom(long seed);
}
