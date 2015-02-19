package gov.noaa.nws.ncep.viz.rsc.ncscat.rsc;

import gov.noaa.nws.ncep.common.dataplugin.ncscat.NcscatMode;
import gov.noaa.nws.ncep.common.dataplugin.ncscat.NcscatPoint;
import gov.noaa.nws.ncep.common.dataplugin.ncscat.NcscatRecord;
import gov.noaa.nws.ncep.common.dataplugin.ncscat.NcscatMode.LongitudeCoding;
import gov.noaa.nws.ncep.common.dataplugin.ncscat.NcscatMode.WindDirectionSense;
import gov.noaa.nws.ncep.gempak.parameters.colorbar.ColorBarOrientation;
import gov.noaa.nws.ncep.ui.pgen.display.DisplayElementFactory;
import gov.noaa.nws.ncep.ui.pgen.display.IDisplayable;
import gov.noaa.nws.ncep.ui.pgen.display.IVector;
import gov.noaa.nws.ncep.ui.pgen.display.IVector.VectorType;
import gov.noaa.nws.ncep.ui.pgen.elements.Vector;
import gov.noaa.nws.ncep.viz.common.ui.NmapCommon;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.colorBar.ColorBarResource;
import gov.noaa.nws.ncep.viz.resources.colorBar.ColorBarResourceData;
import gov.noaa.nws.ncep.viz.ui.display.ColorBar;
import gov.noaa.nws.ncep.viz.ui.display.NCMapDescriptor;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.measure.unit.NonSI;
import javax.measure.unit.Unit;

import org.eclipse.swt.graphics.RGB;

import com.raytheon.uf.common.dataplugin.HDF5Util;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.datastorage.DataStoreFactory;
import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.common.datastorage.Request;
import com.raytheon.uf.common.datastorage.StorageException;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.HorizontalAlignment;
import com.raytheon.uf.viz.core.IGraphicsTarget.TextStyle;
import com.raytheon.uf.viz.core.PixelExtent;
import com.raytheon.uf.viz.core.drawables.IFont;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.geom.PixelCoordinate;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.ResourceProperties;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * NcscatResource - Class for display of all types of satellite
 * scatterometer/radiometer data showing ocean surface winds.
 * 
 * This code has been developed by the SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 28 Oct 2009  176        B. Hebbard  Initial creation (as QuikSCAT).
 * 14 Feb 2010  235A       B. Hebbard  Convert from QuikSCAT (@D3) to NCSCAT (@D6)
 * 11 Jun 2010  235B       B. Hebbard  Expand for all Ocean Winds data types
 * 14 Jul 2010  235C       B. Hebbard  Use common NC ColorBar
 * 14 Jan 2011  235D       B. Hebbard  Add density select; performance enhancement via new PGEN aggregate vector display
 * 03 Feb 2011  235E       B. Hebbard  Add support for ambiguity variants
 * 16 Nov 2011             B. Hebbard  Fix excess rowCount increment in paintFrame()
 * 05/23/12       785      Q. Zhou     Added getName for legend.
 * 16 Aug 2012  843        B. Hebbard  Added OSCAT
 * 17 Aug 2012  655        B. Hebbard  Added paintProps as parameter to IDisplayable draw
 *  12/19/2012    #960     Greg Hull   override propertiesChanged() to update colorBar.
 * 30 May 2013             B. Hebbard  Merge changes by RTS in OB13.3.1 for DataStoreFactory.getDataStore(...)
 * 11 Apr 2014  1128       B. Hebbard  Prevent overflow if unsigned longitude >= 327.68W (32768)
 * 20 May 2014  TTR985     B. Hebbard  Reverse bit ordering in getBit(...); tweak QC circle scaling
 * 29 Jul 2014  R4279      B. Hebbard  (TTR 1046) Add call to processNewRscDataList() in initResource()
 *                                     (instead of waiting for ANCR.paintInternal() to do it) so
 *                                     long CGM retrieval and parsing is done by the InitJob, and thus
 *                                     (1) user sees "Initializing..." and (2) GUI doesn't lock up
 * 10 Oct 2014  R4864      B. Hebbard  (TTR 986) ColorBar plotting high to low when in horizontal;
 *                                     regression caused by reverseOrder enhancement for imagery
 *                                     resources (in vertical).  Fix:  Set reverseOrder for NCSCAT colorbars
 *                                     to false for horizontal and true for vertical.
 * 
 * </pre>
 * 
 * @author bhebbard
 * @version 1.0
 */
