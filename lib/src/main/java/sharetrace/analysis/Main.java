package sharetrace.analysis;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import sharetrace.analysis.handler.EventHandler;
import sharetrace.analysis.handler.EventHandlers;
import sharetrace.analysis.model.Context;
import sharetrace.analysis.model.EventRecord;
import sharetrace.analysis.model.Results;
import sharetrace.config.InstanceFactory;
import sharetrace.logging.jackson.Jackson;
import sharetrace.model.factory.IdFactory;

public final class Main {

  private Main() {}

  public static void main(String[] args) {
    var context = loadContexts();
    var handlers = analyzeLogs(context);
    var results = collectResults(handlers, context);
    saveResults(results);
  }

  private static Map<String, Context> loadContexts() {
    return new ContextLoader(Jackson.objectMapper()).loadContexts(logsDirectory());
  }

  private static Map<String, EventHandler> analyzeLogs(Map<String, Context> contexts) {
    var config = loadConfig();
    var handlers = new HashMap<String, EventHandler>();
    try (var records = loadEventRecords()) {
      records.forEach(record -> processRecord(record, contexts, handlers, config));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return handlers;
  }

  private static Config loadConfig() {
    return ConfigFactory.load().getConfig("sharetrace.analysis");
  }

  private static Stream<EventRecord> loadEventRecords() throws IOException {
    return new EventRecordsLoader(Jackson.ionObjectMapper()).loadEventRecords(logsDirectory());
  }

  private static void processRecord(
      EventRecord record,
      Map<String, Context> contexts,
      Map<String, EventHandler> handlers,
      Config config) {
    handlers
        .computeIfAbsent(record.key(), x -> newEventHandler(config))
        .onNext(record.event(), contexts.get(record.key()));
  }

  private static EventHandler newEventHandler(Config config) {
    return config.getStringList("handlers").stream()
        .map(className -> InstanceFactory.getInstance(EventHandler.class, className))
        .collect(Collectors.collectingAndThen(Collectors.toList(), EventHandlers::new));
  }

  private static Results collectResults(
      Map<String, EventHandler> handlers, Map<String, Context> contexts) {
    var results = new Results();
    handlers.forEach(
        (key, handler) -> handler.onComplete(results.withScope(key), contexts.get(key)));
    return results;
  }

  private static void saveResults(Results results) {
    try {
      Jackson.objectMapper().writeValue(newResultsFile(), results);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static File newResultsFile() {
    return logsDirectory().resolve("results-" + IdFactory.newId() + ".json").toFile();
  }

  private static Path logsDirectory() {
    return Path.of(System.getProperty("logs.dir"));
  }
}
