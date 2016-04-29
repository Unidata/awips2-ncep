package gov.noaa.nws.ncep.viz.resources;

import gov.noaa.nws.ncep.viz.common.display.INatlCntrsDescriptor;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData.TimelineGenMethod;
import gov.noaa.nws.ncep.viz.resources.time_match.NCTimeMatcher;
import gov.noaa.nws.ncep.viz.ui.display.NCMapDescriptor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.IFrameCoordinator;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.IResourceDataChanged;
import com.raytheon.uf.viz.core.rsc.LoadProperties;

/**
 * <pre>
 * 
 * This class is part of a redesign described in Redmine 11819.  The goal of
 * the redesign is to separate out the code that handles data loading from
 * the Resource classes.  The goal is to  make it such that any given resource 
 * shouldn't need to know how to load data.
 * 
 * Resource classes will get a *DataLoader from a *ResourceData class.  The
 * *DataLoader will class will have data loading code tailored to that resource.
 * These *DataLoader classes will typically be called in the initResource()
 * method for the preloading of data.  Eventually, they will be used for
 * "lazy loading" ( loading one frame, as each frame is brought up in CAVE )
 * too.
 * 
 * A new class in the same package as AbstractNatlCntrsResource called
 * AbstractDataLoader will house the default data loading methods originally
 * in AbstractNatlCntrsResource.   AbstractDataLoader will also implment the
 * IDataLoader interface.  All *DataLoader class will be extended from
 * AbstractDataLoader overriding the default data loading methods as needed.
 * 
 * The frameDataMap and newRscDataObjsQueue collections are used both in
 * data loading and presentation.  Since the AbstractNatlCntrsResource class
 * uses generic typing and nested inner classes/interfaces which are extended
 * by child classes it wasn't possible to pass these collections into the
 * new design without having the new design dependent on the 
 * AbstractNatlCntrsResource.  
 * 
 * Instead, this class AbstractNatlCntrsResource2 was created to get around
 * those data type/input argument issues and to sever the dependcies on
 * AbstractNatlCntrsResource.  This class has had the data loading code removed,
 * with plans to remove more as is possible.  AbstractNatlCntrsResource2 also
 * shows what AbstractNatlCntrsResource would eventually look like with the
 * 38 child resources classes converted over.  When that point is reached this
 * class can be swapped out for that.  
 * 
 * That is a considerable amount of work so this class exists to make the
 * transition more graceful and convenient.
 * 
 * Implementing the new design for any given resource is a signficant amount
 * of work so replacing methods deprecated in the AbstractNatlCntrsResource
 * class should be discussed on a case by case basis.
 * 
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer          Description
 * ------------ ----------  -----------     ----------------------------------
 * 11/20/2015   RM 11819    Steve Russell   Initial Creation. Copied
 *                                          AbstractNatlCntrsResource2 and
 *                                          trimmed out the data loading code
 * 
 * @author ghull (original), srussell ( this truncated version )
 * @version 1
 */
