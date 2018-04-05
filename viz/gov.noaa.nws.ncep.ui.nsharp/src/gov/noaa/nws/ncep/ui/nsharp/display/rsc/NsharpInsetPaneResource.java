package gov.noaa.nws.ncep.ui.nsharp.display.rsc;

/**
 * 
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    	Engineer    Description
 * -------		------- 	-------- 	-----------
 * 04/23/2012	229			Chin Chen	Initial coding
 * 01/27/2015   DR#17006,
 *              Task#5929   Chin Chen   NSHARP freezes when loading a sounding from MDCRS products 
 *                                      in Volume Browser
 * 07/05/2016   RM#15923    Chin Chen   NSHARP - Native Code replacement
 * 
 * </pre>
 * 
 * @author Chin Chen
 * @version 1.0
 */

import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibBasics;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.WindComponent;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;
import gov.noaa.nws.ncep.ui.nsharp.NsharpConstants;
import gov.noaa.nws.ncep.ui.nsharp.NsharpWGraphics;
import gov.noaa.nws.ncep.ui.nsharp.background.NsharpGenericPaneBackground;
import gov.noaa.nws.ncep.ui.nsharp.display.NsharpAbstractPaneDescriptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;

import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.HorizontalAlignment;
import com.raytheon.uf.viz.core.IGraphicsTarget.LineStyle;
import com.raytheon.uf.viz.core.IGraphicsTarget.TextStyle;
import com.raytheon.uf.viz.core.IGraphicsTarget.VerticalAlignment;
import com.raytheon.uf.viz.core.PixelExtent;
import com.raytheon.uf.viz.core.drawables.IWireframeShape;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.AbstractResourceData;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.capabilities.OutlineCapability;

public class NsharpInsetPaneResource extends NsharpAbstractPaneResource {
    private int currentInsetPage = 1;

    private Integer markerWidth = 1;

    private IWireframeShape thetaEPressureYRscShape = null;

    private IWireframeShape thetaEPressureWRscShape = null;

    private IWireframeShape thetaEPressureRRscShape = null;

    private IWireframeShape thetaEHeightYRscShape = null;

    private IWireframeShape thetaEHeightWRscShape = null;

    private IWireframeShape thetaEHeightRRscShape = null;

    private IWireframeShape srWindBRscShape = null;

    private IWireframeShape srWindWRscShape = null;

    private IWireframeShape srWindRRscShape = null;

    private IWireframeShape srWindGRscShape = null;

    private IWireframeShape srWindMRscShape = null;

    private IWireframeShape psblWatchTypeBkgShape = null;

    private RGB wwTypeColor = NsharpConstants.color_gold;

    private NsharpGenericPaneBackground thetaEHeightBackground;

    private NsharpGenericPaneBackground srWindsBackground;

    private NsharpGenericPaneBackground stormSlinkyBackground;

    private NsharpGenericPaneBackground srWindVectorBackground;

    private NsharpGenericPaneBackground thetaEPresureBackground;

    private NsharpGenericPaneBackground psblWatchTypeBackground;

    private int insetPaneWidth = NsharpConstants.INSET_PANE_REC_WIDTH;

    private int insetPaneHeight = NsharpConstants.INSET_PANE_REC_HEIGHT;

    private float xRatio = 1;

    private float yRatio = 1;

    private int insetWidth = NsharpConstants.INSET_WIDTH;

    private int insetHeight = NsharpConstants.INSET_HEIGHT;

    private int thetahXOrig = NsharpConstants.THETAH_X_ORIG;

    private int thetahYOrig = NsharpConstants.THETAH_Y_ORIG;

    private int thetapXOrig = NsharpConstants.THETAP_X_ORIG;

    private int thetapYOrig = NsharpConstants.THETAP_Y_ORIG;

    private int srwindsXOrig = NsharpConstants.SRWINDS_X_ORIG;

    private int srwindsYOrig = NsharpConstants.SRWINDS_Y_ORIG;

    private int stormSlinkXOrig = NsharpConstants.STORMSLINKY_X_ORIG;

    private int stormSlinkYOrig = NsharpConstants.STORMSLINKY_Y_ORIG;

    private int srwindvtrXOrig = NsharpConstants.SRWINDVTRS_X_ORIG;

    private int srwindvtrYOrig = NsharpConstants.SRWINDVTRS_Y_ORIG;

    private int psbWatchXOrig = NsharpConstants.PSBLWATCH_X_ORIG;

    private int psbWatchYOrig = NsharpConstants.PSBLWATCH_Y_ORIG;

    private HashMap<Integer, RGB> stormSlinkyColorMap = new HashMap<>();

    public NsharpInsetPaneResource(AbstractResourceData resourceData,
            LoadProperties loadProperties, NsharpAbstractPaneDescriptor desc) {
        super(resourceData, loadProperties, desc);

        thetaEHeightBackground = new NsharpGenericPaneBackground(new Rectangle(
                NsharpConstants.THETAH_X_ORIG, NsharpConstants.THETAH_Y_ORIG,
                NsharpConstants.INSET_WIDTH, NsharpConstants.INSET_HEIGHT));
        srWindsBackground = new NsharpGenericPaneBackground(new Rectangle(
                NsharpConstants.SRWINDS_X_ORIG, NsharpConstants.SRWINDS_Y_ORIG,
                NsharpConstants.INSET_WIDTH, NsharpConstants.INSET_HEIGHT));
        stormSlinkyBackground = new NsharpGenericPaneBackground(new Rectangle(
                NsharpConstants.STORMSLINKY_X_ORIG,
                NsharpConstants.STORMSLINKY_Y_ORIG,
                NsharpConstants.INSET_WIDTH, NsharpConstants.INSET_HEIGHT));
        srWindVectorBackground = new NsharpGenericPaneBackground(new Rectangle(
                NsharpConstants.SRWINDVTRS_X_ORIG,
                NsharpConstants.SRWINDVTRS_Y_ORIG, NsharpConstants.INSET_WIDTH,
                NsharpConstants.INSET_HEIGHT));
        psblWatchTypeBackground = new NsharpGenericPaneBackground(
                new Rectangle(NsharpConstants.PSBLWATCH_X_ORIG,
                        NsharpConstants.PSBLWATCH_Y_ORIG,
                        NsharpConstants.INSET_WIDTH,
                        NsharpConstants.INSET_HEIGHT));
        thetaEPresureBackground = new NsharpGenericPaneBackground(
                new Rectangle(NsharpConstants.THETAP_X_ORIG,
                        NsharpConstants.THETAP_Y_ORIG,
                        NsharpConstants.INSET_WIDTH,
                        NsharpConstants.INSET_HEIGHT));
        // based on BigNsharp storm slinky color used and gempak color
        // definition
        stormSlinkyColorMap.put(new Integer(3), NsharpConstants.color_green);
        stormSlinkyColorMap.put(new Integer(7), NsharpConstants.color_magenta);
        stormSlinkyColorMap.put(new Integer(6), NsharpConstants.color_cyan);
        stormSlinkyColorMap.put(new Integer(13),
                NsharpConstants.color_violet_md);
        stormSlinkyColorMap.put(new Integer(20),
                NsharpConstants.color_yellow_DK);
        stormSlinkyColorMap.put(new Integer(27), NsharpConstants.color_cyan_md);
    }

