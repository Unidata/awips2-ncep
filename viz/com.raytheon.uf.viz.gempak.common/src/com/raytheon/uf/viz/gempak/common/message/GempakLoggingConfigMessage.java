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
package com.raytheon.uf.viz.gempak.common.message;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.viz.ncep.grid.NcgribLogger;

/**
 * Message object for telling a GEMPAK subprocess the logging configuration to
 * use.
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
@DynamicSerialize
public class GempakLoggingConfigMessage implements IGempakMessage {

    @DynamicSerializeElement
    private NcgribLogger config;

    /**
     * Empty constructor for serialization.
     */
    public GempakLoggingConfigMessage() {
    }

    /**
     * Constructor.
     *
     * @param config
     *            the logging config
     */
    public GempakLoggingConfigMessage(NcgribLogger config) {
        this.config = config;
    }

    /**
     * @return the config
     */
    public NcgribLogger getConfig() {
        return config;
    }

    /**
     * @param config
     *            the config to set
     */
    public void setConfig(NcgribLogger config) {
        this.config = config;
    }
}
