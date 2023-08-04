package sharetrace.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import sharetrace.analysis.handler.EventHandler;
import sharetrace.analysis.handler.EventHandlers;
import sharetrace.analysis.model.EventRecord;
import sharetrace.config.InstanceFactory;
import sharetrace.util.Parser;

record Main() {

  public static void main(String[] args) {
    var config = ConfigFactory.load().getConfig("sharetrace.analysis");
    var parser = newEventRecordParser();
    var handlers = new Object2ObjectOpenHashMap<String, EventHandler>();
    try (var stream = newEventStream(config)) {
      for (var it = stream.iterator(); it.hasNext(); ) {
        var line = it.next();
        var record = parser.parse(line);
        var handler = handlers.computeIfAbsent(record.key(), x -> newHandler(config));
        handler.onNext(record.event());
      }
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    } finally {
      handlers.values().forEach(EventHandler::onComplete);
    }
  }

  private static Parser<String, EventRecord> newEventRecordParser() {
    return new EventRecordParser(new ObjectMapper().findAndRegisterModules());
  }

  private static Stream<String> newEventStream(Config config) throws IOException {
    var directory = Path.of(config.getString("log-directory"));
    return EventStream.of(directory);
  }

  private static EventHandler newHandler(Config config) {
    var factory = new InstanceFactory();
    return config.getStringList("handlers").stream()
        .<EventHandler>map(factory::getInstance)
        .collect(Collectors.collectingAndThen(ObjectArrayList.toList(), EventHandlers::new));
  }
}
