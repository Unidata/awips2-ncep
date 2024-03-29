/*
 * gov.noaa.nws.ncep.viz.rsc.ncradar.rsc.RadarImageResource
 *
 * 12-07-2011
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.viz.rsc.ncradar.rsc;

import java.awt.Rectangle;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.measure.IncommensurableException;
import javax.measure.UnconvertibleException;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.format.ParserException;

import org.eclipse.swt.graphics.RGB;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.raytheon.uf.common.colormap.ColorMap;
import com.raytheon.uf.common.colormap.ColorMapException;
import com.raytheon.uf.common.colormap.ColorMapLoader;
import com.raytheon.uf.common.colormap.IColorMap;
import com.raytheon.uf.common.colormap.image.ColorMapData;
import com.raytheon.uf.common.colormap.prefs.ColorMapParameters;
import com.raytheon.uf.common.colormap.prefs.DataMappingPreferences;
import com.raytheon.uf.common.colormap.prefs.DataMappingPreferences.DataMappingEntry;
import com.raytheon.uf.common.dataplugin.radar.RadarRecord;
import com.raytheon.uf.common.dataplugin.radar.util.RadarUtil;
import com.raytheon.uf.common.datastorage.StorageException;
import com.raytheon.uf.common.geospatial.MapUtil;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.style.StyleException;
import com.raytheon.uf.common.style.image.ColorMapParameterFactory;
import com.raytheon.uf.common.style.image.ImagePreferences;
import com.raytheon.uf.common.style.image.NumericFormat;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.units.PiecewisePixel;
import com.raytheon.uf.common.units.UnitConv;
import com.raytheon.uf.viz.core.DrawableImage;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IMesh;
import com.raytheon.uf.viz.core.IMeshCallback;
import com.raytheon.uf.viz.core.PixelCoverage;
import com.raytheon.uf.viz.core.data.IColorMapDataRetrievalCallback;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.IImage;
import com.raytheon.uf.viz.core.drawables.IWireframeShape;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.drawables.ext.colormap.IColormappedImageExtension;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.IResourceDataChanged;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.ResourceProperties;
import com.raytheon.uf.viz.core.rsc.capabilities.ColorMapCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.ImagingCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.OutlineCapability;
import com.raytheon.uf.viz.core.rsc.hdf5.ImageTile;
import com.raytheon.viz.awipstools.capabilities.RangeRingsOverlayCapability;
import com.raytheon.viz.awipstools.capabilityInterfaces.IRangeableResource;
import com.raytheon.viz.radar.VizRadarRecord;
import com.raytheon.viz.radar.util.DataUtilities;

import gov.noaa.nws.ncep.viz.common.ColorMapUtil;
import gov.noaa.nws.ncep.viz.resources.colorBar.ColorBarResource;
import gov.noaa.nws.ncep.viz.resources.colorBar.ColorBarResourceData;
import gov.noaa.nws.ncep.viz.ui.display.ColorBarFromColormap;
import tec.uom.se.AbstractConverter;
import tec.uom.se.AbstractUnit;
import tec.uom.se.format.SimpleUnitFormat;
import tec.uom.se.function.MultiplyConverter;

/**
 * TODO Add Description
 *
 * This class is based on Raytheon's code.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer   Description
 * ------------- -------- ---------- -------------------------------------------
 * Dec 07, 2011  541      S. Gurung  Initial creation
 * Dec 16, 2011           S. Gurung  Added resourceAttrsModified()
 * Jan 03, 2011           S. Gurung  Changed circle color to black
 * Apr 02, 2012  651      S. Gurung  Added fix for applying resource attributes
 *                                   changes.
 * Jun 07, 2012  717      Archana    Updated setColorMapParameters() to store
 *                                   label information for the colorbar
 * Dec 19, 2012  960      Greg Hull  override propertiesChanged() to update
 *                                   colorBar.
 * Jun 10, 2013  999      Greg Hull  RadarRecords from RadarFrameData, rm
 *                                   interrogator
 * Jun 30, 2014  3165     njensen    Use ColorMapLoader to get ColorMap
 * Jun 15, 2016  19647    bsteffen   Remove unused reference to RadarTileSet,
 *                                   formatting, and cleanup.
 * Mar 20, 2019  7569     tgurney    Fix colorbar alignment
 * Apr 15, 2019 7596      lsingh     Updated units framework to JSR-363.
 * Apr 20, 2020  8145     randerso   Replace SamplePreferences with SampleFormat
 * 
 *
 * </pre>
 *
 * @author sgurung
 */

