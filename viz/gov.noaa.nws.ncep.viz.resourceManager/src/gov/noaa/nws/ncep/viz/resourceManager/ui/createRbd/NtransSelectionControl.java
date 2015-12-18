package gov.noaa.nws.ncep.viz.resourceManager.ui.createRbd;

import gov.noaa.nws.ncep.viz.common.display.NcDisplayType;
import gov.noaa.nws.ncep.viz.common.preferences.NcepGeneralPreferencesPage;
import gov.noaa.nws.ncep.viz.common.ui.NmapCommon;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData.TimelineGenMethod;
import gov.noaa.nws.ncep.viz.resources.manager.AttributeSet;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceCategory;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceDefinition;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceDefnsMngr;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceName;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * Date         Ticket#     Engineer     Description
 * ------------ ----------  -----------  --------------------------
 * 07/23/2014     R7317     B. Hebbard   Fork off NTRANS-specific code from ResourceSelectionControl
 * 08/26/2014     R7326     B. Hebbard   Adjust metafile column comparator to put latest data at top
 * 09/15/2014     R7445     B. Hebbard   At CPC request, persist model selected across dialog close/open 
 *                                       even if resource not preselected (from existing RBD contents).
 *                                       (This now differs from non-NTRANS behavior.)
 * 09/15/2014     R7445     B. Hebbard   Remove bogus "km" from product group name if it appears (per CPC)
 * 05/20/2015     R8048     P. Chowdhuri "Select New Resource" dialog should remember last selection
 *                                        for NMAP
 * 06/08/2015     R8048     P. Chowdhuri "Select New Resource" dialog should remember last selection
 *                                        for NTRANS
 * 11/03/2015     R8554     P. Chowdhuri  Filter was set to last filter used per data category
 * 12/16/2015     R8554     A. Su         Modified to remember last selected filter and resource.
 * 
 * </pre>
 * 
 * @author bhebbard
 * @version 1
 */
public class NtransSelectionControl extends ResourceSelectionControl {

    /**
     * Resource Category for this class.
     */
    public static final ResourceCategory NTRANS_RSC_CATEGORY = ResourceCategory.NtransRscCategory;

    /**
     * The suffix for Resource Type for NTRANS resources/
     */
    public static final String NTRANS_RSC_TYPE_SUFFIX = "_NT";

    /**
     * Previously or currently selected filter name. "ALL" is the default value.
     */
    protected static String prevSelectedFilter = RSC_FILTER_ALL;

    /**
     * Previously selected Ntrans Resource Name.
     */
    protected static ResourceName prevSelectedNtransRscName = null;

    /**
     * A mapping from Resource Type to last selected ResourceName for this type.
     */
    protected static HashMap<String, ResourceName> prevTypes2SelectedRscNames = new HashMap<String, ResourceName>();

    // protected Label rscGroupLbl = null;

    protected Label metafileLbl = null;

    protected Label productLbl = null;

    protected ListViewer metafileLViewer;

    protected ListViewer productLViewer;

    protected Map<String, ArrayList<String>> metafileToProductsMap = null;

    protected String selectedMetafile = "";

    protected String prevSelectedMetafile = "";

    protected String selectedProductName = "";

    public NtransSelectionControl(Composite parent, Boolean replaceVisible,
            Boolean replaceEnabled, ResourceName initRscName,
            Boolean multiPane, NcDisplayType dispType) throws VizException {

        // Skip parent's constructor, but call grandparent's constructor.
        super(parent);

        displayType = dispType;

        showLatestTimes = NmapCommon.getNcepPreferenceStore().getBoolean(
                NcepGeneralPreferencesPage.ShowLatestResourceTimes);
        onlyShowResourcesWithData = false;

        rscDefnsMngr = ResourceDefnsMngr.getInstance();

        replaceBtnVisible = replaceVisible;
        replaceBtnEnabled = replaceEnabled;

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

        setContentProviders();
        addSelectionListeners();

        initWidgets(initRscName);
    }

