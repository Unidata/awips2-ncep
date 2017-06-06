package gov.noaa.nws.ncep.viz.rsc.ncscat.rsc;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import javax.measure.unit.NonSI;
import javax.measure.unit.Unit;

import org.eclipse.swt.graphics.RGB;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.DrawableCircle;
import com.raytheon.uf.viz.core.DrawableLine;
import com.raytheon.uf.viz.core.DrawableString;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.HorizontalAlignment;
import com.raytheon.uf.viz.core.IGraphicsTarget.VerticalAlignment;
import com.raytheon.uf.viz.core.PixelExtent;
import com.raytheon.uf.viz.core.drawables.IFont;
import com.raytheon.uf.viz.core.drawables.IRenderable;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.geom.PixelCoordinate;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.ResourceProperties;
import com.vividsolutions.jts.geom.Coordinate;

import gov.noaa.nws.ncep.gempak.parameters.colorbar.ColorBarOrientation;
import gov.noaa.nws.ncep.ui.pgen.display.DisplayElementFactory;
import gov.noaa.nws.ncep.ui.pgen.display.IDisplayable;
import gov.noaa.nws.ncep.ui.pgen.display.IVector;
import gov.noaa.nws.ncep.ui.pgen.display.IVector.VectorType;
import gov.noaa.nws.ncep.ui.pgen.elements.Vector;
import gov.noaa.nws.ncep.viz.common.ui.NmapCommon;
import gov.noaa.nws.ncep.viz.resources.AbstractFrameData;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource2;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.IRscDataObject;
import gov.noaa.nws.ncep.viz.resources.colorBar.ColorBarResource;
import gov.noaa.nws.ncep.viz.resources.colorBar.ColorBarResourceData;
import gov.noaa.nws.ncep.viz.ui.display.ColorBar;
import gov.noaa.nws.ncep.viz.ui.display.NCMapDescriptor;

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
 * 10/28/2009   176        B. Hebbard  Initial creation (as QuikSCAT).
 * 02/14/2010   235A       B. Hebbard  Convert from QuikSCAT (@D3) to NCSCAT (@D6)
 * 06/11/2010   235B       B. Hebbard  Expand for all Ocean Winds data types
 * 07/14/2010   235C       B. Hebbard  Use common NC ColorBar
 * 01/14/2011   235D       B. Hebbard  Add density select; performance enhancement via new PGEN aggregate vector display
 * 02/03/2011   235E       B. Hebbard  Add support for ambiguity variants
 * 11/16/2011              B. Hebbard  Fix excess rowCount increment in paintFrame()
 * 05/23/2012   785        Q. Zhou     Added getName for legend.
 * 08/16/2012   843        B. Hebbard  Added OSCAT
 * 08/17/2012   655        B. Hebbard  Added paintProps as parameter to IDisplayable draw
 * 12/19/2012   960        Greg Hull   override propertiesChanged() to update colorBar.
 * 05/30/2013              B. Hebbard  Merge changes by RTS in OB13.3.1 for DataStoreFactory.getDataStore(...)
 * 04/11/2014   1128       B. Hebbard  Prevent overflow if unsigned longitude >= 327.68W (32768)
 * 05/20/2014   TTR985     B. Hebbard  Reverse bit ordering in getBit(...); tweak QC circle scaling
 * 07/29/2014   R4279      B. Hebbard  (TTR 1046) Add call to processNewRscDataList() in initResource()
 *                                     (instead of waiting for ANCR.paintInternal() to do it) so
 *                                     long CGM retrieval and parsing is done by the InitJob, and thus
 *                                     (1) user sees "Initializing..." and (2) GUI doesn't lock up
 * 10/10/2014   R4864      B. Hebbard  (TTR 986) ColorBar plotting high to low when in horizontal;
 *                                     regression caused by reverseOrder enhancement for imagery
 *                                     resources (in vertical).  Fix:  Set reverseOrder for NCSCAT colorbars
 *                                     to false for horizontal and true for vertical.
 * 01/10/2015   R5939      B. Hebbard  (TTR 984) Handle new year change between observation and
 *                                     display time.
 * 05 Nov 2015   5070      randerso    Adjust font sizes for dpi scaling
 * 
 * 01/27/2016   R10155     B. Hebbard  To reduce memory usage, move get-HDF5-and-convert-to-NcscatRowData-list 
 *                                     work from updateFrameData() up to earlier processRecord(), so frames
 *                                     sharing the same NcscatRowData will share the same object, instead of 
 *                                     creating a duplicate copy.  NcscatRowData thus becomes the new 
 *                                     time-matchable RDO, which also improves timing accuracy of row
 *                                     assignment to frames.  Fix timestamps to ensure UTC.
 *                                     Refactor to cache PGEN drawables (vectors) and IGraphicsTarget
 *                                     renderables in FrameData (as new RenderableFrame), and only regenerate
 *                                     when needed instead of on each paint.
 *                                     Refactor to separate out IDataLoader methods into new NcscatDataLoader
 *                                     class (per new common design scheme).
 * 05/09/2017    R27171    P. Moyer    Modified initResource to take parent resource's
 *                                     visibility and apply it to the newly created color bar
 * </pre>
 * 
 * @author bhebbard
 * @version 1.0
 */
