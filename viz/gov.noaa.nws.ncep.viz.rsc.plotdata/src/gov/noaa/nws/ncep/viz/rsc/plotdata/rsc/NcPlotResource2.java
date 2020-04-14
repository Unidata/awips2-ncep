package gov.noaa.nws.ncep.viz.rsc.plotdata.rsc;

import static java.lang.System.out;
import gov.noaa.nws.ncep.ui.pgen.display.DisplayElementFactory;
import gov.noaa.nws.ncep.ui.pgen.display.IDisplayable;
import gov.noaa.nws.ncep.ui.pgen.display.IVector;
import gov.noaa.nws.ncep.ui.pgen.elements.SymbolLocationSet;
import gov.noaa.nws.ncep.viz.common.Activator;
import gov.noaa.nws.ncep.viz.common.ui.NmapCommon;
import gov.noaa.nws.ncep.viz.common.ui.color.GempakColor;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.attributes.ResourceAttrSet;
import gov.noaa.nws.ncep.viz.resources.attributes.ResourceAttrSet.RscAttrValue;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceName;
import gov.noaa.nws.ncep.viz.rsc.plotdata.PointDataDisplayPreferences;
import gov.noaa.nws.ncep.viz.rsc.plotdata.parameters.PlotParameterDefns;
import gov.noaa.nws.ncep.viz.rsc.plotdata.parameters.PlotParameterDefnsMngr;
import gov.noaa.nws.ncep.viz.rsc.plotdata.plotModels.IMetParamRetrievalListener;
import gov.noaa.nws.ncep.viz.rsc.plotdata.plotModels.IPointInfoRenderingListener;
import gov.noaa.nws.ncep.viz.rsc.plotdata.plotModels.NcPlotDataRequestor;
import gov.noaa.nws.ncep.viz.rsc.plotdata.plotModels.NcPlotImageCreator;
import gov.noaa.nws.ncep.viz.rsc.plotdata.plotModels.StaticPlotInfoPV;
import gov.noaa.nws.ncep.viz.rsc.plotdata.plotModels.StaticPlotInfoPV.SPIEntry;
import gov.noaa.nws.ncep.viz.rsc.plotdata.plotModels.elements.PlotModel;
import gov.noaa.nws.ncep.viz.rsc.plotdata.plotModels.elements.PlotModelElement;
import gov.noaa.nws.ncep.viz.rsc.plotdata.progdisc.ProgressiveDisclosure;
import gov.noaa.nws.ncep.viz.rsc.plotdata.progdisc.ProgressiveDisclosure.IProgDiscListener;
import gov.noaa.nws.ncep.viz.rsc.plotdata.queue.QueueEntry;
import gov.noaa.nws.ncep.viz.ui.display.NCMapDescriptor;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;

import java.awt.Color;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.DrawableString;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.HorizontalAlignment;
import com.raytheon.uf.viz.core.IGraphicsTarget.TextStyle;
import com.raytheon.uf.viz.core.IGraphicsTarget.VerticalAlignment;
import com.raytheon.uf.viz.core.PixelExtent;
import com.raytheon.uf.viz.core.catalog.DirectDbQuery;
import com.raytheon.uf.viz.core.catalog.DirectDbQuery.QueryLanguage;
import com.raytheon.uf.viz.core.drawables.IFont;
import com.raytheon.uf.viz.core.drawables.IFont.Style;
import com.raytheon.uf.viz.core.drawables.IImage;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.jobs.JobPool;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.viz.pointdata.IPlotModelGeneratorCaller;
import com.raytheon.viz.pointdata.PlotAlertParser;
import com.raytheon.viz.pointdata.PlotInfo;
import com.raytheon.viz.pointdata.def.ConditionalFilter;
import com.raytheon.viz.pointdata.rsc.retrieve.PointDataPlotInfoRetriever;
import org.locationtech.jts.geom.Coordinate;

/**
 * Provides a resource that will display plot data for a given reference time.
 * 
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 *  11/20/2006             brockwoo    Initial creation.
 *  02/17/2009             njensen     Refactored to new rsc architecture.
 *  03/17/2009      2105   jsanchez    Plot goessounding/poessounding availability.
 *  03/30/2009      2169   jsanchez    Updated initNewFrame.
 *  04/09/2009       952   jsanchez    Plot acars.   
 *  04/13/2009      2251   jsanchez    Plot profilers. 
 *  04/21/2009             chammack    Refactor to common pointData model
 *  04/28/2010     #275    ghull       Refactor raytn's class to work with 
 *                                     AbstractNatlCntrsResource
 *  07/28/2010	   #291	   gzhang	   corrected Plot position in paintFrame()   
 *  10/04/2010     #307    ghull       PlotInfoRscDataObj wrapper for PlotInfo            
 *  03/07/2011     migration ghull     use AbstractDbPlotInfoRetriever; for now we are not 
 *                                     using the resourceChanged version of getStations.
 *  04/25/2011     n/a     bhebbard    Check for null station.distValue in run()
 *  04/27/2001	   #361    xguo        Display parameter list
 *  09/14/2011     #457    sgurung     Renamed H5 to nc
 *  09/20/2011     #459    ghull       use lat,lon as map key instead of stationId since
 *                                     the stationId is no longer uniq in all cases.
 *  10/19/2011             ghull       remove special ncuair PlotInfoRetriever and AlertParsers.                                 
 *  11/01/2011     #482    ghull       progressive disclosure fixes, rm mesowest/metar enabled. 
 *  12/05/2011             sgurung     Added method isStationMapEmpty
 *  12/07/2011     #529    bhebbard    Change "plotAll" criteria for new user "Plot All" option
 *  12/16/2011     #529    bhebbard    Suppress (for now) pre-draw check for non-timematching
 *                                     stations, due to undesirable "blinking" effect
 *  02/16/2012     #555    sgurung     Changed setPopulated() to setPopulated(true) in populateFrame().
 *  02/16/2012     #639    Q.Zhou      Changed maxDensity to 3.0(Could do 4 or 5 if needed)
 *  04/02/2012     #615    sgurung     Use modified version of PlotModelGenerator2 constructor
 *  05/18/2012     #809    sgurung     Use a separate PlotModelGenerator2 thread to create plots for stations
 *  								   within a predefined Data Area (from Preferences) but outside
 *  								   of the current display area to improve panning performance
 *  05/23/2012     785     Q. Zhou     Added getName for legend.
 *  08/22/2012     #809    sgurung     For bgGenerator thread, add stations to queue only when zoomLevel > 0.10
 *  								   (this fixes the issue of slow performance when zooming all the way in, when Data Area is set)
 *  10/18/2012     896     sgurung     Refactored PlotResource2 to use new generator class: NcPlotDataThreadPool. Added FrameLoaderJob to populate all frames.
 *  								   Added code to plot stations within 25% of the area outside of the current display area.
 *  05/20/2013     988     Archana.S   Refactored this class for performance improvement	
 *  11/07/2013             sgurung     Added fix for "no data for every other frame" issue (earlier fix was added to 13.5.2 on 10/24/2013)
 *  03/18/2013    1064     B. Hebbard  Added handling of matrixType request constraint, for PAFM
 *  06/24/2014    1009     kbugenhagen Reload framedata if no stations found
 *  07/08/2014 TTR1028     B. Hebbard  Modified paintFrame() to return right away if prog disc in progress, instead of
 *                                     allowing multiple PD/HDF5/image trains to run concurrently for the same frame
 *                                     in case of pan/zoom continuing after PD launched.  Improves performance and
 *                                     reduces 'trickling in' of stations after pan/zoom.  Also changed resourceAttrsModified()
 *                                     to remove all stations' met parameter data if requeryDataAndReCreateAllStnImages true,
 *                                     to force re-query (now that we're bypassing stations that already have data).
 *  Aug 08, 2014  3477     bclement    changed plot info locations to floats
 *  09/03/2014    1009     kbugenhagen Reload Framedata.stationMap if all stations' dist values are null
 *  Aug 05, 2015  4486     rjpeter     Changed Timestamp to Date.
 *  08/14/2015   R7757     B. Hebbard  Refactor to connect imageCreator directly to this class, instead of indirectly via dataRequestor;
 *                                     add FrameStatus state tracking (instead of just processing-in-progress boolean)
 *  11/17/2015   R9579     B. Hebbard  Tell DisplayElementFactory to allow filled symbols; move Station class to separate file; cleanups
 *  12/14/2015   R9579     B. Hebbard  In populateStationsForTheFcstPointRscFrames(), prevent array-out-of-bounds condition if getFrameTimes() returns empty
 *  11/29/2017   5863      bsteffen    Change dataTimes to a NavigableSet
 * 
 * </pre>
 * 
 * @author brockwoo
 * @version 1.0
 */
public class NcPlotResource2 extends
        AbstractNatlCntrsResource<PlotResourceData, NCMapDescriptor> implements
        IPlotModelGeneratorCaller, INatlCntrsResource, IProgDiscListener,
        IMetParamRetrievalListener, IPointInfoRenderingListener

