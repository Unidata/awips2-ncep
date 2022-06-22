package gov.noaa.nws.ncep.common.dataplugin.atcf.dao;

import com.raytheon.edex.db.dao.DefaultPluginDao;
import com.raytheon.uf.common.dataplugin.PluginException;

/**
 * Set of DAO methods for ATCF data.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#     Engineer    Description
 * ------------ ----------- ----------- --------------------------
 * 06/23/10     283         F. J. Yen   Initial Coding
 * 10/05/2016   R22939      bhebbard    Adapted to ATCF
 * Jun 22, 2022 8865        mapeters    Extend DefaultPluginDao, remove unused methods
 * </pre>
 *
 * @author fjyen
 **/
public class AtcfDao extends DefaultPluginDao {

    /**
     * Creates a new AtcfDao
     *
     * @throws PluginException
     */
    public AtcfDao(String pluginName) throws PluginException {
        super(pluginName);
    }
}
