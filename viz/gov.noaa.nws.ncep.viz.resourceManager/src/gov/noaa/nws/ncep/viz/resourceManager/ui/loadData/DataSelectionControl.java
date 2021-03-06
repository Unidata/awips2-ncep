package gov.noaa.nws.ncep.viz.resourceManager.ui.loadData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.exception.VizException;

import gov.noaa.nws.ncep.viz.common.display.NcDisplayType;
import gov.noaa.nws.ncep.viz.common.preferences.NcepGeneralPreferencesPage;
import gov.noaa.nws.ncep.viz.common.ui.NmapCommon;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData.DayReference;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData.TimelineGenMethod;
import gov.noaa.nws.ncep.viz.resources.manager.AttrSetLabelsManager;
import gov.noaa.nws.ncep.viz.resources.manager.AttributeSet;
import gov.noaa.nws.ncep.viz.resources.manager.LocalRadarStationManager;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceCategory;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceDefinition;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceDefnsMngr;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceName;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;

/**
 * Data Selection dialog.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 01/26/10      #226        Greg Hull   Broke out from RscBndlDefnDialog
 * 04/05/10      #226        Greg Hull   Add back PGEN selection
 * 06/18/10      #273        Greg Hull   Rework for new ResourceCnfgMngr
 * 09/13/10      #307        Greg Hull   implement cycle times.
 * 09/28/10      #307        Greg Hull   save the fcst/observed mode when re-initing
 * 10/01/10      #298        B. Hebbard  handle MOS resources in updateCycleTimes()
 * 10/20/10                  X. Guo      Rename getCycleTimeStringFromDataTime to getTimeStringFromDataTime
 * 10/20/10      #277        M. Li       get model name for ensemble
 * 11/18/10       277        M. Li       get correct cycle for ensemble
 * 11/29/10                 mgamazaychikov  Changed updateCycleTime method for GEMPAK data source
 * 02/28/11      #408        Greg Hull   Replace Forecast/Observed with Filter combo
 * 04/18/11                  Greg Hull   caller sets name of the 'select' button
 * 06/07/11       #445       Xilin Guo   Data Manager Performance Improvements
 * 09/20/2011               mgamazaychikov  Made changes associated with removal of DatatypeTable class
 * 12/22/2011     #578       Greg Hull   Ensemble selection
 * 01/06/2012                S. Gurung   Add/display cycle times at 00Z only for nctaf
 * 01/10/2012                S. Gurung   Changed resource parameter name plugin to pluginName in updateCycleTimes()
 * 01/31/2012     #606       Greg Hull   Get Cycle Times, Types & Sub-Types from inventory
 * 04/08/2012     #606       Greg Hull   Don't allow selection for data that is not available
 * 04/25/2012     #606       Greg Hull   allow disabling the inventory, add Check Availability button
 * 06/06/2012     #816       Greg Hull   Alphabetize lists. Change content of listViewer to ResourceDefinitions
 * 08/26/2012     #          Greg Hull   allow for disabling resources
 * 08/29/2012     #860       Greg Hull   show latest time with attr sets
 * 12/17/2012     #957       Greg Hull   change content of attrSetListViewer from String to to AttributeSet 
 * 02/22/2013     #972       G. Hull     Only show resources for given NcDisplayType
 * 04/11/2013     #864       G. Hull     rm special case for taf and use USE_FCST_FRAME_INTERVAL_FROM_REF_TIME
 * 04/15/2013     #864       G. Hull     attach LViewers to positions and save previous width
 * 10/24/2013     #1043      G. Hull     init Select Resource GUI to highlighted rsc
 * 07/23/2014       ?        B. Yin      Handle grid analysis 
 * 07/23/2014       ?        B. Hebbard  Make extensible for NTRANS-specific subclass
 * 05/18/2015     R8048     P. Chowdhuri "Select New Resource" dialog should remember last selection
 * 05/18/2015     R7656      A. Su       Displayed the aliases of local radar stations in the menu.
 * 06/10/2015     R7656      A. Su       Rewrote the displaying logic for LocalRadar for clarity.
 * 10/15/2015     R7190      R. Reynolds Display subTypeGenerator and attributes mods.
 * 10/30/2015     R8824      A. Su       Added display aliases for Grid resource attributes.
 * 11/03/2015     R8554     P. Chowdhuri Filter was set to last filter used per data category
 * 12/03/2015     R12953    R. Reynolds  Added Mcidas constants
 * 12/17/2015     R8554     A. Su        Modified to remember last selected Resource and filter per RBD type.
 * 01/25/2016     R14142    RCReynolds   Moved mcidas related string construction out to ResourceDefinition
 * 01/27/2016     R12859    A. Su        Sorted the list of cycle times in the cycleTimeCombo widget.
 *                                       Removed unneeded code for remembering last selected resource.
 *                                       Removed dead code for mcidas.
 * 04/05/2016   RM#10435    rjpeter      Removed Inventory usage.
 * 06/16/2016     R17949    Jeff Beck    Add the capability to select PGEN resources from available times,
 *                                       using a comboBox and using the same UI layout familiar to users when selecting "Cycle Times".
 *                                       Previous PGEN data in the UI will be labeled "Available Times" not "Cycle Times".
 * 08/15/2016     R17368    Jeff Beck    If a resource in the parent window (Create RBD) is selected, display it
 *                                       when opening this Resource Selection Control. Include cycle times. 
 *                                       If there's unselected resource(s), display the most previous one, with cycle times.
 * 11/04/2016     R23113    K.Bugenhagen In updateCycletimes, get cycletime in milliseconds.
 * 
 * </pre>
 * 
 * @author ghull
 * @version 1
 */
public class DataSelectionControl extends Composite {

    protected static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(DataSelectionControl.class);

    // All GRID resource attributes are specified in this directory.
    public static final String GRID_DATA = "ModelFcstGridContours";

    public static final String NATL_MOSAIC_DISPLAY_NAME = "NatlMosaic";

    public static final String LOCAL_RADAR_PREFIX = "LocalRadar:";

    public static final String RADAR_CATEGORY = "RADAR";

    public static final String RSC_FILTER_ALL = "All";

    public static final String GMT = "GMT";

    public static final String GDFILE = "GDFILE";

    public static final String DEFAULT = "default";

