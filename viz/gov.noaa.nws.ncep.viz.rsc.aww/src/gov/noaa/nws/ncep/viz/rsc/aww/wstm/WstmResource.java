/*
 * WstmResource
 * 
 * Date created (November 05, 2010)
 *
 *  This code has been developed by the SIB for use in the AWIPS2 system. 
 */
package gov.noaa.nws.ncep.viz.rsc.aww.wstm;

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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.raytheon.uf.common.dataquery.requests.DbQueryRequest;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint.ConstraintType;
import com.raytheon.uf.common.dataquery.responses.DbQueryResponse;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.time.TimeRange;
import com.raytheon.uf.edex.decodertools.core.LatLonPoint;
import com.raytheon.uf.viz.core.DrawableString;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.VizApp;
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
import com.raytheon.viz.ui.UiPlugin;
import com.raytheon.viz.ui.editor.AbstractEditor;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKBReader;

/**
 * WstmResource - Displays Winter Storm Misc Resource
 * 
 * 
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 05-Nov- 2010   247       Archana    Initial creation.   
 * 16 Feb 2012    555      S. Gurung   Added call to setAllFramesAsPopulated() in queryRecords().
 * 05/23/2012     785      Q. Zhou     Added getName for legend.
 * 17 Aug 2012    655      B. Hebbard  Added paintProps as parameter to IDisplayable draw
 * 31-Jan-2013    976      Archana         Updated paintFrame() to not render any null strings
 *                                                                Replaced the depreciated target.drawString() method with target.drawStrings().
 * 17-Nov-2014    RM 5125   J. Huber   Removed dead and deprecated code. Implemented use of new common object for AWW and removed local WstmRscDataObject. Implemented
 *                                     common method to create display objects for individual counties. Broke out label display from the return of the frame data
 *                                     because of issue found with multiple products being valid at the same time. Added DisplayLabel class to be able to create
 *                                     a list of display labels and draw all labels at one time instead of county by county.
 *                                                                                    
 * </pre>
 * 
 * @author archana
 * @version 1.0
 */
