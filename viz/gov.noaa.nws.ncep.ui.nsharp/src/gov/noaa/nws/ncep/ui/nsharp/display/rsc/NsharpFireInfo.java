package gov.noaa.nws.ncep.ui.nsharp.display.rsc;

import java.util.List;

import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibBasics;
import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibSkparams;
import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibSndglib;
import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibThermo;
import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibWinds;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.LParcelValues;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.Parcel;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.WindComponent;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingTools;

/**
 *
 *
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 *
 * All methods developed in this class are based on the algorithm developed in
 * BigSharp native C file, basics.c , by John A. Hart/SPC. All methods name are
 * defined with same name as the C function name defined in native code.
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date         Ticket#     Engineer    Description
 * -------      -------     --------    -----------
 * 07/05/2016   RM#15923    Chin Chen   NSHARP - Native Code replacement
 * May, 5, 2018 49896       mgamazaychikov  Fixed an NPE for pcl, fixed formatting
 *
 * </pre>
 *
 * @author Chin Chen
 * @version 1.0
 *
 */
public class NsharpFireInfo {
    private int sfcRhColor;

    private int pwColor;

    private int blMaxColor;

    private int fosbergColor;

    private String sfcRh;

    private String sfc;

    private String zeroOneKmRh;

    private String zeroOneKmMean;

    private String blMeanRh;

    private String blMean;

    private String pw;

    private String blMax;

    private String fosbergIndex;

    public NsharpFireInfo() {
        super();
    }

    /****************************************************************************
     * computeFireInfo() --------- this function is derived from show_fire() of
     * xwvid3.c of BigNsharp by Rich Thompson SPC OUN. Rewrite code to get all
     * computed parameters/string for CAVE. All original BigNsharp plotting code
     * are removed.
     *************************************************************************/
    public static NsharpFireInfo computeFireInfo(List<NcSoundingLayer> sndLys) {
        NsharpFireInfo fireInfo = new NsharpFireInfo();

        float p1, p2, sfcrh;

        // remove original code "define_parcel(1,0)" here, as it is doing
        // nothing here

        // surface relative humidity
        sfcrh = NsharpLibThermo.relh(sndLys, -1);

        if (sfcrh > 35) {
            fireInfo.setSfcRhColor(8);
        } else if (sfcrh > 30) {
            fireInfo.setSfcRhColor(18);
        } else if (sfcrh > 20) {
            fireInfo.setSfcRhColor(31);
        } else if (sfcrh > 15) {
            fireInfo.setSfcRhColor(19);
        } else if (sfcrh > 10) {
            fireInfo.setSfcRhColor(2);
        } else if (sfcrh >= 0) {
            fireInfo.setSfcRhColor(7);
        } else {
            fireInfo.setSfcRhColor(31);
        }

        String sfcRhStr;
        if (NsharpLibBasics.qc(sfcrh)) {
            sfcRhStr = String.format("SFC RH  = %.0f", sfcrh) + "%";
        } else {
            sfcRhStr = "SFC RH  = M";
        }
        fireInfo.setSfcRh(sfcRhStr);

        // compute surface mean wind using sfc pressure.
        p1 = NsharpLibBasics.sfcPressure(sndLys);
        p2 = p1;
        WindComponent meanWindCompSfc = NsharpLibWinds.mean_wind(sndLys, p1, p2);
        String sfcStr;
        if (NsharpLibBasics.qc(meanWindCompSfc.getWdir())) {
            sfcStr = String.format("SFC = %4.0f/%.0f", meanWindCompSfc.getWdir(), meanWindCompSfc.getWspd());
        } else {
            sfcStr = "M";
        }
        fireInfo.setSfc(sfcStr);

        // 0-1km mean rh
        float zeroOneKmRh = NsharpLibSkparams.mean_relhum(sndLys, p1,
                NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.msl(sndLys, 1000)));
        String zeroOneKmRhStr = String.format("0-1 km RH  = %.0f", zeroOneKmRh) + "%";
        fireInfo.setZeroOneKmRh(zeroOneKmRhStr);

