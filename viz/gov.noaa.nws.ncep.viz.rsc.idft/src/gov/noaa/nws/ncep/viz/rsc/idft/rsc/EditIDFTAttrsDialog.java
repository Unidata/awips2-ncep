/*
 * gov.noaa.nws.ncep.viz.idft.rsc.EditIDFTAttrsDialog
 * 
 * September 2009
 *
 * This code has been developed by the SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.viz.rsc.idft.rsc;

import gov.noaa.nws.ncep.viz.common.ui.color.ColorButtonSelector;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResourceData;
import gov.noaa.nws.ncep.viz.resources.attributes.AbstractEditResourceAttrsDialog;
import gov.noaa.nws.ncep.viz.resources.attributes.ResourceAttrSet.RscAttrValue;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.viz.core.rsc.capabilities.Capabilities;

/**
 * UI for editing IDFT resource attributes.
 * 
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 09/11/2009   154        Gang Zhang  Initial creation.
 * 04/27/2010    #245      Greg Hull    Added Apply Button
 * 11/04/2010    #307      Greg Hull    Change Lengths,Widths from String to Double
 * 03/13/2013   982        Archana     add display stn number attr
 * 04/05/2016   R15715     dgilling    Refactored for new AbstractEditResourceAttrsDialog constructor.
 * 
 * </pre>
 * 
 * @author gzhang
 * @version 1.0
 */

public class EditIDFTAttrsDialog extends AbstractEditResourceAttrsDialog {

    public EditIDFTAttrsDialog(Shell parentShell, INatlCntrsResourceData r,
            Capabilities capabilities, Boolean apply) {
        super(parentShell, r, capabilities, apply);
    }

    @Override
    public Composite createDialog(Composite topComp) {

        GridLayout gridLayout = new GridLayout(2, false);
        gridLayout.makeColumnsEqualWidth = true;

        Composite colComp = new Composite(topComp, SWT.NONE);

        GridData gd = new GridData();
        gd.horizontalIndent = 0;
        gd.verticalIndent = 5;
        colComp.setLayoutData(gd);

        gridLayout.verticalSpacing = 0;
        colComp.setLayout(gridLayout);
        final RscAttrValue rscAttrStationNumber = editedRscAttrSet
                .getRscAttr("displayStationNumber");
        final RscAttrValue rscAttrDistColor = editedRscAttrSet
                .getRscAttr("distanceColor"); // --- attrs of number,point,and
                                              // arrow

        final RscAttrValue rscAttrPointColor = editedRscAttrSet
                .getRscAttr("pointColor");

        final RscAttrValue rscAttrArrowColor = editedRscAttrSet
                .getRscAttr("arrowColor");
        final RscAttrValue rscAttrArrowLength = editedRscAttrSet
                .getRscAttr("arrowLength");
        final RscAttrValue rscAttrArrowLineWidth = editedRscAttrSet
                .getRscAttr("arrowLineWidth");

        if (rscAttrStationNumber == null
                || rscAttrStationNumber.getAttrClass() != Boolean.class) {
            statusHandler
                    .error("rscAttrStationNumber is null or not of expected class Boolean?");
            return null;
        }

        if (rscAttrArrowColor == null
                || rscAttrArrowColor.getAttrClass() != RGB.class) {
            statusHandler
                    .error("rscAttrArrowColor is null or not of expected class RGB?");
            return null;
        }
        if (rscAttrDistColor == null
                || rscAttrDistColor.getAttrClass() != RGB.class) {
            statusHandler
                    .error("rscAttrDistColor is null or not of expected class RGB?");
            return null;
        }
        if (rscAttrArrowLength == null
                || rscAttrArrowLength.getAttrClass() != Double.class) {
            statusHandler
                    .error("rscAttrArrowLength is null or not of expected class Double?");
            return null;
        }
        if (rscAttrArrowLineWidth == null
                || rscAttrArrowLineWidth.getAttrClass() != Double.class) {
            statusHandler
                    .error("rscAttrArrowLineWidth is null or not of expected class Double?");
            return null;
        }

        final Label labelNumber = new Label(colComp, SWT.NONE);
        labelNumber.setText("Number: ");// --- number row

        final ColorButtonSelector colorSelNumber = new ColorButtonSelector(
                colComp, 50, 20);
        colorSelNumber.setColorValue((RGB) rscAttrDistColor.getAttrValue());
        colorSelNumber.addListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                rscAttrDistColor.setAttrValue(event.getNewValue());
            }
        });

        final Label labelPoint = new Label(colComp, SWT.NONE);
        labelPoint.setText("Point: "); // --- point row

        final ColorButtonSelector colorSelPoint = new ColorButtonSelector(
                colComp, 50, 20);
        colorSelPoint.setColorValue((RGB) rscAttrPointColor.getAttrValue());
        colorSelPoint.addListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                rscAttrPointColor.setAttrValue(event.getNewValue());
            }
        });

        final Label labelArrow = new Label(colComp, SWT.NONE);
        labelArrow.setText("Arrow: "); // --- arrow row

        final ColorButtonSelector colorSelArrow = new ColorButtonSelector(
                colComp, 50, 20);
        colorSelArrow.setColorValue((RGB) rscAttrArrowColor.getAttrValue());
        colorSelArrow.addListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                rscAttrArrowColor.setAttrValue(event.getNewValue());
            }
        });

        final Label labelArrowLength = new Label(colComp, SWT.NONE);
        labelArrowLength.setText("Length: ");

        final Combo comboArrowLength = new Combo(colComp, SWT.READ_ONLY);
        comboArrowLength.setItems(new String[] { "1.0", "1.25", "1.5" });
        String lengthString = rscAttrArrowLength.getAttrValue().toString();
        comboArrowLength.setText("".equals(lengthString) ? "1.25"
                : lengthString);

        comboArrowLength.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                rscAttrArrowLength.setAttrValue(Double
                        .parseDouble(comboArrowLength.getText()));
            }
        });

        final Label labelArrowLineWidth = new Label(colComp, SWT.NONE);
        labelArrowLineWidth.setText("LineWidth: ");

        final Combo comboArrowLineWidth = new Combo(colComp, SWT.READ_ONLY);
        comboArrowLineWidth.setItems(new String[] { "1.0", "1.25", "1.5" });
        String lineWidthString = rscAttrArrowLineWidth.getAttrValue()
                .toString();
        comboArrowLineWidth.setText("".equals(lineWidthString) ? "1.25"
                : lineWidthString);

        comboArrowLineWidth.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                rscAttrArrowLineWidth.setAttrValue(Double
                        .parseDouble(comboArrowLineWidth.getText()));
            }
        });

        final Button displayStationNumberBtn = new Button(colComp, SWT.CHECK);
        displayStationNumberBtn.setEnabled(true);
        displayStationNumberBtn.setSelection((Boolean) rscAttrStationNumber
                .getAttrValue());
        displayStationNumberBtn.setText("Display Station Number");
        displayStationNumberBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                rscAttrStationNumber.setAttrValue(displayStationNumberBtn
                        .getSelection());

            }
        });
        return topComp;
    }

    @Override
    public void initWidgets() {
        // done in createDialog

    }
}
