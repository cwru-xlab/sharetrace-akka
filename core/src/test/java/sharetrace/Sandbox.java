package sharetrace;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import sharetrace.graph.FileTemporalNetworkFactory;
import sharetrace.graph.TemporalNetwork;

public class Sandbox {

  public static void main(String[] args) throws IOException {
    TemporalNetwork<Integer> network =
        FileTemporalNetworkFactory.<Integer>builder()
            .delimiter("\\s+")
            .path(Path.of("core/src/main/resources/datasets/InVS13.txt"))
            .referenceTimestamp(Instant.now())
            .nodeParser(Integer::valueOf)
            .build()
            .getNetwork();
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    objectMapper.writeValue(System.out, network);
  }
}
