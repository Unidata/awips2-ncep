package gov.noaa.nws.ncep.viz.rsc.ncgrid.dgdriv;

/**
 * 
 */

import gov.noaa.nws.ncep.viz.common.util.CommonDateFormatUtil;
import gov.noaa.nws.ncep.viz.gempak.grid.jna.GridDiag;
import gov.noaa.nws.ncep.viz.gempak.grid.jna.GridDiag.gempak;
import gov.noaa.nws.ncep.viz.gempak.grid.units.GempakGridParmInfoLookup;
import gov.noaa.nws.ncep.viz.gempak.grid.units.GempakGridVcrdInfoLookup;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.FloatGridData;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.NcgribLogger;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.customCoverage.CustomLambertConformalCoverage;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.customCoverage.CustomLatLonCoverage;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.customCoverage.CustomMercatorCoverage;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.customCoverage.CustomPolarStereoCoverage;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.dgdriv.NcgridDataCache.NcgridData;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.rsc.NcEnsembleResourceData;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.rsc.NcgridResourceData;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.measure.converter.ConversionException;

import org.geotools.coverage.grid.GridGeometry2D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.comm.CommunicationException;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.grid.dataquery.GridQueryAssembler;
import com.raytheon.uf.common.dataplugin.grid.datastorage.GridDataRetriever;
import com.raytheon.uf.common.dataquery.requests.DbQueryRequest;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint.ConstraintType;
import com.raytheon.uf.common.dataquery.responses.DbQueryResponse;
import com.raytheon.uf.common.datastorage.StorageException;
import com.raytheon.uf.common.datastorage.records.FloatDataRecord;
import com.raytheon.uf.common.geospatial.ISpatialObject;
import com.raytheon.uf.common.geospatial.util.GridGeometryWrapChecker;
import com.raytheon.uf.common.gridcoverage.Corner;
import com.raytheon.uf.common.gridcoverage.GridCoverage;
import com.raytheon.uf.common.gridcoverage.LambertConformalGridCoverage;
import com.raytheon.uf.common.gridcoverage.LatLonGridCoverage;
import com.raytheon.uf.common.gridcoverage.MercatorGridCoverage;
import com.raytheon.uf.common.gridcoverage.PolarStereoGridCoverage;
import com.raytheon.uf.common.gridcoverage.exception.GridCoverageException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.units.UnitConv;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.requests.ThriftClient;
import com.sun.jna.Native;
import com.sun.jna.ptr.FloatByReference;
import com.sun.jna.ptr.IntByReference;

/**
 * The Dgdriv class provides setters GEMPAK for grid diagnostic parameters and
 * then executes the grid retrieval methods.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date			Ticket#		Engineer    	Description
 * ------------ ----------	----------- 	--------------------------
 * 3/2009 		168			T. Lee			Initial creation
 * 4/2010					mgamazaychikov	Added returnedObject, changed returned 
 * 											error message from StringDataRecord to String,
 * 											added return object type NcFloatDataVector;
 * 											added flipping of the data.
 * 6/2010		168			mgamazaychikov	Changed from NcFloatDataVector to NcFloatDataRecord
 * 											took out listing of the data
 * 10/2010		168			mgamazaychikov	Moved from uengine, added dbtimeToDattim, 
 * 											flipData, getSErver methods
 * 10/2010		168			mgamazaychikov	Added call to db_wsetnavtime_ native function
 * 10/2010	    277			M. Li			Add min and max printout
 * 11/2010		168			mgamazaychikov	Added call to db_init
 * 01/2011					M. Li			Fix vector scale problem
 * 02/2011      168         mgamazaychikov  removed call to db_wsetserver_, getServer()
 * 03/2011                  M. Li			Add log and dataSource
 * 03/2011      168         mgamazaychikov  add inner class for callback functionality
 * 04/2011		168			mgamazaychikov	add flipping of data returned _to_ GEMPAK, and
 * 											flopping of data returned _from_ GEMPAK
 * 04/2011					M. Li			replace gvect and gfunc with gdpfun
 * 05/2011		168			mgamazaychikov  re-implemented data retrieval callback 
 * 											functionality in inner class
 * 06/2011		168			mgamazaychikov	added callback classes for dataURI, navigation 
 * 											retrievals, and for diagnostic (debug and info) 
 * 											messages from gempak.so
 * 09/2011		168			mgamazaychikov	added callback class for ensemble member name
 * 											retrievals, set native logging from Dgdriv,
 * 											made changes for ensemble functionality.
 * 10/2011		168			mgamazaychikov	added methods to removed dependency on datatype.tbl
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
 * 11/2013       845        T. Lee          Implemented parameter scaling
 * 04/11/2014    981        D.Sushon        Code cleanup, enforce explicit use of curly-brackets {}
 * 04/14/2014               S. Gilbert      Remove dataUri from query - cleanup old unused methods.
 * 07/2014                  T. Lee          Fixed specific humidity scaling for NAM
 * 01/26/2016    9817       dgilling        Ensure primary models' coverage is used for ensemble
 *                                          calculations.
 * 04/26/2016    R17741     S. Gilbert      Change to use FloatGridData, and replace NcepLogger
 * 08/18/2016    R17569     K Bugenhagen    Modified calls to NcEnsembleResourceData methods 
 *                                          since they are no longer static.
 * 10/23/2018               mjames@ucar     Remove logging for speed.
 * </pre>
 * 
 * @author tlee
 * @version 1.0
 */

