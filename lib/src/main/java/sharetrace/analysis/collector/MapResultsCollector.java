package sharetrace.analysis.collector;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public record MapResultsCollector(Map<String, Object> results, String scope, String delimiter)
    implements ResultsCollector {

  public MapResultsCollector(String delimiter) {
    this(new HashMap<>(), null, delimiter);
  }

  @Override
  public ResultsCollector put(String key, Object result) {
    results.put(join(scope, key), result);
    return this;
  }

  @Override
  public ResultsCollector withScope(String scope) {
    return new MapResultsCollector(results, join(this.scope, scope), delimiter);
  }

  @Override
  @JsonValue
  public Map<String, Object> results() {
    return Collections.unmodifiableMap(results);
  }

  private String join(String... elements) {
    return Arrays.stream(elements).filter(Objects::nonNull).collect(Collectors.joining(delimiter));
  }
}
