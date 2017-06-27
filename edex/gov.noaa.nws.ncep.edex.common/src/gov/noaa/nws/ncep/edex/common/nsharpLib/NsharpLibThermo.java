package gov.noaa.nws.ncep.edex.common.nsharpLib;

/**
 * 
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 * All methods developed in this class are based on the
 * algorithm developed in BigSharp native C file, thermo.c , by John A.
 * Hart/SPC. All methods name are defined with same name as the C
 * function name defined in native code. 
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#     Engineer    Description
 * -------      -------     --------    -----------
 * 02/25/2016   RM#15923    Chin Chen   NSHARP - Native Code replacement Phase 1&2
 * 
 * </pre>
 * 
 * @author Chin Chen 
 * @version 1.0
 * 
 */

import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.LayerParameters;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.Parcel;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;

import java.util.List;

public class NsharpLibThermo {

    public static float temp_at_mixrat(float mr, float pres)
    /*************************************************************/
    /*                                                           */
    /* Returns the temperature(c) of air at the given */
    /* mixing ratio (mr, g/kg) and pressure (pres, mb). */
    /*                                                           */
    /*************************************************************/
    {
        double c1, c2, c3, c4, c5, c6, x, tmrk;
        if (!NsharpLibBasics.qc(mr) || !NsharpLibBasics.qc(pres)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }

        c1 = 0.0498646455;
        c2 = 2.4082965;
        c3 = 7.07475;
        c4 = 38.9114;
        c5 = 0.0915;
        c6 = 1.2035;

        x = Math.log10(mr * pres / (622.0 + mr));
        tmrk = Math.pow(10.0, c1 * x + c2) - c3 + c4
                * Math.pow(Math.pow(10.0, c5 * x) - c6, 2.0);
        return (float) (tmrk - 273.15);
    }

    public static float mixratio(float pres, float temp)
    /*************************************************************/
    /*                                                           */
    /* Returns the mixing ratio (g/kg) of the */
    /* parcel(pres, temp). */
    /*                                                           */
    /* pres - Pressure of parcel(mb) */
    /* temp - Temperature of parcel (c) */
    /*************************************************************/
    {
        float x, wfw, fwesw;
        if (!NsharpLibBasics.qc(pres) || !NsharpLibBasics.qc(temp)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }

        x = 0.02f * (temp - 12.5f + 7500.0f / pres);
        wfw = 1.0f + 0.0000045f * pres + 0.0014f * x * x;
        fwesw = wfw * vappres(temp);
        return 621.97f * (fwesw / (pres - fwesw));
    }

    public static float vappres(float temp)
    /*************************************************************/
    /*                                                           */
    /* Returns the vapor pressure of dry air at */
    /* temperature (temp). */
    /*                                                           */
    /* temp - Temperature of parcel (c) */
    /*************************************************************/
    {
        double pol;
        if (!NsharpLibBasics.qc(temp)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }

        pol = temp * (1.1112018e-17 + temp * (-3.0994571e-20));
        pol = temp * (2.1874425e-13 + temp * (-1.789232e-15 + pol));
        pol = temp * (4.3884180e-09 + temp * (-2.988388e-11 + pol));
        pol = temp * (7.8736169e-05 + temp * (-6.111796e-07 + pol));
        pol = .99999683e-00 + temp * (-9.082695e-03 + pol);
        pol = (pol * pol);
        pol = (pol * pol);
        return 6.1078f / (float) (pol * pol);
    }

