package sharetrace.logging.event.user;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("L")
public record LastEvent(int self, long timestamp) implements UserEvent {}
