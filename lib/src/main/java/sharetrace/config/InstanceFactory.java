package sharetrace.config;

import java.util.Arrays;

public final class InstanceFactory {

  private InstanceFactory() {}

  public static <T> T getInstance(String name, Object... parameters) {
    try {
      var types = Arrays.stream(parameters).map(InstanceFactory::getClass).toArray(Class[]::new);
      var clazz = ClassFactory.<T>getClass(name);
      return clazz.getConstructor(types).newInstance(parameters);
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  private static Class<?> getClass(Object obj) {
    if (obj instanceof Boolean) return boolean.class;
    else if (obj instanceof Byte) return byte.class;
    else if (obj instanceof Short) return short.class;
    else if (obj instanceof Character) return char.class;
    else if (obj instanceof Integer) return int.class;
    else if (obj instanceof Long) return long.class;
    else if (obj instanceof Float) return float.class;
    else if (obj instanceof Double) return double.class;
    else return obj.getClass();
  }
}
