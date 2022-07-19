package org.sharetrace.util;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Map;

public class Indexer<T> {

  protected static final int DEFAULT_INITIAL_CAPACITY = 16;
  private final Map<T, Integer> index;
  private int value;

  public Indexer() {
    this(DEFAULT_INITIAL_CAPACITY);
  }

  public Indexer(int capacity) {
    index = newIndex(capacity);
    value = 0;
  }

  protected Map<T, Integer> newIndex(int capacity) {
    return new Object2IntOpenHashMap<>(capacity);
  }

  public int index(T value) {
    Integer indexed = index.putIfAbsent(value, this.value);
    return (indexed == null) ? this.value++ : indexed;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{value=" + value + '}';
  }
}
