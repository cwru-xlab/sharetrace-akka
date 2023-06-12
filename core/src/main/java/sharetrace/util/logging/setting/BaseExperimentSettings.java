package sharetrace.util.logging.setting;

import org.immutables.value.Value;
import sharetrace.experiment.Context;
import sharetrace.model.UserParameters;
import sharetrace.util.cache.CacheParameters;

@Value.Immutable
@SuppressWarnings({"immutables", "immutables:from"})
interface BaseExperimentSettings extends SettingsRecord {

  UserParameters userParameters();

  CacheParameters<?> cacheParameters();

  Context context();

  String stateId();

  String networkId();

  String networkType();
}
