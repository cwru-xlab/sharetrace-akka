package org.sharetrace.logging;

import org.immutables.value.Value;

@Value.Immutable
interface BaseCurrentRefreshEvent extends ScoreChangeEvent {

  @Override
  @Value.Derived
  default String name() {
    return getClass().getSimpleName();
  }

  String of();
}
