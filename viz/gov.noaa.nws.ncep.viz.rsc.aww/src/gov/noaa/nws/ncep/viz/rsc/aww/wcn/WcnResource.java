package gov.noaa.nws.ncep.viz.rsc.aww.wcn;

import java.awt.Color;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.graphics.RGB;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.io.WKBReader;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.raytheon.uf.common.dataquery.requests.DbQueryRequest;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint.ConstraintType;
import com.raytheon.uf.common.dataquery.responses.DbQueryResponse;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.time.TimeRange;
import com.raytheon.uf.edex.decodertools.core.LatLonPoint;
import com.raytheon.uf.viz.core.DrawableString;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.HorizontalAlignment;
import com.raytheon.uf.viz.core.IGraphicsTarget.LineStyle;
import com.raytheon.uf.viz.core.IGraphicsTarget.TextStyle;
import com.raytheon.uf.viz.core.IGraphicsTarget.VerticalAlignment;
import com.raytheon.uf.viz.core.catalog.DirectDbQuery;
import com.raytheon.uf.viz.core.catalog.DirectDbQuery.QueryLanguage;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.IFont;
import com.raytheon.uf.viz.core.drawables.IShadedShape;
import com.raytheon.uf.viz.core.drawables.IWireframeShape;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.IMapDescriptor;
import com.raytheon.uf.viz.core.requests.ThriftClient;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.viz.core.rsc.jts.JTSCompiler;
import com.raytheon.viz.core.rsc.jts.JTSCompiler.PointStyle;
import com.raytheon.viz.ui.editor.AbstractEditor;

import gov.noaa.nws.ncep.common.dataplugin.aww.AwwFips;
import gov.noaa.nws.ncep.common.dataplugin.aww.AwwRecord;
import gov.noaa.nws.ncep.common.dataplugin.aww.AwwRecord.AwwReportType;
import gov.noaa.nws.ncep.common.dataplugin.aww.AwwUgc;
import gov.noaa.nws.ncep.common.dataplugin.aww.AwwVtec;
import gov.noaa.nws.ncep.edex.common.stationTables.IStationField;
import gov.noaa.nws.ncep.edex.common.stationTables.Station;
import gov.noaa.nws.ncep.edex.common.stationTables.StationTable;
import gov.noaa.nws.ncep.ui.pgen.display.DisplayElementFactory;
import gov.noaa.nws.ncep.ui.pgen.display.IDisplayable;
import gov.noaa.nws.ncep.ui.pgen.elements.Symbol;
import gov.noaa.nws.ncep.viz.common.ui.NmapCommon;
import gov.noaa.nws.ncep.viz.localization.NcPathManager;
import gov.noaa.nws.ncep.viz.localization.NcPathManager.NcPathConstants;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResource;
import gov.noaa.nws.ncep.viz.rsc.aww.query.WcnCountyQueryResult;
import gov.noaa.nws.ncep.viz.ui.display.NCMapDescriptor;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;

/**
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer       Description
 * ------------- -------- -------------- ---------------------------------------
 *                        Uma Josy       la Initial creation.
 * Oct 01, 2010  307      Greg Hull      implement processRecords and change to
 *                                       process WcnData as the IRscDataObj
 * Jan 10, 2011           Uma Josy       la Made changes to preprocess update
 *                                       and event date
 * Jul 28, 2011  450      Greg Hull      NcPathManager
 * Feb 16, 2012  555      S. Gurung      Added call to setAllFramesAsPopulated()
 *                                       in queryRecords().
 * May 23, 2012  785      Q. Zhou        Added getName for legend.
 * Aug 17, 2012  655      B. Hebba       d  Added paintProps as parameter to
 *                                       IDisplayable draw
 * Sep 30, 2012  857      Q. Zhou        Displayed watch number. Modified time
 *                                       string and alignment in
 *                                       drawTimeLabelWatchNumber(). Added
 *                                       label/time for union.  Modified fill
 *                                       alpha to 0.5. Remove constraint &
 *                                       metamap in initResource().
 * Aug 14, 2013  1028     G. Hull        Move to aww project. Use AwwReportType
 *                                       enum.
 * Sep 15, 2014  4637     J. Huber       Fixed a duplicate key issue in the
 *                                       display hash map as well as as logic
 *                                       error in parsing the ugc line where it
 *                                       started with a string that need to be
 *                                       re-parsed. Also incorporating changes
 *                                       made by Dimitry for actually handling
 *                                       the > in UGC as well as modifying times
 *                                       as appropriate.
 * Oct 16, 2014  4637     J. Huber       Fixed display label issue. Changed
 *                                       labeling mechanism from each WCN
 *                                       message to each county. Cleaned up
 *                                       deprecated methods as well. Changed
 *                                       gathering of reportType to read
 *                                       phenomena and significance from VTEC
 *                                       line.
 * Nov 05, 2015  5070     randerso       Adjust font sizes for dpi scaling
 * Mar 15, 2016  15560    K. Bugenhagen  Cleanup and local refactoring.
 * Jul 15, 2020  8191     randerso       Updated for changes to LatLonPoint
 *
 * </pre>
 *
 * @author ujosyula
 */

