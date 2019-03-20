/*
 * SpenesAttrDlg
 *
 * Date created: June 2012
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.ui.pgen.attrdialog;

import java.awt.Color;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.raytheon.uf.viz.core.exception.VizException;

import gov.noaa.nws.ncep.ui.pgen.PgenStaticDataProvider;
import gov.noaa.nws.ncep.ui.pgen.display.IAttribute;
import gov.noaa.nws.ncep.ui.pgen.elements.Spenes;
import gov.noaa.nws.ncep.viz.common.ui.color.ColorButtonSelector;


/**
 * Singleton attribute dialog for a spenes.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * ??                      B. Yin       Initial creation
 * May 04        #734      J. Zeng      Add calling method to create/edit a spenes
 * Mar 2013      #928      B. Yin       Made the button bar smaller.
 * Mar 20, 2019  #7572     dgilling     Code cleanup.
 *
 * </pre>
 *
 * @author
 */
public class SpenesAttrDlg extends LineAttrDlg {

    private static final int CREATE_ID = IDialogConstants.CLIENT_ID + 0;

    private static final int EDIT_ID = IDialogConstants.CLIENT_ID + 1;

    /**
     * single instance
     */
    private static SpenesAttrDlg INSTANCE = null;

    private static SpenesFormatDlg sfDlg = null;

    /*
     * FIXME: Fix LineAttrDlg and this class's constructor declaration to no
     * longer throw VizException since constructor can't actually throw that
     * exception.
     */
    private SpenesAttrDlg(Shell parShell) throws VizException {
        super(parShell);
    }

    /**
     * Creates a spenes attribute dialog if the dialog does not exist and
     * returns the instance. If the dialog exists, return the instance.
     *
     * @param parShell
     * @return
     */
    public static synchronized SpenesAttrDlg getInstance(Shell parShell) {
        if (INSTANCE == null) {
            /* FIXME: remove unnecessary try-catch after fixing constructor. */
            try {
                INSTANCE = new SpenesAttrDlg(parShell);
            } catch (VizException e) {
                e.printStackTrace();
            }
        }

        return INSTANCE;
    }

    /**
     * initialize components
     */
    @Override
    protected void initializeComponents() {

        this.getShell().setText("SPENES Attributes");

       //Panel to hold line attributes
        Composite panel0 = new Composite(top, SWT.NONE);

        // layout for the line attributes
        GridLayout p0Layout = new GridLayout(2, false);
        p0Layout.marginHeight = 3;
        p0Layout.marginWidth = 3;
        panel0.setLayout(p0Layout);

        createColorAttr(panel0);
        createWidthAttr(panel0);
        createSmoothAttr(panel0);


    }

    /**
     * Create the button bar for this dialog override from the super class
     *
     * @param Composite:
     *            parent of the dialog
     */
    @Override
    public void createButtonsForButtonBar(Composite parent) {
        Button applyBtn = createButton(parent, IDialogConstants.OK_ID, "Apply",
                true);
        applyBtn.setEnabled(false);

        Button createBtn = createButton(parent, CREATE_ID, "Create", true);
        createBtn.setEnabled(true);

        Button editBtn = createButton(parent, EDIT_ID, "Edit", true);
        editBtn.setEnabled(true);

        Button cancelBtn = createButton(parent, IDialogConstants.CANCEL_ID,
                IDialogConstants.CANCEL_LABEL, true);
        cancelBtn.setEnabled(false);
    }

    /**
     * button listener method override from super class.
     *
     * @param int
     *            button id for button.
     */
    @Override
    protected void buttonPressed(int buttonId) {
        if (IDialogConstants.OK_ID == buttonId) {
            okPressed();
        } else if (CANCEL == buttonId) {
            cancelPressed();
        } else if (CREATE_ID == buttonId) {
            createPressed();
        } else if (EDIT_ID == buttonId) {
            editPressed();
        }
    }

    /**
     * Sets values of all attributes of the dialog.
     */
    @Override
    public void setAttrForDlg(IAttribute iattr) {

        // if ( iattr instanceof Spenes ){
        Spenes attr = (Spenes) iattr;
        Color[] clr = attr.getColors();
        if (clr != null) {
                this.setColor(clr);
            }

        float lw = attr.getLineWidth();
        if (lw > 0) {
                this.widthSpinnerSlider.setSelection((int)lw);
            }

        double ps = attr.getSmoothFactor();
        if (ps > 0) {
                this.smoothLvlCbo.select((int)ps);
            }

        this.setSpenes(attr);

        // }
    }

    /**
     * Create widgets for the Color attribute
     */
    private void createColorAttr(Composite comp) {

        colorLbl = new Label(comp, SWT.LEFT);
        colorLbl.setText("Color:");
        cs = new ColorButtonSelector(comp);
        cs.setColorValue(new RGB(255, 0, 0));
    }

