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
package com.raytheon.uf.viz.gempak.common.exception;

/**
 * A GEMPAK exception for when an error occurs that is specific to the current
 * processing strategy (IGempakProcessingStrategy) being used and influences its
 * ability to process future requests.
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
 * Oct 16, 2018 54483      mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public class GempakStrategyException extends GempakException {

    private static final long serialVersionUID = 1L;

    public GempakStrategyException(String message) {
        super(message);
    }

    public GempakStrategyException(String message, Throwable t) {
        super(message, t);
    }

    public GempakStrategyException(Throwable t) {
        super(t);
    }
}
