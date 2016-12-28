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
/**
 * SARS (Sounding Analog Retrieval System)
 */
import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibBasics;
import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibSndglib;
import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibWinds;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.Parcel;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.WindComponent;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;
import gov.noaa.nws.ncep.ui.nsharp.display.rsc.NsharpNlistFile.NlistLineInfo;
import gov.noaa.nws.ncep.ui.nsharp.display.rsc.NsharpSupFile.SupLineInfo;

import java.util.ArrayList;
import java.util.List;

public class NsharpSarsInfo {

    private List<String> hailStr = new ArrayList<>();

    private List<Integer> hailStrColor = new ArrayList<>();

    private List<String> supcellStr = new ArrayList<>();

    private List<Integer> supcellStrColor = new ArrayList<>();

    public static int SARS_STRING_LINES = 12;

    private int matches2 = 0;

    private float avsize = 0;

    public NsharpSarsInfo() {
        super();
    }

    /**
     * This function is derived from show_sars() of xwvid3.c of BigNsharp by
     * John Hart NSSFC KCMO Rewrite code to get all computed parameters/string
     * for CAVE. All original BigNsharp gui functions are removed.
     * 
     * @param sndLys
     * @param weatherDataStore
     * @return sarsInfo
     */
    public static NsharpSarsInfo computeSarsInfo(List<NcSoundingLayer> sndLys,
            NsharpWeatherDataStore weatherDataStore) {
        NsharpSarsInfo sarsInfo = new NsharpSarsInfo();

        Parcel mlParcel = weatherDataStore.getParcelMap().get(
                NsharpLibSndglib.PARCELTYPE_MEAN_MIXING);
        Parcel muParcel = weatherDataStore.getParcelMap().get(
                NsharpLibSndglib.PARCELTYPE_MOST_UNSTABLE);

        if (muParcel == null || mlParcel == null) {
            return sarsInfo;
        }
        float mlcape = mlParcel.getBplus();
        float mllcl = mlParcel.getLclAgl();
        float mucape = muParcel.getBplus();
        float mumixr = muParcel.getMixRatio();

        /* Compute Hail Sars Data */
        float temp500 = NsharpLibBasics.i_temp(sndLys, 500);
        float lr75 = weatherDataStore.getSevenHundredTo500mbLapseRate();
        float shr6 = NsharpLibBasics.kt_to_mps(weatherDataStore
                .getStormTypeToWindShearMap().get("SFC-6km"));
        float shr3 = NsharpLibBasics.kt_to_mps(weatherDataStore
                .getStormTypeToWindShearMap().get("SFC-3km"));
        WindComponent windComp9k = NsharpLibWinds.wind_shear(
                sndLys,
                NsharpLibBasics.sfcPressure(sndLys),
                NsharpLibBasics.i_pres(sndLys,
                        NsharpLibBasics.msl(sndLys, 9000)));
        float shr9 = NsharpLibBasics.kt_to_mps(windComp9k.getWspd());
        float srh3 = weatherDataStore.getStormTypeToHelicityMap()
                .get("SFC-3km").getTotalHelicity();
        float srh1 = weatherDataStore.getStormTypeToHelicityMap()
                .get("SFC-1km").getTotalHelicity();
        float pBot = weatherDataStore.getEffLyPress().getBottomPress();
        float hBot = NsharpLibBasics.agl(sndLys,
                NsharpLibBasics.i_hght(sndLys, pBot));
        if (hBot > 0) {
            float pTop = weatherDataStore.getEffLyPress().getTopPress();
            float hTop = NsharpLibBasics.agl(sndLys,
                    NsharpLibBasics.i_hght(sndLys, pTop));

            WindComponent windComp = NsharpLibWinds.wind_shear(
                    sndLys,
                    pBot,
                    NsharpLibBasics.i_pres(sndLys,
                            NsharpLibBasics.msl(sndLys, hTop * 0.25f)));
            shr3 = NsharpLibBasics.kt_to_mps(windComp.getWspd());
            windComp = NsharpLibWinds.wind_shear(
                    sndLys,
                    pBot,
                    NsharpLibBasics.i_pres(sndLys,
                            NsharpLibBasics.msl(sndLys, hTop * 0.5f)));
            shr6 = NsharpLibBasics.kt_to_mps(windComp.getWspd());
            windComp = NsharpLibWinds.wind_shear(
                    sndLys,
                    pBot,
                    NsharpLibBasics.i_pres(sndLys,
                            NsharpLibBasics.msl(sndLys, hTop * 0.75f)));
            shr9 = NsharpLibBasics.kt_to_mps(windComp.getWspd());

            // Note: original Bigsharp code, use pBot, and pTop as input
            // parameters. It is not correct, as input to helicity() should be
            // higher and lower level's AGL not pressure
            srh3 = NsharpLibWinds.helicity(sndLys, hBot, hTop,
                    weatherDataStore.getSmdir(), weatherDataStore.getSmspd())
                    .getTotalHelicity();
            // Not sure if following statement is correct, we just follow
            // original Bigsharp source code
            srh1 = srh3;
        }

        /***********************************************************************
         * The following block of code is converted from sars() subroutine of
         * sars.f of BigSharp This function is specifically used by Bigsharp for
         * SARS graph display. We will just code it here as a block of sars
         * computation.
         ************************************************************************/
        // these were input parameters to sars() and returned to caller
        List<String> sndgList = new ArrayList<>();
        List<Float> hailList = new ArrayList<>();
        int sigcnt = 0;
        int matches = 0;
        int matches2 = 0;
        float avsize = 0;
        float totalSize = 0;
        int tier1 = 0; // number of soundings
        float p1 = 0;
        /**
         * BEGIN of sars()
         */

        // read in nlist.txt contents
        List<NlistLineInfo> nlistLineList = NsharpNlistFile.readNlistFile();
        for (NlistLineInfo lineInfo : nlistLineList) {
            // mixing ratio ranges (g/kg)
            float ranmr = 2;
            float ranmrt1 = 2;
            // determine cape ranges based on cape magnitude (j/kg)
            float rancape = mucape * .30f;
            float rancapet1;
            if (mucape < 500) {
                rancapet1 = mucape * .50f;
            } else if (mucape > 500 && mucape < 2000) {
                rancapet1 = mucape * .25f;
            } else {
                rancapet1 = mucape * .20f;
            }
            // 700-500 mb lapse rate ranges (c/km)
            float ranlr = 2.0f;
            float ranlrt1 = 0.4f;
            // 500 mb temperature ranges (c)
            float rantemp = 9;
            float rantempt1 = 1.5f;
            // 0-6 km shear ranges (m/s)
            float ranshr = 12;
            float ranshrt1 = 6;
            // 0-9 km shear ranges(m/s)
            float rankm9 = 22;
            float rankm9t1 = 15;
            // 0-3 km shear ranges (m/s)
            float ranshr3 = 10;
            float ranshr3t1 = 8;
            // SRH shear ranges (m/s)
            float ransrht1;
            if (srh3 < 50) {
                ransrht1 = 25;
            } else {
                ransrht1 = srh3 * 0.5f;
            }
            boolean mrMatch = false;
            boolean capeMatch = false;
            boolean lrMatch = false;
            boolean tempMatch = false;
            boolean shrMatch = false;
            boolean km9Match = false;
            boolean shr3Match = false;
            boolean srhMatch = false;
            if (mumixr > (lineInfo.getMixingRatio() - ranmr)
                    && mumixr < (lineInfo.getMixingRatio() + ranmr)) {
                mrMatch = true;
            }
            if (mucape > (lineInfo.getCape() - rancape)
                    && mucape < (lineInfo.getCape() + rancape)) {
                capeMatch = true;
            }
            if (lr75 > (lineInfo.getLapseRate75() - ranlr)
                    && lr75 < (lineInfo.getLapseRate75() + ranlr)) {
                lrMatch = true;
            }
            if (temp500 > (lineInfo.getTemp500() - rantemp)
                    && temp500 < (lineInfo.getTemp500() + rantemp)) {
                tempMatch = true;
            }
            if (shr6 > (lineInfo.getShear6km() - ranshr)
                    && shr6 < (lineInfo.getShear6km() + ranshr)) {
                shrMatch = true;
            }
            if (shr9 > (lineInfo.getShear9km() - rankm9)
                    && shr9 < (lineInfo.getShear9km() + rankm9)) {
                km9Match = true;
            }
            if (shr3 > (lineInfo.getShear3km() - ranshr3)
                    && shr3 < (lineInfo.getShear3km() + ranshr3)) {
                shr3Match = true;
            }
            // Check if all 7 parameters are met, exclude datestn
            if (mrMatch && capeMatch && lrMatch && tempMatch && shrMatch
                    && km9Match && shr3Match) {
                // It's a match
                matches = matches + 1;
                // Determine if majority of matches are correct category
                if (lineInfo.isSignificantMatch()) {
                    sigcnt++;
                }
            }
            // Reset Variable for Tier 1 matches
            mrMatch = false;
            capeMatch = false;
            lrMatch = false;
            tempMatch = false;
            shrMatch = false;
            km9Match = false;
            shr3Match = false;
            srhMatch = false;
            // TIER 1
            if (mumixr > (lineInfo.getMixingRatio() - ranmrt1)
                    && mumixr < (lineInfo.getMixingRatio() + ranmrt1)) {
                mrMatch = true;
            }
            if (mucape > (lineInfo.getCape() - rancapet1)
                    && mucape < (lineInfo.getCape() + rancapet1)) {
                capeMatch = true;
            }
            if (lr75 > (lineInfo.getLapseRate75() - ranlrt1)
                    && lr75 < (lineInfo.getLapseRate75() + ranlrt1)) {
                lrMatch = true;
            }
            if (temp500 > (lineInfo.getTemp500() - rantempt1)
                    && temp500 < (lineInfo.getTemp500() + rantempt1)) {
                tempMatch = true;
            }
            if (shr6 > (lineInfo.getShear6km() - ranshrt1)
                    && shr6 < (lineInfo.getShear6km() + ranshrt1)) {
                shrMatch = true;
            }
            if (shr9 > (lineInfo.getShear9km() - rankm9t1)
                    && shr9 < (lineInfo.getShear9km() + rankm9t1)) {
                km9Match = true;
            }
            if (shr3 > (lineInfo.getShear3km() - ranshr3t1)
                    && shr3 < (lineInfo.getShear3km() + ranshr3t1)) {
                shr3Match = true;
            }
            if (srh3 > (lineInfo.getHelicity3km() - ransrht1)
                    && srh3 < (lineInfo.getHelicity3km() + ransrht1)) {
                srhMatch = true;
            }
            // See if sounding matches
            if (mrMatch && capeMatch && lrMatch && tempMatch && shrMatch
                    && km9Match && shr3Match && srhMatch) {
                tier1++;
                if (tier1 < 15) {
                    sndgList.add(lineInfo.getDateStnStr());
                    hailList.add(lineInfo.getSize());
                }
            }
            // Run again, using some new ranges, to find the average match size
            // determine cape ranges based on cape magnitude (j/kg)
            rancape = mucape * .40f;
            // 700-500 mb lapse rate ranges (c/km)
            ranlr = 1.5f;
            // 500 mb temperature ranges (c)
            rantemp = 7;
            // 0-6 km shear ranges (m/s)
            ranshr = 9;

            // Reset Variable
            mrMatch = false;
            capeMatch = false;
            lrMatch = false;
            tempMatch = false;
            shrMatch = false;
            km9Match = false;
            shr3Match = false;
            srhMatch = false;
            if (mumixr > (lineInfo.getMixingRatio() - ranmr)
                    && mumixr < (lineInfo.getMixingRatio() + ranmr)) {
                mrMatch = true;
            }
            if (mucape > (lineInfo.getCape() - rancape)
                    && mucape < (lineInfo.getCape() + rancape)) {
                capeMatch = true;
            }
            if (lr75 > (lineInfo.getLapseRate75() - ranlr)
                    && lr75 < (lineInfo.getLapseRate75() + ranlr)) {
                lrMatch = true;
            }
            if (temp500 > (lineInfo.getTemp500() - rantemp)
                    && temp500 < (lineInfo.getTemp500() + rantemp)) {
                tempMatch = true;
            }
            if (shr6 > (lineInfo.getShear6km() - ranshr)
                    && shr6 < (lineInfo.getShear6km() + ranshr)) {
                shrMatch = true;
            }
            if (shr9 > (lineInfo.getShear9km() - rankm9)
                    && shr9 < (lineInfo.getShear9km() + rankm9)) {
                km9Match = true;
            }
            if (shr3 > (lineInfo.getShear3km() - ranshr3)
                    && shr3 < (lineInfo.getShear3km() + ranshr3)) {
                shr3Match = true;
            }
            // Check if all 7 parameters are met, exclude datestn
            if (mrMatch && capeMatch && lrMatch && tempMatch && shrMatch
                    && km9Match && shr3Match) {
                matches2++;
                totalSize = totalSize + lineInfo.getSize();
            }
        }
        if (matches2 > 0) {
            avsize = totalSize / matches2;
            sarsInfo.setMatches2(matches2);
            sarsInfo.setAvsize(avsize);
        }
        if (matches > 0) {
            p1 = sigcnt / matches * 100;
        }
        /**
         * END of sars()
         */
        // hail match strings...on right side of graphs
        // if number of matched sounding greater than 0
        if (tier1 > 0) {
            if (tier1 > 10) {
                // only display up to 10 sounding lines
                tier1 = 10;
            }
            for (int i = 0; i < 10; i++) {
                if (i < tier1) {
                    float hailSize = hailList.get(i);
                    if (hailSize < 1) {
                        sarsInfo.getHailStrColor().add(i, 8);
                    } else if (hailSize < 2) {
                        sarsInfo.getHailStrColor().add(i, 18);
                    } else {
                        sarsInfo.getHailStrColor().add(i, 5);
                    }
                    // note: sndgList contains matched date and stn from
                    // nlist.txt's first column
                    // hailList contains matched hail size from nlist.txt's 3rd
                    // column
                    String hailString = String.format("%s %.2f",
                            sndgList.get(i), hailList.get(i));
                    sarsInfo.getHailStr().add(i, hailString);
                } else {
                    sarsInfo.getHailStr().add(i, "");
                    sarsInfo.getHailStrColor().add(i, 31);
                }
            }
        } else {
            for (int i = 0; i < 10; i++) {
                sarsInfo.getHailStr().add(i, "");
                sarsInfo.getHailStrColor().add(i, 31);
            }
            sarsInfo.getHailStr().set(5, "No Quality HAIL Match");
        }
        // ----- Plot Hail SARS Result -----
        String st1 = "No Matches";
        sarsInfo.getHailStrColor().add(10, 31);
        sarsInfo.getHailStrColor().add(11, 31);
        if (matches > 0) {
            st1 = "Non-sig Hail";
        }
        if (p1 >= 50) {
            sarsInfo.getHailStrColor().set(10, 12);
            sarsInfo.getHailStrColor().set(11, 12);
            st1 = "**SIG HAIL!**";
        }
        if ((p1 >= 50) || (p1 < 50 && matches > 0)) {
            String str = String.format("(%d matches/%d sndgs)", matches,
                    nlistLineList.size());
            sarsInfo.getHailStr().add(10, str);
        } else {
            sarsInfo.getHailStr().add(10, "");
        }
        String str = String.format("SARS:%s(%.0f%s SIG)", st1, p1, "%");
        sarsInfo.getHailStr().add(11, str);

        // Supercell match plot on left side of graph from here....
        if (hBot > 0) {
            // Note that to compute supercell match, we are using original
            // shr3, shr6 and shr9.
            // However, when compute hail match above, if hBot>0, we have to
            // use different values for them, therefore, we have to reset
            // shr3, shr6, shr9 here when hBot>0.
            shr6 = NsharpLibBasics.kt_to_mps(weatherDataStore
                    .getStormTypeToWindShearMap().get("SFC-6km"));
            shr3 = NsharpLibBasics.kt_to_mps(weatherDataStore
                    .getStormTypeToWindShearMap().get("SFC-3km"));
            shr9 = NsharpLibBasics.kt_to_mps(windComp9k.getWspd());
        }
        /*************************************************************************
         * The following block of code is converted from sup_nsharp() subroutine
         * of sup_nsharp.f of BigSharp. This function is specifically used by
         * Bigsharp for SARS graph display. We will just code it here as a block
         * of sars computation.
         ************************************************************************/
        /**
         * BEGIN of sup_nsharp()
         */
        // read in sup.txt contents
        List<SupLineInfo> supLineList = NsharpSupFile.readSupFile();
        // count number of soundings
        int cnt = supLineList.size();

        // mlcape ranges
        float ranmlcape = 1300;
        float ranmlcapet1 = mlcape * 0.25f;

        // mllcl ranges
        float ranmllcl = 50;
        float ranmllclt1 = 200;

        // 0-6 km shear ranges (kt) -
        float ranshr = 14;
        float ranshrt1 = 10;

        // 0-1 km srh ranges (m2/s2)
        float ransrh;
        if (Math.abs(srh1) < 50) {
            ransrh = 100;
        } else {
            ransrh = srh1;
        }
        float ransrht1;
        if (Math.abs(srh1) < 100) {
            ransrht1 = 50;
        } else {
            ransrht1 = (Math.abs(srh1)) * 0.30f;
        }
        // 0-3 srh tier 1 ranges
        float ransrh3t1;
        if (Math.abs(srh3) < 100) {
            ransrh3t1 = 50;
        } else {
            ransrh3t1 = (Math.abs(srh3)) * 0.50f;
        }

        // 500 mb temperature ranges (c)
        float rantemp = 7;
        float rantempt1 = 5;

        // 700-500 mb lapse rate ranges (c/km)
        float ranlr = 1.0f;
        float ranlrt1 = 0.8f;

        // 3km and 9km shear matching
        float ranshr3kt1 = 15;
        float ranshr9kt1 = 25;

        List<String> supSndgList = new ArrayList<>();
        List<Integer> supList = new ArrayList<>();
        // initialize counters..
        tier1 = 0;
        matches = 0;
        int torcnt = 0;
        int noncnt = 0;
        p1 = 0;

        for (SupLineInfo supLineInfo : supLineList) {
            boolean mlcapeMatch = false;
            boolean mllclMatch = false;
            boolean shrMatch = false;
            boolean srhMatch = false;
            boolean tempMatch = false;
            boolean lrMatch = false;

            if (mlcape >= (supLineInfo.getCape() - ranmlcape)
                    && mlcape <= (supLineInfo.getCape() + ranmlcape)) {
                mlcapeMatch = true;
            }
            if (mllcl >= (supLineInfo.getLcl() - ranmllcl)
                    && mllcl <= (supLineInfo.getLcl() + ranmllcl)) {
                mllclMatch = true;
            }
            if (shr6 >= (supLineInfo.getShear6km() - ranshr)
                    && shr6 <= (supLineInfo.getShear6km() + ranshr)) {
                shrMatch = true;
            }
            if (srh1 >= (supLineInfo.getHelicity1km() - ransrh)
                    && srh1 <= (supLineInfo.getHelicity1km() + ransrh)) {
                srhMatch = true;
            }
            if (temp500 >= (supLineInfo.getTemp500() - rantemp)
                    && temp500 <= (supLineInfo.getTemp500() + rantemp)) {
                tempMatch = true;
            }
            if (lr75 >= (supLineInfo.getLapseRate75() - ranlr)
                    && lr75 <= (supLineInfo.getLapseRate75() + ranlr)) {
                lrMatch = true;
            }
            // Check if all 6 parameters are met
            if (mlcapeMatch && mllclMatch && shrMatch && srhMatch && tempMatch
                    && lrMatch) {
                // Determine if majority of matches are correct category
                if (supLineInfo.getTornadoType() == 1
                        || supLineInfo.getTornadoType() == 2) {
                    torcnt = torcnt + 1;
                } else if (supLineInfo.getTornadoType() == 0) {
                    noncnt = noncnt + 1;
                }

            }
            // Reset Variable for Tier 1 matches
            mlcapeMatch = false;
            mllclMatch = false;
            shrMatch = false;
            srhMatch = false;
            tempMatch = false;
            lrMatch = false;
            boolean shr3kMatch = false;
            boolean shr9kMatch = false;
            boolean srh3kMatch = false;
            // TIER 1 run
            if (mlcape >= (supLineInfo.getCape() - ranmlcapet1)
                    && mlcape <= (supLineInfo.getCape() + ranmlcapet1)) {
                mlcapeMatch = true;
            }
            if (mllcl >= (supLineInfo.getLcl() - ranmllclt1)
                    && mllcl <= (supLineInfo.getLcl() + ranmllclt1)) {
                mllclMatch = true;
            }
            if (shr6 >= (supLineInfo.getShear6km() - ranshrt1)
                    && shr6 <= (supLineInfo.getShear6km() + ranshrt1)) {
                shrMatch = true;
            }
            if (srh1 >= (supLineInfo.getHelicity1km() - ransrht1)
                    && srh1 <= (supLineInfo.getHelicity1km() + ransrht1)) {
                srhMatch = true;
            }
            if (temp500 >= (supLineInfo.getTemp500() - rantempt1)
                    && temp500 <= (supLineInfo.getTemp500() + rantempt1)) {
                tempMatch = true;
            }
            if (lr75 >= (supLineInfo.getLapseRate75() - ranlrt1)
                    && lr75 <= (supLineInfo.getLapseRate75() + ranlrt1)) {
                lrMatch = true;
            }
            if (shr3 >= (supLineInfo.getShear3km() - ranshr3kt1)
                    && shr3 <= (supLineInfo.getShear3km() + ranshr3kt1)) {
                shr3kMatch = true;
            }
            if (shr9 >= (supLineInfo.getShear9km() - ranshr9kt1)
                    && shr9 <= (supLineInfo.getShear9km() + ranshr9kt1)) {
                shr9kMatch = true;
            }
            if (srh3 >= (supLineInfo.getHelicity3km() - ransrh3t1)
                    && srh3 <= (supLineInfo.getHelicity3km() + ransrh3t1)) {
                srh3kMatch = true;
            }
            // Check if all 9 parameters are met
            if (mlcapeMatch && mllclMatch && shrMatch && srhMatch && tempMatch
                    && lrMatch && shr3kMatch && shr9kMatch && srh3kMatch) {
                tier1 = tier1 + 1;
                if (tier1 < 15) {
                    supSndgList.add(supLineInfo.getDateStnStr());
                    supList.add(supLineInfo.getTornadoType());

                }
            }
            matches = torcnt + noncnt;
            if (matches != 0) {
                p1 = torcnt / matches * 100;

            }
        }
        /**
         * END of sup_nsharp()
         */

        if (tier1 > 0) {
            if (tier1 > 10)
                tier1 = 10;
            for (int i = 0; i < 10; i++) {
                if (i < tier1) {
                    // supList stores tornado type as defined in sup_nsharp.f
                    // 0: "NONTOR", 1: "WEAKTOR", 2: "SIGTOR" };
                    int tornadoType = supList.get(i);
                    String tornadoStr;
                    switch (tornadoType) {
                    case 0:
                        sarsInfo.getSupcellStrColor().add(i, 18);
                        tornadoStr = "NONTOR";
                        break;
                    case 1:
                        sarsInfo.getSupcellStrColor().add(i, 6);
                        tornadoStr = "WEAKTOR";
                        break;
                    case 2:
                        sarsInfo.getSupcellStrColor().add(i, 2);
                        tornadoStr = "SIGTOR";
                        break;
                    default:
                        sarsInfo.getSupcellStrColor().add(i, 18);
                        tornadoStr = "NONTOR";
                        break;
                    }
                    String supStr = String.format("%s  %s", supSndgList.get(i),
                            tornadoStr);
                    sarsInfo.getSupcellStr().add(i, supStr);

                } else {
                    sarsInfo.getSupcellStr().add(i, "");
                    sarsInfo.getSupcellStrColor().add(i, 31);
                }
            }

        } else {
            for (int i = 0; i < 10; i++) {
                sarsInfo.getSupcellStr().add(i, "");
                sarsInfo.getSupcellStrColor().add(i, 31);
            }

            sarsInfo.getSupcellStr().set(5, "No Quality Supercell Match");
        }
        // Plot Supercell SARS Result -----
        sarsInfo.getSupcellStrColor().add(10, 31);
        sarsInfo.getSupcellStrColor().add(11, 31);
        st1 = "No Matches";
        Parcel sfcParcel = weatherDataStore.getParcelMap().get(
                NsharpLibSndglib.PARCELTYPE_OBS_SFC);

        if (sfcParcel.getBplus() >= 100) {
            if (matches > 0) {
                st1 = "NONTOR";
            }
            if (p1 > 50) {
                sarsInfo.getSupcellStrColor().set(10, 12);
                sarsInfo.getSupcellStrColor().set(11, 12);
                st1 = "**TOR!**";
            }
            if ((p1 > 50) || (p1 <= 50 && matches > 0)) {
                str = String.format("(%d matches/%d sndgs)", matches, cnt);
                sarsInfo.getSupcellStr().add(10, str);
            } else {
                sarsInfo.getSupcellStr().add(10, "");
            }
            str = String.format("SARS:%s(%.0f%s TOR)", st1, p1, "%");
            sarsInfo.getSupcellStr().add(11, str);
        } else {
            sarsInfo.getSupcellStr().add(10, "");
            sarsInfo.getSupcellStr().add(11, st1);
        }

        return sarsInfo;
    }

    public List<String> getHailStr() {
        return hailStr;
    }

    public void setHailStr(List<String> hailStr) {
        this.hailStr = hailStr;
    }

    public List<Integer> getHailStrColor() {
        return hailStrColor;
    }

    public void setHailStrColor(List<Integer> hailStrColor) {
        this.hailStrColor = hailStrColor;
    }

    public List<String> getSupcellStr() {
        return supcellStr;
    }

    public void setSupcellStr(List<String> supcellStr) {
        this.supcellStr = supcellStr;
    }

    public List<Integer> getSupcellStrColor() {
        return supcellStrColor;
    }

    public void setSupcellStrColor(List<Integer> supcellStrColor) {
        this.supcellStrColor = supcellStrColor;
    }

    public int getMatches2() {
        return matches2;
    }

    public void setMatches2(int matches2) {
        this.matches2 = matches2;
    }

    public float getAvsize() {
        return avsize;
    }

    public void setAvsize(float avsize) {
        this.avsize = avsize;
    }

}
