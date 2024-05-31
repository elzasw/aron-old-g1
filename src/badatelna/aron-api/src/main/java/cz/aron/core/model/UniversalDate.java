package cz.aron.core.model;

import java.time.LocalDateTime;

import lombok.extern.slf4j.Slf4j;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Lukas Jane (inQool) 05.11.2020.
 */
@Getter
@Setter
@Slf4j
public class UniversalDate {
    private String from;

    /**
     * Signalise that the {@link #from} is an estimate value
     */
    private boolean valueFromEstimated;

    private String to;

    /**
     * Signalise that the {@link #to} is an estimate value
     */
    private boolean valueToEstimated;

    /**
     * Stored date format.
     * <p>
     * Available format characters:
     * <ul>
     *   <li><strong>C</strong> - century</li>
     *   <li><strong>Y</strong> - year</li>
     *   <li><strong>YM</strong> - year/month</li>
     *   <li><strong>D</strong> - date (year/month/day)</li>
     *   <li><strong>DT</strong> - datetime</li>
     *   <li><strong>-</strong> - interval separator</li>
     * </ul>
     * <p>
     * Format syntax options:
     * <ul>
     *   <li>one value (i.e.: {@code Y})</li>
     *   <li>interval (i.e.: {@code Y-Y})</li>
     *   <li>one-sided interval (i.e.: {@code Y-}) NOTE one sided intervals can't actually be used in this class</li>
     * </ul>
     *
     * @see <a href="https://frnk.lightcomp.cz/download/cam/modely/index.html?goto=9:3:1:308">TimeInterval#fmt</a>
     */
    private String format;

    public static boolean isLower(String d1, String d2) {
        try {
            var i1 = LocalDateTime.parse(d1);
            var i2 = LocalDateTime.parse(d2);
            return i1.compareTo(i2) < 0;
        } catch (NumberFormatException nfEx) {
            log.error("Fail to parse date, d1={}, d2={}", d1, d2);
        }
        return false;
    }

    public static boolean isHigher(String d1, String d2) {
        try {
            var i1 = LocalDateTime.parse(d1);
            var i2 = LocalDateTime.parse(d2);
            return i1.compareTo(i2) > 0;
        } catch (NumberFormatException nfEx) {
            log.error("Fail to parse date, d1={}, d2={}", d1, d2);
        }
        return false;
    }

}
