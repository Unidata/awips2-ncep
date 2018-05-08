package gov.noaa.nws.ncep.ui.nsharp.display.rsc;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import javax.measure.converter.UnitConverter;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.DrawableString;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.HorizontalAlignment;
import com.raytheon.uf.viz.core.IGraphicsTarget.LineStyle;
import com.raytheon.uf.viz.core.IGraphicsTarget.PointStyle;
import com.raytheon.uf.viz.core.IGraphicsTarget.TextStyle;
import com.raytheon.uf.viz.core.IGraphicsTarget.VerticalAlignment;
import com.raytheon.uf.viz.core.PixelExtent;
import com.raytheon.uf.viz.core.drawables.IFont;
import com.raytheon.uf.viz.core.drawables.IShadedShape;
import com.raytheon.uf.viz.core.drawables.IWireframeShape;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.AbstractResourceData;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.d2d.ui.perspectives.D2D5Pane;
import com.raytheon.viz.ui.perspectives.VizPerspectiveListener;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibBasics;
import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibSkparams;
import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibThermo;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.DendriticZone;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.EffectiveLayerPressures;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.LParcelValues;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.LapseRateMax;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.LayerParameters;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.Parcel;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;
import gov.noaa.nws.ncep.ui.nsharp.NsharpConstants;
import gov.noaa.nws.ncep.ui.nsharp.NsharpLineProperty;
import gov.noaa.nws.ncep.ui.nsharp.NsharpOperationElement;
import gov.noaa.nws.ncep.ui.nsharp.NsharpShapeAndLineProperty;
import gov.noaa.nws.ncep.ui.nsharp.NsharpSoundingElementStateProperty;
import gov.noaa.nws.ncep.ui.nsharp.NsharpStationInfo;
import gov.noaa.nws.ncep.ui.nsharp.NsharpWGraphics;
import gov.noaa.nws.ncep.ui.nsharp.NsharpWxMath;
import gov.noaa.nws.ncep.ui.nsharp.background.NsharpGenericPaneBackground;
import gov.noaa.nws.ncep.ui.nsharp.background.NsharpIcingPaneBackground;
import gov.noaa.nws.ncep.ui.nsharp.background.NsharpSkewTPaneBackground;
import gov.noaa.nws.ncep.ui.nsharp.background.NsharpTurbulencePaneBackground;
import gov.noaa.nws.ncep.ui.nsharp.display.NsharpSkewTPaneDescriptor;
import gov.noaa.nws.ncep.ui.nsharp.display.rsc.NsharpCloudInfo.CloudLayer;
import gov.noaa.nws.ncep.ui.nsharp.view.NsharpLoadDialog;
import gov.noaa.nws.ncep.ui.nsharp.view.NsharpPaletteWindow;
import gov.noaa.nws.ncep.ui.pgen.display.IDisplayable;
import gov.noaa.nws.ncep.ui.pgen.display.IVector.VectorType;
import gov.noaa.nws.ncep.ui.pgen.elements.Vector;
import gov.noaa.nws.ncep.viz.common.ui.NmapCommon;

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
 * May 08, 2013 1847        bsteffen    Allow painting with no Wind Data.
 * 02/03/2014   1106        Chin Chen   Need to be able to use clicking on the Src,Time, or StnId to select display
 * 08/04/2014               Chin Chen   fixed effective level line drawing, height marker drawing
 * 01/27/2015   DR#17006,
 *              Task#5929   Chin Chen   NSHARP freezes when loading a sounding from MDCRS products
 *                                      in Volume Browser
 * 02/03/2015   DR#17084    Chin Chen   Model soundings being interpolated below the surface for elevated sites
 * 02/05/2015   DR16888     Chin Chen   fixed issue that "Comp(Src) button not functioning properly in NSHARP display"
 *                                      merged 12/11/2014 fixes at version 14.2.2 and check in again to 14.3.1
 * 07/05/2016   RM#15923    Chin Chen   NSHARP - Native Code replacement
 * 01/06/2017   RM#21993    Chin Chen   fixed "Text attached to the mouse covers up edit points in Edit Graph mode"
 * 06/13/2017   RM#34793    Chin Chen   Add max lapse rate bar on skewT pane
 * 09/1/2017    RM#34794    Chin Chen   NSHARP - Updates for March 2017 bigSharp version
 *                                      - Update the dendritic growth layer calculations and other skewT
 *                                      updates.
 * 11/29/2017   5863        bsteffen    Change dataTimes to a NavigableSet
 * Apr 18, 2018   17341 mgamazaychikov  Fix the cursor plotting for station with negative surface elevation,
 *                                      add scaling for pressure surface height labels
 *
 * </pre>
 *
 * @author Chin Chen
 * @version 1.0
 */
public class NsharpSkewTPaneResource extends NsharpAbstractPaneResource {
    private NsharpSkewTPaneBackground skewTBackground;

    private NsharpIcingPaneBackground icingBackground;

    private NsharpTurbulencePaneBackground turbBackground;

    private PaintProperties paintProps;

    private int currentGraphMode = NsharpConstants.GRAPH_SKEWT;

    private int currentSkewTEditMode = NsharpConstants.SKEWT_EDIT_MODE_EDITPOINT;

    private int skewtWidth = NsharpConstants.SKEWT_WIDTH;

    private int skewtHeight = NsharpConstants.SKEWT_HEIGHT;

    private int skewtXOrig = NsharpConstants.SKEWT_X_ORIG;

    private int skewtYOrig = NsharpConstants.SKEWT_Y_ORIG;

    private float omegaXOrig = NsharpConstants.OMEGA_X_ORIG;

    private float omegaYOrig = NsharpConstants.OMEGA_Y_ORIG;

    private float omegaWidth = NsharpConstants.OMEGA_WIDTH;

    private float omegaHeight = NsharpConstants.OMEGA_HEIGHT;

    private float omegaYEnd = NsharpConstants.OMEGA_Y_END;

    private float xRatio = 1;

    private float yRatio = 1;

    private String sTemperatureC = "";

    private String sTemperatureF = "";

    private String sThetaInK = "";

    private String sWThetaInK = "";

    private String sEThetaInK = "";

    private String sMixingRatio = "";

    private String sPressure = "";

    private double dPressure;

    private IWireframeShape heightMarkRscShape = null;

    private IWireframeShape wetBulbTraceRscShape = null;

    private IWireframeShape vtempTraceCurveRscShape = null;

    private IWireframeShape omegaBkgShape = null;

    private IWireframeShape titleBoxShape = null;

    private IWireframeShape omegaRscShape = null;

    private IShadedShape cloudFMShape = null;

    private IWireframeShape cloudFMLabelShape = null;

    private IShadedShape cloudCEShape = null;

    // Virtual temperature Parcel trace
    private IWireframeShape parcelVtTraceRscShape;

    // Real temperature parcel trace also considering comparison/overlay,
    // therefore using a list
    private List<NsharpShapeAndLineProperty> parcelRtShapeList = new ArrayList<>();

    private IWireframeShape dacpeTraceRscShape;

    private List<NsharpShapeAndLineProperty> pressureTempRscShapeList = new ArrayList<>();

    // ICING wireframe shape
    private IWireframeShape icingTempShape = null;

    private IWireframeShape icingRHShape = null;

    private IWireframeShape icingEPIShape = null;

    // Turbulence wireframe shape
    private IWireframeShape turbLnShape = null;

    private IWireframeShape turbWindShearShape = null;

    private IWireframeShape lclShape = null;

    private IWireframeShape elShape = null;

    private IWireframeShape mplShape = null;

    private IWireframeShape lfcShape = null;

    private IWireframeShape fzlShape = null;

    private IWireframeShape effectiveLayerLineShape = null;

    private IWireframeShape lrmShape = null;

    private IWireframeShape dendriticShape = null;

    private IWireframeShape frzShape = null;

    private IWireframeShape wbzShape = null;

    public static final float ENTRAIN_DEFAULT = 0.0f;

    public int TEMP_TYPE = 1;

    public int DEWPOINT_TYPE = 2;

    private int currentTempCurveType;

    private Coordinate interactiveTempPointCoordinate;

    private boolean plotInteractiveTemp = false;

    private boolean cursorInSkewT = false;

    private static int CURSER_FONT_INC_STEP = 3;

    private static int CURSER_FONT_10 = 10;

    private static int CURSER_STRING_OFF = CURSER_FONT_10 + 5 * CURSER_FONT_INC_STEP;

    private int curseToggledFontLevel = 15;

    private String myPerspective = NmapCommon.NatlCntrsPerspectiveID;

    public static ReentrantLock reentryLock = new ReentrantLock();

    private boolean justMoveToSidePane = false;

    private boolean cursorTopWindBarb = false;

    private boolean windBarbMagnify = false;

    private static final transient IUFStatusHandler statusHandler = UFStatus.getHandler(NsharpSkewTPaneResource.class);

    public boolean isWindBarbMagnify() {
        return windBarbMagnify;
    }

    public void setWindBarbMagnify(boolean windBarbMagnify) {
        this.windBarbMagnify = windBarbMagnify;
    }

    public boolean isCursorTopWindBarb() {
        return cursorTopWindBarb;
    }

    public void setCursorTopWindBarb(boolean cursorTopWindBarb) {
        this.cursorTopWindBarb = cursorTopWindBarb;
    }

    public boolean isJustMoveToSidePane() {
        return justMoveToSidePane;
    }

    public void setJustMoveToSidePane(boolean justMoveToSidePane) {
        this.justMoveToSidePane = justMoveToSidePane;
    }

    private boolean justBackToMainPane = false;

    public boolean isJustBackToMainPane() {
        return justBackToMainPane;
    }

    public void setJustBackToMainPane(boolean justBackToMainPane) {
        this.justBackToMainPane = justBackToMainPane;
    }

    public NsharpSkewTPaneResource(AbstractResourceData resourceData, LoadProperties loadProperties,
            NsharpSkewTPaneDescriptor desc) {
        super(resourceData, loadProperties, desc);
        skewTBackground = new NsharpSkewTPaneBackground((NsharpSkewTPaneDescriptor) descriptor);
        icingBackground = new NsharpIcingPaneBackground((NsharpSkewTPaneDescriptor) descriptor);
        turbBackground = new NsharpTurbulencePaneBackground((NsharpSkewTPaneDescriptor) descriptor);

        if (VizPerspectiveListener.getCurrentPerspectiveManager() != null) {
            myPerspective = VizPerspectiveListener.getCurrentPerspectiveManager().getPerspectiveId();
        }

    }

    @Override
    protected void disposeInternal() {
        skewTBackground.disposeInternal();
        icingBackground.disposeInternal();
        turbBackground.disposeInternal();
        skewTBackground = null;
        icingBackground = null;
        turbBackground = null;
        disposeRscWireFrameShapes();
        titleBoxShape.dispose();
        pressureTempRscShapeList = null;
        parcelRtShapeList = null;
        super.disposeInternal();
    }

    private void plotPressureTempEditPoints(IGraphicsTarget target, NsharpWGraphics world, RGB color, int type,
            List<NcSoundingLayer> soundingLys) throws VizException {
        double maxPressure = NsharpWxMath.reverseSkewTXY(new Coordinate(0, world.getWorldYmax())).y;
        double minPressure = NsharpWxMath.reverseSkewTXY(new Coordinate(0, world.getWorldYmin())).y;
        PointStyle ps = PointStyle.CIRCLE;
        for (NcSoundingLayer layer : soundingLys) {
            double t;
            if (type == TEMP_TYPE) {
                t = layer.getTemperature();
            } else if (type == DEWPOINT_TYPE) {
                t = layer.getDewpoint();
            } else {
                break;
            }
            double pressure = layer.getPressure();
            if (t != INVALID_DATA && pressure >= minPressure && pressure <= maxPressure) {

                Coordinate c1 = NsharpWxMath.getSkewTXY(pressure, t);

                c1.x = world.mapX(c1.x);
                c1.y = world.mapY(c1.y);

                target.drawPoint(c1.x, c1.y, 0.0, color, ps);

            }
        }
    }

    /*
     * This function mostly follow display_effective_layer() of xwvid1.c
     */
    public void createEffectiveLayerLinesShape() {
        if (effectiveLayerLineShape != null) {
            effectiveLayerLineShape.dispose();
            effectiveLayerLineShape = null;
        }
        EffectiveLayerPressures effLayer = weatherDataStore.getEffLyPress();
        float topPF = effLayer.getTopPress();
        float botPF = effLayer.getBottomPress();

        if (!NsharpLibBasics.qc(botPF)) {
            return;
        }

        effectiveLayerLineShape = target.createWireframeShape(false, descriptor);
        effectiveLayerLineShape.allocate(8);
        double dispX0;
        double dispX1;
        double dispX2;
        double dispX3;
        IExtent ext = getDescriptor().getRenderableDisplay().getExtent();
        dispX0 = ext.getMinX() + ext.getWidth() / 5;
        dispX1 = dispX0 + 20 * currentZoomLevel * xRatio;
        dispX2 = dispX1 + 20 * currentZoomLevel * xRatio;
        dispX3 = dispX2 + 20 * currentZoomLevel * xRatio;
        String botStr, topStr;
        float aglTop, aglBot;
        aglTop = NsharpLibBasics.agl(soundingLys, NsharpLibBasics.i_hght(soundingLys, topPF));
        aglBot = NsharpLibBasics.agl(soundingLys, NsharpLibBasics.i_hght(soundingLys, botPF));
        // Draw effective sfc level
        if (aglBot < 1) {
            botStr = "SFC";
        } else {
            botStr = String.format("%.0fm", aglBot);
        }
        double y = world.mapY(NsharpWxMath.getSkewTXY(botPF, 10).y);
        double[][] line1 = { { dispX1, y }, { dispX3, y } };
        effectiveLayerLineShape.addLineSegment(line1);
        double[] lblXy = { dispX0, y };
        effectiveLayerLineShape.addLabel(botStr, lblXy);

        // Draw effective top level
        topStr = String.format("%.0fm", aglTop);
        double y1 = world.mapY(NsharpWxMath.getSkewTXY(topPF, 10).y);
        double[][] line2 = { { dispX1, y1 }, { dispX3, y1 } };
        effectiveLayerLineShape.addLineSegment(line2);
        if (aglTop >= aglBot) {
            double[] lbl1Xy = { dispX0, y1 - 10 * yRatio };
            effectiveLayerLineShape.addLabel(topStr, lbl1Xy);
        }

        // Draw connecting line
        double[][] line3 = { { dispX2, y }, { dispX2, y1 } };
        effectiveLayerLineShape.addLineSegment(line3);
        // Compute and display effective helicity
        float helicity = weatherDataStore.getStormTypeToHelicityMap().get("Eff Inflow").getTotalHelicity();
        String helicityStr = String.format("%4.0f m%cs%c", helicity, NsharpConstants.SQUARE_SYMBOL,
                NsharpConstants.SQUARE_SYMBOL);

        // draw kelicity
        double[] lbl2Xy = { dispX2, y1 - 10 * yRatio };
        effectiveLayerLineShape.addLabel(helicityStr, lbl2Xy);
        effectiveLayerLineShape.compile();
    }