    @Override
    protected synchronized void initInternal(IGraphicsTarget target)
            throws VizException {
        super.initInternal(target);
        currentCanvasBoundWidth = NsharpConstants.INSET_PANE_REC_WIDTH;
        currentCanvasBoundHeight = NsharpConstants.INSET_PANE_REC_HEIGHT;
        myDefaultCanvasWidth = NsharpConstants.INSET_PANE_REC_WIDTH;
        myDefaultCanvasHeight = NsharpConstants.INSET_PANE_REC_HEIGHT;
        thetaEHeightBackground.initInternal(target);
        stormSlinkyBackground.initInternal(target);
        srWindsBackground.initInternal(target);
        srWindVectorBackground.initInternal(target);
        psblWatchTypeBackground.initInternal(target);
        thetaEPresureBackground.initInternal(target);
        createInsetWireFrameShapes();
    }

    @Override
    protected void disposeInternal() {
        disposeInsetWireFrameShapes();
        if (thetaEPresureBackground != null) {
            thetaEPresureBackground.disposeInternal();
            thetaEPresureBackground = null;
        }
        if (psblWatchTypeBackground != null) {
            psblWatchTypeBackground.disposeInternal();
            psblWatchTypeBackground = null;
        }
        if (srWindVectorBackground != null) {
            srWindVectorBackground.disposeInternal();
            srWindVectorBackground = null;
        }
        if (srWindsBackground != null) {
            srWindsBackground.disposeInternal();
            srWindsBackground = null;
        }
        if (stormSlinkyBackground != null) {
            stormSlinkyBackground.disposeInternal();
            stormSlinkyBackground = null;
        }
        if (thetaEPresureBackground != null) {
            thetaEPresureBackground.disposeInternal();
            thetaEPresureBackground = null;
        }
        super.disposeInternal();
    }

