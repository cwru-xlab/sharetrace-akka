package sharetrace.logging;

import sharetrace.Buildable;
import sharetrace.graph.ContactNetwork;
import sharetrace.model.Context;
import sharetrace.model.Parameters;

@Buildable
public record ExecutionProperties(
    Context context, Parameters parameters, ContactNetwork contactNetwork) implements LogRecord {}
