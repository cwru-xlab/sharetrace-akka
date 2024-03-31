package sharetrace.logging.event.user;

public record LastEvent(int self, long timestamp) implements UserEvent {}
