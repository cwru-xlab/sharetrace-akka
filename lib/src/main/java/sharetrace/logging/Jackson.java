package sharetrace.logging;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.dataformat.ion.IonFactory;
import com.fasterxml.jackson.dataformat.ion.IonObjectMapper;
import java.time.InstantSource;
import org.apache.commons.math3.random.RandomGenerator;

public final class Jackson {

  private Jackson() {}

  public static JsonFactory newIonJsonFactory() {
    return new IonFactory(newIonObjectMapper());
  }

  public static ObjectMapper newIonObjectMapper() {
    return configured(new IonObjectMapper());
  }

  public static ObjectMapper newObjectMapper() {
    return configured(new ObjectMapper());
  }

  private static ObjectMapper configured(ObjectMapper mapper) {
    return mapper.findAndRegisterModules().registerModule(shareTraceModule());
  }

  private static Module shareTraceModule() {
    return new SimpleModule()
        .addSerializer(InstantSource.class, ToStringSerializer.instance)
        .addSerializer(RandomGenerator.class, ToClassNameSerializer.INSTANCE)
        .addSerializer(Class.class, ClassSerializer.INSTANCE);
  }
}
