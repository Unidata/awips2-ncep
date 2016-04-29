package gov.noaa.nws.ncep.viz.resources.misc;

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

import com.raytheon.uf.viz.core.rsc.ResourceList.MoveOperation;

/**
 * 
 * Move down a resource in the legend list. It works for resources in a group
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
public class NCMoveDownAction extends AbstractMoveResourceAction {

    @Override
    public String getText() {
        return "Move Down";
    }

    @Override
    MoveOperation getMoveOperation() {
        return MoveOperation.Down;
    }
}
