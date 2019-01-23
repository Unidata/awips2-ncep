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
 * 07/10/2012	229			Chin Chen	Initial coding + EBS 
 * 10/01/2012               Chin Chen   Add STP Stats
 * 05/23/2014               Chin Chen   update SHIP, STP Stats based on bigsharp version 2013Jun12
 * 08/18/2014               Chin Chen   implemented SARS, FIRE, HAIL, WINTER SPC graphs based on 
 *                                      bigsharp version 2013Jun12
 * 12/03/2014   DR#16884    Chin Chen   fixed issue, NSHARP crashes if user loops a product and 
 *                                      then clicks WINTER/FIRE buttons in Toolbar
 * 01/27/2015   DR#17006,
 *              Task#5929   Chin Chen   NSHARP freezes when loading a sounding from MDCRS products 
 *                                      in Volume Browser
 * 07/05/2016   RM#15923    Chin Chen   NSHARP - Native Code replacement
 * 23/01/2019   DR21039      smoorthy   make fonts the same as on 18.1.2
 * </pre>
 * 
 * @author Chin Chen
 * @version 1.0
 */

import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibBasics;
import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibSndglib;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.Helicity;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.Parcel;
import gov.noaa.nws.ncep.ui.nsharp.NsharpConstants;
import gov.noaa.nws.ncep.ui.nsharp.display.NsharpAbstractPaneDescriptor;
import gov.noaa.nws.ncep.ui.nsharp.display.rsc.NsharpHailInfo.HailInfoContainer;
import gov.noaa.nws.ncep.ui.nsharp.view.NsharpPaletteWindow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;

import com.raytheon.uf.viz.core.DrawableLine;
import com.raytheon.uf.viz.core.DrawableString;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.HorizontalAlignment;
import com.raytheon.uf.viz.core.IGraphicsTarget.LineStyle;
import com.raytheon.uf.viz.core.IGraphicsTarget.VerticalAlignment;
import com.raytheon.uf.viz.core.PixelExtent;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.AbstractResourceData;
import com.raytheon.uf.viz.core.rsc.LoadProperties;

public class NsharpSpcGraphsPaneResource extends NsharpAbstractPaneResource {
    private double spcLeftXOrig;

    private double spcRightXOrig;

    private double spcYOrig;

    private double spcYEnd;

    private double spcWidth;

    private double spcFrameWidth;

    private double spcHeight;

    private NsharpConstants.SPCGraph leftGraph = NsharpConstants.SPCGraph.EBS;

    private NsharpConstants.SPCGraph rightGraph = NsharpConstants.SPCGraph.STP;
    
    // set default user vrot value as 0
    private float userVrotValue = 0;

    private static int left = 0;

    private static int right = 1;

    private double xpos, xstart, xend;

    private double ypos, ystart;

    private double hRatio;

    /* sb supercell mean ebs values by percentage of storm depth */
    /* values in m/s */
    /* values in kt */
    private float supercell[] = { 18.0f, 27.6f, 34.4f, 40.0f, 44.4f, 48.8f,
            54.0f, 59.2f, 62.6f, 62.2f };

    /* mrgl supercell mean ebs values by percentage of storm depth */
    /* values in m/s */
    /* values in kt */
    private float mrglSupercell[] = { 12.4f, 20.4f, 25.6f, 28.6f, 32.2f, 36.0f,
            39.8f, 43.6f, 47.6f, 48.4f };

    /* nonsupercell mean ebs values by percentage of storm depth */
    /* values in m/s */
    /*
     * /* values in kt
     */
    private float nonSupercell[] = { 8.2f, 12.2f, 14.4f, 15.8f, 17.0f, 20.0f,
            23.2f, 27.0f, 30.4f, 32.0f };

    private RGB white = NsharpConstants.color_white;

    private RGB cyan = NsharpConstants.color_cyan_md;

    private String hailSize[][] = {
            { "", "<1", "1-1.5", "1.75", "2", "2.5", "2.75", "3-4", ">4" },
            { "+1 STD", "1.9", "2.0", "2.3", "2.8", "2.9", "3.0", "3.0", "3.0" },
            { "AVG", "1.5", "1.5", "1.8", "2.3", "2.5", "2.5", "2.6", "2.7" },
            { "-1 STD", "1.1", "1.1", "1.3", "1.7", "2.1", "2.1", "2.2", "2.4" } };

    private RGB hailSizeColor[][] = {
            { white, white, white, white, white, white, white, white, white },
            { white, cyan, cyan, cyan, cyan, cyan, cyan, cyan, cyan },
            { white, cyan, cyan, cyan, cyan, cyan, cyan, cyan, cyan },
            { white, cyan, cyan, cyan, cyan, cyan, cyan, cyan, cyan } };

    public NsharpSpcGraphsPaneResource(AbstractResourceData resourceData,
            LoadProperties loadProperties, NsharpAbstractPaneDescriptor desc) {
        super(resourceData, loadProperties, desc);
        leftGraph = NsharpPaletteWindow.getLeftGraph();
        rightGraph = NsharpPaletteWindow.getRightGraph();
    }

    private void setXyStartingPosition(int side) {
        ystart = spcYOrig;
        if (side == left) {
            xstart = spcLeftXOrig + 0.5 * charWidth;
            xend = spcLeftXOrig + spcFrameWidth;
        } else {
            xstart = spcRightXOrig + 0.5 * charWidth;
            xend = spcRightXOrig + spcFrameWidth;
        }
    }
    /**
     * This method is converted from C function show_cond_tor() of xwvid3.c from "March 2017 bigSharp version" 
     * Original C function Author: Rich Thompson of SPC   
     */
    private void plotCondTor(int side) throws VizException {
    	List<DrawableLine> lineList = new ArrayList<>();
        List<DrawableString> strList = new ArrayList<>();
        this.font11.setSmoothing(false);
        this.font11.setScaleFont(false);
        setXyStartingPosition(side);
        DrawableString titleStr = new DrawableString(
        		"Conditional Tornado Probs based on STP",
                NsharpConstants.color_white);
        titleStr.font = font11;
        titleStr.horizontalAlignment = HorizontalAlignment.LEFT;
        titleStr.verticallAlignment = VerticalAlignment.TOP;
        xpos = xstart;
        ypos = ystart;
        titleStr.setCoordinates(xpos, ypos);
        strList.add(titleStr);
        DrawableString subTStr1 = new DrawableString(
        		"EF0+",
                NsharpConstants.color_darkgreen);
        ypos = ypos +  charHeight;
        xpos = xstart + spcFrameWidth/4;
        subTStr1.font = font10;
        subTStr1.horizontalAlignment = HorizontalAlignment.LEFT;
        subTStr1.verticallAlignment = VerticalAlignment.TOP;
        subTStr1.setCoordinates(xpos, ypos);
        strList.add(subTStr1);
        
        DrawableString subTStr2 = new DrawableString(
        		"EF2+",
                NsharpConstants.color_gold);
        xpos = xstart + 2* spcFrameWidth/4;
        subTStr2.font = font10;
        subTStr2.horizontalAlignment = HorizontalAlignment.LEFT;
        subTStr2.verticallAlignment = VerticalAlignment.TOP;
        subTStr2.setCoordinates(xpos, ypos);
        strList.add(subTStr2);
        
        DrawableString subTStr3 = new DrawableString(
        		"EF4+",
                NsharpConstants.color_magenta);
        xpos = xstart + 3* spcFrameWidth/4;
        subTStr3.font = font10;
        subTStr3.horizontalAlignment = HorizontalAlignment.LEFT;
        subTStr3.verticallAlignment = VerticalAlignment.TOP;
        subTStr3.setCoordinates(xpos, ypos);
        strList.add(subTStr3);
        
        ypos = ypos + 1.5 * charHeight;
        // ----- Plot Y-Coordinate hash marks, 0 - 70  -----
        int maxval = 70;
        double knotDist = (spcYEnd - 2 * charHeight - ypos) / 70.0;
        for (int i = maxval; i >= 0; i = i - 10) {
            DrawableString lb = new DrawableString(Integer.toString(i),
                    NsharpConstants.color_vanilla);
            lb.font = font10;
            lb.horizontalAlignment = HorizontalAlignment.LEFT;
            lb.verticallAlignment = VerticalAlignment.MIDDLE;
            xpos = xstart;
            lb.setCoordinates(xpos, ypos);
            strList.add(lb);
            DrawableLine line = new DrawableLine();
            line.lineStyle = LineStyle.DASHED;
            line.basics.color = NsharpConstants.color_dodgerblue4;
            line.width = 1;
            xpos = xpos + 2 * charWidth;
            line.setCoordinates(xpos, ypos);
            line.addPoint(xend, ypos);
            lineList.add(line);
            ypos = ypos + 10 * knotDist;
        }
        //  plotting area starting 5 characters from the left frame boundary. 
        double xgap = (spcFrameWidth - 5 * charWidth) / 8;
        ypos = spcYEnd - 0.5 * charHeight;
        double cellYPosStart = spcYEnd - 2 * charHeight;
        /*  
         * Original EF0+, Ef2+, EF4+ raw value from C code.
         * EF0+ conditional tor probs by STPC bins 
         */
        float condTorF0[] = {12.4f, 15.3f, 21.1f, 27.2f, 37.3f, 40.8f, 40.8f, 40.8f};
        /* 
         * EF2+ conditional tor probs by STPC bins 
         */
        float condTorF2[] = {0.6f, 1.2f, 3.7f, 4.7f, 10f, 10.6f, 12.9f, 18.8f};
        /* 
         * EF4+ conditional tor probs by STPC bins 
         */
        float condTorF4[] = {0f, 0f, 0.1f, 0.2f, 1.4f, 1.4f, 1.6f, 6.3f};
        
        // EF0+ line
        DrawableLine ef0SolidLine = new DrawableLine();
        ef0SolidLine.lineStyle = LineStyle.SOLID;
        ef0SolidLine.basics.color = NsharpConstants.color_darkgreen;
        ef0SolidLine.width = 2;
        // EF2+ line
        DrawableLine ef2SolidLine = new DrawableLine();
        ef2SolidLine.lineStyle = LineStyle.SOLID;
        ef2SolidLine.basics.color = NsharpConstants.color_gold;
        ef2SolidLine.width = 2;
        
        // EF4+ line
        DrawableLine ef4SolidLine = new DrawableLine();
        ef4SolidLine.lineStyle = LineStyle.SOLID;
        ef4SolidLine.basics.color = NsharpConstants.color_magenta;
        ef4SolidLine.width = 2;
        
        // x-axis labels
        String xLabel[] = {".01-.25", ".25-1", "1-2", "2-3", "3-4", "4-6", "6-8", ">8"}; 
        
        // plot x-axis labels and EF0+ - EF4+ lines
        xpos = xstart +  5* charWidth;
        for (int i = 0; i < 8; i++) {
        	// label for x-axis number
        	DrawableString lb = new DrawableString(xLabel[i],
        			NsharpConstants.color_white);

        	lb.font = font9;
        	lb.horizontalAlignment = HorizontalAlignment.CENTER;
        	lb.verticallAlignment = VerticalAlignment.BOTTOM;
        	lb.setCoordinates(xpos, ypos);
        	strList.add(lb);

        	ef0SolidLine.addPoint(xpos, cellYPosStart
        				- condTorF0[i]* knotDist);
        	ef2SolidLine.addPoint(xpos, cellYPosStart
        				- condTorF2[i] * knotDist);
        	ef4SolidLine.addPoint(xpos, cellYPosStart
        				- condTorF4[i] * knotDist);
        	xpos = xpos + xgap;
        	
        }
        lineList.add(ef0SolidLine);
        lineList.add(ef2SolidLine);
        lineList.add(ef4SolidLine);
        
        // plot vertical stp line
        float stpc = weatherDataStore.getStpCin();
        DrawableLine stpcLine = new DrawableLine();
		stpcLine.lineStyle = LineStyle.DOTS;
		stpcLine.basics.color = NsharpConstants.color_white;
		stpcLine.width = 2;
		xpos = xstart;
        if (stpc >= 8.0){
        	xpos = xpos + 8* xgap;
        }
        else if (stpc >= 6.0){
        	xpos = xpos + 7* xgap;
        }
        else if (stpc >= 4.0){
        	xpos = xpos + 6* xgap;
        }
        else if (stpc >= 3.0){
        	xpos = xpos + 5* xgap;
        }
        else if (stpc >= 2.0){
        	xpos = xpos + 4* xgap;
        }
        else if (stpc >= 1.0){
        	xpos = xpos + 3* xgap;
        }
        else if (stpc >= .25){
        	xpos = xpos + 2 * xgap;
        }
        else if (stpc >= .01){
        	xpos = xpos + 1* xgap;
        }
        else if(stpc >= 0){
        	xpos = xpos + 0.4* xgap;
        }
        // plot vertical stpc line only if stpc >= 0 
        if(stpc >= 0 ){
        	stpcLine.addPoint(xpos, cellYPosStart);
        	stpcLine.addPoint(xpos, cellYPosStart -  70 * knotDist);
        	lineList.add(stpcLine);
        }

        target.drawStrings(strList.toArray(new DrawableString[strList.size()]));
        target.drawLine(lineList.toArray(new DrawableLine[lineList.size()]));
    }
    
