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
package com.raytheon.uf.viz.gempak.subprocess.request.handler;

import com.raytheon.uf.common.status.IPerformanceStatusHandler;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.PerformanceStatus;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.gempak.common.Dgdriv;
import com.raytheon.uf.viz.gempak.common.comm.IGempakCommunicator;
import com.raytheon.uf.viz.gempak.common.data.GempakDataRecord;
import com.raytheon.uf.viz.gempak.common.exception.DgdrivException;
import com.raytheon.uf.viz.gempak.common.request.GempakDataRecordRequest;
import com.raytheon.uf.viz.gempak.common.request.handler.IGempakRequestHandler;
import com.raytheon.uf.viz.gempak.subprocess.data.retriever.GempakSubprocessDataURIRetriever;
import com.raytheon.uf.viz.gempak.subprocess.data.retriever.GempakSubprocessDbDataRetriever;
import com.raytheon.uf.viz.gempak.subprocess.data.retriever.GempakSubprocessNavigationRetriever;
import com.raytheon.uf.viz.gempak.subprocess.data.retriever.GempakSubprocessSubgridCoverageRetriever;
import com.raytheon.uf.viz.ncep.grid.FloatGridData;

/**
 * Handler for taking a {@link GempakDataRecordRequest}, processing it, and
 * returning a {@link GempakDataRecord}. This is done in a GEMPAK subprocess.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 10, 2018 54483      mapeters    Initial creation
 * Oct 23, 2018 54483      mapeters    Don't throw data exception, add
 *                                     performance logging
 * Oct 25, 2018 54483      mapeters    Pass navigation and subgrid retrievers
 *                                     to {@link Dgdriv}
 *
 * </pre>
 *
 * @author mapeters
 */
public class GempakDataRecordRequestHandler
        implements IGempakRequestHandler<GempakDataRecordRequest> {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(GempakDataRecordRequestHandler.class);

    private static final IPerformanceStatusHandler perfLog = PerformanceStatus
            .getHandler(
                    GempakDataRecordRequestHandler.class.getSimpleName() + ":");

    private final IGempakCommunicator communicator;

    /**
     * Constructor
     *
     * @param communicator
     *            used for communicating with CAVE (to request intermediate data
     *            and to send back processed data record)
     */
    public GempakDataRecordRequestHandler(IGempakCommunicator communicator) {
        this.communicator = communicator;
    }

    @Override
    public GempakDataRecord handleRequest(GempakDataRecordRequest request) {
        long t0 = System.currentTimeMillis();

        Dgdriv dgdriv = new Dgdriv(request.getDataInput(),
                new GempakSubprocessDataURIRetriever(communicator),
                new GempakSubprocessDbDataRetriever(communicator),
                new GempakSubprocessNavigationRetriever(communicator),
                new GempakSubprocessSubgridCoverageRetriever(communicator));

        GempakDataRecord rval = null;
        try {
            FloatGridData floatData = dgdriv.execute();
            if (floatData != null) {
                rval = new GempakDataRecord(floatData,
                        dgdriv.getSubgSpatialObj());
            }
        } catch (DgdrivException e) {
            statusHandler.error(
                    "Error performing GEMPAK data processing for " + request,
                    e);
        }

        long t1 = System.currentTimeMillis();
        perfLog.logDuration("Performing GEMPAK data processing for " + request,
                t1 - t0);

        return rval;
    }
}
