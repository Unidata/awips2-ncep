package gov.noaa.nws.ncep.viz.rsc.satellite.rsc;

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
import gov.noaa.nws.ncep.viz.resources.util.VariableSubstitutorNCEP;
import gov.noaa.nws.ncep.viz.rsc.satellite.units.NcIRPixelToTempConverter;
import gov.noaa.nws.ncep.viz.rsc.satellite.units.NcIRTempToPixelConverter;
import gov.noaa.nws.ncep.viz.rsc.satellite.units.NcSatelliteUnits;
import gov.noaa.nws.ncep.viz.ui.display.ColorBarFromColormap;

import java.awt.Rectangle;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;

import org.geotools.coverage.grid.GeneralGridEnvelope;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.GeneralEnvelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.raytheon.uf.common.colormap.ColorMap;
import com.raytheon.uf.common.colormap.prefs.ColorMapParameters;
import com.raytheon.uf.common.colormap.prefs.DataMappingPreferences;
import com.raytheon.uf.common.colormap.prefs.DataMappingPreferences.DataMappingEntry;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.persist.IPersistable;
import com.raytheon.uf.common.dataplugin.satellite.units.counts.DerivedWVPixel;
import com.raytheon.uf.common.dataplugin.satellite.units.generic.GenericPixel;
import com.raytheon.uf.common.dataplugin.satellite.units.goes.PolarPrecipWaterPixel;
import com.raytheon.uf.common.dataplugin.satellite.units.water.BlendedTPWPixel;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint.ConstraintType;
import com.raytheon.uf.common.derivparam.library.DerivedParameterRequest;
import com.raytheon.uf.common.geospatial.ISpatialObject;
import com.raytheon.uf.common.geospatial.MapUtil;
import com.raytheon.uf.common.geospatial.interpolation.GridDownscaler;
import com.raytheon.uf.common.style.AbstractStylePreferences;
import com.raytheon.uf.common.style.ParamLevelMatchCriteria;
import com.raytheon.uf.common.style.StyleException;
import com.raytheon.uf.common.style.StyleManager;
import com.raytheon.uf.common.style.StyleRule;
import com.raytheon.uf.common.style.image.DataScale;
import com.raytheon.uf.common.style.image.ImagePreferences;
import com.raytheon.uf.common.style.image.SamplePreferences;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.capabilities.ColorMapCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.ImagingCapability;
import com.raytheon.uf.viz.core.tile.RecordTileSetRenderable;
import com.raytheon.viz.satellite.SatelliteConstants;

/**
 * Provides Mcidas satellite raster rendering support.
 * 
 * <pre>
 * 
 *  SOFTWARE HISTORY
 * 
 *  Date        Ticket#     Engineer    Description
 *  ----------  ----------  ----------- --------------------------
 *  05/24/2010    #281       ghull        Initial creation 
 *  06/07/2012    #717       archana      Added the methods getImageTypeNumber(),
 *                                        getParameterList(), getLocFilePathForImageryStyleRule()
 *                                        Updated getDataUnitsFromRecord() to get the units from the database
 *  11/29/2012    #630       ghull        IGridGeometryProvider
 *  02/13/2015    #R6345     mkean        add areaName, resolution and getRscAttrSetName 
 *                                        to legendStr
 *  10/15/2015    #R7190     R. Reynolds  Added support for Mcidas
 *  12/03/2015    R12953     R. Reynolds  Modified to enhance Legend title
 *  02/04/2016    R14142     RCReynolds   Moved mcidas related string construction out to ResourceDefinition
 *  04/12/2016    R15945     RCReynolds   Added code to build customized string for input to getLegendString
 *  06/06/2016    R15945     RCReynolds   Used McidasConstants instead of SatelliteConstants
 *  06/01/2016    R18511     kbugenhagen  Refactored to use NcSatelliteResource
 *                                        instead of AbstractSatelliteResource
 *                                        in order to use TileSetRenderable.
 * </pre>
 * 
 * @author ghull
 * @version 1
 */

