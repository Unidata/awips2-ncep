/*
 * gov.noaa.nws.ncep.ui.pgen.controls.PgenRemindDialog
 * 
 * 31 DECEMBER 2009
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */
package gov.noaa.nws.ncep.ui.pgen.controls;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog used to Remind users that a closing PGEN session has unsaved changes.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 12/09        #158        S. Gilbert  Initial creation
 * 05/10/2016   R13560      SRussell    Updated createButtonsForButtonBar()
 *                                      to include a "Cancel" button in the
 *                                      dialog box.  Overrode buttonPressed()
 *                                      to include new button choices.
 *                                      Created otherButtonPressed() to 
 *                                      process button codes for new button
 *                                      choices in the dialog
 * </pre>
 * 
 * @author
 * @version 1
 */
public class PgenRemindDialog extends Dialog {

    private final String dlgTitle = "PGEN Exit Confirmation";

    private Composite dlgArea = null;

    // private Button yesButton = null;

    // private Button noButton = null;

    private Image image;

    /*
     * Constructor
     */
    public PgenRemindDialog(Shell parShell, Image im) {
        super(parShell);
        setShellStyle(SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
        this.image = im;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets
     * .Shell)
     */
    @Override
    protected void configureShell(Shell shell) {

        super.configureShell(shell);
        shell.setText(dlgTitle);
        // shell.setSize(500, 400);
        shell.setLocation(500, 200);
    }

    /*
     * (non-Javadoc) Create all of the widgets on the Dialog
     * 
     * @see
     * org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets
     * .Composite)
     */
    @Override
    public Control createDialogArea(Composite parent) {

        dlgArea = (Composite) super.createDialogArea(parent);

        GridLayout gl = new GridLayout(1, true);
        gl.marginTop = 5;
        gl.marginBottom = 5;
        gl.verticalSpacing = 10;
        dlgArea.setLayout(gl);

        /*
         * Create a label for the table
         */
        Label info = new Label(dlgArea, SWT.NONE);
        info.setText("There are unsaved changes in this PGEN session.\nDo you want to save them?");
        info.setLayoutData(new GridData());

        /*
         * Set image of editor to be displayed
         */
        Button pic = new Button(dlgArea, SWT.FLAT);
        pic.setImage(image);

        return dlgArea;
    }

    /*
     * Create YES/NO/Cancel buttons
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.YES_ID, "YES", false);
        createButton(parent, IDialogConstants.NO_ID, "NO", false);
        createButton(parent, IDialogConstants.CANCEL_ID, "CANCEL", false);
    }

    /**
     * 
     * Our version of Eclipse and SWT doesn't have more than OK_ID or CANCEL_ID
     * implemented so we need to override this methods below to provide YES and
     * NO options
     */

    @Override
    protected void buttonPressed(int buttonId) {
        if (IDialogConstants.YES_ID == buttonId) {
            otherButtonPressed(IDialogConstants.YES_ID);
        } else if (IDialogConstants.NO_ID == buttonId) {
            otherButtonPressed(IDialogConstants.NO_ID);
        } else {
            super.buttonPressed(buttonId);
        }

    }

    /**
     * Set the return code for the custom button codes above
     * 
     * @param buttonID
     */
    protected void otherButtonPressed(int buttonID) {
        super.setReturnCode(buttonID);
        super.close();
    }

}
