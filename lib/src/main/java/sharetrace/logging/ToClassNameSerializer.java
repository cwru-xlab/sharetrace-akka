package sharetrace.logging;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;

public final class ToClassNameSerializer extends StdSerializer<Object> {

  public ToClassNameSerializer() {
    super(Object.class);
  }

  @Override
  public void serialize(Object value, JsonGenerator generator, SerializerProvider provider)
      throws IOException {
    generator.writeString(value.getClass().getName());
  }
}
