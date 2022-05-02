package org.sharetrace.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class EventLogs {

  private final Map<Class<? extends LoggableEvent>, EventLog<?>> logs;

  public EventLogs() {
    this.logs = new HashMap<>();
  }

  @SuppressWarnings("unchecked")
  public <E extends Enum<E> & LoggableEvent> EventLog<E> get(Class<E> clazz) {
    return (EventLog<E>) logs.get(clazz);
  }

  public <E extends Enum<E> & LoggableEvent> void put(Class<E> clazz, EventLog<E> log) {
    logs.put(clazz, log);
  }

  @Override
  public int hashCode() {
    return Objects.hash(logs);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EventLogs eventLogs = (EventLogs) o;
    return Objects.equals(logs, eventLogs.logs);
  }

  @Override
  public String toString() {
    Map<String, Integer> nameAndSize =
        logs.entrySet().stream()
            .map(log -> Map.entry(log.getKey().getSimpleName(), log.getValue().size()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    return "EventLogs{" + nameAndSize + '}';
  }
}
