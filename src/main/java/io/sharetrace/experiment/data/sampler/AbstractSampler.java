package io.sharetrace.experiment.data.sampler;

import org.apache.commons.math3.distribution.RealDistribution;

abstract class AbstractSampler<T> implements Sampler<T> {

    protected static double normalizedSample(RealDistribution distribution, double multiplier) {
        return normalizedSample(distribution) * multiplier;
    }

    protected static double normalizedSample(RealDistribution distribution) {
        double max = Math.min(Double.MAX_VALUE, distribution.getSupportUpperBound());
        double min = Math.max(Double.MIN_VALUE, distribution.getSupportLowerBound());
        double sample = Math.max(Double.MIN_VALUE, Math.min(Double.MAX_VALUE, distribution.sample()));
        return (sample - min) / (max - min);
    }
}
