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
package gov.noaa.nws.ncep.ui.nsharp.view;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.viz.ui.dialogs.CaveJFACEDialog;

import gov.noaa.nws.ncep.ui.nsharp.NsharpConfigManager;
import gov.noaa.nws.ncep.ui.nsharp.NsharpConfigStore;

/**
 * An abstract base class for a dialog that contains common patterns used by the
 * Nsharp configuration dialogs.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 17, 2018  #7081     dgilling     Initial creation
 *
 * </pre>
 *
 * @author dgilling
 */

public abstract class AbstractNsharpConfigDlg extends CaveJFACEDialog {

    protected final IUFStatusHandler statusHandler = UFStatus
            .getHandler(getClass());

    private static final int APPLY_ID = IDialogConstants.CLIENT_ID + 1;

    private static final int SAVE_ID = IDialogConstants.CLIENT_ID + 2;

    protected final NsharpConfigManager mgr;

    protected final NsharpConfigStore configStore;

    private final String title;

    protected AbstractNsharpConfigDlg(Shell parentShell, String title) {
        super(parentShell);

        this.title = title;

        this.mgr = NsharpConfigManager.getInstance();
        this.configStore = mgr.retrieveNsharpConfigStoreFromFs();
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, APPLY_ID, "Apply", false);

        createButton(parent, SAVE_ID, "Save", false);

        createButton(parent, IDialogConstants.CLOSE_ID,
                IDialogConstants.CLOSE_LABEL, false);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(title);
    }

    @Override
    protected void buttonPressed(int buttonId) {
        switch (buttonId) {
        case APPLY_ID:
            handleApplyClicked();
            break;
        case SAVE_ID:
            handleSaveClicked();
            break;
        case IDialogConstants.CLOSE_ID:
            handleCloseClicked();
            break;
        }
    }

    protected void handleApplyClicked() {

    }

    protected void handleSaveClicked() {

    }

    protected void handleCloseClicked() {
        close();
    }
}
