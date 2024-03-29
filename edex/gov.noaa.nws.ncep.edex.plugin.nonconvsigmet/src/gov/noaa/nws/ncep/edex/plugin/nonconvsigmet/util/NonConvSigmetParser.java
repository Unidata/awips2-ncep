package gov.noaa.nws.ncep.edex.plugin.nonconvsigmet.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.raytheon.edex.esb.Headers;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.time.TimeRange;
import com.raytheon.uf.common.wmo.WMOHeader;
import com.raytheon.uf.common.wmo.WMOTimeParser;
import com.raytheon.uf.edex.decodertools.core.LatLonPoint;

import gov.noaa.nws.ncep.common.dataplugin.nonconvsigmet.NonConvSigmetLocation;
import gov.noaa.nws.ncep.common.dataplugin.nonconvsigmet.NonConvSigmetRecord;
import gov.noaa.nws.ncep.edex.tools.decoder.LatLonLocTbl;
import gov.noaa.nws.ncep.edex.util.UtilN;

/**
 * Non-Convective Significant Meteorological Information DecoderUtil
 *
 * This java class intends to serve as a decoder utility for NonConvsigmet.
 *
 * This code has been developed by the SIB for use in the AWIPS2 system.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer     Description
 * ------------- -------- ------------ -----------------------------------------
 * Jun 2009      ????     Uma Josyula  Initial creation
 * Jul 2011      ????     F. J. Yen    Fix for RTN TTR 9973--ConvSigment Decoder
 *                                     Ignoring time range (NonConvsigmet, too).
 *                                     Set the rangeEnd time to the endTime
 * Jan 17, 2014           njensen      Handle if one or more locations not found
 *                                     in LatLonLocTbl
 * May 14, 2014  2536     bclement     moved WMO Header to common, removed
 *                                     TimeTools usage
 * Jul 15, 2020  8191     randerso     Updated for changes to LatLonPoint
 *
 * </pre>
 *
 * @author Uma Josyula
 */
public class NonConvSigmetParser {

    /**
     * Constructor
     */
    public NonConvSigmetParser() {
    }

    /**
     * Parse the WMO line and store WMO header, OfficeID, issue time,
     * designatorBBB,...
     *
     * @param wmoline
     *            The bulletin message
     *
     * @return a NonConvsigmetRecord
     */
    public static NonConvSigmetRecord processWMO(String wmoline,
            Headers headers) {

        NonConvSigmetRecord currentRecord = null;
        // Regular expression for WMO/ICAO, station ID, and issue date (and
        // maybe designator BBB)
        final String WMO_EXP = "([A-Z]{4}[0-9]{2}) ([A-Z]{4}) ([0-9]{6})( ([A-Z]{3}))?";

        // Pattern used for extracting WMO header,issue office and issue date
        // and designatorBBB
        final Pattern wmoPattern = Pattern.compile(WMO_EXP);
        Matcher theMatcher = wmoPattern.matcher(wmoline);

        if (theMatcher.find()) {
            currentRecord = new NonConvSigmetRecord();

            currentRecord.setWmoHeader(theMatcher.group(1));
            currentRecord.setIssueOffice(theMatcher.group(2));
            currentRecord.setDesignatorBBB(theMatcher.group(5));

            // Decode the issue time.
            String fileName = (String) headers.get(WMOHeader.INGEST_FILE_NAME);
            Calendar issueTime = WMOTimeParser.findDataTime(theMatcher.group(3),
                    fileName);
            currentRecord.setIssueTime(issueTime);

            /*
             * 999999999999999999999999999999 DataTime dataTime = new
             * DataTime(issueTime); currentRecord.setDataTime(dataTime); 999
             */
        }
        return currentRecord;
    }

    /**
     * Obtains start time and end time and forecast Region from input report
     *
     * @param theBullMsg
     *            The bulletin message which contains start time and end time.
     *
     * @param NonConvSigmetRecord
     *
     * @return a NonConvsigmetRecord
     */

