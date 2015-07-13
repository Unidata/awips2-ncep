package gov.noaa.nws.ncep.ui.pgen.controls;

import gov.noaa.nws.ncep.common.dataplugin.pgen.PgenRecord;
import gov.noaa.nws.ncep.ui.pgen.PgenUtil;
import gov.noaa.nws.ncep.ui.pgen.controls.RetrieveActivityDialog.ActivityElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.TreeSet;

import com.raytheon.uf.common.dataquery.requests.DbQueryRequest;
import com.raytheon.uf.common.dataquery.responses.DbQueryResponse;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.localization.LocalizationManager;
import com.raytheon.uf.viz.core.requests.ThriftClient;

/**
 * This class retrieves the information of activityElements from the database,
 * maintains the data in a consistent way internally, and prepares the data in
 * proper formats so that they can be easily accessed by the class
 * RetrieveActivityDialog to construct the PGEN open dialog.
 * 
 * Specifically, this class implements the following criteria to determine the
 * listings of Site, Desk, Type, Subtype, and Activity labels. If a Site is
 * selected by users, the listings of Desk, Type, Subtype, and Activity labels
 * will change based on this Site. Likewise, if a Type is selected, the listings
 * of Subtype and Activity labels will change based on the current Site and
 * Desk. The rule applies to Desk and Subtype in similar ways. Precisely,
 * 
 * <pre>
 * a change to Site by users will trigger a change to Desk;
 * a change to Desk will trigger a changes to Type;
 * a change to Type will trigger a change to Subtype;
 * a change to Subtype will trigger a change to Activity labels.   
 * The rules are applied sequentially and immediately.
 * 
 * The listing of Site includes all the sites of Activities in the PGEN table in the database.
 * The listing of Desk includes all the desks for the current Site.
 * The listing of Type includes all the types for the current Site and Desk.
 * The listing of Subtype includes all the subtypes for the current Site and Desk and Type.
 * The listing of Activity labels includes all the labels for the current Site and Desk and Type and Subtype.
 * </pre>
 * 
 * When the listing of Site, Desk, Type, or Subtype is generated, the first item
 * on the listing is selected in default, which is usually the option "All",
 * although the class RetrieveActivityDialog can choose to hide the option "All"
 * for Type and Subtype.
 * 
 * The values for Site and Desk are defaulted to the current SITE and DESK of
 * the CAVE session.
 * 
 * The dialog will remember the selections of Site, Desk, Type, and Subtype
 * within a single instance of CAVE. A new CAVE should reset back to normal.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- -----------------------------------
 * 06/24/2015   R7806       A. Su       Initial creation.
 * 
 * </pre>
 * 
 * @author Augustine Su
 * @version 1
 */
public class ActivityCollection {
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(RetrieveActivityDialog.class);

    /**
     * The menu item in pull-down menus and a SWT list to designate all.
     */
    private final static String optionAll = "All";

    /**
     * The internal string for empty desk or subtype.
     */
    private final static String optionSavedEmptyString = "      ";

    /**
     * The menu item in pull-down menus and a SWT list to designate no desk or
     * subtype specified.
     */
    private final static String optionDisplayedEmptyString = "None";

    /**
     * A list of activityElements retrieved from the database.
     */
    private java.util.List<ActivityElement> retrievedActivityList = null;;

    /**
     * The current listing of the menu items in the pull-down menu for Site.
     */
    private String[] currentSiteList = null;

    /**
     * The current listing of the menu items in the pull-down menu for Desk.
     */
    private String[] currentDeskList = null;

    /**
     * The current listing of the items in the SWT list for Type.
     */
    private String[] currentTypeList = null;

    /**
     * The current listing of the items in the SWT list for Subtype.
     */
    private String[] currentSubtypeList = null;

    /**
     * The current listing of the items in the SWT list for Activity Labels.
     */
    private java.util.List<ActivityElement> currentActivityList = null;

