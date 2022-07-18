package org.sharetrace.graph;

import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.jgrapht.Graph;
import org.sharetrace.RiskPropagation;
import org.sharetrace.User;

/**
 * An undirected simple graph in which a node represents a user and an edge between two nodes
 * indicates that the associated users of the incident nodes came in contact. Node identifiers are
 * zero-based contiguous natural numbers. In an instance of {@link RiskPropagation}, the topology of
 * this graph is mapped to a collection {@link User} actors.
 *
 * @see User
 * @see Contact
 */
public interface ContactNetwork {

  int nUsers();

  int nContacts();

  IntStream users();

  Stream<Contact> contacts();

  void logMetrics();

  Graph<Integer, Edge<Integer>> topology();
}
