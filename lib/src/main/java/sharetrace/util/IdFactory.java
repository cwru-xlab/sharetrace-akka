package sharetrace.util;

import de.huxhorn.sulky.ulid.ULID;
import java.util.concurrent.ThreadLocalRandom;

public final class IdFactory {

  private static final ULID ULID = new ULID();

  private IdFactory() {}

  public static long newLong() {
    return ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
  }

  @SuppressWarnings("SpellCheckingInspection")
  public static String nextUlid() {
    return ULID.nextULID();
  }
}
