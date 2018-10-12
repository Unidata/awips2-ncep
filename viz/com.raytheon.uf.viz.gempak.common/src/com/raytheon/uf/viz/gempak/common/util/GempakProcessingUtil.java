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
package com.raytheon.uf.viz.gempak.common.util;

import java.io.File;

import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

/**
 * Constants and utility methods for GEMPAK processing.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 05, 2018 54480      mapeters    Initial creation
 * Oct 08, 2018 54483      mapeters    Moved from gov.noaa.nws.ncep.viz.rsc.ncgrid.gempak,
 *                                     subprocess script moved to /awips2/gempak/gempak.sh
 *
 * </pre>
 *
 * @author mapeters
 */
public class GempakProcessingUtil {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(GempakProcessingUtil.class);

    /**
     * The key used for mapping GEMPAK ports.
     */
    public static final String PORT_KEY = "gempakPort";

    /**
     * The localization path of the GEMPAK processing config file.
     */
    public static final String CONFIG_PATH = "ncep" + IPathManager.SEPARATOR
            + "gempak" + IPathManager.SEPARATOR + "gempakProcessing.xml";

    /**
     * The path to the GEMPAK subprocess start-up script.
     */
    public static final String SUBPROCESS_SCRIPT_PATH = File.separator
            + "awips2" + File.separator + "gempak" + File.separator
            + "gempak.sh";

    private static final int MIN_SUBPROCESS_LIMIT = 1;

    /*
     * TODO: Set better max limit, should try to be dynamic with hardware
     * changes as well, instead of hard-coded.
     */
    private static final int MAX_SUBPROCESS_LIMIT = 10;

    /**
     * Private constructor to prevent instantiation (everything's static).
     */
    private GempakProcessingUtil() {
    }

    /**
     * Determine the valid subprocess limit to use based on the provided limit.
     *
     * @param subprocessLimit
     *            the limit to validate
     * @return a valid limit
     */
    public static int validateSubprocessLimit(int subprocessLimit) {
        if (subprocessLimit < MIN_SUBPROCESS_LIMIT) {
            statusHandler.warn("Invalid subprocess limit (" + subprocessLimit
                    + ") in " + CONFIG_PATH
                    + ", setting to minimum accepted value ("
                    + MIN_SUBPROCESS_LIMIT + ")");
            subprocessLimit = MIN_SUBPROCESS_LIMIT;
        } else if (subprocessLimit > MAX_SUBPROCESS_LIMIT) {
            statusHandler.warn("Invalid subprocess limit (" + subprocessLimit
                    + ") in " + CONFIG_PATH
                    + ", setting to maximum accepted value ("
                    + MAX_SUBPROCESS_LIMIT + ")");
            subprocessLimit = MAX_SUBPROCESS_LIMIT;
        }
        return subprocessLimit;
    }
}
