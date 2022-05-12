package org.sharetrace.data.factory;

import java.time.Instant;

@FunctionalInterface
public interface ContactTimeFactory {

  Instant getContactTime(int node1, int node2);
}