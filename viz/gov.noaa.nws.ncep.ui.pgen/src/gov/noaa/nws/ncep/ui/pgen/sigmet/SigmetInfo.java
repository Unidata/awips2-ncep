/*
 * gov.noaa.nws.ncep.ui.pgen.sigmet.SigmetInfo
 *
 * September 2009
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.ui.pgen.sigmet;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.Query;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.FeatureIterator;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.datum.DefaultEllipsoid;
import org.opengis.feature.simple.SimpleFeature;

import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.PathManager;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.SingleTypeJAXBManager;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.map.IMapDescriptor;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

import gov.noaa.nws.ncep.edex.common.stationTables.Station;
import gov.noaa.nws.ncep.ui.pgen.PgenSession;
import gov.noaa.nws.ncep.ui.pgen.PgenStaticDataProvider;
import gov.noaa.nws.ncep.ui.pgen.PgenUtil;
import gov.noaa.nws.ncep.ui.pgen.elements.AbstractDrawableComponent;

/**
 * Utility class for sigmet.
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 09/09        160         G. Zhang    Initial Creation.
 * 12/09        182         G. Zhang    Added Snapping for ConvSigmet
 * 05/11        ?           J. Wu       Correct VOR text format for GFA
 * 07/11        ?           J. Wu       Comment out the message box in
 *                                         SnapVor.getSnapWithStation()
 * 08/11        ?           B. Yin      Fixed part of TTR 239.
 * 07/11        450         G. Hull     NcPathManager for station tables
 * 10/11        ?           J. Wu       Fixed non-snappable points for outlook.
 * 01/12        597         S. Gurung   Removed Snapping for ConvSigmet
 * 02/2012      #676        Q.Zhou      Fixed spellings in TREND_ARRAY and SIGMET_TYPES[0].
 * 02/2012      #675        Q.Zhou      Modified PHEN_MAP.
 * 02/2012      #597        S. Gurung   Moved snap functionalities to SnapUtil. Removed GUI snapping for Non ConvSigmet.
 * 02/2012                  S. Gurung   Moved back isSnapADC() and getNumOfCompassPts() to SigmetInfo from SnapUtil
 * 11/12        #893        J. Wu       TTR635 - Fix volcano in alphabetical breakdown order.
 * Mar 11, 2014 #2718       randerso    Changes for GeoTools 10.5
 * Apr 21, 2020 76155       ksunil      ID values for INTL is dependent on WMO value selected
 * May 10, 2020 78441       ksunil      Removed reference to KNHC
 * Apr 28, 2020 77667       smanoj      Flight Information Region (FIR) update.
 * May 22, 2020 78000       ksunil      New Tropical Cyclone UI components for Fcst
 * Jun 4,  2020 79256       ksunil      Series ID is now a function of Issuing Office
 * Jun 11, 2020 79243       smanoj      Added Caribbean and South American FIRs.
 * Feb 24, 2021 86827       srussell    Updated getIsolated() to use consistent
 *                                      units when making an ellipsoid
 * Mar 15, 2021 86159       srussell    Added isValidLatLonArray method and 1
 *                                      call to it.
 * Apr 08, 2021 90325       smanoj      CARSAM Backup WMO headers update.
 * 
 * </pre>
 *
 * @author gzhang
 */

public class SigmetInfo {

    private static final String INTL_SIGMENT_ISSUE_LIST_FILE = "ncep"
            + IPathManager.SEPARATOR + "pgen" + IPathManager.SEPARATOR
            + "IntlSigmetIssueList.xml";

    private static final String AWC_BACKUP_CARSAM_WMO_HEADERS = "ncep"
            + IPathManager.SEPARATOR + "pgen" + IPathManager.SEPARATOR
            + "AWCBackupCarSamWmoHeaders.xml";

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(SigmetInfo.class);

    public static List<Station> VOLCANO_STATION_LIST;

    public static IntlSigmetIssueList availableOffices;

    public static AWCBackupCarSamWmoHeaders awcBackupCarSamWmoHeaders;

    public static final String[] SIGMET_TYPES = new String[] { "INTL", "CONV",
            "NCON", "AIRM", "OUTL" };// should be consistent with plugin.xml

    public static final String GFA_TEXT = new String("GFA_TYPE");

    public static final Map<String, String[]> AREA_MAP = new HashMap<>();

