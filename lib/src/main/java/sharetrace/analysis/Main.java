package sharetrace.analysis;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;
import sharetrace.analysis.handler.EventHandler;
import sharetrace.analysis.handler.EventHandlers;
import sharetrace.analysis.model.EventRecord;
import sharetrace.config.InstanceFactory;
import sharetrace.logging.Jackson;
import sharetrace.util.Parser;

public final class Main {

  private Main() {}

  public static void main(String[] args) {
    var config = getConfig();
    var handlers = newEventHandlersMap();
    try (var stream = newEventRecordStream()) {
      stream.forEach(record -> processRecord(record, handlers, config));
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    } finally {
      handlers.values().forEach(EventHandler::onComplete);
    }
  }

  private static Config getConfig() {
    return ConfigFactory.load().getConfig("sharetrace.analysis");
  }

  private static Map<String, EventHandler> newEventHandlersMap() {
    return new Object2ObjectOpenHashMap<>();
  }

  private static Stream<EventRecord> newEventRecordStream() throws IOException {
    var directory = Path.of(System.getProperty("config.logs"));
    var parser = newEventRecordParser();
    return new EventRecordStream(parser).open(directory);
  }

  private static Parser<String, EventRecord> newEventRecordParser() {
    return new EventRecordParser(Jackson.newObjectMapper());
  }

  private static void processRecord(
      EventRecord record, Map<String, EventHandler> handlers, Config config) {
    handlers.computeIfAbsent(record.key(), k -> newEventHandler(k, config)).onNext(record.event());
  }

  private static EventHandler newEventHandler(String key, Config config) {
    var handlers =
        config.getStringList("handlers").stream()
            .<EventHandler>map(InstanceFactory::getInstance)
            .collect(ObjectArrayList.toList());
    return new EventHandlers(key, handlers);
  }
}
