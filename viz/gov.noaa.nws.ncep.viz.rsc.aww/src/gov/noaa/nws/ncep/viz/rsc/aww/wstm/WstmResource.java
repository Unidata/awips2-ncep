package gov.noaa.nws.ncep.viz.rsc.aww.wstm;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
import com.raytheon.uf.viz.core.geom.PixelCoordinate;
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
import gov.noaa.nws.ncep.ui.pgen.display.DisplayElementFactory;
import gov.noaa.nws.ncep.ui.pgen.display.IDisplayable;
import gov.noaa.nws.ncep.ui.pgen.elements.Symbol;
import gov.noaa.nws.ncep.viz.common.ui.NmapCommon;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResource;
import gov.noaa.nws.ncep.viz.rsc.aww.query.WstmQueryResult;
import gov.noaa.nws.ncep.viz.rsc.aww.utils.CountyObjectCreator;
import gov.noaa.nws.ncep.viz.rsc.aww.utils.PreProcessDisplay;
import gov.noaa.nws.ncep.viz.ui.display.NCMapDescriptor;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;

/**
 * WstmResource - Displays Winter Storm Misc Resource
 *
 *
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer        Description
 * ------------- -------- --------------- --------------------------------------
 * Nov 05, 2010  247      Archana         Initial creation.
 * Feb 16, 2012  555      S. Gurung       Added call to
 *                                        setAllFramesAsPopulated() in
 *                                        queryRecords().
 * May 23, 2012  785      Q. Zhou         Added getName for legend.
 * Aug 17, 2012  655      B. Hebbard      Added paintProps as parameter to
 *                                        IDisplayable draw
 * Jan 31, 2013  976      Archana         Updated paintFrame() to not render any
 *                                        null strings Replaced the depreciated
 *                                        target.drawString() method with
 *                                        target.drawStrings().
 * Nov 17, 2014  5125     J. Huber        Removed dead and deprecated code.
 *                                        Implemented use of new common object
 *                                        for AWW and removed local
 *                                        WstmRscDataObject. Implemented common
 *                                        method to create display objects for
 *                                        individual counties. Broke out label
 *                                        display from the return of the frame
 *                                        data because of issue found with
 *                                        multiple products being valid at the
 *                                        same time. Added DisplayLabel class to
 *                                        be able to create a list of display
 *                                        labels and draw all labels at one time
 *                                        instead of county by county.
 * Nov 05, 2015  5070     randerso        Adjust font sizes for dpi scaling
 * Mar 15, 2016  15560    K.  Bugenhagen  Cleanup and local refactoring.
 * Jul 15, 2020  8191     randerso        Code cleanup
 *
 * </pre>
 *
 * @author archana
 */
