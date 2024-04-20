package sharetrace.config;

import java.util.stream.Stream;

public final class InstanceFactory {

  private InstanceFactory() {}

  public static <T> T getInstance(Class<T> type, String name, Object... parameters) {
    try {
      var types = Stream.of(parameters).map(InstanceFactory::getClass).toArray(Class[]::new);
      var clazz = ClassFactory.getClass(type, name);
      return clazz.getConstructor(types).newInstance(parameters);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unused")
  private static Class<?> getClass(Object obj) {
    return switch (obj) {
      case Boolean b -> boolean.class;
      case Byte b -> byte.class;
      case Short s -> short.class;
      case Character c -> char.class;
      case Integer i -> int.class;
      case Long l -> long.class;
      case Float f -> float.class;
      case Double d -> double.class;
      default -> obj.getClass();
    };
  }
}
