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
import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibSndglib;
import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibWinds;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.Parcel;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.WindComponent;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

public class NsharpHailInfo {
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(NsharpHailInfo.class);

    public static int HAIL_STRING_LINES = 8;

    public static int HAIL_STRING_LINES_NO_MATCH = 7;

    public class HailInfoContainer {

        private String reportHailStr;

        private int matches;// 0 means no match

        private int member; // 0 means No Convecting members

        private List<String> hailStrList = new ArrayList<>();

        private List<Integer> hailStrColorList = new ArrayList<>();

        public String getReportHailStr() {
            return reportHailStr;
        }

        public void setReportHailStr(String reportHailStr) {
            this.reportHailStr = reportHailStr;
        }

        public int getMatches() {
            return matches;
        }

        public void setMatches(int matches) {
            this.matches = matches;
        }

        public int getMember() {
            return member;
        }

        public void setMember(int member) {
            this.member = member;
        }

        public List<String> getHailStrList() {
            return hailStrList;
        }

        public void setHailStrList(List<String> hailStrList) {
            this.hailStrList = hailStrList;
        }

        public List<Integer> getHailStrColorList() {
            return hailStrColorList;
        }

        public void setHailStrColorList(List<Integer> hailStrColorList) {
            this.hailStrColorList = hailStrColorList;
        }

    }

    // internally used variables
    private List<NcSoundingLayer> hailSndLys;

    // TFI array in original code; high resolution sounding array
    // 1= pressure, 2 = always 0 saved in SpecHumidity, 3= Temp, 4= dew point, 5
    // = Height
    private Map<Integer, NcSoundingLayer> tfiSndLys = new HashMap<>();

    // WOLKDTA array in original code, ARRAY FOR CLOUD MODEL OUTPUT
    // 1= pressure, 2 = CP/HGTE (use SpecHumidity to save) ,3= Temp, 4= dew
    // point, 5 = T1 (Tmax, use omega to save), 6= RS (use DPD to save, 7=
    // VU (use WindSpeed to save), 8= Height, 9 = total water ( use wind
    // direction to save it)
    private Map<Integer, NcSoundingLayer> wolkdtaSndLys = new HashMap<>();

    private float esicat = 0;

    // UPDRAFT VELOCITY AT CLOUD BASE
    private float WBASVU;

    // SURFACE PRESSURE
    private float OPPDRUK;

    // CLOUD BASE PRESSURE
    private float WBASP;

    // CLOUD BASE TEMP
    private float WBASTMP;

    // SATURATION MIXING RATIO AT CLOUD BASE
    private float WBASRS;

    // cloud type
    private int ITIPWLK;

    private float CAPE;

    // PSEUDOADIABAT (IN DEGREES K)
    private float PSEUDO;

    private float NTRAIN;

    // BETA = BETTS' ENTRAINMENT PARAMETER
    private float BETA;

    private float UPMAXV, ZBAS, nofroze, Daloft, DENSE;

    // DIAMETER OF HAILSTONE
    private float D;

    private float ESPT, ESPP, G = 9.78956f, PI = 3.141592654f;

    // global set by MASSAGR() and used by HEATBUD()
    private float GM, GM1, DGM, DGMW, DGMI, DI, GMW, TS, FW;

    /*************************************************************
     * This function is derived from show_skewtpage1() and show_hail_new() of
     * xwvid3.c of BigNsharp by John Hart NSSFC KCMO Rewrite code to get all
     * computed parameters/string for CAVE. All original BigNsharp gui code are
     * removed.
     *************************************************************/
    public HailInfoContainer computeHailInfo(List<NcSoundingLayer> sndLys,
            NsharpWeatherDataStore weatherDataStore, NsharpSarsInfo sarsInfo) {
        HailInfoContainer hailInfo = new HailInfoContainer();
        Parcel muParcel = weatherDataStore.getParcelMap().get(
                NsharpLibSndglib.PARCELTYPE_MOST_UNSTABLE);
        float p_bot = weatherDataStore.getEffLyPress().getBottomPress();
        float el = 12000.0f;
        if (muParcel != null && muParcel.getBplus() >= 100) {
            el = muParcel.getElAgl();
        }
        float base = NsharpLibBasics.agl(sndLys,
                NsharpLibBasics.i_hght(sndLys, p_bot));
        float depth = (el - base);
        float effdep = base + (depth * 0.5f);
        float effPress = NsharpLibBasics.i_pres(sndLys,
                NsharpLibBasics.msl(sndLys, effdep));
        WindComponent windShear = NsharpLibWinds.wind_shear(sndLys, p_bot,
                effPress);
        float ebs = NsharpLibBasics.kt_to_mps(windShear.getWspd()) / effdep;
        float mumixr;
        Map<Integer, Float> hvars = null;
        if (muParcel != null) {
            mumixr = muParcel.getMixRatio();
            hvars = hailCast1(ebs, mumixr, sndLys);
        }
        if (hvars == null) {
            return null;
        }
        // ----- Hail Model Output -----
        String str = String
                .format("Hailcast1->(%.0f convecting)T/Td= %.0fF/%.0fF StormCat:%.0f/4",
                        hvars.get(17), NsharpLibBasics.ctof(hvars.get(1)),
                        NsharpLibBasics.ctof(hvars.get(2)), hvars.get(24));
        hailInfo.getHailStrList().add(0, str);
        hailInfo.getHailStrColorList().add(0, 31);
        if (hvars.get(23) >= 1.00 && hvars.get(17) >= 1) {
            hailInfo.getHailStrColorList().add(1, 3);
        } else if (hvars.get(23) >= 1.95) {
            hailInfo.getHailStrColorList().add(1, 2);
        } else {
            hailInfo.getHailStrColorList().add(1, 31);
        }

        str = String.format(
                "Avg:%.1f in. Max:%.1f in. Min:%.1f in. SIG =%.0f SVR =%.0f",
                hvars.get(18), hvars.get(19), hvars.get(20), hvars.get(21),
                hvars.get(22));
        hailInfo.getHailStrList().add(1, str);

        hailInfo.getHailStrColorList().add(2, 31);
        str = String
                .format("Hailcast2->(%.0f convecting)T/Td= %.0fF/%.0fF StormCat:%.0f/4",
                        hvars.get(3), NsharpLibBasics.ctof(hvars.get(1)),
                        NsharpLibBasics.ctof(hvars.get(2)), hvars.get(16));
        hailInfo.getHailStrList().add(2, str);
        if (hvars.get(14) >= 1.00 && hvars.get(3) >= 1) {
            hailInfo.getHailStrColorList().add(3, 3);
        } else if (hvars.get(14) >= 1.95) {
            hailInfo.getHailStrColorList().add(3, 2);
        } else {
            hailInfo.getHailStrColorList().add(3, 31);
        }

        str = String.format(
                "Avg:%.1f in. Max:%.1f in. Min:%.1f in. SIG =%.0f SVR =%.0f",
                hvars.get(4), hvars.get(5), hvars.get(6), hvars.get(7),
                hvars.get(8));
        hailInfo.getHailStrList().add(3, str);
        if (hvars.get(3) == 0) {
            hvars.put(14, 0f);
        }
        if (hvars.get(3) == 0 && hvars.get(17) == 0) {
            hailInfo.getHailStrColorList().add(4, 31);
            hailInfo.getHailStrList().add(4, "No Convecting Members");
            hailInfo.setMember(0);
            hailInfo.getHailStrColorList().add(5, 31);
            hailInfo.getHailStrList().add(5, "");
        } else {
            // If convecting members then...........
            hailInfo.setMember(1);
            if (hvars.get(23) < 1.00) {
                hailInfo.getHailStrColorList().add(4, 31);
            } else if (hvars.get(23) >= 1.00 && hvars.get(17) >= 1) {
                hailInfo.getHailStrColorList().add(4, 3);
            } else if (hvars.get(23) >= 1.95) {
                hailInfo.getHailStrColorList().add(4, 2);
            } else {
                hailInfo.getHailStrColorList().add(4, 31);
            }
            str = String.format("Hailcast1--->   %.1f", hvars.get(23));
            hailInfo.getHailStrList().add(4, str);
            if (hvars.get(14) < 1.00) {
                hailInfo.getHailStrColorList().add(5, 31);
            } else if (hvars.get(14) >= 1.00 && hvars.get(3) >= 1) {
                hailInfo.getHailStrColorList().add(5, 3);
            } else if (hvars.get(14) >= 1.95) {
                hailInfo.getHailStrColorList().add(5, 2);
            } else {
                hailInfo.getHailStrColorList().add(5, 31);
            }
            str = String.format("Hailcast2--->   %.1f", hvars.get(14));
            hailInfo.getHailStrList().add(5, str);
        }

        // ----- SARS matches -----
        // SARS hail size
        int matches2 = sarsInfo.getMatches2();
        float avsize = sarsInfo.getAvsize();
        hailInfo.setMatches(matches2);
        if (matches2 == 0) {
            hailInfo.getHailStrColorList().add(6, 31);
            hailInfo.getHailStrList().add(6, "No Matches");
            hailInfo.getHailStrColorList().add(7, 31);
            hailInfo.getHailStrList().add(7, "");
        } else if (matches2 == 1 || avsize <= 1.49) {
            hailInfo.getHailStrColorList().add(6, 31);
            hailInfo.getHailStrColorList().add(7, 31);
        } else if (matches2 >= 2 && (avsize < 2.06 && avsize > 1.49)) {
            hailInfo.getHailStrColorList().add(6, 3);
            hailInfo.getHailStrColorList().add(7, 3);
        } else if (matches2 >= 2 && avsize >= 2.06) {
            hailInfo.getHailStrColorList().add(6, 2);
            hailInfo.getHailStrColorList().add(7, 2);
        } else {
            hailInfo.getHailStrColorList().add(6, 31);
            hailInfo.getHailStrColorList().add(7, 31);
        }
        if (matches2 > 0) {
            if (avsize <= 1.49) {
                hailInfo.getHailStrList().add(6,
                        "Best guess from SARS = < 1 inch");
                hailInfo.setReportHailStr("<1");
            } else if ((avsize > 1.49) && (avsize <= 1.68)) {
                hailInfo.getHailStrList().add(6,
                        "Best guess from SARS = 1 - 1.5 inch");
                hailInfo.setReportHailStr("1-1.5");
            } else if ((avsize > 1.68) && (avsize <= 2.06)) {
                hailInfo.getHailStrList().add(6,
                        "Best guess from SARS = 1.75 inch");
                hailInfo.setReportHailStr("1.75");
            } else if ((avsize > 2.06) && (avsize <= 2.39)) {
                hailInfo.getHailStrList().add(6,
                        "Best guess from SARS = 2 inch");
                hailInfo.setReportHailStr("2");
            } else if ((avsize > 2.39) && (avsize <= 2.52)) {
                hailInfo.getHailStrList().add(6,
                        "Best guess from SARS = 2.5 inch");
                hailInfo.setReportHailStr("2.5");
            } else if ((avsize > 2.52) && (avsize <= 2.56)) {
                hailInfo.getHailStrList().add(6,
                        "Best guess from SARS = 2.75 inch");
                hailInfo.setReportHailStr("2.75");
            } else if ((avsize > 2.56) && (avsize <= 2.64)) {
                hailInfo.getHailStrList().add(6,
                        "Best guess from SARS = 3 - 4 inch");
                hailInfo.setReportHailStr("3-4");
            } else {
                hailInfo.getHailStrList().add(6,
                        "Best guess from SARS = > 4 inch");
                hailInfo.setReportHailStr(">4");
            }
            str = String.format("AVG size = %.2f (based on %d matches)",
                    avsize, matches2);
            hailInfo.getHailStrList().add(7, str);

        }
        return hailInfo;
    }

