/**
 * gov.noaa.nws.ncep.viz.rsc.warn.rsc.WcnCountyQueryResult
 * 
 * Date created September 28, 2011
 *
 *  This code is developed by the SIB for use in the AWIPS2 system. 
 */

package gov.noaa.nws.ncep.viz.rsc.aww.query;

import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource.IRscDataObject;
import gov.noaa.nws.ncep.viz.rsc.aww.wcn.WcnResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.raytheon.uf.viz.core.catalog.DirectDbQuery;
import com.raytheon.uf.viz.core.catalog.DirectDbQuery.QueryLanguage;

/**
 * Class for loading and storing county query result for WCN.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#  Engineer       Description
 * ------------ ------- -----------     ---------------------------------------
 * 09/28/11     #456    G. Zhang        Initial Creation        
 * 08/15/13     #1028   G. Hull         don't add duplicate fips to the query
 * 03/15/2016   R15560  K. Bugenhagen   Cleanup and refactoring.
 * 
 * </pre>
 * 
 * @author gzhang
 * @version 1.0
 */

public class WcnCountyQueryResult {

    private static Logger logger = Logger.getLogger(WcnCountyQueryResult.class
            .getCanonicalName());

    private StringBuilder queryCounty = new StringBuilder();

    private StringBuilder queryMarineZone = new StringBuilder();

    private Map<String, ArrayList<ArrayList<Object[]>>> fipsMultiResultMap = new HashMap<String, ArrayList<ArrayList<Object[]>>>();

    private List<String> fipsList = new ArrayList<String>();

    public WcnCountyQueryResult() {

    }

    /**
     * Build query using fips
     */
    public void buildQueryPart(IRscDataObject dataObject) {
        WcnResource.WcnRscDataObj wData = (WcnResource.WcnRscDataObj) dataObject;

        if (wData == null || wData.countyFips == null) {
            return;
        }
        for (String fips : wData.countyFips) {
            if (!fipsList.contains(fips)) {
                fipsList.add(fips);
                if (wData.isCounty) {
                    queryCounty.append(" ( fips ='");
                    queryCounty.append(fips);
                    queryCounty.append("' ) OR  ");
                } else {
                    queryMarineZone.append(" ( id ='").append(fips)
                            .append("' ) OR  ");
                }
            }
        }
    }

    /**
     * store query result into a map.
     */
    public void populateMap(String queryPrefix) {

        List<Object[]> results = null;
        StringBuilder query = new StringBuilder();

        String wholeQuery = "";
        try {
            if (queryPrefix.equals(WcnResource.QUERY_PREFIX_COUNTY)) {
                query = queryCounty;
            } else if (queryPrefix.equals(WcnResource.QUERY_PREFIX_MARINE_ZONE)) {
                query = queryMarineZone;
            }
            if (query.length() > 0) {
                wholeQuery = queryPrefix + AwwQueryResult.GEO_CONSTRAINT
                        + " AND ("
                        + query.substring(0, query.lastIndexOf("OR")) + " );";
            }
            if (!wholeQuery.isEmpty()) {
                results = DirectDbQuery.executeQuery(wholeQuery, "maps",
                        QueryLanguage.SQL);
            }
        } catch (Exception e) {
            logger.log(
                    Level.SEVERE,
                    "_____ Exception in query string or result: "
                            + e.getMessage() + ": query = " + wholeQuery);
            return;
        }

        if (results != null) {
            for (Object[] o : results) {
                if (o == null || o.length != 5 || o[2] == null || o[3] == null
                        || o[4] == null) {
                    logger.log(Level.WARNING,
                            "DirectDBQuery results had nulls or wrong size");
                    continue;
                }

                ArrayList<Object[]> obs = new ArrayList<Object[]>();
                obs.add(new Object[] { o[0], o[1] });

                String key = (String) o[4];

                if (fipsMultiResultMap.containsKey(key)) {
                    fipsMultiResultMap.get(key).add(obs);
                } else {
                    ArrayList<ArrayList<Object[]>> list = new ArrayList<ArrayList<Object[]>>();
                    list.add(obs);
                    fipsMultiResultMap.put(key, list);
                }

            }
        }
    }

    /**
     * 2011-09-28: Loiza county in Puerto Rico with fips 72087 has NO record in
     * Raytheon's database: maps mapdata.county table and we need to handle
     * cases like that.
     * 
     * TODO: move this handling to the query place?
     */
    public ArrayList<ArrayList<Object[]>> getStateCountyResult(String fips) {

        ArrayList<ArrayList<Object[]>> list = fipsMultiResultMap.get(fips);

        if (list == null) {
            logger.log(Level.FINEST, "_______ No result for fips: " + fips);

            return new ArrayList<ArrayList<Object[]>>();
        }

        return list;
    }
}
