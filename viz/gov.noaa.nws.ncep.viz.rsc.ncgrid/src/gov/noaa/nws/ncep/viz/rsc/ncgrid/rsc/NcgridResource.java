package gov.noaa.nws.ncep.viz.rsc.ncgrid.rsc;

import gov.noaa.nws.ncep.common.dataplugin.ncgrib.request.StoreGridRequest;
import gov.noaa.nws.ncep.gempak.parameters.colors.COLORS;
import gov.noaa.nws.ncep.gempak.parameters.core.marshaller.garea.GraphicsAreaCoordinates;
import gov.noaa.nws.ncep.gempak.parameters.hilo.HILOBuilder;
import gov.noaa.nws.ncep.gempak.parameters.hilo.HILOStringParser;
import gov.noaa.nws.ncep.gempak.parameters.hlsym.HLSYM;
import gov.noaa.nws.ncep.gempak.parameters.intext.TextStringParser;
import gov.noaa.nws.ncep.gempak.parameters.title.TITLE;
import gov.noaa.nws.ncep.viz.common.Activator;
import gov.noaa.nws.ncep.viz.common.preferences.GraphicsAreaPreferences;
import gov.noaa.nws.ncep.viz.common.ui.HILORelativeMinAndMaxLocator;
import gov.noaa.nws.ncep.viz.common.ui.ModelListInfo;
import gov.noaa.nws.ncep.viz.common.ui.color.GempakColor;
import gov.noaa.nws.ncep.viz.common.util.CommonDateFormatUtil;
import gov.noaa.nws.ncep.viz.gempak.util.GempakGrid;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData.TimeMatchMethod;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceName;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.FloatGridData;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.NcgribLogger;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.NcgribLoggerPreferences;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.actions.SaveGridInput;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.contours.ContourAttributes;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.contours.ContourRenderable;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.contours.GridIndicesDisplay;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.contours.GridPointMarkerDisplay;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.contours.GridPointValueDisplay;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.contours.GridRelativeHiLoDisplay;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.contours.GriddedVectorDisplay;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.dgdriv.Dgdriv;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.dgdriv.DgdrivException;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.dgdriv.GridDBConstants;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.dgdriv.NcgridDataCache;
import gov.noaa.nws.ncep.viz.ui.display.NCMapDescriptor;

import java.io.FileNotFoundException;
import java.nio.FloatBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.measure.converter.UnitConverter;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.dataplugin.grid.GridInfoRecord;
import com.raytheon.uf.common.dataplugin.grid.GridRecord;
import com.raytheon.uf.common.dataplugin.level.Level;
import com.raytheon.uf.common.dataquery.requests.DbQueryRequest;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint.ConstraintType;
import com.raytheon.uf.common.dataquery.responses.DbQueryResponse;
import com.raytheon.uf.common.datastorage.StorageException;
import com.raytheon.uf.common.geospatial.ISpatialObject;
import com.raytheon.uf.common.geospatial.MapUtil;
import com.raytheon.uf.common.geospatial.ReferencedCoordinate;
import com.raytheon.uf.common.gridcoverage.GridCoverage;
import com.raytheon.uf.common.parameter.Parameter;
import com.raytheon.uf.common.parameter.lookup.ParameterLookupException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.style.level.SingleLevel;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.requests.ThriftClient;
import com.raytheon.uf.viz.core.rsc.DisplayType;
import com.raytheon.uf.viz.core.rsc.LoadProperties;

/**
 * Grid contour Resource
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb, 2010                M. Li           Initial creation
 * Jun, 2010                M. Li           Retrieve grid data from Grid Diagnostic instead of HDF5
 * Oct, 2010     #307       G. Hull         use NcGridDataProxy to support correct time matching
 * Oct, 2010     #320       X. Guo          Replace special characters in TITLE parameter
 * Oct, 2010     #307       m.gamazaychikov Add handling of DgdrivException in getDataRecord method
 * Oct, 2010                X. Guo          Rename getCycleTimeStringFromDataTime to getTimeStringFromDataTime
 * Oct, 2010     #277       M. Li           Parsed first model name from ensemble model list
 * Nov, 2010                M. Li           modified for new vector algorithm
 * 11/29/2010               mgamazaychikov  Updated queryRecords and updateFrameData
 * 12/06/2010    #363       X. Guo          Plot relative minima and maxima gridded data
 * 12/19/2010     365       Greg Hull       Replace dataSource with pluginName
 * 01/03/11                 M. Li           Add hilo and hlsysm to contourAttributes
 * 01/07/11                 M. Li           Use Vector array
 * 03/08/11                 M. Li           refactor ContourRenderable
 * 04/29/11                 M. Li           move gridIndiceDisplay to a separate class
 * 06/2011                  mgamazaychikov  Add spatialObject to the Dgdriv fields.
 * 07/2011                  mgamazaychikov  Add substituteAlias method.
 * 08/2011                  mgamazaychikov  Add dispose method to FrameData class;
 *                                          change disposeInternal method of NcgridResource class;
 *                                          change aDgdriv from the NcgridResource class variable to local variable.
 * 09/2011                  mgamazaychikov  Made changes associated with removal of DatatypeTable class
 * 10/2011                  X. Guo          Updated
 * 11/2011                  X. Guo          Updated contour attributes
 * 11/16/2011               X. Guo          Corrected Valid/Forecast time in Title
 * 11/22/2011               X. Guo          Used the current frame time to set dgdriv and add dumpNcGribInventory()
 * 12/12/2011               X. Guo          Updated Ensemble requests 
 * 12/06/2012   #538        Q. Zhou         Added skip and filter areas and implements.
 * 02/15/2012               X. Guo          Added schedule job to parallel updating data 
 * 02/16/2012   #555        S. Gurung       Added call to setAllFramesAsPopulated() in queryRecords()
 * 03/01/2012               X. Guo          Added codes to handle attributes modification
 * 03/13/2012               X. Guo          Created multi-threads to generate contours
 * 03/15/2012               X. Guo          Set synchronized block in ContoutSupport
 * 04/03/2012               X. Guo          Created vector wireframe in contour job
 *                                          and changed constraint to query available times
 * 05/15/2012               X. Guo          Used getAvailableDataTimes() to get available times
 * 05/23/2012               X. Guo          Loaded ncgrib logger   
 * 06/07/2012               X. Guo          Catch datauri&grid data for each frame
 * 09/26/2012               X. Guo          Fixed navigation query problems     
 * 08/19/2013   #743        S. Gurung       Added clrbar related changes  
 * 09/14/2013   #1036       S. Gurung       Added TEXT attribute related changes
 * 11/19/2013   #619        T. Lee          Fixed DOW in Title string
 * 11/19/2013   #930        T. Lee          Replaced modelName with "Resource Type"
 * 1/28/2014    #934        T. Lee          Enabled "colors" bangs for grid points plot
 * 02/4/2014    #936        T. Lee          Implemented textSize for point values
 * 04/11/2014   #981        D.Sushon        Added fault tolerance for when some data is bad to display the good data rather than fall-through entire frame
 * 04/14/2014               S.Gilbert       Cleaned up old unused methods
 * 04/22/2014   #1129       B. Hebbard      Feed HILO point count limits to GridRelativeHiLoDisplay constructor instead of HILORelativeMinAndMaxLocator, so can apply dynamically based on current extent
 * 06/27/2014   ?           B. Yin          Handle grid analysis (cycle time is null).
 * 08/01/2014   ?           B. Yin          Handle display type D (directional arrow).
 * 12/11/2014   R5113       J. Wu           Correct parsing for gdfunc.
 * 02/09/2015   RM4980      S. Russell      Updated NcgridLoaderJob.run() and 
 *                                          overrode processNewRscDataList for 
 *                                          the super class
 * 07/28/2015   R8993       S. Russell      Updated NcGridLoaderJob.run() to
 *                                          ensure that preloading of grid data
 *                                          for overlays happens.
 * 07/17/2015   R6916       B. Yin/R. Kean  Changes for Contour fill images
 * 12/28/2015   R8832       S. Russell      Altered NcgridLoaderJob.run() and
 *                                          processNewRscDataList() to switch
 *                                          between processing code for 
 *                                          binning and other time matching
 *                                          methods as appropriate
 * 02/08/2016   R8832       S.Russell       Updated processNewRscDataListWithBinning()
 *                                          to correct an error introduced
 *                                          into a conditional in the method.
 * 02/24/2016   R6821       K.Bugenhagen    Added capability to save grid and 
 *                                          consolidated multiple log messages 
 *                                          into one method. Replaced catching
 *                                          of nullpointerexceptions with
 *                                          null checks.
 * 04/14/2016   R17316      K.Bugenhagen    Added call to paintFrame to reproject
 *                                          for point data values (type = 'p') if
 *                                          projection is changed.
 * 04/21/2016   R17741      S. Gilbert      removed isseuRefresh() that was causing continuous paint.
 * 05/06/2016   R17323      K.Bugenhagen    Save ncgrid proxy with each frame.
 *                                          Remove unnecessary issueRefresh call in
 *                                          paintFrame. Removed getFileName method 
 *                                          (not used anywhere). Use slf4j logger.
 * 08/16/2016   R17603      K.Bugenhagen    Added isDuplicateDatasetId method.
 *                                          Also, cleanup.
 * 
 * 08/18/2016    R17569     K Bugenhagen    Modified calls to NcEnsembleResourceData methods 
 *                                          since they are no longer static. Also, cleanup.
 * </pre>
 * 
 * @author mli
 * @version 1.0
 */
