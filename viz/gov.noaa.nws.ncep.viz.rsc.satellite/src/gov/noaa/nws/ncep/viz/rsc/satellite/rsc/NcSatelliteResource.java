package gov.noaa.nws.ncep.viz.rsc.satellite.rsc;

import gov.noaa.nws.ncep.viz.common.ColorMapUtil;
import gov.noaa.nws.ncep.viz.common.area.AreaName.AreaSource;
import gov.noaa.nws.ncep.viz.common.area.IAreaProviderCapable;
import gov.noaa.nws.ncep.viz.common.ui.NmapCommon;
import gov.noaa.nws.ncep.viz.localization.NcPathManager;
import gov.noaa.nws.ncep.viz.localization.NcPathManager.NcPathConstants;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.colorBar.ColorBarResource;
import gov.noaa.nws.ncep.viz.resources.colorBar.ColorBarResourceData;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceCategory;
import gov.noaa.nws.ncep.viz.rsc.satellite.rsc.SatelliteResourceData.SatelliteType;
import gov.noaa.nws.ncep.viz.rsc.satellite.units.NcIRPixelToTempConverter;
import gov.noaa.nws.ncep.viz.rsc.satellite.units.NcIRTempToPixelConverter;
import gov.noaa.nws.ncep.viz.ui.display.ColorBarFromColormap;
import gov.noaa.nws.ncep.viz.ui.display.NCMapDescriptor;

import java.io.File;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.measure.converter.UnitConverter;
import javax.measure.quantity.Temperature;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;
import javax.xml.bind.JAXBException;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.raytheon.uf.common.colormap.ColorMap;
import com.raytheon.uf.common.colormap.prefs.ColorMapParameters;
import com.raytheon.uf.common.colormap.prefs.DataMappingPreferences;
import com.raytheon.uf.common.colormap.prefs.DataMappingPreferences.DataMappingEntry;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.satellite.SatMapCoverage;
import com.raytheon.uf.common.dataplugin.satellite.SatelliteRecord;
import com.raytheon.uf.common.dataplugin.satellite.units.goes.PolarPrecipWaterPixel;
import com.raytheon.uf.common.dataplugin.satellite.units.ir.IRPixel;
import com.raytheon.uf.common.dataplugin.satellite.units.water.BlendedTPWPixel;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.geospatial.ReferencedCoordinate;
import com.raytheon.uf.common.serialization.JAXBManager;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.jaxb.JAXBClassLocator;
import com.raytheon.uf.common.serialization.jaxb.JaxbDummyObject;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.style.AbstractStylePreferences;
import com.raytheon.uf.common.style.LabelingPreferences;
import com.raytheon.uf.common.style.MatchCriteria;
import com.raytheon.uf.common.style.ParamLevelMatchCriteria;
import com.raytheon.uf.common.style.StyleException;
import com.raytheon.uf.common.style.StyleRule;
import com.raytheon.uf.common.style.StyleRuleset;
import com.raytheon.uf.common.style.image.DataScale;
import com.raytheon.uf.common.style.image.DataScale.Type;
import com.raytheon.uf.common.style.image.ImagePreferences;
import com.raytheon.uf.common.style.image.SamplePreferences;
import com.raytheon.uf.common.style.level.Level;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.DrawableImage;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.drawables.IRenderable;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.reflect.SubClassLocator;
import com.raytheon.uf.viz.core.rsc.IResourceDataChanged;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.ResourceProperties;
import com.raytheon.uf.viz.core.rsc.capabilities.ColorMapCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.ImagingCapability;
import com.raytheon.uf.viz.core.tile.RecordTileSetRenderable;
import com.raytheon.viz.satellite.SatelliteConstants;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * Provides satellite raster rendering support through the use of
 * TileSetRenderables. This class was developed from a hybrid of code from
 * Raytheon's SatResource, NCEP's AbstractSatelliteResource, and NCEP's
 * GiniSatResource. Therefore you can trace much of the logic in here to one of
 * those three classes.
 * 
 * TODO: The Raytheon SatRenderable and/or SatTileSetRenderable should become
 * more reusable and/or extendable to eliminate duplicate code.
 * 
 * TODO: The NCEP AbstractSatelliteResource and/or GiniSatResource should be
 * slimmed down or joined to this class's type hierarchy to eliminate duplicate
 * code.
 * 
 * <pre>
 * 
 *  SOFTWARE HISTORY
 * 
 *  Date         Ticket#    Engineer    Description
 *  ------------ ---------- ----------- --------------------------
 *  09/28/2015   R11385     njensen     Initial creation
 *  10/28/2015   R11385     kbugenhagen Updated to use stylerules to generate colormap
 *  01/18/2016   ------     mjames@ucar Check common_static if ncpath is null
 * 
 * </pre>
 * 
 */