public class Dgdriv {
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(Dgdriv.class);

    private static GridDiag gd;

    private String gdfile, gdpfun, glevel, gvcord, scale, garea, dataSource;

    private String gempakTime;

    private boolean scalar, arrowVector, flop, flip;

    private String gdfileOriginal;

    private NcgridResourceData gridRscData;

    private String ncgribPreferences;

    private ISpatialObject spatialObj, subgSpatialObj;

    private ArrayList<DataTime> dataForecastTimes;

    private NcgridDataCache cacheData;

    public static final int LLMXGD = 1000000; // Max # grid points

    // ENSEMBLE Calculation flag
    private static boolean isEnsCategory = false;

    private static GempakGridParmInfoLookup gempakParmInfo;

    private static GempakGridVcrdInfoLookup gempakVcordInfo;

    public Dgdriv() {
        /*
         * Initialize GEMPLT, DG and grid libraries.
         */
        // gd.initialize();
        gempakParmInfo = GempakGridParmInfoLookup.getInstance();
        gempakVcordInfo = GempakGridVcrdInfoLookup.getInstance();
        gd = GridDiag.getInstance();
        gdfile = "";
        gdfileOriginal = "";
        gdpfun = "";
        glevel = "";
        gvcord = "";
        scale = "";
        garea = "";
        ncgribPreferences = null;
        subgSpatialObj = null;
        scalar = false;
        arrowVector = false;
        flop = true;
        flip = true;
        dataForecastTimes = new ArrayList<>();
    }

