package gov.noaa.nws.ncep.edex.plugin.nonconvsigmet.dao;

import com.raytheon.edex.db.dao.DefaultPluginDao;
import com.raytheon.uf.common.dataplugin.PluginException;

/**
 * Set of Data Access Object methods for non-convective SIGMET data.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 23, 2009            jkorman     Initial creation
 * 09/2011                 Chin Chen   changed to improve purge performance and
 *                                     removed xml serialization as well
 * Dec 14, 2016 5934       njensen     Moved to edex nonconvsigmet plugin
 * Jun 22, 2022 8865       mapeters    Extend DefaultPluginDao, remove unused methods
 * </pre>
 *
 * @author jkorman
 */

public class NonConvSigmetDao extends DefaultPluginDao {

    /**
     * Creates a new ReccoDao
     *
     * @throws PluginException
     */
    public NonConvSigmetDao(String pluginName) throws PluginException {
        super(pluginName);
    }
}
