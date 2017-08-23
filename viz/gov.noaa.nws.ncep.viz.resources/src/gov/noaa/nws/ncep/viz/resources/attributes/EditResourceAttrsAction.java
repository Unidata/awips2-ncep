package gov.noaa.nws.ncep.viz.resources.attributes;

import gov.noaa.nws.ncep.viz.resources.INatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResourceData;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;

import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.viz.ui.cmenu.AbstractRightClickAction;

/**
 * This class is instantiated by Raytheon core code when the context Menu is
 * activated from the legends.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 06 May 2009    #115         ghull        Initial Creation.
 * 26 Jul 2009                 ghull        Migrate to to11
 * 03 Aug 2009                 ghull        pass parent shell to the Dialog (for RBD Mngr)
 * 05 Jan 2010                 ghull        Use ResourceExtPointMngr
 * 27 Apr 2010    #245         ghull        Added Apply Button
 * 20 May 2010    to11dr11     ghull        selectedRsc was changed to ResourcePair
 * 21 Sep 2011                 ghull        move getting ResourceExtPointMngr instance out of constructor since
 *                                          D2D instantiates this class in populating the contextMenu
 * 12/01/2015     R12953       RReynolds    Refactored title string of attribute dialog box
 * 01/01/2016     R14142       RCReynolds   Refactored so title string can be passed from both Legend and Edit Button.
 * 04/05/2016     R15715       dgilling     Refactored out PopupEditAttrsDialog and associated methods.
 * 
 * </pre>
 * 
 * @author ghull
 * @version 1
 */
public class EditResourceAttrsAction extends AbstractRightClickAction {

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(getClass());

    @Override
    public void run() {
        Shell shell = NcDisplayMngr.getCaveShell();

        if (getSelectedRsc() instanceof INatlCntrsResource) {
            // Popup with the Apply button
            EditResourceAttrsDialogFactory factory = new EditResourceAttrsDialogFactory()
                    .setShell(shell)
                    .setResourceData(
                            (INatlCntrsResourceData) getSelectedRsc()
                                    .getResourceData())
                    .setCapabilities(getSelectedRsc().getCapabilities())
                    .setApplyBtn(true).setTitle(getText());
            if (factory.construct()) {
                ((INatlCntrsResource) getSelectedRsc()).resourceAttrsModified();
            }
            getContainer().refresh();

        } else {
            statusHandler
                    .info("Edit Attr sanity check: Resource is not a NC Resource.");
        }
    }

    @Override
    public String getText() {
        return String.format("Edit %s Attributes", getSelectedRsc().getName());
    }
}