    /**
     * constructSoundingListForHailCast()
     * 
     * This method is converted from write_hail_file() of parameterization.c. It
     * constructs a new sounding list specifically for hailcast1() to use
     * 
     * @param sndLys
     * @return List<NcSoundingLayer> hailSndLys
     */
    private List<NcSoundingLayer> constructHailCastSndList(
            List<NcSoundingLayer> sndLys) {
        boolean found850 = false;
        boolean found700 = false;
        boolean found500 = false;
        boolean found300 = false;
        boolean found400 = false;
        boolean found250 = false;
        boolean found200 = false;
        boolean found100 = false;
        List<NcSoundingLayer> hailSndLys = new ArrayList<>();
        float sfcPress = NsharpLibBasics.sfcPressure(sndLys);

        if (sfcPress < 850) {
            found850 = true;
        }

        for (NcSoundingLayer ly : sndLys) {
            float lyPress = ly.getPressure();

            if ((lyPress <= 850) && found850 == false) {
                if (850 != lyPress) {
                    // add 850mb layer
                    NcSoundingLayer newLy = new NcSoundingLayer(850,
                            NsharpLibBasics.i_hght(sndLys, 850),
                            NsharpLibBasics.i_temp(sndLys, 850),
                            NsharpLibBasics.i_dwpt(sndLys, 850),
                            NsharpLibBasics.i_wspd(sndLys, 850) * .514f,
                            NsharpLibBasics.i_wdir(sndLys, 850), 0, 0, 0, 0, 0,
                            0);
                    hailSndLys.add(newLy);
                    found850 = true;
                }
            }

            if ((lyPress <= 700) && found700 == false) {
                if (700 != lyPress) {
                    // add700mb layer
                    NcSoundingLayer newLy = new NcSoundingLayer(700,
                            NsharpLibBasics.i_hght(sndLys, 700),
                            NsharpLibBasics.i_temp(sndLys, 700),
                            NsharpLibBasics.i_dwpt(sndLys, 700),
                            NsharpLibBasics.i_wspd(sndLys, 700) * .514f,
                            NsharpLibBasics.i_wdir(sndLys, 700), 0, 0, 0, 0, 0,
                            0);
                    hailSndLys.add(newLy);
                    found700 = true;
                }
            }

            if ((lyPress <= 500) && found500 == false) {
                if (500 != lyPress) {
                    // add 500mb layer
                    NcSoundingLayer newLy = new NcSoundingLayer(500,
                            NsharpLibBasics.i_hght(sndLys, 500),
                            NsharpLibBasics.i_temp(sndLys, 500),
                            NsharpLibBasics.i_dwpt(sndLys, 500),
                            NsharpLibBasics.i_wspd(sndLys, 500) * .514f,
                            NsharpLibBasics.i_wdir(sndLys, 500), 0, 0, 0, 0, 0,
                            0);
                    hailSndLys.add(newLy);
                    found500 = true;
                }
            }
            if ((lyPress <= 400) && found400 == false) {
                if (400 != lyPress) {
                    // add 400mb layer
                    NcSoundingLayer newLy = new NcSoundingLayer(400,
                            NsharpLibBasics.i_hght(sndLys, 400),
                            NsharpLibBasics.i_temp(sndLys, 400),
                            NsharpLibBasics.i_dwpt(sndLys, 400),
                            NsharpLibBasics.i_wspd(sndLys, 400) * .514f,
                            NsharpLibBasics.i_wdir(sndLys, 400), 0, 0, 0, 0, 0,
                            0);
                    hailSndLys.add(newLy);
                    found400 = true;
                }
            }

            if ((lyPress <= 300) && found300 == false) {
                if (300 != lyPress) {
                    // add 300mb layer
                    NcSoundingLayer newLy = new NcSoundingLayer(300,
                            NsharpLibBasics.i_hght(sndLys, 300),
                            NsharpLibBasics.i_temp(sndLys, 300),
                            NsharpLibBasics.i_dwpt(sndLys, 300),
                            NsharpLibBasics.i_wspd(sndLys, 300) * .514f,
                            NsharpLibBasics.i_wdir(sndLys, 300), 0, 0, 0, 0, 0,
                            0);
                    hailSndLys.add(newLy);
                    found300 = true;
                }
            }

            if ((lyPress <= 250) && found250 == false) {
                if (250 != lyPress) {
                    // add 250mb layer
                    NcSoundingLayer newLy = new NcSoundingLayer(250,
                            NsharpLibBasics.i_hght(sndLys, 250),
                            NsharpLibBasics.i_temp(sndLys, 250),
                            NsharpLibBasics.i_dwpt(sndLys, 250),
                            NsharpLibBasics.i_wspd(sndLys, 250) * .514f,
                            NsharpLibBasics.i_wdir(sndLys, 250), 0, 0, 0, 0, 0,
                            0);
                    hailSndLys.add(newLy);
                    found250 = true;
                }
            }

            if ((lyPress <= 200) && found200 == false) {
                if (200 != lyPress) {
                    // add 200mb layer
                    NcSoundingLayer newLy = new NcSoundingLayer(200,
                            NsharpLibBasics.i_hght(sndLys, 200),
                            NsharpLibBasics.i_temp(sndLys, 200),
                            NsharpLibBasics.i_dwpt(sndLys, 200),
                            NsharpLibBasics.i_wspd(sndLys, 200) * .514f,
                            NsharpLibBasics.i_wdir(sndLys, 200), 0, 0, 0, 0, 0,
                            0);
                    hailSndLys.add(newLy);
                    found200 = true;
                }
            }

            if ((lyPress <= 100) && found100 == false) {
                if (100 != lyPress) {
                    // add 100mb layer
                    NcSoundingLayer newLy = new NcSoundingLayer(100,
                            NsharpLibBasics.i_hght(sndLys, 100),
                            NsharpLibBasics.i_temp(sndLys, 100),
                            NsharpLibBasics.i_dwpt(sndLys, 100),
                            NsharpLibBasics.i_wspd(sndLys, 100) * .514f,
                            NsharpLibBasics.i_wdir(sndLys, 100), 0, 0, 0, 0, 0,
                            0);
                    hailSndLys.add(newLy);
                    found100 = true;
                }
            }

            if (NsharpLibBasics.qc(ly.getTemperature())) {
                NcSoundingLayer newLy = new NcSoundingLayer(ly.getPressure(),
                        ly.getGeoHeight(), ly.getTemperature(),
                        ly.getDewpoint(), ly.getWindSpeed() * .514f,
                        ly.getWindDirection(), ly.getWindU(), ly.getWindV(),
                        ly.getOmega(), ly.getSpecHumidity(),
                        ly.getRelativeHumidity());
                hailSndLys.add(newLy);
            }
        }
        return hailSndLys;
    }

    /**
     * INTERPOLATE A HIGHER RESOLUTION SOUNDING
     * 
     * SUBROUTINE INTSOUN()
     */
    private void INTSOUN(List<NcSoundingLayer> hailCloudSndLys) {
        int itel = hailCloudSndLys.size();
        tfiSndLys.clear();
        // FIND THE STATION HEIGHT FROM THE INPUT SOUNDING
        NcSoundingLayer tfiLy = new NcSoundingLayer();
        NcSoundingLayer cloudLy = hailCloudSndLys.get(0);
        tfiLy.setGeoHeight(cloudLy.getGeoHeight());

        float PVLAK = (cloudLy.getPressure() / 10) * 10;
        int I = 0;
        int JTEL = 0;

        // SEARCH FOR THE TWO LEVELS EACH SIDE OF Pressure
        while (I < itel - 1 && JTEL < 90) {
            NcSoundingLayer cloudLyI = hailCloudSndLys.get(I);
            NcSoundingLayer cloudLyIp1 = hailCloudSndLys.get(I + 1);
            float pI = cloudLyI.getPressure();
            float pIp1 = cloudLyIp1.getPressure();
            if (PVLAK <= pI && PVLAK > pIp1 && pIp1 != 0.0) {
                float PDIFF = pI - pIp1;
                float VDIFF = pI - PVLAK;
                float VERH = VDIFF / PDIFF;
                float TDIFF = cloudLyIp1.getTemperature()
                        - cloudLyI.getTemperature();
                float TDDIFF = cloudLyIp1.getDewpoint()
                        - cloudLyI.getDewpoint();
                tfiLy.setPressure(PVLAK);
                tfiLy.setTemperature(cloudLyI.getTemperature() + (TDIFF * VERH));

                if (cloudLyI.getTemperature() >= 350.0) {
                    tfiLy.setDewpoint(cloudLyIp1.getDewpoint());

                } else {
                    tfiLy.setDewpoint(cloudLyI.getDewpoint() + (TDDIFF * VERH));
                }
                if (PVLAK == pI) {
                    tfiLy.setDewpoint(cloudLyI.getDewpoint());
                }

                tfiLy.setSpecHumidity(0);// used SpecHumidity to save TFI[N,2],
                                         // and it is always 0

                tfiSndLys.put(JTEL, tfiLy);
                JTEL = JTEL + 1;
                if (JTEL < 90) {
                    // original code check if(IHGTJA == 1), however, IHGTJA is
                    // set
                    // to 1 for all
                    // therefore, do not have to check it
                    // geoHeight for next level, note
                    float geoHeightP1 = (287.04f * (tfiLy.getTemperature() + 1.0f) / (9.80616f * tfiLy
                            .getPressure() * 100.0f))
                            * 2500.0f
                            + tfiLy.getGeoHeight();
                    // construct sndly for new JTEL level
                    tfiLy = new NcSoundingLayer();
                    tfiLy.setGeoHeight(geoHeightP1);
                    PVLAK = PVLAK - 25.0f;
                }
            } else {
                I = I + 1;
            }
        }
        /**
         * IF(sounding(ITEL,2).LE.10.0)THEN TFI(JTEL-1,4)=sounding(ITEL-1,4)
         * TFI(JTEL-1,5)=sounding(ITEL-1,5) ENDIF. This block of original code
         * is not possible as sounding data pressure does not below 10 always.
         */

    }

    /**
     * CLOUD BASE PARAMETERS
     * 
     * WOLKBAS(): construct wolkdtaSndLys, and set WBASR, WBASTMP, WBASP globals
     */

