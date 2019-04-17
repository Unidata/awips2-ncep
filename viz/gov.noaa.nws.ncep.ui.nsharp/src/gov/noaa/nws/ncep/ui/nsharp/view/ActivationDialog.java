/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract EA133W-17-CQ-0082 with the US Government.
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

import gov.noaa.nws.ncep.ui.nsharp.NsharpConstants;
import gov.noaa.nws.ncep.ui.nsharp.NsharpConstants.ActState;
import gov.noaa.nws.ncep.ui.nsharp.NsharpOperationElement;
import gov.noaa.nws.ncep.ui.nsharp.display.NsharpEditor;
import gov.noaa.nws.ncep.ui.nsharp.display.rsc.NsharpResourceHandler;

import java.util.Collections;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

/**
 * 
 * Dialog which allows users to change the {@link ActState} of
 * {@link NsharpOperationElement}s.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------------------------------
 * Nov 13, 2018  7576     bsteffen  Merge three activation dialogs into one.
 * Dec 14, 2018  6872     bsteffen  Rewrite NsharpOperationElement
 *
 * </pre>
 *
 * @author bsteffen
 */
public class ActivationDialog extends Dialog {

    private final String elementDesc;

    private final List<? extends NsharpOperationElement> elementList;

    private final int currentIndex;

    private org.eclipse.swt.widgets.List selectionList;

    /**
     * Construct a new ActivationDialog
     * 
     * @param parentShell
     *            the parent shell.
     * @param elementDesc
     *            User friendly name for elements that is presented in title and
     *            errors.
     * @param elementList
     *            The list of elements to choose from.
     * @param currentIndex
     *            The currently selected index, this cannot be deactivated.
     */
    protected ActivationDialog(Shell parentShell, String elementDesc,
            List<? extends NsharpOperationElement> elementList,
            int currentIndex) {
        super(parentShell);
        this.elementDesc = elementDesc;
        this.elementList = elementList;
        this.currentIndex = currentIndex;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(elementDesc + " Configuration");
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.CANCEL_ID,
                IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite top = (Composite) super.createDialogArea(parent);

        GridLayout mainLayout = new GridLayout(1, false);
        mainLayout.marginHeight = 3;
        mainLayout.marginWidth = 3;

        top.setLayout(mainLayout);

        fillDialogArea(top);
        populateSelectionList();

        return top;
    }

    private void fillDialogArea(Composite parent) {
        Group sndListGp = new Group(parent, SWT.NONE);
        selectionList = new org.eclipse.swt.widgets.List(sndListGp,
                SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
        selectionList.setBounds(0, 0, 2 * NsharpConstants.listWidth,
                NsharpConstants.listHeight * 4);
        selectionList.addListener(SWT.Selection, (e) -> validateSelection());

        Group buttonGp = new Group(parent, SWT.NONE);
        buttonGp.setLayout(new GridLayout(2, false));

        Button activateBtn = new Button(buttonGp, SWT.PUSH);
        activateBtn.setText("Activate");
        activateBtn.addListener(SWT.Selection,
                (e) -> changeState(ActState.ACTIVE));
        Button deactivateBtn = new Button(buttonGp, SWT.PUSH);
        deactivateBtn.setText("DeActivate");
        deactivateBtn.addListener(SWT.Selection,
                (e) -> changeState(ActState.INACTIVE));
    }

    private void populateSelectionList() {
        for (NsharpOperationElement elem : elementList) {
            StringBuilder label = new StringBuilder(elem.getDescription());
            if (elem.getActionState() == NsharpConstants.ActState.INACTIVE)
                label.append("--(InActive)");
            else {
                if (selectionList.getItemCount() == currentIndex) {
                    label.append("--(Active-Current)");
                } else {
                    label.append("--(Active)");
                }
            }
            selectionList.add(label.toString());
        }
    }

    private void validateSelection() {
        for (int i : selectionList.getSelectionIndices()) {
            if (i == currentIndex) {
                selectionList.deselect(currentIndex);
                MessageBox mb = new MessageBox(getShell(),
                        SWT.ICON_WARNING | SWT.OK);
                mb.setMessage(
                        "Current " + elementDesc + " can't be deactivated!");
                mb.open();
                break;
            }
        }
    }

    private void changeState(ActState state) {
        for (int i : selectionList.getSelectionIndices()) {
            elementList.get(i).setActionState(state);
        }
        NsharpEditor editor = NsharpEditor.getActiveNsharpEditor();
        if (editor != null) {
            editor.refresh();
        }
        close();
    }

    /**
     * Create a dialog for changing the activation state of time lines.
     * 
     * @param parentShell
     *            the parent shell.
     */
    public static ActivationDialog createTimeLineActivationDialog(
            Shell parentShell) {
        String desc = "Time Line";
        List<? extends NsharpOperationElement> elementList = Collections
                .emptyList();
        int currentIndex = -1;
        NsharpEditor editor = NsharpEditor.getActiveNsharpEditor();
        if (editor != null) {
            NsharpResourceHandler rsc = editor.getRscHandler();
            if (rsc != null) {
                elementList = rsc.getTimeElementList();
                currentIndex = rsc.getCurrentTimeElementListIndex();
            }
        }
        return new ActivationDialog(parentShell, desc, elementList,
                currentIndex);
    }

    /**
     * Create a dialog for changing the activation state of stations.
     * 
     * @param parentShell
     *            the parent shell.
     */
    public static ActivationDialog createStationActivationDialog(
            Shell parentShell) {
        String desc = "Station";
        List<NsharpOperationElement> elementList = Collections.emptyList();
        int currentIndex = -1;
        NsharpEditor editor = NsharpEditor.getActiveNsharpEditor();
        if (editor != null) {
            NsharpResourceHandler rsc = editor.getRscHandler();
            if (rsc != null) {
                elementList = rsc.getStnElementList();
                currentIndex = rsc.getCurrentStnElementListIndex();
            }
        }
        return new ActivationDialog(parentShell, desc, elementList,
                currentIndex);
    }

    /**
     * Create a dialog for changing the activation state of sounding types.
     * 
     * @param parentShell
     *            the parent shell.
     */
    public static ActivationDialog createSoundingTypeActivationDialog(
            Shell parentShell) {
        String desc = "Sounding Type";
        List<NsharpOperationElement> elementList = Collections.emptyList();
        int currentIndex = -1;
        NsharpEditor editor = NsharpEditor.getActiveNsharpEditor();
        if (editor != null) {
            NsharpResourceHandler rsc = editor.getRscHandler();
            if (rsc != null) {
                elementList = rsc.getSndElementList();
                currentIndex = rsc.getCurrentSndElementListIndex();
            }
        }
        return new ActivationDialog(parentShell, desc, elementList,
                currentIndex);
    }
}
