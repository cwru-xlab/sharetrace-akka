package sharetrace.config;

import com.typesafe.config.Config;

public class ConfigParseException extends RuntimeException {

  public ConfigParseException(String message) {
    super(message);
  }

  public ConfigParseException(String name, Config config) {
    this("Unable to parse " + name + " in " + config);
  }
}
