package gov.noaa.nws.ncep.viz.resources;

import gov.noaa.nws.ncep.viz.common.display.INatlCntrsDescriptor;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataquery.requests.DbQueryRequest;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.dataquery.responses.DbQueryResponse;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.requests.ThriftClient;

/**
 * <pre>
 * 
 * AbstractDataLoader is part of the redesign of the *Resource classes as
 * described in Redmine 11819, Redmine 8830, and in the title block comment
 * of
 * {@link gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource2
 * 
 * This class holds the default data loading methods previously housed in
 * AbstractNatlCntrsResource.  This class is to be extended into resource
 * specific *DataLoader classes that will handle data loading for specific
 * resources.
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 10/21/2015   R 11819      S Russell  Initial Creation.
 * 
 * </pre>
 * 
 * @author S Russell
 * @version 1
 */

public abstract class AbstractDataLoader implements IDataLoader {

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

    // Map from a frame time string to a structure containing
    // displayable data elements for that frame...
    protected TreeMap<Long, AbstractFrameData> frameDataMap;

    @Override
    public void setFrameDataMap(TreeMap<Long, AbstractFrameData> frameDataMap) {
        this.frameDataMap = frameDataMap;

    }

    @Override
    public void setNewRscDataQueue(
            ConcurrentLinkedQueue<IRscDataObject> newRscDataObjsQueue) {
        this.newRscDataObjsQueue = newRscDataObjsQueue;

    }

    /**
     * * This method can be used as a convenience if the MetadataMap constraints
     * are all that is needed to query the data.
     * 
     * @throws VizException
     */
    public void queryRecords() throws VizException {

        HashMap<String, RequestConstraint> queryList = new HashMap<String, RequestConstraint>(
                resourceData.getMetadataMap());

        queryRecords(queryList);
    }

    /**
     * 
     * This method is used for querying data when you need a customized list of
     * query restraints
     * 
     * @param <String, RequestConstraint> requestConstraints
     * @throws VizException
     */

    public void queryRecords(
            HashMap<String, RequestConstraint> requestConstraints)
            throws VizException {

        DbQueryRequest request = new DbQueryRequest();
        request.setConstraints(requestConstraints);

        DbQueryResponse response = (DbQueryResponse) ThriftClient
                .sendRequest(request);

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
            System.out
                    .println("Resource Impl "
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

    // loop thru newDataObjectsList and update frameDataMap. If a frame for a
    // given record time doesn't exist then create a new Frame
    protected synchronized boolean processNewRscDataList() {

        // allow resources to pre process the data before it is added to the
        // frames
        preProcessFrameUpdate();

        // NCTimeMatcher timeMatcher = (NCTimeMatcher)
        // descriptor.getTimeMatcher();

        boolean foundFrame = false;
        boolean lastFrame = false;

        while (!newRscDataObjsQueue.isEmpty()) {
            IRscDataObject rscDataObj = newRscDataObjsQueue.poll();

            foundFrame = false;
            lastFrame = false;

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

            // allow resources to post-process the data after it is added to the
            // frames
            postProcessFrameUpdate();
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

    // set the populated flag in all frames
    // this is done when the data for all frames has been queried at one time
    // as opposed to populating as each frame is displayed.
    protected void setAllFramesAsPopulated() {
        for (AbstractFrameData frameData : frameDataMap.values()) {
            frameData.setPopulated(true);
        }
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
