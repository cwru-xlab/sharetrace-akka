package sharetrace.logging.setting;

import sharetrace.Buildable;
import sharetrace.graph.ContactNetwork;
import sharetrace.model.Parameters;
import sharetrace.util.Context;

@Buildable
public record ExperimentSettings(
    Parameters parameters, Context context, ContactNetwork contactNetwork) implements Settings {}
