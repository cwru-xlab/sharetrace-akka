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

@Value.Immutable
abstract class BaseFileDataset implements Dataset {

  @Override
  public RiskScore getRiskScore(int user) {
    return riskScoreFactory().getRiskScore(user);
  }

  protected abstract RiskScoreFactory riskScoreFactory();

  @Override
  @Value.Derived
  public ContactNetwork getContactNetwork() {
    return FileContactNetwork.builder()
        .addAllLoggable(loggable())
        .delimiter(delimiter())
        .path(path())
        .referenceTime(referenceTime())
        .build();
  }

  protected abstract Set<Class<? extends Loggable>> loggable();

  protected abstract String delimiter();

  protected abstract Path path();

  protected abstract Instant referenceTime();
}
