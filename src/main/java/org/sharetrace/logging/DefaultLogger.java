package org.sharetrace.logging;

import java.util.Set;
import java.util.function.Supplier;
import net.logstash.logback.argument.StructuredArguments;
import org.sharetrace.util.Checks;
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
    nonNullLoggable(loggable);
    nonNullLogger(logger);
    return new DefaultLogger(loggable, () -> logger);
  }

  public static DefaultLogger of(
      Set<Class<? extends Loggable>> loggable, Supplier<org.slf4j.Logger> logger) {
    return new DefaultLogger(nonNullLoggable(loggable), nonNullLogger(logger));
  }

  private static <T> T nonNullLogger(T logger) {
    return Checks.isNotNull(logger, "logger");
  }

  private static <T> T nonNullLoggable(T loggable) {
    return Checks.isNotNull(loggable, "loggable");
  }

  public boolean log(String message, String key, TypedSupplier<? extends Loggable> supplier) {
    Checks.isNotNull(loggable, "loggable");
    boolean logged = false;
    if (loggable.contains(supplier.getType())) {
      Checks.isNotNull(message, "message");
      Checks.isNotNull(key, "key");
      Object supplied = Checks.isNotNull(supplier.get(), "supplied");
      logger.get().info(message, StructuredArguments.value(key, supplied));
      logged = true;
    }
    return logged;
  }
}