    /**
     * This method is converted from C function show_vrot() of xwvid3.c from "March 2017 bigSharp version" 
     * Original C function Author: Bryan Smith and Rich Thompson of SPC   
     */
    private void plotVrot(int side) throws VizException {
    	List<DrawableLine> lineList = new ArrayList<>();
        List<DrawableString> strList = new ArrayList<>();
        this.font11.setSmoothing(false);
        this.font11.setScaleFont(false);
        setXyStartingPosition(side);
        DrawableString titleStr = new DrawableString(
        		"EF-scale Probs based on Vrot with RM",
                NsharpConstants.color_white);
        titleStr.font = font11;
        titleStr.horizontalAlignment = HorizontalAlignment.LEFT;
        titleStr.verticallAlignment = VerticalAlignment.TOP;
        xpos = xstart;
        ypos = ystart;
        titleStr.setCoordinates(xpos, ypos);
        strList.add(titleStr);
        
        DrawableString subTStr1 = new DrawableString(
        		"EF0+",
                NsharpConstants.color_brown);
        ypos = ypos +  charHeight;
        xpos = xstart + spcFrameWidth/6;
        subTStr1.font = font10;
        subTStr1.horizontalAlignment = HorizontalAlignment.LEFT;
        subTStr1.verticallAlignment = VerticalAlignment.TOP;
        subTStr1.setCoordinates(xpos, ypos);
        strList.add(subTStr1);
        
        DrawableString subTStr2 = new DrawableString(
        		"EF1+",
                NsharpConstants.color_darkgreen);
        xpos = xstart + 2* spcFrameWidth/6;
        subTStr2.font = font10;
        subTStr2.horizontalAlignment = HorizontalAlignment.LEFT;
        subTStr2.verticallAlignment = VerticalAlignment.TOP;
        subTStr2.setCoordinates(xpos, ypos);
        strList.add(subTStr2);
        
        DrawableString subTStr3 = new DrawableString(
        		"EF2+",
                NsharpConstants.color_gold);
        xpos = xstart + 3* spcFrameWidth/6;
        subTStr3.font = font10;
        subTStr3.horizontalAlignment = HorizontalAlignment.LEFT;
        subTStr3.verticallAlignment = VerticalAlignment.TOP;
        subTStr3.setCoordinates(xpos, ypos);
        strList.add(subTStr3);
        
        DrawableString subTStr4 = new DrawableString(
        		"EF3+",
                NsharpConstants.color_red);
        xpos = xstart + 4* spcFrameWidth/6;
        subTStr4.font = font10;
        subTStr4.horizontalAlignment = HorizontalAlignment.LEFT;
        subTStr4.verticallAlignment = VerticalAlignment.TOP;
        subTStr4.setCoordinates(xpos, ypos);
        strList.add(subTStr4);
        
        DrawableString subTStr5 = new DrawableString(
        		"EF4+",
                NsharpConstants.color_magenta);
        xpos = xstart + 5* spcFrameWidth/6;
        subTStr5.font = font10;
        subTStr5.horizontalAlignment = HorizontalAlignment.LEFT;
        subTStr5.verticallAlignment = VerticalAlignment.TOP;
        subTStr5.setCoordinates(xpos, ypos);
        strList.add(subTStr5);
        
        ypos = ypos + 1.5 * charHeight;
        // ----- Plot Y-Coordinate hash marks, 0 - 70  -----
        int maxval = 70;
        double knotDist = (spcYEnd - 2 * charHeight - ypos) / 70.0;
        for (int i = maxval; i >= 0; i = i - 10) {
            DrawableString lb = new DrawableString(Integer.toString(i),
                    NsharpConstants.color_vanilla);
            lb.font = font10;
            lb.horizontalAlignment = HorizontalAlignment.LEFT;
            lb.verticallAlignment = VerticalAlignment.MIDDLE;
            xpos = xstart;
            lb.setCoordinates(xpos, ypos);
            strList.add(lb);
            DrawableLine line = new DrawableLine();
            line.lineStyle = LineStyle.DASHED;
            line.basics.color = NsharpConstants.color_dodgerblue4;
            line.width = 1;
            xpos = xpos + 2 * charWidth;
            line.setCoordinates(xpos, ypos);
            line.addPoint(xend, ypos);
            lineList.add(line);
            ypos = ypos + 10 * knotDist;
        }
        //  plotting area starting 5 characters from the left frame boundary. 
        double xgap = (spcFrameWidth - 5 * charWidth) / 11;
        ypos = spcYEnd - 0.5 * charHeight;
        xpos = xstart;
        double cellYPosStart = spcYEnd - 2 * charHeight;

        /* original raw values from Thompson et al. (2017; Fig. 8) by 10 kt bins -> vrot_f1_0 = 0-9.9, vrot_f1_1 = 10-19.9, etc.) */
        /* used Smith et al. (2015) conditional tor probs for vrot values >70 kt since all such cases were tornadic 2014-2015 */
        /* EF0+ conditional tor probs by VROT bins, value was defined as " 0.0f, 11.9f, 16.5f, 26.3f,46.6f, 63.4f,  86.3f, 100.0f, 100.0f,  100.0f, 100.0f" */
        /* However, original C code modified plot to cap values at 70%, therefore for easy implementation, defined first value above 70 as 70 and the rest as -1. */
        float vrotF0[] = { 0.0f, 11.9f, 16.5f, 26.3f,46.6f, 63.4f,  70f, -1f, -1f,-1f,-1f};
        /* Same rules apply to EF1+, EF2+, EF3+, EF4+ */
        /* EF1+ conditional tor probs by VROT bins, value was defined as " 0f, 1.2f, 4.2f, 11.5f, 26.1f, 49.6f, 71.3f, 90.3f, 93.2f, 100f, 100f" */
        float vrotF1[] = { 0f, 1.2f, 4.2f, 11.5f, 26.1f, 49.6f,  70f, -1f, -1f,-1f,-1f};
        /* EF2+ conditional tor probs by VROT bins, value was defined as "0f, 0f, 0.4f, 1.5f, 4.4f, 20.7f, 43.8f, 71f, 84.1f, 94.4f, 100f" */
        float vrotF2[] = { 0f, 0f, 0.4f, 1.5f, 4.4f, 20.7f, 43.8f, 70f, -1f,-1f,-1f};
        /* EF3+ conditional tor probs by VROT bins, value was defined as "0f, 0f, 0f, 0.3f, 0.6f, 5.3f, 16.3f, 46.2f, 70f, 88.9f, 91.7f" */
        float vrotF3[] = { 0f, 0f, 0f, 0.3f, 0.6f, 5.3f, 16.3f, 46.2f,70f,-1f,-1f};
        /* EF4+ conditional tor probs by VROT bins, value was defined as "0f, 0f, 0f, 0f, 0.2f, 0.4f, 2.5f, 10.8f, 20.5f, 44.4f, 66.7f" */
        /* since all EF4+ values are below 70, no modification needed */
        float vrotF4[] = { 0f, 0f, 0f, 0f, 0.2f, 0.4f, 2.5f, 10.8f, 20.5f, 44.4f, 66.7f};

        // EF0+ line
        DrawableLine ef0SolidLine = new DrawableLine();
        ef0SolidLine.lineStyle = LineStyle.SOLID;
        ef0SolidLine.basics.color = NsharpConstants.color_brown;
        ef0SolidLine.width = 2;
        DrawableLine ef0DashLine = new DrawableLine();
        ef0DashLine.lineStyle = LineStyle.DASHED;
        ef0DashLine.basics.color = NsharpConstants.color_brown;
        ef0DashLine.width = 2;
        // EF1+ line
        DrawableLine ef1SolidLine = new DrawableLine();
        ef1SolidLine.lineStyle = LineStyle.SOLID;
        ef1SolidLine.basics.color = NsharpConstants.color_darkgreen;
        ef1SolidLine.width = 2;
        DrawableLine ef1DashLine = new DrawableLine();
        ef1DashLine.lineStyle = LineStyle.DASHED;
        ef1DashLine.basics.color = NsharpConstants.color_darkgreen;
        ef1DashLine.width = 2;
        // EF2+ line
        DrawableLine ef2SolidLine = new DrawableLine();
        ef2SolidLine.lineStyle = LineStyle.SOLID;
        ef2SolidLine.basics.color = NsharpConstants.color_gold;
        ef2SolidLine.width = 2;
        DrawableLine ef2DashLine = new DrawableLine();
        ef2DashLine.lineStyle = LineStyle.DASHED;
        ef2DashLine.basics.color = NsharpConstants.color_gold;
        ef2DashLine.width = 2;
        // EF3+ line
        DrawableLine ef3SolidLine = new DrawableLine();
        ef3SolidLine.lineStyle = LineStyle.SOLID;
        ef3SolidLine.basics.color = NsharpConstants.color_red;
        ef3SolidLine.width = 2;
        DrawableLine ef3DashLine = new DrawableLine();
        ef3DashLine.lineStyle = LineStyle.DASHED;
        ef3DashLine.basics.color = NsharpConstants.color_red;
        ef3DashLine.width = 2;
        // EF4+ line
        DrawableLine ef4SolidLine = new DrawableLine();
        ef4SolidLine.lineStyle = LineStyle.SOLID;
        ef4SolidLine.basics.color = NsharpConstants.color_magenta;
        ef4SolidLine.width = 2;
        //Note:  since all EF4+ values are below 70, no capped 70% dash line are needed for EF4+ */
        
        // plot x-axis labels and EF0+ - EF5+ lines
        xpos = xpos +  2* charWidth;
        for (int i = 10; i <= 110; i = i + 10) {
        	// label for x-axis number
        	DrawableString lb = new DrawableString(Integer.toString(i-10),
        			NsharpConstants.color_white);

        	lb.font = font10;
        	lb.horizontalAlignment = HorizontalAlignment.CENTER;
        	lb.verticallAlignment = VerticalAlignment.BOTTOM;
        	lb.setCoordinates(xpos, ypos);
        	strList.add(lb);

        	int cellIndex = i / 10 - 1;
        	if(vrotF0[cellIndex] >= 0){
        		ef0SolidLine.addPoint(xpos, cellYPosStart
        				- vrotF0[cellIndex] * knotDist);
        	}
        	else {
        		ef0DashLine.addPoint(xpos, cellYPosStart
        				- 70 * knotDist);
        	}
        	if(vrotF1[cellIndex] >= 0){
        		ef1SolidLine.addPoint(xpos, cellYPosStart
        				- vrotF1[cellIndex] * knotDist);
        	}
        	else {
        		ef1DashLine.addPoint(xpos, cellYPosStart 
        				- 70 * knotDist);
        	}
        	if(vrotF2[cellIndex] >= 0){
        		ef2SolidLine.addPoint(xpos, cellYPosStart
        				- vrotF2[cellIndex] * knotDist);
        	}
        	else {
        		ef2DashLine.addPoint(xpos, cellYPosStart
        				- 70 * knotDist);

        	}
        	if(vrotF3[cellIndex] >= 0){
        		ef3SolidLine.addPoint(xpos, cellYPosStart
        				- vrotF3[cellIndex] * knotDist);
        	}
        	else {
        		ef3DashLine.addPoint(xpos, cellYPosStart
        				- 70 * knotDist);

        	}
        	if(vrotF4[cellIndex] >= 0){
        		ef4SolidLine.addPoint(xpos, cellYPosStart
        				- vrotF4[cellIndex] * knotDist);
        	}
        	// Note that EF4+ does not have negativbe value defined.
        	xpos = xpos + xgap;
        	
        }
        lineList.add(ef0SolidLine);
        lineList.add(ef0DashLine);
        lineList.add(ef1SolidLine);
        lineList.add(ef1DashLine);
        lineList.add(ef2SolidLine);
        lineList.add(ef2DashLine);
        lineList.add(ef3SolidLine);
        lineList.add(ef3DashLine);
        lineList.add(ef4SolidLine);
        
        if (userVrotValue > 0) {
        	// note: original C code, hard coded display values without explanation. Therefore
        	// we are doing the same thing here.
        	xpos = xstart + 0.3 *xgap;
        	DrawableLine vrotDashLine = new DrawableLine();
    		vrotDashLine.lineStyle = LineStyle.DASHED;
    		vrotDashLine.basics.color = NsharpConstants.color_white;
    		vrotDashLine.width = 2;
        	if (userVrotValue < 10.0){
        		DrawableString lb = new DrawableString("0",NsharpConstants.color_brown);
            	lb.font = font10;
            	lb.horizontalAlignment = HorizontalAlignment.CENTER;
            	lb.verticallAlignment = VerticalAlignment.BOTTOM;
            	lb.setCoordinates(xpos+ 2* charWidth, cellYPosStart -  5 * knotDist);
            	strList.add(lb);
        	}
        	else if  (userVrotValue < 20.0){
        		xpos = xpos+ xgap;
         		DrawableString lb = new DrawableString("12",NsharpConstants.color_brown);
            	lb.font = font10;
            	lb.horizontalAlignment = HorizontalAlignment.CENTER;
            	lb.verticallAlignment = VerticalAlignment.TOP;
            	lb.setCoordinates(xpos+ 2* charWidth, cellYPosStart -  15 * knotDist);
            	strList.add(lb);
            	lb = new DrawableString("1",NsharpConstants.color_darkgreen);
            	lb.font = font10;
            	lb.horizontalAlignment = HorizontalAlignment.CENTER;
            	lb.verticallAlignment = VerticalAlignment.TOP;
            	lb.setCoordinates(xpos+ 2* charWidth, cellYPosStart -  5 * knotDist);
            	strList.add(lb);
        	}
        	else if  (userVrotValue < 30.0){
        		xpos = xpos+ 2 * xgap;
        		DrawableString lb = new DrawableString("17",NsharpConstants.color_brown);
            	lb.font = font10;
            	lb.horizontalAlignment = HorizontalAlignment.CENTER;
            	lb.verticallAlignment = VerticalAlignment.TOP;
            	lb.setCoordinates(xpos+ 2* charWidth, cellYPosStart -  20 * knotDist);
            	strList.add(lb);
            	lb = new DrawableString("4",NsharpConstants.color_darkgreen);
            	lb.font = font10;
            	lb.horizontalAlignment = HorizontalAlignment.CENTER;
            	lb.verticallAlignment = VerticalAlignment.TOP;
            	lb.setCoordinates(xpos  -2* charWidth, cellYPosStart -  10 * knotDist);
            	strList.add(lb);
        	}
        	else if (userVrotValue < 40.0){
        		xpos = xpos+ 3 * xgap;
        		DrawableString lb = new DrawableString("26",NsharpConstants.color_brown);
            	lb.font = font10;
            	lb.horizontalAlignment = HorizontalAlignment.CENTER;
            	lb.verticallAlignment = VerticalAlignment.TOP;
            	lb.setCoordinates(xpos+ 2* charWidth, cellYPosStart -  30 * knotDist);
            	strList.add(lb);
            	lb = new DrawableString("12",NsharpConstants.color_darkgreen);
            	lb.font = font10;
            	lb.horizontalAlignment = HorizontalAlignment.CENTER;
            	lb.verticallAlignment = VerticalAlignment.TOP;
            	lb.setCoordinates(xpos -2* charWidth, cellYPosStart -  15 * knotDist);
            	strList.add(lb);
            	lb = new DrawableString("2",NsharpConstants.color_gold);
            	lb.font = font10;
            	lb.horizontalAlignment = HorizontalAlignment.CENTER;
            	lb.verticallAlignment = VerticalAlignment.TOP;
            	lb.setCoordinates(xpos -2* charWidth, cellYPosStart -  5 * knotDist);
            	strList.add(lb);
        	}
        	else if (userVrotValue < 50.0){
        		xpos = xpos+ 4.1 * xgap;
        		DrawableString lb = new DrawableString("47",NsharpConstants.color_brown);
            	lb.font = font10;
            	lb.horizontalAlignment = HorizontalAlignment.CENTER;
            	lb.verticallAlignment = VerticalAlignment.TOP;
            	lb.setCoordinates(xpos+ 2* charWidth, cellYPosStart -  45 * knotDist);
            	strList.add(lb);
            	lb = new DrawableString("26",NsharpConstants.color_darkgreen);
            	lb.font = font10;
            	lb.horizontalAlignment = HorizontalAlignment.CENTER;
            	lb.verticallAlignment = VerticalAlignment.TOP;
            	lb.setCoordinates(xpos -2* charWidth, cellYPosStart -  25 * knotDist);
            	strList.add(lb);
            	lb = new DrawableString("4",NsharpConstants.color_gold);
            	lb.font = font10;
            	lb.horizontalAlignment = HorizontalAlignment.CENTER;
            	lb.verticallAlignment = VerticalAlignment.TOP;
            	lb.setCoordinates(xpos -2* charWidth, cellYPosStart -  5 * knotDist);
            	strList.add(lb);
        	}
        	else if (userVrotValue < 60.0){
        		xpos = xpos+ 5.1 * xgap;
        		DrawableString lb = new DrawableString("63",NsharpConstants.color_brown);
            	lb.font = font10;
            	lb.horizontalAlignment = HorizontalAlignment.CENTER;
            	lb.verticallAlignment = VerticalAlignment.BOTTOM;
            	lb.setCoordinates(xpos- 2* charWidth, cellYPosStart -  60 * knotDist);
            	strList.add(lb);
            	lb = new DrawableString("50",NsharpConstants.color_darkgreen);
            	lb.font = font10;
            	lb.horizontalAlignment = HorizontalAlignment.CENTER;
            	lb.verticallAlignment = VerticalAlignment.TOP;
            	lb.setCoordinates(xpos -2* charWidth, cellYPosStart -  50 * knotDist);
            	strList.add(lb);
            	lb = new DrawableString("21",NsharpConstants.color_gold);
            	lb.font = font10;
            	lb.horizontalAlignment = HorizontalAlignment.CENTER;
            	lb.verticallAlignment = VerticalAlignment.BOTTOM;
            	lb.setCoordinates(xpos -2* charWidth, cellYPosStart -  20 * knotDist);
            	strList.add(lb);
            	lb = new DrawableString("5",NsharpConstants.color_red);
            	lb.font = font10;
            	lb.horizontalAlignment = HorizontalAlignment.CENTER;
            	lb.verticallAlignment = VerticalAlignment.BOTTOM;
            	lb.setCoordinates(xpos -2* charWidth, cellYPosStart -  5 * knotDist);
            	strList.add(lb);
        	}
        	else if (userVrotValue < 70.0){
        		xpos = xpos+ 6.1 * xgap;
        		DrawableString lb = new DrawableString("71",NsharpConstants.color_darkgreen);
            	lb.font = font10;
            	lb.horizontalAlignment = HorizontalAlignment.CENTER;
            	lb.verticallAlignment = VerticalAlignment.TOP;
            	lb.setCoordinates(xpos -2* charWidth, cellYPosStart -  70 * knotDist);
            	strList.add(lb);
            	lb = new DrawableString("44",NsharpConstants.color_gold);
            	lb.font = font10;
            	lb.horizontalAlignment = HorizontalAlignment.CENTER;
            	lb.verticallAlignment = VerticalAlignment.BOTTOM;
            	lb.setCoordinates(xpos -2* charWidth, cellYPosStart -  40 * knotDist);
            	strList.add(lb);
            	lb = new DrawableString("16",NsharpConstants.color_red);
            	lb.font = font10;
            	lb.horizontalAlignment = HorizontalAlignment.CENTER;
            	lb.verticallAlignment = VerticalAlignment.BOTTOM;
            	lb.setCoordinates(xpos -2* charWidth, cellYPosStart -  15 * knotDist);
            	strList.add(lb);
            	lb = new DrawableString("3",NsharpConstants.color_magenta);
            	lb.font = font10;
            	lb.horizontalAlignment = HorizontalAlignment.CENTER;
            	lb.verticallAlignment = VerticalAlignment.BOTTOM;
            	lb.setCoordinates(xpos- 2* charWidth, cellYPosStart );
            	strList.add(lb);       	
        	}
        	else if (userVrotValue < 80.0){
        		xpos = xpos+ 7.1 * xgap;
        		DrawableString lb = new DrawableString("71",NsharpConstants.color_gold);
            	lb.font = font10;
            	lb.horizontalAlignment = HorizontalAlignment.CENTER;
            	lb.verticallAlignment = VerticalAlignment.TOP;
            	lb.setCoordinates(xpos -2* charWidth, cellYPosStart -  70 * knotDist);
            	strList.add(lb);
            	lb = new DrawableString("46",NsharpConstants.color_red);
            	lb.font = font10;
            	lb.horizontalAlignment = HorizontalAlignment.CENTER;
            	lb.verticallAlignment = VerticalAlignment.BOTTOM;
            	lb.setCoordinates(xpos -2* charWidth, cellYPosStart -  40 * knotDist);
            	strList.add(lb);
            	lb = new DrawableString("11",NsharpConstants.color_magenta);
            	lb.font = font10;
            	lb.horizontalAlignment = HorizontalAlignment.CENTER;
            	lb.verticallAlignment = VerticalAlignment.BOTTOM;
            	lb.setCoordinates(xpos- 2* charWidth, cellYPosStart -  10 * knotDist );
            	strList.add(lb);	
        	}
        	else if (userVrotValue < 90.0){
        		xpos = xpos+ 8.1 * xgap;
        		DrawableString lb = new DrawableString("68",NsharpConstants.color_red);
            	lb.font = font10;
            	lb.horizontalAlignment = HorizontalAlignment.CENTER;
            	lb.verticallAlignment = VerticalAlignment.TOP;
            	lb.setCoordinates(xpos -2* charWidth, cellYPosStart -  68 * knotDist);
            	strList.add(lb);
            	lb = new DrawableString("21",NsharpConstants.color_magenta);
            	lb.font = font10;
            	lb.horizontalAlignment = HorizontalAlignment.CENTER;
            	lb.verticallAlignment = VerticalAlignment.BOTTOM;
            	lb.setCoordinates(xpos- 2* charWidth, cellYPosStart -  20 * knotDist );
            	strList.add(lb);      	
        	}
        	else if (userVrotValue < 100.0){
        		xpos = xpos+ 9.1 * xgap;
        		DrawableString lb = new DrawableString("89",NsharpConstants.color_red);
            	lb.font = font10;
            	lb.horizontalAlignment = HorizontalAlignment.CENTER;
            	lb.verticallAlignment = VerticalAlignment.TOP;
            	lb.setCoordinates(xpos -2* charWidth, cellYPosStart -  68 * knotDist);
            	strList.add(lb);
            	lb = new DrawableString("44",NsharpConstants.color_magenta);
            	lb.font = font10;
            	lb.horizontalAlignment = HorizontalAlignment.CENTER;
            	lb.verticallAlignment = VerticalAlignment.BOTTOM;
            	lb.setCoordinates(xpos- 2* charWidth, cellYPosStart -  40 * knotDist );
            	strList.add(lb);      	
        	}
        	else {
        		xpos = xpos+ 10.1 * xgap;
        		DrawableString lb = new DrawableString("67",NsharpConstants.color_magenta);
            	lb.font = font10;
            	lb.horizontalAlignment = HorizontalAlignment.CENTER;
            	lb.verticallAlignment = VerticalAlignment.TOP;
            	lb.setCoordinates(xpos- 2* charWidth, cellYPosStart -  68 * knotDist );
            	strList.add(lb);      	
        	}
        	vrotDashLine.addPoint(xpos, cellYPosStart);
    		vrotDashLine.addPoint(xpos, cellYPosStart -  70 * knotDist);
        	lineList.add(vrotDashLine);
        }
        target.drawStrings(strList.toArray(new DrawableString[strList.size()]));
        target.drawLine(lineList.toArray(new DrawableLine[lineList.size()]));
        
    }
    
