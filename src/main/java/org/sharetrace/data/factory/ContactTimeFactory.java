package org.sharetrace.data.factory;

import java.time.Instant;

@FunctionalInterface
public interface ContactTimeFactory {

  Instant getContactTime(int user1, int user2);
}
