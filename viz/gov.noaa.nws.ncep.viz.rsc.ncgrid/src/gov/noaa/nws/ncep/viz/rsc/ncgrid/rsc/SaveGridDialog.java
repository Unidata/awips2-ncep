package gov.noaa.nws.ncep.viz.rsc.ncgrid.rsc;

import gov.noaa.nws.ncep.viz.resources.INatlCntrsResourceData;
import gov.noaa.nws.ncep.viz.resources.attributes.AbstractEditResourceAttrsDialog;
import gov.noaa.nws.ncep.viz.resources.attributes.ResourceAttrSet;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.actions.SaveGridInput;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.raytheon.uf.viz.core.rsc.capabilities.Capabilities;

/**
 * The grid contour attribute editing dialog.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer       Description
 * ------------ ----------  -----------    --------------------------
 * 03/01/2016   R6821       K. Bugenhagen  Initial creation.
 * 04/05/2016   R15715      dgilling       Refactored for new AbstractEditResourceAttrsDialog constructor.
 * 
 * @author kbugenhagen
 * @version 1
 */

public class SaveGridDialog extends AbstractEditResourceAttrsDialog {

    private Text gdfileText;

    private Text modelNameText;

    private Text gparmText;

    private Text gvcordText;

    private Text glevelText;

    private Text gdattimText;

    private SaveGridInput saveInput;

    public SaveGridDialog(SaveGridInput saveInput, Shell parentShell,
            INatlCntrsResourceData rd, Capabilities capabilities) {
        super(parentShell, rd, capabilities, false);
        this.dlgTitle = "Save " + this.rscData.getResourceName() + " Grid";
        this.saveInput = saveInput;
    }

