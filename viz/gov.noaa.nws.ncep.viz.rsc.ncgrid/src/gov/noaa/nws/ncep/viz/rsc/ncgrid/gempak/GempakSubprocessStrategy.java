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
package gov.noaa.nws.ncep.viz.rsc.ncgrid.gempak;

import java.util.concurrent.Semaphore;

import com.raytheon.uf.common.status.IPerformanceStatusHandler;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.PerformanceStatus;
import com.raytheon.uf.common.status.UFStatus;

/**
 * GEMPAK processing strategy that spawns subprocesses to perform concurrent
 * data processing. Subprocesses are created on an as-needed basis, with a 1:1
 * ratio between subprocesses and data processing requests.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 04, 2018 7417       mapeters    Initial creation
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

    /**
     * Semaphore for enforcing the subprocess limit.
     */
    private final Semaphore subprocessLimitSemaphore;

    /**
     * Create a {@link GempakSubprocessStrategy} for performing GEMPAK
     * processing in subprocesses.
     *
     * @param subprocessLimit
     *            the maximum number of subprocesses that can run concurrently
     *            (must be between
     *            {@value GempakProcessingConstants#MIN_SUBPROCESS_LIMIT} and
     *            {@value GempakProcessingConstants#MAX_SUBPROCESS_LIMIT})
     */
    public GempakSubprocessStrategy(int subprocessLimit) {
        subprocessLimit = validateSubprocessLimit(subprocessLimit);
        subprocessLimitSemaphore = new Semaphore(subprocessLimit);
    }

    @Override
    public GempakDataRecord getDataRecord(GempakDataInput dataInput) {
        perfLog.log("Approximately " + subprocessLimitSemaphore.getQueueLength()
                + " GEMPAK processing requests are waiting to run");
        try {
            // Block for other subprocesses to complete (if necessary)
            subprocessLimitSemaphore.acquire();
        } catch (InterruptedException e) {
            String msg = "Thread interrupted while waiting for other "
                    + "subprocesses to complete";
            statusHandler.error(msg, e);
            return null;
        }

        /*
         * We acquired a permit to run, perform the processing and ensure that
         * we release our permit
         */
        try {
            GempakSubprocessSpawner proc = new GempakSubprocessSpawner(dataInput);
            return proc.getDataRecord();
        } finally {
            subprocessLimitSemaphore.release();
        }
    }

    private int validateSubprocessLimit(int subprocessLimit) {
        int minLimit = GempakProcessingConstants.MIN_SUBPROCESS_LIMIT;
        int maxLimit = GempakProcessingConstants.MAX_SUBPROCESS_LIMIT;
        if (subprocessLimit < minLimit) {
            statusHandler.warn("Invalid subprocess limit (" + subprocessLimit
                    + "), setting to minimum accepted value (" + minLimit
                    + ")");
            subprocessLimit = minLimit;
        } else if (subprocessLimit > maxLimit) {
            statusHandler.warn("Invalid subprocess limit (" + subprocessLimit
                    + "), setting to maximum accepted value (" + maxLimit
                    + ")");
            subprocessLimit = maxLimit;
        }
        return subprocessLimit;
    }
}
