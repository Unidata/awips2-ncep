/**
 * 
 * gov.noaa.nws.ncep.ui.nsharp.background.NsharpSkewTPaneBackground
 * 
 * This java class performs the NSHARP NsharpSkewTPaneBackground functions.
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date        Ticket#    Engineer   Description
 * ----------- ---------- ---------- -----------
 * 05/02/2012   229       Chin Chen  Initial coding for multiple display panes implementation
 * 09/01/2017   RM#34794  Chin Chen  NSHARP - Updates for March 2017 bigSharp version
 *                                      -     Update the dendritic growth layer calculations and other skewT
 *                                            updates.
 * 09/28/2018   7479      bsteffen   Fix temperature lines and labels.
 * 10/16/2018   6835      bsteffen   Extract printing logic.
 *
 * </pre>
 * 
 * @author Chin Chen
 * @version 1.0
 */
package gov.noaa.nws.ncep.ui.nsharp.background;

import static java.lang.Math.pow;
import static java.lang.Math.signum;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.graphics.Rectangle;

import com.raytheon.uf.common.sounding.util.Equations;
import com.raytheon.uf.common.sounding.util.UAPoint;
import com.raytheon.uf.viz.core.DrawableLine;
import com.raytheon.uf.viz.core.DrawableString;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.HorizontalAlignment;
import com.raytheon.uf.viz.core.IGraphicsTarget.LineStyle;
import com.raytheon.uf.viz.core.IGraphicsTarget.VerticalAlignment;
import com.raytheon.uf.viz.core.PixelExtent;
import com.raytheon.uf.viz.core.drawables.IWireframeShape;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;
import org.locationtech.jts.geom.Coordinate;

import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;
import gov.noaa.nws.ncep.ui.nsharp.NsharpConfigManager;
import gov.noaa.nws.ncep.ui.nsharp.NsharpConstants;
import gov.noaa.nws.ncep.ui.nsharp.NsharpGraphProperty;
import gov.noaa.nws.ncep.ui.nsharp.NsharpWGraphics;
import gov.noaa.nws.ncep.ui.nsharp.NsharpWxMath;
import gov.noaa.nws.ncep.ui.nsharp.display.NsharpSkewTPaneDescriptor;

public class NsharpSkewTPaneBackground extends NsharpGenericPaneBackground {

    private static final float[] MIXING_RATIOS = { .5f, 1, 1.5f, 2, 3, 4, 5, 6,
            7, 9, 10, 12.5f, 15, 20, 25, 30 };

    private static final List<List<UAPoint>> SATURATED_POINTS = Equations
            .getSaturatedAdiabats(1000, 100, 20, -60, 60, 5);

    private static final List<List<UAPoint>> DRY_POINTS = getDryAdiabats(10,
            -40, 20);

    private static final List<Integer> MAIN_PRESSURE_LEVELS = Arrays
            .asList(NsharpConstants.PRESSURE_MAIN_LEVELS);

    private static final List<Float> MAIN_MIXING_RATIOS = Arrays.asList(.5f, 1f,
            2f, 5f, 10f, 20f);

    private static final double Rd = 0.2870586d;

    private IWireframeShape mixRatioShape;

    private IWireframeShape dryAdiabatsShape;

    private IWireframeShape moistAdiabatsShape;

    private IWireframeShape temperatureLineShape;

    private IWireframeShape presslinesNumbersShape;

    private List<DrawableString> temperatureLabels = new ArrayList<>();

    private double currentZoomLevel = 1;

    private IExtent currentExtent;

    private int skewtWidth = NsharpConstants.SKEWT_WIDTH;

    private float xRatio = 1;

    private NsharpGraphProperty graphConfigProperty;

    private int tempOffset = 0;

    public NsharpSkewTPaneBackground(NsharpSkewTPaneDescriptor desc) {
        super();

        this.rectangle = new Rectangle(NsharpConstants.SKEWT_X_ORIG,
                NsharpConstants.SKEWT_Y_ORIG, NsharpConstants.SKEWT_WIDTH,
                NsharpConstants.SKEWT_HEIGHT);
        pe = new PixelExtent(this.rectangle);
        world = new NsharpWGraphics(this.rectangle);

        world.setWorldCoordinates(NsharpConstants.left, NsharpConstants.top,
                NsharpConstants.right, NsharpConstants.bottom);
        this.desc = desc;

        NsharpConfigManager configMgr = NsharpConfigManager.getInstance();
        graphConfigProperty = configMgr.retrieveNsharpConfigStoreFromFs()
                .getGraphProperty();
    }

