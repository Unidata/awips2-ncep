/*
 * gov.noaa.nws.ncep.ui.pgen.controls.RetrieveActivityDialog
 * 
 * 29 March 2013
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */
package gov.noaa.nws.ncep.ui.pgen.controls;

import gov.noaa.nws.ncep.ui.pgen.PgenSession;
import gov.noaa.nws.ncep.ui.pgen.PgenUtil;
import gov.noaa.nws.ncep.ui.pgen.elements.AbstractDrawableComponent;
import gov.noaa.nws.ncep.ui.pgen.elements.Jet;
import gov.noaa.nws.ncep.ui.pgen.elements.Layer;
import gov.noaa.nws.ncep.ui.pgen.elements.Product;
import gov.noaa.nws.ncep.ui.pgen.rsc.PgenResource;
import gov.noaa.nws.ncep.ui.pgen.sigmet.VaaInfo;
import gov.noaa.nws.ncep.ui.pgen.store.PgenStorageException;
import gov.noaa.nws.ncep.ui.pgen.store.StorageUtils;
import gov.noaa.nws.ncep.ui.pgen.tools.PgenSnapJet;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.viz.ui.dialogs.CaveJFACEDialog;

/**
 * A dialog to Retrieve PGEN activities from EDEX.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date       	Ticket#		Engineer	Description
 * ------------	----------	-----------	-----------------------------------
 * 03/13		#977		S.gilbert	Modified from PgenFileManageDialog1
 * 01/2014      #1105       jwu         Use "subtype" for query as well.
 * 08/2014      TTR867      jwu         Add "time stamp" for activities with same label.
 * 08/2014      ?           jwu         Preserve "outputFile" name when opening an activity.
 * 06/24/2015   R7806       A. Su       Added two pull-down menus (site & desk), and a list (subtype).
 *                                      Rearranged three sets of two-choice radio buttons.
 *                                      Implemented new logic for selecting activity labels.
 * 
 * </pre>
 * 
 * @author
 * @version 1
 */
public class RetrieveActivityDialog extends CaveJFACEDialog {

    /*
     * Internal class used to hold some characteristics of an Activity
     */
    static class ActivityElement {
        String site;

        String desk;

        String dataURI;

        String activityType;

        String activitySubtype;

        String activityLabel;

        Date refTime;
    }

    /*
     * Used to compare Activity's reference times.
     */
    class ActivityTimeComparator extends ViewerComparator {

        @Override
        public int compare(Viewer viewer, Object e1, Object e2) {
            Date elem1 = ((ActivityElement) e1).refTime;
            Date elem2 = ((ActivityElement) e2).refTime;
            return -1 * elem1.compareTo(elem2); // multiply by -1 to reverse
                                                // ordering
        }

    }

    /**
     * R7806: This flag determines if the options "All" is hidden in the
     * pull-down menu for Type.
     */
    private final static boolean isToHideOptionAllInType = true;

    /**
     * R7806: This flag determines if the options "All" is hidden in the
     * pull-down menu for Subtype.
     */
    private final static boolean isToHideOptionAllInSubtype = true;

    /**
     * The initial value for the width of the area that display Activity labels.
     */
    private final static int widthForListArea = 500;

    /**
     * The initial values for the widths of the areas that display Types and
     * Subtypes.
     */
    private final static int widthForListArea2 = 240;

    /**
     * The height of the area that displays Activity labels.
     */
    private final static int heightForListArea = 200;

    /**
     * The last selected Site.
     */
    private static String lastSelectedSite = null;

    /**
     * The last selected Desk.
     */
    private static String lastSelectedDesk = null;

    /**
     * The last selected Type.
     */
    private static String lastSelectedType = null;

    /**
     * The last selected Subtype.
     */
    private static String lastSelectedSubtype = null;

    /**
     * This is to access auxiliary methods in the class ActivityCollection.
     */
    private final ActivityCollection ac = new ActivityCollection();

    /**
     * The title of this dialog window.
     */
    private String title = null;

    /**
     * The SWT Shell, a window.
     */
    private Shell shell;

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
    private Button[] listRadioButtons = new Button[2];

    /**
     * The radio buttons for the options of sorting order.
     */
    private Button[] sortRadioButtons = new Button[2];

