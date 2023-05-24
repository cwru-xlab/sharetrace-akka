package io.sharetrace.experiment.state;

import io.sharetrace.model.message.RiskScoreMessage;
import io.sharetrace.util.cache.CacheParameters;

public interface DistributionFactoryContext extends CacheParametersContext {

  CacheParameters<RiskScoreMessage> cacheParameters();
}