    public void setResourceData(NcgridResourceData rscData) {
        this.gridRscData = rscData;
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
        if (gridRscData.isEnsemble()) {
            gdfile = ((NcEnsembleResourceData) gridRscData)
                    .convertGdfileToCycleTimeString(gdfile, gridRscData
                            .getResourceName().getCycleTime());
        }
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

    public void setPreferences(String preferences) {
        this.ncgribPreferences = preferences;
    }

    public void setCacheData(NcgridDataCache data) {
        this.cacheData = data;
    }

    final int BUFRLENGTH = 128;

    final int PRMLENGTH = 40;

    final int IMISSD = -9999;

    IntByReference iret = new IntByReference(0);

    IntByReference ier = new IntByReference(0);

    IntByReference iscale = new IntByReference(0);

    IntByReference iscalv = new IntByReference(0);

    IntByReference chngnv = new IntByReference(1);

    IntByReference coladd = new IntByReference(0);

    IntByReference gottm = new IntByReference(0);

    IntByReference drpflg = new IntByReference(0);

    IntByReference level1 = new IntByReference(0);

    IntByReference level2 = new IntByReference(0);

    IntByReference ivcord = new IntByReference(0);

    IntByReference fileflg = new IntByReference(0);

    IntByReference termflg = new IntByReference(1);

    IntByReference maxgrd = new IntByReference(LLMXGD);

    IntByReference ix1 = new IntByReference(0);

    IntByReference iy1 = new IntByReference(0);

    IntByReference ix2 = new IntByReference(0);

    IntByReference iy2 = new IntByReference(0);

    IntByReference kx = new IntByReference(IMISSD);

    IntByReference ky = new IntByReference(IMISSD);

    IntByReference igx = new IntByReference(0);

    IntByReference igy = new IntByReference(0);

    IntByReference numerr = new IntByReference(0);

    FloatByReference rmax = new FloatByReference(0.F);

    FloatByReference rmin = new FloatByReference(0.F);

    String garout = "";

    String prjout = "";

    String satfil = "";

    String outfil = "";

    String skip = "N";

    String errorURI = "";

    byte[] pfunc = new byte[BUFRLENGTH];

    byte[] parmu = new byte[PRMLENGTH];

    byte[] parmv = new byte[PRMLENGTH];

    byte[] time = new byte[PRMLENGTH];

    byte[] time1 = new byte[BUFRLENGTH];

    byte[] time2 = new byte[BUFRLENGTH];

    byte[] gareabuf = new byte[BUFRLENGTH];

    byte[] prjbuf = new byte[BUFRLENGTH];

    boolean proces = true;

    Map<Integer, String> hm = new HashMap<>();

    // private String eventName;
    DiagnosticsCallback diagCallback = null;

    ReturnFileNameCallback flnmCallback = null;

    ReturnCycleForecastHoursCallback fhrsCallback = null;

    ReturnNavigationCallback navCallback = null;

    ReturnDataCallback dataCallback = null;

    ReturnDataURICallback duriCallback = null;

    ReturnSubgridCRSCallback subgCrsCallback = null;

    private class DiagnosticsCallback implements gempak.DbCallbackWithMessage {

        @Override
        public boolean callback(String msg) {
            return true;
        }
    }

    private class ReturnDataCallback implements gempak.DbCallbackWithMessage {

        @Override
        public boolean callback(String msg) {
            if (msg.contains("/")) {
                try {
                    String[] msgArr = msg.split(";");
                    if (msgArr.length == 3) {
                        boolean addData = false;
                        String dataURI = msgArr[0];
                        int nx = Integer.parseInt(msgArr[1].trim());
                        int ny = Integer.parseInt(msgArr[2].trim());
                        long t0 = System.currentTimeMillis();
                        NcgridData gData = cacheData.getGridData(dataURI);
                        float[] rData;
                        if (gData == null) {
                            addData = true;
                            rData = retrieveDataFromRetriever(dataURI);
                            if (rData == null) {
                                errorURI = msg;
                                proces = false;
                                return true;
                            }
                        } else {
                            rData = gData.getData();
                        }
                        long t1 = System.currentTimeMillis();
                        int rDataSize = rData.length;
                        IntByReference datSize = new IntByReference(rDataSize);

                        if ((nx * ny) == rDataSize) {
                            if (addData) {
                                cacheData.addGridData(dataURI, nx, ny, rData);
                            }
                            if (flip) {
                                gd.gem.db_returndata(flipData(rData, nx, ny),
                                        datSize);
                            } else {
                                gd.gem.db_returndata(checkMissingData(rData),
                                        datSize);
                            }
                        } else {
                            errorURI = msg;
                            proces = false;
                        }
                        return true;
                    } else {
                        errorURI = msg;
                        proces = false;
                        return true;
                    }
                } catch (VizException e) {
                    errorURI = msg;
                    proces = false;
                    return true;
                }
            } else {
                return true;
            }
        }
    }

    private class ReturnNavigationCallback implements
            gempak.DbCallbackWithMessage {

        @Override
        public boolean callback(String msg) {
            String navigationString = null;
            if (gdfile.startsWith("{") && gdfile.endsWith("}")) {
                navigationString = getEnsembleNavigation(msg);
            } else {
                navigationString = getGridNavigationContent(spatialObj);
            }
            gd.gem.db_returnnav(navigationString);
            return true;
        }
    }

    private class ReturnDataURICallback implements gempak.DbCallbackWithMessage {

        @Override
        public boolean callback(String msg) {
            try {
                String dataURI = getDataURIFromAssembler(msg);
                gd.gem.db_returnduri(dataURI);
                return true;
            } catch (VizException e) {
                errorURI = msg;
                proces = false;
                return true;
            }
        }
    }

    private class ReturnSubgridCRSCallback implements
            gempak.DbCallbackWithMessage {

        @Override
        public boolean callback(String msg) {
            createNewISpatialObj(msg);
            return true;
        }
    }

    private class ReturnCycleForecastHoursCallback implements
            gempak.DbCallbackWithoutMessage {

        @Override
        public boolean callback() {
            String cycleFcstHrsString = getCycleFcstHrsString(dataForecastTimes);
            gd.gem.db_returnfhrs(cycleFcstHrsString);
            return true;
        }
    }

    private class ReturnFileNameCallback implements
            gempak.DbCallbackWithMessage {

        @Override
        public boolean callback(String msg) {
            try {
                String fileNames = executeScript(msg);
                gd.gem.db_returnflnm(fileNames);
                return true;
            } catch (VizException e) {
                errorURI = msg;
                proces = false;
                return true;
            }
        }
    }

    public FloatGridData execute() throws DgdrivException {

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
            throw new DgdrivException("From in_bdta: error initializing GEMPAK");
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

            /*
             * Get the next time to process from time server.
             */
            gd.gem.dgc_ntim_(chngnv, coladd, time1, time2, gottm, iret);

            if (iret.getValue() != 0) {
                gd.gem.erc_wmsg("DG", iret, "", ier);
                proces = false;
            } else {
                gd.gem.tgc_dual(time1, time2, time, iret);
            }
        }

        /*
         * Set the map projection and graphics area.
         */
        long t09a = System.currentTimeMillis();

        /*
         * Setup the grid subset that covers the graphics area.
         */
        if (this.ncgribPreferences != null && proces) {
            String[] prefs = this.ncgribPreferences.split(";");
            gd.gem.db_setsubgnav_(new Float(prefs[0]), new Float(prefs[1]),
                    new Float(prefs[2]), new Float(prefs[3]), iret);
            if (iret.getValue() != 0) {
                proces = false;
            }

            if (proces) {
                gd.gem.dgc_subg_(skip, maxgrd, ix1, iy1, ix2, iy2, iret);
                if (iret.getValue() != 0) {
                    proces = false;
                }
            }
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

        float[] ugrid = null;
        float[] vgrid = null;
        int grid_size = kx.getValue() * ky.getValue();
        if (kx.getValue() > 0 && ky.getValue() > 0) {
            ugrid = new float[grid_size];
            vgrid = new float[grid_size];
        } else {
            proces = false;
        }

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
            if (!proces) {
                throw new DgdrivException("Error retrieving data record "
                        + errorURI);
            }
            if (iret.getValue() != 0) {
                gd.gem.erc_wmsg("DG", iret, Native.toString(pfunc), ier);
                proces = false;
            }
        }

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
                    fds.setYdata(flopData(vgrid, igx.getValue(), igy.getValue()));
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
        String template = this.gdfileOriginal + "_db";
        if (this.gdfileOriginal.contains(":")) {
            template = this.gdfileOriginal.substring(0,
                    this.gdfileOriginal.indexOf(":"))
                    + "_db";
        }
        IntByReference iret = new IntByReference(0);

        gd.gem.db_seta2dtinfo_(alias, path, template, iret);

    }

    private void prepareEnsembleDTInfo() {
        String thePath = "A2DB_GRID";
        StringBuilder sba = new StringBuilder();
        StringBuilder sbp = new StringBuilder();
        StringBuilder sbt = new StringBuilder();
        String[] gdfileArray = this.gdfileOriginal.substring(
                this.gdfileOriginal.indexOf("{") + 1,
                this.gdfileOriginal.indexOf("}")).split(",");
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

        gd.gem.db_seta2dtinfo_(alias, path, template, iret);
    }

