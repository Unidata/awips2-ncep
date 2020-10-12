package gov.noaa.nws.ncep.viz.resourceManager.ui.manageResources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.text.StringMatcher;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.exception.VizException;

import gov.noaa.nws.ncep.viz.resourceManager.ui.manageResources.ManageResourceControl.IEditResourceComposite;
import gov.noaa.nws.ncep.viz.resources.manager.AttrSetGroup;
import gov.noaa.nws.ncep.viz.resources.manager.AttrSetGroup.RscAndGroupName;
import gov.noaa.nws.ncep.viz.resources.manager.AttrSetLabel;
import gov.noaa.nws.ncep.viz.resources.manager.AttrSetLabels;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceDefinition;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceDefnsMngr;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceName;

/**
 *
 *
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer   Description
 * ------------- -------- ---------- -------------------------------------------
 * Jun 09, 2010  273      Greg Hull  Created
 * Jul 22, 2011  450      Greg Hull  Save to User Localization
 * Jun 03, 2012  816      Greg Hull  Add Filter for attr set list
 * Jun 07, 2012  816      Greg Hull  Add ability to apply to other resources
 * Sep 16, 2015  8824     A. Su      Modified to preserve display aliases after
 *                                   the Manage Resources dialog changes
 *                                   attribute list.
 * Jun 04, 2019  7886     tgurney    Remove use of no longer existent Eclipse
 *                                   API
 * Oct 12, 2020  8241     randerso   Eclipse 4.17 moved StringMatcher class
 * 
 * </pre>
 *
 * @author
 */
class EditAttrSetGroupComp extends Composite implements IEditResourceComposite {
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(EditAttrSetGroupComp.class);

    private static final String OK = "OK";

    private static final String DONE = "Done";

    private ResourceDefnsMngr rscDefnMngr;

    // the parent composite
    private ManageResourceControl mngrControl;

    private ResourceName seldRscName = null;

    private ResourceDefinition seldRscDefn;

    private AttrSetGroup seldAttrSetGroup;

    private Text attrSetGroupNameTxt;

    private Text resourceTxt;

    private ListViewer applyForRscsLViewer;

    private Button applyForAllRscsBtn;

    private Text filterTxt;

    private Button addAttrSetBtn;

    private Button removeAttrSetBtn;

    private ListViewer availAttrSetsLViewer;

    private ListViewer seldAttrSetsLViewer;

    private Button saveAttrSetGroupBtn;

    private Button newAttrSetGroupBtn;

    private Button cancelBtn;

    private List<String> availRscsForGroup;

    private List<String> availAttrSets;

    private List<String> seldAttrSets;

    // used for all 3 of the ListViewers
    private IStructuredContentProvider stringArrayListContentProvider = new IStructuredContentProvider() {
        @Override
        public Object[] getElements(Object inputElement) {
            @SuppressWarnings("unchecked")
            List<String> stringArrayList = (List<String>) inputElement;
            return stringArrayList.toArray();
        }

        @Override
        public void dispose() {
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput,
                Object newInput) {
        }
    };

    protected class AttrSetViewerFilter extends ViewerFilter {

        private StringMatcher filterMatcher = null;

        @Override
        public boolean select(Viewer viewer, Object parentElement,
                Object element) {

            String elemStr = (String) element;
            return filterMatcher == null
                    || filterMatcher.match(elemStr, 0, elemStr.length());
        }

        public void setFilterMatcher(StringMatcher sm) {
            filterMatcher = sm;
        }
    }

    private AttrSetViewerFilter attrSetFilter = new AttrSetViewerFilter();

