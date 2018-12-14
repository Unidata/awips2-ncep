package gov.noaa.nws.ncep.ui.nsharp.display.rsc;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.RGB;

import com.raytheon.uf.common.sounding.WxMath;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.HorizontalAlignment;
import com.raytheon.uf.viz.core.IGraphicsTarget.PointStyle;
import com.raytheon.uf.viz.core.IGraphicsTarget.TextStyle;
import com.raytheon.uf.viz.core.IGraphicsTarget.VerticalAlignment;
import com.raytheon.uf.viz.core.PixelExtent;
import com.raytheon.uf.viz.core.drawables.IFont;
import com.raytheon.uf.viz.core.drawables.IWireframeShape;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.AbstractResourceData;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.vividsolutions.jts.geom.Coordinate;

/**
 *
 *
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date         Ticket#     Engineer    Description
 * -------      -------     --------    -----------
 * 04/23/2012   229         Chin Chen   Initial coding
 * 01/27/2015   DR#17006,
 *              Task#5929   Chin Chen   NSHARP freezes when loading a sounding from MDCRS products
 *                                      in Volume Browser
 * 07/05/2016   RM#15923    Chin Chen   NSHARP - Native Code replacement
 * May, 5, 2018 49896       mgamazaychikov  Reconciled with RODO 5070, fixed formatting
 * Nov 21, 2018 7574        bsteffen    Fix comparison coloring.
 * Dec 14, 2018 6872        bsteffen    Handle missing bunkers.
 *
 * </pre>
 *
 * @author Chin Chen
 * @version 1.0
 */

import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibBasics;
import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibSndglib;
import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibWinds;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.WindComponent;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;
import gov.noaa.nws.ncep.ui.nsharp.NsharpConstants;
import gov.noaa.nws.ncep.ui.nsharp.NsharpGraphProperty;
import gov.noaa.nws.ncep.ui.nsharp.NsharpShapeAndLineProperty;
import gov.noaa.nws.ncep.ui.nsharp.NsharpSoundingElementStateProperty;
import gov.noaa.nws.ncep.ui.nsharp.NsharpWGraphics;
import gov.noaa.nws.ncep.ui.nsharp.background.NsharpHodoPaneBackground;
import gov.noaa.nws.ncep.ui.nsharp.display.NsharpHodoPaneDescriptor;

public class NsharpHodoPaneResource extends NsharpAbstractPaneResource {

    private NsharpHodoPaneBackground hodoBackground = null;

    private String sWindSpeed = "";

    private String sWindDirection = "";

    private List<NsharpShapeAndLineProperty> hodoWindRscShapeList = new ArrayList<>();

    private IWireframeShape hodoWindMotionBoxShape = null;

    private boolean cursorInHodo = false;

    private Integer markerWidth = 1;

    private IFont fixedFont;

    private int hodoWidth = NsharpConstants.HODO_WIDTH;

    private int hodoHeight = NsharpConstants.HODO_HEIGHT;

    private float xRatio = 1;

    private float yRatio = 1;

    public NsharpHodoPaneResource(AbstractResourceData resourceData, LoadProperties loadProperties,
            NsharpHodoPaneDescriptor desc) {
        super(resourceData, loadProperties, desc);

        hodoBackground = new NsharpHodoPaneBackground((NsharpHodoPaneDescriptor) descriptor);
    }