public class McidasSatResource extends NcSatelliteResource {

    private int numLevels;

    public McidasSatResource(SatelliteResourceData data, LoadProperties props) {
        super(data, props);

        /**
         * Get configuredLegendString (with aliases) from resourceDefinition. If
         * empty string is returned then get regular aliased legend.
         * 
         */
        legendStr = createLegendString();
    }

    /**
     * Create legend string from subtypes
     * 
     * @return
     */
    private String createLegendString() {

        StringBuffer sb = new StringBuffer("");
        char x;
        ResourceName rscName = resourceData.getResourceName();
        String legendStr = "";

        try {
            ResourceDefnsMngr rscDefnsMngr = ResourceDefnsMngr.getInstance();
            ResourceDefinition rscDefn = rscDefnsMngr
                    .getResourceDefinition(rscName.getRscType());
            AttributeSet attributeSet = rscDefnsMngr.getAttrSet(rscName);
            rscDefn.setAttributeSet(attributeSet);
            HashMap<String, String> attributes = attributeSet.getAttributes();
            Map<String, String> variables = new HashMap<>();
            String legendStringAttribute = attributes.get("legendString");
            String[] subtypeParam = rscDefn.getSubTypeGenerator().split(",");
            for (int k = 0; k < subtypeParam.length; k++) {
                legendStr += resourceData.getMetadataMap()
                        .get(subtypeParam[k].toString()).getConstraintValue()
                        .toString()
                        + "_";
            }
            legendStr = rscDefn.getRscGroupDisplayName(legendStr) + " ";
            // what's in subType generators
            String[] keysValues = rscDefn.getMcidasAliasedValues().split(",");

            //
            /**
             * These are the keywords found in the legendString (built from
             * subType generator content)
             */
            String value = "";
            for (int i = 0; i < keysValues.length; i++) {
                value = keysValues[i].split(":")[1];
                if (value != null && !value.isEmpty()) {
                    if (keysValues[i].startsWith(McidasConstants.SATELLLITE
                            + ":")) {
                        variables.put(McidasConstants.SATELLLITE, value);
                    } else if (keysValues[i].startsWith(McidasConstants.AREA
                            + ":")) {
                        variables.put(McidasConstants.AREA, value);
                    } else if (keysValues[i]
                            .startsWith(McidasConstants.RESOLUTION + ":")) {
                        variables.put(McidasConstants.RESOLUTION, value);
                    }
                }
            }

            // add in these last two.

            value = rscDefn.getResourceDefnName();
            if (value != null && !value.isEmpty())
                variables.put(McidasConstants.RESOURCE_DEFINITION,
                        rscDefn.getResourceDefnName());

            value = rscDefnsMngr.getAttrSet(rscName).getName();
            if (value != null && !value.isEmpty())
                variables.put(McidasConstants.CHANNEL,
                        rscDefnsMngr.getAttrSet(rscName).getName());

            if (legendStringAttribute == null) {
                return legendStr;
            }

            /*
             * "variables map" now contains keywords/values available for
             * building the custom legend string. Examine marked-up legend
             * string looking for {keyword} that matches in "variables". If it
             * doesn't then remove it from legendString.
             */
            Pattern p = Pattern.compile("\\{(.*?)\\}");
            Matcher m = p.matcher(legendStringAttribute.toString());
            while (m.find()) {
                value = variables.get(m.group(1));
                if (value == null || value.isEmpty()) {
                    legendStringAttribute = legendStringAttribute.replace("{"
                            + m.group(1) + "}", "");
                }
            }

            /*
             * change all occurrences of '{' to "${" because that's what
             * VariableSubstituterNCEP expects
             */
            for (int ipos = 0; ipos < legendStringAttribute.length(); ipos++) {
                x = legendStringAttribute.charAt(ipos);
                sb.append(x == '{' ? "${" : x);
            }

            String customizedlegendString = VariableSubstitutorNCEP
                    .processVariables(sb.toString(), variables);

            /*
             * If user coded legendString properly there shouldn't be any "${"
             * present, but if there are then change them back to "{"
             */
            sb.setLength(0);
            for (int ipos = 0; ipos < customizedlegendString.length(); ipos++) {
                x = customizedlegendString.charAt(ipos);
                sb.append(x == '$' ? "{" : x);
            }
            customizedlegendString = sb.toString();
            legendStr += rscDefn.getRscAttributeDisplayName("");
            legendStr = (customizedlegendString.isEmpty()) ? legendStr
                    : customizedlegendString;
            legendStr = legendStr.substring(0, legendStr.length() - 1);
            legendStr = rscDefn.getRscGroupDisplayName(legendStr) + " ";
            AttributeSet attSet = rscDefnsMngr.getAttrSet(rscName);
            rscDefn.setAttributeSet(attSet);
            legendStr += rscDefn.getRscAttributeDisplayName("");
            legendStr.trim();
        } catch (Exception ex) {
            statusHandler.error("Error building legend string ", ex);
        }

        return legendStr;

    }

