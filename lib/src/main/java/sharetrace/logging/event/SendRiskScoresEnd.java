package sharetrace.logging.event;

import java.time.Instant;

public record SendRiskScoresEnd(Instant timestamp) implements RiskPropagationEvent {}
