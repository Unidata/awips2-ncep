package gov.noaa.nws.ncep.viz.common.area;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.radarapps.core.RadarApps;

/**
 * 
 * This class provides common functionality for image-centered areas. An image
 * centered area is positioned and zoomed on the specific coordinates (lat/lon)
 * for the selected area by resource (Local Radar).
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ----------   --------   ----------  --------------------------
 * 02/15/2017   R21992     mkean       created.
 * 
 * </pre>
 * 
 * @author mkean
 * @version 1.0
 */

public class AreaCenteredLocalRadar {

    protected static final transient IUFStatusHandler statusHandler =
            UFStatus.getHandler(AreaCenteredLocalRadar.class);

    public static final String CENTERED_AREA_NAME = "Centered-LocalRadar";

    public static final String LOCAL_RADAR_RESOURCE = "RADAR/LocalRadar";

    private static final Pattern LOCATION_RDA_PATTERN =
            Pattern.compile(LOCAL_RADAR_RESOURCE + ":([^/]*)");

    /**
     * parse the location rda id for a local radar station from the full
     * resource/area name. (Rda is an acronym for "Radar Data Acquisition".)
     * 
     * @param fullName:
     *            detailed selected area name String to be parsed, passed into
     *            method
     * @return rdaId String: the local radar rda location key field, parsed from
     *         the given fullname String
     */
    public static String parseLocationRdaId(String fullName) {

        String locationRdaId = "";

        // capture characters after radar token, stop at slash
        Matcher matcher = LOCATION_RDA_PATTERN.matcher(fullName);

        while (matcher.find()) {

            try {
                locationRdaId = matcher.group(1);

            } catch (IllegalStateException ex) {

                statusHandler.handle(Priority.PROBLEM,
                        "IllegalStateException: Error parsing location from resource.",
                        ex);

            } catch (IndexOutOfBoundsException ex) {

                statusHandler.handle(Priority.PROBLEM,
                        "IndexOutOfBoundsException: Error parsing location from resource.",
                        ex);
            }
        }
        return locationRdaId;
    }

    /**
     * check the given PredefinedArea name and if its centred return true,
     * otherwise return false
     * 
     * @param area
     *            - given PredefinedArea to be checked
     * @return - boolean is true it the area name indicates it is centered
     */
    public static boolean isCentered(PredefinedArea area) {
        return area.getAreaName().equalsIgnoreCase(CENTERED_AREA_NAME);
    }

    /**
     * Performs coordinate adjustment on given the PredefinedArea if required
     * for this resource
     * 
     * @param area
     *            - given PredefinedArea, to be adjusted/centered
     */
    public static void adjustCentredArea(PredefinedArea area) {

        // handle Local Radar specific coordinate adjustments
        if (area.getFullAreaMenuName().matches(LOCAL_RADAR_RESOURCE + ".*")) {

            String rdaId = parseLocationRdaId(area.getFullAreaMenuName());

            double mapCenterFromArea[] = area.getMapCenter();
            float mapCenterFromRadar[] = RadarApps.getRadarLocation(rdaId);

            mapCenterFromArea[0] = mapCenterFromRadar[1];
            mapCenterFromArea[1] = mapCenterFromRadar[0];

            // update area coordinates
            area.setMapCenter(mapCenterFromArea);
        }
    }

}