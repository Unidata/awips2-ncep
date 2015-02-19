package gov.noaa.nws.ncep.viz.rsc.ncgrid.rsc;

import gov.noaa.nws.ncep.common.log.logger.NcepLogger;
import gov.noaa.nws.ncep.common.log.logger.NcepLoggerManager;
import gov.noaa.nws.ncep.edex.common.dataRecords.NcFloatDataRecord;
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
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceName;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.NcgribLogger;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.NcgribLoggerPreferences;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.FloatBuffer;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
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

import com.raytheon.uf.common.dataplugin.grid.GridRecord;
import com.raytheon.uf.common.dataquery.requests.DbQueryRequest;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint.ConstraintType;
import com.raytheon.uf.common.dataquery.responses.DbQueryResponse;
import com.raytheon.uf.common.datastorage.StorageException;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.geospatial.ISpatialObject;
import com.raytheon.uf.common.geospatial.MapUtil;
import com.raytheon.uf.common.geospatial.ReferencedCoordinate;
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
 * Feb, 2010           		M. Li      		Initial creation
 * Jun, 2010			    M. Li	   		Retrieve grid data from Grid Diagnostic instead of HDF5
 * Oct, 2010     #307       G. Hull    		use NcGridDataProxy to support correct time matching
 * Oct, 2010     #320       X. Guo     		Replace special characters in TITLE parameter
 * Oct, 2010	 #307       m.gamazaychikov	Add handling of DgdrivException in getDataRecord method
 * Oct, 2010                X. Guo          Rename getCycleTimeStringFromDataTime to getTimeStringFromDataTime
 * Oct, 2010	 #277		M. Li			Parsed first model name from ensemble model list
 * Nov, 2010			    M. Li			modified for new vector algorithm
 * 11/29/2010				mgamazaychikov	Updated queryRecords and updateFrameData
 * 12/06/2010    #363       X. Guo          Plot relative minima and maxima gridded data
 * 12/19/2010     365       Greg Hull       Replace dataSource with pluginName
 * 01/03/11				    M. Li			Add hilo and hlsysm to contourAttributes
 * 01/07/11				    M. Li			Use Vector array
 * 03/08/11					M. Li			refactor ContourRenderable
 * 04/29/11				    M. Li			move gridIndiceDisplay to a separate class
 * 06/2011					mgamazaychikov	Add spatialObject to the Dgdriv fields.
 * 07/2011					mgamazaychikov	Add substituteAlias method.
 * 08/2011					mgamazaychikov	Add dispose method to FrameData class;
 * 											change disposeInternal method of NcgridResource class;
 * 											change aDgdriv from the NcgridResource class variable to local variable.
 * 09/2011					mgamazaychikov	Made changes associated with removal of DatatypeTable class
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
 * 02/4/2014	#936		T. Lee			Implemented textSize for point values
 * 04/11/2014   #981        D.Sushon        Added fault tolerance for when some data is bad to display the good data rather than fall-through entire frame
 * 04/14/2014               S.Gilbert       Cleaned up old unused methods
 * 04/22/2014   #1129       B. Hebbard      Feed HILO point count limits to GridRelativeHiLoDisplay constructor instead of HILORelativeMinAndMaxLocator, so can apply dynamically based on current extent
 * 06/27/2014   ?           B. Yin          Handle grid analysis (cycle time is null).
 * 08/01/2014   ?           B. Yin          Handle display type D (directional arrow).
 * </pre>
 * 
 * @author mli
 * @version 1.0
 */
