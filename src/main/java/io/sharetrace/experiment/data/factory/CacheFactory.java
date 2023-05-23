package io.sharetrace.experiment.data.factory;

import io.sharetrace.util.cache.IntervalCache;

@FunctionalInterface
public interface CacheFactory<V extends Comparable<? super V>> {

  IntervalCache<V> newCache();
}
