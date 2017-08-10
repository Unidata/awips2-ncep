package gov.noaa.nws.ncep.edex.plugin.atcf.handler;

import gov.noaa.nws.ncep.common.dataplugin.atcf.AtcfRecord;
import gov.noaa.nws.ncep.common.dataplugin.atcf.request.RetrieveAtcfDeckRequest;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.raytheon.uf.common.dataquery.db.QueryParam;
import com.raytheon.uf.common.serialization.comm.IRequestHandler;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.edex.database.plugin.PluginDao;
import com.raytheon.uf.edex.database.plugin.PluginFactory;
import com.raytheon.uf.edex.database.query.DatabaseQuery;

/**
 * 
 * Handler for RetrieveAtcfRequest. Retrieves the ATCF records for the given
 * deck and storm ID (deck file name)
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 22, 2013            sgilbert    Initial creation [as RetrieveActivityHandler]
 * 10/15/2016   R22939     bhebbard    Adapted to RetrieveAtcfDeckHandler
 * 
 * </pre>
 * 
 * @author bhebbard
 * @version 1.0
 */
public class RetrieveAtcfDeckHandler implements
        IRequestHandler<RetrieveAtcfDeckRequest> {

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(RetrieveAtcfDeckHandler.class);

    private final static String ATCF_PLUGIN_NAME = "atcf";

    private final static String EQUALS = "=";
    
    private final static String B_DECK_ID = "b";  // prefix letter identifying a B-deck file name
    
    private final static String BEST_TRACK_TECHNIQUE_ID = "BEST";

    @Override
    public String[] handleRequest(RetrieveAtcfDeckRequest request)
            throws Exception {

    	statusHandler.info("RetrieveAtcfDeckRequest for " + request.getDeckID());

        PluginDao atcfDao = PluginFactory.getInstance().getPluginDao(
                ATCF_PLUGIN_NAME);

        String[] response = null;  // the deck strings to retrieve and return 
        
        // example deck ID string to parse:  "bal142016.dat"
        final String ATCF_DECK_ID_PATTERN = "(A|B|E|F)(WP|IO|SH|CP|EP|AL)(\\d{2})(\\d{4})(.DAT)";
        final Pattern pattern = Pattern.compile(ATCF_DECK_ID_PATTERN);
        Matcher matcher = pattern.matcher(request.getDeckID().toUpperCase());

        if (matcher.find()) {

            String requestedDeckType = matcher.group(1);
            String requestedBasin = matcher.group(2);
            String requestedCycloneNumber = matcher.group(3);
            String requestedSeasonYear = matcher.group(4);

            /*
             * Create query and set 4 query constraints
             */
            DatabaseQuery query = new DatabaseQuery(AtcfRecord.class);

            // Constraint 1:  B-deck gets all (and only) records with 
            // 'technique' of BEST track; A-deck gets all others
            if (requestedDeckType.equalsIgnoreCase(B_DECK_ID)) {
                query.addQueryParam("technique", BEST_TRACK_TECHNIQUE_ID, "=");
            } else {
                query.addQueryParam("technique", BEST_TRACK_TECHNIQUE_ID, "!=");
            }

            // Constraint 2:   Storm basin (AL, EP, etc.) must match exactly
            query.addQueryParam("basin", requestedBasin, EQUALS);

            // Constraint 3:  Cyclone number must match exactly
            int requestedCycloneNumberInteger = Integer
                    .parseInt(requestedCycloneNumber);
            query.addQueryParam("cycloneNum", requestedCycloneNumberInteger,
                    EQUALS);

            // Constraint 4:  Season/year of storm much match exactly.
            // NOTE, however...
            // The season/year value of the record isn't currently stored
            // separately in DB,since it doesn't appear in the ATCF record.  
            // Instead, we use the  year of the observation time, and filter 
            // on that.  However, this  could cause problems in (rare) cases 
            // of tropical storms lasting across the new year change.  Need to
            // check with NHC on how this should be handled.  May want to 
            // consider using year from the deck file name on import, and 
            // storing this as a field of each record in the DB.
            int requestedSeasonYearInteger = Integer
                    .parseInt(requestedSeasonYear);
            Calendar cal = Calendar.getInstance();
            cal.set(requestedSeasonYearInteger, 0, 1, 0, 0, 0);
            query.addQueryParam("dataTime.refTime", cal.getTime(),
                    QueryParam.QueryOperand.GREATERTHANEQUALS);
            cal.add(Calendar.YEAR, +1);
            query.addQueryParam("dataTime.refTime", cal.getTime(),
                    QueryParam.QueryOperand.LESSTHAN);

            /* Execute the query */
            List<?> dbList = atcfDao.queryByCriteria(query);

            List<String> stringsToReturn = new ArrayList<>();

            for (Object object : dbList) {
                if (object instanceof AtcfRecord) {
                    AtcfRecord atcfRecord = (AtcfRecord) object;
                    stringsToReturn.add(atcfRecord.toDeckString());
                }
            }

            response = stringsToReturn.toArray(new String[0]);
        }

        return response;
    }
}