    /*
     * This function is based on show_sars() in xwvid3.c of BigNsharp
     */
    private void plotSars(int side) throws VizException {
        NsharpSarsInfo sarsInfo = weatherDataStore.getSarsInfo();
        List<DrawableLine> lineList = new ArrayList<>();
        List<DrawableString> strList = new ArrayList<>();
        this.font11.setSmoothing(false);
        this.font11.setScaleFont(false);
        this.font9.setSmoothing(false);
        this.font9.setScaleFont(false);
        this.font10.setSmoothing(false);
        setXyStartingPosition(side);
        DrawableString titleStr = new DrawableString(
                "SARS-Sounding Analog Retrieval System",
                NsharpConstants.color_white);
        titleStr.font = font11;
        titleStr.horizontalAlignment = HorizontalAlignment.CENTER;
        titleStr.verticallAlignment = VerticalAlignment.TOP;
        xpos = xstart + 0.5 * spcFrameWidth;
        ypos = ystart + 0.3 * charHeight;
        titleStr.setCoordinates(xpos, ypos);
        strList.add(titleStr);

        DrawableLine line1 = new DrawableLine();
        line1.lineStyle = LineStyle.SOLID;
        line1.basics.color = NsharpConstants.color_white;
        line1.width = 1;
        ypos = ypos + 2 * charHeight;
        line1.setCoordinates(xstart, ypos);
        line1.addPoint(xend, ypos);
        lineList.add(line1);

        DrawableLine line2 = new DrawableLine();
        line2.lineStyle = LineStyle.SOLID;
        line2.basics.color = NsharpConstants.color_white;
        line2.width = 1;
        xpos = xstart + 0.5 * spcFrameWidth;
        line2.setCoordinates(xpos, ypos);
        line2.addPoint(xpos, ypos + spcHeight - 2.3 * charHeight);
        lineList.add(line2);

        DrawableString supercellTitleStr = new DrawableString("SUPERCELL",
                NsharpConstants.color_white);
        supercellTitleStr.font = font11;
        supercellTitleStr.horizontalAlignment = HorizontalAlignment.LEFT;
        supercellTitleStr.verticallAlignment = VerticalAlignment.TOP;
        xpos = xstart + 0.15 * spcFrameWidth;
        ypos = ypos + 0.5 * charHeight;
        supercellTitleStr.setCoordinates(xpos, ypos);
        strList.add(supercellTitleStr);

        DrawableString hailTitleStr = new DrawableString("SGFNT HAIL",
                NsharpConstants.color_white);
        hailTitleStr.font = font11;
        hailTitleStr.horizontalAlignment = HorizontalAlignment.LEFT;
        hailTitleStr.verticallAlignment = VerticalAlignment.TOP;
        xpos = xstart + 0.65 * spcFrameWidth;
        hailTitleStr.setCoordinates(xpos, ypos);
        strList.add(hailTitleStr);

        DrawableLine line3 = new DrawableLine();
        line3.lineStyle = LineStyle.SOLID;
        line3.basics.color = NsharpConstants.color_white;
        line3.width = 1;
        ypos = ypos + 1.2 * charHeight;
        line3.setCoordinates(xstart, ypos);
        line3.addPoint(xend, ypos);
        lineList.add(line3);
        // since numHailstr and numSupstr should be 10, based on design,
        // we do both together.
        // for (String supStr : sarsInfo.getSupcellStr()) {
        for (int i = 0; i < NsharpSarsInfo.SARS_STRING_LINES; i++) {
            String supStr = sarsInfo.getSupcellStr().get(i);
            if (!supStr.isEmpty()) {
                RGB strColor = NsharpConstants.gempakColorToRGB.get(sarsInfo
                        .getSupcellStrColor().get(i));
                DrawableString supercellMatchStr = new DrawableString(supStr,
                        strColor);
                supercellMatchStr.font = font9;
                supercellMatchStr.horizontalAlignment = HorizontalAlignment.LEFT;
                supercellMatchStr.verticallAlignment = VerticalAlignment.TOP;
                xpos = xstart;
                supercellMatchStr.setCoordinates(xpos, ypos);
                strList.add(supercellMatchStr);
            }
            String hailStr = sarsInfo.getHailStr().get(i);
            // make sure this line is valid
            if (!hailStr.isEmpty()) {
                RGB strColor = NsharpConstants.gempakColorToRGB.get(sarsInfo
                        .getHailStrColor().get(i));
                DrawableString hailMatchStr = new DrawableString(hailStr,
                        strColor);
                hailMatchStr.font = font9;
                hailMatchStr.horizontalAlignment = HorizontalAlignment.LEFT;
                hailMatchStr.verticallAlignment = VerticalAlignment.TOP;
                xpos = xstart + 0.51 * spcFrameWidth;
                hailMatchStr.setCoordinates(xpos, ypos);
                strList.add(hailMatchStr);
            }
            ypos = ypos + charHeight;
        }
        target.drawStrings(strList.toArray(new DrawableString[strList.size()]));
        target.drawLine(lineList.toArray(new DrawableLine[lineList.size()]));
    }

