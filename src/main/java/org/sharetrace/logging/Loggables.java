package org.sharetrace.logging;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import net.logstash.logback.argument.StructuredArguments;
import org.sharetrace.util.TypedSupplier;
import org.slf4j.Logger;

public final class Loggables {

  private final Set<Class<? extends Loggable>> loggable;
  private final Supplier<Logger> logger;

  private Loggables(Set<Class<? extends Loggable>> loggable, Supplier<Logger> logger) {
    this.loggable = Collections.unmodifiableSet(loggable);
    this.logger = Objects.requireNonNull(logger);
  }

  public static Loggables create(Set<Class<? extends Loggable>> loggable, Logger logger) {
    return new Loggables(loggable, () -> logger);
  }

  public static Loggables create(Set<Class<? extends Loggable>> loggable, Supplier<Logger> logger) {
    return new Loggables(loggable, logger);
  }

  public void info(String message, String key, Loggable value) {
    info(message, key, TypedSupplier.of(value));
  }

  public void info(String message, String key, TypedSupplier<? extends Loggable> supplier) {
    if (loggable.contains(supplier.getType())) {
      logger.get().info(message, StructuredArguments.value(key, supplier.get()));
    }
  }

  public void debug(String message, String key, Loggable value) {
    debug(message, key, TypedSupplier.of(value));
  }

  public void debug(String message, String key, TypedSupplier<? extends Loggable> supplier) {
    if (loggable.contains(supplier.getType())) {
      logger.get().debug(message, StructuredArguments.value(key, supplier.get()));
    }
  }

  public Set<Class<? extends Loggable>> loggable() {
    return loggable;
  }
}