    private void WOLKBAS(float TMAX, float TDOU) {
        wolkdtaSndLys.clear();
        // CALC THE CLOUD BASE PARAMETERS USING THE SFC T AND TD
        float CP = 1004.64f;
        float RD = 287.04f;
        float RV = 461.48f;
        float EPS = 0.622f;
        float DP = 100.0f;
        float T1 = TMAX;
        float TD1 = TDOU;
        float P = OPPDRUK;

        // CALC THE MIXING RATIO USING THE CLAUSSIUS CLAPEYRON EQTN:
        // R(AT T1)=RS(AT TD1)
        float AL = (-0.566f * (TD1 - 273.16f) + 597.3f) * 4186.0f;
        float E = 611.0f * (float) Math.exp((AL / RV)
                * (1.0f / 273.16f - 1.0f / TD1));
        float R = EPS * E / P;
        float RS = 0;
        // CALC T ALONG THE DALR UNTIL RS=R IE., T=TD (CLOUD BASE)
        int i = 0;
        Integer[] tfiKey = {};
        tfiKey = tfiSndLys.keySet().toArray(tfiKey);
        for (int count = 0; count < 500; count++) {
            if (i >= tfiKey.length) {
                break;
            }
            NcSoundingLayer tfiLy = tfiSndLys.get(tfiKey[i]);
            // CALC THE MIXING RATIO AT T1:
            AL = (-0.566f * (T1 - 273.16f) + 597.3f) * 4186.0f;
            float ES = 611.0f * (float) Math.exp((AL / RV)
                    * (1.0f / 273.16f - 1.0f / T1));
            RS = EPS * ES / P;
            // WRITE THE DATA TO WOLKDTA
            if (tfiLy.getPressure() >= (P / 100.0f)) {
                NcSoundingLayer wolkdtaLy = new NcSoundingLayer();
                wolkdtaLy.setPressure(tfiLy.getPressure());
                wolkdtaLy.setTemperature(tfiLy.getTemperature() - 273.16f);
                wolkdtaLy.setDewpoint(tfiLy.getDewpoint() - 273.16f);
                wolkdtaLy.setGeoHeight(tfiLy.getGeoHeight());
                // use unused omega variable to save T1 here, i.e. at WOLKDTA[N,
                // 5]
                wolkdtaLy.setOmega(T1 - 273.16f);
                // use unused DPD variable to save RS here, i.e. at WOLKDTA[N,
                // 6]
                wolkdtaLy.setDpd(RS * 1000);

                wolkdtaLy.setSpecHumidity(0);// used SpecHumidity to save
                                             // WOLKDTA[N,2]
                wolkdtaSndLys.put(tfiKey[i], wolkdtaLy);
                i++;

            }
            if (RS <= R) {
                WBASRS = RS;
                WBASP = P;
                WBASTMP = T1;
                NcSoundingLayer wolkdtaLy = new NcSoundingLayer();
                // save WOLKDTA[N,1]
                wolkdtaLy.setPressure(WBASP / 100.0f);
                // save WOLKDTA[N,2] use SpecHumidity
                wolkdtaLy.setSpecHumidity(XINTBAS(WBASP, 5));
                // save WOLKDTA[N,3]
                wolkdtaLy.setTemperature(XINTBAS(WBASP, 3) - 273.16f);
                // save WOLKDTA[N,4]
                wolkdtaLy.setDewpoint(XINTBAS(WBASP, 4) - 273.16f);
                // save WOLKDTA[N,5] use omega variable to save T1 here
                wolkdtaLy.setOmega(T1 - 273.16f);
                // save WOLKDTA[N,6] use DPD variable to save RS here
                wolkdtaLy.setDpd(RS * 1000);
                // save WOLKDTA[N,8]
                wolkdtaLy.setGeoHeight(XINTBAS(WBASP, 5));

                wolkdtaSndLys.put(tfiKey[i], wolkdtaLy);
                break;
            }
            // CALC THE TEMP AT THE NEXT LEVEL FOLLOWING THE DALR
            T1 = T1 - RD * T1 / (CP * P) * DP;
            P = P - DP;
        }

        if (RS > R) {
            // CLOUD BASE NOT OBTAINED - AIR TOO DRY
            WBASRS = 9999;
            WBASTMP = 9999;
            WBASP = 9999;
        }

    }

    /*******************************************************************
     *** INTERPOLATION OF CLOUD BASE BETWEEN 2 LEVELS
     *******************************************************************/
    private float XINTBAS(float PP, int JVAL) {
        // EARCH FOR THE 2 LEVELS EACH SIDE OF P
        float P = PP / 100.0f;
        return XINTERP(P, JVAL);
    }

    /**
     * INTERPOLATE BETWEEN TWO LEVELS
     */
    private float XINTERP(float P, int JVAL) {
        // SOEK DIE 2 VLAKKE WEERSKANTE VAN P
        Integer[] tfiKey = {};
        tfiKey = tfiSndLys.keySet().toArray(tfiKey);
        for (int i = 0; i < tfiKey.length - 1; i++) {
            NcSoundingLayer tfiLy = tfiSndLys.get(tfiKey[i]);
            NcSoundingLayer tfiLyP1 = tfiSndLys.get(tfiKey[i + 1]);
            if (P < tfiLy.getPressure() && P >= tfiLyP1.getPressure()) {
                float PDIFF = tfiLy.getPressure() - tfiLyP1.getPressure();
                float VDIFF = tfiLy.getPressure() - P;
                float VERH = VDIFF / PDIFF;
                float ADIFF = 0;
                float lowLevelValue = 0;
                if (JVAL == 3) {
                    ADIFF = tfiLyP1.getTemperature() - tfiLy.getTemperature();
                    lowLevelValue = tfiLy.getTemperature();
                } else if (JVAL == 4) {
                    ADIFF = tfiLyP1.getDewpoint() - tfiLy.getDewpoint();
                    lowLevelValue = tfiLy.getDewpoint();
                } else if (JVAL == 5) {
                    ADIFF = tfiLyP1.getGeoHeight() - tfiLy.getGeoHeight();
                    lowLevelValue = tfiLy.getGeoHeight();
                }
                boolean adiffNeg = false;
                if (ADIFF < 0.0) {
                    ADIFF = -1.0f * ADIFF;
                    adiffNeg = true;
                }
                float XINTERP;
                if (adiffNeg) {
                    XINTERP = lowLevelValue - (ADIFF * VERH);
                } else {
                    XINTERP = lowLevelValue + (ADIFF * VERH);
                }
                return XINTERP;
            }
        }
        return -9999;

    }

    /**
     * CALCULATE THE TEMP AT THE LEVEL DP PASCAL HIGHER AS P ALONG THE WALR
     */
    private float XNATV(float TK, float P, float DP) {
        float CP = 1004.64f, RD = 287.04f, RV = 461.48f, EPS = 0.622f;
        // CALCULATE THE LATENT HEAT RELEASE IN PARCEL DUE TO CONDENSATION
        // OF CLOUD WATER
        float AL;
        if (TK < 233.16) {
            AL = (-0.566f * (TK - 273.16f) + 1200.0f) * 4186.0f;
        } else {
            AL = (-0.566f * (TK - 273.16f) + 597.3f) * 4186.0f;
        }
        float TB = TK;
        // CALC DIFFERENT PARTS OF THE EQUATION
        float XP = 611.0f * (float) Math.exp((AL / RV)
                * (1.0f / 273.16f - 1.0f / TB));
        float A = 1.0f + (AL * EPS * XP / (RD * TB * P));
        float B = 1.0f + (EPS * EPS * AL * AL * XP / (CP * RD * P * TB * TB));
        // CALC THE TEMP AT THE NEXT LEVEL BY FOLLOWING THE WALR
        float XNATV = TB - (A / B) * ((RD * TB * DP) / (P * CP));
        return XNATV;
    }

    /**
     * CALC THE TEMP AT THE NEXT LEVEL WHICH IS DP PASCAL HIGHER THAN P.
     * PARCELTEMP DECREASES ALONG THE DALR
     */
    private float DATV(float T1, float P, float DP) {
        float CP = 1004.64f;
        float RD = 287.04f;
        float DATV = T1 - RD * T1 / (CP * P) * DP;
        return DATV;
    }

    /*******************************************************************
     * USE SOUNDING DATA TO DETERMINE THE MOST LIKELY MODE OF CONVECTION
     * ACCORDING TO THE AMOUNT OF CAPE AND VERTICAL WIND SHEAR. POSSIBLE MODES
     * OF CONVECTION ARE CUMULUS, AIR-MASS THUNDERSTORM, MULTI-CELL THUNDERSTORM
     * AND SUPERCELL THUNDERSTORM
     *******************************************************************/
    private void WOLKSRT(List<NcSoundingLayer> hailCloudSndLys, float ebs,
            float sh1, float sh2, float sh3) {
        float RD = 287.04f;
        // CALC CAPE BY INTEGRATING THE POSITIVE AREA I.E., PARCEL WARMER
        // THAN ENVIRONMENTAL AIR. INCREMENTS OF 5 MB
        float DP = 500.0f;
        float PRES = WBASP;
        PSEUDO = WBASTMP;
        while (PRES < 100500) {
            PSEUDO = XNATV(PSEUDO, PRES, -500.0f);
            PRES = PRES + 500.0f;
        }

        // SET PRESSURE AND TEMP TO THOSE VALUES AT CLOUD BASE
        PRES = WBASP;
        float TNAT = WBASTMP;
        CAPE = 0.0f;

        while (PRES > 15000.0) {
            // T1 IS PARCEL TEMP, TO1 IS AMBIENT TEMP
            float T1 = TNAT;
            float TO1 = XINTERP(PRES / 100, 3);
            float DP1000 = PRES - 100000;
            float Q1 = DATV(T1, PRES, DP1000);
            float QO1 = DATV(TO1, PRES, DP1000);
            float T2 = XNATV(T1, PRES, DP);
            PRES = PRES - DP;
            float TO2 = XINTERP(PRES / 100, 3);
            DP1000 = PRES - 100000;
            float Q2 = DATV(T2, PRES, DP1000);
            float QO2 = DATV(TO2, PRES, DP1000);

            // CALC THE CAPE IF THE PARCEL'S TEMP IS WARMER THAN THAT OF THE
            // ENVIRONMENT
            // THE CAPE IS CALC BY INTEGRATING THE POSITIVE AREA BETWEEN
            // Q1,QO1,Q2,QO2
            float TEST = (0.5f * RD * (T1 + T2) * 0.5f
                    / (0.5f * (PRES + PRES + DP))
                    * ((Q2 - QO2) / QO2 + (Q1 - QO1) / QO1) * DP);
            if (Q2 >= QO2 && TEST >= 0.0) {
                CAPE = CAPE
                        + (0.5f * RD * (T1 + T2) * 0.5f
                                / (0.5f * (PRES + PRES + DP))
                                * ((Q2 - QO2) / QO2 + (Q1 - QO1) / QO1) * DP);

            }
            TNAT = T2;
        }
        // Modulate ESI thresholds
        float TYPE = CAPE * ebs;
        if (TYPE <= sh1) {
            ITIPWLK = 0;
            esicat = 1;
        }
        if (TYPE <= sh2 && TYPE > sh1) {
            ITIPWLK = 1;
            esicat = 2;
        }

        if (TYPE > sh2 && TYPE <= sh3) {
            ITIPWLK = 2;
            esicat = 3;
        }

        if (TYPE > sh3) {
            ITIPWLK = 3;
            esicat = 4;
        }

    }

    /*******************************************************************
     *** CALCULATE THE PRESSURE AND TEMP AT THE SATURATION POINT (SP) - SP IS
     * WHERE THE SURFACE AIR BECOMES SATURATED BY LIFTING IT ADIABATICALLY FROM
     * THE SURFACE
     *******************************************************************/
    private void SP(float TE, float TD, float P) {
        // set globals ESPT,ESPP
        float CP = 1004.64f, RD = 287.04f, RV = 461.48f, EPS = 0.622f;
        // CALCUATE THE MIXING RATIO USING THE CLAUSSIUS CLAPEYRON EQUATION
        // R(AT TE)= RS(AT TD)
        float DP = 500.0f;
        float AL = (-0.566f * (TD - 273.16f) + 597.3f) * 4186.0f;
        float E = 611 * (float) Math.exp((AL / RV)
                * (1.0f / 273.16f - 1.0f / TD));
        float R = EPS * E / P;
        ESPP = P;
        ESPT = TE;
        // CALC T ALONG THE DALR UNTIL RS=R I.E., T=TD, THE SATURATION POINT
        // OR LCL
        for (int i = 1; i <= 100; i++) {
            // CALCULATE THE SATURATION MIXING RATIO FOR TE
            AL = (-0.566f * (TE - 273.16f) + 597.3f) * 4186.0f;
            float ES = 611 * (float) Math.exp((AL / RV)
                    * (1.0f / 273.16f - 1.0f / TE));
            float RS = EPS * ES / ESPP;
            if (RS <= R) {
                break;
            }

            // CALCULATE THE TEMP AT THE NEXT LEVEL ALONG THE DALR
            ESPT = TE - RD * TE / (CP * ESPP) * DP;
            TE = ESPT;
            ESPP = ESPP - DP;
        }
    }

