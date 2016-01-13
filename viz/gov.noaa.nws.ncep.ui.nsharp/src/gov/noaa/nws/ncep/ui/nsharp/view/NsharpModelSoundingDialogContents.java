/**
 * 
 * gov.noaa.nws.ncep.ui.nsharp.view.ModelSoundingDialogContents
 * 
 * This java class performs the NSHARP NsharpLoadDialog functions.
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    	Engineer    Description
 * -------		------- 	-------- 	-----------
 * 01/2011	    229			Chin Chen	Initial coding
 * 03/09/2015   RM#6674     Chin Chen   Support model sounding query data interpolation and nearest point option                       
 * 07202015     RM#9173     Chin Chen   use NcSoundingQuery.genericSoundingDataQuery() to query grid model sounding data
 * 08/24/2015   RM#10188    Chin Chen   Model selection upgrades - use grid resource definition name for model type display
 * 09/28/2015   RM#10295    Chin Chen   Let sounding data query run in its own thread to avoid gui locked out during load
 *
 * </pre>
 * 
 * @author Chin Chen
 * @version 1.0
 */
package gov.noaa.nws.ncep.ui.nsharp.view;

import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingProfile.MdlSndType;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingTimeLines;
import gov.noaa.nws.ncep.ui.nsharp.NsharpConfigManager;
import gov.noaa.nws.ncep.ui.nsharp.NsharpConfigStore;
import gov.noaa.nws.ncep.ui.nsharp.NsharpConstants;
import gov.noaa.nws.ncep.ui.nsharp.NsharpGraphProperty;
import gov.noaa.nws.ncep.ui.nsharp.NsharpGridInventory;
import gov.noaa.nws.ncep.ui.nsharp.SurfaceStationPointData;
import gov.noaa.nws.ncep.ui.nsharp.display.NsharpEditor;
import gov.noaa.nws.ncep.ui.nsharp.display.map.NsharpMapResource;
import gov.noaa.nws.ncep.ui.nsharp.display.map.NsharpModelSoundingQuery;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceCategory;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceDefinition;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceDefnsMngr;
import gov.noaa.nws.ncep.viz.soundingrequest.NcSoundingQuery;

import java.sql.Timestamp;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.exception.VizException;
import com.vividsolutions.jts.geom.Coordinate;

public class NsharpModelSoundingDialogContents {
    // Status handling
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(NsharpModelSoundingDialogContents.class);

    private Composite parent;

    private org.eclipse.swt.widgets.List modelTypeRscDefNameList = null,
            availableFileList = null, sndTimeList = null;

    // timeLineToFileMap maps time line (rangeStart time in sndTimeList) to
    // available file (reftime in availableFileList)
    private Map<String, String> timeLineToFileMap = new HashMap<String, String>();

    // soundingLysLstMap maps "lat;lon timeline" string to its queried sounding
    // layer list
    private Map<String, List<NcSoundingLayer>> soundingLysLstMap = new HashMap<String, List<NcSoundingLayer>>();

    private Group modelTypeGp, bottomGp, availableFileGp, sndTimeListGp, topGp,
            locationMainGp;

    private Button timeBtn, latlonBtn, stationBtn, loadBtn;

    private Text locationText;

    private Label locationLbl;

    private boolean timeLimit = false;

    private NsharpLoadDialog ldDia;

    private Font newFont;

    private List<String> selectedFileList = new ArrayList<String>();

    private List<String> selectedTimeList = new ArrayList<String>();

    private float lat, lon;

    private String stnStr = "";

    private final String GOOD_LATLON_STR = " A good input looked like this:\n 38.95;-77.45 or 38.95,-77.45";

    private final String GOOD_STN_STR = " A good input looked like this:\n GAI or gai";

    private final int MAX_LOCATION_TEXT = 15;

    String gribDecoderName = "grid";

    private String selectedModelType = ""; // used for query to database

    private String selectedRscDefName = ""; // use for display on GUI