    public static final Map<String, String[]> ID_MAP = new HashMap<>();

    public static final Map<String, String[]> PHEN_MAP = new HashMap<>();

    public static final String[] SPEED_ARRAY = new String[] { "5", "10", "15",
            "20", "25", "30", "35", "40", "45", "50" };

    public static final String[] ROUND_TO_ARRAY = new String[] { "0", "5", "15",
            "30" };

    public static final String[] DIRECT_ARRAY = new String[] { "N", "NNE", "NE",
            "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW",
            "NW", "NNW" };

    public static final String[] CAR_SAM_WMO = new String[] { "MMMX", "MUHA",
            "TTPP", "MDSD", "MKJP" };

    public static final String[] FIR_ARRAY = new String[] { "PAZA|ANCHORAGE",
            "KZHU|HOUSTON_OCEANIC", "KZMA|MIAMI_OCEANIC",
            "KZWY|NEW_YORK_OCEANIC", "KZAK|OAKLAND_OCEANIC", "TJZS|SAN_JUAN",
            "MMFO|MAZATLAN_OCEANIC", "MMFR|MEXICO", "MUFH|HABANA",
            "MKJK|KINGSTON", "MDCS|SANTO_DOMINGO", "TTZP|PIARCO" };

    public static final String[] FIR_PACIFIC = new String[] { "PAZA", "KZAK" };

    public static final String[] FIR_ATLANTIC = new String[] { "KZHU", "KZWY",
            "KZMA", "TJZS" };

    public static final String[] FIR_MEXICO = new String[] { "MMFO", "MMFR" };

    public static final String[] FIR_OTHER = new String[] { "MUFH", "MKJK",
            "MDCS", "TTZP" };

    public static final String[] TREND_ARRAY = new String[] { "-none-", "NC",
            "WKN", "INTSF" };

    public static final String[] REM_ARRAY = new String[] { "-none-",
            "BASED_ON_SATELLITE_OBS", "BASED_ON_ACFT_AND_SAT",
            "BASED_ON_LATST_ADVSRY", "BASED_ON_SAT_AND_LTG_OBS",
            "BASED_ON_SATELLITE_OBS_AND_LATEST_ADVSRY",
            "BASED_ON_LATEST_WASHINGTON_VAAC_ADVISORY", "BASED_ON_ACFT_RPT" };

    public static final String[] VOL_NAME_BUCKET_ARRAY = new String[] {
            "-Not_listed,_Enter_Name/Location-", "AA-AM", "AN-AZ", "B", "CA-CH",
            "CI-CZ", "D-E", "F", "G", "H", "I-J", "KA-KH", "KI-KZ", "L",
            "MA-MC", "ME-MZ", "N", "O-P", "Q-R", "SA-SE", "SF-SZ", "TA-TH",
            "TI-TZ", "U", "V-Z" };

    public static final String LINE_SEPERATER = ":::";

    public static Map<String, List<String>> VOLCANO_BUCKET_MAP;

    static {

        try {

            VOLCANO_STATION_LIST = PgenStaticDataProvider.getProvider()
                    .getVolcanoTbl().getStationList();

            VOLCANO_BUCKET_MAP = initVolBucketMap();

            availableOffices = readAvailableOfficesList();

            awcBackupCarSamWmoHeaders= readAWCBackupCarSamWmoHeaders();

            String[] officeNames = availableOffices.getOffices().stream()
                    .map(IssueOffice::getName).toArray(String[]::new);
            AREA_MAP.put(SIGMET_TYPES[0], officeNames);
            AREA_MAP.put(SIGMET_TYPES[1], new String[] { "KMKC" });
            AREA_MAP.put(SIGMET_TYPES[2], new String[] { "KSFO", "KSLC", "KCHI",
                    "KDFW", "KBOS", "KMIA", "PHNL", "PANC", "PAFA", "PAJN" });
            AREA_MAP.put(SIGMET_TYPES[3], new String[] { "KSFO", "KSLC", "KCHI",
                    "KDFW", "KBOS", "KMIA", "PHNL", "PANC", "PAFA", "PAJN" });
            AREA_MAP.put(SIGMET_TYPES[4], new String[] { "KSFO", "KSLC", "KCHI",
                    "KDFW", "KBOS", "KMIA", "PHNL", "PANC", "PAFA", "PAJN" });

            ID_MAP.put(SIGMET_TYPES[1],
                    new String[] { "EAST", "CENTRAL", "WEST" });
            ID_MAP.put(SIGMET_TYPES[2],
                    new String[] { "NOVEMBER", "OSCAR", "PAPA", "QUEBEC",
                            "ROMEO", "UNIFORM", "VICTOR", "WHISKEY", "XRAY",
                            "YANKEE" });
            ID_MAP.put(SIGMET_TYPES[3],
                    new String[] { "SIERRA", "TANGO", "ZULU" });
            ID_MAP.put(SIGMET_TYPES[4],
                    new String[] { "EAST", "CENTRAL", "WEST" });

            PHEN_MAP.put(SIGMET_TYPES[0],
                    new String[] { "FRQ_TS", "OBSC_TS", "EMBD_TS", "SQL_TS",
                            "SEV_TURB", "SEV_ICE", "VOLCANIC_ASH",
                            "TROPICAL_CYCLONE", "RDOACT_CLD" });

        } catch (Exception e) {
            System.out.println(" SigmetInfo initialize error: "
                    + e.getMessage().toString());
        }
    }