    public void createDendriticShapes() {
        // dendriticShape
        if (dendriticShape != null) {
            dendriticShape.dispose();
            dendriticShape = null;
        }
        double dispX1;
        double dispX2;
        IExtent ext = getDescriptor().getRenderableDisplay().getExtent();
        dispX1 = ext.getMaxX() - ext.getWidth() / 8;
        dispX2 = dispX1 - 25 * currentZoomLevel * xRatio;
        if (weatherDataStore.getWinterInfo() != null && weatherDataStore.getWinterInfo().getDendZone() != null) {
            DendriticZone dendZone = weatherDataStore.getWinterInfo().getDendZone();
            dendriticShape = target.createWireframeShape(false, descriptor);
            dendriticShape.allocate(24);

            // create bottom line
            float botPressure = dendZone.getBottomPress();
            double y = world.mapY(NsharpWxMath.getSkewTXY(botPressure, 10).y);
            String textStr = "%.0f'";
            textStr = String.format(textStr, dendZone.getBottomHeight());
            double[][] lines = { { dispX1, y }, { dispX2, y } };
            dendriticShape.addLineSegment(lines);
            double[] lblXy = { dispX2 - 10 * xRatio, y };
            dendriticShape.addLabel(textStr, lblXy);
            // create top line
            float topPressure = dendZone.getTopPress();
            y = world.mapY(NsharpWxMath.getSkewTXY(topPressure, 10).y);
            textStr = "%.0f'";
            textStr = String.format(textStr, dendZone.getTopHeight());
            double[][] lines2 = { { dispX1, y }, { dispX2, y } };
            dendriticShape.addLineSegment(lines2);
            double[] lblXy2 = { dispX2 - 10 * xRatio, y };
            dendriticShape.addLabel(textStr, lblXy2);

            // create dendritic trace line overlay on temperature trace
            // first loop to find closest bottom and top layer index
            int topIndex = 0, botIndex = 0;
            boolean botFound = false;
            for (NcSoundingLayer layer : soundingLys) {
                double temp = layer.getTemperature();
                double pressure = layer.getPressure();
                if (NsharpLibBasics.qc((float) temp)) {
                    if (pressure <= botPressure) {
                        if (botFound == false) {
                            botIndex = soundingLys.indexOf(layer);
                            botFound = true;
                        }
                        if (pressure < topPressure) {
                            topIndex = soundingLys.indexOf(layer) - 1;
                            break;
                        }
                    }
                }
            }
            if ((botIndex < topIndex) && (botIndex < soundingLys.size()) && (topIndex < soundingLys.size())) {
                Coordinate c0 = null;
                for (int i = botIndex; i <= topIndex; i++) {
                    NcSoundingLayer layer = soundingLys.get(i);
                    double temp = layer.getTemperature();
                    double pressure = layer.getPressure();
                    if (NsharpLibBasics.qc((float) temp) && pressure >= topPressure && pressure <= botPressure) {
                        Coordinate c1 = NsharpWxMath.getSkewTXY(pressure, temp);
                        c1.x = world.mapX(c1.x);
                        c1.y = world.mapY(c1.y);
                        if (c0 != null) {
                            double[][] traceline = { { c0.x, c0.y }, { c1.x, c1.y } };
                            dendriticShape.addLineSegment(traceline);
                        }
                        c0 = c1;
                    }
                    // add bot layer
                    if (i == botIndex) {
                        double botTemp = NsharpLibBasics.i_temp(soundingLys, botPressure);
                        Coordinate c1 = NsharpWxMath.getSkewTXY(botPressure, botTemp);
                        c1.x = world.mapX(c1.x);
                        c1.y = world.mapY(c1.y);
                        double[][] traceline = { { c0.x, c0.y }, { c1.x, c1.y } };
                        dendriticShape.addLineSegment(traceline);
                    } else if (i == topIndex) {
                        // add top layer
                        double topTemp = NsharpLibBasics.i_temp(soundingLys, topPressure);
                        Coordinate c1 = NsharpWxMath.getSkewTXY(topPressure, topTemp);
                        c1.x = world.mapX(c1.x);
                        c1.y = world.mapY(c1.y);
                        double[][] traceline = { { c0.x, c0.y }, { c1.x, c1.y } };
                        dendriticShape.addLineSegment(traceline);
                    }
                }
            }
            dendriticShape.compile();
        }
        // create frz shape
        if (frzShape != null) {
            frzShape.dispose();
            frzShape = null;
        }
        // frz height in msl and fgz height in AGL, pressure FGZ is same for
        // both
        if (NsharpLibBasics.qc(weatherDataStore.getFgzPress())) {
            frzShape = target.createWireframeShape(false, descriptor);
            frzShape.allocate(2);
            double pressure = weatherDataStore.getFgzPress();
            double y = world.mapY(NsharpWxMath.getSkewTXY(pressure, 10).y);
            String textStr = "FRZ= %.0f'";
            textStr = String.format(textStr, weatherDataStore.getFrzft());
            double[][] lines = { { dispX1, y }, { dispX2, y } };
            frzShape.addLineSegment(lines);
            double[] lblXy = { dispX2 - 25 * xRatio, y };
            frzShape.addLabel(textStr, lblXy);
            frzShape.compile();
        }
        // create wbz shape
        if (wbzShape != null) {
            wbzShape.dispose();
            wbzShape = null;
        }
        // wbz height in msl
        if (NsharpLibBasics.qc(weatherDataStore.getWbzp())) {
            wbzShape = target.createWireframeShape(false, descriptor);
            wbzShape.allocate(2);
            double pressure = weatherDataStore.getWbzp();
            double y = world.mapY(NsharpWxMath.getSkewTXY(pressure, 10).y);
            String textStr = "WBZ= %.0f'";
            textStr = String.format(textStr, weatherDataStore.getWbzMslft());
            double[][] lines = { { dispX1, y }, { dispX2, y } };
            wbzShape.addLineSegment(lines);
            double[] lblXy = { dispX2 - 25 * xRatio, y };
            wbzShape.addLabel(textStr, lblXy);
            wbzShape.compile();
        }
    }

    public void createLrmShape() {
        if (lrmShape != null) {
            lrmShape.dispose();
            lrmShape = null;
        }
        lrmShape = target.createWireframeShape(false, descriptor);
        lrmShape.allocate(6);
        LapseRateMax lrm = weatherDataStore.getLrm();
        // create bottom line
        float lrmPressure = NsharpLibBasics.i_pres(soundingLys, NsharpLibBasics.msl(soundingLys, lrm.getLrmHeight()));
        float lrmTemp = NsharpLibBasics.i_temp(soundingLys, lrmPressure);
        // draw line at right of lrm layer, range from (lrm temp + 2 degree C)
        // to (lrm temp + 4 degree C)
        Coordinate cbot1 = NsharpWxMath.getSkewTXY(lrmPressure, lrmTemp + 2);
        Coordinate cbot2 = NsharpWxMath.getSkewTXY(lrmPressure, lrmTemp + 4);
        cbot1.x = world.mapX(cbot1.x);
        cbot1.y = world.mapY(cbot1.y);
        cbot2.x = world.mapX(cbot2.x);
        cbot2.y = world.mapY(cbot2.y);
        double[][] lineBot = { { cbot1.x, cbot1.y }, { cbot2.x, cbot2.y } };
        lrmShape.addLineSegment(lineBot);
        // create top line
        lrmPressure = NsharpLibBasics.i_pres(soundingLys, NsharpLibBasics.msl(soundingLys, lrm.getLrmHeight() + 2000));
        lrmTemp = NsharpLibBasics.i_temp(soundingLys, lrmPressure);
        Coordinate ctop1 = NsharpWxMath.getSkewTXY(lrmPressure, lrmTemp);
        Coordinate ctop2 = NsharpWxMath.getSkewTXY(lrmPressure, lrmTemp + 1.5);
        ctop1.x = world.mapX(ctop1.x);
        ctop1.y = world.mapY(ctop1.y);
        ctop2.x = world.mapX(ctop2.x);
        ctop2.y = world.mapY(ctop2.y);
        double[][] lineTop = { { cbot1.x, ctop1.y }, { cbot2.x, ctop2.y } };
        lrmShape.addLineSegment(lineTop);
        // create line connect top and bottom lines
        double[][] lineCenter = { { (cbot1.x + cbot2.x) / 2, ctop1.y }, { (cbot1.x + cbot2.x) / 2, cbot1.y } };
        lrmShape.addLineSegment(lineCenter);

        String textStr = "%.1f C/km";
        textStr = String.format(textStr, lrm.getLrm());
        double[] lblXy = { (cbot1.x + cbot2.x) / 2, ctop1.y - 10 * yRatio };
        lrmShape.addLabel(textStr, lblXy);

        lrmShape.compile();
    }

    public void createLCLEtcLinesShape() {
        if (target == null) {
            return;
        }
        if (lclShape != null) {
            lclShape.dispose();
            lclShape = null;
        }
        if (elShape != null) {
            elShape.dispose();
            elShape = null;
        }
        if (mplShape != null) {
            mplShape.dispose();
            mplShape = null;
        }
        if (fzlShape != null) {
            fzlShape.dispose();
            fzlShape = null;
        }
        if (lfcShape != null) {
            lfcShape.dispose();
            lfcShape = null;
        }
        double dispX1;
        double dispX2;
        IExtent ext = getDescriptor().getRenderableDisplay().getExtent();
        dispX1 = ext.getMaxX() - ext.getWidth() / 9;
        dispX2 = dispX1 + 20 * currentZoomLevel * xRatio;
        int currentParcel = rscHandler.getCurrentParcel();
        Parcel parcel = weatherDataStore.getParcelMap().get(currentParcel);
        // draw LCL line
        if (parcel != null && NsharpLibBasics.qc(parcel.getLclpres())) {
            lclShape = target.createWireframeShape(false, descriptor);
            lclShape.allocate(4);
            double pressure = parcel.getLclpres();
            double y = world.mapY(NsharpWxMath.getSkewTXY(pressure, 10).y);
            double[][] lines = { { dispX1, y }, { dispX2, y } };
            lclShape.addLineSegment(lines);
            double[] lblXy = { dispX1 + 10 * xRatio, y + 10 * yRatio };
            lclShape.addLabel("LCL", lblXy);
            lclShape.compile();
        }
        // LFC line
        if (parcel != null && parcel.getLclpres() != parcel.getLfcpres() && NsharpLibBasics.qc(parcel.getLfcpres())) {
            lfcShape = target.createWireframeShape(false, descriptor);
            lfcShape.allocate(4);
            double pressure = parcel.getLfcpres();
            double y = world.mapY(NsharpWxMath.getSkewTXY(pressure, 10).y);
            double[][] lines = { { dispX1, y }, { dispX2, y } };
            lfcShape.addLineSegment(lines);
            double[] lblXy = { dispX1 + 10 * xRatio, y - 10 * yRatio };
            lfcShape.addLabel("LFC", lblXy);
            lfcShape.compile();
        }
        // draw EL line
        if (parcel != null && parcel.getElpres() != parcel.getLfcpres() && parcel.getLclpres() != parcel.getElpres()
                && NsharpLibBasics.qc(parcel.getElpres())) {
            elShape = target.createWireframeShape(false, descriptor);
            elShape.allocate(4);
            double pressure = parcel.getElpres();
            double y = world.mapY(NsharpWxMath.getSkewTXY(pressure, 10).y);
            double[][] lines = { { dispX1, y }, { dispX2, y } };
            elShape.addLineSegment(lines);
            double[] lblXy = { dispX1 + 10 * xRatio, y - 10 * yRatio };
            elShape.addLabel("EL", lblXy);
            elShape.compile();
        }
        // draw MPL line
        if (parcel != null && parcel.getMplpres() != parcel.getLfcpres() && parcel.getMplpres() != parcel.getElpres()
                && parcel.getMplpres() != parcel.getLclpres() && NsharpLibBasics.qc(parcel.getMplpres())) {
            mplShape = target.createWireframeShape(false, descriptor);
            mplShape.allocate(4);
            double pressure = parcel.getMplpres();
            double y = world.mapY(NsharpWxMath.getSkewTXY(pressure, 10).y);
            double[][] lines = { { dispX1, y }, { dispX2, y } };
            mplShape.addLineSegment(lines);
            double[] lblXy = { dispX1 + 10 * xRatio, y + 10 * yRatio };
            mplShape.addLabel("MPL", lblXy);
            mplShape.compile();
        }
        // draw FZL line, -20Cline and -30Cline in one same shape, fzlShape
        dispX2 = dispX1 - 20 * currentZoomLevel * xRatio;
        dispX1 = dispX2 - 20 * currentZoomLevel * xRatio;
        if (NsharpLibBasics.qc(weatherDataStore.getFgzPress())) {
            fzlShape = target.createWireframeShape(false, descriptor);
            fzlShape.allocate(6);
            double pressure = weatherDataStore.getFgzPress();
            double y = world.mapY(NsharpWxMath.getSkewTXY(pressure, 10).y);
            String textStr = "FZL= %.0f'";
            textStr = String.format(textStr, weatherDataStore.getFgzft());
            double[][] lines = { { dispX1, y }, { dispX2, y } };
            fzlShape.addLineSegment(lines);
            double[] lblXy = { dispX1, y - 10 * yRatio };
            fzlShape.addLabel(textStr, lblXy);

        }
        // draw -20Cline
        if (NsharpLibBasics.qc(weatherDataStore.getN20CPress())) {
            if (fzlShape == null) {
                fzlShape = target.createWireframeShape(false, descriptor);
                fzlShape.allocate(4);
            }
            double pressure = weatherDataStore.getN20CPress();
            double y = world.mapY(NsharpWxMath.getSkewTXY(pressure, -20).y);
            String textStr = "-20C= %.0f'";
            textStr = String.format(textStr, weatherDataStore.getN20CHeightFt());
            double[][] lines = { { dispX1, y }, { dispX2, y } };
            fzlShape.addLineSegment(lines);
            double[] lblXy = { dispX1, y - 10 * yRatio };
            fzlShape.addLabel(textStr, lblXy);
        }
        // draw -30Cline
        if (NsharpLibBasics.qc(weatherDataStore.getN30CPress())) {
            if (fzlShape == null) {
                fzlShape = target.createWireframeShape(false, descriptor);
                fzlShape.allocate(4);
            }
            double pressure = weatherDataStore.getN30CPress();
            double y = world.mapY(NsharpWxMath.getSkewTXY(pressure, 10).y);
            String textStr = "-30C= %.0f'";
            textStr = String.format(textStr, weatherDataStore.getN30CHeightFt());
            double[][] lines = { { dispX1, y }, { dispX2, y } };
            fzlShape.addLineSegment(lines);
            double[] lblXy = { dispX1, y - 10 * yRatio };
            fzlShape.addLabel(textStr, lblXy);
        }

        if (fzlShape != null) {
            fzlShape.compile();
        }

    }

    public float getTempDewPtSmallestGap() {
        float gap = soundingLys.get(0).getTemperature() - soundingLys.get(0).getDewpoint();
        for (NcSoundingLayer layer : soundingLys) {
            if (gap > layer.getTemperature() - layer.getDewpoint()) {
                gap = layer.getTemperature() - layer.getDewpoint();
            }
        }
        return gap;
    }

    private void plotNsharpMovingTempLine(IGraphicsTarget target, NsharpWGraphics world, RGB color)
            throws VizException {
        float currentLayerTemp, currentLayerDewP;
        Coordinate inC = NsharpWxMath.reverseSkewTXY(this.getWorld().unMap(interactiveTempPointCoordinate));
        float inTemp = (float) inC.x;
        float smallestGap = getTempDewPtSmallestGap();
        float tempShiftedDist;
        currentLayerTemp = soundingLys.get(currentSoundingLayerIndex).getTemperature();
        currentLayerDewP = soundingLys.get(currentSoundingLayerIndex).getDewpoint();
        if (currentTempCurveType == TEMP_TYPE) {
            if (inTemp < currentLayerTemp) {
                // shift to left, tempShiftedDist should be a negative number
                if ((currentLayerTemp - inTemp) > smallestGap) {
                    tempShiftedDist = -smallestGap;
                } else {
                    tempShiftedDist = inTemp - currentLayerTemp;
                }
            } else {
                // shift to right, tempShiftedDist should be a positive number
                tempShiftedDist = inTemp - currentLayerTemp;
            }
        } else {
            if (inTemp < currentLayerDewP) {
                // shift to left, tempShiftedDist should be a negative number
                tempShiftedDist = inTemp - currentLayerDewP;
            } else {
                // shift to right, tempShiftedDist should be a positive number
                if ((inTemp - currentLayerDewP) > smallestGap) {
                    tempShiftedDist = smallestGap;
                } else {
                    tempShiftedDist = inTemp - currentLayerDewP;
                }
            }
        }
        Coordinate c0 = null;
        // draw the line
        for (NcSoundingLayer layer : soundingLys) {
            double t;
            if (currentTempCurveType == TEMP_TYPE) {
                t = layer.getTemperature();
            } else {
                t = layer.getDewpoint();
            }
            double pressure = layer.getPressure();
            if (NsharpLibBasics.qc((float) t)) {

                Coordinate c1 = NsharpWxMath.getSkewTXY(pressure, t + tempShiftedDist);

                c1.x = world.mapX(c1.x);
                c1.y = world.mapY(c1.y);
                if (c0 != null) {
                    target.drawLine(c1.x, c1.y, 0.0, c0.x, c0.y, 0.0, color, commonLinewidth, LineStyle.SOLID);
                }
                c0 = c1;
            }

        }

    }

    private void plotNsharpInteractiveEditingTemp(IGraphicsTarget target, double zoomLevel, NsharpWGraphics world,
            RGB color) throws VizException {
        if (soundingLys.size() < 4) {
            return;
        }

        double aboveLayerPressure, belowLayerPressure;
        double aboveLayerT = 0, aboveLayerD = 0, belowLayerT = 0, belowLayerD = 0;
        int aboveLayerIndex, belowLayerIndex;
        double plotAboveT, plotBelowT;
        if (currentSoundingLayerIndex == 0) {
            aboveLayerIndex = currentSoundingLayerIndex;
            belowLayerIndex = currentSoundingLayerIndex + 1;
        } else if (currentSoundingLayerIndex == soundingLys.size() - 1) {
            belowLayerIndex = currentSoundingLayerIndex;
            aboveLayerIndex = currentSoundingLayerIndex - 1;
        } else {
            belowLayerIndex = currentSoundingLayerIndex + 1;
            aboveLayerIndex = currentSoundingLayerIndex - 1;
        }
        aboveLayerPressure = soundingLys.get(aboveLayerIndex).getPressure();
        aboveLayerT = soundingLys.get(aboveLayerIndex).getTemperature();
        aboveLayerD = soundingLys.get(aboveLayerIndex).getDewpoint();
        belowLayerPressure = soundingLys.get(belowLayerIndex).getPressure();
        belowLayerT = soundingLys.get(belowLayerIndex).getTemperature();
        belowLayerD = soundingLys.get(belowLayerIndex).getDewpoint();

        if (currentTempCurveType == TEMP_TYPE) {
            plotAboveT = aboveLayerT;
            plotBelowT = belowLayerT;

        } else {
            plotAboveT = aboveLayerD;
            plotBelowT = belowLayerD;

        }
        Coordinate c1 = NsharpWxMath.getSkewTXY(aboveLayerPressure, plotAboveT);
        c1.x = world.mapX(c1.x);
        c1.y = world.mapY(c1.y);
        target.drawLine(c1.x, c1.y, 0.0, interactiveTempPointCoordinate.x, interactiveTempPointCoordinate.y, 0.0, color,
                commonLinewidth, LineStyle.DASHED);
        c1 = NsharpWxMath.getSkewTXY(belowLayerPressure, plotBelowT);
        c1.x = world.mapX(c1.x);
        c1.y = world.mapY(c1.y);
        target.drawLine(c1.x, c1.y, 0.0, interactiveTempPointCoordinate.x, interactiveTempPointCoordinate.y, 0.0, color,
                commonLinewidth, LineStyle.DASHED);
    }

    public static Comparator<NcSoundingLayer> windSpeedComparator() {

        return new Comparator<NcSoundingLayer>() {

            @Override
            public int compare(NcSoundingLayer layerA, NcSoundingLayer layerB) {
                int retValue = 0;
                if (layerA != layerB) {
                    // reverse sort relative to pressure!
                    retValue = Double.compare(layerB.getWindSpeed(), layerA.getWindSpeed());
                }
                return retValue;
            }
        };
    }

    enum eleState {
        RE_MAX_WIND, PICKED, UNPICKED
    };

    class windPickedElement {
        NcSoundingLayer layer;

        eleState myState;

        public windPickedElement(NcSoundingLayer layer, eleState myState) {
            super();
            this.layer = layer;
            this.myState = myState;
        }

    };

