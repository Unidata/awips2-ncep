package gov.noaa.nws.ncep.viz.rsc.satellite.rsc;

import java.io.File;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.format.ParserException;
import javax.measure.quantity.Temperature;
import javax.xml.bind.JAXBException;

import org.geotools.coverage.grid.GridGeometry2D;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.raytheon.uf.common.colormap.ColorMap;
import com.raytheon.uf.common.colormap.prefs.ColorMapParameters;
import com.raytheon.uf.common.colormap.prefs.DataMappingPreferences;
import com.raytheon.uf.common.colormap.prefs.DataMappingPreferences.DataMappingEntry;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.persist.IPersistable;
import com.raytheon.uf.common.dataplugin.persist.PersistableDataObject;
import com.raytheon.uf.common.dataplugin.satellite.SatMapCoverage;
import com.raytheon.uf.common.dataplugin.satellite.SatelliteConstants;
import com.raytheon.uf.common.dataplugin.satellite.SatelliteRecord;
import com.raytheon.uf.common.dataplugin.satellite.units.goes.PolarPrecipWaterPixel;
import com.raytheon.uf.common.dataplugin.satellite.units.ir.IRPixel;
import com.raytheon.uf.common.dataplugin.satellite.units.water.BlendedTPWPixel;
import com.raytheon.uf.common.geospatial.IGridGeometryProvider;
import com.raytheon.uf.common.geospatial.ReferencedCoordinate;
import com.raytheon.uf.common.serialization.JAXBManager;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.jaxb.JAXBClassLocator;
import com.raytheon.uf.common.serialization.jaxb.JaxbDummyObject;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.style.AbstractStylePreferences;
import com.raytheon.uf.common.style.ImageryLabelingPreferences;
import com.raytheon.uf.common.style.MatchCriteria;
import com.raytheon.uf.common.style.ParamLevelMatchCriteria;
import com.raytheon.uf.common.style.StyleException;
import com.raytheon.uf.common.style.StyleRule;
import com.raytheon.uf.common.style.StyleRuleset;
import com.raytheon.uf.common.style.image.DataScale;
import com.raytheon.uf.common.style.image.DataScale.Type;
import com.raytheon.uf.common.style.image.ImagePreferences;
import com.raytheon.uf.common.style.image.NumericFormat;
import com.raytheon.uf.common.style.image.SampleFormat;
import com.raytheon.uf.common.style.level.Level;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.DrawableImage;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.drawables.IRenderable;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.reflect.SubClassLocator;
import com.raytheon.uf.viz.core.rsc.IInputHandler;
import com.raytheon.uf.viz.core.rsc.IInputHandler.InputPriority;
import com.raytheon.uf.viz.core.rsc.IResourceDataChanged;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.ResourceProperties;
import com.raytheon.uf.viz.core.rsc.capabilities.ColorMapCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.ImagingCapability;
import com.raytheon.uf.viz.core.tile.RecordTileSetRenderable;

