package de.schroedingerscat.coverblame.blame;

import java.io.File;
import java.util.Map;

/**
 *
 */
public interface Blamer {

    Map<Integer, String> blame(File sourceFile);
}
