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

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.gempak.common.exception.GempakCommunicationException;
import com.raytheon.uf.viz.gempak.common.exception.GempakException;
import com.raytheon.uf.viz.gempak.common.exception.GempakProcessingException;
import com.raytheon.uf.viz.gempak.common.message.GempakProcessingExceptionMessage;
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
 * Nov 01, 2018 54483      mapeters    Communicate {@link GempakProcessingException}s across
 *                                     processes during request/response, to prevent them from
 *                                     getting out of sync
 *
 * </pre>
 *
 * @author mapeters
 */
public abstract class AbstractGempakCommunicator
        implements IGempakCommunicator {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(AbstractGempakCommunicator.class);

    private final Map<Class<? extends IGempakRequest>, IGempakRequestHandler<? extends IGempakRequest>> requestHandlers = new HashMap<>();

    private final Map<Class<? extends IGempakMessage>, IGempakMessageHandler<? extends IGempakMessage>> messageHandlers = new HashMap<>();

    @Override
    public <T extends IGempakRequest> void registerRequestHandler(
            Class<T> requestType, IGempakRequestHandler<T> handler) {
        requestHandlers.put(requestType, handler);
    }

    /**
     * Respond to the given request.
     *
     * @param request
     * @throws GempakException
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void handleRequest(IGempakRequest request)
            throws GempakException {
        IGempakRequestHandler handler = getRequestHandler(request);
        if (handler == null) {
            throw new GempakCommunicationException(
                    "No registered handler for request: "
                            + request.getClass().getName());
        }

        try {
            Object response = handler.handleRequest(request);
            sendObject(response);
        } catch (GempakProcessingException e) {
            /*
             * An error occurred that is specific to this request and shouldn't
             * influence future requests. The other process is still expecting a
             * response, so we send the exception across so that it knows what
             * happened and can move on from this request. Other exceptions are
             * propagated because they indicate an error with the actual
             * connection/communication, so we need to fully cancel the current
             * request/message processing in that case.
             */
            statusHandler.error("Error processing GEMPAK request: " + request,
                    e);
            send(new GempakProcessingExceptionMessage(e));
        }
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

        try {
            return handler.handleMessage(message);
        } catch (GempakProcessingException e) {
            /*
             * An error occurred that is specific to this request and shouldn't
             * influence future requests. Other errors are allowed to propagate
             * as they indicate an error with the actual
             * connection/communication, which prevents us from being able to
             * process future requests correctly.
             */
            if (message.isIntentionalException()) {
                /*
                 * If the message's purpose is to indicate an error that
                 * occurred in the other process, propagate the exception in
                 * case this message is part of a bigger request in order to
                 * cancel the overall request, since the other side will be
                 * moving on as well. We do this instead of returning false
                 * because we still want to process future requests.
                 */
                throw e;
            }

            /*
             * If the exception wasn't intentional to cancel the current
             * processing, just log it and try to continue with the current
             * processing, as the other process will be proceeding normally as
             * well.
             */
            statusHandler.error("Error processing GEMPAK message: " + message,
                    e);
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    private <T extends IGempakMessage> IGempakMessageHandler<T> getMessageHandler(
            T message) {
        return (IGempakMessageHandler<T>) messageHandlers
                .get(message.getClass());
    }

    /**
     * Send the given object.
     *
     * @param object
     * @throws GempakCommunicationException
     *             if an error occurs sending the object
     */
    protected abstract void sendObject(Object object)
            throws GempakCommunicationException;
}
