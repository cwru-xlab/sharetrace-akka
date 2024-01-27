package sharetrace.config;

import com.typesafe.config.Config;
import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import sharetrace.util.DistributedRandom;

public record DistributedRandomParser(RandomGenerator randomGenerator)
    implements ConfigParser<DistributedRandom> {

  @Override
  public DistributedRandom parse(Config config) {
    var type = config.getString("type");
    return switch (type) {
      case ("normal") -> normalRandom(config);
      case ("beta") -> betaRandom(config);
      case ("uniform") -> uniformRandom(config);
      default -> throw new IllegalArgumentException(type);
    };
  }

  private DistributedRandom normalRandom(Config config) {
    var mean = config.getDouble("mean");
    var stdDev = config.getDouble("standard-deviation");
    return DistributedRandom.from(new NormalDistribution(randomGenerator, mean, stdDev));
  }

  private DistributedRandom betaRandom(Config config) {
    var alpha = config.getDouble("alpha");
    var beta = config.getDouble("beta");
    return DistributedRandom.from(new BetaDistribution(randomGenerator, alpha, beta));
  }

  private DistributedRandom uniformRandom(Config config) {
    var lower = config.getDouble("lower-bound");
    var upper = config.getDouble("upper-bound");
    return DistributedRandom.from(new UniformRealDistribution(randomGenerator, lower, upper));
  }
}