    private void createRscHodoWindShape(NsharpWGraphics world, List<NcSoundingLayer> soundingLays, RGB incolor) {

        Coordinate c0 = null;
        Coordinate c1;
        NsharpShapeAndLineProperty shNcolor;
        IWireframeShape shapeR = null, shapeG = null, shapeY = null, shapeC = null, shapeV = null, shapeIn = null;
        if (incolor == null) {
            // creating regular Hodo shape with 5 colors
            shNcolor = new NsharpShapeAndLineProperty();
            shapeR = target.createWireframeShape(false, descriptor);
            shNcolor.setShape(shapeR);
            shapeR.allocate(soundingLays.size() * 2);
            shNcolor.getLp().setLineColor(NsharpConstants.color_red);
            hodoWindRscShapeList.add(shNcolor);
            shNcolor = new NsharpShapeAndLineProperty();
            shapeG = target.createWireframeShape(false, descriptor);
            shNcolor.setShape(shapeG);
            shapeG.allocate(soundingLays.size() * 2);
            shNcolor.getLp().setLineColor(NsharpConstants.color_green);
            hodoWindRscShapeList.add(shNcolor);
            shNcolor = new NsharpShapeAndLineProperty();
            shapeY = target.createWireframeShape(false, descriptor);
            shNcolor.setShape(shapeY);
            shapeY.allocate(soundingLays.size() * 2);
            shNcolor.getLp().setLineColor(NsharpConstants.color_yellow);
            hodoWindRscShapeList.add(shNcolor);
            shNcolor = new NsharpShapeAndLineProperty();
            shapeC = target.createWireframeShape(false, descriptor);
            shNcolor.setShape(shapeC);
            shapeC.allocate(soundingLays.size() * 2);
            shNcolor.getLp().setLineColor(NsharpConstants.color_cyan);
            hodoWindRscShapeList.add(shNcolor);
            shNcolor = new NsharpShapeAndLineProperty();
            shapeV = target.createWireframeShape(false, descriptor);
            shNcolor.setShape(shapeV);
            shapeV.allocate(soundingLays.size() * 2);
            shNcolor.getLp().setLineColor(NsharpConstants.color_violet);
            hodoWindRscShapeList.add(shNcolor);
        } else {
            shNcolor = new NsharpShapeAndLineProperty();
            shapeIn = target.createWireframeShape(false, descriptor);
            shNcolor.setShape(shapeIn);
            shapeIn.allocate(soundingLays.size() * 2);
            shNcolor.getLp().setLineColor(incolor);
            hodoWindRscShapeList.add(shNcolor);
        }

        float surfaceLevel = soundingLays.get(0).getGeoHeight();
        for (NcSoundingLayer layer : soundingLays) {
            if (layer.getPressure() < 100 || layer.getWindSpeed() < 0) {
                continue;
            }
            float wspd = layer.getWindSpeed();
            float wdir = layer.getWindDirection();
            c1 = NsharpLibWinds.uvComp(wspd, wdir);
            if (c0 != null) {
                double[][] lines = { { world.mapX(c0.x), world.mapY(c0.y) }, { world.mapX(c1.x), world.mapY(c1.y) } };
                if (incolor == null) {
                    // use MSL here, so Converts height from (meters) AGL to
                    // MSL.
                    if (layer.getGeoHeight() < (3000 + surfaceLevel)) {
                        shapeR.addLineSegment(lines);
                    } else if (layer.getGeoHeight() < (6000 + surfaceLevel)) {
                        shapeG.addLineSegment(lines);
                    } else if (layer.getGeoHeight() < (9000 + surfaceLevel)) {
                        shapeY.addLineSegment(lines);
                    } else if (layer.getGeoHeight() < (12000 + surfaceLevel)) {
                        shapeC.addLineSegment(lines);
                    } else {
                        shapeV.addLineSegment(lines);
                    }
                } else {
                    shapeIn.addLineSegment(lines);
                }
            }

            c0 = c1;
        }
        if (incolor == null) {
            shapeR.compile();
            shapeG.compile();
            shapeY.compile();
            shapeV.compile();
            shapeC.compile();
        } else {
            shapeIn.compile();
        }

    }

    public void createRscHodoWindShapeAll() {
        if (target == null || rscHandler == null || soundingLys == null || hodoWindRscShapeList == null) {
            return;
        }
        if (hodoWindRscShapeList.size() > 0) {
            for (NsharpShapeAndLineProperty shapeColor : hodoWindRscShapeList) {
                shapeColor.getShape().dispose();
            }
            hodoWindRscShapeList.clear();
        }

        world = hodoBackground.computeWorld();

        if(rscHandler.isAnyCompareOn()){
            for(NsharpSoundingElementStateProperty prop : rscHandler.getComparisonProperties()){
                List<NcSoundingLayer> soundingLayeys = prop.getSndLyLst();
                int colorIndex = prop.getCompColorIndex();
                RGB color = linePropertyMap
                        .get(NsharpConstants.lineNameArray[colorIndex])
                        .getLineColor();
                createRscHodoWindShape(world, soundingLayeys, color);
            }
        } else if (rscHandler.isOverlayIsOn()) {
            List<NcSoundingLayer> previousSoundingLys = rscHandler.getPreviousSoundingLys();
            createRscHodoWindShape(world, this.soundingLys,
                    linePropertyMap.get(NsharpConstants.lineNameArray[NsharpConstants.LINE_OVERLAY1]).getLineColor());
            if (previousSoundingLys != null && !soundingLys.equals(previousSoundingLys)) {
                createRscHodoWindShape(world, previousSoundingLys, linePropertyMap
                        .get(NsharpConstants.lineNameArray[NsharpConstants.LINE_OVERLAY2]).getLineColor());
            }
        } else {
            createRscHodoWindShape(world, this.soundingLys, null);
        }

    }