    /*******************************************************************
     *** CALCULATE THE UPDRAFT VELOCITY DUE TO BUOYANCY
     *******************************************************************/
    private float UPDRAFT(float DR, float TK, float RS, float VU) {
        float RD = 287.04f;
        float DP = 50.0f;
        float TC = TK;
        float P = DR / 100.0f;
        float TA = XINTERP(P, 3);
        // BEREKEN LUGDIGTHEID IN KD/M3
        float DIGTA = (P * 100.0f / (RD * (1.0f + 0.609f * RS / (1.0f + RS)) * (TA)));
        // CALC THE Z-INCREMENT USING THE HYDROSTATIC EQUATION
        float DELZ = -100.0f * (-DP) / (DIGTA * G);
        // CALC THE TOTAL WATER CONTENT AT LEVEL P
        float CLWATER = WBASRS - RS;
        // CALC VIRTUAL TEMP OF THE AMBIENT AIR.
        float TAKELV = TA;
        float VIRT = (1.0f + 0.608f * (RS / (1.0f + RS)));
        float TAVIR = VIRT * TAKELV;
        // CALC THE VIRTUAL TEMP. OF THE CLOUD/PARCEL

        float TCKELV = TC;
        float TCVIR = VIRT * TCKELV;
        // CALCULATE THE UPDRAFT VELOCITY
        float A = VU * VU + 2.0f * G * DELZ * ((TCVIR - TAVIR) / TAVIR);
        float B = -2.0f * G * CLWATER * DELZ;
        float VOLGVU = (float) Math.sqrt(Math.abs(A + B));
        if (A + B < 0.0) {
            VOLGVU = -1 * VOLGVU;
        }
        return VOLGVU;
    }

    /*******************************************************************
     *** INTERPOLATE CLOUD DATA FOR LEVEL P LOCATED BETWEEN 2 WET ADIABTIC LEVELS
     *******************************************************************/
    private float XINTWLK(float P1, float P2, float PTFI, float WAARDE1,
            float WAARDE2) {
        // SEARCH FOR THE TWO LEVELS EACH SIDE OF P
        if (PTFI < P1 && PTFI >= P2) {
            float PDIFF = P1 - P2;
            float VDIFF = P1 - PTFI;
            float VERH = VDIFF / PDIFF;
            VERH = ((float) Math.exp(VERH) - 1.0f) / 1.71828f;
            float ADIFF = WAARDE1 - WAARDE2;
            float XINTWLK = WAARDE1 - (ADIFF * VERH);
            return XINTWLK;
        }
        return 0;
    }

    /*******************************************************************
     *** CALC THE PARCEL'S TEMPERATURE AND MIXING RATIO. THE UPDRAFT VELOCITY IS
     * THEN DETERMINED BY CALCULATING THE DIFFERENC EETWEEN THE PARCEL TEMP AND
     * THAT OF THE AMBIENT AIR
     *******************************************************************/
    private void NATAD() {
        // need to set NTRAIN, WMAX, BETA
        float RV = 461.48f, EPS = 0.622f, DP = 5000.0f, BL = 2464759.0f;
        // UPDRAFT VELOCITY AT CLOUD BASE IN M/S
        // SET THE ENTRAINMENT PARAMETER ACCORDING TO THE TYPE OF CLOUD
        // HERE 0.1 EQUALS 10% ENTRAINMENT AND 0.075 7.5% ENTRAINMENT
        if (ITIPWLK == 0 || ITIPWLK == 1) {
            BETA = 0.1f;
        }
        if (ITIPWLK == 2) {
            BETA = 0.075f;
        }
        if (ITIPWLK == 3) {
            BETA = 0.050f;
        }
        // SET THE TYPE OF ENTRAINMENT: CLOUDTOP FOR CUMULONIMBUS (TYPE 1-4)
        // AND LATERAL FOR TYPE 0
        if (ITIPWLK == 1) {
            NTRAIN = 1;
        } else {
            NTRAIN = 2;
        }
        // SPECIFY THE INITIAL CLOUD PARAMETER VALUES AT CLOUD BASE
        float TK = WBASTMP;
        float VU = WBASVU;
        float RS = WBASRS;
        float CSPT = WBASTMP;
        float CSPP = WBASP;
        float VORIGP = WBASP;
        float VORIGTK = WBASTMP;
        float VORIGRS = WBASRS;
        float VORIGVU = WBASVU;
        float P = WBASP - DP;
        float T = WBASTMP;
        float WMAX = VU;
        // FIND THE CLOUD BASE
        for (Map.Entry<Integer, NcSoundingLayer> entry : wolkdtaSndLys
                .entrySet()) {
            NcSoundingLayer wolkdtaLy = entry.getValue();
            // use WindSpeed to save WOLKDTA[N,7]
            wolkdtaLy.setWindSpeed(WBASVU);
            NcSoundingLayer tfiLy = tfiSndLys.get(entry.getKey());
            if (WBASP > tfiLy.getPressure() * 100.0) {
                break;
            }
        }
        float PTE = 0, PTD = 0;
        // CALC THE SP AT CLOUD TOP - ASSUMED TO BE 300MB
        if (NTRAIN == 1) {
            float PTE3 = XINTERP(300.0f, 3);
            float PTD3 = XINTERP(300.0f, 4);
            float PTE4 = XINTERP(400.0f, 3);
            float PTD4 = XINTERP(400.0f, 4);
            PTE = PTE3;
            PTD = PTE - ((PTE3 - PTD3) + (PTE4 - PTD4)) * 0.5f;
            SP(PTE, PTD, 30000);
        }
        float TDEPRES = 0;
        NcSoundingLayer tfiLastLy = tfiSndLys.get(tfiSndLys.size() - 1);
        for (int i = 1; i <= 200; i++) {
            if (NTRAIN == 2) {
                // LATERAL ENTRAINMENT - INTERPOLATE THE ENVIRONMENTAL TEMP AND
                // DEW-POINT (TO LEVEL P) IN KELVIN
                PTE = XINTERP((P / 100), 3);
                // HERE WE ASSUME THAT THE MOISTURE REMAINS THE SAME ABOVE 300
                // HPA
                if (P > 30000.0) {
                    PTD = XINTERP((P / 100), 4);
                } else {
                    PTD = PTE - TDEPRES;
                }
                // CALC THE LEVEL OF THE SATURATION POINT (SP) IN MB
                SP(PTE, PTD, P);
            }
            // FOR CLOUDTOP ENTRAINMENT: NO ENTRAINMENT ABOVE 300 HPA, IE BETA=0
            if (NTRAIN == 1 && (P / 100) < 300.0) {
                BETA = 0.0f;
            }
            // CALC EDELQW AND EDELQL AT ESPP - THAT THE TEMP DIFFERENCE BETWEEN
            // ESPT AND XNATV AT ESPP (FROM CSPP), AND DATV AT ESPP
            // RESPECTIVELY
            float PRES = CSPP;
            float ESPTNAT = CSPT;
            float ESPTDRG = CSPT;
            while (PRES > (ESPP + 300.0)) {
                ESPTNAT = XNATV(ESPTNAT, PRES, 500.0f);
                ESPTDRG = DATV(ESPTDRG, PRES, 500.0f);
                PRES = PRES - 500.0f;
            }
            float EDELQW = Math.abs(ESPTNAT - ESPT);
            float EDELQL = Math.abs(ESPT - ESPTDRG);
            // CALC THE LEVEL OF THE SATURATION POINT (SP)P FOR THE MIXED PARCEL
            float AMSPP = CSPP - BETA * (CSPP - P);
            // CALC THE TEMP OF THE ENTRAINED/MIXED PARCEL
            PRES = CSPP;
            float AMTNAT = CSPT;
            float AMTDRG = CSPT;
            while (PRES > (AMSPP + 50.0)) {
                AMTNAT = XNATV(AMTNAT, PRES, 100.0f);
                AMTDRG = DATV(AMTDRG, PRES, 100.0f);
                PRES = PRES - 100.0f;
            }
            float AMND = Math.abs(AMTNAT - AMTDRG);
            // NOW CALC AMDELQW AND THEN AMSPT
            float AMDELQW = AMND * (1 - EDELQL / (EDELQL + EDELQW));
            float AMSPT = AMTNAT - AMDELQW;
            // CALC THE PARCEL'S TEMP AND DEW-POINT AT LEVEL P
            PRES = AMSPP;
            T = AMSPT;

            while (PRES > (P + 50.0)) {
                T = XNATV(T, PRES, 100.0f);
                PRES = PRES - 100.0f;
            }
            // SET THE NEW PARCEL'S SP
            CSPP = AMSPP;
            CSPT = AMSPT;
            // GET THE DEW-POINT DEPRESSION - WILL BE USED LATER IF DR<300MB
            TDEPRES = PTE - PTD;
            TK = T;
            // CALC THE FINAL VAPOUR PRESSURE AND MIXING RATIO OF PARCEL AFTER
            // ENTRAINMENT
            float E = 611.0f * (float) Math.exp((BL / RV)
                    * (1.0f / 273.16f - 1.0f / TK));
            RS = EPS * E / (P - E);
            // CALC THE UPDRAFT VELOCITY IN M/S
            VU = UPDRAFT(P, TK, RS, VU);
            // move CLWATER computation out of UPDRAFT, so UPDRAFT only return
            // VU
            float CLWATER = WBASRS - RS;
            // TEST IF DR LIES ON ONE OF THE TFI'S PRESSURE LEVELS
            Integer[] tfiKey = {};
            tfiKey = tfiSndLys.keySet().toArray(tfiKey);
            for (int k = 0; k < tfiKey.length - 1; k++) {
                NcSoundingLayer tfiLy = tfiSndLys.get(tfiKey[k]);
                if (tfiLy.getPressure() >= (P / 100.0)
                        && tfiLy.getPressure() < (VORIGP / 100.0)) {
                    try {
                        NcSoundingLayer wolkdtaLy = wolkdtaSndLys
                                .get(tfiKey[k] + 1);
                        if (wolkdtaLy == null) {
                            continue;
                        }
                        // save WOLKDTA[N,1]
                        wolkdtaLy.setPressure(tfiLy.getPressure());
                        // save WOLKDTA[N,2] use SpecHumidity
                        wolkdtaLy.setSpecHumidity(tfiLy.getSpecHumidity());
                        // save WOLKDTA[N,3]
                        wolkdtaLy
                                .setTemperature(tfiLy.getTemperature() - 273.16f);
                        // save WOLKDTA[N,4]
                        wolkdtaLy.setDewpoint(tfiLy.getDewpoint() - 273.16f);
                        // save WOLKDTA[N,5] use omega variable to save T1 here
                        float t1 = XINTWLK(VORIGP, P,
                                tfiLy.getPressure() * 100.0f,
                                (VORIGTK - 273.16f), (TK - 273.16f));
                        wolkdtaLy.setOmega(t1);
                        // save WOLKDTA[N,6] use DPD variable to save RS here
                        float rs = XINTWLK(VORIGP, P,
                                tfiLy.getPressure() * 100.0f,
                                (VORIGRS * 1000.0f), (RS * 1000.0f));
                        wolkdtaLy.setDpd(rs);
                        // use WindSpeed to save WOLKDTA[N,7]
                        float vu = XINTWLK(VORIGP, P,
                                tfiLy.getPressure() * 100.0f, VORIGVU, VU);
                        wolkdtaLy.setWindSpeed(vu);
                        // save WOLKDTA[N,8]
                        wolkdtaLy.setGeoHeight(tfiLy.getGeoHeight());
                        // save WOLKDTA[N,9]
                        wolkdtaLy.setWindDirection(CLWATER * 1000.0f);
                    } catch (IndexOutOfBoundsException e) {
                        statusHandler.handle(Priority.PROBLEM,
                                e.getLocalizedMessage(), e);
                        break;
                    }
                }
            }
            // GO TO NEXT LEVEL
            VORIGP = P;
            VORIGTK = TK;
            VORIGRS = RS;
            VORIGVU = VU;
            P = P - DP;
            // FIND THE MAX UPDRAFT VELOCITY
            if (VU > WMAX) {
                WMAX = VU;
            }
            // TEST FOR THE END OF THE RUN - UPDRAFT LESS THAN 0 M/S OR
            // PRESSURE L.T. 100 HPA
            if (VU < -20.0
                    || (tfiLastLy != null && P <= tfiLastLy.getPressure() * 100.0f)
                    || P <= 10000.0) {
                break;
            }
        }
    }