    private static double poisson(double startPressure, double stopPressure,
            double temperature) {
        return temperature * pow((startPressure / stopPressure), Rd);
    }

    public static List<List<UAPoint>> getDryAdiabats(double increment,
            double startTemp, double tempDist) {
        List<List<UAPoint>> dryAdiabats = new ArrayList<>();

        for (double t = startTemp; t < 100; t += tempDist) {
            dryAdiabats
                    .add(dryAdiabats(1000, 100, /* 20 */increment, t + 273.15));
        }
        return dryAdiabats;
    }

    private static List<UAPoint> dryAdiabats(double startPressure,
            double stopPressure, double increment, double adiabat) {
        ArrayList<UAPoint> adiabats = null;
        if (startPressure != stopPressure) {
            if (increment > 0) {
                adiabats = new ArrayList<>();

                double delta = signum(stopPressure - startPressure) * increment;

                double basePressure = startPressure;
                for (; startPressure >= stopPressure; startPressure += delta) {
                    UAPoint point = new UAPoint();
                    point.pressure = startPressure;
                    point.temperature = poisson(startPressure, basePressure,
                            adiabat);
                    adiabats.add(point);
                }
            }
        }
        return adiabats;
    }

    /**
     * Returns the point that two lines instersect, or null if they do not.
     * 
     * @param l1
     * @param l2
     * @return
     */
    private static Point2D.Double getLineIntersection(Line2D.Double l1,
            Line2D.Double l2) {
        if (!l1.intersectsLine(l2)) {
            return null;
        }

        Point2D.Double intersection = new Point2D.Double();
        double x1 = l1.getX1(), y1 = l1.getY1(), x2 = l1.getX2(),
                y2 = l1.getY2(), x3 = l2.getX1(), y3 = l2.getY1(),
                x4 = l2.getX2(), y4 = l2.getY2();

        intersection.x = det(det(x1, y1, x2, y2), x1 - x2, det(x3, y3, x4, y4),
                x3 - x4) / det(x1 - x2, y1 - y2, x3 - x4, y3 - y4);
        intersection.y = det(det(x1, y1, x2, y2), y1 - y2, det(x3, y3, x4, y4),
                y3 - y4) / det(x1 - x2, y1 - y2, x3 - x4, y3 - y4);

        return intersection;
    }

    private static double det(double a, double b, double c, double d) {
        return a * d - b * c;
    }

    @Override
    public void disposeInternal() {
        super.disposeInternal();
        if (mixRatioShape != null) {
            mixRatioShape.dispose();
        }
        if (dryAdiabatsShape != null) {
            dryAdiabatsShape.dispose();
        }
        if (moistAdiabatsShape != null) {
            moistAdiabatsShape.dispose();
        }
        if (temperatureLineShape != null) {
            temperatureLineShape.dispose();
            temperatureLineShape = null;
        }
        if (presslinesNumbersShape != null) {
            presslinesNumbersShape.dispose();
        }
    }

    @Override
    public void initInternal(IGraphicsTarget target) {
        super.initInternal(target);
        createMoistAdiabatsShape();
        createDryAdiabatsShape();
        createMixingRatioShape();
        createTempNumberAndLineShape();
    }

