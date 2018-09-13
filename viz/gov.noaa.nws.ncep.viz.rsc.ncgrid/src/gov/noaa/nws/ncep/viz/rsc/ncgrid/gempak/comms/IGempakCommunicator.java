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

import gov.noaa.nws.ncep.viz.rsc.ncgrid.gempak.IGempakRequest;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.gempak.IGempakRequestHandler;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.gempak.exception.GempakCommunicationException;

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
 *
 * </pre>
 *
 * @author mapeters
 */
public interface IGempakCommunicator {

    /**
     * Communicate the given request and wait for a response. In between the
     * response and the request, this may receive intermediate requests to
     * respond to (using the registered handlers).
     *
     * @param request
     * @param responseType
     *            the expected response type
     * @return the response (may be null)
     * @throws GempakCommunicationException
     */
    public <T> T request(IGempakRequest request, Class<T> responseType)
            throws GempakCommunicationException;

    /**
     * Respond to a request (using the registered handlers). This method blocks
     * until a request is received.
     *
     * @throws GempakCommunicationException
     */
    public void respond() throws GempakCommunicationException;

    /**
     * Register a handler for the given request type.
     *
     * @param requestType
     * @param handler
     */
    public <T extends IGempakRequest> void registerHandler(Class<T> requestType,
            IGempakRequestHandler<T> handler);
}
