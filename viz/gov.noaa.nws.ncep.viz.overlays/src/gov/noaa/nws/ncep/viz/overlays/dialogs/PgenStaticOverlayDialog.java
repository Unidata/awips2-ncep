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
package gov.noaa.nws.ncep.viz.overlays.dialogs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.RGBColors;
import com.raytheon.uf.viz.core.drawables.AbstractRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.IMapDescriptor;
import com.raytheon.uf.viz.core.map.MapDescriptor;
import com.raytheon.uf.viz.core.maps.display.MapRenderableDisplay;
import com.raytheon.uf.viz.core.procedures.Bundle;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.ResourceProperties;
import com.raytheon.viz.ui.BundleProductLoader;
import com.raytheon.viz.ui.EditorUtil;
import com.raytheon.viz.ui.dialogs.CaveSWTDialog;
import com.raytheon.viz.ui.dialogs.colordialog.ColorData;
import com.raytheon.viz.ui.dialogs.colordialog.ColorWheelComp;
import com.raytheon.viz.ui.editor.IMultiPaneEditor;

import gov.noaa.nws.ncep.ui.pgen.controls.ActivityCollection;
import gov.noaa.nws.ncep.ui.pgen.controls.ActivityElement;
import gov.noaa.nws.ncep.ui.pgen.controls.PgenRetrieveCommonDialogArea;
import gov.noaa.nws.ncep.viz.overlays.resources.PgenStaticOverlayResourceData;

/**
 *
 * Dialog for selecting PGEN data to load as a static overlay
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Mar 01, 2019  7752     tjensen   Initial creation
 * Aug 26, 2019  67216    ksunil    Widget changes to implement the ticket.
 *                                  Code-refactored
 * Feb 06, 2020  57972    tjensen   Add option to display in map layer. Used for
 *                                  GFE
 *
 * </pre>
 *
 * @author tjensen
 */
public class PgenStaticOverlayDialog extends CaveSWTDialog {

    private static final RGB WHITE = RGBColors.getRGBColor("white");

    private PgenRetrieveCommonDialogArea commonDialogArea = null;

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(this.getClass());

    private Text locationText;

    private Button fileBrowseBtn;

    private ColorWheelComp displayColorWheel;

    private Button monoColorBtn;

    private final boolean displayAsMap;

    private final Map<String, ActivityElement> dbEntries;

    public PgenStaticOverlayDialog(Shell parentShell, boolean displayAsMap) {
        super(parentShell, SWT.DIALOG_TRIM | SWT.MIN,
                CAVE.PERSPECTIVE_INDEPENDENT | CAVE.DO_NOT_BLOCK);
        setText("Load PGEN Static Overlay");

        this.displayAsMap = displayAsMap;

        dbEntries = new HashMap<>();
        ActivityCollection ac = new ActivityCollection();
        for (ActivityElement elem : ac.getCurrentActivityList()) {
            ActivityElement myElem = dbEntries.get(elem.getActivityLabel());
            if (myElem == null
                    || myElem.getRefTime().compareTo(elem.getRefTime()) > 0) {
                dbEntries.put(elem.getActivityLabel(), elem);
            }
        }
    }

    @Override
    protected void initializeComponents(Shell shell) {

        initializeLocationGroup();

        // Create the color wheel for the display.
        ColorData initialDisplay = new ColorData(WHITE);
        displayColorWheel = new ColorWheelComp(shell, " Display Color ", true);
        displayColorWheel.setColor(initialDisplay);

        monoColorBtn = new Button(shell, SWT.CHECK);
        GridData gridData = new GridData(SWT.DEFAULT, SWT.DEFAULT);
        monoColorBtn.setLayoutData(gridData);
        monoColorBtn.setText("Display Mono Color");
        monoColorBtn.setSelection(true);
        monoColorBtn.setToolTipText(
                "Override colors in PGEN products and display all in single color");

        // Create the bottom control buttons.
        createBottomButtons();
    }