    private String getEnsembleTemplate(String ensName, String perturbationNum) {
        String ensTemplate = ensName + "_db_" + perturbationNum
                + "_YYYYMMDDHHfFFF";
        return ensTemplate;
    }

    private String getTaggedGdfile() {
        if (this.dataSource.equalsIgnoreCase("grid")) {
            // String tag = this.dataSource + "_";
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
                String[] gdfileArray = this.gdfileOriginal.substring(
                        this.gdfileOriginal.indexOf("{") + 1,
                        this.gdfileOriginal.indexOf("}")).split(",");
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

    /**
     * Create sub-grid coverage
     * 
     * @param subgGempakFormat
     *            -- prj;nx;ny;lllat;lllon;urlat;urlon;angle1;angle2;angle3
     */
    private void createNewISpatialObj(String subgGempakFormat) {
        String[] gareaStr = subgGempakFormat.split(";");

        if (subgSpatialObj == null && gareaStr != null && gareaStr.length >= 7) {
            if (gareaStr[0].equalsIgnoreCase("CED")) {
                createLatlonISPatialObj(Integer.parseInt(gareaStr[1]),
                        Integer.parseInt(gareaStr[2]),
                        Double.parseDouble(gareaStr[3]),
                        Double.parseDouble(gareaStr[4]),
                        Double.parseDouble(gareaStr[5]),
                        Double.parseDouble(gareaStr[6]));
            } else if (gareaStr[0].equalsIgnoreCase("LCC")) {
                createLambertConformalISPatialObj(
                        Integer.parseInt(gareaStr[1]),
                        Integer.parseInt(gareaStr[2]),
                        Double.parseDouble(gareaStr[3]),
                        Double.parseDouble(gareaStr[4]));
            } else if (gareaStr[0].equalsIgnoreCase("MER")) {
                createMercatorISPatialObj(Integer.parseInt(gareaStr[1]),
                        Integer.parseInt(gareaStr[2]),
                        Double.parseDouble(gareaStr[3]),
                        Double.parseDouble(gareaStr[4]),
                        Double.parseDouble(gareaStr[5]),
                        Double.parseDouble(gareaStr[6]));
            } else if (gareaStr[0].equalsIgnoreCase("STR")) {
                createPolarStereoISPatialObj(Integer.parseInt(gareaStr[1]),
                        Integer.parseInt(gareaStr[2]),
                        Double.parseDouble(gareaStr[3]),
                        Double.parseDouble(gareaStr[4]));
            }
        }
    }

    /**
     * Create Latitude/longitude coverage
     * 
     * @param nx
     *            - Number of points along a parallel
     * @param ny
     *            - Number of points along a meridian
     * @param la1
     *            - Latitude of first grid point
     * @param lo1
     *            - Longitude of the first grid point
     * @param la2
     *            - Latitude of the last grid point
     * @param lo2
     *            - Longitude of the last grid point
     */
    private void createLatlonISPatialObj(int nx, int ny, double la1,
            double lo1, double la2, double lo2) {

        CustomLatLonCoverage cv = new CustomLatLonCoverage();
        cv.setNx(nx);
        cv.setNy(ny);
        cv.setLa1(la1);
        cv.setLo1(lo1);
        cv.setLa2(la2);
        cv.setLo2(lo2);
        GridCoverage gc = (GridCoverage) spatialObj;

        LatLonGridCoverage llgc = (LatLonGridCoverage) gc;
        cv.setDx(llgc.getDx());
        cv.setDy(llgc.getDy());
        cv.setSpacingUnit(llgc.getSpacingUnit());
        if (cv.build()) {
            setSubgSpatialObj(cv);
        }
    }

    /**
     * Create Lambert-Conformal coverage
     * 
     * @param nx
     *            - Number of points along the x-axis
     * @param ny
     *            - Number of points along the y-axis
     * @param la1
     *            - Latitude of the first grid point
     * @param lo1
     *            - Longitude of the first grid point
     */
    private void createLambertConformalISPatialObj(int nx, int ny, double la1,
            double lo1) {

        CustomLambertConformalCoverage cv = new CustomLambertConformalCoverage();
        cv.setNx(nx);
        cv.setNy(ny);
        cv.setLa1(la1);
        cv.setLo1(lo1);

        GridCoverage gc = (GridCoverage) spatialObj;

        LambertConformalGridCoverage llgc = (LambertConformalGridCoverage) gc;
        cv.setMajorAxis(llgc.getMajorAxis());
        cv.setMinorAxis(llgc.getMinorAxis());
        cv.setLatin1(llgc.getLatin1());
        cv.setLatin2(llgc.getLatin2());
        cv.setLov(llgc.getLov());
        cv.setDx(llgc.getDx());
        cv.setDy(llgc.getDy());
        cv.setSpacingUnit(llgc.getSpacingUnit());
        if (cv.build()) {
            setSubgSpatialObj(cv);
        }
    }

    /**
     * Create Mercator coverage
     * 
     * @param nx
     *            - Number of points along a parallel
     * @param ny
     *            - Number of points along a meridian
     * @param la1
     *            - Latitude of first grid point
     * @param lo1
     *            - Longitude of the first grid point
     * @param la2
     *            - Latitude of the last grid point
     * @param lo2
     *            - Longitude of the last grid point
     */
    private void createMercatorISPatialObj(int nx, int ny, double la1,
            double lo1, double la2, double lo2) {

        CustomMercatorCoverage cv = new CustomMercatorCoverage();
        cv.setNx(nx);
        cv.setNy(ny);
        cv.setLa1(la1);
        cv.setLo1(lo1);
        cv.setLa2(la2);
        cv.setLo2(lo2);
        GridCoverage gc = (GridCoverage) spatialObj;

        MercatorGridCoverage llgc = (MercatorGridCoverage) gc;
        cv.setMajorAxis(llgc.getMajorAxis());
        cv.setMinorAxis(llgc.getMinorAxis());
        cv.setDx(llgc.getDx());
        cv.setDy(llgc.getDy());
        cv.setSpacingUnit(llgc.getSpacingUnit());
        if (cv.build()) {
            setSubgSpatialObj(cv);
        }
    }

    /**
     * Create Polar-Stereo coverage
     * 
     * @param nx
     *            - Number of points along the x-axis
     * @param ny
     *            - Number of points along the y-axis
     * @param la1
     *            - Latitude of the first grid point
     * @param lo1
     *            - Longitude of the first grid point
     */
    private void createPolarStereoISPatialObj(int nx, int ny, double la1,
            double lo1) {

        CustomPolarStereoCoverage cv = new CustomPolarStereoCoverage();
        cv.setNx(nx);
        cv.setNy(ny);
        cv.setLa1(la1);
        cv.setLo1(lo1);

        GridCoverage gc = (GridCoverage) spatialObj;

        PolarStereoGridCoverage llgc = (PolarStereoGridCoverage) gc;
        cv.setMajorAxis(llgc.getMajorAxis());
        cv.setMinorAxis(llgc.getMinorAxis());
        cv.setLov(llgc.getLov());
        cv.setLov(llgc.getLov());
        cv.setDx(llgc.getDx());
        cv.setDy(llgc.getDy());
        cv.setSpacingUnit(llgc.getSpacingUnit());
        if (cv.build()) {
            setSubgSpatialObj(cv);
        }
    }

    private String getCycleFcstHrsString(ArrayList<DataTime> dataForecastTimes) {
        // TODO Auto-generated method stub
        StringBuilder resultsBuf = new StringBuilder();
        for (DataTime dt : dataForecastTimes) {
            resultsBuf
                    .append(CommonDateFormatUtil.dbtimeToDattim(dt.toString()));
            resultsBuf.append("|");
        }
        return resultsBuf.substring(0, resultsBuf.length() - 1);
    }

    /*
     * Flips the data from CAVE order and changes the missing data value from
     * CAVE -999999.0f to GEMPAK -9999.0f
     */
    private float[] flipData(float[] inGrid, int nx, int ny) {

        float[] outGridFlipped = new float[inGrid.length];

        int kk = 0;

        for (int jj = 0; jj < ny; jj++) {
            int m1 = nx * ny - nx * (jj + 1);
            int m2 = nx * ny - nx * jj;
            for (int ii = m1; ii < m2; ii++) {
                if (inGrid[ii] < -900000.0) {
                    outGridFlipped[kk] = -9999.0f;
                    kk++;
                } else {
                    outGridFlipped[kk] = inGrid[ii];
                    kk++;
                }
            }
        }

        return outGridFlipped;
    }

    /*
     * Changes the missing data value from CAVE -999999.0f to GEMPAK -9999.0f
     */
    private float[] checkMissingData(float[] inGrid) {

        float[] outGridFlipped = new float[inGrid.length];

        for (int ii = 0; ii < inGrid.length; ii++) {
            if (inGrid[ii] < -900000.0) {
                outGridFlipped[ii] = -9999.0f;
            } else {
                outGridFlipped[ii] = inGrid[ii];
            }
        }

        return outGridFlipped;
    }

    /*
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

    /*
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

    private float[] retrieveDataFromRetriever(String dataURI)
            throws VizException {

        // create GridDataRetriever using datauri
        // setUnit for parameter
        // setWorldWrapColumns (1);
        // check its return value/exception and decide to update coverage or not
        GridDataRetriever dataRetriever = new GridDataRetriever(dataURI);
        try {
            dataRetriever.setWorldWrapColumns(1);
        } catch (GridCoverageException e) {
            // ignore setWorldWrapColumns exception.
        }
        try {
            String gempakParm = cacheData.getGempakParam(dataURI);
            if (gempakParm != null) {
                dataRetriever.setUnit(UnitConv.deserializer(gempakParmInfo
                        .getParmUnit(gempakParm)));
            }
        } catch (ConversionException | ParseException e) {
            // ignore setUnit exception. use default units
        }
        try {
            FloatDataRecord dataRecord = dataRetriever.getDataRecord();
            float[] data = dataRecord.getFloatData();

            /*
             * For ensemble calculations, we need to be sure we apply the
             * primary model's grid coverage as the coverage used for the entire
             * ensemble calculation. Fortunately, the native lib always passes
             * the primary model through this interface first when performing an
             * enemble calculation, So, the first time we pass through this code
             * will be when subgSpatialObj has its initial value: null.
             */
            if (subgSpatialObj == null) {
                setSubgSpatialObj(dataRetriever.getCoverage());
            }
            return data;
        } catch (StorageException s) {
            return null;
        }
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

        // if (DataMode.getSystemMode() == DataMode.THRIFT) {
        // file = new File(File.separator + dataURI.split("/")[1]
        // + File.separator + path + File.separator + sb.toString());
        // } else if (DataMode.getSystemMode() == DataMode.PYPIES) {
        file = new File(
        // TODO--OK?? VizApp.getServerDataDir() + File.separator +
                dataURI.split("/")[1] + File.separator + path + File.separator
                        + sb.toString());
        // } else {
        // file = new File(VizApp.getDataDir() + File.separator
        // + dataURI.split("/")[1] + File.separator + path
        // + File.separator + sb.toString());
        // }

        if (file != null) {
            filename = file.getAbsolutePath();
        }
        return filename;
    }

