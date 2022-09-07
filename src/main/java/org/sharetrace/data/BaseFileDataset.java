package org.sharetrace.data;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Set;
import org.immutables.value.Value;
import org.sharetrace.data.factory.RiskScoreFactory;
import org.sharetrace.graph.ContactNetwork;
import org.sharetrace.graph.FileContactNetwork;
import org.sharetrace.logging.Loggable;
import org.sharetrace.message.RiskScore;

@SuppressWarnings("DefaultAnnotationParam")
@Value.Immutable(copy = true)
abstract class BaseFileDataset implements Dataset {

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

  protected abstract Set<Class<? extends Loggable>> loggable();

  protected abstract String delimiter();

  protected abstract Path path();

  protected abstract Instant refTime();

  @Override
  public RiskScore riskScore(int user) {
    return scoreFactory().riskScore(user);
  }

  protected abstract RiskScoreFactory scoreFactory();
}
