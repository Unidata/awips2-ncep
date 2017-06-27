package gov.noaa.nws.ncep.edex.plugin.ntrans.decoder;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NTRANS Decoder Calculator
 * 
 * This class provides support to the NtransDecoder in the decoding of legacy
 * NTRANS Metafiles.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#  Author      Description
 * ------------ -------- ----------- -------------------------------------
 * 05/23/2017   R32628   mkean       Initial coding
 *
 * </pre>
 * 
 * This code has been developed by the SIB for use in the AWIPS2 system.
 */
public class NtransDecoderCalculator {

    private final static String REGEX_FOR_PRODUCT_NAME = "[_ :\\.&,/-]+";

    private final static String REGEX_FOR_PRODUCT_NAME_BEGIN = "^-";

    private final static String REGEX_FOR_PRODUCT_NAME_END = "-$";

    private final Pattern PATTERN_PRODUCT_NAME_SPECIAL_CHARACTERS =
            Pattern.compile(REGEX_FOR_PRODUCT_NAME);

    private final Pattern PATTERN_PRODUCT_NAME_SPECIAL_CHARACTERS_BEGIN =
            Pattern.compile(REGEX_FOR_PRODUCT_NAME_BEGIN);

    private final Pattern PATTERN_PRODUCT_NAME_SPECIAL_CHARACTERS_END =
            Pattern.compile(REGEX_FOR_PRODUCT_NAME_END);

    public final static String EMPTY_STRING = "";

    /**
     * Calculates the hour used for setting the initial (reference) or cycle
     * time (ie: the time/hour the model was initialized)
     * 
     * @param Integer
     *            fcstHour
     * @param Integer
     *            hourFromFrameHeader
     * @return Integer initializationHour
     */
    protected Integer calulateInitializationHour(Integer forecastHour,
            Integer hourFromFrameHeader) {

        int initializationHour = 0;

        int numberOfDays = forecastHour / 24;
        int numberOfHoursLeftInDay = (forecastHour - (numberOfDays * 24));

        // new get absolute difference between hourFromFrameHeader
        initializationHour =
                Math.abs(hourFromFrameHeader - numberOfHoursLeftInDay);

        return initializationHour;
    }

    /**
     * 
     * Establish upper bound on what the specified time means. For F-type
     * string, that would be the cycle time. For V-type string, that would be
     * the valid time.
     * 
     * Starts with the decode (system) time... using the given Integer time
     * elements from the frame header (if specified), yearFromFrameHeader and
     * monthFromFrameHeader
     * 
     * @param Integer
     *            yearFromFrameHeader
     * @param Integer
     *            monthFromFrameHeader
     * @param NtransDecoder
     *            ntransDecoderIn
     * @return Calendar upperBoundTime
     */
    public Calendar calculateUpperBoundTime(Integer yearFromFrameHeader,
            Integer monthFromFrameHeader, NtransDecoder ntransDecoderIn) {

        // Start with the decode (system) time...
        Calendar upperBoundTime =
                Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        upperBoundTime.setTime(ntransDecoderIn.getDecodeTime().getTime());

        // YEAR: If specified in the frame header, that takes priority...
        if (yearFromFrameHeader != null) {
            upperBoundTime.set(Calendar.YEAR, yearFromFrameHeader);
        }
        // ...otherwise use value from file name, if available...
        else if (ntransDecoderIn.getYearFromFileName() != null) {
            upperBoundTime.set(Calendar.YEAR,
                    ntransDecoderIn.getYearFromFileName());
        }
        // ...otherwise defaults to the current year.

        // MONTH: If specified in the frame header, that takes priority...
        if (monthFromFrameHeader != null) {
            upperBoundTime.set(Calendar.MONTH,
                    CalendarMonth(monthFromFrameHeader));
        }
        // ...otherwise use value from file name, if available
        else if (ntransDecoderIn.getMonthFromFileName() != null) {
            upperBoundTime.set(Calendar.MONTH,
                    CalendarMonth(ntransDecoderIn.getMonthFromFileName()));
        }
        // ...otherwise defaults to current month

        // use value from file name, if available ... otherwise defaults to
        // current date
        if (ntransDecoderIn.getDateFromFileName() != null) {
            upperBoundTime.set(Calendar.DAY_OF_MONTH,
                    ntransDecoderIn.getDateFromFileName());
        }

        return upperBoundTime;
    }

