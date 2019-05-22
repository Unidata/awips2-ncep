/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.viz.gempak.common;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.raytheon.uf.common.dataquery.requests.DbQueryRequest;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint.ConstraintType;
import com.raytheon.uf.common.dataquery.responses.DbQueryResponse;
import com.raytheon.uf.common.geospatial.ISpatialObject;
import com.raytheon.uf.common.gridcoverage.Corner;
import com.raytheon.uf.common.gridcoverage.GridCoverage;
import com.raytheon.uf.common.status.IPerformanceStatusHandler;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.PerformanceStatus;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.requests.ThriftClient;
import com.raytheon.uf.viz.gempak.common.data.GempakDataInput;
import com.raytheon.uf.viz.gempak.common.data.GempakDbDataResponse;
import com.raytheon.uf.viz.gempak.common.data.GempakNavigationResponse;
import com.raytheon.uf.viz.gempak.common.data.retriever.IGempakDataURIRetriever;
import com.raytheon.uf.viz.gempak.common.data.retriever.IGempakDbDataRetriever;
import com.raytheon.uf.viz.gempak.common.data.retriever.IGempakNavigationRetriever;
import com.raytheon.uf.viz.gempak.common.data.retriever.IGempakSubgridCoverageRetriever;
import com.raytheon.uf.viz.gempak.common.exception.DgdrivException;
import com.raytheon.uf.viz.gempak.common.exception.GempakException;
import com.raytheon.uf.viz.gempak.common.request.GempakDataURIRequest;
import com.raytheon.uf.viz.gempak.common.request.GempakDbDataRequest;
import com.raytheon.uf.viz.gempak.common.request.GempakNavigationRequest;
import com.raytheon.uf.viz.gempak.common.request.GempakSubgridCoverageRequest;
import com.raytheon.uf.viz.ncep.grid.FloatGridData;
import com.raytheon.uf.viz.ncep.grid.NcgribLogger;
import com.raytheon.uf.viz.ncep.grid.util.GridDBConstants;
import com.sun.jna.Native;
import com.sun.jna.ptr.FloatByReference;
import com.sun.jna.ptr.IntByReference;

import gov.noaa.nws.ncep.viz.gempak.grid.jna.GridDiag;
import gov.noaa.nws.ncep.viz.gempak.grid.jna.GridDiag.gempak;
import gov.noaa.nws.ncep.viz.gempak.util.CommonDateFormatUtil;

/**
 * The Dgdriv class provides setters GEMPAK for grid diagnostic parameters and
 * then executes the grid retrieval methods.
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer        Description
 * ------------ ----------  -----------     --------------------------
 * 3/2009       168         T. Lee          Initial creation
 * 4/2010                   mgamazaychikov  Added returnedObject, changed returned
 *                                          error message from StringDataRecord to String,
 *                                          added return object type NcFloatDataVector;
 *                                          added flipping of the data.
 * 6/2010       168         mgamazaychikov  Changed from NcFloatDataVector to NcFloatDataRecord
 *                                          took out listing of the data
 * 10/2010      168         mgamazaychikov  Moved from uengine, added dbtimeToDattim,
 *                                          flipData, getSErver methods
 * 10/2010      168         mgamazaychikov  Added call to db_wsetnavtime_ native function
 * 10/2010      277         M. Li           Add min and max printout
 * 11/2010      168         mgamazaychikov  Added call to db_init
 * 01/2011                  M. Li           Fix vector scale problem
 * 02/2011      168         mgamazaychikov  removed call to db_wsetserver_, getServer()
 * 03/2011                  M. Li           Add log and dataSource
 * 03/2011      168         mgamazaychikov  add inner class for callback functionality
 * 04/2011      168         mgamazaychikov  add flipping of data returned _to_ GEMPAK, and
 *                                          flopping of data returned _from_ GEMPAK
 * 04/2011                  M. Li           replace gvect and gfunc with gdpfun
 * 05/2011      168         mgamazaychikov  re-implemented data retrieval callback
 *                                          functionality in inner class
 * 06/2011      168         mgamazaychikov  added callback classes for dataURI, navigation
 *                                          retrievals, and for diagnostic (debug and info)
 *                                          messages from gempak.so
 * 09/2011      168         mgamazaychikov  added callback class for ensemble member name
 *                                          retrievals, set native logging from Dgdriv,
 *                                          made changes for ensemble functionality.
 * 10/2011      168         mgamazaychikov  added methods to removed dependency on datatype.tbl
 * 10/2011                  X. Guo          Added grib inventory
 * 11/22/2011               X. Guo          Re-contrain datauri request map
 * 12/12/2011               X. Guo          Updated Ensemble requests
 * 01/12/2011               X. Guo          Updated getFilename()
 * 02/02/2012               X. Guo          Updated query ensemble navigation
 * 03/13/2012               X. Guo          Clean up
 * 03/28/2012               X. Guo          Don't need to convert gdfile toUppercase
 * 05/10/2012               X. Guo          Calculated sub-grid
 * 05/15/2012               X. Guo          Used the new NcGribInventory()
 * 05/23/2012               X. Guo          Loaded ncgrib logger
 * 06/07/2012               X. Guo          Catch datauri&grid data, use DbQueryResponse to query
 *                                          dataURI instead of ScriptCreator
 * 09/06/2012               X. Guo          Query glevel2
 * 09/26/2012               X. Guo          Fixed missing value problems
 * 04/26/2013               B. Yin          Fixed the world wrap problems.
 * 11/2013      845         T. Lee          Implemented parameter scaling
 * 04/11/2014   981         D.Sushon        Code cleanup, enforce explicit use of curly-brackets {}
 * 04/14/2014               S. Gilbert      Remove dataUri from query - cleanup old unused methods.
 * 07/2014                  T. Lee          Fixed specific humidity scaling for NAM
 * 01/26/2016   9817        dgilling        Ensure primary models' coverage is used for ensemble
 *                                          calculations.
 * 04/26/2016   R17741      S. Gilbert      Change to use FloatGridData, and replace NcepLogger
 * 08/18/2016   R17569      K Bugenhagen    Modified calls to NcEnsembleResourceData methods
 *                                          since they are no longer static.
 * 03/27/2017   R19634      bsteffen        Support subgrids.
 * 09/05/2018   54480       mapeters        Updates to support either running in CAVE or in subprocess
 * 09/26/2018   54483       mapeters        Extracted out data URI and DB data retrieval
 * 10/23/2018   54483       mapeters        Use {@link IPerformanceStatusHandler}
 * 10/25/2018   54483       mapeters        Extracted out navigation and subgrid retrieval
 * 01/11/2019   57970       mrichardson     Updated to indicate missing data as NPE culprit
 * 02/01/2019   7720        mrichardson     Incorporate changes for subgrids.
 * </pre>
 *
 * @author tlee
 */

