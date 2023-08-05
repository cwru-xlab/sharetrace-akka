package sharetrace.logging;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.ion.IonFactory;
import com.fasterxml.jackson.dataformat.ion.IonObjectMapper;

public final class Jackson {

  private Jackson() {}

  public static JsonFactory newJsonFactory() {
    return new IonFactory(newObjectMapper());
  }

  public static ObjectMapper newObjectMapper() {
    return new IonObjectMapper().findAndRegisterModules();
  }
}
