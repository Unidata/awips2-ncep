package gov.noaa.nws.ncep.viz.overlays.resources;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.raytheon.uf.viz.core.DrawableString;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.HorizontalAlignment;
import com.raytheon.uf.viz.core.IGraphicsTarget.VerticalAlignment;
import com.raytheon.uf.viz.core.drawables.IFont;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.MapDescriptor;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.ResourceProperties;
import com.vividsolutions.jts.geom.Coordinate;

import gov.noaa.nws.ncep.ui.pgen.display.DisplayElementFactory;
import gov.noaa.nws.ncep.ui.pgen.display.IDisplayable;
import gov.noaa.nws.ncep.ui.pgen.elements.SymbolLocationSet;
import gov.noaa.nws.ncep.viz.common.staticPointDataSource.IStaticPointDataSource;
import gov.noaa.nws.ncep.viz.common.staticPointDataSource.LabeledPoint;
import gov.noaa.nws.ncep.viz.common.staticPointDataSource.StaticPointDataSourceMngr;
import gov.noaa.nws.ncep.viz.common.ui.Markers.MarkerState;
import gov.noaa.nws.ncep.viz.common.ui.Markers.MarkerTextSize;
import gov.noaa.nws.ncep.viz.resources.IStaticDataNatlCntrsResource;

/**
 * <pre>
 *   
 * SOFTWARE HISTORY
 *   
 *    Date          Ticket#       Engineer      Description
 * -----------    ----------    -----------    --------------------------
 * 07/03/2013       #1010         ghull         Initial creation
 * 12/03/2015       R9407         pchowdhuri    Maps/overlays need to be able to reference the 
 *                                              Localization Cave>Bundles>Maps XML files
 * 02/17/2016       #13554        dgilling      Implement IStaticDataNatlCntrsResource.
 * 07/03/13         #1010         ghull         Initial creation
 * 11/05/2015       #5070         randerso      Adjust font sizes for dpi scaling
 * 09/09/2016   -----     mjames@ucar Simple name.
 * 11/12/2016       R20573        jbeck         Change the legend string text, and the algorithm for creating it (for county names)
 * 
 * </pre>
 * 
 * @author randerso
 * 
 */