public class Dgdriv {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(Dgdriv.class);

    private static final IPerformanceStatusHandler perfLog = PerformanceStatus
            .getHandler(Dgdriv.class.getSimpleName() + ":");

    private final IGempakDataURIRetriever dataURIRetriever;

    private final IGempakDbDataRetriever dbDataRetriever;

    private final IGempakNavigationRetriever navigationRetriever;

    private final IGempakSubgridCoverageRetriever subgridCoverageRetriever;

    private static GridDiag gd;

    private String ensembleMember;

    private String gdfile, gdpfun, glevel, gvcord, scale, garea, dataSource;

    private String gempakTime;

    /*
     * TODO: flip/flop are always true and are therefore unnecessary. They
     * should either be removed if it is correct for them to always be true, or
     * updated to be false when appropriate.
     */
    private boolean scalar, arrowVector, flop, flip;

    private String gdfileOriginal;

    private ISpatialObject spatialObj, subgSpatialObj;

    private List<DataTime> dataForecastTimes;

    private static NcgribLogger ncgribLogger = NcgribLogger.getInstance();

    // Max # grid points
    public static final int LLMXGD = 8_000_000;

    /*
     * TODO Work around solution - need to find a way to set logging level
     * programmatically
     */
    private static String[] nativeLogTypes = { "|critical", "|error", "|info",
            "|debug" };

    private static final int NATIVE_LOG_LEVEL = 10;

    // ENSEMBLE Calculation flag
    private static boolean isEnsCategory = false;

    public Dgdriv(GempakDataInput dataInput,
            IGempakDataURIRetriever dataURIRetriever,
            IGempakDbDataRetriever dbDataRetriever,
            IGempakNavigationRetriever navigationRetriever,
            IGempakSubgridCoverageRetriever subgridCoverageRetriever) {
        /*
         * Initialize GEMPLT, DG and grid libraries.
         */
        this.dataURIRetriever = dataURIRetriever;
        this.dbDataRetriever = dbDataRetriever;
        this.navigationRetriever = navigationRetriever;
        this.subgridCoverageRetriever = subgridCoverageRetriever;
        gd = GridDiag.getInstance();
        flop = true;
        flip = true;
        setEnsembleMember(dataInput.getEnsembleMember());
        setCycleForecastTimes(dataInput.getCycleForecastTimes());
        setSpatialObject(dataInput.getSpatialObject());
        setGdattim(dataInput.getGdattim());
        setGarea(dataInput.getGarea());
        setProj(dataInput.getProj());
        setGdfile(dataInput.getGdfile());
        setGdpfun(dataInput.getGdpfun());
        setGlevel(dataInput.getGlevel());
        setGvcord(dataInput.getGvcord());
        setScale(dataInput.getScale());
        setDataSource(dataInput.getDataSource());
        setScalar(dataInput.isScalar());
        setArrowVector(dataInput.isArrowVector());
    }

    public void setEnsembleMember(String ensembleMember) {
        this.ensembleMember = ensembleMember;
    }

    public String getGdpfun() {
        return gdpfun;
    }

    public void setGdpfun(String gdpfun) {
        this.gdpfun = gdpfun;
    }

    public boolean isScalar() {
        return scalar;
    }

    public void setScalar(boolean scalar) {
        this.scalar = scalar;
    }

    public boolean isArrowVector() {
        return arrowVector;
    }

    public void setArrowVector(boolean arrowVector) {
        this.arrowVector = arrowVector;
    }

    public void setGdfile(String gdfile) {
        this.gdfile = gdfile;
        this.gdfileOriginal = gdfile;
    }

    public void setGdattim(String gdattim) {
        this.gempakTime = CommonDateFormatUtil.dbtimeToDattim(gdattim);
    }

    public void setGlevel(String glevel) {
        this.glevel = glevel;
    }

    public void setGvcord(String gvcord) {
        this.gvcord = gvcord;
    }

    public void setScale(String scale) {
        this.scale = scale;
    }

    public void setGarea(String garea) {
        this.garea = garea.toUpperCase();
    }

    public void setProj(String proj) {
        this.proj = proj.toUpperCase();
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource.trim().toUpperCase();
        if (this.dataSource.contains("GEMPAK")) {
            flop = true;
        }
    }

    public void setSpatialObject(ISpatialObject spatialObject) {
        this.spatialObj = spatialObject;
        setGridFlip(spatialObject);
    }

    public ISpatialObject getSubgSpatialObj() {
        return this.subgSpatialObj;
    }

    public void setSubgSpatialObj(ISpatialObject obj) {
        this.subgSpatialObj = obj;
    }

    private static final int BUFRLENGTH = 128;

    private static final int PRMLENGTH = 40;

    private static final int IMISSD = -9999;

    private IntByReference iret = new IntByReference(0);

    private IntByReference ier = new IntByReference(0);

    private IntByReference iscale = new IntByReference(0);

