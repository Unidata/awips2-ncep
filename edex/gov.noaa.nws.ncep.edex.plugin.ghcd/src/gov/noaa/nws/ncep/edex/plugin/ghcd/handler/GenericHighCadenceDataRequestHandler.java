/**
 * This code has unlimited rights, and is provided "as is" by the National Centers 
 * for Environmental Prediction, without warranty of any kind, either expressed or implied, 
 * including but not limited to the implied warranties of merchantability and/or fitness 
 * for a particular purpose.
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 */
package gov.noaa.nws.ncep.edex.plugin.ghcd.handler;

import gov.noaa.nws.ncep.common.dataplugin.ghcd.GenericHighCadenceDataConstants;
import gov.noaa.nws.ncep.common.dataplugin.ghcd.dao.GenericHighCadenceDataDao;
import gov.noaa.nws.ncep.common.dataplugin.ghcd.product.GenericHighCadenceDataContainer;
import gov.noaa.nws.ncep.common.dataplugin.ghcd.product.GenericHighCadenceDataItem;
import gov.noaa.nws.ncep.common.dataplugin.ghcd.product.GenericHighCadenceDataTypeInfo;
import gov.noaa.nws.ncep.common.dataplugin.ghcd.query.GenericHighCadenceDataReqMsg;
import gov.noaa.nws.ncep.common.dataplugin.ghcd.query.GenericHighCadenceDataReqMsg.GenericHighCadenceDataReqType;

import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.raytheon.edex.exception.DecoderException;
import com.raytheon.uf.common.dataplugin.PluginException;
import com.raytheon.uf.common.serialization.comm.IRequestHandler;

/**
 * Handler class for GenericHighCadenceDataReqMsg.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#     Engineer    Description
 * -------      -------     --------    -----------
 * 01/22/2014   1100        sgurung     Initial creation
 * 07/15/2014   1100        sgurung     Modified handleRequest() to include GenericHighCadenceDataTypeInfo.datatype
 * 
 * 
 * </pre>
 * 
 * @author sgurung
 * @version 1.0
 */

public class GenericHighCadenceDataRequestHandler implements
        IRequestHandler<GenericHighCadenceDataReqMsg> {

    private GenericHighCadenceDataDao ghcdDao;

    public GenericHighCadenceDataRequestHandler() throws DecoderException {
        super();
        try {
            ghcdDao = new GenericHighCadenceDataDao(
                    GenericHighCadenceDataConstants.PLUGIN_NAME);

        } catch (PluginException e) {
            e.printStackTrace();
        }
    }

    private Object createXmlFileString(Class<?> targetClass,
            Object targetInstance) {
        try {
            JAXBContext ctx;
            ctx = JAXBContext.newInstance(targetClass);
            if (ctx != null) {

                // This block to to generate XML
                Marshaller mar = ctx.createMarshaller();
                if (mar != null) {
                    mar.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
                            Boolean.TRUE);
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    mar.marshal(targetInstance, os);
                    return (Object) (os.toString());
                }
            }
        } catch (JAXBException e) {
            e.printStackTrace();

        }
        return null;
    }

    @Override
    public Object handleRequest(GenericHighCadenceDataReqMsg request)
            throws Exception {
        if (ghcdDao == null)
            return null;
        GenericHighCadenceDataReqType msgType = request.getReqType();
        String instrument = request.getInstrument();
        String datatype = request.getDatatype();
        String source = request.getSource();
        String dataResolUnits = request.getDataResolUnits();
        Integer dataResolVal = request.getDataResolVal();

        Date refTime;
        GenericHighCadenceDataContainer stnProd;
        switch (msgType) {
        case GET_GHCD_DATA_ITEMS:
            List<GenericHighCadenceDataItem> dataItems = ghcdDao
                    .getGhcdDataItems(request.getQueryTimeList(), source,
                            dataResolUnits, dataResolVal, instrument, datatype);
            if (dataItems != null) {
                return dataItems;
            }
            break;
        case GET_GHCD_TYPE_INFO_OBJECT:
        case GET_GHCD_TYPE_INFO_XML:
            if (instrument != null) {
                GenericHighCadenceDataTypeInfo prodInfo = ghcdDao
                        .getGhcdTypeInfo(source, instrument, datatype);
                if (prodInfo != null)
                    if (msgType
                            .equals(GenericHighCadenceDataReqType.GET_GHCD_TYPE_INFO_OBJECT))
                        return (Object) prodInfo;
                    else
                        return createXmlFileString(
                                GenericHighCadenceDataTypeInfo.class,
                                (Object) prodInfo);
            }
            break;

        case GET_GHCD_TYPE_ITEM_OBJECT:
        case GET_GHCD_DATA_ITEM_XML:
            refTime = request.getRefTime();
            stnProd = ghcdDao.getGhcdContainer(refTime,
            // GenericHighCadenceDataQueryKey.BY_SOURCE,
                    source, dataResolUnits, dataResolVal, instrument, datatype);
            if (stnProd != null)
                if (msgType
                        .equals(GenericHighCadenceDataReqType.GET_GHCD_TYPE_ITEM_OBJECT)) {
                    return (Object) stnProd;
                } else {
                    return createXmlFileString(
                            GenericHighCadenceDataContainer.class,
                            (Object) stnProd);
                }
            break;
        case GET_GHCD_TYPE_OBJECT:
        case GET_GHCD_TYPE_XML:
            refTime = request.getRefTime();
            GenericHighCadenceDataContainer pdCon = ghcdDao.getGhcdContainer(
                    refTime, // GenericHighCadenceDataQueryKey.BY_TYPE_NAME,
                    null, null, null, instrument, datatype);
            if (pdCon != null)
                if (msgType
                        .equals(GenericHighCadenceDataReqType.GET_GHCD_TYPE_OBJECT)) {
                    return (Object) pdCon;

                } else {
                    return createXmlFileString(
                            GenericHighCadenceDataContainer.class,
                            (Object) pdCon);
                }
            break;
        case PURGE_GHCD_EXPIRED_TYPE:
            ghcdDao.purgeExpiredData();
            break;
        case PURGE_GHCD_ALL_TYPES:
            ghcdDao.purgeAllData();
            break;
        case GET_GHCD_ALL_AVAILABLE_TYPES:
            return (Object) (ghcdDao.getGhcdAvailDataTypes(msgType));
        default:
            break;
        }
        return null;
    }
}
