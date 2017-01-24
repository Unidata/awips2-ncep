package gov.noaa.nws.ncep.viz.resources.manager;

import java.util.List;

import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.common.util.CollectionUtil;

// an entry in the availTimesCache which also may listen for
// updates from Raytheon's MenuUpdater
//
/**
 * 
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * -----------  ----------  ----------  --------------------------
 *  09/05/12    #860        Greg Hull   Created for the ResourceDefinitions time cache
 *  07/14       TTR1034+    J. Wu       Make sure the input newTime is updated as latestTime 
 *                                      in setAvailableTimes() and updateTime() to reflect 
 *                                      a newer DataTime in DB.
 * 04/05/2016   RM#10435    rjpeter     Fixed sync issues, remove tracking with uriCatalog.
 * </pre>
 * 
 * @author ghull
 * @version 1
 */

public class DataTimesCacheEntry {

    private static final long CACHE_HOLD_TIME = 60 * TimeUtil.MILLIS_PER_SECOND;

    /* may be set separately from dataTimes */
    private volatile DataTime latestTime;

    private volatile List<DataTime> dataTimes;

    /* Time cache expires */
    private volatile long cacheExpireDataTimes;

    private volatile long cacheExpireLatestTime;

    public DataTimesCacheEntry() {
        cacheExpireDataTimes = 0;
        cacheExpireLatestTime = 0;
        dataTimes = null;
        latestTime = null;
    }

    /**
     * Returns the latest time, or a Null dataTime if no data, or a null value
     * if either the cache has expired or a time has not been set yet.
     * 
     * @return
     */
    public DataTime getLatestTime() {
        DataTime rval = null;

        if (System.currentTimeMillis() < cacheExpireLatestTime) {
            rval = latestTime;
        }

        return rval;
    }

    /**
     * Return latest queried times if in cache and has not expired.
     */
    public List<DataTime> getAvailableTimes() {
        List<DataTime> rval = null;

        if (System.currentTimeMillis() < cacheExpireDataTimes) {
            rval = dataTimes;
        }

        return rval;
    }

    /**
     * Clears the cached dataTimes.
     */
    public void clearCache() {
        cacheExpireDataTimes = 0;
        cacheExpireLatestTime = 0;
        dataTimes = null;
        latestTime = null;
    }

    /**
     * Sets the available times in the cache.
     * 
     * @param dtlist
     */
    public void setAvailableTimes(List<DataTime> dataTimes) {
        this.dataTimes = dataTimes;
        cacheExpireDataTimes = System.currentTimeMillis() + CACHE_HOLD_TIME;

        if (CollectionUtil.isNullOrEmpty(dataTimes)) {
            setLatestTime(null);
            latestTime = null;
        } else {
            setLatestTime(dataTimes.get(dataTimes.size() - 1));
        }
    }

    /**
     * Sets the latest time.
     * 
     * @param latestTime
     */
    public void setLatestTime(DataTime latestTime) {
        this.latestTime = latestTime;
        cacheExpireLatestTime = System.currentTimeMillis() + CACHE_HOLD_TIME;
    }

}
