package gov.noaa.nws.ncep.ui.nsharp.display.rsc;


import gov.noaa.nws.ncep.ui.nsharp.NsharpConstants;
import gov.noaa.nws.ncep.ui.nsharp.NsharpOperationElement;
import gov.noaa.nws.ncep.ui.nsharp.NsharpSoundingElementStateProperty;
import gov.noaa.nws.ncep.ui.nsharp.NsharpTimeOperationElement;
import gov.noaa.nws.ncep.ui.nsharp.display.NsharpAbstractPaneDescriptor;

import java.awt.geom.Rectangle2D;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;

import com.raytheon.uf.viz.core.DrawableString;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.HorizontalAlignment;
import com.raytheon.uf.viz.core.IGraphicsTarget.VerticalAlignment;
import com.raytheon.uf.viz.core.PixelExtent;
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
 * Date          Ticket#  Engineer   Description
 * ------------- -------- ---------- -------------------------------------------
 * Apr 23, 2012  229      Chin Chen  Initial coding
 * Feb 03, 2015  17079    Chin Chen  Soundings listed out of order if frames go
 *                                   into new month
 * Feb 23, 2015  5694     Chin Chen  add  code to support previous time line
 *                                   format
 * Nov 05, 2018  6800     bsteffen   Eliminate some duplicate fields from the
 *                                   resource handler.
 * Dec 14, 2018  6872     bsteffen   Display more precision when needed
 * 
 * </pre>
 * 
 * @author Chin Chen
 */
public class NsharpTimeStnPaneResource extends NsharpAbstractPaneResource {
    private Rectangle timeLineRectangle;

    private Rectangle stnIdRectangle;

    private Rectangle sndRectangle;

    private Rectangle colorNoteRectangle;

    private List<NsharpOperationElement> stnElemList;

    private List<NsharpTimeOperationElement> timeElemList;

    private List<NsharpOperationElement> sndElemList;

    private List<List<List<NsharpSoundingElementStateProperty>>> stnTimeSndTable;

    private int curTimeLineIndex = 0;

    private int curStnIndex = 0;

    private int curSndIndex = 0;

    private int curTimeLinePage = 1;

    private int curStnIdPage = 1;

    private int curSndPage = 1;

    private int totalTimeLinePage = 1;

    private int totalStnIdPage = 1;

    private int totalSndPage = 1;

    private int numLineToShowPerPage = 1;

    private int paneHeight = NsharpConstants.TIMESTN_PANE_REC_HEIGHT;

    private int dtXOrig = NsharpConstants.DATA_TIMELINE_X_ORIG;

    private int dtYOrig = NsharpConstants.DATA_TIMELINE_Y_ORIG;

    private int dtWidth = NsharpConstants.DATA_TIMELINE_WIDTH;

    private int dtHeight = NsharpConstants.DATA_TIMELINE_HEIGHT;

    private int dtNextPageEnd = NsharpConstants.DATA_TIMELINE_NEXT_PAGE_END_;

    private int stnXOrig = NsharpConstants.STATION_ID_X_ORIG;

    private int stnYOrig = NsharpConstants.STATION_ID_Y_ORIG;

    private int stnWidth = NsharpConstants.STATION_ID_WIDTH;

    private int stnHeight = NsharpConstants.STATION_ID_HEIGHT;

    private int sndXOrig = NsharpConstants.SND_TYPE_X_ORIG;

    private int sndYOrig = NsharpConstants.SND_TYPE_Y_ORIG;

    private int sndWidth = NsharpConstants.SND_TYPE_WIDTH;

    private int sndHeight = NsharpConstants.SND_TYPE_HEIGHT;

    private int cnXOrig = NsharpConstants.COLOR_NOTATION_X_ORIG;

    private int cnYOrig = NsharpConstants.COLOR_NOTATION_Y_ORIG;

    private int cnWidth = NsharpConstants.COLOR_NOTATION_WIDTH;

    private int cnHeight = NsharpConstants.COLOR_NOTATION_HEIGHT;

    private float xRatio = 1;

    private float yRatio = 1;

    private String sndTypeStr = "NA";

    private String timelineStr = "NA";

    private String stationStr = "NA";

    private boolean compareStnIsOn;

    private boolean compareTmIsOn;

    private boolean compareSndIsOn;

