package gov.noaa.nws.ncep.viz.rsc.satellite.rsc;

import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.measure.unit.Unit;

import org.geotools.coverage.grid.GridGeometry2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.raytheon.uf.common.colormap.prefs.ColorMapParameters;
import com.raytheon.uf.common.dataplugin.HDF5Util;
import com.raytheon.uf.common.dataplugin.persist.IPersistable;
import com.raytheon.uf.common.datastorage.DataStoreFactory;
import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.common.datastorage.records.FloatDataRecord;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.capabilities.ColorMapCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.ImagingCapability;
import com.raytheon.uf.viz.core.tile.Tile;
import com.raytheon.uf.viz.core.tile.TileSetRenderable;

import gov.noaa.nws.ncep.common.dataplugin.modis.ModisRecord;
import gov.noaa.nws.ncep.common.dataplugin.modis.dao.ModisDao;
import gov.noaa.nws.ncep.viz.resources.AbstractFrameData;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceDefinition;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceDefnsMngr;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceName;

/**
 * Class for display of the MODIS satellite data. Also provides the capability
 * to generate a GeoTIFF image from the MODIS image.
 * 
 * <pre>
 * 
 *  SOFTWARE HISTORY
 * 
 *  Date         Ticket#    Engineer     Description
 *  ------------ --------   -----------  --------------------------
 *  05/17/2016   R18511     kbugenhagen  Refactored to use a common base class
 *  01/30/2017   R17933     R Reynolds   Build legendString from DB query parameters
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1
 */
