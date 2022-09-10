package org.sharetrace.data.factory;

import org.sharetrace.util.IntervalCache;

@FunctionalInterface
public interface CacheFactory<T> {

  IntervalCache<T> newCache();
}
