package io.sharetrace.experiment.data;

import io.sharetrace.util.cache.IntervalCache;

@FunctionalInterface
public interface IntervalCacheFactory<V extends Comparable<? super V>> {

  IntervalCache<V> newCache();
}
