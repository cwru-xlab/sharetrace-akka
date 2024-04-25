package sharetrace.algorithm;

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.function.BinaryOperator;
import sharetrace.model.factory.TimeFactory;

final class ContactStore extends ExpirableStore<Contact> {

  private static final BinaryOperator<Contact> MERGE_OPERATOR = mergeOperator();

  private final Map<Integer, Contact> store;

  public ContactStore(TimeFactory timeFactory) {
    super(timeFactory);
    store = new Int2ReferenceOpenHashMap<>();
  }

  @Override
  public void add(Contact contact) {
    store.merge(contact.id(), contact, MERGE_OPERATOR);
    super.add(contact);
  }

  @Override
  protected Collection<Contact> values() {
    return store.values();
  }

  private static BinaryOperator<Contact> mergeOperator() {
    return BinaryOperator.maxBy(Comparator.comparingLong(Contact::expiryTime));
  }
}
