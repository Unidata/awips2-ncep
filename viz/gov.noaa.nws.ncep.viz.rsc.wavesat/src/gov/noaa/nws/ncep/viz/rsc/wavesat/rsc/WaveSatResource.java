package gov.noaa.nws.ncep.viz.rsc.wavesat.rsc;

import gov.noaa.nws.ncep.gempak.parameters.colorbar.ColorBarOrientation;
import gov.noaa.nws.ncep.viz.common.ui.NmapCommon;
import gov.noaa.nws.ncep.viz.resources.AbstractFrameData;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource2;
import gov.noaa.nws.ncep.viz.resources.IDataLoader;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.IRscDataObject;
import gov.noaa.nws.ncep.viz.resources.colorBar.ColorBarResource;
import gov.noaa.nws.ncep.viz.resources.colorBar.ColorBarResourceData;
import gov.noaa.nws.ncep.viz.ui.display.ColorBar;
import gov.noaa.nws.ncep.viz.ui.display.NCMapDescriptor;

import java.awt.geom.Rectangle2D;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint.ConstraintType;
import com.raytheon.uf.common.pointdata.PointDataContainer;
import com.raytheon.uf.common.pointdata.PointDataDescription.Type;
import com.raytheon.uf.common.pointdata.PointDataView;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.DrawableString;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.LineStyle;
import com.raytheon.uf.viz.core.PixelExtent;
import com.raytheon.uf.viz.core.drawables.IFont;
import com.raytheon.uf.viz.core.drawables.IWireframeShape;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.ResourceProperties;
import com.raytheon.viz.pointdata.PointDataRequest;

/**
 * AbstractWaveSatResource - Display SGWH(V) data.
 * 
 * This code has been developed by the SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 *  09/21/2011  #248       Greg Hull    Initial creation [as WaveSatResource]. 
 *  02/16/2012  #555       S. Gurung    Added call to setPopulated(true) in queryData().
 *  05/23/2012  #785       Q. Zhou      Added getName for legend
 *  12/19/2012  #960       Greg Hull    override propertiesChanged() to update colorBar.
 *  04/23/2015  R6281      B. Hebbard   Adapt AbstractWaveSatResource from WaveSatResource
 *  05/12/2015  R6281      B. Hebbard   Make needsUpdate flag per-frame instead of global to resource
 *  06/01/2015  R6281      B. Hebbard   Need separate names for fields in PDO (for auto-
 *                                      update via AlertMessage) vs. IDataStore retrieval
 *  06/16/2015  R6281      B. Hebbard   Clean up per code review comments
 *  11/23/2015  RM11819    S. Russell   Altered initResource() and changed 
 *                                      parent class to load all data after
 *                                      the user leaves the resource manager.
 *                                      Replaced deprecate getStringBounds()
 *                                      calls in paintFrame().
 *  04/05/2016  R10365     R. Kean      Added project() method, frames need 
 *                                      update/refresh on area change, and
 *                                      condition to normalize longitude due
 *                                      to missing data point issue
 * 
 * </pre>
 * 
 * @author bhebbard
 * @version 1.0
 */
