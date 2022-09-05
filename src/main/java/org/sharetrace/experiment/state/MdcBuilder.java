package org.sharetrace.experiment.state;

import java.util.Map;
import java.util.function.Function;

public interface MdcBuilder extends MessageParametersBuilder {

  MessageParametersBuilder mdc(Map<String, String> mdc);

  MessageParametersBuilder mdc(Function<MdcContext, Map<String, String>> factory);
}
