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
package com.raytheon.uf.viz.gempak.cave.strategy;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.raytheon.uf.common.status.IPerformanceStatusHandler;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.PerformanceStatus;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.gempak.cave.GempakProcessingManager;
import com.raytheon.uf.viz.gempak.cave.GempakSubprocessSpawner;
import com.raytheon.uf.viz.gempak.common.data.GempakDataInput;
import com.raytheon.uf.viz.gempak.common.data.GempakDataRecord;
import com.raytheon.uf.viz.gempak.common.exception.GempakCommunicationException;
import com.raytheon.uf.viz.gempak.common.exception.GempakConnectionException;
import com.raytheon.uf.viz.gempak.common.exception.GempakException;
import com.raytheon.uf.viz.gempak.common.exception.GempakShutdownException;
import com.raytheon.uf.viz.gempak.common.util.GempakProcessingUtil;

/**
 * GEMPAK processing strategy that spawns a fixed set of subprocesses to perform
 * concurrent data processing. The subprocesses run until this strategy is
 * shutdown, processing requests as they occur.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 04, 2018 54480      mapeters    Initial creation
 * Oct 16, 2018 54483      mapeters    Single subprocess can handle multiple requests, add
 *                                     shutdown support
 *
 * </pre>
 *
 * @author mapeters
 */
public class GempakSubprocessStrategy implements IGempakProcessingStrategy {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(GempakSubprocessStrategy.class);

    private static final IPerformanceStatusHandler perfLog = PerformanceStatus
            .getHandler(GempakSubprocessStrategy.class.getSimpleName() + ":");

    private static final long SPAWNER_POLL_INTERVAL_MS = 10_000;

    private final ReentrantReadWriteLock shutdownLock = new ReentrantReadWriteLock();

    private final BlockingQueue<GempakSubprocessSpawner> idleSubprocessSpawners;

    private final AtomicInteger subprocessLimit;

    private final AtomicInteger waitingRequestsCounter = new AtomicInteger();

    private boolean shutdown = false;

