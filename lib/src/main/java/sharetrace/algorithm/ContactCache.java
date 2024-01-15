package sharetrace.algorithm;

import com.google.common.collect.Iterators;
import java.time.InstantSource;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;
import sharetrace.util.Cache;

final class ContactCache implements Cache<Contact> {

  private final InstantSource timeSource;
  private final Map<Contact, Contact> cache;
  private final Comparator<Contact> comparator;
  private final BinaryOperator<Contact> merger;

  private long min;

  public ContactCache(InstantSource timeSource) {
    this.timeSource = timeSource;
    this.comparator = Comparator.naturalOrder();
    this.merger = BinaryOperator.maxBy(comparator);
    this.cache = new HashMap<>();
    updateMin();
  }

  @Override
  public Optional<Contact> max() {
    return stream().max(comparator);
  }

  @Override
  public Optional<Contact> max(long atMost) {
    return stream().filter(value -> value.timestamp() <= atMost).max(comparator);
  }

  @Override
  public void add(Contact contact) {
    cache.merge(contact, contact, merger);
    updateMin(contact);
  }

  @Override
  public ContactCache refresh() {
    var currentTime = timeSource.millis();
    if (min < currentTime) {
      values().removeIf(value -> value.isExpired(currentTime));
      updateMin();
    }
    return this;
  }

  @Override
  @SuppressWarnings("NullableProblems")
  public Iterator<Contact> iterator() {
    return Iterators.unmodifiableIterator(values().iterator());
  }

  private void updateMin(Contact contact) {
    min = Math.min(min, contact.expiryTime());
  }

  private void updateMin() {
    min = stream().map(Contact::expiryTime).min(Long::compareTo).orElse(Long.MAX_VALUE);
  }

  private Stream<Contact> stream() {
    return cache.values().stream();
  }

  private Collection<Contact> values() {
    return cache.values();
  }
}
