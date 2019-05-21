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

import com.raytheon.uf.common.geospatial.ISpatialObject;
import com.raytheon.uf.viz.gempak.common.comm.IGempakCommunicator;
import com.raytheon.uf.viz.gempak.common.data.retriever.IGempakSubgridCoverageRetriever;
import com.raytheon.uf.viz.gempak.common.exception.GempakException;
import com.raytheon.uf.viz.gempak.common.request.GempakSubgridCoverageRequest;

/**
 * Implementation of {@link IGempakSubgridCoverageRetriever} for retrieving
 * GEMPAK subgrid coverage from a GEMPAK subprocess.
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
public class GempakSubprocessSubgridCoverageRetriever
        implements IGempakSubgridCoverageRetriever {

    private final IGempakCommunicator communicator;

    /**
     * Constructor.
     *
     * @param communicator
     *            used for sending requests to CAVE, since that is where the
     *            actual subgrid coverage retrieval is done
     */
    public GempakSubprocessSubgridCoverageRetriever(
            IGempakCommunicator communicator) {
        this.communicator = communicator;
    }

    @Override
    public ISpatialObject getSubgridCoverage(
            GempakSubgridCoverageRequest request) throws GempakException {
        return communicator.request(request, ISpatialObject.class);
    }
}