    /*
     * This function is based on this function is derived from show_skewtpage1()
     * plus show_hail_new() of xwvid3.c of BigNsharp.
     */
    private void plotHail(int side) throws VizException {
        HailInfoContainer hailInfo = weatherDataStore.getHailInfoContainer();
        if (hailInfo == null) {
            DrawableString noinfoStr = new DrawableString(
                    "* * * Hail info not available * * *",
                    NsharpConstants.color_yellow);
            noinfoStr.font = font10;
            noinfoStr.horizontalAlignment = HorizontalAlignment.CENTER;
            noinfoStr.verticallAlignment = VerticalAlignment.TOP;
            xpos = xstart + 0.5 * spcFrameWidth;
            ypos = ystart + 0.3 * charHeight;
            noinfoStr.setCoordinates(xpos, ypos);
            target.drawStrings(noinfoStr);
            return;
        }
        List<DrawableLine> lineList = new ArrayList<>();
        List<DrawableString> strList = new ArrayList<>();
        this.font11.setSmoothing(false);
        this.font11.setScaleFont(false);
        this.font10.setSmoothing(false);
        this.font10.setScaleFont(false);
        this.font9.setSmoothing(false);
        this.font9.setScaleFont(false);
        setXyStartingPosition(side);
        // title string is hard coded
        DrawableString titleStr = new DrawableString(
                "* * * HAILCAST HAIL MODEL - 4/21/10 * * *",
                NsharpConstants.color_yellow);
        titleStr.font = font10;
        titleStr.horizontalAlignment = HorizontalAlignment.CENTER;
        titleStr.verticallAlignment = VerticalAlignment.TOP;
        xpos = xstart + 0.5 * spcFrameWidth;
        ypos = ystart + 0.3 * charHeight;
        titleStr.setCoordinates(xpos, ypos);
        strList.add(titleStr);
        int numLineHailStr;
        if (hailInfo.getMatches() <= 0) {
            numLineHailStr = NsharpHailInfo.HAIL_STRING_LINES_NO_MATCH;
        } else {
            numLineHailStr = NsharpHailInfo.HAIL_STRING_LINES;
        }
        for (int i = 0; i < numLineHailStr; i++) {
            ypos = ypos + charHeight;
            String hailStr = hailInfo.getHailStrList().get(i);
            RGB strColor = NsharpConstants.gempakColorToRGB.get(hailInfo
                    .getHailStrColorList().get(i));
            DrawableString hailDrawStr = new DrawableString(hailStr.trim(),
                    strColor);
            hailDrawStr.font = font9;
            hailDrawStr.horizontalAlignment = HorizontalAlignment.LEFT;
            hailDrawStr.verticallAlignment = VerticalAlignment.TOP;
            xpos = xstart;
            if (i == 4) {
                DrawableLine line1 = new DrawableLine(); // draw line
                line1.lineStyle = LineStyle.SOLID;
                line1.basics.color = NsharpConstants.color_white;
                line1.width = 1;
                line1.setCoordinates(xstart, ypos);
                line1.addPoint(xend, ypos);
                lineList.add(line1);
                hailDrawStr.font = font11; // string line 5 has bigger font
                if (hailInfo.getMember() == 0) {
                    // in this case, string line 6 is empty line
                    hailDrawStr.horizontalAlignment = HorizontalAlignment.CENTER;
                    xpos = xstart + 0.5 * spcFrameWidth;
                } else {
                    xpos = xstart + 0.1 * spcFrameWidth;
                }
            }
            if (i == 5) {
                ypos = ypos - charHeight; // string line 6 has same y
                                          // position as line 5
                hailDrawStr.font = font11; // string line 6 has bigger font
                if (hailInfo.getMember() > 0) {
                    xpos = xend - 0.1 * spcFrameWidth;
                    hailDrawStr.horizontalAlignment = HorizontalAlignment.RIGHT;
                }
            }
            if (i == 6) {
                // add a line
                DrawableLine line1 = new DrawableLine();
                line1.lineStyle = LineStyle.SOLID;
                line1.basics.color = NsharpConstants.color_white;
                line1.width = 1;
                ypos = ypos + 0.3 * charHeight;
                line1.setCoordinates(xstart, ypos);
                line1.addPoint(xend, ypos);
                lineList.add(line1);

                // sarsHailStr is hard coded
                ypos = ypos + 0.3 * charHeight;
                DrawableString sarsHailStr = new DrawableString(
                        "* * * SARS HAIL SIZE * * *",
                        NsharpConstants.color_yellow);
                sarsHailStr.font = font9;
                sarsHailStr.horizontalAlignment = HorizontalAlignment.CENTER;
                sarsHailStr.verticallAlignment = VerticalAlignment.TOP;
                xpos = xstart + 0.5 * spcFrameWidth;
                sarsHailStr.setCoordinates(xpos, ypos);
                strList.add(sarsHailStr);

                // string line 7, with bigger font
                ypos = ypos + charHeight;
                hailDrawStr.font = font9;
                hailDrawStr.horizontalAlignment = HorizontalAlignment.CENTER;
                if (hailInfo.getMatches() <= 0) {
                    // no match,line 8 is not needed, add a line
                    DrawableLine line2 = new DrawableLine();
                    line2.lineStyle = LineStyle.SOLID;
                    line2.basics.color = NsharpConstants.color_white;
                    line2.width = 1;
                    line2.setCoordinates(xstart, ypos + 1.5 * charHeight);
                    line2.addPoint(xend, ypos + 1.5 * charHeight);
                    lineList.add(line2);
                } else {
                    // need continue for string line 8
                    ypos = ypos + 0.2 * charHeight;
                }
            }
            if (i == 7 && hailInfo.getMatches() > 0) { // has match, need line 8
                hailDrawStr.horizontalAlignment = HorizontalAlignment.CENTER;
                xpos = xstart + 0.5 * spcFrameWidth;
                // add a line
                DrawableLine line2 = new DrawableLine();
                line2.lineStyle = LineStyle.SOLID;
                line2.basics.color = NsharpConstants.color_white;
                line2.width = 1;
                line2.setCoordinates(xstart, ypos + charHeight);
                line2.addPoint(xend, ypos + charHeight);
                lineList.add(line2);

            }
            hailDrawStr.setCoordinates(xpos, ypos);
            strList.add(hailDrawStr);
        }
        if (hailInfo.getMatches() > 0) {
            DrawableString sarsOutputStr = new DrawableString(
                    "SARS output ranges for reported sizes (white)",
                    NsharpConstants.color_white);
            sarsOutputStr.font = font9;
            sarsOutputStr.horizontalAlignment = HorizontalAlignment.CENTER;
            sarsOutputStr.verticallAlignment = VerticalAlignment.TOP;
            xpos = xstart + 0.5 * spcFrameWidth;
            ypos = ypos + charHeight;
            sarsOutputStr.setCoordinates(xpos, ypos);
            strList.add(sarsOutputStr);
            double tokenLen = spcFrameWidth / hailSize[0].length;

            String reportHailStr = new String(hailInfo.getReportHailStr());
            reportHailStr = reportHailStr.trim();
            double boxYStart = ypos + 1.4 * charHeight;
            double boxXStart = 0;
            for (int row = 0; row < hailSize.length; ++row) {
                ypos = ypos + 1.2 * charHeight;

                for (int column = 0; column < hailSize[row].length; ++column) {
                    DrawableString hailSizeStr = new DrawableString(
                            hailSize[row][column], hailSizeColor[row][column]);
                    hailSizeStr.horizontalAlignment = HorizontalAlignment.LEFT;
                    hailSizeStr.verticallAlignment = VerticalAlignment.TOP;
                    xpos = xstart + column * tokenLen;
                    hailSizeStr.setCoordinates(xpos, ypos);
                    if (row == 0 && hailSize[row][column].equals(reportHailStr)) {
                        hailSizeStr.font = font9;
                        boxXStart = xpos - charWidth;
                    } else {
                        hailSizeStr.font = font9;
                    }
                    strList.add(hailSizeStr);
                }
            }
            if (boxXStart > 0) {
                PixelExtent pixExt1 = new PixelExtent(boxXStart, boxXStart + 6
                        * charWidth, boxYStart, ypos + 1.4 * charHeight);
                target.drawRect(pixExt1, NsharpConstants.color_cyan_md, 1.0f,
                        1.0f);

            }
        }
        target.drawStrings(strList.toArray(new DrawableString[strList.size()]));
        target.drawLine(lineList.toArray(new DrawableLine[lineList.size()]));
    }

    /*
     * This function is based on show_fire() in xwvid3.c of BigNsharp
     */
    private void plotFire(int side) throws VizException {
        NsharpFireInfo fireInfo = weatherDataStore.getFireInfo();
        String sfcRh = new String(fireInfo.getSfcRh());
        String sfc = new String(fireInfo.getSfc());
        String zeroOneKmRh = new String(fireInfo.getZeroOneKmRh());
        String zeroOneKmMean = new String(fireInfo.getZeroOneKmMean());
        String blMeanRh = new String(fireInfo.getBlMeanRh());
        String blMean = new String(fireInfo.getBlMean());
        String pw = new String(fireInfo.getPw());
        String blMax = new String(fireInfo.getBlMax());
        String fosberg = new String(fireInfo.getFosbergIndex());
        RGB sfcRhColor = NsharpConstants.gempakColorToRGB.get(fireInfo
                .getSfcRhColor());
        RGB pwColor = NsharpConstants.gempakColorToRGB.get(fireInfo
                .getPwColor());
        RGB blMaxColor = NsharpConstants.gempakColorToRGB.get(fireInfo
                .getBlMaxColor());
        RGB fosbergColor = NsharpConstants.gempakColorToRGB.get(fireInfo
                .getFosbergColor());
        List<DrawableLine> lineList = new ArrayList<>();
        List<DrawableString> strList = new ArrayList<>();
        this.font11.setSmoothing(false);
        this.font11.setScaleFont(false);
        this.font12.setSmoothing(false);
        this.font12.setScaleFont(false);
        this.font10.setScaleFont(false);
        this.font10.setSmoothing(false);
        setXyStartingPosition(side);
        DrawableString titleStr = new DrawableString("Fire Weather Parameters",
                NsharpConstants.color_white);
        titleStr.font = font12;
        titleStr.horizontalAlignment = HorizontalAlignment.LEFT;
        titleStr.verticallAlignment = VerticalAlignment.TOP;
        xpos = xstart + 0.25 * spcFrameWidth;
        ypos = ystart;
        titleStr.setCoordinates(xpos, ypos);
        strList.add(titleStr);
        DrawableString moistureTStr = new DrawableString("Moisture",
                NsharpConstants.color_lawngreen);
        moistureTStr.font = font12;
        moistureTStr.horizontalAlignment = HorizontalAlignment.LEFT;
        moistureTStr.verticallAlignment = VerticalAlignment.TOP;
        double xleft = xstart + 0.1 * spcFrameWidth;
        ypos = ypos + 2 * charHeight;
        moistureTStr.setCoordinates(xleft, ypos);
        strList.add(moistureTStr);
        DrawableString llWindTStr = new DrawableString("Low-Level Wind",
                NsharpConstants.color_dodgerblue);
        llWindTStr.font = font12;
        llWindTStr.horizontalAlignment = HorizontalAlignment.LEFT;
        llWindTStr.verticallAlignment = VerticalAlignment.TOP;
        double xright = xstart + 0.5 * spcFrameWidth;
        llWindTStr.setCoordinates(xright, ypos);
        strList.add(llWindTStr);
        DrawableLine line1 = new DrawableLine();
        line1.lineStyle = LineStyle.SOLID;
        line1.basics.color = NsharpConstants.color_white;
        line1.width = 1;
        ypos = ypos + 2 * charHeight;
        line1.setCoordinates(xstart, ypos);
        line1.addPoint(xend, ypos);
        lineList.add(line1);
        DrawableString sfcRhStr = new DrawableString(sfcRh.trim(), sfcRhColor);
        sfcRhStr.font = font12;
        sfcRhStr.horizontalAlignment = HorizontalAlignment.LEFT;
        sfcRhStr.verticallAlignment = VerticalAlignment.TOP;
        ypos = ypos + 0.3 * charHeight;
        sfcRhStr.setCoordinates(xleft, ypos);
        strList.add(sfcRhStr);
        DrawableString sfcStr = new DrawableString(sfc.trim(),
                NsharpConstants.color_white);
        sfcStr.font = font10;
        sfcStr.horizontalAlignment = HorizontalAlignment.LEFT;
        sfcStr.verticallAlignment = VerticalAlignment.TOP;
        sfcStr.setCoordinates(xright, ypos);
        strList.add(sfcStr);
        DrawableString zeroOneRhStr = new DrawableString(zeroOneKmRh.trim(),
                NsharpConstants.color_white);
        zeroOneRhStr.font = font10;
        zeroOneRhStr.horizontalAlignment = HorizontalAlignment.LEFT;
        zeroOneRhStr.verticallAlignment = VerticalAlignment.TOP;
        ypos = ypos + 1.5 * charHeight;
        zeroOneRhStr.setCoordinates(xleft, ypos);
        strList.add(zeroOneRhStr);
        DrawableString zeroOneKmMeanStr = new DrawableString(
                zeroOneKmMean.trim(), NsharpConstants.color_white);
        zeroOneKmMeanStr.font = font10;
        zeroOneKmMeanStr.horizontalAlignment = HorizontalAlignment.LEFT;
        zeroOneKmMeanStr.verticallAlignment = VerticalAlignment.TOP;
        zeroOneKmMeanStr.setCoordinates(xright, ypos);
        strList.add(zeroOneKmMeanStr);
        DrawableString blMeanRhStr = new DrawableString(blMeanRh.trim(),
                NsharpConstants.color_white);
        blMeanRhStr.font = font10;
        blMeanRhStr.horizontalAlignment = HorizontalAlignment.LEFT;
        blMeanRhStr.verticallAlignment = VerticalAlignment.TOP;
        ypos = ypos + 1.5 * charHeight;
        blMeanRhStr.setCoordinates(xleft, ypos);
        strList.add(blMeanRhStr);
        DrawableString blMeanStr = new DrawableString(blMean.trim(),
                NsharpConstants.color_white);
        blMeanStr.font = font10;
        blMeanStr.horizontalAlignment = HorizontalAlignment.LEFT;
        blMeanStr.verticallAlignment = VerticalAlignment.TOP;
        blMeanStr.setCoordinates(xright, ypos);
        strList.add(blMeanStr);
        DrawableString pwStr = new DrawableString(pw.trim(), pwColor);
        if (pwColor.equals(NsharpConstants.color_red)) {
            pwStr.font = font12;
        } else {
            pwStr.font = font10;
        }
        pwStr.horizontalAlignment = HorizontalAlignment.LEFT;
        pwStr.verticallAlignment = VerticalAlignment.TOP;
        ypos = ypos + 1.5 * charHeight;
        pwStr.setCoordinates(xleft, ypos);
        strList.add(pwStr);
        DrawableString blMaxStr = new DrawableString(blMax.trim(), blMaxColor);
        blMaxStr.font = font12;
        blMaxStr.horizontalAlignment = HorizontalAlignment.LEFT;
        blMaxStr.verticallAlignment = VerticalAlignment.TOP;
        blMaxStr.setCoordinates(xright, ypos);
        strList.add(blMaxStr);
        DrawableString derivedTStr = new DrawableString("Derived Indices",
                NsharpConstants.color_orange);
        derivedTStr.font = font12;
        derivedTStr.horizontalAlignment = HorizontalAlignment.LEFT;
        derivedTStr.verticallAlignment = VerticalAlignment.TOP;
        ypos = ypos + 3 * charHeight;
        derivedTStr.setCoordinates(xstart + 0.3 * spcFrameWidth, ypos);
        strList.add(derivedTStr);
        DrawableLine line2 = new DrawableLine();
        line2.lineStyle = LineStyle.SOLID;
        line2.basics.color = NsharpConstants.color_orange;
        line2.width = 1;
        ypos = ypos + 1.3 * charHeight;
        line2.setCoordinates(xstart, ypos);
        line2.addPoint(xend, ypos);
        lineList.add(line2);
        DrawableString fosbergStr = new DrawableString(fosberg.trim(),
                fosbergColor);
        fosbergStr.font = font12;
        fosbergStr.horizontalAlignment = HorizontalAlignment.LEFT;
        fosbergStr.verticallAlignment = VerticalAlignment.TOP;
        ypos = ypos + charHeight;
        fosbergStr.setCoordinates(xstart + 0.27 * spcFrameWidth, ypos);
        strList.add(fosbergStr);
        target.drawStrings(strList.toArray(new DrawableString[strList.size()]));
        target.drawLine(lineList.toArray(new DrawableLine[lineList.size()]));
    }

