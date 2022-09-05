package org.sharetrace.experiment.state;

import java.util.Map;

public interface MessageParametersContext extends MdcContext {

  Map<String, String> mdc();
}
