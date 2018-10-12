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
package com.raytheon.uf.viz.gempak.common.request.handler;

import com.raytheon.uf.viz.gempak.common.exception.GempakException;
import com.raytheon.uf.viz.gempak.common.request.IGempakRequest;

/**
 * Handler for {@link IGempakRequest}s sent between a GEMPAK subprocess and
 * CAVE.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 05, 2018 54480      mapeters    Initial creation
 * Sep 26, 2018 54483      mapeters    {@link #handleRequest} throws {@link GempakException}
 *
 * </pre>
 *
 * @author mapeters
 * @param <T>
 *            the request type handled by this handler
 */
public interface IGempakRequestHandler<T extends IGempakRequest> {

    /**
     * Handle the given request and return the response.
     *
     * @param request
     * @return the response
     * @throws GempakException
     *             if an error occurs processing the request
     */
    Object handleRequest(T request) throws GempakException;
}