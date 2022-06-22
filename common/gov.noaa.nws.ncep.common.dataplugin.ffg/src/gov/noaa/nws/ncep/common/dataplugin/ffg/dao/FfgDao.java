package gov.noaa.nws.ncep.common.dataplugin.ffg.dao;

import com.raytheon.edex.db.dao.DefaultPluginDao;
import com.raytheon.uf.common.dataplugin.PluginException;

/**
 * FfgDao
 *
 * This java class performs data access for FFG.
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 05/2010      14          T. Lee      Migration to TO11DR11
 * 09/2011                  Chin Chen   changed to improve purge performance and
 *                                      removed xml serialization as well
 * Jun 22, 2022 8865        mapeters    Extend DefaultPluginDao, remove unused methods
 * </pre>
 *
 * @author T.Lee
 */
public class FfgDao extends DefaultPluginDao {

    /**
     * FfgDao constructor.
     *
     * @throws PluginException
     */
    public FfgDao(String pluginName) throws PluginException {
        super(pluginName);
    }
}