    public static NonConvSigmetRecord processStartEndTime(String theBullMsg,
            NonConvSigmetRecord nconvRecord, Headers headers) {
        final String STARTTIME_EXP = "([A-Z]{4}) WS ([0-9]{6})( [A-Z]{3})?";
        final String ENDTIME_EXP = "VALID UNTIL ([0-9]{6})";

        final Pattern starttimePattern = Pattern.compile(STARTTIME_EXP);
        final Pattern endtimePattern = Pattern.compile(ENDTIME_EXP);

        Calendar stTime = null;
        Calendar endTime = null;
        /*
         * Default equal to one hour if there is no valid time in report
         */

        NonConvSigmetRecord currentRecord = nconvRecord;

        String fileName = (String) headers.get(WMOHeader.INGEST_FILE_NAME);

        // Calculate the startTime
        Matcher theMatcher = starttimePattern.matcher(theBullMsg);
        if (theMatcher.find()) {
            currentRecord.setForecastRegion(theMatcher.group(1));
            stTime = WMOTimeParser.findDataTime(theMatcher.group(2), fileName);
        }
        if (stTime == null) {
            currentRecord.setStartTime(currentRecord.getIssueTime());
        } else {
            currentRecord.setStartTime(stTime);
        }

        // Calculate the endTime
        theMatcher = endtimePattern.matcher(theBullMsg);

        if (theMatcher.find()) {
            endTime = WMOTimeParser.findDataTime(theMatcher.group(1), fileName);
        }
        if (endTime == null) {
            endTime = currentRecord.getIssueTime();
            currentRecord.setEndTime(endTime);
        } else {
            currentRecord.setEndTime(endTime);
        }

        /* 9999999999999999999999999999999 */
        DataTime dataTime = new DataTime(stTime, new TimeRange(stTime.getTime(),
                endTime.getTimeInMillis() - stTime.getTimeInMillis()));
        currentRecord.setDataTime(dataTime);
        /* 999 */
        return currentRecord;
    }

    /**
     * Obtains SigmetId from input report
     *
     * @param theBullMsg
     *            The bulletin message.
     *
     * @param NonConvSigmetRecord
     *
     * @return a NonConvsigmetRecord
     */
    public static NonConvSigmetRecord processSigmetId(String theBullMsg,
            NonConvSigmetRecord nconvRecord) {
        final String SIGMETID_EXP = "SIGMET ([A-Z]{2,} [0-9]{1})";
        final Pattern sigPattern = Pattern.compile(SIGMETID_EXP);
        NonConvSigmetRecord currentRecord = nconvRecord;
        // Parse sigmetId
        Matcher theMatcher = sigPattern.matcher(theBullMsg);
        if (theMatcher.find()) {
            currentRecord.setSigmetId(theMatcher.group(1));
        }

        return currentRecord;
    }

