package org.sharetrace.experiment.state;

import org.sharetrace.message.RiskScoreMsg;
import org.sharetrace.util.CacheParams;

public interface DistributionFactoryContext extends CacheParamsContext {

  CacheParams<RiskScoreMsg> cacheParams();
}
