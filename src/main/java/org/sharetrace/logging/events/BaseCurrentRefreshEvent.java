package org.sharetrace.logging.events;

import org.immutables.value.Value;

@Value.Immutable
interface BaseCurrentRefreshEvent extends ScoreChangeEvent {

  String of();
}
