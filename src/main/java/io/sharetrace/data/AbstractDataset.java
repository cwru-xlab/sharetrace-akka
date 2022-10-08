package io.sharetrace.data;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import io.sharetrace.data.factory.RiskScoreFactory;
import io.sharetrace.graph.Contact;
import io.sharetrace.graph.ContactNetwork;
import io.sharetrace.model.RiskScore;
import io.sharetrace.util.Uid;
import java.util.Set;
import org.immutables.value.Value;

@JsonIgnoreType
abstract class AbstractDataset implements Dataset {

  @Override
  public RiskScore riskScore(int user) {
    return scoreFactory().riskScore(user);
  }

  protected abstract RiskScoreFactory scoreFactory();

  @Value.Derived
  public String datasetId() {
    return Uid.ofIntString();
  }

  @Override
  public String networkId() {
    return contactNetwork().networkId();
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

  protected abstract ContactNetwork contactNetwork();
}