    /**
     * Create a {@link GempakSubprocessStrategy}, initializing the given number
     * of subprocesses for performing GEMPAK processing.
     *
     * @param subprocessLimit
     *            the maximum number of subprocesses that can run concurrently
     * @throws GempakConnectionException
     *             if an error occurs initializing the subprocesses
     */
    public GempakSubprocessStrategy(int subprocessLimit)
            throws GempakConnectionException {
        long t0 = System.currentTimeMillis();
        /*
         * The limit should have already been validated, we just do it here to
         * make sure
         */
        subprocessLimit = GempakProcessingUtil
                .validateSubprocessLimit(subprocessLimit);

        // Start up the subprocess spawners
        Set<FutureTask<GempakSubprocessSpawner>> spawnerInits = new HashSet<>(
                subprocessLimit, 1f);
        for (int i = 0; i < subprocessLimit; ++i) {
            FutureTask<GempakSubprocessSpawner> spawnerInit = new FutureTask<>(
                    new Callable<GempakSubprocessSpawner>() {

                        @Override
                        public GempakSubprocessSpawner call()
                                throws GempakConnectionException {
                            return new GempakSubprocessSpawner();
                        }
                    });

            spawnerInits.add(spawnerInit);
            new Thread(spawnerInit).start();
        }

        // Populate the blocking queue as the spawners complete initialization
        this.subprocessLimit = new AtomicInteger(subprocessLimit);
        idleSubprocessSpawners = new ArrayBlockingQueue<>(subprocessLimit);
        try {
            for (FutureTask<GempakSubprocessSpawner> spawnerInit : spawnerInits) {
                idleSubprocessSpawners.add(spawnerInit.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new GempakConnectionException(
                    "Error initializing GEMPAK subprocesses", e);
        }
        long t1 = System.currentTimeMillis();
        perfLog.logDuration(
                "Initializing GEMPAK subprocess strategy with limit of "
                        + subprocessLimit,
                t1 - t0);
    }

    @Override
    public GempakDataRecord getDataRecord(GempakDataInput dataInput)
            throws GempakException {
        shutdownLock.readLock().lock();
        try {
            if (shutdown) {
                // This is intended to never happen
                throw new GempakShutdownException(
                        "Attempted to perform GEMPAK data processing using shutdown strategy");
            }

            statusHandler
                    .debug("Queueing GEMPAK processing request to run in a subprocess on thread "
                            + Thread.currentThread().getName()
                            + " (approximately " + waitingRequestsCounter.get()
                            + " requests are currently waiting to run)");

            GempakSubprocessSpawner spawner;
            try {
                waitingRequestsCounter.incrementAndGet();
                /*
                 * Block until we get an available spawner or there are none
                 * left
                 */
                do {
                    spawner = idleSubprocessSpawners.poll(
                            SPAWNER_POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
                } while (spawner == null && subprocessLimit.get() > 0);
                waitingRequestsCounter.decrementAndGet();
            } catch (InterruptedException e) {
                throw new GempakException(
                        "Thread interrupted while waiting for other "
                                + "subprocesses to complete",
                        e);
            }

            if (spawner == null) {
                /*
                 * All subprocesses have failed and could not be replaced,
                 * GEMPAK processing should be restarting right now and the data
                 * should need to be reloaded, so just return null
                 */
                return null;
            }

            /*
             * Got an available spawner, use it to process the data request,
             * making sure to add it back into the queue upon completion (or a
             * replacement)
             */
            boolean isValidSpawner = true;
            GempakDataRecord rval = null;
            try {
                rval = spawner.getDataRecord(dataInput);
            } catch (GempakConnectionException | GempakCommunicationException
                    | GempakShutdownException e) {
                /*
                 * Catch errors indicating a likely invalid subprocess in order
                 * to replace it, let others propagate
                 */
                isValidSpawner = false;
                statusHandler.error(
                        "Error processing data through GEMPAK, the subprocess "
                                + "that was used will be replaced as it is likely invalid",
                        e);
                spawner.shutdown();
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            GempakSubprocessSpawner spawner = new GempakSubprocessSpawner();
                            idleSubprocessSpawners.add(spawner);
                        } catch (GempakConnectionException e) {
                            int newLimit = subprocessLimit.decrementAndGet();
                            if (newLimit <= 0) {
                                statusHandler
                                        .error("All current GEMPAK subprocesses have failed, so "
                                                + "GEMPAK processing will be restarted. "
                                                + "The current data likely needs to be reloaded.");
                                GempakProcessingManager.getInstance().restart();
                            } else {
                                statusHandler
                                        .error("Error initializing GEMPAK subprocess to replace invalid one, "
                                                + "the number of subprocesses has decreased to "
                                                + newLimit, e);
                            }
                        }
                    }
                }).start();
            } finally {
                // Add the spawner back if it isn't being replaced
                if (isValidSpawner) {
                    idleSubprocessSpawners.add(spawner);
                }
            }

            return rval;
        } finally {
            shutdownLock.readLock().unlock();
        }
    }

    @Override
    public void shutdown() {
        shutdownLock.writeLock().lock();
        try {
            if (shutdown) {
                return;
            }

            statusHandler.info(
                    "Shutting down GEMPAK processing within subprocesses");
            shutdown = true;

            // Shutdown the subprocess spawners concurrently
            Set<Thread> threads = new HashSet<>(idleSubprocessSpawners.size(),
                    1f);
            for (GempakSubprocessSpawner spawner : idleSubprocessSpawners) {
                Thread thread = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        spawner.shutdown();
                    }
                });
                thread.start();
            }
            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    statusHandler.warn(
                            "Thread interrupted while waiting for subprocess spawner to cleanly shutdown",
                            e);
                }
            }
        } finally {
            shutdownLock.writeLock().unlock();
        }
    }
}
