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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.viz.core.exception.VizException;

import gov.noaa.nws.ncep.ui.nsharp.NsharpConstants;
import gov.noaa.nws.ncep.ui.nsharp.display.NsharpEditor;

/**
 * gov.noaa.nws.ncep.ui.nsharp.palette.NsharpPaneConfigDialog
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
 * 06/28/2012    229       Chin Chen    Initial coding
 * Aug 17, 2018  7081      dgilling     Refactor based on CaveJFACEDialog.
 *
 * </pre>
 *
 * @author Chin Chen
 */

public class NsharpPaneConfigDialog extends AbstractNsharpConfigDlg {

    private String paneConfigurationName;

    private Combo paneCfgCombo;

    public NsharpPaneConfigDialog(Shell parentShell) {
        super(parentShell, "Nsharp Pane Configuration Selection");

        if (configStore != null) {
            paneConfigurationName = configStore.getGraphProperty()
                    .getPaneConfigurationName();
        }
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Label paneCfgComboLbl = new Label(composite, SWT.LEFT);
        paneCfgComboLbl.setText("Pane Configuration Selection :");

        paneCfgCombo = new Combo(composite, SWT.NONE);
        paneCfgCombo.setItems(NsharpConstants.PANE_CONFIGURATION_NAME);
        int selectionIdx = paneCfgCombo.indexOf(paneConfigurationName);
        selectionIdx = Math.max(selectionIdx, 0);
        paneCfgCombo.select(selectionIdx);
        paneCfgCombo.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                statusHandler.debug("Default selected index: "
                        + paneCfgCombo.getSelectionIndex() + ", selected item: "
                        + (paneCfgCombo.getSelectionIndex() == -1 ? "<null>"
                                : paneCfgCombo.getItem(
                                        paneCfgCombo.getSelectionIndex()))
                        + ", text content in the text field: "
                        + paneCfgCombo.getText());
                String text = paneCfgCombo.getText();
                if (paneCfgCombo.indexOf(text) < 0) {
                    paneCfgCombo.add(text);
                }
            }
        });

        return composite;
    }

    @Override
    protected void handleApplyClicked() {
        String newpaneConfigurationName = paneCfgCombo
                .getItem(paneCfgCombo.getSelectionIndex());
        paneConfigurationName = newpaneConfigurationName;
        updateCfgStore();
        NsharpEditor editor = NsharpEditor.getActiveNsharpEditor();
        if (editor != null) {
            editor.restartEditor(paneConfigurationName);
        } else {
            NsharpPaletteWindow paletteWin = NsharpPaletteWindow.getInstance();
            if (paletteWin != null) {
                paletteWin.updateSpecialGraphBtn(paneConfigurationName);
            }
        }

        setReturnCode(OK);
    }

    private void updateCfgStore() {
        if (configStore != null) {
            configStore.getGraphProperty()
                    .setPaneConfigurationName(paneConfigurationName);
        }
    }

    @Override
    protected void handleSaveClicked() {
        // save to xml
        paneConfigurationName = paneCfgCombo
                .getItem(paneCfgCombo.getSelectionIndex());
        updateCfgStore();
        try {
            mgr.saveConfigStoreToFs(configStore);
        } catch (VizException e) {
            statusHandler.error("Could not save pane configuration.", e);
        }
    }
}