    /*
     * create all the widgets in the Resource Selection (top) section of the
     * sashForm.
     */
    protected void createSelectResourceGroup(Boolean multiPane) {

        // first create the lists and then attach the label to the top of them
        rscTypeLViewer = new ListViewer(sel_rsc_comp, SWT.SINGLE | SWT.BORDER
                | SWT.V_SCROLL | SWT.H_SCROLL);
        FormData fd = new FormData();
        fd.height = RSC_LIST_VIEWER_HEIGHT;
        fd.top = new FormAttachment(0, 75);
        fd.left = new FormAttachment(0, 10);
        fd.right = new FormAttachment(17, -8);

        // This allows a resize to change the size of the lists.
        fd.bottom = new FormAttachment(100, -125);

        rscTypeLViewer.getList().setLayoutData(fd);

        rscTypeLbl = new Label(sel_rsc_comp, SWT.NONE);
        rscTypeLbl.setText("Model");
        fd = new FormData();
        fd.left = new FormAttachment(rscTypeLViewer.getList(), 0, SWT.LEFT);
        fd.bottom = new FormAttachment(rscTypeLViewer.getList(), -3, SWT.TOP);

        rscTypeLbl.setLayoutData(fd);

        filterCombo = new Combo(sel_rsc_comp, SWT.DROP_DOWN | SWT.READ_ONLY);
        fd = new FormData();
        fd.width = 130;
        fd.bottom = new FormAttachment(rscTypeLViewer.getList(), -30, SWT.TOP);
        fd.left = new FormAttachment(rscTypeLViewer.getList(), 0, SWT.LEFT);
        filterCombo.setLayoutData(fd);

        Label filt_lbl = new Label(sel_rsc_comp, SWT.NONE);
        filt_lbl.setText("Model Filter");
        fd = new FormData();
        fd.left = new FormAttachment(filterCombo, 0, SWT.LEFT);
        fd.bottom = new FormAttachment(filterCombo, -3, SWT.TOP);
        filt_lbl.setLayoutData(fd);

        // first create the lists and then attach the label to the top of them
        metafileLViewer = new ListViewer(sel_rsc_comp, SWT.SINGLE | SWT.BORDER
                | SWT.V_SCROLL | SWT.H_SCROLL);
        fd = new FormData();
        fd.height = RSC_LIST_VIEWER_HEIGHT;
        fd.top = new FormAttachment(rscTypeLViewer.getList(), 0, SWT.TOP);
        fd.left = new FormAttachment(17, 0);
        fd.right = new FormAttachment(45, -8);

        fd.bottom = new FormAttachment(rscTypeLViewer.getList(), 0, SWT.BOTTOM);
        metafileLViewer.getList().setLayoutData(fd);

        metafileLbl = new Label(sel_rsc_comp, SWT.NONE);
        metafileLbl.setText("Metafile Name");
        fd = new FormData();
        fd.left = new FormAttachment(metafileLViewer.getList(), 0, SWT.LEFT);
        fd.bottom = new FormAttachment(metafileLViewer.getList(), -3, SWT.TOP);
        metafileLbl.setLayoutData(fd);

        // first create the lists and then attach the label to the top of them
        productLViewer = new ListViewer(sel_rsc_comp, SWT.SINGLE | SWT.BORDER
                | SWT.V_SCROLL | SWT.H_SCROLL);
        fd = new FormData();
        fd.height = RSC_LIST_VIEWER_HEIGHT;
        fd.top = new FormAttachment(metafileLViewer.getList(), 0, SWT.TOP);
        fd.left = new FormAttachment(45, 0);
        fd.right = new FormAttachment(84, -8);

        fd.bottom = new FormAttachment(metafileLViewer.getList(), 0, SWT.BOTTOM);
        productLViewer.getList().setLayoutData(fd);

        productLbl = new Label(sel_rsc_comp, SWT.NONE);
        productLbl.setText("Product Group");
        fd = new FormData();
        fd.left = new FormAttachment(productLViewer.getList(), 0, SWT.LEFT);
        fd.bottom = new FormAttachment(productLViewer.getList(), -3, SWT.TOP);
        productLbl.setLayoutData(fd);

        rscAttrSetLViewer = new ListViewer(sel_rsc_comp, SWT.SINGLE
                | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        fd = new FormData();
        fd.height = RSC_LIST_VIEWER_HEIGHT;
        fd.top = new FormAttachment(productLViewer.getList(), 0, SWT.TOP);
        fd.left = new FormAttachment(84, 0);
        fd.right = new FormAttachment(100, -10);
        fd.bottom = new FormAttachment(productLViewer.getList(), 0, SWT.BOTTOM);
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

        seldRscNameTxt = new Text(sel_rsc_comp, SWT.SINGLE | SWT.BORDER
                | SWT.READ_ONLY);

        fd = new FormData();
        fd.top = new FormAttachment(rscTypeLViewer.getList(), 40, SWT.BOTTOM);
        fd.left = new FormAttachment(rscTypeLViewer.getList(), 0, SWT.LEFT);
        fd.right = new FormAttachment(75, 0);
        seldRscNameTxt.setLayoutData(fd);

        Label seld_rsc_name_lbl = new Label(sel_rsc_comp, SWT.None);
        seld_rsc_name_lbl.setText("Selected NTRANS Resource Name");
        fd = new FormData();
        fd.left = new FormAttachment(seldRscNameTxt, 0, SWT.LEFT);
        fd.bottom = new FormAttachment(seldRscNameTxt, -3, SWT.TOP);
        seld_rsc_name_lbl.setLayoutData(fd);

        addResourceBtn = new Button(sel_rsc_comp, SWT.None);

        fd = new FormData();

        if (replaceBtnVisible) {
            fd.top = new FormAttachment(seldRscNameTxt, 20, SWT.BOTTOM);
            fd.right = new FormAttachment(50, -20);
        } else {
            fd.top = new FormAttachment(seldRscNameTxt, 20, SWT.BOTTOM);
            fd.left = new FormAttachment(50, 20);
        }

        addResourceBtn.setLayoutData(fd);
        addResourceBtn.setText("  Add Resource ");

        replaceResourceBtn = new Button(sel_rsc_comp, SWT.None);
        fd = new FormData();
        fd.left = new FormAttachment(50, 20);
        fd.top = new FormAttachment(addResourceBtn, 0, SWT.TOP);
        replaceResourceBtn.setLayoutData(fd);
        replaceResourceBtn.setText(" Replace Resource ");

        // both for now unless we change it to be one or the other
        replaceResourceBtn.setVisible(replaceBtnVisible);

        addToAllPanesBtn = new Button(sel_rsc_comp, SWT.CHECK);
        fd = new FormData();
        fd.left = new FormAttachment(seldRscNameTxt, 40, SWT.RIGHT);
        fd.top = new FormAttachment(replaceResourceBtn, 0, SWT.TOP);
        addToAllPanesBtn.setLayoutData(fd);
        addToAllPanesBtn.setText("Add To All Panes");

        addToAllPanesBtn.setVisible(multiPane);

        // allow the user to enter any previous Datatime
        cycleTimeCombo = new Combo(sel_rsc_comp, SWT.READ_ONLY);
        fd = new FormData();
        fd.left = new FormAttachment(80, 0);
        fd.right = new FormAttachment(100, -20);
        fd.top = new FormAttachment(seldRscNameTxt, 0, SWT.TOP);

        cycleTimeCombo.setLayoutData(fd);

        cycleTimeLbl = new Label(sel_rsc_comp, SWT.None);
        cycleTimeLbl.setText("");
        fd = new FormData();
        fd.left = new FormAttachment(cycleTimeCombo, 0, SWT.LEFT);
        fd.bottom = new FormAttachment(cycleTimeCombo, -3, SWT.TOP);
        cycleTimeLbl.setLayoutData(fd);
    }

    protected void setContentProviders() {

        rscTypeLViewer.setContentProvider(new IStructuredContentProvider() {
            @Override
            public Object[] getElements(Object inputElement) {

                String inputFilter = prevSelectedFilter;
                if (inputFilter.equals(RSC_FILTER_ALL)) {
                    inputFilter = "";
                }

                if (selectedRscName.getRscCategory() != ResourceCategory.NullCategory) {
                    try {
                        // Include generated types only; include enabled types.
                        List<ResourceDefinition> rscTypes = rscDefnsMngr
                                .getResourceDefnsForCategory(
                                        NTRANS_RSC_CATEGORY, inputFilter,
                                        displayType, true, false);

                        return rscTypes.toArray();

                    } catch (VizException e) {
                        MessageDialog errDlg = new MessageDialog(NcDisplayMngr
                                .getCaveShell(), "Error", null,
                                "Error getting Resource Types\n"
                                        + e.getMessage(), MessageDialog.ERROR,
                                new String[] { OK }, 0);
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

            public int category(Object element) {
                ResourceDefinition rd = (ResourceDefinition) element;
                return (rd.isForecast() ? 1 : 0);
            }

            @Override
            public int compare(Viewer viewer, Object obj1, Object obj2) {
                int catComp = category(obj1) - category(obj2);
                return (catComp != 0 ? catComp : rscDefnsMngr
                        .getDefaultRscDefnComparator().compare(
                                (ResourceDefinition) obj1,
                                (ResourceDefinition) obj2));
            }
        });

        rscTypeLViewer.setLabelProvider(new LabelProvider() {
            public String getText(Object element) {
                ResourceDefinition rd = (ResourceDefinition) element;

                return (rd == null ? "null" : remove_NT_Suffix(rd
                        .getResourceDefnName()));
            }
        });

        metafileLViewer.setContentProvider(new IStructuredContentProvider() {
            @Override
            public Object[] getElements(Object inputElement) {
                String rscType = selectedRscName.getRscType();

                if (!rscType.isEmpty()) {
                    // if this resource uses attrSetGroups then get get the list
                    // of groups. (PGEN uses groups but we will list the
                    // subTypes (products) and not the single PGEN attr set
                    // group)
                    if (rscDefnsMngr.doesResourceUseAttrSetGroups(rscType)
                            && !selectedRscName.isPgenResource()) {

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

                                buildMetafileToProductsMap(rscGroups);

                                return metafileToProductsMap.keySet().toArray(
                                        new String[0]);
                            }
                        } catch (VizException e) {
                            MessageDialog errDlg = new MessageDialog(
                                    NcDisplayMngr.getCaveShell(), "Error",
                                    null, "Error getting sub-types\n"
                                            + e.getMessage(),
                                    MessageDialog.ERROR, new String[] { OK }, 0);
                            errDlg.open();
                        }
                    }
                }

                buildMetafileToProductsMap(new String[] {});
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

        metafileLViewer.setComparator(new ViewerComparator() {
            @Override
            public int compare(Viewer viewer, Object obj1, Object obj2) {

                // Ordering of the metafile name column is a bit more
                // complicated than the other columns, because we want to
                // present files in reverse chronological order so the most
                // recent data appear at the top. But among files representing
                // the same date"+"time, we want to revert to standard
                // lexicographical ordering.
                if (!(obj1 instanceof String && obj2 instanceof String)) {
                    return super.compare(viewer, obj1, obj2);
                } else {

                    // This pattern covers known date-time orderings in
                    // metafile names (modified _ to - as already done by
                    // decoder)
                    final Pattern pattern = Pattern
                            .compile("((\\d\\d){3,4})-?(\\d\\d)?");
                    Matcher m1 = pattern.matcher((String) obj1);
                    Matcher m2 = pattern.matcher((String) obj2);
                    String datetime1 = "";
                    String datetime2 = "";

                    // Must handle multiple matches -- if found, take the
                    // longest match
                    while (m1.find()) {
                        if (m1.group(0).length() >= datetime1.length()) {
                            datetime1 = m1.group(0);
                        }
                    }
                    while (m2.find()) {
                        if (m2.group(0).length() >= datetime2.length()) {
                            datetime2 = m2.group(0);
                        }
                    }
                    if (datetime1.equals(datetime2)) {
                        return super.compare(viewer, obj1, obj2);
                    } else { // Latest date/time first.
                        return -1 * super.compare(viewer, datetime1, datetime2);
                    }
                }
            }
        });

        productLViewer.setContentProvider(new IStructuredContentProvider() {
            @Override
            public Object[] getElements(Object inputElement) {

                if (metafileToProductsMap == null || selectedMetafile.isEmpty()) {
                    return new String[] {};
                }

                // return all product names associated with the selected
                // metafile
                ArrayList<String> products = metafileToProductsMap
                        .get(selectedMetafile);

                if (products == null) {
                    return new String[] {};
                }
                return products.toArray(new String[0]);
            }

            @Override
            public void dispose() {
            }

            @Override
            public void inputChanged(Viewer viewer, Object oldInput,
                    Object newInput) {
            }
        });

        productLViewer.setComparator(new ViewerComparator() {
            @Override
            public int compare(Viewer viewer, Object obj1, Object obj2) {
                return super.compare(viewer, obj1, obj2);
            }
        });

        productLViewer.setLabelProvider(new LabelProvider() {
            public String getText(Object element) {
                String productName = (String) element;

                final String removeMe = "km";
                if (productName.endsWith(removeMe)) {
                    statusHandler
                            .warn("[WARNING:  Caught a productName ending in '"
                                    + removeMe + "']");
                    return productName.substring(0, productName.length() - 2);
                } else {
                    return productName;
                }
            }
        });

        rscAttrSetLViewer.setContentProvider(new IStructuredContentProvider() {
            @Override
            public Object[] getElements(Object inputElement) {

                if (!selectedRscName.getRscType().isEmpty()) {
                    List<AttributeSet> attrSets = rscDefnsMngr
                            .getAttrSetsForResource(selectedRscName, true);

                    maxLengthOfSelectableAttrSets = 0;

                    for (AttributeSet attrSet : attrSets) {
                        if (attrSet != null
                                && attrSet.getName().length() > maxLengthOfSelectableAttrSets) {
                            maxLengthOfSelectableAttrSets = attrSet.getName()
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
            public int compare(Viewer viewer, Object obj1, Object obj2) {
                String str1 = ((AttributeSet) obj1).getName();
                String str2 = ((AttributeSet) obj2).getName();

                if (str1.equals(DEFAULT) || str1.equals(STANDARD)) {
                    return -1;
                } else if (str2.equals(DEFAULT) || str2.equals(STANDARD)) {
                    return 1;
                } else {
                    return (str1.compareTo(str2));
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

                ResourceName rscName = new ResourceName(selectedRscName);
                rscName.setRscAttrSetName(attrSetName);

                ResourceDefinition rscDefn = rscDefnsMngr
                        .getResourceDefinition(rscName.getRscType());

                if (rscDefn == null) {
                    return "";
                }

                if (!showLatestTimes || rscDefn.isForecast()) {
                    return attrSetName;
                }

                while (attrSetName.length() < maxLengthOfSelectableAttrSets) {
                    attrSetName = attrSetName + " ";
                }

                // If we aren't using the inventory then the query is too slow
                // for the gui. <p> TODO: If the inventory doesn't pan out then
                // we could either implement this in another thread and accept
                // the delay or add a 'Check Availability' button.
                if (rscName.isValid() && rscDefn.usesInventory()
                        && rscDefn.getInventoryEnabled()) {

                    try {
                        DataTime latestTime = rscDefn
                                .getLatestDataTime(rscName);

                        if (latestTime.isNull()) {
                            attrSetName = attrSetName + " (No Data)";
                        } else {
                            String latestTimeStr = NmapCommon
                                    .getTimeStringFromDataTime(latestTime, "_");

                            attrSetName = attrSetName + " (" + latestTimeStr
                                    + ")";
                        }
                    } catch (VizException e) {
                        statusHandler.error(e.getMessage());
                    }
                }
                return attrSetName;
            }
        });
    }

    protected void buildMetafileToProductsMap(String[] rscGroups) {

        // Given an array of combined metafile_product strings, build map from
        // metafiles to lists of associated products.
        if (metafileToProductsMap == null) {
            metafileToProductsMap = new HashMap<String, ArrayList<String>>();
        } else {
            metafileToProductsMap.clear();
        }

        for (String pairname : rscGroups) {
            String[] splits = pairname.split("_", 2);
            if (splits == null || splits.length < 2) {
                // error
            } else {
                String metafile = splits[0];
                String product = splits[1];

                ArrayList<String> products = metafileToProductsMap
                        .get(metafile);
                // if map doesn't yet contain an entry (products list) for
                // this metafile, add one
                if (products == null) {
                    products = new ArrayList<String>();
                    metafileToProductsMap.put(metafile, products);
                }
                products.add(product);
            }
        }
    }

    /*
     * add all of the listeners for widgets on this dialog
     */
    protected void addSelectionListeners() {

        filterCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {

                String selectedFilter = filterCombo.getText();

                if ((selectedFilter != null)
                        && (selectedFilter.equals(prevSelectedFilter))) {
                    return;
                }

                prevSelectedFilter = selectedFilter;

                updateResourceTypes();
            }
        });

        rscTypeLViewer
                .addSelectionChangedListener(new ISelectionChangedListener() {
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

                        ResourceName savedRscName = prevTypes2SelectedRscNames
                                .get(selectedType);

                        if (savedRscName != null) {
                            selectedRscName = savedRscName;
                        } else {
                            selectedRscName.setRscType(selectedType);
                            selectedRscName.setRscGroup("");
                            selectedRscName.setRscAttrSetName("");
                            selectedRscName.setCycleTime(null);
                        }

                        updateMetafiles();
                    }
                });

        metafileLViewer
                .addSelectionChangedListener(new ISelectionChangedListener() {
                    public void selectionChanged(SelectionChangedEvent event) {
                        StructuredSelection selectedElement = (StructuredSelection) event
                                .getSelection();
                        selectedMetafile = (String) selectedElement
                                .getFirstElement();

                        selectedRscName.setRscAttrSetName("");

                        updateProducts();
                    }
                });

        productLViewer
                .addSelectionChangedListener(new ISelectionChangedListener() {
                    public void selectionChanged(SelectionChangedEvent event) {
                        StructuredSelection selectedElement = (StructuredSelection) event
                                .getSelection();
                        selectedRscName.setRscGroup((String) selectedElement
                                .getFirstElement());
                        selectedRscName.setRscAttrSetName("");

                        updateResourceAttrSets();
                    }
                });

        rscAttrSetLViewer
                .addSelectionChangedListener(new ISelectionChangedListener() {
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

        /*
         * get the selected rsc and add to the list. ignoring the cycle time for
         * now.
         */
        addResourceBtn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent ev) {
                selectResource(false, false);
            }
        });

        /*
         * TODO: do we want replace to pop down the dialog?
         */
        replaceResourceBtn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent ev) {
                selectResource(true, false);
            }
        });

        /*
         * a double click will add the resource and close the dialog
         */
        rscAttrSetLViewer.getList().addListener(SWT.MouseDoubleClick,
                new Listener() {
                    public void handleEvent(Event event) {
                        if (addResourceBtn.isVisible()) {
                            selectResource(false, true);
                        } else {
                            selectResource(true, true);
                        }
                    }
                });

        cycleTimeCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent ev) {
                updateSelectedResource();
            }
        });
    }