    /**
     * Create the bottom control buttons.
     */
    private void createBottomButtons() {
        // Create a composite that will contain the control buttons.
        Composite bottonBtnComposite = new Composite(shell, SWT.NONE);
        GridLayout gl = new GridLayout(3, true);
        gl.horizontalSpacing = 10;
        bottonBtnComposite.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        bottonBtnComposite.setLayoutData(gd);

        // Create the OK button
        gd = new GridData(GridData.FILL_HORIZONTAL);
        Button okBtn = new Button(bottonBtnComposite, SWT.PUSH);
        okBtn.setText("OK");
        okBtn.setLayoutData(gd);
        okBtn.setEnabled(true);
        okBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                try {
                    loadStaticOverlay();
                    close();
                } catch (IOException | VizException e) {
                    statusHandler.handle(Priority.PROBLEM,
                            "Unable to load PGEN Static Overlay", e);
                }
            }
        });

        // Create the Cancel button
        gd = new GridData(GridData.FILL_HORIZONTAL);
        Button cancelBtn = new Button(bottonBtnComposite, SWT.PUSH);
        cancelBtn.setText("Cancel");
        cancelBtn.setLayoutData(gd);
        cancelBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                close();
            }
        });
    }

    protected IDisplayPane[] getSelectedPanes(IDisplayPaneContainer editor) {
        IDisplayPane[] displayPanes = editor.getDisplayPanes();

        if (editor instanceof IMultiPaneEditor) {
            IDisplayPane selected = ((IMultiPaneEditor) editor)
                    .getSelectedPane(IMultiPaneEditor.LOAD_ACTION);
            if (selected != null) {
                displayPanes = new IDisplayPane[] { selected };
            }
        }
        return displayPanes;
    }

    private void loadStaticOverlay() throws IOException, VizException {
        // Build bundle from dialog selections
        PgenStaticOverlayResourceData resourceData = new PgenStaticOverlayResourceData();
        String productName = "";
        // if file is selected and has a valid and not empty text use local file
        // loading
        if (locationText.isEnabled() && locationText.getText() != null
                && !locationText.getText().isEmpty()) {
            String filePath = locationText.getText();

            File pgenFile = new File(filePath);
            if (pgenFile.exists()) {
                resourceData.setPgenStaticProductLocation(pgenFile.getParent());
                productName = pgenFile.getName();
            } else {
                throw new FileNotFoundException(
                        "File '" + filePath + " does not exist.");
            }
        }
        // if DB
        else if (((IStructuredSelection) commonDialogArea.getFileListViewer()
                .getSelection()).getFirstElement() != null) {

            IStructuredSelection selection = (IStructuredSelection) commonDialogArea
                    .getFileListViewer().getSelection();

            if (selection.getFirstElement() instanceof ActivityElement) {
                ActivityElement elem = (ActivityElement) selection
                        .getFirstElement();
                productName = dbEntries.get(elem.getActivityLabel())
                        .getActivityLabel();
            }
        }

        if (productName.isEmpty()) {
            throw new IOException(
                    "Unable to determine PGEN activity from selected inputs.");
        }
        resourceData.setPgenStaticProductName(productName);
        resourceData.setMonoColorEnable(monoColorBtn.getSelection());
        resourceData.setColor(displayColorWheel.getColorData().rgbColor);
        resourceData.setIsEdited(true);

        ResourcePair resourcePair = new ResourcePair();
        resourcePair.setResourceData(resourceData);
        resourcePair.setLoadProperties(new LoadProperties());
        ResourceProperties properties = new ResourceProperties();
        properties.setVisible(true);
        properties.setRenderingOrderId("MAP_OUTLINE");
        properties.setMapLayer(displayAsMap);
        resourcePair.setProperties(properties);
        IMapDescriptor mapDescriptor;

        mapDescriptor = new MapDescriptor();
        mapDescriptor.getResourceList().add(resourcePair);
        MapRenderableDisplay display = new MapRenderableDisplay(mapDescriptor);
        Bundle bundle = new Bundle();
        bundle.setDisplays(new AbstractRenderableDisplay[] { display });

        IDisplayPaneContainer cont = EditorUtil.getActiveVizContainer();
        BundleProductLoader bpl = new BundleProductLoader(cont, bundle);
        bpl.schedule();
    }

    protected void initializeLocationGroup() {

        commonDialogArea = new PgenRetrieveCommonDialogArea();
        commonDialogArea.createComponents(shell);

        GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, true);
        Group locationGroup = new Group(shell, SWT.NONE);
        locationGroup.setLayoutData(gridData);
        locationGroup.setLayout(new GridLayout(2, false));
        locationGroup.setText("Load PGEN From");

        Button loadFromFileRdo = new Button(locationGroup, SWT.RADIO);
        loadFromFileRdo.setText("File");
        loadFromFileRdo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                locationText.setEnabled(true);
                fileBrowseBtn.setEnabled(true);
            }
        });

        Composite fileComposite = new Composite(locationGroup, SWT.NONE);
        fileComposite.setLayout(new GridLayout(2, false));
        locationText = new Text(fileComposite, SWT.BORDER);
        GC gc = new GC(locationText);
        gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        gridData.widthHint = gc.getFontMetrics().getAverageCharWidth() * 50;
        locationText.setLayoutData(gridData);
        locationText.setText("");
        locationText.setEnabled(false);
        fileBrowseBtn = new Button(fileComposite, SWT.PUSH);
        fileBrowseBtn.setText("Browse ...");
        fileBrowseBtn.setEnabled(false);
        fileBrowseBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                selectXMLFile();
            }
        });

    }

    protected void selectXMLFile() {
        FileDialog fileDialog = new FileDialog(this.shell, SWT.SAVE);
        File file = new File(locationText.getText());
        fileDialog.setFileName(file.getName());
        if ((file.getParentFile() != null)
                && file.getParentFile().isDirectory()) {
            fileDialog.setFilterPath(file.getParent());
        }

        String path = fileDialog.open();
        if (path != null) {
            this.locationText.setText(path);
        }
    }
}
