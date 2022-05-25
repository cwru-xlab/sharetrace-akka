package org.sharetrace.util;

import java.time.Duration;
import org.immutables.value.Value;

@Value.Immutable
interface BaseCacheParameters {

  int nIntervals();

  int nLookAhead();

  Duration interval();

  Duration refreshPeriod();
}
