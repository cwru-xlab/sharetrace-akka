package io.sharetrace.logging;

import io.sharetrace.util.TypedSupplier;
import java.util.function.Supplier;
import net.logstash.logback.argument.StructuredArguments;

public final class Logger {

  private final Supplier<org.slf4j.Logger> delegate;

  Logger(Supplier<org.slf4j.Logger> delegate) {
    this.delegate = delegate;
  }

  public boolean log(String key, TypedSupplier<? extends Loggable> loggable) {
    org.slf4j.Logger logger = delegate.get();
    boolean logged = logger.isInfoEnabled() && Logging.isEnabled(loggable.getType());
    if (logged) {
      logger.info(key, StructuredArguments.value(key, loggable.get()));
    }
    return logged;
  }
}