    private void hailcloud(float Tinput, float Tdinput, float ebs,
            float elevated, int lplcount, float sh1, float sh2, float sh3) {
        /*****************************************************************
         *** SKYWATCH ONE-DIMENSIONAL CLOUD MODEL
         *** 
         *** THE MODEL IS BASED ON THE PARCEL METHOD, BUT ALLOWS FOR WATER LOADING
         * AND ENTRAINMENT. THE SURFACE TEMP AND DEWPOINT ARE USED TO LIFT THE
         * PARCEL TO ITS LCL
         * 
         *** CODE TRANSLATED INTO ENGLISH BY J.C. BRIMELOW, JUNE 2002
         **************************************************************** 
         ************************************************* 
         *** CLOUD MODEL PARAMETERS:
         ************************************************* 
         * 
         * 
         * To/TMAX = CONTROL TEMP Tdo/TDOU = CONTROL DEWPOINT OPPDRUK = SURFACE
         * PRESSURE
         * 
         *** TFI= ARRAY FOR TFI DATA sounding = ARRAY INTO WHICH INPUT SOUNDING
         * DATA IS READ WOLKDTA = ARRAY FOR CLOUD MODEL OUTPUT
         * 
         *** R = MIXING RATIO RS = SATURATION MIXING RATIO DIGTA = AIR DENSITY
         * CLWATER = CONDENSED CLOUD WATER TCVIR = VIRTUAL PARCEL TEMPERATURE VU
         * = UPDRAFT VELOCITY
         * 
         *** WOLK= CLOUD WOLKBAS = CLOUD BASE WBASP = CLOUD BASE PRESSURE WBASTMP=
         * CLOUD BASE TEMP WBASVU= UPDRAFT VELOCITY AT CLOUD BASE WBASRS =
         * SATURATION MIXING RATIO AT CLOUD BASE
         * 
         *** WMAX = MAX. UPDRAFT VELOCITY BETA = BETTS' ENTRAINMENT PARAMETER
         * PSEUDO = PSEUDOADIABAT (IN DEGREES K)
         * 
         *** ITIPWLK = CLOUD TYPE WHERE, 0 = CUMULUS OR NIL 1-3 = AIR-MASS
         * THUNDERSTORM 3-5 = MULTI-CELL 5+ = SUPERCELL
         *** 
         ***************************************************/
        // CONVERT TMAX (Tinput) AND COINCIDENT DEW-POINT (Tdinput) TO KELVIN
        float TMAX = Tinput + 273.16f;
        float TDOU = Tdinput + 273.16f;
        List<NcSoundingLayer> hailCloudSndLys = new ArrayList<>(hailSndLys);
        if (elevated == 1) {
            // If sounding is elevated, then shift array values down so that
            // muparcel level will now be the surface (1st level)
            hailCloudSndLys = hailCloudSndLys.subList(lplcount,
                    hailCloudSndLys.size());
        }
        for (NcSoundingLayer ly : hailCloudSndLys) {
            // if dewpoint value off sounding is less than -90, set to -90
            if (ly.getDewpoint() < -90) {
                ly.setDewpoint(-90);
            }
            // if temperature is less than -85, set to -85
            if (ly.getTemperature() < -85) {
                ly.setTemperature(-85);
            }

            // CONVERT TEMPERATURE AND DEWPOINT TO KELVIN
            ly.setDewpoint(ly.getDewpoint() + 273.16f);
            ly.setTemperature(ly.getTemperature() + 273.16f);
        }
        // was OPPDRUK in original code
        OPPDRUK = hailCloudSndLys.get(0).getPressure() * 100;
        // INTERPOLATE A HIGHER RESOLUTION SOUNDING
        INTSOUN(hailCloudSndLys);
        if (tfiSndLys.size() == 0) {
            return;
        }
        // compute CLOUD BASE PARAMETERS
        WOLKBAS(TMAX, TDOU);
        if (wolkdtaSndLys.size() == 0) {
            return;
        }

        // DETERMINE THE TYPE OF CLOUD USING CAPE AND VERTICAL WIND SHEAR
        // I.E. ESI = ebs*CAPE
        WOLKSRT(hailCloudSndLys, ebs, sh1, sh2, sh3);

        // CALC THE PARCEL'S TEMPERATURE, AFTER INCL. ENTRAINMENT FROM
        // CLOUD BASE TO 300 HPA
        NATAD();
    }

    private void LEESDTA() {
        UPMAXV = 0;
        for (Map.Entry<Integer, NcSoundingLayer> entry : wolkdtaSndLys
                .entrySet()) {
            NcSoundingLayer wolkLy = entry.getValue();
            // get HGTE
            float HGTE = wolkLy.getSpecHumidity();
            if (HGTE != 0) {
                WBASP = wolkLy.getPressure(); // 1st element in WOLK array
                ZBAS = wolkLy.getGeoHeight();// 8th element
            }
            // MAX UPDRAFT VELOCITY TO M/S
            wolkLy.setWindSpeed(wolkLy.getWindSpeed() * 100);
        }
        for (Map.Entry<Integer, NcSoundingLayer> entry : wolkdtaSndLys
                .entrySet()) {
            NcSoundingLayer wolkLy = entry.getValue();
            float vuu = wolkLy.getWindSpeed();
            if (UPMAXV < vuu) {
                UPMAXV = vuu;
            }
            if ((wolkLy.getOmega() < 0 && wolkLy.getOmega() < wolkLy
                    .getTemperature()) || vuu < 0) {
                break;
            }
        }
        UPMAXV = UPMAXV / 100;

    }

    /*****************************************************************
     * INTERPOLATE VALUES OF RS AT LEVEL P BETWEEN 2 LEVELS OF wolkdtaSndLys
     * JVAL : variable type for wolkdtaSndLys
     * 
     * return interpolated value, if failed return -9999
     ******************************************************************/
    private float INTERP(int JVAL, float P) {
        float RS = 0;
        // PP(100) in orignal code is never initialized, a BUG
        // use wolkdtaSndLys instead

        // SEARCH FOR THE 2 LEVELS EACH SIDE OF P
        boolean found = false;
        NcSoundingLayer wolkLy = null, wolkLy1 = null;
        Integer[] key = {};
        key = wolkdtaSndLys.keySet().toArray(key);
        for (int i = 0; i < key.length - 1; i++) {
            wolkLy = wolkdtaSndLys.get(key[i]);
            wolkLy1 = wolkdtaSndLys.get(key[i + 1]);
            if (P <= wolkLy.getPressure() && P > wolkLy1.getPressure()) {
                found = true;
                break;
            }
        }
        if (!found || wolkLy == null || wolkLy1 == null) {
            return -9999;

        }

        // CALC RATIO BETWEEN VDIFF AND PDIFF
        float PDIFF = wolkLy.getPressure() - wolkLy1.getPressure();
        float VDIFF = wolkLy.getPressure() - P;
        float VERH = VDIFF / PDIFF;
        // CALCULATE THE DIFFERENCE BETWEEN 2 R VALUES
        float R = 0, R1 = 0;
        float RDIFF;
        if (JVAL == 5) {// omega, TCA
            R = wolkLy.getOmega();
            R1 = wolkLy1.getOmega();
        } else if (JVAL == 6) {// dpd, R
            R = wolkLy.getDpd();
            R1 = wolkLy1.getDpd();
        } else if (JVAL == 7) {// windSpeed, VUU
            R = wolkLy.getWindSpeed();
            R1 = wolkLy1.getWindSpeed();
        }
        // CALCULATE NEW VALUE
        RDIFF = Math.abs(R1 - R);
        if (RDIFF < 0) {
            RS = R - (RDIFF * VERH);
        } else {
            RS = R + (RDIFF * VERH);
        }
        return RS;
    }

    /********************************************************************
     *** CALCULATE THE TERMINAL VELOCTY OF THE HAILSTONE (SI-UNITS)
     ********************************************************************/
    private float TERMINL(float DENSA, float DENSE, float D, float TC) {
        float VT, W, Y, RE;
        float DENSASI = DENSA * 1000;
        float DENSESI = DENSE * 1000;
        float DD = D / 100;
        // MASS OF STONE IN GRAMS
        float GMASS = (DENSESI * PI * (float) Math.pow(DD, 3)) / 6;
        float TCK = TC + 273.16f;
        // CALC DYNAMIC VISCOSITY
        float ANU = (0.00001718f) * (273.16f + 120) / (TCK + 120)
                * (float) Math.pow((TCK / 273.16f), (3 / 2));
        // CALC THE BEST NUMBER, X AND REYNOLDS NUMBER, RE
        float GX = (8 * GMASS * G * DENSASI)
                / (PI * (float) Math.pow(ANU, 2.0));
        RE = (float) Math.pow((GX / 0.6), 0.5);
        // SELECT APPROPRIATE EQUATIONS FOR TERMINAL VELOCITY DEPENDING ON
        // THE BEST NUMBER
        if (GX < 550) {
            W = (float) Math.log10(GX);
            Y = -1.7095f + 1.33438f * W - 0.11591f * (W * W);
            RE = (float) Math.pow(10, Y);
        } else if (GX >= 550 && GX < 1800) {
            W = (float) Math.log10(GX);
            Y = -1.81391f + 1.34671f * W - 0.12427f * (W * W) + 0.0063f
                    * (W * W * W);
            RE = (float) Math.pow(10, Y);
        } else if (GX > 1800 && GX < 3.45E08) {
            RE = 0.4487f * (float) Math.pow(GX, 0.5536);
        } else {
            RE = (float) Math.pow((GX / 0.6), 0.5);
        }
        VT = ANU * RE / (DD * DENSASI) * 100;
        return VT;
    }

    /**************************************************************
     * CALC THE DIFFERENCE IN SATURATION VAPOUR DENSITY (SI UNITS) BETWEEN THAT
     * OVER THE HAILSTONE'S SURFACE AND THE IN-CLOUD AIR, DEPENDS ON THE
     * WATER/ICE RATIO OF THE UPDRAFT, AND IF THE STONE IS IN WET OR DRY GROWTH
     * REGIME
     **************************************************************/
    private float DAMPDIG(float PC, float TS, float TC) {
        float DELRW, ESAT;
        float RV = 461.48f, ALV = 2500000.f, ALS = 2836050.f;
        // FOR HAILSTONE: FIRST TEST IF STONE IS IN WET OR DRY GROWTH
        float TSK = TS + 273.16f;
        float TCK = TC + 273.16f;
        if (TSK >= (273.16)) {
            // IF WET GROWTH
            ESAT = 611 * (float) Math.exp(ALV / RV * (1. / 273.16f - 1. / TSK));
        } else {
            // IF DRY GROWTH
            ESAT = 611 * (float) Math.exp(ALS / RV * (1. / 273.16f - 1. / TSK));
        }
        float RHOKOR = ESAT / (RV * TSK);
        // NOW FOR THE AMBIENT/IN-CLOUD CONDITIONS
        float ESATW = 611 * (float) Math.exp(ALV / RV
                * (1. / 273.16 - 1. / TCK));
        float RHOOMGW = ESATW / (RV * TCK);
        float ESATI = 611 * (float) Math.exp(ALS / RV
                * (1. / 273.16 - 1. / TCK));
        float RHOOMGI = ESATI / (RV * TCK);
        float RHOOMG = PC * (RHOOMGI - RHOOMGW) + RHOOMGW;
        // CALC THE DIFFERENCE(G/CM3): <0 FOR CONDENSATION, >0 FOR EVAPORATION
        DELRW = (RHOKOR - RHOOMG) / 1000;
        return DELRW;
    }