public abstract class RadarImageResource<D extends IDescriptor>
        extends AbstractRadarResource<D>
        implements IMeshCallback, IRangeableResource, IResourceDataChanged {
    protected static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(RadarImageResource.class);

    private static final int RANGE_CIRCLE_PTS = 360;

    protected Map<Float, IWireframeShape> rangeCircle;

    // a map from the time of the radar record to the image
    // TODO : move DrawableImage to a member of ImageRadarFrameData.
    protected Map<DataTime, DrawableImage> images = new ConcurrentHashMap<>();

    protected ColorBarResource cbarResource;

    protected ResourcePair cbarRscPair;

    protected boolean refreshImage = false;

    /**
     * @param resourceData
     * @param loadProperties
     * @throws VizException
     */
    protected RadarImageResource(RadarResourceData resourceData,
            LoadProperties loadProperties) throws VizException {
        super(resourceData, loadProperties);
        rangeCircle = new HashMap<>();
        refreshImage = false;
    }

    // TODO : add/move DrawableImage here too
    protected class RadarImageFrameData extends RadarFrameData {

        public RadarImageFrameData(DataTime time, int interval) {
            super(time, interval);
        }

        @Override
        public boolean updateFrameData(IRscDataObject rscDataObj) {

            super.updateFrameData(rscDataObj);

            try {
                setColorMapParameters(getRadarRecord());
            } catch (VizException e) {
                statusHandler.debug(
                        "Failed to set color map parameters from radar record "
                                + getRadarRecord().getDataURI(),
                        e);
            }
            return true;
        }

        @Override
        public void dispose() {

        }
    }

    @Override
    protected AbstractFrameData createNewFrame(DataTime frameTime,
            int frameInterval) {
        return new RadarImageFrameData(frameTime, frameInterval);
    }

    @SuppressWarnings("unchecked")
    private void setColorMapParameters(VizRadarRecord radarRecord)
            throws VizException {

        // TODO : replace this with call to VizRadarRecord
        // dataStore = radarRecord.getStoredData();
        // File loc = HDF5Util.findHDF5Location(radarRecord);
        //
        // IDataStore dataStore = DataStoreFactory.getDataStore(loc);
        //
        // RadarDataRetriever.populateRadarRecord(dataStore, radarRecord);
        // Don't need the store for the units
        Unit<?> dataUnit = null;
        if (radarRecord.getUnit() != null) {
            try {
                dataUnit = SimpleUnitFormat.getInstance(SimpleUnitFormat.Flavor.ASCII).parseProductUnit(
                        radarRecord.getUnit(), new ParsePosition(0));
            } catch (ParserException e) {
                throw new VizException("Unable to parse units ", e);
            }
        } else {
            dataUnit = AbstractUnit.ONE;
        }
        int numLevels = radarRecord.getNumLevels();
        Object[] thresholds = radarRecord.getDecodedThresholds();
        DataMappingPreferences dmPref = new DataMappingPreferences();
        DataMappingEntry dmEntry;
        List<DataMappingEntry> dmEntriesList = new ArrayList<>(0);
        if (numLevels <= 16) {
            List<Integer> pixel = new ArrayList<>();
            List<Float> real = new ArrayList<>();
            for (int i = 0; i < numLevels; i++) {
                dmEntry = new DataMappingEntry();

                dmEntry.setPixelValue((double) (i * 16));

                if (thresholds[i] instanceof Float) {
                    pixel.add(i);
                    real.add((Float) thresholds[i]);
                    dmEntry.setDisplayValue(
                            ((Float) thresholds[i]).doubleValue());
                    dmEntry.setLabel(((Float) thresholds[i]).toString());
                } else if (thresholds[i] == null) {
                    dmEntry.setLabel("");
                } else {
                    dmEntry.setLabel(thresholds[i].toString());
                }

                dmEntriesList.add(dmEntry);
            }

            double[] pix = new double[pixel.size()];
            int i = 0;
            for (Integer p : pixel) {
                pix[i] = p;
                i++;
            }

            double[] std = new double[real.size()];
            i = 0;
            for (Float r : real) {
                std[i] = r;
                i++;
            }

            dataUnit = new PiecewisePixel(dataUnit, pix, std);
        } else {
            double offset = radarRecord.getThreshold(0);
            double scale = radarRecord.getThreshold(1);
            offset -= 2 * scale;
            double[] pix = { 2, 255 };
            double[] data = { 2 * scale + offset, 255 * scale + offset };
            dataUnit = new PiecewisePixel(dataUnit, pix, data);
        }

        ColorMap colorMap;
        try {
            colorMap = (ColorMap) ColorMapUtil
                    .loadColorMap(
                            resourceData.getResourceName().getRscCategory()
                                    .getCategoryName(),
                            resourceData.getColorMapName());
        } catch (VizException e) {
            throw new VizException(
                    "Error loading colormap: " + resourceData.getColorMapName(),
                    e);
        }

        ColorBarFromColormap colorBar = resourceData.getColorBar();

        colorBar.setColorMap(colorMap);

        ColorMapParameters colorMapParameters = new ColorMapParameters();
        colorMapParameters.setColorMap(colorMap);

        if (colorMapParameters.getDisplayUnit() == null) {
            colorMapParameters.setDisplayUnit(dataUnit);
        }

        colorMapParameters.setColorMapMax(255);
        colorMapParameters.setColorMapMin(0);
        colorMapParameters.setDataMax(255);
        colorMapParameters.setDataMin(0);

        // set in the capability not in the radar record.
        getCapability(ColorMapCapability.class)
                .setColorMapParameters(colorMapParameters);
        if (!dmEntriesList.isEmpty()) {
            DataMappingEntry[] dmEntryArray = new DataMappingEntry[dmEntriesList
                    .size()];
            dmEntriesList.toArray(dmEntryArray);
            dmPref.setSerializableEntries(dmEntryArray);
            ImagePreferences imgPref = new ImagePreferences();
            imgPref.setDataMapping(dmPref);
            imgPref.setSampleFormat(new NumericFormat(0.0, 255.0));
            colorBar.setImagePreferences(imgPref);
            colorBar.setDisplayUnitStr(radarRecord.getUnit());
            colorBar.setAlignLabelInTheMiddleOfInterval(
                    radarRecord.getNumLevels() > 16
                            || colorMap.getColors().size() <= 16);
            this.cbarResource.getResourceData().setColorBar(colorBar);
        }
    }

    @Override
    protected void disposeInternal() {
        super.disposeInternal();

        for (IWireframeShape shape : rangeCircle.values()) {
            if (shape != null) {
                shape.dispose();
            }
        }
        rangeCircle.clear();
        for (DrawableImage image : images.values()) {
            disposeImage(image);
        }
        images.clear();
        if (cbarResource.getResourceData().getColorbar() != null) {
            cbarResource.getResourceData().getColorbar().dispose();
            cbarResource.getResourceData().setColorBar(null);
        }

        issueRefresh();
    }

    @Override
    public void initResource(IGraphicsTarget target) throws VizException {

        /*
         * create the colorBar Resource and add it to the resourceList for this
         * descriptor.
         */
        cbarRscPair = ResourcePair.constructSystemResourcePair(
                new ColorBarResourceData(resourceData.getColorBar()));

        getDescriptor().getResourceList().add(cbarRscPair);
        getDescriptor().getResourceList().instantiateResources(getDescriptor(),
                true);

        cbarResource = (ColorBarResource) cbarRscPair.getResource();

        queryRecords();
    }

    /**
     * Create the radar tile given the tileRecord to populate and a RadarRecord
     * with all data populated
     *
     * @param target
     * @param tiltRecord
     * @param populatedRecord
     * @throws StorageException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws VizException
     */
    protected void createTile(IGraphicsTarget target,
            VizRadarRecord populatedRecord) throws StorageException,
            IOException, ClassNotFoundException, VizException {

        ColorMapParameters params = getColorMapParameters(target,
                populatedRecord);

        PixelCoverage coverage = buildCoverage(target, populatedRecord);
        if (coverage.getMesh() == null) {
            coverage.setMesh(buildMesh(target, populatedRecord));
        }

        IImage image = createImage(target, params, populatedRecord,
                new Rectangle(0, 0, populatedRecord.getNumBins(),
                        populatedRecord.getNumRadials()));
        DrawableImage dImage = images.put(populatedRecord.getDataTime(),
                new DrawableImage(image, coverage));
        if (dImage != null) {
            disposeImage(dImage);
        }
    }

    /**
     * Get the colormap parameters, expects a radar record populated with data
     *
     * @param target
     * @param record
     * @return
     * @throws VizException
     */
    protected ColorMapParameters getColorMapParameters(IGraphicsTarget target,
            RadarRecord record) throws VizException {
        ColorMapParameters params = getCapability(ColorMapCapability.class)
                .getColorMapParameters();

        String colorMapName = "";
        IColorMap colorMap = null;
        float cMapMax = 255;
        float cMapMin = 0;
        if (params != null && params.getDataUnit() != null) {
            return params;
        } else if (params != null) {
            colorMapName = params.getColorMapName();
            colorMap = params.getColorMap();
            // if colorMap range has changed, get updated max and min
            if (refreshImage) {
                cMapMax = params.getColorMapMax();
                cMapMin = params.getColorMapMin();
            }
        }

        // Setup the ColorMap settings
        int prodCode = record.getProductCode();
        Unit<?> dataUnit = DataUtilities.getDataUnit(record);

        try {
            params = ColorMapParameterFactory.build(null,
                    Integer.toString(prodCode), dataUnit, null,
                    resourceData.mode);
        } catch (StyleException e) {
            throw new VizException(e.getLocalizedMessage(), e);
        }
        if (params.getDisplayUnit() == null) {
            params.setDisplayUnit(record.getUnitObject());
        }
        if (params.getImageUnit() == dataUnit && record.getNumLevels() <= 16) {
            DataMappingPreferences dataMapping = new DataMappingPreferences();
            Object[] thresholds = record.getDecodedThresholds();
            for (int i = 1; i < record.getNumLevels(); i++) {
                DataMappingEntry entry = new DataMappingEntry();

                // Sets the position left or right, should be normalized to
                // the numLevels
                entry.setPixelValue((double) (i * 16));

                // Set the data value
                if (thresholds[i] instanceof Float) {
                    entry.setDisplayValue(
                            params.getDataToDisplayConverter().convert(i));
                } else if (thresholds[i] instanceof String) {
                    entry.setLabel((String) thresholds[i]);
                } else if (thresholds[i] == null) {
                    entry.setLabel("");
                } else {
                    entry.setLabel(thresholds[i].toString());
                }
                dataMapping.addEntry(entry);
            }
            params.setDataMapping(dataMapping);
            params.setColorBarIntervals(null);
        }

        getCapability(ColorMapCapability.class).setColorMapParameters(params);

        if (colorMap != null) {
            params.setColorMap(colorMap);
            params.setColorMapName(colorMapName);
        }

        if (params.getColorMap() == null) {
            if (colorMapName == null) {
                colorMapName = "Radar/OSF/16 Level Reflectivity";
            } else if (colorMapName.isEmpty()) {
                colorMapName = params.getColorMapName();
            }

            try {
                params.setColorMap(ColorMapLoader.loadColorMap(colorMapName));
            } catch (ColorMapException e) {
                throw new VizException(e);
            }

        }

        params.setColorMapMax(cMapMax);
        params.setColorMapMin(cMapMin);
        params.setDataMax(255);
        params.setDataMin(0);

        return params;
    }

    @Override
    protected void paintFrame(AbstractFrameData frameData,
            IGraphicsTarget target, PaintProperties paintProps)
            throws VizException {

        if (paintProps == null || paintProps.getDataTime() == null) {
            return;
        }

        VizRadarRecord radarRecord = ((RadarFrameData) getCurrentFrame())
                .getRadarRecord();

        if (radarRecord == null) {
            issueRefresh();
            return;
        }

        synchronized (this.images) {

            DataTime imgTime = radarRecord.getDataTime();

            this.actualLevel = String.format("%1.1f",
                    radarRecord.getTrueElevationAngle());

            try {
                DrawableImage image = images.get(imgTime);
                if (refreshImage) {
                    redoImage(imgTime);
                    image = null;
                    images.clear();
                }
                if (image == null || image.getCoverage() == null) {
                    if (radarRecord.getStoredDataAsync() == null) {
                        issueRefresh();
                        return;
                    }
                    createTile(target, radarRecord);
                    image = images.get(imgTime);
                }

                if (image != null) {

                    ImagingCapability imgCap = new ImagingCapability(); // getCapability(ImagingCapability.class);//
                    imgCap.setBrightness(resourceData.getBrightness());
                    imgCap.setContrast(resourceData.getContrast());
                    imgCap.setAlpha(resourceData.getAlpha());

                    image.getImage()
                            .setBrightness(resourceData.getBrightness());
                    image.getImage().setContrast(resourceData.getContrast());
                    image.getImage()
                            .setInterpolated(imgCap.isInterpolationState());

                    paintProps.setAlpha(resourceData.getAlpha());
                    target.drawRasters(paintProps, image);
                }

                if (image == null || image.getCoverage() == null
                        || image.getCoverage().getMesh() == null) {
                    issueRefresh();
                }
            } catch (Exception e) {
                String msg = e.getMessage();
                if (msg == null) {
                    msg = "Error rendering radar";
                }
                throw new VizException(msg, e);
            }
        }

        refreshImage = false;

        // Draw circle
        if (resourceData.rangeRings) {
            IWireframeShape rangeCircle = this.rangeCircle.get(actualLevel);

            Float elev = 0.0f;
            if (radarRecord.getPrimaryElevationAngle() != null) {
                elev = radarRecord.getPrimaryElevationAngle().floatValue();
            }
            // create range circle
            rangeCircle = this.rangeCircle.get(elev);
            if (rangeCircle == null) {
                // Attempt to create envelope, adapted from AbstractTileSet
                double maxExtent = RadarUtil.calculateExtent(radarRecord);
                rangeCircle = computeRangeCircle(target, radarRecord.getCRS(),
                        maxExtent);
                if (rangeCircle != null) {
                    this.rangeCircle.put(elev, rangeCircle);
                }
            }

            if (rangeCircle != null
                    && getCapability(OutlineCapability.class).isOutlineOn()) {
                target.drawWireframeShape(rangeCircle, new RGB(0, 0, 0),
                        getCapability(OutlineCapability.class)
                                .getOutlineWidth(),
                        getCapability(OutlineCapability.class).getLineStyle(),
                        paintProps.getAlpha());
            }
        }
        RangeRingsOverlayCapability rrcap = getCapability(
                RangeRingsOverlayCapability.class);
        rrcap.setRangeableResource(this);
        rrcap.paint(target, paintProps);
    }

    /**
     * Shared by image and non-image
     *
     * @param target
     * @param crs
     * @param range
     * @return
     */
    public IWireframeShape computeRangeCircle(IGraphicsTarget target,
            CoordinateReferenceSystem crs, double range) {
        IWireframeShape rangeCircle = target.createWireframeShape(true,
                descriptor);

        try {
            MathTransform mt = CRS.findMathTransform(crs,
                    MapUtil.getLatLonProjection());

            double[][] pts = new double[RANGE_CIRCLE_PTS + 1][2];
            double azDelta = 2 * Math.PI / RANGE_CIRCLE_PTS;
            double az = 0.0;
            double[] input = new double[2];
            double[] output = new double[2];
            for (int i = 0; i < pts.length; i++) {
                input[0] = range * Math.cos(az);
                input[1] = range * Math.sin(az);
                mt.transform(input, 0, output, 0, 1);
                pts[i] = descriptor.worldToPixel(output);
                az += azDelta;
            }
            pts[RANGE_CIRCLE_PTS] = pts[0];

            rangeCircle.allocate(pts.length);
            rangeCircle.addLineSegment(pts);
        } catch (TransformException | FactoryException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Unable to compute the range circle", e);
            return null;
        }
        return rangeCircle;
    }

    protected byte[] createConversionTable(ColorMapParameters params,
            RadarRecord record) {
        // Sometimes the data unit may not match what is in the params so always
        // use what we really have
        UnitConverter dataToImage = null;
        Unit<?> dataUnit = DataUtilities.getDataUnit(record);
        if (dataUnit != null && !dataUnit.equals(params.getDataUnit())) {
            Unit<?> imageUnit = params.getImageUnit();
            if (imageUnit != null) {
                if (dataUnit.isCompatible(imageUnit)) {
                    dataToImage = UnitConv.getConverterToUnchecked(dataUnit,
                            imageUnit);
                } else {
                    dataUnit = DataUtilities.getDataUnit(record,
                            resourceData.mode);
                    if (dataUnit.isCompatible(imageUnit)) {
                        dataToImage = UnitConv.getConverterToUnchecked(dataUnit,
                                imageUnit);
                    }
                }
            }
        } else {
            dataToImage = params.getDataToImageConverter();
        }
        if (dataToImage == null && record.getNumLevels() <= 16) {
            dataToImage = new MultiplyConverter(16);
        } else if (dataToImage == null) {
            dataToImage = AbstractConverter.IDENTITY;
        }
        // precompute the converted value for every possible value in the
        // record.
        byte[] table = new byte[record.getNumLevels()];
        for (int i = 0; i < record.getNumLevels(); i++) {
            double image = dataToImage.convert(i);
            if (Double.isNaN(image)) {
                double d = Double.NaN;
                try {
                    if (dataUnit != null) {
                        d = dataUnit.getConverterToAny(params.getDisplayUnit())
                                .convert(i);
                    }
                } catch (IncommensurableException | UnconvertibleException e) {
                    SimpleUnitFormat fm = SimpleUnitFormat
                            .getInstance(SimpleUnitFormat.Flavor.ASCII);
                    statusHandler.handle(Priority.INFO,
                            "Unable to convert unit " + fm.format(dataUnit)
                                    + " to unit "
                                    + fm.format(params.getDisplayUnit()),
                            e);
                }
                if (Double.isNaN(d)) {
                    // This means that the data is a non-numeric value, most
                    // likely a flag of some sort
                    if (record.getNumLevels() <= 16) {
                        // For 3 and 4 bit products products try to match the
                        // flag value to a string value in the params
                        String value = record.getDecodedThreshold(i).toString();
                        for (DataMappingEntry entry : params.getDataMapping()
                                .getEntries()) {
                            if (value.equals(entry.getLabel())
                                    || value.equals(entry.getSample())) {
                                table[i] = entry.getPixelValue().byteValue();
                                break;
                            }
                        }
                    } else {
                        // For 8 bit products the flag value should be
                        // specifically handled in the style rules. For example
                        // if 1 is a flag for RF than pixel value 1 in the style
                        // rules will need to be RF. This is not
                        // a graceful separation of data and representation but
                        // it works
                        table[i] = (byte) i;
                    }
                } else {
                    // the data value is outside the range of the colormap
                    UnitConverter image2disp = params
                            .getImageToDisplayConverter();
                    if (image2disp == null) {
                        continue;
                    }
                    for (int j = 0; j < 256; j++) {
                        double disp = image2disp.convert(j);
                        if (Double.isNaN(disp)) {
                            continue;
                        }
                        if (d < disp) {
                            // Map data values smaller than the colormap min to
                            // 0, which should be no data.
                            // table[i] = (byte) 0;
                            // If we want small values to appear as the lowest
                            // data value than do this next line instead
                            // This was changed for the DUA product so
                            // differences less than -5 get mapped to a data
                            // value.
                            table[i] = (byte) j;
                            break;
                        }
                        if (d > disp) {
                            // map data values larger than the colormap max to
                            // the highest value
                            table[i] = (byte) j;
                        }

                    }
                }
            } else {
                table[i] = (byte) Math.round(image);
            }
        }
        return table;
    }

    protected IImage createImage(IGraphicsTarget target,
            ColorMapParameters params, RadarRecord record, Rectangle rect)
            throws VizException {
        byte[] table = createConversionTable(params, record);
        return target.getExtension(IColormappedImageExtension.class)
                .initializeRaster(
                        new RadarImageDataRetrievalAdapter(record, table, rect),
                        params);
    }

    public IMesh buildMesh(IGraphicsTarget target, VizRadarRecord radarRecord)
            throws VizException {
        return null;
    }

    public PixelCoverage buildCoverage(IGraphicsTarget target,
            VizRadarRecord timeRecord) throws VizException {
        return new PixelCoverage(new Coordinate(0, 0), 0, 0);
    }

    @Override
    public void meshCalculated(ImageTile tile) {
        issueRefresh();
    }

    @Override
    public void project(CoordinateReferenceSystem crs) throws VizException {
        for (IWireframeShape ring : rangeCircle.values()) {
            ring.dispose();
        }
        rangeCircle.clear();
    }

    public void redoImage(DataTime time) {
        disposeImage(images.remove(time));
    }

    protected void disposeImage(DrawableImage image) {
        if (image != null) {
            image.dispose();
        }
    }

    // the colorBar and/or the colormap may have changed so update the
    // colorBarPainter and the colorMapParametersCapability which holds
    // the instance of the colorMap that Raytheon's code needs
    @Override
    public void resourceAttrsModified() {

        // update the colorbarPainter with a possibly new colorbar
        ColorBarFromColormap colorBar = resourceData.getColorBar();

        cbarResource.setColorBar(colorBar);

        ColorMapParameters cmapParams = getCapability(ColorMapCapability.class)
                .getColorMapParameters();
        cmapParams.setColorMap(colorBar.getColorMap());
        cmapParams.setColorMapName(resourceData.getColorMapName());

        getCapability(ColorMapCapability.class)
                .setColorMapParameters(cmapParams);
        refreshImage = true;

        // TODO : how to migrate this to to11dr11? Or do we still need to do
        // this?
        // baseTile.resourceChanged(ChangeType.CAPABILITY, this.getCapability(
        // ColorMapCapability.class));
    }

    @Override
    public void propertiesChanged(ResourceProperties updatedProps) {

        if (cbarRscPair != null) {
            cbarRscPair.getProperties().setVisible(updatedProps.isVisible());
        }
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
                ColorMapParameters newColorMapParms = newColorMapCap
                        .getColorMapParameters();

                if (newColorMapParms != null) {
                    colorMapCap.setColorMapParameters(newColorMapParms, false);
                    ColorMap theColorMap = (ColorMap) colorMapCap
                            .getColorMapParameters().getColorMap();
                    String colorMapName = colorMapCap.getColorMapParameters()
                            .getColorMapName();
                    resourceData.setColorMapName(colorMapName);
                    resourceData.getRscAttrSet().setAttrValue("colorMapName",
                            colorMapName);
                    ColorBarFromColormap cBar = resourceData.getColorBar();
                    cBar.setColorMap(theColorMap);
                    resourceData.getRscAttrSet().setAttrValue("colorBar", cBar);
                    resourceData.setIsEdited(true);

                    refreshImage = true;
                    issueRefresh();
                }
            }
        }
    }

    protected static class RadarImageDataRetrievalAdapter
            implements IColorMapDataRetrievalCallback {

        protected final RadarRecord record;

        protected final byte[] table;

        protected Rectangle rect;

        private final int hashCode;

        public RadarImageDataRetrievalAdapter(RadarRecord record, byte[] table,
                Rectangle rect) {
            this.record = record;
            this.table = table;
            this.rect = rect;

            final int prime = 31;
            int hashCode = 1;
            hashCode = prime * hashCode
                    + (record == null ? 0 : record.hashCode());
            hashCode = prime * hashCode + Arrays.hashCode(table);
            hashCode = prime * hashCode + rect.hashCode();
            this.hashCode = hashCode;
        }

        @Override
        public ColorMapData getColorMapData() {
            return new ColorMapData(ByteBuffer.wrap(convertData()),
                    new int[] { rect.width, rect.height });
        }

        public byte[] convertData() {
            return record.getRawData();
        }

        protected boolean createCircle(byte[] imageData, int h, int w, int i) {
            // do nothing
            return false;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            RadarImageDataRetrievalAdapter other = (RadarImageDataRetrievalAdapter) obj;
            if (record == null) {
                if (other.record != null) {
                    return false;
                }
            } else if (!record.equals(other.record)) {
                return false;
            }
            if (!Arrays.equals(table, other.table)) {
                return false;
            }
            return true;
        }

    }
}
