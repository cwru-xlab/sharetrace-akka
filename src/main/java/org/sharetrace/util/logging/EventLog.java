package org.sharetrace.util.logging;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;

public final class EventLog<E extends Enum<E> & LoggableEvent> {

  private final Collection<E> enabled;

  private EventLog(Collection<E> enabled) {
    this.enabled = enabled;
  }

  public static <E extends Enum<E> & LoggableEvent> EventLog<E> enable(Collection<E> enable) {
    return new EventLog<>(EnumSet.copyOf(enable));
  }

  @SafeVarargs
  public static <E extends Enum<E> & LoggableEvent> EventLog<E> enable(E first, E... rest) {
    return new EventLog<>(EnumSet.of(first, rest));
  }

  public static <E extends Enum<E> & LoggableEvent> EventLog<E> enableAll(Class<E> clazz) {
    return new EventLog<>(EnumSet.allOf(clazz));
  }

  public static <E extends Enum<E> & LoggableEvent> EventLog<E> disable(Collection<E> disable) {
    return new EventLog<>(EnumSet.complementOf(EnumSet.copyOf(disable)));
  }

  @SafeVarargs
  public static <E extends Enum<E> & LoggableEvent> EventLog<E> disable(E first, E... rest) {
    return new EventLog<>(EnumSet.complementOf(EnumSet.of(first, rest)));
  }

  public static <E extends Enum<E> & LoggableEvent> EventLog<E> disableAll(Class<E> clazz) {
    return new EventLog<>(EnumSet.noneOf(clazz));
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