    @Override
    public Composite createDialog(Composite composite) {

        // contour attributes editing
        Group contourAttributesGroup = new Group(composite, SWT.SHADOW_NONE);
        GridLayout contourAttrGridLayout = new GridLayout();
        contourAttrGridLayout.numColumns = 1;
        contourAttrGridLayout.marginHeight = 8;
        contourAttrGridLayout.marginWidth = 2;
        contourAttrGridLayout.horizontalSpacing = 20;
        contourAttrGridLayout.verticalSpacing = 8;
        contourAttributesGroup.setLayout(contourAttrGridLayout);

        Composite comp = new Composite(contourAttributesGroup, SWT.SHADOW_NONE);
        GridLayout contourIntervalsGridLayout = new GridLayout();
        contourIntervalsGridLayout.numColumns = 2;
        comp.setLayout(contourIntervalsGridLayout);

        // GDFILE
        Label gdfileLabel = new Label(comp, SWT.NONE);
        gdfileLabel.setText("GDFILE:");
        gdfileText = new Text(comp, SWT.SINGLE | SWT.BORDER);
        gdfileText.setLayoutData(new GridData(230, SWT.DEFAULT));
        gdfileText.setText(saveInput.getGdfile());
        gdfileText.setEditable(false);

        // MODEL NAME
        Label modelNameLabel = new Label(comp, SWT.NONE);
        modelNameLabel.setText("MODEL NAME:");
        modelNameText = new Text(comp, SWT.SINGLE | SWT.BORDER);
        modelNameText.setLayoutData(new GridData(230, SWT.DEFAULT));
        modelNameText.setText("");
        modelNameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                saveInput.setModelName(modelNameText.getText().trim());
            }
        });

        // GPARM
        Label gparmLabel = new Label(comp, SWT.NONE);
        gparmLabel.setText("GPARM:");
        gparmText = new Text(comp, SWT.SINGLE | SWT.BORDER);
        gparmText.setLayoutData(new GridData(230, SWT.DEFAULT));
        gparmText.setText(saveInput.getGparm());
        gparmText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                saveInput.setGparm(gparmText.getText().trim());
            }
        });

        // GVCORD
        Label gvcordLabel = new Label(comp, SWT.NONE);
        gvcordLabel.setText("GVCORD:");
        gvcordText = new Text(comp, SWT.SINGLE | SWT.BORDER);
        gvcordText.setLayoutData(new GridData(230, SWT.DEFAULT));
        gvcordText.setText(saveInput.getGvcord());
        gvcordText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                saveInput.setGvcord(gvcordText.getText().trim());
            }
        });

        // GLEVEL
        Label glevelLabel = new Label(comp, SWT.NONE);
        glevelLabel.setText("GLEVEL:");
        glevelText = new Text(comp, SWT.SINGLE | SWT.BORDER);
        glevelText.setLayoutData(new GridData(230, SWT.DEFAULT));
        glevelText.setText(saveInput.getGlevel());
        glevelText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                saveInput.setGlevel(glevelText.getText().trim());
            }

        });

        // GDATTIM
        Label refTimeLabel = new Label(comp, SWT.NONE);
        refTimeLabel.setText("GDATTIM:");
        gdattimText = new Text(comp, SWT.SINGLE | SWT.BORDER);
        gdattimText.setLayoutData(new GridData(230, SWT.DEFAULT));
        gdattimText.setText(saveInput.getGdattim());
        gdattimText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                saveInput.setModelName(gdattimText.getText().trim());
            }

        });

        // Save All
        Label saveAllLabel = new Label(comp, SWT.NONE);
        saveAllLabel.setText("Save All Frames:");
        final Button saveAll = new Button(comp, SWT.CHECK);
        saveAll.setLayoutData(new GridData(230, SWT.DEFAULT));
        // saveAll.setText("Save All Frames");
        saveAll.setSelection(saveInput.isSaveAll());
        saveAll.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                saveInput.setSaveAll(saveAll.getSelection());
                if (saveAll.getSelection()) {
                    gdattimText.setText("ALL");
                    gdattimText.setEnabled(false);
                } else {
                    gdattimText.setText(saveInput.getGdattim());
                    gdattimText.setEnabled(true);
                }
            }
        });

        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        Label sepLbl = new Label(contourAttributesGroup, SWT.SEPARATOR
                | SWT.HORIZONTAL);
        sepLbl.setLayoutData(gd);

        final Button toolTipDisplay = new Button(contourAttributesGroup,
                SWT.CHECK);
        toolTipDisplay.setLayoutData(new GridData(SWT.DEFAULT, SWT.DEFAULT));
        toolTipDisplay.setText("ToolTips OFF");
        toolTipDisplay.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                if (toolTipDisplay.getSelection()) {
                    toolTipDisplay.setText("ToolTips ON");
                    EnableToolTip(true);
                } else {
                    toolTipDisplay.setText("ToolTips OFF");
                    EnableToolTip(false);
                }
            }
        });
        return composite;
    }

    private void EnableToolTip(boolean on) {
        if (on) {
            modelNameText.setToolTipText(NcgridAttributesHelp
                    .ModelNameToolTipText());
            gparmText.setToolTipText(NcgridAttributesHelp.GparmToolTipText());
            gvcordText.setToolTipText(NcgridAttributesHelp.GvcordToolTipText());
            glevelText.setToolTipText(NcgridAttributesHelp.GlevelToolTipText());
            gdattimText.setToolTipText(NcgridAttributesHelp
                    .GdattimToolTipText());
        } else {
            modelNameText.setToolTipText(null);
            gparmText.setToolTipText(null);
            gvcordText.setToolTipText(null);
            glevelText.setToolTipText(null);
            gdattimText.setToolTipText(null);

        }
    }

    @Override
    public void initWidgets() {
        // TODO Auto-generated method stub
    }

    @Override
    public Object open() {
        Display display = getParent().getDisplay();

        // copy the attrSet
        editedRscAttrSet = new ResourceAttrSet(rscData.getRscAttrSet());

        createShell();

        initWidgets();

        shell.pack();
        shell.open();

        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        dispose();

        if (ok) {
            return saveInput;
        } else {
            return null;
        }
    }

    /**
     * @return the saveInput
     */
    public SaveGridInput getSaveInput() {
        return saveInput;
    }

    /**
     * @param saveInput
     *            the saveInput to set
     */
    public void setSaveInput(SaveGridInput saveInput) {
        this.saveInput = saveInput;
    }
}
