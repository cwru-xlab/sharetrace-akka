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
import org.sharetrace.util.Indexer;
import org.sharetrace.util.LoggableRef;
import org.sharetrace.util.TimeRef;

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
    return helper().users();
  }

  @Override
  public Set<Contact> contacts() {
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

  @Value.Derived
  protected Map<Set<Integer>, Instant> contactMap() {
    ContactsResult result;
    try (BufferedReader reader = Files.newBufferedReader(path())) {
      result = toContacts(reader.lines()::iterator);
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
    adjustTimes(result);
    return result.contacts;
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

  private void adjustTimes(ContactsResult result) {
    Duration offset = Duration.between(result.lastContactTime, refTime());
    result.contacts.replaceAll((x, time) -> time.plus(offset));
  }

  private ContactsResult toContacts(Iterable<String> lines) {
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
    return new ContactsResult(contacts, lastContactTime);
  }

  private ContactTimeFactory contactTimeFactory() {
    return (user1, user2) -> contactMap().get(key(user1, user2));
  }

  private static final class ContactsResult {

    private final Map<Set<Integer>, Instant> contacts;
    private final Instant lastContactTime;

    private ContactsResult(Map<Set<Integer>, Instant> contacts, Instant lastContactTime) {
      this.contacts = contacts;
      this.lastContactTime = lastContactTime;
    }
  }
}
