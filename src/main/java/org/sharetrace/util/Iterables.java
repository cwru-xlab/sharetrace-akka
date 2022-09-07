package org.sharetrace.util;

import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class Iterables {

  private Iterables() {}

  public static <T> Iterable<T> fromStream(Stream<T> stream) {
    return stream::iterator;
  }

  public static Iterable<Integer> fromStream(IntStream stream) {
    return stream::iterator;
  }
}
