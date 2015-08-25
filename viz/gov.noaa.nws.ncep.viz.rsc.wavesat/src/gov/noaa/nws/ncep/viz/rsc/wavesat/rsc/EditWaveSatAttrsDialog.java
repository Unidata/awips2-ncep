package gov.noaa.nws.ncep.viz.rsc.wavesat.rsc;

import gov.noaa.nws.ncep.viz.common.ui.color.ColorButtonSelector;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResourceData;
import gov.noaa.nws.ncep.viz.resources.attributes.AbstractEditResourceAttrsDialog;
import gov.noaa.nws.ncep.viz.resources.attributes.ResourceAttrSet.RscAttrValue;
import gov.noaa.nws.ncep.viz.resources.colorBar.ColorBarEditor;
import gov.noaa.nws.ncep.viz.ui.display.ColorBar;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

/**
 * A Dialog to edit SGWH resource attributes.
 * 
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 09/25/11      #248        Greg Hull      Initial Creation.
 * 07/01/14     TTR 1018     Steve Russell  Updated call to ColorBarEditor
 * 04/15/15     R6281        Bruce Hebbard  Add meters/feet selection
 * 06/16/15     R6281        Bruce Hebbard  Clean up per code review comments
 * 
 * </pre>
 * 
 * @author ghull
 * @version 1
 */

public class EditWaveSatAttrsDialog extends AbstractEditResourceAttrsDialog {

    public EditWaveSatAttrsDialog(Shell parentShell, INatlCntrsResourceData r,
            Boolean apply) {
        super(parentShell, r, apply);
    }

    private RscAttrValue useFeetInsteadOfMetersAttr = null;

    private RscAttrValue colorBarForMetersAttr = null;

    private RscAttrValue colorBarForFeetAttr = null;

    private RscAttrValue fontNameAttr = null;

    private RscAttrValue fontSizeAttr = null;

    private RscAttrValue timeDisplayIntervalAttr = null;

    private RscAttrValue timeDisplayColorAttr = null;

    private String[] availFonts = { "Times", "Serif", "Sans", "Utopia",
            "Roman", "Courier" };

    private String[] availFontsSizes = { "6", "10", "12", "14", "16", "18",
            "20", "24", "28" };

    private ColorBarEditor colorBarEditorMeters = null;

    private ColorBarEditor colorBarEditorFeet = null;

    Button metersButton = null;

    Button feetButton = null;

    ColorBar editedColorBarMeters = null;

    ColorBar editedColorBarFeet = null;

    Group colorBarGrpMeters = null;

    Group colorBarGrpFeet = null;

