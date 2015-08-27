package gov.noaa.nws.ncep.viz.gridManager.ui.loadGrid;

import static java.lang.System.out;
import gov.noaa.nws.ncep.viz.common.display.NcDisplayType;
import gov.noaa.nws.ncep.viz.common.preferences.NcepGeneralPreferencesPage;
import gov.noaa.nws.ncep.viz.common.ui.NmapCommon;
import gov.noaa.nws.ncep.viz.gempak.util.GempakGrid;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData.TimelineGenMethod;
import gov.noaa.nws.ncep.viz.resources.manager.AttributeSet;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceCategory;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceDefinition;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceDefnsMngr;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceName;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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

import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.exception.VizException;

/**
 * Data Selection dialog.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date       	Ticket#		Engineer	Description
 * ------------	----------	-----------	--------------------------
 * 01/26/10 	 #226	     Greg Hull	 Broke out from RscBndlDefnDialog
 * 04/05/10      #226        Greg Hull   Add back PGEN selection
 * 06/18/10      #273        Greg Hull   Rework for new ResourceCnfgMngr
 * 09/13/10      #307        Greg Hull   implement cycle times.
 * 09/28/10      #307        Greg Hull   save the fcst/observed mode when re-initing
 * 10/01/10      #298        B. Hebbard  handle MOS resources in updateCycleTimes()
 * 10/20/10                  X. Guo      Rename getCycleTimeStringFromDataTime to getTimeStringFromDataTime
 * 10/20/10		 #277		 M. Li		 get model name for ensemble
 * 11/18/10	      277		 M. Li		 get correct cycle for ensemble
 * 11/29/10				  	mgamazaychikov	Changed updateCycleTime method for GEMPAK data source
 * 02/28/11      #408        Greg Hull   Replace Forecast/Observed with Filter combo
 * 04/18/11                  Greg Hull   caller sets name of the 'select' button
 * 06/07/11       #445       Xilin Guo   Data Manager Performance Improvements
 * 09/20/2011				mgamazaychikov	Made changes associated with removal of DatatypeTable class
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
 * 07/23/2014		?		 B. Yin		 Handle grid analysis 
 * 07/23/2014       ?        B. Hebbard  Make extensible for NTRANS-specific subclass
 * 
 * </pre>
 * 
 * @author ghull
 * @version 1
 */
public class GridSelectionControl extends Composite {

    protected ResourceDefnsMngr rscDefnsMngr;

    protected ResourceName seldResourceName = null;

    protected String seldFilterStr = "";

    private static ResourceCategory prevSeldCat = ResourceCategory.NullCategory;

    // a map to store the previous selections for each category.
    protected static HashMap<ResourceCategory, ResourceName> prevCatSeldRscNames;

    // this list must stay in sync with the cycleTimeCombo.
    protected ArrayList<DataTime> cycleTimes = new ArrayList<DataTime>();

    protected Composite sel_rsc_comp = null;

    protected Label seldRscNameTxt = null;

    protected Label availDataTimeLbl = null;

    protected Combo cycleTimeCombo = null;

    // For now only one of following two will be visible but we may want to
    // allow both later (and remove the Modify button from the Create RBD tab)

    protected Button addResourceBtn = null;

    protected Button replaceResourceBtn = null;

    protected Boolean replaceBtnVisible;

    protected Boolean replaceBtnEnabled;

    //protected Button addToAllPanesBtn = null;

    protected Label rscTypeLbl = null;

    private Label rscTypeGroupLbl = null;

    //protected ListViewer rscCatLViewer = null;

    protected ListViewer rscTypeLViewer = null;

    private ListViewer rscGroupLViewer = null;

    protected ListViewer rscAttrSetLViewer = null;

    protected final static int rscListViewerHeight = 210;

    protected static Rectangle prevShellBounds = new Rectangle(0, 0, 800, 460);

    protected Boolean showLatestTimes = false;

    protected Boolean onlyShowResourcesWithData = false;

    protected Integer maxLengthOfSelectableAttrSets = 0; // used in justifying
                                                         // the
                                                         // times in the
                                                         // attrSetsList

    public interface IResourceSelectedListener {
        public void resourceSelected(ResourceName rscName, boolean replace,
                boolean addAllPanes, boolean done);
    }

    private Set<IResourceSelectedListener> rscSelListeners = new HashSet<IResourceSelectedListener>();

    protected NcDisplayType seldDisplayType;

