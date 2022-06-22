package gov.noaa.nws.ncep.edex.common.dao;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

import com.raytheon.edex.db.dao.DefaultPluginDao;
import com.raytheon.uf.common.dataplugin.PluginException;

/**
 * Default Data Access Object for NCEP plugins.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 *                                     Initial creation
 * Jun 22, 2022 8865       mapeters    Extend DefaultPluginDao
 *
 * </pre>
 *
 * @author unknown
 */
public class NcepDefaultPluginDao extends DefaultPluginDao {

    protected List<String> tableClassNameList = new ArrayList<>();

    public List<String> getTableClassName() {
        return tableClassNameList;
    }

    public void setTableClassName(List<String> tableClassNamelst) {
        this.tableClassNameList = tableClassNamelst;
    }

    public NcepDefaultPluginDao(String pluginName) throws PluginException {
        super(pluginName);
    }

    /**
     * Purges all metadata for the owning plugin from the database
     *
     * @param date
     *            The data cutoff date
     * @return The number of items deleted
     */
    protected int purgeAllTables() {
        int results = 0;
        logger.info("purging..." + this.pluginName);
        if (this.daoClass == null) {
            logger.info("No record class specified for " + this.pluginName
                    + " plugin. EDEX will not purge data for " + this.pluginName
                    + " plugin");
        } else {
            try (Session s = getSession()) {
                Transaction tx = s.beginTransaction();
                try {

                    String queryString = "delete " + daoClass.getSimpleName()
                            + " x";
                    logger.info("query..." + queryString);
                    Query query = s.createQuery(queryString);
                    results = query.executeUpdate();
                    tx.commit();

                    s.flush();
                } catch (Throwable t) {
                    logger.error("Error purging " + pluginName + "/" + daoClass,
                            t);
                    tx.rollback();
                }
            }
        }
        return results;
    }
}
