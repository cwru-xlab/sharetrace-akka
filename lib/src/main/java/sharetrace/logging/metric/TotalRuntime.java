package sharetrace.logging.metric;

import java.time.Duration;

public record TotalRuntime(Duration runtime) implements RuntimeMetric {}
