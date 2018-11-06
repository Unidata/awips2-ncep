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
package com.raytheon.uf.viz.gempak.subprocess.conn;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;

import com.raytheon.uf.viz.gempak.common.comm.IGempakCommunicator;
import com.raytheon.uf.viz.gempak.common.conn.GempakSocketConnector;
import com.raytheon.uf.viz.gempak.common.exception.GempakConnectionException;
import com.raytheon.uf.viz.gempak.common.util.GempakProcessingUtil;

/**
 * Class for connecting to CAVE from a GEMPAK subprocess. It connects to a
 * server socket set up by CAVE. Note that this class is not thread-safe as is.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 07, 2018 54480      mapeters    Initial creation
 * Sep 26, 2018 54483      mapeters    Extend {@link GempakSocketConnector}
 *
 * </pre>
 *
 * @author mapeters
 */
public class GempakClientSocketConnector extends GempakSocketConnector {

    private final int port;

    /**
     * Create a {@link GempakClientSocketConnector}, extracting the server port
     * from the given connectionData to prepare for connecting to that port on
     * localhost.
     *
     * @param connectionData
     *            map of key-value pairs specifying data needed to connect (must
     *            contain {@link GempakProcessingUtil#PORT_KEY} mapping)
     * @throws GempakConnectionException
     *             if the connectionData is invalid
     */
    public GempakClientSocketConnector(Map<String, String> connectionData)
            throws GempakConnectionException {
        String portStr = connectionData.get(GempakProcessingUtil.PORT_KEY);
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            throw new GempakConnectionException(
                    "Invalid port specified: " + portStr, e);
        }
    }

    @Override
    public IGempakCommunicator connect() throws GempakConnectionException {
        try {
            socket = new Socket(InetAddress.getLoopbackAddress(), port);
            return createCommunicator();
        } catch (IOException e) {
            throw new GempakConnectionException(
                    "Error setting up socket on port " + port, e);
        }
    }
}
