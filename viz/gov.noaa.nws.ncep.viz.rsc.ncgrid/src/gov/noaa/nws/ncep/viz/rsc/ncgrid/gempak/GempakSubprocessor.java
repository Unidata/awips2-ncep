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
import java.net.Socket;

import com.raytheon.uf.common.status.IPerformanceStatusHandler;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.PerformanceStatus;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.viz.ui.personalities.awips.AbstractAWIPSComponent;

import gov.noaa.nws.ncep.viz.rsc.ncgrid.FloatGridData;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.dgdriv.Dgdriv;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.dgdriv.DgdrivException;

/**
 * This class is started as a subprocess from CAVE, and handles GEMPAK data
 * processing, communicating with a {@link GempakSubprocessSpawner} on the CAVE side
 * to take the data to process and return the processed data record.
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
public class GempakSubprocessor extends AbstractAWIPSComponent {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(GempakSubprocessor.class);

    private static final IPerformanceStatusHandler perfLog = PerformanceStatus
            .getHandler(GempakSubprocessor.class.getSimpleName() + ":");

    /**
     * Handler for taking a {@link GempakDataRecordRequest}, processing it, and
     * returning a {@link GempakDataRecord}.
     */
    private static final IGempakRequestHandler<GempakDataRecordRequest> dataRecordRequestHandler = new IGempakRequestHandler<GempakDataRecordRequest>() {

        @Override
        public GempakDataRecord handleRequest(GempakDataRecordRequest request) {
            Dgdriv dgdriv = new Dgdriv(request.getDataInput());
            try {
                FloatGridData floatData = dgdriv.execute();
                if (floatData != null) {
                    return new GempakDataRecord(floatData,
                            dgdriv.getSubgSpatialObj());
                }
            } catch (DgdrivException e) {
                statusHandler.error("Error performing GEMPAK data processing",
                        e);
            }
            return null;
        }
    };

    @Override
    protected void startInternal(String componentName) throws Exception {
        String portStr = System.getenv(GempakProcessingConstants.PORT_KEY);
        Thread.currentThread()
                .setName(getClass().getSimpleName() + "-" + portStr);
        long t0 = System.currentTimeMillis();

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            statusHandler.error("Invalid port specified: " + portStr, e);
            return;
        }

        /*
         * Connect to server socket that was opened on the CAVE side for
         * communicating with us
         */
        try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), port);
                InputStream is = socket.getInputStream();
                OutputStream os = socket.getOutputStream()) {
            StreamCommunicator communicator = new StreamCommunicator(is, os);
            /*
             * We are expecting CAVE to send us the data processing request, so
             * register the data request handler with the communicator and then
             * tell the communicator to read the request and respond to it
             */
            communicator.registerHandler(GempakDataRecordRequest.class,
                    dataRecordRequestHandler);
            communicator.respond();
        } catch (IOException e) {
            statusHandler.error("Error creating socket on port: " + port, e);
        }

        long t1 = System.currentTimeMillis();
        perfLog.logDuration("Performing GEMPAK data processing on port " + port
                + " (within subprocess)", t1 - t0);
    }

    @Override
    protected int getRuntimeModes() {
        return NON_UI | ALERT_VIZ;
    }
}
