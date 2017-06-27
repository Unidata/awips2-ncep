package gov.noaa.nws.ncep.viz.rsc.asdi.rsc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.eclipse.swt.graphics.RGB;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.edex.decodertools.core.LatLonPoint;
import com.raytheon.uf.viz.core.DrawableLine;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.LineStyle;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.geom.PixelCoordinate;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.vividsolutions.jts.geom.Coordinate;

import gov.noaa.nws.ncep.common.dataplugin.asdi.AsdiRecord;
import gov.noaa.nws.ncep.ui.pgen.gfa.GfaSnap;
import gov.noaa.nws.ncep.viz.common.display.INatlCntrsDescriptor;
import gov.noaa.nws.ncep.viz.resources.AbstractFrameData;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource2;
import gov.noaa.nws.ncep.viz.resources.IDataLoader;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.IRscDataObject;
import gov.noaa.nws.ncep.viz.resources.attributes.ResourceAttrSet;
import gov.noaa.nws.ncep.viz.resources.attributes.ResourceAttrSet.RscAttrValue;
import gov.noaa.nws.ncep.viz.resources.colorBar.ColorBarResource;
import gov.noaa.nws.ncep.viz.resources.colorBar.ColorBarResourceData;
import gov.noaa.nws.ncep.viz.ui.display.ColorBar;
import gov.noaa.nws.ncep.viz.ui.display.NCMapDescriptor;

/**
 * Display Aircraft Situational Display to Industry (ASDI) flight data.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer     Description
 * ------------ ---------- -----------  --------------------------
 * 04/05/2017   R28579     RCReynolds   ASDI
 * 
 * </pre>
 * 
 * @author RCReynolds
 * @version 1.0
 */
