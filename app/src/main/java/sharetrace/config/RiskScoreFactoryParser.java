package sharetrace.config;

import com.typesafe.config.Config;
import sharetrace.model.Parameters;
import sharetrace.model.factory.CachedRiskScoreFactory;
import sharetrace.model.factory.IdFactory;
import sharetrace.model.factory.RandomRiskScoreFactoryBuilder;
import sharetrace.model.factory.RiskScoreFactory;
import sharetrace.model.factory.TimeFactory;
import sharetrace.model.DistributedRandom;

public record RiskScoreFactoryParser(
    Parameters parameters,
    ConfigParser<DistributedRandom> randomParser,
    ConfigParser<TimeFactory> timeFactoryParser)
    implements ConfigParser<RiskScoreFactory> {

  @Override
  public RiskScoreFactory parse(Config config) {
    return decorated(baseFactory(config), config);
  }

  private RiskScoreFactory baseFactory(Config config) {
    return RandomRiskScoreFactoryBuilder.create()
        .id(IdFactory.newId())
        .random(randomParser.parse(config.getConfig("random")))
        .timeFactory(timeFactoryParser.parse(config.getConfig("time-factory")))
        .scoreExpiry(parameters.scoreExpiry())
        .build();
  }

  private RiskScoreFactory decorated(RiskScoreFactory factory, Config config) {
    return config.getBoolean("cached") ? new CachedRiskScoreFactory(factory) : factory;
  }
}
