package gov.noaa.nws.ncep.standalone.xmlConverter;

import gov.noaa.nws.ncep.ui.pgen.elements.Arc;
import gov.noaa.nws.ncep.ui.pgen.elements.AvnText;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableElement;
import gov.noaa.nws.ncep.ui.pgen.elements.KinkLine;
import gov.noaa.nws.ncep.ui.pgen.elements.Line;
import gov.noaa.nws.ncep.ui.pgen.elements.MidCloudText;
import gov.noaa.nws.ncep.ui.pgen.elements.SinglePointElement;
import gov.noaa.nws.ncep.ui.pgen.elements.Text;
import gov.noaa.nws.ncep.ui.pgen.elements.Track;
import gov.noaa.nws.ncep.ui.pgen.elements.Vector;
import gov.noaa.nws.ncep.ui.pgen.gfa.Gfa;
import gov.noaa.nws.ncep.ui.pgen.sigmet.Sigmet;
import gov.noaa.nws.ncep.ui.pgen.sigmet.Volcano;
import gov.noaa.nws.ncep.ui.pgen.tca.BPGeography;
import gov.noaa.nws.ncep.ui.pgen.tca.BreakpointPair;
import gov.noaa.nws.ncep.ui.pgen.tca.IslandBreakpoint;
import gov.noaa.nws.ncep.ui.pgen.tca.TCAElement;
import gov.noaa.nws.ncep.ui.pgen.tca.TropicalCycloneAdvisory;
import gov.noaa.nws.ncep.ui.pgen.tca.WaterBreakpoint;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

/**
 * ToTag
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 3/15/2010   137         Q. Zhou     Initial created
 * 4/21/2010   137         Q. Zhou     Add watch.
 * 5/18/2010   137         Q. Zhou     modified to suit TestXmlConvert.java
 * 8/12/2010               Q. Zhou     modified contours group number -- count
 * 9/09/2010   137         Q. Zhou     Tuning for elements.  Added vol and ash.
 * 10/12/2010  137         Q. Zhou     Added Outlook, Cloud, and Turbulence.
 * 10/20/2010  137         Q. Zhou     Added Jet text and hash rotations correction to match CAVE
 * 11/2/2010   137         Q. Zhou     Added ccfp.   Added labeledSymbol.
 * 12/2/2010   137         Q. Zhou     Added Symbol to contour
 * 									   Modified all text part for rotn, text string et al. Modified Front code.	
 * 01/06/2011  137         Q. Zhou     Fixed width problem of vec and jet's vec. Fixed gfa typo.
 *                                     Fixed the float and int problem of front pipsiz.
 *                                     Fixed volcano getPoints problem duo to the removing of isconverter environment.
 * 01/13/2011  137         Q. Zhou     watch read mzcntys.xml instead county.xml.  Modified gfa.
 * 01/31/2011  137         Q. Zhou     modified gfa from general string to specific hazard string
 * 02/08/2011  137         Q. Zhou     added 500 points check for line and spline.
 * 02/22/2011  137         Q. Zhou     removed deprecation methods for time
 * 03/16/2011  137         Q. Zhou     handle gfa nblocks, 1 nblocks=1024
 * 3/24/2011               Q. Zhou     Added parameter category to getSymType(). for HPC
 * 6/8/2011                Q. Zhou     Added cast to de. Modified gfa on snap, smear & outlook checking
 * 11/1/2011   137         Q. Zhou     Added displayType and removed outline for Text.
 * 11/8/2011   137         Q. Zhou     Modified getDeTrack on extra time logic (modified min and hour).
 * 11/10/2011              Q. Zhou     Removed CCF to ToTagCollection
 * 08/30/2012  TTR183      Q. Zhou     Changed sigmet/vac line distance unit.
 * 10/25/2012  916 TTR657  Q. Zhou     Modified sigmet. Corrected tops default. Corrected dropdown list value.
 * 11/22/2013  1065-TTR850 J. Wu       Added kink lines.
 * 10/17/2014  TTR 657     J. Wu       Correct some defaults for CONV_SIGMET.
 * 02/15       R6158       J. Wu       Add iwidth/ithw to preserve them for legacy Text.
 * 02/24/2016  R13544      S.Russell   Updated getDeFront() to retrieve the 
 *                                     flipSide value from the input argument
 *                                     (rather than hard code it )
 *                                     and to assign it to string tags used to
 *                                     create a *.tag file
 * </pre>
 * 
 * @author Q. Zhou
 * @version 1
 */

public class ToTag {

    public static String tagVGF(String vgf, DrawableElement de) {

        if (de.getPgenCategory().equalsIgnoreCase("Lines")
                && (de.getPgenType().equalsIgnoreCase("LINE_SOLID") || de
                        .getPgenType().startsWith("LINE_DASHED"))) {
            vgf = getDeLine(de);
        } else if (de.getPgenCategory().equalsIgnoreCase("Lines")) {
            vgf = getDeSpLine(de);

        } else if (de.getPgenCategory().equalsIgnoreCase("Front")) {
            vgf = getDeFront(de);

        } else if (de.getPgenCategory().equalsIgnoreCase("Symbol")
                || de.getPgenCategory().equalsIgnoreCase("Combo")
                || de.getPgenCategory().equalsIgnoreCase("Marker")) {
            vgf = getDeSymbol(de);

        } else if (de.getPgenCategory().equalsIgnoreCase("Vector")) {
            vgf = getDeVector(de);

        } else if (de.getPgenCategory().equalsIgnoreCase("Text")) {
            vgf = getDeText(de);

        } else if (de.getPgenCategory().equalsIgnoreCase("Arc")) {
            vgf = getDeArc(de);

        } else if (de.getPgenCategory().equalsIgnoreCase("Track")) {
            vgf = getDeTrack(de);

        } else if (de.getPgenCategory().equalsIgnoreCase("SIGMET")
                && de.getPgenType().equalsIgnoreCase("VOLC_SIGMET")) {
            vgf = getDeVol(de);

        } else if (de.getPgenCategory().equalsIgnoreCase("SIGMET")
                && de.getPgenType().equalsIgnoreCase("VACL_SIGMET")) {
            vgf = getDeVac(de);

        } else if (de.getPgenCategory().equalsIgnoreCase("SIGMET")) {
            vgf = getDeSigmet(de);

        } else if (de.getPgenCategory().equalsIgnoreCase("MET")
                && de.getPgenType().equalsIgnoreCase("TCA")) { // tca
            vgf = getDeTca(de);

        } else if (de.getPgenCategory().equalsIgnoreCase("MET")
                && de.getPgenType().equalsIgnoreCase("GFA")) { // gfa
            vgf = getDeGfa(de);

        }

        return vgf;
    }