    /**
     * The index of the selected menu item to the pull-down menu for Site.
     */
    private static int currentSiteIndex = -1;

    /**
     * The index of the selected menu item to the pull-down menu for Desk.
     */
    private int currentDeskIndex = -1;

    /**
     * The index of the selected item to the SWT list for Type.
     */
    private int currentTypeIndex = -1;

    /**
     * The index of the selected item to the SWT list for Subtype.
     */
    private int currentSubtypeIndex = -1;

    /**
     * This constructor retrieves the information of activityElements from the
     * database and sets up the default values for the SWT widgets in the PGEN
     * Open dialog.
     */
    ActivityCollection() {
        retrievedActivityList = retrieveFromDB();

        if (retrievedActivityList == null || retrievedActivityList.size() == 0) {
            setUpDummyDefaultValues();
            return;
        }

        setUpDefaultValues();
    }

    /**
     * This method produces the default value of the index number to the
     * pull-down menu for Site. It is intended to be called during the setup of
     * the PGEN Open dialog.
     * 
     * @return the default index if found, or 0 (the first item) otherwise.
     */
    public int getDefaultSiteIndex() {
        String siteString = PgenUtil.getCurrentOffice();
        int index = 0;

        if (currentSiteList != null && currentSiteList.length > 0) {
            index = Arrays.asList(currentSiteList).indexOf(siteString);
        }

        if (index == -1) {
            index = 0;
        }

        return index;
    }

    /**
     * This method produces the default value of the index number to the
     * pull-down menu for Desk. It is intended to be called during the setup of
     * the PGEN Open dialog.
     * 
     * @return the default index if found, or 0 (the first item) otherwise.
     */
    public int getDefaultDeskListIndex() {
        String deskString = LocalizationManager
                .getContextName(LocalizationLevel.valueOf("DESK"));
        int index = 0;

        if (currentDeskList != null && currentDeskList.length > 0) {
            index = Arrays.asList(currentDeskList).indexOf(deskString);
        }

        if (index == -1) {
            index = 0;
        }

        return index;
    }

    /**
     * Call this method if the user changes the selection in the pull-down menu
     * for Site.
     * 
     * A change to Site will trigger a change to Desk, which will trigger a
     * change to Type, which will trigger a change to Subtype, which will
     * trigger a change to Activity labels.
     * 
     * @param siteIndex
     *            the newly selected index to the pull-down menu for Site.
     */
    public void changeSiteIndex(int siteIndex) {
        if (siteIndex < 0 || currentSiteList == null
                || currentSiteList.length <= siteIndex) {
            statusHandler.handle(Priority.PROBLEM, "PGEN ActivityCollection",
                    "Input <" + siteIndex
                            + "> to changeSiteIndex() out of bound");
            return;
        }

        if (siteIndex == currentSiteIndex) {
            return;
        }

        if (siteIndex == 0) {
            setUpDefaultValues();
            return;
        }

        currentSiteIndex = siteIndex;

        // Linear search through the retrievedActivityList
        String siteString = currentSiteList[currentSiteIndex];
        java.util.List<ActivityElement> activityList = new ArrayList<ActivityElement>();
        TreeSet<String> deskSet = new TreeSet<String>();
        TreeSet<String> typeSet = new TreeSet<String>();
        TreeSet<String> subtypeSet = new TreeSet<String>();

        for (ActivityElement e : retrievedActivityList) {
            if (e.site.equals(siteString)) {
                if (e.desk == null || e.desk.length() == 0) {
                    deskSet.add(optionSavedEmptyString);
                } else {
                    deskSet.add(e.desk);
                }

                typeSet.add(e.activityType);

                if (e.activitySubtype == null
                        || e.activitySubtype.length() == 0) {
                    subtypeSet.add(optionSavedEmptyString);
                } else {
                    subtypeSet.add(e.activitySubtype);
                }

                activityList.add(e);
            }
        }

        int i = 0;
        currentDeskList = new String[deskSet.size() + 1];
        currentDeskList[i++] = optionAll;
        currentDeskIndex = 0;
        for (String desk : deskSet) {
            currentDeskList[i++] = desk;
        }

        if (currentDeskList[1].equals(optionSavedEmptyString)) {
            currentDeskList[1] = optionDisplayedEmptyString;
        }

        int j = 0;
        currentTypeList = new String[typeSet.size() + 1];
        currentTypeList[j++] = optionAll;
        currentTypeIndex = 0;
        for (String type : typeSet) {
            currentTypeList[j++] = type;
        }

        int k = 0;
        currentSubtypeList = new String[subtypeSet.size() + 1];
        currentSubtypeList[k++] = optionAll;
        currentSubtypeIndex = 0;
        for (String subtype : subtypeSet) {
            currentSubtypeList[k++] = subtype;
        }

        if (currentSubtypeList[1].equals(optionSavedEmptyString)) {
            currentSubtypeList[1] = optionDisplayedEmptyString;
        }

        currentActivityList = activityList;
    }

