package org.sharetrace.data;

import org.sharetrace.util.IntervalCache;

@FunctionalInterface
public interface CacheFactory<T> {

  IntervalCache<T> create();
}