    /*
     * This function is based on show_winter_new() in xwvid3.c of BigNsharp
     */
    private void plotWinter(int side) throws VizException {
        NsharpWinterInfo winterInfo = weatherDataStore.getWinterInfo();
        String temp1 = new String(winterInfo.getTempProfile1());
        String temp2 = new String(winterInfo.getTempProfile2());
        String temp3 = new String(winterInfo.getTempProfile3());
        String wetbulb1 = new String(winterInfo.getWetbulbProfile1());
        String wetbulb2 = new String(winterInfo.getWetbulbProfile2());
        String wetbulb3 = new String(winterInfo.getWetbulbProfile3());
        String bestGuess1 = new String(winterInfo.getBestGuess1());
        String bestGuess2 = new String(winterInfo.getBestGuess2());
        String initPhase = new String(winterInfo.getInitPhase());
        String meanLayerMixRat = new String(winterInfo.getMeanLayerMixRat());
        String meanLayerOmega = new String(winterInfo.getMeanLayerOmega());
        String meanLayerPw = new String(winterInfo.getMeanLayerPw());
        String meanLayerRh = new String(winterInfo.getMeanLayerRh());
        String layerDepth = new String(winterInfo.getLayerDepth());
        String oprh = new String(winterInfo.getOprh());

        List<DrawableLine> lineList = new ArrayList<>();
        List<DrawableString> strList = new ArrayList<>();
        this.font9.setSmoothing(false);
        this.font9.setScaleFont(false);
        this.font10.setScaleFont(false);
        this.font10.setSmoothing(false);
        setXyStartingPosition(side);
        DrawableString titleStr = new DrawableString(
                "* * * DENDRITIC GROWTH ZONE (-12 to -17C) * * *",
                NsharpConstants.color_yellow);
        titleStr.font = font10;
        titleStr.horizontalAlignment = HorizontalAlignment.LEFT;
        titleStr.verticallAlignment = VerticalAlignment.TOP;
        xpos = xstart;
        ypos = ystart;
        titleStr.setCoordinates(xpos, ypos);
        strList.add(titleStr);
        RGB oprhColor;
        if (winterInfo.getMopw() < -.1f) {
            oprhColor = NsharpConstants.color_red; // GEMPAK color 13
        } else {
            oprhColor = NsharpConstants.color_white;// GEMPAK color 31
        }
        DrawableString oprhStr = new DrawableString(oprh.trim(), oprhColor);
        oprhStr.font = font10;
        oprhStr.horizontalAlignment = HorizontalAlignment.LEFT;
        oprhStr.verticallAlignment = VerticalAlignment.TOP;
        xpos = xstart + 0.3 * spcFrameWidth;
        ypos = ypos + charHeight;

        oprhStr.setCoordinates(xpos, ypos);
        strList.add(oprhStr);

        DrawableString layerDepthStr = new DrawableString(layerDepth.trim(),
                NsharpConstants.color_white);
        layerDepthStr.font = font9;
        layerDepthStr.horizontalAlignment = HorizontalAlignment.LEFT;
        layerDepthStr.verticallAlignment = VerticalAlignment.TOP;
        xpos = xstart + 0.3 * charWidth;
        ypos = ypos + charHeight;

        layerDepthStr.setCoordinates(xpos, ypos);
        strList.add(layerDepthStr);

        DrawableString meanLayerRhStr = new DrawableString(meanLayerRh.trim(),
                NsharpConstants.color_white);
        meanLayerRhStr.font = font9;
        meanLayerRhStr.horizontalAlignment = HorizontalAlignment.LEFT;
        meanLayerRhStr.verticallAlignment = VerticalAlignment.TOP;
        xpos = xstart + 0.3 * charWidth;
        ypos = ypos + 1.2 * charHeight;

        meanLayerRhStr.setCoordinates(xpos, ypos);
        strList.add(meanLayerRhStr);

        DrawableLine line = new DrawableLine();
        line.lineStyle = LineStyle.SOLID;
        line.basics.color = NsharpConstants.color_white;
        line.width = 1;
        xpos = xstart + 0.5 * spcFrameWidth;
        line.setCoordinates(xpos, ypos);
        line.addPoint(xpos, ypos + 2 * charHeight);
        lineList.add(line);

        DrawableString meanLayerMixRatStr = new DrawableString(
                meanLayerMixRat.trim(), NsharpConstants.color_white);
        meanLayerMixRatStr.font = font9;
        meanLayerMixRatStr.horizontalAlignment = HorizontalAlignment.LEFT;
        meanLayerMixRatStr.verticallAlignment = VerticalAlignment.TOP;
        xpos = xstart + 0.5 * charWidth + 0.5 * spcFrameWidth;
        meanLayerMixRatStr.setCoordinates(xpos, ypos);
        strList.add(meanLayerMixRatStr);

        DrawableString meanLayerPwStr = new DrawableString(meanLayerPw.trim(),
                NsharpConstants.color_white);
        meanLayerPwStr.font = font9;
        meanLayerPwStr.horizontalAlignment = HorizontalAlignment.LEFT;
        meanLayerPwStr.verticallAlignment = VerticalAlignment.TOP;
        xpos = xstart + 0.3 * charWidth;
        ypos = ypos + charHeight;

        meanLayerPwStr.setCoordinates(xpos, ypos);
        strList.add(meanLayerPwStr);

        DrawableString meanLayerOmegaStr = new DrawableString(
                meanLayerOmega.trim(), NsharpConstants.color_white);
        meanLayerOmegaStr.font = font9;
        meanLayerOmegaStr.horizontalAlignment = HorizontalAlignment.LEFT;
        meanLayerOmegaStr.verticallAlignment = VerticalAlignment.TOP;
        xpos = xstart + 0.5 * charWidth + 0.5 * spcFrameWidth;
        meanLayerOmegaStr.setCoordinates(xpos, ypos);
        strList.add(meanLayerOmegaStr);

        DrawableLine line1 = new DrawableLine();
        line1.lineStyle = LineStyle.SOLID;
        line1.basics.color = NsharpConstants.color_white;
        line1.width = 1;
        xpos = xstart + 0.3 * charWidth;
        ypos = ypos + charHeight;
        line1.setCoordinates(xpos, ypos);
        line1.addPoint(xend - charWidth, ypos);
        lineList.add(line1);

        DrawableString initPhaseStr = new DrawableString(initPhase.trim(),
                NsharpConstants.color_white);
        initPhaseStr.font = font9;
        initPhaseStr.horizontalAlignment = HorizontalAlignment.LEFT;
        initPhaseStr.verticallAlignment = VerticalAlignment.TOP;
        xpos = xstart + 0.3 * charWidth;
        ypos = ypos + 0.2 * charHeight;
        initPhaseStr.setCoordinates(xpos, ypos);
        strList.add(initPhaseStr);

        DrawableLine line2 = new DrawableLine();
        line2.lineStyle = LineStyle.SOLID;
        line2.basics.color = NsharpConstants.color_white;
        line2.width = 1;
        xpos = xstart + 0.3 * charWidth;
        ypos = ypos + charHeight;
        line2.setCoordinates(xpos, ypos);
        line2.addPoint(xend - charWidth, ypos);
        lineList.add(line2);

        DrawableLine line3 = new DrawableLine();
        line3.lineStyle = LineStyle.SOLID;
        line3.basics.color = NsharpConstants.color_white;
        line3.width = 1;
        xpos = xstart + 0.5 * spcFrameWidth;
        line3.setCoordinates(xpos, ypos);
        double line3End = ypos + 5 * charHeight;
        line3.addPoint(xpos, line3End);
        lineList.add(line3);

        DrawableString tempProfileTitleStr = new DrawableString(
                "TEMPERATURE PROFILE", NsharpConstants.color_white);
        tempProfileTitleStr.font = font9;
        tempProfileTitleStr.horizontalAlignment = HorizontalAlignment.LEFT;
        tempProfileTitleStr.verticallAlignment = VerticalAlignment.TOP;
        xpos = xstart + 0.3 * charWidth;
        ypos = ypos + 0.2 * charHeight;
        tempProfileTitleStr.setCoordinates(xpos, ypos);
        strList.add(tempProfileTitleStr);

        DrawableString wetbulbTitleStr = new DrawableString("WETBULB PROFILE",
                NsharpConstants.color_white);
        wetbulbTitleStr.font = font9;
        wetbulbTitleStr.horizontalAlignment = HorizontalAlignment.LEFT;
        wetbulbTitleStr.verticallAlignment = VerticalAlignment.TOP;
        xpos = xstart + 0.5 * charWidth + 0.5 * spcFrameWidth;
        wetbulbTitleStr.setCoordinates(xpos, ypos);
        strList.add(wetbulbTitleStr);

        DrawableString temp1Str = new DrawableString(temp1.trim(),
                NsharpConstants.color_white);
        temp1Str.font = font9;
        temp1Str.horizontalAlignment = HorizontalAlignment.LEFT;
        temp1Str.verticallAlignment = VerticalAlignment.TOP;
        xpos = xstart + 0.3 * charWidth;
        ypos = ypos + charHeight;
        temp1Str.setCoordinates(xpos, ypos);
        strList.add(temp1Str);

        DrawableString wetbulb1Str = new DrawableString(wetbulb1.trim(),
                NsharpConstants.color_white);
        wetbulb1Str.font = font9;
        wetbulb1Str.horizontalAlignment = HorizontalAlignment.LEFT;
        wetbulb1Str.verticallAlignment = VerticalAlignment.TOP;
        xpos = xstart + 0.5 * charWidth + 0.5 * spcFrameWidth;
        wetbulb1Str.setCoordinates(xpos, ypos);
        strList.add(wetbulb1Str);

        DrawableString temp2Str = new DrawableString(temp2.trim(),
                NsharpConstants.color_white);
        temp2Str.font = font9;
        temp2Str.horizontalAlignment = HorizontalAlignment.LEFT;
        temp2Str.verticallAlignment = VerticalAlignment.TOP;
        xpos = xstart + 0.3 * charWidth;
        ypos = ypos + charHeight;
        temp2Str.setCoordinates(xpos, ypos);
        strList.add(temp2Str);

        DrawableString wetbulb2Str = new DrawableString(wetbulb2.trim(),
                NsharpConstants.color_white);
        wetbulb2Str.font = font9;
        wetbulb2Str.horizontalAlignment = HorizontalAlignment.LEFT;
        wetbulb2Str.verticallAlignment = VerticalAlignment.TOP;
        xpos = xstart + 0.5 * charWidth + 0.5 * spcFrameWidth;
        wetbulb2Str.setCoordinates(xpos, ypos);
        strList.add(wetbulb2Str);

        DrawableString temp3Str = new DrawableString(temp3.trim(),
                NsharpConstants.color_white);
        temp3Str.font = font9;
        temp3Str.horizontalAlignment = HorizontalAlignment.LEFT;
        temp3Str.verticallAlignment = VerticalAlignment.TOP;
        xpos = xstart + 0.3 * charWidth;
        ypos = ypos + charHeight;
        temp3Str.setCoordinates(xpos, ypos);
        strList.add(temp3Str);

        DrawableString wetbulb3Str = new DrawableString(wetbulb3.trim(),
                NsharpConstants.color_white);
        wetbulb3Str.font = font9;
        wetbulb3Str.horizontalAlignment = HorizontalAlignment.LEFT;
        wetbulb3Str.verticallAlignment = VerticalAlignment.TOP;
        xpos = xstart + 0.5 * charWidth + 0.5 * spcFrameWidth;
        wetbulb3Str.setCoordinates(xpos, ypos);
        strList.add(wetbulb3Str);

        DrawableLine line4 = new DrawableLine();
        line4.lineStyle = LineStyle.SOLID;
        line4.basics.color = NsharpConstants.color_white;
        line4.width = 1;
        xpos = xstart + 0.3 * charWidth;
        line4.setCoordinates(xpos, line3End);
        line4.addPoint(xend - charWidth, line3End);
        lineList.add(line4);

        DrawableString bestGuessTitleStr = new DrawableString(
                "* * * BEST GUESS PRECIP TYPE * * *",
                NsharpConstants.color_white);
        bestGuessTitleStr.font = font9;
        bestGuessTitleStr.horizontalAlignment = HorizontalAlignment.LEFT;
        bestGuessTitleStr.verticallAlignment = VerticalAlignment.TOP;
        xpos = xstart + 0.25 * spcFrameWidth;
        ypos = ypos + 2 * charHeight;
        bestGuessTitleStr.setCoordinates(xpos, ypos);
        strList.add(bestGuessTitleStr);

        DrawableString bestGuess1Str = new DrawableString(bestGuess1.trim(),
                NsharpConstants.color_white);
        bestGuess1Str.font = font9;
        bestGuess1Str.horizontalAlignment = HorizontalAlignment.LEFT;
        bestGuess1Str.verticallAlignment = VerticalAlignment.TOP;
        xpos = xstart + 0.45 * spcFrameWidth;
        ypos = ypos + 1.2 * charHeight;
        bestGuess1Str.setCoordinates(xpos, ypos);
        strList.add(bestGuess1Str);

        DrawableString bestGuess2Str = new DrawableString(bestGuess2.trim(),
                NsharpConstants.color_white);
        bestGuess2Str.font = font9;
        bestGuess2Str.horizontalAlignment = HorizontalAlignment.LEFT;
        bestGuess2Str.verticallAlignment = VerticalAlignment.TOP;
        xpos = xstart + 0.25 * spcFrameWidth;
        ypos = ypos + 1.2 * charHeight;
        bestGuess2Str.setCoordinates(xpos, ypos);
        strList.add(bestGuess2Str);

        target.drawStrings(strList.toArray(new DrawableString[strList.size()]));
        target.drawLine(lineList.toArray(new DrawableLine[lineList.size()]));
    }

