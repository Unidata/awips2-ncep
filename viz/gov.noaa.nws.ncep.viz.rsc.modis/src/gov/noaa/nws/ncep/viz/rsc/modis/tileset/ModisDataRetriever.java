/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 * 
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 * 
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 * 
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package gov.noaa.nws.ncep.viz.rsc.modis.tileset;

import gov.noaa.nws.ncep.common.dataplugin.modis.ModisRecord;

import java.awt.Rectangle;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Map;

import javax.measure.unit.Unit;

import com.raytheon.uf.common.colormap.image.ColorMapData;
import com.raytheon.uf.common.colormap.image.ColorMapData.ColorMapDataType;
import com.raytheon.uf.common.dataplugin.satellite.units.generic.GenericPixel;
import com.raytheon.uf.common.datastorage.DataStoreFactory;
import com.raytheon.uf.common.datastorage.Request;
import com.raytheon.uf.common.datastorage.records.ByteDataRecord;
import com.raytheon.uf.common.datastorage.records.FloatDataRecord;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.datastorage.records.ShortDataRecord;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.data.IColorMapDataRetrievalCallback;
import com.raytheon.uf.viz.datacube.DataCubeContainer;
import com.raytheon.uf.common.inventory.exception.DataCubeException;

/**
 * {@link IColorMapDataRetrievalCallback} for MODIS satellite imagery data.
 * Supports signed and unsigned byte and short data
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 12/23/2014   5116       kbugenhagen Initial creation
 * 
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1.0
 */
public class ModisDataRetriever implements IColorMapDataRetrievalCallback {
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(ModisDataRetriever.class);

    protected Rectangle datasetBounds;

    protected ModisRecord record;

    protected String dataset;

    private int level;

    public ModisDataRetriever(ModisRecord record, int level,
            Rectangle dataSetBounds) {
        this.record = record;
        this.datasetBounds = dataSetBounds;
        this.level = level;
        dataset = DataStoreFactory.createDataSetName(null,
                ModisRecord.SAT_DATASET_NAME, level);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.viz.core.data.IDataRetrievalCallback#getData()
     */
    @Override
    public ColorMapData getColorMapData() {
        Buffer data = null;
        Unit<?> dataUnit = Unit.ONE;
        boolean signed = false;
        Request req = Request.buildSlab(new int[] { this.datasetBounds.x,
                this.datasetBounds.y }, new int[] {
                this.datasetBounds.x + this.datasetBounds.width,
                this.datasetBounds.y + this.datasetBounds.height });
        IDataRecord[] dataRecord = null;
        IDataRecord rawData = null;
        try {
            dataRecord = DataCubeContainer.getDataRecord(record, req,
                    ModisRecord.getDataSet(level));
            if (dataRecord != null && dataRecord.length == 1) {
                rawData = dataRecord[0];
                if (rawData instanceof ByteDataRecord) {
                    data = ByteBuffer.wrap((byte[]) rawData.getDataObject());
                } else if (rawData instanceof ShortDataRecord) {
                    data = ShortBuffer.wrap((short[]) rawData.getDataObject());
                } else if (rawData instanceof FloatDataRecord) {
                    data = FloatBuffer.wrap((float[]) rawData.getDataObject());
                }
                Unit<?> recordUnit = getRecordUnit(this.record);
                signed = recordUnit instanceof GenericPixel;
                dataUnit = getDataUnit(recordUnit, rawData);
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
     * @param record2
     * @return
     */
    public static Unit<?> getRecordUnit(ModisRecord record) {
        Unit<?> recordUnit = new GenericPixel();

        return recordUnit;
    }

    /**
     * Extracts the data units for the data record given the PDO's base unit
     * 
     * @param recordUnit
     * @param dataRecord
     * @return
     */
    private static Unit<?> getDataUnit(Unit<?> recordUnit,
            IDataRecord dataRecord) {
        Unit<?> units = recordUnit != null ? recordUnit : Unit.ONE;
        Map<String, Object> attrs = dataRecord.getDataAttributes();
        if (attrs != null) {
            Number offset = (Number) attrs.get(ModisRecord.OFFSET_ID);
            Number scale = (Number) attrs.get(ModisRecord.SCALE_ID);

            if (offset != null) {
                double offsetVal = offset.doubleValue();
                if (offsetVal != 0.0) {
                    units = units.plus(offsetVal);
                }
            }
            if (scale != null) {
                double scaleVal = scale.doubleValue();
                if (scaleVal != 0.0 && scaleVal != 1.0) {
                    units = units.times(scaleVal);
                }
            }
        }
        return units;
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
        ModisDataRetriever other = (ModisDataRetriever) obj;
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
