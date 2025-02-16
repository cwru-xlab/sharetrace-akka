package sharetrace.algorithm;

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import java.util.Collection;
import java.util.Map;
import sharetrace.model.factory.TimeFactory;

final class ContactStore extends ExpirableStore<Contact> {

  private final Map<Integer, Contact> store;

  public ContactStore(TimeFactory timeFactory) {
    super(timeFactory);
    store = new Int2ReferenceOpenHashMap<>();
  }

  @Override
  public void add(Contact contact) {
    store.merge(contact.id(), contact, Contact::merge);
    super.add(contact);
  }

  @Override
  protected Collection<Contact> values() {
    return store.values();
  }
}
