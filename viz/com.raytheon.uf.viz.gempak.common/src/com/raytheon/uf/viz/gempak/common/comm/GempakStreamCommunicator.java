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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import com.raytheon.uf.common.serialization.DynamicSerializationManager;
import com.raytheon.uf.common.serialization.DynamicSerializationManager.SerializationType;
import com.raytheon.uf.viz.gempak.common.exception.GempakCommunicationException;
import com.raytheon.uf.viz.gempak.common.exception.GempakException;
import com.raytheon.uf.viz.gempak.common.request.IGempakRequest;
import com.raytheon.uf.viz.gempak.common.request.handler.IGempakRequestHandler;
import com.raytheon.uf.common.serialization.SerializationException;

/**
 * Handles GEMPAK requests/responses through provided input/output streams.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 05, 2018 54480      mapeters    Initial creation
 * Sep 26, 2018 54483      mapeters    Flush streams as needed
 *
 * </pre>
 *
 * @author mapeters
 */
public class GempakStreamCommunicator implements IGempakCommunicator {

    private final DynamicSerializationManager manager = DynamicSerializationManager
            .getManager(SerializationType.Thrift);

    private final InputStream is;

    private final OutputStream os;

    private final Map<Class<?>, IGempakRequestHandler<? extends IGempakRequest>> handlers = new HashMap<>();

    /**
     * Constructor. The calling code is responsible for closing the streams when
     * necessary.
     *
     * @param is
     *            the input stream
     * @param os
     *            the output stream
     */
    public GempakStreamCommunicator(InputStream is, OutputStream os) {
        this.is = is;
        this.os = os;
    }

    @Override
    public <T> T request(IGempakRequest request, Class<T> responseType)
            throws GempakCommunicationException {
        if (IGempakRequest.class.isAssignableFrom(responseType)) {
            /*
             * Sanity check to ensure we can actually know when we've received
             * the expected response
             */
            throw new GempakCommunicationException(
                    "The expected response type cannot be "
                            + IGempakRequest.class.getSimpleName()
                            + " (or any of its subtypes): "
                            + responseType.getClass().getName());
        }

        try {
            manager.serialize(request, os);
            os.flush();

            Object obj;
            while ((obj = manager.deserialize(is)) != null) {
                if (obj instanceof IGempakRequest) {
                    // Received intermediate request, respond to it
                    respond((IGempakRequest) obj);
                } else if (responseType.isInstance(obj)) {
                    // Received response, return it
                    return responseType.cast(obj);
                } else {
                    // Received unexpected type
                    throw new GempakCommunicationException(
                            "Received unexpected type: "
                                    + obj.getClass().getName());
                }
            }

            /*
             * Broke out of while loop by deserializing a null response, return
             * it
             */
            return null;
        } catch (SerializationException e) {
            throw new GempakCommunicationException(
                    "Serialization error occurred when processing request: "
                            + request,
                    e);
        } catch (IOException e) {
            throw new GempakCommunicationException(
                    "Error flushing serialized request to output stream: "
                            + request,
                    e);
        }
    }

    @Override
    public void respond() throws GempakCommunicationException {
        Object obj;
        try {
            // TODO need some sort of timeout if no request is being received?
            obj = manager.deserialize(is);
        } catch (SerializationException e) {
            throw new GempakCommunicationException(
                    "Error deserializing request", e);
        }

        if (obj instanceof IGempakRequest) {
            // Received request, respond to it
            respond((IGempakRequest) obj);
        } else {
            // Received unexpected type
            throw new GempakCommunicationException(
                    "Received unexpected request type: "
                            + obj.getClass().getName());
        }
    }

    /**
     * Respond to the given request.
     *
     * @param request
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void respond(IGempakRequest request)
            throws GempakCommunicationException {
        IGempakRequestHandler handler = getHandler(request);
        if (handler == null) {
            throw new GempakCommunicationException(
                    "No registered handler for request: "
                            + request.getClass().getName());
        }

        // Process request
        Object response;
        try {
            response = handler.handleRequest(request);
        } catch (GempakException e) {
            throw new GempakCommunicationException(
                    "Error handling request: " + request, e);
        }

        // Send back response
        try {
            manager.serialize(response, os);
            os.flush();
        } catch (SerializationException | IOException e) {
            throw new GempakCommunicationException(
                    "Error sending response: " + response, e);
        }
    }

    @Override
    public <T extends IGempakRequest> void registerHandler(Class<T> requestType,
            IGempakRequestHandler<T> handler) {
        handlers.put(requestType, handler);
    }

    @SuppressWarnings("unchecked")
    private <T extends IGempakRequest> IGempakRequestHandler<T> getHandler(
            T request) {
        return (IGempakRequestHandler<T>) handlers.get(request.getClass());
    }
}
