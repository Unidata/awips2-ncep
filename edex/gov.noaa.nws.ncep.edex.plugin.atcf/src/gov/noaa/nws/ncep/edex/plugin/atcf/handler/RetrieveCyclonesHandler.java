package gov.noaa.nws.ncep.edex.plugin.atcf.handler;

import gov.noaa.nws.ncep.common.dataplugin.atcf.AtcfRecord;
import gov.noaa.nws.ncep.common.dataplugin.atcf.request.RetrieveCyclonesRequest;
import gov.noaa.nws.ncep.edex.uengine.tasks.atcf.AtcfCyclone;
import gov.noaa.nws.ncep.edex.uengine.tasks.atcf.AtcfTrack;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataquery.requests.DbQueryRequest;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint.ConstraintType;
import com.raytheon.uf.common.dataquery.responses.DbQueryResponse;
import com.raytheon.uf.common.serialization.comm.IRequestHandler;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.time.TimeRange;
import com.raytheon.uf.edex.database.handlers.DbQueryHandler;

/**
 * 
 * Handler for RetrieveCyclonesRequest. Retrieves ATCF Cyclones.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#  Engineer     Description
 * ------------ -------  -----------  --------------------------
 * 7/14/2016    R9715    kbugenhagen  Initial creation.
 * 
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1.0
 */
