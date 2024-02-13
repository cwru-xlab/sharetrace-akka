package sharetrace.analysis;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
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
import sharetrace.logging.jackson.Jackson;

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
    return handlers;
  }

  private static Config getConfig() {
    return ConfigFactory.load().getConfig("sharetrace.analysis");
  }

  private static Stream<EventRecord> eventRecords() throws IOException {
    var parser = new EventRecordParser(Jackson.ionObjectMapper());
    return new EventRecordStream(parser).open(logsDirectory());
  }

  private static void processRecord(
      EventRecord record, Map<String, EventHandler> handlers, Config config) {
    handlers.computeIfAbsent(record.key(), x -> newEventHandler(config)).onNext(record.event());
  }

  private static EventHandler newEventHandler(Config config) {
    return config.getStringList("handlers").stream()
        .map(className -> InstanceFactory.getInstance(EventHandler.class, className))
        .collect(Collectors.collectingAndThen(Collectors.toList(), EventHandlers::new));
  }

  private static Results collectResults(Map<String, EventHandler> handlers) {
    var results = new MapResults();
    handlers.forEach((key, handler) -> handler.onComplete(results.withScope(key)));
    return results;
  }

  private static void saveResults(Results results) {
    try {
      Jackson.objectMapper().writeValue(resultsFile(), results);
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  private static File resultsFile() {
    var filename = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    return logsDirectory().resolve(filename + ".json").toFile();
  }

  private static Path logsDirectory() {
    return Path.of(System.getProperty("analysis.logs"));
  }
}
