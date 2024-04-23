package sharetrace.config;

import com.typesafe.config.Config;
import sharetrace.model.Context;
import sharetrace.model.factory.RandomTimeFactoryBuilder;
import sharetrace.model.factory.TimeFactory;
import sharetrace.model.random.DistributedRandom;

public record TimeFactoryParser(Context context, ConfigParser<DistributedRandom> randomParser)
    implements ConfigParser<TimeFactory> {

  @Override
  public TimeFactory parse(Config config) {
    return RandomTimeFactoryBuilder.create()
        .referenceTime(context.referenceTime())
        .period(ConfigSupport.getMillisDuration(config, "period"))
        .random(randomParser.parse(config.getConfig("random")))
        .build();
  }
}
