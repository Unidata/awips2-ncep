package gov.noaa.nws.ncep.ui.nsharp.display.rsc;

import java.util.HashMap;
import java.util.List;

import javax.measure.converter.UnitConverter;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;

import com.raytheon.uf.viz.core.DrawableString;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.LineStyle;
import com.raytheon.uf.viz.core.PixelExtent;
import com.raytheon.uf.viz.core.drawables.IDescriptor.FramesInfo;
import com.raytheon.uf.viz.core.drawables.IFont;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.AbstractResourceData;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.capabilities.ColorableCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.OutlineCapability;
import com.raytheon.viz.core.ColorUtil;
import com.vividsolutions.jts.geom.Coordinate;

import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;
import gov.noaa.nws.ncep.ui.nsharp.NsharpConstants;
import gov.noaa.nws.ncep.ui.nsharp.NsharpGraphProperty;
import gov.noaa.nws.ncep.ui.nsharp.NsharpLineProperty;
import gov.noaa.nws.ncep.ui.nsharp.NsharpWGraphics;
import gov.noaa.nws.ncep.ui.nsharp.display.NsharpAbstractPaneDescriptor;

/**
 *
 *
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date         Ticket#        Engineer    Description
 * -------        -------     --------     -----------
 * 04/23/2012    229         Chin Chen    Initial coding
 * 07/05/2016   RM#15923     Chin Chen    NSHARP - Native Code replacement
 * 07/10/2017   RM#34796     Chin Chen    NSHARP - Updates for March 2017 bigSharp version
 *                                        - Reformat the lower left data page
 * 11/29/2017   5863         bsteffen     Change dataTimes to a NavigableSet
 * May, 5, 2018 49896       mgamazaychikov  Reconciled with RODO 5070, 5863, fixed formatting
 * 11/21/2018   7574         bsteffen     Get previous layers from handler to ensure consistency.
 *
 * </pre>
 *
 * @author Chin Chen
 * @version 1.0
 */
