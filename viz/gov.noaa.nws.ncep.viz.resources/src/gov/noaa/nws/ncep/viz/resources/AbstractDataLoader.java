package gov.noaa.nws.ncep.viz.resources;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataquery.requests.DbQueryRequest;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.dataquery.responses.DbQueryResponse;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.drawables.IFrameCoordinator;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.requests.ThriftClient;

import gov.noaa.nws.ncep.viz.common.display.INatlCntrsDescriptor;
import gov.noaa.nws.ncep.viz.resources.time_match.NCTimeMatcher;

/**
 * <pre>
 * 
 * AbstractDataLoader is part of the redesign of the *Resource classes as
 * described in Redmine 11819, Redmine 8830, and in the title block comment of
 * {@link gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource2
 * 
 * This class holds the default data loading methods previously housed in
 * AbstractNatlCntrsResource. This class is to be extended into resource
 * specific *DataLoader classes that will handle data loading for specific
 * resources.
 * 
 * SOFTWARE HISTORY Date Ticket# Engineer Description ------------
 * --------------------- -------------------------- 10/21/2015 R11819 S Russell
 * Initial Creation. 03/07/2016 R10155 B Hebbard In order to handle auto-update
 * fully, move update-related code here from AbstractNatlCntrsResource2.
 * 04/13/2016 R15954 S Russell Added a statusHandler to use for all child
 * classes
 * 
 * </pre>
 * 
 * @author S Russell
 * @version 1
 */
public abstract class AbstractDataLoader implements IDataLoader {

    /**
     * Use this for all child classes
     */
    protected static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(AbstractDataLoader.class);

    /**
     * The resource associated with this data loader.
     */
    protected AbstractNatlCntrsResource2<?, ?> resource;

    /**
     * The resource data that was used to construct this resource, and generic
     * properties that should be used in the rendering of the data
     */
    protected AbstractNatlCntrsRequestableResourceData resourceData;

    /**
     * The descriptor that this resource is contained in. This is frequently
     * useful for finding information out about the background
     * environment/information about the temporal or spatial context
     */
    protected INatlCntrsDescriptor descriptor;

    // This list is populated by the initial query and also by the auto update
    // when new data is ingested for this resource. These objects time matched
    // to one or more frames.
    protected ConcurrentLinkedQueue<IRscDataObject> newRscDataObjsQueue;

    // This list caches objects that are ingested and are newer than the latest
    // frame. When a frame is later created due to auto updating, these
    // objects are moved to the newRscDataObjsQueue.
    protected ArrayList<IRscDataObject> autoUpdateCache;

    // The new frame times that will be created during the next auto update
    protected ArrayList<DataTime> newFrameTimesList;

    protected boolean autoUpdateReady = false;

    // If the frameGenMthd is USE_FRAME_INTERVAL then this is the time
    // when the next frame will be created if autoupdate is on.
    // Note: that more than one frame may be created if the user turns
    // autoupdate on long after the next frame time has past.
    private DataTime nextFrameTime = null;

    // Map from a frame time string to a structure containing data for that
    // frame
    protected TreeMap<Long, AbstractFrameData> frameDataMap;

    protected DbQueryRequest dbRequest;

    /**
     * Constructor
     */
    public AbstractDataLoader() {
        autoUpdateCache = new ArrayList<>();
        newFrameTimesList = new ArrayList<>();
        newRscDataObjsQueue = new ConcurrentLinkedQueue<>();
        dbRequest = new DbQueryRequest();
    }

    /**
     * Processes a new PluginDataObject (PDO), or an entire array of them, that
     * have come in after the resource is loaded and displaying (that is, after
     * initial data load). Processes each PDO into one or more Resource Data
     * Objects (RDOs) which are individually time matchable, and queues them for
     * later
     * 
     * @param object
     *            Either one incoming PDO, or an array of them
     */
    public void resourceDataUpdate(Object object) {
        if (object instanceof Object[]) {
            for (Object obj : (Object[]) object) {
                for (IRscDataObject dataObj : processRecord(obj)) {
                    newRscDataObjsQueue.add(dataObj);
                }
            }
        } else {
            for (IRscDataObject dataObj : processRecord(object)) {
                newRscDataObjsQueue.add(dataObj);
            }
        }

        // We can't call processNewRscDataList here since we are not in the UI
        // thread when the AutoUpdater calls us and this will end up updating
        // the status line's frame time is a new frame is created.
        //
        autoUpdateReady = true;
    }

    @Override
    public void setFrameDataMap(TreeMap<Long, AbstractFrameData> frameDataMap) {
        this.frameDataMap = frameDataMap;

    }

    @Override
    public void setNewRscDataQueue(
            ConcurrentLinkedQueue<IRscDataObject> newRscDataObjsQueue) {
        this.newRscDataObjsQueue = newRscDataObjsQueue;

    }