    private IntByReference iscalv = new IntByReference(0);

    private IntByReference chngnv = new IntByReference(1);

    private IntByReference coladd = new IntByReference(0);

    private IntByReference gottm = new IntByReference(0);

    private IntByReference drpflg = new IntByReference(0);

    private IntByReference level1 = new IntByReference(0);

    private IntByReference level2 = new IntByReference(0);

    private IntByReference ivcord = new IntByReference(0);

    private IntByReference maxgrd = new IntByReference(LLMXGD);

    private IntByReference ix1 = new IntByReference(0);

    private IntByReference iy1 = new IntByReference(0);

    private IntByReference ix2 = new IntByReference(0);

    private IntByReference iy2 = new IntByReference(0);

    private IntByReference kx = new IntByReference(IMISSD);

    private IntByReference ky = new IntByReference(IMISSD);

    private IntByReference igx = new IntByReference(0);

    private IntByReference igy = new IntByReference(0);

    private IntByReference numerr = new IntByReference(0);

    private FloatByReference rmax = new FloatByReference(0.F);

    private FloatByReference rmin = new FloatByReference(0.F);

    private String proj = "CED";

    private String satfil = "";

    private String skip = "N";

    private byte[] pfunc = new byte[BUFRLENGTH];

    private byte[] parmu = new byte[PRMLENGTH];

    private byte[] parmv = new byte[PRMLENGTH];

    private byte[] time = new byte[PRMLENGTH];

    private byte[] time1 = new byte[BUFRLENGTH];

    private byte[] time2 = new byte[BUFRLENGTH];

    private boolean proces = true;

    private class DiagnosticsCallback implements gempak.DbCallbackWithMessage {

        @Override
        public boolean callback(String msg) {
            long t0 = System.currentTimeMillis();
            String sep = "::";

            int lvl = checkNativeLoggerLevel(msg);
            if (lvl > NATIVE_LOG_LEVEL) {
                return true;
            }

            if (msg.contains("|debug")) {
                String logMessage = msg.split("\\|")[0] + ":"
                        + msg.split(sep)[1];
                statusHandler.debug("C DEBUG MESSAGE " + logMessage);
            } else if (msg.contains("|info")) {
                String logMessage;
                if (msg.split("\\|").length > 2) {
                    logMessage = msg.split("\\|")[0] + ":" + msg.split(sep)[1];
                } else {
                    logMessage = msg;
                }
                statusHandler.debug("C INFO MESSAGE " + logMessage);
            } else if (msg.contains("|error")) {
                String logMessage = msg.split("\\|")[0] + ":"
                        + msg.split(sep)[1];
                statusHandler.error("C ERROR MESSAGE " + logMessage);
            }
            long t1 = System.currentTimeMillis();
            perfLog.logDuration("Handling diagnostics callback for " + msg,
                    t1 - t0);

            return true;
        }
    }

    private class ReturnDataCallback implements gempak.DbCallbackWithMessage {

        @Override
        public boolean callback(String msg) {
            if (msg.contains("/")) {
                if (ncgribLogger.isEnableDiagnosticLogs()) {
                    statusHandler.debug(" enter ReturnDataCallback:" + msg);
                }
                String[] msgArr = msg.split(";");
                if (msgArr.length == 3) {
                    String dataURI = msgArr[0];
                    int nx = Integer.parseInt(msgArr[1].trim());
                    int ny = Integer.parseInt(msgArr[2].trim());
                    long t0 = System.currentTimeMillis();
                    GempakDbDataRequest gempakRequest = new GempakDbDataRequest(
                            dataURI, nx, ny, flip);
                    GempakDbDataResponse response = null;
                    try {
                        response = dbDataRetriever.getDbData(gempakRequest);
                        if (response == null) {
                            statusHandler
                                    .warn("Null response for GEMPAK DB data request indicates missing data for: "
                                            + gempakRequest.getDataURI());
                        }
                    } catch (GempakException e) {
                        statusHandler
                                .error("Error performing GEMPAK DB data request for "
                                        + msg, e);
                    }
                    if (response == null) {
                        proces = false;
                    } else {
                        float[] data = response.getData();
                        /*
                         * For ensemble calculations, we need to be sure we
                         * apply the primary model's grid coverage as the
                         * coverage used for the entire ensemble calculation.
                         * Fortunately, the native lib always passes the primary
                         * model through this interface first when performing an
                         * ensemble calculation, So, the first time we pass
                         * through this code will be when subgSpatialObj has its
                         * initial value: null.
                         */
                        if (subgSpatialObj == null
                                && response.getSubgSpatialObj() != null) {
                            ISpatialObject newSubgSpatialObj = response
                                    .getSubgSpatialObj();
                            if (NcgribLogger.getInstance()
                                    .isEnableDiagnosticLogs()) {
                                statusHandler.debug("===new coverage nx:"
                                        + newSubgSpatialObj.getNx() + " ny:"
                                        + newSubgSpatialObj.getNy());
                            }
                            setSubgSpatialObj(newSubgSpatialObj);
                        }
                        IntByReference datSize = new IntByReference(
                                data.length);
                        gd.gem.db_returndata(data, datSize);
                    }
                    long t1 = System.currentTimeMillis();
                    perfLog.logDuration("Returning data for " + msg, t1 - t0);
                    return true;
                } else {
                    proces = false;
                    return true;
                }
            } else {
                return true;
            }
        }
    }

    private class ReturnNavigationCallback
            implements gempak.DbCallbackWithMessage {

        @Override
        public boolean callback(String msg) {
            String navigationString = null;
            statusHandler.debug("request navigation for: " + msg);
            long t0 = System.currentTimeMillis();
            try {
                if (gdfile.startsWith("{") && gdfile.endsWith("}")) {
                    navigationString = getEnsembleNavigation(msg);
                } else {
                    navigationString = getNavigation(spatialObj);
                }
                gd.gem.db_returnnav(navigationString);
                long t1 = System.currentTimeMillis();
                perfLog.logDuration("Returning grid navigation "
                        + navigationString + " for " + msg, t1 - t0);
            } catch (GempakException e) {
                statusHandler.error("Error getting grid navigation for " + msg,
                        e);
                proces = false;
            }
            return true;
        }
    }

