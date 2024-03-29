package gov.noaa.nws.ncep.viz.rsc.ncgrid.contours;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.RGB;
import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.geospatial.ISpatialObject;
import com.raytheon.uf.common.geospatial.MapUtil;
import com.raytheon.uf.common.geospatial.ReferencedCoordinate;
import com.raytheon.uf.common.geospatial.ReferencedObject.Type;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.LineStyle;
import com.raytheon.uf.viz.core.drawables.IWireframeShape;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.drawables.ext.colormap.IColormapShadedShapeExtension;
import com.raytheon.uf.viz.core.drawables.ext.colormap.IColormapShadedShapeExtension.IColormapShadedShape;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.IMapDescriptor;
import com.raytheon.uf.viz.core.point.display.VectorGraphicsConfig;
import com.raytheon.uf.viz.core.rsc.DisplayType;
import com.raytheon.uf.viz.ncep.grid.FloatGridData;
import com.raytheon.uf.viz.ncep.grid.NcgribLogger;

import gov.noaa.nws.ncep.common.tools.IDecoderConstantsN;
import gov.noaa.nws.ncep.viz.common.ui.color.GempakColor;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.rsc.NcgridResourceData;

/**
 *
 * Performs same functions as the original GriddedVectorDisplay using wireframe
 * shapes instead of svg for much faster performance. This is still slightly
 * experimental but seems to work well. It should also have the drawing code
 * extracted to a class similar to PointWindDisplay so wireframe shape barbs and
 * arrows can be used elsewhere.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 22, 2010            bsteffen     Initial creation
 * Nov 22, 2010            M. Li        modified from RTS for NCGRID
 * Nov 02, 2011            X. Guo       Updated
 * Feb 06, 2012  #538      Q. Zhou      Changed density to filter.
 * Feb 15, 2012  #539      Q. Zhou      Change barb tail direction on south hemisphere
 * Mar 01, 2012            X. Guo       Added isDirectional and contourAttributes
 *                                       to handle vector type changes
 * Apr 03, 2012            X. Guo       Added createWireFrame
 * May 23, 2012            X. Guo       Loaded ncgrib logger
 * Apr 26, 2013            B. Yin       Don't plot missing values.
 * Dec.12, 2014  R5113     J. Wu        Detect real change in view extent to avoid
 *                                      re-creation of the wireframeShape.
 * May 05, 2015  RM7058    S. Russell   Made wind barbs same size as in NMAP,
 *                                      made flags be filled shapes at user
 *                                      request. Altered code in constructor,
 *                                      paint(),paintGlobalImage,paintImage() and paintBarb().
 *                                      Added getFilledShape()
 * Sep 25, 2015  R12041                 Update the vgconfig.setMinimumMagnitude
 *                                      call in the constructor
 * Apr 21, 2016  R17741    S. Gilbert   Calculate speed direction when plotting instead of up front
 * Oct 25, 2018  54483     mapeters     Handle {@link NcgribLogger} refactor
 * Nov 15, 2018  58493     edebebe      Enabled configurable 'Wind Barb' properties
 * </pre>
 *
 * @author bsteffen
 *
 */
public class GriddedVectorDisplay extends AbstractGriddedDisplay<Coordinate> {

    private final Logger logger = LoggerFactory.getLogger("PerformanceLogger");

    private final ISpatialObject gridLocation;

    private static final int SIZE = 64;

    private float lineWidth = 1;

    private LineStyle lineStyle = LineStyle.SOLID;

    private double scale = 0.6;

    private IExtent lastExtent;

    private IWireframeShape lastShape;

    private DisplayType displayType;

    private GeodeticCalculator gc;

    private FloatGridData data;

    private ContourAttributes contourAttributes;

    private Coordinate latLon;

    private boolean directional;

    private static NcgribLogger ncgribLogger  = NcgribLogger.getInstance();

    private VectorGraphicsConfig vgconfig = null;

    private boolean fillFlag = false;

    private String flagFill = "1";

    private IColormapShadedShape filledShape;

