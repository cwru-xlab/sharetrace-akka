package io.sharetrace.util;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleLists;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntLists;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public final class Collecting {

  private Collecting() {}

  public static Set<Integer> ofInts(int... elements) {
    return IntSet.of(elements);
  }

  public static <E> List<E> newArrayList(int size) {
    return new ObjectArrayList<>(size);
  }

  public static <E> Set<E> newHashSet(Set<E> set) {
    return new ObjectOpenHashSet<>(set);
  }

  public static <E> Set<E> newHashSet() {
    return new ObjectOpenHashSet<>();
  }

  public static <K, V> Map<K, V> newHashMap() {
    return new Object2ObjectOpenHashMap<>();
  }

  public static <V> Map<Integer, V> newIntKeyedHashMap() {
    return new Int2ObjectOpenHashMap<>();
  }

  public static <V> Map<Integer, V> newIntKeyedHashMap(int size) {
    return new Int2ObjectOpenHashMap<>(size);
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

  public static <E> Set<E> unmodifiable(Set<? extends E> set) {
    return Collections.unmodifiableSet(set);
  }

  public static <K, V> Map<K, V> unmodifiable(Map<? extends K, ? extends V> map) {
    return Collections.unmodifiableMap(map);
  }

  public static <E> Collector<E, ?, List<E>> toUnmodifiableList(int size) {
    return Collectors.collectingAndThen(
        ObjectArrayList.toListWithExpectedSize(size), ObjectLists::unmodifiable);
  }

  public static <E> Collector<E, ?, Set<E>> toUnmodifiableSet(int size) {
    return Collectors.collectingAndThen(
        ObjectOpenHashSet.toSetWithExpectedSize(size), ObjectSets::unmodifiable);
  }

  public static Collector<Integer, ?, List<Integer>> toUnmodifiableIntList(int size) {
    return Collectors.collectingAndThen(
        Collector.of(
            () -> new IntArrayList(size),
            List::add,
            (left, right) -> {
              left.addAll(right);
              return left;
            }),
        IntLists::unmodifiable);
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
