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
 * An exception for when an error occurs performing Dgdriv functions.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 *                                     Initial creation
 * Oct 08, 2018 54483      mapeters    Moved from gov.noaa.nws.ncep.viz.rsc.ncgrid.dgdriv
 *
 * </pre>
 *
 * @author unknown
 */
public class DgdrivException extends Exception {

    private static final long serialVersionUID = 1L;

    public DgdrivException() {
        super();
    }

    public DgdrivException(String message) {
        super(message);
    }

    public DgdrivException(String message, Throwable cause) {
        super(message, cause);
    }

    public DgdrivException(Throwable cause) {
        super(cause);
    }

}
