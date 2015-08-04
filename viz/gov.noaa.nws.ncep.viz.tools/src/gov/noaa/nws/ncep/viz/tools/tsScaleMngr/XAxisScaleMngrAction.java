/**
 * This code has unlimited rights, and is provided "as is" by the National Centers 
 * for Environmental Prediction, without warranty of any kind, either expressed or implied, 
 * including but not limited to the implied warranties of merchantability and/or fitness 
 * for a particular purpose.
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 **/
package gov.noaa.nws.ncep.viz.tools.tsScaleMngr;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Popup X-axis Scale manager dialog in National Centers perspective.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 09/2014      R4875      sgurung     Initial creation
 * 
 * </pre>
 * 
 * @author sgurung
 * @version 1
 * 
 */
public class XAxisScaleMngrAction extends AbstractHandler {

    private static XAxisScaleMngrDialog xAxisScaleMngr;

    public Object execute(ExecutionEvent arg0) throws ExecutionException {

        try {
            Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                    .getShell();
            if (xAxisScaleMngr == null)
                xAxisScaleMngr = new XAxisScaleMngrDialog(shell);

            if (!xAxisScaleMngr.isOpen())
                xAxisScaleMngr.open();
        } finally {
            xAxisScaleMngr = null;
        }

        return null;
    }
}
