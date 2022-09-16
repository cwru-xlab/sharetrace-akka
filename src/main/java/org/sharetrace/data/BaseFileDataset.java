package org.sharetrace.data;

import java.nio.file.Path;
import org.immutables.value.Value;
import org.sharetrace.data.factory.RiskScoreFactory;
import org.sharetrace.graph.ContactNetwork;
import org.sharetrace.graph.FileContactNetwork;
import org.sharetrace.model.RiskScore;
import org.sharetrace.model.TimeRef;

@SuppressWarnings("DefaultAnnotationParam")
@Value.Immutable(copy = true)
abstract class BaseFileDataset implements Dataset, TimeRef {

  @Override
  @Value.Lazy
  public ContactNetwork contactNetwork() {
    return FileContactNetwork.builder()
        .addAllLoggable(loggable())
        .delimiter(delimiter())
        .path(path())
        .refTime(refTime())
        .build();
  }

  @Override
  public RiskScore riskScore(int user) {
    return scoreFactory().riskScore(user);
  }

  protected abstract String delimiter();

  protected abstract Path path();

  protected abstract RiskScoreFactory scoreFactory();
}