public class WstmResource
        extends AbstractNatlCntrsResource<WstmResourceData, NCMapDescriptor>
        implements INatlCntrsResource {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(WstmResource.class);

    private IFont font = null;

    private WstmResourceData wstmResourceDataObj;

    /**
     * Constructor Invokes the base class constructor to process the incoming
     * records. Also associates the WSTM resource with its corresponding
     * resource data object
     *
     * @param resourceData
     *            - The editable attributes of the WSTM resource.
     * @param props
     *            - the options for loading the data for this resource
     */
    protected WstmResource(WstmResourceData resourceData,
            LoadProperties props) {
        super(resourceData, props);
        wstmResourceDataObj = resourceData;
    }

    @Override
    /**
     * Creates a new frame with the specified reference time and frame interval
     *
     * @param frameTime
     *            - The reference time for the frame to be created
     * @param frameInterval
     *            - The interval between the created frames
     *
     * @return Returns the new frame with the specified frame reference time and
     *         time interval
     */
    protected AbstractFrameData createNewFrame(DataTime frameTime,
            int frameInterval) {
        return new FrameData(frameTime, frameInterval);
    }

    @Override
    /**
     * Overridden method. Invokes queryRecords() to fetch the records from the
     * database per the metadata map in WSTM.xml
     */
    public void initResource(IGraphicsTarget grphTarget) throws VizException {
        queryRecords();
    }

    @Override
    /***
     * Renders the WSTM resource on the frame
     */
    public void paintFrame(AbstractFrameData frameData, IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {

        if (font == null) {
            font = target.initializeFont("Monospace", 12,
                    new IFont.Style[] { IFont.Style.BOLD });
        }

        if (areaChangeFlag) {
            areaChangeFlag = false;
            postProcessFrameUpdate();
        }
        if (frameData != null) {
            FrameData currFrameData = (FrameData) frameData;

            List<PreProcessDisplay> sortedWstmRecords = new ArrayList<>();

            // Sort records by issue time and then send them to be processed and
            // return with one county in each object.
            for (Map.Entry<String, PreProcessDisplay> entry : currFrameData.wstmDataMap
                    .entrySet()) {
                sortedWstmRecords.add(entry.getValue());
            }
            Collections.sort(sortedWstmRecords);
            HashMap<String, DisplayLabel> displayLabelMap;
            displayLabelMap = new HashMap<>();
            List<PreProcessDisplay> displayObjs = CountyObjectCreator
                    .PreProcessDisplay(sortedWstmRecords);

            for (PreProcessDisplay eachPreProcessDisplayObj : displayObjs) {
                Boolean draw = false;
                WstmResourceAttributes wstmRscAttr = null;
                String symbolTypeStr = "";
                eachPreProcessDisplayObj.zoneName = wqr
                        .getZoneName(eachPreProcessDisplayObj.singleFipsCode);

                /*
                 * Retrieve the user-configurable attributes depending on
                 * whether the WstmRscDataObject denotes an advisory, watch or a
                 * warning
                 */

                // Removed ENUMs and just used raw record
                switch (eachPreProcessDisplayObj.evSignificance.toUpperCase()) {
                case "Y":
                    wstmRscAttr = new WstmResourceAttributes(
                            wstmResourceDataObj.getWstmAdvisoryEnable(),
                            wstmResourceDataObj.getWstmAdvisoryColor(),
                            wstmResourceDataObj.getWstmAdvisoryLineWidth(),
                            wstmResourceDataObj.getWstmAdvisorySymbolWidth(),
                            wstmResourceDataObj.getWstmAdvisorySymbolSize());
                    symbolTypeStr = new String(
                            EditWstmAttrDialog.advisoryMarkerData);
                    break;
                case "W":

                    wstmRscAttr = new WstmResourceAttributes(
                            wstmResourceDataObj.getWstmWarningEnable(),
                            wstmResourceDataObj.getWstmWarningColor(),
                            wstmResourceDataObj.getWstmWarningLineWidth(),
                            wstmResourceDataObj.getWstmWarningSymbolWidth(),
                            wstmResourceDataObj.getWstmWarningSymbolSize());
                    symbolTypeStr = new String(
                            EditWstmAttrDialog.warningMarkerData);
                    break;
                case "A":
                    wstmRscAttr = new WstmResourceAttributes(
                            wstmResourceDataObj.getWstmWatchEnable(),
                            wstmResourceDataObj.getWstmWatchColor(),
                            wstmResourceDataObj.getWstmWatchLineWidth(),
                            wstmResourceDataObj.getWstmWatchSymbolWidth(),
                            wstmResourceDataObj.getWstmWatchSymbolSize());
                    symbolTypeStr = new String(
                            EditWstmAttrDialog.watchMarkerData);
                }

                if (wstmRscAttr != null && wstmRscAttr.getEventEnable()) {
                    RGB colorOfEventRGB = wstmRscAttr.getColorOfEvent();

                    if (getCurrentFrameTime().getValidTimeAsDate()
                            .getTime() < eachPreProcessDisplayObj.displayEnd
                                    .getValidPeriod().getEnd().getTime()
                            || getCurrentFrameTime().getValidTimeAsDate()
                                    .getTime() >= eachPreProcessDisplayObj.displayStart
                                            .getValidPeriod().getStart()
                                            .getTime()) {
                        draw = true;
                    }

                    if (getCurrentFrameTime().getValidTimeAsDate()
                            .getTime() > eachPreProcessDisplayObj.displayEnd
                                    .getValidPeriod().getEnd().getTime()) {
                        draw = false;
                    }

                    if (getCurrentFrameTime().getValidTimeAsDate()
                            .getTime() < eachPreProcessDisplayObj.displayStart
                                    .getValidPeriod().getStart().getTime()) {
                        draw = false;
                    }

                    if (getCurrentFrameTime().getValidTimeAsDate()
                            .getTime() == (eachPreProcessDisplayObj.displayEnd
                                    .getValidPeriod().getEnd().getTime())) {
                        // do not draw endtime frame, that's what nmap2 does
                        draw = false;
                    }
                    if (draw) {
                        List<String> enabledText = new ArrayList<>();
                        String timeString = null;
                        DisplayLabel countyZoneLabel = new DisplayLabel();

                        /*
                         * If the flag is enabled - Plot the name of the current
                         * FIPS zone in which this weather hazard is valid
                         */
                        int size = 0;
                        if (wstmResourceDataObj.getZoneNameEnable()) {
                            String zoneName = eachPreProcessDisplayObj.zoneName;
                            countyZoneLabel.isZoneNameEnabled = true;
                            if (zoneName != null) {
                                enabledText.add(zoneName);
                            }
                        }

                        /*
                         * If the flag is enabled - Plot the duration for which
                         * the weather hazard (WstmRescDataObject) is valid
                         */
                        if (wstmResourceDataObj.getTimeEnable()) {
                            if (eachPreProcessDisplayObj.displayStart != null
                                    && eachPreProcessDisplayObj.displayEnd != null) {
                                countyZoneLabel.isTimeEnabled = true;
                                DataTime startTime = new DataTime(
                                        eachPreProcessDisplayObj.origStartTime
                                                .getValidPeriod().getStart());
                                DataTime endTime = new DataTime(
                                        eachPreProcessDisplayObj.origEndTime
                                                .getValidPeriod().getEnd());
                                timeString = startTime.toString().substring(8,
                                        10) + "/"
                                        + startTime.toString().substring(11, 13)
                                        + startTime.toString().substring(14, 16)
                                        + "-"
                                        + endTime.toString().substring(8, 10)
                                        + "/"
                                        + endTime.toString().substring(11, 13)
                                        + endTime.toString().substring(14, 16);
                                enabledText.add(timeString);
                            }
                        }
                        countyZoneLabel.eventColor = wstmRscAttr
                                .getColorOfEvent();
                        countyZoneLabel.symbolWidth = wstmRscAttr
                                .getSymbolWidth();
                        countyZoneLabel.symbolSize = wstmRscAttr
                                .getSymbolSize();
                        countyZoneLabel.symbolTypeStr = symbolTypeStr;
                        countyZoneLabel.lineWidth = wstmRscAttr.getLineWidth();
                        LatLonPoint zoneLatLon = new LatLonPoint(
                                eachPreProcessDisplayObj.singleCountyZoneLat,
                                eachPreProcessDisplayObj.singleCountyZoneLon);
                        Coordinate thisMarkerCoord = this
                                .convertCentroidToWorldCoordinates(zoneLatLon);
                        countyZoneLabel.markerCoordinate = thisMarkerCoord;
                        // Base the label offset on the symbol size so
                        // it is never in the middle of the marker
                        float labelOffsetX = (float) .03
                                * countyZoneLabel.symbolSize;
                        float labelOffsetY = (float) .015
                                * countyZoneLabel.symbolSize;
                        PixelCoordinate pixCoord = null;
                        double worldC[] = new double[] {
                                thisMarkerCoord.x + labelOffsetX,
                                // offset label based on symbol size
                                thisMarkerCoord.y - labelOffsetY };
                        pixCoord = new PixelCoordinate(
                                descriptor.worldToPixel(worldC));

                        countyZoneLabel.displayCoords = pixCoord;

                        /*
                         * Since multiple products could be in affect at the
                         * same time we must build the display label arrays and
                         * color arrays for each county prior to trying to
                         * create the draw string. The display color for
                         * markers, outlines, and zone names are determined by
                         * which products are in effect. Priority for color is
                         * Warning, then Advisory, then Watch. Labels will be
                         * drawn in the order of issue time, oldest to newest
                         * based on the frame data.
                         */

                        // If this is the first time this zone has come through
                        // put it in the displayLabelMap
                        if (!displayLabelMap.containsKey(
                                eachPreProcessDisplayObj.singleFipsCode)) {

                            countyZoneLabel.displayLabel = enabledText;
                            List<RGB> rgbColors = new ArrayList<>();
                            for (String element : enabledText) {
                                rgbColors.add(colorOfEventRGB);
                            }
                            countyZoneLabel.displayColors = rgbColors;
                            displayLabelMap.put(
                                    eachPreProcessDisplayObj.singleFipsCode,
                                    countyZoneLabel);
                        } else {
                            // If this is not the first time get the object and
                            // add/change elements within as necessary
                            DisplayLabel currentDisplayLabelObj = displayLabelMap
                                    .get(eachPreProcessDisplayObj.singleFipsCode);
                            // add latest time string
                            currentDisplayLabelObj.displayLabel.add(timeString);
                            // add color of latest time string
                            currentDisplayLabelObj.displayColors
                                    .add(colorOfEventRGB);
                            // If the zone name is enabled it should be the RGB
                            // of the most significant event for that county.
                            // Order is Warning, Advisory, Watch.
                            if (currentDisplayLabelObj.isZoneNameEnabled) {
                                RGB currentZoneNameColor = currentDisplayLabelObj.displayColors
                                        .get(0);
                                // If it is already a warning, leave it alone
                                if (!currentZoneNameColor
                                        .equals(wstmResourceDataObj
                                                .getWstmWarningColor())) {
                                    // If it is an advisory, only change it if
                                    // the incoming product is a warning
                                    if (currentZoneNameColor
                                            .equals(wstmResourceDataObj
                                                    .getWstmAdvisoryColor())) {
                                        if (countyZoneLabel.eventColor
                                                .equals(wstmResourceDataObj
                                                        .getWstmWarningColor())) {
                                            currentDisplayLabelObj.displayCoords = countyZoneLabel.displayCoords = pixCoord;
                                            currentDisplayLabelObj.lineWidth = countyZoneLabel.lineWidth;
                                            currentDisplayLabelObj.symbolWidth = countyZoneLabel.symbolWidth;
                                            currentDisplayLabelObj.symbolSize = countyZoneLabel.symbolSize;
                                            currentDisplayLabelObj.symbolTypeStr = EditWstmAttrDialog.warningMarkerData;
                                            currentDisplayLabelObj.eventColor = countyZoneLabel.eventColor;
                                            currentDisplayLabelObj.displayColors
                                                    .set(0, wstmResourceDataObj
                                                            .getWstmWarningColor());
                                        }
                                        // If it is a watch change it if the
                                        // incoming product is a warning or
                                        // advisory
                                    } else {
                                        if (countyZoneLabel.eventColor
                                                .equals(wstmResourceDataObj
                                                        .getWstmWarningColor())) {
                                            currentDisplayLabelObj.displayCoords = countyZoneLabel.displayCoords = pixCoord;
                                            currentDisplayLabelObj.lineWidth = countyZoneLabel.lineWidth;
                                            currentDisplayLabelObj.symbolWidth = countyZoneLabel.symbolWidth;
                                            currentDisplayLabelObj.symbolSize = countyZoneLabel.symbolSize;
                                            currentDisplayLabelObj.symbolTypeStr = EditWstmAttrDialog.warningMarkerData;
                                            currentDisplayLabelObj.eventColor = countyZoneLabel.eventColor;
                                            currentDisplayLabelObj.displayColors
                                                    .set(0, wstmResourceDataObj
                                                            .getWstmWarningColor());
                                        } else if (countyZoneLabel.eventColor
                                                .equals(wstmResourceDataObj
                                                        .getWstmAdvisoryColor())) {
                                            currentDisplayLabelObj.displayCoords = countyZoneLabel.displayCoords = pixCoord;
                                            currentDisplayLabelObj.lineWidth = countyZoneLabel.lineWidth;
                                            currentDisplayLabelObj.symbolWidth = countyZoneLabel.symbolWidth;
                                            currentDisplayLabelObj.symbolSize = countyZoneLabel.symbolSize;
                                            currentDisplayLabelObj.symbolTypeStr = EditWstmAttrDialog.advisoryMarkerData;
                                            currentDisplayLabelObj.eventColor = countyZoneLabel.eventColor;
                                            currentDisplayLabelObj.displayColors
                                                    .set(0, wstmResourceDataObj
                                                            .getWstmAdvisoryColor());
                                        }
                                    }

                                }
                            } else {
                                RGB currentEventColor = currentDisplayLabelObj.eventColor;
                                if (!currentEventColor
                                        .equals(wstmResourceDataObj
                                                .getWstmWarningColor())) {
                                    // If it is an advisory, only change it if
                                    // the incoming product is a warning
                                    if (currentEventColor
                                            .equals(wstmResourceDataObj
                                                    .getWstmAdvisoryColor())) {
                                        if (countyZoneLabel.eventColor
                                                .equals(wstmResourceDataObj
                                                        .getWstmWarningColor())) {
                                            currentDisplayLabelObj.displayCoords = countyZoneLabel.displayCoords = pixCoord;
                                            currentDisplayLabelObj.lineWidth = countyZoneLabel.lineWidth;
                                            currentDisplayLabelObj.symbolWidth = countyZoneLabel.symbolWidth;
                                            currentDisplayLabelObj.symbolSize = countyZoneLabel.symbolSize;
                                            currentDisplayLabelObj.symbolTypeStr = EditWstmAttrDialog.warningMarkerData;
                                            currentDisplayLabelObj.eventColor = countyZoneLabel.eventColor;
                                        }
                                        // If it is a watch change it if the
                                        // incoming product is a warning or
                                        // advisory
                                    } else {
                                        if (countyZoneLabel.eventColor
                                                .equals(wstmResourceDataObj
                                                        .getWstmWarningColor())) {
                                            currentDisplayLabelObj.displayCoords = countyZoneLabel.displayCoords = pixCoord;
                                            currentDisplayLabelObj.lineWidth = countyZoneLabel.lineWidth;
                                            currentDisplayLabelObj.symbolWidth = countyZoneLabel.symbolWidth;
                                            currentDisplayLabelObj.symbolSize = countyZoneLabel.symbolSize;
                                            currentDisplayLabelObj.symbolTypeStr = EditWstmAttrDialog.warningMarkerData;
                                            currentDisplayLabelObj.eventColor = countyZoneLabel.eventColor;
                                        } else if (countyZoneLabel.eventColor
                                                .equals(wstmResourceDataObj
                                                        .getWstmAdvisoryColor())) {
                                            currentDisplayLabelObj.displayCoords = countyZoneLabel.displayCoords = pixCoord;
                                            currentDisplayLabelObj.lineWidth = countyZoneLabel.lineWidth;
                                            currentDisplayLabelObj.symbolWidth = countyZoneLabel.symbolWidth;
                                            currentDisplayLabelObj.symbolSize = countyZoneLabel.symbolSize;
                                            currentDisplayLabelObj.symbolTypeStr = EditWstmAttrDialog.advisoryMarkerData;
                                            currentDisplayLabelObj.eventColor = countyZoneLabel.eventColor;
                                        }
                                    }

                                }
                            }
                            displayLabelMap.put(
                                    eachPreProcessDisplayObj.singleFipsCode,
                                    currentDisplayLabelObj);
                        }
                    }
                }
                wstmRscAttr = null;
            }

            // loop through display label hashmap and draw labels.

            List<DisplayLabel> outlineWarningList = new ArrayList<>();
            List<DisplayLabel> outlineAdvisoryList = new ArrayList<>();
            List<DisplayLabel> outlineWatchList = new ArrayList<>();

            for (Entry<String, DisplayLabel> entry : displayLabelMap
                    .entrySet()) {
                DisplayLabel currentDisplayLabelObj = entry.getValue();
                currentDisplayLabelObj.fipsCode = entry.getKey();
                String[] textLabel = new String[currentDisplayLabelObj.displayLabel
                        .size()];
                textLabel = currentDisplayLabelObj.displayLabel
                        .toArray(textLabel);
                if (!currentDisplayLabelObj.isTimeEnabled
                        && currentDisplayLabelObj.isZoneNameEnabled) {
                    if (currentDisplayLabelObj.displayLabel.size() > 2) {
                        for (int i = 2; i <= currentDisplayLabelObj.displayLabel
                                .size() - 1; i++) {
                            textLabel[i] = "";
                        }
                    }
                }
                if (currentDisplayLabelObj.isTimeEnabled
                        || currentDisplayLabelObj.isZoneNameEnabled) {
                    RGB[] color = new RGB[currentDisplayLabelObj.displayColors
                            .size()];
                    color = currentDisplayLabelObj.displayColors.toArray(color);
                    DrawableString zoneNameString = new DrawableString(
                            textLabel, color);
                    zoneNameString.setCoordinates(
                            currentDisplayLabelObj.displayCoords.getX(),
                            currentDisplayLabelObj.displayCoords.getY());
                    zoneNameString.textStyle = TextStyle.NORMAL;
                    zoneNameString.horizontalAlignment = HorizontalAlignment.LEFT;
                    zoneNameString.verticallAlignment = VerticalAlignment.TOP;
                    target.drawStrings(zoneNameString);
                }
                if (!wstmResourceDataObj.getOutlineEnable()) {
                    if (currentDisplayLabelObj.markerCoordinate != null) {
                        DisplayElementFactory df = new DisplayElementFactory(
                                target, getNcMapDescriptor());
                        List<IDisplayable> displayEls = new ArrayList<>(0);
                        Color eventColor = new Color(
                                currentDisplayLabelObj.eventColor.red,
                                currentDisplayLabelObj.eventColor.green,
                                currentDisplayLabelObj.eventColor.blue);
                        Color[] symbolColor = { eventColor };
                        Symbol symbol = new Symbol(null, symbolColor,
                                currentDisplayLabelObj.lineWidth,
                                // scale per NMAP
                                currentDisplayLabelObj.symbolSize, false,
                                currentDisplayLabelObj.markerCoordinate,
                                "Symbol", this.getActualSymbolName(
                                        currentDisplayLabelObj.symbolTypeStr));
                        displayEls = df.createDisplayElements(symbol,
                                paintProps);
                        if (displayEls != null && !displayEls.isEmpty()) {
                            for (IDisplayable each : displayEls) {
                                each.draw(target, paintProps);
                                each.dispose();
                            }
                        }
                    }
                } else {
                    // Build lists for outline lists to be plotted separately.
                    if (currentDisplayLabelObj.eventColor.equals(
                            wstmResourceDataObj.getWstmWarningColor())) {
                        outlineWarningList.add(currentDisplayLabelObj);
                    } else if (currentDisplayLabelObj.eventColor.equals(
                            wstmResourceDataObj.getWstmAdvisoryColor())) {
                        outlineAdvisoryList.add(currentDisplayLabelObj);
                    } else {
                        outlineWatchList.add(currentDisplayLabelObj);

                    }
                }
                /*
                 * if the outline is enabled, create the outline in backward
                 * priority order such warning outlines are always on top,
                 * followed by advisory, followed by watches.
                 */
                if (wstmResourceDataObj.getOutlineEnable()) {
                    for (DisplayLabel watch : outlineWatchList) {
                        drawOutlineForZone(watch.fipsCode, target,
                                watch.eventColor, watch.lineWidth);
                    }
                    for (DisplayLabel advisory : outlineAdvisoryList) {
                        drawOutlineForZone(advisory.fipsCode, target,
                                advisory.eventColor, advisory.lineWidth);
                    }
                    for (DisplayLabel warning : outlineWarningList) {
                        drawOutlineForZone(warning.fipsCode, target,
                                warning.eventColor, warning.lineWidth);
                    }
                }
            }
        }
    }

    @Override
    /***
     * Overridden method to process the incoming AWW records
     *
     * @param pdo
     *            - the AwwRecord Returns an array of IRscDataObject processed
     *            from the AwwRecord
     */
    protected IRscDataObject[] processRecord(Object pdo) {
        if (!(pdo instanceof AwwRecord)) {
            statusHandler.error(
                    "Object is of type " + pdo.getClass().getCanonicalName()
                            + "instead of type AwwRecord");
            return new IRscDataObject[] {};
        }
        AwwRecord awwRecord = (AwwRecord) pdo;

        List<PreProcessDisplay> wstmRscDataObjectList = getWstmData(awwRecord);
        if (wstmRscDataObjectList == null || wstmRscDataObjectList.isEmpty()) {
            return new IRscDataObject[] {};
        } else {
            return wstmRscDataObjectList.toArray(new PreProcessDisplay[0]);
        }
    }

    /***
     *
     * @param latLonPt
     * @return
     */
    private Coordinate convertCentroidToWorldCoordinates(LatLonPoint latLonPt) {
        Coordinate worldCoord = null;
        if (latLonPt != null) {
            double pointArr[] = new double[] { latLonPt.getLongitude(),
                    latLonPt.getLatitude() };
            worldCoord = new Coordinate(pointArr[0], pointArr[1]);
        }
        return worldCoord;
    }

    /***
     *
     * @param iconName
     * @return
     */
    private String getActualSymbolName(String iconName) {
        String actualSymbolName = "ASTERISK";
        if (iconName.compareTo("TRIANGLE") == 0) {
            // refer symbolPatterns.xml
            actualSymbolName = "FILLED_TRIANGLE";
        } else if (iconName.compareTo("OCTAGON") == 0) {
            actualSymbolName = "FILLED_OCTAGON";
        } else if (iconName.compareTo("SQUARE") == 0) {
            actualSymbolName = "FILLED_BOX";
        } else if (iconName.compareTo("STAR") == 0) {
            actualSymbolName = "FILLED_STAR";
        } else if (iconName.compareTo("DIAMOND") == 0) {
            actualSymbolName = "FILLED_DIAMOND";
        }
        return actualSymbolName;
    }

    /***
     * Returns a list of <code>WstmRscDataObject</code> Each
     * <code>WstmRscDataObject</code> in the list maps to a Vtec line from the
     * original bulletin.
     *
     * @param awwRecord
     *            - the AwwRecord retrieved from the database
     * @return a list of <code>WstmRscDataObject</code>
     */
    private List<PreProcessDisplay> getWstmData(AwwRecord awwRecord) {
        PreProcessDisplay wstmRscDataObject = null;
        List<PreProcessDisplay> wstmRscDataObjectList = new ArrayList<>(0);
        Set<AwwUgc> thisAwwUgcSet = awwRecord.getAwwUGC();

        for (AwwUgc eachAwwUgc : thisAwwUgcSet) {
            Set<AwwVtec> aSetOfAwwVtec = new HashSet<>(
                    eachAwwUgc.getAwwVtecLine());
            Set<AwwFips> aSetOfAwwFips = new HashSet<>(eachAwwUgc.getAwwFIPS());

            for (AwwVtec thisVtec : aSetOfAwwVtec) {
                /*
                 * Only add objects if they are a member of the winter storm
                 * family.
                 */
                wstmRscDataObject = new PreProcessDisplay();
                String buildReportType = thisVtec.getPhenomena() + "."
                        + thisVtec.getSignificance();

                // TODO: consider using a map
                switch (buildReportType.toUpperCase()) {
                case "BZ.A":
                    wstmRscDataObject.reportType = AwwReportType.BLIZZARD_WATCH;
                    break;
                case "BZ.W":
                    wstmRscDataObject.reportType = AwwReportType.BLIZZARD_WARNING;
                    break;
                case "IS.W":
                    wstmRscDataObject.reportType = AwwReportType.ICE_STORM_WARNING;
                    break;
                case "LE.A":
                    wstmRscDataObject.reportType = AwwReportType.LAKE_EFFECT_SNOW_WATCH;
                    break;
                case "LE.W":
                    wstmRscDataObject.reportType = AwwReportType.LAKE_EFFECT_SNOW_WARNING;
                    break;
                case "LE.Y":
                    wstmRscDataObject.reportType = AwwReportType.LAKE_EFFECT_SNOW_ADVISORY;
                    break;
                case "WS.A":
                    wstmRscDataObject.reportType = AwwReportType.WINTER_STORM_WATCH;
                    break;
                case "WS.W":
                    wstmRscDataObject.reportType = AwwReportType.WINTER_STORM_WARNING;
                    break;
                case "WW.Y":
                    wstmRscDataObject.reportType = AwwReportType.WINTER_WEATHER_ADVISORY;
                    break;
                case "ZR.Y":
                    wstmRscDataObject.reportType = AwwReportType.FREEZING_RAIN_ADVISORY;
                    break;
                default:
                    wstmRscDataObject.reportType = null;
                }

                if (wstmRscDataObject.reportType != null) {
                    /*
                     * From each VTEC line in the bulletin retrieve the
                     * following information about the weather hazard:
                     */
                    wstmRscDataObject.evSignificance = thisVtec
                            .getSignificance();
                    wstmRscDataObject.evPhenomena = thisVtec.getPhenomena();

                    wstmRscDataObject.evTrack = thisVtec
                            .getEventTrackingNumber();
                    wstmRscDataObject.evOfficeId = thisVtec.getOfficeID();
                    wstmRscDataObject.issueTime = new DataTime(
                            awwRecord.getIssueTime());
                    wstmRscDataObject.eventType = thisVtec.getAction();
                    Calendar startTimeCal = awwRecord.getIssueTime();
                    Calendar endTimeCal = thisVtec.getEventEndTime();
                    Calendar eventStartTime = thisVtec.getEventStartTime();
                    /*
                     * The startTimeCal will be null if the product is issued
                     * after the event started. In this case, the start time is
                     * set to the issue-time.
                     */

                    if (eventStartTime == null) {
                        eventStartTime = awwRecord.getIssueTime();
                    }

                    if (startTimeCal != null && endTimeCal != null) {
                        wstmRscDataObject.endTime = new DataTime(endTimeCal);
                        wstmRscDataObject.eventTime = new DataTime(startTimeCal,
                                new TimeRange(startTimeCal, endTimeCal));
                    }
                    wstmRscDataObject.origStartTime = new DataTime(
                            eventStartTime);
                    wstmRscDataObject.origEndTime = wstmRscDataObject.endTime;
                    wstmRscDataObject.displayStart = wstmRscDataObject.issueTime;
                    wstmRscDataObject.displayEnd = wstmRscDataObject.origEndTime;

                    // If zone does not have a record in the zone table
                    // do not add it to the list of zones to be added into
                    // the list of things to eventually be drawn.
                    boolean doAdd = true;
                    if (aSetOfAwwFips != null && !aSetOfAwwFips.isEmpty()) {
                        wstmRscDataObject.fipsCodesList = createListOfFipsInfoObjects(
                                aSetOfAwwFips);
                        // Since we are using a common AWW object this
                        // information needs to be determined prior to the wqr
                        // map being populated.
                        for (String element : wstmRscDataObject.fipsCodesList) {
                            Double fipsLat = getLatLon(element, "lat");
                            Double fipsLon = getLatLon(element, "lon");
                            if (!fipsLat.equals(0.0) && !fipsLon.equals(0.0)) {
                                wstmRscDataObject.countyZoneLatList
                                        .add(fipsLat);
                                wstmRscDataObject.countyZoneLonList
                                        .add(fipsLon);
                            } else {
                                doAdd = false;
                            }
                        }
                    }
                    if (doAdd) {
                        wstmRscDataObjectList.add(wstmRscDataObject);
                    }

                }
            }
        }
        return wstmRscDataObjectList;
    }

    // Since AWW resources use common data to display county by county,
    // a common object was created thus removing the need to have a separate
    // object for each resource.
    protected class FrameData extends AbstractFrameData {
        private Map<String, PreProcessDisplay> wstmDataMap;

        /**
         * Overloaded Constructor
         *
         * @param ftime
         * @param frameInterval
         */
        protected FrameData(DataTime ftime, int frameInterval) {
            super(ftime, frameInterval);
            wstmDataMap = new HashMap<>();
        }

        @Override
        /**
         * Updates the <code> Map of WstmRscDataObject </code> in each frame,
         * based on the action type of the incoming
         * <code> WstmRscDataObject </code>
         */
        public boolean updateFrameData(IRscDataObject rscDataObj) {
            if (!(rscDataObj instanceof PreProcessDisplay)) {
                statusHandler.error("rscDataObj belongs to class"
                        + rscDataObj.getClass().getCanonicalName());

                return false;
            }
            PreProcessDisplay thisWstmRscDataObject = (PreProcessDisplay) rscDataObj;
            String key = thisWstmRscDataObject.evOfficeId + "."
                    + thisWstmRscDataObject.evTrack + "."
                    + thisWstmRscDataObject.evPhenomena + "."
                    + thisWstmRscDataObject.evSignificance + "."
                    + thisWstmRscDataObject.fipsCodesList.get(0) + "."
                    + thisWstmRscDataObject.issueTime + "."
                    + thisWstmRscDataObject.eventType;

            if (wstmDataMap.containsKey(key)) {
                wstmDataMap.put(
                        thisWstmRscDataObject.evOfficeId + "."
                                + thisWstmRscDataObject.evTrack + "."
                                + thisWstmRscDataObject.evPhenomena + "."
                                + thisWstmRscDataObject.evSignificance + "."
                                + thisWstmRscDataObject.issueTime + "."
                                + thisWstmRscDataObject.eventType + ". "
                                + String.join(" ",
                                        thisWstmRscDataObject.fipsCodesList),
                        thisWstmRscDataObject);
            } else {
                wstmDataMap.put(key, thisWstmRscDataObject);
            }

            /*
             * Changed where display times were being handled to county by
             * county object creation instead of when it went into the
             * wstmDataMap.
             */
            return true;
        }

    }

    /***
     * Private class to capture the attributes (such as the color, line width,
     * symbol width etc) of each event from the WstmResourceData class.
     *
     * @author archana
     *
     * @param <Boolean>
     *            - flag to check if the model is enabled
     * @param <RGB>
     *            - the color of the model
     * @param <Integer>
     *            - the width of the outline (line width)
     * @param <Integer>
     *            - the width of the the symbol
     * @param <Float>
     *            - the size of the symbol
     *
     */

    // Removed parameterization as it was not needed.
    private class WstmResourceAttributes {
        private Boolean eventEnable;

        private Integer symbolWidth;

        private Float symbolSize;

        private Integer lineWidth;

        private RGB colorOfEvent;

        public WstmResourceAttributes(Boolean evEnable, RGB eventColor,
                Integer lineWidth, Integer symbolWidth, Float symbolSize) {
            this.colorOfEvent = eventColor;
            this.symbolSize = symbolSize;
            this.symbolWidth = symbolWidth;
            this.lineWidth = lineWidth;
            this.eventEnable = evEnable;
        }

        /**
         * @return the eventEnable
         */
        private Boolean getEventEnable() {
            return eventEnable;
        }

        /**
         * @return the symbolWidth
         */
        private Integer getSymbolWidth() {
            return symbolWidth;
        }

        /**
         * @return the symbolSize
         */
        private Float getSymbolSize() {
            return symbolSize;
        }

        /**
         * @return the lineWidth
         */
        private Integer getLineWidth() {
            return lineWidth;
        }

        /**
         * @return the colorOfEvent
         */
        private RGB getColorOfEvent() {
            return colorOfEvent;
        }
    }

    private WstmQueryResult wqr = new WstmQueryResult();

    // for pre-calculate the IWiredframeShape
    private ZoneResultJob zrJob = new ZoneResultJob("");

    // Area change flag
    private boolean areaChangeFlag = false;

    @Override
    public void queryRecords() throws VizException {
        DbQueryRequest request = new DbQueryRequest();
        Map<String, RequestConstraint> queryList = new HashMap<>();
        RequestConstraint pluginName = new RequestConstraint("aww");
        List<Object[]> results = null;
        IDescriptor.FramesInfo frameTimes = this.descriptor.getFramesInfo();
        int numberOfFramesForArray = frameTimes.getFrameCount() - 1;
        Calendar startFrameTime = frameTimes.getFrameTimes()[0]
                .getRefTimeAsCalendar();
        Calendar endFrameTime = frameTimes
                .getFrameTimes()[numberOfFramesForArray].getRefTimeAsCalendar();
        DataTime queryStartTime = new DataTime(startFrameTime);
        DataTime queryEndTime = new DataTime(endFrameTime);

        StringBuilder querybuilder = new StringBuilder();
        querybuilder.append(
                "select distinct aww.id from aww,aww_ugc,aww_vtec where aww.id = aww_ugc.parentid and aww_ugc.recordid = aww_vtec.parentid and aww_vtec.eventendtime >='");
        querybuilder.append(queryStartTime);
        querybuilder.append("' and aww.reftime <= '");
        querybuilder.append(queryEndTime);
        querybuilder.append("' and aww.reporttype = 'WINTER WEATHER';");

        results = DirectDbQuery.executeQuery(querybuilder.toString(),
                "metadata", QueryLanguage.SQL);
        Collection<String> id = new ArrayList<>();
        for (Object[] result : results) {
            id.add(result[0].toString());
        }
        queryList.put("pluginName", pluginName);
        queryList.put("reportType",
                resourceData.getMetadataMap().get("reportType"));
        queryList.put("id", new RequestConstraint(id));
        request.setConstraints(queryList);
        DbQueryResponse response = (DbQueryResponse) ThriftClient
                .sendRequest(request);
        List<Object> pdoList = new ArrayList<>();
        for (Map<String, Object> element : response.getResults()) {
            for (Map.Entry<String, Object> entry : element.entrySet()) {
                pdoList.add(entry.getValue());
            }
        }

        class ProcessRecordRunnable implements Runnable {
            private Object runpdo = null;

            public ProcessRecordRunnable(Object pdo) {
                runpdo = pdo;
            }

            @Override
            public void run() {
                for (IRscDataObject dataObject : processRecord(runpdo)) {
                    newRscDataObjsQueue.add(dataObject);
                }
            }
        }
        ExecutorService pool = Executors.newFixedThreadPool(10);
        for (Object pdo : pdoList) {
            pool.submit(new ProcessRecordRunnable(pdo));
        }
        try {
            pool.shutdown();
            pool.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            statusHandler.error("Thread interrupted.");
        }

        for (IRscDataObject dataObject : newRscDataObjsQueue) {
            wqr.buildQueryPart(dataObject);

        }

        wqr.populateFipsMap();
        setAllFramesAsPopulated();
    }

    private List<String> createListOfFipsInfoObjects(
            Set<AwwFips> aSetOfAwwFips) {

        List<String> thisListOfFipsInfo = new ArrayList<>();
        for (AwwFips af : aSetOfAwwFips) {
            String fips = af.getFips();
            thisListOfFipsInfo.add(fips);
        }
        return thisListOfFipsInfo;
    }

    /**
     * handles the IWireframeShape pre-calculation
     *
     * @author gzhang
     */
    private class ZoneResultJob extends org.eclipse.core.runtime.jobs.Job {

        private Map<String, Result> keyResultMap = new java.util.concurrent.ConcurrentHashMap<>();

        private IGraphicsTarget target;

        private IMapDescriptor descriptor;

        private final RGB symbolColor = new RGB(155, 155, 155);

        public class Result {

            public IWireframeShape outlineShape;

            private Result(IWireframeShape outlineShape,
                    IWireframeShape nuShape, IShadedShape shadedShape,
                    Map<Object, RGB> colorMap) {

                this.outlineShape = outlineShape;
            }
        }

        public ZoneResultJob(String name) {
            super(name);
        }

        public void setRequest(IGraphicsTarget target,
                IMapDescriptor descriptor, String query, boolean labeled,
                boolean shaded, Map<Object, RGB> colorMap) {

            this.target = target;
            this.descriptor = descriptor;
            this.run(null);

        }

        @Override
        protected org.eclipse.core.runtime.IStatus run(
                org.eclipse.core.runtime.IProgressMonitor monitor) {

            for (AbstractFrameData afd : frameDataMap.values()) {
                FrameData fd = (FrameData) afd;
                for (PreProcessDisplay wrdo : fd.wstmDataMap.values()) {
                    for (String fi : wrdo.fipsCodesList) {
                        Collection<Geometry> gw = new ArrayList<>();
                        for (List<Object[]> zones : wqr.getZoneResult(fi)) {
                            if (zones == null) {
                                continue;
                            }
                            WKBReader wkbReader = new WKBReader();
                            for (Object[] result : zones) {
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
                                    statusHandler.error(e.getMessage(), e);
                                }
                            }
                        }
                        if (!gw.isEmpty()) {
                            keyResultMap.put(fi, new Result(
                                    getEachWrdoShape(gw), null, null, null));
                        }
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
                statusHandler.error(e.getMessage(), e);
            }

            return newOutlineShape;
        }
    }

    private void drawOutlineForZone(String fipsCode, IGraphicsTarget target,
            RGB lineColor, int lineWidth) throws VizException {

        ZoneResultJob.Result result = zrJob.keyResultMap.get(fipsCode);

        // for storing result of pre-calculation
        IWireframeShape outlineShape;

        if (result != null) {
            outlineShape = result.outlineShape;
        } else {
            return;
        }
        if (outlineShape != null && outlineShape.isDrawable()) {
            try {
                target.drawWireframeShape(outlineShape, lineColor, lineWidth,
                        LineStyle.SOLID);
            } catch (VizException e) {
                statusHandler.error(e.getMessage(), e);
            }
        }
    }

    @Override
    protected boolean postProcessFrameUpdate() {

        AbstractEditor ncme = NcDisplayMngr.getActiveNatlCntrsEditor();

        zrJob.setRequest(ncme.getActiveDisplayPane().getTarget(),
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
                    "===== find IRscDataObject rscDataObj.getDataTime() returned NULL!!!");
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
                || fd.wstmDataMap.size() == 0) {
            return legendString + "-No Data";
        }
        return legendString + " "
                + NmapCommon.getTimeStringFromDataTime(fd.getFrameTime(), "/");
    }

    public double getLatLon(String zoneFips, String latLon) {
        List<Object[]> results = null;
        String queryPrefix = "select " + latLon
                + " from mapdata.zonelowres where state_zone =";
        String dbZoneFips = zoneFips.substring(0, 2) + zoneFips.substring(3);
        try {
            String wholeQuery = queryPrefix + "'" + dbZoneFips + "'" + ";";

            results = DirectDbQuery.executeQuery(wholeQuery, "maps",
                    QueryLanguage.SQL);
            double zoneFipsLon = ((Number) results.get(0)[0]).doubleValue();
            return zoneFipsLon;
        } catch (Exception e) {
            // if there is a problem with the query make a note in the the
            // console log and skip it.
            statusHandler.warn("Longitude information for " + zoneFips
                    + " was not found in the zone table. This zone will be skipped.",
                    e);
            return 0;
        }
    }

    /*
     * Added display label class to pass from the PreProcessDisplay label map to
     * the display label logic.
     */
    public class DisplayLabel {
        private List<String> displayLabel;

        private List<RGB> displayColors;

        private PixelCoordinate displayCoords;

        private List<String> evSignificance;

        private boolean isZoneNameEnabled;

        private boolean isTimeEnabled;

        private int lineWidth;

        private RGB eventColor;

        private Coordinate markerCoordinate;

        private Integer symbolWidth;

        private Float symbolSize;

        private String symbolTypeStr;

        private String fipsCode;
    }
}
