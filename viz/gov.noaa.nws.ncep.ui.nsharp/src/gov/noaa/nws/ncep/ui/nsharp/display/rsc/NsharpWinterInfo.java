package gov.noaa.nws.ncep.ui.nsharp.display.rsc;

/**
 * 
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system. 
 * 
 * All methods developed in this class are based on the algorithm developed in BigSharp 
 * native C file, basics.c , by John A. Hart/SPC.
 * All methods name are defined with same name as the C function name defined in native code.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#     Engineer    Description
 * -------      -------     --------    -----------
 * 07/05/2016   RM#15923    Chin Chen   NSHARP - Native Code replacement
 *
 * </pre>
 * 
 * @author Chin Chen
 * @version 1.0
 * 
 */
import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibBasics;
import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibSkparams;
import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibSndglib;
import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibThermo;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingTools;

import java.util.List;

public class NsharpWinterInfo {
    private float mopw;

    private String oprh;

    private String layerDepth;

    private String meanLayerRh;

    private String meanLayerMixRat;

    private String meanLayerPw;

    private String meanLayerOmega;

    private String initPhase;

    private String tempProfile1;

    private String tempProfile2;

    private String tempProfile3;

    private String wetbulbProfile1;

    private String wetbulbProfile2;

    private String wetbulbProfile3;

    private String bestGuess1;

    private String bestGuess2;

    public NsharpWinterInfo() {
        super();

    }

    class PosnegTemp {
        float pos;

        float neg;

        float top;

        float bot;

        public PosnegTemp() {
            super();
            pos = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

            neg = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

            top = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

            bot = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }

        public float getPos() {
            return pos;
        }

        public void setPos(float pos) {
            this.pos = pos;
        }

        public float getNeg() {
            return neg;
        }

        public void setNeg(float neg) {
            this.neg = neg;
        }

        public float getTop() {
            return top;
        }

        public void setTop(float top) {
            this.top = top;
        }

        public float getBot() {
            return bot;
        }

        public void setBot(float bot) {
            this.bot = bot;
        }
    }

