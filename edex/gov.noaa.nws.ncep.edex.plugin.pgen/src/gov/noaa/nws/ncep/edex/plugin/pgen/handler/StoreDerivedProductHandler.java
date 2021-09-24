package gov.noaa.nws.ncep.edex.plugin.pgen.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.common.datastorage.IDataStore.StoreOp;
import com.raytheon.uf.common.datastorage.records.ByteDataRecord;
import com.raytheon.uf.common.datastorage.records.DataUriMetadataIdentifier;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.datastorage.records.StringDataRecord;
import com.raytheon.uf.common.serialization.comm.IRequestHandler;
import com.raytheon.uf.edex.database.plugin.PluginDao;
import com.raytheon.uf.edex.database.plugin.PluginFactory;

import gov.noaa.nws.ncep.common.dataplugin.pgen.DerivedProduct;
import gov.noaa.nws.ncep.common.dataplugin.pgen.PgenRecord;
import gov.noaa.nws.ncep.common.dataplugin.pgen.ResponseMessageValidate;
import gov.noaa.nws.ncep.common.dataplugin.pgen.request.StoreDerivedProductRequest;

/**
 *
 * Handler for StoreDerivedProductRequest. Stores a new, or overwrites an
 * existing, derived product for a given PGEN Activity
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 22, 2013            sgilbert    Initial creation
 * Sep 23, 2021 8608       mapeters    Pass metadata ids to datastore
 *
 * </pre>
 *
 * @author sgilbert
 */
public class StoreDerivedProductHandler
        implements IRequestHandler<StoreDerivedProductRequest> {

    private static final Logger logger = Logger
            .getLogger(StoreDerivedProductHandler.class.toString());

    private static final String PGEN = "pgen";

    private static final String DATASET_TYPE = "TYPE";

    @Override
    public ResponseMessageValidate handleRequest(
            StoreDerivedProductRequest request) throws Exception {

        Map<String, Object> dataAttributes = null;
        ResponseMessageValidate response = new ResponseMessageValidate();

        PluginDao dao = PluginFactory.getInstance().getPluginDao(PGEN);
        PgenRecord record = new PgenRecord(request.getDataURI());
        IDataStore dataStore = dao.getDataStore(record);

        for (DerivedProduct prod : request.getProductList()) {
            IDataRecord dataset;
            if (prod.getProduct() instanceof String) {
                dataset = new StringDataRecord(prod.getName(),
                        request.getDataURI(),
                        new String[] { (String) prod.getProduct() });
                dataAttributes = new HashMap<>();
                dataAttributes.put(DATASET_TYPE, prod.getProductType());
                dataset.setDataAttributes(dataAttributes);
            } else if (prod.getProduct() instanceof byte[]) {
                dataset = new ByteDataRecord(prod.getName(),
                        request.getDataURI(), (byte[]) prod.getProduct());
                dataAttributes = new HashMap<>();
                dataAttributes.put(DATASET_TYPE, prod.getProductType());
                dataset.setDataAttributes(dataAttributes);
            } else {
                StringBuilder sb = new StringBuilder(
                        "StoreDerivedProductRequest: Product type ");
                sb.append(prod.getProductType());
                sb.append(" not recognized for ");
                sb.append(prod.getName());
                logger.warning(sb.toString());
                continue;
            }
            dataStore.addDataRecord(dataset,
                    new DataUriMetadataIdentifier(record));
        }

        response.setDataURI(record.getDataURI());

        logger.info("StoreDerivedProductRequest for " + record.getDataURI()
                + " with " + request.getProductList().size() + " Datasets.");

        try {
            dataStore.store(StoreOp.REPLACE);
        } catch (Exception e) {
            response.setMessage(e.getMessage());
            response.setResult(Boolean.FALSE);
            return response;
        }

        response.setResult(Boolean.TRUE);

        return response;
    }
}