    /**
     * Call this method if the user changes the selection in the pull-down menu
     * for Desk.
     * 
     * A change to Desk will trigger a change to Type, which will trigger a
     * change to Subtype, which will trigger a change to Activity labels.
     * 
     * @param deskIndex
     *            the newly selected index to the pull-down menu for Desk.
     */
    public void changeDeskIndex(int deskIndex) {
        if (deskIndex < 0 || currentDeskList == null
                || currentDeskList.length <= deskIndex) {
            statusHandler.handle(Priority.PROBLEM, "PGEN ActivityCollection",
                    "Input <" + deskIndex
                            + "> to changeDeskIndex() out of bound");
            return;
        }

        if (deskIndex == currentDeskIndex) {
            return;
        }

        if (currentSiteIndex == 0 && deskIndex == 0) {
            setUpDefaultValues();
            return;
        }

        currentDeskIndex = deskIndex;
        boolean isEmptyDeskSelected = (currentDeskIndex == 1 && currentDeskList[1]
                .equals(optionDisplayedEmptyString));

        // Linear search
        String siteString = currentSiteList[currentSiteIndex];
        String deskString = currentDeskList[currentDeskIndex];
        TreeSet<String> typeSet = new TreeSet<String>();
        TreeSet<String> subtypeSet = new TreeSet<String>();
        java.util.List<ActivityElement> activityList = new ArrayList<ActivityElement>();

        for (ActivityElement e : retrievedActivityList) {
            if (currentSiteIndex != 0 && !e.site.equals(siteString)) {
                continue;
            }

            if (currentDeskIndex != 0
                    && (!isEmptyDeskSelected || (e.desk != null && e.desk
                            .length() > 0)) && !e.desk.equals(deskString)) {
                continue;
            }

            typeSet.add(e.activityType);

            if (e.activitySubtype == null || e.activitySubtype.length() == 0) {
                subtypeSet.add(optionSavedEmptyString);
            } else {
                subtypeSet.add(e.activitySubtype);
            }

            activityList.add(e);
        }

        int i = 0;
        currentTypeList = new String[typeSet.size() + 1];
        currentTypeList[i++] = optionAll;
        currentTypeIndex = 0;
        for (String type : typeSet) {
            currentTypeList[i++] = type;
        }

        int j = 0;
        currentSubtypeList = new String[subtypeSet.size() + 1];
        currentSubtypeList[j++] = optionAll;
        currentSubtypeIndex = 0;

        for (String subtype : subtypeSet) {
            currentSubtypeList[j++] = subtype;
        }

        if (currentSubtypeList[1].equals(optionSavedEmptyString)) {
            currentSubtypeList[1] = optionDisplayedEmptyString;
        }

        currentActivityList = activityList;
        return;
    }

