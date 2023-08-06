package sharetrace.logging.event;

import java.time.Instant;

public record RiskPropagationStart(Instant timestamp) implements RiskPropagationEvent {}
