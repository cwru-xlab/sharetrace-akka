package sharetrace.logging.event;

import sharetrace.model.Timestamp;

public record SendContactsEnd(Timestamp timestamp) implements RiskPropagationEvent {}
