package org.sharetrace.logging;

import static net.logstash.logback.argument.StructuredArguments.value;
import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;
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
    info(logger, message, key, value.getClass(), () -> value);
  }

  public void info(
      Logger logger,
      String message,
      String key,
      Class<? extends Loggable> clazz,
      Supplier<Loggable> supplier) {
    if (loggable.contains(clazz)) {
      logger.info(message, value(key, getValue(supplier, clazz)));
    }
  }

  private <T> T getValue(Supplier<T> supplier, Class<?> specifiedType) {
    T value = supplier.get();
    Class<?> valueType = value.getClass();
    if (!valueType.equals(specifiedType)) {
      throw new ClassCastException(
          "Type of the supplied value "
              + valueType
              + " does not match the specified type "
              + specifiedType);
    }
    return value;
  }

  public void info(
      String message, String key, Class<? extends Loggable> clazz, Supplier<Loggable> supplier) {
    info(logger, message, key, clazz, supplier);
  }

  public void info(Logger logger, String message, String key, Loggable value) {
    info(logger, message, key, value.getClass(), () -> value);
  }

  public void debug(Logger logger, String message, String key, Loggable value) {
    debug(logger, message, key, value.getClass(), () -> value);
  }

  public void debug(
      Logger logger,
      String message,
      String key,
      Class<? extends Loggable> clazz,
      Supplier<Loggable> supplier) {
    if (loggable.contains(clazz)) {
      logger.debug(message, value(key, getValue(supplier, clazz)));
    }
  }

  public void debug(
      String message, String key, Class<? extends Loggable> clazz, Supplier<Loggable> supplier) {
    debug(logger, message, key, clazz, supplier);
  }

  public void debug(String message, String key, Loggable value) {
    debug(logger, message, key, value.getClass(), () -> value);
  }

  public Set<Class<? extends Loggable>> loggable() {
    return loggable;
  }
}
