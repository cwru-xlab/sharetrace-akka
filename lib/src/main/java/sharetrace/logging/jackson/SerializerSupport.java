package sharetrace.logging.jackson;

final class SerializerSupport {

  private SerializerSupport() {}

  public static String toString(Class<?> cls) {
    return cls.getSimpleName();
  }
}
