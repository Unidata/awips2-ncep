package gov.noaa.nws.ncep.viz.rsc.viirs.rsc;

import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.AbstractSatelliteRecordData;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.colorBar.ColorBarResource;
import gov.noaa.nws.ncep.viz.resources.colorBar.ColorBarResourceData;
import gov.noaa.nws.ncep.viz.resources.util.Sampler;
import gov.noaa.nws.ncep.viz.ui.display.ColorBarFromColormap;
import gov.noaa.nws.ncep.viz.ui.display.NCMapDescriptor;

import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.measure.Measure;
import javax.measure.converter.UnitConverter;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;

import org.geotools.coverage.grid.GeneralGridGeometry;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.raytheon.uf.common.colormap.ColorMap;
import com.raytheon.uf.common.colormap.prefs.ColorMapParameters;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.npp.viirs.VIIRSDataRecord;
import com.raytheon.uf.common.datastorage.Request;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.geospatial.IGridGeometryProvider;
import com.raytheon.uf.common.geospatial.ReferencedCoordinate;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.style.LabelingPreferences;
import com.raytheon.uf.common.style.StyleException;
import com.raytheon.uf.common.style.StyleManager;
import com.raytheon.uf.common.style.StyleManager.StyleType;
import com.raytheon.uf.common.style.StyleRule;
import com.raytheon.uf.common.style.image.DataScale;
import com.raytheon.uf.common.style.image.ImagePreferences;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.DrawableImage;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.RasterMode;
import com.raytheon.uf.viz.core.IMesh;
import com.raytheon.uf.viz.core.PixelCoverage;
import com.raytheon.uf.viz.core.drawables.ColorMapLoader;
import com.raytheon.uf.viz.core.drawables.IImage;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.drawables.ext.IImagingExtension.ImageProvider;
import com.raytheon.uf.viz.core.drawables.ext.colormap.IColormappedImageExtension;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.IMapMeshExtension;
import com.raytheon.uf.viz.core.rsc.IInputHandler;
import com.raytheon.uf.viz.core.rsc.IInputHandler.InputPriority;
import com.raytheon.uf.viz.core.rsc.IResourceDataChanged;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.ResourceProperties;
import com.raytheon.uf.viz.core.rsc.capabilities.ColorMapCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.ImagingCapability;
import com.raytheon.uf.viz.core.sampling.ISamplingResource;
import com.raytheon.uf.viz.core.tile.Tile;
import com.raytheon.uf.viz.core.tile.TileSetRenderable;
import com.raytheon.uf.viz.core.tile.TileSetRenderable.TileImageCreator;
import com.raytheon.uf.viz.datacube.DataCubeContainer;
import com.raytheon.uf.viz.npp.viirs.rsc.VIIRSDataCallback;
import com.raytheon.uf.viz.npp.viirs.style.VIIRSDataRecordCriteria;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

/**
 * 
 * ViirsResource - Class for display of the VIIRS satellite data
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 06/23/2014               Yukuan Song  Initial creation
 * 08/20/2014   R4644       kbugenhagen  Modified to work with NCP perspective
 * 11/19/2015   R13133      kbugenhagen  Added sampling capability.
 * 
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1.0
 */

