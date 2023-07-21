package sharetrace.util;

import java.util.concurrent.ThreadLocalRandom;

public final class IdFactory {

  private IdFactory() {}

  public static int newInt() {
    return ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
  }

  public static long newLong() {
    return ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
  }
}
