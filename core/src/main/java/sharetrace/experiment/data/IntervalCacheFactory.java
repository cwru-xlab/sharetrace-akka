package sharetrace.experiment.data;

import sharetrace.util.cache.IntervalCache;

@FunctionalInterface
public interface IntervalCacheFactory<V extends Comparable<? super V>> {

  IntervalCache<V> newCache();
}
