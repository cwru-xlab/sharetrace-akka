package org.sharetrace.graph;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.immutables.value.Value;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultEdge;
import org.sharetrace.data.factory.ContactTimeFactory;
import org.sharetrace.logging.Loggable;

@Value.Immutable
abstract class BaseSampledContactNetwork implements ContactNetwork {

  @Override
  public int numUsers() {
    return helper().numUsers();
  }

  @Override
  public int numContacts() {
    return helper().numContacts();
  }

  @Override
  public IntStream users() {
    return helper().users();
  }

  @Override
  public Stream<Contact> contacts() {
    return contactSet().stream();
  }

  @Override
  public void logMetrics() {
    helper().logMetrics();
  }

  @Value.Derived
  protected Set<Contact> contactSet() {
    return helper()
        .contacts(contactTimeFactory())
        .collect(ObjectOpenHashSet.toSetWithExpectedSize(numContacts()));
  }

  protected abstract ContactTimeFactory contactTimeFactory();

  @Value.Derived
  protected ContactNetworkHelper helper() {
    return ContactNetworkHelper.of(graphGenerator(), loggable());
  }

  protected abstract GraphGenerator<Integer, DefaultEdge, ?> graphGenerator();

  protected abstract Set<Class<? extends Loggable>> loggable();
}
