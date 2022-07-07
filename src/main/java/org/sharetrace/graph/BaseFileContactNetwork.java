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
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.immutables.value.Value;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.generate.GraphGenerator;
import org.sharetrace.logging.Loggable;

@Value.Immutable
abstract class BaseFileContactNetwork implements ContactNetwork {

  private static Set<Integer> key(int user1, int user2) {
    return Set.of(user1, user2);
  }

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
    return helper().contacts(this::toContact);
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
    List<Integer> users;
    for (Entry<Set<Integer>, Instant> contact : contactMap().entrySet()) {
      users = List.copyOf(contact.getKey());
      Graphs.addEdgeWithVertices(target, users.get(0), users.get(1));
    }
  }

  @Value.Derived
  protected Map<Set<Integer>, Instant> contactMap() {
    LastContactTime lastContactTime = new LastContactTime();
    Map<Set<Integer>, Instant> contacts = newContacts();
    IdIndexer indexer = new IdIndexer();
    try (BufferedReader reader = Files.newBufferedReader(path())) {
      reader.lines().forEach(line -> processLine(line, contacts, indexer, lastContactTime));
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
    adjustTimestamps(contacts, lastContactTime);
    return contacts;
  }

  private Map<Set<Integer>, Instant> newContacts() {
    return new Object2ObjectOpenHashMap<>();
  }

  protected abstract Path path();

  private void processLine(
      String line,
      Map<Set<Integer>, Instant> contacts,
      IdIndexer indexer,
      LastContactTime lastContactTime) {
    Parsed parsed = new Parsed(line, delimiter(), indexer);
    if (parsed.user1 != parsed.user2) {
      contacts.merge(parsed.key(), parsed.timestamp, BaseFileContactNetwork::newer);
      lastContactTime.update(parsed.timestamp);
    }
  }

  private void adjustTimestamps(
      Map<Set<Integer>, Instant> contacts, LastContactTime lastContactTime) {
    Duration offset = Duration.between(lastContactTime.value, referenceTime());
    contacts.replaceAll((users, timestamp) -> timestamp.plus(offset));
  }

  protected abstract String delimiter();

  private static Instant newer(Instant timestamp1, Instant timestamp2) {
    return timestamp1.isAfter(timestamp2) ? timestamp1 : timestamp2;
  }

  protected abstract Instant referenceTime();

  private Contact toContact(Edge<Integer> edge) {
    Set<Integer> key = key(edge.source(), edge.target());
    Instant timestamp = contactMap().get(key);
    return helper().toContact(edge, timestamp);
  }

  private static final class Parsed {

    private final int user1;
    private final int user2;
    private final Instant timestamp;

    private Parsed(String string, String delimiter, IdIndexer indexer) {
      String[] args = string.split(delimiter);
      this.user1 = indexer.index(Integer.parseInt(args[1].strip()));
      this.user2 = indexer.index(Integer.parseInt(args[2].strip()));
      this.timestamp = Instant.ofEpochSecond(Long.parseLong(args[0].strip()));
    }

    public Set<Integer> key() {
      return BaseFileContactNetwork.key(user1, user2);
    }

    @Override
    public String toString() {
      return "Parsed{" + "user1=" + user1 + ", user2=" + user2 + ", timestamp=" + timestamp + '}';
    }
  }

  private static final class LastContactTime {

    private Instant value = Instant.MIN;

    public void update(Instant timestamp) {
      value = newer(value, timestamp);
    }
  }

  private static final class IdIndexer {

    private final Map<Integer, Integer> index = new Int2IntOpenHashMap();
    private int currentId = 0;

    public int index(int id) {
      Integer indexed = index.putIfAbsent(id, currentId);
      return (indexed == null) ? currentId++ : indexed;
    }
  }
}
