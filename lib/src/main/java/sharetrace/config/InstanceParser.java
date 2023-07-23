package sharetrace.config;

import com.typesafe.config.Config;

public record InstanceParser<T>(String path) implements ConfigParser<T> {

  @Override
  public T parse(Config config) {
    var className = config.getString(path);
    var clazz = new ClassFactory().<T>getClass(className);
    try {
      return clazz.getConstructor().newInstance();
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }
}