    /**
     *
     * Draws Wind barb vs height This function followed algorithm in plot_barbs
     * (void) at xwvid1.c to choose wind bulbs with minimum distance defined @
     * graphConfigProperty.getWindBarbDistance()
     *
     */
    private void drawNsharpWindBarb(IGraphicsTarget target, double zoomLevel, NsharpWGraphics world, RGB iicolor,
            List<NcSoundingLayer> sndLys, double xPosition, double botPress) throws VizException {
        if (sndLys.size() < 4) {
            return;
        }

        List<windPickedElement> layerStateList = new ArrayList<windPickedElement>();
        float lastHeight = -9999;
        RGB icolor = iicolor;
        // #1: find relative max wind layers. I.e. a layer's wind is stronger
        // than immediate above and below layers
        NcSoundingLayer curLayer, aboveLayer, belowLayer;
        for (int i = 0; i < sndLys.size(); i++) {
            curLayer = sndLys.get(i);
            float spd = curLayer.getWindSpeed();
            if (spd < 0) {
                continue;
            }
            windPickedElement newEle = new windPickedElement(curLayer, eleState.UNPICKED);
            layerStateList.add(newEle);
            if (i == 0 || i == sndLys.size() - 1) {
                continue;
            }
            aboveLayer = sndLys.get(i + 1);
            belowLayer = sndLys.get(i - 1);
            if (spd > aboveLayer.getWindSpeed() && spd > belowLayer.getWindSpeed()) {
                newEle.myState = eleState.RE_MAX_WIND;

            }
        }
        // handle when all winds are not positive case
        if (layerStateList.isEmpty()) {
            return;
        }
        // #2: apply minimum distance rule, i.e no two wind layer closer than
        // the minimum distance, also make sure
        // relative max wind layer is picked.
        lastHeight = -9999;
        windPickedElement lastEle = layerStateList.get(0);
        for (windPickedElement ele : layerStateList) {
            float pressure = ele.layer.getPressure();
            float spd = ele.layer.getWindSpeed();
            if (pressure < botPress || spd < 0) {
                continue;
            }
            if ((ele.layer.getGeoHeight() - lastHeight) < graphConfigProperty.getWindBarbDistance() * zoomLevel) {
                if (ele.myState.equals(eleState.RE_MAX_WIND) && spd > lastEle.layer.getWindSpeed()) {
                    // swapped last picked layer with this relative max wind
                    // layer
                    lastEle.myState = eleState.UNPICKED;
                    lastHeight = ele.layer.getGeoHeight();
                    lastEle = ele;
                    continue;
                } else {
                    ele.myState = eleState.UNPICKED;
                    continue;
                }
            } else {
                if (ele.myState.equals(eleState.UNPICKED)) {
                    ele.myState = eleState.PICKED;
                }
                lastHeight = ele.layer.getGeoHeight();
                lastEle = ele;
            }

        }
        double windX = xPosition;
        double windY = 0;
        // plot wind barbs
        Color[] colors = new Color[1];
        Color color = new Color(icolor.red, icolor.green, icolor.blue);
        colors[0] = color;

        NsharpDisplayElementFactory df = new NsharpDisplayElementFactory(target, this.descriptor);
        ArrayList<IDisplayable> elements = new ArrayList<IDisplayable>();

        float wbSize = graphConfigProperty.getWindBarbSize();
        float wbWidth = graphConfigProperty.getWindBarbLineWidth();
        for (windPickedElement ele : layerStateList) {
            NcSoundingLayer layer = ele.layer;
            float pressure = layer.getPressure();
            if (pressure < 100) {
                continue;
            }
            float spd = layer.getWindSpeed();
            float dir = layer.getWindDirection();

            if (currentGraphMode == NsharpConstants.GRAPH_SKEWT) {
                windY = NsharpWxMath.getSkewTXY(pressure, 0).y;
            } else if (currentGraphMode == NsharpConstants.GRAPH_ICING) {
                // Chin:Y axis (pressure) is scaled using log scale and
                // increaing downward
                // WorldYmin= at pressure 1000,its value actually is 1000 (max),
                // wolrdYmax = at pressure 300, its value is 825 (min)
                windY = world.getWorldYmax() + (world.getWorldYmin() - icingBackground.toLogScale(pressure));
            } else if (currentGraphMode == NsharpConstants.GRAPH_TURB) {
                // Chin:Y axis (pressure) is scaled using log scale and
                // increaing downward
                // WorldYmin= at pressure 1000,its value actually is 1000 (max),
                // wolrdYmax = at pressure 300, its value is 825 (min)
                windY = world.getWorldYmax() + (world.getWorldYmin() - turbBackground.toLogScale(pressure));
            } else {
                continue;
            }

            float curWbSize = wbSize;
            float curWbWidth = wbWidth;
            if (ele.myState.equals(eleState.UNPICKED)) {
                if (graphConfigProperty.isShowFilteredWindInCircle()) {
                    // Chin::if we want pgen to draw un-picked wind as a circle,
                    // then set this.
                    spd = 0.1f;
                    curWbSize = 1;
                } else {
                    continue;
                }
            }

            // use PGEN tool
            Vector vect = new Vector();
            vect.setPgenType("Barb");
            vect.setVectorType(VectorType.WIND_BARB);
            vect.setArrowHeadSize(1.0);
            vect.setDirection(dir);
            vect.setSpeed(spd);
            vect.setSizeScale(curWbSize);
            vect.setLineWidth(curWbWidth);
            vect.setClear(true);
            vect.setColors(colors);
            Coordinate location = new Coordinate(world.mapX(windX), world.mapY(windY));
            vect.setLocation(location);
            ArrayList<IDisplayable> subelements = df.createDisplayElements(vect, paintProps);
            elements.addAll(subelements);
        }
        for (IDisplayable each : elements) {
            try {
                each.draw(target, paintProps);
                each.dispose();
            } catch (Exception e) {
                statusHandler.handle(Priority.PROBLEM, "drawNsharpWindBarb exception:", e);

            }
        }
    }

    private void drawNsharpSkewtCursorData(IGraphicsTarget target) throws VizException {
        IFont myFont;
        myFont = target.initializeFont("Monospace", curseToggledFontLevel, null);
        myFont.setSmoothing(false);
        myFont.setScaleFont(false);

        Coordinate c = NsharpWxMath.reverseSkewTXY(world.unMap(cursorCor.x, cursorCor.y));
        double p_mb = c.y;
        double temp = c.x;
        float htFt, htM, relh = -1;
        String curStrFormat, curStrFormat1, htMStr, htFtStr;
        String curStr, curStr1;
        VerticalAlignment vAli;
        HorizontalAlignment hAli;

        htM = NsharpLibBasics.agl(soundingLys, NsharpLibBasics.i_hght(soundingLys, (float) p_mb));
        htFt = NsharpLibBasics.mtof(htM);
        htMStr = Integer.toString(Math.round(htM));
        htFtStr = Integer.toString(Math.round(htFt));
        if (NsharpLibBasics.i_temp(soundingLys, (float) p_mb) > -9998.0
                && NsharpLibBasics.i_dwpt(soundingLys, (float) p_mb) > -9998.0) {
            relh = NsharpLibThermo.relh(soundingLys, (float) p_mb);
            curStrFormat = "%4.0f/%.0fkt %4.0fmb  %sft/%sm agl  %2.0f%%\n";
            curStr = String.format(curStrFormat, NsharpLibBasics.i_wdir(soundingLys, (float) p_mb),
                    NsharpLibBasics.i_wspd(soundingLys, (float) p_mb), p_mb, htFtStr, htMStr, relh);
        } else {
            curStrFormat = "%4.0f/%.0fkt %4.0fmb  %sft/%sm agl\n";
            curStr = String.format(curStrFormat, NsharpLibBasics.i_wdir(soundingLys, (float) p_mb),
                    NsharpLibBasics.i_wspd(soundingLys, (float) p_mb), p_mb, htFtStr, htMStr);
        }

        curStrFormat1 = "%s(%s) %4.1f/%4.1f%cF(%4.1f/%4.1f%cC)\n";
        temp = NsharpLibBasics.i_temp(soundingLys, (float) p_mb);
        UnitConverter celciusToFahrenheit = SI.CELSIUS.getConverterTo(NonSI.FAHRENHEIT);
        double tempF = celciusToFahrenheit.convert(temp);
        double dp = NsharpLibBasics.i_dwpt(soundingLys, (float) p_mb);
        double dpF = celciusToFahrenheit.convert(dp);
        curStr1 = String.format(curStrFormat1, sTemperatureF, sTemperatureC, tempF, dpF, NsharpConstants.DEGREE_SYMBOL,
                temp, dp, NsharpConstants.DEGREE_SYMBOL);

        // Adjust string plotting position
        if (cursorCor.x < skewtXOrig + (200 / currentZoomLevel) * xRatio) {
            hAli = HorizontalAlignment.LEFT;
        } else {
            hAli = HorizontalAlignment.RIGHT;
        }
        if (cursorCor.y > skewtYOrig + (50 / currentZoomLevel) * yRatio) {
            vAli = VerticalAlignment.BOTTOM;
        } else {
            vAli = VerticalAlignment.TOP;
        }

        target.drawString(myFont, curStr + curStr1, cursorCor.x, cursorCor.y, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_yellow, hAli, vAli, null);
        myFont.dispose();
    }

    private void paintIcing(double zoomLevel, IGraphicsTarget target) throws VizException {
        NsharpWGraphics plotWorld = icingBackground.getWorld();
        target.setupClippingPlane(icingBackground.getPe());
        try {

            if ((graphConfigProperty != null && graphConfigProperty.isWindBarb() == true)
                    || graphConfigProperty == null) {
                plotWorld.setWorldCoordinates(NsharpConstants.ICING_RELATIVE_HUMIDITY_LEFT,
                        icingBackground.toLogScale(NsharpConstants.ICING_PRESSURE_LEVEL_BOTTOM),
                        NsharpConstants.ICING_RELATIVE_HUMIDITY_RIGHT,
                        icingBackground.toLogScale(NsharpConstants.ICING_PRESSURE_LEVEL_TOP));
                double xPos = icingBackground.getWindBarbXPosition();

                drawNsharpWindBarb(target, zoomLevel, plotWorld, graphConfigProperty.getWindBarbColor(),
                        this.soundingLys, xPos, NsharpConstants.ICING_PRESSURE_LEVEL_TOP);
            }
        } catch (VizException e) {
            statusHandler.handle(Priority.PROBLEM, "paintIcing exception:", e);
        }
        // Chin NOTE: icining wireframeshapes are created dynamically ONLY when
        // icing display is to be shown
        // However, Skewt wireframeshapes are created when new sounding is
        // loaded.
        if (icingRHShape == null) {
            // current WorldCoordinates for RH already loaded
            plotWorld.setWorldCoordinates(NsharpConstants.ICING_RELATIVE_HUMIDITY_LEFT,
                    icingBackground.toLogScale(NsharpConstants.ICING_PRESSURE_LEVEL_TOP),
                    NsharpConstants.ICING_RELATIVE_HUMIDITY_RIGHT,
                    icingBackground.toLogScale(NsharpConstants.ICING_PRESSURE_LEVEL_BOTTOM));

            createIcingRHShape(plotWorld);
        }
        if (icingRHShape != null) {
            NsharpLineProperty lp = linePropertyMap.get(NsharpConstants.lineNameArray[NsharpConstants.LINE_ICING_RH]);
            target.drawWireframeShape(icingRHShape, lp.getLineColor(), lp.getLineWidth(), lp.getLineStyle(), font10);
        }
        if (icingTempShape == null) {
            plotWorld.setWorldCoordinates(NsharpConstants.ICING_TEMPERATURE_LEFT,
                    icingBackground.toLogScale(NsharpConstants.ICING_PRESSURE_LEVEL_TOP),
                    NsharpConstants.ICING_TEMPERATURE_RIGHT,
                    icingBackground.toLogScale(NsharpConstants.ICING_PRESSURE_LEVEL_BOTTOM));
            createIcingTempShape(plotWorld);
        }
        if (icingTempShape != null) {
            NsharpLineProperty lp = linePropertyMap.get(NsharpConstants.lineNameArray[NsharpConstants.LINE_ICING_TEMP]);
            target.drawWireframeShape(icingTempShape, lp.getLineColor(), lp.getLineWidth(), lp.getLineStyle(), font10);
        }
        if (icingEPIShape == null) {
            plotWorld.setWorldCoordinates(NsharpConstants.ICING_TEMPERATURE_LEFT,
                    icingBackground.toLogScale(NsharpConstants.ICING_PRESSURE_LEVEL_TOP),
                    NsharpConstants.ICING_TEMPERATURE_RIGHT,
                    icingBackground.toLogScale(NsharpConstants.ICING_PRESSURE_LEVEL_BOTTOM));
            createIcingEPIShape(plotWorld);
        }
        if (icingEPIShape != null) {
            NsharpLineProperty lp = linePropertyMap.get(NsharpConstants.lineNameArray[NsharpConstants.LINE_ICING_EPI]);
            target.drawWireframeShape(icingEPIShape, lp.getLineColor(), lp.getLineWidth(), lp.getLineStyle(), font10);
        }

        target.clearClippingPlane();

    }

    private void paintTurbulence(double zoomLevel, IGraphicsTarget target) throws VizException {
        NsharpWGraphics plotWorld = turbBackground.getWorld();
        target.setupClippingPlane(turbBackground.getPe());
        // Chin NOTE: turbulence wireframeshapes are created dynamically ONLY
        // when turbulence display is to be shown
        // However, Skewt wireframeshapes are created when new sounding is
        // loaded.
        try {
            // Chin:NOTE: LN Richardson number is plotted with positive number
            // increase to left and netagive number decrease to its right side.
            // Therefore, we have to set its world X coordintion in a reverse
            // way as plotting Icing wind barb.
            if ((graphConfigProperty != null && graphConfigProperty.isWindBarb() == true)
                    || graphConfigProperty == null) {
                plotWorld.setWorldCoordinates(NsharpConstants.TURBULENCE_LN_RICHARDSON_NUMBER_RIGHT,
                        turbBackground.toLogScale(NsharpConstants.TURBULENCE_PRESSURE_LEVEL_BOTTOM),
                        NsharpConstants.TURBULENCE_LN_RICHARDSON_NUMBER_LEFT,
                        turbBackground.toLogScale(NsharpConstants.TURBULENCE_PRESSURE_LEVEL_TOP));
                double xPos = turbBackground.getWindBarbXPosition();

                drawNsharpWindBarb(target, zoomLevel, plotWorld, graphConfigProperty.getWindBarbColor(),
                        this.soundingLys, xPos/* 7 */, NsharpConstants.TURBULENCE_PRESSURE_LEVEL_TOP);
            }
        } catch (VizException e) {
            statusHandler.handle(Priority.PROBLEM, "paintTurbulence exception:", e);
        }
        if (turbLnShape == null || turbWindShearShape == null) {
            createTurbulenceShapes(plotWorld);
        }
        if (turbLnShape != null) {
            NsharpLineProperty lp = linePropertyMap
                    .get(NsharpConstants.lineNameArray[NsharpConstants.LINE_TURBULENCE_LN]);
            target.drawWireframeShape(turbLnShape, lp.getLineColor(), lp.getLineWidth(), lp.getLineStyle(), font10);

        }
        if (turbWindShearShape != null) {
            NsharpLineProperty lp = linePropertyMap
                    .get(NsharpConstants.lineNameArray[NsharpConstants.LINE_TURBULENCE_WS]);
            target.drawWireframeShape(turbWindShearShape, lp.getLineColor(), lp.getLineWidth(), lp.getLineStyle(),
                    font10);
        }

        target.clearClippingPlane();

    }

