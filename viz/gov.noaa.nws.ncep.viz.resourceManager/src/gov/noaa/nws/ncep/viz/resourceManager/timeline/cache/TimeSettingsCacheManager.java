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

import gov.noaa.nws.ncep.viz.resources.manager.ResourceName;
import gov.noaa.nws.ncep.viz.resources.time_match.NCTimeMatcherSettings;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Used to store and retrieve cached timeline settings associated with a
 * resource. Will cache settings for as long as CAVE remains open. This will
 * allow a user to completely close the RDB dialog and when it is re-opened, any
 * previously cached timeline settings will still be available.
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

public class TimeSettingsCacheManager {

    private static final TimeSettingsCacheManager INSTANCE = new TimeSettingsCacheManager();

    private String currentKey;

    private final ConcurrentMap<String, TimeSettingsCache> timeSettingsCacheMap = new ConcurrentHashMap<>();

    public static TimeSettingsCacheManager getInstance() {
        return INSTANCE;
    }

    protected TimeSettingsCacheManager() {
    }

    /**
     * Retrieves any cached settings for the specified {@link ResourceName}.
     * 
     * @param resourceName
     *            the specified {@link ResourceName}
     * @return the associated {@link NCTimeMatcherSettings} if they exist; null,
     *         otherwise.
     */
    public NCTimeMatcherSettings getCachedSettings(
            final ResourceName resourceName) {

        final TimeSettingsResourceKey lookupKey = new TimeSettingsResourceKey(
                resourceName);
        TimeSettingsCache currentCache = this.timeSettingsCacheMap
                .get(this.currentKey);
        if (currentCache == null) {
            return null;
        }

        return currentCache.retrieveSettings(lookupKey);
    }

    /**
     * Caches the specified {@link NCTimeMatcherSettings} and associates them
     * with the specified {@link ResourceName}.
     * 
     * @param resourceName
     *            the specified {@link ResourceName}
     * @param settings
     *            the {@link NCTimeMatcherSettings} to cache
     */
    public void cacheSettings(final ResourceName resourceName,
            final NCTimeMatcherSettings settings) {
        TimeSettingsCache currentCache = this.timeSettingsCacheMap
                .get(this.currentKey);
        if (resourceName == null) {
            /*
             * will occur during transitions as dominant resources are replaced.
             */
            return;
        }
        if (currentCache == null) {
            currentCache = new TimeSettingsCache();
            this.timeSettingsCacheMap.put(currentKey, currentCache);
        }
        final TimeSettingsResourceKey lookupKey = new TimeSettingsResourceKey(
                resourceName);
        currentCache.cacheSettings(settings, lookupKey, this.currentKey);
    }

    /**
     * Used to transition to and/or create a new {@link TimeSettingsCache}
     * associated with a newly selected RDB.
     * 
     * @param currentKey
     *            the name of the RDB to associate a cache with.
     */
    public void updateCacheLookupKey(final String currentKey) {
        this.currentKey = currentKey;
        if (this.timeSettingsCacheMap.containsKey(currentKey) == false) {
            this.timeSettingsCacheMap.put(currentKey, new TimeSettingsCache());
        }
    }

    /**
     * Removes any previously cached settings. Used when resetting the RDB
     * Manager.
     */
    public void reset() {
        this.timeSettingsCacheMap.clear();
    }
}