    /**
     * Call this method if the user changes the selection in the SWT list for
     * Type.
     * 
     * A change to Type, which will trigger a change to Subtype, which will
     * trigger a change to Activity labels.
     * 
     * @param typeIndex
     *            the newly selected index to the SWT list for Type.
     */
    public void changeTypeIndex(int typeIndex) {
        if (typeIndex < 0 || currentTypeList.length <= typeIndex) {
            statusHandler.handle(Priority.PROBLEM, "PGEN ActivityCollection",
                    "Input <" + typeIndex
                            + "> to changeTypeIndex() out of bound");
            return;
        }

        if (typeIndex == currentTypeIndex) {
            return;
        }

        if (currentSiteIndex == 0 && currentDeskIndex == 0 && typeIndex == 0) {
            setUpDefaultValues();
            return;
        }

        currentTypeIndex = typeIndex;
        boolean isEmptyDeskSelected = (currentDeskIndex == 1 && currentDeskList[1]
                .equals(optionDisplayedEmptyString));

        // Linear search
        String siteString = currentSiteList[currentSiteIndex];
        String deskString = currentDeskList[currentDeskIndex];
        String typeString = currentTypeList[currentTypeIndex];
        TreeSet<String> subtypeSet = new TreeSet<String>();
        java.util.List<ActivityElement> activityList = new ArrayList<ActivityElement>();

        for (ActivityElement e : retrievedActivityList) {
            if (currentSiteIndex != 0 && !e.site.equals(siteString)) {
                continue;
            }

            if (currentDeskIndex != 0
                    && (!isEmptyDeskSelected || (e.desk != null && e.desk
                            .length() > 0)) && !e.desk.equals(deskString)) {
                continue;
            }

            if (currentTypeIndex != 0 && !e.activityType.equals(typeString)) {
                continue;
            }

            if (e.activitySubtype == null || e.activitySubtype.length() == 0) {
                subtypeSet.add(optionSavedEmptyString);
            } else {
                subtypeSet.add(e.activitySubtype);
            }

            activityList.add(e);
        }

        int i = 0;
        currentSubtypeList = new String[subtypeSet.size() + 1];
        currentSubtypeList[i++] = optionAll;
        currentSubtypeIndex = 0;
        for (String subtype : subtypeSet) {
            currentSubtypeList[i++] = subtype;
        }

        if (currentSubtypeList[1].equals(optionSavedEmptyString)) {
            currentSubtypeList[1] = optionDisplayedEmptyString;
        }

        currentActivityList = activityList;
        return;
    }

    /**
     * Call this method if the user changes the selection in the SWT list for
     * Subtype.
     * 
     * A change to Subtype, which will trigger a change to Activity labels.
     * 
     * @param subtypeIndex
     *            the newly selected index to the SWT list for Subype.
     */
    public void changeSubtypeIndex(int subtypeIndex) {
        if (subtypeIndex < 0 || currentSubtypeList.length <= subtypeIndex) {
            statusHandler.handle(Priority.PROBLEM, "PGEN ActivityCollection",
                    "Input <" + subtypeIndex
                            + "> to changeSubtypeIndex() out of bound");
            return;
        }

        if (subtypeIndex == currentSubtypeIndex) {
            return;
        }

        if (currentSiteIndex == 0 && currentDeskIndex == 0
                && currentTypeIndex == 0 && subtypeIndex == 0) {
            setUpDefaultValues();
            return;
        }

        currentSubtypeIndex = subtypeIndex;
        boolean isEmptyDeskSelected = (currentDeskIndex == 1 && currentDeskList[1]
                .equals(optionDisplayedEmptyString));
        boolean isEmptySubtypeSelected = (currentSubtypeIndex == 1 && currentSubtypeList[1]
                .equals(optionDisplayedEmptyString));

        // Linear search
        String siteString = currentSiteList[currentSiteIndex];
        String deskString = currentDeskList[currentDeskIndex];
        String typeString = currentTypeList[currentTypeIndex];
        String subtypeString = currentSubtypeList[currentSubtypeIndex];
        java.util.List<ActivityElement> activityList = new ArrayList<ActivityElement>();

        for (ActivityElement e : retrievedActivityList) {
            if (currentSiteIndex != 0 && !e.site.equals(siteString)) {
                continue;
            }

            if (currentDeskIndex != 0
                    && (!isEmptyDeskSelected || (e.desk != null && e.desk
                            .length() > 0)) && !e.desk.equals(deskString)) {
                continue;
            }

            if (currentTypeIndex != 0 && !e.activityType.equals(typeString)) {
                continue;
            }

            if (currentSubtypeIndex != 0
                    && (!isEmptySubtypeSelected || (e.activitySubtype != null && e.activitySubtype
                            .length() > 0))
                    && !e.activitySubtype.equals(subtypeString)) {
                continue;
            }

            activityList.add(e);
        }

        currentActivityList = activityList;
        return;
    }

