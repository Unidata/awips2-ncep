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
package com.raytheon.uf.viz.gempak.cave.request.handler;

import com.raytheon.uf.viz.gempak.cave.data.retriever.GempakCaveNavigationRetriever;
import com.raytheon.uf.viz.gempak.common.data.GempakNavigationResponse;
import com.raytheon.uf.viz.gempak.common.exception.GempakException;
import com.raytheon.uf.viz.gempak.common.request.GempakNavigationRequest;
import com.raytheon.uf.viz.gempak.common.request.handler.IGempakRequestHandler;

/**
 * Handler for taking a {@link GempakNavigationRequest}, processing it, and
 * returning a {@link GempakNavigationResponse}. This is done in CAVE.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 25, 2018 54483      mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public class GempakNavigationRequestHandler
        implements IGempakRequestHandler<GempakNavigationRequest> {

    private final GempakCaveNavigationRetriever retriever = new GempakCaveNavigationRetriever();

    @Override
    public GempakNavigationResponse handleRequest(
            GempakNavigationRequest request) throws GempakException {
        return retriever.getNavigation(request);
    }
}
