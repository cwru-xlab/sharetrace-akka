package io.sharetrace.util.cache;

import io.sharetrace.model.UserParams;
import io.sharetrace.model.message.RiskScoreMsg;
import java.util.function.BinaryOperator;

public final class DefaultCacheMergeStrategy implements BinaryOperator<RiskScoreMsg> {

  private final UserParams params;

  public DefaultCacheMergeStrategy(UserParams params) {
    this.params = params;
  }

  @Override
  public RiskScoreMsg apply(RiskScoreMsg oldValue, RiskScoreMsg newValue) {
    if (isHigher(newValue, oldValue)) {
      return newValue;
    } else if (isOlder(newValue, oldValue) && isApproxEqual(newValue, oldValue)) {
      return newValue;
    } else {
      return oldValue;
    }
  }

  private boolean isHigher(RiskScoreMsg left, RiskScoreMsg right) {
    return left.value() > right.value();
  }

  private boolean isOlder(RiskScoreMsg left, RiskScoreMsg right) {
    return left.time().isBefore(right.time());
  }

  private boolean isApproxEqual(RiskScoreMsg left, RiskScoreMsg right) {
    return Math.abs(left.value() - right.value()) < params.tolerance();
  }
}