    private static Map<String, String> gridModelToRscDefNameMap = new HashMap<String, String>();

    private static Map<String, String> rscDefNameToGridModelMap = new HashMap<String, String>();

    private static final String SND_TIMELINE_NOT_AVAIL_STRING = "No Sounding Time for Nsharp";

    public enum LocationType {
        LATLON, STATION
    }

    private LocationType currentLocType = LocationType.LATLON;

    public LocationType getCurrentLocType() {
        return currentLocType;
    }

    public Text getLocationText() {
        return locationText;
    }

    public NsharpModelSoundingDialogContents(Composite parent) {
        this.parent = parent;
        ldDia = NsharpLoadDialog.getAccess();
        newFont = ldDia.getNewFont();
        try {
            createModelTypeToRscDefNameMapping();
        } catch (VizException e) {
            statusHandler
                    .handle(Priority.ERROR,
                            "NsharpModelSoundingDialogContents: exception while createModelTypeToRscDefNameMapping.",
                            e);
        }
    }

    private void createMDLAvailableFileList() {
        if (sndTimeList != null)
            sndTimeList.removeAll();
        if (availableFileList != null)
            availableFileList.removeAll();
        HashMap<String, RequestConstraint> rcMap = new HashMap<String, RequestConstraint>();
        rcMap.put("info.datasetId", new RequestConstraint(selectedModelType));
        ldDia.startWaitCursor();
        ArrayList<String> queryRsltsList1 = NsharpGridInventory.getInstance()
                .searchInventory(rcMap, "dataTime");
        /*
         * Chin As of 12/11/2013, the returned string format is like this,
         * 2012-01-17_16:00:00.0_(6) We will have to strip off ":00:00.0_(6)",
         * also replace first "_" with space, to get grid file name like this
         * "2012-01-17 16".
         */
        if (queryRsltsList1 != null && !queryRsltsList1.isEmpty()) {
            Collections.sort(queryRsltsList1, String.CASE_INSENSITIVE_ORDER);
            Collections.reverse(queryRsltsList1);

            for (String queryRslt : queryRsltsList1) {
                String refTime = queryRslt.substring(0, queryRslt.indexOf('_'));
                refTime = refTime
                        + " "
                        + queryRslt.substring(queryRslt.indexOf('_') + 1,
                                queryRslt.indexOf(':'));
                // Chin: a same refTime may be returned more than once.
                int index = availableFileList.indexOf(refTime);
                if (index == -1) // index = -1 means it is not in the list
                    availableFileList.add(refTime);
            }
        }
        ldDia.stopWaitCursor();

    }

    private void createMDLSndTimeList(List<String> selectedFlLst) {
        if (selectedFlLst.size() <= 0)
            return;
        if (sndTimeList != null)
            sndTimeList.removeAll();
        if (timeLineToFileMap != null)
            timeLineToFileMap.clear();
        // set max resource name length to 10 chars for displaying
        int nameLen = Math.min(10, selectedRscDefName.length());
        String modelName = selectedRscDefName.substring(0, nameLen);
        // query using NcSoundingQuery to query
        DateFormatSymbols dfs = new DateFormatSymbols();
        String[] defaultDays = dfs.getShortWeekdays();
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        ldDia.startWaitCursor();
        for (int i = 0; i < selectedFlLst.size(); i++) {
            String fl = selectedFlLst.get(i);
            long reftimeMs = NcSoundingQuery.convertRefTimeStr(fl);
            NcSoundingTimeLines timeLines = NcSoundingQuery
                    .soundingRangeTimeLineQuery(MdlSndType.ANY.toString(), fl,
                            selectedModelType);
            if (timeLines != null && timeLines.getTimeLines().length > 0) {
                for (Object obj : timeLines.getTimeLines()) {
                    Timestamp rangestart = (Timestamp) obj;
                    // need to format rangestart to GMT time string.
                    // Timestamp.toString produce a local time Not GMT time
                    cal.setTimeInMillis(rangestart.getTime());
                    long vHour = (cal.getTimeInMillis() - reftimeMs) / 3600000;
                    String dayOfWeek = defaultDays[cal
                            .get(Calendar.DAY_OF_WEEK)];
                    String gmtTimeStr = String.format(
                            "%1$ty%1$tm%1$td/%1$tH(%4$s)V%2$03d %3$s", cal,
                            vHour, modelName, dayOfWeek);
                    if (sndTimeList.indexOf(gmtTimeStr) != -1) {
                        // this indicate that gmtTimeStr is already in the
                        // sndTimeList, then we dont need to add it to list
                        // again.
                        continue;
                    }
                    if (!timeLimit) {
                        sndTimeList.add(gmtTimeStr);
                        timeLineToFileMap.put(gmtTimeStr, fl);
                    } else {
                        int hour = cal.get(Calendar.HOUR_OF_DAY);
                        if ((hour == 0) || (hour == 12)) {
                            sndTimeList.add(gmtTimeStr);
                            timeLineToFileMap.put(gmtTimeStr, fl);
                        }
                    }
                }
            }
        }
        if (sndTimeList != null && sndTimeList.getItemCount() <= 0) {
            sndTimeList.add(SND_TIMELINE_NOT_AVAIL_STRING);
        }
        ldDia.stopWaitCursor();
    }

