package sharetrace.logging.jackson;

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

  public static JsonFactory ionJsonFactory() {
    return InstanceHolder.ION_JSON_FACTORY;
  }

  public static JsonFactory jsonFactory() {
    return InstanceHolder.JSON_FACTORY;
  }

  public static ObjectMapper ionObjectMapper() {
    return InstanceHolder.ION_OBJECT_MAPPER;
  }

  public static ObjectMapper objectMapper() {
    return InstanceHolder.OBJECT_MAPPER;
  }

  private static class InstanceHolder {

    public static final ObjectMapper ION_OBJECT_MAPPER = configured(new IonObjectMapper());
    public static final ObjectMapper OBJECT_MAPPER = configured(new ObjectMapper());
    public static final JsonFactory ION_JSON_FACTORY = new IonFactory(ION_OBJECT_MAPPER);
    public static final JsonFactory JSON_FACTORY = new JsonFactory(OBJECT_MAPPER);

    private static ObjectMapper configured(ObjectMapper mapper) {
      return mapper.findAndRegisterModules().registerModule(shareTraceModule());
    }

    private static Module shareTraceModule() {
      return new SimpleModule()
          .addSerializer(InstantSource.class, new ToStringSerializer())
          .addSerializer(RandomGenerator.class, new ToClassNameSerializer())
          .addSerializer(Class.class, new ClassSerializer())
          .addKeySerializer(Class.class, new ClassKeySerializer());
    }
  }
}
