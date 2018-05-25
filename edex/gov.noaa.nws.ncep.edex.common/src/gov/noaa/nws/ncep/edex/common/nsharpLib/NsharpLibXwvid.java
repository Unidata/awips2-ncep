package gov.noaa.nws.ncep.edex.common.nsharpLib;

import java.util.List;

/**
 *
 *
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 *
 * All methods developed in this class are based on the
 * algorithm developed in BigSharp native C files, xwvid1.c, xwvid2.c,
 * ...xwvid6.c, by John A.Hart and Richard L. Thompson/SPC. All methods name are
 * defined with same name as the C function name defined in native code.
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date         Ticket#     Engineer    Description
 * -------      -------     --------    -----------
 * 02/25/2016   RM#13744    Chin Chen   NSHARP - Native Code replacement. Phase 1&2. Initial coding.
 * 06/13/2017   RM#34793    Chin Chen   Add support of max lapse rate
 * 07/28/2017   RM#34795    Chin Chen   NSHARP - Updates for March 2017 bigSharp version
 *                                      - Added output for the "large hail parameter" and
 *                                      the "modified SHERBE" parameter,..etc.
 * May, 5, 2018 49896       mgamazaychikov  Fixed NPEs (lines 808, 1061, 1136), fixed formatting
 *
 * </pre>
 *
 * @author Chin Chen
 * @version 1.0
 *
 */
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.EffectiveLayerPressures;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.Helicity;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.LParcelValues;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.LapseRateMax;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.LayerParameters;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.Parcel;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.WindComponent;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;

public class NsharpLibXwvid {

    public static EffectiveLayerPressures effective_inflow_layer(List<NcSoundingLayer> sndLys, float ecape, float ecinh,
            Parcel muParcel)
    /******************************************************************/
    /* Effective Inflow Layer */
    /* John Hart & Rich Thompson SPC Norman OK */
    /*                                                                */
    /* Calculates the effective layer top and bottom (mb). */
    /* Based on research by Thompson et. al. 2004 */
    /* Input: most unstable parcel */
    /******************************************************************/
    {
        float mucape = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        float mucin = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        float mucp = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        float mucn = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        EffectiveLayerPressures effLayerParams = new EffectiveLayerPressures();
        if (muParcel == null) {
            return effLayerParams;
        }
        // original code calls define_parcel(3, 300); and then calls parcel()
        LParcelValues lparcelVs = NsharpLibSkparams.define_parcel(sndLys, NsharpLibSndglib.PARCELTYPE_MOST_UNSTABLE,
                300);
        Parcel parcel = NsharpLibSkparams.parcel(sndLys, -1.0F, -1.0F, lparcelVs.getPres(), lparcelVs.getTemp(),
                lparcelVs.getDwpt());

        if (parcel != null) {
            mucp = parcel.getBplus();
            mucn = parcel.getBminus();
        }
        // original code calls define_parcel(3, 400); and then calls parcel()
        // that means defines parcel as most unstable parcel with pressure at
        // 400 mb,
        // this is the same as the default parcel value already computed
        // first thing at NsharpLibManager.computeWeatherParameters() when
        // sounding is set.
        // Therefore, just get it from NsharpLibManager
        mucape = muParcel.getBplus();
        mucin = muParcel.getBminus();

        /*
         * scenario where shallow buoyancy present for lesser theta parcel near
         * ground
         */
        if (mucp > mucape) {
            mucape = mucp;
            mucin = mucn;
        }

        if (mucape >= 100 && mucin >= -250) {

            int sfcIndex = NsharpLibBasics.sfcIndex(sndLys);
            int soundingSize = sndLys.size();
            if (sfcIndex < 0 || soundingSize <= 0) {
                return effLayerParams;
            }
            /*
             * ----- Begin at surface and search upward for "Effective Surface"
             * -----
             */

            float botPress = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
            for (int i = sfcIndex; i <= soundingSize - 1; i++) {
                NcSoundingLayer sndLayer = sndLys.get(i);
                parcel = NsharpLibSkparams.parcel(sndLys, -1.0F, -1.0F, sndLayer.getPressure(),
                        sndLayer.getTemperature(), sndLayer.getDewpoint());

                if ((parcel.getBplus() >= ecape) && (parcel.getBminus() >= ecinh)) {
                    botPress = sndLayer.getPressure();
                    effLayerParams.setBottomPress(botPress);
                    break;
                }
            }

            if (botPress == NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA) {
                return effLayerParams;
            }

            float topPress = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

            /* ----- Keep searching upward for the "Effective Top" ----- */
            for (int i = sfcIndex; i <= soundingSize - 1; i++) {
                NcSoundingLayer sndLayer = sndLys.get(i);
                if (sndLayer.getPressure() <= effLayerParams.getBottomPress()) {
                    if (NsharpLibBasics.qc(sndLayer.getTemperature()) && NsharpLibBasics.qc(sndLayer.getDewpoint())) {

                        parcel = NsharpLibSkparams.parcel(sndLys, -1.0F, -1.0F, sndLayer.getPressure(),
                                sndLayer.getTemperature(), sndLayer.getDewpoint());
                        if ((parcel.getBplus() <= ecape) || (parcel.getBminus() <= ecinh))
                        /*
                         * check for missing T/Td data with significant wind
                         * levels in obs soundings
                         */
                        {
                            boolean ok = true;
                            int index;
                            int j = 1;
                            NcSoundingLayer sndLayer2;
                            while (ok) {
                                index = i - j;
                                if (index < 0) {
                                    return effLayerParams;
                                }
                                sndLayer2 = sndLys.get(index);

                                if (NsharpLibBasics.qc(sndLayer2.getTemperature())
                                        && NsharpLibBasics.qc(sndLayer2.getDewpoint())) {
                                    topPress = sndLayer2.getPressure();
                                    ok = false;
                                } else {
                                    j++;
                                }
                            }

                            effLayerParams.setTopPress(topPress);
                            return effLayerParams;
                        }
                    }
                }
            }
        }
        return effLayerParams;
    }

    public static float damaging_wind(List<NcSoundingLayer> sndLys, Parcel mmParcel)
    /********************************************************************/
    /* Damaging wind potential */
    /* (uses mlcape, 0-3 km lr, and 850-600 mb mean wind speed) */
    /* Input: mean mixing parcel */
    /* Rich Thompson SPC OUN */
    /********************************************************************/

