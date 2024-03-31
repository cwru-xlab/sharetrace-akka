package sharetrace.config;

import com.typesafe.config.Config;
import sharetrace.model.factory.AutoIncrementingKeyFactory;
import sharetrace.model.factory.KeyFactory;

public record KeyFactoryParser() implements ConfigParser<KeyFactory> {

  @Override
  public KeyFactory parse(Config config) {
    var type = config.getString("type");
    if (type.equals("auto-incrementing")) {
      return new AutoIncrementingKeyFactory(config.getLong("initial-value"));
    }
    throw new IllegalArgumentException(type);
  }
}
