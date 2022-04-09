package org.sharetrace.experiment;

public enum GraphType {
  BARABASI_ALBERT,
  GEOMETRIC,
  SCALE_FREE;

  public GraphType match(String string) {
    return valueOf(string.strip().toUpperCase());
  }
}
