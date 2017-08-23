/*
 * gov.noaa.nws.ncep.ui.pgen.tools.PgenVolcanoCreateTool
 * 
 * May 2010
 *
 * This code has been developed by the SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.viz.tools.wipe;

import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData;
import gov.noaa.nws.ncep.viz.resources.groupresource.GroupResourceData;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;
import gov.noaa.nws.ncep.viz.ui.display.NcEditorUtil;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.rsc.AbstractResourceData;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.ResourceList;
import com.raytheon.viz.ui.editor.AbstractEditor;

//import gov.noaa.nws.ncep.viz.overlays.resources.*;

/**
 * The class for unloading all but overlay data
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date       	Ticket#		Engineer	Description
 * ------------	----------	-----------	--------------------------
 * 05/10		#265		G. Zhang   	Initial Creation.
 * 05/10        #265        G. Hull     test for AbstractNatlCntrsRequestableResourceData to
 *                                      remove PGEN dependency
 * 02/13        #972        G. Hull     setDisplayModified flag
 * 09/14        ?           B. Yin      Removed GroupResource
 * 10/20/2016   R20700     pmoyer       Implemented redraw of GUI to allow for updating of 
 *                                      FadeDisplay control state when resources are deleted.
 * 
 * </pre>
 * 
 * @author G. Zhang
 */
public class WipeResultsAction extends AbstractHandler {

    private Shell shell = null;

    private WipeDialog wDlg = null;

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands
     * .ExecutionEvent)
     */
    public Object execute(ExecutionEvent arg0) throws ExecutionException {

        // Pop up a Message Box for confirmation

        shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        wDlg = new WipeDialog(shell);
        Object userResponse = wDlg.open();

        // if user choose OK, we proceed to delete data
        if (userResponse instanceof Boolean) {
            if (((Boolean) userResponse).booleanValue()) {

                // delete all, but overlays and the basic map;
                // based on NmapUiUtil's findResource().

                AbstractEditor editor = NcDisplayMngr
                        .getActiveNatlCntrsEditor();

                if (editor == null)
                    return null;

                IRenderableDisplay disp = editor.getActiveDisplayPane()
                        .getRenderableDisplay();
                if (disp == null)
                    return null;

                IDescriptor idtor = disp.getDescriptor();
                if (idtor == null)
                    return null;
                ResourceList rscList = idtor.getResourceList();

                List<ResourcePair> rmList = new ArrayList<ResourcePair>();

                for (ResourcePair rp : rscList) {

                    if (rp != null && isRemovable(rp.getResource())) {
                        rmList.add(rp);
                    }
                }

                rscList.removeAll(rmList); // rscList.clear() removes
                                           // everything.

                NcEditorUtil.setDisplayAvailable(editor, true);

                editor.refresh();
                NcEditorUtil.refreshGUIElements(editor);

            }
        } else {
            return null;
        }

        // mapEditor.refresh();

        return null;
    }

    /*
     * all but overlays and basic geo-political map remains
     */
    private boolean isRemovable(AbstractVizResource avr) {

        if (avr == null)
            return false;

        AbstractResourceData ard = avr.getResourceData();
        if (ard == null)
            return false;

        if (ard instanceof AbstractNatlCntrsRequestableResourceData
                || ard instanceof GroupResourceData)
            return true;
        else
            return false;
    }

}
