package gov.noaa.nws.ncep.viz.rsc.satellite.rsc;

import gov.noaa.nws.ncep.viz.resources.AbstractFrameData;
import gov.noaa.nws.ncep.viz.resources.AbstractSatelliteRecordData;
import gov.noaa.nws.ncep.viz.resources.DfltRecordRscDataObj;
import gov.noaa.nws.ncep.viz.resources.IRscDataObject;
import gov.noaa.nws.ncep.viz.resources.util.Sampler;
import gov.noaa.nws.ncep.viz.ui.display.ColorBarFromColormap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.measure.Measure;
import javax.measure.converter.UnitConverter;
import javax.measure.unit.Unit;

import org.geotools.coverage.grid.GeneralGridGeometry;

import com.raytheon.uf.common.colormap.ColorMap;
import com.raytheon.uf.common.colormap.prefs.ColorMapParameters;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.persist.IPersistable;
import com.raytheon.uf.common.geospatial.IGridGeometryProvider;
import com.raytheon.uf.common.geospatial.ReferencedCoordinate;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.style.LabelingPreferences;
import com.raytheon.uf.common.style.StyleException;
import com.raytheon.uf.common.style.image.DataScale;
import com.raytheon.uf.common.style.image.ImagePreferences;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.DrawableImage;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.RasterMode;
import com.raytheon.uf.viz.core.IMesh;
import com.raytheon.uf.viz.core.PixelCoverage;
import com.raytheon.uf.viz.core.drawables.IImage;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.drawables.ext.colormap.IColormappedImageExtension;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.IMapMeshExtension;
import com.raytheon.uf.viz.core.rsc.IInputHandler;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.capabilities.ColorMapCapability;
import com.raytheon.uf.viz.core.sampling.ISamplingResource;
import com.raytheon.uf.viz.core.tile.Tile;
import com.raytheon.uf.viz.core.tile.TileSetRenderable.TileImageCreator;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

/**
 * Abstract class for display of polar-orbiting (MODIS/VIIRS) satellite data.
 * 
 * <pre>
 * 
 *  SOFTWARE HISTORY
 * 
 *  Date         Ticket#    Engineer     Description
 *  ------------ --------   -----------  --------------------------
 *  06/01/2016   R18511     kbugenhagen  Initial creation.
 * 
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1
 * @param <R>
 *            data record for resource
 */

