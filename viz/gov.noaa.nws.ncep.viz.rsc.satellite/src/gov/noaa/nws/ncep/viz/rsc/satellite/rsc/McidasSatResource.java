package gov.noaa.nws.ncep.viz.rsc.satellite.rsc;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import si.uom.SI;

import org.geotools.coverage.grid.GridGeometry2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.raytheon.uf.common.colormap.ColorMap;
import com.raytheon.uf.common.colormap.prefs.ColorMapParameters;
import com.raytheon.uf.common.colormap.prefs.DataMappingPreferences;
import com.raytheon.uf.common.colormap.prefs.DataMappingPreferences.DataMappingEntry;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.persist.IPersistable;
import com.raytheon.uf.common.geospatial.interpolation.GridDownscaler;
import com.raytheon.uf.common.style.AbstractStylePreferences;
import com.raytheon.uf.common.style.ParamLevelMatchCriteria;
import com.raytheon.uf.common.style.StyleException;
import com.raytheon.uf.common.style.StyleManager;
import com.raytheon.uf.common.style.StyleRule;
import com.raytheon.uf.common.style.image.DataScale;
import com.raytheon.uf.common.style.image.ImagePreferences;
import com.raytheon.uf.common.style.image.NumericFormat;
import com.raytheon.uf.common.style.image.SampleFormat;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.capabilities.ColorMapCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.ImagingCapability;
import com.raytheon.uf.viz.core.tile.RecordTileSetRenderable;

import gov.noaa.nws.ncep.common.dataplugin.mcidas.McidasConstants;
import gov.noaa.nws.ncep.common.dataplugin.mcidas.McidasMapCoverage;
import gov.noaa.nws.ncep.common.dataplugin.mcidas.McidasRecord;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.NcUnits;
import gov.noaa.nws.ncep.viz.common.ColorMapUtil;
import gov.noaa.nws.ncep.viz.resources.AbstractFrameData;
import gov.noaa.nws.ncep.viz.resources.DfltRecordRscDataObj;
import gov.noaa.nws.ncep.viz.resources.IRscDataObject;
import gov.noaa.nws.ncep.viz.resources.manager.AttributeSet;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceDefinition;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceDefnsMngr;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceName;
import gov.noaa.nws.ncep.viz.rsc.satellite.units.NcIRPixelToTempConverter;
import gov.noaa.nws.ncep.viz.rsc.satellite.units.NcIRTempToPixelConverter;
import gov.noaa.nws.ncep.viz.rsc.satellite.units.NcSatelliteUnits;
import gov.noaa.nws.ncep.viz.ui.display.ColorBarFromColormap;

/**
 * Provides Mcidas satellite raster rendering support.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer     Description
 * ------------- -------- ------------ -----------------------------------------
 * May 24, 2010  281      ghull        Initial creation
 * Jun 07, 2012  717      archana      Added the methods getImageTypeNumber(),
 *                                     getParameterList(),
 *                                     getLocFilePathForImageryStyleRule()
 *                                     Updated getDataUnitsFromRecord() to get
 *                                     the units from the database
 * Nov 29, 2012  630      ghull        IGridGeometryProvider
 * Feb 13, 2015  6345     mkean        add areaName, resolution and
 *                                     getRscAttrSetName to legendStr
 * Oct 15, 2015  7190     R. Reynolds  Added support for Mcidas
 * Dec 03, 2015  12953    R. Reynolds  Modified to enhance Legend title
 * Feb 04, 2016  14142    RCReynolds   Moved mcidas related string construction
 *                                     out to ResourceDefinition
 * Apr 12, 2016  15945    RCReynolds   Added code to build customized string for
 *                                     input to getLegendString
 * Jun 06, 2016  15945    RCReynolds   Used McidasConstants instead of
 *                                     SatelliteConstants
 * Jun 01, 2016  18511    kbugenhagen  Refactored to use NcSatelliteResource
 *                                     instead of AbstractSatelliteResource in
 *                                     order to use TileSetRenderable.
 * Jul 26, 2016  19277    bsteffen     Move redundant code into
 *                                     McidasRecord.getGridGeometry()
 * Jul 29, 2016  17936    mkean        null in legendString for unaliased
 *                                     satellite.
 * Sep 16, 2016  15716    SRussell     Updated the FrameData constructor,
 *                                     Removed setRenderable() method, Updated
 *                                     FrameData.updateFramData(), Extended
 *                                     getGridGeometry method(), Updated
 *                                     isCloudHeightCapable() method
 * Dec 14, 2016  20988    kbugenhagen  Update initializeFirstFrame to allow for
 *                                     override of colormap name in SPF.
 * Nov 29, 2017  5863     bsteffen     Change dataTimes to a NavigableSet
 * Nov 07, 2018  7552     dgilling     Remove unnecessary interface
 *                                     ICloudHeightCapable.
 * Apr 20, 0020  8145     randerso     Replace SamplePreferences with
 *                                     SampleFormat
 * Jun 08, 2020  7983     randerso     Fix compatibility issue from merge     
 *
 * </pre>
 *
 * @author ghull
 */

