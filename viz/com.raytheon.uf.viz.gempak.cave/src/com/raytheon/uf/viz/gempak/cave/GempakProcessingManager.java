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
package com.raytheon.uf.viz.gempak.cave;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.xml.bind.JAXBException;

import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.ILocalizationPathObserver;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.SingleTypeJAXBManager;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.gempak.cave.config.GempakProcessingConfig;
import com.raytheon.uf.viz.gempak.cave.strategy.GempakSameProcessStrategy;
import com.raytheon.uf.viz.gempak.cave.strategy.GempakSubprocessStrategy;
import com.raytheon.uf.viz.gempak.cave.strategy.IGempakProcessingStrategy;
import com.raytheon.uf.viz.gempak.common.data.GempakDataInput;
import com.raytheon.uf.viz.gempak.common.data.GempakDataRecord;
import com.raytheon.uf.viz.gempak.common.exception.GempakConnectionException;
import com.raytheon.uf.viz.gempak.common.exception.GempakException;
import com.raytheon.uf.viz.gempak.common.util.GempakProcessingUtil;

/**
 * Manages processing data through GEMPAK.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 05, 2018 54480      mapeters    Initial creation
 * Sep 26, 2018 54483      mapeters    Propagate exception in
 *                                     {@link #getDataRecord(GempakDataInput)}
 * Oct 08, 2018 54483      mapeters    Moved from gov.noaa.nws.ncep.viz.rsc.ncgrid.gempak,
 *                                     only use subprocesses if subprocess RPM installed
 * Oct 16, 2018 54483      mapeters    Listen to localization file changes, add
 *                                     activation/deactivation support
 * Oct 25, 2018 54483      mapeters    Remove redundant log message
 *
 * </pre>
 *
 * @author mapeters
 */
public class GempakProcessingManager implements ILocalizationPathObserver {

    private static final GempakProcessingManager INSTANCE = new GempakProcessingManager();

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(getClass());

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private GempakActivationStatus status = GempakActivationStatus.INACTIVE;

    private IGempakProcessingStrategy processingStrategy;

    /**
     * Private constructor for singleton.
     */
    private GempakProcessingManager() {
    }

    /**
     * Gets the singleton manager instance.
     *
     * @return the {@link GempakProcessingManager} singleton
     */
    public static GempakProcessingManager getInstance() {
        return INSTANCE;
    }

