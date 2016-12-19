/**
 * AwwDao
 * 
 * This java class represents the county FIPS for an AWW record.
 * 
 * <pre>
 * 
 * HISTORY
 *
 * Date         Author     Description
 * ------------ ---------- -------------------------------------
 * 05/2010      L. Lin     Initial creation
 * 09/2011      Chin Chen  changed to improve purge performance and
 *                         removed xml serialization as well
 * Dec 19, 2016 njensen    Moved to edex aww plugin 
 * 
 * This code has been developed by the SIB for use in the AWIPS2 system.
 * 
 * </pre>
 */
package gov.noaa.nws.ncep.edex.plugin.aww.dao;

import java.util.List;

import com.raytheon.uf.common.dataplugin.PluginException;
import com.raytheon.uf.common.dataplugin.persist.IPersistable;
import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.edex.database.DataAccessLayerException;
import com.raytheon.uf.edex.database.plugin.PluginDao;

import gov.noaa.nws.ncep.common.dataplugin.aww.AwwRecord;

public class AwwDao extends PluginDao {

    /**
     * Creates a new AwwDao
     * 
     * @throws PluginException
     */
    public AwwDao(String pluginName) throws PluginException {
        super(pluginName);
    }

    /**
     * Retrieves an sfcobs report using the datauri .
     * 
     * @param dataURI
     *            The dataURI to match against.
     * @return The report record if it exists.
     */
    public AwwRecord queryByDataURI(String dataURI) {
        AwwRecord report = null;
        List<?> obs = null;
        try {
            obs = queryBySingleCriteria("dataURI", dataURI);
        } catch (DataAccessLayerException e) {
            e.printStackTrace();
        }
        if ((obs != null) && (obs.size() > 0)) {
            report = (AwwRecord) obs.get(0);
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

        String sql = "select datauri from awips.aww where datauri='"
                + dataUri + "';";

        Object[] results = executeSQLQuery(sql);

        return results;
    }

	@Override
	protected IDataStore populateDataStore(IDataStore dataStore,
			IPersistable obj) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
}
