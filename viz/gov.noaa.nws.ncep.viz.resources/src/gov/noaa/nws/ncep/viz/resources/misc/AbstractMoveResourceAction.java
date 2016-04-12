/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 * 
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 * 
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 * 
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package gov.noaa.nws.ncep.viz.resources.misc;

import gov.noaa.nws.ncep.viz.resources.groupresource.GroupResource;

import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.rsc.ResourceList.MoveOperation;
import com.raytheon.viz.ui.cmenu.AbstractRightClickAction;

/**
 * Abstract right-click action class for moving resources within the legend
 * list. If the selected resource is part of a group, the resource will be moved
 * within the group only.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 20, 2015  #12870    dgilling     Initial creation
 * 
 * </pre>
 * 
 * @author dgilling
 * @version 1.0
 */

abstract class AbstractMoveResourceAction extends AbstractRightClickAction {

    @Override
    public final void run() {
        IDescriptor desc = getDescriptor();

        for (ResourcePair pair : desc.getResourceList()) {
            if (selectedRsc == pair) {
                desc.getResourceList().moveResource(
                        getTopMostSelectedResource(), getMoveOperation());
                break;
            } else if (pair.getResource() instanceof GroupResource
                    && ((GroupResource) pair.getResource()).getResourceList()
                            .contains(selectedRsc)) {
                ((GroupResource) pair.getResource()).getResourceList()
                        .moveResource(selectedRsc.getResource(),
                                getMoveOperation());
            }
        }
    }

    @Override
    public abstract String getText();

    abstract MoveOperation getMoveOperation();
}