    private class ReturnDataURICallback
            implements gempak.DbCallbackWithMessage {

        @Override
        public boolean callback(String msg) {
            long t0 = System.currentTimeMillis();
            GempakDataURIRequest gempakRequest = new GempakDataURIRequest(msg,
                    isEnsCategory, ensembleMember);
            try {
                String dataURI = dataURIRetriever.getDataURI(gempakRequest);
                gd.gem.db_returnduri(dataURI);
                long t1 = System.currentTimeMillis();
                perfLog.logDuration(
                        "Returning data URI " + dataURI + " for " + msg,
                        t1 - t0);
            } catch (GempakException e) {
                statusHandler.error(
                        "Error performing GEMPAK data URI request for " + msg,
                        e);
                proces = false;
            }
            return true;
        }
    }

    private class ReturnSubgridCRSCallback
            implements gempak.DbCallbackWithMessage {

        @Override
        public boolean callback(String msg) {
            long t0 = System.currentTimeMillis();
            if (ncgribLogger.isEnableDiagnosticLogs()) {
                statusHandler.debug("Rcv'd new subg:" + msg);
            }

            GempakSubgridCoverageRequest request = new GempakSubgridCoverageRequest(
                    spatialObj, msg);
            if (subgSpatialObj == null) {
                try {
                    ISpatialObject subgridCoverage = subgridCoverageRetriever
                            .getSubgridCoverage(request);
                    setSubgSpatialObj(subgridCoverage);
                    long t1 = System.currentTimeMillis();
                    perfLog.logDuration("Returning subgrid CRS for " + msg,
                            t1 - t0);
                } catch (GempakException e) {
                    statusHandler
                            .error("Error performing GEMPAK subgrid coverage request for "
                                    + msg, e);
                    proces = false;
                }
            }
            return true;
        }
    }

    private class ReturnCycleForecastHoursCallback
            implements gempak.DbCallbackWithoutMessage {

        @Override
        public boolean callback() {
            long t0 = System.currentTimeMillis();
            String cycleFcstHrsString = getCycleFcstHrsString(
                    dataForecastTimes);
            gd.gem.db_returnfhrs(cycleFcstHrsString);
            long t1 = System.currentTimeMillis();
            perfLog.logDuration("Returning cycle forecast hours string "
                    + cycleFcstHrsString, t1 - t0);
            return true;
        }
    }

    private class ReturnFileNameCallback
            implements gempak.DbCallbackWithMessage {

        @Override
        public boolean callback(String msg) {
            long t0 = System.currentTimeMillis();
            String fileNames = executeScript(msg);
            gd.gem.db_returnflnm(fileNames);
            long t1 = System.currentTimeMillis();
            perfLog.logDuration(
                    "Returning file names string " + fileNames + " for " + msg,
                    t1 - t0);
            return true;
        }
    }

