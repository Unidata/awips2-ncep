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

import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.viz.application.component.IStandaloneComponent;
import com.raytheon.uf.viz.core.localization.CAVELocalizationAdapter;
import com.raytheon.uf.viz.core.localization.LocalizationInitializer;
import com.raytheon.uf.viz.core.status.VizStatusHandlerFactory;
import com.raytheon.uf.viz.gempak.common.comm.IGempakCommunicator;
import com.raytheon.uf.viz.gempak.common.conn.IGempakConnector;
import com.raytheon.uf.viz.gempak.common.exception.GempakException;
import com.raytheon.uf.viz.gempak.common.message.GempakLoggingConfigMessage;
import com.raytheon.uf.viz.gempak.common.message.GempakShutdownMessage;
import com.raytheon.uf.viz.gempak.common.request.GempakDataRecordRequest;
import com.raytheon.uf.viz.gempak.subprocess.conn.GempakClientSocketConnector;
import com.raytheon.uf.viz.gempak.subprocess.message.handler.GempakLoggingConfigMessageHandler;
import com.raytheon.uf.viz.gempak.subprocess.message.handler.GempakShutdownMessageHandler;
import com.raytheon.uf.viz.gempak.subprocess.request.handler.GempakDataRecordRequestHandler;

/**
 * This class is started as a subprocess from CAVE, and handles GEMPAK data
 * processing, communicating with a subprocess spawner on the CAVE side to take
 * data requests to process and return the processed data records until it is
 * told to shutdown.
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
 * Oct 16, 2018 54483      mapeters    Single subprocessor handles multiple requests until
 *                                     receiving shutdown request, initialize localization
 * Oct 25, 2018 54483      mapeters    Handle {@link GempakLoggingConfigMessage}s, match
 *                                     CAVE logging and prevent Eclipse error pop-ups
 *
 * </pre>
 *
 * @author mapeters
 */
public class GempakSubprocessor implements IStandaloneComponent {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(GempakSubprocessor.class);

    @Override
    public Object startComponent(String componentName) {
        /*
         * Wrap everything in a try/catch and return OK status, as throwing an
         * exception here (or returning a different status) may cause Eclipse to
         * display a pop-up dialog, which we don't want
         */
        try {
            startComponentInternal();
        } catch (Exception e) {
            statusHandler.fatal("Error running GEMPAK subprocess", e);
        }

        return IApplication.EXIT_OK;
    }

    private void startComponentInternal() throws Exception {
        long t0 = System.currentTimeMillis();

        initializeLocalization();

        UFStatus.setHandlerFactory(new VizStatusHandlerFactory());

        /*
         * Connect to server socket that was opened on the CAVE side for
         * communicating with us
         */
        try (IGempakConnector connector = new GempakClientSocketConnector(
                System.getenv())) {
            IGempakCommunicator communicator = connector.connect();
            /*
             * We are expecting CAVE to send us data processing requests, so
             * register the data request handler with the communicator and then
             * tell the communicator to read the requests and respond to them
             *
             * NOTE: these request handlers should only throw exceptions for
             * errors that indicate an invalid subprocess (as opposed to an
             * invalid data request, for example), as any exception will cause
             * this subprocess to shutdown
             */
            communicator.registerRequestHandler(GempakDataRecordRequest.class,
                    new GempakDataRecordRequestHandler(communicator));
            communicator.registerMessageHandler(
                    GempakLoggingConfigMessage.class,
                    new GempakLoggingConfigMessageHandler());
            communicator.registerMessageHandler(GempakShutdownMessage.class,
                    new GempakShutdownMessageHandler());
            communicator.process();
        } catch (GempakException e) {
            statusHandler.error(
                    "Error performing GEMPAK data processing in subprocess", e);
        } catch (IOException e) {
            statusHandler.warn("Error closing GEMPAK connection", e);
        }

        long t1 = System.currentTimeMillis();
        long durationSeconds = (t1 - t0) / TimeUtil.MILLIS_PER_SECOND;
        statusHandler.debug("GEMPAK subprocess shutting down after running for "
                + durationSeconds + " s");
    }

    private void initializeLocalization() throws Exception {
        PathManagerFactory.setAdapter(new CAVELocalizationAdapter());
        new LocalizationInitializer(false, false).run();
    }
}