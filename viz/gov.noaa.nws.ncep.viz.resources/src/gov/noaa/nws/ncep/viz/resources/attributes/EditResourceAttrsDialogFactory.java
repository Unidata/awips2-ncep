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
package gov.noaa.nws.ncep.viz.resources.attributes;

import gov.noaa.nws.ncep.viz.resources.INatlCntrsResourceData;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;

import java.lang.reflect.Constructor;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.rsc.capabilities.Capabilities;

/**
 * Factory class to display the appropriate dialog to edit the given resource's
 * attributes using the gov.noaa.nws.ncep.viz.resources.NC-Resource extension
 * point.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 05, 2016  R15715    dgilling     Initial creation
 * 
 * </pre>
 * 
 * @author dgilling
 * @version 1.0
 */

public final class EditResourceAttrsDialogFactory {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(EditResourceAttrsDialogFactory.class);

    private final ResourceExtPointMngr rscExtPointMngr;

    private Shell parentShell;

    private String title;

    private INatlCntrsResourceData resourceData;

    private boolean applyBtn;

    private Capabilities capabilities;

    public EditResourceAttrsDialogFactory() {
        this.rscExtPointMngr = ResourceExtPointMngr.getInstance();
        this.parentShell = null;
        this.title = StringUtils.EMPTY;
        this.resourceData = null;
        this.applyBtn = false;
        this.capabilities = null;
    }

    public EditResourceAttrsDialogFactory setShell(Shell shell) {
        parentShell = shell;
        return this;
    }

    public EditResourceAttrsDialogFactory setTitle(String title) {
        this.title = title;
        return this;
    }

    public EditResourceAttrsDialogFactory setResourceData(
            INatlCntrsResourceData resourceData) {
        this.resourceData = resourceData;
        return this;
    }

    public EditResourceAttrsDialogFactory setCapabilities(
            Capabilities capabilities) {
        this.capabilities = capabilities;
        return this;
    }

    public EditResourceAttrsDialogFactory setApplyBtn(boolean applyBtn) {
        this.applyBtn = applyBtn;
        return this;
    }

    public boolean construct() {
        if (parentShell == null) {
            statusHandler.warn("Parent shell is null in PopupEditAttrsDialog");
            return false;
        }
        if (resourceData == null) {
            statusHandler.warn("Resource is null in PopupEditAttrsDialog");
            return false;
        }
        if (capabilities == null) {
            statusHandler
                    .warn("Resource capabilities are null in PopupEditAttrsDialog");
            return false;
        }

        Class<?> editDlgClass = rscExtPointMngr
                .getResourceDialogClass(resourceData.getResourceName());
        if (editDlgClass == null) {
            MessageDialog msgDlg = new MessageDialog(
                    NcDisplayMngr.getCaveShell(), "Info", null, "Resource "
                            + resourceData.getResourceName().toString()
                            + " is not editable", MessageDialog.INFORMATION,
                    new String[] { "OK" }, 0);
            msgDlg.open();
            return false;
        }

        try {
            Constructor<?> constr = editDlgClass.getConstructor(new Class[] {
                    Shell.class, INatlCntrsResourceData.class,
                    Capabilities.class, Boolean.class });
            AbstractEditResourceAttrsDialog editAttrsDlg = (AbstractEditResourceAttrsDialog) constr
                    .newInstance(parentShell, resourceData, capabilities,
                            Boolean.valueOf(applyBtn));
            if (!title.isEmpty()) {
                editAttrsDlg.dlgTitle = title;
            }

            if (!editAttrsDlg.isOpen()) {
                editAttrsDlg.open();
                return editAttrsDlg.ok;
            }
        } catch (Exception e) {
            String msg = String.format(
                    "Error instantiating dialog class %s for resource %s",
                    editDlgClass, resourceData.getResourceName());
            statusHandler.error(msg, e);
        }

        return false;
    }
}