    private String getGridNavigationContent(ISpatialObject obj) {

        GridCoverage gc;
        try {
            gc = setCoverageForWorldWrap((GridCoverage) obj);
        } catch (GridCoverageException e) {
            gc = (GridCoverage) obj;
        }

        StringBuilder resultsBuf = new StringBuilder();

        if (gc instanceof LatLonGridCoverage) {
            /*
             * LatLonGridCoverage
             */
            LatLonGridCoverage llgc = (LatLonGridCoverage) gc;
            resultsBuf.append("CED"); // 1
            resultsBuf.append(";");
            resultsBuf.append(llgc.getNx()); // 2
            resultsBuf.append(";");
            resultsBuf.append(llgc.getNy()); // 3
            resultsBuf.append(";");
            Double dummy;
            double ddx = llgc.getDx() * (llgc.getNx() - 1);
            double ddy = llgc.getDy() * (llgc.getNy() - 1);

            if (llgc.getFirstGridPointCorner() == Corner.UpperLeft) {
                // upper left
                dummy = llgc.getLa1() * 10000;
                resultsBuf.append(dummy.intValue());
                resultsBuf.append(";");
                dummy = llgc.getLo1() * 10000;
                resultsBuf.append(dummy.intValue());
                resultsBuf.append(";");
                // lower right
                dummy = (llgc.getLa1() - ddy) * 10000;
                resultsBuf.append(dummy.intValue());
                resultsBuf.append(";");
                dummy = (llgc.getLo1() + ddx) * 10000;
                resultsBuf.append(dummy.intValue());
                resultsBuf.append(";");
            } else { // assume there are only two options: UpperLeft and
                     // LowerLeft
                dummy = (llgc.getLa1() + ddy) * 10000;
                resultsBuf.append(dummy.intValue());
                resultsBuf.append(";");
                dummy = llgc.getLo1() * 10000;
                resultsBuf.append(dummy.intValue());
                resultsBuf.append(";");

                dummy = llgc.getLa1() * 10000;
                resultsBuf.append(dummy.intValue());
                resultsBuf.append(";");
                dummy = (llgc.getLo1() + ddx) * 10000;
                resultsBuf.append(dummy.intValue());
                resultsBuf.append(";");
            }

            dummy = -9999.0;
            resultsBuf.append(dummy.intValue());
            resultsBuf.append(";");
            dummy = llgc.getDx() * 10000;
            resultsBuf.append(dummy.intValue());
            resultsBuf.append(";");
            dummy = llgc.getDy() * 10000;
            resultsBuf.append(dummy.intValue());
        } else if (gc instanceof LambertConformalGridCoverage) {
            resultsBuf.append("LCC");
            resultsBuf.append(";");
            LambertConformalGridCoverage lcgc = (LambertConformalGridCoverage) gc;
            if (lcgc.getFirstGridPointCorner() == Corner.UpperLeft) {
                this.flip = true;
            }
            resultsBuf.append(lcgc.getNx());
            resultsBuf.append(";");
            resultsBuf.append(lcgc.getNy());
            resultsBuf.append(";");
            Double dummy = lcgc.getLa1() * 10000;
            resultsBuf.append(dummy.intValue());
            resultsBuf.append(";");
            dummy = lcgc.getLo1() * 10000;
            resultsBuf.append(dummy.intValue());
            resultsBuf.append(";");
            dummy = lcgc.getLatin1() * 10000;
            resultsBuf.append(dummy.intValue());
            resultsBuf.append(";");
            dummy = lcgc.getLatin2() * 10000;
            resultsBuf.append(dummy.intValue());
            resultsBuf.append(";");
            dummy = lcgc.getLov() * 10000;
            resultsBuf.append(dummy.intValue());
            resultsBuf.append(";");
            dummy = lcgc.getDx() * 10000;
            resultsBuf.append(dummy.intValue());
            resultsBuf.append(";");
            dummy = lcgc.getDy() * 10000;
            resultsBuf.append(dummy.intValue());
        } else if (gc instanceof MercatorGridCoverage) {
            MercatorGridCoverage mgc = (MercatorGridCoverage) gc;
            if (mgc.getFirstGridPointCorner() == Corner.UpperLeft) {
                this.flip = true;
            }
            resultsBuf.append("MER");
            resultsBuf.append(";");
            resultsBuf.append(mgc.getNx());
            resultsBuf.append(";");
            resultsBuf.append(mgc.getNy());
            resultsBuf.append(";");
            Double dummy = mgc.getLa1() * 10000;
            resultsBuf.append(dummy.intValue());
            resultsBuf.append(";");
            dummy = mgc.getLo1() * 10000;
            resultsBuf.append(dummy.intValue());
            resultsBuf.append(";");
            dummy = mgc.getLatin() * 10000;
            resultsBuf.append(dummy.intValue());
            resultsBuf.append(";");
            dummy = mgc.getLa2() * 10000;
            resultsBuf.append(dummy.intValue());
            resultsBuf.append(";");
            dummy = mgc.getLo2() * 10000;
            resultsBuf.append(dummy.intValue());
            resultsBuf.append(";");
            dummy = mgc.getDx() * 10000;
            resultsBuf.append(dummy.intValue());
            resultsBuf.append(";");
            dummy = mgc.getDy() * 10000;
            resultsBuf.append(dummy.intValue());
        } else if (gc instanceof PolarStereoGridCoverage) {
            /*
             * PolarStereoGridCoverage
             */
            PolarStereoGridCoverage psgc = (PolarStereoGridCoverage) gc;
            if (psgc.getFirstGridPointCorner() == Corner.UpperLeft) {
                this.flip = true;
            }
            resultsBuf.append("STR");
            resultsBuf.append(";");
            resultsBuf.append(psgc.getNx());
            resultsBuf.append(";");
            resultsBuf.append(psgc.getNy());
            resultsBuf.append(";");
            Double dummy = psgc.getLa1() * 10000;
            resultsBuf.append(dummy.intValue());
            resultsBuf.append(";");
            dummy = psgc.getLo1() * 10000;
            resultsBuf.append(dummy.intValue());
            resultsBuf.append(";");
            dummy = -9999.0;
            resultsBuf.append(dummy.intValue());
            resultsBuf.append(";");
            dummy = -9999.0;
            resultsBuf.append(dummy.intValue());
            resultsBuf.append(";");
            dummy = psgc.getLov() * 10000;
            resultsBuf.append(dummy.intValue());
            resultsBuf.append(";");
            dummy = psgc.getDx() * 10000;
            resultsBuf.append(dummy.intValue());
            resultsBuf.append(";");
            dummy = psgc.getDy() * 10000;
            resultsBuf.append(dummy.intValue());
        }

        String content = resultsBuf.toString();
        return content;

    }

