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
package com.raytheon.uf.edex.nsbn;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.camel.spi.IdempotentRepository;
import org.apache.camel.support.service.ServiceSupport;

import com.raytheon.uf.common.time.util.TimeUtil;

/**
 *
 * An idempotent registry that tracks all files found by the NSBN File Transfer
 * in the last 30 minutes
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 10, 2018 56039      tjensen     Initial creation
 * Mar  3, 2021 8326       tgurney     Camel 3 fixes
 *
 * </pre>
 *
 * @author tjensen
 */
public class NSBNIdempotentRepository extends ServiceSupport
        implements IdempotentRepository {

    private final Map<String, Long> cache = new ConcurrentHashMap<>();

    private long lastPruned;

    @Override
    public boolean add(String key) {
        synchronized (cache) {
            if (cache.containsKey(key)) {
                return false;
            }
            cache.put(key, System.currentTimeMillis());
        }
        pruneCache();

        return true;
    }

    @Override
    public boolean contains(String key) {
        synchronized (cache) {
            return cache.containsKey(key);
        }
    }

    @Override
    public boolean remove(String key) {
        boolean answer;
        synchronized (cache) {
            answer = cache.remove(key) != null;
            pruneCache();
        }
        return answer;
    }

    @Override
    public boolean confirm(String key) {
        // noop
        return true;
    }

    @Override
    public void clear() {
        synchronized (cache) {
            cache.clear();
        }
    }

    @Override
    protected void doStart() throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    protected void doStop() throws Exception {
        clear();
    }

    public void pruneCache() {
        if (System.currentTimeMillis()
                - lastPruned > TimeUtil.MILLIS_PER_MINUTE) {
            synchronized (this) {
                long pruneTime = System.currentTimeMillis();
                if (pruneTime - lastPruned > TimeUtil.MILLIS_PER_MINUTE) {
                    cache.values().removeIf(v -> (v != null && v < (pruneTime
                            - TimeUtil.MILLIS_PER_MINUTE * 30)));
                    lastPruned = pruneTime;
                }
            }
        }
    }

}