        // 0-1 km mean wind
        WindComponent meanWindComp0To1km = NsharpLibWinds.mean_wind(sndLys, p1,
                NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.msl(sndLys, 1000)));
        String zeroOneKmMeanWindStr;
        if (NsharpLibBasics.qc(meanWindComp0To1km.getWdir())) {
            zeroOneKmMeanWindStr = String.format("0-1 km mean = %4.0f/%.0f", meanWindComp0To1km.getWdir(),
                    meanWindComp0To1km.getWspd());
        } else {
            zeroOneKmMeanWindStr = "0-1 km mean = M";
        }
        fireInfo.setZeroOneKmMean(zeroOneKmMeanWindStr);

        // BL mean RH
        float topP = NsharpLibSkparams.pbl_top(sndLys);
        float blRh = NsharpLibSkparams.mean_relhum(sndLys, p1, topP);
        String blMeanRhStr;
        if (NsharpLibBasics.qc(blRh)) {
            blMeanRhStr = String.format("BL mean RH  = %.0f", blRh) + "%";
        } else {
            blMeanRhStr = "BL mean RH  = M";
        }
        fireInfo.setBlMeanRh(blMeanRhStr);

        // BL mean wind
        WindComponent meanWindCompBl = NsharpLibWinds.mean_wind(sndLys, p1, topP);
        String blMeanWindStr;
        if (NsharpLibBasics.qc(meanWindCompBl.getWdir())) {
            blMeanWindStr = String.format("BL mean = %4.0f/%.0f", meanWindCompBl.getWdir(), meanWindCompBl.getWspd());
        } else {
            blMeanWindStr = "BL mean = M";
        }
        fireInfo.setBlMean(blMeanWindStr);

        // original code calls define_parcel(3, 500); and then calls parcel()
        LParcelValues lparcelVs = NsharpLibSkparams.define_parcel(sndLys, NsharpLibSndglib.PARCELTYPE_MOST_UNSTABLE,
                500);
        Parcel pcl = NsharpLibSkparams.parcel(sndLys, -1.0F, -1.0F, lparcelVs.getPres(), lparcelVs.getTemp(),
                lparcelVs.getDwpt());

        float pw = NcSoundingTools.precip_water(sndLys, -1, -1);
        String pwStr;
        int pwColor;
        if (pw == -1 || pcl == null) {
            pwStr = "PW = M";
            pwColor = 31;
        } else {
            pwStr = String.format("PW  = %.2f", pw / 25.4f) + "in";
            if (((pw / 25.4f) < 0.5) && (pcl.getBplus() > 50) && (sfcrh < 35)) {
                pwColor = 2;
            } else {
                pwColor = 31;
            }
        }
        fireInfo.setPw(pwStr);
        fireInfo.setPwColor(pwColor);

        // BL mean wind
        WindComponent maxWindCompBl = NsharpLibWinds.max_wind(sndLys, -1, topP);
        String blMaxWindStr;
        float blMaxSpd = maxWindCompBl.getWspd();
        if (NsharpLibBasics.qc(blMaxSpd)) {
            blMaxWindStr = String.format("BL max = %4.0f/%.0f", maxWindCompBl.getWdir(), blMaxSpd);
        } else {
            blMaxWindStr = "BL max = M";
        }
        fireInfo.setBlMax(blMaxWindStr);
        if (blMaxSpd > 50) {
            fireInfo.setBlMaxColor(7);
        } else if (blMaxSpd > 40) {
            fireInfo.setBlMaxColor(2);
        } else if (blMaxSpd > 30) {
            fireInfo.setBlMaxColor(19);
        } else if (blMaxSpd > 20) {
            fireInfo.setBlMaxColor(31);
        } else if (blMaxSpd > 10) {
            fireInfo.setBlMaxColor(18);
        } else if (blMaxSpd >= 0) {
            fireInfo.setBlMaxColor(8);
        } else {
            fireInfo.setBlMaxColor(3);
        }

        // fosberg index
        float fosbergIndex = NsharpLibSkparams.fosberg(sndLys);
        String forsbergIndexStr;
        if (NsharpLibBasics.qc(fosbergIndex)) {
            forsbergIndexStr = String.format("Fosberg FWI = %4.0f", fosbergIndex);
        } else {
            forsbergIndexStr = "Fosberg FWI = M";
        }
        fireInfo.setFosbergIndex(forsbergIndexStr);
        if (fosbergIndex >= 70) {
            fireInfo.setFosbergColor(7);
        } else if (fosbergIndex >= 60) {
            fireInfo.setFosbergColor(2);
        } else if (fosbergIndex >= 50) {
            fireInfo.setFosbergColor(19);
        } else if (fosbergIndex >= 40) {
            fireInfo.setFosbergColor(31);
        } else if (fosbergIndex >= 30) {
            fireInfo.setFosbergColor(18);
        } else if (fosbergIndex >= 0) {
            fireInfo.setFosbergColor(8);
        } else {
            fireInfo.setFosbergColor(3);
        }

        return fireInfo;
    }

    public int getSfcRhColor() {
        return sfcRhColor;
    }

    public void setSfcRhColor(int sfcRhColor) {
        this.sfcRhColor = sfcRhColor;
    }

    public int getPwColor() {
        return pwColor;
    }

    public void setPwColor(int pwColor) {
        this.pwColor = pwColor;
    }

    public int getBlMaxColor() {
        return blMaxColor;
    }

    public void setBlMaxColor(int blMaxColor) {
        this.blMaxColor = blMaxColor;
    }

    public int getFosbergColor() {
        return fosbergColor;
    }

    public void setFosbergColor(int fosbergColor) {
        this.fosbergColor = fosbergColor;
    }

    public String getSfcRh() {
        return sfcRh;
    }

    public void setSfcRh(String sfcRh) {
        this.sfcRh = sfcRh;
    }

    public String getSfc() {
        return sfc;
    }

    public void setSfc(String sfc) {
        this.sfc = sfc;
    }

    public String getZeroOneKmRh() {
        return zeroOneKmRh;
    }

    public void setZeroOneKmRh(String zeroOneKmRh) {
        this.zeroOneKmRh = zeroOneKmRh;
    }

    public String getZeroOneKmMean() {
        return zeroOneKmMean;
    }

    public void setZeroOneKmMean(String zeroOneKmMean) {
        this.zeroOneKmMean = zeroOneKmMean;
    }

    public String getBlMeanRh() {
        return blMeanRh;
    }

    public void setBlMeanRh(String blMeanRh) {
        this.blMeanRh = blMeanRh;
    }

    public String getBlMean() {
        return blMean;
    }

    public void setBlMean(String blMean) {
        this.blMean = blMean;
    }

    public String getPw() {
        return pw;
    }

    public void setPw(String pw) {
        this.pw = pw;
    }

    public String getBlMax() {
        return blMax;
    }

    public void setBlMax(String blMax) {
        this.blMax = blMax;
    }

    public String getFosbergIndex() {
        return fosbergIndex;
    }

    public void setFosbergIndex(String fosberg) {
        this.fosbergIndex = fosberg;
    }

}
