package sharetrace;

import java.io.IOException;
import java.util.stream.IntStream;
import sharetrace.util.IdFactory;
import sharetrace.util.Timer;

public class Sandbox {

  public static void main(String[] args) throws IOException {

    var timer = new Timer<String>();
    var iterations = 1000_000;
    timer.start();
    testLong(timer, iterations);
    testUlid(timer, iterations);
    timer.stop();
    System.out.println("long " + timer.duration("long"));
    System.out.println("ulid " + timer.duration("ulid"));
  }

  private static void testLong(Timer<String> timer, int iterations) {
    runLong(iterations);
    timer.time(() -> runLong(iterations), "long");
  }

  private static void testUlid(Timer<String> timer, int iterations) {
    runUlid(iterations);
    timer.time(() -> runUlid(iterations), "ulid");
  }

  private static void runLong(int iterations) {
    IntStream.range(0, iterations).forEach(x -> IdFactory.newLong());
  }

  private static void runUlid(int iterations) {
    IntStream.range(0, iterations).forEach(x -> IdFactory.nextUlid());
  }
}
