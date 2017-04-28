package gov.noaa.nws.ncep.viz.resourceManager.ui.createRbd;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.drawables.AbstractRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.AbstractResourceData;
import com.raytheon.uf.viz.core.rsc.ResourceList;
import com.raytheon.uf.viz.core.rsc.ResourceProperties;
import com.raytheon.uf.viz.core.rsc.capabilities.Capabilities;
import com.raytheon.viz.ui.UiPlugin;
import com.raytheon.viz.ui.editor.AbstractEditor;

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
import gov.noaa.nws.ncep.viz.resourceManager.timeline.cache.TimeSettingsCacheManager;
import gov.noaa.nws.ncep.viz.resourceManager.ui.createRbd.ResourceSelectionControl.IResourceSelectedListener;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResourceData;
import gov.noaa.nws.ncep.viz.resources.attributes.EditResourceAttrsDialogFactory;
import gov.noaa.nws.ncep.viz.resources.groupresource.GroupResourceData;
import gov.noaa.nws.ncep.viz.resources.manager.AbstractRBD;
import gov.noaa.nws.ncep.viz.resources.manager.AreaMenuTree;
import gov.noaa.nws.ncep.viz.resources.manager.AttributeSet;
import gov.noaa.nws.ncep.viz.resources.manager.NcMapRBD;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceBndlLoader;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceDefinition;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceDefnsMngr;
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
 * 04/27/2017     R29983    Steve Russell Added method handleSelectedResourceGroup
 *                                        Updated listeners
 *                                        groupListViewer.getTable().addSelectionListener() 
 *                                        delGrpBtn.addSelectionListener() in
 *                                        CreateRbdControl.createGroupGrp()
 * </pre>
 * 
 * @author ghull
 * @version 1
 */

public class CreateRbdControl extends Composite implements IPartListener2 {

    private ResourceSelectionDialog rscSelDlg = null;

    private RscBundleDisplayMngr rbdMngr;

    private Shell shell;

    private SashForm sashForm = null;

    private Group rbdGroup = null;

    private Text rbdNameText = null;

    private Label rbdNameLabel = null;

    private Combo dispTypeCombo = null;

    private Label dispTypeLabel = null;

    private Button selectResourceButton = null;

    private Button multiPaneToggle = null;

    private Button autoUpdateButton = null;

    private Button geoSyncPanesToggle = null;

    private Group selectedResourceGroup = null;

    private ListViewer selectedResourceViewer = null;

    private TableViewer groupListViewer;

    private Text nameTxt;

    private Button delGrpBtn;

    private Composite grpColorComp;

    private ColorButtonSelector grpColorBtn;

    private Button grpMoveUpBtn;

    private Button grpMoveDownBtn;

    private int currentlySelectedGroup = -1;

    private Button replaceResourceButton = null;

    private Button editResourceButton = null;

    private Button deleteResourceButton = null;

    private Button disableResourceButton = null;

    private Button moveResourceUpButton = null;

    private Button moveResourceDownButton = null;

    private ToolItem areaToolItem;

    private AreaMenuItem seldAreaMenuItem = null;

    // dispose if new Font created

    private Font areaFont = null;

    private Group geoAreaGroup = null;

    private MenuManager areaMenuMngr = null;

    // only one of these visible at a time

    private Composite geoAreaInfoComp = null;

    // depending on if a satellite area is selected

    private Composite resourceAreaOptsComp = null;

    // view-only projection and map center info

    private Text projInfoText = null;

    // view-only projection and map center info

    private Text mapCenterText = null;

    private Button fitToScreenButton = null;

    private Button sizeOfImageButton = null;

    private Button customAreaButton = null;

    private Group paneLayoutGroup = null;

    private Group groupGrp = null;

    private Button paneSelectionButtons[][] = null;

    private Button importPaneButton = null;

    private Button loadPaneButton = null;

    private Button clearPaneButton = null;

    private Label importLabel = null;

    private Combo importRbdCombo = null;

    private Button loadRbdButton = null;

    private Button loadAndCloseButton = null;

    private Button saveRbdButton = null;

    private Button clearRbdButton = null;

    // when part of the 'Edit Rbd' dialog these will replace the Clear, Save,
    // and Load buttons

    private Button cancelEditButton = null;

    private Button okEditButton = null;

    // set on OK when this is an 'Edit Rbd' dialog

    private AbstractRBD<?> editedRbd = null;

    // used to initialize the Save Dialog

    private String savedSpfGroup = null;

    private String savedSpfName = null;

    private String savedRbdName;

    private Point initDlgSize = new Point(750, 860);

    private int multiPaneDlgWidth = 950;

    private TimelineControl timelineControl = null;

    private final String ImportFromSPF = "From SPF...";

    private Group timelineGroup;

    private int grpColor = 1;

    // NCP group rendering order
    static private int NCP_GROUP_RENDERING_ORDER = 500;

    private static Map<String, String> gempakProjMap = GempakProjectionValuesUtil
            .initializeProjectionNameMap();

    private static String unGroupString = "Static";

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(CreateRbdControl.class);

