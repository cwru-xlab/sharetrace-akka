package io.sharetrace.experiment.state;

import io.sharetrace.model.MsgParams;

public interface CacheParamsContext extends MsgParamsContext {

  MsgParams msgParams();
}