    public GridSelectionControl(Composite parent, Boolean replaceVisible,
            Boolean replaceEnabled, ResourceName initRscName,
            Boolean multiPane, NcDisplayType dispType) throws VizException {
        super(parent, SWT.SHADOW_NONE);

        seldDisplayType = dispType;

        showLatestTimes = NmapCommon.getNcepPreferenceStore().getBoolean(
                NcepGeneralPreferencesPage.ShowLatestResourceTimes);
        onlyShowResourcesWithData = false; // NmapCommon.getNcepPreferenceStore().getBoolean(
                                           // NcepGeneralPreferencesPage.OnlyShowResourcesWithData
                                           // );

        rscDefnsMngr = ResourceDefnsMngr.getInstance();

        replaceBtnVisible = replaceVisible;
        replaceBtnEnabled = replaceEnabled;

        if (prevCatSeldRscNames == null) {
            prevCatSeldRscNames = new HashMap<ResourceCategory, ResourceName>();
        }

        sel_rsc_comp = this;

        GridData gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        gd.widthHint = prevShellBounds.width;
        gd.heightHint = prevShellBounds.height;
        sel_rsc_comp.setLayoutData(gd);

        sel_rsc_comp.setLayout(new FormLayout());

        sel_rsc_comp.addListener(SWT.Resize, new Listener() {
            @Override
            public void handleEvent(Event event) {
                prevShellBounds = sel_rsc_comp.getBounds();
            }
        });

        createSelectResourceGroup(multiPane);
        // set up the content providers for the ListViewers
        setContentProviders();
        addSelectionListeners();
        initWidgets(initRscName);
    }
    
