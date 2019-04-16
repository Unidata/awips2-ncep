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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * 
 * The time information necessary to uniquely identify a sounding. This consists
 * of a time for the sounding and a forecast offset. The forecast offset denotes
 * how far in the future the time is from the reference time of the forecast.
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
public class NsharpTimeOperationElement extends NsharpOperationElement {

    /**
     * Placeholder used by {@link #getDisplayFormatter(List)} and
     * {@link #getDisplayText(DateTimeFormatter)} to denote the location of the
     * forecast information.
     */
    public static final String FORECAST_PLACEHOLDER = "Vxxx";

    private static final DateTimeFormatter PARSE_FORMAT = DateTimeFormatter
            .ofPattern("yyMMdd/HH[mm][ss](EEE)", Locale.US)
            .withZone(ZoneOffset.UTC);

    private final Instant time;

    private final Optional<Duration> fcstAmount;

    public NsharpTimeOperationElement(Instant time) {
        this(time, Optional.empty());
    }

    public NsharpTimeOperationElement(Instant time, Duration fcstAmount) {
        this(time, Optional.of(fcstAmount));
    }

    public NsharpTimeOperationElement(Instant time,
            Optional<Duration> fcstAmount) {
        this.time = time;
        this.fcstAmount = fcstAmount;
    }

    public Instant getTime() {
        return time;
    }

    public Optional<Duration> getFcstAmount() {
        return fcstAmount;
    }

    private String getForecastString() {
        return fcstAmount.map(d -> String.format("V%03d", d.toHours()))
                .orElse("");
    }

    @Override
    public String getDescription() {
        DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();
        builder.appendPattern("yyMMdd/HH");
        ZonedDateTime utc = time.atZone(ZoneOffset.UTC);
        if (utc.getSecond() != 0) {
            builder.appendValue(ChronoField.MINUTE_OF_HOUR, 2);
            builder.appendValue(ChronoField.SECOND_OF_MINUTE, 2);
        } else if (utc.getMinute() != 0) {
            builder.appendValue(ChronoField.MINUTE_OF_HOUR, 2);
        }
        builder.appendLiteral('(');
        builder.appendText(ChronoField.DAY_OF_WEEK, TextStyle.SHORT);
        builder.appendLiteral(')');
        if (fcstAmount.isPresent()) {
            builder.appendLiteral(getForecastString());
        }
        return builder.toFormatter().format(utc);
    }

    /**
     * Format this object using the supplied formatter. If the formatter
     * contains a literal instance of {@link #FORECAST_PLACEHOLDER} it will be
     * replaced with the forecast amount in hours if it is present.
     * 
     * @param formatter
     *            the formatter to use.
     * @return formatted text
     * @see #getDisplayFormatter(List)
     */
    public String getDisplayText(DateTimeFormatter formatter) {
        String result = formatter.format(time);
        return result.replace(FORECAST_PLACEHOLDER, getForecastString());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((fcstAmount == null) ? 0 : fcstAmount.hashCode());
        result = prime * result + ((time == null) ? 0 : time.hashCode());
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
        NsharpTimeOperationElement other = (NsharpTimeOperationElement) obj;
        if (fcstAmount == null) {
            if (other.fcstAmount != null)
                return false;
        } else if (!fcstAmount.equals(other.fcstAmount))
            return false;
        if (time == null) {
            if (other.time != null)
                return false;
        } else if (!time.equals(other.time))
            return false;
        return true;
    }

    @Override
    public int compareTo(NsharpOperationElement o) {
        if (o instanceof NsharpTimeOperationElement) {
            NsharpTimeOperationElement other = (NsharpTimeOperationElement) o;
            int result = time.compareTo(other.time);
            if (result == 0) {
                if (fcstAmount.isPresent()) {
                    if (other.fcstAmount.isPresent()) {
                        result = fcstAmount.get()
                                .compareTo(other.fcstAmount.get());
                    } else {
                        return -1;
                    }
                } else if (other.fcstAmount.isPresent()) {
                    return 1;
                }
            }
            return result;
        }
        return super.compareTo(o);
    }

    public static NsharpTimeOperationElement parse(String desc) {
        Optional<Duration> fcstAmount;
        if (desc.contains("V")) {
            String[] parts = desc.split("V");
            /*
             * Input data is inconsistent, sometimes the day of week and the V
             * hour is swapped. "yyMMdd/HH(EEE)Vvvv" vs. "yyMMdd/HHVvvv(EEE)".
             * To handle this just pull out the V hour and stick what is left
             * back together.
             */
            desc = parts[0] + parts[1].substring(3);
            long delta = Integer.parseInt(parts[1].substring(0, 3));
            fcstAmount = Optional.of(Duration.ofHours(delta));
        } else {
            fcstAmount = Optional.empty();
        }
        Instant time = ZonedDateTime.parse(desc, PARSE_FORMAT).toInstant();
        return new NsharpTimeOperationElement(time, fcstAmount);
    }

    /**
     * Determine the best format to use for displaying a list of elements. The
     * preferred format is always a concise format only displaying the day and
     * hour. If any of the time elements contain non-zero minutes then minutes
     * is also used and if this is not enough to differentiate 2 elements then
     * seconds will also be included. The returned format will contain a
     * {@link #FORECAST_PLACEHOLDER} so that forecast information can be added
     * if necessary.
     * 
     * @param elements the elements to be considered.
     * @return the format to use for the elements
     * @see #getDisplayText(DateTimeFormatter)
     */
    public static DateTimeFormatter getDisplayFormatter(
            List<NsharpTimeOperationElement> elements) {
        Set<NsharpTimeOperationElement> truncSet = new HashSet<>();
        boolean needsMinutes = false;
        for (NsharpTimeOperationElement element : elements) {
            ZonedDateTime utc = element.time.atZone(ZoneOffset.UTC);
            if (utc.getMinute() != 0) {
                needsMinutes = true;
            }
            ZonedDateTime trunc = utc.truncatedTo(ChronoUnit.MINUTES);
            if (trunc.equals(utc)) {
                truncSet.add(element);
            } else {
                truncSet.add(new NsharpTimeOperationElement(trunc.toInstant(),
                        element.fcstAmount));
            }
        }
        boolean needsSeconds = truncSet.size() < elements.size();

        DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();
        builder.appendValue(ChronoField.DAY_OF_MONTH, 2);
        builder.appendLiteral(".");
        builder.appendValue(ChronoField.HOUR_OF_DAY, 2);
        if (needsSeconds) {
            builder.appendLiteral(':');
            builder.appendValue(ChronoField.MINUTE_OF_HOUR, 2);
            builder.appendLiteral(':');
            builder.appendValue(ChronoField.SECOND_OF_MINUTE, 2);
        } else if (needsMinutes) {
            builder.appendValue(ChronoField.MINUTE_OF_HOUR, 2);
        }
        builder.appendLiteral(FORECAST_PLACEHOLDER);
        builder.appendLiteral('(');
        builder.appendText(ChronoField.DAY_OF_WEEK, TextStyle.SHORT);
        builder.appendLiteral(')');
        return builder.toFormatter().withZone(ZoneOffset.UTC);
    }

}