public class RetrieveCyclonesHandler implements
        IRequestHandler<RetrieveCyclonesRequest> {

    private static Logger logger = Logger
            .getLogger(RetrieveCyclonesHandler.class.toString());

    protected static final String FIELD_PLUGIN_NAME = "pluginName";

    protected static final String FIELD_VALID_START = "dataTime.validPeriod.start";

    protected static final String FIELD_VALID_END = "dataTime.validPeriod.end";

    protected static final String FIELD_REFTIME = "dataTime.refTime";

    protected static final String FIELD_DATATIME = "dataTime";

    protected static final String FIELD_TECHNIQUE = "technique";

    protected static final String FIELD_BASIN = "basin";

    protected static final String FIELD_CYCLONE_NUM = "cycloneNum";

    protected static final String RADII_WIND_FIELD = "radWind";

    protected static final String RADII_WIND_CONSTRAINT_VALUE = "34";

    protected static final String FORECAST_HOUR_FIELD = "fcstHour";

    protected static final String FORECAST_HOUR_CONSTRAINT_VALUE = "0";

    protected static final int ONE_WEEK_IN_HOURS = 168;

    private final static String ATCF_DAO = "atcf";

    /**
     * Container class for cyclone ids, which consist of a basin and cylone
     * number.
     */
    public class CycloneId {
        private String basin;

        private int cycloneNum;

        public CycloneId(String basin, int cycloneNum) {
            super();
            this.basin = basin;
            this.cycloneNum = cycloneNum;
        }

        public String getBasin() {
            return basin;
        }

        public void setBasin(String basin) {
            this.basin = basin;
        }

        public int getCycloneNum() {
            return cycloneNum;
        }

        public void setCycloneNum(int cycloneNum) {
            this.cycloneNum = cycloneNum;
        }

        @Override
        public boolean equals(Object c) {
            if (c == null) {
                return false;
            }
            if (!(c instanceof CycloneId)) {
                return false;
            }
            CycloneId other = (CycloneId) c;
            if (Objects.equals(other.basin, basin)
                    && Objects.equals(other.cycloneNum, cycloneNum)) {
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return basin + cycloneNum;
        }
    }

    /**
     * Container class for cyclone times, which consist of a dataTime and cylone
     * id.
     */
    public class CycloneTime {

        private DataTime dataTime;

        private CycloneId cycloneId;

        public CycloneTime(DataTime recordTime, CycloneId cycloneId) {
            this.dataTime = recordTime;
            this.cycloneId = cycloneId;
        }

        public DataTime getDataTime() {
            return dataTime;
        }

        public void setDataTime(DataTime dataTime) {
            this.dataTime = dataTime;
        }

        public CycloneId getCycloneId() {
            return cycloneId;
        }

        public void setCycloneId(CycloneId cycloneId) {
            this.cycloneId = cycloneId;
        }

        @Override
        public boolean equals(Object c) {
            if (c == null) {
                return false;
            }
            if (!(c instanceof CycloneTime)) {
                return false;
            }
            CycloneTime other = (CycloneTime) c;
            if (Objects.equals(other.dataTime, dataTime)
                    && Objects.equals(other.cycloneId, cycloneId)) {
                return true;
            }
            return false;
        }
    }

    /**
     * Handles a cyclones request.
     * 
     * @param request
     *            cyclone request
     * @return list of cyclones
     * 
     *         (non-Javadoc)
     * 
     * @see com.raytheon.uf.common.serialization.comm.IRequestHandler#handleRequest
     *      (com.raytheon.uf.common.serialization.comm.IServerRequest)
     */
    @Override
    public List<AtcfCyclone> handleRequest(RetrieveCyclonesRequest request) {

        List<AtcfCyclone> response = new ArrayList<>();

        // query for all records in time range
        DbQueryResponse queryResults = queryAtcfRecordsForTimeRange(request);

        Map<CycloneTime, List<String>> cycloneTechniques = createCycloneTechniques(queryResults);

        // Create cyclones
        for (CycloneTime cycloneTime : cycloneTechniques.keySet()) {

            List<String> techniques = cycloneTechniques.get(cycloneTime);
            CycloneId cycloneId = cycloneTime.getCycloneId();
            DataTime dataTime = cycloneTime.getDataTime();
            AtcfCyclone cyclone = new AtcfCyclone(cycloneId.toString());

            // Create a cyclone for each its techniques
            for (String technique : techniques) {
                DbQueryRequest dbRequest = new DbQueryRequest();

                if (technique.equals("CARQ")) {

                    queryResults = getCarqTracks(cycloneId, dataTime, technique);

                } else {

                    // non-CARQ techniques
                    dbRequest = buildBaseTrackDataQuery(cycloneId, dataTime,
                            technique);
                    dbRequest.setOrderByField(FORECAST_HOUR_FIELD);

                    try {
                        queryResults = new DbQueryHandler()
                                .handleRequest(dbRequest);
                    } catch (Exception e) {
                        logger.log(Level.SEVERE,
                                "Error querying for track data for non-CARQ technique.  DbQueryRequest = "
                                        + dbRequest.toString(), e);
                    }
                }

                addTracksToCyclone(queryResults, cyclone, technique);

                response.add(cyclone);

            }
        }

        return response;
    }

    /**
     * Adds tracks from query results to a cyclone
     * 
     * @param queryResults
     *            tracks from query
     * @param cyclone
     * @param technique
     *            cyclone technique
     */
    private void addTracksToCyclone(DbQueryResponse queryResults,
            AtcfCyclone cyclone, String technique) {

        List<Map<String, Object>> results = null;
        int numResults = queryResults.getNumResults();
        if (numResults > 0) {
            results = queryResults.getResults();
            List<PluginDataObject> recordList = new ArrayList<>();
            for (int i = 0; i < numResults; i++) {
                Map<String, Object> result = results.get(i);
                AtcfRecord record = (AtcfRecord) result.get(null);
                recordList.add(record);
            }
            AtcfTrack track = new AtcfTrack(technique, recordList);
            cyclone.addTrack(track);
        } else {
            logger.info("No track data results for technique = " + technique);
        }

    }

    /**
     * Queries for CARQ-specific tracks
     * 
     * @param cycloneId
     *            cyclone identifier
     * @param dataTime
     *            cyclone dataTime
     * @param technique
     *            cyclone technique
     * @return query results containing tracks
     */
    private DbQueryResponse getCarqTracks(CycloneId cycloneId,
            DataTime dataTime, String technique) {

        // query for past track data
        DbQueryRequest dbRequest = buildBaseTrackDataQuery(cycloneId, dataTime,
                technique);
        DbQueryResponse queryResults = queryCarqTrackData(dbRequest, dataTime,
                false);

        // query for future track data
        dbRequest = buildBaseTrackDataQuery(cycloneId, dataTime, technique,
                true);
        DbQueryResponse queryResultsFuture = queryCarqTrackData(dbRequest,
                dataTime, true);

        // add future track data to past tracks
        int numResults = queryResultsFuture.getNumResults();
        for (int i = 0; i < numResults; i++) {
            queryResults.getResults().add(
                    queryResultsFuture.getResults().get(i));
        }

        return queryResults;

    }

    /**
     * Create map of techniques for cyclones
     * 
     * @param queryResults
     *            query results containing ATCF records for timerange
     * @return map of techniques for cyclones
     */
    private Map<CycloneTime, List<String>> createCycloneTechniques(
            DbQueryResponse queryResults) {
        int numResults = queryResults.getNumResults();
        List<Map<String, Object>> results = queryResults.getResults();
        List<CycloneTime> cycloneTimes = new ArrayList<>();
        Map<CycloneTime, List<String>> cycloneTechniques = new HashMap<>();

        for (int i = 0; i < numResults; i++) {
            Map<String, Object> result = results.get(i);
            AtcfRecord record = (AtcfRecord) result.get(null);
            DataTime dataTime = record.getDataTime();
            CycloneId cycloneId = new CycloneId(record.getBasin(),
                    record.getCycloneNum());
            CycloneTime cycloneTime = new CycloneTime(dataTime, cycloneId);
            if (!cycloneTimes.contains(cycloneTime)) {
                cycloneTimes.add(cycloneTime);
                List<String> techniqueList = new ArrayList<>();

                // query for list of techniques for this cyclone time
                queryResults = queryTechniques(cycloneId, dataTime);
                int numTechniqueResults = queryResults.getNumResults();
                List<Map<String, Object>> techniqueResults = queryResults
                        .getResults();
                for (int j = 0; j < numTechniqueResults; j++) {
                    result = techniqueResults.get(j);
                    String technique = (String) result.get("technique");
                    techniqueList.add(technique);
                }
                cycloneTechniques.put(cycloneTime, techniqueList);
            }
        }

        return cycloneTechniques;

    }

    private DbQueryRequest buildBaseTrackDataQuery(CycloneId cycloneId,
            DataTime dataTime, String technique) {

        return buildBaseTrackDataQuery(cycloneId, dataTime, technique, false);

    }

    /**
     * 
     * Creates database query for track data.
     * 
     * @param cycloneId
     *            cyclone identifier
     * @param dataTime
     *            cyclone dataTime
     * @param technique
     *            cyclone technique
     * @param isTimeRangeConstraint
     *            if true, create timerange constraint
     * @return database query request
     */
    private DbQueryRequest buildBaseTrackDataQuery(CycloneId cycloneId,
            DataTime dataTime, String technique, boolean isTimeRangeConstraint) {

        DbQueryRequest dbRequest = new DbQueryRequest();
        dbRequest.addConstraint(FIELD_PLUGIN_NAME, new RequestConstraint(
                ATCF_DAO));

        if (isTimeRangeConstraint) {
            RequestConstraint dataTimeStart = new RequestConstraint(
                    String.valueOf(dataTime));
            Calendar endTime = Calendar.getInstance();
            endTime.setTime(dataTime.getValidTimeAsDate());
            endTime.add(Calendar.HOUR_OF_DAY, ONE_WEEK_IN_HOURS);
            RequestConstraint dataTimeEnd = new RequestConstraint(endTime
                    .getTime().toString(), ConstraintType.LESS_THAN_EQUALS);
            dbRequest.addConstraint(FIELD_VALID_START, dataTimeStart);
            dbRequest.addConstraint(FIELD_VALID_END, dataTimeEnd);
        } else {
            RequestConstraint dataTimeConstraint = new RequestConstraint(
                    String.valueOf(dataTime));
            dbRequest.addConstraint(FIELD_DATATIME, dataTimeConstraint);
        }
        RequestConstraint basinConstraint = new RequestConstraint(
                cycloneId.getBasin());
        dbRequest.addConstraint(FIELD_BASIN, basinConstraint);
        RequestConstraint cycloneNumConstraint = new RequestConstraint(
                String.valueOf(cycloneId.getCycloneNum()));
        dbRequest.addConstraint(FIELD_CYCLONE_NUM, cycloneNumConstraint);
        RequestConstraint techniqueConstraint = new RequestConstraint(technique);
        dbRequest.addConstraint(FIELD_TECHNIQUE, techniqueConstraint);

        return dbRequest;

    }

    /**
     * Queries for data specific to CARQ technique.
     * 
     * @param dbRequest
     *            database request
     * @param dataTime
     *            cyclione data time
     * @param isFutureQuery
     *            if true, indicates to query for future data.
     * @return database response containing track data
     */
    private DbQueryResponse queryCarqTrackData(DbQueryRequest dbRequest,
            DataTime dataTime, boolean isFutureQuery) {

        DbQueryResponse queryResults = new DbQueryResponse();
        RequestConstraint foreCastHourConstraint;

        if (isFutureQuery) {
            foreCastHourConstraint = new RequestConstraint(
                    FORECAST_HOUR_CONSTRAINT_VALUE);
            dbRequest.setOrderByField(FIELD_DATATIME);
        } else {
            foreCastHourConstraint = new RequestConstraint(
                    FORECAST_HOUR_CONSTRAINT_VALUE, ConstraintType.LESS_THAN);
            dbRequest.setOrderByField(FORECAST_HOUR_FIELD);
        }

        RequestConstraint radWindConstraint = new RequestConstraint(
                RADII_WIND_CONSTRAINT_VALUE);
        dbRequest.addConstraint(RADII_WIND_FIELD, radWindConstraint);
        dbRequest.addConstraint(FORECAST_HOUR_FIELD, foreCastHourConstraint);

        try {

            queryResults = new DbQueryHandler().handleRequest(dbRequest);

        } catch (Exception e) {
            logger.log(Level.SEVERE,
                    "Error querying for track data for CARQ technique.  DbQueryRequest = "
                            + dbRequest.toString(), e);
        }

        return queryResults;

    }

    /**
     * Queries for techniques for a cyclone.
     * 
     * @param cycloneId
     *            cyclone identifier
     * @param dataTime
     *            cyclone datatime
     * @return query response containing techniques
     */
    private DbQueryResponse queryTechniques(CycloneId cycloneId,
            DataTime dataTime) {

        DbQueryRequest dbRequest = new DbQueryRequest();
        DbQueryResponse queryResults = new DbQueryResponse();
        dbRequest.addRequestField(FIELD_TECHNIQUE);
        dbRequest.setDistinct(true);
        dbRequest.addConstraint(FIELD_PLUGIN_NAME, new RequestConstraint(
                ATCF_DAO));
        RequestConstraint basinConstraint = new RequestConstraint(
                cycloneId.getBasin());
        RequestConstraint cycloneNumConstraint = new RequestConstraint(
                String.valueOf(cycloneId.getCycloneNum()));
        RequestConstraint refTimeConstraint = new RequestConstraint(
                String.valueOf(dataTime.getRefTime()));
        dbRequest.addConstraint(FIELD_BASIN, basinConstraint);
        dbRequest.addConstraint(FIELD_CYCLONE_NUM, cycloneNumConstraint);
        dbRequest.addConstraint(FIELD_REFTIME, refTimeConstraint);

        try {

            queryResults = new DbQueryHandler().handleRequest(dbRequest);

        } catch (Exception e) {
            logger.log(Level.SEVERE,
                    "Error querying for techniques.  DbQueryRequest = "
                            + dbRequest.toString(), e);
        }

        return queryResults;

    }

    /**
     * Queries for ATCF records within a time range.
     * 
     * @param request
     *            cyclone request
     * @return database response containing ATCF records.
     */
    private DbQueryResponse queryAtcfRecordsForTimeRange(
            RetrieveCyclonesRequest request) {

        TimeRange timeRange = new TimeRange(request.getStartTime(),
                request.getEndTime());
        DbQueryRequest dbRequest = new DbQueryRequest();
        DbQueryResponse queryResults = new DbQueryResponse();
        dbRequest.addConstraint(FIELD_PLUGIN_NAME, new RequestConstraint(
                ATCF_DAO));

        // Add the TimeRange Constraint
        if (timeRange != null) {
            RequestConstraint afterReqStart = new RequestConstraint(timeRange
                    .getStart().toString(), ConstraintType.GREATER_THAN_EQUALS);
            RequestConstraint beforeReqEnd = new RequestConstraint(timeRange
                    .getEnd().toString(), ConstraintType.LESS_THAN_EQUALS);
            dbRequest.addConstraint(FIELD_VALID_START, afterReqStart);
            dbRequest.addConstraint(FIELD_VALID_END, beforeReqEnd);
        }

        try {

            queryResults = new DbQueryHandler().handleRequest(dbRequest);

        } catch (Exception e) {
            logger.log(Level.SEVERE,
                    "Error querying for ATCF records.  DbQueryRequest = "
                            + dbRequest.toString(), e);
        }
        return queryResults;

    }
}
