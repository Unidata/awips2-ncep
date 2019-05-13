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
package com.raytheon.uf.viz.gempak.common.message.handler;

import com.raytheon.uf.viz.gempak.common.exception.GempakProcessingException;
import com.raytheon.uf.viz.gempak.common.message.GempakProcessingExceptionMessage;

/**
 * Handler for processing a {@link GempakProcessingExceptionMessage} indicating
 * that an exception occurred in the other process. This is used in both CAVE
 * and GEMPAK subprocesses.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 01, 2018 54483      mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public class GempakProcessingExceptionMessageHandler
        implements IGempakMessageHandler<GempakProcessingExceptionMessage> {

    @Override
    public boolean handleMessage(GempakProcessingExceptionMessage message)
            throws GempakProcessingException {
        /*
         * Just throw the exception to indicate that the current processing
         * failed
         */
        throw new GempakProcessingException(
                "Error occurred performing GEMPAK processing in other process",
                message.getException());
    }
}
