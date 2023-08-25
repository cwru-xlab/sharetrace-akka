package sharetrace.config;

import com.typesafe.config.Config;
import sharetrace.model.Parameters;
import sharetrace.model.ParametersBuilder;

public record ParametersParser() implements ConfigParser<Parameters> {

  @Override
  public Parameters parse(Config config) {
    return ParametersBuilder.create()
        .contactExpiry(config.getDuration("contact-expiry"))
        .scoreExpiry(config.getDuration("score-expiry"))
        .idleTimeout(config.getDuration("idle-timeout"))
        .timeBuffer(config.getDuration("time-buffer"))
        .sendCoefficient(config.getDouble("send-coefficient"))
        .transmissionRate(config.getDouble("transmission-rate"))
        .build();
  }
}
