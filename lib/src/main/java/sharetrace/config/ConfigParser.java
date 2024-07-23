package sharetrace.config;

import com.typesafe.config.Config;

public interface ConfigParser<T> {

  T parse(Config config);
}
