package gov.noaa.nws.ncep.viz.resources.attributes;

import gov.noaa.nws.ncep.viz.resources.INatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResourceData;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;

import java.lang.reflect.Constructor;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
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
 * 12/01/201520   R12953       RReynolds    Refactored title string of attribute dialog box
 * 
 * </pre>
 * 
 * @author ghull
 * @version 1
 */
public class EditResourceAttrsAction extends AbstractRightClickAction {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(EditResourceAttrsAction.class);

    private ResourceExtPointMngr rscExtPointMngr = null;

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.action.Action#run()
     */
    @Override
    public void run() {
        // add sanity check that this is an INatlCntrsResource
        Shell shell = NcDisplayMngr.getCaveShell();

        if (selectedRsc.getResource() instanceof INatlCntrsResource) {
            // Popup with the Apply button
            if (PopupEditAttrsDialog(shell,
                    (INatlCntrsResourceData) selectedRsc.getResourceData(),
                    true)) {
                ((INatlCntrsResource) selectedRsc.getResource())
                        .resourceAttrsModified();
            }
            getContainer().refresh();

        } else
            statusHandler.handle(Priority.INFO,
                    "Edit Attr sanity check: Resource is not a NC Resource.");
    }

    // TODO : This method is also called by the RBD Manager. Consider moving to
    // another class.

    @SuppressWarnings("unchecked")
    public boolean PopupEditAttrsDialog(Shell sh,
            INatlCntrsResourceData rscData, Boolean applyBtn) {

        if (rscExtPointMngr == null) {
            rscExtPointMngr = ResourceExtPointMngr.getInstance();
        }

        if (rscData == null) {
            statusHandler.handle(Priority.INFO,
                    "Resource is null in PopupEditAttrsDialog");
            System.out.println("Resource is null in PopupEditAttrsDialog");
            return false;
        }
        AbstractEditResourceAttrsDialog editAttrsDlg = null;

        Class<?> editDlgClass = rscExtPointMngr.getResourceDialogClass(rscData
                .getResourceName());

        // this should be a sanity check since the Edit button should be
        // desensitized if this
        // resource doesn't extent the Abstract Dialog class
        if (editDlgClass == null) {
            MessageDialog msgDlg = new MessageDialog(
                    NcDisplayMngr.getCaveShell(), "Info", null, "Resource "
                            + rscData.getResourceName().toString()
                            + " is not editable", MessageDialog.INFORMATION,
                    new String[] { "OK" }, 0);
            msgDlg.open();

            return false;
        }
        Constructor<?> constr;

        try {
            constr = editDlgClass.getConstructor(new Class[] { Shell.class,
                    INatlCntrsResourceData.class, Boolean.class });
            editAttrsDlg = (AbstractEditResourceAttrsDialog) constr
                    .newInstance(sh, rscData, applyBtn);

            editAttrsDlg.dlgTitle = "Edit "
                    + selectedRsc.getResource().getName().toString()
                    + " Attributes";

            if (!editAttrsDlg.isOpen()) {
                editAttrsDlg.open();
                return editAttrsDlg.ok;
            }

        } catch (Exception e) {
            statusHandler.handle(Priority.ERROR,
                    "Error instantiating Edit Dialog for Resource: "
                            + rscData.getResourceName().toString()
                            + " Dialog class is: " + editDlgClass);
        }

        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.action.Action#getText()
     */
    @SuppressWarnings("unchecked")
    public String getText() {
        return "Edit " + selectedRsc.getResource().getName().toString()
                + " Attributes";
    }

    @Override
    public void setSelectedRsc(ResourcePair selectedRsc) {
        super.setSelectedRsc(selectedRsc);
    }
}
