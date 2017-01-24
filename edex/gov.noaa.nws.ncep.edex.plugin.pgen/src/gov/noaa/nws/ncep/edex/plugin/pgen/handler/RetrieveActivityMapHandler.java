package gov.noaa.nws.ncep.edex.plugin.pgen.handler;

import gov.noaa.nws.ncep.common.dataplugin.pgen.PgenRecord;
import gov.noaa.nws.ncep.common.dataplugin.pgen.request.RetrieveActivityMapRequest;
import gov.noaa.nws.ncep.common.dataplugin.pgen.response.ActivityMapData;
import gov.noaa.nws.ncep.common.dataplugin.pgen.response.RetrieveActivityMapResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.raytheon.uf.common.dataquery.requests.DbQueryRequest;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.dataquery.responses.DbQueryResponse;
import com.raytheon.uf.common.serialization.comm.IRequestHandler;
import com.raytheon.uf.edex.database.handlers.DbQueryHandler;

/**
 * 
 * Handler for RetrieveAllProductsRequest. Retrieves the PGEN activity XML and
 * all derived products for the given datauri.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 05/13/2016   R9714        byin      Initial creation
 * 
 * </pre>
 * 
 * @author byin
 * @version 1.0
 */

public class RetrieveActivityMapHandler implements
        IRequestHandler<RetrieveActivityMapRequest> {

    private static Logger logger = Logger
            .getLogger(RetrieveActivityMapHandler.class.toString());

    private final static String PGEN_PLUGIN_NAME = "pgen";

    /**
     *
     */
    @Override
    public RetrieveActivityMapResponse handleRequest(
            RetrieveActivityMapRequest mapRequest) {

        logger.info("RetrieveActivityMapRequest");

        HashMap<String, RequestConstraint> queryTerms = new HashMap<String, RequestConstraint>();
        queryTerms.put("pluginName", new RequestConstraint(PGEN_PLUGIN_NAME));
        RetrieveActivityMapResponse response = new RetrieveActivityMapResponse();

        DbQueryRequest request = new DbQueryRequest();
        request.setConstraints(queryTerms);
        request.addRequestField(PgenRecord.ACTIVITY_TYPE);
        request.addRequestField(PgenRecord.ACTIVITY_SUBTYPE);
        request.addRequestField(PgenRecord.ACTIVITY_LABEL);
        request.addRequestField(PgenRecord.REF_TIME);
        request.addRequestField(PgenRecord.ACTIVITY_NAME);
        request.addRequestField(PgenRecord.DATAURI);

        DbQueryResponse queryResults = null;

        try {
            queryResults = new DbQueryHandler().handleRequest(request);
            ActivityMapData[] data = new ActivityMapData[queryResults
                    .getNumResults()];

            for (int ii = 0; ii < queryResults.getNumResults(); ii++) {
                Map<String, Object> result = queryResults.getResults().get(ii);
                data[ii] = new ActivityMapData();
                data[ii].setActivityLabel((String) result
                        .get(PgenRecord.ACTIVITY_LABEL));
                data[ii].setActivityName((String) result
                        .get(PgenRecord.ACTIVITY_NAME));
                data[ii].setActivitySubtype((String) result
                        .get(PgenRecord.ACTIVITY_SUBTYPE));
                data[ii].setActivityType((String) result
                        .get(PgenRecord.ACTIVITY_TYPE));
                data[ii].setDataURI((String) result.get(PgenRecord.DATAURI));
                data[ii].setRefTime(result.get(PgenRecord.REF_TIME).toString());
            }

            response.setData(data);

        } catch (Exception e) {
            logger.warning("Error retrieving PGEN activity map.");
        }

        return response;
    }
}