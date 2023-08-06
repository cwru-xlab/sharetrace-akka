package sharetrace.logging.event;

import java.time.Instant;

public record SendContactsEnd(Instant timestamp) implements RiskPropagationEvent {}
