package sharetrace.algorithm;

import com.google.common.collect.Iterators;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import java.time.InstantSource;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BinaryOperator;

final class ContactCache implements Iterable<Contact> {

  private static final BinaryOperator<Contact> MERGER = BinaryOperator.maxBy(Contact::compareTo);

  private final InstantSource timeSource;
  private final Map<Integer, Contact> cache;

  private long minExpiryTime;

  public ContactCache(InstantSource timeSource) {
    this.timeSource = timeSource;
    this.cache = new Int2ReferenceOpenHashMap<>();
    updateMinExpiryTime();
  }

  public void add(Contact contact) {
    cache.merge(contact.id(), contact, MERGER);
    updateMinExpiryTime(contact);
  }

  public void refresh() {
    var currentTime = timeSource.millis();
    if (minExpiryTime < currentTime) {
      cache.values().removeIf(value -> value.isExpired(currentTime));
      updateMinExpiryTime();
    }
  }

  @Override
  @SuppressWarnings("NullableProblems")
  public Iterator<Contact> iterator() {
    return Iterators.unmodifiableIterator(cache.values().iterator());
  }

  private void updateMinExpiryTime(Contact contact) {
    minExpiryTime = Math.min(minExpiryTime, contact.expiryTime());
  }

  private void updateMinExpiryTime() {
    minExpiryTime =
        cache.values().stream()
            .map(Contact::expiryTime)
            .min(Long::compareTo)
            .orElse(Long.MAX_VALUE);
  }
}