    public FloatGridData execute() throws DgdrivException {

        DiagnosticsCallback diagCallback = null;
        ReturnFileNameCallback flnmCallback = null;
        ReturnCycleForecastHoursCallback fhrsCallback = null;
        ReturnNavigationCallback navCallback = null;
        ReturnDataCallback dataCallback = null;
        ReturnDataURICallback duriCallback = null;
        ReturnSubgridCRSCallback subgCrsCallback = null;
        /*
         * Tag the gdfile string for ncgrib dataSource
         */
        this.gdfile = getTaggedGdfile();
        proces = true;

        /*
         * TODO Work around solution - need to find away to set logging level
         * programmatically
         */

        diagCallback = new DiagnosticsCallback();
        gd.gem.db_diagCallback(diagCallback);

        flnmCallback = new ReturnFileNameCallback();
        gd.gem.db_flnmCallback(flnmCallback);
        fhrsCallback = new ReturnCycleForecastHoursCallback();
        gd.gem.db_fhrsCallback(fhrsCallback);
        navCallback = new ReturnNavigationCallback();
        gd.gem.db_navCallback(navCallback);
        dataCallback = new ReturnDataCallback();
        gd.gem.db_dataCallback(dataCallback);
        duriCallback = new ReturnDataURICallback();
        gd.gem.db_duriCallback(duriCallback);
        subgCrsCallback = new ReturnSubgridCRSCallback();
        gd.gem.db_subgCrsCallback(subgCrsCallback);
        long t0 = System.currentTimeMillis();

        gd.gem.in_bdta_(iret);
        if (iret.getValue() != 0) {
            throw new DgdrivException(
                    "From in_bdta: error initializing GEMPAK");
        }

        gd.gem.gd_init_(iret);
        if (iret.getValue() != 0) {
            throw new DgdrivException(
                    "From gd_init: error initializing Grid library common area");
        }

        IntByReference mode = new IntByReference(0);
        gd.gem.gg_init_(mode, iret);
        if (iret.getValue() != 0) {
            throw new DgdrivException("From gg_init: error starting GPLT");
        }

        gd.gem.dg_intl_(iret);
        if (iret.getValue() != 0) {
            throw new DgdrivException(
                    "From dg_intl: error initializing Grid diagnostics common block");
        }

        gd.gem.db_init_(iret);
        if (iret.getValue() != 0) {
            throw new DgdrivException(
                    "From db_init: error initializing DB common area");
        }

        String currentTime = gempakTime;
        gd.gem.db_wsetnavtime_(currentTime, iret);
        if (iret.getValue() != 0) {
            throw new DgdrivException(
                    "From db_wsetnavtime: error setting the navigation time "
                            + currentTime);
        }

        /*
         * Process the gdfile string for ensemble request
         */
        boolean isEns = true;
        if (this.gdfile.startsWith("{") && this.gdfile.endsWith("}")) {
            prepareEnsembleDTInfo();
            isEnsCategory = true;
        } else {
            isEns = false;
            prepareGridDTInfo();
        }

        long t01 = System.currentTimeMillis();
        perfLog.logDuration("init and settime", t01 - t0);
        /*
         * Process the GDFILE input.
         */
        if (proces) {

            if (gdfile.contains(":")) {
                if (!isEns) {
                    gd.gem.db_wsetevtname_(gdfile.split(":")[1], iret);
                }
            }

            gd.gem.dgc_nfil_(gdfile, "", iret);

            if (iret.getValue() != 0) {
                gd.gem.erc_wmsg("DG", iret, "", ier);
                proces = false;
            }
        }

        long t02 = System.currentTimeMillis();
        perfLog.logDuration("dgc_nfil", t02 - t01);

        /*
         * Process the GDATTIM input; setup the time server.
         */
        if (proces) {
            gd.gem.dgc_ndtm_(gempakTime, iret);
            if (iret.getValue() != 0) {
                gd.gem.erc_wmsg("DG", iret, "", ier);
                proces = false;
            }
        }

        long t03 = System.currentTimeMillis();
        perfLog.logDuration("dgc_ndtm", t03 - t02);

        if (proces) {
            /*
             * Check if GAREA == "grid", if so, then set coladd= false to NOT
             * add a column to globe wrapping grids.
             */
            if (garea.compareToIgnoreCase("GRID") == 0) {
                coladd = new IntByReference(0);
            }
            /*
             * Set the attributes that do not vary within the time loop.
             */
            gd.gem.inc_scal(scale, iscale, iscalv, iret);
            long t03b = System.currentTimeMillis();
            perfLog.logDuration("inc_scal", t03b - t03);

            /*
             * Get the next time to process from time server.
             */
            gd.gem.dgc_ntim_(chngnv, coladd, time1, time2, gottm, iret);
            long t04 = System.currentTimeMillis();
            perfLog.logDuration("dgc_ntim", t04 - t03b);

            if (iret.getValue() != 0) {
                gd.gem.erc_wmsg("DG", iret, "", ier);
                proces = false;
            } else {
                gd.gem.tgc_dual(time1, time2, time, iret);
                long t04b = System.currentTimeMillis();
                perfLog.logDuration("tgc_dual", t04b - t04);
            }
        }

        long t05 = System.currentTimeMillis();

        /*
         * Setup the grid subset that covers the graphics area.
         */
        if (proces && !"dset".equalsIgnoreCase(garea)) {
            gd.gem.ggc_maps(Native.toByteArray(proj), Native.toByteArray(garea), satfil, drpflg, iret);
            if (iret.getValue() != 0) {
                gd.gem.erc_wmsg("DG", iret, "", ier);
                proces = false;
            }

            long t06 = System.currentTimeMillis();
            perfLog.logDuration("ggc_maps took: ", t06 - t05);

            if (proces) {
                gd.gem.dgc_subg_(skip, maxgrd, ix1, iy1, ix2, iy2, iret);
                if (iret.getValue() != 0) {
                    proces = false;
                }
            }
            long t07 = System.currentTimeMillis();
            perfLog.logDuration("dgc_subg", t07 - t06);
        }

        /*
         * Return grid dimension for grid diagnostic calculation.
         */
        if (proces) {
            gd.gem.dg_kxky_(kx, ky, iret);
            if (iret.getValue() != 0) {
                gd.gem.erc_wmsg("DG", iret, "", ier);
                proces = false;
            }
        }

        statusHandler.debug("kx:" + kx.getValue() + "  ky:" + ky.getValue());
        float[] ugrid = null;
        float[] vgrid = null;
        int grid_size = kx.getValue() * ky.getValue();
        if (kx.getValue() > 0 && ky.getValue() > 0) {
            ugrid = new float[grid_size];
            vgrid = new float[grid_size];
        } else {
            proces = false;
        }

        long t08 = System.currentTimeMillis();
        perfLog.logDuration("From dgc_nfil to dgc_grid", t08 - t02);
        /*
         * Compute the requested grid.
         */
        if (proces) {

            if (scalar) {
                gd.gem.dgc_grid_(time, glevel, gvcord, gdpfun, pfunc, ugrid,
                        igx, igy, time1, time2, level1, level2, ivcord, parmu,
                        iret);
            } else {
                gd.gem.dgc_vecr_(time, glevel, gvcord, gdpfun, pfunc, ugrid,
                        vgrid, igx, igy, time1, time2, level1, level2, ivcord,
                        parmu, parmv, iret);
            }
            if (iret.getValue() != 0) {
                gd.gem.erc_wmsg("DG", iret, Native.toString(pfunc), ier);
                proces = false;
            }
        }
        long t09 = System.currentTimeMillis();
        perfLog.logDuration("dgc_grid", t09 - t08);

        /*
         * Compute the scaling factor and scale the grid data.
         */
        if (proces) {
            FloatGridData fds = new FloatGridData();
            IntByReference ix12 = new IntByReference(1);
            IntByReference iy12 = new IntByReference(1);
            if (scalar) {
                gd.gem.grc_sscl(iscale, igx, igy, ix12, iy12, igx, igy, ugrid,
                        rmin, rmax, iret);
            } else if (arrowVector) {
                gd.gem.grc_sscl(iscalv, igx, igy, ix12, iy12, igx, igy, ugrid,
                        rmin, rmax, iret);
            }
            if (flop) {
                fds.setXdata(flopData(ugrid, igx.getValue(), igy.getValue()));
            } else {
                fds.setXdata(revertGempakData2CAVE(ugrid));
            }
            fds.setDimension(2);
            fds.setSizes(new long[] { igx.getValue(), igy.getValue() });
            fds.setVector(false);

            if (!scalar) { // vector
                if (arrowVector) {
                    gd.gem.grc_sscl(iscalv, igx, igy, ix12, iy12, igx, igy,
                            vgrid, rmin, rmax, iret);
                }
                if (flop) {
                    fds.setYdata(
                            flopData(vgrid, igx.getValue(), igy.getValue()));
                } else {
                    fds.setYdata(revertGempakData2CAVE(vgrid));
                }
                fds.setDimension(2);
                fds.setSizes(new long[] { igx.getValue(), igy.getValue() });
                fds.setVector(true);
            }
            /*
             * Free memory for all internal grids
             */
            gd.gem.dg_fall_(iret);
            long t1 = System.currentTimeMillis();
            perfLog.logDuration("Scaling", t1 - t09);
            return fds;
        } else {
            /*
             * Write error message only in case of failure
             */
            gd.gem.er_gnumerr_(numerr, iret);
            int num = numerr.getValue();
            int index = 0;
            byte[] errmsg = new byte[BUFRLENGTH];
            StringBuilder strBuf = new StringBuilder();
            while (index < num) {
                numerr.setValue(index);
                gd.gem.er_gerrmsg_(numerr, errmsg, iret);
                strBuf.append(Native.toString(errmsg));
                strBuf.append("\n");
                index++;
            }
            return null;
        }
    }

