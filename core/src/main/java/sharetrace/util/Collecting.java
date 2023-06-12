package sharetrace.util;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleLists;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public final class Collecting {

  private Collecting() {}

  public static <E> Set<E> newHashSet() {
    return new ObjectOpenHashSet<>();
  }

  public static <K, V> Map<K, V> newHashMap() {
    return new Object2ObjectOpenHashMap<>();
  }

  public static <K, V> Map<K, V> newHashMap(int size) {
    return new Object2ObjectOpenHashMap<>(size);
  }

  public static <K> Map<K, Integer> newIntValuedHashMap() {
    return new Object2IntOpenHashMap<>();
  }

  public static <V> Map<Long, V> newLongKeyedHashMap() {
    return new Long2ObjectOpenHashMap<>();
  }

  public static <K> Map<K, Long> newLongValuedHashMap() {
    return new Object2LongOpenHashMap<>();
  }

  public static <K, V> Map<K, V> unmodifiable(Map<? extends K, ? extends V> map) {
    return Collections.unmodifiableMap(map);
  }

  public static Collector<Double, ?, List<Double>> toUnmodifiableDoubleList() {
    return toUnmodifiableDoubleList(DoubleArrayList.DEFAULT_INITIAL_CAPACITY);
  }

  public static Collector<Double, ?, List<Double>> toUnmodifiableDoubleList(int size) {
    return Collectors.collectingAndThen(
        Collector.of(
            () -> new DoubleArrayList(size),
            List::add,
            (left, right) -> {
              left.addAll(right);
              return left;
            }),
        DoubleLists::unmodifiable);
  }
}
