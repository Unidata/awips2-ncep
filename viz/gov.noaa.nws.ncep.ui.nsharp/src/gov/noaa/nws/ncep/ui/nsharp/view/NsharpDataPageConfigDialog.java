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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.raytheon.uf.viz.core.exception.VizException;

import gov.noaa.nws.ncep.ui.nsharp.NsharpConstants;
import gov.noaa.nws.ncep.ui.nsharp.NsharpDataPageProperty;
import gov.noaa.nws.ncep.ui.nsharp.display.NsharpEditor;
import gov.noaa.nws.ncep.ui.nsharp.display.rsc.NsharpResourceHandler;

/**
 * NsharpDataPageConfigDialog
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
 * 05/23/2010               Chin Chen   Initial coding
 * 08/10/2015    RM#9396    Chin Chen   implement new OPC pane configuration
 * Aug 20, 2018  #7081      dgilling    Refactor based on CaveJFACEDialog.
 *
 * </pre>
 *
 * @author Chin Chen
 */

public class NsharpDataPageConfigDialog extends AbstractNsharpConfigDlg {

    private NsharpDataPageProperty dpp;

    private NsharpDataPageProperty editingDpp;

    // element 0 is always empty--avoids +1/-1 index math in this code
    private int[] pageOrderNumberArray;

    // element 0 is always empty--avoids +1/-1 index math in this code
    private int[] editingOrderNumberArray;

    private String paneConfigurationName;

    private int numberPagePerDisplay;

    private Text[] currentOrderTextArray;

    private Text[] newOrderTextArray;

