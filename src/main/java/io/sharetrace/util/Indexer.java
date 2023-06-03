package io.sharetrace.util;

import java.util.Map;

public final class Indexer<V> {

  private final Map<V, Integer> index = Collecting.newIntValuedHashMap();

  private int current = 0;

  public int index(V value) {
    return index.computeIfAbsent(value, key -> current++);
  }
}
