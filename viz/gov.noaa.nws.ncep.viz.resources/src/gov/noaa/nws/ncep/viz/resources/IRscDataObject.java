package gov.noaa.nws.ncep.viz.resources;

import com.raytheon.uf.common.time.DataTime;

/**
 * IRscDataObject is part of the Resource classes redesign as specified
 * in Redmine 11819.  For more details about that redesign please see that
 * ticket and the title block comment of 
 * {@link gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource2
 * 
 * Before the redesign IRscDataObject was an inner class of
 * AbstractNatlCntrsResource.  It was put into its own file in the same 
 * package to enable collections generically typed to AbstractFrameData to
 * be passed into classes of the new design.
 * 
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 11/03/2015   R 11819    srussell    Initial creation
 * 
 * </pre>
 * 
 * @author srussell
 * @version 1.0
 */

public interface IRscDataObject {
    abstract DataTime getDataTime();

}
