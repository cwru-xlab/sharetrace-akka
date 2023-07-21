package sharetrace.util.logging;

import ch.qos.logback.core.spi.PropertyContainer;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import sharetrace.util.logging.event.EventRecord;
import sharetrace.util.logging.metric.MetricRecord;
import sharetrace.util.logging.setting.SettingsRecord;

public final class Logging {

  private static final Map<String, String> mdc = Maps.newHashMap();
  private static final Set<Class<? extends LogRecord>> enabled = Sets.newHashSet();

  private Logging() {}

  public static RecordLogger<MetricRecord> metricsLogger() {
    return newLogger("MetricsLogger", "metric");
  }

  public static RecordLogger<EventRecord> eventsLogger() {
    return newLogger("EventsLogger", "event");
  }

  public static RecordLogger<SettingsRecord> settingsLogger() {
    return newLogger("SettingsLogger", "setting");
  }

  public static Path logDirectory() {
    PropertyContainer properties = (PropertyContainer) LoggerFactory.getILoggerFactory();
    return Path.of(properties.getProperty("logDirectory"));
  }

  public static Map<String, String> getMdc() {
    return ImmutableMap.copyOf(mdc);
  }

  public static synchronized void setMdc(String stateId) {
    mdc.put("sid", stateId); // MDC for all threads
    MDC.setContextMap(mdc); // Still set MDC for the calling/main thread
  }

  public static synchronized void enable(Collection<Class<? extends LogRecord>> types) {
    enabled.clear();
    enabled.addAll(types);
  }

  private static <T extends LogRecord> RecordLogger<T> newLogger(String name, String key) {
    return new DefaultLogger<>(LoggerFactory.getLogger(name), key);
  }

  private static final class DefaultLogger<T extends LogRecord> implements RecordLogger<T> {

    private final Logger delegate;
    private final String key;

    public DefaultLogger(Logger delegate, String key) {
      this.delegate = delegate;
      this.key = key;
    }

    @Override
    public <R extends T> boolean log(Class<R> type, Supplier<R> record) {
      boolean logged = delegate.isInfoEnabled() && enabled.contains(type);
      if (logged) {
        delegate.info(key, StructuredArguments.value(key, record.get()));
      }
      return logged;
    }
  }
}
