package sharetrace.config;

public record ClassFactory() {

  @SuppressWarnings("unchecked")
  public <T> Class<T> getClass(String name) {
    try {
      var classLoader = Thread.currentThread().getContextClassLoader();
      return (Class<T>) classLoader.loadClass(name);
    } catch (ClassNotFoundException exception) {
      throw new RuntimeException(exception);
    }
  }
}
