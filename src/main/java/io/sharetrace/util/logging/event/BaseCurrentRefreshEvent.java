package io.sharetrace.util.logging.event;

import org.immutables.value.Value;

@Value.Immutable
interface BaseCurrentRefreshEvent extends ScoreChangeEvent {

  String user();
}
