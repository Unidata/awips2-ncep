package gov.noaa.nws.ncep.viz.rsc.modis.rsc;

import gov.noaa.nws.ncep.common.dataplugin.modis.ModisRecord;
import gov.noaa.nws.ncep.common.dataplugin.modis.dao.ModisDao;
import gov.noaa.nws.ncep.viz.common.ColorMapUtil;
import gov.noaa.nws.ncep.viz.localization.NcPathManager;
import gov.noaa.nws.ncep.viz.localization.NcPathManager.NcPathConstants;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.colorBar.ColorBarResource;
import gov.noaa.nws.ncep.viz.resources.colorBar.ColorBarResourceData;
import gov.noaa.nws.ncep.viz.rsc.modis.tileset.ModisDataRetriever;
import gov.noaa.nws.ncep.viz.ui.display.ColorBarFromColormap;
import gov.noaa.nws.ncep.viz.ui.display.NCMapDescriptor;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.measure.converter.UnitConverter;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;
import javax.xml.bind.JAXBException;

import org.geotools.coverage.grid.GeneralGridGeometry;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridCoverageWriter;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.raytheon.uf.common.colormap.ColorMap;
import com.raytheon.uf.common.colormap.image.ColorMapData;
import com.raytheon.uf.common.colormap.image.Colormapper;
import com.raytheon.uf.common.colormap.prefs.ColorMapParameters;
import com.raytheon.uf.common.dataplugin.HDF5Util;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.datastorage.DataStoreFactory;
import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.common.datastorage.Request;
import com.raytheon.uf.common.datastorage.records.FloatDataRecord;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.geospatial.util.EnvelopeIntersection;
import com.raytheon.uf.common.inventory.exception.DataCubeException;
import com.raytheon.uf.common.serialization.JAXBManager;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.jaxb.JAXBClassLocator;
import com.raytheon.uf.common.serialization.jaxb.JaxbDummyObject;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.style.AbstractStylePreferences;
import com.raytheon.uf.common.style.LabelingPreferences;
import com.raytheon.uf.common.style.MatchCriteria;
import com.raytheon.uf.common.style.ParamLevelMatchCriteria;
import com.raytheon.uf.common.style.StyleException;
import com.raytheon.uf.common.style.StyleRule;
import com.raytheon.uf.common.style.StyleRuleset;
import com.raytheon.uf.common.style.image.DataScale;
import com.raytheon.uf.common.style.image.ImagePreferences;
import com.raytheon.uf.common.style.level.Level;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.util.ArraysUtil;
import com.raytheon.uf.viz.core.DrawableImage;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.RasterMode;
import com.raytheon.uf.viz.core.IMesh;
import com.raytheon.uf.viz.core.PixelCoverage;
import com.raytheon.uf.viz.core.drawables.IImage;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.drawables.ext.IImagingExtension.ImageProvider;
import com.raytheon.uf.viz.core.drawables.ext.colormap.IColormappedImageExtension;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.IMapMeshExtension;
import com.raytheon.uf.viz.core.reflect.SubClassLocator;
import com.raytheon.uf.viz.core.rsc.IResourceDataChanged;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.ResourceProperties;
import com.raytheon.uf.viz.core.rsc.capabilities.ColorMapCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.ImagingCapability;
import com.raytheon.uf.viz.core.tile.Tile;
import com.raytheon.uf.viz.core.tile.TileSetRenderable;
import com.raytheon.uf.viz.core.tile.TileSetRenderable.TileImageCreator;
import com.raytheon.uf.viz.datacube.DataCubeContainer;
import com.vividsolutions.jts.algorithm.CGAlgorithms;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;

/**
 * 
 * Class for display of the MODIS satellite data. Also provides the capability
 * to generate a GeoTIFF image from the MODIS image.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer     Description
 * ------------ ---------- -----------  --------------------------
 * 10/01/2014   R5116      kbugenhagen  Initial creation.
 * 08/19/2015   R7270      kbugenhagen  Geotiff now displays correctly.
 * 08/31/2015   R7270      kbugenhagen  Updated to use Ben Steffensmeier's 
 *                                      createGeoTiff method.
 * 09/22/2015   R7270      kbugenhagen  Allow for separate geotiffs per 
 *                                      ModisRecord
 * 
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1.0
 */

