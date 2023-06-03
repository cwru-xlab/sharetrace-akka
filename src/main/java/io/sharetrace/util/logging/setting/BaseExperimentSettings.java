package io.sharetrace.util.logging.setting;

import io.sharetrace.experiment.Context;
import io.sharetrace.model.UserParameters;
import io.sharetrace.util.cache.CacheParameters;
import org.immutables.value.Value;

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
