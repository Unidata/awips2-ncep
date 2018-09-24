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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.raytheon.uf.viz.core.exception.VizException;

import gov.noaa.nws.ncep.ui.nsharp.NsharpConstants;
import gov.noaa.nws.ncep.ui.nsharp.display.NsharpEditor;
import gov.noaa.nws.ncep.ui.nsharp.display.rsc.NsharpResourceHandler;
import gov.noaa.nws.ncep.viz.common.ui.color.ColorMatrixSelector;

/**
 * gov.noaa.nws.ncep.ui.nsharp.palette.NsharpDataDisplayConfigDialog
 *
 *
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 03/21/2012    229       Chin Chen    Initial coding
 * Aug 17, 2018  7081      dgilling     Refactor based on CaveJFACEDialog.
 *
 * </pre>
 *
 * @author dgilling
 */

public class NsharpWindBarbConfigDialog extends AbstractNsharpConfigDlg {

    private static final String ERROR_MESSAGE = "Input Error!";

    private static final float DEFAULT_SIZE = 1f;

    private static final float DEFAULT_LINE_WIDTH = 1f;

    private static final int DEFAULT_DISTANCE = 400;

    private float curLineWidth;

    private RGB curColor;

    private float curSize;

    private int curDist;

    private boolean showFilteredWind;

    private Text windBarbWidthText;

    private Text windBarbSizeText;

    private Text windBarbDistText;