    public NsharpTimeStnPaneResource(AbstractResourceData resourceData,
            LoadProperties loadProperties, NsharpAbstractPaneDescriptor desc) {
        super(resourceData, loadProperties, desc);

        timeLineRectangle = new Rectangle(dtXOrig, dtYOrig, dtWidth, dtHeight);
        stnIdRectangle = new Rectangle(stnXOrig, stnYOrig, stnWidth, stnHeight);
        sndRectangle = new Rectangle(sndXOrig, sndYOrig, sndWidth, sndHeight);
        colorNoteRectangle = new Rectangle(cnXOrig, cnYOrig, cnWidth, cnHeight);
    }

    private void drawNsharpColorNotation(IGraphicsTarget target, Rectangle rect)
            throws VizException {

        PixelExtent extent = new PixelExtent(rect);
        RGB color;
        target.setupClippingPlane(extent);
        target.drawRect(extent, NsharpConstants.backgroundColor, 1.0f, 1.0f);
        // plot notations:
        if (dtHeight >= paneHeight)
            return;
        // draw time line page title etc. only when pane box height is larger
        // than timeline box height
        double x = cnXOrig + 5 * xRatio;
        double y = cnYOrig + 1.5 * charHeight;

        color = NsharpConstants.color_white;
        DrawableString str = new DrawableString("Line State:", color);
        str.font = font9;
        str.setCoordinates(x, y);
        double horizRatio = paintProps.getView().getExtent().getWidth()
                / paintProps.getCanvasBounds().width;
        x = x + target.getStringsBounds(str).getWidth() * horizRatio;

        color = NsharpConstants.color_green;
        DrawableString str1 = new DrawableString("Current", color);
        str1.setCoordinates(x, y);
        str1.font = font9;
        x = x + target.getStringsBounds(str1).getWidth() * horizRatio * 1.1;
        color = NsharpConstants.color_yellow;
        DrawableString str2 = new DrawableString("Active", color);
        str2.setCoordinates(x, y);
        str2.font = font9;
        x = x + target.getStringsBounds(str2).getWidth() * horizRatio * 1.1;

        color = NsharpConstants.color_white;
        DrawableString str3 = new DrawableString("InActive", color);
        str3.setCoordinates(x, y);
        str3.font = font9;

        x = cnXOrig + 5 * xRatio;
        y = y + charHeight * 1.3;
        color = NsharpConstants.color_white;
        DrawableString str4 = new DrawableString("Load Status:", color);
        str4.setCoordinates(x, y);
        str4.font = font9;
        x = x + target.getStringsBounds(str4).getWidth() * horizRatio * 1.1;
        color = NsharpConstants.color_red;
        DrawableString str5 = new DrawableString("* :Loaded", color);
        str5.setCoordinates(x, y);
        str5.font = font9;
        x = x + target.getStringsBounds(str5).getWidth() * horizRatio * 1.1;
        color = NsharpConstants.color_purple;
        DrawableString str6 = new DrawableString("* :UnLoaded", color);
        str6.setCoordinates(x, y);
        str6.font = font9;
        target.drawStrings(str, str1, str2, str3, str4, str5, str6);

        if (compareStnIsOn || compareSndIsOn || compareTmIsOn) {
            x = cnXOrig + 5 * xRatio;
            y = y + charHeight * 1.3;
            color = NsharpConstants.color_white;
            String baseStr = "";
            String cursndType, stnId, tl;
            if (curTimeLineIndex < 0) {
                tl = timelineStr;
            } else {
                DateTimeFormatter formatter = NsharpTimeOperationElement
                        .getDisplayFormatter(timeElemList);
                timelineStr = tl = timeElemList.get(curTimeLineIndex)
                        .getDisplayText(formatter);
            }
            if (curSndIndex < 0) {
                cursndType = sndTypeStr;
            } else {
                sndTypeStr = cursndType = sndElemList.get(curSndIndex)
                        .getDescription();
            }
            if (curStnIndex < 0) {
                stnId = stationStr;
            } else {
                stationStr = stnId = stnElemList.get(curStnIndex)
                        .getDescription();
            }
            if (compareStnIsOn)
                baseStr = "Comp Stn Based on: " + tl + "-" + cursndType;
            else if (compareSndIsOn)
                baseStr = "Comp Src Based on: " + tl + "-" + stnId;
            else
                baseStr = "Comp Tm Based on: " + cursndType + "-" + stnId;

            DrawableString baseDrawStr = new DrawableString(baseStr, color);
            baseDrawStr.setCoordinates(x, y);
            baseDrawStr.font = font9;
            target.drawStrings(baseDrawStr);
        }
        target.clearClippingPlane();

    }

