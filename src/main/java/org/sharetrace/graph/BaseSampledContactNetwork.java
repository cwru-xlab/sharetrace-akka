package org.sharetrace.graph;

import java.util.Set;
import org.immutables.value.Value;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultEdge;
import org.sharetrace.data.factory.ContactTimeFactory;
import org.sharetrace.model.LoggableRef;

@Value.Immutable
abstract class BaseSampledContactNetwork implements ContactNetwork, LoggableRef {

  @Override
  public Set<Integer> users() {
    return helper().users();
  }

  @Override
  @Value.Lazy
  public Set<Contact> contacts() {
    return helper().contacts();
  }

  @Override
  public void logMetrics() {
    helper().logMetrics();
  }

  protected abstract ContactTimeFactory contactTimeFactory();

  @Value.Lazy
  protected ContactNetworkHelper helper() {
    return ContactNetworkHelper.of(graphGenerator(), contactTimeFactory(), loggable());
  }

  protected abstract GraphGenerator<Integer, DefaultEdge, ?> graphGenerator();
}
