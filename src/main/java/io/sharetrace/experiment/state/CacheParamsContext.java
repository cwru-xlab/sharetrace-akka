package io.sharetrace.experiment.state;

import java.util.Map;

public interface CacheParamsContext extends MdcContext {

  Map<String, String> mdc();
}
