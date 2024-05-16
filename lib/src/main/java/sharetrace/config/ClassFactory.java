package sharetrace.config;

public final class ClassFactory {

  private ClassFactory() {}

  public static <T> T getInstance(Class<T> type, String name) {
    try {
      return getClass(type, name).getConstructor().newInstance();
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> Class<T> getClass(Class<T> expected, String name) {
    try {
      return checkType(expected, Class.forName(name));
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> Class<T> checkType(Class<? super T> expected, Class<?> actual) {
    if (!expected.isAssignableFrom(actual)) {
      throw new IllegalArgumentException(
          "%s must be of type %s".formatted(actual.getName(), expected.getName()));
    }
    return (Class<T>) actual;
  }
}
