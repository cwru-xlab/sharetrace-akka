package io.sharetrace.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class IndexerTests {

  private Indexer<Integer> indexer;

  @BeforeEach
  public void setUp() {
    indexer = new Indexer<>();
  }

  @Test
  public void indexNonExistingFirstEntryReturnsZero() {
    Assertions.assertEquals(indexer.index(1), 0);
  }

  @Test
  public void indexExistingFirstEntryReturnsZero() {
    indexer.index(1);
    Assertions.assertEquals(indexer.index(1), 0);
  }

  @Test
  public void indexNonExistingSecondEntryReturnsOne() {
    indexer.index(1);
    Assertions.assertEquals(indexer.index(2), 1);
  }
}
