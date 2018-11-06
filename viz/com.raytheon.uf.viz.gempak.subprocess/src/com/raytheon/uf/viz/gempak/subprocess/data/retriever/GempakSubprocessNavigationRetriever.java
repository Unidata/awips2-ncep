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
package com.raytheon.uf.viz.gempak.subprocess.data.retriever;

import com.raytheon.uf.viz.gempak.common.comm.IGempakCommunicator;
import com.raytheon.uf.viz.gempak.common.data.GempakNavigationResponse;
import com.raytheon.uf.viz.gempak.common.data.retriever.IGempakNavigationRetriever;
import com.raytheon.uf.viz.gempak.common.exception.GempakException;
import com.raytheon.uf.viz.gempak.common.request.GempakNavigationRequest;

/**
 * Implementation of {@link IGempakNavigationRetriever} for retrieving a GEMPAK
 * grid navigation string from a GEMPAK subprocess.
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
public class GempakSubprocessNavigationRetriever
        implements IGempakNavigationRetriever {

    private final IGempakCommunicator communicator;

    /**
     * Constructor.
     *
     * @param communicator
     *            used for sending requests to CAVE, since that is where the
     *            actual navigation retrieval is done
     */
    public GempakSubprocessNavigationRetriever(
            IGempakCommunicator communicator) {
        this.communicator = communicator;
    }

    @Override
    public GempakNavigationResponse getNavigation(
            GempakNavigationRequest request) throws GempakException {
        return communicator.request(request, GempakNavigationResponse.class);
    }
}
