package sharetrace.config;

import java.util.Arrays;

public final class InstanceFactory {

  private InstanceFactory() {}

  public static <T> T getInstance(String name, Object... parameters) {
    try {
      var types = Arrays.stream(parameters).map(Object::getClass).toArray(Class[]::new);
      var clazz = ClassFactory.<T>getClass(name);
      return clazz.getConstructor(types).newInstance(parameters);
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }
}
