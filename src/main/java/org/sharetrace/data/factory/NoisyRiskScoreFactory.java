package org.sharetrace.data.factory;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Map;
import org.apache.commons.math3.distribution.RealDistribution;
import org.sharetrace.model.RiskScore;

public final class NoisyRiskScoreFactory implements RiskScoreFactory {

  private final RealDistribution noise;
  private final RiskScoreFactory scoreFactory;
  private final Map<Integer, RiskScore> clean;
  private final Map<Integer, RiskScore> noisy;

  private NoisyRiskScoreFactory(
      RealDistribution noise,
      RiskScoreFactory scoreFactory,
      Map<Integer, RiskScore> original,
      Map<Integer, RiskScore> noisy) {
    this.noise = noise;
    this.scoreFactory = scoreFactory;
    this.clean = original;
    this.noisy = noisy;
  }

  public static NoisyRiskScoreFactory of(RealDistribution noise, RiskScoreFactory scoreFactory) {
    return new NoisyRiskScoreFactory(noise, scoreFactory, newScoreMap(), newScoreMap());
  }

  private static Map<Integer, RiskScore> newScoreMap() {
    return new Int2ObjectOpenHashMap<>();
  }

  public NoisyRiskScoreFactory withNoise(RealDistribution noise) {
    // Only invalidate the noisy risk scores.
    return new NoisyRiskScoreFactory(noise, scoreFactory, clean, newScoreMap());
  }

  public NoisyRiskScoreFactory withScoreFactory(RiskScoreFactory scoreFactory) {
    // Invalidate both the original and the noisy risk scores.
    return of(noise, scoreFactory);
  }

  @Override
  public RiskScore riskScore(int user) {
    return noisy.computeIfAbsent(user, this::computeNoisyScore);
  }

  private RiskScore computeNoisyScore(int user) {
    RiskScore score = clean.computeIfAbsent(user, scoreFactory::riskScore);
    return score.withValue((float) (score.value() + noise.sample()));
  }
}
