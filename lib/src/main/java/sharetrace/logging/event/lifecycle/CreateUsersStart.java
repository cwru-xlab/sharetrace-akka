package sharetrace.logging.event.lifecycle;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("CUS")
public record CreateUsersStart(long timestamp) implements LifecycleEvent {}
