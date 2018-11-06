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
package com.raytheon.uf.viz.gempak.cave.data.retriever;

import java.text.ParseException;

import javax.measure.converter.ConversionException;

import com.raytheon.uf.common.dataplugin.grid.datastorage.GridDataRetriever;
import com.raytheon.uf.common.datastorage.StorageException;
import com.raytheon.uf.common.datastorage.records.FloatDataRecord;
import com.raytheon.uf.common.geospatial.ISpatialObject;
import com.raytheon.uf.common.gridcoverage.exception.GridCoverageException;
import com.raytheon.uf.common.status.IPerformanceStatusHandler;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.PerformanceStatus;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.units.UnitConv;
import com.raytheon.uf.viz.gempak.common.data.GempakDbDataResponse;
import com.raytheon.uf.viz.gempak.common.data.retriever.IGempakDbDataRetriever;
import com.raytheon.uf.viz.gempak.common.request.GempakDbDataRequest;
import com.raytheon.uf.viz.ncep.grid.NcgribLogger;
import com.raytheon.uf.viz.ncep.grid.NcgridDataCache;
import com.raytheon.uf.viz.ncep.grid.NcgridDataCache.NcgridData;

import gov.noaa.nws.ncep.viz.gempak.grid.units.GempakGridParmInfoLookup;

/**
 * Implementation of {@link IGempakDbDataRetriever} for retrieving GEMPAK DB
 * data from within CAVE.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 10, 2018 54483      mapeters    Initial creation
 * Oct 23, 2018 54476      tjensen     Change cache to singleton
 * Oct 23, 2018 54483      mapeters    Use {@link IPerformanceStatusHandler}
 * Oct 25, 2018 54483      mapeters    Handle {@link NcgribLogger} refactor
 *
 * </pre>
 *
 * @author mapeters
 */
public class GempakCaveDbDataRetriever implements IGempakDbDataRetriever {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(GempakCaveDbDataRetriever.class);

    private static final IPerformanceStatusHandler perfLog = PerformanceStatus
            .getHandler(GempakCaveDbDataRetriever.class.getSimpleName() + ":");

    private final NcgridDataCache dataCache;

    /**
     * Constructor.
     */
    public GempakCaveDbDataRetriever() {
        this.dataCache = NcgridDataCache.getInstance();
    }

    @Override
    public GempakDbDataResponse getDbData(GempakDbDataRequest request) {
        boolean addData = false;
        String dataURI = request.getDataURI();
        int nx = request.getNx();
        int ny = request.getNy();
        long t0 = System.currentTimeMillis();
        NcgridData gData = dataCache.getGridData(dataURI);
        float[] rData;
        ISpatialObject subgSpatialObj = null;
        if (gData == null) {
            addData = true;
            GempakDbDataResponse unprocessedResponse = retrieveDataFromRetriever(
                    dataURI);
            rData = unprocessedResponse.getData();
            subgSpatialObj = unprocessedResponse.getSubgSpatialObj();
            if (rData == null) {
                if (NcgribLogger.getInstance().isEnableDiagnosticLogs()) {
                    statusHandler
                            .debug("??? retrieveDataFromRetriever return NULL for dataURI("
                                    + dataURI + ")");
                }
                return null;
            }
        } else {
            if (NcgribLogger.getInstance().isEnableDiagnosticLogs()) {
                long t00 = System.currentTimeMillis();
                perfLog.logDuration("++++ retrieve data (nx:" + gData.getNx()
                        + "-ny:" + gData.getNy() + ") from cache", t00 - t0);
            }
            rData = gData.getData();
        }
        long t1 = System.currentTimeMillis();
        int rDataSize = rData.length;

        GempakDbDataResponse rval = null;
        if ((nx * ny) == rDataSize) {
            if (addData) {
                dataCache.addGridData(dataURI, nx, ny, rData);
            }
            if (request.isFlip()) {
                rData = flipData(rData, nx, ny);
            } else {
                rData = checkMissingData(rData);
            }
            rval = new GempakDbDataResponse(rData, subgSpatialObj);
        } else {
            statusHandler.debug("retrieve data size(" + rDataSize
                    + ") mismatch with navigation nx=" + nx + " ny=" + ny);
        }
        long t2 = System.currentTimeMillis();
        perfLog.log("Retrieving GEMPAK DB data took " + (t2 - t0)
                + " ms (retrieval=" + (t1 - t0) + ", processing=" + (t2 - t1)
                + ")");
        return rval;
    }

    private GempakDbDataResponse retrieveDataFromRetriever(String dataURI) {
        long t001 = System.currentTimeMillis();
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
        String gempakParm = null;
        try {
            gempakParm = dataCache.getGempakParam(dataURI);
            if (gempakParm != null) {
                dataRetriever
                        .setUnit(UnitConv.deserializer(GempakGridParmInfoLookup
                                .getInstance().getParmUnit(gempakParm)));
            }
        } catch (ConversionException | ParseException e) {
            statusHandler
                    .info("Problem with dataRetriever setUnit for gempakParm: "
                            + gempakParm, e);
            // ignore setUnit exception. use default units
        }
        long t002 = System.currentTimeMillis();
        if (NcgribLogger.getInstance().isEnableDiagnosticLogs()) {
            perfLog.logDuration(
                    "***Initialize GridDataRetriever for " + dataURI,
                    t002 - t001);
        }
        try {
            t001 = System.currentTimeMillis();
            FloatDataRecord dataRecord = dataRetriever.getDataRecord();
            float[] data = dataRecord.getFloatData();

            GempakDbDataResponse rval = new GempakDbDataResponse(data,
                    dataRetriever.getCoverage());
            t002 = System.currentTimeMillis();
            if (NcgribLogger.getInstance().isEnableDiagnosticLogs()) {
                perfLog.logDuration("***Reading " + dataURI
                        + " from hdf5 (return size: " + data.length + ")",
                        t002 - t001);
            }
            return rval;
        } catch (StorageException s) {
            if (NcgribLogger.getInstance().isEnableDiagnosticLogs()) {
                statusHandler.debug(
                        "???? getDataRecord --- throw StorageException", s);
            }
            return null;
        }
    }

    /**
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

    /**
     * Changes the missing data value from CAVE -999999.0f to GEMPAK -9999.0f
     */
    private float[] checkMissingData(float[] inGrid) {
        float[] outGridChecked = new float[inGrid.length];

        for (int ii = 0; ii < inGrid.length; ii++) {
            if (inGrid[ii] < -900000.0) {
                outGridChecked[ii] = -9999.0f;
            } else {
                outGridChecked[ii] = inGrid[ii];
            }
        }

        return outGridChecked;
    }
}
