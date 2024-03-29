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

/**
 * Interface used to denote messages that are sent between CAVE and subprocesses
 * for GEMPAK data processing. Unlike requests, messages don't expect responses.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 16, 2018 54483      mapeters    Initial creation
 * Nov 02, 2018 54483      mapeters    Add {@link #isIntentionalException()}
 *
 * </pre>
 *
 * @author mapeters
 */
@DynamicSerialize
public interface IGempakMessage {

    /**
     * Return whether or not this message intentionally throws an exception that
     * indicates the current request should be canceled.
     *
     * @return true if this message intentionally throws an exception, false
     *         otherwise
     */
    default boolean isIntentionalException() {
        return false;
    }
}
