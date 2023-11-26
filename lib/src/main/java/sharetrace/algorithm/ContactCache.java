package sharetrace.algorithm;

import com.google.common.collect.Iterators;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;
import sharetrace.model.Timestamp;
import sharetrace.util.Cache;
import sharetrace.util.TimeSource;

final class ContactCache implements Cache<Contact> {

  private final TimeSource timeSource;
  private final Map<Contact, Contact> cache;
  private final Comparator<Contact> comparator;
  private final BinaryOperator<Contact> merger;
  private Timestamp min;

  public ContactCache(TimeSource timeSource) {
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
  public Optional<Contact> max(Timestamp atMost) {
    return stream().filter(value -> !value.timestamp().isAfter(atMost)).max(comparator);
  }

  @Override
  public void add(Contact contact) {
    cache.merge(contact, contact, merger);
    updateMin(contact);
  }

  @Override
  public ContactCache refresh() {
    var currentTime = timeSource.timestamp();
    if (min.isBefore(currentTime)) {
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
    min = Timestamp.min(min, contact.expiryTime());
  }

  private void updateMin() {
    min = stream().map(Contact::expiryTime).min(Timestamp::compareTo).orElse(Timestamp.MAX);
  }

  private Stream<Contact> stream() {
    return cache.values().stream();
  }

  private Collection<Contact> values() {
    return cache.values();
  }
}