    /**
     * The radio buttons for the options of auto saving.
     */
    private Button[] autoSaveRadioButtons = new Button[2];

    private static final int ADD_ID = IDialogConstants.CLIENT_ID + 7587;

    private static final String ADD_LABEL = "Add";

    private static final int REPLACE_ID = IDialogConstants.CLIENT_ID + 7586;

    private static final String REPLACE_LABEL = "Replace";

    private static final int ADVANCE_ID = IDialogConstants.CLIENT_ID + 7588;

    private static final String ADVANCE_LABEL = "Advanced";

    private static final int CLOSE_ID = IDialogConstants.CLIENT_ID + 7590;

    private static final String CLOSE_LABEL = "Close";

    private Button replaceBtn = null;

    private Button addBtn = null;

    private Button appendBtn = null;

    private Button cancelBtn = null;

    private static String fullName = null;

    /*
     * Constructor
     */
    public RetrieveActivityDialog(Shell parShell, String btnName)
            throws VizException {

        super(parShell);

        setTitle(btnName);

    }

    /*
     * Set up the file mode.
     */
    private void setTitle(String btnName) {

        if (btnName.equals("Open")) {
            title = "Retrieve a PGEN Activity";

            // To make this dialog window resizable.
            setShellStyle(getShellStyle() | SWT.RESIZE);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets
     * .Shell)
     */
    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        this.setShellStyle(SWT.RESIZE | SWT.PRIMARY_MODAL);

        this.shell = shell;
        if (title != null) {
            shell.setText(title);
        }
    }

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
    @Override
    public Control createDialogArea(Composite parent) {

        Composite dialogArea = (Composite) super.createDialogArea(parent);
        dialogArea.setLayout(new GridLayout(1, false));

        // The dialog area is organized vertically as four sub-areas.
        //
        // The first vertical sub-area is for two pull-down menus displaying
        // Site and Desk.
        Composite headerArea = new Composite(dialogArea, SWT.RESIZE);
        GridLayout menusLayout = new GridLayout();
        menusLayout.numColumns = 2;
        menusLayout.horizontalSpacing = 45;
        headerArea.setLayout(menusLayout);
        GridData headerAreaData = new GridData(GridData.HORIZONTAL_ALIGN_END
                | GridData.VERTICAL_ALIGN_CENTER);
        headerAreaData.horizontalAlignment = SWT.CENTER;
        headerArea.setLayoutData(headerAreaData);

        Composite superLeftGrid = new Composite(headerArea, SWT.NONE);
        superLeftGrid.setLayout(new GridLayout(2, false));

        Label siteLabel = new Label(superLeftGrid, SWT.NONE);
        siteLabel.setText("Site: ");

        siteFilter = new Combo(superLeftGrid, SWT.READ_ONLY | SWT.DROP_DOWN);

        // The initial value of Site is set to the last selected value if it
        // exists, or the current site recorded in the system.
        // A change to Site triggers changes to Desk, Type, Subtype, and
        // Activity labels.

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

        // A change to Site by users will trigger changes to Desk, Type,
        // Subtype, and Activity labels sequentially and immediately.
        siteFilter.addSelectionListener(new SelectionAdapter() {
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
                if (isToHideOptionAllInType) {
                    boolean isToSkip = true;

                    for (String type : typeList) {
                        if (isToSkip) {
                            isToSkip = false;
                            continue;
                        }
                        typeListWidget.add(type);
                    }

                    // The default selection is the first one.
                    // Since the option "All" is not displayed (index 0
                    // internally), index 1 is used to change type.
                    if (typeList.length > 1) {
                        ac.changeTypeIndex(1);
                        lastSelectedType = typeList[1];
                    }
                } else {
                    for (String type : typeList) {
                        typeListWidget.add(type);
                    }
                    lastSelectedType = typeList[0];
                }
                typeListWidget.select(0);

                String[] subtypeList = ac.getCurrentSubtypeList();
                if (isToHideOptionAllInSubtype) {
                    boolean isToSkip = true;

                    for (String subtype : subtypeList) {
                        if (isToSkip) {
                            isToSkip = false;
                            continue;
                        }
                        subtypeListWidget.add(subtype);
                    }

                    // The default selection is the first one.
                    // Since the option "All" is not displayed (index 0
                    // internally), index 1 is used to change subtype.
                    if (subtypeList.length > 1) {
                        ac.changeSubtypeIndex(1);
                        lastSelectedSubtype = subtypeList[1];
                    }
                } else {
                    for (String subtype : subtypeList) {
                        subtypeListWidget.add(subtype);
                    }
                    lastSelectedSubtype = subtypeList[0];
                }
                subtypeListWidget.select(0);

                listActivities();
            }
        });

