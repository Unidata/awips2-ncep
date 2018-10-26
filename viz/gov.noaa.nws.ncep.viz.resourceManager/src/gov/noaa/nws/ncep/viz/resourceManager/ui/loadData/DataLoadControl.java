package gov.noaa.nws.ncep.viz.resourceManager.ui.loadData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.drawables.AbstractRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.capabilities.Capabilities;
import com.raytheon.viz.ui.UiPlugin;
import com.raytheon.viz.ui.editor.AbstractEditor;

import gov.noaa.nws.ncep.viz.common.display.INcPaneID;
import gov.noaa.nws.ncep.viz.common.display.INcPaneLayout;
import gov.noaa.nws.ncep.viz.common.display.NcDisplayType;
import gov.noaa.nws.ncep.viz.resourceManager.timeline.GraphTimelineControl;
import gov.noaa.nws.ncep.viz.resourceManager.timeline.TimelineControl;
import gov.noaa.nws.ncep.viz.resourceManager.timeline.TimelineControl.IDominantResourceChangedListener;
import gov.noaa.nws.ncep.viz.resourceManager.timeline.cache.TimeSettingsCacheManager;
import gov.noaa.nws.ncep.viz.resourceManager.ui.loadData.DataSelectionControl.IResourceSelectedListener;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResourceData;
import gov.noaa.nws.ncep.viz.resources.attributes.EditResourceAttrsDialogFactory;
import gov.noaa.nws.ncep.viz.resources.groupresource.GroupResourceData;
import gov.noaa.nws.ncep.viz.resources.manager.AbstractRBD;
import gov.noaa.nws.ncep.viz.resources.manager.AttributeSet;
import gov.noaa.nws.ncep.viz.resources.manager.NcMapRBD;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceBndlLoader;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceDefinition;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceDefnsMngr;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceFactory;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceFactory.ResourceSelection;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceName;
import gov.noaa.nws.ncep.viz.resources.manager.RscBundleDisplayMngr;
import gov.noaa.nws.ncep.viz.resources.time_match.NCTimeMatcher;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;
import gov.noaa.nws.ncep.viz.ui.display.NcEditorUtil;

/**
 * Creates the Resource Manager's Data Selection dialog.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 01/26/10       #226       Greg Hull   Broke out and refactored from ResourceMngrDialog
 * 04/27/10       #245       Greg Hull   Added Apply Button
 * 06/13/10       #273       Greg Hull   RscBndlTemplate->ResourceSelection, use ResourceName
 * 07/14/10       #273       Greg Hull   remove Select Overlay list (now in ResourceSelection)
 * 07/21/10       #273       Greg Hull   add un-implemented Up/Down/OnOff buttons
 * 08/18/10       #273       Greg Hull   implement Clear RBD button
 * 08/23/10       #303       Greg Hull   changes from workshop
 * 09/01/10       #307       Greg Hull   implement auto update
 * 01/25/11                  Greg Hull   fix autoImport of current Display, autoImport
 *                                       of reprojected SAT area, 
 * 02/11/11       #408       Greg Hull   Move Clear to Clear Pane; Add Load&Close btn                                 
 * 02/22/11       #408       Greg Hull   allow for use by EditRbdDialog
 * 06/07/11       #445       Xilin Guo   Data Manager Performance Improvements
 * 07/11/11                  Greg Hull   Rm code supporting 'Save All to SPF' capability.
 * 08/20/11       #450       Greg Hull   Use new SpfsManager
 * 10/22/11       #467       Greg Hull   Add Modify button
 * 11/03/11       #???       B. Hebbard  Add "Save Source Timestamp As:" Constant / Latest 
 * 02/15/2012     627        Archana     Updated the call to addRbd() to accept 
 *                                       a NCMapEditor object as one of the arguments
 * 04/26/2012     #766       Quan Zhou   Modified rscSelDlg listener for double click w. existing rsc--close the dlg.
 * 04/03/2012     #765       S. Gurung   Modified method importRBD to change the display when a RBD is imported
 * 05/17/2012     #791       Quan Zhou   Added getDefaultRbdRsc() to get name and rsc from original defaultRbd.xml
 *                                       Modified LoadRBD to check if default editor is empty, then replace it.
 *                                       findCloseEmptyEdotor() is ready but not used now.
 * 06/18/2012     #624       Greg Hull   set size correctly when initially importing mult-pane
 * 06/18/2012     #713       Greg Hull   clone the RbdBundl when importing
 * 06/20/2012     #647       Greg Hull   dont call selectDominantResource() after importRbd.
 * 06/20/2012                S. Gurung   Fix for TTR# 539 (Auto-update checkbox gets reset to OFF)
 * 06/21/2012     #646       Greg Hull   import full PredefinedArea instead of just the name.
 * 06/28/2012     #824       Greg Hull   update the importRbdCombo in the ActivateListener.
 * 08/01/2012     #836       Greg Hull   check for paneLayout when using empty editor
 * 08/02/2012     #568       Greg Hull   Clear Rbd -> Reset Rbd. get the Default RBD
 * 11/16/2012     #630       Greg Hull   Allow selection of resource-defined areas.
 * 12/02/2012     #630       Greg Hull   add zoomLevel combo
 * 01/24/2013     #972       Greg Hull   add NcDisplayType to support NonMap and solar based RBDs
 * 05/24/2013     #862       Greg Hull   get areas from menus file and change combo to a cascading menu
 * 06/03/2013     #1001      Greg Hull   allow multiple Remove/TurnOff of resources
 * 10/22/2013     #1043      Greg Hull   setSelectedResource() if rsc sel dlg is already up.
 * 11/25/2013     #1079      Greg Hull   adjust size/font of area toolbar based on the text
 * 05/07/2014     TTR991     D. Sushon   if a different NCP editor is selected, the CreateRDB tab should now adjust.
 * 05/29/2014     #1131      qzhou       Added NcDisplayType
 *                                       Modified creating new timelineControl in const and updateGUI
 * 08/14/2014       ?        B. Yin      Added power legend (resource group) support.
 * 09/092014        ?        B. Yin      Fixed NumPad enter issue and the "ResetToDefault" issue for groups. 
 * 07/28/2014     R4079      sgurung     Fixed the issue related to CreateRbd dialog size (bigger than usual).
 *                                       Also, added code to set geosync to true for graphs.
 * 11/12/2015     R8829      B. Yin      Implemented up/down arrows to move a resource in list.
 * 01/14/2016     R14896     J. Huber    Repair Replace Resource button which was broken during cleanup of
 *                                       previous change.
 * 01/01/2016     R14142    RCReynolds   Reformatted Mcidas resource string
 * 01/25/2016     R14142    RCReynolds   Moved mcidas related sting construction out to ResourceDefinition
 * 02/16/2016     R15244    bkowal       Cleaned up warnings.
 * 04/05/2016     R15715    dgilling     Refactored out PopupEditAttrsDialog and associated methods.
 * 06/20/2016     R8878     J. Lopez     Changed createAvailAreaMenuItems() to use AreaMenuTree 
 *                                       Auto selects the new group when creating a new resource group
 *                                       Renamed variables to be CamelCase
 * 08/25/2016     R15518    Jeff Beck    Added check for isDisposed() before calling refresh() in editResourceData()
 * 10/20/2016     R17365    K.Bugenhagen Added call to SpfsManager method to remember
 *                                       selected spf group/name and RBD
 *                                       name in between calls to save RBD dialogue.  
 *                                       Cleanup.
 * 11/14/2016     R17362    Jeff Beck    Added functionality to remove user defined resources from the dominant resource combo
 *                                       when clicking the "X" on the GUI.
 * </pre>
 * 
 * @author ghull
 * @version 1
 */