public class NcscatResource extends
        AbstractNatlCntrsResource<NcscatResourceData, NCMapDescriptor>
        implements INatlCntrsResource {

    // DEBUG boolean OVERRIDErainQcFlag = true;
    // DEBUG boolean OVERRIDEhighWindSpeedFlag = true;
    // DEBUG boolean OVERRIDElowWindSpeedFlag = true;
    // DEBUG boolean OVERRIDEavailRedunFlag = true;

    private NcscatResourceData ncscatResourceData;

    private Unit<?> windSpeedUnits = NonSI.KNOT;

    protected ColorBarResource cbar1Resource;

    protected ResourcePair cbar1RscPair;

    protected ColorBarResource cbar2Resource;

    protected ResourcePair cbar2RscPair;

    private class FrameData extends AbstractFrameData {
        ArrayList<NcscatRowData> frameRows = new ArrayList<NcscatRowData>();

        public FrameData(DataTime frameTime, int timeInt) {
            super(frameTime, timeInt);
            frameRows = new ArrayList<NcscatRowData>();
        }

        public boolean updateFrameData(IRscDataObject rscDataObj) {
            PluginDataObject pdo = ((DfltRecordRscDataObj) rscDataObj).getPDO();

            NcscatRecord nsRecord = (NcscatRecord) pdo;

            // Given a NcscatRecord (a PDO, previously retrieved from the DB),
            // get the HDF5 data associated with it, as raw bytes...
            byte[] hdf5Data = getRawData(nsRecord);
            if (hdf5Data == null) {
                return false;
            }

            else {
                // ...convert to point data for paint, grouped by rows...
                ArrayList<NcscatRowData> rowsDataFromPdo = processHDF5Data(hdf5Data);
                // ...and add to existing row data for this frame
                frameRows.addAll(rowsDataFromPdo);
                return true;
            }
        }

        private byte[] getRawData(NcscatRecord nsRecord) {

            // Given the NcscatRecord, locate the associated HDF5 data...
            File location = HDF5Util.findHDF5Location(nsRecord);

            // TODO... Investigate: Why is the following statement needed?
            // Starting in OB13.5.3, the PDO (nsRecord) has a non-null, but
            // bogus, value in its dataURI field at this point (and earlier,
            // as soon as it is deserialized after return from the metadata
            // query). nsRecord.getDataURI() below will get this bad value,
            // leading to failure on the ds.retrieve(...). Instead we force
            // it to synthesize the dataURI -- which getDataURI() does
            // correctly -- by setting the field to null first. But why is
            // this happening, and why only in OB13.5.3, and why only for
            // some resources...? (bh) (see also NTRANS resource)
            nsRecord.setDataURI(null); // force getDataURI() to construct one

            String group = nsRecord.getDataURI();
            String dataset = "Ncscat";

            // ...and retrieve it
            IDataStore ds = DataStoreFactory.getDataStore(location);
            IDataRecord dr;
            try {
                dr = ds.retrieve(group, dataset, Request.ALL);
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null;
            } catch (StorageException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null;
            }

            return (byte[]) dr.getDataObject();
        }

        private ArrayList<NcscatRowData> processHDF5Data(byte[] hdf5Msg) {

            // Note: This code lifted from NcscatProcessing.processHDFData,
            // modified to (1) return point data in structures already
            // optimized for paint() and (2) preserve intermediate
            // organization by rows. Some further optimization may
            // be desirable. (TODO)

            final int shortsPerPoint = 9;
            final int bytesPerPoint = 2 * shortsPerPoint;
            int ji = 0, byteNumber = 0;
            int day, hour, min, sec;
            ArrayList<NcscatRowData> returnList = new ArrayList<NcscatRowData>();
            ByteBuffer byteBuffer = null;
            byteBuffer = ByteBuffer.allocate(hdf5Msg.length);
            // TODO: investigate...?
            // byteBuffer.order(ncscatMode.getByteOrder());
            byteBuffer.put(hdf5Msg, 0, hdf5Msg.length);

            while (ji < hdf5Msg.length) {
                day = byteBuffer.getShort(byteNumber);
                hour = byteBuffer.getShort(byteNumber + 2);
                min = byteBuffer.getShort(byteNumber + 4);
                sec = byteBuffer.getShort(byteNumber + 6);
                ji += 8;
                byteNumber += 8;
                Calendar startTime = Calendar.getInstance();
                startTime.set(Calendar.DAY_OF_YEAR, day);
                startTime.set(Calendar.HOUR_OF_DAY, hour);
                startTime.set(Calendar.MINUTE, min);
                startTime.set(Calendar.SECOND, sec);
                // Prepare a row object (minimized for paint), which has a
                // common timestamp for all the points it will contain
                NcscatRowData rowData = new NcscatRowData(startTime);

                int scatNumber = ncscatMode.getPointsPerRow();
                for (int j = ji; j < ji + scatNumber * bytesPerPoint
                        && byteNumber < hdf5Msg.length; j += bytesPerPoint) {
                    NcscatPoint sPointObj = new NcscatPoint();
                    // sPointObj.setStTime(startTime);
                    sPointObj.setLat(byteBuffer.getShort(j));
                    sPointObj.setLon(byteBuffer.getShort(j + 2));
                    sPointObj.setIql(byteBuffer.getShort(j + 4));
                    sPointObj.setIsp(byteBuffer.getShort(j + 6));
                    sPointObj.setIdr(byteBuffer.getShort(j + 8));
                    // sPointObj.setIrn(byteBuffer.getShort(j+10));
                    // sPointObj.setIb1(byteBuffer.getShort(j+12));
                    // sPointObj.setIb2(byteBuffer.getShort(j+14));
                    // sPointObj.setIb3(byteBuffer.getShort(j+16));
                    byteNumber += bytesPerPoint;
                    // returnList.add(sPointObj);
                    // Above code put things into a NcscatPoint (common with
                    // decoder); now convert to a NcscatPointData (local class)
                    // optimized for display via paint(), and add to the row
                    rowData.points.add(new NcscatPointData(sPointObj));
                } // for

                ji = byteNumber;

                // Row complete; add it to the list we're building covering all
                // rows in this HDF5 message
                returnList.add(rowData);

            } // while

            return returnList;
        }

    }

    // Structure containing displayable information for a
    // displayable element -- that is, a single point observation.

    // @formatter:off

    private class NcscatPointData {
        Coordinate location;        // lat/lon of this point
        float direction;            // "from" direction in bulletin
        float speed;                // in knots
        boolean rainQcFlag;         // is point marked 'rain' or 'QC fail'?
        boolean highWindSpeedFlag;  // is point marked 'high wind speed'?
        boolean lowWindSpeedFlag;   // is point marked 'low wind speed'?
        boolean availRedunFlag;     // is point marked 'some data unavailable' or 'redundant'?

        // Constructor to convert an existing NcscatPoint (common to decoder)
        // into a NcscatPointData (local class here), in which things are
        // minimized to -- and optimized for -- what we need for display.
        // (For example, don't want to flip direction at every paint().)

        public NcscatPointData(NcscatPoint point) {
            int scaledLatitude = point.getLat();   // signed int from signed short
            int scaledLongitude = point.getLon();  // signed int from either signed short, OR...
            if (ncscatMode.getLongitudeCoding() == LongitudeCoding.UNSIGNED) {
                if (scaledLongitude < 0) {             // ...*unsigned* short
                    scaledLongitude += 65536;          // aka  1<<16
                }
                if (scaledLongitude >  18000) {  // unsigned longitude comes in as 0--360 east of Greenwich
                    scaledLongitude -= 36000;    // convert to -180 -- +180
                }
            }
            location = new Coordinate(scaledLongitude / 100.0,   // de-scale and float
                                      scaledLatitude  / 100.0);
            
            direction = point.getIdr() / 100.0f;
            // some (but not all) data sources require us to... 
            if (ncscatMode.getWindDirectionSense() == WindDirectionSense.METEOROLOGICAL) { 
                direction = direction > 180.0f ? // ...reverse direction sense
                            direction - 180.0f
                          : direction + 180.0f;
            }
            
            speed = point.getIsp() / 100.0f;
            speed *= (3600.0f / 1852.0f); // m/s --> kt //TODO: Use Unit conversions...?
            
            int qualityBits = point.getIql();

            switch (ncscatMode) {
            case QUIKSCAT:
            case QUIKSCAT_HI:
                // TODO:?? if ((point.getIql() & 0xFFC0) != 0x0000) {
                // for QuikSCAT only, if ANY of bits 0-9 on...
                // speed = -9999.9f; // ...then do not plot, regardless of user settings
                // could break; but let's set the flags below anyway...
                // }
                rainQcFlag =       !getBit(qualityBits, 12) &&  // bit 12 == 0 AND
                                    getBit(qualityBits, 13);    // bit 13 == 1   rain
                highWindSpeedFlag = getBit(qualityBits, 10);    // bit 10 == 1
                lowWindSpeedFlag =  getBit(qualityBits, 11);    // bit 11 == 1
                availRedunFlag =    getBit(qualityBits, 14);    // bit 14 == 1   availability
                break;
            case ASCAT:
            case ASCAT_HI:
            case EXASCT:
            case EXASCT_HI:
                // qcFlag     =     getBit(qualityBits,  5);    // bit  5 == 1
                // rainQcFlag =    !getBit(qualityBits, 12) &&  // bit 12 == 0 AND
                //                  getBit(qualityBits, 13);    // bit 13 == 1
                rainQcFlag =        getBit(qualityBits,  5);    // bit  5 == 1
                highWindSpeedFlag = getBit(qualityBits, 10);    // bit 10 == 1
                lowWindSpeedFlag =  getBit(qualityBits, 11);    // bit 11 == 1
                // availabilityFlag = getBit(qualityBits, 14);  // bit 14 == 1
                // redundancyFlag = getBit(qualityBits, 15);    // bit 15 == 1
                availRedunFlag =    getBit(qualityBits, 15);    // bit 15 == 1   redundancy
                break;
            case WSCAT:
                rainQcFlag = getBit(qualityBits,  0) ||
                             getBit(qualityBits, 15);           // bits 0 OR 15 == 1   rain
                highWindSpeedFlag = false;
                lowWindSpeedFlag =  false;
                availRedunFlag =    false;
                break;
            case OSCAT:  // Don't have...
            case OSCAT_HI: // ...these flags
            case UNKNOWN:
            default:
                rainQcFlag = false;
                highWindSpeedFlag = false;
                lowWindSpeedFlag =  false;
                availRedunFlag =    false;
                break;
            }
        }
        
        private boolean getBit (int bits, int bitNum) {
            // Little endian bit numbering:
            // bit 0 is LSB; bit 15 is MSB
            final int masks[] = {0x0001, 0x0002, 0x0004, 0x0008,
                                 0x0010, 0x0020, 0x0040, 0x0080,
                                 0x0100, 0x0200, 0x0400, 0x0800,
                                 0x1000, 0x2000, 0x4000, 0x8000};
            return (bits & masks[bitNum]) != 0;
        }
    }
    // @formatter:on

    // Structure grouping displayable information for a single row element
    //
    private class NcscatRowData {
        Calendar rowTime; // timestamp on this row

        ArrayList<NcscatPointData> points; // individual points in row

        public NcscatRowData(Calendar rowTime) {
            this.rowTime = rowTime;
            this.points = new ArrayList<NcscatPointData>();
        }
    }

    private IFont font;

    private NcscatMode ncscatMode = NcscatMode.UNKNOWN;

    // ------------------------------------------------------------

    /**
     * Create an NCSCAT resource.
     * 
     * @throws VizException
     */
    public NcscatResource(NcscatResourceData resourceData,
            LoadProperties loadProperties) throws VizException {
        super(resourceData, loadProperties);
        ncscatResourceData = (NcscatResourceData) resourceData;
    }

    protected AbstractFrameData createNewFrame(DataTime frameTime, int timeInt) {
        return (AbstractFrameData) new FrameData(frameTime, timeInt);
    }

    public void initResource(IGraphicsTarget grphTarget) throws VizException {
        font = grphTarget.initializeFont("Monospace", 14,
                new IFont.Style[] { IFont.Style.BOLD });
        // resourceAttrsModified();
        ncscatResourceData.setNcscatMode();
        ncscatMode = ncscatResourceData.getNcscatMode();
        queryRecords();
        // create a system resource for the colorBar and add it to the resource
        // list.
        //
        ColorBar colorBar1 = ncscatResourceData.getColorBar1();
        colorBar1
                .setReverseOrder(colorBar1.getOrientation() == ColorBarOrientation.Vertical);
        cbar1RscPair = ResourcePair
                .constructSystemResourcePair(new ColorBarResourceData(colorBar1));

        getDescriptor().getResourceList().add(cbar1RscPair);
        getDescriptor().getResourceList().instantiateResources(getDescriptor(),
                true);

        cbar1Resource = (ColorBarResource) cbar1RscPair.getResource();

        ColorBar colorBar2 = ncscatResourceData.getColorBar2();
        colorBar2
                .setReverseOrder(colorBar2.getOrientation() == ColorBarOrientation.Vertical);
        cbar2RscPair = ResourcePair
                .constructSystemResourcePair(new ColorBarResourceData(
                        ncscatResourceData.getColorBar2()));

        getDescriptor().getResourceList().add(cbar2RscPair);
        getDescriptor().getResourceList().instantiateResources(getDescriptor(),
                true);

        cbar2Resource = (ColorBarResource) cbar2RscPair.getResource();

        if (!ncscatResourceData.use2ndColorForRainEnable) {
            getDescriptor().getResourceList().remove(cbar2RscPair);
        }

        // following is done in ANCR.paintInternal too, but want to get it done
        // on the init thread since it's time-consuming and (1) we want to show
        // the "Initializing..." pacifier message, and (2) not lock up the GUI
        // thread during loading
        if (!newRscDataObjsQueue.isEmpty()
        // || (!newFrameTimesList.isEmpty() && getDescriptor().isAutoUpdate())
        ) {
            processNewRscDataList();
        }

    }

    public void paintFrame(AbstractFrameData frameData, IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {

        FrameData currFrameData = (FrameData) frameData;

        // Get view extent, for clipping purposes

        IExtent extent = paintProps.getView().getExtent();

        double extentMaxX = (extent.getMaxX() < 0 ? 0 : extent.getMaxX());
        double extentMinX = (extent.getMinX() < 0 ? 0 : extent.getMinX());
        double extentMaxY = (extent.getMaxY() < 0 ? 0 : extent.getMaxY());
        double extentMinY = (extent.getMinY() < 0 ? 0 : extent.getMinY());
        extentMaxX = (extentMaxX > 19999 ? 19999 : extentMaxX);
        extentMinX = (extentMinX > 19999 ? 19999 : extentMinX);
        extentMaxY = (extentMaxY > 9999 ? 9999 : extentMaxY);
        extentMinY = (extentMinY > 9999 ? 9999 : extentMinY);

        PixelExtent correctedExtent = new PixelExtent(extentMinX, extentMaxX,
                extentMinY, extentMaxY);

        // Allocate font and calculate offset parameters for text

        font = target.initializeFont("Monospace", 14,
                new IFont.Style[] { IFont.Style.BOLD });
        double screenToWorldRatio = paintProps.getCanvasBounds().width
                / paintProps.getView().getExtent().getWidth();
        // System.out.println("screenToWorldRatio = " + screenToWorldRatio);
        // Rectangle2D charSize = target.getStringBounds(font, "N");
        // double charHeight = charSize.getHeight();
        // double offsetY = charHeight / screenToWorldRatio;

        // Color Bars
        ColorBar colorBar1 = ncscatResourceData.getColorBar1();
        colorBar1.setNumDecimals(0);
        ColorBar colorBar2 = ncscatResourceData.getColorBar2();
        colorBar2.setNumDecimals(0);

        // Collection of (PGEN) vectors to be generated for display
        List<IVector> windVectors = new ArrayList<IVector>();

        // Initialize with safe defaults...
        RGB color = new RGB(155, 155, 155);
        Color[] colors = new Color[] { new Color(0, 255, 0) };
        Boolean clear = false;
        Coordinate location = new Coordinate(0.0, 0.0);
        double speed = 0.0;
        double direction = 0.0;

        // ...or option-dependent values invariant across all points.
        // Note constants in the following are "tuned" to match NMAP
        float lineWidth = (float) (ncscatResourceData.arrowWidth * 1.0);
        double sizeScale = ncscatResourceData.arrowSize * 0.135;
        double arrowHeadSize = ncscatResourceData.headSize * 0.2;
        double rainQcCircleRadiusPixels = ncscatResourceData.arrowSize
                / screenToWorldRatio * 1.0;

        // Arrow type
        String pgenCategory = "Vector";
        String pgenType = ncscatResourceData.arrowStyle.getPgenType();
        VectorType vc = ncscatResourceData.arrowStyle.getVectorType();
        boolean directionOnly = ncscatResourceData.arrowStyle
                .getDirectionOnly();

        // TODO: Unify this with above. For now, do some arrow-style-specific
        // fine tuning...
        switch (ncscatResourceData.arrowStyle) {
        case DIRECTIONAL_ARROW:
        case REGULAR_ARROW:
            sizeScale *= 1.5;
            break;
        case WIND_BARB:
            break;
        default:
            break;
        }

        // User tells us how many rows -- and points within each row -- to
        // *skip* in between ones that get displayed; add one to get full
        // cycle interval...
        int interval = ncscatResourceData.skipEnable ? ncscatResourceData.skipValue + 1
                // ...OR if skip option not selected, auto-compute interval
                // (1<=x<=6) based on zoom and user-specified density
                // (i.e., selectable progressive disclosure)
                : Math.max(
                        Math.min(
                                (int) (50 / screenToWorldRatio / ncscatResourceData.densityValue),
                                6), 1);
        // /System.out.println("interval = " + interval);
        // /System.out.println("S:W = " + screenToWorldRatio +
        // / " interval = " + interval +
        // / " density = " + ncscatResourceData.densityValue);
        int lastTimestampedMinute = -99; // flag so we only timestamp each
                                         // minute once

        // Loop through the (preprocessed) NCSCAT data records

        ArrayList<NcscatRowData> frameRows = currFrameData.frameRows;

        // Loop over ROWS in the satellite track...

        for (int rowNumber = 0; rowNumber < frameRows.size(); rowNumber++) {

            NcscatRowData rowData = frameRows.get(rowNumber);

            boolean displayRow = (rowNumber % interval == 0);

            // Loop over POINTS in this row...

            double maxXOfRow = -99999.999;
            double minXOfRow = +99999.999;
            double yAtMaxXOfRow = 0.0;
            double yAtMinXOfRow = 0.0;

            for (int pointNumber = 0; pointNumber < rowData.points.size(); pointNumber += interval) {

                // DEBUG pointData.rainQcFlag=OVERRIDErainQcFlag;
                // DEBUG pointData.highWindSpeedFlag=OVERRIDEhighWindSpeedFlag;
                // DEBUG pointData.lowWindSpeedFlag=OVERRIDElowWindSpeedFlag;
                // DEBUG pointData.availRedunFlag=OVERRIDEavailRedunFlag;

                // If point is consistent with data flags and user options...
                NcscatPointData pointData = rowData.points.get(pointNumber);
                if ((!pointData.availRedunFlag || ncscatResourceData.availabilityFlagEnable)
                        && (!pointData.rainQcFlag || ncscatResourceData.rainFlagEnable)
                        && (!pointData.highWindSpeedFlag || ncscatResourceData.highWindSpeedEnable)
                        && (!pointData.lowWindSpeedFlag || ncscatResourceData.lowWindSpeedEnable)
                        && pointData.speed > 0.0f) {
                    location = pointData.location;
                    double[] locLatLon = { location.x, location.y };
                    double[] locPix = this.descriptor.worldToPixel(locLatLon);
                    // ...and is currently in visible range
                    if (locPix != null
                            && correctedExtent.contains(locPix[0], locPix[1])) {
                        // Remember left- and right-most point locations of row,
                        // for possible time stamp line -- *even if we are not
                        // displaying the points in this row*
                        if (locPix[0] > maxXOfRow) {
                            maxXOfRow = locPix[0];
                            yAtMaxXOfRow = locPix[1];
                        }
                        if (locPix[0] < minXOfRow) {
                            minXOfRow = locPix[0];
                            yAtMinXOfRow = locPix[1];
                        }
                        if (displayRow) {
                            // Now we are "go" to display the point (build its
                            // wind vector and, if applicable, draw associated
                            // circle)
                            speed = pointData.speed;
                            direction = pointData.direction;
                            ColorBar colorBarToUse = pointData.rainQcFlag
                                    && ncscatResourceData.use2ndColorForRainEnable ? colorBar2
                                    : colorBar1;
                            color = getColorForSpeed(speed, colorBarToUse);
                            colors = new Color[] { new Color(color.red,
                                    color.green, color.blue) };
                            windVectors.add(new Vector(null, colors, lineWidth,
                                    sizeScale, clear, location, vc, speed,
                                    direction, arrowHeadSize, directionOnly,
                                    pgenCategory, pgenType));
                            if (pointData.rainQcFlag
                                    && ncscatResourceData.plotCirclesForRainEnable) {
                                PixelCoordinate pixelLoc = new PixelCoordinate(
                                        descriptor.worldToPixel(new double[] {
                                                location.x, location.y }));
                                target.drawCircle(pixelLoc.getX(),
                                        pixelLoc.getY(), pixelLoc.getZ(),
                                        rainQcCircleRadiusPixels, color,
                                        lineWidth);
                            }
                        }
                    }
                }
            }

            // Draw Time Stamps

            int hourOfDay = rowData.rowTime.get(Calendar.HOUR_OF_DAY);
            int minuteOfHour = rowData.rowTime.get(Calendar.MINUTE);
            int minuteOfDay = hourOfDay * 60 + minuteOfHour;

            if (ncscatResourceData.timeStampEnable &&
            // If this minute (of day) is a multiple of the selected timestamp
            // interval...
                    minuteOfDay % ncscatResourceData.timeStampInterval == 0 &&
                    // ...and we haven't already considered it for stamping
                    minuteOfDay != lastTimestampedMinute) {
                // Draw time line/string for the first row within that minute,
                // IF any point of that row is within visible range.
                // (Note: This is *not* the same as the first row with visible
                // point within that minute.)
                lastTimestampedMinute = minuteOfDay; // Been here; done this
                if (maxXOfRow - minXOfRow <= 2000
                        && // TODO: Find a better way to prevent wraparound
                           // Visible?
                        correctedExtent.contains(minXOfRow, yAtMinXOfRow)
                        && correctedExtent.contains(maxXOfRow, yAtMaxXOfRow)) {
                    // Draw line across track -- or as much as is currently
                    // visible...
                    target.drawLine(minXOfRow, yAtMinXOfRow, 0.0, maxXOfRow,
                            yAtMaxXOfRow, 0.0,
                            ncscatResourceData.timeStampColor,
                            ncscatResourceData.timeStampLineWidth);
                    // ...and mark the time digits...
                    String timeString = String.format("%02d", hourOfDay)
                            + String.format("%02d", minuteOfHour);
                    // ...both at the "leftmost" point...
                    target.drawString(font, timeString, minXOfRow,
                            yAtMinXOfRow, 0.0, TextStyle.NORMAL,
                            ncscatResourceData.timeStampColor,
                            HorizontalAlignment.RIGHT, 0.0);
                    // ...and the "rightmost" point
                    target.drawString(font, timeString, maxXOfRow,
                            yAtMaxXOfRow, 0.0, TextStyle.NORMAL,
                            ncscatResourceData.timeStampColor,
                            HorizontalAlignment.LEFT, 0.0);
                }
            }
        }

        font.dispose();

        // Draw the combined wind vectors for all points

        DisplayElementFactory df = new DisplayElementFactory(target,
                getNcMapDescriptor());
        ArrayList<IDisplayable> displayEls = df.createDisplayElements(
                windVectors, paintProps);
        for (IDisplayable each : displayEls) {
            each.draw(target, paintProps);
            each.dispose();
        }

    }

    private RGB getColorForSpeed(double speed, ColorBar colorBar) {
        for (int i = 0; i < colorBar.getNumIntervals(); i++) {
            if (colorBar.isValueInInterval(i, (float) speed, windSpeedUnits)) {
                return colorBar.getRGB(i);
            }
        }
        return new RGB(255, 0, 0);
    }

    public void disposeInternal() {
        // if( font != null ) {
        // font.dispose();
        // }
        getDescriptor().getResourceList().remove(cbar1RscPair);
        getDescriptor().getResourceList().remove(cbar2RscPair);
    }

    public void resourceAttrsModified() {
        // update the colorbarPainters with possibly new colorbars
        boolean isCbar2Enabled = (getDescriptor().getResourceList().indexOf(
                cbar2RscPair) != -1);
        //
        if (ncscatResourceData.use2ndColorForRainEnable && !isCbar2Enabled) {
            cbar2RscPair = ResourcePair
                    .constructSystemResourcePair(new ColorBarResourceData(
                            ncscatResourceData.getColorBar2()));

            getDescriptor().getResourceList().add(cbar2RscPair);
            getDescriptor().getResourceList().instantiateResources(
                    getDescriptor(), true);

            cbar2Resource = (ColorBarResource) cbar2RscPair.getResource();
        } else if (!ncscatResourceData.use2ndColorForRainEnable
                && isCbar2Enabled) {
            // this will cause the ResourceCatalog to dispose of the
            // resource so we will need to create a new one here.
            getDescriptor().getResourceList().remove(cbar2RscPair);
            cbar2RscPair = null;
            cbar2Resource = null;
        }

        ColorBar colorBar1 = ncscatResourceData.getColorBar1();
        colorBar1
                .setReverseOrder(colorBar1.getOrientation() == ColorBarOrientation.Vertical);
        cbar1Resource.setColorBar(colorBar1);

        if (cbar2Resource != null) {
            ColorBar colorBar2 = ncscatResourceData.getColorBar2();
            colorBar2
                    .setReverseOrder(colorBar2.getOrientation() == ColorBarOrientation.Vertical);
            cbar2Resource.setColorBar(colorBar2);
        }
    }

    @Override
    public void propertiesChanged(ResourceProperties updatedProps) {

        if (cbar1RscPair != null) {
            cbar1RscPair.getProperties().setVisible(updatedProps.isVisible());
        }
        if (cbar2RscPair != null) {
            cbar2RscPair.getProperties().setVisible(updatedProps.isVisible());
        }
    }

    @Override
    public String getName() {
        String legendString = super.getName();
        FrameData fd = (FrameData) getCurrentFrame();
        if (fd == null || fd.getFrameTime() == null || fd.frameRows.size() == 0) {
            return legendString + "-No Data";
        }
        return legendString + " "
                + NmapCommon.getTimeStringFromDataTime(fd.getFrameTime(), "/");
    }
}