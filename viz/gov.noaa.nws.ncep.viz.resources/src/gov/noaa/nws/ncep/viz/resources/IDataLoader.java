package gov.noaa.nws.ncep.viz.resources;

import gov.noaa.nws.ncep.viz.common.display.INatlCntrsDescriptor;

import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * IDataLoader is part of the Resource classes redesign as specified
 * in Redmine 11819.  For more details about that redesign please see that
 * ticket and the title block comment of 
 * {@link gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource2
 * 
 * IDataLoader is an Interface that serves as a template for all *DataLoader
 * classes.  These classes will do the preloading ( and eventually all data
 * loading ) of data for the resources.  Each resource will have its own
 * unique *DataLoader class to call in initResources to preload data for it,
 * only needing to call the method loadData()
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 10/19/2015   R 11819    srussell     Initial creation
 * 
 * </pre>
 * 
 * @author srussell
 * @version 1.0
 */

public interface IDataLoader {

    /**
     * This is the function that Resource classes will call in their
     * initResource() methods to preload data. That is, put all of the existing
     * data into the appropriate frames after the Resource Manager is closed via
     * the "Load and Close" and "Load" buttons.
     * 
     */
    public abstract void loadData();

    public abstract void setFrameDataMap(
            TreeMap<Long, AbstractFrameData> frameDataMap);

    public abstract void setNewRscDataQueue(
            ConcurrentLinkedQueue<IRscDataObject> newRscDataObjsQueue);

    public abstract void setResourceData(
            AbstractNatlCntrsRequestableResourceData resourceData);

    public abstract void setDescriptor(INatlCntrsDescriptor descriptor);

}