    /*
     * This function is based on show_ship_stats() in xwvid3.c of BigNsharp
     */
    private void plotSHIP(int side) throws VizException {
        List<DrawableLine> lineList = new ArrayList<>();
        List<DrawableString> strList = new ArrayList<>();
        this.font12.setSmoothing(false);
        this.font12.setScaleFont(false);
        this.font10.setSmoothing(false);

        setXyStartingPosition(side);
        DrawableString titleStr = new DrawableString(
                "Significant Hail Parameter (SHIP)",
                NsharpConstants.color_white);
        titleStr.font = font12;
        titleStr.horizontalAlignment = HorizontalAlignment.LEFT;
        titleStr.verticallAlignment = VerticalAlignment.TOP;
        xpos = xstart + 0.1 * spcFrameWidth;
        ypos = ystart;
        titleStr.setCoordinates(xpos, ypos);
        strList.add(titleStr);

        // Plot Y-Coord hash marks
        int maxSHIPValue = 7;
        // shipDist = one unit of SHIP distance in Y-axis
        // 7 SHIP unit in total at Y axis
        ypos = ypos + 2 * charHeight;
        double shipDist = (spcYEnd - 2 * charHeight - ypos) / 7.0;
        double ship7Ypos = ypos;
        for (int i = maxSHIPValue; i >= 0; i--) {
            DrawableString lb = new DrawableString(Integer.toString(i),
                    NsharpConstants.color_white);
            lb.font = font10;
            lb.horizontalAlignment = HorizontalAlignment.LEFT;
            lb.verticallAlignment = VerticalAlignment.MIDDLE;
            xpos = xstart;
            lb.setCoordinates(xpos, ypos);
            strList.add(lb);
            DrawableLine line = new DrawableLine();
            line.lineStyle = LineStyle.DASHED;
            line.basics.color = NsharpConstants.temperatureColor;
            line.width = 1;
            xpos = xpos + 2 * charWidth;
            line.setCoordinates(xpos, ypos);
            line.addPoint(xend, ypos);
            lineList.add(line);
            ypos = ypos + shipDist;
        }
        // plot hail box and whiskers
        double ship0Ypos = ypos - shipDist;
        double shipHeight = ship0Ypos - ship7Ypos;
        double boxWidth = (xend - xstart) / 5.0;
        // nonsig hail box and whiskers values and sig hail box and whiskers
        // values
        String boxName[] = { "< 2in", ">= 2in" };
        double boxWhiskerValue[][] = { { 1.6, 1.0, 0.4, 0.3 },
                { 3.2, 2.5, 1.3, 1.0 } };
        for (int i = 0; i < 2; i++) {
            DrawableString lb = new DrawableString(boxName[i],
                    NsharpConstants.color_white);
            lb.font = font10;
            lb.horizontalAlignment = HorizontalAlignment.CENTER;
            lb.verticallAlignment = VerticalAlignment.TOP;
            xpos = xstart + (xend - xstart) / 3.0 * (i + 1);
            lb.setCoordinates(xpos, ship0Ypos + 0.5 * charWidth);
            strList.add(lb);
            DrawableLine upWhiskerline = new DrawableLine();
            upWhiskerline.lineStyle = LineStyle.SOLID;
            upWhiskerline.basics.color = NsharpConstants.color_darkgreen;
            upWhiskerline.width = 3;
            double s90Ypos = ship0Ypos
                    - (boxWhiskerValue[i][0] / 7.0 * shipHeight);
            upWhiskerline.setCoordinates(xpos, s90Ypos);
            double s75Ypos = ship0Ypos
                    - (boxWhiskerValue[i][1] / 7.0 * shipHeight);
            upWhiskerline.addPoint(xpos, s75Ypos);
            lineList.add(upWhiskerline);

            double s25Ypos = ship0Ypos
                    - (boxWhiskerValue[i][2] / 7.0 * shipHeight);
            PixelExtent pixExt1 = new PixelExtent(xpos - boxWidth / 2.0, xpos
                    + boxWidth / 2.0, s75Ypos, s25Ypos);
            target.drawRect(pixExt1, NsharpConstants.color_darkgreen, 3.0f,
                    1.0f);
            DrawableLine lowWiskerline = new DrawableLine();
            lowWiskerline.lineStyle = LineStyle.SOLID;
            lowWiskerline.basics.color = NsharpConstants.color_darkgreen;
            lowWiskerline.width = 3;
            lowWiskerline.setCoordinates(xpos, s25Ypos);
            double s10Ypos = ship0Ypos
                    - (boxWhiskerValue[i][3] / 7.0 * shipHeight);
            lowWiskerline.addPoint(xpos, s10Ypos);
            lineList.add(lowWiskerline);
        }
        float ship = weatherDataStore.getShip();
        if (NsharpLibBasics.qc(ship)) {
            double shipcY = ship0Ypos - (ship / 7.0 * shipHeight);
            RGB shipColor;
            if (ship >= 5)
                shipColor = NsharpConstants.color_magenta; // GEMPAK color 7
            else if (ship >= 2)
                shipColor = NsharpConstants.color_red;// GEMPAK color 2
            else if (ship >= 1)
                shipColor = NsharpConstants.color_gold;// GEMPAK color 19
            else if (ship >= .5)
                shipColor = NsharpConstants.color_white;// GEMPAK color 31
            else
                shipColor = NsharpConstants.color_brown; // (ship < .5)
                                                         // GEMPAK color 8

            DrawableLine shipline = new DrawableLine();
            shipline.lineStyle = LineStyle.SOLID;
            shipline.basics.color = shipColor;
            shipline.width = 3;
            shipline.setCoordinates(xstart, shipcY);
            shipline.addPoint(xend, shipcY);
            lineList.add(shipline);
        }
        target.drawStrings(strList.toArray(new DrawableString[strList.size()]));
        target.drawLine(lineList.toArray(new DrawableLine[lineList.size()]));
    }

