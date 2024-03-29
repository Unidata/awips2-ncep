package gov.noaa.nws.ncep.viz.gempak.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.raytheon.uf.common.time.DataTime;

/**
 * Static methods to convert between AWIPS2 Date strings to GEMPAK DATTIM
 * string. Also contains helper methods to convert forecast time between time
 * hours and minutes representation to time in seconds.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 29, 2014            jbernier     Initial creation
 * 
 * </pre>
 * 
 * @author jbernier
 * @version 1.0
 */

public class CommonDateFormatUtil {

    public static final String AWIPS2_DATETIME_FORMAT = "YYYY-MM-dd HH:mm:ss.S",
            GEMPAK_DATTIM_FORMAT = "yyMMdd/HHmm";

    /**
     * 
     * Converts AWIPS2 date time string into GEMPAK DATTIM string
     * 
     * @param dbTime
     *            eg "2011-10-09 06:20:00.0 (1)"
     * @return eq "111009/0620f00100"
     */
    public static String dbtimeToDattim(String dbTime) {
        StringBuilder aDattimBuilder = new StringBuilder(dbTime.substring(0,
                AWIPS2_DATETIME_FORMAT.length()));

        aDattimBuilder.delete(0, 2);
        deleteAllChar(aDattimBuilder, "-");
        deleteAllChar(aDattimBuilder, ":");
        deleteAllChar(aDattimBuilder, ".0");
        int indexOfDateTimeDelimiter = (dbTime.contains("_") ? aDattimBuilder
                .indexOf("_") : aDattimBuilder.indexOf(" "));
        aDattimBuilder.replace(indexOfDateTimeDelimiter,
                indexOfDateTimeDelimiter + 1, "/");

        if (dbTime.contains("(") && dbTime.contains(")")) {
            aDattimBuilder.append("f").append(getForecastTimeString(dbTime));
        }
        return aDattimBuilder.toString();
    }

    private static void deleteAllChar(StringBuilder charSeq, String charToDelete) {
        int startIndex = charSeq.indexOf(charToDelete);
        while (startIndex >= 0 && startIndex < charSeq.length()) {
            int endIndex = startIndex + charToDelete.length();
            charSeq.delete(startIndex, endIndex);
            startIndex = charSeq.indexOf(charToDelete);
        }
    }

    /**
     * Returns date string in the format to query AWIPS2
     * 
     * @param queryDate
     *            eg a date object with date and time
     * @return eq "2011-10-09 06:20:00.0"
     */
    public static String dateToDbtimeQuery(Date queryDate) {
        if (queryDate == null) {
            return null;
        }
        SimpleDateFormat queryFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:00.0");
        queryFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        return queryFormat.format(queryDate);
    }

    /**
     * 
     * Converts GEMPAK DATTIM string into AWIPS2 date time string
     * 
     * @param aDattim
     *            eg "111009/0620f00100"
     * @return eq "2011-10-09 06:20:00.0 (1)"
     */
    public static String dattimToDbtime(String aDattim) {
        StringBuilder dbTimeBuilder = new StringBuilder(
                AWIPS2_DATETIME_FORMAT.length() + 10);
        String[] dateTime = aDattim.substring(0, GEMPAK_DATTIM_FORMAT.length())
                .split("/");
        int date = Integer.parseInt(dateTime[0]), time = Integer
                .parseInt(dateTime[1]);

        dbTimeBuilder.append(String.format("20%02d-%02d-%02d %02d:%02d:00.0",
                date / 10000, (date % 10000) / 100, date % 100, time / 100,
                time % 100));

        if ((aDattim = aDattim.toUpperCase()).contains("F")) {
            int spaceIndex = dbTimeBuilder.indexOf(" ");
            dbTimeBuilder.replace(spaceIndex, spaceIndex + 1, "_");
            dbTimeBuilder.append("_(")
                    .append(getForecastColonTimeString(aDattim)).append(")");
        }
        return dbTimeBuilder.toString();
    }