    @Override
    protected void initWidgets(ResourceName initRscName) {

        if (prevSelectedNtransRscName != null) {
            selectedRscName = new ResourceName(prevSelectedNtransRscName);
        } else {
            selectedRscName = new ResourceName(initRscName);
            selectedRscName.setRscCategory(NTRANS_RSC_CATEGORY);
        }

        addToAllPanesBtn.setSelection(false);

        updateResourceFilters();
    }

    /**
     * Update the contents of the filter combo.
     */
    @Override
    protected void updateResourceFilters() {

        List<String> filterList = rscDefnsMngr.getAllFilterLabelsForCategory(
                NTRANS_RSC_CATEGORY, displayType);

        Collections.sort(filterList);
        filterList.add(0, RSC_FILTER_ALL);

        String[] filterArray = new String[0];
        filterArray = filterList.toArray(filterArray);
        filterCombo.setItems(filterArray);

        boolean isMatched = false;
        for (int i = 0; i < filterArray.length; i++) {
            if (filterArray[i].equals(prevSelectedFilter)) {
                filterCombo.select(i);
                isMatched = true;
                break;
            }
        }

        if (!isMatched) {
            filterCombo.select(0);
            prevSelectedFilter = RSC_FILTER_ALL;

            selectedRscName.setRscType("");
            selectedRscName.setRscGroup("");
            selectedRscName.setRscAttrSetName("");
            selectedRscName.setCycleTime(null);
        }

        updateResourceTypes();
    }

