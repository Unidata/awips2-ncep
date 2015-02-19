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
 * 04/23/2014               Chin Chen   Add d2d lite page  
 * 08/11/2014               Chin Chen   fix typo
 * 01/27/2015   DR#17006,
 *              Task#5929   Chin Chen   NSHARP freezes when loading a sounding from MDCRS products 
 *                                      in Volume Browser
 *
 * </pre>
 * 
 * @author Chin Chen
 * @version 1.0
 */

import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;
import gov.noaa.nws.ncep.ui.nsharp.NsharpConstants;
import gov.noaa.nws.ncep.ui.nsharp.background.NsharpGenericPaneBackground;
import gov.noaa.nws.ncep.ui.nsharp.display.NsharpAbstractPaneDescriptor;
import gov.noaa.nws.ncep.ui.nsharp.natives.NsharpNative.NsharpLibrary._lplvalues;
import gov.noaa.nws.ncep.ui.nsharp.natives.NsharpNative.NsharpLibrary._parcel;
import gov.noaa.nws.ncep.ui.nsharp.natives.NsharpNativeConstants;
import gov.noaa.nws.ncep.ui.nsharp.view.NsharpParcelDialog;

import java.awt.geom.Rectangle2D;
import java.util.List;

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
import com.sun.jna.ptr.FloatByReference;
import com.vividsolutions.jts.geom.Coordinate;

public class NsharpDataPaneResource extends NsharpAbstractPaneResource {
    private int currentTextChapter = 1;

    private int[] pageDisplayOrderNumberArray; // index is the real page defined
                                               // in NsharpConstants to be
                                               // shown, value is the order
                                               // number of this page. index 0
                                               // point to a dummy.

    private static final String NO_DATA = "NO VALID DATA AVAILABLE FOR THIS PAGE";
    private static final String INSUFFICIENT_DATA = "INSUFFICIENT DATA FOR PARAMETERS COMPUTATION";

    // private double charHeight = NsharpConstants.CHAR_HEIGHT_;
    private double curY;

    private double parcelLineYStart, parcelLineYEnd;

    private double firstToken, secondToken, thirdToken, forthToken, fifthToken,
            sixthToken;

    private FloatByReference fValue = new FloatByReference(0);

    private FloatByReference fValue1 = new FloatByReference(0);

    private FloatByReference fValue2 = new FloatByReference(0);

    private FloatByReference fValue3 = new FloatByReference(0);

    private FloatByReference fValue4 = new FloatByReference(0);

    private FloatByReference fValue5 = new FloatByReference(0);

    private FloatByReference fValue6 = new FloatByReference(0);

    private FloatByReference fValue7 = new FloatByReference(0);

    private FloatByReference fValue8 = new FloatByReference(0);

    private PixelExtent extent;

    // physical number of panel in editor display
    // total software defined panels to be displayed
    private Rectangle[] panelRectArray = new Rectangle[NsharpConstants.dsiplayPanelSize];

    private int numberPagePerDisplay = 1;

    private NsharpGenericPaneBackground dataPanel1Background;

    private NsharpGenericPaneBackground dataPanel2Background;

    private int dataPaneWidth = NsharpConstants.DATA_PANE_REC_WIDTH;

    private int dataPaneHeight = NsharpConstants.DATA_PANE_REC_HEIGHT;

    // private int dataWidth = NsharpConstants.DATAPANEL1_WIDTH;
    // private int dataHeight = NsharpConstants.DATAPANEL1_HEIGHT;
    private int dp1XOrig = NsharpConstants.DATAPANEL1_X_ORIG;

    private int dp1YOrig = NsharpConstants.DATAPANEL1_Y_ORIG;

    private int dp2XOrig = NsharpConstants.DATAPANEL2_X_ORIG;

    private int dp2YOrig = NsharpConstants.DATAPANEL2_Y_ORIG;

    private float xRatio = 1;

    private float yRatio = 1;

    private IFont defaultFont = font10;

    private boolean initDone = false;

    private boolean resizedone = false; // d2dlite

    private short currentParcel = NsharpNativeConstants.PARCELTYPE_MOST_UNSTABLE;;

    // private int parcelLinesInPhysicalPanelNumber;
    private boolean sumP1Visible = false;

    public NsharpDataPaneResource(AbstractResourceData resourceData,
            LoadProperties loadProperties, NsharpAbstractPaneDescriptor desc) {
        super(resourceData, loadProperties, desc);
        dataPanel1Background = new NsharpGenericPaneBackground(new Rectangle(
                NsharpConstants.DATAPANEL1_X_ORIG,
                NsharpConstants.DATAPANEL1_Y_ORIG,
                NsharpConstants.DATAPANEL1_WIDTH,
                NsharpConstants.DATAPANEL1_HEIGHT));
        dataPanel2Background = new NsharpGenericPaneBackground(new Rectangle(
                NsharpConstants.DATAPANEL2_X_ORIG,
                NsharpConstants.DATAPANEL2_Y_ORIG,
                NsharpConstants.DATAPANEL2_WIDTH,
                NsharpConstants.DATAPANEL2_HEIGHT));
    }

