/**
 * gov.noaa.nws.ncep.viz.rsc.ffa.rsc.WstmQueryResult
 *
 * Date created October 03, 2011
 *
 * This code is developed by the SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.viz.rsc.aww.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.edex.decodertools.core.LatLonPoint;
import com.raytheon.uf.viz.core.catalog.DirectDbQuery;
import com.raytheon.uf.viz.core.catalog.DirectDbQuery.QueryLanguage;

import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource.IRscDataObject;
import gov.noaa.nws.ncep.viz.rsc.aww.utils.PreProcessDisplay;

/**
 * This class handles database query part for the WstmResource.
 *
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer       Description
 * ------------- -------- -------------- ---------------------------------------
 * Jul 10, 2270  456      G. Zhang       Initial creation.
 * Jul 12, 2188  581      B. Hebbard     Fix attempted cast of BigDecimal to
 *                                       Double (for LatLon) (TTR#319)
 * Sep 01, 2198  976      Archana        Updated the queryPrefix string to
 *                                       include the 'name' field. Updated
 *                                       populateFipsMap() to use the 'name'
 *                                       field if the 'shortname' field is null
 * Mar 15, 2016  15560    K. Bugenhagen  Refactoring and cleanup.
 * Jul 15, 2020  8191     randerso       Updated for changes to LatLonPoint
 *
 * </pre>
 *
 * @author gzhang
 */

public class WstmQueryResult {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(WstmQueryResult.class);

    private List<String> fipsConstraints = new ArrayList();

    private String queryPrefix = "select ST_AsBinary(the_geom) G, ST_AsBinary(the_geom_0_001) G1, lat,lon,state_zone, shortname, name from mapdata.zonelowres where ";

    private Map<String, String> fipsNameMap = new HashMap<>();

    private Map<String, LatLonPoint> fipsLatLonMap = new HashMap<>();

    private Map<String, List<List<Object[]>>> fipsMultiResultMap = new HashMap<>();

    public WstmQueryResult() {

    }

    /**
     * build query part with fips
     *
     * @param aSetOfAwwFips
     */
    public void buildQueryPart(IRscDataObject dataObject) {
        PreProcessDisplay wData = (PreProcessDisplay) dataObject;

        if (wData.fipsCodesList != null) {
            for (String afips : wData.fipsCodesList) {
                StringBuilder sb = new StringBuilder();
                sb.append(" ( state_zone = '");
                // taking off 'Z' as in 'PAZ008'
                sb.append(afips.substring(0, 2)).append(afips.substring(3));
                sb.append("' )");
                fipsConstraints.add(sb.toString());
            }
        }
    }

    /**
     * query the database then fill the map.
     */
    public void populateFipsMap() {

        if (fipsConstraints.isEmpty()) {
            return;
        }

        List<Object[]> results = null;

        try {
            StringBuilder wholeQuery = new StringBuilder(queryPrefix);
            wholeQuery.append(AwwQueryResult.GEO_CONSTRAINT);
            wholeQuery.append(" AND (");
            wholeQuery.append(String.join(" OR ", fipsConstraints));
            wholeQuery.append(" );");

            results = DirectDbQuery.executeQuery(wholeQuery.toString(), "maps",
                    QueryLanguage.SQL);
        } catch (Exception e) {
            statusHandler.error(
                    "Exception with query string or result: " + e.getMessage(),
                    e);
            return;
        }

        /*
         * loop through query result
         */
        for (Object[] o : results) {

            if (o == null || o.length != 7 || o[2] == null || o[3] == null
                    || o[4] == null) {
                continue;
            }
            if (o[5] == null) {
                if (o[6] == null) {
                    /*
                     * continue only if both the shortname as well as the name
                     * column is null
                     */
                    continue;
                }
            }

            // geometry
            List<Object[]> obs = new ArrayList<>();
            obs.add(new Object[] { o[0], o[1] });

            // state_zone
            String fips = (String) o[4];

            if (fips == null || fips.length() != 5) {
                continue;
            }

            // zone name
            String name;
            if (o[5] != null) {
                name = (String) o[5];
            } else {
                name = (String) o[6];
            }

            // put 'Z' back as in 'PAZ008'
            String key = fips.substring(0, 2) + "Z" + fips.substring(2);

            // put into fipsMultiResultMap
            if (fipsMultiResultMap.containsKey(key)) {
                fipsMultiResultMap.get(key).add(obs);
            } else {
                List<List<Object[]>> list = new ArrayList<>();
                list.add(obs);
                fipsMultiResultMap.put(key, list);
            }

            LatLonPoint value = new LatLonPoint(((Number) o[2]).doubleValue(),
                    ((Number) o[3]).doubleValue());
            fipsLatLonMap.put(key, value);
            fipsNameMap.put(key, name);
        }
    }

    public LatLonPoint getLatLonPoint(String fips) {

        return this.fipsLatLonMap.get(fips);

    }

    public List<List<Object[]>> getZoneResult(String fips) {

        List<List<Object[]>> list = fipsMultiResultMap.get(fips);

        if (list == null) {
            statusHandler.warn("No result for fips: " + fips);

            return new ArrayList<>();
        }

        return list;

    }

    public String getZoneName(String fips) {
        String n = fipsNameMap.get(fips);

        return (n == null) ? "" : n;
    }

}
