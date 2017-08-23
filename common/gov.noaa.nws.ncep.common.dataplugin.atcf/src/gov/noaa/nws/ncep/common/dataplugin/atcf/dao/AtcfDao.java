/**
 * Set of DAO methods for ATCF data.
 *
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date			Ticket#		Engineer	Description
 * ------------ -----------	----------- --------------------------
 * 06/23/10		283			F. J. Yen	Initial Coding
 * 10/05/2016   R22939      bhebbard    Adapted to ATCF
 * </pre>
 * 
 * @author fjyen
 * @version 1.0
 **/
package gov.noaa.nws.ncep.common.dataplugin.atcf.dao;

import gov.noaa.nws.ncep.common.dataplugin.atcf.AtcfRecord;
import gov.noaa.nws.ncep.edex.common.dao.NcepDefaultPluginDao;

import java.util.List;

import com.raytheon.uf.common.dataplugin.PluginException;
import com.raytheon.uf.common.dataplugin.persist.IPersistable;
import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.edex.database.DataAccessLayerException;
import com.raytheon.uf.edex.database.plugin.PluginDao;

public class AtcfDao extends PluginDao {

    /**
     * Creates a new AtcfDao
     * 
     * @throws PluginException
     */
    public AtcfDao(String pluginName) throws PluginException {
        super(pluginName);
    }

    /**
     * Retrieves an ATCF record using the dataURI .
     * 
     * @param dataURI
     *            The dataURI to match against.
     * @return The report record if it exists.
     */
    public AtcfRecord queryByDataURI(String dataURI) {
        AtcfRecord report = null;
        List<?> obs = null;
        try {
            obs = queryBySingleCriteria("dataURI", dataURI);
        } catch (DataAccessLayerException e) {
            e.printStackTrace();
        }
        if ((obs != null) && (obs.size() > 0)) {
            report = (AtcfRecord) obs.get(0);
        }
        return report;
    }

    /**
     * Queries for to determine if a given data uri exists on the ATCF table.
     * 
     * @param dataUri
     *            The DataURI to find.
     * @return An array of objects. If not null, there should only be a single
     *         element.
     */
    public Object[] queryDataUriColumn(final String dataUri) {

    	String sql = "select datauri from awips.atcf where datauri = :datauri";

    	Object[] results = executeSQLQuery(sql, "datauri", dataUri);

        return results;
    }

	@Override
	protected IDataStore populateDataStore(IDataStore dataStore, IPersistable obj) throws Exception {
		// Must implement (unused) stub for PluginDao; may use later if IDataStore used for ATCF data
		return null;
	}
}
