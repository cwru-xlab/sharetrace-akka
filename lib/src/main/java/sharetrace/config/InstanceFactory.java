package sharetrace.config;

public class InstanceFactory {

  public <T> T getInstance(String name) {
    try {
      var clazz = new ClassFactory().<T>getClass(name);
      return clazz.getConstructor().newInstance();
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }
}
