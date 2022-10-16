package io.sharetrace.graph;

import io.sharetrace.actor.RiskPropagation;
import io.sharetrace.model.Identifiable;
import java.util.Set;

/**
 * An undirected simple graph in which a vertex represents a user and an edge indicates that the
 * associated users of the incident vertices came in contact. Vertices are enumerated, starting at
 * 0.
 *
 * @see RiskPropagation
 * @see Contact
 */
public interface ContactNetwork extends Identifiable {

  Set<Integer> users();

  Set<Contact> contacts();

  void logMetrics();
}
