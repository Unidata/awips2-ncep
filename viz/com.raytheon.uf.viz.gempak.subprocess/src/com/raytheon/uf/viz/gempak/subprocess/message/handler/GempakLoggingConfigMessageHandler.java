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

import com.raytheon.uf.viz.gempak.common.exception.GempakException;
import com.raytheon.uf.viz.gempak.common.message.GempakLoggingConfigMessage;
import com.raytheon.uf.viz.gempak.common.message.handler.IGempakMessageHandler;
import com.raytheon.uf.viz.ncep.grid.NcgribLogger;

/**
 * Handler for processing a {@link GempakLoggingConfigMessage} to update the
 * logging configuration. This is done in a GEMPAK subprocess.
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
public class GempakLoggingConfigMessageHandler
        implements IGempakMessageHandler<GempakLoggingConfigMessage> {

    @Override
    public boolean handleMessage(GempakLoggingConfigMessage message)
            throws GempakException {
        NcgribLogger.getInstance().update(message.getConfig());
        return true;
    }
}
