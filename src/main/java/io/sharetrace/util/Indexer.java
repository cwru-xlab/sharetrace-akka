package io.sharetrace.util;

import com.google.common.base.MoreObjects;

import java.util.Map;

public final class Indexer<T> {

    private final Map<T, Integer> index;
    private int value;

    public Indexer() {
        index = Collecting.newIntValuedHashMap();
        value = 0;
    }

    public int index(T value) {
        return index.computeIfAbsent(value, x -> this.value++);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("value", value).toString();
    }
}
