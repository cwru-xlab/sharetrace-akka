package sharetrace.logging;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;

final class ToClassNameSerializer extends StdSerializer<Object> {

  private static final ClassSerializer CLASS_SERIALIZER = new ClassSerializer();

  public ToClassNameSerializer() {
    super(Object.class);
  }

  @Override
  public void serialize(Object value, JsonGenerator generator, SerializerProvider provider)
      throws IOException {
    CLASS_SERIALIZER.serialize(value.getClass(), generator, provider);
  }
}
