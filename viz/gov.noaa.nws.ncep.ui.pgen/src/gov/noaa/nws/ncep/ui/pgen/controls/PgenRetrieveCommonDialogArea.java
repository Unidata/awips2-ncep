/*
 * gov.noaa.nws.ncep.ui.pgen.controls.PgenRetrieveCommonDialogArea
 *
 * 29 Aug 2019
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */
package gov.noaa.nws.ncep.ui.pgen.controls;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.TimeZone;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

/**
 * A helper class to Retrieve PGEN activities from EDEX.
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Aug 27, 2019  67216    ksunil    Initial creation. Code harvested from
 *                                  RetrieveActivityDialog
 * Mar 04, 2020  75757    tjensen   Default to sort by Date; Clean up dead code
 *
 * </pre>
 *
 * @author
 */
public class PgenRetrieveCommonDialogArea {

    /*
     * Used to compare Activity's reference times.
     */
    class ActivityTimeComparator extends ViewerComparator {

        @Override
        public int compare(Viewer viewer, Object e1, Object e2) {
            // Comparing e2 to e1 to get reverse ordering
            return ((ActivityElement) e2).compareTo(e1);
        }

    }

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(this.getClass());

    /**
     * The last selected Site.
     */
    private String lastSelectedSite = null;

    /**
     * The last selected Desk.
     */
    private String lastSelectedDesk = null;

    /**
     * The last selected Type.
     */
    private String lastSelectedType = null;

    /**
     * The last selected Subtype.
     */
    private String lastSelectedSubtype = null;

    /**
     * This is to access auxiliary methods in the class ActivityCollection.
     */
    private final ActivityCollection ac = new ActivityCollection();

    /**
     * The SWT widget for displaying Site.
     */
    private Combo siteFilter = null;

    /**
     * The SWT widget for displaying Desk.
     */
    private Combo deskFilter = null;

    /**
     * The SWT widget for displaying Types.
     */
    private List typeListWidget = null;

    /**
     * The SWT widget for displaying Subtypes.
     */
    private List subtypeListWidget = null;

    /**
     * The SWT widget for displaying Activity labels.
     */
    private ListViewer fileListViewer = null;

    /**
     * The radio buttons for the options of listing "latest" or "all" activity
     * labels.
     */
    private final Button[] listRadioButtons = new Button[2];

    /**
     * The radio buttons for the options of sorting order.
     */
    private final Button[] sortRadioButtons = new Button[2];

    /**
     * The radio buttons for the options of auto saving.
     */
    private final Button[] autoSaveRadioButtons = new Button[2];

    /**
     * The separator between the label and the date string.
     */
    private static final String DATE_STRING_SEPARATOR = "    ";

    /**
     * R7806: This method uses SWT GridLayouts (replacing FormLayout) to glue
     * together the major components, SWT widgets, of the PGEN Open dialog
     * window, which include two pull-down menus for Site and Desk, two SWT
     * lists for Type and Subtype, three sets of two-choice radio buttons, and a
     * SWT list for Activity labels.
     *
     * It relies on the class ActivityCollection to provide the contents of the
     * SWT widgets.
     */
    public Control createComponents(Composite parent) {

        /*
         * The dialog area is organized vertically as four sub-areas.
         *
         * The first vertical sub-area is for two pull-down menus displaying
         * Site and Desk.
         */
        createSiteAndDeskSubArea(parent);

        /*
         * The second vertical sub-area is for two SWT lists displaying Type and
         * Subtype.
         */
        createTypeSubtypeArea(parent);

        // The sub-area is for three sets of two-choice radio buttons
        createChoiceButtonArea(parent);

        // The fourth vertical for a SWT list displaying Activity labels.
        createActivityLabelArea(parent);

        return parent;
    }

    public Button[] getAutoSaveRadioButtons() {
        return autoSaveRadioButtons;
    }

    public ListViewer getFileListViewer() {
        return fileListViewer;
    }

