package gov.noaa.nws.ncep.viz.resourceManager.ui.createRbd;

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
import gov.noaa.nws.ncep.viz.common.ui.color.ColorButtonSelector;
import gov.noaa.nws.ncep.viz.common.ui.color.GempakColor;
import gov.noaa.nws.ncep.viz.resourceManager.timeline.GraphTimelineControl;
import gov.noaa.nws.ncep.viz.resourceManager.timeline.TimelineControl;
import gov.noaa.nws.ncep.viz.resourceManager.timeline.TimelineControl.IDominantResourceChangedListener;
import gov.noaa.nws.ncep.viz.resourceManager.ui.createRbd.ResourceSelectionControl.IResourceSelectedListener;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResourceData;
import gov.noaa.nws.ncep.viz.resources.attributes.EditResourceAttrsAction;
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
import gov.noaa.nws.ncep.viz.ui.display.NcPaneLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
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
 * 01/26/10		  #226		 Greg Hull	 Broke out and refactored from ResourceMngrDialog
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
 * 02/15/2012     627        Archana      Updated the call to addRbd() to accept 
 *                                      a NCMapEditor object as one of the arguments
 * 04/26/2012     #766       Quan Zhou   Modified rscSelDlg listener for double click w. existing rsc--close the dlg.
 * 04/03/2012     #765       S. Gurung   Modified method importRBD to change the display when a RBD is imported
 * 05/17/2012     #791       Quan Zhou   Added getDefaultRbdRsc() to get name and rsc from original defaultRbd.xml
 * 										 Modified LoadRBD to check if default editor is empty, then replace it.
 * 										 findCloseEmptyEdotor() is ready but not used now.
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
 * 05/07/2014   TTR991       D. Sushon   if a different NCP editor is selected, the CreateRDB tab should now adjust.
 * 05/29/2014     #1131      qzhou       Added NcDisplayType
 *                                       Modified creating new timelineControl in const and updateGUI
 * 08/14/2014		?		 B. Yin		 Added power legend (resource group) support.
 * 09/092014		?		 B. Yin		 Fixed NumPad enter issue and the "ResetToDefault" issue for groups. 
 * 07/28/2014     R4079      sgurung     Fixed the issue related to CreateRbd dialog size (bigger than usual).
 *                                       Also, added code to set geosync to true for graphs.
 * </pre>
 * 
 * @author ghull
 * @version 1
 */
public class LoadGempakControl extends Composite implements IPartListener2 {

    private RscBundleDisplayMngr rbdMngr;

    private Shell shell;

    private SashForm sash_form = null;

    private Group rbd_grp = null;

    private Group add_grp = null;

    private ResourceSelectionControl sel_rsc_cntrl = null;
    
    private Set<IResourceSelectedListener> rscSelListeners = new HashSet<IResourceSelectedListener>();
    
    private String rbd_name_txt = null;

    private ListViewer seld_rscs_lviewer = null;

    private TableViewer groupListViewer;

    private MenuManager areaMenuMngr = null;

    private Label import_lbl = null;
        
    private Button load_rbd_btn = null;

    private Button load_and_close_btn = null;

    private Button save_rbd_btn = null;

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

        /*
         * Create RBD Group
         */
        rbd_grp = new Group(sash_form, SWT.SHADOW_NONE);
        rbd_grp.setText("Create Bundle");
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;

        rbd_grp.setLayoutData(gd);

        rbd_grp.setLayout(new FormLayout());

        createRBDGroup();
        

        /*
         * Timeline Control Group
         */
        timeline_grp = new Group(sash_form, SWT.SHADOW_NONE);
        timeline_grp.setText("Select Timeline");
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        timeline_grp.setLayoutData(gd);

        timeline_grp.setLayout(new GridLayout());

        if (mngr.getRbdType().equals(NcDisplayType.GRAPH_DISPLAY)) {
            timelineControl = (GraphTimelineControl) new GraphTimelineControl(
                    timeline_grp);
        } else {
            timelineControl = new TimelineControl(timeline_grp);
        }

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
    	clear_rbd_btn.setText("Clear All");
        FormData fd = new FormData();
        fd.width = 130;
        fd.top = new FormAttachment(0, 7);
        fd.left = new FormAttachment(17, -65);
        clear_rbd_btn.setLayoutData(fd);

        save_rbd_btn = new Button(loadSaveComp, SWT.PUSH);
        save_rbd_btn.setText("Save");
        fd = new FormData();
        fd.width = 100;
        fd.top = new FormAttachment(0, 7);
        fd.left = new FormAttachment(40, -50);
        save_rbd_btn.setLayoutData(fd);

