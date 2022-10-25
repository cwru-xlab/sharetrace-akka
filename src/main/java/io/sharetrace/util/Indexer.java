package io.sharetrace.util;

import com.google.common.base.MoreObjects;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Map;

public final class Indexer<T> {

  private static final String VALUE = "value";

  private final Map<T, Integer> index;
  private int value;

  public Indexer() {
    this(Object2IntOpenHashMap.DEFAULT_INITIAL_SIZE);
  }

  public Indexer(int capacity) {
    index = new Object2IntOpenHashMap<>(capacity);
    value = 0;
  }

  public int index(T value) {
    return index.computeIfAbsent(value, x -> this.value++);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add(VALUE, value).toString();
  }
}