    /*
     * This function is based on show_stp_stats() in xwvid3.c of BigNsharp
     */
    private void plotSTP(int side) throws VizException {
        List<DrawableLine> lineList = new ArrayList<>();
        List<DrawableString> strList = new ArrayList<>();
        this.font12.setSmoothing(false);
        this.font12.setScaleFont(false);
        this.font10.setSmoothing(false);
        this.font10.setScaleFont(false);
        setXyStartingPosition(side);
        DrawableString titleStr = new DrawableString(
                "Effective-Layer STP (with CIN)", NsharpConstants.color_white);
        titleStr.font = font12;
        titleStr.horizontalAlignment = HorizontalAlignment.LEFT;
        titleStr.verticallAlignment = VerticalAlignment.TOP;
        xpos = xstart + 0.1 * spcFrameWidth;
        ypos = ystart;
        titleStr.setCoordinates(xpos, ypos);
        strList.add(titleStr);

        // Plot Y-Coord hash marks
        int maxSTPValue = 11;
        // stpDist = one STP unit distance in Y-axis
        // 11 STP unit in total at Y axis
        ypos = ypos + 1.5 * charHeight;
        double stpDist = (spcYEnd - 2 * charHeight - ypos) / 11.0;
        double stp11Ypos = ypos;
        for (int i = maxSTPValue; i >= 0; i--) {
            DrawableString lb = new DrawableString(Integer.toString(i),
                    NsharpConstants.color_white);
            lb.font = font10;
            lb.horizontalAlignment = HorizontalAlignment.LEFT;
            lb.verticallAlignment = VerticalAlignment.MIDDLE;
            xpos = xstart;
            lb.setCoordinates(xpos, ypos);
            strList.add(lb);
            DrawableLine line = new DrawableLine();
            line.lineStyle = LineStyle.DASHED;
            line.basics.color = NsharpConstants.temperatureColor;
            line.width = 1;
            xpos = xpos + 2 * charWidth;
            line.setCoordinates(xpos, ypos);
            line.addPoint(xend, ypos);
            lineList.add(line);
            ypos = ypos + stpDist;
        }

        // plot tor box and whiskers
        double stp0Ypos = ypos - stpDist;
        double stpHeight = stp0Ypos - stp11Ypos;
        double boxWidth = (xend - xstart) / 9.5;
        // EF4+, EF3, EF2, EF1, EF0, nontor boxes and whiskers values
        // definition, see show_stp_stats()
        String boxName[] = { "EF4+", "EF3", "EF2", "EF1", "EF0", "NONTOR" };
        double boxWhiskerValue[][] = { { 11.0, 8.3, 5.3, 2.8, 1.2 },
                { 8.4, 4.5, 2.4, 1.0, 0.2 }, { 5.6, 3.7, 1.7, 0.6, 0.0 },
                { 4.5, 2.6, 1.2, 0.3, 0.0 }, { 3.7, 2.0, 0.8, 0.1, 0.0 },
                { 1.5, 0.7, 0.2, 0.0, 0.0 } };
        RGB whiskerColor[] = { NsharpConstants.color_lawngreen,
                NsharpConstants.color_lawngreen,
                NsharpConstants.color_lawngreen, NsharpConstants.color_mdgreen,
                NsharpConstants.color_mdgreen, NsharpConstants.color_darkgreen };
        for (int i = 0; i < boxName.length; i++) {
            DrawableString lb = new DrawableString(boxName[i],
                    NsharpConstants.color_white);
            lb.font = font10;
            lb.horizontalAlignment = HorizontalAlignment.CENTER;
            lb.verticallAlignment = VerticalAlignment.TOP;
            xpos = xstart + (xend - xstart) / 7.0 * (i + 1);
            lb.setCoordinates(xpos, stp0Ypos + 0.5 * charWidth);
            strList.add(lb);
            DrawableLine upWhiskerline = new DrawableLine();
            upWhiskerline.lineStyle = LineStyle.SOLID;
            upWhiskerline.basics.color = whiskerColor[i];
            upWhiskerline.width = 3;
            double s90Ypos = stp0Ypos
                    - (boxWhiskerValue[i][0] / 11.0 * stpHeight);
            upWhiskerline.setCoordinates(xpos, s90Ypos);
            double s75Ypos = stp0Ypos
                    - (boxWhiskerValue[i][1] / 11.0 * stpHeight);
            upWhiskerline.addPoint(xpos, s75Ypos);
            lineList.add(upWhiskerline);
            double s50Ypos = stp0Ypos
                    - (boxWhiskerValue[i][2] / 11.0 * stpHeight);
            PixelExtent pixExt = new PixelExtent(xpos - boxWidth / 2.0, xpos
                    + boxWidth / 2.0, s75Ypos, s50Ypos);
            target.drawRect(pixExt, whiskerColor[i], 3.0f, 1.0f);
            double s25Ypos = stp0Ypos
                    - (boxWhiskerValue[i][3] / 11.0 * stpHeight);
            PixelExtent pixExt1 = new PixelExtent(xpos - boxWidth / 2.0, xpos
                    + boxWidth / 2.0, s50Ypos, s25Ypos);
            target.drawRect(pixExt1, whiskerColor[i], 3.0f, 1.0f);
            DrawableLine lowWiskerline = new DrawableLine();
            lowWiskerline.lineStyle = LineStyle.SOLID;
            lowWiskerline.basics.color = whiskerColor[i];
            lowWiskerline.width = 3;
            lowWiskerline.setCoordinates(xpos, s25Ypos);
            double s10Ypos = stp0Ypos
                    - (boxWhiskerValue[i][4] / 11.0 * stpHeight);
            lowWiskerline.addPoint(xpos, s10Ypos);
            lineList.add(lowWiskerline);
        }
        // plot sounding value of STPC first
        // "STP (CIN)"
        float cin = weatherDataStore.getStpCin();
        if (cin > maxSTPValue) {
            cin = maxSTPValue;
        }

        double stpcY = stp0Ypos - (cin / 11.0 * stpHeight);
        RGB cinColor;
        if (cin >= 8)
            cinColor = NsharpConstants.color_magenta; // GEMPAK color 7
        else if (cin >= 4)
            cinColor = NsharpConstants.color_red;// GEMPAK color 2
        else if (cin >= 2)
            cinColor = NsharpConstants.color_gold;// GEMPAK color 19
        else if (cin >= 1)
            cinColor = NsharpConstants.color_white;// GEMPAK color 31
        else if (cin >= 0.5)
            cinColor = NsharpConstants.color_darkorange;// GEMPAK color 18
        else
            cinColor = NsharpConstants.color_brown; // GEMPAK color 8
        DrawableLine STPCline = new DrawableLine();
        STPCline.lineStyle = LineStyle.SOLID;
        STPCline.basics.color = cinColor;
        STPCline.width = 3;
        STPCline.setCoordinates(xstart, stpcY);
        STPCline.addPoint(xend, stpcY);
        lineList.add(STPCline);
        target.drawStrings(strList.toArray(new DrawableString[strList.size()]));
        target.drawLine(lineList.toArray(new DrawableLine[lineList.size()]));

        // Calculates and plots the probability of an F2+ tornado
        // (given a supercell) based on MLCAPE alone. Probabilities
        // are derived from Thompson et al. 2005 RUC soundings
        // based on prob_sigt_mlcape() of xwvid3.c

        // tornado probability inset box
        PixelExtent tboxExt;
        double tboxStart = xstart + (xend - xstart) * 4.0 / 9.0;
        double tboxValueStart;
        tboxExt = new PixelExtent(tboxStart, xend, stp11Ypos, stp11Ypos + 8.5
                * charHeight);
        // box background and border line
        target.drawShadedRect(tboxExt, NsharpConstants.color_black, 1f, null);
        target.drawRect(tboxExt, NsharpConstants.color_white, 1f, 1f);
        strList.clear();
        lineList.clear();
        DrawableString lb = new DrawableString("Prob EF2+ torn, supercell",
                NsharpConstants.color_white);
        lb.font = font10;
        lb.horizontalAlignment = HorizontalAlignment.LEFT;
        lb.verticallAlignment = VerticalAlignment.TOP;
        xpos = tboxStart + 0.5 * charWidth;
        ypos = stp11Ypos;
        lb.setCoordinates(xpos, ypos);
        strList.add(lb);
        DrawableString lb1 = new DrawableString("Sample CLIMO = .15 sigtor",
                NsharpConstants.color_white);
        lb1.font = font10;
        lb1.horizontalAlignment = HorizontalAlignment.LEFT;
        lb1.verticallAlignment = VerticalAlignment.TOP;
        xpos = tboxStart + 0.5 * charWidth;
        ypos = ypos + charHeight;
        lb1.setCoordinates(xpos, ypos);
        strList.add(lb1);
        DrawableLine divline = new DrawableLine();
        divline.lineStyle = LineStyle.SOLID;
        divline.basics.color = NsharpConstants.color_white;
        divline.width = 1;
        ypos = ypos + 1.2 * charHeight;
        divline.setCoordinates(tboxStart, ypos);
        divline.addPoint(xend, ypos);
        lineList.add(divline);

        // get ML parcel
        Parcel parcel = weatherDataStore.getParcelMap().get(
                NsharpLibSndglib.PARCELTYPE_MEAN_MIXING);
        String psigt_mlcape = "0.00";
        RGB mlcapeColor = NsharpConstants.color_brown;// GEMPAK color 8
        if (parcel != null) {
            float mlcape = parcel.getBplus();

            // logic statements for Thompson et al. (2012) WAF sample
            if (mlcape >= 4000) {
                psigt_mlcape = "0.16";
                mlcapeColor = NsharpConstants.color_white;// GEMPAK color 31
            } else if (mlcape >= 3000) {
                psigt_mlcape = "0.20";
                mlcapeColor = NsharpConstants.color_gold;// GEMPAK color 19
            } else if (mlcape >= 2500) {
                psigt_mlcape = "0.18";
                mlcapeColor = NsharpConstants.color_gold;// GEMPAK color 19
            } else if (mlcape >= 2000) {
                psigt_mlcape = "0.14";
                mlcapeColor = NsharpConstants.color_white;// GEMPAK color 31
            } else if (mlcape >= 1500) {
                psigt_mlcape = "0.13";
                mlcapeColor = NsharpConstants.color_white;// GEMPAK color 31
            } else if (mlcape >= 1000) {
                psigt_mlcape = "0.15";
                mlcapeColor = NsharpConstants.color_white;// GEMPAK color 31
            } else if (mlcape >= 500) {
                psigt_mlcape = "0.16";
                mlcapeColor = NsharpConstants.color_white;// GEMPAK color 31
            } else if (mlcape >= 250) {
                psigt_mlcape = "0.14";
                mlcapeColor = NsharpConstants.color_white;// GEMPAK color 31
            } else if (mlcape > 0) {
                psigt_mlcape = "0.12";
                mlcapeColor = NsharpConstants.color_darkorange;// GEMPAK color
                                                               // 18
            } else {
                psigt_mlcape = "0.00";
                mlcapeColor = NsharpConstants.color_brown;// GEMPAK color 8
            }
        }
        DrawableString lbCAPE = new DrawableString("based on CAPE: ",
                NsharpConstants.color_white);
        lbCAPE.font = font10;
        lbCAPE.horizontalAlignment = HorizontalAlignment.LEFT;
        lbCAPE.verticallAlignment = VerticalAlignment.TOP;
        xpos = tboxStart + 0.5 * charWidth;
        lbCAPE.setCoordinates(xpos, ypos);
        tboxValueStart = tboxStart + (xend - tboxStart) * 0.7;
        strList.add(lbCAPE);
        DrawableString valueCAPE = new DrawableString(psigt_mlcape, mlcapeColor);
        valueCAPE.font = font10;
        valueCAPE.horizontalAlignment = HorizontalAlignment.LEFT;
        valueCAPE.verticallAlignment = VerticalAlignment.TOP;
        valueCAPE.setCoordinates(tboxValueStart, ypos);
        strList.add(valueCAPE);

        // (given a supercell) based on MLLCL alone. Probabilities
        // are derived from Thompson et al. 2005 RUC soundings
        // based on prob_sigt_mllcl() of xwvid3.c
        float mllcl;
        if (parcel != null) {
            mllcl = NsharpLibBasics.ftom(parcel.getLclAgl());
        } else {
            mllcl = 2501; // a good number should be smaller than 2500
        }
        String psigt_mllcl;
        RGB mllclColor;
        /* logic statements for Thompson et al. (2012) WAF sample */
        if (mllcl <= 750) {
            psigt_mllcl = "0.19";
            mllclColor = NsharpConstants.color_gold;// GEMPAK color 19
        } else if (mllcl <= 1000) {
            psigt_mllcl = "0.19";
            mllclColor = NsharpConstants.color_gold;// GEMPAK color 19
        } else if (mllcl <= 1250) {
            psigt_mllcl = "0.15";
            mllclColor = NsharpConstants.color_white;// GEMPAK color 31
        } else if (mllcl <= 1500) {
            psigt_mllcl = "0.10";
            mllclColor = NsharpConstants.color_darkorange;// GEMPAK color 18
        } else if (mllcl <= 1750) {
            psigt_mllcl = "0.06";
            mllclColor = NsharpConstants.color_brown;// GEMPAK color 8
        } else if (mllcl <= 2000) {
            psigt_mllcl = "0.06";
            mllclColor = NsharpConstants.color_brown;// GEMPAK color 8
        } else if (mllcl <= 2500) {
            psigt_mllcl = "0.02";
            mllclColor = NsharpConstants.color_brown;// GEMPAK color 8
        } else {
            psigt_mllcl = "0.00";
            mllclColor = NsharpConstants.color_brown;// GEMPAK color 8
        }
        DrawableString lbLCL = new DrawableString("based on LCL: ",
                NsharpConstants.color_white);
        lbLCL.font = font10;
        lbLCL.horizontalAlignment = HorizontalAlignment.LEFT;
        lbLCL.verticallAlignment = VerticalAlignment.TOP;
        xpos = tboxStart + 0.5 * charWidth;
        ypos = ypos + charHeight;
        lbLCL.setCoordinates(xpos, ypos);
        strList.add(lbLCL);
        DrawableString valueLCL = new DrawableString(psigt_mllcl, mllclColor);
        valueLCL.font = font10;
        valueLCL.horizontalAlignment = HorizontalAlignment.LEFT;
        valueLCL.verticallAlignment = VerticalAlignment.TOP;
        valueLCL.setCoordinates(tboxValueStart, ypos);
        strList.add(valueLCL);

        // Calculates and plots the probability of an F2+ tornado
        // (given a supercell) based on effective SRH alone.
        // Probabilities are derived from Thompson et al. 2005 RUC soundings
        // based on prob_sigt_esrh() of xwvid3.c
        float esrh = 0f;
        Helicity helicity = weatherDataStore.getStormTypeToHelicityMap().get(
                "Eff Inflow");
        if (NsharpLibBasics.qc(helicity.getTotalHelicity())) {
            esrh = helicity.getTotalHelicity();
        }
        String psigt_esrh;
        RGB esrhColor;
        // logic statement for Thompson et al. (2012) WAF sample
        // 68 sigtor, 94 non-sigtor supercells
        if (esrh > 700) {
            psigt_esrh = "0.42";
            esrhColor = NsharpConstants.color_red;// GEMPAK color 2);
        }
        // 74 sigtor, 128 non-sigtor supercells
        else if (esrh > 600) {
            psigt_esrh = "0.37";
            esrhColor = NsharpConstants.color_red;// GEMPAK color 2
        }
        // 130 sigtor, 213 non-sigtor supercells
        else if (esrh > 500) {
            psigt_esrh = "0.38";
            esrhColor = NsharpConstants.color_red;// GEMPAK color 2
        }
        // 146 sigtor, 391 non-sigtor supercells
        else if (esrh >= 400) {
            psigt_esrh = "0.27";
            esrhColor = NsharpConstants.color_gold;// GEMPAK color 19
        }
        // 180 sigtor, 710 non-sigtor supercells
        else if (esrh >= 300) {
            psigt_esrh = "0.20";
            esrhColor = NsharpConstants.color_gold;// GEMPAK color 19
        }
        // 170 sigtor, 1074 non-sigtor supercells
        else if (esrh >= 200) {
            psigt_esrh = "0.14";
            esrhColor = NsharpConstants.color_white;// GEMPAK color 31
        }
        // 126 sigtor, 1440 non-sigtor supercells
        else if (esrh >= 100) {
            psigt_esrh = "0.08";
            esrhColor = NsharpConstants.color_darkorange;// GEMPAK color 18
        }
        // 44 sigtor, 711 non-sigtor supercells
        else if (esrh >= 50) {
            psigt_esrh = "0.06";
            esrhColor = NsharpConstants.color_brown;// GEMPAK color 8
        }
        // 64 sigtor, 941 non-sigtor supercells
        else {
            psigt_esrh = "0.06";
            esrhColor = NsharpConstants.color_brown;// GEMPAK color 8
        }
        DrawableString lbesrh = new DrawableString("based on ESRH:",
                NsharpConstants.color_white);
        lbesrh.font = font10;
        lbesrh.horizontalAlignment = HorizontalAlignment.LEFT;
        lbesrh.verticallAlignment = VerticalAlignment.TOP;
        xpos = tboxStart + 0.5 * charWidth;
        ypos = ypos + charHeight;
        lbesrh.setCoordinates(xpos, ypos);
        strList.add(lbesrh);
        DrawableString valueEsrh = new DrawableString(psigt_esrh, esrhColor);
        valueEsrh.font = font10;
        valueEsrh.horizontalAlignment = HorizontalAlignment.LEFT;
        valueEsrh.verticallAlignment = VerticalAlignment.TOP;
        valueEsrh.setCoordinates(tboxValueStart, ypos);
        strList.add(valueEsrh);

        // (given a supercell) based on effective bulk shear alone.
        // Probabilities are derived from Thompson et al. 2005 RUC soundings
        // based on prob_sigt_eshear() of xwvid3.c
        // get MU parcel
        float eshear = weatherDataStore.getEffShear();
        if (!NsharpLibBasics.qc(eshear)) {
            eshear = 0.0f;
        }
        String psigt_eshear;
        RGB eshearColor;
        // logic statements for Thompson et al. (2012) WAF sample
        // 17 sigtor, 49 non-sigtor supercells
        if (eshear >= 80) {
            psigt_eshear = "0.26";
            eshearColor = NsharpConstants.color_gold;// GEMPAK color 19);
        }
        // 111 sigtor, 300 non-sigtor supercells
        else if (eshear >= 70) {
            psigt_eshear = "0.36";
            eshearColor = NsharpConstants.color_red;// GEMPAK color 2
        }
        // 251 sigtor, 676 non-sigtor supercells
        else if (eshear >= 60) {
            psigt_eshear = "0.27";
            eshearColor = NsharpConstants.color_gold;// GEMPAK color 19
        }
        // 303 sigtor, 1260 non-sigtor supercells
        else if (eshear >= 50) {
            psigt_eshear = "0.19";
            eshearColor = NsharpConstants.color_gold;// GEMPAK color 19
        }
        // 218 sigtor, 1647 non-sigtor supercells
        else if (eshear >= 40) {
            psigt_eshear = "0.12";
            eshearColor = NsharpConstants.color_darkorange;// GEMPAK color 18
        }
        // 74 sigtor, 1264 non-sigtor supercells
        else if (eshear >= 30) {
            psigt_eshear = "0.06";
            eshearColor = NsharpConstants.color_brown;// GEMPAK color 8
        }
        // 23 sigtor, 437 non-sigtor supercells
        else if (eshear >= 20) {
            psigt_eshear = "0.05";
            eshearColor = NsharpConstants.color_brown;// GEMPAK color 8
        }
        // 5 sigtor, 169 non-sigtor supercells
        else if (eshear > 0) {
            psigt_eshear = "0.03";
            eshearColor = NsharpConstants.color_brown;// GEMPAK color 8
        } else {
            psigt_eshear = "0.00";
            eshearColor = NsharpConstants.color_brown;// GEMPAK color 8
        }
        DrawableString lbEshear = new DrawableString("based on EBWD:",
                NsharpConstants.color_white);
        lbEshear.font = font10;
        lbEshear.horizontalAlignment = HorizontalAlignment.LEFT;
        lbEshear.verticallAlignment = VerticalAlignment.TOP;
        xpos = tboxStart + 0.5 * charWidth;
        ypos = ypos + charHeight;
        lbEshear.setCoordinates(xpos, ypos);
        strList.add(lbEshear);
        DrawableString valueEshear = new DrawableString(psigt_eshear,
                eshearColor);
        valueEshear.font = font10;
        valueEshear.horizontalAlignment = HorizontalAlignment.LEFT;
        valueEshear.verticallAlignment = VerticalAlignment.TOP;
        valueEshear.setCoordinates(tboxValueStart, ypos);
        strList.add(valueEshear);

        DrawableLine dashDivline = new DrawableLine();
        dashDivline.lineStyle = LineStyle.DASHED;
        dashDivline.basics.color = NsharpConstants.color_white;
        dashDivline.width = 1;
        ypos = ypos + 1.2 * charHeight;
        dashDivline.setCoordinates(tboxStart, ypos);
        dashDivline.addPoint(xend, ypos);
        lineList.add(dashDivline);
        // (given a supercell) based on the Sigtor Parameter that
        // includes CIN. Probabilities are derived from Thompson et al. 2005 RUC
        // soundings
        // based on prob_sigt_stpc() of xwvid3.c
        String psigt_stpcin;
        RGB stpcColor;
        // logic statements for Thompson et al. (2012) WAF sample
        // 39 sigtor, 28 non-sigtor supercells
        if (cin >= 10) {
            // GEMPAK color 7
            psigt_stpcin = "0.58";
            stpcColor = NsharpConstants.color_magenta;

        }
        // 39 sigtor, 32 non-sigtor supercells
        else if (cin >= 8) {
            // GEMPAK color 7
            psigt_stpcin = "0.55";
            stpcColor = NsharpConstants.color_magenta;
        }
        // 54 sigtor, 104 non-sigtor supercells
        else if (cin >= 6) {
            psigt_stpcin = "0.34";
            stpcColor = NsharpConstants.color_red;// GEMPAK color 2
        }
        // 146 sigtor, 305 non-sigtor supercells
        else if (cin >= 4) {
            psigt_stpcin = "0.32";
            stpcColor = NsharpConstants.color_red;// GEMPAK color 2
        }
        // 219 sigtor, 842 non-sigtor supercells
        else if (cin >= 2) {
            psigt_stpcin = "0.21";
            stpcColor = NsharpConstants.color_gold;// GEMPAK color 19
        }
        // 200 sigtor, 963 non-sigtor supercells
        else if (cin >= 1) {
            psigt_stpcin = "0.17";
            stpcColor = NsharpConstants.color_white;// GEMPAK color 31
        }
        // 112 sigtor, 823 non-sigtor supercells
        else if (cin >= .5) {
            psigt_stpcin = "0.12";
            stpcColor = NsharpConstants.color_darkorange;// GEMPAK color 18
        }
        // 84 sigtor, 1026 non-sigtor supercells
        else if (cin > .1) {
            psigt_stpcin = "0.08";
            stpcColor = NsharpConstants.color_darkorange;// GEMPAK color 18
        }
        // 109 sigtor, 1571 non-sigtor supercells
        else {
            psigt_stpcin = "0.06";
            stpcColor = NsharpConstants.color_brown;// GEMPAK color 8
        }

        DrawableString lbStpc = new DrawableString("based on STPC:",
                NsharpConstants.color_white);
        lbStpc.font = font10;
        lbStpc.horizontalAlignment = HorizontalAlignment.LEFT;
        lbStpc.verticallAlignment = VerticalAlignment.TOP;
        xpos = tboxStart + 0.5 * charWidth;

        lbStpc.setCoordinates(xpos, ypos);
        strList.add(lbStpc);
        DrawableString valueStpc = new DrawableString(psigt_stpcin, stpcColor);
        valueStpc.font = font10;
        valueStpc.horizontalAlignment = HorizontalAlignment.LEFT;
        valueStpc.verticallAlignment = VerticalAlignment.TOP;
        valueStpc.setCoordinates(tboxValueStart, ypos);
        strList.add(valueStpc);

        // (given a supercell) based on the Sigtor Parameter.
        // Probabilities are derived from Thompson et al. 2005 RUC soundings
        // based on prob_sigt_stp() of xwvid3.c
        float stp_nocin = weatherDataStore.getStpFixed();
        String psigt_stp;
        RGB stpColor;
        // logic statements for Thompson et al. (2012) WAF sample */
        // 32 sigtor, 23 non-sigtor supercells
        if (stp_nocin >= 9) {
            psigt_stp = "0.58";
            stpColor = NsharpConstants.color_magenta;// GEMPAK color 7
        }
        // 46 sigtor, 37 non-sigtor supercells
        else if (stp_nocin >= 7) {
            psigt_stp = "0.55";
            stpColor = NsharpConstants.color_magenta;// GEMPAK color 7
        }
        // 112 sigtor, 172 non-sigtor supercells
        else if (stp_nocin >= 5) {
            psigt_stp = "0.39";
            stpColor = NsharpConstants.color_red;// GEMPAK color 2
        }
        // 172 sigtor, 515 non-sigtor supercells, >3 same as > 2
        // 177 sigtor, 541 non-sigtor supercells
        else if (stp_nocin >= 2) {
            psigt_stp = "0.25";
            stpColor = NsharpConstants.color_gold;// GEMPAK color 19
        }
        // 218 sigtor, 1091 non-sigtor supercells
        else if (stp_nocin >= 1) {
            psigt_stp = "0.17";
            stpColor = NsharpConstants.color_white;// GEMPAK color 31
        }
        // 114 sigtor, 950 non-sigtor supercells
        else if (stp_nocin >= .5) {
            psigt_stp = "0.11";
            stpColor = NsharpConstants.color_darkorange;// GEMPAK color 18
        }
        // 71 sigtor, 1107 non-sigtor supercells
        else if (stp_nocin >= .1) {
            psigt_stp = "0.06";
            stpColor = NsharpConstants.color_brown;// GEMPAK color 8
        }
        // 60 sigtor, 1266 non-sigtor supercells
        else {
            psigt_stp = "0.05";
            stpColor = NsharpConstants.color_brown;// GEMPAK color 8
        }

        // based on STP_fixed
        DrawableString lbStp = new DrawableString("based on STPF:",
                NsharpConstants.color_white);
        lbStp.font = font10;
        lbStp.horizontalAlignment = HorizontalAlignment.LEFT;
        lbStp.verticallAlignment = VerticalAlignment.TOP;
        xpos = tboxStart + 0.5 * charWidth;
        ypos = ypos + charHeight;
        lbStp.setCoordinates(xpos, ypos);
        strList.add(lbStp);
        DrawableString valueStp = new DrawableString(psigt_stp, stpColor);
        valueStp.font = font10;
        valueStp.horizontalAlignment = HorizontalAlignment.LEFT;
        valueStp.verticallAlignment = VerticalAlignment.TOP;
        valueStp.setCoordinates(tboxValueStart, ypos);
        strList.add(valueStp);
        target.drawStrings(strList.toArray(new DrawableString[strList.size()]));
        target.drawLine(lineList.toArray(new DrawableLine[lineList.size()]));

    }

