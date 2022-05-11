package org.sharetrace.experiment;

import java.nio.file.Path;
import org.sharetrace.RiskPropagation;

public final class SocioPatternsExperiment extends FileExperiment {

  private SocioPatternsExperiment(Path path, String delimiter, int nRepeats, long seed) {
    super(path, delimiter, nRepeats, seed);
  }

  /**
   * Runs {@link RiskPropagation} on the SocioPatterns 2013 workplace dataset.
   *
   * <p>See <a
   * href="http://www.sociopatterns.org/datasets/contacts-in-a-workplace/">http://www.sociopatterns.org</a>
   */
  public static void runInVs13(Path path, int nRepeats, long seed) {
    runTabDelimited(path, nRepeats, seed);
  }

  /**
   * Runs {@link RiskPropagation} on the SocioPatterns 2015 workplace dataset.
   *
   * <p>See <a
   * href="http://www.sociopatterns.org/datasets/contacts-in-a-workplace/">http://www.sociopatterns.org</a>
   */
  public static void runInVs15(Path path, int nRepeats, long seed) {
    runSpaceDelimited(path, nRepeats, seed);
  }

  /**
   * Runs {@link RiskPropagation} on the SocioPatterns 2011 primary-school dataset.
   *
   * <p>See <a
   * href="http://www.sociopatterns.org/datasets/primary-school-temporal-network-data/">http://www.sociopatterns.org</a>
   */
  public static void runLyonSchool(Path path, int nRepeats, long seed) {
    runTabDelimited(path, nRepeats, seed);
  }

  /**
   * Runs {@link RiskPropagation} on the SocioPatterns hospital ward dataset.
   *
   * <p>See <a
   * href="http://www.sociopatterns.org/datasets/hospital-ward-dynamic-contact-network/">http://www.sociopatterns.org</a>
   */
  public static void runLh10(Path path, int nRepeats, long seed) {
    runTabDelimited(path, nRepeats, seed);
  }

  /**
   * Runs {@link RiskPropagation} on the SocioPatterns 2011 high-school dataset.
   *
   * <p>See <a
   * href="http://www.sociopatterns.org/datasets/high-school-dynamic-contact-networks/">http://www.sociopatterns.org</a>
   */
  public static void runThiers11(Path path, int nRepeats, long seed) {
    runTabDelimited(path, nRepeats, seed);
  }

  /**
   * Runs {@link RiskPropagation} on the SocioPatterns 2012 high-school dataset.
   *
   * <p>See <a
   * href="http://www.sociopatterns.org/datasets/high-school-dynamic-contact-networks/">http://www.sociopatterns.org</a>
   */
  public static void runThiers12(Path path, int nRepeats, long seed) {
    runTabDelimited(path, nRepeats, seed);
  }

  /**
   * Runs {@link RiskPropagation} on the SocioPatterns 2009 scientific conference dataset.
   *
   * <p>See <a
   * href="http://www.sociopatterns.org/datasets/sfhh-conference-data-set/">http://www.sociopatterns.org</a>
   */
  public static void runSfhh(Path path, int nRepeats, long seed) {
    runSpaceDelimited(path, nRepeats, seed);
  }
}