    public static LayerParameters drylift(float pres1, float temp1, float dwpt1)
    /*************************************************************/
    /* Lifts a parcel(p1,t1,td1) to the LCL and returns its */
    /* new level(p2) and temperature(t2). */
    /* Inputs: */
    /* pres1 - Pressure of initial parcel(mb) */
    /* temp1 - Temperature of initial parcel (c) */
    /* dwpt1 - Dew Point of initial parcel (c) */
    /*                                                           */
    /* Return: temperature, pressure in NcSoundingLayer structure */
    /* rtnPress - Pressure at LCL (mb) */
    /* rtnTemp - Temperature at LCL (c) */
    /************************************************************/
    {
        if (!NsharpLibBasics.qc(pres1) || !NsharpLibBasics.qc(temp1)
                || !NsharpLibBasics.qc(dwpt1)) {
            return null;
        }
        LayerParameters returnLayer = new LayerParameters();

        float rtnTemp = lcltemp(temp1, dwpt1);
        float rtnPress = thalvl(theta(pres1, temp1, 1000), rtnTemp);
        returnLayer.setTemperature(rtnTemp);
        returnLayer.setPressure(rtnPress);
        return returnLayer;
    }

    public static float wetlift(float pres, float temp, float pres2)
    /*************************************************************/
    /*                                                           */
    /* Lifts a parcel(pres,temp) moist adiabatically to its */
    /* new level(pres2), and returns the new level temperature(c) */
    /*                                                           */
    /* pres - Pressure of initial parcel(mb) */
    /* temp - Temperature of initial parcel (c) */
    /* pres2 - new level Pressure (mb) */
    /*************************************************************/
    {
        float tha, thm, woth, wott;
        if (!NsharpLibBasics.qc(temp) || !NsharpLibBasics.qc(pres)
                || !NsharpLibBasics.qc(pres2)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }

        tha = theta(pres, temp, 1000.0f);
        woth = wobf(tha);
        wott = wobf(temp);
        thm = tha - woth + wott;
        return satlft(pres2, thm);
    }

    public static float lcltemp(float temp, float dwpt)
    /*************************************************************/
    /* Returns the temperature(c) of a parcel (temp,dwpt) */
    /* when raised to its LCL. */
    /*                                                           */
    /* temp - Temperature of initial parcel (c) */
    /* dwpt - Dew Point of initial parcel (c) */
    /*************************************************************/
    {
        float s, dlt;
        if (!NsharpLibBasics.qc(temp) || !NsharpLibBasics.qc(dwpt)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }

        s = temp - dwpt;
        dlt = s
                * (1.2185f + .001278f * temp + s
                        * (-.00219f + 1.173E-05f * s - .0000052f * temp));
        return temp - dlt;
    }

