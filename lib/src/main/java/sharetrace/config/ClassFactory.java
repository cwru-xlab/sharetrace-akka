package sharetrace.config;

public record ClassFactory() {

  @SuppressWarnings("unchecked")
  public <T> Class<T> getClass(String name) {
    try {
      return (Class<T>) Class.forName(name);
    } catch (ClassNotFoundException exception) {
      throw new RuntimeException(exception);
    }
  }
}
