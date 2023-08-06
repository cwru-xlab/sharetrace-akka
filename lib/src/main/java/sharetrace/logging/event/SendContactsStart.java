package sharetrace.logging.event;

import java.time.Instant;

public record SendContactsStart(Instant timestamp) implements RiskPropagationEvent {}
