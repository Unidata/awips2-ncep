/**
 * gov.noaa.nws.ncep.viz.rsc.ffa.rsc.WstmQueryResult
 * 
 * Date created October 03, 2011
 *
 * This code is developed by the SIB for use in the AWIPS2 system. 
 */

package gov.noaa.nws.ncep.viz.rsc.aww.query;

import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource.IRscDataObject;
import gov.noaa.nws.ncep.viz.rsc.aww.utils.PreProcessDisplay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.raytheon.uf.edex.decodertools.core.LatLonPoint;
import com.raytheon.uf.viz.core.catalog.DirectDbQuery;
import com.raytheon.uf.viz.core.catalog.DirectDbQuery.QueryLanguage;

/**
 * This class handles database query part for the WstmResource.
 * 
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket# Engineer    Description
 * ------------ ------- ----------- --------------------------
 * 2011-10-03   456     G. Zhang       Initial creation.
 * 2011-12-21   581     B. Hebbard     Fix attempted cast of BigDecimal to Double (for LatLon) (TTR#319)
 * 2013-01-31   976     Archana        Updated the queryPrefix string to include the 'name' field.
 *                                     Updated populateFipsMap() to use the 'name' field if the 'shortname' field
 *                                     is null
 * 03/15/2016   R15560  K. Bugenhagen  Refactoring and cleanup.
 * </pre>
 * 
 * @author gzhang
 * @version 1.0
 */

public class WstmQueryResult {

    private static Logger logger = Logger
            .getLogger("gov.noaa.nws.ncep.viz.rsc.wstm.rsc.WstmQueryResult");

    private StringBuilder query = new StringBuilder();

    private String queryPrefix = "select ST_AsBinary(the_geom) G, ST_AsBinary(the_geom_0_001) G1, lat,lon,state_zone, shortname, name from mapdata.zonelowres where ";

    private Map<String, String> fipsNameMap = new HashMap<String, String>();

    private Map<String, LatLonPoint> fipsLatLonMap = new HashMap<String, LatLonPoint>();

    private Map<String, ArrayList<ArrayList<Object[]>>> fipsMultiResultMap = new HashMap<String, ArrayList<ArrayList<Object[]>>>();

    public WstmQueryResult() {

    }

    /**
     * build query part with fips
     * 
     * @param aSetOfAwwFips
     */
    public void buildQueryPart(IRscDataObject dataObject) {
        PreProcessDisplay wData = (PreProcessDisplay) dataObject;

        if (wData.fipsCodesList == null || wData.fipsCodesList.size() == 0)
            return;

        for (String afips : wData.fipsCodesList) {
            query.append(" ( state_zone = '");
            // taking off 'Z' as in 'PAZ008'
            query.append(afips.substring(0, 2)).append(afips.substring(3));
            query.append("' ) OR ");
        }

    }

    /**
     * query the database then fill the map.
     */
    public void populateFipsMap() {

        if (query == null || query.length() == 0)
            return;

        List<Object[]> results = null;

        try {
            String wholeQuery = queryPrefix + AwwQueryResult.GEO_CONSTRAINT
                    + " AND (" + query.substring(0, query.lastIndexOf("OR"))
                    + " );";
            results = DirectDbQuery.executeQuery(wholeQuery, "maps",
                    QueryLanguage.SQL);
        } catch (Exception e) {
            logger.log(
                    Level.SEVERE,
                    "_____ Exception with query string or result: "
                            + e.getMessage());
            return;
        }

        /*
         * loop through query result
         */
        for (Object[] o : results) {

            if (o == null || o.length != 7 || o[2] == null || o[3] == null
                    || o[4] == null)
                continue;
            if (o[5] == null) {
                if (o[6] == null) {
                    continue; // continue only if both the shortname as well as
                              // the name column is null
                }
            }

            // geometry
            ArrayList<Object[]> obs = new ArrayList<Object[]>();
            obs.add(new Object[] { o[0], o[1] });

            // state_zone
            String fips = (String) o[4];

            if (fips == null || fips.length() != 5)
                continue;

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
            if (fipsMultiResultMap.containsKey(key))

                fipsMultiResultMap.get(key).add(obs);
            else {
                ArrayList<ArrayList<Object[]>> list = new ArrayList<ArrayList<Object[]>>();
                list.add(obs);
                fipsMultiResultMap.put(key, list);
            }

            LatLonPoint value = new LatLonPoint(((Number) o[2]).doubleValue(),
                    ((Number) o[3]).doubleValue(), LatLonPoint.INDEGREES);
            fipsLatLonMap.put(key, value);
            fipsNameMap.put(key, name);
        }
    }

    public LatLonPoint getLatLonPoint(String fips) {

        return this.fipsLatLonMap.get(fips);

    }

    public ArrayList<ArrayList<Object[]>> getZoneResult(String fips) {

        ArrayList<ArrayList<Object[]>> list = fipsMultiResultMap.get(fips);

        if (list == null) {
            logger.log(Level.WARNING, "_______ No result for fips: " + fips);

            return new ArrayList<ArrayList<Object[]>>();
        }

        return list;

    }

    public String getZoneName(String fips) {
        String n = fipsNameMap.get(fips);

        return (n == null) ? "" : n;
    }

}
