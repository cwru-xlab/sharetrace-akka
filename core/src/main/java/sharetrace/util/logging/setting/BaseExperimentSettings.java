package sharetrace.util.logging.setting;

import org.immutables.value.Value;
import sharetrace.experiment.Context;
import sharetrace.model.Parameters;

@Value.Immutable
@SuppressWarnings({"immutables", "immutables:from"})
interface BaseExperimentSettings extends SettingsRecord {

  Parameters parameters();

  Context context();

  String stateId();

  String networkId();

  String networkType();
}
