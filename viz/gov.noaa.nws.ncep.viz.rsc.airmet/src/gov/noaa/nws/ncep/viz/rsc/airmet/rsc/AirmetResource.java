package gov.noaa.nws.ncep.viz.rsc.airmet.rsc;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.RGB;
import org.locationtech.jts.geom.Coordinate;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.time.TimeRange;
import com.raytheon.uf.edex.decodertools.core.LatLonPoint;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.HorizontalAlignment;
import com.raytheon.uf.viz.core.IGraphicsTarget.TextStyle;
import com.raytheon.uf.viz.core.IGraphicsTarget.VerticalAlignment;
import com.raytheon.uf.viz.core.drawables.IFont;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.geom.PixelCoordinate;
import com.raytheon.uf.viz.core.rsc.LoadProperties;

import gov.noaa.nws.ncep.common.dataplugin.airmet.AirmetLocation;
import gov.noaa.nws.ncep.common.dataplugin.airmet.AirmetRecord;
import gov.noaa.nws.ncep.common.dataplugin.airmet.AirmetReport;
import gov.noaa.nws.ncep.ui.pgen.display.DisplayElementFactory;
import gov.noaa.nws.ncep.ui.pgen.display.IDisplayable;
import gov.noaa.nws.ncep.ui.pgen.elements.Symbol;
import gov.noaa.nws.ncep.viz.common.ui.NmapCommon;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResource;
import gov.noaa.nws.ncep.viz.ui.display.NCMapDescriptor;

/**
 * AirmetResource - Display AIRMET data.
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- ------------------------------------------
 * Apr 22, 2010  245      Greg Hull   Initial creation.
 * Sep 30, 2010  307      Greg Hull   AirmetRscDataObject implements
 *                                    IRscDataObject
 * Aug 05, 2011  n/a      B. Hebbard  In preProcessFrameUpdate(), preserve
 *                                    newRscDataObjsQueue
 * May 23, 2012  785      Q. Zhou     Added getName for legend.
 * Aug 17, 2012  655      B. Hebbard  Added paintProps as parameter to
 *                                    IDisplayable draw
 * Nov 05, 2015  5070     randerso    Adjust font sizes for dpi scaling
 * Jul 15, 2020  8191     randerso    Updated for changes to LatLonPoint
 *
 * </pre>
 *
 * @author ghull
 */
