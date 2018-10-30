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

import com.raytheon.uf.viz.gempak.common.exception.GempakException;
import com.raytheon.uf.viz.gempak.common.message.IGempakMessage;

/**
 * Handler for {@link IGempakMessage}s sent between a GEMPAK subprocess and
 * CAVE.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 16, 2018 54483      mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 * @param <T>
 *            the message type handled by this handler
 */
public interface IGempakMessageHandler<T extends IGempakMessage> {

    /**
     * Handle the given message.
     *
     * @param message
     * @return whether or not to continue GEMPAK data processing
     * @throws GempakException
     *             if an error occurs processing the message
     */
    boolean handleMessage(T message) throws GempakException;
}