    // the rbdMngr will be used to set the gui so it should either be
    // initialized/cleared or set with the initial RBD.
    public CreateRbdControl(Composite parent, RscBundleDisplayMngr mngr)
            throws VizException {
        super(parent, SWT.NONE);
        shell = parent.getShell();

        rbdMngr = mngr;

        rscSelDlg = new ResourceSelectionDialog(shell);

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

        rbdGroup = new Group(sashForm, SWT.SHADOW_NONE);
        rbdGroup.setText("Resource Bundle Display");
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;

        rbdGroup.setLayoutData(gd);

        rbdGroup.setLayout(new FormLayout());

        createRBDGroup();

        timelineGroup = new Group(sashForm, SWT.SHADOW_NONE);
        timelineGroup.setText("Select Timeline");
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
                        if (newDomRsc == null) {
                            autoUpdateButton
                                    .setSelection(rbdMngr.isAutoUpdate());
                            autoUpdateButton.setEnabled(false);
                        } else if (newDomRsc.isAutoUpdateable()) {
                            autoUpdateButton.setEnabled(true);
                            autoUpdateButton.setSelection(true);
                            if (rbdMngr.getRbdType()
                                    .equals(NcDisplayType.GRAPH_DISPLAY)) {
                                geoSyncPanesToggle.setSelection(true);
                                rbdMngr.syncPanesToArea();
                            }
                        } else {
                            autoUpdateButton.setSelection(false);
                            autoUpdateButton.setEnabled(false);
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
        clearRbdButton.setText(" Reset To Default ");
        FormData fd = new FormData();
        fd.width = 130;
        fd.top = new FormAttachment(0, 7);
        fd.left = new FormAttachment(17, -65);
        clearRbdButton.setLayoutData(fd);

        saveRbdButton = new Button(loadSaveComp, SWT.PUSH);
        saveRbdButton.setText(" Save RBD ");
        fd = new FormData();
        fd.width = 100;
        fd.top = new FormAttachment(0, 7);
        fd.left = new FormAttachment(40, -50);
        saveRbdButton.setLayoutData(fd);

        loadRbdButton = new Button(loadSaveComp, SWT.PUSH);
        loadRbdButton.setText("Load RBD");
        fd = new FormData();
        fd.width = 100;
        fd.top = new FormAttachment(0, 7);
        fd.left = new FormAttachment(63, -50);
        loadRbdButton.setLayoutData(fd);

        loadAndCloseButton = new Button(loadSaveComp, SWT.PUSH);
        loadAndCloseButton.setText("Load And Close");
        fd = new FormData();
        fd.width = 120;
        fd.top = new FormAttachment(0, 7);
        fd.left = new FormAttachment(83, -50);
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

        sashForm.setWeights(new int[] { 50, 35 });

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
                            .removePartListener(CreateRbdControl.this);
                    // now mark for disposal
                    CreateRbdControl.this.dispose();
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

        importRbdCombo = new Combo(rbdGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        FormData form_data = new FormData();
        form_data.left = new FormAttachment(0, 15);
        form_data.top = new FormAttachment(0, 30);
        form_data.right = new FormAttachment(24, 0);
        importRbdCombo.setLayoutData(form_data);
        importRbdCombo.setEnabled(true);

        importLabel = new Label(rbdGroup, SWT.None);
        importLabel.setText("Import");
        form_data = new FormData();
        form_data.left = new FormAttachment(importRbdCombo, 0, SWT.LEFT);
        form_data.bottom = new FormAttachment(importRbdCombo, -3, SWT.TOP);
        importLabel.setLayoutData(form_data);

        rbdNameText = new Text(rbdGroup, SWT.SINGLE | SWT.BORDER);
        form_data = new FormData(200, 20);
        form_data.left = new FormAttachment(importRbdCombo, 25, SWT.RIGHT);
        form_data.top = new FormAttachment(importRbdCombo, 0, SWT.TOP);
        rbdNameText.setLayoutData(form_data);

        rbdNameLabel = new Label(rbdGroup, SWT.None);
        rbdNameLabel.setText("RBD Name");
        form_data = new FormData();
        form_data.width = 180;
        form_data.left = new FormAttachment(rbdNameText, 0, SWT.LEFT);
        form_data.bottom = new FormAttachment(rbdNameText, -3, SWT.TOP);
        rbdNameLabel.setLayoutData(form_data);

        dispTypeCombo = new Combo(rbdGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        form_data = new FormData(120, 20);
        form_data.left = new FormAttachment(importRbdCombo, 0, SWT.LEFT);
        form_data.top = new FormAttachment(importRbdCombo, 45, SWT.BOTTOM);
        dispTypeCombo.setLayoutData(form_data);
        dispTypeCombo.setEnabled(true);

        dispTypeCombo
                .setItems(new String[] { NcDisplayType.NMAP_DISPLAY.getName(),
                        NcDisplayType.NTRANS_DISPLAY.getName(),
                        NcDisplayType.SOLAR_DISPLAY.getName(),
                        NcDisplayType.GRAPH_DISPLAY.getName() });

        dispTypeLabel = new Label(rbdGroup, SWT.None);
        dispTypeLabel.setText("RBD Type");
        form_data = new FormData();
        form_data.left = new FormAttachment(dispTypeCombo, 0, SWT.LEFT);
        form_data.bottom = new FormAttachment(dispTypeCombo, -3, SWT.TOP);
        dispTypeLabel.setLayoutData(form_data);

        multiPaneToggle = new Button(rbdGroup, SWT.CHECK);
        multiPaneToggle.setText("Multi-Pane");
        form_data = new FormData();
        form_data.top = new FormAttachment(rbdNameText, -10, SWT.TOP);
        form_data.left = new FormAttachment(rbdNameText, 15, SWT.RIGHT);
        multiPaneToggle.setLayoutData(form_data);

        autoUpdateButton = new Button(rbdGroup, SWT.CHECK);
        form_data = new FormData();
        autoUpdateButton.setText("Auto Update");
        form_data.top = new FormAttachment(multiPaneToggle, 10, SWT.BOTTOM);
        form_data.left = new FormAttachment(multiPaneToggle, 0, SWT.LEFT);
        autoUpdateButton.setLayoutData(form_data);
        autoUpdateButton.setEnabled(false);

        geoSyncPanesToggle = new Button(rbdGroup, SWT.CHECK);
        form_data = new FormData();
        geoSyncPanesToggle.setText("Geo-Sync Panes");
        form_data.top = new FormAttachment(autoUpdateButton, 10, SWT.BOTTOM);
        form_data.left = new FormAttachment(autoUpdateButton, 0, SWT.LEFT);
        geoSyncPanesToggle.setLayoutData(form_data);

        createAreaGroup();

        // create all the widgets used to show and edit the Selected Resources
        selectedResourceGroup = createSeldRscsGroup();

        createPaneLayoutGroup();

        createGroupGrp();
    }

    private void createAreaGroup() {
        geoAreaGroup = new Group(rbdGroup, SWT.SHADOW_NONE);
        geoAreaGroup.setText("Area");
        geoAreaGroup.setLayout(new FormLayout());
        FormData form_data = new FormData();
        form_data.top = new FormAttachment(dispTypeCombo, 25, SWT.BOTTOM);
        // room for the Load and Save buttons
        form_data.bottom = new FormAttachment(100, 0);
        form_data.left = new FormAttachment(0, 10);
        form_data.right = new FormAttachment(24, 0);

        geoAreaGroup.setLayoutData(form_data);

        ToolBar areaTBar = new ToolBar(geoAreaGroup,
                SWT.SHADOW_OUT | SWT.HORIZONTAL | SWT.RIGHT | SWT.WRAP);
        form_data = new FormData();
        form_data.left = new FormAttachment(0, 10);
        form_data.top = new FormAttachment(0, 15);
        form_data.right = new FormAttachment(100, -10);
        form_data.height = 30;
        areaTBar.setLayoutData(form_data);

        this.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                if (areaFont != null) {
                    areaFont.dispose();
                }
            }
        });

        areaToolItem = new ToolItem(areaTBar, SWT.DROP_DOWN);
        areaMenuMngr = new MenuManager("CreateRbdControl");
        areaMenuMngr.setRemoveAllWhenShown(true);
        final Menu areaCtxMenu = areaMenuMngr.createContextMenu(shell);

        areaCtxMenu.setVisible(false);
        geoAreaGroup.setMenu(areaCtxMenu);

        areaMenuMngr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager amngr) {
                AreaMenuTree areaMenu = rbdMngr.getAvailAreaMenuItems();
                createAvailAreaMenuItems(amngr, areaMenu);
            }
        });

        // Main toolbar button clicked: show the areas popup menu at
        // the location of the toolbar so it appears like a combo
        // dropdown. This will also trigger the menu manager to create
        // the menu items for the available areas.
        areaToolItem.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                ToolItem ti = ((ToolItem) event.widget);
                Rectangle bounds = ti.getBounds();
                Point point = ti.getParent().toDisplay(bounds.x,
                        bounds.y + bounds.height);

                areaCtxMenu.setLocation(point);
                areaCtxMenu.setVisible(true);
            }
        });

        // 2 Composites. 1 for when a predefined area is selected which will
        // show the projection and map center. And 1 for when a satellite
        // resource is select which will let the user select either FitToScreen
        // or SizeOfImage

        geoAreaInfoComp = new Composite(geoAreaGroup, SWT.NONE);
        geoAreaInfoComp.setLayout(new FormLayout());
        resourceAreaOptsComp = new Composite(geoAreaGroup, SWT.NONE);
        resourceAreaOptsComp.setLayout(new GridLayout(1, true));

        geoAreaInfoComp.setVisible(true);
        resourceAreaOptsComp.setVisible(false);

        form_data = new FormData();
        form_data.left = new FormAttachment(0, 10);
        form_data.top = new FormAttachment(areaTBar, 15, SWT.BOTTOM);
        form_data.right = new FormAttachment(100, -10);

        // both overlap each other since only one visible at a time
        geoAreaInfoComp.setLayoutData(form_data);

        form_data.top = new FormAttachment(areaTBar, 30, SWT.BOTTOM);
        resourceAreaOptsComp.setLayoutData(form_data);

        fitToScreenButton = new Button(resourceAreaOptsComp, SWT.RADIO);
        fitToScreenButton.setText("Fit To Screen");

        sizeOfImageButton = new Button(resourceAreaOptsComp, SWT.RADIO);
        sizeOfImageButton.setText("Size Of Image");

        // radio behavior

        fitToScreenButton.setSelection(true);
        sizeOfImageButton.setSelection(false);

        Label proj_lbl = new Label(geoAreaInfoComp, SWT.None);
        proj_lbl.setText("Projection");
        form_data = new FormData();
        form_data.left = new FormAttachment(0, 0);
        form_data.top = new FormAttachment(0, 0);
        form_data.right = new FormAttachment(100, 0);
        proj_lbl.setLayoutData(form_data);

        projInfoText = new Text(geoAreaInfoComp,
                SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
        form_data = new FormData();
        form_data.left = new FormAttachment(0, 0);
        form_data.top = new FormAttachment(proj_lbl, 2, SWT.BOTTOM);
        form_data.right = new FormAttachment(100, 0);
        projInfoText.setLayoutData(form_data);
        projInfoText.setText("");

        // indicate Read-only
        projInfoText.setBackground(rbdGroup.getBackground());

        Label map_center_lbl = new Label(geoAreaInfoComp, SWT.None);
        map_center_lbl.setText("Map Center");
        form_data = new FormData();
        form_data.left = new FormAttachment(0, 0);
        form_data.top = new FormAttachment(projInfoText, 15, SWT.BOTTOM);
        form_data.right = new FormAttachment(100, 0);
        map_center_lbl.setLayoutData(form_data);

        mapCenterText = new Text(geoAreaInfoComp,
                SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
        form_data = new FormData();
        form_data.left = new FormAttachment(0, 0);
        form_data.top = new FormAttachment(map_center_lbl, 2, SWT.BOTTOM);
        form_data.right = new FormAttachment(100, 0);
        mapCenterText.setLayoutData(form_data);
        mapCenterText.setText(" ");

        // indicate Read-only
        mapCenterText.setBackground(rbdGroup.getBackground());

        // TODO : move this to be a Tool from main menu to create and name
        // predefined areas and move this button to be an option under the
        // predefined areas list

        customAreaButton = new Button(geoAreaGroup, SWT.PUSH);
        form_data = new FormData();
        form_data.left = new FormAttachment(0, 40);
        form_data.right = new FormAttachment(100, -40);
        form_data.bottom = new FormAttachment(100, -15);

        customAreaButton.setLayoutData(form_data);
        customAreaButton.setText(" Custom ... ");

        // not implemented
        customAreaButton.setEnabled(false);
        customAreaButton.setVisible(false);
    }

    // create the Selected Resources List, the Edit, Delete and Clear buttons
    private Group createSeldRscsGroup() {
        Group seld_rscs_grp = new Group(rbdGroup, SWT.SHADOW_NONE);
        seld_rscs_grp.setText("Selected Resources");
        seld_rscs_grp.setLayout(new FormLayout());
        FormData form_data = new FormData();
        form_data.top = new FormAttachment(autoUpdateButton, 15, SWT.BOTTOM);
        form_data.left = new FormAttachment(geoAreaGroup, 10, SWT.RIGHT);
        form_data.right = new FormAttachment(100, -300);
        form_data.bottom = new FormAttachment(100, 0);
        seld_rscs_grp.setLayoutData(form_data);

        // This is multi-select to make Deleting resources easier.
        selectedResourceViewer = new ListViewer(seld_rscs_grp,
                SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        form_data = new FormData();
        form_data.top = new FormAttachment(0, 5);
        form_data.left = new FormAttachment(0, 5);
        form_data.right = new FormAttachment(100, -95);// -110 ); //80, 0 );
        form_data.bottom = new FormAttachment(100, -47);
        selectedResourceViewer.getList().setLayoutData(form_data);

        editResourceButton = new Button(seld_rscs_grp, SWT.PUSH);
        editResourceButton.setText(" Edit ...");
        form_data = new FormData();
        form_data.width = 90;
        form_data.bottom = new FormAttachment(100, -10);
        form_data.left = new FormAttachment(40, 20);

        editResourceButton.setLayoutData(form_data);
        editResourceButton.setEnabled(false);

        selectResourceButton = new Button(seld_rscs_grp, SWT.PUSH);
        selectResourceButton.setText(" New ... ");
        form_data = new FormData();
        form_data.width = 90;
        form_data.bottom = new FormAttachment(100, -10);
        form_data.right = new FormAttachment(40, -20);
        selectResourceButton.setLayoutData(form_data);

        replaceResourceButton = new Button(seld_rscs_grp, SWT.PUSH);
        replaceResourceButton.setText(" Replace ...");
        form_data = new FormData();
        form_data.width = 90;
        form_data.bottom = new FormAttachment(100, -10);
        form_data.left = new FormAttachment(editResourceButton, 30, SWT.RIGHT);
        replaceResourceButton.setLayoutData(form_data);
        replaceResourceButton.setEnabled(false);

        replaceResourceButton.setVisible(false);

        deleteResourceButton = new Button(seld_rscs_grp, SWT.PUSH);
        deleteResourceButton.setText("Remove");
        form_data = new FormData();
        form_data.width = 75;
        form_data.top = new FormAttachment(10, -10);
        form_data.right = new FormAttachment(100, -10);
        deleteResourceButton.setLayoutData(form_data);
        deleteResourceButton.setEnabled(false);

        disableResourceButton = new Button(seld_rscs_grp, SWT.TOGGLE);
        disableResourceButton.setText("Turn Off");
        form_data = new FormData();
        form_data.width = 75;
        form_data.right = new FormAttachment(100, -10);
        form_data.top = new FormAttachment(30, -10);
        disableResourceButton.setLayoutData(form_data);

        moveResourceDownButton = new Button(seld_rscs_grp,
                SWT.ARROW | SWT.DOWN);
        moveResourceDownButton.setToolTipText("Move Down");
        form_data = new FormData();
        form_data.width = 35;
        form_data.top = new FormAttachment(50, -10);
        form_data.right = new FormAttachment(100, -10);
        moveResourceDownButton.setLayoutData(form_data);
        moveResourceDownButton.setEnabled(false);

        moveResourceDownButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {

                StructuredSelection groups = ((StructuredSelection) groupListViewer
                        .getSelection());
                ResourceSelection grpSelected = (ResourceSelection) groups
                        .getFirstElement();

                StructuredSelection resources = ((StructuredSelection) selectedResourceViewer
                        .getSelection());
                ResourceSelection resSelected = (ResourceSelection) resources
                        .getFirstElement();

                if (resSelected != null && grpSelected != null) {

                    if (groupListViewer.getTable().getSelection()[0].getText()
                            .equalsIgnoreCase(unGroupString)) {
                        rbdMngr.moveDownResource(resSelected, null);

                        selectedResourceViewer
                                .setInput(rbdMngr.getUngroupedResources());
                        selectedResourceViewer.refresh();
                        selectedResourceViewer.setSelection(resources);
                    } else {
                        rbdMngr.moveDownResource(resSelected,
                                (GroupResourceData) grpSelected
                                        .getResourceData());
                        selectedResourceViewer.setInput(
                                rbdMngr.getResourcesInGroup(groupListViewer
                                        .getTable().getSelection().length == 0
                                                ? null
                                                : groupListViewer.getTable()
                                                        .getSelection()[0]
                                                                .getText()));
                        int ii = 0;

                        for (ResourceSelection rs : rbdMngr.getResourcesInGroup(
                                groupListViewer.getTable().getSelection()[0]
                                        .getText())) {
                            if (rs.getResourcePair() == resSelected
                                    .getResourcePair()) {
                                selectedResourceViewer.getList().select(ii);
                                break;
                            }
                            ii++;
                        }
                    }
                }
            }
        });

        moveResourceUpButton = new Button(seld_rscs_grp, SWT.ARROW | SWT.UP);
        moveResourceUpButton.setToolTipText("Move Up");
        form_data = new FormData();
        form_data.width = 35;
        form_data.top = new FormAttachment(moveResourceDownButton, 0, SWT.TOP);
        form_data.left = new FormAttachment(disableResourceButton, 0, SWT.LEFT);
        moveResourceUpButton.setLayoutData(form_data);
        moveResourceUpButton.setEnabled(false);

        moveResourceUpButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {

                StructuredSelection groups = ((StructuredSelection) groupListViewer
                        .getSelection());
                ResourceSelection grpSelected = (ResourceSelection) groups
                        .getFirstElement();

                StructuredSelection resources = ((StructuredSelection) selectedResourceViewer
                        .getSelection());
                ResourceSelection resSelected = (ResourceSelection) resources
                        .getFirstElement();

                if (resSelected != null && grpSelected != null) {

                    if (groupListViewer.getTable().getSelection()[0].getText()
                            .equalsIgnoreCase(unGroupString)) {
                        rbdMngr.moveUpResource(resSelected, null);

                        selectedResourceViewer
                                .setInput(rbdMngr.getUngroupedResources());
                        selectedResourceViewer.refresh();
                        selectedResourceViewer.setSelection(resources);
                    } else {
                        rbdMngr.moveUpResource(resSelected,
                                (GroupResourceData) grpSelected
                                        .getResourceData());
                        selectedResourceViewer.setInput(
                                rbdMngr.getResourcesInGroup(groupListViewer
                                        .getTable().getSelection().length == 0
                                                ? null
                                                : groupListViewer.getTable()
                                                        .getSelection()[0]
                                                                .getText()));
                        selectedResourceViewer.refresh();

                        int ii = 0;

                        for (ResourceSelection rs : rbdMngr.getResourcesInGroup(
                                groupListViewer.getTable().getSelection()[0]
                                        .getText())) {
                            if (rs.getResourcePair() == resSelected
                                    .getResourcePair()) {
                                selectedResourceViewer.getList().select(ii);
                                break;
                            }
                            ii++;
                        }

                    }
                }
            }
        });

        Button edit_span_btn = new Button(seld_rscs_grp, SWT.PUSH);
        edit_span_btn.setText(" Bin ... ");
        form_data = new FormData();
        form_data.width = 75;
        form_data.top = new FormAttachment(70, -10);
        form_data.right = new FormAttachment(100, -10);
        edit_span_btn.setLayoutData(form_data);
        edit_span_btn.setEnabled(false);

        return seld_rscs_grp;
    }

    private void createGroupGrp() {
        groupGrp = new Group(rbdGroup, SWT.SHADOW_NONE);
        groupGrp.setText("Resource Group");

        FormData fd = new FormData();
        fd.left = new FormAttachment(selectedResourceGroup, 10, SWT.RIGHT);
        fd.top = new FormAttachment(0, 3);
        fd.right = new FormAttachment(100, 0);
        fd.bottom = new FormAttachment(100, 0);
        groupGrp.setLayoutData(fd);

        groupGrp.setLayout(new FormLayout());

        Label nameLbl = new Label(groupGrp, SWT.NONE);
        nameLbl.setText("Name:");
        fd = new FormData();
        fd.left = new FormAttachment(0, 5);
        fd.top = new FormAttachment(0, 5);
        nameLbl.setLayoutData(fd);

        nameTxt = new Text(groupGrp, SWT.SINGLE | SWT.BORDER);
        fd = new FormData(200, 20);
        fd.left = new FormAttachment(nameLbl, 5, SWT.RIGHT);
        fd.right = new FormAttachment(100, -40);
        fd.top = new FormAttachment(nameLbl, -5, SWT.TOP);
        nameTxt.setLayoutData(fd);

        nameTxt.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent ke) {
                Text txt = (Text) ke.widget;
                if ((ke.keyCode == SWT.CR || ke.keyCode == SWT.KEYPAD_CR)
                        && !txt.getText().isEmpty()) {

                    if (txt.getText().equalsIgnoreCase(unGroupString)) {
                        currentlySelectedGroup = -1;
                        selectUngroupedGrp();
                        selectedResourceViewer
                                .setInput(rbdMngr.getUngroupedResources());
                        setGroupButtons();
                        return;
                    }

                    int ii = 0;

                    for (ResourceSelection rsel : rbdMngr.getGroupResources()) {
                        if (rsel.getResourceData() instanceof GroupResourceData) {
                            if (((GroupResourceData) rsel.getResourceData())
                                    .getGroupName()
                                    .equalsIgnoreCase(txt.getText())) {

                                groupListViewer.getTable().setSelection(ii);
                                currentlySelectedGroup = groupListViewer
                                        .getTable().getSelectionIndex();
                                selectedResourceViewer.setInput(rbdMngr
                                        .getResourcesInGroup(groupListViewer
                                                .getTable()
                                                .getSelection().length == 0
                                                        ? null
                                                        : groupListViewer
                                                                .getTable()
                                                                .getSelection()[0]
                                                                        .getText()));
                                setGroupButtons();

                                return;

                            }
                        }
                        ii++;
                    }
                    addResourceGroup(txt.getText());
                    setGroupButtons();
                    txt.setText("");
                }
            }
        });

        groupListViewer = new TableViewer(groupGrp, SWT.SINGLE | SWT.V_SCROLL
                | SWT.H_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
        fd = new FormData();
        fd.top = new FormAttachment(nameLbl, 15, SWT.BOTTOM);
        fd.bottom = new FormAttachment(100, -5);
        fd.left = new FormAttachment(0, 5);
        fd.right = new FormAttachment(100, -40);

        groupListViewer.getTable().setLayoutData(fd);

        groupListViewer.setContentProvider(new IStructuredContentProvider() {

            @Override
            public Object[] getElements(Object inputElement) {

                // Add "Ungrouped" group
                ResourcePair group = new ResourcePair();

                GroupResourceData grd = new GroupResourceData(unGroupString, 0,
                        grpColorBtn.getColorValue());
                group.setResourceData(grd);

                ResourceSelection ungrouped = null;
                try {
                    ungrouped = ResourceFactory.createResource(group);
                } catch (VizException e) {
                }

                ResourceSelection[] groups = (ResourceSelection[]) inputElement;

                ResourceSelection[] groups1;

                if (ungrouped != null) {
                    if (groups != null) {
                        List<ResourceSelection> list = new ArrayList<>(
                                Arrays.asList(groups));
                        list.add(0, ungrouped);
                        groups1 = list
                                .toArray(new ResourceSelection[list.size()]);
                    } else {
                        groups1 = new ResourceSelection[] { ungrouped };
                    }
                } else {
                    groups1 = groups;
                }

                return groups1;

            }

            @Override
            public void dispose() {
            }

            @Override
            public void inputChanged(Viewer viewer, Object oldInput,
                    Object newInput) {
            }
        });

        groupListViewer.setLabelProvider(new LabelProvider() {
            @Override
            public String getText(Object element) {
                ResourceSelection rscSel = (ResourceSelection) element;
                if (rscSel.getResourceData() instanceof GroupResourceData) {
                    return ((GroupResourceData) rscSel.getResourceData())
                            .getGroupName();
                } else {
                    return "No Group Name";
                }
            }
        });

        // enable/disable the Edit/Delete/Clear buttons...
        groupListViewer
                .addSelectionChangedListener(new ISelectionChangedListener() {
                    @Override
                    public void selectionChanged(SelectionChangedEvent event) {
                    }
                });

        groupListViewer.getTable().addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleSelectedResourceGroup();
            }
        });

        groupListViewer.setColumnProperties(new String[] { "Group Name" });

        groupListViewer.setCellModifier(new ICellModifier() {

            @Override
            public boolean canModify(Object element, String property) {
                // TODO Auto-generated method stub
                return true;
            }

            @Override
            public Object getValue(Object element, String property) {

                ResourceSelection sel = (ResourceSelection) element;
                return ((GroupResourceData) sel.getResourceData())
                        .getGroupName();
            }

            @Override
            public void modify(Object element, String property, Object value) {

                if (value != null && !((String) value).isEmpty()) {
                    ResourceSelection sel = (ResourceSelection) ((TableItem) element)
                            .getData();
                    ((GroupResourceData) sel.getResourceData())
                            .setGroupName((String) value);
                    groupListViewer.refresh();
                }
            }

        });

        groupListViewer.setCellEditors(new CellEditor[] {
                new TextCellEditor(groupListViewer.getTable()) });

        ColumnViewerEditorActivationStrategy activationSupport = new ColumnViewerEditorActivationStrategy(
                groupListViewer) {
            @Override
            protected boolean isEditorActivationEvent(
                    // Enable editor only with mouse double click
                    ColumnViewerEditorActivationEvent event) {
                if (event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION) {
                    EventObject source = event.sourceEvent;
                    if (source instanceof MouseEvent
                            && ((MouseEvent) source).button == 3) {
                        return false;
                    }
                    if (((GroupResourceData) ((ResourceSelection) ((org.eclipse.jface.viewers.ViewerCell) event
                            .getSource()).getElement()).getResourcePair()
                                    .getResourceData()).getGroupName()
                                            .equalsIgnoreCase(unGroupString)) {
                        return false;
                    }

                    return true;
                }

                return false;
            }
        };

        TableViewerEditor.create(groupListViewer, activationSupport,
                ColumnViewerEditor.TABBING_HORIZONTAL
                        | ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR
                        | ColumnViewerEditor.TABBING_VERTICAL
                        | ColumnViewerEditor.KEYBOARD_ACTIVATION);

        groupListViewer.addDoubleClickListener(new IDoubleClickListener() {

            @Override
            public void doubleClick(DoubleClickEvent event) {

                TableViewer tv = (TableViewer) event.getSource();

                tv.setSelection(event.getSelection());
                selectedResourceViewer.setInput(rbdMngr.getResourcesInGroup(
                        groupListViewer.getTable().getSelection().length == 0
                                ? null
                                : groupListViewer.getTable().getSelection()[0]
                                        .getText()));

                currentlySelectedGroup = groupListViewer.getTable()
                        .getSelectionIndex();

                if (groupListViewer.getTable().getSelection()[0].getText()
                        .equalsIgnoreCase(unGroupString)) {
                    selectUngroupedGrp();
                    selectedResourceViewer
                            .setInput(rbdMngr.getUngroupedResources());
                    currentlySelectedGroup = -1;
                }

                setGroupButtons();
            }

        });

        groupListViewer.getTable().addListener(SWT.MouseUp, new Listener() {
            @Override
            public void handleEvent(Event event) {
                org.eclipse.swt.widgets.Table grpList = (org.eclipse.swt.widgets.Table) event.widget;

                if (grpList.getItemCount() > 0) {

                    if (event.y > grpList.getItemCount()
                            * grpList.getItemHeight()) {
                        grpList.deselectAll();
                        currentlySelectedGroup = -1;

                        selectUngroupedGrp();

                        selectedResourceViewer
                                .setInput(rbdMngr.getUngroupedResources());
                        setGroupButtons();

                        nameTxt.setText("");
                    }
                }
            }
        });

        grpMoveUpBtn = new Button(groupGrp, SWT.ARROW | SWT.UP);
        grpMoveUpBtn.setToolTipText("Move Up");
        fd = new FormData();
        fd.width = 30;
        fd.top = new FormAttachment(50, -70);
        fd.left = new FormAttachment(groupListViewer.getTable(), 5, SWT.RIGHT);
        grpMoveUpBtn.setLayoutData(fd);
        grpMoveUpBtn.setEnabled(true);
        grpMoveUpBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                StructuredSelection isel = ((StructuredSelection) groupListViewer
                        .getSelection());
                ResourceSelection sel = (ResourceSelection) isel
                        .getFirstElement();
                if (sel != null) {
                    rbdMngr.moveUpGrp(sel.getResourcePair());
                    groupListViewer.setInput(rbdMngr.getGroupResources());
                    groupListViewer.refresh();
                    groupListViewer.setSelection(isel);
                    currentlySelectedGroup = groupListViewer.getTable()
                            .getSelectionIndex();
                    setGroupButtons();
                }
            }
        });

        grpMoveDownBtn = new Button(groupGrp, SWT.ARROW | SWT.DOWN);
        grpMoveDownBtn.setToolTipText("Move Down");
        fd = new FormData();
        fd.width = 30;
        fd.top = new FormAttachment(grpMoveUpBtn, 10, SWT.BOTTOM);
        fd.left = new FormAttachment(groupListViewer.getTable(), 5, SWT.RIGHT);
        grpMoveDownBtn.setLayoutData(fd);
        grpMoveDownBtn.setEnabled(true);
        grpMoveDownBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                StructuredSelection isel = ((StructuredSelection) groupListViewer
                        .getSelection());
                ResourceSelection sel = (ResourceSelection) isel
                        .getFirstElement();
                if (sel != null) {
                    rbdMngr.moveDownGrp(sel.getResourcePair());
                    groupListViewer.setInput(rbdMngr.getGroupResources());
                    groupListViewer.setSelection(isel);

                    groupListViewer.refresh();
                    currentlySelectedGroup = groupListViewer.getTable()
                            .getSelectionIndex();
                    setGroupButtons();
                }
            }
        });
        delGrpBtn = new Button(groupGrp, SWT.PUSH);
        delGrpBtn.setText("X");
        delGrpBtn.setToolTipText("Remove");
        fd = new FormData();
        fd.width = 30;
        fd.top = new FormAttachment(grpMoveDownBtn, 10, SWT.BOTTOM);
        fd.left = new FormAttachment(groupListViewer.getTable(), 5, SWT.RIGHT);
        delGrpBtn.setLayoutData(fd);
        delGrpBtn.setEnabled(true);

        delGrpBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {

                ResourceSelection sel = (ResourceSelection) ((StructuredSelection) groupListViewer
                        .getSelection()).getFirstElement();

                if (sel != null) {
                    // Remove Selection
                    removeUserDefinedResourceGroup(sel);
                    groupListViewer.setInput(rbdMngr.getGroupResources());
                    groupListViewer.refresh();
                    selectUngroupedGrp();

                    handleSelectedResourceGroup();

                }
            }
        });

        grpColorComp = new Composite(groupGrp, SWT.NONE);
        grpColorComp.setLayout(new FormLayout());
        grpColorComp.setToolTipText("Legend Color");
        grpColorBtn = new ColorButtonSelector(grpColorComp, 28, 22);
        grpColorBtn.setColorValue(GempakColor.convertToRGB(1));

        fd = new FormData();
        fd.width = 30;
        fd.top = new FormAttachment(delGrpBtn, 10, SWT.BOTTOM);
        fd.left = new FormAttachment(groupListViewer.getTable(), 6, SWT.RIGHT);
        grpColorComp.setLayoutData(fd);
        grpColorComp.pack();

        grpColorBtn.addListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                ResourceSelection sel = getGroupResourceSelection();
                if (sel != null) {

                    ((GroupResourceData) sel.getResourceData())
                            .setLegendColor(grpColorBtn.getColorValue());
                }
            }
        });

        groupListViewer.setInput(rbdMngr.getGroupResources());

        groupListViewer.refresh(true);

        selectUngroupedGrp();

        setGroupButtons();

        groupGrp.pack();
        groupGrp.setVisible(true);
    }

    private void createPaneLayoutGroup() {
        paneLayoutGroup = new Group(rbdGroup, SWT.SHADOW_NONE);
        paneLayoutGroup.setText("Pane Layout");
        paneLayoutGroup.setLayout(new FormLayout());
        FormData fd = new FormData();
        fd.left = new FormAttachment(selectedResourceGroup, 10, SWT.RIGHT);
        fd.top = new FormAttachment(0, 3);
        fd.right = new FormAttachment(100, 0);
        fd.bottom = new FormAttachment(100, 0);
        paneLayoutGroup.setLayoutData(fd);

        Composite num_rows_cols_comp = new Composite(paneLayoutGroup, SWT.NONE);
        GridLayout gl = new GridLayout(rbdMngr.getMaxPaneLayout().getColumns(),
                false);

        num_rows_cols_comp.setLayout(gl);

        fd = new FormData();
        fd.left = new FormAttachment(0, 100);
        fd.top = new FormAttachment(0, 3);
        fd.right = new FormAttachment(100, -10);
        num_rows_cols_comp.setLayoutData(fd);

        Button num_rows_btns[] = new Button[rbdMngr.getMaxPaneLayout()
                .getRows()];
        Button num_cols_btns[] = new Button[rbdMngr.getMaxPaneLayout()
                .getColumns()];

        for (int r = 0; r < rbdMngr.getMaxPaneLayout().getRows(); r++) {
            num_rows_btns[r] = new Button(num_rows_cols_comp, SWT.PUSH);
            num_rows_btns[r].setText(Integer.toString(r + 1));
            num_rows_btns[r].setSize(20, 20);
            num_rows_btns[r].setData(new Integer(r + 1));
            num_rows_btns[r].addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    selectPane(
                            rbdMngr.setPaneLayout(
                                    new NcPaneLayout(
                                            (Integer) e.widget.getData(),
                                            ((NcPaneLayout) rbdMngr
                                                    .getPaneLayout())
                                                            .getColumns())));
                    updatePaneLayout();
                }
            });
        }

        GridData gd = new GridData();
        gd.widthHint = 50;
        gd.grabExcessHorizontalSpace = true;

        for (int c = 0; c < rbdMngr.getMaxPaneLayout().getColumns(); c++) {
            num_cols_btns[c] = new Button(num_rows_cols_comp, SWT.PUSH);
            num_cols_btns[c].setText(Integer.toString(c + 1));
            num_cols_btns[c].setData(new Integer(c + 1));

            num_cols_btns[c].addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    selectPane(
                            rbdMngr.setPaneLayout(
                                    new NcPaneLayout(
                                            ((NcPaneLayout) rbdMngr
                                                    .getPaneLayout()).getRows(),
                                            (Integer) e.widget.getData())));
                    updatePaneLayout();
                }
            });
        }

        Label num_rows_lbl = new Label(paneLayoutGroup, SWT.NONE);
        num_rows_lbl.setText("Rows:");
        fd = new FormData();
        fd.right = new FormAttachment(num_rows_cols_comp, -5, SWT.LEFT);
        fd.top = new FormAttachment(num_rows_cols_comp, 10, SWT.TOP);
        num_rows_lbl.setLayoutData(fd);

        Label num_cols_lbl = new Label(paneLayoutGroup, SWT.NONE);
        num_cols_lbl.setText("Columns:");
        fd = new FormData();
        fd.right = new FormAttachment(num_rows_cols_comp, -5, SWT.LEFT);
        fd.top = new FormAttachment(num_rows_lbl, 15, SWT.BOTTOM);
        num_cols_lbl.setLayoutData(fd);

        Label sel_pane_lbl = new Label(paneLayoutGroup, SWT.NONE);
        sel_pane_lbl.setText("Select Pane");
        fd = new FormData();
        fd.left = new FormAttachment(0, 5);
        fd.top = new FormAttachment(num_rows_cols_comp, 2, SWT.BOTTOM);
        sel_pane_lbl.setLayoutData(fd);

        Label sep = new Label(paneLayoutGroup, SWT.SEPARATOR | SWT.HORIZONTAL);
        fd = new FormData();
        fd.left = new FormAttachment(sel_pane_lbl, 5, SWT.RIGHT);
        fd.right = new FormAttachment(100, 0);
        fd.top = new FormAttachment(num_rows_cols_comp, 11, SWT.BOTTOM);
        sep.setLayoutData(fd);

        Composite pane_sel_comp = new Composite(paneLayoutGroup, SWT.NONE);
        pane_sel_comp.setLayout(
                new GridLayout(rbdMngr.getMaxPaneLayout().getColumns(), true));

        fd = new FormData();
        fd.left = new FormAttachment(0, 25);
        fd.top = new FormAttachment(sep, 12);
        fd.bottom = new FormAttachment(100, -45);
        fd.right = new FormAttachment(100, -15);
        pane_sel_comp.setLayoutData(fd);

        paneSelectionButtons = new Button[rbdMngr.getMaxPaneLayout()
                .getRows()][rbdMngr.getMaxPaneLayout().getColumns()];

        int numPanes = rbdMngr.getMaxPaneLayout().getNumberOfPanes();
        for (

        int p = 0; p < numPanes; p++)

        {
            NcPaneID pid = (NcPaneID) rbdMngr.getMaxPaneLayout()
                    .createPaneId(p);
            int r = pid.getRow();
            int c = pid.getColumn();

            paneSelectionButtons[r][c] = new Button(pane_sel_comp, SWT.TOGGLE);
            paneSelectionButtons[r][c].setText(pid.toString());

            paneSelectionButtons[r][c].setData(pid);
            paneSelectionButtons[r][c]
                    .addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            NcPaneID seldPane = (NcPaneID) e.widget.getData();
                            selectPane(seldPane);
                        }
                    });
            paneSelectionButtons[r][c].setSelection((r == 0 && c == 0));
        }

        importPaneButton = new Button(paneLayoutGroup, SWT.PUSH);
        fd = new FormData();
        fd.top = new FormAttachment(pane_sel_comp, 10, SWT.BOTTOM);
        fd.left = new FormAttachment(50, -120);
        importPaneButton.setLayoutData(fd);
        importPaneButton.setText("Import...");
        importPaneButton.setEnabled(true);

        loadPaneButton = new Button(paneLayoutGroup, SWT.PUSH);
        fd = new FormData();
        fd.top = new FormAttachment(importPaneButton, 0, SWT.TOP);
        fd.left = new FormAttachment(50, -38);
        loadPaneButton.setLayoutData(fd);
        loadPaneButton.setText(" Re-Load ");

        clearPaneButton = new Button(paneLayoutGroup, SWT.PUSH);
        clearPaneButton.setText("  Clear  ");
        fd = new FormData();
        fd.top = new FormAttachment(importPaneButton, 0, SWT.TOP);
        fd.left = new FormAttachment(50, 50);
        clearPaneButton.setLayoutData(fd);

        paneLayoutGroup.setVisible(false);

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

        dispTypeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                NcDisplayType selDispType = NcDisplayType
                        .getDisplayType(dispTypeCombo.getText());
                if (rbdMngr.getRbdType() != selDispType) {
                    if (rbdMngr.isRbdModified()) {
                        MessageDialog confirmDlg = new MessageDialog(shell,
                                "Confirm", null,
                                "This RBD has been modified.\n\n"
                                        + "Do you want to clear the current RBD selections?",
                                MessageDialog.QUESTION,
                                new String[] { "Yes", "No" }, 0);
                        confirmDlg.open();

                        if (confirmDlg.getReturnCode() != MessageDialog.OK) {
                            return;
                        }
                    }

                    try {
                        rbdMngr.setRbdType(selDispType);

                        AbstractRBD<?> dfltRbd = AbstractRBD
                                .getDefaultRBD(rbdMngr.getRbdType());

                        rbdMngr.initFromRbdBundle(dfltRbd);

                    } catch (VizException ve) {
                        MessageDialog errDlg = new MessageDialog(shell, "Error",
                                null, ve.getMessage(), MessageDialog.ERROR,
                                new String[] { "OK" }, 0);
                        errDlg.open();

                        rbdMngr.init(rbdMngr.getRbdType());
                    }
                    updateGUI();
                }
            }
        });

        selectResourceButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                StructuredSelection sel_elems = (StructuredSelection) selectedResourceViewer
                        .getSelection();
                List<ResourceSelection> seldRscsList = sel_elems.toList();
                int numSeldRscs = selectedResourceViewer.getList()
                        .getSelectionCount();

                Boolean isBaseLevelRscSeld = false;
                // the initially selected resource is the first non-baselevel
                // rsc
                ResourceName initRscName = null;

                for (ResourceSelection rscSel : seldRscsList) {
                    isBaseLevelRscSeld |= rscSel.isBaseLevelResource();
                    if (initRscName == null && !rscSel.isBaseLevelResource()) {
                        initRscName = rscSel.getResourceName();
                    }
                }

                // the replace button is enabled if there is only 1 resource
                // selected and it is not the base resource
                if (rscSelDlg.isOpen()) {
                    if (initRscName != null) {
                        rscSelDlg.setSelectedResource(initRscName);
                    }
                } else {
                    // Replace button is visible replace enabled
                    rscSelDlg.open(true,
                            (numSeldRscs == 1 && !isBaseLevelRscSeld),
                            initRscName, multiPaneToggle.getSelection(),
                            rbdMngr.getRbdType(),
                            SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MODELESS);
                }
            }
        });

        // may be invisible, if implementing the Replace on the Select Resource
        // Dialog
        replaceResourceButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (!rscSelDlg.isOpen()) {
                    StructuredSelection sel_elems = (StructuredSelection) selectedResourceViewer
                            .getSelection();
                    ResourceSelection rscSel = (ResourceSelection) sel_elems
                            .getFirstElement();

                    // Replace button is visible

                    rscSelDlg.open(true, (rscSel != null ? true : false),
                            rscSel.getResourceName(),
                            multiPaneToggle.getSelection(),
                            rbdMngr.getRbdType(), SWT.DIALOG_TRIM | SWT.RESIZE
                                    | SWT.APPLICATION_MODAL);
                }
            }
        });

        rscSelDlg.addResourceSelectionListener(new IResourceSelectedListener() {
            @Override
            public void resourceSelected(ResourceName rscName, boolean replace,
                    boolean addAllPanes, boolean done) {

                try {
                    ResourceSelection rbt = ResourceFactory
                            .createResource(rscName);

                    rbdMngr.getSelectedArea();

                    // if replacing existing resources, get the selected
                    // resources (For now just replace the 1st if more than one
                    // selected.)

                    if (replace) {
                        StructuredSelection sel_elems = (StructuredSelection) selectedResourceViewer
                                .getSelection();
                        ResourceSelection rscSel = (ResourceSelection) sel_elems
                                .getFirstElement();

                        StructuredSelection grp = (StructuredSelection) groupListViewer
                                .getSelection();

                        ResourceSelection sel = (ResourceSelection) grp
                                .getFirstElement();

                        // If the group resource functionality exists (which it
                        // does in all cases currently) sel will have a value.
                        // If in the future it is removed in some places, use
                        // the standard replace method.

                        if (sel != null) {

                            // If we are in the Static group, just replace the
                            // resource(s) as normal. If we are in a group
                            // resource follow the group resource replace method

                            if (((GroupResourceData) sel.getResourcePair()
                                    .getResourceData()).getGroupName()
                                            .equalsIgnoreCase(unGroupString)) {
                                sel = null;
                                rbdMngr.replaceSelectedResource(rscSel, rbt);

                            } else {
                                ((GroupResourceData) sel.getResourceData())
                                        .replaceResourcePair(
                                                rscSel.getResourcePair(),
                                                rbt.getResourcePair());

                                selectedResourceViewer.setInput(rbdMngr
                                        .getResourcesInGroup(groupListViewer
                                                .getTable()
                                                .getSelection().length == 0
                                                        ? null
                                                        : groupListViewer
                                                                .getTable()
                                                                .getSelection()[0]
                                                                        .getText()));

                                selectedResourceViewer.refresh(true);
                            }
                        } else {
                            rbdMngr.replaceSelectedResource(rscSel, rbt);
                        }

                        // remove this from the list of available dominant
                        // resources.
                        if (rscSel
                                .getResourceData() instanceof AbstractNatlCntrsRequestableResourceData) {
                            timelineControl.removeAvailDomResource(
                                    (AbstractNatlCntrsRequestableResourceData) rscSel
                                            .getResourceData());
                        }

                        // if replacing a resource which is set to provide the
                        // geographic area check to see if the current area has
                        // been reset to the default because it was not
                        // available

                        updateAreaGUI();

                    } else {
                        if (addAllPanes) {
                            if (!rbdMngr.addSelectedResourceToAllPanes(rbt)) {
                                if (done) {
                                    rscSelDlg.close();
                                }
                                return;
                            }
                        } else {

                            StructuredSelection grp = (StructuredSelection) groupListViewer
                                    .getSelection();

                            ResourceSelection sel = (ResourceSelection) grp
                                    .getFirstElement();

                            if (sel != null && ((GroupResourceData) sel
                                    .getResourcePair().getResourceData())
                                            .getGroupName()
                                            .equalsIgnoreCase(unGroupString)) {
                                sel = null;
                            }

                            if (!rbdMngr.addSelectedResource(rbt, sel)) {
                                if (sel != null) {
                                    selectedResourceViewer.setInput(rbdMngr
                                            .getResourcesInGroup(groupListViewer
                                                    .getTable()
                                                    .getSelection().length == 0
                                                            ? null
                                                            : groupListViewer
                                                                    .getTable()
                                                                    .getSelection()[0]
                                                                            .getText()));

                                    selectedResourceViewer.refresh(true);
                                }

                                if (done) {
                                    rscSelDlg.close();
                                }
                                return;
                            }
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

                if (done) {
                    rscSelDlg.close();
                }
            }
        });

        sizeOfImageButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                rbdMngr.setZoomLevel((sizeOfImageButton.getSelection()
                        ? ZoomLevelStrings.SizeOfImage.toString()
                        : ZoomLevelStrings.FitToScreen.toString()));

            }
        });

        fitToScreenButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                rbdMngr.setZoomLevel((fitToScreenButton.getSelection()
                        ? ZoomLevelStrings.FitToScreen.toString()
                        : ZoomLevelStrings.SizeOfImage.toString()));
            }
        });

        // TODO: if single pane and there are resources selected
        // in other panes then should we prompt the user on whether
        // to clear them? We can ignore them if Loading/Saving a
        // single pane, but if they reset multi-pane then should the
        // resources in other panes still be selected.
        multiPaneToggle.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                rbdMngr.setMultiPane(multiPaneToggle.getSelection());

                updateGUIforMultipane(multiPaneToggle.getSelection());

                if (multiPaneToggle.getSelection()) {
                    updatePaneLayout();
                } else {
                    selectPane(new NcPaneID());
                }

                if (rscSelDlg != null && rscSelDlg.isOpen()) {
                    rscSelDlg.setMultiPaneEnabled(
                            multiPaneToggle.getSelection());
                }
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

        customAreaButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
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
                updateAreaGUI();

            }
        });

        disableResourceButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                StructuredSelection sel_elems = (StructuredSelection) selectedResourceViewer
                        .getSelection();
                Iterator<?> itr = sel_elems.iterator();

                while (itr.hasNext()) {
                    ResourceSelection rscSel = (ResourceSelection) itr.next();
                    rscSel.setIsVisible(!disableResourceButton.getSelection());
                }

                selectedResourceViewer.refresh(true);

                updateSelectedResourcesView(false);
            }
        });

        clearPaneButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                clearSeldResources();
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

        loadPaneButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                loadPane();
            }
        });

        saveRbdButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                saveRBD(false);
            }
        });

        importRbdCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                importRBD(importRbdCombo.getText());
            }
        });

        // TODO : Currently we can't detect if the user clicks on the import
        // combo and then clicks on the currently selected
        // item which means the users can't re-import the current selection (or
        // import another from an spf.) The following may
        // allow a hackish work around later.... (I tried virtually every
        // listener and these where the only ones to trigger.)
        // ....update...with new Eclipse this seems to be working; ie.
        // triggering a selection when
        // combo is clicked on but selection isn't changed.
        importRbdCombo.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent e) {
            }
        });
        importRbdCombo.addListener(SWT.MouseDown, new Listener() { // and
                                                                   // SWT.MouseUp
            @Override
            public void handleEvent(Event event) {
            }
        });
        importRbdCombo.addListener(SWT.Activate, new Listener() {
            @Override
            public void handleEvent(Event event) {
                updateImportCombo();
            }
        });
        importRbdCombo.addListener(SWT.Deactivate, new Listener() {
            @Override
            public void handleEvent(Event event) {
            }
        });

        importPaneButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                SelectRbdsDialog impDlg = new SelectRbdsDialog(shell,
                        "Import Pane", true, false, true);

                if (!impDlg.open()) {
                    return;
                }

                AbstractRBD<?> impRbd = impDlg.getSelectedRBD();

                if (impRbd != null) {
                    impRbd.resolveLatestCycleTimes();

                    try {
                        importPane(impRbd, impRbd.getSelectedPaneId());
                    } catch (VizException e) {
                        MessageDialog errDlg = new MessageDialog(shell, "Error",
                                null,
                                "Error Importing Rbd, " + impRbd.getRbdName()
                                        + ".\n" + e.getMessage(),
                                MessageDialog.ERROR, new String[] { "OK" }, 0);
                        errDlg.open();
                    }
                }
            }
        });

    }

    // import the current editor or initialize the widgets.

    public void initWidgets() {

        rbdNameText.setText("");

        updateAreaGUI();// should be the default area

        shell.setSize(initDlgSize);

        updateGUIforMultipane(rbdMngr.isMultiPane());

        timelineControl.clearTimeline();
    }

    // if this is called from the Edit Rbd Dialog (from the LoadRbd tab), then
    // remove widgets that don't apply. (ie, import, Save, and Load)

    public void configureForEditRbd() {
        importLabel.setVisible(false);
        importRbdCombo.setVisible(false);
        clearRbdButton.setVisible(false);
        saveRbdButton.setVisible(false);
        loadPaneButton.setVisible(false);
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

        FormData fd = (FormData) rbdNameText.getLayoutData();
        fd.left = new FormAttachment(20, 0);
        rbdNameText.setLayoutData(fd);

        timelineControl.getParent().setVisible(false);
        sashForm.setWeights(new int[] { 10, 1 });
        shell.setSize(shell.getSize().x - 100, 350);
        shell.pack(true);
    }

    public void updateImportCombo() {
        // check for possible new Displays that may be imported.
        NcDisplayName seldImport = NcDisplayName
                .parseNcDisplayNameString(importRbdCombo.getText());

        importRbdCombo.removeAll();
        for (AbstractEditor ncDisplay : NcDisplayMngr.getAllNcDisplays()) {

            NcDisplayName displayName = NcEditorUtil.getDisplayName(ncDisplay);
            importRbdCombo.add(displayName.toString());

            // if this was selected before, select it again
            if (seldImport == null || seldImport.equals(displayName)) {
                importRbdCombo.select(importRbdCombo.getItemCount() - 1);
            }
        }

        // if the previous selection wasn't found then select 'from SPF'
        importRbdCombo.add(ImportFromSPF);

        if (importRbdCombo.getSelectionIndex() == -1) {
            importRbdCombo.select(importRbdCombo.getItemCount() - 1);
        }
    }

    // Note: if the text set for the ToolItem doesn't fit on the size of the
    // item then it will become blank and unselectable. Need to make sure this
    // doesn't happen so create a multi-line text string for the tool item and
    // make sure it is wide and high enough to hold the string.
    //
    public void setAreaTextOnMenuItem(AreaName areaName) {
        seldAreaMenuItem = new AreaMenuItem(areaName);

        // based on the starting width of the dialog and current attachments,
        // the ToolItem has a width of 136. This will display up to 13
        // characters.
        //

        // current width and height
        Point toolBarSize = areaToolItem.getParent().getSize();

        if (toolBarSize.x == 0) { // gui not initialized yet
            return;
        }

        int maxChars = toolBarSize.x * 13 / 136;

        // normal font
        int fontSize = 10;

        boolean truncated = false;

        // string that will be used to set the menuItem
        String menuText = seldAreaMenuItem.getMenuName();

        // if greater than 13 then we will have to figure out how to make it fit
        if (menuText.length() > maxChars) {
            // if its close then just change the font size
            if (menuText.length() <= maxChars + 1) {
                fontSize = 8;
            } else if (menuText.length() <= maxChars + 2) {
                fontSize = 7;
            } else if (menuText.length() <= maxChars + 3) {
                fontSize = 7;
            }
            // if this is a Display-defined area then just truncate it since the
            // display id should be enough to let the user know what the area is
            else if (areaName.getSource() == AreaSource.DISPLAY_AREA) {
                fontSize = 8;
                menuText = menuText.substring(0, maxChars + 3);
                truncated = true;
            }

            else if (areaName.getSource().isImagedBased()) {
                // if a Mcidas or Gini satellite then the name is the satellite
                // name and area or sector name
                // separated with '/'
                // in this case we can leave off the satelliteName
                int sepIndx = menuText.indexOf(File.separator);
                if (sepIndx == -1) {
                    statusHandler.handle(Priority.INFO,
                            "Expecting '/' in satellite defined area???? ");
                    menuText = menuText.substring(0, maxChars);
                    truncated = true;
                } else {
                    String satName = menuText.substring(0, sepIndx);
                    String area = menuText.substring(sepIndx + 1);

                    // if the areaName is close then change the font size
                    if (area.length() > maxChars) {
                        if (area.length() <= maxChars + 1) {
                            fontSize = 8;
                        } else if (area.length() <= maxChars + 2) {
                            fontSize = 7;
                        } else if (area.length() <= maxChars + 3) {
                            fontSize = 7;
                            // else have to truncate
                        } else {
                            fontSize = 7;
                            area = area.substring(0, maxChars + 3);
                            truncated = true;
                        }
                    }
                    if (satName.length() > maxChars + 10 - fontSize) {
                        satName = satName.substring(0,
                                maxChars + 10 - fontSize);
                        truncated = true;
                    }

                    menuText = satName + "\n" + area;
                }
            } else {
                fontSize = 8;
                menuText = menuText.substring(0, maxChars + 1);
                truncated = true;
            }
        }

        // change the font and set the text (don't dispose the original font).
        Font curFont = areaToolItem.getParent().getFont();
        FontData[] fd = curFont.getFontData();

        if (fd[0].getHeight() != fontSize) {
            fd[0].setHeight(fontSize);
            Device dev = curFont.getDevice();

            if (areaFont != null) {
                areaFont.dispose();
            }
            areaFont = new Font(dev, fd);
            areaToolItem.getParent().setFont(areaFont);
        }

        int tbHght = (menuText.indexOf("\n") > 0 ? 47 : 30);

        if (tbHght != toolBarSize.y) {
            toolBarSize.y = (menuText.indexOf("\n") > 0 ? 47 : 30);
            // without this the size will revert back when the dialog is resized
            // for multi-pane
            FormData formData = (FormData) areaToolItem.getParent()
                    .getLayoutData();
            formData.height = tbHght;
            areaToolItem.getParent().getLayoutData();
            areaToolItem.getParent().getParent().layout(true);
        }
        areaToolItem.setText(menuText);

        // if truncated then show the menuname in the tooltips
        areaToolItem.setToolTipText(
                truncated ? seldAreaMenuItem.getMenuName() : "");

    }

    // set the area and update the proj/center field

    public void updateAreaGUI() {

        PredefinedArea area = rbdMngr.getSelectedArea();

        setAreaTextOnMenuItem(
                new AreaName(area.getSource(), area.getAreaName()));

        geoAreaInfoComp.setVisible(false);
        resourceAreaOptsComp.setVisible(false);

        if (area.getSource().isImagedBased()) {

            resourceAreaOptsComp.setVisible(true);

            if (area.getZoomLevel()
                    .equals(ZoomLevelStrings.FitToScreen.toString())) {
                fitToScreenButton.setSelection(true);
                sizeOfImageButton.setSelection(false);
            } else if (area.getZoomLevel()
                    .equals(ZoomLevelStrings.SizeOfImage.toString())) {
                fitToScreenButton.setSelection(false);
                sizeOfImageButton.setSelection(true);
            } else {
                area.setZoomLevel("1.0");
                fitToScreenButton.setSelection(true);
                sizeOfImageButton.setSelection(false);
            }
        } else {
            geoAreaInfoComp.setVisible(true);

            String projStr = rbdMngr.getSelectedArea().getGridGeometry()
                    .getCoordinateReferenceSystem().getName().toString();

            projInfoText.setText(projStr);
            projInfoText.setToolTipText(projStr);

            // use the GEMPAK name if possible.
            for (String gemProj : gempakProjMap.keySet()) {

                if (gempakProjMap.get(gemProj).equals(projStr)) {
                    projInfoText.setText(gemProj.toUpperCase());
                    break;
                }
            }

            if (area.getMapCenter() != null) {
                Integer lat = (int) (area.getMapCenter()[1] * 1000.0);
                Integer lon = (int) (area.getMapCenter()[0] * 1000.0);

                mapCenterText.setText(Double.toString((double) lat / 1000.0)
                        + "/" + Double.toString((double) lon / 1000.0));
            } else {
                mapCenterText.setText("N/A");
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
                rbdMngr.setSelectedAreaName(
                        new AreaName(AreaSource.getAreaSource(ami.getSource()),
                                ami.getAreaName()));
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

    /**
     * Recursive method to create MenuManager from a Tree
     * 
     * @param IMenuManager
     * 
     * @param AreaMenuTree
     */
    public void createAvailAreaMenuItems(IMenuManager areaMenuMngr,
            AreaMenuTree areaMenu) {

        for (AreaMenuTree areaSubMenu : areaMenu.getSubMenu()) {

            // Base case: if there aren't any sub-menus add the AreaMenuItem to
            // the menu
            if (!areaSubMenu.hasSubMenu()) {

                areaMenuMngr.add(
                        new SelectAreaAction(areaSubMenu.getAreaMenuItem()));

            }
            // If there's a sub-menu, create a IMenuManager recursively call
            // createAvailAreaMenuItems with the subMenuManager and areaSubMenu
            if (areaSubMenu.hasSubMenu()) {
                String subMenuName = areaSubMenu.getMenuName();
                IMenuManager subMenuManager = new MenuManager(subMenuName,
                        areaMenuMngr.getId() + "." + subMenuName);
                areaMenuMngr.add(subMenuManager);
                createAvailAreaMenuItems(subMenuManager, areaSubMenu);

            }

        }

    }

    // called when the user switches to this tab in the ResourceManagerDialog or
    // when the EditRbd Dialog initializes
    public void updateDialog() {

        updateImportCombo();

        // If the gui has not been set with the current rbdMngr then do it now.
        updateGUI();

        // updateGUI triggers the spinner which ends up calling
        // rbdMngr.setPaneLayout(), so we need to reset this here.
        rbdMngr.setRbdModified(false);
    }

    // set widgets based on rbdMngr
    public void updateGUI() {

        // update the display type combo
        for (int i = 0; i < dispTypeCombo.getItemCount(); i++) {
            NcDisplayType dType = NcDisplayType
                    .getDisplayType(dispTypeCombo.getItems()[i]);
            if (dType.equals(rbdMngr.getRbdType())) {
                dispTypeCombo.select(i);
            }
        }

        if (dispTypeCombo.getSelectionIndex() == -1) {
            dispTypeCombo.select(0);
        }

        rbdNameText.setText(rbdMngr.getRbdName());

        rbdNameText.setSelection(0, rbdMngr.getRbdName().length());
        rbdNameText.setFocus();

        importRbdCombo.deselectAll();

        for (int i = 0; i < importRbdCombo.getItemCount(); i++) {
            String importRbdName = importRbdCombo.getItems()[i];
            importRbdName = NcDisplayName
                    .parseNcDisplayNameString(importRbdName).getName();
            if (importRbdName.equals(rbdMngr.getRbdName())) {
                importRbdCombo.select(i);
                break;
            }
        }

        TimeSettingsCacheManager.getInstance()
                .updateCacheLookupKey(rbdMngr.getRbdName());
        if (importRbdCombo.getSelectionIndex() == -1) {
            importRbdCombo.select(importRbdCombo.getItemCount() - 1);
        }

        updateAreaGUI();

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
                        if (newDomRsc == null) {
                            autoUpdateButton
                                    .setSelection(rbdMngr.isAutoUpdate());
                            autoUpdateButton.setEnabled(false);
                        } else if (newDomRsc.isAutoUpdateable()) {
                            autoUpdateButton.setEnabled(true);
                            autoUpdateButton.setSelection(true);
                            if (rbdMngr.getRbdType()
                                    .equals(NcDisplayType.GRAPH_DISPLAY)) {
                                geoSyncPanesToggle.setSelection(true);
                                rbdMngr.syncPanesToArea();
                            }
                        } else {
                            autoUpdateButton.setSelection(false);
                            autoUpdateButton.setEnabled(false);
                        }
                    }
                });

        timelineGroup.pack();
        shell.pack();
        shell.setSize(initDlgSize);

        geoSyncPanesToggle.setSelection(rbdMngr.isGeoSyncPanes());
        multiPaneToggle.setSelection(rbdMngr.isMultiPane());

        updateGUIforMultipane(rbdMngr.isMultiPane());

        // set the pane sel buttons based on the combos also sets the rbdMngr's
        // layout
        if (rbdMngr.isMultiPane()) {
            updatePaneLayout();
        }

        selectPane(rbdMngr.getSelectedPaneId());

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

        if (timeMatcher.getDominantResource() != null
                && timeMatcher.getDominantResource().isAutoUpdateable()) {
            autoUpdateButton.setEnabled(true);
            autoUpdateButton.setSelection(rbdMngr.isAutoUpdate());
        } else {
            autoUpdateButton.setSelection(false);
            autoUpdateButton.setEnabled(false);
        }
    }

    // TODO : we could have the pane buttons indicate whether
    // there are resources selected for them by changing the foreground color to
    // yellow or green...
    private void updatePaneLayout() {

        int colCnt = ((NcPaneLayout) rbdMngr.getPaneLayout()).getColumns();
        int rowCnt = ((NcPaneLayout) rbdMngr.getPaneLayout()).getRows();

        for (int r = 0; r < rbdMngr.getMaxPaneLayout().getRows(); r++) {
            for (int c = 0; c < rbdMngr.getMaxPaneLayout().getColumns(); c++) {
                paneSelectionButtons[r][c].setVisible(r < rowCnt && c < colCnt);
            }
        }
    }

    // update the GUI with the selections (area/list of rscs) for the selected
    // pane
    private void selectPane(INcPaneID pane) {

        NcPaneID seldPane = (NcPaneID) pane;

        rbdMngr.setSelectedPaneId(seldPane);

        if (rbdMngr.isMultiPane()) {
            selectedResourceGroup.setText(
                    "Selected Resources for Pane " + seldPane.toString());
        } else {
            selectedResourceGroup.setText("Selected Resources");
        }

        // implement radio behavior
        for (int r = 0; r < rbdMngr.getMaxPaneLayout().getRows(); r++) {
            for (int c = 0; c < rbdMngr.getMaxPaneLayout().getColumns(); c++) {
                paneSelectionButtons[r][c].setSelection(
                        (r == seldPane.getRow() && c == seldPane.getColumn()));
            }
        }

        updateAreaGUI();

        updateSelectedResourcesView(true);
    }

    public void removeSelectedResource(ResourceSelection rscSel) {
        if (groupListViewer.getSelection() == null
                || groupListViewer.getSelection().isEmpty()
                || groupListViewer.getTable().getSelection()[0].getText()
                        .equalsIgnoreCase(unGroupString)) {
            rbdMngr.removeSelectedResource(rscSel);
        } else { // remove from group
            ((GroupResourceData) getGroupResourceSelection().getResourceData())
                    .getResourceList().remove(rscSel.getResourcePair());
        }

        // remove this from the list of available dominant resources.
        if (rscSel
                .getResourceData() instanceof AbstractNatlCntrsRequestableResourceData) {
            timelineControl.removeAvailDomResource(
                    (AbstractNatlCntrsRequestableResourceData) rscSel
                            .getResourceData());
        }

    }

    /**
     * Removes a User Defined Resource Group. Removes resources from the list of
     * dominant resources. Removes resources from the dominant resource combo.
     * If there are no resources left in any group, it will also remove the
     * timeline.
     * 
     * @param rscSel
     *            the user defined group to remove
     */
    private void removeUserDefinedResourceGroup(ResourceSelection rscSel) {
        AbstractResourceData data = null;

        // Remove resources from the resource list
        rbdMngr.removeSelectedResource(rscSel);

        // Remove resources from the dominant resource combo and remove the
        // timeline if no resources are left in any user group or the static
        // group
        ResourceList groupResourceList = ((GroupResourceData) rscSel
                .getResourceData()).getResourceList();

        for (int i = 0; i < groupResourceList.size(); i++) {
            data = groupResourceList.get(i).getResourceData();

            if (data instanceof AbstractNatlCntrsRequestableResourceData) {
                timelineControl.removeAvailDomResource(
                        (AbstractNatlCntrsRequestableResourceData) data);
            }
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

        currentlySelectedGroup = -1;
        groupListViewer.setInput(rbdMngr.getGroupResources());
        selectedResourceViewer.setInput(rbdMngr.getUngroupedResources());
        selectedResourceViewer.refresh();
        setGroupButtons();

    }

    // reset the ListViewer's input and update all of the buttons
    // TODO; get the currently selected resources and reselect them.
    private void updateSelectedResourcesView(boolean updateList) {

        if (updateList) {

            StructuredSelection orig_sel_elems = (StructuredSelection) selectedResourceViewer
                    .getSelection();

            List<?> origSeldRscsList = orig_sel_elems.toList();

            if (groupListViewer.getSelection().isEmpty()
                    || groupListViewer.getTable().getSelection()[0].getText()
                            .equalsIgnoreCase(unGroupString)) {
                selectedResourceViewer
                        .setInput(rbdMngr.getUngroupedResources());
            } else {
                selectedResourceViewer.setInput(rbdMngr.getResourcesInGroup(
                        groupListViewer.getTable().getSelection().length == 0
                                ? null
                                : groupListViewer.getTable().getSelection()[0]
                                        .getText()));
            }
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

        // the Clear button is enabled if there are more than 1 resources in the
        // list.
        clearPaneButton.setEnabled(numRscs > 1);

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

        // the replace button is enabled if there is only 1 resource selected
        // and it is not the base resource
        rscSelDlg.setReplaceEnabled((numSeldRscs == 1 && !isBaseLevelRscSeld));

        // the delete button is only disabled if there is one the one base
        // resource selected.
        deleteResourceButton.setEnabled(
                numSeldRscs > 1 || (numSeldRscs == 1 && !isBaseLevelRscSeld));

        // the disable_rsc_btn is always enabled.
        disableResourceButton.setEnabled((numSeldRscs > 0));
        moveResourceDownButton.setEnabled((numSeldRscs == 1) && !lastSelected);
        moveResourceUpButton.setEnabled((numSeldRscs == 1) && !firstSelected);

        if (allRscsAreVisible) {
            disableResourceButton.setSelection(false);
            disableResourceButton.setText("Turn Off");
        } else {
            disableResourceButton.setSelection(true);
            disableResourceButton.setText("Turn On");
        }
    }

    // This will load the currently configured RBD (all panes) into
    // a new editor or the active editor if the name matches the current
    // name of the RBD
    public void loadRBD(boolean close) {

        String rbdName = rbdNameText.getText().trim();

        if (rbdName == null || rbdName.isEmpty()) {
            rbdName = "Preview";
        }

        // Since rbdLoader uses the same resources in the rbdBundle to load in
        // the editor, we will need to make a copy here so that future edits are
        // not immediately reflected in the loaded display. The easiest way to
        // do this is to marshal and then unmarshal the rbd.
        try {

            rbdMngr.setGeoSyncPanes(geoSyncPanesToggle.getSelection());
            rbdMngr.setAutoUpdate(autoUpdateButton.getSelection());

            AbstractRBD<?> rbdBndl = rbdMngr.createRbdBundle(rbdName,
                    timelineControl.getTimeMatcher());

            rbdBndl = AbstractRBD.clone(rbdBndl);
            rbdBndl.resolveDominantResource();

            ResourceBndlLoader rbdLoader = null;
            rbdLoader = new ResourceBndlLoader("RBD Previewer");

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

            // 3- if the rbd was imported from a display and the name was not
            // changed get this display and attempt to use it.

            if (editor == null) {
                NcDisplayName importDisplayName = NcDisplayName
                        .parseNcDisplayNameString(importRbdCombo.getText());

                if (importDisplayName.getName().equals(rbdName)) {
                    // get by ID since the rbd name doesn't have to be unique
                    editor = NcDisplayMngr.findDisplayByID(
                            rbdBndl.getDisplayType(), importDisplayName);

                    if (editor != null && NcEditorUtil.getPaneLayout(editor)
                            .compare(rbdBndl.getPaneLayout()) != 0) {
                        editor = null;
                    }
                }
            }

            // 4-reuse if it is a Preview editor.
            if (editor == null && rbdName.equals("Preview")) {
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
            ResourceSelection rsel = getGroupResourceSelection();

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
                importRbdCombo.add(editor.getPartName());
                importRbdCombo.setText(editor.getPartName());
                rbdMngr.setRbdModified(false);
                importRBD(editor.getPartName());
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
        String rbdName = rbdNameText.getText();

        if (rbdName == null || rbdName.isEmpty()) {
            rbdName = "Preview";
        }

        // Since rbdLoader uses the same resources in the rbdBundle to load in
        // the editor, we will need to make a copy here so that future edits are
        // not immediately reflected in the loaded display. The easiest way to
        // do this is to marshal and then unmarshall the rbd.
        try {
            rbdMngr.setGeoSyncPanes(geoSyncPanesToggle.getSelection());
            rbdMngr.setAutoUpdate(autoUpdateButton.getSelection());

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

    public void importRBD(String seldDisplayName) {

        AbstractRBD<?> impRbd;
        if (seldDisplayName.equals(ImportFromSPF)) {

            SelectRbdsDialog impDlg = new SelectRbdsDialog(shell, "Import RBD",
                    false, false, false);

            if (!impDlg.open()) {
                return;
            }

            impRbd = impDlg.getSelectedRBD();

            impRbd.resolveLatestCycleTimes();
        } else {
            // get NcMapRBD from selected display
            AbstractEditor seldEditor = NcDisplayMngr.findDisplayByID(
                    NcDisplayName.parseNcDisplayNameString(seldDisplayName));

            if (seldEditor == null) {
                statusHandler.handle(Priority.PROBLEM,
                        "Unable to load Display :"
                                + seldDisplayName.toString());
                return;
            }

            try {
                impRbd = AbstractRBD.createRbdFromEditor(seldEditor);

                impRbd = AbstractRBD.clone(impRbd);
            } catch (VizException e) {
                MessageDialog errDlg = new MessageDialog(shell, "Error", null,
                        "Error Importing Rbd from Display, "
                                + seldDisplayName.toString() + ".\n"
                                + e.getMessage(),
                        MessageDialog.ERROR, new String[] { "OK" }, 0);
                errDlg.open();
                return;
            }

        }

        // if any selections have been made then popup a confirmation msg
        if (rbdMngr.isRbdModified()) {
            MessageDialog confirmDlg = new MessageDialog(shell, "Confirm", null,
                    "You are about to replace the entire contents of this dialog. There is no 'undo'.\n\n"
                            + "Do you want to continue the import and clear the current RBD selections?",
                    MessageDialog.QUESTION, new String[] { "Yes", "No" }, 0);
            confirmDlg.open();

            if (confirmDlg.getReturnCode() != MessageDialog.OK) {
                return;
            }
        }

        NcDisplayType curDispType = rbdMngr.getRbdType();

        try {
            rbdMngr.initFromRbdBundle(impRbd);

            groupListViewer.setInput(rbdMngr.getGroupResources());
            // the new group is always added at the top.
            if (currentlySelectedGroup != -1) {
                groupListViewer.getTable().setSelection(currentlySelectedGroup);
                groupListViewer.refresh();
            } else {
                this.selectUngroupedGrp();
            }

        } catch (VizException e) {
            rbdMngr.init(curDispType);

            MessageDialog errDlg = new MessageDialog(shell, "Error", null,
                    "Error Importing RBD:" + impRbd.getRbdName() + "\n\n"
                            + e.getMessage(),
                    MessageDialog.ERROR, new String[] { "OK" }, 0);
            errDlg.open();
        }

        updateGUI();

        // updateGUI triggers the spinner which ends up calling
        // rbdMngr.setPaneLayout(), so we need to reset this here.
        rbdMngr.setRbdModified(false);

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

        updateAreaGUI();

        updateSelectedResourcesView(true);
    }

    private void updateGUIforMultipane(boolean isMultiPane) {
        FormData fd = new FormData();

        geoSyncPanesToggle.setVisible(isMultiPane);

        paneLayoutGroup.setVisible(isMultiPane);

        if (isMultiPane) {
            groupGrp.setVisible(false);

            fd.left = new FormAttachment(geoAreaGroup, 10, SWT.RIGHT);
            fd.top = new FormAttachment(geoSyncPanesToggle, 10, SWT.BOTTOM);
            fd.bottom = new FormAttachment(geoAreaGroup, 0, SWT.BOTTOM);
            fd.right = new FormAttachment(100, -300);

            selectedResourceGroup.setLayoutData(fd);

            shell.setSize(new Point(multiPaneDlgWidth, shell.getSize().y));
        } else {
            groupGrp.setVisible(true);

            fd.left = new FormAttachment(geoAreaGroup, 10, SWT.RIGHT);
            fd.top = new FormAttachment(autoUpdateButton, 5, SWT.BOTTOM);
            fd.right = new FormAttachment(100, -10);
            fd.bottom = new FormAttachment(geoAreaGroup, 0, SWT.BOTTOM);
            shell.setSize(new Point(multiPaneDlgWidth - 10, shell.getSize().y));
        }

        // the area name may be truncated based on a shorter toolbar widget
        // reset it now that it is wider.
        PredefinedArea area = rbdMngr.getSelectedArea();
        setAreaTextOnMenuItem(
                new AreaName(area.getSource(), area.getAreaName()));
    }

    public void saveRBD(boolean new_pane) {
        try {
            NCTimeMatcher timeMatcher = timelineControl.getTimeMatcher();
            boolean saveRefTime = !timeMatcher.isCurrentRefTime();
            boolean saveTimeAsConstant = false; // TODO -- how should this be
                                                // set???

            SpfsManager spfsMgr = SpfsManager.getInstance();
            savedSpfGroup = spfsMgr.getLatestSpfGroup();
            savedSpfName = spfsMgr.getLatestSpfName();
            savedRbdName = spfsMgr.getLatestRbdName();

            // get the filename to save to.
            SaveRbdDialog saveDlg = new SaveRbdDialog(shell, savedSpfGroup,
                    savedSpfName, savedRbdName, saveRefTime,
                    saveTimeAsConstant);

            if ((Boolean) saveDlg.open() == false) {
                return;
            }

            savedSpfGroup = saveDlg.getSeldSpfGroup();
            savedSpfName = saveDlg.getSeldSpfName();
            savedRbdName = saveDlg.getSeldRbdName();

            saveRefTime = saveDlg.getSaveRefTime();
            saveTimeAsConstant = saveDlg.getSaveTimeAsConstant();

            // Set the name to the name that was actually used to save the RBD.
            rbdNameText.setText(savedRbdName);

            rbdMngr.setGeoSyncPanes(geoSyncPanesToggle.getSelection());
            rbdMngr.setAutoUpdate(autoUpdateButton.getSelection());

            AbstractRBD<?> rbdBndl = rbdMngr.createRbdBundle(savedRbdName,
                    timeMatcher);

            spfsMgr.saveRbdToSpf(savedSpfGroup, savedSpfName, rbdBndl,
                    saveRefTime, saveTimeAsConstant);

            VizApp.runSync(new Runnable() {
                @Override
                public void run() {
                    String msg = null;
                    msg = new String(
                            "Resource Bundle Display " + rbdNameText.getText()
                                    + " Saved to SPF " + savedSpfGroup
                                    + File.separator + savedSpfName + ".");
                    MessageBox mb = new MessageBox(shell, SWT.OK);
                    mb.setText("RBD Saved");
                    mb.setMessage(msg);
                    mb.open();

                    rbdMngr.setRbdModified(false);
                }
            });
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

    // if Editing then save the current rbd to an AbstractRBD<?>
    private void createEditedRbd() {

        try {
            NCTimeMatcher timeMatcher = timelineControl.getTimeMatcher();

            if (!rbdNameText.getText().isEmpty()) {
                rbdMngr.setRbdName(rbdNameText.getText());
            }

            rbdMngr.setGeoSyncPanes(geoSyncPanesToggle.getSelection());
            rbdMngr.setAutoUpdate(autoUpdateButton.getSelection());

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
        // In this method, if the CreateRbdControl object is not disposed and
        // the part that is brought to top is an instance of NatlCntrsEditor,
        // loop through item strings in import_rbd_combo and find the item which
        // is identical to the title string of the NatlCntrsEditor. Set that
        // item as selected for import_rbd_combo and call importRBD() for that
        // item to update RBD contents.

        AbstractEditor seldEditor = NcDisplayMngr.findDisplayByID(
                NcDisplayName.parseNcDisplayNameString(partRef.getPartName()));

        if ((false == this.isDisposed())
                && seldEditor instanceof NatlCntrsEditor) {

            for (String item : importRbdCombo.getItems()) {
                if (item.equalsIgnoreCase(seldEditor.getPartName())) {
                    try {
                        importRBD(item);
                    } catch (NullPointerException npe) {
                        // null DataTime can cause a panic here but is not an
                        // erroneous situation
                    }
                }
            }
        }

    }

    private void addResourceGroup(String name) {
        ResourcePair group = new ResourcePair();

        // group always at bottom
        group.setProperties(new ResourceProperties());
        group.getProperties().setRenderingOrder(NCP_GROUP_RENDERING_ORDER);

        // Add group, the default hot key is F1 and will be re-assigned at
        // the time all groups are loaded.
        grpColorBtn.setColorValue(GempakColor.convertToRGB(grpColor++));

        GroupResourceData grd = new GroupResourceData(name, 1,
                grpColorBtn.getColorValue());
        group.setResourceData(grd);
        try {
            ResourceSelection sel = ResourceFactory.createResource(group);
            rbdMngr.addSelectedResource(sel, false);
            groupListViewer.setInput(rbdMngr.getGroupResources());

            // the new group is always added at the top.
            groupListViewer.getTable().setSelection(1);
            currentlySelectedGroup = 0;

            groupListViewer.refresh();

            selectedResourceViewer.setInput(null);
            selectedResourceViewer.refresh();

        } catch (VizException e) {

        }

    }

    private void setGroupButtons() {

        if (groupListViewer.getTable().getSelectionCount() <= 0
                || groupListViewer.getTable().getSelection()[0].getText()
                        .equalsIgnoreCase(unGroupString)) {
            grpMoveUpBtn.setEnabled(false);
            grpMoveDownBtn.setEnabled(false);
            grpColorComp.setEnabled(false);
            delGrpBtn.setEnabled(false);

        } else if (groupListViewer.getTable().getItemCount() == 1) {
            grpMoveUpBtn.setEnabled(false);
            grpMoveDownBtn.setEnabled(false);
            grpColorComp.setEnabled(true);
            delGrpBtn.setEnabled(true);
        } else {
            int idx = groupListViewer.getTable().getSelectionIndex();
            if (idx == 1) {
                grpMoveUpBtn.setEnabled(false);
                grpMoveDownBtn.setEnabled(true);
                grpColorComp.setEnabled(true);
                delGrpBtn.setEnabled(true);
            }
            if (idx == groupListViewer.getTable().getItemCount() - 1) {
                grpMoveUpBtn.setEnabled(true);
                grpMoveDownBtn.setEnabled(false);
                grpColorComp.setEnabled(true);
                delGrpBtn.setEnabled(true);
            }
            if (idx != 1
                    && idx != groupListViewer.getTable().getItemCount() - 1) {
                grpMoveUpBtn.setEnabled(true);
                grpMoveDownBtn.setEnabled(true);
                grpColorComp.setEnabled(true);
                delGrpBtn.setEnabled(true);
            }
        }

        // set the color button
        ResourceSelection sel = getGroupResourceSelection();
        if (sel != null) {
            grpColorBtn.setColorValue(sel.getResourceData().getLegendColor());
        }

    }

    private ResourceSelection getGroupResourceSelection() {
        StructuredSelection isel = ((StructuredSelection) groupListViewer
                .getSelection());
        return (ResourceSelection) isel.getFirstElement();
    }

    private void selectUngroupedGrp() {
        groupListViewer.getTable()
                .setSelection(groupListViewer.getTable().getItemCount() - 1);
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

    /**
     * Peform all of the necessary tasks for when a Resource Group is selected
     */
    public void handleSelectedResourceGroup() {
        currentlySelectedGroup = groupListViewer.getTable().getSelectionIndex();
        if (groupListViewer.getTable().getSelection()[0].getText()
                .equalsIgnoreCase(unGroupString)) {
            currentlySelectedGroup = -1;
            selectedResourceViewer.setInput(rbdMngr.getUngroupedResources());
        } else {
            selectedResourceViewer.setInput(rbdMngr.getResourcesInGroup(
                    groupListViewer.getTable().getSelection().length == 0 ? null
                            : groupListViewer.getTable().getSelection()[0]
                                    .getText()));
        }

        setGroupButtons();
    }

}
