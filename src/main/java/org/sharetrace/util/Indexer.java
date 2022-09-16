package org.sharetrace.util;

import com.google.common.base.MoreObjects;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Map;

public final class Indexer<T> {

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
    Checks.isNotNull(value, "value");
    return index.computeIfAbsent(value, x -> this.value++);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("value", value).toString();
  }
}
