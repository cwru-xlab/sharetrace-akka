package sharetrace.config;

public final class ClassFactory {

  private ClassFactory() {}

  public static <T> Class<T> getClass(String name, Class<T> expected) {
    try {
      return checkType(Class.forName(name), expected);
    } catch (ClassNotFoundException exception) {
      throw new RuntimeException(exception);
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> Class<T> checkType(Class<?> clazz, Class<T> expected) {
    if (!expected.isAssignableFrom(clazz)) {
      throw new IllegalArgumentException(
              "%s must be of type %s".formatted(clazz.getSimpleName(), expected.getSimpleName()));
    }
    return (Class<T>) clazz;
  }
}
