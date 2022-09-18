package io.sharetrace.data;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import io.sharetrace.data.factory.RiskScoreFactory;
import io.sharetrace.graph.Contact;
import io.sharetrace.graph.ContactNetwork;
import io.sharetrace.model.RiskScore;
import java.util.Set;

@JsonIgnoreType
abstract class AbstractDataset implements Dataset {

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

  public abstract AbstractDataset withNewContactNetwork();

  protected abstract RiskScoreFactory scoreFactory();

  protected abstract ContactNetwork contactNetwork();
}
