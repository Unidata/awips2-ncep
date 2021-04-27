package gov.noaa.nws.ncep.edex.common.nsharpLib;

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
 * 02/25/2016   RM#15923    Chin Chen   NSHARP - Native Code replacement Phase 1&2
 * 07/28/2017   RM#34795    Chin Chen   NSHARP - Updates for March 2017 bigSharp version
 *                                      - Added output for the "large hail parameter" and 
 *                                      the "modified SHERBE" parameter,..etc.
 * 09/1/2017   RM#34794    Chin Chen   NSHARP - Updates for March 2017 bigSharp version
 *                                      - Update the dendritic growth layer calculations and other skewT
 *                                      updates.
 *
 * </pre>
 * 
 * @author Chin Chen
 * @version 1.0
 * 
 */

import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;

import java.util.List;

public class NsharpLibBasics {

    private static final float NOT_POSSIBLE_DATA = 2.0E+05F;

    public static float mtof(float value)
    /*************************************************************/
    /* MTOF */
    /*                                                           */
    /* Converts given distance (m) to (ft). */
    /*                                                           */
    /*************************************************************/
    {
        if (!qc(value)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        } else {
            return (value / 0.3048f);
        }
    }

    public static float ftom(float value)
    /*************************************************************/
    /* FTOM */
    /*                                                           */
    /* Converts given distance (ft) to (m). */
    /*                                                           */
    /*************************************************************/
    {
        if (!qc(value)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        } else {
            return (value * .3048f);
        }
    }

    public static float ctof(float value)
    /*************************************************************/
    /* CTOF */
    /*                                                           */
    /* Converts given temperature (c) to (f). */
    /*************************************************************/
    {
        if (!qc(value)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        } else {
            return ((1.8f * value) + 32.0f);
        }
    }

    public static float kt_to_mps(float spd)
    /*************************************************************/
    /* KT_TO_MPS */
    /*                                                           */
    /* Converts speed (knots) to meters per second. */
    /*************************************************************/
    {
        if (!qc(spd)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        } else {
            return (spd * 0.51479f);
        }
    }