    @Override
    public void setResource(AbstractNatlCntrsResource2<?, ?> resource) {
        this.resource = resource;
    }

    /**
     * This method can be used as a convenience if the MetadataMap constraints
     * are all that is needed to query the data.
     * 
     * @throws VizException
     */
    public void queryRecords() throws VizException {

        HashMap<String, RequestConstraint> queryList = new HashMap<>(
                resourceData.getMetadataMap());

        queryRecords(queryList);
    }

    /**
     * 
     * This method is used for querying data when you need a customized list of
     * query restraints
     * 
     * @param <String,
     *            RequestConstraint> requestConstraints
     * @throws VizException
     */

    public void queryRecords(
            HashMap<String, RequestConstraint> requestConstraints)
                    throws VizException {

        dbRequest.setConstraints(requestConstraints);
        DbQueryResponse response = (DbQueryResponse) ThriftClient
                .sendRequest(dbRequest);

        for (Map<String, Object> result : response.getResults()) {
            for (Object pdo : result.values()) {
                for (IRscDataObject dataObject : processRecord(pdo)) {
                    newRscDataObjsQueue.add(dataObject);
                }
            }
        }

        setAllFramesAsPopulated();
    }

    /**
     * 
     * This is the default implementation for the common case where the Resource
     * simply wants to process the PluginDataObject itself but some resources
     * may want to override this method if a record contains more than one 'data
     * object' and these objects are to be time matched. (ex. some sigmet
     * resources have multiple sigmets per record and each has a separate valid
     * time.)
     * 
     * @param Object
     *            pdo - plugin data object
     * @return IRscDataObject[]
     * 
     */

    protected IRscDataObject[] processRecord(Object pdo) {
        if (!(pdo instanceof PluginDataObject)) {
            System.out.println(
                    "Resource Impl " + getClass().getName() + " must override "
                            + "the processRecord method to process data objects of class: "
                            + pdo.getClass().getName());
            return null;
        }

        DfltRecordRscDataObj rscDataObj = new DfltRecordRscDataObj(
                (PluginDataObject) pdo);
        return new DfltRecordRscDataObj[] { rscDataObj };
    }

    /**
     * Takes incoming Resource Data Objects (RDOs) that have been queued to
     * newRscDataObjsQueue, and assigns each of them to one or more frames
     * according to time matching criteria. Also handles auto-update situations
     * (for data coming in after the resource is loaded and displaying.)
     * 
     * @return
     */
    protected synchronized boolean processNewRscDataList() {

        // Allow resources to pre process the data before it is added to the
        // frames
        preProcessFrameUpdate();

        NCTimeMatcher timeMatcher = (NCTimeMatcher) descriptor.getTimeMatcher();

        while (!newRscDataObjsQueue.isEmpty()) {
            IRscDataObject rscDataObj = newRscDataObjsQueue.poll();

            boolean foundFrame = false;
            boolean lastFrame = false;

            // loop through the frames and add this record to all frames
            // to which it time matches
            //
            for (AbstractFrameData frameData : frameDataMap.values()) {
                if (frameData != null) {
                    if (frameData.isRscDataObjInFrame(rscDataObj)) {
                        if (addRscDataToFrame(frameData, rscDataObj)) {
                            foundFrame = true;
                        }

                        if (frameData == frameDataMap.lastEntry().getValue()) {
                            lastFrame = true;
                        }
                    }
                }
            }

            // If not in any frames (or if in the last frame) and if updating
            // and if this the data is newer than the latest frame then cache
            // the data for auto update.
            //
            // NOTE: this will 'drop' (not cache) data from the initial query
            // that doesn't match the selected timeline which means that if the
            // user later enables auto-update, this data will not be loaded as
            // part of the auto-update. This is by design but could be changed
            // if the user wants a different behaviour.)
            //
            // NOTE: if the data is in the last frame then it may still
            // potentially match the next frame when an update occurs. (WAPITA)
            // (Note2: the auto update code is written to update a frame even
            // if it is not the last frame. (ie. data is received out of order
            // for some reason.) but this will cause any non-dominant data to
            // not be displayed on these frames because we are only checking for
            // the last frame here.)
            long dataTimeMs = getDataTimeMs(rscDataObj);

            if (timeMatcher.isAutoUpdateable()) {

                if ((!foundFrame && autoUpdateReady
                        && dataTimeMs > frameDataMap.firstKey()) || lastFrame) {
                    // If there is the possibility of auto updating (if the
                    // dominant resource is a satellite or radar image) then
                    // store off the data in the autoUpdateCache and then update
                    // the timeline if auto update is enabled..
                    autoUpdateCache.add(rscDataObj);

                    // If this is the dominantResource, and this data is from a
                    // data update alert, then determine if a new frame is
                    // needed.
                    if (resource.isDominantResource() && autoUpdateReady) {
                        newFrameTimesList
                                .addAll(timeMatcher.determineNewFrameTimes(
                                        rscDataObj.getDataTime()));
                    }
                }
            }
        }

        // allow resources to post-process the data after it is added to the
        // frames
        postProcessFrameUpdate();

        autoUpdateReady = false;

        // If there is data in the auto update cache and if auto update is now
        // enabled and if this is the dominant resource for the timeline then
        // we need to update the timeline and process the data in the cache.

        if (!newFrameTimesList.isEmpty() && descriptor.isAutoUpdate()) {

            // Update the list of times in the timeMatcher. This will then
            // trigger the descriptor to update its datatimes and notify its
            // resources (including this one) to create and update the frames.
            timeMatcher.updateTimeline(newFrameTimesList);

            newFrameTimesList.clear();
            // advance to the new last frame (per legacy; could change)...
            descriptor.getFrameCoordinator().changeFrame(
                    IFrameCoordinator.FrameChangeOperation.LAST,
                    IFrameCoordinator.FrameChangeMode.TIME_ONLY);
            // ...and make sure it gets painted
            // resource.issueRefresh();
        }

        return true;
    }

