package org.sharetrace.util;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public final class EventLog<E extends Enum<E> & LoggableEvent> {

  private final Set<E> enabled;

  private EventLog(Set<E> enabled) {
    this.enabled = enabled;
  }

  public static <E extends Enum<E> & LoggableEvent> EventLog<E> enable(EnumSet<E> enable) {
    return new EventLog<>(enable);
  }

  @SafeVarargs
  public static <E extends Enum<E> & LoggableEvent> EventLog<E> enable(E first, E... rest) {
    return enable(EnumSet.of(first, rest));
  }

  public static <E extends Enum<E> & LoggableEvent> EventLog<E> enable(Collection<E> enable) {
    return enable(EnumSet.copyOf(enable));
  }

  public static <E extends Enum<E> & LoggableEvent> EventLog<E> enableAll(Class<E> clazz) {
    return enable(EnumSet.allOf(clazz));
  }

  public static <E extends Enum<E> & LoggableEvent> EventLog<E> disable(EnumSet<E> disable) {
    return enable(EnumSet.complementOf(disable));
  }

  @SafeVarargs
  public static <E extends Enum<E> & LoggableEvent> EventLog<E> disable(E first, E... rest) {
    return disable(EnumSet.of(first, rest));
  }

  public static <E extends Enum<E> & LoggableEvent> EventLog<E> disable(Collection<E> disable) {
    return disable(EnumSet.copyOf(disable));
  }

  public static <E extends Enum<E> & LoggableEvent> EventLog<E> disableAll(Class<E> clazz) {
    return enable(EnumSet.noneOf(clazz));
  }

  public boolean contains(E event) {
    return enabled.contains(event);
  }

  public int size() {
    return enabled.size();
  }

  @Override
  public int hashCode() {
    return Objects.hash(enabled);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EventLog<?> eventLog = (EventLog<?>) o;
    return Objects.equals(enabled, eventLog.enabled);
  }

  @Override
  public String toString() {
    return "EventLog{" + enabled + '}';
  }
}
