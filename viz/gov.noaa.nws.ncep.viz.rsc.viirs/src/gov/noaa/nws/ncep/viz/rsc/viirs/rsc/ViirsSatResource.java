package gov.noaa.nws.ncep.viz.rsc.viirs.rsc;

import java.awt.Rectangle;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.measure.converter.UnitConverter;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;

import org.geotools.coverage.grid.GeneralGridGeometry;
import org.geotools.coverage.grid.GridGeometry2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.raytheon.uf.common.colormap.prefs.ColorMapParameters;
import com.raytheon.uf.common.dataplugin.npp.viirs.VIIRSDataRecord;
import com.raytheon.uf.common.dataplugin.persist.IPersistable;
import com.raytheon.uf.common.datastorage.DataStoreFactory;
import com.raytheon.uf.common.datastorage.Request;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.style.StyleException;
import com.raytheon.uf.common.style.StyleManager;
import com.raytheon.uf.common.style.StyleManager.StyleType;
import com.raytheon.uf.common.style.StyleRule;
import com.raytheon.uf.common.style.image.DataScale;
import com.raytheon.uf.common.style.image.ImagePreferences;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.DrawableImage;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.RasterMode;
import com.raytheon.uf.viz.core.IMesh;
import com.raytheon.uf.viz.core.PixelCoverage;
import com.raytheon.uf.viz.core.drawables.IImage;
import com.raytheon.uf.viz.core.drawables.ext.colormap.IColormappedImageExtension;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.IMapMeshExtension;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.capabilities.ColorMapCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.ImagingCapability;
import com.raytheon.uf.viz.core.tile.Tile;
import com.raytheon.uf.viz.core.tile.TileSetRenderable;
import com.raytheon.uf.viz.core.tile.TileSetRenderable.TileImageCreator;
import com.raytheon.uf.viz.datacube.DataCubeContainer;
import com.raytheon.uf.viz.npp.viirs.style.VIIRSDataRecordCriteria;

import gov.noaa.nws.ncep.viz.resources.AbstractFrameData;
import gov.noaa.nws.ncep.viz.rsc.satellite.rsc.AbstractPolarOrbitSatDataRetriever;
import gov.noaa.nws.ncep.viz.rsc.satellite.rsc.AbstractPolarOrbitSatResource;

/**
 * Class for display of the Viirs satellite data.
 * 
 * <pre>
 * 
 *  SOFTWARE HISTORY
 * 
 *  Date         Ticket#    Engineer     Description
 *  ------------ --------   -----------  --------------------------
 *  05/17/2016   R18511     kbugenhagen  Refactored to use a common base class.
 *                                       This should be in the satellite project,
 *                                       along with Gini, Modis, Mcidas, etc.,
 *                                       but was moved back to this project
 *                                       due to an RPM cyclical build dependency
 *                                       on the npp viirs project and nsharp.
 *  07/01/2016   R17376     kbugenhagen  Updated to use colormap name in 
 *                                       attribute set file.  Removed overloaded
 *                                       loadColorMapParameters method.
 *  02/14/2017   R21492     kbugenhagen  Added call to suppress "Change Colormap"
 *                                       menu item in setColorMapUnits method.
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1
 */
