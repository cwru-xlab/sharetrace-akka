package org.sharetrace.data;

import java.nio.file.Path;
import java.util.Set;
import org.immutables.value.Value;
import org.sharetrace.data.factory.RiskScoreFactory;
import org.sharetrace.graph.Contact;
import org.sharetrace.graph.ContactNetwork;
import org.sharetrace.graph.FileContactNetwork;
import org.sharetrace.model.RiskScore;
import org.sharetrace.model.TimeRef;

@SuppressWarnings("DefaultAnnotationParam")
@Value.Immutable(copy = true)
abstract class BaseFileDataset implements Dataset, TimeRef {

  @Override
  public RiskScore riskScore(int user) {
    return scoreFactory().riskScore(user);
  }

  @Override
  public Set<Integer> users() {
    return contactNetwork().users();
  }

  @Override
  public Set<Contact> contacts() {
    return contactNetwork().contacts();
  }

  @Override
  public void logMetrics() {
    contactNetwork().logMetrics();
  }

  @Value.Default // Allows the contact network to be passed on to a copied instance.
  protected ContactNetwork contactNetwork() {
    return FileContactNetwork.builder()
        .addAllLoggable(loggable())
        .delimiter(delimiter())
        .path(path())
        .refTime(refTime())
        .build();
  }

  protected abstract String delimiter();

  protected abstract Path path();

  protected abstract RiskScoreFactory scoreFactory();
}
