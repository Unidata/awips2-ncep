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
package com.raytheon.uf.viz.gempak.common.exception;

/**
 * A GEMPAK exception for when an error occurs during the actual data processing
 * of an individual request. It is not influenced by the current processing
 * strategy being used and should not influence the ability for the strategy to
 * process further requests.
 *
 * NOTE: See {@link GempakException}'s javadoc for the importance of the
 * exceptions used in GEMPAK processing.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 11, 2018 54480      mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public class GempakProcessingException extends GempakException {

    private static final long serialVersionUID = 1L;

    public GempakProcessingException(String message) {
        super(message);
    }

    public GempakProcessingException(String message, Throwable t) {
        super(message, t);
    }

    public GempakProcessingException(Throwable t) {
        super(t);
    }
}
