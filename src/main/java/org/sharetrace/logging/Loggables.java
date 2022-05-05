package org.sharetrace.logging;

import static net.logstash.logback.argument.StructuredArguments.value;
import java.util.Collections;
import java.util.Set;
import org.sharetrace.util.TypedSupplier;
import org.slf4j.Logger;

public final class Loggables {

  private final Set<Class<? extends Loggable>> loggable;
  private final Logger logger;

  private Loggables(Set<Class<? extends Loggable>> loggable) {
    this(loggable, null);
  }

  private Loggables(Set<Class<? extends Loggable>> loggable, Logger logger) {
    this.loggable = Collections.unmodifiableSet(loggable);
    this.logger = logger;
  }

  public static Loggables create(Set<Class<? extends Loggable>> loggable) {
    return new Loggables(loggable);
  }

  public static Loggables create(Set<Class<? extends Loggable>> loggable, Logger logger) {
    return new Loggables(loggable, logger);
  }

  public void info(String message, String key, Loggable value) {
    info(logger, message, key, TypedSupplier.of(value.getClass(), value));
  }

  public void info(
      Logger logger, String message, String key, TypedSupplier<? extends Loggable> supplier) {
    if (loggable.contains(supplier.getType())) {
      logger.info(message, value(key, supplier.get()));
    }
  }

  public void info(String message, String key, TypedSupplier<? extends Loggable> supplier) {
    info(logger, message, key, supplier);
  }

  public void info(Logger logger, String message, String key, Loggable value) {
    info(logger, message, key, TypedSupplier.of(value.getClass(), value));
  }

  public void debug(Logger logger, String message, String key, Loggable value) {
    debug(logger, message, key, TypedSupplier.of(value.getClass(), value));
  }

  public void debug(
      Logger logger, String message, String key, TypedSupplier<? extends Loggable> supplier) {
    if (loggable.contains(supplier.getType())) {
      logger.debug(message, value(key, supplier.get()));
    }
  }

  public void debug(String message, String key, TypedSupplier<? extends Loggable> supplier) {
    debug(logger, message, key, supplier);
  }

  public void debug(String message, String key, Loggable value) {
    debug(logger, message, key, TypedSupplier.of(value.getClass(), value));
  }

  public Set<Class<? extends Loggable>> loggable() {
    return loggable;
  }
}
