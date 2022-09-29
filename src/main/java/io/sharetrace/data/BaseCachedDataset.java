package io.sharetrace.data;

import io.sharetrace.data.factory.CachedRiskScoreFactory;
import io.sharetrace.data.factory.RiskScoreFactory;
import io.sharetrace.graph.Contact;
import io.sharetrace.logging.Loggable;
import io.sharetrace.model.RiskScore;
import org.immutables.value.Value;

import java.util.Set;

@Value.Immutable
abstract class BaseCachedDataset implements Dataset {

  @Override
  public Dataset withScoreFactory(RiskScoreFactory scoreFactory) {
    return CachedDataset.of(cached().withScoreFactory(scoreFactory));
  }

  @Override
  public Dataset withNewContactNetwork() {
    return CachedDataset.of(cached().withNewContactNetwork());
  }

  @Override
  public RiskScore riskScore(int user) {
    return cachedScoreFactory().riskScore(user);
  }

  @Override
  public Set<Integer> users() {
    return cached().users();
  }

  @Override
  @Value.Lazy
  public Set<Contact> contacts() {
    return cached().contacts();
  }

  @Override
  public void logMetrics() {
    cached().logMetrics();
  }

  @Override
  public Set<Class<? extends Loggable>> loggable() {
    return cached().loggable();
  }

  @Value.Lazy
  protected RiskScoreFactory cachedScoreFactory() {
    return CachedRiskScoreFactory.of(cached());
  }

  @Value.Parameter
  protected abstract Dataset cached();
}