    /**
     * Retrieve the current listing of Site.
     * 
     * @return the current listing of Site.
     */
    public String[] getCurrentSiteList() {
        return currentSiteList;
    }

    /**
     * Retrieve the current listing of Desk.
     * 
     * @return the current listing of Desk.
     */
    public String[] getCurrentDeskList() {
        return currentDeskList;
    }

    /**
     * Retrieve the current listing of Type.
     * 
     * @return the current listing of Type.
     */
    public String[] getCurrentTypeList() {
        return currentTypeList;
    }

    /**
     * Retrieve the current listing of Subtype.
     * 
     * @return the current listing of Subtype.
     */
    public String[] getCurrentSubtypeList() {
        return currentSubtypeList;
    }

    /**
     * Retrieve the current index to the listing of Site.
     * 
     * @return the current index to the listing of Site.
     */
    public int getCurrentSiteIndex() {
        return currentSiteIndex;
    }

    /**
     * Retrieve the current index to the listing of Desk.
     * 
     * @return the current index to the listing of Desk.
     */
    public int getCurrentDeskIndex() {
        return currentDeskIndex;
    }

    /**
     * Retrieve the current index to the listing of Type.
     * 
     * @return the current index to the listing of Type.
     */
    public int getCurrentTypeIndex() {
        return currentTypeIndex;
    }

    /**
     * Retrieve the current index to the listing of Subtype.
     * 
     * @return the current index to the listing of Subtype.
     */
    public int getCurrentSubtypeIndex() {
        return currentSubtypeIndex;
    }

    /**
     * Retrieve the current listing of ActivityElements after filtering.
     * 
     * @return the current listing of ActivityElements after filtering.
     */
    public java.util.List<ActivityElement> getCurrentActivityList() {
        return currentActivityList;
    }