    /*
     * sub-area is for two pull-down menus displaying Site and Desk.
     */
    private void createSiteAndDeskSubArea(Composite parent) {
        Composite headerArea = new Composite(parent, SWT.RESIZE);
        GridLayout menusLayout = new GridLayout();
        menusLayout.numColumns = 2;
        menusLayout.horizontalSpacing = 45;
        headerArea.setLayout(menusLayout);
        GridData headerAreaData = new GridData(
                GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_CENTER);
        headerAreaData.horizontalAlignment = SWT.CENTER;
        headerArea.setLayoutData(headerAreaData);

        Composite superLeftGrid = new Composite(headerArea, SWT.NONE);
        superLeftGrid.setLayout(new GridLayout(2, false));

        Label siteLabel = new Label(superLeftGrid, SWT.NONE);
        siteLabel.setText("Site: ");

        siteFilter = new Combo(superLeftGrid, SWT.READ_ONLY | SWT.DROP_DOWN);

        /*
         * The initial value of Site is set to the last selected value if it
         * exists, or the current site recorded in the system. A change to Site
         * triggers changes to Desk, Type, Subtype, and Activity labels.
         */

        String[] currentSiteList = ac.getCurrentSiteList();
        siteFilter.setItems(currentSiteList);

        if (lastSelectedSite == null) {
            int defaultIndex = ac.getDefaultSiteIndex();
            ac.changeSiteIndex(defaultIndex);
            lastSelectedSite = currentSiteList[defaultIndex];
        } else {
            int index = Arrays.asList(currentSiteList)
                    .indexOf(lastSelectedSite);

            if (index == -1) {
                index = 0;
            }

            ac.changeSiteIndex(index);
            lastSelectedSite = currentSiteList[index];
        }
        siteFilter.select(ac.getCurrentSiteIndex());

        /*
         * A change to Site by users will trigger changes to Desk, Type,
         * Subtype, and Activity labels sequentially and immediately.
         */
        siteFilter.addSelectionListener(new SiteFilterAdapter());
        Composite superRightGrid = new Composite(headerArea, SWT.NONE);
        superRightGrid.setLayout(new GridLayout(2, false));

        Label deskLabel = new Label(superRightGrid, SWT.NONE);
        deskLabel.setText("Desk: ");

        deskFilter = new Combo(superRightGrid, SWT.READ_ONLY | SWT.DROP_DOWN);

        /*
         * The initial value of Desk is set to the last selected value if it
         * exists, or the current desk recorded in the system. A change to Desk
         * triggers changes to Type, Subtype, and Activity labels.
         */
        String[] currentDeskList = ac.getCurrentDeskList();
        deskFilter.setItems(currentDeskList);

        if (lastSelectedDesk == null) {
            int defaultIndex = ac.getDefaultDeskListIndex();
            ac.changeDeskIndex(defaultIndex);
            lastSelectedDesk = currentDeskList[defaultIndex];
        } else {
            int index = Arrays.asList(currentDeskList)
                    .indexOf(lastSelectedDesk);

            if (index == -1) {
                index = 0;
            }

            ac.changeDeskIndex(index);
            lastSelectedDesk = currentDeskList[index];
        }
        deskFilter.select(ac.getCurrentDeskIndex());

        /*
         * A change to Desk by users will trigger changes to Type, Subtype, and
         * Activity labels sequentially and immediately.
         */
        deskFilter.addSelectionListener(new DeskFilterAdapter());

    }

