package org.sharetrace.logging;

import java.util.Set;
import java.util.function.Supplier;
import net.logstash.logback.argument.StructuredArguments;
import org.sharetrace.util.TypedSupplier;

final class DefaultLogger implements Logger {

  private final Set<Class<? extends Loggable>> loggable;
  private final Supplier<org.slf4j.Logger> logger;

  private DefaultLogger(
      Set<Class<? extends Loggable>> loggable, Supplier<org.slf4j.Logger> logger) {
    this.loggable = Set.copyOf(loggable);
    this.logger = logger;
  }

  public static DefaultLogger of(Set<Class<? extends Loggable>> loggable, org.slf4j.Logger logger) {
    return new DefaultLogger(loggable, () -> logger);
  }

  public static DefaultLogger of(
      Set<Class<? extends Loggable>> loggable, Supplier<org.slf4j.Logger> logger) {
    return new DefaultLogger(loggable, logger);
  }

  public boolean log(String message, String key, TypedSupplier<? extends Loggable> supplier) {
    boolean logged = false;
    if (loggable.contains(supplier.getType())) {
      logger.get().info(message, StructuredArguments.value(key, supplier.get()));
      logged = true;
    }
    return logged;
  }
}
