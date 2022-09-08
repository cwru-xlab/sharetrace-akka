package org.sharetrace.data;

import java.nio.file.Path;
import org.immutables.value.Value;
import org.sharetrace.data.factory.RiskScoreFactory;
import org.sharetrace.graph.ContactNetwork;
import org.sharetrace.graph.FileContactNetwork;
import org.sharetrace.message.RiskScore;
import org.sharetrace.util.TimeRef;

@SuppressWarnings("DefaultAnnotationParam")
@Value.Immutable(copy = true)
abstract class BaseFileDataset implements Dataset, TimeRef {

  @Override
  @Value.Derived
  public ContactNetwork contactNetwork() {
    return FileContactNetwork.builder()
        .addAllLoggable(loggable())
        .delimiter(delimiter())
        .path(path())
        .refTime(refTime())
        .build();
  }

  protected abstract String delimiter();

  protected abstract Path path();

  @Override
  public RiskScore riskScore(int user) {
    return scoreFactory().riskScore(user);
  }

  protected abstract RiskScoreFactory scoreFactory();
}
