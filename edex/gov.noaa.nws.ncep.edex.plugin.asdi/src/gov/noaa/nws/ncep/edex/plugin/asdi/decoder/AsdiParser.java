/**
 *
 * AsdiParser
 * 
 * Called from AsdiDecoder, AsdiParser manages construction of ASDIRecord objects for the Decoder Plug-In.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 06/21/17     283         RCReynolds  Initial Creation
 * </pre>
 * 
 * @author RCReynolds
 * @version 1
 * 
 */

package gov.noaa.nws.ncep.edex.plugin.asdi.decoder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.raytheon.uf.common.time.DataTime;

import gov.noaa.nws.ncep.common.dataplugin.asdi.AsdiRecord;
import gov.noaa.nws.ncep.common.tools.IDecoderConstantsN;

public class AsdiParser {

    public Log logger = LogFactory.getLog(getClass());

    private static final String ASDI_MISSING = "-";

    /**
     * Constructor
     */
    public AsdiParser() {
    }

    /*
     * parse ASDI comma delimited records
     * 
     * @param asdiCsvRecord... CSV ASDI record
     * 
     * @param flightTime.. flight time, constructed in ASDI decoder
     * 
     * @return ASDI record
     */
    public static AsdiRecord processFields(String asdiCsvRecord,
            DataTime flightTime) {

        AsdiRecord record = null;

        String[] csvContent = asdiCsvRecord.split(",");

        // correct record length ?
        if (csvContent.length == 14) {

            record = new AsdiRecord();

            for (int csvValue = 0; csvValue < csvContent.length; csvValue++) {

                // set any missing values (hyphen) to an empty string
                if (csvContent[csvValue].contentEquals(ASDI_MISSING)) {
                    csvContent[csvValue] = "";
                    // special case for latitude and longitude; use
                    // IDecoderConstantsN.FLOAT_MISSING
                    if (csvValue == 3 || csvValue == 4) {
                        csvContent[csvValue] = Float
                                .toString(IDecoderConstantsN.FLOAT_MISSING);
                        // and for aircraft speed use
                        // IDecoderConstantsN.INTEGER_MISSING
                    } else if (csvValue == 11) {
                        csvContent[csvValue] = Integer
                                .toString(IDecoderConstantsN.INTEGER_MISSING);
                    }
                }

            }

            record.setDataTime(flightTime);
            record.setFlightNumber(csvContent[0].trim());
            record.setFlightType(csvContent[1].trim());
            record.setAircraftType(csvContent[2].trim());
            record.setLatitude(Float.parseFloat(csvContent[3].trim()));
            record.setLongitude(Float.parseFloat(csvContent[4].trim()));
            record.setDepartureAirport(csvContent[5].trim());
            record.setDepartureTime(csvContent[6].trim());
            record.setArrivalAirport(csvContent[7].trim());
            record.setArrivalTime(csvContent[8].trim());
            record.setAircraftAltitude(csvContent[9].trim());
            record.setEstimatedFlightDirection(csvContent[10].trim());
            record.setAircraftSpeed(Integer.parseInt(csvContent[11].trim()));
            record.setFlightStatus(csvContent[12].trim());
            record.setLastReportAge(csvContent[13].trim());
            record.setReportType("asdi");
        }
        return record;
    }
}