public class AsdiResource
        extends AbstractNatlCntrsResource2<AsdiResourceData, NCMapDescriptor>
        implements INatlCntrsResource, IDataLoader {

    private final static String FLIGHT_DEPARTURE_CHARACTER = "D";

    private final static String FLIGHT_ARRIVAL_CHARACTER = "A";

    private final static String FLIGHT_ARRIVAL_OR_DEPARTURE_CHARACTER = "B";

    String[] userSelectedAirportNames = null;

    private int departArrive = 0;

    private int timeLimitValue = 0;

    private ColorBar colorBar = null;

    private ResourcePair colorbarResourcePair = null;

    private AsdiResourceData asdiResourceData;

    protected AsdiDataLoader dataLoader;

    // Two color bars are used in this resource; one for aircraft height and the
    // other for EDR values

    // aircraft height colorbar
    private ColorBarResource heightColorBarResource = null;;

    private ResourcePair heightColorbarResourcePair = null;

    private ColorBarResource edrColorBarResource = null;;

    private ResourcePair edrColorbarResourcePair = null;

    protected AsdiResource(AsdiResourceData resourceData,
            LoadProperties props) {
        super(resourceData, props);
        asdiResourceData = (AsdiResourceData) resourceData;
    }

    /*
     * Holds previous pixel coordinates for flights. Used to keep track of the
     * last pixel coordinate used to draw flight path line in previous frame.
     */
    private Map<String, PixelCoordinate> prevLocForFlight = new HashMap<>();

    /**
     * Wrapper for ASDI PDO objects
     * 
     */
    public static class AsdiRscDataObject implements IRscDataObject {

        private AsdiRecord asdiRecord;

        public AsdiRscDataObject(PluginDataObject pdo) {
            asdiRecord = (AsdiRecord) pdo;
        }

        public AsdiRecord getAsdiRecord() {
            return asdiRecord;
        }

        @Override
        public DataTime getDataTime() {
            return asdiRecord.getDataTime();
        }

    }

    private class FrameData extends AbstractFrameData {

        private List<PixelCoordinate> flightBeginEndCoordinates = new ArrayList<>();

        // map of a flight number to its ASDI records
        private Map<String, List<AsdiRecord>> flightData = new HashMap<>();

        // Flag to indicate frame drawables need to be regenerated
        private boolean needToRegenerateDrawables = false;

        private Deque<DrawableLine> drawableLines = new ArrayDeque<>();

        public FrameData(DataTime frameTime, int timeInt) {
            super(frameTime, timeInt, asdiResourceData);
        }

        /**
         * Called when graphics need to be refreshed
         * 
         * @param target
         * @throws VizException
         */
        public void paint(IGraphicsTarget target) throws VizException {

            // if parameters have been changed via the Attributes Editor then
            // clear all plottable lists
            if (asdiResourceData.getIsEdited()) {
                asdiResourceData.setIsEdited(false);
                initResource(target);
            }

            // set in initResource, needToRegenerateDrawables will flag if
            // drawable lines need to be recalculated
            if (needToRegenerateDrawables) {
                generateDrawables(target);
                needToRegenerateDrawables = false;
            }

            paintDrawables(target);

        }

        /**
         * does the drawing to graphics target
         * 
         * @param target
         *            - IGraphicsTarget Base class for accessing all the drawing
         *            functionality available for displaying things on a
         *            renderable display.
         * @throws VizException
         */
        private void paintDrawables(IGraphicsTarget target)
                throws VizException {

            // Draw lines

            for (DrawableLine dl : drawableLines) {
                target.drawLine(dl);
            }

            // plot plus-sign at the beginning and end of a flight.
            if (this.flightBeginEndCoordinates.size() > 0) {
                for (int coord = 0; coord < flightBeginEndCoordinates
                        .size(); coord++) {
                    PixelCoordinate pc = flightBeginEndCoordinates.get(coord);
                    target.drawPoint(pc.getX(), pc.getY(), 0.,
                            new RGB(255, 255, 255),
                            IGraphicsTarget.PointStyle.CROSS);
                }
            }

        }

        /**
         * find all ASDI records timeLimitValue earlier than frame time.
         * 
         * @param records
         *            - AsdiRecord is the Data Access component for Aircraft
         *            Situational Display to Industry (ASDI) data.
         * @param frameTimeMilli
         *            - The current frame time in milliseconds
         * @param timeLimitValue
         *            - Can be between 2-30 minutes. Is how far back in time to
         *            include records. This determines how long the comet-like
         *            tail behind the aircraft will appear on the plot.
         * @return
         */
        private List<AsdiRecord> getLaggedRecords(List<AsdiRecord> records,
                long frameTimeMilli, long timeLimitValue) {

            long tailMilliSeconds = 1000 * 60 * (timeLimitValue + 1);

            List<AsdiRecord> laggedRecords = new ArrayList<>();

            for (AsdiRecord record : records) {
                if ((frameTimeMilli
                        - record.getDataTime().getRefTimeAsCalendar()
                                .getTimeInMillis()) <= tailMilliSeconds) {
                    laggedRecords.add(record);
                }
            }
            /*
             * Sort by time which establishes the drawing order
             */

            laggedRecords.sort(
                    (o1, o2) -> o1.getDataTime().compareTo(o2.getDataTime()));

            return laggedRecords;
        }

        /**
         * build list of plottable lines. This method is only called when the
         * list of lines needs to change; as in the user selecting new data or
         * changing parameter values(ie wanting to plot departure airports
         * instead of arrivals).
         * 
         * @param target
         *            - IGraphicsTarget Base class for accessing all the drawing
         *            functionality available for displaying things on a
         *            renderable display.
         * @throws VizException
         */
        private void generateDrawables(IGraphicsTarget target)
                throws VizException {

            Long frameTimeMilli = currFrameTime.getRefTimeAsCalendar()
                    .getTimeInMillis();

            // can be a 0 (plot by height) or 1 (plot by time)
            int aircraftAltitudeOrTime = 0;

            LineStyle lineStyle = LineStyle.SOLID;

            float trackWidth = 0.0f;

            Map<String, List<AsdiRecord>> flightData = ((FrameData) getFrame(
                    currFrameTime)).getFlightData();

            for (String flightId : flightData.keySet()) {

                PixelCoordinate prevLoc = null;
                List<AsdiRecord> records = flightData.get(flightId);

                records = getLaggedRecords(records, frameTimeMilli,
                        timeLimitValue);

                if (records.size() == 0)
                    continue;

                if (lineLengthIsReasonable(records)) {

                    PixelCoordinate currLoc = null;

                    if (drawableLines == null) {
                        drawableLines = new ArrayDeque<>();
                    }

                    float lat = records.get(0).getLatitude();
                    float lon = records.get(0).getLongitude();

                    prevLoc = convertFloatToPixelCoordinate(lat, lon);

                    // All flights begin and end with a single ASDI Record. If
                    // thats' the situation then plot a plus sign at those
                    // flight coordinates.
                    if (records.size() == 1) {
                        flightBeginEndCoordinates.add(prevLoc);
                    }

                    for (int recCount = 0; recCount < records
                            .size(); recCount++) {

                        AsdiRecord record = records.get(recCount);

                        lat = record.getLatitude();
                        lon = record.getLongitude();

                        if (asdiResourceData.plotByHeight()) {
                            aircraftAltitudeOrTime = Integer
                                    .parseInt(record.getAircraftAltitude());
                        } else {
                            // data are already sorted into 2-minute
                            // steps
                            aircraftAltitudeOrTime = recCount * 2;
                        }

                        currLoc = convertFloatToPixelCoordinate(lat, lon);

                        DrawableLine dLine = new DrawableLine();

                        dLine.width = trackWidth;
                        dLine.lineStyle = lineStyle;

                        // draw the track point by point
                        dLine.setCoordinates(prevLoc.getX(), prevLoc.getY(),
                                prevLoc.getZ());
                        dLine.addPoint(currLoc.getX(), currLoc.getY(),
                                currLoc.getZ());

                        if (aircraftAltitudeOrTime == 0) {

                            dLine.basics.color = colorBar.getRGB(0);

                        } else {

                            for (int intervals = 0; intervals < colorBar
                                    .getNumIntervals(); intervals++) {
                                if (aircraftAltitudeOrTime > colorBar
                                        .getIntervalMin(intervals)) {
                                    dLine.basics.color = colorBar
                                            .getRGB(intervals);
                                }
                            }

                        }

                        drawableLines.add(dLine);

                        prevLoc = currLoc;
                    }
                }
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * gov.noaa.nws.ncep.viz.resources.AbstractFrameData#updateFrameData(gov
         * .noaa.nws.ncep.viz.resources.IRscDataObject)
         */
        @Override
        public boolean updateFrameData(IRscDataObject rscDataObj) {

            String flightID = "";
            String selectedFlightID = "";
            String departureAirport = "";
            String arrivalAirport = "";

            if (!(rscDataObj instanceof AsdiRscDataObject)) {
                statusHandler.error("Data must be of type AsdiRscDataObject");
                return false;
            }

            AsdiRecord record = ((AsdiRscDataObject) rscDataObj)
                    .getAsdiRecord();

            // get name of departure and arrival airport in flight record
            departureAirport = record.getDepartureAirport().trim();
            arrivalAirport = record.getArrivalAirport().trim();

            // Departure or arrival airport may be missing,
            // but not both. Decoder made sure of that.

            // algorithm favors departure. Also making sure the departure
            // airport matches any of the desired airports the user has selected
            // through the Attributes Editor.
            if (!departureAirport.isEmpty()
                    && matchFoundInUserSelectedAirportNames(departureAirport)) {
                flightID = FLIGHT_DEPARTURE_CHARACTER + departureAirport
                        + record.getFlightNumber();
                // see if flight ID can be constructed around an arrival flight
            } else if (!arrivalAirport.isEmpty()
                    && matchFoundInUserSelectedAirportNames(arrivalAirport)) {
                flightID = FLIGHT_ARRIVAL_CHARACTER + arrivalAirport
                        + record.getFlightNumber();
            } else {
                // ...if a match is not found between the airport name and the
                // airport names user selected in the Attributes Editor list
                // then no flight
                flightID = "";
            }

            if (!flightID.isEmpty()) {

                // construct flightID key
                /*
                 * Construct flight ID key Logic is derived from legacy. Based
                 * on if user wants to plot departing flights, arrival flights
                 * or both.
                 */
                selectedFlightID = "";

                // if user wants to plot just departing flights...
                if (departArrive == 0) {
                    // if record's flight ID starts with a "D"
                    if (flightID.startsWith(FLIGHT_DEPARTURE_CHARACTER)) {
                        // set key to this flight ID
                        selectedFlightID = flightID;
                    } // if user selects to see just arrival flights plot...
                } else if (departArrive == 1) {
                    // if record's flight ID starts with an "A"
                    if (flightID.startsWith(FLIGHT_ARRIVAL_CHARACTER)) {
                        // set key to this flight ID
                        selectedFlightID = flightID;
                    }
                } else { // else user selected to see both departure and arrival
                         // flights plot... prepend a "B" in
                         // front of records flight ID after first removing the
                         // "D" or "A" in string position 1.
                    selectedFlightID = FLIGHT_ARRIVAL_OR_DEPARTURE_CHARACTER
                            + flightID.substring(1);
                }

                // see if any records have been stored for this particular
                // flight ID. If not then create a list.
                List<AsdiRecord> records = flightData.get(selectedFlightID);
                if (records == null) {
                    records = new ArrayList<>();
                }

                // sanity check
                if (!records.contains(record)) {
                    // update the the records for this particular flight ID
                    records.add(record);
                    // put flight ID and associated records into flight data
                    // list
                    flightData.put(selectedFlightID, records);
                }

            }
            needToRegenerateDrawables = true;
            return true;
        }

        public void markDrawablesNeedRegen() {
            needToRegenerateDrawables = true;

        }

        public Map<String, List<AsdiRecord>> getFlightData() {
            return flightData;
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource2#initResource(
     * com.raytheon.uf.viz.core.IGraphicsTarget)
     */
    @Override
    public void initResource(IGraphicsTarget grphTarget) throws VizException {

        setModifiedAttrValues();
        clearAllFramesFlightData();

        dataLoader = (AsdiDataLoader) asdiResourceData.getDataLoader();
        dataLoader.setFrameDataMap(frameDataMap);
        dataLoader.setResourceData(asdiResourceData);
        dataLoader.setNewRscDataQueue(newRscDataObjsQueue);
        dataLoader.setDescriptor(this.getDescriptor());
        dataLoader.loadData();

        // Any change to resource attributes triggers image regeneration across
        // all frames
        markAllFramesDrawablesNeedRegen();

    }

    /**
     * in response to user selecting new parameters from Attributes Editor
     */
    private void setModifiedAttrValues() {
        ResourceAttrSet editedRscAttrSet = new ResourceAttrSet(
                asdiResourceData.getRscAttrSet());

        colorBar = assignColorBar(editedRscAttrSet);

        departArrive = asdiResourceData.getDepartArrive();

        timeLimitValue = asdiResourceData.getTimeLimitValue();

        colorBar = assignColorBar(editedRscAttrSet);

        userSelectedAirportNames = asdiResourceData.getSelectedAirportNames()
                .toArray(new String[0]);

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource2#paintFrame(gov
     * .noaa.nws.ncep.viz.resources.AbstractFrameData,
     * com.raytheon.uf.viz.core.IGraphicsTarget,
     * com.raytheon.uf.viz.core.drawables.PaintProperties)
     */
    @Override
    protected void paintFrame(AbstractFrameData frameData,
            IGraphicsTarget target, PaintProperties paintProps)
                    throws VizException {

        ((FrameData) frameData).paint(target);

    }

    /***
     * Converts the input Latitude and Longitude values into a PixelCoordinate
     * object.
     * 
     * @param aLat
     *            - input Latitude
     * @param aLon
     *            - input Longitude
     * @return the PixelCoordiante equivalent of the input Lat/Lon values
     */
    public PixelCoordinate convertFloatToPixelCoordinate(float aLat,
            float aLon) {

        LatLonPoint llp = new LatLonPoint(aLat, aLon, LatLonPoint.INDEGREES);
        Coordinate worldCoord = new Coordinate(
                llp.getLongitude(LatLonPoint.INDEGREES),
                llp.getLatitude(LatLonPoint.INDEGREES));
        double[] thisWorldPixDblArray = { worldCoord.x, worldCoord.y };
        double[] pixelArr = descriptor.worldToPixel(thisWorldPixDblArray);

        return new PixelCoordinate(pixelArr);
    }

    /**
     * flag indicating plot data needs to be recalculated
     */
    private void markAllFramesDrawablesNeedRegen() {

        for (AbstractFrameData frame : frameDataMap.values()) {
            if (frame instanceof FrameData) { // sanity check
                ((FrameData) frame).markDrawablesNeedRegen();
            }
        }
    }

    /**
     * Clear out lists prior to setting new data
     */
    private void clearAllFramesFlightData() {

        for (AbstractFrameData frame : frameDataMap.values()) {
            if (frame instanceof FrameData) { // sanity check
                ((FrameData) frame).flightData.clear();
                ((FrameData) frame).drawableLines.clear();
                ((FrameData) frame).flightBeginEndCoordinates.clear();
            }
        }

    }

    @Override
    protected AbstractFrameData createNewFrame(DataTime frameTime,
            int timeInt) {

        return new FrameData(frameTime, timeInt);
    }

    public AsdiResourceData getAsdiResourceData() {

        return asdiResourceData;
    }

    public void setAsdiResourceData(AsdiResourceData asdiResourceData) {

        this.asdiResourceData = asdiResourceData;
    }

    public AsdiDataLoader getDataLoader() {

        return dataLoader;
    }

    public void setDataLoader(AsdiDataLoader dataLoader) {

        this.dataLoader = dataLoader;
    }

    public Map<String, PixelCoordinate> getPrevLocForFlight() {

        return prevLocForFlight;
    }

    public void setPrevLocForFlight(
            Map<String, PixelCoordinate> prevLocForFlight) {

        this.prevLocForFlight = prevLocForFlight;
    }

    public ColorBarResource getHeightColorBarResource() {

        return heightColorBarResource;
    }

    public void setHeightColorBarResource(
            ColorBarResource heightColorBarResource) {

        this.heightColorBarResource = heightColorBarResource;
    }

    public ResourcePair getHeightColorbarResourcePair() {

        return heightColorbarResourcePair;
    }

    public void setHeightColorbarResourcePair(
            ResourcePair heightColorbarResourcePair) {
        this.heightColorbarResourcePair = heightColorbarResourcePair;
    }

    public ColorBarResource getEdrColorBarResource() {

        return edrColorBarResource;
    }

    public void setEdrColorBarResource(ColorBarResource edrColorBarResource) {

        this.edrColorBarResource = edrColorBarResource;
    }

    public ResourcePair getEdrColorbarResourcePair() {

        return edrColorbarResourcePair;
    }

    public void setEdrColorbarResourcePair(
            ResourcePair edrColorbarResourcePair) {

        this.edrColorbarResourcePair = edrColorbarResourcePair;
    }

    @Override
    public void setFrameDataMap(TreeMap<Long, AbstractFrameData> frameDataMap) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setNewRscDataQueue(
            ConcurrentLinkedQueue<IRscDataObject> newRscDataObjsQueue) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setResourceData(
            AbstractNatlCntrsRequestableResourceData resourceData) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setDescriptor(INatlCntrsDescriptor descriptor) {
        // TODO Auto-generated method stub

    }

    @Override
    public void resourceDataUpdate(Object object) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setResource(AbstractNatlCntrsResource2<?, ?> resource) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setNextFrameTime(DataTime nextFrameTime) {
        // TODO Auto-generated method stub

    }

    @Override
    public void processAnyNewData() {
        // TODO Auto-generated method stub

    }

    @Override
    public void loadData() {
        // TODO Auto-generated method stub

    }

    /**
     * get color bar for height or time
     * 
     * @param editedRscAttrSet
     *            - ResourceAttrSet: This class stores the attributes for a Natl
     *            Cntrs resource.
     * @return
     */
    ColorBar assignColorBar(ResourceAttrSet editedRscAttrSet) {

        ColorBar colorBar = null;

        if (colorbarResourcePair != null)
            getDescriptor().getResourceList().remove(colorbarResourcePair);

        colorbarResourcePair = null;

        if (asdiResourceData.plotByHeight()) {

            colorBar = asdiResourceData.getColorBarHeight();
            if (colorBar == null) {
                RscAttrValue colorBarHeightAttr = editedRscAttrSet
                        .getRscAttr("colorBarHeight");
                colorBar = (ColorBar) colorBarHeightAttr.getAttrValue();
            }
        } else {

            colorBar = asdiResourceData.getColorBarTime();
            if (colorBar == null) {
                RscAttrValue colorBarTimeAttr = editedRscAttrSet
                        .getRscAttr("colorBarTime");
                colorBar = (ColorBar) colorBarTimeAttr.getAttrValue();
            }
        }

        colorbarResourcePair = ResourcePair.constructSystemResourcePair(

                new ColorBarResourceData(colorBar));
        getDescriptor().getResourceList().add(colorbarResourcePair);
        getDescriptor().getResourceList().instantiateResources(getDescriptor(),
                true);

        return colorBar;
    }

    /**
     * Look to see if airport in ASDI Record matches the airports the user wants
     * to plot.
     * 
     * @param Airport
     *            in ASDI record
     * @return
     */
    private boolean matchFoundInUserSelectedAirportNames(String airport) {

        for (int i = 0; i < userSelectedAirportNames.length; i++) {
            if (airport.contains(userSelectedAirportNames[i])) {
                return true;
            }
        }

        return false;
    }

    /**
     * See NCEP_GEMPAK/nawips/gempak/source/gemlib/gg/ggasdiqc.f This routine
     * ensures line segment lengths are within reason. It does this by computing
     * the average speed over the distance traveled. If the average speed > 700
     * KTs the line segment is not plotted.
     * 
     * @param records-
     *            AsdiRecord is the Data Access component for Aircraft
     *            Situational Display to Industry (ASDI) data.
     * @param frameTimeMilli-
     *            Current frame time
     * @return
     */
    private boolean lineLengthIsReasonable(List<AsdiRecord> records) {

        // GFA snapping functionality.
        GfaSnap gfaSnap = GfaSnap.getInstance();

        // Max Speed in knots; average speed must be less than this
        float MXSPD = 700;

        AsdiRecord rec1 = new AsdiRecord();
        AsdiRecord rec2 = new AsdiRecord();

        double totalDist = 0;
        double totalAge = 0;

        Coordinate startPt = new Coordinate(0., 0.);
        Coordinate endPt = new Coordinate(0., 0.);

        /*
         * odd case where start and end points are the same... so time diff and
         * separation distance are zero.
         */
        if (records.size() < 2) {
            return true;
        }

        /*
         * Calculate average speed
         */
        for (int rec = 1; rec < records.size(); rec++) {

            rec1 = records.get(rec - 1);
            rec2 = records.get(rec);

            startPt = new Coordinate(rec1.getLongitude(), rec1.getLatitude());
            endPt = new Coordinate(rec2.getLongitude(), rec2.getLatitude());

            totalDist += gfaSnap.distance(startPt, endPt); // meters

            totalAge += Math.abs((((double) rec2.getDataTime()
                    .getRefTimeAsCalendar().getTimeInMillis()
                    - (double) rec1.getDataTime().getRefTimeAsCalendar()
                            .getTimeInMillis())));

        }

        /*
         * determine if line length is "within reason"
         */
        // meters per millisecond to Knots: 1943.84
        if ((totalDist / totalAge * 1943.84) < MXSPD) {

            return true;
        } else {
            return false;
        }
    }

}