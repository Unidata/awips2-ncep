package gov.noaa.nws.ncep.edex.plugin.tcm.dao;

import com.raytheon.edex.db.dao.DefaultPluginDao;
import com.raytheon.uf.common.dataplugin.PluginException;

/**
 *
 * TcmDao
 *
 * This java class performs data access for TCM.
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 05/2010      128        T. Lee      Migration to TO11DR11
 * 09/2011                 Chin Chen   changed to improve purge performance and
 *                                     removed xml serialization as well
 * Dec 14, 2016 5934       njensen     Moved to edex tcm plugin
 * Jun 22, 2022 8865       mapeters    Extend DefaultPluginDao, remove unused methods
 * </pre>
 *
 * @author T.Lee
 */
public class TcmDao extends DefaultPluginDao {

    /**
     * TcmDao constructor.
     *
     * @throws PluginException
     */
    public TcmDao(String pluginName) throws PluginException {
        super(pluginName);
    }
}
