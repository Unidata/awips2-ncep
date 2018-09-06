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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import com.raytheon.uf.common.serialization.DynamicSerializationManager;
import com.raytheon.uf.common.serialization.DynamicSerializationManager.SerializationType;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

/**
 *
 * Handles communicating with another process through provided input/output
 * streams.
 *
 * TODO wrap in a SocketCommunicator? Both implement IProcessCommunicator?
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 5, 2018  7417       mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public class StreamCommunicator {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(StreamCommunicator.class);

    private final DynamicSerializationManager manager = DynamicSerializationManager
            .getManager(SerializationType.Thrift);

    private final InputStream is;

    private final OutputStream os;

    private final Map<String, IGempakRequestHandler<? extends IGempakRequest>> handlers = new HashMap<>();

    /**
     * Constructor. The calling code is responsible for closing the streams when
     * necessary.
     *
     * @param is
     *            the input stream
     * @param os
     *            the output stream
     */
    public StreamCommunicator(InputStream is, OutputStream os) {
        this.is = is;
        this.os = os;
    }

    /**
     * Communicate the given request and wait for a response. In between the
     * response and the request, this may receive intermediate requests to
     * respond to (using the registered handlers).
     *
     * @param request
     * @param responseType
     *            the expected response type
     * @return the response (or null if an error occurred)
     */
    public <T> T request(IGempakRequest request, Class<T> responseType) {
        if (IGempakRequest.class.isAssignableFrom(responseType)) {
            /*
             * Sanity check to ensure we can actually know when we've received
             * the expected response
             */
            statusHandler.error("The expected response type cannot be "
                    + IGempakRequest.class.getSimpleName()
                    + " (or any of its subtypes): "
                    + responseType.getClass().getName());
            return null;
        }

        try {
            manager.serialize(request, os);

            Object obj;
            while ((obj = manager.deserialize(is)) != null) {
                if (obj instanceof IGempakRequest) {
                    // Received intermediate request, respond to it
                    if (!respond((IGempakRequest) obj)) {
                        // Failed to respond, stop request handling
                        break;
                    }
                } else if (responseType.isInstance(obj)) {
                    // Received response, return it
                    return responseType.cast(obj);
                } else {
                    // Received unexpected type, log and stop request handling
                    statusHandler.error("Received unexpected type: "
                            + obj.getClass().getName());
                    break;
                }
            }
        } catch (SerializationException e) {
            statusHandler
                    .error("Serialization error occurred when processing request: "
                            + request, e);
        }
        return null;
    }

    /**
     * Respond to a request (using the registered handlers). This method blocks
     * until a request is received.
     *
     * @return whether or not a request was successfully received and responded
     *         to
     */
    public boolean respond() {
        Object obj;
        try {
            // TODO need some sort of timeout if no request is being received?
            obj = manager.deserialize(is);
        } catch (SerializationException e) {
            statusHandler.error("Error deserializing a request", e);
            return false;
        }

        if (obj instanceof IGempakRequest) {
            // Received request, respond to it
            return respond((IGempakRequest) obj);
        } else {
            // Received unexpected type
            statusHandler.error("Received unexpected request type: "
                    + obj.getClass().getName());
        }

        return false;
    }

    /**
     * Respond to the given request.
     *
     * @param request
     * @return whether or not the request was successfully responded to
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private boolean respond(IGempakRequest request) {
        IGempakRequestHandler handler = getHandler(request);
        if (handler == null) {
            statusHandler.error("No registered handler for request: "
                    + request.getClass().getCanonicalName());
        } else {
            Object response = handler.handleRequest(request);
            try {
                manager.serialize(response, os);
                return true;
            } catch (SerializationException e) {
                statusHandler.error("Error serializing response: " + response,
                        e);
            }
        }

        return false;
    }

    /**
     * Register a handler for the given request type.
     *
     * @param requestType
     * @param handler
     */
    public <T extends IGempakRequest> void registerHandler(Class<T> requestType,
            IGempakRequestHandler<T> handler) {
        handlers.put(requestType.getCanonicalName(), handler);
    }

    @SuppressWarnings("unchecked")
    private <T extends IGempakRequest> IGempakRequestHandler<T> getHandler(
            T request) {
        return (IGempakRequestHandler<T>) handlers
                .get(request.getClass().getCanonicalName());
    }
}
