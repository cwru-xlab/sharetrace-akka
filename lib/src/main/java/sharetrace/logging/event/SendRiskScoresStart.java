package sharetrace.logging.event;

import sharetrace.model.Timestamp;

public record SendRiskScoresStart(Timestamp timestamp) implements RiskPropagationEvent {}
