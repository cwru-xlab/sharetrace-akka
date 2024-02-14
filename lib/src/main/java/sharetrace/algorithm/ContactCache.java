package sharetrace.algorithm;

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import java.time.InstantSource;
import java.util.Collection;
import java.util.Map;

final class ContactCache extends Cache<Contact> {

  private final Map<Integer, Contact> cache;

  public ContactCache(InstantSource timeSource) {
    super(timeSource);
    this.cache = new Int2ReferenceOpenHashMap<>();
  }

  @Override
  protected void doAdd(Contact value) {
    cache.merge(value.id(), value, merger);
  }

  @Override
  protected Collection<Contact> values() {
    return cache.values();
  }
}
