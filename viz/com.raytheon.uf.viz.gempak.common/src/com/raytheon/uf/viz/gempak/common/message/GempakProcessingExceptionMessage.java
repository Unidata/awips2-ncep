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
import com.raytheon.uf.viz.gempak.common.exception.GempakProcessingException;

/**
 * Message object for telling the other process (either CAVE or a GEMPAK
 * subprocess, depending which one we are in) that an
 * {@link GempakProcessingException} occrrred.
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
@DynamicSerialize
public class GempakProcessingExceptionMessage implements IGempakMessage {

    @DynamicSerializeElement
    private GempakProcessingException exception;

    /**
     * Empty constructor for serialization.
     */
    public GempakProcessingExceptionMessage() {
    }

    /**
     * Constructor
     *
     * @param e
     *            the exception to communicate
     */
    public GempakProcessingExceptionMessage(GempakProcessingException e) {
        this.exception = e;
    }

    /**
     * @return the exception
     */
    public GempakProcessingException getException() {
        return exception;
    }

    /**
     * @param exception
     *            the exception to set
     */
    public void setException(GempakProcessingException exception) {
        this.exception = exception;
    }

    @Override
    public boolean isIntentionalException() {
        return true;
    }
}