import gov.noaa.nws.ncep.common.dataplugin.mcidas.McidasConstants;
import gov.noaa.nws.ncep.viz.common.ColorMapUtil;
import gov.noaa.nws.ncep.viz.common.area.AreaName.AreaSource;
import gov.noaa.nws.ncep.viz.common.area.IAreaProviderCapable;
import gov.noaa.nws.ncep.viz.common.ui.NmapCommon;
import gov.noaa.nws.ncep.viz.localization.NcPathManager;
import gov.noaa.nws.ncep.viz.localization.NcPathManager.NcPathConstants;
import gov.noaa.nws.ncep.viz.resources.AbstractFrameData;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource2;
import gov.noaa.nws.ncep.viz.resources.DfltRecordRscDataObj;
import gov.noaa.nws.ncep.viz.resources.IRscDataObject;
import gov.noaa.nws.ncep.viz.resources.colorBar.ColorBarResource;
import gov.noaa.nws.ncep.viz.resources.colorBar.ColorBarResourceData;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceDefinition;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceDefnsMngr;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceName;
import gov.noaa.nws.ncep.viz.resources.util.VariableSubstitutorNCEP;
import gov.noaa.nws.ncep.viz.rsc.satellite.rsc.SatelliteResourceData.SatelliteType;
import gov.noaa.nws.ncep.viz.rsc.satellite.units.NcIRPixelToTempConverter;
import gov.noaa.nws.ncep.viz.rsc.satellite.units.NcIRTempToPixelConverter;
import gov.noaa.nws.ncep.viz.ui.display.ColorBarFromColormap;
import gov.noaa.nws.ncep.viz.ui.display.NCMapDescriptor;
import si.uom.SI;
import tec.uom.se.format.SimpleUnitFormat;

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
 * <pre>
 *
 *  SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer     Description
 * ------------- -------- ------------ -----------------------------------------
 * Sep 28, 2015  11385    njensen      Initial creation
 * Oct 28, 2015  11385    kbugenhagen  Updated to use stylerules to generate
 *                                     colormap
 * Apr 12, 2016  16367    kbugenhagen  Added support for SIMGOESR
 * Apr 15, 2016  15954    SRussell     Updated FrameData.updateFrameData()
 *                                     Integrated class with IDataLoader design
 * Apr 12, 2016  15945    RCReynolds   Added code to build custom string for
 *                                     input to getLegendString for GINI and
 *                                     HIMAWARI
 * Jun 06, 2016  15945    RCReynolds   Using McidasConstants instead of
 *                                     SatelliteConstants
 * Jun 01, 2016  18511    kbugenhagen  Refactored to support Modis, Viirs and
 *                                     Mcidas satellite viz resources and remove
 *                                     need for AbstractSatelliteResource class.
 * Jul 01, 2016  17376    kbugenhagen  Added getColorMapName method to allow
 *                                     different satellite resources to use
 *                                     different methods for getting the
 *                                     colormap name (i.e. via stylerules or
 *                                     attribute files.
 * Jul 29, 2016  17936    mkean        null in legendString for unaliased
 *                                     satellite.
 * Sep 16, 2016  15716    SRussell     Added a new FrameData constructor, Added
 *                                     method getLastRecordAdded()
 * Dec 14, 2016  20988    kbugenhagen  Remove setting colormap capability in
 *                                     setColorMapUnits so colormap name isn't
 *                                     overwritten.
 * Nov 29, 2017  5863     bsteffen     Change dataTimes to a NavigableSet
 * Jun 11, 2018  7310     mapeters     Update SatelliteConstants import
 * Nov 07, 2018  7552     dgilling     Implement isCloudHeightCompatible and
 *                                     getGridGeometry for expanded
 *                                     functionality of Cloud Height tool.
 * May 21, 2019  64168    ksunil       Use ImageryLabelingPreference
 * Apr 20, 2020  8145     randerso     Replace SamplePreferences with
 *                                     SampleFormat
 *
 * </pre>
 *
 */
public class NcSatelliteResource extends
        AbstractNatlCntrsResource2<SatelliteResourceData, NCMapDescriptor>
        implements IResourceDataChanged, IAreaProviderCapable {

    private static final String CREATING_ENTITY_KEY = "creatingEntity";

    private static final String SECTOR_ID_KEY = "sectorID";

    protected static final String COLORBAR_STRING_FORMAT = "%.1f";

    protected static final String DEFAULT_COLORMAP_NAME = "colorMapName";

    private static JAXBManager jaxb;

    protected String legendStr = "Satellite";

    protected String physicalElement;

    protected ColorBarResource cbarResource;

    protected ImagePreferences imagePreferences;

    private SatelliteResourceData satelliteResourceData = null;

    // for sampling
    private final IInputHandler inputAdapter = getInputHandler();

    protected volatile boolean initialized = false;

    // slimmed down variation on what was in AbstractSatelliteResource
    protected class FrameData extends AbstractFrameData {

        protected DataTime tileTimePrevAddedRecord = null;

        protected IPersistable last_record_added = null;

        protected RecordTileSetRenderable tileSet;

        // one renderable per frame, each renderable may have multiple tiles
        // protected SatRenderable<SatMapCoverage> renderable;
        protected SatRenderable renderable;

        protected FrameData(DataTime time, int interval) {
            super(time, interval, satelliteResourceData);
            setRenderable(new SatRenderable<SatMapCoverage>());
        }

        protected FrameData(DataTime time, int interval,
                SatRenderable<?> childClassRndble) {
            super(time, interval, satelliteResourceData);
            setRenderable(childClassRndble);
        }

        @Override
        public void dispose() {
            renderable.dispose();
        }

        @Override
        public boolean updateFrameData(IRscDataObject rscDataObj) {
            IPersistable satRec = (IPersistable) ((DfltRecordRscDataObj) rscDataObj)
                    .getPDO();

            setRenderable(renderable);
            if (!initialized) {
                synchronized (NcSatelliteResource.this) {
                    if (!initialized) {
                        try {
                            // initialize colormap
                            initializeFirstFrame(satRec);
                        } catch (Exception e) {
                            statusHandler.error(
                                    "Error initializing against SatelliteRecord "
                                            + satRec,
                                    e);
                            return false;
                        }
                    }
                }
            }

            int imageTypeNumber = getImageTypeNumber(satRec);

            if (isExistingDataBetterTimeMatch(satRec)) {
                return false;
            }

            generateAndStoreColorBarLabelingInformation(satRec,
                    imageTypeNumber);

            if (satRec != null) {
                tileTimePrevAddedRecord = ((PluginDataObject) satRec)
                        .getDataTime();
                last_record_added = satRec;
                getRenderable().addRecord(satRec);

            }

            return true;
        }

        /**
         * Determine if the previously added satellite record's timestamp is a
         * closer time match than the current record's.
         *
         * @param newSatRec
         *            current satellite record
         * @return true if previous timestamp is closer
         */
        boolean isExistingDataBetterTimeMatch(IPersistable newSatRec) {
            boolean oldDataBetterTimeMatch = false;

            if (tileTimePrevAddedRecord == null) {
                oldDataBetterTimeMatch = false;
                return oldDataBetterTimeMatch;
            } else if (newSatRec == null) {
                oldDataBetterTimeMatch = true;
                return oldDataBetterTimeMatch;
            }

            DataTime newDataTime = ((PluginDataObject) newSatRec).getDataTime();

            // Existing data is a better time match
            if (timeMatch(newDataTime) >= timeMatch(tileTimePrevAddedRecord)) {
                oldDataBetterTimeMatch = true;
            }

            return oldDataBetterTimeMatch;
        }

        public SatRenderable<?> getRenderable() {
            return renderable;
        }

        public void setRenderable(SatRenderable<?> renderable) {
            this.renderable = renderable;
        }

        public IPersistable getLastRecordAdded() {
            return last_record_added;
        }

    }

    public IPersistable getRecord() {
        FrameData fd = (FrameData) this.getCurrentFrame();
        IPersistable record = fd.getLastRecordAdded();
        return record;
    }

    /**
     * Renderable for displaying satellite data. There is one renderable per
     * frame but potentially multiple tiles per renderable.
     *
     * This class is almost an exact copy of
     * com.raytheon.viz.satellite.rsc.SatResource.SatRenderable. The differences
     * are only in addRecord() and interrogate(). See TODO above.
     *
     * @param <T>
     *            satellite spatial coverage
     */
    protected class SatRenderable<T extends PersistableDataObject<?>>
            implements IRenderable {

        protected Map<T, RecordTileSetRenderable> tileMap = new HashMap<>();

        @Override
        public void paint(IGraphicsTarget target, PaintProperties paintProps)
                throws VizException {
            Collection<DrawableImage> images = getImagesToRender(target,
                    paintProps);
            if (!images.isEmpty()) {
                target.drawRasters(paintProps,
                        images.toArray(new DrawableImage[0]));
            }
        }

        /**
         * Get DrawableImages for rendering.
         *
         * @param target
         *            graphics target
         * @param paintProps
         *            screen painting properties
         * @return list of images
         * @throws VizException
         */
        public Collection<DrawableImage> getImagesToRender(
                IGraphicsTarget target, PaintProperties paintProps)
                throws VizException {
            List<DrawableImage> images = new ArrayList<>();
            synchronized (tileMap) {
                for (RecordTileSetRenderable renderable : tileMap.values()) {
                    images.addAll(
                            renderable.getImagesToRender(target, paintProps));
                }
            }
            return images;
        }

        /**
         * Create a tileset from a satellite data record and add it to the
         * collection of tilesets.
         *
         * @param record
         *            satellite data record
         */
        @SuppressWarnings("unchecked")
        public void addRecord(IPersistable record) {
            synchronized (tileMap) {

                RecordTileSetRenderable tileSet = tileMap
                        .get(((SatelliteRecord) record).getCoverage());
                SatelliteRecord satRecord = (SatelliteRecord) record;

                if (tileSet != null) {
                    // Get rid of it to make way for the new one
                    tileSet.dispose();
                    tileSet = null;
                    tileMap.clear();
                }
                if (tileSet == null) {
                    tileSet = new RecordTileSetRenderable(
                            NcSatelliteResource.this, satRecord,
                            satRecord.getGridGeometry(),
                            satRecord.getInterpolationLevels() + 1);
                    tileSet.project(descriptor.getGridGeometry());
                    tileMap.put((T) satRecord.getCoverage(), tileSet);
                }
            }
        }

        /**
         * Project the tile set for use with the target geometry.
         */
        public void project() {
            synchronized (tileMap) {
                for (RecordTileSetRenderable renderable : tileMap.values()) {
                    renderable.project(descriptor.getGridGeometry());
                }
            }
        }

        /**
         * Dispose renderable objects.
         */
        public void dispose() {
            synchronized (tileMap) {
                for (RecordTileSetRenderable renderable : tileMap.values()) {
                    renderable.dispose();
                }
                tileMap.clear();
            }
        }

        /**
         * Interrogate to get the data value at a lat/lon.
         *
         * @param latLon
         *            lat/long coordiate
         * @param requestUnit
         *            unit the result will be returned in
         * @return
         * @throws VizException
         */
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

        public Map<T, RecordTileSetRenderable> getTileMap() {
            return tileMap;
        }

    }

    /**
     * NOTE: logic copied from GiniSatResource constructor
     */
    public NcSatelliteResource(SatelliteResourceData data,
            LoadProperties props) {
        super(data, props);
        this.resourceData = data;
        this.satelliteResourceData = data;

        resourceData.addChangeListener(this);

        legendStr = createLegendString();

    }

    // return input handler for sampling
    public IInputHandler getInputHandler() {
        return null;
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

    /**
     * One-time initialization to set up colormap.
     *
     * @param record
     *            satellite data record
     * @throws VizException
     */
    protected void initializeFirstFrame(IPersistable record)
            throws VizException {
        initialized = true;

        if (record instanceof SatelliteRecord) {
            physicalElement = ((SatelliteRecord) (record)).getPhysicalElement();
        }

        // create the colorMap and set it in the colorMapParameters
        imagePreferences = getStyleRulePreferences(record);
        ColorMapParameters colorMapParameters = loadColorMapParameters(record);
        getCapability(ColorMapCapability.class)
                .setColorMapParameters(colorMapParameters);
    }

    /**
     * Create legend string from subtypes
     *
     * @return
     */
    private String createLegendString() {

        String legendString = "";
        String area = "";
        String satellite = "";
        String channel = "";
        String rd = "";
        String customizedLegendString = "";

        try {

            ResourceDefnsMngr rscDefnsMngr = ResourceDefnsMngr.getInstance();
            ResourceName rscName = resourceData.getResourceName();
            Map<String, String> variables = new HashMap<>();
            ResourceDefinition rscDefn = rscDefnsMngr
                    .getResourceDefinition(rscName.getRscType());
            Map<String, String> attributes = rscDefnsMngr.getAttrSet(rscName)
                    .getAttributes();

            // legendString can be assigned by the user in the attribute set,
            // if it does not exist.. legendStringAttribute will be null
            String legendStringAttribute = attributes.get("legendString");

            channel = resourceData.getRscAttrSet().getRscAttrSetName();
            rd = rscDefn.getResourceDefnName();
            final boolean gotEntity = resourceData.getMetadataMap()
                    .containsKey(CREATING_ENTITY_KEY);
            final boolean gotSector = resourceData.getMetadataMap()
                    .containsKey(SECTOR_ID_KEY);

            if (gotEntity) {
                satellite = resourceData.getMetadataMap()
                        .get(CREATING_ENTITY_KEY).getConstraintValue();
                if (satellite != null && !satellite.isEmpty()) {
                    variables.put(McidasConstants.SATELLLITE, satellite);
                }
            }
            if (gotSector) {
                area = resourceData.getMetadataMap().get(SECTOR_ID_KEY)
                        .getConstraintValue();
                if (area != null && !area.isEmpty()) {
                    variables.put(McidasConstants.AREA, area);
                }
            }
            if (rd != null && !rd.isEmpty()) {
                variables.put(McidasConstants.RESOURCE_DEFINITION, rd);
            }
            if (channel != null && !channel.isEmpty()) {
                variables.put(McidasConstants.CHANNEL, channel);
            }

            // process legendStringAttribute if assignment exist
            if (legendStringAttribute != null) {

                customizedLegendString = constructCustomLegendString(variables,
                        legendStringAttribute);
            }

            /*
             * standard, original legend string
             */
            if (gotEntity && gotSector) {
                legendString = satellite + " " + area + " " + channel;
                legendString = legendString.replace('%', ' ');
            }
            legendString = (customizedLegendString.isEmpty()) ? legendString
                    : customizedLegendString;

        } catch (Exception ex) {
            statusHandler.error("Error creating legend string", ex);
        }

        return legendString;
    }

    /**
     * This method will construct the custom legend string. The variable maps in
     * the legendStringAttribute will be processed.
     *
     * @param variables
     * @param legendStringAttribute
     * @return the custom legend string
     * @throws VizException
     */
    public String constructCustomLegendString(Map<String, String> variables,
            String legendStringAttribute) throws VizException {

        StringBuilder sb = new StringBuilder();
        String customizedLegendString;
        String value = "";
        char x;

        /*
         * "variables map" now contains keywords/values available for building
         * the custom legend string. Examine marked-up legend string looking for
         * {keyword} that matches in "variables". If it doesn't then remove it
         * from legendString.
         */
        Pattern p = Pattern.compile("\\{(.*?)\\}");
        Matcher m = p.matcher(legendStringAttribute);
        while (m.find()) {
            value = variables.get(m.group(1));
            if (value == null || value.isEmpty()) {
                legendStringAttribute = legendStringAttribute
                        .replace("{" + m.group(1) + "}", "");
            }
        }

        /*
         * change all occurrences of '{' to "${" because thats what
         * VariableSubstituterNCEP expects
         */
        for (int ipos = 0; ipos < legendStringAttribute.length(); ipos++) {
            x = legendStringAttribute.charAt(ipos);
            sb.append(x == '{' ? "${" : x);
        }
        customizedLegendString = VariableSubstitutorNCEP
                .processVariables(sb.toString(), variables);

        /*
         * If user coded legendString properly there shoulden't be any "${"
         * present, but if there are then change them back to "{"
         */
        sb.setLength(0);
        for (int ipos = 0; ipos < customizedLegendString.length(); ipos++) {
            x = customizedLegendString.charAt(ipos);
            sb.append(x == '$' ? "{" : x);
        }
        customizedLegendString = sb.toString();
        return customizedLegendString;
    }

    /**
     * Retrieves preferences specified in style rule
     *
     * @param dataRecord
     *            satellite data record
     * @return The preferences specified in the style rule file
     * @throws VizException
     */
    protected ImagePreferences getStyleRulePreferences(IPersistable dataRecord)
            throws VizException {
        ImagePreferences preferences = null;
        String styleRuleFile = getLocFilePathForImageryStyleRule(
                getResourceData().getSatelliteType());
        File file = NcPathManager.getInstance().getStaticFile(styleRuleFile);
        StyleRule styleRule = null;
        List<String> paramList = getParameterList(dataRecord);
        ParamLevelMatchCriteria matchCriteria = new ParamLevelMatchCriteria();
        matchCriteria.setParameterName(paramList);

        try {
            StyleRuleset styleSet = getJaxbManager()
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
        } catch (SerializationException | JAXBException e) {
            statusHandler.handle(UFStatus.Priority.PROBLEM,
                    e.getLocalizedMessage(), e);
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
     *            satellite data record
     * @return colormap parameters
     * @throws VizException
     */
    protected ColorMapParameters loadColorMapParameters(IPersistable dataRecord)
            throws VizException {
        ColorMapParameters colorMapParameters = getCapability(
                ColorMapCapability.class).getColorMapParameters();
        if (colorMapParameters == null) {
            colorMapParameters = new ColorMapParameters();
        }
        if (colorMapParameters.getColorMap() == null) {
            String colorMapName = getColorMapName(colorMapParameters);
            // load colormap by name
            ColorMap colorMap = (ColorMap) ColorMapUtil
                    .loadColorMap(resourceData.getResourceName()
                            .getRscCategory().getCategoryName(), colorMapName);
            colorMapParameters.setColorMap(colorMap);
        }
        try {
            setColorMapUnits(dataRecord, colorMapParameters);
        } catch (StyleException e) {
            throw new VizException(e.getLocalizedMessage(), e);
        }

        return colorMapParameters;
    }

    /**
     * Gets the colormap name from the resource data (attributes file). If image
     * preferences (style rules) specify the name, get it from there.
     *
     * @param colorMapParameters
     * @return name of colormap
     */
    protected String getColorMapName(ColorMapParameters colorMapParameters) {
        String name = colorMapParameters.getColorMapName();
        if (name == null || name.equals(DEFAULT_COLORMAP_NAME)) {
            name = resourceData.getColorMapName();
            if (imagePreferences != null) {
                name = imagePreferences.getDefaultColormap();
            }
            if (name == null || name.equals(DEFAULT_COLORMAP_NAME)) {
                name = "IR Default";
            }
        }

        return name;
    }

    /**
     * Set color map attributes
     *
     * @param record
     *            satellite data record
     * @param colorMapParameters
     * @throws StyleException
     */
    protected void setColorMapUnits(IPersistable record,
            ColorMapParameters colorMapParameters) throws StyleException {

        Unit<?> colorMapUnit = null;
        try {
            colorMapUnit = imagePreferences.getColorMapUnitsObject();
        } catch (StyleException e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
        }
        if (colorMapUnit == null) {
            colorMapUnit = getRecordUnit(record);
        }

        Unit<?> displayUnit = imagePreferences.getDisplayUnits();
        if (displayUnit == null) {
            displayUnit = colorMapUnit;
        } else if (colorMapUnit == null) {
            colorMapUnit = displayUnit;
        }

        colorMapParameters.setColorMapUnit(new IRPixel());
        colorMapParameters.setDisplayUnit(displayUnit);
        colorMapParameters.setDataMapping(imagePreferences.getDataMapping());

        Float displayMin = null, displayMax = null;
        DataScale scale = imagePreferences.getDataScale();
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
                if (entries != null && !entries.isEmpty()) {
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
            colorMapMin = displayToColorMap.convert(displayMin).floatValue();
            colorMapMax = displayToColorMap.convert(displayMax).floatValue();
        }

        colorMapParameters.setColorMapMin(colorMapMin);
        colorMapParameters.setColorMapMax(colorMapMax);

        ImageryLabelingPreferences labeling = imagePreferences
                .getColorbarLabeling();
        if (labeling != null) {
            if (labeling.getValues() != null) {
                colorMapParameters.setColorBarIntervals(labeling.getValues());
            } else if (labeling.getIncrement() != 0) {
                float increment = labeling.getIncrement();
                float initialPoint = (float) (Math.ceil(colorMapMin / increment)
                        * increment);
                float finalPoint = (float) (Math.floor(colorMapMin / increment)
                        * increment);
                int count = (int) ((finalPoint - initialPoint) / increment) + 1;
                float[] vals = new float[count];
                for (int i = 0; i < count; i += 1) {
                    vals[i] = initialPoint + increment * i;
                }
            }
        }
    }

    /**
     * Get parameters used in matching criteria used to locate style rule.
     *
     * @param dataRecord
     *            satellite data record
     * @return parameters
     */
    protected List<String> getParameterList(IPersistable dataRecord) {
        List<String> paramList = new ArrayList<>(0);
        String paramStr = ((SatelliteRecord) dataRecord).getPhysicalElement();
        paramList.add(paramStr);
        return paramList;
    }

    /**
     * Get image type number used to generate color bar.
     *
     * @param dataRecord
     *            satellite data record
     * @return image type number
     */
    protected int getImageTypeNumber(IPersistable dataRecord) {
        return -1;
    }

    /**
     * Get the record unit associated with the satellite data record.
     *
     * @param dataRecord
     *            satellite data record0
     * @return record unit
     */
    public Unit<?> getRecordUnit(IPersistable dataRecord) {
        Unit<?> recordUnit = null;
        SatelliteRecord record = (SatelliteRecord) dataRecord;
        String physicalElement = record.getPhysicalElement();

        if (record.getUnits() != null && !record.getUnits().isEmpty()) {
            try {
                recordUnit = SimpleUnitFormat.getInstance(SimpleUnitFormat.Flavor.ASCII).parseProductUnit(
                        record.getUnits(), new ParsePosition(0));
            } catch (ParserException e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Unable to parse satellite units: " + record.getUnits(),
                        e);
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

    /**
     * Get the file path for a satellite type
     *
     * @param type
     *            satellite type
     * @return file path
     */
    protected String getLocFilePathForImageryStyleRule(SatelliteType type) {
        if (type == SatelliteType.HIMAWARI) {
            return NcPathConstants.HIMAWARI_IMG_STYLE_RULES;
        } else if (type == SatelliteType.SIMGOESR) {
            return NcPathConstants.SIMGOESR_IMG_STYLE_RULES;
        } else if (type == SatelliteType.MODIS) {
            return NcPathConstants.MODIS_IMG_STYLE_RULES;
        } else if (type == SatelliteType.MCIDAS) {
            return NcPathConstants.MCIDAS_IMG_STYLE_RULES;
        } else {
            return NcPathConstants.GINI_IMG_STYLE_RULES;
        }
    }

    /**
     * Get JAXB manager used in style rule lookup.
     *
     * @return JAXBManager
     * @throws JAXBException
     */
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
        // dispose of the frameData
        super.disposeInternal();
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

        IDisplayPaneContainer container = getResourceContainer();
        if (container != null && inputAdapter != null) {
            container.unregisterMouseHandler(inputAdapter);
        }

    }

    @Override
    public void initResource(IGraphicsTarget target) throws VizException {

        // Create the colorBar resource and add it to the resourceList for this
        // descriptor
        ResourcePair cbarRscPair = ResourcePair.constructSystemResourcePair(
                new ColorBarResourceData(resourceData.getColorBar()));
        getDescriptor().getResourceList().add(cbarRscPair);
        getDescriptor().getResourceList().instantiateResources(getDescriptor(),
                true);
        cbarResource = (ColorBarResource) cbarRscPair.getResource();

        IDisplayPaneContainer container = getResourceContainer();
        if (container != null && inputAdapter != null) {
            container.registerMouseHandler(inputAdapter,
                    InputPriority.RESOURCE);
        }

        dataLoader.loadData();
    }

    @Override
    protected void paintFrame(AbstractFrameData frmData, IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {
        FrameData currFrame = (FrameData) frmData;
        SatRenderable<SatMapCoverage> satr = currFrame.renderable;

        if (satr != null) {
            ImagingCapability imgCap = new ImagingCapability();
            imgCap.setBrightness(resourceData.getBrightness());
            imgCap.setContrast(resourceData.getContrast());
            imgCap.setAlpha(resourceData.getAlpha());
            paintProps.setAlpha(resourceData.getAlpha());
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

    /**
     * Look up the data value for a lat/lon
     *
     * @param coord
     *            lat/lon coordinate
     * @return data value
     * @throws VizException
     */
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

        return currFrame.getRenderable().interrogate(latlon,
                getCapability(ColorMapCapability.class).getColorMapParameters()
                        .getDisplayUnit());
    }

    /**
     * Determine whether this satellite resource is compatible with cloud height
     * tool
     *
     * @return
     */
    public boolean isCloudHeightCompatible() {
        return (getTemperatureUnits() == SI.CELSIUS);
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
            return currFrame.getRenderable().interrogate(latlon, null);
        } catch (VizException e) {
            return null;
        }
    }

    /**
     * Gets the temp in the units set as the display units in the colormap
     * parameters.
     *
     * @param latlon
     *            coordinate
     * @return the temperature
     */
    public Double getSatIRTemperature(Coordinate latlon) {
        if (!isCloudHeightCompatible()) {
            return null;
        }

        FrameData currFrame = (FrameData) getCurrentFrame();
        Unit<?> unit = null;

        if (currFrame == null) {
            return null;
        }
        try {

            SatRenderable<?> sr = currFrame.getRenderable();
            return sr.interrogate(latlon, unit);

        } catch (VizException e) {
            statusHandler.error("Error getting temperature", e);
            return null;
        }

    }

    /**
     * This is an ICloudHeightCapable Interface method Get the display Units. If
     * it is Celsius, the resource is usable with the Cloud Height Tool which
     * calculates the height of clouds from temperature data
     *
     * @return Unit
     */

    @SuppressWarnings("unchecked")
    public Unit<Temperature> getTemperatureUnits() {

        if (resourceData.getDisplayUnit() == null) {
            return SI.CELSIUS;
        } else {
            return (Unit<Temperature>) resourceData.getDisplayUnit();
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

        getCapability(ColorMapCapability.class)
                .setColorMapParameters(cmapParams);
        cbarResource.setColorBar(colorBar);
    }

    public String getLegendString() {
        return legendStr;
    }

    @Override
    public void resourceChanged(ChangeType type, Object object) {
        if (type != null && type == ChangeType.CAPABILITY) {
            if (object instanceof ImagingCapability) {
                ImagingCapability imgCap = getCapability(
                        ImagingCapability.class);
                ImagingCapability newImgCap = (ImagingCapability) object;
                imgCap.setBrightness(newImgCap.getBrightness(), false);
                imgCap.setContrast(newImgCap.getContrast(), false);
                imgCap.setAlpha(newImgCap.getAlpha(), false);
                resourceData.setAlpha(imgCap.getAlpha());
                resourceData.setBrightness(imgCap.getBrightness());
                resourceData.setContrast(imgCap.getContrast());
                issueRefresh();
            } else if (object instanceof ColorMapCapability) {
                ColorMapCapability colorMapCap = getCapability(
                        ColorMapCapability.class);
                ColorMapCapability newColorMapCap = (ColorMapCapability) object;
                colorMapCap.setColorMapParameters(
                        newColorMapCap.getColorMapParameters(), false);
                ColorMap theColorMap = (ColorMap) colorMapCap
                        .getColorMapParameters().getColorMap();
                String colorMapName = colorMapCap.getColorMapParameters()
                        .getColorMapName();
                resourceData.setColorMapName(colorMapName);
                resourceData.getRscAttrSet().setAttrValue(DEFAULT_COLORMAP_NAME,
                        colorMapName);

                ColorBarFromColormap cBar = resourceData.getColorBar();
                cBar.setColorMap(theColorMap);
                ColorBarFromColormap colorBar = (ColorBarFromColormap) this.cbarResource
                        .getResourceData().getColorbar();
                if (colorBar != null) {
                    if (colorBar.getImagePreferences() != null
                            && cBar.getImagePreferences() == null) {
                        cBar.setImagePreferences(
                                colorBar.getImagePreferences());
                    }

                    cBar.setIsScalingAttemptedForThisColorMap(
                            colorBar.isScalingAttemptedForThisColorMap());
                    cBar.setNumPixelsToReAlignLabel(
                            colorBar.isAlignLabelInTheMiddleOfInterval());
                }
                resourceData.getRscAttrSet().setAttrValue("colorBar", cBar);
                resourceData.setIsEdited(true);
                issueRefresh();
            }
        }
    }

    /**
     * Create color bar.
     *
     * @param record
     *            satellite data record
     * @param imageTypeNumber
     *            satellite image type
     */
    public void generateAndStoreColorBarLabelingInformation(IPersistable record,
            int imageTypeNumber) {

        String dataUnitString = getUnits(record);
        double minPixVal = Double.NaN;
        double maxPixVal = Double.NaN;

        ColorBarFromColormap colorBar = (ColorBarFromColormap) this.cbarResource
                .getResourceData().getColorbar();
        if (colorBar.getColorMap() == null) {
            colorBar.setColorMap(
                    (ColorMap) getCapability(ColorMapCapability.class)
                            .getColorMapParameters().getColorMap());
        }

        try {
            // Might need to change this if/when we use the data-scaling
            SampleFormat sampleFormat = imagePreferences.getSampleFormat();
            if (sampleFormat instanceof NumericFormat) {
                minPixVal = ((NumericFormat) sampleFormat).getMinValue();
                maxPixVal = ((NumericFormat) sampleFormat).getMaxValue();
            } else if (imagePreferences.getDataScale() != null) {
                DataScale ds = imagePreferences.getDataScale();
                if (ds.getMaxValue() != null) {
                    maxPixVal = ds.getMaxValue().doubleValue();
                }
                if (ds.getMinValue() != null) {
                    minPixVal = ds.getMinValue().doubleValue();
                }
            }

            colorBar.setImagePreferences(imagePreferences);
            if (imagePreferences.getDisplayUnitLabel() != null) {
                colorBar.setDisplayUnitStr(
                        imagePreferences.getDisplayUnitLabel());
            }
        } catch (Exception e) {
            statusHandler.error("Error generating colorbar data", e);
        }

        // these label value calculations are from legacy imlabl.f
        if ((dataUnitString != null) && ("BRIT".equals(dataUnitString))
                && ((imageTypeNumber == 8) || (imageTypeNumber == 128))
                && (minPixVal != Double.NaN) && (maxPixVal != Double.NaN)
                && (colorBar != null)
                && (imagePreferences.getDataMapping() == null)) {
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

            imagePreferences.setDataMapping(dmPref);
            colorBar.setImagePreferences(imagePreferences);
            if (!colorBar.isScalingAttemptedForThisColorMap()) {
                colorBar.scalePixelValues();
            }

            colorBar.setDisplayUnitStr(imagePreferences.getDisplayUnitLabel());

        } else if (imagePreferences.getDataMapping() == null) {
            // no existing data mapping, so we generate it
            if (imagePreferences.getDisplayUnitLabel() != null) {
                colorBar.setDisplayUnitStr(
                        imagePreferences.getDisplayUnitLabel());
            } else {
                colorBar.setDisplayUnitStr(dataUnitString);
            }

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
                imagePreferences = new ImagePreferences();
                imagePreferences.setDataMapping(dmPref);
                imagePreferences.setSampleFormat(new NumericFormat(0.0, 255.0));
                colorBar.setImagePreferences(imagePreferences);
                colorBar.scalePixelValues();
            }

        }
        colorBar.setAlignLabelInTheMiddleOfInterval(false);
        if (!colorBar.equals(resourceData.getColorBar())) {
            resourceData.setColorBar(colorBar);
        }
    }

    protected String getUnits(IPersistable record) {
        return ((SatelliteRecord) record).getUnits();
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

    @Override
    public AreaSource getSourceProvider() {
        return resourceData.getSourceProvider();
    }

    @Override
    public String getAreaName() {
        return resourceData.getAreaName();
    }

    public GridGeometry2D getGridGeometry() {
        IPersistable record = getRecord();
        if (record instanceof IGridGeometryProvider) {
            return ((IGridGeometryProvider) record).getGridGeometry();
        }

        return null;
    }
}
