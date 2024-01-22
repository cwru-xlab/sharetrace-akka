package sharetrace.logging.event.user;

public record ContactEvent(int self, int contact, long contactTime, long timestamp)
    implements UserEvent {}
