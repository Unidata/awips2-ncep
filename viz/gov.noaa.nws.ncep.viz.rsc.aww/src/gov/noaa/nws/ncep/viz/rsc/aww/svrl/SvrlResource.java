package gov.noaa.nws.ncep.viz.rsc.aww.svrl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.swt.graphics.RGB;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
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
import com.raytheon.uf.viz.core.map.IMapDescriptor;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.viz.core.rsc.jts.JTSCompiler;
import com.raytheon.viz.core.rsc.jts.JTSCompiler.PointStyle;
import com.raytheon.viz.ui.editor.AbstractEditor;

import gov.noaa.nws.ncep.common.dataplugin.aww.AwwRecord;
import gov.noaa.nws.ncep.common.dataplugin.aww.AwwRecord.AwwReportType;
import gov.noaa.nws.ncep.common.dataplugin.aww.AwwUgc;
import gov.noaa.nws.ncep.common.dataplugin.aww.AwwVtec;
import gov.noaa.nws.ncep.edex.common.stationTables.IStationField;
import gov.noaa.nws.ncep.edex.common.stationTables.Station;
import gov.noaa.nws.ncep.edex.common.stationTables.StationTable;
import gov.noaa.nws.ncep.viz.common.ui.NmapCommon;
import gov.noaa.nws.ncep.viz.localization.NcPathManager;
import gov.noaa.nws.ncep.viz.localization.NcPathManager.NcPathConstants;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResource;
import gov.noaa.nws.ncep.viz.rsc.aww.query.AwwQueryResult;
import gov.noaa.nws.ncep.viz.ui.display.NCMapDescriptor;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;

/**
 * SVRL resource - Display severe local storm watch data from aww data.
 *
 * This code has been developed by the SIB for use in the AWIPS2 system.
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer       Description
 * ------------- -------- -------------- ---------------------------------------
 * May 27, 2010           Uma Josyula    Initial creation.
 * Jan 10, 2011           Uma Josyula    Made changes to preprocess update and
 *                                       event date
 * Feb 16, 2012  555      S. Gurung      Added call to setAllFramesAsPopulated()
 *                                       in queryRecords().
 * Sep 11, 2012  852      Q. Zhou        Modified time string and alignment in
 *                                       drawTimeLabelWatchNumber().
 * Feb 25, 2013  972      Greg Hull      define on NCMapDescriptor instead of
 *                                       IMapDescriptor
 * Aug 14, 2013  1028     G. Hull        Move to aww project. Use AwwReportType
 *                                       enum.
 * Dec 14        ?        B. Yin         Remove ScriptCreator, use Thrift
 *                                       Client.
 * Mar 15, 2016  15560    K. Bugenhagen  Refactored common code into
 *                                       AwwQueryResult class, removing need for
 *                                       SvrlCountyQueryResult class.  Also
 *                                       cleanup.
 * Jul 15, 2020  8191     randerso       Updated for changes to LatLonPoint
 *
 * </pre>
 *
 * @author ujosyula
 */
