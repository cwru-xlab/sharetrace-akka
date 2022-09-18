package io.sharetrace.data;

import io.sharetrace.data.factory.RiskScoreFactory;
import io.sharetrace.graph.Contact;
import io.sharetrace.graph.ContactNetwork;
import io.sharetrace.graph.FileContactNetwork;
import io.sharetrace.model.RiskScore;
import io.sharetrace.model.TimeRef;
import java.nio.file.Path;
import java.util.Set;
import org.immutables.value.Value;

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

  public FileDataset withNewContactNetwork() {
    return FileDataset.builder()
        .addAllLoggable(loggable())
        .path(path())
        .refTime(refTime())
        .delimiter(delimiter())
        .scoreFactory(scoreFactory())
        .build();
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
