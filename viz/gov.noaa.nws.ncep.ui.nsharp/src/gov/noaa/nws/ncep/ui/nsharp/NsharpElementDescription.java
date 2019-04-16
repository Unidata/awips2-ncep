/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract EA133W-17-CQ-0082 with the US Government.
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
package gov.noaa.nws.ncep.ui.nsharp;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * 
 * Contains the identifying information for a single sounding.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Dec 14, 2018  6872     bsteffen  Initial creation
 * 
 * </pre>
 *
 * @author bsteffen
 */
public class NsharpElementDescription
        implements Comparable<NsharpElementDescription> {

    private final NsharpOperationElement station;

    private final NsharpTimeOperationElement time;

    private final NsharpOperationElement type;

    private NsharpElementDescription(String station, String type,
            NsharpTimeOperationElement time) {
        Optional<Duration> fcstAmount = time.getFcstAmount();
        if (fcstAmount.isPresent() && !type.contains("@")) {
            ZonedDateTime refTime = time.getTime().minus(fcstAmount.get())
                    .atZone(ZoneOffset.UTC);
            int day = refTime.getDayOfMonth();
            int hour = refTime.getHour();
            type = String.format("%02d.%02d@%s", day, hour, type);
        }
        station.replaceAll(" ", "_");
        this.station = new NsharpStringOperationElement(station);
        this.time = time;
        this.type = new NsharpStringOperationElement(type);
    }

    public NsharpElementDescription(String station, String type, Instant time,
            Duration fcstAmount) {
        this(station, type, new NsharpTimeOperationElement(time, fcstAmount));
    }

    public NsharpElementDescription(String station, String type, Instant time) {
        this(station, type, new NsharpTimeOperationElement(time));
    }

    public NsharpOperationElement getStationElement() {
        return station;
    }

    public NsharpTimeOperationElement getTimeElement() {
        return time;
    }

    public NsharpOperationElement getTypeElement() {
        return type;
    }

    /**
     * Opposite of {@link #parse(String)}. The description is stored in archive
     * files so this must be kept in sync with the parse method and backwards
     * compatible.
     * 
     * @return String version of this description
     */
    public String getDescription() {
        return String.format("%s %s %s", station.getDescription(),
                time.getDescription(), type.getDescription());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((station == null) ? 0 : station.hashCode());
        result = prime * result + ((time == null) ? 0 : time.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        NsharpElementDescription other = (NsharpElementDescription) obj;
        if (station == null) {
            if (other.station != null)
                return false;
        } else if (!station.equals(other.station))
            return false;
        if (time == null) {
            if (other.time != null)
                return false;
        } else if (!time.equals(other.time))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        return true;
    }

    @Override
    public int compareTo(NsharpElementDescription o) {
        int result = station.compareTo(o.station);
        if (result == 0) {
            result = time.compareTo(o.time);
            if (result == 0) {
                result = type.compareTo(o.type);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return getDescription();
    }

    /**
     * Opposite of {@link #getDescription()}. The description is stored in
     * archive files so this must remain backwards compatible.
     * 
     * @param description
     *            String version of description
     * @return parsed description
     */
    public static NsharpElementDescription parse(String description) {
        int start = 0;
        int end = description.indexOf(' ');
        String station = description.substring(start, end);
        start = end + 1;
        end = description.indexOf(' ', start);
        String timeLine = description.substring(start, end);
        start = end + 1;
        String type = description.substring(start);
        return new NsharpElementDescription(station, type,
                NsharpTimeOperationElement.parse(timeLine));
    }

}
