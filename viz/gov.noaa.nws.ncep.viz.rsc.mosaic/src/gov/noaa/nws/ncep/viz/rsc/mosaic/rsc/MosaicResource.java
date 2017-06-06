package gov.noaa.nws.ncep.viz.rsc.mosaic.rsc;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;

import org.eclipse.swt.graphics.RGB;
import org.geotools.coverage.grid.GridGeometry2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.raytheon.uf.common.colormap.ColorMap;
import com.raytheon.uf.common.colormap.prefs.ColorMapParameters;
import com.raytheon.uf.common.colormap.prefs.DataMappingPreferences;
import com.raytheon.uf.common.colormap.prefs.DataMappingPreferences.DataMappingEntry;
import com.raytheon.uf.common.dataplugin.HDF5Util;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.annotations.DataURI;
import com.raytheon.uf.common.datastorage.DataStoreFactory;
import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.common.datastorage.Request;
import com.raytheon.uf.common.datastorage.StorageException;
import com.raytheon.uf.common.datastorage.records.ByteDataRecord;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.datastorage.records.ShortDataRecord;
import com.raytheon.uf.common.geospatial.ReferencedCoordinate;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.style.image.ImagePreferences;
import com.raytheon.uf.common.style.image.SamplePreferences;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.units.PiecewisePixel;
import com.raytheon.uf.viz.core.DrawableImage;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IMesh;
import com.raytheon.uf.viz.core.PixelCoverage;
import com.raytheon.uf.viz.core.data.IColorMapDataRetrievalCallback;
import com.raytheon.uf.viz.core.drawables.IColormappedImage;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.drawables.ext.colormap.IColormappedImageExtension;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.IMapMeshExtension;
import com.raytheon.uf.viz.core.rsc.IResourceDataChanged;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.ResourceProperties;
import com.raytheon.uf.viz.core.rsc.capabilities.ColorMapCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.ColorableCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.ImagingCapability;

import gov.noaa.nws.ncep.edex.plugin.mosaic.common.MosaicRecord;
import gov.noaa.nws.ncep.edex.plugin.mosaic.uengine.MosaicTiler;
import gov.noaa.nws.ncep.viz.common.ColorMapUtil;
import gov.noaa.nws.ncep.viz.common.ui.NmapCommon;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.colorBar.ColorBarResource;
import gov.noaa.nws.ncep.viz.resources.colorBar.ColorBarResourceData;
import gov.noaa.nws.ncep.viz.ui.display.ColorBarFromColormap;
import gov.noaa.nws.ncep.viz.ui.display.NCMapDescriptor;

/**
 * Provide Radar Mosaic raster rendering support
 * 
 * <pre>
 * 
 *  SOFTWARE HISTORY
 * 
 *  Date         Ticket#     Engineer     Description
 *  ------------ ----------  -----------  --------------------------
 *  01/2010	       204        M. Li       Initial Creation.
 *  03/2010                   B. Hebbard  Port TO11D6->TO11DR3; add localization
 *  04/2010        259        Greg Hull   Added Colorbar
 *  09/2010        307        Greg Hull   move getName to resourceData and base on 
 *                                        the productCode in the metadataMap 
 *  07/11/11                  Greg Hull   ColorBarResource 
 *  06-07-2012     717         Archana	  Updated setColorMapParameters() to store label information
 *                                        for the colorbar                                     
 *  06/21/2012     #825       Greg Hull   rm mosaicInfo.txt; get legend info from the Record.
 *  07/18/12       717        Archana     Refactored a field used to align the label data
 * 12/19/2012     #960        Greg Hull   override propertiesChanged() to update colorBar.
 * 06/15/2016     R19647      bsteffen    Improve performance
 * 10/20/2016     R20700      P. Moyer    Added image brightness adjustment to paintFrame
 * 05/09/2017     R27171      P. Moyer    Modified initResource to take parent resource's
 *                                        visibility and apply it to the newly created color bar
 * 
 * </pre>
 * 
 * @author mli
 * @version 1
 */

