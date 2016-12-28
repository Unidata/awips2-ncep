package gov.noaa.nws.ncep.edex.common.nsharpLib;

/**
 * 
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system. 
 * 
 * All methods developed in this class are based on the algorithm developed in BigSharp native 
 * C file, skparams.c , by John A. Hart/SPC.
 * All methods name are defined with same name as the C function name defined in native code.
 *
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

import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.LParcelValues;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.LayerParameters;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.MixHeight;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.Parcel;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.WindComponent;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingTools;

import java.util.List;

public class NsharpLibSkparams {

    public static float max_temp(List<NcSoundingLayer> sndLys, float mixlyr)
    /*************************************************************/
    /*                                                           */
    /* Calculates a maximum temperature forecast based on */
    /* depth of mixing level and low level temperatures. */
    /*                                                           */
    /* maxTemp = Returned max temp (c) */
    /* mixlyr = Assumed depth of mixing layer [-1=100mb] */
    /*************************************************************/
    {
        float maxTemp, temp, sfcpres;

        maxTemp = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

        NcSoundingLayer sfcLy = NsharpLibBasics.sfc(sndLys);
        if (sfcLy != null) {
            sfcpres = sfcLy.getPressure();
            /* ----- See if default layer is specified ----- */
            if (mixlyr == -1) {

                mixlyr = sfcpres - 100.0f;
            }
            temp = NsharpLibBasics.i_temp(sndLys, mixlyr) + 273.15f + 2.0f;

            maxTemp = (float) (temp * Math.pow((sfcpres / mixlyr),
                    NsharpLibSndglib.ROCP)) - 273.15f;
        }

        return maxTemp;
    }

    public static float mean_mixratio(List<NcSoundingLayer> sndLys,
            float lowerLayerPres, float upperLayerPres)
    /*************************************************************/
    /* Calculates the Mean Mixing Ratio from data in */
    /* SNDG array within specified layer. */
    /* Value is returned . */
    /*                                                           */
    /* lowerLayerPres = pressure at bottom level of layer (mb) [ -1=SFC] */
    /* upperLayerPres = pressure at top level of layer (mb) [ -1=SFC-100mb] */
    /* mmr = Returned mean mixing ratio (g/kg) */
    /*************************************************************/
    {
        int lowIndex = 0, upperIndex = 0;
        float mmr, num, dp1, dp2, totd, dbar, p1, p2, pbar, totp;

        /* ----- See if default layer is specified ----- */
        NcSoundingLayer sfcLy;
        if (lowerLayerPres == -1) {
            sfcLy = NsharpLibBasics.sfc(sndLys);
            if (sfcLy != null) {
                lowerLayerPres = sfcLy.getPressure();
            } else {
                return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
            }
        }
        if (upperLayerPres == -1) {
            sfcLy = NsharpLibBasics.sfc(sndLys);
            if (sfcLy != null) {
                upperLayerPres = sfcLy.getPressure() - 100f;
            } else {
                return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
            }
        }

        /* ----- Make sure this is a valid layer ----- */
        if (NsharpLibBasics.i_temp(sndLys, upperLayerPres) == NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA)
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        if (NsharpLibBasics.i_temp(sndLys, lowerLayerPres) == NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA) {
            // use surface layer instead
            sfcLy = NsharpLibBasics.sfc(sndLys);
            if (sfcLy != null) {
                lowerLayerPres = sfcLy.getPressure();
            } else {
                return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
            }
        }

        /* ----- Find lowest observation in layer ----- */
        boolean foundLow = false;
        for (int i = 0; i < sndLys.size(); i++) {
            if ((sndLys.get(i).getPressure() < lowerLayerPres)
                    && (sndLys.get(i).getDewpoint() != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA)) {
                lowIndex = i;
                foundLow = true;
                break;
            }
        }

        /* ----- Find highest observation in layer ----- */
        boolean foundHi= false;
        for (int i = sndLys.size() - 1; i >= 0; i--) {
            if (sndLys.get(i).getPressure() > upperLayerPres
                    && (sndLys.get(i).getDewpoint() != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA)) {
                upperIndex = i;
                foundHi = true;
                break;
            }
        }

        /* ----- Start with interpolated bottom layer ----- */
        dp1 = NsharpLibBasics.i_dwpt(sndLys, lowerLayerPres);
        p1 = lowerLayerPres;
        num = 1;

        totd = 0;
        totp = 0;
        if(foundHi && foundLow){
        	for (int i = lowIndex; i <= upperIndex; i++) {
        		if (sndLys.get(i).getDewpoint() != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA) {
        			/* ----- Calculate every level that reports a dwpt ----- */
        			dp2 = sndLys.get(i).getDewpoint();
        			p2 = sndLys.get(i).getPressure();
        			dbar = (dp1 + dp2) / 2.0f;
        			pbar = (p1 + p2) / 2.0f;
        			totd = totd + dbar;
        			totp = totp + pbar;
        			dp1 = dp2;
        			p1 = p2;
        			num++;
        		}
        	}
        }

        /* ----- Finish with interpolated top layer ----- */
        dp2 = NsharpLibBasics.i_dwpt(sndLys, upperLayerPres);
        p2 = upperLayerPres;
        dbar = (dp1 + dp2) / 2.0f;
        pbar = (p1 + p2) / 2.0f;
        totd = totd + dbar;
        totp = totp + pbar;
        mmr = NsharpLibThermo.mixratio(totp / num, totd / num);

        return mmr;
    }

    public static float unstbl_lvl(List<NcSoundingLayer> sndLys,
            float lowerLayerPres, float upperLayerPres)
    /************************************************************
     * Finds the pressure of most unstable level between levels upper and lower.
     * 
     * @param - sndLys a List of NcSoundingLayer
     * @param - lowerLayerPres= pressure at bottom level of layer (mb) [ -1=SFC]
     * @param - upperLayerPres= pressure at top level of layer (mb) [ -1=SFC-
     *        300mb]
     * @return - most unstable level (mb)
     *************************************************************/
    {
        int lowIndex, upperIndex;
        float t1, p2, t2, tmax, pmax, pres, temp, dwpt;
        /* ----- See if default layer is specified ----- */
        if (lowerLayerPres == -1) {
            lowerLayerPres = NsharpLibBasics.sfcPressure(sndLys);
            if (lowerLayerPres == NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA)
                return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        if (upperLayerPres == -1) {
            upperLayerPres = NsharpLibBasics.sfcPressure(sndLys) - 300f;
            if (upperLayerPres == NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA)
                return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }

        /* ----- Make sure this is a valid layer ----- */
        while (NsharpLibBasics.i_dwpt(sndLys, upperLayerPres) == NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA) {
            upperLayerPres += 50.0;
            if (upperLayerPres >= lowerLayerPres) {
                // upperLayerPres should not greater than lowerLayerPres
                return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
            }
        }
        if (NsharpLibBasics.i_dwpt(sndLys, lowerLayerPres) == NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA) {
            // use surface layer instead
            lowerLayerPres = NsharpLibBasics.sfcPressure(sndLys);
            if (lowerLayerPres == NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA)
                return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        /* Find lowest observation in layer ----- */
        boolean foundLow = false;
        lowIndex = 0;
        for (int i = 0; i < sndLys.size(); i++) {
            if ((sndLys.get(i).getPressure() < lowerLayerPres)
                    && (sndLys.get(i).getDewpoint() != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA)) {
                lowIndex = i;
                foundLow = true;
                break;
            }
        }

        /* ----- Find highest observation in layer ----- */
        boolean foundHi = false;
        upperIndex = sndLys.size() - 1;
        for (int i = sndLys.size() - 1; i >= 0; i--) {
            if (sndLys.get(i).getPressure() > upperLayerPres
                    && (sndLys.get(i).getDewpoint() != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA)) {
                upperIndex = i;
                foundHi = true;
                break;
            }
        }

        /* ----- Start with interpolated bottom layer ----- */
        p2 = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        t2 = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        LayerParameters sndLy = NsharpLibThermo.drylift(lowerLayerPres,
                NsharpLibBasics.i_temp(sndLys, lowerLayerPres),
                NsharpLibBasics.i_dwpt(sndLys, lowerLayerPres));
        if (sndLy != null) {
            p2 = sndLy.getPressure();
            t2 = sndLy.getTemperature();
        } else {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        tmax = NsharpLibThermo.wetlift(p2, t2, 1000.0f);
        pmax = lowerLayerPres;

        if(foundHi && foundLow){
        	for (int i = lowIndex; i <= upperIndex; i++) {
        		if (NsharpLibBasics.qc(sndLys.get(i).getDewpoint())) {
        			/* ----- Calculate every level that reports a dwpt ----- */
        			pres = sndLys.get(i).getPressure();
        			temp = sndLys.get(i).getTemperature();
        			dwpt = sndLys.get(i).getDewpoint();
        			sndLy = NsharpLibThermo.drylift(pres, temp, dwpt);
        			if (sndLy != null) {
        				p2 = sndLy.getPressure();
        				t2 = sndLy.getTemperature();
        			} else {
        				continue; // skip this layer
        			}
        			t1 = NsharpLibThermo.wetlift(p2, t2, 1000.0f);
        			if (t1 > tmax) {
        				tmax = t1;
        				pmax = pres;
        			}
        		}
        	}
        }
        /* ----- Finish with interpolated top layer ----- */
        sndLy = NsharpLibThermo.drylift(upperLayerPres,
                NsharpLibBasics.i_temp(sndLys, upperLayerPres),
                NsharpLibBasics.i_dwpt(sndLys, upperLayerPres));
        if (sndLy != null) {
            p2 = sndLy.getPressure();
            t2 = sndLy.getTemperature();
            t1 = NsharpLibThermo.wetlift(p2, t2, 1000f);
            if (t1 > tmax) {
            	int i = upperIndex+1;
            	while(i <= sndLys.size() - 1){
                    if(NsharpLibBasics.qc(sndLys.get(i).getDewpoint())){
                        pmax = sndLys.get(i).getPressure();
                        break;
                    }
                    i++;
            	}
            }
        }
        return pmax;
    }

    public static float mean_theta(List<NcSoundingLayer> sndLys,
            float lowerLayerPres, float upperLayerPres)
    /*************************************************************/
    /* Calculates the Mean Potential temperature of the */
    /* SNDG array within specified layer. */
    /*                                                           */
    /* lowerLayerPres= Bottom level of layer (mb) [ -1=SFC] */
    /* upperLayerPres= Top level of layer (mb) [ -1=SFC-100mb] */
    /* Return: mean theta (C) */
    /*************************************************************/
    {
        int lowIndex, upperIndex;
        float num, dp1, dp2, totd, dbar, p1, p2, pbar, totp, meanTheta;
        meanTheta = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

        /* ----- See if default layer is specified ----- */
        NcSoundingLayer sfcLy;
        if (lowerLayerPres == -1) {
            sfcLy = NsharpLibBasics.sfc(sndLys);
            if (sfcLy != null) {
                lowerLayerPres = sfcLy.getPressure();
            } else {
                return meanTheta;
            }
        }
        if (upperLayerPres == -1) {
            sfcLy = NsharpLibBasics.sfc(sndLys);
            if (sfcLy != null) {
                upperLayerPres = sfcLy.getPressure() - 100f;
            } else {
                return meanTheta;
            }
        }
        /* ----- Make sure this is a valid layer ----- */
        if (!NsharpLibBasics.qc(NsharpLibBasics.i_temp(sndLys, upperLayerPres))) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        if (!NsharpLibBasics.qc(NsharpLibBasics.i_temp(sndLys, lowerLayerPres))) {
            sfcLy = NsharpLibBasics.sfc(sndLys);
            if (sfcLy != null) {
                lowerLayerPres = sfcLy.getPressure();
            } else {
                return meanTheta;
            }
        }
        /* Find lowest observation in layer ----- */
        lowIndex = 0;
        boolean foundLow = false;
        for (int i = 0; i < sndLys.size(); i++) {
            if ((sndLys.get(i).getPressure() < lowerLayerPres)
                    && (sndLys.get(i).getTemperature() != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA)) {
                lowIndex = i;
                foundLow = true;
                break;
            }
        }
        boolean foundHi = false;
        /* ----- Find highest observation in layer ----- */
        upperIndex = sndLys.size() - 1;
        for (int i = sndLys.size() - 1; i >= 0; i--) {
            if (sndLys.get(i).getPressure() > upperLayerPres
                    && (sndLys.get(i).getTemperature() != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA)) {
                upperIndex = i;
                foundHi = true;
                break;
            }
        }
        /* ----- Start with interpolated bottom layer ----- */
        dp1 = NsharpLibThermo.theta(lowerLayerPres,
                NsharpLibBasics.i_temp(sndLys, lowerLayerPres), 1000.0f);
        p1 = lowerLayerPres;
        num = 1;

        totd = 0;
        totp = 0;
        if(foundHi && foundLow){
        	for (int i = lowIndex; i <= upperIndex; i++) {
        		if (NsharpLibBasics.qc(sndLys.get(i).getTemperature())) {
        			/* ----- Calculate every level that reports a temp ----- */
        			dp2 = NsharpLibThermo.theta(sndLys.get(i).getPressure(), sndLys
        					.get(i).getTemperature(), 1000.0f);
        			p2 = sndLys.get(i).getPressure();
        			dbar = (dp1 + dp2) / 2.0f;
        			pbar = (p1 + p2) / 2.0f;
        			totd = totd + dbar;
        			totp = totp + pbar;
        			dp1 = dp2;
        			p1 = p2;
        			num++;
        		}
        	}
        }

        /* ----- Finish with interpolated top layer ----- */
        dp2 = NsharpLibThermo.theta(upperLayerPres,
                NsharpLibBasics.i_temp(sndLys, upperLayerPres), 1000.0f);
        p2 = upperLayerPres;
        dbar = (dp1 + dp2) / 2.0f;
        pbar = (p1 + p2) / 2.0f;
        totd = totd + dbar;
        totp = totp + pbar;
        meanTheta = totd / num;

        return meanTheta;
    }

    public static float temp_lvl(List<NcSoundingLayer> sndLys, float temp)
    /*************************************************************/
    /* TEMP_LVL */
    /* Calculates the level (mb) of the first occurrence of */
    /* (temp) in the environment. */
    /* Input: */
    /* temp Temperature (c) to search for. */
    /* Return: level (mb). */
    /*************************************************************/
    {
        float p0, p1, t0, t1;
        double nm1, nm2, nm3;
        for (int i = 0; i < sndLys.size(); i++) {
            if ((NsharpLibBasics.qc(sndLys.get(i).getTemperature()))
                    && (sndLys.get(i).getTemperature() <= temp)) {
                if (i == 0)
                    return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
                if (sndLys.get(i).getTemperature() == temp)
                    return sndLys.get(i).getPressure();

                p0 = sndLys.get(i - 1).getPressure();
                t0 = NsharpLibBasics.i_temp(sndLys, p0);
                p1 = sndLys.get(i).getPressure();
                t1 = sndLys.get(i).getTemperature();
                nm1 = temp - t0;
                nm2 = t1 - t0;
                nm3 = Math.log(p1 / p0);
                return (float) (p0 * Math.exp((nm1 / nm2) * nm3));
            }
        }
        return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
    }

    public static float bulk_rich(List<NcSoundingLayer> sndLys, float ptop,
            float pbot, float lplpress, float bplus)
    /*************************************************************/
    /* BRNShear */
    /* Calculates the Bulk Richardson Number for given parcel. */
    /* BRNShear Value is returned from function. */
    /*************************************************************/
    {

        /* ----- First, calculate lowest 500m mean wind ----- */
        float mnu1, mnv1;
        WindComponent windComp = NsharpLibWinds.mean_wind(
                sndLys,
                pbot,
                NsharpLibBasics.i_pres(sndLys,
                        NsharpLibBasics.i_hght(sndLys, pbot) + 500.0f));
        mnu1 = windComp.getUcomp();
        mnv1 = windComp.getVcomp();
        /* ----- Next, calculate 6000m mean wind ----- */
        float mnu2, mnv2;
        windComp = NsharpLibWinds.mean_wind(sndLys, pbot, ptop);
        mnu2 = windComp.getUcomp();
        mnv2 = windComp.getVcomp();

        /* ----- Check to make sure CAPE and SHEAR are avbl ----- */
        if (!NsharpLibBasics.qc(bplus) || !NsharpLibBasics.qc(mnu1)
                || !NsharpLibBasics.qc(mnu2))
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

        /* ----- Calculate shear between winds ----- */
        float dmnu = mnu2 - mnu1;
        float dmnv = mnv2 - mnv1;
        float brnshear = (float) (Math.sqrt((dmnu * dmnu) + (dmnv * dmnv))) * 0.51479f;
        brnshear = brnshear * brnshear / 2.0f;

        /* ----- return BRN shear ----- */
        return brnshear;
    }

    /*************************************************************/
    /* define_parcel() */
    /* Based on Parcel flag, assigns parcel basic values to */
    /* LParcelValues. */
    /*                                                           */
    /* flag -Parcel selection, see NsharpLibSndglib for definitions */
    /* -1 = Use Previous Selection */
    /* 1 = Observed sfc parcel */
    /* 2 = Fcst sfc parcel */
    /* 3 = Most unstable parcel */
    /* 4 = Mean mixlyr parcel */
    /* 5 = User defined parcel */
    /* 6 = Mean Effective Layer parcel */
    /*                                                           */
    /* pres - Variable pressure level (mb) */
    /*************************************************************/
    public static LParcelValues define_parcel(List<NcSoundingLayer> sndLys,
            int parcelFlag, float pres) {
        float sfcPres, mmr, mtha;
        NcSoundingLayer sfcLy;
        LParcelValues lplvals = new LParcelValues();

        switch (parcelFlag) {
        case NsharpLibSndglib.PARCELTYPE_OBS_SFC:
            sfcLy = NsharpLibBasics.sfc(sndLys);
            if (sfcLy != null) {
                lplvals.setTemp(sfcLy.getTemperature());
                lplvals.setPres(sfcLy.getPressure());
                lplvals.setDwpt(sfcLy.getDewpoint());
            }
            break;

        case NsharpLibSndglib.PARCELTYPE_FCST_SFC:
            lplvals.setTemp(max_temp(sndLys, -1));
            mmr = mean_mixratio(sndLys, -1, -1);
            sfcPres = NsharpLibBasics.sfcPressure(sndLys);
            lplvals.setDwpt(NsharpLibThermo.temp_at_mixrat(mmr, sfcPres));
            lplvals.setPres(sfcPres);
            break;

        case NsharpLibSndglib.PARCELTYPE_MOST_UNSTABLE:
            sfcPres = NsharpLibBasics.sfcPressure(sndLys);
            float muPrese;
            if (sfcPres <= NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA) {
                muPrese = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
            } else {
                muPrese = unstbl_lvl(sndLys, -1, sfcPres - pres);
            }
            lplvals.setPres(muPrese);
            lplvals.setTemp(NsharpLibBasics.i_temp(sndLys, muPrese));
            lplvals.setDwpt(NsharpLibBasics.i_dwpt(sndLys, muPrese));
            break;

        case NsharpLibSndglib.PARCELTYPE_MEAN_MIXING:
            sfcPres = NsharpLibBasics.sfcPressure(sndLys);
            mtha = mean_theta(sndLys, -1, sfcPres - pres);
            lplvals.setTemp(NsharpLibThermo.theta(1000.0f, mtha, sfcPres));
            mmr = mean_mixratio(sndLys, -1, sfcPres - pres);
            lplvals.setDwpt(NsharpLibThermo.temp_at_mixrat(mmr, sfcPres));
            lplvals.setPres(sfcPres);
            break;

        case NsharpLibSndglib.PARCELTYPE_USER_DEFINED:
            lplvals.setTemp(NsharpLibBasics.i_temp(sndLys, pres));
            lplvals.setDwpt(NsharpLibBasics.i_dwpt(sndLys, pres));
            lplvals.setPres(pres);
            break;

        case NsharpLibSndglib.PARCELTYPE_EFF:
            float p_top = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA,
            p_bot = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
            /*
             * port effective_inflow_layer_thermo(float ecape, float ecinh,
             * float *bot, float *top) to the following section to avoid
             * recursive call to this function, define_parcel(). When original
             * code make call to effective_inflow_layer_thermo(), its statement
             * is effective_inflow_layer_thermo(100,-250, &p_bot, &p_top);
             */
            // define ecape and ecinh according to original code
            float ecape = 100f,
            ecinh = -250f;
            sfcPres = NsharpLibBasics.sfcPressure(sndLys);
            if (sfcPres != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA) {
                muPrese = unstbl_lvl(sndLys, -1, sfcPres - pres);
                if (muPrese != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA) {
                    float muTemp = NsharpLibBasics.i_temp(sndLys, muPrese);
                    float muDew = NsharpLibBasics.i_dwpt(sndLys, muPrese);
                    Parcel pcl = parcel(sndLys, -1, -1, muPrese, muTemp, muDew);
                    float mucape = pcl.getBplus();
                    float mucin = pcl.getBminus();
                    if (mucape >= 100 && mucin > -250) {
                        /*
                         * ----- Begin at surface and search upward for
                         * "Effective Surface" -----
                         */
                        int sfcIndex = NsharpLibBasics.sfcIndex(sndLys);
                        if (sfcIndex >= 0) {
                            for (int i = sfcIndex; i <= sndLys.size() - 1; i++) {
                                pcl = parcel(sndLys, -1, -1, sndLys.get(i)
                                        .getPressure(), sndLys.get(i)
                                        .getTemperature(), sndLys.get(i)
                                        .getDewpoint());
                                if ((pcl.getBplus() >= ecape)
                                        && (pcl.getBminus() >= ecinh)) {
                                    p_bot = sndLys.get(i).getPressure();
                                    break;
                                }
                            }
                        }
                        if (p_bot != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA) {
                            /*
                             * ----- Keep searching upward for the
                             * "Effective Top" -----
                             */
                            if (sfcIndex >= 0) {
                                for (int i = sfcIndex; i <= sndLys.size() - 1; i++) {
                                    if (sndLys.get(i).getPressure() <= p_bot) {
                                        pcl = parcel(sndLys, -1, -1, sndLys
                                                .get(i).getPressure(), sndLys
                                                .get(i).getTemperature(),
                                                sndLys.get(i).getDewpoint());
                                        if (((pcl.getBplus() <= ecape) || (pcl
                                                .getBminus() <= ecinh))
                                                && i > 0) {
                                            p_top = sndLys.get(i - 1)
                                                    .getPressure();
                                            break;
                                        }

                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (p_bot > 0 && p_top > 0) {
                mtha = mean_theta(sndLys, p_bot, p_top);
                lplvals.setTemp(NsharpLibThermo.theta(1000.0f, mtha,
                        (p_top + p_bot) / 2));
                mmr = mean_mixratio(sndLys, p_bot, p_top);
                lplvals.setDwpt(NsharpLibThermo.temp_at_mixrat(mmr,
                        (p_top + p_bot) / 2));
                lplvals.setPres((p_top + p_bot) / 2);
            } else { // p_bot= NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA
                sfcLy = NsharpLibBasics.sfc(sndLys);
                if (sfcLy != null) {
                    lplvals.setTemp(sfcLy.getTemperature());
                    lplvals.setPres(sfcLy.getPressure());
                    lplvals.setDwpt(sfcLy.getDewpoint());
                }
            }
            break;
        }

        lplvals.setParcelFlag(parcelFlag);
        return lplvals;
    }

    public static Parcel parcel(List<NcSoundingLayer> sndLys, float lowerPress,
            float upperPress, float pres, float temp, float dwpt)
    /**************************************************************/
    /* PARCEL */
    /* Lifts specified parcel, calculating various levels and */
    /* parameters from data in sndLys. */
    /* B+/B- are calculated based on layer (lower-upper). */
    /*                                                            */
    /* All calculations use the virtual temperature correction. */
    /* Input: */
    /* lower = Bottom level of layer (mb) [ -1=SFC] */
    /* upper = Top level of layer (mb) [ -1=TOP] */
    /* pres = LiftPArcelLayer pressure (mb) */
    /* temp = LiftPArcelLayer temperature (c) */
    /* dwpt = LiftPArcelLayer dew point (c) */
    /* Return: Parcel structure */
    /**************************************************************/
    {
        if (sndLys == null || sndLys.size() < 1)
            return null;
        int i;
        float te1, pe1, te2 = 0, pe2 = 0, h1, h2, lyre, tdef1, tdef2, totp, totn;
        float te3, pe3, h3, tp1, tp2 = 0, tp3, tdef3, lyrf, lyrlast, pelast;
        float tote = 0, totx, cap_strength, li_max, li_maxpres, blupper;
        float cap_strengthpres, pp, pp1, pp2, dz;
        float theta_parcel, tv_env_bot, tv_env_top, lclhght, blmr,cinh_old=0;

        Parcel rtnParcel = new Parcel();
        // initialize some fields in Parcel
        rtnParcel.setLplpres(pres);
        rtnParcel.setLpltemp(temp);
        rtnParcel.setLpldwpt(dwpt);
        rtnParcel.setBlayer(lowerPress);
        rtnParcel.setTlayer(upperPress);
        lyre = -1;
        cap_strength = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        cap_strengthpres = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        li_max = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        li_maxpres = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        totp = 0;
        totn = 0;

        /* ----- See if default layer is specified ----- */
        NcSoundingLayer sfcLy;
        if (lowerPress == -1) {
            sfcLy = NsharpLibBasics.sfc(sndLys);
            if (sfcLy != null) {
                lowerPress = sfcLy.getPressure();
                rtnParcel.setBlayer(lowerPress);
            } else {
                return null;
            }
        }
        if (upperPress == -1) {
            upperPress = sndLys.get(sndLys.size() - 1).getPressure();
            if (upperPress == NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA) {
                return null;
            }
            rtnParcel.setTlayer(upperPress);
        }
        /* ----- Make sure this is a valid layer ----- */
        if (lowerPress > pres) {
            lowerPress = pres;
            rtnParcel.setBlayer(lowerPress);
        }

        if (!NsharpLibBasics.qc(NsharpLibBasics.i_vtmp(sndLys, upperPress))
                || !NsharpLibBasics.qc(NsharpLibBasics.i_vtmp(sndLys,
                        lowerPress))) {
            return null;
        }

        /* ----- Begin with Mixing Layer (LPL-LCL) ----- */
        te1 = NsharpLibBasics.i_vtmp(sndLys, pres);
        pe1 = lowerPress;
        h1 = NsharpLibBasics.i_hght(sndLys, pe1);
        tp1 = NsharpLibThermo.virtemp(pres, temp, dwpt);

        /* lift parcel and return LCL pres (mb) and LCL temp (C) */
        LayerParameters sndLy = NsharpLibThermo.drylift(pres, temp, dwpt);
        if (sndLy != null) {
            pe2 = sndLy.getPressure();
            tp2 = sndLy.getTemperature();
        } else {
            pe2 = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
            tp2 = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        /* define top of layer as LCL pres */
        blupper = pe2;
        h2 = NsharpLibBasics.i_hght(sndLys, pe2);
        te2 = NsharpLibBasics.i_vtmp(sndLys, pe2);
        rtnParcel.setLclpres(pe2);
        rtnParcel.setLclAgl(NsharpLibBasics.mtof(NsharpLibBasics.agl(sndLys,
                NsharpLibBasics.i_hght(sndLys, pe2))));

        /* calculate lifted parcel theta for use in iterative CINH loop below */
        /* recall that lifted parcel theta is CONSTANT from LPL to LCL */
        theta_parcel = NsharpLibThermo.theta(pe2, tp2, 1000.0f);

        /* environmental theta and mixing ratio at LPL */
        blmr = NsharpLibThermo.mixratio(pres, dwpt);

        /* ----- Accumulate CINH in mixing layer below the LCL ----- */
        /* This will be done in 10mb increments, and will use the */
        /* virtual temperature correction where possible. */
        /*
         * initialize negative area to zero and iterate from LPL to LCL in 10mb
         * increments
         */
        totn = 0;
        for (pp = lowerPress; pp > blupper; pp -= 10.0) {
            pp1 = pp;
            pp2 = pp - 10.0f;
            if (pp2 < blupper)
                pp2 = blupper;

            dz = NsharpLibBasics.i_hght(sndLys, pp2)
                    - NsharpLibBasics.i_hght(sndLys, pp1);

            /*
             * calculate difference between Tv_parcel and Tv_environment at top
             * and bottom of 10mb layers
             */
            /* make use of constant lifted parcel theta and mixr from LPL to LCL */
            tv_env_bot = NsharpLibThermo.virtemp(
                    pp1,
                    NsharpLibThermo.theta(pp1,
                            NsharpLibBasics.i_temp(sndLys, pp1), 1000.0f),
                    NsharpLibBasics.i_dwpt(sndLys, pp1));
            tdef1 = ((NsharpLibThermo.virtemp(pp1, theta_parcel,
                    NsharpLibThermo.temp_at_mixrat(blmr, pp1))) - tv_env_bot)
                    / (tv_env_bot + 273.13f);

            tv_env_top = NsharpLibThermo.virtemp(
                    pp2,
                    NsharpLibThermo.theta(pp2,
                            NsharpLibBasics.i_temp(sndLys, pp2), 1000.0f),
                    NsharpLibBasics.i_dwpt(sndLys, pp2));
            tdef2 = ((NsharpLibThermo.virtemp(pp2, theta_parcel,
                    NsharpLibThermo.temp_at_mixrat(blmr, pp2))) - tv_env_top)
                    / (tv_env_top + 273.15f);

            lyre = 9.8f * (tdef1 + tdef2) / 2.0f * dz;
            if (lyre < 0){
                totn += lyre;
            }
        }

        if (lowerPress > pe2) {
            lowerPress = pe2;
            rtnParcel.setBlayer(lowerPress);
        }

        /* ----- Find lowest observation in layer ----- */
        int lowIndex = sndLys.size() - 1;
        for (i = 0; i < sndLys.size(); i++) {
            if (sndLys.get(i).getPressure() < lowerPress
                    && NsharpLibBasics.qc(sndLys.get(i).getDewpoint())) {
                lowIndex = i;
                break;
            }
        }

        /* ----- Find highest observation in layer ----- */
        int upIndex = 0;
        for (i = sndLys.size() - 1; i >= 0; i--) {
            if (sndLys.get(i).getPressure() > upperPress) {
                upIndex = i;
                break;
            }
        }

        /* ----- Start with interpolated bottom layer ----- */
        /* begin moist ascent from lifted parcel LCL (pe2, tp2) */
        pe1 = lowerPress;
        h1 = NsharpLibBasics.i_hght(sndLys, pe1);
        te1 = NsharpLibBasics.i_vtmp(sndLys, pe1);
        tp1 = NsharpLibThermo.wetlift(pe2, tp2, pe1);
        totp = 0;
        lyre = 0;
        for (i = lowIndex; i < sndLys.size(); i++) {
            if (NsharpLibBasics.qc(sndLys.get(i).getTemperature())) {
                /* ----- Calculate every level that reports a temp ----- */
                pe2 = sndLys.get(i).getPressure();
                h2 = sndLys.get(i).getGeoHeight();
                te2 = NsharpLibBasics.i_vtmp(sndLys, pe2);
                tp2 = NsharpLibThermo.wetlift(pe1, tp1, pe2);
                tdef1 = (NsharpLibThermo.virtemp(pe1, tp1, tp1) - te1)
                        / (te1 + 273.15f);
                tdef2 = (NsharpLibThermo.virtemp(pe2, tp2, tp2) - te2)
                        / (te2 + 273.15f);
                lyrlast = lyre;
                lyre = 9.8f * (tdef1 + tdef2) / 2.0f * (h2 - h1);

                /* ----- Check for Max LI ----- */
                if ((NsharpLibThermo.virtemp(pe2, tp2, tp2) - te2) > li_max) {
                    li_max = NsharpLibThermo.virtemp(pe2, tp2, tp2) - te2;
                    li_maxpres = pe2;
                }

                /* ----- Check for Max Cap Strength ----- */
                if ((te2 - NsharpLibThermo.virtemp(pe2, tp2, tp2)) > cap_strength) {
                    cap_strength = te2 - NsharpLibThermo.virtemp(pe2, tp2, tp2);
                    cap_strengthpres = pe2;
                }

                /* add layer energy to total positive if lyre > 0 */
                if (lyre > 0) {
                    totp += lyre;
                }
                /*
                 * add layer energy to total negative if lyre < 0, only up to
                 * the EL
                 */
                else {
                    if (pe2 > 500.0) {
                        totn += lyre;
                    }
                }

                tote += lyre;
                pelast = pe1;

                pe1 = pe2;
                h1 = h2;
                te1 = te2;
                tp1 = tp2;

                /* ----- Is this the top of given layer ----- */
                if ((i >= upIndex)
                        && (!NsharpLibBasics.qc(rtnParcel.getBplus()))) {
                    pe3 = pe1;
                    h3 = h1;
                    te3 = te1;
                    tp3 = tp1;
                    lyrf = lyre;

                    if (lyrf > 0.0) {
                        rtnParcel.setBplus(totp - lyrf);
                        rtnParcel.setBminus(totn);
                    } else {
                        rtnParcel.setBplus(totp);
                        if (pe2 > 500.0) {
                            rtnParcel.setBminus(totn + lyrf);
                        } else {
                            rtnParcel.setBminus(totn);
                        }
                    }

                    pe2 = upperPress;
                    h2 = NsharpLibBasics.i_hght(sndLys, pe2);
                    te2 = NsharpLibBasics.i_vtmp(sndLys, pe2);
                    tp2 = NsharpLibThermo.wetlift(pe3, tp3, pe2);
                    tdef3 = (NsharpLibThermo.virtemp(pe3, tp3, tp3) - te3)
                            / (te3 + 273.15f);
                    tdef2 = (NsharpLibThermo.virtemp(pe2, tp2, tp2) - te2)
                            / (te2 + 273.15f);
                    lyrf = 9.8f * (tdef3 + tdef2) / 2.0f * (h2 - h3);
                    if (lyrf > 0.0f) {
                        rtnParcel.setBplus(rtnParcel.getBplus() + lyrf);
                    } else {
                        if (pe2 > 600.0) {
                            rtnParcel.setBminus(rtnParcel.getBminus() + lyrf);
                        }
                    }
                    if (rtnParcel.getBplus() == 0.0) {
                        rtnParcel.setBminus(0);
                    }
                }

                /* ----- Is this the freezing level ----- */
                if ((te2 <= 0.0) && !NsharpLibBasics.qc(rtnParcel.getBfzl())) {
                    pe3 = pelast;
                    h3 = NsharpLibBasics.i_hght(sndLys, pe3);
                    te3 = NsharpLibBasics.i_vtmp(sndLys, pe3);
                    tp3 = NsharpLibThermo.wetlift(pe1, tp1, pe3);
                    lyrf = lyre;

                    if (lyrf > 0.0) {
                        rtnParcel.setBfzl(totp - lyrf);
                    } else {
                        rtnParcel.setBfzl(totp);
                    }

                    /*
                     * error check for LCL temp colder than 0 C by Patrick Marsh
                     * and RLT 1/6/12
                     */
                    pe2 = temp_lvl(sndLys, 0f);
                    if (pe2 > rtnParcel.getLclpres()) {
                        rtnParcel.setBfzl(0.0f);
                    } else if (NsharpLibBasics.qc(pe2)) {
                        h2 = NsharpLibBasics.i_hght(sndLys, pe2);
                        te2 = NsharpLibBasics.i_vtmp(sndLys, pe2);
                        tp2 = NsharpLibThermo.wetlift(pe3, tp3, pe2);
                        tdef3 = (NsharpLibThermo.virtemp(pe3, tp3, tp3) - te3)
                                / (te3 + 273.15f);
                        tdef2 = (NsharpLibThermo.virtemp(pe2, tp2, tp2) - te2)
                                / (te2 + 273.15f);
                        lyrf = 9.8f * (tdef3 + tdef2) / 2.0f * (h2 - h3);
                        if (lyrf > 0.0) {
                            rtnParcel.setBfzl(rtnParcel.getBfzl() + lyrf);
                        }
                    }
                }

                /* ----- Is this the -10c level ----- */
                if ((te2 <= -10.0) && !NsharpLibBasics.qc(rtnParcel.getWm10c())) {
                    pe3 = pelast;
                    h3 = NsharpLibBasics.i_hght(sndLys, pe3);
                    te3 = NsharpLibBasics.i_vtmp(sndLys, pe3);
                    tp3 = NsharpLibThermo.wetlift(pe1, tp1, pe3);
                    lyrf = lyre;

                    if (lyrf > 0.0) {
                        rtnParcel.setWm10c(totp - lyrf);
                    } else {
                        rtnParcel.setWm10c(totp);
                    }
                    /*
                     * error check for LCL temp colder than -10 C by Patrick
                     * Marsh and RLT 1/6/12
                     */
                    pe2 = temp_lvl(sndLys, -10.0f);
                    if (pe2 > rtnParcel.getLclpres()) {
                        rtnParcel.setWm10c(0);
                    } else if (NsharpLibBasics.qc(pe2)) {
                        h2 = NsharpLibBasics.i_hght(sndLys, pe2);
                        te2 = NsharpLibBasics.i_vtmp(sndLys, pe2);
                        tp2 = NsharpLibThermo.wetlift(pe3, tp3, pe2);
                        tdef3 = (NsharpLibThermo.virtemp(pe3, tp3, tp3) - te3)
                                / (te3 + 273.15f);
                        tdef2 = (NsharpLibThermo.virtemp(pe2, tp2, tp2) - te2)
                                / (te2 + 273.15f);
                        lyrf = 9.8f * (tdef3 + tdef2) / 2.0f * (h2 - h3);
                        if (lyrf > 0.0) {
                            rtnParcel.setWm10c(rtnParcel.getWm10c() + lyrf);
                        }
                    }
                }

                /* ----- Is this the -20c level ----- */
                if ((te2 <= -20.0) && !NsharpLibBasics.qc(rtnParcel.getWm20c())) {
                    pe3 = pelast;
                    h3 = NsharpLibBasics.i_hght(sndLys, pe3);
                    te3 = NsharpLibBasics.i_vtmp(sndLys, pe3);
                    tp3 = NsharpLibThermo.wetlift(pe1, tp1, pe3);
                    lyrf = lyre;

                    if (lyrf > 0.0) {
                        rtnParcel.setWm20c(totp - lyrf);
                    } else {
                        rtnParcel.setWm20c(totp);
                    }
                    /*
                     * error check for LCL temp colder than -20 C by Patrick
                     * Marsh and RLT 1/6/12
                     */
                    pe2 = temp_lvl(sndLys, -20.0f);
                    if (pe2 > rtnParcel.getLclpres()) {
                        rtnParcel.setWm20c(0);
                    } else if (NsharpLibBasics.qc(pe2)) {
                        h2 = NsharpLibBasics.i_hght(sndLys, pe2);
                        te2 = NsharpLibBasics.i_vtmp(sndLys, pe2);
                        tp2 = NsharpLibThermo.wetlift(pe3, tp3, pe2);
                        tdef3 = (NsharpLibThermo.virtemp(pe3, tp3, tp3) - te3)
                                / (te3 + 273.15f);
                        tdef2 = (NsharpLibThermo.virtemp(pe2, tp2, tp2) - te2)
                                / (te2 + 273.15f);
                        lyrf = 9.8f * (tdef3 + tdef2) / 2.0f * (h2 - h3);
                        if (lyrf > 0.0) {
                            rtnParcel.setWm20c(rtnParcel.getWm20c() + lyrf);
                        }
                    }
                }

                /* ----- Is this the -30c level ----- */
                if ((te2 <= -30.0) && !NsharpLibBasics.qc(rtnParcel.getWm30c())) {
                    pe3 = pelast;
                    h3 = NsharpLibBasics.i_hght(sndLys, pe3);
                    te3 = NsharpLibBasics.i_vtmp(sndLys, pe3);
                    tp3 = NsharpLibThermo.wetlift(pe1, tp1, pe3);
                    lyrf = lyre;
                    if (lyrf > 0.0) {
                        rtnParcel.setWm30c(totp - lyrf);
                    } else {
                        rtnParcel.setWm30c(totp);
                    }

                    /*
                     * error check for LCL temp colder than -30 C by Patrick
                     * Marsh and RLT 1/6/12
                     */
                    pe2 = temp_lvl(sndLys, -30.0f);
                    if (pe2 > rtnParcel.getLclpres()) {
                        rtnParcel.setWm30c(0);
                    } else if (NsharpLibBasics.qc(pe2)) {
                        h2 = NsharpLibBasics.i_hght(sndLys, pe2);
                        te2 = NsharpLibBasics.i_vtmp(sndLys, pe2);
                        tp2 = NsharpLibThermo.wetlift(pe3, tp3, pe2);
                        tdef3 = (NsharpLibThermo.virtemp(pe3, tp3, tp3) - te3)
                                / (te3 + 273.15f);
                        tdef2 = (NsharpLibThermo.virtemp(pe2, tp2, tp2) - te2)
                                / (te2 + 273.15f);
                        lyrf = 9.8f * (tdef3 + tdef2) / 2.0f * (h2 - h3);
                        if (lyrf > 0.0) {
                            rtnParcel.setWm30c(rtnParcel.getWm30c() + lyrf);
                        }
                    }
                }

                /* ----- Is this the 3km level ----- */
                lclhght = NsharpLibBasics.agl(sndLys,
                        NsharpLibBasics.i_hght(sndLys, rtnParcel.getLclpres()));
                if (lclhght < 3000) {
                    if ((NsharpLibBasics.agl(sndLys,
                            NsharpLibBasics.i_hght(sndLys, pe2)) >= 3000.0f)
                            && (!NsharpLibBasics.qc(rtnParcel.getCape3km()))) {
                        pe3 = pelast;
                        h3 = NsharpLibBasics.i_hght(sndLys, pe3);
                        te3 = NsharpLibBasics.i_vtmp(sndLys, pe3);
                        tp3 = NsharpLibThermo.wetlift(pe1, tp1, pe3);
                        lyrf = lyre;

                        if (lyrf > 0.0) {
                            rtnParcel.setCape3km(totp - lyrf);
                        } else {
                            rtnParcel.setCape3km(totp);
                        }
                        pe2 = NsharpLibBasics.i_pres(sndLys,
                                NsharpLibBasics.msl(sndLys, 3000f));
                        if (NsharpLibBasics.qc(pe2)) {
                            h2 = NsharpLibBasics.msl(sndLys, 3000.0f);
                            te2 = NsharpLibBasics.i_vtmp(sndLys, pe2);
                            tp2 = NsharpLibThermo.wetlift(pe3, tp3, pe2);
                            tdef3 = (NsharpLibThermo.virtemp(pe3, tp3, tp3) - te3)
                                    / (te3 + 273.15f);
                            tdef2 = (NsharpLibThermo.virtemp(pe2, tp2, tp2) - te2)
                                    / (te2 + 273.15f);
                            lyrf = 9.8f * (tdef3 + tdef2) / 2.0f * (h2 - h3);
                            if (lyrf > 0.0) {
                                rtnParcel.setCape3km(rtnParcel.getCape3km()
                                        + lyrf);
                            }
                        }
                    }
                } else {
                    rtnParcel.setCape3km(0.0f);
                }
                /* ----- Is this the 6km level ----- */
                if (lclhght < 6000) {
                    if ((NsharpLibBasics.agl(sndLys,
                            NsharpLibBasics.i_hght(sndLys, pe2)) >= 6000.0f)
                            && (!NsharpLibBasics.qc(rtnParcel.getCape6km()))) {
                        pe3 = pelast;
                        h3 = NsharpLibBasics.i_hght(sndLys, pe3);
                        te3 = NsharpLibBasics.i_vtmp(sndLys, pe3);
                        tp3 = NsharpLibThermo.wetlift(pe1, tp1, pe3);
                        lyrf = lyre;

                        if (lyrf > 0.0) {
                            rtnParcel.setCape6km(totp - lyrf);
                        } else {
                            rtnParcel.setCape6km(totp);
                        }

                        pe2 = NsharpLibBasics.i_pres(sndLys,
                                NsharpLibBasics.msl(sndLys, 6000f));
                        if (NsharpLibBasics.qc(pe2)) {
                            h2 = NsharpLibBasics.msl(sndLys, 6000.0f);
                            te2 = NsharpLibBasics.i_vtmp(sndLys, pe2);
                            tp2 = NsharpLibThermo.wetlift(pe3, tp3, pe2);
                            tdef3 = (NsharpLibThermo.virtemp(pe3, tp3, tp3) - te3)
                                    / (te3 + 273.15f);
                            tdef2 = (NsharpLibThermo.virtemp(pe2, tp2, tp2) - te2)
                                    / (te2 + 273.15f);
                            lyrf = 9.8f * (tdef3 + tdef2) / 2.0f * (h2 - h3);
                            if (lyrf > 0.0) {
                                rtnParcel.setCape6km(rtnParcel.getCape6km()
                                        + lyrf);
                            }
                        }
                    }
                } else {
                    rtnParcel.setCape6km(0.0f);
                }

                /* ----- LFC Possibility ----- */
                if ((lyre >= 0) && (lyrlast <= 0)) {
                    tp3 = tp1;
                    te3 = te1;
                    pe2 = pe1;
                    pe3 = pelast;
                    while (NsharpLibBasics.i_vtmp(sndLys, pe3) > NsharpLibThermo
                            .virtemp(pe3,
                                    NsharpLibThermo.wetlift(pe2, tp3, pe3),
                                    NsharpLibThermo.wetlift(pe2, tp3, pe3))) {
                        pe3 -= 5;
                    }
                    rtnParcel.setLfcpres(pe3);
                    rtnParcel.setLfcTemp(NsharpLibBasics.i_temp(sndLys, pe3));
                    rtnParcel.setLfcAgl(NsharpLibBasics.mtof(NsharpLibBasics
                            .agl(sndLys, NsharpLibBasics.i_hght(sndLys, pe3))));
                    rtnParcel
                            .setElpres(NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA);
                    cinh_old = totn; 

                    tote = 0;
                    li_max = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

                    if (cap_strength < 0.0) {
                        cap_strength = 0.0f;
                    }
                    rtnParcel.setCap(cap_strength);
                    rtnParcel.setCappres(cap_strengthpres);
                }

                /* ----- EL Possibility ----- */
                if ((lyre <= 0.0) && (lyrlast >= 0.0)) {
                    tp3 = tp1;
                    te3 = te1;
                    pe2 = pe1;
                    pe3 = pelast;
                    while (NsharpLibBasics.i_vtmp(sndLys, pe3) < NsharpLibThermo
                            .virtemp(pe3,
                                    NsharpLibThermo.wetlift(pe2, tp3, pe3),
                                    NsharpLibThermo.wetlift(pe2, tp3, pe3))) {
                        pe3 -= 5.0;
                    }

                    rtnParcel.setElpres(pe3);
                    rtnParcel.setElTemp(NsharpLibBasics.i_temp(sndLys, pe3));
                    rtnParcel.setElAgl(NsharpLibBasics.mtof(NsharpLibBasics
                            .agl(sndLys, NsharpLibBasics.i_hght(sndLys, pe3))));
                    rtnParcel.setLimax(-li_max);
                    rtnParcel.setLimaxpres(li_maxpres);
                    rtnParcel
                            .setMplpres(NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA);
                }

                /* ----- MPL Possibility ----- */
                if (((tote <= 0.0)
                        && (!NsharpLibBasics.qc(rtnParcel.getMplpres())) && (NsharpLibBasics
                            .qc(rtnParcel.getElpres())))) {
                    pe3 = pelast;
                    h3 = NsharpLibBasics.i_hght(sndLys, pe3);
                    te3 = NsharpLibBasics.i_vtmp(sndLys, pe3);
                    tp3 = NsharpLibThermo.wetlift(pe1, tp1, pe3);
                    totx = tote - lyre;
                    pe2 = pelast;
                    while (totx > 0.0) {
                        pe2 -= 1;
                        te2 = NsharpLibBasics.i_vtmp(sndLys, pe2);
                        tp2 = NsharpLibThermo.wetlift(pe3, tp3, pe2);
                        h2 = NsharpLibBasics.i_hght(sndLys, pe2);
                        tdef3 = (NsharpLibThermo.virtemp(pe3, tp3, tp3) - te3)
                                / (te3 + 273.15f);
                        tdef2 = (NsharpLibThermo.virtemp(pe2, tp2, tp2) - te2)
                                / (te2 + 273.15f);
                        lyrf = 9.8f * (tdef3 + tdef2) / 2.0f * (h2 - h3);
                        totx += lyrf;

                        tp3 = tp2;
                        te3 = te2;
                        pe3 = pe2;
                    }
                    rtnParcel.setMplpres(pe2);
                    rtnParcel.setMplAgl(NsharpLibBasics.mtof(NsharpLibBasics
                            .agl(sndLys, NsharpLibBasics.i_hght(sndLys, pe2))));
                }
                /* ----- 500mb Lifted Index ----- */
                if ((sndLys.get(i).getPressure() <= 500)
                        && (rtnParcel.getLi5() == NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA)) {
                    float li5 = NsharpLibBasics.i_vtmp(sndLys, 500)
                            - NsharpLibThermo.virtemp(500,
                                    NsharpLibThermo.wetlift(pe1, tp1, 500),
                                    NsharpLibThermo.wetlift(pe1, tp1, 500));

                    rtnParcel.setLi5(li5);
                }

                /* ----- 300mb Lifted Index ----- */
                if ((sndLys.get(i).getPressure() <= 300)
                        && (rtnParcel.getLi3() == NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA)) {
                    float li3 = NsharpLibBasics.i_vtmp(sndLys, 300)
                            - NsharpLibThermo.virtemp(300,
                                    NsharpLibThermo.wetlift(pe1, tp1, 300),
                                    NsharpLibThermo.wetlift(pe1, tp1, 300));
                    rtnParcel.setLi3(li3);
                }
            } /* end if (NsharpLibBasics.qc(sndLys.get(i).getTemperature())) */
        } /* end for(i = lowIndex; i < sndLys.size(); i++) */

        // CHIN::: A potential bug here: do not know why set bminus here, as cinh_old 
        // only set once at "LFC Possibility" block with "cinh_old  = totn;"
        // bminus was set in many other areas with values not the same as totn
        // In other words, those setting of bminus may be useless.
        rtnParcel.setBminus(cinh_old);
        if (rtnParcel.getBplus() == 0.0) {
            rtnParcel.setBminus(0);
        }
        return rtnParcel;
    }

    public static Parcel createParcel(List<NcSoundingLayer> sndLys,
            int parcelFlag, float parcelPres, float lowerPress, float upperPress)
    /**
     * CREATE PARCEL This method combines define_parcel() and parcel() functions
     * to make it simpler to use.
     * 
     * Lifts specified parcel, calculating various levels and parameters from
     * data in sndLys. B+/B- are calculated based on layer (lower-upper).
     * 
     * All calculations use the virtual temperature correction.
     * 
     * @param parcelFlag
     *            -Parcel selection, see NsharpLibSndglib for definitions -1 =
     *            Use Previous Selection 1 = Observed sfc parcel 2 = Fcst sfc
     *            parcel 3 = Most unstable parcel 4 = Mean mixlyr parcel 5 =
     *            User defined parcel 6 = Mean Effective Layer parcel
     * 
     * @param parcelPres
     *            -defined parcel pressure, see NsharpLibSndglib for definitions
     * 
     * @param sndLys
     *            - List of NcSoundingLayer of sounding data layer
     * 
     * @param lowerPress
     *            - Bottom level of layer (mb) [ -1=SFC]
     * 
     * @param upperPress
     *            - Top level of layer (mb) [ -1=TOP]
     * 
     * @Return Parcel - created Parcel data structure with parcel weather
     *         parameters computed
     */
    {

        if (sndLys == null || sndLys.size() < 1)
            return null;

        float sfcPres, mmr, mtha;
        NcSoundingLayer sfcLy;
        float liftParcelPres = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        float liftParcelTemp = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        float liftParcelDwpt = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        sfcLy = NsharpLibBasics.sfc(sndLys);
        switch (parcelFlag) {
        case NsharpLibSndglib.PARCELTYPE_OBS_SFC:
            if (sfcLy != null) {
                liftParcelTemp = sfcLy.getTemperature();
                liftParcelPres = sfcLy.getPressure();
                liftParcelDwpt = sfcLy.getDewpoint();
            }
            break;

        case NsharpLibSndglib.PARCELTYPE_FCST_SFC:
            liftParcelTemp = max_temp(sndLys, -1);
            mmr = mean_mixratio(sndLys, -1, -1);
            sfcPres = NsharpLibBasics.sfcPressure(sndLys);
            liftParcelDwpt = NsharpLibThermo.temp_at_mixrat(mmr, sfcPres);
            liftParcelPres = sfcPres;
            break;

        case NsharpLibSndglib.PARCELTYPE_MOST_UNSTABLE:
            sfcPres = NsharpLibBasics.sfcPressure(sndLys);
            float muPrese;
            if (sfcPres <= NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA) {
                muPrese = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
            } else {
                muPrese = unstbl_lvl(sndLys, -1, sfcPres - parcelPres);
            }
            liftParcelPres = muPrese;
            liftParcelTemp = NsharpLibBasics.i_temp(sndLys, muPrese);
            liftParcelDwpt = NsharpLibBasics.i_dwpt(sndLys, muPrese);
            break;

        case NsharpLibSndglib.PARCELTYPE_MEAN_MIXING:
            sfcPres = NsharpLibBasics.sfcPressure(sndLys);
            mtha = mean_theta(sndLys, -1, sfcPres - parcelPres);
            liftParcelTemp = NsharpLibThermo.theta(1000.0f, mtha, sfcPres);
            mmr = mean_mixratio(sndLys, -1, sfcPres - parcelPres);
            liftParcelDwpt = NsharpLibThermo.temp_at_mixrat(mmr, sfcPres);
            liftParcelPres = sfcPres;
            break;

        case NsharpLibSndglib.PARCELTYPE_USER_DEFINED:
            liftParcelTemp = NsharpLibBasics.i_temp(sndLys, parcelPres);
            liftParcelDwpt = NsharpLibBasics.i_dwpt(sndLys, parcelPres);
            liftParcelPres = parcelPres;
            break;

        case NsharpLibSndglib.PARCELTYPE_EFF:
            float p_top = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA,
            p_bot = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
            /*
             * port effective_inflow_layer_thermo(float ecape, float ecinh,
             * float *bot, float *top) to the following section to avoid
             * recursive call to this function, define_parcel(). When original
             * code make call to effective_inflow_layer_thermo(), its statement
             * is effective_inflow_layer_thermo(100,-250, &p_bot, &p_top);
             */
            // define ecape and ecinh according to original code
            float ecape = 100f,
            ecinh = -250f;
            sfcPres = NsharpLibBasics.sfcPressure(sndLys);
            if (sfcPres != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA) {
                muPrese = unstbl_lvl(sndLys, -1, sfcPres - parcelPres);
                float muTemp = NsharpLibBasics.i_temp(sndLys, muPrese);
                float muDew = NsharpLibBasics.i_dwpt(sndLys, muPrese);
                Parcel pcl = parcel(sndLys, -1, -1, muPrese, muTemp, muDew);
                float mucape = pcl.getBplus();
                float mucin = pcl.getBminus();
                if (mucape >= 100 && mucin > -250) {
                    /*
                     * ----- Begin at surface and search upward for
                     * "Effective Surface" -----
                     */
                    int sfcIndex = NsharpLibBasics.sfcIndex(sndLys);
                    if (sfcIndex >= 0) {
                        for (int i = sfcIndex; i <= sndLys.size() - 1; i++) {
                            pcl = parcel(sndLys, -1, -1, sndLys.get(i)
                                    .getPressure(), sndLys.get(i)
                                    .getTemperature(), sndLys.get(i)
                                    .getDewpoint());
                            if ((pcl.getBplus() >= ecape)
                                    && (pcl.getBminus() >= ecinh)) {
                                p_bot = sndLys.get(i).getPressure();
                                break;
                            }
                        }
                    }
                    if (p_bot != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA) {
                        /*
                         * ----- Keep searching upward for the "Effective Top"
                         * -----
                         */
                        if (sfcIndex >= 0) {
                            for (int i = sfcIndex; i <= sndLys.size() - 1; i++) {
                                if (sndLys.get(i).getPressure() <= p_bot) {
                                    pcl = parcel(sndLys, -1, -1, sndLys.get(i)
                                            .getPressure(), sndLys.get(i)
                                            .getTemperature(), sndLys.get(i)
                                            .getDewpoint());
                                    if (((pcl.getBplus() <= ecape) || (pcl
                                            .getBminus() <= ecinh)) && i > 0) {
                                        p_top = sndLys.get(i - 1).getPressure();
                                        break;
                                    }

                                }
                            }
                        }
                    }
                }
            }

            if (p_bot > 0 && p_top > 0) {
                mtha = mean_theta(sndLys, p_bot, p_top);
                liftParcelTemp = NsharpLibThermo.theta(1000.0f, mtha,
                        (p_top + p_bot) / 2);
                mmr = mean_mixratio(sndLys, p_bot, p_top);
                liftParcelDwpt = NsharpLibThermo.temp_at_mixrat(mmr,
                        (p_top + p_bot) / 2);
                liftParcelPres = (p_top + p_bot) / 2;
            } else { // p_bot= NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA
                if (sfcLy != null) {
                    liftParcelTemp = sfcLy.getTemperature();
                    liftParcelPres = sfcLy.getPressure();
                    liftParcelDwpt = sfcLy.getDewpoint();
                }
            }
            break;
        }
        // code above is original define_parcel() code and
        // original parcel() code starting here
        int i;
        float te1, pe1, te2 = 0, pe2 = 0, h1, h2, lyre, tdef1, tdef2, totp, totn;
        float te3, pe3, h3, tp1, tp2 = 0, tp3, tdef3, lyrf, lyrlast, pelast;
        float tote = 0, totx, cap_strength, li_max, li_maxpres, blupper;
        float cap_strengthpres, pp, pp1, pp2, dz;
        float theta_parcel, tv_env_bot, tv_env_top, lclhght, blmr;

        Parcel rtnParcel = new Parcel();
        // initialize some fields in Parcel
        rtnParcel.setLplpres(liftParcelPres);
        rtnParcel.setLpltemp(liftParcelTemp);
        rtnParcel.setLpldwpt(liftParcelDwpt);
        rtnParcel.setBlayer(lowerPress);
        rtnParcel.setTlayer(upperPress);
        lyre = -1;
        cap_strength = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        cap_strengthpres = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        li_max = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        li_maxpres = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        totp = 0;
        totn = 0;

        /* ----- See if default layer is specified ----- */
        if (lowerPress == -1) {
            if (sfcLy != null) {
                lowerPress = sfcLy.getPressure();
                rtnParcel.setBlayer(lowerPress);
            } else {
                return null;
            }
        }
        if (upperPress == -1) {
            upperPress = sndLys.get(sndLys.size() - 1).getPressure();
            if (upperPress == NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA) {
                return null;
            }
            rtnParcel.setTlayer(upperPress);
        }
        /* ----- Make sure this is a valid layer ----- */
        if (lowerPress > liftParcelPres) {
            lowerPress = liftParcelPres;
            rtnParcel.setBlayer(lowerPress);
        }

        if (!NsharpLibBasics.qc(NsharpLibBasics.i_vtmp(sndLys, upperPress))
                || !NsharpLibBasics.qc(NsharpLibBasics.i_vtmp(sndLys,
                        lowerPress))) {
            return null;
        }

        /* ----- Begin with Mixing Layer (LPL-LCL) ----- */
        te1 = NsharpLibBasics.i_vtmp(sndLys, liftParcelPres);
        pe1 = lowerPress;
        h1 = NsharpLibBasics.i_hght(sndLys, pe1);
        tp1 = NsharpLibThermo.virtemp(liftParcelPres, liftParcelTemp,
                liftParcelDwpt);

        /* lift parcel and return LCL pres (mb) and LCL temp (C) */
        LayerParameters sndLy = NsharpLibThermo.drylift(liftParcelPres,
                liftParcelTemp, liftParcelDwpt);
        if (sndLy != null) {
            pe2 = sndLy.getPressure();
            tp2 = sndLy.getTemperature();
        } else {
            pe2 = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
            tp2 = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        /* define top of layer as LCL pres */
        blupper = pe2;
        h2 = NsharpLibBasics.i_hght(sndLys, pe2);
        te2 = NsharpLibBasics.i_vtmp(sndLys, pe2);
        rtnParcel.setLclpres(pe2);
        rtnParcel.setLclAgl(NsharpLibBasics.mtof(NsharpLibBasics.agl(sndLys,
                NsharpLibBasics.i_hght(sndLys, pe2))));

        /* calculate lifted parcel theta for use in iterative CINH loop below */
        /* recall that lifted parcel theta is CONSTANT from LPL to LCL */
        theta_parcel = NsharpLibThermo.theta(pe2, tp2, 1000.0f);

        /* environmental theta and mixing ratio at LPL */
        blmr = NsharpLibThermo.mixratio(liftParcelPres, liftParcelDwpt);

        /* ----- Accumulate CINH in mixing layer below the LCL ----- */
        /* This will be done in 10mb increments, and will use the */
        /* virtual temperature correction where possible. */
        /*
         * initialize negative area to zero and iterate from LPL to LCL in 10mb
         * increments
         */
        totn = 0;
        for (pp = lowerPress; pp > blupper; pp -= 10.0) {
            pp1 = pp;
            pp2 = pp - 10.0f;
            if (pp2 < blupper)
                pp2 = blupper;

            dz = NsharpLibBasics.i_hght(sndLys, pp2)
                    - NsharpLibBasics.i_hght(sndLys, pp1);

            /*
             * calculate difference between Tv_parcel and Tv_environment at top
             * and bottom of 10mb layers
             */
            /* make use of constant lifted parcel theta and mixr from LPL to LCL */
            tv_env_bot = NsharpLibThermo.virtemp(
                    pp1,
                    NsharpLibThermo.theta(pp1,
                            NsharpLibBasics.i_temp(sndLys, pp1), 1000.0f),
                    NsharpLibBasics.i_dwpt(sndLys, pp1));
            tdef1 = ((NsharpLibThermo.virtemp(pp1, theta_parcel,
                    NsharpLibThermo.temp_at_mixrat(blmr, pp1))) - tv_env_bot)
                    / (tv_env_bot + 273.15f);

            tv_env_top = NsharpLibThermo.virtemp(
                    pp2,
                    NsharpLibThermo.theta(pp2,
                            NsharpLibBasics.i_temp(sndLys, pp2), 1000.0f),
                    NsharpLibBasics.i_dwpt(sndLys, pp2));
            tdef2 = ((NsharpLibThermo.virtemp(pp2, theta_parcel,
                    NsharpLibThermo.temp_at_mixrat(blmr, pp2))) - tv_env_top)
                    / (tv_env_top + 273.15f);

            lyre = 9.8f * (tdef1 + tdef2) / 2.0f * dz;
            if (lyre < 0)
                totn += lyre;
        }

        if (lowerPress > pe2) {
            lowerPress = pe2;
            rtnParcel.setBlayer(lowerPress);
        }

        te1 = NsharpLibBasics.i_vtmp(sndLys, liftParcelPres);
        pe1 = lowerPress;
        h1 = NsharpLibBasics.i_hght(sndLys, pe1);
        tp1 = NsharpLibThermo.virtemp(liftParcelPres, liftParcelTemp,
                liftParcelDwpt);

        sndLy = NsharpLibThermo.drylift(liftParcelPres, liftParcelTemp,
                liftParcelDwpt);
        if (sndLy != null) {
            pe2 = sndLy.getPressure();
            tp2 = sndLy.getTemperature();
        } else {
            pe2 = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
            tp2 = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        h2 = NsharpLibBasics.i_hght(sndLys, pe2);
        te2 = NsharpLibBasics.i_vtmp(sndLys, pe2);

        /* ----- Find lowest observation in layer ----- */
        int lowIndex = sndLys.size() - 1;
        for (i = 0; i < sndLys.size(); i++) {
            if (sndLys.get(i).getPressure() < lowerPress
                    && NsharpLibBasics.qc(sndLys.get(i).getDewpoint())) {
                lowIndex = i;
                break;
            }
        }

        /* ----- Find highest observation in layer ----- */
        int upIndex = 0;
        for (i = sndLys.size() - 1; i >= 0; i--) {
            if (sndLys.get(i).getPressure() > upperPress) {
                upIndex = i;
                break;
            }
        }

        /* ----- Start with interpolated bottom layer ----- */
        /* begin moist ascent from lifted parcel LCL (pe2, tp2) */
        pe1 = lowerPress;
        h1 = NsharpLibBasics.i_hght(sndLys, pe1);
        te1 = NsharpLibBasics.i_vtmp(sndLys, pe1);
        tp1 = NsharpLibThermo.wetlift(pe2, tp2, pe1);
        totp = 0;
        lyre = 0;
        for (i = lowIndex; i < sndLys.size(); i++) {
            if (NsharpLibBasics.qc(sndLys.get(i).getTemperature())) {
                /* ----- Calculate every level that reports a temp ----- */
                pe2 = sndLys.get(i).getPressure();
                h2 = sndLys.get(i).getGeoHeight();
                te2 = NsharpLibBasics.i_vtmp(sndLys, pe2);
                tp2 = NsharpLibThermo.wetlift(pe1, tp1, pe2);
                tdef1 = (NsharpLibThermo.virtemp(pe1, tp1, tp1) - te1)
                        / (te1 + 273.15f);
                tdef2 = (NsharpLibThermo.virtemp(pe2, tp2, tp2) - te2)
                        / (te2 + 273.15f);
                lyrlast = lyre;
                lyre = 9.8f * (tdef1 + tdef2) / 2.0f * (h2 - h1);

                /* ----- Check for Max LI ----- */
                if ((NsharpLibThermo.virtemp(pe2, tp2, tp2) - te2) > li_max) {
                    li_max = NsharpLibThermo.virtemp(pe2, tp2, tp2) - te2;
                    li_maxpres = pe2;
                }

                /* ----- Check for Max Cap Strength ----- */
                if ((te2 - NsharpLibThermo.virtemp(pe2, tp2, tp2)) > cap_strength) {
                    cap_strength = te2 - NsharpLibThermo.virtemp(pe2, tp2, tp2);
                    cap_strengthpres = pe2;
                }

                /* add layer energy to total positive if lyre > 0 */
                if (lyre > 0) {
                    totp += lyre;
                }
                /*
                 * add layer energy to total negative if lyre < 0, only up to
                 * the EL
                 */
                else {
                    if (pe2 > 500.0) {
                        totn += lyre;
                    }
                }

                tote += lyre;
                pelast = pe1;

                pe1 = pe2;
                h1 = h2;
                te1 = te2;
                tp1 = tp2;

                /* ----- Is this the top of given layer ----- */
                if ((i >= upIndex)
                        && (!NsharpLibBasics.qc(rtnParcel.getBplus()))) {
                    pe3 = pe1;
                    h3 = h1;
                    te3 = te1;
                    tp3 = tp1;
                    lyrf = lyre;

                    if (lyrf > 0.0) {
                        rtnParcel.setBplus(totp - lyrf);
                        rtnParcel.setBminus(totn);
                    } else {
                        rtnParcel.setBplus(totp);
                        if (pe2 > 500.0) {
                            rtnParcel.setBminus(totn + lyrf);
                        } else {
                            rtnParcel.setBminus(totn);
                        }
                    }

                    pe2 = upperPress;
                    h2 = NsharpLibBasics.i_hght(sndLys, pe2);
                    te2 = NsharpLibBasics.i_vtmp(sndLys, pe2);
                    tp2 = NsharpLibThermo.wetlift(pe3, tp3, pe2);
                    tdef3 = (NsharpLibThermo.virtemp(pe3, tp3, tp3) - te3)
                            / (te3 + 273.15f);
                    tdef2 = (NsharpLibThermo.virtemp(pe2, tp2, tp2) - te2)
                            / (te2 + 273.15f);
                    lyrf = 9.8f * (tdef3 + tdef2) / 2.0f * (h2 - h3);
                    if (lyrf > 0.0f) {
                        rtnParcel.setBplus(rtnParcel.getBplus() + lyrf);
                    } else {
                        if (pe2 > 600.0) {
                            rtnParcel.setBminus(rtnParcel.getBminus() + lyrf);
                        }
                    }
                    if (rtnParcel.getBplus() == 0.0) {
                        rtnParcel.setBminus(0);
                    }
                }

                /* ----- Is this the freezing level ----- */
                if ((te2 <= 0.0) && !NsharpLibBasics.qc(rtnParcel.getBfzl())) {
                    pe3 = pelast;
                    h3 = NsharpLibBasics.i_hght(sndLys, pe3);
                    te3 = NsharpLibBasics.i_vtmp(sndLys, pe3);
                    tp3 = NsharpLibThermo.wetlift(pe1, tp1, pe3);
                    lyrf = lyre;

                    if (lyrf > 0.0) {
                        rtnParcel.setBfzl(totp - lyrf);
                    } else {
                        rtnParcel.setBfzl(totp);
                    }

                    /*
                     * error check for LCL temp colder than 0 C by Patrick Marsh
                     * and RLT 1/6/12
                     */
                    pe2 = temp_lvl(sndLys, 0f);
                    if (pe2 > rtnParcel.getLclpres()) {
                        rtnParcel.setBfzl(0.0f);
                    } else if (NsharpLibBasics.qc(pe2)) {
                        h2 = NsharpLibBasics.i_hght(sndLys, pe2);
                        te2 = NsharpLibBasics.i_vtmp(sndLys, pe2);
                        tp2 = NsharpLibThermo.wetlift(pe3, tp3, pe2);
                        tdef3 = (NsharpLibThermo.virtemp(pe3, tp3, tp3) - te3)
                                / (te3 + 273.15f);
                        tdef2 = (NsharpLibThermo.virtemp(pe2, tp2, tp2) - te2)
                                / (te2 + 273.15f);
                        lyrf = 9.8f * (tdef3 + tdef2) / 2.0f * (h2 - h3);
                        if (lyrf > 0.0) {
                            rtnParcel.setBfzl(rtnParcel.getBfzl() + lyrf);
                        }
                    }
                }

                /* ----- Is this the -10c level ----- */
                if ((te2 <= -10.0) && !NsharpLibBasics.qc(rtnParcel.getWm10c())) {
                    pe3 = pelast;
                    h3 = NsharpLibBasics.i_hght(sndLys, pe3);
                    te3 = NsharpLibBasics.i_vtmp(sndLys, pe3);
                    tp3 = NsharpLibThermo.wetlift(pe1, tp1, pe3);
                    lyrf = lyre;

                    if (lyrf > 0.0) {
                        rtnParcel.setWm10c(totp - lyrf);
                    } else {
                        rtnParcel.setWm10c(totp);
                    }
                    /*
                     * error check for LCL temp colder than -10 C by Patrick
                     * Marsh and RLT 1/6/12
                     */
                    pe2 = temp_lvl(sndLys, -10.0f);
                    if (pe2 > rtnParcel.getLclpres()) {
                        rtnParcel.setWm10c(0);
                    } else if (NsharpLibBasics.qc(pe2)) {
                        h2 = NsharpLibBasics.i_hght(sndLys, pe2);
                        te2 = NsharpLibBasics.i_vtmp(sndLys, pe2);
                        tp2 = NsharpLibThermo.wetlift(pe3, tp3, pe2);
                        tdef3 = (NsharpLibThermo.virtemp(pe3, tp3, tp3) - te3)
                                / (te3 + 273.15f);
                        tdef2 = (NsharpLibThermo.virtemp(pe2, tp2, tp2) - te2)
                                / (te2 + 273.15f);
                        lyrf = 9.8f * (tdef3 + tdef2) / 2.0f * (h2 - h3);
                        if (lyrf > 0.0) {
                            rtnParcel.setWm10c(rtnParcel.getWm10c() + lyrf);
                        }
                    }
                }

                /* ----- Is this the -20c level ----- */
                if ((te2 <= -20.0) && !NsharpLibBasics.qc(rtnParcel.getWm20c())) {
                    pe3 = pelast;
                    h3 = NsharpLibBasics.i_hght(sndLys, pe3);
                    te3 = NsharpLibBasics.i_vtmp(sndLys, pe3);
                    tp3 = NsharpLibThermo.wetlift(pe1, tp1, pe3);
                    lyrf = lyre;

                    if (lyrf > 0.0) {
                        rtnParcel.setWm20c(totp - lyrf);
                    } else {
                        rtnParcel.setWm20c(totp);
                    }
                    /*
                     * error check for LCL temp colder than -20 C by Patrick
                     * Marsh and RLT 1/6/12
                     */
                    pe2 = temp_lvl(sndLys, -20.0f);
                    if (pe2 > rtnParcel.getLclpres()) {
                        rtnParcel.setWm20c(0);
                    } else if (NsharpLibBasics.qc(pe2)) {
                        h2 = NsharpLibBasics.i_hght(sndLys, pe2);
                        te2 = NsharpLibBasics.i_vtmp(sndLys, pe2);
                        tp2 = NsharpLibThermo.wetlift(pe3, tp3, pe2);
                        tdef3 = (NsharpLibThermo.virtemp(pe3, tp3, tp3) - te3)
                                / (te3 + 273.15f);
                        tdef2 = (NsharpLibThermo.virtemp(pe2, tp2, tp2) - te2)
                                / (te2 + 273.15f);
                        lyrf = 9.8f * (tdef3 + tdef2) / 2.0f * (h2 - h3);
                        if (lyrf > 0.0) {
                            rtnParcel.setWm20c(rtnParcel.getWm20c() + lyrf);
                        }
                    }
                }

                /* ----- Is this the -30c level ----- */
                if ((te2 <= -30.0) && !NsharpLibBasics.qc(rtnParcel.getWm30c())) {
                    pe3 = pelast;
                    h3 = NsharpLibBasics.i_hght(sndLys, pe3);
                    te3 = NsharpLibBasics.i_vtmp(sndLys, pe3);
                    tp3 = NsharpLibThermo.wetlift(pe1, tp1, pe3);
                    lyrf = lyre;
                    if (lyrf > 0.0) {
                        rtnParcel.setWm30c(totp - lyrf);
                    } else {
                        rtnParcel.setWm30c(totp);
                    }

                    /*
                     * error check for LCL temp colder than -30 C by Patrick
                     * Marsh and RLT 1/6/12
                     */
                    pe2 = temp_lvl(sndLys, -30.0f);
                    if (pe2 > rtnParcel.getLclpres()) {
                        rtnParcel.setWm30c(0);
                    } else if (NsharpLibBasics.qc(pe2)) {
                        h2 = NsharpLibBasics.i_hght(sndLys, pe2);
                        te2 = NsharpLibBasics.i_vtmp(sndLys, pe2);
                        tp2 = NsharpLibThermo.wetlift(pe3, tp3, pe2);
                        tdef3 = (NsharpLibThermo.virtemp(pe3, tp3, tp3) - te3)
                                / (te3 + 273.15f);
                        tdef2 = (NsharpLibThermo.virtemp(pe2, tp2, tp2) - te2)
                                / (te2 + 273.15f);
                        lyrf = 9.8f * (tdef3 + tdef2) / 2.0f * (h2 - h3);
                        if (lyrf > 0.0) {
                            rtnParcel.setWm30c(rtnParcel.getWm30c() + lyrf);
                        }
                    }
                }

                /* ----- Is this the 3km level ----- */
                lclhght = NsharpLibBasics.agl(sndLys,
                        NsharpLibBasics.i_hght(sndLys, rtnParcel.getLclpres()));
                if (lclhght < 3000) {
                    if ((NsharpLibBasics.agl(sndLys,
                            NsharpLibBasics.i_hght(sndLys, pe2)) >= 3000.0f)
                            && (!NsharpLibBasics.qc(rtnParcel.getCape3km()))) {
                        pe3 = pelast;
                        h3 = NsharpLibBasics.i_hght(sndLys, pe3);
                        te3 = NsharpLibBasics.i_vtmp(sndLys, pe3);
                        tp3 = NsharpLibThermo.wetlift(pe1, tp1, pe3);
                        lyrf = lyre;

                        if (lyrf > 0.0) {
                            rtnParcel.setCape3km(totp - lyrf);
                        } else {
                            rtnParcel.setCape3km(totp);
                        }
                        pe2 = NsharpLibBasics.i_pres(sndLys,
                                NsharpLibBasics.msl(sndLys, 3000f));
                        if (NsharpLibBasics.qc(pe2)) {
                            h2 = NsharpLibBasics.msl(sndLys, 3000.0f);
                            te2 = NsharpLibBasics.i_vtmp(sndLys, pe2);
                            tp2 = NsharpLibThermo.wetlift(pe3, tp3, pe2);
                            tdef3 = (NsharpLibThermo.virtemp(pe3, tp3, tp3) - te3)
                                    / (te3 + 273.15f);
                            tdef2 = (NsharpLibThermo.virtemp(pe2, tp2, tp2) - te2)
                                    / (te2 + 273.15f);
                            lyrf = 9.8f * (tdef3 + tdef2) / 2.0f * (h2 - h3);
                            if (lyrf > 0.0) {
                                rtnParcel.setCape3km(rtnParcel.getCape3km()
                                        + lyrf);
                            }
                        }
                    }
                } else {
                    rtnParcel.setCape3km(0.0f);
                }
                /* ----- Is this the 6km level ----- */
                if (lclhght < 6000) {
                    if ((NsharpLibBasics.agl(sndLys,
                            NsharpLibBasics.i_hght(sndLys, pe2)) >= 6000.0f)
                            && (!NsharpLibBasics.qc(rtnParcel.getCape6km()))) {
                        pe3 = pelast;
                        h3 = NsharpLibBasics.i_hght(sndLys, pe3);
                        te3 = NsharpLibBasics.i_vtmp(sndLys, pe3);
                        tp3 = NsharpLibThermo.wetlift(pe1, tp1, pe3);
                        lyrf = lyre;

                        if (lyrf > 0.0) {
                            rtnParcel.setCape6km(totp - lyrf);
                        } else {
                            rtnParcel.setCape6km(totp);
                        }

                        pe2 = NsharpLibBasics.i_pres(sndLys,
                                NsharpLibBasics.msl(sndLys, 6000f));
                        if (NsharpLibBasics.qc(pe2)) {
                            h2 = NsharpLibBasics.msl(sndLys, 6000.0f);
                            te2 = NsharpLibBasics.i_vtmp(sndLys, pe2);
                            tp2 = NsharpLibThermo.wetlift(pe3, tp3, pe2);
                            tdef3 = (NsharpLibThermo.virtemp(pe3, tp3, tp3) - te3)
                                    / (te3 + 273.15f);
                            tdef2 = (NsharpLibThermo.virtemp(pe2, tp2, tp2) - te2)
                                    / (te2 + 273.15f);
                            lyrf = 9.8f * (tdef3 + tdef2) / 2.0f * (h2 - h3);
                            if (lyrf > 0.0) {
                                rtnParcel.setCape6km(rtnParcel.getCape6km()
                                        + lyrf);
                            }
                        }
                    }
                } else {
                    rtnParcel.setCape6km(0.0f);
                }

                /* ----- LFC Possibility ----- */
                if ((lyre >= 0) && (lyrlast <= 0)) {
                    tp3 = tp1;
                    te3 = te1;
                    pe2 = pe1;
                    pe3 = pelast;
                    while (NsharpLibBasics.i_vtmp(sndLys, pe3) > NsharpLibThermo
                            .virtemp(pe3,
                                    NsharpLibThermo.wetlift(pe2, tp3, pe3),
                                    NsharpLibThermo.wetlift(pe2, tp3, pe3))) {
                        pe3 -= 5;
                    }
                    rtnParcel.setLfcpres(pe3);
                    rtnParcel.setLfcTemp(NsharpLibBasics.i_temp(sndLys, pe3));
                    rtnParcel.setLfcAgl(NsharpLibBasics.mtof(NsharpLibBasics
                            .agl(sndLys, NsharpLibBasics.i_hght(sndLys, pe3))));
                    rtnParcel
                            .setElpres(NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA);
                    // cinh_old = totn; // Chin: not used

                    tote = 0;
                    li_max = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

                    if (cap_strength < 0.0) {
                        cap_strength = 0.0f;
                    }
                    rtnParcel.setCap(cap_strength);
                    rtnParcel.setCappres(cap_strengthpres);
                }

                /* ----- EL Possibility ----- */
                if ((lyre <= 0.0) && (lyrlast >= 0.0)) {
                    tp3 = tp1;
                    te3 = te1;
                    pe2 = pe1;
                    pe3 = pelast;
                    while (NsharpLibBasics.i_vtmp(sndLys, pe3) < NsharpLibThermo
                            .virtemp(pe3,
                                    NsharpLibThermo.wetlift(pe2, tp3, pe3),
                                    NsharpLibThermo.wetlift(pe2, tp3, pe3))) {
                        pe3 -= 5.0;
                    }

                    rtnParcel.setElpres(pe3);
                    rtnParcel.setElTemp(NsharpLibBasics.i_temp(sndLys, pe3));
                    rtnParcel.setElAgl(NsharpLibBasics.mtof(NsharpLibBasics
                            .agl(sndLys, NsharpLibBasics.i_hght(sndLys, pe3))));
                    rtnParcel.setLimax(-li_max);
                    rtnParcel.setLimaxpres(li_maxpres);
                    rtnParcel
                            .setMplpres(NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA);
                }

                /* ----- MPL Possibility ----- */
                if (((tote <= 0.0)
                        && (!NsharpLibBasics.qc(rtnParcel.getMplpres())) && (NsharpLibBasics
                            .qc(rtnParcel.getElpres())))) {
                    pe3 = pelast;
                    h3 = NsharpLibBasics.i_hght(sndLys, pe3);
                    te3 = NsharpLibBasics.i_vtmp(sndLys, pe3);
                    tp3 = NsharpLibThermo.wetlift(pe1, tp1, pe3);
                    totx = tote - lyre;
                    pe2 = pelast;
                    while (totx > 0.0) {
                        pe2 -= 1;
                        te2 = NsharpLibBasics.i_vtmp(sndLys, pe2);
                        tp2 = NsharpLibThermo.wetlift(pe3, tp3, pe2);
                        h2 = NsharpLibBasics.i_hght(sndLys, pe2);
                        tdef3 = (NsharpLibThermo.virtemp(pe3, tp3, tp3) - te3)
                                / (te3 + 273.15f);
                        tdef2 = (NsharpLibThermo.virtemp(pe2, tp2, tp2) - te2)
                                / (te2 + 273.15f);
                        lyrf = 9.8f * (tdef3 + tdef2) / 2.0f * (h2 - h3);
                        totx += lyrf;

                        tp3 = tp2;
                        te3 = te2;
                        pe3 = pe2;
                    }
                    rtnParcel.setMplpres(pe2);
                    rtnParcel.setMplAgl(NsharpLibBasics.mtof(NsharpLibBasics
                            .agl(sndLys, NsharpLibBasics.i_hght(sndLys, pe2))));
                }
                /* ----- 500mb Lifted Index ----- */
                if ((sndLys.get(i).getPressure() <= 500)
                        && (rtnParcel.getLi5() == NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA)) {
                    float li5 = NsharpLibBasics.i_vtmp(sndLys, 500)
                            - NsharpLibThermo.virtemp(500,
                                    NsharpLibThermo.wetlift(pe1, tp1, 500),
                                    NsharpLibThermo.wetlift(pe1, tp1, 500));

                    rtnParcel.setLi5(li5);
                }

                /* ----- 300mb Lifted Index ----- */
                if ((sndLys.get(i).getPressure() <= 300)
                        && (rtnParcel.getLi3() == NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA)) {
                    float li3 = NsharpLibBasics.i_vtmp(sndLys, 300)
                            - NsharpLibThermo.virtemp(300,
                                    NsharpLibThermo.wetlift(pe1, tp1, 300),
                                    NsharpLibThermo.wetlift(pe1, tp1, 300));
                    rtnParcel.setLi3(li3);
                }
            } /* end if (NsharpLibBasics.qc(sndLys.get(i).getTemperature())) */
        } /* end for(i = lowIndex; i < sndLys.size(); i++) */

        // Chin: do not know why set bminus here, as cinh_old may not be
        // initialized, and bminus is set earlier
        // rtnParcel.setBminus(cinh_old);
        if (rtnParcel.getBplus() == 0.0) {
            rtnParcel.setBminus(0);
        }
        return rtnParcel;
    }

    public static float wb_lvl(List<NcSoundingLayer> sndLys, float temp)
    /*************************************************************/
    /* WB_LVL */
    /*                                                           */
    /* Calculates the level (mb) of the given Wet-bulb */
    /* temperature. */
    /* Input: */
    /* temp : Wet-bulb temperature (c) to search for. */
    /* Return: */
    /* wet-bulb level pressure */
    /*************************************************************/
    {

        for (int i = 0; i < sndLys.size(); i++) {
            NcSoundingLayer sndLayer1 = sndLys.get(i);
            float p1 = sndLayer1.getPressure();
            float t1 = sndLayer1.getTemperature();
            float d1 = sndLayer1.getDewpoint();

            if (NsharpLibBasics.qc(t1) && NsharpLibBasics.qc(d1)) {
                float wb1 = NsharpLibThermo.wetbulb(p1, t1, d1);
                if (wb1 < temp) {
                    if (i == 0) {
                        return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
                    }
                    if (wb1 == temp) {
                        return p1;
                    }
                    NcSoundingLayer sndLayer0 = sndLys.get(i - 1);
                    float p0 = sndLayer0.getPressure();
                    float t0 = sndLayer0.getTemperature();
                    float d0 = sndLayer0.getDewpoint();
                    float wb0 = NsharpLibThermo.wetbulb(p0, t0, d0);
                    float nm1 = temp - wb0;
                    float nm2 = wb1 - wb0;
                    float nm3 = (float) Math.log(p1 / p0);
                    return (float) (p0 * Math.exp((nm1 / nm2) * nm3));
                }
            }
        }
        return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
    }

    public static float lapse_rate(List<NcSoundingLayer> sndLys, float lower,
            float upper)
    /*************************************************************/
    /* LAPSE_RATE */
    /*                                                           */
    /* Calculates the lapse rate (C/km) from sounding data list */
    /* Value is returned. */
    /*************************************************************/
    {

        if (!NsharpLibBasics.qc(NsharpLibBasics.i_vtmp(sndLys, lower))
                || !NsharpLibBasics.qc(NsharpLibBasics.i_vtmp(sndLys, upper))) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        } else {
            float dt, dz;
            dt = NsharpLibBasics.i_vtmp(sndLys, upper)
                    - NsharpLibBasics.i_vtmp(sndLys, lower);
            dz = NsharpLibBasics.i_hght(sndLys, upper)
                    - NsharpLibBasics.i_hght(sndLys, lower);
            return ((dt / dz) * -1000.0f);
        }

    }

    public static float k_index(List<NcSoundingLayer> sndLys)
    /*************************************************************/
    /* K_INDEX */
    /*                                                           */
    /* Calculates the K-Index from sounding data list */
    /* Value is returned. */
    /*************************************************************/
    {
        float t850, t500, t700, td700, td850;
        t850 = NsharpLibBasics.i_temp(sndLys, 850.0f);
        t700 = NsharpLibBasics.i_temp(sndLys, 700.0f);
        t500 = NsharpLibBasics.i_temp(sndLys, 500.0f);
        td700 = NsharpLibBasics.i_dwpt(sndLys, 700.0f);
        td850 = NsharpLibBasics.i_dwpt(sndLys, 850.0f);

        if (!NsharpLibBasics.qc(t850) || !NsharpLibBasics.qc(td850)
                || !NsharpLibBasics.qc(t700) || !NsharpLibBasics.qc(td700)
                || !NsharpLibBasics.qc(t500)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        } else {
            return (t850 - t500 + td850 - (t700 - td700));
        }

    }

    public static float Mean_thetae(List<NcSoundingLayer> sndLys, float lower,
            float upper)
    /*************************************************************/
    /* MEAN_THETAE */
    /*                                                           */
    /* Computes the average ThetaE temperature between the */
    /* two given levels (lower,upper) */
    /*                                                           */
    /* lower = Bottom level of layer (mb) [ -1=WBZ+50] */
    /* upper = Top level of layer (mb) [ -1=WBZ-50] */
    /*************************************************************/
    {
        float num, eqPotenTemp1, eqPotenTemp2, totd, dbar, p1, p2, pbar, totp;

        /* ----- See if default layer is specified ----- */
        if (lower == -1) {
            lower = wb_lvl(sndLys, 0) + 50.0f;
        }
        if (upper == -1) {
            upper = wb_lvl(sndLys, 0) - 50.0f;
        }
        /* ----- Make sure this is a valid layer ----- */
        if (!NsharpLibBasics.qc(NsharpLibBasics.i_dwpt(sndLys, upper))) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        if (!NsharpLibBasics.qc(NsharpLibBasics.i_dwpt(sndLys, lower))) {
            lower = NsharpLibBasics.sfcPressure(sndLys);
        }

        int lowIndex = 0, upperIndex = 0;
        /* ----- Find lowest observation in layer ----- */
        boolean foundLow = false;
        for (int i = 0; i < sndLys.size(); i++) {
            if (NsharpLibBasics.qc(sndLys.get(i).getPressure())
                    && NsharpLibBasics.qc(sndLys.get(i).getTemperature())
                    && sndLys.get(i).getPressure() < lower) {
                lowIndex = i;
                foundLow = true;
                break;
            }
        }

        /* ----- Find highest observation in layer ----- */
        boolean foundHi = false;
        for (int i = sndLys.size() - 1; i >= 0; i--) {
            if (NsharpLibBasics.qc(sndLys.get(i).getPressure())
                    && sndLys.get(i).getPressure() > upper) {
                upperIndex = i;
                foundHi = true;
                break;
            }
        }

        /* ----- Start with interpolated bottom layer ----- */
        eqPotenTemp1 = NsharpLibThermo.thetae(lower,
                NsharpLibBasics.i_temp(sndLys, lower),
                NsharpLibBasics.i_dwpt(sndLys, lower));
        p1 = lower;
        num = 1;
        totd = 0;
        totp = 0;
        if(foundLow && foundHi){
        	for (int i = lowIndex; i <= upperIndex; i++) {
        		if (NsharpLibBasics.qc(sndLys.get(i).getDewpoint())) {
        			/* ----- Calculate every level that reports a temp ----- */
        			eqPotenTemp2 = NsharpLibThermo.thetae(sndLys.get(i)
        					.getPressure(), sndLys.get(i).getTemperature(), sndLys
        					.get(i).getDewpoint());
        			p2 = sndLys.get(i).getPressure();
        			dbar = (eqPotenTemp1 + eqPotenTemp2) / 2.0f;
        			pbar = (p1 + p2) / 2.0f;
        			totd = totd + dbar;
        			totp = totp + pbar;
        			eqPotenTemp1 = eqPotenTemp2;
        			p1 = p2;
        			num++;
        		}
        	}
        }

        /* ----- Finish with interpolated top layer ----- */
        eqPotenTemp2 = NsharpLibThermo.thetae(upper,
                NsharpLibBasics.i_temp(sndLys, upper),
                NsharpLibBasics.i_dwpt(sndLys, upper));
        p2 = upper;
        dbar = (eqPotenTemp1 + eqPotenTemp2) / 2.0f;
        pbar = (p1 + p2) / 2.0f;
        totd = totd + dbar;
        totp = totp + pbar;
        return totd / num;
    }

    public static float dcape(List<NcSoundingLayer> sndLys,
            LayerParameters layerParms)
    /*************************************************************/
    /* DCAPE */
    /*                                                           */
    /* Computes downdraft instability using a modified */
    /* DCAPE routine. Method determines the min ThetaE */
    /* in the lowest 400mb, and descends the parcel dry- */
    /* adiabatically to the surface. Resulting value in */
    /* J/kg. */
    /*                                                           */
    /* Input : */
    /* an instance of NsharpLibSndglib.LayerParameters,used to */
    /* save the following two parameters for caller */
    /* pressure = pressure (mb) of layer being descended. */
    /* temperature = Downdraft temperature at surface (C). */
    /* Returns : downdraft instability value in J/kg. */
    /*                                                           */
    /*************************************************************/
    {
        float te1, pe1, te2, pe2, h1, h2, lyre, tdef1, tdef2;
        float tp1, tp2, upper;
        float tote, mine, minep, ent2;

        // Chin: I think the following two lines from original code are
        // not relevant to our application. As dcape_entrain always be
        // value 0, in BigSharp application.
        // if ((int)dcape_entrain == -1)
        // dcape_entrain = ENTRAIN_DEFAULT;

        int highIndex = -1;
        /* ----- Find highest observation in layer ----- */
        float sfcPress = NsharpLibBasics.sfcPressure(sndLys);
        for (int i = sndLys.size() - 1; i >= 0; i--) {
            if (NsharpLibBasics.qc(sndLys.get(i).getPressure())
                    && sndLys.get(i).getPressure() > (sfcPress - 400)) {
                highIndex = i;
                break;
            }
        }
        /* ----- Find min ThetaE layer ----- */
        mine = 1000.0f;
        minep = -999.0f;
        for (int i = 0; i < highIndex; i++) {
            if (NsharpLibBasics.qc(sndLys.get(i).getDewpoint())
                    && (NsharpLibBasics.qc(NsharpLibBasics.i_temp(sndLys,
                            sndLys.get(i).getPressure() + 100)))) {
                float meanThetae = Mean_thetae(sndLys, sndLys.get(i)
                        .getPressure(), sndLys.get(i).getPressure() - 100);
                if (NsharpLibBasics.qc(meanThetae) && (meanThetae < mine)) {
                    mine = meanThetae;
                    minep = sndLys.get(i).getPressure() - 50.0f;
                }
            }
        }

        if (minep < 0) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }

        upper = minep;
        /* ----- Find highest observation in layer Again ----- */
        highIndex = -1;
        for (int i = sndLys.size() - 1; i >= 0; i--) {
            if (NsharpLibBasics.qc(sndLys.get(i).getPressure())
                    && sndLys.get(i).getPressure() > upper) {
                highIndex = i;
                break;
            }
        }
        tp1 = NsharpLibThermo.wetbulb(upper,
                NsharpLibBasics.i_temp(sndLys, upper),
                NsharpLibBasics.i_dwpt(sndLys, upper));

        pe1 = upper;
        te1 = NsharpLibBasics.i_temp(sndLys, pe1);
        h1 = NsharpLibBasics.i_hght(sndLys, pe1);
        tote = 0;
        lyre = 0;
        int sfcIndex = NsharpLibBasics.sfcIndex(sndLys);
        tp2 = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        if (sfcIndex >= 0) {
            for (int i = highIndex; i >= sfcIndex; i--) {
                pe2 = sndLys.get(i).getPressure();
                te2 = sndLys.get(i).getTemperature();
                h2 = sndLys.get(i).getGeoHeight();
                tp2 = NsharpLibThermo.wetlift(pe1, tp1, pe2);

                /* Account for Entrainment */
                /* dcape_entrain is global var - (.2 would be 20% per km) */
                // ent2 = dcape_entrain * ((h1 - h2) / 1000.0);
                // Chin, above 3 lines was original code and comment, however,
                // searching
                // through BigSharp original code, dcape_entrain only set as 0,
                // therefore, ent2 would be 0.
                ent2 = 0;
                tp2 = tp2 + ((te2 - tp2) * ent2);

                if (NsharpLibBasics.qc(te1) && NsharpLibBasics.qc(te2)) {
                    tdef1 = (tp1 - te1) / (te1 + 273.15f);
                    tdef2 = (tp2 - te2) / (te2 + 273.15f);
                    lyre = 9.8f * (tdef1 + tdef2) / 2.0f * (h2 - h1);
                    tote += lyre;
                }

                pe1 = pe2;
                te1 = te2;
                h1 = h2;
                tp1 = tp2;
            }
        }
        layerParms.setTemperature(tp2);
        layerParms.setPressure(upper);
        return tote;
    }

    public static float mean_relhum(List<NcSoundingLayer> sndLys, float lower,
            float upper)
    /*************************************************************/
    /* MEAN_RELHUM */
    /*                                                           */
    /* Calculates the Mean Relative Humidity from data in */
    /* sndLys List within specified layer. */
    /* Input: */
    /* lower = Bottom level of layer (mb) [ -1=SFC] */
    /* upper = Top level of layer (mb) [ -1=TOP] */
    /* Return: mean relative Humidity (%) */
    /*************************************************************/
    {
        int lowIndex = 0, upperIndex = 0;
        float num, dp1, dp2, totd, dbar, p1, p2, pbar, totp;
        float t1, t2, tbar, tott;
        /* ----- See if default layer is specified ----- */
        if (lower == -1) {
            lower = NsharpLibBasics.sfcPressure(sndLys);
        }
        if (upper == -1) {
            upper = sndLys.get(sndLys.size() - 1).getPressure();
        }
        /* ----- Make sure this is a valid layer ----- */
        while (!NsharpLibBasics.qc(NsharpLibBasics.i_dwpt(sndLys, upper))
                && (upper < lower)) {
            upper += 50.0;
        }
        if (!NsharpLibBasics.qc(NsharpLibBasics.i_temp(sndLys, lower))) {
            lower = NsharpLibBasics.sfcPressure(sndLys);
        }

        if (upper >= lower) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }

        /* ----- Find lowest observation in layer ----- */
        boolean foundLow = false;
        for (int i = 0; i < sndLys.size(); i++) {
            if (NsharpLibBasics.qc(sndLys.get(i).getPressure())
                    && sndLys.get(i).getPressure() < lower) {
                lowIndex = i;
                foundLow = true;
                break;
            }
        }

        /* ----- Find highest observation in layer ----- */
        boolean foundHi = false;
        for (int i = sndLys.size() - 1; i >= 0; i--) {
            if (NsharpLibBasics.qc(sndLys.get(i).getPressure())
                    && sndLys.get(i).getPressure() > upper) {
                upperIndex = i;
                foundHi = true;
                break;
            }
        }

        /* ----- Start with interpolated bottom layer ----- */
        t1 = NsharpLibBasics.i_temp(sndLys, lower);
        dp1 = NsharpLibBasics.i_dwpt(sndLys, lower);
        p1 = lower;
        num = 1;

        totd = 0;
        totp = 0;
        tott = 0;
        if(foundHi && foundLow){
        	for (int i = lowIndex; i <= upperIndex; i++) {
        		if (NsharpLibBasics.qc(sndLys.get(i).getDewpoint())) {
        			/* ----- Calculate every level that reports a dwpt ----- */
        			dp2 = sndLys.get(i).getDewpoint();
        			t2 = sndLys.get(i).getTemperature();
        			p2 = sndLys.get(i).getPressure();
        			tbar = (t1 + t2) / 2.0f;
        			dbar = (dp1 + dp2) / 2.0f;
        			pbar = (p1 + p2) / 2.0f;
        			totd += dbar;
        			totp += pbar;
        			tott += tbar;
        			dp1 = dp2;
        			p1 = p2;
        			t1 = t2;
        			num++;
        		}
        	}
        }
        /* ----- Finish with interpolated top layer ----- */
        if (NsharpLibBasics.qc(NsharpLibBasics.i_dwpt(sndLys, upper))) {
            dp2 = NsharpLibBasics.i_dwpt(sndLys, upper);
            t2 = NsharpLibBasics.i_temp(sndLys, upper);
            p2 = upper;
            tbar = (t1 + t2) / 2.0f;
            dbar = (dp1 + dp2) / 2.0f;
            pbar = (p1 + p2) / 2.0f;
            tott += tbar;
            totd += dbar;
            totp += pbar;
        }
        return 100.0f * NsharpLibThermo.mixratio(totp / num, totd / num)
                / NsharpLibThermo.mixratio(totp / num, tott / num);

    }

    public static float mean_omeg(List<NcSoundingLayer> sndLys, float lower,
            float upper)
    /************************************************************
     * MEAN_OMEG
     * 
     * Calculates the Mean omega from data in sounding list within specified
     * layer. Value is returned.
     * 
     * @param lower
     *            - Bottom level of layer (mb) [ -1=700mb]
     * @param upper
     *            - Top level of layer (mb) [ -1=500mb]
     * @return mean omega (microb/s)
     *************************************************************/
    {
        float num, omgP1, omgP2, omgTot, omgBar, p1, p2, pBar, pTot;

        /* ----- See if default layer is specified ----- */
        if (lower == -1) {
            lower = 700;
        }
        if (upper == -1) {
            upper = 500;
        }

        /* ----- Make sure this is a valid layer ----- */
        if (!NsharpLibBasics.qc(NsharpLibBasics.i_temp(sndLys, upper))) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        if (!NsharpLibBasics.qc(NsharpLibBasics.i_temp(sndLys, lower))) {
            lower = NsharpLibBasics.sfcPressure(sndLys);
        }
        int lowIndex = 0, upperIndex = 0;
        /* ----- Find lowest observation in layer ----- */
        boolean foundLow = false;
        for (int i = 0; i < sndLys.size(); i++) {
            if (NsharpLibBasics.qc(sndLys.get(i).getPressure())
                    && NsharpLibBasics.qc(sndLys.get(i).getOmega())
                    && sndLys.get(i).getPressure() < lower) {
                lowIndex = i;
                foundLow = true;
                break;
            }
        }

        /* ----- Find highest observation in layer ----- */
        boolean foundHi = false;
        for (int i = sndLys.size() - 1; i >= 0; i--) {
            if (NsharpLibBasics.qc(sndLys.get(i).getPressure())
                    && NsharpLibBasics.qc(sndLys.get(i).getOmega())
                    && sndLys.get(i).getPressure() > upper) {
                upperIndex = i;
                foundHi = true;
                break;
            }
        }

        if (lowIndex == upperIndex) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }

        /* ----- Start with interpolated bottom layer ----- */
        omgP1 = NsharpLibBasics.i_omeg(sndLys, lower);
        p1 = lower;
        num = 1;

        omgTot = 0;
        pTot = 0;
        if(foundHi && foundLow){
        for (int i = lowIndex; i <= upperIndex; i++) {
            if (NsharpLibBasics.qc(sndLys.get(i).getOmega())) {
                /* ----- Calculate every level that reports omeg ----- */
                omgP2 = sndLys.get(i).getOmega();
                p2 = sndLys.get(i).getPressure();
                omgBar = (omgP1 + omgP2) / 2;
                pBar = (p1 + p2) / 2;
                omgTot = omgTot + omgBar;
                pTot = pTot + pBar;
                omgP1 = omgP2;
                p1 = p2;
                num += 1;
            }
        }
        }

        /* ----- Finish with interpolated top layer ----- */
        omgP2 = NsharpLibBasics.i_omeg(sndLys, upper);
        if (NsharpLibBasics.qc(omgP2)) {
            p2 = upper;
            omgBar = (omgP1 + omgP2) / 2;
            pBar = (p1 + p2) / 2;
            omgTot = omgTot + omgBar;
            pTot = pTot + pBar;
        }
        return (omgTot / num);
    }

    public static float cnvtv_temp(List<NcSoundingLayer> sndLys, float mincinh)
    /*************************************************************/
    /* CNVTV_TEMP */
    /*                                                           */
    /* Computes convective temperature, assuming no change in */
    /* moisture profile. Parcels are iteratively lifted until */
    /* only (mincinh) j/kg are left as a cap. The first guess */
    /* is the observed surface temperature. */
    /*
     * return convT in C
     * /************************************************************
     */
    {
        float mmr, sfcpres, sfctemp, sfcdwpt, excess;
        mmr = mean_mixratio(sndLys, -1, -1);
        sfcpres = NsharpLibBasics.sfcPressure(sndLys);
        sfctemp = NsharpLibBasics.sfcTemperature(sndLys);
        sfcdwpt = NsharpLibThermo.temp_at_mixrat(mmr, sfcpres);

        /*
         * Do a quick search to find whether to continue. If you need to heat up
         * more than 25C, don't compute.
         */
        Parcel parcel = parcel(sndLys, -1.0F, -1.0F, sfcpres, sfctemp + 25.0f,
                sfcdwpt);

        if ((parcel.getBplus() == 0.0f) || (parcel.getBminus() < mincinh)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }

        excess = sfcdwpt - sfctemp;
        if (excess > 0.0) {
            sfctemp = (sfctemp + excess + 4.0f);
        }

        parcel = parcel(sndLys, -1.0F, -1.0F, sfcpres, sfctemp, sfcdwpt);

        if (parcel.getBplus() == 0.0f) {
            parcel.setBminus(-999.0f);
        }
        while (parcel.getBminus() < mincinh) {
            if (parcel.getBminus() < -100.0) {
                sfctemp += 2.0;
            } else {
                sfctemp += 0.5;
            }
            parcel = parcel(sndLys, -1.0F, -1.0F, sfcpres, sfctemp, sfcdwpt);
            if (parcel.getBplus() == 0.0f) {
                parcel.setBminus(-999.0f);
            }
            // just a infinite loop breaker
            if (sfctemp > 150) {
                return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
            }
        }

        return sfctemp;
    }

    public static float coniglio1(List<NcSoundingLayer> sndLys)
    /*************************************************************/
    /* Return: Coniglio MCS Maintenance Parameter */
    /*************************************************************/
    {
        // 3-8km LR=lr38
        // 3-12km MW=mw312
        // 1-10km MBSmbs110);
        float lr38, mw312, mbs110;
        /* 3-8km AGL LR */
        lr38 = lapse_rate(
                sndLys,
                NsharpLibBasics.i_pres(sndLys,
                        NsharpLibBasics.msl(sndLys, 3000)),
                NsharpLibBasics.i_pres(sndLys,
                        NsharpLibBasics.msl(sndLys, 8000)));

        /* 3-12km AGL Mean Wind Speed */
        WindComponent windCompMW = NsharpLibWinds.mean_wind(
                sndLys,
                NsharpLibBasics.i_pres(sndLys,
                        NsharpLibBasics.msl(sndLys, 3000)),
                NsharpLibBasics.i_pres(sndLys,
                        NsharpLibBasics.msl(sndLys, 12000)));
        mw312 = NsharpLibBasics.kt_to_mps(windCompMW.getWspd());

        /* CAPE */
        float surfacePress = NsharpLibBasics.sfcPressure(sndLys);
        float unstablePress, unstableDwpt, unstableTemp;
        unstablePress = unstbl_lvl(sndLys, -1, surfacePress - 300);
        unstableTemp = NsharpLibBasics.i_temp(sndLys, unstablePress);
        unstableDwpt = NsharpLibBasics.i_dwpt(sndLys, unstablePress);
        Parcel pcl = parcel(sndLys, -1, -1, unstablePress, unstableTemp,
                unstableDwpt);
        float cape = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        if (pcl != null) {
            cape = pcl.getBplus();
        }

        /* Max Bulk Shear between 0-1km and 6-10km levels */
        float maxshr = 0;
        float l1, l2, p1, p2;
        for (l1 = 0; l1 <= 1000; l1 += 500) {
            p1 = NsharpLibBasics
                    .i_pres(sndLys, NsharpLibBasics.msl(sndLys, l1));
            for (l2 = 6000; l2 <= 10000; l2 += 500) {
                p2 = NsharpLibBasics.i_pres(sndLys,
                        NsharpLibBasics.msl(sndLys, l2));
                WindComponent windCompWSh = NsharpLibWinds.wind_shear(sndLys,
                        p1, p2);
                if (windCompWSh.getWspd() > maxshr) {
                    maxshr = windCompWSh.getWspd();
                }
            }
        }
        mbs110 = NsharpLibBasics.kt_to_mps(maxshr);

        // Computing Coniglio Maintenance Parameter:
        /* Calculate Probability based on regression equation */
        float a0, a1, a2, a3, a4;
        a0 = 13;
        a1 = -4.59E-2f;
        a2 = -1.16f;
        a3 = -6.17E-4f;
        a4 = -0.170f;
        if (cape != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA) {
            double mmp = 1 / (1 + Math.exp(a0 + (a1 * mbs110) + (a2 * lr38)
                    + (a3 * cape) + (a4 * mw312)));

            return (float) mmp;
        } else {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
    }

    public static float top_moistlyr(List<NcSoundingLayer> sndLys)
    /*************************************************************/
    /* TOP_MOISTLYR */
    /*                                                           */
    /* Determines the top of the moist layer of a given */
    /* environment. Criteria: 50% dcrs in mix ratio in mb. */
    /*                                                           */
    /* Return: top of moist layer (mb) */
    /*************************************************************/
    {
        float p1, p2, q1, q2, mq1, mq2, mh1, mh2, dqdz, dz;
        NcSoundingLayer sfcLyr = NsharpLibBasics.sfc(sndLys);
        q1 = NsharpLibThermo.mixratio(sfcLyr.getPressure(),
                sfcLyr.getDewpoint());
        p1 = sfcLyr.getPressure();
        mq1 = q1;
        mh1 = NsharpLibBasics.i_hght(sndLys, p1);
        for (int i = NsharpLibBasics.sfcIndex(sndLys); i < sndLys.size(); i++) {
            NcSoundingLayer lyr = sndLys.get(i);
            if (NsharpLibBasics.qc(lyr.getDewpoint())
                    && NsharpLibBasics.qc(lyr.getTemperature())) {
                /* ----- Calculate every level that reports a dwpt ----- */
                p2 = lyr.getPressure();
                q2 = NsharpLibThermo.mixratio(p2, lyr.getDewpoint());

                mq2 = (q1 + q2) / 2.0f;
                mh2 = (NsharpLibBasics.i_hght(sndLys, p2) + NsharpLibBasics
                        .i_hght(sndLys, p1)) / 2.0f;
                dz = mh2 - mh1;
                if (dz == 0.0)
                    dz = 0.001f;
                dqdz = (mq2 - mq1) / dz;
                if ((dqdz < -0.01)
                        && (i > NsharpLibBasics.sfcIndex(sndLys) + 1)
                        && (p2 >= 500.0)) {
                    return p1;
                }
                q1 = q2;
                p1 = p2;
                mq1 = mq2;
                mh1 = mh2;
            }
        }

        return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
    }

    public static float t_totals(List<NcSoundingLayer> sndLys)
    /*************************************************************/
    /* T_TOTALS */
    /*                                                           */
    /* Calculates the Total Totals index from sounding data */
    /* layers */
    /* RETURN: Total Totals index */
    /*************************************************************/
    {
        float t850, t500, td850;
        t850 = NsharpLibBasics.i_temp(sndLys, 850.0f);
        t500 = NsharpLibBasics.i_temp(sndLys, 500.0f);
        td850 = NsharpLibBasics.i_dwpt(sndLys, 850.0f);

        if (!NsharpLibBasics.qc(t850) || !NsharpLibBasics.qc(t500)
                || !NsharpLibBasics.qc(td850)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        } else {
            float vt = t850 - t500;
            float ct = td850 - t500;
            return (vt + ct);
        }
    }

    public static float sweat_index(List<NcSoundingLayer> sndLys)
    /*************************************************************/
    /* SWEAT_INDEX */
    /*                                                           */
    /* Calculates the SWEAT-Index from sounding data layers */
    /* Return: sweat index */
    /*************************************************************/
    {
        float td850, tt, wsp850, wsp500, sw, wdir850, wdir500, sinw, angl;
        td850 = NsharpLibBasics.i_dwpt(sndLys, 850.0f);

        tt = t_totals(sndLys);
        wsp850 = NsharpLibBasics.i_wspd(sndLys, 850.0f);
        wsp500 = NsharpLibBasics.i_wspd(sndLys, 500.0f);
        wdir850 = NsharpLibBasics.i_wdir(sndLys, 850.0f);
        wdir500 = NsharpLibBasics.i_wdir(sndLys, 500.0f);

        if (!NsharpLibBasics.qc(td850) || !NsharpLibBasics.qc(tt)
                || !NsharpLibBasics.qc(wsp850) || !NsharpLibBasics.qc(wsp500)
                || !NsharpLibBasics.qc(wdir850) || !NsharpLibBasics.qc(wdir500)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }

        sinw = 0;
        sw = 0;
        if (td850 > 0.0) {
            sw = 12.0f * td850;
        }
        if (tt > 49.0f) {
            sw = sw + (20.0f * (tt - 49.0f));
        }
        sw = sw + (2.0f * wsp850) + wsp500;

        if ((wdir850 >= 130.0) && (wdir850 <= 250.0)) {
            if ((wdir500 >= 210.0) && (wdir500 <= 310.0)) {
                if (wdir500 > wdir850) {
                    angl = (wdir500 - wdir850) * (NsharpLibSndglib.PI / 180.0f);
                    sinw = (float) Math.sin(angl);
                }
            }
        }
        if (sinw > 0.0) {
            sw = sw + (125.0f * (sinw + 0.2f));
        }

        return sw;
    }

    public static float ThetaE_diff(List<NcSoundingLayer> sndLys)
    /*************************************************************/
    /* THETAE_DIFF */
    /*                                                           */
    /* Finds the maximum and minimum theta-e values in the */
    /* sounding below 500mb and compute their difference. */
    /* return: the thetaE difference. */
    /*                                                           */
    /*************************************************************/
    {
        float maxe = -999.0f, mine = 999.0f, the;
        for (int i = NsharpLibBasics.sfcIndex(sndLys) + 1; i < sndLys.size(); i++) {
            NcSoundingLayer lyr = sndLys.get(i);
            if (NsharpLibBasics.qc(lyr.getDewpoint())
                    && NsharpLibBasics.qc(lyr.getTemperature())) {
                the = NsharpLibThermo.thetae(lyr.getPressure(),
                        lyr.getTemperature(), lyr.getDewpoint());
                if (the > maxe) {
                    maxe = the;
                }
                if (the < mine) {
                    mine = the;
                }
                if (lyr.getPressure() < 500.0) {
                    break;
                }
            }
        }

        return (maxe - mine);
    }

    public static LayerParameters low_inv(List<NcSoundingLayer> sndLys)
    /************************************************************************/
    /* OPC MODIFICATION - LOW_INV */
    /* Calculates base height of lowest temperature inversion. */
    /* inv_mb - Pressure of inversion level (mb) */
    /* inv_dC - Change in temperature (C) */
    /* Return: inv_mb and inv_dc in LayerParameters data structure as */
    /* pressure and temperature parameters. */
    /* Note: This function is not in BigSharp, therefore port it from legacy */
    /* nsharp. */
    /************************************************************************/
    {
        float inv_mb, inv_dC;
        LayerParameters rtnParms = new LayerParameters();
        for (int i = 0; i < sndLys.size() - 1; i++) {
            NcSoundingLayer lyr0 = sndLys.get(i);
            NcSoundingLayer lyr1 = sndLys.get(i + 1);
            if (NsharpLibBasics.qc(lyr0.getTemperature())
                    && NsharpLibBasics.qc(lyr1.getTemperature())) {
                if (lyr1.getTemperature() > lyr0.getTemperature()) {
                    inv_mb = lyr0.getPressure();
                    inv_dC = lyr1.getTemperature() - lyr0.getTemperature();
                    rtnParms.setPressure(inv_mb);
                    rtnParms.setTemperature(inv_dC);
                    return rtnParms;
                }
            }
        }
        return rtnParms;
    }

    public static MixHeight mix_height(List<NcSoundingLayer> sndLys, int flag)
    /****************************************************************************/
    /* mix_height */
    /* This function modifies original mix_height function which computes */
    /* hydro-meteorological parameters within empirical mix boundary layer. */
    /* Input: */
    /* flag - 0: Surface-based lapse rate; 1: Layer-based lapse rate */
    /* Return: MixHeight data structure with the following parameters, */
    /* mh_pres - Pressure at mixing height (mb) */
    /* mh_drct - Wind direction at mixing height (deg) */
    /* mh_sped - Wind speed at mixing height (kts) */
    /* mh_dC - Layer change in temperature (C) */
    /* mh_lr - Layer lapse rate (C/km) */
    /* mh_drct_max - Layer maximum wind direction (deg) */
    /* mh_sped_max - Layer maximum wind speed (kts) */
    /* Chin:: this function is not in BigNsharp, therefore ported from */
    /* legacy nsharp. However, original code have a bug that it always returns */
    /* same result for both surface based and layer based mixing height. This */
    /* method fix that problem */
    /****************************************************************************/
    {
        float thresh, lapser, lapser_1, mdrct, msped, drct, sped;
        float dt, dz;
        float mh_pres, mh_drct, mh_sped, mh_dC, mh_lr, mh_drct_max, mh_sped_max;

        thresh = 8.3f;
        drct = 0;
        sped = 0;
        lapser = lapser_1 = 0;
        NcSoundingLayer lyrPlus;
        NcSoundingLayer lyr;
        NcSoundingLayer savedCurrentLyr=null;
        int sfcIndex = NsharpLibBasics.sfcIndex(sndLys);
        NcSoundingLayer sfclyr = sndLys.get(sfcIndex);
        // set default mdrct and msped to surface layer wind direction and speed value
        mdrct = sfclyr.getWindDirection();
        msped = sfclyr.getWindSpeed();
        for (int ii = sfcIndex; ii < sndLys.size() - 1; ii++) {
        	lyr = sndLys.get(ii);
        	if(!NsharpLibBasics.qc(lyr.getTemperature())){
        		continue;
        	}
        	int plusIndex = ii+1;
        	lyrPlus = sndLys.get(plusIndex);
        	while(!NsharpLibBasics.qc(lyrPlus.getTemperature())){
        		// skip layers with no valid temperature
        		plusIndex++;
        		if(plusIndex > sndLys.size() - 1){
        			// reach end of layers list, can not continue
        			return new MixHeight();
        		}
        		lyrPlus = sndLys.get(plusIndex);
        	}

        	if(flag == 0){
        		savedCurrentLyr = lyr;
        		drct = savedCurrentLyr.getWindDirection();
            	sped = savedCurrentLyr.getWindSpeed();
        		// surface based, always use surface layer as lower layer 
        		lyr = sfclyr;
        	}else {
        	    drct = lyr.getWindDirection();
        	    sped = lyr.getWindSpeed();
        	}
        	/* ----- Calculate Lapse Rate ----- */
        	dt = lyrPlus.getTemperature() - lyr.getTemperature();
        	dz = lyrPlus.getGeoHeight() - lyr.getGeoHeight();
        	lapser = (dt / dz) * -1000;


        	/* ----- Store Maximum Wind Data ----- */
        	
        	if (sped > msped) {
        		mdrct = drct;
        		msped = sped;
        	}

        	if (lapser < thresh)  {
        		// yes, it is a stable layer
        		if(ii == sfcIndex){// || (flag ==0 && ii == sfcIndex+1)){
        			// first level is already stable, handle it differently from other level
        			mh_pres = lyr.getPressure();
        			mh_drct = lyr.getWindDirection();
        			mh_sped = lyr.getWindSpeed();
        			mh_dC = -dt;
        			mh_lr = lapser;
        			mh_drct_max =  lyr.getWindDirection();;
        			mh_sped_max =  lyr.getWindSpeed();
        			MixHeight mixHeight = new MixHeight(mh_pres, mh_drct,
        					mh_sped, mh_dC, mh_lr, mh_drct_max, mh_sped_max);
        			return mixHeight;
        		}
        		/* ----- Above Mixing Height ----- */
        		if(flag != 0){
        			/* ----- Calculate Previous Rate ----- */
        			int minusIndex = ii-1;
        			NcSoundingLayer lyrMinus=null;  
        			// find valid layer minus
        			while(minusIndex >= 0){
        				// skip layers with no valid temperature
        				lyrMinus  = sndLys.get(minusIndex);
        				if(NsharpLibBasics.qc(lyrMinus.getTemperature())){
        					break;
        				}     
        				else {
        					minusIndex--;
        					if(minusIndex<0){
        						return new MixHeight();
        					}
        				}
        			}
        			dt = lyr.getTemperature() - lyrMinus.getTemperature();
        			dz = lyr.getGeoHeight() - lyrMinus.getGeoHeight();
        			lapser_1 = (dt / dz) * -1000;
        			mh_pres = lyr.getPressure();
            		mh_drct = lyr.getWindDirection();
            		mh_sped = lyr.getWindSpeed();
        		}
        		else {
        			dt = savedCurrentLyr.getTemperature() - sfclyr.getTemperature();
        			dz = savedCurrentLyr.getGeoHeight() - sfclyr.getGeoHeight();
        			lapser_1 = (dt / dz) * -1000;
        			mh_pres = savedCurrentLyr.getPressure();
            		mh_drct = savedCurrentLyr.getWindDirection();
            		mh_sped = savedCurrentLyr.getWindSpeed();
        		}

        		mh_dC = -dt;
        		mh_lr = lapser_1;
        		mh_drct_max = mdrct;
        		mh_sped_max = msped;
        		MixHeight mixHeight1 = new MixHeight(mh_pres, mh_drct,
        				mh_sped, mh_dC, mh_lr, mh_drct_max, mh_sped_max);
        		return mixHeight1;
        	}
        }

        MixHeight mixHeight = new MixHeight();
        return mixHeight;
    }

    public static float Rogash_QPF(List<NcSoundingLayer> sndLys)
    /*************************************************************
     * Rogash_Rate Computes an approximate cellular convective precipitation
     * rate (expressed in inches per hour). Based on research by Joe Rogash.
     * 
     * @return - 1 hour rain rate (in/hr)
     *************************************************************/
    {
        float pres, temp, dwpt;
        float rog_updraft, rog_water;

        /* Lift a MOST UNSTABLE Parcel */
        pres = unstbl_lvl(sndLys, -1, -1);
        if(pres == NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA){
        	return 0;
        }
        temp = NsharpLibBasics.i_temp(sndLys, pres);
        dwpt = NsharpLibBasics.i_dwpt(sndLys, pres);
        Parcel pcl = parcel(sndLys, -1, -1, pres, temp, dwpt);

        if (pcl == null || pcl.getBplus() <= 0) {
            return 0;
        }
        /* Begin by determining theoretical updraft strength */
        rog_updraft = (float) Math.sqrt(2 * pcl.getBplus());
        rog_updraft = rog_updraft * 0.25f;
        /* Now calculate PW density. precip_water() return in mm */
        rog_water = NcSoundingTools.precip_water(sndLys, pcl.getLfcpres(), -1);
        rog_water = rog_water
                / ((NsharpLibBasics.i_hght(sndLys, pcl.getElpres()) - NsharpLibBasics
                        .i_hght(sndLys, pcl.getLfcpres())));
        float rainFallRate = rog_updraft * rog_water * 3600.0f;
        // convert result to inch
        return rainFallRate / 25.4f;
    }

    public static float Mean_WBtemp(List<NcSoundingLayer> sndLys, float lower,
            float upper)
    /************************************************************
     * MEAN_WBTEMP
     * 
     * Computes the average Wetbulb temperature between the two given levels
     * (lower,upper)
     * 
     * @param - lower = Bottom level of layer (mb) [ -1=SFC]
     * @param - upper = Top level of layer (mb) [ -1=WBZ]
     * @return - mean wetbulb temperature
     *************************************************************/
    {

        /* ----- See if default layer is specified ----- */
        if (lower == -1) {
            lower = NsharpLibBasics.sfcPressure(sndLys);
        }
        if (upper == -1) {
            upper = wb_lvl(sndLys, 0);
        }

        /* ----- Make sure this is a valid layer ----- */
        if (!NsharpLibBasics.qc(NsharpLibBasics.i_dwpt(sndLys, upper))) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        if (!NsharpLibBasics.qc(NsharpLibBasics.i_dwpt(sndLys, lower))) {
            lower = NsharpLibBasics.sfcPressure(sndLys);
        }
        int lowIndex = 0, upperIndex = 0;
        /* ----- Find lowest observation in layer ----- */
        boolean foundLow = false;
        for (int i = 0; i < sndLys.size(); i++) {
            if (NsharpLibBasics.qc(sndLys.get(i).getPressure())
                    && sndLys.get(i).getPressure() < lower) {
                lowIndex = i;
                foundLow= true;
                break;
            }
        }

        /* ----- Find highest observation in layer ----- */
        boolean foundHi = false;
        for (int i = sndLys.size() - 1; i >= 0; i--) {
            if (NsharpLibBasics.qc(sndLys.get(i).getPressure())
                    && sndLys.get(i).getPressure() > upper) {
                upperIndex = i;
                foundHi = true;
                break;
            }
        }

        float num, dp1, dp2, totd, dbar, p1, p2, pbar, totp;
        /* ----- Start with interpolated bottom layer ----- */
        dp1 = NsharpLibThermo.wetbulb(lower,
                NsharpLibBasics.i_temp(sndLys, lower),
                NsharpLibBasics.i_dwpt(sndLys, lower));
        p1 = lower;
        num = 1;

        totd = 0;
        totp = 0;
        if(foundHi && foundLow){
        	for (int i = lowIndex; i <= upperIndex; i++) {
        		if (NsharpLibBasics.qc(sndLys.get(i).getDewpoint())
        				&& NsharpLibBasics.qc(sndLys.get(i).getTemperature())) {
        			/*
        			 * ----- Calculate every level that reports a temp & dew point
        			 * -----
        			 */
        			dp2 = NsharpLibThermo.wetbulb(sndLys.get(i).getPressure(),
        					sndLys.get(i).getTemperature(), sndLys.get(i)
        					.getDewpoint());
        			p2 = sndLys.get(i).getPressure();
        			dbar = (dp1 + dp2) / 2.0f;
        			pbar = (p1 + p2) / 2.0f;
        			totd = totd + dbar;
        			totp = totp + pbar;
        			dp1 = dp2;
        			p1 = p2;
        			num++;
        		}
        	}
        }
        /* ----- Finish with interpolated top layer ----- */
        dp2 = NsharpLibThermo.wetbulb(upper,
                NsharpLibBasics.i_temp(sndLys, upper),
                NsharpLibBasics.i_dwpt(sndLys, upper));
        p2 = upper;
        dbar = (dp1 + dp2) / 2.0f;
        pbar = (p1 + p2) / 2.0f;
        totd = totd + dbar;
        totp = totp + pbar;
        return totd / num;
    }

    public static float mean_temp(List<NcSoundingLayer> sndLys, float lower,
            float upper)
    /************************************************************
     * MEAN_TEMP
     * 
     * Calculates the Mean Temperature from data in sounding list within
     * specified layer.
     * 
     * @param sndLys
     *            = sounding layer list
     * @param lower
     *            = Bottom level of layer (mb) [ -1=SFC]
     * @param upper
     *            = Top level of layer (mb) [ -1=SFC-100mb]
     * @return mean temp (c)
     *************************************************************/
    {
        /* ----- See if default layer is specified ----- */
        if (lower == -1) {
            lower = NsharpLibBasics.sfcPressure(sndLys);
        }
        if (upper == -1) {
            upper = NsharpLibBasics.sfcPressure(sndLys) - 100.0f;
        }
        /* ----- Make sure this is a valid layer ----- */
        if (!NsharpLibBasics.qc(NsharpLibBasics.i_temp(sndLys, upper))) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        if (!NsharpLibBasics.qc(NsharpLibBasics.i_temp(sndLys, lower))) {
            lower = NsharpLibBasics.sfcPressure(sndLys);
        }
        /* ----- Find lowest observation in layer ----- */
        boolean foundLow = false;
        int lowerIndex = 0;
        for (int i = 0; i < sndLys.size(); i++) {
            if (NsharpLibBasics.qc(sndLys.get(i).getPressure())
                    && NsharpLibBasics.qc(sndLys.get(i).getTemperature())
                    && sndLys.get(i).getPressure() < lower) {
                lowerIndex = i;
                foundLow= true;
                break;
            }
        }
        /* ----- Find highest observation in layer ----- */
        boolean foundHi = false;
        int upperIndex = sndLys.size() - 1;
        for (int i = sndLys.size() - 1; i >= 0; i--) {
            if (NsharpLibBasics.qc(sndLys.get(i).getPressure())
                    && sndLys.get(i).getPressure() > upper) {
                upperIndex = i;
                foundHi= true;
                break;
            }
        }
        /* ----- Start with interpolated bottom layer ----- */
        float numLayer, temp1, temp2, totTemp, tempBar, press1, press2, pressBar, totPress;
        temp1 = NsharpLibBasics.i_temp(sndLys, lower);
        press1 = lower;
        numLayer = 1;
        totTemp = 0;
        totPress = 0;
        if(foundHi && foundLow){
        	for (int i = lowerIndex; i <= upperIndex; i++) {
        		if (NsharpLibBasics.qc(sndLys.get(i).getTemperature())) {
        			/* ----- Calculate every level that reports a temp ----- */
        			temp2 = sndLys.get(i).getTemperature();
        			press2 = sndLys.get(i).getPressure();
        			tempBar = (temp1 + temp2) / 2.0f;
        			pressBar = (press1 + press2) / 2.0f;
        			totTemp = totTemp + tempBar;
        			totPress = totPress + pressBar;
        			temp1 = temp2;
        			press1 = press2;
        			numLayer++;
        		}
        	}
        }
        /* ----- Finish with interpolated top layer ----- */
        temp2 = NsharpLibBasics.i_temp(sndLys, upper);
        press2 = upper;
        tempBar = (temp1 + temp2) / 2.0f;
        pressBar = (press1 + press2) / 2.0f;
        totTemp = totTemp + tempBar;
        totPress = totPress + pressBar;
        return totTemp / numLayer;
    }

    public static float advection_layer(List<NcSoundingLayer> sndLys,
            float lower, float upper)
    /*************************************************************
     * ADVECTION_LAYER
     * 
     * @param sndLys
     *            = sounding layer list
     * @param lower
     *            = lower end of layer (mb)
     * @param upper
     *            = upper end of layer (mb)
     * @return in (C/hr)
     *************************************************************/
    {
        /* Mean wind through the layer */
        WindComponent meanWindComp = NsharpLibWinds.mean_wind(sndLys, lower,
                upper);
        if (!NsharpLibBasics.qc(meanWindComp.getWdir())) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }

        /* Mean temperature through the layer */
        float mean_t = mean_temp(sndLys, lower, upper) + 273.15f;
        if (!NsharpLibBasics.qc(mean_t)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }

        /* Shear through the layer */
        WindComponent shearWindComp = NsharpLibWinds.wind_shear(sndLys, lower,
                upper);
        if (!NsharpLibBasics.qc(shearWindComp.getWdir())) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }

        /* Depth of the layer */
        float dpth = NsharpLibBasics.i_hght(sndLys, upper)
                - NsharpLibBasics.i_hght(sndLys, lower);
        if (!NsharpLibBasics.qc(dpth)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        float term1 = NsharpLibBasics.kt_to_mps(meanWindComp.getVcomp())
                * (NsharpLibBasics.kt_to_mps(shearWindComp.getUcomp()) / dpth);
        float term2 = NsharpLibBasics.kt_to_mps(meanWindComp.getUcomp())
                * (NsharpLibBasics.kt_to_mps(shearWindComp.getVcomp()) / dpth);
        float con1 = (.0004f / 9.81f); /* -f/g */
        float term3 = con1 * mean_t * (term1 - term2) * 3600;
        return term3;
    }

    public static float pbl_top(List<NcSoundingLayer> sndLys)
    /*************************************************************/
    /* PBL_TOP */
    /*************************************************************/
    {
        float tv1, tvsfc;
        /* Determine Thetav of surface parcel */
        NcSoundingLayer sfcLyr = NsharpLibBasics.sfc(sndLys);
        float sfcVtemp = NsharpLibThermo.virtemp(sfcLyr.getPressure(),
                sfcLyr.getTemperature(), sfcLyr.getDewpoint());
        tvsfc = NsharpLibThermo.theta(sfcLyr.getPressure(), sfcVtemp, 1000);

        for (int i = NsharpLibBasics.sfcIndex(sndLys); i < sndLys.size(); i++) {
            NcSoundingLayer sndLyr = sndLys.get(i);
            float vtemp = NsharpLibThermo.virtemp(sndLyr.getPressure(),
                    sndLyr.getTemperature(), sndLyr.getDewpoint());
            tv1 = NsharpLibThermo.theta(sndLyr.getPressure(), vtemp, 1000);
            if (tv1 > tvsfc + .5) {
                return (sndLys.get(i - 1).getPressure());
            }
        }
        return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
    }

    public static float fosberg(List<NcSoundingLayer> sndLys)
    /*************************************************************/
    /* Fosberg Fire Weather Index */
    /* returns Fosberg Index */
    /*************************************************************/
    {
        float rh, em, fmph, em30, u_sq, fmdc, tmpf;

        NcSoundingLayer sfcLyr = NsharpLibBasics.sfc(sndLys);
        tmpf = NsharpLibBasics.ctof(sfcLyr.getTemperature());
        fmph = 1.1516f * sfcLyr.getWindSpeed();
        rh = NsharpLibThermo.relh(sndLys, -1);
        if (!NsharpLibBasics.qc(tmpf) || !NsharpLibBasics.qc(fmph)
                || !NsharpLibBasics.qc(rh)) {
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        }
        if (rh <= 10) {
            em = 0.03229f + 0.281073f * rh - 0.000578f * rh * tmpf;
        } else if (rh <= 50) {
            em = 2.22749f + 0.160107f * rh - 0.014784f * tmpf;
        } else {
            em = 21.0606f + 0.005565f * rh * rh - 0.00035f * rh * tmpf
                    - 0.483199f * rh;
        }
        em30 = em / 30;
        u_sq = fmph * fmph;
        fmdc = 1 - 2 * em30 + 1.5f * em30 * em30 - 0.5f * em30 * em30 * em30;

        return (fmdc * (float) Math.sqrt(1 + u_sq)) / 0.3002f;
    }
}
