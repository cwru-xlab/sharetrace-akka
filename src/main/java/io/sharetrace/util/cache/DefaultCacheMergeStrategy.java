package io.sharetrace.util.cache;

import io.sharetrace.model.TemporalProbability;
import io.sharetrace.model.UserParams;
import java.util.function.BinaryOperator;

public final class DefaultCacheMergeStrategy<T extends TemporalProbability>
    implements BinaryOperator<T> {

  private final UserParams params;

  public DefaultCacheMergeStrategy(UserParams params) {
    this.params = params;
  }

  @Override
  public T apply(T oldValue, T newValue) {
    if (isHigher(newValue, oldValue)) {
      return newValue;
    } else if (isOlder(newValue, oldValue) && isApproxEqual(newValue, oldValue)) {
      return newValue;
    } else {
      return oldValue;
    }
  }

  private boolean isHigher(T left, T right) {
    return left.value() > right.value();
  }

  private boolean isOlder(T left, T right) {
    return left.time().isBefore(right.time());
  }

  private boolean isApproxEqual(T left, T right) {
    return Math.abs(left.value() - right.value()) < params.tolerance();
  }
}