    private static final double barbAdjSizeMultiplier = 0.030;

    private static final double barbAAMultiplier = 0.707;

    private static final double barbSPDIncrease = 2.5;
    
    private static final double barbSizeScaler = 0.55;

    //Parameters used to construct 'VectorGraphicsConfig'
    private static final String PLUGIN_NAME = "NcgridPlugin";
    private static final String CLASS_NAME = "GriddedVectorDisplay";

    /**
     * @param NcFloatDataRecord
     * @param DisplayType
     */
    public GriddedVectorDisplay(FloatGridData rec, DisplayType displayType,
            boolean directional, IMapDescriptor descriptor,
            ISpatialObject gridLocation, ContourAttributes attrs) {
        super(descriptor, MapUtil.getGridGeometry(gridLocation),
                gridLocation.getNx(), gridLocation.getNy());
        long t1 = System.currentTimeMillis();
        this.data = rec;
        this.contourAttributes = attrs;

        long t2 = System.currentTimeMillis();
        logger.debug(
                "GriddedVectorDisplay after check -999999 took:" + (t2 - t1));
        this.gridLocation = gridLocation;
        this.displayType = displayType;
        this.gc = new GeodeticCalculator(descriptor.getCRS());
        this.directional = directional;

        int colorIndex = 31;
        float sizeFactor = 1;
        lineWidth = 1;
        String windAttr = attrs.getWind();
        if (windAttr != null && !windAttr.isEmpty()) {

            windAttr = windAttr.replaceAll("[^0-9./]", "");

            String[] attr = windAttr.trim().split("/");
            colorIndex = Integer.parseInt(attr[0].trim());
            if (attr.length >= 2 && !attr[1].trim().isEmpty()) {
                sizeFactor = Float.parseFloat(attr[1].trim());
            }
            if (attr.length >= 3 && !attr[2].trim().isEmpty()) {
                lineWidth = Float.parseFloat(attr[2].trim());
            }
            if (attr.length >= 4 && !attr[3].trim().isEmpty()) {
                attr[3] = attr[3].trim();
                if (attr[3].length() >= 3) {
                    flagFill = attr[3].substring(2);
                    if ("2".equals(flagFill)) {
                        fillFlag = true;
                    }
                }
            }

        }

        setSize(sizeFactor * SIZE);
        setColor(GempakColor.convertToRGB(colorIndex));

        vgconfig = new VectorGraphicsConfig(PLUGIN_NAME, CLASS_NAME);
        vgconfig.setSizeScaler(barbSizeScaler);
        vgconfig.setBarbFillFiftyTriangle(fillFlag);
    }