public class NcgridResource extends
        AbstractNatlCntrsResource<NcgridResourceData, NCMapDescriptor>
        implements INatlCntrsResource {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(NcgridResource.class);

    private final Logger logger = LoggerFactory.getLogger("PerformanceLogger");

    private static SimpleDateFormat QUERY_DATE_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss");

    private final static String DATA_SET_ID_TAG = "datasetId";

    // For Ensembles this will be the NcEnsembleResourceData
    protected NcgridResourceData gridRscData;

    protected SingleLevel[] levels;

    protected float displayedLevel;

    protected boolean ready = false;

    protected IGraphicsTarget lastTarget = null;

    protected PaintProperties lastPaintProps = null;

    protected SingleLevel level;

    protected String levelUnits;

    protected UnitConverter conversion;

    private long initTime = 0;

    private static NcgribLogger ncgribLogger;

    private ArrayList<DataTime> dataTimesForDgdriv = new ArrayList<>();

    private ContourAttributes[] contourAttributes;

    private TextStringParser text;

    // grid preferences
    // lower left latitude/lower left longitude/upper right latitude/upper right
    // longitude
    private String ncgribPreferences = null;

    // Sub-grid coverage
    private ISpatialObject subgObj = null;

    // expression
    private String expr = "((-|\\+)?[0-9]+(\\.[0-9]+)?)+";

    // These objects are used as proxys to time match the frames.
    // These are created by querying the db for available times and then
    // they are assigned to the FrameData's which will then use the time
    // in a separate query to the DgDriv to get the grid data.
    // The dataTime is set first and then the spatial object since this
    // is determined later in updateFrameData after the time matching is done.
    // This is because, for the GHM model, the spatialObjects will be different
    // for each time.
    public class NcGridDataProxy implements IRscDataObject {

        private DataTime dataTime;

        private ISpatialObject spatialObj;

        private ISpatialObject newSpatialObj;

        public NcGridDataProxy(DataTime dt) {
            dataTime = new DataTime(dt.getRefTime(), dt.getFcstTime());
            spatialObj = null;
        }

        @Override
        public DataTime getDataTime() {
            return dataTime;
        }

        public void setSpatialObject(ISpatialObject spatObj) {
            spatialObj = spatObj;
        }

        public ISpatialObject getSpatialObject() {
            return spatialObj;
        }

        public void setNewSpatialObject(ISpatialObject spatObj) {
            newSpatialObj = spatObj;
        }

        public ISpatialObject getNewSpatialObject() {
            return newSpatialObj;
        }
    }

    protected class NcgridLoaderJob extends Job {

        private boolean cancel = false;

        public NcgridLoaderJob(String name) {
            super(name);
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {

            TimeMatchMethod timeMatchMethod = resourceData.getTimeMatchMethod();

            boolean isFirst = true;

            long t1 = System.currentTimeMillis();
            if (ncgribLogger.enableRscLogs())
                logger.debug("==from init to run loadNcgridData took: "
                        + (t1 - initTime));

            // For each frame
            for (AbstractFrameData fd : frameDataMap.values()) {
                FrameData frameData = (FrameData) fd;

                // Search each DataTime obj for one whose date matches the frame
                if (timeMatchMethod == TimeMatchMethod.BINNING_FOR_GRID_RESOURCES) {
                    this.processDateTimeWithBinning(frameData);
                } else {
                    this.processDateTime(frameData);
                }

                if (cancel) {
                    return Status.CANCEL_STATUS;
                }

                processNewRscDataList();

                if (isFirst) {
                    isFirst = false;
                    int cnt = 0;
                    while (!frameData.isPaintAble()) {
                        try {
                            Thread.sleep(5);
                            if (cnt > 20) {
                                break;
                            }
                            cnt++;
                        } catch (InterruptedException e) {
                            statusHandler
                                    .error("NcgridResource.NcgridLoaderJob.run()"
                                            + " InterruptedException on thread",
                                            e);
                        }
                    }

                }

                issueRefresh();
            }

            return Status.OK_STATUS;
        }

        public void loadNcgridData() {
            if (this.getState() != Job.RUNNING) {
                this.schedule();
            }
        }

        public void setCancelFlag(boolean cl) {
            cancel = cl;
        }

        private void processDateTime(FrameData frameData) {

            for (DataTime dt : dataTimesForDgdriv) {

                IRscDataObject[] dataObject = processRecord(dt);
                if (frameData.isRscDataObjInFrame(dataObject[0])) {
                    newRscDataObjsQueue.add(dataObject[0]);
                    break;
                }
            }
        }

        private void processDateTimeWithBinning(FrameData frameData) {

            int closestMatch = 0;
            IRscDataObject lastDataObj = null;
            boolean frameMatched = false;

            // Scroll each available date for a date that matches the frame
            for (DataTime dt : dataTimesForDgdriv) {

                // Get the data associated with that date
                IRscDataObject[] dataObject = processRecord(dt);

                // If the date is in the range of the current frame
                if (frameData.isRscDataObjInFrame(dataObject[0])) {

                    closestMatch = frameData.closestToFrame(dataObject[0],
                            lastDataObj);

                    // Add the data for that date to a list of data objects
                    if (closestMatch == 1) {
                        newRscDataObjsQueue.add(dataObject[0]);
                    } else if (closestMatch == 2) {
                        newRscDataObjsQueue.add(lastDataObj);
                    } else if (closestMatch == 0) {
                        newRscDataObjsQueue.add(lastDataObj);
                    }

                    frameMatched = true;
                    break;
                }
                lastDataObj = dataObject[0];

            }

            if (!frameMatched && lastDataObj != null) {
                newRscDataObjsQueue.add(lastDataObj);
            }

            // Add the date based grid data to the frame it was matched to
            if (!newRscDataObjsQueue.isEmpty()) {
                addRscDataToFrame(frameData, newRscDataObjsQueue.poll());
                frameData.setPopulated(true);
            }

        }// end method

    }// end NcGridLoaderJob

    protected class NcgridAttrModifiedJob extends Job {

        private boolean cancel = false;

        public NcgridAttrModifiedJob(String name) {
            super(name);
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            separateAttributes();

            for (AbstractFrameData fd : frameDataMap.values()) {

                if (cancel) {
                    return Status.CANCEL_STATUS;
                }

                FrameData frameData = (FrameData) fd;
                frameData.setAttrsModifiedFlag(true);
                frameData.setPaintFlag(false);
                frameData.procAttrsModified();
            }
            return Status.OK_STATUS;
        }

        public void procAttrs() {
            if (this.getState() != Job.RUNNING) {
                this.schedule();
            }
        }

        public void setCancelFlag(boolean cl) {
            cancel = cl;
        }
    }

    private NcgridLoaderJob ncgridLoader;

    private NcgridAttrModifiedJob ncgribAttrsModified;

    public class FrameData extends AbstractFrameData {
        ContourRenderable[] contourRenderable;

        NcGridDataProxy gdPrxy = null;

        GriddedVectorDisplay[] vectorDisplay;

        GridPointMarkerDisplay gridPointMarkerDisplay;

        GridIndicesDisplay gridIndicesDisplay;

        NcgridDataCache cacheData = new NcgridDataCache();

        GridPointValueDisplay[] gridPointValueDisplay;

        boolean hasData = false;

        String gfunc = "";

        String glevel = "";

        String gvcord = "";

        String skip = "";

        String filter = "";

        String scale = "";

        String colors = "";

        String text = "";

        Boolean frameLoaded = false;

        Boolean paintAble = false;

        boolean isReProject = false;

        boolean isAttrModified = false;

        boolean isFirst = true;

        protected class GenerateContourJob extends Job {

            public GenerateContourJob(String name) {
                super(name);
            }

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                createContours();
                return Status.OK_STATUS;
            }

            public void genCntrs() {
                if (this.getState() != Job.RUNNING) {
                    this.schedule();
                }
            }
        }

        private GenerateContourJob genContrs = null;

        public void generateContours(String name) {
            if (genContrs != null && genContrs.getState() == Job.RUNNING) {
                return;
            }
            genContrs = new GenerateContourJob(name);
            genContrs.genCntrs();
        }

        private GenerateContourJob getGenCntrJob() {
            return this.genContrs;
        }

        protected FrameData(DataTime time, int interval) {
            super(time, interval);
        }

        public NcGridDataProxy getProxy() {
            return this.gdPrxy;
        }

        private void setProxy(NcGridDataProxy proxy) {
            gdPrxy = proxy;
        }

        @Override
        public boolean updateFrameData(IRscDataObject rscDataObj) {
            if (!(rscDataObj instanceof NcGridDataProxy)) {
                statusHandler
                        .error("Unexpected rscDataObject type in NcGridResource:updateFrameData:"
                                + rscDataObj.getClass().getName());
                return false;
            }

            /*
             * set navigation
             */
            gdPrxy = (NcGridDataProxy) rscDataObj;

            long st = System.currentTimeMillis();
            if (ncgribLogger.enableRscLogs()) {
                logger.debug("From init resource to updated frame("
                        + gdPrxy.getDataTime().toString() + ") data took:"
                        + (st - initTime));
            }

            if (gridRscData.getPluginName().equalsIgnoreCase(
                    GempakGrid.gempakPluginName)) {
                try {
                    String dataLocation = null;
                    try { // if this throws, gdPrxy won't be fully initiated
                        dataLocation = GempakGrid.getGempakGridPath(gridRscData
                                .getGdfile());

                    } catch (VizException e) {
                        throw new VizException(
                                "Unable to specify location for "
                                        + gridRscData.getPluginName() + " "
                                        + gridRscData.getGdfile(), e);
                    }
                    ISpatialObject cov = (ISpatialObject) GempakGrid
                            .getGridNavigation(gridRscData.getGdfile(),
                                    dataLocation, rscDataObj.getDataTime()
                                            .toString());
                    gdPrxy.setSpatialObject(cov);
                    gdPrxy.setNewSpatialObject(cov);
                } catch (Exception e) {
                    statusHandler.error(
                            "Error retrieving GEMPAK grid navigation block: "
                                    + e.getMessage(), e);
                }
            } else {

                HashMap<String, RequestConstraint> queryList = new HashMap<>(
                        gridRscData.getMetadataMap());

                if (gridRscData.isEnsemble()) {
                    String gdfileWithTimeCycles = ((NcEnsembleResourceData) gridRscData)
                            .convertGdfileToCycleTimeString(gridRscData
                                    .getGdfile(), gridRscData.getResourceName()
                                    .getCycleTime());
                    gridRscData
                            .setGdfile(((NcEnsembleResourceData) gridRscData)
                                    .convertGdfileToWildcardString(
                                            gdfileWithTimeCycles, gridRscData
                                                    .getResourceName()
                                                    .getCycleTime()));
                    ModelListInfo modelListInfo = new ModelListInfo(
                            gdfileWithTimeCycles);
                    String modelName = modelListInfo.getModelList().get(0)
                            .getModelName();
                    String perturbationNum = null;

                    if (modelName.contains(":")) {
                        String[] gdfileArrs = modelName.split(":");
                        modelName = gdfileArrs[0];
                        if (gdfileArrs[0].contains("%")) {
                            modelName = gdfileArrs[0].split("%")[1];
                        }
                        perturbationNum = gdfileArrs[1];
                    } else {
                        if (modelName.contains("%")) {
                            modelName = modelName.split("%")[1];
                        }
                    }
                    queryList.put(GridDBConstants.MODEL_NAME_QUERY,
                            new RequestConstraint(modelName,
                                    ConstraintType.EQUALS));

                    if (perturbationNum != null) {
                        queryList.put(GridDBConstants.ENSEMBLE_ID_QUERY,
                                new RequestConstraint(perturbationNum,
                                        ConstraintType.EQUALS));
                    }
                } else {
                    queryList.remove(GridDBConstants.EVENT_NAME_QUERY);
                    queryList.remove(GridDBConstants.ENSEMBLE_ID_QUERY);
                    if (gridRscData.getEnsembelMember() != null) {
                        queryList.put(
                                GridDBConstants.ENSEMBLE_ID_QUERY,
                                new RequestConstraint(gridRscData
                                        .getEnsembelMember(),
                                        ConstraintType.EQUALS));
                    } else {
                        if (gridRscData.getEventName() != null) {
                            queryList.put(
                                    GridDBConstants.EVENT_NAME_QUERY,
                                    new RequestConstraint(gridRscData
                                            .getEventName(),
                                            ConstraintType.LIKE));
                        }
                    }
                }
                long t1 = System.currentTimeMillis();
                String[] dts = gdPrxy.getDataTime().toString().split(" ");
                String reftime = dts[0] + " "
                        + dts[1].substring(0, dts[1].length() - 2);
                queryList.put(GridDBConstants.REF_TIME_QUERY,
                        new RequestConstraint(reftime));
                queryList.put(
                        GridDBConstants.FORECAST_TIME_QUERY,
                        new RequestConstraint(Integer.toString(gdPrxy
                                .getDataTime().getFcstTime())));

                DbQueryRequest request = new DbQueryRequest();
                request.addRequestField(GridDBConstants.NAVIGATION_QUERY);
                request.setLimit(1);
                request.setConstraints(queryList);
                try {
                    DbQueryResponse response = (DbQueryResponse) ThriftClient
                            .sendRequest(request);
                    // extract list of results
                    List<Map<String, Object>> responseList = null;
                    if (response != null) {
                        responseList = response.getResults();
                    } else {
                        // empty list to simplify code
                        responseList = new ArrayList<>(0);
                    }
                    ISpatialObject cov = null;
                    if (responseList.size() > 0) {
                        Object spatialObj = responseList.get(0).get(
                                GridDBConstants.NAVIGATION_QUERY);
                        if (spatialObj != null
                                && spatialObj instanceof ISpatialObject) {
                            cov = (ISpatialObject) spatialObj;
                        }
                    }
                    // update the spatial object in the gdProxy
                    long t4 = System.currentTimeMillis();

                    if (cov != null) {
                        if (ncgribLogger.enableRscLogs()) {
                            logger.debug("retrieving grid navigation("
                                    + cov.toString() + ") : "
                                    + gdPrxy.getDataTime().toString()
                                    + " took:" + (t4 - t1));
                        }
                        gdPrxy.setSpatialObject(cov);
                        gdPrxy.setNewSpatialObject(cov);
                    } else {
                        if (ncgribLogger.enableRscLogs()) {
                            logger.debug("Error retrieving ncgrid navigation for "
                                    + gdPrxy.getDataTime().toString()
                                    + " took:" + (t4 - t1));
                        }
                    }
                } catch (VizException e) {
                    statusHandler.error(
                            "Error retrieving ncgrid record for the spatial object: "
                                    + e.getMessage(), e);
                }
            }

            /*
             * query data
             */
            if (contourRenderable == null) {
                contourRenderable = new ContourRenderable[contourAttributes.length];
            }

            if (gridPointValueDisplay == null) {
                gridPointValueDisplay = new GridPointValueDisplay[contourAttributes.length];
            }
            if (vectorDisplay == null) {
                vectorDisplay = new GriddedVectorDisplay[contourAttributes.length];
            }

            FloatGridData gridData = null;
            long t11, t12;
            for (int i = 0; i < contourAttributes.length; i++) {

                DisplayType displayType = getVectorType(contourAttributes[i]
                        .getType());
                String attrType = contourAttributes[i].getType().toUpperCase();

                if (attrType.contains("M") && gridPointMarkerDisplay == null) {
                    if (gdPrxy != null && gdPrxy.getNewSpatialObject() != null) {
                        gridPointMarkerDisplay = new GridPointMarkerDisplay(
                                contourAttributes[i].getMarker(),
                                getNcMapDescriptor(),
                                gdPrxy.getNewSpatialObject());
                    } else if (ncgribLogger.enableDiagnosticLogs()) {
                        logMissingDataCondition(i);
                    }
                }

                if (attrType.contains("G") && gridIndicesDisplay == null) {
                    if (gdPrxy != null && gdPrxy.getNewSpatialObject() != null) {
                        gridIndicesDisplay = new GridIndicesDisplay(
                                GempakColor.convertToRGB(gridRscData
                                        .getGrdlbl()), getNcMapDescriptor(),
                                gdPrxy.getNewSpatialObject());
                    } else if (ncgribLogger.enableDiagnosticLogs()) {
                        logMissingDataCondition(i);
                    }
                }
                /*
                 * Vector data
                 */
                if (displayType == DisplayType.ARROW
                        || displayType == DisplayType.BARB) {
                    t11 = System.currentTimeMillis();
                    boolean isDirectionalArrow = attrType.equals("D");

                    if (vectorDisplay[i] == null) {
                        FloatGridData rec = null;
                        rec = findGriddedDataFromVector(i);

                        // Avoid duplicate data retrieval
                        if (rec == null) {
                            rec = getGriddedData(i);
                        }
                        if (rec != null) {
                            hasData = false;
                            if (gdPrxy != null
                                    && gdPrxy.getNewSpatialObject() != null) {
                                vectorDisplay[i] = new GriddedVectorDisplay(
                                        rec, displayType, isDirectionalArrow,
                                        getNcMapDescriptor(),
                                        gdPrxy.getNewSpatialObject(),
                                        contourAttributes[i]);
                                hasData = true;
                            } else if (ncgribLogger.enableDiagnosticLogs()) {
                                logMissingDataCondition(i);
                            }
                        } else {
                            if (ncgribLogger.enableDiagnosticLogs()) {
                                logMissingDataCondition(i);
                            }
                            logger.debug(this.getClass().getCanonicalName()
                                    + ":\n"
                                    + "***Something may be wrong: griddedData was null, was populated, but was found to still be null? (1)\n"
                                    + " For debug, possibly useful parameters used in the load attempt follow:\n");
                            logMissingDataCondition(i);
                        }
                    } else {
                        gridData = vectorDisplay[i].getData();
                        if (gridData != null) {
                            hasData = false;
                            if (vectorDisplay[i].checkAttrsChanged(displayType,
                                    isDirectionalArrow,
                                    contourAttributes[i].getWind())) {
                                vectorDisplay[i].dispose();
                                if (gdPrxy != null
                                        && gdPrxy.getNewSpatialObject() != null) {

                                    vectorDisplay[i] = new GriddedVectorDisplay(
                                            gridData, displayType,
                                            isDirectionalArrow,
                                            getNcMapDescriptor(),
                                            gdPrxy.getNewSpatialObject(),
                                            contourAttributes[i]);
                                    hasData = true;
                                } else if (ncgribLogger.enableDiagnosticLogs()) {
                                    logMissingDataCondition(i);
                                }
                            }
                        } else {
                            if (ncgribLogger.enableDiagnosticLogs()) {
                                logMissingDataCondition(i);
                            }
                            logger.debug(this.getClass().getCanonicalName()
                                    + ":\n"
                                    + "***Something may be wrong: gridData was null, was populated, but was found to still be null? (2)\n"
                                    + " For debug, possibly useful parameters used in the load attempt follow:\n");
                            logMissingDataCondition(i);
                        }
                    }
                    t12 = System.currentTimeMillis();
                    logger.debug("==create vector took:" + (t12 - t11));
                } else if (displayType == DisplayType.CONTOUR
                        || displayType == DisplayType.STREAMLINE
                        || attrType.contains("P")) {
                    /*
                     * Scalar data
                     */
                    t11 = System.currentTimeMillis();
                    String contourName = "";
                    if (gdPrxy != null && gdPrxy.getNewSpatialObject() != null) {
                        contourName = createContourName(gdPrxy,
                                contourAttributes[i]);
                    } else if (ncgribLogger.enableDiagnosticLogs()) {
                        logMissingDataCondition(i);
                    }

                    // New creation
                    if (contourRenderable[i] == null) {

                        // Duplicate data
                        gridData = findGriddedDataFromContourRenderable(i);

                        // Avoid duplicate data retrieval
                        if (gridData == null) {
                            gridData = getGriddedData(i);
                        }

                        if (gridData != null) {
                            hasData = false;
                            if (gdPrxy != null
                                    && gdPrxy.getNewSpatialObject() != null) {

                                // Set the proxy for this frame. Gets used in
                                // ContourSupport to get imageGridGeometry.
                                FrameData frame = (FrameData) getCurrentFrame();
                                if (frame != null) {
                                    frame.setProxy(gdPrxy);
                                }

                                contourRenderable[i] = new ContourRenderable(
                                        gridData, getNcMapDescriptor(),
                                        MapUtil.getGridGeometry(gdPrxy
                                                .getNewSpatialObject()),
                                        contourAttributes[i], contourName);
                                contourRenderable[i]
                                        .setResource(NcgridResource.this);
                                hasData = true;
                            } else if (ncgribLogger.enableDiagnosticLogs()) {
                                logMissingDataCondition(i);
                            }
                        } else {
                            if (ncgribLogger.enableDiagnosticLogs()) {
                                logMissingDataCondition(i, false);
                            }
                            logger.debug(this.getClass().getCanonicalName()
                                    + ":\n"
                                    + "***Something may be wrong: gridData was null, was populated, but was found to still be null? (3)\n"
                                    + " For debug, possibly useful parameters used in the load attempt follow:\n");
                            logMissingDataCondition(i, false);
                        }
                    }
                    // Attributes or navigation change
                    else {
                        contourRenderable[i]
                                .setContourAttributes(contourAttributes[i]);
                    }

                    // Grid point value
                    if (attrType.contains("P") && contourRenderable[i] != null) {
                        if (gridPointValueDisplay == null
                                || !contourAttributes[i].getGdpfun()
                                        .equalsIgnoreCase(gfunc)
                                || !contourAttributes[i].getGlevel()
                                        .equalsIgnoreCase(glevel)
                                || !contourAttributes[i].getGvcord()
                                        .equalsIgnoreCase(gvcord)
                                || !contourAttributes[i].getSkip()
                                        .equalsIgnoreCase(skip)
                                || !contourAttributes[i].getFilter()
                                        .equalsIgnoreCase(filter)
                                || !contourAttributes[i].getScale()
                                        .equalsIgnoreCase(scale)
                                || !contourAttributes[i].getColors()
                                        .equalsIgnoreCase(colors)
                                || !contourAttributes[i].getText()
                                        .equalsIgnoreCase(text)) {

                            if (gdPrxy != null
                                    && gdPrxy.getNewSpatialObject() != null) {

                                gridPointValueDisplay[i] = createGridPointValueDisplay(
                                        contourRenderable[i].getData(), gdPrxy,
                                        contourAttributes[i]);

                                gfunc = contourAttributes[i].getGdpfun();
                                glevel = contourAttributes[i].getGlevel();
                                gvcord = contourAttributes[i].getGvcord();
                                skip = contourAttributes[i].getSkip();
                                filter = contourAttributes[i].getFilter();
                                scale = contourAttributes[i].getScale();
                                colors = contourAttributes[i].getColors();
                                text = contourAttributes[i].getText();
                            } else if (ncgribLogger.enableDiagnosticLogs()) {
                                logMissingDataCondition(i, false);
                            }

                        }
                    }

                    // create HILO symbols
                    if ((attrType.contains("F") || attrType.contains("I") || attrType
                            .contains("C")) && (contourRenderable[i] != null)) {
                        if (contourRenderable[i].getGridRelativeHiLo() == null) {
                            if (gdPrxy != null
                                    && gdPrxy.getNewSpatialObject() != null) {

                                contourRenderable[i]
                                        .setGridRelativeHiLo(createGridRelativeHiLoDisplay(
                                                contourRenderable[i].getData(),
                                                gdPrxy, contourAttributes[i]));
                            } else if (ncgribLogger.enableDiagnosticLogs()) {
                                logMissingDataCondition(i, false);
                            }

                        }
                    }
                    t12 = System.currentTimeMillis();
                    logger.debug("==init contour took:" + (t12 - t11));
                }
            } // end of for loop

            Collections.sort(NcgridResource.this.dataTimes);

            frameLoaded = true;
            long t1 = System.currentTimeMillis();
            if (ncgribLogger.enableRscLogs()) {
                logger.debug("*updateFrameData("
                        + ((gdPrxy != null) ? gdPrxy.getDataTime().toString()
                                : " ") + "): completed diagnostic took: "
                        + (t1 - st));
            }
            logger.debug("updateFrameData: from init resource to complete diagnostic took: "
                    + (t1 - initTime));
            if (getGraphicsTarget() != null && getPaintProperties() != null) {
                generateContours(gdPrxy.getDataTime().toString());
            }

            cacheData.clear();
            return true;
        }

        public void procAttrsModified() {

            synchronized (this) {
                FloatGridData gridData = null;
                if (contourRenderable == null) {
                    return;
                }

                for (int i = 0; i < contourRenderable.length; i++) {

                    DisplayType displayType = getVectorType(contourAttributes[i]
                            .getType());
                    String attrType = contourAttributes[i].getType()
                            .toUpperCase();

                    if (attrType.contains("M")
                            && gridPointMarkerDisplay == null) {

                        if (gdPrxy != null
                                && gdPrxy.getNewSpatialObject() != null) {
                            gridPointMarkerDisplay = new GridPointMarkerDisplay(
                                    contourAttributes[i].getMarker(),
                                    getNcMapDescriptor(),
                                    gdPrxy.getNewSpatialObject());
                        } else if (ncgribLogger.enableDiagnosticLogs()) {
                            logMissingDataCondition(i);
                        }
                    } else if (!attrType.contains("M")
                            && gridPointMarkerDisplay != null) {
                        gridPointMarkerDisplay = null;
                    }

                    if (attrType.contains("G") && gridIndicesDisplay == null) {
                        if (gdPrxy != null
                                && gdPrxy.getNewSpatialObject() != null) {
                            gridIndicesDisplay = new GridIndicesDisplay(
                                    GempakColor.convertToRGB(gridRscData
                                            .getGrdlbl()),
                                    getNcMapDescriptor(),
                                    gdPrxy.getNewSpatialObject());
                        } else if (ncgribLogger.enableDiagnosticLogs()) {
                            logMissingDataCondition(i);
                        }
                    } else if (!attrType.contains("G")
                            && gridIndicesDisplay != null) {
                        gridIndicesDisplay = null;
                    }
                    /*
                     * Vector data
                     */
                    if (displayType == DisplayType.ARROW
                            || displayType == DisplayType.BARB) {

                        boolean isDirectionalArrow = attrType.equals("D");

                        if (vectorDisplay != null && vectorDisplay[i] != null) {
                            gridData = vectorDisplay[i].getData();
                            if (gridData != null) {
                                if (vectorDisplay[i].checkAttrsChanged(
                                        displayType, isDirectionalArrow,
                                        contourAttributes[i].getWind())) {
                                    vectorDisplay[i].dispose();
                                    if (gdPrxy != null
                                            && gdPrxy.getNewSpatialObject() != null) {
                                        vectorDisplay[i] = new GriddedVectorDisplay(
                                                gridData, displayType,
                                                isDirectionalArrow,
                                                getNcMapDescriptor(),
                                                gdPrxy.getNewSpatialObject(),
                                                contourAttributes[i]);
                                    } else if (ncgribLogger
                                            .enableDiagnosticLogs()) {
                                        logMissingDataCondition(i);
                                    }
                                }
                            }
                        }

                    } else if (displayType != DisplayType.ARROW
                            && displayType != DisplayType.BARB) {
                        if (vectorDisplay != null && vectorDisplay[i] != null) {
                            vectorDisplay[i].dispose();
                            vectorDisplay[i] = null;
                        }
                    }
                    if (displayType == DisplayType.CONTOUR
                            || displayType == DisplayType.STREAMLINE
                            || attrType.contains("P")) {

                        if (contourRenderable[i] != null) {
                            contourRenderable[i]
                                    .setContourAttributes(contourAttributes[i]);
                            contourRenderable[i].updatedContourRenderable();
                        }

                        // Grid point value
                        if (attrType.contains("P")
                                && contourRenderable[i] != null) {
                            if (gridPointValueDisplay == null
                                    || !contourAttributes[i].getGdpfun()
                                            .equalsIgnoreCase(gfunc)
                                    || !contourAttributes[i].getGlevel()
                                            .equalsIgnoreCase(glevel)
                                    || !contourAttributes[i].getGvcord()
                                            .equalsIgnoreCase(gvcord)
                                    || !contourAttributes[i].getSkip()
                                            .equalsIgnoreCase(skip)
                                    || !contourAttributes[i].getFilter()
                                            .equalsIgnoreCase(filter)
                                    || !contourAttributes[i].getScale()
                                            .equalsIgnoreCase(scale)
                                    || !contourAttributes[i].getColors()
                                            .equalsIgnoreCase(colors)
                                    || !contourAttributes[i].getText()
                                            .equalsIgnoreCase(text)) {

                                if (gdPrxy != null
                                        && gdPrxy.getNewSpatialObject() != null) {
                                    gridPointValueDisplay[i] = createGridPointValueDisplay(
                                            contourRenderable[i].getData(),
                                            gdPrxy, contourAttributes[i]);

                                    gfunc = contourAttributes[i].getGdpfun();
                                    glevel = contourAttributes[i].getGlevel();
                                    gvcord = contourAttributes[i].getGvcord();
                                    skip = contourAttributes[i].getSkip();
                                    filter = contourAttributes[i].getFilter();
                                    scale = contourAttributes[i].getScale();
                                    colors = contourAttributes[i].getColors();
                                    text = contourAttributes[i].getText();
                                } else if (ncgribLogger.enableDiagnosticLogs()) {
                                    logMissingDataCondition(i);
                                }
                            }
                        }

                        // create HILO symbols
                        if ((attrType.contains("F") || attrType.contains("I") || attrType
                                .contains("C"))
                                && (contourRenderable[i] != null)) {

                            if (contourRenderable[i].getGridRelativeHiLo() == null) {
                                if (gdPrxy != null
                                        && gdPrxy.getNewSpatialObject() != null) {
                                    contourRenderable[i]
                                            .setGridRelativeHiLo(createGridRelativeHiLoDisplay(
                                                    contourRenderable[i]
                                                            .getData(), gdPrxy,
                                                    contourAttributes[i]));
                                } else if (ncgribLogger.enableDiagnosticLogs()) {
                                    logMissingDataCondition(i);
                                }
                            } else {
                                if (contourAttributes[i].getHilo() == null
                                        || contourAttributes[i].getHilo()
                                                .length() == 0) {
                                    contourRenderable[i]
                                            .setGridRelativeHiLo(null);
                                } else {
                                    if (gdPrxy != null
                                            && gdPrxy.getNewSpatialObject() != null) {
                                        contourRenderable[i]
                                                .setGridRelativeHiLo(createGridRelativeHiLoDisplay(
                                                        contourRenderable[i]
                                                                .getData(),
                                                        gdPrxy,
                                                        contourAttributes[i]));
                                    } else if (ncgribLogger
                                            .enableDiagnosticLogs()) {
                                        logMissingDataCondition(i);
                                    }
                                }
                            }
                        }
                    }
                } // end of for loop

            } // end of synchronized
            frameLoaded = true;
            paintAble = true;
            issueRefresh();
        }

        private boolean createContours() {

            if (contourRenderable == null || !frameLoaded) {
                return false;
            }
            if (getGraphicsTarget() == null || getPaintProperties() == null) {
                return false;
            }

            long t1 = System.currentTimeMillis();
            int cnt = contourRenderable.length;
            for (int i = 0; i < cnt; i++) {
                if (contourAttributes[i] != null) {
                    String type = contourAttributes[i].getType().toUpperCase();

                    ContourRenderable contourGroup = contourRenderable[i];

                    if ((type.contains("C") || type.contains("F")
                            || type.contains("I") || type.contains("S"))
                            && (contourGroup != null)) {

                        try {
                            contourGroup.createContours(getGraphicsTarget(),
                                    getPaintProperties());
                        } catch (VizException e) {
                            return false;
                        }
                    }

                    if (type.contains("B") || type.contains("A")
                            || type.contains("D")) {

                        GriddedVectorDisplay griddedVectorDisplay = vectorDisplay[i];

                        if (griddedVectorDisplay != null) {
                            griddedVectorDisplay.createWireFrame(gridRscData,
                                    getGraphicsTarget(), getPaintProperties());
                        }
                    }
                }
            }
            paintAble = true;
            issueRefresh();
            long t2 = System.currentTimeMillis();
            logger.debug("**createContours for("
                    + gdPrxy.getDataTime().toString() + ") took:" + (t2 - t1));
            if (ncgribLogger.enableTotalTimeLogs()) {
                logger.debug("**From init to complete createContours/wireframe ("
                        + gdPrxy.getDataTime().toString()
                        + ") took:"
                        + (t2 - initTime));
            }
            return true;
        }

        Boolean isFrameLoaded() {
            return frameLoaded;
        }

        Boolean isPaintAble() {
            return paintAble;
        }

        public void setFrameLoadedFlag(boolean load) {
            frameLoaded = load;
        }

        public void setPaintFlag(boolean paint) {
            paintAble = paint;
        }

        private String createContourName(NcGridDataProxy gdPrxy,
                ContourAttributes contourAttributes) {
            StringBuilder contourName = new StringBuilder();
            contourName.append(gdPrxy.getDataTime().toString());
            return contourName.toString();
        }

        public boolean getReProjectFlag() {
            return isReProject;
        }

        public void setReProjectFlag(boolean proj) {
            isReProject = proj;
        }

        public boolean getAttrsModifiedFlag() {
            return isAttrModified;
        }

        public void setAttrsModifiedFlag(boolean mod) {
            isAttrModified = mod;
        }

        @Override
        public void dispose() {

            if (genContrs != null) {
                genContrs.cancel();
            }

            if (contourRenderable != null) {
                for (ContourRenderable cr : contourRenderable) {
                    if (cr != null) {
                        cr.dispose();
                    }

                }
                contourRenderable = null;
            }

            if (vectorDisplay != null) {
                for (GriddedVectorDisplay vd : vectorDisplay) {
                    if (vd != null) {
                        vd.dispose();
                    }

                }
                vectorDisplay = null;
            }
            if (gridPointValueDisplay != null) {
                for (GridPointValueDisplay gp : gridPointValueDisplay) {
                    if (gp != null) {
                        gp.dispose();
                    }
                }
                gridPointValueDisplay = null;
            }
            gdPrxy = null;
        }

        private FloatGridData findGriddedDataFromContourRenderable(int i) {
            FloatGridData rec = null;
            if (i > 0) {
                for (int n = 0; n < i; n++) {
                    if (contourRenderable[n] != null) {
                        if (contourRenderable[n].isMatch(contourAttributes[i])) {
                            rec = contourRenderable[n].getData();
                            break;
                        }
                    }
                }
            }
            return rec;
        }

        private FloatGridData findGriddedDataFromVector(int i) {
            FloatGridData rec = null;

            if (i > 0) {
                for (int n = 0; n < i; n++) {
                    if (vectorDisplay[n] != null) {
                        if (vectorDisplay[n].isMatch(contourAttributes[i])) {
                            rec = vectorDisplay[n].getData();
                            break;
                        }
                    }
                }
            }
            return rec;
        }

        private FloatGridData getGriddedData(int i) {
            FloatGridData gridData = null;
            try {
                long t1 = System.currentTimeMillis();

                if (gdPrxy != null && gdPrxy.getNewSpatialObject() != null) {
                    gridData = getDataRecord(gdPrxy, contourAttributes[i],
                            this.cacheData);
                } else if (ncgribLogger.enableDiagnosticLogs()) {
                    logMissingDataCondition(i);
                }
                long t2 = System.currentTimeMillis();
                if (gridData != null) {
                    logger.debug("getDataRecord return: kx="
                            + (int) gridData.getSizes()[0] + "  ky="
                            + (int) gridData.getSizes()[1]);
                    if (subgObj != null) {
                        gdPrxy.setNewSpatialObject(subgObj);
                    }
                }
                if (ncgribLogger.enableRscLogs()) {
                    logger.debug("getGriddedData contour/streamline/vector grid data("
                            + gdPrxy.getDataTime().toString()
                            + ") took:"
                            + (t2 - t1));
                }
            } catch (DgdrivException e) {
                statusHandler.error("Error getting data from Dgdriv", e);

                return null;
            }
            return gridData;
        }
    }

    /**
     * Constructor
     */
    public NcgridResource(NcgridResourceData data, LoadProperties props) {
        super(data, props);
        gridRscData = data;
    }

    /**
     * override the base version which queries the db based on the metadatamap
     * since this will return too many objects which won't be used. This will
     * just query the availableTimes and use them to time match to the frames.
     */

    @Override
    public void queryRecords() throws VizException {

        ResourceName rscName = getResourceData().getResourceName();

        DataTime cycleTime = rscName.getCycleTime();

        // latest should already be resolved here.
        if (rscName.isLatestCycleTime()) {
            return;
        }

        List<DataTime> availableTimes = new ArrayList<>();

        if (cycleTime != null
                && gridRscData.getPluginName().equalsIgnoreCase(
                        GempakGrid.gempakPluginName)) {
            try {
                String dataLocation = null;
                try {
                    dataLocation = GempakGrid.getGempakGridPath(gridRscData
                            .getGdfile());

                } catch (VizException e) {
                    statusHandler.error(
                            "Unable to specify location for "
                                    + gridRscData.getPluginName() + " "
                                    + gridRscData.getGdfile(), e);
                    return;
                }
                String[] gridAvailableTimes = GempakGrid.getAvailableGridTimes(
                        dataLocation, cycleTime.toString(), gridRscData
                                .getGdfile().toLowerCase());
                for (int ii = 0; ii < gridAvailableTimes.length; ii++) {
                    availableTimes.add(new DataTime(gridAvailableTimes[ii]));
                }
            } catch (Exception e) {
                return;
            }
        } else {
            availableTimes = gridRscData.getAvailableDataTimes();
        }

        ArrayList<DataTime> dataTimes = new ArrayList<>();

        // loop thru all the times for the grib records for this model and
        // if the time matches the cycle time for this resource and if
        // it hasn't already been added, add it to the queue of data objects
        // to be processed by the abstract class.
        for (DataTime dt : availableTimes) {

            // create a dataTime without a possible validPeriod.
            DataTime availTime = new DataTime(dt.getRefTime(), dt.getFcstTime());
            DataTime refTime = new DataTime(dt.getRefTime());

            if (cycleTime != null) {
                if (cycleTime.equals(refTime)) {
                    if (!dataTimes.contains(availTime)) {
                        dataTimes.add(availTime);
                    }
                }
            } else { // for grid analysis
                dataTimes.add(availTime);
            }
        }
        setDataTimesForDgdriv(dataTimes);
        setAllFramesAsPopulated();
    }

    private void setDataTimesForDgdriv(ArrayList<DataTime> dataTimes) {
        dataTimesForDgdriv = dataTimes;
    }

    // TODO : if called from the auto update code this will be a Record so add
    // code to get the time and process as an NcGridDataTimeObj
    //
    @Override
    protected IRscDataObject[] processRecord(Object obj) {
        if (obj instanceof DataTime) {
            NcGridDataProxy rscDataObj = new NcGridDataProxy((DataTime) obj);
            return new NcGridDataProxy[] { rscDataObj };
        } else if (obj instanceof GridRecord) {
            NcGridDataProxy rscDataObj = new NcGridDataProxy(
                    ((GridRecord) obj).getDataTime());
            return new NcGridDataProxy[] { rscDataObj };
        } else {
            logger.info("Unexpected object in NcGridResource.processRecord ");
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.core.rsc.IVizResource#getName()
     */
    @Override
    public String getName() {
        FrameData currFrame = (FrameData) getCurrentFrame();
        String nameStr = "";
        /*
         * rm 5/-1/. 5/-1/ ~ @ HEIGHTS, ISOTACHS AND WIND (KTS)!0
         */
        TITLE title = new TITLE(gridRscData.getTitle());
        String tit = title.getTitleString();

        if (currFrame == null || currFrame.gdPrxy == null || !currFrame.hasData) {
            if (tit != null) {
                return String.format("%s %s", gridRscData.getGdfile(), tit)
                        + "-No Data";
            } else {
                return String.format("%s", gridRscData.getGdfile())
                        + "-No Data";
            }
        }

        if (tit != null) {
            nameStr = generateTitleInfo(tit, currFrame.gdPrxy.getDataTime());
            gridRscData.setLegendColor(title.getTitleColor());
        }

        return String.format("%s", nameStr);
    }

    @Override
    public void disposeInternal() {
        this.dataTimesForDgdriv.clear();
        if (ncgridLoader != null) {
            ncgridLoader.setCancelFlag(true);
            ncgridLoader.cancel();
        }
        if (ncgribAttrsModified != null) {
            ncgribAttrsModified.setCancelFlag(true);
            ncgribAttrsModified.cancel();
        }
        for (AbstractFrameData frameData : frameDataMap.values()) {
            frameData.dispose();
        }
        frameDataMap.clear();
    }

    @Override
    public void initResource(IGraphicsTarget target) throws VizException {
        synchronized (this) {
            long t0 = 0;
            QUERY_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
            if (initTime == 0) {
                initTime = System.currentTimeMillis();
            }
            t0 = initTime;
            separateAttributes();
            getClippingArea();
            getNcgridLoggerCfgInfo();
            this.lastTarget = target;
            queryRecords();
            long t1 = System.currentTimeMillis();
            if (ncgribLogger.enableRscLogs()) {
                logger.debug("NcgridResource.initResource query all avariable times: "
                        + (t1 - t0));
            }
            if (ncgridLoader != null) {
                ncgridLoader.cancel();
            }
            ncgridLoader = new NcgridLoaderJob("Ncgrid Loading...");
            ncgridLoader.loadNcgridData();
            t1 = System.currentTimeMillis();
            logger.debug("\t\t NcgridResource.initResource took: " + (t1 - t0));
        }

    }

    @Override
    public void paintFrame(AbstractFrameData frmData, IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {

        if (target == null || paintProps == null) {
            return;
        }
        this.lastTarget = target;
        this.lastPaintProps = paintProps;
        FrameData currFrame = (FrameData) frmData; // will not be null
        if (!currFrame.isFrameLoaded()) {
            return;
        }
        if (!currFrame.isPaintAble()) {
            if (currFrame.getGenCntrJob() == null) {
                // repaints start here
                currFrame.generateContours("Rendering");
            }
            return;
        }

        long t11 = System.currentTimeMillis();

        if (currFrame.gdPrxy == null || currFrame.contourRenderable == null
                || currFrame.vectorDisplay == null) {
            return;
        }

        for (int i = 0; i < currFrame.contourRenderable.length; i++) {

            String type = contourAttributes[i].getType().toUpperCase();

            /*
             * Plot grid point markers if needed
             */
            if (type.contains("M")) {
                GridPointMarkerDisplay gridPointMarkerDisplay = currFrame.gridPointMarkerDisplay;
                if (gridPointMarkerDisplay != null) {
                    gridPointMarkerDisplay.paint(target, paintProps);
                }
            }
            /*
             * Plot grid indices(row/column numbers) if requested
             */
            if (type.contains("G")) {
                GridIndicesDisplay gridIndicesDisplay = currFrame.gridIndicesDisplay;
                if (gridIndicesDisplay != null) {
                    gridIndicesDisplay.paint(target, paintProps);
                }
            }
            /*
             * Draw wind barb or wind arrow
             */
            if (type.contains("B") || type.contains("A") || type.contains("D")) {

                GriddedVectorDisplay griddedVectorDisplay = currFrame.vectorDisplay[i];
                if (griddedVectorDisplay != null) {
                    griddedVectorDisplay.paint(gridRscData, target, paintProps);
                }
            } else {
                /*
                 * Draw contours or streamlines
                 */
                ContourRenderable contourGroup = currFrame.contourRenderable[i];
                if (contourGroup == null) {
                    continue;
                }

                if (contourGroup != null
                        && (type.contains("C") || type.contains("F")
                                || type.contains("I") || type.contains("S"))) {
                    if (currFrame.getReProjectFlag()) {
                        contourGroup
                                .setMapProject(currFrame.getReProjectFlag());
                        contourGroup.setIMapDescriptor(getNcMapDescriptor());
                    }
                    if (currFrame.getAttrsModifiedFlag()) {
                        contourGroup.setMapProject(currFrame
                                .getAttrsModifiedFlag());
                    }
                    contourGroup.paint(target, paintProps);
                    contourGroup.setMapProject(false);
                }
                /*
                 * Plot HILO if needed
                 */
                if (type.contains("C") || type.contains("F")
                        || type.contains("I")) {
                    GridRelativeHiLoDisplay gridRelativeHiLoDisplay = contourGroup
                            .getGridRelativeHiLo();
                    if (gridRelativeHiLoDisplay != null) {
                        gridRelativeHiLoDisplay
                                .setDescriptor(getNcMapDescriptor());
                        gridRelativeHiLoDisplay.paint(target, paintProps);
                    }
                }
                /*
                 * Draw grid point values if needed
                 */
                if (type.contains("P")) {
                    GridPointValueDisplay gridPointValueDisplay = currFrame.gridPointValueDisplay[i];
                    if (gridPointValueDisplay != null) {
                        if (currFrame.getReProjectFlag()) {
                            gridPointValueDisplay.reproject();
                        }
                        gridPointValueDisplay.paint(target, paintProps);
                    }
                }

            }
        }
        currFrame.setReProjectFlag(false);
        currFrame.setAttrsModifiedFlag(false);
        if (currFrame.isFirst) {
            currFrame.isFirst = false;
            long t1 = System.currentTimeMillis();
            logger.debug("Paint this frame("
                    + currFrame.gdPrxy.getDataTime().toString() + ") took:"
                    + (t1 - t11));
        }
    }

    @Override
    public void project(CoordinateReferenceSystem mapData) throws VizException {
        super.project(mapData);
        for (AbstractFrameData fd : frameDataMap.values()) {
            FrameData frameData = (FrameData) fd;
            frameData.setReProjectFlag(true);
        }
    }

    @Override
    public String inspect(ReferencedCoordinate coord) throws VizException {
        return "Sampling not implemented for ncgridResource";
    }

    @Override
    protected AbstractFrameData createNewFrame(DataTime frameTime,
            int frameInterval) {
        return new FrameData(frameTime, frameInterval);
    }

    /**
     * Retrieve the data record
     * 
     * @param obj
     * @return
     * @throws FileNotFoundException
     * @throws StorageException
     * @throws VizException
     */
    protected FloatGridData getDataRecord(NcGridDataProxy gdPrxy,
            ContourAttributes cattr, NcgridDataCache cacheData)
            throws DgdrivException {

        if (gdPrxy == null) {
            return null;
        }
        /*
         * Instantiate and populate the object for data retrieval from GEMPAK GD
         */
        String inputGdfile = gridRscData.getGdfile();

        if (gridRscData.getEnsembelMember() != null) {
            inputGdfile = inputGdfile + ":" + gridRscData.getEnsembelMember();
        } else {
            if (gridRscData.getEventName() != null) {
                inputGdfile = inputGdfile + ":" + gridRscData.getEventName();
            }
        }

        ArrayList<DataTime> dataTimes = new ArrayList<>();
        dataTimes.add(gdPrxy.getDataTime());
        synchronized (Dgdriv.class) {
            Dgdriv aDgdriv = new Dgdriv();
            aDgdriv.setResourceData(gridRscData);
            aDgdriv.setCycleForecastTimes(dataTimes);
            aDgdriv.setSpatialObject(gdPrxy.getSpatialObject());
            aDgdriv.setGdattim(gdPrxy.getDataTime().toString());
            aDgdriv.setGarea("dset");
            aDgdriv.setGdfile(inputGdfile);
            aDgdriv.setGdpfun(cattr.getGdpfun());
            aDgdriv.setGlevel(cattr.getGlevel());
            aDgdriv.setGvcord(cattr.getGvcord());
            aDgdriv.setScale(cattr.getScale());
            aDgdriv.setDataSource(gridRscData.getPluginName());
            aDgdriv.setCacheData(cacheData);
            if (ncgribPreferences != null) {
                aDgdriv.setPreferences(ncgribPreferences);
            }

            DisplayType displayType = getVectorType(cattr.getType());
            if (displayType == DisplayType.ARROW
                    || displayType == DisplayType.BARB
                    || displayType == DisplayType.STREAMLINE) {
                /*
                 * Specify vector data retrieval from GEMPAK GD
                 */
                aDgdriv.setScalar(false);
                if (displayType == DisplayType.ARROW) {
                    aDgdriv.setArrowVector(true);
                } else {
                    aDgdriv.setArrowVector(false);
                }

                // for directional arrow
                if (cattr.getType().equalsIgnoreCase("D")) {
                    aDgdriv.setScalar(true);
                    aDgdriv.setArrowVector(false);
                }

            } else {
                /*
                 * Specify scalar data retrieval from GEMPAK GD
                 */
                aDgdriv.setScalar(true);
                aDgdriv.setArrowVector(false);
            }
            FloatGridData data;
            try {
                data = aDgdriv.execute();
                if (data != null) {
                    subgObj = aDgdriv.getSubgSpatialObj();
                }
                return data;
            } catch (DgdrivException e) {
                statusHandler.error("GEMPAK GD error stack:", e);
                return null;
            }
        }
    }

    protected IGraphicsTarget getGraphicsTarget() {
        return this.lastTarget;
    }

    protected PaintProperties getPaintProperties() {
        return this.lastPaintProps;
    }

    @Override
    public void resourceAttrsModified() {
        // Repaint the data
        if (ncgribAttrsModified != null) {
            ncgribAttrsModified.cancel();
        }
        ncgribAttrsModified = new NcgridAttrModifiedJob(
                "Ncgrid Attrs Modifying...");
        ncgribAttrsModified.procAttrs();
    }

    private GridPointValueDisplay createGridPointValueDisplay(
            FloatGridData rec, NcGridDataProxy gdPrxy, ContourAttributes attr) {

        if (rec == null || rec.getXdata() == null) {
            return null;
        }

        if (!attr.getType().toUpperCase().contains("P")) {
            return null;
        }

        FloatBuffer plotData = rec.getXdata();
        COLORS color = new COLORS(attr.getColors());

        text = new TextStringParser(attr.getText());

        return new GridPointValueDisplay(plotData, color.getFirstColor(),
                text.getTextSize(), getNcMapDescriptor(),
                gdPrxy.getNewSpatialObject());
    }

    private GridRelativeHiLoDisplay createGridRelativeHiLoDisplay(
            FloatGridData rec, NcGridDataProxy gdPrxy, ContourAttributes attr) {

        DisplayType displayType = getVectorType(attr.getType());
        if (displayType != DisplayType.CONTOUR) {
            return null;
        }

        if (rec == null || rec.getXdata() == null) {
            return null;
        }

        if (attr.getHilo() == null || attr.getHilo().isEmpty()) {
            return null;
        }

        HILOStringParser hilo = new HILOStringParser(attr.getHilo());

        if (!(hilo.isHiLoStringParsed())) {
            // Parse HILO failure
            return null;
        }

        HILOBuilder hiloBuild = hilo.getInstanceOfHiLoBuilder();

        int nx = gdPrxy.getNewSpatialObject().getNx();
        int ny = gdPrxy.getNewSpatialObject().getNy();

        HILORelativeMinAndMaxLocator hiloLocator = new HILORelativeMinAndMaxLocator(
                rec.getXdataAsArray(), nx, ny, hiloBuild.getRadius(),
                hiloBuild.getInterp(), hiloBuild.getRangeHiMinval(),
                hiloBuild.getRangeHiMaxval(), hiloBuild.getRangeLoMinval(),
                hiloBuild.getRangeLoMaxval());

        if (!(hiloLocator.isHILOMinAndMaxLocated())) {
            // Not locate HILO minima and maxima
            return null;
        }

        if (gridRscData.getHlsym() == null) {
            gridRscData.setHlsym("");
        }

        HLSYM hlsym = new HLSYM(gridRscData.getHlsym());
        TextStringParser txtMarkerStr = new TextStringParser(
                hlsym.getMarkerString());
        TextStringParser txtValueStr = new TextStringParser(
                hlsym.getValueString());

        return new GridRelativeHiLoDisplay(hiloBuild, hiloLocator,
                hiloBuild.getCountHi(), hiloBuild.getCountLo(), txtMarkerStr,
                txtValueStr, getNcMapDescriptor(), gdPrxy.getNewSpatialObject());
    }

    public DisplayType getVectorType(String type) {
        if (type.toUpperCase().contains("B")) {
            return DisplayType.BARB;
        } else if (type.toUpperCase().contains("A")
                || type.toUpperCase().contains("D")) {
            return DisplayType.ARROW;
        } else if (type.toUpperCase().contains("S")) {
            return DisplayType.STREAMLINE;
        } else if (type.toUpperCase().contains("C")
                || type.toUpperCase().contains("F")
                || type.toUpperCase().contains("I")) {
            return DisplayType.CONTOUR;
        } else {
            return null;
        }
    }

    private void separateAttributes() {
        /*
         * A trailing "!" may cause the field before "!" to be created twice -
         * so need to remove it before splitting into array.
         */

        String gfuncStr = gridRscData.getGdpfun().replaceAll("\\s+", "").trim();
        if (gfuncStr != null && gfuncStr.endsWith("!")) {
            gfuncStr = gfuncStr.substring(0, gfuncStr.length() - 1);
        }
        String[] gfuncArray = gfuncStr.split("!", -1);

        String[] glevelArray = gridRscData.getGlevel().replaceAll("\\s+", "")
                .split("!", -1);
        String[] gvcordArray = gridRscData.getGvcord().replaceAll("\\s+", "")
                .split("!", -1);
        String[] skipArray = gridRscData.getSkip().replaceAll("\\s+", "")
                .split("!", -1);
        String[] filterArray = gridRscData.getFilter().replaceAll("\\s+", "")
                .split("!", -1);
        String[] scaleArray = gridRscData.getScale().replaceAll("\\s+", "")
                .split("!", -1);
        String[] typeArray = gridRscData.getType().replaceAll("\\s+", "")
                .split("!", -1);
        String[] cintArray = gridRscData.getCint().replaceAll("\\s+", "")
                .split("!", -1);
        String[] lineArray = gridRscData.getLineAttributes()
                .replaceAll("\\s+", "").split("!", -1);
        String[] fintArray = gridRscData.getFint().replaceAll("\\s+", "")
                .split("!", -1);
        String[] flineArray = gridRscData.getFline().replaceAll("\\s+", "")
                .split("!", -1);
        String[] hiloArray = gridRscData.getHilo().replaceAll("\\s+", "")
                .split("!", -1);
        String[] hlsymArray = gridRscData.getHlsym().replaceAll("\\s+", "")
                .split("!", -1);
        String[] windArray = gridRscData.getWind().replaceAll("\\s+", "")
                .split("!", -1);
        String[] markerArray = gridRscData.getMarker().replaceAll("\\s+", "")
                .split("!", -1);
        String[] clrbarArray = gridRscData.getClrbar().replaceAll("\\s+", "")
                .split("!", -1);
        String[] textArray = gridRscData.getText().replaceAll("\\s+", "")
                .split("!", -1);
        String[] colorArray = gridRscData.getColors().replaceAll("\\s+", "")
                .split("!", -1);
        /* Clean up cint -- max 5 zoom level */
        if (cintArray != null && cintArray.length > 0) {
            for (int i = 0; i < cintArray.length; i++) {
                String[] tmp = cintArray[i].split(">");
                if (tmp.length > 5) {
                    cintArray[i] = tmp[0] + ">" + tmp[1] + ">" + tmp[2] + ">"
                            + tmp[3] + ">" + tmp[4];
                }
            }
        }

        for (int i = 0; i < gfuncArray.length; i++) {
            if (gfuncArray[i].contains("//")) {
                String[] tmpstr = gfuncArray[i].split("//", 2);
                gfuncArray[i] = tmpstr[0];
                String referencedAlias = tmpstr[1];
                String referencedFunc = tmpstr[0];
                /*
                 * Need to substitute all occurrences of referencedAlias with
                 * referencedFunc
                 */
                for (int j = i + 1; j < gfuncArray.length; j++) {
                    /*
                     * First need to find out if the gfuncArray[i] is a derived
                     * quantity
                     */
                    gfuncArray[j] = substituteAlias(referencedAlias,
                            referencedFunc, gfuncArray[j]);
                }
            } else {

                /*
                 * Handle blank GDPFUN
                 */
                if (gfuncArray[i].isEmpty()) {
                    if (i > 0) {
                        gfuncArray[i] = gfuncArray[i - 1];
                    }
                }

            }
        }

        contourAttributes = new ContourAttributes[gfuncArray.length];

        for (int i = 0; i < gfuncArray.length; i++) {
            contourAttributes[i] = new ContourAttributes();
            contourAttributes[i].setGdpfun(gfuncArray[i]);

            if (i == 0) {
                contourAttributes[i].setGlevel(glevelArray[0]);
                contourAttributes[i].setGvcord(gvcordArray[0]);
                contourAttributes[i].setSkip(skipArray[0]);
                contourAttributes[i].setFilter(filterArray[0]);
                contourAttributes[i].setScale(scaleArray[0]);
                contourAttributes[i].setType(typeArray[0]);
                contourAttributes[i].setCint(cintArray[0]);
                contourAttributes[i].setLine(lineArray[0]);
                contourAttributes[i].setFint(fintArray[0]);
                contourAttributes[i].setFline(flineArray[0]);
                contourAttributes[i].setHilo(hiloArray[0]);
                contourAttributes[i].setHlsym(hlsymArray[0]);
                contourAttributes[i].setWind(windArray[0]);
                contourAttributes[i].setMarker(markerArray[0]);
                contourAttributes[i].setClrbar(clrbarArray[0]);
                contourAttributes[i].setText(textArray[0]);
                contourAttributes[i].setColors(colorArray[0]);
            } else {
                int idx = (glevelArray.length > i) ? i
                        : (glevelArray.length - 1);
                if (glevelArray[idx].isEmpty() && idx > 0) {
                    glevelArray[idx] = glevelArray[idx - 1];
                }
                contourAttributes[i].setGlevel(glevelArray[idx]);

                idx = (gvcordArray.length > i) ? i : gvcordArray.length - 1;
                if (gvcordArray[idx].isEmpty() && idx > 0) {
                    gvcordArray[idx] = gvcordArray[idx - 1];
                }
                contourAttributes[i].setGvcord(gvcordArray[idx]);

                idx = (skipArray.length > i) ? i : skipArray.length - 1;
                if (skipArray[idx].isEmpty() && idx > 0) {
                    skipArray[idx] = skipArray[idx - 1];
                }
                contourAttributes[i].setSkip(skipArray[idx]);

                idx = (filterArray.length > i) ? i : filterArray.length - 1;
                if (filterArray[idx].isEmpty() && idx > 0) {
                    filterArray[idx] = filterArray[idx - 1];
                }
                contourAttributes[i].setFilter(filterArray[idx]);

                idx = (scaleArray.length > i) ? i : scaleArray.length - 1;
                if (scaleArray[idx].isEmpty() && idx > 0) {
                    scaleArray[idx] = scaleArray[idx - 1];
                }
                contourAttributes[i].setScale(scaleArray[idx]);

                idx = (typeArray.length > i) ? i : typeArray.length - 1;
                if (typeArray[idx].isEmpty() && idx > 0) {
                    typeArray[idx] = typeArray[idx - 1];
                }
                contourAttributes[i].setType(typeArray[idx]);

                idx = (cintArray.length > i) ? i : cintArray.length - 1;
                if (cintArray[idx].isEmpty() && idx > 0) {
                    cintArray[idx] = cintArray[idx - 1];
                }
                contourAttributes[i].setCint(cintArray[idx]);

                idx = (lineArray.length > i) ? i : lineArray.length - 1;
                if (lineArray[idx].isEmpty() && idx > 0) {
                    lineArray[idx] = lineArray[idx - 1];
                }
                contourAttributes[i].setLine(lineArray[idx]);

                idx = (fintArray.length > i) ? i : fintArray.length - 1;
                if (fintArray[idx].isEmpty() && idx > 0) {
                    fintArray[idx] = fintArray[idx - 1];
                }
                contourAttributes[i].setFint(fintArray[idx]);

                idx = (flineArray.length > i) ? i : flineArray.length - 1;
                if (flineArray[idx].isEmpty() && idx > 0) {
                    flineArray[idx] = flineArray[idx - 1];
                }
                contourAttributes[i].setFline(flineArray[idx]);

                idx = (hiloArray.length > i) ? i : hiloArray.length - 1;
                if (hiloArray[idx].isEmpty() && idx > 0) {
                    hiloArray[idx] = hiloArray[idx - 1];
                }
                contourAttributes[i].setHilo(hiloArray[idx]);

                idx = (hlsymArray.length > i) ? i : hlsymArray.length - 1;
                if (hlsymArray[idx].isEmpty() && idx > 0) {
                    hlsymArray[idx] = hlsymArray[idx - 1];
                }
                contourAttributes[i].setHlsym(hlsymArray[idx]);

                idx = (windArray.length > i) ? i : windArray.length - 1;
                if (windArray[idx].isEmpty() && idx > 0) {
                    windArray[idx] = windArray[idx - 1];
                }
                contourAttributes[i].setWind(windArray[idx]);

                idx = (markerArray.length > i) ? i : markerArray.length - 1;
                if (markerArray[idx].isEmpty() && idx > 0) {
                    markerArray[idx] = markerArray[idx - 1];
                }
                contourAttributes[i].setMarker(markerArray[idx]);

                idx = (clrbarArray.length > i) ? i : clrbarArray.length - 1;
                if (clrbarArray[idx].isEmpty() && idx > 0) {
                    clrbarArray[idx] = clrbarArray[idx - 1];
                }
                contourAttributes[i].setClrbar(clrbarArray[idx]);

                idx = (textArray.length > i) ? i : textArray.length - 1;
                if (textArray[idx].isEmpty() && idx > 0) {
                    textArray[idx] = textArray[idx - 1];
                }
                contourAttributes[i].setText(textArray[idx]);

                idx = (colorArray.length > i) ? i : colorArray.length - 1;
                if (colorArray[idx].isEmpty() && idx > 0) {
                    colorArray[idx] = colorArray[idx - 1];
                }
                contourAttributes[i].setColors(colorArray[idx]);

            }
        }
    }

    private void getClippingArea() {

        IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();
        String llLat = prefs.getString(GraphicsAreaPreferences.LLLAT);
        String llLon = prefs.getString(GraphicsAreaPreferences.LLLON);
        String urLat = prefs.getString(GraphicsAreaPreferences.URLAT);
        String urLon = prefs.getString(GraphicsAreaPreferences.URLON);
        if (llLat == null || llLon == null || urLat == null || urLon == null) {
            return;
        }

        if (!llLat.matches(expr) || !llLon.matches(expr)
                || !urLat.matches(expr) || !urLon.matches(expr)) {
            return;
        }

        String garea = llLat + ";" + llLon + ";" + urLat + ";" + urLon;

        GraphicsAreaCoordinates gareaCoordObj = new GraphicsAreaCoordinates(
                garea);
        if (!gareaCoordObj.parseGraphicsAreaString(garea)) {
            return;
        }

        ncgribPreferences = new String(garea);
    }

    private void getNcgridLoggerCfgInfo() {
        IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();
        boolean enableAll = prefs
                .getBoolean(NcgribLoggerPreferences.ENABLE_ALL_LOGGER);

        ncgribLogger = NcgribLogger.getInstance();

        if (enableAll) {
            ncgribLogger.setEnableRscLogs(true);
            ncgribLogger.setEnableDiagnosticLogs(true);
            ncgribLogger.setEnableCntrLogs(true);
            ncgribLogger.setEnableTotalTimeLogs(true);
        } else {
            boolean enableRsc = prefs
                    .getBoolean(NcgribLoggerPreferences.ENABLE_RSC_LOGGER);
            if (enableRsc) {
                ncgribLogger.setEnableRscLogs(true);
            } else {

                ncgribLogger.setEnableRscLogs(false);
            }

            boolean enableDiagnostic = prefs
                    .getBoolean(NcgribLoggerPreferences.ENABLE_DGD_LOGGER);
            if (enableDiagnostic) {
                ncgribLogger.setEnableDiagnosticLogs(true);
            } else {
                ncgribLogger.setEnableDiagnosticLogs(false);
            }

            boolean enableCntr = prefs
                    .getBoolean(NcgribLoggerPreferences.ENABLE_CNTR_LOGGER);
            if (enableCntr) {
                ncgribLogger.setEnableCntrLogs(true);
            } else {
                ncgribLogger.setEnableCntrLogs(false);
            }

            boolean enableTT = prefs
                    .getBoolean(NcgribLoggerPreferences.ENABLE_FINAL_LOGGER);
            if (enableTT) {
                ncgribLogger.setEnableTotalTimeLogs(true);
            } else {
                ncgribLogger.setEnableTotalTimeLogs(false);
            }
        }
    }

    /**
     * Substitutes alias referencedAlias in the String returnedFunc with String
     * referencedFunc.
     * 
     * @param referencedAlias
     * @param referencedFunc
     * @param returnedFunc
     * @return
     */
    private String substituteAlias(String referencedAlias,
            String referencedFunc, String aFunc) {

        String returnedFunc = aFunc;
        /*
         * Process single word functions first
         */
        if (!returnedFunc.contains("(")) {
            if (returnedFunc.trim().equalsIgnoreCase(referencedAlias)) {
                return returnedFunc.replace(referencedAlias, referencedFunc);
            }
            return returnedFunc;
        }

        /*
         * Need to make sure that the number of closing and opening parenthesis
         * is the same.
         */
        int openParenthesisNumber = 0;
        int closeParenthesisNumber = 0;

        for (char c : returnedFunc.toCharArray()) {
            if (c == '(') {
                openParenthesisNumber++;
            } else if (c == ')') {
                closeParenthesisNumber++;
            }
        }

        /*
         * If the some closing parenthesis are missing add them at the end of
         * returnedFunc
         */
        if (openParenthesisNumber != closeParenthesisNumber) {
            int parenthesisDeficit = openParenthesisNumber
                    - closeParenthesisNumber;
            for (int idef = 0; idef < parenthesisDeficit; idef++) {
                returnedFunc = returnedFunc + ")";
            }
        }

        /*
         * Find all the words that make up our returnedFunc
         */
        String delims = "[ (),]+";
        String[] returnedFuncWords = returnedFunc.split(delims);

        /*
         * Go over each returnedFunc word and replaced each referencedAlias with
         * referencedFunc
         */
        for (String component : returnedFuncWords) {
            if (component.equalsIgnoreCase(referencedAlias)) {
                /*
                 * Word that potentially needs de-aliasing found.
                 */
                boolean doneDealiasing = false;
                int startInd = 0;
                while (!doneDealiasing) {
                    int componentBeforePosition = returnedFunc.indexOf(
                            component, startInd) - 1;
                    int componentAfterPosition = componentBeforePosition
                            + component.length() + 1;
                    boolean isRoundedBefore = Character.toString(
                            returnedFunc.charAt(componentBeforePosition))
                            .equalsIgnoreCase("(")
                            || Character.toString(
                                    returnedFunc
                                            .charAt(componentBeforePosition))
                                    .equalsIgnoreCase(")")
                            || Character.toString(
                                    returnedFunc
                                            .charAt(componentBeforePosition))
                                    .equalsIgnoreCase(",");
                    boolean isRroundedAfter = Character.toString(
                            returnedFunc.charAt(componentAfterPosition))
                            .equalsIgnoreCase("(")
                            || Character
                                    .toString(
                                            returnedFunc
                                                    .charAt(componentAfterPosition))
                                    .equalsIgnoreCase(")")
                            || Character
                                    .toString(
                                            returnedFunc
                                                    .charAt(componentAfterPosition))
                                    .equalsIgnoreCase(",");
                    if (isRoundedBefore && isRroundedAfter) {
                        /*
                         * De-alias word since surrounded by '(', ')', or ','
                         */
                        StringBuilder strb = new StringBuilder();
                        int startIndx = componentBeforePosition + 1;
                        int endIndx = startIndx + component.length();
                        int returnedFuncLen = returnedFunc.length();
                        strb.append(returnedFunc, 0, startIndx);
                        strb.append(referencedFunc);
                        strb.append(returnedFunc, endIndx, returnedFuncLen);
                        returnedFunc = strb.toString();
                        doneDealiasing = true;
                    } else {
                        startInd = componentAfterPosition;
                    }
                }
            }
        }

        logger.debug("returnedFunc=" + returnedFunc);

        return returnedFunc;
    }

    /*
     * Generate Title information
     */
    private String generateTitleInfo(String title, DataTime cTime) {
        String titleInfoStr;
        String titleStr = title;
        String shrttlStr = null;

        /*
         * Break title string into title and short title
         */
        int pos = title.indexOf('|');

        if (pos == 0) {
            titleStr = title.substring(1, title.length()).trim();
        } else if (pos > 0 && pos < title.length() - 1) {
            titleStr = title.substring(0, pos).trim();
            shrttlStr = title.substring(pos + 1, title.length()).trim();
        } else if (pos == title.length() - 1) {
            titleStr = title.substring(0, pos).trim();
        }

        String modelname = gridRscData.getResourceName().getRscType();
        if (gridRscData.isEnsemble()) {
            String gdfile = ((NcEnsembleResourceData) gridRscData)
                    .convertGdfileToCycleTimeString(gridRscData.getGdfile(),
                            gridRscData.getResourceName().getCycleTime());
            modelname = modelname + " " + gdfile.toUpperCase();
        }
        titleInfoStr = modelname
                + replaceTitleSpecialCharacters(titleStr, cTime);
        if (shrttlStr != null) {
            titleInfoStr = titleInfoStr + " | "
                    + replaceTitleSpecialCharacters(shrttlStr, cTime);
        }
        return titleInfoStr;
    }

    /*
     * In the grid display programs, special characters will be replaced as
     * follows: ^ Forecast date/time ~ Valid date/time
     * 
     * @ Vertical level _ Grid function $ Nonzero scaling factor # Grid point
     * location ? Day of the week flag
     */
    private String replaceTitleSpecialCharacters(String title, DataTime cTime) {
        StringBuilder titleBuilder = new StringBuilder(title);
        int pos, posV, posF;

        /*
         * check '!' and remove it for now
         */
        if ((pos = titleBuilder.indexOf("!")) > 0) {
            titleBuilder = titleBuilder.delete(pos, titleBuilder.length());
        }
        /*
         * get '~'/'^' position and decide which one to use.
         */
        if (((posF = titleBuilder.indexOf("^")) >= 0)
                | ((posV = titleBuilder.indexOf("~")) >= 0)) {
            Calendar cal;
            String timestampFormat;
            /*
             * The time will be placed where the furthest ~ or ^ is positioned
             */
            pos = (posF > posV ? posF : posV);
            /*
             * The time to use will be which ever one comes latest in the title
             * string. *EXCEPT* if the ~ is the first character in which case
             * the valid time is used.
             */
            if (posF > posV && posV != 0) {
                cal = cTime.getRefTimeAsCalendar();
                timestampFormat = "%02d%02d%02d/%02d%02dF%s";
                /*
                 * Legacy behavior will put the forcast time at the next
                 * position after the ^ when there is both ~ and ^ the ^ is
                 * after the ~ and the ~ is not the first character. We must add
                 * a space at the end if the ^ is the last character in the
                 * title.
                 */
                if (posF >= 0 && posV > 0) {
                    if (++pos == titleBuilder.length()) {
                        titleBuilder.append(" ");
                    }
                }
            } else {
                // Use cycle time (ctime) instead of
                // cal = currFrameTm.getValidTime();
                cal = cTime.getValidTime();
                timestampFormat = "%02d%02d%02d/%02d%02dV%s";
            }
            String forecastTime = CommonDateFormatUtil
                    .getForecastTimeString(cTime.getFcstTime());

            /*
             * check '?' flag for day of week
             */
            if (titleBuilder.indexOf("?") >= 0) {
                deleteWildcard(titleBuilder, "?");
                String dayOfWeek = cal.getDisplayName(Calendar.DAY_OF_WEEK,
                        Calendar.SHORT, Locale.ENGLISH).toUpperCase()
                        + " ";
                titleBuilder.insert(pos, dayOfWeek);
                pos = pos + dayOfWeek.length();
            }
            titleBuilder.insert(pos, String.format(timestampFormat,
                    (cal.get(Calendar.YEAR) % 100),
                    (cal.get(Calendar.MONTH) + 1),
                    cal.get(Calendar.DAY_OF_MONTH),
                    cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE),
                    forecastTime));

            deleteWildcard(titleBuilder, "^");
            deleteWildcard(titleBuilder, "~");
        }
        /*
         * check '@' for Vertical level
         */
        if ((pos = titleBuilder.indexOf("@")) >= 0) {
            deleteWildcard(titleBuilder, "@");
            titleBuilder.insert(pos, gridRscData.getGlevel() + " "
                    + getVerticalLevelUnits(gridRscData.getGvcord()));
        }
        /*
         * check '_' for Grid function
         */
        if ((pos = titleBuilder.indexOf("_")) >= 0) {
            deleteWildcard(titleBuilder, "_");
            titleBuilder.insert(pos, gridRscData.getGdpfun().toUpperCase());
        }
        /*
         * check '$' for Nonzero scaling factor
         */
        if ((pos = titleBuilder.indexOf("$")) >= 0) {
            deleteWildcard(titleBuilder, "$");
            if (gridRscData.getScale().compareTo("0") != 0) {
                titleBuilder.insert(pos, "(*10**" + gridRscData.getScale()
                        + ")");
            }
        }
        /*
         * check '#' for Grid point location
         */
        if ((pos = titleBuilder.indexOf("#")) >= 0) {
            deleteWildcard(titleBuilder, "#");
        }

        return titleBuilder.toString();
    }

    /*
     * Utility method to delete wildcard characters
     */
    private void deleteWildcard(StringBuilder charSeq, String wildcard) {
        int startIndex = charSeq.indexOf(wildcard);
        if (startIndex >= 0) {
            charSeq.delete(startIndex, startIndex + wildcard.length());
        }
    }

    /*
     * Base on GVCORD to get Vertical level Units
     */
    private String getVerticalLevelUnits(String gVCord) {

        String tmp = gVCord.toUpperCase();
        if (tmp.compareTo("PRES") == 0) {
            return "MB";
        }
        if (tmp.compareTo("THTA") == 0) {
            return "K ";
        }
        if (tmp.compareTo("HGHT") == 0) {
            return "M ";
        }
        if (tmp.compareTo("SGMA") == 0) {
            return "SG";
        }
        if (tmp.compareTo("DPTH") == 0) {
            return "M ";
        }
        return "";
    }

    /**
     * 
     * Overridden from the super class to allow use of the the new time matching
     * method BINNING_FOR_GRID_RESOURCES. Removed unnecessary autoupdate code
     * and changed the order of the concentric for-loops to process data
     * appropriately for this new time match method
     * 
     * (non-Javadoc)
     * 
     * @see gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource#
     *      processNewRscDataList()
     */

    @Override
    protected synchronized boolean processNewRscDataList() {
        TimeMatchMethod timeMatchMethod = resourceData.getTimeMatchMethod();

        if (timeMatchMethod == TimeMatchMethod.BINNING_FOR_GRID_RESOURCES) {
            this.processNewRscDataListWithBinning();
        } else {
            super.processNewRscDataList();
        }

        return true;

    }

    protected synchronized boolean processNewRscDataListWithBinning() {

        // allow resources to pre process the data before it is added to the
        // frames
        preProcessFrameUpdate();

        boolean frameMatched = false;
        IRscDataObject lastDataObj = null;
        IRscDataObject rscDataObj = null;
        int closestMatch = 0;

        for (AbstractFrameData frameData : frameDataMap.values()) {
            if (frameData != null) {

                frameMatched = false;

                while (!newRscDataObjsQueue.isEmpty()) {
                    rscDataObj = newRscDataObjsQueue.poll();

                    if (frameData.isRscDataObjInFrame(rscDataObj)) {

                        // Which is a closer match to the frame, the current
                        // IRscDataObject or the one before?
                        closestMatch = frameData.closestToFrame(rscDataObj,
                                lastDataObj);

                        // If current IRscDataObject rscDataObj is closer match
                        if (closestMatch == 1) {
                            addRscDataToFrame(frameData, rscDataObj);
                            lastDataObj = rscDataObj;
                        }
                        // Else last IRscDataObject lastDataObj, closer (2)
                        // OR last & current IRscDataObject are equal (0)
                        // AND BOTH are not null (-1)
                        else if (closestMatch != -1) {
                            addRscDataToFrame(frameData, lastDataObj);
                        }

                        frameMatched = true;
                        break;
                    }

                    lastDataObj = rscDataObj;

                }// end while

                if (!frameMatched) {

                    // Which IRscDataObject is a closer time match ?
                    // the last one processed or the current one?
                    closestMatch = frameData.closestToFrame(rscDataObj,
                            lastDataObj);

                    // Latest IRscDataObject rscDataObj is the closest match
                    if (closestMatch == 1) {
                        addRscDataToFrame(frameData, rscDataObj);
                        lastDataObj = rscDataObj;
                    }
                    // IF the previous IRscDataObject lastDataObj was the
                    // closest match (2)
                    // OR
                    // IF the latest & previous were equally good matches(0)
                    // and the two objects were equal without being both null
                    // (-1)
                    else if (closestMatch != -1) {
                        addRscDataToFrame(frameData, lastDataObj);
                    }

                }

            }
        }// end for

        // allow resources to post-process the data after it is added to the
        // frames
        postProcessFrameUpdate();
        autoUpdateReady = false;
        return true;
    }

    /**
     * 
     * @param userSaveInput
     * @return true if grid saved successfully
     */
    public boolean saveGridAs(SaveGridInput userSaveInput) {

        boolean successfulSave = true;
        if (!userSaveInput.validInput()) {
            statusHandler.handle(UFStatus.Priority.CRITICAL,
                    "Save input not valid: " + userSaveInput);
            return false;
        }

        String gdfile = userSaveInput.getGdfile();
        String modelName = userSaveInput.getModelName();

        // Don't allow saving grid that would overwrite an existing grid
        try {
            if (isDuplicateDatasetId(modelName)) {
                statusHandler.handle(UFStatus.Priority.CRITICAL,
                        "Can not overwrite existing operational grid model "
                                + modelName);
                return false;
            }
        } catch (VizException e) {
            statusHandler.handle(
                    UFStatus.Priority.CRITICAL,
                    "Error querying for duplicate dataset id"
                            + e.getLocalizedMessage(), e);
        }

        Level level = userSaveInput.getLevel();
        Parameter parameter = userSaveInput.getParameter();
        Collection<FrameData> framesToSave = new ArrayList<>();

        if (userSaveInput.isSaveAll()) {

            for (AbstractFrameData frame : frameDataMap.values()) {
                framesToSave.add((FrameData) frame);
            }
        } else {
            framesToSave.add((FrameData) getCurrentFrame());
        }
        for (FrameData frameToSave : framesToSave) {
            DataTime refTime = frameToSave.gdPrxy.getDataTime();

            float[] gridData = frameToSave.getGriddedData(0).getXdataAsArray();
            GridCoverage navigation = (GridCoverage) frameToSave.gdPrxy
                    .getNewSpatialObject();

            try {
                GridRecord grid = new GridRecord();
                int scale = Integer.parseInt(frameToSave.contourRenderable[0]
                        .getContourAttributes().getScale());

                grid.setDatasetId(modelName);
                grid.setMessageData((scale == 0 ? gridData : getUnScaledGrid(
                        gridData, scale)));
                grid.setParameter(parameter);
                grid.setPersistenceTime(new Date());
                grid.setDataTime(refTime);
                grid.setLevel(level);
                grid.setLocation(navigation);
                if (gdfile.startsWith("{") && gdfile.endsWith("}")) {
                    grid.setSecondaryId(gdfile);
                }
                grid.addExtraAttribute("gridid", navigation.getName());

                StoreGridRequest sgr = new StoreGridRequest(grid);
                ThriftClient.sendRequest(sgr);
            } catch (VizException e) {
                statusHandler.handle(UFStatus.Priority.CRITICAL,
                        e.getLocalizedMessage(), e);

                successfulSave = false;
            } catch (ParameterLookupException e) {
                statusHandler.handle(UFStatus.Priority.CRITICAL,
                        e.getLocalizedMessage(), e);
                successfulSave = false;
            }
        }
        return successfulSave;
    }

    /**
     * Checks if datasetid exists in database
     * 
     * @param id
     *            datasetid to check
     * @return true, if it exists
     * @throws VizException
     */
    public boolean isDuplicateDatasetId(String id) throws VizException {

        DbQueryRequest request = new DbQueryRequest();
        HashMap<String, RequestConstraint> constraints = new HashMap<>();
        constraints.put(DATA_SET_ID_TAG, new RequestConstraint(id));
        request.setEntityClass(GridInfoRecord.class);
        request.setConstraints(constraints);
        request.addRequestField(DATA_SET_ID_TAG);
        request.setDistinct(true);
        DbQueryResponse response = (DbQueryResponse) ThriftClient
                .sendRequest(request);
        if (response.getNumResults() > 0) {
            return true;
        }
        return false;
    }

    private float[] getUnScaledGrid(float[] gridData, int scale) {
        float[] unscaledGridData = Arrays.copyOf(gridData, gridData.length);
        for (int i = 0; i < gridData.length; i++) {
            unscaledGridData[i] = new Double(gridData[i] / Math.pow(10, scale))
                    .floatValue();
        }
        return unscaledGridData;
    }

    private void logMissingDataCondition(int i) {
        logMissingDataCondition(i, true);
    }

    private void logMissingDataCondition(int i, boolean addWindInfo) {
        DisplayType displayType = getVectorType(contourAttributes[i].getType());
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getCanonicalName());
        sb.append(":Missing data was expected for:");
        sb.append("GDPFUN:");
        sb.append(contourAttributes[i].getGdpfun());
        sb.append(";GLEVEL:");
        sb.append(contourAttributes[i].getGlevel());
        sb.append(";GVCORD:");
        sb.append(contourAttributes[i].getGvcord());
        sb.append(";displayType:");
        sb.append(displayType);
        if (addWindInfo) {
            sb.append(";WIND:");
            sb.append(contourAttributes[i].getWind());
            sb.append(";");
        }
        logger.info(sb.toString());
    }

}
