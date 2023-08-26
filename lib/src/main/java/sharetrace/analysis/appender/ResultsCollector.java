package sharetrace.analysis.appender;

/*
ResultsAppenderBuilder

- append(key, result) operations can be handled in multiple ways
  - No grouping: put all appends in the same place
  - Group by key: put all results of the same key in the same place
  - Group by appender: pull all results belonging to the same appender in the same place

- The 'key' may correspond to a filename path or a JSON key
  - Decide on the "layout" or "strategy"
 */

import sharetrace.analysis.model.Result;

public interface ResultsCollector {

  ResultsCollector withHandlerKey(String key);

  ResultsCollector add(Result<?> result);
}