public class MosaicResource
        extends AbstractNatlCntrsResource<MosaicResourceData, NCMapDescriptor>
        implements IResourceDataChanged {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(MosaicResource.class, "Mosaic");

    /** The line color */
    private static final RGB DEFAULT_COLOR = new RGB(255, 255, 255);

    protected ColorBarResource cbarResource;

    protected class FrameData extends AbstractFrameData {

        DrawableImage image;

        MosaicRecord mosaicRecord;

        protected FrameData(DataTime time, int interval) {
            super(time, interval);
        }

        public boolean updateFrameData(IRscDataObject rscDataObj) {
            PluginDataObject pdo = ((DfltRecordRscDataObj) rscDataObj).getPDO();
            MosaicRecord mosaicRecord = (MosaicRecord) pdo;
            if (this.mosaicRecord != null) {
                if (timeMatch(mosaicRecord.getDataTime()) >= timeMatch(
                        this.mosaicRecord.getDataTime())) {
                    return false;
                } else if (image != null) {
                    /*
                     * if this is a better match, we need to create a new image.
                     */
                    image.dispose();
                    image = null;
                }
            }
            ColorMapParameters colorMapParameters = getCapability(
                    ColorMapCapability.class).getColorMapParameters();
            if (colorMapParameters == null
                    || colorMapParameters.getColorMap() == null) {
                try {
                    setColorMapParameters(mosaicRecord);
                } catch (FileNotFoundException | StorageException
                        | VizException e) {
                    statusHandler.error("Error initializing color map.", e);
                }
            }

            this.mosaicRecord = mosaicRecord;
            return true;
        }

        public DataTime getRecordTime() {
            if (mosaicRecord == null) {
                return null;
            } else {
                return mosaicRecord.getDataTime();
            }
        }

        public String getLegendString() {
            if (mosaicRecord == null) {
                return "";
            } else {
                String prodName = mosaicRecord.getProdName();
                Integer numLevels = mosaicRecord.getNumLevels();
                String unitName = mosaicRecord.getUnit();
                return createLegend(prodName, numLevels, unitName);
            }
        }

        public DrawableImage getImage(IGraphicsTarget target)
                throws VizException {
            if (image == null) {
                IColormappedImageExtension cmapExtension = target
                        .getExtension(IColormappedImageExtension.class);
                IColorMapDataRetrievalCallback callback = new MosaicColorMapDataRetrievalCallback(
                        mosaicRecord);
                ColorMapParameters colorMapParameters = getCapability(
                        ColorMapCapability.class).getColorMapParameters();
                IColormappedImage cmapImage = cmapExtension
                        .initializeRaster(callback, colorMapParameters);

                IMapMeshExtension meshExtension = target
                        .getExtension(IMapMeshExtension.class);
                GridGeometry2D imageGeometry = new MosaicTiler(mosaicRecord)
                        .constructGridGeometry();
                IMesh mesh = meshExtension.constructMesh(imageGeometry,
                        descriptor.getGridGeometry());
                PixelCoverage coverage = new PixelCoverage(mesh);
                image = new DrawableImage(cmapImage, coverage);
            }
            return image;
        }

        public void reproject() throws VizException {
            if (image != null) {
                PixelCoverage coverage = image.getCoverage();
                IMesh oldMesh = coverage.getMesh();
                IMesh newMesh = oldMesh.clone(descriptor.getGridGeometry());
                coverage.setMesh(newMesh);
                oldMesh.dispose();
            }
        }

        @Override
        public void dispose() {
            if (image != null) {
                image.dispose();
                image = null;
            }
        }
    }

    public MosaicResource(MosaicResourceData rrd, LoadProperties loadProps) {
        super(rrd, loadProps);
        rrd.addChangeListener(this);

        this.dataTimes = new ArrayList<>();

        ColorableCapability colorCap = getCapability(ColorableCapability.class);
        if (colorCap.getColor() == null) {
            colorCap.setColor(DEFAULT_COLOR);
        }
    }

    @Override
    protected boolean postProcessFrameUpdate() {
        /*
         * This method will perform a bulk hdf5 request for all the data for all
         * frames.
         */
        Map<String, MosaicRecord> mosaicRecordMap = new HashMap<>();
        Map<File, Set<String>> datasetGroupPaths = new HashMap<>();

        for (AbstractFrameData afd : frameDataMap.values()) {
            FrameData frame = (FrameData) afd;
            MosaicRecord mosaicRecord = frame.mosaicRecord;
            if (mosaicRecord.getRawData() == null) {
                String dataURI = mosaicRecord.getDataURI();
                mosaicRecordMap.put(dataURI, mosaicRecord);
                File loc = HDF5Util.findHDF5Location(mosaicRecord);
                Set<String> paths = datasetGroupPaths.get(loc);
                if (paths == null) {
                    paths = new HashSet<>();
                    datasetGroupPaths.put(loc, paths);
                }
                paths.add(dataURI + DataURI.SEPARATOR
                        + MosaicRecord.DATA_DATASET_NAME);
                paths.add(dataURI + DataURI.SEPARATOR
                        + MosaicRecord.THRESHOLD_DATASET_NAME);
            }
        }
        for (Entry<File, Set<String>> entry : datasetGroupPaths.entrySet()) {
            IDataStore dataStore = DataStoreFactory
                    .getDataStore(entry.getKey());
            try {
                IDataRecord[] dataRecords = dataStore.retrieveDatasets(
                        entry.getValue().toArray(new String[0]), Request.ALL);
                for (IDataRecord dataRecord : dataRecords) {
                    String dataURI = dataRecord.getGroup();
                    MosaicRecord mosaicRecord = mosaicRecordMap.get(dataURI);
                    if (dataRecord.getName()
                            .equals(MosaicRecord.DATA_DATASET_NAME)) {
                        ByteDataRecord byteData = (ByteDataRecord) dataRecord;
                        mosaicRecord.setRawData(byteData.getByteData());
                    } else if (dataRecord.getName()
                            .equals(MosaicRecord.THRESHOLD_DATASET_NAME)) {
                        ShortDataRecord shortData = (ShortDataRecord) dataRecord;
                        mosaicRecord.setThresholds(shortData.getShortData());
                    }
                }
            } catch (StorageException | FileNotFoundException e) {
                /*
                 * This is only a debug because the individual records will be
                 * requested when they are needed and the specific mosaic record
                 * that is having problems will be able to log a more
                 * descriptive error at that time.
                 */
                statusHandler.handle(Priority.DEBUG,
                        "Bulk mosaic retrieval has failed.", e);
            }

        }
        return true;
    }

    @Override
    protected AbstractFrameData createNewFrame(DataTime frameTime,
            int frameInterval) {
        return new FrameData(frameTime, frameInterval);
    }

    private static String createLegend(String prodName, int numLevels,
            String unitName) {
        if (unitName == null) {
            unitName = " ";
        } else {
            if (!unitName.contains("(")) {
                unitName = " (" + unitName + ") ";
            }
            if (unitName.contains("/10")) {
                unitName = unitName.replace("/10", "");
            }
            if (unitName.contains("*1000")) {
                unitName = unitName.replace("*1000", "");
            }
        }

        int bitDepth = (int) (Math.log(numLevels) / Math.log(2));
        return prodName + unitName + bitDepth + "-bit ";
    }

    @Override
    public String getName() {

        FrameData fd = (FrameData) getCurrentFrame();

        if (fd == null) {
            return "Natl Mosaic-No Data";
        } else if (fd.getRecordTime() == null
                || descriptor.getFramesInfo().getFrameCount() == 0) {

            return fd.getLegendString() + "-No Data";
        } else {
            return fd.getLegendString() + NmapCommon
                    .getTimeStringFromDataTime(fd.getRecordTime(), "/");
        }
    }

    @Override
    public void disposeInternal() {
        super.disposeInternal();

        descriptor.getResourceList().removeRsc(cbarResource);
    }

    @Override
    public void initResource(IGraphicsTarget target) throws VizException {
        synchronized (this) {
            /*
             * create the colorBar Resource and add it to the resourceList for
             * this descriptor.
             */
            ResourcePair cbarRscPair = ResourcePair.constructSystemResourcePair(
                    new ColorBarResourceData(resourceData.getColorBar()));

            descriptor.getResourceList().add(cbarRscPair);
            descriptor.getResourceList().instantiateResources(descriptor, true);

            cbarResource = (ColorBarResource) cbarRscPair.getResource();

            // set the color bar's visiblity to match that of the parent
            // resource
            // by changing the Resource Parameter's isVisible value.

            boolean parentVisibility = getProperties().isVisible();
            cbarRscPair.getProperties().setVisible(parentVisibility);

            getCapability(ImagingCapability.class)
                    .setSuppressingMenuItems(true);
            getCapability(ColorMapCapability.class)
                    .setSuppressingMenuItems(true);
            getCapability(ColorableCapability.class)
                    .setSuppressingMenuItems(true);
            queryRecords();

        }
    }

    @Override
    public void paintFrame(AbstractFrameData frmData, IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {
        if (paintProps == null || paintProps.getDataTime() == null) {
            return;
        }

        FrameData currFrame = (FrameData) frmData;
        DrawableImage image = currFrame.image;

        ColorMapParameters colorMapParameters = getCapability(
                ColorMapCapability.class).getColorMapParameters();

        try {
            if (image == null) {
                IColormappedImage cmapImage = target
                        .getExtension(IColormappedImageExtension.class)
                        .initializeRaster(
                                new MosaicColorMapDataRetrievalCallback(
                                        currFrame.mosaicRecord),
                                colorMapParameters);

                IMesh mesh = target.getExtension(IMapMeshExtension.class)
                        .constructMesh(
                                new MosaicTiler(currFrame.mosaicRecord)
                                        .constructGridGeometry(),
                                descriptor.getGridGeometry());
                PixelCoverage coverage = new PixelCoverage(mesh);
                image = new DrawableImage(cmapImage, coverage);

                currFrame.image = image;
            }
            ImagingCapability imgCap = new ImagingCapability();
            imgCap.setBrightness(resourceData.getBrightness());
            imgCap.setContrast(resourceData.getContrast());
            imgCap.setAlpha(resourceData.getAlpha());

            image.getImage().setBrightness(resourceData.getBrightness());
            image.getImage().setContrast(resourceData.getContrast());
            image.getImage().setInterpolated(imgCap.isInterpolationState());

            paintProps.setAlpha(resourceData.getAlpha());
            resourceData.fireChangeListeners(ChangeType.CAPABILITY, imgCap);

            // TODO: Suggest making the following more resilient to errors
            // -- handle case where
            // "params" is null, which does occur if colormap file was not
            // found.
            if (colorMapParameters.getColorMap() == null) {
                throw new VizException("ColorMap not specified");
            }

            target.drawRasters(paintProps, image);

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg == null) {
                msg = "Error rendering radar mosaic";
            }
            throw new VizException(msg, e);
        }

    }

    private void setColorMapParameters(MosaicRecord radarRecord)
            throws FileNotFoundException, StorageException, VizException {

        Unit<?> dataUnit = null;
        if (radarRecord.getUnit() != null) {
            try {
                dataUnit = UnitFormat.getUCUMInstance().parseProductUnit(
                        radarRecord.getUnit(), new ParsePosition(0));
            } catch (ParseException e) {
                throw new VizException("Unable to parse units ", e);
            }
        } else {
            dataUnit = Unit.ONE;
        }
        Object[] thresholds = radarRecord.getDecodedThresholds();
        if (thresholds[0] == null) {
            File loc = HDF5Util.findHDF5Location(radarRecord);
            IDataStore dataStore = DataStoreFactory.getDataStore(loc);
            radarRecord.retrieveFromDataStore(dataStore);
            thresholds = radarRecord.getDecodedThresholds();
        }
        DataMappingPreferences dmPref = new DataMappingPreferences();
        DataMappingEntry dmEntry;
        List<DataMappingEntry> dmEntriesList = new ArrayList<>(0);
        if (radarRecord.isFourBit()) {
            int numLevels = radarRecord.getNumLevels();
            ArrayList<Integer> pixel = new ArrayList<>();
            ArrayList<Float> real = new ArrayList<>();
            for (int i = 0; i < numLevels; i++) {
                dmEntry = new DataMappingEntry();
                dmEntry.setPixelValue(new Double(i));
                if (thresholds[i] instanceof Float) {
                    pixel.add(i);
                    real.add((Float) thresholds[i]);
                    dmEntry.setDisplayValue(
                            ((Float) thresholds[i]).doubleValue());
                    dmEntry.setLabel(((Float) thresholds[i]).toString());
                } else {
                    dmEntry.setDisplayValue(Double.NaN);

                    if (((String) thresholds[i])
                            .compareToIgnoreCase("NO DATA") == 0) {
                        dmEntry.setLabel("ND");
                    } else
                        dmEntry.setLabel((String) thresholds[i]);
                }

                dmEntriesList.add(dmEntry);

            }

            double[] pix = new double[pixel.size()];
            int i = 0;
            for (Integer p : pixel) {
                pix[i++] = p;
            }

            double[] std = new double[real.size()];
            i = 0;
            for (Float r : real) {
                std[i++] = r;
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
            throw new VizException("Error loading colormap: "
                    + resourceData.getColorMapName());
        }

        ColorMapParameters colorMapParameters = new ColorMapParameters();
        if (colorMapParameters.getDisplayUnit() == null) {
            colorMapParameters.setDisplayUnit(dataUnit);

        }

        colorMapParameters.setColorMap(colorMap);
        colorMapParameters.setColorMapMax(255);
        colorMapParameters.setColorMapMin(0);
        colorMapParameters.setDataMax(255);
        colorMapParameters.setDataMin(0);
        getCapability(ColorMapCapability.class)
                .setColorMapParameters(colorMapParameters);
        if (dmEntriesList.size() > 0) {
            DataMappingEntry[] dmEntryArray = new DataMappingEntry[dmEntriesList
                    .size()];
            dmEntriesList.toArray(dmEntryArray);
            dmPref.setSerializableEntries(dmEntryArray);
            ImagePreferences imgPref = new ImagePreferences();
            imgPref.setDataMapping(dmPref);
            SamplePreferences sampPref = new SamplePreferences();
            sampPref.setMinValue(0);
            sampPref.setMaxValue(255);
            imgPref.setSamplePrefs(sampPref);
            ColorBarFromColormap cBar = (ColorBarFromColormap) this.cbarResource
                    .getResourceData().getColorbar();
            if (cBar != null) {
                cBar.setImagePreferences(imgPref);

                if (!cBar.isScalingAttemptedForThisColorMap())
                    cBar.scalePixelValues();

                if (radarRecord.getUnit().compareToIgnoreCase("in") == 0) {
                    cBar.setDisplayUnitStr("INCHES");
                } else {
                    cBar.setDisplayUnitStr(radarRecord.getUnit());
                }

                cBar.setAlignLabelInTheMiddleOfInterval(true);
                cBar.setColorMap(colorMap);

                resourceData.setColorBar(cBar);
            }

        }

    }

    @Override
    public String inspect(ReferencedCoordinate latLon) throws VizException {
        return "NO DATA";
    }

    @Override
    public Map<String, Object> interrogate(ReferencedCoordinate coord)
            throws VizException {
        return new HashMap<>();
    }

    @Override
    public void project(CoordinateReferenceSystem mapData) throws VizException {
        for (AbstractFrameData afd : frameDataMap.values()) {
            FrameData frame = (FrameData) afd;
            frame.reproject();
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
                resourceData.getRscAttrSet().setAttrValue("colorBar", cBar);
                resourceData.setIsEdited(true);
                issueRefresh();

            }

        }

    }

    @Override
    public void propertiesChanged(ResourceProperties updatedProps) {

        if (cbarResource != null) {
            cbarResource.getProperties().setVisible(updatedProps.isVisible());
        }
    }

    // the colorBar and/or the colormap may have changed so update the
    // colorBarPainter and the colorMapParametersCapability which holds
    // the instance of the colorMap that Raytheon's code needs
    @Override
    public void resourceAttrsModified() {
        // update the colorbarPainter with a possibly new colorbar
        ColorBarFromColormap colorBar = resourceData.getColorBar();
        // ColorBarFromColormap colorBar = (ColorBarFromColormap)
        // radarRscData.getRscAttrSet().getRscAttr("colorBar").getAttrValue();
        cbarResource.setColorBar(colorBar);

        ColorMapParameters cmapParams = getCapability(ColorMapCapability.class)
                .getColorMapParameters();
        cmapParams.setColorMap(colorBar.getColorMap());
        cmapParams.setColorMapName(resourceData.getColorMapName());

        getCapability(ColorMapCapability.class)
                .setColorMapParameters(cmapParams);

        // TODO : how to migrate this to to11dr11? Or do we still need to do
        // this?
        // baseTile.resourceChanged(ChangeType.CAPABILITY, this.getCapability(
        // ColorMapCapability.class));
    }
}
