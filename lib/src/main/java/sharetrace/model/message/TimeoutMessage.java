package sharetrace.model.message;

public record TimeoutMessage(int key) implements UserMessage, MonitorMessage {}
