package gov.noaa.nws.ncep.edex.plugin.gempak.handler;

import gov.noaa.nws.ncep.common.dataplugin.gempak.request.UpperAirDataRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.raytheon.uf.common.pointdata.PointDataContainer;
import com.raytheon.uf.common.pointdata.PointDataDescription.Type;
import com.raytheon.uf.common.pointdata.PointDataView;
import com.raytheon.uf.common.serialization.comm.IRequestHandler;
import com.raytheon.uf.edex.pointdata.PointDataQuery;

/**
 * Executes point data queries for station upper air levels data under a single
 * timestamp.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 19, 2016 R17968     pmoyer      Initial creation
 * 
 * </pre>
 * 
 * @author pmoyer
 * @version 1.0
 */
public class UpperAirDataRequestHandler implements
        IRequestHandler<UpperAirDataRequest> {

    private static final String DB_REF_TIME = "dataTime.refTime";

    private static final String REF_HOUR = "refHour";

    private static final String REP_TYPE = "reportType";

    private static final String REF_TIME = "refTime";

    private static final String LATITUDE = "latitude";

    private static final String LONGITUDE = "longitude";

    private static final String STATION_NAME = "stationName"; // For METAR

    private static final String STATION_ID = "stationId"; // For SYNOP

    /**
     * Accepts the passed in UpperAirDataRequest passed in by the
     * RequestServiveExecutor. Returns a map of Station text ID's to lists of
     * data parameters for those stations for the requested timestamp (if it
     * exists).
     */
    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.common.serialization.comm.IRequestHandler#handleRequest
     * (com.raytheon.uf.common.serialization.comm.IServerRequest)
     */
    @Override
    public Map<String, Object> handleRequest(UpperAirDataRequest request)
            throws Exception {

        // map for stations to parameters. each station string is associated
        // with a HashMap object of station parameters (each of which can have
        // individual, Object, or Collection values)
        Map<String, Object> stationParams = new HashMap<>();

        PointDataQuery query = new PointDataQuery(request.getPluginName());

        // add additional fields for param list for request:
        // refTime
        // longitude
        // latitude
        // stationName

        String additionalHDF5parameters = "," + REF_TIME + "," + LATITUDE + ","
                + LONGITUDE + "," + STATION_NAME + "," + STATION_ID;

        query.setParameters(request.getParmList() + additionalHDF5parameters);

        if (!request.getPluginName().equalsIgnoreCase("bufrua")) {
            query.addParameter(REF_HOUR, request.getRefTime().toString(), "=");
            query.addParameter(DB_REF_TIME, request.getRefTime().toString(),
                    "<=");
        } else {
            query.addParameter(DB_REF_TIME, request.getRefTime().toString(),
                    "=");
        }
        if (!request.getPartNumber().equals("0")) {
            query.addParameter(REP_TYPE, request.getPartNumber(), "=");
        }

        query.requestAllLevels();

        PointDataContainer resultPDC = null;

        resultPDC = query.execute();
        if (resultPDC == null) {
            return stationParams;
        }

        // driving list for HDF5 parameters from initial request
        ArrayList<String> hdf5ParamList = new ArrayList<>(Arrays.asList(request
                .getParmList().split(",")));

        int pdcSize = resultPDC.getAllocatedSz();
        resultPDC.setCurrentSz(pdcSize);

        // determine which parameter STATION_NAME or STATION_ID actually
        // existed. (lack of either should just return empty stationParams)
        String stationIdentifier = "";

        Set<String> knownParameters = resultPDC.getParameters();
        if (knownParameters.contains(STATION_NAME)) {
            stationIdentifier = STATION_NAME;
        } else if (knownParameters.contains(STATION_ID)) {
            stationIdentifier = STATION_ID;
        } else {
            return stationParams;
        }

        // for each station in retrieved list, iterate through it and its
        // parameters in parallel
        for (int uriCounter = 0; uriCounter < pdcSize; uriCounter++) {
            PointDataView pdv = resultPDC.readRandom(uriCounter);

            String stationID = new String(pdv.getString(stationIdentifier));

            Map<String, Object> parameterValues = new HashMap<>(
                    hdf5ParamList.size());

            for (String param : hdf5ParamList) {

                int dimensions = pdv.getDimensions(param);

                Type t = pdv.getType(param);

                switch (t) {
                case FLOAT:
                case INT:
                case LONG:
                    if (dimensions == 2) {
                        parameterValues.put(param,
                                pdv.getNumberAllLevels(param));
                    } else {
                        parameterValues.put(param, pdv.getNumber(param));
                    }
                    break;
                case STRING:
                    if (dimensions == 2) {
                        parameterValues.put(param,
                                pdv.getStringAllLevels(param));
                    } else {
                        parameterValues.put(param, pdv.getString(param));
                    }
                    break;
                }

            }

            stationParams.put(stationID, parameterValues);

        }

        return stationParams;
    }
}
