package io.sharetrace.experiment.data.sampler;

import io.sharetrace.model.RiskScore;

import java.time.Instant;

import org.apache.commons.math3.distribution.RealDistribution;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseRiskScoreSampler extends AbstractSampler<RiskScore> {

    @Override
    public RiskScore sample() {
        return RiskScore.builder()
                .value((float) normalizedSample(values(), RiskScore.RANGE))
                .time(timeSampler().sample())
                .build();
    }

    protected abstract RealDistribution values();

    protected abstract Sampler<Instant> timeSampler();
}
