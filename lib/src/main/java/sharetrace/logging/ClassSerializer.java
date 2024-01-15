package sharetrace.logging;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;

@SuppressWarnings("rawtypes")
final class ClassSerializer extends StdSerializer<Class> {

  public ClassSerializer() {
    super(Class.class);
  }

  @Override
  public void serialize(Class value, JsonGenerator generator, SerializerProvider provider)
      throws IOException {
    generator.writeString(value.getSimpleName());
  }
}
