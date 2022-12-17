package io.sharetrace.experiment.data.factory;

import org.apache.commons.math3.distribution.RealDistribution;

@FunctionalInterface
public interface DistributionFactory {

    RealDistribution get(long seed);
}