    //
    @Override
    public Composite createDialog(Composite topComp) {

        // Get and validate attribute values

        useFeetInsteadOfMetersAttr = editedRscAttrSet
                .getRscAttr("useFeetInsteadOfMeters");
        colorBarForMetersAttr = editedRscAttrSet
                .getRscAttr("colorBarForMeters");
        colorBarForFeetAttr = editedRscAttrSet.getRscAttr("colorBarForFeet");
        fontNameAttr = editedRscAttrSet.getRscAttr("fontName");
        fontSizeAttr = editedRscAttrSet.getRscAttr("fontSize");
        timeDisplayIntervalAttr = editedRscAttrSet
                .getRscAttr("timeDisplayInterval");
        timeDisplayColorAttr = editedRscAttrSet.getRscAttr("timeDisplayColor");

        if (useFeetInsteadOfMetersAttr == null
                || useFeetInsteadOfMetersAttr.getAttrClass() != Boolean.class) {
            System.out
                    .println("useFeetInsteadOfMeters is null or not of expected class Boolean?");
            return null;
        }
        if (colorBarForMetersAttr == null
                || colorBarForMetersAttr.getAttrClass() != ColorBar.class) {
            System.out
                    .println("colorBarForMeters is null or not of expected class ColorBar?");
            return null;
        }
        if (colorBarForFeetAttr == null
                || colorBarForFeetAttr.getAttrClass() != ColorBar.class) {
            System.out
                    .println("colorBarForFeet is null or not of expected class ColorBar?");
            return null;
        }
        if (fontNameAttr == null || fontNameAttr.getAttrClass() != String.class) {
            System.out
                    .println("fontName is null or not of expected class String?");
            return null;
        }
        if (fontSizeAttr == null
                || fontSizeAttr.getAttrClass() != Integer.class) {
            System.out
                    .println("fontSize is null or not of expected class Integer?");
            return null;
        }
        if (timeDisplayIntervalAttr == null
                || timeDisplayIntervalAttr.getAttrClass() != Integer.class) {
            System.out
                    .println("timeDisplayInterval is null or not of expected class Integer?");
            return null;
        }
        if (timeDisplayColorAttr == null
                || timeDisplayColorAttr.getAttrClass() != RGB.class) {
            System.out
                    .println("timeDisplayColor is null or not of expected class RGB?");
            return null;
        }

        // Global layout of composite

        FormLayout layout0 = new FormLayout();
        topComp.setLayout(layout0);

        // Unit selection group

        Group unitsGrp = new Group(topComp, SWT.BORDER);
        FormData fd = new FormData();
        unitsGrp.setText("Select Units");

        unitsGrp.setLayout(new FormLayout());
        // fd.width = 90;
        fd.left = new FormAttachment(0, 14);
        fd.right = new FormAttachment(31, 0);
        fd.bottom = new FormAttachment(0, 100);
        fd.top = new FormAttachment(0, 10);
        unitsGrp.setLayoutData(fd);

        feetButton = new Button(unitsGrp, SWT.RADIO);
        fd = new FormData();
        fd.left = new FormAttachment(12, 0);
        fd.top = new FormAttachment(50, -12);
        feetButton.setLayoutData(fd);
        feetButton.setSelection(((Boolean) useFeetInsteadOfMetersAttr
                .getAttrValue()).booleanValue());
        feetButton.setText("Feet");
        feetButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                Boolean useFeet = feetButton.getSelection();
                useFeetInsteadOfMetersAttr.setAttrValue(useFeet);
                colorBarGrpMeters.setVisible(metersButton.getSelection());
                colorBarGrpFeet.setVisible(feetButton.getSelection());
            }
        });

        metersButton = new Button(unitsGrp, SWT.RADIO);
        fd = new FormData();
        fd.left = new FormAttachment(50, 0);
        fd.top = new FormAttachment(50, -12);
        metersButton.setLayoutData(fd);
        metersButton.setSelection(!((Boolean) useFeetInsteadOfMetersAttr
                .getAttrValue()).booleanValue());
        metersButton.setText("Meters");
        metersButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                Boolean useFeet = feetButton.getSelection();
                useFeetInsteadOfMetersAttr.setAttrValue(useFeet);
                colorBarGrpMeters.setVisible(metersButton.getSelection());
                colorBarGrpFeet.setVisible(feetButton.getSelection());
            }
        });

        // Font selection group

        Group selFontGrp = new Group(topComp, SWT.BORDER);
        fd = new FormData();
        selFontGrp.setText("Select Font");

        selFontGrp.setLayout(new FormLayout());
        // fd.width = 220;
        fd.left = new FormAttachment(35, 0);
        fd.right = new FormAttachment(65, 0);
        fd.bottom = new FormAttachment(0, 100);
        fd.top = new FormAttachment(0, 10);
        selFontGrp.setLayoutData(fd);

        final Combo fontNameCombo = new Combo(selFontGrp, SWT.READ_ONLY
                | SWT.DROP_DOWN);
        fd = new FormData();
        fd.left = new FormAttachment(10, 0);
        fd.top = new FormAttachment(0, 30);
        fontNameCombo.setLayoutData(fd);

        fontNameCombo.setItems(availFonts);

        fontNameCombo.setText(((String) fontNameAttr.getAttrValue()));

        fontNameCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                int selIndx = fontNameCombo.getSelectionIndex();

                fontNameAttr.setAttrValue(availFonts[selIndx]);
            }
        });

        Label nameLbl = new Label(selFontGrp, SWT.None);
        fd = new FormData();
        fd.left = new FormAttachment(fontNameCombo, 0, SWT.LEFT);
        fd.bottom = new FormAttachment(fontNameCombo, -3, SWT.TOP);
        nameLbl.setLayoutData(fd);
        nameLbl.setText("Name");
        // nameLbl.setVisible( false );

        final Combo fontSizeCombo = new Combo(selFontGrp, SWT.READ_ONLY
                | SWT.DROP_DOWN);
        fd = new FormData();
        fd.left = new FormAttachment(fontNameCombo, 20, SWT.RIGHT);
        fd.top = new FormAttachment(fontNameCombo, 0, SWT.TOP);
        fontSizeCombo.setLayoutData(fd);

        fontSizeCombo.setItems(availFontsSizes);
        fontSizeCombo.select(0);

        for (int i = 0; i < availFontsSizes.length; i++) {
            if (Integer.parseInt(availFontsSizes[i]) == (Integer) fontSizeAttr
                    .getAttrValue()) {
                fontSizeCombo.select(i);
                break;
            }
        }

        Label fontSizeLbl = new Label(selFontGrp, SWT.None);
        fd = new FormData();
        fd.left = new FormAttachment(fontSizeCombo, 0, SWT.LEFT);
        fd.bottom = new FormAttachment(fontSizeCombo, -3, SWT.TOP);
        fontSizeLbl.setLayoutData(fd);
        fontSizeLbl.setText("Size");
        // fontSizeLbl.setVisible( false );

        fontSizeCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                int selIndx = fontSizeCombo.getSelectionIndex();

                fontSizeAttr.setAttrValue(Integer
                        .parseInt(availFontsSizes[selIndx]));
            }
        });

        // Time Stamp group

        Group selTimeStampGrp = new Group(topComp, SWT.BORDER);
        fd = new FormData();
        selTimeStampGrp.setText("Time Stamp");

        selTimeStampGrp.setLayout(new FormLayout());

        fd.top = new FormAttachment(selFontGrp, 0, SWT.TOP);
        // fd.width = 270;
        fd.left = new FormAttachment(69, 0);
        fd.right = new FormAttachment(100, -14);
        fd.bottom = new FormAttachment(selFontGrp, 0, SWT.BOTTOM);
        selTimeStampGrp.setLayoutData(fd);

        final Spinner timeStampIntervalSpinner = new Spinner(selTimeStampGrp,
                SWT.BORDER);
        timeStampIntervalSpinner
                .setToolTipText("Minutes between time stamps/lines");
        fd = new FormData();
        fd.left = new FormAttachment(10, 0);
        fd.top = new FormAttachment(0, 30);
        timeStampIntervalSpinner.setLayoutData(fd);
        timeStampIntervalSpinner.setDigits(0);
        timeStampIntervalSpinner.setMinimum(1);
        timeStampIntervalSpinner.setMaximum(90);
        timeStampIntervalSpinner.setIncrement(1);
        timeStampIntervalSpinner.setPageIncrement(1);
        timeStampIntervalSpinner
                .setSelection(((Integer) timeDisplayIntervalAttr.getAttrValue())
                        .intValue());
        timeStampIntervalSpinner.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                timeDisplayIntervalAttr.setAttrValue(new Integer(
                        timeStampIntervalSpinner.getSelection()));
            }
        });

        Label timeStampIntLbl1 = new Label(selTimeStampGrp, SWT.None);
        fd = new FormData();
        fd.left = new FormAttachment(timeStampIntervalSpinner, 0, SWT.LEFT);
        fd.bottom = new FormAttachment(timeStampIntervalSpinner, -3, SWT.TOP);
        timeStampIntLbl1.setLayoutData(fd);
        timeStampIntLbl1.setText("Interval");

        Label timeStampIntLbl2 = new Label(selTimeStampGrp, SWT.None);
        fd = new FormData();
        fd.left = new FormAttachment(timeStampIntervalSpinner, 6, SWT.RIGHT);
        fd.bottom = new FormAttachment(timeStampIntervalSpinner, 0, SWT.CENTER);
        timeStampIntLbl2.setLayoutData(fd);
        timeStampIntLbl2.setText("min");

        Composite selColComp = new Composite(selTimeStampGrp, SWT.NONE);
        fd = new FormData();

        fd.bottom = new FormAttachment(timeStampIntervalSpinner, 4, SWT.BOTTOM);
        fd.left = new FormAttachment(timeStampIntervalSpinner, 93, SWT.LEFT);
        selColComp.setLayoutData(fd);
        selColComp.setLayout(new GridLayout());

        final ColorButtonSelector colBtnSel = new ColorButtonSelector(
                selColComp, 55, 25);
        colBtnSel.setColorValue((RGB) timeDisplayColorAttr.getAttrValue());
        colBtnSel.addListener(new IPropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                timeDisplayColorAttr.setAttrValue(event.getNewValue());
            }
        });

        Label timeStampIntLbl3 = new Label(selTimeStampGrp, SWT.None);
        fd = new FormData();
        fd.left = new FormAttachment(timeStampIntLbl1, 100, SWT.LEFT);
        fd.top = new FormAttachment(timeStampIntLbl1, 0, SWT.TOP);
        timeStampIntLbl3.setLayoutData(fd);
        timeStampIntLbl3.setText("Color");

        // Color Bar group - Meters

        colorBarGrpMeters = new Group(topComp, SWT.NONE);
        colorBarGrpMeters.setText("Edit Color Bar (Meters)");
        fd = new FormData();// 400,300);
        fd.left = new FormAttachment(0, 15);
        fd.right = new FormAttachment(100, -15);
        fd.top = new FormAttachment(selFontGrp, 15, SWT.BOTTOM);
        fd.bottom = new FormAttachment(100, -20);
        colorBarGrpMeters.setLayoutData(fd);
        colorBarGrpMeters.setLayout(new FormLayout());
        colorBarGrpMeters.setVisible(metersButton.getSelection());

        editedColorBarMeters = (ColorBar) colorBarForMetersAttr.getAttrValue();

        colorBarEditorMeters = new ColorBarEditor(colorBarGrpMeters,
                editedColorBarMeters, true);

        // Color Bar group - Feet

        colorBarGrpFeet = new Group(topComp, SWT.NONE);
        colorBarGrpFeet.setText("Edit Color Bar (Feet)");
        fd = new FormData();// 400,300);
        fd.left = new FormAttachment(0, 15);
        fd.right = new FormAttachment(100, -15);
        fd.top = new FormAttachment(selFontGrp, 15, SWT.BOTTOM);
        fd.bottom = new FormAttachment(100, -20);
        colorBarGrpFeet.setLayoutData(fd);
        colorBarGrpFeet.setLayout(new FormLayout());
        colorBarGrpFeet.setVisible(feetButton.getSelection());

        editedColorBarFeet = (ColorBar) colorBarForFeetAttr.getAttrValue();

        colorBarEditorFeet = new ColorBarEditor(colorBarGrpFeet,
                editedColorBarFeet, true);

        return topComp;
    }

    @Override
    public void initWidgets() {
        // done in createDialog
    }

    @Override
    protected void dispose() {
        super.dispose();
        colorBarEditorMeters.dispose();
        colorBarEditorFeet.dispose();
    }
}