    /**
     * 
     * Utility method to return forecast time in seconds given either GEMPAK or
     * AWIPS2 date time strings
     * 
     * @param anyTimeString
     *            eg "140808/0620f00115" or "2014-08-08 06:20:00.0 (1:15)"
     * @return eg 4500
     */
    public static int getForecastTimeInSec(String anyTimeString) {
        anyTimeString = anyTimeString.toUpperCase();
        int forecastHrs = 0, forecastMins = 0;

        if (anyTimeString.contains("F")) {
            String forecastTime = anyTimeString.substring(anyTimeString
                    .indexOf("F") + 1);
            forecastHrs = Integer.parseInt(forecastTime.substring(0,
                    "000".length()));
            forecastMins = forecastTime.length() == 5 ? Integer
                    .parseInt(forecastTime.substring("000".length())) : 0;
        } else if (anyTimeString.contains("(") && anyTimeString.contains(")")) {
            String[] forecastTime = anyTimeString.substring(
                    anyTimeString.indexOf("(") + 1, anyTimeString.indexOf(")"))
                    .split(":");
            forecastHrs = Integer.parseInt(forecastTime[0]);
            forecastMins = forecastTime.length == 2 ? Integer
                    .parseInt(forecastTime[1]) : 0;
        } else {
            forecastHrs = 0;
            forecastMins = 0;
        }
        return forecastHrs * 3600 + forecastMins * 60;
    }

    /**
     * 
     * Utility method to return forecast time string formatted to HHHmm given
     * either GEMPAK or AWIPS2 date time format string
     * 
     * @param anyTimeFormat
     *            eg "140808/0620f00115" or "2014-08-08 06:20:00.0 (1:15)"
     * @return eg "00115"
     */
    public static String getForecastTimeString(String anyTimeFormat) {
        int forecastTimeInSec = getForecastTimeInSec(anyTimeFormat);

        return getForecastTimeString(forecastTimeInSec);
    }

    /**
     * 
     * Utility method to return forecast time string formatted to HHHmm given a
     * forecast time in seconds
     * 
     * @param forecastTimeInSec
     *            eg 4500
     * @return eg "00115"
     */
    public static String getForecastTimeString(int forecastTimeInSec) {
        int forecastHrs = forecastTimeInSec / 3600;
        int forecastMins = forecastTimeInSec % 3600 / 60;

        return String.format("%03d", forecastHrs)
                + (forecastMins > 0 ? String.format("%02d", forecastMins) : "");
    }

    /**
     * 
     * Utility method to return forecast time string formatted to HHH:mm given
     * either GEMPAK or AWIPS2 date time format string
     * 
     * @param anyTimeFormat
     *            eg "140808/0620f00115" or "2014-08-08 06:20:00.0 (1:15)"
     * @return eg "1:15"
     */
    public static String getForecastColonTimeString(String anyTimeFormat) {
        int forecastTimeInSec = getForecastTimeInSec(anyTimeFormat);

        return getForecastColonTimeString(forecastTimeInSec);
    }

    /**
     * 
     * Utility method to return forecast time string formatted to HHH:mm given a
     * forecast time in seconds
     * 
     * @param forecastTimeInSec
     *            eg 4500
     * @return eg "1:15"
     */
    public static String getForecastColonTimeString(int forecastTimeInSec) {
        int forecastHrs = forecastTimeInSec / 3600;
        int forecastMins = forecastTimeInSec % 3600 / 60;

        return String.valueOf(forecastHrs)
                + (forecastMins > 0 ? ":" + String.format("%02d", forecastMins)
                        : "");
    }

    /**
     * 
     * Utility method to return a cycle time string formatted to MM/HH given a
     * Date
     * 
     * @param dt
     *            - date object for any month on the 8th day of the month at 12
     *            midnight GMT
     * @return eg "08/00"
     */
    public static String getCycleTimeString(Date dt) {
        if (dt == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.setTime(dt);

        return String.format("%02d/%02d", cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.HOUR_OF_DAY));
    }

    /**
     * 
     * Utility method to return a cycle time string formatted to MM/HH given a
     * DataTime. This method will use the DataTime's reference time.
     * 
     * @param dt
     *            - DataTime object for any month on the 8th day of the month at
     *            12 midnight GMT
     * @return eg "08/00"
     */
    public static String getCycleTimeString(DataTime dataTime) {
        return getCycleTimeString(dataTime.getRefTime());
    }
}