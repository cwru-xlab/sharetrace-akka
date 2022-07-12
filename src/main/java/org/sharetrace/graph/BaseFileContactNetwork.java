package org.sharetrace.graph;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
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
import org.sharetrace.data.factory.ContactTimeFactory;
import org.sharetrace.logging.Loggable;

@Value.Immutable
abstract class BaseFileContactNetwork implements ContactNetwork {

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
    return helper().contacts(contactTimeFactory());
  }

  @Override
  public void logMetrics() {
    helper().logMetrics();
  }

  @Value.Derived
  protected ContactNetworkHelper helper() {
    return ContactNetworkHelper.create(graphGenerator(), loggable());
  }

  private GraphGenerator<Integer, Edge<Integer>, Integer> graphGenerator() {
    return (target, x) -> generateGraph(target);
  }

  protected abstract Set<Class<? extends Loggable>> loggable();

  private void generateGraph(Graph<Integer, Edge<Integer>> target) {
    contactMap().keySet().stream()
        .map(List::copyOf)
        .forEach(users -> Graphs.addEdgeWithVertices(target, users.get(0), users.get(1)));
  }

  @Value.Derived
  protected Map<Set<Integer>, Instant> contactMap() {
    LastContactTime lastContactTime = new LastContactTime();
    Map<Set<Integer>, Instant> contactMap = newContactMap();
    IdIndexer indexer = new IdIndexer();
    try (BufferedReader reader = Files.newBufferedReader(path())) {
      reader.lines().forEach(line -> processLine(line, contactMap, indexer, lastContactTime));
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
    adjustTimestamps(contactMap, lastContactTime);
    return contactMap;
  }

  private Map<Set<Integer>, Instant> newContactMap() {
    return new Object2ObjectOpenHashMap<>();
  }

  protected abstract Path path();

  private void processLine(
      String line,
      Map<Set<Integer>, Instant> contacts,
      IdIndexer indexer,
      LastContactTime lastContactTime) {
    String[] args = line.split(delimiter());
    int user1 = parseAndIndexUser(args[1], indexer);
    int user2 = parseAndIndexUser(args[2], indexer);
    if (user1 != user2) {
      Instant timestamp = parseTimestamp(args[0]);
      contacts.merge(key(user1, user2), timestamp, BaseFileContactNetwork::newer);
      lastContactTime.update(timestamp);
    }
  }

  private void adjustTimestamps(
      Map<Set<Integer>, Instant> contacts, LastContactTime lastContactTime) {
    Duration offset = Duration.between(lastContactTime.value, referenceTime());
    contacts.replaceAll((users, timestamp) -> timestamp.plus(offset));
  }

  protected abstract String delimiter();

  private static int parseAndIndexUser(String user, IdIndexer indexer) {
    return indexer.index(Integer.parseInt(user.strip()));
  }

  private static Instant parseTimestamp(String timestamp) {
    return Instant.ofEpochSecond(Long.parseLong(timestamp.strip()));
  }

  private static Set<Integer> key(int user1, int user2) {
    return Set.of(user1, user2);
  }

  private static Instant newer(Instant timestamp1, Instant timestamp2) {
    return timestamp1.isAfter(timestamp2) ? timestamp1 : timestamp2;
  }

  protected abstract Instant referenceTime();

  private ContactTimeFactory contactTimeFactory() {
    return (user1, user2) -> contactMap().get(key(user1, user2));
  }

  private static final class LastContactTime {

    private Instant value = Instant.MIN;

    public void update(Instant timestamp) {
      value = newer(value, timestamp);
    }

    @Override
    public String toString() {
      return "LastContactTime{value=" + value + '}';
    }
  }

  private static final class IdIndexer {

    private final Map<Integer, Integer> index = new Int2IntOpenHashMap();
    private int currentId = 0;

    public int index(int id) {
      Integer indexed = index.putIfAbsent(id, currentId);
      return (indexed == null) ? currentId++ : indexed;
    }

    @Override
    public String toString() {
      return "IdIndexer{currentId=" + currentId + '}';
    }
  }
}