public class AirmetResource
        extends AbstractNatlCntrsResource<AirmetResourceData, NCMapDescriptor>
        implements INatlCntrsResource {
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(AirmetResource.class);

    private AirmetResourceData airmetResourceData;

    private class AirmetRscDataObject implements IRscDataObject {
        private HazardType hazardType = HazardType.UNKNOWN;

        private String sequenceID = "";

        /** has a validPeriod for the start/end times */
        private DataTime eventTime;

        /** the end of dataTime's validPeriod */
        private DataTime endTime;

        private String flightLevel1Str;

        private String flightLevel2Str;

        private Integer updateNum;

        private LatLonPoint[] points;

        // assume only the first level can be freezing.
        public boolean isFreezingLevel() {
            return flightLevel1Str != null && "FRZLVL".equals(flightLevel1Str);
        }

        public Integer getFlightLevel1() {
            if (flightLevel1Str == null) {
                return Integer.MIN_VALUE;
            } else if ("FRZLVL".equals(flightLevel1Str)) {
                return null;
            } else {
                return Integer.parseInt(flightLevel1Str);
            }
        }

        public Integer getFlightLevel2() {
            if (flightLevel2Str == null) {
                return Integer.MAX_VALUE;
            }
            return Integer.parseInt(flightLevel2Str);
        }

        @Override
        public DataTime getDataTime() {
            return eventTime;
        }
    }

    private enum HazardType {
        IFR,
        MNT_OBSCUR,
        TURB,
        ICING,
        SUST_WINDS,
        LOW_LVL_WIND_SHEAR,
        OUTLOOK,
        UNKNOWN
    }

    // A map from an identifier string (which is unique within a single frame
    // to a structure for a single displayable element
    private class FrameData extends AbstractFrameData {
        HashMap<String, AirmetRscDataObject> airmetDataMap;

        public FrameData(DataTime frameTime, int timeInt) {
            super(frameTime, timeInt);
            airmetDataMap = new HashMap<>();
        }

        // put the AirmetDataObject in the map
        @Override
        public boolean updateFrameData(IRscDataObject rscDataObj) {
            AirmetRscDataObject airmetData = (AirmetRscDataObject) rscDataObj;

            // Since the SeqID is not unique, we will need to append the hazard
            // type to create a key
            String keyStr = airmetData.sequenceID
                    + airmetData.hazardType.toString();
            AirmetRscDataObject existingAirmetDataObject = airmetDataMap
                    .get(keyStr);

            // If this airmet hasn't been processed yet or if it is an updated
            // issue
            if (existingAirmetDataObject == null
                    || airmetData.updateNum > existingAirmetDataObject.updateNum) {
//              airmetData.issueTime.greaterThan( existingAirmetDataObject.issueTime) ) {

//              if( existingAirmetDataObject != null ) {
//                  statusHandler.debug("Updating "+ keyStr +"from "+ existingAirmetDataObject.updateNum +" to " +
//                          airmetData.updateNum + " for time "+frameTime.toString() );
//              }
                airmetDataMap.put(keyStr, airmetData);
            }

            return true;
        }
    }

    private Map<String, DataTime> canceledAirmets;

    private IFont font = null;

    private double charHeight = 10;

    private double charWidth = 3;

    private float baseFontSize = 11.8f;

    private IFont symbolFonts[];

    public AirmetResource(AirmetResourceData resourceData,
            LoadProperties loadProperties) throws VizException {
        super(resourceData, loadProperties);
        airmetResourceData = resourceData;
        canceledAirmets = new HashMap<>();
    }

    @Override
    protected AbstractFrameData createNewFrame(DataTime frameTime,
            int timeInt) {
        return new FrameData(frameTime, timeInt);
    }

    @Override
    public void initResource(IGraphicsTarget grphTarget) throws VizException {
        queryRecords();
    }

    // this overrides the default method which just adds the pluginDataRecords
    // to the queue.
    // Here we need to store the airmet reports.
    //
    @Override
    protected IRscDataObject[] processRecord(Object recordObject) {

        if (!(recordObject instanceof AirmetRecord)) {
            statusHandler.debug(
                    "AirmetResource.processRecord: object must be an AirmetRecord???");
            return new IRscDataObject[] {};
        }

        // loop thru the airmet Reports in this record
        AirmetRecord amRec = (AirmetRecord) recordObject;
        DataTime amTime = amRec.getDataTime();

        // The correction flag is :
        // 0 - Normal, 1 - correction, 2-Amendment, 3 - Test, 4 - Nil(?)
        if (amRec.getCorrectionFlag() == 3 || amRec.getCorrectionFlag() == 4) {
            return new IRscDataObject[] {};
        }

        List<AirmetRscDataObject> airmetDataList = new ArrayList<>();

        // This method is really just intended to convert a pluginDataObject to
        // data objects to be added to the frameData, but in this case we're
        // also
        // going to save off the cancellation notices since its just easier to
        // do here
        //
        for (AirmetReport aRep : amRec.getAirmetReport()) {
            // if this is a cancel notification, save it otherwise
            // loop thru all of the frames and add it to all that apply.
            if (aRep.getCancelFlag() == 1) {
                String keyStr = aRep.getSequenceID()
                        + aRep.getHazardType().toString();
                if (canceledAirmets.get(keyStr) != null) {
                    canceledAirmets.put(keyStr,
                            new DataTime(aRep.getEndTime()));
                }
            } else if ("OUTLOOK".equals(aRep.getReportIndicator())) {
                // not processing OUTLOOKS....
            } else {
                AirmetRscDataObject airmetData = getAirmetRscDataObject(aRep,
                        amRec);

                if (airmetData != null) {
                    airmetDataList.add(airmetData);
                }
            }
        }

        return airmetDataList.toArray(new AirmetRscDataObject[0]);
    }

    @Override
    public void paintFrame(AbstractFrameData frameData, IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {

        if (paintProps == null || frameData == null) {
            return;
        }
        // Allocate font and calculate vertical offset parameter for lines of
        // text
        if (font == null) {
            font = target.initializeFont("Monospace", baseFontSize,
                    new IFont.Style[] { IFont.Style.BOLD });
            Rectangle2D charSize = target.getStringBounds(font, "N");
            charHeight = charSize.getHeight();
            charWidth = charSize.getWidth();
        }

        double screenToWorldRatio = paintProps.getCanvasBounds().width
                / paintProps.getView().getExtent().getWidth();
        double charPixHeight = charHeight / screenToWorldRatio;
        double charPixWidth = charWidth / screenToWorldRatio;

        // Loop through the airmet data and draw the airmet data
        Collection<AirmetRscDataObject> airmetDataSet = ((FrameData) frameData).airmetDataMap
                .values();

        for (AirmetRscDataObject airmetData : airmetDataSet) {

            boolean enabled = false;
            RGB color = new RGB(155, 155, 155);
            int lineWidth = 2;
            int symbolWidth = 2;
            int symbolSize = 2;
            String symbolType = "ASTERISK";

            /*
             * if true the symbolType will be drawn as a Text string instead of
             * a symbol.
             */
            boolean symIsText = false;

            // Set class-dependent attributes, where applicable
            switch (airmetData.hazardType) {
            case IFR:
                enabled = airmetResourceData.getIfrEnable();
                color = airmetResourceData.getIfrColor();
                lineWidth = airmetResourceData.getIfrLineWidth();
                symbolSize = airmetResourceData.getIfrSymbolSize();
                symbolType = "IFR";
                symIsText = true;
                break;
            case MNT_OBSCUR:
                enabled = airmetResourceData.getMntObscEnable();
                color = airmetResourceData.getMntObscColor();
                lineWidth = airmetResourceData.getMntObscLineWidth();
//                  symbolWidth = airmetResourceData.getMntObscSymbolWidth();
                symbolSize = airmetResourceData.getMntObscSymbolSize();
                symbolType = "MTOS";
                symIsText = true;
                break;
            case TURB:
                enabled = airmetResourceData.getTurbEnable();
                // sanity check ; shouldn't be freezing level
                if (!airmetData.isFreezingLevel()
                        && airmetData.getFlightLevel1() < airmetResourceData
                                .getTurbLevel()) {
                    color = airmetResourceData.getTurbColor1();
                } else {
                    color = airmetResourceData.getTurbColor2();
                }
                lineWidth = airmetResourceData.getTurbLineWidth();
                symbolWidth = airmetResourceData.getTurbSymbolWidth();
                symbolSize = airmetResourceData.getTurbSymbolSize();
                symbolType = "TURBULENCE_4";
                break;
            case ICING:
                enabled = airmetResourceData.getIcingEnable();
                color = airmetResourceData.getIcingColor();
                lineWidth = airmetResourceData.getIcingLineWidth();
                symbolWidth = airmetResourceData.getIcingSymbolWidth();
                symbolSize = airmetResourceData.getIcingSymbolSize();
                symbolType = "ICING_08";
                break;
            case SUST_WINDS:
                enabled = airmetResourceData.getSustWindsEnable();
                color = airmetResourceData.getSustWindsColor();
                lineWidth = airmetResourceData.getSustWindsLineWidth();
                symbolWidth = airmetResourceData.getSustWindsSymbolWidth();
                symbolSize = airmetResourceData.getSustWindsSymbolSize();
                symbolType = "30_KT_BARB";
                break;
            case LOW_LVL_WIND_SHEAR:
                enabled = airmetResourceData.getLowLevelWindShearEnable();
                color = airmetResourceData.getLowLevelWindShearColor();
                lineWidth = airmetResourceData.getLowLevelWindShearLineWidth();
//                  symbolWidth = airmetResourceData.getLowLevelWindShearSymbolWidth();
                symbolSize = airmetResourceData
                        .getLowLevelWindShearSymbolSize();
                symbolType = "LLWS";
                symIsText = true;
                break;
            case UNKNOWN:
            default:
            }

            // If there are upper and/or lower filters we will not show airmets
            // with a flight level 1
            // outside of the filter range
            if (enabled) {
                // if there is a non-freezing flight level 1
                if (airmetData.flightLevel1Str != null
                        && !airmetData.isFreezingLevel()) {

                    if (airmetResourceData.getLowerFilterLevel() > 0
                            && airmetData.getFlightLevel1() < airmetResourceData
                                    .getLowerFilterLevel()) {
                        enabled = false;
                    }
                    if (airmetResourceData.getUpperFilterLevel() > 0
                            && airmetData.getFlightLevel1() > airmetResourceData
                                    .getUpperFilterLevel()) {
                        enabled = false;
                    }
                }
            }

            if (enabled) {
                PixelCoordinate prevLoc = null;
                PixelCoordinate labelCoord = null;

                for (LatLonPoint currentPoint : airmetData.points) {
                    if (currentPoint == null) {
                        continue;
                    }

                    double[] latLon = { currentPoint.getLongitude(),
                            currentPoint.getLatitude() };
                    PixelCoordinate currLoc = new PixelCoordinate(
                            descriptor.worldToPixel(latLon));

                    // skip first location
                    if (prevLoc != null) {
                        target.drawLine(prevLoc.getX(), prevLoc.getY(),
                                prevLoc.getZ(), currLoc.getX(), currLoc.getY(),
                                currLoc.getZ(), color, lineWidth);
                    }

                    if (labelCoord == null
                            || labelCoord.getY() < currLoc.getY()) {
                        labelCoord = currLoc;
                    }
                    prevLoc = currLoc;
                }

                PixelCoordinate currLblCoord = new PixelCoordinate(labelCoord);

                // draw the seqId if enabled
                if (airmetResourceData.getSeqIdEnable()) {
                    target.drawString(font, airmetData.sequenceID,
                            currLblCoord.getX() - charPixWidth / 2
                                    * airmetData.sequenceID.length(),
                            currLblCoord.getY(), 0.0, TextStyle.NORMAL, color,
                            HorizontalAlignment.LEFT, VerticalAlignment.TOP,
                            0.0);
                    currLblCoord.addToY(charPixHeight);
                }

                // if drawing the symbol as a text string
                if (airmetResourceData.symbolEnable && symbolType != null) {
                    if (symIsText) {
                        // float symFontSize = getSymbolFontSize( symbolSize,
                        // target );

                        /*
                         * Allocate font and calculate vertical offset parameter
                         * for lines of text
                         */
                        IFont symFont = getSymbolFontSize(symbolSize, target);
                        Rectangle2D symCharSize = target
                                .getStringBounds(symFont, "N");

                        // currLblCoord.addToY(
                        // symCharSize.getHeight()/screenToWorldRatio);
                        currLblCoord.addToX(-.5 * symbolType.length()
                                * symCharSize.getWidth() / screenToWorldRatio);

                        target.drawString(symFont, symbolType,
                                currLblCoord.getX(), currLblCoord.getY(), 0.0,
                                TextStyle.NORMAL, color,
                                HorizontalAlignment.LEFT, VerticalAlignment.TOP,
                                0.0);
                        // currLblCoord.addToY( charPixHeight*.25 );
                    } else {
                        // approx symbol size
                        currLblCoord.addToY(charPixHeight);

                        /*
                         * draw the symbol if it is enabled and update the coord
                         * for the labels
                         */
                        Color[] colors = new Color[] {
                                new Color(color.red, color.green, color.blue) };
                        /*
                         * Compute offset center of symbol in pixel coordinates,
                         * based on number of text lines so far...
                         */
                        double[] pixelSymbolLoc = { currLblCoord.getX(),
                                currLblCoord.getY() };
                        /*
                         * ...and convert back to world coordinates, as
                         * Coordinate, for Symbol()
                         */
                        double[] worldSymbolLoc = descriptor
                                .pixelToWorld(pixelSymbolLoc);
                        Coordinate symCoords = new Coordinate(worldSymbolLoc[0],
                                worldSymbolLoc[1]);
                        String category = new String("Symbol");
                        Symbol symbol = new Symbol(null, // unused
                                colors, symbolWidth, symbolSize * 0.60, // scale
                                                                        // per
                                                                        // NMAP
                                false, // clear
                                symCoords, category, symbolType);
                        DisplayElementFactory df = new DisplayElementFactory(
                                target, getNcMapDescriptor());
                        List<IDisplayable> displayEls = df
                                .createDisplayElements(symbol, paintProps);
                        for (IDisplayable each : displayEls) {
                            each.draw(target, paintProps);
                            each.dispose();
                        }
                    }
                }

                currLblCoord = new PixelCoordinate(labelCoord);
                currLblCoord.addToY(charPixHeight * 2.5);

                if (airmetResourceData.timeEnable) {
                    String endTimeS = airmetData.endTime.toString();
                    String timeLblStr = endTimeS.substring(11, 13) + // hour
                            endTimeS.substring(14, 16); // minute
                    target.drawString(font, timeLblStr, currLblCoord.getX(),
                            currLblCoord.getY(), 0.0, TextStyle.NORMAL, color,
                            HorizontalAlignment.LEFT, VerticalAlignment.TOP,
                            0.0);
                }

                currLblCoord = labelCoord;
                currLblCoord.addToX(charPixWidth * 3);

                if (airmetResourceData.flightLevelEnable) {
                    if (airmetData.flightLevel1Str != null) {
                        target.drawString(font, airmetData.flightLevel1Str,
                                currLblCoord.getX(), currLblCoord.getY(), 0.0,
                                TextStyle.NORMAL, color,
                                HorizontalAlignment.LEFT, VerticalAlignment.TOP,
                                0.0);
                    }

                    currLblCoord.addToY(charPixHeight * 1.5);

                    if (airmetData.flightLevel2Str != null) {
                        target.drawString(font, airmetData.flightLevel2Str,
                                currLblCoord.getX(), currLblCoord.getY(), 0.0,
                                TextStyle.NORMAL, color,
                                HorizontalAlignment.LEFT, VerticalAlignment.TOP,
                                0.0);

                    }

                }
            }
        }
    }

    // get just the data from the AirmetReport that we will need to display the
    // resource
    private AirmetRscDataObject getAirmetRscDataObject(
            AirmetReport airmetSection, AirmetRecord airmetRec) {

//      if (airmetSection.getSequenceID() == null)
//          return null;
        AirmetRscDataObject airmetData = new AirmetRscDataObject();

        try {
            // TODO: consider using a map
            switch (airmetSection.getHazardType()) {
            case "INSTRUMENT FLIGHT RULES":
                airmetData.hazardType = HazardType.IFR;
                break;
            case "MOUNTAIN OBSCURATION":
                airmetData.hazardType = HazardType.MNT_OBSCUR;
                break;
            case "TURBULENCE":
                airmetData.hazardType = HazardType.TURB;
                break;
            case "ICING":
                airmetData.hazardType = HazardType.ICING;
                break;
            case "SUSTAINED SFC WINDS":
                airmetData.hazardType = HazardType.SUST_WINDS;
                break;
            case "LOW  asfasdfa": // TODO: what is this?
                airmetData.hazardType = HazardType.LOW_LVL_WIND_SHEAR;
                break;
            default:
                statusHandler.debug("Unrecognized AIRMET Hazard Type "
                        + airmetSection.getHazardType());
                airmetData.hazardType = HazardType.UNKNOWN;
            }

            airmetData.sequenceID = airmetSection.getSequenceID();
            airmetData.eventTime = new DataTime(airmetSection.getStartTime(),
                    new TimeRange(airmetSection.getStartTime(),
                            airmetSection.getEndTime()));

            airmetData.endTime = new DataTime(airmetSection.getEndTime());

            airmetData.flightLevel1Str = airmetSection.getFlightLevel1();
            airmetData.flightLevel2Str = airmetSection.getFlightLevel2();

//          airmetData.issueTime    = new DataTime( airmetRec.getIssueTime() );
            airmetData.updateNum = airmetRec.getUpdateNumber();
        } catch (NumberFormatException e) {
            statusHandler.debug("Error parsing flight level: "
                    + airmetSection.getFlightLevel1() + " or "
                    + airmetSection.getFlightLevel2(), e);
            // TODO : Are these reasonable defaults?
//          airmetData.flightLevel1 = "0";
//          airmetData.flightLevel2 = "500;
        } catch (IllegalArgumentException e) {
            airmetData.hazardType = HazardType.UNKNOWN;
            statusHandler.debug(e.getMessage(), e);
        }

        // get the lat/lon points
        int numPoints = airmetSection.getAirmetLocation().size();

        if (numPoints > 1) {
            airmetData.points = new LatLonPoint[numPoints];
            for (AirmetLocation airmetLocation : airmetSection
                    .getAirmetLocation()) {
                LatLonPoint point = new LatLonPoint(
                        airmetLocation.getLatitude(),
                        airmetLocation.getLongitude());
                int index = airmetLocation.getIndex() - 1;
                if (airmetData.points[index] != null) {
                    statusHandler.debug(
                            "Error reading Airmet Locations: duplicate Index found??");
                    return null;
                }
                airmetData.points[index] = point;
            }
        } else if (numPoints == 1) {
            statusHandler.debug("Error Airmet only has 1 Location");
        }

        return airmetData;
    }

    // some 'symbols' are really strings so get the appropriate font size for
    // the symbol size.
    // a symbol size of 2 is 14
    private IFont getSymbolFontSize(int symSize, IGraphicsTarget grphTarget) {
        if (symbolFonts == null) {
            // allow 10 symbol sizes
            symbolFonts = new IFont[11];
        }
        if (symSize >= symbolFonts.length) {
            symSize = symbolFonts.length - 1;
        }

        // final float symSizeMapping[] = { 0.8f, 6.8f, 11.8f, 12.7f, 13.5f,
        // 14.4f, 15.2f, 16.1f, 16.9f, 17.8f, 18.6f };

        if (symbolFonts[symSize] == null) {
//          float fontSize = symSizeMapping[symSize];
            float fontSize = (symSize == 1 ? 6.8f
                    : (symSize == 2 ? 11.7f : symSize + 11.8f));

            symbolFonts[symSize] = grphTarget.initializeFont("Monospace",
                    fontSize, new IFont.Style[] { IFont.Style.BOLD });
        }
        return symbolFonts[symSize];
    }

    // This method is called after the AirmetRscDataObject have been added
    // to the newRscDataObjsList queue and before these objects are timeMatched
    // to
    // the frames. processRecords has saved off the cancellations into
    // the canceledAirmets. We will use this list to correct or cancel the times
    // in the
    // newRscDataObjsList so the time matching will be correct.
    //
    @Override
    protected boolean preProcessFrameUpdate() {

        for (IRscDataObject rscDataObj : newRscDataObjsQueue) {

            AirmetRscDataObject airmetRscDataObj = (AirmetRscDataObject) rscDataObj;

            String keyStr = airmetRscDataObj.sequenceID
                    + airmetRscDataObj.hazardType.toString();

            // if this airmet has been cancelled then update the end time
            if (canceledAirmets.containsKey(keyStr)) {
                DataTime cancelTime = canceledAirmets.get(keyStr);

                if (cancelTime != null
                        && !airmetRscDataObj.endTime.greaterThan(cancelTime)) {

                    airmetRscDataObj.endTime = cancelTime;
                    Calendar startTime = airmetRscDataObj.getDataTime()
                            .getRefTimeAsCalendar();
                    airmetRscDataObj.eventTime = new DataTime(startTime,
                            new TimeRange(startTime,
                                    cancelTime.getRefTimeAsCalendar()));
                    statusHandler
                            .debug("Airmet " + keyStr + " has been canceled.");
                }
            }
        }

        canceledAirmets.clear();

        return true;
    }

    @Override
    public void disposeInternal() {
        super.disposeInternal();
        if (font != null) {
            font.dispose();
            font = null;
        }
        if (symbolFonts != null) {
            for (IFont symFont : symbolFonts) {
                if (symFont != null) {
                    symFont.dispose();
                    symFont = null;
                }
            }
            symbolFonts = null;
        }
    }

    @Override
    public void resourceAttrsModified() {
        // don't need to do anything
    }

    @Override
    public String getName() {
        String legendString = super.getName();
        FrameData fd = (FrameData) getCurrentFrame();
        if (fd == null || fd.getFrameTime() == null
                || fd.airmetDataMap.size() == 0) {
            return legendString + "-No Data";
        }
        return legendString + " "
                + NmapCommon.getTimeStringFromDataTime(fd.getFrameTime(), "/");
    }
}