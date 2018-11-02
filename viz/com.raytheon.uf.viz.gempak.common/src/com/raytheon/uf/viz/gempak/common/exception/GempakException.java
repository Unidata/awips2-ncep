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
 * Abstract class to provide a consistent supertype for GEMPAK exceptions.
 *
 * NOTE: Some of the logic used to process GEMPAK data significantly relies on
 * the type of exception thrown, so it is important that developers choose the
 * correct one, and consider this when creating new exception types.
 *
 * {@link GempakStrategyException} and its subclasses should be used for
 * anything involving the IGempakProcessingStrategy being used and its ability
 * to process future requests, e.g. if an error occurs connecting or
 * communicating between a subprocess and CAVE, or if a strategy is used after
 * it has been shutdown.
 *
 * {@link GempakProcessingException} should be used for anything that is not
 * influenced by the strategy being used, and does not influence the strategies
 * ability to process future requests. For example, the GempakCave*Retrievers
 * are used by both strategies, although they take different routes to get to
 * them. So, any errors in there should likely be
 * {@link GempakProcessingException}s, e.g. errors attempting to retrieve data
 * from EDEX.
 *
 * This is primarily important for the subprocess strategy, as it needs to know
 * which exceptions indicate that the subprocess itself is invalid
 * ({@link GempakStrategyException}), and which ones indicate an issue with the
 * data itself or something else ({@link GempakProcessingException}). This is
 * used to determine whether the subprocess should continue processing requests
 * or if it needs to shutdown when an exception occurs. It is also necessary to
 * keep the requests/responses between CAVE and a subprocess in sync.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 11, 2018 54480      mapeters    Initial creation
 * Nov 01, 2018 54483      mapeters    Make abstract, add to javadoc
 *
 * </pre>
 *
 * @author mapeters
 */
public abstract class GempakException extends Exception {

    private static final long serialVersionUID = 1L;

    public GempakException(String message) {
        super(message);
    }

    public GempakException(String message, Throwable t) {
        super(message, t);
    }

    public GempakException(Throwable t) {
        super(t);
    }
}
