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
package com.raytheon.uf.viz.gempak.subprocess.data.retriever;

import com.raytheon.uf.viz.gempak.common.comm.IGempakCommunicator;
import com.raytheon.uf.viz.gempak.common.data.retriever.IGempakDataURIRetriever;
import com.raytheon.uf.viz.gempak.common.exception.GempakException;
import com.raytheon.uf.viz.gempak.common.request.GempakDataURIRequest;

/**
 * Implementation of {@link IGempakDataURIRetriever} for retrieving GEMPAK data
 * URIs from a GEMPAK subprocess.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 10, 2018 54483      mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public class GempakSubprocessDataURIRetriever
        implements IGempakDataURIRetriever {

    private final IGempakCommunicator communicator;

    /**
     * Constructor.
     *
     * @param communicator
     *            used for sending requests to CAVE, since that is where the
     *            actual data URI retrieval/caching is done
     */
    public GempakSubprocessDataURIRetriever(IGempakCommunicator communicator) {
        this.communicator = communicator;
    }

    @Override
    public String getDataURI(GempakDataURIRequest gempakRequest)
            throws GempakException {
        return communicator.request(gempakRequest, String.class);
    }
}