    public static String getSigmetTypeString(String pgenType) {
        if (pgenType == null || "".equals(pgenType)) {
            return SIGMET_TYPES[0];
        }
        for (String temp : SIGMET_TYPES) {
            if (pgenType.contains(temp)) {
                return temp;
            }
        }
        return SIGMET_TYPES[0]; // default INTL
    }

    private static AWCBackupCarSamWmoHeaders readAWCBackupCarSamWmoHeaders() {
        PathManager pm = (PathManager) PathManagerFactory.getPathManager();
        ILocalizationFile lFile = pm.getStaticLocalizationFile(
                LocalizationType.CAVE_STATIC, AWC_BACKUP_CARSAM_WMO_HEADERS);
        if (lFile != null) {

            try (InputStream is = lFile.openInputStream()) {

                SingleTypeJAXBManager<AWCBackupCarSamWmoHeaders> sTypeJAXB = SingleTypeJAXBManager
                        .createWithoutException(
                                AWCBackupCarSamWmoHeaders.class);

                AWCBackupCarSamWmoHeaders awcBackupCarSamWmoHeaders = sTypeJAXB
                        .unmarshalFromInputStream(is);

                return awcBackupCarSamWmoHeaders;

            } catch (SerializationException | LocalizationException
                    | IOException e) {
                statusHandler.error("Unable to read AWCBackupCarSamWmoHeaders", e);
            }
        }
        return new AWCBackupCarSamWmoHeaders();
    }

    private static IntlSigmetIssueList readAvailableOfficesList() {

        PathManager pm = (PathManager) PathManagerFactory.getPathManager();
        ILocalizationFile lFile = pm.getStaticLocalizationFile(
                LocalizationType.CAVE_STATIC, INTL_SIGMENT_ISSUE_LIST_FILE);
        if (lFile != null) {
            try (InputStream is = lFile.openInputStream()) {

                SingleTypeJAXBManager<IntlSigmetIssueList> sTypeJAXB = SingleTypeJAXBManager
                        .createWithoutException(IntlSigmetIssueList.class);

                IntlSigmetIssueList officeList = sTypeJAXB
                        .unmarshalFromInputStream(is);

                return officeList;

            } catch (SerializationException | LocalizationException
                    | IOException e) {
                statusHandler.error("Unable to read IntlSigmetIssueList", e);
            }
        }
        return new IntlSigmetIssueList();

    }

    public static String[] getSeriesIDs(String office) {
        for (IssueOffice ofc : availableOffices.getOffices()) {
            if (office.equals(ofc.getName())) {
                return (ofc.getSeriesIDs().split(","));
            }
        }
        return new String[0];

    }

