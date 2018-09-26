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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

import gov.noaa.nws.ncep.viz.rsc.ncgrid.gempak.exception.GempakConnectionException;

/**
 * Abstract class for creating a connection between CAVE and a GEMPAK subprocess
 * to allow them to communicate.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 26, 2018 54483      mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public abstract class GempakSocketConnector implements IGempakConnector {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(GempakSocketConnector.class);

    protected Socket socket;

    private InputStream is;

    private OutputStream os;

    protected IGempakCommunicator createCommunicator()
            throws GempakConnectionException {
        if (socket == null) {
            throw new GempakConnectionException(
                    "Attempted to communicate over a null socket");
        }
        try {
            is = new BufferedInputStream(socket.getInputStream());
            os = new BufferedOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            throw new GempakConnectionException(
                    "Error getting I/O streams for socket on port "
                            + socket.getLocalPort(),
                    e);
        }
        return new GempakStreamCommunicator(is, os);
    }

    @Override
    public void close() throws IOException {
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
                statusHandler
                        .warn("Error closing input stream for socket on port "
                                + socket.getLocalPort(), e);
            }
        }
        if (os != null) {
            try {
                os.close();
            } catch (IOException e) {
                statusHandler
                        .warn("Error closing output stream for socket on port "
                                + socket.getLocalPort(), e);
            }
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                statusHandler.warn("Error closing socket on port "
                        + socket.getLocalPort());
            }
        }
    }
}