    private void plotHodoEditPoints(IGraphicsTarget target, RGB color) throws VizException {

        Coordinate c1;
        for (NcSoundingLayer layer : this.soundingLys) {
            if (layer.getPressure() < 100 || layer.getWindSpeed() < 0) {
                continue;
            }
            float wspd = layer.getWindSpeed();
            float wdir = layer.getWindDirection();
            c1 = WxMath.uvComp(wspd, wdir);
            target.drawPoint(world.mapX(c1.x), world.mapY(c1.y), 0.0, color, PointStyle.CIRCLE);

        }
    }

    private void plotNsharpHodoVectors(IGraphicsTarget target, float zoomLevel) throws VizException {
        double radiusUnit = 5 * xRatio;
        Coordinate c;
        String textStr;
        /*
         * smvtype: 1: small circle, 2 large circle, 3: square
         */
        // plot Mean Wind Vector, yellow square, by default plot it
        if (((graphConfigProperty != null) && (graphConfigProperty.isMeanWind())) || (graphConfigProperty == null)) {
            NsharpWeatherDataStore.ParcelMiscParams parcelMiscParams = weatherDataStore.getParcelMiscParamsMap()
                    .get(rscHandler.getCurrentParcel());
            // the default mean wind used here is from LFC to El according
            // to original mean wind computation at bigsharp
            WindComponent meanWind = null;
            if (parcelMiscParams != null) {
                meanWind = parcelMiscParams.getMeanWindCompLfcToEl();
            }

            if (meanWind != null && NsharpLibBasics.qc(meanWind.getWdir()) && NsharpLibBasics.qc(meanWind.getWspd())) {
                // get wind uv component
                c = new Coordinate(meanWind.getUcomp(), meanWind.getVcomp());
                c = world.map(c);

                PixelExtent pe = new PixelExtent(c.x - radiusUnit, c.x + radiusUnit, c.y - radiusUnit,
                        c.y + radiusUnit);
                target.drawRect(pe, NsharpConstants.color_yellow, markerWidth * 2, 1.0f);
                textStr = String.format("%.0f/%.0f MW", meanWind.getWdir(), meanWind.getWspd());
                target.drawString(font10, textStr, c.x - radiusUnit, c.y + radiusUnit + 5, 0.0, TextStyle.NORMAL,
                        NsharpConstants.color_yellow, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);

            }
        }
        // plot 15/85 and/or 30/75 SMV, by default dont plot it
        if ((graphConfigProperty != null) && (graphConfigProperty.isSmv1585() || graphConfigProperty.isSmv3075())) {
            // get surface to 6 km mean wind
            WindComponent meanWind6k = weatherDataStore.stormTypeToMeanWindMap.get("SFC-6km");

            if (NsharpLibBasics.qc(meanWind6k.getWdir()) && NsharpLibBasics.qc(meanWind6k.getWspd())) {
                // Plot 30/75 Storm Motion Vector; a small red circle
                if (graphConfigProperty.isSmv3075()) {

                    float dir = (meanWind6k.getWdir() + 30.0f) % 360;
                    float spd = meanWind6k.getWspd() * 0.75f;
                    float ucomp = NsharpLibWinds.ucomp(dir, spd);
                    float vcomp = NsharpLibWinds.vcomp(dir, spd);
                    c = new Coordinate(ucomp, vcomp);
                    c = world.map(c);

                    RGB color = NsharpConstants.color_red;
                    target.drawCircle(c.x, c.y, 0, radiusUnit, color, markerWidth * 2);
                    target.drawLine(c.x - radiusUnit / 2, c.y, 0.0, c.x + radiusUnit / 2, c.y, 0.0, color, markerWidth);
                    target.drawLine(c.x, c.y - radiusUnit / 2, 0.0, c.x, c.y + radiusUnit / 2, 0.0, color, markerWidth);

                }
                // ----- Plot 15/85 Storm Motion Vector ----- small green
                // color circle
                if (graphConfigProperty.isSmv1585()) {
                    float dir = (meanWind6k.getWdir() + 15.0f) % 360;
                    float spd = meanWind6k.getWspd() * 0.85f;
                    float ucomp = NsharpLibWinds.ucomp(dir, spd);
                    float vcomp = NsharpLibWinds.vcomp(dir, spd);
                    c = new Coordinate(ucomp, vcomp);
                    c = world.map(c);

                    RGB color = NsharpConstants.color_green;
                    target.drawCircle(c.x, c.y, 0, radiusUnit, color, markerWidth * 2);
                    target.drawLine(c.x - radiusUnit / 2, c.y, 0.0, c.x + radiusUnit / 2, c.y, 0.0, color, markerWidth);
                    target.drawLine(c.x, c.y - radiusUnit / 2, 0.0, c.x, c.y + radiusUnit / 2, 0.0, color, markerWidth);

                }

            }
        }
        // plot Corfidi Vectors, color_stellblue small circles, by default Not
        // plot it
        if ((graphConfigProperty != null) && graphConfigProperty.isCorfidiV()) {

            WindComponent[] cofidiWindComp = weatherDataStore.getCofidiShearWindComp();
            // Downwind-Propagating MCS motion vector is saved in
            // cofidiWindComp[0]
            // use cofidi down shear wind here
            c = new Coordinate(cofidiWindComp[0].getUcomp(), cofidiWindComp[0].getVcomp());
            c = world.map(c);
            RGB color = NsharpConstants.color_lightblue;
            target.drawCircle(c.x, c.y, 0, radiusUnit / 2, color, markerWidth);
            textStr = String.format("DP= %.0f/%.0f", cofidiWindComp[0].getWdir(), cofidiWindComp[0].getWspd());
            target.drawString(font10, textStr, c.x, c.y, 0.0, TextStyle.NORMAL, color, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);

            // Upwind-Propagating MCS motion vector is saved in
            // cofidiWindComp[1]
            // use cofidi up shear wind here
            c = new Coordinate(cofidiWindComp[1].getUcomp(), cofidiWindComp[1].getVcomp());
            c = world.map(c);
            target.drawCircle(c.x, c.y, 0, radiusUnit / 2, color, markerWidth);
            textStr = String.format("UP= %.0f/%.0f", cofidiWindComp[1].getWdir(), cofidiWindComp[1].getWspd());
            target.drawString(font10, textStr, c.x, c.y, 0.0, TextStyle.NORMAL, color, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        // plot Bunkers Vector,by default plot them
        if ((graphConfigProperty != null) && graphConfigProperty.isSmvBunkersR()
                || graphConfigProperty.isSmvBunkersL()) {
            WindComponent[] bunkersStormMotionWindComp = weatherDataStore
                    .getBunkersStormMotionWindComp();
            WindComponent rightComp = null;
            WindComponent leftComp = null;
            if (bunkersStormMotionWindComp != null) {
                rightComp = bunkersStormMotionWindComp[0];
                leftComp = bunkersStormMotionWindComp[1];
            }
            if (graphConfigProperty.isSmvBunkersR() && rightComp != null) {
                c = new Coordinate(rightComp.getUcomp(), rightComp.getVcomp());
                c = world.map(c);
                RGB color = NsharpConstants.color_firebrick;
                target.drawCircle(c.x, c.y, 0, radiusUnit, color, markerWidth);
                target.drawLine(c.x - radiusUnit, c.y, 0.0, c.x + radiusUnit,
                        c.y, 0.0, color, markerWidth);
                target.drawLine(c.x, c.y - radiusUnit, 0.0, c.x,
                        c.y + radiusUnit, 0.0, color, markerWidth);
                textStr = String.format("%.0f/%.0f RM", rightComp.getWdir(),
                        rightComp.getWspd());
                target.drawString(font10, textStr, c.x,
                        c.y + 10 * zoomLevel * yRatio, 0.0, TextStyle.NORMAL,
                        color, HorizontalAlignment.RIGHT, VerticalAlignment.TOP,
                        null);
            }
            if (graphConfigProperty.isSmvBunkersL() && leftComp != null) {
                c = new Coordinate(leftComp.getUcomp(), leftComp.getVcomp());
                c = world.map(c);
                RGB color = NsharpConstants.color_skyblue;
                target.drawCircle(c.x, c.y, 0, radiusUnit, color, markerWidth);
                target.drawLine(c.x - radiusUnit, c.y, 0.0, c.x + radiusUnit,
                        c.y, 0.0, color, markerWidth);
                target.drawLine(c.x, c.y - radiusUnit, 0.0, c.x,
                        c.y + radiusUnit, 0.0, color, markerWidth);
                textStr = String.format("%.0f/%.0f LM", leftComp.getWdir(),
                        leftComp.getWspd());
                target.drawString(font10, textStr, c.x,
                        c.y - 10 * zoomLevel * yRatio, 0.0, TextStyle.NORMAL,
                        color, HorizontalAlignment.LEFT,
                        VerticalAlignment.BOTTOM, null);
            }
        }

        // plot current storm motion vector (mouse click) marker
        Coordinate hodoStmCenter = NsharpLibWinds.uvComp(weatherDataStore.getSmspd(), weatherDataStore.getSmdir());
        hodoStmCenter = world.map(hodoStmCenter);
        target.drawCircle(hodoStmCenter.x, hodoStmCenter.y, 0, radiusUnit, NsharpConstants.color_white, markerWidth);
        target.drawLine(hodoStmCenter.x - radiusUnit, hodoStmCenter.y, 0.0, hodoStmCenter.x + radiusUnit,
                hodoStmCenter.y, 0.0, NsharpConstants.color_white, markerWidth);
        target.drawLine(hodoStmCenter.x, hodoStmCenter.y - radiusUnit, 0.0, hodoStmCenter.x,
                hodoStmCenter.y + radiusUnit, 0.0, NsharpConstants.color_white, markerWidth);
        textStr = String.format("%.0f/%.0f", weatherDataStore.getSmdir(), weatherDataStore.getSmspd());
        target.drawString(font10, textStr, hodoStmCenter.x, hodoStmCenter.y + radiusUnit * 2, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_white, HorizontalAlignment.CENTER, VerticalAlignment.TOP, null);

        // draw lines from storm motion to top and bottom of effective layer
        float botWindU = weatherDataStore.getEffLayerBotWindUComp();
        float botWindV = weatherDataStore.getEffLayerBotWindVComp();
        if (NsharpLibBasics.qc(botWindU) && NsharpLibBasics.qc(botWindV)) {
            c = new Coordinate(botWindU, botWindV);
            c = world.map(c);
            target.drawLine(hodoStmCenter.x, hodoStmCenter.y, 0.0, c.x, c.y, 0.0, NsharpConstants.color_skyblue,
                    markerWidth);
        }
        float topWindU = weatherDataStore.getEffLayerTopWindUComp();
        float topWindV = weatherDataStore.getEffLayerTopWindVComp();
        if (NsharpLibBasics.qc(topWindU) && NsharpLibBasics.qc(topWindV)) {
            c = new Coordinate(topWindU, topWindV);
            c = world.map(c);
            target.drawLine(hodoStmCenter.x, hodoStmCenter.y, 0.0, c.x, c.y, 0.0, NsharpConstants.color_skyblue,
                    markerWidth);
        }
    }

    private void drawHodoDynamicData(IGraphicsTarget target, double zoomLevel) throws VizException {
        // draw running temp, theta, height etc data at window palette bottom
        double dispX, xmin;
        double dispY, ymin, ymax;
        // display wind direction, speed in m/s and knots
        // Line 1 - wind direction, speed
        IExtent ext = getDescriptor().getRenderableDisplay().getExtent();
        xmin = ext.getMinX(); // Extent's viewable envelope min x and y
        ymin = ext.getMinY();
        dispX = xmin + 20 * zoomLevel * xRatio;
        dispY = ymin + 40 * zoomLevel * yRatio;
        target.drawString(fixedFont, sWindDirection + "  " + sWindSpeed, dispX, dispY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_cyan, HorizontalAlignment.LEFT, VerticalAlignment.BOTTOM, null);
        // plot critical angle
        float ca = weatherDataStore.getCriticalAngle();
        if (ca != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA) {
            ymax = ext.getMaxY();
            dispY = ymax - 20 * zoomLevel;
            String textStr = String.format("Critical Angle = %.0f", ca);
            target.drawString(fixedFont, textStr, dispX, dispY, 0.0, TextStyle.NORMAL, NsharpConstants.color_cyan,
                    HorizontalAlignment.LEFT, VerticalAlignment.BOTTOM, null);
        }
    }

    @Override
    protected void paintInternal(IGraphicsTarget target, PaintProperties paintProps) throws VizException {
        super.paintInternal(target, paintProps);
        if (rscHandler == null) {
            return;
        }

        hodoBackground.paintInternal(target, paintProps);
        if ((soundingLys != null) && (soundingLys.size() > 2)) {
            this.font10.setSmoothing(false);
            this.font10.setScaleFont(false);
            this.font9.setSmoothing(false);
            this.font9.setScaleFont(false);
            this.font12.setSmoothing(false);
            this.font12.setScaleFont(false);
            fixedFont.setSmoothing(false);
            fixedFont.setScaleFont(false);
            // plot HODO
            PixelExtent extent = new PixelExtent(hodoBackground.getRectangle());
            target.setupClippingPlane(extent);
            if (((graphConfigProperty != null) && graphConfigProperty.isHodo()) || (graphConfigProperty == null)) {
                for (NsharpShapeAndLineProperty shapeNColor : hodoWindRscShapeList) {
                    target.drawWireframeShape(shapeNColor.getShape(), shapeNColor.getLp().getLineColor(),
                            commonLinewidth * 2, commonLineStyle, font10);
                }
            }
            boolean compareStnIsOn = rscHandler.isCompareStnIsOn();
            boolean editGraphOn = rscHandler.isEditGraphOn();
            if (editGraphOn && !compareStnIsOn) {
                plotHodoEditPoints(target, NsharpConstants.color_white);
            }
            if (!compareStnIsOn) {
                plotNsharpHodoVectors(target, currentZoomLevel);
            }
            target.clearClippingPlane();
            if (cursorInHodo) {
                drawHodoDynamicData(target, currentZoomLevel);
            }
        }

    }

    @Override
    protected void initInternal(IGraphicsTarget target) throws VizException {
        super.initInternal(target);
        currentCanvasBoundWidth = NsharpConstants.HODO_PANE_REC_WIDTH;
        currentCanvasBoundHeight = NsharpConstants.HODO_PANE_REC_HEIGHT;
        myDefaultCanvasWidth = NsharpConstants.HODO_PANE_REC_WIDTH;
        myDefaultCanvasHeight = NsharpConstants.HODO_PANE_REC_HEIGHT;
        hodoBackground.initInternal(target);
        createRscHodoWindShapeAll();
        IFont.Style[] style = { IFont.Style.BOLD };
        fixedFont = target.initializeFont("Monospace", 10, style);

    }

    @Override
    protected void disposeInternal() {
        if (hodoWindRscShapeList.size() > 0) {
            for (NsharpShapeAndLineProperty shapeColor : hodoWindRscShapeList) {
                shapeColor.getShape().dispose();
            }
            hodoWindRscShapeList.clear();
            hodoWindRscShapeList = null;
        }
        if (hodoBackground != null) {
            hodoBackground.disposeInternal();
            hodoBackground = null;
        }
        if (hodoWindMotionBoxShape != null) {
            hodoWindMotionBoxShape.dispose();
        }
        super.disposeInternal();
    }

    public void updateDynamicData(Coordinate c) throws VizException {

        try {
            if (hodoBackground.contains(c)) {

                c = hodoBackground.getWorld().unMap(c.x, c.y);
                c = NsharpLibWinds.speedDir((float) c.x, (float) c.y);

                sWindDirection = String.format("%.0f%c", c.y, NsharpConstants.DEGREE_SYMBOL);
                sWindSpeed = String.format("%.0f Knots (%.0f m/s)", c.x, c.x * NsharpConstants.KnotsToMetersPerSecond);
            }

        } catch (Exception e) {
            UFStatus.getHandler().handle(Priority.PROBLEM, "Exception translating coordinate", e);
        }
    }

    public void setCursorInHodo(boolean cursorInHodo) {
        this.cursorInHodo = cursorInHodo;

    }

    public NsharpHodoPaneBackground getHodoBackground() {
        return hodoBackground;
    }

    @Override
    public void handleResize() {
        this.resize = false;
        IExtent ext = getDescriptor().getRenderableDisplay().getExtent();
        ext.reset();

        float prevHeight = hodoHeight;
        float prevWidth = hodoWidth;
        hodoWidth = (int) (ext.getWidth());
        hodoHeight = (int) ext.getHeight();
        xRatio = xRatio * hodoWidth / prevWidth;
        yRatio = yRatio * hodoHeight / prevHeight;
        hodoBackground.handleResize(ext);
        world = hodoBackground.computeWorld();
        createRscHodoWindShapeAll();

    }

    @Override
    public void setGraphConfigProperty(NsharpGraphProperty graphConfigProperty) {
        super.setGraphConfigProperty(graphConfigProperty);
        hodoBackground.setPaneConfigurationName(paneConfigurationName);
    }
}