    /*
     * refresh the types list based on the type in the seldResourceName use
     * seldResourceName to select the type
     */
    protected void updateResourceTypes() {

        rscTypeLViewer.setInput(rscDefnsMngr);
        rscTypeLViewer.refresh();

        org.eclipse.swt.widgets.List modelList = rscTypeLViewer.getList();
        modelList.deselectAll();

        String selectedModel = remove_NT_Suffix(selectedRscName.getRscType());

        boolean isMatched = false;
        if (!selectedModel.isEmpty()) {
            for (int i = 0; i < modelList.getItemCount(); i++) {
                if (modelList.getItem(i).equals(selectedModel)) {
                    modelList.select(i);
                    isMatched = true;
                    break;
                }
            }
        }

        if (!isMatched) {
            selectedModel = modelList.getItem(0);
            modelList.select(0);
            selectedRscName.setRscType(selectedModel + NTRANS_RSC_TYPE_SUFFIX);
            selectedRscName.setRscGroup("");
            selectedRscName.setRscAttrSetName("");
            selectedRscName.setCycleTime(null);
        }

        updateMetafiles();
    }

    protected void updateMetafiles() {
        metafileLViewer.setInput(rscDefnsMngr);
        metafileLViewer.refresh();

        if (metafileLViewer.getList().getItemCount() == 0) {
            selectedRscName.setRscAttrSetName("");
            selectedRscName.setCycleTime(null);
        } else {
            // there are items in the metafiles list if a metafile has been
            // selected (before?) then select it in the list, otherwise
            // select the first in the list and update the seldResourceName
            metafileLViewer.getList().deselectAll();

            if (!selectedRscName.getRscGroup().isEmpty()) {
                selectedMetafile = selectedRscName.getRscGroup().split("_")[0];
                for (int i = 0; i < metafileLViewer.getList().getItemCount(); i++) {

                    if (metafileLViewer.getList().getItem(i)
                            .equals(selectedMetafile)) {
                        metafileLViewer.getList().select(i);
                        break;
                    }
                }

                if (metafileLViewer.getList().getSelectionCount() == 0) {
                    selectedRscName.setRscAttrSetName("");
                }
            }

            // if no metafile is selected or it is not found for some reason,
            // select the first
            if (selectedRscName.getRscGroup().isEmpty()
                    && metafileLViewer.getList().getItemCount() > 0) {

                metafileLViewer.getList().select(0);
                StructuredSelection seld_elem = (StructuredSelection) metafileLViewer
                        .getSelection();

                selectedMetafile = ((String) seld_elem.getFirstElement());
                if (!selectedMetafile.isEmpty()
                        && !selectedProductName.isEmpty()) {
                    selectedRscName.setRscGroup(selectedMetafile + "_"
                            + selectedProductName);
                }
            }
        }

        updateProducts();
    }

