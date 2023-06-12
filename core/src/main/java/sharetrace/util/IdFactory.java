package sharetrace.util;

import java.util.concurrent.ThreadLocalRandom;

public final class IdFactory {

  private IdFactory() {}

  public static String newIntString() {
    return String.valueOf(newInt());
  }

  public static int newInt() {
    return ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
  }

  public static String newLongString() {
    return String.valueOf(newLong());
  }

  public static long newLong() {
    return ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
  }
}