    private void drawNsharpFileNameAndSampling(IGraphicsTarget target, double zoomLevel) throws VizException {
        double dispX, xmin;
        double dispY, ymin;
        IExtent ext = getDescriptor().getRenderableDisplay().getExtent();
        xmin = ext.getMinX(); // Extent's viewable envelope min x and y
        ymin = ext.getMinY();
        dispX = xmin + 20 * zoomLevel * xRatio;
        dispY = ymin + 35 * zoomLevel * yRatio;
        double hRatio = paintProps.getView().getExtent().getWidth() / paintProps.getCanvasBounds().width;
        double vRatio = paintProps.getView().getExtent().getHeight() / paintProps.getCanvasBounds().height;

        RGB pickedStnColor = NsharpConstants.color_green;
        String pickedStnInfoStr = "";
        String latlon = "";
        if (rscHandler.isOverlayIsOn()) {
            NsharpSoundingElementStateProperty preSndProfileProp = rscHandler.getPreSndProfileProp();
            if (preSndProfileProp != null) {
                pickedStnColor = linePropertyMap.get(NsharpConstants.lineNameArray[NsharpConstants.LINE_OVERLAY2])
                        .getLineColor();
                String stnInfoStr = preSndProfileProp.getElementDescription();
                latlon = Math.rint(preSndProfileProp.getStnInfo().getLatitude() * 100) / 100 + ","
                        + Math.rint(preSndProfileProp.getStnInfo().getLongitude() * 100) / 100;
                DrawableString str = new DrawableString(stnInfoStr, pickedStnColor);
                str.font = font20;
                str.setCoordinates(dispX + 300 * zoomLevel * xRatio, dispY);
                str.horizontalAlignment = HorizontalAlignment.LEFT;
                str.verticallAlignment = VerticalAlignment.TOP;
                DrawableString latlonstr = new DrawableString(latlon, pickedStnColor);
                latlonstr.font = font20;
                latlonstr.setCoordinates(dispX + 300 * zoomLevel * xRatio,
                        dispY + target.getStringsBounds(str).getHeight() * vRatio);
                latlonstr.horizontalAlignment = HorizontalAlignment.LEFT;
                latlonstr.verticallAlignment = VerticalAlignment.TOP;
                target.drawStrings(str, latlonstr);
            }
            pickedStnColor = linePropertyMap.get(NsharpConstants.lineNameArray[NsharpConstants.LINE_OVERLAY1])
                    .getLineColor();
        }

        pickedStnInfoStr = rscHandler.getPickedStnInfoStr();

        // Also draw stn lat/lon info string and sounding type string
        NsharpStationInfo pickedStnInfo = rscHandler.getPickedStnInfo();
        if (pickedStnInfo != null) {
            latlon = Math.rint(pickedStnInfo.getLatitude() * 100) / 100 + ","
                    + Math.rint(pickedStnInfo.getLongitude() * 100) / 100;
        }

        DrawableString str = new DrawableString(pickedStnInfoStr, pickedStnColor);
        str.font = font20;
        str.setCoordinates(dispX, dispY);
        str.horizontalAlignment = HorizontalAlignment.LEFT;
        str.verticallAlignment = VerticalAlignment.TOP;
        DrawableString latlonstr = new DrawableString(latlon, pickedStnColor);
        latlonstr.font = font20;
        latlonstr.setCoordinates(dispX, dispY + target.getStringsBounds(str).getHeight() * vRatio);
        latlonstr.horizontalAlignment = HorizontalAlignment.LEFT;
        latlonstr.verticallAlignment = VerticalAlignment.TOP;
        Rectangle2D rect = target.getStringsBounds(str);

        PixelExtent boxExt;
        if (cursorInSkewT == true) {
            double boxMinX = dispX, boxMinY = dispY, sampleStrsWidth;
            dispY = dispY + target.getStringsBounds(str).getHeight() * vRatio;
            // draw dynamic temp, theta, height
            // Column 1: pressure, C and F
            DrawableString str1 = new DrawableString(sPressure + "     ", NsharpConstants.color_white);
            str1.font = font20;
            dispY = dispY + target.getStringsBounds(str).getHeight() * vRatio;
            str1.setCoordinates(dispX, dispY);
            str1.horizontalAlignment = HorizontalAlignment.LEFT;
            str1.verticallAlignment = VerticalAlignment.TOP;
            DrawableString str2 = new DrawableString(sTemperatureC, NsharpConstants.color_red);
            str2.font = font20;
            dispY = dispY + target.getStringsBounds(str).getHeight() * vRatio;
            str2.setCoordinates(dispX, dispY);
            str2.horizontalAlignment = HorizontalAlignment.LEFT;
            str2.verticallAlignment = VerticalAlignment.TOP;
            DrawableString str3 = new DrawableString(sTemperatureF, NsharpConstants.color_red);
            str3.font = font20;
            dispY = dispY + target.getStringsBounds(str).getHeight() * vRatio;
            str3.setCoordinates(dispX, dispY);
            str3.horizontalAlignment = HorizontalAlignment.LEFT;
            str3.verticallAlignment = VerticalAlignment.TOP;

            // column 2: m, ft, mixing ratio
            float heightM = NsharpLibBasics.i_hght(soundingLys, (float) dPressure);
            String sHeightM = String.format("%.0fm", heightM);
            dispX = dispX + target.getStringsBounds(str1).getWidth() * hRatio;
            dispY = ymin + 35 * zoomLevel * yRatio + 2 * target.getStringsBounds(str).getHeight() * vRatio;
            DrawableString str4 = new DrawableString(sHeightM, NsharpConstants.color_cyan);
            str4.font = font20;
            str4.setCoordinates(dispX, dispY);
            str4.horizontalAlignment = HorizontalAlignment.LEFT;
            str4.verticallAlignment = VerticalAlignment.TOP;
            String sHeightFt = String.format("%.0fft", NsharpConstants.metersToFeet.convert(heightM));
            DrawableString str5 = new DrawableString(sHeightFt + "     ", NsharpConstants.color_cyan);
            str5.font = font20;
            dispY = dispY + target.getStringsBounds(str).getHeight() * vRatio;
            str5.setCoordinates(dispX, dispY);
            str5.horizontalAlignment = HorizontalAlignment.LEFT;
            str5.verticallAlignment = VerticalAlignment.TOP;
            DrawableString str6 = new DrawableString(sMixingRatio, NsharpConstants.color_green);
            str6.font = font20;
            dispY = dispY + target.getStringsBounds(str).getHeight() * vRatio;
            str6.setCoordinates(dispX, dispY);
            str6.horizontalAlignment = HorizontalAlignment.LEFT;
            str6.verticallAlignment = VerticalAlignment.TOP;

            // column 3: Theta, ThetaW, ThetaE
            dispX = dispX + target.getStringsBounds(str5).getWidth() * hRatio;
            dispY = ymin + 35 * zoomLevel * yRatio + 2 * target.getStringsBounds(str).getHeight() * vRatio;
            DrawableString str7 = new DrawableString(sThetaInK, NsharpConstants.color_yellow);
            str7.font = font20;
            str7.setCoordinates(dispX, dispY);
            str7.horizontalAlignment = HorizontalAlignment.LEFT;
            str7.verticallAlignment = VerticalAlignment.TOP;
            DrawableString str8 = new DrawableString(sWThetaInK, NsharpConstants.color_yellow);
            str8.font = font20;
            dispY = dispY + target.getStringsBounds(str).getHeight() * vRatio;
            str8.setCoordinates(dispX, dispY);
            str8.horizontalAlignment = HorizontalAlignment.LEFT;
            str8.verticallAlignment = VerticalAlignment.TOP;
            DrawableString str9 = new DrawableString(sEThetaInK, NsharpConstants.color_yellow);
            str9.font = font20;
            dispY = dispY + target.getStringsBounds(str).getHeight() * vRatio;
            str9.setCoordinates(dispX, dispY);
            str9.horizontalAlignment = HorizontalAlignment.LEFT;
            str9.verticallAlignment = VerticalAlignment.TOP;
            sampleStrsWidth = target.getStringsBounds(str1).getWidth() + target.getStringsBounds(str5).getWidth()
                    + target.getStringsBounds(str8).getWidth();
            double boxWidth;
            if (rect.getWidth() < sampleStrsWidth) {
                boxWidth = sampleStrsWidth + 1;
            } else {
                boxWidth = rect.getWidth() + 1;
            }
            boxExt = new PixelExtent(boxMinX, boxMinX + boxWidth * hRatio, boxMinY - 1 * vRatio,
                    boxMinY + rect.getHeight() * vRatio * 5);
            // blank out box, should draw this first and then draw data on top
            // of it
            target.drawShadedRect(boxExt, NsharpConstants.color_black, 1f, null);
            target.drawStrings(str, str1, str2, str3, str4, str5, str6, str7, str8, str9);
        } else {
            boxExt = new PixelExtent(dispX, dispX + (rect.getWidth() + 1) * hRatio, dispY - 1 * vRatio,
                    dispY + 2 * rect.getHeight() * vRatio);
            // blank out box
            target.drawShadedRect(boxExt, NsharpConstants.color_black, 1f, null);
        }
        target.drawStrings(str, latlonstr);

        RGB wwTypeColor = weatherDataStore.getWwTypeColor();
        // box border line colored with "Psbl Watch Type" color
        target.drawRect(boxExt, wwTypeColor, 2f, 1f);
    }

    @Override
    protected void paintInternal(IGraphicsTarget target, PaintProperties paintProps) throws VizException {
        this.paintProps = paintProps;
        super.paintInternal(target, paintProps);
        if (soundingLys == null) {
            drawNoDataMessage(target);
            return;
        }

        if (rscHandler == null) {
            return;
        }

        if (myPerspective.equals(D2D5Pane.ID_PERSPECTIVE)) {
            // swapping
            if (justMoveToSidePane) {
                reentryLock.lock();
                // to check a scenario that sounding data is removed while
                // thread is locked
                if (soundingLys == null || (soundingLys != null && soundingLys.size() < 2)) {
                    reentryLock.unlock();
                    return;
                }
                rscHandler.recomputeWeatherData();
                handleResize();
                justMoveToSidePane = false;
                reentryLock.unlock();
            } else if (justBackToMainPane) {
                reentryLock.lock();
                if (soundingLys == null || (soundingLys != null && soundingLys.size() < 2)) {
                    reentryLock.unlock();
                    return;
                }
                rscHandler.recomputeWeatherData();// swapping
                createRscWireFrameShapes();
                justBackToMainPane = false;
                reentryLock.unlock();
                NsharpPaletteWindow paletteWin = NsharpPaletteWindow.getInstance();
                if (paletteWin != null) {
                    paletteWin.restorePaletteWindow(paneConfigurationName, rscHandler.getCurrentGraphMode(),
                            rscHandler.isInterpolateIsOn(), rscHandler.isOverlayIsOn(), rscHandler.isCompareStnIsOn(),
                            rscHandler.isCompareTmIsOn(), rscHandler.isEditGraphOn(), rscHandler.isCompareSndIsOn());
                }
            }
        }

        if (currentGraphMode == NsharpConstants.GRAPH_SKEWT) {
            skewTBackground.paint(target, paintProps);
        } else if (currentGraphMode == NsharpConstants.GRAPH_ICING) {
            icingBackground.paint(target, paintProps);
        } else if (currentGraphMode == NsharpConstants.GRAPH_TURB) {
            turbBackground.paint(target, paintProps);
        } else {
            // default
            skewTBackground.paint(target, paintProps);
        }

        if (soundingLys != null) {
            this.font10.setSmoothing(false);
            this.font10.setScaleFont(false);
            this.font9.setSmoothing(false);
            this.font9.setScaleFont(false);
            this.font12.setSmoothing(false);
            this.font12.setScaleFont(false);

            if (currentGraphMode == NsharpConstants.GRAPH_SKEWT) {

                target.setupClippingPlane(pe);
                // plot temp curve, when constructing pressureTempRscShapeList,
                // it already considered
                // comparison, overlay, etc..so, just draw it.
                for (NsharpShapeAndLineProperty shapeNLp : pressureTempRscShapeList) {
                    target.drawWireframeShape(shapeNLp.getShape(), shapeNLp.getLp().getLineColor(),
                            shapeNLp.getLp().getLineWidth(), shapeNLp.getLp().getLineStyle(), font9);
                }
                // plot real temp parcel trace, when constructing
                // parcelRtShapeList, it already considered
                // comparison, overlay, etc..so, just draw it.
                // color is following comparison/overlay lines' configuration.
                // line width and line style are following parcel line
                // configuration
                if (graphConfigProperty.isParcel() == true && rscHandler.isGoodData()) {
                    NsharpLineProperty parcelLp = linePropertyMap
                            .get(NsharpConstants.lineNameArray[NsharpConstants.LINE_PARCEL]);
                    for (NsharpShapeAndLineProperty shapeNLp : parcelRtShapeList) {
                        target.drawWireframeShape(shapeNLp.getShape(), shapeNLp.getLp().getLineColor(),
                                parcelLp.getLineWidth(), parcelLp.getLineStyle(), font10);
                    }
                }
                boolean compareStnIsOn = rscHandler.isCompareStnIsOn();
                boolean compareSndIsOn = rscHandler.isCompareSndIsOn();
                boolean compareTmIsOn = rscHandler.isCompareTmIsOn();
                boolean editGraphOn = rscHandler.isEditGraphOn();
                boolean overlayIsOn = rscHandler.isOverlayIsOn();
                if (graphConfigProperty != null) {
                    if (graphConfigProperty.isTemp() == true && !compareStnIsOn && !compareTmIsOn && !compareSndIsOn) {
                        if (editGraphOn) {
                            plotPressureTempEditPoints(target, world, NsharpConstants.color_red, TEMP_TYPE,
                                    this.soundingLys);
                        }
                    }
                    // dew point curve
                    if (graphConfigProperty.isDewp() == true && !compareStnIsOn && !compareTmIsOn && !compareSndIsOn) {
                        if (editGraphOn) {
                            plotPressureTempEditPoints(target, world, NsharpConstants.color_green, DEWPOINT_TYPE,
                                    this.soundingLys);
                        }
                    }
                    // plot wet bulb trace
                    if (graphConfigProperty.isWetBulb() == true && rscHandler.isGoodData() && !compareStnIsOn
                            && !compareTmIsOn && !compareSndIsOn) {
                        NsharpLineProperty lp = linePropertyMap
                                .get(NsharpConstants.lineNameArray[NsharpConstants.LINE_WETBULB]);
                        target.drawWireframeShape(wetBulbTraceRscShape, lp.getLineColor(), lp.getLineWidth(),
                                lp.getLineStyle(), font10);
                    }
                    // plot virtual temperature trace
                    if (graphConfigProperty.isVTemp() == true && rscHandler.isGoodData() && !compareStnIsOn
                            && !compareTmIsOn && !compareSndIsOn) {
                        NsharpLineProperty lp = linePropertyMap
                                .get(NsharpConstants.lineNameArray[NsharpConstants.LINE_VIRTUAL_TEMP]);
                        target.drawWireframeShape(vtempTraceCurveRscShape, lp.getLineColor(), lp.getLineWidth(),
                                lp.getLineStyle(), font10);
                    }
                    // virtual temperature parcel trace curve
                    if (graphConfigProperty.isParcelTv() == true && rscHandler.isGoodData() // #5929
                            && !compareStnIsOn && !compareTmIsOn && !compareSndIsOn && !overlayIsOn) {
                        if (soundingLys.size() > 0) {
                            NsharpLineProperty lp = linePropertyMap
                                    .get(NsharpConstants.lineNameArray[NsharpConstants.LINE_PARCEL_TV]);
                            target.drawWireframeShape(parcelVtTraceRscShape, lp.getLineColor(), lp.getLineWidth(),
                                    lp.getLineStyle(), font10);
                        }
                    }

                    if (graphConfigProperty.isDcape() == true && rscHandler.isGoodData() && dacpeTraceRscShape != null
                            && !compareStnIsOn && !compareSndIsOn && !compareTmIsOn && !overlayIsOn) {
                        if (soundingLys.size() > 0) {
                            NsharpLineProperty lp = linePropertyMap
                                    .get(NsharpConstants.lineNameArray[NsharpConstants.LINE_DCAPE]);
                            target.drawWireframeShape(dacpeTraceRscShape, lp.getLineColor(), lp.getLineWidth(),
                                    lp.getLineStyle(), font10);

                        }
                    }
                    if (graphConfigProperty.isEffLayer() == true && rscHandler.isGoodData() && !compareStnIsOn
                            && !compareTmIsOn && !compareSndIsOn) {
                        // draw effective layer lines
                        target.drawWireframeShape(effectiveLayerLineShape, NsharpConstants.color_cyan_md, 2,
                                commonLineStyle, font10);
                    }
                    // cloud
                    if (graphConfigProperty.isCloud() == true && rscHandler.isGoodData() && !compareStnIsOn
                            && !compareTmIsOn && !compareSndIsOn) {
                        if (cloudFMShape != null) {
                            target.drawShadedShape(cloudFMShape, 1f);
                        }
                        if (cloudFMLabelShape != null) {
                            target.drawWireframeShape(cloudFMLabelShape, NsharpConstants.color_chocolate,
                                    commonLinewidth * 3, commonLineStyle, font9);
                        }
                        if (cloudCEShape != null) {
                            target.drawShadedShape(cloudCEShape, 1f);
                        }
                    }
                    if (graphConfigProperty.isOmega() == true && !compareStnIsOn && !compareTmIsOn && !compareSndIsOn) {
                        if (NsharpLoadDialog.getAccess() != null && (NsharpLoadDialog.getAccess()
                                .getActiveLoadSoundingType() == NsharpLoadDialog.MODEL_SND
                                || NsharpLoadDialog.getAccess()
                                        .getActiveLoadSoundingType() == NsharpLoadDialog.PFC_SND)) {
                            // plot omega
                            drawOmega();
                        }
                    }
                } else {
                    // by default, draw everything
                    if (!compareStnIsOn && !compareTmIsOn && !compareSndIsOn) {
                        if (editGraphOn) {
                            plotPressureTempEditPoints(target, world, NsharpConstants.color_red, TEMP_TYPE,
                                    this.soundingLys);
                        }
                        // dew point curve
                        if (editGraphOn) {
                            plotPressureTempEditPoints(target, world, NsharpConstants.color_green, DEWPOINT_TYPE,
                                    this.soundingLys);
                        }
                        if (rscHandler.isGoodData()) {
                            // plot wetbulb trace
                            NsharpLineProperty lp = linePropertyMap
                                    .get(NsharpConstants.lineNameArray[NsharpConstants.LINE_WETBULB]);
                            target.drawWireframeShape(wetBulbTraceRscShape, lp.getLineColor(), lp.getLineWidth(),
                                    lp.getLineStyle(), font10);
                            // plot virtual temp trace
                            lp = linePropertyMap.get(NsharpConstants.lineNameArray[NsharpConstants.LINE_VIRTUAL_TEMP]);
                            target.drawWireframeShape(vtempTraceCurveRscShape, lp.getLineColor(), lp.getLineWidth(),
                                    lp.getLineStyle(), font10);

                            // virtual temperature parcel trace curve
                            if (!overlayIsOn) {
                                lp = linePropertyMap.get(NsharpConstants.lineNameArray[NsharpConstants.LINE_PARCEL_TV]);
                                target.drawWireframeShape(parcelVtTraceRscShape, lp.getLineColor(), lp.getLineWidth(),
                                        lp.getLineStyle(), font10);
                                if (dacpeTraceRscShape != null) {
                                    lp = linePropertyMap.get(NsharpConstants.lineNameArray[NsharpConstants.LINE_DCAPE]);
                                    target.drawWireframeShape(dacpeTraceRscShape, lp.getLineColor(), lp.getLineWidth(),
                                            lp.getLineStyle(), font10);
                                }
                            }
                            // draw effective layer lines
                            // drawEffectiveLayerLines(target);
                            target.drawWireframeShape(effectiveLayerLineShape, NsharpConstants.color_cyan_md, 2,
                                    commonLineStyle, font10);
                        }
                        if (NsharpLoadDialog.getAccess() != null && (NsharpLoadDialog.getAccess()
                                .getActiveLoadSoundingType() == NsharpLoadDialog.MODEL_SND
                                || NsharpLoadDialog.getAccess()
                                        .getActiveLoadSoundingType() == NsharpLoadDialog.PFC_SND)) {
                            // plot omega
                            drawOmega();
                        }
                    }
                }
                if (plotInteractiveTemp == true) {
                    if (currentSkewTEditMode == NsharpConstants.SKEWT_EDIT_MODE_EDITPOINT) {
                        plotNsharpInteractiveEditingTemp(target, currentZoomLevel, world, NsharpConstants.color_white);
                    } else if (currentSkewTEditMode == NsharpConstants.SKEWT_EDIT_MODE_MOVELINE) {
                        plotNsharpMovingTempLine(target, world, NsharpConstants.color_white);
                    }

                }
                target.clearClippingPlane();

                // Wind Barb
                if ((graphConfigProperty != null && graphConfigProperty.isWindBarb() == true)
                        || graphConfigProperty == null) {
                    double xPos = skewTBackground.getWindBarbXPosition();
                    if (overlayIsOn == true && this.previousSoundingLys != null) {
                        drawNsharpWindBarb(target, currentZoomLevel, world, linePropertyMap
                                .get(NsharpConstants.lineNameArray[NsharpConstants.LINE_OVERLAY1]).getLineColor(),
                                this.soundingLys, xPos, 100);
                        if (!previousSoundingLys.equals(soundingLys)) {
                            drawNsharpWindBarb(target, currentZoomLevel, world,
                                    linePropertyMap.get(NsharpConstants.lineNameArray[NsharpConstants.LINE_OVERLAY2])
                                            .getLineColor(),
                                    this.previousSoundingLys, xPos - NsharpResourceHandler.BARB_LENGTH, 100);
                        }
                    } else {
                        if (!compareStnIsOn && !compareTmIsOn && !compareSndIsOn) {
                            drawNsharpWindBarb(target, currentZoomLevel, world, graphConfigProperty.getWindBarbColor(),
                                    this.soundingLys, xPos, 100);
                        } else {
                            int currentTimeListIndex = rscHandler.getCurrentTimeElementListIndex();
                            int currentStnListIndex = rscHandler.getCurrentStnElementListIndex();
                            int currentSndListIndex = rscHandler.getCurrentSndElementListIndex();
                            List<NsharpOperationElement> stnElemList = rscHandler.getStnElementList();
                            List<NsharpOperationElement> timeElemList = rscHandler.getTimeElementList();
                            List<NsharpOperationElement> sndElemList = rscHandler.getSndElementList();
                            List<List<List<NsharpSoundingElementStateProperty>>> stnTimeSndTable = rscHandler
                                    .getStnTimeSndTable();
                            if (compareTmIsOn && currentStnListIndex >= 0 && currentSndListIndex >= 0) {
                                int colorIndex;
                                for (NsharpOperationElement elm : timeElemList) {
                                    if (elm.getActionState() == NsharpConstants.ActState.ACTIVE
                                            && stnTimeSndTable.get(currentStnListIndex).get(timeElemList.indexOf(elm))
                                                    .get(currentSndListIndex) != null) {
                                        List<NcSoundingLayer> soundingLayeys = stnTimeSndTable.get(currentStnListIndex)
                                                .get(timeElemList.indexOf(elm)).get(currentSndListIndex).getSndLyLst();
                                        colorIndex = stnTimeSndTable.get(currentStnListIndex)
                                                .get(timeElemList.indexOf(elm)).get(currentSndListIndex)
                                                .getCompColorIndex();
                                        NsharpLineProperty lp = linePropertyMap
                                                .get(NsharpConstants.lineNameArray[colorIndex]);
                                        drawNsharpWindBarb(target, currentZoomLevel, world, lp.getLineColor(),
                                                soundingLayeys, xPos, 100);
                                    }
                                }
                            } else if (compareStnIsOn && currentTimeListIndex >= 0 && currentSndListIndex >= 0) {
                                int colorIndex;
                                for (NsharpOperationElement elm : stnElemList) {
                                    if (elm.getActionState() == NsharpConstants.ActState.ACTIVE
                                            && stnTimeSndTable.get(stnElemList.indexOf(elm)).get(currentTimeListIndex)
                                                    .get(currentSndListIndex) != null) {
                                        List<NcSoundingLayer> soundingLayeys = stnTimeSndTable
                                                .get(stnElemList.indexOf(elm)).get(currentTimeListIndex)
                                                .get(currentSndListIndex).getSndLyLst();
                                        colorIndex = stnTimeSndTable.get(stnElemList.indexOf(elm))
                                                .get(currentTimeListIndex).get(currentSndListIndex).getCompColorIndex();
                                        NsharpLineProperty lp = linePropertyMap
                                                .get(NsharpConstants.lineNameArray[colorIndex]);
                                        drawNsharpWindBarb(target, currentZoomLevel, world, lp.getLineColor(),
                                                soundingLayeys, xPos, 100);
                                    }
                                }
                            } else if (compareSndIsOn && currentStnListIndex >= 0 && currentTimeListIndex >= 0) {
                                int colorIndex;

                                List<NsharpResourceHandler.CompSndSelectedElem> sndCompElementList = rscHandler
                                        .getCompSndSelectedElemList();
                                for (NsharpResourceHandler.CompSndSelectedElem compElem : sndCompElementList) {
                                    NsharpSoundingElementStateProperty elemProp = stnTimeSndTable
                                            .get(compElem.getStnIndex()).get(compElem.getTimeIndex())
                                            .get(compElem.getSndIndex());
                                    if (sndElemList.get(compElem.getSndIndex())
                                            .getActionState() == NsharpConstants.ActState.ACTIVE && elemProp != null) {
                                        List<NcSoundingLayer> soundingLayeys = elemProp.getSndLyLst();
                                        colorIndex = elemProp.getCompColorIndex();
                                        NsharpLineProperty lp = linePropertyMap
                                                .get(NsharpConstants.lineNameArray[colorIndex]);
                                        drawNsharpWindBarb(target, currentZoomLevel, world, lp.getLineColor(),
                                                soundingLayeys, xPos, 100);
                                    }
                                }
                            }
                        }
                    }
                }
                if (rscHandler.isGoodData()) {
                    drawHeightMark(target);
                    // draw EL, LFC, LCL, FZL, -20C, -30C lines
                    if (lclShape != null) {
                        target.drawWireframeShape(lclShape, NsharpConstants.color_green, 2, LineStyle.SOLID, font9);
                    }
                    if (elShape != null) {
                        target.drawWireframeShape(elShape, NsharpConstants.color_magenta, 2, LineStyle.SOLID, font9);
                    }
                    if (mplShape != null) {
                        target.drawWireframeShape(mplShape, NsharpConstants.color_red, 2, LineStyle.SOLID, font9);
                    }
                    if (lfcShape != null) {
                        target.drawWireframeShape(lfcShape, NsharpConstants.color_yellow, 2, LineStyle.SOLID, font9);
                    }

                    if (lrmShape != null) {
                        target.drawWireframeShape(lrmShape, weatherDataStore.getLrmColor(), 2, LineStyle.SOLID, font9);
                    }

                    NsharpConstants.SPCGraph leftGraph = NsharpPaletteWindow.getLeftGraph();
                    NsharpConstants.SPCGraph rightGraph = NsharpPaletteWindow.getRightGraph();

                    if (fzlShape != null) {
                        if ((leftGraph != NsharpConstants.SPCGraph.WINTER)
                                && (rightGraph != NsharpConstants.SPCGraph.WINTER)) {
                            target.drawWireframeShape(fzlShape, NsharpConstants.color_cyan, 2, LineStyle.SOLID, font9);
                        }
                    }
                    if (dendriticShape != null) {
                        if ((leftGraph == NsharpConstants.SPCGraph.WINTER)
                                || (rightGraph == NsharpConstants.SPCGraph.WINTER)) {
                            target.drawWireframeShape(dendriticShape, NsharpConstants.color_gold, 2, LineStyle.SOLID,
                                    font9);
                        }
                    }
                    if (frzShape != null) {
                        if ((leftGraph == NsharpConstants.SPCGraph.WINTER)
                                || (rightGraph == NsharpConstants.SPCGraph.WINTER)) {
                            target.drawWireframeShape(frzShape, NsharpConstants.color_orangered, 2, LineStyle.SOLID,
                                    font9);
                        }
                    }
                    if (wbzShape != null) {
                        if ((leftGraph == NsharpConstants.SPCGraph.WINTER)
                                || (rightGraph == NsharpConstants.SPCGraph.WINTER)) {
                            target.drawWireframeShape(wbzShape, NsharpConstants.color_lawngreen, 2, LineStyle.SOLID,
                                    font9);
                        }
                    }

                }
                drawNsharpFileNameAndSampling(target, currentZoomLevel);
                // draw cursor data
                if (cursorInSkewT == true && rscHandler.isGoodData()) {
                    if ((curseToggledFontLevel < CURSER_STRING_OFF)
                            && (cursorTopWindBarb == false || windBarbMagnify == false)) {
                        drawNsharpSkewtCursorData(target);
                    }
                }

            } // end of currentGraphMode= NsharpConstants.GRAPH_SKEWT
            else if (currentGraphMode == NsharpConstants.GRAPH_ICING && rscHandler.isGoodData()) {
                paintIcing(currentZoomLevel, target);
            } else if (currentGraphMode == NsharpConstants.GRAPH_TURB && rscHandler.isGoodData()) {
                paintTurbulence(currentZoomLevel, target);
            }

        }
    }