    private void drawNsharpTimelinBox(IGraphicsTarget target, Rectangle rect)
            throws VizException {
        PixelExtent extent = new PixelExtent(rect);
        target.setupClippingPlane(extent);
        target.drawRect(extent, NsharpConstants.backgroundColor, 1.0f, 1.0f);
        double x;
        double y;
        String s;
        if (dtHeight < paneHeight) {
            // draw time line page title etc. only when pane box height is
            // larger than timeline box height
            // start d2dlite

            x = dtXOrig;
            y = dtYOrig - 1.5 * charHeight * yRatio;
            s = timeElemList.size() + " times,p" + curTimeLinePage + "/"
                    + totalTimeLinePage;
            target.drawString(font9, s, x, y, 0.0,
                    IGraphicsTarget.TextStyle.NORMAL,
                    NsharpConstants.color_green, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
            y = dtYOrig;
            PixelExtent boxExt = new PixelExtent(dtXOrig, dtXOrig + dtWidth,
                    dtYOrig + 2 * charHeight * yRatio, dtYOrig);
            // blank out box, should draw this first and then draw data on top
            // of it
            target.drawShadedRect(boxExt, NsharpConstants.color_lightblue, 1f,
                    null);
            PixelExtent boxExt1 = new PixelExtent(dtXOrig, dtXOrig + dtWidth
                    / 2, dtYOrig + 2 * charHeight * yRatio, dtYOrig);
            target.drawRect(boxExt1, NsharpConstants.color_red, 4, 1);
            PixelExtent boxExt2 = new PixelExtent(dtXOrig + dtWidth / 2,
                    dtXOrig + dtWidth, dtYOrig + 2 * charHeight * yRatio,
                    dtYOrig);
            target.drawRect(boxExt2, NsharpConstants.color_red, 4, 1);
            x = dtXOrig + 5;
            y = y + 2 * charHeight * yRatio;

            s = " NxP      ";
            target.drawString(font12, s, x, y, 0.0,
                    IGraphicsTarget.TextStyle.DROP_SHADOW,
                    NsharpConstants.color_black, HorizontalAlignment.LEFT,
                    VerticalAlignment.BOTTOM, null);
            x = dtXOrig + dtWidth / 2 + 5;
            s = " PvP     ";
            target.drawString(font12, s, x, y, 0.0,
                    IGraphicsTarget.TextStyle.DROP_SHADOW,
                    NsharpConstants.color_black, HorizontalAlignment.LEFT,
                    VerticalAlignment.BOTTOM, null);

        }
        int startIndex = (curTimeLinePage - 1) * numLineToShowPerPage;
        if (startIndex < 0)
            startIndex = 0;
        if (timeElemList != null) {
            DateTimeFormatter formatter = NsharpTimeOperationElement.getDisplayFormatter(timeElemList);
            int colorIndex;
            double ly = dtNextPageEnd + charHeight;
            RGB color;
            double hRatio = paintProps.getView().getExtent().getWidth()
                    / paintProps.getCanvasBounds().width;
            Rectangle2D strBD = target.getStringBounds(font9, "*");
            double xGap = 2 * strBD.getWidth() * hRatio;
            for (int j = startIndex, count = 0; j < timeElemList.size()
                    && count < numLineToShowPerPage; j++, count++) {
                x = dtXOrig + 5;
                boolean avail = false;
                NsharpTimeOperationElement elm = timeElemList.get(j);
                NsharpConstants.ActState sta = elm.getActionState();

                if (sta == NsharpConstants.ActState.ACTIVE
                        && j == curTimeLineIndex)
                    sta = NsharpConstants.ActState.CURRENT;
                if (curStnIndex >= 0 && curSndIndex >= 0) {
                    s = "*";
                    if (stnTimeSndTable.get(curStnIndex).get(j)
                            .get(curSndIndex) != null) {
                        avail = true;
                    }
                    if (avail) {
                        color = NsharpConstants.color_red;
                        if (sta == NsharpConstants.ActState.CURRENT)
                            color = NsharpConstants.color_green;
                    } else {
                        color = NsharpConstants.color_purple;
                    }

                    target.drawString(font9, s, x, ly, 0.0,
                            IGraphicsTarget.TextStyle.NORMAL, color,
                            HorizontalAlignment.LEFT, VerticalAlignment.BOTTOM,
                            null);
                }

                x = x + xGap;
                RGB tmLnColor = rscHandler.getElementColorMap().get(sta.name());
                String tmDesStr = elm.getDisplayText(formatter);
                double tmX = x;

                if (compareTmIsOn
                        && elm.getActionState() == NsharpConstants.ActState.ACTIVE
                        && avail) {
                    colorIndex = stnTimeSndTable.get(curStnIndex).get(j)
                            .get(curSndIndex).getCompColorIndex();
                    strBD = target.getStringBounds(font9, tmDesStr);
                    String colorIndexStr = ""
                            + (colorIndex % NsharpConstants.LINE_COMP1 + 1);// compIndex;
                    x = x + strBD.getWidth() * hRatio + 5;
                    target.drawString(
                            font9,
                            colorIndexStr,
                            x,
                            ly,
                            0.0,
                            IGraphicsTarget.TextStyle.NORMAL,
                            linePropertyMap.get(
                                    NsharpConstants.lineNameArray[colorIndex])
                                    .getLineColor(), HorizontalAlignment.LEFT,
                            VerticalAlignment.BOTTOM, null);
                    tmLnColor = linePropertyMap.get(
                            NsharpConstants.lineNameArray[colorIndex])
                            .getLineColor();
                } else if ((compareStnIsOn || compareSndIsOn)
                        && elm.getActionState() == NsharpConstants.ActState.ACTIVE
                        && timeElemList.indexOf(elm) == curTimeLineIndex
                        && avail) {
                    colorIndex = stnTimeSndTable.get(curStnIndex)
                            .get(curTimeLineIndex).get(curSndIndex)
                            .getCompColorIndex();
                    strBD = target.getStringBounds(font9, tmDesStr);
                    String colorIndexStr = ""
                            + (colorIndex % NsharpConstants.LINE_COMP1 + 1);// compIndex;
                    x = x + strBD.getWidth() * hRatio + 5;
                    target.drawString(
                            font9,
                            colorIndexStr,
                            x,
                            ly,
                            0.0,
                            IGraphicsTarget.TextStyle.NORMAL,
                            linePropertyMap.get(
                                    NsharpConstants.lineNameArray[colorIndex])
                                    .getLineColor(), HorizontalAlignment.LEFT,
                            VerticalAlignment.BOTTOM, null);
                    tmLnColor = linePropertyMap.get(
                            NsharpConstants.lineNameArray[colorIndex])
                            .getLineColor();
                }
                target.drawString(font9, tmDesStr, tmX, ly, 0.0,
                        IGraphicsTarget.TextStyle.NORMAL, tmLnColor,
                        HorizontalAlignment.LEFT, VerticalAlignment.BOTTOM,
                        null);
                ly = ly + lineHeight;
                if (ly >= cnYOrig)
                    // we dont show time line that extends below time line box
                    break;
            }
        }
    }

    private void drawNsharpStationIdBox(IGraphicsTarget target, Rectangle rect)
            throws VizException {
        PixelExtent extent = new PixelExtent(rect);
        target.setupClippingPlane(extent);
        target.drawRect(extent, NsharpConstants.backgroundColor, 1.0f, 1.0f);
        double x;
        double y;
        String s;
        if (dtHeight < paneHeight) {
            // draw time line page title etc. only when pane box height is
            // larger than timeline box height

            x = stnXOrig;
            y = stnYOrig - 1.5 * charHeight * yRatio;
            s = stnElemList.size() + "stn,p" + curStnIdPage + "/"
                    + totalStnIdPage;
            target.drawString(font9, s, x, y, 0.0,
                    IGraphicsTarget.TextStyle.NORMAL,
                    NsharpConstants.color_green, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
            y = dtYOrig;
            PixelExtent boxExt = new PixelExtent(stnXOrig, stnXOrig + stnWidth,
                    stnYOrig + 2 * charHeight * yRatio, stnYOrig);
            // blank out box, should draw this first and then draw data on top
            // of it
            target.drawShadedRect(boxExt, NsharpConstants.color_lightblue, 1f,
                    null);
            PixelExtent boxExt1 = new PixelExtent(stnXOrig, stnXOrig + stnWidth
                    / 2, stnYOrig + 2 * charHeight * yRatio, stnYOrig);
            target.drawRect(boxExt1, NsharpConstants.color_red, 4, 1);
            PixelExtent boxExt2 = new PixelExtent(stnXOrig + stnWidth / 2,
                    stnXOrig + stnWidth, stnYOrig + 2 * charHeight * yRatio,
                    stnYOrig);
            target.drawRect(boxExt2, NsharpConstants.color_red, 4, 1);

            x = stnXOrig + 5;
            y = y + 2 * charHeight * yRatio;
            s = "NxP  ";
            target.drawString(font12, s, x, y, 0.0,
                    IGraphicsTarget.TextStyle.DROP_SHADOW,
                    NsharpConstants.color_black, HorizontalAlignment.LEFT,
                    VerticalAlignment.BOTTOM, null);
            x = stnXOrig + stnWidth / 2 + 5;
            s = "PvP  ";
            target.drawString(font12, s, x, y, 0.0,
                    IGraphicsTarget.TextStyle.DROP_SHADOW,
                    NsharpConstants.color_black, HorizontalAlignment.LEFT,
                    VerticalAlignment.BOTTOM, null);

        }

        int startIndex = (curStnIdPage - 1)
                * numLineToShowPerPage;
        if (startIndex < 0)
            startIndex = 0;
        int colorIndex;
        double ly = dtNextPageEnd + charHeight;
        RGB color;
        Rectangle2D strBD = target.getStringBounds(font9, "*");
        double hRatio = paintProps.getView().getExtent().getWidth()
                / paintProps.getCanvasBounds().width;
        double xGap = 2 * strBD.getWidth() * hRatio;
        for (int j = startIndex, count = 0; j < stnElemList.size()
                && count < numLineToShowPerPage; j++, count++) {
            boolean avail = false;
            x = stnXOrig + 5;
            NsharpOperationElement elm = stnElemList.get(j);
            NsharpConstants.ActState sta = elm.getActionState();
            if (sta == NsharpConstants.ActState.ACTIVE && j == curStnIndex)
                sta = NsharpConstants.ActState.CURRENT;

            if (curTimeLineIndex >= 0 && curSndIndex >= 0) {
                if (stnTimeSndTable.get(j).get(curTimeLineIndex)
                        .get(curSndIndex) != null) {
                    avail = true;
                }
                s = "*";
                if (avail) {
                    color = NsharpConstants.color_red;
                    if (sta == NsharpConstants.ActState.CURRENT)
                        color = NsharpConstants.color_green;
                } else {
                    color = NsharpConstants.color_purple;
                }
                target.drawString(font9, s, x, ly, 0.0,
                        IGraphicsTarget.TextStyle.NORMAL, color,
                        HorizontalAlignment.LEFT, VerticalAlignment.BOTTOM,
                        null);
            }
            x = x + xGap;
            String stnId = elm.getDescription();
            double stnIdX = x;
            color = rscHandler.getElementColorMap().get(sta.name());

            if (elm.getActionState() == NsharpConstants.ActState.ACTIVE
                    && avail) {
                if (compareStnIsOn) {
                    strBD = target.getStringBounds(font9, stnId);

                    colorIndex = stnTimeSndTable.get(j).get(curTimeLineIndex)
                            .get(curSndIndex).getCompColorIndex();
                    color = linePropertyMap.get(
                            NsharpConstants.lineNameArray[colorIndex])
                            .getLineColor();
                    s = "" + (colorIndex % NsharpConstants.LINE_COMP1 + 1);
                    x = x + strBD.getWidth() * hRatio + 5;
                    target.drawString(font9, s, x, ly, 0.0,
                            IGraphicsTarget.TextStyle.NORMAL, color,
                            HorizontalAlignment.LEFT, VerticalAlignment.BOTTOM,
                            null);
                } else if ((compareTmIsOn || compareSndIsOn)
                        && j == curStnIndex) {
                    strBD = target.getStringBounds(font9, stnId);

                    colorIndex = stnTimeSndTable.get(curStnIndex)
                            .get(curTimeLineIndex).get(curSndIndex)
                            .getCompColorIndex();
                    color = linePropertyMap.get(
                            NsharpConstants.lineNameArray[colorIndex])
                            .getLineColor();
                    s = "" + (colorIndex % NsharpConstants.LINE_COMP1 + 1);
                    x = x + strBD.getWidth() * hRatio + 5;
                    target.drawString(font9, s, x, ly, 0.0,
                            IGraphicsTarget.TextStyle.NORMAL, color,
                            HorizontalAlignment.LEFT, VerticalAlignment.BOTTOM,
                            null);
                }
            }
            target.drawString(font9, stnId, stnIdX, ly, 0.0,
                    IGraphicsTarget.TextStyle.NORMAL, color,
                    HorizontalAlignment.LEFT, VerticalAlignment.BOTTOM, null);
            ly = ly + lineHeight;
            if (ly >= cnYOrig)
                // we dont show stn id that extends below box
                break;
        }
    }

    private void drawNsharpSndTypeBox(IGraphicsTarget target, Rectangle rect)
            throws VizException {
        PixelExtent extent = new PixelExtent(rect);
        target.setupClippingPlane(extent);
        target.drawRect(extent, NsharpConstants.backgroundColor, 1.0f, 1.0f);
        double x;
        double y;
        String s;
        if (dtHeight < paneHeight) {
            // draw time line page title etc. only when pane box height is
            // larger than timeline box height

            x = sndXOrig;
            y = sndYOrig - 1.5 * charHeight * yRatio;
            s = sndElemList.size() + " src,p" + curSndPage + "/" + totalSndPage;
            target.drawString(font9, s, x, y, 0.0,
                    IGraphicsTarget.TextStyle.NORMAL,
                    NsharpConstants.color_green, HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP, null);
            y = sndYOrig;
            PixelExtent boxExt = new PixelExtent(sndXOrig, sndXOrig + sndWidth,
                    sndYOrig + 2 * charHeight * yRatio, sndYOrig);
            // blank out box, should draw this first and then draw data on top
            // of it
            target.drawShadedRect(boxExt, NsharpConstants.color_lightblue, 1f,
                    null);
            PixelExtent boxExt1 = new PixelExtent(sndXOrig, sndXOrig + sndWidth
                    / 2, sndYOrig + 2 * charHeight * yRatio, sndYOrig);
            target.drawRect(boxExt1, NsharpConstants.color_red, 4, 1);
            PixelExtent boxExt2 = new PixelExtent(sndXOrig + sndWidth / 2,
                    sndXOrig + sndWidth, sndYOrig + 2 * charHeight * yRatio,
                    sndYOrig);
            target.drawRect(boxExt2, NsharpConstants.color_red, 4, 1);

            x = sndXOrig + 5;
            y = y + 2 * charHeight * yRatio;
            s = " NxP    ";
            target.drawString(font12, s, x, y, 0.0,
                    IGraphicsTarget.TextStyle.DROP_SHADOW,
                    NsharpConstants.color_black, HorizontalAlignment.LEFT,
                    VerticalAlignment.BOTTOM, null);

            x = sndXOrig + sndWidth / 2 + 5;
            s = " PvP     ";
            target.drawString(font12, s, x, y, 0.0,
                    IGraphicsTarget.TextStyle.DROP_SHADOW,
                    NsharpConstants.color_black, HorizontalAlignment.LEFT,
                    VerticalAlignment.BOTTOM, null);

        }

        int startIndex = (curSndPage - 1)
                * numLineToShowPerPage;
        if (startIndex < 0)
            startIndex = 0;
        int colorIndex;
        double ly = dtNextPageEnd + charHeight;
        RGB color;
        Rectangle2D strBD = target.getStringBounds(font9, "*");
        double hRatio = paintProps.getView().getExtent().getWidth()
                / paintProps.getCanvasBounds().width;
        double xGap = 2 * strBD.getWidth() * hRatio;
        for (int j = startIndex, count = 0; j < sndElemList.size()
                && count < numLineToShowPerPage; j++, count++) {
            boolean avail = false;
            x = sndXOrig + 5;
            NsharpOperationElement elm = sndElemList.get(j);
            NsharpConstants.ActState sta = elm.getActionState();
            if (sta == NsharpConstants.ActState.ACTIVE && j == curSndIndex)
                sta = NsharpConstants.ActState.CURRENT;

            if (curTimeLineIndex >= 0 && curStnIndex >= 0) {
                if (stnTimeSndTable.get(curStnIndex).get(curTimeLineIndex)
                        .get(j) != null) {
                    avail = true;
                }
                s = "*";
                if (avail) {
                    color = NsharpConstants.color_red;
                    if (sta == NsharpConstants.ActState.CURRENT)
                        color = NsharpConstants.color_green;
                } else {
                    color = NsharpConstants.color_purple;
                }
                target.drawString(font9, s, x, ly, 0.0,
                        IGraphicsTarget.TextStyle.NORMAL, color,
                        HorizontalAlignment.LEFT, VerticalAlignment.BOTTOM,
                        null);
            }
            x = x + xGap;
            String sndType = elm.getDescription();
            double sndX = x;
            color = rscHandler.getElementColorMap().get(sta.name());

            if (elm.getActionState() == NsharpConstants.ActState.ACTIVE) {
                if (compareSndIsOn) {
                    strBD = target.getStringBounds(font9, sndType);

                    List<NsharpResourceHandler.CompSndSelectedElem> sndCompElementList = rscHandler
                            .getCompSndSelectedElemList();
                    s = "";
                    for (NsharpResourceHandler.CompSndSelectedElem compElem : sndCompElementList) {
                        if (compElem.getSndIndex() != j)
                            continue;
                        if (stnTimeSndTable.get(compElem.getStnIndex())
                                .get(compElem.getTimeIndex())
                                .get(compElem.getSndIndex()) == null)
                            continue;
                        colorIndex = stnTimeSndTable
                                .get(compElem.getStnIndex())
                                .get(compElem.getTimeIndex())
                                .get(compElem.getSndIndex())
                                .getCompColorIndex();
                        color = linePropertyMap.get(
                                NsharpConstants.lineNameArray[colorIndex])
                                .getLineColor();
                        s = s + (colorIndex % NsharpConstants.LINE_COMP1 + 1)
                                + ",";
                    }
                    if (s.length() > 0)
                        s = s.substring(0, s.length() - 1);
                    x = x + strBD.getWidth() * hRatio + 5;
                    target.drawString(font9, s, x, ly, 0.0,
                            IGraphicsTarget.TextStyle.NORMAL, color,
                            HorizontalAlignment.LEFT, VerticalAlignment.BOTTOM,
                            null);

                } else if ((compareTmIsOn || compareStnIsOn)
                        && j == curSndIndex && avail) {
                    strBD = target.getStringBounds(font9, sndType);

                    colorIndex = stnTimeSndTable.get(curStnIndex)
                            .get(curTimeLineIndex).get(curSndIndex)
                            .getCompColorIndex();
                    color = linePropertyMap.get(
                            NsharpConstants.lineNameArray[colorIndex])
                            .getLineColor();
                    s = "" + (colorIndex % NsharpConstants.LINE_COMP1 + 1);
                    x = x + strBD.getWidth() * hRatio + 5;
                    target.drawString(font9, s, x, ly, 0.0,
                            IGraphicsTarget.TextStyle.NORMAL, color,
                            HorizontalAlignment.LEFT, VerticalAlignment.BOTTOM,
                            null);
                }
            }
            target.drawString(font9, sndType, sndX, ly, 0.0,
                    IGraphicsTarget.TextStyle.NORMAL, color,
                    HorizontalAlignment.LEFT, VerticalAlignment.BOTTOM, null);
            ly = ly + lineHeight;
            if (ly >= cnYOrig)
                // we dont show stn id that extends below box
                break;
        }
    }

    @Override
    protected void paintInternal(IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {
        super.paintInternal(target, paintProps);
        if (rscHandler == null)
            return;
        stnElemList = rscHandler.getStnElementList();
        timeElemList = rscHandler.getTimeElementList();
        sndElemList = rscHandler.getSndElementList();
        curTimeLineIndex = rscHandler.getCurrentTimeElementListIndex();
        curStnIndex = rscHandler.getCurrentStnElementListIndex();
        curSndIndex = rscHandler.getCurrentSndElementListIndex();
        calculateTimeStnBoxData();
        compareStnIsOn = rscHandler.isCompareStnIsOn();
        compareSndIsOn = rscHandler.isCompareSndIsOn();
        compareTmIsOn = rscHandler.isCompareTmIsOn();
        stnTimeSndTable = rscHandler.getStnTimeSndTable();
        this.font10.setSmoothing(false);
        this.font10.setScaleFont(false);
        this.font9.setSmoothing(false);
        this.font9.setScaleFont(false);
        this.font12.setSmoothing(false);
        this.font12.setScaleFont(false);

        // plot data time line
        drawNsharpTimelinBox(target, timeLineRectangle);

        // plot station id
        drawNsharpStationIdBox(target, stnIdRectangle);

        drawNsharpSndTypeBox(target, sndRectangle);
        // plot color notations
        drawNsharpColorNotation(target, colorNoteRectangle);

    }
    
    private void calculateTimeStnBoxData() {
        totalTimeLinePage = timeElemList.size() / numLineToShowPerPage;
        if (timeElemList.size() % numLineToShowPerPage != 0)
            totalTimeLinePage = totalTimeLinePage + 1;
        curTimeLinePage = curTimeLineIndex / numLineToShowPerPage + 1;
        
        totalStnIdPage = stnElemList.size() / numLineToShowPerPage;
        if (stnElemList.size() % numLineToShowPerPage != 0)
            totalStnIdPage++;
        curStnIdPage = curStnIndex / numLineToShowPerPage + 1;
        
        totalSndPage = sndElemList.size() / numLineToShowPerPage;
        if (sndElemList.size() % numLineToShowPerPage != 0)
            totalSndPage++;
        curSndPage = curSndIndex / numLineToShowPerPage + 1;
    }

    public Rectangle getTimeLineRectangle() {
        return timeLineRectangle;
    }

    public Rectangle getStnIdRectangle() {
        return stnIdRectangle;
    }

    public Rectangle getSndRectangle() {
        return sndRectangle;
    }

    public Rectangle getColorNoteRectangle() {
        return colorNoteRectangle;
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
        defineCharHeight(font12);
        paneHeight = (int) ext.getHeight();
        int paneWidth = (int) (ext.getWidth());
        xRatio = 1;
        yRatio = 1;
        // if pane height is less than 10 char height, then just plot time line.
        // not plot "messages/title/notations" etc..
        if (paneHeight > (int) (10 * charHeight * yRatio)) {
            dtYOrig = (int) ext.getMinY() + (int) (2 * charHeight * yRatio);
            cnHeight = (int) (4 * charHeight * yRatio);
            dtHeight = paneHeight - (int) (6 * charHeight * yRatio);
            dtNextPageEnd = dtYOrig + (int) (3 * charHeight * yRatio);
        } else {
            dtYOrig = (int) (ext.getMinY());
            cnHeight = 0;
            dtHeight = paneHeight;
            dtNextPageEnd = dtYOrig;
        }
        dtXOrig = (int) (ext.getMinX());
        dtWidth = paneWidth * 40 / 100;
        int dtXEnd = dtXOrig + dtWidth;
        stnXOrig = dtXEnd;
        stnYOrig = dtYOrig;
        stnWidth = paneWidth * 25 / 100;
        int stnXEnd = stnXOrig + stnWidth;
        sndXOrig = stnXEnd;
        sndYOrig = stnYOrig;
        sndWidth = paneWidth * 35 / 100;
        stnHeight = dtHeight;
        cnXOrig = dtXOrig;
        cnYOrig = dtYOrig + dtHeight;
        cnWidth = paneWidth;

        timeLineRectangle = new Rectangle(dtXOrig, (int) ext.getMinY(),
                dtWidth, paneHeight - cnHeight);
        stnIdRectangle = new Rectangle(stnXOrig, (int) ext.getMinY(), stnWidth,
                paneHeight - cnHeight);
        sndRectangle = new Rectangle(sndXOrig, (int) ext.getMinY(), sndWidth,
                paneHeight - cnHeight);
        colorNoteRectangle = new Rectangle(cnXOrig, cnYOrig, cnWidth, cnHeight);

        numLineToShowPerPage = (cnYOrig - dtNextPageEnd) / (int) lineHeight - 1;
        if (numLineToShowPerPage < 1) {
            numLineToShowPerPage = dtHeight / (int) lineHeight - 1;
            if (numLineToShowPerPage < 1)
                numLineToShowPerPage = 1;
        }
    }
    
    public int getUserClickedSndIndex(Coordinate c){
        if (c.y < dtNextPageEnd) {
            // change to next/previous page
            if (totalSndPage == 1)
                return -1;
            if ((c.x - dtXOrig) < (dtWidth / 2)) {
                curSndPage++;
                if (curSndPage > totalSndPage) {
                    curSndPage = 1;
                }
            } else {
                curSndPage--;
                if (curSndPage <= 0) {
                    curSndPage = totalSndPage;
                }
            }
            return -1;
        }
        double dIndex = (c.y - dtNextPageEnd) / lineHeight
                + (curSndPage - 1) * numLineToShowPerPage;
        return (int) dIndex;
    }
    
    public int getUserClickedTimeIndex(Coordinate c){
        if (c.y < dtNextPageEnd) {
            // change to next/previous page
            if (totalTimeLinePage == 1) {
                return -1;
            }
            if ((c.x - dtXOrig) < (dtWidth / 2)) {
                curTimeLinePage++;
                if (curTimeLinePage > totalTimeLinePage) {
                    curTimeLinePage = 1;
                }
            } else {
                curTimeLinePage--;
                if (curTimeLinePage <= 0) {
                    curTimeLinePage = totalTimeLinePage;
                }
            }
            return -1;
        }
        double dIndex = (c.y - dtNextPageEnd) / lineHeight
                + (curTimeLinePage - 1) * numLineToShowPerPage;
        return (int) dIndex;
    }
    
    public int getUserClickedStationIdIndex(Coordinate c){
        if (c.y < dtNextPageEnd) {// d2dlite
            // change to next/previous page
            if (totalStnIdPage == 1)
                return -1;
            if ((c.x - (dtXOrig + dtWidth)) < (dtWidth / 2)) {
                curStnIdPage++;
                if (curStnIdPage > totalStnIdPage)
                    curStnIdPage = 1;
            } else {
                curStnIdPage--;
                if (curStnIdPage <= 0)
                    curStnIdPage = totalStnIdPage;
            }
            return -1;
        }
        double dIndex = ((c.y - dtNextPageEnd)) / lineHeight
                + (curStnIdPage - 1) * numLineToShowPerPage;

        return (int) dIndex;
    }
}
