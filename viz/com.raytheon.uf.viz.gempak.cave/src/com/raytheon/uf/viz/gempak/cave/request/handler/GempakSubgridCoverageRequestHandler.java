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

import com.raytheon.uf.common.geospatial.ISpatialObject;
import com.raytheon.uf.viz.gempak.cave.data.retriever.GempakCaveSubgridCoverageRetriever;
import com.raytheon.uf.viz.gempak.common.data.retriever.IGempakSubgridCoverageRetriever;
import com.raytheon.uf.viz.gempak.common.exception.GempakException;
import com.raytheon.uf.viz.gempak.common.request.GempakSubgridCoverageRequest;
import com.raytheon.uf.viz.gempak.common.request.handler.IGempakRequestHandler;

/**
 * Handler for taking a {@link GempakSubgridCoverageRequest}, processing it, and
 * returning the subgrid coverage {@link ISpatialObject}. This is done in CAVE.
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
public class GempakSubgridCoverageRequestHandler
        implements IGempakRequestHandler<GempakSubgridCoverageRequest> {

    private final IGempakSubgridCoverageRetriever retriever = new GempakCaveSubgridCoverageRetriever();

    @Override
    public ISpatialObject handleRequest(GempakSubgridCoverageRequest request)
            throws GempakException {
        return retriever.getSubgridCoverage(request);
    }
}
