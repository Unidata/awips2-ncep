package gov.noaa.nws.ncep.viz.rsc.modis.rsc;

import gov.noaa.nws.ncep.common.dataplugin.modis.ModisRecord;
import gov.noaa.nws.ncep.common.dataplugin.modis.dao.ModisDao;
import gov.noaa.nws.ncep.viz.common.ColorMapUtil;
import gov.noaa.nws.ncep.viz.common.ui.color.GempakColor;
import gov.noaa.nws.ncep.viz.localization.NcPathManager;
import gov.noaa.nws.ncep.viz.localization.NcPathManager.NcPathConstants;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.colorBar.ColorBarResource;
import gov.noaa.nws.ncep.viz.resources.colorBar.ColorBarResourceData;
import gov.noaa.nws.ncep.viz.rsc.modis.tileset.ModisDataRetriever;
import gov.noaa.nws.ncep.viz.ui.display.ColorBarFromColormap;
import gov.noaa.nws.ncep.viz.ui.display.NCMapDescriptor;

import java.io.File;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.measure.Measure;
import javax.measure.converter.UnitConverter;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;
import javax.xml.bind.JAXBException;

import org.eclipse.swt.graphics.RGB;
import org.geotools.coverage.grid.GeneralGridGeometry;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;

import com.raytheon.uf.common.colormap.ColorMap;
import com.raytheon.uf.common.colormap.prefs.ColorMapParameters;
import com.raytheon.uf.common.dataplugin.HDF5Util;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.datastorage.DataStoreFactory;
import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.common.datastorage.Request;
import com.raytheon.uf.common.datastorage.records.FloatDataRecord;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.geospatial.IGridGeometryProvider;
import com.raytheon.uf.common.geospatial.ReferencedCoordinate;
import com.raytheon.uf.common.geospatial.util.EnvelopeIntersection;
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
import com.raytheon.uf.viz.core.DrawableImage;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.LineStyle;
import com.raytheon.uf.viz.core.IGraphicsTarget.RasterMode;
import com.raytheon.uf.viz.core.IMesh;
import com.raytheon.uf.viz.core.PixelCoverage;
import com.raytheon.uf.viz.core.drawables.IImage;
import com.raytheon.uf.viz.core.drawables.IWireframeShape;
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
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;

/**
 * 
 * Class for display of the MODIS satellite data
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 01, 2014            kbugenhagen Initial creation.
 * 
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1.0
 */