public abstract class AbstractPolarOrbitSatResource<R extends IPersistable>
        extends NcSatelliteResource implements ISamplingResource {

    static final String SAMPLING_METHOD_NAME = "findBestValueForCoordinate";

    // sampled coordinate
    protected ReferencedCoordinate sampleCoord;

    protected boolean sampling = false;

    // virtual cursor location
    protected Coordinate virtualCursor;

    // resource name
    protected String name;

    public AbstractPolarOrbitSatResource(SatelliteResourceData data,
            LoadProperties props) {
        super(data, props);
    }

    /**
     * Provides methods to create an image for a tile
     */
    protected abstract class SatTileImageCreator implements TileImageCreator {

        protected R record;

        protected AbstractPolarOrbitSatDataRetriever<R> dataRetriever;

        protected abstract AbstractPolarOrbitSatDataRetriever<R> getDataRetriever(
                Tile tile);

        protected SatTileImageCreator(R record) {
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

            dataRetriever = (AbstractPolarOrbitSatDataRetriever<R>) getDataRetriever(tile);
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
     * Every data record will have a corresponding RecordData object
     */
    public class RecordData extends AbstractSatelliteRecordData<R> {

        protected final static double INTERSECTION_FACTOR = 10.0;

        protected boolean createdGeoTiff = false;

        public RecordData(R dataRecord) {
        }

        @Override
        protected double interrogate(Coordinate latLon) throws VizException {
            return super.interrogate(latLon);
        }

        @Override
        protected double getIntersectionFactor() {
            return INTERSECTION_FACTOR;
        }

    }

    protected abstract class FrameData extends NcSatelliteResource.FrameData {

        RecordData recordData;

        protected String legendStr = "No Data";

        // Map of frametimes to RecordData records, which contain images.
        // Each record that matches a frame's time is used to create a
        // RecordData object. RecordData objects are responsible for creating
        // the images (tilesets) that get grouped together for a frame.
        protected Map<DataTime, List<RecordData>> recordDataMap;

        protected FrameData(DataTime time, int interval) {
            super(time, interval);
            recordDataMap = new HashMap<>();
            recordDataMap.put(frameTime, new ArrayList<RecordData>());
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * gov.noaa.nws.ncep.viz.rsc.satellite.rsc.NcSatelliteResource.FrameData
         * #
         * updateFrameData(gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource
         * .IRscDataObject)
         */
        @Override
        public boolean updateFrameData(IRscDataObject rscDataObj) {
            IPersistable pdo = (IPersistable) ((DfltRecordRscDataObj) rscDataObj)
                    .getPDO();
            R record = (R) pdo;

            synchronized (this) {
                try {
                    boolean match = timeMatch(((PluginDataObject) record)
                            .getDataTime()) > -1;
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

        public RecordData getRecordData() {
            return recordData;
        }

        public Map<DataTime, List<RecordData>> getRecordDataMap() {
            return recordDataMap;
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

        public abstract void setLegendForFrame(R rec);

        @Override
        public void dispose() {
            recordData.dispose();
            recordDataMap.clear();
        }

    }

    /**
     * Add a data record to be displayed.
     * 
     * @param dataRecord
     *            data record for resource
     * @throws VizException
     */
    public abstract RecordData addRecord(R dataRecord) throws VizException;

    /**
     * Create color map from a record.
     * 
     * @param dataRecord
     *            data record data record for resource
     * 
     * @throws VizException
     */
    protected void createColorMap(R dataRecord) throws VizException {
        imagePreferences = getStyleRulePreferences(dataRecord);
        ColorMapParameters colorMapParameters = loadColorMapParameters(dataRecord);
        setColorBar(imagePreferences, colorMapParameters);
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
     * Set color bar from preferences.
     * 
     * @param preferences
     *            image preferences
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

    /**
     * Set color map unit, min, and max.
     * 
     * @param dataRecord
     *            data record
     * @param preferences
     *            stylerule preferences
     * @param colorMapParameters
     * @throws VizException
     */
    @Override
    protected void setColorMapUnits(IPersistable record,
            ColorMapParameters colorMapParameters) throws StyleException {

        colorMapParameters.setColorMapUnit(Unit.ONE);

        Unit<?> displayUnit = Unit.ONE;
        if (imagePreferences != null) {
            if (imagePreferences.getDisplayUnits() != null) {
                displayUnit = imagePreferences.getDisplayUnits();
            }
        }
        colorMapParameters.setDisplayUnit(displayUnit);
        colorMapParameters.setColorMapMin(10.0f);
        colorMapParameters.setColorMapMax(500.0f);
        if (imagePreferences != null) {
            DataScale scale = imagePreferences.getDataScale();
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

    /*
     * (non-Javadoc)
     * 
     * @see
     * gov.noaa.nws.ncep.viz.rsc.satellite.rsc.NcSatelliteResource#paintFrame
     * (gov
     * .noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource.AbstractFrameData,
     * com.raytheon.uf.viz.core.IGraphicsTarget,
     * com.raytheon.uf.viz.core.drawables.PaintProperties)
     */
    @SuppressWarnings("unchecked")
    @Override
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
                    createGeoTiff(recordData);
                    recordData.createdGeoTiff = true;
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

    /**
     * Get all the images for a frame
     * 
     * @param frameData
     *            data for frame
     * @param target
     *            graphics target
     * @param paintProps
     *            painting properties
     * @return list of images
     * @throws VizException
     */
    public List<Collection<DrawableImage>> getAllImages(FrameData frameData,
            IGraphicsTarget target, PaintProperties paintProps)
            throws VizException {
        List<RecordData> recordDataList = frameData.getRecordDataMap().get(
                frameData.getFrameTime());
        List<Collection<DrawableImage>> images = new ArrayList<>();

        if (recordDataList != null) {
            for (AbstractSatelliteRecordData<R> recordData : recordDataList) {
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

    /**
     * Generate geoTiff file from image. Doesn't actually paint; need to import
     * the geotiff to paint.
     * 
     * @param recordData
     *            data for interrogation and projection of satellite resource
     *            record
     */
    protected abstract void createGeoTiff(RecordData recordData);

    public String getLegendStr() {
        FrameData curFrame = (FrameData) getCurrentFrame();
        return (curFrame != null ? curFrame.getLegendForFrame()
                : "No Matching Data");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.viz.core.sampling.ISamplingResource#isSampling()
     */
    @Override
    public boolean isSampling() {
        return sampling;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.core.sampling.ISamplingResource#setSampling(boolean)
     */
    @Override
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

        Map<String, Object> interMap = new HashMap<>();
        ColorMapParameters colorMapParameters = getCapability(
                ColorMapCapability.class).getColorMapParameters();
        double noDataValue = colorMapParameters.getNoDataValue();
        double bestValue = Double.NaN;
        R bestRecord = null;
        List<AbstractPolarOrbitSatResource<R>.RecordData> recordDataList = getRecordDataList((FrameData) getCurrentFrame());

        if (recordDataList != null) {
            for (AbstractPolarOrbitSatResource<R>.RecordData data : recordDataList) {
                if (data != null && data.contains(p)) {
                    double value;
                    value = data.interrogate(latLon);
                    if (Double.isNaN(value) == false && value != noDataValue) {
                        bestValue = value;
                        bestRecord = (R) data.getRecord();
                    }
                }
            }
        }
        double dataValue = Double.NaN;
        if (Double.isNaN(bestValue) == false) {
            dataValue = colorMapParameters.getDataToDisplayConverter().convert(
                    bestValue);
            dataValue = convertDataValue(dataValue);
        }
        if (bestRecord != null) {
            interMap.put(IGridGeometryProvider.class.toString(), bestRecord);
        }
        interMap.put(AbstractSatelliteRecordData.SATELLITE_DATA_INTERROGATE_ID,
                Measure.valueOf(dataValue, colorMapParameters.getDisplayUnit()));

        return interMap;
    }

    /**
     * Returns a converted sampled value - default behavior is no conversion.
     * 
     * @param dataValue
     *            value to be converted
     * @return converted value
     */
    protected double convertDataValue(double dataValue) {
        return dataValue;
    }

    @Override
    public IInputHandler getInputHandler() {
        return new PolarOrbitSatInputAdapter<>(this);
    }

    /**
     * Get a list of RecordData for the resource for a frame.
     * 
     * @param frameData
     *            data for the frame
     * @return list of RecordData
     */
    protected List<AbstractPolarOrbitSatResource<R>.RecordData> getRecordDataList(
            FrameData frameData) {
        return frameData.getRecordDataMap().get(frameData.getFrameTime());
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
        ColorMapParameters params = getCapability(ColorMapCapability.class)
                .getColorMapParameters();

        return params.getDisplayUnit().toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * gov.noaa.nws.ncep.viz.rsc.satellite.rsc.NcSatelliteResource#getRecordUnit
     * (com.raytheon.uf.common.dataplugin.persist.IPersistable)
     */
    @Override
    public Unit<?> getRecordUnit(IPersistable dataRecord) {
        return Unit.ONE;
    }

}