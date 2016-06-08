package gov.noaa.nws.ncep.edex.plugin.ncgrib.handler;

import gov.noaa.nws.ncep.common.dataplugin.ncgrib.request.ResponseMessageValidate;
import gov.noaa.nws.ncep.common.dataplugin.ncgrib.request.StoreGridRequest;

import java.util.Arrays;
import java.util.logging.Logger;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.grid.GridRecord;
import com.raytheon.uf.common.datastorage.StorageStatus;
import com.raytheon.uf.common.serialization.comm.IRequestHandler;
import com.raytheon.uf.edex.database.plugin.PluginDao;
import com.raytheon.uf.edex.database.plugin.PluginFactory;

/**
 * Receives the StoreGridRequest from CAVE and saves the GridRecord to the
 * datastores.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 03/01/2016   R6821      kbugenhagen  Initial creation
 * 
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1.0
 */

public class StoreGridHandler implements IRequestHandler<StoreGridRequest> {

    private static Logger logger = Logger.getLogger(StoreGridHandler.class
            .toString());

    private final static String GRID_PLUGIN_NAME = "grid";

    private PluginDao gridDao;

    @Override
    public Object handleRequest(StoreGridRequest request) throws Exception {

        ResponseMessageValidate response = new ResponseMessageValidate();
        gridDao = PluginFactory.getInstance().getPluginDao(GRID_PLUGIN_NAME);

        GridRecord gridRecord = request.getGridRecord();

        logger.info("StoreGridHandler for " + gridRecord.getDataURI());

        try {

            // Save HDF file

            StorageStatus status = gridDao.persistToHDF5(gridRecord);

            logger.info("StorageStatus [exceptions="
                    + Arrays.toString(status.getExceptions())
                    + ", operationPerformed=" + status.getOperationPerformed()
                    + ", indexOfAppend="
                    + Arrays.toString(status.getIndexOfAppend()) + "]");

            if (status.getExceptions().length > 0) {
                response.setMessage(gridRecord.getDataURI() + "\n"
                        + status.getExceptions()[0].getMessage());
                response.setResult(Boolean.FALSE);
                return response;
            } else {

                // Save metadata in database

                PluginDataObject[] objects = gridDao
                        .persistToDatabase(gridRecord);
                logger.info("BERNJA: PluginDataObject ["
                        + Arrays.toString(objects) + "]");
            }
        } catch (Exception e) {
            response.setMessage(gridRecord.getDataURI() + "\n" + e.getMessage());
            response.setResult(Boolean.FALSE);
            return response;
        }
        response.setMessage("Persisted: " + gridRecord.getDataURI());
        response.setResult(Boolean.TRUE);

        return response;
    }
}