package org.sharetrace.util;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class Log {

  private final Set<Loggable> enabled;

  private Log(Set<Loggable> enabled) {
    this.enabled = enabled;
  }

  public static Log enable(Loggable first, Loggable... rest) {
    return new Log(EnumSet.of(first, rest));
  }

  public static Log enableAll() {
    return new Log(EnumSet.allOf(Loggable.class));
  }

  public static Log disable(Loggable... disable) {
    Log log = enableAll();
    List.of(disable).forEach(log.enabled::remove);
    return log;
  }

  public static Log disableAll() {
    return new Log(EnumSet.noneOf(Loggable.class));
  }

  public boolean contains(Loggable loggable) {
    return enabled.contains(loggable);
  }
}
