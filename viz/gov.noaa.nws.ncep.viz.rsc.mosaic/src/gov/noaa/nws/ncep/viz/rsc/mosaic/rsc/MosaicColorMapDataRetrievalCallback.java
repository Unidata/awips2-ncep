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
package gov.noaa.nws.ncep.viz.rsc.mosaic.rsc;

import gov.noaa.nws.ncep.edex.plugin.mosaic.common.MosaicRecord;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;

import com.raytheon.uf.common.colormap.image.ColorMapData;
import com.raytheon.uf.common.dataplugin.HDF5Util;
import com.raytheon.uf.common.datastorage.DataStoreFactory;
import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.common.datastorage.StorageException;
import com.raytheon.uf.viz.core.data.IColorMapDataRetrievalCallback;
import com.raytheon.uf.viz.core.exception.VizException;

/**
 * 
 * Converts a {@link MosaicRecord} to a {@link ColorMapData}.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Jun 15, 2016  R19647   bsteffen  Initial creation
 * 
 * </pre>
 * 
 * @author bsteffen
 */
public class MosaicColorMapDataRetrievalCallback implements
        IColorMapDataRetrievalCallback {

    private static final int FOUR_BIT_TO_EIGHT_BIT_MULTIPLIER = 16;

    private final MosaicRecord record;

    public MosaicColorMapDataRetrievalCallback(MosaicRecord record) {
        this.record = record;
    }

    @Override
    public ColorMapData getColorMapData() throws VizException {
        byte[] rawData = record.getRawData();
        if (rawData == null) {
            File loc = HDF5Util.findHDF5Location(record);
            IDataStore dataStore = DataStoreFactory.getDataStore(loc);
            try {
                record.retrieveFromDataStore(dataStore);
            } catch (FileNotFoundException | StorageException e) {
                throw new VizException(e);
            }
            rawData = record.getRawData();
        }
        int[] dimensions = { record.getNy(), record.getNx() };
        ByteBuffer buffer = ByteBuffer.wrap(rawData);
        if (record.isFourBit()) {
            ByteBuffer destBuffer = ByteBuffer.allocate(buffer.capacity());
            while (buffer.hasRemaining()) {
                destBuffer
                        .put((byte) (buffer.get() * FOUR_BIT_TO_EIGHT_BIT_MULTIPLIER));
            }
            destBuffer.rewind();
            buffer = destBuffer;
        }
        return new ColorMapData(buffer, dimensions);
    }

}
