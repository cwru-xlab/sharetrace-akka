package sharetrace.config;

import java.util.Arrays;

public final class InstanceFactory {

  private InstanceFactory() {}

  public static <T> T getInstance(Class<T> type, String name, Object... parameters) {
    try {
      var types = Arrays.stream(parameters).map(InstanceFactory::getClass).toArray(Class[]::new);
      var clazz = ClassFactory.getClass(type, name);
      return clazz.getConstructor(types).newInstance(parameters);
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  private static Class<?> getClass(Object obj) {
      return switch (obj) {
          case Boolean b -> boolean.class;
          case Byte b -> byte.class;
          case Short i -> short.class;
          case Character c -> char.class;
          case Integer i -> int.class;
          case Long l -> long.class;
          case Float v -> float.class;
          case Double v -> double.class;
          default -> obj.getClass();
      };
  }
}
