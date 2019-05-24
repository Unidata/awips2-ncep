
package gov.noaa.nws.ncep.edex.plugin.mosaic.uengine;

import javax.measure.UnitConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.coverage.grid.GridGeometry2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.datastorage.records.ByteDataRecord;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.datastorage.records.ShortDataRecord;

import gov.noaa.nws.ncep.edex.plugin.mosaic.common.MosaicRecord;
import tec.uom.se.AbstractConverter;

/**
 * Derived from original uEngine DecodeMosaicImage task.
 * 
 * <pre>
 * 
 * Date         Ticket#         Engineer        Description
 * ------------ ----------      -----------     --------------------------
 * 09/2009      143             L. Lin          Initial creation
 * 12/2009      143             mgamazaychikov  Added constructor for GEMPAK client;
 *                                              Changed the size of mosaicTiler from fixed
 *                                              to data-dependent
 * Dec 16, 2016 5934            njensen         Removed extending deprecated ScriptTask
 * </pre>
 * 
 * This code has been developed by the SIB for use in the AWIPS2 system.
 * 
 * @author L. Lin
 */

public class DecodeMosaicImage {

    private final Log theLogger = LogFactory.getLog(getClass());

    private MosaicRecord mosaicRecord;

    private MosaicTiler mosaicTiler;

    private GridGeometry2D geometry;

    public DecodeMosaicImage(PluginDataObject aDataRecord, IDataRecord[] records) {

        mosaicRecord = (MosaicRecord) aDataRecord;
        for (IDataRecord dataRecord : records) {
            if ("Data".equals(dataRecord.getName())) {
                mosaicRecord.setRawData(((ByteDataRecord) dataRecord)
                        .getByteData());
            } else if ("Thresholds".equals(dataRecord.getName())) {
                mosaicRecord.setThresholds(((ShortDataRecord) dataRecord)
                        .getShortData());
            }
        }
        int tileSize = 2 * mosaicRecord.getNx();
        mosaicTiler = new MosaicTiler(mosaicRecord, 1, tileSize);
        geometry = mosaicTiler.constructGridGeometry();
    }
    
    /*
     * This constructor differs from one above in that it specifies the identity 
     * data converter in the MosaicTiler constructor - meaning that the data are being 
     * read as they are stored in the HDF5 data repository.
     */
    public DecodeMosaicImage(PluginDataObject aDataRecord, IDataRecord[] records, String clientName) {

        mosaicRecord = (MosaicRecord) aDataRecord;
        for (IDataRecord dataRecord : records) {
            if ("Data".equals(dataRecord.getName())) {
                mosaicRecord.setRawData(((ByteDataRecord) dataRecord)
                        .getByteData());
            } else if ("Thresholds".equals(dataRecord.getName())) {
                mosaicRecord.setThresholds(((ShortDataRecord) dataRecord)
                        .getShortData());
            }
        }
        if ("GEMPAK".equals(clientName)) {
        	UnitConverter dataConverter = AbstractConverter.IDENTITY;
            int tileSize = 2 * mosaicRecord.getNx();
            mosaicTiler = new MosaicTiler(mosaicRecord, 1, tileSize, dataConverter);
        }
        else {
            int tileSize = 2 * mosaicRecord.getNx();
            mosaicTiler = new MosaicTiler(mosaicRecord, 1, tileSize);
        }
        geometry = mosaicTiler.constructGridGeometry();
    }

    public Object execute() {
        byte[] bi = null;
        // get the mosaic file
        bi = generateProduct();
        return bi;
    }

    private byte[] generateProduct() {
        long t0 = System.currentTimeMillis();
        byte[] image = mosaicTiler.createFullImage();
        long t = System.currentTimeMillis() - t0;
        theLogger.info("createFullImage: " + t + " ms");
        return image;
    }

    public GridGeometry2D getGridGeometry() {
        return geometry;
    }

    public CoordinateReferenceSystem getCrs() {
        return geometry.getEnvelope2D().getCoordinateReferenceSystem();
    }
}
