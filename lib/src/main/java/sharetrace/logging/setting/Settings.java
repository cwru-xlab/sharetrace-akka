package sharetrace.logging.setting;

import sharetrace.logging.LogRecord;

public interface Settings extends LogRecord {

  static String key() {
    return "setting";
  }
}