    /******************************************************************
     *** CALC THE STONE'S INCREASE IN MASS
     ******************************************************************/
    private void MASSAGR(float TC, float P, float DENSE, float VT, float XW,
            float XI, float SEKDEL) {
        // CALCULATE THE DIFFUSIVITY DI
        float D0 = 0.226f;
        DI = D0 * (float) Math.pow(((TC + 273.16f) / 273.16f), 1.81f)
                * (1000 / P);

        // COLLECTION EFFICIENCY FOR WATER AND ICE
        float EW = 1.0f;
        // IF TS WARMER THAN -5 DEGC THEN ACCRETE ALL THE ICE (EI=1.0)
        // OTHERWISE EI=0.21
        float EI;
        if (TS >= -5.0) {
            EI = 1.00f;
        } else {
            EI = 0.21f;
        }
        // CALC HAILSTONE'S MASS (GM), MASS OF WATER (GMW) AND THE MASS OF
        // ICE IN THE STONE (GMI)
        GM = PI / 6 * (float) Math.pow(D, 3.) * DENSE;
        GMW = FW * GM;
        float GMI = GM - GMW;
        // STORE THE MASS
        GM1 = GM;
        /********************* 4. STONE'S MASS GROWTH *******************/
        // CALCULATE THE NEW DIAMETER
        D = D + SEKDEL * 0.5f * VT / DENSE * (XW * EW + XI * EI);
        // CALCULATE THE INCREASE IN MASS DUE TO INTERCEPTED CLOUD WATER
        float GMW2 = GMW + SEKDEL * (PI / 4.f * D * D * VT * XW * EW);
        DGMW = GMW2 - GMW;
        GMW = GMW2;
        // CALCULATE THE INCREASE IN MASS DUE INTERCEPTED CLOUD ICE
        float GMI2 = GMI + SEKDEL * (PI / 4.f * D * D * VT * XI * EI);
        DGMI = GMI2 - GMI;
        GMI = GMI2;

        // CALCULATE THE TOTAL MASS CHANGE
        DGM = DGMW + DGMI;
    }

    /******************************************************************
     *** CALCULATE HAILSTONE'S HEAT BUDGET
     ******************************************************************/
    private void HEATBUD(float TC, float DENSA, float VT, float DELRW,
            float SEKDEL, float ITIPE, float P) {
        float ALF = 79.7f, ALV = 597.3f, ALS = 677.0f, CI = 0.5f, CW = 1.f;
        // CALCULATE THE CONSTANTS
        float AK = (5.8f + 0.0184f * TC) * (float) Math.pow(10., (-5.));
        float TK = TC + 273.15f;
        float ANU = 1.717f * (0.0001f) * (393.0f / (TK + 120.0f))
                * (float) Math.pow(TK / 273.15f, 1.5);
        // CALCULATE THE REYNOLDS NUMBER
        float RE = D * VT * DENSA / ANU;
        float H = (float) Math.pow(0.71, (1.0 / 3.0))
                * (float) Math.pow(RE, 0.50);
        float E = (float) Math.pow(0.60, (1.0 / 3.0))
                * (float) Math.pow(RE, 0.50);
        // SELECT APPROPRIATE VALUES OF AH AND AE ACCORDING TO RE
        float AH, AE;
        if (RE < 6000.0) {
            AH = 0.78f + 0.308f * H;
            AE = 0.78f + 0.308f * E;
        } else if (RE >= 6000.0 && RE < 20000.0) {
            AH = 0.76f * H;
            AE = 0.76f * E;
        } else {
            AH = (0.57f + 0.000009f * RE) * H;
            AE = (0.57f + 0.000009f * RE) * E;
        }
        // FOR DRY GROWTH FW=0, CALCULATE NEW TS, ITIPE=1
        // FOR WET GROWTH TS=0, CALCULATE NEW FW, ITIPE=2
        if (ITIPE == 1) {
            // DRY GROWTH; CALC NEW TEMP OF THE STONE
            TS = TS
                    - TS
                    * DGM
                    / GM1
                    + SEKDEL
                    / (GM1 * CI)
                    * (2.f * PI * D
                            * (AH * AK * (TC - TS) - AE * ALS * DI * DELRW)
                            + DGMW / SEKDEL * (ALF + CW * TC) + DGMI / SEKDEL
                            * CI * TC);
        } else if (ITIPE == 2) {
            // WET GROWTH; CALC NEW FW
            FW = FW
                    - FW
                    * DGM
                    / GM1
                    + SEKDEL
                    / (GM1 * ALF)
                    * (2 * PI * D * (AH * AK * TC - AE * ALV * DI * DELRW)
                            + DGMW / SEKDEL * (ALF + CW * TC) + DGMI / SEKDEL
                            * CI * TC);
        }
        if (FW > 1) {
            FW = 1;
        }
        if (FW < 0) {
            FW = 0;
        }
    }

    /**************************************************************
     * TEST IF AMOUNT OF WATER ON SURFACE EXCEEDS CRTICAL LIMIT- IF SO INVOKE
     * SHEDDING SCHEME
     **************************************************************/
    private void BREAKUP() {
        // ONLY TEST FOR EXCESS WATER IF STONE'S D IS GT 9 MM
        if (D <= 0.9) {
            return;
        }
        float WATER = FW * GM;
        float GMI = GM - WATER;
        // CALC CRTICAL MASS CAPABLE OF BEING "SUPPORTED" ON THE STONE'S
        // SURFACE
        float CRIT = 0.268f + 0.1389f * GMI;
        if (WATER > CRIT) {
            float WAT = WATER - CRIT;
            GM = GM - WAT;
            FW = (CRIT) / GM;
        } else {
            return;
        }
        if (FW > 1.0) {
            FW = 1;
        }
        if (FW < 0) {
            FW = 0;
        }
        // RECALCULATE DENSITY AND DIAMETER AFTER SHEDDING
        DENSE = (FW * (1 - 0.9f) + 0.9f);
        D = (float) Math.pow((6 * GM / (PI * DENSE)), (1 / 3));
    }

    /*****************************************************************
     * Compute the nondim parameter tao (in WAF, Dec 1996, pg 591) to determine
     * whether a falling ice crystal melts. Use in a precipitation type
     * algorithm. a: diameter in cm
     * *****************************************************************/
    private float hailstonelpl(float depth, float tenv, float a) {
        a = 0.5f * a / 100;// convert to radius and cm -> meters
        float r = a;// r is the hail radius in meters (before melting, r = a)
        float ka = .02f;// thermal conductivity of air
        float lf = 3.34f * 0.00001f;// latent heat of melting/fusion
        float lv = 2.5f * 0.000001f;// latent heat of vaporization
        float t0 = 273.155f;// temp of ice/water melting interface
        float dv = 0.25f * 0.0001f; // diffusivity of water vapor (m2/s)
        float rv = 1004 - 287;// gas constant for water vapor
        float rhoice = 917.0f;// density of ice (kg/m**3)
        // compute terminal speed based on Houze Cloud Dyn. (pg 91) simple
        // method.
        float tervel = 100 * 9 * (float) Math.pow((a * 2 * 100), 0.8);// cm/s
        // computer residence time in the warm layer...
        float tres = (depth * 100) / tervel; // residence time in warm layer
        // Calc dmdt based on eqn (3) of Goyer et al. (1969)
        // Reynolds number...from pg 317 of Atmo Physics (Salby 1996)
        // Just use the density of air at 850 mb...close enough.
        float rho = 85000 / (287 * tenv);
        float re = rho * r * tervel * .01f / (1.7f * 0.00001f);
        float delt = tenv - t0;// temp difference between env and hailstone
                               // sfc.
        // calculate the differencein vapor density of air stream and equil
        // vapor
        // density at the sfc of the hailstone
        float esenv = 6.108f * ((float) Math.exp((17.27f * (tenv - 273.155f))
                / (237.3f + (tenv - 273.155f))));// es environment in mb
        esenv = esenv * 100;// mb to pa
        float rhosenv = esenv / (rv * tenv);
        float essfc = 6.108f * ((float) Math.exp((17.27f * (t0 - 273.155f))
                / (237.3f + (t0 - 273.155f))));// es environment in mb
        essfc = essfc * 100;// mb to pa
        float rhosfc = essfc / (rv * t0);
        float dsig = rhosenv - rhosfc;

        float dmdt = (-1.7f * PI * r * (float) Math.pow(re, 0.5) / lf)
                * ((ka * delt) + ((lv - lf) * dv * dsig));
        if (dmdt > 0.0) {
            dmdt = 0;
        }
        float mass = dmdt * tres;
        // now find the new diameter of the hailstone...
        float massorg = (4 / 3) * PI * r * r * r * rhoice;
        float newmass = massorg + mass;
        if (newmass < 0.0) {
            newmass = 0;
        }
        a = (float) Math.pow((0.75f * newmass / (PI * rhoice)), 0.333333333);
        // CONVERT a TO CENTIMETERS & from radius to diameter
        a = a * 2 * 100;
        return a;
    }

    /*****************************************************************
     * This is a spherical hail melting estimate based on the Goyer et al.
     * (1969) eqn (3). The depth of the warm layer, estimated terminal velocity,
     * and mean temperature of the warm layer are used. DRB. 11/17/2003.
     * *****************************************************************/
    private void melt(List<NcSoundingLayer> subcloudSndLys) {
        float tsum = 0;
        float tdsum = 0;
        float psum = 0;
        int lplcount = subcloudSndLys.size();
        // depth
        float layer = subcloudSndLys.get(lplcount - 1).getGeoHeight()
                - subcloudSndLys.get(0).getGeoHeight();
        for (int i = 0; i < lplcount; i++) {
            tsum = subcloudSndLys.get(i).getTemperature() + tsum;
            tdsum = subcloudSndLys.get(i).getDewpoint() + tdsum;
            psum = subcloudSndLys.get(i).getPressure() + psum;
        }
        float tlayer = tsum / lplcount;
        float dlayer = tdsum / lplcount;
        float player = psum / lplcount;
        // Convert the mean layer temp and mean layer dewpt to wet bulb temp...
        // Now...calculate the wet bulb temperature.
        float eenv = 0.6108f * ((float) Math.exp((17.27f * dlayer)
                / (237.3f + dlayer)));
        eenv = eenv * 10;// convert to mb
        float gamma = 6.6f * 0.0001f * player;
        float delta = (4098 * eenv) / (float) Math.pow((dlayer + 237.7f), 22);
        float wetbulb = ((gamma * tlayer) + (delta * dlayer)) / (gamma + delta);
        // Now iterate to precisely determine wet bulb temp.
        int wcnt = 0;
        // calc vapor pressure at wet bulb temp
        float wetold;
        do {
            float ewet = 0.6108f * ((float) Math.exp((17.27f * wetbulb)
                    / (237.3f + wetbulb)));
            ewet = ewet * 10; // convert to mb
            float de = (0.0006355f * player * (tlayer - wetbulb))
                    - (ewet - eenv);
            float der = (ewet * (.0091379024f - (6106.396f / (float) Math.pow(
                    (273.155f + wetbulb), 2)))) - (0.0006355f * player);
            wetold = wetbulb;
            wetbulb = wetbulb - de / der;
            wcnt = wcnt + 1;

        } while ((Math.abs(wetbulb - wetold) / wetbulb) > .0001 && (wcnt < 11));
        float lt = wetbulb + 273.155f;
        // D in cm
        D = hailstonelpl(layer, lt, D);

    }