    /**
     * Create widgets for the Line Width attribute
     */
    private void createWidthAttr(Composite comp) {

        widthLbl = new Label(comp, SWT.LEFT);
        widthLbl.setText("Line Width:");

        GridLayout gl = new GridLayout(3, false);
        Group widthGrp = new Group(comp, SWT.NONE);
        widthGrp.setLayout(gl);

        widthSpinnerSlider = new gov.noaa.nws.ncep.ui.pgen.attrdialog.vaadialog.SpinnerSlider(
                widthGrp, SWT.HORIZONTAL, 1);
        widthSpinnerSlider.setLayoutData(new GridData(180,30));
        widthSpinnerSlider.setMinimum(1);
        widthSpinnerSlider.setMaximum(10);
        widthSpinnerSlider.setIncrement(1);
        widthSpinnerSlider.setPageIncrement(3);
        widthSpinnerSlider.setDigits(0);
    }

    /**
     * Create widgets for the smooth Level attribute
     */
    private void createSmoothAttr(Composite comp) {

        smoothLbl = new Label(comp, SWT.LEFT);
        smoothLbl.setText("Smooth Level:");

        smoothLvlCbo = new Combo(comp, SWT.DROP_DOWN | SWT.READ_ONLY);

        smoothLvlCbo.add("0");
        smoothLvlCbo.add("1");
        smoothLvlCbo.add("2");

        smoothLvlCbo.select(0);
    }

    /**
     * Return color from the color picker of the dialog
     */
    @Override
    public Color[] getColors() {
        // IAttribute requires to return an array of colors
        // Only the first color is used at this time.
        Color[] colors = new Color[2];

        colors[0] = new java.awt.Color(cs.getColorValue().red,
                cs.getColorValue().green, cs.getColorValue().blue);

        colors[1] = Color.green;

        return colors;
    }

    /**
     * Sets the color of the color picker of the dialog.
     *
     * @param clr
     */
    @Override
    public void setColor(Color clr[]) {

        cs.setColorValue(
                new RGB(clr[0].getRed(), clr[0].getGreen(), clr[0].getBlue()));

    }

    /**
     * Return line with from the attribute dialog
     */
    @Override
    public float getLineWidth(){
        return widthSpinnerSlider.getSelection();
    }

    /**
     * Return smooth level from the attribute dialog
     */
    @Override
    public int getSmoothFactor(){
        return smoothLvlCbo.getSelectionIndex();
    }

    /**
     * Get the size scale.
     */
    @Override
    public double getSizeScale(){
        return 0;
    }

    /**
     * Return false because jet cannot be closed line.
     */
    @Override
    public Boolean isClosedLine(){
        return true;
    }

    /**
     * Return false because jet cannot be filled.
     */
    @Override
    public Boolean isFilled(){
        return false;
    }

    /**
     * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
     */
    @Override
    public void okPressed() {
        super.okPressed();
        if (de != null) {
            Spenes spenes = (Spenes) de;
            MessageDialog infoDlg = null;
            if (!PgenStaticDataProvider.getProvider().isRfcLoaded()) {
                infoDlg = new MessageDialog(
                        PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                                .getShell(),
                        "Information", null,
                        "Loading state, cwa, and rfc maps.\nPlease wait.",
                        MessageDialog.INFORMATION, new String[] { "OK" }, 0);
                infoDlg.setBlockOnOpen(false);
                infoDlg.open();
            }
            spenes.generateStatesWfosRfcs();
            spenes.setStateZ000(spenes.getStates());
            spenes.setLocation(spenes.getStates());
            spenes.setAttnWFOs(spenes.getCwas());
            spenes.setAttnRFCs(spenes.getRfcs());
            spenes.setLatLon(spenes.getLinePoints());

            if (infoDlg != null && infoDlg.getShell() != null) {
                infoDlg.close();
            }
        }
    }

    /**
     * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
     */
    public void createPressed() {
        if (de != null) {

            okPressed();

            ((Spenes) de).setInitTime();
            sfDlg = SpenesFormatDlg.getInstance(SpenesAttrDlg.this.getShell(),
                    SpenesAttrDlg.INSTANCE, (Spenes) de);

            sfDlg.setBlockOnOpen(false);

            sfDlg.open();

        }

    }

    /**
     * button for editing spenes
     */
    public void editPressed() {

        if (de != null) {
            sfDlg = SpenesFormatDlg.getInstance(SpenesAttrDlg.this.getShell(),
                    SpenesAttrDlg.INSTANCE, (Spenes) de);
            sfDlg.setBlockOnOpen(false);

            sfDlg.open();
            // sfDlg.setAttrForDlg(spenes);
        } else {
            MessageDialog infoDlg = null;
            infoDlg = new MessageDialog(
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                            .getShell(),
                    "Information", null, "No drawing element to edit.",
                    MessageDialog.INFORMATION, new String[] { "OK" }, 0);
            infoDlg.setBlockOnOpen(false);
            infoDlg.open();
        }
    }

    /**
     * set spenes
     *
     * @param elem
     */
    public void setSpenes(Spenes elem) {
        de = elem;
    }

    /**
     * get spenes
     *
     * @return
     */
    public Spenes getSpenes() {
        return (Spenes) de;
    }
}
