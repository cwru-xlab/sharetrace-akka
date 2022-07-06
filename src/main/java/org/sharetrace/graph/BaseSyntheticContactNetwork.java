package org.sharetrace.graph;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.time.Instant;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.immutables.value.Value;
import org.jgrapht.generate.GraphGenerator;
import org.sharetrace.data.factory.ContactTimeFactory;
import org.sharetrace.logging.Loggable;

@Value.Immutable
abstract class BaseSyntheticContactNetwork implements ContactNetwork {

  @Override
  public int nUsers() {
    return helper().nUsers();
  }

  @Override
  public int nContacts() {
    return helper().nContacts();
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
  protected ContactNetworkHelper helper() {
    return ContactNetworkHelper.create(graphGenerator(), loggable());
  }

  protected abstract GraphGenerator<Integer, Edge<Integer>, ?> graphGenerator();

  protected abstract Set<Class<? extends Loggable>> loggable();

  @Value.Derived
  protected Set<Contact> contactSet() {
    return helper().contactNetwork().edgeSet().stream()
        .map(this::toContact)
        .collect(ObjectOpenHashSet.toSetWithExpectedSize(nContacts()));
  }

  protected abstract ContactTimeFactory contactTimeFactory();

  private Contact toContact(Edge<Integer> edge) {
    int user1 = edge.source();
    int user2 = edge.target();
    Instant timestamp = contactTimeFactory().getContactTime(user1, user2);
    return helper().toContact(edge, timestamp);
  }
}