    protected class FrameData extends NcSatelliteResource.FrameData {

        // One renderable per frame, each renderable may have multiple tiles
        protected McidasSatRenderable renderable;

        // Spatial coverage for previously added record. Used to dispose of
        // previous tile in tilemap if current record is better time match.
        protected McidasMapCoverage coveragePrevAddedRecord;

        protected FrameData(DataTime time, int interval) {
            super(time, interval);
            setRenderable(new McidasSatRenderable());
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
                                            + satRec, e);
                            return false;
                        }
                    }
                }
            }

            if (tileTimePrevAddedRecord != null) {

                DataTime newTileTime = ((PluginDataObject) satRec)
                        .getDataTime();

                // new record not a better match
                if (timeMatch(newTileTime) >= timeMatch(tileTimePrevAddedRecord)) {
                    return false;
                } else {
                    // new record is a better match, so dispose of old tile
                    getRenderable().getTileMap()
                            .remove(coveragePrevAddedRecord);
                }
            }

            Collections.sort(McidasSatResource.this.dataTimes);
            int imageTypeNumber = getImageTypeNumber(satRec);

            generateAndStoreColorBarLabelingInformation(satRec, imageTypeNumber);

            tileTimePrevAddedRecord = ((PluginDataObject) satRec).getDataTime();
            coveragePrevAddedRecord = ((McidasRecord) satRec).getCoverage();
            getRenderable().addRecord(satRec);

            return true;
        }

        @Override
        public SatRenderable<McidasMapCoverage> getRenderable() {
            return renderable;
        }

        public void setRenderable(McidasSatRenderable renderable) {
            this.renderable = renderable;
        }

    }

    protected class McidasSatRenderable extends
            NcSatelliteResource.SatRenderable<McidasMapCoverage> {

        /*
         * (non-Javadoc)
         * 
         * @see
         * gov.noaa.nws.ncep.viz.rsc.satellite.rsc.NcSatelliteResource.SatRenderable
         * #addRecord(com.raytheon.uf.common.dataplugin.persist.IPersistable)
         */
        @Override
        public void addRecord(IPersistable record) {
            RecordTileSetRenderable tileSet = null;

            synchronized (tileMap) {
                McidasRecord satRecord = (McidasRecord) record;
                ISpatialObject spatialObj = (ISpatialObject) satRecord
                        .getSpatialObject();
                GridGeometry2D gridGeom = MapUtil.getGridGeometry(spatialObj);
                if (tileSet == null) {
                    String projection = getProjectionFromRecord(satRecord);
                    if (!projection.equalsIgnoreCase("STR")
                            && !projection.equalsIgnoreCase("MER")
                            && !projection.equalsIgnoreCase("LCC")) {

                        gridGeom = createNativeGeometry(satRecord);
                    }
                    tileSet = new RecordTileSetRenderable(
                            McidasSatResource.this, satRecord, gridGeom,
                            numLevels);
                    tileSet.project(descriptor.getGridGeometry());
                    tileMap.put(satRecord.getCoverage(), tileSet);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see gov.noaa.nws.ncep.viz.rsc.satellite.rsc.NcSatelliteResource#
     * initializeFirstFrame
     * (com.raytheon.uf.common.dataplugin.persist.IPersistable)
     */
    @Override
    protected void initializeFirstFrame(IPersistable rec) throws VizException {

        initialized = true;

        NcSatelliteUnits.register();
        NcUnits.register();

        // NOTE : This will replace Raytheons IRPixel (and
        // IRPixelToTempConverter) with ours
        // even for D2D's GiniSatResource.

        Unit<?> dataUnit = null;
        PluginDataObject record = (PluginDataObject) rec;
        String dataUnitStr = getDataUnitsFromRecord((PluginDataObject) record);
        String imgType = null;
        DerivedParameterRequest request = (DerivedParameterRequest) record
                .getMessageData();

        if (request == null) {
            imgType = getImageTypeFromRecord(record);
        } else {
            imgType = request.getParameterAbbreviation();
        }

        if (dataUnitStr != null && !dataUnitStr.isEmpty() && request == null) {
            try {
                dataUnit = UnitFormat.getUCUMInstance().parseSingleUnit(
                        dataUnitStr, new ParsePosition(0));
            } catch (ParseException e) {
                throw new VizException("Unable parse units : " + dataUnitStr, e);
            }
        } else if (request != null) {
            if (imgType.equals("satDivWVIR")) {
                dataUnit = new DerivedWVPixel();
            } else {
                dataUnit = new GenericPixel();
            }
        } else
            dataUnit = new GenericPixel();

        // This logic came from Raytheon's SatResource.
        String creatingEntity = null;

        if (imgType.equals(SatelliteConstants.PRECIP)) {

            creatingEntity = getCreatingEntityFromRecord(record);

            if (creatingEntity.equals(SatelliteConstants.DMSP)
                    || creatingEntity.equals(SatelliteConstants.POES)) {

                dataUnit = new PolarPrecipWaterPixel();
            } else if (creatingEntity.equals(SatelliteConstants.MISC)) {

                dataUnit = new BlendedTPWPixel();
            }
        }

        // Create the colorMap and set it in the colorMapParameters and init the
        // colorBar
        ColorMapParameters colorMapParameters = new ColorMapParameters();

        ColorMap colorMap;
        try {
            colorMap = (ColorMap) ColorMapUtil.loadColorMap(resourceData
                    .getResourceName().getRscCategory().getCategoryName(),
                    resourceData.getColorMapName());
        } catch (VizException e) {
            throw new VizException("Error loading colormap: "
                    + resourceData.getColorMapName());
        }

        colorMapParameters.setDisplayUnit(resourceData.getDisplayUnit());
        colorMapParameters.setDataUnit(dataUnit);

        // Set real color and data max/min
        colorMapParameters.setDataMin(0.0f);
        colorMapParameters.setDataMax(255.0f);
        colorMapParameters.setColorMapMin(0.0f);
        colorMapParameters.setColorMapMax(255.0f);

        colorMapParameters.setColorMap(colorMap);
        getCapability(ColorMapCapability.class).setColorMapParameters(
                colorMapParameters);

        getCapability(ImagingCapability.class).setSuppressingMenuItems(true);
        getCapability(ColorMapCapability.class).setSuppressingMenuItems(true);

        McidasRecord mcidas = (McidasRecord) record;
        McidasMapCoverage cov = mcidas.getCoverage();
        try {
            Rectangle[] downscaleSizes = GridDownscaler.getDownscaleSizes(cov
                    .getGridGeometry());
            numLevels = downscaleSizes.length;
        } catch (Exception e) {
            throw new VizException("Unable to get grid geometry for record: "
                    + record);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see gov.noaa.nws.ncep.viz.rsc.satellite.rsc.NcSatelliteResource#
     * generateAndStoreColorBarLabelingInformation
     * (com.raytheon.uf.common.dataplugin.persist.IPersistable, int)
     */
    @Override
    public void generateAndStoreColorBarLabelingInformation(
            IPersistable record, int imageTypeNumber) {

        double minPixVal = Double.NaN;
        double maxPixVal = Double.NaN;
        ParamLevelMatchCriteria matchCriteria = new ParamLevelMatchCriteria();

        String dataUnitString = getDataUnitsFromRecord((McidasRecord) record);
        List<String> parameterList = getParameterList(record);

        ImagePreferences imgPref = new ImagePreferences();

        ColorBarFromColormap colorBar = (ColorBarFromColormap) this.cbarResource
                .getResourceData().getColorbar();
        if (colorBar.getColorMap() == null) {
            colorBar.setColorMap((ColorMap) getCapability(
                    ColorMapCapability.class).getColorMapParameters()
                    .getColorMap());
        }
        matchCriteria.setParameterName(parameterList);

        try {

            // Use new style rule manager
            StyleRule sr = StyleManager.getInstance().getStyleRule(
                    StyleManager.StyleType.IMAGERY, matchCriteria);

            if (sr != null) {
                AbstractStylePreferences stylePref = sr.getPreferences();
                if (stylePref != null && stylePref instanceof ImagePreferences) {
                    imgPref = (ImagePreferences) stylePref;

                    // Might need to change this if/when we use the data-scaling
                    SamplePreferences samplePref = imgPref.getSamplePrefs();
                    if (samplePref != null) {
                        minPixVal = imgPref.getSamplePrefs().getMinValue();
                        maxPixVal = imgPref.getSamplePrefs().getMaxValue();
                    } else if (imgPref.getDataScale() != null) {
                        DataScale ds = imgPref.getDataScale();
                        if (ds.getMaxValue() != null)
                            maxPixVal = ds.getMaxValue().doubleValue();
                        if (ds.getMinValue() != null)
                            minPixVal = ds.getMinValue().doubleValue();
                    }

                    colorBar.setImagePreferences(imgPref);
                    if (imgPref.getDisplayUnitLabel() != null) {
                        colorBar.setDisplayUnitStr(imgPref
                                .getDisplayUnitLabel());
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
            entry.setLabel(String.format(COLORBAR_STRING_FORMAT, tmpc));
            dmPref.addEntry(entry);

            int ibrit = (imndlv - 1) + (int) minPixVal;
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
            if (!colorBar.isScalingAttemptedForThisColorMap())
                colorBar.scalePixelValues();

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
                SamplePreferences sPref = new SamplePreferences();
                sPref.setMaxValue(255);
                sPref.setMinValue(0);
                imgPref.setSamplePrefs(sPref);
                colorBar.setImagePreferences(imgPref);
                colorBar.scalePixelValues();
            }

        }

        colorBar.setAlignLabelInTheMiddleOfInterval(false);
        if (!colorBar.equals(resourceData.getColorBar())) {
            this.resourceData.setColorBar(colorBar);
        }

    }

    /**
     * Create grid geometry for the data record.
     * 
     * @param pdo
     *            data record
     * @return geometry
     */
    public GridGeometry2D createNativeGeometry(PluginDataObject pdo) {

        if (!(pdo instanceof McidasRecord))
            return null;

        McidasRecord satRec = (McidasRecord) pdo;
        McidasMapCoverage coverage = satRec.getCoverage();

        GeneralEnvelope env = new GeneralEnvelope(2);
        env.setCoordinateReferenceSystem(satRec.getCoverage().getCrs());

        int minX = coverage.getUpperLeftElement();
        int maxX = coverage.getUpperLeftElement()
                + (coverage.getNx() * coverage.getElementRes());
        int minY = coverage.getUpperLeftLine()
                + (coverage.getNy() * coverage.getLineRes());
        minY = -minY;
        int maxY = -1 * coverage.getUpperLeftLine();
        env.setRange(0, minX, maxX);
        env.setRange(1, minY, maxY);

        GridGeometry2D mapGeom = new GridGeometry2D(new GeneralGridEnvelope(
                new int[] { 0, 0 }, new int[] { coverage.getNx(),
                        coverage.getNy() }, false), env);

        return mapGeom;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * gov.noaa.nws.ncep.viz.rsc.satellite.rsc.NcSatelliteResource#createNewFrame
     * (com.raytheon.uf.common.time.DataTime, int)
     */
    @Override
    protected AbstractFrameData createNewFrame(DataTime frameTime,
            int frameInterval) {
        return (AbstractFrameData) new FrameData(frameTime, frameInterval);
    }

    /*
     * (non-Javadoc)
     * 
     * @see gov.noaa.nws.ncep.viz.rsc.satellite.rsc.NcSatelliteResource#
     * isCloudHeightCompatible()
     */
    @Override
    public boolean isCloudHeightCompatible() {
        RequestConstraint imgTypeConstraint = resourceData.getMetadataMap()
                .get("imageTypeId");
        if (imgTypeConstraint != null) {
            if (imgTypeConstraint.getConstraintType() == ConstraintType.EQUALS
                    && imgTypeConstraint.getConstraintValue().equals("IR")) {
                return true;
            }
        }
        return false;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * gov.noaa.nws.ncep.viz.rsc.satellite.rsc.NcSatelliteResource#project(org
     * .opengis.referencing.crs.CoordinateReferenceSystem)
     */
    @Override
    public void project(CoordinateReferenceSystem mapData) throws VizException {
        for (AbstractFrameData frm : frameDataMap.values()) {
            SatRenderable<McidasMapCoverage> sr = ((FrameData) frm).renderable;
            if (sr != null) {
                sr.project();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * gov.noaa.nws.ncep.viz.rsc.satellite.rsc.NcSatelliteResource#getParameterList
     * (com.raytheon.uf.common.dataplugin.persist.IPersistable)
     */
    @Override
    protected List<String> getParameterList(IPersistable pdo) {

        String paramStr = ((McidasRecord) pdo).getSatelliteId() + "_"
                + ((McidasRecord) pdo).getImageTypeId();
        List<String> paramList = new ArrayList<>(0);
        paramList.add(paramStr);
        return paramList;
    }

    String getProjectionFromRecord(PluginDataObject pdo) {
        return ((McidasRecord) pdo).getProjection();
    }

    /*
     * (non-Javadoc)
     * 
     * @see gov.noaa.nws.ncep.viz.rsc.satellite.rsc.NcSatelliteResource#
     * getImageTypeNumber
     * (com.raytheon.uf.common.dataplugin.persist.IPersistable)
     */
    @Override
    protected int getImageTypeNumber(IPersistable pdo) {
        return Integer.parseInt(((McidasRecord) pdo).getImageTypeId());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * gov.noaa.nws.ncep.viz.rsc.satellite.rsc.NcSatelliteResource#getUnits(
     * com.raytheon.uf.common.dataplugin.persist.IPersistable)
     */
    @Override
    protected String getUnits(IPersistable record) {
        return ((McidasRecord) record).getCalType();
    }

    /*
     * (non-Javadoc)
     * 
     * @see gov.noaa.nws.ncep.viz.rsc.satellite.rsc.NcSatelliteResource#
     * getImageTypeFromRecord
     * (com.raytheon.uf.common.dataplugin.PluginDataObject)
     */
    @Override
    String getImageTypeFromRecord(PluginDataObject pdo) {
        return ((McidasRecord) pdo).getImageTypeId();
    }

    /*
     * (non-Javadoc)
     * 
     * @see gov.noaa.nws.ncep.viz.rsc.satellite.rsc.NcSatelliteResource#
     * getDataUnitsFromRecord
     * (com.raytheon.uf.common.dataplugin.PluginDataObject)
     */
    @Override
    String getDataUnitsFromRecord(PluginDataObject pdo) {
        return ((McidasRecord) pdo).getCalType();
    }

    String getCreatingEntityFromRecord(PluginDataObject pdo) {
        return ((McidasRecord) pdo).getSatelliteId();
    }

}