    private static void createModelTypeToRscDefNameMapping()
            throws VizException {
        ResourceDefnsMngr rscDefnsMngr = ResourceDefnsMngr.getInstance();
        gridModelToRscDefNameMap.clear();
        rscDefNameToGridModelMap.clear();
        if (rscDefnsMngr != null) {
            ResourceCategory cat = ResourceCategory.createCategory("GRID");
            List<ResourceDefinition> rscTypes = rscDefnsMngr
                    .getResourceDefnsForCategory(cat);
            for (ResourceDefinition rd : rscTypes) {
                HashMap<String, String> rpmap = rd.getResourceParameters(false);
                if (rpmap != null) {
                    String mdlType = rpmap.get("GDFILE");
                    String rscDefName = rd.getResourceDefnName();
                    gridModelToRscDefNameMap.put(mdlType, rscDefName);
                    rscDefNameToGridModelMap.put(rscDefName, mdlType);
                } else
                    continue;
            }
        }
    }

    private void createModelTypeList() {
        if (modelTypeRscDefNameList != null)
            modelTypeRscDefNameList.removeAll();
        if (sndTimeList != null)
            sndTimeList.removeAll();
        if (availableFileList != null)
            availableFileList.removeAll();
        ldDia.startWaitCursor();
        List<String> cfgList = null;
        NsharpConfigManager configMgr = NsharpConfigManager.getInstance();
        NsharpConfigStore configStore = configMgr
                .retrieveNsharpConfigStoreFromFs();
        NsharpGraphProperty graphConfigProperty = configStore
                .getGraphProperty();
        cfgList = graphConfigProperty.getGribModelTypeList();
        HashMap<String, RequestConstraint> rcMap = new HashMap<String, RequestConstraint>();
        rcMap.put("pluginName", new RequestConstraint("grid"));

        ArrayList<String> queryRsltsList = NsharpGridInventory.getInstance()
                .searchInventory(rcMap, "info.datasetId");

        /*
         * the returned string has format like this, "gfsP5". Therefore, we do
         * not have to process on it.
         */
        if (queryRsltsList != null && !queryRsltsList.isEmpty()) {
            Collections.sort(queryRsltsList, String.CASE_INSENSITIVE_ORDER);
            for (String modelName : queryRsltsList) {
                String rscDefName = gridModelToRscDefNameMap.get(modelName);
                if (cfgList != null && cfgList.size() > 0) {
                    if (cfgList.contains(rscDefName)) {
                        if (rscDefName != null)
                            modelTypeRscDefNameList.add(rscDefName);
                    }
                } else if (rscDefName != null)
                    modelTypeRscDefNameList.add(rscDefName);

            }
        }
        ldDia.stopWaitCursor();

    }