public class McidasSatResource extends NcSatelliteResource {

    private int numLevels;

    public McidasSatResource(SatelliteResourceData data, LoadProperties props) {
        super(data, props);

        /**
         * Get configuredLegendString (with aliases) from resourceDefinition. If
         * empty string is returned then get regular aliased legend.
         */
        legendStr = createLegendString();
    }

    /**
     * Create legend string from subtypes
     *
     * @return
     */
    private String createLegendString() {

        ResourceName rscName = resourceData.getResourceName();
        String legendString = "";

        try {
            ResourceDefnsMngr rscDefnsMngr = ResourceDefnsMngr.getInstance();
            ResourceDefinition rscDefn = rscDefnsMngr
                    .getResourceDefinition(rscName.getRscType());
            AttributeSet attributeSet = rscDefnsMngr.getAttrSet(rscName);
            rscDefn.setAttributeSet(attributeSet);
            Map<String, String> attributes = attributeSet.getAttributes();
            Map<String, String> variables = new HashMap<>();

            // legendString can be assigned by the user in the attribute set,
            // if it does not exist.. legendStringAttribute will be null
            String legendStringAttribute = attributes.get("legendString");

            String[] subtypeParam = rscDefn.getSubTypeGenerator().split(",");
            StringBuilder sb = new StringBuilder();
            for (String element : subtypeParam) {
                sb.append(resourceData.getMetadataMap().get(element)
                        .getConstraintValue()).append("_");
            }
            legendString = rscDefn.getRscGroupDisplayName(sb.toString()) + " ";
            // what's in subType generators
            String[] keysValues = rscDefn.getMcidasAliasedValues().split(",");

            /**
             * These are the keywords found in the legendString (built from
             * subType generator content)
             */
            String value = "";
            for (String keysValue : keysValues) {
                value = keysValue.split(":")[1];
                if (value != null && !value.isEmpty()) {
                    if (keysValue
                            .startsWith(McidasConstants.SATELLLITE + ":")) {
                        variables.put(McidasConstants.SATELLLITE, value);
                    } else if (keysValue
                            .startsWith(McidasConstants.AREA + ":")) {
                        variables.put(McidasConstants.AREA, value);
                    } else if (keysValue
                            .startsWith(McidasConstants.RESOLUTION + ":")) {
                        variables.put(McidasConstants.RESOLUTION, value);
                    }
                }
            }

            // Add in these last two.
            value = rscDefn.getResourceDefnName();
            if (value != null && !value.isEmpty()) {
                variables.put(McidasConstants.RESOURCE_DEFINITION,
                        rscDefn.getResourceDefnName());
            }
            value = rscDefnsMngr.getAttrSet(rscName).getName();

            if (value != null && !value.isEmpty()) {
                variables.put(McidasConstants.CHANNEL,
                        rscDefnsMngr.getAttrSet(rscName).getName());
            }

            // Process legendStringAttribute if assignment exist
            if (legendStringAttribute != null) {

                String customizedlegendString = constructCustomLegendString(
                        variables, legendStringAttribute);

                legendString = (customizedlegendString.isEmpty()) ? legendString
                        : customizedlegendString;
            }

            legendString = rscDefn.getRscGroupDisplayName(legendString) + " ";
            AttributeSet attSet = rscDefnsMngr.getAttrSet(rscName);
            rscDefn.setAttributeSet(attSet);
            legendString += rscDefn.getRscAttributeDisplayName("");
            legendString = legendString.trim();

        } catch (Exception ex) {
            statusHandler.error("Error building legend string ", ex);
        }

        return legendString;
    }

    protected class FrameData extends NcSatelliteResource.FrameData {

        // One renderable per frame, each renderable may have multiple tiles
        // protected McidasSatRenderable renderable;

        // Spatial coverage for previously added record. Used to dispose of
        // previous tile in tilemap if current record is better time match.
        protected McidasMapCoverage coveragePrevAddedRecord;

        protected FrameData(DataTime time, int interval) {
            super(time, interval, new McidasSatRenderable());
        }

