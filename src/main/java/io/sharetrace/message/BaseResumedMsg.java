package io.sharetrace.message;

import org.immutables.value.Value;

@Value.Immutable
interface BaseResumedMsg extends AlgorithmMsg {

  @Value.Parameter
  int user();
}
