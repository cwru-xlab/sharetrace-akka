package sharetrace.logging;

import sharetrace.Buildable;
import sharetrace.graph.ContactNetwork;
import sharetrace.model.Parameters;
import sharetrace.util.Context;

@Buildable
public record Settings(Context context, Parameters parameters, ContactNetwork contactNetwork)
    implements LogRecord {}