    private static Map<String, List<String>> initVolBucketMap() {
        Map<String, List<String>> result = new HashMap<>();

        List<Station> volcanoStnList = SigmetInfo.VOLCANO_STATION_LIST;
        ArrayList<String> volcanoList = new ArrayList<>();
        for (Station s : volcanoStnList) {
            volcanoList.add(s.getStnname());
        }

        java.util.Collections.sort(volcanoList);

        // Put each volcano into its bucket in the drop-down menu.
        for (int ii = 1; ii < VOL_NAME_BUCKET_ARRAY.length; ii++) {
            String bktStr = VOL_NAME_BUCKET_ARRAY[ii];
            String[] keys = bktStr.toUpperCase().split("-");
            ArrayList<String> volSubList = new ArrayList<>();

            if (keys != null && keys.length >= 1) {

                char key0 = keys[0].charAt(0);
                char key1 = (keys[0].length() > 1) ? keys[0].charAt(1) : 'A';
                char key2 = (keys.length > 1) ? keys[1].charAt(0)
                        : keys[0].charAt(0);
                char key3 = (keys.length > 1 && keys[1].length() > 1)
                        ? keys[1].charAt(1) : 'Z';

                for (String volName : volcanoList) {
                    String volN = new String(volName).toUpperCase();

                    if (key0 <= volN.charAt(0) && volN.charAt(0) <= key2
                            && key1 <= volN.charAt(1)
                            && volN.charAt(1) <= key3) {
                        volSubList.add(volName);
                    }
                }
            }

            result.put(bktStr, volSubList);
        }

        return result;
    }

    public static boolean isVolcanoNameEntered(String name) {
        // if( name == null || "".equals(name.trim())) return false;

        Collection<List<String>> lists = VOLCANO_BUCKET_MAP.values();
        for (List<String> list : lists) {
            if (list != null && list.contains(name)) {
                return false;
            }
        }
        return true;
    }

    public static Polygon getPolygon(double[] latlonArray,
            IMapDescriptor mapDescriptor) {
        Coordinate[] coorArray = new Coordinate[latlonArray.length / 2];
        double[] point = new double[3];
        for (int i = 0, j = 0; i < latlonArray.length - 1
                && j < coorArray.length; i += 2, j++) {
            point = mapDescriptor.worldToPixel(
                    new double[] { latlonArray[i + 1], latlonArray[i], 0.0 });// lon
                                                                              // ,
                                                                              // lat
            coorArray[j] = new Coordinate(point[0], point[1]);
        }

        GeometryFactory gf = new GeometryFactory();
        return gf.createPolygon(gf.createLinearRing(coorArray),
                new LinearRing[] {});
    }

    public static Polygon getPolygon(Coordinate[] latlonArray,
            IMapDescriptor mapDescriptor) {
        Coordinate[] coorArray = latlonToPixelInCoor(latlonArray,
                mapDescriptor);
        if (!(coorArray.length > 3)) {
            coorArray = new Coordinate[] {};
        }

        GeometryFactory gf = new GeometryFactory();
        return gf.createPolygon(gf.createLinearRing(coorArray),
                new LinearRing[] {});
    }

    public static Coordinate[] latlonToPixelInCoor(Coordinate[] coor,
            IMapDescriptor mapDescriptor) {
        Coordinate[] result = new Coordinate[coor.length];
        double[][] temp = PgenUtil.latlonToPixel(coor, mapDescriptor);
        for (int i = 0; i < result.length; i++) {
            result[i] = new Coordinate(temp[i][0], temp[i][1]);
        }
        return result;
    }

    public static Coordinate latlonToPixelInCoor1(Coordinate coor,
            IMapDescriptor mapDescriptor) {
        return latlonToPixelInCoor(new Coordinate[] { coor }, mapDescriptor)[0];
    }

    public static Coordinate[] pixelsToCoorArray(double[][] pixels) {
        Coordinate[] result = new Coordinate[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            result[i] = new Coordinate(pixels[i][0], pixels[i][1]);
        }
        return result;
    }

