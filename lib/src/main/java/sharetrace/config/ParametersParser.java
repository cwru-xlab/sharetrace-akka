package sharetrace.config;

import com.typesafe.config.Config;
import java.util.concurrent.TimeUnit;
import sharetrace.model.Parameters;
import sharetrace.model.ParametersBuilder;

public record ParametersParser() implements ConfigParser<Parameters> {

  @Override
  public Parameters parse(Config config) {
    return ParametersBuilder.create()
        .transmissionRate(config.getDouble("transmission-rate"))
        .sendCoefficient(config.getDouble("send-coefficient"))
        .timeBuffer(parseDuration(config, "time-buffer"))
        .scoreExpiry(parseDuration(config, "score-expiry"))
        .contactExpiry(parseDuration(config, "contact-expiry"))
        .batchTimeout(config.getDuration("batch-timeout"))
        .idleTimeout(config.getDuration("idle-timeout"))
        .build();
  }

  private static long parseDuration(Config config, String name) {
    return config.getDuration(name, TimeUnit.MILLISECONDS);
  }
}
