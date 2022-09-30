package io.sharetrace.experiment;

import java.util.EnumSet;
import java.util.Set;

public enum GraphType {
  // Synthetic
  BARABASI_ALBERT("BarabasiAlbert"),
  GNM_RANDOM("GnmRandom"),
  RANDOM_REGULAR("RandomRegular"),
  SCALE_FREE("ScaleFree"),
  WATTS_STROGATZ("WattsStrogatz"),
  // SocioPatterns
  INVS13("InVS13"),
  INVS15("InVS15"),
  LH10("LH10"),
  LYON_SCHOOL("LyonSchool"),
  SFHH("SFHH"),
  THIERS11("Thiers11"),
  THIERS12("Thiers12");

  private static final Set<GraphType> SYNTHETIC =
      EnumSet.of(BARABASI_ALBERT, GNM_RANDOM, RANDOM_REGULAR, SCALE_FREE, WATTS_STROGATZ);

  private static final Set<GraphType> SOCIO_PATTERNS =
      EnumSet.of(INVS13, INVS15, LH10, LYON_SCHOOL, SFHH, THIERS11, THIERS12);

  private final String name;

  GraphType(String name) {
    this.name = name;
  }

  public static Set<GraphType> synthetic() {
    return SYNTHETIC;
  }

  public static Set<GraphType> socioPatterns() {
    return SOCIO_PATTERNS;
  }

  public String toString() {
    return name;
  }
}
