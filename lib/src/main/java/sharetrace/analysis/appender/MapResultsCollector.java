package sharetrace.analysis.appender;

import java.util.HashMap;
import java.util.Map;
import sharetrace.analysis.model.Result;

public final class MapResultsCollector implements ResultsCollector {

  private final Map<String, Object> results;

  public MapResultsCollector() {
    this.results = new HashMap<>();
  }

  @Override
  public ResultsCollector add(Result<?> result) {
    results.put(result.key(), result);
    return this;
  }

  @Override
  public ResultsCollector withHandlerKey(String key) {
    return this;
  }

  @Override
  public String toString() {
    return results.toString();
  }
}