public class WcnResource
        extends AbstractNatlCntrsResource<WcnResourceData, NCMapDescriptor>
        implements INatlCntrsResource, IStationField {
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(WcnResource.class);

    public static final String QUERY_PREFIX_COUNTY = "select ST_AsBinary(the_geom) G, ST_AsBinary(the_geom_0_001) G1, state,countyname,fips from mapdata.countylowres where ";

    public static final String QUERY_PREFIX_MARINE_ZONE = "select ST_AsBinary(the_geom) G, ST_AsBinary(the_geom_0_001) G1, wfo,name,id from mapdata.mzlowres where ";

    private static final String QUERY_PREFIX_MZ_LATLONS = "select wfo,name,id, lat, lon from mapdata.mzlowres";

    private static final String ENDTIME_REGEX = "([0-9]{6})";

    private static final Pattern ENDTIME_PATTERN = Pattern
            .compile(ENDTIME_REGEX);

    private static final long TWELVE_HOURS_IN_MILLISECS = Duration.ofHours(12)
            .toMillis();

    private static final String WATCH_NUMBER_CONSTRAINT = "watchNumber";

    private IFont font;

    private StationTable stationTable;

    private WcnResourceData wcnRscData;

    private List<WcnRscDataObj> modifyList;

    // for pre-query the database
    private WcnCountyQueryResult queryResult;

    // for storing result of pre-calculation
    private IWireframeShape outlineShape;

    // for pre-calculate the IWiredframeShape
    private CountyResultJob crJob = new CountyResultJob("");

    // Area change flag
    private boolean areaChangeFlag = false;

    private RGB color = new RGB(155, 155, 155);

    public class WcnRscDataObj
            implements IRscDataObject, Comparable<WcnRscDataObj> {
        /** used as a key string */
        private String datauri;

        /** issue time from bulletin */
        private DataTime issueTime;

        private DataTime eventTime;

        private AwwReportType reportType;

        private int countyNumPoints;

        private float[] countyLat;

        private float[] countyLon;

        private List<LatLonPoint> countyPoints;

        private List<String> countyUgc;

        private List<String> countyNames;

        private List<String> stateNames;

        public List<String> countyFips = new ArrayList<>();

        private String eventType;

        /** watch number to be displayed */
        private String watchNumber;

        private String evTrack;

        private DataTime evEndTime;

        private DataTime origEndTime;

        private DataTime displayStart;

        private DataTime displayEnd;

        private String evOfficeId;

        private String evPhenomena;

        private String evProductClass;

        private String evSignificance;

        public boolean isCounty;

        public String getKey() {
            return getWcnRscDataObjKey(this);
        }

        @Override
        public DataTime getDataTime() {
            return eventTime;
        }

        @Override
        public int compareTo(WcnRscDataObj o) {
            return (int) (this.issueTime.getRefTime().getTime()
                    - o.issueTime.getRefTime().getTime());
        }
    }

    protected class FrameData extends AbstractFrameData {
        private Map<String, WcnRscDataObj> wcnDataMap;

        public FrameData(DataTime frameTime, int timeInt) {
            super(frameTime, timeInt);
            wcnDataMap = new HashMap<>();
        }

        @Override
        public boolean updateFrameData(IRscDataObject rscDataObj) {
            if (!(rscDataObj instanceof WcnRscDataObj)) {
                statusHandler
                        .warn("WcnResource.updateFrameData: expecting objects "
                                + " of type WcnRscDataObj???");
                return false;
            }

            WcnRscDataObj wcnRscDataObj = (WcnRscDataObj) rscDataObj;
            WcnRscDataObj existingWcnData = wcnDataMap
                    .get(wcnRscDataObj.datauri);

            // Make unique keys so that all ugc entries are mapped.

            if (existingWcnData == null
                    || wcnRscDataObj.issueTime
                            .greaterThan(existingWcnData.issueTime)
                    || wcnDataMap.containsKey(wcnRscDataObj.datauri)) {
                if (wcnDataMap.containsKey(wcnRscDataObj.datauri)) {
                    // Put all counties in the WCN into the hash map key to
                    // avoid iterator clean up issues
                    wcnDataMap.put(
                            wcnRscDataObj.datauri + "  "
                                    + String.join(" ",
                                            wcnRscDataObj.countyFips),
                            wcnRscDataObj);
                } else {
                    wcnDataMap.put(wcnRscDataObj.datauri, wcnRscDataObj);
                }
            }
            return true;
        }
    }

    public WcnResource(WcnResourceData rscData, LoadProperties loadProperties)
            throws VizException {
        super(rscData, loadProperties);
        wcnRscData = resourceData;
        modifyList = new ArrayList<>();
    }

    @Override
    protected AbstractFrameData createNewFrame(DataTime frameTime,
            int timeInt) {
        return new FrameData(frameTime, timeInt);
    }

    // turn the db record into an WarnRscDataObj which will be timeMatched and
    // added to one or more of the FrameData's.

    @Override
    protected IRscDataObject[] processRecord(Object pdo) {
        AwwRecord awwRecord = (AwwRecord) pdo;
        List<WcnRscDataObj> wcnDataList = getAwwtData(awwRecord);
        if (wcnDataList == null) {
            return new IRscDataObject[] {};
        } else {
            return wcnDataList.toArray(new WcnRscDataObj[0]);
        }
    }

    private void fipsRangeReparse(String inUgcPart, String countyname,
            List<String> outList) {

        String county = countyname;

        if (inUgcPart.length() == 10) { // "([A-Z]{3}[0-9]{3}[>][0-9]{3})"
            // Format: NAMDDD1>DDD2
            String intervalToken = inUgcPart.substring(3, 10);
            county = inUgcPart.substring(0, 3);
            fipsRangeTokenizer(intervalToken, county, outList);
        } else if (inUgcPart.length() == 7) { // "([0-9]{3}[>][0-9]{3})"
            // Format: DDD1>DDD2
            fipsRangeTokenizer(inUgcPart, county, outList);
        } else {
            outList.add(inUgcPart);
        }
    }

    private void fipsRangeTokenizer(String intervalToken, String county,
            List<String> outList) {
        final String inclusiveDelim = ">";

        StringTokenizer twoTokens = new StringTokenizer(intervalToken,
                inclusiveDelim);
        String firstToken = twoTokens.nextToken();
        String secondToken = twoTokens.nextToken();

        Integer countyBegin = Integer.parseInt(firstToken);
        Integer countyEnd = Integer.parseInt(secondToken);

        for (int counter = countyBegin; counter <= countyEnd; counter++) {
            String inclusiveToken = Integer.toString(counter);

            // set "1" to "001" ...etc
            if (counter < 10) {
                inclusiveToken = "00".concat(inclusiveToken);
            }
            // set "10" to "010" ...etc
            else if (counter < 100) {
                inclusiveToken = "0".concat(inclusiveToken);
            }
            String countyFips = county.concat(inclusiveToken);

            outList.add(countyFips);
        }
    }

    private ArrayList<WcnRscDataObj> getAwwtData(AwwRecord awwRecord) {
        WcnRscDataObj wcnStatusData = null;
        List<WcnRscDataObj> wcnDataList = new ArrayList<>();

        try {
            Set<AwwUgc> awwUgc = awwRecord.getAwwUGC();
            for (AwwUgc awwugcs : awwUgc) {
                wcnStatusData = new WcnRscDataObj();

                wcnStatusData.issueTime = new DataTime(
                        awwRecord.getIssueTime());
                wcnStatusData.datauri = awwRecord.getDataURI();
                wcnStatusData.isCounty = isCountyUgs(awwugcs);

                // get the ugc line to find the counties
                String ugcline = awwugcs.getUgc();
                if (ugcline != null && !ugcline.isEmpty()) {

                    wcnStatusData.watchNumber = awwugcs
                            .getEventTrackingNumber();
                    wcnStatusData.countyUgc = new ArrayList<>();

                    int i = 0;
                    String temp;
                    String countyname = ugcline.substring(0, 3);
                    StringTokenizer strugcs = new StringTokenizer(ugcline);
                    while (strugcs.hasMoreTokens()) {
                        temp = strugcs.nextToken("-");

                        boolean dontSkip = true;

                        Matcher endTimeMatcher = ENDTIME_PATTERN.matcher(temp);
                        if (endTimeMatcher.find()) {
                            dontSkip = false;
                        }

                        if (temp != null && dontSkip) {
                            if (temp.startsWith("\r\r\n")) {
                                String temp1 = temp.substring(3);
                                temp = temp1;
                            }

                            if (temp.contains(countyname)) {

                                if (temp.length() == 6) {
                                    if ((0 == Character.getNumericValue(
                                            temp.toCharArray()[3]))
                                            && (0 == Character.getNumericValue(
                                                    temp.toCharArray()[4]))
                                            && (0 == Character.getNumericValue(
                                                    temp.toCharArray()[5]))) {
                                    } else {
                                        (wcnStatusData.countyUgc).add(temp);
                                    }
                                } else {
                                    fipsRangeReparse(temp, countyname,
                                            (wcnStatusData.countyUgc));
                                }

                            } else if (temp.length() == 7) {
                                fipsRangeReparse(temp, countyname,
                                        (wcnStatusData.countyUgc));
                            } else {
                                if (!"".equalsIgnoreCase(temp) && Character
                                        .isLetter(temp.toCharArray()[0])) {
                                    countyname = temp.substring(0, 3);
                                    String temp2 = countyname.substring(0, 3)
                                            + temp.substring(3);
                                    (wcnStatusData.countyUgc).add(temp2);
                                }

                                String temp2 = countyname.substring(0, 3)
                                        + temp;
                                if (temp2.length() > 6) {
                                    if (9 == temp2.length()) {
                                        (wcnStatusData.countyUgc)
                                                .add(temp2.substring(3, 9));
                                    } else {
                                        fipsRangeReparse(temp2.substring(0, 6),
                                                countyname,
                                                (wcnStatusData.countyUgc));
                                    }
                                } else {
                                    if (!(6 > temp2.length())
                                            && (0 == Character.getNumericValue(
                                                    temp2.toCharArray()[3]))
                                            && (0 == Character.getNumericValue(
                                                    temp2.toCharArray()[4]))
                                            && (0 == Character.getNumericValue(
                                                    temp2.toCharArray()[5]))) {
                                        statusHandler
                                                .error("Error in county name: "
                                                        + temp2);
                                    } else {
                                        (wcnStatusData.countyUgc).add(temp2);
                                    }
                                }
                            }
                            i++;
                        }
                    }
                    if (i > 1) {
                        wcnStatusData.countyUgc
                                .remove(wcnStatusData.countyUgc.size() - 1);// cleanup
                    }

                    wcnStatusData = getCountyNameLatLon(wcnStatusData);
                }

                int vtechNumber = awwugcs.getAwwVtecLine().size();
                if (vtechNumber > 0) {
                    for (AwwVtec awwVtech : awwugcs.getAwwVtecLine()) {
                        wcnStatusData.eventType = awwVtech.getAction();
                        wcnStatusData.evTrack = awwVtech
                                .getEventTrackingNumber();
                        wcnStatusData.evEndTime = new DataTime(
                                awwVtech.getEventEndTime());
                        wcnStatusData.origEndTime = wcnStatusData.evEndTime;
                        wcnStatusData.displayStart = wcnStatusData.issueTime;
                        wcnStatusData.displayEnd = wcnStatusData.origEndTime;
                        wcnStatusData.evOfficeId = awwVtech.getOfficeID();
                        wcnStatusData.evPhenomena = awwVtech.getPhenomena();
                        wcnStatusData.evProductClass = awwVtech
                                .getProductClass();
                        wcnStatusData.evSignificance = awwVtech
                                .getSignificance();
                        // Create the report type from the VTEC phenomena
                        // and significance.
                        String buildReportType = wcnStatusData.evPhenomena + "."
                                + wcnStatusData.evSignificance;
                        switch (buildReportType.toUpperCase()) {
                        case "TO.A":
                            wcnStatusData.reportType = AwwReportType.TORNADO_WATCH;
                            break;
                        case "SV.A":
                            wcnStatusData.reportType = AwwReportType.SEVERE_THUNDERSTORM_WATCH;
                            break;
                        default:
                            wcnStatusData.reportType = AwwReportType
                                    .getReportType(awwRecord.getReportType());
                            if (awwRecord.getBullMessage()
                                    .contains("THUNDERSTORM")) {
                                wcnStatusData.reportType = AwwReportType.SEVERE_THUNDERSTORM_WATCH;// "THUNDERSTORM";
                            } else if (awwRecord.getBullMessage()
                                    .contains("TORNADO")) {
                                wcnStatusData.reportType = AwwReportType.TORNADO_WATCH;// "TORNADO";
                            }
                        }

                        if (Set.of("COR", "CAN", "NEW", "EXA", "EXB", "EXT",
                                "EXP", "CON")
                                .contains(awwVtech.getAction().toUpperCase())) {
                            modifyList.add(wcnStatusData);
                        }

                        if (awwVtech.getEventStartTime() != null
                                && awwVtech.getEventEndTime() != null) {
                            wcnStatusData.eventTime = new DataTime(
                                    awwVtech.getEventStartTime(),
                                    new TimeRange(awwVtech.getEventStartTime(),
                                            awwVtech.getEventEndTime()));

                        } else if (awwVtech.getEventEndTime() != null) {
                            wcnStatusData.eventTime = new DataTime(
                                    wcnStatusData.issueTime
                                            .getRefTimeAsCalendar(),
                                    new TimeRange(
                                            wcnStatusData.issueTime
                                                    .getRefTimeAsCalendar(),
                                            awwVtech.getEventEndTime()));

                        } else if (awwVtech.getEventStartTime() != null) {
                            wcnStatusData.eventTime = new DataTime(
                                    awwVtech.getEventStartTime(),
                                    new TimeRange(awwVtech.getEventStartTime(),
                                            wcnStatusData.issueTime
                                                    .getRefTimeAsCalendar()));
                        } else {
                            wcnStatusData.eventTime = wcnStatusData.issueTime;

                        }

                    }
                }

                wcnDataList.add(wcnStatusData);
            }

        } catch (NumberFormatException nfe) {
            statusHandler.error(
                    "Error in county name: " + nfe.getLocalizedMessage(), nfe);

        } catch (Exception e) {
            statusHandler.warn(e.getLocalizedMessage(), e);
        }

        preProcessFrameUpdate();

        return (ArrayList<WcnRscDataObj>) wcnDataList;
    }

    private WcnRscDataObj getCountyNameLatLon(WcnRscDataObj wdata) {
        wdata.countyPoints = new ArrayList<>();
        wdata.countyNames = new ArrayList<>();
        wdata.stateNames = new ArrayList<>();
        wdata.countyLat = new float[wdata.countyUgc.size()];
        wdata.countyLon = new float[wdata.countyUgc.size()];

        try {
            int i = 0;

            for (String theKey : wdata.countyUgc) {
                Station station = stationTable.getStation(StationField.STID,
                        theKey);
                if (station != null) {
                    LatLonPoint point = new LatLonPoint(station.getLatitude(),
                            station.getLongitude());
                    wdata.countyPoints.add(point);
                    wdata.countyNames.add(station.getStnname());
                    wdata.stateNames.add(station.getState());
                    wdata.countyLat[i] = station.getLatitude();
                    wdata.countyLon[i] = station.getLongitude();

                    if (wdata.isCounty) {
                        String s = station.getStnnum();
                        wdata.countyFips.add(s.length() == 4 ? "0" + s : s);
                    }

                } else {
                    List<Object[]> results = DirectDbQuery
                            .executeQuery(
                                    QUERY_PREFIX_MZ_LATLONS + " where id = '"
                                            + theKey + "'",
                                    "maps", QueryLanguage.SQL);

                    LatLonPoint point = new LatLonPoint(
                            Float.parseFloat(results.get(0)[3].toString()),
                            Float.parseFloat(results.get(0)[4].toString()));
                    wdata.countyPoints.add(point);
                    wdata.countyNames.add(results.get(0)[1].toString());
                    wdata.stateNames.add("");
                    wdata.countyLat[i] = Float
                            .parseFloat(results.get(0)[3].toString());
                    wdata.countyLon[i] = Float
                            .parseFloat(results.get(0)[4].toString());
                    wdata.countyFips.add(results.get(0)[2].toString());
                }
                i++;
            }
        } catch (IndexOutOfBoundsException idxOobEx) {
            statusHandler.debug(idxOobEx.getLocalizedMessage(), idxOobEx);
        } catch (Exception e) {
            statusHandler.warn(e.getLocalizedMessage(), e);
        }

        wdata.countyNumPoints = wdata.countyNames.size();

        return wdata;
    }

    @Override
    protected boolean preProcessFrameUpdate() {

        modifyQueue();

        return true;
    }

    private void modifyQueue() {
        if (modifyList != null) {
            for (WcnRscDataObj modify : modifyList) {
                for (IRscDataObject rscDataObj : newRscDataObjsQueue) {
                    WcnRscDataObj candidate = (WcnRscDataObj) rscDataObj;

                    if (modify.evTrack.equalsIgnoreCase(candidate.evTrack)
                            && modify.evOfficeId
                                    .equalsIgnoreCase(candidate.evOfficeId)
                            && modify.evPhenomena
                                    .equalsIgnoreCase(candidate.evPhenomena)
                            && modify.evProductClass
                                    .equalsIgnoreCase(candidate.evProductClass)
                            && modify.evSignificance.equalsIgnoreCase(
                                    candidate.evSignificance)) {
                        switch (candidate.eventType.toUpperCase()) {
                        case "CAN":
                            candidate.evEndTime = candidate.origEndTime;
                            candidate.eventTime = new DataTime(
                                    candidate.eventTime.getRefTimeAsCalendar(),
                                    new TimeRange(
                                            candidate.eventTime
                                                    .getRefTimeAsCalendar(),
                                            candidate.evEndTime
                                                    .getRefTimeAsCalendar()));// ?
                            break;
                        case "COR":
                            candidate.evEndTime = modify.evEndTime;
                            break;
                        case "EXP":
                        case "EXA":
                        case "EXB":
                        case "CON":
                        case "EXT":
                            candidate.evEndTime = candidate.origEndTime;
                            break;
                        case "NEW":
                            candidate.eventTime = modify.eventTime;
                            candidate.evEndTime = modify.evEndTime;
                            break;
                        default:
                            candidate.evEndTime = modify.evEndTime;
                        }
                        candidate.eventTime = new DataTime(
                                candidate.eventTime.getRefTimeAsCalendar(),
                                new TimeRange(
                                        candidate.eventTime
                                                .getRefTimeAsCalendar(),
                                        candidate.evEndTime
                                                .getRefTimeAsCalendar()));
                    }

                }
            }
        }
    }

    @Override
    public void initResource(IGraphicsTarget grphTarget) throws VizException {
        font = grphTarget.initializeFont("Monospace", 12,
                new IFont.Style[] { IFont.Style.BOLD });
        stationTable = new StationTable(NcPathManager.getInstance()
                .getStaticFile(NcPathConstants.COUNTY_STN_TBL)
                .getAbsolutePath());
        queryRecords();
        populateQueryResultMap();
    }

    private void populateQueryResultMap() {

        Iterator<IRscDataObject> iter = newRscDataObjsQueue.iterator();
        while (iter.hasNext()) {
            IRscDataObject dataObject = iter.next();
            queryResult.buildQueryPart(dataObject);
        }
        queryResult.populateMap(QUERY_PREFIX_COUNTY);
        queryResult.populateMap(QUERY_PREFIX_MARINE_ZONE);
        setAllFramesAsPopulated();
    }

    @Override
    public void disposeInternal() {
    }

    @Override
    public void paintFrame(AbstractFrameData frameData, IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {

        if (paintProps == null) {
            return;
        }

        if (areaChangeFlag) {
            areaChangeFlag = false;
            postProcessFrameUpdate();
        }

        FrameData currFrameData = (FrameData) frameData;
        RGB symbolColor = new RGB(155, 155, 155);
        LineStyle lineStyle = LineStyle.SOLID;
        int symbolWidth = 2;
        int symbolSize = 2;

        // Need to sort the WCNs by issue time prior to passing in to
        // be chopped into individual counties.
        List<WcnRscDataObj> sortedWcns = new ArrayList<>();

        for (Map.Entry<String, WcnRscDataObj> entry : currFrameData.wcnDataMap
                .entrySet()) {
            sortedWcns.add(entry.getValue());
        }
        Collections.sort(sortedWcns);

        List<PreProcessDisplayObj> displayObjs = PreProcessDisplay(sortedWcns);

        for (PreProcessDisplayObj wcnData : displayObjs) {
            Boolean draw = false, drawLabel = true;

            if (wcnRscData.getColorCodeEnable()) {
                int watchNumberchoice = Integer.parseInt(wcnData.watchNumber)
                        % 10;

                switch (watchNumberchoice) {

                case 0:
                    if (wcnRscData.Watchxxx0Enable) {
                        color = wcnRscData.Watchxxx0Color;
                        symbolWidth = wcnRscData.Watchxxx0SymbolWidth;
                        symbolSize = wcnRscData.Watchxxx0SymbolSize;
                        symbolColor = wcnRscData.Watchxxx0SymbolColor;
                    }
                    break;
                case 1:
                    if (wcnRscData.Watchxxx1Enable) {
                        color = wcnRscData.Watchxxx1Color;
                        symbolWidth = wcnRscData.Watchxxx1SymbolWidth;
                        symbolSize = wcnRscData.Watchxxx1SymbolSize;
                        symbolColor = wcnRscData.Watchxxx1SymbolColor;
                    }
                    break;
                case 2:
                    if (wcnRscData.Watchxxx2Enable) {
                        color = wcnRscData.Watchxxx2Color;
                        symbolWidth = wcnRscData.Watchxxx2SymbolWidth;
                        symbolSize = wcnRscData.Watchxxx2SymbolSize;
                        symbolColor = wcnRscData.Watchxxx2SymbolColor;
                    }
                    break;
                case 3:
                    if (wcnRscData.Watchxxx3Enable) {
                        color = wcnRscData.Watchxxx3Color;
                        symbolWidth = wcnRscData.Watchxxx3SymbolWidth;
                        symbolSize = wcnRscData.Watchxxx3SymbolSize;
                        symbolColor = wcnRscData.Watchxxx3SymbolColor;
                    }
                    break;
                case 4:
                    if (wcnRscData.Watchxxx4Enable) {
                        color = wcnRscData.Watchxxx4Color;
                        symbolWidth = wcnRscData.Watchxxx4SymbolWidth;
                        symbolSize = wcnRscData.Watchxxx4SymbolSize;
                        symbolColor = wcnRscData.Watchxxx4SymbolColor;
                    }
                    break;
                case 5:
                    if (wcnRscData.Watchxxx5Enable) {
                        color = wcnRscData.Watchxxx5Color;
                        symbolWidth = wcnRscData.Watchxxx5SymbolWidth;
                        symbolSize = wcnRscData.Watchxxx5SymbolSize;
                        symbolColor = wcnRscData.Watchxxx5SymbolColor;
                    }
                    break;
                case 6:
                    if (wcnRscData.Watchxxx6Enable) {
                        color = wcnRscData.Watchxxx6Color;
                        symbolWidth = wcnRscData.Watchxxx6SymbolWidth;
                        symbolSize = wcnRscData.Watchxxx6SymbolSize;
                        symbolColor = wcnRscData.Watchxxx6SymbolColor;
                    }
                    break;
                case 7:
                    if (wcnRscData.Watchxxx7Enable) {
                        color = wcnRscData.Watchxxx7Color;
                        symbolWidth = wcnRscData.Watchxxx7SymbolWidth;
                        symbolSize = wcnRscData.Watchxxx7SymbolSize;
                        symbolColor = wcnRscData.Watchxxx7SymbolColor;
                    }
                    break;
                case 8:
                    if (wcnRscData.Watchxxx8Enable) {
                        color = wcnRscData.Watchxxx8Color;
                        symbolWidth = wcnRscData.Watchxxx8SymbolWidth;
                        symbolSize = wcnRscData.Watchxxx8SymbolSize;
                        symbolColor = wcnRscData.Watchxxx8SymbolColor;
                    }
                    break;
                case 9:
                    if (wcnRscData.Watchxxx9Enable) {
                        color = wcnRscData.Watchxxx9Color;
                        symbolWidth = wcnRscData.Watchxxx9SymbolWidth;
                        symbolSize = wcnRscData.Watchxxx9SymbolSize;
                        symbolColor = wcnRscData.Watchxxx9SymbolColor;
                    }
                    break;
                }
            } else {
                if (wcnData.reportType == AwwReportType.SEVERE_THUNDERSTORM_WATCH) {
                    color = wcnRscData.thunderstormColor;
                    symbolWidth = wcnRscData.thunderstormSymbolWidth;
                    symbolSize = wcnRscData.thunderstormSymbolSize;
                    symbolColor = wcnRscData.thunderstormSymbolColor;
                } else if (wcnData.reportType == AwwReportType.TORNADO_WATCH) {
                    color = wcnRscData.tornadoColor;
                    symbolWidth = wcnRscData.tornadoSymbolWidth;
                    symbolSize = wcnRscData.tornadoSymbolSize;
                    symbolColor = wcnRscData.tornadoSymbolColor;
                }
            }

            if ((wcnData.reportType == AwwReportType.SEVERE_THUNDERSTORM_WATCH
                    && wcnRscData.thunderstormEnable)
                    || (wcnData.reportType == AwwReportType.TORNADO_WATCH
                            && wcnRscData.tornadoEnable)) {

                draw = true;
            }
            if (getCurrentFrameTime().getValidTimeAsDate()
                    .getTime() < wcnData.displayEnd.getValidPeriod().getEnd()
                            .getTime()
                    || getCurrentFrameTime().getValidTimeAsDate()
                            .getTime() >= wcnData.displayStart.getTime()) {
                draw = true;
            }

            if (getCurrentFrameTime().getValidTimeAsDate()
                    .getTime() > wcnData.displayEnd.getValidPeriod().getEnd()
                            .getTime()) {
                draw = false;
            }

            if (getCurrentFrameTime().getValidTimeAsDate()
                    .getTime() < wcnData.displayStart.getTime()) {
                draw = false;
            }

            if (getCurrentFrameTime().getValidTimeAsDate()
                    .getTime() == (wcnData.displayEnd.getValidPeriod().getEnd()
                            .getTime())) {
                // do not draw endtime frame, that's what nmap2 does
                draw = false;
            }
            // draw the polygon
            if (draw) {

                if (wcnRscData.getWatchBoxMarkerEnable()) {
                    try {
                        Color[] colors = new Color[] {
                                new Color(color.red, color.green, color.blue) };
                        Coordinate coord = new Coordinate(wcnData.countyLon,
                                wcnData.countyLat);
                        Symbol pointSymbol = new Symbol(null, colors,
                                symbolWidth, symbolSize * 0.4, false, coord,
                                "Symbol", "FILLED_DIAMOND");
                        DisplayElementFactory df = new DisplayElementFactory(
                                target, getNcMapDescriptor());
                        List<IDisplayable> displayEls = df
                                .createDisplayElements(pointSymbol, paintProps);
                        for (IDisplayable each : displayEls) {
                            each.draw(target, paintProps);
                            each.dispose();
                        }
                    } catch (Exception e) {
                        statusHandler.warn(e.getLocalizedMessage(), e);
                    }
                }
                if (wcnRscData.getWatchBoxFillEnable()) {
                    if (!color.equals(wcnData.color)) {
                        postProcessFrameUpdate();
                        wcnData.color = color;
                    }
                    drawCountyOutline2(wcnData, target, symbolColor,
                            symbolWidth, lineStyle, paintProps, 1);

                    // only if watchbox union enable is false
                    if (!wcnRscData.getWatchBoxUnionEnable()) {
                        drawTimeLabelWatchNumber(wcnData, target, color);
                    }
                }

                if (wcnRscData.getWatchBoxUnionEnable()) {

                    drawLabel = false;
                    double allX = wcnData.countyLon;
                    double allY = wcnData.countyLat;
                    double[] labelLatLon = { allX, allY };
                    double[] labelPix = descriptor.worldToPixel(labelLatLon);
                    if (labelPix != null) {
                        String[] text = new String[2];
                        List<String> enabledText = new ArrayList<>();

                        if (wcnRscData.getWatchBoxNumberEnable()) {
                            enabledText.add(wcnData.watchNumber);
                        }

                        if (wcnRscData.getWatchBoxTimeEnable()) {
                            DataTime startTime = new DataTime(wcnData.eventTime
                                    .getValidPeriod().getStart());
                            DataTime endTime = new DataTime(wcnData.origEndTime
                                    .getValidPeriod().getEnd());
                            String temp = startTime.toString().substring(11, 13)
                                    + startTime.toString().substring(14, 16)
                                    + "-" + endTime.toString().substring(11, 13)
                                    + endTime.toString().substring(14, 16);
                            enabledText.add(temp);
                        }

                        for (int i = enabledText.size(); i < 2; i++) {
                            enabledText.add("");
                        }

                        text = enabledText.toArray(text);
                        DrawableString wcnTime = new DrawableString(text,
                                color);
                        wcnTime.font = font;
                        wcnTime.setCoordinates(labelPix[0], labelPix[1], 0.0);
                        wcnTime.textStyle = TextStyle.NORMAL;
                        wcnTime.horizontalAlignment = HorizontalAlignment.LEFT;
                        wcnTime.verticallAlignment = VerticalAlignment.BOTTOM;
                        target.drawStrings(wcnTime);
                    }

                    drawCountyOutline2(wcnData, target, color, symbolWidth,
                            lineStyle, paintProps, 2);
                }

                else if (wcnRscData.getWatchBoxOutlineEnable()) {
                    drawCountyOutline2(wcnData, target, color, symbolWidth,
                            lineStyle, paintProps, 0);
                }

                if (((wcnRscData.getWatchBoxTimeEnable()
                        || wcnRscData.getWatchBoxLabelEnable()
                        || wcnRscData.getWatchBoxNumberEnable()) && drawLabel)
                        && !wcnRscData.watchBoxFillEnable) {
                    drawTimeLabelWatchNumber(wcnData, target, color);
                }
                // if draw = true
            }

            // end-foreach in wcndata..
        }
    }

    public void drawTimeLabelWatchNumber(PreProcessDisplayObj wcnData,
            IGraphicsTarget target, RGB color) {
        try {
            double[] labelLatLon = { wcnData.countyLon, wcnData.countyLat };
            double[] labelPix = descriptor.worldToPixel(labelLatLon);

            if (labelPix != null) {
                String[] text = new String[3];
                List<String> enabledText = new ArrayList<>();

                if (wcnRscData.getWatchBoxNumberEnable()) {
                    enabledText.add(wcnData.watchNumber);
                }

                if (wcnRscData.getWatchBoxLabelEnable()) {
                    enabledText.add(wcnData.countyNames);
                }

                if (wcnRscData.getWatchBoxTimeEnable()) {
                    DataTime startTime = new DataTime(
                            wcnData.eventTime.getValidPeriod().getStart());
                    DataTime endTime = new DataTime(
                            wcnData.origEndTime.getValidPeriod().getEnd());
                    String temp = startTime.toString().substring(11, 13)
                            + startTime.toString().substring(14, 16) + "-"
                            + endTime.toString().substring(11, 13)
                            + endTime.toString().substring(14, 16);
                    enabledText.add(temp);
                }

                for (int j = enabledText.size(); j < 3; j++) {
                    enabledText.add("");
                }
                text = enabledText.toArray(text);
                DrawableString wcnTime = new DrawableString(text, color);
                wcnTime.font = font;
                wcnTime.setCoordinates(labelPix[0], labelPix[1], 0.0);
                wcnTime.textStyle = TextStyle.NORMAL;
                wcnTime.horizontalAlignment = HorizontalAlignment.LEFT;
                wcnTime.verticallAlignment = VerticalAlignment.BOTTOM;
                target.drawStrings(wcnTime);
            }
        } catch (Exception e) {
            statusHandler.error(
                    "Error drawing time label watch number" + e.getMessage(),
                    e);
        }
    }

    // County by county display. Creates a subset of the master
    // WcnRscDataObj and boils it down to only what is needed for display.
    public class PreProcessDisplayObj
            implements Comparable<PreProcessDisplayObj> {

        /** issue time from bulletin */
        private DataTime issueTime;

        private DataTime eventTime;

        private AwwReportType reportType;

        private float countyLat;

        private float countyLon;

        private String countyNames;

        private String stateNames;

        public String countyFips;

        private String eventType;

        /** watch number to be displayed */
        private String watchNumber;

        private DataTime origEndTime;

        private Date displayStart;

        private DataTime displayEnd;

        private String evTrack;

        private String evPhenomena;

        private String evSignificance;

        private String evOfficeId;

        private RGB color = new RGB(155, 155, 155);

        public String getKey() {
            return getpreProcessDisplayObjKey(this);
        }

        // Used to sort finished collection of single county objects.
        @Override
        public int compareTo(PreProcessDisplayObj o) {

            return (int) (this.issueTime.getRefTime().getTime()
                    - o.issueTime.getRefTime().getTime());
        }
    }

    /*
     * RM 4637 time labels. The original implementation of this resource was to
     * take each WCN message and use that as a basis for display. This had two
     * flaws. One it caused multiple labels to be created for the same county.
     * Two, if an EXT was issued it would cause a collision of labels. To fix
     * this we pre-process all valid messages for the current frame data and
     * make sure the county only has one label displayed at all times. For every
     * message except EXT and EXB the first instance of the county in any WCN
     * will set the time label. Since when the message is decoded if the VTEC
     * start time is 000000T0000Z it will point back to NEW product, it does not
     * matter which eventType we used to create the initial start time label.
     * For EXT the display needs to be updated to reflect that. In those cases
     * it resets the displayEnd variable time of the previous label so that
     * there is only one label displayed at all times and the end time is
     * correct based on the frame time. Once all the county labels are
     * determined, the array is sorted and returned to have the labels drawn.
     */
    private List<PreProcessDisplayObj> PreProcessDisplay(
            List<WcnRscDataObj> wcnCollection) {
        HashMap<String, PreProcessDisplayObj> displayMap;
        HashMap<String, DataTime> extMap;
        HashMap<String, String> splitWatch;
        displayMap = new HashMap<>();
        extMap = new HashMap<>();
        splitWatch = new HashMap<>();
        List<PreProcessDisplayObj> displayObj = new ArrayList<>();
        for (WcnRscDataObj processWCN : wcnCollection) {
            for (int i = 0; i < processWCN.countyNumPoints; i++) {
                PreProcessDisplayObj singleCounty = new PreProcessDisplayObj();
                singleCounty.countyFips = processWCN.countyFips.get(i);
                singleCounty.countyLat = processWCN.countyLat[i];
                singleCounty.countyLon = processWCN.countyLon[i];
                singleCounty.countyNames = processWCN.countyNames.get(i);
                if (!Set.of("NEW", "CON")
                        .contains(processWCN.eventType.toUpperCase())) {
                    singleCounty.displayStart = processWCN.issueTime
                            .getValidPeriod().getStart();
                } else {
                    singleCounty.displayStart = processWCN.eventTime
                            .getValidPeriod().getStart();
                }
                singleCounty.displayEnd = processWCN.origEndTime;
                singleCounty.eventTime = processWCN.eventTime;
                singleCounty.eventType = processWCN.eventType;
                singleCounty.issueTime = processWCN.issueTime;
                singleCounty.origEndTime = processWCN.origEndTime;
                singleCounty.reportType = processWCN.reportType;
                singleCounty.stateNames = processWCN.stateNames.get(i);
                singleCounty.watchNumber = processWCN.watchNumber;
                // Some watches begin when a previous is ending. The watch
                // number in the WCN then is 001/002.
                // Need to track this to avoid multiple labels being displayed.
                if (singleCounty.watchNumber.contains("/")) {
                    splitWatch.put(singleCounty.countyFips,
                            singleCounty.watchNumber);
                }
                singleCounty.evTrack = processWCN.evTrack;
                singleCounty.evPhenomena = processWCN.evPhenomena;
                singleCounty.evSignificance = processWCN.evSignificance;
                singleCounty.evOfficeId = processWCN.evOfficeId;
                if (Set.of("EXT", "EXB", "EXA")
                        .contains(singleCounty.eventType.toUpperCase())) {
                    if (extMap.containsKey(singleCounty.countyFips + " "
                            + singleCounty.evTrack)) {
                        DataTime retrievePreviousEXTTime = extMap
                                .get(singleCounty.countyFips + " "
                                        + singleCounty.evTrack);
                        PreProcessDisplayObj retrievePreviousEXT = displayMap
                                .get(singleCounty.countyFips + " "
                                        + singleCounty.evTrack + " " + "EXT"
                                        + retrievePreviousEXTTime);
                        if (retrievePreviousEXT == null) {
                            retrievePreviousEXT = displayMap
                                    .get(singleCounty.countyFips + " "
                                            + singleCounty.evTrack + " "
                                            + "EXT");
                            retrievePreviousEXT.displayEnd = singleCounty.issueTime;
                            displayMap.put(singleCounty.countyFips + " "
                                    + singleCounty.evTrack + " " + "EXT",
                                    retrievePreviousEXT);
                            extMap.put(
                                    singleCounty.countyFips + " "
                                            + singleCounty.evTrack,
                                    singleCounty.issueTime);
                        } else {
                            retrievePreviousEXT.displayEnd = singleCounty.issueTime;
                            displayMap.put(
                                    singleCounty.countyFips + " "
                                            + singleCounty.evTrack + " " + "EXT"
                                            + " " + retrievePreviousEXTTime,
                                    retrievePreviousEXT);
                            displayMap.put(
                                    singleCounty.countyFips + " "
                                            + singleCounty.evTrack + " " + "EXT"
                                            + " " + singleCounty.issueTime,
                                    singleCounty);
                            extMap.put(
                                    singleCounty.countyFips + " "
                                            + singleCounty.evTrack,
                                    singleCounty.issueTime);
                        }
                    } else if (displayMap.containsKey(singleCounty.countyFips
                            + " " + singleCounty.evTrack)) {
                        PreProcessDisplayObj retrieveCurrent = displayMap
                                .get(singleCounty.countyFips + " "
                                        + singleCounty.evTrack);
                        retrieveCurrent.displayEnd = singleCounty.issueTime;
                        displayMap.put(
                                singleCounty.countyFips + " "
                                        + singleCounty.evTrack,
                                retrieveCurrent);
                        displayMap.put(
                                singleCounty.countyFips + " "
                                        + singleCounty.evTrack + " " + "EXT",
                                singleCounty);
                        extMap.put(
                                singleCounty.countyFips + " "
                                        + singleCounty.evTrack,
                                singleCounty.issueTime);
                    } else {
                        displayMap.put(singleCounty.countyFips + " "
                                + singleCounty.evTrack, singleCounty);
                    }
                } else if (Set.of("CAN", "EXP")
                        .contains(singleCounty.eventType.toUpperCase())) {
                    if (extMap.containsKey(singleCounty.countyFips + " "
                            + singleCounty.evTrack)) {
                        DataTime retrievePreviousEXTTime = extMap
                                .get(singleCounty.countyFips + " "
                                        + singleCounty.evTrack);
                        PreProcessDisplayObj retrievePreviousEXT = displayMap
                                .get(singleCounty.countyFips + " "
                                        + singleCounty.evTrack + " " + "EXT"
                                        + retrievePreviousEXTTime);
                        if (retrievePreviousEXT == null) {
                            retrievePreviousEXT = displayMap
                                    .get(singleCounty.countyFips + " "
                                            + singleCounty.evTrack + " "
                                            + "EXT");
                            if ("CAN"
                                    .equalsIgnoreCase(singleCounty.eventType)) {
                                retrievePreviousEXT.displayEnd = singleCounty.issueTime;
                            } else {
                                retrievePreviousEXT.displayEnd = singleCounty.origEndTime;
                            }
                            displayMap.put(singleCounty.countyFips + " "
                                    + singleCounty.evTrack + " " + "EXT",
                                    retrievePreviousEXT);
                            extMap.put(
                                    singleCounty.countyFips + " "
                                            + singleCounty.evTrack,
                                    singleCounty.origEndTime);
                        } else {
                            retrievePreviousEXT.displayEnd = singleCounty.origEndTime;
                            displayMap.put(
                                    singleCounty.countyFips + " "
                                            + singleCounty.evTrack + " " + "EXT"
                                            + " " + retrievePreviousEXTTime,
                                    retrievePreviousEXT);
                            extMap.put(
                                    singleCounty.countyFips + " "
                                            + singleCounty.evTrack,
                                    singleCounty.origEndTime);
                        }
                    } else if (displayMap.containsKey(singleCounty.countyFips
                            + " " + singleCounty.evTrack)) {
                        PreProcessDisplayObj retrieveCurrent;
                        retrieveCurrent = displayMap.get(singleCounty.countyFips
                                + " " + singleCounty.evTrack);
                        if ("CAN".equalsIgnoreCase(singleCounty.eventType)) {
                            retrieveCurrent.displayEnd = singleCounty.issueTime;
                        } else {
                            retrieveCurrent.displayEnd = singleCounty.origEndTime;
                        }
                        displayMap.put(
                                singleCounty.countyFips + " "
                                        + singleCounty.evTrack,
                                retrieveCurrent);
                    }
                } else {
                    if (!displayMap.containsKey(singleCounty.countyFips + " "
                            + singleCounty.evTrack)) {
                        displayMap.put(singleCounty.countyFips + " "
                                + singleCounty.evTrack, singleCounty);
                    }
                }
            }
        }

        for (Map.Entry<String, PreProcessDisplayObj> entry : displayMap
                .entrySet()) {
            displayObj.add(entry.getValue());
        }
        Collections.sort(displayObj);

        return (displayObj);
    }

    class CountyResultJob extends org.eclipse.core.runtime.jobs.Job {

        private java.util.Map<String, Result> keyResultMap = new java.util.concurrent.ConcurrentHashMap<>();

        private java.util.Map<String, Result> keyShadeMap = new java.util.concurrent.ConcurrentHashMap<>();

        private java.util.Map<String, Result> keyUnionMap = new java.util.concurrent.ConcurrentHashMap<>();

        private IGraphicsTarget target;

        private IMapDescriptor descriptor;

        public class Result {

            public IWireframeShape outlineShape;

            public IWireframeShape newUnionShape;

            public IShadedShape shadedShape;

            public java.util.Map<Object, RGB> colorMap;

            private Result(IWireframeShape outlineShape,
                    IWireframeShape nuShape, IShadedShape shadedShape,
                    java.util.Map<Object, RGB> colorMap) {

                this.outlineShape = outlineShape;
                this.shadedShape = shadedShape;
                this.newUnionShape = nuShape;
                this.colorMap = colorMap;
            }
        }

        public CountyResultJob(String name) {
            super(name);
        }

        public void setRequest(IGraphicsTarget target,
                IMapDescriptor descriptor, String query, boolean labeled,
                boolean shaded, java.util.Map<Object, RGB> colorMap) {

            this.target = target;
            this.descriptor = descriptor;
            this.run(null);// this.schedule();

        }

        @Override
        protected org.eclipse.core.runtime.IStatus run(
                org.eclipse.core.runtime.IProgressMonitor monitor) {

            for (AbstractFrameData afd : frameDataMap.values()) {

                FrameData fd = (FrameData) afd;

                for (WcnRscDataObj wrdo : fd.wcnDataMap.values()) {

                    Collection<Geometry> gw = new ArrayList<>(),
                            gu = new ArrayList<>();

                    for (String element : wrdo.countyFips) {
                        // another loop handles multiple rows in maps
                        // mapdata.county table
                        for (ArrayList<Object[]> results : queryResult
                                .getStateCountyResult(element)) {

                            if (results == null) {
                                continue;
                            }

                            WKBReader wkbReader = new WKBReader();
                            for (Object[] result : results) {
                                int k = 0;
                                byte[] wkb1 = (byte[]) result[k];

                                MultiPolygon countyGeo = null;
                                try {
                                    countyGeo = (MultiPolygon) wkbReader
                                            .read(wkb1);

                                    if (countyGeo != null
                                            && !countyGeo.isEmpty()) {
                                        gu.add((MultiPolygon) countyGeo
                                                .clone());
                                        gw.add((MultiPolygon) countyGeo
                                                .clone());
                                    }
                                } catch (Exception e) {
                                    statusHandler.warn(e.getMessage(), e);
                                }
                            }
                        }
                    }
                    if (!gw.isEmpty()) {
                        setEachWrdoShape(wrdo, gw, gu);
                    }
                }

            }

            return org.eclipse.core.runtime.Status.OK_STATUS;
        }

        public void setEachWrdoShape(WcnRscDataObj wrdo,
                Collection<Geometry> gw, Collection<Geometry> gu) {

            IWireframeShape newOutlineShape = target.createWireframeShape(false,
                    descriptor, 0.0f);
            IWireframeShape newUnionShape = target.createWireframeShape(false,
                    descriptor, 0.0f);
            IShadedShape newShadedShape = target.createShadedShape(false,
                    descriptor.getGridGeometry(), true);

            JTSCompiler jtsCompiler = new JTSCompiler(newShadedShape,
                    newOutlineShape, descriptor, PointStyle.CROSS);
            JTSCompiler jcu = new JTSCompiler(null, newUnionShape, descriptor,
                    PointStyle.CROSS);

            GeometryCollection gColl = (GeometryCollection) new GeometryFactory()
                    .buildGeometry(gw);

            GeometryCollection gCollu = (GeometryCollection) new GeometryFactory()
                    .buildGeometry(gu);

            try {
                gColl.normalize();
                gCollu.normalize();

                jtsCompiler.handle(gColl, color);
                jcu.handle(gCollu.union(), color);

                newOutlineShape.compile();
                newUnionShape.compile();
                newShadedShape.compile();

            } catch (Exception e) {
                statusHandler.warn(e.getMessage(), e);
            }

            String key = wrdo.getKey();
            keyResultMap.put(key,
                    new Result(newOutlineShape, null, null, null));
            keyShadeMap.put(key, new Result(null, null, newShadedShape, null));
            keyUnionMap.put(key, new Result(null, newUnionShape, null, null));
        }

    }

    private void drawCountyOutline2(PreProcessDisplayObj wData,
            IGraphicsTarget target, RGB color, int outlineWidth,
            LineStyle lineStyle, PaintProperties paintProps,
            int drawOutlineOrShadedshapeorUnion) {

        String key = wData.getKey();
        CountyResultJob.Result result = crJob.keyResultMap.get(key);
        CountyResultJob.Result resultShaded = crJob.keyShadeMap.get(key);
        CountyResultJob.Result resultU = crJob.keyUnionMap.get(key);

        if (result != null) {
            if (outlineShape != null) {
                outlineShape.dispose();
            }
            outlineShape = result.outlineShape;
        } else {
            return;
        }

        // for storing result of pre-calculation of shade
        IShadedShape shadedShape = null;
        if (resultShaded != null) {
            shadedShape = resultShaded.shadedShape;
        } else {
            return;
        }

        // for storing result of pre-calculation of union
        IWireframeShape newUnionShape = null;
        if (resultU != null) {
            newUnionShape = resultU.newUnionShape;
        } else {
            return;
        }

        if (shadedShape != null && shadedShape.isDrawable()
                && drawOutlineOrShadedshapeorUnion == 1) {
            try {
                target.drawShadedShape(shadedShape, 0.5f);
            } catch (VizException e) {
                statusHandler.warn(e.getMessage(), e);
            }
        }

        if (outlineShape != null && outlineShape.isDrawable()
                && drawOutlineOrShadedshapeorUnion == 0) {
            try {
                target.drawWireframeShape(outlineShape, color, outlineWidth,
                        lineStyle);
            } catch (VizException e) {
                statusHandler.warn(e.getMessage(), e);
            }

        }

        if (newUnionShape != null && newUnionShape.isDrawable()
                && drawOutlineOrShadedshapeorUnion == 2) {
            try {
                target.drawWireframeShape(newUnionShape, color, outlineWidth,
                        lineStyle);
            } catch (VizException e) {
                statusHandler.warn(e.getMessage(), e);
            }
        }
    }

    @Override
    public void project(CoordinateReferenceSystem crs) throws VizException {
        areaChangeFlag = true;
    }

    /**
     * avoid null pointers exception in super class
     */
    @Override
    protected long getDataTimeMs(IRscDataObject rscDataObj) {
        if (rscDataObj == null) {
            return 0;
        }

        java.util.Calendar validTimeInCalendar = null;
        DataTime dataTime = rscDataObj.getDataTime();
        if (dataTime != null) {
            validTimeInCalendar = dataTime.getValidTime();

        } else {
            statusHandler.info(
                    "===== find IRscDataObject rscDataObj.getDataTime() return NULL!!!");
        }
        long dataTimeInMs = 0;
        if (validTimeInCalendar != null) {
            dataTimeInMs = validTimeInCalendar.getTimeInMillis();
        }
        return dataTimeInMs;
    }

    public String getWcnRscDataObjKey(WcnRscDataObj w) {

        StringBuilder sb = new StringBuilder(w.evOfficeId);

        sb.append(w.evTrack).append(w.evPhenomena).append(w.evSignificance)
                .append(w.eventType);

        for (String s : w.countyFips) {
            sb.append(s);
        }
        sb.append(w.countyFips);

        return sb.toString();
    }

    public String getpreProcessDisplayObjKey(PreProcessDisplayObj w) {

        StringBuilder sb = new StringBuilder(w.evOfficeId);

        sb.append(w.evTrack).append(w.evPhenomena).append(w.evSignificance)
                .append(w.eventType);

        sb.append(w.countyFips);

        return sb.toString();
    }

    @Override
    protected boolean postProcessFrameUpdate() {

        AbstractEditor ncme = NcDisplayMngr.getActiveNatlCntrsEditor();

        crJob.setRequest(ncme.getActiveDisplayPane().getTarget(),
                getNcMapDescriptor(), null, false, false, null);

        return true;
    }

    @Override
    public void queryRecords() throws VizException {
        // Changed query to use ThirftClient instead of uengine. Also,
        // limit response to the last twelve hours from frame
        // time requested.
        DbQueryRequest request = new DbQueryRequest();
        DbQueryRequest requestWatchNumber = new DbQueryRequest();
        Map<String, RequestConstraint> reqConstraints = new HashMap<>();
        RequestConstraint pluginName = new RequestConstraint(
                wcnRscData.getPluginName());
        IDescriptor.FramesInfo frameTimes = this.descriptor.getFramesInfo();
        long watchcheck = frameTimes.getFrameTimes()[0].getRefTimeAsCalendar()
                .getTimeInMillis();
        watchcheck = watchcheck - TWELVE_HOURS_IN_MILLISECS;
        Date queryTime = new Date(watchcheck);
        DataTime setQueryTime = new DataTime(queryTime);
        reqConstraints.put("pluginName", pluginName);
        reqConstraints.put("reportType",
                new RequestConstraint("WATCH COUNTY NOTIFICATION"));
        reqConstraints.put("dataTime.refTime", new RequestConstraint(
                setQueryTime.toString(), ConstraintType.GREATER_THAN));
        requestWatchNumber.setConstraints(reqConstraints);
        requestWatchNumber.addRequestField(WATCH_NUMBER_CONSTRAINT);
        requestWatchNumber.setDistinct(true);
        DbQueryResponse responseWatch = (DbQueryResponse) ThriftClient
                .sendRequest(requestWatchNumber);

        reqConstraints.remove("dataTime.refTime");
        List<Object> pdoList = new ArrayList<>();
        for (Map<String, Object> element : responseWatch.getResults()) {
            for (Map.Entry<String, Object> watchentry : element.entrySet()) {
                reqConstraints.put(WATCH_NUMBER_CONSTRAINT,
                        new RequestConstraint(
                                watchentry.getValue().toString()));
                request.setConstraints(reqConstraints);
                DbQueryResponse response = (DbQueryResponse) ThriftClient
                        .sendRequest(request);
                reqConstraints.remove(WATCH_NUMBER_CONSTRAINT);
                for (Map<String, Object> element2 : response.getResults()) {
                    for (Map.Entry<String, Object> pdoentry : element2
                            .entrySet()) {
                        pdoList.add(pdoentry.getValue());
                    }
                }
            }
        }

        queryResult = new WcnCountyQueryResult();

        for (Object pdo : pdoList) {
            for (IRscDataObject dataObject : processRecord(pdo)) {
                newRscDataObjsQueue.add(dataObject);
            }
        }
    }

    public boolean isCountyUgs(AwwUgc au) {
        Set<AwwFips> awwFipsSet = au.getAwwFIPS();
        boolean out = false;

        if (awwFipsSet == null) {
            return false;
        } else {

            for (AwwFips eachAwwFips : awwFipsSet) {

                String eachFips = eachAwwFips.getFips();

                if (eachFips == null || eachFips.isEmpty()
                        || eachFips.length() != 6) {
                    return false;
                }

                out = ('C' == eachFips.charAt(2));
            }
        }

        return out;
    }

    @Override
    public String getName() {
        String legendString = super.getName();
        FrameData fd = (FrameData) getCurrentFrame();
        if (fd == null || fd.getFrameTime() == null
                || fd.wcnDataMap.isEmpty()) {
            return legendString + "-No Data";
        }
        return legendString + " "
                + NmapCommon.getTimeStringFromDataTime(fd.getFrameTime(), "/");
    }
}
