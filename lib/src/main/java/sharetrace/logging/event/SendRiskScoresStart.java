package sharetrace.logging.event;

import java.time.Instant;

public record SendRiskScoresStart(Instant timestamp) implements RiskPropagationEvent {}
