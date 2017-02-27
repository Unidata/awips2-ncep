package gov.noaa.nws.ncep.viz.rsc.aww.svrl;

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
 * Svrl resourceResource - Display SVRL from aww data.
 * 
 *  This code has been developed by the SIB for use in the AWIPS2 system.
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 27 May 2010           Uma Josyula  Initial creation.
 * 01/10/11				Uma Josyula	 Made changes to preprocess update and event date  
 * 02/16/2012   555        S. Gurung   Added call to setAllFramesAsPopulated() in queryRecords().
 * 12/14         ?      B. Yin       Remove ScriptCreator, use Thrift Client.
 * 11/05/2015   5070      randerso    Adjust font sizes for dpi scaling
 * </pre>
 * 
 * @author ujosyula 
 * @version 1.0
 */
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.graphics.RGB;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.raytheon.uf.common.geospatial.MapUtil;
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
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;

/**
 * SVRL resource - Display severe local storm watch data from aww data.
 * 
 * This code has been developed by the SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 
 * 27 May 2010          Uma Josyula    Initial creation.
 * 01/10/11             Uma Josyula    Made changes to preprocess update and event date  
 * 02/16/2012   555     S. Gurung      Added call to setAllFramesAsPopulated() in queryRecords().
 * 09/11/12     852     Q. Zhou        Modified time string and alignment in drawTimeLabelWatchNumber().
 * 02/25/13     972     Greg Hull      define on NCMapDescriptor instead of IMapDescriptor
 * 08/14/13     1028    G. Hull        Move to aww project. Use AwwReportType enum.
 * 12/14         ?      B. Yin         Remove ScriptCreator, use Thrift Client.
 * 03/15/2016   R15560  K. Bugenhagen  Refactored common code into AwwQueryResult 
 *                                     class, removing need for SvrlCountyQueryResult 
 *                                     class.  Also cleanup.
 * </pre>
 * 
 * @author ujosyula
 * @version 1.0
 */
