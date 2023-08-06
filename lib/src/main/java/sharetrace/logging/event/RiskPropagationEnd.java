package sharetrace.logging.event;

import java.time.Instant;

public record RiskPropagationEnd(Instant timestamp) implements RiskPropagationEvent {}
