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

import com.raytheon.uf.common.status.IPerformanceStatusHandler;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.PerformanceStatus;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.gempak.cave.conn.GempakServerSocketConnector;
import com.raytheon.uf.viz.gempak.cave.conn.IGempakServer;
import com.raytheon.uf.viz.gempak.cave.request.handler.GempakDataURIRequestHandler;
import com.raytheon.uf.viz.gempak.cave.request.handler.GempakDbDataRequestHandler;
import com.raytheon.uf.viz.gempak.common.comm.IGempakCommunicator;
import com.raytheon.uf.viz.gempak.common.data.GempakDataInput;
import com.raytheon.uf.viz.gempak.common.data.GempakDataRecord;
import com.raytheon.uf.viz.gempak.common.exception.GempakCommunicationException;
import com.raytheon.uf.viz.gempak.common.exception.GempakConnectionException;
import com.raytheon.uf.viz.gempak.common.request.GempakDataRecordRequest;
import com.raytheon.uf.viz.gempak.common.request.GempakDataURIRequest;
import com.raytheon.uf.viz.gempak.common.request.GempakDbDataRequest;
import com.raytheon.uf.viz.gempak.common.util.GempakProcessingUtil;

/**
 * This class is used when GEMPAK processing is being done in subprocesses. It
 * handles the CAVE side of the processing, which includes spawning a
 * subprocess, sending it the data to process, and getting the processed data
 * back from it (after which the subprocess will terminate).
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

    private final GempakDataInput dataInput;

    /**
     * Create a {@link GempakSubprocessSpawner} for spawning a GEMPAK subprocess
     * from CAVE to process the given dataInput.
     *
     * @param dataInput
     *            the data to process
     */
    public GempakSubprocessSpawner(GempakDataInput dataInput) {
        this.dataInput = dataInput;
    }

    /**
     * Get the processed data record by spawning a GEMPAK subprocess, sending
     * the data request to it, and listening for the result to be sent back from
     * the subprocess.
     *
     * @return the processed data
     */
    public GempakDataRecord getDataRecord() {
        long t0 = System.currentTimeMillis();
        Process subprocess = null;
        GempakDataRecord data = null;
        try (IGempakServer server = new GempakServerSocketConnector()) {
            // Start the subprocess, telling it the port to connect to
            ProcessBuilder builder = new ProcessBuilder(
                    GempakProcessingUtil.SUBPROCESS_SCRIPT_PATH);
            builder.environment().putAll(server.getConnectionData());
            try {
                subprocess = builder.start();
            } catch (IOException e) {
                statusHandler.error("Error spawning GEMPAK subprocess", e);
                return null;
            }

            /*
             * Once the subprocess connects to our server socket, request the
             * processed data from it
             */
            IGempakCommunicator communicator = server.connect();
            communicator.registerHandler(GempakDataURIRequest.class,
                    new GempakDataURIRequestHandler(dataInput.getCacheData()));
            communicator.registerHandler(GempakDbDataRequest.class,
                    new GempakDbDataRequestHandler(dataInput.getCacheData()));
            data = communicator.request(new GempakDataRecordRequest(dataInput),
                    GempakDataRecord.class);
        } catch (GempakConnectionException | GempakCommunicationException e) {
            statusHandler.error(
                    "Error connecting/communicating with GEMPAK subprocess", e);
            if (subprocess != null) {
                subprocess.destroy();
            }
        } catch (IOException e) {
            statusHandler.warn("Error closing GEMPAK connection", e);
        }

        long t1 = System.currentTimeMillis();
        perfLog.logDuration(
                "Performing GEMPAK data processing (starting from CAVE)",
                t1 - t0);

        return data;
    }
}
