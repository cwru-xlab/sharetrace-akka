package sharetrace.config;

import com.typesafe.config.Config;
import java.util.Collection;
import java.util.List;

@FunctionalInterface
public interface ConfigParser<T> {

  T parse(Config config);

  default List<T> parseAll(Collection<? extends Config> configs) {
    return configs.stream().map(this::parse).toList();
  }
}
