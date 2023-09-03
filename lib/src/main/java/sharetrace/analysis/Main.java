package sharetrace.analysis;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import sharetrace.analysis.handler.EventHandler;
import sharetrace.analysis.handler.EventHandlers;
import sharetrace.analysis.model.EventRecord;
import sharetrace.analysis.results.MapResults;
import sharetrace.analysis.results.Results;
import sharetrace.config.InstanceFactory;
import sharetrace.logging.Jackson;

public final class Main {

  private Main() {}

  public static void main(String[] args) {
    var handlers = analyzeLogs();
    var results = collectResults(handlers);
    saveResults(results);
  }

  private static Map<String, EventHandler> analyzeLogs() {
    var config = getConfig();
    var handlers = new HashMap<String, EventHandler>();
    try (var records = eventRecords()) {
      records.forEach(record -> processRecord(record, handlers, config));
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
    return Collections.unmodifiableMap(handlers);
  }

  private static Config getConfig() {
    return ConfigFactory.load().getConfig("sharetrace.analysis");
  }

  private static Stream<EventRecord> eventRecords() throws IOException {
    var directory = Path.of(System.getProperty("analysis.logs"));
    var parser = new EventRecordParser(Jackson.newIonObjectMapper());
    return new EventRecordStream(parser).open(directory);
  }

  private static void processRecord(
      EventRecord record, Map<String, EventHandler> handlers, Config config) {
    handlers.computeIfAbsent(record.key(), x -> newEventHandler(config)).onNext(record.event());
  }

  private static EventHandler newEventHandler(Config config) {
    return config.getStringList("handlers").stream()
        .<EventHandler>map(InstanceFactory::getInstance)
        .collect(Collectors.collectingAndThen(Collectors.toList(), EventHandlers::new));
  }

  private static Results collectResults(Map<String, EventHandler> handlers) {
    var results = new MapResults(".");
    handlers.forEach((key, handler) -> handler.onComplete(results.withScope(key)));
    return results;
  }

  private static void saveResults(Results results) {
    try {
      Jackson.newObjectMapper()
          .writerWithDefaultPrettyPrinter()
          .writeValue(new File("test.json"), results);
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }
}
