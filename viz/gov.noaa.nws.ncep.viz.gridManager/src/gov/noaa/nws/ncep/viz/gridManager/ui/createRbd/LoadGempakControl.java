package gov.noaa.nws.ncep.viz.gridManager.ui.createRbd;

import gov.noaa.nws.ncep.viz.common.area.AreaMenus.AreaMenuItem;
import gov.noaa.nws.ncep.viz.common.area.AreaName;
import gov.noaa.nws.ncep.viz.common.area.AreaName.AreaSource;
import gov.noaa.nws.ncep.viz.common.area.IGridGeometryProvider.ZoomLevelStrings;
import gov.noaa.nws.ncep.viz.common.area.PredefinedArea;
import gov.noaa.nws.ncep.viz.common.customprojection.GempakProjectionValuesUtil;
import gov.noaa.nws.ncep.viz.common.display.INcPaneID;
import gov.noaa.nws.ncep.viz.common.display.INcPaneLayout;
import gov.noaa.nws.ncep.viz.common.display.NcDisplayName;
import gov.noaa.nws.ncep.viz.common.display.NcDisplayType;
import gov.noaa.nws.ncep.viz.gridManager.timeline.TimelineControl;
import gov.noaa.nws.ncep.viz.gridManager.timeline.TimelineControl.IDominantResourceChangedListener;
import gov.noaa.nws.ncep.viz.gridManager.ui.createRbd.ResourceSelectionControl.IResourceSelectedListener;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData;
import gov.noaa.nws.ncep.viz.resources.groupresource.GroupResourceData;
import gov.noaa.nws.ncep.viz.resources.manager.AbstractRBD;
import gov.noaa.nws.ncep.viz.resources.manager.NcMapRBD;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceBndlLoader;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceFactory;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceFactory.ResourceSelection;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceName;
import gov.noaa.nws.ncep.viz.resources.manager.RscBundleDisplayMngr;
import gov.noaa.nws.ncep.viz.resources.manager.SpfsManager;
import gov.noaa.nws.ncep.viz.resources.time_match.NCTimeMatcher;
import gov.noaa.nws.ncep.viz.ui.display.NatlCntrsEditor;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;
import gov.noaa.nws.ncep.viz.ui.display.NcEditorUtil;
import gov.noaa.nws.ncep.viz.ui.display.NcPaneID;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;

import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.drawables.AbstractRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.viz.ui.UiPlugin;
import com.raytheon.viz.ui.editor.AbstractEditor;

/**
 * Data Selection dialog.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date       	Ticket#		Engineer	Description
 * ------------	----------	-----------	--------------------------
 * 07/04/15		    		mjames@ucar	Copied and modified from CreateRbdControl
 * </pre>
 * 
 * @author ghull
 * @version 1
 */
public class LoadGempakControl extends Composite implements IPartListener2 {

    private RscBundleDisplayMngr rbdMngr;

    private Shell shell;

    private SashForm sash_form = null;

    private Group add_grp = null;

    private ResourceSelectionControl sel_rsc_cntrl = null;
    
    private Set<IResourceSelectedListener> rscSelListeners = new HashSet<IResourceSelectedListener>();
    
    private String rbd_name_txt = null;

    //private ListViewer seld_rscs_lviewer = null;
    
    private Object seld_rscs_list = null;

    private MenuManager areaMenuMngr = null;

    private Label import_lbl = null;
        
    private Button load_rbd_btn = null;

    private Button clear_rbd_btn = null;

    private Button cancel_edit_btn = null; // when part of the 'Edit Rbd' dialog
                                           // these will

    private Button ok_edit_btn = null; // replace the Clear, Save, and Load
                                       // buttons

    private AbstractRBD<?> editedRbd = null; // set on OK when this is an 'Edit
                                             // Rbd' dialog

    // used to initialize the Save Dialog
    private String savedSpfGroup = null;

    private String savedSpfName = null;

    private Point initDlgSize = new Point(850, 860);

    private TimelineControl timelineControl = null;

    private final String ImportFromSPF = "Import";

    private Group timeline_grp;

    private static Map<String, String> gempakProjMap = GempakProjectionValuesUtil
            .initializeProjectionNameMap();

