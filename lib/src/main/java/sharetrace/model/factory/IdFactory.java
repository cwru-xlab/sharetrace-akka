package sharetrace.model.factory;

import java.util.UUID;

public final class IdFactory {

  private IdFactory() {}

  public static String newId() {
    return UUID.randomUUID().toString();
  }
}