    public static float agl(List<NcSoundingLayer> sndLys, float height)
    /*************************************************************/
    /* AGL */
    /*                                                           */
    /* Converts height (meters) MSL to AGL. */
    /*************************************************************/
    {
        if (!qc(height)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        NcSoundingLayer sfcLayer = sfc(sndLys);
        if (sfcLayer == null || !qc(sfcLayer.getGeoHeight())) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        return (height - sfcLayer.getGeoHeight());
    }

    public static float msl(List<NcSoundingLayer> sndLys, float height)
    /*************************************************************/
    /* MSL */
    /*                                                           */
    /* Converts height (meters) AGL to MSL. */
    /*************************************************************/
    {
        if (!qc(height)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        NcSoundingLayer sfcLayer = sfc(sndLys);
        if (sfcLayer == null || !qc(sfcLayer.getGeoHeight())) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        return (height + sfcLayer.getGeoHeight());
    }

    public static NcSoundingLayer sfc(List<NcSoundingLayer> sndLys) {
        /*************************************************************/
        /* sfc */
        /* return a valid surface layer */
        /*************************************************************/
        for (int i = 0; i < sndLys.size(); i++) {
            if (sndLys.get(i).getTemperature() != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA)
                return sndLys.get(i);
        }
        return null;
    }

    public static int sfcIndex(List<NcSoundingLayer> sndLys) {
        /*************************************************************/
        /* sfcIndex */
        /* return a valid surface layer's index or -1 (invalid) */
        /*************************************************************/
        for (int i = 0; i < sndLys.size(); i++) {
            if (sndLys.get(i).getTemperature() != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA) {
                return i;
            }
        }
        return -1;
    }

    public static float sfcPressure(List<NcSoundingLayer> sndLys) {
        /*************************************************************/
        /* sfcPressure */
        /* return a valid surface layer's pressure */
        /*************************************************************/
        NcSoundingLayer sfcLy = sfc(sndLys);
        if (sfcLy != null) {
            return sfcLy.getPressure();
        } else {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
    }
    
    public static float sfcHeight(List<NcSoundingLayer> sndLys) {
        /*************************************************************/
        /* sfcHeight */
        /* return a valid surface layer's height */
        /*************************************************************/
        NcSoundingLayer sfcLy = sfc(sndLys);
        if (sfcLy != null) {
            return sfcLy.getGeoHeight();
        } else {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
    }

    public static float sfcTemperature(List<NcSoundingLayer> sndLys) {
        /*************************************************************/
        /* sfcTemperature */
        /* return a valid surface layer's temperature */
        /*************************************************************/
        NcSoundingLayer sfcLy = sfc(sndLys);
        if (sfcLy != null) {
            return sfcLy.getTemperature();
        } else {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
    }
    
    public static float sfcDewPoint(List<NcSoundingLayer> sndLys) {
        /*************************************************************/
        /* sfcDewpoint */
        /* return a valid surface layer's dew point */
        /*************************************************************/
        NcSoundingLayer sfcLy = sfc(sndLys);
        if (sfcLy != null) {
            return sfcLy.getDewpoint();
        } else {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
    }

    public static float i_pres(List<NcSoundingLayer> sndLys, float hght) {
        /*************************************************************/
        /* I_PRES */
        /*                                                           */
        /* Interpolates the given data to calculate a pressure(mb) */
        /* at height (hght). */
        /*                                                           */
        /* hght - Height(m) of level */
        /*************************************************************/
        if (!qc(hght)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        int belowIndex = 0, aboveIndex = 0;
        boolean ok = false;
        double nm1, nm2, nm3;

        /* ----- Find Pressure Immediately Above level ----- */
        for (int i = 0; i < sndLys.size(); i++) {
            if ((sndLys.get(i).getGeoHeight() >= hght)
                    && qc(sndLys.get(i).getPressure())) {
                aboveIndex = i;
                ok = true;
                break;
            }
        }
        if (!ok) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }

        /* ----- Find Pressure Below level ----- */
        ok = false;
        for (int i = sndLys.size() - 1; i >= 0; i--) {
            if ((sndLys.get(i).getGeoHeight() <= hght)
                    && qc(sndLys.get(i).getPressure())) {
                belowIndex = i;
                ok = true;
                break;
            }
        }
        if (!ok) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }

        /* ----- If both levels are the same, return them ---- */
        if (aboveIndex == belowIndex) {
            return sndLys.get(aboveIndex).getPressure();
        }

        /* ----- Now we need to interpolate to get the Pressure ----- */
        nm1 = hght - sndLys.get(belowIndex).getGeoHeight();
        nm2 = sndLys.get(aboveIndex).getGeoHeight()
                - sndLys.get(belowIndex).getGeoHeight();
        nm3 = Math.log(sndLys.get(aboveIndex).getPressure()
                / sndLys.get(belowIndex).getPressure());
        return (float) (sndLys.get(belowIndex).getPressure() * Math
                .exp((nm1 / nm2) * nm3));
    }

    public static float i_hght(List<NcSoundingLayer> sndLys, float pres) {
        /*************************************************************/
        /*                                                           */
        /* Interpolates the given data to calculate a height */
        /* at pressure level (pres). */
        /*                                                           */
        /* pres - Level(mb) to compute a Height */
        /*************************************************************/

        int below, above, i, ok;
        double nm1, nm2, nm4;
        if (!qc(pres)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        below = 0;
        above = 0;

        /* ----- Find Height Immediately Above level ----- */
        ok = 0;
        for (i = 0; i < sndLys.size(); i++) {
            if ((sndLys.get(i).getPressure() == pres)
                    && (sndLys.get(i).getGeoHeight() != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA)) {
                /* no need to interpolate if we have the level!!! */
                return sndLys.get(i).getGeoHeight();
            }
            if ((sndLys.get(i).getPressure() < pres)
                    && (sndLys.get(i).getGeoHeight() != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA)
                    && (sndLys.get(i).getPressure() != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA)) {
                above = i;
                ok = 1;
                break;
            }
        }
        if (ok == 0)
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

        /* ----- Find Height Immediately Below level ----- */
        ok = 0;
        for (i = sndLys.size() - 1; i > -1; i--) {
            if ((sndLys.get(i).getPressure() >= pres)
                    && (sndLys.get(i).getGeoHeight() != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA)) {
                below = i;
                ok = 1;
                break;
            }
        }
        if (ok == 0)
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

        /* ----- If both levels are the same, return them ---- */
        if (above == below) {
            return sndLys.get(above).getGeoHeight();
        }

        /* ----- Now we need to interpolate to get the height ----- */
        nm1 = sndLys.get(above).getGeoHeight()
                - sndLys.get(below).getGeoHeight();
        nm2 = Math.log(sndLys.get(below).getPressure()
                / sndLys.get(above).getPressure());
        nm4 = Math.log(sndLys.get(below).getPressure() / pres);
        return (float) (sndLys.get(below).getGeoHeight() + ((nm4 / nm2) * nm1));
    }

    public static float i_temp(List<NcSoundingLayer> sndLys, float pres) {
        /*************************************************************/
        /* Interpolates the given data to calculate a temperature. */
        /* at pressure level (pres). */
        /*                                                           */
        /* pres - Level(mb) to compute a temperature */
        /*************************************************************/

        int below, above, i, ok;
        double nm1, nm2, nm4;
        if (!qc(pres)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        below = 0;
        above = 0;

        /* ----- Find Temperature Immediately Above level ----- */
        ok = 0;
        for (i = 0; i < sndLys.size(); i++) {
            if ((sndLys.get(i).getPressure() == pres)
                    && (sndLys.get(i).getTemperature() != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA)) {
                /*
                 * if pressure level exists, and temp exists, no need to
                 * interpolate
                 */
                return sndLys.get(i).getTemperature();
            }
            if ((sndLys.get(i).getPressure() < pres)
                    && (sndLys.get(i).getTemperature() != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA)) {
                above = i;
                ok = 1;
                break;
            }
        }
        if (ok == 0) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }

        /* ----- Find Temperature Immediately Below level ----- */
        ok = 0;
        for (i = sndLys.size() - 1; i > -1; i--) {
            if ((sndLys.get(i).getPressure() >= pres)
                    && (sndLys.get(i).getTemperature() != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA)) {
                below = i;
                ok = 1;
                break;
            }
        }
        if (ok == 0) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }

        /* ----- If both levels are the same, return them ---- */
        if (above == below) {
            return sndLys.get(above).getTemperature();
        }

        /* ----- Now we need to interpolate to get the temperature ----- */

        nm1 = sndLys.get(above).getTemperature()
                - sndLys.get(below).getTemperature();
        nm2 = Math.log(sndLys.get(below).getPressure()
                / sndLys.get(above).getPressure());
        nm4 = Math.log(sndLys.get(below).getPressure() / pres);
        return (float) (sndLys.get(below).getTemperature() + ((nm4 / nm2) * nm1));
    }

    public static float i_dwpt(List<NcSoundingLayer> sndLys, float pres) {
        /*************************************************************/
        /*                                                           */
        /* Interpolates the given data to calculate a dew point */
        /* at pressure level (pres). */
        /*                                                           */
        /* pres - Level(mb) to compute a Dew Point */
        /*************************************************************/

        int below, above, i, ok;
        double nm1, nm2, nm4;
        if (!qc(pres)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        below = 0;
        above = 0;

        /* ----- Find Dew Point Immediately Above level ----- */
        ok = 0;
        for (i = 0; i < sndLys.size(); i++) {
            if ((sndLys.get(i).getPressure() == pres)
                    && (sndLys.get(i).getDewpoint() != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA)
                    && (sndLys.get(i).getPressure() != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA)) {
                /*
                 * no need to interpolate if we have the level and dewpoint
                 * exists
                 */
                return sndLys.get(i).getDewpoint();
            }
            if ((sndLys.get(i).getPressure() < pres)
                    && (sndLys.get(i).getDewpoint() != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA)) {
                above = i;
                ok = 1;
                break;
            }
        }
        if (ok == 0) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }

        /* ----- Find Dew Point Immediately Below level ----- */
        ok = 0;
        for (i = sndLys.size() - 1; i > -1; i--) {
            if ((sndLys.get(i).getPressure() >= pres)
                    && (sndLys.get(i).getDewpoint() != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA)) {
                below = i;
                ok = 1;
                break;
            }
        }
        if (ok == 0) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }

        /* ----- If both levels are the same, return them ---- */
        if (above == below) {
            return sndLys.get(above).getDewpoint();
        }

        /* ----- Now we need to interpolate to get the dew point ----- */
        nm1 = sndLys.get(above).getDewpoint() - sndLys.get(below).getDewpoint();
        nm2 = Math.log(sndLys.get(below).getPressure()
                / sndLys.get(above).getPressure());
        nm4 = Math.log(sndLys.get(below).getPressure() / pres);
        return (float) (sndLys.get(below).getDewpoint() + ((nm4 / nm2) * nm1));
    }

    public static float i_omeg(List<NcSoundingLayer> sndLys, float pres)
    /*************************************************************/
    /* I_OMEG */
    /*                                                           */
    /* Interpolates the given data to calculate a omega */
    /* at pressure level (pres). */
    /*                                                           */
    /* pres - Level(mb) to compute a omega */
    /*************************************************************/
    {
        int below, above, i, ok;
        double nm1, nm2, nm4;

        if (!qc(pres)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }

        below = 0;
        above = 0;

        /* ----- Find Immediately Above level ----- */

        ok = 0;
        for (i = 0; i < sndLys.size(); i++) {
            if ((sndLys.get(i).getPressure() == pres)
                    && (sndLys.get(i).getOmega() != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA)
                    && (sndLys.get(i).getPressure() != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA)) {
                /*
                 * no need to interpolate if we have the level and dewpoint
                 * exists
                 */
                return sndLys.get(i).getOmega();
            }
            if ((sndLys.get(i).getPressure() < pres)
                    && (sndLys.get(i).getOmega() != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA)) {
                above = i;
                ok = 1;
                break;
            }
        }
        if (ok == 0) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }

        /* ----- Find Immediately Below level ----- */
        ok = 0;
        for (i = sndLys.size() - 1; i > -1; i--) {
            if ((sndLys.get(i).getPressure() >= pres)
                    && (sndLys.get(i).getOmega() != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA)) {
                below = i;
                ok = 1;
                break;
            }
        }
        if (ok == 0) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        /* ----- If both levels are the same, return them ---- */
        if (above == below) {
            return sndLys.get(above).getOmega();
        }

        /* ----- Now we need to interpolate to get the omega ----- */
        nm1 = sndLys.get(above).getOmega() - sndLys.get(below).getOmega();
        nm2 = Math.log(sndLys.get(below).getPressure()
                / sndLys.get(above).getPressure());
        nm4 = Math.log(sndLys.get(below).getPressure() / pres);
        return (float) (sndLys.get(below).getOmega() + ((nm4 / nm2) * nm1));
    }

    /*************************************************************/
    /*                                                           */
    /* Interpolates the given data to calculate a virtual temp */
    /* at pressure level (pres). */
    /*                                                           */
    /* pres - Level(mb) to compute a virtual temp */
    /*************************************************************/
    public static float i_vtmp(List<NcSoundingLayer> sndLys, float pres) {
        if (!qc(pres)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        double cta, eps, tk, w;
        float t, td;

        t = i_temp(sndLys, pres);
        td = i_dwpt(sndLys, pres);

        /*
         * Return temperature in those cases when we can't get the dewpoint temp
         */
        if (!qc(td)) {
            if (!qc(t)) {
                return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
            } else {
                return t;
            }
        }

        cta = 273.15;
        eps = 0.62197;

        tk = t + cta;
        w = 0.001 * (double) NsharpLibThermo.mixratio(pres, td);
        return (float) (tk * (1.0 + w / eps) / (1.0 + w) - cta);
    }

    public static float i_wdir(List<NcSoundingLayer> sndLys, float pres)
    /*************************************************************/
    /* Interpolates the given data to calculate a wind */
    /* direction (deg) at pressure level (pres). */
    /*                                                           */
    /* pres - Level(mb) to compute a Wind */
    /*************************************************************/
    {
        float u, v;

        if (!qc(i_wndu(sndLys, pres)) || !qc(i_wndv(sndLys, pres))) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        u = i_wndu(sndLys, pres);
        v = i_wndv(sndLys, pres);
        return NsharpLibWinds.angle(u, v);
    }

    public static float i_wspd(List<NcSoundingLayer> sndLys, float pres) {
        /*************************************************************/
        /*                                                           */
        /* Interpolates the given data to calculate a wind */
        /* magnitude (kt) at pressure level (pres). */
        /*                                                           */
        /* pres - Level(mb) to compute a Wind */
        /*************************************************************/

        float u, v;

        if (!qc(i_wndu(sndLys, pres)) || !qc(i_wndv(sndLys, pres))) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }

        u = i_wndu(sndLys, pres);
        v = i_wndv(sndLys, pres);
        return (float) (Math.sqrt((u * u) + (v * v)));
    }

    public static float i_wndu(List<NcSoundingLayer> sndLys, float pres)
    /*************************************************************/
    /*                                                           */
    /* Interpolates the given data to calculate a */
    /* u-component to the wind at pressure level (pres). */
    /*                                                           */
    /* pres - Level(mb) to compute a U-Component */
    /*************************************************************/
    {
        int below, above, i;
        float utop, ubot, nm1;
        boolean ok;
        below = 0;
        above = 0;

        /* ----- Find Wind Immediately Above level ----- */
        ok = false;
        for (i = 0; i < sndLys.size(); i++) {
            if ((sndLys.get(i).getPressure() == pres)
                    && (sndLys.get(i).getWindDirection() != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA)
                    && (sndLys.get(i).getWindSpeed() != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA)) {
                return (NsharpLibWinds.ucomp(sndLys.get(i).getWindDirection(),
                        sndLys.get(i).getWindSpeed()));

            }
            if ((sndLys.get(i).getPressure() < pres)
                    && (sndLys.get(i).getWindDirection() != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA)) {
                above = i;
                ok = true;
                break;
            }
        }
        if (ok == false) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }

        /* ----- Find Wind Immediately Below level ----- */
        ok = false;
        for (i = sndLys.size() - 1; i > -1; i--) {
            if ((sndLys.get(i).getPressure() >= pres)
                    && (sndLys.get(i).getWindDirection() != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA)) {
                below = i;
                ok = true;
                break;
            }
        }
        if (ok == false) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }

        /* ----- If both levels are the same, return them ---- */
        if (above == below) {
            return NsharpLibWinds.ucomp(sndLys.get(above).getWindDirection(),
                    sndLys.get(above).getWindSpeed());
        }

        /* ----- Now we need to interpolate to get the Wind ----- */
        nm1 = sndLys.get(below).getPressure() - pres;
        ubot = NsharpLibWinds.ucomp(sndLys.get(below).getWindDirection(),
                sndLys.get(below).getWindSpeed());
        utop = NsharpLibWinds.ucomp(sndLys.get(above).getWindDirection(),
                sndLys.get(above).getWindSpeed());
        return ubot
                - (nm1 / (sndLys.get(below).getPressure() - sndLys.get(above)
                        .getPressure())) * (ubot - utop);
    }

    public static float i_wndv(List<NcSoundingLayer> sndLys, float pres)
    /*************************************************************/
    /*                                                           */
    /* Interpolates the given data to calculate a */
    /* v-component to the wind at pressure level (pres). */
    /*                                                           */
    /* pres - Level(mb) to compute a V-Component */
    /*************************************************************/
    {
        int below, above, i;
        float vtop, vbot, nm1;
        boolean ok;
        below = 0;
        above = 0;
        /* ----- Find Wind Immediately Above level ----- */
        ok = false;
        for (i = 0; i < sndLys.size(); i++) {
            if ((sndLys.get(i).getPressure() == pres)
                    && (sndLys.get(i).getWindDirection() != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA)
                    && (sndLys.get(i).getWindSpeed() != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA))
                return NsharpLibWinds.vcomp(sndLys.get(i).getWindDirection(),
                        sndLys.get(i).getWindSpeed());
            if ((sndLys.get(i).getPressure() < pres)
                    && (sndLys.get(i).getWindDirection() != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA)) {
                above = i;
                ok = true;
                break;
            }
        }
        if (ok == false)
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

        /* ----- Find Wind Immediately Below level ----- */
        ok = false;
        for (i = sndLys.size() - 1; i > -1; i--) {
            if ((sndLys.get(i).getPressure() >= pres)
                    && (sndLys.get(i).getWindDirection() != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA)) {
                below = i;
                ok = true;
                break;
            }
        }
        if (ok == false)
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

        /* ----- If both levels are the same, return them ---- */
        if (above == below) {
            return NsharpLibWinds.vcomp(sndLys.get(above).getWindDirection(),
                    sndLys.get(above).getWindSpeed());
        }

        /* ----- Now we need to interpolate to get the Wind ----- */
        nm1 = sndLys.get(below).getPressure() - pres;
        vbot = NsharpLibWinds.vcomp(sndLys.get(below).getWindDirection(),
                sndLys.get(below).getWindSpeed());
        vtop = NsharpLibWinds.vcomp(sndLys.get(above).getWindDirection(),
                sndLys.get(above).getWindSpeed());
        return vbot
                - ((nm1 / (sndLys.get(below).getPressure() - sndLys.get(above)
                        .getPressure())) * (vbot - vtop));
    }

    public static boolean qc(float value)
    /*************************************************************/
    /* Quality control of sndg data. Searches for missing */
    /*
     * data (-9999) or too big data and returns (true), (false = missing or too
     * big data)
     */
    /*************************************************************/
    {
        if (value <= NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA/10
                || value >= NOT_POSSIBLE_DATA) {
            return false;
        }
        return true;
    }
}