    public EditAttrSetGroupComp(Composite parent, int style,
            ManageResourceControl mgrCtl) {
        super(parent, style);
        Composite top_form = this;

        FormData fd = new FormData();
        // offset so the title shows up
        fd.top = new FormAttachment(0, 12);
        fd.left = new FormAttachment(0, 0);
        fd.right = new FormAttachment(100, 0);
        fd.bottom = new FormAttachment(100, 0);
        top_form.setLayoutData(fd);

        setLayoutData(fd);

        top_form.setLayout(new FormLayout());

        mngrControl = mgrCtl;
        rscDefnMngr = mngrControl.getRscDefnMngr();

        attrSetGroupNameTxt = new Text(top_form, SWT.SINGLE | SWT.BORDER);
        attrSetGroupNameTxt.setText("");

        fd = new FormData();
        fd.width = 120;
        fd.top = new FormAttachment(0, 25);
        fd.left = new FormAttachment(0, 15);
        attrSetGroupNameTxt.setLayoutData(fd);

        Label attrSetGroupNameLbl = new Label(top_form, SWT.NONE);
        attrSetGroupNameLbl.setText("Group Name");
        fd = new FormData();
        fd.bottom = new FormAttachment(attrSetGroupNameTxt, -3, SWT.TOP);
        fd.left = new FormAttachment(attrSetGroupNameTxt, 0, SWT.LEFT);
        attrSetGroupNameLbl.setLayoutData(fd);

        resourceTxt = new Text(top_form,
                SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
        resourceTxt.setText("");
        // indicate readonly
        resourceTxt.setBackground(getParent().getBackground());

        fd = new FormData();
        fd.width = 120;
        fd.top = new FormAttachment(attrSetGroupNameTxt, 35, SWT.BOTTOM);
        fd.left = new FormAttachment(attrSetGroupNameTxt, 0, SWT.LEFT);
        resourceTxt.setLayoutData(fd);

        resourceTxt.setVisible(false);

        Label applyToLbl = new Label(top_form, SWT.NONE);
        applyToLbl.setText("Applies For:");
        fd = new FormData();
        fd.bottom = new FormAttachment(resourceTxt, -3, SWT.TOP);
        fd.left = new FormAttachment(resourceTxt, 0, SWT.LEFT);
        applyToLbl.setLayoutData(fd);

        applyForRscsLViewer = new ListViewer(top_form,
                SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        fd = new FormData(120, 300);
        fd.width = 120;
        fd.top = new FormAttachment(attrSetGroupNameTxt, 35, SWT.BOTTOM);
        fd.left = new FormAttachment(attrSetGroupNameTxt, 0, SWT.LEFT);
        fd.bottom = new FormAttachment(100, -95);
        applyForRscsLViewer.getList().setLayoutData(fd);
        applyForRscsLViewer.getList().setToolTipText(
                "Use <Control> to Multi-Select or <Shift> to select a group.");

        applyForAllRscsBtn = new Button(top_form, SWT.PUSH);
        applyForAllRscsBtn.setText("Apply for All");
        fd = new FormData();
        fd.top = new FormAttachment(applyForRscsLViewer.getList(), 10,
                SWT.BOTTOM);
        fd.left = new FormAttachment(applyForRscsLViewer.getList(), -50,
                SWT.CENTER);
        applyForAllRscsBtn.setLayoutData(fd);

        Label filterLbl = new Label(top_form, SWT.NONE);
        filterLbl.setText("Filter Attribute Sets");
        fd = new FormData();
        fd.left = new FormAttachment(26, 0);
        fd.top = new FormAttachment(0, 22);
        filterLbl.setLayoutData(fd);

        filterTxt = new Text(top_form, SWT.SINGLE | SWT.BORDER);
        filterTxt.setToolTipText(
                "Enter a regular expression using '*' and '?'.");

        fd = new FormData();
        fd.width = 115;
        fd.top = new FormAttachment(filterLbl, -2, SWT.TOP);
        fd.left = new FormAttachment(filterLbl, 5, SWT.RIGHT);
        filterTxt.setLayoutData(fd);

        Button filterBtn = new Button(top_form, SWT.PUSH);
        filterBtn.setText(" Filter ");
        fd = new FormData();
        fd.top = new FormAttachment(filterTxt, 0, SWT.TOP);
        fd.right = new FormAttachment(filterTxt, -20, SWT.LEFT);
        filterBtn.setLayoutData(fd);
        filterBtn.setVisible(false);

        availAttrSetsLViewer = new ListViewer(top_form,
                SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        fd = new FormData();
        fd.top = new FormAttachment(filterTxt, 35, SWT.BOTTOM);
        fd.left = new FormAttachment(26, 0);
        fd.right = new FormAttachment(61, 0);
        fd.bottom = new FormAttachment(100, -95);
        availAttrSetsLViewer.getList().setLayoutData(fd);
        availAttrSetsLViewer.getList().setToolTipText(
                "Use <Control> to Multi-Select or <Shift> to select a group.");

        Label addAttrSetsLbl = new Label(top_form, SWT.NONE);
        addAttrSetsLbl.setText("Available Attribute Sets");
        fd = new FormData();
        fd.bottom = new FormAttachment(availAttrSetsLViewer.getList(), -3,
                SWT.TOP);
        fd.left = new FormAttachment(availAttrSetsLViewer.getList(), 0,
                SWT.LEFT);
        addAttrSetsLbl.setLayoutData(fd);

        seldAttrSetsLViewer = new ListViewer(top_form,
                SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        fd = new FormData();
        fd.top = new FormAttachment(availAttrSetsLViewer.getList(), 0, SWT.TOP);
        fd.left = new FormAttachment(63, 0);
        fd.right = new FormAttachment(98, 0);
        fd.bottom = new FormAttachment(availAttrSetsLViewer.getList(), 0,
                SWT.BOTTOM);
        seldAttrSetsLViewer.getList().setLayoutData(fd);

        Label seldAttrSetsLbl = new Label(top_form, SWT.NONE);
        seldAttrSetsLbl.setText("Selected Attribute Sets In Group");
        fd = new FormData();
        fd.bottom = new FormAttachment(seldAttrSetsLViewer.getList(), -3,
                SWT.TOP);
        fd.left = new FormAttachment(seldAttrSetsLViewer.getList(), 0,
                SWT.LEFT);
        seldAttrSetsLbl.setLayoutData(fd);

        addAttrSetBtn = new Button(top_form, SWT.PUSH);
        addAttrSetBtn.setText("Add ->");
        fd = new FormData();
        fd.width = 100;
        fd.top = new FormAttachment(availAttrSetsLViewer.getList(), 10,
                SWT.BOTTOM);
        fd.left = new FormAttachment(availAttrSetsLViewer.getList(), -50,
                SWT.CENTER);
        addAttrSetBtn.setLayoutData(fd);

        removeAttrSetBtn = new Button(top_form, SWT.PUSH);
        removeAttrSetBtn.setText("<- Remove");
        fd = new FormData();
        fd.width = 100;
        fd.top = new FormAttachment(seldAttrSetsLViewer.getList(), 10,
                SWT.BOTTOM);
        fd.left = new FormAttachment(seldAttrSetsLViewer.getList(), -50,
                SWT.CENTER);
        removeAttrSetBtn.setLayoutData(fd);

        saveAttrSetGroupBtn = new Button(top_form, SWT.PUSH);
        saveAttrSetGroupBtn.setText("Save");
        fd = new FormData();
        fd.width = 100;
        fd.bottom = new FormAttachment(100, -10);
        fd.right = new FormAttachment(100, -30);
        saveAttrSetGroupBtn.setLayoutData(fd);

        newAttrSetGroupBtn = new Button(top_form, SWT.PUSH);
        newAttrSetGroupBtn.setText("Create");
        fd = new FormData();
        fd.width = 100;
        fd.bottom = new FormAttachment(100, -10);
        fd.right = new FormAttachment(100, -30);
        newAttrSetGroupBtn.setLayoutData(fd);

        cancelBtn = new Button(top_form, SWT.PUSH);
        cancelBtn.setText("Cancel");
        fd = new FormData();
        fd.width = 100;
        fd.bottom = new FormAttachment(100, -10);
        fd.right = new FormAttachment(saveAttrSetGroupBtn, -20, SWT.LEFT);
        cancelBtn.setLayoutData(fd);

        applyForRscsLViewer.setContentProvider(stringArrayListContentProvider);

        availAttrSetsLViewer.setContentProvider(stringArrayListContentProvider);
        seldAttrSetsLViewer.setContentProvider(stringArrayListContentProvider);

        applyForRscsLViewer.setInput(availRscsForGroup);
        availAttrSetsLViewer.setInput(availAttrSets);
        seldAttrSetsLViewer.setInput(seldAttrSets);

        applyForRscsLViewer.setComparator(new ViewerComparator() {
            @Override
            public int compare(Viewer viewer, Object e1, Object e2) {
                if (seldRscName.getRscType().equals(e1)) {
                    return -1;
                } else if (seldRscName.getRscType().equals(e2)) {
                    return 1;
                } else {
                    return ((String) e1).compareTo((String) e2);
                }
            }
        });

        // check to see if there is already an ASG for this resource with this
        // name
        // and prompt the user if they want to replace it
        applyForRscsLViewer.addSelectionChangedListener(
                event -> applyForRscsLViewer.getList().select(0));

        availAttrSetsLViewer.setComparator(new ViewerComparator() {

            @Override
            public int category(Object element) {
                return super.category(element);
            }

            @Override
            public int compare(Viewer viewer, Object e1, Object e2) {
                return ((String) e1).compareTo((String) e2);
            }
        });

        // just use the same one since they do the same thing (unless we
        // implemented the category)
        seldAttrSetsLViewer.setComparator(availAttrSetsLViewer.getComparator());

        attrSetGroupNameTxt.addModifyListener(e -> {
            String newTextStr = attrSetGroupNameTxt.getText().trim();

            if (newTextStr.isEmpty()) {
                saveAttrSetGroupBtn.setEnabled(false);
                saveAttrSetGroupBtn.setEnabled(false);
            } else {
                if (seldRscDefn != null) {
                    saveAttrSetGroupBtn.setEnabled(true);

                    // if the name has been changed, the 'save' button acts
                    // as a 'Rename' or Save As
                    //
                    // disable the New button if the name hasn't been
                    // changed.
                    //
                    if (seldAttrSetGroup == null) {
                        saveAttrSetGroupBtn.setEnabled(false);
                        newAttrSetGroupBtn.setEnabled(false);
                    } else if (seldAttrSetGroup.getAttrSetGroupName()
                            .equals(newTextStr)) {
                        saveAttrSetGroupBtn.setEnabled(true);
                        saveAttrSetGroupBtn.setText("Save");

                        newAttrSetGroupBtn.setEnabled(false);
                    } else {
                        saveAttrSetGroupBtn.setEnabled(true);
                        saveAttrSetGroupBtn.setText("Save As");
                        newAttrSetGroupBtn.setEnabled(true);

                        // disable the Save button if the new name already
                        // exists
                        if (rscDefnMngr
                                .getAttrSetGroupNamesForResource(
                                        seldRscDefn.getResourceDefnName())
                                .contains(
                                        attrSetGroupNameTxt.getText().trim())) {
                            saveAttrSetGroupBtn.setEnabled(false);
                        }
                    }
                }
            }
        });

        filterTxt.addModifyListener(e -> {
            String filterExpr = filterTxt.getText().trim();
            try {
                attrSetFilter.setFilterMatcher(
                        new StringMatcher(filterExpr, true, false));

                availAttrSetsLViewer.refresh();
            } catch (IllegalArgumentException iaex) {
                statusHandler.error("StringMatcherSyntaxError: " + filterExpr,
                        iaex);
            }
        });

        ViewerFilter vFilters[] = new ViewerFilter[] { attrSetFilter };

        availAttrSetsLViewer.setFilters(vFilters);

        applyForAllRscsBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                applyForRscsLViewer.getList().selectAll();
            }
        });

        saveAttrSetGroupBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                saveAttrSetGroup();
            }
        });

        newAttrSetGroupBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                saveAttrSetGroup();
            }
        });

        cancelBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                mngrControl.editActionCanceled();
            }
        });

        addAttrSetBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                // get the selected attr sets and move them to the seld list
                StructuredSelection attrSetSels = (StructuredSelection) availAttrSetsLViewer
                        .getSelection();
                Iterator selIter = attrSetSels.iterator();

                while (selIter.hasNext()) {
                    String attrSet = (String) selIter.next();
                    availAttrSets.remove(attrSet);
                    seldAttrSets.add(attrSet);
                }

                availAttrSetsLViewer.setInput(availAttrSets);
                seldAttrSetsLViewer.setInput(seldAttrSets);
                availAttrSetsLViewer.refresh();
                seldAttrSetsLViewer.refresh();
            }
        });

        availAttrSetsLViewer.getList().addListener(SWT.MouseDoubleClick,
                event -> {
                    StructuredSelection attrSetSels = (StructuredSelection) availAttrSetsLViewer
                            .getSelection();
                    String attrSet = (String) attrSetSels.getFirstElement();
                    availAttrSets.remove(attrSet);
                    seldAttrSets.add(attrSet);

                    availAttrSetsLViewer.setInput(availAttrSets);
                    seldAttrSetsLViewer.setInput(seldAttrSets);
                    availAttrSetsLViewer.refresh();
                    seldAttrSetsLViewer.refresh();
                });

        removeAttrSetBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                // get the selected attr sets and move them to the seld list
                StructuredSelection attrSetSels = (StructuredSelection) seldAttrSetsLViewer
                        .getSelection();
                Iterator selIter = attrSetSels.iterator();

                while (selIter.hasNext()) {
                    String attrSet = (String) selIter.next();
                    seldAttrSets.remove(attrSet);
                    availAttrSets.add(attrSet);
                }

                availAttrSetsLViewer.setInput(availAttrSets);
                seldAttrSetsLViewer.setInput(seldAttrSets);
                availAttrSetsLViewer.refresh();
                seldAttrSetsLViewer.refresh();
            }
        });

        seldAttrSetsLViewer.getList().addListener(SWT.MouseDoubleClick,
                event -> {
                    StructuredSelection attrSetSels = (StructuredSelection) seldAttrSetsLViewer
                            .getSelection();
                    String attrSet = (String) attrSetSels.getFirstElement();
                    seldAttrSets.remove(attrSet);
                    availAttrSets.add(attrSet);

                    availAttrSetsLViewer.setInput(availAttrSets);
                    seldAttrSetsLViewer.setInput(seldAttrSets);
                    availAttrSetsLViewer.refresh();
                    seldAttrSetsLViewer.refresh();
                });
    }

    @Override
    public void activate() {
        setVisible(true);
        if (getParent() instanceof Group) {
            ((Group) getParent()).setText(getTitle());
        }
    }

    @Override
    public void copySelectedResource(ResourceName rscName) {
        setSelectedResource(rscName);
        newAttrSetGroupBtn.setVisible(true);
        saveAttrSetGroupBtn.setVisible(false);
        attrSetGroupNameTxt.setEditable(true);
        attrSetGroupNameTxt
                .setBackground(availAttrSetsLViewer.getList().getBackground());

        attrSetGroupNameTxt.setText("CopyOf" + attrSetGroupNameTxt.getText());
        attrSetGroupNameTxt.setSelection(0,
                attrSetGroupNameTxt.getText().length());
        attrSetGroupNameTxt.setFocus();
    }

    @Override
    public void editSelectedResource(ResourceName rscName) {
        setSelectedResource(rscName);
        newAttrSetGroupBtn.setVisible(false);
        saveAttrSetGroupBtn.setVisible(true);

        attrSetGroupNameTxt.setEditable(false);
        attrSetGroupNameTxt.setBackground(getParent().getBackground());
    }

    public void setSelectedResource(ResourceName rscName) {

        seldRscName = rscName;

        attrSetGroupNameTxt.setEditable(false);

        if (seldRscName.getRscGroup().isEmpty()) {
            attrSetGroupNameTxt.setText("");
            return;
        }

        attrSetGroupNameTxt.setText(seldRscName.getRscGroup());

        seldRscDefn = rscDefnMngr.getResourceDefinition(seldRscName);

        if (!seldRscDefn.applyAttrSetGroups()) {

        }

        seldAttrSetGroup = rscDefnMngr.getAttrSetGroupForResource(seldRscName);

        if (seldAttrSetGroup == null) {
            statusHandler.debug("sanity check: can't find AttrSetGroup for "
                    + seldRscName.toString());
            return;
        }

        resourceTxt.setText(seldRscName.getRscType());

        filterTxt.setText("");

        availRscsForGroup = new ArrayList<>(
                rscDefnMngr.getRscTypesForRscImplementation(
                        seldRscDefn.getRscImplementation()));

        seldAttrSets = new ArrayList<>();
        seldAttrSets.addAll(seldAttrSetGroup.getAttrSetNames());

        availAttrSets = new ArrayList<>();
        availAttrSets.addAll(rscDefnMngr.getAvailAttrSetsForRscImpl(
                seldRscDefn.getRscImplementation()));

        for (String as : seldAttrSets) {
            availAttrSets.remove(as);
        }

        applyForRscsLViewer.setInput(availRscsForGroup);

        // select the first entry in the list. This will be
        // seldRscName since it will be sorted to be at the top.
        //
        applyForRscsLViewer.getList().select(0);

        applyForRscsLViewer.refresh();

        availAttrSetsLViewer.setInput(availAttrSets);
        seldAttrSetsLViewer.setInput(seldAttrSets);
        availAttrSetsLViewer.refresh();
        seldAttrSetsLViewer.refresh();
    }

    @Override
    public ResourceName getSelectedResourceName() {
        return seldRscName;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void deactivate() {
        setVisible(false);
        // good to clear out all of the selections even if they can't be seen?
    }

    @Override
    public String getTitle() {
        return "Edit Attribute Set Group";
    }

    private void saveAttrSetGroup() {
        try {
            String attrSetGroupName = attrSetGroupNameTxt.getText().trim();

            List<String> applyToRscsList = Arrays
                    .asList(applyForRscsLViewer.getList().getSelection());

            // If the user want to do this then they need to Remove the ASG
            // explicitly (if in the USER level)
            // if we are removing this group from the currently selected
            // resource
            for (String rscName : applyToRscsList) {

                // create the new AttrSetGroup from the GUI selections.
                AttrSetGroup newAttrSetGroup = rscDefnMngr
                        .getAttrSetGroupForResource(
                                new RscAndGroupName(rscName, attrSetGroupName));

                if (newAttrSetGroup == null) {
                    newAttrSetGroup = new AttrSetGroup();

                    newAttrSetGroup.setAttrSetGroupName(attrSetGroupName);
                    newAttrSetGroup.setResource(rscName);
                } else {
                    // if adding this asg for another resource, first check to
                    // see if the resource
                    // already has an asg with this name and if so confirm that
                    // they really want
                    // to override it.
                    if (!seldAttrSetGroup.getResource().equals(rscName)) {
                        MessageDialog confirmDlg = new MessageDialog(getShell(),
                                "Confirm", null,
                                "The Attribute Set Group " + attrSetGroupName
                                        + " for resource " + rscName
                                        + " already exists.\n\n"
                                        + "Are you sure you want to override it?",
                                MessageDialog.QUESTION,
                                new String[] { "Yes", "No" }, 0);
                        confirmDlg.open();

                        if (confirmDlg
                                .getReturnCode() == MessageDialog.CANCEL) {
                            continue;
                        }
                    }
                }

                // Generate a new list of attribute names for newAttrSetGroup
                // based on the list of seldAttrSets so that the display aliases
                // for the attributes in the old newAttrSetGroup can be
                // preserved after the Manage Resource dialog changes the
                // attribute list.
                AttrSetLabels labels = newAttrSetGroup.getAttrSetLabels();
                ArrayList<AttrSetLabel> labelsList = labels.getLabelList();
                ArrayList<String> asNameList = new ArrayList<>();
                if (labelsList != null) {
                    for (AttrSetLabel label : labelsList) {
                        asNameList.add(label.getName());
                    }
                }
                // Preserve the intersection of "newAttrSetGroup" and
                // "seldAttrSets" by removing the elements that are not in
                // "seldAttrSets" from "newAttrSetGroup".
                for (String asName : asNameList) {
                    if (!seldAttrSets.contains(asName)) {
                        newAttrSetGroup.removeAttrSet(asName);
                    }
                }
                // Augment "newAttrSetGroup" by adding the elements
                // not in the original "newAttrSetGroup" but in "seldAttrSets".
                for (String asName : seldAttrSets) {
                    if (!asNameList.contains(asName)) {
                        newAttrSetGroup.addAttrSetName(asName);
                    }
                }

                rscDefnMngr.saveAttrSetGroup(newAttrSetGroup);
            }

            ResourceName newSeldRscName = new ResourceName();
            newSeldRscName.setRscCategory(seldRscDefn.getResourceCategory());
            newSeldRscName.setRscType(seldRscDefn.getResourceDefnName());
            newSeldRscName.setRscGroup(attrSetGroupName);

            mngrControl.updateResourceSelections(newSeldRscName);

            String msgStr = "Saved Attribute Set Group " + attrSetGroupName
                    + ".";

            MessageDialog saveMsgDlg = new MessageDialog(getShell(), DONE, null,
                    msgStr + "\n\n", MessageDialog.INFORMATION,
                    new String[] { OK }, 0);
            saveMsgDlg.open();

            return;
        } catch (VizException e) {
            statusHandler.error("Error Saving AttrSetGroup", e);
        }
    }
}