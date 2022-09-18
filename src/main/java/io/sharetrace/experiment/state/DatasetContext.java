package io.sharetrace.experiment.state;

import org.apache.commons.math3.distribution.RealDistribution;

public interface DatasetContext extends DistributionFactoryContext {

  RealDistribution scoreValues();

  RealDistribution scoreTimes();

  RealDistribution contactTimes();
}
