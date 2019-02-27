package gov.noaa.nws.ncep.ui.nsharp.display.rsc;

import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;

import com.raytheon.uf.viz.core.DrawableString;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.HorizontalAlignment;
import com.raytheon.uf.viz.core.IGraphicsTarget.TextStyle;
import com.raytheon.uf.viz.core.IGraphicsTarget.VerticalAlignment;
import com.raytheon.uf.viz.core.PixelExtent;
import com.raytheon.uf.viz.core.drawables.IFont;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.AbstractResourceData;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.viz.core.graphing.LineStroke;
import com.raytheon.viz.core.graphing.WindBarbFactory;
import com.vividsolutions.jts.geom.Coordinate;

import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibBasics;
import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibSndglib;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.Helicity;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.MixHeight;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.Parcel;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.WindComponent;
import gov.noaa.nws.ncep.ui.nsharp.NsharpConstants;
import gov.noaa.nws.ncep.ui.nsharp.background.NsharpGenericPaneBackground;
import gov.noaa.nws.ncep.ui.nsharp.display.NsharpAbstractPaneDescriptor;
import gov.noaa.nws.ncep.ui.nsharp.display.rsc.NsharpWeatherDataStore.ParcelMiscParams;
import gov.noaa.nws.ncep.ui.nsharp.natives.NsharpNativeConstants;

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
 * 04/23/2012    229            Chin Chen    Initial coding
 * 04/23/2014               Chin Chen   Add d2d lite page
 * 08/11/2014               Chin Chen   fix typo
 * 01/27/2015   DR#17006,
 *              Task#5929   Chin Chen   NSHARP freezes when loading a sounding from MDCRS products
 *                                      in Volume Browser
 * 08/10/2015   RM#9396     Chin Chen   implement new OPC pane configuration
 * 07/05/2016   RM#15923    Chin Chen   NSHARP - Native Code replacement
 * 07/10/2017   RM#34796    Chin Chen   NSHARP - Updates for March 2017 bigSharp version
 *                                     - Reformat the lower left data page
 * 07/28/2017   RM#34795    Chin Chen   NSHARP - Updates for March 2017 bigSharp version
 *                                      - Added output for the "large hail parameter" and
 *                                      the "modified SHERBE" parameter,..etc.
 * 09/1/2017   RM#34794    Chin Chen   NSHARP - Updates for March 2017 bigSharp version
 *                                      - Update the dendritic growth layer calculations and other skewT
 *                                      updates.
 * 05/05/2018   DCS20492    mgamazaychikov  Fixed an NPE for parcelMiscs (line 492), fixed formatting.
 * Oct 16, 2018  6845       bsteffen    Remove unnecessary jna code.
 * 10/26/2018   DR20904     mgamazaychikov  Changed how parcel indices are set in drawPanel4.
 * Nov 21, 2018  7574       bsteffen    Remove unused override
 * Dec 20, 2018  7575       bsteffen    Remove some NsharpNativeConstants.
 *
 * </pre>
 *
 * @author Chin Chen
 * @version 1.0
 */
public class NsharpDataPaneResource extends NsharpAbstractPaneResource {
    // Note: SR Helicity only show first 4 storm types in PAGE2
    private static final int STORM_HELICITY_MAX = 4;

    // index is the real page defined in NsharpConstants to be shown, value is
    // the order number of this page. index 0 point to a dummy.
    private int[] pageDisplayOrderNumberArray;

    private static final String INSUFFICIENT_DATA = "INSUFFICIENT DATA FOR PARAMETERS COMPUTATION";

    private double curY;

    private double parcelLineYStart, parcelLineYEnd;

    private PixelExtent extent;

    // physical number of panel in editor display
    // total software defined panels to be displayed
    private Rectangle[] panelRectArray = new Rectangle[NsharpConstants.dsiplayPanelSize];

    private int numberPagePerDisplay = 1;

    private NsharpGenericPaneBackground dataPanel1Background;

    private NsharpGenericPaneBackground dataPanel2Background;

    private int dataPaneWidth = NsharpConstants.DATA_PANE_REC_WIDTH;

    private int dataPaneHeight = NsharpConstants.DATA_PANE_REC_HEIGHT;

    private float xRatio = 1;

    private float yRatio = 1;

    private IFont defaultFont = font8;

    private boolean initDone = false;

    private boolean resizedone = false;

    private int currentParcel = NsharpLibSndglib.PARCELTYPE_MOST_UNSTABLE;;

    private boolean sumP1Visible = false;

    /*
     * Page Sum1 string definitions
     */
    public static final String PAGE1TEXT1_SB_STR = "SURFACE";

    public static final String PAGE1TEXT1_ML_STR = "ML 100 mb";

    public static final String PAGE1TEXT1_FCST_STR = "FCST SFC    ";

    public static final String PAGE1TEXT1_MU_STR = "MU";

    public static final String PAGE1TEXT1_USER_STR = "USER DEF";

    public static final String PAGE1TEXT1_EFF_STR = "EFF LAYER";

    // parcel header string
    public static final String PARCEL_DATA_STR = "\t\t\t\tPARCEL DATA    \r\n";

    public static final String PARCEL_OBS_SFC_STR = "\t\t*** SFC PARCEL ***\r\n";

    public static final String PARCEL_FORECAST_SFC_STR = "\t\t*** FCST SFC PARCEL ***\r\n";

    public static final String PARCEL_MEAN_MIXING_STR = "\t\t*** MEAN MIXING LAYER PARCEL ***\r\n";

    public static final String PARCEL_MOST_UNSTABLE_STR = "\t\t*** MOST UNSTABLE PARCEL ***\r\n";

    public static final String PARCEL_MEAN_EFF_STR = "\t\t*** MEAN EFFECTIVE PARCEL ***\r\n";

    public static final String PARCEL_USR_DEFINED_STR = "\t\t*** %.1f mb PARCEL ***\r\n";

    // parcel lines
    public static final String PARCEL_LPL_LINE_ = "LPL:_%dmb_%dC/%dC_%dF/%dF";

    public static final String PARCEL_LPL_MISSING = "LPL:   Missing";

    public static final String PARCEL_CAPE_LINE = "CAPE =  %.0f  J/Kg";

    public static final String PARCEL_CAPE_MISSING = "CAPE =  M";

    public static final String PARCEL_LI_LINE = "LI(500mb) =%5.0fC\r\n";

    public static final String PARCEL_LI_MISSING = "LI(500mb) =  M\r\n";

    public static final String PARCEL_BFZL_LINE = "BFZL =  %.0f J/Kg";

    public static final String PARCEL_BFZL_MISSING = "BFZL =  M";

    public static final String PARCEL_LIMIN_LINE = "LImin =  %4.0fC /%4.0fmb\r\n";

    public static final String PARCEL_LIMIN_MISSING = "LImin =  M / M\r\n";

    public static final String PARCEL_CINH_LINE = "CINH =  %.0f J/Kg";

    public static final String PARCEL_CINH_MISSING = "CINH =  M";

    public static final String PARCEL_CAP_LINE = "Cap =  %4.0fC /%4.0fmb\r\n\r\n";

    public static final String PARCEL_CAP_MISSING = "Cap =  M / M\r\n\r\n";

    public static final String PARCEL_LEVEL_LINE_ = "LEVEL_PRES_HGT(AGL)_TEMP";

    public static final String PARCEL_LCL_LINE_ = "LCL_%5.0fmb_%7.0fft_ ";

    public static final String PARCEL_LCL_MISSING_ = "LCL_M_M_ ";

    public static final String PARCEL_LFC_LINE_ = "LFC_%5.0fmb_%7.0fft_%6.0fC";

    public static final String PARCEL_LFC_MISSING_ = "LFC_M_M_M";

    public static final String PARCEL_EL_LINE_ = "EL_%5.0fmb_%7.0fft_%6.0fC";

    public static final String PARCEL_EL_MISSING_ = "EL_M_M_M";

    public static final String PARCEL_MPL_LINE_ = "MPL_%5.0fmb_%7.0fft_ ";

    public static final String PARCEL_MPL_MISSING_ = "MPL_M_M_ ";

    // use parcel type to retrieve parcel header string for display
    private static final Map<Integer, String> parcelToHdrStrMap = new HashMap<Integer, String>() {
        private static final long serialVersionUID = 1L;

        {
            put(NsharpLibSndglib.PARCELTYPE_OBS_SFC, PARCEL_OBS_SFC_STR);
            put(NsharpLibSndglib.PARCELTYPE_FCST_SFC, PARCEL_FORECAST_SFC_STR);
            put(NsharpLibSndglib.PARCELTYPE_MEAN_MIXING, PARCEL_MEAN_MIXING_STR);
            put(NsharpLibSndglib.PARCELTYPE_MOST_UNSTABLE, PARCEL_MOST_UNSTABLE_STR);
            put(NsharpLibSndglib.PARCELTYPE_USER_DEFINED, PARCEL_USR_DEFINED_STR);
            put(NsharpLibSndglib.PARCELTYPE_EFF, PARCEL_MEAN_EFF_STR);
        }
    };

    private static final Map<Integer, String> parcelToTypeStrMap = new HashMap<Integer, String>() {
        private static final long serialVersionUID = 1L;

        {
            put(NsharpLibSndglib.PARCELTYPE_OBS_SFC, PAGE1TEXT1_SB_STR);
            put(NsharpLibSndglib.PARCELTYPE_FCST_SFC, PAGE1TEXT1_FCST_STR);
            put(NsharpLibSndglib.PARCELTYPE_MEAN_MIXING, PAGE1TEXT1_ML_STR);
            put(NsharpLibSndglib.PARCELTYPE_MOST_UNSTABLE, PAGE1TEXT1_MU_STR);
            put(NsharpLibSndglib.PARCELTYPE_USER_DEFINED, PAGE1TEXT1_USER_STR);
            put(NsharpLibSndglib.PARCELTYPE_EFF, PAGE1TEXT1_EFF_STR);
        }
    };

    // parcel LI string map used in MISC parameters on page 4
    private static final Map<String, Integer> parcelLIStrMap = new LinkedHashMap<String, Integer>() {
        private static final long serialVersionUID = 1L;

        {
            put("SBP LI = ", NsharpLibSndglib.PARCELTYPE_OBS_SFC);
            put("FCP LI = ", NsharpLibSndglib.PARCELTYPE_FCST_SFC);
            put("MUP LI = ", NsharpLibSndglib.PARCELTYPE_MOST_UNSTABLE);
            put("MLP LI = ", NsharpLibSndglib.PARCELTYPE_MEAN_MIXING);
            put("USP LI = ", NsharpLibSndglib.PARCELTYPE_USER_DEFINED);
            put("EFP LI = ", NsharpLibSndglib.PARCELTYPE_EFF);
        }
    };

    public NsharpDataPaneResource(AbstractResourceData resourceData, LoadProperties loadProperties,
            NsharpAbstractPaneDescriptor desc) {
        super(resourceData, loadProperties, desc);
        dataPanel1Background = new NsharpGenericPaneBackground(
                new Rectangle(NsharpConstants.DATAPANEL1_X_ORIG, NsharpConstants.DATAPANEL1_Y_ORIG,
                        NsharpConstants.DATAPANEL1_WIDTH, NsharpConstants.DATAPANEL1_HEIGHT));
        dataPanel2Background = new NsharpGenericPaneBackground(
                new Rectangle(NsharpConstants.DATAPANEL2_X_ORIG, NsharpConstants.DATAPANEL2_Y_ORIG,
                        NsharpConstants.DATAPANEL2_WIDTH, NsharpConstants.DATAPANEL2_HEIGHT));
    }

    @Override
    protected void paintInternal(IGraphicsTarget target, PaintProperties paintProps) throws VizException {
        super.paintInternal(target, paintProps);
        dataPanel1Background.paint(target, paintProps);
        if (numberPagePerDisplay == 2) {
            dataPanel2Background.paint(target, paintProps);
        }
        if (rscHandler == null) {
            return;
        }

        if (!resizedone) {
            resizedone = true;
            handleResize();
        }
        currentParcel = rscHandler.getCurrentParcel();
        if ((soundingLys != null) && (rscHandler.isGoodData())) {
            this.defaultFont.setSmoothing(false);
            this.defaultFont.setScaleFont(false);
            // write to panels

            sumP1Visible = false;
            int currentTextChapter = rscHandler.getCurrentTextChapter();
            if (numberPagePerDisplay == 1) {
                drawPanel(target, currentTextChapter, 1);
            } else if (numberPagePerDisplay == 2) {
                // When One chapter = 2 pages. i.e. show current page and its
                // next page with 2 physical panels.
                // currently, we have total of 11 "pages" to display on 2
                // "physical display panels per design.
                for (int i = currentTextChapter * numberPagePerDisplay
                        - 1, physicalPanelNum = 1; i <= currentTextChapter
                                * numberPagePerDisplay; i++, physicalPanelNum++) {
                    int pageNum = i % NsharpConstants.PAGE_MAX_NUMBER;
                    if (pageNum == 0) {
                        pageNum = NsharpConstants.PAGE_MAX_NUMBER;
                    }

                    drawPanel(target, pageNum, physicalPanelNum);
                }
            }
        } else {
            drawInsuffDataMessage(target, panelRectArray[0]);
        }
    }

    @Override
    protected void initInternal(IGraphicsTarget target) throws VizException {
        super.initInternal(target);
        if (paneConfigurationName.equals(NsharpConstants.PANE_DEF_CFG_2_STR)
                || paneConfigurationName.equals(NsharpConstants.PANE_SPCWS_CFG_STR)
                || paneConfigurationName.equals(NsharpConstants.PANE_SIMPLE_D2D_CFG_STR)) {
            myDefaultCanvasWidth = (int) (NsharpConstants.DISPLAY_WIDTH
                    * (1 - NsharpConstants.PANE_DEF_CFG_2_LEFT_GP_WIDTH_RATIO)
                    * NsharpConstants.PANE_DEF_CFG_2_DATA_WIDTH_RATIO);
            myDefaultCanvasHeight = (int) (NsharpConstants.DISPLAY_HEIGHT
                    * NsharpConstants.PANE_DEF_CFG_2_DATA_HEIGHT_RATIO);
        } else if (paneConfigurationName.equals(NsharpConstants.PANE_DEF_CFG_1_STR)) {
            myDefaultCanvasWidth = (int) (NsharpConstants.DISPLAY_WIDTH
                    * (1 - NsharpConstants.PANE_DEF_CFG_1_LEFT_GP_WIDTH_RATIO)
                    * NsharpConstants.PANE_DEF_CFG_1_DATA_WIDTH_RATIO);
            myDefaultCanvasHeight = (int) (NsharpConstants.DISPLAY_HEIGHT
                    * NsharpConstants.PANE_DEF_CFG_1_DATA_HEIGHT_RATIO);
        }
        panelRectArray[0] = dataPanel1Background.getRectangle();
        panelRectArray[1] = dataPanel2Background.getRectangle();
        dataPanel1Background.initInternal(target);
        dataPanel2Background.initInternal(target);
        defaultFont = font8;
        handleResize();
        initDone = true;
    }