    /*
     * This function is based on show_ebs_stats() in xwvid3.c of BigNsharp
     */
    private void plotEBS(int side) throws VizException {
        List<DrawableLine> lineList = new ArrayList<>();
        List<DrawableString> strList = new ArrayList<>();
        this.font11.setSmoothing(false);
        this.font11.setScaleFont(false);
        setXyStartingPosition(side);
        DrawableString titleStr = new DrawableString(
                "Effective Bulk Wind Difference(kt,Yaxis)",
                NsharpConstants.color_white);
        titleStr.font = font12;
        titleStr.horizontalAlignment = HorizontalAlignment.LEFT;
        titleStr.verticallAlignment = VerticalAlignment.TOP;
        xpos = xstart;
        ypos = ystart;
        titleStr.setCoordinates(xpos, ypos);
        strList.add(titleStr);

        DrawableString subTStr1 = new DrawableString(
                "supercell mrgl_supercell(dashed) ",
                NsharpConstants.color_lawngreen);
        ypos = ypos + 1.5 * charHeight;
        subTStr1.font = font10;
        subTStr1.horizontalAlignment = HorizontalAlignment.LEFT;
        subTStr1.verticallAlignment = VerticalAlignment.TOP;
        subTStr1.setCoordinates(xpos, ypos);
        strList.add(subTStr1);
        DrawableString subTStr2 = new DrawableString("non-supercell",
                NsharpConstants.color_darkorange);
        subTStr2.font = font10;
        subTStr2.horizontalAlignment = HorizontalAlignment.LEFT;
        subTStr2.verticallAlignment = VerticalAlignment.TOP;
        xpos = xpos + (target.getStringsBounds(subTStr1).getWidth()) * hRatio;
        subTStr2.setCoordinates(xpos, ypos);
        strList.add(subTStr2);

        ypos = ypos + 1.5 * charHeight;
        // ----- Plot Y-Coordinate hash marks, 0 - 70 kt -----
        int maxval = 70;
        // knotDist = one Knot distance in Y-axis
        // 70 Knots in total at Y axis
        double knotDist = (spcYEnd - 2 * charHeight - ypos) / 70.0;
        for (int i = maxval; i >= 0; i = i - 10) {
            DrawableString lb = new DrawableString(Integer.toString(i),
                    NsharpConstants.color_vanilla);
            lb.font = font10;
            lb.horizontalAlignment = HorizontalAlignment.LEFT;
            lb.verticallAlignment = VerticalAlignment.MIDDLE;
            xpos = xstart;
            lb.setCoordinates(xpos, ypos);
            strList.add(lb);
            DrawableLine line = new DrawableLine();
            line.lineStyle = LineStyle.DASHED;
            line.basics.color = NsharpConstants.color_dodgerblue4;
            line.width = 1;
            xpos = xpos + 2 * charWidth;
            line.setCoordinates(xpos, ypos);
            line.addPoint(xend, ypos);
            lineList.add(line);
            ypos = ypos + 10 * knotDist;
        }

        double xgap = (spcFrameWidth - 5 * charWidth) / 10;
        ypos = spcYEnd - 0.5 * charHeight;
        xpos = xstart;
        double cellYPosStart = spcYEnd - 2 * charHeight;
        // supercell line
        DrawableLine supercellline = new DrawableLine();
        supercellline.lineStyle = LineStyle.SOLID;
        supercellline.basics.color = NsharpConstants.color_lawngreen;
        supercellline.width = 2;
        // meglsupercell line
        DrawableLine mrglSupercellline = new DrawableLine();
        mrglSupercellline.lineStyle = LineStyle.DASHED;
        mrglSupercellline.basics.color = NsharpConstants.color_lawngreen;
        mrglSupercellline.width = 2;// nonsupercell line
        DrawableLine nonSupercellline = new DrawableLine();
        nonSupercellline.lineStyle = LineStyle.SOLID;
        nonSupercellline.basics.color = NsharpConstants.color_darkorange;
        nonSupercellline.width = 2;
        for (int i = 10; i <= 100; i = i + 10) {
            xpos = xpos + xgap;
            // lb for x-axis number
            DrawableString lb = new DrawableString(Integer.toString(i),
                    NsharpConstants.color_white);
            lb.font = font10;
            lb.horizontalAlignment = HorizontalAlignment.CENTER;
            lb.verticallAlignment = VerticalAlignment.BOTTOM;
            lb.setCoordinates(xpos, ypos);
            strList.add(lb);
            int cellIndex = i / 10 - 1;
            nonSupercellline.addPoint(xpos, cellYPosStart
                    - nonSupercell[cellIndex] * knotDist);
            supercellline.addPoint(xpos, cellYPosStart - supercell[cellIndex]
                    * knotDist);
            mrglSupercellline.addPoint(xpos, cellYPosStart
                    - mrglSupercell[cellIndex] * knotDist);
        }
        lineList.add(nonSupercellline);
        lineList.add(supercellline);
        lineList.add(mrglSupercellline);

        Map<Integer, Float> esbMap = weatherDataStore.getEbsMap();

        if (esbMap.size() <= 0) {
            xpos = xstart + 0.2 * spcFrameWidth;
            DrawableString lb = new DrawableString("No Effective Inflow Layer",
                    NsharpConstants.color_yellow);
            lb.font = font12;
            lb.horizontalAlignment = HorizontalAlignment.LEFT;
            lb.verticallAlignment = VerticalAlignment.TOP;
            lb.setCoordinates(xpos, cellYPosStart - 7 * knotDist);
            strList.add(lb);
        } else {
            xpos = xstart;
            DrawableLine ebsline = new DrawableLine();
            ebsline.lineStyle = LineStyle.SOLID;
            ebsline.basics.color = NsharpConstants.color_yellow;
            ebsline.width = 3;
            for (int i = 10; i <= 100; i = i + 10) {
                xpos = xpos + xgap;
                Float ebs = esbMap.get(i);
                if (ebs == null) {
                    continue;
                }
                ebsline.addPoint(xpos, cellYPosStart - ebs * knotDist);
            }
            lineList.add(ebsline);
        }
        // x-axis mark number
        target.drawStrings(strList.toArray(new DrawableString[strList.size()]));
        target.drawLine(lineList.toArray(new DrawableLine[lineList.size()]));
    }

    @Override
    protected void paintInternal(IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {
        super.paintInternal(target, paintProps);

        if (rscHandler == null || rscHandler.getSoundingLys() == null
                || !rscHandler.isGoodData())
            return;
        this.font10.setSmoothing(false);
        this.font10.setScaleFont(false);
        hRatio = paintProps.getView().getExtent().getWidth()
                / paintProps.getCanvasBounds().width;
        DrawableLine line = new DrawableLine();
        line.setCoordinates(spcRightXOrig, spcYOrig);
        line.addPoint(spcRightXOrig, spcYOrig + spcHeight);
        line.lineStyle = LineStyle.SOLID;
        line.basics.color = NsharpConstants.color_white;
        line.width = 1;
        target.drawLine(line);
        PixelExtent spcExt = new PixelExtent(new Rectangle((int) spcLeftXOrig,
                (int) spcYOrig, (int) spcWidth, (int) spcHeight));
        target.drawRect(spcExt, NsharpConstants.color_white, 1f, 1f);
        switch (leftGraph) {
        case EBS:
            plotEBS(left);
            break;
        case STP:
            plotSTP(left);
            break;
        case SHIP:
            plotSHIP(left);
            break;
        case FIRE:
            plotFire(left);
            break;
        case WINTER:
            plotWinter(left);
            break;
        case HAIL:
            plotHail(left);
            break;
        case SARS:
            plotSars(left);
            break;
        case VROT:
        	plotVrot(left);
            break;
        case CONDTOR:
            plotCondTor(left);
            break;
        default:
        	plotEBS(left);
            break;
        }
        switch (rightGraph) {
        case EBS:
            plotEBS(right);
            break;
        case STP:
            plotSTP(right);
            break;
        case SHIP:
            plotSHIP(right);
            break;
        case FIRE:
            plotFire(right);
            break;
        case WINTER:
            plotWinter(right);
            break;
        case HAIL:
            plotHail(right);
            break;
        case SARS:
            plotSars(right);
            break;
        case VROT:
        	plotVrot(right);
            break;
        case CONDTOR:
        	plotCondTor(right);
            break;
        default:
        	plotEBS(right);
            break;
        }

    }

    @Override
    protected void initInternal(IGraphicsTarget target) throws VizException {
        super.initInternal(target);
    }

    @Override
    protected void disposeInternal() {
        super.disposeInternal();
    }

    public NsharpConstants.SPCGraph getLeftGraph() {
        return leftGraph;
    }

    public void setGraphs(NsharpConstants.SPCGraph leftGraph,
            NsharpConstants.SPCGraph rightGraph, float userVrot) {
        this.leftGraph = leftGraph;
        this.rightGraph = rightGraph;
        this.userVrotValue=userVrot;
        rscHandler.refreshPane();
    }

    public NsharpConstants.SPCGraph getRightGraph() {
        return rightGraph;
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
        defineCharHeight(font10);
        spcLeftXOrig = (ext.getMinX());
        spcYOrig = ext.getMinY();
        spcWidth = (ext.getWidth());
        spcFrameWidth = spcWidth / 2;
        spcRightXOrig = spcLeftXOrig + spcFrameWidth;
        spcHeight = ext.getHeight();
        spcYEnd = ext.getMaxY();
    }

}
