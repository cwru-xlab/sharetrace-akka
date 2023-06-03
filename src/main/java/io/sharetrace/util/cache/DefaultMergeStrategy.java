package io.sharetrace.util.cache;

import io.sharetrace.model.TemporalScore;
import io.sharetrace.model.UserParameters;
import java.util.function.BinaryOperator;

public final class DefaultMergeStrategy<T extends TemporalScore> implements BinaryOperator<T> {

  private final UserParameters parameters;

  public DefaultMergeStrategy(UserParameters parameters) {
    this.parameters = parameters;
  }

  @Override
  public T apply(T oldScore, T newScore) {
    if (isHigher(newScore, oldScore)) {
      return newScore;
    } else if (isOlder(newScore, oldScore) && isApproximatelyEqual(newScore, oldScore)) {
      return newScore;
    } else {
      return oldScore;
    }
  }

  private boolean isHigher(T left, T right) {
    return left.value() > right.value();
  }

  private boolean isOlder(T left, T right) {
    return left.timestamp().isBefore(right.timestamp());
  }

  private boolean isApproximatelyEqual(T left, T right) {
    return Math.abs(left.value() - right.value()) < parameters.tolerance();
  }
}
