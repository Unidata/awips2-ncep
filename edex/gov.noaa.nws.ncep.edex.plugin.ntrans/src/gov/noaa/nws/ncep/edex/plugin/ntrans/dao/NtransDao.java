package gov.noaa.nws.ncep.edex.plugin.ntrans.dao;

import java.util.ArrayList;
import java.util.List;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.PluginException;
import com.raytheon.uf.common.dataplugin.persist.IPersistable;
import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.common.datastorage.StorageException;
import com.raytheon.uf.common.datastorage.StorageProperties;
import com.raytheon.uf.common.datastorage.records.ByteDataRecord;
import com.raytheon.uf.common.datastorage.records.DataUriMetadataIdentifier;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.edex.database.plugin.PluginDao;

import gov.noaa.nws.ncep.common.dataplugin.ntrans.NtransRecord;

/**
 * NtransDao
 *
 * This java class performs the dataaccess layer functionality to the HDF5 for
 * ASCAT,Quikscat
 *
 * <pre>
 * HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 11/2009                 Uma Josyula Initial creation
 * Dec 14, 2016 5934       njensen     Moved to edex ntrans plugin
 * Sep 23, 2021 8608       mapeters    Pass metadata ids to datastore
 * Jun 22, 2022 8865       mapeters    Update populateDataStore to return boolean
 *
 * </pre>
 *
 * This code has been developed by the SIB for use in the AWIPS2 system.
 */
public class NtransDao extends PluginDao {

    public NtransDao(String pluginName) throws PluginException {
        super(pluginName);
    }

    @Override
    protected boolean populateDataStore(IDataStore dataStore,
            IPersistable record) throws StorageException {

        NtransRecord ntransRecord = (NtransRecord) record;
        IDataRecord imageDataRecord = new ByteDataRecord("NTRANS",
                ntransRecord.getDataURI(), ntransRecord.getImageData());

        StorageProperties props = new StorageProperties();

        imageDataRecord.setProperties(props);
        imageDataRecord.setCorrelationObject(ntransRecord);
        dataStore.addDataRecord(imageDataRecord,
                new DataUriMetadataIdentifier(ntransRecord));

        return true;
    }

    @Override
    public List<IDataRecord[]> getHDF5Data(List<PluginDataObject> objects,
            int tileSet) throws PluginException {
        List<IDataRecord[]> retVal = new ArrayList<>();

        for (PluginDataObject obj : objects) {
            IDataRecord[] record = null;
            if (obj instanceof IPersistable) {
                /* connect to the data store and retrieve the data */
                try {
                    record = getDataStore((IPersistable) obj)
                            .retrieve(obj.getDataURI());
                } catch (Exception e) {
                    throw new PluginException(
                            "Error retrieving NTRANS HDF5 data", e);
                }
                retVal.add(record);
            }
        }

        return retVal;
    }

}
