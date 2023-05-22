package io.sharetrace.util;

import java.util.Map;

public final class Indexer<V> {

  private final Map<V, Integer> index = Collecting.newIntValuedHashMap();

  private int value = 0;

  public int index(V value) {
    return index.computeIfAbsent(value, x -> this.value++);
  }
}
