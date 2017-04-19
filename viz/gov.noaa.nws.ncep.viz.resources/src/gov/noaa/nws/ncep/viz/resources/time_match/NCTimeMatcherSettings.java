/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 * 
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 * 
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 * 
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package gov.noaa.nws.ncep.viz.resources.time_match;

import java.util.Calendar;
import java.util.Set;

/**
 * POJO used to store time matching settings that will be used to override the
 * default timeline settings associated with a dominant resource.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 02/11/2016   R15244     bkowal      Initial creation
 * 02/01/2017   R17975     kbugenhagen Added FORECAST_REF_TIME_SELECTION enum
 *                                     and modified update method to support
 *                                     forecast time matching.
 * 
 * </pre>
 * 
 * @author bkowal
 * @version 1.0
 */

public class NCTimeMatcherSettings {

    public static enum REF_TIME_SELECTION {
        CURRENT, LATEST, CALENDAR
    }

    public static enum FORECAST_REF_TIME_SELECTION {
        CYCLE_TIME, CALENDAR
    }

    private Integer numberFrames;

    private Integer timeRange;

    private Integer skipValue;

    private Integer frameInterval;

    private REF_TIME_SELECTION refTimeSelection;

    private FORECAST_REF_TIME_SELECTION forecastRefTimeSelection;

    /**
     * Defined when the {@link #refTimeSelection} is set to
     * {@link REF_TIME_SELECTION#CALENDAR}. An {@link IllegalArgumentException}
     * will be thrown if a ref time selection of Calendar is stored without an
     * associated selected ref time.
     */
    private Calendar selectedRefTime;

    /*
     * Need to keep track of the selected frame times when non-default.
     */
    private Set<Calendar> selectedFrameTimes;

    public void update(NCTimeMatcherSettings update) {
        if (update.numberFrames != null) {
            this.numberFrames = update.numberFrames;
        }
        if (update.timeRange != null) {
            this.timeRange = update.timeRange;
        }
        if (update.skipValue != null) {
            this.skipValue = update.skipValue;
        }
        if (update.frameInterval != null) {
            this.frameInterval = update.frameInterval;
        }
        if (update.refTimeSelection != null) {
            this.refTimeSelection = update.refTimeSelection;
            if (this.refTimeSelection == REF_TIME_SELECTION.CALENDAR) {
                this.selectedRefTime = update.selectedRefTime;
                if (this.selectedRefTime == null) {
                    throw new IllegalArgumentException(
                            "An associated selectedRefTime must be specified when ref time selection is set to: CALENDAR.");
                }
            } else {
                this.selectedRefTime = null;
            }
        } else if (update.forecastRefTimeSelection != null) {
            this.forecastRefTimeSelection = update.forecastRefTimeSelection;
            if (this.forecastRefTimeSelection == FORECAST_REF_TIME_SELECTION.CALENDAR) {
                this.selectedRefTime = update.selectedRefTime;
                if (this.selectedRefTime == null) {
                    throw new IllegalArgumentException(
                            "An associated selectedRefTime must be specified when ref time selection is set to: CALENDAR.");
                }
            } else {
                this.selectedRefTime = null;
            }
        }

        if (update.selectedFrameTimes != null) {
            this.selectedFrameTimes = update.selectedFrameTimes;
        }
    }

    /**
     * @return the numberFrames
     */
    public Integer getNumberFrames() {
        return numberFrames;
    }

    /**
     * @param numberFrames
     *            the numberFrames to set
     */
    public void setNumberFrames(Integer numberFrames) {
        this.numberFrames = numberFrames;
    }

    /**
     * @return the timeRange
     */
    public Integer getTimeRange() {
        return timeRange;
    }

    /**
     * @param timeRange
     *            the timeRange to set
     */
    public void setTimeRange(Integer timeRange) {
        this.timeRange = timeRange;
    }

    /**
     * @return the skipValue
     */
    public Integer getSkipValue() {
        return skipValue;
    }

    /**
     * @param skipValue
     *            the skipValue to set
     */
    public void setSkipValue(Integer skipValue) {
        this.skipValue = skipValue;
    }

    /**
     * @return the frameInterval
     */
    public Integer getFrameInterval() {
        return frameInterval;
    }

    /**
     * @param frameInterval
     *            the frameInterval to set
     */
    public void setFrameInterval(Integer frameInterval) {
        this.frameInterval = frameInterval;
    }

    /**
     * @return the refTimeSelection
     */
    public REF_TIME_SELECTION getRefTimeSelection() {
        return refTimeSelection;
    }

    /**
     * @param refTimeSelection
     *            the refTimeSelection to set
     */
    public void setRefTimeSelection(REF_TIME_SELECTION refTimeSelection) {
        this.refTimeSelection = refTimeSelection;
    }

    public FORECAST_REF_TIME_SELECTION getForecastRefTimeSelection() {
        return forecastRefTimeSelection;
    }

    public void setForecastRefTimeSelection(
            FORECAST_REF_TIME_SELECTION forecastRefTimeSelection) {
        this.forecastRefTimeSelection = forecastRefTimeSelection;
    }

    /**
     * @return the selectedRefTime
     */
    public Calendar getSelectedRefTime() {
        return selectedRefTime;
    }

    /**
     * @param selectedRefTime
     *            the selectedRefTime to set
     */
    public void setSelectedRefTime(Calendar selectedRefTime) {
        this.selectedRefTime = selectedRefTime;
    }

    /**
     * @return the selectedFrameTimes
     */
    public Set<Calendar> getSelectedFrameTimes() {
        return selectedFrameTimes;
    }

    /**
     * @param selectedFrameTimes
     *            the selectedFrameTimes to set
     */
    public void setSelectedFrameTimes(Set<Calendar> selectedFrameTimes) {
        this.selectedFrameTimes = selectedFrameTimes;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("NCTimeMatcherSettings [");
        boolean comma = false;

        comma = addField("numberFrames", this.numberFrames, comma, sb);
        comma = addField("timeRange", this.timeRange, comma, sb) || comma;
        comma = addField("skipValue", this.skipValue, comma, sb) || comma;
        comma = addField("frameInterval", this.frameInterval, comma, sb)
                || comma;
        comma = addField("refTimeSelection", this.refTimeSelection, comma, sb)
                || comma;
        comma = addField("selectedRefTime",
                (this.selectedRefTime == null) ? null
                        : this.selectedRefTime.getTime().toString(),
                comma, sb) || comma;
        if (this.selectedFrameTimes != null) {
            if (comma) {
                sb.append(", ");
            }
            sb.append("selectedFrameTimes=");
            if (this.selectedFrameTimes.isEmpty()) {
                sb.append("{ }");
            } else {
                sb.append("{ ");
                boolean first = true;
                for (Calendar selection : this.selectedFrameTimes) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(", ");
                    }
                    sb.append(selection.getTime().toString());
                }
                sb.append(" }");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Helper method used to add non-null fields to the String representation of
     * this object.
     * 
     * @param name
     *            the name of the field
     * @param value
     *            the current value associated with the field
     * @param comma
     *            boolean flag indicating whether or not a comma needs to be
     *            added to the string to separate multiple fields
     * @param sb
     *            the {@link StringBuilder} used to construct the String
     *            representation of this object.
     * @return
     */
    private static boolean addField(String name, Object value, boolean comma,
            StringBuilder sb) {
        if (value == null) {
            return false;
        }

        if (comma) {
            sb.append(", ");
        }
        sb.append(name).append("=").append(value);
        return true;
    }
}