    private void drawInsuffDataMessage(IGraphicsTarget target, Rectangle rect) throws VizException {
        IFont myfont;
        if (paneConfigurationName.equals(NsharpConstants.PANE_LITE_D2D_CFG_STR)) {
            myfont = font9;
        } else {
            myfont = font20;
        }
        defineCharHeight(myfont);
        myfont.setSmoothing(false);
        myfont.setScaleFont(false);
        sumP1Visible = true;
        extent = new PixelExtent(rect);
        target.setupClippingPlane(extent);
        target.drawString(myfont, INSUFFICIENT_DATA, rect.x, rect.y, 0.0, TextStyle.NORMAL, NsharpConstants.color_cyan,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        return;

    }

    private void drawPanel(IGraphicsTarget target, int pageOrderNumber, int dsiplayPanelNumber) throws VizException {
        if (pageOrderNumber > NsharpConstants.PAGE_MAX_NUMBER || dsiplayPanelNumber > numberPagePerDisplay) {
            return;
        }
        int physicalPanelNumber = dsiplayPanelNumber - 1;
        int displayPageNumber = 0;
        // find a page with its order number equal to pageOrderNumber
        for (int i = 1; i <= NsharpConstants.PAGE_MAX_NUMBER; i++) {
            if (pageDisplayOrderNumberArray[i] == pageOrderNumber) {
                // array index is the page number and value is the order number
                displayPageNumber = i;
                break;
            }
        }
        switch (displayPageNumber) {
        case NsharpConstants.PAGE_SUMMARY1:
            drawPanel1(target, panelRectArray[physicalPanelNumber]);
            break;
        case NsharpConstants.PAGE_SUMMARY2:
            drawPanel2(target, panelRectArray[physicalPanelNumber]);
            break;
        case NsharpConstants.PAGE_PARCEL_DATA:
            drawPanel3(target, panelRectArray[physicalPanelNumber]);
            break;
        case NsharpConstants.PAGE_THERMODYNAMIC_DATA:
            drawPanel4(target, panelRectArray[physicalPanelNumber]);
            break;
        case NsharpConstants.PAGE_OPC_DATA:
            drawPanel5(target, panelRectArray[physicalPanelNumber]);
            break;
        case NsharpConstants.PAGE_MIXING_HEIGHT:
            drawPanel6(target, panelRectArray[physicalPanelNumber]);
            break;
        case NsharpConstants.PAGE_STORM_RELATIVE:
            drawPanel7(target, panelRectArray[physicalPanelNumber]);
            break;
        case NsharpConstants.PAGE_MEAN_WIND:
            drawPanel8(target, panelRectArray[physicalPanelNumber]);
            break;
        case NsharpConstants.PAGE_CONVECTIVE_INITIATION:
            drawPanel9(target, panelRectArray[physicalPanelNumber]);
            break;
        case NsharpConstants.PAGE_SEVERE_POTENTIAL:
            drawPanel10(target, panelRectArray[physicalPanelNumber]);
            break;
        case NsharpConstants.PAGE_D2DLITE:
            drawPanel11(target, panelRectArray[physicalPanelNumber]);
            break;
        case NsharpConstants.PAGE_FUTURE:
            drawPanel12(target, panelRectArray[physicalPanelNumber]);
            break;
        default:
            break;
        }
    }

    public void setUserPickedParcelLine(Coordinate c) {
        // make sure is in virtualPanel 1 as Parcel Line is defined in it.
        if (rscHandler != null) {
            if (!sumP1Visible) {
                return;
            }
        } else {
            return;
        }
        // make sure within parcel line area
        if (c.y >= parcelLineYStart && c.y <= parcelLineYEnd) {
            int index = ((int) (c.y - parcelLineYStart)) / (int) charHeight;
            if (index < NsharpLibSndglib.PARCEL_MAX) {
                currentParcel = (index + 1);
                // notify rscHandler
                rscHandler.setCurrentParcel(currentParcel);
            }
        }
    }

    private void drawPanel1(IGraphicsTarget target, Rectangle rect) throws VizException {
        IFont myfont;

        myfont = defaultFont;
        defineCharHeight(myfont);
        myfont.setSmoothing(false);
        myfont.setScaleFont(false);
        sumP1Visible = true;
        extent = new PixelExtent(rect);
        target.setupClippingPlane(extent);

        /*
         * Chin's NOTE:::: This pages based on BigSharp data page display
         */

        String textStr = "", CAPE3Str = "", NCAPEStr = "";
        curY = rect.y;

        //
        // Start with Parcel Data
        //
        Rectangle2D strBD = target.getStringBounds(myfont, PAGE1TEXT1_FCST_STR + "XX");
        double hRatio = paintProps.getView().getExtent().getWidth() / paintProps.getCanvasBounds().width;
        double startX = rect.x + 0.5 * charWidth;
        // 6 parameters CAPE, CINH, LCL, LI, LFC, EL per line
        double widthGap = (rect.width - strBD.getWidth() * hRatio * xRatio) / 6;
        double firstToken = rect.x + strBD.getWidth() * hRatio * xRatio;
        double secondToken = firstToken + widthGap;
        double thirdToken = secondToken + widthGap;
        double forthToken = thirdToken + widthGap;
        double fifthToken = forthToken + widthGap;
        double sixthToken = fifthToken + widthGap;
        RGB textColor = NsharpConstants.color_white;
        target.drawString(myfont, "PARCEL", startX, rect.y, 0.0, TextStyle.NORMAL, textColor, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        target.drawString(myfont, "CAPE", firstToken, rect.y, 0.0, TextStyle.NORMAL, textColor,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        target.drawString(myfont, "CIN", secondToken, rect.y, 0.0, TextStyle.NORMAL, textColor,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        target.drawString(myfont, "LCL", thirdToken, rect.y, 0.0, TextStyle.NORMAL, textColor, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        target.drawString(myfont, "LI", forthToken, rect.y, 0.0, TextStyle.NORMAL, textColor, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        target.drawString(myfont, "LFC", fifthToken, rect.y, 0.0, TextStyle.NORMAL, textColor, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        target.drawString(myfont, "EL", sixthToken, rect.y, 0.0, TextStyle.NORMAL, textColor, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        curY = curY + charHeight;
        target.drawLine(rect.x, curY, 0.0, rect.x + rect.width, curY, 0.0, textColor, 1);
        parcelLineYStart = curY;

        strBD = target.getStringBounds(myfont, "CAPE");
        firstToken = firstToken + strBD.getWidth() * hRatio * xRatio;
        strBD = target.getStringBounds(myfont, "CIN");
        secondToken = secondToken + strBD.getWidth() * hRatio * xRatio;
        strBD = target.getStringBounds(myfont, "LCL");
        thirdToken = thirdToken + strBD.getWidth() * hRatio * xRatio;
        strBD = target.getStringBounds(myfont, "LI");
        forthToken = forthToken + strBD.getWidth() * hRatio * xRatio;
        strBD = target.getStringBounds(myfont, "LFC");
        fifthToken = fifthToken + strBD.getWidth() * hRatio * xRatio;
        strBD = target.getStringBounds(myfont, "EL   ");
        sixthToken = sixthToken + strBD.getWidth() * hRatio * xRatio;
        for (int parcelNumber = 1; parcelNumber <= NsharpLibSndglib.PARCEL_MAX; parcelNumber++) {
            Parcel parcel = weatherDataStore.getParcelMap().get(parcelNumber);
            textColor = NsharpConstants.color_white;
            // draw parcel name
            if (parcelNumber == NsharpLibSndglib.PARCELTYPE_MOST_UNSTABLE) {
                NsharpWeatherDataStore.ParcelMiscParams parcelMiscs = weatherDataStore.getParcelMiscParamsMap()
                        .get(parcelNumber);
                if (parcelMiscs != null) {
                    textStr = parcelMiscs.getMuName();
                }
            } else {
                textStr = parcelToTypeStrMap.get(parcelNumber);
            }
            if (parcelNumber == currentParcel) {
                PixelExtent pixExt = new PixelExtent(rect.x, rect.x + rect.width, curY, curY + charHeight);
                target.drawRect(pixExt, NsharpConstants.color_gold, 1.0f, 1.0f);
                textColor = NsharpConstants.color_cyan_md;
            }
            target.drawString(myfont, textStr, startX, curY, 0.0, TextStyle.NORMAL, textColor, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);

            // draw CAPE
            float cape = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
            if (parcel != null && NsharpLibBasics.qc(parcel.getBplus())) {
                cape = parcel.getBplus();
                if (parcelNumber == currentParcel) {
                    if (cape < 100) {
                        textColor = NsharpConstants.color_brown;
                    } else if (cape < 500) {
                        textColor = NsharpConstants.color_darkorange;
                    } else if (cape < 1000) {
                        textColor = NsharpConstants.color_apricot;
                    } else if (cape < 2500) {
                        textColor = NsharpConstants.color_gold;
                    } else if (cape < 4000) {
                        textColor = NsharpConstants.color_red;
                    } else {
                        textColor = NsharpConstants.color_magenta;
                    }
                }
                target.drawString(myfont, String.format("%.0f", cape), firstToken, curY, 0.0, TextStyle.NORMAL,
                        textColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, null);
            } else {
                target.drawString(myfont, "M", firstToken, curY, 0.0, TextStyle.NORMAL, NsharpConstants.color_white,
                        HorizontalAlignment.RIGHT, VerticalAlignment.TOP, null);
            }
            // CIN
            if (parcel != null && NsharpLibBasics.qc(parcel.getBminus())) {
                if (parcelNumber == currentParcel) {
                    // set color according to CAPE first
                    if (cape != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA && cape < 1) {
                        textColor = NsharpConstants.color_brown;
                    } else {
                        textColor = NsharpConstants.color_lawngreen;
                    }
                    // then set according to CIN value
                    float cin = parcel.getBminus();
                    if (cin < -100) {
                        textColor = NsharpConstants.color_brown;
                    } else if (cin < -50) {
                        textColor = NsharpConstants.color_darkorange;
                    } else if (cin < -25) {
                        textColor = NsharpConstants.color_darkgreen;
                    } else if (cin < -10) {
                        textColor = NsharpConstants.color_mdgreen;
                    }
                }
                target.drawString(myfont, String.format("%.0f", parcel.getBminus()), secondToken, curY, 0.0,
                        TextStyle.NORMAL, textColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, null);
            } else {
                target.drawString(myfont, "M", secondToken, curY, 0.0, TextStyle.NORMAL, NsharpConstants.color_white,
                        HorizontalAlignment.RIGHT, VerticalAlignment.TOP, null);
            }
            // draw LCL
            if (parcel != null && NsharpLibBasics.qc(NsharpLibBasics.ftom(parcel.getLclAgl()))) {
                float lcl = NsharpLibBasics.ftom(parcel.getLclAgl());
                if (parcelNumber == currentParcel) {
                    if (lcl < 500) {
                        textColor = NsharpConstants.color_lawngreen;
                    } else if (lcl < 1000) {
                        textColor = NsharpConstants.color_mdgreen;
                    } else if (lcl < 1500) {
                        textColor = NsharpConstants.color_darkgreen;
                    } else if (lcl < 2000) {
                        textColor = NsharpConstants.color_darkorange;
                    } else {
                        textColor = NsharpConstants.color_brown;
                    }
                }
                target.drawString(myfont, String.format("%.0fm", lcl), thirdToken, curY, 0.0, TextStyle.NORMAL,
                        textColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, null);
            } else {
                target.drawString(myfont, "M", thirdToken, curY, 0.0, TextStyle.NORMAL, NsharpConstants.color_white,
                        HorizontalAlignment.RIGHT, VerticalAlignment.TOP, null);
            }
            // draw LI
            if (parcel != null && NsharpLibBasics.qc(parcel.getLi5()) && parcel.getLi5() < 100) {
                target.drawString(myfont, String.format("%5.0f", parcel.getLi5()), forthToken, curY, 0.0,
                        TextStyle.NORMAL, NsharpConstants.color_white, HorizontalAlignment.RIGHT, VerticalAlignment.TOP,
                        null);
            } else {
                target.drawString(myfont, "M", forthToken, curY, 0.0, TextStyle.NORMAL, NsharpConstants.color_white,
                        HorizontalAlignment.RIGHT, VerticalAlignment.TOP, null);
            }
            // draw LFC
            if (parcel != null && NsharpLibBasics.qc(NsharpLibBasics.ftom(parcel.getLfcAgl()))) {
                target.drawString(myfont, String.format("%.0fm", NsharpLibBasics.ftom(parcel.getLfcAgl())), fifthToken,
                        curY, 0.0, TextStyle.NORMAL, NsharpConstants.color_white, HorizontalAlignment.RIGHT,
                        VerticalAlignment.TOP, null);
            } else {
                target.drawString(myfont, "M", fifthToken, curY, 0.0, TextStyle.NORMAL, NsharpConstants.color_white,
                        HorizontalAlignment.RIGHT, VerticalAlignment.TOP, null);
            }
            // draw EL
            if (parcel != null && NsharpLibBasics.qc(parcel.getElAgl())) {
                target.drawString(myfont, String.format("%.0f'", parcel.getElAgl()), sixthToken, curY, 0.0,
                        TextStyle.NORMAL, NsharpConstants.color_white, HorizontalAlignment.RIGHT, VerticalAlignment.TOP,
                        null);
            } else {
                target.drawString(myfont, "M", sixthToken, curY, 0.0, TextStyle.NORMAL, NsharpConstants.color_white,
                        HorizontalAlignment.RIGHT, VerticalAlignment.TOP, null);
            }
            curY = curY + charHeight;

        }
        target.drawLine(rect.x, curY, 0.0, rect.x + rect.width, curY, 0.0, NsharpConstants.color_white, 1);
        parcelLineYEnd = curY;

        // THERMO DYNAMIC DATA
        firstToken = rect.x + rect.width / 48 * 12 * xRatio;
        secondToken = rect.x + rect.width / 48 * 27 * xRatio;
        thirdToken = rect.x + rect.width / 48 * 39 * xRatio;
        DrawableString str = new DrawableString("ABCDEF", NsharpConstants.color_white);
        str.font = myfont;
        double equalSignPos = (startX + target.getStringsBounds(str).getWidth()) * hRatio * xRatio;

        float pw = weatherDataStore.getPw();
        if (pw >= 0) {
            textStr = String.format("%.2fin", pw);
        } else {
            textStr = " M";
        }
        str.setCoordinates(startX, curY);
        str.horizontalAlignment = HorizontalAlignment.LEFT;
        str.verticallAlignment = VerticalAlignment.TOP;
        str.font = myfont;
        str.setText("PW = " + textStr, NsharpConstants.color_white);
        DrawableString str1 = new DrawableString(textStr, NsharpConstants.color_white);
        str1.setCoordinates(equalSignPos, curY);
        str1.horizontalAlignment = HorizontalAlignment.LEFT;
        str1.verticallAlignment = VerticalAlignment.TOP;
        str1.font = myfont;
        target.drawStrings(str);

        // 3CAPE
        Parcel parcel = weatherDataStore.getParcelMap().get(NsharpLibSndglib.PARCELTYPE_MEAN_MIXING);
        if (parcel != null && NsharpLibBasics.qc(parcel.getCape3km())) {
            CAPE3Str = String.format("%.0fJ/kg", parcel.getCape3km());
        } else {
            CAPE3Str = " M";
        }

        str.setText("3CAPE = " + CAPE3Str, NsharpConstants.color_white);
        str.setCoordinates(firstToken, curY);
        str1.setText(CAPE3Str, NsharpConstants.color_white);
        str1.setCoordinates(firstToken + equalSignPos, curY);
        target.drawStrings(str);

        float wbzft = weatherDataStore.getWbzft();
        if (NsharpLibBasics.qc(wbzft)) {
            textStr = String.format("%.0f'", wbzft);
        } else {
            textStr = " M";
        }
        str.setText("WBZ = " + textStr, NsharpConstants.color_white);
        str.setCoordinates(secondToken, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(secondToken + equalSignPos, curY);
        target.drawStrings(str);

        float wndg = weatherDataStore.getWndg();
        if (NsharpLibBasics.qc(wndg)) {
            textStr = String.format("%.2f", wndg);
        } else {
            textStr = " M";
        }
        str.setText("WNDG = " + textStr, NsharpConstants.color_white);
        str.setCoordinates(thirdToken, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(thirdToken + equalSignPos, curY);
        target.drawStrings(str);

        // move to new line
        curY = curY + charHeight;

        float kIndex = weatherDataStore.getkIndex();
        if (NsharpLibBasics.qc(kIndex)) {
            textStr = String.format("%.0f", kIndex);
        } else {
            textStr = " M";
        }
        str.setText("K = " + textStr, NsharpConstants.color_white);
        str.setCoordinates(startX, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(equalSignPos, curY);
        target.drawStrings(str);
        // DCAPE
        float dcape = weatherDataStore.getDcape();

        if (NsharpLibBasics.qc(dcape)) {
            textStr = String.format("%.0fJ/kg", dcape);
        } else {
            textStr = " M";
        }
        str.setText("DCAPE = " + textStr, NsharpConstants.color_white);
        str.setCoordinates(firstToken, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(firstToken + equalSignPos, curY);
        target.drawStrings(str);

        // FZL
        float fgzft = weatherDataStore.getFgzft();
        if (NsharpLibBasics.qc(fgzft)) {
            textStr = String.format("%.0f'", fgzft);
        } else {
            textStr = " M";
        }
        str.setText("FZL = " + textStr, NsharpConstants.color_white);
        str.setCoordinates(secondToken, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(secondToken + equalSignPos, curY);
        target.drawStrings(str);
        // ESP
        float esp = weatherDataStore.getEsp();
        if (NsharpLibBasics.qc(esp)) {
            textStr = String.format("%.2f", esp);
        } else {
            textStr = " M";
        }
        str.setText("ESP = " + textStr, NsharpConstants.color_white);
        str.setCoordinates(thirdToken, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(thirdToken + equalSignPos, curY);
        target.drawStrings(str);

        // move to new line
        curY = curY + charHeight;

        // MidRH
        float midRh = weatherDataStore.getMidRh();
        if (NsharpLibBasics.qc(midRh)) {
            textStr = String.format("%.0f%c", midRh, NsharpConstants.PERCENT_SYMBOL);
        } else {
            textStr = " M";
        }
        str.setText("MidRH = " + textStr, NsharpConstants.color_white);
        str.setCoordinates(startX, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(equalSignPos, curY);
        target.drawStrings(str);
        // DownT
        float downT = weatherDataStore.getDownT();
        if (NsharpLibBasics.qc(downT)) {
            textStr = String.format("%.0fF", downT);
        } else {
            textStr = " M";
        }
        str.setText("DownT = " + textStr, NsharpConstants.color_white);
        str.setCoordinates(firstToken, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(firstToken + equalSignPos, curY);
        target.drawStrings(str);
        // ConvT
        float conTempF = weatherDataStore.getConvT();
        if (NsharpLibBasics.qc(conTempF)) {
            textStr = String.format(" %.0fF", conTempF);
        } else {
            textStr = " M";
        }
        str.setText("ConvT = " + textStr, NsharpConstants.color_white);
        str.setCoordinates(secondToken, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(secondToken + equalSignPos, curY);
        target.drawStrings(str);

        // MMP: Coniglio MCS Maintenance Parameter
        float mmp = weatherDataStore.getMmp();
        if (NsharpLibBasics.qc(mmp)) {
            textStr = String.format("%.2f", mmp);
        } else {
            textStr = " M";
        }
        str.setText("MMP = " + textStr, NsharpConstants.color_white);
        str.setCoordinates(thirdToken, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(thirdToken + equalSignPos, curY);
        target.drawStrings(str);

        // move to new line
        curY = curY + charHeight;
        // lowRH
        float lowRh = weatherDataStore.getLowRh();
        if (NsharpLibBasics.qc(lowRh)) {
            textStr = String.format("%.0f%c", lowRh, NsharpConstants.PERCENT_SYMBOL);
        } else {
            textStr = " M";
        }
        str.setText("LowRH = " + textStr, NsharpConstants.color_white);
        str.setCoordinates(startX, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(equalSignPos, curY);
        target.drawStrings(str);

        if (NsharpLibBasics.qc(weatherDataStore.getMeanMixRatio())) {
            textStr = String.format("%.1fg/kg", weatherDataStore.getMeanMixRatio());
        } else {
            textStr = " M";
        }
        str.setText("MeanW =" + textStr, NsharpConstants.color_white);
        str.setCoordinates(firstToken, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(firstToken + equalSignPos, curY);
        target.drawStrings(str);

        float maxT = weatherDataStore.getMaxTemp();
        if (NsharpLibBasics.qc(maxT)) {
            textStr = String.format("%.0fF", maxT);
        } else {
            textStr = " M";
        }
        str.setText("MaxT = " + textStr, NsharpConstants.color_white);
        str.setCoordinates(secondToken, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(secondToken + equalSignPos, curY);
        target.drawStrings(str);

        // NCAPE
        float ncape = weatherDataStore.getNcape();
        if (NsharpLibBasics.qc(ncape)) {
            NCAPEStr = String.format("%.2f", ncape);
        } else {
            NCAPEStr = " M";
        }
        str.setText("NCAPE =" + NCAPEStr, NsharpConstants.color_white);
        str.setCoordinates(thirdToken, curY);
        str1.setText(NCAPEStr, NsharpConstants.color_white);
        str1.setCoordinates(thirdToken + equalSignPos, curY);
        target.drawStrings(str);

        // move to new line
        curY = curY + charHeight;
        target.drawLine(rect.x, curY, 0.0, rect.x + rect.width, curY, 0.0, NsharpConstants.color_white, 1);

        // draw a vertical line from 2/3 of x axis
        str.setText("sfc-3km AglLapseRate=xxC/x.xC/kmX", NsharpConstants.color_black);
        str.font = myfont;
        double lineXPos = target.getStringsBounds(str).getWidth() * hRatio * xRatio;
        if (lineXPos < rect.width * 2 / 3) {
            lineXPos = rect.width * 2 / 3;
        }
        firstToken = rect.x + lineXPos;
        target.drawLine(firstToken, curY, 0.0, firstToken, rect.y + rect.height, 0.0, NsharpConstants.color_white, 1);

        firstToken = firstToken + 0.5 * charWidth;

        // more thermodynamic data
        // the following follow show_parcel_new() at xwvid.c of bigNsharp
        // implementation
        // sfc-3km Lapse rate
        //
        float sfcTo3kmTempDelta = weatherDataStore.getSfcTo3kmTempDelta();
        float sfcTo3kmLapseRate = weatherDataStore.getSfcTo3kmLapseRate();
        if (NsharpLibBasics.qc(sfcTo3kmLapseRate)) {
            textStr = String.format("%.0fC/%.1fC/km", sfcTo3kmTempDelta, sfcTo3kmLapseRate);
        } else {
            textStr = " M";
        }
        str.setText("sfc-3km Agl LapseRate= ", NsharpConstants.color_white);
        str.setCoordinates(startX, curY);
        equalSignPos = rect.x + target.getStringsBounds(str).getWidth() * hRatio * xRatio;
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(equalSignPos, curY);
        target.drawStrings(str, str1);

        textColor = NsharpConstants.color_white;
        // "Super cell"
        float superCell = weatherDataStore.getScp();
        if (NsharpLibBasics.qc(superCell)) {
            textStr = String.format("%.1f", superCell);
            if (superCell < -0.45) {
                textColor = NsharpConstants.color_cyan;
            } else if (superCell < 0.45) {
                textColor = NsharpConstants.color_brown;
            } else if (superCell < 1.95) {
                textColor = NsharpConstants.color_darkorange;
            } else if (superCell < 11.95) {
                textColor = NsharpConstants.color_gold;
            } else if (superCell < 19.95) {
                textColor = NsharpConstants.color_red;
            } else {
                textColor = NsharpConstants.color_magenta;
            }
        } else {
            textStr = " M";
        }
        str.setText("Supercell=  ", textColor);
        double equalSignPos1 = firstToken + target.getStringsBounds(str).getWidth() * hRatio * xRatio;
        str.setCoordinates(firstToken, curY);
        str1.setText(textStr, textColor);
        str1.setCoordinates(equalSignPos1, curY);
        target.drawStrings(str, str1);

        // move to new line
        curY = curY + charHeight;

        // 3km-6km Lapse rate
        float threeKmTo6kmTempDelta = weatherDataStore.getThreeKmTo6kmTempDelta();
        float threeKmTo6kmLapseRate = weatherDataStore.getThreeKmTo6kmLapseRate();
        if (NsharpLibBasics.qc(threeKmTo6kmLapseRate)) {
            textStr = String.format("%.0fC/%.1fC/km", threeKmTo6kmTempDelta, threeKmTo6kmLapseRate);

        } else {
            textStr = " M";
        }
        str.setText("3-6km Agl LapseRate=", NsharpConstants.color_white);
        str.setCoordinates(startX, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(equalSignPos, curY);
        target.drawStrings(str, str1);
        textColor = NsharpConstants.color_white;
        float cin = weatherDataStore.getStpCin();
        if (NsharpLibBasics.qc(cin)) {
            textStr = String.format("%.1f", cin);
            if (cin < 0.5) {
                textColor = NsharpConstants.color_brown;
            } else if (cin < 1) {
                textColor = NsharpConstants.color_darkorange;
            } else if (cin < 2) {
                textColor = NsharpConstants.color_white;
            } else if (cin < 4) {
                textColor = NsharpConstants.color_gold;
            } else if (cin < 8) {
                textColor = NsharpConstants.color_red;
            } else {
                textColor = NsharpConstants.color_magenta;
                                                          
            }
        } else {
            textStr = " M";
        }
        str.setText("STP(eff)=", textColor);
        str.setCoordinates(firstToken, curY);
        str1.setText(textStr, textColor);
        str1.setCoordinates(equalSignPos1, curY);
        target.drawStrings(str, str1);

        // move to new line
        curY = curY + charHeight;

        float eight50To500mbTempDelta = weatherDataStore.getEight50To500mbTempDelta();
        float eight50To500mbLapseRate = weatherDataStore.getEight50To500mbLapseRate();
        if (NsharpLibBasics.qc(eight50To500mbTempDelta) && NsharpLibBasics.qc(eight50To500mbLapseRate)) {
            textStr = String.format("%.0fC/%.1fC/km", eight50To500mbTempDelta, eight50To500mbLapseRate);
        } else {
            textStr = " M";
        }
        str.setText("850-500mb LapseRate=", NsharpConstants.color_white);
        str.setCoordinates(startX, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(equalSignPos, curY);
        target.drawStrings(str, str1);
        textColor = NsharpConstants.color_white;
        float fixedStp = weatherDataStore.getStpFixed();
        if (NsharpLibBasics.qc(fixedStp)) {
            textStr = String.format("%.1f", fixedStp);
            if (fixedStp < 0.5) {
                textColor = NsharpConstants.color_brown;
            } else if (fixedStp < 1) {
                textColor = NsharpConstants.color_darkorange;
            } else if (fixedStp < 2) {
                textColor = NsharpConstants.color_white;
            } else if (fixedStp < 5) {
                textColor = NsharpConstants.color_gold;
            } else if (fixedStp < 7) {
                textColor = NsharpConstants.color_red;
            } else {
                textColor = NsharpConstants.color_magenta;
            }
        } else {
            textStr = " M";
        }
        str.setText("STP(fixed)=", textColor);
        str.setCoordinates(firstToken, curY);
        str1.setText(textStr, textColor);
        str1.setCoordinates(equalSignPos1, curY);
        target.drawStrings(str, str1);

        // move to new line
        curY = curY + charHeight;

        float sevenHundredTo500mbTempDelta = weatherDataStore.getSevenHundredTo500mbTempDelta();
        float sevenHundredTo500mbLapseRate = weatherDataStore.getSevenHundredTo500mbLapseRate();
        if (NsharpLibBasics.qc(sevenHundredTo500mbTempDelta) && NsharpLibBasics.qc(sevenHundredTo500mbLapseRate)) {
            textStr = String.format("%.0fC/%.1fC/km", sevenHundredTo500mbTempDelta, sevenHundredTo500mbLapseRate);
        } else {
            textStr = " M";
        }
        str.setText("700-500mb LapseRate=", NsharpConstants.color_white);
        str.setCoordinates(startX, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(equalSignPos, curY);
        target.drawStrings(str, str1);
        // "SHIP"
        textColor = NsharpConstants.color_white;
        float ship = weatherDataStore.getShip();
        if (NsharpLibBasics.qc(ship)) {
            textStr = String.format("%.1f", ship);
            if (ship < 0.45) {
                textColor = NsharpConstants.color_brown;
            } else if (ship < 0.95) {
                textColor = NsharpConstants.color_white;
            } else if (ship < 1.95) {
                textColor = NsharpConstants.color_gold;
            } else if (ship < 4.95) {
                textColor = NsharpConstants.color_red;
            } else {
                textColor = NsharpConstants.color_magenta;
            }
        } else {
            textStr = " M";
        }
        str.setText("SHIP=", textColor);
        str.setCoordinates(firstToken, curY);
        str1.setText(textStr, textColor);
        str1.setCoordinates(equalSignPos1, curY);
        target.drawStrings(str, str1);
    }

    private void drawPanel2(IGraphicsTarget target, Rectangle rect) throws VizException {
        IFont myfont;

        /*
         * Chin's NOTE:::: This pages based on BigNsharp show_shear_new() at
         * xwvid3.c
         */
        myfont = defaultFont;
        defineCharHeight(myfont);
        myfont.setSmoothing(false);
        myfont.setScaleFont(false);
        extent = new PixelExtent(rect);
        target.setupClippingPlane(extent);
        curY = rect.y;
        String textStr;

        //
        // Start with Header
        //
        double startX = rect.x + 0.5 * charWidth;
        // 4 parameters, SRH(m%c/s%c) Shear(kt) MnWind SRW, plus 1 header = 5
        double widthGap = (rect.width) / 5;
        double firstToken = rect.x + widthGap;
        double secondToken = firstToken + widthGap;
        double thirdToken = secondToken + widthGap;
        double forthToken = thirdToken + widthGap;
        target.drawString(myfont, "Sum2", startX, rect.y, 0.0, TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);

        textStr = String.format("SRH(m%c/s%c)", NsharpConstants.SQUARE_SYMBOL, NsharpConstants.SQUARE_SYMBOL);
        target.drawString(myfont, textStr, firstToken, curY, 0.0, TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        target.drawString(myfont, "Shear(kt)", secondToken, curY, 0.0, TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        target.drawString(myfont, "MnWind", thirdToken, curY, 0.0, TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        target.drawString(myfont, "SRW", forthToken, curY, 0.0, TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        // move to new line
        curY = curY + charHeight;
        target.drawLine(rect.x, curY, 0.0, rect.x + rect.width, curY, 0.0, NsharpConstants.color_white, 1);

        for (int i = 0; i < NsharpWeatherDataStore.STORM_MOTION_TYPE_STR.length; i++) {
            RGB textColor = NsharpConstants.color_white;
            if ("Eff Inflow".equals(NsharpWeatherDataStore.STORM_MOTION_TYPE_STR[i])
                    || "Eff Shear(EBWD)".equals(NsharpWeatherDataStore.STORM_MOTION_TYPE_STR[i])) {
                textColor = NsharpConstants.color_yellow;
            }
            // draw row header
            target.drawString(myfont, NsharpWeatherDataStore.STORM_MOTION_TYPE_STR[i], startX, curY, 0.0,
                    TextStyle.NORMAL, textColor, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);

            // draw helicity only for the first 4 rows as Bigsharp does
            if (i < STORM_HELICITY_MAX) {
                float totHeli = weatherDataStore.getStormTypeToHelicityMap()
                        .get(NsharpWeatherDataStore.STORM_MOTION_TYPE_STR[i]).getTotalHelicity();

                if (NsharpLibBasics.qc(totHeli)) {
                    textStr = String.format("%.0f", totHeli);
                } else {
                    textStr = "M";
                }
                target.drawString(myfont, textStr, firstToken, curY, 0.0, TextStyle.NORMAL, textColor,
                        HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
            }

            float windShear = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
            WindComponent meanwindComp = null;
            WindComponent srMeanwindComp = null;
            // LCL layer -EL layer value is parcel relevant, get them from
            // parcelMap
            if ("LCL-EL(Cloud Layer)".equals(NsharpWeatherDataStore.STORM_MOTION_TYPE_STR[i])) {
                NsharpWeatherDataStore.ParcelMiscParams parcelMiscs = weatherDataStore.getParcelMiscParamsMap()
                        .get(this.currentParcel);
                if (parcelMiscs != null) {
                    windShear = parcelMiscs.getWindShearLclToEl();
                    meanwindComp = parcelMiscs.getMeanWindCompLclToEl();
                    srMeanwindComp = parcelMiscs.getSrMeanWindCompLclToEl();
                }
            } else {
                windShear = weatherDataStore.getStormTypeToWindShearMap()
                        .get(NsharpWeatherDataStore.STORM_MOTION_TYPE_STR[i]);
                meanwindComp = weatherDataStore.getStormTypeToMeanWindMap()
                        .get(NsharpWeatherDataStore.STORM_MOTION_TYPE_STR[i]);
                srMeanwindComp = weatherDataStore.getStormTypeToSrMeanWindMap()
                        .get(NsharpWeatherDataStore.STORM_MOTION_TYPE_STR[i]);
            }
            // draw wind shear
            if (NsharpLibBasics.qc(windShear)) {
                textStr = String.format("%.0f", windShear);
            } else {
                textStr = "M";
            }
            target.drawString(myfont, textStr, secondToken, curY, 0.0, TextStyle.NORMAL, textColor,
                    HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);

            // draw mean wind
            if (meanwindComp != null && NsharpLibBasics.qc(meanwindComp.getWdir())
                    && NsharpLibBasics.qc(meanwindComp.getWspd())) {
                textStr = String.format("%.0f/%.0f", meanwindComp.getWdir(), meanwindComp.getWspd());
            } else {
                textStr = "M";
            }
            target.drawString(myfont, textStr, thirdToken, curY, 0.0, TextStyle.NORMAL, textColor,
                    HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);

            // draw pressure-weighted SR mean wind
            if (srMeanwindComp != null && NsharpLibBasics.qc(srMeanwindComp.getWdir())
                    && NsharpLibBasics.qc(srMeanwindComp.getWspd())) {
                textStr = String.format("%.0f/%.0f", srMeanwindComp.getWdir(), srMeanwindComp.getWspd());
            } else {
                textStr = "M";
            }
            target.drawString(myfont, textStr, forthToken, curY, 0.0, TextStyle.NORMAL, textColor,
                    HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);

            if ("Eff Inflow".equals(NsharpWeatherDataStore.STORM_MOTION_TYPE_STR[i])
                    || "Eff Shear(EBWD)".equals(NsharpWeatherDataStore.STORM_MOTION_TYPE_STR[i])) {
                // draw bax around it
                Rectangle rectangle = new Rectangle(rect.x, (int) curY, rect.width, (int) charHeight);
                PixelExtent pixExt = new PixelExtent(rectangle);
                target.drawRect(pixExt, NsharpConstants.color_darkorange, 1.0f, 1.0f);
            }

            // move to new line
            curY = curY + charHeight;
        }
        // align all following parameters output with "Corfidi Downshear"
        double hRatio = paintProps.getView().getExtent().getWidth() / paintProps.getCanvasBounds().width;
        DrawableString str = new DrawableString("Corfidi Downshear = ", NsharpConstants.color_white);
        double equalSignPos = secondToken;

        // BRN Shear
        Parcel parcel = weatherDataStore.getParcelMap().get(this.currentParcel);
        if (parcel != null && NsharpLibBasics.qc(parcel.getBrnShear())) {
            textStr = String.format("%.0f m%c/s%c", parcel.getBrnShear(), NsharpConstants.SQUARE_SYMBOL,
                    NsharpConstants.SQUARE_SYMBOL);
        } else {
            textStr = "  M";
        }
        str.setCoordinates(startX, curY);
        str.horizontalAlignment = HorizontalAlignment.LEFT;
        str.verticallAlignment = VerticalAlignment.TOP;
        str.font = myfont;
        str.setText("BRN Shear = ", NsharpConstants.color_white);
        DrawableString str1 = new DrawableString(textStr, NsharpConstants.color_white);
        str1.setCoordinates(equalSignPos, curY);
        str1.horizontalAlignment = HorizontalAlignment.LEFT;
        str1.verticallAlignment = VerticalAlignment.TOP;
        str1.font = myfont;
        target.drawStrings(str, str1);

        // move to new line
        curY = curY + charHeight;
        // 4-6km srw
        WindComponent windComp = weatherDataStore.getSrMeanWindComp4To6km();
        if (NsharpLibBasics.qc(windComp.getWdir()) && NsharpLibBasics.qc(windComp.getWspd())) {
            textStr = String.format("%.0f/%.0f kt", windComp.getWdir(), windComp.getWspd());
        } else {
            textStr = " M";
        }
        str.setText("4-6km SR Wind =", NsharpConstants.color_white);
        str.setCoordinates(startX, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(equalSignPos, curY);
        target.drawStrings(str, str1);

        // move to new line
        curY = curY + charHeight;

        // Corfidi Downshear @[0]
        WindComponent[] corfidiWindComp = weatherDataStore.getCofidiShearWindComp();
        textStr = String.format("%.0f/%.0f kt", corfidiWindComp[0].getWdir(), corfidiWindComp[0].getWspd());
        str.setText("Corfidi Downshear =", NsharpConstants.color_white);
        str.setCoordinates(startX, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(equalSignPos, curY);
        target.drawStrings(str, str1);
        // move to new line
        curY = curY + charHeight;

        // Corfidi Upshear @[1]
        textStr = String.format("%.0f/%.0f kt", corfidiWindComp[1].getWdir(), corfidiWindComp[1].getWspd());
        str.setText("Corfidi Upshear =", NsharpConstants.color_white);
        str.setCoordinates(startX, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(equalSignPos, curY);
        target.drawStrings(str, str1);
        // move to new line
        curY = curY + charHeight;

        // Bunkers Right @ [0]
        WindComponent[] bunkerStormWndComp = weatherDataStore.getBunkersStormMotionWindComp();
        textStr = String.format("%.0f/%.0f kt", bunkerStormWndComp[0].getWdir(), bunkerStormWndComp[0].getWspd());
        str.setText("Bunkers Right =", NsharpConstants.color_dkpink);
        str.setCoordinates(startX, curY);
        str1.setText(textStr, NsharpConstants.color_dkpink);
        str1.setCoordinates(equalSignPos, curY);
        target.drawStrings(str, str1);

        // move to new line
        curY = curY + charHeight;
        // Bunkers Left @ [1]
        textStr = String.format("%.0f/%.0f kt", bunkerStormWndComp[1].getWdir(), bunkerStormWndComp[1].getWspd());
        str.setText("Bunkers Left =", NsharpConstants.color_cyan);
        str.setCoordinates(startX, curY);
        str1.setText(textStr, NsharpConstants.color_cyan);
        str1.setCoordinates(equalSignPos, curY);
        target.drawStrings(str, str1);
        // move to new line
        curY = curY + charHeight;

        // STP(eff)LR -
        float stpLr = weatherDataStore.getStpLr();
        // the following color setting is based on show_shear_new() at xwvid3.c
        RGB textColor = NsharpConstants.color_brown;
        if (stpLr >= 8) {
            textColor = NsharpConstants.color_magenta;
        } else if (stpLr >= 4) {
            textColor = NsharpConstants.color_red;
        } else if (stpLr >= 2) {
            textColor = NsharpConstants.color_gold;
        } else if (stpLr >= 1) {
            textColor = NsharpConstants.color_white;
        } else if (stpLr >= 0.5) {
            textColor = NsharpConstants.color_darkorange;
        }

        if (NsharpLibBasics.qc(stpLr)) {
            textStr = String.format("%.1f", stpLr);
        } else {
            textStr = " M";
        }
        str.setText("STP(eff)LR =", textColor);
        str.setCoordinates(startX, curY);
        str1.setText(textStr, textColor);
        str1.setCoordinates(equalSignPos, curY);
        target.drawStrings(str, str1);

        // wind barb labels
        // rest str1 to get a right x position
        textStr = "1km";
        str.setText(textStr, NsharpConstants.color_red);
        str.setCoordinates(thirdToken, curY);
        textStr = " & ";
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(thirdToken + target.getStringsBounds(str).getWidth() * hRatio * xRatio, curY);
        textStr = "6km";
        DrawableString str2 = new DrawableString(textStr, NsharpConstants.color_cyan);
        str2.horizontalAlignment = HorizontalAlignment.LEFT;
        str2.verticallAlignment = VerticalAlignment.TOP;
        str2.font = myfont;
        str2.setCoordinates(
                thirdToken + (target.getStringsBounds(str).getWidth() + target.getStringsBounds(str1).getWidth())
                        * hRatio * xRatio,
                curY);
        textStr = " AGL Wind Barb";
        DrawableString str3 = new DrawableString(textStr, NsharpConstants.color_white);
        str3.horizontalAlignment = HorizontalAlignment.LEFT;
        str3.verticallAlignment = VerticalAlignment.TOP;
        str3.font = myfont;
        str3.setCoordinates(
                thirdToken + (target.getStringsBounds(str).getWidth() + target.getStringsBounds(str1).getWidth()
                        + target.getStringsBounds(str2).getWidth()) * hRatio * xRatio,
                curY);
        target.drawStrings(str, str1, str2, str3);

        // 1 km wind barb
        double wbToken = (thirdToken + forthToken) / 2;
        // rest str1 to get a right x position
        str1.setText("1234/56 ktSPACESPACESPACE", NsharpConstants.color_red);
        double yOri = curY - 3 * charHeight;
        double barbScaleF = charHeight;
        List<LineStroke> barb = WindBarbFactory.getWindGraphics(
                (double) NsharpLibBasics.i_wspd(soundingLys,
                        NsharpLibBasics.i_pres(soundingLys, NsharpLibBasics.msl(soundingLys, 1000))),
                (double) NsharpLibBasics.i_wdir(soundingLys,
                        NsharpLibBasics.i_pres(soundingLys, NsharpLibBasics.msl(soundingLys, 1000))));
        if (barb != null) {
            WindBarbFactory.scaleBarb(barb, barbScaleF);

            double cur0X = wbToken, cur0Y = yOri, cur1X = wbToken, cur1Y = yOri, newY = 0;
            WindBarbFactory.translateBarb(barb, cur0X, yOri);

            for (LineStroke stroke : barb) {
                Coordinate point = stroke.getPoint();
                // Chin NOte; when using WindBarbFactory.getWindGraphics() to
                // create barb, the Y axis is growing
                // upwards. However, on this canvas, Y axis is growing
                // downwards. Therefore, the following Y coordinate
                // adjustment is necessary.
                //
                newY = yOri - (point.y - yOri);
                // Note: stroke.render(gc, relativeX, relativeY) is not working
                // here. Therefore, need to draw wind barb ourself.
                if (stroke.getType() == "M") {
                    cur0X = point.x;
                    cur0Y = newY;
                } else if (stroke.getType() == "D") {
                    cur1X = point.x;
                    cur1Y = newY;
                    target.drawLine(cur0X, cur0Y, 0.0, cur1X, cur1Y, 0.0, NsharpConstants.color_red, 1);
                    cur0X = cur1X;
                    cur0Y = cur1Y;
                }
            }
        }
        // 6 km wind barb
        barb.clear();
        barb = WindBarbFactory.getWindGraphics(
                (double) NsharpLibBasics.i_wspd(soundingLys,
                        NsharpLibBasics.i_pres(soundingLys, NsharpLibBasics.msl(soundingLys, 6000))),
                (double) NsharpLibBasics.i_wdir(soundingLys,
                        NsharpLibBasics.i_pres(soundingLys, NsharpLibBasics.msl(soundingLys, 6000))));
        if (barb != null) {
            WindBarbFactory.scaleBarb(barb, barbScaleF);

            double cur0X = wbToken, cur0Y = yOri, cur1X = wbToken, cur1Y = yOri, newY = 0;
            WindBarbFactory.translateBarb(barb, cur0X, yOri);

            for (LineStroke stroke : barb) {
                Coordinate point = stroke.getPoint();
                // Chin NOte; when using WindBarbFactory.getWindGraphics() to
                // create barb, the Y axis is growing
                // upwards. However, on this canvas, Y axis is growing
                // downwards. Therefore, the following Y coordinate
                // adjustment is necessary.
                newY = yOri - (point.y - yOri);
                //
                // Note: stroke.render(gc, relativeX, relativeY) is not working
                // here. Therefore, need to draw wind barb ourself.
                if (stroke.getType() == "M") {
                    cur0X = point.x;
                    cur0Y = newY;
                } else if (stroke.getType() == "D") {
                    cur1X = point.x;
                    cur1Y = newY;
                    target.drawLine(cur0X, cur0Y, 0.0, cur1X, cur1Y, 0.0, NsharpConstants.color_cyan, 1);
                    cur0X = cur1X;
                    cur0Y = cur1Y;
                }
            }
        }
        // move to new line
        curY = curY + charHeight;
        // draw lhp
        float lhp = weatherDataStore.getLhp();
        if (NsharpLibBasics.qc(lhp)) {
            textStr = String.format("%.1f", lhp);
        } else {
            textStr = " M";
        }
        // the following color setting is based on show_shear_new() at xwvid3.c
        textColor = NsharpConstants.color_white;
        if (lhp >= 9) {
            // use Gempak color 7 red
            textColor = NsharpConstants.color_red;
        } else if (lhp >= 7) {
            // use Gempak color 2 magenta
            textColor = NsharpConstants.color_magenta;
        } else if (lhp >= 5) {
            // use Gempak color 17 orange
            textColor = NsharpConstants.color_orange;
        } else if (lhp >= 3) {
            // use Gempak color 12 dk pink
            textColor = NsharpConstants.color_dkpink;
        }
        str.setText("LGHAIL =", textColor);
        str.setCoordinates(startX, curY);
        str1.setText(textStr, textColor);
        str1.setCoordinates(equalSignPos, curY);
        str.font = font9;
        str1.font = font9;
        target.drawStrings(str, str1);

        // draw modSherbe
        float modSherbe = weatherDataStore.getModSherbe();
        if (NsharpLibBasics.qc(modSherbe)) {
            textStr = String.format("%.1f", modSherbe);
        } else {
            textStr = " M";
        }
        str.setText("MOSHE = " + textStr, NsharpConstants.color_white);
        str.setCoordinates(thirdToken, curY);
        target.drawStrings(str);
    }

    private void drawPanel3(IGraphicsTarget target, Rectangle rect) throws VizException {
        IFont myfont;
        myfont = defaultFont;
        defineCharHeight(myfont);
        myfont.setSmoothing(false);
        myfont.setScaleFont(false);
        extent = new PixelExtent(rect);
        target.setupClippingPlane(extent);
        String splitedStr[];
        String textStr;
        curY = rect.y;
        // Chin's NOTE::::this function is coded based on native nsharp
        // show_parcel() in xwvid3.c
        // moved from NsharpPaletteWindow.showParcelData()

        String title = PARCEL_DATA_STR;

        target.drawString(myfont, title, rect.x + rect.width / 3, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_cyan, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);

        String hdrStr;
        float layerPressure = 0;
        // get user selected parcel type
        hdrStr = parcelToHdrStrMap.get(currentParcel);
        layerPressure = NsharpWeatherDataStore.parcelToLayerPressMap.get(currentParcel);
        if (currentParcel == NsharpLibSndglib.PARCELTYPE_USER_DEFINED) {
            layerPressure = rscHandler.getCurrentParcelLayerPressure();
            hdrStr = String.format(hdrStr, layerPressure);
        }
        curY = curY + charHeight;
        target.drawString(myfont, hdrStr, rect.x + rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_yellow, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        Parcel parcel = weatherDataStore.getParcelMap().get(currentParcel);
        if (parcel != null) {
            textStr = PARCEL_LPL_LINE_;
            textStr = String.format(textStr, (int) parcel.getLplpres(), (int) parcel.getLpltemp(),
                    (int) parcel.getLpldwpt(), (int) NsharpLibBasics.ctof(parcel.getLpltemp()),
                    (int) NsharpLibBasics.ctof(parcel.getLpldwpt()));

            // Chin: note: target.drawString does NOT handle formatted string.
            // For
            // example, "ABC\tabc" will be printed
            // as "ABCabc" So, we have to add '_' to separate each value when
            // formatting String.
            // Then, use String.split() to have each value in a sub-string.
            splitedStr = textStr.split("_", -1);
            for (int i = 0; i < splitedStr.length; i++) {
                target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                        NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
            }
        } else {
            target.drawString(myfont, PARCEL_LPL_MISSING, rect.x, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }

        curY = curY + 2 * charHeight;

        if (parcel != null && NsharpLibBasics.qc(parcel.getBplus())) {
            textStr = PARCEL_CAPE_LINE;
            textStr = String.format(textStr, parcel.getBplus());
        } else {
            textStr = PARCEL_CAPE_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);

        if (parcel != null && NsharpLibBasics.qc(parcel.getLi5())) {
            textStr = PARCEL_LI_LINE;
            textStr = String.format(textStr, parcel.getLi5());
        } else {
            textStr = PARCEL_LI_MISSING;
        }
        target.drawString(myfont, textStr, rect.x + rect.width / 2, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;
        if (parcel != null && NsharpLibBasics.qc(parcel.getBfzl())) {
            textStr = PARCEL_BFZL_LINE;
            textStr = String.format(textStr, parcel.getBfzl());
        } else {
            textStr = PARCEL_BFZL_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        if (parcel != null && NsharpLibBasics.qc(parcel.getLimax())) {
            textStr = PARCEL_LIMIN_LINE;
            textStr = String.format(textStr, parcel.getLimax(), parcel.getLimaxpres());
        } else {
            textStr = PARCEL_LIMIN_MISSING;
        }
        target.drawString(myfont, textStr, rect.x + rect.width / 2, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;
        if (parcel != null && NsharpLibBasics.qc(parcel.getBminus())) {
            textStr = PARCEL_CINH_LINE;
            textStr = String.format(textStr, parcel.getBminus());
        } else {
            textStr = PARCEL_CINH_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);

        if (parcel != null && NsharpLibBasics.qc(parcel.getCap())) {
            textStr = PARCEL_CAP_LINE;
            textStr = String.format(textStr, parcel.getCap(), parcel.getCappres());
        } else {
            textStr = PARCEL_CAP_MISSING;
        }
        target.drawString(myfont, textStr, rect.x + rect.width / 2, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + 2 * charHeight;

        textStr = PARCEL_LEVEL_LINE_;
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;
        target.drawLine(rect.x, curY, 0.0, rect.x + rect.width, curY, 0.0, NsharpConstants.color_white, 1);

        if (parcel != null && NsharpLibBasics.qc(parcel.getLclpres()) && NsharpLibBasics.qc(parcel.getLclAgl())) {
            textStr = PARCEL_LCL_LINE_;
            textStr = String.format(textStr, parcel.getLclpres(), parcel.getLclAgl());
        } else {
            textStr = PARCEL_LCL_MISSING_;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        if (parcel != null && NsharpLibBasics.qc(parcel.getLfcpres()) && NsharpLibBasics.qc(parcel.getLfcAgl())
                && NsharpLibBasics.qc(parcel.getLfcTemp())) {
            textStr = PARCEL_LFC_LINE_;
            textStr = String.format(textStr, parcel.getLfcpres(), parcel.getLfcAgl(), parcel.getLfcTemp());
        } else {
            textStr = PARCEL_LFC_MISSING_;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        if (parcel != null && NsharpLibBasics.qc(parcel.getElpres()) && NsharpLibBasics.qc(parcel.getElAgl())
                && NsharpLibBasics.qc(parcel.getElTemp())) {
            textStr = PARCEL_EL_LINE_;
            textStr = String.format(textStr, parcel.getElpres(), parcel.getElAgl(), parcel.getElTemp());
        } else {
            textStr = PARCEL_EL_MISSING_;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        if (parcel != null && NsharpLibBasics.qc(parcel.getMplpres()) && NsharpLibBasics.qc(parcel.getMplAgl())) {
            textStr = PARCEL_MPL_LINE_;
            textStr = String.format(textStr, parcel.getMplpres(), parcel.getMplAgl());
        } else {
            textStr = PARCEL_MPL_MISSING_;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }

    }

    private void drawPanel4(IGraphicsTarget target, Rectangle rect) throws VizException {
        IFont myfont;
        myfont = defaultFont;
        defineCharHeight(myfont);
        myfont.setSmoothing(false);
        myfont.setScaleFont(false);
        extent = new PixelExtent(rect);
        target.setupClippingPlane(extent);
        String textStr;
        curY = rect.y;

        /*
         * Chin's NOTE::::this function is coded based on legacy native nsharp
         * software show_thermoparms(), show_moisture(),show_instability() in
         * xwvid3.c
         */
        target.drawString(myfont, NsharpNativeConstants.THERMO_DATA_STR, rect.x + rect.width / 3, curY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_cyan, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;
        target.drawString(myfont, NsharpNativeConstants.THERMO_MOISTURE_STR, rect.x + rect.width / 4, curY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_yellow, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        float pw = weatherDataStore.getPw();
        if (pw >= 0) {
            textStr = NsharpNativeConstants.THERMO_PWATER_LINE;
            textStr = String.format(textStr, pw);
        } else {
            textStr = NsharpNativeConstants.THERMO_PWATER_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);

        float meanRh = weatherDataStore.getMeanRh();
        if (NsharpLibBasics.qc(meanRh)) {
            textStr = NsharpNativeConstants.THERMO_MEANRH_LINE;
            textStr = String.format(textStr, meanRh, NsharpConstants.PERCENT_SYMBOL);
        } else {
            textStr = NsharpNativeConstants.THERMO_MEANRH_MISSING;
        }
        target.drawString(myfont, textStr, rect.x + rect.width / 2, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        if (NsharpLibBasics.qc(weatherDataStore.getMeanMixRatio())) {
            textStr = NsharpNativeConstants.THERMO_MEANW_LINE;
            textStr = String.format(textStr, weatherDataStore.getMeanMixRatio());
        } else {
            textStr = NsharpNativeConstants.THERMO_MEANW_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);

        // mean LRH value
        if (NsharpLibBasics.qc(weatherDataStore.getLowRh())) {
            textStr = NsharpNativeConstants.THERMO_MEANLRH_LINE;
            textStr = String.format(textStr, weatherDataStore.getLowRh(), NsharpConstants.PERCENT_SYMBOL);
        } else {
            textStr = NsharpNativeConstants.THERMO_MEANLRH_MISSING;
        }
        target.drawString(myfont, textStr, rect.x + rect.width / 2, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        if (NsharpLibBasics.qc(weatherDataStore.getTopMoistLyrPress())) {
            textStr = NsharpNativeConstants.THERMO_TOP_LINE;
            textStr = String.format(textStr, weatherDataStore.getTopMoistLyrPress(),
                    weatherDataStore.getTopMoistLyrHeight());
        } else {
            textStr = NsharpNativeConstants.THERMO_TOP_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        // instability data--------------//
        // yellow and bold for parcel header
        target.drawString(myfont, NsharpNativeConstants.THERMO_INSTABILITY_STR, rect.x + rect.width / 4, curY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_yellow, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);

        curY = curY + charHeight;

        float sevenHundredTo500mbTempDelta = weatherDataStore.getSevenHundredTo500mbTempDelta();
        float sevenHundredTo500mbLapseRate = weatherDataStore.getSevenHundredTo500mbLapseRate();
        if (NsharpLibBasics.qc(sevenHundredTo500mbTempDelta) && NsharpLibBasics.qc(sevenHundredTo500mbLapseRate)) {
            textStr = NsharpNativeConstants.THERMO_700500mb_LINE;
            textStr = String.format(textStr, sevenHundredTo500mbTempDelta, sevenHundredTo500mbLapseRate);
        } else {
            textStr = NsharpNativeConstants.THERMO_700500mb_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        float eight50To500mbTempDelta = weatherDataStore.getEight50To500mbTempDelta();
        float eight50To500mbLapseRate = weatherDataStore.getEight50To500mbLapseRate();

        if (NsharpLibBasics.qc(eight50To500mbTempDelta) && NsharpLibBasics.qc(eight50To500mbLapseRate)) {
            textStr = NsharpNativeConstants.THERMO_850500mb_LINE;
            textStr = String.format(textStr, eight50To500mbTempDelta, eight50To500mbLapseRate);
        } else {
            textStr = NsharpNativeConstants.THERMO_850500mb_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        // misc parameters data--------------//
        target.drawString(myfont, NsharpNativeConstants.THERMO_MISC_PARMS_STR, rect.x + rect.width / 4, curY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_yellow, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        if (NsharpLibBasics.qc(weatherDataStore.getTotTots())) {
            textStr = NsharpNativeConstants.THERMO_TOTAL_LINE;
            textStr = String.format(textStr, weatherDataStore.getTotTots());
        } else {
            textStr = NsharpNativeConstants.THERMO_TOTAL_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);

        if (NsharpLibBasics.qc(weatherDataStore.getkIndex())) {
            textStr = NsharpNativeConstants.THERMO_KINDEX_LINE;
            textStr = String.format(textStr, weatherDataStore.getkIndex());
        } else {
            textStr = NsharpNativeConstants.THERMO_KINDEX_MISSING;
        }
        target.drawString(myfont, textStr, rect.x + rect.width / 2, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        if (NsharpLibBasics.qc(weatherDataStore.getSweatIndex())) {
            textStr = NsharpNativeConstants.THERMO_SWEAT_LINE;
            textStr = String.format(textStr, weatherDataStore.getSweatIndex());
        } else {
            textStr = NsharpNativeConstants.THERMO_SWEAT_MISSING;
        }

        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);

        float maxTempF = weatherDataStore.getMaxTemp();
        if (NsharpLibBasics.qc(maxTempF)) {
            textStr = NsharpNativeConstants.THERMO_MAXT_LINE;
            textStr = String.format(textStr, maxTempF);
        } else {
            textStr = NsharpNativeConstants.THERMO_MAXT_MISSING;
        }
        target.drawString(myfont, textStr, rect.x + rect.width / 2, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        float theDiff = weatherDataStore.getThetaDiff();
        if (NsharpLibBasics.qc(theDiff)) {
            textStr = NsharpNativeConstants.THERMO_THETAE_LINE;
            textStr = String.format(textStr, theDiff);
        } else {
            textStr = NsharpNativeConstants.THERMO_THETAE_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);

        float conTempF = weatherDataStore.getConvT();
        if (NsharpLibBasics.qc(conTempF)) {
            textStr = NsharpNativeConstants.THERMO_CONVT_LINE;
            textStr = String.format(textStr, conTempF);
        } else {
            textStr = NsharpNativeConstants.THERMO_CONVT_MISSING;
        }
        target.drawString(myfont, textStr, rect.x + rect.width / 2, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        float wbzft = weatherDataStore.getWbzft();
        if (NsharpLibBasics.qc(wbzft)) {
            textStr = NsharpNativeConstants.THERMO_WBZ_LINE;
            textStr = String.format(textStr, wbzft);
        } else {
            textStr = NsharpNativeConstants.THERMO_WBZ_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);

        if (NsharpLibBasics.qc(weatherDataStore.getFgzft())) {
            textStr = NsharpNativeConstants.THERMO_FGZ_LINE;
            textStr = String.format(textStr, weatherDataStore.getFgzft());
        } else {
            textStr = NsharpNativeConstants.THERMO_FGZ_MISSING;
        }

        target.drawString(myfont, textStr, rect.x + rect.width / 2, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        // LIs
        Set<String> liSet = parcelLIStrMap.keySet();
        String[] liStr = new String[liSet.size()];
        liStr = (String[]) liSet.toArray(liStr);
        for (int parcelNumber = 1; parcelNumber <= NsharpLibSndglib.PARCEL_MAX; parcelNumber++) {
            String li = liStr[parcelNumber - 1];

            // display 2 parcels LI per line
            // Therefore, update Y position every 2 parcels,
            // and X position to rec.x and rec.x+rect.width/2 accordingly
            curY = curY + charHeight * ((parcelNumber) % 2);
            double x = rect.x + rect.width / 2 * ((parcelNumber + 1) % 2);

            Parcel parcel = weatherDataStore.getParcelMap().get(parcelLIStrMap.get(li));
            if (parcel != null && NsharpLibBasics.qc(parcel.getLi5()) && parcel.getLi5() < 100) {
                target.drawString(myfont, String.format(li + "%5.0f", parcel.getLi5()), x, curY, 0.0, TextStyle.NORMAL,
                        NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);

            } else {
                target.drawString(myfont, li + " M", x, curY, 0.0, TextStyle.NORMAL, NsharpConstants.color_white,
                        HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
            }
        }
    }

    private void drawPanel5(IGraphicsTarget target, Rectangle rect) throws VizException {
        IFont myfont;
        myfont = defaultFont;
        defineCharHeight(myfont);
        myfont.setSmoothing(false);
        myfont.setScaleFont(false);
        extent = new PixelExtent(rect);
        target.setupClippingPlane(extent);
        String splitedStr[];
        String textStr;
        curY = rect.y;

        /*
         * Chin's NOTE::::this function is coded based on legacy nsharp software
         * show_gradient() in xwvid3.c
         */
        target.drawString(myfont, NsharpNativeConstants.OPC_LOW_LEVEL_STR, rect.x + rect.width / 3, curY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_cyan, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;
        target.drawString(myfont, NsharpNativeConstants.OPC_SURFACE975_STR, rect.x + rect.width / 4, curY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_yellow, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        textStr = NsharpNativeConstants.OPC_LEVEL_LINE_;
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;
        target.drawLine(rect.x, curY, 0.0, rect.x + rect.width, curY, 0.0, NsharpConstants.color_white, 1);
        if (!NsharpLibBasics.qc(weatherDataStore.getHeight975())) {
            textStr = NsharpNativeConstants.OPC_975_LINE_MISSING_;
        } else {
            textStr = NsharpNativeConstants.OPC_975_LINE_;
            textStr = String.format(textStr, weatherDataStore.getHeight975(), weatherDataStore.getTemp975());
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        if (NsharpLibBasics.qc(weatherDataStore.getSfcLayer().getPressure())
                && NsharpLibBasics.qc(weatherDataStore.getSfcLayer().getGeoHeight())
                && NsharpLibBasics.qc(weatherDataStore.getSfcLayer().getTemperature())) {
            textStr = NsharpNativeConstants.OPC_SURFACE_LINE_;
            textStr = String.format(textStr, weatherDataStore.getSfcLayer().getPressure(),
                    weatherDataStore.getSfcLayer().getGeoHeight(), weatherDataStore.getSfcLayer().getTemperature());
        } else {
            textStr = NsharpNativeConstants.OPC_SURFACE_MISSING_;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        /* ----- Sfc-975 Grad ----- */
        /*
         * make sure both 975mb layer and surface layer temperatures are
         * available
         */
        if (NsharpLibBasics.qc(weatherDataStore.getTemp975())
                && NsharpLibBasics.qc(weatherDataStore.getSfcLayer().getTemperature())) {
            textStr = NsharpNativeConstants.OPC_975_SURFACE_LINE;
            textStr = String.format(textStr,
                    weatherDataStore.getTemp975() - weatherDataStore.getSfcLayer().getTemperature());
        } else {
            textStr = NsharpNativeConstants.OPC_975_SURFACE_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;
        /*
         * Chin's NOTE::::this function is coded based on legacy nsharp software
         * show_inversion() in xwvid3.c
         */
        // yellow and bold for parcel header
        target.drawString(myfont, NsharpNativeConstants.OPC_LOWEST_INV_STR, rect.x + rect.width / 4, curY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_yellow, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + 2 * charHeight;

        if (NsharpLibBasics.qc(weatherDataStore.getLowestInvHeight())) {
            textStr = NsharpNativeConstants.OPC_BASEHEIGHT_LINE;
            textStr = String.format(textStr, weatherDataStore.getLowestInvHeight());
        } else {
            textStr = NsharpNativeConstants.OPC_BASEHEIGHT_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        if (NsharpLibBasics.qc(weatherDataStore.getLowestInvPressure())) {
            textStr = NsharpNativeConstants.OPC_BASEPRESSURE_LINE;
            textStr = String.format(textStr, weatherDataStore.getLowestInvPressure());
        } else {
            textStr = NsharpNativeConstants.OPC_BASEPRESSURE_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        if (NsharpLibBasics.qc(weatherDataStore.getLowestInvTempChange())) {
            textStr = NsharpNativeConstants.OPC_CHANGE_IN_TEMP_LINE;
            textStr = String.format(textStr, weatherDataStore.getLowestInvTempChange());
        } else {
            textStr = NsharpNativeConstants.OPC_CHANGE_IN_TEMP_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);

    }

    private void drawPanel6(IGraphicsTarget target, Rectangle rect) throws VizException {
        IFont myfont;
        myfont = defaultFont;
        defineCharHeight(myfont);
        myfont.setSmoothing(false);
        myfont.setScaleFont(false);
        extent = new PixelExtent(rect);
        target.setupClippingPlane(extent);
        String splitedStr[];
        String textStr;
        curY = rect.y;

        /*
         * Chin's NOTE::::this function is coded based on legacy nsharp software
         * show_mixheight() in xwvid3.c Calculates the mixing height using
         * legacy mix_height()
         */

        // yellow and bold for parcel header
        target.drawString(myfont, NsharpNativeConstants.OPC_MIXING_HGT_STR, rect.x + rect.width / 10, curY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_yellow, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;
        textStr = NsharpNativeConstants.OPC_DRY_AD_LINE;
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;
        textStr = NsharpNativeConstants.OPC_THRESH_LAPSE_LINE;
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        // Cyan color for Layer Based string
        target.drawString(myfont, NsharpNativeConstants.OPC_LAYER_BASED_STR, rect.x + rect.width / 3, curY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_cyan, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        // Layer-based lapse rate data
        MixHeight mixHgt = weatherDataStore.getMixHeightLayerBased();

        if (NsharpLibBasics.qc(mixHgt.getMh_hgt())) {
            textStr = NsharpNativeConstants.OPC_MIXINGHEIGHT_LINE;
            textStr = String.format(textStr, mixHgt.getMh_hgt());
        } else {
            textStr = NsharpNativeConstants.OPC_MIXINGHEIGHT_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        if (NsharpLibBasics.qc(mixHgt.getMh_pres())) {
            textStr = NsharpNativeConstants.OPC_MIXINGPRESSURE_LINE;
            textStr = String.format(textStr, mixHgt.getMh_pres());
        } else {
            textStr = NsharpNativeConstants.OPC_MIXINGPRESSURE_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        if (NsharpLibBasics.qc(mixHgt.getMh_drct()) && NsharpLibBasics.qc(mixHgt.getMh_sped())) {
            textStr = NsharpNativeConstants.OPC_TOPMIXLAYER_LINE;
            textStr = String.format(textStr, (int) mixHgt.getMh_drct(), NsharpConstants.DEGREE_SYMBOL,
                    (int) (mixHgt.getMh_sped()));
        } else {
            textStr = NsharpNativeConstants.OPC_TOPMIXLAYER_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        if (NsharpLibBasics.qc(mixHgt.getMh_drct_max()) && NsharpLibBasics.qc(mixHgt.getMh_sped_max())) {
            textStr = NsharpNativeConstants.OPC_MIXLAYERMAX_LINE;
            textStr = String.format(textStr, (int) mixHgt.getMh_drct_max(), NsharpConstants.DEGREE_SYMBOL,
                    (int) mixHgt.getMh_sped_max());
        } else {
            textStr = NsharpNativeConstants.OPC_MIXLAYERMAX_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        if (NsharpLibBasics.qc(mixHgt.getMh_dC()) && NsharpLibBasics.qc(mixHgt.getMh_lr())) {
            textStr = NsharpNativeConstants.OPC_LAYER_LAPSE_LINE;
            textStr = String.format(textStr, mixHgt.getMh_dC(), mixHgt.getMh_lr());
        } else {
            textStr = NsharpNativeConstants.OPC_LAYER_LAPSE_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        // Purple color for Layer Based string
        target.drawString(myfont, NsharpNativeConstants.OPC_SURFACE_BASED_STR, rect.x + rect.width / 3, curY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_violet, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        // Surface-based lapse rate data
        mixHgt = weatherDataStore.getMixHeightSurfaceBased();
        // white color for text
        if (NsharpLibBasics.qc(mixHgt.getMh_hgt())) {
            textStr = NsharpNativeConstants.OPC_MIXINGHEIGHT_LINE;
            textStr = String.format(textStr, mixHgt.getMh_hgt());
        } else {
            textStr = NsharpNativeConstants.OPC_MIXINGHEIGHT_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        if (NsharpLibBasics.qc(mixHgt.getMh_pres())) {
            textStr = NsharpNativeConstants.OPC_MIXINGPRESSURE_LINE;
            textStr = String.format(textStr, mixHgt.getMh_pres());
        } else {
            textStr = NsharpNativeConstants.OPC_MIXINGPRESSURE_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        if (NsharpLibBasics.qc(mixHgt.getMh_drct()) && NsharpLibBasics.qc(mixHgt.getMh_sped())) {
            textStr = NsharpNativeConstants.OPC_TOPMIXLAYER_LINE;
            textStr = String.format(textStr, (int) mixHgt.getMh_drct(), NsharpConstants.DEGREE_SYMBOL,
                    (int) mixHgt.getMh_sped());
        } else {
            textStr = NsharpNativeConstants.OPC_TOPMIXLAYER_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        if (NsharpLibBasics.qc(mixHgt.getMh_drct_max()) && NsharpLibBasics.qc(mixHgt.getMh_sped_max())) {
            textStr = NsharpNativeConstants.OPC_MIXLAYERMAX_LINE;
            textStr = String.format(textStr, (int) mixHgt.getMh_drct_max(), NsharpConstants.DEGREE_SYMBOL,
                    (int) mixHgt.getMh_sped_max());
        } else {
            textStr = NsharpNativeConstants.OPC_MIXLAYERMAX_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        if (NsharpLibBasics.qc(mixHgt.getMh_dC()) && NsharpLibBasics.qc(mixHgt.getMh_lr())) {
            textStr = NsharpNativeConstants.OPC_LAYER_LAPSE_LINE;
            textStr = String.format(textStr, mixHgt.getMh_dC(), mixHgt.getMh_lr());
        } else {
            textStr = NsharpNativeConstants.OPC_LAYER_LAPSE_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }

    }

    private void drawPanel7(IGraphicsTarget target, Rectangle rect) throws VizException {
        IFont myfont;
        myfont = defaultFont;
        defineCharHeight(myfont);
        myfont.setSmoothing(false);
        myfont.setScaleFont(false);
        extent = new PixelExtent(rect);
        target.setupClippingPlane(extent);
        String splitedStr[];
        String textStr;
        curY = rect.y;
        /*
         * Chin's NOTE::::this function is coded based on legacy nsharp software
         * show_srdata() in xwvid3.c. Hard coded numerical numbers are directly
         * copied from it.
         */
        float totHeli;

        target.drawString(myfont, NsharpNativeConstants.STORM_RELATIVE_STR, rect.x + rect.width / 3, curY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_cyan, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        float smDir = weatherDataStore.getSmdir();
        float smSpd = weatherDataStore.getSmspd();
        if (NsharpLibBasics.qc(smSpd) && NsharpLibBasics.qc(smDir)) {
            textStr = NsharpNativeConstants.STORM_MOTION_LINE;
            textStr = String.format(textStr, smDir, NsharpConstants.DEGREE_SYMBOL, smSpd,
                    NsharpLibBasics.kt_to_mps(smSpd));
        } else {
            textStr = NsharpNativeConstants.STORM_MOTION_MISSING;
        }
        target.drawString(myfont, textStr, rect.x + rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        // yellow and bold for parcel header
        target.drawString(myfont, NsharpNativeConstants.STORM_HELICITY_STR, rect.x + rect.width / 4, curY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_yellow, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;
        textStr = NsharpNativeConstants.STORM_LAYER_POS_STR;
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;
        target.drawLine(rect.x, curY, 0.0, rect.x + rect.width, curY, 0.0, NsharpConstants.color_white, 1);

        // get helicity for sfc-2 km
        Helicity helicity = weatherDataStore.getStormTypeToHelicityMap()
                .get(NsharpWeatherDataStore.STORM_MOTION_TYPE_STR[1]);
        totHeli = helicity.getTotalHelicity();
        if (NsharpLibBasics.qc(helicity.getPosHelicity()) && NsharpLibBasics.qc(helicity.getNegHelicity())) {
            textStr = NsharpNativeConstants.STORM_SFC2KM_LINE;
            textStr = String.format(textStr, helicity.getPosHelicity(), helicity.getNegHelicity(), totHeli,
                    NsharpConstants.SQUARE_SYMBOL, NsharpConstants.SQUARE_SYMBOL);

        } else {
            textStr = NsharpNativeConstants.STORM_SFC2KM_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;
        // get helicity for sfc-3 km
        helicity = weatherDataStore.getStormTypeToHelicityMap().get(NsharpWeatherDataStore.STORM_MOTION_TYPE_STR[2]);
        totHeli = helicity.getTotalHelicity();
        if (NsharpLibBasics.qc(helicity.getPosHelicity()) && NsharpLibBasics.qc(helicity.getNegHelicity())) {
            textStr = NsharpNativeConstants.STORM_SFC3KM_LINE;
            textStr = String.format(textStr, helicity.getPosHelicity(), helicity.getNegHelicity(), totHeli,
                    NsharpConstants.SQUARE_SYMBOL, NsharpConstants.SQUARE_SYMBOL);
        } else {
            textStr = NsharpNativeConstants.STORM_SFC3KM_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;
        // get sw helicity for LPL - LFC
        helicity = null;
        NsharpWeatherDataStore.ParcelMiscParams parcelMiscs = weatherDataStore.getParcelMiscParamsMap()
                .get(this.currentParcel);
        if (parcelMiscs != null) {
            helicity = parcelMiscs.getHelicityLplToLfc();
        }
        if (helicity != null && NsharpLibBasics.qc(helicity.getPosHelicity())
                && NsharpLibBasics.qc(helicity.getNegHelicity())) {
            textStr = NsharpNativeConstants.STORM_LPL_LFC_LINE;
            textStr = String.format(textStr, helicity.getPosHelicity(), helicity.getNegHelicity(),
                    helicity.getTotalHelicity(), NsharpConstants.SQUARE_SYMBOL, NsharpConstants.SQUARE_SYMBOL);
        } else {
            textStr = NsharpNativeConstants.STORM_LPL_LFC_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;
        // yellow and bold for header
        target.drawString(myfont, NsharpNativeConstants.STORM_WIND_STR, rect.x + rect.width / 4, curY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_yellow, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + 2 * charHeight;
        textStr = NsharpNativeConstants.STORM_LAYER_VECTOR_STR;
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;
        target.drawLine(rect.x, curY, 0.0, rect.x + rect.width, curY, 0.0, NsharpConstants.color_white, 1);

        // pressure-weighted SR mean wind at sfc-2 km
        WindComponent srMeanwindComp = weatherDataStore.getStormTypeToSrMeanWindMap().get("SFC-2km");

        if (NsharpLibBasics.qc(srMeanwindComp.getWdir())) {
            textStr = NsharpNativeConstants.STORM_SFC2KM_VECT_LINE;
            textStr = String.format(textStr, srMeanwindComp.getWdir(), srMeanwindComp.getWspd(),
                    NsharpLibBasics.kt_to_mps(srMeanwindComp.getWspd()));
        } else {
            textStr = NsharpNativeConstants.STORM_SFC2KM_VECT_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        // pressure-weighted SR mean wind at 4-6 km
        srMeanwindComp = weatherDataStore.getSrMeanWindComp4To6km();
        if (NsharpLibBasics.qc(srMeanwindComp.getWdir())) {
            textStr = NsharpNativeConstants.STORM_4_6KM_VECT_LINE;
            textStr = String.format(textStr, srMeanwindComp.getWdir(), srMeanwindComp.getWspd(),
                    NsharpLibBasics.kt_to_mps(srMeanwindComp.getWspd()));
        } else {
            textStr = NsharpNativeConstants.STORM_4_6KM_VECT_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        // pressure-weighted SR mean wind at 9-11 km
        srMeanwindComp = weatherDataStore.getSrMeanWindComp9To11km();
        if (NsharpLibBasics.qc(srMeanwindComp.getWdir())) {
            textStr = NsharpNativeConstants.STORM_9_11KM_VECT_LINE;
            textStr = String.format(textStr, srMeanwindComp.getWdir(), srMeanwindComp.getWspd(),
                    NsharpLibBasics.kt_to_mps(srMeanwindComp.getWspd()));
        } else {
            textStr = NsharpNativeConstants.STORM_9_11KM_VECT_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
    }

    private void drawPanel8(IGraphicsTarget target, Rectangle rect) throws VizException {
        IFont myfont;
        myfont = defaultFont;
        defineCharHeight(myfont);
        myfont.setSmoothing(false);
        myfont.setScaleFont(false);
        extent = new PixelExtent(rect);
        target.setupClippingPlane(extent);
        String splitedStr[];
        String textStr;
        curY = rect.y;
        /*
         * Chin's NOTE::::this function is coded based on legacy nsharp software
         * show_meanwind() in xwvid3.c
         */
        target.drawString(myfont, NsharpNativeConstants.MEAN_WIND_STR, rect.x + rect.width * 0.4, curY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_cyan, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        // mean wind at 0-6 km
        WindComponent windComp = weatherDataStore.getStormTypeToMeanWindMap().get("SFC-6km");
        if (NsharpLibBasics.qc(windComp.getWdir()) && NsharpLibBasics.qc(windComp.getWspd())) {
            textStr = NsharpNativeConstants.MEANWIND_SFC6KM_LINE;
            textStr = String.format(textStr, windComp.getWdir(), windComp.getWspd(),
                    NsharpConstants.KnotsToMetersPerSecond * windComp.getWspd());
        } else {
            textStr = NsharpNativeConstants.MEANWIND_SFC6KM_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;
        // mean wind at LFC-EL
        windComp = null;
        NsharpWeatherDataStore.ParcelMiscParams parcelMiscs = weatherDataStore.getParcelMiscParamsMap()
                .get(currentParcel);
        if (parcelMiscs != null) {
            windComp = parcelMiscs.getMeanWindCompLfcToEl();
        }
        if (windComp != null && NsharpLibBasics.qc(windComp.getWdir()) && NsharpLibBasics.qc(windComp.getWspd())) {
            textStr = NsharpNativeConstants.MEANWIND_LFC_EL_LINE;
            textStr = String.format(textStr, windComp.getWdir(), windComp.getWspd(),
                    NsharpConstants.KnotsToMetersPerSecond * windComp.getWspd());
        } else {
            textStr = NsharpNativeConstants.MEANWIND_LFC_EL_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;
        // mean wind at 850-200 mb
        windComp = weatherDataStore.getMeanWindComp850To200mb();
        if (NsharpLibBasics.qc(windComp.getWdir()) && NsharpLibBasics.qc(windComp.getWspd())) {
            textStr = NsharpNativeConstants.MEANWIND_850_200MB_LINE;
            textStr = String.format(textStr, windComp.getWdir(), windComp.getWspd(),
                    NsharpConstants.KnotsToMetersPerSecond * windComp.getWspd());
        } else {
            textStr = NsharpNativeConstants.MEANWIND_850_200MB_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        /*
         * Chin's NOTE::::this function is coded based on legacy nsharp software
         * show_shear() in xwvid3.c
         */
        target.drawString(myfont, NsharpNativeConstants.ENVIRONMENTAL_SHEAR_STR, rect.x + rect.width / 3, curY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_cyan, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        textStr = NsharpNativeConstants.SHEAR_LAYER_DELTA_STR;
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 3, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;
        target.drawLine(rect.x, curY, 0.0, rect.x + rect.width, curY, 0.0, NsharpConstants.color_white, 1);

        // get wind shear at Low - 3 km
        float windShear = weatherDataStore.getStormTypeToWindShearMap().get("SFC-3km");
        if (NsharpLibBasics.qc(windShear)) {
            textStr = NsharpNativeConstants.SHEAR_LOW_3KM_LINE;
            textStr = String.format(textStr, windShear, NsharpLibBasics.kt_to_mps(windShear),
                    NsharpLibBasics.kt_to_mps(windShear) / .3F);
        } else {
            textStr = NsharpNativeConstants.SHEAR_LOW_3KM_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 3, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        // get wind shear at Sfc - 2 km
        windShear = weatherDataStore.getStormTypeToWindShearMap().get("SFC-2km");
        if (NsharpLibBasics.qc(windShear)) {
            textStr = NsharpNativeConstants.SHEAR_SFC_2KM_LINE;
            textStr = String.format(textStr, windShear, NsharpLibBasics.kt_to_mps(windShear),
                    NsharpLibBasics.kt_to_mps(windShear) / .2F);
        } else {
            textStr = NsharpNativeConstants.SHEAR_SFC_2KM_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 3, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        // get wind shear at Sfc - 6 km
        windShear = weatherDataStore.getStormTypeToWindShearMap().get("SFC-6km");
        if (NsharpLibBasics.qc(windShear)) {
            textStr = NsharpNativeConstants.SHEAR_SFC_6KM_LINE;
            textStr = String.format(textStr, windShear, NsharpLibBasics.kt_to_mps(windShear),
                    NsharpLibBasics.kt_to_mps(windShear) / .6F);
        } else {
            textStr = NsharpNativeConstants.SHEAR_SFC_6KM_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 3, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        // Calculate wind shear at Sfc - 12 km
        windShear = weatherDataStore.getShearWindCompSfcTo12km().getWspd();
        if (NsharpLibBasics.qc(windShear)) {
            textStr = NsharpNativeConstants.SHEAR_SFC_12KM_LINE;
            textStr = String.format(textStr, windShear, NsharpLibBasics.kt_to_mps(windShear),
                    NsharpLibBasics.kt_to_mps(windShear) / 1.2F);
        } else {
            textStr = NsharpNativeConstants.SHEAR_SFC_12KM_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 3, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
    }

    private void drawPanel9(IGraphicsTarget target, Rectangle rect) throws VizException {
        IFont myfont;
        myfont = defaultFont;

        defineCharHeight(myfont);
        myfont.setSmoothing(false);
        myfont.setScaleFont(false);
        extent = new PixelExtent(rect);
        target.setupClippingPlane(extent);
        String splitedStr[];
        String textStr;
        curY = rect.y;
        /*
         * Chin's NOTE::::this function is coded based on legacy nsharp software
         * show_initiation(): Displays thunderstorm initiation parameters,
         * show_heavypcpn(), show_preciptype() and show_stormtype() in xwvid3.c
         */
        Parcel parcel = weatherDataStore.getParcelMap().get(currentParcel);
        // CONVECTIVE_INITIATION
        target.drawString(myfont, NsharpNativeConstants.CONVECTIVE_INITIATION_STR, rect.x + rect.width / 3, curY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_cyan, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;
        // CINH
        if (parcel != null && NsharpLibBasics.qc(parcel.getBminus())) {
            textStr = NsharpNativeConstants.CONVECTIVE_CINH_LINE;
            textStr = String.format(textStr, parcel.getBminus());
        } else {
            textStr = NsharpNativeConstants.CONVECTIVE_CINH_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }

        // cap
        if (parcel != null && NsharpLibBasics.qc(parcel.getCap()) && NsharpLibBasics.qc(parcel.getCappres())) {
            textStr = NsharpNativeConstants.CONVECTIVE_CAP_LINE;
            textStr = String.format(textStr, parcel.getCap(), parcel.getCappres());
        } else {
            textStr = NsharpNativeConstants.CONVECTIVE_CAP_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + rect.width / 2 + i * rect.width / 4, curY, 0.0,
                    TextStyle.NORMAL, NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP,
                    null);
        }
        curY = curY + charHeight;

        // K-index
        if (NsharpLibBasics.qc(weatherDataStore.getkIndex())) {
            textStr = NsharpNativeConstants.CONVECTIVE_KINDEX_LINE;
            textStr = String.format(textStr, weatherDataStore.getkIndex());
        } else {
            textStr = NsharpNativeConstants.CONVECTIVE_KINDEX_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }

        // Mean RH
        if (NsharpLibBasics.qc(weatherDataStore.getMeanRh())) {
            textStr = NsharpNativeConstants.CONVECTIVE_MEANRH_LINE;
            textStr = String.format(textStr, weatherDataStore.getMeanRh(), NsharpConstants.PERCENT_SYMBOL);
        } else {
            textStr = NsharpNativeConstants.CONVECTIVE_MEANRH_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + rect.width / 2 + i * rect.width / 4, curY, 0.0,
                    TextStyle.NORMAL, NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP,
                    null);
        }
        curY = curY + 2 * charHeight;

        // Top of M layer
        if (NsharpLibBasics.qc(weatherDataStore.getTopMoistLyrPress())
                && NsharpLibBasics.qc(weatherDataStore.getTopMoistLyrHeight())) {

            textStr = NsharpNativeConstants.CONVECTIVE_TOP_LINE;
            textStr = String.format(textStr, weatherDataStore.getTopMoistLyrPress(),
                    weatherDataStore.getTopMoistLyrHeight());
        } else {
            textStr = NsharpNativeConstants.CONVECTIVE_TOP_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;
        // LFC height
        if (parcel != null && NsharpLibBasics.qc(parcel.getLfcpres()) && NsharpLibBasics.qc(parcel.getLfcAgl())) {
            textStr = NsharpNativeConstants.CONVECTIVE_LFC_LINE;
            textStr = String.format(textStr, parcel.getLfcpres(), parcel.getLfcAgl());
        } else {
            textStr = NsharpNativeConstants.CONVECTIVE_LFC_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        // STORM TYPE
        target.drawString(myfont, NsharpNativeConstants.STORM_TYPE_STR, rect.x + rect.width / 3, curY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_cyan, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        // CAPE
        if (parcel != null && NsharpLibBasics.qc(parcel.getBplus())) {
            textStr = NsharpNativeConstants.STORM_TYPE_CAPE_LINE;
            textStr = String.format(textStr, parcel.getBplus());
        } else {
            textStr = NsharpNativeConstants.STORM_TYPE_CAPE_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }

        // EFF. SREH
        // Bigsharp uses default bottom/top pressure (i.e. sfc/3km pressure) as
        // input.
        // Therefore, it is the same as sfc-3km helicity in page2
        // However, to be consistent with legacy Nsharp, we compute it with
        // default value
        float hel = weatherDataStore.getStormTypeToHelicityMap().get(NsharpWeatherDataStore.STORM_MOTION_TYPE_STR[2])
                .getTotalHelicity();
        hel = weatherDataStore.getEffSreh();
        if (NsharpLibBasics.qc(hel)) {
            textStr = NsharpNativeConstants.STORM_TYPE_EFF_LINE;
            textStr = String.format(textStr, hel, NsharpConstants.SQUARE_SYMBOL, NsharpConstants.SQUARE_SYMBOL);
        } else {
            textStr = NsharpNativeConstants.STORM_TYPE_EFF_MISSING;
        }

        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + rect.width / 2 + i * rect.width / 4, curY, 0.0,
                    TextStyle.NORMAL, NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP,
                    null);
        }
        curY = curY + charHeight;

        // EHI : energy helicity
        // @ Bigsharp skparams.c ehi(), it is a simple computation to
        // get ehi as following.
        // ehi = (cape * hel) / 160000.0
        if (parcel != null && NsharpLibBasics.qc(parcel.getBplus()) && NsharpLibBasics.qc(hel)) {
            float ehi = (parcel.getBplus() * hel) / 160000.0f;
            textStr = NsharpNativeConstants.STORM_TYPE_EHI_LINE;
            textStr = String.format(textStr, ehi);
        } else {
            textStr = NsharpNativeConstants.STORM_TYPE_EHI_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }

        // 3km Shear
        // Bigsharp uses default bottom/top pressure (i.e. sfc/3km pressure) as
        // input params.
        // Therefore, it is the same as sfc-3km shear in page2
        float shear3km = weatherDataStore.getWindShear3km();
        if (NsharpLibBasics.qc(shear3km)) {
            textStr = NsharpNativeConstants.STORM_TYPE_3KM_LINE;
            textStr = String.format(textStr, shear3km);
        } else {
            textStr = NsharpNativeConstants.STORM_TYPE_3KM_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + rect.width / 2 + i * rect.width / 4, curY, 0.0,
                    TextStyle.NORMAL, NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP,
                    null);
        }
        curY = curY + charHeight;

        // BRN
        if (parcel != null && NsharpLibBasics.qc(parcel.getBrn())) {
            textStr = NsharpNativeConstants.STORM_TYPE_BRN_LINE;
            textStr = String.format(textStr, parcel.getBrn());
        } else {
            textStr = NsharpNativeConstants.STORM_TYPE_BRN_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }

        // BRN Shear
        if (parcel != null && NsharpLibBasics.qc(parcel.getBrnShear())) {
            textStr = NsharpNativeConstants.STORM_TYPE_BRNSHEAR_LINE;
            textStr = String.format(textStr, parcel.getBrnShear(), NsharpConstants.SQUARE_SYMBOL,
                    NsharpConstants.SQUARE_SYMBOL);
        } else {
            textStr = NsharpNativeConstants.STORM_TYPE_BRNSHEAR_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + rect.width / 2 + i * rect.width / 4, curY, 0.0,
                    TextStyle.NORMAL, NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP,
                    null);
        }
        curY = curY + charHeight;

        // PRECIPITATION_TYPE
        target.drawString(myfont, NsharpNativeConstants.PRECIPITATION_TYPE_STR, rect.x + rect.width / 3, curY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_cyan, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        // Melting Level
        if (NsharpLibBasics.qc(weatherDataStore.getWbzft()) && NsharpLibBasics.qc(weatherDataStore.getWbzp())) {
            textStr = NsharpNativeConstants.PRECIPITATION_MELTING_LINE;
            textStr = String.format(textStr, weatherDataStore.getWbzft(), weatherDataStore.getWbzp());
        } else {
            textStr = NsharpNativeConstants.PRECIPITATION_MELTING_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;
        // HEAVY_RAINFAL
        target.drawString(myfont, NsharpNativeConstants.HEAVY_RAINFALL_STR, rect.x + rect.width / 3, curY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_cyan, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        if (NsharpLibBasics.qc(weatherDataStore.getRogashRainRate())) {
            textStr = NsharpNativeConstants.HEAVY_ROGASH_LINE;
            textStr = String.format(textStr, weatherDataStore.getRogashRainRate());
        } else {
            textStr = NsharpNativeConstants.HEAVY_ROGASH_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);

    }

    private void drawPanel10(IGraphicsTarget target, Rectangle rect) throws VizException {
        IFont myfont = defaultFont;

        defineCharHeight(myfont);
        myfont.setSmoothing(false);
        myfont.setScaleFont(false);
        extent = new PixelExtent(rect);
        target.setupClippingPlane(extent);
        String splitedStr[];
        String textStr;
        curY = rect.y;
        /*
         * Chin's NOTE::::this function is coded based on legacy nsharp software
         * show_hailpot(), show_torpot() in xwvid3.c
         */

        target.drawString(myfont, NsharpNativeConstants.SEVERE_POTENTIAL_STR, rect.x + rect.width / 3, curY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_cyan, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        target.drawString(myfont, NsharpNativeConstants.SEVERE_HAIL_POTENTIAL_STR, rect.x + rect.width / 4, curY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_yellow, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;
        Parcel parcel = weatherDataStore.getParcelMap().get(currentParcel);
        // CAPE
        if (parcel != null && NsharpLibBasics.qc(parcel.getBplus())) {
            textStr = NsharpNativeConstants.SEVERE_CAPE_LINE;
            textStr = String.format(textStr, parcel.getBplus());
        } else {
            textStr = NsharpNativeConstants.SEVERE_CAPE_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }

        // WBZ level
        if (NsharpLibBasics.qc(weatherDataStore.getWbzft())) {
            textStr = NsharpNativeConstants.SEVERE_WBZ_LINE;
            textStr = String.format(textStr, weatherDataStore.getWbzft());
        } else {
            textStr = NsharpNativeConstants.SEVERE_WBZ_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + rect.width / 2 + i * rect.width / 4, curY, 0.0,
                    TextStyle.NORMAL, NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP,
                    null);
        }
        curY = curY + charHeight;

        // Mid Level RH
        if (NsharpLibBasics.qc(weatherDataStore.getMidLvlRH500To700())) {
            textStr = NsharpNativeConstants.SEVERE_MIDRH_LINE;
            textStr = String.format(textStr, weatherDataStore.getMidLvlRH500To700(), NsharpConstants.PERCENT_SYMBOL);
        } else {
            textStr = NsharpNativeConstants.SEVERE_MIDSRW_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }

        // FZG level
        if (NsharpLibBasics.qc(weatherDataStore.getFgzft())) {
            textStr = NsharpNativeConstants.SEVERE_FGZ_LINE;
            textStr = String.format(textStr, weatherDataStore.getFgzft());
        } else {
            textStr = NsharpNativeConstants.SEVERE_FGZ_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + rect.width / 2 + i * rect.width / 4, curY, 0.0,
                    TextStyle.NORMAL, NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP,
                    null);
        }
        curY = curY + charHeight;

        ParcelMiscParams parcelMiscParams = weatherDataStore.getParcelMiscParamsMap().get(currentParcel);
        // EL Storm
        if (parcelMiscParams != null && NsharpLibBasics.qc(parcelMiscParams.getSrWindCompEl().getWspd())) {
            textStr = NsharpNativeConstants.SEVERE_ELSTORM_LINE;
            textStr = String.format(textStr, parcelMiscParams.getSrWindCompEl().getWspd());
        } else {
            textStr = NsharpNativeConstants.SEVERE_ELSTORM_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;
        // CHI1
        if (parcelMiscParams != null && NsharpLibBasics.qc(parcelMiscParams.getChi1())) {
            textStr = NsharpNativeConstants.SEVERE_CHI1_LINE;
            textStr = String.format(textStr, parcelMiscParams.getChi1());
        } else {
            textStr = NsharpNativeConstants.SEVERE_CHI1_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
        // CHI2
        if (parcelMiscParams != null && NsharpLibBasics.qc(parcelMiscParams.getChi2())) {
            textStr = NsharpNativeConstants.SEVERE_CHI2_LINE;
            textStr = String.format(textStr, parcelMiscParams.getChi2());
        } else {
            textStr = NsharpNativeConstants.SEVERE_CHI2_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + rect.width / 2 + i * rect.width / 4, curY, 0.0,
                    TextStyle.NORMAL, NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP,
                    null);
        }
        curY = curY + charHeight;

        // Avg BL
        if (NsharpLibBasics.qc(weatherDataStore.getAvgWetbulbTemp())) {
            textStr = NsharpNativeConstants.SEVERE_AVGBL_LINE;
            textStr = String.format(textStr, weatherDataStore.getAvgWetbulbTemp(), NsharpConstants.DEGREE_SYMBOL);
        } else {
            textStr = NsharpNativeConstants.SEVERE_AVGBL_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        // TORNADO_POTENTIAL
        target.drawString(myfont, NsharpNativeConstants.SEVERE_TORNADO_POTENTIAL_STR, rect.x + rect.width / 4, curY,
                0.0, TextStyle.NORMAL, NsharpConstants.color_yellow, HorizontalAlignment.LEFT, VerticalAlignment.TOP,
                null);
        curY = curY + charHeight;

        // Low SRW Sfc
        if (parcelMiscParams != null && NsharpLibBasics.qc(parcelMiscParams.getSrMeanWindCompSfcToLfc().getWspd())) {
            textStr = NsharpNativeConstants.SEVERE_LOWSRWSFC_LINE;
            textStr = String.format(textStr, parcelMiscParams.getSrMeanWindCompSfcToLfc().getWspd());
        } else {
            textStr = NsharpNativeConstants.SEVERE_LOWSRWSFC_MISSING;
        }

        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        if (parcelMiscParams != null
                && NsharpLibBasics.qc(parcelMiscParams.getSrMeanWindCompLfcToLFCP4km().getWspd())) {
            textStr = NsharpNativeConstants.SEVERE_MIDSRW_LINE;
            textStr = String.format(textStr, parcelMiscParams.getSrMeanWindCompLfcToLFCP4km().getWspd());
        } else {
            textStr = NsharpNativeConstants.SEVERE_MIDSRW_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        if (parcelMiscParams != null && NsharpLibBasics.qc(parcelMiscParams.getSrMeanWindCompElM4kmToEl().getWspd())) {
            textStr = NsharpNativeConstants.SEVERE_UPPERSRWEL_LINE;
            textStr = String.format(textStr, parcelMiscParams.getSrMeanWindCompElM4kmToEl().getWspd());
        } else {
            textStr = NsharpNativeConstants.SEVERE_UPPERSRWEL_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        }
    }

    private void drawPanel11(IGraphicsTarget target, Rectangle rect) throws VizException {
        IFont myfont;

        myfont = defaultFont;

        defineCharHeight(myfont);
        myfont.setSmoothing(false);
        myfont.setScaleFont(false);
        extent = new PixelExtent(rect);
        target.setupClippingPlane(extent);

        String textStr;
        curY = rect.y;

        //
        // Start with Parcel Data
        //
        float layerPressure = 0;
        DrawableString str1 = new DrawableString("12345ft", NsharpConstants.color_red);
        str1.font = myfont;
        double hRatio = paintProps.getView().getExtent().getWidth() / paintProps.getCanvasBounds().width;
        double startX = rect.x + 0.5 * charWidth;
        double widthGap = rect.width / 4;
        double aglWidth = target.getStringsBounds(str1).getWidth() * hRatio * xRatio;
        str1.setText("D2D Lite Page", NsharpConstants.color_red);
        str1.setCoordinates(startX, curY);
        str1.horizontalAlignment = HorizontalAlignment.LEFT;
        str1.verticallAlignment = VerticalAlignment.TOP;
        DrawableString str2 = new DrawableString("", NsharpConstants.color_white);
        str2.horizontalAlignment = HorizontalAlignment.LEFT;
        str2.verticallAlignment = VerticalAlignment.TOP;
        str2.font = myfont;
        DrawableString str3 = new DrawableString("", NsharpConstants.color_white);
        str3.horizontalAlignment = HorizontalAlignment.RIGHT;
        str3.verticallAlignment = VerticalAlignment.TOP;
        str3.font = myfont;
        DrawableString str4 = new DrawableString("", NsharpConstants.color_white);
        str4.horizontalAlignment = HorizontalAlignment.LEFT;
        str4.verticallAlignment = VerticalAlignment.TOP;
        str4.font = myfont;
        DrawableString str5 = new DrawableString("", NsharpConstants.color_white);
        str5.horizontalAlignment = HorizontalAlignment.RIGHT;
        str5.verticallAlignment = VerticalAlignment.TOP;
        str5.font = myfont;
        DrawableString str6 = new DrawableString("", NsharpConstants.color_white);
        str6.horizontalAlignment = HorizontalAlignment.LEFT;
        str6.verticallAlignment = VerticalAlignment.TOP;
        str6.font = myfont;
        DrawableString str7 = new DrawableString("", NsharpConstants.color_white);
        str7.horizontalAlignment = HorizontalAlignment.RIGHT;
        str7.verticallAlignment = VerticalAlignment.TOP;
        str7.font = myfont;
        DrawableString str8 = new DrawableString("", NsharpConstants.color_white);
        str8.horizontalAlignment = HorizontalAlignment.LEFT;
        str8.verticallAlignment = VerticalAlignment.TOP;
        str8.font = myfont;
        DrawableString str9 = new DrawableString("", NsharpConstants.color_white);
        str9.horizontalAlignment = HorizontalAlignment.RIGHT;
        str9.verticallAlignment = VerticalAlignment.TOP;
        str9.font = myfont;
        DrawableString str10 = new DrawableString("", NsharpConstants.color_white);
        str10.horizontalAlignment = HorizontalAlignment.LEFT;
        str10.verticallAlignment = VerticalAlignment.TOP;
        str10.font = myfont;
        DrawableString str11 = new DrawableString("", NsharpConstants.color_white);
        str11.horizontalAlignment = HorizontalAlignment.RIGHT;
        str11.verticallAlignment = VerticalAlignment.TOP;
        str11.font = myfont;
        DrawableString str12 = new DrawableString("", NsharpConstants.color_white);
        str12.horizontalAlignment = HorizontalAlignment.LEFT;
        str12.verticallAlignment = VerticalAlignment.TOP;
        str12.font = myfont;
        DrawableString str13 = new DrawableString("", NsharpConstants.color_white);
        str13.horizontalAlignment = HorizontalAlignment.RIGHT;
        str13.verticallAlignment = VerticalAlignment.TOP;
        str13.font = myfont;

        target.drawStrings(str1);
        curY = curY + charHeight;
        target.drawLine(rect.x, curY, 0.0, rect.x + rect.width, curY, 0.0, NsharpConstants.color_white, 1);
        double firstToken = 0;
        double secondToken = 0;
        double thirdToken = 0;
        if (paneConfigurationName.equals(NsharpConstants.PANE_LITE_D2D_CFG_STR)
                || paneConfigurationName.equals(NsharpConstants.PANE_OPC_CFG_STR)) {
            firstToken = rect.x + widthGap + aglWidth;
            secondToken = rect.x + 2 * widthGap - charWidth;
            thirdToken = rect.x + 3 * widthGap + aglWidth;
            for (int parcelNumber = 1; parcelNumber <= NsharpLibSndglib.PARCEL_D2DLITE_MAX; parcelNumber++) {
                textStr = parcelToTypeStrMap.get(parcelNumber);
                str1.setText(textStr, NsharpConstants.color_gold);

                str1.setCoordinates(startX, curY);
                Parcel parcel = weatherDataStore.getParcelMap().get(parcelNumber);
                curY = curY + charHeight;
                // draw CAPE
                str2.setText("CAPE=", NsharpConstants.color_white);
                str2.setCoordinates(startX, curY);
                if (parcel != null && NsharpLibBasics.qc(parcel.getBplus())) {
                    str3.setText(String.format("%.0f", parcel.getBplus()), NsharpConstants.color_white);
                } else {
                    str3.setText("M", NsharpConstants.color_white);
                }
                str3.setCoordinates(firstToken, curY);

                // draw CINH
                str4.setText("CINH=", NsharpConstants.color_white);
                str4.setCoordinates(secondToken, curY);

                if (parcel != null && NsharpLibBasics.qc(parcel.getBminus())) {
                    str5.setText(String.format("%.0f", parcel.getBminus()), NsharpConstants.color_white);
                } else {
                    str5.setText("M", NsharpConstants.color_white);
                }
                str5.setCoordinates(thirdToken, curY);
                curY = curY + charHeight;
                // draw LCL
                str6.setText("LCL=", NsharpConstants.color_white);
                str6.setCoordinates(startX, curY);
                str8.setText("LCL(AGL)=", NsharpConstants.color_white);
                str8.setCoordinates(secondToken, curY);
                if (parcel != null && NsharpLibBasics.qc(parcel.getLclpres())
                        && NsharpLibBasics.qc(parcel.getLclAgl())) {
                    str7.setText(String.format("%5.0fmb", parcel.getLclpres()), NsharpConstants.color_white);
                    str9.setText(String.format("%7.0f'", parcel.getLclAgl()), NsharpConstants.color_white);
                } else {
                    str7.setText("M", NsharpConstants.color_white);
                    str9.setText("M", NsharpConstants.color_white);
                }
                str7.setCoordinates(firstToken, curY);
                str9.setCoordinates(thirdToken, curY);
                curY = curY + charHeight;
                // draw LFC
                str10.setText("LFC=", NsharpConstants.color_white);
                str10.setCoordinates(startX, curY);
                str12.setText("LFC(AGL)=", NsharpConstants.color_white);
                str12.setCoordinates(secondToken, curY);
                if (parcel != null && NsharpLibBasics.qc(parcel.getLfcpres())
                        && NsharpLibBasics.qc(parcel.getLfcAgl())) {
                    str11.setText(String.format("%5.0fmb", parcel.getLfcpres()), NsharpConstants.color_white);
                    str13.setText(String.format("%7.0f'", parcel.getLfcAgl()), NsharpConstants.color_white);
                } else {
                    str11.setText("M", NsharpConstants.color_white);
                    str13.setText("M", NsharpConstants.color_white);
                }
                str11.setCoordinates(firstToken, curY);
                str13.setCoordinates(thirdToken, curY);
                target.drawStrings(str1, str2, str3, str4, str5, str6, str7, str8, str9, str10, str11, str12, str13);
                curY = curY + charHeight;
            }
        } else if (paneConfigurationName.equals(NsharpConstants.PANE_SIMPLE_D2D_CFG_STR)
                || paneConfigurationName.equals(NsharpConstants.PANE_SPCWS_CFG_STR)) {
            widthGap = rect.width / 3;
            firstToken = rect.x + widthGap * 0.8;
            secondToken = rect.x + widthGap;
            thirdToken = secondToken + widthGap * 0.8;
            double forthToken = rect.x + 2 * widthGap;
            double fifthToken = forthToken + widthGap * 0.8;
            for (int parcelNumber = 1; parcelNumber <= NsharpLibSndglib.PARCEL_D2DLITE_MAX; parcelNumber++) {
                textStr = parcelToTypeStrMap.get(parcelNumber);
                str1.setText(textStr, NsharpConstants.color_gold);
                str1.setCoordinates(startX, curY);
                Parcel parcel = weatherDataStore.getParcelMap().get(parcelNumber);
                curY = curY + charHeight;
                // draw CAPE
                str2.setText("CAPE=", NsharpConstants.color_white);
                str2.setCoordinates(startX, curY);
                if (parcel != null && NsharpLibBasics.qc(parcel.getBplus())) {
                    str3.setText(String.format("%.0f", parcel.getBplus()), NsharpConstants.color_white);
                } else {
                    str3.setText("M", NsharpConstants.color_white);
                }
                str3.setCoordinates(firstToken, curY);

                // draw LCL
                str4.setText("LCL(mb)=", NsharpConstants.color_white);
                str4.setCoordinates(secondToken, curY);
                str6.setText("LCL(ft)=", NsharpConstants.color_white);
                str6.setCoordinates(forthToken, curY);
                if (parcel != null && NsharpLibBasics.qc(parcel.getLclpres())
                        && NsharpLibBasics.qc(parcel.getLclAgl())) {
                    str5.setText(String.format("%5.0f", parcel.getLclpres()), NsharpConstants.color_white);
                    str7.setText(String.format("%7.0f", parcel.getLclAgl()), NsharpConstants.color_white);
                } else {
                    str5.setText("M", NsharpConstants.color_white);
                    str7.setText("M", NsharpConstants.color_white);
                }
                str5.setCoordinates(thirdToken, curY);
                str7.setCoordinates(fifthToken, curY);
                curY = curY + charHeight;
                // draw CINH
                str8.setText("CINH=", NsharpConstants.color_white);
                str8.setCoordinates(startX, curY);
                if (parcel != null && NsharpLibBasics.qc(parcel.getBminus())) {
                    str9.setText(String.format("%.0f", parcel.getBminus()), NsharpConstants.color_white);
                } else {
                    str9.setText("M", NsharpConstants.color_white);
                }
                str9.setCoordinates(firstToken, curY);

                // draw LFC
                str10.setText("LFC(mb)=", NsharpConstants.color_white);
                str10.setCoordinates(secondToken, curY);
                str12.setText("LFC(ft)=", NsharpConstants.color_white);
                str12.setCoordinates(forthToken, curY);
                if (parcel != null && NsharpLibBasics.qc(parcel.getLfcpres())
                        && NsharpLibBasics.qc(parcel.getLfcAgl())) {
                    str11.setText(String.format("%5.0f", parcel.getLfcpres()), NsharpConstants.color_white);
                    str13.setText(String.format("%7.0f", parcel.getLfcAgl()), NsharpConstants.color_white);
                } else {
                    str11.setText("M", NsharpConstants.color_white);
                    str13.setText("M", NsharpConstants.color_white);
                }
                str11.setCoordinates(thirdToken, curY);
                str13.setCoordinates(fifthToken, curY);
                target.drawStrings(str1, str2, str3, str4, str5, str6, str7, str8, str9, str10, str11, str12, str13);
                curY = curY + charHeight;
            }
        }

        str2.horizontalAlignment = HorizontalAlignment.RIGHT;
        str3.horizontalAlignment = HorizontalAlignment.LEFT;
        str4.horizontalAlignment = HorizontalAlignment.RIGHT;
        str5.horizontalAlignment = HorizontalAlignment.LEFT;
        str6.horizontalAlignment = HorizontalAlignment.RIGHT;
        str7.horizontalAlignment = HorizontalAlignment.LEFT;
        str8.horizontalAlignment = HorizontalAlignment.RIGHT;
        str9.horizontalAlignment = HorizontalAlignment.LEFT;
        str10.horizontalAlignment = HorizontalAlignment.RIGHT;
        str11.horizontalAlignment = HorizontalAlignment.LEFT;
        str12.horizontalAlignment = HorizontalAlignment.RIGHT;
        target.drawLine(rect.x, curY, 0.0, rect.x + rect.width, curY, 0.0, NsharpConstants.color_white, 1);
        str1.setText("PW =", NsharpConstants.color_white);
        str1.setCoordinates(startX, curY);
        float pw = weatherDataStore.getPw();
        if (pw >= 0) {
            textStr = String.format("%.2fin", pw);
        } else {
            textStr = "M";
        }
        str2.setText(textStr, NsharpConstants.color_white);
        str2.setCoordinates(firstToken, curY);
        str3.setText("ConvT =", NsharpConstants.color_white);
        str3.setCoordinates(secondToken, curY);
        float conTempF = weatherDataStore.getConvT();

        if (NsharpLibBasics.qc(conTempF)) {
            textStr = String.format("%.0fF", conTempF);
        } else {
            textStr = "M";
        }
        str4.setText(textStr, NsharpConstants.color_white);
        str4.setCoordinates(thirdToken, curY);
        curY = curY + charHeight;
        str5.setText("WBZ =", NsharpConstants.color_white);
        str5.setCoordinates(startX, curY);
        float wbzft = weatherDataStore.getWbzft();
        if (NsharpLibBasics.qc(wbzft)) {
            textStr = String.format("%.0f'", wbzft);
        } else {
            textStr = "M";
        }
        str6.setText(textStr, NsharpConstants.color_white);
        str6.setCoordinates(firstToken, curY);
        str7.setText("FGZ =", NsharpConstants.color_white);
        str7.setCoordinates(secondToken, curY);
        if (NsharpLibBasics.qc(weatherDataStore.getFgzft())) {
            textStr = String.format("%.0f'", weatherDataStore.getFgzft());
        } else {
            textStr = "M";
        }
        str8.setText(textStr, NsharpConstants.color_white);
        str8.setCoordinates(thirdToken, curY);

        target.drawStrings(str1, str2, str3, str4, str5, str6, str7, str8);
        curY = curY + charHeight;
        str1.setText("BRN =", NsharpConstants.color_white);
        str1.setCoordinates(startX, curY);
        Parcel parcel = weatherDataStore.getParcelMap().get(this.currentParcel);
        if (parcel != null && NsharpLibBasics.qc(parcel.getBrn())) {
            textStr = NsharpNativeConstants.STORM_TYPE_BRN_LINE;
            textStr = String.format("%6.0f", parcel.getBrn());
        } else {
            textStr = "M";
        }
        str2.setText(textStr, NsharpConstants.color_white);
        str2.setCoordinates(firstToken, curY);
        str3.setText("BRN Shr=", NsharpConstants.color_white);
        str3.setCoordinates(secondToken, curY);
        float brnShear = parcel.getBrnShear();
        if (parcel != null && NsharpLibBasics.qc(brnShear)) {
            textStr = String.format("  %.0f m%c/s%c", brnShear, NsharpConstants.SQUARE_SYMBOL,
                    NsharpConstants.SQUARE_SYMBOL);
        } else {
            textStr = "M";
        }
        str4.setText(textStr, NsharpConstants.color_white);
        str4.setCoordinates(thirdToken, curY);
        curY = curY + charHeight;
        target.drawLine(rect.x, curY, 0.0, rect.x + rect.width, curY, 0.0, NsharpConstants.color_white, 1);
        str5.setText("Bunkers Right=", NsharpConstants.color_white);
        str5.setCoordinates(startX, curY);
        float dir = weatherDataStore.getBunkersStormMotionWindComp()[0].getWdir();
        float spd = weatherDataStore.getBunkersStormMotionWindComp()[0].getWspd();
        textStr = String.format("%.0f/%.0f kt", dir, spd);

        str6.setText(textStr, NsharpConstants.color_white);
        str6.setCoordinates(thirdToken, curY);
        curY = curY + charHeight;

        target.drawStrings(str1, str2, str3, str4, str5, str6);

        str1.setText("0-1km Helicity=", NsharpConstants.color_white);
        str1.setCoordinates(startX, curY);
        // get helicity for sfc- 1km
        Helicity helicity = weatherDataStore.getStormTypeToHelicityMap()
                .get(NsharpWeatherDataStore.STORM_MOTION_TYPE_STR[0]);
        float totHeli = helicity.getTotalHelicity();
        if (NsharpLibBasics.qc(helicity.getPosHelicity()) && NsharpLibBasics.qc(helicity.getPosHelicity())) {
            textStr = String.format("%.0f m%c/s%c", totHeli, NsharpConstants.SQUARE_SYMBOL,
                    NsharpConstants.SQUARE_SYMBOL);
        } else {
            textStr = "M";
        }
        str2.setText(textStr, NsharpConstants.color_white);
        str2.setCoordinates(thirdToken, curY);
        curY = curY + charHeight;
        str3.setText("0-3km Helicity=", NsharpConstants.color_white);
        str3.setCoordinates(startX, curY);
        // get helicity for sfc-3 km
        helicity = weatherDataStore.getStormTypeToHelicityMap().get(NsharpWeatherDataStore.STORM_MOTION_TYPE_STR[2]);
        totHeli = helicity.getTotalHelicity();

        if (NsharpLibBasics.qc(helicity.getPosHelicity()) && NsharpLibBasics.qc(helicity.getPosHelicity())) {
            textStr = String.format("%.0f m%c/s%c", totHeli, NsharpConstants.SQUARE_SYMBOL,
                    NsharpConstants.SQUARE_SYMBOL);
        } else {
            textStr = "M";
        }
        str4.setText(textStr, NsharpConstants.color_white);
        str4.setCoordinates(thirdToken, curY);
        target.drawStrings(str1, str2, str3, str4);
    }

    // future (dummy) page
    private void drawPanel12(IGraphicsTarget target, Rectangle rect) throws VizException {
        IFont myfont;
        myfont = defaultFont;
        defineCharHeight(myfont);
        myfont.setSmoothing(false);
        myfont.setScaleFont(false);
        extent = new PixelExtent(rect);
        target.setupClippingPlane(extent);
        target.drawString(myfont, "               FUTURE PAGE", rect.x + rect.width / 2, rect.y + rect.height / 2, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_cyan, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, null);
        return;
    }

    public boolean isSumP1Visible() {
        return sumP1Visible;
    }

    public NsharpGenericPaneBackground getDataPanel1Background() {
        return dataPanel1Background;
    }

    public NsharpGenericPaneBackground getDataPanel2Background() {
        return dataPanel2Background;
    }

    public void setPageDisplayOrderNumberArray(int[] pageDisplayOrderNumberArray, int numberPagePerDisplay) {
        this.pageDisplayOrderNumberArray = pageDisplayOrderNumberArray;
        if (paneConfigurationName.equals(NsharpConstants.PANE_DEF_CFG_1_STR)) {
            // This configuration always show 2 pages layout vertically
            this.numberPagePerDisplay = 2;
        } else if (paneConfigurationName.equals(NsharpConstants.PANE_LITE_D2D_CFG_STR)
                || paneConfigurationName.equals(NsharpConstants.PANE_OPC_CFG_STR)) {
            this.numberPagePerDisplay = 1;
        } else {
            this.numberPagePerDisplay = numberPagePerDisplay;
        }
        if (rscHandler != null) {
            int displayDataPageMax = NsharpConstants.PAGE_MAX_NUMBER / numberPagePerDisplay;
            rscHandler.setDisplayDataPageMax(displayDataPageMax);
        }
        if (initDone) {

            defaultFont = font8;

            handleResize();
        }

    }

    @Override
    protected void adjustFontSize(float canvasW, float canvasH) {
    }

    @Override
    public void handleResize() {
        super.handleResize();
        // Chin Note; ext size is its view size Not canvas size
        IExtent ext = getDescriptor().getRenderableDisplay().getExtent();
        ext.reset();
        this.rectangle = new Rectangle((int) ext.getMinX(), (int) ext.getMinY(), (int) ext.getWidth(),
                (int) ext.getHeight());
        pe = new PixelExtent(this.rectangle);
        getDescriptor().setNewPe(pe);
        defineCharHeight(defaultFont);
        float prevHeight = dataPaneHeight;
        float prevWidth = dataPaneWidth;
        int dp1XOrig = (int) (ext.getMinX());
        int dp1YOrig = (int) (ext.getMinY());
        int dp2XOrig =  NsharpConstants.DATAPANEL2_X_ORIG;
        int dp2YOrig =  NsharpConstants.DATAPANEL2_Y_ORIG;
        if (paneConfigurationName.equals(NsharpConstants.PANE_OPC_CFG_STR)
                || paneConfigurationName.equals(NsharpConstants.PANE_SPCWS_CFG_STR)
                || paneConfigurationName.equals(NsharpConstants.PANE_SIMPLE_D2D_CFG_STR)
                || paneConfigurationName.equals(NsharpConstants.PANE_LITE_D2D_CFG_STR)
                || paneConfigurationName.equals(NsharpConstants.PANE_DEF_CFG_2_STR)) {
            if (numberPagePerDisplay == 2) {
                // these 2 configurations lay 2 data panels side by side
                dataPaneWidth = (int) (ext.getWidth() / 2);
                dataPaneHeight = (int) ext.getHeight();
                dp2XOrig = dp1XOrig + dataPaneWidth;
                dp2YOrig = dp1YOrig;
            } else {
                dataPaneWidth = (int) ext.getWidth();
                dataPaneHeight = (int) ext.getHeight();
                dp2XOrig = dp1XOrig;
                dp2YOrig = dp1YOrig;
            }
        } else if (paneConfigurationName.equals(NsharpConstants.PANE_DEF_CFG_1_STR)) {
            // this configuration lays 2 data panels top/down
            // always display 2 pages
            dataPaneWidth = (int) ext.getWidth();
            dataPaneHeight = (int) ext.getHeight() / 2;
            dp2XOrig = dp1XOrig;
            dp2YOrig = dp1YOrig + dataPaneHeight;
        }

        xRatio = xRatio * dataPaneWidth / prevWidth;
        // turn off
        xRatio = 1;
        yRatio = yRatio * dataPaneHeight / prevHeight;
        // turn off
        yRatio = 1;
        charHeight = (int) (charHeight * yRatio);

        Rectangle rectangle = new Rectangle(dp1XOrig, dp1YOrig, dataPaneWidth, dataPaneHeight);
        dataPanel1Background.handleResize(rectangle);
        rectangle = new Rectangle(dp2XOrig, dp2YOrig, dataPaneWidth, dataPaneHeight);
        dataPanel2Background.handleResize(rectangle);
        panelRectArray[0] = dataPanel1Background.getRectangle();
        panelRectArray[1] = dataPanel2Background.getRectangle();

    }

    @Override
    public void handleZooming() {
        magnifyFont(currentZoomLevel);
    }
}