    private static String getDeGfa(DrawableElement de) {
        String vgf = "!\n";

        String hazard = ((Gfa) de).getGfaHazard();
        String fcst = ((Gfa) de).getGfaFcstHr().trim();
        String layout = "";

        if (hazard.equalsIgnoreCase("IFR") && fcst.length() <= 2) // SNAPSHOT
            layout = "STA;FHR::ZFHR::TAG;HZD;WX|7";
        else if (hazard.equalsIgnoreCase("IFR") && fcst.length() > 2) // non
                                                                      // SNAPSHOT
            layout = "STA;FHR::TAG;HZD;WX|7";
        else if (hazard.equalsIgnoreCase("MVFR"))
            layout = "STA;FHR::ZFHR::TAG;CVR;BLW;VIS;WX|7";
        else if (hazard.equalsIgnoreCase("CLD"))
            layout = "STA;FHR::ZFHR::TAG;CVR;WX;HZD;BSE";
        else if (hazard.equalsIgnoreCase("CLD_TOPS"))
            layout = "STA;FHR::ZFHR::TAG;HZD;TOP;"; // vgf has end ";"
        else if (hazard.equalsIgnoreCase("MT_OBSC") && fcst.length() <= 2)
            layout = "STA;FHR::ZFHR::TAG;SSYM|52;WX|7";
        else if (hazard.equalsIgnoreCase("MT_OBSC") && fcst.length() > 2)
            layout = "STA;FHR::TAG;SSYM|52;WX|7";
        else if (hazard.equalsIgnoreCase("ICE") && fcst.length() <= 2)
            layout = "STA;FHR::ZFHR::TAG;ISYM|5;TOP;BSE";
        else if (hazard.equalsIgnoreCase("ICE") && fcst.length() > 2)
            layout = "STA;FHR::TAG;ISYM|5;TOP;BSE";
        else if (hazard.startsWith("TURB") && fcst.length() <= 2)
            layout = "STA;FHR::ZFHR::TAG;TSYM|4;TOP;BSE";
        else if (hazard.startsWith("TURB") && fcst.length() > 2)
            layout = "STA;FHR::TAG;TSYM|4;TOP;BSE";
        else if (hazard.equalsIgnoreCase("SFC_WND") && fcst.length() <= 2)
            layout = "STA;FHR::ZFHR::TAG;SSYM|54/1.2/3/2";
        else if (hazard.equalsIgnoreCase("SFC_WND") && fcst.length() > 2)
            layout = "STA;FHR::TAG;SSYM|54/1.2/3/2";
        else if (hazard.equalsIgnoreCase("SIGWX"))
            layout = "STA;FHR::ZFHR::TAG;HZD";
        else if (hazard.equalsIgnoreCase("CIG_CLD"))
            layout = "STA;FHR::ZFHR::TAG;CRV;WX|7;HZD;TOP;BSE";
        else if (hazard.equalsIgnoreCase("TCU_CLD"))
            layout = "STA;FHR::ZFHR::TAG;HZD";
        else if (hazard.equalsIgnoreCase("MTW"))
            layout = "STA;FHR::ZFHR::TAG;ITS;HZD;TOP;BSE";
        else if (hazard.equalsIgnoreCase("FZLVL") && fcst.length() <= 2)
            layout = "STA;FHR::ZFHR::TAG;HZD:LVL";
        else if (hazard.equalsIgnoreCase("FZLVL") && fcst.length() > 2)
            layout = "STA;FHR::TAG;HZD:LVL";
        else if (hazard.equalsIgnoreCase("M_FZLVL") && fcst.length() <= 2)
            layout = "STA;FHR::ZFHR::TAG;HZD;TOP;BSE";
        else if (hazard.equalsIgnoreCase("M_FZLVL") && fcst.length() > 2)
            layout = "STA;FHR::TAG;HZD;TOP;BSE";
        else if (hazard.equalsIgnoreCase("LLWS") && fcst.length() <= 2)
            layout = "STA;FHR::ZFHR::TAG;HZD";
        else if (hazard.equalsIgnoreCase("LLWS") && fcst.length() > 2)
            layout = "STA;FHR::TAG;HZD";
        else if (hazard.equalsIgnoreCase("TS"))
            layout = "STA;FHR::ZFHR::TAG;CTG;FRQ;HZD::WX;TOP;BSE";
        else
            layout = "STA;FHR::ZFHR::TAG;HZD:LVL";

        String linetype = "";
        String lineElm = "";
        String level = "";
        if (((Gfa) de).getGfaValue("Level") != null)
            level = ((Gfa) de).getGfaValue("Level");

        if (hazard.equalsIgnoreCase("FZLVL") && level.equalsIgnoreCase("SFC")) {
            linetype = "33";
            lineElm = "20";
        } else if (hazard.equalsIgnoreCase("FZLVL")) {
            linetype = "34";
            lineElm = "20";
        } else {
            linetype = "1";
            lineElm = "1";
        }

        String subType = "";

        String[] hr = fcst.split("-"); // 3, 6, 0-6, 9-9, 12-12, other
        boolean outlook = ((Gfa) de).isOutlook();

        if (hazard.equalsIgnoreCase("IFR") && hr.length == 1) // SNAPSHOT
            subType = "10";
        else if (hazard.equalsIgnoreCase("IFR") && hr.length > 1 && !outlook) // smear
            subType = "15";
        else if (hazard.equalsIgnoreCase("IFR")) // outlook
            subType = "16";

        else if (hazard.equalsIgnoreCase("MVFR") && hr.length == 1)
            subType = "180";
        else if (hazard.equalsIgnoreCase("MVFR") && hr.length > 1 && !outlook)
            subType = "185";
        else if (hazard.equalsIgnoreCase("MVFR"))
            subType = "186";

        else if (hazard.equalsIgnoreCase("CLD") && hr.length == 1)
            subType = "120";
        else if (hazard.equalsIgnoreCase("CLD") && hr.length > 1 && !outlook)
            subType = "125";
        else if (hazard.equalsIgnoreCase("CLD"))
            subType = "126";

        else if (hazard.equalsIgnoreCase("CLD_TOPS") && hr.length == 1)
            subType = "250";
        else if (hazard.equalsIgnoreCase("CLD_TOPS") && hr.length > 1
                && !outlook)
            subType = "255";
        else if (hazard.equalsIgnoreCase("CLD_TOPS"))
            subType = "256";

        else if (hazard.equalsIgnoreCase("MT_OBSC") && hr.length == 1)
            subType = "20";
        else if (hazard.equalsIgnoreCase("MT_OBSC") && hr.length > 1
                && !outlook)
            subType = "25";
        else if (hazard.equalsIgnoreCase("MT_OBSC"))
            subType = "26";

        else if (hazard.equalsIgnoreCase("ICE") && hr.length == 1)
            subType = "30";
        else if (hazard.equalsIgnoreCase("ICE") && hr.length > 1 && !outlook)
            subType = "35";
        else if (hazard.equalsIgnoreCase("ICE"))
            subType = "36";

        else if (hazard.equalsIgnoreCase("TURB") && hr.length == 1)
            subType = "40";
        else if (hazard.equalsIgnoreCase("TURB") && hr.length > 1 && !outlook)
            subType = "45";
        else if (hazard.equalsIgnoreCase("TURB"))
            subType = "46";

        else if (hazard.equalsIgnoreCase("TURB_HI") && hr.length == 1)
            subType = "50";
        else if (hazard.equalsIgnoreCase("TURB_HI") && hr.length > 1
                && !outlook)
            subType = "55";
        else if (hazard.equalsIgnoreCase("TURB_HI"))
            subType = "56";

        else if (hazard.equalsIgnoreCase("TURB_LO") && hr.length == 1)
            subType = "60";
        else if (hazard.equalsIgnoreCase("TURB_LO") && hr.length > 1
                && !outlook)
            subType = "65";
        else if (hazard.equalsIgnoreCase("TURB_LO"))
            subType = "66";

        else if (hazard.equalsIgnoreCase("SFC_WND") && hr.length == 1
                && ((Gfa) de).getGfaValue("Speed").equalsIgnoreCase("30"))
            subType = "70";
        else if (hazard.equalsIgnoreCase("SFC_WND") && hr.length > 1
                && !outlook
                && ((Gfa) de).getGfaValue("Speed").equalsIgnoreCase("30"))
            subType = "75";
        else if (hazard.equalsIgnoreCase("SFC_WND")
                && ((Gfa) de).getGfaValue("Speed").equalsIgnoreCase("30"))
            subType = "76";

        else if (hazard.equalsIgnoreCase("SFC_WND") && hr.length == 1
                && ((Gfa) de).getGfaValue("Speed").equalsIgnoreCase("20"))
            subType = "190";
        else if (hazard.equalsIgnoreCase("SFC_WND") && hr.length > 1
                && !outlook)
            subType = "195";
        else if (hazard.equalsIgnoreCase("SFC_WND"))
            subType = "196";

        else if (hazard.equalsIgnoreCase("SIGWX") && hr.length == 1)
            subType = "80";
        else if (hazard.equalsIgnoreCase("SIGWX") && hr.length > 1 && !outlook)
            subType = "85";
        else if (hazard.equalsIgnoreCase("SIGWX"))
            subType = "86";

        else if (hazard.equalsIgnoreCase("CIG_CLD") && hr.length == 1)
            subType = "90";
        else if (hazard.equalsIgnoreCase("CIG_CLD") && hr.length > 1
                && !outlook)
            subType = "95";
        else if (hazard.equalsIgnoreCase("CIG_CLD"))
            subType = "96";

        else if (hazard.equalsIgnoreCase("TCU_CLD") && hr.length == 1)
            subType = "100";
        else if (hazard.equalsIgnoreCase("TCU_CLD") && hr.length > 1
                && !outlook)
            subType = "105";
        else if (hazard.equalsIgnoreCase("TCU_CLD"))
            subType = "106";

        else if (hazard.equalsIgnoreCase("MTW") && hr.length == 1)
            subType = "110";
        else if (hazard.equalsIgnoreCase("MTW") && hr.length > 1 && !outlook)
            subType = "115";
        else if (hazard.equalsIgnoreCase("MTW"))
            subType = "116";

        else if (hazard.equalsIgnoreCase("FZLVL") && hr.length == 1
                && level.equalsIgnoreCase("SFC"))
            subType = "130";
        else if (hazard.equalsIgnoreCase("FZLVL") && hr.length > 1 && !outlook
                && level.equalsIgnoreCase("SFC"))
            subType = "135";
        else if (hazard.equalsIgnoreCase("FZLVL")
                && level.equalsIgnoreCase("SFC"))
            subType = "136";

        else if (hazard.equalsIgnoreCase("FZLVL") && hr.length == 1)
            subType = "140";
        else if (hazard.equalsIgnoreCase("FZLVL") && hr.length > 1 && !outlook)
            subType = "145";
        else if (hazard.equalsIgnoreCase("FZLVL"))
            subType = "146";

        else if (hazard.equalsIgnoreCase("M_FZLVL") && hr.length == 1)
            subType = "150";
        else if (hazard.equalsIgnoreCase("M_FZLVL") && hr.length > 1
                && !outlook)
            subType = "155";
        else if (hazard.equalsIgnoreCase("M_FZLVL"))
            subType = "156";

        else if (hazard.equalsIgnoreCase("LLWS") && hr.length == 1)
            subType = "160";
        else if (hazard.equalsIgnoreCase("LLWS") && hr.length > 1 && !outlook)
            subType = "165";
        else if (hazard.equalsIgnoreCase("LLWS"))
            subType = "166";

        else if (hazard.equalsIgnoreCase("TS") && hr.length == 1)
            subType = "170";
        else if (hazard.equalsIgnoreCase("TS") && hr.length > 1 && !outlook)
            subType = "175";
        else if (hazard.equalsIgnoreCase("TS"))
            subType = "176";

        String s = ((Gfa) de).getGfaValue("FZL Top/Bottom");
        String[] fzl = null;
        String top = "", bott = "";
        if (s != null) {
            fzl = s.split("/");
            if (fzl != null && fzl.length > 1) {
                top = fzl[0];
                bott = fzl[1];
            } else if (fzl != null)
                top = fzl[0];
        } else {
            top = "";
            bott = "";
        }

        GeometryFactory factory = new GeometryFactory();
        Coordinate[] arrow = new Coordinate[de.getPoints().size() + 1];
        de.getPoints().toArray(arrow);
        arrow[arrow.length - 1] = arrow[0]; // add the first point to the end
        LineString g = factory.createLineString(arrow);
        Point p = g.getCentroid();

        String hazardStr = "";
        if (hazard.equalsIgnoreCase("IFR")) {
            hazardStr = VgfTags.CIG + getGfaCigVis(de, "CIG") + VgfTags.VIS
                    + getGfaCigVis(de, "VIS") + VgfTags.Type
                    + ((Gfa) de).getGfaType();
        } else if (hazard.equalsIgnoreCase("MVFR")
                || hazard.equalsIgnoreCase("SIGWX")
                || hazard.equalsIgnoreCase("CIG_CLD")
                || hazard.equalsIgnoreCase("TCU_CLD")
                || hazard.equalsIgnoreCase("LLWS"))
            hazardStr = "";
        else if (hazard.equalsIgnoreCase("CLD"))
            hazardStr = VgfTags.Coverage
                    + (((Gfa) de).getGfaValue("Coverage") == null ? ""
                            : ((Gfa) de).getGfaValue("Coverage"))
                    + VgfTags.gfa_bottom
                    + (((Gfa) de).getGfaValue("Bottom") == null ? ""
                            : ((Gfa) de).getGfaValue("Bottom"));
        else if (hazard.equalsIgnoreCase("CLD_TOPS"))
            hazardStr = VgfTags.Type
                    + ((Gfa) de).getGfaType()
                    + VgfTags.gfa_top
                    + (((Gfa) de).getGfaValue("Top") == null ? "" : ((Gfa) de)
                            .getGfaValue("Top"));
        else if (hazard.equalsIgnoreCase("MT_OBSC"))
            hazardStr = VgfTags.Type
                    + ((Gfa) de).getGfaType()
                    + VgfTags.gfa_top
                    + (((Gfa) de).getGfaValue("Top") == null ? "" : ((Gfa) de)
                            .getGfaValue("Top"))
                    + VgfTags.gfa_bottom
                    + (((Gfa) de).getGfaValue("Bottom") == null ? ""
                            : ((Gfa) de).getGfaValue("Bottom"));

        else if (hazard.equalsIgnoreCase("ICE"))
            hazardStr = VgfTags.Type
                    + (((Gfa) de).getGfaType() == null ? "ICE" : ((Gfa) de)
                            .getGfaType())
                    + VgfTags.gfa_top
                    + (((Gfa) de).getGfaValue("Top") == null ? "" : ((Gfa) de)
                            .getGfaValue("Top"))
                    + VgfTags.gfa_bottom
                    + (((Gfa) de).getGfaValue("Bottom") == null ? ""
                            : ((Gfa) de).getGfaValue("Bottom"))
                    + VgfTags.Severity
                    + (((Gfa) de).getGfaValue("Severity") == null ? "MOD"
                            : ((Gfa) de).getGfaValue("Severity"))// empty
                                                                 // Gfa.java
                                                                 // line 80
                    + VgfTags.fzlTop + top + VgfTags.fzlBottom + bott;

        else if (hazard.startsWith("TURB"))
            hazardStr = VgfTags.gfa_top
                    + (((Gfa) de).getGfaValue("Top") == null ? "" : ((Gfa) de)
                            .getGfaValue("Top"))
                    + VgfTags.gfa_bottom
                    + (((Gfa) de).getGfaValue("Bottom") == null ? ""
                            : ((Gfa) de).getGfaValue("Bottom"))
                    + VgfTags.Severity
                    + (((Gfa) de).getGfaValue("Severity") == null ? "MOD"
                            : ((Gfa) de).getGfaValue("Severity"))
                    + VgfTags.DUETO
                    + (((Gfa) de).getGfaValue("DUE TO") == null ? ""
                            : ((Gfa) de).getGfaValue("DUE TO"));

        else if (hazard.equalsIgnoreCase("SFC_WND"))
            hazardStr = VgfTags.Speed
                    + (((Gfa) de).getGfaValue("Speed") == null ? ""
                            : ((Gfa) de).getGfaValue("Speed"));

        else if (hazard.equalsIgnoreCase("MTW"))
            hazardStr = VgfTags.gfa_top
                    + (((Gfa) de).getGfaValue("Top") == null ? "" : ((Gfa) de)
                            .getGfaValue("Top"))
                    + VgfTags.gfa_bottom
                    + (((Gfa) de).getGfaValue("Bottom") == null ? ""
                            : ((Gfa) de).getGfaValue("Bottom"))
                    + VgfTags.Intensity
                    + (((Gfa) de).getGfaValue("Intensity") == null ? ""
                            : ((Gfa) de).getGfaValue("Intensity"));
        else if (hazard.equalsIgnoreCase("FZLVL"))
            hazardStr = VgfTags.Contour
                    + (((Gfa) de).getGfaValue("Contour") == null ? ""
                            : ((Gfa) de).getGfaValue("Contour"))
                    + VgfTags.Level
                    + (((Gfa) de).getGfaValue("Level") == null ? ""
                            : ((Gfa) de).getGfaValue("Level"))
                    + VgfTags.gfa_fzlRange
                    + (((Gfa) de).getGfaValue("FZL RANGE") == null ? ""
                            : ((Gfa) de).getGfaValue("FZL RANGE"));
        else if (hazard.equalsIgnoreCase("M_FZLVL"))
            hazardStr = VgfTags.gfa_top
                    + (((Gfa) de).getGfaValue("Top") == null ? "" : ((Gfa) de)
                            .getGfaValue("Top"))
                    + VgfTags.gfa_bottom
                    + (((Gfa) de).getGfaValue("Bottom") == null ? ""
                            : ((Gfa) de).getGfaValue("Bottom"));

        else if (hazard.equalsIgnoreCase("TS"))
            hazardStr = VgfTags.Type
                    + ((Gfa) de).getGfaType()
                    + VgfTags.gfa_top
                    + (((Gfa) de).getGfaValue("Top") == null ? "" : ((Gfa) de)
                            .getGfaValue("Top"))
                    + VgfTags.gfa_bottom
                    + (((Gfa) de).getGfaValue("Bottom") == null ? ""
                            : ((Gfa) de).getGfaValue("Bottom"))
                    + VgfTags.Category
                    + (((Gfa) de).getGfaValue("Category") == null ? ""
                            : ((Gfa) de).getGfaValue("Category"))
                    + VgfTags.Frequency
                    + (((Gfa) de).getGfaValue("Frequency") == null ? ""
                            : ((Gfa) de).getGfaValue("Frequency"));

        int nblocks = 1;

        vgf += VgfTags.vg_type
                + XmlUtil.getType(de.getPgenCategory(), de.getPgenType())
                + VgfTags.vg_class + "15" + VgfTags.delete + "0"
                + VgfTags.filled + 0 + VgfTags.closed
                + (hazard.equalsIgnoreCase("FZLVL") == true ? 0 : 1)
                + VgfTags.smooth + "0" + VgfTags.version + "0" + VgfTags.grptyp
                + "8" + VgfTags.grpnum + "0" + VgfTags.maj_col
                + XmlUtil.getColorTag(XmlUtil.getColorMaj(de))
                + VgfTags.min_col
                + XmlUtil.getColorTag(XmlUtil.getColorMin(de)) + VgfTags.recsz
                + "0" + VgfTags.range_min_lat + XmlUtil.getSortedLat(de)[0]
                + VgfTags.range_min_lon + XmlUtil.getSortedLon(de)[0]
                + VgfTags.range_max_lat
                + XmlUtil.getSortedLat(de)[XmlUtil.getSortedLat(de).length - 1]
                + VgfTags.range_max_lon
                + XmlUtil.getSortedLon(de)[XmlUtil.getSortedLon(de).length - 1]

                + VgfTags.gfa_nblocks + nblocks + VgfTags.gfa_npts
                + de.getPoints().size() + VgfTags.gfa_areaType + hazard
                + VgfTags.gfa_fcstHr + fcst + VgfTags.gfa_tag
                + ((Gfa) de).getGfaTag() + ((Gfa) de).getGfaDesk()
                + VgfTags.gfa_cycle + ((Gfa) de).getGfaCycleHour()
                + VgfTags.gfa_status + ((Gfa) de).getGfaIssueType();

        // Assign FA region, area, state list, and conditional wording.
        String area = (((Gfa) de).getGfaArea() == null ? "" : ((Gfa) de)
                .getGfaArea());
        String region = "";
        if (area.startsWith("SFO") || area.startsWith("SLC")) {
            region = "W";
        } else if (area.startsWith("CHI") || area.startsWith("DFW")) {
            region = "C";
        } else if (area.startsWith("BOS") || area.startsWith("MIA")) {
            region = "E";
        }

        vgf += VgfTags.gfa_region
                + region
                + VgfTags.gfa_areas
                + area
                + VgfTags.gfa_statesList
                + (((Gfa) de).getGfaStates() == null ? "" : ((Gfa) de)
                        .getGfaStates())
                + VgfTags.gfa_condsBegin
                + (((Gfa) de).getGfaBeginning() == null ? "" : ((Gfa) de)
                        .getGfaBeginning())
                + VgfTags.gfa_condsEnd
                + (((Gfa) de).getGfaEnding() == null ? "" : ((Gfa) de)
                        .getGfaEnding());

        // Assign more.
        vgf += hazardStr

                + VgfTags.gfa_lineWidth
                + ((Gfa) de).getLineWidth()
                // CAVE not implemented
                + VgfTags.gfa_subType
                + subType
                + VgfTags.gfa_linelm
                + lineElm
                + VgfTags.gfa_linetype
                + linetype
                + VgfTags.gfa_arrowSize
                + "1.00"
                + VgfTags.gfa_txtColor
                + XmlUtil.getColorTag(XmlUtil.getColorMaj(de)) // 2
                + VgfTags.gfa_txtSize + "1.00" + VgfTags.gfa_txtFont + "1"
                + VgfTags.gfa_txtHardware + "2" + VgfTags.gfa_txtWidth + "1"
                + VgfTags.gfa_txtAlign + "0" + VgfTags.gfa_txtLayout + layout
                + VgfTags.gfa_arrow_lat + p.getY() + VgfTags.gfa_arrow_lon
                + p.getX() + VgfTags.gfa_lat
                + ((Gfa) de).getGfaTextCoordinate().y + VgfTags.gfa_lon
                + ((Gfa) de).getGfaTextCoordinate().x + VgfTags.gfa_points
                + XmlUtil.getPoints(de) + "\n";

        // calculate nblocks. if nblocks>1, exchange it in the vgf string
        int iNblocks = vgf.indexOf("gfa_nblocks");
        int iNpts = vgf.indexOf("gfa_npts");
        int iLatlon = vgf.indexOf("gfa_points");

        if (iNpts != -1 && iLatlon > iNpts) {
            int firstInBlock = vgf.indexOf("<", iNpts);
            if (((iLatlon - 1) - firstInBlock) / 1024 >= 1) {
                nblocks = ((iLatlon - 1) - firstInBlock) / 1024 + 1;
            }
        }
        if (nblocks > 1) { // exchange nblocks in vgf string
            vgf = vgf.substring(0, iNblocks + 12) + nblocks
                    + vgf.substring(iNblocks + 13); // 12: gfa_nblocks+1
        }

        return vgf;
    }

