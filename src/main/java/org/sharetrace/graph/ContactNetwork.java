package org.sharetrace.graph;

import java.util.Set;
import org.sharetrace.actor.RiskPropagation;

/**
 * An undirected simple graph in which a vertex represents a user and an edge indicates that the
 * associated users of the incident vertices came in contact. Vertices are enumerated, starting at
 * 0.
 *
 * @see RiskPropagation
 * @see Contact
 */
public interface ContactNetwork {

  Set<Integer> users();

  Set<Contact> contacts();

  void logMetrics();
}
