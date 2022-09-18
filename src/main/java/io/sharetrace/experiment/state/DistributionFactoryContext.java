package io.sharetrace.experiment.state;

import io.sharetrace.message.RiskScoreMsg;
import io.sharetrace.model.CacheParams;

public interface DistributionFactoryContext extends CacheParamsContext {

  CacheParams<RiskScoreMsg> cacheParams();
}
