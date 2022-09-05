package org.sharetrace.experiment.state;

import java.util.function.Function;
import org.sharetrace.message.RiskScoreMessage;
import org.sharetrace.util.CacheParameters;

public interface CacheParametersBuilder extends RiskScoreValueBuilder {

  RiskScoreValueBuilder cacheParameters(CacheParameters<RiskScoreMessage> parameters);

  RiskScoreValueBuilder cacheParameters(
      Function<CacheParametersContext, CacheParameters<RiskScoreMessage>> factory);
}