    // the timeline (times in the timeMatcher) have changed so we must update
    // the frames in the frameDataMap and process any data saved in the
    // autoUpdateCache
    public Boolean updateTimeline() {
        NCTimeMatcher timeMatcher = (NCTimeMatcher) descriptor.getTimeMatcher();
        List<DataTime> newFrameTimes = timeMatcher.getFrameTimes();

        // Loop through all of the new frame times, and if the frameDataMap
        // doesn't
        // have an entry for this time, than create a new entry.
        for (DataTime frameTime : newFrameTimes) {
            if (!frameDataMap.containsKey(
                    frameTime.getValidTime().getTime().getTime())) {
                AbstractFrameData newFrame = resource.createNewFrame(frameTime,
                        resourceData.frameSpan);
                frameDataMap.put(frameTime.getValidTime().getTime().getTime(),
                        newFrame);
            }
        }

        // Loop through all of the times in the frameDataMap, and if the time is
        // not in the new frameTimes, then remove the frame from the map.
        ArrayList<Long> frameTimesInMap = new ArrayList<>(
                frameDataMap.keySet());

        for (long frameTimeMs : frameTimesInMap) {
            if (!newFrameTimes.contains(new DataTime(new Date(frameTimeMs)))) {
                frameDataMap.get(frameTimeMs).dispose();
                frameDataMap.remove(frameTimeMs);
            }
        }

        // Copy all of the cached objects for auto update and process them
        newRscDataObjsQueue.addAll(autoUpdateCache);
        autoUpdateCache.clear();

        return true;
    }

    public void setNextFrameTime(DataTime nextFrameTime) {
        this.nextFrameTime = nextFrameTime;
    }

    /**
     * Resource can call this (as on each paint) to tell the data loader to
     * check for any new incoming data, and load it if appropriate
     */
    public void processAnyNewData() {
        if (!newRscDataObjsQueue.isEmpty() || (!newFrameTimesList.isEmpty()
                && descriptor.isAutoUpdate())) {
            processNewRscDataList();
        }
    }

    // break out from updateFrames to allow subclasses to override if needed
    protected boolean addRscDataToFrame(AbstractFrameData frameData,
            IRscDataObject rscDataObj) {
        return frameData.updateFrameData(rscDataObj);
    }

    // allow this to be overridden if derived class needs to
    protected boolean preProcessFrameUpdate() {
        return true;
    }

    // allow this to be overridden if derived class needs to
    protected boolean postProcessFrameUpdate() {
        return true;
    }

    // set the populated flag in all frames
    // this is done when the data for all frames has been queried at one time
    // as opposed to populating as each frame is displayed.
    protected void setAllFramesAsPopulated() {
        for (AbstractFrameData frameData : frameDataMap.values()) {
            frameData.setPopulated(true);
        }
    }

    public DbQueryRequest getDbRequest() {
        return dbRequest;
    }

    public void setDbRequest(DbQueryRequest dbRequest) {
        this.dbRequest = dbRequest;
    }

    public void addDbRequestOrderBy(String field) {
        dbRequest.setOrderByField(field);
    }

    // added since WarnResource gets null pointers in processNewRscDataList()
    protected long getDataTimeMs(IRscDataObject rscDataObj) {
        return rscDataObj.getDataTime().getValidTime().getTime().getTime();
    }

    @Override
    public void setDescriptor(INatlCntrsDescriptor descriptor) {
        this.descriptor = descriptor;
    }

}// end of class AbstractDataLoader