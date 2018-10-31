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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.raytheon.uf.common.serialization.DynamicSerializationManager;
import com.raytheon.uf.common.serialization.DynamicSerializationManager.SerializationType;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.viz.gempak.common.exception.GempakCommunicationException;
import com.raytheon.uf.viz.gempak.common.exception.GempakException;
import com.raytheon.uf.viz.gempak.common.message.IGempakMessage;
import com.raytheon.uf.viz.gempak.common.request.IGempakRequest;

/**
 * Handles GEMPAK requests/responses and messages through provided input/output
 * streams.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 05, 2018 54480      mapeters    Initial creation
 * Sep 26, 2018 54483      mapeters    Flush streams as needed
 * Oct 16, 2018 54483      mapeters    Added {@link IGempakMessage} support, abstracted out
 *                                     handler logic to {@link AbstractGempakCommunicator}
 *
 * </pre>
 *
 * @author mapeters
 */
public class GempakStreamCommunicator extends AbstractGempakCommunicator {

    private final DynamicSerializationManager manager = DynamicSerializationManager
            .getManager(SerializationType.Thrift);

    private final InputStream is;

    private final OutputStream os;

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
    public void send(IGempakMessage request)
            throws GempakCommunicationException {
        sendInternal(request);
    }

    @Override
    public <T> T request(IGempakRequest request, Class<T> responseType)
            throws GempakException {
        if (IGempakRequest.class.isAssignableFrom(responseType)
                || IGempakMessage.class.isAssignableFrom(responseType)) {
            /*
             * Sanity check to ensure we can actually know when we've received
             * the expected response
             */
            throw new GempakCommunicationException(
                    "The expected response type cannot be "
                            + IGempakRequest.class.getSimpleName() + " or "
                            + IGempakMessage.class.getSimpleName()
                            + " (or any of their subtypes): "
                            + responseType.getClass().getName());
        }

        sendInternal(request);
        try {
            Object obj;
            while ((obj = manager.deserialize(is)) != null) {
                if (obj instanceof IGempakRequest) {
                    // Received intermediate request, respond to it
                    Object response = handleRequest((IGempakRequest) obj);
                    sendInternal(response);
                } else if (obj instanceof IGempakMessage) {
                    // Received intermediate message, handle it
                    if (!handleMessage((IGempakMessage) obj)) {
                        break;
                    }
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
             * Broke out of while loop by deserializing a null response or
             * receiving a message that indicated to stop processing, return
             * null
             */
            return null;
        } catch (SerializationException e) {
            throw new GempakCommunicationException(
                    "Error deserializing response for request: " + request, e);
        }
    }

    @Override
    public void process() throws GempakException {
        while (processInternal()) {
            /*
             * Empty loop to process individual requests/messages until one
             * returns false to indicate to stop
             */
        }
    }

    /**
     * Handle a single request/message (using the registered handlers). This
     * method blocks until a request/message is received.
     *
     * @return whether or not we should continue communicating and processing
     *         requests/messages
     * @throws GempakCommunicationException
     */
    private boolean processInternal() throws GempakException {
        Object obj;
        try {
            obj = manager.deserialize(is);
        } catch (SerializationException e) {
            throw new GempakCommunicationException(
                    "Error deserializing request", e);
        }

        if (obj instanceof IGempakRequest) {
            // Received request, respond to it
            Object response = handleRequest((IGempakRequest) obj);
            sendInternal(response);
        } else if (obj instanceof IGempakMessage) {
            // Received message, handle it
            return handleMessage((IGempakMessage) obj);
        } else {
            // Received unexpected type
            throw new GempakCommunicationException(
                    "Received unexpected request type: "
                            + obj.getClass().getName());
        }

        return true;
    }

    private void sendInternal(Object object)
            throws GempakCommunicationException {
        try {
            manager.serialize(object, os);
            os.flush();
        } catch (SerializationException | IOException e) {
            throw new GempakCommunicationException(
                    "Error sending object: " + object, e);
        }
    }
}
