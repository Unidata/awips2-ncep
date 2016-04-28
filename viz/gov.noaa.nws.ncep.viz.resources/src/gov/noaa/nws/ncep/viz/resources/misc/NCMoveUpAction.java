/*
 * gov.noaa.nws.ncep.viz.resources.misc
 * 
 * 13 August 2014
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.viz.resources.misc;

import com.raytheon.uf.viz.core.rsc.ResourceList.MoveOperation;

/**
 * 
 * Move up a resource in the legend list. It works for resources in a group
 * resource.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 08/14            ?        B. Yin    Initial Creation.
 * Nov 20, 2015  #12870    dgilling    Re-implement based on AbstractMoveResourceAction.
 * 
 * </pre>
 * 
 * @author byin
 * @version 1
 */
public class NCMoveUpAction extends AbstractMoveResourceAction {

    @Override
    public String getText() {
        return "Move Up";
    }

    @Override
    MoveOperation getMoveOperation() {
        return MoveOperation.Up;
    }
}