    public static Coordinate[] getIsolated(Coordinate vertex,
            double widthInNautical, IMapDescriptor mapDescriptor) {

        Double widthInMeters = widthInNautical * PgenUtil.NM2M;

        double[] tmp = { vertex.x, vertex.y, 0.0 };
        double[] center = mapDescriptor.worldToPixel(tmp);

        // Ellipsoid with axis in meters
        GeodeticCalculator gc = new GeodeticCalculator(DefaultEllipsoid.WGS84);
        gc.setStartingGeographicPoint(tmp[0], tmp[1]);
        gc.setDirection(0.0, widthInMeters);

        double[] tmp2 = { gc.getDestinationGeographicPoint().getX(),
                gc.getDestinationGeographicPoint().getY(), 0.0 };
        double[] circum = mapDescriptor.worldToPixel(tmp2);

        int numpts = 360;
        double axisAngle = 0.0;

        double cosineAxis = Math.cos(-Math.toRadians(axisAngle));
        double sineAxis = Math.sin(-Math.toRadians(axisAngle));

        ArrayList<Coordinate> list = new ArrayList<>();

        double diff[] = { circum[0] - center[0], circum[1] - center[1] };
        double width = Math.sqrt((diff[0] * diff[0]) + (diff[1] * diff[1]));

        double angle = 0.0;

        for (int i = 0; i < numpts; i++) {
            double thisSine = Math.sin(-Math.toRadians(angle));
            double thisCosine = Math.cos(-Math.toRadians(angle));

            double[] temp = mapDescriptor.pixelToWorld(new double[] {
                    center[0] + width
                            * (cosineAxis * thisCosine - sineAxis * thisSine),
                    center[1] + width * (sineAxis * thisCosine
                            + cosineAxis * thisSine) });
            list.add(new Coordinate(temp[0], temp[1]));

            angle += 1.0;
        }
        return list.toArray(new Coordinate[] {});
    }

    public static Polygon getIsolatedPolygon(Coordinate vertex,
            double widthInNautical, IMapDescriptor mapDescriptor) {
        Coordinate[] isolated = getIsolated(vertex, widthInNautical,
                mapDescriptor);
        Coordinate[] ip = new Coordinate[isolated.length + 1];
        ip = Arrays.copyOf(isolated, isolated.length);
        ip[ip.length - 1] = isolated[0];
        return getPolygon(ip, mapDescriptor);
    }

    public static Polygon getSOLPolygon(Coordinate[] coors, String line,
            double width, IMapDescriptor mapDescriptor) {
        Coordinate[] ip = getSOLCoors(coors, line, width, mapDescriptor);
        Coordinate[] ipPlus = new Coordinate[ip.length + 1];
        ipPlus = Arrays.copyOf(ip, ipPlus.length);
        ipPlus[ipPlus.length - 1] = ip[0];
        return getPolygon(ipPlus, mapDescriptor);
    }

    public static Coordinate[] getSOLCoors(Coordinate[] pts, String lineType,
            double width, IMapDescriptor mapDescriptor) {
        GeodeticCalculator gc = new GeodeticCalculator(DefaultEllipsoid.WGS84);

        if (!"ESOL".equals(lineType)) {

            double azimuth = 0.0;
            if ("SOF".equalsIgnoreCase(lineType)) {
                azimuth = 180.0;
            } else if ("EOF".equalsIgnoreCase(lineType)) {
                azimuth = 90.0;
            } else if ("WOF".equalsIgnoreCase(lineType)) {
                azimuth = -90.0;
            }
            Coordinate[] sides = new Coordinate[pts.length + 2];
            sides[0] = pts[0];
            sides[sides.length - 1] = pts[pts.length - 1];

            for (int i = 0; i < pts.length; i++) {
                gc.setStartingGeographicPoint(pts[i].x, pts[i].y);
                gc.setDirection(azimuth, width);
                Point2D jpt = gc.getDestinationGeographicPoint();
                sides[i + 1] = new Coordinate(jpt.getX(), jpt.getY());
            }
            return sides;

        } else {// "ESOL"
            Coordinate[][] sides = getSides(pts, width);
            Coordinate[][] sidesWithArcIntsc = getSidesWithArcIntsc(
                    mapDescriptor, pts, sides[0], sides[1]);

            Coordinate[] result = new Coordinate[sidesWithArcIntsc[0].length
                    + sidesWithArcIntsc[1].length];
            System.arraycopy(sidesWithArcIntsc[0], 0, result, 0,
                    sidesWithArcIntsc[0].length);
            System.arraycopy(sidesWithArcIntsc[1], 0, result,
                    sidesWithArcIntsc[0].length, sidesWithArcIntsc[1].length);

            return result;
        }
    }

    public static double[] getAzimuth(Coordinate[] vertice) {
        double[] azimuthS = new double[vertice.length];
        GeodeticCalculator gc = new GeodeticCalculator(DefaultEllipsoid.WGS84);

        for (int i = 0; i < vertice.length; i++) {
            if (i > 0) {
                gc.setStartingGeographicPoint(vertice[i - 1].x,
                        vertice[i - 1].y);
                gc.setDestinationGeographicPoint(vertice[i].x, vertice[i].y);
                azimuthS[i - 1] = gc.getAzimuth();

                if (i == vertice.length - 1) {
                    azimuthS[i] = azimuthS[i - 1];
                }
            }
        }

        return azimuthS;
    }

