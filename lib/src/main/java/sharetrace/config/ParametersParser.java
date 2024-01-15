package sharetrace.config;

import com.typesafe.config.Config;
import sharetrace.model.Parameters;
import sharetrace.model.ParametersBuilder;

public record ParametersParser() implements ConfigParser<Parameters> {

  @Override
  public Parameters parse(Config config) {
    return ParametersBuilder.create()
        .contactExpiry(config.getDuration("contact-expiry").toMillis())
        .scoreExpiry(config.getDuration("score-expiry").toMillis())
        .timeout(config.getDuration("timeout").toMillis())
        .timeBuffer(config.getDuration("time-buffer").toMillis())
        .sendCoefficient(config.getDouble("send-coefficient"))
        .transmissionRate(config.getDouble("transmission-rate"))
        .build();
  }
}
