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
package com.raytheon.uf.viz.gempak.cave;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.raytheon.uf.common.status.IPerformanceStatusHandler;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.PerformanceStatus;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.gempak.cave.conn.GempakServerSocketConnector;
import com.raytheon.uf.viz.gempak.cave.conn.IGempakServer;
import com.raytheon.uf.viz.gempak.cave.request.handler.GempakDataURIRequestHandler;
import com.raytheon.uf.viz.gempak.cave.request.handler.GempakDbDataRequestHandler;
import com.raytheon.uf.viz.gempak.cave.request.handler.GempakNavigationRequestHandler;
import com.raytheon.uf.viz.gempak.cave.request.handler.GempakSubgridCoverageRequestHandler;
import com.raytheon.uf.viz.gempak.common.comm.IGempakCommunicator;
import com.raytheon.uf.viz.gempak.common.data.GempakDataInput;
import com.raytheon.uf.viz.gempak.common.data.GempakDataRecord;
import com.raytheon.uf.viz.gempak.common.exception.GempakCommunicationException;
import com.raytheon.uf.viz.gempak.common.exception.GempakConnectionException;
import com.raytheon.uf.viz.gempak.common.exception.GempakException;
import com.raytheon.uf.viz.gempak.common.exception.GempakShutdownException;
import com.raytheon.uf.viz.gempak.common.message.GempakLoggingConfigMessage;
import com.raytheon.uf.viz.gempak.common.message.GempakShutdownMessage;
import com.raytheon.uf.viz.gempak.common.request.GempakDataRecordRequest;
import com.raytheon.uf.viz.gempak.common.request.GempakDataURIRequest;
import com.raytheon.uf.viz.gempak.common.request.GempakDbDataRequest;
import com.raytheon.uf.viz.gempak.common.request.GempakNavigationRequest;
import com.raytheon.uf.viz.gempak.common.request.GempakSubgridCoverageRequest;
import com.raytheon.uf.viz.gempak.common.util.GempakProcessingUtil;
import com.raytheon.uf.viz.ncep.grid.NcgribLogger;

/**
 * This class is used when GEMPAK processing is being done in subprocesses. It
 * handles the CAVE side of the processing, which includes spawning a
 * subprocess, sending it the data requests to process, and getting the
 * processed data records back from it.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 05, 2018 54480      mapeters    Initial creation
 * Sep 26, 2018 54483      mapeters    Register handlers for callback requests
 *                                     from subprocess
 * Oct 08, 2018 54483      mapeters    Moved from gov.noaa.nws.ncep.viz.rsc.ncgrid.gempak,
 *                                     subprocess script is now installed to /awips2/gempak/
 * Oct 23, 2018 54476      tjensen     Change cache to singleton
 * Oct 23, 2018 54483      mapeters    Single subprocess can handle multiple requests, add
 *                                     shutdown support
 * Oct 25, 2018 54483      mapeters    Handle navigation and subgrid requests,
 *                                     send logging config to subprocess
 *
 * </pre>
 *
 * @author mapeters
 */
public class GempakSubprocessSpawner {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(GempakSubprocessSpawner.class);

    private static final IPerformanceStatusHandler perfLog = PerformanceStatus
            .getHandler(GempakSubprocessSpawner.class.getSimpleName() + ":");

    private static final int SHUTDOWN_TIMEOUT = 10_000;

    private final IGempakServer server;

    private final IGempakCommunicator communicator;

    private final Process subprocess;

    private final Object lock = new Object();

    private boolean shutdown = false;

    /**
     * Create a {@link GempakSubprocessSpawner}, spawning a GEMPAK subprocess
     * and establishing a connection with it.
     *
     * @throws GempakConnectionException
     */
    public GempakSubprocessSpawner() throws GempakConnectionException {
        try {
            server = new GempakServerSocketConnector();
            // Start the subprocess, telling it the port to connect to
            ProcessBuilder builder = new ProcessBuilder(
                    GempakProcessingUtil.SUBPROCESS_SCRIPT_PATH);
            builder.environment().putAll(server.getConnectionData());
            subprocess = builder.start();

            /*
             * Once the subprocess connects to our server socket, set up the
             * necessary request handlers
             */
            communicator = server.connect();
            communicator.registerRequestHandler(GempakDataURIRequest.class,
                    new GempakDataURIRequestHandler());
            communicator.registerRequestHandler(GempakDbDataRequest.class,
                    new GempakDbDataRequestHandler());
            communicator.registerRequestHandler(GempakNavigationRequest.class,
                    new GempakNavigationRequestHandler());
            communicator.registerRequestHandler(
                    GempakSubgridCoverageRequest.class,
                    new GempakSubgridCoverageRequestHandler());
        } catch (GempakConnectionException | IOException e) {
            throw new GempakConnectionException(
                    "Error initializing GEMPAK subprocess", e);
        }
    }

    /**
     * Get the processed data record by spawning a GEMPAK subprocess, sending
     * the data request to it, and listening for the result to be sent back from
     * the subprocess.
     *
     * @return the processed data
     * @throws GempakException
     */
    public GempakDataRecord getDataRecord(GempakDataInput dataInput)
            throws GempakException {
        GempakDataRecord data = null;
        synchronized (lock) {
            if (shutdown) {
                // This is intended to never happen
                throw new GempakShutdownException(
                        "Attempted to perform GEMPAK data processing using shutdown subprocess spawner");
            }
            long t0 = System.currentTimeMillis();
            // Send logging config here (instead of on init) in case it changes
            communicator.send(
                    new GempakLoggingConfigMessage(NcgribLogger.getInstance()));
            data = communicator.request(new GempakDataRecordRequest(dataInput),
                    GempakDataRecord.class);
            long t1 = System.currentTimeMillis();
            perfLog.logDuration(
                    "Performing GEMPAK data processing (starting from CAVE) for "
                            + dataInput,
                    t1 - t0);
        }

        return data;
    }

    /**
     * Shutdown this subprocess spawner, terminating the subprocess and freeing
     * up any other resources. Any subsequent calls to
     * {@link #getDataRecord(GempakDataInput)} will throw a
     * {@link GempakShutdownException}.
     */
    public void shutdown() {
        synchronized (lock) {
            if (shutdown) {
                return;
            }
            shutdown = true;
            try {
                communicator.send(new GempakShutdownMessage());
            } catch (GempakCommunicationException e) {
                statusHandler.warn(
                        "Error sending shutdown request to GEMPAK subprocess",
                        e);
            }

            try {
                boolean terminated = subprocess.waitFor(SHUTDOWN_TIMEOUT,
                        TimeUnit.MILLISECONDS);
                if (!terminated) {
                    statusHandler.warn(
                            "GEMPAK subprocess spawner timed out waiting for subprocess to terminate, it may not cleanly shutdown");
                }
            } catch (InterruptedException e) {
                statusHandler.warn(
                        "Thread interrupted while waiting for GEMPAK subprocess to cleanly shutdown",
                        e);
            }

            try {
                server.close();
            } catch (IOException e) {
                statusHandler.warn("Error closing GEMPAK connector", e);
            }
        }
    }
}