        @Override
        public boolean updateFrameData(IRscDataObject rscDataObj) {
            IPersistable satRec = (IPersistable) ((DfltRecordRscDataObj) rscDataObj)
                    .getPDO();

            setRenderable(renderable);
            if (!initialized) {
                synchronized (McidasSatResource.this) {
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

            if (tileTimePrevAddedRecord != null) {

                DataTime newTileTime = ((PluginDataObject) satRec)
                        .getDataTime();

                // new record not a better match
                if (timeMatch(newTileTime) >= timeMatch(
                        tileTimePrevAddedRecord)) {
                    return false;
                } else {
                    // new record is a better match, so dispose of old tile
                    renderable.getTileMap().remove(coveragePrevAddedRecord);
                }
            }

            int imageTypeNumber = getImageTypeNumber(satRec);

            generateAndStoreColorBarLabelingInformation(satRec,
                    imageTypeNumber);

            tileTimePrevAddedRecord = ((PluginDataObject) satRec).getDataTime();
            coveragePrevAddedRecord = ((McidasRecord) satRec).getCoverage();
            last_record_added = satRec;
            McidasSatRenderable msr = (McidasSatRenderable) renderable;
            msr.addRecord(satRec);

            return true;
        }
    }

    protected class McidasSatRenderable
            extends NcSatelliteResource.SatRenderable<McidasMapCoverage> {

        @Override
        public void addRecord(IPersistable record) {
            RecordTileSetRenderable tileSet = null;

            synchronized (tileMap) {
                McidasRecord satRecord = (McidasRecord) record;
                GridGeometry2D gridGeom = satRecord.getGridGeometry();
                tileSet = new RecordTileSetRenderable(McidasSatResource.this,
                        satRecord, gridGeom, numLevels);
                tileSet.project(descriptor.getGridGeometry());
                tileMap.put(satRecord.getCoverage(), tileSet);
            }
        }
    }

    @Override
    protected void initializeFirstFrame(IPersistable rec) throws VizException {

        initialized = true;

        NcSatelliteUnits.register();
        NcUnits.register();
        PluginDataObject record = (PluginDataObject) rec;

        // Create the colorMap and set it in the colorMapParameters and init the
        // colorBar
        ColorMapParameters colorMapParameters = new ColorMapParameters();

        ColorMap colorMap;
        String colorMapName = resourceData.getColorMapName();

        try {

            // default to colormap name from attribute file

            // colormap name from SPF can override default
            String spfColorMapName = getCapability(ColorMapCapability.class)
                    .getColorMapParameters().getColorMapName();
            if (spfColorMapName != null
                    && !spfColorMapName.equals(DEFAULT_COLORMAP_NAME)
                    && !spfColorMapName.equals(colorMapName)) {
                colorMapName = spfColorMapName;
            }

            colorMap = (ColorMap) ColorMapUtil.loadColorMap(resourceData
                    .getResourceName().getRscCategory().getCategoryName(),
                    colorMapName);
        } catch (VizException e) {
            throw new VizException("Error loading colormap: " + colorMapName,
                    e);
        }

        colorMapParameters.setDisplayUnit(resourceData.getDisplayUnit());
        colorMapParameters.setColorMapMin(0.0f);
        colorMapParameters.setColorMapMax(255.0f);

        colorMapParameters.setColorMap(colorMap);
        getCapability(ColorMapCapability.class)
                .setColorMapParameters(colorMapParameters);

        getCapability(ImagingCapability.class).setSuppressingMenuItems(true);
        getCapability(ColorMapCapability.class).setSuppressingMenuItems(true);

        McidasRecord mcidas = (McidasRecord) record;
        McidasMapCoverage cov = mcidas.getCoverage();
        try {
            Rectangle[] downscaleSizes = GridDownscaler
                    .getDownscaleSizes(cov.getGridGeometry());
            numLevels = downscaleSizes.length;
        } catch (Exception e) {
            throw new VizException(
                    "Unable to get grid geometry for record: " + record, e);
        }
    }

    @Override
    public void generateAndStoreColorBarLabelingInformation(IPersistable record,
            int imageTypeNumber) {

        Double minPixVal = null;
        Double maxPixVal = null;
        ParamLevelMatchCriteria matchCriteria = new ParamLevelMatchCriteria();

        String dataUnitString = getDataUnitsFromRecord((McidasRecord) record);
        List<String> parameterList = getParameterList(record);

        ImagePreferences imgPref = new ImagePreferences();

        ColorBarFromColormap colorBar = (ColorBarFromColormap) this.cbarResource
                .getResourceData().getColorbar();
        if (colorBar.getColorMap() == null) {
            colorBar.setColorMap(
                    (ColorMap) getCapability(ColorMapCapability.class)
                            .getColorMapParameters().getColorMap());
        }
        matchCriteria.setParameterName(parameterList);

        try {

            // Use new style rule manager
            StyleRule sr = StyleManager.getInstance().getStyleRule(
                    StyleManager.StyleType.IMAGERY, matchCriteria);

            if (sr != null) {
                AbstractStylePreferences stylePref = sr.getPreferences();
                if (stylePref != null
                        && stylePref instanceof ImagePreferences) {
                    imgPref = (ImagePreferences) stylePref;

                    // Might need to change this if/when we use the data-scaling
                    SampleFormat sampleFormat = imgPref.getSampleFormat();
                    if (sampleFormat instanceof NumericFormat) {
                        minPixVal = ((NumericFormat) sampleFormat)
                                .getMinValue();
                        maxPixVal = ((NumericFormat) sampleFormat)
                                .getMaxValue();
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
                        colorBar.setDisplayUnitStr(
                                imgPref.getDisplayUnitLabel());
                    }
                }
            }
        } catch (StyleException | NullPointerException e) {
            statusHandler.error("Error getting stylerule prefences", e);
        }
        // These label value calculations are from legacy imlabl.f
        if ((dataUnitString != null)
                && ((dataUnitString).compareTo("BRIT") == 0)
                && ((imageTypeNumber == 8) || (imageTypeNumber == 128))
                && (minPixVal != null) && (maxPixVal != null)
                && (colorBar != null) && (imgPref.getDataMapping() == null)) {

            int imndlv = (int) Math.min(256, (maxPixVal - minPixVal + 1));
            NcIRPixelToTempConverter pixelToTemperatureConverter = new NcIRPixelToTempConverter();
            double tmpk = pixelToTemperatureConverter.convert(minPixVal.doubleValue());
            double tmpc = SI.KELVIN.getConverterTo(SI.CELSIUS).convert(tmpk);

            DataMappingPreferences dmPref = new DataMappingPreferences();
            DataMappingEntry entry = new DataMappingEntry();

            entry.setPixelValue(1.0);
            entry.setDisplayValue(tmpc);
            entry.setLabel(String.format(COLORBAR_STRING_FORMAT, tmpc));
            dmPref.addEntry(entry);

            int ibrit = (imndlv - 1) + minPixVal.intValue();
            tmpk = pixelToTemperatureConverter.convert(ibrit);
            tmpc = SI.KELVIN.getConverterTo(SI.CELSIUS).convert(tmpk);

            entry = new DataMappingEntry();
            entry.setPixelValue(Double.valueOf(imndlv));
            entry.setDisplayValue(tmpc);
            entry.setLabel(String.format(COLORBAR_STRING_FORMAT, tmpc));
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

        }

        // No existing data mapping, so we generate it
        else if (imgPref.getDataMapping() == null) {
            // For all other images, the native units are used for display
            if (imgPref.getDisplayUnitLabel() != null) {

                colorBar.setDisplayUnitStr(imgPref.getDisplayUnitLabel());
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
                imgPref = new ImagePreferences();
                imgPref.setDataMapping(dmPref);
                imgPref.setSampleFormat(new NumericFormat(0.0, 255.0));
                colorBar.setImagePreferences(imgPref);
                colorBar.scalePixelValues();
            }

        }

        colorBar.setAlignLabelInTheMiddleOfInterval(false);
        if (!colorBar.equals(resourceData.getColorBar())) {
            this.resourceData.setColorBar(colorBar);
        }

    }

    @Override
    protected AbstractFrameData createNewFrame(DataTime frameTime,
            int frameInterval) {
        return new FrameData(frameTime, frameInterval);
    }

    @Override
    public void project(CoordinateReferenceSystem mapData) throws VizException {
        for (AbstractFrameData frm : frameDataMap.values()) {
            McidasSatRenderable sr = (McidasSatRenderable) ((FrameData) frm).renderable;
            if (sr != null) {
                sr.project();
            }
        }
    }

    @Override
    protected List<String> getParameterList(IPersistable pdo) {

        String paramStr = ((McidasRecord) pdo).getSatelliteId() + "_"
                + ((McidasRecord) pdo).getImageTypeId();
        List<String> paramList = new ArrayList<>(0);
        paramList.add(paramStr);
        return paramList;
    }

    @Override
    protected int getImageTypeNumber(IPersistable pdo) {
        return Integer.parseInt(((McidasRecord) pdo).getImageTypeId());
    }

    @Override
    protected String getUnits(IPersistable record) {
        return ((McidasRecord) record).getCalType();
    }

    @Override
    String getImageTypeFromRecord(PluginDataObject pdo) {
        return ((McidasRecord) pdo).getImageTypeId();
    }

    @Override
    String getDataUnitsFromRecord(PluginDataObject pdo) {
        return ((McidasRecord) pdo).getCalType();
    }

    String getCreatingEntityFromRecord(PluginDataObject pdo) {
        return ((McidasRecord) pdo).getSatelliteId();
    }
}
