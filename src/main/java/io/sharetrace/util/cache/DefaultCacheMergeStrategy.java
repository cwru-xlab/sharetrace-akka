package io.sharetrace.util.cache;

import io.sharetrace.model.TemporalScore;
import io.sharetrace.model.UserParameters;
import java.util.function.BinaryOperator;

public final class DefaultCacheMergeStrategy<T extends TemporalScore> implements BinaryOperator<T> {

  private final UserParameters parameters;

  public DefaultCacheMergeStrategy(UserParameters parameters) {
    this.parameters = parameters;
  }

  @Override
  public T apply(T oldValue, T newValue) {
    if (isHigher(newValue, oldValue)) {
      return newValue;
    } else if (isOlder(newValue, oldValue) && isApproximatelyEqual(newValue, oldValue)) {
      return newValue;
    } else {
      return oldValue;
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