public class SvrlResource
        extends AbstractNatlCntrsResource<SvrlResourceData, NCMapDescriptor>
        implements INatlCntrsResource, IStationField {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(SvrlResource.class);

    private static final String QUERY_PREFIX = "select ST_AsBinary(the_geom) G, ST_AsBinary(the_geom_0_001) G1, state,countyname,fips from mapdata.countylowres where ";

    private static final String QUERY_PREFIX2 = "select ST_AsBinary(the_geom), ST_AsBinary(the_geom_0_001) from mapdata.countylowres where countyname ='";

    private static final String QUERY_COLUMN_NAME = "fips";

    private IFont font;

    private StationTable stationTable;

    private SvrlResourceData svrlRscData;

    private List<SvrlData> modifyList;

    // for handling query part
    private AwwQueryResult queryResult = new AwwQueryResult();

    // for pre-calculate the IWiredframeShape
    private CountyResultJob crjob = new CountyResultJob("");

    // for storing result of pre-calculation
    private IWireframeShape outlineShape;

    // Area change flag
    private boolean areaChangeFlag = false;

    public class SvrlData implements IRscDataObject {
        /** used as a key string */
        private String datauri;

        /** issue time from bulletin */
        private DataTime issueTime;

        /** Event start time of Vtec */
        private DataTime evStartTime;

        /** Event end time of of Vtec */
        private DataTime evEndTime;

        private DataTime eventTime;

        private AwwReportType reportType;

        private int countyNumPoints;

        private float[] countyLat;

        private float[] countyLon;

        private List<LatLonPoint> countyPoints;

        // To get all the counties for the warning
        private List<String> countyUgc;

        private List<String> countyNames;

        private List<String> stateNames;

        public List<String> countyFips = new ArrayList<>();

        private String eventType;

        /** watch number to be displayed */
        private String watchNumber;

        private String evTrack;

        private String evOfficeId;

        private String evPhenomena;

        private String evProductClass;

        private String evSignificance;

        @Override
        public DataTime getDataTime() {
            return eventTime;

        }
    }

    protected class FrameData extends AbstractFrameData {
        private Map<String, SvrlData> svrlDataMap;

        public FrameData(DataTime frameTime, int timeInt) {
            super(frameTime, timeInt);
            svrlDataMap = new HashMap<>();
        }

        // turn the db record into an WarnRscDataObj which will be timeMatched
        // and added to one or more of the FrameData's.
        //
        @Override
        public boolean updateFrameData(IRscDataObject rscDataObj) {

            if (!(rscDataObj instanceof SvrlData)) {
                statusHandler.error(
                        "SVRL Resource.updateFrameData: expecting objects "
                                + " of type SvrlRscDataObj");
                return false;
            }
            SvrlData svrlRscDataObj = (SvrlData) rscDataObj;
            SvrlData existingSvrlData = svrlDataMap.get(svrlRscDataObj.datauri);
            if (existingSvrlData == null || svrlRscDataObj.issueTime
                    .greaterThan(existingSvrlData.issueTime)) {
                svrlDataMap.put(svrlRscDataObj.datauri, svrlRscDataObj);

            }

            return true;
        }
    }

    @Override
    public IRscDataObject[] processRecord(Object pdo) {
        AwwRecord awwRecord = (AwwRecord) pdo;
        List<SvrlData> svrlDataList = getAwwtData(awwRecord);
        if (svrlDataList == null) {
            return new IRscDataObject[] {};
        } else {
            return svrlDataList.toArray(new SvrlData[0]);
        }
    }

    private List<SvrlData> getAwwtData(AwwRecord awwRecord) {
        SvrlData svrlStatusData = null;
        List<SvrlData> svrlDataList = new ArrayList<>();
        try {

            Set<AwwUgc> awwUgc = awwRecord.getAwwUGC();
            for (AwwUgc awwugcs : awwUgc) {
                svrlStatusData = new SvrlData();
                svrlStatusData.issueTime = new DataTime(
                        awwRecord.getIssueTime());
                svrlStatusData.datauri = awwRecord.getDataURI();
                svrlStatusData.watchNumber = awwRecord.getWatchNumber();
                svrlStatusData.reportType = AwwReportType
                        .getReportType(awwRecord.getReportType());

                // TODO : change this to use UGCHeaderUtil (fixes a bug)
                // (I noticed a zone ugc code for a Tor. watch outling update)
                //
                // get the ugc line to find the counties
                String ugcline = awwugcs.getUgc();
                if (ugcline != null && !ugcline.isEmpty()) {

                    svrlStatusData.countyUgc = new ArrayList<>();
                    int i = 0;
                    String temp;
                    String countyname = ugcline.substring(0, 3);
                    StringTokenizer strugcs = new StringTokenizer(ugcline);
                    while (strugcs.hasMoreTokens()) {
                        temp = strugcs.nextToken("-");
                        if (temp != null) {
                            if (temp.contains("\r\r\n")) {
                                String temp1 = temp.substring(3);
                                temp = temp1;
                            }
                            if (temp.contains(countyname)) {
                                (svrlStatusData.countyUgc).add(temp);
                            } else {
                                (svrlStatusData.countyUgc)
                                        .add(countyname.concat(temp));
                            }
                            i++;
                        }
                    }
                    if (i > 1) {
                        svrlStatusData.countyUgc.remove(i - 1);
                        svrlStatusData.countyUgc.remove(i - 2);
                    }

                    svrlStatusData = getCountyNameLatLon(svrlStatusData);
                }
                int vtechNumber = awwugcs.getAwwVtecLine().size();

                // To obtain the event start and end time
                if (vtechNumber > 0) {
                    for (AwwVtec awwVtech : awwugcs.getAwwVtecLine()) {

                        svrlStatusData.eventType = awwVtech.getAction();
                        svrlStatusData.evTrack = awwVtech
                                .getEventTrackingNumber();
                        svrlStatusData.evEndTime = new DataTime(
                                awwVtech.getEventEndTime());
                        svrlStatusData.evOfficeId = awwVtech.getOfficeID();
                        svrlStatusData.evPhenomena = awwVtech.getPhenomena();
                        svrlStatusData.evProductClass = awwVtech
                                .getProductClass();
                        svrlStatusData.evSignificance = awwVtech
                                .getSignificance();
                        if (Set.of("COR", "CAN", "EXP")
                                .contains(awwVtech.getAction().toUpperCase())) {
                            modifyList.add(svrlStatusData);
                        }
                        if (awwVtech.getEventStartTime() != null
                                && awwVtech.getEventEndTime() != null) {
                            svrlStatusData.eventTime = new DataTime(
                                    awwVtech.getEventStartTime(),
                                    new TimeRange(awwVtech.getEventStartTime(),
                                            awwVtech.getEventEndTime()));
                        } else if (awwVtech.getEventEndTime() != null) {
                            svrlStatusData.eventTime = new DataTime(
                                    svrlStatusData.issueTime
                                            .getRefTimeAsCalendar(),
                                    new TimeRange(
                                            svrlStatusData.issueTime
                                                    .getRefTimeAsCalendar(),
                                            awwVtech.getEventEndTime()));
                        } else if (awwVtech.getEventStartTime() != null) {
                            svrlStatusData.eventTime = new DataTime(
                                    awwVtech.getEventStartTime(),
                                    new TimeRange(awwVtech.getEventStartTime(),
                                            svrlStatusData.issueTime
                                                    .getRefTimeAsCalendar()));
                        } else {
                            svrlStatusData.eventTime = svrlStatusData.issueTime;
                        }

                    }
                }

                svrlDataList.add(svrlStatusData);

            }

        } catch (Exception e) {
            statusHandler.error("Error getting AWW data: " + e.getMessage(), e);
        }

        return svrlDataList;
    }

    private SvrlData getCountyNameLatLon(SvrlData sdata) {
        sdata.countyPoints = new ArrayList<>();
        sdata.countyNames = new ArrayList<>();
        sdata.stateNames = new ArrayList<>();
        sdata.countyLat = new float[sdata.countyUgc.size()];
        sdata.countyLon = new float[sdata.countyUgc.size()];

        try {
            int i = 0;
            for (String string : sdata.countyUgc) {
                Station station = stationTable.getStation(StationField.STID,
                        string);
                if (station != null) {
                    LatLonPoint point = new LatLonPoint(station.getLatitude(),
                            station.getLongitude());
                    sdata.countyPoints.add(point);
                    sdata.countyNames.add(station.getStnname());
                    sdata.stateNames.add(station.getState());
                    sdata.countyLat[i] = station.getLatitude();
                    sdata.countyLon[i] = station.getLongitude();
                    i++;
                    String s = station.getStnnum();
                    sdata.countyFips.add(s.length() == 4 ? "0" + s : s);
                }

            }
        } catch (Exception e) {
            statusHandler.error("Error getting lat/lon: " + e.getMessage(), e);
        }
        sdata.countyNumPoints = sdata.countyNames.size();
        return sdata;

    }

    public SvrlResource(SvrlResourceData rscData, LoadProperties loadProperties)
            throws VizException {
        super(rscData, loadProperties);
        svrlRscData = resourceData;
        modifyList = new ArrayList<>();
    }

    @Override
    protected AbstractFrameData createNewFrame(DataTime frameTime,
            int timeInt) {
        return new FrameData(frameTime, timeInt);
    }

    @Override
    protected boolean preProcessFrameUpdate() {

        modifyQueue();

        return true;
    }

    private void modifyQueue() {
        if (modifyList != null) {
            for (SvrlData modify : modifyList) {

                for (IRscDataObject rscDataObj : newRscDataObjsQueue) {
                    SvrlData candidate = (SvrlData) rscDataObj;

                    if (modify.evTrack.equalsIgnoreCase(candidate.evTrack)
                            && modify.evOfficeId
                                    .equalsIgnoreCase(candidate.evOfficeId)
                            && modify.evPhenomena
                                    .equalsIgnoreCase(candidate.evPhenomena)
                            && modify.evProductClass
                                    .equalsIgnoreCase(candidate.evProductClass)
                            && modify.evSignificance.equalsIgnoreCase(
                                    candidate.evSignificance)) {
                        if (Set.of("CAN", "COR", "EXP")
                                .contains(candidate.eventType.toUpperCase())) {
                        } else {
                            candidate.evEndTime = modify.issueTime;
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
    }

    @Override
    public void initResource(IGraphicsTarget grphTarget) throws VizException {
        font = grphTarget.initializeFont("Monospace", 14,
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
            SvrlResource.SvrlData sData = (SvrlResource.SvrlData) dataObject;
            queryResult.buildQueryPart(sData.countyFips, QUERY_COLUMN_NAME);
        }
        queryResult.populateMap(QUERY_PREFIX);
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

        RGB color = new RGB(155, 155, 155);
        LineStyle lineStyle = LineStyle.SOLID;
        int symbolWidth = 2;
        int symbolSize = 2;

        Collection<SvrlData> svrlDataValues = currFrameData.svrlDataMap
                .values();

        for (SvrlData svrlData : svrlDataValues) {
            Boolean draw = false, drawLabel = true;

            if (svrlRscData.getColorCodeEnable()) {
                int watchNumberchoice = Integer.parseInt(svrlData.watchNumber)
                        % 10;

                switch (watchNumberchoice) {
                case 0:
                    color = svrlRscData.Watchxxx0Color;
                    symbolWidth = svrlRscData.Watchxxx0SymbolWidth;
                    symbolSize = svrlRscData.Watchxxx0SymbolSize;
                    break;
                case 1:
                    color = svrlRscData.Watchxxx1Color;
                    symbolWidth = svrlRscData.Watchxxx1SymbolWidth;
                    symbolSize = svrlRscData.Watchxxx1SymbolSize;
                    break;
                case 2:
                    color = svrlRscData.Watchxxx2Color;
                    symbolWidth = svrlRscData.Watchxxx2SymbolWidth;
                    symbolSize = svrlRscData.Watchxxx2SymbolSize;
                    break;
                case 3:
                    color = svrlRscData.Watchxxx3Color;
                    symbolWidth = svrlRscData.Watchxxx3SymbolWidth;
                    symbolSize = svrlRscData.Watchxxx3SymbolSize;
                    break;
                case 4:
                    color = svrlRscData.Watchxxx4Color;
                    symbolWidth = svrlRscData.Watchxxx4SymbolWidth;
                    symbolSize = svrlRscData.Watchxxx4SymbolSize;
                    break;
                case 5:
                    color = svrlRscData.Watchxxx5Color;
                    symbolWidth = svrlRscData.Watchxxx5SymbolWidth;
                    symbolSize = svrlRscData.Watchxxx5SymbolSize;
                    break;
                case 6:
                    color = svrlRscData.Watchxxx6Color;
                    symbolWidth = svrlRscData.Watchxxx6SymbolWidth;
                    symbolSize = svrlRscData.Watchxxx6SymbolSize;
                    break;
                case 7:
                    color = svrlRscData.Watchxxx7Color;
                    symbolWidth = svrlRscData.Watchxxx7SymbolWidth;
                    symbolSize = svrlRscData.Watchxxx7SymbolSize;
                    break;
                case 8:
                    color = svrlRscData.Watchxxx8Color;
                    symbolWidth = svrlRscData.Watchxxx8SymbolWidth;
                    symbolSize = svrlRscData.Watchxxx8SymbolSize;
                    break;
                case 9:
                    color = svrlRscData.Watchxxx9Color;
                    symbolWidth = svrlRscData.Watchxxx9SymbolWidth;
                    symbolSize = svrlRscData.Watchxxx9SymbolSize;
                    break;
                }

            } else {
                if (svrlData.reportType == AwwReportType.SEVERE_THUNDERSTORM_WATCH) {
                    color = svrlRscData.thunderstormColor;
                    symbolWidth = svrlRscData.thunderstormSymbolWidth;
                    symbolSize = svrlRscData.thunderstormSymbolSize;

                } else if (svrlData.reportType == AwwReportType.TORNADO_WATCH_OUTLINE_UPDATE) {
                    color = svrlRscData.tornadoColor;
                    symbolWidth = svrlRscData.tornadoSymbolWidth;
                    symbolSize = svrlRscData.tornadoSymbolSize;

                }
            }

            if ((svrlData.reportType == AwwReportType.SEVERE_THUNDERSTORM_WATCH
                    && svrlRscData.thunderstormEnable)
                    || (svrlData.reportType == AwwReportType.TORNADO_WATCH_OUTLINE_UPDATE
                            && svrlRscData.tornadoEnable)) {
                draw = true;
            }

            // draw the polygon

            if (draw) {
                if (svrlRscData.getWatchBoxOutlineEnable()) {

                    drawCountyOutline2(svrlData, target, color, symbolWidth,
                            lineStyle, paintProps);// T456
                }

                if (svrlRscData.getWatchBoxTimeEnable()
                        || svrlRscData.getWatchBoxLabelEnable()) {
                    drawTimeLabelWatchNumber(svrlData, target, color);

                }
            }
        }
    }

    public void drawCountyOutline(SvrlData svrlData, IGraphicsTarget target,
            RGB color, int symbolWidth, LineStyle lineStyle,
            PaintProperties paintProps) throws VizException {
        String countyName, stateName;
        Envelope env = null;
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
        for (int i = 0; i < svrlData.countyNames.size(); i++) {
            countyName = svrlData.countyNames.get(i);
            stateName = svrlData.stateNames.get(i);
            if (countyName.contains("_")) {
                countyName = countyName.replace("_", " ");
            }
            StringBuilder query = new StringBuilder(QUERY_PREFIX2);
            query.append(countyName);
            query.append("' AND  state ='");
            query.append(stateName);
            query.append("' AND ");
            query.append(geoConstraint);
            query.append(";");
            List<Object[]> results = DirectDbQuery
                    .executeQuery(query.toString(), "maps", QueryLanguage.SQL);
            IWireframeShape newOutlineShape = target.createWireframeShape(false,
                    descriptor, 0.0f);
            IShadedShape newShadedShape = target.createShadedShape(false,
                    descriptor, true);
            JTSCompiler jtsCompiler = new JTSCompiler(newShadedShape,
                    newOutlineShape, descriptor, PointStyle.CROSS);

            WKBReader wkbReader = new WKBReader();
            for (Object[] result : results) {
                int k = 0;
                byte[] wkb = (byte[]) result[k + 1];
                Geometry g;
                try {
                    g = wkbReader.read(wkb);
                    if (!(g instanceof Point)) {
                        jtsCompiler.handle(g, color);
                    }

                } catch (VizException e) {
                    statusHandler.error(
                            "Error reprojecting map outline: " + e.getMessage(),
                            e);
                } catch (ParseException e) {
                    statusHandler.error(
                            "Error parsing query result: " + e.getMessage(), e);
                }
            }
            newOutlineShape.compile();

            if (newOutlineShape != null && newOutlineShape.isDrawable()) {
                target.drawWireframeShape(newOutlineShape, color, symbolWidth,
                        lineStyle);

            }
        }

    }

    public void drawTimeLabelWatchNumber(SvrlData svrlData,
            IGraphicsTarget target, RGB color) {
        try {
            for (int i = 0; i < svrlData.countyNumPoints; i++) {
                double[] labelLatLon = { svrlData.countyLon[i],
                        svrlData.countyLat[i] };
                double[] labelPix = descriptor.worldToPixel(labelLatLon);

                if (labelPix != null) {

                    String[] text = new String[2];
                    List<String> enabledText = new ArrayList<>();

                    if (svrlRscData.getWatchBoxLabelEnable()) {
                        enabledText.add(svrlData.countyNames.get(i));
                    }

                    if (svrlRscData.getWatchBoxTimeEnable()) {
                        DataTime startTime = new DataTime(
                                svrlData.eventTime.getValidPeriod().getStart());
                        DataTime endTime = new DataTime(
                                svrlData.eventTime.getValidPeriod().getEnd());
                        String temp = startTime.toString().substring(11, 13)
                                + startTime.toString().substring(14, 16) + "-"
                                + endTime.toString().substring(11, 13)
                                + endTime.toString().substring(14, 16);
                        enabledText.add(temp);
                    }

                    for (int j = enabledText.size(); j < 2; j++) {
                        enabledText.add("");
                    }

                    text = enabledText.toArray(text);

                    target.drawStrings(font, text, labelPix[0], labelPix[1],
                            0.0, TextStyle.NORMAL,
                            new RGB[] { color, color, color },
                            HorizontalAlignment.LEFT, VerticalAlignment.TOP);
                }
            }
        } catch (Exception e) {
            statusHandler.error(
                    "Error drawing time label watch number: " + e.getMessage(),
                    e);
        }
    }

    /**
     * handles the IWireframeShape pre-calculation
     */
    private class CountyResultJob extends org.eclipse.core.runtime.jobs.Job {

        private java.util.Map<String, Result> keyResultMap = new java.util.concurrent.ConcurrentHashMap<>();

        private IGraphicsTarget target;

        private IMapDescriptor descriptor;

        private RGB symbolColor = new RGB(155, 155, 155);

        public CountyResultJob(String name) {
            super(name);
        }

        public class Result {

            public IWireframeShape outlineShape;

            private Result(IWireframeShape outlineShape,
                    IWireframeShape nuShape, IShadedShape shadedShape,
                    java.util.Map<Object, RGB> colorMap) {

                this.outlineShape = outlineShape;

            }
        }

        public void setRequest(IGraphicsTarget target,
                IMapDescriptor descriptor, String query, boolean labeled,
                boolean shaded, java.util.Map<Object, RGB> colorMap) {

            this.target = target;
            this.descriptor = descriptor;
            this.run(null);

        }

        @Override
        protected org.eclipse.core.runtime.IStatus run(
                org.eclipse.core.runtime.IProgressMonitor monitor) {

            for (AbstractFrameData afd : frameDataMap.values()) {
                FrameData fd = (FrameData) afd;
                for (SvrlData sd : fd.svrlDataMap.values()) {
                    Collection<Geometry> gw = new ArrayList<>();
                    for (String element : sd.countyFips) {
                        for (ArrayList<Object[]> counties : queryResult
                                .getCountyResult(element)) {
                            if (counties == null) {
                                continue;
                            }
                            WKBReader wkbReader = new WKBReader();
                            for (Object[] result : counties) {
                                int k = 0;
                                byte[] wkb1 = (byte[]) result[k];
                                MultiPolygon countyGeo = null;
                                try {
                                    countyGeo = (MultiPolygon) wkbReader
                                            .read(wkb1);
                                    if (countyGeo != null && countyGeo.isValid()
                                            && (!countyGeo.isEmpty())) {
                                        gw.add(countyGeo);
                                    }
                                } catch (Exception e) {
                                    statusHandler.error(
                                            "Exception in run, CountyResultJob: "
                                                    + e.getMessage(),
                                            e);
                                }
                            }
                        }
                    }
                    if (!gw.isEmpty()) {
                        keyResultMap.put(sd.datauri, new Result(
                                getEachWrdoShape(gw), null, null, null));
                    }
                }

            }

            return org.eclipse.core.runtime.Status.OK_STATUS;
        }

        public IWireframeShape getEachWrdoShape(Collection<Geometry> gw) {

            IWireframeShape newOutlineShape = target.createWireframeShape(false,
                    descriptor, 0.0f);

            JTSCompiler jtsCompiler = new JTSCompiler(null, newOutlineShape,
                    descriptor, PointStyle.CROSS);

            GeometryCollection gColl = (GeometryCollection) new GeometryFactory()
                    .buildGeometry(gw);

            try {
                gColl.normalize();

                jtsCompiler.handle(gColl, symbolColor);

                newOutlineShape.compile();

            } catch (Exception e) {
                statusHandler.error(
                        "Exception in getEachWrdoShape(), CountyResultJob: "
                                + e.getMessage(),
                        e);
            }

            return newOutlineShape;
        }
    }

    private void drawCountyOutline2(SvrlData wData, IGraphicsTarget target,
            RGB color, int outlineWidth, LineStyle lineStyle,
            PaintProperties paintProps) {

        String key = wData.datauri;
        CountyResultJob.Result result = crjob.keyResultMap.get(key);

        if (result != null) {
            if (outlineShape != null) {
                outlineShape.dispose();
            }
            outlineShape = result.outlineShape;
        } else {
            return;
        }

        if (outlineShape != null && outlineShape.isDrawable()) {
            try {
                target.drawWireframeShape(outlineShape, color, outlineWidth,
                        lineStyle);
            } catch (VizException e) {
                statusHandler
                        .error("Exception drawing county outline wireframe: "
                                + e.getMessage(), e);
            }

        }
    }

    @Override
    protected boolean postProcessFrameUpdate() {

        AbstractEditor ed = NcDisplayMngr.getActiveNatlCntrsEditor();

        crjob.setRequest(ed.getActiveDisplayPane().getTarget(),
                getNcMapDescriptor(), null, false, false, null);

        return true;
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
            statusHandler.error(
                    "===== find IRscDataObject rscDataObj.getDataTime() return NULL!!!");
        }
        long dataTimeInMs = 0;
        if (validTimeInCalendar != null) {
            dataTimeInMs = validTimeInCalendar.getTimeInMillis();
        }
        return dataTimeInMs;
    }

    @Override
    public String getName() {
        String legendString = super.getName();
        FrameData fd = (FrameData) getCurrentFrame();
        if (fd == null || fd.getFrameTime() == null
                || fd.svrlDataMap.size() == 0) {
            return legendString + "-No Data";
        }
        return legendString + " "
                + NmapCommon.getTimeStringFromDataTime(fd.getFrameTime(), "/");
    }
}
