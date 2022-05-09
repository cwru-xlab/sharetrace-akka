package org.sharetrace.data;

import java.time.Duration;
import java.time.Instant;
import org.apache.commons.math3.distribution.RealDistribution;
import org.sharetrace.message.RiskScore;

public final class DataSamplers {

  private DataSamplers() {}

  public static Instant contactTime(
      Instant referenceTime, RealDistribution ttlDistribution, Duration ttl) {
    return sampledTimestamp(referenceTime, ttlDistribution, ttl);
  }

  private static Instant sampledTimestamp(
      Instant referenceTime, RealDistribution ttlDistribution, Duration ttl) {
    double max = ttlDistribution.getSupportUpperBound();
    double min = ttlDistribution.getSupportLowerBound();
    double normalizedSample = (ttlDistribution.sample() - min) / (max - min);
    long lookBack = Math.round(normalizedSample * ttl.getSeconds());
    return referenceTime.minus(Duration.ofSeconds(lookBack));
  }

  public static RiskScore riskScore(
      RealDistribution valueDistribution,
      RealDistribution ttlDistribution,
      Instant referenceTime,
      Duration ttl) {
    return RiskScore.builder()
        .value(valueDistribution.sample())
        .timestamp(sampledTimestamp(referenceTime, ttlDistribution, ttl))
        .build();
  }
}
