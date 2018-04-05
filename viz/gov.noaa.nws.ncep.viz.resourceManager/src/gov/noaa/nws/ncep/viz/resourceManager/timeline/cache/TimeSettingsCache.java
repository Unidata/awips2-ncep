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
package gov.noaa.nws.ncep.viz.resourceManager.timeline.cache;

import gov.noaa.nws.ncep.viz.resources.time_match.NCTimeMatcherSettings;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

/**
 * Manages {@link NCTimeMatcherSettings} cached by
 * {@link TimeSettingsResourceKey}.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 02/11/2016   R15244     bkowal      Initial creation
 * 
 * </pre>
 * 
 * @author bkowal
 * @version 1.0
 */

public class TimeSettingsCache {

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(getClass());

    private final ConcurrentMap<TimeSettingsResourceKey, NCTimeMatcherSettings> resourceTimeSettingsMap = new ConcurrentHashMap<>();

    public TimeSettingsCache() {
    }

    /**
     * Caches the specified {@link NCTimeMatcherSettings} and associates them
     * with the specified {@link TimeSettingsResourceKey}. The specified
     * NCTimeMatcherSettings will only consist of the fields that have actually
     * been updated that require caching. This information will either be merged
     * into an existing NCTimeMatcherSettings associated with the specified
     * TimeSettingsResourceKey or it will be stored as a completely new cached
     * object.
     * 
     * @param settings
     *            the specified {@link NCTimeMatcherSettings}
     * @param lookupKey
     *            the specified {@link TimeSettingsResourceKey}
     * @param cacheKey
     *            RDB identifier that this cache is associated with. Only used
     *            for logging purposes.
     */
    public void cacheSettings(NCTimeMatcherSettings settings,
            final TimeSettingsResourceKey lookupKey, final String cacheKey) {
        NCTimeMatcherSettings existing = this.resourceTimeSettingsMap
                .get(lookupKey);
        if (existing == null) {
            this.resourceTimeSettingsMap.put(lookupKey, settings);
        } else {
            existing.update(settings);
            // just for logging purposes.
            settings = existing;
        }
        statusHandler.info("Cached: " + settings.toString()
                + " for lookup key: " + lookupKey.toString() + "; cache key: "
                + cacheKey + ".");
    }

    /**
     * Retrieves any cached settings associated with the specified
     * {@link TimeSettingsResourceKey}.
     * 
     * @param lookupKey
     *            the specified {@link TimeSettingsResourceKey} to retrieve
     *            settings for
     * @return any cached {@link NCTimeMatcherSettings} that may exist or null
     *         if none exist.
     */
    public NCTimeMatcherSettings retrieveSettings(
            final TimeSettingsResourceKey lookupKey) {
        return this.resourceTimeSettingsMap.get(lookupKey);
    }
}
