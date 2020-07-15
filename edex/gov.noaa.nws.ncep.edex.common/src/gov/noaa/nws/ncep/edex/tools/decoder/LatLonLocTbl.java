package gov.noaa.nws.ncep.edex.tools.decoder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.LocalizationUtil;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.edex.decodertools.core.LatLonPoint;

import gov.noaa.nws.ncep.edex.common.stationTables.IStationField.StationField;
import gov.noaa.nws.ncep.edex.common.stationTables.Station;
import gov.noaa.nws.ncep.edex.common.stationTables.StationTable;

/**
 * LatLonLocTbl - A Java class to define some known VORs and Intlsig talbes used
 * to define convective/nonconvective/airmet/intl SIGMET locations.
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#    Engineer    Description
 * ------------- ---------- ----------- ----------------------------------------
 * Jun 12, 2009  95/132     B. Hebbard  Initial creation.
 * Sep 10, 2009  39/87/114  L. Lin      Remove the temporary enum and add xml
 *                                      for VORs and Intlsig gempak tables.
 * Sep 30, 2009  3102       jkorman     Changed printlns to logging statements.
 * Jan 07, 2014             njensen     Better error messages
 * Jul 15, 2020  8191       randerso    Updated for changes to LatLonPoint
 *
 * </pre>
 *
 * This code has been developed by the SIB for use in the AWIPS2 system.
 *
 * @author L. Lin
 */
public class LatLonLocTbl {
    private static Logger logger = LoggerFactory.getLogger(LatLonLocTbl.class);

    private static StationTable myloc = null;

    private double latitude;

    private double longitude;

    private LatLonLocTbl(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public static void readLocTable(String tableName) throws Exception {

        final String VORS_TABLE = LocalizationUtil.join("ncep", "stns",
                "vors.xml");
        IPathManager manager = PathManagerFactory.getPathManager();

        LocalizationFile file = null;
        if ("vors".equals(tableName)) {
            file = manager.getStaticLocalizationFile(VORS_TABLE);
        }

        if (file != null) {
            myloc = new StationTable(file.getFile().getAbsolutePath());
        }

    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public LatLonPoint getLatLonPoint() {
        return new LatLonPoint(latitude, longitude);
    }

    private enum Direction {
        N, NNE, NE, ENE, E, ESE, SE, SSE, S, SSW, SW, WSW, W, WNW, NW, NNW;

        public double getDegrees() {
            return ordinal() * 22.5;
        }
    }

    /**
     * Given a relative reference string, returns a LatLonPoint
     * (com.raytheon.uf.edex.decodertools.core.LatLonPoint).
     *
     * @param location
     *            A String such as... "BOS" "20S EMI" "30 WNW BUM" " 40ENE HUH "
     *            ...referencing a VOR listed in AC 00-45F (Appendix F),
     *            optionally preceded by distance in nautical miles and 16-point
     *            compass direction string.
     * @param locTable
     *            A string such as "vors" referring to "vors" location table or
     *            "intlsig" referring to intl location table
     * @return The decoded location as a LatLonPoint; null on error (such as
     *         unrecognized VOR identifier or direction string).
     *
     */
    public static LatLonPoint getLatLonPoint(String location, String locTable) {
        LatLonPoint point = null;
        Station vor = null;
        // Wrap decoding in a try block, in case of exception on
        // one of the xml or direction enum, or other problems.

        try {
            location = location.trim();

            // VOR is always last 3 nonblank char of location
            String navaid = location.substring(location.length() - 3);

            // Read in the location table XML if not exists
            if (myloc == null) {
                readLocTable(locTable);
                logger.debug(" - read vors.xml to cache");
            }
            // Search station ID and return whole station record
            if (myloc != null) {
                logger.debug(" - navaid = " + navaid);
                vor = myloc.getStation(StationField.STID, navaid);
            } else {
                logger.debug(" - myloc is null");
            }

            // Get LatLonPoint from lat/lon
            if (vor != null) {
                point = new LatLonPoint(vor.getLatitude(), vor.getLongitude());
            } else {
                logger.warn(" - DID NOT find station ID " + navaid
                        + " in vors.xml");
            }

            // If there's an offset direction/bearing, process it
            if (location.length() > 3 && point != null) {
                String u = location.substring(0, location.length() - 3);

                Pattern p = Pattern.compile("^([0-9]+)\\s*([A-Z]+)");
                Matcher m = p.matcher(u);
                if (m.find()) {
                    String distanceStr = m.group(1);

                    String bearingStr = m.group(2);

                    int distanceNM = Integer.parseInt(distanceStr);

                    // LatLonPoint.positionOf thinks bearing is CCW, not CW...
                    double bearingDeg = Direction.valueOf(bearingStr)
                            .getDegrees();
                    point = point.positionOf(bearingDeg, distanceNM);
                    logger.debug(" - get a good latlon point");
                }
            }
            return point;
        } catch (Exception e) {
            logger.error("[Error decoding location in LatLonLocTbl:  "
                    + location + "]", e);
            return null;
        }
    }

}
