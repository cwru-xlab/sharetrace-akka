package org.sharetrace.logging;

import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import net.logstash.logback.argument.StructuredArguments;
import org.sharetrace.util.TypedSupplier;

class DefaultLogger implements Logger {

  private final Set<Class<? extends Loggable>> loggable;
  private final Supplier<org.slf4j.Logger> logger;

  private DefaultLogger(
      Set<Class<? extends Loggable>> loggable, Supplier<org.slf4j.Logger> logger) {
    this.loggable = Set.copyOf(loggable);
    this.logger = logger;
  }

  public static DefaultLogger of(Set<Class<? extends Loggable>> loggable, org.slf4j.Logger logger) {
    Objects.requireNonNull(logger);
    return new DefaultLogger(Objects.requireNonNull(loggable), () -> logger);
  }

  public static DefaultLogger of(
      Set<Class<? extends Loggable>> loggable, Supplier<org.slf4j.Logger> logger) {
    return new DefaultLogger(Objects.requireNonNull(loggable), Objects.requireNonNull(logger));
  }

  public boolean log(String message, String key, TypedSupplier<? extends Loggable> loggable) {
    boolean logged = false;
    if (this.loggable.contains(loggable.getType())) {
      logger.get().info(message, StructuredArguments.value(key, loggable.get()));
      logged = true;
    }
    return logged;
  }
}