    protected void updateProducts() {
        productLViewer.setInput(rscDefnsMngr);
        productLViewer.refresh();
        productLViewer.getList().deselectAll();

        String seldRscGroup = selectedRscName.getRscGroup();

        if (!seldRscGroup.isEmpty()) {

            String tmpMeta = seldRscGroup.split("_")[0];
            String tmpRscGrp = seldRscGroup.split("_")[1];
            if (!tmpMeta.equals(selectedMetafile)) {
                selectedRscName.setRscGroup(selectedMetafile + "_" + tmpRscGrp);
            }

            String selectedProduct = selectedRscName.getRscGroup().split("_")[1];

            for (int i = 0; i < productLViewer.getList().getItemCount(); i++) {

                if (productLViewer.getList().getItem(i).equals(selectedProduct)) {
                    productLViewer.getList().select(i);
                    break;
                }
            }

            if (productLViewer.getList().getSelectionCount() == 0) {
                ArrayList<String> products = (ArrayList<String>) metafileToProductsMap
                        .get(selectedMetafile);

                Collections.sort(products);
                selectedProductName = (String) products.get(0);

                if (!selectedMetafile.equals(prevSelectedMetafile)) {
                    productLViewer.getList().select(0);
                }

                if (!selectedMetafile.isEmpty()
                        && !selectedProductName.isEmpty()) {
                    selectedRscName.setRscGroup(selectedMetafile + "_"
                            + selectedProductName);
                }

                selectedRscName.setRscAttrSetName("");
            }
        }

        if (selectedRscName.getRscGroup().isEmpty()
                && productLViewer.getList().getItemCount() > 0) {

            productLViewer.getList().select(0);
            StructuredSelection seld_elem = (StructuredSelection) productLViewer
                    .getSelection();

            selectedProductName = ((String) seld_elem.getFirstElement());
            if (!selectedMetafile.isEmpty() && !selectedProductName.isEmpty()) {
                selectedRscName.setRscGroup(selectedMetafile + "_"
                        + selectedProductName);
            }

            selectedRscName.setRscAttrSetName("");
        }

        updateResourceAttrSets();
    }

