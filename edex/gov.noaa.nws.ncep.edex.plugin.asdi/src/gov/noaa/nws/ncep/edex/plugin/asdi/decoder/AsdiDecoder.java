package gov.noaa.nws.ncep.edex.plugin.asdi.decoder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.raytheon.edex.exception.DecoderException;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.PluginException;
import com.raytheon.uf.common.time.DataTime;

import gov.noaa.nws.ncep.common.dataplugin.asdi.AsdiRecord;
import gov.noaa.nws.ncep.common.tools.IDecoderConstantsN;

/**
 * 
 * AsdiDecoder
 * 
 * Decoder implementation for ASDI Plug-In
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#  Engineer     Description
 * ------------ -------- -----------  --------------------------
* 03/07/2017   R28579     R.Reynolds  Initial coding.
 * 
 * </pre>
 * 
 * @author RC Reynolds
 * @version 1
 * 
 */

public class AsdiDecoder {

    AsdiRecord asdiRecord = null;

    InputStream inputStream = null;

    Log logger = LogFactory.getLog(getClass());

    /**
     * Constructor
     * 
     */
    public AsdiDecoder() {
    }

    /**
     * Decode ASDI ascii comma separated file
     * 
     * @param inputFile
     *            ASDI file
     * @return
     * @throws DecoderException
     * @throws PluginExceptionPluginDataObject...
     *             Abstract class from which all plugin specific data types
     *             inherit
     */
    public PluginDataObject[] decode(File inputFile)
            throws DecoderException, PluginException {

        String filename = null;

        filename = inputFile.getName();

        String fileContentsString;

        String flightRecords[];

        List<AsdiRecord> records = new ArrayList<>();

        try {

            DataTime flightTime = null;
            inputStream = new FileInputStream(inputFile);
            long inputFileCount = inputFile.length();

            if (inputFileCount > Integer.MAX_VALUE) {
                throw new DecoderException(
                        "Input file too big! " + inputFileCount + " bytes");

            }

            byte[] fileBytes = new byte[(int) inputFileCount];
            int fileBytesReadIn = inputStream.read(fileBytes);
            if (fileBytesReadIn != inputFileCount) {
                throw new DecoderException(
                        "Input file not read correctly/completely! "
                                + fileBytesReadIn + " of " + inputFileCount
                                + " bytes read");
            }

            // get flight time, valid for all records in this file
            flightTime = getFlightTime(filename);

            /*
             * ASDI ascii, csv records arrive in single string.
             */
            fileContentsString = new String(fileBytes);

            /*
             * Split the string on line separator (assume linux line separator)
             * and fill list with individual flight records.
             */
            flightRecords = fileContentsString.split("\\n");

            records = new ArrayList<>();

            for (int rec = 0; rec < flightRecords.length; rec++) {

                asdiRecord = AsdiParser.processFields(flightRecords[rec],
                        flightTime);

                if (isValidAsdiRecord()) {
                    updateAltitude();
                    records.add(asdiRecord);
                }
            }

            closeInputStream(inputFile);

            return records.toArray(new PluginDataObject[records.size()]);

        } catch (IOException ioe) {
            logger.error(
                    "Error decoding ASDI input file " + inputFile.getName(),
                    ioe);
        } catch (Exception e) {
            logger.error(
                    "Error decoding ASDI input file " + inputFile.getName(), e);
        } finally {
            closeInputStream(inputFile);
        }

        if (records.isEmpty()) {
            return new PluginDataObject[0];
        } else {
            PluginDataObject[] pdos = records.toArray(new PluginDataObject[0]);
            return pdos;
        }

    }

