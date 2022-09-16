package org.sharetrace.experiment.state;

import org.apache.commons.math3.distribution.RealDistribution;

public interface DataFactoryContext extends DistributionFactoryContext {

  RealDistribution scoreValues();

  RealDistribution scoreTimes();

  RealDistribution contactTimes();
}