    public static Coordinate[][] getSides(Coordinate[] vertice,
            double attrSigLineWidth) {
        double[] azimuthS = getAzimuth(vertice);
        GeodeticCalculator gc = new GeodeticCalculator(DefaultEllipsoid.WGS84);

        int sidesLength = vertice.length * 2;
        Coordinate[] sides = new Coordinate[sidesLength],
                sidesOther = new Coordinate[sidesLength];

        double azimuth = 0.0, a1 = 0.0, a2 = 0.0;

        for (int i = 0; i < sidesLength; i++) {

            if (i == 0) {
                sides[i] = vertice[i];
                sidesOther[i] = vertice[i];
                continue;
            }
            if (i > 2 && i == sidesLength - 1) {
                sides[i] = vertice[vertice.length - 1];
                sidesOther[i] = vertice[vertice.length - 1];
                break;
            }

            gc.setStartingGeographicPoint(vertice[i / 2].x, vertice[i / 2].y);
            azimuth = azimuthS[(i - 1) / 2];

            a1 = (azimuth > 90.0 && azimuth <= 180.0) ? azimuth - 270
                    : azimuth + 90.0;
            gc.setDirection(a1, attrSigLineWidth);
            Point2D jpt = gc.getDestinationGeographicPoint();
            sides[i] = new Coordinate(jpt.getX(), jpt.getY());

            a2 = (azimuth < -90.0 && azimuth >= -180.0) ? azimuth + 270
                    : azimuth - 90.0;
            gc.setDirection(a2, attrSigLineWidth);
            Point2D jptOther = gc.getDestinationGeographicPoint();
            sidesOther[i] = new Coordinate(jptOther.getX(), jptOther.getY());

        }

        return new Coordinate[][] { sides, sidesOther };
    }

    public static Coordinate[][] getSidesWithArcIntsc(
            IMapDescriptor mapDescriptor, Coordinate[] vertice,
            Coordinate[] sides, Coordinate[] sidesOther) {
        ArrayList<Coordinate> sidesList = new ArrayList<>();
        ArrayList<Coordinate> sidesOtherList = new ArrayList<>();
        sidesList.add(sides[0]);
        sidesOtherList.add(sidesOther[0]);
        sidesList.add(sides[1]);
        sidesOtherList.add(sidesOther[1]);

        for (int i = 1; i < sides.length - 4; i = i + 2) {

            LineSegment ls1 = new LineSegment(
                    latlonToPixelInCoor1(sides[i], mapDescriptor),
                    latlonToPixelInCoor1(sides[i + 1], mapDescriptor));
            LineSegment ls2 = new LineSegment(
                    latlonToPixelInCoor1(sides[i + 2], mapDescriptor),
                    latlonToPixelInCoor1(sides[i + 3], mapDescriptor));
            Coordinate coor = ls1.intersection(ls2);

            LineSegment lsA = new LineSegment(
                    latlonToPixelInCoor1(sidesOther[i], mapDescriptor),
                    latlonToPixelInCoor1(sidesOther[i + 1], mapDescriptor));
            LineSegment lsB = new LineSegment(
                    latlonToPixelInCoor1(sidesOther[i + 2], mapDescriptor),
                    latlonToPixelInCoor1(sidesOther[i + 3], mapDescriptor));
            Coordinate coor2 = lsA.intersection(lsB);

            if (coor != null) {
                double[] aPixel = mapDescriptor
                        .pixelToWorld(new double[] { coor.x, coor.y });
                coor = new Coordinate(aPixel[0], aPixel[1]);
                sidesList.add(coor);
            } else {
                ArrayList<Coordinate> list = getArcPath(mapDescriptor,
                        vertice[(i + 1) / 2], sides[i + 1], sides[i + 2]);
                sidesList.add(list != null && list.size() > 0 ? list.get(0)
                        : sides[i + 1]);
                sidesList.addAll(list);
                sidesList.add(list != null && list.size() > 0
                        ? list.get(list.size() - 1) : sides[i + 2]);
            }

            if (coor2 != null) {
                double[] bPixel = mapDescriptor
                        .pixelToWorld(new double[] { coor2.x, coor2.y });
                coor2 = new Coordinate(bPixel[0], bPixel[1]);
                sidesOtherList.add(coor2);
            } else {
                ArrayList<Coordinate> lhList = getArcPath(mapDescriptor,
                        vertice[(i + 1) / 2], sidesOther[i + 1],
                        sidesOther[i + 2]);
                sidesOtherList.add(lhList != null && lhList.size() > 0
                        ? lhList.get(lhList.size() - 1) : sidesOther[i + 1]);

                for (int ii = lhList.size() - 1; ii >= 0; ii--) {
                    sidesOtherList.add(lhList.get(ii));
                }
                sidesOtherList.add(lhList != null && lhList.size() > 0
                        ? lhList.get(0) : sidesOther[i + 2]);
            }
        }
        sidesList.add(sides[sides.length - 2]);
        sidesOtherList.add(sidesOther[sidesOther.length - 2]);

        sidesList.add(sides[sides.length - 1]);
        sidesOtherList.add(sidesOther[sidesOther.length - 1]);

        return new Coordinate[][] { sidesList.toArray(new Coordinate[] {}),
                sidesOtherList.toArray(new Coordinate[] {}) };
    }