    private static PosnegTemp posneg_temperature(NsharpWinterInfo winterInfo,
            List<NcSoundingLayer> soundingLys, float upperPre, int tempType)
    /***********************************************************************/
    /* POSNEG */
    /* Calculates positive and negative areas as related to winter weather */
    /* forecasting. Search begins at 500mb, but only returns results if */
    /* a positive area is found, overlaying a negative area. */
    /*
     * tempType 1 = real temp tempType 2 = wetBulb temp
     */
    /***********************************************************************/
    {
        float pe1, h1, te1, totp, totn, pe2, h2, te2, tdef1, tdef2;
        float lyre, ptop, pbot;
        int lptr, uptr, warmlayer = 0, coldlayer = 0;

        PosnegTemp posnegTemp = winterInfo.new PosnegTemp();

        /* ----- If there is no sounding, do not compute ----- */
        if (!NsharpLibBasics.qc(NsharpLibBasics.i_temp(soundingLys, 500))
                && !NsharpLibBasics
                        .qc(NsharpLibBasics.i_temp(soundingLys, 850))) {
            return posnegTemp;
        }

        if (tempType != 1 && tempType != 2) {
            return posnegTemp;
        }
        /* ----- Find lowest observation in layer ----- */
        lptr = NsharpLibBasics.sfcIndex(soundingLys);

        /* ----- Find highest observation in layer ----- */
        uptr = 0;
        for (int i = soundingLys.size() - 1; i >= 0; i--) {
            if (soundingLys.get(i).getPressure() > upperPre) {
                uptr = i;
                break;
            }
        }

        /* ----- Start with top layer ----- */
        pe1 = upperPre;
        h1 = NsharpLibBasics.i_hght(soundingLys, pe1);
        if (tempType == 1) {
            te1 = NsharpLibBasics.i_temp(soundingLys, pe1);
        } else {
            te1 = NsharpLibThermo.wetbulb(pe1,
                    NsharpLibBasics.i_temp(soundingLys, pe1),
                    NsharpLibBasics.i_dwpt(soundingLys, pe1));
        }
        totp = totn = ptop = pbot = 0;

        for (int i = uptr; i >= lptr; i--) {
            NcSoundingLayer sndLy = soundingLys.get(i);
            if (NsharpLibBasics.qc(sndLy.getDewpoint())) {
                /* ----- Calculate every level that reports a temp ----- */
                pe2 = sndLy.getPressure();
                h2 = sndLy.getGeoHeight();
                if (tempType == 1) {
                    te2 = NsharpLibBasics.i_temp(soundingLys, pe2);
                } else {
                    te2 = NsharpLibThermo.wetbulb(pe2,
                            NsharpLibBasics.i_temp(soundingLys, pe2),
                            NsharpLibBasics.i_dwpt(soundingLys, pe2));
                }
                tdef1 = (0 - te1) / (te1 + 273.15f);
                tdef2 = (0 - te2) / (te2 + 273.15f);
                lyre = 9.8F * (tdef1 + tdef2) / 2.0F * (h2 - h1);

                /* Has a warm layer been found yet? */
                if (te2 > 0) {
                    if (warmlayer == 0) {
                        warmlayer = 1;
                        ptop = pe2;
                    }
                }
                /* Has a cold layer been found yet? */
                if (te2 < 0) {
                    if ((warmlayer == 1) && (coldlayer == 0)) {
                        coldlayer = 1;
                        pbot = pe2;
                    }
                }
                if (warmlayer > 0) {
                    if (lyre > 0) {
                        totp += lyre;
                    } else {
                        totn += lyre;
                    }
                }

                pe1 = pe2;
                h1 = h2;
                te1 = te2;
            }
        }
        if ((warmlayer == 1) && (coldlayer == 1)) {
            posnegTemp.setPos(totp);
            posnegTemp.setNeg(totn);
            posnegTemp.setTop(ptop);
            posnegTemp.setBot(pbot);
        } else {
            posnegTemp.setPos(0);
            posnegTemp.setNeg(0);
            posnegTemp.setTop(0);
            posnegTemp.setBot(0);
        }
        return posnegTemp;
    }

    /***********************************************************************/
    /* BEST_GUESS */
    /***********************************************************************/
    private static String best_guess(List<NcSoundingLayer> soundingLys,
            float init_lvl, float init_temp, float init_phase, float tpos,
            float tneg, float tsfc) {
        String bestGuess;

        /* Case: no precip */
        if (init_phase < 0) {
            return "None.";
        }

        /* Case: always too warm - Rain */
        if ((init_phase == 0) && (tneg >= 0) && (tsfc > 0)) {
            return "Rain.";
        }

        /* Case: always too cold - Snow */
        if ((init_phase == 3) && (tpos <= 0) && (tsfc <= 0)) {
            return "Snow.";
        }

        /* Case: ZR too warm at sfc - Rain */
        if ((init_phase == 1) && (tpos <= 0) && (tsfc > 0)) {
            return "Rain.";
        }

        /* Case: non-snow init...always too cold - Initphase&sleet */
        if ((init_phase == 1) && (tpos <= 0) && (tsfc <= 0)) {
            if (NsharpLibBasics.agl(soundingLys,
                    NsharpLibBasics.i_hght(soundingLys, init_lvl)) >= 3000) {
                if (init_temp <= -4) {
                    return "Sleet and Snow.";
                } else {
                    return "Sleet.";
                }
            } else {
                return "Freezing Rain/Drizzle.";
            }
        }

        /* Case: Snow...but warm at sfc */
        if ((init_phase == 3) && (tpos <= 0) && (tsfc > 0)) {
            if (tsfc > 4) {
                bestGuess = "Rain.";
            } else {
                bestGuess = "Snow.";
            }
            return bestGuess;
        }

        /* Case: Warm layer. */
        if (tpos > 0) {
            float y2;
            y2 = (.62f * tpos) + 60;
            if (-tneg > y2) {
                bestGuess = "Sleet.";
            } else {
                if (tsfc <= 0) {
                    bestGuess = "Freezing Rain.";
                } else {
                    bestGuess = "Rain.";
                }
            }
            return bestGuess;
        }

        return "Unknown.";
    }

