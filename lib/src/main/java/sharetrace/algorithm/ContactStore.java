package sharetrace.algorithm;

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import java.util.Collection;
import java.util.Map;
import java.util.function.BinaryOperator;
import sharetrace.model.Expirable;
import sharetrace.model.factory.TimeFactory;

final class ContactStore extends ExpirableStore<Contact> {

  private static final BinaryOperator<Contact> MERGER = BinaryOperator.maxBy(Expirable::compare);

  private final Map<Integer, Contact> store;

  public ContactStore(TimeFactory timeFactory) {
    super(timeFactory);
    store = new Int2ReferenceOpenHashMap<>();
  }

  @Override
  public void add(Contact contact) {
    store.merge(contact.id(), contact, MERGER);
    super.add(contact);
  }

  @Override
  protected Collection<Contact> values() {
    return store.values();
  }
}
