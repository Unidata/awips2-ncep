package gov.noaa.nws.ncep.viz.tools.plotModelMngr;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Popup Point data display manager dialog in National Centers perspective.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 2009                 M. Li    	Initial creation. 
 * Dec. 2009                Greg Hull   change name to plot model editor. Don't refresh mapEditor
 * 03/29/2016   R7567       A. Su       Added a check to prevent more than one dialog to open.
 * 
 * </pre>
 * 
 * @author mli
 * @version 1.0
 * 
 */

public class PlotModelMngrAction extends AbstractHandler {

    private static PlotModelMngrDialog plotModelMngrDialog;

    public Object execute(ExecutionEvent arg0) throws ExecutionException {

        // Allow only one dialog to open.
        if (plotModelMngrDialog != null)
            return null;

        try {
            Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                    .getShell();
            if (plotModelMngrDialog == null)
                plotModelMngrDialog = new PlotModelMngrDialog(shell,
                        arg0.getParameter("perspective"));

            if (!plotModelMngrDialog.isOpen())
                plotModelMngrDialog.open();
        } finally {
            plotModelMngrDialog = null;
        }

        return null;
    }
}