    // the rbdMngr will be used to set the gui so it should either be
    // initialized/cleared or set with the initial RBD.
    public LoadGempakControl(Composite parent, RscBundleDisplayMngr mngr)
            throws VizException {
        super(parent, SWT.NONE);
        shell = parent.getShell();

        rbdMngr = mngr;

        Composite top_comp = this;
        top_comp.setLayout(new GridLayout(1, true));

        // top_comp.setSize( 400, 400 );

        sash_form = new SashForm(top_comp, SWT.VERTICAL);
        GridData gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;

        sash_form.setLayoutData(gd);
        sash_form.setSashWidth(10);
        
        /*
         * Add Grid Group
         */
        createAddGroup();
        seld_rscs_list = rbdMngr.getUngroupedResources();

        /*
         * Timeline Control Group
         */
        timeline_grp = new Group(sash_form, SWT.SHADOW_NONE);
        timeline_grp.setText("");
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        timeline_grp.setLayoutData(gd);

        timeline_grp.setLayout(new GridLayout());
        timelineControl = new TimelineControl(timeline_grp);
        timelineControl
                .addDominantResourceChangedListener(new IDominantResourceChangedListener() {
                    @Override
                    public void dominantResourceChanged(
                            AbstractNatlCntrsRequestableResourceData newDomRsc) {
                        if (rbdMngr.getRbdType().equals(
                                NcDisplayType.GRAPH_DISPLAY)) {
                            rbdMngr.syncPanesToArea();
                        }
                    }
                });

        timelineControl.setTimeMatcher(new NCTimeMatcher());

        Composite loadSaveComp = new Composite(top_comp, SWT.SHADOW_NONE);
        gd = new GridData();
        gd.minimumHeight = 40;
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;

        loadSaveComp.setLayoutData(gd);

        loadSaveComp.setLayout(new FormLayout());

        clear_rbd_btn = new Button(loadSaveComp, SWT.PUSH);
    	clear_rbd_btn.setText("Clear");
        FormData fd = new FormData();
        fd.width = 130;
        fd.top = new FormAttachment(0, 7);
        fd.left = new FormAttachment(17, -65);
        clear_rbd_btn.setLayoutData(fd);

        load_rbd_btn = new Button(loadSaveComp, SWT.PUSH);
    	load_rbd_btn.setText("Load");
        fd = new FormData();
        fd.width = 100;
        fd.top = new FormAttachment(0, 7);
        // fd.bottom = new FormAttachment( 100, -7 );
        fd.left = new FormAttachment(40, -50);
        load_rbd_btn.setLayoutData(fd);

        cancel_edit_btn = new Button(loadSaveComp, SWT.PUSH);
        cancel_edit_btn.setText(" Cancel ");
        fd = new FormData();
        fd.width = 80;
        fd.top = new FormAttachment(0, 7);
        // fd.bottom = new FormAttachment( 100, -7 );
        fd.right = new FormAttachment(45, 0);
        cancel_edit_btn.setLayoutData(fd);
        
        ok_edit_btn = new Button(loadSaveComp, SWT.PUSH);
        ok_edit_btn.setText("   Ok   ");
        fd = new FormData();
        fd.width = 80;
        fd.top = new FormAttachment(0, 7);
        // fd.bottom = new FormAttachment( 100, -7 );
        fd.left = new FormAttachment(55, 0);
        ok_edit_btn.setLayoutData(fd);

        cancel_edit_btn.setVisible(false); // only visible if
                                           // configureForEditRbd is called
        ok_edit_btn.setVisible(false);

        sash_form.setWeights(new int[] { 50, 50 });

        // set up the content providers for the ListViewers
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
                            .removePartListener(LoadGempakControl.this);

                    LoadGempakControl.this.dispose(); // now mark for disposal
                } catch (NullPointerException npe) {
                    // do nothing if already disposed, another thread already
                    // swept it up..
                    // System.out.println(this.getClass().getCanonicalName() +
                    // ":" + npe.getMessage());
                }
            }
        });

    }

    
    // create all the widgets in the Resource Bundle Definition (bottom) section
    // of the sashForm.
    //
    private void createAddGroup() {
    	ResourceName initRscName = null;
    	
        add_grp = new Group(sash_form, SWT.SHADOW_NONE);
        add_grp.setText("");
        
        
        GridLayout mainLayout = new GridLayout(1, true);
        mainLayout.marginHeight = 1;
        mainLayout.marginWidth = 1;

        shell.setLayout(mainLayout);

        try {
            sel_rsc_cntrl = new ResourceSelectionControl(add_grp,
                    false, false, initRscName,
                    false);
            for (IResourceSelectedListener lstnr : rscSelListeners) {
                sel_rsc_cntrl.addResourceSelectionListener(lstnr);
            }
        } catch (VizException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        
        add_grp.pack(true);
        
    }

    public void addResourceSelectionListener(IResourceSelectedListener lstnr) {
        rscSelListeners.add(lstnr);
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
                    rbdMngr.addSelectedResource(rbt, null);
                    if (rbt.getResourceData() instanceof AbstractNatlCntrsRequestableResourceData) {
                        timelineControl
                                .addAvailDomResource((AbstractNatlCntrsRequestableResourceData) rbt
                                        .getResourceData());
                        if (timelineControl.getDominantResource() == null) {
                            timelineControl
                                    .setDominantResource((AbstractNatlCntrsRequestableResourceData) rbt
                                            .getResourceData());
                        }
                    }
                } catch (VizException e) {
                    System.out.println("Error Adding Resource to List: "
                            + e.getMessage());
                    MessageDialog errDlg = new MessageDialog(shell, "Error",
                            null, "Error Creating Resource:"
                                    + rscName.toString() + "\n\n"
                                    + e.getMessage(), MessageDialog.ERROR,
                            new String[] { "OK" }, 0);
                    errDlg.open();
                }
                updateSelectedResourcesView(true);
            }
        });
        clear_rbd_btn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent ev) {
                clearRBD();
            }
        });
        load_rbd_btn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent ev) {
                loadRBD(false);
            }
        });
    }

    // import the current editor or initialize the widgets.
    //
    public void initWidgets() {
    	rbd_name_txt = "";
        updateAreaGUI();// should be the default area
        shell.setSize(initDlgSize);
        //timelineControl.clearTimeline();
    }

    // if this is called from the Edit Rbd Dialog (from the LoadRbd tab), then
    // remove widgets that don't apply. (ie, import, Save, and Load)
    //
    public void configureForEditRbd() {
        import_lbl.setVisible(false);
        clear_rbd_btn.setVisible(false);
        load_rbd_btn.setVisible(false);
        cancel_edit_btn.setVisible(true);
        ok_edit_btn.setVisible(true);
        cancel_edit_btn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent ev) {
                editedRbd = null;
                shell.dispose();
            }
        });

        // dispose and leave the edited RBD in
        ok_edit_btn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent ev) {
                createEditedRbd();
                shell.dispose();
            }
        });

        timelineControl.getParent().setVisible(false);
        sash_form.setWeights(new int[] { 10, 1 });
        shell.setSize(shell.getSize().x - 100, 350);
        shell.pack(true);
    }

    // set the area and update the proj/center field
    // the
    public void updateAreaGUI() {

        PredefinedArea area = rbdMngr.getSelectedArea();

        if (area.getSource().isImagedBased()) {

            if (area.getZoomLevel().equals(
                ZoomLevelStrings.FitToScreen.toString())) {
            } else if (area.getZoomLevel().equals(
                ZoomLevelStrings.SizeOfImage.toString())) {
            } else {
                area.setZoomLevel("1.0");
            }
        } else {
            String projStr = rbdMngr.getSelectedArea().getGridGeometry()
                    .getCoordinateReferenceSystem().getName().toString();

            // use the GEMPAK name if possible.
            for (String gemProj : gempakProjMap.keySet()) {

                if (gempakProjMap.get(gemProj).equals(projStr)) {
                    break;
                }
            }
        }
    }

    private class SelectAreaAction extends Action {
        private AreaMenuItem ami;

        public SelectAreaAction(AreaMenuItem a) {
            super(a.getMenuName());
            ami = a;
        }

        @Override
        public void run() {
            try {
                rbdMngr.setSelectedAreaName(new AreaName(AreaSource
                        .getAreaSource(ami.getSource()), ami.getAreaName()));
                updateAreaGUI();
            } catch (VizException e) {
                MessageDialog errDlg = new MessageDialog(
                        NcDisplayMngr.getCaveShell(), "Error", null,
                        e.getMessage(), MessageDialog.ERROR,
                        new String[] { "OK" }, 0);
                errDlg.open();
            }
        }
    }

    public void createAvailAreaMenuItems(IMenuManager aMenuMngr) {
        // a map from the sub-menu name to a list of menu item
        List<List<AreaMenuItem>> availMenuItems = rbdMngr
                .getAvailAreaMenuItems();

        for (List<AreaMenuItem> amiList : availMenuItems) {
            if (amiList == null || amiList.isEmpty()) {
                continue;
            }

            // all the submenu name in the list should be the same.
            String subMenuName = amiList.get(0).getSubMenuName();
            IMenuManager menuMngrToAddTo = aMenuMngr;

            if (subMenuName != null && !subMenuName.isEmpty()) {
                IMenuManager subMenu = new MenuManager(subMenuName,
                        areaMenuMngr.getId() + "." + subMenuName);
                aMenuMngr.add(subMenu);
                menuMngrToAddTo = subMenu;
            }

            for (AreaMenuItem ami : amiList) {
                menuMngrToAddTo.add(new SelectAreaAction(ami));
            }
        }
    }

    // called when the user switches to this tab in the GridManagerDialog or
    // when the EditRbd Dialog initializes
    //
    public void updateDialog() {
        updateGUI();
        rbdMngr.setRbdModified(false);
    }

    // set widgets based on rbdMngr
    public void updateGUI() {

        rbd_name_txt = rbdMngr.getRbdName();
        updateAreaGUI();

        // create new GraphTimelineControl if loading a Graph Display
        timelineControl.dispose();
        timelineControl = new TimelineControl(timeline_grp);
        timelineControl
                .addDominantResourceChangedListener(new IDominantResourceChangedListener() {
                    @Override
                    public void dominantResourceChanged(
                            AbstractNatlCntrsRequestableResourceData newDomRsc) {
                        if (rbdMngr.getRbdType().equals(
                                NcDisplayType.GRAPH_DISPLAY)) {
                            rbdMngr.syncPanesToArea();
                        }
                    }
                });

        timeline_grp.pack();
        shell.pack();
        shell.setSize(initDlgSize);

        INcPaneLayout paneLayout = rbdMngr.getPaneLayout();

        // set the list of available resources for the timeline
        for (int paneIndx = 0; paneIndx < paneLayout.getNumberOfPanes(); paneIndx++) {

            for (ResourceSelection rscSel : rbdMngr
                    .getRscsForPane((NcPaneID) paneLayout
                            .createPaneId(paneIndx))) {
                if (rscSel.getResourceData() instanceof AbstractNatlCntrsRequestableResourceData) {
                    timelineControl
                            .addAvailDomResource((AbstractNatlCntrsRequestableResourceData) rscSel
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


    public void clearSeldResources() {
        // remove the requestable resources from the timeline
        //
        for (ResourceSelection rbt : rbdMngr.getSelectedRscs()) {
            if (rbt.getResourceData() instanceof AbstractNatlCntrsRequestableResourceData) {
                timelineControl
                        .removeAvailDomResource((AbstractNatlCntrsRequestableResourceData) rbt
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
    //
    public void clearRBD() {
        // TODO : ? reset the predefined area to the default???
        if (rbdMngr.isMultiPane()) {
            MessageDialog confirmDlg = new MessageDialog(shell, "Confirm",
                    null, "The will remove all resources selections\n"
                            + "from all panes in this Bundle. \n\n"
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

        updateGUI();
        seld_rscs_list = rbdMngr.getUngroupedResources();

    }

    // reset the ListViewer's input and update all of the buttons
    // TODO; get the currently selected resources and reselect them.
    private void updateSelectedResourcesView(boolean updateList) {
        if (updateList) {
            seld_rscs_list = rbdMngr.getUngroupedResources();
            Object newList = seld_rscs_list;
        }
    }

    // This will load the currently configured RBD (all panes) into
    // a new editor or the active editor if the name matches the current
    // name of the RBD
    public void loadRBD(boolean close) {

    	String rbdName = rbd_name_txt.trim();

        if (rbdName == null || rbdName.isEmpty()) {
            rbdName = "Ncgrid";
        }

        // Since rbdLoader uses the same resources in the rbdBundle to load in
        // the editor, we will need to make a copy here so that future edits are
        // not immediately reflected in the loaded display. The easiest way to
        // do this is to marshal and then unmarshal the rbd.
        try {
            AbstractRBD<?> rbdBndl = rbdMngr.createRbdBundle(rbdName,
                    timelineControl.getTimeMatcher());

            rbdBndl = AbstractRBD.clone(rbdBndl);
            rbdBndl.resolveDominantResource();

            ResourceBndlLoader rbdLoader = null;
    	    rbdLoader = new ResourceBndlLoader("Bundle Previewer");

            // TODO : Allow the user to define preferences such as
            // whether to prompt when re-loading into an existing editor,
            // whether to look for an empty editor first before re-loading an
            // exising one...
            //
            // Determine which Display Editor to use.
            //
            // 1-check if the name of the active editor is the same as the RBD
            // and if so, re-load into this editor.
            // 2-if the RBD was imported then look specifically for this display
            // ID.
            // 3-if no rbd name was given it will be called "Preview" and look
            // for an existing "Preview" editor.
            // 4-
            // First look for a preview editor and if not found then check if
            // the active editor name matches that of the current rbd name
            // (Note: don't look for an editor matching the rbd name because it
            // is possible to have the same named display for a different RBD.)
            // If the active editor doesn't match then load into a new display
            // editor
            AbstractEditor editor = null;

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
                else if (NcEditorUtil.getPaneLayout(editor).compare(
                        rbdBndl.getPaneLayout()) != 0) {
                    System.out
                            .println("Creating new Editor because the paneLayout differs.");
                    editor = null;
                }
            }

            // 2- look for an available editor that has not been modified.
            // This is usually the initial 'default' editor but could be one
            // that has been
            // cleared/wiped, or one from the New Display menu.
            if (editor == null) {

                editor = NcDisplayMngr.findUnmodifiedDisplayToLoad(rbdBndl
                        .getDisplayType());
                if (editor != null
                        && NcEditorUtil.getPaneLayout(editor).compare(
                                rbdBndl.getPaneLayout()) != 0) {
                    System.out
                            .println("Creating new Editor because the paneLayout differs.");
                    editor = null;
                }
                // String name = NcEditorUtil.getDisplayName(editor).getName();
                // NcEditorUtil.setDisplayName(
                // editor, name.substring(0, name.indexOf("-")+1) + rbdName);
            }

            // 3- if the rbd was imported from a display and the name was not
            // changed get this display and attempt to use it.

            if (editor == null) {
                NcDisplayName importDisplayName = NcDisplayName
                        .parseNcDisplayNameString("tmp");

                if (importDisplayName.getName().equals(rbdName)) {
                    // get by ID since the rbd name doesn't have to be unique
                    editor = NcDisplayMngr.findDisplayByID(
                            rbdBndl.getDisplayType(), importDisplayName);

                    if (editor != null
                            && NcEditorUtil.getPaneLayout(editor).compare(
                                    rbdBndl.getPaneLayout()) != 0) {
                        editor = null;
                    }
                }
            }

            // 4-reuse if it is a Preview editor.
            //
            if (editor == null && rbdName.equals("Preview")) {
                editor = NcDisplayMngr.findDisplayByName(
                        rbdBndl.getDisplayType(), rbdName);

                if (editor != null
                        && NcEditorUtil.getPaneLayout(editor).compare(
                                rbdBndl.getPaneLayout()) != 0) {
                    editor = null;
                }
            }

            // 5-Create a new one.
            // Note that it is possible for a display name matching the RBD name
            // to not be reused
            // if the display is not active. This is because the names may not
            // be unique and so
            // we can't determine which unless it was specifically imported into
            // the Manager and
            // even in this case they may not want to re-load it.
            //
            if (editor == null) {
                editor = NcDisplayMngr.createNatlCntrsEditor(
                        rbdBndl.getDisplayType(), rbdName,
                        rbdBndl.getPaneLayout());
            }

            if (editor == null) {
                throw new VizException(
                        "Unable to create a display to load the Bundle.");
            }

            NcDisplayMngr.bringToTop(editor);

            // Assign hot keys to group resources.
            // Set visible for the selected group.

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

            // They aren't going to like this if there is an error loading....

            // update the "Import RBD" list once a new RBD is loaded.
            // after the rbdLoader starts, if the close flag is false, update
            // the import_rbd_combo.
            if (close) {
                shell.dispose();
            } else {
                //import_rbd_combo.add(editor.getPartName());
                rbdMngr.setRbdModified(false);
                importRBD(editor.getPartName());
            }

        } catch (VizException e) {
            final String msg = e.getMessage();
            VizApp.runSync(new Runnable() {
                public void run() {
                    Status status = new Status(Status.ERROR,
                            UiPlugin.PLUGIN_ID, 0, msg, null);
                    ErrorDialog.openError(
                            Display.getCurrent().getActiveShell(), "ERROR",
                            "Error.", status);
                }
            });
        }
    }

    // After Loading an RBD the user may 're-load' a modified Pane. Currently
    // the number of panes has to be the same as previously displayed.
    public void loadPane() {
    	String rbdName = rbd_name_txt;

        if (rbdName == null || rbdName.isEmpty()) {
            rbdName = "Preview";
        }

        // Since rbdLoader uses the same resources in the rbdBundle to load in
        // the editor, we will need to make a copy here so that future edits are
        // not immediately reflected in the loaded display. The easiest way to
        // do this is to marshal and then unmarshall the rbd.
        try {
            // TODO : check timeline compatibility with other panes...
            AbstractRBD<?> rbdBndl = rbdMngr.createRbdBundle(rbdName,
                    timelineControl.getTimeMatcher());
            rbdBndl = AbstractRBD.clone(rbdBndl);

            ResourceBndlLoader rbdLoader = null;
    	    rbdLoader = new ResourceBndlLoader("Bundle Previewer");

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
                MessageDialog confirmDlg = new MessageDialog(
                        shell,
                        "Confirm Load Pane",
                        null,
                        "You will first need to Load the Bundle before\n"
                                + "re-loading a pane. \n\n"
                                + "Do you want to load the currently defined Bundle?",
                        MessageDialog.QUESTION, new String[] { "Yes", "No" }, 0);
                confirmDlg.open();

                if (confirmDlg.getReturnCode() == MessageDialog.OK) {
                    loadRBD(false);
                }
                return;
            }

            // TODO : We could make this smarter by adjusting the display...
            //
            if (!NcEditorUtil.getPaneLayout(editor).equals(
                    rbdBndl.getPaneLayout())) {
                MessageDialog msgDlg = new MessageDialog(
                        shell,
                        "Load Pane",
                        null,
                        "The pane layout of the display doesn't match the currently selected\n"
                                + "Bundle pane layout. You will first need to Load the Bundle before\n"
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
                public void run() {
                    Status status = new Status(Status.ERROR,
                            UiPlugin.PLUGIN_ID, 0, msg, null);
                    ErrorDialog.openError(
                            Display.getCurrent().getActiveShell(), "ERROR",
                            "Error.", status);
                }
            });
        }
    }

    public void importRBD(String seldDisplayName) {

        AbstractRBD<?> impRbd;
        if (seldDisplayName.equals(ImportFromSPF)) {

            SelectRbdsDialog impDlg = new SelectRbdsDialog(shell, "Import Bundle",
                    false, false, false);

            if (!impDlg.open()) {
                return;
            }

            impRbd = impDlg.getSelectedRBD();

            impRbd.resolveLatestCycleTimes();
        } else {
            // get NcMapRBD from selected display
            AbstractEditor seldEditor = NcDisplayMngr
                    .findDisplayByID(NcDisplayName
                            .parseNcDisplayNameString(seldDisplayName));

            if (seldEditor == null) {
                System.out.println("Unable to load Display :"
                        + seldDisplayName.toString());
                return;
            }

            try {
                impRbd = AbstractRBD.createRbdFromEditor(seldEditor);

                // impRbd.initFromEditor(seldEditor);

                impRbd = AbstractRBD.clone(impRbd);
            } catch (VizException e) {
                MessageDialog errDlg = new MessageDialog(shell, "Error", null,
                        "Error Importing Bundle from Display, "
                                + seldDisplayName.toString() + ".\n"
                                + e.getMessage(), MessageDialog.ERROR,
                        new String[] { "OK" }, 0);
                errDlg.open();
                return;
            }

        }

        // boolean confirm = ( rbdMngr.getSelectedRscs().length > 1 ) ||
        // rbdMngr.isMultiPane();

        // if any selections have been made then popup a confirmation msg
        if (rbdMngr.isRbdModified()) { // confirm ) {
            MessageDialog confirmDlg = new MessageDialog(
                    shell,
                    "Confirm",
                    null,
                    "You are about to replace the entire contents of this dialog. There is no 'undo'.\n\n"
                            + "Do you want to continue the import and clear the current Bundle selections?",
                    MessageDialog.QUESTION, new String[] { "Yes", "No" }, 0);
            confirmDlg.open();

            if (confirmDlg.getReturnCode() != MessageDialog.OK) {
                return;
            }
        }

        NcDisplayType curDispType = rbdMngr.getRbdType();

        try {
            rbdMngr.initFromRbdBundle(impRbd);

        } catch (VizException e) {
            rbdMngr.init(curDispType);

            MessageDialog errDlg = new MessageDialog(shell, "Error", null,
                    "Error Importing Bundle:" + impRbd.getRbdName() + "\n\n"
                            + e.getMessage(), MessageDialog.ERROR,
                    new String[] { "OK" }, 0);
            errDlg.open();
        }

        updateGUI();

        // updateGUI triggers the spinner which ends up calling
        // rbdMngr.setPaneLayout(),
        // so we need to reset this here.
        rbdMngr.setRbdModified(false);

        // timelineControl.setTimeMatcher( impRbd.getTimeMatcher() );
    }

    // import just the given pane in the rbdBndl into the dialog's
    // currently selected pane.
    // Note: paneID is not the currently selected pane id when
    // we are importing just a single pane.
    public void importPane(AbstractRBD<?> rbdBndl, INcPaneID paneId)
            throws VizException {

        if (rbdBndl.getDisplayType() != rbdMngr.getRbdType()) {
            throw new VizException("Can't import a non-matching display type.");// sanity                                                                      // check
        }

        clearSeldResources();
        rbdMngr.setPaneData(rbdBndl.getDisplayPane(paneId));
        for (ResourceSelection rscSel : rbdMngr.getRscsForPane(rbdMngr
                .getSelectedPaneId())) {
            if (rscSel.getResourceData() instanceof AbstractNatlCntrsRequestableResourceData) {
                timelineControl
                        .addAvailDomResource((AbstractNatlCntrsRequestableResourceData) rscSel
                                .getResourceData());
            }
        }

        timelineControl.setTimeMatcher(rbdBndl.getTimeMatcher());
        updateAreaGUI();
        updateSelectedResourcesView(true);
    }

    // if Editing then save the current rbd to an AbstractRBD<?>
    private void createEditedRbd() {

        try {
            NCTimeMatcher timeMatcher = timelineControl.getTimeMatcher();

            if (!rbd_name_txt.isEmpty()) {
                rbdMngr.setRbdName(rbd_name_txt);
            }

            editedRbd = rbdMngr.createRbdBundle(rbdMngr.getRbdName(),
                    timeMatcher);

            editedRbd.setIsEdited(rbdMngr.isRbdModified());

        } catch (VizException e) {
            editedRbd = null;
            final String msg = e.getMessage();
            VizApp.runSync(new Runnable() {
                public void run() {
                    Status status = new Status(Status.ERROR,
                            UiPlugin.PLUGIN_ID, 0, msg, null);
                    ErrorDialog.openError(
                            Display.getCurrent().getActiveShell(), "ERROR",
                            "Error.", status);
                }
            });
        }
    }

    public AbstractRBD<?> getEditedRbd() {
        return editedRbd;
    }

    @Override
    public void partBroughtToTop(IWorkbenchPartReference partRef) {
        // In this method, if the CreateRbdControl object is not disposed and
        // the part that is brought to top is an instance of NatlCntrsEditor,
        // loop through item strings in import_rbd_combo and find the item which
        // is identical to the title string of the NatlCntrsEditor. Set that
        // item as selected for import_rbd_combo and call importRBD() for that
        // item to update RBD contents.

        AbstractEditor seldEditor = NcDisplayMngr.findDisplayByID(NcDisplayName
                .parseNcDisplayNameString(partRef.getPartName()));
        // AbstractEditor editor = NcDisplayMngr.getActiveNatlCntrsEditor();
        if ((false == this.isDisposed())
                && seldEditor instanceof NatlCntrsEditor) {
        }
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