public class NcscatResource
        extends AbstractNatlCntrsResource2<NcscatResourceData, NCMapDescriptor>
        implements INatlCntrsResource {

    private static final IUFStatusHandler logger = UFStatus
            .getHandler(NcscatResource.class);

    private NcscatResourceData ncscatResourceData;

    private Unit<?> windSpeedUnits = NonSI.KNOT;

    // Two color bars are used in this resource; one for normal points, and an
    // optional one to be applied only to certain points (e.g, marked as
    // affected by rain, or quality control failure)

    private ColorBarResource colorBar1Resource = null;;

    private ResourcePair colorbar1ResourcePair = null;

    private ColorBarResource colorBar2Resource = null;

    private ResourcePair colorBar2ResourcePair = null;

    // Font to use (for timestamps)
    private IFont font;

    /**
     * FrameRenderables -- just a local container class to hold the current
     * graphical representation of one frame
     */
    private class FrameRenderables implements IRenderable {
        // PGEN drawable objects each representing one wind vector
        List<IVector> windVectors = null;

        // A2 (IGraphicsTarget) drawable circles (sometimes used to flag
        // certain wind vector cells)
        Collection<DrawableCircle> drawableCircles = null;

        // A2 (IGraphicsTarget) drawable lines (used for timestamp indication)
        Collection<DrawableLine> drawableLines = null;

        // A2 (IGraphicsTarget) drawable strings (used for timestamp text)
        Collection<DrawableString> drawableStrings = null;

        public FrameRenderables() {
            windVectors = new ArrayList<IVector>();
            drawableCircles = new ArrayList<DrawableCircle>();
            drawableLines = new ArrayList<DrawableLine>();
            drawableStrings = new ArrayList<DrawableString>();
        }

        public void paint(IGraphicsTarget target, PaintProperties paintProps) {
            paintVectors(target, paintProps);
            paintCircles(target, paintProps);
            paintLines(target, paintProps);
            paintStrings(target, paintProps);
        }

        private void paintVectors(IGraphicsTarget target,
                PaintProperties paintProps) {

            // Draw the combined wind vectors for all points

            DisplayElementFactory df = new DisplayElementFactory(target,
                    getNcMapDescriptor());

            // From the PGEN 'drawable' vectors, generate the lower-level
            // 'displayable' objects -- which are aggregated wireframes, each
            // for all vectors of the same color...
            Collection<IDisplayable> displayableElements = df
                    .createDisplayElements(windVectors, paintProps);

            // ...which we draw and immediately dispose. (Could cache these
            // in FrameRenderables instead, but that was found to consume
            // excessive memory.)
            for (IDisplayable de : displayableElements) {
                de.draw(target, paintProps);
                de.dispose();
            }
        }

        private void paintCircles(IGraphicsTarget target,
                PaintProperties paintProps) {
            for (DrawableCircle dc : drawableCircles) {
                try {
                    target.drawCircle(dc);
                } catch (VizException e) {
                    logger.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
                }
            }
        }

        private void paintLines(IGraphicsTarget target,
                PaintProperties paintProps) {
            for (DrawableLine dl : drawableLines) {
                try {
                    target.drawLine(dl);
                } catch (VizException e) {
                    logger.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
                }
            }
        }

        private void paintStrings(IGraphicsTarget target,
                PaintProperties paintProps) {
            try {
                target.drawStrings(drawableStrings);
            } catch (VizException e) {
                logger.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
            }
        }
    }

    private class FrameData extends AbstractFrameData {

        // Numerical/boolean data for all rows matched to this frame
        ArrayList<NcscatRowData> frameRows = null;

        // Graphical representation generated from the above
        FrameRenderables frameRenderables = null;

        // Flag to indicate frame renderables need to be regenerated
        private boolean renderablesNeedRegen = false;

        // PixelExtent in effect when this frame's renderables last generated
        private PixelExtent correctedExtent = null;

        // Constructor
        public FrameData(DataTime frameTime, int timeInt) {
            super(frameTime, timeInt, ncscatResourceData);
            frameRows = new ArrayList<NcscatRowData>();
        }

        @Override
        public boolean updateFrameData(IRscDataObject rdo) {

            // We're given an RDO -- which for this resource is an NcscatRowData
            if (!(rdo instanceof NcscatRowData)) { // sanity check
                logger.error(
                        "NcscatResource expecting NcscatRowData instead of:  "
                                + rdo.getClass().getName());
                return false;
            } else {
                NcscatRowData ncscatRowData = (NcscatRowData) rdo;

                // Add the row to the frame's list of rows to paint
                frameRows.add(ncscatRowData);

                // New data added to this frame (either originally, or later on
                // auto-update) means renderables will need to be (re)created
                markRenderablesNeedRegen();

                return true;
            }
        }

        /*
         * (Re)generate graphical representation for this frame, given its
         * numerical/boolean data (frameRows), the current resource attributes
         * in effect (ncscatResourceData), and the current paintProperties.
         */
        private void generateRenderables(IGraphicsTarget target,
                PaintProperties paintProps) {

            // Create new container for renderable data (releasing storage from
            // any previous one)
            frameRenderables = new FrameRenderables();

            // Get view extent (for clipping purposes) and screen-to-world ratio

            correctedExtent = computeCorrectedExtent(paintProps);
            double screenToWorldRatio = computeScreenToWorldRatio(paintProps);

            // Get main and alternate Color Bars to use for color lookups based
            // on speed of each point
            ColorBar colorBar1 = ncscatResourceData.getColorBar1();
            colorBar1.setNumDecimals(0);
            ColorBar colorBar2 = ncscatResourceData.getColorBar2();
            colorBar2.setNumDecimals(0);

            // Collection of (PGEN) vectors to be generated for display
            List<IVector> windVectors = new ArrayList<IVector>();

            // Initialize with safe defaults for point-specific parameters...
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

            // Vector (arrow characteristics...
            String pgenCategory = "Vector";
            String pgenType = ncscatResourceData.arrowStyle.getPgenType();
            VectorType vc = ncscatResourceData.arrowStyle.getVectorType();
            boolean directionOnly = ncscatResourceData.arrowStyle
                    .getDirectionOnly();
            // ...and any arrow-style-specific fine tuning...
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

            // Usually we only display every "n"th row, for clarity.
            // Determine "n" (selected or computed)...
            int rowDisplayInterval = determineRowDisplayInterval(
                    ncscatResourceData, screenToWorldRatio);

            int lastTimestampedMinute = -99; // flag so we only timestamp each
                                             // minute once

            // Loop over ROWS in the satellite track...

            for (int rowNumber = 0; rowNumber < frameRows.size(); rowNumber++) {

                NcscatRowData rowData = frameRows.get(rowNumber);

                boolean displayRow = (rowNumber % rowDisplayInterval == 0);

                // Loop over POINTS in this row...

                double[] firstVisiblePointOfRow = null;
                double[] lastVisiblePointOfRow = null;

                for (int pointNumber = 0; pointNumber < rowData.rowPoints
                        .size(); pointNumber += rowDisplayInterval) {

                    // If point is consistent with data flags and user
                    // options...
                    NcscatPointData pointData = rowData.rowPoints
                            .get(pointNumber);
                    if (pointData
                            .pointMeetsSelectedCriteria(ncscatResourceData)) {
                        location = pointData.getLocation();
                        double[] locLatLon = { location.x, location.y };
                        double[] locPix = descriptor.worldToPixel(locLatLon);
                        // ...and is currently in visible range
                        if (locPix != null && correctedExtent
                                .contains(locPix[0], locPix[1])) {
                            // Remember first and last visible (in extent) point
                            // locations of row, for possible time stamp line --
                            // *even if we are not displaying the points in this
                            // row*
                            if (firstVisiblePointOfRow == null) {
                                firstVisiblePointOfRow = locPix;
                            }
                            lastVisiblePointOfRow = locPix;
                            if (displayRow) {
                                // Now we are "go" to display the point (build
                                // its wind vector and, if applicable, construct
                                // associated drawable circle)
                                speed = pointData.getSpeed();
                                direction = pointData.getDirection();
                                ColorBar colorBarToUse = pointData
                                        .getRainQcFlag()
                                        && ncscatResourceData.use2ndColorForRainEnable
                                                ? colorBar2 : colorBar1;
                                color = getColorForSpeed(speed, colorBarToUse);
                                colors = new Color[] { new Color(color.red,
                                        color.green, color.blue) };
                                windVectors.add(new Vector(null, colors,
                                        lineWidth, sizeScale, clear, location,
                                        vc, speed, direction, arrowHeadSize,
                                        directionOnly, pgenCategory, pgenType));
                                if (pointData.getRainQcFlag()
                                        && ncscatResourceData.plotCirclesForRainEnable) {
                                    PixelCoordinate pixelLoc = new PixelCoordinate(
                                            descriptor.worldToPixel(
                                                    new double[] { location.x,
                                                            location.y }));
                                    DrawableCircle dc = new DrawableCircle();
                                    dc.basics.x = pixelLoc.getX();
                                    dc.basics.y = pixelLoc.getY();
                                    dc.basics.z = pixelLoc.getZ();
                                    dc.basics.color = color;
                                    dc.radius = rainQcCircleRadiusPixels;
                                    dc.lineWidth = lineWidth;
                                    frameRenderables.drawableCircles.add(dc);
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
                        // If this minute (of day) is a multiple of the selected
                        // timestamp interval...
                minuteOfDay % ncscatResourceData.timeStampInterval == 0 &&
                        // ...and we haven't already considered it for stamping
                        minuteOfDay != lastTimestampedMinute) {
                    // Draw time line/string for the first row within that
                    // minute, IF any point of that row is within visible range.
                    // (Note: This is *not* the same as the first row with
                    // visible point within that minute.)
                    lastTimestampedMinute = minuteOfDay; // Been here; done this
                    if (firstVisiblePointOfRow != null
                            && lastVisiblePointOfRow != null
                            && correctedExtent.contains(firstVisiblePointOfRow)
                            && correctedExtent.contains(lastVisiblePointOfRow)
                            && Math.abs(firstVisiblePointOfRow[0] // world wrap
                                    - lastVisiblePointOfRow[0]) < 2000) {
                        // Draw line across track -- or as much as is currently
                        // visible...
                        DrawableLine dl = new DrawableLine();
                        dl.setCoordinates(firstVisiblePointOfRow[0],
                                firstVisiblePointOfRow[1]);
                        dl.addPoint(lastVisiblePointOfRow[0],
                                lastVisiblePointOfRow[1]);
                        dl.basics.color = ncscatResourceData.timeStampColor;
                        dl.width = ncscatResourceData.timeStampLineWidth;
                        frameRenderables.drawableLines.add(dl);
                        // ...and mark the time digits...
                        String timeString = String.format("%02d", hourOfDay)
                                + String.format("%02d", minuteOfHour);
                        boolean leftToRight = (firstVisiblePointOfRow[0] < lastVisiblePointOfRow[0]);
                        boolean bottomToTop = (firstVisiblePointOfRow[1] < lastVisiblePointOfRow[1]);
                        // ...both at the first point of row...
                        DrawableString ds1 = new DrawableString(timeString);
                        ds1.basics.x = firstVisiblePointOfRow[0];
                        ds1.basics.y = firstVisiblePointOfRow[1];
                        ds1.font = font;
                        ds1.horizontalAlignment = leftToRight
                                ? HorizontalAlignment.RIGHT
                                : HorizontalAlignment.LEFT;
                        ds1.verticallAlignment = bottomToTop
                                ? VerticalAlignment.BOTTOM
                                : VerticalAlignment.TOP;
                        ds1.setText(timeString,
                                ncscatResourceData.timeStampColor);
                        frameRenderables.drawableStrings.add(ds1);
                        // ...and the last point of row
                        DrawableString ds2 = new DrawableString(timeString);
                        ds2.basics.x = lastVisiblePointOfRow[0];
                        ds2.basics.y = lastVisiblePointOfRow[1];
                        ds2.font = font;
                        ds2.horizontalAlignment = leftToRight
                                ? HorizontalAlignment.LEFT
                                : HorizontalAlignment.RIGHT;
                        ds2.verticallAlignment = bottomToTop
                                ? VerticalAlignment.TOP
                                : VerticalAlignment.BOTTOM;
                        ds2.setText(timeString,
                                ncscatResourceData.timeStampColor);
                        frameRenderables.drawableStrings.add(ds2);
                    }
                }

                // Store the combined wind vectors for all points
                frameRenderables.windVectors = windVectors;
            }
        }

        private int determineRowDisplayInterval(NcscatResourceData resourceData,
                double screenToWorldRatio) {
            if (resourceData.skipEnable) {
                // User can select how many rows -- and points within each row
                // -- to *skip* in between ones that get displayed; add one to
                // get full cycle interval...
                return ncscatResourceData.skipValue + 1;
            } else {
                // ...OR if skip option not selected, auto-compute the interval
                // based on zoom and user-specified relative density index
                // (1-99) (i.e., selectable progressive disclosure)
                int computedInterval = (int) (50 / screenToWorldRatio
                        / ncscatResourceData.densityValue);
                // Limit computed interval to between 1 and 6
                computedInterval = computedInterval > 6 ? 6 : computedInterval;
                computedInterval = computedInterval < 1 ? 1 : computedInterval;
                return computedInterval;
            }
        }

        public void markRenderablesNeedRegen() {
            renderablesNeedRegen = true;
        }

        public void paint(IGraphicsTarget target, PaintProperties paintProps) {
            if (renderablesNeedRegen || frameRenderables == null
                    || hasExtentChanged(paintProps)) {
                generateRenderables(target, paintProps);
                renderablesNeedRegen = false;
            }
            frameRenderables.paint(target, paintProps);
        }

        private boolean hasExtentChanged(PaintProperties paintProps) {
            // Has the corrected extent changed since last regen?
            PixelExtent currentCorrectedExtent = computeCorrectedExtent(
                    paintProps);
            if (correctedExtent == null) {
                return true;
            } else if (correctedExtent.equals(currentCorrectedExtent)) {
                return false;
            } else {
                return true;
            }
        }
    }

    // ------------------------------------------------------------

    /**
     * Create an NCSCAT resource.
     * 
     * @throws VizExceptionj
     */
    public NcscatResource(NcscatResourceData resourceData,
            LoadProperties loadProperties) throws VizException {
        super(resourceData, loadProperties);
        ncscatResourceData = (NcscatResourceData) resourceData;
    }

    protected AbstractFrameData createNewFrame(DataTime frameTime,
            int timeInt) {
        return (AbstractFrameData) new FrameData(frameTime, timeInt);
    }

    public void initResource(IGraphicsTarget grphTarget) throws VizException {
        font = grphTarget.initializeFont("Monospace", 12,
                new IFont.Style[] { IFont.Style.BOLD });

        ncscatResourceData.setNcscatMode();

        dataLoader.loadData();

        // Create a system resource for the colorBar and add it to the resource
        // list.
        //
        ColorBar colorBar1 = ncscatResourceData.getColorBar1();
        colorBar1.setReverseOrder(
                colorBar1.getOrientation() == ColorBarOrientation.Vertical);
        colorbar1ResourcePair = ResourcePair.constructSystemResourcePair(
                new ColorBarResourceData(colorBar1));

        getDescriptor().getResourceList().add(colorbar1ResourcePair);
        getDescriptor().getResourceList().instantiateResources(getDescriptor(),
                true);

        colorBar1Resource = (ColorBarResource) colorbar1ResourcePair
                .getResource();

        ColorBar colorBar2 = ncscatResourceData.getColorBar2();
        colorBar2.setReverseOrder(
                colorBar2.getOrientation() == ColorBarOrientation.Vertical);
        colorBar2ResourcePair = ResourcePair.constructSystemResourcePair(
                new ColorBarResourceData(ncscatResourceData.getColorBar2()));

        getDescriptor().getResourceList().add(colorBar2ResourcePair);
        getDescriptor().getResourceList().instantiateResources(getDescriptor(),
                true);

        colorBar2Resource = (ColorBarResource) colorBar2ResourcePair
                .getResource();

        // set the color bar's visiblity to match that of the parent resource
        // by changing the Resource Parameter's isVisible value.

        boolean parentVisibility = getProperties().isVisible();
        colorbar1ResourcePair.getProperties().setVisible(parentVisibility);
        colorBar2ResourcePair.getProperties().setVisible(parentVisibility);

        if (!ncscatResourceData.use2ndColorForRainEnable) {
            getDescriptor().getResourceList().remove(colorBar2ResourcePair);
        }

    }

    public void paintFrame(AbstractFrameData frameData, IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {
        // Delegate painting to the frame itself
        ((FrameData) frameData).paint(target, paintProps);
    }

    private PixelExtent computeCorrectedExtent(PaintProperties paintProps) {

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

        return new PixelExtent(extentMinX, extentMaxX, extentMinY, extentMaxY);

    }

    private double computeScreenToWorldRatio(PaintProperties paintProps) {
        return paintProps.getCanvasBounds().width
                / paintProps.getView().getExtent().getWidth();

    }

    private RGB getColorForSpeed(double speed, ColorBar colorBar) {
        for (int i = 0; i < colorBar.getNumIntervals(); i++) {
            if (colorBar.isValueInInterval(i, (float) speed, windSpeedUnits)) {
                return colorBar.getRGB(i);
            }
        }
        return new RGB(255, 0, 0); // default to RED if no lookup match
                                   // (shouldn't happen)
    }

    public void disposeInternal() {
        if (font != null) {
            font.dispose();
            font = null;
        }
        getDescriptor().getResourceList().remove(colorbar1ResourcePair);
        getDescriptor().getResourceList().remove(colorBar2ResourcePair);
    }

    public void resourceAttrsModified() {
        // update the colorbarPainters with possibly new colorbars
        boolean isColorBar2Enabled = (getDescriptor().getResourceList()
                .indexOf(colorBar2ResourcePair) != -1);
        //
        if (ncscatResourceData.use2ndColorForRainEnable
                && !isColorBar2Enabled) {
            colorBar2ResourcePair = ResourcePair
                    .constructSystemResourcePair(new ColorBarResourceData(
                            ncscatResourceData.getColorBar2()));

            getDescriptor().getResourceList().add(colorBar2ResourcePair);
            getDescriptor().getResourceList()
                    .instantiateResources(getDescriptor(), true);

            colorBar2Resource = (ColorBarResource) colorBar2ResourcePair
                    .getResource();
        } else if (!ncscatResourceData.use2ndColorForRainEnable
                && isColorBar2Enabled) {
            // This will cause the ResourceCatalog to dispose of the
            // resource so we will need to create a new one here.
            getDescriptor().getResourceList().remove(colorBar2ResourcePair);
            colorBar2ResourcePair = null;
            colorBar2Resource = null;
        }

        ColorBar colorBar1 = ncscatResourceData.getColorBar1();
        colorBar1.setReverseOrder(
                colorBar1.getOrientation() == ColorBarOrientation.Vertical);
        colorBar1Resource.setColorBar(colorBar1);

        if (colorBar2Resource != null) {
            ColorBar colorBar2 = ncscatResourceData.getColorBar2();
            colorBar2.setReverseOrder(
                    colorBar2.getOrientation() == ColorBarOrientation.Vertical);
            colorBar2Resource.setColorBar(colorBar2);
        }

        // Any change to resource attributes triggers image regeneration across
        // all frames
        markAllFramesRenderablesNeedRegen();
    }

    @Override
    public void propertiesChanged(ResourceProperties updatedProps) {
        if (colorbar1ResourcePair != null) {
            colorbar1ResourcePair.getProperties()
                    .setVisible(updatedProps.isVisible());
        }
        if (colorBar2ResourcePair != null) {
            colorBar2ResourcePair.getProperties()
                    .setVisible(updatedProps.isVisible());
        }
    }

    @Override
    public String getName() {
        String legendString = super.getName();
        FrameData fd = (FrameData) getCurrentFrame();
        if (fd == null || fd.getFrameTime() == null
                || fd.frameRows.size() == 0) {
            return legendString + " - No Data";
        }
        return legendString + " "
                + NmapCommon.getTimeStringFromDataTime(fd.getFrameTime(), "/");
    }

    private void markAllFramesRenderablesNeedRegen() {
        for (AbstractFrameData frame : frameDataMap.values()) {
            if (frame instanceof FrameData) { // sanity check
                ((FrameData) frame).markRenderablesNeedRegen();
            }
        }
    }

}