public class NsharpAbstractPaneResource
        extends AbstractVizResource<AbstractResourceData, NsharpAbstractPaneDescriptor> {

    protected IGraphicsTarget target = null;

    protected Rectangle rectangle;

    protected NsharpWGraphics world;

    protected PixelExtent pe;

    protected static final UnitConverter celciusToFahrenheit = SI.CELSIUS.getConverterTo(NonSI.FAHRENHEIT);

    protected static final UnitConverter celciusToKelvin = SI.CELSIUS.getConverterTo(SI.KELVIN);

    protected List<NcSoundingLayer> soundingLys = null;

    protected NsharpResourceHandler rscHandler = null;

    protected NsharpGraphProperty graphConfigProperty = null;

    protected HashMap<String, NsharpLineProperty> linePropertyMap = null;

    protected int currentSoundingLayerIndex = 0;

    protected IFont font8 = null;

    protected IFont font9 = null;

    protected IFont font10 = null;

    protected IFont font11 = null;

    protected IFont font12 = null;

    protected IFont font20 = null;

    protected float currentFont10Size = 10;

    protected int commonLinewidth;

    protected LineStyle commonLineStyle;

    protected Coordinate interactiveTempPointCoordinate;

    protected float currentZoomLevel = 1f;

    protected float currentCanvasBoundWidth;

    protected float currentCanvasBoundHeight;

    protected float myDefaultCanvasHeight;

    protected float myDefaultCanvasWidth;

    protected boolean resize = false;

    protected String paneConfigurationName;

    protected Coordinate cursorCor;

    protected double charHeight = NsharpConstants.CHAR_HEIGHT_;

    protected double charWidth;

    protected double lineHeight = charHeight * 1.2;

    protected PaintProperties paintProps;

    protected NsharpWeatherDataStore weatherDataStore;

    public NsharpAbstractPaneResource(AbstractResourceData resourceData, LoadProperties loadProperties,
            NsharpAbstractPaneDescriptor desc) {
        super(resourceData, loadProperties, false);
        descriptor = desc;

    }

    @Override
    protected void disposeInternal() {
        if (font8 != null) {
            font8.dispose();
            font8 = null;
        }
        if (font9 != null) {
            font9.dispose();
            font9 = null;
        }
        if (font10 != null) {
            font10.dispose();
            font10 = null;
        }
        if (font11 != null) {
            font11.dispose();
            font11 = null;
        }
        if (font12 != null) {
            font12.dispose();
            font12 = null;
        }
        if (font20 != null) {
            font20.dispose();
            font20 = null;
        }
        this.target.dispose();
        target = null;
    }

    @Override
    protected void paintInternal(IGraphicsTarget target, PaintProperties paintProps) throws VizException {
        this.paintProps = paintProps;
        this.target = target;
        if (rscHandler == null || rscHandler.getSoundingLys() == null) {
            return;
        }
        float zoomLevel = paintProps.getZoomLevel();
        if (zoomLevel > 1.0f) {
            zoomLevel = 1.0f;
        }
        if ((zoomLevel != currentZoomLevel)) {
            currentZoomLevel = zoomLevel;
            handleZooming();

        }
        if (this.resize == true) {
            handleResize();
        }
    }

    @Override
    protected void initInternal(IGraphicsTarget target) throws VizException {
        this.target = target;
        this.font8 = target.initializeFont("Monospace", 8, null);
        this.font9 = target.initializeFont("Monospace", 7.5f, null);
        this.font10 = target.initializeFont("Monospace", 8, null);
        this.font11 = target.initializeFont("Monospace", 9, null);
        IFont.Style[] style = { IFont.Style.BOLD };
        this.font12 = target.initializeFont("Monospace", 10, style);
        this.font20 = target.initializeFont("Monospace", 17, null); // d2dlite
        this.font8.setSmoothing(false);
        this.font8.setScaleFont(false);
        this.font9.setSmoothing(false);
        this.font9.setScaleFont(false);
        this.font10.setSmoothing(false);
        this.font10.setScaleFont(false);
        this.font11.setSmoothing(false);
        this.font11.setScaleFont(false);
        this.font12.setSmoothing(false);
        this.font12.setScaleFont(false);
        this.font20.setSmoothing(false);
        this.font20.setScaleFont(false);
        commonLinewidth = getCapability(OutlineCapability.class).getOutlineWidth();
        commonLineStyle = getCapability(OutlineCapability.class).getLineStyle();
        this.resize = true;

    }

    public void resetData(List<NcSoundingLayer> soundingLys) {
        this.soundingLys = soundingLys;
        descriptor.setFramesInfo(new FramesInfo(0));
    }

    public NsharpWGraphics getWorld() {
        return world;
    }

    protected void adjustFontSize(float canvasW, float canvasH) {
        // TODO: this is likely to need work after the LX upgrade changes
        float font8Size, font9Size, font10Size, font11Size, font12Size, font20Size;

        float fontAdjusted = 0;
        float fontBaseH = 90f;
        float fontBaseW = 120f;
        if (canvasH < myDefaultCanvasHeight && canvasW < myDefaultCanvasWidth) {
            // both width and height are smaller than default
            float wAdjust = (myDefaultCanvasWidth - canvasW) / fontBaseW;
            float hAdjust = (myDefaultCanvasHeight - canvasH) / fontBaseH;
            fontAdjusted = Math.max(wAdjust, hAdjust);
        } else if (canvasW < myDefaultCanvasWidth) {
            // only width smaller than default
            fontAdjusted = (myDefaultCanvasWidth - canvasW) / fontBaseW;
        } else if (canvasH < myDefaultCanvasHeight) {
            // only height smaller than default
            fontAdjusted = (myDefaultCanvasHeight - canvasH) / fontBaseH;
        }
        // Ron: This would probably work better if the font adjustment was
        // multiplied like a scale factor instead of added
        // Chin: Can not bigger than 9, otherwise, fint9 size will be negative.
        // After many "try and error" experiments...use 8.8
        if (fontAdjusted > 8.8) {
            fontAdjusted = 8.8f;
        }

        font8Size = 8 - fontAdjusted;
        font9Size = 7.5f - fontAdjusted;
        font10Size = 8 - fontAdjusted;
        font11Size = 9 - fontAdjusted;
        font12Size = 10 - fontAdjusted;
        font20Size = 17 - fontAdjusted; // d2dlite

        if (font8 != null) {
            font8.dispose();
        }
        font8 = target.initializeFont("Monospace", font8Size, null);

        if (font9 != null) {
            font9.dispose();
        }
        font9 = target.initializeFont("Monospace", font9Size, null);
        if (font10 != null) {
            font10.dispose();
        }
        font10 = target.initializeFont("Monospace", font10Size, null);
        if (font11 != null) {
            font11.dispose();
        }
        font11 = target.initializeFont("Monospace", font11Size, null);
        if (font12 != null) {
            font12.dispose();
        }
        IFont.Style[] style = { IFont.Style.BOLD };
        font12 = target.initializeFont("Monospace", font12Size, style);

        if (font20 != null) {
            font20.dispose();
        }
        font20 = target.initializeFont("Monospace", font20Size, style);
        currentFont10Size = font10Size;

    }

    protected void magnifyFont(double zoomLevel) {
        float magFactor = 1.0f / (float) zoomLevel;
        font8.setMagnification(magFactor);
        font9.setMagnification(magFactor);
        font10.setMagnification(magFactor);
        font11.setMagnification(magFactor);
        font12.setMagnification(magFactor);
        font20.setMagnification(magFactor);
    }

    @Override
    public void setDescriptor(NsharpAbstractPaneDescriptor descriptor) {
        super.setDescriptor(descriptor);
        RGB rgb = ColorUtil.getNewColor(descriptor);
        getCapability(ColorableCapability.class).setColor(rgb);

    }

    public void setSoundingLys(List<NcSoundingLayer> soundingLys) {
        this.soundingLys = soundingLys;

    }

    public HashMap<String, NsharpLineProperty> getLinePropertyMap() {
        return linePropertyMap;
    }

    public void setLinePropertyMap(HashMap<String, NsharpLineProperty> linePropertyMap) {
        this.linePropertyMap = linePropertyMap;

    }

    public NsharpGraphProperty getGraphConfigProperty() {
        return graphConfigProperty;
    }

    public void setGraphConfigProperty(NsharpGraphProperty graphConfigProperty) {
        this.graphConfigProperty = graphConfigProperty;
        paneConfigurationName = this.graphConfigProperty.getPaneConfigurationName();

    }

    public NsharpResourceHandler getRscHandler() {
        return rscHandler;
    }

    public int getCurrentSoundingLayerIndex() {
        return currentSoundingLayerIndex;
    }

    public void setRscHandler(NsharpResourceHandler rscHandler) {
        this.rscHandler = rscHandler;
        weatherDataStore = rscHandler.getWeatherDataStore();
        if (descriptor != null) {
            descriptor.setRscHandler(rscHandler);
        }
    }

    public void handleResize() {
        this.resize = false;
        if (paintProps != null && (currentCanvasBoundWidth != paintProps.getCanvasBounds().width
                || currentCanvasBoundHeight != paintProps.getCanvasBounds().height)) {
            currentCanvasBoundWidth = paintProps.getCanvasBounds().width;
            currentCanvasBoundHeight = paintProps.getCanvasBounds().height;
            adjustFontSize(currentCanvasBoundWidth, currentCanvasBoundHeight);
        }

    }

    public void setResize(boolean resize) {
        this.resize = resize;
    }

    public void handleZooming() {

    }

    protected void defineCharHeight(IFont font) {
        if (paintProps == null) {
            return;
        }
        DrawableString str = new DrawableString("CHINCHEN", NsharpConstants.color_black);
        str.font = font;
        double vertRatio = paintProps.getView().getExtent().getHeight() / paintProps.getCanvasBounds().height;
        double horizRatio = paintProps.getView().getExtent().getWidth() / paintProps.getCanvasBounds().width;
        charHeight = target.getStringsBounds(str).getHeight() * vertRatio;
        lineHeight = charHeight * 1.2;
        charWidth = target.getStringsBounds(str).getWidth() * horizRatio / 8;

    }

    protected String timeDescriptionToDisplayStr(String timeDescription) {
        /*
         * As of 2014 April 9, current time description string is defined as
         * "YYMMDD/HH(DOW)" or "YYMMDD/HH(DOW)Vxxx". Convert them to
         * "DD.HH(DOW)" or "DD.HHVxxx(DOW)" for GUI display.
         */
        String rtnStr = timeDescription.substring(4); // get rid of YYMM
        if (rtnStr.contains("V")) {
            // split DD/HH(DOW)Vxxx to "DD/HH(DOW)" and "xxx"
            String[] s1Str = rtnStr.split("V");
            // split "DD/HH(DOW)" to "DD/HH" and "DOW)"
            String[] s2Str = s1Str[0].split("\\(");
            // put together to "DD/HHVxxx(DOW)"
            rtnStr = s2Str[0] + "V" + s1Str[1] + "(" + s2Str[1];
        }
        rtnStr = rtnStr.replace("/", "."); // replace "/" with "."
        return rtnStr;
    }

    protected String pickedStnInfoStrToDisplayStr(String pickedStnInfoStr) {
        /*
         * As of 2014 April 9, current pickedStnInfoStr string is defined as
         * "stnId YYMMDD/HH(DOW)Vxxx sndType". This function is to convert it to
         * "stnId DD.HHVxxx(DOW) sndType" for GUI display. for example,
         * "ATLH 101209/03(Thu)V003 GFS230" converts to
         * "ATLH 09.03V003(Thu) GFS230"
         */
        String[] s1Str = pickedStnInfoStr.split(" ");
        if (s1Str.length == 3) {
            String rtnStr = timeDescriptionToDisplayStr(s1Str[1]);
            rtnStr = s1Str[0] + " " + rtnStr + " " + s1Str[2];
            return rtnStr;
        } else {
            return pickedStnInfoStr; // not a good input, just return it
        }
    }
}