    /**
     * Set up default values for the listings of Site, Desk, Type, Subtype, and
     * Activity labels as well as the indices to Site, Desk, Type, and Subtype.
     */
    private void setUpDefaultValues() {

        TreeSet<String> siteSet = new TreeSet<String>();
        TreeSet<String> deskSet = new TreeSet<String>();
        TreeSet<String> typeSet = new TreeSet<String>();
        TreeSet<String> subtypeSet = new TreeSet<String>();
        for (ActivityElement e : retrievedActivityList) {
            siteSet.add(e.site);

            if (e.desk == null || e.desk.length() == 0) {
                deskSet.add(optionSavedEmptyString);
            } else {
                deskSet.add(e.desk);
            }

            typeSet.add(e.activityType);

            if (e.activitySubtype == null || e.activitySubtype.length() == 0) {
                subtypeSet.add(optionSavedEmptyString);
            } else {
                subtypeSet.add(e.activitySubtype);
            }
        }

        int i = 0;
        currentSiteList = new String[siteSet.size() + 1];
        currentSiteList[i++] = optionAll;
        currentSiteIndex = 0;

        for (String site : siteSet) {
            currentSiteList[i++] = site;
        }

        int j = 0;
        currentDeskList = new String[deskSet.size() + 1];
        currentDeskList[j++] = optionAll;
        currentDeskIndex = 0;

        for (String desk : deskSet) {
            currentDeskList[j++] = desk;
        }

        if (currentDeskList[1].equals(optionSavedEmptyString))
            currentDeskList[1] = optionDisplayedEmptyString;

        int m = 0;
        currentTypeList = new String[typeSet.size() + 1];
        currentTypeList[m++] = optionAll;
        currentTypeIndex = 0;

        for (String type : typeSet) {
            currentTypeList[m++] = type;
        }

        int n = 0;
        currentSubtypeList = new String[subtypeSet.size() + 1];
        currentSubtypeList[n++] = optionAll;
        currentSubtypeIndex = 0;

        for (String subtype : subtypeSet) {
            currentSubtypeList[n++] = subtype;
        }

        if (currentSubtypeList[1].equals(optionSavedEmptyString)) {
            currentSubtypeList[1] = optionDisplayedEmptyString;
        }

        currentActivityList = new ArrayList<ActivityElement>(
                retrievedActivityList);
    }

    /**
     * If the DB returns an empty list of ActivityElements, set up default
     * values for the listings of Site, Desk, Type, Subtype, and Activity labels
     * as well as the indices to Site, Desk, Type, and Subtype.
     */
    private void setUpDummyDefaultValues() {
        currentSiteList = new String[1];
        currentSiteList[0] = optionAll;
        currentSiteIndex = 0;

        currentDeskList = new String[1];
        currentDeskList[0] = optionAll;
        currentDeskIndex = 0;

        currentTypeList = new String[1];
        currentTypeList[0] = optionAll;
        currentTypeIndex = 0;

        currentSubtypeList = new String[1];
        currentSubtypeList[0] = optionAll;
        currentSubtypeIndex = 0;

        currentActivityList = new ArrayList<ActivityElement>();
    }

    /**
     * Retrieve the information of ActivityElements from the database.
     * 
     * @return a list of ActivityElements.
     */
    private java.util.List<ActivityElement> retrieveFromDB() {
        DbQueryRequest request = new DbQueryRequest();
        request.setEntityClass(PgenRecord.class.getName());
        request.addRequestField(PgenRecord.SITE);
        request.addRequestField(PgenRecord.DESK);
        request.addRequestField(PgenRecord.ACTIVITY_TYPE);
        request.addRequestField(PgenRecord.ACTIVITY_SUBTYPE);
        request.addRequestField(PgenRecord.ACTIVITY_LABEL);
        request.addRequestField(PgenRecord.DATAURI);
        request.addRequestField(PgenRecord.REF_TIME);

        DbQueryResponse response;
        java.util.List<ActivityElement> elements = new ArrayList<ActivityElement>();

        try {
            response = (DbQueryResponse) ThriftClient.sendRequest(request);

            for (Map<String, Object> result : response.getResults()) {
                ActivityElement e = new ActivityElement();
                e.site = (String) result.get(PgenRecord.SITE);
                e.desk = (String) result.get(PgenRecord.DESK);
                e.activityType = (String) result.get(PgenRecord.ACTIVITY_TYPE);
                e.activitySubtype = (String) result
                        .get(PgenRecord.ACTIVITY_SUBTYPE);
                e.activityLabel = (String) result
                        .get(PgenRecord.ACTIVITY_LABEL);
                e.dataURI = (String) result.get(PgenRecord.DATAURI);
                e.refTime = (Date) result.get(PgenRecord.REF_TIME);

                elements.add(e);
            }
        } catch (VizException ex) {
            statusHandler
                    .handle(Priority.PROBLEM, ex.getLocalizedMessage(), ex);
        }

        return elements;
    }
}