    /**************************************************************************
     * This function is based on show_winter_new() of xwvid3.c by John Hart
     * NSSFC KCMO. Rewrite code to get all computed parameters/string for Nsharp
     * All original BigNsharp plotting code are removed.
     *************************************************************************/
    public static NsharpWinterInfo computeWinterInfo(
            List<NcSoundingLayer> soundingLys) {
        NsharpWinterInfo winterInfo = new NsharpWinterInfo();
        float pose, nege;
        float ptop, pbot, htop, hbot, mrh, mq, mo, pw, mopw;
        float init_lvl = 0;
        float init_temp = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        float init_phase = -1;
        float tsfc;
        String initStr = "N/A";

        /* Do dendritic growth zone calculations between -12C and -17C */
        ptop = NsharpLibSkparams.temp_lvl(soundingLys, -17);
        pbot = NsharpLibSkparams.temp_lvl(soundingLys, -12);
        if (ptop < 0) {
            ptop = NsharpLibBasics.sfcPressure(soundingLys);
        }
        if (pbot < 0) {
            pbot = NsharpLibBasics.sfcPressure(soundingLys);
        }
        htop = NsharpLibBasics.i_hght(soundingLys, ptop);
        hbot = NsharpLibBasics.i_hght(soundingLys, pbot);
        mrh = NsharpLibSkparams.mean_relhum(soundingLys, pbot, ptop);
        mq = NsharpLibSkparams.mean_mixratio(soundingLys, pbot, ptop);
        mo = NsharpLibSkparams.mean_omeg(soundingLys, pbot, ptop) * 1000;
        pw = NcSoundingTools.precip_water(soundingLys, ptop, pbot);
        if (pw >= 0) {
            pw = pw / 25.4f;
        } else {
            pw = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        if (!NsharpLibBasics.qc(pw) || !NsharpLibBasics.qc(mo)
                || !NsharpLibBasics.qc(mrh)) {
            mopw = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        } else {
            mopw = (mo * pw) * mrh / 100;
        }
        winterInfo.setMopw(mopw);
        String oprhStr;
        if (NsharpLibBasics.qc(mopw)) {
            oprhStr = String.format("OPRH (Omega*PW*RH):  %.2f", mopw);
        } else {
            oprhStr = "OPRH (Omega*PW*RH):  M";
        }
        winterInfo.setOprh(oprhStr);
        String layerDepth = String.format(
                "Layer Depth:     %.0f ft (%.0f - %.0f ft msl)",
                NsharpLibBasics.mtof(htop - hbot), NsharpLibBasics.mtof(hbot),
                NsharpLibBasics.mtof(htop));
        winterInfo.setLayerDepth(layerDepth);
        String meanLayerRh;
        if (NsharpLibBasics.qc(mrh)) {
            meanLayerRh = String.format("Mean Layer RH:   %.0f ", mrh) + "%";
        } else {
            meanLayerRh = "Mean Layer RH:   M";
        }
        winterInfo.setMeanLayerRh(meanLayerRh);

        String meanLayerMixRat;
        if (NsharpLibBasics.qc(mq)) {
            meanLayerMixRat = String.format("Mean Layer MixRat:%.1fg/kg", mq);
        } else {
            meanLayerMixRat = "Mean Layer MixRat: M";
        }
        winterInfo.setMeanLayerMixRat(meanLayerMixRat);

        String meanLayerPw;
        if (NsharpLibBasics.qc(pw)) {
            meanLayerPw = String.format("Mean Layer PW: %.2f in.", pw);
        } else {
            meanLayerPw = "Mean Layer PW: M";
        }
        winterInfo.setMeanLayerPw(meanLayerPw);

        String meanLayerOmega;
        if (NsharpLibBasics.qc(mo)) {
            meanLayerOmega = String.format("Mean Layer Omega: %.0f ub/s", mo);
        } else {
            meanLayerOmega = "Mean Layer Omega: M";
        }
        winterInfo.setMeanLayerOmega(meanLayerOmega);

        /**************************************************************
         * Compute init phase begin: this section is converted from init_phase()
         * of winter.c
         **************************************************************/
        /* First, determine whether VVELS are available. If they are, */
        /* use them to determine level where precipitation will develop. */
        int avail = 0;
        for (int i = 0; i < soundingLys.size(); i++) {
            if (NsharpLibBasics.qc(soundingLys.get(i).getOmega())
                    && (soundingLys.get(i).getOmega() < 1)) {
                avail++;
            }
        }
        boolean ok = false;
        float rh;
        float p1;
        if (avail < 5) {
            /* No VVELS...must look for saturated level */
            /*
             * ----- Find the highest near-saturated 50mb layer blo 5km agl ----
             */
            for (int i = soundingLys.size() - 1; i > 0; i--) {
                NcSoundingLayer sndLy = soundingLys.get(i);
                if (NsharpLibBasics.agl(soundingLys, sndLy.getGeoHeight()) < 5000.0) {
                    rh = NsharpLibThermo.mixratio(sndLy.getPressure(),
                            sndLy.getDewpoint())
                            / NsharpLibThermo.mixratio(sndLy.getPressure(),
                                    sndLy.getTemperature());
                    if (rh > 0.8) {
                        p1 = sndLy.getPressure() + 50;
                        if ((NsharpLibThermo.mixratio(p1,
                                NsharpLibBasics.i_dwpt(soundingLys, p1)) / NsharpLibThermo
                                .mixratio(p1,
                                        NsharpLibBasics.i_temp(soundingLys, p1))) > 0.8) {
                            ok = true;
                            init_lvl = p1 - 25.0f;
                            break;
                        }
                    }
                }
            }
        } else {
            /*
             * ----- Find the highest near-saturated layer with UVV in the
             * lowest 5km -----
             */
            for (int i = soundingLys.size() - 1; i > 0; i--) {

                NcSoundingLayer sndLy = soundingLys.get(i);
                if ((NsharpLibBasics.agl(soundingLys, sndLy.getGeoHeight()) < 5000)
                        && (soundingLys.get(i).getOmega() <= 0)) {
                    rh = NsharpLibThermo.mixratio(sndLy.getPressure(),
                            sndLy.getDewpoint())
                            / NsharpLibThermo.mixratio(sndLy.getPressure(),
                                    sndLy.getTemperature());
                    if (rh > 0.8) {
                        p1 = sndLy.getPressure() + 50;
                        if ((NsharpLibThermo.mixratio(p1,
                                NsharpLibBasics.i_dwpt(soundingLys, p1)) / NsharpLibThermo
                                .mixratio(p1,
                                        NsharpLibBasics.i_temp(soundingLys, p1))) > 0.8) {
                            ok = true;
                            init_lvl = p1 - 25;
                            break;
                        }
                    }
                }
            }
        }

        if (ok) {
            init_temp = NsharpLibBasics.i_temp(soundingLys, init_lvl);

            if (init_temp > 0) {
                init_phase = 0;
                initStr = "Rain";
            }
            if ((init_temp <= 0) && (init_temp > -5)) {
                init_phase = 1;
                initStr = "Freezing Rain";
            }
            if ((init_temp <= -5) && (init_temp > -9)) {
                init_phase = 1;
                initStr = "ZR/S Mix";
            }
            if (init_temp <= -9) {
                init_phase = 3;
                initStr = "Snow";
            }
            /**************************************************************
             * end of init_phase()
             **************************************************************/
        }

        if (init_lvl > 100) {
            String initPhaseStr = String.format(
                    "Init Phase:%s from:%.0fmb(%.0fft msl;%.1fC)", initStr,
                    init_lvl, NsharpLibBasics.mtof(NsharpLibBasics.i_hght(
                            soundingLys, init_lvl)), NsharpLibBasics.i_temp(
                            soundingLys, init_lvl));
            winterInfo.setInitPhase(initPhaseStr);

        } else {
            winterInfo
                    .setInitPhase("Init Phase: No Precipitation layers found.");
        }

        /* ----- Temperature Pos/Neg Areas ----- */
        float startP;
        if (init_lvl > 0) {
            startP = init_lvl;
        } else {
            startP = 500;
        }
        PosnegTemp posnegTemp = posneg_temperature(winterInfo, soundingLys,
                startP, 1);
        pose = posnegTemp.getPos();
        nege = posnegTemp.getNeg();
        ptop = posnegTemp.getTop();
        pbot = posnegTemp.getBot();
        if ((pose > 0) && (nege < 0)) {
            String tempProfile1 = String.format("Pos=%.0fJ/kg Neg=%.0fJ/kg",
                    pose, nege);
            winterInfo.setTempProfile1(tempProfile1);

            float p2Height = NsharpLibBasics.mtof(NsharpLibBasics.i_hght(
                    soundingLys, ptop)
                    - NsharpLibBasics.i_hght(soundingLys, pbot));
            String tempProfile2 = String.format(
                    "Melt Lyr:%3.0f-%3.0fmb(%.0fft)", ptop, pbot, p2Height);
            winterInfo.setTempProfile2(tempProfile2);

            float p3Pres = NsharpLibBasics.sfcPressure(soundingLys);
            float p3Height = NsharpLibBasics.mtof(NsharpLibBasics.i_hght(
                    soundingLys, pbot)
                    - NsharpLibBasics.i_hght(soundingLys, p3Pres));
            String tempProfile3 = String.format(
                    "Frz Lyr:%3.0f-%4.0fmb(%.0fft)", pbot, p3Pres, p3Height);
            winterInfo.setTempProfile3(tempProfile3);
        } else {
            winterInfo.setTempProfile1("");
            winterInfo.setTempProfile2("Warm/Cold layers not found\n");
            winterInfo.setTempProfile3("");
        }

        /* ----- WetBulb Pos/Neg Areas ----- */
        PosnegTemp posnegWebbulbTemp = posneg_temperature(winterInfo,
                soundingLys, startP, 2);
        pose = posnegWebbulbTemp.getPos();
        nege = posnegWebbulbTemp.getNeg();
        ptop = posnegWebbulbTemp.getTop();
        pbot = posnegWebbulbTemp.getBot();
        if ((pose > 0) && (nege < 0)) {
            String wetbulbProfile1 = String.format("Pos=%.0fJ/kg Neg=%.0fJ/kg",
                    pose, nege);
            winterInfo.setWetbulbProfile1(wetbulbProfile1);

            float p2Height = NsharpLibBasics.mtof(NsharpLibBasics.i_hght(
                    soundingLys, ptop)
                    - NsharpLibBasics.i_hght(soundingLys, pbot));
            String wetbulbProfile2 = String.format(
                    "Melt Lyr:%3.0f-%3.0fmb(%.0fft)", ptop, pbot, p2Height);
            winterInfo.setWetbulbProfile2(wetbulbProfile2);

            float p3Pres = NsharpLibBasics.sfcPressure(soundingLys);
            float p3Height = NsharpLibBasics.mtof(NsharpLibBasics.i_hght(
                    soundingLys, pbot)
                    - NsharpLibBasics.i_hght(soundingLys, p3Pres));
            String wetbulbProfile3 = String.format(
                    "Frz Lyr:%3.0f-%4.0fmb(%.0fft)", pbot, p3Pres, p3Height);
            winterInfo.setWetbulbProfile3(wetbulbProfile3);
        } else {
            winterInfo.setWetbulbProfile1("");
            winterInfo.setWetbulbProfile2("Warm/Cold layers not found\n");
            winterInfo.setWetbulbProfile3("");
        }
        // Chin: the following are derived from best_guess_ptype() of xwvid3.c

        tsfc = NsharpLibBasics.sfcTemperature(soundingLys);
        String bestGuess1 = best_guess(soundingLys, init_lvl, init_temp,
                init_phase, pose, nege, tsfc);
        winterInfo.setBestGuess1(bestGuess1);
        String bestGuess2 = String.format("Based on sfc temperature of %.1f F",
                NsharpLibBasics.ctof(tsfc));
        winterInfo.setBestGuess2(bestGuess2);

        return winterInfo;
    }

    public float getMopw() {
        return mopw;
    }

    public void setMopw(float mopw) {
        this.mopw = mopw;
    }

    public String getOprh() {
        return oprh;
    }

    public void setOprh(String oprh) {
        this.oprh = oprh;
    }

    public String getLayerDepth() {
        return layerDepth;
    }

    public void setLayerDepth(String layerDepth) {
        this.layerDepth = layerDepth;
    }

    public String getMeanLayerRh() {
        return meanLayerRh;
    }

    public void setMeanLayerRh(String meanLayerRh) {
        this.meanLayerRh = meanLayerRh;
    }

    public String getMeanLayerMixRat() {
        return meanLayerMixRat;
    }

    public void setMeanLayerMixRat(String meanLayerMixRat) {
        this.meanLayerMixRat = meanLayerMixRat;
    }

    public String getMeanLayerPw() {
        return meanLayerPw;
    }

    public void setMeanLayerPw(String meanLayerPw) {
        this.meanLayerPw = meanLayerPw;
    }

    public String getMeanLayerOmega() {
        return meanLayerOmega;
    }

    public void setMeanLayerOmega(String meanLayerOmega) {
        this.meanLayerOmega = meanLayerOmega;
    }

    public String getInitPhase() {
        return initPhase;
    }

    public void setInitPhase(String initPhase) {
        this.initPhase = initPhase;
    }

    public String getTempProfile1() {
        return tempProfile1;
    }

    public void setTempProfile1(String tempProfile1) {
        this.tempProfile1 = tempProfile1;
    }

    public String getTempProfile2() {
        return tempProfile2;
    }

    public void setTempProfile2(String tempProfile2) {
        this.tempProfile2 = tempProfile2;
    }

    public String getTempProfile3() {
        return tempProfile3;
    }

    public void setTempProfile3(String tempProfile3) {
        this.tempProfile3 = tempProfile3;
    }

    public String getWetbulbProfile1() {
        return wetbulbProfile1;
    }

    public void setWetbulbProfile1(String wetbulbProfile1) {
        this.wetbulbProfile1 = wetbulbProfile1;
    }

    public String getWetbulbProfile2() {
        return wetbulbProfile2;
    }

    public void setWetbulbProfile2(String wetbulbProfile2) {
        this.wetbulbProfile2 = wetbulbProfile2;
    }

    public String getWetbulbProfile3() {
        return wetbulbProfile3;
    }

    public void setWetbulbProfile3(String wetbulbProfile3) {
        this.wetbulbProfile3 = wetbulbProfile3;
    }

    public String getBestGuess1() {
        return bestGuess1;
    }

    public void setBestGuess1(String bestGuess1) {
        this.bestGuess1 = bestGuess1;
    }

    public String getBestGuess2() {
        return bestGuess2;
    }

    public void setBestGuess2(String bestGuess2) {
        this.bestGuess2 = bestGuess2;
    }

}
