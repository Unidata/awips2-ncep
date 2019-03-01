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
package gov.noaa.nws.ncep.viz.overlays;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.raytheon.viz.ui.tools.AbstractTool;

import gov.noaa.nws.ncep.viz.overlays.dialogs.PgenStaticOverlayDialog;

/**
 *
 * Handler for creating the PGEN Static Overlay Dialog
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 1, 2019  7752       tjensen     Initial creation
 *
 * </pre>
 *
 * @author tjensen
 */
public class PgenStaticOverlayHandler extends AbstractTool {

    private PgenStaticOverlayDialog dialog;

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Shell shell = HandlerUtil.getActiveShell(event);

        if (dialog == null || dialog.getShell() == null
                || dialog.isDisposed()) {
            dialog = new PgenStaticOverlayDialog(shell);
            dialog.open();
        } else {
            dialog.bringToTop();
        }

        return null;
    }

}