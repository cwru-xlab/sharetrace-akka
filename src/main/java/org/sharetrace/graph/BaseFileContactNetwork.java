package org.sharetrace.graph;

import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.immutables.value.Value;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultEdge;
import org.sharetrace.data.factory.ContactTimeFactory;
import org.sharetrace.model.LoggableRef;
import org.sharetrace.model.TimeRef;
import org.sharetrace.util.Indexer;

@Value.Immutable
abstract class BaseFileContactNetwork implements ContactNetwork, TimeRef, LoggableRef {

  private static Set<Integer> key(int user1, int user2) {
    return IntSet.of(user1, user2);
  }

  private static Instant newer(Instant time1, Instant time2) {
    return time1.isAfter(time2) ? time1 : time2;
  }

  @Override
  public Set<Integer> users() {
    return impl().users();
  }

  @Override
  public Set<Contact> contacts() {
    return impl().contacts();
  }

  @Override
  public void logMetrics() {
    impl().logMetrics();
  }

  @Value.Lazy
  protected ContactNetwork impl() {
    return ContactNetworkImpl.of(graphGenerator(), contactTimeFactory(), loggable());
  }

  @Value.Lazy
  protected Map<Set<Integer>, Instant> contactMap() {
    try (BufferedReader reader = Files.newBufferedReader(path())) {
      return toContacts(reader.lines()::iterator);
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  protected abstract Path path();

  protected abstract String delimiter();

  private GraphGenerator<Integer, DefaultEdge, ?> graphGenerator() {
    return (target, x) -> generate(target);
  }

  private void generate(Graph<Integer, DefaultEdge> target) {
    List<Integer> users;
    for (Set<Integer> contact : contactMap().keySet()) {
      users = List.copyOf(contact);
      Graphs.addEdgeWithVertices(target, users.get(0), users.get(1));
    }
  }

  private Map<Set<Integer>, Instant> toContacts(Iterable<String> lines) {
    Instant lastContactTime = Instant.MIN;
    Indexer<String> indexer = new Indexer<>();
    Map<Set<Integer>, Instant> contacts = new Object2ObjectOpenHashMap<>();
    for (String line : lines) {
      String[] args = line.split(delimiter());
      int user1 = indexer.index(args[1].strip());
      int user2 = indexer.index(args[2].strip());
      if (user1 != user2) {
        Instant time = Instant.ofEpochSecond(Long.parseLong(args[0].strip()));
        contacts.merge(key(user1, user2), time, BaseFileContactNetwork::newer);
        lastContactTime = newer(lastContactTime, time);
      }
    }
    Duration offset = Duration.between(lastContactTime, refTime());
    contacts.replaceAll((x, time) -> time.plus(offset));
    return contacts;
  }

  private ContactTimeFactory contactTimeFactory() {
    return (user1, user2) -> contactMap().get(key(user1, user2));
  }
}