    /**
     * Parse the phenomena... Hazard Type,Hazard Intensity,Hazard Cause,Hazard
     * Condition,FlightLevel1,FlightLevel2,SigmetId,AWIPSId,CorAmdTest,StateList
     *
     * @param theBullMsg
     *            from bulletin message
     *
     * @param a
     *            NonConvsigmetRecord
     *
     * @return a NonConvsigmetRecord
     */
    public static NonConvSigmetRecord processPhenomena(String theBullMsg,
            NonConvSigmetRecord nconvRecord) {

        final String FL_EXP1 = "BTN (FL)?([0-9]{3}) AND (FL)?([0-9]{3})";

        final String FL_EXP2 = "BLW (FL)?([0-9]{3})";

        final String HAZARDTYPE_EXP = " (TURB|ICGICIP|ICE) ";

        final String HAZARDINTS_EXP = "(OCNL [A-Z0-9]{2,})(BLW (FL)?([0-9]{3}).|BTN|TURB|ICGICIP|ICE|ICGIC|VA|DU)?";

        final String HAZARDCAUS_EXP = "OCNL ([\\w| ])*(.|DUE TO)?([\\w| ]*). CONDS";

        final String HAZARDCOND_EXP = "(CONDS [\\w| ]*)(\\x0d\\x0d\\x0a)?([\\w| ]*)([0-9]{4})Z";

        final String STATELIST_EXP = "(([A-Z]{2})( [A-Z]{2})*)\\x0d\\x0d\\x0a";

        final String AWIPSID_EXP = "([A-Z]{2}[0-9]{1}[A-Z]{1})( )*\\x0d\\x0d\\x0a";

        final String CORREMARK_EXP = "(COR|AMD|TEST)";

        final Pattern flPattern1 = Pattern.compile(FL_EXP1);

        final Pattern flPattern2 = Pattern.compile(FL_EXP2);

        final Pattern hzTypePattern = Pattern.compile(HAZARDTYPE_EXP);

        final Pattern hzIntsPattern = Pattern.compile(HAZARDINTS_EXP);

        final Pattern hzCausPattern = Pattern.compile(HAZARDCAUS_EXP);

        final Pattern hzCondPattern = Pattern.compile(HAZARDCOND_EXP);

        final Pattern stPattern = Pattern.compile(STATELIST_EXP);

        final Pattern awipsPattern = Pattern.compile(AWIPSID_EXP);

        final Pattern corRemarkPattern = Pattern.compile(CORREMARK_EXP);

        NonConvSigmetRecord currentRecord = nconvRecord;

        Matcher theMatcher = flPattern1.matcher(theBullMsg);

        if (theMatcher.find()) {
            currentRecord
                    .setFlightLevel2(Integer.parseInt(theMatcher.group(4)));
            currentRecord
                    .setFlightLevel1(Integer.parseInt(theMatcher.group(2)));
        } else {
            theMatcher = flPattern2.matcher(theBullMsg);
            if (theMatcher.find()) {
                currentRecord
                        .setFlightLevel1(Integer.parseInt(theMatcher.group(2)));
            }
        }
        theMatcher = hzCausPattern.matcher(theBullMsg);
        if (theMatcher.find()) {
            currentRecord.setHazardCause(theMatcher.group(3));
        }
        theMatcher = hzCondPattern.matcher(theBullMsg);
        if (theMatcher.find()) {
            currentRecord.setHazardCondition(theMatcher.group(0));
        }
        theMatcher = hzIntsPattern.matcher(theBullMsg);
        if (theMatcher.find()) {
            currentRecord.setHazardIntensity(theMatcher.group(1));
        }
        theMatcher = hzTypePattern.matcher(theBullMsg);
        if (theMatcher.find()) {
            if ("TURB".equals(theMatcher.group(1))) {
                currentRecord.setHazardType("TURBULENCE");
            } else {
                currentRecord.setHazardType(theMatcher.group(1));
            }
        }
        theMatcher = stPattern.matcher(theBullMsg);
        if (theMatcher.find()) {
            currentRecord.setStateList(theMatcher.group(1));
        }
        theMatcher = awipsPattern.matcher(theBullMsg);
        if (theMatcher.find()) {
            currentRecord.setAwipsId(theMatcher.group(1));
        }
        theMatcher = corRemarkPattern.matcher(theBullMsg);
        if (theMatcher.find()) {
            currentRecord.setCorrectionRemarks(theMatcher.group(0));
        }

        return currentRecord;

    }