    private GridCoverage setCoverageForWorldWrap(GridCoverage orig)
            throws GridCoverageException {
        GridCoverage dataLoc = orig;
        GridGeometry2D dataGeom = dataLoc.getGridGeometry();
        int wrapX = GridGeometryWrapChecker.checkForWrapping(dataGeom);
        if (wrapX != -1) {
            // add one column
            int newX = wrapX + 1;
            if (newX == dataLoc.getNx()) {
                return orig;
            } else if (newX < dataLoc.getNx()) {
                return orig;
            } else {
                GridCoverage requestCoverage;
                if (dataLoc instanceof LatLonGridCoverage) {
                    LatLonGridCoverage newLoc = new LatLonGridCoverage(
                            (LatLonGridCoverage) dataLoc);
                    newLoc.setLa2(0);
                    newLoc.setLo2(0);
                    requestCoverage = newLoc;
                } else if (dataLoc instanceof MercatorGridCoverage) {
                    MercatorGridCoverage newLoc = new MercatorGridCoverage(
                            (MercatorGridCoverage) dataLoc);
                    newLoc.setLa2(null);
                    newLoc.setLo2(null);
                    requestCoverage = newLoc;
                } else if (dataLoc instanceof LambertConformalGridCoverage) {
                    requestCoverage = new LambertConformalGridCoverage(
                            (LambertConformalGridCoverage) dataLoc);
                } else if (dataLoc instanceof PolarStereoGridCoverage) {
                    requestCoverage = new PolarStereoGridCoverage(
                            (PolarStereoGridCoverage) dataLoc);
                } else {
                    throw new GridCoverageException(
                            "Cannot wrap data for projection of type "
                                    + dataLoc.getClass().getName());
                }
                requestCoverage.setNx(newX);
                requestCoverage.setGridGeometry(null);
                requestCoverage.initialize();
                return requestCoverage;
            }
        } else {
            return orig;
        }
    }