public class ModisSatResource
        extends AbstractPolarOrbitSatResource<ModisRecord> {

    public static final String MODIS_LEGEND_NAME = "MODIS";

    /**
     * Map for data records to renderable data, synchronized on for painting,
     * disposing, adding, and removing
     */
    protected Map<IPersistable, RecordData> dataRecordMap;

    public ModisSatResource(SatelliteResourceData data, LoadProperties props) {
        super(data, props);
        dataRecordMap = new LinkedHashMap<>();
        resourceData = data;
        legendStr = createLegendString(null);

    }

    private class ModisDataRetriever
            extends AbstractPolarOrbitSatDataRetriever<ModisRecord> {

        public ModisDataRetriever(ModisRecord record, int level,
                Rectangle dataSetBounds) {
            super(record, level, dataSetBounds);
        }

        /*
         * (non-Javadoc)
         * 
         * @see gov.noaa.nws.ncep.viz.rsc.satellite.rsc.
         * AbstractPolarOrbitSatDataRetriever#getSatelliteDatasetName()
         */
        @Override
        public String getSatelliteDatasetName() {
            return DataStoreFactory.createDataSetName(null,
                    ModisRecord.SAT_DATASET_NAME, level);
        }

    }

    private class ModisTileImageCreator extends SatTileImageCreator {

        protected ModisTileImageCreator(ModisRecord record) {
            super(record);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * gov.noaa.nws.ncep.viz.rsc.satellite.rsc.AbstractPolarOrbitSatResource
         * . SatTileImageCreator#getDataRetriever(com.raytheon.uf.viz.core.tile.
         * Tile )
         */
        @Override
        protected ModisDataRetriever getDataRetriever(Tile tile) {
            return new ModisDataRetriever(record, tile.tileLevel,
                    tile.getRectangle());
        }
    }

    protected class RecordData
            extends AbstractPolarOrbitSatResource<ModisRecord>.RecordData {

        private final static int TILE_SIZE = 2048;

        private ModisTileImageCreator imageCreator;

        public RecordData(ModisRecord dataRecord) {
            super(dataRecord);
            float[] latitudes = dataRecord.getCoverage().getLatitudes();
            float[] longitudes = dataRecord.getCoverage().getLongitudes();
            final GridGeometry2D geom2D = dataRecord.getCoverage()
                    .getGridGeometry(latitudes, longitudes);
            this.imageCreator = new ModisTileImageCreator(dataRecord);
            this.tileSet = new TileSetRenderable(
                    getCapability(ImagingCapability.class), geom2D,
                    imageCreator, dataRecord.getLevels(), TILE_SIZE);
            this.resolution = Math.min(dataRecord.getCoverage().getDx(),
                    dataRecord.getCoverage().getDy()) * INTERSECTION_FACTOR;
            this.project = true;
            this.record = dataRecord;
        }

    }

    protected class FrameData
            extends AbstractPolarOrbitSatResource<ModisRecord>.FrameData {

        protected FrameData(DataTime time, int interval) {
            super(time, interval);
        }

        @Override
        public void setLegendForFrame(ModisRecord rec) {
            name = MODIS_LEGEND_NAME;
            DataTime dateTime = rec.getDataTime();
            String refTime = dateTime.getDisplayString().split("\\[")[0];
            String[] timeParts = refTime.split(":");

            legendStr = createLegendString(rec) + " " + timeParts[0] + ":"
                    + timeParts[1];

        }
    }

    @Override
    public String createLegendString(IPersistable rec) {

        ModisRecord modisSatRec = (ModisRecord) rec;
        String satellite = "";
        String resolution = "";
        String legendStr = "";
        String rd = "";
        String channel = "";

        try {

            ResourceName rscName = resourceData.getResourceName();

            Map<String, String> recordElements = new HashMap<>();

            ResourceDefnsMngr rscDefnsMngr = ResourceDefnsMngr.getInstance();

            ResourceDefinition rscDefn = rscDefnsMngr
                    .getResourceDefinition(rscName.getRscType());

            HashMap<String, String> attributes = rscDefnsMngr
                    .getAttrSet(rscName).getAttributes();

            String legendStringAttribute = attributes
                    .get(LEGEND_STRING_ATTRIBUTE_NAME);

            if (legendStringAttribute == null
                    || legendStringAttribute.trim().isEmpty()) {
                legendStringAttribute = DEFAULT_LEGEND_STRING_ATTRIBUTE;
            }

            if (rec != null) {

                satellite = modisSatRec.getCreatingEntity();

                resolution = "(" + "DX:"
                        + modisSatRec.getCoverage().getDx().toString() + ","
                        + "DY:" + modisSatRec.getCoverage().getDy() + ")";

                recordElements.put(SATELLITE, satellite);

                recordElements.put(RESOLUTION, resolution);
            }

            channel = resourceData.getRscAttrSet().getRscAttrSetName();

            rd = rscDefn.getResourceDefnName();

            recordElements.put(RESOURCE_DEFINITION, rd);

            recordElements.put(CHANNEL, channel);

            legendStr = constructCustomLegendString(recordElements,
                    legendStringAttribute);

        } catch (Exception ex) {
            statusHandler.handle(Priority.ERROR,
                    "Error in MODIS building legend string ",
                    ex.getStackTrace().toString());
        }
        return legendStr;
    }

    /**
     * Add a data record to be displayed.
     * 
     * @param dataRecord
     *            Modis data record
     * @throws VizException
     */
    @Override
    public RecordData addRecord(ModisRecord dataRecord) throws VizException {
        RecordData recordData = null;
        getLatLons(dataRecord);
        createColorMap(dataRecord);
        RecordData data = dataRecordMap.get(dataRecord);
        if (data == null) {
            recordData = new RecordData(dataRecord);
            dataRecordMap.put(dataRecord, recordData);
        } else {
            recordData = data;
        }
        return recordData;
    }

    /**
     * Read latitude and longitude arrays from HDF file. These are too big to
     * store in the database.
     * 
     * @param dataRecord
     *            Modis data record associated with latitudes and longitudes
     */
    private void getLatLons(ModisRecord dataRecord) {
        try {
            File hdf5File = HDF5Util.findHDF5Location(dataRecord);
            IDataStore dataStore = DataStoreFactory.getDataStore(hdf5File);
            IDataRecord[] dataRecords = dataStore
                    .retrieve(dataRecord.getDataURI());
            for (IDataRecord rec : dataRecords) {
                if (rec instanceof FloatDataRecord) {
                    if (rec.getName().equals(ModisDao.LATITUDE_DATASET_NAME)) {
                        dataRecord.getCoverage().setLatitudes(
                                ((FloatDataRecord) rec).getFloatData());
                    } else if (rec.getName()
                            .equals(ModisDao.LONGITUDE_DATASET_NAME)) {
                        dataRecord.getCoverage().setLongitudes(
                                ((FloatDataRecord) rec).getFloatData());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * Generates a geoTIFF file from the modis spatial coverage.
     * 
     * @see
     * gov.noaa.nws.ncep.viz.rsc.satellite.rsc.AbstractPolarOrbitSatResource
     * #createGeoTiff
     * (gov.noaa.nws.ncep.viz.rsc.satellite.rsc.AbstractPolarOrbitSatResource
     * .RecordData)
     */
    @Override
    protected void createGeoTiff(
            AbstractPolarOrbitSatResource<ModisRecord>.RecordData recordData) {
        ColorMapParameters colorMapParameters = getCapability(
                ColorMapCapability.class).getColorMapParameters();
        ModisSatGeoTiffCreator geoTiffCreator = new ModisSatGeoTiffCreator(
                recordData.getRecord(), colorMapParameters, getName());
        geoTiffCreator.create();
    }

    /*
     * (non-Javadoc)
     * 
     * @see gov.noaa.nws.ncep.viz.rsc.satellite.rsc.NcSatelliteResource#
     * createNewFrame (com.raytheon.uf.common.time.DataTime, int)
     */
    @Override
    protected AbstractFrameData createNewFrame(DataTime frameTime,
            int frameInterval) {
        return (AbstractFrameData) new FrameData(frameTime, frameInterval);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * gov.noaa.nws.ncep.viz.rsc.satellite.rsc.NcSatelliteResource#getName()
     */
    @Override
    public String getName() {
        FrameData currFrame = (FrameData) getCurrentFrame();

        if (currFrame != null) {
            name = currFrame.getLegendForFrame();
        }
        if (name == null || name.trim().isEmpty())
            name = createLegendString(null) + " -NO DATA";

        return name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * gov.noaa.nws.ncep.viz.rsc.satellite.rsc.AbstractPolarOrbitSatResource
     * #getUnits(com.raytheon.uf.common.dataplugin.persist.IPersistable)
     */
    @Override
    protected String getUnits(IPersistable record) {
        ColorMapParameters params = getCapability(ColorMapCapability.class)
                .getColorMapParameters();

        return params.getDisplayUnit().toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * gov.noaa.nws.ncep.viz.rsc.satellite.rsc.AbstractPolarOrbitSatResource
     * #getRecordUnit(com.raytheon.uf.common.dataplugin.persist.IPersistable)
     */
    @Override
    public Unit<?> getRecordUnit(IPersistable dataRecord) {
        return Unit.ONE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see gov.noaa.nws.ncep.viz.rsc.satellite.rsc.NcSatelliteResource#
     * getParameterList (com.raytheon.uf.common.dataplugin.persist.IPersistable)
     */
    @Override
    protected List<String> getParameterList(IPersistable pdo) {
        String paramStr = ((ModisRecord) pdo).getParameter();
        List<String> paramList = new ArrayList<>(0);
        paramList.add(paramStr);
        return paramList;
    }

    /*
     * (non-Javadoc) Converts a sampled value to its cyanobacterial index (CI)
     * value
     * 
     * @see gov.noaa.nws.ncep.viz.rsc.satellite.rsc.ModisViirsSatResource#
     * convertDataValue(double)
     */
    @Override
    protected double convertDataValue(double dataValue) {
        double ciValue = dataValue;
        if (dataValue != 0.0f && dataValue != 1.0f) {
            ciValue = (float) Math.pow(10, ((dataValue - 0.5) / 100.0 - 4.0));
        }
        return ciValue;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * gov.noaa.nws.ncep.viz.rsc.satellite.rsc.NcSatelliteResource#project(org
     * .opengis.referencing.crs.CoordinateReferenceSystem)
     */
    @Override
    public void project(CoordinateReferenceSystem crs) throws VizException {
        synchronized (dataRecordMap) {
            try {
                for (RecordData data : dataRecordMap.values()) {
                    data.project();
                }
            } catch (Exception e) {
                statusHandler.error("Error reprojecting MODIS data", e);
            }
        }
    }

}