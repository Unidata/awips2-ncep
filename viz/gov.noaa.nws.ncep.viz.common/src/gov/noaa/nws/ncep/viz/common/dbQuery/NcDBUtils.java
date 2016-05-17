package gov.noaa.nws.ncep.viz.common.dbQuery;

import java.util.HashMap;
import java.util.Map;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.PluginException;
import com.raytheon.uf.common.dataplugin.annotations.DataURIUtil;
import com.raytheon.uf.common.dataquery.requests.DbQueryRequest;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.dataquery.responses.DbQueryResponse;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.requests.ThriftClient;

/**
 * Miscellaneous reusable data base methods
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 2, 2016  R15954     SRussell     Initial creation
 * 
 * </pre>
 * 
 * @author srussell
 * @version 1.0
 */

public class NcDBUtils {

    /**
     * Load a single Plugin Data Object(PDO) from a DataURI query
     * 
     * @param dataURI
     *            the data URI
     * @return the fully filled out Plugin Data Object
     * @throws VizException
     * 
     */
    public static PluginDataObject loadDataURI2PDO(String dataURI)
            throws VizException {

        Map<String, RequestConstraint> constraints = new HashMap<>();

        try {
            constraints
                    .putAll(RequestConstraint.toConstraintMapping(DataURIUtil
                            .createDataURIMap(dataURI)));
        } catch (PluginException e) {
            throw new VizException(e);
        }

        DbQueryRequest request = new DbQueryRequest(constraints);
        DbQueryResponse response = (DbQueryResponse) ThriftClient
                .sendRequest(request);
        PluginDataObject[] pdos = response
                .getEntityObjects(PluginDataObject.class);
        if (pdos.length == 0) {
            return null;
        }

        return pdos[0];
    }

}// end class NCDBUtils()