    protected void updateResourceAttrSets() {
        rscAttrSetLViewer.setInput(rscDefnsMngr);

        rscAttrSetLViewer.getList().deselectAll();

        if (!selectedRscName.getRscAttrSetName().isEmpty()) {
            for (int itmIndx = 0; itmIndx < rscAttrSetLViewer.getList()
                    .getItemCount(); itmIndx++) {

                AttributeSet attrSet = (AttributeSet) rscAttrSetLViewer
                        .getElementAt(itmIndx);

                if (attrSet.getName().equals(
                        selectedRscName.getRscAttrSetName())) {
                    rscAttrSetLViewer.getList().select(itmIndx);
                    break;
                }
            }

            if (rscAttrSetLViewer.getList().getSelectionCount() == 0) {
                selectedRscName.setRscAttrSetName("");
            }
        }

        if (selectedRscName.getRscAttrSetName().isEmpty()
                && rscAttrSetLViewer.getList().getItemCount() > 0) {

            rscAttrSetLViewer.getList().select(0);
            StructuredSelection seld_elem = (StructuredSelection) rscAttrSetLViewer
                    .getSelection();

            selectedRscName.setRscAttrSetName(((AttributeSet) seld_elem
                    .getFirstElement()).getName());
        }

        updateCycleTimes();

        updateSelectedResource();
    }

