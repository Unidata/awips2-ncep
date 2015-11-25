/**
 * This is a Data Access Object (DAO) driver to interact with McIDAS image 
 * properties (satellite name and image type) and HDF5 data store.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    	Engineer    Description
 * ------------ ---------- 	----------- --------------------------
 * 10/2009		144			T. Lee		Created
 * 11/2009		144			T. Lee		Implemented area name DAO
 * Nov 14, 2013 2393        bclement    added in-java interpolation
 * Mar 07, 2014 2791        bsteffen    Move Data Source/Destination to numeric
 *                                      plugin.
 * 08/15/2015   R7190      R. Reynolds  Modifications to handle mcidas area header info.

 * </pre>
 * 
 * @author tlee
 * @version 1.0
 */

package gov.noaa.nws.ncep.edex.plugin.mcidas.dao;

import gov.noaa.nws.ncep.common.dataplugin.mcidas.McidasMapCoverage;
import gov.noaa.nws.ncep.common.dataplugin.mcidas.McidasRecord;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Calendar;
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
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.geospatial.interpolation.GridDownscaler;
import com.raytheon.uf.common.numeric.buffer.BufferWrapper;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.edex.database.plugin.DownscaleStoreUtil;
import com.raytheon.uf.edex.database.plugin.DownscaleStoreUtil.IDataRecordCreator;
import com.raytheon.uf.edex.database.plugin.PluginDao;

public class McidasDao extends PluginDao {

    public McidasDao(String pluginName) throws PluginException {
        super(pluginName);
    }

    @Override
    protected IDataStore populateDataStore(IDataStore dataStore,
            IPersistable record) throws StorageException {
        final McidasRecord satRecord = (McidasRecord) record;

        /*
         * Write McIDAS Area file header block.
         */
        if (satRecord.getHeaderBlock() != null) {
            AbstractStorageRecord storageRecord = new ByteDataRecord("Header",
                    satRecord.getDataURI(),
                    (byte[]) satRecord.getHeaderBlock(), 1,
                    new long[] { satRecord.getHeaderBlock().length });
            storageRecord.setCorrelationObject(satRecord);
            dataStore.addDataRecord(storageRecord);
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
                                + satRecord, storageRecord, e);
            }
            GridDownscaler downScaler = new GridDownscaler(gridGeom);

            storageRecord.setProperties(props);
            storageRecord.setCorrelationObject(satRecord);
            // Store the base record.
            dataStore.addDataRecord(storageRecord);

            BufferWrapper dataSource = BufferWrapper.wrapArray(
                    storageRecord.getDataObject(), xdim, ydim);
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
                            long[] sizes = new long[] { size.width, size.height };
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

                    });
        }
        return dataStore;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.edex.database.plugin.PluginDao#purgeAllData()
     */
    @Override
    public void purgeAllData() throws PluginException {
        super.purgeAllData();
        purgeSpatialData(true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.edex.database.plugin.PluginDao#purgeExpiredData()
     */
    @Override
    public void purgeExpiredData() throws PluginException {
        super.purgeExpiredData();
        purgeSpatialData(false);
    }

    /**
     * Deletes all mcidas_spatial metadata which doesn't have spatial data and
     * older than 4 hours if all == false
     */
    private void purgeSpatialData(boolean all) {
        List<Object> args = new ArrayList<Object>(3);
        args.add(McidasMapCoverage.class.getAnnotation(Table.class).name());
        args.add(McidasRecord.class.getAnnotation(Table.class).name());
        String formatString = "delete from %s where gid not in (select distinct coverage_gid from %s)";
        if (all == false) {
            formatString += " and reftime < '%s'";
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.HOUR, -4);
            args.add(new DataTime(cal.getTime()));
        }

        this.executeSQLUpdate(String.format(formatString,
                args.toArray(new Object[args.size()])));
    }

}