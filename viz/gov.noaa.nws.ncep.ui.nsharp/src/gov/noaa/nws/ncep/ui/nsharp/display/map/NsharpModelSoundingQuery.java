package gov.noaa.nws.ncep.ui.nsharp.display.map;

import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingCube;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingProfile;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingProfile.MdlSndType;
import gov.noaa.nws.ncep.ui.nsharp.NsharpConfigManager;
import gov.noaa.nws.ncep.ui.nsharp.NsharpConfigStore;
import gov.noaa.nws.ncep.ui.nsharp.NsharpStationInfo;
import gov.noaa.nws.ncep.ui.nsharp.display.NsharpEditor;
import gov.noaa.nws.ncep.ui.nsharp.display.rsc.NsharpResourceHandler;
import gov.noaa.nws.ncep.ui.nsharp.natives.NsharpDataHandling;
import gov.noaa.nws.ncep.viz.soundingrequest.NcSoundingQuery;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Display;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * 
 * 
 * This java class performs the NSHARP NsharpLoadDialog functions.
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#     Engineer    Description
 * -------      -------     --------    -----------
 * 10/06/2015   RM#10295    Chin Chen   initial coding - moving query and loading code form NsharpModelSoundingDialogContents.java
 *                                      Let sounding data query run in its own thread to avoid gui locked out during load
 * 04/10/2017   DR#30518    nabowle     Run sounding queries in parallel.
 *
 * </pre>
 * 
 * @author Chin Chen
 */
public class NsharpModelSoundingQuery extends Job {
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(NsharpModelSoundingQuery.class);
    
    /**
     * Used to run sounding queries in parallel. CAVE restricts the number of
     * concurrent connections to EDEX, so we can't achieve parallelism
     * beyond that limit, but that limit also prevents us from overwhelming
     * EDEX.
     */
    private static final ExecutorService QUERY_EXECUTOR = Executors
            .newFixedThreadPool(
                    Integer.getInteger("nsharp.concurrent.model.queries", 8));

    public NsharpModelSoundingQuery(String name) {
        super(name);
    }

    private boolean stnQuery;
    private Map<String, List<NcSoundingLayer>> soundingLysLstMap;
    private NsharpEditor skewtEdt;
    private List<String> selectedTimeList;
    private Map<String, String> timeLineToFileMap;
    private float lat, lon;
    private String stnStr;
    private String selectedModelType;
    private String selectedRscDefName;

    /**
     * Query and load the Sounding data.
     * 
     * @param stnQuery
     *            true if this is a station query, false if a point query.
     * @param skewtEdt
     *            The skew editor to display the data on.
     * @param soundingLysLstMap
     *            The map location+times to sounding layers to store results in.
     * @param selectedTimeList
     *            The list of selected times.
     * @param timeLineToFileMap
     * @param lat
     *            The latitude
     * @param lon
     *            The longitude
     * @param stnStr
     *            The station
     * @param selectedModelType
     *            The model name used for database queries.
     * @param selectedRscDefName
     *            The displayed name.
     */
    public void queryAndLoadData(boolean stnQuery, NsharpEditor skewtEdt,
            Map<String, List<NcSoundingLayer>> soundingLysLstMap,
            List<String> selectedTimeList,
            Map<String, String> timeLineToFileMap, float lat, float lon,
            String stnStr, String selectedModelType, String selectedRscDefName) {
        this.stnQuery = stnQuery;
        this.skewtEdt = skewtEdt;
        this.soundingLysLstMap = soundingLysLstMap;
        this.selectedTimeList = selectedTimeList;
        this.timeLineToFileMap = timeLineToFileMap;
        this.lat = lat;
        this.lon = lon;
        this.selectedModelType = selectedModelType;
        this.stnStr = stnStr;
        this.selectedRscDefName = selectedRscDefName;
        if (this.getState() != Job.RUNNING) {
            this.schedule();
        }
    }