    // Chin: to handle dynamically moving pressure lines and its number within
    // viewable zone when zooming, I could not use wireframeShape successfully
    // It will chop off lower part. Therefore use this draw function.
    private void drawPressureLineNumber(IGraphicsTarget target)
            throws VizException {
        // pressureLineNumberShape
        if (target == null) {
            return;
        }
        String s = "";

        double xend;
        IExtent ext = desc.getRenderableDisplay().getExtent();
        double xmin = ext.getMinX(); // Extent's viewable envelope min x and y
        double xDefault = world.mapX(NsharpConstants.left);
        if (xmin < xDefault)
            xmin = xDefault;
        double dispX = xmin + 25 * currentZoomLevel * xRatio;
        // draw pressure line, pressure mark and pressure number label all at
        // once
        // Progressively change pressure line density when zoom in/out
        List<DrawableString> strings = new ArrayList<>();
        List<DrawableLine> lines = new ArrayList<>();
        for (int pressure : NsharpConstants.PRESSURE_MARK_LEVELS) {
            // we only care about pressure for this case, temp is no important
            // when calling getSkewTXY
            Coordinate coor = NsharpWxMath.getSkewTXY(pressure, 0);
            int mod = pressure % 100;
            if (MAIN_PRESSURE_LEVELS.contains(pressure)
                    || (currentZoomLevel <= 0.7 && mod == 0)
                    || (currentZoomLevel <= 0.4)) {
                // case 1: pressure main level line defined in
                // NsharpConstants.PRESSURE_MAIN_LEVELS
                // case 2: zoom factor <= 0.7, and pressure level at 100th
                // case 3: zoom factor < 0.4, all pressure lines
                // draw pressure line all the way from left to right on
                // skewt pane
                xend = ext.getMaxX();
                // also draw pressure number label
                s = NsharpConstants.pressFormat.format(pressure);
                DrawableString string = new DrawableString(s,
                        NsharpConstants.pressureColor);
                string.font = smallFont;
                string.setCoordinates(dispX, world.mapY(coor.y));
                string.verticallAlignment = VerticalAlignment.MIDDLE;
                strings.add(string);
            } else {
                // only mark pressure line to a small length
                // NsharpGraphProperty graphConfigProperty;
                xend = xmin + 15 * currentZoomLevel * xRatio;
            }
            DrawableLine line = new DrawableLine();
            line.addPoint(xmin, world.mapY(coor.y));
            line.addPoint(xend, world.mapY(coor.y));
            line.basics.color = NsharpConstants.pressureColor;
            lines.add(line);

        }
        target.drawStrings(strings);
        target.drawLine(lines.toArray(new DrawableLine[0]));
    }

    private void createDryAdiabatsShape() {
        if (target == null) {
            return;
        }
        // DryAdiabats shape
        dryAdiabatsShape = target.createWireframeShape(false, desc);
        dryAdiabatsShape.allocate(1500);
        for (List<UAPoint> points : DRY_POINTS) {
            UAPoint firstPoint = points.get(0);
            Coordinate startCoor = NsharpWxMath.getSkewTXY(firstPoint.pressure,
                    NsharpConstants.kelvinToCelsius
                            .convert(firstPoint.temperature));
            for (UAPoint p : points) {
                Coordinate endCoor = NsharpWxMath.getSkewTXY(p.pressure,
                        NsharpConstants.kelvinToCelsius.convert(p.temperature));
                double[][] lines = {
                        { world.mapX(startCoor.x), world.mapY(startCoor.y) },
                        { world.mapX(endCoor.x), world.mapY(endCoor.y) } };
                dryAdiabatsShape.addLineSegment(lines);

                startCoor = endCoor;
            }
        }
        dryAdiabatsShape.compile();
    }

    private void createMoistAdiabatsShape() {
        if (target == null) {
            return;
        }
        // moist Adiabats shape
        moistAdiabatsShape = target.createWireframeShape(false, desc);
        moistAdiabatsShape.allocate(2500);
        for (List<UAPoint> points : SATURATED_POINTS) {
            UAPoint firstPoint = points.get(0);
            Coordinate coor1 = NsharpWxMath.getSkewTXY(firstPoint.pressure,
                    NsharpConstants.kelvinToCelsius
                            .convert(firstPoint.temperature));
            for (UAPoint p : points) {
                Coordinate coor2 = NsharpWxMath.getSkewTXY(p.pressure,
                        NsharpConstants.kelvinToCelsius.convert(p.temperature));
                double[][] lines = {
                        { world.mapX(coor1.x), world.mapY(coor1.y) },
                        { world.mapX(coor2.x), world.mapY(coor2.y) } };
                moistAdiabatsShape.addLineSegment(lines);
                coor1 = coor2;
            }
        }
        moistAdiabatsShape.compile();
    }

