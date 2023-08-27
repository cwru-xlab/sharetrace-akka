package sharetrace.analysis.collector;

public interface ResultsCollector {

  ResultsCollector put(String key, Object result);

  ResultsCollector withPrefix(String prefix);
}
