package sharetrace.config;

import com.typesafe.config.Config;
import java.util.concurrent.TimeUnit;
import sharetrace.model.Parameters;
import sharetrace.model.ParametersBuilder;

public record ParametersParser() implements ConfigParser<Parameters> {

  @Override
  public Parameters parse(Config config) {
    return ParametersBuilder.create()
        .contactExpiry(parseDuration(config, "contact-expiry"))
        .scoreExpiry(parseDuration(config, "score-expiry"))
        .timeout(parseDuration(config, "timeout"))
        .timeBuffer(parseDuration(config, "time-buffer"))
        .sendCoefficient(config.getDouble("send-coefficient"))
        .transmissionRate(config.getDouble("transmission-rate"))
        .build();
  }

  private static long parseDuration(Config config, String name) {
    return config.getDuration(name, TimeUnit.MILLISECONDS);
  }
}