    @Override
    protected void initInternal(IGraphicsTarget target) throws VizException {
        super.initInternal(target);
        currentCanvasBoundWidth = NsharpConstants.SKEWT_PANE_REC_WIDTH;
        currentCanvasBoundHeight = NsharpConstants.SKEWT_PANE_REC_HEIGHT;
        myDefaultCanvasWidth = NsharpConstants.SKEWT_PANE_REC_WIDTH;
        myDefaultCanvasHeight = NsharpConstants.SKEWT_PANE_REC_HEIGHT;
        this.rectangle = new Rectangle(skewtXOrig, skewtYOrig, skewtWidth, skewtHeight);
        pe = new PixelExtent(this.rectangle);
        world = new NsharpWGraphics(this.rectangle);

        world.setWorldCoordinates(NsharpConstants.left, NsharpConstants.top, NsharpConstants.right,
                NsharpConstants.bottom);
        skewTBackground.initInternal(target);
        icingBackground.initInternal(target);
        turbBackground.initInternal(target);
        titleBoxShape = target.createWireframeShape(false, descriptor);
        titleBoxShape.allocate(8);
        if (rscHandler != null) {
            currentGraphMode = rscHandler.getCurrentGraphMode();
        } else {
            currentGraphMode = NsharpConstants.GRAPH_SKEWT;
        }
        createRscWireFrameShapes();

    }

    public void updateDynamicData(Coordinate c) throws VizException {
        this.cursorCor = c;
        try {
            if (skewTBackground.contains(c)) {
                c = NsharpWxMath.reverseSkewTXY(skewTBackground.getWorld().unMap(c.x, c.y));
                double p_mb = c.y;
                double t_C = c.x; // Celsius
                double t_F = celciusToFahrenheit.convert(c.x);
                double theta = celciusToKelvin.convert(NsharpLibThermo.theta((float) p_mb, (float) t_C, 1000f));
                double wtheta = celciusToKelvin.convert(NsharpLibThermo.thetaw((float) p_mb, (float) t_C, (float) t_C));
                double etheta = celciusToKelvin.convert(NsharpLibThermo.thetae((float) p_mb, (float) t_C, (float) t_C));
                double mixRatio = NsharpLibThermo.mixratio((float) p_mb, (float) t_C);
                dPressure = p_mb;

                sPressure = String.format("%.0f mb", p_mb, NsharpConstants.THETA_SYMBOL);
                sTemperatureC = String.format("%.1f%cC", t_C, NsharpConstants.DEGREE_SYMBOL);
                sTemperatureF = String.format("%.1f%cF", t_F, NsharpConstants.DEGREE_SYMBOL);

                sThetaInK = String.format("%c=%.0fK", NsharpConstants.THETA_SYMBOL, theta);
                sWThetaInK = String.format("%cw=%.0fK", NsharpConstants.THETA_SYMBOL, wtheta);
                sEThetaInK = String.format("%ce=%.0fK", NsharpConstants.THETA_SYMBOL, etheta);
                sMixingRatio = String.format("%.0fg/Kg", mixRatio);
            }
        } catch (Exception e) {
            UFStatus.getHandler().handle(Priority.PROBLEM, "Exception translating coordinate", e);
        }
        return;
    }

    // Creating real temperature parcel trace shape list - considering
    // normal/comparison/overlay scenarios
    public void createRscParcelRtTraceShapesList(int parcelType, float userPre) {
        if (target == null) {
            return;
        }
        if (parcelRtShapeList.size() > 0) {
            for (NsharpShapeAndLineProperty shapeColor : parcelRtShapeList) {
                shapeColor.getShape().dispose();
            }
            parcelRtShapeList.clear();
        }

        int currentTimeListIndex = rscHandler.getCurrentTimeElementListIndex();
        int currentStnListIndex = rscHandler.getCurrentStnElementListIndex();
        int currentSndListIndex = rscHandler.getCurrentSndElementListIndex();
        List<NsharpOperationElement> stnElemList = rscHandler.getStnElementList();
        List<NsharpOperationElement> timeElemList = rscHandler.getTimeElementList();
        List<NsharpOperationElement> sndElemList = rscHandler.getSndElementList();
        List<List<List<NsharpSoundingElementStateProperty>>> stnTimeSndTable = rscHandler.getStnTimeSndTable();
        if (rscHandler.isCompareStnIsOn() && currentTimeListIndex >= 0 && currentSndListIndex >= 0) {
            for (NsharpOperationElement elm : stnElemList) {
                if (elm.getActionState() == NsharpConstants.ActState.ACTIVE && stnTimeSndTable
                        .get(stnElemList.indexOf(elm)).get(currentTimeListIndex).get(currentSndListIndex) != null) {
                    List<NcSoundingLayer> soundingLayeys = stnTimeSndTable.get(stnElemList.indexOf(elm))
                            .get(currentTimeListIndex).get(currentSndListIndex).getSndLyLst();
                    int colorIndex = stnTimeSndTable.get(stnElemList.indexOf(elm)).get(currentTimeListIndex)
                            .get(currentSndListIndex).getCompColorIndex();
                    NsharpLineProperty lp = linePropertyMap.get(NsharpConstants.lineNameArray[colorIndex]);
                    IWireframeShape shape = createRTParcelTraceShapes(parcelType, userPre, soundingLayeys);
                    NsharpShapeAndLineProperty shNLp = new NsharpShapeAndLineProperty();
                    shNLp.setShape(shape);
                    shNLp.setLp(lp);
                    parcelRtShapeList.add(shNLp);
                }
            }
        } else if (rscHandler.isCompareTmIsOn() && currentStnListIndex >= 0 && currentSndListIndex >= 0) {
            for (NsharpOperationElement elm : timeElemList) {
                if (elm.getActionState() == NsharpConstants.ActState.ACTIVE && stnTimeSndTable.get(currentStnListIndex)
                        .get(timeElemList.indexOf(elm)).get(currentSndListIndex) != null) {
                    List<NcSoundingLayer> soundingLayeys = stnTimeSndTable.get(currentStnListIndex)
                            .get(timeElemList.indexOf(elm)).get(currentSndListIndex).getSndLyLst();
                    int colorIndex = stnTimeSndTable.get(currentStnListIndex).get(timeElemList.indexOf(elm))
                            .get(currentSndListIndex).getCompColorIndex();
                    NsharpLineProperty lp = linePropertyMap.get(NsharpConstants.lineNameArray[colorIndex]);
                    IWireframeShape shape = createRTParcelTraceShapes(parcelType, userPre, soundingLayeys);
                    NsharpShapeAndLineProperty shNLp = new NsharpShapeAndLineProperty();
                    shNLp.setShape(shape);
                    shNLp.setLp(lp);
                    parcelRtShapeList.add(shNLp);
                }
            }
        } else if (rscHandler.isCompareSndIsOn() && currentStnListIndex >= 0 && currentTimeListIndex >= 0) {
            for (NsharpOperationElement elm : sndElemList) {
                if (elm.getActionState() == NsharpConstants.ActState.ACTIVE && stnTimeSndTable.get(currentStnListIndex)
                        .get(currentTimeListIndex).get(sndElemList.indexOf(elm)) != null) {
                    List<NcSoundingLayer> soundingLayeys = stnTimeSndTable.get(currentStnListIndex)
                            .get(currentTimeListIndex).get(sndElemList.indexOf(elm)).getSndLyLst();
                    int colorIndex = stnTimeSndTable.get(currentStnListIndex).get(currentTimeListIndex)
                            .get(sndElemList.indexOf(elm)).getCompColorIndex();
                    NsharpLineProperty lp = linePropertyMap.get(NsharpConstants.lineNameArray[colorIndex]);
                    IWireframeShape shape = createRTParcelTraceShapes(parcelType, userPre, soundingLayeys);
                    NsharpShapeAndLineProperty shNLp = new NsharpShapeAndLineProperty();
                    shNLp.setShape(shape);
                    shNLp.setLp(lp);
                    parcelRtShapeList.add(shNLp);
                }
            }
        } else if (rscHandler.isOverlayIsOn() == true) {
            previousSoundingLys = rscHandler.getPreviousSoundingLys();
            IWireframeShape shape = createRTParcelTraceShapes(parcelType, userPre, this.soundingLys);
            NsharpShapeAndLineProperty shNLp = new NsharpShapeAndLineProperty();
            shNLp.setShape(shape);
            shNLp.setLp(linePropertyMap.get(NsharpConstants.lineNameArray[NsharpConstants.LINE_OVERLAY1]));
            parcelRtShapeList.add(shNLp);
            if (this.previousSoundingLys != null && !previousSoundingLys.equals(soundingLys)) {
                shape = createRTParcelTraceShapes(parcelType, userPre, previousSoundingLys);
                shNLp = new NsharpShapeAndLineProperty();
                shNLp.setShape(shape);
                shNLp.setLp(linePropertyMap.get(NsharpConstants.lineNameArray[NsharpConstants.LINE_OVERLAY2]));
                parcelRtShapeList.add(shNLp);
            }
        } else {
            IWireframeShape shape = createRTParcelTraceShapes(parcelType, userPre, this.soundingLys);
            NsharpShapeAndLineProperty shNLp = new NsharpShapeAndLineProperty();
            shNLp.setShape(shape);
            shNLp.setLp(linePropertyMap.get(NsharpConstants.lineNameArray[NsharpConstants.LINE_PARCEL]));
            parcelRtShapeList.add(shNLp);
        }
    }

