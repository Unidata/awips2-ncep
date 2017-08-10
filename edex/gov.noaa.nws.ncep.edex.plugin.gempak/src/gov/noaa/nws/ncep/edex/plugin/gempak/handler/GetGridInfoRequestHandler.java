package gov.noaa.nws.ncep.edex.plugin.gempak.handler;

import gov.noaa.nws.ncep.common.dataplugin.gempak.request.GetGridInfoRequest;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.raytheon.uf.common.dataplugin.grid.GridRecord;
import com.raytheon.uf.common.dataplugin.level.mapping.LevelMapper;
import com.raytheon.uf.common.parameter.mapping.ParameterMapper;
import com.raytheon.uf.common.serialization.comm.IRequestHandler;
import com.raytheon.uf.edex.database.DataAccessLayerException;
import com.raytheon.uf.edex.database.dao.CoreDao;
import com.raytheon.uf.edex.database.dao.DaoConfig;
import com.raytheon.uf.edex.database.query.DatabaseQuery;

/**
 * 
 * Handler for GetGridInfoRequest. Retrieves Grid field information from the
 * GridDao using the requested model id, reference time, and forecast time. The
 * model id (datasetId) is required. The reference time is optional and can be
 * specified as a complete reference time or a two digit model run cycle. If no
 * reference time is specified, information is returned for the latest reference
 * time only. If only the two digit cycle time is specified, then information is
 * returned for the latest reference time for that cycle. The forecast time is
 * optional.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * ??? ??, ????            Jacobs     Initial creation
 * Sep 14, 2015 RM8764     Gilbert    Added logic to precess reftime and fcsttime
 * 
 * </pre>
 * 
 * @author sgilbert
 * @version 1.0
 */
public class GetGridInfoRequestHandler implements
        IRequestHandler<GetGridInfoRequest> {

    private static final String DATA_TIME = "dataTime";

    private static final String REF_TIME = DATA_TIME + ".refTime";

    private static final String FCST_TIME = DATA_TIME + ".fcstTime";

    private static final String MODEL_ID = "info.datasetId";

    private static final String EQUALS = "=";

    private static final String GEMPAK = "GEMPAK";

    @Override
    public List<Map<String, String>> handleRequest(GetGridInfoRequest request)
            throws Exception {
        CoreDao gribDao = null;
        gribDao = new CoreDao(DaoConfig.forClass(GridRecord.class));

        List<Map<String, String>> gridList = new ArrayList<Map<String, String>>();

        DatabaseQuery query = buildQuery(request, gribDao);

        List<?> dbList = gribDao.queryByCriteria(query);
        if (dbList != null && !dbList.isEmpty()) {
            for (Object pdo : dbList) {
                GridRecord record = (GridRecord) pdo;

                Map<String, String> gridMap = new HashMap<String, String>();

                gridMap.put("model", record.getDatasetId());
                gridMap.put("second", record.getSecondaryId());
                gridMap.put("ensemble", record.getEnsembleId());
                gridMap.put(
                        "param",
                        ParameterMapper
                                .getInstance()
                                .lookupAlias(
                                        record.getParameter().getAbbreviation(),
                                        GEMPAK));
                gridMap.put(
                        "vcoord",
                        LevelMapper.getInstance().lookupAlias(
                                record.getLevel().getMasterLevel().getName(),
                                GEMPAK));
                gridMap.put("level1", record.getLevel()
                        .getLevelOneValueAsString());
                gridMap.put("level2", record.getLevel()
                        .getLevelTwoValueAsString());
                gridMap.put("reftime", record.getDataTime().getRefTime()
                        .toString());
                gridMap.put("fcstsec",
                        String.valueOf(record.getDataTime().getFcstTime()));

                gridList.add(gridMap);

            }
        }

        return gridList;
    }

    /*
     * Builds the query for grid information given the requested reference time
     * and forecast time, if provided.
     */
    private DatabaseQuery buildQuery(GetGridInfoRequest request, CoreDao gribDao)
            throws DataAccessLayerException {

        DatabaseQuery query = new DatabaseQuery(GridRecord.class);
        query.addQueryParam(MODEL_ID, request.getModelId(), EQUALS);

        /*
         * if forecast time specified, set query constraint on it
         */
        if (!request.getFcstsec().isEmpty()) {
            query.addQueryParam(FCST_TIME, request.getFcstsec(), EQUALS);
        }

        /*
         * If reference time is specified, process it as a "cycle" time or
         * complete reference time, as appropriate
         */
        if (!request.getReftime().isEmpty()) {
            if (request.getReftime().length() <= 2) {
                // Calculate reftime
                Date reftime = findLatestReftime(request, gribDao);
                query.addQueryParam(REF_TIME, reftime, EQUALS);
            } else {
                query.addQueryParam(REF_TIME, request.getReftime(), EQUALS);
            }
        }

        return query;
    }

    /*
     * Query for the latest reference time for given model and cycle
     */
    private Date findLatestReftime(GetGridInfoRequest request, CoreDao gribDao)
            throws DataAccessLayerException {

        Date reftime = null;

        /*
         * Create query for available reference times for this model in
         * descending order
         */
        DatabaseQuery query = new DatabaseQuery(GridRecord.class);
        query.addQueryParam(MODEL_ID, request.getModelId(), EQUALS);
        query.addDistinctParameter(REF_TIME);
        query.addOrder(REF_TIME, false);

        /*
         * Execute query
         */
        List<?> refList;
        refList = gribDao.queryByCriteria(query);

        if (request.getReftime().length() != 2) {
            // Use latest ref time available
            if (refList.get(0) instanceof Date) {
                reftime = (Date) refList.get(0);
            }
        } else {
            // Get latest reference time with the specified cycle time
            String cycle = request.getReftime();
            for (Object obj : refList) {
                if (obj instanceof Date) {
                    Date rtime = (Date) obj;
                    if (matchCycle(cycle, rtime)) {
                        reftime = rtime;
                        break;
                    }
                }
            }
        }
        return reftime;
    }

    /*
     * Determines if the given cycle matches the hour specified in the reference
     * time.
     */
    private boolean matchCycle(String cycle, Date rtime) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.setTime(rtime);
        int run = cal.get(Calendar.HOUR_OF_DAY);
        int icycle = Integer.parseInt(cycle);
        return (run == icycle);
    }
}
