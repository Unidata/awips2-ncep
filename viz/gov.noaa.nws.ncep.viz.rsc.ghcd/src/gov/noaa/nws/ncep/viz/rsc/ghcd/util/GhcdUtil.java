/**
 * This code has unlimited rights, and is provided "as is" by the National Centers 
 * for Environmental Prediction, without warranty of any kind, either expressed or implied, 
 * including but not limited to the implied warranties of merchantability and/or fitness 
 * for a particular purpose.
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 **/
package gov.noaa.nws.ncep.viz.rsc.ghcd.util;

import gov.noaa.nws.ncep.common.dataplugin.ghcd.product.GenericHighCadenceDataItem;
import gov.noaa.nws.ncep.common.dataplugin.ghcd.query.GenericHighCadenceDataReqMsg;
import gov.noaa.nws.ncep.common.dataplugin.ghcd.query.GenericHighCadenceDataReqMsg.GenericHighCadenceDataReqType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.requests.ThriftClient;

/**
 * Utility class for generic high cadence display (ghcd) plugin.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#    Engineer    Description
 * ------------  ---------- ----------- --------------------------
 * Sep 9, 2014    R4508       sgurung    Initial creation   
 * Mar 12, 2015   R6920       sgurung    Modified javaDoc for method getGhcdDataItems()
 * 
 * </pre>
 * 
 * @author sgurung
 * @version 1.0
 */

public class GhcdUtil {

    public static final String YSCALE_TYPE_LOG = "Log";

    public static final String YSCALE_TYPE_LINEAR = "Linear";

    public static final String TITLE_POSITION_LEFT = "Left";

    public static final String TITLE_POSITION_RIGHT = "Right";

    /**
     * 
     * Retrieves a list of GenericHighCadenceDataItem records
     * 
     * @param refTimeList
     *            The list of reference times
     * @param source
     *            The source
     * @param dataResolUnits
     *            The data resolution unit
     * @param dataResolVal
     *            The data resolution value
     * @param instrument
     *            The instrument
     * @param datatype
     *            The data type
     * @return List of GenericHighCadenceDataItem records
     * @throws VizException
     */
    public static List<GenericHighCadenceDataItem> getGhcdDataItems(
            List<Date> refTimes, String source, String instrument,
            String datatype, String dataResolUnits, Integer dataResolVal)
            throws VizException {

        GenericHighCadenceDataReqMsg req = new GenericHighCadenceDataReqMsg();
        req.setReqType(GenericHighCadenceDataReqType.GET_GHCD_DATA_ITEMS);
        req.setQueryTimeList(refTimes);
        req.setSource(source);
        req.setInstrument(instrument);
        req.setDatatype(datatype);
        req.setDataResolUnits(dataResolUnits);
        req.setDataResolVal(dataResolVal);
        List<GenericHighCadenceDataItem> rsltsList = new ArrayList<GenericHighCadenceDataItem>();
        try {
            Object rslts = ThriftClient.sendRequest(req);

            if (rslts != null && rslts instanceof List<?>) {
                rsltsList = (ArrayList<GenericHighCadenceDataItem>) rslts;
            }
        } catch (Exception e) {
            throw new VizException(
                    "Error during retrieval request of GHCD data items.", e);
        }

        return rsltsList;
    }

}