package sharetrace.algorithm;

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import java.time.InstantSource;
import java.util.Collection;
import java.util.Map;
import java.util.function.BinaryOperator;
import sharetrace.model.Expirable;

final class ContactCache extends Cache<Contact> {

  private static final BinaryOperator<Contact> MERGER = BinaryOperator.maxBy(Expirable::compare);

  private final Map<Integer, Contact> cache;

  public ContactCache(InstantSource timeSource) {
    super(timeSource);
    this.cache = new Int2ReferenceOpenHashMap<>();
  }

  @Override
  public void add(Contact value) {
    cache.merge(value.id(), value, MERGER);
    super.add(value);
  }

  @Override
  protected Collection<Contact> values() {
    return cache.values();
  }
}
