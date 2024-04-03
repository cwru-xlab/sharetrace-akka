package sharetrace.model.factory;

public final class IdFactory {

  private IdFactory() {}

  public static String newId() {
    return String.valueOf(System.currentTimeMillis());
  }
}
