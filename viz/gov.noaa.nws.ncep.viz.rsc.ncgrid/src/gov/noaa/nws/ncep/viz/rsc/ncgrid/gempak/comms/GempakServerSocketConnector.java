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
package gov.noaa.nws.ncep.viz.rsc.ncgrid.gempak.comms;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.Map;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

import gov.noaa.nws.ncep.viz.rsc.ncgrid.gempak.GempakProcessingUtil;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.gempak.exception.GempakConnectionException;

/**
 * Class for connecting to a GEMPAK subprocess from CAVE. It sets up a server
 * socket for the subprocess to connect to. Note that this class is not
 * thread-safe as is.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 07, 2018 54480      mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public class GempakServerSocketConnector extends GempakSocketConnector
        implements IGempakServer {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(GempakServerSocketConnector.class);

    /**
     * Time (in ms) to wait for a connection to be made to the server socket
     * before throwing an exception.
     *
     * TODO set better value
     */
    private static final int TIMEOUT = 30_000;

    private final ServerSocket serverSocket;

    /**
     * Create a {@link GempakServerSocketConnector}, setting up a
     * {@link ServerSocket} for a GEMPAK subprocess to connect to.
     *
     * @throws GempakConnectionException
     *             if an error occurs when setting up the server socket
     */
    public GempakServerSocketConnector() throws GempakConnectionException {
        try {
            /*
             * Automatically allocate an available port on localhost, with a
             * maximum of 1 waiting client connection.
             */
            serverSocket = new ServerSocket(0, 1,
                    InetAddress.getLoopbackAddress());
            serverSocket.setSoTimeout(TIMEOUT);
        } catch (IOException e) {
            throw new GempakConnectionException(
                    "Error setting up server socket", e);
        }
    }

    @Override
    public Map<String, String> getConnectionData() {
        return Collections.singletonMap(GempakProcessingUtil.PORT_KEY,
                Integer.toString(serverSocket.getLocalPort()));
    }

    @Override
    public IGempakCommunicator connect() throws GempakConnectionException {
        try {
            socket = serverSocket.accept();
            return createCommunicator();
        } catch (IOException e) {
            throw new GempakConnectionException(
                    "Error setting up socket connection on port "
                            + serverSocket.getLocalPort(),
                    e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } catch (IOException e) {
            statusHandler.warn("Error closing socket connector on port "
                    + socket.getLocalPort(), e);
        }
        try {
            serverSocket.close();
        } catch (IOException e) {
            statusHandler.warn("Error closing server socket on port "
                    + serverSocket.getLocalPort());
        }
    }
}