    /*
     * when an attrSetName is selected and resource name, with possible cycle
     * time, is ready for selection
     * 
     * @see
     * gov.noaa.nws.ncep.viz.resourceManager.ui.createRbd.ResourceSelectionControl
     * #updateSelectedResource()
     */
    @Override
    public void updateSelectedResource() {
        String availMsg = "Data Not Available";
        String temp = "";

        // enable/disable the Add Resource Button and set the name of the
        // Resource
        boolean enableSelections = true;

        ResourceDefinition rscDefn = rscDefnsMngr
                .getResourceDefinition(selectedRscName.getRscType());

        if (!selectedRscName.isValid() || rscDefn == null) {
            enableSelections = false;
        }

        if (enableSelections) {
            try {
                // this call will query just for the inventory params needed to
                // instantiate the resource (ie imageType, productCode...) and
                // not the actual dataTimes. rscDefnsMngr.verifyParametersExist(
                // seldResourceName );
                if (rscDefn.isForecast()) {
                    if (cycleTimes.isEmpty()) {
                        enableSelections = false;
                    }
                } else if (rscDefn.isPgenResource()) {
                    availMsg = "";
                } else if (!rscDefn.isRequestable()) {
                    availMsg = "";
                } else {
                    // If we aren't using the inventory then the query is too
                    // slow for the gui. <p> TODO: If the inventory doesn't pan
                    // out then we could either implement this in another thread
                    // and accept the delay or add a 'Check Availability'
                    // button.
                    DataTime latestTime = rscDefn
                            .getLatestDataTime(selectedRscName);

                    if (latestTime == null || latestTime.isNull()) {
                        enableSelections = false;
                    } else {
                        availMsg = "Latest Data: "
                                + NmapCommon.getTimeStringFromDataTime(
                                        latestTime, "/");
                    }
                }
            } catch (VizException e) {
                statusHandler.warn(e.getMessage());
                availMsg = "Error getting latest time.";
                enableSelections = false;
            }

            // create the product group for user-selected metafile
            String rscGroup = selectedRscName.getRscGroup();
            int indexSep = rscGroup.lastIndexOf("_");

            if (-1 == indexSep) {
                temp = selectedMetafile + "_" + selectedRscName.getRscGroup();
                selectedRscName.setRscGroup(temp);
            }
        }

        if (enableSelections) {

            addResourceBtn.setEnabled(true);
            replaceResourceBtn.setEnabled(replaceBtnEnabled);

            if (rscDefn.isForecast()) {

                cycleTimeLbl.setEnabled(true);
                cycleTimeCombo.setEnabled(true);
                cycleTimeLbl.setVisible(true);
                cycleTimeCombo.setVisible(true);

                // Cycle for Ensemble
                int seldCycleTimeIndx = cycleTimeCombo.getSelectionIndex();

                // TODO: Allow the user to select 'LATEST' specifically
                selectedRscName.setCycleTimeLatest();

                if (seldCycleTimeIndx == -1) {
                    selectedRscName.setCycleTimeLatest();
                } else if (seldCycleTimeIndx < cycleTimes.size()) {
                    selectedRscName.setCycleTime(cycleTimes
                            .get(seldCycleTimeIndx));
                } else { // shouldn't happen
                    selectedRscName.setCycleTimeLatest();
                }

                availDataTimeLbl.setVisible(false);
            } else {
                availDataTimeLbl.setVisible(true);
                availDataTimeLbl.setText(availMsg);
            }

            // For now, don't let the user select 'Latest'
            if (selectedRscName.isLatestCycleTime()) {

                addResourceBtn.setEnabled(false);
                replaceResourceBtn.setEnabled(false);
                seldRscNameTxt.setText("");
            } else {
                seldRscNameTxt.setText(selectedRscName.toString());
            }
        } else {
            seldRscNameTxt.setText("");
            addResourceBtn.setEnabled(false);
            replaceResourceBtn.setEnabled(false);

            availDataTimeLbl.setVisible(true);
            availDataTimeLbl.setText(availMsg);

            cycleTimeLbl.setVisible(false);
            cycleTimeCombo.setVisible(false);
        }

        prevTypes2SelectedRscNames.put(selectedRscName.getRscType(),
                new ResourceName(selectedRscName));
        prevSelectedNtransRscName = selectedRscName;
    }

