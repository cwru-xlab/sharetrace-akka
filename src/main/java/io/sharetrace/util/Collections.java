package io.sharetrace.util;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatLists;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntLists;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public final class Collections {

  private Collections() {}

  public static Set<Integer> ofInts(int... elements) {
    return IntSet.of(elements);
  }

  public static <T> List<T> newArrayList(int size) {
    return new ObjectArrayList<>(size);
  }

  public static <T> Set<T> newHashSet(Set<T> set) {
    return new ObjectOpenHashSet<>(set);
  }

  public static <T> Set<T> newHashSet() {
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

  public static <T> Set<T> immutable(Set<? extends T> set) {
    return java.util.Collections.unmodifiableSet(set);
  }

  public static <K, V> Map<K, V> immutable(Map<? extends K, ? extends V> map) {
    return java.util.Collections.unmodifiableMap(map);
  }

  @SuppressWarnings("unchecked")
  public static <T> Collector<T, ?, List<T>> toImmutableList(int size) {
    return Collectors.collectingAndThen(
        Collector.of(
            () -> (ObjectList<T>) new ObjectArrayList<>(size),
            List::add,
            (left, right) -> {
              left.addAll(right);
              return left;
            }),
        ObjectLists::unmodifiable);
  }

  public static <T> Collector<T, ?, Set<T>> toImmutableSet(int size) {
    return Collectors.collectingAndThen(
        ObjectOpenHashSet.toSetWithExpectedSize(size), ObjectSets::unmodifiable);
  }

  public static Collector<Integer, ?, List<Integer>> toImmutableIntList(int size) {
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

  public static Collector<Float, ?, List<Float>> toImmutableFloatList(int size) {
    return Collectors.collectingAndThen(
        Collector.of(
            () -> new FloatArrayList(size),
            List::add,
            (left, right) -> {
              left.addAll(right);
              return left;
            }),
        FloatLists::unmodifiable);
  }

  public static Collector<Float, ?, List<Float>> toImmutableFloatList() {
    return Collectors.collectingAndThen(
        Collector.of(
            FloatArrayList::new,
            List::add,
            (left, right) -> {
              left.addAll(right);
              return left;
            }),
        FloatLists::unmodifiable);
  }
}