    /**
     * Parse the location lines... Add location table to the section table if
     * any
     *
     * @param theBullMsg
     *            from bulletin message
     *
     * @param recordTable
     *            The record Table
     *
     * @return true if finds a location line
     */
    public static Boolean processLocation(String theBullMsg,
            NonConvSigmetRecord recordTable) {

        Boolean hasLocationLine = true;
        String locationDelimiter = "-| TO ";
        Set<String> terminationList = Set.of("OCNL", "SEV");

        LatLonPoint point;

        /*
         * throws away the first section which is not the "FROM" location
         */
        try (Scanner sclocations = new Scanner(theBullMsg)
                .useDelimiter("FROM")) {
            String locationRecord = sclocations.next();

            if (sclocations.hasNext()) {
                locationRecord = sclocations.next();

                String lines = " ";
                String curLine = null;
                List<String> locationList = new ArrayList<>();
                locationList.clear();
                Boolean notBreak = true;

                try (Scanner scLocationLine = new Scanner(locationRecord)
                        .useDelimiter("\\x0d\\x0d\\x0a")) {
                    while (scLocationLine.hasNext() && notBreak) {
                        // Get next location line
                        curLine = scLocationLine.next();
                        try (Scanner scLocationToken = new Scanner(curLine)) {
                            if (scLocationToken.hasNext()) {
                                // Check the first token from each line
                                String firstToken = scLocationToken.next();
                                if (terminationList.contains(firstToken)) {
                                    // terminate
                                    notBreak = false;
                                    break;
                                }
                            }
                        }
                        lines = lines.concat(" ").concat(curLine);
                    }
                }

                // Clean up the leading space
                lines = UtilN.removeLeadingWhiteSpaces(lines);

                // Parse the location lines by a "-" or "TO"
                locationList.clear();

                // Get all locations
                try (Scanner scLocation = new Scanner(lines)
                        .useDelimiter(locationDelimiter)) {
                    while (scLocation.hasNext()) {
                        locationList.add(scLocation.next());
                    }
                }

                // set locations to data base
                if (locationList.size() > 1) {
                    Integer idxLocation = 0;
                    for (String location : locationList) {
                        NonConvSigmetLocation currentLocation = new NonConvSigmetLocation();
                        currentLocation.setLocationLine(lines);
                        currentLocation.setLocation(location);
                        point = LatLonLocTbl.getLatLonPoint(location, "vors");
                        if (point != null) {
                            currentLocation.setIndex(idxLocation + 1);
                            idxLocation++;
                            currentLocation.setLatitude(point.getLatitude());
                            currentLocation.setLongitude(point.getLongitude());
                            recordTable
                                    .addNonConvSigmetLocation(currentLocation);
                        }
                    }
                    hasLocationLine = (idxLocation > 0);
                } else {
                    hasLocationLine = false;
                }

            } else {
                hasLocationLine = false;
            }
        }

        return hasLocationLine;
    }

    /**
     * process regular section of a non-convective sigmet report
     *
     * @return a NonConvsigmetRecord table
     */
    public static NonConvSigmetRecord processRecord(String theBullMsg,
            Headers headers) {
        final String CANCELSIGMET_EXP = "(CANCEL SIGMET|TEST)";
        final Pattern cancelSigPattern = Pattern.compile(CANCELSIGMET_EXP);
        Matcher theMatcher = cancelSigPattern.matcher(theBullMsg);
        // Decode the WMO Header
        NonConvSigmetRecord currentRecord = NonConvSigmetParser
                .processWMO(theBullMsg, headers);
        // Decode the sigmetId
        currentRecord = NonConvSigmetParser.processSigmetId(theBullMsg,
                currentRecord);
        // Decode the Start Time,End Time,ForecastRegion
        currentRecord = NonConvSigmetParser.processStartEndTime(theBullMsg,
                currentRecord, headers);
        // Decode the phenomena description
        currentRecord = NonConvSigmetParser.processPhenomena(theBullMsg,
                currentRecord);
        // Replace the special characters
        currentRecord.setBullMessage(
                theBullMsg.replace('\r', ' ').replace('\003', ' ')
                        .replace('\000', ' ').replace('\001', ' '));

        if (theMatcher.find()) {
            if ("UNKNOWN".equals(currentRecord.getHazardType())) {
                if ("TEST".equals(theMatcher.group(0))) {
                    currentRecord.setHazardType("TEST");
                } else {
                    currentRecord.setHazardType("CANCEL");
                }
            }
        } else {
            // Decode the locations
            NonConvSigmetParser.processLocation(theBullMsg, currentRecord);
        }
        return currentRecord;
    }

}