public class WaveSatResource extends
        AbstractNatlCntrsResource2<WaveSatResourceData, NCMapDescriptor>
        implements INatlCntrsResource {

    private static final IUFStatusHandler logger = UFStatus
            .getHandler(WaveSatResource.class);

    private WaveSatResourceData waveSatResourceData;

    private Unit<?> waveHeightUnits = SI.METER;

    private IFont font = null;

    protected ColorBarResource colorBarResource;

    protected ResourcePair colorBarResourcePair;

    protected String REFTIME_PARAM = "REFTIME";

    // The following hold IDataStore names that vary by EDEX plugin,
    // and so are initialized in the concrete class constructors:

    protected String latParam;

    protected String latParamInPdo;

    protected String lonParam;

    protected String lonParamInPdo;

    protected String satIdParam;

    protected String satIdParamInPdo;

    protected String waveHeightParam;

    protected String windSpeedParam;

    private final double METERS_TO_FEET = 1.0 / 0.3048;

    static public class WaveSatRscDataObj implements IRscDataObject {

        DataTime dataTime;

        double lat;

        double lon;

        // TODO: Remove this when we are able to create a full
        // WaveSatRscDataObj object from the parser either by
        // re-creating or re-querying using the URI.
        // Until then, this is a flag to indicate that the waveHeight
        // is not set and we will need to requery the data using
        // a PointDataRequest.
        boolean createdFromAutoUpdate = false;

        String satelliteId;

        double waveHeight;

        double wavePeriod;

        double windSpeed;

        @Override
        public DataTime getDataTime() {
            return dataTime;
        }
    }

    private final static Double MissingValue = -9999998.0;

    class FrameData extends AbstractFrameData {

        public TreeMap<Long, WaveSatRscDataObj> waveSatDataMap;

        // A list of WaveSatRscDataObj objects created from the AlertParser.
        // These need to be re-queried since they don't have the waveHeights
        // set in them.
        public List<WaveSatRscDataObj> autoUpdateList;

        private PixelExtent lastPixelExtent;

        // One wireframe per color interval.
        private List<IWireframeShape> waveHeightWireframes;

        private IWireframeShape timeDisplayWireframe;

        protected float prevZoomLevel = 2f;

        private boolean needsUpdate = true;

        public FrameData(DataTime frameTime, int timeInt) {
            super(frameTime, timeInt, waveSatResourceData);
            waveSatDataMap = new TreeMap<>();
            autoUpdateList = new ArrayList<>();
            waveHeightWireframes = new ArrayList<>();
        }

        public void populateFrame() throws VizException {
            if (isPopulated()) {
                return;
            }
            HashMap<String, RequestConstraint> reqConstraints = new HashMap<>(
                    waveSatResourceData.getMetadataMap());

            RequestConstraint timeConstraint = new RequestConstraint();
            String[] constraintList = { startTime.toString(),
                    endTime.toString() };
            timeConstraint.setBetweenValueList(constraintList);
            timeConstraint
                    .setConstraintType(RequestConstraint.ConstraintType.BETWEEN);
            reqConstraints.put("dataTime", timeConstraint);
            queryData(reqConstraints);
        }

        // Called by populateFrame with the start/end time constraints and
        // by the updateFrameDataFromAutoUpdateList with the ID constraints.
        public void queryData(HashMap<String, RequestConstraint> reqConstraints)
                throws VizException {

            List<String> params = new ArrayList<>();
            params.add(REFTIME_PARAM);
            params.add(latParam);
            params.add(lonParam);
            params.add(satIdParam);
            params.add(waveHeightParam);
            params.add(windSpeedParam);

            PointDataContainer pdc = null;

            try {
                pdc = PointDataRequest.requestPointDataAllLevels(
                        waveSatResourceData.getPluginName(),
                        params.toArray(new String[params.size()]), null,
                        reqConstraints);

            } catch (VizException ve) {
                logger.error("Error querying for sgwh point data", ve);
                throw ve;
            }

            if (pdc == null) {
                return;
            } else {
                pdc.setCurrentSz(pdc.getAllocatedSz());
            }

            for (int uriCounter = 0; uriCounter < pdc.getAllocatedSz(); uriCounter++) {
                PointDataView pdv = pdc.readRandom(uriCounter);
                if (pdv != null) {
                    for (IRscDataObject dataObject : processRecord(pdv)) {
                        newRscDataObjsQueue.add(dataObject);
                    }
                }
            }

            setPopulated(true);
        }

        // Query the data in the autoUpdateList and add results to the
        // waveSatDataMap
        //
        public void updateFrameDataFromAutoUpdateList() {

            HashMap<String, RequestConstraint> reqConstraints = new HashMap<>(
                    waveSatResourceData.getMetadataMap());

            RequestConstraint timeConstraint = new RequestConstraint();
            timeConstraint.setConstraintType(ConstraintType.IN);

            while (!autoUpdateList.isEmpty()) {
                int i = 0, maxQueryTimes = 500;

                timeConstraint
                        .setConstraintValue(autoUpdateList.get(0).dataTime
                                .toString());
                autoUpdateList.remove(0);

                while (!autoUpdateList.isEmpty() && i < maxQueryTimes) {
                    timeConstraint.addToConstraintValueList(autoUpdateList
                            .get(0).dataTime.toString());
                    autoUpdateList.remove(0);
                    i++;
                }
                reqConstraints.put("dataTime", timeConstraint);

                try {
                    queryData(reqConstraints);
                } catch (VizException ve) {
                    logger.error("Error requesting auto update WaveSat data",
                            ve);
                }
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * gov.noaa.nws.ncep.viz.resources.AbstractFrameData#updateFrameData
         * (gov.noaa.nws.ncep.viz.resources.IRscDataObject)
         * 
         * Add the WaveSatRscDataObj to the list for this frame.
         */
        @Override
        public boolean updateFrameData(IRscDataObject rscDataObj) {
            if (!(rscDataObj instanceof WaveSatRscDataObj)) {

                logger.debug("WaveSat:updateFrameData expecting WaveSatRscDataObj instead of: "
                        + rscDataObj.getClass().getName());
                return false;
            }

            WaveSatRscDataObj waveSatRscDataObj = (WaveSatRscDataObj) rscDataObj;

            // If created from the AlertParser, the waveHeight is not set yet.
            // This is a placeholder used to query the data. Remove this when
            // the WaveSatAlertParser is able to create a valid data object.
            if (waveSatRscDataObj.createdFromAutoUpdate) {
                autoUpdateList.add(waveSatRscDataObj);
            } else {
                waveSatDataMap.put(waveSatRscDataObj.dataTime.getRefTime()
                        .getTime(), waveSatRscDataObj);
            }

            needsUpdate = true;

            return true;
        }

        /*
         * (non-Javadoc)
         * 
         * @see gov.noaa.nws.ncep.viz.resources.AbstractFrameData#dispose()
         */
        @Override
        public void dispose() {
            waveSatDataMap.clear();

            if (timeDisplayWireframe != null) {
                timeDisplayWireframe.dispose();
            }

            for (IWireframeShape shape : waveHeightWireframes) {
                shape.dispose();
            }
            waveHeightWireframes.clear();
        }

    }

    public WaveSatResource(WaveSatResourceData ncresourceData,
            LoadProperties loadProperties) {
        super(ncresourceData, loadProperties);
        this.waveSatResourceData = (WaveSatResourceData) resourceData;
        this.waveHeightUnits = this.waveSatResourceData
                .getUseFeetInsteadOfMeters() ? NonSI.FOOT : SI.METER;

        ncresourceData.setAlertParser(new WaveSatAlertParser());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource2#processRecord
     * (java.lang.Object)
     */
    @Override
    protected IRscDataObject[] processRecord(Object pdo) {

        // If called from the AutoUpdate, this is the object created by the
        // WaveSatAlertParser which is a WaveSatRscDataObj.
        // Currently this object doesn't have all the data set (the sgwh,
        // windSpeed...) since the info is not in the URI and a URI-base query
        // returns a SgwhRecord without the HDF5 parameters set.)
        if (pdo instanceof WaveSatRscDataObj) {
            WaveSatRscDataObj waveSatData = (WaveSatRscDataObj) pdo;
            if (waveSatData.createdFromAutoUpdate == false) {
                logger.debug("WaveSat processRecord() : sanity check #1");
                return null;
            }
            return new WaveSatRscDataObj[] { waveSatData };
        }
        if (!(pdo instanceof PointDataView)) {
            logger.debug("WaveSat processRecord() : Expecting PointDataView object instead of: "
                    + pdo.getClass().getName());
            return new WaveSatRscDataObj[] {};
        }

        WaveSatRscDataObj waveSatData = new WaveSatRscDataObj();
        PointDataView pdv = (PointDataView) pdo;

        if (pdv.getType(REFTIME_PARAM) != Type.LONG
                || pdv.getType(latParam) != Type.FLOAT
                || pdv.getType(lonParam) != Type.FLOAT
                || pdv.getType(satIdParam) != Type.LONG
                || pdv.getType(waveHeightParam) != Type.FLOAT
                || pdv.getType(windSpeedParam) != Type.FLOAT) {
            logger.debug("SGWHV parameter not found.");
            return new WaveSatRscDataObj[] {};
        }

        waveSatData.dataTime = new DataTime(new Date(
                (Long) pdv.getNumber(REFTIME_PARAM)));
        waveSatData.satelliteId = Long.toString((Long) pdv
                .getNumber(satIdParam).longValue());
        waveSatData.lat = pdv.getNumber(latParam).doubleValue();
        waveSatData.lon = pdv.getNumber(lonParam).doubleValue();
        waveSatData.waveHeight = pdv.getNumber(waveHeightParam).doubleValue();
        waveSatData.windSpeed = pdv.getNumber(windSpeedParam).doubleValue();

        return new WaveSatRscDataObj[] { waveSatData };
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource2#initResource
     * (com.raytheon.uf.viz.core.IGraphicsTarget)
     */
    @Override
    public void initResource(IGraphicsTarget grphTarget) throws VizException {

        setNeedsUpdateAllFrames(true);

        ColorBar colorBar = waveSatResourceData.getUseFeetInsteadOfMeters() ? waveSatResourceData
                .getColorBarForFeet() : waveSatResourceData
                .getColorBarForMeters();
        colorBar.setReverseOrder(colorBar.getOrientation() == ColorBarOrientation.Vertical);

        colorBarResourcePair = ResourcePair
                .constructSystemResourcePair(new ColorBarResourceData(colorBar));

        getDescriptor().getResourceList().add(colorBarResourcePair);
        getDescriptor().getResourceList().instantiateResources(getDescriptor(),
                true);

        colorBarResource = (ColorBarResource) colorBarResourcePair
                .getResource();

        // Load all of the data after the user presses the "Load And Close" or
        // "Load" button in the Resource Manager.
        //
        // NOTE: the code to load data frame by frame as the user pages to
        // is still in this class. If that is what is desired all that is
        // necessary is to comment out the block of code below.
        // AbstractWaveSatResource also has code for painting and autoupdating
        // data which should remain in this class.
        IDataLoader waveSatDataLoader = waveSatResourceData.getDataLoader();
        waveSatDataLoader.setFrameDataMap(frameDataMap);
        waveSatDataLoader.setResourceData(waveSatResourceData);
        waveSatDataLoader.setNewRscDataQueue(newRscDataObjsQueue);
        waveSatDataLoader.setDescriptor(this.getDescriptor());
        waveSatDataLoader.loadData();
    }

    /*
     * Sets the needsUpdate flag on every frame to the given boolean "b". (ie:
     * setting to "true" will cause the updateCurrentFrame() method to redisplay
     * all data points on all frames.)
     */
    private void setNeedsUpdateAllFrames(boolean b) {
        for (AbstractFrameData afd : frameDataMap.values()) {
            FrameData fd = (FrameData) afd;
            fd.needsUpdate = b;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.core.rsc.AbstractVizResource#project(org.opengis.
     * referencing.crs.CoordinateReferenceSystem)
     */
    @Override
    public void project(CoordinateReferenceSystem crs) throws VizException {

        // area change... set needsUpdate on all frames
        setNeedsUpdateAllFrames(true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource2#paintFrame
     * (gov.noaa.nws.ncep.viz.resources.AbstractFrameData,
     * com.raytheon.uf.viz.core.IGraphicsTarget,
     * com.raytheon.uf.viz.core.drawables.PaintProperties)
     */
    @Override
    public void paintFrame(AbstractFrameData frameData,
            IGraphicsTarget grphTarget, PaintProperties paintProps)
            throws VizException {

        FrameData currFrameData = (FrameData) frameData;

        if (!currFrameData.isPopulated()) {
            currFrameData.populateFrame();
        } else if (!currFrameData.autoUpdateList.isEmpty()) {
            currFrameData.updateFrameDataFromAutoUpdateList();
        }

        IExtent extent = paintProps.getView().getExtent();

        ColorBar colorBar = waveSatResourceData.getUseFeetInsteadOfMeters() ? waveSatResourceData
                .getColorBarForFeet() : waveSatResourceData
                .getColorBarForMeters();
        colorBar.setReverseOrder(colorBar.getOrientation() == ColorBarOrientation.Vertical);
        colorBar.setNumDecimals(1);

        if (!currFrameData.needsUpdate
                && !currFrameData.waveHeightWireframes.isEmpty()
                && currFrameData.prevZoomLevel == paintProps.getZoomLevel()
                || paintProps.isZooming()) { // isZooming() check limits flicker

            grphTarget.drawWireframeShape(currFrameData.timeDisplayWireframe,
                    waveSatResourceData.getTimeDisplayColor(), 1.0f,
                    LineStyle.SOLID, font);

            for (int s = currFrameData.waveHeightWireframes.size() - 1; s >= 0; s--) {
                grphTarget.drawWireframeShape(
                        currFrameData.waveHeightWireframes.get(s),
                        colorBar.getRGB(s), 1.0f, LineStyle.SOLID, font);
            }

        } else {

            // refresh wave data points on frame
            paintCurrentFrame(grphTarget, paintProps, currFrameData, extent,
                    colorBar);
        }
    }

    /*
     * update wave data points on the map, for the current frame
     */
    private void paintCurrentFrame(IGraphicsTarget grphTarget,
            PaintProperties paintProps, FrameData currFrameData,
            IExtent extent, ColorBar colorBar) throws VizException {

        currFrameData.needsUpdate = false;

        currFrameData.prevZoomLevel = paintProps.getZoomLevel();

        if (font == null) {
            font = grphTarget.initializeFont(waveSatResourceData.getFontName(),
                    waveSatResourceData.getFontSize().floatValue(), null);
            // Disable anti-aliasing, for sharper text.
            font.setSmoothing(false);
            font.setScaleFont(false);
        }

        currFrameData.lastPixelExtent = (PixelExtent) extent.clone();

        // create or reuse the wireframe shapes for the times
        // and for each color's waveHeight values.
        if (currFrameData.timeDisplayWireframe == null) {
            currFrameData.timeDisplayWireframe = grphTarget
                    .createWireframeShape(true, this.descriptor);
        } else {
            currFrameData.timeDisplayWireframe.reset();
        }

        for (int i = 0; i < colorBar.getNumIntervals(); i++) {
            if (i >= currFrameData.waveHeightWireframes.size()) {
                currFrameData.waveHeightWireframes.add(grphTarget
                        .createWireframeShape(true, this.descriptor));
            } else {
                currFrameData.waveHeightWireframes.get(i).reset();
            }
        }

        // If intervals have been removed from the colorBar, delete
        // wireframes.
        while (currFrameData.waveHeightWireframes.size() > colorBar
                .getNumIntervals()) {
            IWireframeShape wf = currFrameData.waveHeightWireframes
                    .get(currFrameData.waveHeightWireframes.size() - 1);
            wf.dispose();
            currFrameData.waveHeightWireframes.remove(wf);
        }

        Rectangle2D bnds = null;
        PixelExtent prevPixExtent = null;
        long prevDisplayedTime = 0;

        for (WaveSatRscDataObj waveSatData : currFrameData.waveSatDataMap
                .values()) {

            // normalize longitude to convention -180 to +180
            // (due to missing data points for lons > 180)
            if (waveSatData.lon > 180) {
                waveSatData.lon -= 360.0;
            }

            double[] latLon = { waveSatData.lon, waveSatData.lat };

            double[] waveHeightPixLoc = this.descriptor.worldToPixel(latLon);

            if (waveHeightPixLoc != null) {
                Double waveHeight = waveSatData.waveHeight;
                if (waveSatResourceData.getUseFeetInsteadOfMeters()) {
                    waveHeight *= METERS_TO_FEET;
                }

                // Round to the nearest tenth.
                waveHeight = (double) Math.round(waveHeight * 10) / 10;
                String waveHeightStr = waveHeight.toString();

                double scale = extent.getWidth()
                        / paintProps.getCanvasBounds().width;

                DrawableString wavehs = new DrawableString(waveHeightStr);
                wavehs.font = font;
                bnds = grphTarget.getStringsBounds(wavehs);

                // We have to have a line or the labels won't draw, but this
                // will get drawn behind the label and so will not show up.
                double[][] line = new double[2][2];
                line[0][0] = waveHeightPixLoc[0] - .1;
                line[0][1] = waveHeightPixLoc[1];
                line[1][0] = waveHeightPixLoc[0] + .1;
                line[1][1] = waveHeightPixLoc[1];

                // Alignment set in addLabel is CENTER for x and BOTTOM for
                // y and since we currently can't change this, we have to
                // adjust for the latitude.
                waveHeightPixLoc[1] += scale * bnds.getHeight() / 2;

                double minX = waveHeightPixLoc[0] - scale * bnds.getWidth() / 2;
                double maxX = waveHeightPixLoc[0] + scale * bnds.getWidth() / 2;
                double minY = waveHeightPixLoc[1] - scale * bnds.getHeight()
                        / 2;
                double maxY = waveHeightPixLoc[1] + scale * bnds.getHeight()
                        / 2;

                PixelExtent pixelExtent = new PixelExtent(minX, maxX, minY,
                        maxY);

                if (!waveHeight.equals(MissingValue)) {
                    if (prevPixExtent == null
                            || !prevPixExtent.intersect(pixelExtent)) {

                        for (int i = 0; i < colorBar.getNumIntervals(); i++) {
                            if (colorBar.isValueInInterval(i,
                                    waveHeight.floatValue(),
                                    // currently ignored
                                    waveHeightUnits)) {

                                IWireframeShape wf = currFrameData.waveHeightWireframes
                                        .get(i);
                                wf.addLineSegment(line);

                                wf.addLabel(waveHeightStr, waveHeightPixLoc);
                                prevPixExtent = pixelExtent;
                                break;
                            }
                        }
                    }
                }

                DataTime waveTime = waveSatData.getDataTime();

                if (waveTime.getRefTime().getTime() - prevDisplayedTime > waveSatResourceData
                        .getTimeDisplayInterval() * 1000 * 60) {

                    // Create a time for this

                    SimpleDateFormat dateFormat = new SimpleDateFormat(
                            "MMdd/HHmm");
                    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                    Calendar cal = waveTime.getValidTime();

                    // Not only do we wait to make sure "interval" time has
                    // passed since prevDisplayedTime, we also make sure
                    // that we only display time if it's an even multiple of
                    // "interval" minutes since midnight UTC (like legacy).
                    int hourOfDay = cal.get(Calendar.HOUR_OF_DAY);
                    int minuteOfHour = cal.get(Calendar.MINUTE);
                    int minuteOfDay = hourOfDay * 60 + minuteOfHour;
                    if (minuteOfDay
                            % waveSatResourceData.getTimeDisplayInterval() != 0) {
                        continue;
                    }

                    String timeString = dateFormat.format(cal.getTime());
                    DrawableString timestr = new DrawableString(timeString);
                    timestr.font = font;
                    Rectangle2D timeBnds = grphTarget.getStringsBounds(timestr);

                    double[][] timeLine = new double[2][2];
                    timeLine[0][0] = pixelExtent.getMaxX() + 5 * scale;
                    timeLine[0][1] = waveHeightPixLoc[1] - timeBnds.getHeight()
                            / 2 * scale;
                    timeLine[1][0] = pixelExtent.getMaxX() + 40 * scale;
                    timeLine[1][1] = waveHeightPixLoc[1] - timeBnds.getHeight()
                            / 2 * scale;

                    currFrameData.timeDisplayWireframe.addLineSegment(timeLine);

                    double[] timeLoc = new double[2];
                    timeLoc[0] = pixelExtent.getMaxX() + scale
                            * (timeBnds.getWidth() + 40) / 2;
                    timeLoc[1] = waveHeightPixLoc[1];
                    currFrameData.timeDisplayWireframe.addLabel(timeString
                            .toUpperCase().trim(), timeLoc);

                    prevDisplayedTime = waveTime.getRefTime().getTime();
                }
            }
        }

        // Draw the time first so it can't obscure any of the data.
        currFrameData.timeDisplayWireframe.compile();

        grphTarget.drawWireframeShape(currFrameData.timeDisplayWireframe,
                waveSatResourceData.getTimeDisplayColor(), 1.0f,
                LineStyle.SOLID, font);

        for (int i = colorBar.getNumIntervals() - 1; i >= 0; i--) {
            IWireframeShape wf = currFrameData.waveHeightWireframes.get(i);
            wf.compile();
            grphTarget.drawWireframeShape(wf, colorBar.getRGB(i), 1.0f,
                    LineStyle.SOLID, font);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource2#disposeInternal
     * ()
     */
    @Override
    public void disposeInternal() {
        super.disposeInternal();
        if (font != null) {
            font.dispose();
            font = null;
        }
        if (colorBarResourcePair != null) {
            getDescriptor().getResourceList().remove(colorBarResourcePair);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource2#
     * resourceAttrsModified()
     */
    @Override
    public void resourceAttrsModified() {

        setNeedsUpdateAllFrames(true);

        ColorBar colorBar = waveSatResourceData.getUseFeetInsteadOfMeters() ? waveSatResourceData
                .getColorBarForFeet() : waveSatResourceData
                .getColorBarForMeters();
        colorBar.setReverseOrder(colorBar.getOrientation() == ColorBarOrientation.Vertical);

        getDescriptor().getResourceList().remove(colorBarResourcePair);

        colorBarResourcePair = ResourcePair
                .constructSystemResourcePair(new ColorBarResourceData(colorBar));

        getDescriptor().getResourceList().add(colorBarResourcePair);
        getDescriptor().getResourceList().instantiateResources(getDescriptor(),
                true);

        colorBarResource = (ColorBarResource) colorBarResourcePair
                .getResource();

        if (font != null) {
            font.dispose();
            font = null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource2#createNewFrame
     * (com.raytheon.uf.common.time.DataTime, int)
     */
    @Override
    protected AbstractFrameData createNewFrame(DataTime frameTime, int timeInt) {
        return new FrameData(frameTime, timeInt);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.core.rsc.AbstractVizResource#propertiesChanged(com
     * .raytheon.uf.viz.core.rsc.ResourceProperties)
     */
    @Override
    public void propertiesChanged(ResourceProperties updatedProps) {
        if (colorBarResourcePair != null) {
            colorBarResourcePair.getProperties().setVisible(
                    updatedProps.isVisible());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.viz.core.rsc.AbstractVizResource#getName()
     */
    @Override
    public String getName() {
        String legendString = super.getName();
        FrameData fd = (FrameData) getCurrentFrame();
        if (fd == null || fd.getFrameTime() == null
                || fd.waveSatDataMap.size() == 0) {
            return legendString + "-No Data";
        }
        return legendString + " "
                + NmapCommon.getTimeStringFromDataTime(fd.getFrameTime(), "/");
    }

    public String getLatParam() {
        return latParam;
    }

    public void setLatParam(String latParam) {
        this.latParam = latParam;
    }

    public String getLatParamInPdo() {
        return latParamInPdo;
    }

    public void setLatParamInPdo(String latParamInPdo) {
        this.latParamInPdo = latParamInPdo;
    }

    public String getLonParam() {
        return lonParam;
    }

    public void setLonParam(String lonParam) {
        this.lonParam = lonParam;
    }

    public String getLonParamInPdo() {
        return lonParamInPdo;
    }

    public void setLonParamInPdo(String lonParamInPdo) {
        this.lonParamInPdo = lonParamInPdo;
    }

    public String getSatIdParam() {
        return satIdParam;
    }

    public void setSatIdParam(String satIdParam) {
        this.satIdParam = satIdParam;
    }

    public String getSatIdParamInPdo() {
        return satIdParamInPdo;
    }

    public void setSatIdParamInPdo(String satIdParamInPdo) {
        this.satIdParamInPdo = satIdParamInPdo;
    }

    public String getWaveHeightParam() {
        return waveHeightParam;
    }

    public void setWaveHeightParam(String waveHeightParam) {
        this.waveHeightParam = waveHeightParam;
    }

    public String getWindSpeedParam() {
        return windSpeedParam;
    }

    public void setWindSpeedParam(String windSpeedParam) {
        this.windSpeedParam = windSpeedParam;
    }
}
