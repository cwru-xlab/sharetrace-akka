package io.sharetrace.util.logging;

import ch.qos.logback.core.spi.PropertyContainer;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public final class Logging {

  private static final Map<String, String> mdc = new Object2ObjectOpenHashMap<>();
  private static final Set<Class<? extends Loggable>> enabled = new ObjectOpenHashSet<>();

  private Logging() {}

  public static Logger metricsLogger() {
    return logger(LoggerFactory.getLogger("MetricsLogger"));
  }

  private static Logger logger(org.slf4j.Logger delegate) {
    return new DefaultLogger(delegate);
  }

  public static Logger eventsLogger() {
    return logger(LoggerFactory.getLogger("EventsLogger"));
  }

  public static Logger settingsLogger() {
    return logger(LoggerFactory.getLogger("SettingsLogger"));
  }

  public static Path graphsPath() {
    PropertyContainer properties = (PropertyContainer) LoggerFactory.getILoggerFactory();
    return Path.of(properties.getProperty("graphs.log.dir"));
  }

  public static synchronized void setMdc(String stateId) {
    mdc.put("sid", stateId); // MDC for all threads
    MDC.setContextMap(mdc); // Still set MDC for calling/main thread.
  }

  public static Map<String, String> getMdc() {
    return Collections.unmodifiableMap(mdc);
  }

  public static synchronized void setLoggable(Collection<Class<? extends Loggable>> loggable) {
    enabled.clear();
    enabled.addAll(loggable);
  }

  private static final class DefaultLogger implements Logger {

    private final org.slf4j.Logger delegate;

    public DefaultLogger(org.slf4j.Logger delegate) {
      this.delegate = delegate;
    }

    @Override
    public <T extends Loggable> boolean log(String key, Class<T> type, Supplier<T> loggable) {
      boolean logged = delegate.isInfoEnabled() && enabled.contains(type);
      if (logged) {
        delegate.info(key, StructuredArguments.value(key, loggable.get()));
      }
      return logged;
    }
  }
}