public class NcSatelliteResource extends
        AbstractNatlCntrsResource<SatelliteResourceData, NCMapDescriptor>
        implements IResourceDataChanged, IAreaProviderCapable {

    private static IUFStatusHandler logger = UFStatus
            .getHandler(NcSatelliteResource.class);

    private static final String CREATING_ENTITY_KEY = "creatingEntity";

    private static final String SECTOR_ID_KEY = "sectorID";

    static final String DEFAULT_COLORMAP_NAME = "colorMapName";

    private static JAXBManager jaxb;

    protected String legendStr = "Satellite";

    protected String physicalElement;

    protected ColorBarResource cbarResource;

    protected volatile boolean initialized = false;

    // slimmed down variation on what was in AbstractSatelliteResource
    protected class FrameData extends AbstractFrameData {

        // one renderable per frame, each renderable may have multiple tiles
        SatRenderable renderable;

        protected FrameData(DataTime time, int interval) {
            super(time, interval);
            renderable = new SatRenderable();
        }

        @Override
        public void dispose() {
            renderable.dispose();
        }

        @Override
        public boolean updateFrameData(IRscDataObject rscDataObj) {
            SatelliteRecord satRec = (SatelliteRecord) ((DfltRecordRscDataObj) rscDataObj)
                    .getPDO();
            if (!initialized) {
                synchronized (NcSatelliteResource.this) {
                    if (!initialized) {
                        try {
                            initializeFirstFrame(satRec);
                        } catch (Exception e) {
                            logger.error(
                                    "Error initializing against SatelliteRecord "
                                            + satRec, e);
                            return false;
                        }
                    }
                }
            }

            Collections.sort(NcSatelliteResource.this.dataTimes);
            int imgNum = -1;
            generateAndStoreColorBarLabelingInformation(satRec, imgNum);
            renderable.addRecord(satRec);

            return true;
        }
    }

    /**
     * Renderable for displaying satellite data. There is one renderable per
     * frame but potentially multiple tiles per renderable.
     * 
     * This class is almost an exact copy of
     * com.raytheon.viz.satellite.rsc.SatResource.SatRenderable. The differences
     * are only in addRecord() and interrogate(). See TODO above.
     */
    protected class SatRenderable implements IRenderable {

        private final Map<SatMapCoverage, RecordTileSetRenderable> tileMap = new HashMap<>();

        @Override
        public void paint(IGraphicsTarget target, PaintProperties paintProps)
                throws VizException {
            Collection<DrawableImage> images = getImagesToRender(target,
                    paintProps);
            if (images.isEmpty() == false) {
                target.drawRasters(paintProps,
                        images.toArray(new DrawableImage[0]));
            }
        }

        public Collection<DrawableImage> getImagesToRender(
                IGraphicsTarget target, PaintProperties paintProps)
                throws VizException {
            List<DrawableImage> images = new ArrayList<DrawableImage>();
            synchronized (tileMap) {
                for (RecordTileSetRenderable renderable : tileMap.values()) {
                    images.addAll(renderable.getImagesToRender(target,
                            paintProps));
                }
            }
            return images;
        }

        public void addRecord(SatelliteRecord record) {
            synchronized (tileMap) {
                RecordTileSetRenderable tileSet = tileMap.get(record
                        .getCoverage());
                if (tileSet == null) {
                    tileSet = new RecordTileSetRenderable(
                            NcSatelliteResource.this, record,
                            record.getGridGeometry(),
                            record.getInterpolationLevels() + 1);
                    tileSet.project(descriptor.getGridGeometry());
                    tileMap.put(record.getCoverage(), tileSet);
                }
            }
        }

        public void project() {
            synchronized (tileMap) {
                for (RecordTileSetRenderable renderable : tileMap.values()) {
                    renderable.project(descriptor.getGridGeometry());
                }
            }
        }

        public void dispose() {
            synchronized (tileMap) {
                for (RecordTileSetRenderable renderable : tileMap.values()) {
                    renderable.dispose();
                }
                tileMap.clear();
            }
        }

        public Double interrogate(Coordinate latLon, Unit<?> requestUnit)
                throws VizException {
            synchronized (tileMap) {
                for (RecordTileSetRenderable renderable : tileMap.values()) {
                    double rValue = renderable.interrogate(latLon, requestUnit);
                    if (!Double.isNaN(rValue)) {
                        return rValue;
                    }
                }
            }
            return null;
        }
    }

    /**
     * NOTE: logic copied from GiniSatResource constructor
     */
    public NcSatelliteResource(SatelliteResourceData data, LoadProperties props) {
        super(data, props);
        resourceData.addChangeListener(this);

        if (resourceData.getMetadataMap().containsKey(CREATING_ENTITY_KEY)
                && resourceData.getMetadataMap().containsKey(SECTOR_ID_KEY)) {

            legendStr = resourceData.getMetadataMap().get(CREATING_ENTITY_KEY)
                    .getConstraintValue()
                    + " "
                    + resourceData.getMetadataMap().get(SECTOR_ID_KEY)
                            .getConstraintValue()
                    + " "
                    + resourceData.getRscAttrSet().getRscAttrSetName();

            legendStr = legendStr.replace('%', ' ');
        }
    }

    @Override
    public String getName() {
        FrameData fd = (FrameData) getCurrentFrame();
        if (fd == null || fd.getFrameTime() == null
                || descriptor.getFramesInfo().getFrameCount() == 0) {
            return legendStr + "-No Data";

        }

        return legendStr + " "
                + NmapCommon.getTimeStringFromDataTime(fd.getFrameTime(), "/");
    }

    @Override
    protected AbstractFrameData createNewFrame(DataTime frameTime,
            int frameInterval) {
        return new FrameData(frameTime, frameInterval);
    }

    private void initializeFirstFrame(SatelliteRecord record)
            throws VizException {
        initialized = true;

        physicalElement = record.getPhysicalElement();

        // create the colorMap and set it in the colorMapParameters
        ImagePreferences preferences = getStyleRulePreferences(record);
        ColorMapParameters colorMapParameters = loadColorMapParameters(record,
                preferences);
        getCapability(ColorMapCapability.class).setColorMapParameters(
                colorMapParameters);
    }

    /**
     * Retrieves style rule matching data record parameters
     * 
     * @param dataRecord
     *            MODIS data record
     * @return The preferences
     * @throws VizException
     */
    protected ImagePreferences getStyleRulePreferences(
            SatelliteRecord dataRecord) throws VizException {
        ImagePreferences preferences = null;
        String styleRuleFile = getLocFilePathForImageryStyleRule(getResourceData()
                .getSatelliteType());
        File file = NcPathManager.getInstance().getStaticFile(styleRuleFile);
        StyleRule styleRule = null;
        List<String> paramList = getParameterList(dataRecord);
        ParamLevelMatchCriteria matchCriteria = new ParamLevelMatchCriteria();
        matchCriteria.setParameterName(paramList);

        try {
            // Using old method of getting style rules since himawari style
            // rules file already exists in D2D and its values do not work
            // in NCP perspective.
            StyleRuleset styleSet = (StyleRuleset) getJaxbManager()
                    .unmarshalFromXmlFile(StyleRuleset.class, file);
            if (styleSet != null) {
                List<StyleRule> styleRuleList = styleSet.getStyleRules();
                for (StyleRule sr : styleRuleList) {
                    MatchCriteria styleMatchCriteria = sr.getMatchCriteria();
                    Integer matchValue = styleMatchCriteria.matches(matchCriteria);
                    if (styleMatchCriteria.matches(matchCriteria) > 0) {
                        styleRule = sr;
                        break;
                    }
                }
            }
        } catch (StyleException e) {
            throw new VizException(e.getLocalizedMessage(), e);
        } catch (SerializationException | JAXBException e) {
            statusHandler.handle(UFStatus.Priority.PROBLEM,
                    e.getLocalizedMessage(), e);
        }

        if (styleRule != null) {
            preferences = (ImagePreferences) styleRule.getPreferences();
        }

        return preferences;
    }

    protected List<String> getParameterList(PluginDataObject pdo) {
        List<String> paramList = new ArrayList<String>(0);
        String paramStr = ((SatelliteRecord) pdo).getPhysicalElement();
        paramList.add(paramStr);
        return paramList;
    }

    /**
     * Loads color map parameters from preferences.
     * 
     * @param dataRecord
     *            MODIS data record
     * @param preferences
     *            preferences read from style rule
     * @return colormap parameters
     * @throws VizException
     */
    private ColorMapParameters loadColorMapParameters(
            SatelliteRecord dataRecord, ImagePreferences preferences)
            throws VizException {
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
            ColorMap colorMap = (ColorMap) ColorMapUtil.loadColorMap(
                    resourceData.getResourceName().getRscCategory()
                            .getCategoryName(), resourceData.getColorMapName());
            colorMapParameters.setColorMap(colorMap);
        }

        try {
            setColorMapUnits(dataRecord, preferences, colorMapParameters);
        } catch (StyleException e) {
            throw new VizException(e.getLocalizedMessage(), e);
        }

        return colorMapParameters;
    }

    private void setColorMapUnits(SatelliteRecord record,
            ImagePreferences preferences, ColorMapParameters colorMapParameters)
            throws StyleException {

        Unit<?> colorMapUnit = null;
        try {
            colorMapUnit = preferences.getColorMapUnitsObject();
        } catch (StyleException e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
        }
        if (colorMapUnit == null) {
            colorMapUnit = getRecordUnit(record);
        }

        Unit<?> displayUnit = preferences.getDisplayUnits();
        if (displayUnit == null) {
            displayUnit = colorMapUnit;
        } else if (colorMapUnit == null) {
            colorMapUnit = displayUnit;
        }

        colorMapParameters.setColorMapUnit(new IRPixel());
        colorMapParameters.setDisplayUnit(displayUnit);
        colorMapParameters.setDataMapping(preferences.getDataMapping());
        colorMapParameters.setColorMapName(preferences.getDefaultColormap());

        Float displayMin = null, displayMax = null;
        DataScale scale = preferences.getDataScale();
        boolean mirrored = false;
        Type scaleType = Type.LINEAR;

        if (scale != null) {
            if (scale.getMinValue() != null) {
                displayMin = scale.getMinValue().floatValue();
            }
            if (scale.getMaxValue() != null) {
                displayMax = scale.getMaxValue().floatValue();
            }
            mirrored = scale.isMirror();
            scaleType = scale.getScaleType();
        }

        if (displayMin == null || displayMax == null) {
            // Could not find min/max in DataScale, attempt to get from
            // DataMappingPreferences if set
            if (colorMapParameters.getDataMapping() != null) {
                DataMappingPreferences mapping = colorMapParameters
                        .getDataMapping();
                List<DataMappingEntry> entries = mapping.getEntries();
                if (entries != null && entries.isEmpty() == false) {
                    if (displayMin == null) {
                        DataMappingEntry min = entries.get(0);
                        if (min.getPixelValue() != null
                                && min.getDisplayValue() != null) {
                            displayMin = min.getDisplayValue().floatValue();
                        }
                    }
                    if (displayMax == null) {
                        DataMappingEntry max = entries.get(entries.size() - 1);
                        if (max.getPixelValue() != null
                                && max.getDisplayValue() != null) {
                            displayMax = max.getDisplayValue().floatValue();
                        }
                    }
                }
            } else if (mirrored && (displayMin != null || displayMax != null)) {
                // Mirror value set
                if (displayMin == null) {
                    displayMin = -displayMax;
                } else {
                    displayMax = -displayMin;
                }
            }
            if (displayMin == null || displayMax == null) {
                throw new StyleException(
                        "Unable to determine colormap min/max.");
            }
        }

        colorMapParameters.setMirror(mirrored);
        colorMapParameters.setLogarithmic(scaleType == Type.LOG);

        // Convert to colormap min/max
        float colorMapMin = displayMin;
        float colorMapMax = displayMax;

        UnitConverter displayToColorMap = colorMapParameters
                .getDisplayToColorMapConverter();
        if (displayToColorMap != null) {
            colorMapMin = (float) displayToColorMap.convert(displayMin);
            colorMapMax = (float) displayToColorMap.convert(displayMax);
        }

        colorMapParameters.setColorMapMin(colorMapMin);
        colorMapParameters.setColorMapMax(colorMapMax);

        LabelingPreferences labeling = preferences.getColorbarLabeling();
        if (labeling != null) {
            if (labeling.getValues() != null) {
                colorMapParameters.setColorBarIntervals(labeling.getValues());
            } else if (labeling.getIncrement() != 0) {
                float increment = labeling.getIncrement();
                float initialPoint = (float) (Math
                        .ceil(colorMapMin / increment) * increment);
                float finalPoint = (float) (Math.floor(colorMapMin / increment) * increment);
                int count = (int) ((finalPoint - initialPoint) / increment) + 1;
                float[] vals = new float[count];
                for (int i = 0; i < count; i += 1) {
                    vals[i] = initialPoint + increment * i;
                }
            }
        }
    }

    public static Unit<?> getRecordUnit(SatelliteRecord record) {
        Unit<?> recordUnit = null;
        String physicalElement = record.getPhysicalElement();

        if (record.getUnits() != null && record.getUnits().isEmpty() == false) {
            try {
                recordUnit = UnitFormat.getUCUMInstance().parseProductUnit(
                        record.getUnits(), new ParsePosition(0));
            } catch (ParseException e) {
                statusHandler
                        .handle(Priority.PROBLEM,
                                "Unable to parse satellite units: "
                                        + record.getUnits(), e);
            }
        }

        if (physicalElement.equals(SatelliteConstants.PRECIP)) {
            String creatingEntity = record.getCreatingEntity();
            if (creatingEntity.equals(SatelliteConstants.DMSP)
                    || creatingEntity.equals(SatelliteConstants.POES)) {
                recordUnit = new PolarPrecipWaterPixel();
            } else if (creatingEntity.equals(SatelliteConstants.MISC)) {
                recordUnit = new BlendedTPWPixel();
            }
        }

        return recordUnit;
    }

    public static Unit<?> getDataUnit(Unit<?> recordUnit, IDataRecord dataRecord) {
        Unit<?> units = recordUnit != null ? recordUnit : Unit.ONE;
        Map<String, Object> attrs = dataRecord.getDataAttributes();
        if (attrs != null) {
            Number offset = (Number) attrs.get(SatelliteRecord.SAT_ADD_OFFSET);
            Number scale = (Number) attrs.get(SatelliteRecord.SAT_SCALE_FACTOR);

            if (offset != null) {
                double offsetVal = offset.doubleValue();
                if (offsetVal != 0.0) {
                    units = units.plus(offsetVal);
                }
            }
            if (scale != null) {
                double scaleVal = scale.doubleValue();
                if (scaleVal != 0.0) {
                    units = units.times(scaleVal);
                }
            }
        }
        return units;
    }

    protected List<String> getParameterList(SatelliteRecord dataRecord) {
        List<String> paramList = Arrays
                .asList(new String[] { physicalElement });

        return paramList;
    }

    protected String getLocFilePathForImageryStyleRule(SatelliteType type) {
        if (type == SatelliteType.HIMAWARI) {
            return NcPathConstants.HIMAWARI_IMG_STYLE_RULES;
        } else {
            return NcPathConstants.GINI_IMG_STYLE_RULES;
        }
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
    public void disposeInternal() {
        super.disposeInternal(); // dispose of the frameData
        ResourcePair cbarPair = null;
        for (ResourcePair rp : descriptor.getResourceList()) {
            if (rp != null && rp.getResource() == cbarResource) {
                cbarPair = rp;
                break;
            }
        }
        if (cbarPair != null) {
            descriptor.getResourceList().remove(cbarPair);
        }
        cbarResource.dispose();
    }

    @Override
    public void initResource(IGraphicsTarget target) throws VizException {

        // Create the colorBar resource and add it to the resourceList for this
        // descriptor
        ResourcePair cbarRscPair = ResourcePair
                .constructSystemResourcePair(new ColorBarResourceData(
                        resourceData.getColorBar()));
        getDescriptor().getResourceList().add(cbarRscPair);
        getDescriptor().getResourceList().instantiateResources(getDescriptor(),
                true);
        cbarResource = (ColorBarResource) cbarRscPair.getResource();

        queryRecords();
    }

    @Override
    /*
     * Everything after this point is an exact copy or almost an exact copy of
     * code from AbstractSatelliteResource. isCloudHeightCompatible() comes from
     * GiniSatResource
     */
    public void paintFrame(AbstractFrameData frmData, IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {
        FrameData currFrame = (FrameData) frmData;

        SatRenderable satr = currFrame.renderable;

        if (satr != null) {
            ImagingCapability imgCap = new ImagingCapability();
            imgCap.setBrightness(resourceData.getBrightness());
            imgCap.setContrast(resourceData.getContrast());
            imgCap.setAlpha(resourceData.getAlpha());
            paintProps.setAlpha(resourceData.getAlpha());

            ColorMapParameters params = getCapability(ColorMapCapability.class)
                    .getColorMapParameters();
                String colorMapName = resourceData.getColorMapName();
                if (colorMapName == null) {
                    colorMapName = "Sat/VIS/ZA (Vis Default)";
                }
                
                // the D2D name is sent here, which was unaccounted for in ColorMapUtil
                params.setColorMap(ColorMapUtil.loadColorMap(
                        ResourceCategory.SatelliteRscCategory.getCategoryName(),
                        colorMapName));

            satr.paint(target, paintProps);
        }
    }

    @Override
    public void project(CoordinateReferenceSystem mapData) throws VizException {
        for (AbstractFrameData frm : frameDataMap.values()) {
            SatRenderable sr = ((FrameData) frm).renderable;
            if (sr != null) {
                sr.project();
            }
        }
    }

    @Override
    public String inspect(ReferencedCoordinate coord) throws VizException {
        Double value = inspectValue(coord);
        if (value == null) {
            return "NO DATA";
        }

        ColorMapParameters cmp = getCapability(ColorMapCapability.class)
                .getColorMapParameters();
        Unit<?> unit = cmp.getDisplayUnit();
        float[] intervals = cmp.getColorBarIntervals();
        if (intervals != null) {
            float f1 = intervals[0];
            float f2 = intervals[intervals.length - 1];
            if (value > f1 && value > f2) {
                return String.format(">%.1f%s", Math.max(f1, f2),
                        unit == null ? "" : unit.toString());
            }
            if (value < f1 && value < f2) {
                return String.format("<%.1f%s", Math.min(f1, f2),
                        unit == null ? "" : unit.toString());
            }
        }
        return String.format("%.1f%s", value,
                unit == null ? "" : unit.toString());
    }

    public Double inspectValue(ReferencedCoordinate coord) throws VizException {
        FrameData currFrame = (FrameData) getCurrentFrame();
        if (currFrame == null) {
            return null;
        }

        Coordinate latlon = null;
        try {
            latlon = coord.asLatLon();
        } catch (Exception e) {
            throw new VizException("Error transforming coordinate to lat/lon",
                    e);
        }

        return currFrame.renderable.interrogate(latlon,
                getCapability(ColorMapCapability.class).getColorMapParameters()
                        .getDisplayUnit());
    }

    public boolean isCloudHeightCompatible() {
        return "Imager 11 micron IR".equals(physicalElement);
    }

    public Double getRawIRImageValue(Coordinate latlon) {
        if (!isCloudHeightCompatible()) {
            return null;
        }

        FrameData currFrame = (FrameData) getCurrentFrame();
        if (currFrame == null) {
            return null;
        }

        try {
            return currFrame.renderable.interrogate(latlon, null);
        } catch (VizException e) {
            return null;
        }
    }

    /**
     * Gets the temp in the units set as the display units in the colormap
     * parameters.
     * 
     * @param latlon
     * @return the temperature
     */
    public Double getSatIRTemperature(Coordinate latlon) {
        if (!isCloudHeightCompatible()) {
            return null;
        }

        FrameData currFrame = (FrameData) getCurrentFrame();

        if (currFrame == null) {
            return null;
        }
        try {
            return currFrame.renderable.interrogate(latlon,
                    getCapability(ColorMapCapability.class)
                            .getColorMapParameters().getDisplayUnit());
        } catch (VizException e) {
            e.printStackTrace();
            return null;
        }

    }

    @SuppressWarnings("unchecked")
    public Unit<Temperature> getTemperatureUnits() {
        if (isCloudHeightCompatible()) {
            if (resourceData.getDisplayUnit() == null) {
                return SI.CELSIUS;
            } else {
                return (Unit<Temperature>) resourceData.getDisplayUnit();
            }
        } else {
            return null;
        }
    }

    // the colorBar and/or the colormap may have changed so update the
    // colorBarPainter and the colorMapParametersCapability which holds
    // the instance of the colorMap that Raytheon's code needs
    @Override
    public void resourceAttrsModified() {
        // update the colorbarPainter with a possibly new colorbar
        ColorBarFromColormap colorBar = resourceData.getColorBar();

        ColorMapParameters cmapParams = getCapability(ColorMapCapability.class)
                .getColorMapParameters();
        cmapParams.setColorMap(colorBar.getColorMap());
        cmapParams.setColorMapName(resourceData.getColorMapName());
        // not currently an attribute but could be.
        cmapParams.setDisplayUnit(resourceData.getDisplayUnit());

        getCapability(ColorMapCapability.class).setColorMapParameters(
                cmapParams);
        cbarResource.setColorBar(colorBar);
    }

    public String getLegendString() {
        return legendStr;
    }

    @Override
    public void resourceChanged(ChangeType type, Object object) {
        if (type != null && type == ChangeType.CAPABILITY) {
            if (object instanceof ImagingCapability) {
                ImagingCapability imgCap = getCapability(ImagingCapability.class);
                ImagingCapability newImgCap = (ImagingCapability) object;
                imgCap.setBrightness(newImgCap.getBrightness(), false);
                imgCap.setContrast(newImgCap.getContrast(), false);
                imgCap.setAlpha(newImgCap.getAlpha(), false);
                resourceData.setAlpha(imgCap.getAlpha());
                resourceData.setBrightness(imgCap.getBrightness());
                resourceData.setContrast(imgCap.getContrast());
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
                resourceData.setColorMapName(colorMapName);
                resourceData.getRscAttrSet().setAttrValue("colorMapName",
                        colorMapName);

                ColorBarFromColormap cBar = resourceData.getColorBar();
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
                resourceData.getRscAttrSet().setAttrValue("colorBar", cBar);
                resourceData.setIsEdited(true);
                issueRefresh();
            }
        }
    }

    public void generateAndStoreColorBarLabelingInformation(
            SatelliteRecord record, int imageTypeNumber) {

        ImagePreferences imgPref = new ImagePreferences();

        try {
            imgPref = getStyleRulePreferences(record);
        } catch (VizException e1) {
            statusHandler
                    .handle(Priority.PROBLEM, e1.getLocalizedMessage(), e1);
        }

        String dataUnitString = record.getUnits();
        double minPixVal = Double.NaN;
        double maxPixVal = Double.NaN;

        ColorBarFromColormap colorBar = (ColorBarFromColormap) this.cbarResource
                .getResourceData().getColorbar();
        if (colorBar.getColorMap() == null) {
            colorBar.setColorMap((ColorMap) getCapability(
                    ColorMapCapability.class).getColorMapParameters()
                    .getColorMap());
        }

        try {
            // Might need to change this if/when we use the data-scaling
            SamplePreferences samplePref = imgPref.getSamplePrefs();
            if (samplePref != null) {
                minPixVal = imgPref.getSamplePrefs().getMinValue();
                maxPixVal = imgPref.getSamplePrefs().getMaxValue();
            } else if (imgPref.getDataScale() != null) {
                DataScale ds = imgPref.getDataScale();
                if (ds.getMaxValue() != null) {
                    maxPixVal = ds.getMaxValue().doubleValue();
                }
                if (ds.getMinValue() != null) {
                    minPixVal = ds.getMinValue().doubleValue();
                }
            }

            colorBar.setImagePreferences(imgPref);
            if (imgPref.getDisplayUnitLabel() != null) {
                colorBar.setDisplayUnitStr(imgPref.getDisplayUnitLabel());
            }
        } catch (Exception e) {
            logger.error("Error generating colorbar data", e);
        }

        // these label value calculations are from legacy imlabl.f
        if ((dataUnitString != null) && (dataUnitString.equals("BRIT"))
                && ((imageTypeNumber == 8) || (imageTypeNumber == 128))
                && (minPixVal != Double.NaN) && (maxPixVal != Double.NaN)
                && (colorBar != null) && (imgPref.getDataMapping() == null))

        {
            int imndlv = (int) Math.min(256, (maxPixVal - minPixVal + 1));
            NcIRPixelToTempConverter pixelToTemperatureConverter = new NcIRPixelToTempConverter();
            double tmpk = pixelToTemperatureConverter.convert(minPixVal);
            double tmpc = SI.KELVIN.getConverterTo(SI.CELSIUS).convert(tmpk);

            DataMappingPreferences dmPref = new DataMappingPreferences();
            DataMappingEntry entry = new DataMappingEntry();

            entry.setPixelValue(1.0);
            entry.setDisplayValue(tmpc);
            entry.setLabel(String.format("%.1f", tmpc));
            dmPref.addEntry(entry);

            int ibrit = (imndlv - 1) + (int) minPixVal;
            tmpk = pixelToTemperatureConverter.convert(ibrit);
            tmpc = SI.KELVIN.getConverterTo(SI.CELSIUS).convert(tmpk);

            entry = new DataMappingEntry();
            entry.setPixelValue(Double.valueOf(imndlv));
            entry.setDisplayValue(tmpc);
            entry.setLabel(String.format("%.1f", tmpc));
            dmPref.addEntry(entry);

            NcIRTempToPixelConverter tempToPixConv = new NcIRTempToPixelConverter();
            tmpc = -100;

            tmpk = SI.CELSIUS.getConverterTo(SI.KELVIN).convert(tmpc);
            double brit = tempToPixConv.convert(tmpk);
            ibrit = (int) (Math.round(brit) - minPixVal);
            while (tmpc < 51) {
                tmpk = SI.CELSIUS.getConverterTo(SI.KELVIN).convert(tmpc);
                brit = tempToPixConv.convert(tmpk);

                ibrit = (int) (Math.round(brit) - minPixVal);
                if (ibrit > 0) {
                    entry = new DataMappingEntry();

                    entry.setPixelValue(Double.valueOf(ibrit));
                    entry.setDisplayValue(tmpc);
                    entry.setLabel(Integer.toString((int) tmpc));
                    dmPref.addEntry(entry);
                }
                tmpc += 10;
            }

            imgPref.setDataMapping(dmPref);
            colorBar.setImagePreferences(imgPref);
            if (!colorBar.isScalingAttemptedForThisColorMap()) {
                colorBar.scalePixelValues();
            }

            colorBar.setDisplayUnitStr(imgPref.getDisplayUnitLabel());

        } else if (imgPref.getDataMapping() == null) {
            // no existing data mapping, so we generate it
            if (imgPref.getDisplayUnitLabel() != null)
                colorBar.setDisplayUnitStr(imgPref.getDisplayUnitLabel());
            else
                colorBar.setDisplayUnitStr(dataUnitString);

            int imndlv = (int) Math.min(256, maxPixVal - minPixVal + 1);
            double ratio = (maxPixVal - minPixVal) / 255;
            DataMappingEntry dmEntry = new DataMappingEntry();
            dmEntry.setPixelValue(1.0);
            dmEntry.setDisplayValue(minPixVal);
            dmEntry.setLabel(Double.toString(minPixVal));
            DataMappingPreferences dmPref = new DataMappingPreferences();
            dmPref.addEntry(dmEntry);
            double level = -1;
            for (int ii = 2; ii < imndlv; ii++) {
                if ((ii - 1) % 16 == 0) {
                    level = Math.round((ii - 1) * ratio) + minPixVal;
                    dmEntry = new DataMappingEntry();
                    dmEntry.setPixelValue((double) ii);
                    dmEntry.setDisplayValue(level);
                    dmEntry.setLabel(Double.toString(level));
                    dmPref.addEntry(dmEntry);
                }
            }
            level = Math.round((imndlv - 1) * ratio) + minPixVal;
            dmEntry = new DataMappingEntry();
            dmEntry.setPixelValue((double) imndlv);
            dmEntry.setDisplayValue(level);
            dmEntry.setLabel(Double.toString(level));
            dmPref.addEntry(dmEntry);

            if (!colorBar.isScalingAttemptedForThisColorMap()) {
                imgPref = new ImagePreferences();
                imgPref.setDataMapping(dmPref);
                SamplePreferences sPref = new SamplePreferences();
                sPref.setMaxValue(255);
                sPref.setMinValue(0);
                imgPref.setSamplePrefs(sPref);
                colorBar.setImagePreferences(imgPref);
                colorBar.scalePixelValues();
            }

        }
        colorBar.setAlignLabelInTheMiddleOfInterval(false);
        if (!colorBar.equals(resourceData.getColorBar()))
            resourceData.setColorBar(colorBar);
    }

    @Override
    public void propertiesChanged(ResourceProperties updatedProps) {
        if (cbarResource != null && cbarResource.getProperties() != null) {
            cbarResource.getProperties().setVisible(updatedProps.isVisible());
        }
    }

    String getDataUnitsFromRecord(PluginDataObject pdo) {
        return ((SatelliteRecord) pdo).getUnits();
    }

    String getImageTypeFromRecord(PluginDataObject pdo) {
        return ((SatelliteRecord) pdo).getPhysicalElement();
    }

    /*
     * for IAreaProviderCapable which triggers the Fit To Screen and Size Of
     * Image context menus
     */
    @Override
    public AreaSource getSourceProvider() {
        return resourceData.getSourceProvider();
    }

    @Override
    public String getAreaName() {
        return resourceData.getAreaName();
    }
}
