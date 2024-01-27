package sharetrace.config;

import com.typesafe.config.Config;

@FunctionalInterface
public interface ConfigParser<T> extends Parser<Config, T> {}