    /**
     * Close input stream
     * 
     * @param inputFile
     */
    private void closeInputStream(File inputFile) {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException ioe) {
                logger.error("Could not close input Stream for "
                        + inputFile.getName());
            }
        }
    }

    /*
     * 
     * The ASDI files are in a standard YYYYMMDD_HHMM readable format with HHMM
     * in Z time. YYYY is a 4-digit year MM is the 2-digit month: 01-12 DD is
     * the 2-digit day: 01-31 HH is the 2-digit hour:00-23, MM is the 2-digit
     * minute 00-59 return flight time; Get the time
     * 
     * @param filename... name of file, contains time information
     * 
     * @return
     */
    private DataTime getFlightTime(String filename) {
        Calendar cal = GregorianCalendar.getInstance();
        cal.setTimeInMillis(0);
        cal.set(Calendar.YEAR, Integer.parseInt(filename.substring(0, 4)));
        cal.set(Calendar.MONTH, Integer.parseInt(filename.substring(4, 6)) - 1);
        cal.set(Calendar.DAY_OF_MONTH,
                Integer.parseInt(filename.substring(6, 8)));
        cal.set(Calendar.HOUR_OF_DAY,
                Integer.parseInt(filename.substring(9, 11)));
        cal.set(Calendar.MINUTE, Integer.parseInt(filename.substring(11, 13)));
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return new DataTime(cal);
    }

    /**
     * validate ASDI Record
     * 
     * @return boolean
     */
    private boolean isValidAsdiRecord() {

        if (asdiRecord == null) {
            return false;
        }
        // must have a flight number
        if (!isFlightNumberValid(asdiRecord.getFlightNumber())) {
            return false;
        }

        // latitude and longitude must be present
        if (!areLatitudeAndLongitudeValid(asdiRecord.getLatitude(),
                asdiRecord.getLongitude())) {
            return false;
        }

        // a departure or arrival airport must be present
        if (!isDepartureOrArrivalAirportPresent(
                asdiRecord.getDepartureAirport(),
                asdiRecord.getArrivalAirport())) {
            return false;
        }

        // last report age and status must = "CUR" and "E" respectively
        if (!areLastReportAgeAndStatusValid(asdiRecord.getLastReportAge(),
                asdiRecord.getFlightStatus())) {
            return false;
        }

        return true;
    }

    /**
     * Update/correct altitude as per legacy code
     */
    public void updateAltitude() {

        /*
         * Remove a "C" from end of altitude and test if valid int.
         */

        String altitude = asdiRecord.getAircraftAltitude();

        altitude = altitude.replaceAll("C", "");

        // altitude is defaulted to zero
        try {
            Integer.parseInt(altitude);
        } catch (NumberFormatException ex) {
            altitude = "0";
        }

        if (altitude.isEmpty() || altitude.length() > 4)
            altitude = "0";

        asdiRecord.setAircraftAltitude(altitude);

    }

    /**
     * determine if last report age and status are "CUR" and "E" respectively
     * 
     * @param lastReportAge
     * @param status
     * @return
     */
    public Boolean areLastReportAgeAndStatusValid(String lastReportAge,
            String status) {
        if (!status.equalsIgnoreCase("E")
                || !lastReportAge.equalsIgnoreCase("CUR")) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * determine if flight number exists
     * 
     * @param flightNumber
     *            - Aircraft flight number
     * @return boolean
     */
    public Boolean isFlightNumberValid(String flightNumber) {
        if (flightNumber.isEmpty()) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * determine if both latitude and longitude exist
     * 
     * @param lat
     *            latitude of flight
     * @param lon
     *            longitude of flight
     * @return boolean
     */
    public Boolean areLatitudeAndLongitudeValid(float lat, float lon) {

        if (lat == IDecoderConstantsN.FLOAT_MISSING
                || lon == IDecoderConstantsN.FLOAT_MISSING) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * determine if at least one airport exists
     * 
     * @param departure
     *            - departure airport
     * @param arrival
     *            - arrival airport
     * @return boolean
     */
    public boolean isDepartureOrArrivalAirportPresent(String departure,
            String arrival) {
        if (departure.isEmpty() && arrival.isEmpty()) {
            return false;
        } else {
            return true;
        }
    }

}