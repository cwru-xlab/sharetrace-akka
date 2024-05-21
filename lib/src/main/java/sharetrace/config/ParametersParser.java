package sharetrace.config;

import com.typesafe.config.Config;
import sharetrace.model.Parameters;
import sharetrace.model.ParametersBuilder;

public record ParametersParser() implements ConfigParser<Parameters> {

  @Override
  public Parameters parse(Config config) {
    return ParametersBuilder.create()
        .transmissionRate(config.getDouble("transmission-rate"))
        .sendCoefficient(config.getDouble("send-coefficient"))
        .tolerance(config.getDouble("tolerance"))
        .timeBuffer(getMillisDuration(config, "time-buffer"))
        .scoreExpiry(getMillisDuration(config, "score-expiry"))
        .contactExpiry(getMillisDuration(config, "contact-expiry"))
        .flushTimeout(config.getDuration("flush-timeout"))
        .idleTimeout(config.getDuration("idle-timeout"))
        .build();
  }

  private static long getMillisDuration(Config config, String name) {
    return ConfigSupport.getMillisDuration(config, name);
  }
}