    public GridSelectionControl(Composite parent, Boolean replaceVisible,
            Boolean replaceEnabled, ResourceName initRscName,
            Boolean multiPane ) throws VizException {
        super(parent, SWT.SHADOW_NONE);

        seldDisplayType = NcDisplayType.NMAP_DISPLAY;

        showLatestTimes = NmapCommon.getNcepPreferenceStore().getBoolean(
                NcepGeneralPreferencesPage.ShowLatestResourceTimes);
        onlyShowResourcesWithData = false; // NmapCommon.getNcepPreferenceStore().getBoolean(
                                           // NcepGeneralPreferencesPage.OnlyShowResourcesWithData
                                           // );

        rscDefnsMngr = ResourceDefnsMngr.getInstance();

        replaceBtnVisible = replaceVisible;
        replaceBtnEnabled = replaceEnabled;

        if (prevCatSeldRscNames == null) {
            prevCatSeldRscNames = new HashMap<ResourceCategory, ResourceName>();
        }

        sel_rsc_comp = parent;

        GridData gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        gd.widthHint = prevShellBounds.width;
        gd.heightHint = prevShellBounds.height;
        sel_rsc_comp.setLayoutData(gd);

        sel_rsc_comp.setLayout(new FormLayout());
        
        // first create the lists and then attach the label to the top of them
        rscTypeLViewer = new ListViewer(sel_rsc_comp, SWT.SINGLE | SWT.BORDER
                | SWT.V_SCROLL);
        FormData fd = new FormData();// 150, rscListViewerHeight);
        fd.top = new FormAttachment( 0, 35 );
    	fd.left = new FormAttachment( 0, 10 );
    	fd.bottom = new FormAttachment( 100, -70 );
    	fd.right = new FormAttachment( 25, -3 );
        rscTypeLViewer.getList().setLayoutData(fd);

        rscTypeLbl = new Label(sel_rsc_comp, SWT.NONE);
    	rscTypeLbl.setText("GridGridGridGrid");
        fd = new FormData();
        fd.left = new FormAttachment(rscTypeLViewer.getList(), 0, SWT.LEFT);
        fd.bottom = new FormAttachment(rscTypeLViewer.getList(), -2, SWT.TOP);
        rscTypeLbl.setLayoutData(fd);

        // first create the lists and then attach the label to the top of them
        rscGroupLViewer = new ListViewer(sel_rsc_comp, SWT.SINGLE | SWT.BORDER
                | SWT.V_SCROLL | SWT.H_SCROLL);
        fd = new FormData();// 150, rscListViewerHeight);
        fd = new FormData(); //150, rscListViewerHeight);
    	fd.top = new FormAttachment( rscTypeLViewer.getList(), 0, SWT.TOP );
    	fd.left = new FormAttachment( rscTypeLViewer.getList(), 5, SWT.RIGHT );//rscTypeLViewer.getList(), 10, SWT.RIGHT );
    	fd.bottom = new FormAttachment( rscTypeLViewer.getList(), 0, SWT.BOTTOM );
    	fd.right = new FormAttachment( 45, -3);
        rscGroupLViewer.getList().setLayoutData(fd);

        rscTypeGroupLbl = new Label(sel_rsc_comp, SWT.NONE);
        rscTypeGroupLbl.setText("Group");
        fd = new FormData();
    	fd.left = new FormAttachment( rscGroupLViewer.getList(), 0, SWT.LEFT );
    	fd.bottom = new FormAttachment( rscGroupLViewer.getList(), -5, SWT.TOP );
    	fd.right = new FormAttachment( rscGroupLViewer.getList(), 5, SWT.RIGHT );
        rscTypeGroupLbl.setLayoutData(fd);

        rscAttrSetLViewer = new ListViewer(sel_rsc_comp, SWT.SINGLE
                | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        fd = new FormData();
        fd.top = new FormAttachment(rscGroupLViewer.getList(), 0, SWT.TOP);
        fd.left = new FormAttachment(rscGroupLViewer.getList(), 8, SWT.RIGHT);
        fd.right = new FormAttachment(100, -10);
        fd.bottom = new FormAttachment(rscGroupLViewer.getList(), 0, SWT.BOTTOM);
        rscAttrSetLViewer.getList().setLayoutData(fd);

        Label rscAttrsLbl = new Label(sel_rsc_comp, SWT.NONE);
        rscAttrsLbl.setText("Attribute File");
        fd = new FormData();
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

       	seldRscNameTxt = new Label( sel_rsc_comp, SWT.NONE );
    	fd.top = new FormAttachment( rscGroupLViewer.getList(), 10, SWT.BOTTOM );
        fd.left = new FormAttachment(rscGroupLViewer.getList(), 0, SWT.LEFT);
        //fd.right = new FormAttachment(rscAttrSetLViewer.getList(), 50, SWT.RIGHT);
        seldRscNameTxt.setLayoutData(fd);

        addResourceBtn = new Button(sel_rsc_comp, SWT.None);
        fd = new FormData();
        fd.top  = new FormAttachment( rscAttrSetLViewer.getList(), 30, SWT.BOTTOM );
        fd.right = new FormAttachment( 100, -10 );
        addResourceBtn.setLayoutData(fd);
    	addResourceBtn.setText( "   Add   " ); // Add To RBD
        
        // allow the user to enter any previous datatime
        cycleTimeCombo = new Combo(sel_rsc_comp, SWT.READ_ONLY);
        fd = new FormData();
        fd.width = 194;
        fd.left = new FormAttachment( rscTypeLViewer.getList(), 0, SWT.LEFT  );
        fd.top  = new FormAttachment( rscTypeLViewer.getList(), 5, SWT.BOTTOM );
        cycleTimeCombo.setLayoutData(fd);


        
        // set up the content providers for the ListViewers
        setContentProviders();
        addSelectionListeners();
        initWidgets(initRscName);
        
        for (IResourceSelectedListener lstnr : rscSelListeners) {
            this.addResourceSelectionListener(lstnr);
        }
    }

    public GridSelectionControl(Composite parent) throws VizException {
        // TODO: dorky?? This is a pass-thru constructor, just so
        // NtransSelectionControl can call the grandparent constructor, but do
        // its own version of the useful work of the main constructor above.
        // See discussion at:
        // http://stackoverflow.com/questions/1878558/jump-over-parent-constructor-to-call-grandparents
        // Better solution might be to do a refactor "pull"
        // AbstractResourceSelectionControl superclass out of
        // GridSelectionControl
        // and have NtransSelectionControl extend the former. But that would
        // mess
        // with existing GridSelectionControl, which is working and tested
        // with
        // non-NTRANS resources. Maybe someday...?
        super(parent, SWT.SHADOW_NONE);
    }

    // create all the widgets in the Resource Selection (top) section of the
    // sashForm.
    //
    private void createSelectResourceGroup(Boolean multiPane) {

        // first create the lists and then attach the label to the top of them
        rscTypeLViewer = new ListViewer(sel_rsc_comp, SWT.SINGLE | SWT.BORDER
                | SWT.V_SCROLL | SWT.H_SCROLL);
        FormData fd = new FormData();// 150, rscListViewerHeight);
        fd.height = rscListViewerHeight;
    	fd.top = new FormAttachment( 0, 20 );
        fd.left = new FormAttachment(0, 10);
        fd.right = new FormAttachment( 15, 0 );
        rscTypeLViewer.getList().setLayoutData(fd);

        rscTypeLbl = new Label(sel_rsc_comp, SWT.NONE);
    	rscTypeLbl.setText("Source");
        fd = new FormData();
        fd.left = new FormAttachment(rscTypeLViewer.getList(), 0, SWT.LEFT);
        fd.bottom = new FormAttachment(rscTypeLViewer.getList(), -2, SWT.TOP);

        rscTypeLbl.setLayoutData(fd);



        // first create the lists and then attach the label to the top of them
        rscGroupLViewer = new ListViewer(sel_rsc_comp, SWT.SINGLE | SWT.BORDER
                | SWT.V_SCROLL | SWT.H_SCROLL);
        fd = new FormData();// 150, rscListViewerHeight);
        fd.height = rscListViewerHeight;
        fd.top = new FormAttachment(rscTypeLViewer.getList(), 0, SWT.TOP);
        fd.left = new FormAttachment(rscTypeLViewer.getList(), 8, SWT.RIGHT);
    	fd.width = 120;
        fd.bottom = new FormAttachment(rscTypeLViewer.getList(), 0, SWT.BOTTOM);
        rscGroupLViewer.getList().setLayoutData(fd);

        rscTypeGroupLbl = new Label(sel_rsc_comp, SWT.NONE);
        rscTypeGroupLbl.setText("Group");
        fd = new FormData();
        fd.left = new FormAttachment(rscGroupLViewer.getList(), 0, SWT.LEFT);
        fd.bottom = new FormAttachment(rscGroupLViewer.getList(), -2, SWT.TOP);
        rscTypeGroupLbl.setLayoutData(fd);

        rscAttrSetLViewer = new ListViewer(sel_rsc_comp, SWT.SINGLE
                | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        fd = new FormData();// 260, rscListViewerHeight);
        fd.height = rscListViewerHeight;
        fd.top = new FormAttachment(rscGroupLViewer.getList(), 0, SWT.TOP);
        fd.left = new FormAttachment(rscGroupLViewer.getList(), 8, SWT.RIGHT);
        fd.right = new FormAttachment(100, -10);
        fd.bottom = new FormAttachment(rscGroupLViewer.getList(), 0, SWT.BOTTOM);
        rscAttrSetLViewer.getList().setLayoutData(fd);

        Label rscAttrsLbl = new Label(sel_rsc_comp, SWT.NONE);
        rscAttrsLbl.setText("Attributes");
        fd = new FormData();
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
    	
       	seldRscNameTxt = new Label( sel_rsc_comp, SWT.None );
    	fd = new FormData();
    	fd.top = new FormAttachment( rscAttrSetLViewer.getList(), 10, SWT.BOTTOM );
        fd.left = new FormAttachment(rscAttrSetLViewer.getList(), 0, SWT.LEFT);
        seldRscNameTxt.setLayoutData(fd);

        addResourceBtn = new Button(sel_rsc_comp, SWT.None);

        fd = new FormData();
        fd.top  = new FormAttachment( seldRscNameTxt, 0, SWT.TOP );
        fd.right = new FormAttachment( 100, -10 );
        addResourceBtn.setLayoutData(fd);
    	addResourceBtn.setText( "   Add   " ); // Add To RBD

        // allow the user to enter any previous datatime
        cycleTimeCombo = new Combo(sel_rsc_comp, SWT.READ_ONLY);
        fd = new FormData();
        fd.width = 130;
        fd.top  = new FormAttachment( seldRscNameTxt, 0, SWT.TOP );
    	fd.right = new FormAttachment( 100, -20 );

        cycleTimeCombo.setLayoutData(fd);
    }

    private void setContentProviders() {
    	
        // order the Categories according to the

        rscTypeLViewer.setContentProvider(new IStructuredContentProvider() {
            @Override
            public Object[] getElements(Object inputElement) {
                // String rscCat = (String)inputElement;
                if (seldResourceName.getRscCategory() != ResourceCategory.NullCategory) {
                    try {
                        List<ResourceDefinition> rscTypes = rscDefnsMngr
                                .getResourceDefnsForCategory(
                                        seldResourceName.getRscCategory(),
                                        seldFilterStr, seldDisplayType, true, // include
                                                                              // generated
                                                                              // types
                                        false); // only include enabled types

                        return rscTypes.toArray();
                    } catch (VizException e) {
                        MessageDialog errDlg = new MessageDialog(NcDisplayMngr
                                .getCaveShell(), "Error", null,
                                "Error getting Resource Types\n"
                                        + e.getMessage(), MessageDialog.ERROR,
                                new String[] { "OK" }, 0);
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

            // TODO : implement this if we want to group definitions according
            // to
            // some meaningful category....
            public int category(Object element) {
                ResourceDefinition rd = (ResourceDefinition) element;
                return (rd.isForecast() ? 1 : 0);
                // return super.category(element);
            }

            @Override
            public int compare(Viewer viewer, Object e1, Object e2) {
                int catComp = category(e1) - category(e2);
                return (catComp != 0 ? catComp : rscDefnsMngr
                        .getDefaultRscDefnComparator().compare(
                                (ResourceDefinition) e1,
                                (ResourceDefinition) e2));
            }
        });

        rscTypeLViewer.setLabelProvider(new LabelProvider() {
            public String getText(Object element) {
                ResourceDefinition rd = (ResourceDefinition) element;
                return (rd == null ? "null" : rd.getResourceDefnName());
            }
        });

        rscGroupLViewer.setContentProvider(new IStructuredContentProvider() {
            @Override
            public Object[] getElements(Object inputElement) {
                String rscType = seldResourceName.getRscType();

                if (!rscType.isEmpty()) {
                    // if this resource uses attrSetGroups then get get the list
                    // of
                    // groups. (PGEN uses groups but we will list the subTypes
                    // (products)
                    // and not the single PGEN attr set group)
                    if (rscDefnsMngr.doesResourceUseAttrSetGroups(rscType)
                            && !seldResourceName.isPgenResource()) {

                        List<String> rscAttrSetsList = rscDefnsMngr
                                .getAttrSetGroupNamesForResource(rscType);

                        if (rscAttrSetsList != null
                                && !rscAttrSetsList.isEmpty()) {
                            return rscAttrSetsList.toArray();
                        }
                    } else {
                        try {
                            String[] rscGroups = rscDefnsMngr
                                    .getResourceSubTypes(rscType);

                            if (rscGroups != null && rscGroups.length != 0) {
                                return rscGroups;// .toArray();
                            }
                        } catch (VizException e) {
                            MessageDialog errDlg = new MessageDialog(
                                    NcDisplayMngr.getCaveShell(), "Error",
                                    null, "Error getting sub-types\n"
                                            + e.getMessage(),
                                    MessageDialog.ERROR, new String[] { "OK" },
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
            public int compare(Viewer viewer, Object e1, Object e2) {
                return super.compare(viewer, e1, e2);
            }
        });

        rscAttrSetLViewer.setContentProvider(new IStructuredContentProvider() {
            @Override
            public Object[] getElements(Object inputElement) {

                // if there is a group selected then
                // if( !seldResourceName.getRscGroup().isEmpty() ) {
                // if( rscDefnsMngr.getAttrSetsForResource( seldResourceName ))
                // }

                // if an attrSetGroup is selected, return the attrSets in the
                // group
                if (!seldResourceName.getRscType().isEmpty()) {
                    List<AttributeSet> attrSets = rscDefnsMngr
                            .getAttrSetsForResource(seldResourceName, true);

                    maxLengthOfSelectableAttrSets = 0;

                    for (AttributeSet as : attrSets) {
                        if (as != null
                                && as.getName().length() > maxLengthOfSelectableAttrSets) {
                            maxLengthOfSelectableAttrSets = as.getName()
                                    .length();
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
            public int compare(Viewer viewer, Object e1, Object e2) {
                AttributeSet a1 = (AttributeSet) e1;
                AttributeSet a2 = (AttributeSet) e2;

                if (a1.getName().equals("default")
                        || a1.getName().equals("standard")) {
                    return -1;
                } else if (a2.getName().equals("default")
                        || a2.getName().equals("standard")) {
                    return 1;
                } else {
                    // super calls getText which can trigger a bunch of
                    // inventory queries in some cases
                    return (a1.getName().compareTo(a2.getName())); // super.compare(viewer,
                                                                   // e1, e2);
                }
            }
        });

        rscAttrSetLViewer.setLabelProvider(new LabelProvider() {
            public String getText(Object element) {
                String attrSetName = ((AttributeSet) element).getName();

                if (attrSetName.endsWith(".attr")) {
                    attrSetName = attrSetName.substring(0,
                            attrSetName.length() - 5);
                }

                ResourceName rscName = new ResourceName(seldResourceName);
                rscName.setRscAttrSetName(attrSetName);

                ResourceDefinition rscDefn = rscDefnsMngr
                        .getResourceDefinition(rscName.getRscType());

                if (rscDefn == null) {
                    return "";
                }
                //
                if (!showLatestTimes ||
                // !onlyShowResourcesWithData ||
                        rscDefn.isForecast()) {
                    return attrSetName;
                }

                while (attrSetName.length() < maxLengthOfSelectableAttrSets) {
                    attrSetName = attrSetName + " ";
                }

                // If we aren't using the inventory then the query is too slow
                // for the gui.
                // TODO : If the inventory doesn't pan out then we could either
                // implement this in another thread and accept the delay or add
                // a
                // 'Check Availability' button.
                //
                if (rscName.isValid() && rscDefn.usesInventory()
                        && rscDefn.getInventoryEnabled()) {
                    try {
                        DataTime latestTime = rscDefn
                                .getLatestDataTime(rscName);

                        if (latestTime.isNull()) {
                            attrSetName = attrSetName + " (No Data)";
                        } else {
                            DataTime refTime = new DataTime(latestTime
                                    .getRefTime());
                            String latestTimeStr = NmapCommon
                                    .getTimeStringFromDataTime(latestTime, "_");

                            attrSetName = attrSetName + " (" + latestTimeStr
                                    + ")";
                        }
                    } catch (VizException vizex) {
                        out.println(vizex.getMessage());
                    }
                }
                return attrSetName;
            }
        });
    }

    // add all of the listeners for widgets on this dialog
    private void addSelectionListeners() {


        rscTypeLViewer
                .addSelectionChangedListener(new ISelectionChangedListener() {
                    public void selectionChanged(SelectionChangedEvent event) {
                        StructuredSelection seld_elem = (StructuredSelection) event
                                .getSelection();
                        String seld_rsc_type = ((ResourceDefinition) seld_elem
                                .getFirstElement()).getResourceDefnName();

                        seldResourceName.setRscType(seld_rsc_type);
                        seldResourceName.setRscGroup("");
                        seldResourceName.setRscAttrSetName("");
                        seldResourceName.setCycleTime(null);

                        // updateCycleTimes();

                        updateResourceGroups();
                    }
                });

        rscGroupLViewer
                .addSelectionChangedListener(new ISelectionChangedListener() {
                    public void selectionChanged(SelectionChangedEvent event) {
                        StructuredSelection seld_elem = (StructuredSelection) event
                                .getSelection();
                        seldResourceName.setRscGroup((String) seld_elem
                                .getFirstElement());
                        seldResourceName.setRscAttrSetName("");

                        updateResourceAttrSets();
                    }
                });

        rscAttrSetLViewer
                .addSelectionChangedListener(new ISelectionChangedListener() {
                    public void selectionChanged(SelectionChangedEvent event) {
                        StructuredSelection seld_elem = (StructuredSelection) event
                                .getSelection();

                        seldResourceName
                                .setRscAttrSetName(((AttributeSet) seld_elem
                                        .getFirstElement()).getName());

                        updateCycleTimes();

                        updateSelectedResource();
                    }
                });

        // get the selected rsc and add to the list.
        // ignoring the cycle time for now.
        //
        addResourceBtn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent ev) {
        		selectResource( true, true );
        	}
       	});
        // a double click will add the resource and close the dialog
        rscAttrSetLViewer.getList().addListener(SWT.MouseDoubleClick,
                new Listener() {
                    public void handleEvent(Event event) {
                    	selectResource(true, true);
                    }
                });
        cycleTimeCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent ev) {
                updateSelectedResource();
            }
        });
    }

    // set the initial values of the widgets. (base this on previously selected
    // values??)
    //
    protected void initWidgets(ResourceName initRscName) {

        seldResourceName = new ResourceName(initRscName);

        if (seldResourceName != null) {
            prevSeldCat = seldResourceName.getRscCategory();
        }
        seldResourceName = new ResourceName();
        seldResourceName.setRscCategory(ResourceCategory.GridRscCategory);
        updateResourceTypes();
    }

    // refresh the types list based on the type in the seldResourceName
    // use seldResourceName to select the type
    protected void updateResourceTypes() {

        rscTypeLViewer.setInput(rscDefnsMngr);
        rscTypeLViewer.refresh();

        rscTypeLViewer.getList().deselectAll();

        //
        if (!seldResourceName.getRscType().isEmpty()) {
            for (int itmIndx = 0; itmIndx < rscTypeLViewer.getList()
                    .getItemCount(); itmIndx++) {

                if (rscTypeLViewer.getList().getItem(itmIndx)
                        .equals(seldResourceName.getRscType())) {
                    rscTypeLViewer.getList().select(itmIndx);
                    break;
                }
            }

            if (rscTypeLViewer.getList().getSelectionCount() == 0) {
                seldResourceName.setRscType("");
                seldResourceName.setRscGroup("");
                seldResourceName.setRscAttrSetName("");
                seldResourceName.setCycleTime(null);
            }
        }

        // if no type is selected or it is not found for some reason, select the
        // first
        if (seldResourceName.getRscType().isEmpty()
                && rscTypeLViewer.getList().getItemCount() > 0) {

            rscTypeLViewer.getList().select(0);
            StructuredSelection seld_elem = (StructuredSelection) rscTypeLViewer
                    .getSelection();
            String rscType = ((ResourceDefinition) seld_elem.getFirstElement())
                    .getResourceDefnName();
            seldResourceName.setRscType(rscType);
            seldResourceName.setRscGroup("");
            seldResourceName.setRscAttrSetName("");
            seldResourceName.setCycleTime(null);
        }

        // updateCycleTimes();

        updateResourceGroups();
    }

    protected void updateResourceGroups() {
        rscGroupLViewer.setInput(rscDefnsMngr);
        rscGroupLViewer.refresh();

        // if there are no groups
        if (rscGroupLViewer.getList().getItemCount() == 0) {
            if (!seldResourceName.getRscGroup().isEmpty()) {
                // ????
                seldResourceName.setRscGroup("");
                seldResourceName.setRscAttrSetName("");
                seldResourceName.setCycleTime(null);
            }
        } else { // there are items in the groups list
                 // if a group has been selected then select it in the list,
                 // otherwise
                 // select the first in the list and update the seldResourceName
                 //
            rscGroupLViewer.getList().deselectAll();

            //
            if (!seldResourceName.getRscGroup().isEmpty()) {
                for (int itmIndx = 0; itmIndx < rscGroupLViewer.getList()
                        .getItemCount(); itmIndx++) {

                    if (rscGroupLViewer.getList().getItem(itmIndx)
                            .equals(seldResourceName.getRscGroup())) {
                        rscGroupLViewer.getList().select(itmIndx);
                        break;
                    }
                }

                if (rscGroupLViewer.getList().getSelectionCount() == 0) {
                    seldResourceName.setRscGroup("");
                    seldResourceName.setRscAttrSetName("");
                }
            }

            // if no type is selected or it is not found for some reason, select
            // the first
            if (seldResourceName.getRscGroup().isEmpty()
                    && rscGroupLViewer.getList().getItemCount() > 0) {

                rscGroupLViewer.getList().select(0);
                StructuredSelection seld_elem = (StructuredSelection) rscGroupLViewer
                        .getSelection();

                seldResourceName.setRscGroup((String) seld_elem
                        .getFirstElement());
                seldResourceName.setRscAttrSetName("");
            }
        }

        updateResourceAttrSets();
    }

    private void updateResourceAttrSets() {
        rscAttrSetLViewer.setInput(rscDefnsMngr);
        // rscAttrSetLViewer.refresh();

        rscAttrSetLViewer.getList().deselectAll();

        //
        if (!seldResourceName.getRscAttrSetName().isEmpty()) {
            for (int itmIndx = 0; itmIndx < rscAttrSetLViewer.getList()
                    .getItemCount(); itmIndx++) {

                AttributeSet attrSet = (AttributeSet) rscAttrSetLViewer
                        .getElementAt(itmIndx);

                if (attrSet.getName().equals(
                        seldResourceName.getRscAttrSetName())) {
                    rscAttrSetLViewer.getList().select(itmIndx);
                    break;
                }
            }

            if (rscAttrSetLViewer.getList().getSelectionCount() == 0) {
                seldResourceName.setRscAttrSetName("");
            }
        }

        // if no attr set is selected or it is not found for some reason, select
        // the first
        if (seldResourceName.getRscAttrSetName().isEmpty()
                && rscAttrSetLViewer.getList().getItemCount() > 0) {

            rscAttrSetLViewer.getList().select(0);
            StructuredSelection seld_elem = (StructuredSelection) rscAttrSetLViewer
                    .getSelection();

            seldResourceName.setRscAttrSetName(((AttributeSet) seld_elem
                    .getFirstElement()).getName());
        }

        updateCycleTimes();

        updateSelectedResource();
    }

    // when an attrSetName is selected and resource name, with possible cycle
    // time,
    // is ready for selection
    //
    public void updateSelectedResource() {

        String availMsg = "Data Not Available";

        // enable/disable the Add Resource Button
        // and set the name of the Resource
        boolean enableSelections = true;

        ResourceDefinition rscDefn = rscDefnsMngr
                .getResourceDefinition(seldResourceName.getRscType());

        if (!seldResourceName.isValid() || rscDefn == null) {
            enableSelections = false;
        }

        //
        if (enableSelections) {
            // if( onlyShowResourcesWithData ) {
            try {
                // this call will query just for the inventory params needed to
                // instantiate the resource
                // (ie imageType, productCode...) and not the actual dataTimes.
                // rscDefnsMngr.verifyParametersExist( seldResourceName );

                if (this.isForecast()) {
                    if (cycleTimes.isEmpty()) {
                        enableSelections = false;
                    }
                } else if (rscDefn.isPgenResource()) {
                    availMsg = "";
                } else if (!rscDefn.isRequestable()) {
                    availMsg = "";
                } else {
                    // If we aren't using the inventory then the query is too
                    // slow for the gui.
                    // TODO : If the inventory doesn't pan out then we could
                    // either
                    // implement this in another thread and accept the delay or
                    // add a
                    // 'Check Availability' button.
                    DataTime latestTime = rscDefn
                            .getLatestDataTime(seldResourceName);

                    if (latestTime == null || latestTime.isNull()) {
                        enableSelections = false;
                    } else {
                        availMsg = "Latest Data: "
                                + NmapCommon.getTimeStringFromDataTime(
                                        latestTime, "/");
                    }
                }
            } catch (VizException vizex) {
                out.println(vizex.getMessage());
                availMsg = "Error getting latest time.";
                enableSelections = false;
            }
            // }
        }

        if (enableSelections) {

            addResourceBtn.setEnabled(true);
            //replaceResourceBtn.setEnabled(replaceBtnEnabled);

            if (this.isForecast()) {
                cycleTimeCombo.setEnabled(true);
                cycleTimeCombo.setVisible(true);

                int seldCycleTimeIndx = cycleTimeCombo.getSelectionIndex(); // Cycle
                                                                            // for
                                                                            // Ensemble

                // TODO : Allow the user to select 'LATEST' specifically
                if (seldCycleTimeIndx == -1) {
                    seldResourceName.setCycleTimeLatest();
                } else if (seldCycleTimeIndx < cycleTimes.size()) {
                    seldResourceName.setCycleTime(cycleTimes
                            .get(seldCycleTimeIndx));
                } else { // shoulndn't happen
                    seldResourceName.setCycleTimeLatest();
                }

                availDataTimeLbl.setVisible(false);
            } else {
                availDataTimeLbl.setVisible(true);
                availDataTimeLbl.setText(availMsg);
                cycleTimeCombo.setEnabled(false);
                cycleTimeCombo.setVisible(true);
            }

            // For now, don't let the user select 'Latest'
            if (seldResourceName.isLatestCycleTime()) {

                addResourceBtn.setEnabled(false);
                replaceResourceBtn.setEnabled(false);
                seldRscNameTxt.setText("");
            } else {
                seldRscNameTxt.setText(seldResourceName.toString());
            }
        } else {
            seldRscNameTxt.setText("");
            addResourceBtn.setEnabled(false);
			//replaceResourceBtn.setEnabled( false );	

            availDataTimeLbl.setVisible(true);
            availDataTimeLbl.setText(availMsg);

            cycleTimeCombo.setEnabled(true);
            cycleTimeCombo.setVisible(true);
        }

        prevCatSeldRscNames.put(seldResourceName.getRscCategory(),
                seldResourceName);
    }

    // code for the Listeners for the Add Resource button and the double Click
    // on the list
    public void selectResource(boolean replaceRsc, boolean done) {

        if (seldResourceName.isValid()) {
            for (IResourceSelectedListener lstnr : rscSelListeners) {
                lstnr.resourceSelected(seldResourceName, replaceRsc,
                        false, done);
            }
        } 
    }

    public ResourceName getCurrentlySelectedResource() {
        return seldResourceName;
    }

    public void addResourceSelectionListener(IResourceSelectedListener lstnr) {
        rscSelListeners.add(lstnr);
    }

    // TODO: add a way to let the user specifically choose the "LATEST" cycle
    // time.
    // Currently the user cannot select a forecast resource without selecting an
    // available cycle time.
    //
    public void updateCycleTimes() {
        ResourceDefinition rscDefn = rscDefnsMngr
                .getResourceDefinition(seldResourceName);

        if (rscDefn == null) {
            cycleTimeCombo.setEnabled(true);
            return;
        } else {
            cycleTimeCombo.setEnabled(true);
            //cycleTimeCombo.setVisible(rscDefn.isForecast());
            cycleTimeCombo.setVisible(true);
            availDataTimeLbl.setVisible(!rscDefn.isForecast());

            if (!isForecast()) {
                seldResourceName.setCycleTime(null);
                return;
            }
        }

        try {
            // if this is reading from gempak
            //
            // would like to use the constant in NcGridData but E dependency
            // again.
            if (rscDefn.getPluginName().equals(GempakGrid.gempakPluginName)) {
                /*
                 * For a GEMPAK dataSource get gridCycleTimes from the
                 * dataLocation
                 */
                HashMap<String, String> rscParams = rscDefnsMngr
                        .getAllResourceParameters(seldResourceName);

                cycleTimeCombo.removeAll();
                cycleTimes.clear();
                try {
                    String dataLocation = null;
                    try {
                        dataLocation = GempakGrid.getGempakGridPath(rscParams
                                .get("GDFILE"));
                    } catch (VizException e) {
                        throw new VizException(e);
                    }
                    String[] gridCycleTimes = GempakGrid
                            .getGridCycleTimes(dataLocation,
                                    rscParams.get("GDFILE").toLowerCase());
                    for (String gct : gridCycleTimes) {
                        String gct2DataTimeFormat = "20" + gct.substring(0, 2)
                                + "-" + gct.substring(2, 4) + "-"
                                + gct.substring(4, 6) + " "
                                + gct.substring(7, 9) + ":"
                                + gct.substring(9, 11) + ":00.0 ";
                        cycleTimes.add(0, new DataTime(gct2DataTimeFormat));
                        cycleTimeCombo.add(gct, 0);
                    }
                    if (gridCycleTimes.length > 0) {
                        cycleTimeCombo.select(0);
                    }
                } catch (VizException e) {
                    out.println("Error querying cycle times: "
                            + e.getMessage().split(":")[1]);
                }

                return;
            }

            List<DataTime> availableTimes = null;

            // If the timeline is generated using frame intervals from a given
            // reference/cycle time, then get a list of selectable ref times.
            // Ideally this would also specify a way to generate the ref times
            // but its really
            // just for nctaf right now so just do it like taf needs.
            if (rscDefn.getTimelineGenMethod() == TimelineGenMethod.USE_FCST_FRAME_INTERVAL_FROM_REF_TIME) {
                // rscDefn.getPluginName().equals( "nctaf" ) ) {
                // Integer frameIntvl = rscDefn.getFrameSpan() *
                availableTimes = rscDefn.getNormalizedDataTimes(
                        seldResourceName, 24 * 60);
            } else {
                availableTimes = rscDefn.getDataTimes(seldResourceName);
            }

            // save the currently selected cycle time.
            //
            String curSelTime = cycleTimeCombo.getText();

            cycleTimeCombo.removeAll();
            cycleTimes.clear();

            //
            for (int t = availableTimes.size() - 1; t >= 0; t--) {
                DataTime dt = availableTimes.get(t);
                DataTime refTime = new DataTime(dt.getRefTime());

                if (!cycleTimes.contains(refTime)) {
                    cycleTimes.add(refTime);
                    String timeStr = NmapCommon.getTimeStringFromDataTime(dt,
                            "_");
                    cycleTimeCombo.add(timeStr);
                }
            }

            for (int t = 0; t < cycleTimeCombo.getItemCount(); t++) {
                if (cycleTimeCombo.getItem(t).equals(curSelTime)) {
                    cycleTimeCombo.select(t);
                    break;
                }
            }

            if (cycleTimes.isEmpty()) {
                cycleTimeCombo.setVisible(false);
                availDataTimeLbl.setVisible(true);
                availDataTimeLbl.setText("No Data Available");
            } else if (cycleTimeCombo.getSelectionIndex() == -1) {
                cycleTimeCombo.select(0);
            }

        } catch (VizException ve) {
            MessageDialog errDlg = new MessageDialog(
                    NcDisplayMngr.getCaveShell(), "Error", null,
                    "Error Requesting Cycle Times:" + ve.getMessage(),
                    MessageDialog.ERROR, new String[] { "OK" }, 0);
            errDlg.open();
            return;
        }

        return;
    }

    public void setReplaceEnabled(Boolean rplEnbld) {
        replaceBtnEnabled = rplEnbld;

        if (!isDisposed()) {
            updateSelectedResource();
        }
        // replaceResourceBtn.setEnabled( replaceEnabled );
    }

    public ResourceName getPrevSelectedResource() {
        return seldResourceName;
    }

    private boolean isForecast() {
        boolean gridAnalysis = false;
        StructuredSelection is = (StructuredSelection) rscAttrSetLViewer
                .getSelection();
        AttributeSet attr = (AttributeSet) is.getFirstElement();
        if (attr != null) {
            String gdattim = attr.getAttributes().get("GDATTIM");
            gridAnalysis = gdattim != null
                    && !gdattim.isEmpty()
                    && (gdattim.toUpperCase().contains("ALLF") || (gdattim
                            .toUpperCase().contains("FIRSTF") && gdattim
                            .toUpperCase().contains("LASTF")));
        }

        ResourceDefinition rscDefn = rscDefnsMngr
                .getResourceDefinition(seldResourceName.getRscType());

        return rscDefn.isForecast() && !gridAnalysis;
    }
}