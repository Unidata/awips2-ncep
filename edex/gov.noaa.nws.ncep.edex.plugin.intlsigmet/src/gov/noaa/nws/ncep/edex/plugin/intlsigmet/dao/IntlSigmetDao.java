package gov.noaa.nws.ncep.edex.plugin.intlsigmet.dao;

import com.raytheon.edex.db.dao.DefaultPluginDao;
import com.raytheon.uf.common.dataplugin.PluginException;

/**
 * Set of DAO methods for International SIGMET data.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 20080103            384 jkorman     Initial Coding.
 * 09/2011                 Chin Chen   changed to improve purge performance and
 *                                     removed xml serialization as well
 * Dec 14, 2016 5934       njensen     Moved to edex intlsigmet plugin
 * Jun 22, 2022 8865       mapeters    Extend DefaultPluginDao, remove unused methods
 * </pre>
 *
 * @author jkorman
 */
public class IntlSigmetDao extends DefaultPluginDao {

    /**
     * Creates a new ReccoDao
     *
     * @throws PluginException
     */
    public IntlSigmetDao(String pluginName) throws PluginException {
        super(pluginName);
    }
}
