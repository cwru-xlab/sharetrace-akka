package sharetrace.analysis.model;

import java.time.Duration;

public enum UnknownRuntime implements Runtime {
  INSTANCE;

  @Override
  public Duration value() {
    return Duration.ZERO;
  }
}
