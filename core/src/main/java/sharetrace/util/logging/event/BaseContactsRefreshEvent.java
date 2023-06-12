package sharetrace.util.logging.event;

import org.immutables.value.Value;

@Value.Immutable
interface BaseContactsRefreshEvent extends EventRecord {

  int remaining();

  int expired();
}