    public static final String STANDARD = "standard";

    public static final String OK = "OK";

    protected NcDisplayType displayType = null;

    protected ResourceName prevSelectedRscName = null;

    /**
     * A mapping from NcDisplayType to last selected ResourceName for this type.
     */
    protected static HashMap<NcDisplayType, ResourceName> prevDisplayType2RscName = new HashMap<>();

    /**
     * A mapping from Category to last selected filter for this category.
     */
    protected static HashMap<ResourceCategory, String> prevCat2SelectedFilter = new HashMap<>();

    /**
     * A mapping from Category to last selected ResourceName for this category.
     */
    protected static HashMap<ResourceCategory, ResourceName> prevCat2SelectedRscName = new HashMap<>();

    private static ResourceCategory prevSelectedCat = ResourceCategory.NullCategory;

    protected ResourceDefnsMngr rscDefnsMngr;

    protected ResourceName selectedRscName = null;

    protected Composite sel_rsc_comp = null;

    protected Label availDataTimeLbl = null;

    protected Combo cycleTimeCombo = null;

    private boolean openingDialogWithResources = false;

    // For now only one of following two will be visible but we may want to
    // allow both later (and remove the Modify button from the Create RBD tab)
    protected Button addResourceBtn = null;
    
    protected Label rscTypeLbl = null;

    private Label rscTypeGroupLbl = null;

    protected ListViewer rscCatLViewer = null;

    protected ListViewer rscTypeLViewer = null;

    private ListViewer rscGroupLViewer = null;

    protected ListViewer rscAttrSetLViewer = null;

    protected static final int RSC_LIST_VIEWER_HEIGHT = 220;

    protected Boolean showLatestTimes = false;

    protected Boolean onlyShowResourcesWithData = false;

    // Used in justifying the times in the attrSetsList.
    protected Integer maxLengthOfSelectableAttrSets = 0;

    public interface IResourceSelectedListener {
        public void resourceSelected(ResourceName rscName, boolean replace,
                boolean addAllPanes, boolean done);
    }

    private final Set<IResourceSelectedListener> rscSelListeners = new HashSet<>();

    private Cursor waitCursor = null;

    public DataSelectionControl(Composite parent, ResourceName initRscName,
            NcDisplayType dispType) throws VizException {
        super(parent, SWT.SHADOW_NONE);

        displayType = dispType;
        prevSelectedRscName = prevDisplayType2RscName.get(displayType);

        showLatestTimes = NmapCommon.getNcepPreferenceStore()
                .getBoolean(NcepGeneralPreferencesPage.ShowLatestResourceTimes);
        onlyShowResourcesWithData = false;

        rscDefnsMngr = ResourceDefnsMngr.getInstance();

        sel_rsc_comp = this;

        GridData gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;

        sel_rsc_comp.setLayoutData(gd);

        sel_rsc_comp.setLayout(new FormLayout());

        createSelectResourceGroup();

        setContentProviders();
        addSelectionListeners();

        initWidgets(initRscName);

        waitCursor = new Cursor(getDisplay(), SWT.CURSOR_WAIT);
    }

    /**
     * This is a pass-thru constructor, so NtransSelectionControl can call the
     * grandparent constructor, but do its own version of the useful work of the
     * main constructor above.
     * 
     * @param Composite
     *            parent
     * 
     *            Discussion: Better solution might be to do a refactor "pull"
     *            AbstractDataSelectionControl superclass out of
     *            DataSelectionControl and have NtransSelectionControl
     *            extend the former. But that could disturb existing
     *            DataSelectionControl, which is working and tested with
     *            non-NTRANS resources.
     */
    public DataSelectionControl(Composite parent) throws VizException {
        super(parent, SWT.SHADOW_NONE);
    }

