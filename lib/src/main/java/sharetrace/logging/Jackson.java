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

  public static JsonFactory newJsonFactory() {
    return new IonFactory(newObjectMapper());
  }

  public static ObjectMapper newObjectMapper() {
    return new IonObjectMapper().findAndRegisterModules().registerModule(shareTraceModule());
  }

  private static Module shareTraceModule() {
    return new SimpleModule()
        .addSerializer(InstantSource.class, new ToStringSerializer())
        .addSerializer(RandomGenerator.class, new ToClassNameSerializer());
  }
}