    /*
     * creates sub-area for two SWT lists displaying Type and Subtype.
     */
    private void createTypeSubtypeArea(Composite parent) {
        Composite typeSubtypeArea = new Composite(parent, SWT.RESIZE);
        typeSubtypeArea.setLayout(new GridLayout(2, false));
        GridData typeSubtypeAreaData = new GridData(SWT.FILL, GridData.FILL,
                true, true);
        typeSubtypeArea.setLayoutData(typeSubtypeAreaData);

        Composite leftGrid = new Composite(typeSubtypeArea, SWT.RESIZE);
        leftGrid.setLayout(new GridLayout(1, false));
        GridData midGridLayoutData = new GridData(SWT.FILL, GridData.FILL, true,
                true);
        leftGrid.setLayoutData(midGridLayoutData);

        Label typeLabel = new Label(leftGrid, SWT.NONE);
        typeLabel.setText("Type: ");

        typeListWidget = new List(leftGrid, SWT.SINGLE | SWT.BORDER
                | SWT.V_SCROLL | SWT.H_SCROLL | SWT.RESIZE);
        GridData typeListLayoutData = new GridData(SWT.FILL, SWT.FILL, true,
                true);
        typeListLayoutData.widthHint = 240;
        typeListLayoutData.heightHint = 200;
        typeListWidget.setLayoutData(typeListLayoutData);

        /*
         * The initial value of Type is set to the last selected value if it
         * exists, or the first one. Since the option "All" is not displayed
         * (index 0 internally), index 1 is used to change type.
         */
        String[] currentTypeList = ac.getCurrentTypeList();

        if (lastSelectedType == null) {
            // Hide the "All" option
            if (currentTypeList.length > 1) {
                ac.changeTypeIndex(1);
                lastSelectedType = currentTypeList[1];
            }
        } else {
            int index = Arrays.asList(currentTypeList)
                    .indexOf(lastSelectedType);

            if (index == -1) {
                index = 0;
            }

            ac.changeTypeIndex(index);
            lastSelectedType = currentTypeList[index];
        }

        // Hide the "All" option
        boolean isToSkip = true;
        for (String type : currentTypeList) {
            if (isToSkip) {
                isToSkip = false;
                continue;
            }
            typeListWidget.add(type);
        }
        typeListWidget.select(ac.getCurrentTypeIndex() - 1);

        /*
         * A change to Type will trigger changes to Subtype and Activity labels
         * sequentially and immediately.
         */
        typeListWidget.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {

                int typeIndex = typeListWidget.getSelectionIndex();

                /*
                 * Since the option "All" is not displayed, the external index
                 * (typeListWidget) and the internal index (ActivityCollection)
                 * are off by one.
                 */
                typeIndex += 1;

                ac.changeTypeIndex(typeIndex);
                lastSelectedType = ac.getCurrentTypeList()[typeIndex];

                subtypeListWidget.removeAll();

                String[] subtypeList = ac.getCurrentSubtypeList();

                // Hide the "All" option in subtypes
                boolean isToSkip = true;
                for (String subtype : subtypeList) {
                    if (isToSkip) {
                        isToSkip = false;
                        continue;
                    }
                    subtypeListWidget.add(subtype);
                }

                /*
                 * The default selection is the first one. Since the option
                 * "All" is not displayed (index 0 internally), index 1 is used
                 * to change subtype.
                 */
                if (subtypeList.length > 1) {
                    ac.changeSubtypeIndex(1);
                    lastSelectedSubtype = subtypeList[1];
                }

                subtypeListWidget.select(0);

                listActivities();
            }
        });

        Composite rightGrid = new Composite(typeSubtypeArea, SWT.RESIZE);
        rightGrid.setLayout(new GridLayout(1, false));
        GridData rightGridLayoutData = new GridData(SWT.FILL, GridData.FILL,
                true, true);
        rightGrid.setLayoutData(rightGridLayoutData);

        Label subtypeLabel = new Label(rightGrid, SWT.NONE);
        subtypeLabel.setText("Subtype: ");

        subtypeListWidget = new List(rightGrid, SWT.SINGLE | SWT.BORDER
                | SWT.V_SCROLL | SWT.H_SCROLL | SWT.RESIZE);
        GridData subTypeLayoutData = new GridData(SWT.FILL, SWT.FILL, true,
                true);
        subTypeLayoutData.widthHint = 240;
        subTypeLayoutData.heightHint = 200;
        subtypeListWidget.setLayoutData(subTypeLayoutData);

        /*
         * The initial value of Subtype is set to the last selected value if it
         * exists, or the first one. Since the option "All" is not displayed
         * (index 0 internally), index 1 is used to change subtype.
         */

        String[] currentSubtypeList = ac.getCurrentSubtypeList();

        if (lastSelectedSubtype == null) {
            // Hide the "All" option
            if (currentSubtypeList.length > 1) {
                ac.changeSubtypeIndex(1);
                lastSelectedSubtype = currentSubtypeList[1];
            }
        } else {
            int index = Arrays.asList(currentSubtypeList)
                    .indexOf(lastSelectedSubtype);

            if (index == -1) {
                index = 0;
            }

            ac.changeSubtypeIndex(index);

            lastSelectedSubtype = currentSubtypeList[index];
        }

        // Hide the "All" option
        isToSkip = true;
        for (String subtype : currentSubtypeList) {
            if (isToSkip) {
                isToSkip = false;
                continue;
            }
            subtypeListWidget.add(subtype);
        }

        subtypeListWidget.select(ac.getCurrentSubtypeIndex() - 1);

        // A change to Subtype will trigger a change to Activity labels.
        subtypeListWidget.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                int subtypeIndex = subtypeListWidget.getSelectionIndex();

                /*
                 * Since the option "All" is not displayed, the external index
                 * (typeListWidget) and the internal index (ActivityCollection)
                 * are off by one.
                 */
                subtypeIndex += 1;

                ac.changeSubtypeIndex(subtypeIndex);
                lastSelectedSubtype = ac.getCurrentSubtypeList()[subtypeIndex];

                listActivities();
            }
        });

    }

    /*
     * creates sub-area for three sets of two-choice radio buttons
     */
    private void createChoiceButtonArea(Composite parent) {
        Composite radioButtonsArea = new Composite(parent, SWT.RESIZE);
        GridLayout buttonsLayout = new GridLayout();
        buttonsLayout.numColumns = 3;
        buttonsLayout.horizontalSpacing = 40;
        radioButtonsArea.setLayout(buttonsLayout);
        GridData radioButtonsAreaData = new GridData(
                GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_CENTER);
        radioButtonsAreaData.horizontalAlignment = SWT.CENTER;
        radioButtonsArea.setLayoutData(radioButtonsAreaData);

        Composite listButtonsGrid = new Composite(radioButtonsArea, SWT.NONE);
        listButtonsGrid.setLayout(new GridLayout(2, false));

        Label listLabel = new Label(listButtonsGrid, SWT.NONE);
        listLabel.setText("Activity Labels: ");

        listRadioButtons[0] = new Button(listButtonsGrid, SWT.RADIO);
        listRadioButtons[0].setText("Latest");
        listRadioButtons[0].setSelection(true);

        listRadioButtons[0].addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                listActivities();
            }
        });

        Label emptyLabel1 = new Label(listButtonsGrid, SWT.NONE);
        emptyLabel1.setText("  ");

        listRadioButtons[1] = new Button(listButtonsGrid, SWT.RADIO);
        listRadioButtons[1].setText("All");

        listRadioButtons[1].addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                listActivities();
            }
        });

        Composite sortButtonsGrid = new Composite(radioButtonsArea, SWT.NONE);
        sortButtonsGrid.setLayout(new GridLayout(2, false));

        Label sortLabel = new Label(sortButtonsGrid, SWT.NONE);
        sortLabel.setText("Sort: ");

        sortRadioButtons[0] = new Button(sortButtonsGrid, SWT.RADIO);
        sortRadioButtons[0].setText("Alphabetically");

        sortRadioButtons[0].addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                fileListViewer.setComparator(new ViewerComparator());
                fileListViewer.refresh(true);

            }
        });

        Label emptyLabel2 = new Label(sortButtonsGrid, SWT.NONE);
        emptyLabel2.setText("  ");

        sortRadioButtons[1] = new Button(sortButtonsGrid, SWT.RADIO);
        sortRadioButtons[1].setText("By Date");
        sortRadioButtons[1].setSelection(true);

        sortRadioButtons[1].addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                fileListViewer.setComparator(new ActivityTimeComparator());
                fileListViewer.refresh(true);

            }
        });

        Composite autoSaveGrid = new Composite(radioButtonsArea, SWT.NONE);
        autoSaveGrid.setLayout(new GridLayout(2, false));

        Label autoSaveLbl = new Label(autoSaveGrid, SWT.NONE);
        autoSaveLbl.setText("Auto Save: ");

        autoSaveRadioButtons[0] = new Button(autoSaveGrid, SWT.RADIO);
        autoSaveRadioButtons[0].setText("Off");
        autoSaveRadioButtons[0].setSelection(true);

        Label emptyLabel3 = new Label(autoSaveGrid, SWT.NONE);
        emptyLabel3.setText("  ");

        autoSaveRadioButtons[1] = new Button(autoSaveGrid, SWT.RADIO);
        autoSaveRadioButtons[1].setText("On");

    }

    /*
     * creates subarea for SWT list displaying Activity labels.
     */
    private void createActivityLabelArea(Composite parent) {
        fileListViewer = new ListViewer(parent, SWT.SINGLE | SWT.BORDER
                | SWT.V_SCROLL | SWT.H_SCROLL | SWT.RESIZE);
        GridData fileListLayoutData = new GridData(SWT.FILL, SWT.FILL, true,
                true);
        fileListLayoutData.widthHint = 500;
        fileListLayoutData.heightHint = 200;
        fileListViewer.getList().setLayoutData(fileListLayoutData);

        fileListViewer.setContentProvider(ArrayContentProvider.getInstance());
        if (sortRadioButtons[0].getSelection()) {
            fileListViewer.setComparator(new ViewerComparator());
        } else {
            fileListViewer.setComparator(new ActivityTimeComparator());
        }

        fileListViewer.setLabelProvider(new LabelProvider() {

            @Override
            public String getText(Object element) {
                String label = super.getText(element);
                if (element instanceof ActivityElement) {
                    label = ((ActivityElement) element).getActivityLabel();
                }
                return label;
            }

        });

        fileListViewer
                .addSelectionChangedListener(new ISelectionChangedListener() {
                    @Override
                    public void selectionChanged(SelectionChangedEvent event) {
                        IStructuredSelection selection = (IStructuredSelection) event
                                .getSelection();
                        if (selection
                                .getFirstElement() instanceof ActivityElement) {
                            ActivityElement elem = (ActivityElement) selection
                                    .getFirstElement();
                            fileListViewer.getList()
                                    .setToolTipText(elem.getDataURI());
                        } else {
                            statusHandler.debug(
                                    "GOT??? " + selection.getFirstElement()
                                            .getClass().getCanonicalName());
                        }
                    }
                });

        listActivities();
    }

    private class SiteFilterAdapter extends SelectionAdapter {
        @Override
        public void widgetSelected(SelectionEvent e) {

            int siteIndex = siteFilter.getSelectionIndex();
            ac.changeSiteIndex(siteIndex);
            lastSelectedSite = ac.getCurrentSiteList()[siteIndex];

            String[] currentDeskList = ac.getCurrentDeskList();
            int currentDeskIndex = ac.getCurrentDeskIndex();
            deskFilter.setItems(currentDeskList);
            deskFilter.select(currentDeskIndex);
            lastSelectedDesk = currentDeskList[currentDeskIndex];

            typeListWidget.removeAll();
            subtypeListWidget.removeAll();

            String[] typeList = ac.getCurrentTypeList();
            // Hide the "All" option
            boolean isToSkip = true;
            for (String type : typeList) {
                if (isToSkip) {
                    isToSkip = false;
                    continue;
                }
                typeListWidget.add(type);
            }

            /*
             * The default selection is the first one. Since the option "All" is
             * not displayed (index 0 internally), index 1 is used to change
             * type.
             */
            if (typeList.length > 1) {
                ac.changeTypeIndex(1);
                lastSelectedType = typeList[1];
            }

            typeListWidget.select(0);

            String[] subtypeList = ac.getCurrentSubtypeList();
            // Hide the "All" option
            isToSkip = true;
            for (String subtype : subtypeList) {
                if (isToSkip) {
                    isToSkip = false;
                    continue;
                }
                subtypeListWidget.add(subtype);
            }

            /*
             * The default selection is the first one. Since the option "All" is
             * not displayed (index 0 internally), index 1 is used to change
             * subtype.
             */
            if (subtypeList.length > 1) {
                ac.changeSubtypeIndex(1);
                lastSelectedSubtype = subtypeList[1];
            }
            subtypeListWidget.select(0);

            listActivities();
        }
    }

    private class DeskFilterAdapter extends SelectionAdapter {
        @Override
        public void widgetSelected(SelectionEvent e) {

            int deskIndex = deskFilter.getSelectionIndex();
            ac.changeDeskIndex(deskIndex);
            lastSelectedDesk = ac.getCurrentDeskList()[deskIndex];

            typeListWidget.removeAll();
            subtypeListWidget.removeAll();

            String[] typeList = ac.getCurrentTypeList();

            // Hide the "All" option
            boolean isToSkip = true;
            for (String type : typeList) {
                if (isToSkip) {
                    isToSkip = false;
                    continue;
                }
                typeListWidget.add(type);
            }

            /*
             * The default selection is the first one. Since the option "All" is
             * not displayed (index 0 internally), index 1 is used to change
             * type.
             */
            if (typeList.length > 1) {
                ac.changeTypeIndex(1);
                lastSelectedType = typeList[1];
            }
            typeListWidget.select(0);

            String[] subtypeList = ac.getCurrentSubtypeList();
            // Hide the "All" option
            isToSkip = true;
            for (String subtype : subtypeList) {
                if (isToSkip) {
                    isToSkip = false;
                    continue;
                }
                subtypeListWidget.add(subtype);
            }

            /*
             * The default selection is the first one. Since the option "All" is
             * not displayed (index 0 internally), index 1 is used to change
             * subtype.
             */
            if (subtypeList.length > 1) {
                ac.changeSubtypeIndex(1);
                lastSelectedSubtype = subtypeList[1];
            }
            subtypeListWidget.select(0);
            listActivities();
        }
    }

    /*
     * common utility to filter out the activity list elements and display
     */
    private void listActivities() {

        boolean listLatest = listRadioButtons[0].getSelection();
        if (typeListWidget.getSelectionCount() > 0) {

            java.util.List<ActivityElement> elems = ac.getCurrentActivityList();
            java.util.List<ActivityElement> filterElms = new ArrayList<>();

            if (!elems.isEmpty()) {
                if (listLatest) {

                    // Sort all entries based on time, latest first.
                    Collections.sort(elems, Collections.reverseOrder());

                    // Remove time stamps resulting from "All"
                    java.util.List<String> actLbls = new ArrayList<>();
                    for (ActivityElement ae : elems) {
                        int indx = ae.getActivityLabel()
                                .indexOf(DATE_STRING_SEPARATOR);
                        if (indx >= 0) {
                            ae.setActivityLabel(
                                    ae.getActivityLabel().substring(0, indx));
                        }
                    }

                    // Pick unique labels.
                    for (ActivityElement ae : elems) {
                        if (!actLbls.contains(ae.getActivityLabel())) {
                            actLbls.add(ae.getActivityLabel());
                            filterElms.add(ae);
                        }
                    }
                } else {
                    /*
                     * For activities that have the same label but different
                     * ref. time, affix the ref. time at the end to
                     * differentiate them.
                     */
                    for (ActivityElement ae : elems) {
                        boolean attachReftime = false;
                        for (ActivityElement ae1 : elems) {
                            if (ae1 != ae) {
                                String aLbl = ae1.getActivityLabel();
                                int loc = aLbl.indexOf(DATE_STRING_SEPARATOR);
                                if (loc >= 0) {
                                    aLbl = aLbl.substring(0, loc);
                                }

                                if (ae.getActivityLabel().equals(aLbl)) {
                                    attachReftime = true;
                                    break;
                                }
                            }
                        }

                        // Note, we are using "GMT" time zone when we save.
                        if (attachReftime) {
                            DateFormat fmt = new SimpleDateFormat(
                                    "yyyy-MM-dd   HH:mm:ss");
                            fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
                            ae.setActivityLabel(ae.getActivityLabel()
                                    + DATE_STRING_SEPARATOR
                                    + fmt.format(ae.getRefTime()));
                        }

                    }

                    filterElms.addAll(elems);
                }
            }

            fileListViewer.setInput(filterElms);
            fileListViewer.getList().setToolTipText(null);
            fileListViewer.refresh();
        }
    }

}