    @Override
    protected void paintInternal(IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {
        super.paintInternal(target, paintProps);
        dataPanel1Background.paint(target, paintProps);
        if (numberPagePerDisplay == 2) {
            dataPanel2Background.paint(target, paintProps);
        }
        if (rscHandler == null)
            return;

        if (!resizedone) {
            resizedone = true;
            handleResize();
        }

        if ((soundingLys != null) && (rscHandler.isGoodData())) {//#5929
            this.defaultFont.setSmoothing(false);
            this.defaultFont.setScaleFont(false);
            // write to panels
            // Chin: Note:
            // Current display algorithm is: One chapter = 2 pages. show 2 pages
            // at one time.
            // i.e. show current page and its next page with 2 physical panels.
            // currently, we have total of 11 "pages" to display on 2 "physical
            // display panels per design.
            sumP1Visible = false;
            currentTextChapter = rscHandler.getCurrentTextChapter();
            if (numberPagePerDisplay == 1)
                drawPanel(target, currentTextChapter, 1);
            else if (numberPagePerDisplay == 2) {
                for (int i = currentTextChapter * numberPagePerDisplay - 1, physicalPanelNum = 1; i <= currentTextChapter
                        * numberPagePerDisplay; i++, physicalPanelNum++) {
                    int pageNum = i % NsharpConstants.PAGE_MAX_NUMBER; // d2dlite
                    if (pageNum == 0)
                        pageNum = NsharpConstants.PAGE_MAX_NUMBER; // d2dlite

                    drawPanel(target, pageNum, physicalPanelNum);
                    // System.out.println("current chapter="+currentTextChapter+" page num="+pageNum+
                    // " disPanel="+physicalPanelNum);
                }
            }
        }
        else { //#5929
        	drawInsuffDataMessage(target, panelRectArray[0]);
        }
    }

    @Override
    protected void initInternal(IGraphicsTarget target) throws VizException {
        super.initInternal(target);
        if (paneConfigurationName.equals(NsharpConstants.PANE_DEF_CFG_2_STR)
                || paneConfigurationName
                        .equals(NsharpConstants.PANE_SPCWS_CFG_STR)
                || paneConfigurationName
                        .equals(NsharpConstants.PANE_SIMPLE_D2D_CFG_STR)) {
            myDefaultCanvasWidth = (int) (NsharpConstants.DISPLAY_WIDTH
                    * (1 - NsharpConstants.PANE_DEF_CFG_2_LEFT_GP_WIDTH_RATIO) * NsharpConstants.PANE_DEF_CFG_2_DATA_WIDTH_RATIO);
            myDefaultCanvasHeight = (int) (NsharpConstants.DISPLAY_HEIGHT * NsharpConstants.PANE_DEF_CFG_2_DATA_HEIGHT_RATIO);
        } else if (paneConfigurationName
                .equals(NsharpConstants.PANE_DEF_CFG_1_STR)) {
            myDefaultCanvasWidth = (int) (NsharpConstants.DISPLAY_WIDTH
                    * (1 - NsharpConstants.PANE_DEF_CFG_1_LEFT_GP_WIDTH_RATIO) * NsharpConstants.PANE_DEF_CFG_1_DATA_WIDTH_RATIO);
            myDefaultCanvasHeight = (int) (NsharpConstants.DISPLAY_HEIGHT * NsharpConstants.PANE_DEF_CFG_1_DATA_HEIGHT_RATIO);
        }
        // System.out.println("data pane default canv W="+myDefaultCanvasWidth+" h="+myDefaultCanvasHeight);
        panelRectArray[0] = dataPanel1Background.getRectangle();
        panelRectArray[1] = dataPanel2Background.getRectangle();
        dataPanel1Background.initInternal(target);
        dataPanel2Background.initInternal(target);
        if (numberPagePerDisplay == 1)
            defaultFont = font12;
        else
            defaultFont = font10;
        handleResize();
        initDone = true;
        // System.out.println(": font char height =" + charHeight);
    }

    public void setCurrentParcel(short currentParcel) {
        this.currentParcel = currentParcel;
    }

    public short getCurrentParcel() {
        return currentParcel;
    }

    public void resetCurrentParcel() {
        currentParcel = NsharpNativeConstants.PARCELTYPE_MOST_UNSTABLE;
    }

    @Override
    public void resetData(List<NcSoundingLayer> soundingLys,
            List<NcSoundingLayer> prevsoundingLys) {

        super.resetData(soundingLys, prevsoundingLys);
        currentParcel = NsharpNativeConstants.PARCELTYPE_MOST_UNSTABLE;
    }
    
    @SuppressWarnings("deprecation") //#5929
	private void drawInsuffDataMessage(IGraphicsTarget target, Rectangle rect)
            throws VizException {
    	IFont myfont;
    	if (paneConfigurationName.equals(NsharpConstants.PANE_LITE_D2D_CFG_STR)) 
            myfont = font9;
         else 
        	 myfont = font20;
    	
    	defineCharHeight(myfont);
    	myfont.setSmoothing(false);
    	myfont.setScaleFont(false);
    	sumP1Visible = true;
    	extent = new PixelExtent(rect);
    	target.setupClippingPlane(extent);
    	target.drawString(myfont,  INSUFFICIENT_DATA, rect.x,
    			rect.y, 0.0, TextStyle.NORMAL, NsharpConstants.color_cyan,
    			HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
    	return;
        
    }

    private void drawPanel(IGraphicsTarget target, int pageOrderNumber,
            int dsiplayPanelNumber) throws VizException {
        if (pageOrderNumber > NsharpConstants.PAGE_MAX_NUMBER
                || dsiplayPanelNumber > numberPagePerDisplay)// NsharpConstants.dsiplayPanelSize)
            return;
        int physicalPanelNumber = dsiplayPanelNumber - 1;
        int displayPageNumber = 0;
        // find a page with its order number equal to pageOrderNumber
        for (int i = 1; i <= NsharpConstants.PAGE_MAX_NUMBER; i++) {
            if (pageDisplayOrderNumberArray[i] == pageOrderNumber)
                displayPageNumber = i; // array index is the page number and
                                       // value is the order number
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
        case NsharpConstants.PAGE_D2DLITE: // d2dlite
            drawPanel11(target, panelRectArray[physicalPanelNumber]);
            break;
        case NsharpConstants.PAGE_FUTURE: // d2dlite
            drawPanel12(target, panelRectArray[physicalPanelNumber]);
            break;
        default:
            break;
        }
    }

    public void setUserPickedParcelLine(Coordinate c) {
        // make sure is in virtualPanel 1 as Parcel Line is defined in it.
        if (rscHandler != null) {
            if (!sumP1Visible)
                return;
        } else
            return;
        // System.out.println("setUserPickedParcelLine c.y="+ c.y+
        // " parcelLineYStart="+
        // parcelLineYStart+" parcelLineYEnd="+parcelLineYEnd);
        // make sure within parcel line area
        if (c.y >= parcelLineYStart && c.y <= parcelLineYEnd) {
            int index = ((int) (c.y - parcelLineYStart)) / (int) charHeight;
            if (index < NsharpNativeConstants.PARCEL_MAX) {
                currentParcel = (short) (index + 1);
                // System.out.println("setUserPickedParcelLine at "+
                // currentParcel);
                // notify skewtRsc
                rscHandler.updateParcelFromPanel(currentParcel);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void drawPanel1(IGraphicsTarget target, Rectangle rect)
            throws VizException {
        IFont myfont;
        if (paneConfigurationName.equals(NsharpConstants.PANE_LITE_D2D_CFG_STR)) {
            myfont = font9;
        } else {
            myfont = defaultFont;
        }
        defineCharHeight(myfont);
        myfont.setSmoothing(false);
        myfont.setScaleFont(false);
        sumP1Visible = true;
        extent = new PixelExtent(rect);
        target.setupClippingPlane(extent);

        /*
         * Chin's NOTE:::: This pages based on newer version nsharp from SPC. We
         * dont have source code as of 7/8/2010. Therefore, coding is purly
         * based on a captured screen shot given by SPC's John Hart. This
         * function is coded based on native nsharp codes which can be found in
         * other pages's show functions.
         */
        // if we can not Interpolates a temp with 700 mb pressure, then we dont
        // have enough raw data
        if ((nsharpNative.nsharpLib.qc(nsharpNative.nsharpLib.itemp(700.0F)) == 0)) {
            target.drawString(myfont,  NO_DATA, rect.x,
                    rect.y, 0.0, TextStyle.NORMAL, NsharpConstants.color_cyan,
                    HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
            return;
        }

        // call get_topBotPres to set p_top and p_bot
        FloatByReference topPF = new FloatByReference(0);
        FloatByReference botPF = new FloatByReference(0);
        nsharpNative.nsharpLib.get_effectLayertopBotPres(topPF, botPF);

        String textStr, CAPE3Str = "", NCAPEStr = "";
        curY = rect.y;

        //
        // Start with Parcel Data
        //
        Rectangle2D strBD = target.getStringBounds(myfont,
                NsharpNativeConstants.PAGE1TEXT1_FCST_STR + "XX");
        double hRatio = paintProps.getView().getExtent().getWidth()
                / paintProps.getCanvasBounds().width;
        double startX = rect.x + 0.5 * charWidth;
        double widthGap = (rect.width - strBD.getWidth() * hRatio * xRatio) / 6;// was
                                                                                // -100*xRatio)/6;
        firstToken = rect.x + strBD.getWidth() * hRatio * xRatio;// was
                                                                 // +100.0*xRatio;
        secondToken = firstToken + widthGap;
        thirdToken = secondToken + widthGap;
        forthToken = thirdToken + widthGap;
        fifthToken = forthToken + widthGap;
        sixthToken = fifthToken + widthGap;
        target.drawString(myfont, "Sum1", startX, rect.y, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        target.drawString(myfont, "CAPE", firstToken, rect.y, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        target.drawString(myfont, "CINH", secondToken, rect.y, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        target.drawString(myfont, "LCL", thirdToken, rect.y, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        target.drawString(myfont, "LI", forthToken, rect.y, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        target.drawString(myfont, "LFC", fifthToken, rect.y, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        target.drawString(myfont, "EL", sixthToken, rect.y, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;
        target.drawLine(rect.x, curY, 0.0, rect.x + rect.width, curY, 0.0,
                NsharpConstants.color_white, 1);
        parcelLineYStart = curY;
        float layerPressure = 0;

        // get user selected parcel type
        _lplvalues lpvls;
        _parcel pcl;
        // curY = curY+ (double)charHeight * 0.5;
        for (short parcelNumber = 1; parcelNumber <= NsharpNativeConstants.PARCEL_MAX; parcelNumber++) {
            if (parcelNumber == currentParcel) {
                PixelExtent pixExt = new PixelExtent(rect.x, rect.x
                        + rect.width, curY, curY + charHeight);
                // target.setupClippingPlane(pixExt);
                target.drawRect(pixExt, NsharpConstants.color_gold, 1.0f, 1.0f);
            }
            // call native define_parcel() with parcel type and user defined
            // pressure (if user defined it)
            textStr = NsharpNativeConstants.parcelToTypeStrMap
                    .get(parcelNumber);
            target.drawString(myfont, textStr, startX, curY, 0.0,
                    TextStyle.NORMAL, NsharpConstants.color_white,
                    HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
            float layerPressure1 = NsharpNativeConstants.parcelToLayerMap
                    .get(parcelNumber);
            if (parcelNumber == NsharpNativeConstants.PARCELTYPE_USER_DEFINED) {
                // get user selected parcel type, if available
                layerPressure1 = NsharpParcelDialog.getUserDefdParcelMb();
            }
            // System.out.println("drawPanel1-1 called define_parcel pType="+parcelNumber+" pre="+
            // layerPressure1);

            nsharpNative.nsharpLib.define_parcel(parcelNumber, layerPressure1);

            lpvls = new _lplvalues();
            nsharpNative.nsharpLib.get_lpvaluesData(lpvls);

            float sfctemp, sfcdwpt, sfcpres;
            sfctemp = lpvls.temp;
            sfcdwpt = lpvls.dwpt;
            sfcpres = lpvls.pres;
            // get parcel data by calling native nsharp parcel() API. value is
            // returned in pcl
            pcl = new _parcel();
            nsharpNative.nsharpLib.parcel(-1.0F, -1.0F, sfcpres, sfctemp,
                    sfcdwpt, pcl);
            // draw parcel name
            // draw CAPE
            if (pcl.bplus != NsharpNativeConstants.NSHARP_LEGACY_LIB_INVALID_DATA)
                target.drawString(myfont, String.format("%.0f", pcl.bplus),
                        firstToken, curY, 0.0, TextStyle.NORMAL,
                        NsharpConstants.color_white, HorizontalAlignment.LEFT,
                        VerticalAlignment.TOP, null);
            else
                target.drawString(myfont, "M", firstToken, curY, 0.0,
                        TextStyle.NORMAL, NsharpConstants.color_white,
                        HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
            // draw CINH
            if (pcl.bminus != NsharpNativeConstants.NSHARP_LEGACY_LIB_INVALID_DATA)
                target.drawString(myfont, String.format("%.0f", pcl.bminus),
                        secondToken, curY, 0.0, TextStyle.NORMAL,
                        NsharpConstants.color_white, HorizontalAlignment.LEFT,
                        VerticalAlignment.TOP, null);
            else
                target.drawString(myfont, "M", secondToken, curY, 0.0,
                        TextStyle.NORMAL, NsharpConstants.color_white,
                        HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
            // draw LCL
            float lcl = nsharpNative.nsharpLib.agl(nsharpNative.nsharpLib
                    .ihght(pcl.lclpres));
            if (lcl != NsharpNativeConstants.NSHARP_LEGACY_LIB_INVALID_DATA)
                target.drawString(myfont, String.format("%.0fm", lcl),
                        thirdToken, curY, 0.0, TextStyle.NORMAL,
                        NsharpConstants.color_white, HorizontalAlignment.LEFT,
                        VerticalAlignment.TOP, null);

            else
                target.drawString(myfont, "M", thirdToken, curY, 0.0,
                        TextStyle.NORMAL, NsharpConstants.color_white,
                        HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
            // draw LI
            if (pcl.li5 != NsharpNativeConstants.NSHARP_LEGACY_LIB_INVALID_DATA)
                target.drawString(myfont, String.format("%5.0f", pcl.li5),
                        forthToken, curY, 0.0, TextStyle.NORMAL,
                        NsharpConstants.color_white, HorizontalAlignment.LEFT,
                        VerticalAlignment.TOP, null);
            else
                target.drawString(myfont, "M", forthToken, curY, 0.0,
                        TextStyle.NORMAL, NsharpConstants.color_white,
                        HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
            // draw LFC
            float lfc = nsharpNative.nsharpLib.agl(nsharpNative.nsharpLib
                    .ihght(pcl.lfcpres));
            if (lfc != NsharpNativeConstants.NSHARP_LEGACY_LIB_INVALID_DATA)
                target.drawString(myfont, String.format("%.0fm", lfc),
                        fifthToken, curY, 0.0, TextStyle.NORMAL,
                        NsharpConstants.color_white, HorizontalAlignment.LEFT,
                        VerticalAlignment.TOP, null);
            else
                target.drawString(myfont, "M", fifthToken, curY, 0.0,
                        TextStyle.NORMAL, NsharpConstants.color_white,
                        HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
            // draw EL
            float el = nsharpNative.nsharpLib.agl(nsharpNative.nsharpLib
                    .ihght(pcl.elpres));
            if (el != NsharpNativeConstants.NSHARP_LEGACY_LIB_INVALID_DATA)
                target.drawString(
                        myfont,
                        String.format("%.0f'",
                                NsharpConstants.metersToFeet.convert(el)),
                        sixthToken, curY, 0.0, TextStyle.NORMAL,
                        NsharpConstants.color_white, HorizontalAlignment.LEFT,
                        VerticalAlignment.TOP, null);
            else
                target.drawString(myfont, "M", sixthToken, curY, 0.0,
                        TextStyle.NORMAL, NsharpConstants.color_white,
                        HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);

            curY = curY + charHeight;
            // get 3CAPE value for later to use
            if (parcelNumber == NsharpNativeConstants.PARCELTYPE_MEAN_MIXING) {
                if (nsharpNative.nsharpLib.qc(pcl.cape3km) == 1) {
                    CAPE3Str = String.format("%.0fJ/kg", pcl.cape3km);
                } else {
                    CAPE3Str = " M";
                }
            }// get NCAPE value for later to use
            else if (parcelNumber == NsharpNativeConstants.PARCELTYPE_MOST_UNSTABLE) {
                float j1 = pcl.bplus;
                float j2 = nsharpNative.nsharpLib.ihght(pcl.elpres)
                        - nsharpNative.nsharpLib.ihght(pcl.lfcpres);
                if (nsharpNative.nsharpLib.qc(j1 / j2) == 1) {
                    NCAPEStr = String.format("%.2f", j1 / j2);
                } else {
                    NCAPEStr = "NCAPE= M";
                }
            }
        }
        target.drawLine(rect.x, curY, 0.0, rect.x + rect.width, curY, 0.0,
                NsharpConstants.color_white, 1);
        parcelLineYEnd = curY;
        if (currentParcel == NsharpNativeConstants.PARCELTYPE_USER_DEFINED) {
            layerPressure = NsharpParcelDialog.getUserDefdParcelMb();
        } else
            layerPressure = NsharpNativeConstants.parcelToLayerMap
                    .get(currentParcel);
        // System.out.println("drawPanel1-2 called define_parcel pType="+currentParcel+" pre="+
        // layerPressure);

        // reset and define current parcel
        nsharpNative.nsharpLib.define_parcel(currentParcel, layerPressure);
        lpvls = new _lplvalues();
        nsharpNative.nsharpLib.get_lpvaluesData(lpvls);

        float sfctemp, sfcdwpt, sfcpres;
        sfctemp = lpvls.temp;
        sfcdwpt = lpvls.dwpt;
        sfcpres = lpvls.pres;
        // get parcel data by calling native nsharp parcel() API. value is
        // returned in pcl
        pcl = new _parcel();
        nsharpNative.nsharpLib.parcel(-1.0F, -1.0F, sfcpres, sfctemp, sfcdwpt,
                pcl);
        //
        // THERMO DYNAMIC DATA
        //
        firstToken = rect.x + rect.width / 48 * 12 * xRatio;
        secondToken = rect.x + rect.width / 48 * 27 * xRatio;
        thirdToken = rect.x + rect.width / 48 * 38 * xRatio;
        // curY = curY+ (double)charHeight * 0.5;
        DrawableString str = new DrawableString("ABCDE=",
                NsharpConstants.color_white);
        str.font = myfont;
        double equalSignPos = (startX + target.getStringsBounds(str).getWidth())
                * hRatio * xRatio;
        fValue.setValue(0);
        nsharpNative.nsharpLib.precip_water(fValue, -1.0F, -1.0F);
        if (nsharpNative.nsharpLib.qc(fValue.getValue()) == 1) {
            textStr = String.format("%.2f in", fValue.getValue());
        } else {
            textStr = " M";
        }
        str.setCoordinates(startX, curY);
        str.horizontalAlignment = HorizontalAlignment.LEFT;
        str.verticallAlignment = VerticalAlignment.TOP;
        str.font = myfont;
        str.setText("PW=", NsharpConstants.color_white);
        DrawableString str1 = new DrawableString(textStr,
                NsharpConstants.color_white);
        str1.setCoordinates(equalSignPos, curY);
        str1.horizontalAlignment = HorizontalAlignment.LEFT;
        str1.verticallAlignment = VerticalAlignment.TOP;
        str1.font = myfont;
        target.drawStrings(str, str1);

        // 3CAPE...value was retrieved earlier
        str.setText("3CAPE=", NsharpConstants.color_white);
        str.setCoordinates(firstToken, curY);
        str1.setText(CAPE3Str, NsharpConstants.color_white);
        str1.setCoordinates(firstToken + equalSignPos, curY);
        target.drawStrings(str, str1);

        fValue.setValue(0);
        float wbzft = nsharpNative.nsharpLib.mtof(nsharpNative.nsharpLib
                .agl(nsharpNative.nsharpLib.ihght(nsharpNative.nsharpLib
                        .wb_lvl(0, fValue))));
        if (nsharpNative.nsharpLib.qc(wbzft) == 1) {
            textStr = String.format("%.0f'", wbzft);
        } else {
            textStr = " M";
        }
        str.setText("WBZ=", NsharpConstants.color_white);
        str.setCoordinates(secondToken, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(secondToken + equalSignPos, curY);
        target.drawStrings(str, str1);

        // WNDG
        float wndg = nsharpNative.nsharpLib.damaging_wind();
        if (nsharpNative.nsharpLib.qc(wndg) == 1) {
            textStr = String.format("%.2f", wndg);
        } else {
            textStr = " M";
        }
        str.setText("WNDG=", NsharpConstants.color_white);
        str.setCoordinates(thirdToken, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(thirdToken + equalSignPos, curY);
        target.drawStrings(str, str1);

        curY = curY + charHeight; // move to new line

        fValue.setValue(0);
        nsharpNative.nsharpLib.k_index(fValue);
        if (nsharpNative.nsharpLib.qc(fValue.getValue()) == 1) {
            textStr = String.format("%.0f", fValue.getValue());
        } else {
            textStr = " M";
        }
        str.setText("K=", NsharpConstants.color_white);
        str.setCoordinates(startX, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(equalSignPos, curY);
        target.drawStrings(str, str1);
        // DCAPE
        // fValue1 will be used for DownT to use
        float dcape = nsharpNative.nsharpLib.dcape(fValue, fValue1);
        float downT = fValue1.getValue();
        if (nsharpNative.nsharpLib.qc(dcape) == 1) {
            textStr = String.format("%.0fJ/kg", dcape);
        } else {
            textStr = " M";
        }
        str.setText("DCAPE=", NsharpConstants.color_white);
        str.setCoordinates(firstToken, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(firstToken + equalSignPos, curY);
        target.drawStrings(str, str1);

        // FZL
        fValue.setValue(0);
        float fgzft = nsharpNative.nsharpLib.mtof(nsharpNative.nsharpLib
                .agl(nsharpNative.nsharpLib.ihght(nsharpNative.nsharpLib
                        .temp_lvl(0, fValue))));
        if (nsharpNative.nsharpLib.qc(fgzft) == 1) {
            textStr = String.format("%.0f'", fgzft);
        } else {
            textStr = " M";
        }
        str.setText("FZL=", NsharpConstants.color_white);
        str.setCoordinates(secondToken, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(secondToken + equalSignPos, curY);
        target.drawStrings(str, str1);
        // ESP
        float esp = nsharpNative.nsharpLib.esp();
        if (nsharpNative.nsharpLib.qc(esp) == 1) {
            textStr = String.format("%.2f", esp);
        } else {
            textStr = " M";
        }
        str.setText("ESP=", NsharpConstants.color_white);
        str.setCoordinates(thirdToken, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(thirdToken + equalSignPos, curY);
        target.drawStrings(str, str1);

        curY = curY + charHeight; // move to new line

        fValue.setValue(0);
        // MidRH
        nsharpNative.nsharpLib.get_surface(fValue1, fValue2, fValue3); // fValue
                                                                       // 2 and
                                                                       // fValue3
                                                                       // are
                                                                       // not of
                                                                       // concern
                                                                       // here
        nsharpNative.nsharpLib.mean_relhum(fValue, fValue1.getValue() - 150,
                fValue1.getValue() - 350);
        if (nsharpNative.nsharpLib.qc(fValue.getValue()) == 1) {
            textStr = String.format("%.0f%c", fValue.getValue(),
                    NsharpConstants.PERCENT_SYMBOL);
        } else {
            textStr = " M";
        }
        str.setText("MidRH=", NsharpConstants.color_white);
        str.setCoordinates(startX, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(equalSignPos, curY);
        target.drawStrings(str, str1);
        // DownT
        downT = nsharpNative.nsharpLib.ctof(downT); // convert to F
        if (nsharpNative.nsharpLib.qc(downT) == 1) {
            textStr = String.format("%.0fF", downT);
        } else {
            textStr = " M";
        }
        str.setText("DownT=", NsharpConstants.color_white);
        str.setCoordinates(firstToken, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(firstToken + equalSignPos, curY);
        target.drawStrings(str, str1);
        // ConvT
        fValue.setValue(0);
        float conTempF = nsharpNative.nsharpLib.ctof(nsharpNative.nsharpLib
                .cnvtv_temp(fValue, -1));

        if (nsharpNative.nsharpLib.qc(conTempF) == 1) {
            textStr = String.format("%.0fF", conTempF);
        } else {
            textStr = " M";
        }
        str.setText("ConvT=", NsharpConstants.color_white);
        str.setCoordinates(secondToken, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(secondToken + equalSignPos, curY);
        target.drawStrings(str, str1);

        // MMP: Coniglio MCS Maintenance Parameter
        float mmp = nsharpNative.nsharpLib.coniglio1();
        if (nsharpNative.nsharpLib.qc(mmp) == 1) {
            textStr = String.format("%.2f", mmp);
        } else {
            textStr = " M";
        }
        str.setText("MMP=", NsharpConstants.color_white);
        str.setCoordinates(thirdToken, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(thirdToken + equalSignPos, curY);
        target.drawStrings(str, str1);

        curY = curY + charHeight; // move to new line

        fValue.setValue(0);
        fValue1.setValue(0);
        // get surface pressure (fValue1) before getting mean LRH value
        nsharpNative.nsharpLib.get_surface(fValue1, fValue2, fValue3); // fValue
                                                                       // 2 and
                                                                       // fValue3
                                                                       // are
                                                                       // not of
                                                                       // concern
                                                                       // here
        nsharpNative.nsharpLib.mean_relhum(fValue, -1.0F,
                fValue1.getValue() - 150);
        if (nsharpNative.nsharpLib.qc(fValue.getValue()) == 1) {
            // textStr = NsharpNativeConstants.THERMO_MEANLRH_LINE;
            textStr = String.format("%.0f%c", fValue.getValue(),
                    NsharpConstants.PERCENT_SYMBOL);
        } else {
            textStr = " M";
        }
        str.setText("LowRH=", NsharpConstants.color_white);
        str.setCoordinates(startX, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(equalSignPos, curY);
        target.drawStrings(str, str1);

        fValue.setValue(0);
        nsharpNative.nsharpLib.mean_mixratio(fValue, -1.0F, -1.0F);
        if (nsharpNative.nsharpLib.qc(fValue.getValue()) == 1) {
            textStr = String.format("%.1fg/kg", fValue.getValue());
        } else {
            textStr = " M";
        }
        str.setText("MeanW=", NsharpConstants.color_white);
        str.setCoordinates(firstToken, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(firstToken + equalSignPos, curY);
        target.drawStrings(str, str1);

        fValue.setValue(0);
        float maxT = nsharpNative.nsharpLib.ctof(nsharpNative.nsharpLib
                .max_temp(fValue, -1));
        if (nsharpNative.nsharpLib.qc(maxT) == 1) {
            textStr = String.format("%.0fF", maxT);
        } else {
            textStr = " M";
        }
        str.setText("MaxT=", NsharpConstants.color_white);
        str.setCoordinates(secondToken, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(secondToken + equalSignPos, curY);
        target.drawStrings(str, str1);

        // NCAPE
        str.setText("NCAPE=", NsharpConstants.color_white);
        str.setCoordinates(thirdToken, curY);
        str1.setText(NCAPEStr, NsharpConstants.color_white);
        str1.setCoordinates(thirdToken + equalSignPos, curY);
        target.drawStrings(str, str1);

        curY = curY + charHeight; // move to new line
        target.drawLine(rect.x, curY, 0.0, rect.x + rect.width, curY, 0.0,
                NsharpConstants.color_white, 1);

        // draw a vertical line from 2/3 of x axis
        str.setText("sfc-3km AglLapseRate=xxC/x.xC/kmX",
                NsharpConstants.color_black);
        str.font = myfont;
        double lineXPos = target.getStringsBounds(str).getWidth() * hRatio
                * xRatio;
        if (lineXPos < rect.width * 2 / 3)
            lineXPos = rect.width * 2 / 3;
        firstToken = rect.x + lineXPos;
        target.drawLine(firstToken, curY, 0.0, firstToken,
                rect.y + rect.height, 0.0, NsharpConstants.color_white, 1);
        // curY = curY+ (double)charHeight * 0.5;
        firstToken = firstToken + 0.5 * charWidth;

        // more thermodynamic data
        // the following follow show_parcel_new() at xwvid.c of bigNsharp
        // implementation
        // sfc-3km Lapse rate
        //
        float htsfc = 0;
        float tDelta = nsharpNative.nsharpLib.aglT(0, 3000);
        fValue.setValue(0);
        // get surface pressure (fValue)
        nsharpNative.nsharpLib.get_surface(fValue, fValue1, fValue2);
        if (nsharpNative.nsharpLib.qc(fValue.getValue()) == 1) {
            htsfc = nsharpNative.nsharpLib.ihght(fValue.getValue());
            // get sfc to (sfc+ 3 km) pressure (fValue)
            float threekmPre = nsharpNative.nsharpLib.ipres(htsfc + 3000);
            fValue1.setValue(0);
            nsharpNative.nsharpLib.lapse_rate(fValue1, fValue.getValue(),
                    threekmPre);
            if (nsharpNative.nsharpLib.qc(fValue1.getValue()) == 1) {
                // textStr =
                // String.format("sfc-3km AglLapseRate=%.0fC/%.1fC/km", tDelta,
                // fValue1.getValue());
                textStr = String.format("%.0fC/%.1fC/km", tDelta,
                        fValue1.getValue());
            } else {
                textStr = " M";// "sfc-3km AglLapseRate=  M";
            }
        } else {
            textStr = " M";// "sfc-3km AglLapseRate=  M";
        }
        str.setText("sfc-3km Agl LapseRate= ", NsharpConstants.color_white);
        str.setCoordinates(startX, curY);
        equalSignPos = rect.x + target.getStringsBounds(str).getWidth()
                * hRatio * xRatio;
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(equalSignPos, curY);
        target.drawStrings(str, str1);

        /*
         * target.drawString(myfont, textStr, startX, curY , 0.0,
         * TextStyle.NORMAL, NsharpConstants.color_white,
         * HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
         */

        // "Supercell"
        float smdir = rscHandler.getSmWindDir();// bkRsc.getSmDir(); #10438
        float smspd = rscHandler.getSmWindSpd();// bkRsc.getSmSpd();
        float superCell = nsharpNative.nsharpLib.scp(smdir, smspd);
        if (nsharpNative.nsharpLib.qc(superCell) == 1) {
            textStr = String.format("%.1f", superCell);
        } else {
            textStr = " M";
        }
        str.setText("Supercell=  ", NsharpConstants.color_yellow);
        double equalSignPos1 = firstToken
                + target.getStringsBounds(str).getWidth() * hRatio * xRatio;
        str.setCoordinates(firstToken, curY);
        str1.setText(textStr, NsharpConstants.color_yellow);
        str1.setCoordinates(equalSignPos1, curY);
        target.drawStrings(str, str1);
        /*
         * target.drawString(myfont, textStr, firstToken, curY , 0.0,
         * TextStyle.NORMAL, NsharpConstants.color_yellow,
         * HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
         */

        curY = curY + charHeight; // move to new line

        // 3km-6km Lapse rate
        fValue.setValue(0);
        fValue1.setValue(0);
        tDelta = nsharpNative.nsharpLib.aglT(3000, 6000);
        // get 3 and 6km pressure (fValue)
        float threekmPre = nsharpNative.nsharpLib.ipres(htsfc + 3000);
        float sixkmPre = nsharpNative.nsharpLib.ipres(htsfc + 6000);
        fValue1.setValue(0);
        nsharpNative.nsharpLib.lapse_rate(fValue1, threekmPre, sixkmPre);
        if (nsharpNative.nsharpLib.qc(fValue1.getValue()) == 1) {
            textStr = String.format("%.0fC/%.1fC/km", tDelta,
                    fValue1.getValue());

        } else {
            textStr = " M";
        }
        str.setText("3-6km Agl LapseRate=", NsharpConstants.color_white);
        str.setCoordinates(startX, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(equalSignPos, curY);
        target.drawStrings(str, str1);
        /*
         * target.drawString(myfont, textStr, startX, curY , 0.0,
         * TextStyle.NORMAL, NsharpConstants.color_white,
         * HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
         */
        // "STP (CIN)"
        float cin = nsharpNative.nsharpLib.sigtorn_cin(smdir, smspd);
        if (nsharpNative.nsharpLib.qc(cin) == 1) {
            textStr = String.format("%.1f", cin);
        } else {
            textStr = " M";
        }
        str.setText("STP(CIN)=", NsharpConstants.color_white);
        str.setCoordinates(firstToken, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(equalSignPos1, curY);
        target.drawStrings(str, str1);
        /*
         * target.drawString(myfont, textStr, firstToken, curY , 0.0,
         * TextStyle.NORMAL, NsharpConstants.color_white,
         * HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
         */

        curY = curY + charHeight; // move to new line

        fValue.setValue(0);
        fValue1.setValue(0);
        // nsharpNative.nsharpLib.vert_tot(fValue);
        float delta = nsharpNative.nsharpLib.itemp(850)
                - nsharpNative.nsharpLib.itemp(500);
        nsharpNative.nsharpLib.lapse_rate(fValue1, 850.0F, 500.0F);
        if (nsharpNative.nsharpLib.qc(delta) == 1
                && nsharpNative.nsharpLib.qc(fValue1.getValue()) == 1) {
            textStr = String
                    .format("%.0fC/%.1fC/km", delta, fValue1.getValue());
        } else {
            textStr = " M";
        }
        str.setText("850-500mb LapseRate=", NsharpConstants.color_white);
        str.setCoordinates(startX, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(equalSignPos, curY);
        target.drawStrings(str, str1);
        /*
         * target.drawString(myfont, textStr, startX, curY , 0.0,
         * TextStyle.NORMAL, NsharpConstants.color_white,
         * HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
         */
        // "STP(fixed)"
        float fixedStp = nsharpNative.nsharpLib.sigtorn_fixed(smdir, smspd);
        if (nsharpNative.nsharpLib.qc(fixedStp) == 1) {
            textStr = String.format("%.1f", fixedStp);
        } else {
            textStr = " M";
        }
        str.setText("STP(fixed)=", NsharpConstants.color_white);
        str.setCoordinates(firstToken, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(equalSignPos1, curY);
        target.drawStrings(str, str1);
        /*
         * target.drawString(myfont, textStr, firstToken, curY , 0.0,
         * TextStyle.NORMAL, NsharpConstants.color_white,
         * HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
         */

        curY = curY + charHeight; // move to new line

        fValue.setValue(0);
        fValue1.setValue(0);
        // nsharpNative.nsharpLib.delta_t(fValue);
        nsharpNative.nsharpLib.lapse_rate(fValue1, 700.0F, 500.0F);
        delta = nsharpNative.nsharpLib.itemp(700)
                - nsharpNative.nsharpLib.itemp(500);
        if (nsharpNative.nsharpLib.qc(/* fValue.getValue()) */delta) == 1
                && nsharpNative.nsharpLib.qc(fValue1.getValue()) == 1) {
            textStr = String.format("%.0fC/%.1fC/km",
                    delta/* fValue.getValue() */, fValue1.getValue());
        } else {
            textStr = " M";
        }
        str.setText("700-500mb LapseRate=", NsharpConstants.color_white);
        str.setCoordinates(startX, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(equalSignPos, curY);
        target.drawStrings(str, str1);
        /*
         * target.drawString(myfont, textStr, startX, curY , 0.0,
         * TextStyle.NORMAL, NsharpConstants.color_white,
         * HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
         */
        // "SHIP"
        float ship = nsharpNative.nsharpLib.cave_ship();
        if (nsharpNative.nsharpLib.qc(ship) == 1) {
            textStr = String.format("%.1f", ship);
        } else
            textStr = " M";
        str.setText("SHIP=", NsharpConstants.color_red);
        str.setCoordinates(firstToken, curY);
        str1.setText(textStr, NsharpConstants.color_red);
        str1.setCoordinates(equalSignPos1, curY);
        target.drawStrings(str, str1);
        /*
         * target.drawString(myfont, textStr, firstToken, curY , 0.0,
         * TextStyle.NORMAL, NsharpConstants.color_red,
         * HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
         */
        // System.out.println("shop="+ship);
        // myfont.dispose();
    }

    @SuppressWarnings("deprecation")
    private void drawPanel2(IGraphicsTarget target, Rectangle rect)
            throws VizException {
        IFont myfont;
        if (paneConfigurationName.equals(NsharpConstants.PANE_LITE_D2D_CFG_STR)) {
            myfont = font10;
        } else {
            myfont = defaultFont;
        }
        /*
         * Chin's NOTE:::: This pages based on BigNsharp show_shear_new() at
         * xwvid3.c
         */

        defineCharHeight(myfont);
        myfont.setSmoothing(false);
        myfont.setScaleFont(false);
        extent = new PixelExtent(rect);
        target.setupClippingPlane(extent);
        // myfont = target.initializeFont(fontName, 10, null);
        curY = rect.y;
        String textStr;

        // if we can not Interpolates a temp with 700 mb pressure, then we dont
        // have enough raw data
        if (nsharpNative.nsharpLib.qc(nsharpNative.nsharpLib.itemp(700.0F)) == 0) {
            target.drawString(myfont,NO_DATA, rect.x,
                    rect.y, 0.0, TextStyle.NORMAL, NsharpConstants.color_cyan,
                    HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
            return;
        }

        //
        // Start with Header SRH(m%c/s%c) Shear(kt) MnWind SRW
        //
        double startX = rect.x + 0.5 * charWidth;
        double widthGap = (rect.width) / 5;
        firstToken = rect.x + widthGap;
        secondToken = firstToken + widthGap;
        thirdToken = secondToken + widthGap;
        forthToken = thirdToken + widthGap;
        target.drawString(myfont, "Sum2", startX, rect.y, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);

        textStr = String.format("SRH(m%c/s%c)", NsharpConstants.SQUARE_SYMBOL,
                NsharpConstants.SQUARE_SYMBOL);
        target.drawString(myfont, textStr, firstToken, curY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        target.drawString(myfont, "Shear(kt)", secondToken, curY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        target.drawString(myfont, "MnWind", thirdToken, curY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        target.drawString(myfont, "SRW", forthToken, curY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight; // move to new line
        target.drawLine(rect.x, curY, 0.0, rect.x + rect.width, curY, 0.0,
                NsharpConstants.color_white, 1);
        // curY = curY+ (double)charHeight * 0.5;
        FloatByReference smdir = new FloatByReference(0), smspd = new FloatByReference(
                0);
        nsharpNative.nsharpLib.get_storm(smspd, smdir);
        FloatByReference topPF = new FloatByReference(0);
        FloatByReference botPF = new FloatByReference(0);
        nsharpNative.nsharpLib.get_effectLayertopBotPres(topPF, botPF);
        // System.out.println("top="+topPF.getValue()+" bot="+botPF.getValue());
        for (int i = 0; i < NsharpNativeConstants.STORM_MOTION_TYPE_STR1.length; i++) {
            float h1, h2;
            if (NsharpNativeConstants.STORM_MOTION_TYPE_STR1[i]
                    .equals("Eff Inflow Layer")) {

                if (botPF.getValue() > 0) {
                    h1 = nsharpNative.nsharpLib.agl(nsharpNative.nsharpLib
                            .ihght(botPF.getValue()));
                    h2 = nsharpNative.nsharpLib.agl(nsharpNative.nsharpLib
                            .ihght(topPF.getValue()));
                } else {
                    h1 = -999;
                    h2 = -999;
                }
            } else {
                h1 = NsharpNativeConstants.STORM_MOTION_HEIGHT1[i][0];
                h2 = NsharpNativeConstants.STORM_MOTION_HEIGHT1[i][1];
            }
            // h1 = NsharpNativeConstants.STORM_MOTION_HEIGHT1[i][0];
            // h2 = NsharpNativeConstants.STORM_MOTION_HEIGHT1[i][1];
            if (h1 != -999 && h2 != -999) {
                // SRH
                // calculate helicity

                float totHeli = nsharpNative.nsharpLib.helicity(h1, h2,
                        smdir.getValue(), smspd.getValue(), fValue, fValue1);
                target.drawString(myfont,
                        NsharpNativeConstants.STORM_MOTION_TYPE_STR1[i],
                        startX, curY, 0.0, TextStyle.NORMAL,
                        NsharpConstants.color_white, HorizontalAlignment.LEFT,
                        VerticalAlignment.TOP, null);

                // gc.drawText(NsharpNativeConstants.STORM_MOTION_TYPE_STR[i],0,
                // textLineNumber*textHeight + graphLineNumber);
                if (nsharpNative.nsharpLib.qc(fValue.getValue()) == 1
                        && nsharpNative.nsharpLib.qc(fValue1.getValue()) == 1) {
                    // textStr =
                    // NsharpNativeConstants.STORM_MOTION_TYPE_STR[i]+"          %.0f";
                    textStr = String.format("%.0f", totHeli);
                } else {
                    textStr = "M";
                }
                target.drawString(myfont, textStr, firstToken, curY, 0.0,
                        TextStyle.NORMAL, NsharpConstants.color_white,
                        HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);

                // Calculate wind shear
                // Note: -1 as first parameter indicating bottom layer is
                // surface layer, see wind.c for wind_shear() source code
                nsharpNative.nsharpLib.wind_shear(nsharpNative.nsharpLib
                        .ipres(nsharpNative.nsharpLib.msl(h1)),
                        nsharpNative.nsharpLib.ipres(nsharpNative.nsharpLib
                                .msl(h2)), fValue, fValue1, fValue2, fValue3);
                if (nsharpNative.nsharpLib.qc(fValue3.getValue()) == 1) {
                    textStr = String.format("%.0f", fValue3.getValue());

                } else {
                    textStr = "M";
                }
                target.drawString(myfont, textStr, secondToken, curY, 0.0,
                        TextStyle.NORMAL, NsharpConstants.color_white,
                        HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);

                // Calculate mean wind
                nsharpNative.nsharpLib.mean_wind(nsharpNative.nsharpLib
                        .ipres(nsharpNative.nsharpLib.msl(h1)),
                        nsharpNative.nsharpLib.ipres(nsharpNative.nsharpLib
                                .msl(h2)), fValue, fValue1, fValue2, fValue3);
                if (nsharpNative.nsharpLib.qc(fValue2.getValue()) == 1
                        && nsharpNative.nsharpLib.qc(fValue3.getValue()) == 1) {
                    textStr = String.format("%.0f/%.0f", fValue2.getValue(),
                            fValue3.getValue());
                } else {
                    textStr = "M";
                }
                target.drawString(myfont, textStr, thirdToken, curY, 0.0,
                        TextStyle.NORMAL, NsharpConstants.color_white,
                        HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);

                // calculate pressure-weighted SR mean wind
                nsharpNative.nsharpLib.sr_wind(nsharpNative.nsharpLib
                        .ipres(nsharpNative.nsharpLib.msl(h1)),
                        nsharpNative.nsharpLib.ipres(nsharpNative.nsharpLib
                                .msl(h2)), smdir.getValue(), smspd.getValue(),
                        fValue, fValue1, fValue2, fValue3);
                if (nsharpNative.nsharpLib.qc(fValue2.getValue()) == 1) {
                    textStr = String.format("%.0f/%.0f", fValue2.getValue(),
                            fValue3.getValue());
                } else {
                    textStr = "M";
                }
                target.drawString(myfont, textStr, forthToken, curY, 0.0,
                        TextStyle.NORMAL, NsharpConstants.color_white,
                        HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);

                if (NsharpNativeConstants.STORM_MOTION_TYPE_STR1[i]
                        .equals("Eff Inflow Layer")) {
                    // draw bax around it
                    Rectangle rectangle = new Rectangle(rect.x, (int) curY,
                            rect.width, (int) charHeight);
                    // System.out.println("rect.x="+ rectangle.x+ " y="+
                    // rectangle.y+" w="+rectangle.width+
                    // " h="+rectangle.height);
                    PixelExtent pixExt = new PixelExtent(rectangle);
                    target.drawRect(pixExt, NsharpConstants.color_gold, 1.0f,
                            1.0f);
                }
            }
            curY = curY + charHeight; // move to new line

        }
        float pres = 0;
        short oldlplchoice = 3;
        float sfctemp, sfcdwpt, sfcpres;
        _lplvalues lpvls;
        _parcel pcl;
        lpvls = new _lplvalues();
        // nsharpNative.nsharpLib.get_lpvaluesData(lpvls);
        // sfctemp = lpvls.temp;
        // sfcdwpt = lpvls.dwpt;
        // sfcpres = lpvls.pres;
        // get parcel data by calling native nsharp parcel() API. value is
        // returned in pcl
        pcl = new _parcel();
        for (int i = 0; i < NsharpNativeConstants.STORM_MOTION_TYPE_STR2.length; i++) {
            float h1, h2;

            h1 = -999;
            h2 = -999;
            nsharpNative.nsharpLib.get_lpvaluesData(lpvls);
            if (NsharpNativeConstants.STORM_MOTION_TYPE_STR2[i]
                    .equals("LCL-EL(Cloud Layer)")) {
                sfctemp = lpvls.temp;
                sfcdwpt = lpvls.dwpt;
                sfcpres = lpvls.pres;
                nsharpNative.nsharpLib.parcel(-1.0F, -1.0F, sfcpres, sfctemp,
                        sfcdwpt, pcl);
                if (pcl.bplus > 0) {
                    h1 = nsharpNative.nsharpLib.agl(nsharpNative.nsharpLib
                            .ihght(pcl.lclpres));
                    h2 = nsharpNative.nsharpLib.agl(nsharpNative.nsharpLib
                            .ihght(pcl.elpres));
                }
            } else if (NsharpNativeConstants.STORM_MOTION_TYPE_STR2[i]
                    .equals("Lower Half Storm Depth")) {
                oldlplchoice = lpvls.flag;
                nsharpNative.nsharpLib.define_parcel(
                        NsharpNativeConstants.PARCELTYPE_MOST_UNSTABLE,
                        NsharpNativeConstants.MU_LAYER);
                nsharpNative.nsharpLib.get_lpvaluesData(lpvls);// regain lpvls
                                                               // value after
                                                               // defined parcel
                sfctemp = lpvls.temp;
                sfcdwpt = lpvls.dwpt;
                sfcpres = lpvls.pres;
                nsharpNative.nsharpLib.parcel(-1.0F, -1.0F, sfcpres, sfctemp,
                        sfcdwpt, pcl);
                float el = nsharpNative.nsharpLib.agl(nsharpNative.nsharpLib
                        .ihght(pcl.elpres));
                if (pcl.bplus >= 100.0) {

                    float base = nsharpNative.nsharpLib
                            .agl(nsharpNative.nsharpLib.ihght(botPF.getValue()));
                    float depth = (el - base);
                    h1 = base;
                    h2 = base + (depth * 0.5f);

                    nsharpNative.nsharpLib.get_lpvaluesData(lpvls);
                }

                // System.out.println("drawPanel2-2 called define_parcel pType="+oldlplchoice+" pre="+
                // pres);
                try {
                    if (oldlplchoice == NsharpNativeConstants.PARCELTYPE_USER_DEFINED) {
                        pres = NsharpParcelDialog.getUserDefdParcelMb();
                    } else
                        pres = NsharpNativeConstants.parcelToLayerMap
                                .get(oldlplchoice);

                    // reset and define oldchoice parcel
                    nsharpNative.nsharpLib.define_parcel(oldlplchoice, pres);
                } catch (NullPointerException e) {
                    // when in changing pane configuration situation, an odd
                    // scenario may happened that
                    // "oldlplchoice" may be a null, and
                    // parcelToLayerMap.get(oldlplchoice); throws a
                    // NullPointerException. In that case, we do not
                    // re-define_parcel and continue on
                    e.printStackTrace();
                }

            } else {
                h1 = NsharpNativeConstants.STORM_MOTION_HEIGHT2[i][0];
                h2 = NsharpNativeConstants.STORM_MOTION_HEIGHT2[i][1];
            }
            if (h1 != -999 && h2 != -999) {
                // SRH
                // calculate helicity
                float totHeli = nsharpNative.nsharpLib.helicity(h1, h2,
                        smdir.getValue(), smspd.getValue(), fValue, fValue1);
                target.drawString(myfont,
                        NsharpNativeConstants.STORM_MOTION_TYPE_STR2[i],
                        startX, curY, 0.0, TextStyle.NORMAL,
                        NsharpConstants.color_white, HorizontalAlignment.LEFT,
                        VerticalAlignment.TOP, null);

                // gc.drawText(NsharpNativeConstants.STORM_MOTION_TYPE_STR[i],0,
                // textLineNumber*textHeight + graphLineNumber);
                if (nsharpNative.nsharpLib.qc(fValue.getValue()) == 1
                        && nsharpNative.nsharpLib.qc(fValue1.getValue()) == 1) {
                    // textStr =
                    // NsharpNativeConstants.STORM_MOTION_TYPE_STR[i]+"          %.0f";
                    textStr = String.format("%.0f", totHeli);
                } else {
                    textStr = "M";
                }
                // SR Helicity is not shown for these 4 storm motions, see
                // oroginal BigNsharp show_shear_new()
                // target.drawString(myfont, textStr, firstToken, curY , 0.0,
                // TextStyle.NORMAL, NsharpConstants.color_white,
                // HorizontalAlignment.LEFT,
                // VerticalAlignment.TOP, null);

                // Calculate wind shear
                // Note: -1 as first parameter indicating bottom layer is
                // surface layer, see wind.c for wind_shear() source code
                nsharpNative.nsharpLib.wind_shear(nsharpNative.nsharpLib
                        .ipres(nsharpNative.nsharpLib.msl(h1)),
                        nsharpNative.nsharpLib.ipres(nsharpNative.nsharpLib
                                .msl(h2)), fValue, fValue1, fValue2, fValue3);
                if (nsharpNative.nsharpLib.qc(fValue3.getValue()) == 1) {
                    textStr = String.format("%.0f", fValue3.getValue());

                } else {
                    textStr = "M";
                }
                target.drawString(myfont, textStr, secondToken, curY, 0.0,
                        TextStyle.NORMAL, NsharpConstants.color_white,
                        HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);

                // Calculate mean wind
                nsharpNative.nsharpLib.mean_wind(nsharpNative.nsharpLib
                        .ipres(nsharpNative.nsharpLib.msl(h1)),
                        nsharpNative.nsharpLib.ipres(nsharpNative.nsharpLib
                                .msl(h2)), fValue, fValue1, fValue2, fValue3);
                if (nsharpNative.nsharpLib.qc(fValue2.getValue()) == 1
                        && nsharpNative.nsharpLib.qc(fValue3.getValue()) == 1) {
                    textStr = String.format("%.0f/%.0f", fValue2.getValue(),
                            fValue3.getValue());
                } else {
                    textStr = "M";
                }
                target.drawString(myfont, textStr, thirdToken, curY, 0.0,
                        TextStyle.NORMAL, NsharpConstants.color_white,
                        HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);

                // calculate pressure-weighted SR mean wind
                nsharpNative.nsharpLib.sr_wind(nsharpNative.nsharpLib
                        .ipres(nsharpNative.nsharpLib.msl(h1)),
                        nsharpNative.nsharpLib.ipres(nsharpNative.nsharpLib
                                .msl(h2)), smdir.getValue(), smspd.getValue(),
                        fValue, fValue1, fValue2, fValue3);
                if (nsharpNative.nsharpLib.qc(fValue2.getValue()) == 1) {
                    textStr = String.format("%.0f/%.0f", fValue2.getValue(),
                            fValue3.getValue());
                } else {
                    textStr = "M";
                }
                target.drawString(myfont, textStr, forthToken, curY, 0.0,
                        TextStyle.NORMAL, NsharpConstants.color_white,
                        HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
                if (NsharpNativeConstants.STORM_MOTION_TYPE_STR2[i]
                        .equals("Lower Half Storm Depth")) {
                    // draw bax around it
                    Rectangle rectangle = new Rectangle(rect.x, (int) curY,
                            rect.width, (int) charHeight);
                    // System.out.println("rect.x="+ rectangle.x+ " y="+
                    // rectangle.y+" w="+rectangle.width+
                    // " h="+rectangle.height);
                    PixelExtent pixExt = new PixelExtent(rectangle);
                    target.drawRect(pixExt, NsharpConstants.color_gold, 1.0f,
                            1.0f);
                }
            }
            curY = curY + charHeight; // move to new line

        }
        // align all following parameters output with "Corfidi Downshear"
        double hRatio = paintProps.getView().getExtent().getWidth()
                / paintProps.getCanvasBounds().width;
        DrawableString str = new DrawableString("Corfidi Downshear = ",
                NsharpConstants.color_white);
        double equalSignPos = secondToken;// rect.x+target.getStringsBounds(str).getWidth()*hRatio*xRatio;

        // BRN Shear
        // get parcel data by calling native nsharp parcel() API. value is
        // returned in pcl
        // current parcel is reset earlief already we dont have to call
        // define_parcel() again.
        nsharpNative.nsharpLib.get_lpvaluesData(lpvls);
        sfctemp = lpvls.temp;
        sfcdwpt = lpvls.dwpt;
        sfcpres = lpvls.pres;
        nsharpNative.nsharpLib.parcel(-1.0F, -1.0F, sfcpres, sfctemp, sfcdwpt,
                pcl);

        nsharpNative.nsharpLib.cave_bulk_rich2(fValue);
        if (nsharpNative.nsharpLib.qc(fValue.getValue()) == 1)
            textStr = String.format("%.0f m%c/s%c", fValue.getValue(),
                    NsharpConstants.SQUARE_SYMBOL,
                    NsharpConstants.SQUARE_SYMBOL);
        else
            textStr = "  M";
        str.setCoordinates(startX, curY);
        str.horizontalAlignment = HorizontalAlignment.LEFT;
        str.verticallAlignment = VerticalAlignment.TOP;
        str.font = myfont;
        str.setText("BRN Shear = ", NsharpConstants.color_white);
        DrawableString str1 = new DrawableString(textStr,
                NsharpConstants.color_white);
        str1.setCoordinates(equalSignPos, curY);
        str1.horizontalAlignment = HorizontalAlignment.LEFT;
        str1.verticallAlignment = VerticalAlignment.TOP;
        str1.font = myfont;
        target.drawStrings(str, str1);

        curY = curY + charHeight; // move to new line
        // 4-6km srw
        nsharpNative.nsharpLib.sr_wind(
                nsharpNative.nsharpLib.ipres(nsharpNative.nsharpLib.msl(4000)),
                nsharpNative.nsharpLib.ipres(nsharpNative.nsharpLib.msl(6000)),
                smdir.getValue(), smspd.getValue(), fValue, fValue1, fValue2,
                fValue3);
        if (nsharpNative.nsharpLib.qc(fValue2.getValue()) == 1) {
            textStr = String.format("%.0f/%.0f kt", fValue2.getValue(),
                    fValue3.getValue());
        } else {
            textStr = " M";
        }
        str.setText("4-6km SR Wind =", NsharpConstants.color_white);
        str.setCoordinates(startX, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(equalSignPos, curY);
        target.drawStrings(str, str1);

        curY = curY + charHeight; // move to new line

        // Corfidi Downshear, we use fValue3, fValue4 only by calling
        // corfidi_MCS_motion
        nsharpNative.nsharpLib.corfidi_MCS_motion(fValue1, fValue2, fValue3,
                fValue4, fValue5, fValue6, fValue7, fValue8);
        textStr = String.format("%.0f/%.0f kt", fValue3.getValue(),
                fValue4.getValue());
        str.setText("Corfidi Downshear =", NsharpConstants.color_white);
        str.setCoordinates(startX, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(equalSignPos, curY);
        target.drawStrings(str, str1);
        curY = curY + charHeight; // move to new line

        // Corfidi Upshear, we use fValue7, fValue8 only by calling
        // corfidi_MCS_motion
        textStr = String.format("%.0f/%.0f kt", fValue7.getValue(),
                fValue8.getValue());
        str.setText("Corfidi Upshear =", NsharpConstants.color_white);
        str.setCoordinates(startX, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(equalSignPos, curY);
        target.drawStrings(str, str1);
        curY = curY + charHeight; // move to new line

        // Bunkers Right
        nsharpNative.nsharpLib.bunkers_storm_motion(fValue1, fValue2, fValue3,
                fValue4);
        textStr = String.format("%.0f/%.0f kt", fValue3.getValue(),
                fValue4.getValue());
        str.setText("Bunkers Right =", NsharpConstants.color_red);
        str.setCoordinates(startX, curY);
        str1.setText(textStr, NsharpConstants.color_red);
        str1.setCoordinates(equalSignPos, curY);
        target.drawStrings(str, str1);

        curY = curY + charHeight; // move to new line
        // Bunkers Left
        nsharpNative.nsharpLib.bunkers_left_motion(fValue1, fValue2, fValue3,
                fValue4);
        textStr = String.format("%.0f/%.0f kt", fValue3.getValue(),
                fValue4.getValue());
        str.setText("Bunkers Left =", NsharpConstants.color_cyan);
        str.setCoordinates(startX, curY);
        str1.setText(textStr, NsharpConstants.color_cyan);
        str1.setCoordinates(equalSignPos, curY);
        target.drawStrings(str, str1);
        curY = curY + charHeight; // move to new line

        // STPC(test) - test STP using cape6km
        // Chin Note: BigNsharp sigtorn_test always return -9999..a bug
        float stpTest = nsharpNative.nsharpLib.sigtorn_test(smdir.getValue(),
                smspd.getValue());
        // System.out.println("smdir="+smdir.getValue()+"smspd="+
        // smspd.getValue()+"stpTest="+stpTest+
        // "p_bot="+botPF.getValue()+"p_top="+topPF.getValue());
        if (nsharpNative.nsharpLib.qc(stpTest) == 1)
            textStr = String.format("%.1f", stpTest);
        else
            textStr = " M";
        str.setText("STPC(test) =", NsharpConstants.color_white);
        str.setCoordinates(startX, curY);
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(equalSignPos, curY);
        target.drawStrings(str, str1);

        // wind barb labels
        str1.setText("12345SPACE", NsharpConstants.color_red); // rest str1 to
                                                               // get a right x
                                                               // position
        // firstToken = equalSignPos+
        // (target.getStringsBounds(str1).getMaxX())*hRatio*xRatio;
        textStr = "1km";
        str.setText(textStr, NsharpConstants.color_red);
        str.setCoordinates(thirdToken, curY);
        textStr = " & ";
        str1.setText(textStr, NsharpConstants.color_white);
        str1.setCoordinates(thirdToken
                + target.getStringsBounds(str).getWidth() * hRatio * xRatio,
                curY);
        textStr = "6km";
        DrawableString str2 = new DrawableString(textStr,
                NsharpConstants.color_cyan);
        str2.horizontalAlignment = HorizontalAlignment.LEFT;
        str2.verticallAlignment = VerticalAlignment.TOP;
        str2.font = myfont;
        str2.setCoordinates(thirdToken
                + (target.getStringsBounds(str).getWidth() + target
                        .getStringsBounds(str1).getWidth()) * hRatio * xRatio,
                curY);
        textStr = " AGL Wind Barb";
        DrawableString str3 = new DrawableString(textStr,
                NsharpConstants.color_white);
        str3.horizontalAlignment = HorizontalAlignment.LEFT;
        str3.verticallAlignment = VerticalAlignment.TOP;
        str3.font = myfont;
        str3.setCoordinates(thirdToken
                + (target.getStringsBounds(str).getWidth()
                        + target.getStringsBounds(str1).getWidth() + target
                        .getStringsBounds(str2).getWidth()) * hRatio * xRatio,
                curY);
        target.drawStrings(str, str1, str2, str3);

        // 1 km wind barb
        double wbToken = (thirdToken + forthToken) / 2;
        str1.setText("1234/56 ktSPACESPACESPACE", NsharpConstants.color_red); // rest
                                                                              // str1
                                                                              // to
                                                                              // get
                                                                              // a
                                                                              // right
                                                                              // x
                                                                              // position
        // firstToken = equalSignPos+
        // (target.getStringsBounds(str1).getMaxX())*hRatio*xRatio;
        double yOri = curY - 3 * charHeight;
        double barbScaleF = charHeight;// 12;
        List<LineStroke> barb = WindBarbFactory.getWindGraphics(
                (double) nsharpNative.nsharpLib.iwspd(nsharpNative.nsharpLib
                        .ipres(nsharpNative.nsharpLib.msl(1000))),
                (double) nsharpNative.nsharpLib.iwdir(nsharpNative.nsharpLib
                        .ipres(nsharpNative.nsharpLib.msl(1000))));
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
                // here. Therefore, need to
                // draw wind barb ourself.
                if (stroke.getType() == "M") {
                    cur0X = point.x;
                    cur0Y = newY;
                } else if (stroke.getType() == "D") {
                    cur1X = point.x;
                    cur1Y = newY;
                    target.drawLine(cur0X, cur0Y, 0.0, cur1X, cur1Y, 0.0,
                            NsharpConstants.color_red, 1);
                    // bsteffen added these two lines to fix 50 kts wind barbs
                    cur0X = cur1X;
                    cur0Y = cur1Y;
                }
            }
        }
        // 6 km wind barb
        barb.clear();
        barb = WindBarbFactory.getWindGraphics((double) nsharpNative.nsharpLib
                .iwspd(nsharpNative.nsharpLib.ipres(nsharpNative.nsharpLib
                        .msl(6000))), (double) nsharpNative.nsharpLib
                .iwdir(nsharpNative.nsharpLib.ipres(nsharpNative.nsharpLib
                        .msl(6000))));
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
                // here. Therefore, need to
                // draw wind barb ourself.
                if (stroke.getType() == "M") {
                    cur0X = point.x;
                    cur0Y = newY;
                } else if (stroke.getType() == "D") {
                    cur1X = point.x;
                    cur1Y = newY;
                    target.drawLine(cur0X, cur0Y, 0.0, cur1X, cur1Y, 0.0,
                            NsharpConstants.color_cyan, 1);
                    // bsteffen added these two lines to fix 50 kts wind barbs
                    cur0X = cur1X;
                    cur0Y = cur1Y;
                }
            }
        }
        // myfont.dispose();
    }

    @SuppressWarnings("deprecation")
    private void drawPanel3(IGraphicsTarget target, Rectangle rect)
            throws VizException {
        IFont myfont;
        myfont = defaultFont;
        defineCharHeight(myfont);
        myfont.setSmoothing(false);
        myfont.setScaleFont(false);
        extent = new PixelExtent(rect);
        target.setupClippingPlane(extent);
        // myfont = target.initializeFont(fontName, fontSize, null);
        String splitedStr[];
        String textStr;
        curY = rect.y;
        // Chin's NOTE::::this function is coded based on native nsharp
        // show_parcel() in xwvid3.c
        // moved from NsharpPaletteWindow.showParcelData()

        // if we can not Interpolates a temp with 700 mb pressure, then we dont
        // have enough raw data
        if (nsharpNative.nsharpLib.qc(nsharpNative.nsharpLib.itemp(700.0F)) == 0)
            return;
        String title = NsharpNativeConstants.PARCEL_DATA_STR;

        target.drawString(myfont, title, rect.x + rect.width / 3, curY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_cyan,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);

        String hdrStr;
        // short currentParcel;
        float layerPressure = 0;
        ;
        // get user selected parcel type
        hdrStr = NsharpNativeConstants.parcelToHdrStrMap.get(currentParcel);
        layerPressure = NsharpNativeConstants.parcelToLayerMap
                .get(currentParcel);
        if (currentParcel == NsharpNativeConstants.PARCELTYPE_USER_DEFINED) {
            layerPressure = NsharpParcelDialog.getUserDefdParcelMb();
            hdrStr = String.format(hdrStr, layerPressure);
        }
        curY = curY + charHeight;
        target.drawString(myfont, hdrStr, rect.x + rect.width / 4, curY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_yellow,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        // call native define_parcel() with parcel type and user defined
        // pressure (if user defined it)
        nsharpNative.nsharpLib.define_parcel(currentParcel, layerPressure);

        _lplvalues lpvls = new _lplvalues();
        nsharpNative.nsharpLib.get_lpvaluesData(lpvls);

        float sfctemp, sfcdwpt, sfcpres;
        sfctemp = lpvls.temp;
        sfcdwpt = lpvls.dwpt;
        sfcpres = lpvls.pres;

        // get parcel data by calling native nsharp parcel() API. value is
        // returned in pcl
        _parcel pcl = new _parcel();
        nsharpNative.nsharpLib.parcel(-1.0F, -1.0F, sfcpres, sfctemp, sfcdwpt,
                pcl);
        textStr = NsharpNativeConstants.PARCEL_LPL_LINE_;
        textStr = String.format(textStr, (int) pcl.lplpres, (int) pcl.lpltemp,
                (int) pcl.lpldwpt,
                (int) nsharpNative.nsharpLib.ctof(pcl.lpltemp),
                (int) nsharpNative.nsharpLib.ctof(pcl.lpldwpt));
        // Chin: note: target.drawString does NOT handle formatted string. For
        // example, "ABC\tabc" will be printed
        // as "ABCabc" So, we have to add '_' to separate each value when
        // formatting String.
        // Then, use String.split() to have each value in a sub-string.

        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }

        curY = curY + 2 * charHeight;

        if (nsharpNative.nsharpLib.qc(pcl.bplus) == 1) {
            textStr = NsharpNativeConstants.PARCEL_CAPE_LINE;
            textStr = String.format(textStr, pcl.bplus);
        } else {
            textStr = NsharpNativeConstants.PARCEL_CAPE_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_white, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);

        if (nsharpNative.nsharpLib.qc(pcl.li5) == 1) {
            textStr = NsharpNativeConstants.PARCEL_LI_LINE;
            textStr = String.format(textStr, pcl.li5);
        } else {
            textStr = NsharpNativeConstants.PARCEL_LI_MISSING;
        }
        target.drawString(myfont, textStr, rect.x + rect.width / 2, curY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;
        if (nsharpNative.nsharpLib.qc(pcl.bfzl) == 1) {
            textStr = NsharpNativeConstants.PARCEL_BFZL_LINE;
            textStr = String.format(textStr, pcl.bfzl);
        } else {
            textStr = NsharpNativeConstants.PARCEL_BFZL_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_white, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        if (nsharpNative.nsharpLib.qc(pcl.limax) == 1) {
            textStr = NsharpNativeConstants.PARCEL_LIMIN_LINE;
            textStr = String.format(textStr, pcl.limax, pcl.limaxpres);
        } else {
            textStr = NsharpNativeConstants.PARCEL_LIMIN_MISSING;
        }
        target.drawString(myfont, textStr, rect.x + rect.width / 2, curY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;
        if (nsharpNative.nsharpLib.qc(pcl.bminus) == 1) {
            textStr = NsharpNativeConstants.PARCEL_CINH_LINE;
            textStr = String.format(textStr, pcl.bminus);
        } else {
            textStr = NsharpNativeConstants.PARCEL_CINH_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_white, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);

        if (nsharpNative.nsharpLib.qc(pcl.cap) == 1) {
            textStr = NsharpNativeConstants.PARCEL_CAP_LINE;
            textStr = String.format(textStr, pcl.cap, pcl.cappres);
        } else {
            textStr = NsharpNativeConstants.PARCEL_CAP_MISSING;
        }
        target.drawString(myfont, textStr, rect.x + rect.width / 2, curY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + 2 * charHeight;

        textStr = NsharpNativeConstants.PARCEL_LEVEL_LINE_;
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;
        target.drawLine(rect.x, curY, 0.0, rect.x + rect.width, curY, 0.0,
                NsharpConstants.color_white, 1);

        if (nsharpNative.nsharpLib.qc(pcl.lclpres) == 1
                && nsharpNative.nsharpLib.qc(nsharpNative.nsharpLib
                        .mtof(nsharpNative.nsharpLib.agl(nsharpNative.nsharpLib
                                .ihght(pcl.lclpres)))) == 1) {
            textStr = NsharpNativeConstants.PARCEL_LCL_LINE_;
            textStr = String.format(textStr, pcl.lclpres,
                    nsharpNative.nsharpLib.mtof(nsharpNative.nsharpLib
                            .agl(nsharpNative.nsharpLib.ihght(pcl.lclpres))));
        } else {
            textStr = NsharpNativeConstants.PARCEL_LCL_MISSING_;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        if (nsharpNative.nsharpLib.qc(pcl.lfcpres) == 1
                && nsharpNative.nsharpLib.qc(nsharpNative.nsharpLib
                        .mtof(nsharpNative.nsharpLib.agl(nsharpNative.nsharpLib
                                .ihght(pcl.lfcpres)))) == 1
                && nsharpNative.nsharpLib.qc(nsharpNative.nsharpLib
                        .itemp(pcl.lfcpres)) == 1) {
            textStr = NsharpNativeConstants.PARCEL_LFC_LINE_;
            textStr = String.format(textStr, pcl.lfcpres,
                    nsharpNative.nsharpLib.mtof(nsharpNative.nsharpLib
                            .agl(nsharpNative.nsharpLib.ihght(pcl.lfcpres))),
                    nsharpNative.nsharpLib.itemp(pcl.lfcpres));
        } else {
            textStr = NsharpNativeConstants.PARCEL_LFC_MISSING_;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        if (nsharpNative.nsharpLib.qc(pcl.elpres) == 1
                && nsharpNative.nsharpLib.qc(nsharpNative.nsharpLib
                        .mtof(nsharpNative.nsharpLib.agl(nsharpNative.nsharpLib
                                .ihght(pcl.elpres)))) == 1
                && nsharpNative.nsharpLib.qc(nsharpNative.nsharpLib
                        .itemp(pcl.elpres)) == 1) {
            textStr = NsharpNativeConstants.PARCEL_EL_LINE_;
            textStr = String.format(textStr, pcl.elpres, nsharpNative.nsharpLib
                    .mtof(nsharpNative.nsharpLib.agl(nsharpNative.nsharpLib
                            .ihght(pcl.elpres))), nsharpNative.nsharpLib
                    .itemp(pcl.elpres));
        } else {
            textStr = NsharpNativeConstants.PARCEL_EL_MISSING_;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        if (nsharpNative.nsharpLib.qc(pcl.mplpres) == 1
                && nsharpNative.nsharpLib.qc(nsharpNative.nsharpLib
                        .mtof(nsharpNative.nsharpLib.agl(nsharpNative.nsharpLib
                                .ihght(pcl.mplpres)))) == 1) {
            textStr = NsharpNativeConstants.PARCEL_MPL_LINE_;
            textStr = String.format(textStr, pcl.mplpres,
                    nsharpNative.nsharpLib.mtof(nsharpNative.nsharpLib
                            .agl(nsharpNative.nsharpLib.ihght(pcl.mplpres))));
        } else {
            textStr = NsharpNativeConstants.PARCEL_MPL_MISSING_;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        // myfont.dispose();
    }

    @SuppressWarnings("deprecation")
    private void drawPanel4(IGraphicsTarget target, Rectangle rect)
            throws VizException {
        IFont myfont;
        myfont = defaultFont;
        defineCharHeight(myfont);
        myfont.setSmoothing(false);
        myfont.setScaleFont(false);
        extent = new PixelExtent(rect);
        target.setupClippingPlane(extent);
        // myfont = target.initializeFont(fontName, fontSize, null);
        String textStr;
        curY = rect.y;

        /*
         * Chin's NOTE::::this function is coded based on legacy native nsharp
         * software show_thermoparms(), show_moisture(),show_instability() in
         * xwvid3.c
         * 
         * Moved from NsharpPaletteWindow.showThermoparms()
         */

        target.drawString(myfont, NsharpNativeConstants.THERMO_DATA_STR, rect.x
                + rect.width / 3, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_cyan, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        curY = curY + charHeight;
        target.drawString(myfont, NsharpNativeConstants.THERMO_MOISTURE_STR,
                rect.x + rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_yellow, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        fValue.setValue(0);
        nsharpNative.nsharpLib.precip_water(fValue, -1.0F, -1.0F);
        if (nsharpNative.nsharpLib.qc(fValue.getValue()) == 1) {
            textStr = NsharpNativeConstants.THERMO_PWATER_LINE;
            textStr = String.format(textStr, fValue.getValue());
        } else {
            textStr = NsharpNativeConstants.THERMO_PWATER_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_white, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);

        fValue.setValue(0);
        nsharpNative.nsharpLib.mean_relhum(fValue, -1.0F, -1.0F);
        if (nsharpNative.nsharpLib.qc(fValue.getValue()) == 1) {
            textStr = NsharpNativeConstants.THERMO_MEANRH_LINE;
            textStr = String.format(textStr, fValue.getValue(),
                    NsharpConstants.PERCENT_SYMBOL);
        } else {
            textStr = NsharpNativeConstants.THERMO_MEANRH_MISSING;
        }
        target.drawString(myfont, textStr, rect.x + rect.width / 2, curY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        fValue.setValue(0);
        nsharpNative.nsharpLib.mean_mixratio(fValue, -1.0F, -1.0F);
        if (nsharpNative.nsharpLib.qc(fValue.getValue()) == 1) {
            textStr = NsharpNativeConstants.THERMO_MEANW_LINE;
            textStr = String.format(textStr, fValue.getValue());
        } else {
            textStr = NsharpNativeConstants.THERMO_MEANW_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_white, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        fValue.setValue(0);
        fValue1.setValue(0);
        // get surface pressure (fValue1) before getting mean LRH value
        nsharpNative.nsharpLib.get_surface(fValue1, fValue2, fValue3); // fValue
                                                                       // 2 and
                                                                       // fValue3
                                                                       // are
                                                                       // not of
                                                                       // concern
                                                                       // here
        nsharpNative.nsharpLib.mean_relhum(fValue, -1.0F,
                fValue1.getValue() - 150);
        if (nsharpNative.nsharpLib.qc(fValue.getValue()) == 1) {
            textStr = NsharpNativeConstants.THERMO_MEANLRH_LINE;
            textStr = String.format(textStr, fValue.getValue(),
                    NsharpConstants.PERCENT_SYMBOL);
        } else {
            textStr = NsharpNativeConstants.THERMO_MEANLRH_MISSING;
        }
        target.drawString(myfont, textStr, rect.x + rect.width / 2, curY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        fValue.setValue(0);
        nsharpNative.nsharpLib.top_moistlyr(fValue);
        if (nsharpNative.nsharpLib.qc(fValue.getValue()) == 1) {
            textStr = NsharpNativeConstants.THERMO_TOP_LINE;
            textStr = String
                    .format(textStr, fValue.getValue(), nsharpNative.nsharpLib
                            .mtof(nsharpNative.nsharpLib
                                    .agl(nsharpNative.nsharpLib.ihght(fValue
                                            .getValue()))));
        } else {
            textStr = NsharpNativeConstants.THERMO_TOP_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_white, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        // instability data--------------//
        // yellow and bold for parcel header
        target.drawString(myfont, NsharpNativeConstants.THERMO_INSTABILITY_STR,
                rect.x + rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_yellow, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);

        curY = curY + charHeight;

        fValue.setValue(0);
        fValue1.setValue(0);

        nsharpNative.nsharpLib.delta_t(fValue);
        nsharpNative.nsharpLib.lapse_rate(fValue1, 700.0F, 500.0F);

        if (nsharpNative.nsharpLib.qc(fValue.getValue()) == 1
                && nsharpNative.nsharpLib.qc(fValue1.getValue()) == 1) {
            textStr = NsharpNativeConstants.THERMO_700500mb_LINE;
            textStr = String.format(textStr, fValue.getValue(),
                    fValue1.getValue());
        } else {
            textStr = NsharpNativeConstants.THERMO_700500mb_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_white, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        fValue.setValue(0);
        fValue1.setValue(0);

        nsharpNative.nsharpLib.vert_tot(fValue);
        nsharpNative.nsharpLib.lapse_rate(fValue1, 850.0F, 500.0F);

        if (nsharpNative.nsharpLib.qc(fValue.getValue()) == 1
                && nsharpNative.nsharpLib.qc(fValue1.getValue()) == 1) {
            textStr = NsharpNativeConstants.THERMO_850500mb_LINE;
            textStr = String.format(textStr, fValue.getValue(),
                    fValue1.getValue());
        } else {
            textStr = NsharpNativeConstants.THERMO_850500mb_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_white, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        // misc parameters data--------------//
        target.drawString(myfont, NsharpNativeConstants.THERMO_MISC_PARMS_STR,
                rect.x + rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_yellow, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        fValue.setValue(0);
        fValue1.setValue(0);
        fValue2.setValue(0);
        nsharpNative.nsharpLib.t_totals(fValue, fValue1, fValue2);
        if (nsharpNative.nsharpLib.qc(fValue.getValue()) == 1) {
            textStr = NsharpNativeConstants.THERMO_TOTAL_LINE;
            textStr = String.format(textStr, fValue.getValue());
        } else {
            textStr = NsharpNativeConstants.THERMO_TOTAL_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_white, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);

        fValue.setValue(0);
        nsharpNative.nsharpLib.k_index(fValue);
        if (nsharpNative.nsharpLib.qc(fValue.getValue()) == 1) {
            textStr = NsharpNativeConstants.THERMO_KINDEX_LINE;
            textStr = String.format(textStr, fValue.getValue());
        } else {
            textStr = NsharpNativeConstants.THERMO_KINDEX_MISSING;
        }
        target.drawString(myfont, textStr, rect.x + rect.width / 2, curY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        fValue.setValue(0);
        nsharpNative.nsharpLib.sweat_index(fValue);
        if (nsharpNative.nsharpLib.qc(fValue.getValue()) == 1) {
            textStr = NsharpNativeConstants.THERMO_SWEAT_LINE;
            textStr = String.format(textStr, fValue.getValue());
        } else {
            textStr = NsharpNativeConstants.THERMO_SWEAT_MISSING;
        }

        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_white, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);

        fValue.setValue(0);
        float maxTempF = nsharpNative.nsharpLib.ctof(nsharpNative.nsharpLib
                .max_temp(fValue, -1));
        if (nsharpNative.nsharpLib.qc(maxTempF) == 1) {
            textStr = NsharpNativeConstants.THERMO_MAXT_LINE;
            textStr = String.format(textStr, maxTempF);
        } else {
            textStr = NsharpNativeConstants.THERMO_MAXT_MISSING;
        }
        target.drawString(myfont, textStr, rect.x + rect.width / 2, curY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        fValue.setValue(0);
        float theDiff = nsharpNative.nsharpLib.ThetaE_diff(fValue);
        if (nsharpNative.nsharpLib.qc(theDiff) == 1) {
            textStr = NsharpNativeConstants.THERMO_THETAE_LINE;
            textStr = String.format(textStr, theDiff);
        } else {
            textStr = NsharpNativeConstants.THERMO_THETAE_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_white, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        fValue.setValue(0);
        float conTempF = nsharpNative.nsharpLib.ctof(nsharpNative.nsharpLib
                .cnvtv_temp(fValue, -1));
        if (nsharpNative.nsharpLib.qc(conTempF) == 1) {
            textStr = NsharpNativeConstants.THERMO_CONVT_LINE;
            textStr = String.format(textStr, conTempF);
        } else {
            textStr = NsharpNativeConstants.THERMO_CONVT_MISSING;
        }
        target.drawString(myfont, textStr, rect.x + rect.width / 2, curY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        fValue.setValue(0);
        float wbzft = nsharpNative.nsharpLib.mtof(nsharpNative.nsharpLib
                .agl(nsharpNative.nsharpLib.ihght(nsharpNative.nsharpLib
                        .wb_lvl(0, fValue))));
        if (nsharpNative.nsharpLib.qc(wbzft) == 1) {
            textStr = NsharpNativeConstants.THERMO_WBZ_LINE;
            textStr = String.format(textStr, wbzft);
        } else {
            textStr = NsharpNativeConstants.THERMO_WBZ_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_white, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        fValue.setValue(0);
        float fgzft = nsharpNative.nsharpLib.mtof(nsharpNative.nsharpLib
                .agl(nsharpNative.nsharpLib.ihght(nsharpNative.nsharpLib
                        .temp_lvl(0, fValue))));
        if (nsharpNative.nsharpLib.qc(fgzft) == 1) {
            textStr = NsharpNativeConstants.THERMO_FGZ_LINE;
            textStr = String.format(textStr, fgzft);
        } else {
            textStr = NsharpNativeConstants.THERMO_FGZ_MISSING;
        }
        target.drawString(myfont, textStr, rect.x + rect.width / 2, curY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        // myfont.dispose();
    }

    @SuppressWarnings("deprecation")
    private void drawPanel5(IGraphicsTarget target, Rectangle rect)
            throws VizException {
        IFont myfont;
        myfont = defaultFont;
        defineCharHeight(myfont);
        myfont.setSmoothing(false);
        myfont.setScaleFont(false);
        extent = new PixelExtent(rect);
        target.setupClippingPlane(extent);
        // myfont = target.initializeFont(fontName, fontSize, null);
        String splitedStr[];
        String textStr;
        curY = rect.y;

        /*
         * Chin's NOTE::::this function is coded based on legacy nsharp software
         * show_gradient() in xwvid3.c
         */
        FloatByReference Surfpressure = new FloatByReference(0);
        FloatByReference surfTemp = new FloatByReference(0);
        FloatByReference surfDewpt = new FloatByReference(0);
        target.drawString(myfont, NsharpNativeConstants.OPC_LOW_LEVEL_STR,
                rect.x + rect.width / 3, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_cyan, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        curY = curY + charHeight;
        target.drawString(myfont, NsharpNativeConstants.OPC_SURFACE975_STR,
                rect.x + rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_yellow, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        textStr = NsharpNativeConstants.OPC_LEVEL_LINE_;
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;
        target.drawLine(rect.x, curY, 0.0, rect.x + rect.width, curY, 0.0,
                NsharpConstants.color_white, 1);

        float ht = nsharpNative.nsharpLib.ihght(975);
        if (ht == NsharpNativeConstants.NSHARP_LEGACY_LIB_INVALID_DATA)
            textStr = NsharpNativeConstants.OPC_975_LINE_MISSING_;
        else {
            textStr = NsharpNativeConstants.OPC_975_LINE_;
            textStr = String.format(textStr, nsharpNative.nsharpLib.ihght(975),
                    nsharpNative.nsharpLib.itemp(975));
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        ht = 0;
        // get surface pressure (fValue1), Surface_temp (fValue2)
        nsharpNative.nsharpLib.get_surface(Surfpressure, surfTemp, surfDewpt);
        if (nsharpNative.nsharpLib.qc(Surfpressure.getValue()) == 1)
            ht = nsharpNative.nsharpLib.ihght(Surfpressure.getValue());
        if (nsharpNative.nsharpLib.qc(Surfpressure.getValue()) == 1
                && nsharpNative.nsharpLib.qc(surfTemp.getValue()) == 1) {
            textStr = NsharpNativeConstants.OPC_SURFACE_LINE_;
            textStr = String.format(textStr, Surfpressure.getValue(), ht,
                    surfTemp.getValue());
        } else {
            textStr = NsharpNativeConstants.OPC_SURFACE_MISSING_;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        /* ----- Sfc-975 Grad ----- */
        /*
         * make sure both 975mb layer and surface layer temperatures are
         * available
         */
        if (nsharpNative.nsharpLib.qc(nsharpNative.nsharpLib.itemp(975)) == 1
                && nsharpNative.nsharpLib.qc(surfTemp.getValue()) == 1) {
            textStr = NsharpNativeConstants.OPC_975_SURFACE_LINE;
            textStr = String.format(textStr, nsharpNative.nsharpLib.itemp(975)
                    - surfTemp.getValue());
        } else {
            textStr = NsharpNativeConstants.OPC_975_SURFACE_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_white, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        curY = curY + charHeight;
        /*
         * Chin's NOTE::::this function is coded based on legacy nsharp software
         * show_inversion() in xwvid3.c
         * 
         * inv_mb - Pressure of inversion level (mb) inv_dC - Change in
         * temperature (C)
         */
        FloatByReference inv_mb = new FloatByReference(0);
        FloatByReference inv_dC = new FloatByReference(0);
        ;
        // yellow and bold for parcel header
        target.drawString(myfont, NsharpNativeConstants.OPC_LOWEST_INV_STR,
                rect.x + rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_yellow, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        curY = curY + 2 * charHeight;
        nsharpNative.nsharpLib.low_inv(inv_mb, inv_dC);
        if (nsharpNative.nsharpLib.qc(nsharpNative.nsharpLib.ihght(inv_mb
                .getValue())) == 1) {
            textStr = NsharpNativeConstants.OPC_BASEHEIGHT_LINE;
            textStr = String.format(textStr,
                    nsharpNative.nsharpLib.ihght(inv_mb.getValue()));
        } else {
            textStr = NsharpNativeConstants.OPC_BASEHEIGHT_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_white, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        if (nsharpNative.nsharpLib.qc(inv_mb.getValue()) == 1) {
            textStr = NsharpNativeConstants.OPC_BASEPRESSURE_LINE;
            textStr = String.format(textStr, inv_mb.getValue());
        } else {
            textStr = NsharpNativeConstants.OPC_BASEPRESSURE_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_white, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        if (nsharpNative.nsharpLib.qc(inv_dC.getValue()) == 1) {
            textStr = NsharpNativeConstants.OPC_CHANGE_IN_TEMP_LINE;
            textStr = String.format(textStr, inv_dC.getValue());
        } else {
            textStr = NsharpNativeConstants.OPC_CHANGE_IN_TEMP_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_white, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);

        // myfont.dispose();
    }

    @SuppressWarnings("deprecation")
    private void drawPanel6(IGraphicsTarget target, Rectangle rect)
            throws VizException {
        IFont myfont;
        myfont = defaultFont;
        defineCharHeight(myfont);
        myfont.setSmoothing(false);
        myfont.setScaleFont(false);
        extent = new PixelExtent(rect);
        target.setupClippingPlane(extent);
        // myfont = target.initializeFont(fontName, fontSize, null);
        String splitedStr[];
        String textStr;
        curY = rect.y;

        /*
         * Chin's NOTE::::this function is coded based on legacy nsharp software
         * show_mixheight() in xwvid3.c Calculates the mixing height using
         * legacy mix_height()
         * 
         * void mix_height ( float *mh_mb, float *mh_drct, float *mh_sped, float
         * *mh_dC, float *mh_lr, float *mh_drct_max, float *mh_sped_max, short
         * flag )
         * 
         * Where: flag = 0 Surface-based lapse rate flag = 1 Layer-based lapse
         * rate
         * 
         * mh_mb - Pressure at mixing height (mb) mh_drct - Wind direction at
         * mixing height (deg) mh_sped - Wind speed at mixing height (kt) mh_dC
         * - Layer change in temperature (C) mh_lr - Layer lapse rate (C/km)
         * mh_drct_max - Layer maximum wind direction (deg) mh_sped_max - Layer
         * maximum wind speed (kt)
         */
        FloatByReference mh_mb = new FloatByReference(0);
        FloatByReference mh_drct = new FloatByReference(0);
        FloatByReference mh_sped = new FloatByReference(0);
        FloatByReference mh_dC = new FloatByReference(0);
        FloatByReference mh_lr = new FloatByReference(0);
        FloatByReference mh_drct_max = new FloatByReference(0);
        FloatByReference mh_sped_max = new FloatByReference(0);
        short flag;

        // yellow and bold for parcel header
        target.drawString(myfont, NsharpNativeConstants.OPC_MIXING_HGT_STR,
                rect.x + rect.width / 10, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_yellow, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        curY = curY + charHeight;
        textStr = NsharpNativeConstants.OPC_DRY_AD_LINE;
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;
        textStr = NsharpNativeConstants.OPC_THRESH_LAPSE_LINE;
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        // Cyan color for Layer Based string
        target.drawString(myfont, NsharpNativeConstants.OPC_LAYER_BASED_STR,
                rect.x + rect.width / 3, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_cyan, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        curY = curY + charHeight;
        // calculate Layer-based lapse rate data
        flag = 1;
        nsharpNative.nsharpLib.mix_height(mh_mb, mh_drct, mh_sped, mh_dC,
                mh_lr, mh_drct_max, mh_sped_max, flag);

        if (nsharpNative.nsharpLib.qc(nsharpNative.nsharpLib.ihght(mh_mb
                .getValue())) == 1) {
            textStr = NsharpNativeConstants.OPC_MIXINGHEIGHT_LINE;
            textStr = String.format(textStr,
                    nsharpNative.nsharpLib.ihght(mh_mb.getValue()));
        } else {
            textStr = NsharpNativeConstants.OPC_MIXINGHEIGHT_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        if (nsharpNative.nsharpLib.qc(mh_mb.getValue()) == 1) {
            textStr = NsharpNativeConstants.OPC_MIXINGPRESSURE_LINE;
            textStr = String.format(textStr, mh_mb.getValue());
        } else {
            textStr = NsharpNativeConstants.OPC_MIXINGPRESSURE_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        if (nsharpNative.nsharpLib.qc(mh_drct.getValue()) == 1
                && nsharpNative.nsharpLib.qc(mh_sped.getValue()) == 1) {
            textStr = NsharpNativeConstants.OPC_TOPMIXLAYER_LINE;
            // System.out.println("speed  = " + mh_sped.getValue());
            textStr = String.format(textStr, (int) mh_drct.getValue(),
                    NsharpConstants.DEGREE_SYMBOL, (int) (mh_sped.getValue()));
        } else {
            textStr = NsharpNativeConstants.OPC_TOPMIXLAYER_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        if (nsharpNative.nsharpLib.qc(mh_drct_max.getValue()) == 1
                && nsharpNative.nsharpLib.qc(mh_sped_max.getValue()) == 1) {
            textStr = NsharpNativeConstants.OPC_MIXLAYERMAX_LINE;
            textStr = String
                    .format(textStr, (int) mh_drct_max.getValue(),
                            NsharpConstants.DEGREE_SYMBOL,
                            (int) mh_sped_max.getValue());
        } else {
            textStr = NsharpNativeConstants.OPC_MIXLAYERMAX_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        if (nsharpNative.nsharpLib.qc(mh_dC.getValue()) == 1
                && nsharpNative.nsharpLib.qc(mh_lr.getValue()) == 1) {
            textStr = NsharpNativeConstants.OPC_LAYER_LAPSE_LINE;
            textStr = String
                    .format(textStr, mh_dC.getValue(), mh_lr.getValue());
        } else {
            textStr = NsharpNativeConstants.OPC_LAYER_LAPSE_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        // Purple color for Layer Based string
        target.drawString(myfont, NsharpNativeConstants.OPC_SURFACE_BASED_STR,
                rect.x + rect.width / 3, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_violet, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        curY = curY + charHeight;
        // calculate Surface-based lapse rate data
        flag = 0;
        mh_mb.setValue(0);
        mh_drct.setValue(0);
        mh_sped.setValue(0);
        mh_dC.setValue(0);
        mh_lr.setValue(0);
        mh_drct_max.setValue(0);
        mh_sped_max.setValue(0);
        ;
        nsharpNative.nsharpLib.mix_height(mh_mb, mh_drct, mh_sped, mh_dC,
                mh_lr, mh_drct_max, mh_sped_max, flag);

        // white color for text
        if (nsharpNative.nsharpLib.qc(nsharpNative.nsharpLib.ihght(mh_mb
                .getValue())) == 1) {
            textStr = NsharpNativeConstants.OPC_MIXINGHEIGHT_LINE;
            textStr = String.format(textStr,
                    nsharpNative.nsharpLib.ihght(mh_mb.getValue()));
        } else {
            textStr = NsharpNativeConstants.OPC_MIXINGHEIGHT_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        if (nsharpNative.nsharpLib.qc(mh_mb.getValue()) == 1) {
            textStr = NsharpNativeConstants.OPC_MIXINGPRESSURE_LINE;
            textStr = String.format(textStr, mh_mb.getValue());
        } else {
            textStr = NsharpNativeConstants.OPC_MIXINGPRESSURE_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        if (nsharpNative.nsharpLib.qc(mh_drct.getValue()) == 1
                && nsharpNative.nsharpLib.qc(mh_sped.getValue()) == 1) {
            textStr = NsharpNativeConstants.OPC_TOPMIXLAYER_LINE;
            textStr = String.format(textStr, (int) mh_drct.getValue(),
                    NsharpConstants.DEGREE_SYMBOL, (int) mh_sped.getValue());
        } else {
            textStr = NsharpNativeConstants.OPC_TOPMIXLAYER_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        if (nsharpNative.nsharpLib.qc(mh_drct_max.getValue()) == 1
                && nsharpNative.nsharpLib.qc(mh_sped_max.getValue()) == 1) {
            textStr = NsharpNativeConstants.OPC_MIXLAYERMAX_LINE;
            textStr = String
                    .format(textStr, (int) mh_drct_max.getValue(),
                            NsharpConstants.DEGREE_SYMBOL,
                            (int) mh_sped_max.getValue());
        } else {
            textStr = NsharpNativeConstants.OPC_MIXLAYERMAX_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        if (nsharpNative.nsharpLib.qc(mh_dC.getValue()) == 1
                && nsharpNative.nsharpLib.qc(mh_lr.getValue()) == 1) {
            textStr = NsharpNativeConstants.OPC_LAYER_LAPSE_LINE;
            textStr = String
                    .format(textStr, mh_dC.getValue(), mh_lr.getValue());
        } else {
            textStr = NsharpNativeConstants.OPC_LAYER_LAPSE_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        // myfont.dispose();
    }

    @SuppressWarnings("deprecation")
    private void drawPanel7(IGraphicsTarget target, Rectangle rect)
            throws VizException {
        IFont myfont;
        myfont = defaultFont;
        defineCharHeight(myfont);
        myfont.setSmoothing(false);
        myfont.setScaleFont(false);
        extent = new PixelExtent(rect);
        target.setupClippingPlane(extent);
        // myfont = target.initializeFont(fontName, fontSize, null);
        String splitedStr[];
        String textStr;
        curY = rect.y;
        /*
         * Chin's NOTE::::this function is coded based on legacy nsharp software
         * show_srdata() in xwvid3.c. Hard coded numerical numbers are directly
         * copied from it.
         * 
         * float helicity ( float lower, float upper, float sdir, float sspd,
         * float *phel, float *nhel ) Calculates the storm-relative helicity
         * (m2/s2) of a layer from LOWER(m, agl) to UPPER(m, agl). Uses the
         * storm motion vector (sdir, sspd).
         * 
         * lower - Bottom level of layer (m, AGL)[-1=LPL] upper - Top level of
         * layer (m, AGL) [-1=LFC] sdir - Storm motion direction (degrees) sspd
         * - Storm motion speed (kt) phel - Positive helicity in layer (m2/s2)
         * nhel - Negative helicity in layer (m2/s2) RETURN VALUE - Total
         * helicity (m2/s2)
         * 
         * void sr_wind ( float pbot, float ptop, float sdir, float sspd, float
         * *mnu, float *mnv, float *wdir, float *wspd ) Calculates a
         * pressure-weighted SR mean wind thru the layer (pbot-ptop). Default
         * layer is LFC-EL. pbot - Bottom level of layer (mb) ptop - Top level
         * of layer (mb) sdir - Storm motion dirction (deg) sspd - Storm motion
         * speed (kt) mnu - U-Component of mean wind (kt) mnv - V-Component of
         * mean wind (kt) /
         */

        // FloatByReference sspd= new FloatByReference(0);
        // FloatByReference sdir= new FloatByReference(0);
        FloatByReference phel = new FloatByReference(0);
        FloatByReference nhel = new FloatByReference(0);
        FloatByReference mnu = new FloatByReference(0);
        FloatByReference mnv = new FloatByReference(0);
        FloatByReference smdir = new FloatByReference(0);
        FloatByReference smspd = new FloatByReference(0);
        FloatByReference wdir = new FloatByReference(0);
        FloatByReference wspd = new FloatByReference(0);
        float totHeli;

        target.drawString(myfont, NsharpNativeConstants.STORM_RELATIVE_STR,
                rect.x + rect.width / 3, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_cyan, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        nsharpNative.nsharpLib.get_storm(smspd, smdir);

        if (nsharpNative.nsharpLib.qc(smspd.getValue()) == 1
                && nsharpNative.nsharpLib.qc(smdir.getValue()) == 1) {
            textStr = NsharpNativeConstants.STORM_MOTION_LINE;
            textStr = String.format(textStr, smdir.getValue(),
                    NsharpConstants.DEGREE_SYMBOL, smspd.getValue(),
                    nsharpNative.nsharpLib.kt_to_mps(smspd.getValue()));
        } else {
            textStr = NsharpNativeConstants.STORM_MOTION_MISSING;
        }
        target.drawString(myfont, textStr, rect.x + rect.width / 4, curY, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_white,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        // yellow and bold for parcel header
        target.drawString(myfont, NsharpNativeConstants.STORM_HELICITY_STR,
                rect.x + rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_yellow, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        curY = curY + charHeight;
        textStr = NsharpNativeConstants.STORM_LAYER_POS_STR;
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;
        target.drawLine(rect.x, curY, 0.0, rect.x + rect.width, curY, 0.0,
                NsharpConstants.color_white, 1);

        // calculate helicity for sfc-2 km
        totHeli = nsharpNative.nsharpLib.helicity((float) 0, (float) 2000,
                smdir.getValue(), smspd.getValue(), phel, nhel);
        if (nsharpNative.nsharpLib.qc(phel.getValue()) == 1
                && nsharpNative.nsharpLib.qc(nhel.getValue()) == 1) {
            textStr = NsharpNativeConstants.STORM_SFC2KM_LINE;
            textStr = String.format(textStr, phel.getValue(), nhel.getValue(),
                    totHeli, NsharpConstants.SQUARE_SYMBOL,
                    NsharpConstants.SQUARE_SYMBOL);

        } else {
            textStr = NsharpNativeConstants.STORM_SFC2KM_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;
        // calculate helicity for sfc-3 km
        totHeli = nsharpNative.nsharpLib.helicity((float) 0, (float) 3000,
                smdir.getValue(), smspd.getValue(), phel, nhel);
        if (nsharpNative.nsharpLib.qc(phel.getValue()) == 1
                && nsharpNative.nsharpLib.qc(nhel.getValue()) == 1) {
            textStr = NsharpNativeConstants.STORM_SFC3KM_LINE;
            textStr = String.format(textStr, phel.getValue(), nhel.getValue(),
                    totHeli, NsharpConstants.SQUARE_SYMBOL,
                    NsharpConstants.SQUARE_SYMBOL);
        } else {
            textStr = NsharpNativeConstants.STORM_SFC3KM_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;
        // calculate helicity for LPL - LFC
        totHeli = nsharpNative.nsharpLib.helicity((float) -1, (float) -1,
                smdir.getValue(), smspd.getValue(), phel, nhel);
        if (nsharpNative.nsharpLib.qc(phel.getValue()) == 1
                && nsharpNative.nsharpLib.qc(nhel.getValue()) == 1) {
            textStr = NsharpNativeConstants.STORM_LPL_LFC_LINE;
            textStr = String.format(textStr, phel.getValue(), nhel.getValue(),
                    totHeli, NsharpConstants.SQUARE_SYMBOL,
                    NsharpConstants.SQUARE_SYMBOL);
        } else {
            textStr = NsharpNativeConstants.STORM_LPL_LFC_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;
        // yellow and bold for header
        target.drawString(myfont, NsharpNativeConstants.STORM_WIND_STR, rect.x
                + rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_yellow, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        curY = curY + 2 * charHeight;
        textStr = NsharpNativeConstants.STORM_LAYER_VECTOR_STR;
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;
        target.drawLine(rect.x, curY, 0.0, rect.x + rect.width, curY, 0.0,
                NsharpConstants.color_white, 1);

        // calculate pressure-weighted SR mean wind at sfc-2 km
        nsharpNative.nsharpLib.sr_wind(
                nsharpNative.nsharpLib.ipres(nsharpNative.nsharpLib.msl(0)),
                nsharpNative.nsharpLib.ipres(nsharpNative.nsharpLib.msl(2000)),
                smdir.getValue(), smspd.getValue(), mnu, mnv, wdir, wspd);
        if (nsharpNative.nsharpLib.qc(wdir.getValue()) == 1) {
            textStr = NsharpNativeConstants.STORM_SFC2KM_VECT_LINE;
            textStr = String.format(textStr, wdir.getValue(), wspd.getValue(),
                    nsharpNative.nsharpLib.kt_to_mps(wspd.getValue()));
        } else {
            textStr = NsharpNativeConstants.STORM_SFC2KM_VECT_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        // calculate pressure-weighted SR mean wind at 4-6 km
        // System.out.println("msl(4000))="+ nsharpNative.nsharpLib.msl(4000) +
        // "i_pres(msl(4000))" +
        // nsharpNative.nsharpLib.i_pres(nsharpNative.nsharpLib.msl(4000)));
        // System.out.println("msl(6000))="+ nsharpNative.nsharpLib.msl(6000) +
        // "i_pres(msl(6000))" +
        // nsharpNative.nsharpLib.i_pres(nsharpNative.nsharpLib.msl(6000)));
        // System.out.println("dir "+ smdir.getValue()+ " spd " +
        // smspd.getValue());
        nsharpNative.nsharpLib.sr_wind(
                nsharpNative.nsharpLib.ipres(nsharpNative.nsharpLib.msl(4000)),
                nsharpNative.nsharpLib.ipres(nsharpNative.nsharpLib.msl(6000)),
                smdir.getValue(), smspd.getValue(), mnu, mnv, wdir, wspd);
        if (nsharpNative.nsharpLib.qc(wdir.getValue()) == 1) {
            textStr = NsharpNativeConstants.STORM_4_6KM_VECT_LINE;
            // System.out.println("wdir "+ wdir.getValue() + " widSp " +
            // wspd.getValue());
            textStr = String.format(textStr, wdir.getValue(), wspd.getValue(),
                    nsharpNative.nsharpLib.kt_to_mps(wspd.getValue()));
        } else {
            textStr = NsharpNativeConstants.STORM_4_6KM_VECT_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        // calculate pressure-weighted SR mean wind at 9-11 km
        nsharpNative.nsharpLib
                .sr_wind(nsharpNative.nsharpLib.ipres(nsharpNative.nsharpLib
                        .msl(9000)), nsharpNative.nsharpLib
                        .ipres(nsharpNative.nsharpLib.msl(11000)), smdir
                        .getValue(), smspd.getValue(), mnu, mnv, wdir, wspd);
        if (nsharpNative.nsharpLib.qc(wdir.getValue()) == 1) {
            textStr = NsharpNativeConstants.STORM_9_11KM_VECT_LINE;
            textStr = String.format(textStr, wdir.getValue(), wspd.getValue(),
                    nsharpNative.nsharpLib.kt_to_mps(wspd.getValue()));
        } else {
            textStr = NsharpNativeConstants.STORM_9_11KM_VECT_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }

        // myfont.dispose();
    }

    @SuppressWarnings("deprecation")
    private void drawPanel8(IGraphicsTarget target, Rectangle rect)
            throws VizException {
        IFont myfont;
        myfont = defaultFont;
        defineCharHeight(myfont);
        myfont.setSmoothing(false);
        myfont.setScaleFont(false);
        extent = new PixelExtent(rect);
        target.setupClippingPlane(extent);
        // myfont = target.initializeFont(fontName, fontSize, null);
        String splitedStr[];
        String textStr;
        curY = rect.y;
        /*
         * Chin's NOTE::::this function is coded based on legacy nsharp software
         * show_meanwind() in xwvid3.c
         * 
         * void mean_wind ( float pbot, float ptop, float *mnu, float *mnv,
         * float *wdir, float *wspd ) Calculates a pressure-weighted mean wind
         * thru the layer (pbot-ptop). Default layer is LFC-EL.
         * 
         * pbot - Bottom level of layer (mb) ptop - Top level of layer (mb) mnu
         * - U-Component of mean wind (kt) mnv - V-Component of mean wind (kt)
         */
        FloatByReference mnu = new FloatByReference(0);
        FloatByReference mnv = new FloatByReference(0);
        FloatByReference wdir = new FloatByReference(0);
        FloatByReference wspd = new FloatByReference(0);
        target.drawString(myfont, NsharpNativeConstants.MEAN_WIND_STR, rect.x
                + rect.width * 0.4, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_cyan, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        // fix TT 549890
        // Calculate mean wind at 0-6 km, following the same algorithm used in
        // drawPanel2() at BigNsharp page 2.
        // Like this :
        // mean_wind(nsharpNative.nsharpLib.ipres(nsharpNative.nsharpLib.msl(h1)),
        // nsharpNative.nsharpLib.ipres(nsharpNative.nsharpLib.msl(h2)))
        nsharpNative.nsharpLib.mean_wind(
                nsharpNative.nsharpLib.ipres(nsharpNative.nsharpLib.msl(0)),
                nsharpNative.nsharpLib.ipres(nsharpNative.nsharpLib.msl(6000)),
                mnu, mnv, wdir, wspd);
        // this line was nsharpNative.nsharpLib.mean_wind( -1,
        // nsharpNative.nsharpLib.ipres(nsharpNative.nsharpLib.agl(6000)), mnu,
        // mnv, wdir, wspd);
        // System.out.println("wsp ="+ wspd.getValue()+ " wdir "+
        // wdir.getValue() + " agl(6000)="+nsharpNative.nsharpLib.agl(6000)+
        // " preAt6000="+nsharpNative.nsharpLib.i_pres(nsharpNative.nsharpLib.agl(6000)));
        if (nsharpNative.nsharpLib.qc(wdir.getValue()) == 1
                && nsharpNative.nsharpLib.qc(wspd.getValue()) == 1) {
            textStr = NsharpNativeConstants.MEANWIND_SFC6KM_LINE;
            textStr = String.format(textStr, (wdir.getValue()),
                    wspd.getValue(),
                    nsharpNative.nsharpLib.kt_to_mps(wspd.getValue()));
        } else {
            textStr = NsharpNativeConstants.MEANWIND_SFC6KM_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        // fix TT 549890
        // Calculate mean wind at LFC-EL, following the same algorithm used in
        // drawPanel2() for LCL_EL for BigNsharp pag
        // Replacing LCL with LFC
        _lplvalues lpvls;
        _parcel pcl;
        lpvls = new _lplvalues();
        pcl = new _parcel();
        float h1, h2;
        h1 = -1;
        h2 = -1;
        nsharpNative.nsharpLib.get_lpvaluesData(lpvls);

        float sfctemp = lpvls.temp;
        float sfcdwpt = lpvls.dwpt;
        float sfcpres = lpvls.pres;
        nsharpNative.nsharpLib.parcel(-1.0F, -1.0F, sfcpres, sfctemp, sfcdwpt,
                pcl);
        if (pcl.bplus > 0) {
            h1 = nsharpNative.nsharpLib.agl(nsharpNative.nsharpLib
                    .ihght(pcl.lfcpres));
            h2 = nsharpNative.nsharpLib.agl(nsharpNative.nsharpLib
                    .ihght(pcl.elpres));
        }

        // Calculate mean wind at LFC-EL
        nsharpNative.nsharpLib.mean_wind(
                nsharpNative.nsharpLib.ipres(nsharpNative.nsharpLib.msl(h1)),
                nsharpNative.nsharpLib.ipres(nsharpNative.nsharpLib.msl(h2)),
                mnu, mnv, wdir, wspd);
        if (nsharpNative.nsharpLib.qc(wdir.getValue()) == 1
                && nsharpNative.nsharpLib.qc(wspd.getValue()) == 1) {
            textStr = NsharpNativeConstants.MEANWIND_LFC_EL_LINE;
            textStr = String.format(textStr, wdir.getValue(), wspd.getValue(),
                    nsharpNative.nsharpLib.kt_to_mps(wspd.getValue()));
        } else {
            textStr = NsharpNativeConstants.MEANWIND_LFC_EL_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;
        // Calculate mean wind at 850-200 mb
        nsharpNative.nsharpLib.mean_wind(850, 200, mnu, mnv, wdir, wspd);
        if (nsharpNative.nsharpLib.qc(wdir.getValue()) == 1
                && nsharpNative.nsharpLib.qc(wspd.getValue()) == 1) {
            textStr = NsharpNativeConstants.MEANWIND_850_200MB_LINE;
            textStr = String.format(textStr, wdir.getValue(), wspd.getValue(),
                    nsharpNative.nsharpLib.kt_to_mps(wspd.getValue()));
        } else {
            textStr = NsharpNativeConstants.MEANWIND_850_200MB_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;
        /*
         * Chin's NOTE::::this function is coded based on legacy nsharp software
         * show_shear() in xwvid3.c
         * 
         * void wind_shear ( float pbot, float ptop, float *shu, float *shv,
         * float *sdir, float *smag )
         * 
         * Calculates the shear between the wind at (pbot) and (ptop). Default
         * lower wind is a 1km mean wind, while the default upper layer is 3km.
         * 
         * pbot - Bottom level of layer (mb) ptop - Top level of layer (mb) shu
         * - U-Component of shear (m/s) shv - V-Component of shear (m/s) sdir -
         * Direction of shear vector (degrees) smag - Magnitude of shear vector
         * (m/s)
         */
        FloatByReference shu = new FloatByReference(0);
        FloatByReference shv = new FloatByReference(0);
        FloatByReference sdir = new FloatByReference(0);
        FloatByReference smag = new FloatByReference(0);
        target.drawString(myfont,
                NsharpNativeConstants.ENVIRONMENTAL_SHEAR_STR, rect.x
                        + rect.width / 3, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_cyan, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        textStr = NsharpNativeConstants.SHEAR_LAYER_DELTA_STR;
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 3, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;
        target.drawLine(rect.x, curY, 0.0, rect.x + rect.width, curY, 0.0,
                NsharpConstants.color_white, 1);

        // Calculate wind shear at Low - 3 km
        nsharpNative.nsharpLib.wind_shear(
                nsharpNative.nsharpLib.ipres(nsharpNative.nsharpLib.msl(0)),
                nsharpNative.nsharpLib.ipres(nsharpNative.nsharpLib.msl(3000)),
                shu, shv, sdir, smag);
        if (nsharpNative.nsharpLib.qc(smag.getValue()) == 1) {
            textStr = NsharpNativeConstants.SHEAR_LOW_3KM_LINE;
            textStr = String.format(textStr, smag.getValue(),
                    nsharpNative.nsharpLib.kt_to_mps(smag.getValue()),
                    nsharpNative.nsharpLib.kt_to_mps(smag.getValue()) / .3F);
            // System.out.println("from cave "+smag.getValue() + " kt, " +
            // nsharpNative.nsharpLib.kt_to_mps(smag.getValue())+ " m/s, Tot="+
            // nsharpNative.nsharpLib.kt_to_mps(smag.getValue())/.3F);
        } else {
            textStr = NsharpNativeConstants.SHEAR_LOW_3KM_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 3, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        // Calculate wind shear at Sfc - 2 km
        nsharpNative.nsharpLib.wind_shear(
                nsharpNative.nsharpLib.ipres(nsharpNative.nsharpLib.msl(0)),
                nsharpNative.nsharpLib.ipres(nsharpNative.nsharpLib.msl(2000)),
                shu, shv, sdir, smag);
        if (nsharpNative.nsharpLib.qc(smag.getValue()) == 1) {
            textStr = NsharpNativeConstants.SHEAR_SFC_2KM_LINE;
            textStr = String.format(textStr, smag.getValue(),
                    nsharpNative.nsharpLib.kt_to_mps(smag.getValue()),
                    nsharpNative.nsharpLib.kt_to_mps(smag.getValue()) / .2F);
        } else {
            textStr = NsharpNativeConstants.SHEAR_SFC_2KM_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 3, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        // Calculate wind shear at Sfc - 6 km
        nsharpNative.nsharpLib.wind_shear(
                nsharpNative.nsharpLib.ipres(nsharpNative.nsharpLib.msl(0)),
                nsharpNative.nsharpLib.ipres(nsharpNative.nsharpLib.msl(6000)),
                shu, shv, sdir, smag);
        if (nsharpNative.nsharpLib.qc(smag.getValue()) == 1) {
            textStr = NsharpNativeConstants.SHEAR_SFC_6KM_LINE;
            textStr = String.format(textStr, smag.getValue(),
                    nsharpNative.nsharpLib.kt_to_mps(smag.getValue()),
                    nsharpNative.nsharpLib.kt_to_mps(smag.getValue()) / .6F);
        } else {
            textStr = NsharpNativeConstants.SHEAR_SFC_6KM_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 3, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        // Calculate wind shear at Sfc - 12 km
        nsharpNative.nsharpLib
                .wind_shear(nsharpNative.nsharpLib.ipres(nsharpNative.nsharpLib
                        .msl(0)), nsharpNative.nsharpLib
                        .ipres(nsharpNative.nsharpLib.msl(12000)), shu, shv,
                        sdir, smag);
        if (nsharpNative.nsharpLib.qc(smag.getValue()) == 1) {
            textStr = NsharpNativeConstants.SHEAR_SFC_12KM_LINE;
            textStr = String.format(textStr, smag.getValue(),
                    nsharpNative.nsharpLib.kt_to_mps(smag.getValue()),
                    nsharpNative.nsharpLib.kt_to_mps(smag.getValue()) / 1.2F);
        } else {
            textStr = NsharpNativeConstants.SHEAR_SFC_12KM_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 3, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        // myfont.dispose();
    }

    @SuppressWarnings("deprecation")
    private void drawPanel9(IGraphicsTarget target, Rectangle rect)
            throws VizException {
        IFont myfont;
        if (paneConfigurationName.equals(NsharpConstants.PANE_LITE_D2D_CFG_STR)) {
            myfont = font11;
        } else {
            myfont = defaultFont;
        }
        defineCharHeight(myfont);
        myfont.setSmoothing(false);
        myfont.setScaleFont(false);
        extent = new PixelExtent(rect);
        target.setupClippingPlane(extent);
        // myfont = target.initializeFont(fontName, fontSize, null);
        String splitedStr[];
        String textStr;
        curY = rect.y;
        /*
         * Chin's NOTE::::this function is coded based on legacy nsharp software
         * show_initiation(): Displays thunderstorm initiation parameters,
         * show_heavypcpn(), show_preciptype() and show_stormtype() in xwvid3.c
         */
        FloatByReference fvalue2 = new FloatByReference(0);
        FloatByReference fvalue3 = new FloatByReference(0);
        FloatByReference wdir = new FloatByReference(0);
        FloatByReference wspd = new FloatByReference(0);
        FloatByReference fvalue = new FloatByReference(0);
        FloatByReference fvalue1 = new FloatByReference(0);
        // get parcel data by calling native nsharp parcel() API. value is
        // returned in pcl
        // current parcel is already decided when page 1 is displyed. Note that
        // page 1 is always
        // displayed before this page (page 4). Therefore, we dont have to call
        // define_parcel() again.
        // set default
        // short currentParcel;
        float layerPressure;
        if (currentParcel == NsharpNativeConstants.PARCELTYPE_USER_DEFINED) {
            layerPressure = NsharpParcelDialog.getUserDefdParcelMb();
        } else
            layerPressure = NsharpNativeConstants.parcelToLayerMap
                    .get(currentParcel);
        nsharpNative.nsharpLib.define_parcel(currentParcel, layerPressure);

        _parcel pcl = new _parcel();
        ;
        _lplvalues lpvls = new _lplvalues();
        nsharpNative.nsharpLib.get_lpvaluesData(lpvls);
        float sfctemp, sfcdwpt, sfcpres;
        sfctemp = lpvls.temp;
        sfcdwpt = lpvls.dwpt;
        sfcpres = lpvls.pres;
        nsharpNative.nsharpLib.parcel(-1.0F, -1.0F, sfcpres, sfctemp, sfcdwpt,
                pcl);
        // _parcel.ByValue pcl_byvalue = new _parcel.ByValue();
        // nsharpNative.nsharpLib.parcel( -1.0F, -1.0F, sfcpres, sfctemp,
        // sfcdwpt, pcl_byvalue);

        // CONVECTIVE_INITIATION
        target.drawString(myfont,
                NsharpNativeConstants.CONVECTIVE_INITIATION_STR, rect.x
                        + rect.width / 3, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_cyan, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        curY = curY + charHeight;
        // CINH
        if (nsharpNative.nsharpLib.qc(pcl.bminus) == 1) {
            textStr = NsharpNativeConstants.CONVECTIVE_CINH_LINE;
            textStr = String.format(textStr, pcl.bminus);
        } else {
            textStr = NsharpNativeConstants.CONVECTIVE_CINH_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }

        // cap
        if (nsharpNative.nsharpLib.qc(pcl.cap) == 1
                && nsharpNative.nsharpLib.qc(pcl.cappres) == 1) {
            textStr = NsharpNativeConstants.CONVECTIVE_CAP_LINE;
            textStr = String.format(textStr, pcl.cap, pcl.cappres);
        } else {
            textStr = NsharpNativeConstants.CONVECTIVE_CAP_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + rect.width / 2
                    + i * rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        // K-index
        nsharpNative.nsharpLib.k_index(fvalue);
        if (nsharpNative.nsharpLib.qc(fvalue.getValue()) == 1) {
            textStr = NsharpNativeConstants.CONVECTIVE_KINDEX_LINE;
            textStr = String.format(textStr, fvalue.getValue());
        } else {
            textStr = NsharpNativeConstants.CONVECTIVE_KINDEX_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }

        // Mean RH
        nsharpNative.nsharpLib.mean_relhum(fvalue, -1, -1);
        if (nsharpNative.nsharpLib.qc(fvalue.getValue()) == 1) {
            textStr = NsharpNativeConstants.CONVECTIVE_MEANRH_LINE;
            textStr = String.format(textStr, fvalue.getValue(),
                    NsharpConstants.PERCENT_SYMBOL);
        } else {
            textStr = NsharpNativeConstants.CONVECTIVE_MEANRH_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + rect.width / 2
                    + i * rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + 2 * charHeight;

        // Top of M layer
        nsharpNative.nsharpLib.top_moistlyr(fvalue);
        // System.out.println("top_moistlyr=" + fvalue.getValue() );

        if (nsharpNative.nsharpLib.qc(fvalue.getValue()) == 1) {
            float ht = nsharpNative.nsharpLib.mtof(nsharpNative.nsharpLib
                    .agl(nsharpNative.nsharpLib.ihght(fvalue.getValue())));
            if (nsharpNative.nsharpLib.qc(ht) == 1) {
                textStr = NsharpNativeConstants.CONVECTIVE_TOP_LINE;
                textStr = String.format(textStr, fvalue.getValue(), ht);
            } else {
                textStr = NsharpNativeConstants.CONVECTIVE_TOP_MISSING;
            }
        } else {
            textStr = NsharpNativeConstants.CONVECTIVE_TOP_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_white, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        curY = curY + charHeight;
        // LFC height
        if (nsharpNative.nsharpLib.qc(pcl.lfcpres) == 1) {
            float ht = nsharpNative.nsharpLib.mtof(nsharpNative.nsharpLib
                    .agl(nsharpNative.nsharpLib.ihght(pcl.lfcpres)));
            if (nsharpNative.nsharpLib.qc(ht) == 1) {
                textStr = NsharpNativeConstants.CONVECTIVE_LFC_LINE;
                textStr = String.format(textStr, pcl.lfcpres, ht);
            } else {
                textStr = NsharpNativeConstants.CONVECTIVE_LFC_MISSING;
            }
        } else {
            textStr = NsharpNativeConstants.CONVECTIVE_LFC_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_white, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        // STORM TYPE
        target.drawString(myfont, NsharpNativeConstants.STORM_TYPE_STR, rect.x
                + rect.width / 3, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_cyan, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        // CAPE
        if (nsharpNative.nsharpLib.qc(pcl.bplus) == 1) {
            textStr = NsharpNativeConstants.STORM_TYPE_CAPE_LINE;
            textStr = String.format(textStr, pcl.bplus);
        } else {
            textStr = NsharpNativeConstants.STORM_TYPE_CAPE_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }

        // EFF. SREH
        float hel = 0;
        nsharpNative.nsharpLib.get_storm(wspd, wdir);
        if (nsharpNative.nsharpLib.qc(wdir.getValue()) == 1
                && nsharpNative.nsharpLib.qc(wspd.getValue()) == 1) {
            hel = nsharpNative.nsharpLib.helicity(-1.0F, -1.0F,
                    wdir.getValue(), wspd.getValue(), fvalue, fvalue1);
            if (nsharpNative.nsharpLib.qc(hel) == 1) {
                textStr = NsharpNativeConstants.STORM_TYPE_EFF_LINE;
                textStr = String.format(textStr, hel,
                        NsharpConstants.SQUARE_SYMBOL,
                        NsharpConstants.SQUARE_SYMBOL);
            } else {
                textStr = NsharpNativeConstants.STORM_TYPE_EFF_MISSING;
            }
        } else {
            textStr = NsharpNativeConstants.STORM_TYPE_EFF_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + rect.width / 2
                    + i * rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        // EHI
        if (nsharpNative.nsharpLib.qc(pcl.bplus) == 1) {
            float ehi = nsharpNative.nsharpLib.ehi(pcl.bplus, hel);
            if (nsharpNative.nsharpLib.qc(ehi) == 1) {
                textStr = NsharpNativeConstants.STORM_TYPE_EHI_LINE;
                textStr = String.format(textStr, ehi);
            } else {
                textStr = NsharpNativeConstants.STORM_TYPE_EHI_MISSING;
            }
        } else {
            textStr = NsharpNativeConstants.STORM_TYPE_EHI_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }

        // 3km Shear
        nsharpNative.nsharpLib.wind_shear(-1, -1, fvalue, fvalue1, fvalue2,
                fvalue3);
        if (nsharpNative.nsharpLib.qc(fvalue3.getValue()) == 1) {
            textStr = NsharpNativeConstants.STORM_TYPE_3KM_LINE;
            textStr = String.format(textStr,
                    nsharpNative.nsharpLib.kt_to_mps(fvalue3.getValue()));
        } else {
            textStr = NsharpNativeConstants.STORM_TYPE_3KM_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + rect.width / 2
                    + i * rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        // BRN
        if (nsharpNative.nsharpLib.qc(pcl.brn) == 1) {
            textStr = NsharpNativeConstants.STORM_TYPE_BRN_LINE;
            textStr = String.format(textStr, pcl.brn);
        } else {
            textStr = NsharpNativeConstants.STORM_TYPE_BRN_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }

        // BRN Shear
        // nsharpNative.nsharpLib.cave_bulk_rich(pcl.lplpres, pcl.bplus, fvalue
        // );
        // System.out.println("bulk_rich fvalue = "+ fvalue.getValue());
        nsharpNative.nsharpLib.cave_bulk_rich2(fvalue);
        if (nsharpNative.nsharpLib.qc(fvalue.getValue()) == 1) {
            textStr = NsharpNativeConstants.STORM_TYPE_BRNSHEAR_LINE;
            textStr = String.format(textStr, fvalue.getValue(),
                    NsharpConstants.SQUARE_SYMBOL,
                    NsharpConstants.SQUARE_SYMBOL);
        } else
            textStr = NsharpNativeConstants.STORM_TYPE_BRNSHEAR_MISSING;
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + rect.width / 2
                    + i * rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        // PRECIPITATION_TYPE
        target.drawString(myfont, NsharpNativeConstants.PRECIPITATION_TYPE_STR,
                rect.x + rect.width / 3, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_cyan, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        // Melting Level
        float web = nsharpNative.nsharpLib.wb_lvl(0, fvalue);
        if (nsharpNative.nsharpLib.qc(web) == 1) {

            float aglft = nsharpNative.nsharpLib.mtof(nsharpNative.nsharpLib
                    .agl(nsharpNative.nsharpLib.ihght(web)));
            if (nsharpNative.nsharpLib.qc(aglft) == 1) {
                textStr = NsharpNativeConstants.PRECIPITATION_MELTING_LINE;
                textStr = String.format(textStr, aglft, web);
            } else
                textStr = NsharpNativeConstants.PRECIPITATION_MELTING_MISSING;
        } else {
            textStr = NsharpNativeConstants.PRECIPITATION_MELTING_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_white, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        curY = curY + charHeight;
        // HEAVY_RAINFAL
        target.drawString(myfont, NsharpNativeConstants.HEAVY_RAINFALL_STR,
                rect.x + rect.width / 3, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_cyan, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        // Rogash_QPF, Chin: note: BigNsharp has different implementation of
        // Rogash_QPF()
        // We are using bigNsharp now.
        nsharpNative.nsharpLib.Rogash_QPF(fvalue);
        if (nsharpNative.nsharpLib.qc(fvalue.getValue()) == 1) {
            textStr = NsharpNativeConstants.HEAVY_ROGASH_LINE;
            textStr = String.format(textStr, fvalue.getValue());
        } else {
            textStr = NsharpNativeConstants.HEAVY_ROGASH_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_white, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        // myfont.dispose();
    }

    @SuppressWarnings("deprecation")
    private void drawPanel10(IGraphicsTarget target, Rectangle rect)
            throws VizException {
        IFont myfont;
        if (paneConfigurationName.equals(NsharpConstants.PANE_LITE_D2D_CFG_STR)) {
            myfont = font11;
        } else {
            myfont = defaultFont;
        }
        defineCharHeight(myfont);
        myfont.setSmoothing(false);
        myfont.setScaleFont(false);
        extent = new PixelExtent(rect);
        target.setupClippingPlane(extent);
        // myfont = target.initializeFont(fontName, fontSize, null);
        String splitedStr[];
        String textStr;
        curY = rect.y;
        /*
         * Chin's NOTE::::this function is coded based on legacy nsharp software
         * show_hailpot(), show_torpot() in xwvid3.c
         */
        FloatByReference fvalue2 = new FloatByReference(0);
        FloatByReference fvalue3 = new FloatByReference(0);
        FloatByReference wdir = new FloatByReference(0);
        FloatByReference wspd = new FloatByReference(0);
        FloatByReference fvalue = new FloatByReference(0);
        FloatByReference fvalue1 = new FloatByReference(0);

        target.drawString(myfont, NsharpNativeConstants.SEVERE_POTENTIAL_STR,
                rect.x + rect.width / 3, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_cyan, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        target.drawString(myfont,
                NsharpNativeConstants.SEVERE_HAIL_POTENTIAL_STR, rect.x
                        + rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_yellow, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        _parcel pcl = new _parcel();
        ;
        _lplvalues lpvls = new _lplvalues();
        nsharpNative.nsharpLib.get_lpvaluesData(lpvls);
        float sfctemp, sfcdwpt, sfcpres;
        sfctemp = lpvls.temp;
        sfcdwpt = lpvls.dwpt;
        sfcpres = lpvls.pres;
        nsharpNative.nsharpLib.parcel(-1.0F, -1.0F, sfcpres, sfctemp, sfcdwpt,
                pcl);
        // CAPE
        if (nsharpNative.nsharpLib.qc(pcl.bplus) == 1) {
            textStr = NsharpNativeConstants.SEVERE_CAPE_LINE;
            textStr = String.format(textStr, pcl.bplus);
        } else {
            textStr = NsharpNativeConstants.SEVERE_CAPE_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }

        // WBZ level
        float wbzft = nsharpNative.nsharpLib.mtof(nsharpNative.nsharpLib
                .agl(nsharpNative.nsharpLib.ihght(nsharpNative.nsharpLib
                        .wb_lvl(0, fvalue))));
        if (nsharpNative.nsharpLib.qc(wbzft) == 1) {
            textStr = NsharpNativeConstants.SEVERE_WBZ_LINE;
            textStr = String.format(textStr, wbzft);
        } else {
            textStr = NsharpNativeConstants.SEVERE_WBZ_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + rect.width / 2
                    + i * rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        // Mid Level RH
        nsharpNative.nsharpLib.mean_relhum(fvalue, 700, 500);
        if (nsharpNative.nsharpLib.qc(fvalue.getValue()) == 1) {
            textStr = NsharpNativeConstants.SEVERE_MIDRH_LINE;
            textStr = String.format(textStr, fvalue.getValue(),
                    NsharpConstants.PERCENT_SYMBOL);
        } else {
            textStr = NsharpNativeConstants.SEVERE_MIDSRW_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }

        // FZG level
        float fgzft = nsharpNative.nsharpLib.mtof(nsharpNative.nsharpLib
                .agl(nsharpNative.nsharpLib.ihght(nsharpNative.nsharpLib
                        .temp_lvl(0, fvalue))));
        if (nsharpNative.nsharpLib.qc(fgzft) == 1) {
            textStr = NsharpNativeConstants.SEVERE_FGZ_LINE;
            textStr = String.format(textStr, fgzft);
        } else {
            textStr = NsharpNativeConstants.SEVERE_FGZ_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + rect.width / 2
                    + i * rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        // EL Storm
        nsharpNative.nsharpLib.get_storm(wspd, wdir);
        nsharpNative.nsharpLib.sr_wind(pcl.elpres + 25, pcl.elpres - 25,
                wdir.getValue(), wspd.getValue(), fvalue, fvalue1, fvalue2,
                fvalue3);
        if (nsharpNative.nsharpLib.qc(fvalue3.getValue()) == 1) {
            textStr = NsharpNativeConstants.SEVERE_ELSTORM_LINE;
            textStr = String.format(textStr, fvalue3.getValue());
        } else {
            textStr = NsharpNativeConstants.SEVERE_ELSTORM_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_white, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        curY = curY + charHeight;
        // CHI1
        // _parcel.ByValue pcl_byvalue = new _parcel.ByValue();
        // pr = nsharpNative.nsharpLib.parcel( -1.0F, -1.0F, sfcpres, sfctemp,
        // sfcdwpt, pcl_byvalue);
        // Assigne values that needed for bulk_rich()
        // pcl_byvalue.lplpres = sfcpres;
        // pcl_byvalue.bplus = pcl.bplus;
        nsharpNative.nsharpLib.cave_bulk_rich2(fvalue);
        float rtn = (pcl.bplus * fvalue.getValue())
                / nsharpNative.nsharpLib.agl(nsharpNative.nsharpLib
                        .ihght(nsharpNative.nsharpLib.wb_lvl(0, fvalue1)));
        if (nsharpNative.nsharpLib.qc(rtn) == 1) {
            textStr = NsharpNativeConstants.SEVERE_CHI1_LINE;
            textStr = String.format(textStr, rtn);
        } else {
            textStr = NsharpNativeConstants.SEVERE_CHI1_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        // CHI2
        nsharpNative.nsharpLib.Mean_WBtemp(fvalue2, -1, -1);
        if (nsharpNative.nsharpLib.qc(rtn / fvalue2.getValue()) == 1) {
            textStr = NsharpNativeConstants.SEVERE_CHI2_LINE;
            textStr = String.format(textStr, rtn / fvalue2.getValue());
        } else {
            textStr = NsharpNativeConstants.SEVERE_CHI2_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + rect.width / 2
                    + i * rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        // Avg BL
        nsharpNative.nsharpLib.Mean_WBtemp(fvalue, -1, -1);
        if (nsharpNative.nsharpLib.qc(fvalue.getValue()) == 1) {
            textStr = NsharpNativeConstants.SEVERE_AVGBL_LINE;
            textStr = String.format(textStr, fvalue.getValue(),
                    NsharpConstants.DEGREE_SYMBOL);
        } else {
            textStr = NsharpNativeConstants.SEVERE_AVGBL_MISSING;
        }
        target.drawString(myfont, textStr, rect.x, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_white, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        // TORNADO_POTENTIAL
        target.drawString(myfont,
                NsharpNativeConstants.SEVERE_TORNADO_POTENTIAL_STR, rect.x
                        + rect.width / 4, curY, 0.0, TextStyle.NORMAL,
                NsharpConstants.color_yellow, HorizontalAlignment.LEFT,
                VerticalAlignment.TOP, null);
        curY = curY + charHeight;

        // Low SRW Sfc
        float blyr = nsharpNative.nsharpLib
                .ipres(nsharpNative.nsharpLib.msl(0));
        float tlyr = pcl.lfcpres;
        if (tlyr > 0) {
            nsharpNative.nsharpLib.sr_wind(blyr, tlyr, wdir.getValue(),
                    wspd.getValue(), fvalue, fvalue1, fvalue2, fvalue3);
            if (nsharpNative.nsharpLib.qc(fvalue3.getValue()) == 1) {
                textStr = NsharpNativeConstants.SEVERE_LOWSRWSFC_LINE;
                textStr = String.format(textStr, fvalue3.getValue());
            } else {
                textStr = NsharpNativeConstants.SEVERE_LOWSRWSFC_MISSING;
            }
        } else {
            textStr = NsharpNativeConstants.SEVERE_LOWSRWSFC_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        blyr = pcl.lfcpres;
        tlyr = nsharpNative.nsharpLib.ipres(nsharpNative.nsharpLib
                .ihght(pcl.lfcpres) + 4000);
        if ((tlyr > 0) && (blyr > 0)) {
            nsharpNative.nsharpLib.sr_wind(blyr, tlyr, wdir.getValue(),
                    wspd.getValue(), fvalue, fvalue1, fvalue2, fvalue3);
            if (nsharpNative.nsharpLib.qc(fvalue3.getValue()) == 1) {
                textStr = NsharpNativeConstants.SEVERE_MIDSRW_LINE;
                textStr = String.format(textStr, fvalue3.getValue());
            } else {
                textStr = NsharpNativeConstants.SEVERE_MIDSRW_MISSING;
            }

        } else {
            textStr = NsharpNativeConstants.SEVERE_MIDSRW_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }
        curY = curY + charHeight;

        blyr = nsharpNative.nsharpLib.ipres(nsharpNative.nsharpLib
                .ihght(pcl.elpres) - 4000);
        tlyr = pcl.elpres;
        if ((tlyr > 0) && (blyr > 0)) {
            nsharpNative.nsharpLib.sr_wind(blyr, tlyr, wdir.getValue(),
                    wspd.getValue(), fvalue, fvalue1, fvalue2, fvalue3);
            if (nsharpNative.nsharpLib.qc(fvalue3.getValue()) == 1) {
                textStr = NsharpNativeConstants.SEVERE_UPPERSRWEL_LINE;
                textStr = String.format(textStr, fvalue3.getValue());
            } else {
                textStr = NsharpNativeConstants.SEVERE_UPPERSRWEL_MISSING;
            }

        } else {
            textStr = NsharpNativeConstants.SEVERE_UPPERSRWEL_MISSING;
        }
        splitedStr = textStr.split("_", -1);
        for (int i = 0; i < splitedStr.length; i++) {
            target.drawString(myfont, splitedStr[i], rect.x + i * rect.width
                    / 2, curY, 0.0, TextStyle.NORMAL,
                    NsharpConstants.color_white, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
        }

        // myfont.dispose();
    }

    // Surface based CAPE -p1
    // Forecast parcel CAPE - p1
    // Most unstable CAPE -p1
    // LCL (shown as both mb andft) - p1(m) p3(mb)
    // LFC (shown as both mb and ft) - p1(m)p3(mb)
    // CIN - p1 CINH
    // Convective Temp (maybe) - p1/p4
    // Precipitable Water - p1/p4
    // Freezing Level - p1 FZL (FGZ level)
    // Wet Bulb Zero - p1 WBZ
    // 0-1 km Helicity - p2 SFC-1km SRH
    // 0-3 km Helicity - p2 SFC-3km SRH
    // BRN Shear - p2 BRN Shear, This is true for Surface-based,
    // Forecast, and Mixed-length Parcels. For Most-unstable and User-defined
    // parcels, The bottom level could be at 500m below the parcel level, not at
    // the surface. The depth in computing bulk shear is always 6km.
    // Bunkers RM Storm - p2 Bunkers Right Motion
    // Bulk Richardson Number -p9 BRN

    // start d2dlite
    @SuppressWarnings("deprecation")
    private void drawPanel11(IGraphicsTarget target, Rectangle rect)
            throws VizException {
        IFont myfont;
        if (paneConfigurationName
                .equals(NsharpConstants.PANE_SIMPLE_D2D_CFG_STR)
                || paneConfigurationName
                        .equals(NsharpConstants.PANE_SPCWS_CFG_STR)) {
            myfont = font11;
        } else {
            myfont = defaultFont;
        }
        defineCharHeight(myfont);
        myfont.setSmoothing(false);
        myfont.setScaleFont(false);
        extent = new PixelExtent(rect);
        target.setupClippingPlane(extent);
        // if we can not Interpolates a temp with 700 mb pressure, then we dont
        // have enough raw data
        if ((nsharpNative.nsharpLib.qc(nsharpNative.nsharpLib.itemp(700.0F)) == 0)) {
            target.drawString(myfont,  NO_DATA, rect.x,
                    rect.y, 0.0, TextStyle.NORMAL, NsharpConstants.color_cyan,
                    HorizontalAlignment.LEFT, VerticalAlignment.TOP, null);
            return;
        }

        // call get_topBotPres to set p_top and p_bot
        FloatByReference topPF = new FloatByReference(0);
        FloatByReference botPF = new FloatByReference(0);
        nsharpNative.nsharpLib.get_effectLayertopBotPres(topPF, botPF);

        String textStr;
        curY = rect.y;

        //
        // Start with Parcel Data
        //
        float layerPressure = 0;

        // get user selected parcel type
        _lplvalues lpvls;
        _parcel pcl;
        DrawableString str1 = new DrawableString(
                NsharpNativeConstants.PAGE1TEXT1_FCST_STR + "XX",
                NsharpConstants.color_white);
        str1.font = myfont;
        double hRatio = paintProps.getView().getExtent().getWidth()
                / paintProps.getCanvasBounds().width;
        double startX = rect.x + 0.5 * charWidth;
        double widthGap = rect.width / 4;
        str1.setText("12345ft", NsharpConstants.color_red);
        double aglWidth = target.getStringsBounds(str1).getWidth() * hRatio
                * xRatio;
        str1.setText("D2D Lite Page", NsharpConstants.color_red);
        str1.setCoordinates(startX, curY);
        str1.horizontalAlignment = HorizontalAlignment.LEFT;
        str1.verticallAlignment = VerticalAlignment.TOP;
        DrawableString str2 = new DrawableString("",
                NsharpConstants.color_white);
        // str2.setCoordinates(startX, curY);
        str2.horizontalAlignment = HorizontalAlignment.LEFT;
        str2.verticallAlignment = VerticalAlignment.TOP;
        str2.font = myfont;
        DrawableString str3 = new DrawableString("",
                NsharpConstants.color_white);
        // str3.setCoordinates(firstToken, curY);
        str3.horizontalAlignment = HorizontalAlignment.RIGHT;
        str3.verticallAlignment = VerticalAlignment.TOP;
        str3.font = myfont;
        DrawableString str4 = new DrawableString("",
                NsharpConstants.color_white);
        // str4.setCoordinates(secondToken, curY);
        str4.horizontalAlignment = HorizontalAlignment.LEFT;
        str4.verticallAlignment = VerticalAlignment.TOP;
        str4.font = myfont;
        DrawableString str5 = new DrawableString("",
                NsharpConstants.color_white);
        // str5.setCoordinates(thirdToken, curY);
        str5.horizontalAlignment = HorizontalAlignment.RIGHT;
        str5.verticallAlignment = VerticalAlignment.TOP;
        str5.font = myfont;
        DrawableString str6 = new DrawableString("",
                NsharpConstants.color_white);
        // str6.setCoordinates(startX, curY);
        str6.horizontalAlignment = HorizontalAlignment.LEFT;
        str6.verticallAlignment = VerticalAlignment.TOP;
        str6.font = myfont;
        DrawableString str7 = new DrawableString("",
                NsharpConstants.color_white);
        // str7.setCoordinates(firstToken, curY);
        str7.horizontalAlignment = HorizontalAlignment.RIGHT;
        str7.verticallAlignment = VerticalAlignment.TOP;
        str7.font = myfont;
        DrawableString str8 = new DrawableString("",
                NsharpConstants.color_white);
        // str8.setCoordinates(secondToken, curY);
        str8.horizontalAlignment = HorizontalAlignment.LEFT;
        str8.verticallAlignment = VerticalAlignment.TOP;
        str8.font = myfont;
        DrawableString str9 = new DrawableString("",
                NsharpConstants.color_white);
        // str9.setCoordinates(thirdToken, curY);
        str9.horizontalAlignment = HorizontalAlignment.RIGHT;
        str9.verticallAlignment = VerticalAlignment.TOP;
        str9.font = myfont;
        DrawableString str10 = new DrawableString("",
                NsharpConstants.color_white);
        // str10.setCoordinates(startX, curY);
        str10.horizontalAlignment = HorizontalAlignment.LEFT;
        str10.verticallAlignment = VerticalAlignment.TOP;
        str10.font = myfont;
        DrawableString str11 = new DrawableString("",
                NsharpConstants.color_white);
        // str11.setCoordinates(firstToken, curY);
        str11.horizontalAlignment = HorizontalAlignment.RIGHT;
        str11.verticallAlignment = VerticalAlignment.TOP;
        str11.font = myfont;
        DrawableString str12 = new DrawableString("",
                NsharpConstants.color_white);
        // str12.setCoordinates(secondToken, curY);
        str12.horizontalAlignment = HorizontalAlignment.LEFT;
        str12.verticallAlignment = VerticalAlignment.TOP;
        str12.font = myfont;
        DrawableString str13 = new DrawableString("",
                NsharpConstants.color_white);
        // str13.setCoordinates(thirdToken, curY);
        str13.horizontalAlignment = HorizontalAlignment.RIGHT;
        str13.verticallAlignment = VerticalAlignment.TOP;
        str13.font = myfont;

        target.drawStrings(str1);
        curY = curY + charHeight;
        target.drawLine(rect.x, curY, 0.0, rect.x + rect.width, curY, 0.0,
                NsharpConstants.color_white, 1);

        if (paneConfigurationName.equals(NsharpConstants.PANE_LITE_D2D_CFG_STR)) {
            firstToken = rect.x + widthGap + aglWidth;
            secondToken = rect.x + 2 * widthGap - charWidth;
            thirdToken = rect.x + 3 * widthGap + aglWidth;

            // thirdToken = thirdToken - aglWidth / 2;
            // forthToken = forthToken - aglWidth / 2;
            // fifthToken = fifthToken - aglWidth / 2;
            // sixthToken = sixthToken - aglWidth / 2;
            for (short parcelNumber = 1; parcelNumber <= NsharpNativeConstants.PARCEL_D2DLITE_MAX; parcelNumber++) {
                // call native define_parcel() with parcel type and user defined
                // pressure (if user defined it)
                textStr = NsharpNativeConstants.parcelToTypeStrMap
                        .get(parcelNumber);
                str1.setText(textStr, NsharpConstants.color_gold);

                str1.setCoordinates(startX, curY);
                float layerPressure1 = NsharpNativeConstants.parcelToLayerMap
                        .get(parcelNumber);
                nsharpNative.nsharpLib.define_parcel(parcelNumber,
                        layerPressure1);

                lpvls = new _lplvalues();
                nsharpNative.nsharpLib.get_lpvaluesData(lpvls);

                float sfctemp, sfcdwpt, sfcpres;
                sfctemp = lpvls.temp;
                sfcdwpt = lpvls.dwpt;
                sfcpres = lpvls.pres;
                // get parcel data by calling native nsharp parcel() API. value
                // is
                // returned in pcl
                pcl = new _parcel();
                nsharpNative.nsharpLib.parcel(-1.0F, -1.0F, sfcpres, sfctemp,
                        sfcdwpt, pcl);
                curY = curY + charHeight;
                // draw CAPE
                str2.setText("CAPE=", NsharpConstants.color_white);
                str2.setCoordinates(startX, curY);
                if (pcl.bplus != NsharpNativeConstants.NSHARP_LEGACY_LIB_INVALID_DATA)
                    str3.setText(String.format("%.0f", pcl.bplus),
                            NsharpConstants.color_white);
                else
                    str3.setText("M", NsharpConstants.color_white);
                str3.setCoordinates(firstToken, curY);

                // draw CINH
                str4.setText("CINH=", NsharpConstants.color_white);
                str4.setCoordinates(secondToken, curY);

                if (pcl.bminus != NsharpNativeConstants.NSHARP_LEGACY_LIB_INVALID_DATA)
                    str5.setText(String.format("%.0f", pcl.bminus),
                            NsharpConstants.color_white);
                else
                    str5.setText("M", NsharpConstants.color_white);
                str5.setCoordinates(thirdToken, curY);
                curY = curY + charHeight;
                // draw LCL
                str6.setText("LCL(Pres)=", NsharpConstants.color_white);
                str6.setCoordinates(startX, curY);
                str8.setText("LCL(AGL)=", NsharpConstants.color_white);
                str8.setCoordinates(secondToken, curY);
                if (nsharpNative.nsharpLib.qc(pcl.lclpres) == 1
                        && nsharpNative.nsharpLib.qc(nsharpNative.nsharpLib
                                .mtof(nsharpNative.nsharpLib
                                        .agl(nsharpNative.nsharpLib
                                                .ihght(pcl.lclpres)))) == 1) {
                    float lcl = nsharpNative.nsharpLib
                            .mtof(nsharpNative.nsharpLib
                                    .agl(nsharpNative.nsharpLib
                                            .ihght(pcl.lclpres)));

                    str7.setText(String.format("%5.0fmb", pcl.lclpres),
                            NsharpConstants.color_white);
                    str9.setText(String.format("%7.0fft", lcl),
                            NsharpConstants.color_white);
                } else {
                    str7.setText("M", NsharpConstants.color_white);
                    str9.setText("M", NsharpConstants.color_white);
                }
                str7.setCoordinates(firstToken, curY);
                str9.setCoordinates(thirdToken, curY);
                curY = curY + charHeight;
                // draw LFC
                str10.setText("LFC(Pres)=", NsharpConstants.color_white);
                str10.setCoordinates(startX, curY);
                str12.setText("LFC(AGL)=", NsharpConstants.color_white);
                str12.setCoordinates(secondToken, curY);
                if (nsharpNative.nsharpLib.qc(pcl.lfcpres) == 1
                        && nsharpNative.nsharpLib.qc(nsharpNative.nsharpLib
                                .mtof(nsharpNative.nsharpLib
                                        .agl(nsharpNative.nsharpLib
                                                .ihght(pcl.lfcpres)))) == 1
                        && nsharpNative.nsharpLib.qc(nsharpNative.nsharpLib
                                .itemp(pcl.lfcpres)) == 1) {
                    float lfc = nsharpNative.nsharpLib
                            .mtof(nsharpNative.nsharpLib
                                    .agl(nsharpNative.nsharpLib
                                            .ihght(pcl.lfcpres)));
                    str11.setText(String.format("%5.0fmb", pcl.lfcpres),
                            NsharpConstants.color_white);
                    str13.setText(String.format("%7.0fft", lfc),
                            NsharpConstants.color_white);
                } else {
                    str11.setText("M", NsharpConstants.color_white);
                    str13.setText("M", NsharpConstants.color_white);
                }
                str11.setCoordinates(firstToken, curY);
                str13.setCoordinates(thirdToken, curY);
                target.drawStrings(str1, str2, str3, str4, str5, str6, str7,
                        str8, str9, str10, str11, str12, str13);
                curY = curY + charHeight;
            }
        } else if (paneConfigurationName
                .equals(NsharpConstants.PANE_SIMPLE_D2D_CFG_STR)
                || paneConfigurationName
                        .equals(NsharpConstants.PANE_SPCWS_CFG_STR)) {
            widthGap = rect.width / 3;
            firstToken = rect.x + widthGap * 0.8;
            secondToken = rect.x + widthGap;
            thirdToken = secondToken + widthGap * 0.8;
            forthToken = rect.x + 2 * widthGap;
            fifthToken = forthToken + widthGap * 0.8;
            for (short parcelNumber = 1; parcelNumber <= NsharpNativeConstants.PARCEL_D2DLITE_MAX; parcelNumber++) {
                // call native define_parcel() with parcel type and user defined
                // pressure (if user defined it)
                textStr = NsharpNativeConstants.parcelToTypeStrMap
                        .get(parcelNumber);
                str1.setText(textStr, NsharpConstants.color_gold);

                str1.setCoordinates(startX, curY);
                float layerPressure1 = NsharpNativeConstants.parcelToLayerMap
                        .get(parcelNumber);
                nsharpNative.nsharpLib.define_parcel(parcelNumber,
                        layerPressure1);

                lpvls = new _lplvalues();
                nsharpNative.nsharpLib.get_lpvaluesData(lpvls);

                float sfctemp, sfcdwpt, sfcpres;
                sfctemp = lpvls.temp;
                sfcdwpt = lpvls.dwpt;
                sfcpres = lpvls.pres;
                // get parcel data by calling native nsharp parcel() API. value
                // is
                // returned in pcl
                pcl = new _parcel();
                nsharpNative.nsharpLib.parcel(-1.0F, -1.0F, sfcpres, sfctemp,
                        sfcdwpt, pcl);
                curY = curY + charHeight;
                // draw CAPE
                str2.setText("CAPE=", NsharpConstants.color_white);
                str2.setCoordinates(startX, curY);
                if (pcl.bplus != NsharpNativeConstants.NSHARP_LEGACY_LIB_INVALID_DATA)
                    str3.setText(String.format("%.0f", pcl.bplus),
                            NsharpConstants.color_white);
                else
                    str3.setText("M", NsharpConstants.color_white);
                str3.setCoordinates(firstToken, curY);

                // draw LCL
                str4.setText("LCL(mb)=", NsharpConstants.color_white);
                str4.setCoordinates(secondToken, curY);
                str6.setText("LCL(ft)=", NsharpConstants.color_white);
                str6.setCoordinates(forthToken, curY);
                if (nsharpNative.nsharpLib.qc(pcl.lclpres) == 1
                        && nsharpNative.nsharpLib.qc(nsharpNative.nsharpLib
                                .mtof(nsharpNative.nsharpLib
                                        .agl(nsharpNative.nsharpLib
                                                .ihght(pcl.lclpres)))) == 1) {
                    float lcl = nsharpNative.nsharpLib
                            .mtof(nsharpNative.nsharpLib
                                    .agl(nsharpNative.nsharpLib
                                            .ihght(pcl.lclpres)));

                    str5.setText(String.format("%5.0f", pcl.lclpres),
                            NsharpConstants.color_white);
                    str7.setText(String.format("%7.0f", lcl),
                            NsharpConstants.color_white);
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

                if (pcl.bminus != NsharpNativeConstants.NSHARP_LEGACY_LIB_INVALID_DATA)
                    str9.setText(String.format("%.0f", pcl.bminus),
                            NsharpConstants.color_white);
                else
                    str9.setText("M", NsharpConstants.color_white);
                str9.setCoordinates(firstToken, curY);

                // draw LFC
                str10.setText("LFC(mb)=", NsharpConstants.color_white);
                str10.setCoordinates(secondToken, curY);
                str12.setText("LFC(ft)=", NsharpConstants.color_white);
                str12.setCoordinates(forthToken, curY);
                if (nsharpNative.nsharpLib.qc(pcl.lfcpres) == 1
                        && nsharpNative.nsharpLib.qc(nsharpNative.nsharpLib
                                .mtof(nsharpNative.nsharpLib
                                        .agl(nsharpNative.nsharpLib
                                                .ihght(pcl.lfcpres)))) == 1
                        && nsharpNative.nsharpLib.qc(nsharpNative.nsharpLib
                                .itemp(pcl.lfcpres)) == 1) {
                    float lfc = nsharpNative.nsharpLib
                            .mtof(nsharpNative.nsharpLib
                                    .agl(nsharpNative.nsharpLib
                                            .ihght(pcl.lfcpres)));
                    str11.setText(String.format("%5.0f", pcl.lfcpres),
                            NsharpConstants.color_white);
                    str13.setText(String.format("%7.0f", lfc),
                            NsharpConstants.color_white);
                } else {
                    str11.setText("M", NsharpConstants.color_white);
                    str13.setText("M", NsharpConstants.color_white);
                }
                str11.setCoordinates(thirdToken, curY);
                str13.setCoordinates(fifthToken, curY);
                target.drawStrings(str1, str2, str3, str4, str5, str6, str7,
                        str8, str9, str10, str11, str12, str13);
                curY = curY + charHeight;
            }
        }
        // widthGap = rect.width / 4;
        // firstToken = rect.x + widthGap + aglWidth;
        // secondToken = rect.x + 2 * widthGap;
        // thirdToken = rect.x + 3 * widthGap + aglWidth;

        if (currentParcel == NsharpNativeConstants.PARCELTYPE_USER_DEFINED) {
            layerPressure = NsharpParcelDialog.getUserDefdParcelMb();
        } else
            layerPressure = NsharpNativeConstants.parcelToLayerMap
                    .get(currentParcel);

        // reset and define current parcel
        nsharpNative.nsharpLib.define_parcel(currentParcel, layerPressure);
        lpvls = new _lplvalues();
        nsharpNative.nsharpLib.get_lpvaluesData(lpvls);

        float sfctemp, sfcdwpt, sfcpres;
        sfctemp = lpvls.temp;
        sfcdwpt = lpvls.dwpt;
        sfcpres = lpvls.pres;
        // get parcel data by calling native nsharp parcel() API. value is
        // returned in pcl
        pcl = new _parcel();
        nsharpNative.nsharpLib.parcel(-1.0F, -1.0F, sfcpres, sfctemp, sfcdwpt,
                pcl);

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
        target.drawLine(rect.x, curY, 0.0, rect.x + rect.width, curY, 0.0,
                NsharpConstants.color_white, 1);
        str1.setText("PW =", NsharpConstants.color_white);
        str1.setCoordinates(startX, curY);
        fValue.setValue(0);
        nsharpNative.nsharpLib.precip_water(fValue, -1.0F, -1.0F);
        if (nsharpNative.nsharpLib.qc(fValue.getValue()) == 1) {
            textStr = String.format("%.2fin", fValue.getValue());
        } else {
            textStr = "M";
        }
        str2.setText(textStr, NsharpConstants.color_white);
        str2.setCoordinates(firstToken, curY);
        str3.setText("ConvT =", NsharpConstants.color_white);
        str3.setCoordinates(secondToken, curY);
        fValue.setValue(0);
        float conTempF = nsharpNative.nsharpLib.ctof(nsharpNative.nsharpLib
                .cnvtv_temp(fValue, -1));

        if (nsharpNative.nsharpLib.qc(conTempF) == 1) {
            textStr = String.format("%.0fF", conTempF);
        } else {
            textStr = "M";
        }
        str4.setText(textStr, NsharpConstants.color_white);
        str4.setCoordinates(thirdToken, curY);
        curY = curY + charHeight;
        str5.setText("WBZ =", NsharpConstants.color_white);
        str5.setCoordinates(startX, curY);
        fValue.setValue(0);
        float wbzft = nsharpNative.nsharpLib.mtof(nsharpNative.nsharpLib
                .agl(nsharpNative.nsharpLib.ihght(nsharpNative.nsharpLib
                        .wb_lvl(0, fValue))));
        if (nsharpNative.nsharpLib.qc(wbzft) == 1) {
            textStr = String.format("%.0fft", wbzft);
        } else {
            textStr = "M";
        }
        str6.setText(textStr, NsharpConstants.color_white);
        str6.setCoordinates(firstToken, curY);
        str7.setText("FGZ =", NsharpConstants.color_white);
        str7.setCoordinates(secondToken, curY);
        fValue.setValue(0);
        float fgzft = nsharpNative.nsharpLib.mtof(nsharpNative.nsharpLib
                .agl(nsharpNative.nsharpLib.ihght(nsharpNative.nsharpLib
                        .temp_lvl(0, fValue))));
        if (nsharpNative.nsharpLib.qc(fgzft) == 1) {
            textStr = String.format("%.0fft", fgzft);
        } else {
            textStr = "M";
        }
        str8.setText(textStr, NsharpConstants.color_white);
        str8.setCoordinates(thirdToken, curY);

        target.drawStrings(str1, str2, str3, str4, str5, str6, str7, str8);
        curY = curY + charHeight;
        str1.setText("BRN =", NsharpConstants.color_white);
        str1.setCoordinates(startX, curY);
        if (nsharpNative.nsharpLib.qc(pcl.brn) == 1) {
            textStr = NsharpNativeConstants.STORM_TYPE_BRN_LINE;
            textStr = String.format("%6.0f", pcl.brn);
        } else {
            textStr = "M";
        }
        str2.setText(textStr, NsharpConstants.color_white);
        str2.setCoordinates(firstToken, curY);
        str3.setText("BRN Shr=", NsharpConstants.color_white);
        str3.setCoordinates(secondToken, curY);
        nsharpNative.nsharpLib.get_lpvaluesData(lpvls);
        sfctemp = lpvls.temp;
        sfcdwpt = lpvls.dwpt;
        sfcpres = lpvls.pres;
        nsharpNative.nsharpLib.parcel(-1.0F, -1.0F, sfcpres, sfctemp, sfcdwpt,
                pcl);
        nsharpNative.nsharpLib.cave_bulk_rich2(fValue);
        if (nsharpNative.nsharpLib.qc(fValue.getValue()) == 1)
            textStr = String.format("%.0f m%c/s%c", fValue.getValue(),
                    NsharpConstants.SQUARE_SYMBOL,
                    NsharpConstants.SQUARE_SYMBOL);
        else
            textStr = "M";
        str4.setText(textStr, NsharpConstants.color_white);
        str4.setCoordinates(thirdToken, curY);
        curY = curY + charHeight;
        target.drawLine(rect.x, curY, 0.0, rect.x + rect.width, curY, 0.0,
                NsharpConstants.color_white, 1);
        str5.setText("Bunkers Right=", NsharpConstants.color_white);
        str5.setCoordinates(startX, curY);
        nsharpNative.nsharpLib.bunkers_storm_motion(fValue1, fValue2, fValue3,
                fValue4);
        textStr = String.format("%.0f/%.0f kt", fValue3.getValue(),
                fValue4.getValue());

        str6.setText(textStr, NsharpConstants.color_white);
        str6.setCoordinates(thirdToken, curY);
        curY = curY + charHeight;

        target.drawStrings(str1, str2, str3, str4, str5, str6);

        str1.setText("0-1km Helicity=", NsharpConstants.color_white);
        str1.setCoordinates(startX, curY);
        FloatByReference smdir = new FloatByReference(0), smspd = new FloatByReference(
                0);
        nsharpNative.nsharpLib.get_storm(smspd, smdir);
        float totHeli = nsharpNative.nsharpLib.helicity(0, 1000,
                smdir.getValue(), smspd.getValue(), fValue, fValue1);
        if (nsharpNative.nsharpLib.qc(fValue.getValue()) == 1
                && nsharpNative.nsharpLib.qc(fValue1.getValue()) == 1) {
            textStr = String.format("%.0f m%c/s%c", totHeli,
                    NsharpConstants.SQUARE_SYMBOL,
                    NsharpConstants.SQUARE_SYMBOL);
        } else {
            textStr = "M";
        }
        str2.setText(textStr, NsharpConstants.color_white);
        str2.setCoordinates(thirdToken, curY);
        curY = curY + charHeight;
        str3.setText("0-3km Helicity=", NsharpConstants.color_white);
        str3.setCoordinates(startX, curY);
        totHeli = nsharpNative.nsharpLib.helicity(0, 3000, smdir.getValue(),
                smspd.getValue(), fValue, fValue1);
        if (nsharpNative.nsharpLib.qc(fValue.getValue()) == 1
                && nsharpNative.nsharpLib.qc(fValue1.getValue()) == 1) {
            textStr = String.format("%.0f m%c/s%c", totHeli,
                    NsharpConstants.SQUARE_SYMBOL,
                    NsharpConstants.SQUARE_SYMBOL);
        } else {
            textStr = "M";
        }
        str4.setText(textStr, NsharpConstants.color_white);
        str4.setCoordinates(thirdToken, curY);
        target.drawStrings(str1, str2, str3, str4);
    }

    @SuppressWarnings("deprecation")
    // future (dummy) page
    private void drawPanel12(IGraphicsTarget target, Rectangle rect)
            throws VizException {
        IFont myfont;
        myfont = defaultFont;
        defineCharHeight(myfont);
        myfont.setSmoothing(false);
        myfont.setScaleFont(false);
        extent = new PixelExtent(rect);
        target.setupClippingPlane(extent);
        // if we can not Interpolates a temp with 700 mb pressure, then we dont
        // have enough raw data
        target.drawString(myfont, "               FUTURE PAGE", rect.x
                + rect.width / 2, rect.y + rect.height / 2, 0.0,
                TextStyle.NORMAL, NsharpConstants.color_cyan,
                HorizontalAlignment.RIGHT, VerticalAlignment.TOP, null);
        return;
    }

    // end d2dlite

    public boolean isSumP1Visible() {
        return sumP1Visible;
    }

    public NsharpGenericPaneBackground getDataPanel1Background() {
        return dataPanel1Background;
    }

    public NsharpGenericPaneBackground getDataPanel2Background() {
        return dataPanel2Background;
    }

    public void setPageDisplayOrderNumberArray(
            int[] pageDisplayOrderNumberArray, int numberPagePerDisplay) {
        this.pageDisplayOrderNumberArray = pageDisplayOrderNumberArray;
        if (paneConfigurationName.equals(NsharpConstants.PANE_DEF_CFG_1_STR))
            // This configuration always show 2 pages layout vertically
            this.numberPagePerDisplay = 2;
        else if (paneConfigurationName
                .equals(NsharpConstants.PANE_LITE_D2D_CFG_STR)) // d2dlite
            this.numberPagePerDisplay = 1;
        else
            this.numberPagePerDisplay = numberPagePerDisplay;
        if (initDone) {
            if (this.numberPagePerDisplay == 1)
                defaultFont = font12;
            else
                defaultFont = font10;
            handleResize();
        }

    }

    @Override
    protected void adjustFontSize(float canvasW, float canvasH) {
        /*
         * if(canvasH < myDefaultCanvasHeight/3 || canvasW<
         * myDefaultCanvasWidth/3){ if(myfont!=null){ myfont.dispose(); } myfont
         * = target.initializeFont("Monospace", 8, null); } else if(canvasH <
         * myDefaultCanvasHeight/2 || canvasW< myDefaultCanvasWidth/2){
         * if(myfont!=null){ myfont.dispose(); } myfont =
         * target.initializeFont("Monospace", 8, null); }
         */
    }

    @Override
    public void handleResize() {
        super.handleResize();
        // Chin Note; ext size is its view size Not canvas size
        IExtent ext = getDescriptor().getRenderableDisplay().getExtent();
        ext.reset();
        this.rectangle = new Rectangle((int) ext.getMinX(),
                (int) ext.getMinY(), (int) ext.getWidth(),
                (int) ext.getHeight());
        pe = new PixelExtent(this.rectangle);
        getDescriptor().setNewPe(pe);
        defineCharHeight(defaultFont);
        float prevHeight = dataPaneHeight;
        float prevWidth = dataPaneWidth;
        dp1XOrig = (int) (ext.getMinX());
        dp1YOrig = (int) (ext.getMinY());
        if (paneConfigurationName.equals(NsharpConstants.PANE_DEF_CFG_2_STR)
                || paneConfigurationName
                        .equals(NsharpConstants.PANE_SPCWS_CFG_STR)
                || paneConfigurationName
                        .equals(NsharpConstants.PANE_SIMPLE_D2D_CFG_STR)
                || paneConfigurationName
                        .equals(NsharpConstants.PANE_LITE_D2D_CFG_STR)) {
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
        } else if (paneConfigurationName
                .equals(NsharpConstants.PANE_DEF_CFG_1_STR)) {
            // this configuration lays 2 data panels top/down
            // always display 2 pages
            dataPaneWidth = (int) ext.getWidth();
            dataPaneHeight = (int) ext.getHeight() / 2;
            dp2XOrig = dp1XOrig;
            dp2YOrig = dp1YOrig + dataPaneHeight;
        }

        xRatio = xRatio * dataPaneWidth / prevWidth;
        xRatio = 1; // turn off
        yRatio = yRatio * dataPaneHeight / prevHeight;
        yRatio = 1;// turn off
        charHeight = (int) (charHeight * yRatio);

        Rectangle rectangle = new Rectangle(dp1XOrig, dp1YOrig, dataPaneWidth,
                dataPaneHeight);
        dataPanel1Background.handleResize(rectangle);
        rectangle = new Rectangle(dp2XOrig, dp2YOrig, dataPaneWidth,
                dataPaneHeight);
        dataPanel2Background.handleResize(rectangle);
        panelRectArray[0] = dataPanel1Background.getRectangle();
        panelRectArray[1] = dataPanel2Background.getRectangle();
        // System.out.println("Data: handle resize w="+dataPaneWidth+ " h="+
        // dataPaneHeight);

    }

    @Override
    public void handleZooming() {
        magnifyFont(currentZoomLevel);
    }
}
