package org.sharetrace.data.factory;

import org.apache.commons.math3.distribution.RealDistribution;

@FunctionalInterface
public interface DistributionFactory {

  RealDistribution distribution(long seed);
}
