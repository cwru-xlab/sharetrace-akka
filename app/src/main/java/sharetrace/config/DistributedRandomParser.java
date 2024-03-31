package sharetrace.config;

import com.typesafe.config.Config;
import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import sharetrace.model.random.BetaDistributedRandom;
import sharetrace.model.random.DistributedRandom;
import sharetrace.model.random.NormalDistributedRandom;
import sharetrace.model.random.UniformDistributedRandom;

public record DistributedRandomParser(RandomGenerator randomGenerator)
    implements ConfigParser<DistributedRandom> {

  @Override
  public DistributedRandom parse(Config config) {
    var type = config.getString("type");
    return switch (type) {
      case ("normal") -> normal(config);
      case ("beta") -> beta(config);
      case ("uniform") -> uniform(config);
      default -> throw new IllegalArgumentException(type);
    };
  }

  private DistributedRandom normal(Config config) {
    var location = config.getDouble("location");
    var scale = config.getDouble("scale");
    return new NormalDistributedRandom(new NormalDistribution(randomGenerator, location, scale));
  }

  private DistributedRandom beta(Config config) {
    var alpha = config.getDouble("alpha");
    var beta = config.getDouble("beta");
    return new BetaDistributedRandom(new BetaDistribution(randomGenerator, alpha, beta));
  }

  private DistributedRandom uniform(Config config) {
    var lower = config.getDouble("lower-bound");
    var upper = config.getDouble("upper-bound");
    return new UniformDistributedRandom(new UniformRealDistribution(randomGenerator, lower, upper));
  }
}