    /**
     * Request the sounding cubes for the selected times and display the
     * resource as long as one valid cube was returned.
     * 
     * NcSoundingQueries are run in parallel (limited by the thread limit on
     * {@link #QUERY_EXECUTOR} and the EDEX Http Connections limit) which
     * increases overall throughput when multiple times are selected.
     */
    @Override
    protected IStatus run(IProgressMonitor monitor) {
        try {
            soundingLysLstMap.clear();
            Timestamp refTime = null;
            // Chin Note: Since NcGrib/Grib HDF5 data file is created based
            // on a forecast time line, we can not query more than one time
            // line at one time as Edex server just could not support such
            // query at one shot.
            // This is not the case of PFC sounding (modelsounding db). It
            // has all time lines of one forecast report saved in one file.
            // Therefore, PFC query is much faster.
            List<Future<NcSoundingCube>> runningQueries = new ArrayList<>(selectedTimeList.size());
            for (String timeLine : selectedTimeList) {
                // avail file, ie. its refTime
                String selectedFileStr = timeLineToFileMap.get(timeLine);

                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH");
                df.parse(selectedFileStr);
                Calendar cal = df.getCalendar();
                int offset = cal.get(Calendar.ZONE_OFFSET)
                        + cal.get(Calendar.DST_OFFSET) / (60 * 1000);
                refTime = new Timestamp(cal.getTimeInMillis() + offset);

                String rangeStartStr = NcSoundingQuery
                        .convertSoundTimeDispStringToRangeStartTimeFormat(timeLine);

                NsharpConfigManager mgr = NsharpConfigManager.getInstance();
                NsharpConfigStore configStore = mgr
                        .retrieveNsharpConfigStoreFromFs();
                boolean gridInterpolation;
                if (configStore != null) {
                    gridInterpolation = configStore.getGraphProperty()
                            .isGridInterpolation();
                } else
                    gridInterpolation = true; // by default

                String[] refLTimeStrAry = { selectedFileStr + ":00:00" };
                String[] soundingRangeTimeStrArray = { rangeStartStr };
                Coordinate[] coordArray = { new Coordinate(lon, lat) };
                
                /*
                 * Schedule all of the NcSoundingQueries. The executor service
                 * will handle running these queries in the background and does
                 * not require Future#get() to be called on completed queries
                 * before running more NcSoundingQueries.
                 */
                runningQueries.add(QUERY_EXECUTOR.submit(new Callable<NcSoundingCube>() {
                    @Override
                    public NcSoundingCube call() throws Exception {
                        return NcSoundingQuery.genericSoundingDataQuery(null,
                                null, refLTimeStrAry, soundingRangeTimeStrArray,
                                coordArray, null, MdlSndType.ANY.toString(),
                                NcSoundingLayer.DataType.ALLDATA, false, "-1",
                                selectedModelType, gridInterpolation, false,
                                false);
                    }

                }));
            }
            for (int i = 0; i < runningQueries.size(); i++) {
                String timeLine = selectedTimeList.get(i);
                
                Future<NcSoundingCube> soundingQuery = runningQueries.get(i);
                NcSoundingCube cube;
                try {
                    /*
                     * soundingQuery.get() will immediately return the resultant
                     * Sounding Cube if the query has already completed, or
                     * block this thread until it has finished. If there was an
                     * exception running the query, an ExecutionException is
                     * thrown by get() and handled here.
                     */
                    cube = soundingQuery.get();
                } catch (InterruptedException | ExecutionException e) {
                    statusHandler.handle(Priority.ERROR,
                            "NsharpModelSoundingQuery: exception retrieving sounding cube for "
                                    + timeLine, e);
                    cube = null;
                }
                if (cube != null
                        && cube.getRtnStatus() == NcSoundingCube.QueryStatus.OK) {

                    NcSoundingProfile sndPf = cube.getSoundingProfileList()
                            .get(0);

                    List<NcSoundingLayer> rtnSndLst = sndPf.getSoundingLyLst();
                    if (rtnSndLst != null && rtnSndLst.size() > 1) {
                        // Remove sounding layers that not used by NSHARP
                        rtnSndLst = NsharpDataHandling
                                .organizeSoundingDataForShow(rtnSndLst,
                                        sndPf.getStationElevation());
                        // minimum rtnSndList size will be 2 (50 & 75 mb
                        // layers), but that is not enough.
                        // We need at least 4 regular layers for plotting
                        if (rtnSndLst != null && rtnSndLst.size() > 4) {
                            if (!stnQuery) {
                                soundingLysLstMap.put(lat + "/" + lon + " "
                                        + timeLine, rtnSndLst);
                            } else {
                                // replaced space to _ in stnStr.
                                String stnStrPacked = stnStr.replace(" ", "_");
                                soundingLysLstMap.put(stnStrPacked + " "
                                        + timeLine, rtnSndLst);
                            }
                            continue;
                        }

                    }
                    // code to this point means query result is not good
                    if (!stnQuery) {
                        NsharpSoundingQueryCommon
                                .postToMsgBox("Sounding query with lat/lon ("
                                        + lat + "/" + lon + ") at " + timeLine
                                        + ": Returned\n But without valid data");
                    } else {
                        NsharpSoundingQueryCommon
                                .postToMsgBox("Sounding query with stn "
                                        + stnStr + "at lat/lon (" + lat + "/"
                                        + lon + ") at " + timeLine
                                        + ": Returned\n But without valid data");
                    }
                } else {
                    if (!stnQuery) {
                        if (cube != null)
                            NsharpSoundingQueryCommon
                                    .postToMsgBox("Sounding query at lat/lon("
                                            + lat + "/" + lon + ") at "
                                            + timeLine
                                            + ": failed\nError status:"
                                            + cube.getRtnStatus().toString());
                        else
                            NsharpSoundingQueryCommon
                                    .postToMsgBox("Sounding query at lat/lon("
                                            + lat + "/" + lon + ") at "
                                            + timeLine
                                            + ": failed\nError status:"
                                            + "NULL rtned");
                    } else {
                        if (cube != null)
                            NsharpSoundingQueryCommon
                                    .postToMsgBox("Sounding query with stn "
                                            + stnStr + "at lat/lon (" + lat
                                            + "/" + lon + ") at " + timeLine
                                            + ": failed\nError status:"
                                            + cube.getRtnStatus().toString());
                        else
                            NsharpSoundingQueryCommon
                                    .postToMsgBox("Sounding query with stn "
                                            + stnStr + "at lat/lon (" + lat
                                            + "/" + lon + ") at " + timeLine
                                            + ": failed\nError status:"
                                            + "NULL rtned");
                    }
                }
            }
            if (soundingLysLstMap.size() > 0) {
                NsharpResourceHandler skewRsc = skewtEdt.getRscHandler();
                NsharpStationInfo stnInfo = new NsharpStationInfo();
                stnInfo.setSndType(selectedRscDefName);
                stnInfo.setLatitude(lat);
                stnInfo.setLongitude(lon);
                stnInfo.setStnId(stnStr);
                stnInfo.setReftime(refTime);
                skewRsc.addRsc(soundingLysLstMap, stnInfo);
                skewRsc.setSoundingType(selectedRscDefName);
                try {
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            NsharpEditor.bringEditorToTop();
                        }
                    });
                } catch (SWTException e) {
                    statusHandler
                            .handle(Priority.ERROR,
                                    "NsharpModelSoundingQuery: exception when trying to bringEditorToTop",
                                    e);
                }
            }
        } catch (ParseException e) {
            statusHandler
                    .handle(Priority.ERROR,
                            "NsharpModelSoundingQuery: exception when parsing refTime string",
                            e);
        }
        return Status.OK_STATUS;
    }
}