/*
 * gov.noaa.nws.ncep.ui.pgen.sigmet.AbstractSigmet
 * 
 * September 2009
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.ui.pgen.sigmet;

import java.awt.Color;
import java.util.ArrayList;

import org.eclipse.ui.PlatformUI;

import com.vividsolutions.jts.geom.Coordinate;

import gov.noaa.nws.ncep.ui.pgen.PgenConstant;
import gov.noaa.nws.ncep.ui.pgen.PgenUtil;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.SigmetCommAttrDlg;
import gov.noaa.nws.ncep.ui.pgen.elements.Line;
import gov.noaa.nws.ncep.ui.pgen.display.FillPatternList.FillPattern;

/**
 * Base class for Intl Sigmet,Conv/non-Conv Sigmet, Airmet, and Outlook.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 12/09 160                Gang Zhang  Initial Creation.
 * 01/10 182                G. Zhang    Added ConvSigmet support
 * 04/11 ?                  B. Yin      Re-factor IAttribute
 * 03/12        #676        Q. Zhou     Added Issue Office field.
 * 11/07/2019   70910       smanoj      Convective SIGMET Regional Setting 
 *                                      remember previous selection
 * 01/07/2020   71971       smanoj      Modified code to use PgenConstants
 * 
 * </pre>
 * 
 * @author gzhang
 */

public abstract class AbstractSigmet extends Line implements ISigmet {

    public static final String AREA = "Area", LINE = "Line",
            ISOLATED = "Isolated";

    private String type;

    private double width;

    private String editableAttrArea;

    private String editableAttrIssueOffice;

    private String editableAttrId;

    private String editableAttrSeqNum;

    private String editableAttrFromLine;

    public AbstractSigmet() {

    }

    public AbstractSigmet(Coordinate[] range, Color[] colors, float lineWidth,
            double sizeScale, boolean closed, boolean filled,
            ArrayList<Coordinate> linePoints, int smoothFactor,
            FillPattern fillPattern, String pgenCategory, String pgenType,
            String type, double width, String editableAttrArea,
            String editableAttrIssueOffice, String editableAttrFromLine,
            String editableAttrId, String editableAttrSeqNum) {

        super(range, colors, lineWidth, sizeScale, closed, filled, linePoints,
                smoothFactor, fillPattern, pgenCategory, pgenType);

        this.type = type;
        this.width = width;

        this.editableAttrArea = editableAttrArea;
        this.editableAttrIssueOffice = editableAttrIssueOffice;
        this.editableAttrFromLine = editableAttrFromLine;
        this.editableAttrId = editableAttrId;
        this.editableAttrSeqNum = editableAttrSeqNum;
    }

    @Override
    public String getPatternName() {
        if (PgenConstant.TYPE_CCFP_SIGMET.equalsIgnoreCase(getPgenType())) {
            if ("Line".equalsIgnoreCase(getType())) {
                return "LINE_SOLID";
            } else if ("LineMed".equalsIgnoreCase(getType())) {
                return "LINE_DASHED_3";
            } else {
                return getPgenType();
            }
        }
        return getPgenType();
    }

    public String getType() {
        return type;
    }

    public String getLineType() {
        return getType();
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    /**
     * convert to nautical miles in meters
     */
    public void setWidth(String aWidth) {
        try {
            setWidth(PgenUtil.NM2M * Double.parseDouble(aWidth));
        } catch (NumberFormatException e) {
            setWidth(PgenUtil.NM2M * 10.0);
        }
    }

    public String getEditableAttrArea() {
        return editableAttrArea;
    }

    public void setEditableAttrArea(String editableAttrArea) {
        this.editableAttrArea = editableAttrArea;
    }

    public String getEditableAttrIssueOffice() {
        return editableAttrIssueOffice;
    }

    public void setEditableAttrIssueOffice(String editableAttrIssueOffice) {
        this.editableAttrIssueOffice = editableAttrIssueOffice;
    }

    public String getEditableAttrId() {
        if (editableAttrId == null) {
            SigmetCommAttrDlg sigComAttrDlg = SigmetCommAttrDlg
                    .getInstance(PlatformUI.getWorkbench()
                            .getActiveWorkbenchWindow().getShell());
            editableAttrId = sigComAttrDlg.getEditableAttrId();
        }
        return editableAttrId;
    }

    public void setEditableAttrId(String editableAttrId) {
        this.editableAttrId = editableAttrId;
    }

    public String getEditableAttrSeqNum() {
        return editableAttrSeqNum;
    }

    public void setEditableAttrSeqNum(String editableAttrSeqNum) {
        this.editableAttrSeqNum = editableAttrSeqNum;
    }

    public String getEditableAttrFromLine() {
        return editableAttrFromLine;
    }

    public void setEditableAttrFromLine(String editableAttrFromLine) {
        this.editableAttrFromLine = editableAttrFromLine;
    }

    public boolean isWithTopText() {
        return getPgenType().equalsIgnoreCase(PgenConstant.TYPE_CONV_SIGMET)
                || getPgenType().equalsIgnoreCase(PgenConstant.TYPE_OUTL_SIGMET);
    }

    public String getTopText() {

        if (getPgenType().equalsIgnoreCase(PgenConstant.TYPE_CONV_SIGMET)) {

            StringBuilder sb = new StringBuilder();
            if (this.getEditableAttrSeqNum() == null
                    || this.getEditableAttrSeqNum().length() == 0) {
                sb.append("0");
            } else {
                sb.append(this.getEditableAttrSeqNum());
            }

            if (this.getEditableAttrId() == null) {
                sb.append("E");
            } else {
                sb.append(this.getEditableAttrId().charAt(0));
            }
            return sb.toString();
        } else if (getPgenType().equalsIgnoreCase(PgenConstant.TYPE_OUTL_SIGMET)) {
            if (this.getEditableAttrSeqNum() == null
                    || this.getEditableAttrSeqNum().length() == 0)
                return "0";
            return this.getEditableAttrSeqNum();

        } else {
            return "";
        }
    }

    /**
     * To be Overridden by VolcanoAshCloud Element
     */
    public String[] getDisplayTxt() {

        String[] ss = this.getType().split(SigmetInfo.LINE_SEPERATER);

        return ss.length > 2 ? new String[] { ss[1], ss[2] }
            : ss.length > 1 ? new String[] { ss[1] }
            : new String[] { ss[0] };
    }
}
