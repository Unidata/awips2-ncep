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
package com.raytheon.uf.viz.gempak.common.comm;

import com.raytheon.uf.viz.gempak.common.exception.GempakCommunicationException;
import com.raytheon.uf.viz.gempak.common.exception.GempakException;
import com.raytheon.uf.viz.gempak.common.message.IGempakMessage;
import com.raytheon.uf.viz.gempak.common.message.handler.IGempakMessageHandler;
import com.raytheon.uf.viz.gempak.common.request.IGempakRequest;
import com.raytheon.uf.viz.gempak.common.request.handler.IGempakRequestHandler;

/**
 * Interface for communicating between CAVE and a GEMPAK subprocess.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 07, 2018 54480      mapeters    Initial creation
 * Oct 16, 2018 54483      mapeters    Add support for {@link IGempakMessage}, processing
 *                                     multiple requests until shutdown
 *
 * </pre>
 *
 * @author mapeters
 */
public interface IGempakCommunicator {

    /**
     * Send the given message without waiting for any response.
     *
     * @param message
     * @throws GempakCommunicationException
     *             if an error occurs sending the message
     */
    public void send(IGempakMessage message)
            throws GempakCommunicationException;

    /**
     * Communicate the given request and wait for a response. In between the
     * response and the request, this may receive intermediate requests to
     * respond to (using the registered handlers).
     *
     * @param request
     * @param responseType
     *            the expected response type
     * @return the response (may be null)
     * @throws GempakException
     */
    public <T> T request(IGempakRequest request, Class<T> responseType)
            throws GempakException;

    /**
     * Handle requests/messages (using the registered handlers) until a message
     * is received that indicates to stop processing, or an error occurs. This
     * method blocks until processing is completed.
     *
     * Subclasses are responsible for deciding which {@link GempakException}s
     * should cause processing to terminate, and which ones should allow for
     * further requests/messages to be processed.
     *
     * @throws GempakException
     *             an exception while processing a request/message that
     *             indicates that no further processing should be done
     */
    public void process() throws GempakException;

    /**
     * Register a handler for the given request type.
     *
     * @param requestType
     * @param handler
     */
    public <T extends IGempakRequest> void registerRequestHandler(
            Class<T> requestType, IGempakRequestHandler<T> handler);

    /**
     * Register a handler for the given message type.
     *
     * @param messageType
     * @param handler
     */
    public <T extends IGempakMessage> void registerMessageHandler(
            Class<T> messageType, IGempakMessageHandler<T> handler);
}