    // Creating real temperature parcel trace shape
    private IWireframeShape createRTParcelTraceShapes(int parcelType, float userPre,
            List<NcSoundingLayer> soundingLays) {
        IWireframeShape parcelRtShape;
        parcelRtShape = target.createWireframeShape(false, descriptor);
        parcelRtShape.allocate(40);
        // the input soundingLays list may not be current sounding list,
        // e.g. when performing "comparing stn/timeline/src or overlay",
        // therefore need to re-do parcel computation
        // call define_parcel() with parcel type and user defined
        // pressure (if user defined it)
        LParcelValues lparcelVs = NsharpLibSkparams.define_parcel(soundingLays, parcelType, userPre);
        float sfctemp, sfcdwpt, sfcpres;
        sfctemp = lparcelVs.getTemp();
        sfcdwpt = lparcelVs.getDwpt();
        sfcpres = lparcelVs.getPres();

        LayerParameters dryLiftLayer = NsharpLibThermo.drylift(sfcpres, sfctemp, sfcdwpt);

        if (dryLiftLayer != null) {
            Coordinate a1 = NsharpWxMath.getSkewTXY(sfcpres, sfctemp);
            a1.x = world.mapX(a1.x);
            a1.y = world.mapY(a1.y);
            Coordinate a2 = NsharpWxMath.getSkewTXY(dryLiftLayer.getPressure(), dryLiftLayer.getTemperature());
            a2.x = world.mapX(a2.x);
            a2.y = world.mapY(a2.y);

            double[][] alines = { { a1.x, a1.y }, { a2.x, a2.y } };
            parcelRtShape.addLineSegment(alines);
            a1 = a2;

            float t3;
            for (float i = dryLiftLayer.getPressure() - 50; i >= 100; i = i - 50) {
                t3 = NsharpLibThermo.wetlift(dryLiftLayer.getPressure(), dryLiftLayer.getTemperature(), i);

                a2 = NsharpWxMath.getSkewTXY(i, t3);
                a2.x = world.mapX(a2.x);
                a2.y = world.mapY(a2.y);
                double[][] alines1 = { { a1.x, a1.y }, { a2.x, a2.y } };
                parcelRtShape.addLineSegment(alines1);
                a1 = a2;
            }

            t3 = NsharpLibThermo.wetlift(dryLiftLayer.getPressure(), dryLiftLayer.getTemperature(), 100);

            a2 = NsharpWxMath.getSkewTXY(100, t3);
            a2.x = world.mapX(a2.x);
            a2.y = world.mapY(a2.y);
            double[][] alines1 = { { a1.x, a1.y }, { a2.x, a2.y } };
            parcelRtShape.addLineSegment(alines1);

            parcelRtShape.compile();
        }

        return parcelRtShape;
    }

    // Creating Virtual Temperature parcel and DCAPE trace Shapes
    public void createRscParcelTraceShapes(int parcelType, float userPre) {
        if (target == null) {
            return;
        }
        if (parcelVtTraceRscShape != null) {
            parcelVtTraceRscShape.dispose();
            parcelVtTraceRscShape = null;
        }
        if (dacpeTraceRscShape != null) {
            dacpeTraceRscShape.dispose();
            dacpeTraceRscShape = null;
        }
        parcelVtTraceRscShape = target.createWireframeShape(false, descriptor);
        parcelVtTraceRscShape.allocate(40);
        dacpeTraceRscShape = target.createWireframeShape(false, descriptor);
        dacpeTraceRscShape.allocate(40);

        LParcelValues lparcelVs = NsharpLibSkparams.define_parcel(soundingLys, parcelType, userPre);

        float parcelTemp, parcelDwpt, parcelPres;
        parcelTemp = lparcelVs.getTemp();
        parcelDwpt = lparcelVs.getDwpt();
        parcelPres = lparcelVs.getPres();

        float vtemp = NsharpLibThermo.virtemp(parcelPres, parcelTemp, parcelDwpt);
        Coordinate c1 = NsharpWxMath.getSkewTXY(parcelPres, vtemp);
        c1.x = world.mapX(c1.x);
        c1.y = world.mapY(c1.y);
        Coordinate c2;
        LayerParameters dryLiftLayer = NsharpLibThermo.drylift(parcelPres, parcelTemp, parcelDwpt);
        if (dryLiftLayer != null) {
            vtemp = NsharpLibThermo.virtemp(dryLiftLayer.getPressure(), dryLiftLayer.getTemperature(),
                    dryLiftLayer.getTemperature());
            c2 = NsharpWxMath.getSkewTXY(dryLiftLayer.getPressure(), vtemp);
            c2.x = world.mapX(c2.x);
            c2.y = world.mapY(c2.y);

            double[][] lines = { { c1.x, c1.y }, { c2.x, c2.y } };
            parcelVtTraceRscShape.addLineSegment(lines);
            c1 = c2;

            float t3;
            for (float i = dryLiftLayer.getPressure() - 50; i >= 100; i = i - 50) {
                t3 = NsharpLibThermo.wetlift(dryLiftLayer.getPressure(), dryLiftLayer.getTemperature(), i);
                vtemp = NsharpLibThermo.virtemp(i, t3, t3);
                c2 = NsharpWxMath.getSkewTXY(i, vtemp);
                c2.x = world.mapX(c2.x);
                c2.y = world.mapY(c2.y);

                double[][] lines1 = { { c1.x, c1.y }, { c2.x, c2.y } };
                parcelVtTraceRscShape.addLineSegment(lines1);
                c1 = c2;

            }

            t3 = NsharpLibThermo.wetlift(dryLiftLayer.getPressure(), dryLiftLayer.getTemperature(), 100);
            vtemp = NsharpLibThermo.virtemp(100, t3, t3);
            c2 = NsharpWxMath.getSkewTXY(100, vtemp);
            c2.x = world.mapX(c2.x);
            c2.y = world.mapY(c2.y);

            double[][] lines2 = { { c1.x, c1.y }, { c2.x, c2.y } };
            parcelVtTraceRscShape.addLineSegment(lines2);

            parcelVtTraceRscShape.compile();
        }

        // DCAPE------------------
        // Downdraft Convective Available Potential Energy - DCAPE
        // see trace_dcape() in xwvid1.c for original source
        /* ----- Find highest observation in layer ----- */
        float mine, minep, tp1, tp2, te2, pe1, pe2, h1, h2;
        float surfpressure = NsharpLibBasics.sfcPressure(soundingLys);
        int p5 = 0;
        if (NsharpLibBasics.qc(surfpressure)) {
            NcSoundingLayer layer;
            for (int i = this.soundingLys.size() - 1; i >= 0; i--) {
                layer = this.soundingLys.get(i);
                if (layer.getPressure() > surfpressure - 400) {
                    p5 = i;
                    break;
                }
            }
        }

        /* ----- Find min ThetaE layer ----- */
        mine = 1000;
        minep = -999;
        for (int i = 0; i < p5; i++) {
            NcSoundingLayer layer = this.soundingLys.get(i);
            if (NsharpLibBasics.qc(layer.getDewpoint())
                    && NsharpLibBasics.qc(NsharpLibBasics.i_temp(soundingLys, layer.getPressure() + 100))) {
                float meanThetae = NsharpLibSkparams.Mean_thetae(soundingLys, layer.getPressure(),
                        layer.getPressure() - 100);
                if (NsharpLibBasics.qc(meanThetae) && meanThetae < mine) {
                    mine = meanThetae;
                    minep = layer.getPressure() - 50;
                }
            }
        }
        if (minep < 0) {
            return;
        }

        float upper = minep;
        dacpeTraceRscShape = target.createWireframeShape(false, descriptor);
        dacpeTraceRscShape.allocate(40);

        /* ----- Find highest observation in layer ----- */
        NcSoundingLayer layer;
        int uptr = 0;
        for (int i = this.soundingLys.size() - 1; i >= 0; i--) {
            layer = this.soundingLys.get(i);
            if (layer.getPressure() > upper) {
                uptr = i;
                break;
            }
        }

        /* ----- Define parcel starting point ----- */
        tp1 = NsharpLibThermo.wetbulb(upper, NsharpLibBasics.i_temp(soundingLys, upper),
                NsharpLibBasics.i_dwpt(soundingLys, upper));
        pe1 = upper;

        h1 = NsharpLibBasics.i_hght(soundingLys, pe1);
        float ent2;

        c1 = NsharpWxMath.getSkewTXY(pe1, tp1);
        c1.x = world.mapX(c1.x);
        c1.y = world.mapY(c1.y);
        for (int i = uptr; i >= 0; i--) {
            layer = this.soundingLys.get(i);
            pe2 = layer.getPressure();
            if (pe2 > surfpressure) {
                break;
            }
            te2 = layer.getTemperature();
            h2 = layer.getGeoHeight();
            tp2 = NsharpLibThermo.wetlift(pe1, tp1, pe2);
            if ((NsharpLibBasics.qc(te2)) && NsharpLibBasics.qc(tp2)) {
                /* Account for Entrainment */
                ent2 = ENTRAIN_DEFAULT * ((h1 - h2) / 1000);
                tp2 = tp2 + ((te2 - tp2) * ent2);
                c2 = NsharpWxMath.getSkewTXY(pe2, tp2);
                c2.x = world.mapX(c2.x);
                c2.y = world.mapY(c2.y);

                double[][] lines3 = { { c1.x, c1.y }, { c2.x, c2.y } };
                dacpeTraceRscShape.addLineSegment(lines3);
                c1 = c2;
                h1 = h2;
                pe1 = pe2;
                tp1 = tp2;
            }
        }

        dacpeTraceRscShape.compile();
    }

    private void drawNoDataMessage(IGraphicsTarget target) {
        IExtent ext = descriptor.getRenderableDisplay().getExtent();
        double xmin = ext.getMinX(); // Extent's viewable envelope min x and y
        double xmax = ext.getMaxX();
        double xDefault = world.mapX(NsharpConstants.left);
        if (xmin < xDefault) {
            xmin = xDefault;
        }
        double x = xmin + 15 * currentZoomLevel * xRatio;
        x = (xmax - xmin) / 4;
        double y = world.mapY(NsharpWxMath.getSkewTXY(300, 0).y);
        try {
            target.drawString(font12, "Data is not loaded at selected current time line/station/source", x, y, 0.0,
                    TextStyle.BOXED, NsharpConstants.color_red, HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE,
                    null);
        } catch (VizException e) {
            statusHandler.handle(Priority.PROBLEM, "drawNoDataMessage exception:", e);
        }
    }

    // Chin: to handle dynamically moving height mark within viewable zone when
    // zooming, I could not use wireframeShape successfully
    // It will chop off lower part of marks. Therefore use this draw function.
    private void drawHeightMark(IGraphicsTarget target) {
        // plot meter scales...
        if (soundingLys.size() <= 0) {
            return;
        }
        IExtent ext = descriptor.getRenderableDisplay().getExtent();
        double xmin = ext.getMinX(); // Extent's viewable envelope min x and y
        double xDefault = world.mapX(NsharpConstants.left);
        if (xmin < xDefault) {
            xmin = xDefault;
        }
        double dispX1 = xmin + 15 * currentZoomLevel * xRatio;
        double dispX2 = xmin + 30 * currentZoomLevel * xRatio;
        // scale the offset according to zoom
        double dispYoffset = 5 * currentZoomLevel * yRatio;
        // Chin 08/04/2014, fixed surface height plotting bug
        // also fixed to draw height mraker based on AGL (i.e. above surface
        // level)
        int sfcIndex = NsharpLibBasics.sfcIndex(soundingLys);
        int sfcAsl = 0;
        if (sfcIndex >= 0) {
            double y = world.mapY(NsharpWxMath.getSkewTXY(NsharpLibBasics.sfcPressure(soundingLys), 0).y);
            try {
                target.drawLine(dispX1, y, 0.0, dispX2, y, 0.0, NsharpConstants.color_red, 1);
                sfcAsl = (int) (NsharpLibBasics.sfcHeight(soundingLys));

                target.drawString(font10, "SFC(" + sfcAsl + "m)", dispX2, y - dispYoffset, 0.0, TextStyle.NORMAL,
                        NsharpConstants.color_red, HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, null);
            } catch (VizException e) {
                statusHandler.handle(Priority.PROBLEM, "drawHeightMark exception1:", e);
            }
        }
        for (int j = 0; j < NsharpConstants.HEIGHT_LEVEL_METERS.length; j++) {
            int meters = NsharpConstants.HEIGHT_LEVEL_METERS[j];
            // plot the meters scale
            double pressure = NsharpLibBasics.i_pres(soundingLys, meters + sfcAsl);
            double y = world.mapY(NsharpWxMath.getSkewTXY(pressure, 0).y);
            try {
                target.drawLine(dispX1, y, 0.0, dispX2, y, 0.0, NsharpConstants.color_red, 1);

                target.drawString(font10, Integer.toString(meters / 1000) + " km", dispX2, y - dispYoffset, 0.0,
                        TextStyle.NORMAL, NsharpConstants.color_red, HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE,
                        null);
            } catch (VizException e) {
                statusHandler.handle(Priority.PROBLEM, "drawHeightMark exception2:", e);
            }
        }

    }

    public void createRscwetBulbTraceShape() {
        if (wetBulbTraceRscShape != null) {
            wetBulbTraceRscShape.dispose();
            wetBulbTraceRscShape = null;
        }
        wetBulbTraceRscShape = target.createWireframeShape(false, descriptor);
        wetBulbTraceRscShape.allocate(soundingLys.size() * 2);
        float t1;

        Coordinate c2 = null;
        Coordinate c1;
        // draw trace

        for (NcSoundingLayer layer : this.soundingLys) {
            if (layer.getDewpoint() > -200 && layer.getPressure() >= 100) {
                t1 = NsharpLibThermo.wetbulb(layer.getPressure(), layer.getTemperature(), layer.getDewpoint());

                c1 = NsharpWxMath.getSkewTXY(layer.getPressure(), t1);
                c1.x = world.mapX(c1.x);
                c1.y = world.mapY(c1.y);
                if (c2 != null) {

                    double[][] lines = { { c1.x, c1.y }, { c2.x, c2.y } };
                    wetBulbTraceRscShape.addLineSegment(lines);

                }
                c2 = c1;
            }
        }
        wetBulbTraceRscShape.compile();
    }

    private void createTurbulenceShapes(NsharpWGraphics world) {
        if (turbLnShape != null) {
            turbLnShape.dispose();
        }
        turbLnShape = target.createWireframeShape(false, descriptor);

        turbLnShape.allocate(this.soundingLys.size() * 2);
        turbWindShearShape = target.createWireframeShape(false, descriptor);
        turbWindShearShape.allocate(this.soundingLys.size() * 2);
        Coordinate pointALn = null;
        Coordinate pointBLn = null;
        Coordinate pointAWsh = null;
        Coordinate pointBWsh = null;
        double g = 9.8f, Ri;
        double t0, t1, v0, v1, u0, u1, windshear0, windshearsqrd, tke_windshear_prod;
        double pressure0 = 0, pressure1, midpressure0, p, high0 = 0, high1;
        double theta1 = 0, theta0 = 0, dthetadz0, meanTheta;
        boolean first = true;
        NcSoundingLayer layer0, layer1;
        for (int i = 0; i < soundingLys.size() - 1; i++) {
            layer0 = soundingLys.get(i);
            pressure0 = layer0.getPressure();
            t0 = layer0.getTemperature();
            high0 = layer0.getGeoHeight();
            layer1 = soundingLys.get(i + 1);
            t1 = layer1.getTemperature();
            high1 = layer1.getGeoHeight();
            if (!NsharpLibBasics.qc((float) t0) || !NsharpLibBasics.qc((float) t1)
                    || pressure0 <= NsharpConstants.TURBULENCE_PRESSURE_LEVEL_TOP) {
                continue;
            }
            pressure1 = layer1.getPressure();
            v0 = NsharpLibBasics.i_wndv(soundingLys, (float) pressure0);
            v1 = NsharpLibBasics.i_wndv(soundingLys, (float) pressure1);
            u0 = NsharpLibBasics.i_wndu(soundingLys, (float) pressure0);
            u1 = NsharpLibBasics.i_wndu(soundingLys, (float) pressure1);
            windshear0 = Math.sqrt((u1 - u0) * (u1 - u0) + (v1 - v0) * (v1 - v0)) * .51479 / (high1 - high0);
            midpressure0 = (pressure1 + pressure0) / 2;
            theta0 = NsharpLibThermo.theta((float) pressure0, (float) t0, 1000f) + 273.15;
            theta1 = NsharpLibThermo.theta((float) pressure1, (float) t1, 1000f) + 273.15;
            meanTheta = (theta1 + theta0) / 2.0f;
            dthetadz0 = (theta1 - theta0) / (high1 - high0);
            if (windshear0 != 0.0) {
                windshearsqrd = (windshear0 * windshear0);
                Ri = (g / meanTheta) * (dthetadz0 / windshearsqrd);
                world.setWorldCoordinates(NsharpConstants.TURBULENCE_LN_RICHARDSON_NUMBER_LEFT,
                        turbBackground.toLogScale(NsharpConstants.TURBULENCE_PRESSURE_LEVEL_TOP),
                        NsharpConstants.TURBULENCE_LN_RICHARDSON_NUMBER_RIGHT,
                        turbBackground.toLogScale(NsharpConstants.TURBULENCE_PRESSURE_LEVEL_BOTTOM));

                pointALn = new Coordinate();
                p = turbBackground.toLogScale(midpressure0);
                pointALn.x = world.mapX(Math.log(Ri));
                pointALn.y = world.mapY(p);
                world.setWorldCoordinates(NsharpConstants.TURBULENCE_WIND_SHEAR_TKE_LEFT,
                        turbBackground.toLogScale(NsharpConstants.TURBULENCE_PRESSURE_LEVEL_TOP),
                        NsharpConstants.TURBULENCE_WIND_SHEAR_TKE_RIGHT,
                        turbBackground.toLogScale(NsharpConstants.TURBULENCE_PRESSURE_LEVEL_BOTTOM));
                pointAWsh = new Coordinate();
                tke_windshear_prod = 0.54 * (high1 - high0) * windshearsqrd;
                pointAWsh.x = world.mapX(tke_windshear_prod * 100);
                pointAWsh.y = world.mapY(p);
                if (!first) {
                    double[][] linesLn = { { pointALn.x, pointALn.y }, { pointBLn.x, pointBLn.y } };
                    double[][] linesWsh = { { pointAWsh.x, pointAWsh.y }, { pointBWsh.x, pointBWsh.y } };
                    turbLnShape.addLineSegment(linesLn);
                    turbWindShearShape.addLineSegment(linesWsh);

                } else {
                    first = false;
                }
                pointBLn = pointALn;
                pointBWsh = pointAWsh;
            }
        }

        turbLnShape.compile();
        turbWindShearShape.compile();
    }

