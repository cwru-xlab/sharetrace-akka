package sharetrace.analysis.collector;

import java.util.HashMap;
import java.util.Map;

public final class MapResultsCollector implements ResultsCollector {

  private final Map<String, Object> results;
  private final String delimiter;
  private final String prefix;

  private MapResultsCollector(Map<String, Object> results, String prefix, String delimiter) {
    this.results = results;
    this.delimiter = delimiter;
    this.prefix = prefix;
  }

  public MapResultsCollector(String delimiter) {
    this(new HashMap<>(), "", delimiter);
  }

  @Override
  public ResultsCollector put(String key, Object result) {
    results.put(String.join(delimiter, prefix, key), result);
    return this;
  }

  @Override
  public ResultsCollector withPrefix(String prefix) {
    return new MapResultsCollector(results, prefix, delimiter);
  }

  @Override
  public String toString() {
    return results.toString();
  }
}