    public NsharpDataPageConfigDialog(Shell parentShell) {
        super(parentShell, "Data Page Display Configuration");

        dpp = configStore.getDataPageProperty();
        paneConfigurationName = configStore.getGraphProperty()
                .getPaneConfigurationName();

        pageOrderNumberArray = new int[NsharpConstants.PAGE_MAX_NUMBER + 1];
        pageOrderNumberArray[NsharpConstants.PAGE_SUMMARY1] = dpp
                .getSummary1Page();
        pageOrderNumberArray[NsharpConstants.PAGE_SUMMARY2] = dpp
                .getSummary2Page();
        pageOrderNumberArray[NsharpConstants.PAGE_PARCEL_DATA] = dpp
                .getParcelDataPage();
        pageOrderNumberArray[NsharpConstants.PAGE_THERMODYNAMIC_DATA] = dpp
                .getThermodynamicDataPage();
        pageOrderNumberArray[NsharpConstants.PAGE_OPC_DATA] = dpp
                .getOpcDataPage();
        pageOrderNumberArray[NsharpConstants.PAGE_MIXING_HEIGHT] = dpp
                .getMixingHeightPage();
        pageOrderNumberArray[NsharpConstants.PAGE_STORM_RELATIVE] = dpp
                .getStormRelativePage();
        pageOrderNumberArray[NsharpConstants.PAGE_MEAN_WIND] = dpp
                .getMeanWindPage();
        pageOrderNumberArray[NsharpConstants.PAGE_CONVECTIVE_INITIATION] = dpp
                .getConvectiveInitiationPage();
        pageOrderNumberArray[NsharpConstants.PAGE_SEVERE_POTENTIAL] = dpp
                .getSeverePotentialPage();
        pageOrderNumberArray[NsharpConstants.PAGE_D2DLITE] = dpp
                .getD2dLitePage(); // d2dlite
        // d2dlite
        pageOrderNumberArray[NsharpConstants.PAGE_FUTURE] = dpp.getFuturePage();

        editingOrderNumberArray = pageOrderNumberArray.clone();

        numberPagePerDisplay = dpp.getNumberPagePerDisplay();
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Group pageGroup = new Group(composite, SWT.DEFAULT);
        pageGroup.setText("number of page per display");
        pageGroup.setLayout(new GridLayout());
        pageGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Button onePageBtn = new Button(pageGroup, SWT.RADIO);
        onePageBtn.setText("1");
        onePageBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                numberPagePerDisplay = 1;
            }
        });

        Button twoPageBtn = new Button(pageGroup, SWT.RADIO);
        twoPageBtn.setText("2");
        twoPageBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                numberPagePerDisplay = 2;
            }
        });

        if (numberPagePerDisplay == 1) {
            onePageBtn.setSelection(true);
        } else {
            twoPageBtn.setSelection(true);
        }

        if (NsharpConstants.PANE_LITE_D2D_CFG_STR.equals(paneConfigurationName)
                || NsharpConstants.PANE_OPC_CFG_STR
                        .equals(paneConfigurationName)) {
            pageGroup.setVisible(false);
        }

        Composite fakeTableComp = new Composite(composite, SWT.BORDER);
        GridLayout layout = new GridLayout(3, false);
        layout.verticalSpacing = 0;
        fakeTableComp.setLayout(layout);
        fakeTableComp
                .setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Label nameLbl = new Label(fakeTableComp, SWT.BORDER);
        nameLbl.setText("Page Name");
        nameLbl.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));

        Label curOrderLbl = new Label(fakeTableComp, SWT.BORDER);
        curOrderLbl.setText("Current");
        curOrderLbl.setLayoutData(
                new GridData(SWT.FILL, SWT.DEFAULT, true, false));

        Label newOrderLbl = new Label(fakeTableComp, SWT.BORDER);
        newOrderLbl.setText("New Order");
        newOrderLbl.setLayoutData(
                new GridData(SWT.FILL, SWT.DEFAULT, true, false));

        currentOrderTextArray = new Text[NsharpConstants.PAGE_MAX_NUMBER + 1];
        newOrderTextArray = new Text[NsharpConstants.PAGE_MAX_NUMBER + 1];
        for (int i = 1; i <= NsharpConstants.PAGE_MAX_NUMBER; i++) {
            Label pageNameLbl = new Label(fakeTableComp, SWT.BORDER);
            pageNameLbl.setText(NsharpConstants.PAGE_NAME_ARRAY[i]);
            pageNameLbl.setLayoutData(
                    new GridData(SWT.FILL, SWT.CENTER, true, true));

            currentOrderTextArray[i] = new Text(fakeTableComp,
                    SWT.BORDER | SWT.READ_ONLY);
            Text currentOrderText = currentOrderTextArray[i];
            currentOrderText.setText(Integer.toString(pageOrderNumberArray[i]));
            Color disabledColor = currentOrderText.getDisplay()
                    .getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW);
            currentOrderText.setBackground(disabledColor);
            currentOrderText.setLayoutData(
                    new GridData(SWT.FILL, SWT.DEFAULT, true, false));

            newOrderTextArray[i] = new Text(fakeTableComp, SWT.BORDER);
            Text newOrderText = newOrderTextArray[i];
            newOrderText.setText(Integer.toString(pageOrderNumberArray[i]));
            // to make sure user enter digits only
            newOrderText.addVerifyListener((e) -> {
                /*
                 * Chin note: when "Delete", "Backspace" entered, this handler
                 * will be called, but its chars.length = 0
                 */
                String string = e.text;

                if ((string.length() == 1) && (newOrderText != null)) {
                    if ('-' == string.charAt(0)) {
                        String textStr = newOrderText.getText();
                        if ((textStr != null) && (textStr.length() >= 1)
                                && (textStr.contains("-"))) {
                            e.doit = false;
                            return;
                        }
                    } else if (('0' > string.charAt(0))
                            || ('9' < string.charAt(0))) {
                        e.doit = false;
                        return;
                    }
                }
            });
            newOrderText.setLayoutData(
                    new GridData(SWT.FILL, SWT.DEFAULT, true, false));
        }

        return composite;
    }

    /*
     * To make sure user does not configure same page order number for different
     * page.
     */
    private boolean sanityCheck() {
        for (int i = 1; i <= NsharpConstants.PAGE_MAX_NUMBER; i++) {
            String textStr = newOrderTextArray[i].getText();
            if ((textStr != null) && !(textStr.isEmpty())) {
                if (!textStr.contains("-") || textStr.length() > 1) {
                    int pnum = Integer.valueOf(textStr);
                    if (pnum >= 1 && pnum <= NsharpConstants.PAGE_MAX_NUMBER) {
                        editingOrderNumberArray[i] = pnum;
                    } else {
                        MessageDialog.openWarning(getShell(), StringUtils.EMPTY,
                                "Wrong Configuration! Order number should be within [1-12]");
                        return false;
                    }
                }
            }
        }
        for (int i = 1; i <= NsharpConstants.PAGE_MAX_NUMBER; i++) {
            for (int j = 1; j < NsharpConstants.PAGE_MAX_NUMBER; j++) {
                if ((i != j)
                        && editingOrderNumberArray[i] == editingOrderNumberArray[j]) {
                    MessageDialog.openWarning(getShell(), StringUtils.EMPTY,
                            "Wrong Configuration! Multiple pages with same order.");
                    return false;
                }
            }
        }
        return true;
    }

    private void applyChange() {
        pageOrderNumberArray = editingOrderNumberArray.clone();
        for (int i = 1; i <= NsharpConstants.PAGE_MAX_NUMBER; i++) {
            currentOrderTextArray[i]
                    .setText(Integer.toString(pageOrderNumberArray[i]));
        }
        NsharpEditor editor = NsharpEditor.getActiveNsharpEditor();
        if (editor != null) {
            NsharpResourceHandler rsc = editor.getRscHandler();
            editingDpp = new NsharpDataPageProperty();
            editingDpp.setSummary1Page(
                    pageOrderNumberArray[NsharpConstants.PAGE_SUMMARY1]);
            editingDpp.setSummary2Page(
                    pageOrderNumberArray[NsharpConstants.PAGE_SUMMARY2]);
            editingDpp.setParcelDataPage(
                    pageOrderNumberArray[NsharpConstants.PAGE_PARCEL_DATA]);
            editingDpp.setThermodynamicDataPage(
                    pageOrderNumberArray[NsharpConstants.PAGE_THERMODYNAMIC_DATA]);
            editingDpp.setOpcDataPage(
                    pageOrderNumberArray[NsharpConstants.PAGE_OPC_DATA]);
            editingDpp.setMixingHeightPage(
                    pageOrderNumberArray[NsharpConstants.PAGE_MIXING_HEIGHT]);
            editingDpp.setStormRelativePage(
                    pageOrderNumberArray[NsharpConstants.PAGE_STORM_RELATIVE]);
            editingDpp.setMeanWindPage(
                    pageOrderNumberArray[NsharpConstants.PAGE_MEAN_WIND]);
            editingDpp.setConvectiveInitiationPage(
                    pageOrderNumberArray[NsharpConstants.PAGE_CONVECTIVE_INITIATION]);
            editingDpp.setSeverePotentialPage(
                    pageOrderNumberArray[NsharpConstants.PAGE_SEVERE_POTENTIAL]);
            editingDpp.setD2dLitePage(
                    pageOrderNumberArray[NsharpConstants.PAGE_D2DLITE]); // d2dlite
            editingDpp.setFuturePage(
                    pageOrderNumberArray[NsharpConstants.PAGE_FUTURE]); // d2dlite
            editingDpp.setNumberPagePerDisplay(numberPagePerDisplay);
            rsc.setDataPageProperty(editingDpp);
            editor.refresh();
        }
    }

    @Override
    protected void handleApplyClicked() {
        if (sanityCheck()) {
            applyChange();
        }
    }

    @Override
    protected void handleSaveClicked() {
        if (sanityCheck() == false) {
            applyChange();
            dpp.setSummary1Page(pageOrderNumberArray[NsharpConstants.PAGE_SUMMARY1]);
            dpp.setSummary2Page(pageOrderNumberArray[NsharpConstants.PAGE_SUMMARY2]);
            dpp.setParcelDataPage(pageOrderNumberArray[NsharpConstants.PAGE_PARCEL_DATA]);
            dpp.setThermodynamicDataPage(pageOrderNumberArray[NsharpConstants.PAGE_THERMODYNAMIC_DATA]);
            dpp.setOpcDataPage(pageOrderNumberArray[NsharpConstants.PAGE_OPC_DATA]);
            dpp.setMixingHeightPage(pageOrderNumberArray[NsharpConstants.PAGE_MIXING_HEIGHT]);
            dpp.setStormRelativePage(pageOrderNumberArray[NsharpConstants.PAGE_STORM_RELATIVE]);
            dpp.setMeanWindPage(pageOrderNumberArray[NsharpConstants.PAGE_MEAN_WIND]);
            dpp.setConvectiveInitiationPage(pageOrderNumberArray[NsharpConstants.PAGE_CONVECTIVE_INITIATION]);
            dpp.setSeverePotentialPage(pageOrderNumberArray[NsharpConstants.PAGE_SEVERE_POTENTIAL]);
            dpp.setD2dLitePage(pageOrderNumberArray[NsharpConstants.PAGE_D2DLITE]);// d2dlite
            dpp.setFuturePage(pageOrderNumberArray[NsharpConstants.PAGE_FUTURE]);// d2dlite
            dpp.setNumberPagePerDisplay(numberPagePerDisplay);
            try {
                // save to xml
                mgr.saveConfigStoreToFs(configStore);
            } catch (VizException e) {
                statusHandler.error("Unable to save data page configuration.",
                        e);
            }
        }
    }

    @Override
    protected Point getInitialLocation(Point initialSize) {
        Rectangle parentBounds = getParentShell().getBounds();
        int x = parentBounds.x + parentBounds.width - initialSize.x;
        int y = parentBounds.y + parentBounds.height - initialSize.y;
        return new Point(x, y);
    }
}
