package sharetrace.config;

import com.typesafe.config.Config;
import sharetrace.model.factory.RandomTimeFactoryBuilder;
import sharetrace.model.factory.TimeFactory;
import sharetrace.util.Context;
import sharetrace.util.DistributedRandom;

public record TimeFactoryParser(Context context, ConfigParser<DistributedRandom> randomParser)
    implements ConfigParser<TimeFactory> {

  @Override
  public TimeFactory parse(Config config) {
    return RandomTimeFactoryBuilder.create()
        .timestamp(context.referenceTime())
        .range(config.getDuration("time-range"))
        .random(randomParser.parse(config.getConfig("time-distribution")))
        .build();
  }
}