    /**
     * @param NcgridResourceData
     * @param IGraphicsTarget
     */
    @Override
    public void paint(NcgridResourceData gridRscData, IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {

        if (paintProps.isZooming()) {
            return;
        }

        /*
         * Check the view extent using a 1 pixel sentinel instead of using
         * "equals" check. "equals" checks the equality of two doubles, which is
         * not quite reliable. This helps avoid re-creating the wireframeShape
         * unless a "real" change in view extent happens, e.g., simply clicking
         * or moving mouse won't cause the shape to be re-created.
         */

        if (viewExtentChanged(lastExtent, paintProps.getView().getExtent())) {
            disposeImages();
            lastExtent = paintProps.getView().getExtent().clone();
        }

        if (lastShape == null) {
            lastShape = target.createWireframeShape(false, descriptor);
            super.paint(gridRscData, target, paintProps);
            lastShape.compile();
        }

        target.drawWireframeShape(lastShape, color, lineWidth, lineStyle);

        if (filledShape != null) {
            if (filledShape.isDrawable()) {
                Map<Object, RGB> colorMap = new HashMap<>();
                colorMap.put(this, color);
                target.getExtension(IColormapShadedShapeExtension.class)
                        .drawColormapShadedShape(filledShape, colorMap, 1.0f,
                                1.0f);
            }
        }
    }

    /**
     *
     * @param NcgridResourceData
     * @param IGraphicsTarget
     * @param PaintProperties
     */
    public void createWireFrame(NcgridResourceData gridRscData,
            IGraphicsTarget target, PaintProperties paintProps) {

        if (lastShape == null) {
            try {

                /*
                 * Save this viewExtent so we do not need to recreate the
                 * wireframeShape unless the view extent changes.
                 */
                lastExtent = paintProps.getView().getExtent().clone();

                long t1 = System.currentTimeMillis();
                lastShape = target.createWireframeShape(false, descriptor);
                super.paint(gridRscData, target, paintProps);
                lastShape.compile();
                long t4 = System.currentTimeMillis();
                if (ncgribLogger.isEnableContourLogs()) {
                    logger.debug(
                            "--GriddedVectorDisplay: create wireframe took:"
                                    + (t4 - t1));
                }

            } catch (VizException e) {
                lastShape = null;
                logger.error("Error creating wire frame: " + e);
            }
        }
    }

    private IColormapShadedShape getFilledShape(IGraphicsTarget target)
            throws VizException {

        IColormapShadedShape filledShape = null;
        filledShape = target.getExtension(IColormapShadedShapeExtension.class)
                .createColormapShadedShape(descriptor.getGridGeometry(), false);
        return filledShape;

    }

    @Override
    protected void paintImage(int x, int y, PaintProperties paintProps,
            double adjSize) throws VizException {
        float spd, dir;

        int idx = x + y * this.gridDims[0];
        if (idx < 0 || idx >= (gridDims[0] * gridDims[1])) {
            return;
        }

        if (directional) {
            spd = 40.0f;
            dir = data.getXdata().get(idx);
        } else {
            spd = getSpeed(idx);
            dir = getDirection(idx);
        }

        if (Float.isNaN(spd) || Float.isNaN(dir)) {
            return;
        }

        if (this.isPlotted[idx]) {
            return;
        }

        ReferencedCoordinate newrco = new ReferencedCoordinate(
                new Coordinate(x, y), this.gridGeometryOfGrid,
                Type.GRID_CENTER);
        Coordinate plotLoc = null;

        try {
            plotLoc = newrco.asPixel(this.descriptor.getGridGeometry());
            latLon = newrco.asLatLon();

            if (latLon.x > 180 || latLon.x < -180 || latLon.y < -90
                    || latLon.y > 90) {
                return;
            }

            double[] stationLocation = { latLon.x, latLon.y };
            double[] stationPixelLocation = this.descriptor
                    .worldToPixel(stationLocation);

            if (stationPixelLocation != null) {
                stationPixelLocation[1]--;
                double[] newWorldLocation = this.descriptor
                        .pixelToWorld(stationPixelLocation);
                this.gc.setStartingGeographicPoint(stationLocation[0],
                        stationLocation[1]);
                this.gc.setDestinationGeographicPoint(newWorldLocation[0],
                        newWorldLocation[1]);
            }

            dir = dir + (float) MapUtil.rotation(latLon, gridLocation);
            dir -= this.gc.getAzimuth();
        } catch (Exception e) {
            throw new VizException(e);
        }

        dir = (float) Math.toRadians(dir);

        switch (displayType) {
        case ARROW:
            paintArrow(plotLoc, adjSize, spd, dir);
            break;
        case BARB:
            if (vgconfig.isBarbFillFiftyTriangle() && filledShape == null) {
                filledShape = getFilledShape(target);
            }
            paintBarb(plotLoc, adjSize, spd, dir);
            break;
        case DUALARROW:
            paintDualArrow(plotLoc, adjSize, spd, dir);
            break;
        default:
            throw new VizException("Unsupported disply type: " + displayType);
        }

        this.isPlotted[idx] = true;
    }

    /*
     * Calculate speed from U and V components at grid point idx.
     */
    private float getSpeed(int idx) {
        float u = data.getXdata().get(idx);
        float v = data.getYdata().get(idx);
        float speed;

        if (u == IDecoderConstantsN.GRID_MISSING
                || v == IDecoderConstantsN.GRID_MISSING) {
            return Float.NaN;
        }

        speed = (float) Math.hypot(u, v);
        return speed;
    }

    /*
     * Calculate direction from U and V components at grid point idx.
     */
    private float getDirection(int idx) {
        float u = data.getXdata().get(idx);
        float v = data.getYdata().get(idx);
        float direction;

        if (u == IDecoderConstantsN.GRID_MISSING
                || v == IDecoderConstantsN.GRID_MISSING) {
            return Float.NaN;
        }

        direction = (float) (Math.atan2(u, v) * 180 / Math.PI) + 180;

        return direction;
    }

    private void paintBarb(Coordinate plotLoc, double adjSize, double spd,
            double dir) {

        // Don't plot missing value
        if (spd < vgconfig.getMinimumMagnitude()) {
            return;
        }

        if (spd < vgconfig.getCalmCircleMaximumMagnitude()) {
            double[][] line = new double[9][2];
            double aa = adjSize * barbAdjSizeMultiplier;
            double saa = aa * barbAAMultiplier;

            line[8][0] = line[0][0] = plotLoc.x + aa;
            line[8][1] = line[0][1] = plotLoc.y;
            line[1][0] = plotLoc.x + saa;
            line[1][1] = plotLoc.y + saa;

            line[2][0] = plotLoc.x;
            line[2][1] = plotLoc.y + aa;

            line[3][0] = plotLoc.x - saa;
            line[3][1] = plotLoc.y + saa;

            line[4][0] = plotLoc.x - aa;
            line[4][1] = plotLoc.y;

            line[5][0] = plotLoc.x - saa;
            line[5][1] = plotLoc.y - saa;

            line[6][0] = plotLoc.x;
            line[6][1] = plotLoc.y - aa;

            line[7][0] = plotLoc.x + saa;
            line[7][1] = plotLoc.y - saa;

            lastShape.addLineSegment(line);

            return;
        }

        int speed = (int) (spd + barbSPDIncrease);
        double staff = adjSize * vgconfig.getSizeScaler();
        double barb = staff * vgconfig.getBarbLengthRatio();
        double add = staff * vgconfig.getBarbSpacingRatio();

        if (latLon.y >= 0) {
            vgconfig.setBarbRotationRadians(Math.toRadians(75));
        } else {
            // southern hemisphere
            vgconfig.setBarbRotationRadians(Math.toRadians(115));
        }

        // DIRECTIONS
        double uudd = -spd * Math.sin(dir);
        double vvff = -spd * Math.cos(dir);
        double dix = -uudd / spd;
        double djy = -vvff / spd;
        double barbRotationRadians = vgconfig.getBarbRotationRadians();
        double dix1 = Math.cos(barbRotationRadians) * dix
                + Math.sin(barbRotationRadians) * djy;
        double djy1 = (-1) * Math.sin(barbRotationRadians) * dix
                + Math.cos(barbRotationRadians) * djy;

        // SPEED AND COUNTERS:
        int n50 = speed / 50;
        int calcSpd = speed - 50 * n50;
        int n10 = calcSpd / 10;
        calcSpd = calcSpd - 10 * n10;
        int n5 = calcSpd / 5;
        double sx = ((n50 + n50 + n10 + n5 + 2)) * add;
        staff = Math.max(adjSize * .4, sx);

        // DRAW STAFF
        double ix2 = plotLoc.x;
        double jy2 = plotLoc.y;
        double ix1 = ix2 + dix * staff;
        double jy1 = jy2 - djy * staff;
        lastShape.addLineSegment(new double[][] { { ix2, jy2 }, { ix1, jy1 } });

        // PLOT LONE HALF-BARB, IF NECESSARY
        if (n50 == 0 && n10 == 0) {
            if (latLon.y >= 0) {
                ix2 = ix1 + dix1 * barb / 2.0; // -
                jy2 = jy1 - djy1 * barb / 2.0; // +
            } else {
                ix2 = ix1 - dix1 * barb / 2.0;
                jy2 = jy1 + djy1 * barb / 2.0;
            }

            ix1 = ix1 - dix * add;
            jy1 = jy1 + djy * add;
            lastShape.addLineSegment(
                    new double[][] { { ix2, jy2 }, { ix1, jy1 } });
            return;
        }

        double ix3 = 0;
        double jy3 = 0;

        // PLOT FLAGS, IF NECESSARY
        for (int i = 0; i < n50; i++) {
            ix3 = ix1 - dix * add * 2;
            jy3 = jy1 + djy * add * 2;

            if (latLon.y >= 0) {
                ix2 = ix1 + dix1 * barb; // +
                jy2 = jy1 - djy1 * barb; // -
            } else {
                ix2 = ix1 - dix1 * barb;
                jy2 = jy1 + djy1 * barb;
            }

            lastShape.addLineSegment(
                    new double[][] { { ix2, jy2 }, { ix1, jy1 } });
            lastShape.addLineSegment(
                    new double[][] { { ix2, jy2 }, { ix3, jy3 } });
            if (vgconfig.isBarbFillFiftyTriangle()) {
                double[] triangleRaw = { ix1, jy1, ix2, jy2, ix3, jy3, ix1,
                        jy1 };
                CoordinateSequence triangleSeq = new PackedCoordinateSequence.Double(
                        triangleRaw, 2, 0);
                LineString triangleLS = new GeometryFactory()
                        .createLineString(triangleSeq);
                filledShape.addPolygonPixelSpace(
                        new LineString[] { triangleLS }, this);
            }
            ix1 = ix1 - dix * add * 2;
            jy1 = jy1 + djy * add * 2;
        }

        if (n50 > 0) {
            ix1 = ix1 - dix * add / 2.0;
            jy1 = jy1 + djy * add / 2.0;
        }

        // PLOT BARB, IF NECESSARY
        for (int i = 0; i < n10; i++) {
            if (latLon.y >= 0) {
                ix2 = ix1 + dix1 * barb; // +
                jy2 = jy1 - djy1 * barb;// -
            } else {
                ix2 = ix1 - dix1 * barb;
                jy2 = jy1 + djy1 * barb;
            }

            lastShape.addLineSegment(
                    new double[][] { { ix2, jy2 }, { ix1, jy1 } });
            ix1 = ix1 - dix * add;
            jy1 = jy1 + djy * add;
        }

        // PLOT HALF-BARB, IF NECESSARY
        if (n5 != 0) {
            if (latLon.y >= 0) {
                ix2 = ix1 + dix1 * barb / 2.0; // +
                jy2 = jy1 - djy1 * barb / 2.0; // -
            } else {
                ix2 = ix1 - dix1 * barb / 2.0;
                jy2 = jy1 + djy1 * barb / 2.0;
            }
            lastShape.addLineSegment(
                    new double[][] { { ix2, jy2 }, { ix1, jy1 } });
        }
    }

    private void paintDualArrow(Coordinate plotLoc, double adjSize, double spd,
            double dir) {
        if (spd < 4.0) {
            return;
        }
        double staff = 0.0;
        if (this.scale > 0.0) {
            staff = spd * this.scale;
        } else {
            staff = Math.log10(spd * -this.scale) * 10 + 10;
        }

        double barb = 4.0;

        if (staff < barb) {
            return;
        }

        double ratio = adjSize / size;
        staff *= ratio;
        barb *= ratio;

        // DIRECTIONS
        double uudd = -spd * Math.sin(dir);
        double vvff = -spd * Math.cos(dir);
        double dix = uudd / spd;
        double djy = vvff / spd;
        double dix1 = -dix - djy;
        double djy1 = dix - djy;

        // DRAW BODY OF ARROW
        double ix2 = plotLoc.x;
        double jy2 = plotLoc.y;
        double ix1 = ix2 + dix * staff;
        double jy1 = jy2 - djy * staff;

        double ix3 = ix1 + dix1 * barb;
        double jy3 = jy1 - djy1 * barb;
        double ix4 = ix2 - dix1 * barb;
        double jy4 = jy2 + djy1 * barb;
        lastShape.addLineSegment(new double[][] { { ix4, jy4 }, { ix2, jy2 },
                { ix1, jy1 }, { ix3, jy3 } });

    }

    private void paintArrow(Coordinate plotLoc, double adjSize, double spd,
            double dir) {
        if (spd == 0.0) {
            return;
        }
        double staff = 0.0;
        if (this.scale > 0.0) {
            staff = spd * this.scale;
        } else {
            staff = Math.log10(spd * -this.scale) * 10 + 10;
        }

        double barb = 4.0;

        if (staff < barb) {
            return;
        }

        double ratio = adjSize / size;
        staff *= ratio;
        barb *= ratio;

        // DIRECTIONS
        double uudd = -spd * Math.sin(dir);
        double vvff = -spd * Math.cos(dir);
        double dix = uudd / spd;
        double djy = vvff / spd;
        double dix1 = -dix - djy;
        double djy1 = dix - djy;
        double dix2 = -dix + djy;
        double djy2 = -dix - djy;

        // DRAW BODY OF ARROW
        double ix2 = plotLoc.x;
        double jy2 = plotLoc.y;
        double ix1 = ix2 + dix * staff;
        double jy1 = jy2 - djy * staff;
        lastShape.addLineSegment(new double[][] { { ix2, jy2 }, { ix1, jy1 } });
        // DRAW HEAD OF ARROW.
        ix2 = ix1 + dix1 * barb;
        jy2 = jy1 - djy1 * barb;
        double ix3 = ix1 + dix2 * barb;
        double jy3 = jy1 - djy2 * barb;
        lastShape.addLineSegment(
                new double[][] { { ix2, jy2 }, { ix1, jy1 }, { ix3, jy3 } });

    }

    /**
     *
     * @param color
     */
    public void setScale(double scale) {
        this.scale = scale;
    }

    /**
     *
     * @param float
     *            lineWidth
     */
    public void setLineWidth(float lineWidth) {
        this.lineWidth = lineWidth;
    }

    /**
     * @param lineStyle
     */
    public void setLineStyle(LineStyle lineStyle) {
        this.lineStyle = lineStyle;
    }

    /**
     * @param filter
     *            the filter to set. Changed from density.
     */
    @Override
    public boolean setFilter(double filter) {
        if (super.setFilter(filter)) {
            disposeImages();
            if (this.target != null) {
                this.target.setNeedsRefresh(true);
            }
            return true;
        }
        return false;
    }

    /**
     * @param magnification
     *            the magnification to set
     */
    @Override
    public boolean setMagnification(double magnification) {
        if (super.setMagnification(magnification)) {
            disposeImages();
            if (this.target != null) {
                this.target.setNeedsRefresh(true);
            }
            return true;
        }
        return false;
    }

    @Override
    protected void disposeImages() {
        if (lastShape != null) {
            lastShape.dispose();
            lastShape = null;
        }
        if (filledShape != null) {
            filledShape.dispose();
            filledShape = null;
        }

    }

    @Override
    protected Coordinate createImage(Coordinate coord) throws VizException {
        return coord;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.raytheon.viz.core.contours.rsc.displays.AbstractGriddedImageDisplay
     * #getImage(com.raytheon.uf.common.geospatial.ReferencedCoordinate)
     */
    @Override
    protected Coordinate getImage(Coordinate coord) {
        return coord;
    }

    /**
     * @return NcFloatDataRecord
     */
    public FloatGridData getData() {
        return data;
    }

    /**
     *
     * @param type
     * @param dir
     * @param Stri
     * @return boolean
     */
    public boolean checkAttrsChanged(DisplayType type, boolean dir,
            String attr) {
        boolean isChanged = false;
        if (this.displayType != type || this.directional != dir
                || !this.contourAttributes.getWind().equalsIgnoreCase(attr)) {
            isChanged = true;
        }
        return isChanged;
    }

    /**
     *
     * @param attr
     * @return
     */
    public boolean isMatch(ContourAttributes attr) {
        boolean match = false;
        if (this.contourAttributes == null) {
            return match;
        }
        if (this.contourAttributes.getGlevel()
                .equalsIgnoreCase(attr.getGlevel())
                && this.contourAttributes.getGvcord()
                        .equalsIgnoreCase(attr.getGvcord())
                && this.contourAttributes.getScale()
                        .equalsIgnoreCase(attr.getScale())
                && this.contourAttributes.getGdpfun()
                        .equalsIgnoreCase(attr.getGdpfun())) {
            match = true;
        }
        return match;
    }

    /**
     * TODO: HACK hack hack ... this version of paintImage is being used for
     * global grids. I don't think the grid <-> latlon transforms are working,
     * so the index calculation has been modified. This is not a good solution,
     * but was implemented due to time crunch for 13.5.2
     */
    @Override
    protected void paintGlobalImage(int x, int y, PaintProperties paintProps,
            double adjSize) throws VizException {
        float spd, dir;

        int adjx = x - 1;
        int adjy = y + 1;
        if (x > 0) {
            adjx++;
            adjy = y;
        }
        int idx = adjx + adjy * this.gridDims[0];

        if (idx < 0 || idx >= (gridDims[0] * gridDims[1])) {
            return;
        }

        if (directional) {
            spd = 40.0f;
            dir = data.getXdata().get(idx);
        } else {
            spd = getSpeed(idx);
            dir = getDirection(idx);
        }

        if (Float.isNaN(spd) || Float.isNaN(dir)) {
            return;
        }

        if (this.isPlotted[idx]) {
            return;
        }

        ReferencedCoordinate newrco = new ReferencedCoordinate(
                new Coordinate(x, y), this.gridGeometryOfGrid,
                Type.GRID_CENTER);
        Coordinate plotLoc = null;

        try {
            plotLoc = newrco.asPixel(this.descriptor.getGridGeometry());
            latLon = newrco.asLatLon();

            if (latLon.x > 180 || latLon.x < -180 || latLon.y < -90
                    || latLon.y > 90) {
                return;
            }

            double[] stationLocation = { latLon.x, latLon.y };
            double[] stationPixelLocation = this.descriptor
                    .worldToPixel(stationLocation);

            if (stationPixelLocation != null) {
                stationPixelLocation[1]--;
                double[] newWorldLocation = this.descriptor
                        .pixelToWorld(stationPixelLocation);
                this.gc.setStartingGeographicPoint(stationLocation[0],
                        stationLocation[1]);
                this.gc.setDestinationGeographicPoint(newWorldLocation[0],
                        newWorldLocation[1]);
            }

            dir = dir + (float) MapUtil.rotation(latLon, gridLocation);
            dir -= this.gc.getAzimuth();
        } catch (Exception e) {
            throw new VizException(e);
        }

        dir = (float) Math.toRadians(dir);

        switch (displayType) {
        case ARROW:
            paintArrow(plotLoc, adjSize, spd, dir);
            break;
        case BARB:
            if (vgconfig.isBarbFillFiftyTriangle() && filledShape == null) {
                filledShape = getFilledShape(target);
            }
            paintBarb(plotLoc, adjSize, spd, dir);
            break;
        case DUALARROW:
            paintDualArrow(plotLoc, adjSize, spd, dir);
            break;
        default:
            throw new VizException("Unsupported disply type: " + displayType);
        }

        this.isPlotted[idx] = true;
    }

    /*
     * Check if two PixelExtents has significant changes - R5113.
     */
    private boolean viewExtentChanged(IExtent origExt, IExtent newExt) {
        double sentinel = 1.0;
        if (origExt == null || newExt == null) {
            return true;
        } else {
            if (Math.abs(origExt.getMinX() - newExt.getMinX()) > sentinel
                    || Math.abs(origExt.getMaxX() - newExt.getMaxX()) > sentinel
                    || Math.abs(origExt.getMinY() - newExt.getMinY()) > sentinel
                    || Math.abs(
                            origExt.getMaxY() - newExt.getMaxY()) > sentinel) {
                return true;
            } else {
                return false;
            }
        }
    }
}
