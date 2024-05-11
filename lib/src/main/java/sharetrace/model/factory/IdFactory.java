package sharetrace.model.factory;

import de.huxhorn.sulky.ulid.ULID;

public final class IdFactory {

  private static final ULID ULID = new ULID();

  private IdFactory() {}

  public static String newId() {
    return ULID.nextULID();
  }
}
