/**
 * StormTrackResource
 *
 * Date created 12 Oct 2011
 *
 * This code has been developed by the SIB for use in the AWIPS2 system.
 */
package gov.noaa.nws.ncep.viz.rsc.stormtrack.rsc;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.swt.graphics.RGB;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateList;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.raytheon.uf.common.dataquery.requests.DbQueryRequest;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint.ConstraintType;
import com.raytheon.uf.common.dataquery.responses.DbQueryResponse;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.HorizontalAlignment;
import com.raytheon.uf.viz.core.IGraphicsTarget.TextStyle;
import com.raytheon.uf.viz.core.IGraphicsTarget.VerticalAlignment;
import com.raytheon.uf.viz.core.PixelExtent;
import com.raytheon.uf.viz.core.drawables.IFont;
import com.raytheon.uf.viz.core.drawables.IWireframeShape;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.geom.PixelCoordinate;
import com.raytheon.uf.viz.core.requests.ThriftClient;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.ResourceProperties;

import gov.noaa.nws.ncep.common.dataplugin.stormtrack.StormTrackRecord;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData.TimeMatchMethod;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.colorBar.ColorBarResource;
import gov.noaa.nws.ncep.viz.resources.colorBar.ColorBarResourceData;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceName;
import gov.noaa.nws.ncep.viz.rsc.stormtrack.rsc.StormTrackResourceData.ModelDisplayAttrs;
import gov.noaa.nws.ncep.viz.ui.display.ColorBar;
import gov.noaa.nws.ncep.viz.ui.display.NCMapDescriptor;
import systems.uom.common.USCustomary;

/**
 * Displays the Ensemble Storm Track ( ENS_CYC - MISC resource)
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- ------------------------------------------
 * Aug 10, 2010  284      Archana     Initial creation.
 * Sep 30, 2010  307      Greg Hull   created AtcfCycloneRscDataObject wrapper
 * Oct 19, 2010  307      Archana     updated queryRecords() and
 *                                    createQueryScriptForPython(String, String)
 *                                    for time-matching
 * Nov 11, 2010  307      Greg Hull   Use data with best timeMatch. adjust
 *                                    startTime in query.
 * Mar 01, 2011           Greg Hull   frameInterval -> frameSpan
 * Oct 12, 2011           sgilbert    Modified from ATCFResource
 * Oct 26, 2011           B. Hebbard  Initial commit as StormTrack
 * May 23, 2012  785      Q. Zhou     Added getName for legend.
 * Sep 21, 2012  858      Greg Hull   don't use default queryRecords. constrain
 *                                    on modelNames and query by frame.
 * Sep 28, 2012  858      Greg Hull   cache wireframes by the color/linewidth
 *                                    and don't recompile unless changed
 * Oct 07, 2012  858      Greg Hull   implement forecastHour and
 *                                    colorByWindSpeed options
 * Oct 15, 2012  858      Greg Hull   add options to display forecastHour,
 *                                    modelName, windSpeed and cycloneId
 * Oct 20, 2012  858      Greg Hull   change query to handle the ENS_CYC_FCST
 *                                    version of this resource
 * Dec 19, 2012  960      Greg Hull   override propertiesChanged() to update
 *                                    colorBar.
 * Dec 19, 2014  ?        B. Yin      Remove ScriptCreator, use Thrift Client.
 * Nov 05, 2015  5070     randerso    Adjust font sizes for dpi scaling
 * Jul 15, 2020  8191     randerso    Updated for changes to LatLonPoint
 *
 * </pre>
 *
 * @author sgilbert
 */
