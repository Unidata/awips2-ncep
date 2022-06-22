package gov.noaa.nws.ncep.edex.plugin.mosaic.common.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.PluginException;
import com.raytheon.uf.common.dataplugin.persist.IPersistable;
import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.common.datastorage.records.ByteDataRecord;
import com.raytheon.uf.common.datastorage.records.DataUriMetadataIdentifier;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.datastorage.records.IMetadataIdentifier;
import com.raytheon.uf.common.datastorage.records.ShortDataRecord;
import com.raytheon.uf.common.serialization.DynamicSerializationManager;
import com.raytheon.uf.common.serialization.DynamicSerializationManager.SerializationType;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.SerializationUtil;
import com.raytheon.uf.edex.database.plugin.PluginDao;
import com.raytheon.uf.edex.database.query.DatabaseQuery;

import gov.noaa.nws.ncep.edex.plugin.mosaic.common.MosaicRecord;
import gov.noaa.nws.ncep.edex.plugin.mosaic.util.MosaicConstants;
import gov.noaa.nws.ncep.edex.plugin.mosaic.util.level3.SymbologyBlock;

/**
 * Data Access Object implementation for accessing mosaic data
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 09/2009      143         L. Lin      Initial coding
 * Aug 30, 2013 2298        rjpeter     Make getPluginName abstract
 * Sep 23, 2021 8608        mapeters    Pass metadata ids to datastore
 * Jun 22, 2022 8865        mapeters    Update populateDataStore to return boolean
 * </pre>
 *
 * This code has been developed by the SIB for use in the AWIPS2 system.
 *
 * @author L. Lin
 */
public class MosaicDao extends PluginDao {

    /**
     * Creates a new mosaic dao
     *
     * @param pluginName
     *            "mosaic"
     * @throws PluginException
     *             If the dao cannot be initialized
     */
    public MosaicDao(String pluginName) throws PluginException {
        super(pluginName);
    }

    @Override
    protected boolean populateDataStore(IDataStore dataStore, IPersistable obj)
            throws Exception {
        boolean populated = false;

        MosaicRecord mosaicRec = (MosaicRecord) obj;
        IMetadataIdentifier metaId = new DataUriMetadataIdentifier(mosaicRec);
        if (mosaicRec.getHeaderBlock() != null) {
            IDataRecord rec = new ByteDataRecord("Header",
                    mosaicRec.getDataURI(), mosaicRec.getHeaderBlock(), 1,
                    new long[] { 120 });
            rec.setCorrelationObject(mosaicRec);
            dataStore.addDataRecord(rec, metaId);
            populated = true;
        }

        if (mosaicRec.getRawData() != null) {
            IDataRecord rec = new ByteDataRecord("Data", mosaicRec.getDataURI(),
                    mosaicRec.getRawData(), 2,
                    new long[] { mosaicRec.getNx(), mosaicRec.getNy() });
            rec.setCorrelationObject(mosaicRec);
            dataStore.addDataRecord(rec, metaId);
            populated = true;
        }

        if ((mosaicRec.getThresholds() != null)
                && (mosaicRec.getProductCode() != 2)) {
            IDataRecord rec = new ShortDataRecord("Thresholds",
                    mosaicRec.getDataURI(), mosaicRec.getThresholds(), 1,
                    new long[] { 16 });
            rec.setCorrelationObject(mosaicRec);
            dataStore.addDataRecord(rec, metaId);
            populated = true;
        }

        if (mosaicRec.getSymbologyBlock() != null) {
            byte[] data = DynamicSerializationManager
                    .getManager(SerializationType.Thrift)
                    .serialize(mosaicRec.getSymbologyBlock());
            ByteDataRecord bdr = new ByteDataRecord("Symbology",
                    mosaicRec.getDataURI(), data);
            dataStore.addDataRecord(bdr, metaId);
            populated = true;
        }

        if (mosaicRec.getProductDependentValues() != null) {
            IDataRecord rec = new ShortDataRecord("DependentValues",
                    mosaicRec.getDataURI(),
                    mosaicRec.getProductDependentValues(), 1, new long[] {
                            mosaicRec.getProductDependentValues().length });
            rec.setCorrelationObject(mosaicRec);
            dataStore.addDataRecord(rec, metaId);
            populated = true;
        }

        if (mosaicRec.getRecordVals().isEmpty()) {
            byte[] data = DynamicSerializationManager
                    .getManager(SerializationType.Thrift)
                    .serialize(mosaicRec.getRecordVals());
            ByteDataRecord bdr = new ByteDataRecord("RecordVals",
                    mosaicRec.getDataURI(), data);
            dataStore.addDataRecord(bdr, metaId);
            populated = true;
        }

        return populated;
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
                            "Error retrieving mosaic HDF5 data", e);
                }
                retVal.add(record);
            }
        }

        return retVal;
    }

    @Override
    public PluginDataObject[] getFullRecord(DatabaseQuery query, int tile)
            throws PluginException {
        PluginDataObject[] queryResults = getMetadata(query);

        for (PluginDataObject obj : queryResults) {
            MosaicRecord record = (MosaicRecord) obj;
            IDataRecord[] hdf5Data = getHDF5Data(record, tile);
            record.setMessageData(hdf5Data[0].getDataObject());
            record.setThresholds((short[]) hdf5Data[2].getDataObject());
            record.setProductDependentValues(
                    (short[]) hdf5Data[6].getDataObject());

            record.setProductVals(
                    (HashMap<MosaicConstants.MapValues, Map<String, Map<MosaicConstants.MapValues, String>>>) hdf5Data[5]
                            .getDataObject());
            try {
                record.setSymbologyBlock(
                        (SymbologyBlock) SerializationUtil.transformFromThrift(
                                (byte[]) hdf5Data[3].getDataObject()));
            } catch (SerializationException e) {
                throw new PluginException("Error deserializing symbology block",
                        e);
            }
        }
        return queryResults;
    }

}
