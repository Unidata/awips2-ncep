package gov.noaa.nws.ncep.edex.plugin.asdi.decoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.raytheon.uf.common.dataquery.requests.DbQueryRequest;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint.ConstraintType;
import com.raytheon.uf.common.dataquery.responses.DbQueryResponse;
import com.raytheon.uf.common.serialization.comm.IRequestHandler;
import com.raytheon.uf.common.time.TimeRange;
import com.raytheon.uf.edex.database.handlers.DbQueryHandler;

import gov.noaa.nws.ncep.common.dataplugin.asdi.AsdiRecord;
import gov.noaa.nws.ncep.common.dataplugin.asdi.RetrieveAsdiRequest;

/**
 * 
 * Handler for RetrieveAsdiRequest. Retrieves ASDI records.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#  Engineer     Description
 * ------------ -------  -----------  --------------------------
 * 03/07/2017   R28579   R.Reynolds   Adapted to ASDI
 * 
 * </pre>
 * 
 * @author RReynolds
 * @version 1.0
 */
public class RetrieveAsdiHandler
        implements IRequestHandler<RetrieveAsdiRequest> {

    private static Logger logger = Logger
            .getLogger(RetrieveAsdiHandler.class.toString());

    protected static final String FIELD_PLUGIN_NAME = "pluginName";

    protected static final String FIELD_VALID_START = "dataTime.validPeriod.start";

    protected static final String FIELD_VALID_END = "dataTime.validPeriod.end";

    private final static String ASDI_DAO = "asdi";

    /**
     * Handles ASDI request.
     * 
     */

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.common.serialization.comm.IRequestHandler#handleRequest(
     * com.raytheon.uf.common.serialization.comm.IServerRequest)
     */
    @Override
    public List<AsdiRecord> handleRequest(RetrieveAsdiRequest request) {

        List<AsdiRecord> response = new ArrayList<>();

        // query for all records in time range
        DbQueryResponse queryResults = queryAsdiRecordsForTimeRange(request);

        int numResults = queryResults.getNumResults();

        List<Map<String, Object>> results = queryResults.getResults();

        for (int i = 0; i < numResults; i++) {
            Map<String, Object> result = results.get(i);
            /*
             * .get(null) is used extensively throughout AWIPS. Because Map key
             * is null then get(null) will return the value at "bucket" 0
             */

            response.add((AsdiRecord) result.get(null));
        }

        return response;
    }

    /**
     * Queries for ASDI records within a time range.
     * 
     * @return database response containing ASDI records.
     * @param request
     *            - Request for ASDI data for the given time range. This is a DB
     *            Request.
     * @return DbQueryResponse- Contains a List<Map<String,Object>> where each
     *         Map in the List is a row returned from the query and you can use
     *         the field strings from the request to get the object out of the
     *         Map
     */
    private DbQueryResponse queryAsdiRecordsForTimeRange(
            RetrieveAsdiRequest request) {

        TimeRange timeRange = new TimeRange(request.getStartTime(),
                request.getEndTime());
        DbQueryRequest dbRequest = new DbQueryRequest();
        DbQueryResponse queryResults = new DbQueryResponse();
        dbRequest.addConstraint(FIELD_PLUGIN_NAME,
                new RequestConstraint(ASDI_DAO));

        // Add the TimeRange Constraint
        if (timeRange != null) {
            RequestConstraint afterReqStart = new RequestConstraint(
                    timeRange.getStart().toString(),
                    ConstraintType.GREATER_THAN_EQUALS);
            RequestConstraint beforeReqEnd = new RequestConstraint(
                    timeRange.getEnd().toString(),
                    ConstraintType.LESS_THAN_EQUALS);
            dbRequest.addConstraint(FIELD_VALID_START, afterReqStart);
            dbRequest.addConstraint(FIELD_VALID_END, beforeReqEnd);
        }

        try {

            queryResults = new DbQueryHandler().handleRequest(dbRequest);

        } catch (Exception e) {
            logger.log(Level.SEVERE,
                    "Error querying for ASDI records.  DbQueryRequest = "
                            + dbRequest.toString(),
                    e);
        }
        return queryResults;

    }
}
