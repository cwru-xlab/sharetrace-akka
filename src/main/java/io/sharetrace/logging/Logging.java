package io.sharetrace.logging;

import ch.qos.logback.core.spi.PropertyContainer;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.LoggerFactory;

public final class Logging {

  public static final String EVENTS_LOGGER_NAME = "EventsLogger";
  public static final String METRICS_LOGGER_NAME = "MetricsLogger";
  public static final String SETTINGS_LOGGER_NAME = "SettingsLogger";

  private static final Set<Class<? extends Loggable>> enabled = new ObjectOpenHashSet<>();

  private Logging() {}

  public static Logger metricsLogger() {
    return logger(LoggerFactory.getLogger(METRICS_LOGGER_NAME));
  }

  public static Logger logger(org.slf4j.Logger delegate) {
    return new Logger() {
      @Override
      public <T extends Loggable> boolean log(String key, Class<T> type, Supplier<T> loggable) {
        boolean logged = delegate.isInfoEnabled() && enabled.contains(type);
        if (logged) {
          delegate.info(key, StructuredArguments.value(key, loggable.get()));
        }
        return logged;
      }
    };
  }

  public static Logger settingsLogger() {
    return logger(LoggerFactory.getLogger(SETTINGS_LOGGER_NAME));
  }

  public static Path graphsPath() {
    PropertyContainer properties = (PropertyContainer) LoggerFactory.getILoggerFactory();
    return Path.of(properties.getProperty("graphs.log.dir"));
  }

  public static Map<String, String> mdc(String stateId) {
    return Map.of("sid", stateId);
  }

  public static synchronized void setLoggable(Collection<Class<? extends Loggable>> loggable) {
    enabled.clear();
    enabled.addAll(loggable);
  }
}
