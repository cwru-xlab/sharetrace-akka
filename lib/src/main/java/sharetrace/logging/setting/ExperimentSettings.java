package sharetrace.logging.setting;

import sharetrace.Buildable;
import sharetrace.model.Parameters;
import sharetrace.util.Context;

@Buildable
public record ExperimentSettings(
    Parameters parameters, Context context, String networkId, String networkType)
    implements SettingsRecord {}
