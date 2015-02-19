/**
 * 
 * TcmDao
 * 
 * This java class performs data access for TCM.
 *  
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date         Ticket#         Engineer    Description
 * ------------ ----------      ----------- --------------------------
 * 05/2010		128				T. Lee		Migration to TO11DR11
 * 09/2011      			    Chin Chen   changed to improve purge performance and
 * 										    removed xml serialization as well
 * </pre>
 *
 * @author T.Lee
 * @version 1.0
 */

package gov.noaa.nws.ncep.common.dataplugin.tcm.dao;

import gov.noaa.nws.ncep.common.dataplugin.tcm.TcmRecord;

import java.util.List;
import com.raytheon.uf.common.dataplugin.PluginException;
import com.raytheon.uf.common.dataplugin.persist.IPersistable;
import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.edex.database.DataAccessLayerException;
import com.raytheon.uf.edex.database.plugin.PluginDao;

public class TcmDao extends PluginDao {
	
    /**
     * FfgDao constructor.
     * 
     * @throws PluginException
     */
    public TcmDao(String pluginName) throws PluginException {
        super(pluginName);
    }

    /**
     * Retrieves an FFG report using the datauri .
     * 
     * @param dataURI
     *            The dataURI to match against.
     * @return The report record if it exists.
     */
    public TcmRecord queryByDataURI(String dataURI) {
        TcmRecord report = null;
        List<?> obs = null;
        try {
            obs = queryBySingleCriteria("dataURI", dataURI);
        } catch (DataAccessLayerException e) {
            e.printStackTrace();
        }
        if ((obs != null) && (obs.size() > 0)) {
            report = (TcmRecord) obs.get(0);
        }
        return report;
    }

    /**
     * Queries for to determine if a given data uri exists on the FFG table.
     * 
     * @param dataUri
     *            The DataURI to find.
     * @return An array of objects. If not null, there should only be a single
     *         element.
     */
    public Object[] queryDataUriColumn(final String dataUri) {

        String sql = "select datauri from awips.ffg where datauri='"
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