    private void createMixingRatioShape() {
        if (target == null) {
            return;
        }

        // mixing ratio shape
        // get the location of the 850 pressure line...
        mixRatioShape = target.createWireframeShape(false, desc);
        mixRatioShape.allocate(MIXING_RATIOS.length * 2);

        IExtent ext = desc.getRenderableDisplay().getExtent();
        double xmin = ext.getMinX(); // Extent's viewable envelope min x and y
        double ymax = ext.getMaxY();
        double dispY = ymax - 30 * currentZoomLevel;
        // We are getting Y (pressure) level for plotting mix ratio number,
        // therefore dispX here is not important for the
        // reverseSkewTXY() input.
        Coordinate c = NsharpWxMath.reverseSkewTXY(world.unMap(xmin, dispY));
        double dispPressure = c.y - 10 * currentZoomLevel;
        if ((NsharpConstants.MAX_PRESSURE - dispPressure) < 30)
            dispPressure = NsharpConstants.MAX_PRESSURE - 30 * currentZoomLevel;
        if (dispPressure < 405)
            dispPressure = 405;
        if (dispPressure > 1000)
            dispPressure = 1000;
        Coordinate coorStart = NsharpWxMath.getSkewTXY(dispPressure, -50);
        Coordinate coorEnd = NsharpWxMath.getSkewTXY(dispPressure, 50);

        double startX = world.mapX(coorStart.x);
        double startY = world.mapY(coorStart.y);

        double endX = world.mapX(coorEnd.x);
        double endY = world.mapY(coorEnd.y);

        Line2D.Double ratioLabelLine = new Line2D.Double(startX, startY, endX,
                endY);
        Line2D.Double line2 = new Line2D.Double();
        UAPoint p1 = new UAPoint();
        p1.pressure = 1000;
        UAPoint p2 = new UAPoint();
        p2.pressure = 400;
        for (float ratio : MIXING_RATIOS) {
            if (MAIN_MIXING_RATIOS.contains(ratio)
                    || (currentZoomLevel <= 0.4)) {
                p1.temperature = Equations.invMixingRatio(p1.pressure,
                        ratio / 1000);
                p2.temperature = Equations.invMixingRatio(p2.pressure,
                        ratio / 1000);
                Coordinate coor1 = NsharpWxMath.getSkewTXY(p1.pressure,
                        p1.temperature - 273.15);
                Coordinate coor2 = NsharpWxMath.getSkewTXY(p2.pressure,
                        p2.temperature - 273.15);
                double[][] lines = {
                        { world.mapX(coor1.x), world.mapY(coor1.y) },
                        { world.mapX(coor2.x), world.mapY(coor2.y) } };
                mixRatioShape.addLineSegment(lines);

                line2.setLine(world.mapX(coor1.x), world.mapY(coor1.y),
                        world.mapX(coor2.x), world.mapY(coor2.y));
                Point2D.Double point = getLineIntersection(ratioLabelLine,
                        line2);
                if (point != null) {
                    double[] lblXy = { point.x, point.y };
                    mixRatioShape.addLabel(Float.toString(ratio), lblXy);
                }
            }
        }
        mixRatioShape.compile();

    }

