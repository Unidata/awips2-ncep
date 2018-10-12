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
package com.raytheon.uf.viz.gempak.subprocess;

import java.io.IOException;

import org.eclipse.equinox.app.IApplication;

import com.raytheon.uf.common.status.IPerformanceStatusHandler;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.PerformanceStatus;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.application.component.IStandaloneComponent;
import com.raytheon.uf.viz.gempak.common.comm.IGempakCommunicator;
import com.raytheon.uf.viz.gempak.common.conn.IGempakConnector;
import com.raytheon.uf.viz.gempak.common.exception.GempakCommunicationException;
import com.raytheon.uf.viz.gempak.common.exception.GempakConnectionException;
import com.raytheon.uf.viz.gempak.common.request.GempakDataRecordRequest;
import com.raytheon.uf.viz.gempak.subprocess.conn.GempakClientSocketConnector;
import com.raytheon.uf.viz.gempak.subprocess.request.handler.GempakDataRecordRequestHandler;

/**
 * This class is started as a subprocess from CAVE, and handles GEMPAK data
 * processing, communicating with a subprocess spawner on the CAVE side to take
 * the data to process and return the processed data record.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 05, 2018 54480      mapeters    Initial creation
 * Sep 26, 2018 54483      mapeters    Extract out data record request handler
 *                                     to its own class
 * Oct 08, 2018 54483      mapeters    Moved from gov.noaa.nws.ncep.viz.rsc.ncgrid.gempak,
 *                                     refactored to implement {@link IStandaloneComponent}
 *
 * </pre>
 *
 * @author mapeters
 */
public class GempakSubprocessor implements IStandaloneComponent {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(GempakSubprocessor.class);

    private static final IPerformanceStatusHandler perfLog = PerformanceStatus
            .getHandler(GempakSubprocessor.class.getSimpleName() + ":");

    @Override
    public Object startComponent(String componentName) throws Exception {
        /*
         * TODO need to initialize localization?
         */
        long t0 = System.currentTimeMillis();
        /*
         * Connect to server socket that was opened on the CAVE side for
         * communicating with us
         */
        try (IGempakConnector connector = new GempakClientSocketConnector(
                System.getenv())) {
            IGempakCommunicator communicator = connector.connect();
            /*
             * We are expecting CAVE to send us the data processing request, so
             * register the data request handler with the communicator and then
             * tell the communicator to read the request and respond to it
             */
            communicator.registerHandler(GempakDataRecordRequest.class,
                    new GempakDataRecordRequestHandler(communicator));
            communicator.respond();
        } catch (GempakConnectionException e) {
            statusHandler.error(
                    "Error connecting to CAVE from GEMPAK subprocess", e);
        } catch (GempakCommunicationException e) {
            statusHandler.error(
                    "Error communicating with CAVE from GEMPAK subprocess", e);
        } catch (IOException e) {
            statusHandler.warn("Error closing GEMPAK connection", e);
        }

        long t1 = System.currentTimeMillis();
        perfLog.logDuration(
                "Performing GEMPAK data processing (within subprocess)",
                t1 - t0);

        return IApplication.EXIT_OK;
    }
}