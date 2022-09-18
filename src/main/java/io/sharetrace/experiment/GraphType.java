package io.sharetrace.experiment;

import java.util.EnumSet;
import java.util.Set;

public enum GraphType {
  BARABASI_ALBERT("BarabasiAlbert"),
  GNM_RANDOM("GnmRandom"),
  RANDOM_REGULAR("RandomRegular"),
  SCALE_FREE("ScaleFree"),
  WATTS_STROGATZ("WattsStrogatz"),
  INVS13("InVS13"),
  INVS15("InVS15"),
  LH10("LH10"),
  LYON_SCHOOL("LyonSchool"),
  SFHH("SFHH"),
  THIERS11("Thiers11"),
  THIERS12("Thiers12");

  private final String name;

  GraphType(String name) {
    this.name = name;
  }

  public static Set<GraphType> synthetic() {
    return EnumSet.of(BARABASI_ALBERT, GNM_RANDOM, RANDOM_REGULAR, SCALE_FREE, WATTS_STROGATZ);
  }

  public static Set<GraphType> socioPatterns() {
    return EnumSet.of(INVS13, INVS15, LH10, LYON_SCHOOL, SFHH, THIERS11, THIERS12);
  }

  public String toString() {
    return name;
  }
}