    private void handleAvailFileListSelection() {
        String selectedFile = null;
        if (availableFileList.getSelectionCount() > 0) {
            selectedFileList.clear();
            for (int i = 0; i < availableFileList.getSelectionCount(); i++) {
                selectedFile = availableFileList.getSelection()[i];
                selectedFileList.add(selectedFile);
            }
            createMDLSndTimeList(selectedFileList);
        }
    }

    private void handleSndTimeSelection() {
        String selectedSndTime = null;
        if (sndTimeList.getSelectionCount() > 0
                && sndTimeList.getSelection()[0]
                        .equals(SND_TIMELINE_NOT_AVAIL_STRING) == false) {

            selectedTimeList.clear();
            for (int i = 0; i < sndTimeList.getSelectionCount(); i++) {
                selectedSndTime = sndTimeList.getSelection()[i];
                selectedTimeList.add(selectedSndTime);
            }
            NsharpMapResource.bringMapEditorToTop();
        }
    }

    public void createMdlDialogContents() {
        topGp = new Group(parent, SWT.SHADOW_ETCHED_IN);
        topGp.setLayout(new GridLayout(2, false));
        selectedModelType = ldDia.getActiveMdlSndMdlType();
        selectedRscDefName = rscDefNameToGridModelMap.get(selectedModelType);
        ldDia.createSndTypeList(topGp);

        modelTypeGp = new Group(topGp, SWT.SHADOW_ETCHED_IN);
        modelTypeGp.setText("Model Type");
        modelTypeGp.setFont(newFont);
        modelTypeRscDefNameList = new org.eclipse.swt.widgets.List(modelTypeGp,
                SWT.BORDER | SWT.V_SCROLL);
        modelTypeRscDefNameList.setBounds(modelTypeGp.getBounds().x,
                modelTypeGp.getBounds().y + NsharpConstants.labelGap,
                NsharpConstants.filelistWidth, NsharpConstants.listHeight);
        // query to get and add available sounding models from DB
        modelTypeRscDefNameList.setFont(newFont);
        createModelTypeList();

        // create a selection listener to handle user's selection on list
        modelTypeRscDefNameList.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                if (modelTypeRscDefNameList.getSelectionCount() > 0) {
                    selectedRscDefName = modelTypeRscDefNameList.getSelection()[0];
                    // convert selectedModel, in resource definition name, to
                    // grid model type name
                    selectedModelType = rscDefNameToGridModelMap
                            .get(selectedRscDefName);
                    ldDia.setActiveMdlSndMdlType(selectedModelType);
                    createMDLAvailableFileList();
                }
            }
        });

        availableFileGp = new Group(topGp, SWT.SHADOW_ETCHED_IN);
        availableFileGp.setText("Available Grid files:");
        availableFileGp.setFont(newFont);
        availableFileList = new org.eclipse.swt.widgets.List(availableFileGp,
                SWT.BORDER | SWT.V_SCROLL);
        availableFileList.setBounds(availableFileGp.getBounds().x,
                availableFileGp.getBounds().y + NsharpConstants.labelGap,
                NsharpConstants.filelistWidth, NsharpConstants.listHeight);
        availableFileList.setFont(newFont);
        // create a selection listener to handle user's selection on list
        availableFileList.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                handleAvailFileListSelection();
            }
        });

        // create Sounding Times widget list
        sndTimeListGp = new Group(topGp, SWT.SHADOW_ETCHED_IN);
        sndTimeListGp.setText("Sounding Times:");
        sndTimeListGp.setFont(newFont);
        sndTimeList = new org.eclipse.swt.widgets.List(sndTimeListGp,
                SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
        sndTimeList.removeAll();
        sndTimeList.setFont(newFont);
        sndTimeList.setBounds(sndTimeListGp.getBounds().x,
                sndTimeListGp.getBounds().y + NsharpConstants.labelGap,
                NsharpConstants.listWidth, NsharpConstants.listHeight);
        sndTimeList.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                handleSndTimeSelection();
            }
        });
        timeBtn = new Button(topGp, SWT.CHECK | SWT.BORDER);
        timeBtn.setText("00Z and 12Z only");
        timeBtn.setEnabled(true);
        timeBtn.setFont(newFont);
        timeBtn.addListener(SWT.MouseUp, new Listener() {
            public void handleEvent(Event event) {
                if (timeLimit)
                    timeLimit = false;
                else
                    timeLimit = true;

                // refresh sounding list if file type is selected already
                if (selectedModelType != null && selectedFileList.size() > 0) {
                    createMDLSndTimeList(selectedFileList);
                }

            }
        });
        locationMainGp = new Group(parent, SWT.SHADOW_ETCHED_IN);
        locationMainGp.setLayout(new GridLayout(5, false));
        locationMainGp.setText("Location");
        locationMainGp.setFont(newFont);
        latlonBtn = new Button(locationMainGp, SWT.RADIO | SWT.BORDER);
        latlonBtn.setText("Lat/Lon");
        latlonBtn.setFont(newFont);
        latlonBtn.setEnabled(true);
        latlonBtn.setSelection(true);
        latlonBtn.addListener(SWT.MouseUp, new Listener() {
            public void handleEvent(Event event) {
                currentLocType = LocationType.LATLON;
                locationText.setText("");
            }
        });
        stationBtn = new Button(locationMainGp, SWT.RADIO | SWT.BORDER);
        stationBtn.setText("Station");
        stationBtn.setEnabled(true);
        stationBtn.setFont(newFont);
        stationBtn.addListener(SWT.MouseUp, new Listener() {
            public void handleEvent(Event event) {
                currentLocType = LocationType.STATION;
                locationText.setText("");
            }
        });
        locationLbl = new Label(locationMainGp, SWT.NONE | SWT.BORDER);
        locationLbl.setText("Location:");
        locationLbl.setFont(newFont);
        locationText = new Text(locationMainGp, SWT.BORDER | SWT.SINGLE);
        GridData data1 = new GridData(SWT.FILL, SWT.FILL, true, true);
        locationText.setLayoutData(data1);
        locationText.setTextLimit(MAX_LOCATION_TEXT);
        locationText.setFont(newFont);
        locationText.addListener(SWT.Verify, new Listener() {
            public void handleEvent(Event e) {
                String userInputStr = e.text;
                if (userInputStr.length() > 0) {

                    if (currentLocType == LocationType.LATLON) {
                        // to make sure user enter digits and separated by ";"
                        // or ","only, if lat/lon is used
                        if (userInputStr.length() == 1) {
                            char inputChar = userInputStr.charAt(0);
                            if (!('0' <= inputChar && inputChar <= '9')
                                    && inputChar != ';' && inputChar != ','
                                    && inputChar != '-' && inputChar != '.') {
                                e.doit = false;
                                return;
                            }
                        }
                    } else {
                        // do nothing when station type

                    }
                }
            }
        });

        loadBtn = new Button(locationMainGp, SWT.PUSH);
        loadBtn.setText("Load ");
        loadBtn.setFont(newFont);
        loadBtn.setEnabled(true);
        loadBtn.setBounds(locationMainGp.getBounds().x
                + NsharpConstants.btnGapX, locationLbl.getBounds().y
                + locationLbl.getBounds().height + NsharpConstants.btnGapY,
                NsharpConstants.btnWidth, NsharpConstants.btnHeight);
        loadBtn.addListener(SWT.MouseUp, new Listener() {
            public void handleEvent(Event event) {
                NsharpLoadDialog ldDia = NsharpLoadDialog.getAccess();
                if (selectedTimeList != null && selectedTimeList.size() == 0) {
                    ldDia.setAndOpenMb("Time line(s) is not selected!\n Can not load data!");
                    return;
                }
                String textStr = locationText.getText();
                if ((textStr != null) && !(textStr.isEmpty())) {
                    if (currentLocType == LocationType.LATLON) {
                        // to make sure user enter digits and separated by ";"
                        // or ","only, if lat/lon is used
                        int dividerIndex = textStr.indexOf(';');
                        boolean indexFound = false;
                        if (dividerIndex != -1)
                            indexFound = true;
                        if (indexFound == false) {
                            dividerIndex = textStr.indexOf(',');
                            if (dividerIndex != -1)
                                indexFound = true;
                        }
                        if (indexFound == true) {
                            try {
                                lat = Float.parseFloat(textStr.substring(0,
                                        dividerIndex));
                                lon = Float.parseFloat(textStr
                                        .substring(dividerIndex + 1));
                                if (lat > 90 || lat < -90 || lon > 180
                                        || lon < -180) {
                                    ldDia.setAndOpenMb("lat/lon out of range ("
                                            + textStr + ") entered!\n"
                                            + GOOD_LATLON_STR);
                                    locationText.setText("");
                                    return;
                                }
                                NsharpModelSoundingQuery qryAndLd = new NsharpModelSoundingQuery(
                                        "Querying Sounding Data...");
                                NsharpEditor skewtEdt = NsharpEditor
                                        .createOrOpenEditor();
                                qryAndLd.queryAndLoadData(false, skewtEdt,
                                        soundingLysLstMap, selectedTimeList,
                                        timeLineToFileMap, lat, lon, stnStr,
                                        selectedModelType, selectedRscDefName);

                            } catch (Exception e) {
                                statusHandler
                                        .handle(Priority.ERROR,
                                                "NsharpModelSoundingDialogContents: exception while parsing string to float.",
                                                e);
                                return;
                            }

                        } else {
                            ldDia.setAndOpenMb("Bad lat/lon (" + textStr
                                    + ") entered!\n" + GOOD_LATLON_STR);
                            locationText.setText("");
                            return;
                        }
                    } else if (currentLocType == LocationType.STATION) {
                        // query station lat /lon
                        try {
                            // user may start with a space before enter station
                            // id
                            textStr = textStr.trim();
                            stnStr = textStr.toUpperCase(Locale.getDefault());
                            Coordinate co = SurfaceStationPointData
                                    .getStnCoordinate(stnStr);
                            lat = (float) co.y;
                            lon = (float) co.x;
                            if (lat == SurfaceStationPointData.DEFAULT_LATLON) {
                                ldDia.setAndOpenMb("Bad station id (" + textStr
                                        + ") entered!\n" + GOOD_STN_STR);
                                locationText.setText("");
                                return;
                            }
                            NsharpModelSoundingQuery qryAndLd = new NsharpModelSoundingQuery(
                                    "Querying Sounding Data...");
                            NsharpEditor skewtEdt = NsharpEditor
                                    .createOrOpenEditor();
                            qryAndLd.queryAndLoadData(true, skewtEdt,
                                    soundingLysLstMap, selectedTimeList,
                                    timeLineToFileMap, lat, lon, stnStr,
                                    selectedModelType, selectedRscDefName);
                        } catch (Exception e) {
                            statusHandler
                                    .handle(Priority.ERROR,
                                            "NsharpModelSoundingDialogContents: exception while parsing string to float.",
                                            e);

                            return;
                        }
                    }
                }
            }
        });

        if (selectedModelType != null && selectedModelType.equals("") == false) {
            String[] selectedModelArray = { selectedModelType };
            selectedRscDefName = gridModelToRscDefNameMap
                    .get(selectedModelType);
            modelTypeRscDefNameList.setSelection(selectedModelArray);
            createMDLAvailableFileList();
            selectedFileList = ldDia.getMdlSelectedFileList();
            Object[] selFileObjectArray = selectedFileList.toArray();
            String[] selFileStringArray = Arrays.copyOf(selFileObjectArray,
                    selFileObjectArray.length, String[].class);
            availableFileList.setSelection(selFileStringArray);
            handleAvailFileListSelection();

            selectedTimeList = ldDia.getMdlSelectedTimeList();
            Object[] selTimeObjectArray = selectedTimeList.toArray();
            String[] selTimeStringArray = Arrays.copyOf(selTimeObjectArray,
                    selTimeObjectArray.length, String[].class);
            sndTimeList.setSelection(selTimeStringArray);
            handleSndTimeSelection();

        }
    }

    public void cleanup() {
        if (modelTypeRscDefNameList != null) {
            if (modelTypeRscDefNameList.getListeners(SWT.Selection).length > 0)
                modelTypeRscDefNameList.removeListener(SWT.Selection,
                        modelTypeRscDefNameList.getListeners(SWT.Selection)[0]);
            modelTypeRscDefNameList.dispose();
            modelTypeRscDefNameList = null;
        }
        if (modelTypeGp != null) {
            modelTypeGp.dispose();
            modelTypeGp = null;
        }
        if (timeBtn != null) {
            timeBtn.removeListener(SWT.MouseUp,
                    timeBtn.getListeners(SWT.MouseUp)[0]);
            timeBtn.dispose();
            timeBtn = null;
        }

        NsharpLoadDialog ldDia = NsharpLoadDialog.getAccess();
        ldDia.cleanSndTypeList();

        if (availableFileList != null) {
            availableFileList.removeListener(SWT.Selection,
                    availableFileList.getListeners(SWT.Selection)[0]);
            availableFileList.dispose();
            availableFileList = null;
        }

        if (availableFileGp != null) {
            availableFileGp.dispose();
            availableFileGp = null;
        }
        if (sndTimeList != null) {
            sndTimeList.removeListener(SWT.Selection,
                    sndTimeList.getListeners(SWT.Selection)[0]);
            sndTimeList.dispose();
            sndTimeList = null;
        }
        if (sndTimeListGp != null) {
            sndTimeListGp.dispose();
            sndTimeListGp = null;
        }
        if (bottomGp != null) {
            bottomGp.dispose();
            bottomGp = null;
        }
        if (topGp != null) {
            topGp.dispose();
            topGp = null;
        }

        if (loadBtn != null) {
            loadBtn.removeListener(SWT.MouseUp,
                    loadBtn.getListeners(SWT.MouseUp)[0]);
            loadBtn.dispose();
            loadBtn = null;
        }
        if (stationBtn != null) {
            stationBtn.removeListener(SWT.MouseUp,
                    stationBtn.getListeners(SWT.MouseUp)[0]);
            stationBtn.dispose();
            stationBtn = null;
        }
        if (latlonBtn != null) {
            latlonBtn.removeListener(SWT.MouseUp,
                    latlonBtn.getListeners(SWT.MouseUp)[0]);
            latlonBtn.dispose();
            latlonBtn = null;
        }
        if (locationText != null) {
            locationText.removeListener(SWT.Verify,
                    locationText.getListeners(SWT.Verify)[0]);
            locationText.dispose();
            locationText = null;
        }

        if (locationLbl != null) {
            locationLbl.dispose();
            locationLbl = null;
        }
        if (locationMainGp != null) {
            locationMainGp.dispose();
            locationMainGp = null;
        }
    }

    public static Map<String, String> getGridModelToRscDefNameMap() {
        if (gridModelToRscDefNameMap.isEmpty()) {
            try {
                createModelTypeToRscDefNameMapping();
            } catch (VizException e) {
                statusHandler
                        .handle(Priority.ERROR,
                                "NsharpModelSoundingDialogContents: exception while createModelTypeToRscDefNameMapping.",
                                e);
            }
        }
        return gridModelToRscDefNameMap;
    }

    public static Map<String, String> getRscDefNameToGridModelMap() {
        if (rscDefNameToGridModelMap.isEmpty()) {
            try {
                createModelTypeToRscDefNameMapping();
            } catch (VizException e) {
                statusHandler
                        .handle(Priority.ERROR,
                                "NsharpModelSoundingDialogContents: exception while createModelTypeToRscDefNameMapping.",
                                e);
            }
        }
        return rscDefNameToGridModelMap;
    }

}