public class ViirsResource extends
        AbstractNatlCntrsResource<ViirsResourceData, NCMapDescriptor> implements
        INatlCntrsResource, IResourceDataChanged, ImageProvider,
        ISamplingResource {

    private ViirsResourceData viirsResourceData;

    protected ResourcePair cbarRscPair;

    protected ColorBarResource cbarResource;

    /**
     * Map for data records to renderable data, synchronized on for painting,
     * disposing, adding, and removing
     */
    private Map<VIIRSDataRecord, RecordData> dataRecordMap;

    private String name;

    private boolean sampling = false;

    static final String SAMPLING_METHOD_NAME = "findBestValueForCoordinate";

    private final IInputHandler inputAdapter = getViirsInputHandler();

    protected ReferencedCoordinate sampleCoord;

    protected Coordinate virtualCursor;// virtual cursor location

    private class VIIRSTileImageCreator implements TileImageCreator {

        private VIIRSDataRecord record;

        private VIIRSTileImageCreator(VIIRSDataRecord record) {
            this.record = record;
        }

        @Override
        public DrawableImage createTileImage(IGraphicsTarget target, Tile tile,
                GeneralGridGeometry targetGeometry) throws VizException {
            IImage image = target
                    .getExtension(IColormappedImageExtension.class)
                    .initializeRaster(
                            new VIIRSDataCallback(record, tile.tileLevel,
                                    tile.getRectangle()),
                            getCapability(ColorMapCapability.class)
                                    .getColorMapParameters());
            IMesh mesh = target.getExtension(IMapMeshExtension.class)
                    .constructMesh(tile.tileGeometry, targetGeometry);
            return new DrawableImage(image, new PixelCoverage(mesh),
                    RasterMode.ASYNCHRONOUS);
        }
    }

    /**
     * Every {@link VIIRSDataRecord} will have a corresponding RecordData object
     */
    private class RecordData extends
            AbstractSatelliteRecordData<VIIRSDataRecord> {

        private double INTERSECTION_FACTOR = 10.0;

        public RecordData(VIIRSDataRecord dataRecord) {
            this.tileSet = new TileSetRenderable(
                    getCapability(ImagingCapability.class), dataRecord
                            .getCoverage().getGridGeometry(),
                    new VIIRSTileImageCreator(dataRecord),
                    dataRecord.getLevels(), 256);
            this.resolution = Math.min(dataRecord.getCoverage().getDx(),
                    dataRecord.getCoverage().getDy()) * INTERSECTION_FACTOR;
            this.project = true;
            this.setRecord(dataRecord);
        }

        protected double getIntersectionFactor() {
            return INTERSECTION_FACTOR;
        }

        public double interrogate(Coordinate latLon) throws VizException {
            return tileSet.interrogate(latLon);
        }

        public VIIRSDataRecord getRecord() {
            return record;
        }

        public void setRecord(VIIRSDataRecord record) {
            this.record = record;
        }

    }

    // =================================================================

    protected class FrameData extends AbstractFrameData {

        RecordData recordData;

        // Map of frametimes to RecordData records, which contain images.
        // Each VIIRSDataRecord that matches a frame's time is used to create a
        // RecordData object. RecordData objects are responsible for creating
        // the images (tilesets) that get grouped together for a frame.
        Map<DataTime, List<RecordData>> recordDataMap;

        private String legendStr = "No Data";

        public FrameData(DataTime frameTime, int timeInt) {
            super(frameTime, timeInt);
            recordDataMap = new HashMap<DataTime, List<RecordData>>();
            recordDataMap.put(frameTime, new ArrayList<RecordData>());
        }

        public boolean updateFrameData(IRscDataObject rscDataObj) {

            PluginDataObject pdo = ((DfltRecordRscDataObj) rscDataObj).getPDO();
            VIIRSDataRecord record = (VIIRSDataRecord) pdo;

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

        public String getLegendForFrame() {
            return legendStr;
        }

        public void setLegendForFrame(VIIRSDataRecord rec) {
            name = "NPP VIIRS";
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
     * Create a VIIRS resource.
     * 
     * @throws VizException
     */
    public ViirsResource(ViirsResourceData resourceData,
            LoadProperties loadProperties) throws VizException {
        super(resourceData, loadProperties);
        dataRecordMap = new LinkedHashMap<VIIRSDataRecord, RecordData>();
        resourceData.addChangeListener(this);
        viirsResourceData = (ViirsResourceData) resourceData;
    }

    /**
     * Add a data record to be displayed. Should be synchronized on
     * {@link #dataMap} when called
     * 
     * @param dataRecord
     * @throws VizException
     */
    protected RecordData addRecord(VIIRSDataRecord dataRecord)
            throws VizException {

        RecordData recordData = null;
        if (name == null) {
            createColorMap(dataRecord);
        }
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
     * Create color map from a record
     * 
     * @param dataRecord
     * @throws VizException
     */
    protected void createColorMap(VIIRSDataRecord dataRecord)
            throws VizException {

        name = "NPP VIIRS";
        // First record, process name and parameters
        if (dataRecord.getChannelType() != null) {
            name += " " + dataRecord.getChannelType();
        }
        String parameter = dataRecord.getParameter();
        if (dataRecord.getWavelength() != null) {
            parameter = dataRecord.getWavelength() + parameter;
        }
        name += " " + parameter;

        // Get style rule preferences

        StyleRule styleRule = null;
        try {
            styleRule = StyleManager.getInstance().getStyleRule(
                    StyleType.IMAGERY, new VIIRSDataRecordCriteria(dataRecord));

        } catch (StyleException e) {
            throw new VizException(e.getLocalizedMessage(), e);
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
                if (name == null) {
                    // no preference, absolute default colormap
                    name = "IR Default";
                }
            }
            colorMapParameters.setColorMap(ColorMapLoader.loadColorMap(name));
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
                    VIIRSDataRecord.getDataSet(0))[0];
            Map<String, Object> attrs = record.getDataAttributes();
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
                        dataUnit = UnitFormat.getUCUMInstance().parseObject(
                                unitStr, new ParsePosition(0));
                    } catch (Exception e) {
                        e.printStackTrace();
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

        if (colorMapParameters.getPersisted() != null) {
            colorMapParameters.applyPersistedParameters(colorMapParameters
                    .getPersisted());
        }

        getCapability(ColorMapCapability.class).setColorMapParameters(
                colorMapParameters);

        resourceChanged(ChangeType.CAPABILITY,
                getCapability(ColorMapCapability.class));

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
            VIIRSDataRecord[] records = new VIIRSDataRecord[1];
            if (object instanceof PluginDataObject[]) {
                PluginDataObject[] pdos = (PluginDataObject[]) object;
                records = new VIIRSDataRecord[pdos.length];
                for (int i = 0; i < pdos.length; ++i) {
                    records[i] = (VIIRSDataRecord) pdos[i];
                }
            } else if (object instanceof VIIRSDataRecord) {
                records[0] = (VIIRSDataRecord) object;
            }
        } else if (type != null && type == ChangeType.CAPABILITY) {
            if (object instanceof ImagingCapability) {
                ImagingCapability imgCap = getCapability(ImagingCapability.class);
                ImagingCapability newImgCap = (ImagingCapability) object;
                imgCap.setBrightness(newImgCap.getBrightness(), false);
                imgCap.setContrast(newImgCap.getContrast(), false);
                imgCap.setAlpha(newImgCap.getAlpha(), false);
                viirsResourceData.setAlpha(imgCap.getAlpha());
                viirsResourceData.setBrightness(imgCap.getBrightness());
                viirsResourceData.setContrast(imgCap.getContrast());
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
                viirsResourceData.setColorMapName(colorMapName);
                viirsResourceData.getRscAttrSet().setAttrValue("colorMapName",
                        colorMapName);
                ColorBarFromColormap cBar = viirsResourceData.getColorBar();
                cBar.setColorMap(theColorMap);
                viirsResourceData.getRscAttrSet()
                        .setAttrValue("colorBar", cBar);
                viirsResourceData.setIsEdited(true);
                issueRefresh();
            }
        }
        issueRefresh();

    }

    public void initResource(IGraphicsTarget target) throws VizException {
        cbarRscPair = ResourcePair
                .constructSystemResourcePair(new ColorBarResourceData(
                        viirsResourceData.getColorBar()));
        getDescriptor().getResourceList().add(cbarRscPair);
        getDescriptor().getResourceList().instantiateResources(getDescriptor(),
                true);
        cbarResource = (ColorBarResource) cbarRscPair.getResource();
        getCapability(ImagingCapability.class).setProvider(this);

        IDisplayPaneContainer container = getResourceContainer();
        if (container != null) {
            container
                    .registerMouseHandler(inputAdapter, InputPriority.RESOURCE);
        }

        queryRecords();

    }

    protected void paintFrame(AbstractFrameData frameData,
            IGraphicsTarget target, PaintProperties paintProps)
            throws VizException {

        FrameData currFrame = (FrameData) frameData;
        List<Collection<DrawableImage>> allImagesForframe = getAllImages(
                currFrame, target, paintProps);

        for (Collection<DrawableImage> images : allImagesForframe) {
            if (images != null && images.size() > 0) {
                if (!target.drawRasters(paintProps,
                        images.toArray(new DrawableImage[images.size()]))) {
                    issueRefresh();
                }
            }
        }
        if (isSampling()) {
            ColorMapParameters colorMapParameters = getCapability(
                    ColorMapCapability.class).getColorMapParameters();
            Sampler.INSTANCE.createSampler(descriptor, colorMapParameters,
                    SAMPLING_METHOD_NAME);
            Sampler.INSTANCE.paintResult(target, paintProps, sampleCoord);
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
                        "Error reprojecting VIIRS data", e);
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

        IDisplayPaneContainer container = getResourceContainer();
        if (container != null) {
            container.unregisterMouseHandler(inputAdapter);
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
            for (AbstractSatelliteRecordData<VIIRSDataRecord> recordData : recordDataList) {
                if (recordData != null) {
                    Collection<DrawableImage> recordImages = recordData
                            .getImagesToRender(target, paintProps,
                                    descriptor.getGridGeometry());
                    images.add(recordImages);
                }
            }
        }
        return images;

    }

    @Override
    public void propertiesChanged(ResourceProperties updatedProps) {
        if (cbarRscPair != null) {
            cbarRscPair.getProperties().setVisible(updatedProps.isVisible());
        }
    }

    public String getName() {
        if (name == null) {
            return "NPP VIIRS";
        }
        return name;
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

    public IInputHandler getViirsInputHandler() {
        return new ViirsInputAdapter(this);
    }

    public boolean isSampling() {
        return sampling;
    }

    public void setSampling(boolean sampling) {
        this.sampling = sampling;
    }

    /*
     * Uses TilesetRenderable interrogation to find the closest data value
     * corresponding to the coordinate.
     * 
     * @param p image point used to determine which data record contains it
     * 
     * @param latLon lat/lon coordinate used in TilesetRenderable interrogation
     * 
     * @returns map collection mapping interrogation ID to the closest data
     * value
     */
    public Map<String, Object> findBestValueForCoordinate(Point p,
            Coordinate latLon) throws VizException {

        Map<String, Object> interMap = new HashMap<String, Object>();
        ColorMapParameters colorMapParameters = getCapability(
                ColorMapCapability.class).getColorMapParameters();
        double noDataValue = colorMapParameters.getNoDataValue();
        double bestValue = Double.NaN;
        VIIRSDataRecord bestRecord = null;
        List<RecordData> recordDataList = getRecordDataList((FrameData) getCurrentFrame());

        if (recordDataList != null) {
            for (RecordData data : recordDataList) {
                if (data != null && data.contains(p)) {
                    double value;
                    value = data.interrogate(latLon);
                    if (Double.isNaN(value) == false && value != noDataValue) {
                        bestValue = value;
                        bestRecord = data.getRecord();
                    }
                }
            }
        }
        double dataValue = Double.NaN;
        if (Double.isNaN(bestValue) == false) {
            dataValue = colorMapParameters.getDataToDisplayConverter().convert(
                    bestValue);
        }
        if (bestRecord != null) {
            interMap.put(IGridGeometryProvider.class.toString(), bestRecord);
        }
        interMap.put(AbstractSatelliteRecordData.SATELLITE_DATA_INTERROGATE_ID,
                Measure.valueOf(dataValue, colorMapParameters.getDisplayUnit()));
        return interMap;
    }

    protected List<RecordData> getRecordDataList(FrameData frameData) {
        return frameData.getRecordDataMap().get(frameData.getFrameTime());
    }

}