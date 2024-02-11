package sharetrace.analysis.results;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class MapResults implements Results {

  private final Map<String, Object> results;
  private final String scope;
  private final String delimiter;

  private MapResults(Map<String, Object> results, String scope, String delimiter) {
    this.results = results;
    this.scope = scope;
    this.delimiter = delimiter;
  }

  public MapResults(String delimiter) {
    this(new HashMap<>(), null, delimiter);
  }

  @Override
  public Results put(String key, Object result) {
    results.put(join(scope, key), result);
    return this;
  }

  @Override
  public Results withScope(Object scope) {
    return new MapResults(results, join(this.scope, scope.toString()), delimiter);
  }

  @JsonValue
  private Map<String, Object> value() {
    return results;
  }

  private String join(String... elements) {
    return Stream.of(elements).filter(Objects::nonNull).collect(Collectors.joining(delimiter));
  }
}
