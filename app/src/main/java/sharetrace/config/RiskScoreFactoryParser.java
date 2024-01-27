package sharetrace.config;

import com.typesafe.config.Config;
import sharetrace.model.DistributedRandom;
import sharetrace.model.Parameters;
import sharetrace.model.factory.RandomRiskScoreFactoryBuilder;
import sharetrace.model.factory.RiskScoreFactory;
import sharetrace.model.factory.TimeFactory;

public record RiskScoreFactoryParser(
    Parameters parameters,
    ConfigParser<DistributedRandom> randomParser,
    ConfigParser<TimeFactory> timeFactoryParser)
    implements ConfigParser<RiskScoreFactory> {

  @Override
  public RiskScoreFactory parse(Config config) {
    return RandomRiskScoreFactoryBuilder.create()
        .random(randomParser.parse(config.getConfig("value-distribution")))
        .timeFactory(timeFactoryParser.parse(config.getConfig("time-factory")))
        .scoreExpiry(parameters.scoreExpiry())
        .build();
  }
}
