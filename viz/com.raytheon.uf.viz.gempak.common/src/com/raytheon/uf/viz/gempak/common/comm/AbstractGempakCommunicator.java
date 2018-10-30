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
 */
package com.raytheon.uf.viz.gempak.common.comm;

import java.util.HashMap;
import java.util.Map;

import com.raytheon.uf.viz.gempak.common.exception.GempakCommunicationException;
import com.raytheon.uf.viz.gempak.common.exception.GempakException;
import com.raytheon.uf.viz.gempak.common.message.IGempakMessage;
import com.raytheon.uf.viz.gempak.common.message.handler.IGempakMessageHandler;
import com.raytheon.uf.viz.gempak.common.request.IGempakRequest;
import com.raytheon.uf.viz.gempak.common.request.handler.IGempakRequestHandler;

/**
 * Abstract class for communicating between CAVE and a GEMPAK subprocess.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 16, 2018 54483      mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public abstract class AbstractGempakCommunicator
        implements IGempakCommunicator {

    private final Map<Class<? extends IGempakRequest>, IGempakRequestHandler<? extends IGempakRequest>> requestHandlers = new HashMap<>();

    private final Map<Class<? extends IGempakMessage>, IGempakMessageHandler<? extends IGempakMessage>> messageHandlers = new HashMap<>();

    @Override
    public <T extends IGempakRequest> void registerRequestHandler(
            Class<T> requestType, IGempakRequestHandler<T> handler) {
        requestHandlers.put(requestType, handler);
    }

    /**
     * Handle the given request.
     *
     * @param request
     * @return the response object
     * @throws GempakException
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected Object handleRequest(IGempakRequest request)
            throws GempakException {
        IGempakRequestHandler handler = getRequestHandler(request);
        if (handler == null) {
            throw new GempakCommunicationException(
                    "No registered handler for request: "
                            + request.getClass().getName());
        }
        return handler.handleRequest(request);
    }

    @SuppressWarnings("unchecked")
    private <T extends IGempakRequest> IGempakRequestHandler<T> getRequestHandler(
            T request) {
        return (IGempakRequestHandler<T>) requestHandlers
                .get(request.getClass());
    }

    @Override
    public <T extends IGempakMessage> void registerMessageHandler(
            Class<T> messageType, IGempakMessageHandler<T> handler) {
        messageHandlers.put(messageType, handler);
    }

    /**
     * Handle the given message.
     *
     * @param message
     * @return whether or not to continue processing
     * @throws GempakException
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected boolean handleMessage(IGempakMessage message)
            throws GempakException {
        IGempakMessageHandler handler = getMessageHandler(message);
        if (handler == null) {
            throw new GempakCommunicationException(
                    "No registered handler for message: "
                            + message.getClass().getName());
        }
        return handler.handleMessage(message);
    }

    @SuppressWarnings("unchecked")
    private <T extends IGempakMessage> IGempakMessageHandler<T> getMessageHandler(
            T message) {
        return (IGempakMessageHandler<T>) messageHandlers
                .get(message.getClass());
    }
}
