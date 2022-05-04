package org.sharetrace.logging;

import org.immutables.value.Value;

@Value.Immutable
interface BaseCurrentRefreshEvent extends ScoreChangeEvent {

  String of();
}
