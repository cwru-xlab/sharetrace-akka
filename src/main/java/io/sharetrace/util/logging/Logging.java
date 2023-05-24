package io.sharetrace.util.logging;

import ch.qos.logback.core.spi.PropertyContainer;
import io.sharetrace.util.Collecting;
import io.sharetrace.util.logging.event.LoggableEvent;
import io.sharetrace.util.logging.metric.LoggableMetric;
import io.sharetrace.util.logging.setting.LoggableSetting;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public final class Logging {

  private static final Map<String, String> mdc = Collecting.newHashMap();
  private static final Set<Class<? extends Loggable>> enabled = Collecting.newHashSet();

  private Logging() {}

  public static TypedLogger<LoggableMetric> metricsLogger() {
    return newLogger("MetricsLogger");
  }

  private static <T extends Loggable> TypedLogger<T> newLogger(String name) {
    return new DefaultLogger<>(LoggerFactory.getLogger(name));
  }

  public static TypedLogger<LoggableEvent> eventsLogger() {
    return newLogger("EventsLogger");
  }

  public static TypedLogger<LoggableSetting> settingsLogger() {
    return newLogger("SettingsLogger");
  }

  public static Path graphsPath() {
    PropertyContainer properties = (PropertyContainer) LoggerFactory.getILoggerFactory();
    return Path.of(properties.getProperty("graphs.log.dir"));
  }

  public static Map<String, String> getMdc() {
    return Collecting.unmodifiable(mdc);
  }

  public static synchronized void setMdc(String stateId) {
    mdc.put("sid", stateId); // MDC for all threads
    MDC.setContextMap(mdc); // Still set MDC for calling/main thread.
  }

  public static synchronized void enable(Collection<Class<? extends Loggable>> types) {
    enabled.clear();
    enabled.addAll(types);
  }

  private static final class DefaultLogger<T extends Loggable> implements TypedLogger<T> {

    private final Logger delegate;

    public DefaultLogger(Logger delegate) {
      this.delegate = delegate;
    }

    @Override
    public <R extends T> boolean log(String key, Class<R> type, Supplier<R> loggable) {
      boolean logged = delegate.isInfoEnabled() && enabled.contains(type);
      if (logged) {
        delegate.info(key, StructuredArguments.value(key, loggable.get()));
      }
      return logged;
    }
  }
}