{
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(NcPlotResource2.class);

    protected static final float MAX_DENSITY = 3.0f;

    protected static int DEFAULT_PLOT_WIDTH = 105;

    private JobPool frameRetrievalPool = null;

    private ConcurrentLinkedQueue<SpecialQueueEntry> queueOfFrameTimesAndStations = null;

    private ProgressiveDisclosure progressiveDisclosure = null;

    private ResourceName rscName = null;

    private String cycleTimeStr = null;

    private boolean isFcst = false;

    private double plotDensity = Double.MIN_NORMAL;

    private double plotWidth = Double.MIN_NORMAL;

    private PixelExtent worldExtent;

    private NcPlotDataRequestor dataRequestor = null;

    public NcPlotImageCreator imageCreator = null;

    public StaticPlotInfoPV spi;

    private FrameLoaderTask frameLoaderTask;

    private FcstFrameLoaderTask fcstFrameLoaderTask = null;

    private TimeLogger timeLogger;

    private HashMap<String, RequestConstraint> metadataMap;

    private NCMapDescriptor mapDescriptor;

    Rectangle canvasBounds = null;

    private PlotModel plotModel = null;

    private int existingLevel = -1;

    private ConditionalFilter existingCondFilter = null;

    protected PlotResourceData plotRscData = null;

    boolean matchFound = false;

    boolean dataRequeryNeeded = false;

    boolean onlyReCreateExistingImages = false;

    boolean densityChanged = false;

    boolean isThereAConditionalFilter = false;

    PaintProperties currPaintProp;

    RGB rgb = new RGB(200, 200, 0);

    private DisplayElementFactory df = null;

    List<SymbolLocationSet> listOfSymbolLocationSet = new ArrayList<SymbolLocationSet>();

    private static boolean frameStatusDisplayEnabled = false;

    private static boolean frameStatusDisplayVerbose = false;

    private static IFont frameStatusDisplayFont = null;

    protected class PlotInfoRscDataObj implements IRscDataObject {
        private PlotInfo plotInfo;

        public PlotInfoRscDataObj(PlotInfo pltInfo) {
            plotInfo = pltInfo;
        }

        @Override
        public DataTime getDataTime() {
            return plotInfo.dataTime;
        }

        public PlotInfo getPlotInfo() {
            return plotInfo;
        }
    }

    protected class FcstFrameLoaderTask implements Runnable {
        DataTime frameTime;

        DataTime stationTimeToSet;

        Collection<Station> stationList;

        FcstFrameLoaderTask(DataTime frameTimeToMatch, DataTime stnTimeToSet,
                Collection<Station> listOfStations) {
            Tracer.print("> Entry  " + Tracer.shortTimeString(frameTimeToMatch));
            frameTime = frameTimeToMatch;
            stationTimeToSet = stnTimeToSet;
            stationList = listOfStations;
            Tracer.print("< Exit");
        }

        @Override
        public void run() {
            Tracer.print("> Entry  START FcstFrameLoaderTask TASK "
                    + Tracer.shortTimeString(frameTime));
            String trialKey = null;
            synchronized (frameTime) {
                FrameData frameData = (FrameData) getFrame(frameTime);
                if ((frameData != null && frameData.stationMap != null && frameData.stationMap
                        .isEmpty())
                        && (stationList != null && !stationList.isEmpty())
                        && (frameTime != null)) {

                    // Except for the first frame...
                    if (!getFrameTimes().get(0).equals(frameTime)) {
                        synchronized (stationList) {
                            for (Station station : stationList) {
                                String stnKey = getStationMapKey(
                                        station.info.latitude,
                                        station.info.longitude);

                                if (trialKey == null) {
                                    trialKey = new String(stnKey);
                                }
                                Station newStation = new Station();
                                newStation.info = new PlotInfo(
                                        station.info.stationId,
                                        station.info.latitude,
                                        station.info.longitude,
                                        stationTimeToSet, null);
                                newStation.distanceValue = new Double(
                                        station.distanceValue.doubleValue());
                                newStation.originalDistanceValue = new Double(
                                        station.originalDistanceValue
                                                .doubleValue());
                                newStation.goodnessValue = new Integer(
                                        station.goodnessValue.intValue());

                                synchronized (frameData.stationMap) {
                                    frameData.stationMap
                                            .put(stnKey, newStation);
                                }
                            }

                            Tracer.print("FcstFrameLoaderTask - for frame "
                                    + frameData.getShortFrameTime()
                                    + " the station datatime is: "
                                    + frameData.stationMap.get(trialKey).info.dataTime);
                        }
                    }

                    Tracer.print("From FcstFrameLoaderTask - about to queue frame: "
                            + frameData.getShortFrameTime() + " for PD");
                    progressiveDisclosure.setDensity(plotDensity);
                    synchronized (frameData.stationMap) {
                        Tracer.print("For frame "
                                + frameData.getShortFrameTime()
                                + " the size of stationMap - "
                                + frameData.stationMap.size());
                        frameData.setFrameStatus(FrameStatus.PROGDISC);
                        progressiveDisclosure.queueListOfStationsToBeDisclosed(
                                frameTime, frameData.stationMap.values());
                        issueRefresh();
                    }
                }
            }
            Tracer.print("< Exit  END TASK "
                    + Tracer.shortTimeString(frameTime));
        }
    }

    protected class FrameLoaderTask implements Runnable {

        DataTime dataTime;

        HashMap<String, RequestConstraint> frameLoaderTaskMetadataMap;

        FrameLoaderTask(DataTime dt) {
            dataTime = dt;
            frameLoaderTaskMetadataMap = new HashMap<String, RequestConstraint>(
                    metadataMap);
        }

        @Override
        public void run() {
            Tracer.print("> Entry  START FrameLoaderTask TASK "
                    + Tracer.shortTimeString(dataTime));
            Tracer.print("About to run postgres query for frame: "
                    + Tracer.shortTimeString(dataTime));

            FrameData frameData = (FrameData) getFrame(dataTime);
            frameData.setFrameStatus(FrameStatus.LOADING); // already set?
            RequestConstraint timeConstraint = new RequestConstraint();

            if (!isFcst) {
                String[] constraintList = {
                        frameData.getFrameStartTime().toString(),
                        frameData.getFrameEndTime().toString() };
                timeConstraint.setBetweenValueList(constraintList);
                timeConstraint
                        .setConstraintType(RequestConstraint.ConstraintType.BETWEEN);
                frameLoaderTaskMetadataMap.put("dataTime", timeConstraint);
            }

            try {
                Tracer.printX("frameLoaderTaskMetadataMap = "
                        + frameLoaderTaskMetadataMap);

                frameData.plotInfoObjs = plotRscData.getPlotInfoRetriever()
                        .getStations(frameLoaderTaskMetadataMap);

                if (frameData.plotInfoObjs != null
                        && !frameData.plotInfoObjs.isEmpty()) {
                    Tracer.print("PointDataPlotInfoRetriever.getStations returned "
                            + frameData.plotInfoObjs.size()
                            + " stations for frame "
                            + frameData.getShortFrameTime());
                    synchronized (frameData.plotInfoObjs) {

                        boolean isFramePopulated = frameData.populateFrame();
                        if (isFramePopulated) {
                            Collection<Station> stationsToBeQueuedForProgDisc = progressiveDisclosure
                                    .calculateStaticProgDiscDistancesForStations(
                                            frameData.stationMap.values(),
                                            frameData.dynStations);
                            if (stationsToBeQueuedForProgDisc == null
                                    || stationsToBeQueuedForProgDisc.isEmpty()) {
                                // Frame is READY to paint -- because it's
                                // empty!
                                frameData.setFrameStatus(FrameStatus.READY);
                            } else {
                                synchronized (stationsToBeQueuedForProgDisc) {
                                    for (Station stn : stationsToBeQueuedForProgDisc) {
                                        String stnMapKey = getStationMapKey(
                                                stn.info.latitude,
                                                stn.info.longitude);
                                        Station frameStn = frameData.stationMap
                                                .get(stnMapKey);
                                        frameStn.distanceValue = stn.distanceValue;
                                        synchronized (frameData.stationMap) {
                                            frameData.stationMap.put(stnMapKey,
                                                    frameStn);
                                        }
                                    }

                                    Tracer.print("About to schedule the frame: "
                                            + frameData.getShortFrameTime()
                                            + " for progressive disclosure");
                                    frameData
                                            .setFrameStatus(FrameStatus.PROGDISC);
                                    progressiveDisclosure
                                            .queueListOfStationsToBeDisclosed(
                                                    frameData.getFrameTime(),
                                                    stationsToBeQueuedForProgDisc);

                                    issueRefresh();

                                }
                            }

                        } else {
                            Tracer.print("frameData.populateFrame() returned FALSE for frame "
                                    + frameData.getShortFrameTime());
                            frameData.setFrameStatus(FrameStatus.READY); // empty
                        }

                    }

                } else {
                    Tracer.print("PointDataPlotInfoRetriever.getStations returned null or empty station list for frame "
                            + frameData.getShortFrameTime());
                    frameData.setFrameStatus(FrameStatus.READY); // empty
                }

            } catch (VizException e) {
                e.printStackTrace();
            }
            Tracer.print("< Exit   END TASK   " + frameData.getShortFrameTime());

        }
    }

    private class SpecialQueueEntry extends QueueEntry {
        DataTime stationTimeToSet;

        SpecialQueueEntry(DataTime frameTime, DataTime stnTimeToSet,
                Collection<Station> stnCollection) {
            super(frameTime, stnCollection);
            stationTimeToSet = stnTimeToSet;
        }
    }

    public enum FrameStatus {

        // Frame Status Tracking

        // Each frame progresses through the following states as it becomes
        // ready to paint all the stations it needs to. Progression is in this
        // order, although reprocessing occurs often and may start in the
        // middle, depending on what work needs to be done. (For example, after
        // zooming in or out, we start over at PROGDISC.)

        // This is used internally to track frame states as processing is handed
        // off to various Jobs (threads). As a diagnostic tool for developers,
        // an optional realtime display of all frame states can be activated via
        // CAVE NCEP preferences.

        // @formatter:off
        NEW       (GempakColor.WHITE,  "No Stations Loaded Yet"), 
        LOADING   (GempakColor.RED,    "Loading All Time-matched Stations"), 
        PROGDISC  (GempakColor.ORANGE, "Filtering by Progressive Disclosure and Extent"), 
        METPARAMS (GempakColor.YELLOW, "Retrieving Met Parameters"), 
        IMAGING   (GempakColor.CYAN,   "Creating Plot Image"), 
        READY     (GempakColor.GREEN,  "Ready to Paint");
        // @formatter:on

        private final int maxOrdinal = 5; // READY.ordinal()

        // Constructor
        private FrameStatus(GempakColor gempakColorCode, String description) {
            this.colorCode = gempakColorCode.getRGB();
            this.description = description;
            this.spacing = "";
            for (int i = 0; i < maxOrdinal - ordinal(); i++) {
                spacing += "        ";
            }
            if (ordinal() < maxOrdinal) {
                spacing += "< ";
            }
        }

        private RGB colorCode;

        private String description;

        private String spacing;

        public RGB getColorCode() {
            return colorCode;
        }

        public String getDescription() {
            return description;
        }

        public String getSpacing() {
            return spacing;
        }

    }

    public class FrameData extends AbstractFrameData {

        private FrameStatus frameStatus = FrameStatus.NEW;

        // map from the station Id to the station info (plotInfo and image)
        // (using ConcurrentHashMap to take care of
        // ConcurrentModificationException)
        private Map<String, Station> stationMap = new ConcurrentHashMap<String, Station>();

        private List<PlotInfo> plotInfoObjs = new ArrayList<PlotInfo>();

        private boolean isFramePaintedFirstTime = false;

        private int dynStations;

        private boolean progDiscCalculated;

        private boolean screenExtentsChangedForCurrentFrame = false;

        private List<DrawableString> drawableStrings = null;

        private List<IVector> drawableVectors = null;

        private List<SymbolLocationSet> drawableSymbolSets = null;

        private Set<Station> stationsLastRendered = null;

        public int disclosedStationsCount;

        public int stationsRetrievedThisCallCount;

        protected FrameData(DataTime time, int interval) {
            super(time, interval);
            Tracer.print("> Entry  " + getShortFrameTime());
            dynStations = 0;
            drawableStrings = new ArrayList<DrawableString>(0);
            drawableVectors = new ArrayList<IVector>(0);
            drawableSymbolSets = new ArrayList<SymbolLocationSet>(0);
            stationsLastRendered = new HashSet<Station>(0);
            Tracer.print("< Exit   " + getShortFrameTime());
        }

        @Override
        public boolean updateFrameData(IRscDataObject rscDataObj) {
            Tracer.printX("> Entry  " + getShortFrameTime());

            if (!(rscDataObj instanceof PlotInfoRscDataObj)) { // sanity check
                return false;
            }

            PlotInfo plotInfo = ((PlotInfoRscDataObj) rscDataObj).getPlotInfo();
            if (plotInfo.dataTime == null) {
                plotInfo.dataTime = getFrameTime();
                out.println("dataTime from plotInfo is null. Setting to FrameTime");
            }

            /*
             * This check is to remove stations that have been decoded and
             * stored in the database with missing/invalid lat/lon values
             */

            if (plotInfo.latitude < -90 || plotInfo.latitude > 90
                    || plotInfo.longitude < -180 || plotInfo.longitude > 180) {
                return false;
            }

            String stnMapKey = getStationMapKey(plotInfo.latitude,
                    plotInfo.longitude);

            Station stn = new Station();
            if (stnMapKey != null) {
                stn = stationMap.get(stnMapKey);
            }

            // This can happen during an auto update or if there are multiple
            // reports for this frame.
            //
            if (stn != null) {

                if (stn.info == null) { // shouldn't happen
                    out.println("Sanity check: Found existing Station in stationMap with a null plotInfo???");
                    return false;// ??
                } else {

                    if (!stn.info.stationId.equals(plotInfo.stationId)) {
                        out.println("2 stations " + stn.info.stationId
                                + " and " + plotInfo.stationId
                                + " have the same location?" + "\nLat = "
                                + stn.info.latitude + ",Lon = "
                                + stn.info.longitude + " for the time: "
                                + stn.info.dataTime);
                    }
                    // if these are the same time, should we check which one
                    // should be used, (or combine them?) How to determine which
                    // to use?
                    else if (stn.info.dataTime.getValidTime().getTimeInMillis() != plotInfo.dataTime
                            .getValidTime().getTimeInMillis()) {

                        if (timeMatch(plotInfo.dataTime) < timeMatch(stn.info.dataTime)) {
                            stn.info = plotInfo;
                        }
                    }
                }

            } else {
                stn = new Station();

                if (plotInfo.stationId == null) {
                    plotInfo.stationId = plotInfo.latitude + "#"
                            + plotInfo.longitude;
                }
                stn.info = plotInfo;

                calcStaticStationInfo(stn);
            }
            Tracer.printX("< Exit  " + getShortFrameTime());

            return true;
        }

        // if we are painting a frame which has not yet been loaded with data
        // then we need to request data for this frame and then update the
        // frames
        protected boolean populateFrame() throws VizException {
            Tracer.print("> Entry  " + getShortFrameTime());

            Tracer.print("Starting populateFrame for " + getShortFrameTime());

            dynStations = 0;

            if (plotInfoObjs == null || plotInfoObjs.isEmpty()) {
                return false;
            }

            for (PlotInfo pltInfo : plotInfoObjs) {

                // have to add this since the dataTime is not always getting set
                // by the NcPlotDataRequestor
                if (pltInfo.dataTime == null) {
                    if (plotRscData.isSurfaceOnly()) {
                        pltInfo.dataTime = getFrameTime();
                    } else {
                        out.println("Error getting dataTime from Plot Query");
                    }
                }

                for (IRscDataObject rscDataObj : processRecord(pltInfo)) {
                    // Sanity Check: this should always be true since we
                    // constrained the query with the start/end frame times.
                    if (isRscDataObjInFrame(rscDataObj)) {
                        updateFrameData(rscDataObj);
                    }
                }
            }

            setPopulated((this.stationMap != null)
                    && (this.stationMap.size() > 0));

            Tracer.print("Ending populateFrame for " + getShortFrameTime()
                    + " -- " + (populated ? this.stationMap.size() : "NO")
                    + " stations");
            Tracer.print("< Exit  " + getShortFrameTime());

            return this.populated;
        }

        public boolean calcStaticStationInfo(Station station) {
            Tracer.printX("> Entry  " + getShortFrameTime());
            SPIEntry obsStation = null;
            Coordinate thisLocation = null;
            Coordinate thisPixelLocation = null;
            if (spi != null) {
                obsStation = spi.getSPIEntry(station.info.stationId);
            }
            if (obsStation != null) {
                thisLocation = obsStation.latlon;
                double[] thisLocationLatLon = { thisLocation.x, thisLocation.y };
                double[] thisLocationPixel = descriptor
                        .worldToPixel(thisLocationLatLon);
                if (!worldExtent.contains(thisLocationPixel[0],
                        thisLocationPixel[1])) {
                    return false;
                }
                thisPixelLocation = new Coordinate(thisLocationPixel[0],
                        thisLocationPixel[1]);
                /*
                 * Replaced distFloor with minDist to match RTS -
                 * SpiProgDisclosure logic;
                 */
                if (obsStation.distance < progressiveDisclosure.minDist) {
                    station.originalDistanceValue = progressiveDisclosure.minDist;
                } else {
                    station.originalDistanceValue = obsStation.distance;
                }
            } else {
                thisLocation = new Coordinate(station.info.longitude,
                        station.info.latitude);
                double[] thisLocationLatLon = { thisLocation.x, thisLocation.y };
                double[] thisLocationPixel = descriptor
                        .worldToPixel(thisLocationLatLon);
                if (thisLocationPixel == null
                        || !worldExtent.contains(thisLocationPixel[0],
                                thisLocationPixel[1])) {
                    return false;
                }
                thisPixelLocation = new Coordinate(thisLocationPixel[0],
                        thisLocationPixel[1]);
                station.originalDistanceValue = -1.0;
                dynStations++;
            }

            station.goodnessValue = 0;
            station.pixelLocation = thisPixelLocation;

            String stnMapKey = getStationMapKey(station.info.latitude,
                    station.info.longitude);

            if (stationMap.put(stnMapKey, station) != null) {
                out.println("Updating StationMap with " + stnMapKey);
            }
            Tracer.printX("< Exit   " + getShortFrameTime());
            return true;
        }

        public void clearMetParameters() {
            // TODO synchronize needed?
            Tracer.print("> Entry  " + getShortFrameTime());
            for (Station station : stationMap.values()) {
                station.parametersToPlot.clear();
            }
            Tracer.print("< Exit   " + getShortFrameTime());
        }

        public void dispose() {
            Tracer.print("> Entry  " + getShortFrameTime());
            super.dispose();
            if (stationMap != null && !stationMap.isEmpty()) {
                stationMap.clear();
                stationMap = null;
            }
            if (stationsLastRendered != null && !stationsLastRendered.isEmpty()) {
                stationsLastRendered.clear();
                stationsLastRendered = null;
            }
            if (drawableVectors != null && !drawableVectors.isEmpty()) {
                drawableVectors.clear();
                drawableVectors = null;
            }
            if (drawableSymbolSets != null && !drawableSymbolSets.isEmpty()) {
                drawableSymbolSets.clear();
                drawableSymbolSets = null;
            }
            if (drawableStrings != null && !drawableStrings.isEmpty()) {
                drawableStrings.clear();
                drawableStrings = null;
            }
            if (plotInfoObjs != null && !plotInfoObjs.isEmpty()) {
                plotInfoObjs.clear();
                plotInfoObjs = null;
            }
            isFramePaintedFirstTime = false;
            screenExtentsChangedForCurrentFrame = false;
            setFrameStatus(FrameStatus.NEW);
            populated = false;
            dynStations = 0;
            Tracer.print("< Exit   " + getShortFrameTime());
        }

        public FrameStatus getFrameStatus() {
            return frameStatus;
        }

        public synchronized void setFrameStatus(FrameStatus frameStatus) {
            this.frameStatus = frameStatus;
            Tracer.print("Frame " + getShortFrameTime() + " status set to: "
                    + this.frameStatus);
            if (frameStatusDisplayEnabled) {
                issueRefresh();
            }
        }

        public boolean isFrameProcessingInProgress() {
            return EnumSet.range(FrameStatus.LOADING, FrameStatus.IMAGING)
                    .contains(frameStatus); // that is, between NEW and READY
        }

        public boolean isStationMapEmpty() {
            return stationMap.isEmpty();
        }

        public boolean isProgDiscCalculated() {
            Tracer.printX("> Entry  " + getShortFrameTime());
            Tracer.printX("< Exit   " + getShortFrameTime());
            return progDiscCalculated;
        }

        public void setProgDiscCalculated(boolean p) {
            Tracer.print("> Entry  " + getShortFrameTime() + "  " + p);
            progDiscCalculated = p;
            Tracer.print("< Exit   " + getShortFrameTime());
        }

        public Collection<DrawableString> getStringsToDraw() {
            return drawableStrings;
        }

        public List<IVector> getVectorsToDraw() {
            return drawableVectors;
        }

        public List<SymbolLocationSet> getSymbolsToDraw() {
            return drawableSymbolSets;
        }

        public String getShortFrameTime() {
            // TODO -- Consider making common utility
            String returnString = NmapCommon.getTimeStringFromDataTime(
                    frameTime, "/").substring(4);
            if (frameTime.getFcstTime() != 0) {
                returnString += "(" + frameTime.getFcstTime() + ")";
            }
            return returnString;
        }
    }

    /**
     * Create a point data (plot) resource.
     * 
     * @param target
     *            The graphic target to draw to
     * @param refTime
     *            The reference time to request data against
     * @throws VizException
     */
    public NcPlotResource2(PlotResourceData data, LoadProperties props) {
        super(data, props);
        Tracer.print("> Entry");
        plotRscData = data;
        // The object which is called by the NcAutoUpdater to get a PlotInfo
        // object from the alert URI.
        if (plotRscData.getAlertParser() == null) {
            plotRscData.setAlertParser(new PlotAlertParser());
        }

        if (plotRscData.getPlotInfoRetriever() == null) {
            plotRscData.setPlotInfoRetriever(new PointDataPlotInfoRetriever());
        }

        timeLogger = TimeLogger.getInstance();
        frameRetrievalPool = new JobPool("Querying stations all frames...", 8,
                false);

        rscName = plotRscData.getResourceName();
        isFcst = plotRscData.isForecastResource()
                && !plotRscData.getPluginName().equals("nctaf");

        this.plotWidth = DEFAULT_PLOT_WIDTH;

        setInitialPlotDensityFromRscAttrSet();
        if (plotRscData.getSpiFile() != null
                && !plotRscData.getSpiFile().isEmpty()) {
            this.spi = StaticPlotInfoPV.readStaticPlotInfoPV(plotRscData
                    .getSpiFile());
        }
        mapDescriptor = (NCMapDescriptor) NcDisplayMngr
                .getActiveNatlCntrsEditor().getActiveDisplayPane()
                .getRenderableDisplay().getDescriptor();
        // TODO: what if no SPI file exists?

        if ((progressiveDisclosure == null) && (spi != null)) {
            progressiveDisclosure = new ProgressiveDisclosure(this, spi);
            progressiveDisclosure.setPixelSizeHint(plotRscData
                    .getPixelSizeHint());
            progressiveDisclosure.setPlotWidth(plotWidth);
            progressiveDisclosure.setDensity(plotDensity);
            progressiveDisclosure.setMapDescriptor(mapDescriptor);
        }

        metadataMap = new HashMap<String, RequestConstraint>(
                plotRscData.getMetadataMap());
        if (isFcst) {
            String[] dts = rscName.getCycleTime().toString().split(" ");
            cycleTimeStr = new String(dts[0] + " "
                    + dts[1].substring(0, dts[1].length() - 2));
            timeLogger.append("Cycletime is:" + cycleTimeStr + "\n");
            queueOfFrameTimesAndStations = new ConcurrentLinkedQueue<SpecialQueueEntry>();
        }
        plotModel = plotRscData.getPlotModel();
        RscAttrValue levelKeyValue = plotRscData.getRscAttrSet().getRscAttr(
                "levelKey");
        if (levelKeyValue != null) {
            existingLevel = Integer.parseInt((String) levelKeyValue
                    .getAttrValue());
        }
        existingCondFilter = plotRscData.getConditionalFilter();
        dataRequestor = new NcPlotDataRequestor(plotModel,
                plotRscData.levelKey, metadataMap, this, existingCondFilter);
        imageCreator = new NcPlotImageCreator(this, plotModel);
        Tracer.print("< Exit");

    }

    @Override
    public String getName() {
        String legendString = super.getName();
        FrameData fd = (FrameData) getCurrentFrame();

        if (fd == null || fd.getFrameTime() == null || fd.isStationMapEmpty()) {
            return legendString + "-No Data";
        }

        if (legendString == null || legendString.equalsIgnoreCase("")) {
            return "Plot Data";
        } else {
            return legendString
                    + " "
                    + NmapCommon.getTimeStringFromDataTime(fd.getFrameTime(),
                            "/");
        }
    }

    // override to process PlotInfoRscDataObj instead of PlotInfo
    @Override
    protected IRscDataObject[] processRecord(Object pltInfo) {
        Tracer.printX("> Entry");
        if (!(pltInfo instanceof PlotInfo)) {
            out.println("NcPlotResource2.processRecord method expecting PlotInfoRscData objects "
                    + "instead of: " + pltInfo.getClass().getName());
            return new PlotInfoRscDataObj[0];
        }
        Tracer.printX("< Exit");

        return new PlotInfoRscDataObj[] { new PlotInfoRscDataObj(
                (PlotInfo) pltInfo) };
    }

    public void paintFrame(AbstractFrameData fd, final IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {
        FrameData frameData = (FrameData) fd;
        Tracer.printX("> Entry  " + frameData.getShortFrameTime());
        currPaintProp = paintProps;

        if (frameStatusDisplayEnabled) {
            paintFrameStatusDisplay(frameData, target, paintProps);
        }

        if (fd == null) {
            return;
        }

        if (paintProps.isZooming()) {
            Tracer.print("Zooming in progress...aborting paintFrame for "
                    + frameData.getShortFrameTime());
            return;
        }

        if (frameData.isFrameProcessingInProgress()) {
            Tracer.printX("Frame processing in progress...aborting paintFrame for "
                    + frameData.getShortFrameTime());
            return;
        }

        if (progressiveDisclosure == null) {
            // assumes spi is not null...
            progressiveDisclosure = new ProgressiveDisclosure(this, spi);

            Collection<Station> stationsToDisclose = progressiveDisclosure
                    .calculateStaticProgDiscDistancesForStations(
                            frameData.stationMap.values(),
                            frameData.dynStations);
            if (stationsToDisclose != null && !stationsToDisclose.isEmpty()) {

                synchronized (stationsToDisclose) {
                    for (Station stn : stationsToDisclose) {
                        String stnMapKey = getStationMapKey(stn.info.latitude,
                                stn.info.longitude);
                        Station frameStn = frameData.stationMap.get(stnMapKey);
                        frameStn.distanceValue = stn.distanceValue;
                        frameData.stationMap.put(stnMapKey, frameStn);
                    }
                }
                Tracer.print("paintFrame() - about to schedule progressive disclosure the frame: "
                        + frameData.getShortFrameTime());

                frameData.setFrameStatus(FrameStatus.PROGDISC);

                progressiveDisclosure.queueListOfStationsToBeDisclosed(
                        frameData.getFrameTime(), stationsToDisclose);
            }

        }

        synchronized (frameData) {
            Tracer.printX("calling checkAndUpdateProgDisclosureProperties on frame - "
                    + frameData.getShortFrameTime());
            frameData.screenExtentsChangedForCurrentFrame = progressiveDisclosure
                    .checkAndUpdateProgDisclosureProperties();

            if (frameData.screenExtentsChangedForCurrentFrame) {
                Tracer.print("Screen extents changed in  the frame - "
                        + frameData.getShortFrameTime());
                resetFramePaintFlagForAllFrames();
            }

            if (frameData.screenExtentsChangedForCurrentFrame
                    || progressiveDisclosure.updateNextPaint
                    || !frameData.isFramePaintedFirstTime) {

                frameData.isFramePaintedFirstTime = true;

                if (!frameData.stationMap.isEmpty()) {
                    Tracer.print("Calling from paintFrame() - about to schedule progressive disclosure the frame: "
                            + frameData.getShortFrameTime());

                    boolean mapPopulated = false;
                    for (Station station : frameData.stationMap.values()) {
                        if (station.distanceValue != null) {
                            mapPopulated = true;
                            break;
                        }
                    }

                    // load frame data for new incoming (autoupdated) data
                    if (!mapPopulated) {
                        frameLoaderTask = new FrameLoaderTask(
                                frameData.getFrameTime());
                        frameRetrievalPool.schedule(frameLoaderTask);
                        issueRefresh();
                    }

                    frameData.setFrameStatus(FrameStatus.PROGDISC);

                    progressiveDisclosure.queueListOfStationsToBeDisclosed(
                            frameData.getFrameTime(),
                            frameData.stationMap.values());
                } else {
                    Tracer.print("Calling from paintFrame() - no stations in stationMap for frame: "
                            + frameData.getShortFrameTime()
                            + ".  Loading frame data again.");
                    frameLoaderTask = new FrameLoaderTask(
                            frameData.getFrameTime());
                    frameRetrievalPool.schedule(frameLoaderTask);
                    issueRefresh();
                }
            }

        }

        if (frameData.isFrameProcessingInProgress()) {
            Tracer.print("Frame processing in progress...aborting paintFrame for "
                    + frameData.getShortFrameTime());
            return;
        }

        List<IVector> vectorsToDraw = frameData.getVectorsToDraw();

        Collection<DrawableString> stringsToDraw = frameData.getStringsToDraw();
        List<SymbolLocationSet> symbolLocationSetsToDraw = frameData
                .getSymbolsToDraw();

        long t1 = 0;
        long t2 = 0;

        if (symbolLocationSetsToDraw != null
                && !symbolLocationSetsToDraw.isEmpty()) {
            t1 = System.nanoTime();
            synchronized (symbolLocationSetsToDraw) {
                List<IDisplayable> listOfDisplayables = df
                        .createDisplayElements(paintProps,
                                symbolLocationSetsToDraw);
                for (IDisplayable each : listOfDisplayables) {
                    each.draw(target, paintProps);
                    each.dispose();
                }
            }
            t2 = System.nanoTime();
            Tracer.printX("Took " + (t2 - t1) / 1000000
                    + " ms to render the symbols in the frame "
                    + frameData.getShortFrameTime());
        }

        if (stringsToDraw != null && !stringsToDraw.isEmpty()) {
            t1 = System.nanoTime();
            synchronized (stringsToDraw) {
                target.drawStrings(stringsToDraw);
            }

            t2 = System.nanoTime();
            Tracer.printX("Took " + (t2 - t1) / 1000000 + " ms to draw "
                    + frameData.drawableStrings.size()
                    + " strings in the frame " + frameData.getShortFrameTime());
        }

        if (vectorsToDraw != null && !vectorsToDraw.isEmpty()) {
            t1 = System.nanoTime();
            synchronized (vectorsToDraw) {
                List<IDisplayable> displayElsPoint = df.createDisplayElements(
                        vectorsToDraw, paintProps);
                for (IDisplayable each : displayElsPoint) {
                    each.draw(target, paintProps);
                    each.dispose();
                }
            }
            t2 = System.nanoTime();
            Tracer.printX("Took " + (t2 - t1) / 1000000
                    + " ms to draw the vectors in the frame "
                    + frameData.getShortFrameTime());
        }
        Tracer.printX("< Exit");
    }

    public void initResource(IGraphicsTarget aTarget) throws VizException {
        Tracer.print("> Entry");
        setInitialPlotDensityFromRscAttrSet();
        getPointDataPreferences();
        addPointDataPreferencesListener();
        this.worldExtent = new PixelExtent(0, descriptor.getGridGeometry()
                .getGridRange().getHigh(0), 0, descriptor.getGridGeometry()
                .getGridRange().getHigh(1));

        // load/populate all the frames in the frameDataMap
        loadFrameData();
        issueRefresh();
        // Just initialize P/D values, for comparison on future paints (to see
        // if we need to trigger again). (We don't care about the return value.)
        progressiveDisclosure.checkAndUpdateProgDisclosureProperties();
        Tracer.print("< Exit");

    }

    public void getPointDataPreferences() {
        IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();
        frameStatusDisplayEnabled = prefs
                .getBoolean(PointDataDisplayPreferences.ENABLE_FRAME_STATUS_DISPLAY);
        frameStatusDisplayVerbose = prefs
                .getBoolean(PointDataDisplayPreferences.ENABLE_VERBOSE_FRAME_STATUS);
    }

    private void addPointDataPreferencesListener() {
        // If notified of a preference store change while the resource is
        // active, then reload the preferences relating to point data display.
        IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();
        IPropertyChangeListener listener = new IPropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                getPointDataPreferences();
            }
        };
        prefs.addPropertyChangeListener(listener);
    }

    @Override
    public void disposeInternal() {
        Tracer.print("> Entry");
        initialized = false;
        if (progressiveDisclosure != null) {
            progressiveDisclosure.dispose();
            progressiveDisclosure = null;
        }
        if (dataRequestor != null) {
            dataRequestor.dispose();
            dataRequestor = null;
        }
        if (imageCreator != null) {
            imageCreator.dispose();
            imageCreator = null;
        }
        if (frameStatusDisplayFont != null) {
            frameStatusDisplayFont.dispose();
            frameStatusDisplayFont = null;
        }
        clearImages();
        timeLogger.append("\n Clearing the frameMap\n");
        frameDataMap.clear();
        frameRetrievalPool.cancel();
        frameRetrievalPool = null;
        timeLogger.append("\n Clearing the stationsCache\n");
        Tracer.print(timeLogger.toString());
        timeLogger.clearLog();
        Tracer.print("< Exit");
    }

    protected AbstractFrameData createNewFrame(DataTime frameTime, int timeInt) {
        Tracer.print("> Entry  " + Tracer.shortTimeString(frameTime));
        FrameData newFrame = new FrameData(frameTime, timeInt);
        newFrame.setFrameStatus(FrameStatus.NEW);
        if (df == null) { // TODO why do this here??
            df = new DisplayElementFactory(NcDisplayMngr
                    .getActiveNatlCntrsEditor().getActiveDisplayPane()
                    .getTarget(), getDescriptor());
            // Must explicitly tell the DisplayElementFactory that filled
            // elements are allowed, so filled symbols will draw.
            Boolean mono = false;
            Color color = null;
            Boolean fill = true;
            df.setLayerDisplayAttr(mono, color, fill);
        }
        Tracer.print("< Exit   " + Tracer.shortTimeString(frameTime));
        return newFrame;
    }

    @Override
    public void clearImages() {
        Tracer.print("> Entry");
        timeLogger.append("\n Invoking NcplotResource2.clearImages()\n");
        for (AbstractFrameData frameData : frameDataMap.values()) {
            Collection<Station> collStns = ((FrameData) frameData).stationMap
                    .values();
            for (Station station : collStns) {
                if (station.positionToLocationMap != null) {
                    station.positionToLocationMap.clear();
                    station.positionToLocationMap = null;
                }
                if (station.info != null) {
                    station.info.plotQueued = false;
                }
            }
        }
        issueRefresh();
        Tracer.print("< Exit");

    }

    @Override
    public void modelGenerated(PlotInfo[] keys, IImage image) {
        Tracer.print("> Entry  [empty!]");
        Tracer.print("< Exit");
    }

    @Override
    public void project(CoordinateReferenceSystem crs) throws VizException {
        Tracer.print("> Entry");
        this.worldExtent = new PixelExtent(0, descriptor.getGridGeometry()
                .getGridRange().getHigh(0), 0, descriptor.getGridGeometry()
                .getGridRange().getHigh(1));
        issueRefresh();
        Tracer.print("< Exit");
    }

    @Override
    public void resourceAttrsModified() {
        Tracer.print("> Entry");
        dataRequeryNeeded = false;
        onlyReCreateExistingImages = false;
        densityChanged = false;
        ResourceAttrSet rscAttrSet = plotRscData.getRscAttrSet();
        PlotModel editedPlotModel = (PlotModel) rscAttrSet.getRscAttr(
                "plotModel").getAttrValue();
        if (editedPlotModel == null) {
            return;
        }

        RscAttrValue levelKeyAttr = rscAttrSet.getRscAttr("levelKey");
        if (levelKeyAttr != null) {
            String newLevelString = (String) levelKeyAttr.getAttrValue();
            int editedLevel = Integer.parseInt((newLevelString));
            if (editedLevel != existingLevel) { // Level change requested
                dataRequestor.setLevelStr(newLevelString);
                existingLevel = editedLevel;
                dataRequeryNeeded = true;
            }
        }

        double newDensity = ((Integer) rscAttrSet.getRscAttr("plotDensity")
                .getAttrValue()).doubleValue() / 10;
        if (Math.abs(newDensity - plotDensity) > 0.0001) { // TODO constify
            plotDensity = newDensity;
            densityChanged = true;
        }

        boolean areALLCondFilterParamsInEditedPlotModel = false;
        List<PlotModelElement> oldPMEList = this.plotModel
                .getAllPlotModelElements();
        Set<String> setOfPlotParamsPrevPlotted = new HashSet<String>();
        synchronized (oldPMEList) {
            for (PlotModelElement oldPME : oldPMEList) {
                setOfPlotParamsPrevPlotted.add(oldPME.getParamName());
            }
        }

        Set<String> newPlotParamNamesSet = new HashSet<String>();
        List<PlotModelElement> newPMEList = editedPlotModel
                .getAllPlotModelElements();
        Set<String> setOfCondParamsAlreadyInPlotModel = new HashSet<String>(0);

        synchronized (newPMEList) {
            for (PlotModelElement newPME : newPMEList) {
                newPlotParamNamesSet.add(newPME.getParamName());
            }
        }

        RscAttrValue rscAttrValCondFilter = rscAttrSet
                .getRscAttr("conditionalFilter");
        ConditionalFilter cf = null;

        /*
         * Check if there is a current conditional filter
         */
        if (rscAttrValCondFilter != null) {
            cf = (ConditionalFilter) rscAttrValCondFilter.getAttrValue();
            if (cf.getConditionalFilterMap() != null
                    && !cf.getConditionalFilterMap().isEmpty()) {
                dataRequestor.setConditionalFilter(cf);
                dataRequestor.setUpConditionalFilterParameters();
                /*
                 * Replace the existing conditional filter
                 */
                this.existingCondFilter = new ConditionalFilter(cf);
                imageCreator.isThereAConditionalFilter = false;
                isThereAConditionalFilter = true;
                Tracer.print("Added the conditional filter");
                Map<String, RequestConstraint> mapOfCondFilters = dataRequestor
                        .getConditionalFilterMap();
                if (mapOfCondFilters != null && !mapOfCondFilters.isEmpty()) {
                    synchronized (mapOfCondFilters) {
                        Set<String> keySet = mapOfCondFilters.keySet();
                        for (String condParamName : keySet) {
                            synchronized (newPlotParamNamesSet) {
                                if (!newPlotParamNamesSet
                                        .contains(condParamName)) {
                                    areALLCondFilterParamsInEditedPlotModel = false;
                                    break;
                                } else
                                    areALLCondFilterParamsInEditedPlotModel = true;
                            }
                        }
                        if (!areALLCondFilterParamsInEditedPlotModel) {
                            dataRequeryNeeded = true;
                        }
                    }
                }
            } else {
                /*
                 * Else check if there was a previously existing conditional
                 * filter
                 */
                if (isThereAConditionalFilter) {
                    this.existingCondFilter = null;
                    dataRequestor.setConditionalFilter(null);
                    dataRequestor
                            .updateConditionalFilterMapFromConditionalFilter(null);
                    imageCreator.isThereAConditionalFilter = false;
                    isThereAConditionalFilter = false;
                    densityChanged = true;
                    Tracer.print("Removed the conditional filter\n");
                }
            }
        }

        PlotParameterDefns ppdefs = PlotParameterDefnsMngr.getInstance()
                .getPlotParamDefns(this.plotModel.getPlugin());

        /*
         * Check for the presence of any conditional coloring parameter
         */

        synchronized (newPMEList) {
            for (PlotModelElement newPME : newPMEList) {
                if (newPME.hasAdvancedSettings()) {
                    if (newPlotParamNamesSet.contains(newPME
                            .getConditionalParameter())) {
                        setOfCondParamsAlreadyInPlotModel.add(newPME
                                .getConditionalParameter());
                    } else {
                        dataRequeryNeeded = true;
                        break;
                    }
                }
            }
        }

        if (setOfPlotParamsPrevPlotted != null
                && !setOfPlotParamsPrevPlotted.isEmpty()) {
            if (setOfPlotParamsPrevPlotted.size() != newPlotParamNamesSet
                    .size()) {
                dataRequeryNeeded = true;
            } else {
                synchronized (newPlotParamNamesSet) {
                    for (String newParam : newPlotParamNamesSet) {
                        if (!setOfPlotParamsPrevPlotted.contains(newParam)) {
                            dataRequeryNeeded = true;
                            break;
                        }
                    }
                }
            }
        }

        this.plotModel = editedPlotModel;

        if (dataRequeryNeeded) {
            Tracer.print("Need to requery data (and recreate all station images)");
            try {
                dataRequestor
                        .updateListOfParamsToPlotFromCurrentPlotModel(editedPlotModel);
                dataRequestor
                        .determineConditionalColoringParameters(editedPlotModel);

                imageCreator.setPlotModel(editedPlotModel);

                // Remove all met param data from all stations in all frames.
                // (Since we need to requery all of it anyway, this will force
                // that to occur.
                for (AbstractFrameData afd : frameDataMap.values()) {
                    // TODO sanity check type?
                    FrameData fd = (FrameData) afd;
                    fd.clearMetParameters();
                }

                /* To remove the obsolete strings from the diff maps */
                synchronized (newPMEList) {
                    for (PlotModelElement newPME : newPMEList) {
                        synchronized (oldPMEList) {
                            imageCreator
                                    .removeObsoletePlotEntriesAtThisPositionForAllFrames(imageCreator
                                            .getPositionFromPlotModelElement(newPME));
                        }
                    }
                }

                imageCreator.removeObsoletePMEEntries(editedPlotModel);
                List<DataTime> dtList = getFrameTimes();
                synchronized (dtList) {
                    for (DataTime dt : dtList) {
                        FrameData fd = (FrameData) getFrame(dt);
                        if (densityChanged) {
                            Tracer.print("About to queue stations for prog disc");
                            progressiveDisclosure.setDensity(plotDensity);
                            fd.setFrameStatus(FrameStatus.PROGDISC);
                            progressiveDisclosure
                                    .queueListOfStationsToBeDisclosed(dt,
                                            fd.stationMap.values());
                        } else {
                            Tracer.print("About to queue stations for data requery");
                            fd.setFrameStatus(FrameStatus.METPARAMS);
                            dataRequestor.queueStationsForHdf5Query(dt,
                                    fd.stationsLastRendered);
                        }
                        issueRefresh();
                    }
                }
            } catch (VizException e) {
                e.printStackTrace();
            }
        }

        else {

            if (!setOfCondParamsAlreadyInPlotModel.isEmpty()) {
                dataRequestor
                        .determineConditionalColoringParameters(editedPlotModel);
            }

            imageCreator.setPlotModel(editedPlotModel);
            imageCreator.removeObsoletePMEEntries(editedPlotModel);
            List<DataTime> dtList = getFrameTimes();
            synchronized (dtList) {
                for (DataTime dt : dtList) {
                    FrameData fd = (FrameData) getFrame(dt);
                    if (densityChanged) {
                        progressiveDisclosure.setDensity(plotDensity);
                        Tracer.print("About to queue stations for pg disc");
                        fd.setFrameStatus(FrameStatus.PROGDISC);
                        progressiveDisclosure.queueListOfStationsToBeDisclosed(
                                dt, fd.stationMap.values());
                        issueRefresh();
                    } else {
                        if (areALLCondFilterParamsInEditedPlotModel) {
                            Tracer.print("About to apply conditional filter to stns last rendered");
                            dataRequestor
                                    .updateListOfStationsPerConditionalFilter(
                                            dt, fd.stationsLastRendered);
                            if (!setOfCondParamsAlreadyInPlotModel.isEmpty()) {
                                synchronized (fd.stationsLastRendered) {
                                    for (Station currentStation : fd.stationsLastRendered) {
                                        synchronized (setOfCondParamsAlreadyInPlotModel) {
                                            for (String condParamName : setOfCondParamsAlreadyInPlotModel) {
                                                dataRequestor
                                                        .processConditionalParameterForOneStation(
                                                                currentStation.parametersToPlot,
                                                                currentStation,
                                                                ppdefs.getPlotParamDefn(
                                                                        condParamName)
                                                                        .getMetParamName());
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            if (!dataRequeryNeeded) {
                                if (!setOfCondParamsAlreadyInPlotModel
                                        .isEmpty()) {
                                    synchronized (fd.stationsLastRendered) {
                                        for (Station currentStation : fd.stationsLastRendered) {
                                            synchronized (setOfCondParamsAlreadyInPlotModel) {
                                                for (String condParamName : setOfCondParamsAlreadyInPlotModel) {
                                                    dataRequestor
                                                            .processConditionalParameterForOneStation(
                                                                    currentStation.parametersToPlot,
                                                                    currentStation,
                                                                    ppdefs.getPlotParamDefn(
                                                                            condParamName)
                                                                            .getMetParamName());
                                                }
                                            }
                                        }
                                    }
                                }
                                Tracer.print("Queueing stations only for image creation");
                                fd.setFrameStatus(FrameStatus.IMAGING);
                                imageCreator.queueStationsToCreateImages(dt,
                                        fd.stationsLastRendered);
                            } else {
                                Tracer.print("Queueing stations for data retrieval");
                                fd.setFrameStatus(FrameStatus.METPARAMS);
                                dataRequestor.queueStationsForHdf5Query(dt,
                                        fd.stationsLastRendered);
                            }
                        }
                    }
                    issueRefresh();
                }
            }
        }
        Tracer.print("< Exit");

    }

    @Override
    public void disclosureComplete(DataTime time,
            Collection<Station> disclosedStations) {
        Tracer.print("> Entry");
        FrameData fd = ((FrameData) getFrame(time)); // TODO deal with null

        if (disclosedStations != null && disclosedStations.size() > 0) {
            Tracer.print("Prog disc returned " + disclosedStations.size()
                    + " stations" + " for frame: "
                    + Tracer.shortTimeString(time));
            fd.disclosedStationsCount = disclosedStations.size();
            fd.setFrameStatus(FrameStatus.METPARAMS);
            dataRequestor.queueStationsForHdf5Query(time, disclosedStations);
        } else {
            Tracer.print("Prog disc returned " + "*NO*" + " stations"
                    + " for frame: " + Tracer.shortTimeString(time)
                    + " quitting with no HDF5 retrieval attempted");
            fd.setFrameStatus(FrameStatus.READY);
        }
        Tracer.print("< Exit");

    }

    @Override
    public void retrievalComplete(DataTime time,
            Collection<Station> retrievedStations,
            int stationsRetrievedThisCallCount,
            boolean isThereAConditionalFilter) {
        Tracer.print("> Entry");
        FrameData fd = ((FrameData) getFrame(time)); // TODO deal with null

        fd.stationsRetrievedThisCallCount = stationsRetrievedThisCallCount;

        if (retrievedStations != null && retrievedStations.size() > 0) {
            Tracer.print("Met Param retriever returned "
                    + retrievedStations.size() + " stations" + " for frame: "
                    + Tracer.shortTimeString(time));

            if (isThereAConditionalFilter) {
                // TODO correct logic? (i.e., if false do not change)
                imageCreator.isThereAConditionalFilter = true;
            }
            fd.setFrameStatus(FrameStatus.IMAGING);
            imageCreator.queueStationsToCreateImages(time, retrievedStations);
        } else {
            Tracer.print("Met Param retriever returned " + "*NO*" + " stations"
                    + " for frame: " + Tracer.shortTimeString(time)
                    + " quitting with no HDF5 retrieval attempted");
            fd.setFrameStatus(FrameStatus.READY);
        }
        Tracer.print("< Exit");

    }

    public void retrievalAborted(DataTime time) {
        Tracer.print("> Entry");
        FrameData fd = ((FrameData) getFrame(time)); // TODO deal with null
        Tracer.print("Met Param retriever ABORTED" + " for frame: "
                + Tracer.shortTimeString(time));
        fd.stationsRetrievedThisCallCount = 0;
        fd.setFrameStatus(FrameStatus.READY);
        Tracer.print("< Exit");
    }

    @Override
    public synchronized void renderingComplete(DataTime time,
            Collection<Station> collectionOfStationsToBeRendered,
            List<DrawableString> listOfStringsToDraw,
            List<IVector> listOfVectors,
            List<SymbolLocationSet> listOfSymbolLocSet) {

        Tracer.print("> Entry  " + Tracer.shortTimeString(time));
        FrameData fd = ((FrameData) getFrame(time));

        if (fd != null) {
            if (listOfStringsToDraw != null && !listOfStringsToDraw.isEmpty()) {
                fd.drawableStrings = new ArrayList<DrawableString>(
                        listOfStringsToDraw);
            } else {
                fd.drawableStrings = new ArrayList<DrawableString>(0);
            }

            if (listOfVectors != null && !listOfVectors.isEmpty()) {
                fd.drawableVectors = new ArrayList<IVector>(listOfVectors);
            } else {
                fd.drawableVectors = new ArrayList<IVector>(0);
            }

            if (listOfSymbolLocSet != null && !listOfSymbolLocSet.isEmpty()) {
                fd.drawableSymbolSets = new ArrayList<SymbolLocationSet>(
                        listOfSymbolLocSet);
            } else {
                fd.drawableSymbolSets = new ArrayList<SymbolLocationSet>(0);
            }

            fd.stationsLastRendered = new HashSet<Station>();
            fd.stationsLastRendered.addAll(collectionOfStationsToBeRendered);

            for (Station stn : collectionOfStationsToBeRendered) {
                String stnKey = getStationMapKey(stn.info.latitude,
                        stn.info.longitude);
                synchronized (fd.stationMap) {
                    fd.stationMap.put(stnKey, stn);
                }
            }

            fd.setFrameStatus(FrameStatus.READY);
        }

        Tracer.print("renderingComplete() called for the frame "
                + Tracer.shortTimeString(time) + " with "
                + collectionOfStationsToBeRendered.size() + " stations");
        issueRefresh();
        Tracer.print("< Exit  " + Tracer.shortTimeString(time));

    }

    public void renderingAborted(DataTime time) {
        Tracer.print("> Entry");
        FrameData fd = ((FrameData) getFrame(time)); // TODO deal with null
        Tracer.print("Image Rendering ABORTED" + " for frame: "
                + Tracer.shortTimeString(time));
        fd.setFrameStatus(FrameStatus.READY);
        Tracer.print("< Exit");

    }

    // generate a string used as the key for the StationMap

    private String getStationMapKey(Float lat, Float lon) {
        return new String("" + Math.round(lat * 1000.0) + ","
                + Math.round(lon * 1000.0));
    }

    private void resetFramePaintFlagForAllFrames() {
        Tracer.print("> Entry");
        List<DataTime> dtList = getFrameTimes();
        synchronized (dtList) {
            for (DataTime datatime : dtList) {
                ((FrameData) getFrame(datatime)).isFramePaintedFirstTime = false;
            }
        }
        Tracer.print("< Exit");
    }

    private void setInitialPlotDensityFromRscAttrSet() {
        Tracer.print("> Entry");
        double newPlotDensity = ((Integer) plotRscData.getRscAttrSet()
                .getRscAttr("plotDensity").getAttrValue()).doubleValue() / 10;
        if (plotDensity == Double.MIN_NORMAL
                || Math.abs(plotDensity - newPlotDensity) > 1E-08) { // TODO
                                                                     // const
            plotDensity = newPlotDensity;
        }
        Tracer.print("< Exit");

    }

    private void loadFrameData() {
        Tracer.print("> Entry");
        List<DataTime> listOfFrameTimes = new ArrayList<DataTime>(
                getFrameTimes());
        int frameTimesListSize = (listOfFrameTimes != null) ? listOfFrameTimes
                .size() : 0;

        long t0 = System.nanoTime();

        if (!isFcst) {
            for (int index = frameTimesListSize - 1; index >= 0; --index) {
                DataTime dataTime = listOfFrameTimes.get(index);
                FrameData frameData = (FrameData) getFrame(dataTime);
                frameData.setFrameStatus(FrameStatus.LOADING);
                frameLoaderTask = new FrameLoaderTask(dataTime);
                frameRetrievalPool.schedule(frameLoaderTask);
            }
        } else {
            populateStationsForTheFcstPointRscFrames();
        }

        long t1 = System.nanoTime();
        timeLogger.append("Finished loading " + frameTimesListSize + " in "
                + ((t1 - t0) / 1000) + " microseconds" + "\n");
        Tracer.print("< Exit");
    }

    @Override
    public void messageGenerated(PlotInfo[] key, String message) {
        Tracer.print("> Entry [empty!]");
        Tracer.print("< Exit");
    }

    private void populateStationsForTheFcstPointRscFrames() {
        Tracer.print("> Entry");
        List<DataTime> datatimeList = getFrameTimes();
        if (!datatimeList.isEmpty()) {
            synchronized (datatimeList) {
                FrameData firstFrame = (FrameData) getFrame(datatimeList.get(0));
                firstFrame.setFrameStatus(FrameStatus.LOADING);
                String tableName = this.metadataMap.get("pluginName")
                        .getConstraintValue();
                String matrixType = null;
                RequestConstraint matrixTypeRC = this.metadataMap
                        .get("matrixType");
                if (matrixTypeRC != null) {
                    matrixType = matrixTypeRC.getConstraintValue();
                }
                String query = "";
                if (matrixType == null || matrixType.isEmpty()) {
                    query = "select distinct(" + tableName + ".forecastTime), "
                            + tableName + ".rangeEnd" + " from " + tableName
                            + " where reftime = '" + cycleTimeStr + "';";
                } else {
                    query = "select distinct(" + tableName + ".forecastTime), "
                            + tableName + ".rangeEnd" + " from " + tableName
                            + " where matrixtype = '" + matrixType + "'"
                            + " AND reftime = '" + cycleTimeStr + "';";
                }
                try {
                    List<Object[]> results = null;
                    results = DirectDbQuery.executeQuery(query, "metadata",
                            QueryLanguage.SQL);
                    List<Integer> intList = new ArrayList<Integer>();
                    List<Timestamp> timeStampList = new ArrayList<Timestamp>();
                    if (results != null) {
                        for (Object[] objArr : results) {
                            for (Object o : objArr) {
                                if (o instanceof Integer) {
                                    Integer i = (Integer) o;
                                    intList.add((Integer) i);
                                }
                                if (o instanceof Timestamp) {
                                    timeStampList.add((Timestamp) o);
                                }
                            }
                        }
                        Integer[] iArr = new Integer[0];
                        Timestamp[] tArr = new Timestamp[0];
                        if (!intList.isEmpty()) {
                            iArr = intList.toArray(new Integer[0]);
                            Arrays.sort(iArr);

                            if (!timeStampList.isEmpty()) {
                                tArr = timeStampList.toArray(new Timestamp[0]);
                                Arrays.sort(tArr);

                                Date cycleTimeDate = rscName.getCycleTime()
                                        .getRefTime();

                                int index = 1;
                                boolean retrievedStationsForFirstFrame = false;
                                for (index = 0; index < datatimeList.size(); index++) {
                                    DataTime nextFrameTime = datatimeList
                                            .get(index);
                                    int indexOfTimeStamp = 0;
                                    int sizeOfTimestampList = tArr.length;
                                    for (indexOfTimeStamp = 0; indexOfTimeStamp < sizeOfTimestampList; indexOfTimeStamp++) {
                                        Timestamp timeStamp = tArr[indexOfTimeStamp];
                                        if (timeStamp.getTime() == nextFrameTime
                                                .getRefTime().getTime()) {
                                            int fcstHr = iArr[indexOfTimeStamp]
                                                    .intValue();
                                            DataTime newTime = new DataTime(
                                                    cycleTimeDate);
                                            newTime.setFcstTime(fcstHr);
                                            newTime.setUtilityFlags(firstFrame
                                                    .getFrameTime()
                                                    .getUtilityFlags());
                                            Tracer.print("nextFrameTime = "
                                                    + Tracer.shortTimeString(nextFrameTime)
                                                    + "\n" + "newTime = "
                                                    + newTime.toString());

                                            /*
                                             * For a forecast point resource,
                                             * the number of stations remains
                                             * the same for all forecast hours.
                                             * So a Postgres query is carried
                                             * out only once (for the first
                                             * frame) and the same set of
                                             * stations is subsequently copied
                                             * to the remaining frames, after
                                             * updating the stations' data-times
                                             * to match that of the frame it
                                             * will now belong to....
                                             */

                                            if (!retrievedStationsForFirstFrame) {

                                                /*
                                                 * If this is the first frame
                                                 * retrieve the stations from
                                                 * Postgres
                                                 */
                                                FrameData frameData = (FrameData) getFrame(nextFrameTime);
                                                HashMap<String, RequestConstraint> frameRCMap = new HashMap<String, RequestConstraint>();
                                                frameRCMap
                                                        .put("pluginName",
                                                                metadataMap
                                                                        .get("pluginName"));
                                                matrixTypeRC = this.metadataMap
                                                        .get("matrixType"); // redundant?
                                                if (matrixTypeRC != null) {
                                                    frameRCMap.put(
                                                            "matrixType",
                                                            matrixTypeRC);
                                                }

                                                frameRCMap
                                                        .put("dataTime.fcstTime",
                                                                new RequestConstraint(
                                                                        String.valueOf(iArr[indexOfTimeStamp]
                                                                                .intValue())));
                                                frameRCMap.put(
                                                        "dataTime.refTime",
                                                        new RequestConstraint(
                                                                cycleTimeStr));
                                                frameData.plotInfoObjs = plotRscData
                                                        .getPlotInfoRetriever()
                                                        .getStations(frameRCMap);

                                                if (frameData.plotInfoObjs != null
                                                        && !frameData.plotInfoObjs
                                                                .isEmpty()) {
                                                    synchronized (frameData.plotInfoObjs) {

                                                        boolean isFramePopulated = frameData
                                                                .populateFrame();
                                                        if (isFramePopulated) {
                                                            retrievedStationsForFirstFrame = true;
                                                            Collection<Station> stationsToBeQueuedForProgDisc = progressiveDisclosure
                                                                    .calculateStaticProgDiscDistancesForStations(
                                                                            frameData.stationMap
                                                                                    .values(),
                                                                            frameData.dynStations);
                                                            if (stationsToBeQueuedForProgDisc != null
                                                                    && !stationsToBeQueuedForProgDisc
                                                                            .isEmpty()) {
                                                                synchronized (stationsToBeQueuedForProgDisc) {
                                                                    for (Station stn : stationsToBeQueuedForProgDisc) {
                                                                        String stnMapKey = getStationMapKey(
                                                                                stn.info.latitude,
                                                                                stn.info.longitude);
                                                                        Station frameStn = frameData.stationMap
                                                                                .get(stnMapKey);
                                                                        frameStn.distanceValue = stn.distanceValue;
                                                                        synchronized (frameData.stationMap) {
                                                                            frameData.stationMap
                                                                                    .put(stnMapKey,
                                                                                            frameStn);
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            queueStationsForRemainingFcstFrames(
                                                    nextFrameTime, newTime,
                                                    firstFrame.stationMap
                                                            .values());
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        Tracer.print("< Exit");
    }

    private void queueStationsForRemainingFcstFrames(DataTime frameTime,
            DataTime stnTimeToSet, Collection<Station> stationCollection) {
        Tracer.print("> Entry  " + Tracer.shortTimeString(frameTime));
        SpecialQueueEntry queueEntry = new SpecialQueueEntry(frameTime,
                stnTimeToSet, stationCollection);
        queueOfFrameTimesAndStations.add(queueEntry);
        FrameData frameData = (FrameData) getFrame(frameTime);
        frameData.setFrameStatus(FrameStatus.LOADING);
        scheduleFcstFrameLoaderTask();
        Tracer.print("< Exit   " + Tracer.shortTimeString(frameTime));
    }

    private void scheduleFcstFrameLoaderTask() {
        Tracer.print("> Entry");
        if (queueOfFrameTimesAndStations == null
                || queueOfFrameTimesAndStations.isEmpty())
            return;
        synchronized (queueOfFrameTimesAndStations) {
            while (queueOfFrameTimesAndStations.peek() != null) {
                SpecialQueueEntry currEntry = queueOfFrameTimesAndStations
                        .poll();
                if (currEntry == null)
                    continue;
                fcstFrameLoaderTask = new FcstFrameLoaderTask(
                        currEntry.getDataTime(), currEntry.stationTimeToSet,
                        currEntry.getStations());
                frameRetrievalPool.schedule(fcstFrameLoaderTask);
            }
        }
        Tracer.print("< Exit");
    }

    private void paintFrameStatusDisplay(AbstractFrameData currentFrameData,
            IGraphicsTarget target, PaintProperties paintProps) {

        if (frameStatusDisplayFont == null) {
            // allocate font only on first use
            frameStatusDisplayFont = target.initializeFont("Helvetica",
                    (float) (12 * 1.000f), new Style[] {});
            frameStatusDisplayFont.setSmoothing(false);
            frameStatusDisplayFont.setScaleFont(false);
        }
        IExtent screenExtent = paintProps.getView().getExtent();
        IExtent mapExtent = new PixelExtent(descriptor.getGridGeometry()
                .getGridRange());
        double x0 = Math.max(mapExtent.getMinX(), screenExtent.getMinX()
                + (screenExtent.getWidth() * .020));
        double y0 = Math.max(mapExtent.getMinY(), screenExtent.getMinY()
                + (screenExtent.getHeight() * .035));
        double[] startPixel = new double[] { x0, y0 };
        double x1 = Math.min(mapExtent.getMaxX(), x0
                + (screenExtent.getWidth() * .45));
        double y1 = y0;
        double[] endPixel = new double[] { x1, y1 };

        if (!mapExtent.contains(startPixel) || !mapExtent.contains(endPixel)) {
            return;
        }

        float zoom = paintProps.getZoomLevel();
        double yOffset = 250 * zoom;

        List<DataTime> dtList = getFrameTimes();
        synchronized (dtList) {
            for (DataTime dt : dtList) {
                FrameData fd = (FrameData) getFrame(dt);
                FrameStatus fs = fd.getFrameStatus();
                String frameTimeString = NmapCommon.getTimeStringFromDataTime(
                        fd.getFrameTime(), "/");
                if (!frameStatusDisplayVerbose) { // use terse date
                    frameTimeString = frameTimeString.substring(4);
                }
                String string = frameTimeString
                        + "    "
                        + fs.getSpacing()
                        + (frameStatusDisplayVerbose ? fs.getDescription() : fs
                                .toString());
                y0 += yOffset;
                DrawableString ds = new DrawableString(string,
                        fs.getColorCode());
                ds.setCoordinates(x0, y0);
                ds.font = frameStatusDisplayFont;
                if (fd == currentFrameData) {
                    ds.addTextStyle(TextStyle.BOXED);
                }
                ds.horizontalAlignment = HorizontalAlignment.LEFT;
                ds.verticallAlignment = VerticalAlignment.BOTTOM;
                try {
                    target.drawStrings(ds);
                } catch (VizException e1) {
                    statusHandler.handle(Priority.PROBLEM,
                            "Error when drawing diagnostic frame status display: "
                                    + e1);
                }
            }
        }
    }
}
