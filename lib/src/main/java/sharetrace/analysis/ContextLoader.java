package sharetrace.analysis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import sharetrace.analysis.model.Context;

public record ContextLoader(ObjectMapper mapper) {

  public Map<String, Context> loadContexts(Path directory) {
    try (var lines = Files.lines(directory.resolve("properties.log"))) {
      return lines.map(parseContext()).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private Function<String, Entry<String, Context>> parseContext() {
    var reader = mapper.reader();
    return input -> {
      try {
        var tree = reader.readTree(input);
        var key = tree.get("k").asText();
        var nodes = tree.get("p").get("network").get("nodes").asInt();
        var edges = tree.get("p").get("network").get("edges").asInt();
        return Map.entry(key, new Context(nodes, edges));
      } catch (JsonProcessingException e) {
        throw new UncheckedIOException(e);
      }
    };
  }
}