        Composite superRightGrid = new Composite(headerArea, SWT.NONE);
        superRightGrid.setLayout(new GridLayout(2, false));

        Label deskLabel = new Label(superRightGrid, SWT.NONE);
        deskLabel.setText("Desk: ");

        deskFilter = new Combo(superRightGrid, SWT.READ_ONLY | SWT.DROP_DOWN);

        // The initial value of Desk is set to the last selected value if it
        // exists, or the current desk recorded in the system.
        // A change to Desk triggers changes to Type, Subtype, and Activity
        // labels.
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

        // A change to Desk by users will trigger changes to Type, Subtype, and
        // Activity labels sequentially and immediately.
        deskFilter.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {

                int deskIndex = deskFilter.getSelectionIndex();
                ac.changeDeskIndex(deskIndex);
                lastSelectedDesk = ac.getCurrentDeskList()[deskIndex];

                typeListWidget.removeAll();
                subtypeListWidget.removeAll();

                String[] typeList = ac.getCurrentTypeList();
                if (isToHideOptionAllInType) {

                    boolean isToSkip = true;

                    for (String type : typeList) {
                        if (isToSkip) {
                            isToSkip = false;
                            continue;
                        }
                        typeListWidget.add(type);
                    }

                    // The default selection is the first one.
                    // Since the option "All" is not displayed (index 0
                    // internally), index 1 is used to change type.
                    if (typeList.length > 1) {
                        ac.changeTypeIndex(1);
                        lastSelectedType = typeList[1];
                    }
                } else {
                    for (String type : typeList) {
                        typeListWidget.add(type);
                    }
                    lastSelectedType = typeList[0];
                }
                typeListWidget.select(0);

                String[] subtypeList = ac.getCurrentSubtypeList();
                if (isToHideOptionAllInSubtype) {
                    boolean isToSkip = true;

                    for (String subtype : subtypeList) {
                        if (isToSkip) {
                            isToSkip = false;
                            continue;
                        }
                        subtypeListWidget.add(subtype);
                    }

                    // The default selection is the first one.
                    // Since the option "All" is not displayed (index 0
                    // internally), index 1 is used to change subtype.
                    if (subtypeList.length > 1) {
                        ac.changeSubtypeIndex(1);
                        lastSelectedSubtype = subtypeList[1];
                    }
                } else {
                    for (String subtype : subtypeList) {
                        subtypeListWidget.add(subtype);
                    }
                    lastSelectedSubtype = subtypeList[0];
                }
                subtypeListWidget.select(0);

                listActivities();
            }
        });

        // The second vertical sub-area is for two SWT lists displaying Type and
        // Subtype.
        Composite typeSubtypeArea = new Composite(dialogArea, SWT.RESIZE);
        typeSubtypeArea.setLayout(new GridLayout(2, false));
        GridData typeSubtypeAreaData = new GridData(SWT.FILL, GridData.FILL,
                true, true);
        typeSubtypeArea.setLayoutData(typeSubtypeAreaData);

        Composite leftGrid = new Composite(typeSubtypeArea, SWT.RESIZE);
        leftGrid.setLayout(new GridLayout(1, false));
        GridData midGridLayoutData = new GridData(SWT.FILL, GridData.FILL,
                true, true);
        leftGrid.setLayoutData(midGridLayoutData);

        Label typeLabel = new Label(leftGrid, SWT.NONE);
        typeLabel.setText("Type: ");

        typeListWidget = new List(leftGrid, SWT.SINGLE | SWT.BORDER
                | SWT.V_SCROLL | SWT.H_SCROLL | SWT.RESIZE);
        GridData typeListLayoutData = new GridData(SWT.FILL, SWT.FILL, true,
                true);
        typeListLayoutData.widthHint = widthForListArea2;
        typeListLayoutData.heightHint = heightForListArea;
        typeListWidget.setLayoutData(typeListLayoutData);

        // The initial value of Type is set to the last selected value if it
        // exists, or the first one.
        // Since the option "All" is not
        // displayed (index 0 internally), index 1 is used to change type.
        String[] currentTypeList = ac.getCurrentTypeList();

        if (lastSelectedType == null) {
            if (isToHideOptionAllInType) {
                if (currentTypeList.length > 1) {
                    ac.changeTypeIndex(1);
                    lastSelectedType = currentTypeList[1];
                }
            } else {
                lastSelectedType = currentTypeList[0];
            }
        } else {
            int index = Arrays.asList(currentTypeList)
                    .indexOf(lastSelectedType);

            if (index == -1)
                index = 0;

            ac.changeTypeIndex(index);
            lastSelectedType = currentTypeList[index];
        }

        if (isToHideOptionAllInType) {
            boolean isToSkip = true;

            for (String type : currentTypeList) {
                if (isToSkip) {
                    isToSkip = false;
                    continue;
                }
                typeListWidget.add(type);
            }
            typeListWidget.select(ac.getCurrentTypeIndex() - 1);
        } else {
            for (String type : currentTypeList) {
                typeListWidget.add(type);
            }
            typeListWidget.select(ac.getCurrentTypeIndex());
        }

        // A change to Type will trigger changes to Subtype and Activity labels
        // sequentially and immediately.
        typeListWidget.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {

                int typeIndex = typeListWidget.getSelectionIndex();

                // Since the option "All" is not displayed, the external index
                // (typeListWidget) and the internal index (ActivityCollection)
                // are off by one.
                if (isToHideOptionAllInType) {
                    typeIndex += 1;
                }

                ac.changeTypeIndex(typeIndex);
                lastSelectedType = ac.getCurrentTypeList()[typeIndex];

                subtypeListWidget.removeAll();

                String[] subtypeList = ac.getCurrentSubtypeList();

                if (isToHideOptionAllInSubtype) {
                    boolean isToSkip = true;

                    for (String subtype : subtypeList) {
                        if (isToSkip) {
                            isToSkip = false;
                            continue;
                        }
                        subtypeListWidget.add(subtype);
                    }

                    // The default selection is the first one.
                    // Since the option "All" is not displayed (index 0
                    // internally), index 1 is used to change subtype.
                    if (subtypeList.length > 1) {
                        ac.changeSubtypeIndex(1);
                        lastSelectedSubtype = subtypeList[1];
                    }
                } else {
                    for (String subtype : subtypeList) {
                        subtypeListWidget.add(subtype);
                    }
                    lastSelectedSubtype = subtypeList[0];
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
        subTypeLayoutData.widthHint = widthForListArea2;
        subTypeLayoutData.heightHint = heightForListArea;
        subtypeListWidget.setLayoutData(subTypeLayoutData);

        // The initial value of Subtype is set to the last selected value if it
        // exists, or the first one.
        // Since the option "All" is not
        // displayed (index 0 internally), index 1 is used to change subtype.

        String[] currentSubtypeList = ac.getCurrentSubtypeList();

        if (lastSelectedSubtype == null) {
            if (isToHideOptionAllInSubtype) {
                if (currentSubtypeList.length > 1) {
                    ac.changeSubtypeIndex(1);
                    lastSelectedSubtype = currentSubtypeList[1];
                }
            } else {
                lastSelectedSubtype = currentSubtypeList[0];
            }
        } else {
            int index = Arrays.asList(currentSubtypeList).indexOf(
                    lastSelectedSubtype);

            if (index == -1) {
                index = 0;
            }

            ac.changeSubtypeIndex(index);

            lastSelectedSubtype = currentSubtypeList[index];
        }

        if (isToHideOptionAllInSubtype) {
            boolean isToSkip = true;

            for (String subtype : currentSubtypeList) {
                if (isToSkip) {
                    isToSkip = false;
                    continue;
                }
                subtypeListWidget.add(subtype);
            }

            subtypeListWidget.select(ac.getCurrentSubtypeIndex() - 1);
        } else {
            for (String subtype : currentSubtypeList) {
                subtypeListWidget.add(subtype);
            }
            subtypeListWidget.select(ac.getCurrentSubtypeIndex());
        }

        // A change to Subtype will trigger a change to Activity labels.
        subtypeListWidget.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                int subtypeIndex = subtypeListWidget.getSelectionIndex();

                // Since the option "All" is not displayed, the external index
                // (typeListWidget) and the internal index (ActivityCollection)
                // are off by one.
                if (isToHideOptionAllInSubtype) {
                    subtypeIndex += 1;
                }

                ac.changeSubtypeIndex(subtypeIndex);
                lastSelectedSubtype = ac.getCurrentSubtypeList()[subtypeIndex];

                listActivities();
            }
        });

        // The third vertical sub-area is for three sets of two-choice radio
        // buttons
        Composite radioButtonsArea = new Composite(dialogArea, SWT.RESIZE);
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
            public void widgetSelected(SelectionEvent ev) {
                listActivities();
            }
        });

        Label emptyLabel1 = new Label(listButtonsGrid, SWT.NONE);
        emptyLabel1.setText("  ");

        listRadioButtons[1] = new Button(listButtonsGrid, SWT.RADIO);
        listRadioButtons[1].setText("All");

        listRadioButtons[1].addSelectionListener(new SelectionAdapter() {
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
        sortRadioButtons[0].setSelection(true);

        sortRadioButtons[0].addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent ev) {
                fileListViewer.setComparator(new ViewerComparator());
                fileListViewer.refresh(true);

            }
        });

        Label emptyLabel2 = new Label(sortButtonsGrid, SWT.NONE);
        emptyLabel2.setText("  ");

        sortRadioButtons[1] = new Button(sortButtonsGrid, SWT.RADIO);
        sortRadioButtons[1].setText("By Date");

        sortRadioButtons[1].addSelectionListener(new SelectionAdapter() {
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

        // The fourth vertical sub-area is for a SWT list displaying Activity
        // labels.
        fileListViewer = new ListViewer(dialogArea, SWT.SINGLE | SWT.BORDER
                | SWT.V_SCROLL | SWT.H_SCROLL | SWT.RESIZE);
        GridData fileListLayoutData = new GridData(SWT.FILL, SWT.FILL, true,
                true);
        fileListLayoutData.widthHint = widthForListArea;
        fileListLayoutData.heightHint = heightForListArea;
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
                if (element instanceof ActivityElement)
                    return ((ActivityElement) element).activityLabel;
                else
                    return super.getText(element);
            }

        });

        fileListViewer
                .addSelectionChangedListener(new ISelectionChangedListener() {
                    public void selectionChanged(SelectionChangedEvent event) {
                        IStructuredSelection selection = (IStructuredSelection) event
                                .getSelection();
                        if (selection.getFirstElement() instanceof ActivityElement) {
                            ActivityElement elem = (ActivityElement) selection
                                    .getFirstElement();
                            fileListViewer.getList().setToolTipText(
                                    elem.dataURI);
                        } else
                            System.out.println("GOT??? "
                                    + selection.getFirstElement().getClass()
                                            .getCanonicalName());
                    }
                });

        listActivities();

        return dialogArea;
    }

    /**
     * Create Replace/Append/Cancel button for "Open" a product file or
     * Save/Cancel button for "Save" a product file.
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {

        addBtn = createButton(parent, ADD_ID, ADD_LABEL, true);
        replaceBtn = createButton(parent, REPLACE_ID, REPLACE_LABEL, true);
        appendBtn = createButton(parent, ADVANCE_ID, ADVANCE_LABEL, true);

        replaceBtn.addListener(SWT.MouseUp, new Listener() {
            public void handleEvent(Event event) {
                openProducts(true);
            }
        });

        addBtn.addListener(SWT.MouseUp, new Listener() {
            public void handleEvent(Event event) {
                openProducts(false);
            }
        });

        appendBtn.addListener(SWT.MouseUp, new Listener() {
            public void handleEvent(Event event) {
                appendProducts();
            }
        });

        cancelBtn = createButton(parent, CLOSE_ID, CLOSE_LABEL, true);
        cancelBtn.addListener(SWT.MouseUp, new Listener() {
            public void handleEvent(Event event) {
                close();
            }
        });

    }

    /**
     * Retrieve an activity to replace or append to the current product list in
     * the current PGEN session.
     */
    private void openProducts(boolean replace) {

        ActivityElement elem = getActivitySelection();
        if (elem == null) {
            return;
        }
        fullName = elem.activityLabel;

        java.util.List<Product> pgenProds = null;
        try {
            pgenProds = StorageUtils.retrieveProduct(elem.dataURI);
        } catch (PgenStorageException e) {
            StorageUtils.showError(e);
        }

        /*
         * some Volcano Products are pure texts: TEST/RESUME and cannot be
         * drawn.
         */
        if (VaaInfo.isNoneDrawableTxt(pgenProds)) {
            VaaInfo.openMsgDlg(VaaInfo.NONE_DRAWABLE_MSG);
            return;
        }

        /*
         * Confirm the action
         */
        PgenResource pgen = PgenSession.getInstance().getPgenResource();

        // Force all product/layer display onOff flag to be false at the start.
        /*
         * for ( gov.noaa.nws.ncep.ui.pgen.elements.Product prd : pgenProds ) {
         * prd.setOnOff( false ); for ( gov.noaa.nws.ncep.ui.pgen.elements.Layer
         * lyr : prd.getLayers() ) { lyr.setOnOff( false ); } }
         */
        if (replace) {
            MessageDialog confirmOpen = new MessageDialog(PlatformUI
                    .getWorkbench().getActiveWorkbenchWindow().getShell(),
                    "Confirm File Replace", null, "Replace Activity <"
                            + pgen.getActiveProduct().getType()
                            + "> with New Activity <"
                            + pgenProds.get(0).getType() + "> ?",
                    MessageDialog.INFORMATION, new String[] { "Yes", "No" }, 0);

            confirmOpen.open();

            if (confirmOpen.getReturnCode() != MessageDialog.OK) {
                return;
            }

        }

        pgen.setAutosave(autoSaveRadioButtons[1].getSelection());
        if (fullName.endsWith(".lpf")) {
            pgen.setAutoSaveFilename(fullName.replace(".lpf", "xml"));
        } else {
            pgen.setAutoSaveFilename(fullName);
        }

        pgenProds.get(0).setInputFile(fullName);

        this.setJetTool(pgenProds);

        close();

        /*
         * Replace the active product or add the product to the end
         */
        if (replace) {
            // Reset the output file name.
            // for (gov.noaa.nws.ncep.ui.pgen.elements.Product pp : pgenProds) {
            // pp.setOutputFile(null);
            // }

            PgenFileNameDisplay.getInstance().setFileName(fullName);
            pgen.replaceProduct(pgenProds);
        } else {

            if (pgen.getActiveProduct() == null
                    || pgen.removeEmptyDefaultProduct()) {
                PgenFileNameDisplay.getInstance().setFileName(fullName);
            }

            pgen.addProduct(pgenProds);
        }

        PgenUtil.refresh();

    }

    /**
     * Append the products in a file with those in the current product list.
     */
    private void appendProducts() {

        ActivityElement elem = getActivitySelection();
        if (elem == null) {
            return;
        }
        fullName = elem.activityLabel;

        java.util.List<Product> pgenProds = null;
        try {
            pgenProds = StorageUtils.retrieveProduct(elem.dataURI);
        } catch (PgenStorageException e) {
            StorageUtils.showError(e);
        }

        /*
         * some Volcano Products are pure texts: TEST/RESUME and cannot be
         * drawn.
         */
        if (VaaInfo.isNoneDrawableTxt(pgenProds)) {
            VaaInfo.openMsgDlg(VaaInfo.NONE_DRAWABLE_MSG);
            return;
        }

        PgenResource pgen = PgenSession.getInstance().getPgenResource();

        PgenLayerMergeDialog layerMergeDlg = null;
        try {
            layerMergeDlg = new PgenLayerMergeDialog(shell, pgenProds.get(0),
                    fullName);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (layerMergeDlg != null) {

            layerMergeDlg.open();
            if (layerMergeDlg.getReturnCode() == MessageDialog.OK) {

                pgen.setAutosave(autoSaveRadioButtons[1].getSelection());
                if (fullName.endsWith(".lpf")) {
                    pgen.setAutoSaveFilename(fullName.replace(".lpf", "xml"));
                } else {
                    pgen.setAutoSaveFilename(fullName);
                }

                this.setJetTool(pgenProds);

                close();

                pgen.getResourceData().startProductManage();
            }
        }

    }

    private ActivityElement getActivitySelection() {
        ActivityElement elem = null;

        if (!fileListViewer.getSelection().isEmpty()) {
            IStructuredSelection sel = (IStructuredSelection) fileListViewer
                    .getSelection();
            elem = (ActivityElement) sel.getFirstElement();
        } else {

            MessageDialog confirmDlg = new MessageDialog(PlatformUI
                    .getWorkbench().getActiveWorkbenchWindow().getShell(),
                    "Invalid PGEN Selection", null,
                    "Please select an Activity from the Activity Label list.",
                    MessageDialog.INFORMATION, new String[] { "OK" }, 0);

            confirmDlg.open();

            return null;
        }

        return elem;
    }

    /**
     * Sets the jet snap tool in order to zoom tghe jet correctly.
     * 
     * @param prods
     */
    private void setJetTool(
            java.util.List<gov.noaa.nws.ncep.ui.pgen.elements.Product> prods) {

        PgenSnapJet st = new PgenSnapJet(PgenSession.getInstance()
                .getPgenResource().getDescriptor(), PgenUtil.getActiveEditor(),
                null);

        for (gov.noaa.nws.ncep.ui.pgen.elements.Product prod : prods) {
            for (Layer layer : prod.getLayers()) {

                Iterator<AbstractDrawableComponent> iterator = layer
                        .getComponentIterator();
                while (iterator.hasNext()) {
                    AbstractDrawableComponent adc = iterator.next();
                    if (adc instanceof Jet) {
                        ((Jet) adc).setSnapTool(st);
                        // st.snapJet((Jet)adc);
                    }
                }
            }
        }

    }

    /**
     * List all activities for given site, desk, type, and subtype.
     */
    private void listActivities() {

        boolean listLatest = listRadioButtons[0].getSelection();
        if (typeListWidget.getSelectionCount() > 0) {

            java.util.List<ActivityElement> elems = ac.getCurrentActivityList();
            java.util.List<ActivityElement> filterElms = new ArrayList<ActivityElement>();

            if (elems.size() > 0) {
                if (listLatest) {

                    // Sort all entries based on time, latest first.
                    Collections.sort(elems, new Comparator<ActivityElement>() {
                        @Override
                        public int compare(ActivityElement ae1,
                                ActivityElement ae2) {
                            return -1 * ae1.refTime.compareTo(ae2.refTime);
                        }
                    });

                    // Remove time stamps resulting from "All"
                    java.util.List<String> actLbls = new ArrayList<String>();
                    for (ActivityElement ae : elems) {
                        int indx = ae.activityLabel.indexOf("$");
                        if (indx >= 0) {
                            ae.activityLabel = ae.activityLabel.substring(0,
                                    indx);
                        }
                    }

                    // Pick unique labels.
                    for (ActivityElement ae : elems) {
                        if (!actLbls.contains(ae.activityLabel)) {
                            actLbls.add(ae.activityLabel);
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
                                String aLbl = ae1.activityLabel;
                                int loc = aLbl.indexOf("$");
                                if (loc >= 0) {
                                    aLbl = aLbl.substring(0, loc);
                                }

                                if (ae.activityLabel.equals(aLbl)) {
                                    attachReftime = true;
                                    break;
                                }
                            }
                        }

                        // Note, we are using "GMT" time zone when we save.
                        if (attachReftime) {
                            DateFormat fmt = new SimpleDateFormat(
                                    "yy-MM-dd:HH:mm");
                            fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
                            ae.activityLabel = ae.activityLabel + "$"
                                    + fmt.format(ae.refTime);
                        }

                    }

                    filterElms.addAll(elems);
                }
            }

            fileListViewer.setInput(filterElms);
            fileListViewer.getList().setToolTipText(null);
            fileListViewer.refresh();

            // Update the full file name with the new path
            fullName = null;
        }
    }
}