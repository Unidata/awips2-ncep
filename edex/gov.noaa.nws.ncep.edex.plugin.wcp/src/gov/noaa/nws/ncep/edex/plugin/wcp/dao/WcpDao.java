package gov.noaa.nws.ncep.edex.plugin.wcp.dao;

import com.raytheon.edex.db.dao.DefaultPluginDao;
import com.raytheon.uf.common.dataplugin.PluginException;

/**
 * Set of DAO methods for WCP data.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#     Engineer    Description
 * ------------ ----------- ----------- --------------------------
 * 17May2010    37          F. J. Yen   Initial Coding (Following one of RTN's DAO to refactor)
 * 09/2011                  Chin Chen   changed to improve purge performance and
 *                                      removed xml serialization as well
 * Dec 14, 2016 5934        njensen     Moved to edex wcp plugin
 * Jun 22, 2022 8865        mapeters    Extend DefaultPluginDao, remove unused methods
 * </pre>
 *
 * @author fjyen
 **/
public class WcpDao extends DefaultPluginDao {

    /**
     * Creates a new WcpDao
     *
     * @throws PluginException
     */
    public WcpDao(String pluginName) throws PluginException {
        super(pluginName);
    }
}
