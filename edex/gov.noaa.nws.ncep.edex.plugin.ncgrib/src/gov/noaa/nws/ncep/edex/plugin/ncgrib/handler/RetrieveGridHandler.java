package gov.noaa.nws.ncep.edex.plugin.ncgrib.handler;

import gov.noaa.nws.ncep.common.dataplugin.ncgrib.request.RetrieveGridRequest;

import java.util.logging.Logger;

import com.raytheon.uf.common.dataplugin.grid.GridRecord;
import com.raytheon.uf.common.dataplugin.persist.IPersistable;
import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.common.datastorage.Request;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.datastorage.records.StringDataRecord;
import com.raytheon.uf.common.message.response.AbstractResponseMessage;
import com.raytheon.uf.common.message.response.ResponseMessageError;
import com.raytheon.uf.common.message.response.ResponseMessageGeneric;
import com.raytheon.uf.common.serialization.comm.IRequestHandler;
import com.raytheon.uf.edex.database.plugin.PluginDao;
import com.raytheon.uf.edex.database.plugin.PluginFactory;

/**
 * Handler for grid record retrieval.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer     Description
 * ------------ ---------- -----------  --------------------------
 * 03/01/2016   R6821      kbugenhagen  Initial creation
 * 
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1.0
 */

public class RetrieveGridHandler implements
        IRequestHandler<RetrieveGridRequest> {

    private static Logger logger = Logger.getLogger(RetrieveGridHandler.class
            .toString());

    private final static String GRID_PLUGIN_NAME = "grid";

    private PluginDao gridDao;

    @Override
    public Object handleRequest(RetrieveGridRequest request) throws Exception {
        AbstractResponseMessage response = null;
        IDataRecord records = null;

        logger.info("RetrieveGridRequest for " + request.getDataURI());

        try {
            gridDao = PluginFactory.getInstance()
                    .getPluginDao(GRID_PLUGIN_NAME);

            GridRecord gridRecord = new GridRecord(request.getDataURI());
            IDataStore dataStore = gridDao
                    .getDataStore((IPersistable) gridRecord);
            records = dataStore.retrieve(request.getDataURI(), "Data",
                    Request.ALL);

        } catch (Exception e) {
            response = ResponseMessageError.generateErrorResponse(
                    "Could not retrieve Grid request", e);
            return response;
        }

        if (records instanceof StringDataRecord) {
            String[] stringdata = ((StringDataRecord) records).getStringData();
            response = new ResponseMessageGeneric(stringdata[0]);
            response.setDataURI(request.getDataURI());
        } else {
            response = ResponseMessageError.generateErrorResponse(
                    "Unexpected item returned", null);
        }
        return response;
    }

}
