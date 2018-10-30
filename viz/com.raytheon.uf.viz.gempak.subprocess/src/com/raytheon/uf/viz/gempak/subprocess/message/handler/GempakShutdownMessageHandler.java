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
package com.raytheon.uf.viz.gempak.subprocess.message.handler;

import com.raytheon.uf.viz.gempak.common.message.GempakShutdownMessage;
import com.raytheon.uf.viz.gempak.common.message.handler.IGempakMessageHandler;

/**
 * Handler for processing a {@link GempakShutdownMessage} to stop GEMPAK data
 * processing. This is done in a GEMPAK subprocess.
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
 */
public class GempakShutdownMessageHandler
        implements IGempakMessageHandler<GempakShutdownMessage> {

    @Override
    public boolean handleMessage(GempakShutdownMessage message) {
        // Just indicate that we should stop GEMPAK data processing
        return false;
    }
}