    /*
     * create all the widgets in the Resource Selection (top) section of the
     * sashForm.
     */
    private void createSelectResourceGroup() {

        rscCatLViewer = new ListViewer(sel_rsc_comp,
                SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        FormData fd = new FormData();
        fd.height = RSC_LIST_VIEWER_HEIGHT;
        fd.top = new FormAttachment(0, 0);
        fd.left = new FormAttachment(0, 0);
        fd.right = new FormAttachment(0, 120);

        // This allows a resize to change the size of the lists.
        fd.bottom = new FormAttachment(100, -40);
        rscCatLViewer.getList().setLayoutData(fd);

        Label rscCatLbl = new Label(sel_rsc_comp, SWT.NONE);
        rscCatLbl.setText("Category");
        fd = new FormData();
        fd.left = new FormAttachment(rscCatLViewer.getList(), 0, SWT.LEFT);
        fd.bottom = new FormAttachment(rscCatLViewer.getList(), -3, SWT.TOP);
        rscCatLbl.setLayoutData(fd);

        // first create the lists and then attach the label to the top of them
        rscTypeLViewer = new ListViewer(sel_rsc_comp,
                SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        fd = new FormData();
        fd.height = RSC_LIST_VIEWER_HEIGHT;
        fd.width = 160;
        fd.top = new FormAttachment(rscCatLViewer.getList(), 0, SWT.TOP);
        fd.left = new FormAttachment(rscCatLViewer.getList(), 8, SWT.RIGHT);
        //fd.right = new FormAttachment(37, 0);

        fd.bottom = new FormAttachment(rscCatLViewer.getList(), 0, SWT.BOTTOM);
        rscTypeLViewer.getList().setLayoutData(fd);

        rscTypeLbl = new Label(sel_rsc_comp, SWT.NONE);
        rscTypeLbl.setText("Resource Type");
        fd = new FormData();
        fd.left = new FormAttachment(rscTypeLViewer.getList(), 0, SWT.LEFT);
        fd.bottom = new FormAttachment(rscTypeLViewer.getList(), -3, SWT.TOP);

        rscTypeLbl.setLayoutData(fd);

        // first create the lists and then attach the label to the top of them
        rscGroupLViewer = new ListViewer(sel_rsc_comp,
                SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        fd = new FormData();
        fd.height = RSC_LIST_VIEWER_HEIGHT;
        fd.width = 180;
        fd.top = new FormAttachment(rscTypeLViewer.getList(), 0, SWT.TOP);
        fd.left = new FormAttachment(rscTypeLViewer.getList(), 8, SWT.RIGHT);
        //fd.right = new FormAttachment(62, 0);
        fd.bottom = new FormAttachment(rscTypeLViewer.getList(), 0, SWT.BOTTOM);
        rscGroupLViewer.getList().setLayoutData(fd);

        rscTypeGroupLbl = new Label(sel_rsc_comp, SWT.NONE);
        rscTypeGroupLbl.setText("Resource Group");
        fd = new FormData();
        fd.left = new FormAttachment(rscGroupLViewer.getList(), 0, SWT.LEFT);
        fd.bottom = new FormAttachment(rscGroupLViewer.getList(), -3, SWT.TOP);
        rscTypeGroupLbl.setLayoutData(fd);

        rscAttrSetLViewer = new ListViewer(sel_rsc_comp,
                SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        fd = new FormData();
        fd.height = RSC_LIST_VIEWER_HEIGHT;
        fd.top = new FormAttachment(rscGroupLViewer.getList(), 0, SWT.TOP);
        fd.left = new FormAttachment(rscGroupLViewer.getList(), 8, SWT.RIGHT);
        fd.right = new FormAttachment(100, -10);
        fd.bottom = new FormAttachment(rscGroupLViewer.getList(), 0, SWT.BOTTOM);
        rscAttrSetLViewer.getList().setLayoutData(fd);

        Label rscAttrsLbl = new Label(sel_rsc_comp, SWT.NONE);
        rscAttrsLbl.setText("Resource Attributes");
        fd = new FormData();
        fd.width = 200;
        fd.left = new FormAttachment(rscAttrSetLViewer.getList(), 0, SWT.LEFT);
        fd.bottom = new FormAttachment(rscAttrSetLViewer.getList(), -3, SWT.TOP);
        rscAttrsLbl.setLayoutData(fd);

        availDataTimeLbl = new Label(sel_rsc_comp, SWT.None);
        availDataTimeLbl.setText("");
        fd = new FormData();
        fd.left = new FormAttachment(rscAttrSetLViewer.getList(), 0, SWT.LEFT);
        fd.top = new FormAttachment(rscAttrSetLViewer.getList(), 5, SWT.BOTTOM);
        fd.right = new FormAttachment(rscAttrSetLViewer.getList(), 0, SWT.RIGHT);
        availDataTimeLbl.setLayoutData(fd);

        addResourceBtn = new Button(sel_rsc_comp, SWT.None);

        fd = new FormData();
        fd.top = new FormAttachment(rscCatLViewer.getList(), 10, SWT.BOTTOM);
        fd.right = new FormAttachment(100, -10);
        
        addResourceBtn.setLayoutData(fd);
        addResourceBtn.setText(" Add Resource "); // Add To RBD

        // allow the user to enter any previous datatime
        cycleTimeCombo = new Combo(sel_rsc_comp, SWT.READ_ONLY);
        fd = new FormData();
        fd.width = 200;
        fd.right = new FormAttachment(addResourceBtn, -10, SWT.LEFT);
        fd.top = new FormAttachment(addResourceBtn, 0, SWT.TOP);

        cycleTimeCombo.setLayoutData(fd);

    }

    private void setContentProviders() {

        // input is the rscDefnsMngr and output is a list of categories based
        // on the forecast flag
        rscCatLViewer.setContentProvider(new IStructuredContentProvider() {
            @Override
            public Object[] getElements(Object inputElement) {

                // don't show disabled definitions.
                return rscDefnsMngr.getResourceCategories(false,
                        new NcDisplayType[] { displayType });
            }

            @Override
            public void dispose() {
            }

            @Override
            public void inputChanged(Viewer viewer, Object oldInput,
                    Object newInput) {
            }
        });

        rscTypeLViewer.setContentProvider(new IStructuredContentProvider() {
            @Override
            public Object[] getElements(Object inputElement) {

                if (selectedRscName
                        .getRscCategory() != ResourceCategory.NullCategory) {
                    try {
                        List<ResourceDefinition> rscTypes = rscDefnsMngr
                                .getResourceDefnsForCategory(
                                        selectedRscName.getRscCategory(),
                                        "", displayType, true,
                                        false);

                        return rscTypes.toArray();
                    } catch (VizException e) {
                        MessageDialog errDlg = new MessageDialog(
                                NcDisplayMngr.getCaveShell(), "Error", null,
                                "Error getting Resource Types\n"
                                        + e.getMessage(),
                                MessageDialog.ERROR, new String[] { OK }, 0);
                        errDlg.open();
                    }
                }
                return new ResourceDefinition[] {};
            }

            @Override
            public void dispose() {
            }

            @Override
            public void inputChanged(Viewer viewer, Object oldInput,
                    Object newInput) {
            }
        });

        rscTypeLViewer.setComparator(new ViewerComparator() {

            @Override
            public int category(Object element) {
                ResourceDefinition rd = (ResourceDefinition) element;
                return (rd.isForecast() ? 1 : 0);
            }

            @Override
            public int compare(Viewer viewer, Object obj1, Object obj2) {
                int catComp = category(obj1) - category(obj2);
                return (catComp != 0 ? catComp
                        : rscDefnsMngr.getDefaultRscDefnComparator().compare(
                                (ResourceDefinition) obj1,
                                (ResourceDefinition) obj2));
            }
        });

        rscTypeLViewer.setLabelProvider(new LabelProvider() {

            @Override
            public String getText(Object element) {
                if (element == null) {
                    return "null";
                }

                ResourceDefinition rd = (ResourceDefinition) element;
                String rdName = rd.getResourceDefnName();

                // Display aliases for LocalRadar.
                if (rd.getResourceCategory()
                        .equals(ResourceCategory.RadarRscCategory)) {

                    String alias = LocalRadarStationManager.getInstance()
                            .getDisplayedName(rdName);

                    if (alias != null) {
                        rdName = alias;
                    }
                }

                return rdName;
            }
        });

        // Override the method "compare" in the class "ViewerSorter" to
        // properly sort the menu items (aliases) of local radar stations.
        rscTypeLViewer.setSorter(new ViewerSorter() {

            @Override
            public int compare(Viewer viewer, Object obj1, Object obj2) {

                boolean isLocalRadar = ((obj1 != null)
                        && ((ResourceDefinition) obj1).getResourceCategory()
                                .equals(ResourceCategory.RadarRscCategory))
                        || ((obj2 != null) && ((ResourceDefinition) obj2)
                                .getResourceCategory()
                                .equals(ResourceCategory.RadarRscCategory));

                if (!isLocalRadar) {
                    return super.compare(viewer, obj1, obj2);
                }

                if (obj1 == null) {
                    return 1;
                }

                if (obj2 == null) {
                    return -1;
                }

                final String firstOnRadarMenu = NATL_MOSAIC_DISPLAY_NAME;
                String label1 = ((ResourceDefinition) obj1)
                        .getResourceDefnName();
                if (label1.equals(firstOnRadarMenu)) {
                    return -1;
                }

                String label2 = ((ResourceDefinition) obj2)
                        .getResourceDefnName();
                if (label2.equals(firstOnRadarMenu)) {
                    return 1;
                }

                String displayName1 = LocalRadarStationManager.getInstance()
                        .getDisplayedName(label1);
                if (displayName1 != null) {
                    label1 = displayName1;
                }

                String displayName2 = LocalRadarStationManager.getInstance()
                        .getDisplayedName(label2);
                if (displayName2 != null) {
                    label2 = displayName2;
                }

                return (label1.compareTo(label2));

            }
        });

        rscGroupLViewer.setContentProvider(new IStructuredContentProvider() {
            @Override
            public Object[] getElements(Object inputElement) {
                String rscType = selectedRscName.getRscType();

                if (!rscType.isEmpty()) {
                    // if this resource uses attrSetGroups then get get the list
                    // of groups.
                    // (PGEN uses groups but we will list the subTypes
                    // (products) and not the single PGEN attr set group)
                    if (rscDefnsMngr.doesResourceUseAttrSetGroups(rscType)
                            && !selectedRscName.isPgenResource()) {

                        List<String> rscAttrSetsList = rscDefnsMngr
                                .getAttrSetGroupNamesForResource(rscType);

                        if ((rscAttrSetsList != null)
                                && !rscAttrSetsList.isEmpty()) {
                            return rscAttrSetsList.toArray();
                        }
                    } else {
                        try {
                            String[] rscGroups = rscDefnsMngr
                                    .getResourceSubTypes(rscType);

                            if ((rscGroups != null)
                                    && (rscGroups.length != 0)) {
                                return rscGroups;
                            }
                        } catch (VizException e) {
                            MessageDialog errDlg = new MessageDialog(
                                    NcDisplayMngr.getCaveShell(), "Error", null,
                                    "Error getting sub-types\n"
                                            + e.getMessage(),
                                    MessageDialog.ERROR, new String[] { OK },
                                    0);
                            errDlg.open();
                        }
                    }
                }
                return new String[] {};
            }

            @Override
            public void dispose() {
            }

            @Override
            public void inputChanged(Viewer viewer, Object oldInput,
                    Object newInput) {
            }
        });

        rscGroupLViewer.setComparator(new ViewerComparator() {
            @Override
            public int compare(Viewer viewer, Object obj1, Object obj2) {

                return super.compare(viewer, obj1, obj2);
            }
        });

        rscGroupLViewer.setLabelProvider(new LabelProvider() {

            @Override
            public String getText(Object element) {

                String displayName = (String) element;

                ResourceName rscName = new ResourceName(selectedRscName);

                ResourceDefinition rscDefn = rscDefnsMngr
                        .getResourceDefinition(rscName.getRscType());

                // replace with alias
                displayName = rscDefn.getRscGroupDisplayName(displayName);

                return displayName;

            }

        });

        rscAttrSetLViewer.setContentProvider(new IStructuredContentProvider() {
            @Override
            public Object[] getElements(Object inputElement) {

                if (!selectedRscName.getRscType().isEmpty()) {
                    List<AttributeSet> attrSets = rscDefnsMngr
                            .getAttrSetsForResource(selectedRscName, true);

                    maxLengthOfSelectableAttrSets = 0;

                    for (AttributeSet attributeSet : attrSets) {
                        if ((attributeSet != null) && (attributeSet.getName()
                                .length() > maxLengthOfSelectableAttrSets)) {
                            maxLengthOfSelectableAttrSets = attributeSet
                                    .getName().length();
                        }
                    }
                    return attrSets.toArray(new AttributeSet[0]);
                }
                return new String[] {};
            }

            @Override
            public void dispose() {
            }

            @Override
            public void inputChanged(Viewer viewer, Object oldInput,
                    Object newInput) {
            }
        });

        rscAttrSetLViewer.setComparator(new ViewerComparator() {
            @Override
            public int compare(Viewer viewer, Object obj1, Object obj2) {
                AttributeSet attr1 = (AttributeSet) obj1;
                AttributeSet attr2 = (AttributeSet) obj2;

                String label1 = attr1.getName();
                if (label1.equals(DEFAULT) || label1.equals(STANDARD)) {
                    return -1;
                }

                String label2 = attr2.getName();
                if (label2.equals(DEFAULT) || label2.equals(STANDARD)) {
                    return 1;
                }

                // Display aliases for Grid resource attributes.
                boolean isGridAttributeSet = attr1.getApplicableResource()
                        .equals(GRID_DATA)
                        || attr2.getApplicableResource().equals(GRID_DATA);

                if (isGridAttributeSet) {

                    String type = selectedRscName.getRscType();
                    String group = selectedRscName.getRscGroup();
                    String alias1 = AttrSetLabelsManager.getInstance()
                            .getAlias(type, group, label1);
                    if (alias1 != null) {
                        label1 = alias1;
                    }

                    String alias2 = AttrSetLabelsManager.getInstance()
                            .getAlias(type, group, label2);
                    if (alias2 != null) {
                        label2 = alias2;
                    }
                }

                // super calls getText which can trigger a bunch of
                // inventory queries in some cases
                return (label1.compareTo(label2));
            }
        });

        rscAttrSetLViewer.setLabelProvider(new LabelProvider() {
            @Override
            public String getText(Object element) {

                ResourceName rscName = new ResourceName(selectedRscName);
                ResourceDefinition rscDefn = rscDefnsMngr
                        .getResourceDefinition(rscName.getRscType());

                if (rscDefn == null) {
                    return "";
                }

                AttributeSet attrSet = (AttributeSet) element;
                rscDefn.setAttributeSet(attrSet);

                String originalAttrSetName = attrSet.getName();
                rscName.setRscAttrSetName(originalAttrSetName);

                // replace with alias
                String attrSetName = rscDefn
                        .getRscAttributeDisplayName(originalAttrSetName);

                if (attrSetName.endsWith(".attr")) {
                    attrSetName = attrSetName.substring(0,
                            attrSetName.length() - 5);
                }

                // Display aliases for Grid resource attributes.
                if (rscDefn.getResourceCategory()
                        .equals(ResourceCategory.GridRscCategory)) {

                    String type = selectedRscName.getRscType();
                    String group = selectedRscName.getRscGroup();
                    String alias = AttrSetLabelsManager.getInstance()
                            .getAlias(type, group, attrSetName);

                    if (alias != null) {
                        attrSetName = alias;
                    }
                }

                if (!showLatestTimes || rscDefn.isForecast()) {
                    return attrSetName;
                }

                while (attrSetName.length() < maxLengthOfSelectableAttrSets) {
                    attrSetName = attrSetName + " ";
                }

                try {
                    DataTime latestTime = rscDefn.getLatestDataTime(rscName);

                    if ((latestTime == null) || latestTime.isNull()) {
                        attrSetName = attrSetName + " (No Data)";
                    } else {
                        String latestTimeStr = NmapCommon
                                .getTimeStringFromDataTime(latestTime, "_");

                        attrSetName = attrSetName + " (" + latestTimeStr + ")";
                    }
                } catch (VizException e) {
                    statusHandler.handle(Priority.INFO, e.getMessage());
                }

                return attrSetName;
            }
        });
    }

    /*
     * add all of the listeners for widgets on this dialog
     */
    private void addSelectionListeners() {

        rscCatLViewer
                .addSelectionChangedListener(new ISelectionChangedListener() {
                    @Override
                    public void selectionChanged(SelectionChangedEvent event) {
                        StructuredSelection selectedElements = (StructuredSelection) event
                                .getSelection();
                        ResourceCategory selectedCat = (ResourceCategory) selectedElements
                                .getFirstElement();

                        if ((selectedCat != null)
                                && (selectedCat.equals(prevSelectedCat))) {
                            return;
                        }

                        selectedRscName = new ResourceName();
                        selectedRscName.setRscCategory(selectedCat);
                        prevSelectedCat = selectedCat;

                        if (prevCat2SelectedRscName.containsKey(selectedCat)) {
                            selectedRscName = prevCat2SelectedRscName
                                    .get(selectedCat);
                        }

                        updateResourceTypes();
                    }
                });

        rscTypeLViewer
                .addSelectionChangedListener(new ISelectionChangedListener() {
                    @Override
                    public void selectionChanged(SelectionChangedEvent event) {
                        StructuredSelection selectedElement = (StructuredSelection) event
                                .getSelection();
                        String selectedType = ((ResourceDefinition) selectedElement
                                .getFirstElement()).getResourceDefnName();

                        String prevSelectedType = selectedRscName.getRscType();
                        if ((selectedType != null)
                                && (selectedType.equals(prevSelectedType))) {
                            return;
                        }

                        selectedRscName.setRscType(selectedType);
                        selectedRscName.setRscGroup("");
                        selectedRscName.setRscAttrSetName("");
                        selectedRscName.setCycleTime(null);

                        updateResourceGroups();
                    }
                });

        rscGroupLViewer
                .addSelectionChangedListener(new ISelectionChangedListener() {
                    @Override
                    public void selectionChanged(SelectionChangedEvent event) {
                        StructuredSelection selectedElement = (StructuredSelection) event
                                .getSelection();
                        String selectedGroup = (String) selectedElement
                                .getFirstElement();

                        String prevSelectedGroup = selectedRscName
                                .getRscGroup();
                        if ((selectedGroup != null)
                                && (selectedGroup.equals(prevSelectedGroup))) {
                            return;
                        }

                        selectedRscName.setRscGroup(selectedGroup);
                        selectedRscName.setRscAttrSetName("");

                        updateResourceAttrSets();
                    }
                });

        rscAttrSetLViewer
                .addSelectionChangedListener(new ISelectionChangedListener() {

                    @Override
                    public void selectionChanged(SelectionChangedEvent event) {
                        StructuredSelection selectedElement = (StructuredSelection) event
                                .getSelection();
                        String selectedAttrSetName = ((AttributeSet) selectedElement
                                .getFirstElement()).getName();

                        String prevSelectedAttrName = selectedRscName
                                .getRscAttrSetName();

                        if ((selectedAttrSetName != null)
                                && (selectedAttrSetName
                                        .equals(prevSelectedAttrName))) {
                            return;
                        }

                        selectedRscName.setRscAttrSetName(selectedAttrSetName);

                        updateCycleTimes();
                        updateSelectedResource();
                    }
                });

        addResourceBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                prevSelectedRscName = selectedRscName;
                selectResource(true, true);
            }
        });

        // a double click will add the resource and close the dialog
        rscAttrSetLViewer.getList().addListener(SWT.MouseDoubleClick,
                new Listener() {
                    @Override
                    public void handleEvent(Event event) {
                        if (addResourceBtn.isVisible()) {
                            selectResource(false, true);
                        } else {
                            selectResource(true, true);
                        }
                    }
                });

        cycleTimeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                updateSelectedResource();
            }
        });
    }

    /**
     * Set the initial values of the widgets
     * 
     * @param initRscName
     *            the selected ResourceName in the parent resource list
     */
    protected void initWidgets(ResourceName initRscName) {

        if ((initRscName != null) && initRscName.isValid()) {
            // We do have a selected resource in the create RBD resource list
            openingDialogWithResources = true;

            // Use the selected resource from the create RBD resource list
            selectedRscName = new ResourceName(initRscName);

        } else if ((prevSelectedRscName != null)
                && prevSelectedRscName.isValid()) {

            // We have resources (but none are selected) in the create RBD
            // resource list
            openingDialogWithResources = true;

            // Use the previous ResourceName
            selectedRscName = new ResourceName(prevSelectedRscName);

        }

        if (selectedRscName != null) {
            prevSelectedCat = selectedRscName.getRscCategory();
        }
        rscCatLViewer.setInput(rscDefnsMngr);
        rscCatLViewer.refresh();
        rscCatLViewer.getList().deselectAll();

        if ((selectedRscName == null) || (selectedRscName
                .getRscCategory() == ResourceCategory.NullCategory)) {
            return;
        }

        for (int i = 0; i < rscCatLViewer.getList().getItemCount(); i++) {

            if (rscCatLViewer.getList().getItem(i)
                    .equals(selectedRscName.getRscCategory().toString())) {

                rscCatLViewer.getList().select(i);
                break;
            }
        }

        if (rscCatLViewer.getList().getSelectionCount() == 0) {
            selectedRscName = new ResourceName();
        }

        updateResourceTypes();

        // We are finished with work of opening this dialog.
        // Now we must set this flag to false, so other functionalities that
        // happen in the program will know this.
        openingDialogWithResources = false;
    }


    /*
     * refresh the types list based on the type in the seldResourceName use
     * seldResourceName to select the type
     */
    protected void updateResourceTypes() {

        rscTypeLViewer.setInput(rscDefnsMngr);
        rscTypeLViewer.refresh();

        org.eclipse.swt.widgets.List typeList = rscTypeLViewer.getList();
        typeList.deselectAll();

        String selectedRscType = selectedRscName.getRscType();

        if (!selectedRscType.isEmpty()) {
            String selectedDisplayType = selectedRscType;

            if (selectedRscName.getRscCategory().getCategoryName()
                    .equals(RADAR_CATEGORY)
                    && selectedRscType.startsWith(LOCAL_RADAR_PREFIX)) {

                String alias = LocalRadarStationManager.getInstance()
                        .getDisplayedName(selectedRscType);

                if ((alias != null) && !alias.isEmpty()) {
                    selectedDisplayType = alias;
                }
            }

            for (int i = 0; i < typeList.getItemCount(); i++) {
                String displayType = typeList.getItem(i);

                if (selectedDisplayType.equals(displayType)) {
                    typeList.select(i);
                    break;
                }
            }

            if (typeList.getSelectionCount() == 0) {
                selectedRscName.setRscType("");
                selectedRscName.setRscGroup("");
                selectedRscName.setRscAttrSetName("");
                selectedRscName.setCycleTime(null);
            }
        }

        // if no type is selected or it is not found for some reason, select the
        // first
        if (selectedRscName.getRscType().isEmpty()
                && (rscTypeLViewer.getList().getItemCount() > 0)) {

            typeList.select(0);
            StructuredSelection selectedElement = (StructuredSelection) rscTypeLViewer
                    .getSelection();
            String rscType = ((ResourceDefinition) selectedElement
                    .getFirstElement()).getResourceDefnName();
            selectedRscName.setRscType(rscType);
            selectedRscName.setRscGroup("");
            selectedRscName.setRscAttrSetName("");
            selectedRscName.setCycleTime(null);
        }

        updateResourceGroups();
    }

    protected void updateResourceGroups() {

        rscGroupLViewer.setInput(rscDefnsMngr);
        rscGroupLViewer.refresh();

        // If a group has been selected, then select it in the list.
        // Otherwise, select the first in the list and update the
        // seldResourceName.
        if (rscGroupLViewer.getList().getItemCount() == 0) {
            if (!selectedRscName.getRscGroup().isEmpty()) {
                selectedRscName.setRscGroup("");
                selectedRscName.setRscAttrSetName("");
                selectedRscName.setCycleTime(null);
            }
        } else {
            rscGroupLViewer.getList().deselectAll();

            if (!selectedRscName.getRscGroup().isEmpty()) {
                for (int i = 0; i < rscGroupLViewer.getList()
                        .getItemCount(); i++) {

                    if (rscGroupLViewer.getList().getItem(i)
                            .equals(selectedRscName.getRscGroup())) {
                        rscGroupLViewer.getList().select(i);
                        break;
                    }
                }

                if (rscGroupLViewer.getList().getSelectionCount() == 0) {
                    selectedRscName.setRscGroup("");
                    selectedRscName.setRscAttrSetName("");
                }
            }

            // if no type is selected or it is not found for some reason, select
            // the first
            if (selectedRscName.getRscGroup().isEmpty()
                    && (rscGroupLViewer.getList().getItemCount() > 0)) {

                rscGroupLViewer.getList().select(0);
                StructuredSelection selectedElement = (StructuredSelection) rscGroupLViewer
                        .getSelection();

                selectedRscName.setRscGroup(
                        (String) selectedElement.getFirstElement());
                selectedRscName.setRscAttrSetName("");
            }
        }
        updateResourceAttrSets();
    }

    private void updateResourceAttrSets() {
        rscAttrSetLViewer.setInput(rscDefnsMngr);
        rscAttrSetLViewer.getList().deselectAll();

        String prevSelectedAttrSetName = selectedRscName.getRscAttrSetName();
        if (!prevSelectedAttrSetName.isEmpty()) {

            for (int i = 0; i < rscAttrSetLViewer.getList()
                    .getItemCount(); i++) {

                String attrSetName = ((AttributeSet) rscAttrSetLViewer
                        .getElementAt(i)).getName();

                if (attrSetName.equals(prevSelectedAttrSetName)) {
                    rscAttrSetLViewer.getList().select(i);
                    break;
                }
            }

            if (rscAttrSetLViewer.getList().getSelectionCount() == 0) {
                selectedRscName.setRscAttrSetName("");
            }
        }

        // if no attr set is selected or it is not found for some reason, select
        // the first
        if (selectedRscName.getRscAttrSetName().isEmpty()
                && (rscAttrSetLViewer.getList().getItemCount() > 0)) {

            rscAttrSetLViewer.getList().select(0);
            StructuredSelection selectedElement = (StructuredSelection) rscAttrSetLViewer
                    .getSelection();

            selectedRscName.setRscAttrSetName(
                    ((AttributeSet) selectedElement.getFirstElement())
                            .getName());
        }

        updateCycleTimes();
        updateSelectedResource();
    }

    /**
     * Convenience method to avoid adding isPgenResource() to existing
     * conditional expressions.
     * 
     * @param rscDefn
     *            the definition of the currently selected resoure name
     * 
     * @return true if our intent is to display a cycle times comboBox in the
     *         resource selection manager UI
     */
    private boolean usingCycleTimes(ResourceDefinition rscDefn) {
        return isForecast() || rscDefn.isPgenResource();
    }

    /*
     * Keeps the selected resource updated
     */
    public void updateSelectedResource() {

        String availMsg = "Data Not Available";

        // enable/disable the Add Resource Button
        // and set the name of the Resource
        boolean enableSelections = true;

        ResourceDefinition rscDefn = rscDefnsMngr
                .getResourceDefinition(selectedRscName.getRscType());

        if (!selectedRscName.isValid() || (rscDefn == null)) {
            enableSelections = false;
        }

        if (enableSelections) {
            try {
                if (usingCycleTimes(rscDefn)) {
                    if (cycleTimeCombo.getItems().length == 0) {
                        enableSelections = false;
                    }
                } else if (!rscDefn.isRequestable()) {
                    availMsg = "";
                } else {
                    DataTime latestTime = rscDefn
                            .getLatestDataTime(selectedRscName);

                    if ((latestTime == null) || latestTime.isNull()) {
                        enableSelections = false;
                    } else {
                        availMsg = "Latest Data: " + NmapCommon
                                .getTimeStringFromDataTime(latestTime, "/");
                    }
                }
            } catch (VizException e) {
                statusHandler.handle(Priority.INFO, e.getMessage());
                availMsg = "Error getting latest time.";
                enableSelections = false;
            }
        }

        if (enableSelections) {
            addResourceBtn.setEnabled(true);

            // combo box will now be enabled for PGEN Available times
            if (usingCycleTimes(rscDefn)) {
                cycleTimeCombo.setEnabled(true);
                cycleTimeCombo.setVisible(true);

                if (!openingDialogWithResources) {

                    String cycleTime = cycleTimeCombo.getText();
                    int index = cycleTimeCombo.getSelectionIndex();

                    String[] items = cycleTimeCombo.getItems();
                    DataTime refTime = null;
                    if (items.length > 1 && items[0].equals(items[1])) {
                        refTime = ((List<DataTime>) cycleTimeCombo
                                .getData(cycleTime)).get(index);
                    } else {
                        refTime = (DataTime) cycleTimeCombo.getData(cycleTime);
                    }

                    // set cycle time out to the millisecond
                    selectedRscName.setCycleTime(refTime);

                } else {

                    int i = getSelectedCycleTimeIndex(selectedRscName,
                            cycleTimeCombo);
                    cycleTimeCombo.select(i);
                }

                availDataTimeLbl.setVisible(false);
            } else {
                availDataTimeLbl.setVisible(true);
                availDataTimeLbl.setText(availMsg);
                cycleTimeCombo.setEnabled(false);
                cycleTimeCombo.setVisible(false);
            }

            // For now, don't let the user select 'Latest'
            if (selectedRscName.isLatestCycleTime()) {
                addResourceBtn.setEnabled(false);
            } else {
                // chop off seconds and milliseconds, leaving just hours and
                // minutes
                if (selectedRscName.getCycleTime() != null) {
                    String cycleTimeStr = selectedRscName.getCycleTimeString();
                }
            }
        } else {
            addResourceBtn.setEnabled(false);

            availDataTimeLbl.setVisible(true);
            availDataTimeLbl.setText(availMsg);

            cycleTimeCombo.setVisible(false);
        }

        prevDisplayType2RscName.put(displayType,
                new ResourceName(selectedRscName));
        prevCat2SelectedRscName.put(selectedRscName.getRscCategory(),
                new ResourceName(selectedRscName));
    }

    /*
     * code for the Listeners for the Add Resource button and the double Click
     * on the list. Get the selected rsc and add to the list, ignoring the cycle
     * time for now. This is true and makes the UI "clumsy"" and misleading
     */
    public void selectResource(boolean replaceRsc, boolean done) {

        if ((selectedRscName != null) && selectedRscName.isValid()) {

            for (IResourceSelectedListener lstnr : rscSelListeners) {
                lstnr.resourceSelected(selectedRscName, replaceRsc,
                        false, done);
            }
        }
    }

    public ResourceName getCurrentlySelectedResource() {
        return selectedRscName;
    }

    public void addResourceSelectionListener(IResourceSelectedListener lstnr) {
        rscSelListeners.add(lstnr);
    }

    /*
     * TODO: add a way to let the user specifically choose the "LATEST" cycle
     * time. Currently the user cannot select a forecast resource without
     * selecting an available cycle time.
     */
    protected void updateCycleTimes() {
        ResourceDefinition rscDefn = rscDefnsMngr
                .getResourceDefinition(selectedRscName);

        if (rscDefn == null) {
            cycleTimeCombo.setEnabled(false);
            clearCycleTimeCombo();
            return;
        }

        cycleTimeCombo.setEnabled(true);

        boolean cycleTimeEnabled = (rscDefn.isForecast()
                || rscDefn.isPgenResource());
        cycleTimeCombo.setVisible(cycleTimeEnabled);
        availDataTimeLbl.setVisible(!cycleTimeEnabled);

        if (!isForecast() && !rscDefn.isPgenResource()) {
            selectedRscName.setCycleTime(null);
            clearCycleTimeCombo();
            return;
        }

        try {
            startWaitCursor();
            List<DataTime> availableTimes = null;

            // If the timeline is generated using frame intervals from a given
            // reference/cycle time, then get a list of selectable ref times.
            // Ideally this would also specify a way to generate the ref times
            // but its really just for nctaf right now so just do it like taf
            // needs.
            if (rscDefn
                    .getTimelineGenMethod() == TimelineGenMethod.USE_FCST_FRAME_INTERVAL_FROM_REF_TIME) {
                availableTimes = rscDefn.getNormalizedDataTimes(selectedRscName,
                        24 * 60);
            } else if (rscDefn
                    .getTimelineGenMethod() == TimelineGenMethod.DETERMINE_FROM_RSC_IMPLEMENTATION) {
                int startTime = Integer.parseInt(rscDefn.getCycleReference());
                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(GMT));
                cal.setTime(new Date());

                DayReference day = (rscDefn.getDayReference() != null
                        ? rscDefn.getDayReference() : DayReference.TODAY);
                switch (day) {
                case TOMORROW:
                    cal.add(Calendar.DAY_OF_MONTH, 1);
                    break;
                case TODAY:
                    break;
                case YESTERDAY:
                    cal.add(Calendar.DAY_OF_MONTH, -1);
                    break;
                default:
                    cal = null;
                }
                if (cal != null) {
                    cal.set(Calendar.HOUR_OF_DAY, startTime);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    availableTimes = new ArrayList<>();
                    availableTimes.add(new DataTime(cal));
                }
            } else {

                // create the List of available times from the ResourceName
                availableTimes = rscDefn.getDataTimes(selectedRscName);

            }

            /* Use map to handle dupElim */
            Map<String, DataTime> newTimes = new HashMap<>(
                    availableTimes.size(), 1);

            for (DataTime aTime : availableTimes) {
                DataTime refTime = new DataTime(aTime.getRefTime());
                String cycleTimeMillis = NmapCommon
                        .getTimeStringToMillisFromDataTime(refTime, "_");
                newTimes.put(cycleTimeMillis, refTime);
            }

            /* Determine if any of the times have changed */
            List<String> previousTimes = Arrays
                    .asList(cycleTimeCombo.getItems());
            Set<String> timesToRemove = new HashSet<>(previousTimes);
            timesToRemove.removeAll(newTimes.keySet());
            newTimes.keySet().removeAll(previousTimes);

            if (!timesToRemove.isEmpty() || !newTimes.isEmpty()) {
                List<String> currentTimes = new ArrayList<>(previousTimes);
                currentTimes.removeAll(timesToRemove);
                for (String time : timesToRemove) {
                    cycleTimeCombo.setData(time, null);
                }

                for (String t : newTimes.keySet()) {
                    currentTimes.add(t.substring(0, 11));
                }

                List<DataTime> refTimes = new ArrayList<>();
                String timeKey = null;

                List<String> timeKeys = new ArrayList<>();
                for (Map.Entry<String, DataTime> entry : newTimes.entrySet()) {
                    timeKeys.add(entry.getKey().substring(0, 11));
                }

                boolean duplicateKeys = false;
                /*
                 * If time keys are the same, we are dealing with cycle times
                 * that only differ to the second or millisecond. In this case,
                 * store the differing cycle times as a list for the key.
                 */
                if (timeKeys.size() > 1
                        && timeKeys.get(0).equals(timeKeys.get(1))) {
                    duplicateKeys = true;
                }

                for (Map.Entry<String, DataTime> entry : newTimes.entrySet()) {
                    timeKey = entry.getKey().substring(0, 11);
                    if (duplicateKeys) {
                        refTimes.add(entry.getValue());
                    }
                    // else, store each cycle time with a unique key
                    else {
                        cycleTimeCombo.setData(timeKey, entry.getValue());
                    }

                }
                if (refTimes.size() > 0) {
                    cycleTimeCombo.setData(timeKey, refTimes);
                }

                if (currentTimes.isEmpty()) {
                    cycleTimeCombo.removeAll();
                    cycleTimeCombo.setVisible(false);
                    availDataTimeLbl.setVisible(true);
                    availDataTimeLbl.setText("No Data Available");
                } else {
                    Collections.sort(currentTimes, Collections.reverseOrder());
                    cycleTimeCombo.setItems(currentTimes
                            .toArray(new String[currentTimes.size()]));
                    cycleTimeCombo.select(0);
                }
            } else if (cycleTimeCombo.getItemCount() > 0) {
                cycleTimeCombo.select(0);
            }
        } catch (

        VizException e)

        {
            MessageDialog errDlg = new MessageDialog(
                    NcDisplayMngr.getCaveShell(), "Error", null,
                    "Error Requesting Cycle Times:" + e.getMessage(),
                    MessageDialog.ERROR, new String[] { OK }, 0);
            errDlg.open();
            return;
        } finally

        {
            stopWaitCursor();
        }

        return;

    }

    public ResourceName getPrevSelectedResource() {
        return prevSelectedRscName;
    }

    private boolean isForecast() {
        boolean gridAnalysis = false;
        StructuredSelection is = (StructuredSelection) rscAttrSetLViewer
                .getSelection();
        AttributeSet attr = (AttributeSet) is.getFirstElement();
        if (attr != null) {
            String gdattim = attr.getAttributes().get("GDATTIM");
            gridAnalysis = (gdattim != null) && !gdattim.isEmpty()
                    && (gdattim.toUpperCase().contains(
                            "ALLF")
                    || (gdattim.toUpperCase().contains("FIRSTF")
                            && gdattim.toUpperCase().contains("LASTF")));
        }

        ResourceDefinition rscDefn = rscDefnsMngr
                .getResourceDefinition(selectedRscName.getRscType());

        return rscDefn.isForecast() && !gridAnalysis;
    }

    protected void clearCycleTimeCombo() {
        String[] cycleTimeArray = cycleTimeCombo.getItems();
        for (String cycleTime : cycleTimeArray) {
            cycleTimeCombo.setData(cycleTime, null);
        }
        cycleTimeCombo.removeAll();
    }

    /**
     * Clean up data that are not cleaned up by the Garbage Collector.
     */
    public void cleanup() {
        clearCycleTimeCombo();
    }

    protected void startWaitCursor() {
        if (waitCursor != null) {
            setCursor(waitCursor);
        }
    }

    protected void stopWaitCursor() {
        setCursor(null);
    }

    /**
     * 
     * Get the cycle time from a ResourceName and look for a match in the
     * cycleTimeCombo widget
     * 
     * @param selectedRscName
     *            the selected ResourceName
     * @param cycleTimeCombo
     *            the Combo widget that shows Cycle Times
     * @return the index of the cycleTimeCombo that matches the cycle time
     *         stored in the ResourceName
     */
    public int getSelectedCycleTimeIndex(ResourceName selectedRscName,
            Combo cycleTimeCombo) {

        if (selectedRscName.getCycleTime() != null) {
            String cycleTimeString = selectedRscName.getCycleTimeString();
            String[] items = cycleTimeCombo.getItems();
            int totalItems = items.length;
            int i;

            for (i = 0; i < totalItems; i++) {

                if (items[i].equals(cycleTimeString)) {
                    break;
                }
            }
            return i;
        }
        return 0;
    }

    @Override
    public void dispose() {
        super.dispose();

        if (waitCursor != null) {
            waitCursor.dispose();
            waitCursor = null;
        }
    }

}
