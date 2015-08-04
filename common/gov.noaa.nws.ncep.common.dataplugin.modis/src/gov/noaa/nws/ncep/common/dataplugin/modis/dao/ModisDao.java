package gov.noaa.nws.ncep.common.dataplugin.modis.dao;

import gov.noaa.nws.ncep.common.dataplugin.modis.ModisMessageData;
import gov.noaa.nws.ncep.common.dataplugin.modis.ModisRecord;
import gov.noaa.nws.ncep.common.dataplugin.modis.ModisSpatialCoverage;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Table;

import com.raytheon.uf.common.dataplugin.PluginException;
import com.raytheon.uf.common.dataplugin.persist.IPersistable;
import com.raytheon.uf.common.datastorage.DataStoreFactory;
import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.common.datastorage.StorageException;
import com.raytheon.uf.common.datastorage.StorageProperties;
import com.raytheon.uf.common.datastorage.records.AbstractStorageRecord;
import com.raytheon.uf.common.datastorage.records.FloatDataRecord;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.geospatial.interpolation.BilinearInterpolation;
import com.raytheon.uf.common.geospatial.interpolation.GridDownscaler;
import com.raytheon.uf.common.numeric.buffer.BufferWrapper;
import com.raytheon.uf.common.numeric.buffer.ShortBufferWrapper;
import com.raytheon.uf.common.numeric.dest.DataDestination;
import com.raytheon.uf.common.numeric.filter.InverseFillValueFilter;
import com.raytheon.uf.common.numeric.filter.UnsignedFilter;
import com.raytheon.uf.common.numeric.source.DataSource;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.edex.core.dataplugin.PluginRegistry;
import com.raytheon.uf.edex.database.plugin.DownscaleStoreUtil;
import com.raytheon.uf.edex.database.plugin.DownscaleStoreUtil.IDataRecordCreator;
import com.raytheon.uf.edex.database.plugin.PluginDao;

/**
 * Modis DAO - creates storage records from PDOs
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 1, 2014             kbugenhagen Initial creation
 * 
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1.0
 */

public class ModisDao extends PluginDao {

    public final static String LATITUDE_DATASET_NAME = "latitudes";

    public final static String LONGITUDE_DATASET_NAME = "longitudes";

    private Object latitudes;

    private Object longitudes;

    public ModisDao(String pluginName) throws PluginException {
        super(pluginName);
    }

    /*
     * (non-Javadoc)
     * 
     * Stores the image, latitude and longitude arrays in the HDF file. All
     * levels for the image are stored to support down-scaling.
     * 
     * @see
     * com.raytheon.uf.edex.database.plugin.PluginDao#populateDataStore(com.
     * raytheon.uf.common.datastorage.IDataStore,
     * com.raytheon.uf.common.dataplugin.persist.IPersistable)
     */
    @Override
    protected IDataStore populateDataStore(IDataStore dataStore,
            IPersistable obj) throws Exception {

        final ModisRecord record = (ModisRecord) obj;
        AbstractStorageRecord dataset = null;
        final ModisMessageData messageData = (ModisMessageData) record
                .getMessageData();
        float[] missingValues = messageData.getMissingValues();
        final float fillValue = missingValues[0];
        final StorageProperties props = new StorageProperties();
        String compression = PluginRegistry.getInstance()
                .getRegisteredObject(pluginName).getCompression();
        if (compression != null) {
            props.setCompression(StorageProperties.Compression
                    .valueOf(compression));
        }
        ModisSpatialCoverage spatialRecord = record.getCoverage();
        final int nx = spatialRecord.getNx();
        final int ny = spatialRecord.getNy();
        final long[] sizes = new long[] { nx, ny };

        IDataRecordCreator creator = new IDataRecordCreator() {
            @Override
            public IDataRecord create(Object data, int downScaleLevel,
                    Rectangle size) throws StorageException {
                long[] sizes = new long[] { size.width, size.height };
                IDataRecord idr = DataStoreFactory.createStorageRecord(
                        ModisRecord.getDataSet(downScaleLevel),
                        record.getDataURI(), data, 2, sizes);
                Map<String, Object> attributes = new HashMap<String, Object>();
                attributes.put(ModisRecord.MISSING_VALUE_ID, fillValue);
                attributes.put(ModisRecord.OFFSET_ID, messageData.getOffset());
                attributes.put(ModisRecord.SCALE_ID, messageData.getScale());
                if (messageData.getUnitString() != null) {
                    attributes.put(ModisRecord.UNIT_ID,
                            messageData.getUnitString());
                }
                idr.setDataAttributes(attributes);
                idr.setProperties(props);
                idr.setCorrelationObject(record);
                return idr;
            }

            @Override
            public double getFillValue() {
                return fillValue;
            }

            @Override
            public boolean isSigned() {
                // TODO Auto-generated method stub
                return false;
            }
        };

        Object rawData = messageData.getRawData();
        latitudes = messageData.getLatitudes();
        longitudes = messageData.getLongitudes();

        // create raw image dataset

        // first, add the full image array (level 0)
        IDataRecord fullSize = creator
                .create(rawData, 0, new Rectangle(nx, ny));
        dataStore.addDataRecord(fullSize);

        // Data sources are create anonymously here to avoid having the
        // fillValue/validMin/validMax even checked when getting values but
        // still getting the getDataValueInternal functionality
        BufferWrapper ds = BufferWrapper.wrapArray(rawData, nx, ny);

        DataDestination dest = InverseFillValueFilter.apply(
                (DataDestination) ds, fillValue);

        // Wrap the source and replace set each value which will replace
        // anything in missingValues with fillValue
        DataSource source = ds;
        if (ds instanceof ShortBufferWrapper) {
            source = UnsignedFilter.apply((ShortBufferWrapper) ds);
        }
        source = new ModisDataSourceWrapper(source, missingValues);
        for (int y = 0; y < ny; ++y) {
            for (int x = 0; x < nx; ++x) {
                dest.setDataValue(source.getDataValue(x, y), x, y);
            }
        }

        // now, add the down-scaled image arrays
        GridDownscaler downscaler = new GridDownscaler(
                spatialRecord.getGridGeometry(latitudes, longitudes),
                new BilinearInterpolation());

        DownscaleStoreUtil.storeInterpolated(dataStore, downscaler, ds,
                creator, false);

        // add latitude and longitude datasets
        dataset = new FloatDataRecord(LATITUDE_DATASET_NAME,
                record.getDataURI(), (float[]) latitudes, 2, sizes);
        dataStore.addDataRecord(dataset);
        dataset = new FloatDataRecord(LONGITUDE_DATASET_NAME,
                record.getDataURI(), (float[]) longitudes, 2, sizes);
        dataStore.addDataRecord(dataset);

        dataStore.store();

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
     * Deletes all Modis_spatial metadata which doesn't have spatial data and
     * older than 4 hours if all == false
     */
    private void purgeSpatialData(boolean all) {
        List<Object> args = new ArrayList<Object>(3);
        args.add(ModisSpatialCoverage.class.getAnnotation(Table.class).name());
        args.add(ModisRecord.class.getAnnotation(Table.class).name());
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

    public Object getLatitudes() {
        return latitudes;
    }

    public void setLatitudes(Object latitudes) {
        this.latitudes = latitudes;
    }

    public Object getLongitudes() {
        return longitudes;
    }

    public void setLongitudes(Object longitudes) {
        this.longitudes = longitudes;
    }

}
