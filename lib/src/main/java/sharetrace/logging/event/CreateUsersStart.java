package sharetrace.logging.event;

import java.time.Instant;

public record CreateUsersStart(Instant timestamp) implements RiskPropagationEvent {}