public class NcgridResource extends
        AbstractNatlCntrsResource<NcgridResourceData, NCMapDescriptor>
        implements INatlCntrsResource {

    private static NcepLogger logger = NcepLoggerManager
            .getNcepLogger(NcgridResource.class);

    private static SimpleDateFormat QUERY_DATE_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss");

    private static final DecimalFormat forecastHourFormat = new DecimalFormat(
            "000");

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

    // private boolean enableSystemOut = false;

    private ArrayList<DataTime> dataTimesForDgdriv = new ArrayList<DataTime>();

    // private MathTransform trans;

    // private SymbolLocationSet gridPointMarkerSet = null;

    // private RGB gridIndiceColor = new RGB(255, 255, 0);

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
    private class NcGridDataProxy implements IRscDataObject {

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

            boolean isFirst = true;
            long t1 = System.currentTimeMillis();
            if (ncgribLogger.enableRscLogs())
                logger.info("==from init to run loadNcgridData took: "
                        + (t1 - initTime));
            for (AbstractFrameData fd : frameDataMap.values()) {
                FrameData frameData = (FrameData) fd;

                for (DataTime dt : dataTimesForDgdriv) {

                    IRscDataObject[] dataObject = processRecord(dt);
                    if (frameData.isRscDataObjInFrame(dataObject[0])) {
                        newRscDataObjsQueue.add(dataObject[0]);
                        break;
                    }
                }

                if (cancel) {
                    return Status.CANCEL_STATUS;
                }

                //
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
                            //
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
    }

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

    protected class FrameData extends AbstractFrameData {
        ContourRenderable[] contourRenderable;

        NcGridDataProxy gdPrxy = null;

        GriddedVectorDisplay[] vectorDisplay;

        // GridRelativeHiLoDisplay gridRelativeHiLoDisplay;
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

        //
        public boolean updateFrameData(IRscDataObject rscDataObj) {
            if (!(rscDataObj instanceof NcGridDataProxy)) {
                System.out
                        .println("Unexpected rscDataObject type in NcGridResource:updateFrameData:"
                                + rscDataObj.getClass().getName());
                logger.error("Unexpected rscDataObject type in NcGridResource:updateFrameData:"
                        + rscDataObj.getClass().getName());
                return false;
            }
            // synchronized (AbstractFrameData.class) {
            /*
             * set navigation
             */
            gdPrxy = (NcGridDataProxy) rscDataObj;

            long st = System.currentTimeMillis();
            if (ncgribLogger.enableRscLogs()) {
                logger.info("From init resource to updated frame("
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
                    // TODO Auto-generated catch block
                    System.out
                            .println("Error retrieving GEMPAK grid navigation block: "
                                    + e.getMessage());
                    logger.error(
                            "Error retrieving GEMPAK grid navigation block: "
                                    + e.getMessage(), e);
                    // before TTR981, was: return false;
                }
            } else {

                HashMap<String, RequestConstraint> queryList = new HashMap<String, RequestConstraint>(
                        gridRscData.getMetadataMap());

                if (gridRscData.getGdfile().startsWith("{")
                        && gridRscData.getGdfile().endsWith("}")) {

                    ModelListInfo modelListInfo = new ModelListInfo(
                            gridRscData.getGdfile());
                    String modelName = modelListInfo.getModelList().get(0)
                            .getModelName();
                    String perturbationNum = null;
                    // String eventName = null;

                    if (modelName.contains(":")) {
                        String[] gdfileArrs = modelName.split(":");
                        modelName = gdfileArrs[0];
                        if (gdfileArrs[0].contains("%")) {
                            modelName = gdfileArrs[0].split("%")[1];
                        }
                        // eventName = gdfileArrs[1].toLowerCase();

                        if (isIntNum(gdfileArrs[1])) {
                            perturbationNum = String.valueOf(Integer
                                    .parseInt(gdfileArrs[1]));
                        }
                    } else {
                        if (modelName.contains("%")) {
                            modelName = modelName.split("%")[1];
                        }
                    }
                    queryList.put(GridDBConstants.MODEL_NAME_QUERY,
                            new RequestConstraint(modelName,
                                    ConstraintType.EQUALS));
                    // if ( eventName != null ){
                    // queryList.put("eventName",
                    // new RequestConstraint( eventName, ConstraintType.EQUALS )
                    // );
                    // }
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
                        responseList = new ArrayList<Map<String, Object>>(0);
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
                    // ISpatialObject cov = ((NcgribRecord)
                    // pdoList[0]).getSpatialObject();
                    if (cov != null) {
                        if (ncgribLogger.enableRscLogs()) {
                            logger.info("retrieving grid navigation("
                                    + cov.toString() + ") : "
                                    + gdPrxy.getDataTime().toString()
                                    + " took:" + (t4 - t1));
                        }
                        gdPrxy.setSpatialObject(cov);
                        gdPrxy.setNewSpatialObject(cov);
                    } else {
                        if (ncgribLogger.enableRscLogs()) {
                            logger.info("Error retrieving ncgrid navigation for "
                                    + gdPrxy.getDataTime().toString()
                                    + " took:" + (t4 - t1));
                        }
                        // before TTR981, was: return false;
                    }
                } catch (VizException e) {
                    System.out
                            .println("Error retrieving ncgrid record for the spatial object: "
                                    + e.getMessage());
                    logger.error(
                            "Error retrieving ncgrid record for the spatial object: "
                                    + e.getMessage(), e);
                    // before TTR981, was: return false;
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
            // System.out.println("contourAttributes.length==="+contourAttributes.length);
            NcFloatDataRecord gridData = null;
            long t11, t12;
            for (int i = 0; i < contourAttributes.length; i++) {

                DisplayType displayType = getVectorType(contourAttributes[i]
                        .getType());
                String attrType = contourAttributes[i].getType().toUpperCase();

                if (attrType.contains("M") && gridPointMarkerDisplay == null) {
                    try { // gdPrxy may not have valid spatial objects
                        gridPointMarkerDisplay = new GridPointMarkerDisplay(
                                contourAttributes[i].getMarker(),
                                getNcMapDescriptor(),
                                gdPrxy.getNewSpatialObject());
                    } catch (NullPointerException npe) {
                        if (ncgribLogger.enableDiagnosticLogs()) {
                            logger.info(this.getClass().getCanonicalName()
                                    + ":Missing data was expected for:"
                                    + "GDPFUN:"
                                    + contourAttributes[i].getGdpfun()
                                    + ";GLEVEL:"
                                    + contourAttributes[i].getGlevel()
                                    + ";GVCORD:"
                                    + contourAttributes[i].getGvcord() + ";"
                                    + "displayType:" + displayType + ";WIND:"
                                    + contourAttributes[i].getWind() + ";");
                        }
                        logger.debug(
                                this.getClass().getCanonicalName()
                                        + ":\n"
                                        + "Probable offending statement: gridPointMarkerDisplay = new"
                                        + " GridPointMarkerDisplay(contourAttributes[i].getMarker(),getNcMapDescriptor(), gdPrxy.getNewSpatialObject());\n"
                                        + "***HINT: parts of gdPrxy may have been null\n"
                                        + npe.getMessage(), npe);
                    }
                }

                if (attrType.contains("G") && gridIndicesDisplay == null) {
                    try { // gdPrxy may not have valid spatial objects
                        gridIndicesDisplay = new GridIndicesDisplay(
                                GempakColor.convertToRGB(gridRscData
                                        .getGrdlbl()), getNcMapDescriptor(),
                                gdPrxy.getNewSpatialObject());
                    } catch (NullPointerException npe) {
                        if (ncgribLogger.enableDiagnosticLogs()) {
                            logger.info(this.getClass().getCanonicalName()
                                    + ":Missing data was expected for:"
                                    + "GDPFUN:"
                                    + contourAttributes[i].getGdpfun()
                                    + ";GLEVEL:"
                                    + contourAttributes[i].getGlevel()
                                    + ";GVCORD:"
                                    + contourAttributes[i].getGvcord() + ";"
                                    + "displayType:" + displayType + ";WIND:"
                                    + contourAttributes[i].getWind() + ";");
                        }
                        logger.debug(
                                this.getClass().getCanonicalName()
                                        + ":\n"
                                        + "Probable offending statement: gridIndicesDisplay = new"
                                        + " GridIndicesDisplay(GempakColor.convertToRGB(gridRscData.getGrdlbl()),"
                                        + " getNcMapDescriptor(),gdPrxy.getNewSpatialObject());\n"
                                        + "***HINT: parts of gdPrxy may have been null\n"
                                        + npe.getMessage(), npe);
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
                        NcFloatDataRecord rec = null;
                        rec = findGriddedDataFromVector(i);

                        // Avoid duplicate data retrieval
                        if (rec == null) {
                            rec = getGriddedData(i);
                        }
                        if (rec != null) {
                            try { // gdPrxy may not have valid spatial objects
                                vectorDisplay[i] = new GriddedVectorDisplay(
                                        rec, displayType, isDirectionalArrow,
                                        getNcMapDescriptor(),
                                        gdPrxy.getNewSpatialObject(),
                                        contourAttributes[i]);
                                hasData = true;
                            } catch (NullPointerException npe) {
                                hasData = false;
                                if (ncgribLogger.enableDiagnosticLogs()) {
                                    logger.info(this.getClass()
                                            .getCanonicalName()
                                            + ":Missing data was expected for:"
                                            + "GDPFUN:"
                                            + contourAttributes[i].getGdpfun()
                                            + ";GLEVEL:"
                                            + contourAttributes[i].getGlevel()
                                            + ";GVCORD:"
                                            + contourAttributes[i].getGvcord()
                                            + ";"
                                            + "displayType:"
                                            + displayType
                                            + ";WIND:"
                                            + contourAttributes[i].getWind()
                                            + ";");
                                }
                                logger.debug(
                                        this.getClass().getCanonicalName()
                                                + ":\n"
                                                + "Probable offending statement: vectorDisplay[i] = new GriddedVectorDisplay(rec,displayType,"
                                                + " isDirectionalArrow,getNcMapDescriptor(),gdPrxy.getNewSpatialObject(),contourAttributes[i]);\n"
                                                + "***HINT: parts of gdPrxy may have been null\n"
                                                + npe.getMessage(), npe);
                            }
                        } else {
                            if (ncgribLogger.enableDiagnosticLogs()) {
                                logger.info(this.getClass().getCanonicalName()
                                        + ":Missing data was expected for:"
                                        + "GDPFUN:"
                                        + contourAttributes[i].getGdpfun()
                                        + ";GLEVEL:"
                                        + contourAttributes[i].getGlevel()
                                        + ";GVCORD:"
                                        + contourAttributes[i].getGvcord()
                                        + ";" + "displayType:" + displayType
                                        + ";WIND:"
                                        + contourAttributes[i].getWind() + ";");
                            }
                            logger.debug(this.getClass().getCanonicalName()
                                    + ":\n"
                                    + "***Something may be wrong: griddedData was null, was populated, but was found to still be null? (1)\n"
                                    + " For debug, possibly useful parameters used in the load attempt follow:\n"
                                    + "displayType:" + displayType
                                    + ";\nGLEVEL:"
                                    + contourAttributes[i].getGlevel()
                                    + ";\nGVCORD:"
                                    + contourAttributes[i].getGvcord()
                                    + ";\nGDPFUN:"
                                    + contourAttributes[i].getGdpfun()
                                    + ";\nWIND:"
                                    + contourAttributes[i].getWind() + ";");
                            // before TTR981, was: return false;
                        }
                    } else {
                        gridData = vectorDisplay[i].getData();
                        if (gridData != null) {
                            hasData = true;
                            if (vectorDisplay[i].checkAttrsChanged(displayType,
                                    isDirectionalArrow,
                                    contourAttributes[i].getWind())) {
                                vectorDisplay[i].dispose();
                                try { // gdPrxy may not have valid spatial
                                      // objects
                                    vectorDisplay[i] = new GriddedVectorDisplay(
                                            gridData, displayType,
                                            isDirectionalArrow,
                                            getNcMapDescriptor(),
                                            gdPrxy.getNewSpatialObject(),
                                            contourAttributes[i]);
                                } catch (NullPointerException npe) {
                                    hasData = false;
                                    if (ncgribLogger.enableDiagnosticLogs()) {
                                        logger.info(this.getClass()
                                                .getCanonicalName()
                                                + ":Missing data was expected for:"
                                                + "GDPFUN:"
                                                + contourAttributes[i]
                                                        .getGdpfun()
                                                + ";GLEVEL:"
                                                + contourAttributes[i]
                                                        .getGlevel()
                                                + ";GVCORD:"
                                                + contourAttributes[i]
                                                        .getGvcord()
                                                + ";"
                                                + "displayType:"
                                                + displayType
                                                + ";WIND:"
                                                + contourAttributes[i]
                                                        .getWind() + ";");
                                    }
                                    logger.debug(
                                            this.getClass().getCanonicalName()
                                                    + ":\n"
                                                    + "Probable offending statement: vectorDisplay[i] = new GriddedVectorDisplay(gridData,"
                                                    + " displayType,isDirectionalArrow,getNcMapDescriptor(),gdPrxy.getNewSpatialObject(),contourAttributes[i]);\n"
                                                    + "***HINT: parts of gdPrxy may have been null\n"
                                                    + npe.getMessage(), npe);
                                }
                            }
                        } else {
                            if (ncgribLogger.enableDiagnosticLogs()) {
                                logger.info(this.getClass().getCanonicalName()
                                        + ":Missing data was expected for:"
                                        + "GDPFUN:"
                                        + contourAttributes[i].getGdpfun()
                                        + ";GLEVEL:"
                                        + contourAttributes[i].getGlevel()
                                        + ";GVCORD:"
                                        + contourAttributes[i].getGvcord()
                                        + ";" + "displayType:" + displayType
                                        + ";WIND:"
                                        + contourAttributes[i].getWind() + ";");
                            }
                            logger.debug(this.getClass().getCanonicalName()
                                    + ":\n"
                                    + "***Something may be wrong: gridData was null, was populated, but was found to still be null? (2)\n"
                                    + " For debug, possibly useful parameters used in the load attempt follow:\n"
                                    + "displayType:" + displayType
                                    + ";\nGLEVEL:"
                                    + contourAttributes[i].getGlevel()
                                    + ";\nGVCORD:"
                                    + contourAttributes[i].getGvcord()
                                    + ";\nGDPFUN:"
                                    + contourAttributes[i].getGdpfun()
                                    + ";\nWIND:"
                                    + contourAttributes[i].getWind() + ";");
                            // before TTR981, was: return false;
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
                    try { // gdPrxy may not have been fully assembled upstream
                        contourName = createContourName(gdPrxy,
                                contourAttributes[i]);
                    } catch (NullPointerException npe) {
                        if (ncgribLogger.enableDiagnosticLogs()) {
                            logger.info(this.getClass().getCanonicalName()
                                    + ":Missing data was expected for:"
                                    + "GDPFUN:"
                                    + contourAttributes[i].getGdpfun()
                                    + ";GLEVEL:"
                                    + contourAttributes[i].getGlevel()
                                    + ";GVCORD:"
                                    + contourAttributes[i].getGvcord() + ";"
                                    + "displayType:" + displayType + ";WIND:"
                                    + contourAttributes[i].getWind() + ";");
                        }
                        logger.debug(
                                this.getClass().getCanonicalName()
                                        + ":\n"
                                        + "Probable offending statement: contourName = createContourName(gdPrxy,contourAttributes[i]);\n"
                                        + "***HINT: parts of gdPrxy may have been null\n"
                                        + npe.getMessage(), npe);
                    }

                    // New creation
                    if (contourRenderable[i] == null) {
                        // IDataRecord gridData = null;
                        // Duplicate data
                        gridData = findGriddedDataFromContourRenderable(i);

                        // Avoid duplicate data retrieval
                        if (gridData == null) {
                            gridData = getGriddedData(i);
                        }

                        if (gridData != null) {
                            try { // gdPrxy may not have valid spatial objects
                                contourRenderable[i] = new ContourRenderable(
                                        (IDataRecord) gridData,
                                        getNcMapDescriptor(),
                                        MapUtil.getGridGeometry(gdPrxy
                                                .getNewSpatialObject()),
                                        contourAttributes[i], contourName);
                                hasData = true;
                            } catch (Exception npe) {
                                hasData = false;
                                if (ncgribLogger.enableDiagnosticLogs()) {
                                    logger.info(this.getClass()
                                            .getCanonicalName()
                                            + ":Missing data was expected for:"
                                            + "GDPFUN:"
                                            + contourAttributes[i].getGdpfun()
                                            + ";GLEVEL:"
                                            + contourAttributes[i].getGlevel()
                                            + ";GVCORD:"
                                            + contourAttributes[i].getGvcord()
                                            + ";"
                                            + "displayType:"
                                            + displayType
                                            + ";WIND:"
                                            + contourAttributes[i].getWind()
                                            + ";");
                                }
                                logger.debug(
                                        this.getClass().getCanonicalName()
                                                + ":\n"
                                                + "Probable offending statement: contourRenderable[i] = new ContourRenderable((IDataRecord)"
                                                + " gridData,getNcMapDescriptor(),MapUtil.getGridGeometry(gdPrxy.getNewSpatialObject()),contourAttributes[i],"
                                                + " contourName);\n"
                                                + "***HINT: parts of gdPrxy may have been null\n"
                                                + npe.getMessage(), npe);
                            }

                        } else {
                            if (ncgribLogger.enableDiagnosticLogs()) {
                                logger.info(this.getClass().getCanonicalName()
                                        + ":Missing data was expected for:"
                                        + "contourName:" + contourName
                                        + ";GDPFUN:"
                                        + contourAttributes[i].getGdpfun()
                                        + ";GLEVEL:"
                                        + contourAttributes[i].getGlevel()
                                        + ";GVCORD:"
                                        + contourAttributes[i].getGvcord()
                                        + ";");
                            }
                            logger.debug(this.getClass().getCanonicalName()
                                    + ":\n"
                                    + "***Something may be wrong: gridData was null, was populated, but was found to still be null? (3)\n"
                                    + " For debug, possibly useful parameters used in the load attempt follow:\n"
                                    + "contourName:" + contourName
                                    + ";\nGLEVEL:"
                                    + contourAttributes[i].getGlevel()
                                    + ";\nGVCORD:"
                                    + contourAttributes[i].getGvcord()
                                    + ";\nGDPFUN:"
                                    + contourAttributes[i].getGdpfun() + ";");

                            // return false;
                        }
                    }
                    // Attributes or navigation change
                    else {
                        // gridData =
                        // (NcFloatDataRecord)contourRenderable[i].getData();
                        // contourRenderable[i].dispose();
                        // contourRenderable[i] = new
                        // ContourRenderable((IDataRecord)gridData, descriptor,
                        // MapUtil.getGridGeometry(gdPrxy.getSpatialObject()),
                        // contourAttributes[i], contourName);
                        contourRenderable[i]
                                .setContourAttributes(contourAttributes[i]);
                    }

                    // Grid point value
                    if (attrType.contains("P")
                            && contourRenderable[i] != null
                            && contourRenderable[i].getData() instanceof NcFloatDataRecord) {
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

                            /*
                             * System.out
                             * .println(" In Line#: 747 updateFrameData... " +
                             * " Old color ==>  " + colors + " New colors ===> "
                             * + contourAttributes[i].getColors() + " ");
                             * 
                             * System.out
                             * .println(" In Line#: 755 updateFrameData... " +
                             * " Old levels ==>  " + glevel + " New leves ===> "
                             * + contourAttributes[i].getGlevel() + " ");
                             * 
                             * System.out
                             * .println(" In Line#: 763 updateFrameData... " +
                             * " Old gpfunc ==>  " + gfunc + " New gpfunc ===> "
                             * + contourAttributes[i].getGdpfun() +
                             * " contourRenderable #: " + i);
                             */
                            try { // gdPrxy may not have been fully assembled
                                  // upstream
                                gridPointValueDisplay[i] = createGridPointValueDisplay(
                                        (NcFloatDataRecord) contourRenderable[i]
                                                .getData(), gdPrxy,
                                        contourAttributes[i]);

                                gfunc = contourAttributes[i].getGdpfun();
                                glevel = contourAttributes[i].getGlevel();
                                gvcord = contourAttributes[i].getGvcord();
                                skip = contourAttributes[i].getSkip();
                                filter = contourAttributes[i].getFilter();
                                scale = contourAttributes[i].getScale();
                                colors = contourAttributes[i].getColors();
                                text = contourAttributes[i].getText();
                            } catch (NullPointerException npe) {
                                if (ncgribLogger.enableDiagnosticLogs()) {
                                    logger.info(this.getClass()
                                            .getCanonicalName()
                                            + ":Missing data was expected for:"
                                            + "GDPFUN:"
                                            + contourAttributes[i].getGdpfun()
                                            + ";GLEVEL:"
                                            + contourAttributes[i].getGlevel()
                                            + ";GVCORD:"
                                            + contourAttributes[i].getGvcord()
                                            + ";"
                                            + "displayType:"
                                            + displayType
                                            + ";WIND:"
                                            + contourAttributes[i].getWind()
                                            + ";");
                                }
                                logger.debug(
                                        this.getClass().getCanonicalName()
                                                + ":\n"
                                                + "Probable offending statement: gridPointValueDisplay[i] = createGridPointValueDisplay((NcFloatDataRecord)"
                                                + " contourRenderable[i].getData(),gdPrxy, contourAttributes[i]);\n"
                                                + "***HINT: parts of gdPrxy may have been null\n"
                                                + npe.getMessage(), npe);
                            }
                        }
                    }

                    // create HILO symbols
                    if ((attrType.contains("F") || attrType.contains("C"))
                            && (contourRenderable[i] != null)
                            && (contourRenderable[i].getData() instanceof NcFloatDataRecord)) {
                        if (contourRenderable[i].getGridRelativeHiLo() == null) {
                            try { // gdPrxy may not have been fully assembled
                                  // upstream
                                contourRenderable[i]
                                        .setGridRelativeHiLo(createGridRelativeHiLoDisplay(
                                                (NcFloatDataRecord) contourRenderable[i]
                                                        .getData(), gdPrxy,
                                                contourAttributes[i]));
                            } catch (NullPointerException npe) {
                                if (ncgribLogger.enableDiagnosticLogs()) {
                                    logger.info(this.getClass()
                                            .getCanonicalName()
                                            + ":Missing data was expected for:"
                                            + "GDPFUN:"
                                            + contourAttributes[i].getGdpfun()
                                            + ";GLEVEL:"
                                            + contourAttributes[i].getGlevel()
                                            + ";GVCORD:"
                                            + contourAttributes[i].getGvcord()
                                            + ";"
                                            + "displayType:"
                                            + displayType
                                            + ";WIND:"
                                            + contourAttributes[i].getWind()
                                            + ";");
                                }
                                logger.debug(
                                        this.getClass().getCanonicalName()
                                                + ":\n"
                                                + "Offending statement: contourRenderable[i].setGridRelativeHiLo(createGridRelativeHiLoDisplay((NcFloatDataRecord)"
                                                + " contourRenderable[i].getData(), gdPrxy,contourAttributes[i]));\n"
                                                + "***HINT: parts of gdPrxy may have been null\n"
                                                + npe.getMessage(), npe);
                            }
                        }
                    }
                    t12 = System.currentTimeMillis();
                    logger.debug("==init contour took:" + (t12 - t11));
                }

            } // end of for loop

            Collections.sort(NcgridResource.this.dataTimes);

            frameLoaded = true;
            // while (getGraphicsTarget() == null || getPaintProperties() ==
            // null ) {
            // try {
            // Thread.sleep(5);
            // } catch (InterruptedException e) {
            //
            // }
            // }
            long t1 = System.currentTimeMillis();
            if (ncgribLogger.enableRscLogs()) {
                logger.info("*updateFrameData("
                        + ((gdPrxy != null) ? gdPrxy.getDataTime().toString()
                                : " ") + "): completed diagnostic took: "
                        + (t1 - st));
            }
            logger.debug("updateFrameData: from init resource to complete diagnostic took: "
                    + (t1 - initTime));
            if (getGraphicsTarget() != null && getPaintProperties() != null) {
                generateContours(gdPrxy.getDataTime().toString());
            }
            // }
            cacheData.clear();
            return true;
        }

        public void procAttrsModified() {

            synchronized (this) {
                NcFloatDataRecord gridData = null;
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
                        try { // gdPrxy may not have valid spatial objects
                            gridPointMarkerDisplay = new GridPointMarkerDisplay(
                                    contourAttributes[i].getMarker(),
                                    getNcMapDescriptor(),
                                    gdPrxy.getNewSpatialObject());
                        } catch (NullPointerException npe) {
                            if (ncgribLogger.enableDiagnosticLogs()) {
                                logger.info(this.getClass().getCanonicalName()
                                        + ":Missing data was expected for:"
                                        + "GDPFUN:"
                                        + contourAttributes[i].getGdpfun()
                                        + ";GLEVEL:"
                                        + contourAttributes[i].getGlevel()
                                        + ";GVCORD:"
                                        + contourAttributes[i].getGvcord()
                                        + ";" + "displayType:" + displayType
                                        + ";WIND:"
                                        + contourAttributes[i].getWind() + ";");
                            }
                            logger.debug(
                                    this.getClass().getCanonicalName()
                                            + ":\n"
                                            + "Offending statement: gridPointMarkerDisplay = new"
                                            + " GridPointMarkerDisplay(contourAttributes[i].getMarker(),getNcMapDescriptor(),gdPrxy.getNewSpatialObject());\n"
                                            + "***HINT: parts of gdPrxy may have been null\n"
                                            + npe.getMessage(), npe);
                        }
                    } else if (!attrType.contains("M")
                            && gridPointMarkerDisplay != null) {
                        gridPointMarkerDisplay = null;
                    }

                    if (attrType.contains("G") && gridIndicesDisplay == null) {
                        try { // gdPrxy may not have valid spatial objects
                            gridIndicesDisplay = new GridIndicesDisplay(
                                    GempakColor.convertToRGB(gridRscData
                                            .getGrdlbl()),
                                    getNcMapDescriptor(),
                                    gdPrxy.getNewSpatialObject());
                        } catch (NullPointerException npe) {
                            if (ncgribLogger.enableDiagnosticLogs()) {
                                logger.info(this.getClass().getCanonicalName()
                                        + ":Missing data was expected for:"
                                        + "GDPFUN:"
                                        + contourAttributes[i].getGdpfun()
                                        + ";GLEVEL:"
                                        + contourAttributes[i].getGlevel()
                                        + ";GVCORD:"
                                        + contourAttributes[i].getGvcord()
                                        + ";" + "displayType:" + displayType
                                        + ";WIND:"
                                        + contourAttributes[i].getWind() + ";");
                            }
                            logger.debug(
                                    this.getClass().getCanonicalName()
                                            + ":\n"
                                            + "Offending statement: gridIndicesDisplay = new GridIndicesDisplay(GempakColor.convertToRGB(gridRscData.getGrdlbl()),"
                                            + " getNcMapDescriptor(),gdPrxy.getNewSpatialObject());\n"
                                            + "***HINT: parts of gdPrxy may have been null\n"
                                            + npe.getMessage(), npe);
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
                                    try { // gdPrxy may not have valid spatial
                                          // objects
                                        vectorDisplay[i] = new GriddedVectorDisplay(
                                                gridData, displayType,
                                                isDirectionalArrow,
                                                getNcMapDescriptor(),
                                                gdPrxy.getNewSpatialObject(),
                                                contourAttributes[i]);
                                    } catch (NullPointerException npe) {
                                        if (ncgribLogger.enableDiagnosticLogs()) {
                                            logger.info(this.getClass()
                                                    .getCanonicalName()
                                                    + ":Missing data was expected for:"
                                                    + "GDPFUN:"
                                                    + contourAttributes[i]
                                                            .getGdpfun()
                                                    + ";GLEVEL:"
                                                    + contourAttributes[i]
                                                            .getGlevel()
                                                    + ";GVCORD:"
                                                    + contourAttributes[i]
                                                            .getGvcord()
                                                    + ";"
                                                    + "displayType:"
                                                    + displayType
                                                    + ";WIND:"
                                                    + contourAttributes[i]
                                                            .getWind() + ";");
                                        }
                                        logger.debug(
                                                this.getClass()
                                                        .getCanonicalName()
                                                        + ":\n"
                                                        + "Offending statement: vectorDisplay[i] = new GriddedVectorDisplay(gridData,"
                                                        + " displayType, isDirectionalArrow, getNcMapDescriptor(), gdPrxy.getNewSpatialObject(),"
                                                        + " contourAttributes[i]);\n"
                                                        + "***HINT: parts of gdPrxy may have been null\n"
                                                        + npe.getMessage(), npe);
                                    }
                                }
                            }
                        }
                        /*
                         * else { NcFloatDataRecord rec = null; rec =
                         * findGriddedDataFromVector ( i ); // Avoid duplicate
                         * data retrieval if ( rec == null ) { rec =
                         * getGriddedData ( i ); }
                         * 
                         * // Avoid duplicate data retrieval if (rec == null) {
                         * rec = getGriddedData ( i ); } if ( rec != null) {
                         * vectorDisplay[i] = new GriddedVectorDisplay(rec,
                         * displayType, isDirectionalArrow, descriptor,
                         * gdPrxy.getNewSpatialObject(),contourAttributes[i]); }
                         * }
                         */
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
                        /*
                         * else { String contourName = createContourName(gdPrxy,
                         * contourAttributes[i]); gridData =
                         * findGriddedDataFromContourRenderable ( i );
                         * 
                         * // Avoid duplicate data retrieval if (gridData ==
                         * null) { gridData = getGriddedData ( i ); }
                         * 
                         * if ( gridData != null ) { contourRenderable[i] = new
                         * ContourRenderable((IDataRecord)gridData, descriptor,
                         * MapUtil
                         * .getGridGeometry(gdPrxy.getNewSpatialObject()),
                         * contourAttributes[i], contourName); } }
                         */

                        // Grid point value
                        if (attrType.contains("P")
                                && contourRenderable[i] != null
                                && contourRenderable[i].getData() instanceof NcFloatDataRecord) {
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

                                try { // gdPrxy may not have been fully
                                      // assembled upstream
                                    gridPointValueDisplay[i] = createGridPointValueDisplay(
                                            (NcFloatDataRecord) contourRenderable[i]
                                                    .getData(), gdPrxy,
                                            contourAttributes[i]);

                                    gfunc = contourAttributes[i].getGdpfun();
                                    glevel = contourAttributes[i].getGlevel();
                                    gvcord = contourAttributes[i].getGvcord();
                                    skip = contourAttributes[i].getSkip();
                                    filter = contourAttributes[i].getFilter();
                                    scale = contourAttributes[i].getScale();
                                    colors = contourAttributes[i].getColors();
                                    text = contourAttributes[i].getText();
                                } catch (NullPointerException npe) {
                                    if (ncgribLogger.enableDiagnosticLogs()) {
                                        logger.info(this.getClass()
                                                .getCanonicalName()
                                                + ":Missing data was expected for:"
                                                + "GDPFUN:"
                                                + contourAttributes[i]
                                                        .getGdpfun()
                                                + ";GLEVEL:"
                                                + contourAttributes[i]
                                                        .getGlevel()
                                                + ";GVCORD:"
                                                + contourAttributes[i]
                                                        .getGvcord()
                                                + ";"
                                                + "displayType:"
                                                + displayType
                                                + ";WIND:"
                                                + contourAttributes[i]
                                                        .getWind() + ";");
                                    }
                                    logger.debug(
                                            this.getClass().getCanonicalName()
                                                    + ":\n"
                                                    + "Offending statement: gridPointValueDisplay[i] = createGridPointValueDisplay((NcFloatDataRecord)"
                                                    + " contourRenderable[i].getData(), gdPrxy, contourAttributes[i]);\n"
                                                    + "***HINT: parts of gdPrxy may have been null\n"
                                                    + npe.getMessage(), npe);
                                }
                            }

                            /*
                             * These lines of code result in
                             * NullPointerException when editing from the
                             * legend.
                             */
                            // } else if (!attrType.contains("P")
                            // && gridPointValueDisplay != null) {
                            // gridPointValueDisplay = null;
                        }

                        // create HILO symbols
                        if ((attrType.contains("F") || attrType.contains("C"))
                                && (contourRenderable[i] != null)
                                && (contourRenderable[i].getData() instanceof NcFloatDataRecord)) {

                            if (contourRenderable[i].getGridRelativeHiLo() == null) {
                                try { // gdPrxy may not have been fully
                                      // assembled upstream
                                    contourRenderable[i]
                                            .setGridRelativeHiLo(createGridRelativeHiLoDisplay(
                                                    (NcFloatDataRecord) contourRenderable[i]
                                                            .getData(), gdPrxy,
                                                    contourAttributes[i]));
                                } catch (NullPointerException npe) {
                                    if (ncgribLogger.enableDiagnosticLogs()) {
                                        logger.info(this.getClass()
                                                .getCanonicalName()
                                                + ":Missing data was expected for:"
                                                + "GDPFUN:"
                                                + contourAttributes[i]
                                                        .getGdpfun()
                                                + ";GLEVEL:"
                                                + contourAttributes[i]
                                                        .getGlevel()
                                                + ";GVCORD:"
                                                + contourAttributes[i]
                                                        .getGvcord()
                                                + ";"
                                                + "displayType:"
                                                + displayType
                                                + ";WIND:"
                                                + contourAttributes[i]
                                                        .getWind() + ";");
                                    }
                                    logger.debug(
                                            this.getClass().getCanonicalName()
                                                    + ":\n"
                                                    + "Offending statement: contourRenderable[i].setGridRelativeHiLo(createGridRelativeHiLoDisplay((NcFloatDataRecord)"
                                                    + " contourRenderable[i].getData(), gdPrxy, contourAttributes[i]));\n"
                                                    + "***HINT: parts of gdPrxy may have been null\n"
                                                    + npe.getMessage(), npe);
                                }
                            } else {
                                if (contourAttributes[i].getHilo() == null
                                        || contourAttributes[i].getHilo()
                                                .length() == 0) {
                                    contourRenderable[i]
                                            .setGridRelativeHiLo(null);
                                } else {
                                    try { // gdPrxy may not have been fully
                                          // assembled upstream
                                        contourRenderable[i]
                                                .setGridRelativeHiLo(createGridRelativeHiLoDisplay(
                                                        (NcFloatDataRecord) contourRenderable[i]
                                                                .getData(),
                                                        gdPrxy,
                                                        contourAttributes[i]));
                                    } catch (NullPointerException npe) {
                                        if (ncgribLogger.enableDiagnosticLogs()) {
                                            logger.info(this.getClass()
                                                    .getCanonicalName()
                                                    + ":Missing data was expected for:"
                                                    + "GDPFUN:"
                                                    + contourAttributes[i]
                                                            .getGdpfun()
                                                    + ";GLEVEL:"
                                                    + contourAttributes[i]
                                                            .getGlevel()
                                                    + ";GVCORD:"
                                                    + contourAttributes[i]
                                                            .getGvcord()
                                                    + ";"
                                                    + "displayType:"
                                                    + displayType
                                                    + ";WIND:"
                                                    + contourAttributes[i]
                                                            .getWind() + ";");
                                        }
                                        logger.debug(
                                                this.getClass()
                                                        .getCanonicalName()
                                                        + ":\n"
                                                        + "Offending statement: contourRenderable[i].setGridRelativeHiLo(createGridRelativeHiLoDisplay((NcFloatDataRecord)"
                                                        + " contourRenderable[i].getData(), gdPrxy, contourAttributes[i]));\n"
                                                        + "***HINT: parts of gdPrxy may have been null\n"
                                                        + npe.getMessage(), npe);
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

            // synchronized (ContourRenderable.class){
            long t1 = System.currentTimeMillis();
            int cnt = contourRenderable.length;
            for (int i = 0; i < cnt; i++) {
                if (contourAttributes[i] != null) {
                    String type = contourAttributes[i].getType().toUpperCase();

                    ContourRenderable contourGroup = contourRenderable[i];

                    if ((type.contains("C") || type.contains("F") || type
                            .contains("S")) && (contourGroup != null)) {

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
                logger.info("**From init to complete createContours/wireframe ("
                        + gdPrxy.getDataTime().toString()
                        + ") took:"
                        + (t2 - initTime));
            }
            return true;
            // }
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
            // contourName.append(gridRscData.getGdfile().trim().toUpperCase() +
            // " ");
            // contourName.append(contourAttributes.getGdpfun().trim().toUpperCase()
            // + " ");
            // contourName.append(" ^" + gdPrxy.getDataTime().toString() + " @"
            // + contourAttributes.getGlevel() +
            // " %" + contourAttributes.getGvcord().toUpperCase());
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

        private NcFloatDataRecord findGriddedDataFromContourRenderable(int i) {
            NcFloatDataRecord rec = null;
            if (i > 0) {
                for (int n = 0; n < i; n++) {
                    if (contourRenderable[n] != null) {
                        if (contourRenderable[n].isMatch(contourAttributes[i])
                                && contourRenderable[n].getData() instanceof NcFloatDataRecord) {
                            rec = (NcFloatDataRecord) contourRenderable[n]
                                    .getData();
                            break;
                        }
                    }
                }
            }
            return rec;
        }

        private NcFloatDataRecord findGriddedDataFromVector(int i) {
            NcFloatDataRecord rec = null;

            if (i > 0) {
                for (int n = 0; n < i; n++) {
                    if (vectorDisplay[n] != null) {
                        if (vectorDisplay[n].isMatch(contourAttributes[i])
                                && vectorDisplay[n].getData() instanceof NcFloatDataRecord) {
                            rec = (NcFloatDataRecord) vectorDisplay[n]
                                    .getData();
                            break;
                        }
                    }
                }
            }
            return rec;
        }

        private NcFloatDataRecord getGriddedData(int i) {
            NcFloatDataRecord gridData = null;
            try {
                long t1 = System.currentTimeMillis();
                try {
                    gridData = (NcFloatDataRecord) getDataRecord(gdPrxy,
                            contourAttributes[i], this.cacheData);
                } catch (NullPointerException npe) {
                    if (ncgribLogger.enableDiagnosticLogs()) {
                        logger.info(this.getClass().getCanonicalName()
                                + ":Missing data was expected for:" + "GDPFUN:"
                                + contourAttributes[i].getGdpfun() + ";GLEVEL:"
                                + contourAttributes[i].getGlevel() + ";GVCORD:"
                                + contourAttributes[i].getGvcord() + ";WIND:"
                                + contourAttributes[i].getWind() + ";");
                    }
                    logger.debug(
                            this.getClass().getCanonicalName()
                                    + ":\n"
                                    + "Offending statement: gridData = (NcFloatDataRecord) getDataRecord(gdPrxy,ontourAttributes[i], this.cacheData);\n"
                                    + "***HINT: parts of gdPrxy may have been null\n"
                                    + npe.getMessage(), npe);
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
                    logger.info("getGriddedData contour/streamline/vector grid data("
                            + gdPrxy.getDataTime().toString()
                            + ") took:"
                            + (t2 - t1));
                }
            } catch (DgdrivException e) {
                // TODO Auto-generated catch block
                // e.printStackTrace();
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
        // data.addChangeListener((IResourceDataChanged) this);
        gridRscData = data;
    }

    // override the base version which queries the db based on the metadatamap
    // since this will return too many objects which won't be used. This will
    // just
    // query the availableTimes and use them to time match to the frames.
    //
    @Override
    public void queryRecords() throws VizException {
        ResourceName rscName = getResourceData().getResourceName();

        DataTime cycleTime = rscName.getCycleTime();

        if (rscName.isLatestCycleTime()) { // latest should
                                           // already be
                                           // resolved
                                           // here.
            return;
        }

        List<DataTime> availableTimes = new ArrayList<DataTime>();

        if (cycleTime != null
                && gridRscData.getPluginName().equalsIgnoreCase(
                        GempakGrid.gempakPluginName)) {
            try {
                String dataLocation = null;
                try {
                    dataLocation = GempakGrid.getGempakGridPath(gridRscData
                            .getGdfile());

                } catch (VizException e) {
                    logger.debug(
                            "Unable to specify location for "
                                    + gridRscData.getPluginName() + " "
                                    + gridRscData.getGdfile(), e);
                    return;
                }
                String[] gridAvailableTimes = GempakGrid.getAvailableGridTimes(
                        dataLocation, cycleTime.toString(), gridRscData
                                .getGdfile().toLowerCase());
                // availableTimes = new DataTime[gridAvailableTimes.length];
                for (int ii = 0; ii < gridAvailableTimes.length; ii++) {
                    availableTimes.add(new DataTime(gridAvailableTimes[ii]));
                }
            } catch (Exception e) {
                return;
            }
        } else {
            /*
             * // Note that since this is not constraining the grid parameter,
             * level... there is // still a potential problem if the db is
             * missing times for the displayed param // while times for other
             * params exist. HashMap<String, RequestConstraint> queryList = new
             * HashMap<String, RequestConstraint>(
             * gridRscData.getMetadataMap());
             * 
             * if (gridRscData.getGdfile().startsWith("{") &&
             * gridRscData.getGdfile().endsWith("}")) { ModelListInfo
             * modelListInfo = new ModelListInfo(gridRscData.getGdfile());
             * String modelName =
             * modelListInfo.getModelList().get(0).getModelName(); String
             * perturbationNum = null; // String eventName = null; if (
             * modelName.contains(":") ) { String [] gdfileArrs =
             * modelName.split(":"); modelName = gdfileArrs[0]; if (
             * gdfileArrs[0].contains("%")) { modelName =
             * gdfileArrs[0].split("%")[1]; } // eventName =
             * gdfileArrs[1].toLowerCase(); if ( isIntNum (gdfileArrs[1])) {
             * perturbationNum =
             * String.valueOf(Integer.parseInt(gdfileArrs[1])); } } else { if (
             * modelName.contains("%")) { modelName = modelName.split("%")[1]; }
             * }
             * 
             * queryList.put("modelInfo.modelName", new RequestConstraint(
             * modelName, ConstraintType.EQUALS ) ); // if ( eventName != null
             * ){ // queryList.put("eventName", // new RequestConstraint(
             * eventName, ConstraintType.EQUALS ) ); // } if ( perturbationNum
             * != null ){ queryList.put("modelInfo.perturbationNumber", new
             * RequestConstraint( perturbationNum, ConstraintType.EQUALS ) ); }
             * } else { queryList.put("modelInfo.modelName", new
             * RequestConstraint( gridRscData.getGdfile(), ConstraintType.EQUALS
             * ) ); } LayerProperty property = new LayerProperty();
             * property.setDesiredProduct( ResourceType.PLAN_VIEW );
             * getAllTimes(gridRscData.getGdfile());
             * 
             * try {
             * 
             * long cycMs = cycleTime.getRefTime().getTime()/1000; String
             * cycTimeStr = new String
             * (QUERY_DATE_FORMAT.format(cycleTime.getRefTime
             * ().getTime())+".0");
             * 
             * ArrayList<DataTime> frameTimes = getFrameTimes();
             * Collections.sort(frameTimes);
             * 
             * if( frameTimes != null && frameTimes.size() > 0){
             * 
             * int frameIntrvl = (int)resourceData.getFrameSpan()/60; int lframe
             * = frameTimes.size()-1; int fMs =
             * (int)(((frameTimes.get(0).getRefTime().getTime())/1000 -
             * cycMs)/3600 - frameIntrvl); if ( fMs < 0 ) fMs = 0; int lMs =
             * (int)(((frameTimes.get(lframe).getRefTime().getTime())/1000
             * -cycMs)/3600 + frameIntrvl); // adjust the start and end times to
             * include the frameIntervals. If this isn't dominant, // we need to
             * query everything that may match the first/last frames. String
             * frameStartTimeStr = cycTimeStr + " (" + fMs + ")"; String
             * frameEndTimeStr = cycTimeStr + " (" + lMs + ")"; String timeStr =
             * frameStartTimeStr + "--" + frameEndTimeStr;
             * queryList.put("dataTime", new RequestConstraint (timeStr,
             * ConstraintType.BETWEEN)); } property.setEntryQueryParameters(
             * queryList ); availableTimes = property.getEntryTimes(); } catch(
             * VizException e) { return; }
             */
            availableTimes = gridRscData.getAvailableDataTimes();

        }

        ArrayList<DataTime> dataTimes = new ArrayList<DataTime>();

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
                        // reuse the same gribRec this is a bit of a hack but
                        // hey.
                        // gribRec.setDataTime(availTime);
                        // for( IRscDataObject dataObject : processRecord(
                        // availTime
                        // ) ) { // gribRec ) ) {
                        // newRscDataObjsQueue.add(dataObject);
                        // }
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
            // gribRec.getDataTime(), gribRec.getSpatialObject() );
            return new NcGridDataProxy[] { rscDataObj };
        } else if (obj instanceof GridRecord) {
            NcGridDataProxy rscDataObj = new NcGridDataProxy(
                    ((GridRecord) obj).getDataTime());
            return new NcGridDataProxy[] { rscDataObj };
        } else {
            System.out
                    .println("Unexpected object in NcGridResource.processRecord ");
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
        TITLE title = new TITLE(gridRscData.getTitle()); // rm 5/-1/. 5/-1/ ~ @
                                                         // HEIGHTS, ISOTACHS
                                                         // AND WIND (KTS)!0
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

            // if( currFrame == null || currFrame.gdPrxy == null || !
            // currFrame.hasData ) {
            // return String.format("%s %s", gridRscData.getGdfile(),
            // gridRscData.getTitle())+"-No Data";
            // }
            //
            // TITLE title = new TITLE (gridRscData.getTitle());
            // if ( title.getTitleString() !=null) {
            // nameStr = generateTitleInfo (title.getTitleString(),
            // currFrame.gdPrxy.getDataTime());
            // set legend color
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
                logger.info("NcgridResource.initResource query all avariable times: "
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
                currFrame.generateContours("Generating contours for Frame");
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
                if (contourGroup == null
                        || !(contourGroup.getData() instanceof NcFloatDataRecord)) {
                    continue;
                }

                if (contourGroup != null
                        && (type.contains("C") || type.contains("F") || type
                                .contains("S"))) {
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
                    issueRefresh();
                }
                /*
                 * Plot HILO if needed
                 */
                if (type.contains("C") || type.contains("F")) {
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
            // logger.info( "Paint this frame("+
            // currFrame.gdPrxy.getDataTime().toString()+") took:" +
            // (t1-initTime));
            // logger.info("*****From init resource to complete paint this Frame("+currFrame.gdPrxy.getDataTime().toString()+") took:"
            // + (t1-initTime));
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
    protected IDataRecord getDataRecord(NcGridDataProxy gdPrxy,
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
        // inputGdfile = checkEnsembleGdfiles(inputGdfile);//for perturbation
        // number
        ArrayList<DataTime> dataTimes = new ArrayList<DataTime>();
        dataTimes.add(gdPrxy.getDataTime());
        synchronized (Dgdriv.class) {
            Dgdriv aDgdriv = new Dgdriv();
            // aDgdriv.setCycleForecastTimes(dataTimesForDgdriv);
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
            NcFloatDataRecord data;
            try {
                data = aDgdriv.execute();
                if (data != null) {
                    subgObj = aDgdriv.getSubgSpatialObj();
                }
                return data;
                // return aDgdriv.execute();
            } catch (DgdrivException e) {
                // throw new DgdrivException("GEMPAK GD error stack:\n" +
                // e.getMessage(), e);
                logger.debug("GEMPAK GD error stack:\n" + e.getMessage());
                return null;
            }
            // finally {
            // aDgdriv = null;
            // }
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
            NcFloatDataRecord rec, NcGridDataProxy gdPrxy,
            ContourAttributes attr) {

        if (rec == null || rec.getXdata() == null) {
            return null;
        }

        if (!attr.getType().toUpperCase().contains("P")) {
            return null;
        }

        FloatBuffer plotData = FloatBuffer.wrap(rec.getXdata());
        COLORS color = new COLORS(attr.getColors());

        text = new TextStringParser(attr.getText());

        return new GridPointValueDisplay(plotData, color.getFirstColor(),
                text.getTextSize(), getNcMapDescriptor(),
                gdPrxy.getNewSpatialObject());
    }

    private GridRelativeHiLoDisplay createGridRelativeHiLoDisplay(
            NcFloatDataRecord rec, NcGridDataProxy gdPrxy,
            ContourAttributes attr) {

        DisplayType displayType = getVectorType(attr.getType());
        if (displayType != DisplayType.CONTOUR) {
            return null;
        }

        // NcFloatDataRecord rec = null;
        // try {
        // rec = (NcFloatDataRecord)getDataRecord(gdPrxy, attr);
        // } catch (DgdrivException e) {
        // e.printStackTrace();
        // }
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
                rec.getXdata(), nx, ny, hiloBuild.getRadius(),
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
                || type.toUpperCase().contains("F")) {
            return DisplayType.CONTOUR;
        } else {
            return null;
        }
    }

    private void separateAttributes() {
        String[] gfuncArray = gridRscData.getGdpfun().replaceAll("\\s+", "")
                .split("!", -1);
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
                    // boolean isDerived = gfuncArray[j].trim().contains("(");
                    // boolean isReferenced =
                    // gfuncArray[j].trim().contains(referencedAlias);
                    // if ( isReferenced ) {
                    // gfuncArray[j] = gfuncArray[j].replace(referencedAlias,
                    // referencedFunc);
                    // }
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

                // if (i > scaleArray.length - 1) {
                // contourAttributes[i].setScale("0");
                // } else {
                // contourAttributes[i].setScale(scaleArray[i]);
                // }
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

        System.out.println("returnedFunc=" + returnedFunc);
        return returnedFunc;
        // int parenthesisNumber = returnedFunc.split("\\(").length-1;
        // parenthesisNumber = openParenthesisNumber;
        // System.out.println();
        // System.out.println();
        //
        // /*
        // * Find the indices of opening parenthesis in the returnedFunc
        // */
        // int [] openIndxs = new int[parenthesisNumber];
        // int startOpenIndx = 0;
        // for (int j=0;j<parenthesisNumber; j++){
        // openIndxs[j] = returnedFunc.indexOf("(", startOpenIndx);
        // startOpenIndx = openIndxs[j]+1;
        // }
        // /*
        // * Find the indices of closing parenthesis in the returnedFunc
        // */
        // boolean done = false;
        //
        // int [] closeIndxs = new int[parenthesisNumber];
        // int startCloseIndx = 0;
        // int kk = parenthesisNumber-1;
        // int jj=0;
        // int foundCloseParenthesisNumber = 0;
        // while ( !done ) {
        // boolean foundMatchingParenthesis = false;
        // int anIndex = returnedFunc.indexOf(")", startCloseIndx);
        // kk = 0;
        // while ( !foundMatchingParenthesis) {
        // if ( anIndex < openIndxs[kk]) {
        // jj = kk - 1;
        // if ( closeIndxs[jj] == 0 && openIndxs[jj] !=0 ) {
        // foundMatchingParenthesis = true;
        // closeIndxs[jj] = anIndex;
        // foundCloseParenthesisNumber++;
        // kk++;
        // }
        // }
        // else {
        // kk++;
        // }
        // }
        // if ( foundCloseParenthesisNumber != parenthesisNumber) {
        // done = false;
        // }
        // else {
        // done = true;
        // }
        // }
        // for (int j=0;j<parenthesisNumber; j++){
        // openIndxs[j] = returnedFunc.indexOf("(", startOpenIndx);
        // closeIndxs[kk] = returnedFunc.indexOf(")", startCloseIndx);
        // startCloseIndx = closeIndxs[kk]+1;
        // startOpenIndx = openIndxs[j]+1;
        // kk--;
        // }
        //
        // /*
        // * Traverse the returnedFunc and substitute each occurrence of
        // * referencedAlias with referencedFunc, starting with inner most
        // * argument
        // */
        // for (int j=parenthesisNumber;j>=1; j--){
        // String theNestedArgument = returnedFunc.substring(openIndxs[j-1]+1,
        // closeIndxs[j-1]);
        // System.out.println("returnedFunc="+returnedFunc);
        // System.out.println("returnedFunc="+returnedFunc);
        // System.out.println("the theNestedArgument="+theNestedArgument);
        // if ( theNestedArgument.contains(",")) {
        // String [] nestedArgArray = theNestedArgument.split(",");
        // for (int nn=0; nn<nestedArgArray.length;nn++){
        // if (nestedArgArray[nn].equalsIgnoreCase(referencedAlias)) {
        // StringBuilder strb = new StringBuilder();
        // int startIndx = returnedFunc.indexOf(nestedArgArray[nn]);
        // int endIndx = startIndx + nestedArgArray[nn].length();
        // int returnedFuncLen = returnedFunc.length();
        // strb.append(returnedFunc, 0, startIndx);
        // strb.append(referencedFunc);
        // strb.append(returnedFunc, endIndx, returnedFuncLen);
        // returnedFunc = strb.toString();
        // closeIndxs[j-2] = closeIndxs[j-2] + referencedFunc.length() -
        // referencedAlias.length();
        // }
        // else {
        // StringBuilder strb = new StringBuilder();
        // int startIndx = returnedFunc.indexOf(nestedArgArray[nn]);
        // int endIndx = startIndx + nestedArgArray[nn].length();
        // int returnedFuncLen = returnedFunc.length();
        // strb.append(returnedFunc, 0, startIndx);
        // strb.append(nestedArgArray[nn]);
        // strb.append(returnedFunc, endIndx, returnedFuncLen);
        // returnedFunc = strb.toString();
        // }
        //
        // }
        // }
        // else {
        // if (theNestedArgument.equalsIgnoreCase(referencedAlias)) {
        // StringBuilder strb = new StringBuilder();
        // int startIndx = returnedFunc.indexOf(theNestedArgument);
        // int endIndx = startIndx + theNestedArgument.length();
        // int returnedFuncLen = returnedFunc.length();
        // strb.append(returnedFunc, 0, startIndx);
        // strb.append(referencedFunc);
        // strb.append(returnedFunc, endIndx, returnedFuncLen);
        // returnedFunc = strb.toString();
        // closeIndxs[j-2] = closeIndxs[j-2] + referencedFunc.length() -
        // referencedAlias.length();
        // }
        // else {
        // StringBuilder strb = new StringBuilder();
        // int startIndx = returnedFunc.indexOf(theNestedArgument);
        // int endIndx = startIndx + theNestedArgument.length();
        // int returnedFuncLen = returnedFunc.length();
        // strb.append(returnedFunc, 0, startIndx);
        // strb.append(theNestedArgument);
        // strb.append(returnedFunc, endIndx, returnedFuncLen);
        // returnedFunc = strb.toString();
        // }
        // }
        // System.out.println("returnedFunc="+returnedFunc);
        // }
        // return returnedFunc;

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

        // String modelname = gridRscData.getGdfile();
        String modelname = gridRscData.getResourceName().getRscType();
        // System.out.println(" modelname: " + modelname);
        // if (gridRscData.getEnsembelMember() != null) {
        // modelname = modelname + ":" + gridRscData.getEnsembelMember();
        // } else {
        // if (gridRscData.getEventName() != null) {
        // modelname = modelname + ":" + gridRscData.getEventName();
        // }
        // }
        titleInfoStr = modelname + " "
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

        DataTime currFrameTm = (DataTime) getCurrentFrameTime();

        /*
         * check '!' and remove it for now ???
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
                cal = currFrameTm.getValidTime();
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

    // private String checkEnsembleGdfiles ( String gdfile ) {
    // if (gdfile.startsWith("{") && gdfile.endsWith("}") ) {
    // StringBuffer sba = new StringBuffer();
    // String [] gdfileArray = gdfile.substring(gdfile.indexOf("{")+1,
    // gdfile.indexOf("}")).split(",");
    // for (int igd=0;igd<gdfileArray.length; igd++){
    // if ( gdfileArray[igd].contains("|") ) {
    // String [] tempArr = gdfileArray[igd].split("\\|");
    // if ( tempArr[0].contains(":")) {
    // if ( ! isIntNum (tempArr[0].split(":")[1]) ) continue;
    // }
    //
    // }
    // else {
    // if ( gdfileArray[igd].contains(":")) {
    // if ( ! isIntNum (gdfileArray[igd].split(":")[1]) ) continue;
    // }
    //
    // }
    // sba.append(gdfileArray[igd]);
    // sba.append(",");
    // }
    // String retStr = "";
    // if ( sba.toString().length() > 0 ) {
    // retStr = sba.toString().substring(0, sba.toString().length()-1);
    // }
    // return "{" + retStr + "}";
    //
    // }
    // return gdfile;
    // }
    private boolean isIntNum(String num) {
        String NON_NEGATIVE_INTEGER = "(\\d){1,9}";

        if (num.matches(NON_NEGATIVE_INTEGER)) {
            return true;
        }
        return false;
    }

    public static String getFilename(String dataURI) {
        String filename = null;
        File file = null;
        String[] uriStr = dataURI.split("/");
        String path = uriStr[3];
        StringBuilder sb = new StringBuilder();
        String[] tmStr = uriStr[2].split("_");
        String dataDateStr = tmStr[0];
        String fhrs = tmStr[2].substring(tmStr[2].indexOf("(") + 1,
                tmStr[2].indexOf(")"));
        String fhStr;
        int number = 0;

        if (fhrs == null) {
            fhStr = "000";
        } else {
            try {
                number = Integer.parseInt(fhrs);
            } catch (NumberFormatException e) {
                //
            }
            fhStr = forecastHourFormat.format(number);
        }
        sb.append(path);
        sb.append("-");
        sb.append(dataDateStr);
        String dataTimeStr = tmStr[1].split(":")[0] + "-FH-" + fhStr;
        sb.append("-");
        sb.append(dataTimeStr);
        sb.append(".h5");

        // if (DataMode.getSystemMode() == DataMode.THRIFT) {
        // file = new File(File.separator + dataURI.split("/")[1]
        // + File.separator + path + File.separator + sb.toString());
        // } else if (DataMode.getSystemMode() == DataMode.PYPIES) {
        file = new File(
        // TODO--OK?? VizApp.getServerDataDir() + File.separator +
                dataURI.split("/")[1] + File.separator + path + File.separator
                        + sb.toString());
        // } else {
        // file = new File(VizApp.getDataDir() + File.separator
        // + dataURI.split("/")[1] + File.separator + path
        // + File.separator + sb.toString());
        // }

        if (file != null) {
            filename = file.getAbsolutePath();
        }
        return filename;
    }
}
