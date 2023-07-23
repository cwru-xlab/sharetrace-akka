package sharetrace.logging.metric;

import java.time.Duration;

public record SendRiskScoresRuntime(Duration runtime) implements RuntimeMetric {}