    private void prepareGridDTInfo() {
        String alias = this.gdfile;
        String path = "A2DB_GRID";
        statusHandler.debug("prepareGridDTInfo-- alias:" + alias
                + " gdfileOriginal:" + this.gdfileOriginal);
        String template = this.gdfileOriginal + "_db";
        if (this.gdfileOriginal.contains(":")) {
            template = this.gdfileOriginal.substring(0,
                    this.gdfileOriginal.indexOf(':')) + "_db";
        }
        IntByReference iret = new IntByReference(0);

        gd.gem.db_seta2dtinfo_(alias, path, template, iret);

    }

    private void prepareEnsembleDTInfo() {
        String thePath = "A2DB_GRID";
        StringBuilder sba = new StringBuilder();
        StringBuilder sbp = new StringBuilder();
        StringBuilder sbt = new StringBuilder();
        String[] gdfileArray = this.gdfileOriginal
                .substring(this.gdfileOriginal.indexOf('{') + 1,
                        this.gdfileOriginal.indexOf('}'))
                .split(",");
        for (int igd = 0; igd < gdfileArray.length; igd++) {
            sbp.append(thePath);
            sbp.append("|");
            String perturbationNum = null;
            String ensName = null;
            if (gdfileArray[igd].contains("|")) {
                String[] tempArr = gdfileArray[igd].split("\\|");
                if (tempArr[0].contains(":")) {
                    String[] nameArr = tempArr[0].split(":");
                    ensName = nameArr[0];
                    if (nameArr[0].contains("%")) {
                        ensName = nameArr[0].split("%")[1];
                    }
                    perturbationNum = nameArr[1];
                } else {
                    ensName = tempArr[0];
                    if (tempArr[0].contains("%")) {
                        ensName = tempArr[0].split("%")[1];
                    }
                }

                sba.append("A2DB_" + ensName);
                if (perturbationNum != null) {
                    sba.append(":" + perturbationNum);
                }

                sba.append("|");
                sbt.append(getEnsembleTemplate(ensName, perturbationNum));
                sbt.append("|");
            } else {
                if (gdfileArray[igd].contains(":")) {
                    String[] nameArr = gdfileArray[igd].split(":");
                    ensName = nameArr[0];
                    if (nameArr[0].contains("%")) {
                        ensName = nameArr[0].split("%")[1];
                    }
                    perturbationNum = nameArr[1];
                } else {
                    ensName = gdfileArray[igd];
                    if (gdfileArray[igd].contains("%")) {
                        ensName = gdfileArray[igd].split("%")[1];
                    }
                }

                sba.append("A2DB_" + ensName);
                if (perturbationNum != null) {
                    sba.append(":" + perturbationNum);
                }
                sba.append("|");
                sbt.append(getEnsembleTemplate(ensName, perturbationNum));
                sbt.append("|");
            }
        }
        String alias = sba.toString().substring(0, sba.toString().length() - 1);
        String path = sbp.toString().substring(0, sbp.toString().length() - 1);
        String template = sbt.toString().substring(0,
                sbt.toString().length() - 1);
        IntByReference iret = new IntByReference(0);

        statusHandler.debug("prepareEnsembleDTInfo: alias=" + alias + ", path="
                + path + ",template=" + template);
        gd.gem.db_seta2dtinfo_(alias, path, template, iret);
    }

    private String getEnsembleTemplate(String ensName, String perturbationNum) {
        String ensTemplate = ensName + "_db_" + perturbationNum
                + "_YYYYMMDDHHfFFF";

        statusHandler.debug("getEnsembleTemplate(" + ensTemplate + ") for("
                + ensName + "," + perturbationNum + ")");
        return ensTemplate;
    }

    private String getTaggedGdfile() {
        if ("grid".equalsIgnoreCase(this.dataSource)) {
            String tag = "A2DB_";
            /*
             * For gdfile containing event name do not have to do anything as
             * long as the tag is prefixed eg GHM:greg07e becomes
             * A2DB_GHM:greg07e
             */

            /*
             * For gdfile containing ensemble have to preprocess eg {GFS|11/12,
             * NAM|11/06} becomes {A2DB_GFS|11/12, A2DB_NAM|11/06}
             */
            if (this.gdfileOriginal.startsWith("{")
                    && this.gdfileOriginal.endsWith("}")) {
                String[] gdfileArray = this.gdfileOriginal
                        .substring(this.gdfileOriginal.indexOf('{') + 1,
                                this.gdfileOriginal.indexOf('}'))
                        .split(",");
                StringBuilder strb = new StringBuilder();

                for (int igd = 0; igd < gdfileArray.length; igd++) {
                    strb.append(tag);
                    strb.append(gdfileArray[igd]);
                    strb.append(",");

                }
                String retStr = strb.toString().substring(0,
                        strb.toString().length() - 1);
                return "{" + retStr + "}";
            }
            return tag + this.gdfile;
        } else {
            return this.gdfile;
        }
    }