    private void setGridFlip(ISpatialObject obj) {

        GridCoverage gc = (GridCoverage) obj;

        if (gc.getFirstGridPointCorner() == Corner.UpperLeft) {
            this.flop = true;
        }
    }

    private String executeScript(String scriptToRun) throws VizException {

        if (scriptToRun == null) {
            return null;
        }

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
        rcMap.put(GridDBConstants.PLUGIN_NAME, new RequestConstraint(
                GridDBConstants.GRID_TBL_NAME));
        rcMap.put(GridDBConstants.MODEL_NAME_QUERY, new RequestConstraint(
                modelName, ConstraintType.EQUALS));
        if (!eventName.equalsIgnoreCase("null")) {
            rcMap.put(GridDBConstants.ENSEMBLE_ID_QUERY, new RequestConstraint(
                    eventName, ConstraintType.EQUALS));
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
        String retFileNames = "";
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
                Object fSecValue = responseList.get(i).get(
                        GridDBConstants.FORECAST_TIME_QUERY);
                Object refValue = responseList.get(i).get(
                        GridDBConstants.REF_TIME_QUERY);
                if (fSecValue != null && fSecValue instanceof Integer
                        && refValue != null && refValue instanceof Date) {

                    String refString = sdf.format((Date) refValue);
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
                        retFileNames = retFileNames + "|";
                    }

                    retFileNames = retFileNames
                            + prefix
                            + dt
                            + hh
                            + "f"
                            + CommonDateFormatUtil
                                    .getForecastTimeString(fcstTimeInSec);
                }
            }
        } catch (VizException e) {
            statusHandler.error(
                    "DBQueryRequest failed: " + e.getLocalizedMessage(), e);
        }
        return retFileNames;
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

    private String getDataURIFromAssembler(String parameters)
            throws VizException {
        long t0 = System.currentTimeMillis();

        String datauri = cacheData.getDataURI(parameters);
        if (datauri != null) {
            return datauri;
        }
        Map<String, RequestConstraint> rcMap = getRequestConstraint(parameters);
        if (rcMap == null) {
            return null;
        }

        DbQueryRequest request = new DbQueryRequest();
        // request.addRequestField(GridDBConstants.DATA_URI_QUERY);
        request.setConstraints(rcMap);

        DbQueryResponse response = (DbQueryResponse) ThriftClient
                .sendRequest(request);
        PluginDataObject[] pdos = response
                .getEntityObjects(PluginDataObject.class);
        if (pdos.length > 0) {
            datauri = pdos[0].getDataURI();
        } else {
            datauri = null;
        }

        // datauri = rec.getDataURI();
        if (datauri != null) {
            cacheData.addDataURI(parameters, datauri);
        }
        return datauri;
    }

    public void setCycleForecastTimes(ArrayList<DataTime> dataTimes) {
        this.dataForecastTimes = dataTimes;
    }

    private String getEnsembleNavigation(String msg) {
        String navStr = null;
        String[] tmpAttrs = msg.split("\\|");
        Map<String, RequestConstraint> queryList = new HashMap<>();
        queryList.put(GridDBConstants.PLUGIN_NAME, new RequestConstraint(
                GridDBConstants.GRID_TBL_NAME, ConstraintType.EQUALS));
        queryList.put(GridDBConstants.MODEL_NAME_QUERY, new RequestConstraint(
                tmpAttrs[0], ConstraintType.EQUALS));
        if (tmpAttrs[1] != null && tmpAttrs[1].length() > 0
                && (!tmpAttrs[1].equalsIgnoreCase("null"))) {
            queryList.put(GridDBConstants.ENSEMBLE_ID_QUERY,
                    new RequestConstraint(tmpAttrs[1], ConstraintType.EQUALS));
        }
        if (tmpAttrs[2] != null && tmpAttrs[2].length() > 0) {
            String navTime = buildRefTime(tmpAttrs[2]);
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
            if (responseList.size() > 0) {
                Object spatialObj = responseList.get(0).get(
                        GridDBConstants.NAVIGATION_QUERY);
                if (spatialObj != null && spatialObj instanceof ISpatialObject) {
                    cov = (ISpatialObject) spatialObj;
                }
            }

            if (cov != null) {
                navStr = getGridNavigationContent(cov);
            }
        } catch (VizException e) {
            //
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

    /*
     * Use unified grid plugin mapping
     */
    private Map<String, RequestConstraint> getRequestConstraint(
            String parameters) {
        GridQueryAssembler qAssembler = new GridQueryAssembler("GEMPAK");
        String[] parmList = parameters.split("\\|");
        qAssembler.setDatasetId(parmList[0]);

        if (!parmList[1].isEmpty()) {
            if (isEnsCategory) {
                if (!parmList[1].equalsIgnoreCase("null")) {
                    qAssembler.setEnsembleId(parmList[1]);
                }
            } else {
                if (gridRscData.getEnsembelMember() != null) {
                    qAssembler.setEnsembleId(parmList[1]);
                } else {
                    qAssembler.setSecondaryId(parmList[1]);
                }
            }
        }
        qAssembler.setParameterAbbreviation(parmList[2]);
        qAssembler.setMasterLevelName(parmList[3]);

        int scale_vcord = 1;
        String unitParm = gempakVcordInfo.getVcrdUnit(parmList[3]);
        if ((unitParm == null) || unitParm.isEmpty() || unitParm.equals("-")) {
            try {
                scale_vcord = (int) Math.pow(10, Integer
                        .valueOf(gempakVcordInfo.getParmScale((parmList[3]))));
            } catch (NumberFormatException e) {
                //
            }
        }

        String ll1 = null, ll2 = null;
        if (parmList[4].contains(":")) {
            ll1 = parmList[4].split(":")[0];
            ll2 = parmList[4].split(":")[1];
        } else {
            ll1 = parmList[4];
        }
        if (ll1 == null && ll2 == null) {
            return null;
        }
        if (ll1 != null) {
            Double level1;
            try {
                level1 = Double.valueOf(ll1);
            } catch (NumberFormatException e) {
                return null;
            }
            qAssembler.setLevelOneValue(level1 / scale_vcord);
        }
        if (ll2 != null) {
            Double level2;
            try {
                level2 = Double.valueOf(ll2);
            } catch (NumberFormatException e) {
                return null;
            }
            qAssembler.setLevelTwoValue(level2 / scale_vcord);
        } else {
            qAssembler.setLevelTwoValue(-999999.0);
        }

        qAssembler.setLevelUnits(gempakVcordInfo.getVcrdUnit(parmList[3]));
        Map<String, RequestConstraint> rcMap;
        try {
            rcMap = qAssembler.getConstraintMap();
        } catch (CommunicationException e) {
            return null;
        }
        String refTimeg = parmList[5].toUpperCase().split("F")[0];
        String refTime = CommonDateFormatUtil.dattimToDbtime(refTimeg);
        String fcstTimeInSec = Integer.toString(CommonDateFormatUtil
                .getForecastTimeInSec(parmList[5]));

        rcMap.put(GridDBConstants.REF_TIME_QUERY,
                new RequestConstraint(refTime));
        rcMap.put(GridDBConstants.FORECAST_TIME_QUERY, new RequestConstraint(
                fcstTimeInSec));
        return rcMap;
    }
}
