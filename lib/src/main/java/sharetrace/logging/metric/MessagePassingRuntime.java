package sharetrace.logging.metric;

import java.time.Duration;

public record MessagePassingRuntime(Duration runtime) implements RuntimeMetric {}