    /**
     * Process the data input through GEMPAK and return the data record result.
     *
     * @param dataInput
     *            the data to process
     * @return the processed data record (may be null)
     * @throws GempakException
     *             if an error occurs processing the data
     */
    public GempakDataRecord getDataRecord(GempakDataInput dataInput)
            throws GempakException {
        boolean active = true;

        /*
         * Only activate() if not ACTIVE already, to avoid getting write lock
         * and decreasing concurrency unless it is necessary
         */
        lock.readLock().lock();
        if (status != GempakActivationStatus.ACTIVE) {
            lock.readLock().unlock();
            lock.writeLock().lock();
            active = activate();
            /*
             * Get read lock before releasing write lock to ensure status
             * doesn't change before doing the data processing
             */
            lock.readLock().lock();
            lock.writeLock().unlock();
        }

        try {
            if (active) {
                return processingStrategy.getDataRecord(dataInput);
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Initialize the processing strategy from the current config, shutting down
     * the current strategy (if non-null). The caller is responsible for locking
     * appropriately.
     */
    private void initProcessingStrategy() {
        shutdownProcessingStrategy();
        /*
         * Use subprocess strategy if config exists and specifies it and the
         * subprocess RPM is installed, otherwise default to same process
         * strategy
         */
        GempakProcessingConfig config = readConfig();
        IGempakProcessingStrategy newProcessingStrategy = null;
        if (config != null && config.isRunInSubprocess()) {
            boolean gempakRpmInstalled = new File(
                    GempakProcessingUtil.SUBPROCESS_SCRIPT_PATH).isFile();
            if (gempakRpmInstalled) {
                int subprocessLimit = GempakProcessingUtil
                        .validateSubprocessLimit(config.getSubprocessLimit());
                try {
                    newProcessingStrategy = new GempakSubprocessStrategy(
                            subprocessLimit);
                    statusHandler.info("GEMPAK processing will be performed in "
                            + "subprocesses, with a limit of " + subprocessLimit
                            + " concurrent subprocesses");
                } catch (GempakConnectionException e) {
                    statusHandler.error(
                            "GEMPAK processing is configured to be performed in "
                                    + "subprocesses, but an error occurred initializing them",
                            e);
                }
            } else {
                statusHandler
                        .warn("GEMPAK processing is configured to be performed in "
                                + "subprocesses, but the GEMPAK RPM is not installed");
            }
        }

        if (newProcessingStrategy == null) {
            newProcessingStrategy = new GempakSameProcessStrategy();
            statusHandler.info(
                    "GEMPAK processing will be performed in the CAVE process");
        }

        processingStrategy = newProcessingStrategy;
    }

    private GempakProcessingConfig readConfig() {
        ILocalizationFile configFile = PathManagerFactory.getPathManager()
                .getStaticLocalizationFile(GempakProcessingUtil.CONFIG_PATH);
        GempakProcessingConfig config = null;
        try (InputStream is = configFile.openInputStream()) {
            SingleTypeJAXBManager<GempakProcessingConfig> jaxb = new SingleTypeJAXBManager<>(
                    GempakProcessingConfig.class);
            config = jaxb.unmarshalFromInputStream(is);
        } catch (JAXBException | IOException | LocalizationException
                | SerializationException e) {
            statusHandler.error("Error reading GEMPAK config file: "
                    + GempakProcessingUtil.CONFIG_PATH, e);
        }

        return config;
    }

    @Override
    public void fileChanged(ILocalizationFile file) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                lock.writeLock().lock();
                try {
                    /*
                     * Only need to update for config changes if currently
                     * active
                     */
                    if (status == GempakActivationStatus.ACTIVE) {
                        initProcessingStrategy();
                    }
                } finally {
                    lock.writeLock().unlock();
                }
            }
        }).start();
    }

    /**
     * Attempt to activate GEMPAK processing.
     *
     * @return true if activation is successful, false otherwise
     */
    public boolean activate() {
        lock.writeLock().lock();
        try {
            switch (status) {
            case ACTIVE:
                // Already active, nothing to do
                break;
            case INACTIVE:
                PathManagerFactory.getPathManager().addLocalizationPathObserver(
                        GempakProcessingUtil.CONFIG_PATH, this);
                initProcessingStrategy();
                status = GempakActivationStatus.ACTIVE;
                break;
            case SHUTDOWN:
                statusHandler.warn("Attempted to perform GEMPAK processing "
                        + "after being permanently shutdown. "
                        + "This should only happen on CAVE shutdown.");
            default:
                // All options should be covered above
                throw new IllegalArgumentException(
                        "Unexpected GEMPAK activation status: " + status);
            }
        } finally {
            lock.writeLock().unlock();
        }
        return true;
    }

    /**
     * Shutdown GEMPAK processing, either temporarily to release idle resources
     * or permanently on CAVE shutdown.
     *
     * @param permanent
     *            true if GEMPAK processing should not be reactivated (this
     *            should only be done on CAVE shutdown), false if it can be
     *            reactivated when needed
     */
    public void shutdown(boolean permanent) {
        lock.writeLock().lock();
        try {
            PathManagerFactory.getPathManager().removeLocalizationPathObserver(
                    GempakProcessingUtil.CONFIG_PATH, this);
            shutdownProcessingStrategy();
            status = permanent ? GempakActivationStatus.SHUTDOWN
                    : GempakActivationStatus.INACTIVE;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Restart GEMPAK processing, shutting down the current strategy and
     * re-initializing a new strategy from the current configuration.
     */
    public void restart() {
        lock.writeLock().lock();
        try {
            shutdown(false);
            activate();
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * Shutdown the current processing strategy (if non-null).
     */
    private void shutdownProcessingStrategy() {
        lock.writeLock().lock();
        try {
            if (processingStrategy != null) {
                processingStrategy.shutdown();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private static enum GempakActivationStatus {

        /**
         * Status indicating that the processing strategy is ready to process
         * requests
         */
        ACTIVE,

        /**
         * Status indicating that the processing strategy is currently shutdown,
         * but can be re-activated when needed
         */
        INACTIVE,

        /**
         * Status indicating that the processing strategy is shutdown and should
         * not be re-activated (this should only occur on CAVE shutdown)
         */
        SHUTDOWN
    }
}
