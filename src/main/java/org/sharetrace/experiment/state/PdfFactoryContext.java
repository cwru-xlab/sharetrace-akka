package org.sharetrace.experiment.state;

import org.sharetrace.message.RiskScoreMessage;
import org.sharetrace.util.CacheParameters;

public interface PdfFactoryContext extends CacheParametersContext {

  CacheParameters<RiskScoreMessage> cacheParameters();
}