    public static ArrayList<Coordinate> getArcPath(IMapDescriptor mapDescriptor,
            Coordinate vertex, Coordinate side1, Coordinate side2) {

        double[] tmp = { vertex.x, vertex.y, 0.0 };
        double[] tmp2 = { side1.x, side1.y, 0.0 };
        double[] tmp3 = { side2.x, side2.y, 0.0 };

        double[] center = mapDescriptor.worldToPixel(tmp);
        double[] circum = mapDescriptor.worldToPixel(tmp2);
        double[] circum2 = mapDescriptor.worldToPixel(tmp3);

        double axisAngle = Math.atan2((circum[1] - center[1]),
                (circum[0] - center[0]));
        double axisAngle2 = Math.atan2((circum2[1] - center[1]),
                (circum2[0] - center[0]));

        axisAngle = 360
                - Math.toDegrees((2 * Math.PI + axisAngle) % (2 * Math.PI));
        axisAngle2 = 360
                - Math.toDegrees((2 * Math.PI + axisAngle2) % (2 * Math.PI));
        int numpts = Math
                .abs((int) Math.round(getAngExt(axisAngle, axisAngle2)));
        axisAngle = getStartAngle(axisAngle, axisAngle2);

        double cosineAxis = Math.cos(-Math.toRadians(axisAngle));
        double sineAxis = Math.sin(-Math.toRadians(axisAngle));

        ArrayList<Coordinate> list = new ArrayList<>();

        double diff[] = { circum[0] - center[0], circum[1] - center[1] };
        double width = Math.sqrt((diff[0] * diff[0]) + (diff[1] * diff[1]));

        double angle = 0.0;

        for (int i = 0; i < numpts; i++) {
            double thisSine = Math.sin(-Math.toRadians(angle));
            double thisCosine = Math.cos(-Math.toRadians(angle));

            double[] temp = mapDescriptor.pixelToWorld(new double[] {
                    center[0] + width
                            * (cosineAxis * thisCosine - sineAxis * thisSine),
                    center[1] + width * (sineAxis * thisCosine
                            + cosineAxis * thisSine) });
            list.add(new Coordinate(temp[0], temp[1]));

            angle += 1.0;
        }
        return list;
    }

    public static double getStartAngle(double a1, double a2) {
        if (a2 < a1) {
            if (a1 - a2 <= 180.0) {
                return a2;
            } else {
                return a1;
            }
        } else {
            if (a2 - a1 <= 180.0) {
                return a1;
            } else {
                return a2;
            }
        }
    }

    public static double getAngExt(double a1, double a2) {
        double diff = a2 - a1;
        if (Math.abs(diff) <= 180.0) {
            return -Math.abs(diff);
        } else {
            return Math.abs(diff) - 360;
        }
    }

    public static boolean getAFOSflg() {
        String sigmetFMT = System.getenv("SIGMETFMT");

        if (sigmetFMT == null) {
            return false;
        }
        return "AFOS".equals(sigmetFMT);
    }

