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
package com.raytheon.uf.viz.ncep.grid;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.raytheon.uf.common.time.util.TimeUtil;

/**
 *
 * Soft Reference based cache to store dataURIs and grid data used by NCP
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * May 25, 2012           xguo      Initial creation
 * Apr 26, 2013           B. Yin    Fixed the unit bug
 * Oct 24, 2018  54476    tjensen   Change caching to be a singleton that uses
 *                                  SoftReferences to manage data
 *
 * </pre>
 *
 * @author xguo
 *
 */
public class NcgridDataCache {

    /** The cache instance */
    private static NcgridDataCache instance;

    /* key is the requested constraint */
    private final Map<String, Reference<String>> dataURICache = new ConcurrentHashMap<>();

    private final Map<String, Reference<NcgridData>> gridCache = new ConcurrentHashMap<>();

    private long lastPruned;

    /*
     * Grid data structure
     */
    public static class NcgridData {
        int nx;

        int ny;

        float[] data;

        public float[] getData() {
            return data;
        }

        public void setData(float[] d) {
            data = Arrays.copyOf(d, d.length);
        }

        public int getNx() {
            return nx;
        }

        public void setNx(int x) {
            nx = x;
        }

        public int getNy() {
            return ny;
        }

        public void setNy(int y) {
            ny = y;
        }
    }

    private NcgridDataCache() {
        lastPruned = System.currentTimeMillis();
    }

    public static NcgridDataCache getInstance() {
        if (instance == null) {
            synchronized (NcgridDataCache.class) {
                if (instance == null) {
                    instance = new NcgridDataCache();
                }
            }
        }
        return instance;
    }

    /**
     * Get Data URI
     *
     * @param key
     *            the requested constraints
     * @return DataURI for the requested constraints
     */
    public String getDataURI(String key) {
        Reference<String> ref = dataURICache.get(key);
        return (ref != null) ? ref.get() : null;
    }

    /**
     * Add Data URI to the cache
     *
     * @param key
     *            the requested constraints
     * @param dataUri
     *            DataURI for the requested constraints
     */
    public void addDataURI(String key, String dataUri) {
        dataURICache.put(key, new SoftReference<>(dataUri));
    }

    /**
     * Parses the requested constraints for a given dataURI for the gempak
     * parameter
     *
     * @param dataUriStr
     *            DataURI
     * @return
     */
    public String getGempakParam(String dataUriStr) {
        String parm = null;

        for (Entry<String, Reference<String>> e : dataURICache.entrySet()) {
            Reference<String> ref = e.getValue();
            if (ref != null) {
                String dataURI = ref.get();
                if (dataURI != null && dataURI.equals(dataUriStr)) {
                    String[] parmList = e.getKey().split("\\|");
                    parm = parmList[2];
                    break;
                }
            }
        }
        return parm;
    }

    /**
     * Get grid data
     *
     * @param datauri
     *            URI of the data
     * @return requested grid data
     */
    public NcgridData getGridData(String datauri) {
        Reference<NcgridData> ref = gridCache.get(datauri);
        return (ref != null) ? ref.get() : null;
    }

    /**
     * Creates an NcgridData object and adds it to the cache
     *
     * @param dataUri
     * @param nx
     * @param ny
     * @param data
     */
    public void addGridData(String dataUri, int nx, int ny, float[] data) {
        NcgridData nd = new NcgridData();

        nd.setNx(nx);
        nd.setNy(ny);
        nd.setData(data);
        gridCache.put(dataUri, new SoftReference<>(nd));
    }

    /**
     * Check maps for null references to remove unneeded keys. Skip this check
     * if it's been done in the last minute to avoid redundant looping. Check
     * time once outside the sync block to prevent unneeded syncing when things
     * have recently been prune. Check again inside the sync to see if another
     * thread pruned while you were waiting for a lock.
     */
    public void prune() {
        if (System.currentTimeMillis()
                - lastPruned > TimeUtil.MILLIS_PER_MINUTE) {
            synchronized (this) {
                if (System.currentTimeMillis()
                        - lastPruned > TimeUtil.MILLIS_PER_MINUTE) {
                    dataURICache.values()
                            .removeIf(v -> (v != null && v.get() == null));
                    gridCache.values()
                            .removeIf(v -> (v != null && v.get() == null));
                    lastPruned = System.currentTimeMillis();
                }
            }
        }
    }
}