    private String getCycleFcstHrsString(List<DataTime> dataForecastTimes) {
        StringBuilder resultsBuf = new StringBuilder();
        for (DataTime dt : dataForecastTimes) {
            resultsBuf
                    .append(CommonDateFormatUtil.dbtimeToDattim(dt.toString()));
            resultsBuf.append("|");
        }
        return resultsBuf.substring(0, resultsBuf.length() - 1);
    }

    /**
     * Flops the data from GEMPAK order and changes the missing data value from
     * GEMPAK -9999.0f to CAVE -999999.0f
     */
    private float[] flopData(float[] inGrid, int nx, int ny) {
        float[] outGridFlopped = new float[inGrid.length];
        int kk = 0;
        for (int jj = ny - 1; jj >= 0; jj--) {
            int m1 = nx * jj;
            int m2 = m1 + (nx - 1);
            for (int ii = m1; ii <= m2; ii++) {
                if (inGrid[ii] == -9999.0f) {
                    outGridFlopped[kk] = -999999.0f;
                    kk++;
                } else {
                    outGridFlopped[kk] = inGrid[ii];
                    kk++;
                }
            }

        }
        return outGridFlopped;
    }

    /**
     * Revert data from GEMPAK order and changes the missing data value from
     * GEMPAK -9999.0f to CAVE -999999.0f
     */
    private float[] revertGempakData2CAVE(float[] inGrid) {
        float[] outGridFlopped = new float[inGrid.length];

        for (int ii = 0; ii < inGrid.length; ii++) {
            if (inGrid[ii] == -9999.0f) {
                outGridFlopped[ii] = -999999.0f;
            } else {
                outGridFlopped[ii] = inGrid[ii];
            }
        }

        return outGridFlopped;
    }

    public static String getFilename(String dataURI) {
        String filename = null;
        File file = null;
        String[] uriStr = dataURI.split("/");
        String path = uriStr[3];
        StringBuilder sb = new StringBuilder();
        String[] tmStr = uriStr[2].split("_");
        String dataDateStr = tmStr[0];
        int fcstTimeInSec = CommonDateFormatUtil
                .getForecastTimeInSec(uriStr[2]);
        String fcstTimeStr = CommonDateFormatUtil
                .getForecastTimeString(fcstTimeInSec);

        sb.append(path);
        sb.append("-");
        sb.append(dataDateStr);
        String dataTimeStr = tmStr[1].split(":")[0] + "-FH-" + fcstTimeStr;
        sb.append("-");
        sb.append(dataTimeStr);
        sb.append(".h5");

        file = new File(
                // TODO--OK?? VizApp.getServerDataDir() + File.separator +
                dataURI.split("/")[1] + File.separator + path + File.separator
                        + sb.toString());

        if (file != null) {
            filename = file.getAbsolutePath();
        }
        return filename;
    }

    private void setGridFlip(ISpatialObject obj) {

        GridCoverage gc = (GridCoverage) obj;

        if (gc.getFirstGridPointCorner() == Corner.UpperLeft) {
            this.flop = true;
        }
    }

    private String executeScript(String scriptToRun) {

        if (scriptToRun == null) {
            return null;
        }

        statusHandler.debug("executeScript: scriptToRun=" + scriptToRun);
        String[] parms = scriptToRun.split("\\|");
        if (parms.length < 4) {
            return null;
        }

        String modelName = parms[0];
        String dbTag = parms[1];
        String eventName = parms[2];
        String tmStr = constructTimeStr(parms[3]);
        Pattern p = Pattern.compile(tmStr);

        HashMap<String, RequestConstraint> rcMap = new HashMap<>();
        rcMap.put(GridDBConstants.PLUGIN_NAME,
                new RequestConstraint(GridDBConstants.GRID_TBL_NAME));
        rcMap.put(GridDBConstants.MODEL_NAME_QUERY,
                new RequestConstraint(modelName, ConstraintType.EQUALS));
        if (!"null".equalsIgnoreCase(eventName)) {
            rcMap.put(GridDBConstants.ENSEMBLE_ID_QUERY,
                    new RequestConstraint(eventName, ConstraintType.EQUALS));
        }

        try {
            int fhr = CommonDateFormatUtil.getForecastTimeInSec(parms[3]);
            rcMap.put(GridDBConstants.FORECAST_TIME_QUERY,
                    new RequestConstraint(Integer.toString(fhr),
                            ConstraintType.EQUALS));
        } catch (NumberFormatException e1) {
            // Don't worry if fcsthr not specified. we'll get em all
        }

        DbQueryRequest request = new DbQueryRequest();
        request.addRequestField(GridDBConstants.REF_TIME_QUERY);
        request.addRequestField(GridDBConstants.FORECAST_TIME_QUERY);
        request.setDistinct(true);
        request.setConstraints(rcMap);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHH");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        StringBuilder retFileNames = new StringBuilder();
        try {
            DbQueryResponse response = (DbQueryResponse) ThriftClient
                    .sendRequest(request);
            // extract list of results
            List<Map<String, Object>> responseList = null;
            if (response != null) {
                responseList = response.getResults();
            } else {
                // empty list to simplify code
                responseList = new ArrayList<>(0);
            }

            String prefix = modelName + "_" + dbTag + "_" + eventName + "_";
            for (int i = 0; i < responseList.size(); i++) {
                Object fSecValue = responseList.get(i)
                        .get(GridDBConstants.FORECAST_TIME_QUERY);
                Object refValue = responseList.get(i)
                        .get(GridDBConstants.REF_TIME_QUERY);
                if (fSecValue != null && fSecValue instanceof Integer
                        && refValue != null && refValue instanceof Date) {

                    String refString = sdf.format((Date) refValue);
                    statusHandler.debug("executeScript: match " + refString);
                    statusHandler.debug("executeScript:  with " + tmStr);
                    Matcher m = p.matcher(refString);
                    if (!m.matches()) {
                        continue;
                    }

                    int fcstTimeInSec = ((Integer) fSecValue).intValue();
                    DataTime refTime = new DataTime((Date) refValue);
                    String[] dts = refTime.toString().split(" ");

                    String dt = dts[0].replace("-", "");

                    String hh = dts[1].split(":")[0];
                    if (retFileNames.length() > 0) {
                        retFileNames.append("|");
                    }

                    retFileNames
                        .append(prefix)
                        .append(dt)
                        .append(hh)
                        .append("f")
                        .append(CommonDateFormatUtil
                                .getForecastTimeString(fcstTimeInSec));
                }
            }
        } catch (VizException e) {
            statusHandler.error(
                    "DBQueryRequest failed: " + e.getLocalizedMessage(), e);
        }
        return retFileNames.toString();
    }

