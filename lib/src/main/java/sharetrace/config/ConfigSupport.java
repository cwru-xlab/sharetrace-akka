package sharetrace.config;

import com.typesafe.config.Config;
import java.util.concurrent.TimeUnit;

public final class ConfigSupport {

  private ConfigSupport() {}

  public static long getMillisDuration(Config config, String name) {
    return config.getDuration(name, TimeUnit.MILLISECONDS);
  }
}
