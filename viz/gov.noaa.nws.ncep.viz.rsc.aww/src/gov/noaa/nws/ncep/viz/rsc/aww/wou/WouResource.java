package gov.noaa.nws.ncep.viz.rsc.aww.wou;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.graphics.RGB;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.raytheon.uf.common.geospatial.MapUtil;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.time.TimeRange;
import com.raytheon.uf.edex.decodertools.core.LatLonPoint;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.HorizontalAlignment;
import com.raytheon.uf.viz.core.IGraphicsTarget.LineStyle;
import com.raytheon.uf.viz.core.IGraphicsTarget.TextStyle;
import com.raytheon.uf.viz.core.IGraphicsTarget.VerticalAlignment;
import com.raytheon.uf.viz.core.PixelExtent;
import com.raytheon.uf.viz.core.catalog.DirectDbQuery;
import com.raytheon.uf.viz.core.catalog.DirectDbQuery.QueryLanguage;
import com.raytheon.uf.viz.core.drawables.IFont;
import com.raytheon.uf.viz.core.drawables.IShadedShape;
import com.raytheon.uf.viz.core.drawables.IWireframeShape;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.viz.core.rsc.jts.JTSCompiler;
import com.raytheon.viz.core.rsc.jts.JTSCompiler.PointStyle;

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
import gov.noaa.nws.ncep.viz.rsc.aww.query.AwwQueryResult;
import gov.noaa.nws.ncep.viz.ui.display.NCMapDescriptor;

/**
 * Wou resourceResource - Display WOU from aww data.
 *
 * This code has been developed by the SIB for use in the AWIPS2 system.
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer       Description
 * ------------- -------- -------------- ---------------------------------------
 * May 04, 2010           Uma Josyula    Initial creation.
 * Jan 10, 2011           Uma Josyula    Made changes to preprocess update and
 *                                       event date
 * Jul 28, 2011  450      Greg Hull      NcPathManager
 * Sep 28, 2011           Xilin Guo      Made changes to create IWireframeShape
 *                                       for watch number
 * Dec 27, 2011           Xilin Guo      Checked available watch data
 * May 23, 2012  785      Q. Zhou        Added getName for legend.
 * Aug 17, 2012  655      B. Hebbard     Added paintProps as parameter to
 *                                       IDisplayable draw
 * Sep 05, 2012  857      Q. Zhou        Displayed watch number. Modified time
 *                                       string and alignment in
 *                                       drawTimeLabelWatchNumber(). Added
 *                                       label/time for union.  Fixed a bug for
 *                                       querying county. Modified fill alpha to
 *                                       0.5.
 * Sep 13, 2012  857      Q. Zhou        Remove constraint & metamap in
 *                                       initResource().
 * Aug 14, 2013  1028     G. Hull        Move to aww project. Use AwwReportType
 *                                       enum.
 * Sep 15, 2014  4637     J. Huber       Added fipsRangeReparse to handle
 *                                       "character" in UGC line. Also added
 *                                       logic on when to use it. Also fixed a
 *                                       clean up error for the county list
 *                                       array which prevented some counties
 *                                       from being displayed.
 * Nov 05, 2015  5070     randerso       Adjust font sizes for dpi scaling
 * Mar 15, 2016  15560    K. Bugenhagen  Cleanup and local refactoring.
 * Jul 15, 2020  8191     randerso       Updated for changes to LatLonPoint
 *
 * </pre>
 *
 * @author ujosyula
 */

