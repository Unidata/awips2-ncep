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
package gov.noaa.nws.ncep.viz.rsc.ncgrid.gempak.comms;

import java.io.Closeable;

import gov.noaa.nws.ncep.viz.rsc.ncgrid.gempak.exception.GempakConnectionException;

/**
 * Interface for creating a connection between CAVE and a GEMPAK subprocess to
 * allow them to communicate.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 07, 2018 54480      mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public interface IGempakConnector extends Closeable {

    /**
     * Connect to either CAVE or a GEMPAK subprocess (depending on which side we
     * are on). This method may block if it has to wait for the other side to
     * connect.
     *
     * @return an {@link IGempakCommunicator} for communicating over the
     *         connection
     * @throws GempakConnectionException
     *             if an error occurs when connecting
     */
    IGempakCommunicator connect() throws GempakConnectionException;
}
