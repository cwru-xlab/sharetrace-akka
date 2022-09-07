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
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.immutables.value.Value;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultEdge;
import org.sharetrace.data.factory.ContactTimeFactory;
import org.sharetrace.logging.Loggable;
import org.sharetrace.util.Indexer;
import org.sharetrace.util.Iterables;

@Value.Immutable
abstract class BaseFileContactNetwork implements ContactNetwork {

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
    return helper().contacts(contactTimeFactory());
  }

  @Override
  public void logMetrics() {
    helper().logMetrics();
  }

  @Value.Derived
  protected ContactNetworkHelper helper() {
    return ContactNetworkHelper.of(graphGenerator(), loggable());
  }

  private GraphGenerator<Integer, DefaultEdge, Integer> graphGenerator() {
    return (target, x) -> generate(target);
  }

  private void generate(Graph<Integer, DefaultEdge> target) {
    contactMap().keySet().stream()
        .map(List::copyOf)
        .forEach(users -> Graphs.addEdgeWithVertices(target, users.get(0), users.get(1)));
  }

  @Value.Derived
  protected Map<Set<Integer>, Instant> contactMap() {
    LastContactTime lastContactTime = new LastContactTime();
    Map<Set<Integer>, Instant> contactMap = new Object2ObjectOpenHashMap<>();
    IdIndexer indexer = new IdIndexer();
    try (BufferedReader reader = Files.newBufferedReader(path())) {
      for (String line : Iterables.fromStream(reader.lines())) {
        String[] args = line.split(delimiter());
        int user1 = parseAndIndexUser(args[1], indexer);
        int user2 = parseAndIndexUser(args[2], indexer);
        if (user1 != user2) {
          Instant time = parseTime(args[0]);
          contactMap.merge(key(user1, user2), time, BaseFileContactNetwork::newer);
          lastContactTime.update(time);
        }
      }
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
    adjustTimes(contactMap, lastContactTime);
    return contactMap;
  }

  protected abstract Path path();

  protected abstract String delimiter();

  private static int parseAndIndexUser(String user, IdIndexer indexer) {
    return indexer.index(user.strip());
  }

  private static Instant parseTime(String timestamp) {
    return Instant.ofEpochSecond(Long.parseLong(timestamp.strip()));
  }

  private static Set<Integer> key(int user1, int user2) {
    return IntSet.of(user1, user2);
  }

  private static Instant newer(Instant time1, Instant time2) {
    return time1.isAfter(time2) ? time1 : time2;
  }

  private void adjustTimes(Map<Set<Integer>, Instant> contacts, LastContactTime lastContactTime) {
    Duration offset = Duration.between(lastContactTime.value, refTime());
    contacts.replaceAll((users, time) -> time.plus(offset));
  }

  protected abstract Instant refTime();

  protected abstract Set<Class<? extends Loggable>> loggable();

  private ContactTimeFactory contactTimeFactory() {
    return (user1, user2) -> contactMap().get(key(user1, user2));
  }

  private static final class LastContactTime {

    private Instant value = Instant.MIN;

    public void update(Instant time) {
      value = newer(value, time);
    }
  }

  private static final class IdIndexer extends Indexer<String> {}
}
