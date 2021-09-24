package gov.noaa.nws.ncep.edex.plugin.pgen.dao;

import com.raytheon.uf.common.dataplugin.PluginException;
import com.raytheon.uf.common.dataplugin.persist.IPersistable;
import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.common.datastorage.StorageProperties;
import com.raytheon.uf.common.datastorage.records.AbstractStorageRecord;
import com.raytheon.uf.common.datastorage.records.DataUriMetadataIdentifier;
import com.raytheon.uf.common.datastorage.records.StringDataRecord;
import com.raytheon.uf.edex.database.plugin.PluginDao;

import gov.noaa.nws.ncep.common.dataplugin.pgen.PgenRecord;

/**
 *
 * PluginDao for the pgen data plugin
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
public class PgenDao extends PluginDao {

    public PgenDao(String pluginName) throws PluginException {
        super(pluginName);
    }

    @Override
    protected IDataStore populateDataStore(IDataStore dataStore,
            IPersistable obj) throws Exception {

        AbstractStorageRecord storageRecord = null;
        PgenRecord record = (PgenRecord) obj;

        storageRecord = new StringDataRecord(PgenRecord.ACTIVITY_XML,
                record.getDataURI(), new String[] { record.getActivityXML() });

        StorageProperties props = new StorageProperties();

        storageRecord.setProperties(props);
        storageRecord.setCorrelationObject(record);
        dataStore.addDataRecord(storageRecord,
                new DataUriMetadataIdentifier(record));

        return dataStore;
    }
}
