package org.sharetrace.util;

import com.google.common.base.MoreObjects;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Map;

public class Indexer<T> {

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
    Integer indexed = index.putIfAbsent(value, this.value);
    return (indexed == null) ? this.value++ : indexed;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("value", value).toString();
  }
}