    private static String getDeTca(DrawableElement de) {
        String vgf = "!\n";

        int basin = 0;
        if (((TCAElement) de).getBasin().equalsIgnoreCase("Atlantic"))
            basin = 0;
        else if (((TCAElement) de).getBasin().equalsIgnoreCase("E. Pacific"))
            basin = 1;
        else if (((TCAElement) de).getBasin().equalsIgnoreCase("C. Pacific"))
            basin = 2;
        else if (((TCAElement) de).getBasin().equalsIgnoreCase("W. Pacific"))
            basin = 3;

        int stormType = 0;
        if (((TCAElement) de).getStormType().equalsIgnoreCase("Hurricane"))
            stormType = 0;
        else if (((TCAElement) de).getStormType().equalsIgnoreCase(
                "Tropical Storm"))
            stormType = 1;
        else if (((TCAElement) de).getStormType().equalsIgnoreCase(
                "Tropical Depression"))
            stormType = 2;
        else if (((TCAElement) de).getStormType().equalsIgnoreCase(
                "Subtropical Storm"))
            stormType = 3;
        else if (((TCAElement) de).getStormType().equalsIgnoreCase(
                "Subtropical Depression"))
            stormType = 4;

        String issueStatus = "E";
        if (((TCAElement) de).getIssueStatus().equalsIgnoreCase("Experimental"))
            issueStatus = "E";
        else if (((TCAElement) de).getIssueStatus().equalsIgnoreCase(
                "Operational"))
            issueStatus = "O";
        else if (((TCAElement) de).getIssueStatus().equalsIgnoreCase(
                "Experimental Operational"))
            issueStatus = "X";
        else if (((TCAElement) de).getIssueStatus().equalsIgnoreCase("Test"))
            issueStatus = "T";

        String[] tcaText = {};
        if (((TCAElement) de).getTextLocation() != null)
            tcaText = ((TCAElement) de).getTextLocation().toString().split(",");

        ArrayList<TropicalCycloneAdvisory> list = ((TCAElement) de)
                .getAdvisories();
        int wwNum = 0;
        if (list != null)
            wwNum = list.size();

        String[] severity = new String[wwNum];
        String[] advisoryType = new String[wwNum];
        String[] geographyType = new String[wwNum];
        String[] breakPointNum = new String[wwNum];

        for (int i = 0; i < wwNum; i++) {

            severity[i] = ((TropicalCycloneAdvisory) list.get(i)).getSeverity();
            if (severity[i].equalsIgnoreCase("Tropical Storm")) // change text
                                                                // to number
                severity[i] = "0";
            else if (severity[i].equalsIgnoreCase("Hurricane"))
                severity[i] = "1";

            advisoryType[i] = ((TropicalCycloneAdvisory) list.get(i))
                    .getAdvisoryType();
            if (advisoryType[i].equalsIgnoreCase("Watch"))
                advisoryType[i] = "0";
            else if (advisoryType[i].equalsIgnoreCase("Warning"))
                advisoryType[i] = "1";

            geographyType[i] = ((TropicalCycloneAdvisory) list.get(i))
                    .getGeographyType();
            if (geographyType[i].equalsIgnoreCase("None")) {
                geographyType[i] = "0";
                breakPointNum[i] = "2";
            } else if (geographyType[i].equalsIgnoreCase("Islands")) {
                geographyType[i] = "1";
                breakPointNum[i] = "1";
            } else if (geographyType[i].equalsIgnoreCase("Water")) {
                geographyType[i] = "2";
                breakPointNum[i] = "1";
            }
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd/HHmm"); // set
                                                                    // timeZone
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date date = null;
        if (((TCAElement) de).getAdvisoryTime() != null) {
            date = ((TCAElement) de).getAdvisoryTime().getTime();
            sdf.format(date);
        }

        vgf += VgfTags.vg_type
                + XmlUtil.getType(de.getPgenCategory(), de.getPgenType())
                + VgfTags.vg_class + "15" + VgfTags.delete + "0"
                + VgfTags.filled + "0" + VgfTags.closed + "0" + VgfTags.smooth
                + "0" + VgfTags.version + "0" + VgfTags.grptyp + "8"
                + VgfTags.grpnum + "0" + VgfTags.maj_col + "0"
                + VgfTags.min_col + "0" + VgfTags.recsz + "0"
                + VgfTags.range_min_lat + XmlUtil.getSortedLat(de)[0]
                + VgfTags.range_min_lon + XmlUtil.getSortedLon(de)[0]
                + VgfTags.range_max_lat
                + XmlUtil.getSortedLat(de)[XmlUtil.getSortedLat(de).length - 1]
                + VgfTags.range_max_lon
                + XmlUtil.getSortedLon(de)[XmlUtil.getSortedLon(de).length - 1]
                + VgfTags.tca_stormNum + ((TCAElement) de).getStormNumber()
                + VgfTags.tca_issueStatus + issueStatus + VgfTags.tca_basin
                + basin + VgfTags.tca_advisoryNum
                + ((TCAElement) de).getAdvisoryNumber() + VgfTags.tca_stormName
                + ((TCAElement) de).getStormName() + VgfTags.tca_stormType
                + stormType + VgfTags.tca_validTime + sdf.format(date)
                + VgfTags.tca_timezone + ((TCAElement) de).getTimeZone()
                + VgfTags.tca_textLat + tcaText[1] + VgfTags.tca_textLon
                + tcaText[0].substring(1) + VgfTags.tca_textFont + "1"
                + VgfTags.tca_textSize + "1.0" + VgfTags.tca_textWidth + "3"
                + VgfTags.tca_wwNum + wwNum;

        for (int i = 0; i < wwNum; i++) {
            vgf += VgfTags.tca_tcawwStr_ + i + ">" + severity[i] + "|"
                    + advisoryType[i] + "|" + geographyType[i]
                    + VgfTags.tca_numBreakPts_ + i + ">" + breakPointNum[i];

            BPGeography geo = null;
            if (list != null && !list.isEmpty())
                geo = ((TropicalCycloneAdvisory) list.get(i)).getSegment();

            if (geo instanceof BreakpointPair) {
                String s = geo.getBreakpoints().get(0).getLocation().toString();
                String[] text = s.split(",");
                vgf += VgfTags.tca_breakPts_ + i + ">" + text[1] + "|"
                        + text[0].substring(1) + "|"
                        + geo.getBreakpoints().get(0).getName() + "|";
                s = geo.getBreakpoints().get(1).getLocation().toString();
                text = s.split(",");
                vgf += text[1] + "|" + text[0].substring(1) + "|"
                        + geo.getBreakpoints().get(1).getName() + "|";

            } else if (geo instanceof IslandBreakpoint) {
                String s = geo.getBreakpoints().get(0).getLocation().toString();
                String[] text = s.split(",");
                vgf += VgfTags.tca_breakPts_ + i + ">" + text[1] + "|"
                        + text[0].substring(1) + "|"
                        + geo.getBreakpoints().get(0).getName() + "|";
            } else if (geo instanceof WaterBreakpoint) {
                String s = geo.getBreakpoints().get(0).getLocation().toString();
                String[] text = s.split(",");
                vgf += VgfTags.tca_breakPts_ + i + ">" + text[1] + "|"
                        + text[0].substring(1) + "|"
                        + geo.getBreakpoints().get(0).getName() + "|";
            }

        }
        vgf += "\n";
        return vgf;
    }

    private static String getDeSigmet(DrawableElement de) {
        String vgf = "!\n";

        int subType = 0;
        int sol = 0;
        String[] sa = {};
        if (((Sigmet) de).getType() != null)
            sa = ((Sigmet) de).getType().split(":::");

        if (sa[0].equalsIgnoreCase("Area")) {
            subType = 0;
        } else if (sa[0].equalsIgnoreCase("Line")) {
            subType = 1;
            if (sa.length > 1 && sa[1].equalsIgnoreCase("ESOL"))
                sol = 0;
            else if (sa.length > 1 && sa[1].equalsIgnoreCase("NOF"))
                sol = 1;
            else if (sa.length > 1 && sa[1].equalsIgnoreCase("SOF"))
                sol = 2;
            else if (sa.length > 1 && sa[1].equalsIgnoreCase("EOF"))
                sol = 3;
            else if (sa.length > 1 && sa[1].equalsIgnoreCase("WOF"))
                sol = 4;
        } else if (sa[0].equalsIgnoreCase("Isolated")) {
            subType = 2;
        }

        String mwo = ""; // length of the string 7, counted from tag file.
        String id = ""; // length of the string 11
        String phenom = ""; // length of the string 31
        String phenom2 = ""; // length of the string 31
        String phname = "-";
        String dir = ""; // length of the string 3
        String trend = ""; // length of the string 7
        String remark = ""; // length of the string 49

        mwo = ((Sigmet) de).getEditableAttrArea() == null ? "" : ((Sigmet) de)
                .getEditableAttrArea();
        for (int i = mwo.length(); i < 7; i++)
            mwo += " ";
        id = ((Sigmet) de).getEditableAttrId() == null ? "" : ((Sigmet) de)
                .getEditableAttrId();
        for (int i = id.length(); i < 11; i++)
            id += " ";

        if (de.getPgenType().equalsIgnoreCase("INTL_SIGMET")) {
            phenom = ((Sigmet) de).getEditableAttrPhenom() == null ? " "
                    : ((Sigmet) de).getEditableAttrPhenom();
            for (int i = phenom.length(); i < 31; i++)
                phenom += " ";
            phenom2 = ((Sigmet) de).getEditableAttrPhenom2() == null ? " "
                    : ((Sigmet) de).getEditableAttrPhenom2();
            for (int i = phenom2.length(); i < 31; i++)
                phenom2 += " ";
            phname = ((Sigmet) de).getEditableAttrArea() == null ? ""
                    : ((Sigmet) de).getEditableAttrArea();
            if (phname.equalsIgnoreCase("-"))
                phname += ((Sigmet) de).getEditableAttrArea();
            dir = ((Sigmet) de).getEditableAttrPhenomDirection() == null ? ""
                    : ((Sigmet) de).getEditableAttrPhenomDirection();
            for (int i = dir.length(); i < 3; i++)
                dir += " ";
            trend = ((Sigmet) de).getEditableAttrTrend() == null ? ""
                    : ((Sigmet) de).getEditableAttrTrend();
            for (int i = trend.length(); i < 7; i++)
                trend += " ";
            remark = ((Sigmet) de).getEditableAttrRemarks() == null ? ""
                    : ((Sigmet) de).getEditableAttrRemarks();
            for (int i = remark.length(); i < 49; i++)
                remark += " ";
        }

        if (de.getPgenType().equalsIgnoreCase("INTL_SIGMET")) {
            vgf += VgfTags.vg_type
                    + XmlUtil.getType(de.getPgenCategory(), de.getPgenType())
                    + VgfTags.vg_class
                    + "11"
                    + VgfTags.delete
                    + "0"
                    + VgfTags.filled
                    + (((Sigmet) de).isFilled() == true ? 1 : 0)
                    + VgfTags.closed
                    + (((Sigmet) de).isClosedLine() == true ? 1 : 0)
                    + VgfTags.smooth
                    + "0"
                    + VgfTags.version
                    + "1"
                    + VgfTags.grptyp
                    + "0"
                    + VgfTags.grpnum
                    + "0"
                    + VgfTags.maj_col
                    + XmlUtil.getColorTag(XmlUtil.getColorMaj(de))
                    + VgfTags.min_col
                    + XmlUtil.getColorTag(XmlUtil.getColorMin(de))
                    + VgfTags.recsz
                    + "0"
                    + VgfTags.range_min_lat
                    + XmlUtil.getSortedLat(de)[0]
                    + VgfTags.range_min_lon
                    + XmlUtil.getSortedLon(de)[0]
                    + VgfTags.range_max_lat
                    + XmlUtil.getSortedLat(de)[XmlUtil.getSortedLat(de).length - 1]
                    + VgfTags.range_max_lon
                    + XmlUtil.getSortedLon(de)[XmlUtil.getSortedLon(de).length - 1]
                    + VgfTags.subtype
                    + subType
                    + VgfTags.npts
                    + ((Sigmet) de).getPoints().size()
                    + VgfTags.lintyp
                    + XmlUtil.getSPLineType(de.getPgenType())
                    + VgfTags.linwid
                    + (int) de.getLineWidth()
                    + VgfTags.sol
                    + sol
                    + VgfTags.area
                    + mwo
                    + VgfTags.fir
                    + "" // ((Sigmet)de).getEditableAttrFir()
                    + VgfTags.status
                    + (((Sigmet) de).getEditableAttrStatus() == null ? 0
                            : ((Sigmet) de).getEditableAttrStatus())
                    + VgfTags.distance
                    + ((Sigmet) de).getWidth()
                    + VgfTags.msgid
                    + id
                    + VgfTags.seqnum
                    + ((Sigmet) de).getEditableAttrSeqNum()
                    + VgfTags.stime
                    + (((Sigmet) de).getEditableAttrStartTime() == null ? ""
                            : ((Sigmet) de).getEditableAttrStartTime())
                    + VgfTags.etime
                    + (((Sigmet) de).getEditableAttrEndTime() == null ? ""
                            : ((Sigmet) de).getEditableAttrEndTime())
                    + VgfTags.remarks
                    + remark
                    + VgfTags.sonic
                    + "-9999"
                    + VgfTags.phenom
                    + phenom
                    + VgfTags.phenom2
                    + phenom2
                    + VgfTags.phennam
                    + "-" // ((Sigmet)de).getEditableAttrPhenomName()
                    + VgfTags.phenlat
                    + (((Sigmet) de).getEditableAttrPhenomLat() == null ? ""
                            : ((Sigmet) de).getEditableAttrPhenomLat())
                    + VgfTags.phenlon
                    + (((Sigmet) de).getEditableAttrPhenomLon() == null ? ""
                            : ((Sigmet) de).getEditableAttrPhenomLon())
                    + VgfTags.pres
                    + (((Sigmet) de).getEditableAttrPhenomPressure() == null ? "-9999"
                            : ((Sigmet) de).getEditableAttrPhenomPressure())
                    + VgfTags.maxwind
                    + (((Sigmet) de).getEditableAttrPhenomMaxWind() == null ? "-9999"
                            : ((Sigmet) de).getEditableAttrPhenomMaxWind())
                    + VgfTags.freetext
                    + (((Sigmet) de).getEditableAttrFreeText() == null ? ""
                            : ((Sigmet) de).getEditableAttrFreeText())
                    + VgfTags.trend
                    + trend
                    + VgfTags.move
                    + (((Sigmet) de).getEditableAttrMovement() == null ? ""
                            : ((Sigmet) de).getEditableAttrMovement())
                    + VgfTags.obsfcst
                    + "-9999"
                    + VgfTags.obstime
                    + ""
                    + VgfTags.fl
                    + "-9999"
                    + VgfTags.spd
                    + (((Sigmet) de).getEditableAttrPhenomSpeed() == null ? 0
                            : ((Sigmet) de).getEditableAttrPhenomSpeed())
                    + VgfTags.dir
                    + dir
                    + VgfTags.tops
                    + (((Sigmet) de).getEditableAttrLevel() == null ? ""
                            : ((Sigmet) de).getEditableAttrLevel())
                    + "|"
                    + (((Sigmet) de).getEditableAttrLevelInfo1() == null ? ""
                            : ((Sigmet) de).getEditableAttrLevelInfo1())
                    + "|"
                    + (((Sigmet) de).getEditableAttrLevelText1() == null ? " "
                            : ((Sigmet) de).getEditableAttrLevelText1())
                    + "|"
                    + (((Sigmet) de).getEditableAttrLevelInfo2() == null ? ""
                            : ((Sigmet) de).getEditableAttrLevelInfo2())
                    + "|"
                    + (((Sigmet) de).getEditableAttrLevelText2() == null ? " "
                            : ((Sigmet) de).getEditableAttrLevelText2()) + "|"
                    + VgfTags.fcstr + "" + VgfTags.latlon
                    + XmlUtil.getPoints(de) + "\n";
        }

        else {
            
            String seqNum = ((Sigmet) de).getEditableAttrSeqNum();
            if (seqNum == null || seqNum.trim().startsWith("-")) {
                seqNum = "0";
            }

            String status = ((Sigmet) de).getEditableAttrStatus();
            if (status == null || status.trim().length() == 0) {
                status = "0";
            }
            
            
            
            
            
            vgf += VgfTags.vg_type
                    + XmlUtil.getType(de.getPgenCategory(), de.getPgenType())
                    + VgfTags.vg_class
                    + "11"
                    + VgfTags.delete
                    + "0"
                    + VgfTags.filled
                    + (((Sigmet) de).isFilled() == true ? 1 : 0)
                    + VgfTags.closed
                    + (((Sigmet) de).isClosedLine() == true ? 1 : 0)
                    + VgfTags.smooth
                    + "0"
                    + VgfTags.version
                    + "1"
                    + VgfTags.grptyp
                    + "0"
                    + VgfTags.grpnum
                    + "0"
                    + VgfTags.maj_col
                    + XmlUtil.getColorTag(XmlUtil.getColorMaj(de))
                    + VgfTags.min_col
                    + XmlUtil.getColorTag(XmlUtil.getColorMin(de))
                    + VgfTags.recsz
                    + "0"
                    + VgfTags.range_min_lat
                    + XmlUtil.getSortedLat(de)[0]
                    + VgfTags.range_min_lon
                    + XmlUtil.getSortedLon(de)[0]
                    + VgfTags.range_max_lat
                    + XmlUtil.getSortedLat(de)[XmlUtil.getSortedLat(de).length - 1]
                    + VgfTags.range_max_lon
                    + XmlUtil.getSortedLon(de)[XmlUtil.getSortedLon(de).length - 1]
                    + VgfTags.subtype + subType + VgfTags.npts
                    + ((Sigmet) de).getPoints().size()
                    + VgfTags.lintyp
                    + XmlUtil.getSPLineType(de.getPgenType()) + VgfTags.linwid                    
                    + (int) de.getLineWidth()
                    + VgfTags.sol + sol
                    + VgfTags.area + ((mwo.trim().length() > 0) ? mwo : "KMKC")
                    + VgfTags.fir + "" + VgfTags.status + status
                    + VgfTags.distance + ((Sigmet) de).getWidth()
                    + VgfTags.msgid + ((id.trim().length() > 0) ? id : "East")
                    + VgfTags.seqnum + seqNum + VgfTags.stime + ""
                    + VgfTags.etime + "" + VgfTags.remarks + "" + VgfTags.sonic
                    + "-9999" + VgfTags.phenom + "" + VgfTags.phenom2 + "" 
                    + VgfTags.phennam + "-" + VgfTags.phenlat + "" 
                    + VgfTags.phenlon + "" + VgfTags.pres + "-9999"                    
                    + VgfTags.maxwind + "-9999" + VgfTags.freetext + "" 
                    + VgfTags.trend + "-none" + VgfTags.move + "MVG" 
                    + VgfTags.obsfcst + "-9999" + VgfTags.obstime + "" 
                    + VgfTags.fl + "-9999" + VgfTags.spd + "5" + VgfTags.dir
                    + "" + VgfTags.tops + "-none-|TO| |-none-| |" 
                    + VgfTags.fcstr + "" + VgfTags.latlon 
                    + XmlUtil.getPoints(de) + "\n";
        }

        return vgf;
    }

    private static String getDeTrack(DrawableElement de) {
        String vgf = "!\n";
        String colMaj = "";
        String colMin = "";

        // colMaj and conMin are from getInitialColor and getExtrapColor()
        String tem = "";
        if (((Track) de).getInitialColor() != null) {
            // java.awt.Color[r=0,g=255,b=255]
            tem = ((Track) de).getInitialColor().toString();
        }
        colMaj = tem.substring(tem.indexOf("[") + 1, tem.indexOf("]"));
        // java.awt.Color[r=0,g=255,b=255]
        tem = (((Track) de).getExtrapColor()).toString();
        colMin = tem.substring(tem.indexOf("[") + 1, tem.indexOf("]"));

        // the rest is for times displayed on the track nodes
        String times = "";
        // initial points
        for (int b = 0; b < ((Track) de).getInitialPoints().length - 2; b++)
            times += ",";

        Date date = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd/HHmm");
        if (((Track) de).getFirstTimeCalendar() != null) {
            date = ((Track) de).getFirstTimeCalendar().getTime();
            times += sdf.format(date) + ",";
        }

        if (((Track) de).getSecondTimeCalendar() != null) {
            date = ((Track) de).getSecondTimeCalendar().getTime();
            times += sdf.format(date) + ",";
        }

        // extra points
        int day = Integer.parseInt((new SimpleDateFormat("yyMMdd"))
                .format(date));
        int hour = ((Track) de).getSecondTimeCalendar().get(
                Calendar.HOUR_OF_DAY);
        int min = ((Track) de).getSecondTimeCalendar().get(Calendar.MINUTE);

        String h, m, d;
        int incr = XmlUtil
                .getIntervalTime(((Track) de).getIntervalTimeString());

        /*
         * initial Extra point Extra point time is init end point time, plus
         * incr%30, round to next 1/4 hr.
         */
        min = min + incr % 30;
        if (min >= 60) {
            min = min - 60;
            hour = hour + 1;
            if (hour > 23) {
                hour = hour - 24;
                day = day + 1;
            }
        }

        // round to next 1/4 hr
        if (min <= 15) {
            min = 15;
        } else if (min > 15 && min <= 30) {
            min = 30;
        } else if (min > 30 && min <= 45) {
            min = 45;
        } else if (min > 45 && min < 60) {
            min = 00;
            hour = hour + 1;
            if (hour > 23) {
                hour = hour - 24;
                day = day + 1;
            }
        }

        // rest extra points
        int showSize = ((Track) de).getPoints().size()
                - ((Track) de).getInitialPoints().length;
        String[] display = new String[showSize];

        for (int c = 0; c < showSize; c++) {
            // get display
            h = Integer.toString(hour);
            if (h.length() == 1)
                h = "0" + h;
            m = Integer.toString(min);
            if (m.length() == 1)
                m = "0" + m;
            d = Integer.toString(day);
            if (d.length() == 5)
                d = "0" + d;

            display[c] = d + "/" + h + m;
            times += display[c] + ","; // init first, end, extended points

            hour = hour + (min + incr) / 60;
            if (hour > 23) {
                hour = hour - 24;
                day = day + 1;
            }
            min = (min + incr) % 60;
        }

        times = times.substring(0, times.length() - 1); // remove last ","

        String displayOption = "";
        String option = "";
        if (((Track) de).getExtraPointTimeDisplayOption() != null)
            displayOption = ((Track) de).getExtraPointTimeDisplayOption()
                    .toString();

        if (displayOption.equalsIgnoreCase("SKIP_FACTOR"))
            option = ((Track) de).getSkipFactorTextString();
        else if (displayOption.equalsIgnoreCase("SHOW_FIRST_LAST"))
            option = "-1";
        else if (displayOption.equalsIgnoreCase("ON_ONE_HOUR"))
            option = "-2";
        else if (displayOption.equalsIgnoreCase("ON_HALF_HOUR"))
            option = "-3";

        String fontStyle = "";
        // in Cave it is null
        if (((Track) de).getFontStyle() != null) {
            fontStyle = ((Track) de).getFontStyle().toString();
        }

        vgf += VgfTags.vg_type
                + XmlUtil.getType(de.getPgenCategory(), de.getPgenType())
                + VgfTags.vg_class
                + "10"
                + VgfTags.delete
                + "0"
                + VgfTags.filled
                + "0"
                + VgfTags.closed
                + (((Track) de).isClosedLine() == true ? 1 : 0)
                + VgfTags.smooth
                + ((Track) de).getSmoothFactor()
                + VgfTags.version
                + "1"
                + VgfTags.grptyp
                + "0"
                + VgfTags.grpnum
                + "0"
                + VgfTags.maj_col
                + XmlUtil.getColorTag(colMaj) // diff from other color
                + VgfTags.min_col + XmlUtil.getColorTag(colMin) + VgfTags.recsz
                + "0" + VgfTags.range_min_lat + XmlUtil.getSortedLat(de)[0]
                + VgfTags.range_min_lon + XmlUtil.getSortedLon(de)[0]
                + VgfTags.range_max_lat
                + XmlUtil.getSortedLat(de)[XmlUtil.getSortedLat(de).length - 1]
                + VgfTags.range_max_lon
                + XmlUtil.getSortedLon(de)[XmlUtil.getSortedLon(de).length - 1]
                + VgfTags.subtype + "0" + VgfTags.npts + de.getPoints().size()
                + VgfTags.nipts + ((Track) de).getInitialPoints().length
                + VgfTags.ltype1 + "1" + VgfTags.ltype2 + "2" + VgfTags.mtype1
                + "20" + VgfTags.mtype2 + "20" + VgfTags.width
                + (int) de.getLineWidth() + VgfTags.speed
                + ((Track) de).getSpeed() + VgfTags.dir
                + ((Track) de).getDirectionForExtraPoints() + VgfTags.incr
                + incr + VgfTags.skip + option + VgfTags.itxfn
                + XmlUtil.getFontStyle(((Track) de).getFontName(), fontStyle)
                + VgfTags.ithw + "2" + VgfTags.sztext
                + XmlUtil.getSztext(Float.toString(((Track) de).getFontSize()))
                + VgfTags.times + times + VgfTags.latlon
                + XmlUtil.getPoints(de) + "\n";
        return vgf;
    }

    private static String getDeArc(DrawableElement de) {
        String vgf = "!\n";

        vgf += VgfTags.vg_type
                + XmlUtil.getType(de.getPgenCategory(), de.getPgenType())
                + VgfTags.vg_class + "12" + VgfTags.delete + "0"
                + VgfTags.filled + (((Arc) de).isFilled() == true ? 1 : 0)
                + VgfTags.closed + (((Arc) de).isClosedLine() == true ? 1 : 0)
                + VgfTags.smooth + "0" + VgfTags.version + "0" + VgfTags.grptyp
                + "0" + VgfTags.grpnum + "0" + VgfTags.maj_col
                + XmlUtil.getColorTag(XmlUtil.getColorMaj(de))
                + VgfTags.min_col
                + XmlUtil.getColorTag(XmlUtil.getColorMin(de)) + VgfTags.recsz
                + "0" + VgfTags.range_min_lat + XmlUtil.getSortedLat(de)[0]
                + VgfTags.range_min_lon + XmlUtil.getSortedLon(de)[0]
                + VgfTags.range_max_lat
                + XmlUtil.getSortedLat(de)[XmlUtil.getSortedLat(de).length - 1]
                + VgfTags.range_max_lon
                + XmlUtil.getSortedLon(de)[XmlUtil.getSortedLon(de).length - 1]
                + VgfTags.numpts + de.getPoints().size() + VgfTags.lintyp + "1"
                + VgfTags.lthw + "0" + VgfTags.width + (int) de.getLineWidth()
                + VgfTags.lwhw + "0" + VgfTags.latlon + XmlUtil.getPoints(de)
                + "\n";
        return vgf;
    }

    private static String getDeText(DrawableElement de) {
        String vgf = "!\n";
        String avnText = "";
        String symPattern = "";

        double rotn = 0;
        if (((Text) de).getRotationRelativity().toString()
                .equalsIgnoreCase("NORTH_RELATIVE"))
            rotn = ((Text) de).getRotation() + 1000;
        else
            rotn = ((Text) de).getRotation();

        String textString = "";
        if (de.getPgenType().equalsIgnoreCase("AVIATION_TEXT")) {
            if (((AvnText) de).getBottomValue().equalsIgnoreCase("XXX"))
                textString += ((AvnText) de).getTopValue();
            else
                textString += ((AvnText) de).getTopValue() + "/"
                        + ((AvnText) de).getBottomValue();

            if (((AvnText) de).getAvnTextType() == null)
                avnText = "0";
            else
                avnText = ((AvnText) de).getAvnTextType().toString();

            if (((AvnText) de).getSymbolPatternName() == null)
                symPattern = "0";
            else
                symPattern = ((AvnText) de).getSymbolPatternName();

        } else if (de.getPgenType().equalsIgnoreCase("MID_LEVEL_CLOUD")) {

            textString += XmlUtil.getMidLevSplit(
                    ((MidCloudText) de).getCloudTypes(), "\\|", ";")
                    + "|"
                    + XmlUtil.getMidLevSplit(
                            ((MidCloudText) de).getCloudAmounts(), "\\|", ";")
                    + "|"
                    + XmlUtil.getTurbSymType(((MidCloudText) de)
                            .getIcingPattern())
                    + "|"
                    + ((MidCloudText) de).getIcingLevels()
                    + "|"
                    + XmlUtil.getTurbSymType(((MidCloudText) de)
                            .getTurbulencePattern())
                    + "|"
                    + ((MidCloudText) de).getTurbulenceLevels()
                    + "|"
                    + XmlUtil.getMidLevSplit(
                            ((MidCloudText) de).getTstormTypes(), "\\|", ";")
                    + "|" + ((MidCloudText) de).getTstormLevels(); // + "|" ;
                                                                   // //is not
                                                                   // endding
                                                                   // with |

        } else if (de.getPgenType().equalsIgnoreCase("General Text")) {
            String[] text = ((Text) de).getString();
            if (text != null && text.length != 0) {
                for (int o = 0; o < text.length; o++) {
                    // if text o=="", "\n"+"$$"
                    textString += ((Text) de).getString()[o] + "$$";
                }
                // no end $$
                textString = textString.substring(0, textString.length() - 2);

            }
        }

        vgf += VgfTags.vg_type
                + XmlUtil.getType(de.getPgenCategory(), de.getPgenType())
                + VgfTags.vg_class
                + "5"
                + VgfTags.delete
                + "0"
                + VgfTags.filled
                + "0"
                + VgfTags.closed
                + "0"
                + VgfTags.smooth
                + "0"
                + VgfTags.version
                + "0"
                + VgfTags.grptyp
                + "0"
                + VgfTags.grpnum
                + "0"
                + VgfTags.maj_col
                + XmlUtil.getColorTag(XmlUtil.getColorMaj(de))
                + VgfTags.min_col
                + XmlUtil.getColorTag(XmlUtil.getColorMin(de))
                + VgfTags.recsz
                + "0"
                + VgfTags.range_min_lat
                + XmlUtil.getSortedLat(de)[0]
                + VgfTags.range_min_lon
                + XmlUtil.getSortedLon(de)[0]
                + VgfTags.range_max_lat
                + XmlUtil.getSortedLat(de)[XmlUtil.getSortedLat(de).length - 1]
                + VgfTags.range_max_lon
                + XmlUtil.getSortedLon(de)[XmlUtil.getSortedLon(de).length - 1]
                + VgfTags.rotn
                + rotn
                + VgfTags.sztext
                + XmlUtil.getSztext(Float.toString(((Text) de).getFontSize()))
                + VgfTags.sptxtyp
                + XmlUtil.getSPTextType(de.getPgenType(), avnText, ((Text) de)
                        .maskText(), ((Text) de).getDisplayType().toString())
                + VgfTags.turbsym
                + XmlUtil.getTurbSymType(symPattern)
                + VgfTags.itxfn
                + XmlUtil.getFontStyle(((Text) de).getFontName(), ((Text) de)
                        .getStyle().toString()) + VgfTags.ithw 
                + ((Text) de).getIthw() + VgfTags.iwidth
                + ((Text) de).getIwidth() + VgfTags.txtcol
                + XmlUtil.getColorTag(XmlUtil.getColorMaj(de)) + VgfTags.lincol
                + XmlUtil.getColorTag(XmlUtil.getColorMaj(de)) + VgfTags.filcol
                + XmlUtil.getColorTag(XmlUtil.getColorMaj(de)) + VgfTags.ialign
                + XmlUtil.getIalign(((Text) de).getJustification().toString())
                + VgfTags.lat + XmlUtil.getPointLats(de) + VgfTags.lon
                + XmlUtil.getPointLons(de) + VgfTags.offset_x
                + ((Text) de).getXOffset() + VgfTags.offset_y
                + ((Text) de).getYOffset() + VgfTags.text + textString + "\n";
        return vgf;
    }

    private static String getDeVector(DrawableElement de) {
        String vgf = "!\n";

        vgf += VgfTags.vg_type
                + XmlUtil.getType(de.getPgenCategory(), de.getPgenType())
                + VgfTags.vg_class
                + "6"
                + VgfTags.delete
                + "0"
                + VgfTags.filled
                + "0"
                + VgfTags.closed
                + "0"
                + VgfTags.smooth
                + "0"
                + VgfTags.version
                + "0"
                + VgfTags.grptyp
                + "0"
                + VgfTags.grpnum
                + "0"
                + VgfTags.maj_col
                + XmlUtil.getColorTag(XmlUtil.getColorMaj(de))
                + VgfTags.min_col
                + XmlUtil.getColorTag(XmlUtil.getColorMin(de))
                + VgfTags.recsz
                + (XmlUtil.getRecSize(de.getPgenCategory(), de.getPgenType()) + 8 * de
                        .getPoints().size())
                + VgfTags.range_min_lat
                + XmlUtil.getSortedLat(de)[0]
                + VgfTags.range_min_lon
                + XmlUtil.getSortedLon(de)[0]
                + VgfTags.range_max_lat
                + XmlUtil.getSortedLat(de)[XmlUtil.getSortedLat(de).length - 1]
                + VgfTags.range_max_lon
                + XmlUtil.getSortedLon(de)[XmlUtil.getSortedLon(de).length - 1]
                + VgfTags.numwnd
                + de.getPoints().size()
                + VgfTags.width
                + (int) de.getLineWidth()
                + VgfTags.size
                + de.getSizeScale()
                + VgfTags.wndtyp
                + XmlUtil.getWndType(de.getPgenType(),
                        ((SinglePointElement) de).isClear()) + VgfTags.hdsiz
                + ((Vector) de).getArrowHeadSize() + VgfTags.spddir
                + ((Vector) de).getSpeed() + "," + ((Vector) de).getDirection()
                + VgfTags.latlon + XmlUtil.getPoints(de) + "\n";
        return vgf;
    }

    private static String getDeSymbol(DrawableElement de) {
        String vgf = "!\n";

        vgf += VgfTags.vg_type
                + XmlUtil.getType(de.getPgenCategory(), de.getPgenType())
                + VgfTags.vg_class
                + "4"
                + VgfTags.delete
                + "0"
                + VgfTags.filled
                + "0"
                + VgfTags.closed
                + "0"
                + VgfTags.smooth
                + "0"
                + VgfTags.version
                + "0"
                + VgfTags.grptyp
                + "0"
                + VgfTags.grpnum
                + "0"
                + VgfTags.maj_col
                + XmlUtil.getColorTag(XmlUtil.getColorMaj(de))
                + VgfTags.min_col
                + XmlUtil.getColorTag(XmlUtil.getColorMin(de))
                + VgfTags.recsz
                + (XmlUtil.getRecSize(de.getPgenCategory(), de.getPgenType()) + 8 * de
                        .getPoints().size())
                + VgfTags.range_min_lat
                + XmlUtil.getSortedLat(de)[0]
                + VgfTags.range_min_lon
                + XmlUtil.getSortedLon(de)[0]
                + VgfTags.range_max_lat
                + XmlUtil.getSortedLat(de)[XmlUtil.getSortedLat(de).length - 1]
                + VgfTags.range_max_lon
                + XmlUtil.getSortedLon(de)[XmlUtil.getSortedLon(de).length - 1]
                + VgfTags.numsym
                + de.getPoints().size()
                + VgfTags.width
                + (((SinglePointElement) de).isClear() ? (800 + (int) de
                        .getLineWidth()) : (int) de.getLineWidth())
                + VgfTags.size + de.getSizeScale() + VgfTags.ityp + "0"
                + VgfTags.code + XmlUtil.getSymType(de.getPgenType())
                + VgfTags.latlon + XmlUtil.getPoints(de) + VgfTags.offset_xy
                + "0, 0" + "\n"; // + ((Symbol)de).getXOffset() +"," +
                                 // ((Symbol)de).getYOffset() +"\n";
        return vgf;
    }

    private static String getDeFront(DrawableElement de) {
        String vgf = "!\n";
        String flipSide = "0";

        /*-
         * NMAP retrieves a value for PGen flipping from *.vgf files it reads.
         * It makes a decision to flip/not flip pips on the line 
         * based on those retrieved values in
         * nmap_pgwxd.c  about line 231
         * If the value is "1" NMAP will NOT flip the pip direction.
         * If the value is "-1" it WILL flip the pip direction.  These values
         * have no other meaning in NMAP ( ie, not "up/down" ).
         */

        if (((Line) de).isFlipSide()) {
            // Flip the direction of the PGen pips
            flipSide = "-1";
        } else {
            // Do NOT flip the direction of the PGen pips
            flipSide = "1";
        }

        vgf += VgfTags.vg_type
                + XmlUtil.getType(de.getPgenCategory(), de.getPgenType())
                + VgfTags.vg_class
                + "1"
                + VgfTags.delete
                + "0"
                + VgfTags.filled
                + "0"
                + VgfTags.closed
                + (((Line) de).isClosedLine() == true ? 1 : 0)
                + VgfTags.smooth
                + ((Line) de).getSmoothFactor()
                + VgfTags.version
                + "0"
                + VgfTags.grptyp
                + "0"
                + VgfTags.grpnum
                + "0"
                + VgfTags.maj_col
                + XmlUtil.getColorTag(XmlUtil.getColorMaj(de))
                + VgfTags.min_col
                + XmlUtil.getColorTag(XmlUtil.getColorMin(de))
                + VgfTags.recsz
                + "0"
                + VgfTags.range_min_lat
                + XmlUtil.getSortedLat(de)[0]
                + VgfTags.range_min_lon
                + XmlUtil.getSortedLon(de)[0]
                + VgfTags.range_max_lat
                + XmlUtil.getSortedLat(de)[XmlUtil.getSortedLat(de).length - 1]
                + VgfTags.range_max_lon
                + XmlUtil.getSortedLon(de)[XmlUtil.getSortedLon(de).length - 1]
                + VgfTags.numpts
                + de.getPoints().size()
                + VgfTags.fcode
                + XmlUtil.getFrontType(de.getPgenType(),
                        (int) de.getLineWidth()) + VgfTags.fpipsz
                + Math.round(de.getSizeScale() * 100) + VgfTags.fpipst + "1"
                + VgfTags.fpipdr + flipSide + VgfTags.fwidth
                + (int) de.getLineWidth() + VgfTags.frtlbl + "STJ"
                + VgfTags.latlon + XmlUtil.getPoints(de) + "\n";
        return vgf;
    }

    private static String getDeSpLine(DrawableElement de) {
        String vgf = "!\n";
        int limit = 500; // points limited in one line
        int pointNum = 0;
        String[] pointsX = {};
        String[] pointsY = {};

        if (XmlUtil.getPointLats(de) != null)
            pointsX = XmlUtil.getPointLats(de).split(", ");
        if (XmlUtil.getPointLons(de) != null)
            pointsY = XmlUtil.getPointLons(de).split(", ");

        if (pointsX.length != 0) {
            pointNum = pointsX.length % limit;
            String points500X = "";
            String points500Y = "";
            for (int i = 0; i < pointNum; i++) {
                points500X = points500X + pointsX[i] + ", ";
                points500Y = points500Y + pointsY[i] + ", ";
            }
            if (points500Y.length() > 2)
                points500Y = points500Y.substring(0, (points500Y.length() - 2));

            vgf += VgfTags.vg_type
                    + XmlUtil.getType(de.getPgenCategory(), de.getPgenType())
                    + VgfTags.vg_class
                    + "3"
                    + VgfTags.delete
                    + "0"
                    + VgfTags.filled
                    + XmlUtil.fill_pattern(((Line) de).getFillPattern()
                            .toString(), ((Line) de).isFilled())
                    + VgfTags.closed
                    + (((Line) de).isClosedLine() == true ? 1 : 0)
                    + VgfTags.smooth
                    + ((Line) de).getSmoothFactor()
                    + VgfTags.version
                    + "0"
                    + VgfTags.grptyp
                    + "0"
                    + VgfTags.grpnum
                    + "0"
                    + VgfTags.maj_col
                    + XmlUtil.getColorTag(XmlUtil.getColorMaj(de))
                    + VgfTags.min_col
                    + XmlUtil.getColorTag(XmlUtil.getColorMin(de))
                    + VgfTags.recsz
                    + (XmlUtil.getRecSize(de.getPgenCategory(),
                            de.getPgenType()) + 8 * de.getPoints().size())
                    + VgfTags.range_min_lat
                    + XmlUtil.getSortedLat(de)[0]
                    + VgfTags.range_min_lon
                    + XmlUtil.getSortedLon(de)[0]
                    + VgfTags.range_max_lat
                    + XmlUtil.getSortedLat(de)[XmlUtil.getSortedLat(de).length - 1]
                    + VgfTags.range_max_lon
                    + XmlUtil.getSortedLon(de)[XmlUtil.getSortedLon(de).length - 1]
                    + VgfTags.numpts + pointsX.length % limit + VgfTags.spltyp
                    + XmlUtil.getSPLineType(de.getPgenType()) + VgfTags.splstr
                    + getSplnMultiplier(de) + VgfTags.spldir + "1"
                    + VgfTags.splsiz + de.getSizeScale() + VgfTags.splwid
                    + (int) de.getLineWidth() + VgfTags.latlon + points500X
                    + points500Y + "\n";
        }

        if (pointsX.length != 0 && pointsX.length / limit > 0) {
            int n = pointsX.length / limit;
            pointNum = pointsX.length % limit;
            for (int j = 0; j < n; j++) {
                String points500X = "";
                String points500Y = "";
                for (int i = (pointNum + j * limit); i < (pointNum + (j + 1)
                        * limit); i++) {
                    points500X = points500X + pointsX[i] + ", ";
                    points500Y = points500Y + pointsY[i] + ", ";
                }
                if (points500Y.length() > 2)
                    points500Y = points500Y.substring(0,
                            (points500Y.length() - 2));

                vgf += VgfTags.vg_type
                        + XmlUtil.getType(de.getPgenCategory(),
                                de.getPgenType())
                        + VgfTags.vg_class
                        + "3"
                        + VgfTags.delete
                        + "0"
                        + VgfTags.filled
                        + XmlUtil.fill_pattern(((Line) de).getFillPattern()
                                .toString(), ((Line) de).isFilled())
                        + VgfTags.closed
                        + (((Line) de).isClosedLine() == true ? 1 : 0)
                        + VgfTags.smooth
                        + ((Line) de).getSmoothFactor()
                        + VgfTags.version
                        + "0"
                        + VgfTags.grptyp
                        + "0"
                        + VgfTags.grpnum
                        + "0"
                        + VgfTags.maj_col
                        + XmlUtil.getColorTag(XmlUtil.getColorMaj(de))
                        + VgfTags.min_col
                        + XmlUtil.getColorTag(XmlUtil.getColorMin(de))
                        + VgfTags.recsz
                        + 0
                        + VgfTags.range_min_lat
                        + XmlUtil.getSortedLat(de)[0]
                        + VgfTags.range_min_lon
                        + XmlUtil.getSortedLon(de)[0]
                        + VgfTags.range_max_lat
                        + XmlUtil.getSortedLat(de)[XmlUtil.getSortedLat(de).length - 1]
                        + VgfTags.range_max_lon
                        + XmlUtil.getSortedLon(de)[XmlUtil.getSortedLon(de).length - 1]
                        + VgfTags.numpts + limit + VgfTags.spltyp
                        + XmlUtil.getSPLineType(de.getPgenType())
                        + VgfTags.splstr + getSplnMultiplier(de)
                        + VgfTags.spldir + "1" + VgfTags.splsiz
                        + de.getSizeScale() + VgfTags.splwid
                        + (int) de.getLineWidth() + VgfTags.latlon + points500X
                        + points500Y + "\n";
            }
        }

        return vgf;
    }

    private static String getDeLine(DrawableElement de) {
        String vgf = "!\n";
        int limit = 500; // points limited in one line
        int pointNum = 0;
        String[] pointsX = {};
        String[] pointsY = {};

        if (XmlUtil.getPointLats(de) != null)
            pointsX = XmlUtil.getPointLats(de).split(", ");
        if (XmlUtil.getPointLons(de) != null)
            pointsY = XmlUtil.getPointLons(de).split(", ");

        if (pointsX.length != 0) {
            pointNum = pointsX.length % limit;
            String points500X = "";
            String points500Y = "";
            for (int i = 0; i < pointNum; i++) {
                points500X = points500X + pointsX[i] + ", ";
                points500Y = points500Y + pointsY[i] + ", ";
            }

            if (points500Y.length() > 2)
                points500Y = points500Y.substring(0, (points500Y.length() - 2));

            vgf += VgfTags.vg_type
                    + XmlUtil.getType(de.getPgenCategory(), de.getPgenType())
                    + VgfTags.vg_class
                    + "3"
                    + VgfTags.delete
                    + "0"
                    + VgfTags.filled
                    + XmlUtil.fill_pattern(((Line) de).getFillPattern()
                            .toString(), ((Line) de).isFilled())
                    + VgfTags.closed
                    + (((Line) de).isClosedLine() == true ? 1 : 0)
                    + VgfTags.smooth
                    + ((Line) de).getSmoothFactor()
                    + VgfTags.version
                    + "0"
                    + VgfTags.grptyp
                    + "0"
                    + VgfTags.grpnum
                    + "0"
                    + VgfTags.maj_col
                    + XmlUtil.getColorTag(XmlUtil.getColorMaj(de))
                    + VgfTags.min_col
                    + XmlUtil.getColorTag(XmlUtil.getColorMin(de))
                    + VgfTags.recsz
                    + (XmlUtil.getRecSize(de.getPgenCategory(),
                            de.getPgenType()) + 8 * de.getPoints().size())
                    + VgfTags.range_min_lat
                    + XmlUtil.getSortedLat(de)[0]
                    + VgfTags.range_min_lon
                    + XmlUtil.getSortedLon(de)[0]
                    + VgfTags.range_max_lat
                    + XmlUtil.getSortedLat(de)[XmlUtil.getSortedLat(de).length - 1]
                    + VgfTags.range_max_lon
                    + XmlUtil.getSortedLon(de)[XmlUtil.getSortedLon(de).length - 1]
                    + VgfTags.numpts + pointsX.length % limit + VgfTags.lintyp
                    + XmlUtil.getLineType(de.getPgenType()) + VgfTags.lthw
                    + "0" + VgfTags.width + (int) de.getLineWidth()
                    + VgfTags.lwhw + "0" + VgfTags.latlon + points500X
                    + points500Y + "\n";
        }

        if (pointsX.length != 0 && pointsX.length / limit > 0) {
            int n = pointsX.length / limit;
            pointNum = pointsX.length % limit;

            for (int j = 0; j < n; j++) {
                String points500X = "";
                String points500Y = "";
                for (int i = (pointNum + j * limit); i < (pointNum + (j + 1)
                        * limit); i++) {
                    points500X = points500X + pointsX[i] + ", ";
                    points500Y = points500Y + pointsY[i] + ", ";
                }
                if (points500Y.length() > 2)
                    points500Y = points500Y.substring(0,
                            (points500Y.length() - 2));

                vgf += VgfTags.vg_type
                        + XmlUtil.getType(de.getPgenCategory(),
                                de.getPgenType())
                        + VgfTags.vg_class
                        + "3"
                        + VgfTags.delete
                        + "0"
                        + VgfTags.filled
                        + XmlUtil.fill_pattern(((Line) de).getFillPattern()
                                .toString(), ((Line) de).isFilled())
                        + VgfTags.closed
                        + (((Line) de).isClosedLine() == true ? 1 : 0)
                        + VgfTags.smooth
                        + ((Line) de).getSmoothFactor()
                        + VgfTags.version
                        + "0"
                        + VgfTags.grptyp
                        + "0"
                        + VgfTags.grpnum
                        + "0"
                        + VgfTags.maj_col
                        + XmlUtil.getColorTag(XmlUtil.getColorMaj(de))
                        + VgfTags.min_col
                        + XmlUtil.getColorTag(XmlUtil.getColorMin(de))
                        + VgfTags.recsz
                        + 0
                        + VgfTags.range_min_lat
                        + XmlUtil.getSortedLat(de)[0]
                        + VgfTags.range_min_lon
                        + XmlUtil.getSortedLon(de)[0]
                        + VgfTags.range_max_lat
                        + XmlUtil.getSortedLat(de)[XmlUtil.getSortedLat(de).length - 1]
                        + VgfTags.range_max_lon
                        + XmlUtil.getSortedLon(de)[XmlUtil.getSortedLon(de).length - 1]
                        + VgfTags.numpts + limit + VgfTags.lintyp
                        + XmlUtil.getLineType(de.getPgenType()) + VgfTags.lthw
                        + "0" + VgfTags.width + (int) de.getLineWidth()
                        + VgfTags.lwhw + "0" + VgfTags.latlon + points500X
                        + points500Y + "\n";
            }
        }
        return vgf;
    }

    private static String getDeVol(DrawableElement de) {
        String vgf = "!\n";

        String stnVaac = ((Volcano) de).getOrigStnVAAC();
        String stn = "", vaac = "";
        if (stnVaac != null) {
            String[] s = stnVaac.split("/");
            stn = s[0];
            if (s.length > 1)
                vaac = s[1];
        }

        String elev = ((Volcano) de).getElev(); // "10778  ft (3285  m)" ->
                                                // 10777
        if (elev != null && elev.indexOf("f") != -1)
            elev = elev.substring(0, elev.indexOf("f"));

        double code = 0;
        if (((Volcano) de).getProduct().equalsIgnoreCase("NORMAL"))
            code = 201.000000;

        // where is code 201.0? originstn KWBC, vaac WASHINGTON,...
        vgf += VgfTags.vg_type
                + XmlUtil.getType(de.getPgenCategory(), de.getPgenType())
                + VgfTags.vg_class
                + "11"
                + VgfTags.delete
                + "0"
                + VgfTags.filled
                + "0"
                + VgfTags.closed
                + "0"
                + VgfTags.smooth
                + "0"
                + VgfTags.version
                + "0"
                + VgfTags.grptyp
                + "0"
                + VgfTags.grpnum
                + "0"
                + VgfTags.maj_col
                + XmlUtil.getColorTag(XmlUtil.getColorMaj(de))
                + VgfTags.min_col
                + XmlUtil.getColorTag(XmlUtil.getColorMin(de))
                + VgfTags.recsz
                + "0"
                + VgfTags.range_min_lat
                + "" // XmlUtil.getSortedLat(de)[0]
                + VgfTags.range_min_lon
                + "" // XmlUtil.getSortedLon(de)[0]
                + VgfTags.range_max_lat
                + "" // XmlUtil.getSortedLat(de)[XmlUtil.getSortedLat(de).length-1]
                + VgfTags.range_max_lon
                + "" // XmlUtil.getSortedLon(de)[XmlUtil.getSortedLon(de).length-1]

                + VgfTags.name
                + ((Volcano) de).getName()
                + VgfTags.code
                + code
                + VgfTags.size
                + ((Volcano) de).getSizeScale()
                + VgfTags.width
                + (int) de.getLineWidth()
                + VgfTags.number
                + ((Volcano) de).getNumber()
                + VgfTags.location
                + ((Volcano) de).getTxtLoc()
                + VgfTags.area
                + ((Volcano) de).getArea()
                + VgfTags.elev
                + elev
                + VgfTags.origstn
                + stn
                + VgfTags.vaac
                + vaac
                + VgfTags.wmoid
                + (((Volcano) de).getWmoId() == null ? "" : ((Volcano) de)
                        .getWmoId())
                + VgfTags.hdrnum
                + (((Volcano) de).getHdrNum() == null ? "" : ((Volcano) de)
                        .getHdrNum())
                + VgfTags.year
                + (((Volcano) de).getYear() == null ? "" : ((Volcano) de)
                        .getYear())
                + VgfTags.advnum
                + (((Volcano) de).getAdvNum() == null ? "" : ((Volcano) de)
                        .getAdvNum())
                + VgfTags.infosorc
                + (((Volcano) de).getInfoSource() == null ? "" : ((Volcano) de)
                        .getInfoSource())
                + VgfTags.addlsorc
                + (((Volcano) de).getAddInfoSource() == null ? ""
                        : ((Volcano) de).getAddInfoSource())
                + VgfTags.details
                + (((Volcano) de).getErupDetails() == null ? ""
                        : ((Volcano) de).getErupDetails())
                + VgfTags.obsdate
                + (((Volcano) de).getObsAshDate() == null ? "" : ((Volcano) de)
                        .getObsAshDate())
                + VgfTags.obstime
                + (((Volcano) de).getObsAshTime() == null ? "" : ((Volcano) de)
                        .getObsAshTime())
                + VgfTags.obsashcld
                + (((Volcano) de).getObsFcstAshCloudInfo() == null ? ""
                        : ((Volcano) de).getObsFcstAshCloudInfo())
                + VgfTags.fcst_06
                + (((Volcano) de).getObsFcstAshCloudInfo6() == null ? ""
                        : ((Volcano) de).getObsFcstAshCloudInfo6())
                + VgfTags.fcst_12
                + (((Volcano) de).getObsFcstAshCloudInfo12() == null ? ""
                        : ((Volcano) de).getObsFcstAshCloudInfo12())
                + VgfTags.fcst_18
                + (((Volcano) de).getObsFcstAshCloudInfo18() == null ? ""
                        : ((Volcano) de).getObsFcstAshCloudInfo18())
                + VgfTags.remarks
                + (((Volcano) de).getRemarks() == null ? "" : ((Volcano) de)
                        .getRemarks())
                + VgfTags.nextadv
                + (((Volcano) de).getNextAdv() == null ? "" : ((Volcano) de)
                        .getNextAdv())
                + VgfTags.fcstrs
                + (((Volcano) de).getForecasters() == null ? ""
                        : ((Volcano) de).getForecasters())
                + VgfTags.corr
                + (((Volcano) de).getCorr() == null ? "" : ((Volcano) de)
                        .getCorr())
                + VgfTags.avcc
                + (((Volcano) de).getAviColorCode() == null ? ""
                        : ((Volcano) de).getAviColorCode()) + VgfTags.latlon
                + ((Volcano) de).getConverterVolcPoints().get(0).y + ", "
                + ((Volcano) de).getConverterVolcPoints().get(0).x
                + VgfTags.offset_xy + "0, 0" + "\n";
        return vgf;
    }

    private static String getDeVac(DrawableElement de) {
        String vgf = "!\n";

        String[] freeText = {};
        if (((Sigmet) de).getEditableAttrFreeText() != null)
            freeText = ((Sigmet) de).getEditableAttrFreeText().split(":::");
        String fhr = "", flv1 = "", flv2 = "", dir = "", spd = "";

        // "F00:::SFC::: ::: ::: "
        if (freeText != null && freeText.length != 0) {
            fhr = freeText[0];
            flv1 = freeText[1];
            flv2 = freeText[2];
            dir = freeText[3];
            spd = freeText[4];
        }

        if (fhr.equalsIgnoreCase("F00"))
            fhr = "0";
        else if (fhr.equalsIgnoreCase("F06"))
            fhr = "6";
        else
            fhr = fhr.substring(1);

        String type = ((Sigmet) de).getType();
        int subtype = 0;
        String text = "";
        if (type.equalsIgnoreCase("Area"))
            subtype = 0;
        else if (type.startsWith("Line")) // Line:::ESOL
            subtype = 1;
        else if (type.startsWith("Text:::WINDS")
                || type.startsWith("Text:::VA NOT")) // VA NOTIDENTIFIABLE FROM
                                                     // SATELLITE DATA
            subtype = 2;
        else if (type.startsWith("Text:::NOT AV"))
            subtype = 3;
        else if (type.startsWith("Text:::ASH DISSIPATING"))
            subtype = 5;
        else if (type.startsWith("Text:::NO ASH"))
            subtype = 6;
        else
            // if (type.startsWith("Text:::SFC/FL"))
            subtype = 4;

        if (subtype >= 2)
            text = type.substring(7, type.length());

        vgf += VgfTags.vg_type
                + XmlUtil.getType(de.getPgenCategory(), de.getPgenType())
                + VgfTags.vg_class + "11" + VgfTags.delete + "0"
                + VgfTags.filled + "5" + VgfTags.closed
                + (subtype == 0 ? 1 : 0) + VgfTags.smooth + "0"
                + VgfTags.version + "0" + VgfTags.grptyp + "0" + VgfTags.grpnum
                + "0" + VgfTags.maj_col
                + XmlUtil.getColorTag(XmlUtil.getColorMaj(de))
                + VgfTags.min_col
                + XmlUtil.getColorTag(XmlUtil.getColorMin(de)) + VgfTags.recsz
                + 0 + VgfTags.range_min_lat + XmlUtil.getSortedLat(de)[0]
                + VgfTags.range_min_lon + XmlUtil.getSortedLon(de)[0]
                + VgfTags.range_max_lat
                + XmlUtil.getSortedLat(de)[XmlUtil.getSortedLat(de).length - 1]
                + VgfTags.range_max_lon
                + XmlUtil.getSortedLon(de)[XmlUtil.getSortedLon(de).length - 1]

                + VgfTags.subtype + subtype + VgfTags.npts
                + ((Sigmet) de).getPoints().size() + VgfTags.distance
                + (subtype == 1 ? ((Sigmet) de).getWidth() : 0) + VgfTags.fhr
                + fhr + VgfTags.lintyp + "1" + VgfTags.linwid
                + ((Sigmet) de).getLineWidth() + VgfTags.spd + spd.trim()
                + VgfTags.dir + dir.trim() + VgfTags.flvl1 + flv1.trim()
                + VgfTags.flvl2 + flv2.trim() + VgfTags.text + text
                + VgfTags.latlon + XmlUtil.getPoints(de) + "\n";
        return vgf;
    }

    /*
     * Retrieve CIG or VIS value for a GFA IFR (C&V).
     * 
     * @param de Gfa element
     * 
     * @param which CIG or VIS
     * 
     * @return out value of CIG or VIS
     */
    private static String getGfaCigVis(DrawableElement de, String which) {

        String out = "N/A";

        if (((Gfa) de).getGfaHazard().equalsIgnoreCase("IFR")) {
            String type = ((Gfa) de).getGfaType();
            if (type != null && type.contains(which)) {
                String substr = type.substring(type.indexOf(which));
                String[] values = substr.split(" ");
                if (values.length > 2) {
                    out = values[1] + "_" + values[2];
                    if (out.contains("/")) {
                        out = out.substring(0, out.indexOf("/"));
                    }
                }
            }
        }

        return out;
    }

    /*
     * Retrive the stroke multiplier for special lines such as kink lines.
     * 
     * @param de line element
     * 
     * @return out value of line stroke multiplier
     */
    private static String getSplnMultiplier(DrawableElement de) {

        String out = "1";

        if (de.getPgenType().equalsIgnoreCase("KINK_LINE_1")
                || de.getPgenType().equalsIgnoreCase("KINK_LINE_2")) {
            out = "" + (int) (((KinkLine) de).getKinkPosition() * 100);
        }

        return out;
    }

}