public class WouResource
        extends AbstractNatlCntrsResource<WouResourceData, NCMapDescriptor>
        implements INatlCntrsResource, IStationField {

    private IFont font;

    private StationTable stationTable;

    private WouResourceData wouRscData;

    private List<WouRscDataObj> modifyList;

    // Area change flag
    private boolean areaChangeFlag = false;

    private static final String QUERY_PREFIX = "select countyname, state, ST_AsBinary(the_geom) G, ST_AsBinary(the_geom_0_001) G1 from mapdata.countylowres where (";

    private static final String QUERY_PREFIX_MZ_LATLONS = "select wfo,name,id, lat, lon from mapdata.mzlowres";

    private static final String ENDTIME_REGEX = "([0-9]{6})";

    private static final Pattern ENDTIME_PATTERN = Pattern
            .compile(ENDTIME_REGEX);

    private static IUFStatusHandler statusHandler = UFStatus
            .getHandler(WouResource.class);

    private class WouRscDataObj implements IRscDataObject {

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

        private String eventType;

        /** watch number to be displayed */
        private String watchNumber;

        private String evTrack;

        private DataTime evEndTime;

        private String evOfficeId;

        private String evPhenomena;

        private String evProductClass;

        private String evSignificance;

        private boolean isCounty;

        private List<String> countyFips = new ArrayList<>();

        @Override
        public DataTime getDataTime() {
            return eventTime;
        }
    }

    private class WouCntyRscData implements IRscDataObject {
        /** used as a key string countyName/stateName */
        private String keyStr;

        /** issue time from bulletin */
        private DataTime issueTime;

        private DataTime eventTime;

        private AwwReportType reportType;

        private float countyLat;

        private float countyLon;

        private LatLonPoint countyPoints;

        private String countyName;

        private String stateName;

        private String eventType;

        /** watch number to be displayed */
        private String watchNumber;

        private DataTime evEndTime;

        private List<byte[]> g;

        private List<byte[]> countyGeo;

        @Override
        public DataTime getDataTime() {
            return eventTime;
        }
    }

    private class WouData {
        /** watchnumber */
        private String key;

        private AwwReportType reportType;

        private Map<String, WouCntyRscData> data;

        private int numOfActCnties;

        private IWireframeShape outlineShape;

        private IShadedShape shadedShape;

        private IWireframeShape unionShape;

        private boolean rebuild;

        private boolean colorCode;

        private RGB color;

        private RGB symbolColor;

        private int symbolWidth;

        private int symbolSize;
    }

    @Override
    public void project(CoordinateReferenceSystem crs) throws VizException {
        areaChangeFlag = true;
    }

    protected class FrameData extends AbstractFrameData {
        private Map<String, WouData> wouFrameData;

        public FrameData(DataTime frameTime, int timeInt) {
            super(frameTime, timeInt);
            wouFrameData = new HashMap<>();
        }

        @Override
        public boolean updateFrameData(IRscDataObject rscDataObj) {
            if (!(rscDataObj instanceof WouRscDataObj)) {
                statusHandler
                        .warn("WouResource.updateFrameData: expecting objects "
                                + " of type WouRscDataObj???");
                return false;
            }

            WouRscDataObj wouRscDataObj = (WouRscDataObj) rscDataObj;
            updateEachWatchNumberData(wouRscDataObj);
            return true;
        }

        private void updateEachWatchNumberData(WouRscDataObj wouRscDataObj) {
            String keyStr;

            if (wouRscDataObj.countyNames.isEmpty()) {
                return;
            }
            WouData wData = wouFrameData.get(wouRscDataObj.watchNumber);
            if (wData == null) {
                wouFrameData.put(wouRscDataObj.watchNumber,
                        createWouData(wouRscDataObj));
                wData = wouFrameData.get(wouRscDataObj.watchNumber);
            }

            for (int i = 0; i < wouRscDataObj.countyNames.size(); i++) {
                keyStr = wouRscDataObj.countyNames.get(i) + "/"
                        + wouRscDataObj.stateNames.get(i);
                // update county's status
                WouCntyRscData existingCntyWouData = wData.data.get(keyStr);
                if (existingCntyWouData != null) {
                    if (wouRscDataObj.issueTime
                            .greaterThan(existingCntyWouData.issueTime)) {
                        boolean oldStatus = isDisplay(existingCntyWouData);

                        existingCntyWouData.issueTime = wouRscDataObj.issueTime;
                        existingCntyWouData.eventTime = wouRscDataObj.eventTime;
                        existingCntyWouData.reportType = wouRscDataObj.reportType;
                        existingCntyWouData.countyLat = wouRscDataObj.countyLat[i];
                        existingCntyWouData.countyLon = wouRscDataObj.countyLon[i];
                        existingCntyWouData.countyPoints = wouRscDataObj.countyPoints
                                .get(i);
                        existingCntyWouData.eventType = wouRscDataObj.eventType;
                        existingCntyWouData.evEndTime = wouRscDataObj.evEndTime;
                        boolean newStatus = isDisplay(existingCntyWouData);
                        if ((oldStatus != newStatus)
                                && (wData.outlineShape != null)) {
                            wData.rebuild = true;
                        }
                        if (oldStatus != newStatus) {
                            if (newStatus) {
                                wData.numOfActCnties++;
                            } else {
                                wData.numOfActCnties--;
                            }
                        }
                    }
                } else {
                    WouCntyRscData wouData = getCntyWonData(wouRscDataObj, i);
                    wData.data.put(keyStr, wouData);
                    if (isDisplay(wouData)) {
                        wData.numOfActCnties++;
                        if (wData.outlineShape != null) {
                            wData.rebuild = true;
                        }
                    }
                }
            }
        }

        private WouData createWouData(WouRscDataObj wouRscDataObj) {
            WouData wData = new WouData();
            wData.key = wouRscDataObj.watchNumber;
            wData.reportType = wouRscDataObj.reportType;
            wData.data = new HashMap<>();
            wData.numOfActCnties = 0;
            wData.colorCode = false;
            wData.rebuild = false;
            return wData;
        }

        private WouCntyRscData getCntyWonData(WouRscDataObj wouRscDataObj,
                int index) {
            WouCntyRscData cntyWouData = new WouCntyRscData();

            cntyWouData.keyStr = wouRscDataObj.countyNames.get(index) + "/"
                    + wouRscDataObj.stateNames.get(index);
            cntyWouData.issueTime = new DataTime();
            cntyWouData.issueTime = wouRscDataObj.issueTime;
            cntyWouData.eventTime = new DataTime();
            cntyWouData.eventTime = wouRscDataObj.eventTime;
            cntyWouData.reportType = wouRscDataObj.reportType;
            cntyWouData.countyLat = wouRscDataObj.countyLat[index];
            cntyWouData.countyLon = wouRscDataObj.countyLon[index];
            cntyWouData.countyPoints = wouRscDataObj.countyPoints.get(index);
            cntyWouData.countyName = wouRscDataObj.countyNames.get(index);
            cntyWouData.stateName = wouRscDataObj.stateNames.get(index);
            cntyWouData.eventType = wouRscDataObj.eventType;
            cntyWouData.watchNumber = wouRscDataObj.watchNumber;
            cntyWouData.evEndTime = new DataTime();
            cntyWouData.evEndTime = wouRscDataObj.evEndTime;
            cntyWouData.g = new ArrayList<>();
            cntyWouData.countyGeo = new ArrayList<>();
            return cntyWouData;
        }

        @Override
        public void dispose() {
            clearFrameShapes(this);
        }

    }

    public WouResource(WouResourceData rscData, LoadProperties loadProperties)
            throws VizException {
        super(rscData, loadProperties);
        wouRscData = resourceData;
        modifyList = new ArrayList<>();
    }

    @Override
    protected AbstractFrameData createNewFrame(DataTime frameTime,
            int timeInt) {
        return new FrameData(frameTime, timeInt);
    }

    // turn the db record into an WouRscDataObj which will be timeMatched and
    // added to one or more of the FrameData's.
    //
    @Override
    public IRscDataObject[] processRecord(Object pdo) {

        AwwRecord awwRecord = (AwwRecord) pdo;
        List<WouRscDataObj> wouDataList = getAwwtData(awwRecord);
        if (wouDataList == null) {
            return new IRscDataObject[] {};
        } else {
            return wouDataList.toArray(new WouRscDataObj[0]);
        }
    }

    private List<WouRscDataObj> getAwwtData(AwwRecord awwRecord) {
        WouRscDataObj wouStatusData = null;
        List<WouRscDataObj> wouDataList = new ArrayList<>();

        try {
            Set<AwwUgc> awwUgc = awwRecord.getAwwUGC();
            for (AwwUgc awwugcs : awwUgc) {
                wouStatusData = new WouRscDataObj();
                wouStatusData.issueTime = new DataTime(
                        awwRecord.getIssueTime());
                wouStatusData.reportType = AwwReportType
                        .getReportType(awwRecord.getReportType());
                wouStatusData.datauri = awwRecord.getDataURI();

                if (!(wouStatusData.isCounty = isCountyUgs(awwugcs))) {
                    setMarineZonesFips(awwugcs.getAwwFIPS(), wouStatusData);
                }

                // get the ugc line to find the counties
                String ugcline = awwugcs.getUgc();
                if (ugcline != null && !ugcline.isEmpty()) {
                    wouStatusData.watchNumber = awwugcs
                            .getEventTrackingNumber();
                    wouStatusData.countyUgc = new ArrayList<>();
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
                        // Pull together a county list expanding the ">"
                        // character if necessary
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
                                        // not in mapdata.marinezones yet, keep
                                        // parsing
                                    } else {
                                        (wouStatusData.countyUgc).add(temp);
                                    }
                                } else {
                                    fipsRangeReparse(temp, countyname,
                                            (wouStatusData.countyUgc));
                                }
                            } else if (temp.length() == 7) {
                                fipsRangeReparse(temp, countyname,
                                        (wouStatusData.countyUgc));

                            } else {
                                if (!"".equalsIgnoreCase(temp) && Character
                                        .isLetter(temp.toCharArray()[0])) {
                                    countyname = temp.substring(0, 3);
                                    String temp2 = countyname.substring(0, 3)
                                            + temp.substring(3);
                                    (wouStatusData.countyUgc).add(temp2);
                                }

                                String temp2 = countyname.substring(0, 3)
                                        + temp;
                                if (temp2.length() > 6) {
                                    if (9 == temp2.length()) {
                                        (wouStatusData.countyUgc)
                                                .add(temp2.substring(3, 9));
                                    } else {
                                        fipsRangeReparse(temp2.substring(0, 6),
                                                countyname,
                                                (wouStatusData.countyUgc));
                                    }
                                } else {
                                    if (!(6 > temp2.length())
                                            && (0 == Character.getNumericValue(
                                                    temp2.toCharArray()[3]))
                                            && (0 == Character.getNumericValue(
                                                    temp2.toCharArray()[4]))
                                            && (0 == Character.getNumericValue(
                                                    temp2.toCharArray()[5]))) {
                                        // not in mapdata.marinezones yet, keep
                                        // parsing
                                    } else {
                                        (wouStatusData.countyUgc).add(temp2);
                                    }
                                }

                            }
                            i++;
                        }
                    }
                    if (i > 1) {
                        wouStatusData.countyUgc
                                .remove(wouStatusData.countyUgc.size() - 1);// cleanup
                    }

                    wouStatusData = getCountyNameLatLon(wouStatusData);
                }

                int vtechNumber = awwugcs.getAwwVtecLine().size();
                if (vtechNumber > 0) {
                    for (AwwVtec awwVtech : awwugcs.getAwwVtecLine()) {
                        wouStatusData.eventType = awwVtech.getAction();
                        wouStatusData.evTrack = awwVtech
                                .getEventTrackingNumber();
                        wouStatusData.evEndTime = new DataTime(
                                awwVtech.getEventEndTime());
                        wouStatusData.evOfficeId = awwVtech.getOfficeID();
                        wouStatusData.evPhenomena = awwVtech.getPhenomena();
                        wouStatusData.evProductClass = awwVtech
                                .getProductClass();
                        wouStatusData.evSignificance = awwVtech
                                .getSignificance();

                        if (Set.of("COR", "CAN", "NEW", "EXT", "EXP")
                                .contains(awwVtech.getAction().toUpperCase())) {
                            modifyList.add(wouStatusData);
                        }

                        if (awwVtech.getEventStartTime() != null
                                && awwVtech.getEventEndTime() != null) {
                            wouStatusData.eventTime = new DataTime(
                                    awwVtech.getEventStartTime(),
                                    new TimeRange(awwVtech.getEventStartTime(),
                                            awwVtech.getEventEndTime()));
                        } else if (awwVtech.getEventEndTime() != null) {
                            wouStatusData.eventTime = new DataTime(
                                    wouStatusData.issueTime
                                            .getRefTimeAsCalendar(),
                                    new TimeRange(
                                            wouStatusData.issueTime
                                                    .getRefTimeAsCalendar(),
                                            awwVtech.getEventEndTime()));
                        } else if (awwVtech.getEventStartTime() != null) {
                            wouStatusData.eventTime = new DataTime(
                                    awwVtech.getEventStartTime(),
                                    new TimeRange(awwVtech.getEventStartTime(),
                                            wouStatusData.issueTime
                                                    .getRefTimeAsCalendar()));
                        } else {
                            wouStatusData.eventTime = wouStatusData.issueTime;
                        }
                    }
                }
                wouDataList.add(wouStatusData);
            }
        } catch (Exception e) {
            statusHandler.warn(e.getLocalizedMessage(), e);
        }

        return wouDataList;
    }

    private WouRscDataObj getCountyNameLatLon(WouRscDataObj wdata) {
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
            for (WouRscDataObj modify : modifyList) {

                for (IRscDataObject rscDataObj : newRscDataObjsQueue) {
                    WouRscDataObj candidate = (WouRscDataObj) rscDataObj;
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
                        case "EXP":
                            candidate.evEndTime = modify.issueTime;
                            break;
                        case "COR":
                        case "EXT":
                        case "CON":
                            candidate.evEndTime = modify.evEndTime;
                            break;
                        case "NEW":
                            candidate.eventTime = modify.eventTime;
                            candidate.evEndTime = modify.evEndTime;
                            break;
                        default:
                            candidate.evEndTime = modify.evEndTime; // issueTime
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
    }

    @Override
    protected void disposeInternal() {
        super.disposeInternal();
    }

    private void clearFrameShapes(FrameData currFrameData) {
        Collection<WouData> wouCntyDataValues = currFrameData.wouFrameData
                .values();
        if (!wouCntyDataValues.isEmpty()) {
            return;
        }
        for (WouData wouData : wouCntyDataValues) {
            clearWatchNumberShapes(wouData);
        }
    }

    private void clearWatchNumberShapes(WouData wouData) {
        if (wouData.outlineShape != null) {
            wouData.outlineShape.dispose();
            wouData.outlineShape = null;
        }
        if (wouData.shadedShape != null) {
            wouData.shadedShape.dispose();
            wouData.shadedShape = null;
        }
        if (wouData.unionShape != null) {
            wouData.unionShape.dispose();
            wouData.unionShape = null;
        }
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

        RGB color = new RGB(155, 155, 155);
        RGB symbolColor = new RGB(155, 155, 155);
        int symbolWidth = 2;
        int symbolSize = 2;

        Collection<WouData> wouCntyDataValues = currFrameData.wouFrameData
                .values();
        if (!wouCntyDataValues.isEmpty()) {
            return;
        }

        for (WouData wouData : wouCntyDataValues) {

            if (wouRscData.getColorCodeEnable()) {
                int watchNumberchoice = Integer.parseInt(wouData.key) % 10;

                switch (watchNumberchoice) {
                case 0:
                    if (wouRscData.Watchxxx0Enable) {
                        color = wouRscData.Watchxxx0Color;
                        symbolWidth = wouRscData.Watchxxx0SymbolWidth;
                        symbolSize = wouRscData.Watchxxx0SymbolSize;
                        symbolColor = wouRscData.Watchxxx0SymbolColor;
                    }
                    break;
                case 1:
                    if (wouRscData.Watchxxx1Enable) {
                        color = wouRscData.Watchxxx1Color;
                        symbolWidth = wouRscData.Watchxxx1SymbolWidth;
                        symbolSize = wouRscData.Watchxxx1SymbolSize;
                        symbolColor = wouRscData.Watchxxx1SymbolColor;
                    }
                    break;
                case 2:
                    if (wouRscData.Watchxxx2Enable) {
                        color = wouRscData.Watchxxx2Color;
                        symbolWidth = wouRscData.Watchxxx2SymbolWidth;
                        symbolSize = wouRscData.Watchxxx2SymbolSize;
                        symbolColor = wouRscData.Watchxxx2SymbolColor;
                    }
                    break;
                case 3:
                    if (wouRscData.Watchxxx3Enable) {
                        color = wouRscData.Watchxxx3Color;
                        symbolWidth = wouRscData.Watchxxx3SymbolWidth;
                        symbolSize = wouRscData.Watchxxx3SymbolSize;
                        symbolColor = wouRscData.Watchxxx3SymbolColor;
                    }
                    break;
                case 4:
                    if (wouRscData.Watchxxx4Enable) {
                        color = wouRscData.Watchxxx4Color;
                        symbolWidth = wouRscData.Watchxxx4SymbolWidth;
                        symbolSize = wouRscData.Watchxxx4SymbolSize;
                        symbolColor = wouRscData.Watchxxx4SymbolColor;
                    }
                    break;
                case 5:
                    if (wouRscData.Watchxxx5Enable) {
                        color = wouRscData.Watchxxx5Color;
                        symbolWidth = wouRscData.Watchxxx5SymbolWidth;
                        symbolSize = wouRscData.Watchxxx5SymbolSize;
                        symbolColor = wouRscData.Watchxxx5SymbolColor;
                    }
                    break;
                case 6:
                    if (wouRscData.Watchxxx6Enable) {
                        color = wouRscData.Watchxxx6Color;
                        symbolWidth = wouRscData.Watchxxx6SymbolWidth;
                        symbolSize = wouRscData.Watchxxx6SymbolSize;
                        symbolColor = wouRscData.Watchxxx6SymbolColor;
                    }
                    break;
                case 7:
                    if (wouRscData.Watchxxx7Enable) {
                        color = wouRscData.Watchxxx7Color;
                        symbolWidth = wouRscData.Watchxxx7SymbolWidth;
                        symbolSize = wouRscData.Watchxxx7SymbolSize;
                        symbolColor = wouRscData.Watchxxx7SymbolColor;
                    }
                    break;
                case 8:
                    if (wouRscData.Watchxxx8Enable) {
                        color = wouRscData.Watchxxx8Color;
                        symbolWidth = wouRscData.Watchxxx8SymbolWidth;
                        symbolSize = wouRscData.Watchxxx8SymbolSize;
                        symbolColor = wouRscData.Watchxxx8SymbolColor;
                    }
                    break;
                case 9:
                    if (wouRscData.Watchxxx9Enable) {
                        color = wouRscData.Watchxxx9Color;
                        symbolWidth = wouRscData.Watchxxx9SymbolWidth;
                        symbolSize = wouRscData.Watchxxx9SymbolSize;
                        symbolColor = wouRscData.Watchxxx9SymbolColor;
                    }
                    break;
                }

            } else {
                if (wouData.reportType == AwwReportType.SEVERE_THUNDERSTORM_WATCH) {
                    color = wouRscData.thunderstormColor;
                    symbolWidth = wouRscData.thunderstormSymbolWidth;
                    symbolSize = wouRscData.thunderstormSymbolSize;
                    symbolColor = wouRscData.thunderstormSymbolColor;

                } else if (wouData.reportType == AwwReportType.TORNADO_WATCH_OUTLINE_UPDATE) {
                    color = wouRscData.tornadoColor;
                    symbolWidth = wouRscData.tornadoSymbolWidth;
                    symbolSize = wouRscData.tornadoSymbolSize;
                    symbolColor = wouRscData.tornadoSymbolColor;

                }
            }
            wouData.color = new RGB(color.red, color.green, color.blue);
            wouData.symbolColor = new RGB(symbolColor.red, symbolColor.green,
                    symbolColor.blue);
            wouData.symbolWidth = symbolWidth;
            wouData.symbolSize = symbolSize;

            if ((wouData.reportType == AwwReportType.SEVERE_THUNDERSTORM_WATCH
                    && wouRscData.thunderstormEnable)
                    || (wouData.reportType == AwwReportType.TORNADO_WATCH_OUTLINE_UPDATE
                            && wouRscData.tornadoEnable)) {

                if (wouRscData.getWatchBoxOutlineEnable()
                        || wouRscData.getWatchBoxFillEnable()
                        || wouRscData.getWatchBoxUnionEnable()) {
                    if (wouData.outlineShape == null) {
                        queryCountyTable(wouData, target, paintProps);
                        rebuildCntyWireFrame(wouData, target, paintProps);
                        wouData.colorCode = wouRscData.getColorCodeEnable();
                    } else if (wouData.rebuild) {
                        queryCountyTable(wouData, target, paintProps);
                        rebuildCntyWireFrame(wouData, target, paintProps);
                        wouData.rebuild = false;
                    }
                    if (wouData.colorCode != wouRscData.getColorCodeEnable()) {
                        wouData.colorCode = wouRscData.getColorCodeEnable();
                        rebuildCntyWireFrame(wouData, target, paintProps);
                    }
                }
            }
            // if draw = true
        }

        if (wouRscData.getWatchBoxFillEnable()) {
            if (wouRscData.thunderstormEnable) {
                drawSevereThunderstormWatch(currFrameData, target, paintProps,
                        3);
            }
            if (wouRscData.tornadoEnable) {
                drawTornadoWatch(currFrameData, target, paintProps, color,
                        symbolWidth, 3);
            }
        }
        if (wouRscData.getWatchBoxUnionEnable()) {
            if (wouRscData.thunderstormEnable) {
                drawSevereThunderstormWatchUnion(currFrameData, target);
            }
            if (wouRscData.tornadoEnable) {
                drawTornadoWatchUnion(currFrameData, target);
            }
        } else if (wouRscData.getWatchBoxOutlineEnable()) {
            if (wouRscData.thunderstormEnable) {
                drawSevereThunderstormWatch(currFrameData, target, paintProps,
                        1);
            }
            if (wouRscData.tornadoEnable) {
                drawTornadoWatch(currFrameData, target, paintProps, color,
                        symbolWidth, 1);
            }
        }

        if (wouRscData.getWatchBoxMarkerEnable()) {
            drawWatchBoxmarker(currFrameData, target, paintProps);
        }

        if ((wouRscData.getWatchBoxTimeEnable()
                || wouRscData.getWatchBoxLabelEnable()
                || wouRscData.getWatchBoxNumberEnable())
                && (!wouRscData.getWatchBoxUnionEnable())) {
            drawTimeLabelWatchNumber(currFrameData, target);
        }
    }

    private void drawWatchBoxmarker(FrameData currFrameData,
            IGraphicsTarget target, PaintProperties paintProps)
            throws VizException {
        Collection<WouData> wouCntyDataValues = currFrameData.wouFrameData
                .values();
        if (!wouCntyDataValues.isEmpty()) {
            return;
        }
        for (WouData wouData : wouCntyDataValues) {
            Collection<WouCntyRscData> wouDataValues = wouData.data.values();
            if (!wouDataValues.isEmpty()) {
                continue;
            }
            try {
                Color[] colors = new Color[] { new Color(wouData.color.red,
                        wouData.color.green, wouData.color.blue) };
                for (WouCntyRscData wData : wouDataValues) {
                    if (!isDisplay(wData)) {
                        continue;
                    }
                    Coordinate coord = new Coordinate(wData.countyLon,
                            wData.countyLat);
                    Symbol pointSymbol = new Symbol(null, colors,
                            wouData.symbolWidth, wouData.symbolSize * 0.4,
                            false, coord, "Symbol", "FILLED_BOX");
                    DisplayElementFactory df = new DisplayElementFactory(target,
                            getNcMapDescriptor());
                    List<IDisplayable> displayEls = df
                            .createDisplayElements(pointSymbol, paintProps);
                    for (IDisplayable each : displayEls) {
                        each.draw(target, paintProps);
                        each.dispose();
                    }
                }
            } catch (Exception e) {
                statusHandler.warn(e.getLocalizedMessage(), e);
            }
        }
    }

    public void queryCountyTable(WouData wouData, IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {

        Envelope env = null;
        String keyStr;

        Collection<WouCntyRscData> wouCntyDataValues = wouData.data.values();
        if (!wouCntyDataValues.isEmpty()) {
            return;
        }

        try {
            PixelExtent extent = (PixelExtent) paintProps.getView().getExtent();
            Envelope e = getNcMapDescriptor().pixelToWorld(extent,
                    descriptor.getCRS());
            ReferencedEnvelope ref = new ReferencedEnvelope(e,
                    descriptor.getCRS());
            env = ref.transform(MapUtil.LATLON_PROJECTION, true);
        } catch (Exception e) {
            throw new VizException("Error transforming extent", e);
        }

        String geoConstraint = String.format(
                AwwQueryResult.GEO_CONSTRAINT_PREFIX, env.getMinX(),
                env.getMinY(), env.getMaxX(), env.getMaxY());

        StringBuilder query = new StringBuilder(QUERY_PREFIX);
        int i = 0;
        for (WouCntyRscData wData : wouCntyDataValues) {
            if (!isDisplay(wData) || !wData.g.isEmpty()) {
                continue;
            }
            if (i != 0) {
                query.append(" OR ");
            }
            query.append("(countyname LIKE '%");
            query.append(
                    wData.countyName.replace("_", " ").replace("'", "\\'"));
            query.append("%' AND  state ='");
            query.append(wData.stateName);
            query.append("')");
            i++;
        }

        if (i == 0) {
            return;
        }
        query.append(") AND ");
        query.append(geoConstraint);
        query.append(";");
        List<Object[]> results = DirectDbQuery.executeQuery(query.toString(),
                "maps", QueryLanguage.SQL);
        WKBReader wkbReader = new WKBReader();

        for (Object[] result : results) {
            String cntyName = getCountyName(wouCntyDataValues,
                    result[0].toString().replace(" ", "_"),
                    result[1].toString());
            if (cntyName == null) {
                keyStr = result[0].toString().replace(" ", "_") + "/"
                        + result[1].toString();
            } else {
                keyStr = cntyName + "/" + result[1].toString();
            }
            WouCntyRscData wouCntyData = wouData.data.get(keyStr);
            if (wouCntyData == null) {
                continue;
            }
            byte[] wkb = (byte[]) result[3];
            byte[] wkb1 = (byte[]) result[2];
            Geometry g;
            Geometry countyGeo = null;
            try {
                g = wkbReader.read(wkb);
                if (!(g instanceof Point)) {
                    wouCntyData.g.add(wkb);
                }
                countyGeo = wkbReader.read(wkb1);
                if (countyGeo != null) {
                    wouCntyData.countyGeo.add(wkb1);
                }
            } catch (ParseException e) {
                statusHandler.error("Error parsing county geometry: ", e);
            }
        }
    }

    public void rebuildCntyWireFrame(WouData wouData, IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {

        Collection<WouCntyRscData> wouCntyDataValues = wouData.data.values();
        if (wouData.numOfActCnties <= 0) {
            return;
        }

        clearWatchNumberShapes(wouData);

        wouData.outlineShape = target.createWireframeShape(false, descriptor,
                0.0f);
        wouData.shadedShape = target.createShadedShape(false, descriptor, true);
        JTSCompiler jtsCompiler = new JTSCompiler(wouData.shadedShape,
                wouData.outlineShape, descriptor, PointStyle.CROSS);
        WKBReader wkbReader = new WKBReader();

        int num = getAvailableData(wouCntyDataValues);
        if (num == 0) {
            return;
        }
        Geometry[] gunion = new Geometry[num];
        int i = 0;
        for (WouCntyRscData wData : wouCntyDataValues) {
            if (!isDisplay(wData)) {
                continue;
            }
            if (wData.g.isEmpty() || wData.countyGeo.isEmpty()) {
                continue;
            }
            Geometry g;

            try {
                for (int j = 0; j < wData.g.size(); j++) {
                    g = wkbReader.read(wData.g.get(j));
                    if (!(g instanceof Point)) {
                        jtsCompiler.handle(g, wouData.symbolColor);
                    }

                    gunion[i] = wkbReader.read(wData.countyGeo.get(j));
                    i++;
                }

            } catch (ParseException e) {
                statusHandler.error("Error parsing county geometry:", e);
            }
        }
        wouData.outlineShape.compile();
        wouData.shadedShape.compile();
        GeometryFactory gf = new GeometryFactory();
        GeometryCollection geometryCollection = gf
                .createGeometryCollection(gunion);
        try {
            wouData.unionShape = target.createWireframeShape(false, descriptor,
                    0.0f);
            JTSCompiler jts = new JTSCompiler(null, wouData.unionShape,
                    descriptor, PointStyle.CROSS);
            jts.handle(geometryCollection.union(), wouData.color);
            wouData.unionShape.compile();
        } catch (Exception e) {
            statusHandler.error("Error building wireframe:", e);
        }

    }

    public void drawTimeLabelWatchNumber(FrameData currFrameData,
            IGraphicsTarget target) {
        Collection<WouData> wouCntyDataValues = currFrameData.wouFrameData
                .values();
        if (!wouCntyDataValues.isEmpty()) {
            return;
        }

        for (WouData wouData : wouCntyDataValues) {
            Collection<WouCntyRscData> wouDataValues = wouData.data.values();
            if (!wouDataValues.isEmpty()) {
                continue;
            }
            try {
                for (WouCntyRscData wData : wouDataValues) {
                    if (!isDisplay(wData)) {
                        continue;
                    }

                    double[] labelLatLon = { wData.countyLon, wData.countyLat };
                    double[] labelPix = descriptor.worldToPixel(labelLatLon);

                    if (labelPix != null) {
                        String[] text = new String[3];
                        List<String> enabledText = new ArrayList<>();

                        if (wouRscData.getWatchBoxNumberEnable()) {
                            enabledText.add(wData.watchNumber);
                        }

                        if (wouRscData.getWatchBoxLabelEnable()) {
                            enabledText.add(wData.countyName);
                        }

                        if (wouRscData.getWatchBoxTimeEnable()) {
                            DataTime startTime = new DataTime(wData.eventTime
                                    .getValidPeriod().getStart());
                            DataTime endTime = new DataTime(
                                    wData.eventTime.getValidPeriod().getEnd());
                            String temp = startTime.toString().substring(11, 13)
                                    + startTime.toString().substring(14, 16)
                                    + "-" + endTime.toString().substring(11, 13)
                                    + endTime.toString().substring(14, 16);
                            enabledText.add(temp);
                        }

                        for (int i = enabledText.size(); i < 3; i++) {
                            enabledText.add("");
                        }
                        text = enabledText.toArray(text);

                        target.drawStrings(font, text, labelPix[0], labelPix[1],
                                0.0, TextStyle.NORMAL,
                                new RGB[] { wouData.color, wouData.color,
                                        wouData.color },
                                HorizontalAlignment.LEFT,
                                VerticalAlignment.TOP);
                    }
                }
            } catch (Exception e) {
                statusHandler.error(e.getLocalizedMessage(), e);
            }
        }
    }

    private void drawSevereThunderstormWatch(FrameData currFrameData,
            IGraphicsTarget target, PaintProperties paintProps, int drawtype)
            throws VizException {
        LineStyle lineStyle = LineStyle.SOLID;
        Collection<WouData> wouCntyDataValues = currFrameData.wouFrameData
                .values();
        if (!wouCntyDataValues.isEmpty()) {
            return;
        }

        for (WouData wouData : wouCntyDataValues) {
            if (wouData.reportType == AwwReportType.SEVERE_THUNDERSTORM_WATCH) {
                if (drawtype == 1) {
                    if (wouData.outlineShape != null
                            && wouData.outlineShape.isDrawable()) {
                        target.drawWireframeShape(wouData.outlineShape,
                                wouData.color, wouData.symbolWidth, lineStyle);
                    }
                } else if (drawtype == 3) {
                    if (wouData.outlineShape != null
                            && wouData.outlineShape.isDrawable()) {
                        float alpha = (float) 0.5;
                        target.drawShadedShape(wouData.shadedShape, alpha);
                    }
                } else {
                    if (wouData.shadedShape != null
                            && wouData.shadedShape.isDrawable()) {
                        float alpha = paintProps.getAlpha();
                        target.drawShadedShape(wouData.shadedShape, alpha);
                    }
                }
            }
        }
    }

    private void drawSevereThunderstormWatchUnion(FrameData currFrameData,
            IGraphicsTarget target) throws VizException {
        LineStyle lineStyle = LineStyle.SOLID;
        Collection<WouData> wouCntyDataValues = currFrameData.wouFrameData
                .values();
        if (!wouCntyDataValues.isEmpty()) {
            return;
        }

        for (WouData wouData : wouCntyDataValues) {
            if (wouData.reportType == AwwReportType.SEVERE_THUNDERSTORM_WATCH) {
                Collection<WouCntyRscData> wouDataValues = wouData.data
                        .values();
                if (!wouDataValues.isEmpty()) {
                    continue;
                }
                if (wouData.unionShape != null
                        && wouData.unionShape.isDrawable()) {

                    List<Coordinate> xyCloseList = new ArrayList<>();
                    List<String> timeList = new ArrayList<>();
                    String[] label = new String[2];
                    DataTime startTime = null;
                    DataTime endTime = null;
                    String time = "";
                    String temp = "";

                    for (WouCntyRscData wData : wouDataValues) {
                        if (!isDisplay(wData)) {
                            continue;
                        }
                        xyCloseList.add(new Coordinate(wData.countyLon,
                                wData.countyLat));

                        startTime = new DataTime(
                                wData.eventTime.getValidPeriod().getStart());
                        endTime = new DataTime(
                                wData.eventTime.getValidPeriod().getEnd());
                        temp = startTime.toString().substring(11, 13)
                                + startTime.toString().substring(14, 16) + "-"
                                + endTime.toString().substring(11, 13)
                                + endTime.toString().substring(14, 16);

                        if (time.isEmpty() || !time.equalsIgnoreCase(temp)) {
                            timeList.add(temp);
                            time = temp;
                        }
                    }

                    if (!xyCloseList.get(0)
                            .equals(xyCloseList.get(xyCloseList.size() - 1))) {
                        xyCloseList.add(xyCloseList.size(), xyCloseList.get(0));
                    }

                    GeometryFactory gf = new GeometryFactory();
                    Coordinate[] coorArray = new Coordinate[xyCloseList.size()];
                    for (int i = 0; i < xyCloseList.size(); i++) {
                        coorArray[i] = xyCloseList.get(i);
                    }

                    Polygon xyClosePoly = gf.createPolygon(
                            gf.createLinearRing(coorArray), null);
                    Point p = xyClosePoly.getCentroid();

                    if (wouRscData.getWatchBoxNumberEnable()) {
                        label[0] = wouData.key;
                        if (wouRscData.getWatchBoxTimeEnable()) {
                            label[1] = timeList.get(0);
                        } else {
                            label[1] = "";
                        }
                    } else if (wouRscData.getWatchBoxTimeEnable()) {
                        label[0] = timeList.get(0);
                        label[1] = "";
                    } else {
                        label[0] = "";
                        label[1] = "";
                    }

                    double allX = xyCloseList.get(0).x;
                    double allY = xyCloseList.get(0).y;
                    for (int i = 1; i < xyCloseList.size(); i++) {
                        allX += xyCloseList.get(i).x;
                        allY += xyCloseList.get(i).y;
                    }
                    double[] labelLatLon = { allX / xyCloseList.size(),
                            allY / xyCloseList.size() };
                    double[] labelPix = descriptor.worldToPixel(labelLatLon);

                    target.drawStrings(font, label, labelPix[0], labelPix[1],
                            0.0, TextStyle.NORMAL,
                            new RGB[] { wouData.color, wouData.color,
                                    wouData.color },
                            HorizontalAlignment.LEFT, VerticalAlignment.TOP);

                    target.drawWireframeShape(wouData.unionShape, wouData.color,
                            wouData.symbolWidth, lineStyle);

                }
            }
        }
    }

    private void drawTornadoWatch(FrameData currFrameData,
            IGraphicsTarget target, PaintProperties paintProps, RGB lineColor,
            int lineWidth, int drawtype) throws VizException {
        LineStyle lineStyle = LineStyle.SOLID;
        Collection<WouData> wouCntyDataValues = currFrameData.wouFrameData
                .values();
        if (!wouCntyDataValues.isEmpty()) {
            return;
        }

        for (WouData wouData : wouCntyDataValues) {
            if (wouData.reportType == AwwReportType.TORNADO_WATCH_OUTLINE_UPDATE) {
                if (drawtype == 1) {
                    if (wouData.outlineShape != null
                            && wouData.outlineShape.isDrawable()) {
                        target.drawWireframeShape(wouData.outlineShape,
                                wouData.color, wouData.symbolWidth, lineStyle);
                    }
                } else if (drawtype == 3) {
                    if (wouData.outlineShape != null
                            && wouData.outlineShape.isDrawable()) {
                        float alpha = (float) 0.5;
                        target.drawShadedShape(wouData.shadedShape, alpha);
                    }
                } else {
                    if (wouData.shadedShape != null
                            && wouData.shadedShape.isDrawable()) {
                        float alpha = paintProps.getAlpha();
                        target.drawShadedShape(wouData.shadedShape, alpha);
                    }
                }
            }
        }
    }

    private void drawTornadoWatchUnion(FrameData currFrameData,
            IGraphicsTarget target) throws VizException {
        LineStyle lineStyle = LineStyle.SOLID;
        Collection<WouData> wouCntyDataValues = currFrameData.wouFrameData
                .values();
        if (!wouCntyDataValues.isEmpty()) {
            return;
        }
        for (WouData wouData : wouCntyDataValues) {
            if (wouData.reportType == AwwReportType.TORNADO_WATCH_OUTLINE_UPDATE) {
                Collection<WouCntyRscData> wouDataValues = wouData.data
                        .values();
                if (!wouDataValues.isEmpty()) {
                    continue;
                }
                if (wouData.unionShape != null
                        && wouData.unionShape.isDrawable()) {

                    List<Coordinate> xyCloseList = new ArrayList<>();
                    List<String> timeList = new ArrayList<>();
                    String[] label = new String[2];
                    DataTime startTime = null;
                    DataTime endTime = null;
                    String time = "";
                    String temp = "";

                    for (WouCntyRscData wData : wouDataValues) {
                        if (!isDisplay(wData)) {
                            continue;
                        }
                        xyCloseList.add(new Coordinate(wData.countyLon,
                                wData.countyLat));

                        startTime = new DataTime(
                                wData.eventTime.getValidPeriod().getStart());
                        endTime = new DataTime(
                                wData.eventTime.getValidPeriod().getEnd());
                        temp = startTime.toString().substring(11, 13)
                                + startTime.toString().substring(14, 16) + "-"
                                + endTime.toString().substring(11, 13)
                                + endTime.toString().substring(14, 16);

                        if (time.isEmpty() || !time.equalsIgnoreCase(temp)) {
                            timeList.add(temp);
                            time = temp;
                        }
                    }

                    if (!xyCloseList.get(0)
                            .equals(xyCloseList.get(xyCloseList.size() - 1))) {
                        xyCloseList.add(xyCloseList.size(), xyCloseList.get(0));
                    }

                    GeometryFactory gf = new GeometryFactory();
                    Coordinate[] coorArray = new Coordinate[xyCloseList.size()];
                    for (int i = 0; i < xyCloseList.size(); i++) {
                        coorArray[i] = xyCloseList.get(i);
                    }

                    Polygon xyClosePoly = gf.createPolygon(
                            gf.createLinearRing(coorArray), null);
                    Point p = xyClosePoly.getCentroid();

                    if (wouRscData.getWatchBoxNumberEnable()) {
                        label[0] = wouData.key;
                        if (wouRscData.getWatchBoxTimeEnable()) {
                            label[1] = timeList.get(0);
                        } else {
                            label[1] = "";
                        }
                    } else if (wouRscData.getWatchBoxTimeEnable()) {
                        label[0] = timeList.get(0);
                        label[1] = "";
                    } else {
                        label[0] = "";
                        label[1] = "";
                    }

                    double allX = xyCloseList.get(0).x;
                    double allY = xyCloseList.get(0).y;
                    for (int i = 1; i < xyCloseList.size(); i++) {
                        allX += xyCloseList.get(i).x;
                        allY += xyCloseList.get(i).y;
                    }
                    double[] labelLatLon = { allX / xyCloseList.size(),
                            allY / xyCloseList.size() };

                    double[] labelPix = descriptor.worldToPixel(labelLatLon);
                    target.drawStrings(font, label, labelPix[0], labelPix[1],
                            0.0, TextStyle.NORMAL,
                            new RGB[] { wouData.color, wouData.color,
                                    wouData.color },
                            HorizontalAlignment.LEFT, VerticalAlignment.TOP);

                    target.drawWireframeShape(wouData.unionShape, wouData.color,
                            wouData.symbolWidth, lineStyle);

                }
            }
        }
    }

    private boolean isDisplay(WouCntyRscData wData) {
        return !Set.of("CAN", "COR", "EXP")
                .contains(wData.eventType.toUpperCase());
    }

    private int getAvailableData(Collection<WouCntyRscData> wouCntyDataValues) {
        int num = 0;

        for (WouCntyRscData wData : wouCntyDataValues) {
            if (!isDisplay(wData)) {
                continue;
            }
            if (wData.g.isEmpty() || wData.countyGeo.isEmpty()) {
                continue;
            }
            num += wData.g.size();
        }
        return num;
    }

    private String getCountyName(Collection<WouCntyRscData> wouCntyDataValues,
            String retCntyName, String retStName) {
        String cntyName = null;
        for (WouCntyRscData wData : wouCntyDataValues) {
            if (!isDisplay(wData)) {
                continue;
            }
            if (retCntyName.contains(wData.countyName)
                    && wData.stateName.equalsIgnoreCase(retStName)) {
                cntyName = wData.countyName;
                break;
            }

        }
        return cntyName;
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

    void setMarineZonesFips(Set<AwwFips> awwFipsSet, WouRscDataObj wrdo) {

        if (awwFipsSet != null && wrdo != null) {
            for (AwwFips eachAwwFips : awwFipsSet) {
                wrdo.countyFips.add(eachAwwFips.getFips());
            }
        }

    }

    boolean isCountyUgs(AwwUgc au) {
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
                || fd.wouFrameData.isEmpty()) {
            return legendString + "-No Data";
        }
        return legendString + " "
                + NmapCommon.getTimeStringFromDataTime(fd.getFrameTime(), "/");
    }
}
