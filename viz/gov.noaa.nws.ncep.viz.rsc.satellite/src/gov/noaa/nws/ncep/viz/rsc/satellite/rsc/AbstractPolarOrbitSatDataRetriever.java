package gov.noaa.nws.ncep.viz.rsc.satellite.rsc;

import java.awt.Rectangle;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import com.raytheon.uf.common.colormap.image.ColorMapData;
import com.raytheon.uf.common.colormap.image.ColorMapData.ColorMapDataType;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.persist.IPersistable;
import com.raytheon.uf.common.datastorage.Request;
import com.raytheon.uf.common.datastorage.records.ByteDataRecord;
import com.raytheon.uf.common.datastorage.records.FloatDataRecord;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.datastorage.records.ShortDataRecord;
import com.raytheon.uf.common.inventory.exception.DataCubeException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.data.IColorMapDataRetrievalCallback;
import com.raytheon.uf.viz.datacube.DataCubeContainer;

/**
 * Retriever class {@link IColorMapDataRetrievalCallback} for polar-orbiting
 * (MODIS/VIIRS) satellite imagery data. Supports signed and unsigned byte and
 * short data
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer     Description
 * ------------ ---------- -----------  --------------------------
 *  06/01/2016   R18511     kbugenhagen  Initial creation.
 * 
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1.0
 * @param <R>
 *            data record for resource
 */
public abstract class AbstractPolarOrbitSatDataRetriever<R extends IPersistable>
        implements IColorMapDataRetrievalCallback {
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(AbstractPolarOrbitSatDataRetriever.class);

    protected Rectangle datasetBounds;

    protected R record;

    protected String dataset;

    protected int level;

    public AbstractPolarOrbitSatDataRetriever(R record, int level,
            Rectangle dataSetBounds) {
        this.record = record;
        this.datasetBounds = dataSetBounds;
        this.level = level;
        dataset = getSatelliteDatasetName();
    }

    public abstract String getSatelliteDatasetName();

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.viz.core.data.IDataRetrievalCallback#getData()
     */
    @Override
    public ColorMapData getColorMapData() {
        Buffer data = null;
        boolean signed = false;
        Request req = Request.buildSlab(new int[] { this.datasetBounds.x,
                this.datasetBounds.y }, new int[] {
                this.datasetBounds.x + this.datasetBounds.width,
                this.datasetBounds.y + this.datasetBounds.height });
        IDataRecord[] dataRecord = null;
        IDataRecord rawData = null;
        try {
            dataRecord = DataCubeContainer.getDataRecord(
                    ((PluginDataObject) record), req, getDataSet(level));
            if (dataRecord != null && dataRecord.length == 1) {
                rawData = dataRecord[0];
                if (rawData instanceof ByteDataRecord) {
                    data = ByteBuffer.wrap((byte[]) rawData.getDataObject());
                } else if (rawData instanceof ShortDataRecord) {
                    data = ShortBuffer.wrap((short[]) rawData.getDataObject());
                } else if (rawData instanceof FloatDataRecord) {
                    data = FloatBuffer.wrap((float[]) rawData.getDataObject());
                }
            }
        } catch (DataCubeException e) {
            statusHandler.handle(Priority.SIGNIFICANT,
                    "Error retrieving satellite data", e);
        }

        if (data == null) {
            return null;
        }

        ColorMapDataType dataType = null;
        if (data instanceof ByteBuffer) {
            dataType = signed ? ColorMapDataType.SIGNED_BYTE
                    : ColorMapDataType.BYTE;
        } else if (data instanceof ShortBuffer) {
            dataType = signed ? ColorMapDataType.SHORT
                    : ColorMapDataType.UNSIGNED_SHORT;
        } else {
            dataType = ColorMapData.getDataType(data);
        }

        return new ColorMapData(data, new int[] { datasetBounds.width,
                datasetBounds.height }, dataType);
    }

    /**
     * Get the name of the dataset for the level
     * 
     * @param level
     * @return
     */
    public static String getDataSet(int level) {
        return "Data-" + level;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dataset == null) ? 0 : dataset.hashCode());
        result = prime * result
                + ((datasetBounds == null) ? 0 : datasetBounds.hashCode());
        result = prime * result + ((record == null) ? 0 : record.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        @SuppressWarnings("unchecked")
        AbstractPolarOrbitSatDataRetriever<R> other = (AbstractPolarOrbitSatDataRetriever<R>) obj;
        if (dataset == null) {
            if (other.dataset != null)
                return false;
        } else if (!dataset.equals(other.dataset))
            return false;
        if (datasetBounds == null) {
            if (other.datasetBounds != null)
                return false;
        } else if (!datasetBounds.equals(other.datasetBounds))
            return false;
        if (record == null) {
            if (other.record != null)
                return false;
        } else if (!record.equals(other.record))
            return false;
        return true;
    }

}