    {
        if (mmParcel == null) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        float mlcp, mlcn, wndg, lapseRate;

        NcSoundingLayer sfcLayer = NsharpLibBasics.sfc(sndLys);
        if (sfcLayer == null) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        lapseRate = NsharpLibSkparams.lapse_rate(sndLys, sfcLayer.getPressure(),
                NsharpLibBasics.i_pres(sndLys, sfcLayer.getGeoHeight() + 3000));
        if (lapseRate < 7) {
            lapseRate = 0.0f;
        }

        mlcp = mmParcel.getBplus();

        if (mmParcel.getBminus() > -10.0) {
            mlcn = 1;
        }
        if (mmParcel.getBminus() < -50.0) {
            mlcn = 0;
        } else {
            mlcn = ((50 + mmParcel.getBminus()) / 40);
        }

        WindComponent windComp = NsharpLibWinds.mean_wind_npw(sndLys,
                NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.msl(sndLys, 1000.0f)),
                NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.msl(sndLys, 3500.0f)), 0);
        if (!NsharpLibBasics.qc(windComp.getWspd())) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        wndg = ((mlcp / 2000) * (lapseRate / 9) * (windComp.getWspd() / 30) * (mlcn));

        return wndg;
    }

    public static float esp(List<NcSoundingLayer> sndLys, Parcel mmParcel)
    /********************************************************************/
    /* Enhanced Stretching Potential (Jon Davies) */
    /* (uses 0-3 km mlcape and lapse rate) */
    /* Input: mean mixing parcel */
    /********************************************************************/
    {
        if (mmParcel == null) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        float lapseRate, cape3km, esp;
        NcSoundingLayer sfcLayer = NsharpLibBasics.sfc(sndLys);
        lapseRate = NsharpLibSkparams.lapse_rate(sndLys, sfcLayer.getPressure(),
                NsharpLibBasics.i_pres(sndLys, sfcLayer.getGeoHeight() + 3000f));
        if (lapseRate < 7.0) {
            lapseRate = 7.0f;
        }

        cape3km = mmParcel.getCape3km();

        if (mmParcel.getBplus() < 250.0) {
            esp = 0.0f;
        } else {
            esp = ((lapseRate - 7.0f) / 1.0f) * (cape3km / 50f);
        }
        return esp;
    }

    public static float scp(List<NcSoundingLayer> sndLys, float stdir, float stspd, Parcel muParcel)
    /***************************************************************/
    /*                                                             */
    /* Computes the Supercell Composite Parameter */
    /* (uses effective layer) */
    /* Input: stdir and stspd are the storm wind direction and */
    /* speed of the return from bunkers_storm_motion() */
    /* muParcel: most unstable parcel */
    /* Output: Supercell Composite Parameter */
    /*                                                             */
    /***************************************************************/
    {
        if (!NsharpLibBasics.qc(stdir) || !NsharpLibBasics.qc(stspd) || muParcel == null) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        // original code calls define_parcel(3, 400); and then calls parcel()
        // that means defines parcel as most unstable parcel with pressure at
        // 400 mb,
        // this is the same as the default parcel value already computed
        // first thing at NsharpLibManager.computeWeatherParameters() when
        // sounding is set.
        // Therefore, just get it from NsharpLibManager
        float cape = muParcel.getBplus();
        if (!NsharpLibBasics.qc(cape)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }

        float el = NsharpLibBasics.agl(sndLys, NsharpLibBasics.i_hght(sndLys, muParcel.getElpres()));

        EffectiveLayerPressures effLyPress = effective_inflow_layer(sndLys, 100, -250, muParcel);
        float base = NsharpLibBasics.agl(sndLys, NsharpLibBasics.i_hght(sndLys, effLyPress.getBottomPress()));
        float depth = (el - base);

        WindComponent shearWindComp = NsharpLibWinds.wind_shear(sndLys, effLyPress.getBottomPress(),
                NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.msl(sndLys, base + (depth * 0.5f))));
        float eshear = shearWindComp.getWspd();

        if ((cape >= 100) && (el < 0)) {
            shearWindComp = NsharpLibWinds.wind_shear(sndLys,
                    NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.msl(sndLys, 0)),
                    NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.msl(sndLys, 6000f)));
            eshear = shearWindComp.getWspd();
        }
        Helicity helicity = NsharpLibWinds.helicity(sndLys, base,
                NsharpLibBasics.agl(sndLys, NsharpLibBasics.i_hght(sndLys, effLyPress.getTopPress())), stdir, stspd);
        float esrh = helicity.getTotalHelicity();
        if (eshear < 20) {
            eshear = 0;
        }
        if (eshear > 40) {
            eshear = 1;
        } else {
            eshear = (eshear / 40);
        }
        if (!NsharpLibBasics.qc(esrh)) {
            esrh = 0;
        }

        float scp_new = (eshear * (esrh / 50) * (cape / 1000));

        return scp_new;
    }

    public static WindComponent[] bunkers_storm_motion(List<NcSoundingLayer> sndLys, Parcel muParcel)
    /*************************************************************
     * BUNKERS_STORM_MOTION & BUNKERS_LEFT_MOTION This method now contains two
     * original Bigsharp methods, bunkers_storm_motion() and
     * bunkers_left_motion(), using a method developed by Bunkers et. al.
     * (2000), modified to account for clearly elevated storms
     *
     * bunkers_storm_motion():: Calculates the motion of a right-moving
     * supercell bunkers_left_motion():: Calculates the motion of a left-moving
     * supercell
     *
     * @param :
     *            Parcel - most unstable parcel
     * @return : storm motion wind component array{2]. bunkerStormWndComp[0]:
     *         storm right motion wind component bunkerStormWndComp[1]: storm
     *         left motion wind component Updated based on bigSharp9.3 2017Feb27
     *         version - Chin 06/30/2017
     *************************************************************/
    {
        WindComponent bunkerStormWndComp[] = new WindComponent[2];
        WindComponent rightMotionWIndComp = new WindComponent();
        bunkerStormWndComp[0] = rightMotionWIndComp;
        WindComponent leftMotionWIndComp = new WindComponent();
        bunkerStormWndComp[1] = leftMotionWIndComp;
        if (muParcel == null) {
            return bunkerStormWndComp;
        }
        /* Deviation Value (emperically derived as 8 m/s) */
        float d = 7.5f / 0.51479f;
        // original code calls define_parcel(3, 400); and then calls parcel()
        // that means defines parcel as most unstable parcel with pressure at
        // 400 mb,
        // this is the same as the default parcel value already computed
        // first thing at NsharpLibManager.computeWeatherParameters() when
        // sounding is set.
        // Therefore, just get it from NsharpLibManager

        float mucp = muParcel.getBplus();
        float mucn = muParcel.getBminus();
        float el = NsharpLibBasics.agl(sndLys, NsharpLibBasics.i_hght(sndLys, muParcel.getElpres()));
        EffectiveLayerPressures effLyPress = effective_inflow_layer(sndLys, 100, -250, muParcel);
        float base = NsharpLibBasics.agl(sndLys, NsharpLibBasics.i_hght(sndLys, effLyPress.getBottomPress()));
        float ucompRight = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        float vcompRight = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        float wndDirRight = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        float wndSpdRight = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        float ucompLeft = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        float vcompLeft = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        float wndDirLeft = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        float wndSpdLeft = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        float depth = (el - base);
        if (mucp >= 100 && mucn >= -250 && el > 0 && depth >= 3000) {
            /*
             * 21 March 2014 - include Bunkers et al. (2014) findings using 65%
             * of depth from inflow base to mu parcel EL height
             */
            WindComponent meanWndComp = NsharpLibWinds.mean_wind(sndLys, effLyPress.getBottomPress(),
                    NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.msl(sndLys, base + (depth * 0.65f))));

            WindComponent wndShearComp = NsharpLibWinds.wind_shear(sndLys, effLyPress.getBottomPress(),
                    NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.msl(sndLys, base + (depth * 0.65f))));

            if (NsharpLibBasics.qc(wndShearComp.getUcomp()) && NsharpLibBasics.qc(wndShearComp.getVcomp())) {
                ucompRight = meanWndComp.getUcomp()
                        + ((d / (float) Math.pow((wndShearComp.getUcomp() * wndShearComp.getUcomp())
                                + (wndShearComp.getVcomp() * wndShearComp.getVcomp()), 0.5f))
                                * wndShearComp.getVcomp());
                vcompRight = meanWndComp.getVcomp()
                        - ((d / (float) Math.pow((wndShearComp.getUcomp() * wndShearComp.getUcomp())
                                + (wndShearComp.getVcomp() * wndShearComp.getVcomp()), 0.5f))
                                * wndShearComp.getUcomp());
                wndDirRight = NsharpLibWinds.angle(ucompRight, vcompRight);
                wndSpdRight = NsharpLibWinds.speed(ucompRight, vcompRight);
                ucompLeft = meanWndComp.getUcomp()
                        - ((d / (float) Math.pow((wndShearComp.getUcomp() * wndShearComp.getUcomp())
                                + (wndShearComp.getVcomp() * wndShearComp.getVcomp()), 0.5f))
                                * wndShearComp.getVcomp());
                vcompLeft = meanWndComp.getVcomp()
                        + ((d / (float) Math.pow((wndShearComp.getUcomp() * wndShearComp.getUcomp())
                                + (wndShearComp.getVcomp() * wndShearComp.getVcomp()), 0.5f))
                                * wndShearComp.getUcomp());
                wndDirLeft = NsharpLibWinds.angle(ucompLeft, vcompLeft);
                wndSpdLeft = NsharpLibWinds.speed(ucompLeft, vcompLeft);
            }
        } else {
            /*
             * default to standard 0-6 km layer if cape > 100 but EL height is
             * missing
             */
            /* Sfc-6km mean wind */
            WindComponent meanWndComp = NsharpLibWinds.mean_wind_npw(sndLys, NsharpLibBasics.sfcPressure(sndLys),
                    NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.msl(sndLys, 6000.0f)), 0);
            /* Sfc-6km Shear Vector */
            WindComponent wndShearComp = NsharpLibWinds.wind_shear(sndLys, NsharpLibBasics.sfcPressure(sndLys),
                    NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.msl(sndLys, 6000.0f)));
            if (NsharpLibBasics.qc(wndShearComp.getUcomp()) && NsharpLibBasics.qc(wndShearComp.getVcomp())) {
                ucompRight = meanWndComp.getUcomp()
                        + ((d / (float) Math.pow((wndShearComp.getUcomp() * wndShearComp.getUcomp())
                                + (wndShearComp.getVcomp() * wndShearComp.getVcomp()), 0.5f))
                                * wndShearComp.getVcomp());
                vcompRight = meanWndComp.getVcomp()
                        - ((d / (float) Math.pow((wndShearComp.getUcomp() * wndShearComp.getUcomp())
                                + (wndShearComp.getVcomp() * wndShearComp.getVcomp()), 0.5f))
                                * wndShearComp.getUcomp());
                wndDirRight = NsharpLibWinds.angle(ucompRight, vcompRight);
                wndSpdRight = NsharpLibWinds.speed(ucompRight, vcompRight);
                ucompLeft = meanWndComp.getUcomp()
                        - ((d / (float) Math.pow((wndShearComp.getUcomp() * wndShearComp.getUcomp())
                                + (wndShearComp.getVcomp() * wndShearComp.getVcomp()), 0.5f))
                                * wndShearComp.getVcomp());
                vcompLeft = meanWndComp.getVcomp()
                        + ((d / (float) Math.pow((wndShearComp.getUcomp() * wndShearComp.getUcomp())
                                + (wndShearComp.getVcomp() * wndShearComp.getVcomp()), 0.5f))
                                * wndShearComp.getUcomp());
                wndDirLeft = NsharpLibWinds.angle(ucompLeft, vcompLeft);
                wndSpdLeft = NsharpLibWinds.speed(ucompLeft, vcompLeft);

            }
        }

        rightMotionWIndComp.setUcomp(ucompRight);
        rightMotionWIndComp.setVcomp(vcompRight);
        rightMotionWIndComp.setWdir(wndDirRight);
        rightMotionWIndComp.setWspd(wndSpdRight);

        leftMotionWIndComp.setUcomp(ucompLeft);
        leftMotionWIndComp.setVcomp(vcompLeft);
        leftMotionWIndComp.setWdir(wndDirLeft);
        leftMotionWIndComp.setWspd(wndSpdLeft);

        return bunkerStormWndComp;
    }

    public static float sigtorn_cin(List<NcSoundingLayer> sndLys, float stdir, float stspd, Parcel muParcel,
            Parcel mmParcel)
    /*****************************************************************/
    /* Significant Tornado Parameter (effective layer with CIN) */
    /* Input: stdir and stspd are the storm wind direction and */
    /* speed of the return from bunkers_storm_motion() */
    /* Output: Significant Tornado Parameter */
    /*****************************************************************/
    {

        // original code calls define_parcel(3, 400); and then calls parcel()
        // that means defines parcel as most unstable parcel with pressure at
        // 400 mb,
        // this is the same as the default parcel value already computed
        // first thing at NsharpLibManager.computeWeatherParameters() when
        // sounding is set.
        // Therefore, just get it from NsharpLibManager
        if (muParcel == null || mmParcel == null) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        float cape = muParcel.getBplus();
        if (!NsharpLibBasics.qc(cape)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        float el = NsharpLibBasics.agl(sndLys, NsharpLibBasics.i_hght(sndLys, muParcel.getElpres()));
        EffectiveLayerPressures effLyPress = effective_inflow_layer(sndLys, 100, -250, muParcel);
        float base = NsharpLibBasics.agl(sndLys, NsharpLibBasics.i_hght(sndLys, effLyPress.getBottomPress()));
        float depth = (el - base);

        WindComponent shearWindComp = NsharpLibWinds.wind_shear(sndLys, effLyPress.getBottomPress(),
                NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.msl(sndLys, base + (depth * 0.5f))));
        float eshear = shearWindComp.getWspd();

        if ((cape >= 100) && (el < 0)) {
            shearWindComp = NsharpLibWinds.wind_shear(sndLys,
                    NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.msl(sndLys, 0)),
                    NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.msl(sndLys, 6000f)));
            eshear = shearWindComp.getWspd();
        }
        Helicity helicity = NsharpLibWinds.helicity(sndLys, base,
                NsharpLibBasics.agl(sndLys, NsharpLibBasics.i_hght(sndLys, effLyPress.getTopPress())), stdir, stspd);
        float esrh = helicity.getPosHelicity();
        // original code calls define_parcel(4, 100); and then calls parcel()
        // that means defines parcel as mean mixing parcel with pressure at
        // 100 mb,
        // this is the same as the default parcel value already computed
        // first thing at NsharpLibManager.computeWeatherParameters() when
        // sounding is set.
        // Therefore, just get it from NsharpLibManager

        float mlcape = mmParcel.getBplus();

        float mlcinh = mmParcel.getBminus();

        if (!NsharpLibBasics.qc(mlcape)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }

        if (mlcinh >= -50) {
            mlcinh = 1;
        } else {
            if (mlcinh < -200) {
                mlcinh = 0;
            } else {
                mlcinh = ((200 + mlcinh) / 150);
            }
        }
        float lclh = NsharpLibBasics.agl(sndLys, NsharpLibBasics.i_hght(sndLys, mmParcel.getLclpres()));

        if (lclh > 2000) {
            lclh = 0;
        } else {
            if (lclh <= 1000) {
                lclh = 1;
            } else {
                lclh = ((2000 - lclh) / 1000);
            }
        }
        if (eshear < 25) {
            eshear = 0;
        } else {
            if (eshear >= 60) {
                eshear = 1.5f;
            } else {
                eshear = (eshear / 40);
            }
        }
        if (esrh < -9998) {
            esrh = 0;
        }

        float stp_cin = (eshear) * (esrh / 150) * (mlcape / 1500) * (lclh) * (mlcinh);

        if (NsharpLibBasics.agl(sndLys, NsharpLibBasics.i_hght(sndLys, effLyPress.getBottomPress())) > 0.0) {
            stp_cin = 0;
        }
        if (stp_cin < 0) {
            stp_cin = 0;
        }
        return stp_cin;

    }

    public static float sigtorn_test(List<NcSoundingLayer> sndLys, float stdir, float stspd, Parcel muParcel,
            Parcel mmParcel)
    /*****************************************************************/
    /* Significant Tornado Parameter (effective layer with CIN) */
    /* Input: stdir and stspd are the storm wind direction and */
    /* speed of the return from bunkers_storm_motion() */
    /* Output: Significant Tornado Parameter */
    /*****************************************************************/
    {

        // original code calls define_parcel(3, 400); and then calls parcel()
        // that means defines parcel as most unstable parcel with pressure at
        // 400 mb,
        // this is the same as the default parcel value already computed
        // first thing at NsharpLibManager.computeWeatherParameters() when
        // sounding is set.
        // Therefore, just get it from NsharpLibManager
        if (muParcel == null || mmParcel == null) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        float cape = muParcel.getBplus();
        if (!NsharpLibBasics.qc(cape)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        float el = NsharpLibBasics.agl(sndLys, NsharpLibBasics.i_hght(sndLys, muParcel.getElpres()));
        EffectiveLayerPressures effLyPress = effective_inflow_layer(sndLys, 100, -250, muParcel);
        float base = NsharpLibBasics.agl(sndLys, NsharpLibBasics.i_hght(sndLys, effLyPress.getBottomPress()));
        float depth = (el - base);

        WindComponent shearWindComp = NsharpLibWinds.wind_shear(sndLys, effLyPress.getBottomPress(),
                NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.msl(sndLys, base + (depth * 0.5f))));
        float eshear = shearWindComp.getWspd();

        if ((cape >= 100) && (el < 0)) {
            shearWindComp = NsharpLibWinds.wind_shear(sndLys,
                    NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.msl(sndLys, 0)),
                    NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.msl(sndLys, 6000f)));
            eshear = shearWindComp.getWspd();
        }
        Helicity helicity = NsharpLibWinds.helicity(sndLys, base,
                NsharpLibBasics.agl(sndLys, NsharpLibBasics.i_hght(sndLys, effLyPress.getTopPress())), stdir, stspd);
        float esrh = helicity.getTotalHelicity();
        // original code calls define_parcel(4, 100); and then calls parcel()
        // that means defines parcel as mean mixing parcel with pressure at
        // 100 mb,
        // this is the same as the default parcel value already computed
        // first thing at NsharpLibManager.computeWeatherParameters() when
        // sounding is set.
        // Therefore, just get it from NsharpLibManager
        float cape6km = mmParcel.getCape6km();

        if (!NsharpLibBasics.qc(cape6km)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }

        float lclh = NsharpLibBasics.agl(sndLys, NsharpLibBasics.i_hght(sndLys, mmParcel.getLclpres()));

        if (lclh > 2000) {
            lclh = 0;
        } else {
            if (lclh <= 1000) {
                lclh = 1;
            } else {
                lclh = ((2000 - lclh) / 1000);
            }
        }
        if (eshear < 25) {
            eshear = 0;
        } else {
            if (eshear >= 60) {
                eshear = 1.5f;
            } else {
                eshear = (eshear / 40);
            }
        }
        if (esrh < -9998) {
            esrh = 0;
        }

        float stp = (eshear) * (esrh / 150) * (cape6km / 500) * (lclh);

        if (NsharpLibBasics.agl(sndLys, NsharpLibBasics.i_hght(sndLys, effLyPress.getBottomPress())) > 0.0) {
            stp = 0;
        }
        if (stp < 0) {
            stp = 0;
        }
        return stp;

    }

    public static float sigtorn_fixed(List<NcSoundingLayer> sndLys, float stdir, float stspd, Parcel sfcParcel)
    /***************************************************************/
    /*                                                             */
    /* Significant Tornado Parameter (fixed layer) */
    /* Adapted to sbCAPE and fixed layer shear terms */
    /* Input: stdir and stspd are the storm wind direction and */
    /* speed of the return from bunkers_storm_motion() */
    /* Output: Significant Tornado Parameter */
    /***************************************************************/
    {
        /* sbCAPE and LCL */
        // original code calls define_parcel(1, 0); and then calls parcel()
        // that means defines parcel as obs surface parcel with pressure at
        // 0 mb,
        // this is the same as the default parcel value already computed
        // first thing at NsharpLibManager.computeWeatherParameters() when
        // sounding is set.
        // Therefore, just get it from NsharpLibManager
        if (sfcParcel == null) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        float sbcp = sfcParcel.getBplus();
        if (!NsharpLibBasics.qc(sbcp)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }

        float lclh = NsharpLibBasics.agl(sndLys, NsharpLibBasics.i_hght(sndLys, sfcParcel.getLclpres()));

        /* 0-6 km bulk shear and 0-1 km SRH */
        float sfcPress = NsharpLibBasics.sfcPressure(sndLys);
        WindComponent shearWindComp = NsharpLibWinds.wind_shear(sndLys, sfcPress,
                NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.msl(sndLys, 6000f)));
        float shr6 = shearWindComp.getWspd();
        Helicity helicity = NsharpLibWinds.helicity(sndLys, 0, 1000, stdir, stspd);
        float srh1 = helicity.getTotalHelicity();
        if (sbcp < 0) {
            sbcp = 0;
        }
        if (srh1 < 0) {
            srh1 = 0;
        }
        if (lclh > 2000) {
            lclh = 0;
        } else {
            if (lclh <= 1000) {
                lclh = 1;
            } else {
                lclh = ((2000 - lclh) / 1000);
            }
        }
        if (shr6 < 25) {
            shr6 = 0;
        } else {
            if (shr6 >= 60) {
                shr6 = 1.5f;
            } else {
                shr6 = (shr6 / 40);
            }
        }
        float stpf = (shr6) * (srh1 / 150) * (sbcp / 1500) * (lclh);
        if (stpf < 0) {
            stpf = 0;
        }

        return stpf;
    }

    public static float sig_hail(List<NcSoundingLayer> sndLys, float lr75, float t500, float shear6k, float fzlh,
            Parcel mlParcel, Parcel muParcel)
    /***************************************************************
     * Significant Hail Parameter Input:
     *
     * @param lr75
     *            : 700-500mb lapse rate
     * @param t500
     *            : temperature in C at 500mb
     * @param shear6k
     *            : 0-6 km bulk shear in mps
     * @param fzlh
     *            : freezing agl height in ft
     * @param mlParcel
     *            : mixing layer parcel
     * @param muParcel
     *            : most unstable parcel
     * @return: Significant Hail Parameter
     *
     *          Note:: This method is ported from parameterization.c
     ***************************************************************/
    {
        if (muParcel == null || mlParcel == null) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        // mucape: most unstable parcel cape (bplus)
        float mucape = muParcel.getBplus();
        // mucin: most unstable parcel cinh (bminus)
        float mucin = muParcel.getBminus();
        // mumixr: most unstable parcel mix ratio
        float mumixr = muParcel.getMixRatio();
        // mlacape: mixing layer parcel cape (bplus)
        float mlcape = mlParcel.getBplus();
        if (shear6k > 27) {
            shear6k = 27;
        }
        if (shear6k < 7) {
            shear6k = 7;
        }

        if (mumixr > 13.6) {
            mumixr = 13.6f;
        }
        if (mumixr < 11) {
            mumixr = 11;
        }

        if (t500 > -5.5) {
            t500 = -5.5f;
        }
        EffectiveLayerPressures effLyPress = effective_inflow_layer(sndLys, 100, -250, muParcel);
        float base = NsharpLibBasics.agl(sndLys, NsharpLibBasics.i_hght(sndLys, effLyPress.getBottomPress()));
        if (base == 0) {
            if ((mlcape / mucape) < 0.3) {
                mucape = mlcape / 0.3f;
            }
        }

        float sighail = ((mucape * mumixr * lr75 * (t500 * -1) * shear6k) / 42000000);

        if (mucin < -200) {
            sighail = 0;
        }
        if (mucape < 750) {
            sighail = 0;
        }
        if (lr75 < 5.3) {
            sighail = 0;
        }
        if (t500 < -23.0) {
            sighail = 0;
        }
        if (fzlh < 2100) {
            sighail = 0;
        }
        // change from the following original line after confirmed by Rich
        // T. 10/19/2012
        // if (mucape < 1300) sighail*(mucape / 1300);
        if (mucape < 1300) {
            sighail = sighail * (mucape / 1300);
        }
        if (lr75 < 5.8) {
            sighail = sighail * (lr75 / 5.8f);
        }
        if (fzlh < 2400) {
            sighail = sighail * (fzlh / 2400);
        }

        return sighail;
    }

    public static float sigtorn_lr(List<NcSoundingLayer> sndLys, float stdir, float stspd, Parcel muParcel,
            Parcel mmParcel, EffectiveLayerPressures effPress)
    /***************************************************************/
    /*                                                             */
    /* Significant Tornado Parameter (effective layer) */
    /* modified by 0-500 m lapse rates (reduced for LR < 5.0) */
    /* Rich Thompson SPC OUN */
    /* Ported from bigSharp9.3-2017feb27 xwvid2.c */
    /*                                                             */
    /***************************************************************/
    {
        float eshear, esrh, mlcape, lclh, stp_lr, el, depth, base, cape;

        if (muParcel == null) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        cape = muParcel.getBplus();
        el = muParcel.getElAgl();

        base = NsharpLibBasics.agl(sndLys, NsharpLibBasics.i_hght(sndLys, effPress.getBottomPress()));

        depth = (el - base);

        WindComponent windS = NsharpLibWinds.wind_shear(sndLys, effPress.getBottomPress(),
                NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.msl(sndLys, base + (depth * 0.5f))));
        eshear = windS.getWspd();

        if ((cape >= 100) && (el < 0)) {
            windS = NsharpLibWinds.wind_shear(sndLys, NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.msl(sndLys, 0)),
                    NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.msl(sndLys, 6000)));
            eshear = windS.getWspd();
        }

        Helicity hel = NsharpLibWinds.helicity(sndLys, base,
                NsharpLibBasics.agl(sndLys, NsharpLibBasics.i_hght(sndLys, effPress.getTopPress())), stdir, stspd);
        esrh = hel.getTotalHelicity();

        mlcape = mmParcel.getBplus();

        if (!NsharpLibBasics.qc(mlcape)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }

        lclh = mmParcel.getLclAgl();

        if (lclh > 2000) {
            lclh = 0;
        } else {
            if (lclh <= 1000) {
                lclh = 1;
            } else {
                lclh = ((2000 - lclh) / 1000);
            }
        }
        if (eshear < 25) {
            eshear = 0;
        } else {
            if (eshear >= 60) {
                eshear = 1.5f;
            } else {
                eshear = (eshear / 40);
            }
        }
        if (esrh < -9998) {
            esrh = 0;
        }

        stp_lr = (eshear) * (esrh / 150) * (mlcape / 1500) * (lclh);

        float lr = NsharpLibSkparams.lapse_rate(sndLys, NsharpLibBasics.sfcPressure(sndLys),
                NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.sfcHeight(sndLys) + 500));

        if (lr < 5.0) {
            stp_lr = stp_lr * (lr / 5.0f);
        }
        if (NsharpLibBasics.agl(sndLys, NsharpLibBasics.i_hght(sndLys, effPress.getBottomPress())) > 0.0) {
            stp_lr = 0;
        }
        if (stp_lr < 0) {
            stp_lr = 0;
        }

        return stp_lr;
    }

    public static int ww_type(List<NcSoundingLayer> sndLys, Parcel mlParcel, Parcel muParcel, Parcel sbParcel,
            float stdir, float stspd)
    /********************************************************************
     * Watch type guidance A decision tree to help with ww issuance Modified
     * according to BigNsharpV2013-06-12 by Rich Thompson SPC. Ported from
     * xwvid5.c and modified for CAVE
     *
     * @param sndLys
     *            : sounding layers
     * @param mlParcel
     *            : mixing layer parcel
     * @param muParcel
     *            : most unstable parcel
     * @param sbParcel
     *            : observed surface parcel
     * @param stdir
     *            : stdir is the storm wind direction of the return of
     *            bunkers_storm_motion()
     * @param stspd
     *            : stspd is the storm wind speed of the return of
     *            bunkers_storm_motion()
     * @return - wwType, 0: NONE, color 19 Gold 1: MRGL SVR, color 26 Sky Blue
     *         2: SVR, color 6 Cyan 3: MRGL TOR, color 2 red 4: TOR, color 2 red
     *         5: PDS TOR, color 7 Magenta
     ********************************************************************/
    {
        if (mlParcel == null || muParcel == null || sbParcel == null || !NsharpLibBasics.qc(stdir)
                || !NsharpLibBasics.qc(stspd)) {
            return 0;
        }
        /* sb parcel */
        float sblcl = sbParcel.getLclAgl();
        float sig_tor_winter = sigtorn_fixed(sndLys, stdir, stspd, sbParcel);

        /* ml parcel */
        float mlcn = mlParcel.getBminus();
        float mlcp = mlParcel.getBplus();
        float mllcl = mlParcel.getLclAgl();
        float sig_tor = sigtorn_cin(sndLys, stdir, stspd, muParcel, mlParcel);

        /* mu parcel */
        float mucn = muParcel.getBminus();

        LayerParameters layerParms = new LayerParameters();
        float dncp = NsharpLibSkparams.dcape(sndLys, layerParms);

        float surfacePressure = NsharpLibBasics.sfcPressure(sndLys);
        /* sighail ingredients */
        float lapseRate700To500 = NsharpLibSkparams.lapse_rate(sndLys, 700, 500);
        WindComponent windComp6k = NsharpLibWinds.wind_shear(sndLys, surfacePressure,
                NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.msl(sndLys, 6000)));
        float shr6kSpd = windComp6k.getWspd();

        WindComponent windComp8k = NsharpLibWinds.wind_shear(sndLys, surfacePressure,
                NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.msl(sndLys, 8000)));
        float shr8kSpd = windComp8k.getWspd();
        float temp500m = NsharpLibBasics.i_temp(sndLys, 500);
        float fzlh = NsharpLibBasics.mtof(
                NsharpLibBasics.agl(sndLys, NsharpLibBasics.i_hght(sndLys, NsharpLibSkparams.temp_lvl(sndLys, 0))));
        float sighail = sig_hail(sndLys, lapseRate700To500, temp500m, NsharpLibBasics.kt_to_mps(shr6kSpd), fzlh,
                mlParcel, muParcel);

        float rm_scp = scp(sndLys, stdir, stspd, muParcel);
        float wind_dmg = damaging_wind(sndLys, mlParcel);

        WindComponent srwindComp4To6k = NsharpLibWinds.sr_wind(sndLys,
                NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.msl(sndLys, 4000)),
                NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.msl(sndLys, 6000)), stdir, stspd);

        float sr4kTo6kSpd = srwindComp4To6k.getWspd();
        float srhelicity1k = NsharpLibWinds.helicity(sndLys, 0, 1000, stdir, stspd).getTotalHelicity();
        EffectiveLayerPressures effLyPress = NsharpLibXwvid.effective_inflow_layer(sndLys, 100, -250, muParcel);
        float bot = NsharpLibBasics.agl(sndLys, NsharpLibBasics.i_hght(sndLys, effLyPress.getBottomPress()));
        float top = NsharpLibBasics.agl(sndLys, NsharpLibBasics.i_hght(sndLys, effLyPress.getTopPress()));
        float esrhelicity = NsharpLibWinds.helicity(sndLys, bot, top, stdir, stspd).getTotalHelicity();

        float lpaseRate1k = NsharpLibSkparams.lapse_rate(sndLys, surfacePressure,
                NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.sfc(sndLys).getGeoHeight() + 1000));

        float midrh = NsharpLibSkparams.mean_relhum(sndLys, surfacePressure - 150, surfacePressure - 350);
        float lowrh = NsharpLibSkparams.mean_relhum(sndLys, -1.0F, surfacePressure - 150);

        float low_mid_rh = ((lowrh + midrh) / 2);
        float mmp = NsharpLibSkparams.coniglio1(sndLys);
        float cbsig = (mlcp * NsharpLibBasics.kt_to_mps(shr6kSpd));

        // Decision tree below is identical to the operational "ww_type" flow
        // chart documentation 9/23/09 by RLT
        int ww_choice;
        if ((sig_tor >= 3.0) && (sig_tor_winter >= 4.0) && (srhelicity1k >= 200) && (esrhelicity >= 200)
                && (sr4kTo6kSpd >= 15.0) && (shr8kSpd >= 45.0) && (sblcl < 1000) && (mllcl < 1200)
                && (lpaseRate1k >= 5.0) && (mlcn > -50.0) && (bot == 0.0)) {
            ww_choice = 5;
        } else if (((sig_tor >= 3.0) || (sig_tor_winter >= 4.0)) && (mlcn > -125.0) && (bot == 0.0)) {
            ww_choice = 4;
        } else if (((sig_tor >= 1.0) || (sig_tor_winter >= 1.0)) && ((sr4kTo6kSpd >= 15.0) || (shr8kSpd >= 40.0))
                && (mlcn > -75.0) && (bot == 0.0)) {
            ww_choice = 4;
        } else if (((sig_tor >= 1.0) || (sig_tor_winter >= 1.0)) && (low_mid_rh >= 60) && (lpaseRate1k >= 5.0)
                && (mlcn > -50.0) && (bot == 0.0)) {
            ww_choice = 4;
        } else if (((sig_tor >= 1.0) || (sig_tor_winter >= 1.0)) && (mlcn > -150.0) && (bot == 0.0)) {
            ww_choice = 3;
        } else if ((((sig_tor >= 0.5) && (esrhelicity >= 150)) || ((sig_tor_winter >= 0.5) && (srhelicity1k >= 150)))
                && (mlcn > -50.0) && (bot == 0.0)) {
            ww_choice = 3;
        } else if (((sig_tor_winter >= 1.0) || (rm_scp >= 4.0) || (sig_tor >= 1.0)) && (mucn >= -50.0)) {
            ww_choice = 2;
        } else if ((rm_scp >= 2.0) && ((sighail >= 1.0) || (dncp >= 750)) && (mucn >= -50.0)) {
            ww_choice = 2;
        } else if ((cbsig >= 30000) && (mmp >= 0.6) && (mucn >= -50.0)) {
            ww_choice = 2;
        } else if ((mucn >= -75.0) && ((wind_dmg >= 0.5) || (sighail >= 0.5) || (rm_scp >= 0.5))) {
            ww_choice = 1;
        } else {
            ww_choice = 0;
        }
        return ww_choice;

    }

    public static LapseRateMax lapse_rate_max(List<NcSoundingLayer> sndLys, float bot, float top, float depth)
    /*************************************************************
     * /* Convert from /* LAPSE_RATE_MAX of xwvid1.c /* original author: Rich
     * Thompson SPC OUN /* /* Calculates the max lapse rate (C/km) in the
     * specified /* layer depth, from the layer bottom and top /* Value is
     * returned both as (param) and as a RETURN. /* /* bot = bottom of sounding
     * layer (m AGL) /* top = top of sounding layer (m AGL) /* depth = depth of
     * lapse rate calculation (m) /* Lapse rate returned in C/km /* Ported from
     * bigSharp9.3-2017feb27
     */
    /*************************************************************/
    {
        float maxlr = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        float maxlrHeight = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

        LapseRateMax lrm = new LapseRateMax();

        /*
         * loop through layers every 250 m, beginning at bot AGL , up to "depth"
         * layer
         */
        for (float z = bot; z <= (top - depth); z += 250) {
            float lr = NsharpLibSkparams.lapse_rate(sndLys,
                    NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.msl(sndLys, z)),
                    NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.msl(sndLys, z + depth)));
            if (lr > maxlr) {
                maxlr = lr;
                maxlrHeight = z;
            }
        }
        lrm.setLrm(maxlr);
        lrm.setLrmHeight(maxlrHeight);
        if (maxlr < 6.0) {
            lrm.setColor(8); // Gempak color Brown
        } else if (maxlr < 6.5) {
            lrm.setColor(18); // Gempak color Dk Orange
        } else if (maxlr < 7.5) {
            lrm.setColor(19); // Gempak color Gold
        } else if (maxlr < 8.5) {
            lrm.setColor(2); // Gempak color Red
        } else {
            lrm.setColor(7); // set default to 7 = Gempak color Magenta
        }

        return lrm;
    }

    public static float large_hail_param(List<NcSoundingLayer> sndLys, float st_dir, float st_spd)
    /********************************************************************
     * /* Large Hail Parameter /* Developed by Johnson and Sugden (2014) /* /*
     * Ported from bigSharp9.3-2017feb27 xwvid2.c /* Input; current storm
     * direction and speed /* Return: large hail parameter /
     ********************************************************************/
    {
        float mucp, lr75, shr6, hgz_depth, shrel, grwel, sr_diff, lhp;
        float el, elPress, terma, termb;
        LParcelValues lparcelVs = NsharpLibSkparams.define_parcel(sndLys, NsharpLibSndglib.PARCELTYPE_MOST_UNSTABLE,
                500);
        Parcel parcel = NsharpLibSkparams.parcel(sndLys, -1.0F, -1.0F, lparcelVs.getPres(), lparcelVs.getTemp(),
                lparcelVs.getDwpt());
        if (parcel == null) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        mucp = parcel.getBplus();
        // get el agl in meter
        el = NsharpLibBasics.ftom(parcel.getElAgl());
        elPress = parcel.getElpres();
        WindComponent shearWindComp = NsharpLibWinds.wind_shear(sndLys, NsharpLibBasics.sfcPressure(sndLys),
                NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.msl(sndLys, 6000.0f)));

        shr6 = shearWindComp.getWspd() * .514f;

        lr75 = NsharpLibSkparams.lapse_rate(sndLys, 700.0F, 500.0F);

        /*
         * DIR difference between mean EL to EL-1.5km wind and mean 3-6 km wind
         */
        /* no pressure weighting in mean wind calculations */
        WindComponent meanWndComp36 = NsharpLibWinds.mean_wind_npw(sndLys,
                NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.msl(sndLys, 3000.0f)),
                NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.msl(sndLys, 6000.0f)), 0);

        WindComponent meanWndCompEl = NsharpLibWinds.mean_wind_npw(sndLys,
                NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.msl(sndLys, el - 1500.0f)), elPress, 0);

        grwel = meanWndCompEl.getWdir() - meanWndComp36.getWdir(); // direl -
                                                                   // dir36;
        if (grwel >= 180.0) {
            grwel = -10.0f;
        }

        /* DIR difference between 3-6 km and 0-1 km SR winds */
        WindComponent wndStmCompSfc = NsharpLibWinds.sr_wind(sndLys, NsharpLibBasics.sfcPressure(sndLys),
                NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.msl(sndLys, 1000.0f)), st_dir, st_spd);
        WindComponent wndStmComp36 = NsharpLibWinds.sr_wind(sndLys,
                NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.msl(sndLys, 3000.0f)),
                NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.msl(sndLys, 6000.0f)), st_dir, st_spd);

        sr_diff = wndStmComp36.getWdir() - wndStmCompSfc.getWdir();

        /* BWD between surface wind and mean wind EL to EL-750m */
        WindComponent wndShearElComp = NsharpLibWinds.wind_shear(sndLys, NsharpLibBasics.sfcPressure(sndLys),
                NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.msl(sndLys, el - 750)));
        shrel = wndShearElComp.getWspd() * .514f;

        hgz_depth = NsharpLibBasics.agl(sndLys, NsharpLibBasics.i_hght(sndLys, NsharpLibSkparams.temp_lvl(sndLys, -30)))
                - NsharpLibBasics.agl(sndLys, NsharpLibBasics.i_hght(sndLys, NsharpLibSkparams.temp_lvl(sndLys, -10)));

        /* check for weak CAPE or sub-supercell vertical shear */
        if ((mucp < 400.0) || (shr6 < 14.0)) {
            lhp = 0;
        } else {
            terma = (((mucp - 2000) / 1000) + ((3200 - hgz_depth) / 500) + ((lr75 - 6.5f) / 2));
            termb = (((shrel - 25) / 5) + ((grwel + 5) / 20) + ((sr_diff - 80) / 10));
            lhp = (terma * termb) + 5;
            if (((terma < 0.0) && (termb < 0.0)) || (lhp < 0)) {
                lhp = 0;
            }
        }
        return lhp;
    }

    public static float moshe(List<NcSoundingLayer> sndLys, Parcel muParcel, EffectiveLayerPressures effLyPress)
    /********************************************************************/
    /* Modified SHERBE */
    /* based on work by Sherburn et al.(2016) */
    /* (normalized product of 0-3 km LR, 0-1.5 km bulk shear, */
    /* EBWD, and omega*theta-e change with height) */
    /* Ported from bigSharp9.3-2017feb27 xwvid2.c */
    /********************************************************************/

    {
        float lllr, ebwd, shr15, omega_thetae, moshe, el, base, depth;

        if (muParcel == null) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        // get el agl in meter
        el = NsharpLibBasics.ftom(muParcel.getElAgl());
        base = NsharpLibBasics.agl(sndLys, NsharpLibBasics.i_hght(sndLys, effLyPress.getBottomPress()));
        depth = (el - base);

        WindComponent windShearBP = NsharpLibWinds.wind_shear(sndLys, effLyPress.getBottomPress(),
                NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.msl(sndLys, base + (depth * 0.5f))));
        ebwd = (((windShearBP.getWspd() * .514f) - 8) / 10);
        if (ebwd < 0) {
            ebwd = 0;
        }

        float lapsedRate = NsharpLibSkparams.lapse_rate(sndLys, NsharpLibBasics.sfcPressure(sndLys),
                NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.sfcHeight(sndLys) + 3000));
        if (lapsedRate < 4) {
            lapsedRate = 4;
        }
        lllr = (float) (Math.pow((lapsedRate - 4), 2) / 4);

        WindComponent windShearSfc1500 = NsharpLibWinds.wind_shear(sndLys, NsharpLibBasics.sfcPressure(sndLys),
                NsharpLibBasics.i_pres(sndLys, NsharpLibBasics.sfcHeight(sndLys) + 1500));
        shr15 = (((windShearSfc1500.getWspd() * .514f) - 8) / 10);
        if (shr15 < 0) {
            shr15 = 0;
        }

        float maxtev = NsharpLibSkparams.maxtevv(sndLys);
        if (maxtev < 0) {
            maxtev = 0;
        }
        omega_thetae = ((maxtev + 10) / 9);

        moshe = (ebwd * lllr * shr15 * omega_thetae);
        if (moshe < 0) {
            moshe = 0;
        }
        return moshe;
    }
}
