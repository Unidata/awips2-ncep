package gov.noaa.nws.ncep.common.dataplugin.ncscat.dao;

import java.util.ArrayList;
import java.util.List;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.PluginException;
import com.raytheon.uf.common.dataplugin.persist.IPersistable;
import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.common.datastorage.StorageException;
import com.raytheon.uf.common.datastorage.records.ByteDataRecord;
import com.raytheon.uf.common.datastorage.records.DataUriMetadataIdentifier;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.edex.database.plugin.PluginDao;

import gov.noaa.nws.ncep.common.dataplugin.ncscat.NcscatRecord;

/**
 * NcscatDao
 *
 * This java class performs the dataaccess layer functionality to the HDF5 for
 * ASCAT,Quikscat
 *
 * <pre>
 *
 * HISTORY
 *
 * Date         Ticket#     Author      Description
 * ------------ ----------  ----------- --------------------------
 * 11/2009                  Uma Josyula Initial creation
 * Sep 23, 2021 8608        mapeters    Pass metadata ids to datastore
 *
 * </pre>
 *
 * This code has been developed by the SIB for use in the AWIPS2 system.
 */
public class NcscatDao extends PluginDao {

    public NcscatDao(String pluginName) throws PluginException {
        super(pluginName);
    }

    @Override
    protected IDataStore populateDataStore(IDataStore dataStore,
            IPersistable record) throws StorageException {

        NcscatRecord ncscatRecord = (NcscatRecord) record;
        int msgLength = (ncscatRecord.getConvertedMessage()).length;
        long nx, ny;

        nx = ncscatRecord.getRecordLength() * 2;
        if (msgLength % nx == 0) {
            ny = msgLength / nx;
        } else {
            ny = (msgLength / nx) + 1;

        }

        long[] sizes = new long[] { nx, ny };
        IDataRecord storageRecord = new ByteDataRecord("Ncscat",
                ncscatRecord.getDataURI(), ncscatRecord.getConvertedMessage(),
                2, sizes);

        storageRecord.setCorrelationObject(ncscatRecord);
        dataStore.addDataRecord(storageRecord,
                new DataUriMetadataIdentifier(ncscatRecord));

        return dataStore;
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
                            "Error retrieving ncscat HDF5 data", e);
                }
                retVal.add(record);
            }
        }

        return retVal;
    }

}
