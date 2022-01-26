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

package gov.noaa.nws.ncep.ui.pgen.tools;

import com.raytheon.uf.viz.core.rsc.IInputHandler;

/**
*
* Add New label to contour line in the PGEN tool.
*
* <pre>
*
* SOFTWARE HISTORY
*
* Date          Ticket#  Engineer  Description
* ------------- -------- --------- -----------------
* Sep 01, 2020   81798    smanoj    Initial creation
* 
* 
* </pre>
*
* @author smanoj
*/
public class PgenAddNewLabel extends AbstractPgenTool {

    /**
     * Input handler for mouse events.
     */
    protected PgenAddNewLabelHandler addNewLblHandler = null;

    public PgenAddNewLabel() {
        super();
    }

    /**
     * Returns the current mouse handler.
     * 
     * @return
     */
    public IInputHandler getMouseHandler() {

        if (this.addNewLblHandler == null
                || this.mapEditor != ((PgenAddNewLabelHandler) addNewLblHandler)
                        .getMapEditor()
                || this.drawingLayer != ((PgenAddNewLabelHandler) addNewLblHandler)
                        .getPgenrsc()) {
            this.addNewLblHandler = new PgenAddNewLabelHandler(this);
        }

        return this.addNewLblHandler;
    }
}