public class ViirsSatResource
        extends AbstractPolarOrbitSatResource<VIIRSDataRecord> {

    /**
     * Map for data records to renderable data, synchronized on for painting,
     * disposing, adding, and removing
     */
    protected Map<IPersistable, RecordData> dataRecordMap;

    public static final String SAT_DATASET_NAME = DataStoreFactory.DEF_DATASET_NAME;

    public static final String VIIRS_LEGEND_NAME = "NPP VIIRS";

    public ViirsSatResource(ViirsResourceData data, LoadProperties props) {
        super(data, props);
        resourceData = data;
        dataRecordMap = new LinkedHashMap<>();
    }

    private class ViirsDataRetriever
            extends AbstractPolarOrbitSatDataRetriever<VIIRSDataRecord> {

        public ViirsDataRetriever(VIIRSDataRecord record, int level,
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
            return DataStoreFactory.createDataSetName(null, SAT_DATASET_NAME,
                    level);
        }

    }

    private class ViirsTileImageCreator implements TileImageCreator {

        private VIIRSDataRecord record;

        private ViirsTileImageCreator(VIIRSDataRecord record) {
            this.record = record;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * com.raytheon.uf.viz.core.tile.TileSetRenderable.TileImageCreator#
         * createTileImage(com.raytheon.uf.viz.core.IGraphicsTarget,
         * com.raytheon.uf.viz.core.tile.Tile,
         * org.geotools.coverage.grid.GeneralGridGeometry)
         */
        @Override
        public DrawableImage createTileImage(IGraphicsTarget target, Tile tile,
                GeneralGridGeometry targetGeometry) throws VizException {

            IImage image = target.getExtension(IColormappedImageExtension.class)
                    .initializeRaster(
                            new ViirsDataRetriever(record, tile.tileLevel,
                                    tile.getRectangle()),
                            getCapability(ColorMapCapability.class)
                                    .getColorMapParameters());
            IMesh mesh = target.getExtension(IMapMeshExtension.class)
                    .constructMesh(tile.tileGeometry, targetGeometry);
            return new DrawableImage(image, new PixelCoverage(mesh),
                    RasterMode.ASYNCHRONOUS);
        }
    }

    protected class RecordData
            extends AbstractPolarOrbitSatResource<VIIRSDataRecord>.RecordData {

        private final static int TILE_SIZE = 256;

        private ViirsTileImageCreator imageCreator;

        public RecordData(VIIRSDataRecord dataRecord) {
            super(dataRecord);
            final GridGeometry2D geom2D = dataRecord.getCoverage()
                    .getGridGeometry();
            this.imageCreator = new ViirsTileImageCreator(dataRecord);
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
            extends AbstractPolarOrbitSatResource<VIIRSDataRecord>.FrameData {

        protected FrameData(DataTime time, int interval) {
            super(time, interval);
        }

        @Override
        public void setLegendForFrame(VIIRSDataRecord rec) {
            name = VIIRS_LEGEND_NAME;
            DataTime dateTime = rec.getDataTime();
            String refTime = dateTime.getDisplayString().split("\\[")[0];
            String[] timeParts = refTime.split(":");
            StringBuilder builder = new StringBuilder(name);
            builder.append(" ");
            builder.append(rec.getParameter());
            builder.append(" ");
            builder.append(rec.getWavelength());
            builder.append(" ");
            builder.append(timeParts[0]);
            builder.append(":");
            builder.append(timeParts[1]);
            legendStr = builder.toString();
            name = legendStr;
        }
    }

    /**
     * Add a data record to be displayed.
     * 
     * @param dataRecord
     *            VIIRS data record
     * @throws VizException
     */
    @Override
    public RecordData addRecord(VIIRSDataRecord dataRecord)
            throws VizException {
        RecordData recordData = null;
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

    /*
     * (non-Javadoc)
     * 
     * @see gov.noaa.nws.ncep.viz.rsc.satellite.rsc.NcSatelliteResource#
     * getStyleRulePreferences
     * (com.raytheon.uf.common.dataplugin.persist.IPersistable)
     */
    @Override
    protected ImagePreferences getStyleRulePreferences(IPersistable dataRecord)
            throws VizException {
        StyleRule styleRule = null;
        try {
            styleRule = StyleManager.getInstance().getStyleRule(
                    StyleType.IMAGERY,
                    new VIIRSDataRecordCriteria((VIIRSDataRecord) dataRecord));
        } catch (StyleException e) {
            throw new VizException(e.getLocalizedMessage(), e);
        }
        ImagePreferences preferences = null;
        if (styleRule != null) {
            preferences = (ImagePreferences) styleRule.getPreferences();
        }
        return preferences;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * gov.noaa.nws.ncep.viz.rsc.satellite.rsc.AbstractPolarOrbitSatResource
     * #setColorMapUnits(com.raytheon.uf.common.dataplugin.persist.IPersistable,
     * com.raytheon.uf.common.colormap.prefs.ColorMapParameters)
     */
    @Override
    protected void setColorMapUnits(IPersistable record,
            ColorMapParameters colorMapParameters) throws StyleException {

        // Setup units for display and data
        Unit<?> displayUnit = Unit.ONE;
        if (imagePreferences != null) {
            if (imagePreferences.getDisplayUnits() != null) {
                displayUnit = imagePreferences.getDisplayUnits();
            }
        }
        Unit<?> dataUnit = Unit.ONE;

        try {
            IDataRecord dataRecord = DataCubeContainer.getDataRecord(
                    (VIIRSDataRecord) record,
                    Request.buildPointRequest(new java.awt.Point(0, 0)),
                    VIIRSDataRecord.getDataSet(0))[0];
            Map<String, Object> attrs = dataRecord.getDataAttributes();
            if (attrs != null) {
                Float offset = (Float) attrs.get(VIIRSDataRecord.OFFSET_ID);
                Float scale = (Float) attrs.get(VIIRSDataRecord.SCALE_ID);
                String unitStr = (String) attrs.get(VIIRSDataRecord.UNIT_ID);
                if (attrs.containsKey(VIIRSDataRecord.MISSING_VALUE_ID)) {
                    colorMapParameters.setNoDataValue(((Number) attrs
                            .get(VIIRSDataRecord.MISSING_VALUE_ID))
                                    .doubleValue());
                }
                if (unitStr != null) {
                    try {
                        dataUnit = UnitFormat.getUCUMInstance()
                                .parseObject(unitStr, new ParsePosition(0));
                    } catch (Exception e) {
                        statusHandler.error("Error parsinging dataUnit", e);
                    }
                }
                if (offset != null && offset != 0.0) {
                    dataUnit = dataUnit.plus(offset);
                }
                if (scale != null && scale != 0.0) {
                    dataUnit = dataUnit.times(scale);
                }
            }
        } catch (Exception e) {
            throw new StyleException(e);
        }

        colorMapParameters.setDataUnit(dataUnit);
        colorMapParameters.setColorMapUnit(dataUnit);
        colorMapParameters.setColorMapName(resourceData.getColorMapName());
        colorMapParameters.setDisplayUnit(displayUnit);

        colorMapParameters.setColorMapMin(colorMapParameters.getDataMin());
        colorMapParameters.setColorMapMax(colorMapParameters.getDataMax());
        if (imagePreferences != null) {
            DataScale scale = imagePreferences.getDataScale();
            if (scale != null) {
                UnitConverter displayToData = colorMapParameters
                        .getDisplayToDataConverter();
                UnitConverter displayToColorMap = colorMapParameters
                        .getDisplayToColorMapConverter();
                if (scale.getMinValue() != null) {
                    colorMapParameters.setColorMapMin((float) displayToColorMap
                            .convert(scale.getMinValue()));
                }
                if (scale.getMaxValue() != null) {
                    colorMapParameters.setColorMapMax((float) displayToColorMap
                            .convert(scale.getMaxValue()));
                }
                if (scale.getMinValue2() != null) {
                    colorMapParameters.setDataMin((float) displayToData
                            .convert(scale.getMinValue2()));
                }
                if (scale.getMaxValue2() != null) {
                    colorMapParameters.setDataMax((float) displayToData
                            .convert(scale.getMaxValue2()));
                }
            }
        }

        /*
         * suppresses the "Change colormap ..." menu item in legend right-click
         * drop down
         */
        getCapability(ColorMapCapability.class).setSuppressingMenuItems(true);

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
        if (name == null) {
            return VIIRS_LEGEND_NAME;
        }
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
        String paramStr = ((VIIRSDataRecord) pdo).getParameter();
        List<String> paramList = new ArrayList<>(0);
        paramList.add(paramStr);
        return paramList;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * gov.noaa.nws.ncep.viz.rsc.satellite.rsc.AbstractPolarOrbitSatResource
     * #convertDataValue(double)
     */
    @Override
    protected double convertDataValue(double dataValue) {
        return super.convertDataValue(dataValue);
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

    /*
     * Currently no support for VIIRS geoTIFF
     * 
     * (non-Javadoc)
     * 
     * @see
     * gov.noaa.nws.ncep.viz.rsc.satellite.rsc.AbstractPolarOrbitSatResource
     * #createGeoTiff
     * (gov.noaa.nws.ncep.viz.rsc.satellite.rsc.AbstractPolarOrbitSatResource
     * .RecordData)
     */
    @Override
    protected void createGeoTiff(
            AbstractPolarOrbitSatResource<VIIRSDataRecord>.RecordData recordData) {
    }

}