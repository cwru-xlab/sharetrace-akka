package sharetrace.logging.event;

import sharetrace.model.Timestamp;

public record SendContactsStart(Timestamp timestamp) implements RiskPropagationEvent {}