    private String constructTimeStr(String gempakTimeStr) {
        String gempakTimeStrCycle = gempakTimeStr.split("f")[0];
        gempakTimeStrCycle = gempakTimeStrCycle.replace("[0-9]", ".");
        if (gempakTimeStrCycle.length() < 10) {
            return null;
        }

        String timeStr = gempakTimeStrCycle.substring(0, 4)
                + gempakTimeStrCycle.substring(4, 6)
                + gempakTimeStrCycle.substring(6, 8)
                + gempakTimeStrCycle.substring(8, 10);
        return timeStr;
    }

    public void setCycleForecastTimes(List<DataTime> dataTimes) {
        this.dataForecastTimes = dataTimes;
    }

    private int checkNativeLoggerLevel(String msg) {
        int lvl = 0;
        for (String logType : nativeLogTypes) {
            if (msg.contains(logType)) {
                break;
            }
            lvl++;
        }
        return lvl;
    }

    private String getNavigation(ISpatialObject spatialObject)
            throws GempakException {
        GempakNavigationResponse navigationResponse = navigationRetriever
                .getNavigation(new GempakNavigationRequest(spatialObject));
        if (navigationResponse.isEnableFlip()) {
            flip = true;
        }
        return navigationResponse.getNavigation();
    }

    private String getEnsembleNavigation(String msg) throws GempakException {
        String navStr = null;
        statusHandler.debug("getEnsembleNavigation: " + msg + " dataTime:"
                + dataForecastTimes.get(0).toString());
        String[] tmpAttrs = msg.split("\\|");
        Map<String, RequestConstraint> queryList = new HashMap<>();
        queryList.put(GridDBConstants.PLUGIN_NAME, new RequestConstraint(
                GridDBConstants.GRID_TBL_NAME, ConstraintType.EQUALS));
        queryList.put(GridDBConstants.MODEL_NAME_QUERY,
                new RequestConstraint(tmpAttrs[0], ConstraintType.EQUALS));
        if (tmpAttrs[1] != null && tmpAttrs[1].length() > 0
                && (!"null".equalsIgnoreCase(tmpAttrs[1]))) {
            queryList.put(GridDBConstants.ENSEMBLE_ID_QUERY,
                    new RequestConstraint(tmpAttrs[1], ConstraintType.EQUALS));
        }
        if (tmpAttrs[2] != null && tmpAttrs[2].length() > 0) {
            String navTime = buildRefTime(tmpAttrs[2]);
            statusHandler.debug("getEnsembleNavigation: " + navTime);
            queryList.put(GridDBConstants.DATA_TIME_QUERY,
                    new RequestConstraint(navTime));
        } else {
            queryList.put(GridDBConstants.DATA_TIME_QUERY,
                    new RequestConstraint(dataForecastTimes.get(0).toString()));
        }

        DbQueryRequest request = new DbQueryRequest();
        request.addRequestField(GridDBConstants.NAVIGATION_QUERY);
        request.setLimit(1);
        request.setConstraints(queryList);
        try {
            DbQueryResponse response = (DbQueryResponse) ThriftClient
                    .sendRequest(request);
            // extract list of results
            List<Map<String, Object>> responseList = null;
            if (response != null) {
                responseList = response.getResults();
            } else {
                // empty list to simplify code
                responseList = new ArrayList<>(0);
            }

            ISpatialObject cov = null;
            if (!responseList.isEmpty()) {
                Object spatialObj = responseList.get(0)
                        .get(GridDBConstants.NAVIGATION_QUERY);
                if (spatialObj != null
                        && spatialObj instanceof ISpatialObject) {
                    cov = (ISpatialObject) spatialObj;
                }
            }

            if (cov != null) {
                navStr = getNavigation(cov);
            }
        } catch (VizException e) {
            statusHandler
                    .error("Error performing ensemble grid navigation query for "
                            + msg, e);
        }

        return navStr;
    }

    private String buildRefTime(String navTime) {
        StringBuilder reftime = new StringBuilder();
        String[] dt = navTime.split("f");
        reftime.append(dt[0].substring(0, 4));
        reftime.append("-");
        reftime.append(dt[0].substring(4, 6));
        reftime.append("-");
        reftime.append(dt[0].substring(6, 8));
        reftime.append(" ");
        reftime.append(dt[0].substring(8, dt[0].length()));
        reftime.append(":00:00.0 (");
        int ft = 0;
        if (dt[1] != null && dt[1].length() > 0) {
            ft = Integer.parseInt(dt[1]);
        }
        reftime.append(String.valueOf(ft));
        reftime.append(")");
        return reftime.toString();
    }
}