public class ModisResource extends
        AbstractNatlCntrsResource<ModisResourceData, NCMapDescriptor> implements
        INatlCntrsResource, IResourceDataChanged, ImageProvider {

    static final char SLASH = File.separatorChar;

    static final String DEFAULT_COLORMAP_NAME = "colorMapName";

    private ModisResourceData modisResourceData;

    protected ResourcePair cbarRscPair;

    protected ColorBarResource cbarResource;

    private static JAXBManager jaxb;

    /**
     * Map for data records to renderable data, synchronized on for painting,
     * disposing, adding, and removing
     */
    private Map<ModisRecord, RecordData> dataRecordMap;

    private String name;

    private class ModisTileImageCreator implements TileImageCreator {

        private ModisRecord record;

        private ModisDataRetriever dataRetriever;

        private ModisTileImageCreator(ModisRecord record) {
            this.record = record;
        }

        @Override
        public DrawableImage createTileImage(IGraphicsTarget target, Tile tile,
                GeneralGridGeometry targetGeometry) throws VizException {

            this.dataRetriever = new ModisDataRetriever(record, tile.tileLevel,
                    tile.getRectangle());
            IImage image = target
                    .getExtension(IColormappedImageExtension.class)
                    .initializeRaster(
                            dataRetriever,
                            getCapability(ColorMapCapability.class)
                                    .getColorMapParameters());
            IMesh mesh = target.getExtension(IMapMeshExtension.class)
                    .constructMesh(tile.tileGeometry, targetGeometry);

            return new DrawableImage(image, new PixelCoverage(mesh),
                    RasterMode.ASYNCHRONOUS);
        }
    }

    /**
     * Every {@link ModisRecord} will have a corresponding RecordData object
     */
    private class RecordData {

        private final static double INTERSECTION_FACTOR = 10.0;

        private final static int TILE_SIZE = 2048;

        private ModisRecord record;

        private ModisTileImageCreator imageCreator;

        /** Intersection geometry for the target */
        private List<PreparedGeometry> targetIntersection;

        /** Flag designated if a project call is required next paint */
        private boolean project;

        /** Renderable for the data record */
        private TileSetRenderable tileSet;

        private double resolution;

        private boolean createdGeoTiff = false;

        public RecordData(ModisRecord dataRecord) {
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

        public Collection<DrawableImage> getImagesToRender(
                IGraphicsTarget target, PaintProperties paintProps)
                throws VizException {

            if (project) {
                projectInternal();
                project = false;
            }
            if (targetIntersection != null) {
                return tileSet.getImagesToRender(target, paintProps);
            } else {
                return Collections.emptyList();
            }
        }

        public void project() {
            this.project = true;
        }

        private void projectInternal() {
            GeneralGridGeometry targetGeometry = descriptor.getGridGeometry();
            if (tileSet.getTargetGeometry() != targetGeometry) {
                tileSet.project(targetGeometry);
                try {
                    Envelope tileSetEnvelope = tileSet.getTileSetGeometry()
                            .getEnvelope();
                    targetIntersection = null;
                    Geometry intersection = EnvelopeIntersection
                            .createEnvelopeIntersection(
                                    tileSetEnvelope,
                                    targetGeometry.getEnvelope(),
                                    resolution,
                                    (int) (tileSetEnvelope.getSpan(0) / (resolution * INTERSECTION_FACTOR)),
                                    (int) (tileSetEnvelope.getSpan(1) / (resolution * INTERSECTION_FACTOR)));
                    if (intersection != null) {
                        int numGeoms = intersection.getNumGeometries();
                        targetIntersection = new ArrayList<PreparedGeometry>(
                                numGeoms);
                        for (int n = 0; n < numGeoms; ++n) {
                            targetIntersection.add(PreparedGeometryFactory
                                    .prepare(intersection.getGeometryN(n)
                                            .buffer(resolution
                                                    * INTERSECTION_FACTOR)));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public void dispose() {
            if (tileSet != null) {
                tileSet.dispose();
                tileSet = null;
            }
            targetIntersection = null;
            System.out.println("disposed recorddata");
        }

        public ModisRecord getRecord() {
            return record;
        }

    }

    protected class FrameData extends AbstractFrameData {

        RecordData recordData;

        private String legendStr = "No Data";

        // Map of frametimes to RecordData records, which contain images.
        // Each ModisRecord that matches a frame's time is used to create a
        // RecordData object. RecordData objects are responsible for creating
        // the images (tilesets) that get grouped together for a frame.
        Map<DataTime, List<RecordData>> recordDataMap;

        public FrameData(DataTime frameTime, int timeInt) {
            super(frameTime, timeInt);
            recordDataMap = new HashMap<DataTime, List<RecordData>>();
            recordDataMap.put(frameTime, new ArrayList<RecordData>());
        }

        public boolean updateFrameData(IRscDataObject rscDataObj) {
            PluginDataObject pdo = ((DfltRecordRscDataObj) rscDataObj).getPDO();
            ModisRecord record = (ModisRecord) pdo;

            synchronized (this) {
                try {
                    boolean match = timeMatch(record.getDataTime()) > -1;
                    if (match) {
                        List<RecordData> recordDataList = recordDataMap
                                .get(frameTime);
                        // add record to map, if not already there
                        if (recordDataList != null) {
                            recordData = addRecord(record);
                            if (!recordDataList.contains(recordData)) {
                                recordDataList.add(recordData);
                                recordDataMap.put(frameTime, recordDataList);
                                setLegendForFrame(record);
                            }
                        }
                    } else {
                        return false;
                    }

                } catch (VizException e) {
                    statusHandler.handle(
                            Priority.PROBLEM,
                            "Error adding record from update: "
                                    + e.getLocalizedMessage(), e);

                }
            }
            return true;
        }

        public Map<DataTime, List<RecordData>> getRecordDataMap() {
            return recordDataMap;
        }

        public RecordData getRecordData() {
            return recordData;
        }

        public void setRecordData(RecordData recordData) {
            this.recordData = recordData;
        }

        public String getLegendStr() {
            return legendStr;
        }

        public void setLegendStr(String legendStr) {
            this.legendStr = legendStr;
        }

        public String getLegendForFrame() {
            return legendStr;
        }

        public void setLegendForFrame(ModisRecord rec) {
            name = "NPP MODIS";

            DataTime dateTime = rec.getDataTime();
            String refTime = dateTime.getDisplayString().split("\\[")[0];
            String[] timeParts = refTime.split(":");
            StringBuilder builder = new StringBuilder(name);
            builder.append(" ");
            builder.append(rec.getParameter());
            builder.append(" ");
            builder.append(timeParts[0]);
            builder.append(":");
            builder.append(timeParts[1]);
            legendStr = builder.toString();
        }

        public void dispose() {
            System.out.println("disposed frame");
            recordData.dispose();
            recordDataMap.clear();
        }
    }

    /**
     * Create a MODIS resource.
     * 
     * @throws VizException
     */
    public ModisResource(ModisResourceData resourceData,
            LoadProperties loadProperties) throws VizException {
        super(resourceData, loadProperties);
        dataRecordMap = new LinkedHashMap<ModisRecord, RecordData>();
        resourceData.addChangeListener(this);
        modisResourceData = (ModisResourceData) resourceData;
    }

    /**
     * Add a data record to be displayed.
     * 
     * @param dataRecord
     * @throws VizException
     */
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
     * Read latitude and longitude arrays from HDF file. Too big to store in
     * database.
     * 
     * @param dataRecord
     */
    public void getLatLons(ModisRecord dataRecord) {
        try {
            File hdf5File = HDF5Util.findHDF5Location(dataRecord);
            IDataStore dataStore = DataStoreFactory.getDataStore(hdf5File);
            IDataRecord[] dataRecords = dataStore.retrieve(dataRecord
                    .getDataURI());
            for (IDataRecord rec : dataRecords) {
                if (rec instanceof FloatDataRecord) {
                    if (rec.getName().equals(ModisDao.LATITUDE_DATASET_NAME)) {
                        dataRecord.getCoverage().setLatitudes(
                                ((FloatDataRecord) rec).getFloatData());
                    } else if (rec.getName().equals(
                            ModisDao.LONGITUDE_DATASET_NAME)) {
                        dataRecord.getCoverage().setLongitudes(
                                ((FloatDataRecord) rec).getFloatData());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Create color map from a record.
     * 
     * @param dataRecord
     *            MODIS data record
     * @throws VizException
     */
    protected void createColorMap(ModisRecord dataRecord) throws VizException {
        ImagePreferences preferences = getStyleRulePreferences(dataRecord);
        ColorMapParameters colorMapParameters = loadColorMapParameters(
                dataRecord, preferences);
        setColorBar(preferences, colorMapParameters);
        if (colorMapParameters.getPersisted() != null) {
            colorMapParameters.applyPersistedParameters(colorMapParameters
                    .getPersisted());
        }
        getCapability(ColorMapCapability.class).setColorMapParameters(
                colorMapParameters);
        resourceChanged(ChangeType.CAPABILITY,
                getCapability(ColorMapCapability.class));
    }

    /**
     * Retrieves style rule matching data record parameters
     * 
     * @param dataRecord
     *            MODIS data record
     * @return The preferences
     * @throws VizException
     */
    protected ImagePreferences getStyleRulePreferences(ModisRecord dataRecord)
            throws VizException {
        ImagePreferences preferences = null;
        String styleRuleFile = getLocFilePathForImageryStyleRule();
        File file = NcPathManager.getInstance().getStaticFile(styleRuleFile);
        StyleRule styleRule = null;
        List<String> paramList = getParameterList(dataRecord);
        ParamLevelMatchCriteria matchCriteria = new ParamLevelMatchCriteria();
        matchCriteria.setParameterName(paramList);
        try {
            StyleRuleset styleSet = (StyleRuleset) getJaxbManager()
                    .unmarshalFromXmlFile(StyleRuleset.class, file);
            if (styleSet != null) {
                List<StyleRule> styleRuleList = styleSet.getStyleRules();
                for (StyleRule sr : styleRuleList) {
                    MatchCriteria styleMatchCriteria = sr.getMatchCriteria();
                    if (styleMatchCriteria.matches(matchCriteria) > 0) {
                        styleRule = sr;
                        break;
                    }
                }
            }
        } catch (StyleException e) {
            throw new VizException(e.getLocalizedMessage(), e);
        } catch (SerializationException e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
        } catch (JAXBException e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
        }

        if (styleRule != null) {
            preferences = (ImagePreferences) styleRule.getPreferences();
        }

        return preferences;
    }

    /**
     * Loads color map parameters from preferences.
     * 
     * @param dataRecord
     *            MODIS data record
     * @param preferences
     *            Stylerule preferences
     * @return colormap parameters
     * @throws VizException
     */
    private ColorMapParameters loadColorMapParameters(ModisRecord dataRecord,
            ImagePreferences preferences) throws VizException {
        ColorMapParameters colorMapParameters = getCapability(
                ColorMapCapability.class).getColorMapParameters();
        if (colorMapParameters == null) {
            colorMapParameters = new ColorMapParameters();
        }
        if (colorMapParameters.getColorMap() == null) {
            String name = colorMapParameters.getColorMapName();
            if (name == null || name.equals(DEFAULT_COLORMAP_NAME)) {
                if (preferences != null) {
                    name = preferences.getDefaultColormap();
                }
                if (name == null || name.equals(DEFAULT_COLORMAP_NAME)) {
                    name = "IR Default";
                }
            }
            // load colormap by name
            ColorMap colorMap = (ColorMap) ColorMapUtil.loadColorMap(
                    modisResourceData.getResourceName().getRscCategory()
                            .getCategoryName(), name);
            colorMapParameters.setColorMap(colorMap);

        }
        setColorMapUnits(dataRecord, preferences, colorMapParameters);

        return colorMapParameters;
    }

    /**
     * Set display and data units, min and max for color map.
     * 
     * @param dataRecord
     *            MODIS data record
     * @param preferences
     *            Stylerule preferences
     * @param colorMapParameters
     * @throws VizException
     */
    private void setColorMapUnits(ModisRecord dataRecord,
            ImagePreferences preferences, ColorMapParameters colorMapParameters)
            throws VizException {
        setDataUnit(dataRecord, colorMapParameters);
        Unit<?> displayUnit = Unit.ONE;
        if (preferences != null) {
            if (preferences.getDisplayUnits() != null) {
                displayUnit = preferences.getDisplayUnits();
            }
        }
        colorMapParameters.setDisplayUnit(displayUnit);
        colorMapParameters.setColorMapMin(10.0f);
        colorMapParameters.setColorMapMax(500.0f);
        if (preferences != null) {
            DataScale scale = preferences.getDataScale();
            if (scale != null) {
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
            }
        }

    }

    /**
     * Set data unit for color map.
     * 
     * @param dataRecord
     *            MODIS data record
     * @param colorMapParameters
     * @throws VizException
     */
    private void setDataUnit(ModisRecord dataRecord,
            ColorMapParameters colorMapParameters) throws VizException {
        Unit<?> dataUnit = Unit.ONE;

        try {
            IDataRecord record = DataCubeContainer.getDataRecord(dataRecord,
                    Request.buildPointRequest(new java.awt.Point(0, 0)),
                    ModisRecord.getDataSet(0))[0];
            Map<String, Object> attrs = record.getDataAttributes();
            if (attrs != null) {
                Float offset = (Float) attrs.get(ModisRecord.OFFSET_ID);
                Float scale = (Float) attrs.get(ModisRecord.SCALE_ID);
                String unitStr = (String) attrs.get(ModisRecord.UNIT_ID);
                if (attrs.containsKey(ModisRecord.MISSING_VALUE_ID)) {
                    colorMapParameters.setNoDataValue(((Number) attrs
                            .get(ModisRecord.MISSING_VALUE_ID)).doubleValue());
                }
                if (unitStr != null) {
                    try {
                        dataUnit = UnitFormat.getUCUMInstance().parseObject(
                                unitStr, new ParsePosition(0));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (dataUnit == null) {
                    dataUnit = SI.MILLI(SI.GRAM).divide(SI.CUBIC_METRE);
                }
                if (offset != null && offset != 0.0) {
                    dataUnit = dataUnit.plus(offset);
                }
                if (scale != null && scale != 0.0 && scale != 1.0) {
                    dataUnit = dataUnit.times(scale);
                }
            }
        } catch (DataCubeException e) {
            throw new VizException(e);
        }

        colorMapParameters.setColorMapUnit(dataUnit);

    }

    /**
     * Set color bar from preferences.
     * 
     * @param preferences
     * @param colorMapParameters
     */
    private void setColorBar(ImagePreferences preferences,
            ColorMapParameters colorMapParameters) {
        if (preferences != null && preferences.getColorbarLabeling() != null) {
            LabelingPreferences labelPrefs = preferences.getColorbarLabeling();
            colorMapParameters.setColorBarIntervals(labelPrefs.getValues());
        }

        ColorBarFromColormap colorBar = (ColorBarFromColormap) this.cbarResource
                .getResourceData().getColorbar();
        if (colorBar.getColorMap() == null) {
            colorBar.setColorMap((ColorMap) getCapability(
                    ColorMapCapability.class).getColorMapParameters()
                    .getColorMap());
        }
        colorBar.setIsScalingAttemptedForThisColorMap(true);
        colorBar.scalePixelValues();
        colorBar.setNumPixelsToReAlignLabel(true);
        colorBar.setImagePreferences(preferences);
        colorBar.setDisplayUnitStr(colorMapParameters.getDisplayUnit()
                .toString());
    }

    List<String> getParameterList(PluginDataObject pdo) {
        String paramStr = ((ModisRecord) pdo).getParameter();
        List<String> paramList = new ArrayList<String>(0);
        paramList.add(paramStr);

        return paramList;
    }

    protected AbstractFrameData createNewFrame(DataTime frameTime, int timeInt) {
        return (AbstractFrameData) new FrameData(frameTime, timeInt);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.core.rsc.IResourceDataChanged#resourceChanged(com
     * .raytheon.uf.viz.core.rsc.IResourceDataChanged.ChangeType,
     * java.lang.Object)
     */
    @Override
    public void resourceChanged(ChangeType type, Object object) {
        if (type == ChangeType.DATA_UPDATE) {
            ModisRecord[] records = new ModisRecord[1];
            if (object instanceof PluginDataObject[]) {
                PluginDataObject[] pdos = (PluginDataObject[]) object;
                records = new ModisRecord[pdos.length];
                for (int i = 0; i < pdos.length; ++i) {
                    records[i] = (ModisRecord) pdos[i];
                }
            } else if (object instanceof ModisRecord) {
                records[0] = (ModisRecord) object;
            }
        } else if (type != null && type == ChangeType.CAPABILITY) {
            if (object instanceof ImagingCapability) {
                ImagingCapability imgCap = getCapability(ImagingCapability.class);
                ImagingCapability newImgCap = (ImagingCapability) object;
                imgCap.setBrightness(newImgCap.getBrightness(), false);
                imgCap.setContrast(newImgCap.getContrast(), false);
                imgCap.setAlpha(newImgCap.getAlpha(), false);
                modisResourceData.setAlpha(imgCap.getAlpha());
                modisResourceData.setBrightness(imgCap.getBrightness());
                modisResourceData.setContrast(imgCap.getContrast());
                issueRefresh();
            } else if (object instanceof ColorMapCapability) {
                ColorMapCapability colorMapCap = getCapability(ColorMapCapability.class);
                ColorMapCapability newColorMapCap = (ColorMapCapability) object;
                colorMapCap.setColorMapParameters(
                        newColorMapCap.getColorMapParameters(), false);
                ColorMap theColorMap = (ColorMap) colorMapCap
                        .getColorMapParameters().getColorMap();
                String colorMapName = colorMapCap.getColorMapParameters()
                        .getColorMapName();
                modisResourceData.setColorMapName(colorMapName);
                modisResourceData.getRscAttrSet().setAttrValue(
                        DEFAULT_COLORMAP_NAME, colorMapName);
                ColorBarFromColormap cBar = modisResourceData.getColorBar();
                cBar.setColorMap(theColorMap);

                ColorBarFromColormap colorBar = (ColorBarFromColormap) this.cbarResource
                        .getResourceData().getColorbar();

                if (colorBar.getImagePreferences() != null
                        && cBar.getImagePreferences() == null) {
                    cBar.setImagePreferences(colorBar.getImagePreferences());
                }
                cBar.setIsScalingAttemptedForThisColorMap(colorBar
                        .isScalingAttemptedForThisColorMap());
                cBar.setNumPixelsToReAlignLabel(colorBar
                        .isAlignLabelInTheMiddleOfInterval());
                modisResourceData.getRscAttrSet()
                        .setAttrValue("colorBar", cBar);
                modisResourceData.setIsEdited(true);
                issueRefresh();
            }
        }
        issueRefresh();
    }

    public void initResource(IGraphicsTarget target) throws VizException {
        cbarRscPair = ResourcePair
                .constructSystemResourcePair(new ColorBarResourceData(
                        modisResourceData.getColorBar()));
        getDescriptor().getResourceList().add(cbarRscPair);
        getDescriptor().getResourceList().instantiateResources(getDescriptor(),
                true);
        cbarResource = (ColorBarResource) cbarRscPair.getResource();
        getCapability(ImagingCapability.class).setProvider(this);

        loadRecords();
    }

    private void loadRecords() throws VizException {

        queryRecords();

        List<ModisRecord> records = new ArrayList<ModisRecord>();
        Iterator<IRscDataObject> iter = newRscDataObjsQueue.iterator();
        while (iter.hasNext()) {
            IRscDataObject rdo = iter.next();
            PluginDataObject pdo = ((DfltRecordRscDataObj) rdo).getPDO();
            ModisRecord record = (ModisRecord) pdo;
            records.add(record);
        }
        modisResourceData.setRecords(records.toArray(new ModisRecord[records
                .size()]));
    }

    protected void paintFrame(AbstractFrameData frameData,
            IGraphicsTarget target, PaintProperties paintProps)
            throws VizException {

        FrameData currFrame = (FrameData) frameData;
        RecordData recordData = currFrame.getRecordData();
        List<Collection<DrawableImage>> allImagesForframe = getAllImages(
                currFrame, target, paintProps);

        for (Collection<DrawableImage> images : allImagesForframe) {
            if (images != null && images.size() > 0) {
                if (!target.drawRasters(paintProps,
                        images.toArray(new DrawableImage[images.size()]))) {
                    issueRefresh();
                }
                /*
                 * paintFrame gets called for every display action (e.g. pan,
                 * zoom), but we don't want to generate the geotiff for each
                 * action.
                 */
                if (!recordData.createdGeoTiff) {
                    createGeoTiff(recordData.getRecord());
                    recordData.createdGeoTiff = true;
                }
            }
        }
    }

    protected void createGeoTiff(ModisRecord record) {
        com.vividsolutions.jts.geom.Envelope envelope = new com.vividsolutions.jts.geom.Envelope();

        float[] lons = record.getCoverage().getLongitudes();
        float[] lats = record.getCoverage().getLatitudes();
        for (int i = 0; i < lons.length; i += 1) {
            envelope.expandToInclude(lons[i], lats[i]);
        }
        float[] data = null;
        try {
            IDataRecord[] dataRecords = DataCubeContainer.getDataRecord(record,
                    Request.ALL, ModisRecord.getDataSet(0));
            data = (float[]) dataRecords[0].getDataObject();
        } catch (DataCubeException e) {
            statusHandler.warn("Unable to create geotiff");
            e.printStackTrace();
            return;
        }
        int nx = record.getCoverage().getNx();
        int ny = record.getCoverage().getNy();

        /*
         * The decoder flips the lats and lons in order to display the image
         * correctly. Flip them back so the ordinate indices line up with the
         * data indices. Don't want to calculate 2 different indices for each
         * point so just flip now.
         */
        float[] longitudes = Arrays.copyOf(lons, lons.length);
        float[] latitudes = Arrays.copyOf(lats, lats.length);
        ArraysUtil.flipHoriz(longitudes, ny, nx);
        ArraysUtil.flipHoriz(latitudes, ny, nx);

        int[] dimensions = { 2048, 2048 };
        double dx = envelope.getWidth() / dimensions[0];
        double dy = envelope.getHeight() / dimensions[1];

        float[] grid = new float[dimensions[0] * dimensions[1]];
        Arrays.fill(grid, Float.NaN);

        /* Iterate each pixel of the source image */
        for (int j = 1; j < ny; j += 1) {
            for (int i = 1; i < nx; i += 1) {
                int i0 = (j - 1) * nx + (i - 1);
                int i1 = j * nx + (i - 1);
                int i2 = (j - 1) * nx + i;
                int i3 = j * nx + i;

                Coordinate c0 = new Coordinate(longitudes[i0], latitudes[i0]);
                Coordinate c1 = new Coordinate(longitudes[i1], latitudes[i1]);
                Coordinate c2 = new Coordinate(longitudes[i2], latitudes[i2]);
                Coordinate c3 = new Coordinate(longitudes[i3], latitudes[i3]);

                com.vividsolutions.jts.geom.Envelope cellEnv = new com.vividsolutions.jts.geom.Envelope(
                        c0);
                cellEnv.expandToInclude(c1);
                cellEnv.expandToInclude(c2);
                cellEnv.expandToInclude(c3);

                int minX = (int) ((cellEnv.getMinX() - envelope.getMinX()) / dx);
                int maxX = (int) ((cellEnv.getMaxX() - envelope.getMinX()) / dx);
                int minY = (int) ((cellEnv.getMinY() - envelope.getMinY()) / dy);
                int maxY = (int) ((cellEnv.getMaxY() - envelope.getMinY()) / dy);
                /*
                 * Iterate each target pixel that is in the envelope of the
                 * source pixel.
                 */
                for (int ic = minX; ic <= maxX; ic += 1) {
                    double x = envelope.getMinX() + ic * dx;
                    for (int jc = minY; jc <= maxY; jc += 1) {
                        double y = envelope.getMinY() + jc * dy;
                        Coordinate t = new Coordinate(x, y);
                        if (isInCell(t, c0, c1, c2, c3)) {
                            /*
                             * This is doing bilinear interpolation, it could
                             * easily be switched to nearest neighbor. This data
                             * seems particularly full of static so bilinear
                             * softens the data and nearest neighbor might be
                             * better. Even better yet might be if we do a
                             * better no data value filtering for bilinear or
                             * perhaps some type of maxVal selection .
                             */
                            double d0 = c0.distance(t);
                            double d1 = c1.distance(t);
                            double d2 = c2.distance(t);
                            double d3 = c3.distance(t);
                            double dt = d0 + d1 + d2 + d3;

                            double v0 = (d0 / dt) * data[i0];
                            double v1 = (d1 / dt) * data[i1];
                            double v2 = (d2 / dt) * data[i2];
                            double v3 = (d3 / dt) * data[i3];

                            /*
                             * NOTE: In LonLat space down is smaller values but
                             * in the image space up is smaller values so this
                             * index operation is performing a inversion along
                             * the y axis.
                             */
                            int dataIndex = (dimensions[1] - jc - 1)
                                    * dimensions[0] + ic;
                            if (dataIndex < 0
                                    || dataIndex >= (dimensions[0] * dimensions[1])) {
                                System.out.println("dataIndex: " + dataIndex
                                        + " is out of bounds for granule "
                                        + getName());
                            } else {
                                grid[dataIndex] = (float) (v0 + v1 + v2 + v3);
                            }
                        }
                    }
                }
            }
        }
        ColorMapParameters colorMapParameters = getCapability(
                ColorMapCapability.class).getColorMapParameters();
        ColorMapData colorMapData = new ColorMapData(FloatBuffer.wrap(grid),
                dimensions);

        RenderedImage image = Colormapper.colorMap(colorMapData,
                colorMapParameters);

        GridCoverageFactory factory = new GridCoverageFactory();
        GeoTiffFormat format = new GeoTiffFormat();

        GridCoverage coverage = factory.create("modis.tif", image,
                new ReferencedEnvelope(envelope, DefaultGeographicCRS.WGS84));
        try {
            File tiffFile = new File("/tmp/" + getName() + ".tiff");
            GridCoverageWriter writer = format.getWriter(tiffFile);
            writer.write(coverage, null);
            writer.dispose();
        } catch (IllegalArgumentException | IOException e) {
            statusHandler.warn("Unable to create geotiff");
            e.printStackTrace();
            return;
        }
    }

    /**
     * Custom method for determining if a point lies within a quadrilateral.
     * This only works for convex quadrilaterals but I've never seen an image
     * with a concave pixel so it seems good enough . This method is 10x faster
     * than doing a Polygon.contains(), I'm sure we lose some robustness for the
     * extra speed but it seems to work well for now. The vertices are connected
     * as shown below, the important thing to note is that v1 is not connected
     * to v2, if you accidentally try to connect them then its no longer convex
     * and the whole method falls apart.
     * 
     * <pre>
     * v0---v1
     *  |   |
     *  |   |
     *  |   |
     * v2---v3
     * </pre>
     */
    protected boolean isInCell(Coordinate p, Coordinate v0, Coordinate v1,
            Coordinate v2, Coordinate v3) {
        int orientation0 = CGAlgorithms.orientationIndex(v0, v1, p);
        if (orientation0 == 0) {
            return true;
        }
        int orientation1 = CGAlgorithms.orientationIndex(v1, v3, p);
        if (orientation0 != orientation1) {
            if (orientation1 == 0) {
                return true;
            } else {
                return false;
            }
        }
        int orientation2 = CGAlgorithms.orientationIndex(v3, v2, p);
        if (orientation0 != orientation2) {
            if (orientation2 == 0) {
                return true;
            } else {
                return false;
            }
        }
        int orientation3 = CGAlgorithms.orientationIndex(v2, v0, p);
        if (orientation0 != orientation3) {
            if (orientation3 == 0) {
                return true;
            } else {
                return false;
            }
        }
        return true;

    }

    @Override
    public void project(CoordinateReferenceSystem crs) throws VizException {
        synchronized (dataRecordMap) {
            try {
                for (RecordData data : dataRecordMap.values()) {
                    data.project();
                }
            } catch (Exception e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Error reprojecting MODIS data", e);
            }
        }
    }

    public void disposeInternal() {
        super.disposeInternal();
        synchronized (dataRecordMap) {
            if (!dataRecordMap.isEmpty()) {
                for (RecordData data : dataRecordMap.values()) {
                    if (data != null) {
                        data.dispose();
                    }
                }
            }
            dataRecordMap.clear();
        }
        if (cbarRscPair != null) {
            getDescriptor().getResourceList().remove(cbarRscPair);
        }
    }

    public List<Collection<DrawableImage>> getAllImages(FrameData frameData,
            IGraphicsTarget target, PaintProperties paintProps)
            throws VizException {
        List<RecordData> recordDataList = frameData.getRecordDataMap().get(
                frameData.getFrameTime());
        List<Collection<DrawableImage>> images = new ArrayList<Collection<DrawableImage>>();

        if (recordDataList != null) {
            for (RecordData recordData : recordDataList) {
                if (recordData != null) {
                    Collection<DrawableImage> recordImages = recordData
                            .getImagesToRender(target, paintProps);
                    images.add(recordImages);
                }
            }
        }
        return images;
    }

    public String getLocFilePathForImageryStyleRule() {
        return NcPathConstants.MODIS_IMG_STYLE_RULES;
    }

    public static synchronized JAXBManager getJaxbManager()
            throws JAXBException {

        if (jaxb == null) {
            SubClassLocator locator = new SubClassLocator();
            Collection<Class<?>> classes = JAXBClassLocator.getJAXBClasses(
                    locator, StyleRuleset.class, StyleRule.class, Level.class,
                    AbstractStylePreferences.class, MatchCriteria.class);

            locator.save();

            Class<?>[] jaxbClasses = new Class<?>[classes.size() + 1];
            classes.toArray(jaxbClasses);

            /*
             * Add JaxbDummyObject at the beginning so properties are loaded
             * correctly
             */
            jaxbClasses[jaxbClasses.length - 1] = jaxbClasses[0];
            jaxbClasses[0] = JaxbDummyObject.class;

            jaxb = new JAXBManager(jaxbClasses);
        }

        return jaxb;
    }

    @Override
    public void propertiesChanged(ResourceProperties updatedProps) {
        if (cbarRscPair != null) {
            cbarRscPair.getProperties().setVisible(updatedProps.isVisible());
        }
    }

    public String getName() {
        FrameData currFrame = (FrameData) getCurrentFrame();
        if (currFrame != null) {
            name = currFrame.getLegendForFrame();
        }
        if (name == null) {
            return "NPP MODIS";
        }
        return name;
    }

    public void setName(String str) {
        name = str;
    }

    // Required by IImagingExtension.ImageProvider class
    @Override
    public Collection<DrawableImage> getImages(IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getLegendStr() {
        FrameData curFrame = (FrameData) getCurrentFrame();
        return (curFrame != null ? curFrame.getLegendForFrame()
                : "No Matching Data");
    }

}
