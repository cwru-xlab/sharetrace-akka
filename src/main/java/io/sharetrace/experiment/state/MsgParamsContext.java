package io.sharetrace.experiment.state;

import java.util.Map;

public interface MsgParamsContext extends MdcContext {

  Map<String, String> mdc();
}
