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
 * in AbstractNatlCntrsResource.  AbstractDataLoader will also implement the
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
 * those data type/input argument issues and to sever the dependencies on
 * AbstractNatlCntrsResource.  This class has had the data loading code removed,
 * with plans to remove more as is possible.  AbstractNatlCntrsResource2 also
 * shows what AbstractNatlCntrsResource would eventually look like with the
 * 38 child resources classes converted over.  When that point is reached this
 * class can be swapped out for that.  
 * 
 * That is a considerable amount of work so this class exists to make the
 * transition more graceful and convenient.
 * 
 * Implementing the new design for any given resource is a significant amount
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
 * 03/07/2016   R10155      B Hebbard       In order to handle auto-update fully,
 *                                          move update-related code from this
 *                                          class over to AbstractDataLoader.
 *                                          
 * 
 * @author ghull (original), srussell ( this truncated version )
 * @version 1
 */
public abstract class AbstractNatlCntrsResource2<T extends AbstractNatlCntrsRequestableResourceData, D extends INatlCntrsDescriptor>
        extends
        AbstractVizResource<AbstractNatlCntrsRequestableResourceData, IDescriptor>
        implements INatlCntrsResource {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(AbstractNatlCntrsResource2.class);

    // Has this resource been initialized yet?
    protected boolean initialized = false;

    // The Data Loader object associated with this resource, whose job is to
    // handle loading of both initial data (at resource initialization) and
    // subsequent updates (data arriving after the resource is loaded).
    protected IDataLoader dataLoader;

    // Map from a frame time string to a structure containing data elements for
    // that frame
    protected TreeMap<Long, AbstractFrameData> frameDataMap;

    // This list is populated by the initial query and also by the auto update
    // when new data is ingested for this resource. These objects time matched
    // to one or more frames.
    protected ConcurrentLinkedQueue<IRscDataObject> newRscDataObjsQueue;

    protected DataTime currFrameTime;

    protected AbstractNatlCntrsResource2(T resourceData, LoadProperties props) {
        super(resourceData, props);
        frameDataMap = new TreeMap<Long, AbstractFrameData>();
        newRscDataObjsQueue = new ConcurrentLinkedQueue<IRscDataObject>();

        currFrameTime = null;

        // If requestable, add a resourceChanged listener that is called by
        // Raytheon's AbstractRequestableResource when update() is called.
        if (resourceData instanceof AbstractNatlCntrsRequestableResourceData) {
            resourceData.addChangeListener(new IResourceDataChanged() {
                @Override
                public void resourceChanged(ChangeType type, Object object) {

                    if (object == null) {
                        statusHandler
                                .debug("resourceChanged called with null object for: "
                                        + getResourceData().getResourceName()
                                                .toString());
                        return;
                    }

                    // TODO : need to make sure that these are the same types of
                    // objects that are returned by the queryRecords method (or
                    // the method that the resource uses to populate the
                    // frameData).
                    if (type == ChangeType.DATA_UPDATE) {
                        // New incoming data received; send to data loader for
                        // handling
                        dataLoader.resourceDataUpdate(object);
                    }
                }
            });
        }
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
     * future use of processRecord() should be done with that copy.
     * 
     * @deprecated
     */
    @Deprecated
    protected IRscDataObject[] processRecord(Object pdo) {
        if (!(pdo instanceof PluginDataObject)) {
            statusHandler
                    .debug("Resource Impl "
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
        statusHandler
                .debug("AbstractNatlCntrResource.getDescriptor() returning null????");
        return null;
    }

    // this should only be called by resources that are instantiated with an
    // NcMapDescriptor
    protected NCMapDescriptor getNcMapDescriptor() {
        if (getDescriptor() instanceof NCMapDescriptor) {
            return (NCMapDescriptor) getDescriptor();
        }
        statusHandler.debug("GetNcMapDescriptor() returning null????");
        return null;
    }

    protected abstract AbstractFrameData createNewFrame(DataTime frameTime,
            int timeInt);

    //
    public final void initInternal(IGraphicsTarget grphTarget)
            throws VizException {

        if (!initialized) {
            // create the data loader object
            dataLoader = resourceData.getDataLoader();
            dataLoader.setFrameDataMap(frameDataMap);
            dataLoader.setResource(this);
            dataLoader.setResourceData(resourceData);
            dataLoader.setNewRscDataQueue(newRscDataObjsQueue);
            dataLoader.setDescriptor(this.getDescriptor());

            // create the frameDataMap based on the timeFrames from the
            // timeMatcher
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
                DataTime nextFrameTime = new DataTime(new Date(
                        frameDataMap.lastKey()), timeMatcher.getFrameInterval());
                dataLoader.setNextFrameTime(nextFrameTime);
            }

            ((INatlCntrsDescriptor) descriptor).setFrameTimesForResource(this,
                    frameTimes.toArray(new DataTime[0]));
            // each resource may decide when and how to populate the frameData
            initResource(grphTarget);
            initialized = true;
        }
    }

    abstract public void initResource(IGraphicsTarget grphTarget)
            throws VizException;

    // Don't let derived classes override paintInternal. Override paintFrame()
    public void paintInternal(IGraphicsTarget target, PaintProperties paintProps)
            throws VizException {

        dataLoader.processAnyNewData();

        if (paintProps == null || paintProps.getDataTime() == null) {
            // Should we still call the resource's paintFrame in case it needs
            // to do something even if there is no time?
            return;
        }

        currFrameTime = paintProps.getDataTime();
        AbstractFrameData currFrame = frameDataMap.get(currFrameTime
                .getValidTime().getTime().getTime());

        if (currFrame == null) {
            statusHandler.debug("Unable to find Frame Data for current Time "
                    + currFrameTime);
            return;
        }

        paintFrame(currFrame, target, paintProps);
    }

    protected abstract void paintFrame(AbstractFrameData frameData,
            IGraphicsTarget target, PaintProperties paintProps)
            throws VizException;

    // allow this to be overridden if derived class needs to
    protected boolean preProcessFrameUpdate() {
        return true;
    }

    // allow this to be overridden if derived class needs to
    protected boolean postProcessFrameUpdate() {
        return true;
    }

    public Boolean updateTimeline() {
        // Resource receives this request; delegate to DataLoader
        return dataLoader.updateTimeline();
    }

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

    // Set the populated flag in all frames. This is done when the data for all
    // frames has been queried at one time as opposed to populating as each
    // frame is displayed.
    protected void setAllFramesAsPopulated() {
        for (AbstractFrameData frameData : frameDataMap.values()) {
            frameData.setPopulated(true);
        }
    }
}