    public static Map<String, Polygon> initFirPolygonMapFromShapfile() {
        Map<String, Polygon> result = new HashMap<>();
        IMapDescriptor mapDescriptor = PgenSession.getInstance()
                .getPgenResource().getDescriptor();

        HashMap<String, Coordinate[]> firGeoMap = getGeometriesFromShapefile();

        for (String firId : firGeoMap.keySet()) {
            Coordinate[] coors = firGeoMap.get(firId);
            if (isValidLatLonArray(coors, mapDescriptor)) {
                result.put(firId, SigmetInfo.getPolygon(coors, mapDescriptor));
            }
        }

        return result;
    }

    /*-
     * Screen out latlon arrays with NAN for x or y coordinates.
     */
    public static boolean isValidLatLonArray(Coordinate[] latlonArray,
            IMapDescriptor mapDescriptor) {

        boolean isValidLatLonArray = false;

        Coordinate[] coorArray = latlonToPixelInCoor(latlonArray,
                mapDescriptor);

        for (Coordinate c : coorArray) {
            if (Double.isNaN(c.x) || Double.isNaN(c.y)) {
                isValidLatLonArray = false;
                return isValidLatLonArray;
            }
        }
        isValidLatLonArray = true;
        return isValidLatLonArray;
    }

    /*
     * based on gov.noaa.ncep.ui.locator.LocatorShapefileResource
     */
    private static HashMap<String, Coordinate[]> getGeometriesFromShapefile() {

        String[] LABEL_ATTR = new String[] { "FIR_ID" };

        FeatureIterator<SimpleFeature> featureIterator = null;
        HashMap<String, Coordinate[]> firGeoMap = new HashMap<>();
        ShapefileDataStore shapefileDataStore = null;
        String shapeField = null;

        try {
            File file = PgenStaticDataProvider.getProvider().getFirBoundsFile();
            shapefileDataStore = new ShapefileDataStore(file.toURI().toURL());

            shapeField = shapefileDataStore.getFeatureSource().getSchema()
                    .getGeometryDescriptor().getLocalName();
        } catch (Exception e) {
            System.out.println("------- Exception: " + e.getMessage());
        }

        String[] labelFields = LABEL_ATTR;

        try {

            String[] types = shapefileDataStore.getTypeNames();
            Query query = new Query();
            query.setTypeName(types[0]);

            String[] fields = new String[labelFields.length + 1];
            for (int i = 0; i < labelFields.length; i++) {
                fields[i] = labelFields[i];
            }
            fields[labelFields.length] = shapeField;

            query.setPropertyNames(fields);
            featureIterator = shapefileDataStore.getFeatureSource()
                    .getFeatures(query).features();

            while (featureIterator.hasNext()) {
                SimpleFeature f = featureIterator.next();
                Geometry g = (Geometry) f.getDefaultGeometry();
                firGeoMap.put(f.getAttribute("FIR_ID").toString(),
                        g.getCoordinates());
            }

        } catch (Exception e) {
            System.out.println("---------Exception: " + e.getMessage());
        } finally {
            if (featureIterator != null) {
                featureIterator.close();
            }
        }

        return firGeoMap;
    }

    public static double[][] getESOLArea(Coordinate[] side1, Coordinate[] side2,
            IMapDescriptor map) {
        Coordinate[] sides = new Coordinate[side1.length + side2.length];

        System.arraycopy(side1, 0, sides, 0, side1.length);

        List<Coordinate> list = Arrays.asList(side2);
        Collections.reverse(list);

        System.arraycopy(list.toArray(new Coordinate[] {}), 0, sides,
                side1.length, side2.length);

        GeometryFactory gf = new GeometryFactory();
        return PgenUtil.latlonToPixel(
                gf.createPolygon(gf.createLinearRing(sides), null)
                        .getCoordinates(),
                map);
    }

    /**
     * check if the adc needs snapping
     *
     * @param adc:
     *            the element to be checked
     * @return: true: snapping needed; false not needed
     */
    public static boolean isSnapADC(AbstractDrawableComponent adc) {
        return false;
    }

    /**
     * get the num of compass points for snapping need
     *
     * @param adc:
     *            the element to be snapped
     * @return int: the num of compass points for snapping
     */
    public static int getNumOfCompassPts(AbstractDrawableComponent adc) {
        return 16;
    }
}
