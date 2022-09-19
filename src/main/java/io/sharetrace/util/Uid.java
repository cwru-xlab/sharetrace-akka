package io.sharetrace.util;

import java.util.concurrent.ThreadLocalRandom;

public final class Uid {

  private Uid() {}

  public static int ofInt() {
    return ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
  }

  public static String ofIntString() {
    return String.valueOf(ofInt());
  }

  public static long ofLong() {
    return ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
  }

  public static String ofLongString() {
    return String.valueOf(ofLong());
  }
}
