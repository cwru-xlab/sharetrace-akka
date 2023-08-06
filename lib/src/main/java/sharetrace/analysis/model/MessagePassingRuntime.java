package sharetrace.analysis.model;

import java.time.Duration;

public record MessagePassingRuntime(Duration value) implements Runtime {}
