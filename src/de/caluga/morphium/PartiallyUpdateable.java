package de.caluga.morphium;

import java.util.List;

/**
 * User: Stpehan Bösebeck
 * Date: 11.04.12
 * Time: 08:41
 * <p/>
 * TODO: Add documentation here
 */
public interface PartiallyUpdateable {
    public List<String> getAlteredFields();
}