public class ModisResource extends
        AbstractNatlCntrsResource<ModisResourceData, NCMapDescriptor> implements
        INatlCntrsResource, IResourceDataChanged, ImageProvider {

    private ModisResourceData modisResourceData;

    protected ResourcePair cbarRscPair;

    protected ColorBarResource cbarResource;

    private static JAXBManager jaxb;

    private float[] latitudes;

    private float[] longitudes;

    private final SimpleDateFormat dateFmt = new SimpleDateFormat("yyMMdd/HHmm");

    // wireframe attributes -----------------------

    private final int colorWhiteIndex = 31;

    private final RGB colorWhite = GempakColor.convertToRGB(colorWhiteIndex);

    private final float lineWidth = 1;

    private final LineStyle dashedline = LineStyle.DASHED;

    /**
     * Map for data records to renderable data, synchronized on for painting,
     * disposing, adding, and removing
     */
    private Map<ModisRecord, RecordData> dataRecordMap;

    private String name;

    private class ModisTileImageCreator implements TileImageCreator {

        private ModisRecord record;

        private ModisTileImageCreator(ModisRecord record) {
            this.record = record;
        }

        @Override
        public DrawableImage createTileImage(IGraphicsTarget target, Tile tile,
                GeneralGridGeometry targetGeometry) throws VizException {
            IImage image = target
                    .getExtension(IColormappedImageExtension.class)
                    .initializeRaster(
                            new ModisDataRetriever(record, tile.tileLevel,
                                    tile.getRectangle()),
                            getCapability(ColorMapCapability.class)
                                    .getColorMapParameters());
            IMesh mesh = target.getExtension(IMapMeshExtension.class)
                    .constructMesh(tile.tileGeometry, targetGeometry);

            return new DrawableImage(image, new PixelCoverage(mesh),
                    RasterMode.ASYNCHRONOUS);
        }
    }

    private void logDuration(long startTime, String method) {
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000000;
        if (duration > 0) {
            System.out.println(method + " took: " + duration + " ms");
        }
    }

    /**
     * Every {@link ModisRecord} will have a corresponding RecordData object
     */
    private class RecordData {

        private double INTERSECTION_FACTOR = 10.0;

        private final static int TILE_SIZE = 2048;

        private ModisRecord record;

        /** Intersection geometry for the target */
        private List<PreparedGeometry> targetIntersection;

        /** Flag designated if a project call is required next paint */
        private boolean project;

        /** Renderable for the data record */
        private TileSetRenderable tileSet;

        private double resolution;

        public RecordData(ModisRecord dataRecord) {
            this.tileSet = new TileSetRenderable(
                    getCapability(ImagingCapability.class), dataRecord
                            .getCoverage().getGridGeometry(latitudes,
                                    longitudes), new ModisTileImageCreator(
                            dataRecord), dataRecord.getLevels(), TILE_SIZE);
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
            tileSet.dispose();
            tileSet = null;
            targetIntersection = null;
        }

        public boolean contains(Geometry geom) {
            if (targetIntersection != null) {
                for (PreparedGeometry pg : targetIntersection) {
                    if (pg.contains(geom)) {
                        return true;
                    }
                }
            }
            return false;
        }

        public double interrogate(Coordinate latLon) throws VizException {
            return tileSet.interrogate(latLon);
        }

        public TileSetRenderable getTileSet() {
            return tileSet;
        }

        public ModisRecord getRecord() {
            return record;
        }

    }

    // =================================================================

    protected class FrameData extends AbstractFrameData {

        RecordData recordData;

        private String legendStr = "No Data";

        // wireframes painted around image boundary
        List<IWireframeShape> wireFrames = new ArrayList<IWireframeShape>();

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

        public List<IWireframeShape> getWireFrames() {
            return wireFrames;
        }

        public void setWireFrames(List<IWireframeShape> wireFrames) {
            this.wireFrames = wireFrames;
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
            for (IWireframeShape wireFrame : wireFrames) {
                wireFrame.dispose();
                wireFrame = null;
            }
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
     * Add a data record to be displayed. Should be synchronized on
     * {@link #dataMap} when called
     * 
     * @param dataRecord
     * @throws VizException
     */
    protected RecordData addRecord(ModisRecord dataRecord) throws VizException {
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
                        latitudes = ((FloatDataRecord) rec).getFloatData();
                    } else if (rec.getName().equals(
                            ModisDao.LONGITUDE_DATASET_NAME)) {
                        longitudes = ((FloatDataRecord) rec).getFloatData();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Create color map from a record
     * 
     * @param dataRecord
     * @throws VizException
     */
    protected void createColorMap(ModisRecord dataRecord) throws VizException {

        String styleRuleFile = getLocFilePathForImageryStyleRule();
        File file = NcPathManager.getInstance().getStaticFile(styleRuleFile);

        ColorBarFromColormap colorBar = (ColorBarFromColormap) this.cbarResource
                .getResourceData().getColorbar();
        if (colorBar.getColorMap() == null) {
            colorBar.setColorMap((ColorMap) getCapability(
                    ColorMapCapability.class).getColorMapParameters()
                    .getColorMap());
        }

        // Get style rule preferences

        StyleRule styleRule = null;
        List<String> paramList = getParameterList(dataRecord);
        ParamLevelMatchCriteria matchCriteria = new ParamLevelMatchCriteria();
        matchCriteria.setParameterName(paramList);

        try {
            // this fails to recognize elements in xml file
            // StyleRuleset styleSet = (StyleRuleset) SerializationUtil
            // .jaxbUnmarshalFromXmlFile(file);

            StyleRuleset styleSet = (StyleRuleset) getJaxbManager()
                    .unmarshalFromXmlFile(file);

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
        } catch (SerializationException e1) {
            e1.printStackTrace();
        } catch (JAXBException e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
        }

        ImagePreferences preferences = null;
        if (styleRule != null) {
            preferences = (ImagePreferences) styleRule.getPreferences();
        }

        // Create colormap parameters
        ColorMapParameters colorMapParameters = getCapability(
                ColorMapCapability.class).getColorMapParameters();
        if (colorMapParameters == null) {
            colorMapParameters = new ColorMapParameters();
        }

        // Setup colormap
        if (colorMapParameters.getColorMap() == null) {
            // None set, set name and load
            String name = colorMapParameters.getColorMapName();
            if (name == null || name.equals("colorMapName")) {

                if (preferences != null) {
                    // check preference
                    name = preferences.getDefaultColormap();
                }
                if (name == null || name.equals("colorMapName")) {
                    // no preference, absolute default colormap
                    name = "IR Default";
                }
            }
            ColorMap colorMap = (ColorMap) ColorMapUtil.loadColorMap(
                    modisResourceData.getResourceName().getRscCategory()
                            .getCategoryName(), name);
            colorMapParameters.setColorMap(colorMap);
        }

        // Setup units for display and data
        Unit<?> displayUnit = Unit.ONE;
        if (preferences != null) {
            if (preferences.getDisplayUnits() != null) {
                displayUnit = preferences.getDisplayUnits();
            }
        }
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
        } catch (Exception e) {
            throw new VizException(e);
        }

        colorMapParameters.setDataUnit(dataUnit);
        colorMapParameters.setColorMapUnit(dataUnit);
        colorMapParameters.setDisplayUnit(displayUnit);

        colorMapParameters.setColorMapMin(colorMapParameters.getDataMin());
        colorMapParameters.setColorMapMax(colorMapParameters.getDataMax());
        if (preferences != null) {
            DataScale scale = preferences.getDataScale();
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

        if (preferences != null && preferences.getColorbarLabeling() != null) {
            LabelingPreferences lPrefs = preferences.getColorbarLabeling();
            colorMapParameters.setColorBarIntervals(lPrefs.getValues());
        }

        colorMapParameters.setLogarithmic(true);

        colorBar.setIsScalingAttemptedForThisColorMap(true);

        colorBar.scalePixelValues();

        colorBar.setNumPixelsToReAlignLabel(true);

        colorBar.setImagePreferences(preferences);

        colorBar.setDisplayUnitStr(displayUnit.toString());

        if (colorMapParameters.getPersisted() != null) {
            colorMapParameters.applyPersistedParameters(colorMapParameters
                    .getPersisted());
        }

        getCapability(ColorMapCapability.class).setColorMapParameters(
                colorMapParameters);

        resourceChanged(ChangeType.CAPABILITY,
                getCapability(ColorMapCapability.class));
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
                modisResourceData.getRscAttrSet().setAttrValue("colorMapName",
                        colorMapName);
                ColorBarFromColormap cBar = modisResourceData.getColorBar();
                cBar.setColorMap(theColorMap);

                ColorBarFromColormap colorBar = (ColorBarFromColormap) this.cbarResource
                        .getResourceData().getColorbar();
                if (colorBar != null) {
                    if (colorBar.getImagePreferences() != null
                            && cBar.getImagePreferences() == null) {
                        cBar.setImagePreferences(colorBar.getImagePreferences());
                    }
                    cBar.setIsScalingAttemptedForThisColorMap(colorBar
                            .isScalingAttemptedForThisColorMap());
                    cBar.setNumPixelsToReAlignLabel(colorBar
                            .isAlignLabelInTheMiddleOfInterval());
                }
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
        List<Collection<DrawableImage>> allImagesForframe = getAllImages(
                currFrame, target, paintProps);

        // paint images
        for (Collection<DrawableImage> images : allImagesForframe) {
            if (images != null && images.size() > 0) {
                if (!target.drawRasters(paintProps,
                        images.toArray(new DrawableImage[images.size()]))) {
                    issueRefresh();
                }
            }
        }
        paintWireFrames(target, frameData);
    }

    public List<IWireframeShape> getAllWireFrames(IGraphicsTarget target,
            FrameData frameData) throws VizException {

        List<RecordData> recordDataList = frameData.getRecordDataMap().get(
                frameData.getFrameTime());
        List<IWireframeShape> wireFrames = new ArrayList<IWireframeShape>();
        if (recordDataList != null) {
            for (RecordData recordData : recordDataList) {
                if (recordData != null) {
                    wireFrames.add(createImageBoundaryWireframe(recordData,
                            target));

                    // wireFrames.add(createTargetIntersectionWireframe(
                    // recordData, target));
                }
            }
        }
        return wireFrames;
    }

    // private IWireframeShape createTargetIntersectionWireframe(
    // RecordData recordData, IGraphicsTarget target) throws VizException {
    // PreparedGeometry prepGeom = recordData.targetIntersection.get(0);
    // Geometry geom = prepGeom.getGeometry().getEnvelope();
    // Coordinate[] pixelCoords = geom.getCoordinates();
    // Coordinate[] latLonCoords = new Coordinate[pixelCoords.length];
    // for (int i = 0; i < pixelCoords.length; i++) {
    // Coordinate latLon = getLatLonFromPixel(pixelCoords[i]);
    // System.out
    // .println("createTargetIntersectionWireframe: pixelCoord = "
    // + pixelCoords[i] + " latlon = " + latLon.x + "/"
    // + latLon.y);
    // latLonCoords[i] = latLon;
    // }
    // return createWireFrameFromCoords(target, latLonCoords);
    // }

    private IWireframeShape createImageBoundaryWireframe(RecordData recordData,
            IGraphicsTarget target) throws VizException {
        Geometry envelope = recordData.getRecord().getCoverage().getEnvelope();
        Coordinate[] coords = envelope.getCoordinates();

        // for (int i = 0; i < coords.length; i++) {
        // System.out.println("createImageBoundaryWireframe: coord = "
        // + coords[i].x + "/" + coords[i].y);
        // }

        return createWireFrameFromCoords(target, coords);
    }

    private IWireframeShape createWireFrameFromCoords(IGraphicsTarget target,
            Coordinate[] coords) throws VizException {

        IWireframeShape wireFrame = target.createWireframeShape(false,
                descriptor);
        wireFrame.compile();

        for (int i = 0; i < coords.length - 1; i++) {
            Coordinate[] line = new Coordinate[] { coords[i], coords[i + 1] };
            wireFrame.addLineSegment(line);
        }
        return wireFrame;
    }

    /*
     * Paint wireframes around image boundary
     */
    private void paintWireFrames(IGraphicsTarget target,
            AbstractFrameData frameData) throws VizException {
        FrameData currFrame = (FrameData) frameData;

        List<IWireframeShape> wireFrames = getAllWireFrames(target, currFrame);
        currFrame.setWireFrames(getAllWireFrames(target, currFrame));

        for (IWireframeShape wireFrame : wireFrames) {
            target.drawWireframeShape(wireFrame, colorWhite, lineWidth,
                    dashedline);

        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.core.rsc.AbstractVizResource#project(org.opengis.
     * referencing.crs.CoordinateReferenceSystem)
     */
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
        name = currFrame.getLegendForFrame();
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

    // interrogate methods and attributes
    // *******************************************************************

    public static final String SATELLITE_DATA_INTERROGATE_ID = "satelliteDataValue";

    @Override
    public Map<String, Object> interrogate(ReferencedCoordinate coord)
            throws VizException {

        Map<String, Object> interMap = new HashMap<String, Object>();
        ColorMapParameters params = getCapability(ColorMapCapability.class)
                .getColorMapParameters();
        FrameData currFrame = (FrameData) getCurrentFrame();
        double dataValue = Double.NaN;
        Unit<?> dataUnit = params.getDisplayUnit();
        double noDataValue = params.getNoDataValue();
        Coordinate latLon = null;
        Coordinate crs = null;
        try {
            latLon = coord.asLatLon();
            Coordinate grid = coord.asGridCell(descriptor.getGridGeometry(),
                    PixelInCell.CELL_CENTER);
            MathTransform mt = descriptor.getGridGeometry().getGridToCRS(
                    PixelInCell.CELL_CENTER);
            double[] out = new double[2];
            mt.transform(new double[] { grid.x, grid.y }, 0, out, 0, 1);
            crs = new Coordinate(out[0], out[1]);
        } catch (Exception e) {
            throw new VizException(
                    "Could not get lat/lon from ReferencedCoordinate", e);
        }

        Point crsPoint = new GeometryFactory().createPoint(crs);

        double bestValue = Double.NaN;
        ModisRecord bestRecord = null;

        synchronized (dataRecordMap) {
            List<RecordData> records = currFrame.getRecordDataMap().get(
                    descriptor.getTimeForResource(this));

            if (records != null) {
                // Since there is overlap between granules, the best value is
                // the one that is closest to 0
                for (RecordData recordData : records) {
                    if (recordData.contains(crsPoint)) {
                        double value = recordData.interrogate(latLon);
                        if (Double.isNaN(value) == false
                                && value != noDataValue) {
                            bestValue = value;
                            bestRecord = recordData.getRecord();
                            break;
                        }
                    }
                }
            }

            if (Double.isNaN(bestValue) == false) {
                dataValue = params.getDataToDisplayConverter().convert(
                        bestValue);
            }
            if (bestRecord != null) {
                interMap.put(IGridGeometryProvider.class.toString(), bestRecord);
            }
        }

        interMap.put(SATELLITE_DATA_INTERROGATE_ID,
                Measure.valueOf(dataValue, dataUnit));
        return interMap;

    }
}