    private void createTempNumberAndLineShape() {
        if (target == null) {
            return;
        }
        if (graphConfigProperty != null) {
            tempOffset = graphConfigProperty.getTempOffset();
            NsharpWxMath.setTempOffset(tempOffset);
        }

        int tempSpacing = 10;
        if (currentZoomLevel <= 0.1) {
            tempSpacing = 1;
        } else if (currentZoomLevel <= 0.2) {
            tempSpacing = 2;
        } else if (currentZoomLevel <= 0.3) {
            tempSpacing = 3;
        } else if (currentZoomLevel <= 0.4) {
            tempSpacing = 4;
        } else if (currentZoomLevel <= 0.5) {
            tempSpacing = 5;
        } else if (currentZoomLevel <= 0.75) {
            tempSpacing = 8;
        }

        /*
         * Calculate a top/bottom pressure to determine the y location of labels
         * and a min/max temperature to label.
         */
        IExtent viewableExtent = desc.getRenderableDisplay().getExtent();
        double maxX = viewableExtent.getMaxX();
        double minX = viewableExtent.getMinX();
        double maxY = viewableExtent.getMaxY();
        double minY = viewableExtent.getMinY();
        /* Add a small margin above/below the label. */
        maxY -= 20 * currentZoomLevel;
        minY += 10 * currentZoomLevel;

        Coordinate top = world.unMap(maxX, maxY);
        top = NsharpWxMath.reverseSkewTXY(top);
        Coordinate bot = world.unMap(minX, minY);
        bot = NsharpWxMath.reverseSkewTXY(bot);

        double topPres = top.y;
        double botPres = bot.y;
        /*
         * /* Add more margin? I don't understand why you would want some margin
         * in pressure units rather then pixels but this was here when I showed
         * up.
         */
        topPres -= 10 * currentZoomLevel;
        botPres += 5 * currentZoomLevel;
        /*
         * Bring the labels back on the graph if the extent covers an area above
         * or below the graph.
         */
        if ((NsharpConstants.MAX_PRESSURE - topPres) < 30) {
            topPres = NsharpConstants.MAX_PRESSURE - 30 * currentZoomLevel;
        }
        if ((botPres - NsharpConstants.MIN_PRESSURE) < 10) {
            botPres = NsharpConstants.MIN_PRESSURE + 5 * currentZoomLevel;
        }

        int minTemp = (int) bot.x;
        int maxTemp = (int) top.x;
        /*
         * Round off the temperature boundaries to be an even multiple of the
         * spacing.
         */
        minTemp = tempSpacing * ((minTemp / tempSpacing) - 1);
        maxTemp = tempSpacing * ((maxTemp / tempSpacing) + 1);

        /* Generate labels */
        temperatureLabels.clear();
        for (int temp = minTemp; temp <= maxTemp; temp += tempSpacing) {
            DrawableString string = new DrawableString(Integer.toString(temp),
                    NsharpConstants.color_white);
            Coordinate topLoc = NsharpWxMath.getSkewTXY(topPres, temp);
            topLoc = world.map(topLoc);

            string.font = smallFont;
            string.horizontalAlignment = HorizontalAlignment.CENTER;
            string.verticallAlignment = VerticalAlignment.MIDDLE;
            string.setCoordinates(topLoc.x, topLoc.y);
            temperatureLabels.add(string);

            Coordinate botLoc = NsharpWxMath.getSkewTXY(botPres, temp);
            botLoc = world.map(botLoc);
            string = new DrawableString(string);
            string.setCoordinates(botLoc.x, botLoc.y);
            temperatureLabels.add(string);
        }

        /* Generate lines. */
        if (temperatureLineShape != null) {
            temperatureLineShape.dispose();
            temperatureLineShape = null;
        }
        temperatureLineShape = target.createWireframeShape(false, desc);
        temperatureLineShape
                .allocate((1 + (maxTemp - minTemp) / tempSpacing) * 20);
        for (int temp = minTemp; temp <= maxTemp; temp += tempSpacing) {
            int index = 0;
            double[][] coords = new double[20][2];
            /*
             * Generating a point every 50 mb is the simplest way to ensure that
             * some portion of the line is on the visible portion of the graph.
             * If only 2 points are used then the line won't draw if the
             * endpoints are off the edge of the graph.
             */
            for (int pres = (int) NsharpConstants.MIN_PRESSURE; pres <= NsharpConstants.MAX_PRESSURE; pres += 50) {
                Coordinate loc = NsharpWxMath.getSkewTXY(pres, temp);
                loc = world.map(loc);
                coords[index][0] = loc.x;
                coords[index][1] = loc.y;
                index += 1;
            }
            temperatureLineShape.addLineSegment(coords);
        }
    }

