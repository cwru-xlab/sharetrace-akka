package org.sharetrace.data.sampling;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.stream.IntStream;
import org.apache.commons.math3.distribution.IntegerDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.exception.NumberIsTooLargeException;
import org.apache.commons.math3.exception.OutOfRangeException;
import org.immutables.value.Value;
import org.sharetrace.util.Checks;

@Value.Immutable
abstract class BaseRealMixtureDistribution implements RealDistribution {

  private static final String LOWER_SUPPORT_BOUND_MSG =
      "'componentDistribution' must have a lower support bound of 0; got %s";

  private static final String UPPER_SUPPORT_BOUND_MSG =
      "'componentDistribution' must have an upper support bound of %s; got %s";

  @Override
  public double probability(double x) {
    return weightedValue(d -> d.probability(x));
  }

  @Override
  public double density(double x) {
    return weightedValue(d -> d.density(x));
  }

  @Override
  public double cumulativeProbability(double x) {
    return weightedValue(d -> d.cumulativeProbability(x));
  }

  @Override
  @Deprecated
  public double cumulativeProbability(double x0, double x1) throws NumberIsTooLargeException {
    return weightedValue(d -> d.cumulativeProbability(x0, x1));
  }

  @Override
  public double inverseCumulativeProbability(double p) throws OutOfRangeException {
    return weightedValue(d -> d.inverseCumulativeProbability(p));
  }

  @Override
  public double getNumericalMean() {
    return weightedValue(RealDistribution::getNumericalMean);
  }

  @Override
  public double getNumericalVariance() {
    return weightedValue(RealDistribution::getNumericalVariance);
  }

  @Override
  public double getSupportLowerBound() {
    return components().stream()
        .map(RealDistribution::getSupportLowerBound)
        .min(Double::compare)
        .orElse(Double.NEGATIVE_INFINITY);
  }

  @Override
  public double getSupportUpperBound() {
    return components().stream()
        .map(RealDistribution::getSupportUpperBound)
        .max(Double::compare)
        .orElse(Double.POSITIVE_INFINITY);
  }

  @Override
  @Deprecated
  public boolean isSupportLowerBoundInclusive() {
    return allMatch(RealDistribution::isSupportLowerBoundInclusive);
  }

  @Override
  @Deprecated
  public boolean isSupportUpperBoundInclusive() {
    return allMatch(RealDistribution::isSupportUpperBoundInclusive);
  }

  @Override
  public boolean isSupportConnected() {
    return allMatch(RealDistribution::isSupportConnected);
  }

  @Override
  public void reseedRandomGenerator(long seed) {
    components().forEach(d -> d.reseedRandomGenerator(seed));
    componentDistribution().reseedRandomGenerator(seed);
  }

  @Override
  public double sample() {
    int i = componentDistribution().sample();
    return components().get(i).sample();
  }

  @Override
  public double[] sample(int sampleSize) {
    return IntStream.rangeClosed(1, sampleSize).mapToDouble(x -> sample()).toArray();
  }

  private boolean allMatch(Predicate<RealDistribution> predicate) {
    return components().stream().allMatch(predicate);
  }

  private double weightedValue(ToDoubleFunction<RealDistribution> getValue) {
    return IntStream.rangeClosed(1, components().size())
        .mapToDouble(i -> weightedComponentValue(getValue, i))
        .sum();
  }

  protected abstract List<RealDistribution> components();

  private double weightedComponentValue(ToDoubleFunction<RealDistribution> getValue, int i) {
    double componentValue = getValue.applyAsDouble(components().get(i));
    double componentProbability = componentDistribution().probability(i);
    return componentValue * componentProbability;
  }

  protected abstract IntegerDistribution componentDistribution();

  @Value.Check
  protected void check() {
    checkLowerBound();
    checkUpperBound();
  }

  private void checkLowerBound() {
    int lowerBound = componentDistribution().getSupportLowerBound();
    Checks.checkArgument(lowerBound == 0, LOWER_SUPPORT_BOUND_MSG, lowerBound);
  }

  private void checkUpperBound() {
    int nComponents = components().size();
    int upperBound = componentDistribution().getSupportUpperBound();
    Checks.checkArgument(
        nComponents == upperBound - 1, UPPER_SUPPORT_BOUND_MSG, nComponents, upperBound);
  }
}
