package org.sharetrace.experiment.state;

import org.sharetrace.data.factory.ContactTimeFactory;
import org.sharetrace.data.factory.RiskScoreFactory;

public interface DatasetContext extends CacheParametersContext {

  RiskScoreFactory riskScoreFactory();

  ContactTimeFactory contactTimeFactory();
}
