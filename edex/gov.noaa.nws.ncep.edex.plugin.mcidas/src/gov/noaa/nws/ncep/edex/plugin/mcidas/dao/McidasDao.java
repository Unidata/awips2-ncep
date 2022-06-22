package gov.noaa.nws.ncep.edex.plugin.mcidas.dao;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Table;

import org.geotools.coverage.grid.GridGeometry2D;

import com.raytheon.uf.common.dataplugin.PluginException;
import com.raytheon.uf.common.dataplugin.persist.IPersistable;
import com.raytheon.uf.common.datastorage.DataStoreFactory;
import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.common.datastorage.StorageException;
import com.raytheon.uf.common.datastorage.StorageProperties;
import com.raytheon.uf.common.datastorage.records.AbstractStorageRecord;
import com.raytheon.uf.common.datastorage.records.ByteDataRecord;
import com.raytheon.uf.common.datastorage.records.DataUriMetadataIdentifier;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.datastorage.records.IMetadataIdentifier;
import com.raytheon.uf.common.geospatial.interpolation.GridDownscaler;
import com.raytheon.uf.common.numeric.buffer.BufferWrapper;
import com.raytheon.uf.edex.database.plugin.DownscaleStoreUtil;
import com.raytheon.uf.edex.database.plugin.DownscaleStoreUtil.IDataRecordCreator;
import com.raytheon.uf.edex.database.plugin.PluginDao;

import gov.noaa.nws.ncep.common.dataplugin.mcidas.McidasMapCoverage;
import gov.noaa.nws.ncep.common.dataplugin.mcidas.McidasRecord;

/**
 * This is a Data Access Object (DAO) driver to interact with McIDAS image
 * properties (satellite name and image type) and HDF5 data store.
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 10/2009      144         T. Lee      Created
 * 11/2009      144         T. Lee      Implemented area name DAO
 * Nov 14, 2013 2393        bclement    added in-java interpolation
 * Mar 07, 2014 2791        bsteffen    Move Data Source/Destination to numeric
 *                                      plugin.
 * 08/15/2015   R7190       R. Reynolds Modifications to handle mcidas area header info.
 * 12/03/2015   R13119      R. Reynolds Enable purging of mcidas area files metadata.
 * Sep 23, 2021 8608        mapeters    Pass metadata ids to datastore
 * Jun 22, 2022 8865        mapeters    Update populateDataStore to return boolean
 *
 * </pre>
 *
 * @author tlee
 */
public class McidasDao extends PluginDao {

    public McidasDao(String pluginName) throws PluginException {
        super(pluginName);
    }

    @Override
    protected boolean populateDataStore(IDataStore dataStore,
            IPersistable record) throws StorageException {
        boolean populated = false;
        final McidasRecord satRecord = (McidasRecord) record;

        /*
         * Write McIDAS Area file header block.
         */
        IMetadataIdentifier metaId = new DataUriMetadataIdentifier(satRecord);
        if (satRecord.getHeaderBlock() != null) {
            AbstractStorageRecord storageRecord = new ByteDataRecord("Header",
                    satRecord.getDataURI(), satRecord.getHeaderBlock(), 1,
                    new long[] { satRecord.getHeaderBlock().length });
            storageRecord.setCorrelationObject(satRecord);
            dataStore.addDataRecord(storageRecord, metaId);
            populated = true;
        }

        /*
         * Write McIDAS image data block to HDF5.
         */
        if (satRecord.getMessageData() != null) {
            McidasMapCoverage coverage = satRecord.getCoverage();
            int xdim = coverage.getNx();
            int ydim = coverage.getNy();
            long[] sizes = new long[] { xdim, ydim };
            AbstractStorageRecord storageRecord = new ByteDataRecord(
                    DataStoreFactory.DEF_DATASET_NAME, satRecord.getDataURI(),
                    (byte[]) satRecord.getMessageData(), 2, sizes);

            final StorageProperties props = new StorageProperties();

            GridGeometry2D gridGeom;
            try {
                gridGeom = coverage.getGridGeometry();
            } catch (Exception e) {
                throw new StorageException(
                        "Unable to create grid geometry for record: "
                                + satRecord,
                        storageRecord, e);
            }
            GridDownscaler downScaler = new GridDownscaler(gridGeom);

            storageRecord.setProperties(props);
            storageRecord.setCorrelationObject(satRecord);
            // Store the base record.
            dataStore.addDataRecord(storageRecord, metaId);
            populated = true;

            BufferWrapper dataSource = BufferWrapper
                    .wrapArray(storageRecord.getDataObject(), xdim, ydim);
            // this way of interpolating does not create the Data-interpolated/0
            // link to the full sized data. This shouldn't be an issue since the
            // retrieval code checks for level 0 and requests the full sized
            // data.
            DownscaleStoreUtil.storeInterpolated(dataStore, downScaler,
                    dataSource, new IDataRecordCreator() {

                        @Override
                        public IDataRecord create(Object data,
                                int downScaleLevel, Rectangle size)
                                throws StorageException {
                            long[] sizes = new long[] { size.width,
                                    size.height };
                            String group = DataStoreFactory.createGroupName(
                                    satRecord.getDataURI(), null, true);
                            String name = String.valueOf(downScaleLevel);
                            IDataRecord rval = DataStoreFactory
                                    .createStorageRecord(name, group, data, 2,
                                            sizes);
                            rval.setProperties(props);
                            rval.setCorrelationObject(satRecord);
                            return rval;
                        }

                        @Override
                        public double getFillValue() {
                            return Double.NaN;
                        }

                        @Override
                        public boolean isSigned() {
                            return false;
                        }

                    }, metaId);
        }
        return populated;
    }

    @Override
    public void purgeAllData() throws PluginException {
        super.purgeAllData();
        purgeSpatialData();
    }

    @Override
    public void purgeExpiredData() throws PluginException {
        super.purgeExpiredData();
        purgeSpatialData();
    }

    /**
     * Deletes all mcidas_spatial metadata which doesn't have spatial data and
     * older than 2 days
     */
    private void purgeSpatialData() {
        List<Object> args = new ArrayList<>(3);
        args.add(McidasMapCoverage.class.getAnnotation(Table.class).name());
        args.add(McidasRecord.class.getAnnotation(Table.class).name());
        String formatString = "delete from %s where pid not in (select distinct coverage_pid from %s)";

        this.executeSQLUpdate(String.format(formatString,
                args.toArray(new Object[args.size()])));
    }

}