public class DataLoadControl extends Composite implements IPartListener2 {

    //private ResourceSelectionDialog rscSelDlg = null;

    private RscBundleDisplayMngr rbdMngr;

    private Shell shell;

    private SashForm sashForm = null;
    
    private Group selectedResourceGroup = null;

    private Button geoSyncPanesToggle = null;

    private ListViewer selectedResourceViewer = null;

    private Button editResourceButton = null;

    private Button deleteResourceButton = null;

    private Button moveResourceUpButton = null;

    private Button moveResourceDownButton = null;

    private Button loadRbdButton = null;

    private Button loadAndCloseButton = null;

    private Button clearRbdButton = null;

    // when part of the 'Edit Rbd' dialog these will replace the Clear, Save,
    // and Load buttons

    private Button cancelEditButton = null;

    private Button okEditButton = null;

    // set on OK when this is an 'Edit Rbd' dialog

    private AbstractRBD<?> editedRbd = null;

    private String savedRbdName;

    private Point initDlgSize = new Point(800, 900);

    private TimelineControl timelineControl = null;

    private Group timelineGroup;

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(DataLoadControl.class);
    
    private DataSelectionControl sel_rsc_cntrl = null;
    
    ResourceName initRscName;

    // the rbdMngr will be used to set the gui so it should either be
    // initialized/cleared or set with the initial RBD.
    public DataLoadControl(Composite parent, RscBundleDisplayMngr mngr)
            throws VizException {
        super(parent, SWT.NONE);
        shell = parent.getShell();

        rbdMngr = mngr;

        //rscSelDlg = new ResourceSelectionDialog(shell);

        Composite top_comp = this;
        top_comp.setLayout(new GridLayout(1, true));

        sashForm = new SashForm(top_comp, SWT.VERTICAL);
        GridData gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;

        sashForm.setLayoutData(gd);
        sashForm.setSashWidth(10);
        
        Group sel_rscs_grp = new Group(sashForm, SWT.SHADOW_NONE);
        sel_rscs_grp.setLayout(new GridLayout(1, true));

        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        sel_rscs_grp.setLayoutData(gd);

        sel_rsc_cntrl = new DataSelectionControl(sel_rscs_grp,
        		initRscName, NcDisplayType.NMAP_DISPLAY);

        selectedResourceGroup = new Group(sashForm, SWT.SHADOW_NONE);
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;

        selectedResourceGroup.setLayoutData(gd);

        selectedResourceGroup.setLayout(new FormLayout());

        createRBDGroup();

        StructuredSelection sel_elems = (StructuredSelection) selectedResourceViewer
                .getSelection();
        List<ResourceSelection> seldRscsList = sel_elems.toList();
        int numSeldRscs = selectedResourceViewer.getList()
                .getSelectionCount();

        Boolean isBaseLevelRscSeld = false;

        ResourceName initRscName = null;

        for (ResourceSelection rscSel : seldRscsList) {
            isBaseLevelRscSeld |= rscSel.isBaseLevelResource();
            if (initRscName == null && !rscSel.isBaseLevelResource()) {
                initRscName = rscSel.getResourceName();
            }
        }

        if (initRscName != null) {
            setSelectedResource(initRscName);
        }

        timelineGroup = new Group(sashForm, SWT.SHADOW_NONE);
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        timelineGroup.setLayoutData(gd);

        timelineGroup.setLayout(new GridLayout());

        if (mngr.getRbdType().equals(NcDisplayType.GRAPH_DISPLAY)) {
            timelineControl = new GraphTimelineControl(timelineGroup);
        } else {
            timelineControl = new TimelineControl(timelineGroup);
        }

        timelineControl.addDominantResourceChangedListener(
                new IDominantResourceChangedListener() {
                    @Override
                    public void dominantResourceChanged(
                            AbstractNatlCntrsRequestableResourceData newDomRsc) {
                        if (newDomRsc.isAutoUpdateable()) {
                           
                            if (rbdMngr.getRbdType()
                                    .equals(NcDisplayType.GRAPH_DISPLAY)) {
                                geoSyncPanesToggle.setSelection(true);
                                rbdMngr.syncPanesToArea();
                            }
                        }
                    }
                });

        timelineControl.setTimeMatcher(new NCTimeMatcher());

        Composite loadSaveComp = new Composite(top_comp, SWT.NONE);
        gd = new GridData();
        gd.minimumHeight = 40;
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;

        loadSaveComp.setLayoutData(gd);

        loadSaveComp.setLayout(new FormLayout());

        clearRbdButton = new Button(loadSaveComp, SWT.PUSH);
        clearRbdButton.setText(" Clear ");
        FormData fd = new FormData();
        fd.width = 130;
        fd.top = new FormAttachment(0, 7);
        fd.left = new FormAttachment(17, -65);
        clearRbdButton.setLayoutData(fd);

        loadRbdButton = new Button(loadSaveComp, SWT.PUSH);
        loadRbdButton.setText("Load ");
        fd = new FormData();
        fd.width = 100;
        fd.top = new FormAttachment(0, 7);
        fd.left = new FormAttachment(40, -50);
        loadRbdButton.setLayoutData(fd);

        loadAndCloseButton = new Button(loadSaveComp, SWT.PUSH);
        loadAndCloseButton.setText("Load and Close");
        fd = new FormData();
        fd.width = 120;
        fd.top = new FormAttachment(0, 7);
        fd.left = new FormAttachment(63, -50);
        loadAndCloseButton.setLayoutData(fd);

        cancelEditButton = new Button(loadSaveComp, SWT.PUSH);
        cancelEditButton.setText(" Cancel ");
        fd = new FormData();
        fd.width = 80;
        fd.top = new FormAttachment(0, 7);
        fd.right = new FormAttachment(45, 0);
        cancelEditButton.setLayoutData(fd);

        okEditButton = new Button(loadSaveComp, SWT.PUSH);
        okEditButton.setText("   Ok   ");
        fd = new FormData();
        fd.width = 80;
        fd.top = new FormAttachment(0, 7);
        fd.left = new FormAttachment(55, 0);
        okEditButton.setLayoutData(fd);

        // only visible if configureForEditRbd is called
        cancelEditButton.setVisible(false);
        okEditButton.setVisible(false);

        sashForm.setWeights(new int[] { 45, 20, 35 });

        // set up the content providers for the ListViewers
        setContentProviders();
        
        addSelectionListeners();

        initWidgets();

        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
                .addPartListener(this);
        this.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                try {
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                            .getActivePage()
                            .removePartListener(DataLoadControl.this);
                    // now mark for disposal
                    DataLoadControl.this.dispose();
                } catch (NullPointerException npe) {
                    // do nothing if already disposed, another thread already
                    // swept it up..
                }
            }
        });

    }

    // create all the widgets in the Resource Bundle Definition (bottom) section
    // of the sashForm.
    private void createRBDGroup() {

        geoSyncPanesToggle = new Button(selectedResourceGroup, SWT.CHECK);
        FormData form_data = new FormData();
        form_data.left = new FormAttachment(0, 15);
        form_data.top = new FormAttachment(0, 30);
        form_data.right = new FormAttachment(24, 0);
        geoSyncPanesToggle.setText("Geo-Sync Panes");
        geoSyncPanesToggle.setLayoutData(form_data);

        createSeldRscsGroup();

    }

    // create the Selected Resources List, the Edit, Delete and Clear buttons
    private void createSeldRscsGroup() {

        // This is multi-select to make Deleting resources easier.
        selectedResourceViewer = new ListViewer(selectedResourceGroup,
                SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        FormData form_data = new FormData();
        form_data.top = new FormAttachment(0, 5);
        form_data.left = new FormAttachment(0, 5);
        form_data.right = new FormAttachment(100, -95);// -110 ); //80, 0 );
        form_data.bottom = new FormAttachment(100, -10);
        selectedResourceViewer.getList().setLayoutData(form_data);

        editResourceButton = new Button(selectedResourceGroup, SWT.PUSH);
        editResourceButton.setText("Edit");
        form_data = new FormData();
        form_data.width = 85;
        form_data.top = new FormAttachment(0, 5);
        form_data.right = new FormAttachment(100, -5);

        editResourceButton.setLayoutData(form_data);
        editResourceButton.setEnabled(false);

        deleteResourceButton = new Button(selectedResourceGroup, SWT.PUSH);
        deleteResourceButton.setText("Remove");
        form_data = new FormData();
        form_data.width = 85;
        form_data.top = new FormAttachment(editResourceButton, 5, SWT.BOTTOM);
        form_data.right = new FormAttachment(100, -5);
        deleteResourceButton.setLayoutData(form_data);
        deleteResourceButton.setEnabled(false);

        moveResourceDownButton = new Button(selectedResourceGroup,
                SWT.ARROW | SWT.DOWN);
        moveResourceDownButton.setToolTipText("Move Down");
        form_data = new FormData();
        form_data.width = 35;
        form_data.top = new FormAttachment(deleteResourceButton, 5, SWT.BOTTOM);
        form_data.right = new FormAttachment(100, -5);
        moveResourceDownButton.setLayoutData(form_data);
        moveResourceDownButton.setEnabled(false);

        moveResourceDownButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {

                StructuredSelection resources = ((StructuredSelection) selectedResourceViewer
                        .getSelection());
                ResourceSelection resSelected = (ResourceSelection) resources
                        .getFirstElement();

                if (resSelected != null) {

                    rbdMngr.moveDownResource(resSelected, null);

                    selectedResourceViewer
                            .setInput(rbdMngr.getUngroupedResources());
                    selectedResourceViewer.refresh();
                    selectedResourceViewer.setSelection(resources);
                    
                }
            }
        });

        moveResourceUpButton = new Button(selectedResourceGroup, SWT.ARROW | SWT.UP);
        moveResourceUpButton.setToolTipText("Move Up");
        form_data = new FormData();
        form_data.width = 35;
        form_data.top = new FormAttachment(moveResourceDownButton, 0, SWT.TOP);
        form_data.left = new FormAttachment(deleteResourceButton, 0, SWT.LEFT);
        moveResourceUpButton.setLayoutData(form_data);
        moveResourceUpButton.setEnabled(false);

        moveResourceUpButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {

                StructuredSelection resources = ((StructuredSelection) selectedResourceViewer
                        .getSelection());
                ResourceSelection resSelected = (ResourceSelection) resources
                        .getFirstElement();

                if (resSelected != null) {

                    rbdMngr.moveUpResource(resSelected, null);

                    selectedResourceViewer
                            .setInput(rbdMngr.getUngroupedResources());
                    selectedResourceViewer.refresh();
                    selectedResourceViewer.setSelection(resources);

                }
            }
        });

    }

    private void setContentProviders() {

        selectedResourceViewer
                .setContentProvider(new IStructuredContentProvider() {
                    @Override
                    public void dispose() {
                    }

                    @Override
                    public void inputChanged(Viewer viewer, Object oldInput,
                            Object newInput) {
                    }

                    @Override
                    public Object[] getElements(Object inputElement) {
                        return ((ResourceSelection[]) inputElement);
                    }
                });

        // get the full path of the attr file and then remove .prm extension
        // and the prefix path up to the cat directory.
        selectedResourceViewer.setLabelProvider(new LabelProvider() {
            @Override
            public String getText(Object element) {
                ResourceSelection rscSel = (ResourceSelection) element;
                return rscSel.getRscLabel();
            }
        });

        updateSelectedResourcesView(true);

        // enable/disable the Edit/Delete/Clear buttons...
        selectedResourceViewer
                .addSelectionChangedListener(new ISelectionChangedListener() {
                    @Override
                    public void selectionChanged(SelectionChangedEvent event) {
                        updateSelectedResourcesView(false);
                    }
                });

        selectedResourceViewer.getList().addListener(SWT.MouseDoubleClick,
                new Listener() {
                    @Override
                    public void handleEvent(Event event) {
                        editResourceData();
                    }
                });

    }

    // add all of the listeners for widgets on this dialog
    void addSelectionListeners() {


        sel_rsc_cntrl.addResourceSelectionListener(new IResourceSelectedListener() {
        	            @Override
        	            public void resourceSelected(ResourceName rscName, boolean replace,
        	                    boolean addAllPanes, boolean done) {
        	
        	                try {
        	                    ResourceSelection rbt = ResourceFactory
        	                            .createResource(rscName);
        	
        	                    rbdMngr.getSelectedArea();
      
    	
    	                        if (addAllPanes) {
    	                            rbdMngr.addSelectedResourceToAllPanes(rbt);
    	                        } else {
    	                            if (!rbdMngr.addSelectedResource(rbt, null)) {
    	                                selectedResourceViewer.refresh(true);
    	                            }
    	                        }
    	                    
        	
        	                    // add the new resource to the timeline as a possible
        	                    // dominant resource
        	                    if (rbt.getResourceData() instanceof AbstractNatlCntrsRequestableResourceData) {
        	                        timelineControl.addAvailDomResource(
        	                                (AbstractNatlCntrsRequestableResourceData) rbt
        	                                        .getResourceData());
        	
        	                        // if there is not a dominant resource selected then
        	                        // select this one
        	                        if (timelineControl.getDominantResource() == null) {
        	                            timelineControl
        	                                    .setDominantResource(
        	                                            (AbstractNatlCntrsRequestableResourceData) rbt
        	                                                    .getResourceData(),
        	                                            replace);
        	                        }
        	                    }
        	                } catch (VizException e) {
        	                    statusHandler.handle(Priority.PROBLEM,
        	                            "Error Adding Resource to List: " + e.getMessage());
        	                    MessageDialog errDlg = new MessageDialog(shell, "Error",
        	                            null,
        	                            "Error Creating Resource:" + rscName.toString()
        	                                    + "\n\n" + e.getMessage(),
        	                            MessageDialog.ERROR, new String[] { "OK" }, 0);
        	                    errDlg.open();
        	                }
        	
        	                updateSelectedResourcesView(true);
        	

        	            }
        	        });
        // if syncing the panes
        geoSyncPanesToggle.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {

                if (geoSyncPanesToggle.getSelection()) {

                    MessageDialog confirmDlg = new MessageDialog(shell,
                            "Confirm Geo-Sync Panes", null,
                            "This will set the Area for all panes to the currently\n"
                                    + "selected Area: "
                                    + rbdMngr.getSelectedArea()
                                            .getProviderName()
                                    + "\n\n" + "Continue?",
                            MessageDialog.QUESTION,
                            new String[] { "Yes", "No" }, 0);
                    confirmDlg.open();

                    if (confirmDlg.getReturnCode() != MessageDialog.OK) {
                        geoSyncPanesToggle.setSelection(false);
                        return;
                    }

                    rbdMngr.syncPanesToArea();
                } else {
                    rbdMngr.setGeoSyncPanes(false);
                }
            }
        });

        // only 1 should be selected or this button should be greyed out
        editResourceButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                editResourceData();
            }
        });

        deleteResourceButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent ev) {
                StructuredSelection sel_elems = (StructuredSelection) selectedResourceViewer
                        .getSelection();
                Iterator<?> itr = sel_elems.iterator();

                // note: the base may be selected if there are multi selected
                // with others.
                while (itr.hasNext()) {
                    ResourceSelection rscSel = (ResourceSelection) itr.next();
                    if (!rscSel.isBaseLevelResource()) {
                        removeSelectedResource(rscSel);
                    }
                }

                updateSelectedResourcesView(true);

            }
        });

        clearRbdButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                clearRBD();
            }
        });

        loadRbdButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                loadRBD(false);
            }
        });

        loadAndCloseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                loadRBD(true);
            }
        });

    }
    
    public ResourceName getPrevSelectedResource() {
        return (sel_rsc_cntrl != null ? sel_rsc_cntrl.getPrevSelectedResource()
                : new ResourceName());
    }

    public void setSelectedResource(ResourceName rscName) {
        initRscName = rscName;
        sel_rsc_cntrl.initWidgets(initRscName);
    }
    
    // import the current editor or initialize the widgets.

    public void initWidgets() {

        shell.setSize(initDlgSize);

        updateGUIforMultipane(rbdMngr.isMultiPane());
        
        timelineControl.clearTimeline();

    }

    

    
    public void configureForEditRbd() {
        clearRbdButton.setVisible(false);
        loadRbdButton.setVisible(false);
        loadAndCloseButton.setVisible(false);
        cancelEditButton.setVisible(true);
        okEditButton.setVisible(true);
        cancelEditButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                editedRbd = null;
                shell.dispose();
            }
        });

        // dispose and leave the edited RBD in
        okEditButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                createEditedRbd();
                shell.dispose();
            }
        });

        timelineControl.getParent().setVisible(false);
        sashForm.setWeights(new int[] { 10, 1 });
        shell.setSize(shell.getSize().x - 100, 350);
        shell.pack(true);
    }

    // called when the user switches to this tab in the ResourceManagerDialog or
    // when the EditRbd Dialog initializes
    public void updateDialog() {
    	
        // If the gui has not been set with the current rbdMngr then do it now.
        updateGUI();

        // updateGUI triggers the spinner which ends up calling
        // rbdMngr.setPaneLayout(), so we need to reset this here.
        rbdMngr.setRbdModified(false);
    }

    // set widgets based on rbdMngr
    public void updateGUI() {

        TimeSettingsCacheManager.getInstance()
                .updateCacheLookupKey(rbdMngr.getRbdName());

        // create new GraphTimelineControl if loading a Graph Display
        timelineControl.dispose();
        if (rbdMngr.getRbdType().equals(NcDisplayType.GRAPH_DISPLAY)) {

            timelineControl = new GraphTimelineControl(timelineGroup);
        } else {
            timelineControl = new TimelineControl(timelineGroup);
        }

        timelineControl.addDominantResourceChangedListener(
                new IDominantResourceChangedListener() {
                    @Override
                    public void dominantResourceChanged(
                            AbstractNatlCntrsRequestableResourceData newDomRsc) {
                            if (rbdMngr.getRbdType()
                                    .equals(NcDisplayType.GRAPH_DISPLAY)) {
                                geoSyncPanesToggle.setSelection(true);
                                rbdMngr.syncPanesToArea();
                            }
                        
                    }
                });

        timelineGroup.pack();
        shell.pack();
        shell.setSize(initDlgSize);

        geoSyncPanesToggle.setSelection(rbdMngr.isGeoSyncPanes());

        updateGUIforMultipane(rbdMngr.isMultiPane());

        updateSelectedResourcesView(true);

        INcPaneLayout paneLayout = rbdMngr.getPaneLayout();

        // set the list of available resources for the timeline
        for (int paneIndx = 0; paneIndx < paneLayout
                .getNumberOfPanes(); paneIndx++) {

            for (ResourceSelection rscSel : rbdMngr
                    .getRscsForPane(paneLayout.createPaneId(paneIndx))) {

                if (rscSel.getResourceData() instanceof GroupResourceData) {
                    for (ResourcePair pair : ((GroupResourceData) rscSel
                            .getResourceData()).getResourceList()) {
                        if (pair.getResourceData() instanceof AbstractNatlCntrsRequestableResourceData) {
                            timelineControl.addAvailDomResource(
                                    (AbstractNatlCntrsRequestableResourceData) pair
                                            .getResourceData());
                        }
                    }
                } else if (rscSel
                        .getResourceData() instanceof AbstractNatlCntrsRequestableResourceData) {
                    timelineControl.addAvailDomResource(
                            (AbstractNatlCntrsRequestableResourceData) rscSel
                                    .getResourceData());
                }
            }
        }

        NCTimeMatcher timeMatcher = rbdMngr.getInitialTimeMatcher();

        timelineControl.setTimeMatcher(timeMatcher);

    }



    public void removeSelectedResource(ResourceSelection rscSel) {
    	
        rbdMngr.removeSelectedResource(rscSel);

        // remove this from the list of available dominant resources.
        if (rscSel.getResourceData() instanceof AbstractNatlCntrsRequestableResourceData) {
            timelineControl
                    .removeAvailDomResource((AbstractNatlCntrsRequestableResourceData) rscSel
                            .getResourceData());
        }

    }


    // Listener callback for the Edit Resource button and dbl click on the list
    public void editResourceData() {
        StructuredSelection sel_elems = (StructuredSelection) selectedResourceViewer
                .getSelection();
        ResourceSelection rscSel = (ResourceSelection) sel_elems
                .getFirstElement();

        if (rscSel == null) {
            statusHandler.handle(Priority.INFO, "no resource is selected");
            return;
        }

        Capabilities capObj = new Capabilities();

        ResourcePair rp = rscSel.getResourcePair();

        // getLoadProperties() is not null safe but getCapabilities() is, thus
        // only one check is needed.

        if (rp.getLoadProperties() != null) {
            capObj = rp.getLoadProperties().getCapabilities();
        }
        INatlCntrsResourceData rscData = rscSel.getResourceData();
        if (rscData == null) {
            statusHandler.handle(Priority.INFO,
                    "seld resource is not a INatlCntrsResource");
            return;
        }

        EditResourceAttrsDialogFactory factory = new EditResourceAttrsDialogFactory();
        try {

            ResourceDefnsMngr rscDefnsMngr = ResourceDefnsMngr.getInstance();

            ResourceDefinition rscDefn = rscDefnsMngr.getResourceDefinition(
                    rscData.getResourceName().getRscType());

            AttributeSet attSet = rscDefnsMngr
                    .getAttrSet(rscSel.getResourceName());

            rscDefn.setAttributeSet(attSet);

            String displayName = rscSel.getResourceName().toString()
                    .split("/")[2];

            displayName = rscDefn.getRscGroupDisplayName(displayName);

            String updatedAttrSetName = rscDefn.getRscAttributeDisplayName("");
            // add on aliased attribute name

            // simply change the popup title
            if (!updatedAttrSetName.isEmpty()) {
                displayName += " " + updatedAttrSetName;
                factory.setTitle(displayName);
            }

        } catch (VizException e) {
            statusHandler.handle(Priority.PROBLEM, e.getMessage());
        }

        factory.setShell(shell).setResourceData(rscData).setCapabilities(capObj)
                .setApplyBtn(false);
        if (factory.construct()) {
            rbdMngr.setRbdModified(true);
        }

        // display modified (edited) name
        if (!selectedResourceViewer.getControl().isDisposed()) {
            selectedResourceViewer.refresh(true);
        }
    }

    public void clearSeldResources() {
        // remove the requestable resources from the timeline
        for (ResourceSelection rbt : rbdMngr.getSelectedRscs()) {
            if (rbt.getResourceData() instanceof AbstractNatlCntrsRequestableResourceData) {
                timelineControl.removeAvailDomResource(
                        (AbstractNatlCntrsRequestableResourceData) rbt
                                .getResourceData());
            }
        }

        rbdMngr.removeAllSelectedResources();

        // TODO : should this reset the rbd to not modified if single pane? No
        // since the num panes and/or the area could have changed. clearRbd will
        // reset the modified flag

        updateSelectedResourcesView(true);
    }

    // reset all panes. This includes 'hidden' panes which may have resources
    // selected but are hidden because the user has changed to a smaller layout.
    public void clearRBD() {
        // TODO : ? reset the predefined area to the default???

        if (rbdMngr.isMultiPane()) {
            MessageDialog confirmDlg = new MessageDialog(shell, "Confirm", null,
                    "The will remove all resources selections\n"
                            + "from all panes in this RBD. \n\n"
                            + "Are you sure you want to do this?",
                    MessageDialog.QUESTION, new String[] { "Yes", "No" }, 0);
            confirmDlg.open();

            if (confirmDlg.getReturnCode() != MessageDialog.OK) {
                return;
            }
        }

        NcDisplayType curDispType = rbdMngr.getRbdType();

        try {
            AbstractRBD<?> dfltRbd = NcMapRBD.getDefaultRBD(curDispType);

            rbdMngr.initFromRbdBundle(dfltRbd);

        } catch (VizException e) {
            MessageDialog errDlg = new MessageDialog(shell, "Error", null,
                    e.getMessage(), MessageDialog.ERROR, new String[] { "OK" },
                    0);
            errDlg.open();

            rbdMngr.init(curDispType);
        }

        TimeSettingsCacheManager.getInstance().reset();
        updateGUI();

        selectedResourceViewer.setInput(rbdMngr.getUngroupedResources());
        selectedResourceViewer.refresh();

    }

    // reset the ListViewer's input and update all of the buttons
    // TODO; get the currently selected resources and reselect them.
    private void updateSelectedResourcesView(boolean updateList) {

        if (updateList) {

            StructuredSelection orig_sel_elems = (StructuredSelection) selectedResourceViewer
                    .getSelection();

            List<?> origSeldRscsList = orig_sel_elems.toList();

            selectedResourceViewer
                    .setInput(rbdMngr.getUngroupedResources());

            selectedResourceViewer.refresh(true);

            List<ResourceSelection> newSeldRscsList = new ArrayList<>();

            // create a new list of selected elements
            for (Object object : origSeldRscsList) {
                ResourceSelection rscSel = (ResourceSelection) object;
                for (int r = 0; r < selectedResourceViewer.getList()
                        .getItemCount(); r++) {
                    if (rscSel == selectedResourceViewer.getElementAt(r)) {
                        newSeldRscsList.add(rscSel);
                        break;
                    }
                }
            }
            selectedResourceViewer.setSelection(
                    new StructuredSelection(newSeldRscsList.toArray()), true);
        }

        int numSeldRscs = selectedResourceViewer.getList().getSelectionCount();
        int numRscs = selectedResourceViewer.getList().getItemCount();

        // the edit button is enabled iff there is only one selected resource.
        editResourceButton.setEnabled(numSeldRscs == 1);

        StructuredSelection sel_elems = (StructuredSelection) selectedResourceViewer
                .getSelection();
        List<ResourceSelection> seldRscsList = sel_elems.toList();

        // Can't delete, replace or turn off the base overlay.
        Boolean isBaseLevelRscSeld = false;
        Boolean allRscsAreVisible = true;

        for (ResourceSelection rscSel : seldRscsList) {
            isBaseLevelRscSeld |= rscSel.isBaseLevelResource();

            allRscsAreVisible &= rscSel.isVisible();
        }

        boolean firstSelected = false;
        boolean lastSelected = false;

        if (numSeldRscs == 1) {
            firstSelected = (seldRscsList.get(0) == selectedResourceViewer
                    .getElementAt(0));
            lastSelected = (seldRscsList.get(0) == selectedResourceViewer
                    .getElementAt(numRscs - 1));
        }

        // the delete button is only disabled if there is one the one base
        // resource selected.
        deleteResourceButton.setEnabled(
                numSeldRscs > 1 || (numSeldRscs == 1 && !isBaseLevelRscSeld));

        moveResourceDownButton.setEnabled((numSeldRscs == 1) && !lastSelected);
        moveResourceUpButton.setEnabled((numSeldRscs == 1) && !firstSelected);

    }

    // This will load the currently configured RBD (all panes) into
    // a new editor or the active editor if the name matches the current
    // name of the RBD
    public void loadRBD(boolean close) {

        String rbdName = "GEMPAK";

        // Since rbdLoader uses the same resources in the rbdBundle to load in
        // the editor, we will need to make a copy here so that future edits are
        // not immediately reflected in the loaded display. The easiest way to
        // do this is to marshal and then unmarshal the rbd.
        try {

            //rbdMngr.setGeoSyncPanes(geoSyncPanesToggle.getSelection());
            //rbdMngr.setAutoUpdate(true);

            AbstractRBD<?> rbdBndl = rbdMngr.createRbdBundle(rbdName, timelineControl.getTimeMatcher());

            rbdBndl = AbstractRBD.clone(rbdBndl);
            rbdBndl.resolveDominantResource();

            ResourceBndlLoader rbdLoader = null;
            rbdLoader = new ResourceBndlLoader("RBD Previewer");

            AbstractEditor editor = null;
            //AbstractEditor editor = (AbstractEditor) EditorUtil.getActiveEditor();
            // Use the active editor
            
            

            // 1-if the active editor matches this rbdName, use the active
            // editor
            // Don't attempt to find an editor based just on the display name
            // since they may not be unique without the display id.
            if (editor == null) {
                editor = NcDisplayMngr.getActiveNatlCntrsEditor();

                if (!NcEditorUtil.getDisplayName(editor).getName()
                        .equals(rbdName)) {
                    editor = null;
                }
                // if they changed the layout then we should be able to modify
                // the layout of the current
                // / display but for now just punt and get a new editor.
                else if (NcEditorUtil.getPaneLayout(editor)
                        .compare(rbdBndl.getPaneLayout()) != 0) {
                    statusHandler.handle(Priority.INFO,
                            "Creating new Editor because the paneLayout differs.");
                    editor = null;
                }
            }

            // 2- look for an available editor that has not been modified.
            // This is usually the initial 'default' editor but could be one
            // that has been
            // cleared/wiped, or one from the New Display menu.
            if (editor == null) {

                editor = NcDisplayMngr
                        .findUnmodifiedDisplayToLoad(rbdBndl.getDisplayType());
                if (editor != null && NcEditorUtil.getPaneLayout(editor)
                        .compare(rbdBndl.getPaneLayout()) != 0) {
                    statusHandler.handle(Priority.INFO,
                            "Creating new Editor because the paneLayout differs.");
                    editor = null;
                }
            }

            // 4-reuse if it is a Preview editor.
            if (editor == null && rbdName.equals("GEMPAK")) {
                editor = NcDisplayMngr
                        .findDisplayByName(rbdBndl.getDisplayType(), rbdName);

                if (editor != null && NcEditorUtil.getPaneLayout(editor)
                        .compare(rbdBndl.getPaneLayout()) != 0) {
                    editor = null;
                }
            }

            // 5-Create a new one.
            // Note that it is possible for a display name matching the RBD name
            // to not be reused if the display is not active. This is because
            // the names may not be unique and so we can't determine which
            // unless it was specifically imported into the Manager and even in
            // this case they may not want to re-load it.
            if (editor == null) {
                editor = NcDisplayMngr.createNatlCntrsEditor(
                        rbdBndl.getDisplayType(), rbdName,
                        rbdBndl.getPaneLayout());
            }

            if (editor == null) {
                throw new VizException(
                        "Unable to create a display to load the RBD.");
            }

            NcDisplayMngr.bringToTop(editor);

            // Assign hot keys to group resources. Set visible for the selected
            // group.

            for (AbstractRenderableDisplay rendDisp : rbdBndl.getDisplays()) {
                int funKey = 1;
                for (ResourcePair rp : rendDisp.getDescriptor()
                        .getResourceList()) {
                    if (rp.getResourceData() instanceof GroupResourceData) {

                        GroupResourceData grd = (GroupResourceData) rp
                                .getResourceData();

                        grd.setFuncKeyNum(funKey);
                        funKey++;

                        // If nothing selected, turn on all groups.
                        rp.getProperties().setVisible(true);

                    }
                }
            }

            rbdLoader.addRBD(rbdBndl, editor);

            VizApp.runSync(rbdLoader);

            if (close) {
                shell.dispose();
            } else {
                rbdMngr.setRbdModified(false);
            }

        } catch (VizException e) {
            final String msg = e.getMessage();
            VizApp.runSync(new Runnable() {
                @Override
                public void run() {
                    Status status = new Status(Status.ERROR, UiPlugin.PLUGIN_ID,
                            0, msg, null);
                    ErrorDialog.openError(Display.getCurrent().getActiveShell(),
                            "ERROR", "Error.", status);
                }
            });
        }
    }

    // After Loading an RBD the user may 're-load' a modified Pane. Currently
    // the number of panes has to be the same as previously displayed.
    public void loadPane() {
        String rbdName = "GEMPAK";

        // Since rbdLoader uses the same resources in the rbdBundle to load in
        // the editor, we will need to make a copy here so that future edits are
        // not immediately reflected in the loaded display. The easiest way to
        // do this is to marshal and then unmarshall the rbd.
        try {
            rbdMngr.setGeoSyncPanes(geoSyncPanesToggle.getSelection());
            rbdMngr.setAutoUpdate(true);

            // TODO : check timeline compatibility with other panes...

            AbstractRBD<?> rbdBndl = rbdMngr.createRbdBundle(rbdName,
                    timelineControl.getTimeMatcher());
            rbdBndl = AbstractRBD.clone(rbdBndl);

            ResourceBndlLoader rbdLoader = null;
            rbdLoader = new ResourceBndlLoader("RBD Previewer");

            rbdLoader.setLoadSelectedPaneOnly();

            // Get the Display Editor to use. If the active editor doesn't
            // match the name of the RBD then prompt the user.
            AbstractEditor editor = NcDisplayMngr.getActiveNatlCntrsEditor();

            // Check that the selected pane is within the layout of the Display
            if (editor == null) {
                throw new VizException("Error retrieving the Active Display??");
            }

            String activeEditorName = NcEditorUtil.getDisplayName(editor)
                    .getName();

            if (!activeEditorName.equals(rbdName)) {
                MessageDialog confirmDlg = new MessageDialog(shell,
                        "Confirm Load Pane", null,
                        "You will first need to Load the RBD before\n"
                                + "re-loading a pane. \n\n"
                                + "Do you want to load the currently defined RBD?",
                        MessageDialog.QUESTION, new String[] { "Yes", "No" },
                        0);
                confirmDlg.open();

                if (confirmDlg.getReturnCode() == MessageDialog.OK) {
                    loadRBD(false);
                }
                return;
            }

            // TODO : We could make this smarter by adjusting the display...
            if (!NcEditorUtil.getPaneLayout(editor)
                    .equals(rbdBndl.getPaneLayout())) {
                MessageDialog msgDlg = new MessageDialog(shell, "Load Pane",
                        null,
                        "The pane layout of the display doesn't match the currently selected\n"
                                + "RBD pane layout. You will first need to Load the RBD before\n"
                                + "changing the number of panes.",
                        MessageDialog.INFORMATION, new String[] { "OK" }, 0);
                msgDlg.open();

                return;
            }

            rbdLoader.addRBD(rbdBndl, editor);

            VizApp.runSync(rbdLoader);
        } catch (VizException e) {
            final String msg = e.getMessage();
            VizApp.runSync(new Runnable() {
                @Override
                public void run() {
                    Status status = new Status(Status.ERROR, UiPlugin.PLUGIN_ID,
                            0, msg, null);
                    ErrorDialog.openError(Display.getCurrent().getActiveShell(),
                            "ERROR", "Error.", status);
                }
            });
        }
    }

    // import just the given pane in the rbdBndl into the dialog's currently
    // selected pane. Note: paneID is not the currently selected pane id when
    // we are importing just a single pane.
    public void importPane(AbstractRBD<?> rbdBndl, INcPaneID paneId)
            throws VizException {

        if (rbdBndl.getDisplayType() != rbdMngr.getRbdType()) {
            throw new VizException("Can't import a non-matching display type.");
        }

        clearSeldResources();

        rbdMngr.setPaneData(rbdBndl.getDisplayPane(paneId));

        for (ResourceSelection rscSel : rbdMngr
                .getRscsForPane(rbdMngr.getSelectedPaneId())) {
            if (rscSel
                    .getResourceData() instanceof AbstractNatlCntrsRequestableResourceData) {
                timelineControl.addAvailDomResource(
                        (AbstractNatlCntrsRequestableResourceData) rscSel
                                .getResourceData());
            }
        }

        timelineControl.setTimeMatcher(rbdBndl.getTimeMatcher());

        updateSelectedResourcesView(true);
    }

    private void updateGUIforMultipane(boolean isMultiPane) {
        FormData fd = new FormData();

        geoSyncPanesToggle.setVisible(isMultiPane);

        shell.setSize(new Point(940, shell.getSize().y));
       
    }

    // if Editing then save the current rbd to an AbstractRBD<?>
    private void createEditedRbd() {

        try {
            NCTimeMatcher timeMatcher = timelineControl.getTimeMatcher();

            rbdMngr.setRbdName(savedRbdName);
            rbdMngr.setGeoSyncPanes(geoSyncPanesToggle.getSelection());
            rbdMngr.setAutoUpdate(true);

            editedRbd = rbdMngr.createRbdBundle(rbdMngr.getRbdName(),
                    timeMatcher);

            editedRbd.setIsEdited(rbdMngr.isRbdModified());

        } catch (VizException e) {
            editedRbd = null;
            final String msg = e.getMessage();
            VizApp.runSync(new Runnable() {
                @Override
                public void run() {
                    Status status = new Status(Status.ERROR, UiPlugin.PLUGIN_ID,
                            0, msg, null);
                    ErrorDialog.openError(Display.getCurrent().getActiveShell(),
                            "ERROR", "Error.", status);
                }
            });
        }
    }

    public AbstractRBD<?> getEditedRbd() {
        return editedRbd;
    }

    @Override
    public void partBroughtToTop(IWorkbenchPartReference partRef) {
        // nothing
    }

    @Override
    public void partActivated(IWorkbenchPartReference partRef) {
        // Auto-generated method stub
    }

    @Override
    public void partClosed(IWorkbenchPartReference partRef) {
        // Auto-generated method stub
    }

    @Override
    public void partDeactivated(IWorkbenchPartReference partRef) {
        // Auto-generated method stub
    }

    @Override
    public void partOpened(IWorkbenchPartReference partRef) {
        // Auto-generated method stub
    }

    @Override
    public void partHidden(IWorkbenchPartReference partRef) {
        // Auto-generated method stub
    }

    @Override
    public void partVisible(IWorkbenchPartReference partRef) {
        // Auto-generated method stub
    }

    @Override
    public void partInputChanged(IWorkbenchPartReference partRef) {
        // Auto-generated method stub
    }
}