    /*
     * Chin:: NOTE::: This plotting function is based on the algorithm of
     * draw_ICG() at xwvid1.c of AWC Nsharp source code by LARRY J. HINSON
     * AWC/KCMO
     */
    private void createIcingRHShape(NsharpWGraphics world) {
        icingRHShape = target.createWireframeShape(false, descriptor);
        icingRHShape.allocate(this.soundingLys.size() * 2);
        Coordinate c0 = null;
        System.out.println("createIcingRHShape called");
        for (NcSoundingLayer layer : soundingLys) {
            double pressure = layer.getPressure();
            if (pressure >= NsharpConstants.ICING_PRESSURE_LEVEL_TOP
                    && pressure <= NsharpConstants.ICING_PRESSURE_LEVEL_BOTTOM) {
                float relh = NsharpLibThermo.relh(soundingLys, (float) pressure);
                Coordinate c1 = new Coordinate();
                double p = icingBackground.toLogScale(pressure);
                c1.x = world.mapX(relh);
                c1.y = world.mapY(p);
                if (c0 != null) {
                    double[][] lines = { { c0.x, c0.y }, { c1.x, c1.y } };
                    icingRHShape.addLineSegment(lines);
                }
                c0 = c1;
            }
        }

        icingRHShape.compile();
    }

    private void createIcingEPIShape(NsharpWGraphics world) {
        icingEPIShape = target.createWireframeShape(false, descriptor);
        icingEPIShape.allocate(this.soundingLys.size() * 2);
        Coordinate pointA = null;
        Coordinate pointB = null;
        boolean firstround = true;
        double t0, t1;
        double pressure0 = 0, pressure1, midpressure0, p, high0 = 0, high1;
        double const1 = 2500000.0 / 1004.0;
        double theta1 = 0, thetase1, theta0 = 0, thetase0 = 0, mixratio0, mixratio1, dthetasedz0;
        NcSoundingLayer layer0, layer1;
        for (int i = 0; i < soundingLys.size() - 1; i++) {
            layer0 = soundingLys.get(i);
            layer1 = soundingLys.get(i + 1);
            t0 = layer0.getTemperature();
            t1 = layer1.getTemperature();
            pressure0 = layer0.getPressure();
            pressure1 = layer1.getPressure();
            if (!NsharpLibBasics.qc((float) t0) || !NsharpLibBasics.qc((float) t1)
                    || (pressure0 < NsharpConstants.ICING_PRESSURE_LEVEL_TOP
                            && pressure1 < NsharpConstants.ICING_PRESSURE_LEVEL_TOP)) {
                continue;
            }
            theta1 = NsharpLibThermo.theta((float) pressure1, (float) t1, 1000f) + 273.15;
            mixratio1 = NsharpLibThermo.mixratio((float) pressure1, (float) t1);
            thetase1 = theta1 * Math.exp(const1 * mixratio1 * .001 / (t1 + 273.15));
            high1 = layer1.getGeoHeight();
            theta0 = NsharpLibThermo.theta((float) pressure0, (float) t0, 1000f) + 273.15;
            mixratio0 = NsharpLibThermo.mixratio((float) pressure0, (float) t0);
            thetase0 = theta0 * Math.exp(const1 * mixratio0 * .001 / (t0 + 273.15));
            high0 = layer0.getGeoHeight();
            // Do D-Theta-se/dz
            dthetasedz0 = (thetase1 - thetase0) / (high1 - high0) * 1E3;
            midpressure0 = (pressure1 + pressure0) / 2;
            pointA = new Coordinate();
            p = icingBackground.toLogScale(midpressure0);
            pointA.x = world.mapX(dthetasedz0);
            pointA.y = world.mapY(p);
            if (!firstround) {
                double[][] lines = { { pointA.x, pointA.y }, { pointB.x, pointB.y } };
                icingEPIShape.addLineSegment(lines);

            } else {// this is first round, we need two pints for a line
                    // segment. We only have first point now.
                firstround = false;
            }
            pointB = pointA;

        }
        icingEPIShape.compile();
    }

    /*
     * Chin:: NOTE::: This plotting function is based on the algorithm of
     * draw_ICG() at xwvid1.c of AWC Nsharp source code by LARRY J. HINSON
     * AWC/KCMO
     */
    private void createIcingTempShape(NsharpWGraphics world) {
        icingTempShape = target.createWireframeShape(false, descriptor);
        icingTempShape.allocate(this.soundingLys.size() * 2);
        Coordinate c0 = null;

        for (NcSoundingLayer layer : soundingLys) {
            double t = layer.getTemperature();
            double pressure = layer.getPressure();
            if (pressure >= NsharpConstants.ICING_PRESSURE_LEVEL_TOP
                    && pressure <= NsharpConstants.ICING_PRESSURE_LEVEL_BOTTOM) {

                Coordinate c1 = new Coordinate();
                double p = icingBackground.toLogScale(pressure);
                c1.x = world.mapX(t);
                c1.y = world.mapY(p);
                if (c0 != null) {
                    double[][] lines = { { c0.x, c0.y }, { c1.x, c1.y } };
                    icingTempShape.addLineSegment(lines);
                }
                c0 = c1;
            }
        }

        icingTempShape.compile();
    }

    private void createRscPressTempCurveShape(NsharpWGraphics WGc, List<NcSoundingLayer> soundingLays,
            NsharpLineProperty lineP, IGraphicsTarget target) {
        IWireframeShape shapeT = target.createWireframeShape(false, descriptor);
        shapeT.allocate(soundingLays.size() * 2);
        IWireframeShape shapeD = target.createWireframeShape(false, descriptor);
        shapeD.allocate(soundingLays.size() * 2);
        NsharpShapeAndLineProperty shNcolorT = new NsharpShapeAndLineProperty();
        NsharpShapeAndLineProperty shNcolorD = new NsharpShapeAndLineProperty();
        double maxPressure = NsharpWxMath.reverseSkewTXY(new Coordinate(0, WGc.getWorldYmax())).y;
        double minPressure = NsharpWxMath.reverseSkewTXY(new Coordinate(0, WGc.getWorldYmin())).y;
        boolean drawTemp = true, drawDew = true;
        graphConfigProperty = rscHandler.getGraphConfigProperty();
        if (graphConfigProperty != null) {
            drawTemp = graphConfigProperty.isTemp();
            drawDew = graphConfigProperty.isDewp();
        }
        Coordinate c0 = null, c01 = null;

        for (NcSoundingLayer layer : soundingLays) {
            double t, d;
            t = layer.getTemperature();
            d = layer.getDewpoint();

            double pressure = layer.getPressure();
            if (NsharpLibBasics.qc((float) t) && pressure >= minPressure && pressure <= maxPressure) {

                Coordinate c1 = NsharpWxMath.getSkewTXY(pressure, t);

                c1.x = WGc.mapX(c1.x);
                c1.y = WGc.mapY(c1.y);
                if (c0 != null) {
                    double[][] lines = { { c0.x, c0.y }, { c1.x, c1.y } };

                    shapeT.addLineSegment(lines);
                }
                c0 = c1;
            }
            if (NsharpLibBasics.qc((float) d) && pressure >= minPressure && pressure <= maxPressure) {

                Coordinate c11 = NsharpWxMath.getSkewTXY(pressure, d);

                c11.x = WGc.mapX(c11.x);
                c11.y = WGc.mapY(c11.y);
                if (c01 != null) {
                    double[][] lines = { { c01.x, c01.y }, { c11.x, c11.y } };
                    shapeD.addLineSegment(lines);
                }
                c01 = c11;
            }
        }

        shNcolorD.setShape(shapeD);
        shNcolorT.setShape(shapeT);

        if (!rscHandler.isOverlayIsOn() && !rscHandler.isCompareStnIsOn() && !rscHandler.isCompareTmIsOn()
                && !rscHandler.isCompareSndIsOn()) {
            // use default color

            if (linePropertyMap != null) {
                shNcolorT.setLp(linePropertyMap.get(NsharpConstants.lineNameArray[NsharpConstants.LINE_TEMP]));
                shNcolorD.setLp(linePropertyMap.get(NsharpConstants.lineNameArray[NsharpConstants.LINE_DEWP]));
            }
        } else {
            shNcolorT.setLp(lineP);
            shNcolorD.setLp(lineP);
        }
        // check draw temp and dew here. It is easier to do this way, otherwise,
        // we have to check it every where
        if (drawTemp) {
            float sfcTemp = NsharpLibBasics.sfcTemperature(soundingLays);
            float sfcPress = NsharpLibBasics.sfcPressure(soundingLays);
            Coordinate cSfc = NsharpWxMath.getSkewTXY(sfcPress, sfcTemp);
            cSfc.x = WGc.mapX(cSfc.x);
            cSfc.y = WGc.mapY(cSfc.y);
            double[] lblXy = { cSfc.x, cSfc.y };
            String sfcTempStr = String.valueOf(sfcTemp);
            shapeT.addLabel(sfcTempStr, lblXy);
            shapeT.compile();
            pressureTempRscShapeList.add(shNcolorT);
        } else {
            shNcolorT.getShape().dispose();
        }
        if (drawDew) {
            float sfcDewP = NsharpLibBasics.sfcDewPoint(soundingLays);
            float sfcPress = NsharpLibBasics.sfcPressure(soundingLays);
            Coordinate cSfc = NsharpWxMath.getSkewTXY(sfcPress, sfcDewP);
            cSfc.x = WGc.mapX(cSfc.x);
            cSfc.y = WGc.mapY(cSfc.y);
            double[] lblXy = { cSfc.x, cSfc.y };
            String sfcDewPStr = String.valueOf(sfcDewP);
            shapeD.addLabel(sfcDewPStr, lblXy);
            shapeD.compile();
            pressureTempRscShapeList.add(shNcolorD);
        } else {
            shNcolorD.getShape().dispose();
        }
    }

    public void createRscPressTempCurveShapeAll(IGraphicsTarget target) {

        if (pressureTempRscShapeList.size() > 0) {
            for (NsharpShapeAndLineProperty shapeColor : pressureTempRscShapeList) {
                shapeColor.getShape().dispose();
            }
            pressureTempRscShapeList.clear();
        }

        int currentTimeListIndex = rscHandler.getCurrentTimeElementListIndex();
        int currentStnListIndex = rscHandler.getCurrentStnElementListIndex();
        int currentSndListIndex = rscHandler.getCurrentSndElementListIndex();
        List<NsharpOperationElement> stnElemList = rscHandler.getStnElementList();
        List<NsharpOperationElement> timeElemList = rscHandler.getTimeElementList();
        List<NsharpOperationElement> sndElemList = rscHandler.getSndElementList();
        List<List<List<NsharpSoundingElementStateProperty>>> stnTimeSndTable = rscHandler.getStnTimeSndTable();
        if (rscHandler.isCompareStnIsOn() && currentTimeListIndex >= 0 && currentSndListIndex >= 0) {
            int colorIndex;
            for (NsharpOperationElement elm : stnElemList) {
                if (elm.getActionState() == NsharpConstants.ActState.ACTIVE && stnTimeSndTable
                        .get(stnElemList.indexOf(elm)).get(currentTimeListIndex).get(currentSndListIndex) != null) {
                    List<NcSoundingLayer> soundingLayeys = stnTimeSndTable.get(stnElemList.indexOf(elm))
                            .get(currentTimeListIndex).get(currentSndListIndex).getSndLyLst();
                    colorIndex = stnTimeSndTable.get(stnElemList.indexOf(elm)).get(currentTimeListIndex)
                            .get(currentSndListIndex).getCompColorIndex();
                    NsharpLineProperty lp = linePropertyMap.get(NsharpConstants.lineNameArray[colorIndex]);
                    createRscPressTempCurveShape(world, soundingLayeys, lp, target);
                }
            }
        } else if (rscHandler.isCompareTmIsOn() && currentStnListIndex >= 0 && currentSndListIndex >= 0) {
            int colorIndex;
            for (NsharpOperationElement elm : timeElemList) {
                if (elm.getActionState() == NsharpConstants.ActState.ACTIVE && stnTimeSndTable.get(currentStnListIndex)
                        .get(timeElemList.indexOf(elm)).get(currentSndListIndex) != null) {
                    List<NcSoundingLayer> soundingLayeys = stnTimeSndTable.get(currentStnListIndex)
                            .get(timeElemList.indexOf(elm)).get(currentSndListIndex).getSndLyLst();
                    colorIndex = stnTimeSndTable.get(currentStnListIndex).get(timeElemList.indexOf(elm))
                            .get(currentSndListIndex).getCompColorIndex();
                    NsharpLineProperty lp = linePropertyMap.get(NsharpConstants.lineNameArray[colorIndex]);
                    createRscPressTempCurveShape(world, soundingLayeys, lp, target);
                }
            }
        } else if (rscHandler.isCompareSndIsOn() & currentStnListIndex >= 0 && currentTimeListIndex >= 0) {
            int colorIndex;

            List<NsharpResourceHandler.CompSndSelectedElem> sndCompElementList = rscHandler
                    .getCompSndSelectedElemList();
            for (NsharpResourceHandler.CompSndSelectedElem compElem : sndCompElementList) {
                NsharpSoundingElementStateProperty elemProp = stnTimeSndTable.get(compElem.getStnIndex())
                        .get(compElem.getTimeIndex()).get(compElem.getSndIndex());
                if (sndElemList.get(compElem.getSndIndex()).getActionState() == NsharpConstants.ActState.ACTIVE
                        && elemProp != null) {
                    List<NcSoundingLayer> soundingLayeys = elemProp.getSndLyLst();
                    colorIndex = elemProp.getCompColorIndex();
                    NsharpLineProperty lp = linePropertyMap.get(NsharpConstants.lineNameArray[colorIndex]);
                    createRscPressTempCurveShape(world, soundingLayeys, lp, target);
                }
            }

        } else if (rscHandler.isOverlayIsOn() == true) {

            previousSoundingLys = rscHandler.getPreviousSoundingLys();
            createRscPressTempCurveShape(world, this.soundingLys,
                    linePropertyMap.get(NsharpConstants.lineNameArray[NsharpConstants.LINE_OVERLAY1]), target);
            if (this.previousSoundingLys != null && !previousSoundingLys.equals(soundingLys)) {
                createRscPressTempCurveShape(world, this.previousSoundingLys,
                        linePropertyMap.get(NsharpConstants.lineNameArray[NsharpConstants.LINE_OVERLAY2]), target);
            }

        } else {

            createRscPressTempCurveShape(world, this.soundingLys, null, target);
        }
    }

    public void createRscVTempTraceShape() {

        if ((soundingLys == null) || (soundingLys.size() == 0)) {
            return;
        }
        float t1;
        if (vtempTraceCurveRscShape != null) {
            vtempTraceCurveRscShape.dispose();
            vtempTraceCurveRscShape = null;
        }
        Coordinate c2 = null;
        Coordinate c1;
        // draw trace
        vtempTraceCurveRscShape = target.createWireframeShape(false, descriptor);
        vtempTraceCurveRscShape.allocate(this.soundingLys.size() * 2);
        for (NcSoundingLayer layer : this.soundingLys) {
            if (NsharpLibBasics.qc(layer.getTemperature()) && NsharpLibBasics.qc(layer.getDewpoint())
                    && layer.getPressure() >= 100) {
                t1 = NsharpLibBasics.i_vtmp(soundingLys, layer.getPressure());
                c1 = NsharpWxMath.getSkewTXY(layer.getPressure(), t1);
                c1.x = world.mapX(c1.x);
                c1.y = world.mapY(c1.y);
                if (c2 != null) {

                    double[][] lines = { { c1.x, c1.y }, { c2.x, c2.y } };
                    vtempTraceCurveRscShape.addLineSegment(lines);
                }
                c2 = c1;
            }
        }
        vtempTraceCurveRscShape.compile();
    }

