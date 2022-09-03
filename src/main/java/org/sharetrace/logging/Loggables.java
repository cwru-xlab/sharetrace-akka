package org.sharetrace.logging;

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
    this.loggable = Set.copyOf(loggable);
    this.logger = Objects.requireNonNull(logger);
  }

  public static Loggables create(Set<Class<? extends Loggable>> loggable, Logger logger) {
    return new Loggables(loggable, () -> logger);
  }

  public static Loggables create(Set<Class<? extends Loggable>> loggable, Supplier<Logger> logger) {
    return new Loggables(loggable, logger);
  }

  public boolean log(String messageAndKey, Loggable value) {
    return log(messageAndKey, messageAndKey, TypedSupplier.of(value));
  }

  public boolean log(String message, String key, TypedSupplier<? extends Loggable> supplier) {
    boolean logged = false;
    if (loggable.contains(supplier.getType())) {
      logger.get().info(message, StructuredArguments.value(key, supplier.get()));
      logged = true;
    }
    return logged;
  }

  public boolean log(String messageAndKey, TypedSupplier<? extends Loggable> supplier) {
    return log(messageAndKey, messageAndKey, supplier);
  }

  public Set<Class<? extends Loggable>> loggable() {
    return loggable;
  }
}
