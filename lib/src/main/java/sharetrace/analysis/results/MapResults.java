package sharetrace.analysis.results;

import com.fasterxml.jackson.annotation.JsonValue;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import java.util.Map;

public final class MapResults implements Results {

  private final Map<String, Object> results;

  public MapResults() {
    results = new Object2ReferenceOpenHashMap<>();
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
  @SuppressWarnings("unused")
  private Map<String, Object> value() {
    return results;
  }
}
