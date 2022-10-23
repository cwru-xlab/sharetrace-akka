package io.sharetrace.data.factory;

import io.sharetrace.util.IntervalCache;

@FunctionalInterface
public interface CacheFactory<T extends Comparable<T>> {

  IntervalCache<T> newCache();
}