public class PointOverlayResource
        extends AbstractVizResource<PointOverlayResourceData, MapDescriptor>
        implements IStaticDataNatlCntrsResource {

    private PointOverlayResourceData ptOvrlyRscData;

    private List<LabeledPoint> labeledPoints;

    /* The set of symbols with similar attributes across many locations */
    private SymbolLocationSet symbolSet = null;

    /*
     * A flag indicating new symbols are needed next time we repaint with
     * markers active
     */
    private boolean symbolSetRegenerationNeeded = true;

    /*
     * Whether to draw marker symbol and draw ID at each point These are set
     * from the MarkerState enum in the resourceData
     */
    private boolean drawSymbols = true;

    private boolean drawLabels = true;

    private IFont font = null;

    double charWidth;

    double charHeight;

    private List<DrawableString> labelStrings = null;

    private ArrayList<IDisplayable> symDispElmtsList = null;

    private IExtent prevExtent = null;

    private Integer numVisPoints = 0;

    private Integer numLabeledPoints = 0;

    protected PointOverlayResource(PointOverlayResourceData resourceData,
            LoadProperties loadProperties) {
        super(resourceData, loadProperties);
        ptOvrlyRscData = resourceData;
        updateDrawFlagsFromMarkerState();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.viz.core.rsc.AbstractVizResource#getName()
     */
    @Override
    public String getName() {
        return ptOvrlyRscData.getMapName();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.core.rsc.AbstractVizResource#init(com.raytheon.uf
     * .viz.core.IGraphicsTarget)
     */
    @Override
    public void initInternal(IGraphicsTarget target) throws VizException {

        IStaticPointDataSource ptSrc = StaticPointDataSourceMngr
                .createPointDataSource(ptOvrlyRscData.getSourceType(),
                        ptOvrlyRscData.getSourceName(),
                        ptOvrlyRscData.getSourceParams());
        ptSrc.loadData();

        labeledPoints = ptSrc.getPointData();

        // set the pixel values from the lat/lons
        project(this.descriptor.getCRS());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.core.rsc.IVizResource#paint(com.raytheon.viz.core.
     * IGraphicsTarget, com.raytheon.viz.core.PixelExtent, double, float)
     */
    @Override
    public void paintInternal(IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {

        // This could be implemented in ProgressiveDisclosureProperties(in
        // ResourceProperties)
        // but it is an attribute.
        int displayWidth = (int) (descriptor.getMapWidth()
                * paintProps.getZoomLevel() / 1000);

        if (displayWidth > ptOvrlyRscData.getMaxSymbolDisplayWidth()
                && displayWidth > ptOvrlyRscData.getMaxLabelDisplayWidth()) {
            this.numVisPoints = 0;
            return;
        }

        // if the first time painting or if
        // the extents have changed and we are not zooming (ie wait till last
        // zoom to
        // determine the new labels/symbols
        //
        if (prevExtent == null
                || (!paintProps.isZooming() && !(Math
                        .abs(prevExtent.getMaxX() - paintProps.getView()
                                .getExtent().getMaxX()) < .01
                && Math.abs(prevExtent.getMaxY()
                        - paintProps.getView().getExtent().getMaxY()) < .01
                && Math.abs(prevExtent.getMinX()
                        - paintProps.getView().getExtent().getMinX()) < .01
                && Math.abs(prevExtent.getMinY()
                        - paintProps.getView().getExtent().getMinY()) < .01))) {

            symbolSetRegenerationNeeded = true;

            disposeSymbolElements();
            disposeLabelStrings();
        }

        prevExtent = paintProps.getView().getExtent().clone();

        double screenToWorldRatio = paintProps.getCanvasBounds().width
                / paintProps.getView().getExtent().getWidth();

        if (font == null) {
            font = target.initializeFont("Monospace",
                    12 * ptOvrlyRscData.getMarkerTextSize().getSoftwareSize(),
                    null);
            font.setSmoothing(false);
            font.setScaleFont(false);
            DrawableString line = new DrawableString("N");
            line.font = font;
            Rectangle2D charSize = target.getStringsBounds(line);
            charWidth = charSize.getWidth();
            charHeight = charSize.getHeight();
        }

        if (drawSymbols) {
            if (symbolSetRegenerationNeeded) {

                symbolSetRegenerationNeeded = false;

                disposeSymbolElements();

                symbolSet = null;
                numVisPoints = 0;
                numLabeledPoints = 0;

                // SymbolLocationSet constructor requires a positive-length
                // array of Coordinate
                List<Coordinate> visibleLocs = new ArrayList<>();

                for (LabeledPoint lp : labeledPoints) {
                    double[] latlon = new double[] { lp.getLongitude(),
                            lp.getLatitude() };
                    numLabeledPoints++;
                    double[] pix = this.descriptor.worldToPixel(latlon);

                    // there is at least one bad point at the south pole
                    if (pix == null) {
                        continue;
                    }

                    if (paintProps.getView().isVisible(pix)
                            && displayWidth <= ptOvrlyRscData
                                    .getMaxSymbolDisplayWidth()) {

                        visibleLocs.add(new Coordinate(latlon[0], latlon[1]));
                    }
                }

                if (!visibleLocs.isEmpty()) {
                    numVisPoints = visibleLocs.size();
                    Coordinate[] locations = visibleLocs
                            .toArray(new Coordinate[0]);

                    Color[] colors = new Color[] {
                            new Color(ptOvrlyRscData.getColor().red,
                                    ptOvrlyRscData.getColor().green,
                                    ptOvrlyRscData.getColor().blue) };

                    symbolSet = new SymbolLocationSet(null, colors,
                            ptOvrlyRscData.getMarkerWidth(),
                            // sizeScale
                            ptOvrlyRscData.getMarkerSize() * 0.75,
                            // clear
                            false, locations,
                            // category
                            "Marker",
                            ptOvrlyRscData.getMarkerType().toString());
                }
            }

            if (symbolSet != null) {
                if (symDispElmtsList == null) {
                    DisplayElementFactory df = new DisplayElementFactory(target,
                            this.descriptor);
                    symDispElmtsList = df.createDisplayElements(symbolSet,
                            paintProps);
                }

                if (symDispElmtsList != null) {
                    for (IDisplayable symElmt : symDispElmtsList) {
                        symElmt.draw(target, paintProps);
                    }
                }
            }
        }

        if (drawLabels) {
            if (labelStrings == null || labelStrings.isEmpty()) {
                double offsetX = 0.0;
                double offsetY = 0.0;

                if (drawSymbols) {
                    offsetY = charHeight / screenToWorldRatio;
                }

                labelStrings = new ArrayList<>();

                for (LabeledPoint lp : labeledPoints) {
                    double[] latlon = new double[] { lp.getLongitude(),
                            lp.getLatitude() };
                    double[] pix = this.descriptor.worldToPixel(latlon);

                    if (pix == null) {
                        continue;
                    }

                    if (paintProps.getView().isVisible(pix)
                            && displayWidth <= ptOvrlyRscData
                                    .getMaxLabelDisplayWidth()) {

                        DrawableString drawStr = new DrawableString(
                                lp.getName(), ptOvrlyRscData.getColor());
                        drawStr.font = font;
                        drawStr.setCoordinates(pix[0] + offsetX,
                                pix[1] + offsetY);
                        drawStr.horizontalAlignment = HorizontalAlignment.CENTER;
                        drawStr.verticalAlignment = VerticalAlignment.MIDDLE;
                        labelStrings.add(drawStr);
                    }
                }
            }

            if (labelStrings != null) {
                target.drawStrings(labelStrings);
            }
        }
    }

    @Override
    public void project(CoordinateReferenceSystem mapData) throws VizException {
    }

    public void updateDrawFlagsFromMarkerState() {
        MarkerState markerState = ptOvrlyRscData.getMarkerState();

        drawSymbols = (markerState == MarkerState.MARKER_ONLY
                || markerState == MarkerState.MARKER_PLUS_TEXT);
        drawLabels = (markerState == MarkerState.TEXT_ONLY
                || markerState == MarkerState.MARKER_PLUS_TEXT);
    }

    /**
     * @param markerTextSize
     *            the markerTextSize to set
     */
    public void setMarkerTextSize(MarkerTextSize markerTextSize) {
        ptOvrlyRscData.setMarkerTextSize(markerTextSize);
    }

    @Override
    public void resourceAttrsModified() {

        ResourceProperties rprop = getProperties();
        rprop.getPdProps();

        symbolSetRegenerationNeeded = true;
        updateDrawFlagsFromMarkerState();

        disposeSymbolElements();
        disposeLabelStrings();

        if (font != null) {
            font.dispose();
            font = null;
        }
    }

    @Override
    protected void disposeInternal() {

        if (font != null) {
            font.dispose();
            font = null;
        }

        disposeSymbolElements();
    }

    private void disposeLabelStrings() {
        if (labelStrings != null) {
            labelStrings.clear();
        }
    }

    private void disposeSymbolElements() {
        if (symDispElmtsList != null) {
            for (IDisplayable e : symDispElmtsList) {
                e.dispose();
            }
            symDispElmtsList = null;
        }
    }
}