public class SvrlResource extends
        AbstractNatlCntrsResource<SvrlResourceData, NCMapDescriptor> implements
        INatlCntrsResource, IStationField {

    private final static Logger logger = Logger.getLogger(SvrlResource.class
            .getCanonicalName());

    private final static String QUERY_PREFIX = "select AsBinary(the_geom) G, AsBinary(the_geom_0_001) G1, state,countyname,fips from mapdata.countylowres where ";

    private final static String QUERY_PREFIX2 = "select AsBinary(the_geom), AsBinary(the_geom_0_001) from mapdata.countylowres where countyname ='";

    private final static String QUERY_COLUMN_NAME = "fips";

    private IFont font;

    private StationTable stationTable;

    private SvrlResourceData svrlRscData;

    private List<SvrlData> modifyList;

    // for handling query part
    AwwQueryResult queryResult = new AwwQueryResult();

    // for pre-calculate the IWiredframeShape
    private CountyResultJob crjob = new CountyResultJob("");

    // for storing result of pre-calculation
    private IWireframeShape outlineShape;

    // Area change flag
    private boolean areaChangeFlag = false;

    public class SvrlData implements IRscDataObject {
        String datauri; // used as a key string

        DataTime issueTime; // issue time from bulletin

        DataTime evStartTime; // Event start time of Vtec

        DataTime evEndTime; // Event end time of of Vtec

        DataTime eventTime;

        AwwReportType reportType;

        int countyNumPoints;

        float[] countyLat;

        float[] countyLon;

        List<LatLonPoint> countyPoints;

        // To get all the counties for the warning
        List<String> countyUgc, countyNames, stateNames;

        public List<String> countyFips = new ArrayList<String>();

        String eventType;

        String watchNumber; // watch number to be displayed

        String evTrack;

        String evOfficeId;

        String evPhenomena;

        String evProductClass;

        String evSignificance;

        @Override
        public DataTime getDataTime() {
            return eventTime;

        }
    }

    protected class FrameData extends AbstractFrameData {
        HashMap<String, SvrlData> svrlDataMap;

        public FrameData(DataTime frameTime, int timeInt) {
            super(frameTime, timeInt);
            svrlDataMap = new HashMap<String, SvrlData>();
        }

        // turn the db record into an WarnRscDataObj which will be timeMatched
        // and added to one or more of the FrameData's.
        //
        @Override
        public boolean updateFrameData(IRscDataObject rscDataObj) {

            if (!(rscDataObj instanceof SvrlData)) {
                logger.log(Level.SEVERE,
                        "SVRL Resource.updateFrameData: expecting objects "
                                + " of type SvrlRscDataObj");
                return false;
            }
            SvrlData svrlRscDataObj = (SvrlData) rscDataObj;
            SvrlData existingSvrlData = svrlDataMap.get(svrlRscDataObj.datauri);
            if (existingSvrlData == null
                    || svrlRscDataObj.issueTime
                            .greaterThan(existingSvrlData.issueTime)) {
                svrlDataMap.put(svrlRscDataObj.datauri, svrlRscDataObj);

            }

            return true;
        }
    }

    @Override
    public IRscDataObject[] processRecord(Object pdo) {
        AwwRecord awwRecord = (AwwRecord) pdo;
        ArrayList<SvrlData> svrlDataList = getAwwtData(awwRecord);
        if (svrlDataList == null) {
            return new IRscDataObject[] {};
        } else {
            return svrlDataList.toArray(new SvrlData[0]);
        }
    }

    private ArrayList<SvrlData> getAwwtData(AwwRecord awwRecord) {
        SvrlData svrlStatusData = null;
        List<SvrlData> svrlDataList = new ArrayList<SvrlData>();
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
                if (ugcline != null && ugcline != "") {

                    svrlStatusData.countyUgc = new ArrayList<String>();
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
                                (svrlStatusData.countyUgc).add(countyname
                                        .concat(temp));
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
                        if ((awwVtech.getAction().equalsIgnoreCase("COR"))
                                || (awwVtech.getAction()
                                        .equalsIgnoreCase("CAN"))
                                || (awwVtech.getAction()
                                        .equalsIgnoreCase("EXP"))) {
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
                                    new TimeRange(svrlStatusData.issueTime
                                            .getRefTimeAsCalendar(), awwVtech
                                            .getEventEndTime()));
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
            logger.log(Level.SEVERE,
                    "Error getting AWW data: " + e.getMessage());
        }

        return (ArrayList<SvrlData>) svrlDataList;
    }

    private SvrlData getCountyNameLatLon(SvrlData sdata) {
        sdata.countyPoints = new ArrayList<LatLonPoint>();
        sdata.countyNames = new ArrayList<String>();
        sdata.stateNames = new ArrayList<String>();
        sdata.countyLat = new float[sdata.countyUgc.size()];
        sdata.countyLon = new float[sdata.countyUgc.size()];

        try {
            int i = 0;
            for (Iterator<String> iterator = sdata.countyUgc.iterator(); iterator
                    .hasNext();) {
                Station station = stationTable.getStation(StationField.STID,
                        iterator.next());
                if (station != null) {
                    LatLonPoint point = new LatLonPoint(station.getLatitude(),
                            station.getLongitude(), LatLonPoint.INDEGREES);
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
            logger.log(Level.SEVERE, "Error getting lat/lon: " + e.getMessage());
        }
        sdata.countyNumPoints = sdata.countyNames.size();
        return sdata;

    }

    public SvrlResource(SvrlResourceData rscData, LoadProperties loadProperties)
            throws VizException {
        super(rscData, loadProperties);
        svrlRscData = (SvrlResourceData) resourceData;
        modifyList = new ArrayList<SvrlData>();
    }

    protected AbstractFrameData createNewFrame(DataTime frameTime, int timeInt) {
        return (AbstractFrameData) new FrameData(frameTime, timeInt);
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
                            && modify.evSignificance
                                    .equalsIgnoreCase(candidate.evSignificance)) {
                        if (candidate.eventType.equalsIgnoreCase("CAN")
                                || candidate.eventType.equalsIgnoreCase("COR")
                                || candidate.eventType.equalsIgnoreCase("EXP")) {
                        } else {
                            candidate.evEndTime = modify.issueTime;
                            candidate.eventTime = new DataTime(
                                    candidate.eventTime.getRefTimeAsCalendar(),
                                    new TimeRange(candidate.eventTime
                                            .getRefTimeAsCalendar(),
                                            candidate.evEndTime
                                                    .getRefTimeAsCalendar()));
                        }
                    }

                }
            }
        }
    }

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
            IRscDataObject dataObject = (IRscDataObject) iter.next();
            SvrlResource.SvrlData sData = (SvrlResource.SvrlData) dataObject;
            queryResult.buildQueryPart(sData.countyFips, QUERY_COLUMN_NAME);
        }
        queryResult.populateMap(QUERY_PREFIX);
        setAllFramesAsPopulated();
    }

    @Override
    public void disposeInternal() {

    }

    public void paintFrame(AbstractFrameData frameData, IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {

        if (paintProps == null) {
            return;
        }
        if (areaChangeFlag) {
            areaChangeFlag = false;
            postProcessFrameUpdate();
        }// TODO: dispose old outlineShape
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
                int watchNumberchoice = Integer.parseInt(svrlData.watchNumber) % 10;

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

            if ((svrlData.reportType == AwwReportType.SEVERE_THUNDERSTORM_WATCH && svrlRscData.thunderstormEnable)
                    || (svrlData.reportType == AwwReportType.TORNADO_WATCH_OUTLINE_UPDATE && svrlRscData.tornadoEnable)) {
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

        }// for loop
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
            List<Object[]> results = DirectDbQuery.executeQuery(
                    query.toString(), "maps", QueryLanguage.SQL);
            IWireframeShape newOutlineShape = target.createWireframeShape(
                    false, descriptor, 0.0f);
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
                    logger.log(Level.SEVERE, "Error reprojecting map outline: "
                            + e.getMessage());
                } catch (ParseException e) {
                    logger.log(Level.SEVERE,
                            "Error parsing query result: " + e.getMessage());
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
                    List<String> enabledText = new ArrayList<String>();

                    if (svrlRscData.getWatchBoxLabelEnable()) {
                        enabledText.add(svrlData.countyNames.get(i));
                    }

                    if (svrlRscData.getWatchBoxTimeEnable()) {
                        DataTime startTime = new DataTime(svrlData.eventTime
                                .getValidPeriod().getStart());
                        DataTime endTime = new DataTime(svrlData.eventTime
                                .getValidPeriod().getEnd());
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
                            0.0, TextStyle.NORMAL, new RGB[] { color, color,
                                    color }, HorizontalAlignment.LEFT,
                            VerticalAlignment.TOP);
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error drawing time label watch number: "
                    + e.getMessage());
        }
    }

    /**
     * handles the IWireframeShape pre-calculation
     */
    private class CountyResultJob extends org.eclipse.core.runtime.jobs.Job {

        private java.util.Map<String, Result> keyResultMap = new java.util.concurrent.ConcurrentHashMap<String, Result>();

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
                    Collection<Geometry> gw = new ArrayList<Geometry>();
                    for (int i = 0; i < sd.countyFips.size(); i++) {
                        for (ArrayList<Object[]> counties : queryResult
                                .getCountyResult(sd.countyFips.get(i))) {
                            if (counties == null)
                                continue;
                            WKBReader wkbReader = new WKBReader();
                            for (Object[] result : counties) {
                                int k = 0;
                                byte[] wkb1 = (byte[]) result[k];
                                com.vividsolutions.jts.geom.MultiPolygon countyGeo = null;
                                try {
                                    countyGeo = (com.vividsolutions.jts.geom.MultiPolygon) wkbReader
                                            .read(wkb1);
                                    if (countyGeo != null
                                            && countyGeo.isValid()
                                            && (!countyGeo.isEmpty())) {
                                        gw.add(countyGeo);
                                    }
                                } catch (Exception e) {
                                    logger.log(Level.SEVERE,
                                            "Exception in run, CountyResultJob: "
                                                    + e.getMessage());
                                }
                            }
                        }
                    }
                    if (gw.size() == 0) {
                        continue;
                    } else {
                        keyResultMap.put(sd.datauri, new Result(
                                getEachWrdoShape(gw), null, null, null));
                    }

                }

            }

            return org.eclipse.core.runtime.Status.OK_STATUS;
        }

        public IWireframeShape getEachWrdoShape(Collection<Geometry> gw) {

            IWireframeShape newOutlineShape = target.createWireframeShape(
                    false, descriptor, 0.0f);

            JTSCompiler jtsCompiler = new JTSCompiler(null, newOutlineShape,
                    descriptor, PointStyle.CROSS);

            com.vividsolutions.jts.geom.GeometryCollection gColl = (com.vividsolutions.jts.geom.GeometryCollection) new com.vividsolutions.jts.geom.GeometryFactory()
                    .buildGeometry(gw);

            try {
                gColl.normalize();

                jtsCompiler.handle(gColl, symbolColor);

                newOutlineShape.compile();

            } catch (Exception e) {
                logger.log(Level.SEVERE,
                        "Exception in getEachWrdoShape(), CountyResultJob: "
                                + e.getMessage());
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
            outlineShape = result.outlineShape;
        } else {
            return;
        }

        if (outlineShape != null && outlineShape.isDrawable()) {
            try {
                target.drawWireframeShape(outlineShape, color, outlineWidth,
                        lineStyle);
            } catch (VizException e) {
                logger.log(
                        Level.SEVERE,
                        "Exception drawing county outline wireframe: "
                                + e.getMessage());
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
        if (rscDataObj == null)
            return 0;

        java.util.Calendar validTimeInCalendar = null;
        DataTime dataTime = rscDataObj.getDataTime();
        if (dataTime != null) {
            validTimeInCalendar = dataTime.getValidTime();
        } else {
            logger.log(Level.SEVERE,
                    "===== find IRscDataObject rscDataObj.getDataTime() return NULL!!!");
        }
        long dataTimeInMs = 0;
        if (validTimeInCalendar != null)
            dataTimeInMs = validTimeInCalendar.getTimeInMillis();
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
