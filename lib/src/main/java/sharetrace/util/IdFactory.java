package sharetrace.util;

import java.util.concurrent.ThreadLocalRandom;

public final class IdFactory {

  private IdFactory() {}

  public static String newKey() {
    return String.valueOf(ThreadLocalRandom.current().nextInt(100_000));
  }

  public static long newLong() {
    return ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
  }

  public static long newSeed() {
    return ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
  }
}