    /*
     * TODO: add a way to let the user specifically choose the "LATEST" cycle
     * time.
     * 
     * Currently the user cannot select a forecast resource without selecting an
     * available cycle time.
     * 
     * @see
     * gov.noaa.nws.ncep.viz.resourceManager.ui.createRbd.ResourceSelectionControl
     * #updateCycleTimes()
     */
    public void updateCycleTimes() {
        ResourceDefinition rscDefn = rscDefnsMngr
                .getResourceDefinition(selectedRscName);

        if (rscDefn == null) {
            cycleTimeLbl.setEnabled(false);
            cycleTimeCombo.setEnabled(false);
            return;
        } else {
            cycleTimeLbl.setEnabled(true);
            cycleTimeCombo.setEnabled(true);
            cycleTimeLbl.setVisible(rscDefn.isForecast());
            cycleTimeCombo.setVisible(rscDefn.isForecast());
            cycleTimeLbl.setVisible(false);
            cycleTimeCombo.setVisible(false);
            availDataTimeLbl.setVisible(!rscDefn.isForecast());

            if (!rscDefn.isForecast()) {
                return;
            }
        }

        try {
            List<DataTime> availableTimes = null;

            // If the timeline is generated using frame intervals from a
            // given reference/cycle time, then get a list of selectable ref
            // times. Ideally this would also specify a way to generate the ref
            // times but its really just for nctaf right now so just do it like
            // taf needs.
            if (rscDefn.getTimelineGenMethod() == TimelineGenMethod.USE_FCST_FRAME_INTERVAL_FROM_REF_TIME) {

                availableTimes = rscDefn.getNormalizedDataTimes(
                        selectedRscName, 24 * 60);
            } else {
                availableTimes = rscDefn.getDataTimes(selectedRscName);
            }

            // save the currently selected cycle time.
            String curSelTime = cycleTimeCombo.getText();

            cycleTimeCombo.removeAll();
            cycleTimes.clear();

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
                cycleTimeLbl.setVisible(false);
                availDataTimeLbl.setVisible(true);
                availDataTimeLbl.setText("No Data Available");
            } else if (cycleTimeCombo.getSelectionIndex() == -1) {
                cycleTimeCombo.select(0);
            }

        } catch (VizException e) {
            MessageDialog errDlg = new MessageDialog(
                    NcDisplayMngr.getCaveShell(), "Error", null,
                    "Error Requesting Cycle Times:" + e.getMessage(),
                    MessageDialog.ERROR, new String[] { OK }, 0);
            errDlg.open();
            return;
        }

        return;
    }

    private String remove_NT_Suffix(String input) {
        String output = input;

        if (input.endsWith(NTRANS_RSC_TYPE_SUFFIX)) {
            output = input.substring(0,
                    input.length() - NTRANS_RSC_TYPE_SUFFIX.length());
        }
        return output;
    }
}