public class StormTrackResource extends
        AbstractNatlCntrsResource<StormTrackResourceData, NCMapDescriptor>
        implements INatlCntrsResource {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(StormTrackResource.class);

    private StormTrackResourceData strmTrkRscData;

    private IFont font = null;

    private double charHeight;

    private double charWidth;

    private double screenToWorldRatio;

    protected ColorBarResource cbarResource;

    protected ResourcePair cbarRscPair;

    private PaintProperties prevPaintProps = null;

    // the key in a map to store wireframes based on the color and line width
    // which they will be drawn in.
    //
    private static class WireFrameDisplayAttrs {

        public RGB color;

        public Integer lineWidth;

        public WireFrameDisplayAttrs(RGB rgb, Integer lw) {
            color = rgb;
            lineWidth = lw;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((color == null) ? 0 : color.hashCode());
            result = prime * result
                    + ((lineWidth == null) ? 0 : lineWidth.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            WireFrameDisplayAttrs other = (WireFrameDisplayAttrs) obj;
            if (color == null) {
                if (other.color != null) {
                    return false;
                }
            } else if (!color.equals(other.color)) {
                return false;
            }
            if (lineWidth == null) {
                if (other.lineWidth != null) {
                    return false;
                }
            } else if (!lineWidth.equals(other.lineWidth)) {
                return false;
            }
            return true;
        }
    }

    private class FrameData extends AbstractFrameData {

        // map from the name of the cyclone to the StormTrackCyclone object
        private StormTrackContainer tracks;

        private Map<WireFrameDisplayAttrs, IWireframeShape> wireframesMap;

        private boolean recreateWireframes = true;

        private IGraphicsTarget currGrpTarget;

        private ArrayList<String> modelNames;

        private ArrayList<RGB> colorList;

        public FrameData(DataTime frameTime, int timeInt) {
            super(frameTime, timeInt);
            wireframesMap = new HashMap<>();

            tracks = new StormTrackContainer();

            modelNames = new ArrayList<>();
            colorList = new ArrayList<>();
        }

        public void populateFrame() {

            long t0 = System.currentTimeMillis();
            try {
                RequestConstraint reqConstr = new RequestConstraint();
                Map<String, RequestConstraint> metadataMap = new HashMap<>(
                        strmTrkRscData.getMetadataMap());

                // set the constraints for the query.
                // for forecast resources we need to match the cycle time and
                // create a range constraint on the forecast hour.
                if (strmTrkRscData.isForecastResource()) {

                    ResourceName rscName = getResourceData().getResourceName();

                    String[] dts = rscName.getCycleTime().toString().split(" ");
                    String cycleTimeStr = dts[0] + " "
                            + dts[1].substring(0, dts[1].length() - 2);

                    statusHandler
                            .debug("reftime constraint is:" + cycleTimeStr);

                    reqConstr = new RequestConstraint(cycleTimeStr);
                    metadataMap.put("dataTime.refTime", reqConstr);

                    long refTimeMs = rscName.getCycleTime().getRefTime()
                            .getTime();
                    long frmTimeMs = getFrameTime().getRefTime().getTime();
                    long frmStartMs = getFrameStartTime().getRefTime()
                            .getTime();
                    long frmEndMs = getFrameEndTime().getRefTime().getTime();

                    if (strmTrkRscData
                            .getTimeMatchMethod() == TimeMatchMethod.EXACT) {
                        // create and EQUALS constraint for the forecast hour
                        metadataMap.put("dataTime.fcstTime",
                                new RequestConstraint(Long.toString(
                                        (frmTimeMs - refTimeMs) / 1000)));
                    } else {
                        // determine the fcst hr range for the frame span
                        long beginFcstHr = (frmStartMs - refTimeMs) / 1000;
                        long endFcstHr = (frmEndMs - refTimeMs) / 1000;

                        reqConstr = new RequestConstraint();
                        reqConstr.setBetweenValueList(
                                new String[] { Long.toString(beginFcstHr),
                                        Long.toString(endFcstHr) });
                        reqConstr.setConstraintType(ConstraintType.BETWEEN);

                        metadataMap.put("dataTime.fcstTime", reqConstr);
                    }
                } else {
//                  if( strmTrkRscData.getTimeMatchMethod() == TimeMatchMethod.EXACT ) {
//                      DataTime refTime = new DataTime( frameTime.getRefTime() );
//                      reqConstr.setConstraintValue( refTime.toString() );
//                  }
//                  else {
//                      String[] constraintList = { startTime.toString(), endTime.toString() };
//                      reqConstr.setBetweenValueList(constraintList);
//                      reqConstr.setConstraintType(RequestConstraint.ConstraintType.BETWEEN);
//                  }
                    String[] dts = startTime.toString().split(" ");
                    String startTimeStr = dts[0] + " "
                            + dts[1].substring(0, dts[1].length() - 2);
                    dts = endTime.toString().split(" ");
                    String endTimeStr = dts[0] + " "
                            + dts[1].substring(0, dts[1].length() - 2);
                    String[] constraintList = { startTimeStr, endTimeStr };
                    reqConstr.setBetweenValueList(constraintList);
                    reqConstr.setConstraintType(
                            RequestConstraint.ConstraintType.BETWEEN);

                    metadataMap.put("dataTime.refTime", reqConstr);

//                  metadataMap.put("dataTime", reqConstr );
                }

                // add a constraint for the modelNames
                StringBuilder modelNamesString = new StringBuilder();
                for (ModelDisplayAttrs dispAttrs : strmTrkRscData
                        .getModelDisplayAttributes()) {
                    if (dispAttrs.modelName != null
                            && !dispAttrs.modelName.isEmpty()) {
                        modelNamesString.append(dispAttrs.modelName + ",");
                    }
                }
                if (modelNamesString.toString().isEmpty()) {
                    statusHandler.debug(
                            "???sanity check : no models specified to query??");
                    return;
                }

                modelNamesString
                        .deleteCharAt(modelNamesString.toString().length() - 1);

                reqConstr = new RequestConstraint(modelNamesString.toString(),
                        ConstraintType.IN);

                metadataMap.put("model", reqConstr);

                DbQueryRequest request = new DbQueryRequest();
                request.setConstraints(metadataMap);

                DbQueryResponse response = (DbQueryResponse) ThriftClient
                        .sendRequest(request);

                Object[] pdoList = response.getResults().get(0).values()
                        .toArray();

                /*
                 * This could really be considered an error. If the limit is
                 * reached then this probably took too long and the calling
                 * resource should be making its own query for data.
                 */
                if (pdoList.length == 100_000) {
                    statusHandler.debug(
                            "QUERY WARNING: #Records returned is the max limit set to "
                                    + "100,000. All requested data was not returned.");
                } else {
                    statusHandler.debug(
                            pdoList.length + " Storm Track Records read.");
                }

                for (Object pdo : pdoList) {
                    for (IRscDataObject rscDataObj : processRecord(pdo)) {
                        /*
                         * Note that for the regular (i.e. non-forecast/legacy
                         * behavior) version of this stormTrack resource the
                         * storm track records are time matched just on the
                         * refTimes and not the forecast times. All the track
                         * points for a give storm
                         */
                        if (!strmTrkRscData.isForecastResource()) {
                            if (timeMatch(new DataTime(rscDataObj.getDataTime()
                                    .getRefTime())) >= 0) {
                                updateFrameData(rscDataObj);
                            }
                        } else {
                            if (isRscDataObjInFrame(rscDataObj)) {
                                updateFrameData(rscDataObj);
                            }
                        }
//                      else { this can actually happen if the time is equal to the begin time
//                          statusHandler.debug("stormTrack obj doesn't time match to the frame which queried it???  " + rscDataObj.getDataTime().toString() );
//                      }
                    }
                }

                setPopulated(true);

            } catch (VizException e) {
                statusHandler.debug("Error querying storm track records", e);
            }

            long t1 = System.currentTimeMillis();
            statusHandler.debug("Initializing StormTrack took:" + (t1 - t0));
        }

        // TODO : if called from a URI notification how do we know when the
        // track is complete and
        // to recreate the
        @Override
        public boolean updateFrameData(IRscDataObject rscDataObj) {
            if (rscDataObj instanceof DfltRecordRscDataObj) {
                DfltRecordRscDataObj dfltObj = (DfltRecordRscDataObj) rscDataObj;

                // statusHandler.debug("updateFrameData with:
                // "+dfltObj.getPDO().getClass().getCanonicalName());
                // statusHandler.debug("updateFrameData with:
                // "+dfltObj.getPDO());
                if (dfltObj.getPDO() instanceof StormTrackRecord) {
                    StormTrackRecord record = (StormTrackRecord) dfltObj
                            .getPDO();

                    synchronized (tracks) {
                        tracks.addStormRecord(record);
                        recreateWireframes = true;
                    }
                }
            }

            return true;
        }

        public void paint(PaintProperties paintProps) throws VizException {

            if (!isPopulated()) {
                populateFrame();
            }

            IExtent extent = paintProps.getView().getExtent();

            /*
             * Turns out that even with a whole screen full of tracks it doesn't
             * cost us much to draw wireframes off screen even when zoomed in.
             * This just lets us avoid recomputing the wireframes on a pan. But
             * on a zoom (the end of a zoom) we will still need to recompute
             * because the marker sizes are based on the zoom level.
             */
//           if( prevExtent != null &&
//              !extent.equals( prevExtent ) &&
//              !paintProps.isZooming() ) {
            if (prevPaintProps != null) {
                if (prevPaintProps.isZooming() && !paintProps.isZooming()) {
                    recreateWireframes = true;
                } else if (prevPaintProps.getDataTime().getValidTime()
                        .getTimeInMillis() != paintProps.getDataTime()
                                .getValidTime().getTimeInMillis()) {
                    recreateWireframes = true;
                }
            } else if (wireframesMap == null) {
                recreateWireframes = true;
            }

            prevPaintProps = new PaintProperties(paintProps);

            /*
             * If the wireframes are ready to go and don't need to be recreated
             * just draw them.
             */
            if (!recreateWireframes) {
                for (Entry<WireFrameDisplayAttrs, IWireframeShape> entry : wireframesMap
                        .entrySet()) {
                    WireFrameDisplayAttrs wfAttrs = entry.getKey();
                    IWireframeShape wireFrame = entry.getValue();

                    currGrpTarget.drawWireframeShape(wireFrame, wfAttrs.color,
                            wfAttrs.lineWidth);
                }

                /*
                 * Either draw the list of models or a colorbar for the wind
                 * speed categories
                 */
                if (strmTrkRscData.getColorCodeByWindSpeed()) {
                    if (cbarRscPair == null) {
                        cbarRscPair = ResourcePair.constructSystemResourcePair(
                                new ColorBarResourceData(
                                        strmTrkRscData.getWindSpeedColorBar()));

                        getDescriptor().getResourceList().add(cbarRscPair);
                        getDescriptor().getResourceList()
                                .instantiateResources(getDescriptor(), true);
                        cbarResource = (ColorBarResource) cbarRscPair
                                .getResource();
                    }
                } else {
                    if (cbarRscPair != null) {
                        getDescriptor().getResourceList()
                                .removeRsc(cbarResource);
                        cbarResource.dispose();
                        cbarResource = null;
                        cbarRscPair = null;
                    }

                    // draw the models list/legend
                    //
                    if (!modelNames.isEmpty()) {
                        IExtent screenExtent = paintProps.getView().getExtent();
                        IExtent mapExtent = new PixelExtent(
                                descriptor.getGridGeometry().getGridRange());
                        double x0 = Math.max(mapExtent.getMinX(),
                                screenExtent.getMinX()
                                        + (screenExtent.getWidth() * 0.08));
                        double y0 = Math.min(mapExtent.getMaxY(),
                                screenExtent.getMaxY()
                                        - (screenExtent.getHeight() * 0.10));

                        currGrpTarget.drawStrings(font,
                                modelNames.toArray(new String[0]), x0,
                                y0 /* - offsetY * (modelNames.size()+0.5) */,
                                0.0, TextStyle.NORMAL,
                                colorList.toArray(new RGB[0]),
                                HorizontalAlignment.RIGHT,
                                VerticalAlignment.BOTTOM);
                    }
                }

                return;
            }

            recreateWireframes = false;

//          prevExtent = extent.clone();

            modelNames.clear();
            modelNames.add("Model:");
            colorList.clear();
            colorList.add(new RGB(255, 255, 255));

            // NOTE : Since this is not actually clearing out old and possibly
            // unused
            // wireframes, there may be some that are not used but its highly
            // unlikely that
            // there will be so many as to affect memory or performance.
            for (IWireframeShape wf : wireframesMap.values()) {
                wf.reset();
            }

            screenToWorldRatio = paintProps.getCanvasBounds().width
                    / paintProps.getView().getExtent().getWidth();

            ModelDisplayAttrs[] modelDisplayAttrs = strmTrkRscData
                    .getModelDisplayAttributes();
            double symScale = (extent.getMaxX() - extent.getMinX()) / 200.0;

            for (ModelDisplayAttrs dispAttrs : modelDisplayAttrs) {

                if (!dispAttrs.enabled || dispAttrs.modelName == null
                        || dispAttrs.modelName.isEmpty()
                        || dispAttrs.color == null) {
                    continue;
                }

                // this will not change unless we are coloring according to the
                // wind speed
                WireFrameDisplayAttrs wfDispAttrs = new WireFrameDisplayAttrs(
                        dispAttrs.color, dispAttrs.lineWidth);

                String modelName = dispAttrs.modelName;

                Collection<StormTrack> coll = tracks
                        .getStormTracksByModel(modelName);
                StormLocation seldFcstHrLoc = null;

                for (StormTrack st : coll) {
                    // statusHandler.debug(" Track
                    // "+st.getStormId().getCycloneNum() + " : "
                    // +st.getTrack().size()+ " points");

                    // NOTE: we can do the below check and it will speed up the
                    // panning a little bit. But it also causes
                    // a bug where the user can zoom in 'too' far so that none
                    // of the track points are in the current view even
                    // though the track passes thru the view. In this case the
                    // track will disappear. We could write smarter code to
                    // determine when to draw a track but for now its not
                    // costing much to just draw them all.
                    //
                    // Determine if we need to draw this track.
                    // if any of the track points are in the current extents,
                    // draw the track
                    // for now this is the easiest thing to do. If it turns out
                    // that
                    // we are still drawing too many tracks offscreen then write
                    // code to clip the
                    // tracks
                    boolean drawTrack = false;

                    for (StormLocation sloc : st.getTrack()) {
                        if (strmTrkRscData.getForecastHourEnable()) {
                            if (sloc.getForecastHour() == strmTrkRscData
                                    .getForecastHour()) {
                                seldFcstHrLoc = sloc;
                            } else {
                                continue;
                            }
                        }

                        double[] posA = descriptor.worldToPixel(new double[] {
                                sloc.getLongitude(), sloc.getLatitude(), 0.0 });

                        if (extent.contains(posA)) {
                            drawTrack = true;
                        }
                    }

// see comment above.
//                  if( !drawTrack ) {
//                      continue;
//                  }

                    // only add this model to the 'legend' if it actually has a
                    // track displayed
                    if (!modelNames.contains("   " + modelName)) {
                        modelNames.add("   " + modelName);
                        colorList.add(dispAttrs.color);
                    }

                    TrackSegmentData trackSegData = new TrackSegmentData(
                            dispAttrs, st.getStormId().getCycloneNum(),
                            symScale);

                    // if only displaying one fcst hr then
                    if (strmTrkRscData.getForecastHourEnable()) {

                        if (seldFcstHrLoc != null) {

                            trackSegData.setSingleLocation(seldFcstHrLoc);

                            // determine the color of the fcst point
                            if (strmTrkRscData.getColorCodeByWindSpeed()) {

                                ColorBar windSpeedClrBar = strmTrkRscData
                                        .getWindSpeedColorBar();
                                Boolean windSpeedCatEnabled[] = strmTrkRscData
                                        .getWindSpeedCatEnable();

                                for (int windSpdIntrvl = 0; windSpdIntrvl < windSpeedClrBar
                                        .getNumIntervals(); windSpdIntrvl++) {

                                    if (!windSpeedCatEnabled[windSpdIntrvl]) {
                                        continue;
                                    }
                                    if (windSpeedClrBar.isValueInInterval(
                                            windSpdIntrvl,
                                            seldFcstHrLoc.getWindMax(),
                                            USCustomary.KNOT)) {
                                    }

                                    wfDispAttrs.color = windSpeedClrBar
                                            .getRGB(windSpdIntrvl);

                                    addTrackToWireframe(wfDispAttrs,
                                            trackSegData);

                                    break;
                                }
                            } else {
                                // color by model, wfDispAttrs already set
                                addTrackToWireframe(wfDispAttrs, trackSegData);
                            }
                        }

                    }
                    // if color coding by the model then the track is one line
                    // segment
                    else if (!strmTrkRscData.getColorCodeByWindSpeed()) {

                        trackSegData.setFullTrack(st.getTrack());

                        addTrackToWireframe(wfDispAttrs, trackSegData);
                    } else {
                        // if color coding by the wind speed we will need to add
                        // the
                        // track line segments to different wireframes based on
                        // the color
                        ColorBar windSpeedClrBar = strmTrkRscData
                                .getWindSpeedColorBar();
                        Boolean windSpeedCatEnabled[] = strmTrkRscData
                                .getWindSpeedCatEnable();

                        for (int windSpdIntrvl = 0; windSpdIntrvl < windSpeedClrBar
                                .getNumIntervals(); windSpdIntrvl++) {

                            if (!windSpeedCatEnabled[windSpdIntrvl]) {
                                continue;
                            }

                            trackSegData.resetTrackSegment();

                            StormLocation prevLoc = null;
                            wfDispAttrs.color = windSpeedClrBar
                                    .getRGB(windSpdIntrvl);

                            // loop thru the track locations and add it
                            for (StormLocation sloc : st.getTrack()) {

                                if (windSpeedClrBar.isValueInInterval(
                                        windSpdIntrvl, sloc.getWindMax(),
                                        USCustomary.KNOT)) {

                                    if (trackSegData.stormLocs.isEmpty()
                                            && prevLoc != null) {

                                        trackSegData.addLocation(prevLoc);
                                    }

                                    trackSegData.addLocation(sloc);

                                    if (sloc == st.getTrack().first()) {
                                        trackSegData.firstSegment = true;
                                    }

                                    if (sloc == st.getTrack().last()) {
                                        trackSegData.lastSegment = true;
                                    }
                                } else {
                                    /*
                                     * if this track points wind speed is not
                                     * for this interval
                                     */

                                    /*
                                     * if this is the end of a segment then add
                                     * the segment to the wireframes
                                     */
                                    if (!trackSegData.stormLocs.isEmpty()) {

                                        addTrackToWireframe(wfDispAttrs,
                                                trackSegData);

                                        trackSegData.resetTrackSegment();
                                    }
                                }

                                prevLoc = sloc;
                            }

                            if (!trackSegData.stormLocs.isEmpty()) {

                                // has to be the last loc
                                trackSegData.lastSegment = true;

                                addTrackToWireframe(wfDispAttrs, trackSegData);
                            }
                        }
                    }
                }
            }

            for (Entry<WireFrameDisplayAttrs, IWireframeShape> entry : wireframesMap
                    .entrySet()) {
                WireFrameDisplayAttrs wfAttrs = entry.getKey();
                IWireframeShape wf = entry.getValue();

                wf.compile();

                currGrpTarget.drawWireframeShape(wf, wfAttrs.color,
                        wfAttrs.lineWidth);
            }

            Collections.reverse(modelNames);
            Collections.reverse(colorList);

            if (!strmTrkRscData.getColorCodeByWindSpeed()
                    && !modelNames.isEmpty()) {

                IExtent screenExtent = paintProps.getView().getExtent();
                IExtent mapExtent = new PixelExtent(
                        descriptor.getGridGeometry().getGridRange());
                double x0 = Math.max(mapExtent.getMinX(), screenExtent.getMinX()
                        + (screenExtent.getWidth() * 0.08));
                double y0 = Math.min(mapExtent.getMaxY(), screenExtent.getMaxY()
                        - (screenExtent.getHeight() * 0.10));

                currGrpTarget.drawStrings(font,
                        modelNames.toArray(new String[0]), x0,
                        y0 /* - offsetY * (modelNames.size()+0.5) */, 0.0,
                        TextStyle.NORMAL, colorList.toArray(new RGB[0]),
                        HorizontalAlignment.RIGHT, VerticalAlignment.BOTTOM);
            }
        }

        public IWireframeShape getWireFrame(WireFrameDisplayAttrs wfKey) {
            // if not created yet create a wireframe and store it
            if (!wireframesMap.containsKey(wfKey)) {
                wireframesMap.put(
                        new WireFrameDisplayAttrs(wfKey.color, wfKey.lineWidth),
                        currGrpTarget.createWireframeShape(false, descriptor));
            }

            return wireframesMap.get(wfKey);
        }

        // just used to get data from paint() to addTrackToWireframe
        private class TrackSegmentData {
            private String modelName = "";

            private String cycloneID = "";

            private RGB color;

            private Integer lineWidth;

            private Float symbolSize;

            // based on the extents.
            private double symbolScale;

            private boolean firstSegment = false;

            private boolean lastSegment = false;

            private SortedSet<StormLocation> stormLocs;

            public TrackSegmentData(ModelDisplayAttrs mda, String cycId,
                    double symScale) {
                modelName = mda.modelName;
                cycloneID = cycId;
                color = mda.color;
                lineWidth = mda.lineWidth;
                symbolSize = mda.symbolSize;
                symbolScale = symScale;
                stormLocs = new TreeSet<>();
            }

            public void addLocation(StormLocation sloc) {
                stormLocs.add(sloc);
            }

            public void setSingleLocation(StormLocation sloc) {
                stormLocs.add(sloc);
                firstSegment = true;
                lastSegment = true;
            }

            public void setFullTrack(SortedSet<StormLocation> allLocs) {
                stormLocs = allLocs;
                firstSegment = true;
                lastSegment = true;
            }

            public void resetTrackSegment() {
                stormLocs.clear();
                firstSegment = false;
                lastSegment = false;
            }
        }

        // add the coords and any labels needed for the storm track or track
        // segment
        //
        private void addTrackToWireframe(WireFrameDisplayAttrs wfDispAttrs, // IWireframeShape
                                                                            // wf,
                TrackSegmentData trackData) {

            IWireframeShape wf = getWireFrame(wfDispAttrs);
            CoordinateList trackCoords = new CoordinateList();

            for (StormLocation sloc : trackData.stormLocs) {

                trackCoords.add(
                        new Coordinate(sloc.getLongitude(), sloc.getLatitude()),
                        false);

                double[] posA = descriptor.worldToPixel(new double[] {
                        sloc.getLongitude(), sloc.getLatitude(), 0.0 });

                PixelCoordinate pos = new PixelCoordinate(posA);
                double[] pressLabelPos = new double[] { posA[0], posA[1] };
                double[] windSpeedLabelPos = new double[] { posA[0], posA[1] };
                double[] fcstLabelPos = new double[] { posA[0], posA[1] };
                double[] dateLabelPos = new double[] { posA[0], posA[1] };
                double[] modelLabelPos = new double[] { posA[0], posA[1] };

                if (strmTrkRscData.getDrawMarker()) {
                    // draw markers
                    // double xLength = (extent.getMaxX() - extent.getMinX()) /
                    // 200.0;
                    double markerSize = (trackData.symbolScale
                            * trackData.symbolSize) / 3;

                    double[][] plusLine = new double[2][2];
                    plusLine[0][0] = posA[0] - markerSize;
                    plusLine[0][1] = posA[1];
                    plusLine[1][0] = posA[0] + markerSize;
                    plusLine[1][1] = posA[1];

                    wf.addLineSegment(plusLine);

                    plusLine[0][0] = posA[0];
                    plusLine[0][1] = posA[1] - markerSize;
                    plusLine[1][0] = posA[0];
                    plusLine[1][1] = posA[1] + markerSize;

                    wf.addLineSegment(plusLine);

                    pressLabelPos[0] = posA[0] + markerSize;
                    windSpeedLabelPos[0] = posA[0] + markerSize;
                    fcstLabelPos[0] = posA[0] - markerSize;
                    dateLabelPos[1] = posA[1] + markerSize;
                    modelLabelPos[1] = posA[1] + markerSize;
                }

                double offsetY = charHeight / screenToWorldRatio;
                double offsetX = charWidth / screenToWorldRatio;

                if (strmTrkRscData.getDrawPressure()) {
                    // draw pressure labels
                    String presLabel = Integer.toString((int) sloc.getMslp());
                    pressLabelPos[0] += (presLabel.length() / 2)
                            * (charWidth / screenToWorldRatio);

                    wf.addLabel(presLabel, pressLabelPos);
                }

                if (strmTrkRscData.getDrawWindSpeed()) {
                    // draw pressure labels
                    String wsLabel = Float.toString(sloc.getWindMax()) + "kts";
                    windSpeedLabelPos[0] += (wsLabel.length() / 2)
                            * (charWidth / screenToWorldRatio);

                    wf.addLabel(wsLabel, windSpeedLabelPos);
                }

                if (strmTrkRscData.getDrawForecastHour()) {
                    // draw the forecast hour
                    String fcstLabel = Integer.toString(sloc.getForecastHour());
                    fcstLabelPos[0] -= (fcstLabel.length() / 2)
                            * (charWidth / screenToWorldRatio);

                    wf.addLabel(fcstLabel, fcstLabelPos);
                }

                // draw time of the first point
//              if( strmTrkRscData.getDrawEndDateTime() &&
//                  sloc == trackData.stormLocs.first() &&
//                  trackData.lastSegment ) {

                if (trackData.firstSegment
                        && sloc == trackData.stormLocs.first()) {

                    String modelCyclLabel = "";

                    if (strmTrkRscData.getDrawModelName()
                            || strmTrkRscData.getDrawCycloneID()) {

                        if (strmTrkRscData.getDrawModelName()
                                && strmTrkRscData.getDrawCycloneID()) {
                            modelCyclLabel = trackData.modelName + "-"
                                    + trackData.cycloneID;
                        } else if (!strmTrkRscData.getDrawModelName()) {
                            modelCyclLabel = trackData.cycloneID;
                        } else {
                            modelCyclLabel = trackData.modelName;
                        }

                        modelLabelPos[1] += (charHeight * .5
                                / screenToWorldRatio);

                        wf.addLabel(modelCyclLabel, modelLabelPos);
                    }

                    // if displaying the date/time then
                    //
                    if (strmTrkRscData.getDrawBeginDateTime()) {

                        DataTime endTime = new DataTime(
                                getFrameTime().getRefTime(),
                                sloc.getForecastHour() * 3600);
                        Calendar endTimeC = endTime.getValidTime();
                        String endDateS = String.format("%02d",
                                endTimeC.get(Calendar.DAY_OF_MONTH));

                        String endHourS = String.format("%02d",
                                endTimeC.get(Calendar.HOUR_OF_DAY));

                        String startTimeLabel = endDateS + "/" + endHourS;

                        dateLabelPos[1] += (charHeight * .5
                                / screenToWorldRatio);

                        if (!modelCyclLabel.isEmpty()) {
                            dateLabelPos[1] += charHeight / screenToWorldRatio;
                        }
                        wf.addLabel(startTimeLabel, dateLabelPos);
                    }
                }
            }

            if (trackCoords.size() > 1) {
                wf.addLineSegment(trackCoords.toCoordinateArray());
            }
        }

        @Override
        public void dispose() {
            tracks.clear();
            for (IWireframeShape wf : wireframesMap.values()) {
                wf.dispose();
            }

            wireframesMap.clear();
        }
    }

    @Override
    public void project(CoordinateReferenceSystem crs) throws VizException {
        for (AbstractFrameData fd : frameDataMap.values()) {
            ((FrameData) fd).recreateWireframes = true;
        }
    }

    protected StormTrackResource(StormTrackResourceData resourceData,
            LoadProperties props) {
        super(resourceData, props);

        strmTrkRscData = resourceData;
    }

    @Override
    protected AbstractFrameData createNewFrame(DataTime frameTime,
            int timeInt) {
        return new FrameData(frameTime, timeInt);
    }

    @Override
    public void initResource(IGraphicsTarget graphicsTarget)
            throws VizException {

        if (font == null) {
            font = graphicsTarget.initializeFont("Monospace", 9,
                    new IFont.Style[] { IFont.Style.BOLD });
            Rectangle2D charSize = graphicsTarget.getStringBounds(font, "N");
            charHeight = charSize.getHeight();
            charWidth = charSize.getWidth();
        }

        // if displaying the full storm tracks we will need to override the
        // default time matching
        // mode we will time match based on the valid time with the forecast
        // hour.
    }

//  private String createQueryScriptForPython(String frameStartTimeStr, String frameEndTimeStr ){
//      StringBuilder query = new StringBuilder();
//      query.append("import StormTrackTrackRequest\n");
//      query.append("req = StormTrackTrackRequest.StormTrackTrackRequest()\n");
//      /*A single quote succeeds the '(' character and precedes the ')' character
//       * so that the python script recognizes it as an input string*/
//        if ( frameStartTimeStr != null && !frameStartTimeStr.isEmpty() ){
//         query.append("req.setStartTime('"+frameStartTimeStr+"')\n" );
//        }else{
//            query.append("req.setStartTime()\n");
//        }
//        if ( frameEndTimeStr != null &&  ! frameEndTimeStr.isEmpty() ){
//             query.append("req.setEndTime('"+frameEndTimeStr+"')\n" );
//        }else{
//            query.append("req.setEndTime()\n");
//        }
//        query.append("return req.execute()\n");
//      return query.toString();
//  }

    public PixelCoordinate convertFloatToPixelCoordinate(float aLat,
            float aLon) {
        Coordinate worldCoord = new Coordinate(aLon, aLat);
        double[] thisWorldPixDblArray = { worldCoord.x, worldCoord.y };
        double[] pixelArr = descriptor.worldToPixel(thisWorldPixDblArray);

        return new PixelCoordinate(pixelArr);
    }

    /***
     * Converts each Lat/Lon point in the array to an equivalent PixelCoordinate
     * object.
     *
     * @param latPointArray
     *            - the input array of latitude points
     * @param lonPointArray
     *            - the input array of longitude points
     * @return the array of PixelCoordinate objects
     */
    public PixelCoordinate[] convertTrackLocationPointsToPixelCoordinateArray(
            float latPointArray[], float lonPointArray[]) {
        PixelCoordinate pixCoordArray[] = null;

        if (latPointArray != null && lonPointArray != null
                && latPointArray.length > 0 && lonPointArray.length > 0
                && latPointArray.length == lonPointArray.length) {
            pixCoordArray = new PixelCoordinate[latPointArray.length];
            for (int index = 0; index < latPointArray.length; index++) {
                pixCoordArray[index] = convertFloatToPixelCoordinate(
                        latPointArray[index], lonPointArray[index]);
            }
        }
        return pixCoordArray;
    }

    /***
     * Repaints the StormTrack tracks
     *
     * @param frameData
     *            - the frame to be rendered
     * @param graphicsTarget
     *            - the graphics target
     * @param paintProps
     *            - the pain properties
     */
    @Override
    public void paintFrame(AbstractFrameData frmData,
            IGraphicsTarget graphicsTarget, PaintProperties paintProps)
            throws VizException {
        FrameData frameData = (FrameData) frmData;
        frameData.currGrpTarget = graphicsTarget;
        frameData.paint(paintProps);
    }

    @Override
    protected void disposeInternal() {
        // statusHandler.debug("Disposing StormTrack...");
        super.disposeInternal();
        if (font != null) {
            font.dispose();
        }
    }

    @Override
    public void resourceAttrsModified() {
        for (AbstractFrameData fd : frameDataMap.values()) {
            ((FrameData) fd).recreateWireframes = true;
        }
    }

    @Override
    public void propertiesChanged(ResourceProperties updatedProps) {
        if (cbarRscPair != null) {
            cbarRscPair.getProperties().setVisible(updatedProps.isVisible());
        }
    }
}