    @Override
    public void paintInternal(IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {
        float zoomLevel = paintProps.getZoomLevel();
        if (zoomLevel > 1.0f) {
            zoomLevel = 1.0f;
        }
        IExtent extent = paintProps.getView().getExtent();
        if (zoomLevel != currentZoomLevel || currentExtent == null
                || !currentExtent.equals(extent)) {
            currentExtent = extent;
            currentZoomLevel = zoomLevel;
            handleZooming();
        }
        target.setupClippingPlane(pe);
        this.smallFont.setSmoothing(false);
        this.smallFont.setScaleFont(false);
        if (graphConfigProperty != null) {
            if (graphConfigProperty.isMixratio() == true) {
                target.drawWireframeShape(mixRatioShape,
                        NsharpConstants.mixingRatioColor, 1, LineStyle.DASHED,
                        smallFont);
            }
            if (graphConfigProperty.isDryAdiabat() == true) {
                target.drawWireframeShape(dryAdiabatsShape,
                        NsharpConstants.dryAdiabatColor, 1, LineStyle.SOLID,
                        smallFont);
            }
            if (graphConfigProperty.isMoistAdiabat() == true) {
                target.drawWireframeShape(moistAdiabatsShape,
                        NsharpConstants.moistAdiabatColor, 1, LineStyle.DOTTED,
                        smallFont);
            }
        } else {
            target.drawWireframeShape(dryAdiabatsShape,
                    NsharpConstants.dryAdiabatColor, 1, LineStyle.SOLID,
                    smallFont);
        }

        target.drawWireframeShape(temperatureLineShape,
                NsharpConstants.temperatureColor, 1, LineStyle.DOTS, smallFont);
        target.drawRect(pe, NsharpConstants.backgroundColor, 1.0f, 1.0f);
        target.drawStrings(temperatureLabels);
        drawPressureLineNumber(target);
        target.clearClippingPlane();

    }


    @Override
    protected NsharpWGraphics computeWorld() {
        return world;
    }

    public void setGraphConfigProperty(
            NsharpGraphProperty graphConfigProperty) {
        this.graphConfigProperty = graphConfigProperty;
        createTempNumberAndLineShape();
    }

    @Override
    public double getViewableMaxPressure() {
        IExtent ext = desc.getRenderableDisplay().getExtent();
        double ymax = ext.getMaxY();
        double xmin = ext.getMinX();
        Coordinate c = NsharpWxMath.reverseSkewTXY(world.unMap(xmin, ymax));
        return c.y;
    }

    @Override
    public double getViewableMinPressure() {
        IExtent ext = desc.getRenderableDisplay().getExtent();
        double ymin = ext.getMinY();
        double xmin = ext.getMinX();
        Coordinate c = NsharpWxMath.reverseSkewTXY(world.unMap(xmin, ymin));
        return c.y;
    }

    @Override
    public double getYPositionRatioByPressure(double pressure) {
        Coordinate coorS = NsharpWxMath.getSkewTXY(pressure, 0);
        double rtP = world.mapY(coorS.y);
        coorS = NsharpWxMath.getSkewTXY(getViewableMaxPressure(), 0);
        double maxP = world.mapY(coorS.y);
        coorS = NsharpWxMath.getSkewTXY(getViewableMinPressure(), 0);
        double minP = world.mapY(coorS.y);
        double ratio = (rtP - minP) / (maxP - minP);
        return ratio;
    }

    @Override
    public ViewablePressureContainer getViewablePressureContainer(
            List<NcSoundingLayer> soundingLys) {
        ViewablePressureContainer vpc = new ViewablePressureContainer();
        IExtent ext = desc.getRenderableDisplay().getExtent();
        float ymax = (float) ext.getMaxY();
        float xmin = (float) ext.getMinX();
        float ymin = (float) ext.getMinY();
        Coordinate cmax = NsharpWxMath.reverseSkewTXY(world.unMap(xmin, ymax));
        Coordinate cmin = NsharpWxMath.reverseSkewTXY(world.unMap(xmin, ymin));
        vpc.maxVIewablePressure = (float) cmax.y;
        vpc.minVIewablePressure = (float) cmin.y;
        double maxP = world.mapY(NsharpWxMath.getSkewTXY(cmax.y, 0).y);
        double minP = world.mapY(NsharpWxMath.getSkewTXY(cmin.y, 0).y);
        for (NcSoundingLayer ly : soundingLys) {
            float pressure = ly.getPressure();
            if (pressure >= cmin.y && pressure <= cmax.y) {
                Coordinate coorS = NsharpWxMath.getSkewTXY(pressure, 0);
                double rtP = world.mapY(coorS.y);
                float ratio = (float) (rtP - minP) / (float) (maxP - minP);
                vpc.pyMap.put(pressure, ratio);
            }
        }
        return vpc;
    }

    @Override
    public ViewablePressureContainer getViewablePressureLinesContainer() {
        ViewablePressureContainer vpc = new ViewablePressureContainer();
        IExtent ext = desc.getRenderableDisplay().getExtent();
        float ymax = (float) ext.getMaxY();
        float xmin = (float) ext.getMinX();
        float ymin = (float) ext.getMinY();
        Coordinate cmax = NsharpWxMath.reverseSkewTXY(world.unMap(xmin, ymax));
        Coordinate cmin = NsharpWxMath.reverseSkewTXY(world.unMap(xmin, ymin));
        vpc.maxVIewablePressure = (float) cmax.y;
        vpc.minVIewablePressure = (float) cmin.y;
        double maxP = world.mapY(NsharpWxMath.getSkewTXY(cmax.y, 0).y);
        double minP = world.mapY(NsharpWxMath.getSkewTXY(cmin.y, 0).y);
        for (int i = 0; i < NsharpConstants.PRESSURE_NUMBERING_LEVELS.length; i++) {
            float pressure = NsharpConstants.PRESSURE_NUMBERING_LEVELS[i];
            if (pressure >= cmin.y && pressure <= cmax.y) {
                Coordinate coorS = NsharpWxMath.getSkewTXY(pressure, 0);
                double rtP = world.mapY(coorS.y);
                float ratio = (float) (rtP - minP) / (float) (maxP - minP);
                vpc.pyMap.put(pressure, ratio);
                // System.out.println("skewT press="+pressure+" ratio="+ratio+
                // "rtP="+rtP+" maxP="+maxP+" minP="+minP);
            }
        }
        return vpc;
    }

    /*
     * Called from handleResize() in skewTPaneResource only
     */
    public void handleResize(IExtent ext) {
        // IExtent ext = desc.getRenderableDisplay().getExtent();
        // ext.reset();
        if (mixRatioShape != null) {
            mixRatioShape.dispose();
        }
        if (dryAdiabatsShape != null) {
            dryAdiabatsShape.dispose();
        }
        if (moistAdiabatsShape != null) {
            moistAdiabatsShape.dispose();
        }
        if (temperatureLineShape != null) {
            temperatureLineShape.dispose();
            temperatureLineShape = null;
        }
        if (presslinesNumbersShape != null) {
            presslinesNumbersShape.dispose();
        }
        this.rectangle = new Rectangle((int) ext.getMinX(), (int) ext.getMinY(),
                (int) ext.getWidth(), (int) ext.getHeight());
        pe = new PixelExtent(this.rectangle);
        world = new NsharpWGraphics(this.rectangle);

        world.setWorldCoordinates(NsharpConstants.left, NsharpConstants.top,
                NsharpConstants.right, NsharpConstants.bottom);
        float prevWidth = skewtWidth;
        skewtWidth = (int) (ext.getWidth());
        xRatio = xRatio * skewtWidth / prevWidth;

        createMoistAdiabatsShape();
        createDryAdiabatsShape();
        createMixingRatioShape();
        // createPressureLineNumberShape();
        createTempNumberAndLineShape();
    }

    public void handleZooming() {
        if (presslinesNumbersShape != null) {
            presslinesNumbersShape.dispose();
        }
        if (mixRatioShape != null) {
            mixRatioShape.dispose();
        }
        createTempNumberAndLineShape();
        createMixingRatioShape();
    }

    public double getWindBarbXPosition() {
        IExtent ext = desc.getRenderableDisplay().getExtent();
        double xmax = ext.getMaxX(); // Extent's viewable envelope min x and y
        double ymax = ext.getMaxY();
        double pX = world.mapX(NsharpConstants.right);
        if (pX < xmax) {
            xmax = pX;
        }
        double windBarbSizfactor = graphConfigProperty.getWindBarbSize() / 1.6f;
        if (windBarbSizfactor < 1) {
            windBarbSizfactor = 1;
        }
        double dispX = xmax - ext.getWidth() / 30;
        Coordinate cumap = world.unMap(dispX, ymax);

        return cumap.x;
    }
}