public abstract class AbstractNatlCntrsResource2<T extends AbstractNatlCntrsRequestableResourceData, D extends INatlCntrsDescriptor>
        extends
        AbstractVizResource<AbstractNatlCntrsRequestableResourceData, IDescriptor>
        implements INatlCntrsResource {

    private static final IUFStatusHandler logger = UFStatus
            .getHandler(AbstractNatlCntrsResource2.class);

    protected boolean initialized = false;

    // Map from a frame time string to a structure containing
    // displayable data elements for that frame...
    protected TreeMap<Long, AbstractFrameData> frameDataMap;

    // This list is populated by the initial query and also by the auto update
    // when new data is ingested for this resource. These objects time matched
    // to one or more frames.
    protected ConcurrentLinkedQueue<IRscDataObject> newRscDataObjsQueue;

    // This list caches objects that are ingested and are newer than the latest
    // frame. When a frame is later created due to auto updating, these
    // objects are moved to the newRscDataObjsQueue.
    protected ArrayList<IRscDataObject> autoUpdateCache;

    // the new frame times that will be created during the next auto update
    protected ArrayList<DataTime> newFrameTimesList;

    protected DataTime currFrameTime;

    protected boolean autoUpdateReady = false;

    // if the frameGenMthd is USE_FRAME_INTERVAL then this is the time
    // when the next frame will be created if autoupdate is on.
    // Note: that more than one frame may be created if the user turns
    // autoupdate on long after the next frame time has past.
    private DataTime nextFrameTime = null;

    protected AbstractNatlCntrsResource2(T resourceData, LoadProperties props) {
        super(resourceData, props);
        frameDataMap = new TreeMap<Long, AbstractFrameData>();
        newRscDataObjsQueue = new ConcurrentLinkedQueue<IRscDataObject>();

        autoUpdateCache = new ArrayList<IRscDataObject>();
        newFrameTimesList = new ArrayList<DataTime>();
        currFrameTime = null;

        // if requestable add a resourceChanged listener that is called by
        // Raytheon's AbstractRequestableResource when update() is called.
        if (resourceData instanceof AbstractNatlCntrsRequestableResourceData) {
            resourceData.addChangeListener(new IResourceDataChanged() {
                @Override
                public void resourceChanged(ChangeType type, Object object) {

                    if (object == null) {
                        logger.debug("resourceChanged called with null object for: "
                                + getResourceData().getResourceName()
                                        .toString());
                        return;
                    }

                    // TODO : need to make sure that these are the same types of
                    // objects that are returned by the queryRecords method (or
                    // the method that the resource uses to populate the
                    // frameData).
                    if (type == ChangeType.DATA_UPDATE) {
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

                        // can't call processNewRscDataList here since we are
                        // not in the UI thread when
                        // the AutoUpdater calls us and this will end up
                        // updating the status line's
                        // frame time is a new frame is created.
                        //
                        autoUpdateReady = true;
                    }
                }
            });
        }
    }

    // the timeline (times in the timeMatcher) have changed so we must update
    // the frames in the frameDataMap and process any data saved in the
    // autoUpdateCache.
    public Boolean updateTimeline() {
        NCTimeMatcher timeMatcher = (NCTimeMatcher) descriptor.getTimeMatcher();
        List<DataTime> newFrameTimes = timeMatcher.getFrameTimes();

        // loop thru all of the new frame times and if the frameDataMap doesn't
        // have an entry for this time create a new entry.
        for (DataTime frameTime : newFrameTimes) {
            if (!frameDataMap.containsKey(frameTime.getValidTime().getTime()
                    .getTime())) {
                AbstractFrameData newFrame = this.createNewFrame(frameTime,
                        resourceData.frameSpan);
                frameDataMap.put(frameTime.getValidTime().getTime().getTime(),
                        newFrame);
            }
        }

        // loop thru all of the times in the frameDataMap and if the time is not
        // in the new frameTimes then remove the frame from the map.
        ArrayList<Long> frameTimesInMap = new ArrayList<Long>(
                frameDataMap.keySet());

        for (long frameTimeMs : frameTimesInMap) {
            if (!newFrameTimes.contains(new DataTime(new Date(frameTimeMs)))) {
                frameDataMap.get(frameTimeMs).dispose();
                frameDataMap.remove(frameTimeMs);
            }
        }

        // copy all of the cached objects for auto update and process them.
        newRscDataObjsQueue.addAll(autoUpdateCache);
        autoUpdateCache.clear();

        return true;
    }

    /**
     * Deprecated as part of the Redmine 11819 redesign which is separating out
     * data loading from the resource family. processRecord() is kept here but
     * deprecated for child classes that extend it and where it is not yet
     * practical to refactor its use out. processRecord() is also called in the
     * constructor of this class and kept, but deprecated for the same reasons.
     * 
     * 
     * This is the default implementation for the common case where the Resource
     * simply wants to process the PluginDataObject itself but some resources
     * may want to override this method if a record contains more than one 'data
     * object' and these objects are to be time matched. (ex. some sigmet
     * resources have multiple sigmets per record and each has a separate valid
     * time.)
     * 
     * Another default version of processRecord() is in class AbstractDataLoader
     * future use of processRecord() should be done with that copy
     * 
     * @deprecated
     */
    @Deprecated
    protected IRscDataObject[] processRecord(Object pdo) {
        if (!(pdo instanceof PluginDataObject)) {
            logger.debug("Resource Impl "
                    + getClass().getName()
                    + " must override "
                    + "the processRecord method to process data objects of class: "
                    + pdo.getClass().getName());
            return null;
        }

        DfltRecordRscDataObj rscDataObj = new DfltRecordRscDataObj(
                (PluginDataObject) pdo);
        return new DfltRecordRscDataObj[] { rscDataObj };
    }

    // This assumes that the given time is in the map
    public AbstractFrameData getFrame(DataTime dataTime) {
        AbstractFrameData frameData = null;

        if (dataTime != null) {
            frameData = frameDataMap.get(dataTime.getValidTime().getTime()
                    .getTime());
        }
        return frameData;
    }

    public AbstractFrameData getCurrentFrame() {
        if (currFrameTime == null) {
            return null;
        }
        return frameDataMap.get(currFrameTime.getValidTime().getTime()
                .getTime());
    }

    public DataTime getCurrentFrameTime() {
        return currFrameTime;
    }

    public ArrayList<DataTime> getFrameTimes() {
        ArrayList<DataTime> frmTimes = new ArrayList<DataTime>();
        for (long t : frameDataMap.keySet()) {
            frmTimes.add(new DataTime(new Date(t)));
        }
        return frmTimes;
    }

    @Override
    public INatlCntrsDescriptor getDescriptor() {
        if (super.getDescriptor() instanceof INatlCntrsDescriptor) {
            return (INatlCntrsDescriptor) super.getDescriptor();
        }
        logger.debug("AbstractNatlCntrResource.getDescriptor() returning null????");
        return null;
    }

    // this should only be called by resources that are instantiated with an
    // NcMapDescriptor
    protected NCMapDescriptor getNcMapDescriptor() {
        if (getDescriptor() instanceof NCMapDescriptor) {
            return (NCMapDescriptor) getDescriptor();
        }
        logger.debug("GetNcMapDescriptor() returning null????");
        return null;
    }

    //
    public final void initInternal(IGraphicsTarget grphTarget)
            throws VizException {

        if (!initialized) {
            // create the frameDataMap based on the timeFrames from the
            // timeMatcher.
            NCTimeMatcher timeMatcher = (NCTimeMatcher) descriptor
                    .getTimeMatcher();
            List<DataTime> frameTimes = timeMatcher.getFrameTimes();

            for (DataTime frameTime : frameTimes) {
                if (frameTime != null) {
                    AbstractFrameData newFrame = this.createNewFrame(frameTime,
                            resourceData.frameSpan);
                    frameDataMap.put(frameTime.getValidTime().getTime()
                            .getTime(), newFrame);
                }
            }

            // if using a frameInterval to generate the timeline, predict the
            // time for the next frame which will be created if auto updating
            if (!frameTimes.isEmpty()
                    && getResourceData().getTimelineGenMethod() != TimelineGenMethod.USE_DATA_TIMES) {
                nextFrameTime = new DataTime(new Date(frameDataMap.lastKey()),
                        timeMatcher.getFrameInterval());
            }

            ((INatlCntrsDescriptor) descriptor).setFrameTimesForResource(this,
                    frameTimes.toArray(new DataTime[0]));
            // each resource may decide when and how to populate the frameData.
            initResource(grphTarget);
            initialized = true;
        }
    }

    abstract public void initResource(IGraphicsTarget grphTarget)
            throws VizException;

    // don't let derived classes override paintInternal. Override paintFrame()
    public void paintInternal(IGraphicsTarget target, PaintProperties paintProps)
            throws VizException {

        if (!newRscDataObjsQueue.isEmpty()
                || (!newFrameTimesList.isEmpty() && getDescriptor()
                        .isAutoUpdate())) {
            processNewRscDataList();
        }

        if (paintProps == null || paintProps.getDataTime() == null) {
            // should we still call the resource's paintFrame in case it needs
            // to do something even if there is no time?
            return;
        }

        currFrameTime = paintProps.getDataTime();
        AbstractFrameData currFrame = frameDataMap.get(currFrameTime
                .getValidTime().getTime().getTime());

        if (currFrame == null) {
            logger.debug("Unable to find Frame Data for current Time "
                    + currFrameTime);
            return;
        }

        paintFrame(currFrame, target, paintProps);
    }

    protected abstract void paintFrame(AbstractFrameData frameData,
            IGraphicsTarget target, PaintProperties paintProps)
            throws VizException;

    /**
     * Deprecated as part of the Redmine 11819 redesign that has the goal of
     * separating out data loading code from the resource family
     * 
     * A copy of processNewRscDataList exists in the class AbstractDataLoader
     * 
     * This copy is still here as it is called from paintInternal and is thus
     * part of the painting process. The goal is to eventually remove this copy
     * of the method as well
     * 
     * Loop thru newDataObjectsList and update frameDataMap. If a frame for a
     * given record time doesn't exist then create a new Frame
     * 
     * @deprecated
     */
    @Deprecated
    protected synchronized boolean processNewRscDataList() { // boolean isUpdate

        // allow resources to pre process the data before it is added to the
        // frames
        preProcessFrameUpdate();

        NCTimeMatcher timeMatcher = (NCTimeMatcher) descriptor.getTimeMatcher();

        while (!newRscDataObjsQueue.isEmpty()) {
            IRscDataObject rscDataObj = newRscDataObjsQueue.poll();

            boolean foundFrame = false;
            boolean lastFrame = false;

            // loop through the frames and add this record to all that it time
            // matches to
            //
            for (AbstractFrameData frameData : frameDataMap.values()) {
                if (frameData != null) {
                    if (frameData.isRscDataObjInFrame(rscDataObj)) {
                        if (addRscDataToFrame(frameData, rscDataObj)) {
                            // frameData.setPopulated(true);
                            foundFrame = true;
                        }

                        if (frameData == frameDataMap.lastEntry().getValue()) {
                            lastFrame = true;
                        }
                    }
                }
            }

            // if not in any frames (or if in the last frame) and if updating
            // and if this the data is
            // newer than the latest frame then cache the data for auto update.
            // NOTE: this will 'drop' (not cache) data from the initial query
            // that doesn't match the
            // selected timeline which means that if the user later enables
            // auto-update, this data will
            // not be loaded as part of the auto-update. This is by design but
            // could be changed if the
            // user wants a different behaviour.)
            // NOTE: if the data is in the last frame then it may still
            // potentially match the next frame
            // when an update occurs. (WAPITA) (Note2: the auto update code is
            // written to update a frame even
            // if it is not the last frame. (ie. data is received out of order
            // for some reason.) but this
            // will cause any non-dominant data to not be displayed on these
            // frames because we are only
            // checking for the last frame here.)
            long dataTimeMs = getDataTimeMs(rscDataObj);

            if (timeMatcher.isAutoUpdateable()) {

                if ((!foundFrame && autoUpdateReady && dataTimeMs > frameDataMap
                        .firstKey()) || lastFrame) {
                    // if there is the possibility of auto updating (if the
                    // dominant resource
                    // is a satellite or radar image) then store off the data in
                    // the autoUpdateCache
                    // and then update the timeline if auto update is enabled..
                    autoUpdateCache.add(rscDataObj);

                    // if this is the dominantResource, and this data is from a
                    // data update alert then
                    // determine if a new frame is needed.
                    if (isDominantResource() && autoUpdateReady) {
                        newFrameTimesList.addAll(timeMatcher
                                .determineNewFrameTimes(rscDataObj
                                        .getDataTime()));
                    }
                }
            }
        }

        // allow resources to post-process the data after it is added to the
        // frames
        postProcessFrameUpdate();

        autoUpdateReady = false;

        // if there is data in the auto update cache and if auto update is now
        // enabled and if this is the dominant resource for the timeline then
        // we need to update the timeline and process the data in the cache.

        if (!newFrameTimesList.isEmpty() && getDescriptor().isAutoUpdate()) {

            // update the list of times in the timeMatcher.
            // this will then trigger the descriptor to update its datatimes and
            // notify its resources (including this one) to create and update
            // the frames
            timeMatcher.updateTimeline(newFrameTimesList);

            newFrameTimesList.clear();
            // advance to the new last frame (per legacy; could change)...
            descriptor.getFrameCoordinator().changeFrame(
                    IFrameCoordinator.FrameChangeOperation.LAST,
                    IFrameCoordinator.FrameChangeMode.TIME_ONLY);
            // ...and make sure it gets painted
            issueRefresh();
        }

        return true;
    }

    // break out from updateFrames to allow subclasses to override if needed.
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

    protected abstract AbstractFrameData createNewFrame(DataTime frameTime,
            int frameInterval);

    public boolean isDominantResource() {
        NCTimeMatcher timeMatcher = (NCTimeMatcher) descriptor.getTimeMatcher();

        if (timeMatcher != null
                && timeMatcher.getDominantResourceName() != null) {

            String domRscName = timeMatcher.getDominantResourceName()
                    .toString();

            if (domRscName.equals(getResourceData().getResourceName()
                    .toString())) {
                return true;
            }
        }

        return false;
    }

    protected void removeAllNewDataObjects() {
        newRscDataObjsQueue.clear();
    }

    @Override
    public final void remove(DataTime dataTime) {
    }

    public void resourceAttrsModified() {
    }

    // override this if needed
    @Override
    protected void disposeInternal() {
        for (AbstractFrameData frameData : frameDataMap.values()) {
            frameData.dispose();
        }
    }

    @Override
    public String toString() {
        return this.resourceData.toString();
    }

    // added since WarnResource gets null pointers in processNewRscDataList()
    protected long getDataTimeMs(IRscDataObject rscDataObj) {
        return rscDataObj.getDataTime().getValidTime().getTime().getTime();

    }

    // set the populated flag in all frames
    // this is done when the data for all frames has been queried at one time
    // as opposed to populating as each frame is displayed.
    protected void setAllFramesAsPopulated() {
        for (AbstractFrameData frameData : frameDataMap.values()) {
            frameData.setPopulated(true);
        }
    }
}
