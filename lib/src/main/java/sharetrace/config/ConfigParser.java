package sharetrace.config;

import com.typesafe.config.Config;
import sharetrace.util.Parser;

@FunctionalInterface
public interface ConfigParser<T> extends Parser<Config, T> {}
