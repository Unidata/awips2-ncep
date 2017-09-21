/**
 * gov.noaa.nws.ncep.viz.rsc.ffa.rsc.FfaZoneQueryResult
 * 
 * Date created October 04, 2011
 *
 * This code is developed by the SIB for use in the AWIPS2 system. 
 */

package gov.noaa.nws.ncep.viz.rsc.aww.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.raytheon.uf.viz.core.catalog.DirectDbQuery;
import com.raytheon.uf.viz.core.catalog.DirectDbQuery.QueryLanguage;

/**
 * Handles database queries for the AWW resources.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer         Description
 * ------------ ---------- -------------    -----------------
 * 03/15/2016   R15560     K. Bugenhagen    Initial creation.
 * 
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1.0
 */
public class AwwQueryResult {

    private static final double ENV_MIN_X = -180.0;

    private static final double ENV_MAX_X = 180.0;

    private static final double ENV_MIN_Y = -90;

    private static final double ENV_MAX_Y = 90.0;

    private static Logger logger = Logger.getLogger(AwwQueryResult.class
            .getCanonicalName());

    public static final String GEO_CONSTRAINT_PREFIX = "the_geom_0_001 && ST_SetSrid('BOX3D(%f %f, %f %f)'::box3d,4326)";

    public static final String GEO_CONSTRAINT = String.format(
            GEO_CONSTRAINT_PREFIX, ENV_MIN_X, ENV_MIN_Y, ENV_MAX_X, ENV_MAX_Y);

    private StringBuilder query = new StringBuilder();

    private Map<String, ArrayList<ArrayList<Object[]>>> fipsMultiResultMap = new HashMap<String, ArrayList<ArrayList<Object[]>>>();

    public AwwQueryResult() {

    }

    public void buildQueryPart(List<String> countyFips, String columnName) {
        String fips;
        for (int i = 0; i < countyFips.size(); i++) {
            fips = countyFips.get(i);
            query.append(" ( ");
            query.append(columnName);
            query.append(" ='");
            query.append(fips);
            query.append("' ) OR  ");
        }
    }

    public void populateMap(String queryPrefix) {

        List<Object[]> results = null;

        try {
            if (query.length() > 0) {
                String wholeQuery = queryPrefix + GEO_CONSTRAINT + " AND ("
                        + query.substring(0, query.lastIndexOf("OR")) + " );";
                results = DirectDbQuery.executeQuery(wholeQuery, "maps",
                        QueryLanguage.SQL);
            }
        } catch (Exception e) {
            logger.log(
                    Level.SEVERE,
                    "_____ Exception with query string or result: "
                            + e.getMessage());
            return;
        }

        if (results == null)
            return;

        for (Object[] o : results) {
            if (o == null || o.length != 5 || o[2] == null || o[3] == null
                    || o[4] == null)
                continue;

            ArrayList<Object[]> obs = new ArrayList<Object[]>();
            obs.add(new Object[] { o[0], o[1] });

            String key = (String) o[4];

            if (fipsMultiResultMap.containsKey(key))

                fipsMultiResultMap.get(key).add(obs);
            else {
                ArrayList<ArrayList<Object[]>> list = new ArrayList<ArrayList<Object[]>>();
                list.add(obs);
                fipsMultiResultMap.put(key, list);
            }
        }
    }

    public ArrayList<ArrayList<Object[]>> getCountyResult(String fips) {

        ArrayList<ArrayList<Object[]>> list = fipsMultiResultMap.get(fips);

        if (list == null) {
            logger.info("No County result, empty list returned!");
            return new ArrayList<ArrayList<Object[]>>();
        }

        return list;
    }

    public ArrayList<ArrayList<Object[]>> getZoneResult(String fips) {

        ArrayList<ArrayList<Object[]>> list = fipsMultiResultMap.get(fips);

        if (list == null) {
            logger.log(Level.WARNING, "_______ No result for fips: " + fips);
            return new ArrayList<ArrayList<Object[]>>();
        }

        return list;
    }

}