    @Override
    protected void paintInternal(IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {
        super.paintInternal(target, paintProps);
        if (rscHandler == null)
            return;
        float zoomLevel = paintProps.getZoomLevel();
        if (zoomLevel != currentZoomLevel) {
            magnifyFont(zoomLevel);
        }
        thetaEHeightBackground.paint(target, paintProps);
        srWindsBackground.paint(target, paintProps);
        stormSlinkyBackground.paint(target, paintProps);
        srWindVectorBackground.paint(target, paintProps);
        // Since we only display 4 insets and their backgrounds have same
        // size, only draws 4 backgrounds.
        if ((soundingLys != null) && rscHandler.isGoodData()) {
            this.font10.setSmoothing(false);
            this.font10.setScaleFont(false);
            this.font12.setSmoothing(false);
            this.font12.setScaleFont(false);
            // plot SRWinds- currently always on page 1 and page 2
            PixelExtent extent = new PixelExtent(
                    srWindsBackground.getRectangle());
            target.setupClippingPlane(extent);
            target.drawWireframeShape(srWindBRscShape,
                    NsharpConstants.color_cyan, commonLinewidth,
                    LineStyle.SOLID, font10);
            target.drawWireframeShape(srWindWRscShape,
                    NsharpConstants.color_white, commonLinewidth,
                    LineStyle.SOLID, font12);
            target.drawWireframeShape(srWindRRscShape,
                    NsharpConstants.color_red, commonLinewidth,
                    LineStyle.SOLID, font10);
            target.drawWireframeShape(srWindGRscShape,
                    NsharpConstants.color_green, commonLinewidth,
                    LineStyle.SOLID, font10);
            target.drawWireframeShape(srWindMRscShape,
                    NsharpConstants.color_magenta, commonLinewidth,
                    LineStyle.SOLID, font10);
            target.clearClippingPlane();
            // plot storm slinky - currently always on page 1 and page 2
            NsharpWGraphics plotWorld = stormSlinkyBackground.getWorld();
            plotNsharpStormSlinky(target, currentZoomLevel, plotWorld,
                    stormSlinkyBackground.getRectangle());
            currentInsetPage = rscHandler.getCurrentInsetPage();
            if (currentInsetPage == 1) {
                // plot ThetaE-Pressure
                extent = new PixelExtent(thetaEPresureBackground.getRectangle());
                target.setupClippingPlane(extent);
                target.drawWireframeShape(thetaEPressureYRscShape,
                        NsharpConstants.color_yellow, commonLinewidth,
                        LineStyle.SOLID, font10);
                target.drawWireframeShape(thetaEPressureWRscShape,
                        NsharpConstants.color_white, commonLinewidth,
                        LineStyle.SOLID, font10);
                target.drawWireframeShape(thetaEPressureRRscShape,
                        NsharpConstants.color_red, commonLinewidth,
                        LineStyle.SOLID, font10);
                target.clearClippingPlane();
                // plot possibleWatchType
                NsharpWGraphics WGc = psblWatchTypeBackground.getWorld();
                createBkgPsblWatchShape(WGc);
                extent = new PixelExtent(psblWatchTypeBackground.getRectangle());
                target.setupClippingPlane(extent);
                target.drawWireframeShape(psblWatchTypeBkgShape, wwTypeColor,
                        commonLinewidth, LineStyle.SOLID, font12);
                target.clearClippingPlane();

            } else if (currentInsetPage == 2) {
                // for used by other panel to get wwtpye color
                decideWWType();
                // plot ThetaE-Height
                extent = new PixelExtent(thetaEHeightBackground.getRectangle());
                target.setupClippingPlane(extent);
                target.drawWireframeShape(thetaEHeightYRscShape,
                        NsharpConstants.color_yellow, commonLinewidth,
                        LineStyle.SOLID, font10);
                target.drawWireframeShape(thetaEHeightWRscShape,
                        NsharpConstants.color_white, commonLinewidth,
                        LineStyle.SOLID, font10);
                target.drawWireframeShape(thetaEHeightRRscShape,
                        NsharpConstants.color_red, commonLinewidth,
                        LineStyle.SOLID, font10);
                target.clearClippingPlane();
                // plot SR Wind Vectors
                plotWorld = srWindVectorBackground.getWorld();
                plotNsharpSRWindVectors(target, currentZoomLevel, plotWorld,
                        srWindVectorBackground.getRectangle());
            }

        }

    }

    private void disposeInsetWireFrameShapes() {
        if (thetaEPressureYRscShape != null) {
            thetaEPressureYRscShape.dispose();
            thetaEPressureYRscShape = null;
        }
        if (thetaEPressureRRscShape != null) {
            thetaEPressureRRscShape.dispose();
            thetaEPressureRRscShape = null;
        }
        if (thetaEPressureWRscShape != null) {
            thetaEPressureWRscShape.dispose();
            thetaEPressureWRscShape = null;
        }
        if (thetaEHeightYRscShape != null) {
            thetaEHeightYRscShape.dispose();
            thetaEHeightYRscShape = null;
        }
        if (thetaEHeightWRscShape != null) {
            thetaEHeightWRscShape.dispose();
            thetaEHeightWRscShape = null;
        }
        if (thetaEHeightRRscShape != null) {
            thetaEHeightRRscShape.dispose();
            thetaEHeightRRscShape = null;
        }
        if (srWindBRscShape != null) {
            srWindBRscShape.dispose();
            srWindBRscShape = null;
        }
        if (srWindWRscShape != null) {
            srWindWRscShape.dispose();
            srWindWRscShape = null;
        }
        if (srWindRRscShape != null) {
            srWindRRscShape.dispose();
            srWindRRscShape = null;
        }
        if (srWindGRscShape != null) {
            srWindGRscShape.dispose();
            srWindGRscShape = null;
        }
        if (srWindMRscShape != null) {
            srWindMRscShape.dispose();
            srWindMRscShape = null;
        }
        if (psblWatchTypeBkgShape != null) {
            psblWatchTypeBkgShape.dispose();
            psblWatchTypeBkgShape = null;
        }
    }

    public void createInsetWireFrameShapes() {
        if (target == null)
            return;
        disposeInsetWireFrameShapes();
        if (soundingLys != null && rscHandler.isGoodData()) {

            NsharpWGraphics WGc = thetaEPresureBackground.getWorld();
            createRscThetaEPressureShape(WGc);
            WGc = thetaEHeightBackground.getWorld();
            createRscThetaEHeightShape(WGc);
            WGc = srWindsBackground.getWorld();
            createRscSrWindShape(WGc);
        }
    }

    private String decideWWType() {
        String wwtypeStr;
        if (soundingLys == null) {
            wwtypeStr = "NONE";
            wwTypeColor = NsharpConstants.color_gold;
        } else {
            // See NsharpWeatherDataStore for returned wwtype
            // definitions
            wwTypeColor = weatherDataStore.getWwTypeColor();
            wwtypeStr = weatherDataStore.getWwtypeStr();
        }
        return wwtypeStr;
    }

    public void createBkgPsblWatchShape(NsharpWGraphics world) {
        String wwtypeStr;

        double dispX, dispY;
        /* ----- Plot Label ----- */
        dispX = world.getViewXmin() + 100 * xRatio;
        dispY = world.getViewYmin() + 15 * yRatio;

        if (psblWatchTypeBkgShape != null) {
            psblWatchTypeBkgShape.dispose();
            psblWatchTypeBkgShape = null;
        }
        psblWatchTypeBkgShape = target.createWireframeShape(false, descriptor);
        psblWatchTypeBkgShape.allocate(4);
        double[] lblXy = { dispX, dispY };
        psblWatchTypeBkgShape.addLabel("Psbl Watch Type", lblXy);
        double[] lblXy1 = { dispX, dispY + 75 * yRatio };
        wwtypeStr = decideWWType();

        psblWatchTypeBkgShape.addLabel(wwtypeStr, lblXy1);
        double[][] lines = { { world.getViewXmin(), dispY + 30 * yRatio },
                { world.getViewXmax(), dispY + 30 * yRatio } };
        psblWatchTypeBkgShape.addLineSegment(lines);
        psblWatchTypeBkgShape.compile();
    }

    public void createRscSrWindShape(NsharpWGraphics world) {
        /*
         * This plotting function is based on the algorithm of plot_vertsrw() at
         * xwvid5.c of native nsharp source code
         */
        double dispX, dispY, dispX1, dispY1;
        double xScaleAdjustRatio, yScaleAdjustRatio;
        double bothgt, tophgt;
        double vyMax = world.getViewYmax();
        double vyMin = world.getViewYmin();
        double vxMax = world.getViewXmax();
        double vxMin = world.getViewXmin();

        /* ----- Set Layer (AGL) ----- */

        if (srWindBRscShape != null)
            srWindBRscShape.dispose();
        if (srWindWRscShape != null)
            srWindWRscShape.dispose();
        if (srWindRRscShape != null)
            srWindRRscShape.dispose();
        if (srWindGRscShape != null)
            srWindGRscShape.dispose();
        if (srWindMRscShape != null)
            srWindMRscShape.dispose();
        srWindBRscShape = target.createWireframeShape(false, descriptor);
        srWindBRscShape.allocate(2);
        srWindRRscShape = target.createWireframeShape(false, descriptor);
        srWindRRscShape.allocate(250);
        srWindWRscShape = target.createWireframeShape(false, descriptor);
        srWindWRscShape.allocate(250);
        srWindGRscShape = target.createWireframeShape(false, descriptor);
        srWindGRscShape.allocate(2);
        srWindMRscShape = target.createWireframeShape(false, descriptor);
        srWindMRscShape.allocate(250);
        /* ----- Plot Label ----- */
        dispX = vxMin + 25 * xRatio;
        dispY = vyMin + 15 * yRatio;
        double[] lblXy = { vxMin + (vxMax - vxMin) / 2, dispY };
        srWindWRscShape.addLabel("SR Winds (kt) vs Height", lblXy);

        /* ----- Plot height legend ----- */
        int yNum = 0;
        for (double h = 0; h <= vyMax - vyMin; h += (vyMax - vyMin) / 8) {
            dispY = vyMax - h;
            dispX = vxMin;
            double[][] lines = { { dispX, dispY },
                    { dispX + 10 * xRatio, dispY } };
            srWindWRscShape.addLineSegment(lines);
            if (h > 0 && h < vyMax - vyMin) {
                double[] lblXy1 = { dispX + 10 * xRatio, dispY };
                srWindWRscShape.addLabel(String.valueOf(yNum), lblXy1);
            }
            // 2 = 2000
            yNum = yNum + 2;
        }

        /* ----- Plot horizontal legend ----- */
        int k = 0;
        for (float h = 0; h <= vxMax - vxMin; h += (vxMax - vxMin) / 8) {
            // each segment is scale of 10Kt in BigNsharp
            // therefore, totoal x axis is 80 kt
            dispY = vyMax;
            dispX = vxMin + h;
            double[][] lines = { { dispX, dispY },
                    { dispX, dispY - 10 * yRatio } };
            srWindWRscShape.addLineSegment(lines);
            if (h > 0 && h < vxMax - vxMin) {
                double[] lblXy1 = { dispX, dispY - 10 * yRatio };
                k = k + 10;
                srWindWRscShape.addLabel(String.valueOf(k), lblXy1);
            }
        }
        /* ----- Plot vertical dashed line at 15kt ----- */
        xScaleAdjustRatio = (vxMax - vxMin) / 80;
        {
            dispX = vxMin + (xScaleAdjustRatio * 15);
            for (double i = 0; i < (vyMax - vyMin); i = i + 10 * yRatio) {
                double[][] lines1 = { { dispX, vyMin + i },
                        { dispX, vyMin + (i + 5 * yRatio) } };
                srWindWRscShape.addLineSegment(lines1);
            }
        }
        /* ----- Plot 1/2 vertical dashed line at 40kt ----- */
        {
            dispX = vxMin + (xScaleAdjustRatio * 40);
            for (double i = 0; i < (vyMax - vyMin) / 2; i = i + 10 * yRatio) {
                double[][] lines1 = { { dispX, vyMin + i },
                        { dispX, vyMin + (i + 5 * yRatio) } };
                srWindMRscShape.addLineSegment(lines1);
            }

        }
        /* ----- Plot 1/2 vertical dashed line at 70kt ----- */
        {
            dispX = vxMin + (xScaleAdjustRatio * 70);
            for (double i = 0; i < (vyMax - vyMin) / 2; i = i + 10 * yRatio) {
                double[][] lines1 = { { dispX, vyMin + i },
                        { dispX, vyMin + (i + 5 * yRatio) } };
                srWindMRscShape.addLineSegment(lines1);
            }

        }
        /* ----- Plot vertical srw profile ----- */
        bothgt = 0;
        tophgt = 16000;
        dispX1 = dispY1 = -999;
        yScaleAdjustRatio = (vyMax - vyMin) / tophgt;
        Map<Float, Float> verticalSrWIndMap = weatherDataStore
                .getVerticalSrWindMap();
        for (float h = (float) bothgt; h <= tophgt; h += 250) {
            Float swWindSpeed = verticalSrWIndMap.get(h);
            if (swWindSpeed == null) {
                continue;
            }
            dispX = vxMin + swWindSpeed * xScaleAdjustRatio;
            dispY = vyMax - (yScaleAdjustRatio * h);
            if (dispX1 == -999) {
                dispX1 = dispX;
                dispY1 = dispY;
            }
            double[][] lines1 = { { dispX, dispY }, { dispX1, dispY1 } };
            srWindRRscShape.addLineSegment(lines1);
            dispX1 = dispX;
            dispY1 = dispY;
        }
        /* ----- Plot Mean-Layer SRW value (Sfc-2km) ----- */
        float sfcTo2kmSrWIndSpd = weatherDataStore
                .getStormTypeToSrMeanWindMap().get("SFC-2km").getWspd();
        if (NsharpLibBasics.qc(sfcTo2kmSrWIndSpd)) {
            dispX = vxMin + sfcTo2kmSrWIndSpd * xScaleAdjustRatio;
            dispY = vyMax - (yScaleAdjustRatio * 0);
            dispY1 = vyMax - (yScaleAdjustRatio * 2000);
            double[][] lines1 = { { dispX, dispY }, { dispX, dispY1 } };
            srWindGRscShape.addLineSegment(lines1);
        }
        /* ----- Plot Mean-Layer SRW value (4-6km) ----- */
        float fourTo6kmSrWIndSpd = weatherDataStore.getSrMeanWindComp4To6km()
                .getWspd();
        if (NsharpLibBasics.qc(fourTo6kmSrWIndSpd)) {
            dispX = vxMin + fourTo6kmSrWIndSpd * xScaleAdjustRatio;
            dispY = vyMax - (yScaleAdjustRatio * 4000);
            dispY1 = vyMax - (yScaleAdjustRatio * 6000);
            double[][] lines1 = { { dispX, dispY }, { dispX, dispY1 } };
            srWindBRscShape.addLineSegment(lines1);
        }
        /* ----- Plot Mean-Layer SRW value (9-11km) ----- */
        float nineTo11kmSrWIndSpd = weatherDataStore.getSrMeanWindComp9To11km()
                .getWspd();
        if (NsharpLibBasics.qc(nineTo11kmSrWIndSpd)) {
            dispX = vxMin + nineTo11kmSrWIndSpd * xScaleAdjustRatio;
            dispY = vyMax - (yScaleAdjustRatio * 9000);
            dispY1 = vyMax - (yScaleAdjustRatio * 11000);
            double[][] lines1 = { { dispX, dispY }, { dispX, dispY1 } };
            srWindMRscShape.addLineSegment(lines1);
        }
        srWindBRscShape.compile();
        srWindWRscShape.compile();
        srWindRRscShape.compile();
        srWindGRscShape.compile();
        srWindMRscShape.compile();

    }

    private void createRscThetaEHeightShape(NsharpWGraphics world) {
        /*
         * This plotting function is based on the algorithm of plot_thetae() at
         * xwvid5.c of native nsharp source code
         */
        thetaEHeightYRscShape = target.createWireframeShape(false, descriptor);
        thetaEHeightYRscShape.allocate(2);
        thetaEHeightWRscShape = target.createWireframeShape(false, descriptor);
        thetaEHeightWRscShape.allocate(100);
        thetaEHeightRRscShape = target.createWireframeShape(false, descriptor);
        thetaEHeightRRscShape.allocate(soundingLys.size() * 2);
        double dispX, dispY, dispX1, dispY1;
        double xAxisCenter, xScaleAdjustRatio, thetaEDispWidth;
        double yScaleAdjustRatio;
        float cthe, nct, tophgt;
        double vyMax = world.getViewYmax();
        double vyMin = world.getViewYmin();
        double vxMax = world.getViewXmax();
        double vxMin = world.getViewXmin();
        // add a virtual line to make wire fram shape works when only string in
        // shape
        double[][] lines = { { 0, 0 }, { 0, 0 } };
        thetaEHeightYRscShape.addLineSegment(lines);

        /* ----- Set Layer (AGL) ----- */
        // set max height to at pressure 500
        tophgt = weatherDataStore.getAgl500mb();
        if (!NsharpLibBasics.qc(tophgt)) {
            tophgt = 5500.0F;
        }
        yScaleAdjustRatio = (vyMax - vyMin) / tophgt;
        // ----- Plot Label -----
        dispX = vxMin + 50 * xRatio;
        dispY = vyMin + 30 * yRatio;
        double[] lblXy = { dispX + 70 * xRatio, dispY };
        thetaEHeightYRscShape.addLabel("Theta-E vs Height", lblXy);
        // ----- Plot horizontal legend -----
        cthe = 0.0F;
        nct = 0.0F;
        if (NsharpLibBasics.qc(weatherDataStore.getThetaE850())) {
            cthe = cthe + weatherDataStore.getThetaE850();
            nct++;
        }
        if (NsharpLibBasics.qc(weatherDataStore.getThetaE700())) {
            cthe = cthe + weatherDataStore.getThetaE700();
            nct++;
        }
        if (NsharpLibBasics.qc(weatherDataStore.getThetaESurface())) {
            cthe = cthe + weatherDataStore.getThetaESurface();
            nct++;
        }
        if (nct < 1.0F)
            return;
        cthe = cthe / nct;
        // Adjust Theta-E axis scale with real view X scale
        thetaEDispWidth = 60;
        xAxisCenter = ((vxMax - vxMin) / 2) + vxMin;
        xScaleAdjustRatio = (vxMax - vxMin) / thetaEDispWidth;
        for (double h = -(thetaEDispWidth / 2); h <= (thetaEDispWidth / 2); h += 10.0F) {
            // cthe sits in the middle of x-axis
            int iThetaE = (int) (h + cthe);
            dispX = xAxisCenter + (h * xScaleAdjustRatio);
            dispY = vyMax;
            double[][] lines1 = { { dispX, dispY },
                    { dispX, dispY - 10 * yRatio } };
            thetaEHeightWRscShape.addLineSegment(lines1);

            if ((h != -(thetaEDispWidth / 2)) && (h != (thetaEDispWidth / 2))) {
                // draw scale with kelvin degree
                double[] lblXy1 = { dispX + 1 * xRatio, dispY - 12 * yRatio };
                thetaEHeightYRscShape.addLabel(String.valueOf(iThetaE + 273),
                        lblXy1);
            }
        }

        // ----- Plot vertical theta-e profile -----
        // use soundingLys directly
        dispX1 = dispY1 = -999;
        Map<Float, Float> thetaEHeightMap = weatherDataStore
                .getThetaEHeightMap();
        Map<Float, Float> pressThetaEMap = weatherDataStore.getPressThetaEMap();
        // plot only at pressure greater than or equal to 500
        for (NcSoundingLayer lys : soundingLys) {
            if ((lys.getPressure() >= 500)
                    && (NsharpLibBasics.qc(lys.getDewpoint()))) {
                float thetaE = pressThetaEMap.get(lys.getPressure());
                float adjustedThetaE = thetaE - cthe;
                dispX = xAxisCenter + (adjustedThetaE * xScaleAdjustRatio);
                dispY = vyMax
                        - (thetaEHeightMap.get(thetaE) * yScaleAdjustRatio);

                if (dispX1 == -999) {
                    dispX1 = dispX;
                    dispY1 = dispY;
                }
                double[][] lines1 = { { dispX, dispY }, { dispX1, dispY1 } };
                thetaEHeightRRscShape.addLineSegment(lines1);
                dispX1 = dispX;
                dispY1 = dispY;
            }
        }
        /* ----- Plot Vertical Legend ----- */
        Map<Float, Float> pressHeightMap = weatherDataStore.getPressHeightMap();
        dispX = vxMin;
        for (float pres = 1000; pres > 500; pres -= 100) {
            if (pressHeightMap.get(pres) == null) {
                continue;
            }
            dispY = vyMax - (pressHeightMap.get(pres) * yScaleAdjustRatio);
            double[][] lines1 = { { dispX, dispY },
                    { dispX + 10 * xRatio, dispY } };
            thetaEHeightWRscShape.addLineSegment(lines1);
            int heightAdj = (pressHeightMap.get(pres).intValue() / 100) * 100;
            double[] lblXy1 = { dispX + 20 * xRatio, dispY };
            thetaEHeightYRscShape.addLabel(String.valueOf(heightAdj), lblXy1);
        }
        thetaEHeightYRscShape.compile();
        thetaEHeightWRscShape.compile();
        thetaEHeightRRscShape.compile();
    }

    private void createRscThetaEPressureShape(NsharpWGraphics world) {
        /*
         * This plotting function is based on the algorithm of plot_thetae() at
         * xwvid5.c of native nsharp source code
         */
        thetaEPressureYRscShape = target
                .createWireframeShape(false, descriptor);
        thetaEPressureYRscShape.allocate(2);
        thetaEPressureRRscShape = target
                .createWireframeShape(false, descriptor);
        thetaEPressureRRscShape.allocate(soundingLys.size() * 2);
        thetaEPressureWRscShape = target
                .createWireframeShape(false, descriptor);
        thetaEPressureWRscShape.allocate(100);
        double dispX, dispY, dispX1, dispY1;
        double xAxisCenter, xScaleAdjustRatio, thetaEDispWidth;
        double yScaleAdjustRatio;
        float cthe, nct;
        double vyMax = world.getViewYmax();
        double vyMin = world.getViewYmin();
        double vxMax = world.getViewXmax();
        double vxMin = world.getViewXmin();

        // add a virtual line to make wire fram shape worked when only strings
        // are added to shape
        double[][] lines = { { 0, 0 }, { 0, 0 } };
        thetaEPressureYRscShape.addLineSegment(lines);
        // ----- Plot Label -----
        dispX = vxMin + 20 * xRatio;
        dispY = vyMin + 30 * yRatio;
        double[] lblXy = { dispX + 80 * xRatio, dispY - 10 * yRatio };
        thetaEPressureYRscShape.addLabel("Theta-E vs Pressure", lblXy);

        // plot theta E difference
        double[] lblXy2 = { dispX + 50 * xRatio, dispY + 60 * yRatio };
        float thetaEDiff = weatherDataStore.getThetaDiff();
        if (NsharpLibBasics.qc(thetaEDiff)) {
            String thetaDiffStr = String.format("TEI = %.0f", thetaEDiff);
            thetaEPressureYRscShape.addLabel(thetaDiffStr, lblXy2);
        }
        // ----- Plot horizontal legend -----
        cthe = 0.0F;
        nct = 0.0F;
        if (NsharpLibBasics.qc(weatherDataStore.getThetaE850())) {
            cthe = cthe + weatherDataStore.getThetaE850();
            nct++;
        }
        if (NsharpLibBasics.qc(weatherDataStore.getThetaE700())) {
            cthe = cthe + weatherDataStore.getThetaE700();
            nct++;
        }
        if (NsharpLibBasics.qc(weatherDataStore.getThetaESurface())) {
            cthe = cthe + weatherDataStore.getThetaESurface();
            nct++;
        }
        if (nct < 1.0F)
            return;
        cthe = cthe / nct;
        // Adjust Theta-E axis scale with real view X scale
        thetaEDispWidth = 60;
        xAxisCenter = ((vxMax - vxMin) / 2) + vxMin;
        xScaleAdjustRatio = (vxMax - vxMin) / thetaEDispWidth;
        for (double h = -(thetaEDispWidth / 2); h <= (thetaEDispWidth / 2); h += 10.0F) {
            // cthe sits in the middle of x-axis
            int iThetaE = (int) (h + cthe);
            dispX = xAxisCenter + (h * xScaleAdjustRatio);
            dispY = vyMax;
            double[][] lines1 = { { dispX, dispY },
                    { dispX, dispY - 20 * yRatio } };
            thetaEPressureWRscShape.addLineSegment(lines1);
            if ((h != -(thetaEDispWidth / 2)) && (h != (thetaEDispWidth / 2))) {
                // draw scale with kelvin degree
                double[] lblXy1 = { dispX + 1, dispY - 20 * yRatio };
                thetaEPressureYRscShape.addLabel(String.valueOf(iThetaE + 273),
                        lblXy1);
            }
        }

        // ----- Plot vertical theta-e profile -----
        // use soundingLys directly
        float highPressure = 1000;
        dispX1 = dispY1 = -999;
        // find highest pressure available on sounding data. But should be <=
        // 1000 and > 500
        for (NcSoundingLayer lys : soundingLys) {
            if ((lys.getPressure() >= 500) && (lys.getPressure() <= 1000)) {
                highPressure = lys.getPressure();
                break;
            }
        }
        // plot only at pressure greater than or equal to 500
        Map<Float, Float> pressThetaEMap = weatherDataStore.getPressThetaEMap();
        yScaleAdjustRatio = (vyMax - vyMin) / (highPressure - 500);
        for (NcSoundingLayer lys : soundingLys) {
            if ((lys.getPressure() >= 500)
                    && (NsharpLibBasics.qc(lys.getDewpoint()))) {
                float thetaE = pressThetaEMap.get(lys.getPressure());
                float adjustedThetaE = thetaE - cthe;
                dispX = xAxisCenter + (adjustedThetaE * xScaleAdjustRatio);
                dispY = vyMin + (lys.getPressure() - 500) * yScaleAdjustRatio;
                if (dispX1 == -999) {
                    dispX1 = dispX;
                    dispY1 = dispY;
                }
                double[][] lines1 = { { dispX, dispY }, { dispX1, dispY1 } };
                thetaEPressureRRscShape.addLineSegment(lines1);
                dispX1 = dispX;
                dispY1 = dispY;
            }
        }
        /* ----- Plot Vertical (pressure) Legend ----- */
        dispX = vxMin;
        for (int pres = 900; pres > 500; pres -= 100) {
            dispY = vyMin + (pres - 500) * yScaleAdjustRatio;
            double[][] lines1 = { { dispX, dispY },
                    { dispX + 20 * xRatio, dispY } };
            thetaEPressureWRscShape.addLineSegment(lines1);
            double[] lblXy1 = { dispX + 25 * xRatio, dispY };
            thetaEPressureYRscShape.addLabel(String.valueOf(pres), lblXy1);
        }
        thetaEPressureYRscShape.compile();
        thetaEPressureWRscShape.compile();
        thetaEPressureRRscShape.compile();
    }

    private void plotNsharpStormSlinky(IGraphicsTarget target,
            double zoomLevel, NsharpWGraphics world, Rectangle rect)
            throws VizException {
        /*
         * This plotting function is based on the algorithm of
         * plot_vis(),visual1(), vis_xy() at xwvid5.c of legacy nsharp source
         * code
         * 
         * This function is dynamically chnaged when user change
         * "storm relative" (by click on Hodo graph). Also it uses
         * target.drawCircle. With these 2 reasons, wire frame shape is not
         * used.
         */
        double dispX, dispY, dispX1, dispY1;
        double vyMax = world.getViewYmax();
        double vyMin = world.getViewYmin();
        double vxMax = world.getViewXmax();
        double vxMin = world.getViewXmin();

        PixelExtent extent = new PixelExtent(rect);
        target.setupClippingPlane(extent);
        /* ----- Plot Label ----- */
        dispX = vxMin + 15 * xRatio;
        dispY = vyMax - 20 * yRatio;
        target.drawString(font12, "Storm Slinky", dispX, dispY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_yellow,
                HorizontalAlignment.LEFT, VerticalAlignment.BOTTOM, null);
        // plot X axis
        dispY = vyMin + (vyMax - vyMin) / 2;
        target.drawLine(vxMin, dispY, 0.0, vxMax, dispY, 0.0,
                NsharpConstants.color_stellblue, commonLinewidth / 4F);
        // plot Y axis
        dispX = vxMin + (vxMax - vxMin) / 2;
        target.drawLine(dispX, vyMax, 0.0, dispX, vyMin, 0.0,
                NsharpConstants.color_stellblue, commonLinewidth / 4F);

        int currentparcel = rscHandler.getCurrentParcel();
        NsharpStormSlinkyInfo stormSlinkyInfo = weatherDataStore
                .getStormSlinkyInfoMap().get(currentparcel);
        if (stormSlinkyInfo != null) {
            // ----- Plot storm motion - white line -----
            dispX = vxMin + (vxMax - vxMin) / 2;
            dispY = vyMin + (vyMax - vyMin) / 2;
            dispX1 = dispX + stormSlinkyInfo.getStormSlinkXBase() * xRatio;
            dispY1 = dispY - stormSlinkyInfo.getStormSlinkYBase() * yRatio;
            target.drawLine(dispX, dispY, 0.0, dispX1, dispY1, 0.0,
                    NsharpConstants.color_white, commonLinewidth * 2);

            double minRatio = Math.min(xRatio, yRatio);
            List<Float> slinkyWindUList = stormSlinkyInfo.getTotalSu();
            List<Float> slinkyWindVList = stormSlinkyInfo.getTotalSv();
            List<Integer> slinkyColorList = stormSlinkyInfo.getColor();
            for (int i = 0; i < slinkyWindUList.size(); i++) {
                float xfactor, xs, ys;

                xfactor = .0075f;

                xs = (float) dispX
                        + (-slinkyWindUList.get(i) * xfactor * xRatio);
                ys = (float) dispY
                        + (-slinkyWindVList.get(i) * xfactor * yRatio);
                RGB color = stormSlinkyColorMap.get(slinkyColorList.get(i));

                target.drawCircle(xs, ys, 0, 6 * minRatio, color, markerWidth);
            }
            // write ang and tottim
            dispX = vxMin + 15 * xRatio;
            dispY = vyMin + 30 * yRatio;
            String s = (int) stormSlinkyInfo.getTottim() + " s      "
                    + (int) stormSlinkyInfo.getAngl() + " deg";
            target.drawString(font12, s, dispX, dispY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.BOTTOM, null);
        }
    }

    private void plotNsharpSRWindVectors(IGraphicsTarget target,
            double zoomLevel, NsharpWGraphics world, Rectangle rect)
            throws VizException {
        /*
         * 
         * This plotting function is based on the algorithm of
         * plot_storminflow() at xwvid5.c of legacy nsharp source code
         * 
         * This function is dynamically changed when user change
         * "storm relative" (by click on Hodo graph). Also it uses
         * target.drawArc. With these 2 reasons, wire frame shape is not used.
         */
        double dispX, dispY;
        double vyMax = world.getViewYmax();
        double vyMin = world.getViewYmin();
        double vxMax = world.getViewXmax();
        double vxMin = world.getViewXmin();
        int width = getCapability(OutlineCapability.class).getOutlineWidth();

        PixelExtent extent = new PixelExtent(rect);
        target.setupClippingPlane(extent);
        /* ----- Plot Label ----- */
        dispX = vxMin + 30 * xRatio;
        dispY = vyMin + 30 * yRatio;

        target.drawString(font12, "SR Wind Vectors", dispX, dispY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_yellow,
                HorizontalAlignment.LEFT, VerticalAlignment.BOTTOM, null);
        // plot X axis
        dispY = vyMin + (vyMax - vyMin) / 2;
        target.drawLine(vxMin, dispY, 0.0, vxMax, dispY, 0.0,
                NsharpConstants.color_white, width / 3F);
        // plot Y axis
        dispX = vxMin + (vxMax - vxMin) / 2;
        target.drawLine(dispX, vyMax, 0.0, dispX, vyMin, 0.0,
                NsharpConstants.color_white, width / 3F);

        // plot speed rings
        double minRatio = Math.min(xRatio, yRatio);
        for (int i = 20; i <= insetWidth / 2; i += 20) {
            double radius = i * minRatio;
            target.drawArc(dispX, dispY, 0, radius,
                    NsharpConstants.color_stellblue, markerWidth, 0, 360,
                    LineStyle.DASH_DOTTED, true);
        }

        double xscaleFactor = 0.7 * xRatio;
        double yscaleFactor = 0.7 * yRatio;
        int gap = 10;
        /* ----- Plot 0-2km Inflow Vector ----- */
        WindComponent sfcTo2kmSrWind = weatherDataStore
                .getStormTypeToSrMeanWindMap().get("SFC-2km");
        if (NsharpLibBasics.qc(sfcTo2kmSrWind.getUcomp())) {
            target.drawLine(dispX, dispY, 0.0,
                    dispX + sfcTo2kmSrWind.getUcomp() * xscaleFactor, dispY
                            - sfcTo2kmSrWind.getVcomp() * yscaleFactor, 0.0,
                    NsharpConstants.color_red, width * 2);

            target.drawString(font12, "L", dispX + sfcTo2kmSrWind.getUcomp()
                    * xscaleFactor, dispY - sfcTo2kmSrWind.getVcomp()
                    * yscaleFactor + 2 * gap * yRatio, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_red, HorizontalAlignment.CENTER,
                    VerticalAlignment.MIDDLE, null);
        }
        /* ----- Plot 4-6km Inflow Vector ----- */
        WindComponent fourTo6kmSrWind = weatherDataStore
                .getSrMeanWindComp4To6km();
        if (NsharpLibBasics.qc(fourTo6kmSrWind.getUcomp())) {
            target.drawLine(dispX, dispY, 0.0,
                    dispX + fourTo6kmSrWind.getUcomp() * xscaleFactor, dispY
                            - fourTo6kmSrWind.getVcomp() * yscaleFactor, 0.0,
                    NsharpConstants.color_cyan, width * 2);

            target.drawString(font12, "M", dispX + fourTo6kmSrWind.getUcomp()
                    * xscaleFactor, dispY - fourTo6kmSrWind.getVcomp()
                    * yscaleFactor + 2 * gap * yRatio, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_cyan, HorizontalAlignment.CENTER,
                    VerticalAlignment.MIDDLE, null);
        }
        /* ----- Plot 9-11km Inflow Vector ----- */
        WindComponent nineTo11kmSrWind = weatherDataStore
                .getSrMeanWindComp9To11km();
        if (NsharpLibBasics.qc(nineTo11kmSrWind.getUcomp())) {
            target.drawLine(dispX, dispY, 0.0,
                    dispX + nineTo11kmSrWind.getUcomp() * xscaleFactor, dispY
                            - nineTo11kmSrWind.getVcomp() * yscaleFactor, 0.0,
                    NsharpConstants.color_hotpink, width * 2);

            target.drawString(font12, "H", dispX + nineTo11kmSrWind.getUcomp()
                    * xscaleFactor, dispY - nineTo11kmSrWind.getVcomp()
                    * yscaleFactor + 2 * gap * yRatio, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_hotpink, HorizontalAlignment.CENTER,
                    VerticalAlignment.MIDDLE, null);
        }

    }

    public NsharpGenericPaneBackground getPsblWatchTypeBackground() {
        return psblWatchTypeBackground;
    }

    public RGB getWwTypeColor() {
        return wwTypeColor;
    }

    public NsharpGenericPaneBackground getSrWindsBackground() {
        return srWindsBackground;
    }

    @Override
    public void handleResize() {
        super.handleResize();
        IExtent ext = getDescriptor().getRenderableDisplay().getExtent();
        ext.reset();
        this.rectangle = new Rectangle((int) ext.getMinX(),
                (int) ext.getMinY(), (int) ext.getWidth(),
                (int) ext.getHeight());
        pe = new PixelExtent(this.rectangle);
        getDescriptor().setNewPe(pe);
        float prevHeight = insetPaneHeight;
        float prevWidth = insetPaneWidth;
        insetPaneWidth = (int) (ext.getWidth());
        insetPaneHeight = (int) (ext.getHeight());
        xRatio = xRatio * insetPaneWidth / prevWidth;
        yRatio = yRatio * insetPaneHeight / prevHeight;
        insetWidth = insetPaneWidth / 4;
        insetHeight = insetPaneHeight;
        srwindsXOrig = (int) (ext.getMinX());
        srwindsYOrig = (int) ext.getMinY();
        stormSlinkXOrig = srwindsXOrig + insetWidth;
        stormSlinkYOrig = srwindsYOrig;
        thetahXOrig = stormSlinkXOrig + insetWidth;
        thetahYOrig = srwindsYOrig;
        thetapXOrig = stormSlinkXOrig + insetWidth; // same as thetah
        thetapYOrig = srwindsYOrig;
        srwindvtrXOrig = thetapXOrig + insetWidth;
        srwindvtrYOrig = srwindsYOrig;
        psbWatchXOrig = thetapXOrig + insetWidth; // same as srwindvtr
        psbWatchYOrig = srwindsYOrig;
        Rectangle rectangle = new Rectangle(srwindsXOrig, srwindsYOrig,
                insetWidth, insetHeight);
        srWindsBackground.handleResize(rectangle);
        rectangle = new Rectangle(stormSlinkXOrig, stormSlinkYOrig, insetWidth,
                insetHeight);
        stormSlinkyBackground.handleResize(rectangle);
        rectangle = new Rectangle(thetahXOrig, thetahYOrig, insetWidth,
                insetHeight);
        thetaEHeightBackground.handleResize(rectangle);
        rectangle = new Rectangle(thetapXOrig, thetapYOrig, insetWidth,
                insetHeight);
        thetaEPresureBackground.handleResize(rectangle);
        rectangle = new Rectangle(srwindvtrXOrig, srwindvtrYOrig, insetWidth,
                insetHeight);
        srWindVectorBackground.handleResize(rectangle);
        rectangle = new Rectangle(psbWatchXOrig, psbWatchYOrig, insetWidth,
                insetHeight);
        psblWatchTypeBackground.handleResize(rectangle);
        createInsetWireFrameShapes();
    }
}
