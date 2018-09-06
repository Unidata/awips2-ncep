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
package gov.noaa.nws.ncep.viz.rsc.ncgrid.gempak;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import com.raytheon.uf.common.status.IPerformanceStatusHandler;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.PerformanceStatus;
import com.raytheon.uf.common.status.UFStatus;

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
 * Sep 05, 2018 7417       mapeters    Initial creation
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

    private final GempakDataRecordRequest dataRequest;

    /**
     * Create a {@link GempakSubprocessSpawner} for spawning a GEMPAK subprocess
     * from CAVE to process the given dataInput.
     *
     * @param dataInput
     *            the data to process
     */
    public GempakSubprocessSpawner(GempakDataInput dataInput) {
        this.dataRequest = new GempakDataRecordRequest(dataInput);
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
        int port = -1;
        try (ServerSocket serverSocket = new ServerSocket(0, 0,
                InetAddress.getLoopbackAddress())) {
            String script;
            try {
                script = getSubprocessScriptPath();
            } catch (IOException e) {
                statusHandler.error("Error determining location of GEMPAK "
                        + "subprocess start-up script", e);
                return null;
            }
            statusHandler.debug("Path to GEMPAK subprocess script: " + script);

            // Start the subprocess, telling it the port to connect to
            port = serverSocket.getLocalPort();
            ProcessBuilder builder = new ProcessBuilder(script);
            builder.inheritIO();
            builder.environment().put(GempakProcessingConstants.PORT_KEY,
                    Integer.toString(port));
            try {
                subprocess = builder.start();
            } catch (IOException e) {
                statusHandler.error("Error spawning GEMPAK subprocess", e);
                return null;
            }

            // TODO set better timeout?
            serverSocket.setSoTimeout(10_000);
            /*
             * Once the subprocess connects to our server socket, request the
             * processed data from it
             */
            try (Socket socket = serverSocket.accept();
                    InputStream is = socket.getInputStream();
                    OutputStream os = socket.getOutputStream()) {
                StreamCommunicator communicator = new StreamCommunicator(is,
                        os);
                data = communicator.request(dataRequest,
                        GempakDataRecord.class);
            }
        } catch (IOException e) {
            statusHandler.error(
                    "Error setting up socket for communicating with GEMPAK subprocess",
                    e);
            if (subprocess != null) {
                subprocess.destroy();
            }
        }

        long t1 = System.currentTimeMillis();
        perfLog.logDuration("Performing GEMPAK data processing on port " + port
                + " (starting from CAVE)", t1 - t0);

        return data;
    }

    private String getSubprocessScriptPath() throws IOException {
        Bundle bundle = FrameworkUtil.getBundle(getClass());
        Path scriptPathInBundle = new Path(
                GempakProcessingConstants.SUBPROCESS_SCRIPT_BUNDLE_PATH);
        URL scriptURL = FileLocator.find(bundle, scriptPathInBundle, null);
        scriptURL = FileLocator.toFileURL(scriptURL);
        return scriptURL.getPath();
    }
}
