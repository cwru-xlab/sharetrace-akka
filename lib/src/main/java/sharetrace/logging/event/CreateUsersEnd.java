package sharetrace.logging.event;

import java.time.Instant;

public record CreateUsersEnd(Instant timestamp) implements RiskPropagationEvent {}
