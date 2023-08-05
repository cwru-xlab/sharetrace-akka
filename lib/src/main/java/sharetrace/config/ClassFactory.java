package sharetrace.config;

public final class ClassFactory {

  private ClassFactory() {}

  @SuppressWarnings("unchecked")
  public static <T> Class<T> getClass(String name) {
    try {
      return (Class<T>) Class.forName(name);
    } catch (ClassNotFoundException exception) {
      throw new RuntimeException(exception);
    }
  }
}
