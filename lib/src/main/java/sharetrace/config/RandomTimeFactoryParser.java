package sharetrace.config;

import com.typesafe.config.Config;
import sharetrace.model.factory.RandomTimeFactory;
import sharetrace.model.factory.RandomTimeFactoryBuilder;
import sharetrace.util.Context;
import sharetrace.util.DistributedRandom;

public record RandomTimeFactoryParser(
    Context context, ConfigParser<? extends DistributedRandom> randomParser)
    implements ConfigParser<RandomTimeFactory> {

  @Override
  public RandomTimeFactory parse(Config config) {
    return RandomTimeFactoryBuilder.create()
        .timestamp(context.referenceTime())
        .range(config.getDuration("time-range"))
        .random(randomParser.parse(config.getConfig("time-distribution")))
        .build();
  }
}
