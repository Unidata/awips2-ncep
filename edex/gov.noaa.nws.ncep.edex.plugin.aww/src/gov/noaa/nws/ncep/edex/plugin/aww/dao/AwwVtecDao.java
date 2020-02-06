package gov.noaa.nws.ncep.edex.plugin.aww.dao;

import java.util.Calendar;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import com.raytheon.uf.common.dataquery.db.QueryResult;
import com.raytheon.uf.edex.database.DataAccessLayerException;
import com.raytheon.uf.edex.database.dao.CoreDao;
import com.raytheon.uf.edex.database.dao.DaoConfig;

import gov.noaa.nws.ncep.common.dataplugin.aww.AwwVtec;

/**
 * Dao for AwwVtec.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * ????          ????     ????      Initial creation.
 * Jun 19, 2015  4500     rjpeter   Removed SQL Injection Concern.
 * Sep 17, 2019  7928     randerso  Changed to use non-deprecated hibernate
 *                                  query methods.
 *
 * </pre>
 *
 * @author rjpeter
 */
public class AwwVtecDao extends CoreDao {

    public AwwVtecDao() {
        this(DaoConfig.forClass(AwwVtec.class));
    }

    /**
     * @param config
     */
    public AwwVtecDao(DaoConfig config) {
        super(config);
    }

    /**
     * Returns QueryResult object containing AwwVtec iformation in the database.
     * The detail information depends on individual SQL query
     * 
     * @param nativeSQLQuery
     *
     * @return the list of subscriptions
     */
    public synchronized QueryResult getQueryResultByNativeSQLQuery(
            String nativeSQLQuery) {
        QueryResult queryResult = null;
        try {
            queryResult = executeSelectNativeSqlQuery(nativeSQLQuery);
        } catch (ClassCastException cce) {
            /*
             * do nothing now, if a logger is configured, we will add log
             * message here If the ClassCastException is thrown, it means there
             * are no results returned
             */
        } catch (DataAccessLayerException dalc) {
            /*
             * do nothing now, if a logger is configured, we will add log
             * message here
             */
        }
        return queryResult;
    }

    /**
     * Executes a native SQL statement. This method completely bypasses
     * Hibernate and uses JDBC directly
     *
     * @param sql
     *            The sql string
     * @return A QueryResult Object
     * @throws DataAccessLayerException
     *             If the statement fails
     */
    public QueryResult executeSelectNativeSqlQuery(String selectSQLQuery)
            throws DataAccessLayerException {
        return executeMappedSQLQuery(selectSQLQuery);
    }

    public void populateAwwVtecEventStartTimeWithValidValue(
            final Calendar validEventStartTime, final String productClass,
            final String officeId, final String phenomena,
            final String significance, final String eventTrackingNumber)
            throws DataAccessLayerException {
        txTemplate.execute(new TransactionCallback<Integer>() {
            @Override
            public Integer doInTransaction(TransactionStatus status) {
                StringBuilder queryString = new StringBuilder(160);
                queryString.append(
                        "update AwwVtec set eventStartTime = :eventStartTime where");
                queryString.append(" eventStartTime is null");
                queryString.append(
                        " and eventTrackingNumber = :eventTrackingNumber");
                queryString.append(" and officeID = :officeID");
                queryString.append(" and phenomena = :phenomena");
                queryString.append(" and productClass = :productClass");
                queryString.append(" and significance = :significance");

                Session sess = getCurrentSession();
                Query<?> query = sess.createQuery(queryString.toString());
                query.setParameter("eventStartTime", validEventStartTime);
                query.setParameter("eventTrackingNumber", eventTrackingNumber);
                query.setParameter("officeID", officeId);
                query.setParameter("phenomena", phenomena);
                query.setParameter("productClass", productClass);
                query.setParameter("significance", significance);

                return query.executeUpdate();
            }
        });
    }
}
