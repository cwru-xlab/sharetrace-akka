package sharetrace.logging.event.lifecycle;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("CUE")
public record CreateUsersEnd(long timestamp) implements LifecycleEvent {}
