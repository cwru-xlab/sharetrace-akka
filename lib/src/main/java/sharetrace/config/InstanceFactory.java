package sharetrace.config;

public final class InstanceFactory {

  private InstanceFactory() {}

  public static <T> T getInstance(String name) {
    try {
      var clazz = ClassFactory.<T>getClass(name);
      return clazz.getConstructor().newInstance();
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }
}