    /*
     * Chin:: NOTE::: This plotting function is based on the algorithm of
     * draw_Clouds() at xwvid1.c of AWC Nsharp source code Using Fred Mosher's
     * Algorithm & Chernykh and Eskridge Algorithm
     */
    private void createCloudsShape() {
        NsharpCloudInfo cloudInfo = weatherDataStore.getCloudInfo();
        // draw FM model: Fred Mosher's Algorithm
        if (cloudInfo.getFmCloudLys().size() > 0) {
            cloudFMShape = target.createShadedShape(false, descriptor, false);
            cloudFMLabelShape = target.createWireframeShape(false, descriptor);
            cloudFMLabelShape.allocate(2);
            double[][] lines = { { 0, 0 }, { 0, 0 } };
            cloudFMLabelShape.addLineSegment(lines);
            List<CloudLayer> fmCloudInfo = cloudInfo.getFmCloudLys();
            for (int i = 0; i < fmCloudInfo.size(); i++) {
                CloudLayer fmCloudLyr = fmCloudInfo.get(i);
                double lowY = world.mapY(NsharpWxMath.getSkewTXY(fmCloudLyr.getPressureStart(), -50).y);
                double highY = world.mapY(NsharpWxMath.getSkewTXY(fmCloudLyr.getPressureEnd(), -50).y);
                Coordinate[] coords = new Coordinate[4];
                coords[0] = new Coordinate(skewtXOrig + 150, lowY);
                coords[1] = new Coordinate(skewtXOrig + 200, lowY);
                coords[2] = new Coordinate(skewtXOrig + 200, highY);
                coords[3] = new Coordinate(skewtXOrig + 150, highY);

                /*
                 * Create LineString[] from Coordinates[]
                 */
                GeometryFactory gf = new GeometryFactory();
                LineString[] ls = new LineString[] { gf.createLineString(coords) };

                cloudFMShape.addPolygonPixelSpace(ls, NsharpConstants.color_yellow);
                double[] lblXy = { skewtXOrig + 175, (lowY + highY) / 2 };
                cloudFMLabelShape.addLabel(fmCloudLyr.getCloudType().name(), lblXy);
            }
            cloudFMShape.compile();
            cloudFMLabelShape.compile();
        }
        // draw CE model : Chernykh and Eskridge Algorithm
        if (cloudInfo.getCeCloudLys().size() > 0) {
            cloudCEShape = target.createShadedShape(false, descriptor, false);
            List<CloudLayer> ceCloudInfo = cloudInfo.getCeCloudLys();
            for (int i = 0; i < ceCloudInfo.size(); i++) {
                CloudLayer ceCloudLyr = ceCloudInfo.get(i);
                double lowY = world.mapY(NsharpWxMath.getSkewTXY(ceCloudLyr.getPressureStart(), -50).y);
                double highY = world.mapY(NsharpWxMath.getSkewTXY(ceCloudLyr.getPressureEnd(), -50).y);
                Coordinate[] coords = new Coordinate[4];
                coords[0] = new Coordinate(skewtXOrig + 100, lowY);
                coords[1] = new Coordinate(skewtXOrig + 150, lowY);
                coords[2] = new Coordinate(skewtXOrig + 150, highY);
                coords[3] = new Coordinate(skewtXOrig + 100, highY);

                /*
                 * Create LineString[] from Coordinates[]
                 */
                GeometryFactory gf = new GeometryFactory();
                LineString[] ls = new LineString[] { gf.createLineString(coords) };

                cloudCEShape.addPolygonPixelSpace(ls, NsharpConstants.color_red);

            }
            cloudCEShape.compile();

        }
    }

    // Chin: to handle dynamically moving omega within viewable zone when
    // zooming, I could not use wireframeShape successfully
    // It will chop off lower part of omega. Therefore use this draw function
    // for omega.
    private void drawOmega() {
        // draw label and vertical lines
        IExtent ext = descriptor.getRenderableDisplay().getExtent();
        float xmin = (float) ext.getMinX();

        float xWoldMin = (float) world.mapX(NsharpConstants.left);
        if (xmin > xWoldMin) {
            omegaXOrig = xmin + 40 * xRatio * currentZoomLevel;
        } else {
            omegaXOrig = xWoldMin + 40 * xRatio * currentZoomLevel;
        }
        // we dont really care about temp, as we use pressure for Y axis.
        try {
            // left dash line
            target.drawLine(omegaXOrig + omegaWidth * currentZoomLevel, omegaYOrig, 0.0,
                    omegaXOrig + omegaWidth * currentZoomLevel, omegaYEnd, 0.0, NsharpConstants.color_violet_red, 1,
                    LineStyle.DASHED);

            // center line
            target.drawLine(omegaXOrig + omegaWidth * currentZoomLevel * 0.5f, omegaYOrig, 0.0,
                    omegaXOrig + omegaWidth * currentZoomLevel * 0.5f, omegaYEnd, 0.0, NsharpConstants.color_violet_red,
                    1, LineStyle.DASHED);
            // right dash line,
            target.drawLine(omegaXOrig, omegaYOrig, 0.0, omegaXOrig, omegaYEnd, 0.0, NsharpConstants.color_violet_red,
                    1, LineStyle.DASHED);
            target.drawString(font10, "+1 OMEGA -1", omegaXOrig + omegaWidth * currentZoomLevel * 0.5f,
                    omegaYOrig + 10 * yRatio, 0.0, TextStyle.NORMAL, NsharpConstants.color_violet_red,
                    HorizontalAlignment.CENTER, VerticalAlignment.BOTTOM, null);

            float p, omega, t;
            double xAxisOrigin = omegaXOrig + omegaWidth * currentZoomLevel * 0.5f;
            for (NcSoundingLayer layer : this.soundingLys) {
                p = layer.getPressure();
                t = layer.getTemperature();
                if (layer.getOmega() > -999) {
                    omega = -layer.getOmega() * omegaWidth * currentZoomLevel * 0.5f;
                    Coordinate c1 = NsharpWxMath.getSkewTXY(p, t);
                    Coordinate c2 = new Coordinate();

                    c2.y = world.mapY(c1.y); // what we need here is only
                                             // pressure for Y-axix,

                    target.drawLine(xAxisOrigin, c2.y, 0.0, xAxisOrigin + omega, c2.y, 0.0, NsharpConstants.color_cyan,
                            1);
                }
            }
        } catch (VizException e) {
            statusHandler.handle(Priority.PROBLEM, "drawOmega exception:", e);
        }

    }

    /**
     * Create all wire frame shapes at one place. Should be used only when a new
     * resource is becoming Current active resource to be displayed.
     *
     */

    public void createRscWireFrameShapes() {
        if (target != null) {
            disposeRscWireFrameShapes();
            if (soundingLys != null) {
                if (rscHandler.isGoodData()) {
                    createRscwetBulbTraceShape();
                    createRscPressTempCurveShapeAll(target);
                    createRscVTempTraceShape();
                    // real temp trace
                    createRscParcelRtTraceShapesList(rscHandler.getCurrentParcel(),
                            rscHandler.getCurrentParcelLayerPressure());
                    createRscParcelTraceShapes(rscHandler.getCurrentParcel(),
                            rscHandler.getCurrentParcelLayerPressure());
                    // Virtual Temp Trace and DCAPE trace
                    createLCLEtcLinesShape();
                    createEffectiveLayerLinesShape();
                    createCloudsShape();
                    createLrmShape();
                    createDendriticShapes();
                } else {
                    createRscPressTempCurveShapeAll(target);
                }
            }
        }
    }

    public void disposeRscWireFrameShapes() {
        if (omegaBkgShape != null) {
            omegaBkgShape.dispose();
            omegaBkgShape = null;
        }
        if (omegaRscShape != null) {
            omegaRscShape.dispose();
            omegaRscShape = null;
        }
        if (heightMarkRscShape != null) {
            heightMarkRscShape.dispose();
            heightMarkRscShape = null;
        }
        if (wetBulbTraceRscShape != null) {
            wetBulbTraceRscShape.dispose();
            wetBulbTraceRscShape = null;
        }
        if (vtempTraceCurveRscShape != null) {
            vtempTraceCurveRscShape.dispose();
            vtempTraceCurveRscShape = null;
        }
        if (cloudFMShape != null) {
            cloudFMShape.dispose();
            cloudFMShape = null;
        }
        if (cloudCEShape != null) {
            cloudCEShape.dispose();
            cloudCEShape = null;
        }
        if (cloudFMLabelShape != null) {
            cloudFMLabelShape.dispose();
            cloudFMLabelShape = null;
        }
        if (icingTempShape != null) {
            icingTempShape.dispose();
            icingTempShape = null;
        }
        if (icingRHShape != null) {
            icingRHShape.dispose();
            icingRHShape = null;
        }
        if (icingEPIShape != null) {
            icingEPIShape.dispose();
            icingEPIShape = null;
        }
        if (turbWindShearShape != null) {
            turbWindShearShape.dispose();
            turbWindShearShape = null;
        }
        if (turbLnShape != null) {
            turbLnShape.dispose();
            turbLnShape = null;
        }
        if (parcelRtShapeList.size() > 0) {
            for (NsharpShapeAndLineProperty shapeColor : parcelRtShapeList) {
                shapeColor.getShape().dispose();
            }
            parcelRtShapeList.clear();
        }
        if (parcelVtTraceRscShape != null) {
            parcelVtTraceRscShape.dispose();
            parcelVtTraceRscShape = null;
        }
        if (dacpeTraceRscShape != null) {
            dacpeTraceRscShape.dispose();
            dacpeTraceRscShape = null;
        }
        if (pressureTempRscShapeList.size() > 0) {
            for (NsharpShapeAndLineProperty shapeColor : pressureTempRscShapeList) {
                shapeColor.getShape().dispose();
            }
            pressureTempRscShapeList.clear();

        }
        if (lclShape != null) {
            lclShape.dispose();
            lclShape = null;
        }
        if (elShape != null) {
            elShape.dispose();
            elShape = null;
        }
        if (mplShape != null) {
            mplShape.dispose();
            mplShape = null;
        }
        if (fzlShape != null) {
            fzlShape.dispose();
            fzlShape = null;
        }
        if (lfcShape != null) {
            lfcShape.dispose();
            lfcShape = null;
        }
        if (lrmShape != null) {
            lrmShape.dispose();
            lrmShape = null;
        }
        if (dendriticShape != null) {
            dendriticShape.dispose();
            dendriticShape = null;
        }
        if (frzShape != null) {
            frzShape.dispose();
            frzShape = null;
        }
        if (wbzShape != null) {
            wbzShape.dispose();
            wbzShape = null;
        }

    }

    /*
     * Return the closest point to the input point on either Temp or Dewpoint
     * trace line Also set currentSoundingLayerIndex for plotting later
     */
    public Coordinate getPickedTempPoint(Coordinate c) {

        Coordinate inC = NsharpWxMath.reverseSkewTXY(world.unMap(c));
        double inPressure = inC.y;
        double inTemp = inC.x;
        double prevPressure = 1000;
        double prevT = 0, prevD = 0;
        Coordinate closeptC = new Coordinate(0, 0, 0);
        boolean firstPrevPicked = false;

        /*
         * Note: soundingLys list sorted with highest pressure as first element
         */

        for (NcSoundingLayer layer : this.soundingLys) {
            double t, d;
            t = layer.getTemperature();
            d = layer.getDewpoint();
            double pressure = layer.getPressure();
            if (firstPrevPicked == false) {
                // this is to handle the case, if picked point has same pressure
                // (largest pressure) as first layer
                firstPrevPicked = true;
                prevPressure = pressure;
                prevT = t;
                prevD = d;
            }

            if (pressure >= 100 && pressure <= inPressure) {
                // decide which pressure (layer) should be used. current one or
                // previous one
                double disCurrentP = Math.abs(pressure - inPressure);
                double disPreviousP = Math.abs(prevPressure - inPressure);
                double pickedPressure, pickedTemp, pickedDewpoint;
                if (disPreviousP <= disCurrentP) {
                    pickedPressure = prevPressure;
                    pickedTemp = prevT;
                    pickedDewpoint = prevD;
                    if (this.soundingLys.indexOf(layer) == 0) {
                        currentSoundingLayerIndex = this.soundingLys.indexOf(layer);
                    } else {
                        currentSoundingLayerIndex = this.soundingLys.indexOf(layer) - 1;
                    }
                } else {
                    pickedPressure = pressure;
                    pickedTemp = t;
                    pickedDewpoint = d;
                    currentSoundingLayerIndex = this.soundingLys.indexOf(layer);
                }
                double disTemp = Math.abs(pickedTemp - inTemp);
                double disDew = Math.abs(pickedDewpoint - inTemp);
                // if both dis is not witin editing distance, ie. 4 degree, then
                // return with (0,0);
                if (disTemp > 4 && disDew > 4) {
                    return closeptC;
                }

                // decide which line, temp or dewpoint, closer to user picked
                // point
                if (disTemp <= disDew) {
                    closeptC = NsharpWxMath.getSkewTXY(pickedPressure, pickedTemp);
                    closeptC = world.map(closeptC);
                    currentTempCurveType = TEMP_TYPE;

                } else {
                    closeptC = NsharpWxMath.getSkewTXY(pickedPressure, pickedDewpoint);
                    closeptC = world.map(closeptC);
                    currentTempCurveType = DEWPOINT_TYPE;

                }
                break;
            }
            prevPressure = pressure;
            prevT = t;
            prevD = d;
        }

        return closeptC;
    }

    public void setCursorInSkewT(boolean cursorInSkewT) {
        this.cursorInSkewT = cursorInSkewT;
    }

    public void toggleCurseDisplay() {
        curseToggledFontLevel = curseToggledFontLevel + CURSER_FONT_INC_STEP;
        if (curseToggledFontLevel > CURSER_STRING_OFF) {
            curseToggledFontLevel = CURSER_FONT_10;
        }
        rscHandler.refreshPane();
    }

    public NsharpSkewTPaneBackground getSkewTBackground() {
        return skewTBackground;
    }

    public NsharpIcingPaneBackground getIcingBackground() {
        return icingBackground;
    }

    public NsharpTurbulencePaneBackground getTurbBackground() {
        return turbBackground;
    }

    public NsharpGenericPaneBackground getActiveBackground() {
        if (currentGraphMode == NsharpConstants.GRAPH_SKEWT) {
            return skewTBackground;
        } else if (currentGraphMode == NsharpConstants.GRAPH_ICING) {
            return icingBackground;
        } else if (currentGraphMode == NsharpConstants.GRAPH_TURB) {
            return turbBackground;
        }
        return null;
    }

    public void setCurrentGraphMode(int currentGraphMode) {
        this.currentGraphMode = currentGraphMode;
        handleResize();
        if (rscHandler.getWitoPaneRsc() != null) {
            // simple D2D pane does not display WITO pane
            rscHandler.getWitoPaneRsc().handleResize();
        }
    }

    public void setPlotInteractiveTemp(boolean plotInteractiveTemp) {
        this.plotInteractiveTemp = plotInteractiveTemp;
    }

    public void setInteractiveTempPointCoordinate(Coordinate interactiveTempPointCoordinate) {
        this.interactiveTempPointCoordinate = interactiveTempPointCoordinate;
    }

    public int getCurrentTempCurveType() {
        return currentTempCurveType;
    }

    public int getCurrentSkewTEditMode() {
        return currentSkewTEditMode;
    }

    public void setCurrentSkewTEditMode(int currentSkewTEditMode) {
        this.currentSkewTEditMode = currentSkewTEditMode;
    }

    @Override
    public void handleResize() {
        super.handleResize();
        if (getDescriptor().getRenderableDisplay() == null) {
            return;
        }
        IExtent ext = getDescriptor().getRenderableDisplay().getExtent();
        ext.reset();
        if (ext.getWidth() == 0.0 || ext.getHeight() == 0.0) {
            return;
        }

        this.rectangle = new Rectangle((int) ext.getMinX(), (int) ext.getMinY(), (int) ext.getWidth(),
                (int) ext.getHeight());
        pe = new PixelExtent(this.rectangle);
        getDescriptor().setNewPe(pe);
        world = new NsharpWGraphics(this.rectangle);
        world.setWorldCoordinates(NsharpConstants.left, NsharpConstants.top, NsharpConstants.right,
                NsharpConstants.bottom);
        float prevHeight = skewtHeight;
        float prevWidth = skewtWidth;
        skewtXOrig = (int) (ext.getMinX());
        skewtYOrig = (int) ext.getMinY();
        skewtWidth = (int) (ext.getWidth());
        skewtHeight = (int) ext.getHeight();
        xRatio = xRatio * skewtWidth / prevWidth;
        yRatio = yRatio * skewtHeight / prevHeight;
        omegaYOrig = skewtYOrig;
        omegaWidth = skewtWidth * 0.05f;
        omegaHeight = skewtHeight;
        omegaYEnd = omegaYOrig + omegaHeight;
        createRscWireFrameShapes();
        if (currentGraphMode == NsharpConstants.GRAPH_SKEWT) {
            skewTBackground.handleResize(ext);
        } else if (currentGraphMode == NsharpConstants.GRAPH_ICING) {
            icingBackground.handleResize(ext);
        } else if (currentGraphMode == NsharpConstants.GRAPH_TURB) {
            turbBackground.handleResize(ext);
        }

        if (rscHandler != null && rscHandler.getWitoPaneRsc() != null) {
            // simple/lite D2D panes do not display WITO pane
            rscHandler.getWitoPaneRsc().handleResize();
        }
    }

    @Override
    public void handleZooming() {

        if (heightMarkRscShape != null) {
            heightMarkRscShape.dispose();
        }
        if (omegaBkgShape != null) {
            omegaBkgShape.dispose();
            omegaBkgShape = null;
        }
        if (omegaRscShape != null) {
            omegaRscShape.dispose();
            omegaRscShape = null;
        }
        skewTBackground.handleZooming();
        turbBackground.handleZooming();
        icingBackground.handleZooming();
        if (rscHandler.getWitoPaneRsc() != null) {
            // simple D2D pane does not display WITO pane
            rscHandler.getWitoPaneRsc().handleZooming();
        }

    }

    @Override
    protected void adjustFontSize(float canvasW, float canvasH) {
        super.adjustFontSize(canvasW, canvasH);
        // make a bit bigger font10 size for skewT
        float font10Size = 10;
        if (font10 != null) {
            font10Size = font10.getFontSize() + 1;
            font10.dispose();
        }
        font10 = target.initializeFont("Monospace", font10Size, null);
    }

}