        load_rbd_btn = new Button(loadSaveComp, SWT.PUSH);
    	load_rbd_btn.setText("Load");
        fd = new FormData();
        fd.width = 100;
        fd.top = new FormAttachment(0, 7);
        // fd.bottom = new FormAttachment( 100, -7 );
        fd.left = new FormAttachment(63, -50);
        load_rbd_btn.setLayoutData(fd);

        load_and_close_btn = new Button(loadSaveComp, SWT.PUSH);
    	load_and_close_btn.setText("Load and Close");
        fd = new FormData();
        fd.width = 120;
        fd.top = new FormAttachment(0, 7);
        // fd.bottom = new FormAttachment( 100, -7 );
        fd.left = new FormAttachment(83, -50);
        load_and_close_btn.setLayoutData(fd);

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

        sash_form.setWeights(new int[] { 50, 30, 20 });

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
        add_grp.setText("Load Grid");
        
        
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

    // create all the widgets in the Resource Bundle Definition (bottom) section
    // of the sashForm.
    //
    private void createRBDGroup() {
        import_lbl = new Label(rbd_grp, SWT.None);
    	rbd_name_txt = ""; 

        // This is multi-select to make Deleting resources easier.
        seld_rscs_lviewer = new ListViewer(rbd_grp, SWT.MULTI
                | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        FormData form_data = new FormData();
    	form_data.top = new FormAttachment( 0, 10 );
    	form_data.right = new FormAttachment( 100, -10 );
    	form_data.left = new FormAttachment( 0, 10 );
    	form_data.bottom = new FormAttachment( 100, -10 );
        seld_rscs_lviewer.getList().setLayoutData(form_data);

        rbd_grp.pack(true);
    }
    
   
    private void setContentProviders() {

        seld_rscs_lviewer.setContentProvider(new IStructuredContentProvider() {
            public void dispose() {
            }

            public void inputChanged(Viewer viewer, Object oldInput,
                    Object newInput) {
            }

            public Object[] getElements(Object inputElement) {
                return ((ResourceSelection[]) inputElement);
            }
        });

        // get the full path of the attr file and then remove .prm extension
        // and the prefix path up to the cat directory.
        seld_rscs_lviewer.setLabelProvider(new LabelProvider() {
            public String getText(Object element) {
                ResourceSelection rscSel = (ResourceSelection) element;
                return rscSel.getRscLabel();
            }
        });

        updateSelectedResourcesView(true);

        // enable/disable the Edit/Delete/Clear buttons...
        seld_rscs_lviewer
                .addSelectionChangedListener(new ISelectionChangedListener() {
                    public void selectionChanged(SelectionChangedEvent event) {
                        updateSelectedResourcesView(false);
                    }
                });

        seld_rscs_lviewer.getList().addListener(SWT.MouseDoubleClick,
                new Listener() {
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
                	StructuredSelection sel_elems = (StructuredSelection) seld_rscs_lviewer
                            .getSelection();
                    ResourceSelection sel = (ResourceSelection) sel_elems
                            .getFirstElement();
                    if (!rbdMngr.addSelectedResource(rbt, sel)) {
                        if (sel != null) {
                            seld_rscs_lviewer.setInput(rbdMngr
                                    .getResourcesInGroup(groupListViewer
                                            .getTable().getSelection().length == 0 ? null
                                            : groupListViewer
                                                    .getTable()
                                                    .getSelection()[0]
                                                    .getText()));

                            seld_rscs_lviewer.refresh(true);
                        }
                    }
                    // add the new resource to the timeline as a possible
                    // dominant resource
                    if (rbt.getResourceData() instanceof AbstractNatlCntrsRequestableResourceData) {
                        timelineControl
                                .addAvailDomResource((AbstractNatlCntrsRequestableResourceData) rbt
                                        .getResourceData());

                        // if there is not a dominant resource selected then
                        // select this one
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

        load_and_close_btn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent ev) {
                loadRBD(true);
            }
        });

        save_rbd_btn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent ev) {
                saveRBD(false);
            }
        });
    }

    // import the current editor or initialize the widgets.
    //
    public void initWidgets() {
    	rbd_name_txt = "";
        updateAreaGUI();// should be the default area
        shell.setSize(initDlgSize);
        timelineControl.clearTimeline();
    }

    // if this is called from the Edit Rbd Dialog (from the LoadRbd tab), then
    // remove widgets that don't apply. (ie, import, Save, and Load)
    //
    public void configureForEditRbd() {
        import_lbl.setVisible(false);
        clear_rbd_btn.setVisible(false);
        save_rbd_btn.setVisible(false);
        load_rbd_btn.setVisible(false);
        load_and_close_btn.setVisible(false);
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

    // called when the user switches to this tab in the ResourceManagerDialog or
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
        if (rbdMngr.getRbdType().equals(NcDisplayType.GRAPH_DISPLAY)) {

            timelineControl = (GraphTimelineControl) new GraphTimelineControl(
                    timeline_grp);
        } else {
            timelineControl = new TimelineControl(timeline_grp);
        }

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

                if (rscSel.getResourceData() instanceof GroupResourceData) {
                    for (ResourcePair pair : ((GroupResourceData) rscSel
                            .getResourceData()).getResourceList()) {
                        if (pair.getResourceData() instanceof AbstractNatlCntrsRequestableResourceData) {
                            timelineControl
                                    .addAvailDomResource((AbstractNatlCntrsRequestableResourceData) pair
                                            .getResourceData());
                        }
                    }
                } else if (rscSel.getResourceData() instanceof AbstractNatlCntrsRequestableResourceData) {
                    timelineControl
                            .addAvailDomResource((AbstractNatlCntrsRequestableResourceData) rscSel
                                    .getResourceData());
                }
            }
            // }
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
        StructuredSelection sel_elems = (StructuredSelection) seld_rscs_lviewer
                .getSelection();
        ResourceSelection rscSel = (ResourceSelection) sel_elems
                .getFirstElement();

        if (rscSel == null) {
            System.out.println("sanity check: no resource is selected");
            return;
        }
        INatlCntrsResourceData rscData = rscSel.getResourceData();

        if (rscData == null) {
            System.out
                    .println("sanity check: seld resource is not a INatlCntrsResource");
            return;
        }
        EditResourceAttrsAction editAction = new EditResourceAttrsAction();
        if (editAction.PopupEditAttrsDialog(shell,
                (INatlCntrsResourceData) rscData, false)) {
            rbdMngr.setRbdModified(true);
        }

        seld_rscs_lviewer.refresh(true); // display modified (ie edited*) name
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

        //groupListViewer.setInput(rbdMngr.getGroupResources());
        seld_rscs_lviewer.setInput(rbdMngr.getUngroupedResources());
        seld_rscs_lviewer.refresh();
        //setGroupButtons();

    }

    // reset the ListViewer's input and update all of the buttons
    // TODO; get the currently selected resources and reselect them.
    private void updateSelectedResourcesView(boolean updateList) {

        if (updateList) {

            StructuredSelection orig_sel_elems = (StructuredSelection) seld_rscs_lviewer
                    .getSelection();
            System.out.println("BEFORE: seld_rscs_lviewer: " + seld_rscs_lviewer.getList().getItemCount());
            List<ResourceSelection> origSeldRscsList = (List<ResourceSelection>) orig_sel_elems
                    .toList();

            System.out.println("MID: seld_rscs_lviewer: " + seld_rscs_lviewer.getList().getItemCount());

            // this is where list is reset to default BaseOverlay (GeoPolitical) rather than default RBD (State/County boundaries)
            seld_rscs_lviewer.setInput(rbdMngr.getUngroupedResources());

            seld_rscs_lviewer.refresh(true);

            List<ResourceSelection> newSeldRscsList = new ArrayList<ResourceSelection>();

            // create a new list of selected elements
            for (ResourceSelection rscSel : origSeldRscsList) {
                for (int r = 0; r < seld_rscs_lviewer.getList().getItemCount(); r++) {
                    if (rscSel == seld_rscs_lviewer.getElementAt(r)) {
                        newSeldRscsList.add(rscSel);
                        break;
                    }
                }
            }

            seld_rscs_lviewer.setSelection(new StructuredSelection(
                    newSeldRscsList.toArray()), true);
            System.out.println("AFTER: seld_rscs_lviewer: " + seld_rscs_lviewer.getList().getItemCount());

        }

        int numSeldRscs = seld_rscs_lviewer.getList().getSelectionCount();
        int numRscs = seld_rscs_lviewer.getList().getItemCount();

        StructuredSelection sel_elems = (StructuredSelection) seld_rscs_lviewer
                .getSelection();
        List<ResourceSelection> seldRscsList = (List<ResourceSelection>) sel_elems
                .toList();

        // Can't delete, replace or turn off the base overlay.
        //
        Boolean isBaseLevelRscSeld = false;
        Boolean allRscsAreVisible = true;

        for (ResourceSelection rscSel : seldRscsList) {
            isBaseLevelRscSeld |= rscSel.isBaseLevelResource();

            allRscsAreVisible &= rscSel.isVisible();
        }
    }

    // This will load the currently configured RBD (all panes) into
    // a new editor or the active editor if the name matches the current
    // name of the RBD
    public void loadRBD(boolean close) {

    	String rbdName = rbd_name_txt.trim();

        if (rbdName == null || rbdName.isEmpty()) {
            rbdName = "Preview";
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
            //ResourceSelection rsel = getGroupResourceSelection();
            StructuredSelection sel_elems = (StructuredSelection) seld_rscs_lviewer
                    .getSelection();
           
            ResourceSelection rsel = (ResourceSelection) sel_elems
                    .getFirstElement();
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

                        if (rsel != null
                                && !((GroupResourceData) rsel.getResourcePair()
                                        .getResourceData()).getGroupName()
                                        .equals(grd.getGroupName())) {
                            rp.getProperties().setVisible(false);
                        }

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

    public void saveRBD(boolean new_pane) {
        try {
            NCTimeMatcher timeMatcher = timelineControl.getTimeMatcher();
            boolean saveRefTime = !timeMatcher.isCurrentRefTime();
            boolean saveTimeAsConstant = false; // TODO -- how should this be
                                                // set???

            // get the filename to save to.
            SaveRbdDialog saveDlg = new SaveRbdDialog(shell, savedSpfGroup,
                    savedSpfName, rbd_name_txt, saveRefTime,
                    saveTimeAsConstant);

            if ((Boolean) saveDlg.open() == false) {
                return;
            }

            savedSpfGroup = saveDlg.getSeldSpfGroup();
            savedSpfName = saveDlg.getSeldSpfName();

            saveRefTime = saveDlg.getSaveRefTime();
            saveTimeAsConstant = saveDlg.getSaveTimeAsConstant();

            // Set the name to the name that was actually used to save the RBD.
            // TODO : we could store a list of the RBDNames and load these
            // as items in the combo.
    		rbd_name_txt = saveDlg.getSeldRbdName();

            AbstractRBD<?> rbdBndl = rbdMngr.createRbdBundle(
                    saveDlg.getSeldRbdName(), timeMatcher);

            // if( !checkAndSavePredefinedAreas() ) {
            // return;
            // }

            SpfsManager.getInstance().saveRbdToSpf(savedSpfGroup, savedSpfName,
                    rbdBndl, saveRefTime, saveTimeAsConstant);

            VizApp.runSync(new Runnable() {
                public void run() {
                    String msg = null;
    				msg = new String("bundle \""+
    						rbd_name_txt + "\" saved to \""+
    						savedSpfName+"\"");
                    MessageBox mb = new MessageBox(shell, SWT.OK);
                    mb.setText("Bundle Saved");
                    mb.setMessage(msg);
                    mb.open();

                    rbdMngr.setRbdModified(false);
                }
            });
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

    // check to see if any of the selected areas are defined by another display
    // and if so prompt the user to save them to a file before saving the RBD.
    // (if we don't do this the area can still be saved but there is a problem
    // of what the areaSource will be in this case. It could be
    // INITIAL_DISPLAY_AREA but the factory for this area source is currently
    // designed to only look for loaded displays and not displays that are saved
    // in an RBD which is what the case will be if this RBD is imported into the
    // ResourceManager again. There are other possible ways to fix this but the
    // most straightforward for now is to just require the user to save the area
    // to a file
    // if
    // private Boolean checkAndSavePredefinedAreas( ) {
    //
    // Map<String,AreaName> seldAreas = rbdMngr.getAllSelectedAreaNames();
    // String confirmMsg = "";
    // List<String> pids = new ArrayList<String>(seldAreas.keySet());
    //
    // for( String pid : pids ) {
    // if( seldAreas.get( pid ).getSource() != AreaSource.INITIAL_DISPLAY_AREA )
    // {
    // seldAreas.remove( pid );
    // }
    // }
    //
    // if( seldAreas.isEmpty() ) {
    // return true;
    // }
    //
    // if( !rbdMngr.isMultiPane() ) {
    // if( seldAreas.)
    // MessageDialog confirmDlg = new MessageDialog(
    // shell, "Confirm", null,
    // "This RBD has been modified.\n\n"+
    // "Do you want to clear the current RBD selections?",
    // MessageDialog.QUESTION, new String[]{"Yes", "No"}, 0);
    // confirmDlg.open();
    //
    // if( confirmDlg.getReturnCode() != MessageDialog.OK ) {
    // return;
    // }
    //
    // }
    // // if geoSynced just check the first
    // if( rbdMngr.isGeoSyncPanes() ) {
    //
    // }
    // }

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