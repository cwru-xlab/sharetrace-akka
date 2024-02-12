package sharetrace.analysis.results;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

public final class MapResults implements Results {

  private final Map<String, Object> results;

  private MapResults(Map<String, Object> results) {
    this.results = results;
  }

  public MapResults() {
    this(new HashMap<>());
  }

  @Override
  public Results put(String key, Object result) {
    results.put(key, result);
    return this;
  }

  @Override
  public Results withScope(String scope) {
    return (Results) results.computeIfAbsent(scope, x -> new MapResults());
  }

  @JsonValue
  private Map<String, Object> value() {
    return results;
  }
}
