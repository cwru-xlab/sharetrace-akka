package org.sharetrace.data;

import java.time.Instant;

@FunctionalInterface
public interface ContactTimeFactory {

  Instant create(int node1, int node2);
}