    /*****************************************************************
     * HAILCAST: ONE-DIMENSIONAL HAIL MODEL THE PROGRAM CALCULATES THE MAXIMUM
     * EXPECTED HAIL DIAMETER AT THE SURFACE USING DATA FROM THE 1D CLCompute
     * the nondim parameter tao (in WAF, Dec 1996, pg 591) to determine CJCS
     * whether a falling ice crystal melts. Use in a precipitation type CJCS
     * algorithm.OUD MODEL
     *****************************************************************/
    private void hailstone(float ebs, float elevated, float lplcount,
            float embryo, List<NcSoundingLayer> subcloudSndLys) {
        /******************************************************************/
        /* LIST OF VARIABLES */
        /****************************************************************/
        /*** A = VENTILATION COEFFICIENT */
        /*** AK = THERMAL CONDUCTIVITY */
        /*** ANU = DYNAMIC VISCOSITY */
        /*** ALF = LATENT HEAT OF FUSION */
        /*** ALS = LATENT HEAT OF SUBLIMATION */
        /*** ALV = LATENT HEAT OF EVAPORATION */
        /*** CD = DRAG COEFFICIENT OF HAILSTONE */
        /*** CI = SPECIFIC HEAT CAPACITY OF ICE */
        /*** CW = "             " OF WATER */
        /*** CLADWAT = ADIABATIC CLOUD WATER CONTENT */
        /*** CLWATER = TOTAL "        " */
        /*** D = DIAMETER OF HAILSTONE */
        /*** DELRW = DIFFERENCE IN VAPOUR DENSITY BETWEEN HAIL SURFACE AND */
        /*** UPDRAFT AIR */
        /*** DENSA = DENSITY OF THE IN-CLOUD AIR */
        /*** DENSE = " OF THE HAILSTONE */
        /*** DI = DIFFUSITIVITY */
        /*** DGM = TOTAL INCREASE IN MASS OF THE HAILSTONE */
        /*** DGMI = MASS INCREASE DUE TO ACCRETION OF ICE PARTICLES */
        /*** DGMW = " DUE TO ACCRETION OF WATER DROPLETS */
        /*** EI = COLLECTION EFFICIENCY FOR ICE */
        /*** EW = "          " " WATER */
        /*** FW = FRACTION OF WATER IN HAILSTONE */
        /*** G = ACCELERATION DUE TO GRAVITY */
        /*** GM = TOTAL MASS OF THE HAILSTONE */
        /*** GMW = MASS OF WATER IN THE HAILSTONE */
        /*** GMI = MASS OF ICE IN THE HAILSTONE */
        /*** ISEK = TIME COUNTER (NUMBER OF SECONDS) */
        /*** ISEKDEL = TIME STEP(IN SECONDS) */
        /*** P = PRESSURE */
        /*** PA = ENVIRONMENTAL PRESSURE */
        /*** PC = PERCENT CLOUD WATER */
        /*** R = MIXING RATIO OF THE AIR */
        /*** RE = REYNOLDS NUMBER */
        /*** RS = SATURATED MIXING RATIO OF THE AIR */
        /*** REENWAT = RAINWATER CONTENT OF THE CLOUD */
        /*** TAU = MAXIMUM CLOUD LIFETIME */
        /*** T = INTERPOLATED ENVIRONMENTAL TEMP */
        /*** TA = ENVIRONMENTAL TEMP OF SOUNDING/TFI (MATRIX) */
        /*** TD = ENVIRONMENTAL DEWPOINT OF SOUNDING/TFI */
        /*** TC = CLOUD TEMP AT LEVEL P */
        /*** TCA = CLOUD TEMP MATRIX */
        /*** TS = HAILSTONE'S SURFACE TEMPERATURE */
        /*** V = ACTUAL VELOCITY OF THE HAILSTONE */
        /*** VT = TERMINAL VELOCITY OF THE HAILSTONE */
        /*** VUU = UPDRAFT MATRIX */
        /*** VU = UPDRAFT VELOCITY */
        /*** WBAST = CLOUD BASE TEMP. */
        /*** WBASTD = " DEW-POINT */
        /*** WBASRS = " SATURATED MIXING RATIO */
        /*** WBASTW = " WETBULB POTENTIAL TEMPERATURE */
        /*** WBASP = " PRESSURE */
        /*** WTOPP = CLOUD TOP PRESSURE */
        /*** WTOPT = " TEMP */
        /*** WWATER = CLOUD WATER CONTENT */
        /*** WYS = CLOUD ICE " */
        /*** XI = CONCENTRATION OF ICE ENCOUNTERED BY EMBRYO/STONE */
        /*** XW = CONCENTRATION OF WATER ENCOUNTERED BY EMBRYO/STONE */
        /*** Z = HEIGHT OF EMBRYO/STONE ABOVE THE SURFACE */
        float RD = 287.04f;
        // *** TIME STEP IN SECONDS
        float SEKDEL = 0.2f;
        /********************
         * 1. INPUT DATA: READ OUTPUT DATA FROM THE CLOUD MODEL, FROM c*.dat
         *****************************************************************/
        LEESDTA();
        // BEGIN TIME (SECONDS)
        float BEGTYD1 = 60;
        // INITIAL HAIL EMBRYO DIAMETER IN MICRONS, AT CLOUD BASE
        float DD1 = embryo;
        // UPPER LIMIT OF SIMULATION IN SECONDS
        float TAU = 4200;
        // STATION HEIGHT
        Integer[] key = {};
        key = wolkdtaSndLys.keySet().toArray(key);
        if (key.length == 0) {
            int k = 0;
        }
        float STHGTE = wolkdtaSndLys.get(key[0]).getGeoHeight();
        // original code TCA(1)=TAA(1)
        wolkdtaSndLys.get(key[0]).setOmega(
                wolkdtaSndLys.get(key[0]).getTemperature());
        // DETERMINE VALUES OF PARAMETERS AT CLOUD BASE
        float PBEGIN = 0, RSBEGIN = 0, P0 = 0, RS0 = 0, TCBEGIN = 0, V = 0;
        for (Map.Entry<Integer, NcSoundingLayer> entry : wolkdtaSndLys
                .entrySet()) {
            NcSoundingLayer wolkLy = entry.getValue();
            if (wolkLy.getPressure() == WBASP) {
                PBEGIN = wolkLy.getPressure();
                RSBEGIN = wolkLy.getDpd();
                P0 = wolkLy.getPressure();
                RS0 = wolkLy.getDpd();
                TCBEGIN = wolkLy.getOmega();
                ZBAS = wolkLy.getGeoHeight();
                V = wolkLy.getWindSpeed();
                break;
            }
        }
        // INITIAL HEIGHT OF EMBRYO ABOVE STATION LEVEL
        float ZBEGIN = ZBAS - STHGTE;
        // SET TEST FOR EMBRYO: 0 FOR NOT FROZEN FOR FIRST TIME, IF 1
        // DON'T FREEZE AGAIN
        nofroze = 0;
        // INITIAL VALUES FOR VARIOUS PARAMETERS AT CLOUD BASE
        float SEK = BEGTYD1;
        float VU = V;
        float Z = ZBEGIN;
        float TC = TCBEGIN;
        WBASP = P0;
        float P = PBEGIN;
        WBASRS = RS0;
        float RS = RSBEGIN;
        float RSS = RS / 1000;
        // CALC. DENSITY OF THE UPDRAFT AIR (G/CM3)
        float DENSA = (P * 100 / (RD * (1 + 0.609f * RSS / (1 + RSS)) * (TC + 273.16f))) / 1000;
        // HAILSTONE PARAMETERS
        // INITIALISE SFC.TEMP, DIAMETER(M),FRACTION OF WATER AND DENSITY
        TS = TC;
        D = DD1 / 10000;
        float PC = 0;
        FW = 1;
        DENSE = 1;
        // TAU:: MAX CLOUD LIFETIME
        for (SEK = SEK + SEKDEL; SEK < TAU; SEK = SEK + SEKDEL) {
            /***********************************************************
             * 2. CALCULATE PARAMETERS CALCULATE UPDRAFT PROPERTIES
             ********************************************************/
            // original INTERP(VUU,VU,P,IFOUT), VUU is parameter 7
            VU = INTERP(7, P);
            // CALCULATE DURATION OF THE UPDRAFT ACCORDING TO THE PRODUCT OF
            // CAPE*SHEAR
            float TIME = 0;
            TIME = CAPE * (ebs);
            if (TIME < 1.0) {
                TIME = 1.0f;
            }
            float DUR;

            if (TIME < 5.0) {

                DUR = (-2.5f * TIME * TIME + 25 * TIME - 2.5f) * 60;
            } else {
                DUR = 3600;
            }
            int ITIME = (int) DUR;
            if (ITIME < 600) {
                ITIME = 600;
            }

            if (SEK > ITIME) {
                VU = 0;
            }
            if (VU == -9999) {
                break;
            }
            // CALCULATE TERMINAL VELOCITY OF THE STONE (USE PREVIOUS VALUES)
            float VT = TERMINL(DENSA, DENSE, D, TC);
            // ACTUAL VELCITY OF THE STONES (UPWARDS IS POSITIVE)
            V = VU - VT;
            // USE HYDROSTATIC EQTN TO CALC HEIGHT OF NEXT LEVEL
            P = (P * 100 - (DENSA * 1000 * G * V / 100) * SEKDEL) / 100;
            Z = Z + (V / 100) * SEKDEL;
            // CALCULATE NEW COULD TEMP AT NEW PRESSURE
            // LEVEL,INTERP(TCA,TC,P,IFOUT)
            TC = INTERP(5, P);
            if (TC == -9999) {
                break;
            }
            // CALCULATE PERCENTAGE OF FROZEN WATER USING SCHEME OF VALI AND
            // STANSBURY
            PC = 0.008f * (float) Math.pow(1.274f, (-20 - TC));
            if (TC > -20) {
                PC = 0;
            }
            if (PC > 1) {
                PC = 1;
            } else if (PC < 0) {
                PC = 0;
            }
            // CALC. MIXING RATIO AT NEW P-LEVEL AND THEN NEW DENSITY OF
            // IN-CLOUD AIR
            // original code CALL INTERP(R,RS,P,IFOUT)
            RS = INTERP(6, P);
            if (RS == -9999) {
                break;
            }
            RSS = RS / 1000;
            DENSA = (P * 100 / (RD * (1 + 0.609f * RSS / (1 + RSS)) * (TC + 273.16f))) / 1000;
            // CALC. THE TOTAL WATER CONTENT IN THE CLOUD AT LEVEL P, ALSO CALC
            // ADIABATIC VALUE
            float CLWATER = WBASRS / 1000 - RSS;
            float CLADWAT = DENSA * CLWATER;
            // CLOUD ICE AND LIQUID WATER
            float WWATER = CLADWAT;
            float WYS = PC * CLADWAT;
            float XW = WWATER - WYS;
            float XI = WYS;
            /************************************************************
             * 3. TEST FOR WET OR DRY GROWTH. WET GROWTH - STONE'S SFC
             * TEMP.GE.0, DRY - STONE'S SFC TEMP.LT.0
             * ********************************************************/
            int ITIPE;
            // define temperature at which to freeze embryo
            if (TS >= -9 && TC >= -9 && nofroze == 0) {
                // FREEZE THE HAIL EMBRYO AT -8 DEGC, define emb
                if (TC <= -8) {
                    // DRY GROWTH
                    FW = 0;
                    TS = TC;
                    ITIPE = 1;
                    nofroze = 1;
                } else {
                    // WET GROWTH
                    FW = 1;
                    TS = TC;
                    ITIPE = 2;
                    nofroze = 0;
                }
            } else {
                if (TS < 0) {
                    // DRY GROWTH
                    FW = 0;
                    ITIPE = 1;
                } else {
                    // WET GROWTH
                    TS = 0;
                    ITIPE = 2;
                }
            }
            // DENSITY OF HAILSTONE - DEPENDS ON FW - ONLY WATER=1
            // GM/L=1000KG/M3 ONLY ICE =0.9 GM/L
            DENSE = (FW * (0.1f) + 0.9f);
            // VAPOUR DENSITY DIFFERENCE BETWTEEN STONE AND ENVIRONMENT
            float DELRW = DAMPDIG(PC, TS, TC);
            /********************* 4. STONE'S MASS GROWTH *******************/
            MASSAGR(TC, P, DENSE, VT, XW, XI, SEKDEL);

            /*************** 5. CALC HEAT BUDGET OF HAILSTONE **************/
            HEATBUD(TC, DENSA, VT, DELRW, SEKDEL, ITIPE, P);

            if (D > Daloft) {
                Daloft = D;
            }
            // fixes case where particle gets hung up near cloud base
            if ((SEK > 500) && (V < 50) && (V > -50) && (VU <= 400)
                    && (nofroze == 0)) {
                UPMAXV = 4;
                // original code ic = ic - 1; not useful, just comment it out
                D = 0;
            }
            /*********************************************************************
             * 6. TEST DIAMETER OF STONE AND HEIGHT ABOVE GROUND. TEST IF
             * DIAMETER OF STONE IS GREATER THAN LIMIT, IF SO BREAK UP
             *************************************************************/
            BREAKUP();
            // TEST IF STONE IS BELOW CLOUD BASE--
            // THEN NO CLOUD DROPLETS OR CLOUD ICE
            if (P >= WBASP) {
                XW = 0;
                XI = 0;
            }
            // TEST IF STONE HAS REACHED THE SURFACE
            if (Z <= 0) {
                break;
            }
        }// end of for (SEK = SEK + SEKDEL; SEK < TAU; SEK = SEK + SEKDEL)
         // if this is an elevated sounding, then use melting routine to melt
         // the
         // stone from lpl to original surface.
        if (elevated == 1) {
            melt(subcloudSndLys);
        }
        // WRITE HAIL SIZES OUT
        // IF FW=1.0 THEN HAIL HAS MELTED AND SET D TO 0.0
        if (Math.abs(FW - 1) < 0.001) {
            D = 0;
        }
    }

