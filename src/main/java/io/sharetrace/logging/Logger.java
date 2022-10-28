package io.sharetrace.logging;

import io.sharetrace.util.TypedSupplier;
import java.util.Set;
import java.util.function.Supplier;
import net.logstash.logback.argument.StructuredArguments;

public final class Logger {

  private final Set<Class<? extends Loggable>> loggable;
  private final Supplier<org.slf4j.Logger> delegate;

  Logger(Set<Class<? extends Loggable>> loggable, Supplier<org.slf4j.Logger> delegate) {
    this.loggable = Set.copyOf(loggable);
    this.delegate = delegate;
  }

  public boolean log(String key, TypedSupplier<? extends Loggable> loggable) {
    org.slf4j.Logger logger = delegate.get();
    boolean logged = logger.isInfoEnabled() && this.loggable.contains(loggable.getType());
    if (logged) {
      logger.info(key, StructuredArguments.value(key, loggable.get()));
    }
    return logged;
  }
}