    public static float thalvl(float thta, float temp)
    /*************************************************************/
    /*                                                           */
    /* Returns the level(mb) of a parcel( thta, temp). */
    /*                                                           */
    /* thta - Potential Temperature of parcel(c) */
    /* temp - Temperature of parcel (c) */
    /*************************************************************/
    {
        if (!NsharpLibBasics.qc(temp) || !NsharpLibBasics.qc(thta)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        temp = temp + 273.15f;
        thta = thta + 273.15f;
        return 1000.0f / (float) Math.pow((thta / temp),
                (1 / NsharpLibSndglib.ROCP));
    }

    public static float theta(float pres, float temp, float pres2)
    /*************************************************************/
    /* Returns the potential temperature(c) of the */
    /* parcel(pres, temp). */
    /*                                                           */
    /* pres - Pressure of parcel(mb) */
    /* temp - Temperature of parcel (c) */
    /* pres2 - Reference Level (Usually 1000mb) */
    /*************************************************************/
    {
        if (!NsharpLibBasics.qc(temp) || !NsharpLibBasics.qc(pres)
                || !NsharpLibBasics.qc(pres2)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }

        temp = temp + 273.15f;
        return (temp * (float) Math.pow(pres2 / pres, NsharpLibSndglib.ROCP)) - 273.15f;
    }

    public static float thetae(float pres, float temp, float dwpt)
    /*************************************************************/
    /* THETAE */
    /*                                                           */
    /* Calculates the equivalent potential temperature (C) */
    /* for given parcel (pres, temp, dwpt). */
    /*                                                           */
    /* pres = Pressure of parcel (mb). */
    /* temp = Temperature of parcel (c) */
    /* dwpt = Dew point of parcel (c) */
    /*************************************************************/
    {
        if (!NsharpLibBasics.qc(temp) || !NsharpLibBasics.qc(pres)
                || !NsharpLibBasics.qc(dwpt)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }

        LayerParameters layerParm = drylift(pres, temp, dwpt);
        float newTemp = wetlift(layerParm.getPressure(),
                layerParm.getTemperature(), 100f);
        return theta(100.0f, newTemp, 1000.0f);
    }

    public static float thetaw(float pres, float temp, float dwpt)
    /*************************************************************/
    /* THETAW */
    /*                                                           */
    /* Calculates the wet-bulb potential temperature for given */
    /* parcel (pres, temp, dwpt). */
    /*                                                           */
    /* pres = Pressure of parcel (mb). */
    /* temp = Temperature of parcel (c) */
    /* dwpt = Dew point of parcel (c) */
    /*************************************************************/
    {
        if (!NsharpLibBasics.qc(pres) || !NsharpLibBasics.qc(temp)
                || !NsharpLibBasics.qc(dwpt)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        LayerParameters dryLayer = drylift(pres, temp, dwpt);
        return wetlift(dryLayer.getPressure(), dryLayer.getTemperature(),
                1000.0f);
    }

    public static float wobf(float temp)
    /*************************************************************/
    /* wbof */
    /* temp - temperature of parcel (c) */
    /*************************************************************/
    {
        float x;
        double pol;
        if (!NsharpLibBasics.qc(temp)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }

        x = temp - 20.0f;
        if (x <= 0.0) {
            pol = 1.0
                    + x
                    * (-8.841660499999999e-03 + x
                            * (1.4714143e-04 + x
                                    * (-9.671989000000001e-07 + x
                                            * (-3.2607217e-08 + x
                                                    * (-3.8598073e-10)))));
            pol = pol * pol;
            return 15.13f / (float) (pol * pol);
        } else {
            pol = x
                    * (4.9618922e-07 + x
                            * (-6.1059365e-09 + x
                                    * (3.9401551e-11 + x
                                            * (-1.2588129e-13 + x * (1.6688280e-16)))));
            pol = 1.0 + x * (3.6182989e-03 + x * (-1.3603273e-05 + pol));
            pol = pol * pol;
            return 29.93f / (float) (pol * pol) + 0.96f * x - 14.8f;
        }
    }

    public static float satlft(float pres, float thm)
    /*************************************************************/
    /* Returns the temperature (c) of a parcel (thm), */
    /* when lifted to level (pres). */
    /*                                                           */
    /* pres - Pressure to raise parcel (mb) */
    /* thm - Sat. Pot. Temperature of parcel (c) */
    /*************************************************************/
    {
        float pwrp, t1, t2, woto, wotm, wot2, woe2, e1, e2, rate, eor;
        if (!NsharpLibBasics.qc(pres) || !NsharpLibBasics.qc(thm)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }

        if ((Math.abs(pres - 1000) - 0.001) <= 0.0) {
            return thm;
        }

        eor = 999;
        pwrp = (float) (Math.pow(pres / 1000.0, NsharpLibSndglib.ROCP));
        t1 = (thm + 273.15f) * pwrp - 273.15f;
        woto = wobf(t1);
        wotm = wobf(thm);
        e1 = woto - wotm;
        rate = 1;
        t2 = t1 - e1 * rate;
        e2 = (t2 + 273.15f) / pwrp - 273.15f;
        wot2 = wobf(t2);
        woe2 = wobf(e2);
        e2 = e2 + wot2 - woe2 - thm;
        eor = e2 * rate;
        while (Math.abs(eor) - 0.1 > 0.0) {
            rate = (t2 - t1) / (e2 - e1);
            t1 = t2;
            e1 = e2;
            t2 = t1 - e1 * rate;
            e2 = (t2 + 273.15f) / pwrp - 273.15f;
            wot2 = wobf(t2);
            woe2 = wobf(e2);
            e2 = e2 + wot2 - woe2 - thm;
            eor = e2 * rate;
        }
        return t2 - eor;
    }

    public static float virtemp(float pres, float temp, float dwpt)
    /*************************************************************/
    /* VIRTEMP */
    /* Calculates the virtual temperature (C) of parcel */
    /* (pres, temp, dwpt). */
    /*                                                           */
    /* pres - Level(mb) to compute a virtual temp. */
    /* temp - Temperature(c). */
    /* dwpt - Dew point(c). */
    /*************************************************************/
    {
        double cta, eps, tk, w;
        if (!NsharpLibBasics.qc(dwpt))
            return temp;

        if (!NsharpLibBasics.qc(pres) || !NsharpLibBasics.qc(temp))
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

        cta = 273.15;
        eps = 0.62197;

        tk = temp + cta;
        w = 0.001 * (double) mixratio(pres, dwpt);
        return (float) (tk * (1.0 + w / eps) / (1.0 + w) - cta);
    }

    public static float wetbulb(float pres, float temp, float dwpt)
    /*************************************************************/
    /* WETBULB */
    /* Calculates the wetbulb temperature (c) for given */
    /* parcel (pres, temp, dwpt). */
    /*                                                           */
    /* pres = Pressure of parcel (mb). */
    /* temp = Temperature of parcel (c) */
    /* dwpt = Dew point of parcel (c) */
    /* Return: wetbulb tremperature in C */
    /*************************************************************/
    {
        if (!NsharpLibBasics.qc(dwpt) || !NsharpLibBasics.qc(pres)
                || !NsharpLibBasics.qc(temp)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        LayerParameters layerParms = drylift(pres, temp, dwpt);
        return (wetlift(layerParms.getPressure(), layerParms.getTemperature(),
                pres));
    }

    public static float esfc(List<NcSoundingLayer> sndLys, float val)
    /*************************************************************/
    /* ESFC */
    /*                                                           */
    /* Calculate effective surface for elevated convection. */
    /* Assumes that lowest layer with CAPE >= input val is "sfc" */
    /* Returns level (mb) of effective surface. */
    /*                                                           */
    /*************************************************************/
    {

        /* ----- Begin at surface and search upward for instability ----- */
        for (int i = NsharpLibBasics.sfcIndex(sndLys); i < sndLys.size(); i++) {
            /* Stop looking above 500mb */
            if (sndLys.get(i).getPressure() < 500.0f) {
                break;
            }
            Parcel parcel = NsharpLibSkparams
                    .parcel(sndLys, -1.0F, -1.0F, sndLys.get(i).getPressure(),
                            sndLys.get(i).getTemperature(), sndLys.get(i)
                                    .getDewpoint());
            if (parcel.getBplus() >= val) {
                return sndLys.get(i).getPressure();
            }
        }
        return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
    }

    public static float relh(List<NcSoundingLayer> sndLys, float pres)
    /*************************************************************
     * Relative Humidity
     * 
     * Calculates the RH at specified pressure level
     * 
     * @param sndLys
     *            - list of NcSounding layer
     * @param - pres at specified level (mb) [ -1=SFC]
     * @return - mean relative Humidity (%)
     *************************************************************/
    {
        float p, td, t;

        if (!NsharpLibBasics.qc(pres)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        if (pres == -1) {
            p = NsharpLibBasics.sfcPressure(sndLys);
            td = NsharpLibBasics.sfc(sndLys).getDewpoint();
            t = NsharpLibBasics.sfcTemperature(sndLys);
        } else if (pres > 0) {
            p = pres;
            td = NsharpLibBasics.i_dwpt(sndLys, pres);
            t = NsharpLibBasics.i_temp(sndLys, pres);
        } else {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }

        float rh = 100.0f * mixratio(p, td) / mixratio(p, t);

        return rh;
    }
}