    /**
     * hailCast1() - convert from nhail1.f
     * 
     * @param ebs
     * @param mumixr
     * @param sndLys
     * @return List<Float> hvars, index stating @ 1
     */
    private Map<Integer, Float> hailCast1(float ebs, float mumixr,
            List<NcSoundingLayer> sndLys) {
        Map<Integer, Float> hvars = new HashMap<>();
        NcSoundingLayer sfcLayer = NsharpLibBasics.sfc(sndLys);
        float To = sfcLayer.getTemperature();
        float Tdo = sfcLayer.getDewpoint();
        float sfcPress = sfcLayer.getPressure();
        float Toff = 1.0f;
        float dT = 0.5f;
        hailSndLys = constructHailCastSndList(sndLys);
        // If sounding does not extend to 300 mb,
        int level = hailSndLys.size();
        if (hailSndLys.get(level - 1).getPressure() >= 300) {
            return null;
        }
        /*********************************
         * TOP BUN * ELevated Sounding Sandwich
         ***************************************/
        float thetaemax = -99;
        float plplmax = 0;
        float tlpl = 0;
        float dlpl = 0;
        int lplcount = 0;
        int count = 0;
        for (NcSoundingLayer ly : hailSndLys) {
            float press = ly.getPressure();
            float temp = ly.getTemperature();
            float dew = ly.getDewpoint();
            if (press >= 500) {
                // Calculate LCL Temperature (C) Using Equation from Barnes 1968
                float tlcl = dew - (0.001296f * dew + 0.1963f) * (temp - dew);
                // Convert temp at LCL to kelvin
                tlcl = tlcl + 273.15f;

                float es = 6.1f * (float) Math.exp(0.073f * temp);
                float rh = (float) Math.exp(0.073f * (dew - temp));
                float e = rh * es;
                float q = (e * 287.04f) / (press * 461.55f);
                float eqtemp = (temp + 273.15f)
                        * (1 + ((float) 2.5e6 * q) / (1004 * tlcl));
                float thetae = eqtemp
                        * (float) Math.pow((1000 / press), (0.2859));

                // If maximum thetae, define level
                if (thetae > thetaemax) {
                    thetaemax = thetae;
                    plplmax = press;
                    tlpl = temp;
                    dlpl = dew;
                    lplcount = count;
                }
            }
            count++;
        }
        float elevated = 0;
        // Automatically put parcel Temp and Dew depending upon LPL
        List<NcSoundingLayer> subcloudSndLys;
        if (plplmax < sfcPress) {
            To = tlpl;
            Tdo = dlpl;
            elevated = 1;
            // Elevated Parcel lifted from plplmax mb
            // Now construct subcloud layer list for melting algorithm
            subcloudSndLys = hailSndLys.subList(0, lplcount + 1);

        } else {
            subcloudSndLys = hailSndLys;
        }
        // Adjust parcel T and Td if they are too close to each other
        if (Tdo + 1 > To - 1) {
            To = To + ((Tdo + 1) - (To - 1)) + 0.01f;
        }
        if (Tdo + 1 == To - 1) {
            To = To + 0.01f;
        }
        hvars.put(1, To);
        hvars.put(2, Tdo);
        /**********************************
         * Bottom Bun
         **********************************/
        float sh1, sh2, sh3, embryo, which = 1, bias = 0;
        // choose hail model parameters based on mumixr bin
        if (mumixr < 11) {
            // If which = 0, use hail model average, if which = 1 (default)
            // use max
            sh1 = 5;
            sh2 = 5;
            sh3 = 9;
            embryo = 1500;
            WBASVU = 8;
        } else if (mumixr >= 11 && mumixr < 14) {
            which = 0;
            sh1 = 3;
            sh2 = 6;
            sh3 = 6;
            embryo = 300;
            WBASVU = 8;
            bias = 1;
        }

        else if (mumixr >= 14 && mumixr < 17) {
            sh1 = 0;
            sh2 = 6;
            sh3 = 9;
            embryo = 400;
            WBASVU = 6;
        }

        else { // case of mumixr >= 17
            sh1 = 0;
            sh2 = 2.5f;
            sh3 = 9;
            embryo = 100;
            WBASVU = 5;
        }
        // Run hail model the second time for best category version
        float DavC = 0, Dmax = 0, Dmin = 100000, IC = 0, sig = 0, svr = 0, bias2, ESI, esimax = 0;
        /*
         * separate version =1 and version =2 code. original code uses two goto
         * commands to avoid coding "for loop" twice, but is was very confusing.
         */
        /****************************
         * original ocde (version = 1) run
         *******************************/
        for (float T = To - Toff; T <= To + Toff; T = T + dT) {
            for (float Td = Tdo - Toff; Td <= Tdo + Toff; Td = Td + dT) {
                hailcloud(T, Td, ebs, elevated, lplcount, sh1, sh2, sh3);
                if (tfiSndLys.size() == 0 || wolkdtaSndLys.size() == 0) {
                    continue;
                }
                hailstone(ebs, elevated, lplcount, embryo, subcloudSndLys);

                // convert cm to inches
                D = D / 2.54f;
                if (D >= 1.95) {
                    sig = sig + 1;
                }
                if (D >= 1) {
                    svr = svr + 1;
                }
                ESI = (CAPE * ebs);
                // rej parcel must move faster than 7 m/s and embryo must freeze
                if ((UPMAXV > 7.0) && (nofroze == 1)) {
                    DavC = DavC + D;
                    IC = IC + 1;
                }

                if (D > Dmax) {
                    Dmax = D;
                }
                if (D < Dmin) {
                    Dmin = D;
                }
                if (ESI > esimax) {
                    esimax = ESI;
                }
            }
        }
        hvars.put(3, IC);
        if (IC <= 0) {
            DavC = 0;
        } else {
            DavC = DavC / IC;
        }

        hvars.put(4, DavC);
        hvars.put(5, Dmax);
        if (Dmin > 1000) {
            hvars.put(6, 0f);
        } else {
            hvars.put(6, Dmin);
        }
        hvars.put(7, sig);
        hvars.put(8, svr);
        // best fit for DavC based on reported hail size
        float davcm;
        float report;
        if (DavC <= 0) {
            davcm = 0;
            // 'No hail produced'
            report = 1;
        } else if (DavC < .9) {
            davcm = DavC + .2f;
            // 'Dime to Quarter most likely, isolated Golfball'
            report = 2;
        } else if (DavC <= 1.4) {
            davcm = DavC + .1f;
            // A few golfballs possible
            report = 3;
        } else if (DavC <= 2.2) {
            davcm = DavC + .4f;
            // Tennis / Baseballs possible'
            report = 4;
        } else {
            // case of (DavC > 2.2)
            davcm = DavC + .7f;
            // Baseballs or Large
            report = 5;
        }
        hvars.put(9, davcm);
        hvars.put(10, report);
        float davcb = DavC + .6f;
        hvars.put(11, davcb);
        hvars.put(12, esimax);
        // convert daloft to inches
        Daloft = Daloft / 2.54f;

        hvars.put(13, Daloft);

        if (which == 0) {
            hvars.put(14, DavC + bias);
        } else {
            hvars.put(14, Dmax);
        }
        hvars.put(15, which);
        hvars.put(16, esicat);
        /****************************
         * end of version =1 run
         *******************************/

        /****************************
         * original ocde (version = 2) run
         *******************************/
        which = 1;
        D = 0.0f;
        DavC = 0.0f;
        Dmax = 0.0f;
        Dmin = 100000.0f;
        IC = 0;
        sig = 0f;
        svr = 0;
        esimax = 0;
        esicat = 0;

        sh1 = 1;
        sh2 = 3;
        sh3 = 5;
        embryo = 700;
        WBASVU = 15;
        bias2 = -0.05f;
        for (float T = To - Toff; T <= To + Toff; T = T + dT) {
            for (float Td = Tdo - Toff; Td <= Tdo + Toff; Td = Td + dT) {
                hailcloud(T, Td, ebs, elevated, lplcount, sh1, sh2, sh3);
                if (tfiSndLys.size() == 0 || wolkdtaSndLys.size() == 0) {
                    continue;
                }
                hailstone(ebs, elevated, lplcount, embryo, subcloudSndLys);

                // convert cm to inches
                D = D / 2.54f;
                if (D >= 1.95) {
                    sig = sig + 1;
                }
                if (D >= 1) {
                    svr = svr + 1;
                }
                ESI = (CAPE * ebs);
                // parcel must move faster than 7 m/s and embryo must freeze
                if ((UPMAXV > 7.0) && (nofroze == 1)) {
                    DavC = DavC + D;
                    IC = IC + 1;
                }

                if (D > Dmax) {
                    Dmax = D;
                }
                if (D < Dmin) {
                    Dmin = D;
                }
                if (ESI > esimax) {
                    esimax = ESI;
                }
            }
        }
        /****************************
         * end of version=2 run
         * ************************/

        hvars.put(17, IC);
        if (IC <= 0) {
            DavC = 0;
        } else {
            DavC = DavC / IC;
        }
        hvars.put(18, DavC);
        hvars.put(19, Dmax);
        if (Dmin > 1000) {
            hvars.put(20, 0f);
        } else {
            hvars.put(20, Dmin);
        }
        hvars.put(21, sig);
        hvars.put(22, svr);
        if (Dmax >= 0.05) {
            hvars.put(23, Dmax + bias2);
        } else {
            hvars.put(23, Dmax);
        }

        hvars.put(24, esicat);
        return hvars;
    }
}
