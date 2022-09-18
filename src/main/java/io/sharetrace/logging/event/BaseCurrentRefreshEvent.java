package io.sharetrace.logging.event;

import org.immutables.value.Value;

@Value.Immutable
interface BaseCurrentRefreshEvent extends ScoreChangeEvent {

  String user();
}
