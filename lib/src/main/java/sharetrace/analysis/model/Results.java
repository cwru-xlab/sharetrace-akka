package sharetrace.analysis.model;

import com.fasterxml.jackson.annotation.JsonValue;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import java.util.Map;

public final class Results {

  private final Map<String, Object> results;

  public Results() {
    results = new Object2ReferenceOpenHashMap<>();
  }

  public Results put(String key, Object result) {
    results.put(key, result);
    return this;
  }

  public Results withScope(String scope) {
    return (Results) results.computeIfAbsent(scope, x -> new Results());
  }

  @JsonValue
  @SuppressWarnings("unused")
  private Map<String, Object> value() {
    return results;
  }
}
