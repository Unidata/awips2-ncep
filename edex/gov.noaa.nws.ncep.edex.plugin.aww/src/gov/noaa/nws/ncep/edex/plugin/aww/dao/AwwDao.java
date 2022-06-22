package gov.noaa.nws.ncep.edex.plugin.aww.dao;

import com.raytheon.edex.db.dao.DefaultPluginDao;
import com.raytheon.uf.common.dataplugin.PluginException;

/**
 * Set of Data Access Object methods for AWW data.
 *
 * <pre>
 *
 * HISTORY
 *
 * Date         Ticket# Author      Description
 * ------------ ------- ----------- -------------------------------------
 * 05/2010              L. Lin      Initial creation
 * 09/2011              Chin Chen   changed to improve purge performance and
 *                                  removed xml serialization as well
 * Dec 19, 2016 5934    njensen     Moved to edex aww plugin
 * Jun 22, 2022 8865    mapeters    Extend DefaultPluginDao, remove unused methods
 *
 * This code has been developed by the SIB for use in the AWIPS2 system.
 *
 * </pre>
 */
public class AwwDao extends DefaultPluginDao {

    /**
     * Creates a new AwwDao
     *
     * @throws PluginException
     */
    public AwwDao(String pluginName) throws PluginException {
        super(pluginName);
    }
}
