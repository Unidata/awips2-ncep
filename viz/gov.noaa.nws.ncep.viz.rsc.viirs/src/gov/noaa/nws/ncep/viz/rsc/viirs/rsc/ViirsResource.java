package gov.noaa.nws.ncep.viz.rsc.viirs.rsc;

import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.colorBar.ColorBarResource;
import gov.noaa.nws.ncep.viz.resources.colorBar.ColorBarResourceData;
import gov.noaa.nws.ncep.viz.ui.display.ColorBarFromColormap;
import gov.noaa.nws.ncep.viz.ui.display.NCMapDescriptor;

import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.measure.converter.UnitConverter;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;

import org.geotools.coverage.grid.GeneralGridGeometry;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.raytheon.uf.common.colormap.ColorMap;
import com.raytheon.uf.common.colormap.prefs.ColorMapParameters;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.npp.viirs.VIIRSDataRecord;
import com.raytheon.uf.common.datastorage.Request;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.geospatial.util.EnvelopeIntersection;
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
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.RasterMode;
import com.raytheon.uf.viz.core.IMesh;
import com.raytheon.uf.viz.core.PixelCoverage;
import com.raytheon.uf.viz.datacube.DataCubeContainer;
import com.raytheon.uf.viz.core.drawables.ColorMapLoader;
import com.raytheon.uf.viz.core.drawables.IImage;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.drawables.ext.IImagingExtension.ImageProvider;
import com.raytheon.uf.viz.core.drawables.ext.colormap.IColormappedImageExtension;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.IMapMeshExtension;
import com.raytheon.uf.viz.core.rsc.IResourceDataChanged;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.ResourceProperties;
import com.raytheon.uf.viz.core.rsc.capabilities.ColorMapCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.ImagingCapability;
import com.raytheon.uf.viz.core.tile.Tile;
import com.raytheon.uf.viz.core.tile.TileSetRenderable;
import com.raytheon.uf.viz.core.tile.TileSetRenderable.TileImageCreator;
import com.raytheon.uf.viz.npp.viirs.rsc.VIIRSDataCallback;
import com.raytheon.uf.viz.npp.viirs.style.VIIRSDataRecordCriteria;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;

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
 * Jun 23, 2014            Yukuan Song Initial creation
 * Aug 20, 2014 R4644      kbugenhagen Modified to work with NCP perspective
 * 
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1.0
 */

public class ViirsResource extends
        AbstractNatlCntrsResource<ViirsResourceData, NCMapDescriptor> implements
        INatlCntrsResource, IResourceDataChanged, ImageProvider {

    private ViirsResourceData viirsResourceData;

    protected ResourcePair cbarRscPair;

    protected ColorBarResource cbarResource;

    /**
     * Map for data records to renderable data, synchronized on for painting,
     * disposing, adding, and removing
     */
    private Map<VIIRSDataRecord, RecordData> dataRecordMap;

    private String name;

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
    private class RecordData {

        private double INTERSECTION_FACTOR = 10.0;

        /** Intersection geometry for the target */
        private List<PreparedGeometry> targetIntersection;

        /** Flag designated if a project call is required next paint */
        private boolean project;

        /** Renderable for the data record */
        private TileSetRenderable tileSet;

        private final double resolution;

        public RecordData(VIIRSDataRecord dataRecord) {
            this.tileSet = new TileSetRenderable(
                    getCapability(ImagingCapability.class), dataRecord
                            .getCoverage().getGridGeometry(),
                    new VIIRSTileImageCreator(dataRecord),
                    dataRecord.getLevels(), 256);
            this.resolution = Math.min(dataRecord.getCoverage().getDx(),
                    dataRecord.getCoverage().getDy()) * INTERSECTION_FACTOR;
            this.project = true;
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

    }

    // =================================================================

    protected class FrameData extends AbstractFrameData {

        RecordData recordData;

        // Map of frametimes to RecordData records, which contain images.
        // Each VIIRSDataRecord that matches a frame's time is used to create a
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

}