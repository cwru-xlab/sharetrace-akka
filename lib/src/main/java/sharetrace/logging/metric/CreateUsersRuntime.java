package sharetrace.logging.metric;

import java.time.Duration;

public record CreateUsersRuntime(Duration runtime) implements RuntimeMetric {}
