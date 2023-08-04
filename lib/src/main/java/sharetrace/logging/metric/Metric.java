package sharetrace.logging.metric;

import sharetrace.logging.LogRecord;

public interface Metric extends LogRecord {

  static String key() {
    return "metric";
  }
}
