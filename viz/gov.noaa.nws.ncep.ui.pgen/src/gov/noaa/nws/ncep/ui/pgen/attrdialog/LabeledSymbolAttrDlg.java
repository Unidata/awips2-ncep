/*
 * gov.noaa.nws.ncep.ui.pgen.attrDialog.LabeledSymbolAttrDlg
 *
 * 20 June 2009
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.ui.pgen.attrdialog;

import gov.noaa.nws.ncep.ui.pgen.PgenSession;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.exception.VizException;

/**
 * Singleton attribute dialog for labeled symbols.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 06/09        #116        B. Yin      Initial Creation.
 * 09/09        #149        B. Yin      Added check boxes for multi-selection
 * 07/10        #270        B. Yin      Save the last status for 'Use Line Color'
 * 12/14/2015   R13161      S. Russell  Altered getInstance()
 *                                      Altered initializeComponents()
 * 
 * </pre>
 * 
 * @author B. Yin
 */

public class LabeledSymbolAttrDlg extends SymbolAttrDlg {

    private static final IUFStatusHandler logger = UFStatus
            .getHandler(LabeledSymbolAttrDlg.class);

    static private LabeledSymbolAttrDlg INSTANCE = null;

    protected Button labelChkBox;
    private Button labelColorChkBox;

    private boolean lastLabelStatus;
    private boolean lastUseColorStatus;

    /**
     * constructor
     * 
     * @param parShell
     * @throws VizException
     */
    protected LabeledSymbolAttrDlg(Shell parShell) throws VizException {
        super(parShell);
    }

    /**
     * Creates a symbol attribute dialog if the dialog does not exist and
     * returns the instance. If the dialog exists, return the instance.
     * 
     * @param parShell
     * @return
     */
    public static SymbolAttrDlg getInstance(Shell parShell) {

        // Clear out the DrawableElement from the last invocation,
        // doing so will provide information about the state of things in
        // initializeComponents
        de = null;

        if (INSTANCE == null) {

            try {

                INSTANCE = new LabeledSymbolAttrDlg(parShell);

            } catch (VizException e) {
                logger.error(
                        "Error getting an instance of Labeled Symbol Attribute Dialog",
                        e);
            }
        }

        return INSTANCE;

    }

    @Override
    /**
     * Creates buttons, menus, and other controls in the dialog area
     */
    protected void initializeComponents() {
        super.initializeComponents();

        boolean isALabeledSymbolType = false;
        boolean isASymbolWithALabel = false;

        Composite inCmp = new Composite(top, SWT.NONE);
        inCmp.setLayout(getGridLayout(3, false, 0, 0, 0, 0));

        chkBox[ChkBox.LABEL.ordinal()] = new Button(inCmp, SWT.CHECK);
        chkBox[ChkBox.LABEL.ordinal()].setLayoutData(new GridData(CHK_WIDTH,
                CHK_HEIGHT));
        chkBox[ChkBox.LABEL.ordinal()]
                .addSelectionListener(new SelectionAdapter() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        Button btn = (Button) e.widget;
                        if (btn.getSelection()) {
                            labelChkBox.setEnabled(true);

                        } else {
                            labelChkBox.setEnabled(false);

                        }
                    }

                });

        chkBox[ChkBox.LABEL.ordinal()].setVisible(false);

        labelChkBox = new Button(inCmp, SWT.CHECK);
        labelChkBox.setText("Label");

        labelColorChkBox = new Button(inCmp, SWT.CHECK);
        labelColorChkBox.setText("Use Symbol Color");

        labelChkBox.addListener(SWT.MouseUp, new Listener() {

            @Override
            public void handleEvent(Event event) {

                if (((Button) event.widget).getSelection()) {
                    labelColorChkBox.setEnabled(true);
                } else {
                    labelColorChkBox.setEnabled(false);
                }

                lastLabelStatus = ((Button) event.widget).getSelection();

            }
        });

        labelColorChkBox.addListener(SWT.MouseUp, new Listener() {

            @Override
            public void handleEvent(Event event) {
                lastUseColorStatus = ((Button) event.widget).getSelection();
            }
        });

        // IF a symbol button is being pushed on the palette
        if (de == null) {

            // Set "Label" and "Use Symbol Color" checkboxes to whatever the
            // value was last picked by the user while on the paltte
            labelChkBox.setSelection(lastLabelStatus);
            labelColorChkBox.setSelection(lastUseColorStatus);

        } else {
            // Verify that the object, not on the palette, is indeed a symbol
            // and has a label
            isASymbolWithALabel = de.isASymbolAndHasALabel();

        }

        // If the symbol is labeled, check the "Label" checkbox
        if (isASymbolWithALabel) {
            labelChkBox.setSelection(true);
        }

    }

    /**
     * Check if the label box is checked
     */
    public boolean labelEnabled() {
        return labelChkBox.getSelection();
    }

    /**
     * Check if the 'Use Symbol Color' box is checked.
     * 
     * @return
     */
    public boolean useSymbolColor() {
        return labelColorChkBox.getSelection();
    }

    /**
     * Set the 'Label' check box
     * 
     * @param enabled
     */
    public void setLabelChkBox(boolean enabled) {
        labelChkBox.setEnabled(enabled);

        if (!labelChkBox.isEnabled() || !labelChkBox.getSelection()) {
            labelColorChkBox.setEnabled(false);
        }
    }

    @Override
    public int open() {

        this.create();

        // if current action is MultiSelect, make the check boxes visible
        if (PgenSession.getInstance().getPgenPalette().getCurrentAction()
                .equalsIgnoreCase("MultiSelect")) {
            enableChkBoxes(true);
            enableAllWidgets(false);
        } else {
            enableChkBoxes(false);
        }

        int rt = super.open();
        return rt;
    }

    /**
     * Make the check boxes visible/invisible
     * 
     * @param flag
     */
    private void enableChkBoxes(boolean flag) {

        if (!flag) {
            setAllChkBoxes();
        }
        for (ChkBox chk : ChkBox.values()) {
            chkBox[chk.ordinal()].setVisible(flag);
        }

        chkBox[ChkBox.LAT.ordinal()].setVisible(false);
        chkBox[ChkBox.LON.ordinal()].setVisible(false);
        chkBox[ChkBox.LABEL.ordinal()].setVisible(false);

    }

    /**
     * Enable/disable all widgets in the dialog
     * 
     * @param flag
     */
    private void enableAllWidgets(boolean flag) {

        colorLbl.setEnabled(flag);

        clearLbl.setEnabled(flag);
        clearBtn1.setEnabled(flag);
        clearBtn2.setEnabled(flag);

        widthLbl.setEnabled(flag);
        widthSpinnerSlider.setEnabled(flag);

        sizeLbl.setEnabled(flag);
        sizeSpinnerSlider.setEnabled(flag);

        latitudeLabel.setEnabled(flag);
        latitudeText.setEnabled(flag);

        longitudeLabel.setEnabled(flag);
        longitudeText.setEnabled(flag);

        labelChkBox.setEnabled(flag);

    }

    /**
     * Set all multi-selection check boxes to true.
     */
    private void setAllChkBoxes() {

        for (ChkBox chk : ChkBox.values()) {
            chkBox[chk.ordinal()].setSelection(true);
        }

    }
}