public class WstmResource extends
        AbstractNatlCntrsResource<WstmResourceData, NCMapDescriptor> implements
        INatlCntrsResource {
    List<String> issueOfficeList = new ArrayList<String>(0);

    private IFont font = null;

    int i = 0;

    float baseFontSize = 14;

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
    protected WstmResource(WstmResourceData resourceData, LoadProperties props) {
        super(resourceData, props);
        wstmResourceDataObj = (WstmResourceData) resourceData;
    }

    @Override
    /**
     * Creates a new frame with the specified reference time and frame interval
     * @param frameTime - The reference time for the frame to be created
     * @param frameInterval - The interval between the created frames
     * 
     * @return  Returns the new frame with the specified frame reference time and time interval 
     */
    protected AbstractFrameData createNewFrame(DataTime frameTime,
            int frameInterval) {
        return (AbstractFrameData) new FrameData(frameTime, frameInterval);
    }

    @Override
    /**
     * Overridden method. Invokes queryRecords() to fetch the records from the database
     * per the metadata map in WSTM.xml
     */
    public void initResource(IGraphicsTarget grphTarget) throws VizException {
        long t1 = System.currentTimeMillis();
        queryRecords();
        long t2 = System.currentTimeMillis();
        System.out.println("__^^^__ initResource (t2-t1): " + (t2 - t1));
    }

    @Override
    /***
     * Renders the WSTM resource on the frame
     */
    public void paintFrame(AbstractFrameData frameData, IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {

        if (font == null) {
            font = target.initializeFont("Monospace", 14,
                    new IFont.Style[] { IFont.Style.BOLD });
        }

        if (areaChangeFlag) {
            areaChangeFlag = false;
            postProcessFrameUpdate();
        }// T456
        if (frameData != null) {
            FrameData currFrameData = (FrameData) frameData;
                     
            List<PreProcessDisplay> sortedWstmRecords = new ArrayList<PreProcessDisplay>();

            //RM 5125 sort records by issue time and then send them to be processed and return
            //with one county in each object.
            
            for (Map.Entry<String, PreProcessDisplay> entry : currFrameData.wstmDataMap
                    .entrySet()) {
                sortedWstmRecords.add(entry.getValue());
            }
            Collections.sort(sortedWstmRecords);            
            HashMap<String, DisplayLabel> displayLabelMap;
            displayLabelMap = new HashMap<String,DisplayLabel>();            
            List<PreProcessDisplay> displayObjs = CountyObjectCreator.PreProcessDisplay(sortedWstmRecords);

            for (PreProcessDisplay eachPreProcessDisplayObj : displayObjs) {
                Boolean draw = false;
                WstmResourceAttributes wstmRscAttr = null;
                String symbolTypeStr = "";                
                eachPreProcessDisplayObj.zoneName = wqr.getZoneName(eachPreProcessDisplayObj.singleFipsCode);
                
                /*
                 * Retrieve the user-configurable attributes depending on
                 * whether the WstmRscDataObject denotes an advisory, watch or a
                 * warning
                 */
                
                //RM 5125 removed ENUMs and just used raw record
                if (eachPreProcessDisplayObj.evSignificance.equalsIgnoreCase("Y")) {
                    wstmRscAttr = new WstmResourceAttributes(
                            wstmResourceDataObj.getWstmAdvisoryEnable(),
                            wstmResourceDataObj.getWstmAdvisoryColor(),
                            wstmResourceDataObj.getWstmAdvisoryLineWidth(),
                            wstmResourceDataObj.getWstmAdvisorySymbolWidth(),
                            wstmResourceDataObj.getWstmAdvisorySymbolSize());
                    symbolTypeStr = new String(
                            EditWstmAttrDialog.advisoryMarkerData);
                } else if (eachPreProcessDisplayObj.evSignificance.equalsIgnoreCase("W")) {

                    wstmRscAttr = new WstmResourceAttributes(
                            wstmResourceDataObj.getWstmWarningEnable(),
                            wstmResourceDataObj.getWstmWarningColor(),
                            wstmResourceDataObj.getWstmWarningLineWidth(),
                            wstmResourceDataObj.getWstmWarningSymbolWidth(),
                            wstmResourceDataObj.getWstmWarningSymbolSize());
                    symbolTypeStr = new String(
                            EditWstmAttrDialog.warningMarkerData);
                } else if (eachPreProcessDisplayObj.evSignificance.equalsIgnoreCase("A")) {
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
            
                    if (getCurrentFrameTime().getValidTimeAsDate().getTime() < eachPreProcessDisplayObj.displayEnd.getValidPeriod().getEnd().getTime()
                            || getCurrentFrameTime().getValidTimeAsDate().getTime() >= eachPreProcessDisplayObj.displayStart
                                .getValidPeriod().getStart().getTime()) {
                        draw = true;
                    }

                    if (getCurrentFrameTime().getValidTimeAsDate().getTime() > eachPreProcessDisplayObj.displayEnd
                            .getValidPeriod().getEnd().getTime()) {
                        draw = false;
                    }

                    if (getCurrentFrameTime().getValidTimeAsDate().getTime() < eachPreProcessDisplayObj.displayStart
                            .getValidPeriod().getStart().getTime()) {
                        draw = false;
                    }
                    
                    if (getCurrentFrameTime().getValidTimeAsDate().getTime() == (eachPreProcessDisplayObj.displayEnd
                            .getValidPeriod().getEnd().getTime())) {
                    // do not draw endtime frame, that's what nmap2 does
                        draw = false;
                    }
                    if (draw == true) {
                        List<String> enabledText = new ArrayList<String>();
                        String timeString = null;
                        DisplayLabel countyZoneLabel = new DisplayLabel();
                        

                            /*
                             * If the flag is enabled - Plot the name of the
                             * current FIPS zone in which this weather hazard is
                             * valid
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
                             * If the flag is enabled - Plot the duration for
                             * which the weather hazard (WstmRescDataObject) is
                             * valid
                             */
                        if (wstmResourceDataObj.getTimeEnable()) {
                            if (eachPreProcessDisplayObj.displayStart != null && eachPreProcessDisplayObj.displayEnd != null) {
                                countyZoneLabel.isTimeEnabled = true;
                                DataTime startTime = new DataTime(eachPreProcessDisplayObj.origStartTime
                                        .getValidPeriod().getStart());
                                DataTime endTime = new DataTime(eachPreProcessDisplayObj.origEndTime
                                        .getValidPeriod().getEnd());
                                timeString = startTime.toString()
                                        .substring(8, 10) + "/" + startTime.toString()
                                        .substring(11, 13)
                                        + startTime.toString().substring(14, 16)
                                        + "-"
                                        + endTime.toString()
                                        .substring(8, 10) + "/" 
                                        + endTime.toString().substring(11, 13)
                                        + endTime.toString().substring(14, 16);
                                enabledText.add(timeString);
                            }
                       }
                        countyZoneLabel.eventColor = wstmRscAttr.getColorOfEvent();
                        countyZoneLabel.symbolWidth = wstmRscAttr.getSymbolWidth();
                        countyZoneLabel.symbolSize = wstmRscAttr.getSymbolSize();
                        countyZoneLabel.symbolTypeStr = symbolTypeStr;
                        countyZoneLabel.lineWidth = wstmRscAttr.getLineWidth();
                        LatLonPoint zoneLatLon = new LatLonPoint (eachPreProcessDisplayObj.singleCountyZoneLat,eachPreProcessDisplayObj.singleCountyZoneLon, LatLonPoint.INDEGREES);
                        Coordinate thisMarkerCoord = this.convertCentroidToWorldCoordinates(zoneLatLon);
                        countyZoneLabel.markerCoordinate = thisMarkerCoord;
                      //RM 5125 Base the label offset on the symbol size so it is never in the middle of the marker
                        float labelOffsetX = (float) .03 * countyZoneLabel.symbolSize;
                        float labelOffsetY = (float) .015 * countyZoneLabel.symbolSize;
                        PixelCoordinate pixCoord = null;
                        double worldC[] = new double[] {
                                thisMarkerCoord.x + labelOffsetX,
                                thisMarkerCoord.y - labelOffsetY}; //offset label based on symbol size
                        pixCoord = new PixelCoordinate(descriptor.worldToPixel(worldC));
                        
                        countyZoneLabel.displayCoords = pixCoord;
                        //RM 5125 Since multiple products could be in affect at the same time we must build
                        //the display label arrays and color arrays for each county prior to trying to create 
                        //the draw string. The display color for markers, outlines, and zone names are determined by
                        //which products are in effect. Priority for color is Warning, then Advisory, then Watch.
                        //Labels will be drawn in the order of issue time, oldest to newest based on the frame data.
                        
                        //If this is the first time this zone has come through put it in the displayLabelMap

                        if (!displayLabelMap.containsKey(eachPreProcessDisplayObj.singleFipsCode)){

                            countyZoneLabel.displayLabel = enabledText;
                            List<RGB> rgbColors = new ArrayList<RGB>();
                            for (i = 0; i < enabledText.size();i++){
                                rgbColors.add(colorOfEventRGB);
                            }
                            countyZoneLabel.displayColors = rgbColors;
                            displayLabelMap.put(eachPreProcessDisplayObj.singleFipsCode, countyZoneLabel);
                        } else {
                            //If this is not the first time get the object and add/change elements within as necessary
                            DisplayLabel currentDisplayLabelObj = displayLabelMap.get(eachPreProcessDisplayObj.singleFipsCode);
                            //add latest time string
                            currentDisplayLabelObj.displayLabel.add(timeString);
                            //add color of latest time string
                            currentDisplayLabelObj.displayColors.add(colorOfEventRGB);
                            //If the zone name is enabled it should be the RGB of the most significant event for that county.
                            //Order is Warning, Advisory, Watch.
                            if (currentDisplayLabelObj.isZoneNameEnabled){
                                RGB currentZoneNameColor = currentDisplayLabelObj.displayColors.get(0);
                                //If it is already a warning, leave it alone
                                if(!currentZoneNameColor.equals(wstmResourceDataObj.getWstmWarningColor())){
                                    //If it is an advisory, only change it if the incoming product is a warning
                                    if(currentZoneNameColor.equals(wstmResourceDataObj.getWstmAdvisoryColor())){
                                        if(countyZoneLabel.eventColor.equals(wstmResourceDataObj.getWstmWarningColor())){
                                            currentDisplayLabelObj.displayCoords = countyZoneLabel.displayCoords = pixCoord;
                                            currentDisplayLabelObj.lineWidth = countyZoneLabel.lineWidth;
                                            currentDisplayLabelObj.symbolWidth = countyZoneLabel.symbolWidth;
                                            currentDisplayLabelObj.symbolSize = countyZoneLabel.symbolSize;
                                            currentDisplayLabelObj.symbolTypeStr = EditWstmAttrDialog.warningMarkerData;
                                            currentDisplayLabelObj.eventColor = countyZoneLabel.eventColor;
                                            currentDisplayLabelObj.displayColors.set(0, wstmResourceDataObj.getWstmWarningColor());
                                        }
                                        //If it is a watch change it if the incoming product is a warning or advisory
                                    } else {
                                        if(countyZoneLabel.eventColor.equals(wstmResourceDataObj.getWstmWarningColor())){
                                            currentDisplayLabelObj.displayCoords = countyZoneLabel.displayCoords = pixCoord;
                                            currentDisplayLabelObj.lineWidth = countyZoneLabel.lineWidth;
                                            currentDisplayLabelObj.symbolWidth = countyZoneLabel.symbolWidth;
                                            currentDisplayLabelObj.symbolSize = countyZoneLabel.symbolSize;
                                            currentDisplayLabelObj.symbolTypeStr = EditWstmAttrDialog.warningMarkerData;
                                            currentDisplayLabelObj.eventColor = countyZoneLabel.eventColor;
                                            currentDisplayLabelObj.displayColors.set(0, wstmResourceDataObj.getWstmWarningColor());
                                        } else if (countyZoneLabel.eventColor.equals(wstmResourceDataObj.getWstmAdvisoryColor())){
                                            currentDisplayLabelObj.displayCoords = countyZoneLabel.displayCoords = pixCoord;
                                            currentDisplayLabelObj.lineWidth = countyZoneLabel.lineWidth;
                                            currentDisplayLabelObj.symbolWidth = countyZoneLabel.symbolWidth;
                                            currentDisplayLabelObj.symbolSize = countyZoneLabel.symbolSize;
                                            currentDisplayLabelObj.symbolTypeStr = EditWstmAttrDialog.advisoryMarkerData;
                                            currentDisplayLabelObj.eventColor = countyZoneLabel.eventColor;
                                            currentDisplayLabelObj.displayColors.set(0, wstmResourceDataObj.getWstmAdvisoryColor()); 
                                        }
                                    }
                                        
                                    }
                            } else {
                                RGB currentEventColor = currentDisplayLabelObj.eventColor;
                                if(!currentEventColor.equals(wstmResourceDataObj.getWstmWarningColor())){
                                    //If it is an advisory, only change it if the incoming product is a warning
                                    if(currentEventColor.equals(wstmResourceDataObj.getWstmAdvisoryColor())){
                                        if(countyZoneLabel.eventColor.equals(wstmResourceDataObj.getWstmWarningColor())){
                                            currentDisplayLabelObj.displayCoords = countyZoneLabel.displayCoords = pixCoord;
                                            currentDisplayLabelObj.lineWidth = countyZoneLabel.lineWidth;
                                            currentDisplayLabelObj.symbolWidth = countyZoneLabel.symbolWidth;
                                            currentDisplayLabelObj.symbolSize = countyZoneLabel.symbolSize;
                                            currentDisplayLabelObj.symbolTypeStr = EditWstmAttrDialog.warningMarkerData;
                                            currentDisplayLabelObj.eventColor = countyZoneLabel.eventColor;
                                        }
                                        //If it is a watch change it if the incoming product is a warning or advisory
                                    } else {
                                        if(countyZoneLabel.eventColor.equals(wstmResourceDataObj.getWstmWarningColor())){
                                            currentDisplayLabelObj.displayCoords = countyZoneLabel.displayCoords = pixCoord;
                                            currentDisplayLabelObj.lineWidth = countyZoneLabel.lineWidth;
                                            currentDisplayLabelObj.symbolWidth = countyZoneLabel.symbolWidth;
                                            currentDisplayLabelObj.symbolSize = countyZoneLabel.symbolSize;
                                            currentDisplayLabelObj.symbolTypeStr = EditWstmAttrDialog.warningMarkerData;
                                            currentDisplayLabelObj.eventColor = countyZoneLabel.eventColor;
                                        } else if (countyZoneLabel.eventColor.equals(wstmResourceDataObj.getWstmAdvisoryColor())){
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
                            displayLabelMap.put(eachPreProcessDisplayObj.singleFipsCode, currentDisplayLabelObj);
                        }
                    }
                }
                wstmRscAttr = null;
            }

            //RM 5125 loop through display label hashmap and draw labels.

            List<DisplayLabel> outlineWarningList = new ArrayList<DisplayLabel>();
            List<DisplayLabel> outlineAdvisoryList = new ArrayList<DisplayLabel>();
            List<DisplayLabel> outlineWatchList = new ArrayList<DisplayLabel>();

            for (Entry<String, DisplayLabel> entry : displayLabelMap.entrySet()) {
                DisplayLabel currentDisplayLabelObj = entry.getValue();
                currentDisplayLabelObj.fipsCode = entry.getKey();
                String[] textLabel = new String[currentDisplayLabelObj.displayLabel.size()];
                textLabel = currentDisplayLabelObj.displayLabel.toArray(textLabel);
                if (!currentDisplayLabelObj.isTimeEnabled && currentDisplayLabelObj.isZoneNameEnabled){
                    if (currentDisplayLabelObj.displayLabel.size() > 2){
                        for (int i = 2; i <= currentDisplayLabelObj.displayLabel.size() - 1;i++){
                            textLabel[i] = "";
                        }
                    }
                }
                if (currentDisplayLabelObj.isTimeEnabled || currentDisplayLabelObj.isZoneNameEnabled){
                RGB[] color = new RGB[currentDisplayLabelObj.displayColors.size()];
                color = currentDisplayLabelObj.displayColors.toArray(color);
                DrawableString zoneNameString = new DrawableString(
                      textLabel, color);
                zoneNameString.setCoordinates(currentDisplayLabelObj.displayCoords.getX(), currentDisplayLabelObj.displayCoords.getY());
                zoneNameString.textStyle = TextStyle.NORMAL;
                zoneNameString.horizontalAlignment = HorizontalAlignment.LEFT;
                zoneNameString.verticallAlignment = VerticalAlignment.TOP;
                target.drawStrings(zoneNameString);
                }
                if (!wstmResourceDataObj.getOutlineEnable()) {
                    if (currentDisplayLabelObj.markerCoordinate != null) {
                        DisplayElementFactory df = new DisplayElementFactory(
                                target, getNcMapDescriptor());
                        ArrayList<IDisplayable> displayEls = new ArrayList<IDisplayable>(
                                0);
                        Color eventColor = new Color( currentDisplayLabelObj.eventColor.red,
                                currentDisplayLabelObj.eventColor.green,
                                currentDisplayLabelObj.eventColor.blue);
                        Color[] symbolColor = {eventColor};
                        Symbol symbol = new Symbol(
                                null,
                                symbolColor,
                                currentDisplayLabelObj.lineWidth,
                                currentDisplayLabelObj.symbolSize, /*
                                 * scale
                                 * per
                                 * NMAP
                                 */
                                false,
                                currentDisplayLabelObj.markerCoordinate,
                                "Symbol",
                                this.getActualSymbolName(currentDisplayLabelObj.symbolTypeStr));
                        displayEls = df.createDisplayElements(
                                symbol, paintProps);
                        if (displayEls != null
                                && !displayEls.isEmpty()) {
                            for (IDisplayable each : displayEls) {
                                each.draw(target, paintProps);
                                each.dispose();
                            }
                        }
                    }
                } else {
                    //Build lists for outline lists to be plotted separately.
                    if (currentDisplayLabelObj.eventColor.equals(wstmResourceDataObj.getWstmWarningColor())){
                        outlineWarningList.add(currentDisplayLabelObj);
                    } else if(currentDisplayLabelObj.eventColor.equals(wstmResourceDataObj.getWstmAdvisoryColor())){
                        outlineAdvisoryList.add(currentDisplayLabelObj);
                    } else {
                        outlineWatchList.add(currentDisplayLabelObj);
                    
                }
            }
                //if the outline is enabled, create the outline in backward priority order such
                //warning outlines are always on top, followed by advisory, followed by watches.
                if (wstmResourceDataObj.getOutlineEnable()) {
                    for (DisplayLabel watch : outlineWatchList){
                    drawOutlineForZone2(
                            watch.fipsCode, target,
                            watch.eventColor,
                            watch.lineWidth);
                    }
                    for (DisplayLabel advisory : outlineAdvisoryList){
                    drawOutlineForZone2(
                            advisory.fipsCode, target,
                            advisory.eventColor,
                            advisory.lineWidth);
                    }
                    for (DisplayLabel warning : outlineWarningList){
                    drawOutlineForZone2(
                            warning.fipsCode, target,
                            warning.eventColor,
                            warning.lineWidth);
                    }
                }
            }
        }
    }

    

    @Override
    /***
     * Overridden method to process the incoming AWW records
     * @param pdo - the AwwRecord
     * Returns an array of IRscDataObject processed from the AwwRecord
     */
    protected IRscDataObject[] processRecord(Object pdo) {
        if (!(pdo instanceof AwwRecord)) {
            System.out.println("Error: " + "Object is of type "
                    + pdo.getClass().getCanonicalName()
                    + "instead of type AwwRecord");
            return new IRscDataObject[] {};
        }
        AwwRecord awwRecord = (AwwRecord) pdo;

        List<PreProcessDisplay> wstmRscDataObjectList = getWstmData(awwRecord);
        if (wstmRscDataObjectList == null || wstmRscDataObjectList.size() == 0) {
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
            double pointArr[] = new double[] {
                    latLonPt.getLongitude(LatLonPoint.INDEGREES),
                    latLonPt.getLatitude(LatLonPoint.INDEGREES) };
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
            actualSymbolName = "FILLED_TRIANGLE"; // refer symbolPatterns.xml
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
        List<PreProcessDisplay> wstmRscDataObjectList = new ArrayList<PreProcessDisplay>(
                0);
        Set<AwwUgc> thisAwwUgcSet = awwRecord.getAwwUGC();

        for (AwwUgc eachAwwUgc : thisAwwUgcSet) {
            Set<AwwVtec> aSetOfAwwVtec = new HashSet<AwwVtec>(
                    eachAwwUgc.getAwwVtecLine());
            Set<AwwFips> aSetOfAwwFips = new HashSet<AwwFips>(
                    eachAwwUgc.getAwwFIPS());

            for (AwwVtec thisVtec : aSetOfAwwVtec) {
//RM5125 only add objects if they are a member of the winter storm family.
                wstmRscDataObject = new PreProcessDisplay();
                String buildReportType = thisVtec.getPhenomena() + "." + thisVtec.getSignificance();
                if (buildReportType.equalsIgnoreCase("BZ.A")){
                    wstmRscDataObject.reportType = AwwReportType.BLIZZARD_WATCH;
                } else if (buildReportType.equalsIgnoreCase("BZ.W")){
                    wstmRscDataObject.reportType = AwwReportType.BLIZZARD_WARNING;
                } else if (buildReportType.equalsIgnoreCase("IS.W")){
                    wstmRscDataObject.reportType = AwwReportType.ICE_STORM_WARNING; 
                } else if (buildReportType.equalsIgnoreCase("LE.A")){
                    wstmRscDataObject.reportType = AwwReportType.LAKE_EFFECT_SNOW_WATCH;
                } else if (buildReportType.equalsIgnoreCase("LE.W")){
                    wstmRscDataObject.reportType = AwwReportType.LAKE_EFFECT_SNOW_WARNING;
                } else if (buildReportType.equalsIgnoreCase("LE.Y")){
                    wstmRscDataObject.reportType = AwwReportType.LAKE_EFFECT_SNOW_ADVISORY;
                } else if (buildReportType.equalsIgnoreCase("WS.A")){
                    wstmRscDataObject.reportType = AwwReportType.WINTER_STORM_WATCH;
                } else if (buildReportType.equalsIgnoreCase("WS.W")){
                    wstmRscDataObject.reportType = AwwReportType.WINTER_STORM_WARNING;
                } else if (buildReportType.equalsIgnoreCase("WW.Y")){
                    wstmRscDataObject.reportType = AwwReportType.WINTER_WEATHER_ADVISORY;
                } else if (buildReportType.equalsIgnoreCase("ZR.Y")){
                    wstmRscDataObject.reportType = AwwReportType.FREEZING_RAIN_ADVISORY;
                } else {
                    wstmRscDataObject.reportType = null;
                }
                if (wstmRscDataObject.reportType != null){
                /*
                 * (Non-Javadoc) - From each VTEC line in the bulletin retrieve
                 * the following information about the weather hazard:
                 */
                wstmRscDataObject.evSignificance = thisVtec.getSignificance();
                wstmRscDataObject.evPhenomena = thisVtec.getPhenomena();

                wstmRscDataObject.evTrack = thisVtec
                        .getEventTrackingNumber();
                wstmRscDataObject.evOfficeId = thisVtec.getOfficeID();
                wstmRscDataObject.issueTime = new DataTime(awwRecord.getIssueTime());
                wstmRscDataObject.eventType = thisVtec.getAction();
                Calendar startTimeCal = awwRecord.getIssueTime();
                Calendar endTimeCal = thisVtec.getEventEndTime();
                Calendar eventStartTime = thisVtec.getEventStartTime();
                /*
                 * (Non-Javadoc) The startTimeCal will be null if the product is
                 * issued after the event started. In this case, the start time
                 * is set to the issue-time.
                 */

                if (eventStartTime == null) {
                    eventStartTime = awwRecord.getIssueTime();
                }         

                if (startTimeCal != null && endTimeCal != null) {
                    wstmRscDataObject.endTime = new DataTime(endTimeCal);
                    wstmRscDataObject.eventTime = new DataTime(startTimeCal,
                            new TimeRange(startTimeCal, endTimeCal));
                }
                wstmRscDataObject.origStartTime = new DataTime(eventStartTime);
                wstmRscDataObject.origEndTime = wstmRscDataObject.endTime;
                wstmRscDataObject.displayStart = wstmRscDataObject.issueTime;
                wstmRscDataObject.displayEnd = wstmRscDataObject.origEndTime;

                //RM 5125 if zone does not have a record in the zone table do not add it to the list of zones to be added into
                //the list of things to eventually be drawn.
                boolean doAdd = true;
                if (aSetOfAwwFips != null && aSetOfAwwFips.size() > 0) {
                    //wqr.buildQueryPart(aSetOfAwwFips);
                    wstmRscDataObject.fipsCodesList = createListOfFipsInfoObjects2(aSetOfAwwFips);// T456
                    //RM 5125 since we are using a common AWW object this information needs to be 
                    //determined prior to the wqr map being populated.
                    for (int i = 0; i < wstmRscDataObject.fipsCodesList.size();i++){
                        Double fipsLat = getLatitude(wstmRscDataObject.fipsCodesList.get(i));
                        Double fipsLon = getLongitude(wstmRscDataObject.fipsCodesList.get(i));
                        if (!fipsLat.equals(0.0) && !fipsLon.equals(0.0)){
                        wstmRscDataObject.countyZoneLatList.add(fipsLat);
                        wstmRscDataObject.countyZoneLonList.add(fipsLon);
                        }else {
                            doAdd = false;
                        }
                    }
                }
                if (doAdd){
                wstmRscDataObjectList.add(wstmRscDataObject);
                }


            }
            }
        }
        return wstmRscDataObjectList;
    }
    
    //RM 5125 since AWW resources use common data to display county by county, a common
    //object was created thus removing the need to have a separate object for each resource.

    protected class FrameData extends AbstractFrameData {
        HashMap<String, PreProcessDisplay> wstmDataMap;

        /**
         * Overloaded Constructor
         * 
         * @param ftime
         * @param frameInterval
         */
        protected FrameData(DataTime ftime, int frameInterval) {
            super(ftime, frameInterval);
            wstmDataMap = new HashMap<String, PreProcessDisplay>();
        }

        @Override
        /**
         * Updates the <code> Map of WstmRscDataObject </code> in each frame, based on the action type
         * of the incoming <code> WstmRscDataObject </code>
         */
        public boolean updateFrameData(IRscDataObject rscDataObj) {
            if (!(rscDataObj instanceof PreProcessDisplay)) {
                System.out.println("Error: rscDataObj belongs to class"
                        + rscDataObj.getClass().getCanonicalName());
                return false;
            }
             PreProcessDisplay thisWstmRscDataObject = (PreProcessDisplay) rscDataObj;
                String key = thisWstmRscDataObject.evOfficeId + "."
                        + thisWstmRscDataObject.evTrack + "."
                        + thisWstmRscDataObject.evPhenomena + "."
                        + thisWstmRscDataObject.evSignificance + "."
                        + thisWstmRscDataObject.fipsCodesList.get(0) + "."
                        + thisWstmRscDataObject.issueTime + "." + thisWstmRscDataObject.eventType;

                
                if (wstmDataMap.containsKey(key)){
                    String s = "";
                    String r = "";
                    for (int i = 0; i < thisWstmRscDataObject.fipsCodesList.size(); i++) {
                        s = thisWstmRscDataObject.fipsCodesList.get(i);
                        r = r + " " + s;
                    }
                    wstmDataMap.put(thisWstmRscDataObject.evOfficeId + "."
                            + thisWstmRscDataObject.evTrack + "."
                            + thisWstmRscDataObject.evPhenomena + "."
                            + thisWstmRscDataObject.evSignificance + "."
                            + thisWstmRscDataObject.issueTime + "." + thisWstmRscDataObject.eventType + "." + r,
                            thisWstmRscDataObject);
                } else {
                wstmDataMap.put(key, thisWstmRscDataObject);
                }
                
                //RM 5125 changed where display times were being handled to county by county
                //object creation instead of when it went into the wstmDataMap.
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
    
    //RM5125 removed parameterization as it was not needed.
    private class WstmResourceAttributes {
        Boolean eventEnable;

        Integer symbolWidth;

        Float symbolSize;

        Integer lineWidth;

        RGB colorOfEvent;

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

    // ---------------------------------------------------------------T456:

    WstmQueryResult wqr = new WstmQueryResult();

    // for storing result of pre-calculation
    private IWireframeShape outlineShape;

    // for pre-calculate the IWiredframeShape
    private ZoneResultJob zrJob = new ZoneResultJob("");

    // Area change flag
    private boolean areaChangeFlag = false;

    @Override
    public void queryRecords() throws VizException {
        // this method is almost similar to its super class's queryRecords(),
        // may need to be modified later
        // to use the super class's version for the common part
        DbQueryRequest request = new DbQueryRequest();
        HashMap<String, RequestConstraint> queryList = new HashMap<String, RequestConstraint>();
        RequestConstraint pluginName = new RequestConstraint("aww");
        List<Object[]> results = null;
        IDescriptor.FramesInfo frameTimes = this.descriptor.getFramesInfo();
        int numberOfFramesForArray = frameTimes.getFrameCount() - 1;
        Calendar startFrameTime = frameTimes.getFrameTimes()[0].getRefTimeAsCalendar();
        Calendar endFrameTime = frameTimes.getFrameTimes()[numberOfFramesForArray].getRefTimeAsCalendar();
        DataTime queryStartTime = new DataTime(startFrameTime);
        DataTime queryEndTime = new DataTime(endFrameTime);
        String queryString = "select distinct aww.id from aww,aww_ugc,aww_vtec where aww.id = aww_ugc.parentid and aww_ugc.recordid = aww_vtec.parentid and aww_vtec.eventendtime >='" + queryStartTime + "' and aww.reftime <= '" + queryEndTime + "' and aww.reporttype = 'WINTER WEATHER';";
        results = DirectDbQuery.executeQuery(queryString, "metadata", QueryLanguage.SQL);
        Collection<String> id = new ArrayList<String>();
        for (int i = 0; i < results.size(); i++) {
            id.add(results.get(i)[0].toString());
        }
        queryList.put("pluginName", pluginName);
        queryList.put("reportType", resourceData.getMetadataMap().get("reportType"));
        //queryList.put("dataTime.refTime", new RequestConstraint(
          //      queryTime.toString(), ConstraintType.LESS_THAN_EQUALS));
        queryList.put("id", new RequestConstraint(id));
        request.setConstraints(queryList);
        DbQueryResponse response = (DbQueryResponse) ThriftClient
                .sendRequest(request);
        List<Object> pdoList = new ArrayList<Object>();
        for (int i = 0; i < response.getResults().size(); i++) {
            for (Map.Entry<String, Object> entry : response.getResults().get(i)
                    .entrySet()) {
                pdoList.add(entry.getValue());
            }
        }

        class ProcessRecordRunnable implements Runnable {
            //List<IRscDataObject> dataObjs = new ArrayList<IRscDataObject>();
            Object runpdo = null;

            ProcessRecordRunnable(Object pdo) {
                runpdo = pdo;
            }

            public void run() {
                //System.out.println(Thread.currentThread().getName());
                for (IRscDataObject dataObject : processRecord(runpdo)) {
                    newRscDataObjsQueue.add(dataObject);
                    //wqr.buildQueryPart(dataObject);
                }
            }
        }
        ExecutorService pool = Executors.newFixedThreadPool(10);
        for (Object pdo : pdoList) {
            pool.submit(new ProcessRecordRunnable(pdo) );
        }
        try{
            pool.shutdown();
            pool.awaitTermination(60, TimeUnit.SECONDS);
        }catch (InterruptedException e) {
            System.out.println("Thread interrupted.");
        }
        
            for (IRscDataObject dataObject : newRscDataObjsQueue) {
                wqr.buildQueryPart(dataObject);
                
            }

        wqr.populateFipsMap();
        setAllFramesAsPopulated();
    }

    private List<String> createListOfFipsInfoObjects2(
            Set<AwwFips> aSetOfAwwFips) {

        List<String> thisListOfFipsInfo = new ArrayList<String>();

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

        private Map<String, Result> keyResultMap = new java.util.concurrent.ConcurrentHashMap<String, Result>();

        private IGraphicsTarget target;

        private IMapDescriptor descriptor;

        private RGB symbolColor = new RGB(155, 155, 155);

        public class Result {

            public IWireframeShape outlineShape;

            //public Map<Object, RGB> colorMap;

            private Result(IWireframeShape outlineShape,
                    IWireframeShape nuShape, IShadedShape shadedShape,
                    Map<Object, RGB> colorMap) {

                this.outlineShape = outlineShape;

                //this.colorMap = colorMap;
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
            this.run(null);// this.schedule();

        }

        @Override
        protected org.eclipse.core.runtime.IStatus run(
                org.eclipse.core.runtime.IProgressMonitor monitor) {

            for (AbstractFrameData afd : frameDataMap.values()) {

                FrameData fd = (FrameData) afd;

                for (PreProcessDisplay wrdo : fd.wstmDataMap.values()) {

                    for (String fi : wrdo.fipsCodesList) {

                        Collection<Geometry> gw = new ArrayList<Geometry>();

                        for (ArrayList<Object[]> zones : wqr
                                .getZoneResult(fi)) {

                            if (zones == null)
                                continue;

                            WKBReader wkbReader = new WKBReader();

                            for (Object[] result : zones) {

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
                                    System.out
                                            .println("Exception in run(),ZoneResultJob: "
                                                    + e.getMessage());
                                }
                            }
                        }

                        if (gw.size() == 0)
                            continue;
                        else
                            keyResultMap.put(fi, new Result(
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
                System.out
                        .println("_____Exception in getEachWrdoShape(), ZoneResultJob : "
                                + e.getMessage());
            }

            return newOutlineShape;
        }
    }

    private void drawOutlineForZone2(String fipsCode, IGraphicsTarget target,
            RGB lineColor, int lineWidth) throws VizException {

        ZoneResultJob.Result result = zrJob.keyResultMap.get(fipsCode);

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
                System.out
                        .println("Exception in drawCountyOutline2(), WstmResource"
                                + e.getMessage());
                // e.printStackTrace();
            }

        } else if (outlineShape == null) {

            // target.setNeedsRefresh(true);
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
        // long dataTimeMs =
        // rscDataObj.getDataTime().getValidTime().getTime().getTime();
        if (rscDataObj == null)
            return 0;

        java.util.Calendar validTimeInCalendar = null;
        DataTime dataTime = rscDataObj.getDataTime();
        if (dataTime != null) {
            validTimeInCalendar = dataTime.getValidTime();

        } else {
            System.out
                    .println("===== find IRscDataObject rscDataObj.getDataTime() return NULL!!!");
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
                || fd.wstmDataMap.size() == 0) {
            return legendString + "-No Data";
        }
        return legendString + " "
                + NmapCommon.getTimeStringFromDataTime(fd.getFrameTime(), "/");
    }
    
    public double getLatitude(String zoneFips) {
        List<Object[]> results = null;
        Logger logger = Logger.getLogger("gov.noaa.nws.ncep.viz.rsc.wstm.rsc.WstmResource");
        String queryPrefix = "select lat from mapdata.zone where state_zone =";
        String dbZoneFips = zoneFips.substring(0, 2) + zoneFips.substring(3);
        try{
            String wholeQuery = queryPrefix + "'" + dbZoneFips + "'" + ";";
            
            results = DirectDbQuery.executeQuery(wholeQuery, "maps", QueryLanguage.SQL);
            Double zoneFipsLat = ((Number) results.get(0)[0]).doubleValue();
            return zoneFipsLat;
        }catch(Exception e){ 
            //if there is a problem with the query make a note in the the console log and skip it.
            logger.log(Level.WARNING, "Latitude information for " + zoneFips + " was not found in the zone table. This zone will be skipped.");
            return 0;
        }
    }
    public double getLongitude(String zoneFips) {
        List<Object[]> results = null;
        Logger logger = Logger.getLogger("gov.noaa.nws.ncep.viz.rsc.wstm.rsc.WstmResource");
        String queryPrefix = "select lon from mapdata.zone where state_zone =";
        String dbZoneFips = zoneFips.substring(0, 2) + zoneFips.substring(3);
        try{
            String wholeQuery = queryPrefix + "'" + dbZoneFips + "'" + ";";
            
            results = DirectDbQuery.executeQuery(wholeQuery, "maps", QueryLanguage.SQL);
            double zoneFipsLon = ((Number) results.get(0)[0]).doubleValue();
            return zoneFipsLon;
        }catch(Exception e){         
            //if there is a problem with the query make a note in the the console log and skip it.
            logger.log(Level.WARNING, "Longitude information for " + zoneFips + " was not found in the zone table. This zone will be skipped.");
            return 0;
        }
    }
    
    //RM 5125 Added display label class to pass from the PreProcessDisplay label map to the display label logic.
    public class DisplayLabel{
        List<String> displayLabel;
        List<RGB> displayColors;
        PixelCoordinate displayCoords; 
        List<String> evSignificance;
        boolean isZoneNameEnabled;
        boolean isTimeEnabled;
        int lineWidth;
        RGB eventColor;
        Coordinate markerCoordinate;
        Integer symbolWidth;
        Float symbolSize;
        String symbolTypeStr;
        String fipsCode;
        LatLonPoint zoneLatLon;
    }
}
