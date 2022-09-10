package org.sharetrace.experiment.state;

import org.sharetrace.message.RiskScoreMsg;
import org.sharetrace.model.CacheParams;

public interface DistributionFactoryContext extends CacheParamsContext {

  CacheParams<RiskScoreMsg> cacheParams();
}
