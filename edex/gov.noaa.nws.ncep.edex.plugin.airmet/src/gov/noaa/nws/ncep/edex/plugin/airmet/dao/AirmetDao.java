package gov.noaa.nws.ncep.edex.plugin.airmet.dao;

import java.util.List;

import com.raytheon.edex.db.dao.DefaultPluginDao;
import com.raytheon.uf.common.dataplugin.PluginException;
import com.raytheon.uf.edex.database.DataAccessLayerException;

import gov.noaa.nws.ncep.common.dataplugin.airmet.AirmetRecord;

/**
 * Data Access Object for interacting with the database and data store for
 * AIRMET data.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 23, 2009            jkorman     Initial creation
 * 09/2011                 Chin Chen   changed to improve purge performance and
 *                                     removed xml serialization as well
 * Dec 14, 2016  5934      njensen     Moved to edex airmet plugin
 * Feb 16, 2022  8608      mapeters    Extend DefaultPluginDao
 *
 * </pre>
 *
 * @author jkorman
 */

public class AirmetDao extends DefaultPluginDao {

    /**
     * Creates a new AirmetDao
     *
     * @throws PluginException
     */
    public AirmetDao(String pluginName) throws PluginException {
        super(pluginName);
    }

    /**
     * Retrieves an sfcobs report using the datauri .
     *
     * @param dataURI
     *            The dataURI to match against.
     * @return The report record if it exists.
     */
    public AirmetRecord queryByDataURI(String dataURI) {
        AirmetRecord report = null;
        List<?> obs = null;
        try {
            obs = queryBySingleCriteria("dataURI", dataURI);
        } catch (DataAccessLayerException e) {
            logger.error("Error querying AIRMET data by URI: " + dataURI, e);
        }
        if (obs != null && !obs.isEmpty()) {
            report = (AirmetRecord) obs.get(0);
        }
        return report;
    }

    /**
     * Queries for to determine if a given data uri exists on the sfcobs table.
     *
     * @param dataUri
     *            The DataURI to find.
     * @return An array of objects. If not null, there should only be a single
     *         element.
     */
    public Object[] queryDataUriColumn(final String dataUri) {

        String sql = "select datauri from awips.airmet where datauri='"
                + dataUri + "';";

        Object[] results = executeSQLQuery(sql);

        return results;
    }
}
