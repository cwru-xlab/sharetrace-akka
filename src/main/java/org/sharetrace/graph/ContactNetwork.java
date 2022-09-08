package org.sharetrace.graph;

import java.util.Set;
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

  Set<Integer> users();

  Set<Contact> contacts();

  void logMetrics();
}