    public NsharpWindBarbConfigDialog(Shell parentShell) {
        super(parentShell, "Nsharp Wind Barb");

        this.curLineWidth = NsharpConstants.WINDBARB_WIDTH;
        this.curColor = NsharpConstants.color_yellow;
        this.curSize = NsharpConstants.WINDBARB_SIZE;
        this.curDist = NsharpConstants.WINDBARB_DISTANCE_DEFAULT;

        if (configStore != null) {
            this.curLineWidth = configStore.getGraphProperty()
                    .getWindBarbLineWidth();
            this.curColor = configStore.getGraphProperty().getWindBarbColor();
            this.curSize = configStore.getGraphProperty().getWindBarbSize();
            this.curDist = configStore.getGraphProperty().getWindBarbDistance();
            this.showFilteredWind = configStore.getGraphProperty()
                    .isShowFilteredWindInCircle();
        }
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        composite.setLayout(new GridLayout(2, false));
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Group lineConfigGroup = new Group(composite, SWT.NONE);
        lineConfigGroup.setText("Properties");
        GridLayout layout = new GridLayout();
        layout.marginHeight = 18;
        layout.marginWidth = 18;
        layout.horizontalSpacing = 8;
        layout.verticalSpacing = 8;
        lineConfigGroup.setLayout(layout);
        lineConfigGroup
                .setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Label widthLbl = new Label(lineConfigGroup, SWT.LEFT);
        widthLbl.setText("Width");

        windBarbWidthText = new Text(lineConfigGroup, SWT.BORDER);
        windBarbWidthText.setText(Float.toString(curLineWidth));
        windBarbWidthText.addVerifyListener((e) -> {
            for (int i = 0; i < e.text.length(); i++) {
                char nextChar = e.text.charAt(i);
                if (!('0' <= nextChar && nextChar <= '9')
                        && ('.' != nextChar)) {
                    e.doit = false;
                    return;
                }
            }

            e.doit = true;
        });

        GC gc = new GC(windBarbWidthText);
        int charWidth = gc.getFontMetrics().getAverageCharWidth();
        gc.dispose();

        GridData layoutData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        layoutData.widthHint = charWidth * 12;
        windBarbWidthText.setLayoutData(layoutData);

        Label sizeLbl = new Label(lineConfigGroup, SWT.LEFT);
        sizeLbl.setText("Size");

        windBarbSizeText = new Text(lineConfigGroup, SWT.BORDER);
        windBarbSizeText.setText(Float.toString(curSize));
        windBarbSizeText.addVerifyListener((e) -> {
            for (int i = 0; i < e.text.length(); i++) {
                char nextChar = e.text.charAt(i);
                if (!('0' <= nextChar && nextChar <= '9')
                        && ('.' != nextChar)) {
                    e.doit = false;
                    return;
                }
            }

            e.doit = true;
        });
        layoutData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        layoutData.widthHint = charWidth * 12;
        windBarbSizeText.setLayoutData(layoutData);

        Label distLbl = new Label(lineConfigGroup, SWT.LEFT);
        distLbl.setText("Min dist betw barbs, m");

        windBarbDistText = new Text(lineConfigGroup, SWT.BORDER);
        windBarbDistText.setText(Integer.toString(curDist));
        windBarbDistText.addVerifyListener((e) -> {
            for (int i = 0; i < e.text.length(); i++) {
                char nextChar = e.text.charAt(i);
                if (!('0' <= nextChar && nextChar <= '9')) {
                    e.doit = false;
                    return;
                }
            }

            e.doit = true;
        });
        layoutData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        layoutData.widthHint = charWidth * 12;
        windBarbDistText.setLayoutData(layoutData);

        Group colorConfigGroup = new Group(composite, SWT.NONE);
        colorConfigGroup.setText("Color");
        colorConfigGroup.setLayout(new GridLayout());
        colorConfigGroup
                .setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));

        ColorMatrixSelector cms = new ColorMatrixSelector(colorConfigGroup,
                false, false, 28, 92, 18, 22, 4, 8, 5);
        cms.setColorValue(curColor);
        cms.addListener((e) -> {
            curColor = cms.getColorValue();
        });

        return composite;
    }

    @Override
    protected void handleApplyClicked() {
        applyChanges();
    }

    @Override
    protected void handleSaveClicked() {
        if (applyChanges()) {
            try {
                // save to configuration file
                mgr.saveConfigStoreToFs(configStore);
            } catch (VizException e) {
                statusHandler.error("Unable to save wind barb configuration.",
                        e);
            }
        }
    }

    private boolean applyChanges(){
        String textStr = windBarbDistText.getText();
        if (StringUtils.isNotEmpty(textStr)) {
            try {
                curDist = Integer.valueOf(textStr);
            } catch (NumberFormatException e) {
                MessageDialog.openWarning(getShell(), StringUtils.EMPTY,
                        ERROR_MESSAGE);
                return false;
            }

            if (curDist <= 0) {
                curDist = DEFAULT_DISTANCE;
                windBarbDistText.setText(Integer.toString(curDist));
                MessageDialog.openWarning(getShell(), StringUtils.EMPTY,
                        ERROR_MESSAGE);
                return false;
            }
        }

        textStr = windBarbSizeText.getText();
        if (StringUtils.isNotEmpty(textStr)) {
            try {
                curSize = Float.valueOf(textStr);
            } catch (NumberFormatException e) {
                MessageDialog.openWarning(getShell(), StringUtils.EMPTY,
                        ERROR_MESSAGE);
                return false;
            }

            if (curSize <= 0 || curSize > 15) {
                curSize = DEFAULT_SIZE;
                windBarbSizeText.setText(Float.toString(curSize));
                MessageDialog.openWarning(getShell(), StringUtils.EMPTY,
                        ERROR_MESSAGE);
                return false;
            }
        }

        textStr = windBarbWidthText.getText();
        if (StringUtils.isNotEmpty(textStr)) {
            try {
                curLineWidth = Float.valueOf(textStr);
            } catch (NumberFormatException e) {
                MessageDialog.openWarning(getShell(), StringUtils.EMPTY,
                        ERROR_MESSAGE);
                return false;
            }

            if (curLineWidth <= 0 || curLineWidth > 7) {
                curLineWidth = DEFAULT_LINE_WIDTH;
                windBarbWidthText.setText(Float.toString(curLineWidth));
                MessageDialog.openWarning(getShell(), StringUtils.EMPTY,
                        ERROR_MESSAGE);
                return false;
            }
        }

        configStore.getGraphProperty().setWindBarbColor(curColor);
        configStore.getGraphProperty().setWindBarbDistance(curDist);
        configStore.getGraphProperty().setWindBarbLineWidth(curLineWidth);
        configStore.getGraphProperty().setWindBarbSize(curSize);
        configStore.getGraphProperty()
                .setShowFilteredWindInCircle(showFilteredWind);
        // inform editor
        NsharpEditor editor = NsharpEditor.getActiveNsharpEditor();
        if (editor != null) {
            NsharpResourceHandler rsc = editor.getRscHandler();
            rsc.setGraphConfigProperty(configStore.getGraphProperty());
            editor.refresh();
        }

        return true;
    }
}
