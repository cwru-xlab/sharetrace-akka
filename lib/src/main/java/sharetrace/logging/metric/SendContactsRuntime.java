package sharetrace.logging.metric;

import java.time.Duration;

public record SendContactsRuntime(Duration runtime) implements RuntimeMetric {}