    /**
     * 
     * Calculates the actual valid time, starting with the latest possible
     * time... ie: from the given Calendar upperBoundTime, the given Integer
     * time params from the frame header, monthFromFrameHeader,
     * dateFromFrameHeader and hourFromFrameHeader
     * 
     * @param Integer
     *            monthFromFrameHeader
     * @param Integer
     *            dateFromFrameHeader
     * @param Integer
     *            hourFromFrameHeader
     * @param Calendar
     *            upperBoundTime
     * @return Calendar calculatedCycleTime
     */
    public Calendar calculateCycleTime(Integer monthFromFrameHeader,
            Integer dateFromFrameHeader, Integer hourFromFrameHeader,
            Calendar upperBoundTime) {

        Calendar calculatedValidTime =
                Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        calculatedValidTime.setTime(upperBoundTime.getTime());

        // ..if greater than the latest possible valid date...
        int latestPossibleValidDate = upperBoundTime.get(Calendar.DAY_OF_MONTH);

        if (monthFromFrameHeader == null
                && dateFromFrameHeader > latestPossibleValidDate) {

            // ...then it must be that date in the PRIOR month...
            calculatedValidTime.add(Calendar.MONTH, -1);
        }

        // ...setting the date field to the specified date...
        // doing this here, after the latestPossibleValidDate logic above
        // has already adjusted the correct month, and now set the correct
        // number of days from the frame
        calculatedValidTime.set(Calendar.DATE, dateFromFrameHeader);

        // Now set the hour field to the specified hour...
        calculatedValidTime.set(Calendar.HOUR_OF_DAY, hourFromFrameHeader);

        // ...and finally set sub-hour fields all to zero
        calculatedValidTime = setSubHourToZero(calculatedValidTime);

        // Now calculate actual initial (reference) or cycle time
        Calendar calculatedCycleTime =
                Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        calculatedCycleTime.setTime(calculatedValidTime.getTime());

        return calculatedCycleTime;
    }

    /**
     * handle the special characters identified in the given product name
     * currentProductName, replace with a single dash, and return the
     * replacementProductName String
     * 
     * @param String
     *            currentProductName
     * @return String replacementProductName
     */
    public String
            handleProductNameSpecialCharacters(String currentProductName) {

        String replacementProductName = EMPTY_STRING;
        String replacementCharacter = "-";

        // match to identify set of possible special characters to be removed
        Matcher MATCHER_PRODUCT_NAME_SPECIAL_CHARACTERS =
                PATTERN_PRODUCT_NAME_SPECIAL_CHARACTERS
                        .matcher(currentProductName.trim());

        String replacementProductNameBegin =
                MATCHER_PRODUCT_NAME_SPECIAL_CHARACTERS
                        .replaceAll(replacementCharacter);

        // match to identify beginning special characters
        Matcher MATCHER_PRODUCT_NAME_SPECIAL_CHARACTERS_BEGIN =
                PATTERN_PRODUCT_NAME_SPECIAL_CHARACTERS_BEGIN
                        .matcher(replacementProductNameBegin);

        String replacementProductNameEnd =
                MATCHER_PRODUCT_NAME_SPECIAL_CHARACTERS_BEGIN
                        .replaceAll(EMPTY_STRING);

        // match to identify beginning special characters
        Matcher MATCHER_PRODUCT_NAME_SPECIAL_CHARACTERS_END =
                PATTERN_PRODUCT_NAME_SPECIAL_CHARACTERS_END
                        .matcher(replacementProductNameEnd);

        replacementProductName = MATCHER_PRODUCT_NAME_SPECIAL_CHARACTERS_END
                .replaceAll(EMPTY_STRING);

        return replacementProductName;
    }

    /**
     * Return CalendarMonth int (index starting from 0) for goodOldMonthNumber
     * int (starting at 1).
     * 
     * Just to be proper (since we don't control the values of these
     * "magic constants", and so can't assume they won't change out from under
     * us (say, in the unlikely but possible case that Calendar decides to go
     * with 1-based months (like the rest of the world) instead of 0-based ones.
     * (We're 0-based [array] here, too, but WE control the order, and shield it
     * from the caller.)
     * 
     * @param int
     *            goodOldMonthNumber
     * @return int CalendarMonthConstants
     */
    public int CalendarMonth(int goodOldMonthNumber) {

        final int[] CalendarMonthConstants = {
                // @formatter:off
                Calendar.JANUARY,
                Calendar.FEBRUARY,
                Calendar.MARCH, 
                Calendar.APRIL,
                Calendar.MAY,
                Calendar.JUNE,
                Calendar.JULY, 
                Calendar.AUGUST,
                Calendar.SEPTEMBER,
                Calendar.OCTOBER,
                Calendar.NOVEMBER,
                Calendar.DECEMBER };
              // @formatter:on

        return CalendarMonthConstants[goodOldMonthNumber - 1];
    }

    /**
     * Zeroing the sub hour fields in a Calendar is repeated several times in
     * the code.. so perform in a separate method.
     * 
     * @param Calendar
     *            calendarTimeToBeSet
     * @return Calendar calendarTimeToBeSet
     */
    public Calendar setSubHourToZero(Calendar calendarTimeToBeSet) {

        // set sub-hour fields all to zero
        calendarTimeToBeSet.set(Calendar.MINUTE, 0);
        calendarTimeToBeSet.set(Calendar.SECOND, 0);
        calendarTimeToBeSet.set(Calendar.MILLISECOND, 0);

        return calendarTimeToBeSet;
    }

    /**
     * Return an unsigned int for the given short (shorty)
     * 
     * @param short
     *            shorty
     * @return int
     */
    public int toUnsigned(short shorty) {

        return shorty & 0xffff;
    }

    /**
     * Return unsigned long for the given int (inty)
     * 
     * @param int
     *            inty
     * @return long
     */
    public long toUnsigned(int inty) {

        return inty & 0xffffffffL;
    }
}
