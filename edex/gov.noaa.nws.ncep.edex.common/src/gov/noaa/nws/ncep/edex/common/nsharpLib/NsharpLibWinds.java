package gov.noaa.nws.ncep.edex.common.nsharpLib;

/**
 * 
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 * All methods developed in this class are based on the
 * algorithm developed in BigSharp native C file, winds.c , by John A.
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
 * 
 * @version 1.0
 * 
 */
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.Helicity;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.WindComponent;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;

import java.util.List;

import org.locationtech.jts.geom.Coordinate;

public class NsharpLibWinds {

    public static float ucomp(float wdir, float wspd)
    /*************************************************************/
    /* UCOMP */
    /*                                                           */
    /* Calculates a u-component of the wind (kt), given */
    /* a direction and speed. */
    /*                                                           */
    /* wdir - Wind Direction (deg) */
    /* wspd - Wind Speed (kt) */
    /*************************************************************/
    {
        if (!NsharpLibBasics.qc(wdir) || !NsharpLibBasics.qc(wspd))
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

        while (wdir > 360.0)
            wdir -= 360;
        wdir *= (NsharpLibSndglib.PI / 180.0);
        return (-wspd * (float) Math.sin(wdir));
    }

    public static float vcomp(float wdir, float wspd)
    /*************************************************************/
    /* VCOMP */
    /*                                                           */
    /* Calculates a v-component of the wind (kt), given */
    /* a direction and speed. */
    /*                                                           */
    /* wdir - Wind Direction (deg) */
    /* wspd - Wind Speed (kt) */
    /*************************************************************/
    {
        if (!NsharpLibBasics.qc(wdir) || !NsharpLibBasics.qc(wspd))
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

        while (wdir >= 360.0)
            wdir -= 360;
        wdir *= (NsharpLibSndglib.PI / 180.0);
        return (-wspd * (float) Math.cos(wdir));
    }

    /*************************************************************/
    /* uvCOMP */
    /*                                                           */
    /* Calculates UV component of the wind (kt), given */
    /* a direction and speed. */
    /*                                                           */
    /* wdir - Wind Direction (deg) */
    /* wspd - Wind Speed (kt) */
    /*************************************************************/

    public static Coordinate uvComp(float wspd, float wdir) {
        double angle = Math.toRadians(wdir);
        double u = -wspd * Math.sin(angle);
        double v = -wspd * Math.cos(angle);

        return new Coordinate(u, v);
    }

    /*************************************************************
     * speedDir
     * 
     * Calculates wind direction and speed given wind U/V component
     * 
     * @param u
     *            wind u component
     * @param v
     *            wind v component
     * @return wdir - Wind Direction (deg)
     * @return wspd - Wind Speed (kt)
     * 
     */
    public static Coordinate speedDir(float u, float v) {
        double spd = Math.hypot(u, v);
        double dir = Math.toDegrees(Math.atan2(-u, -v));
        if (dir < 0) {
            dir += 360;
        }
        return new Coordinate(spd, dir);
    }

    public static float angle(float u, float v)
    /*************************************************************/
    /* ANGLE */
    /*                                                           */
    /* Calculates an angle (deg) of the wind (u,v). */
    /*                                                           */
    /* u - U-Component (kt) */
    /* v - V-Component (kt) */
    /*************************************************************/
    {
        double sc, t1;

        if (!NsharpLibBasics.qc(u) || !NsharpLibBasics.qc(v))
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

        sc = NsharpLibSndglib.PI / 180;
        if ((u == 0) && (v == 0))
            return 0;

        if ((u == 0) && (v < 0))
            return 360;
        if ((u == 0) && (v > 0))
            return 180;
        t1 = Math.atan(-v / -u) / sc;
        if (u <= 0) {
            return (float) (90 - t1);
        } else {
            return (float) (270 - t1);
        }
    }

    public static float speed(float u, float v)
    /*************************************************************/
    /* SPEED */
    /* Calculates the speed of the wind (u,v). */
    /*                                                           */
    /* u - U-Component */
    /* v - V-Component */
    /*************************************************************/
    {
        if (!NsharpLibBasics.qc(u) || !NsharpLibBasics.qc(v))
            return NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        return (float) Math.sqrt((u * u) + (v * v));
    }

    public static WindComponent mean_wind(List<NcSoundingLayer> sndLys,
            float pbot, float ptop)
    /*************************************************************/
    /* MEAN_WIND */
    /* Calculates a pressure-weighted mean wind thru the */
    /* layer (pbot-ptop). No Default layer */
    /* Input: */
    /* pbot - Bottom level of layer (mb) */
    /* ptop - Top level of layer (mb) */
    /* Return: WindComponent structure with definitions below */
    /* mnu - U-Component of mean wind (kt) */
    /* mnv - V-Component of mean wind (kt) */
    /* wdir - direction of mean wind */
    /* wspd - speed of mean wind (kt) */
    /*************************************************************/
    {
        float mnu, mnv, wdir = 0, wspd = 0;
        float pinc, usum, vsum, wgt, w1, i, p;
        WindComponent windComp = new WindComponent();
        if (!NsharpLibBasics.qc(pbot) || !NsharpLibBasics.qc(ptop)) {
            return windComp;
        }
        pinc = (pbot - ptop) / 20.0f;

        if (pinc < 1.0) {
            float winduBot, winduTop, windvBot, windvTop;
            winduBot = NsharpLibBasics.i_wndu(sndLys, pbot);
            winduTop = NsharpLibBasics.i_wndu(sndLys, ptop);
            windvBot = NsharpLibBasics.i_wndv(sndLys, pbot);
            windvTop = NsharpLibBasics.i_wndv(sndLys, ptop);
            if (!NsharpLibBasics.qc(winduBot) || !NsharpLibBasics.qc(winduTop)
                    || !NsharpLibBasics.qc(windvBot)
                    || !NsharpLibBasics.qc(windvTop)) {
                return windComp;
            }
            usum = (winduBot * pbot) + (winduTop * ptop);
            vsum = (windvBot * pbot) + (windvTop * ptop);
            wgt = pbot + ptop;
        } else {
            wgt = usum = vsum = 0;
            p = pbot;
            for (i = 0; i <= 20; i++) {
                w1 = p;
                float windu, windv;
                windu = NsharpLibBasics.i_wndu(sndLys, p);
                windv = NsharpLibBasics.i_wndv(sndLys, p);
                if (!NsharpLibBasics.qc(windu) || !NsharpLibBasics.qc(windv)) {
                    p -= pinc;
                    continue;
                }
                usum = usum + (windu * w1);
                vsum = vsum + (windv * w1);
                p -= pinc;
                wgt = wgt + w1;
            }
        }
        if(wgt==0){
        	return windComp;
        }

        mnu = (usum / wgt);
        mnv = (vsum / wgt);

        if (NsharpLibBasics.qc(mnu) && NsharpLibBasics.qc(mnv)) {
            wdir = angle(mnu, mnv);
            wspd = speed(mnu, mnv);
        }
        windComp.setUcomp(mnu);
        windComp.setVcomp(mnv);
        windComp.setWdir(wdir);
        windComp.setWspd(wspd);
        return windComp;
    }

    public static WindComponent mean_wind_npw(List<NcSoundingLayer> sndLys,
            float pbot, float ptop, int currentParcelNumber)
    /*************************************************************/
    /* MEAN_WIND_NPW */
    /* Calculates a mean wind thru the layer (pbot-ptop). */
    /* Does not use a pressure weighting function!!! */
    /* Input: */
    /* pbot - Bottom level of layer (mb) */
    /* ptop - Top level of layer (mb) */
    /* currentParcelNumber - current parcel number, only used when */
    /* pbot and/or ptop set as default value -1. */
    /* Return: WindComponent structure with definition below */
    /* ucomp - U-Component of mean wind (kt) */
    /* vcomp - V-Component of mean wind (kt) */
    /* wdir - direction of mean wind */
    /* wspd - speed of mean wind (kt) */
    /*************************************************************/
    {
        float pinc, usum, vsum, wgt;
        WindComponent windComp = new WindComponent();
        if (!NsharpLibBasics.qc(pbot) || !NsharpLibBasics.qc(ptop)) {
            return windComp;
        }

        pinc = (pbot - ptop) / 20.0f;
        usum = vsum = 0;
        
        if (pinc < 1.0) {
        	float winduBot, winduTop, windvBot, windvTop;
            winduBot = NsharpLibBasics.i_wndu(sndLys, pbot);
            winduTop = NsharpLibBasics.i_wndu(sndLys, ptop);
            windvBot = NsharpLibBasics.i_wndv(sndLys, pbot);
            windvTop = NsharpLibBasics.i_wndv(sndLys, ptop);
            if (!NsharpLibBasics.qc(winduBot) || !NsharpLibBasics.qc(winduTop)
                    || !NsharpLibBasics.qc(windvBot)
                    || !NsharpLibBasics.qc(windvTop)) {
                return windComp;
            }
        	
            usum = winduBot+winduTop;
            vsum = windvBot+windvTop;
            wgt = pbot + ptop;
        } else {
        	wgt = 0;
            for (float i = pbot; i >= ptop; i -= pinc) {
            	if(!NsharpLibBasics.qc( NsharpLibBasics.i_wndu(sndLys, i)) || 
            			!NsharpLibBasics.qc( NsharpLibBasics.i_wndv(sndLys, i))){
            	    continue;
            	}
            	usum = usum + NsharpLibBasics.i_wndu(sndLys, i);
            	vsum = vsum + NsharpLibBasics.i_wndv(sndLys, i);
                wgt++;
            }
        }
        if(wgt==0){
        	return windComp;
        }

        float mnu = (usum / wgt);
        float mnv = (vsum / wgt);
        windComp.setUcomp(mnu);
        windComp.setVcomp(mnv);
        if (NsharpLibBasics.qc(mnu) && NsharpLibBasics.qc(mnv)) {
            float wdir = angle(mnu, mnv);
            float wspd = speed(mnu, mnv);
            windComp.setWdir(wdir);
            windComp.setWspd(wspd);
        }
        return windComp;
    }

    public static WindComponent wind_shear(List<NcSoundingLayer> sndLys,
            float pbot, float ptop)
    /*************************************************************/
    /* WIND_SHEAR */
    /*                                                           */
    /* Calculates the shear between the wind at (pbot) and */
    /* (ptop). Default lower wind (-1)is a 1km mean wind, while */
    /* the default upper layer wind (-1) is 3km. */
    /*                                                           */
    /* pbot - Bottom level of layer (mb) */
    /* ptop - Top level of layer (mb) */
    /* Return: WindComponent structure with definitions below */
    /* ucomp - U-Component of shear (knot) */
    /* vcomp - V-Component of shear (knot) */
    /* wdir - Direction of shear vector (degrees) */
    /* wspd - - Magnitude of shear vector (knot) */
    /*************************************************************/
    {
        float ubot, vbot;
        float shu, shv, shdir, shmag;
        WindComponent windCompSh = new WindComponent();
        if (!NsharpLibBasics.qc(ptop) || !NsharpLibBasics.qc(pbot)) {
            return windCompSh;
        }
        /* ----- Check for Default Values ----- */
        if (pbot == -1) {
            pbot = NsharpLibBasics.sfcPressure(sndLys);
            WindComponent windCompMW = mean_wind(
                    sndLys,
                    pbot,
                    NsharpLibBasics.i_pres(sndLys,
                            NsharpLibBasics.msl(sndLys, 1000.0f)));
            ubot = windCompMW.getUcomp();
            vbot = windCompMW.getVcomp();
        } else {
            ubot = NsharpLibBasics.i_wndu(sndLys, pbot);
            vbot = NsharpLibBasics.i_wndv(sndLys, pbot);
        }

        if (ptop == -1) {
            ptop = NsharpLibBasics.i_pres(sndLys,
                    NsharpLibBasics.agl(sndLys, 3000.0f));
        }

        /* ----- Make sure winds were observed through layer ----- */
        if (NsharpLibBasics.qc(NsharpLibBasics.i_wndu(sndLys, ptop))
                && NsharpLibBasics.qc(NsharpLibBasics.i_wndu(sndLys, pbot))) {
            /* ----- Calculate Vector Difference ----- */
            shu = NsharpLibBasics.i_wndu(sndLys, ptop) - ubot;
            shv = NsharpLibBasics.i_wndv(sndLys, ptop) - vbot;
            windCompSh.setUcomp(shu);
            windCompSh.setVcomp(shv);
            if (NsharpLibBasics.qc(shu) && NsharpLibBasics.qc(shv)) {
                shdir = angle(shu, shv);
                shmag = speed(shu, shv);
                windCompSh.setWdir(shdir);
                windCompSh.setWspd(shmag);
            }
        }
        return windCompSh;
    }

    public static Helicity helicity(List<NcSoundingLayer> sndLys, float lower,
            float upper, float sdir, float sspd)
    /*************************************************************
     * HELICITY
     * 
     * Calculates the storm-relative helicity (m2/s2) of a layer from LOWER(m,
     * agl) to UPPER(m, agl). Uses the storm motion vector (sdir, sspd).
     * 
     * @param sndLys
     *            - list of NcSounding layer
     * @param lower
     *            - Bottom level of layer (m, AGL)[-1=LPL]
     * @param upper
     *            - Top level of layer(m, AGL) [-1=LFC]
     * @param sdir
     *            - Storm motion direction (degrees)
     * @param sspd
     *            - Storm motion speed (kt)
     * 
     * @return - Helicity with Total helicity (m2/s2), positive helicity and
     *         negative helicity
     *************************************************************/
    {
        Helicity heli = new Helicity();
        if (!NsharpLibBasics.qc(sspd) || !NsharpLibBasics.qc(sdir)
                || !NsharpLibBasics.qc(lower) || !NsharpLibBasics.qc(upper)) {
            return heli;
        }

        /* ----- Check for Default Values ----- */
        float esfcHeight = 0;
        if (upper == -1 || lower == -1) {
            esfcHeight = NsharpLibBasics.i_hght(sndLys,
                    NsharpLibThermo.esfc(sndLys, 50.0f));
            if (!NsharpLibBasics.qc(esfcHeight)) {
                // helicity: effective surface not found in sounding.
                return heli;
            }
        }

        if (upper == -1) {
            upper = NsharpLibBasics.agl(sndLys, esfcHeight + 3000.0f);
        }
        if (lower == -1) {
            lower = NsharpLibBasics.agl(sndLys, esfcHeight);
        }
        if (!NsharpLibBasics.qc(upper)) {
            upper = 3000.0f;
        }

        /* ----- See if this is a valid layer ----- */
        if (!NsharpLibBasics.qc(NsharpLibBasics.i_wndu(
                sndLys,
                NsharpLibBasics.i_pres(sndLys,
                        NsharpLibBasics.msl(sndLys, lower))))
                || !NsharpLibBasics.qc(NsharpLibBasics.i_wndu(
                        sndLys,
                        NsharpLibBasics.i_pres(sndLys,
                                NsharpLibBasics.msl(sndLys, upper))))) {
            return heli;
        }

        /* ----- Make sure winds were observed through layer ----- */
        float stormMotionX, stormMotionY;
        /* ----- Calculate Storm Motion x,y (kt) ----- */
        stormMotionX = ucomp(sdir, sspd);
        stormMotionY = vcomp(sdir, sspd);

        /* ----- Find lowest observation in layer ----- */
        boolean foundLow = false;
        int lowIndex = 0;
        for (int i = 0; i < sndLys.size(); i++) {
            if ((NsharpLibBasics.agl(sndLys, sndLys.get(i).getGeoHeight()) > lower)) {
                lowIndex = i;
                foundLow = true;
                break;
            }
        }

        /* ----- Find highest observation in layer ----- */
        boolean foundHi = false;
        int upperIndex = sndLys.size() - 1;
        for (int i = sndLys.size() - 1; i >= 0; i--) {
            if ((NsharpLibBasics.agl(sndLys, sndLys.get(i).getGeoHeight()) < upper)) {
                upperIndex = i;
                foundHi = true;
                break;
            }
        }

        /* ----- Start with interpolated bottom layer ----- */
        float sru1, srv1, sru2, srv2, lyrh;
        sru1 = NsharpLibBasics.kt_to_mps(NsharpLibBasics.i_wndu(
                sndLys,
                NsharpLibBasics.i_pres(sndLys,
                        NsharpLibBasics.msl(sndLys, lower)))
                - stormMotionX);
        srv1 = NsharpLibBasics.kt_to_mps(NsharpLibBasics.i_wndv(
                sndLys,
                NsharpLibBasics.i_pres(sndLys,
                        NsharpLibBasics.msl(sndLys, lower)))
                - stormMotionY);

        float phel;// Positive helicity in layer (m2/s2)
        float nhel;// Negative helicity in layer (m2/s2)
        phel = nhel = 0.0f;

        if(foundHi && foundLow){
        	for (int i = lowIndex; i <= upperIndex; i++) {
        		if (NsharpLibBasics.qc(sndLys.get(i).getWindSpeed())
        				&& NsharpLibBasics.qc(sndLys.get(i).getWindDirection())) {
        			sru2 = NsharpLibBasics.kt_to_mps(ucomp(sndLys.get(i)
        					.getWindDirection(), sndLys.get(i).getWindSpeed())
        					- stormMotionX);
        			srv2 = NsharpLibBasics.kt_to_mps(vcomp(sndLys.get(i)
        					.getWindDirection(), sndLys.get(i).getWindSpeed())
        					- stormMotionY);

        			lyrh = (sru2 * srv1) - (sru1 * srv2);
        			if (lyrh > 0.0) {
        				phel += lyrh;
        			} else {
        				nhel += lyrh;
        			}
        			sru1 = sru2;
        			srv1 = srv2;
        		}
        	}
        }

        /* ----- Finish with interpolated top layer ----- */
        sru2 = NsharpLibBasics.kt_to_mps(NsharpLibBasics.i_wndu(
                sndLys,
                NsharpLibBasics.i_pres(sndLys,
                        NsharpLibBasics.msl(sndLys, upper)))
                - stormMotionX);
        srv2 = NsharpLibBasics.kt_to_mps(NsharpLibBasics.i_wndv(
                sndLys,
                NsharpLibBasics.i_pres(sndLys,
                        NsharpLibBasics.msl(sndLys, upper)))
                - stormMotionY);

        lyrh = (sru2 * srv1) - (sru1 * srv2);
        if (lyrh > 0.0) {
            phel += lyrh;
        } else {
            nhel += lyrh;
        }
        heli.setNegHelicity(nhel);
        heli.setPosHelicity(phel);
        heli.setTotalHelicity(nhel + phel);
        return heli;
    }

    public static WindComponent sr_wind(List<NcSoundingLayer> sndLys,
            float pbot, float ptop, float stdir, float stspd)
    /*************************************************************/
    /* SR_WIND */
    /* Calculates a pressure-weighted SR mean wind thru the */
    /* layer (pbot-ptop). Default layer is SFC-3KM. */
    /*                                                           */
    /* Input: */
    /* pbot - Bottom level of layer (mb) */
    /* ptop - Top level of layer (mb) */
    /* stdir - Storm motion dirction (deg) */
    /* stspd - Storm motion speed (kt) */
    /* Return: WindComponent structure with definitions below */
    /* mnu - U-Component of mean wind (kt) */
    /* mnv - V-Component of mean wind (kt) */
    /* wdir - direction of mean wind */
    /* wspd - speed of mean wind (kt) */
    /*************************************************************/
    {
        WindComponent windComp = new WindComponent();

        if (!NsharpLibBasics.qc(stspd) || !NsharpLibBasics.qc(stdir)
                || !NsharpLibBasics.qc(pbot) || !NsharpLibBasics.qc(ptop)) {
            return windComp;
        }
        float stu, stv;
        /* ----- Calculate Storm motion vectors ----- */
        stu = ucomp(stdir, stspd);
        stv = vcomp(stdir, stspd);

        /* ----- Check for Default Values ----- */
        if (pbot == -1) {
            pbot = NsharpLibBasics.sfcPressure(sndLys);
        }
        if (ptop == -1) {
            ptop = NsharpLibBasics.i_pres(sndLys,
                    NsharpLibBasics.msl(sndLys, 3000.0f));
        }

        float pinc = (pbot - ptop) / 20.0f;

        float usum, vsum, wgt;
        if (pinc < 1.0) {
        	float winduBot, winduTop, windvBot, windvTop;
            winduBot = NsharpLibBasics.i_wndu(sndLys, pbot);
            winduTop = NsharpLibBasics.i_wndu(sndLys, ptop);
            windvBot = NsharpLibBasics.i_wndv(sndLys, pbot);
            windvTop = NsharpLibBasics.i_wndv(sndLys, ptop);
            if (!NsharpLibBasics.qc(winduBot) || !NsharpLibBasics.qc(winduTop)
                    || !NsharpLibBasics.qc(windvBot)
                    || !NsharpLibBasics.qc(windvTop)) {
                return windComp;
            }
            usum = ((winduBot - stu) * pbot)
                    + ((winduTop - stu) * ptop);
            vsum = ((windvBot - stv) * pbot)
                    + ((windvTop - stv) * ptop);
            wgt = pbot + ptop;
        } else {
            wgt = usum = vsum = 0;
            for (float i = pbot; i >= ptop; i -= pinc) {
                float w1 = i;
                if(!NsharpLibBasics.qc( NsharpLibBasics.i_wndu(sndLys, i)) || 
            			!NsharpLibBasics.qc( NsharpLibBasics.i_wndv(sndLys, i))){
            	    continue;
            	}
                usum = usum + ((NsharpLibBasics.i_wndu(sndLys, i) - stu) * w1);
                vsum = vsum + ((NsharpLibBasics.i_wndv(sndLys, i) - stv) * w1);
                wgt = wgt + w1;
            }
        }
        if(wgt==0){
        	return windComp;
        }
        float mnu, mnv;
        mnu = usum / wgt;
        mnv = vsum / wgt;
        windComp.setUcomp(mnu);
        windComp.setVcomp(mnv);

        if (NsharpLibBasics.qc(mnu) && NsharpLibBasics.qc(mnv)) {
            windComp.setWdir(angle(mnu, mnv));
            windComp.setWspd(speed(mnu, mnv));
        }

        return windComp;
    }

    public static WindComponent[] corfidi_MCS_motion(
            List<NcSoundingLayer> sndLys)
    /*************************************************************/
    /* CORFIDI_FPMCS */
    /*                                                           */
    /* Calculates the Corfidi forward-propagating mcs motion. */
    /* Return: */
    /* WindComponent[0]:wind component of corfidi down */
    /* WindComponent[1]:wind component of corfidi up */
    /*************************************************************/
    {
        float u850, v850, uSfc, vSfc;
        /* 850-300mb mean wind, parcel number is ignored for this invoking */
        WindComponent windComp850 = mean_wind_npw(sndLys, 850, 300, 0);
        u850 = windComp850.getUcomp();
        v850 = windComp850.getVcomp();
        /* Sfc-1500m mean wind, parcel number is ignored for this invoking */
        WindComponent windCompSfc = mean_wind_npw(
                sndLys,
                NsharpLibBasics.sfcPressure(sndLys),
                NsharpLibBasics.i_pres(sndLys,
                        NsharpLibBasics.msl(sndLys, 1500.0f)), 0);
        uSfc = windCompSfc.getUcomp();
        vSfc = windCompSfc.getVcomp();

        /* Calculate the Upwind-Propagating MCS motion vector */
        float corfidiUpU = u850 - uSfc;
        float corfidiUpV = v850 - vSfc;
        float corfidiUpDir = angle(corfidiUpU, corfidiUpV);
        float corfidiUpSpd = speed(corfidiUpU, corfidiUpV);
        WindComponent windCompUp = new WindComponent();
        windCompUp.setUcomp(corfidiUpU);
        windCompUp.setVcomp(corfidiUpV);
        windCompUp.setWdir(corfidiUpDir);
        windCompUp.setWspd(corfidiUpSpd);
        /* Calculate the Downwind_Propagating MCS motion vector */
        float corfidiDownU = u850 + corfidiUpU;
        float corfidiDownV = v850 + corfidiUpV;
        float corfidiDownDir = angle(corfidiDownU, corfidiDownV);
        float corfidiDownSpd = speed(corfidiDownU, corfidiDownV);
        WindComponent windCompDown = new WindComponent();
        windCompDown.setUcomp(corfidiDownU);
        windCompDown.setVcomp(corfidiDownV);
        windCompDown.setWdir(corfidiDownDir);
        windCompDown.setWspd(corfidiDownSpd);
        WindComponent[] windComp = new WindComponent[2];
        windComp[0] = windCompDown;
        windComp[1] = windCompUp;
        return windComp;
    }

    public static WindComponent max_wind(List<NcSoundingLayer> sndLys,
            float lower, float upper)
    /************************************************************
     * MAX_WIND Finds the maximum wind speed between levels upper and lower.
     * 
     * @param lower
     *            = Bottom level of layer (mb) [ -1=SFC]
     * @param upper
     *            = Top level of layer (mb) [ -1=low 300mb]
     * @return max wind speed and direction
     ************************************************************/
    {
        float s1, p_maxspd, pres, maxspd;

        WindComponent windComp = new WindComponent();
        /* ----- See if default layer is specified ----- */
        if (lower == -1) {
            lower = NsharpLibBasics.sfcPressure(sndLys);
            if (!NsharpLibBasics.qc(NsharpLibBasics.i_wdir(sndLys, lower))) {
                return windComp;
            }
        }
        if (upper == -1) {
            upper = NsharpLibBasics.sfcPressure(sndLys) - 300;
        }

        /* ----- Make sure this is a valid layer ----- */
        while (!NsharpLibBasics.qc(NsharpLibBasics.i_wspd(sndLys, upper))) {
            upper += 50.0;
            if (upper > NsharpLibBasics.sfcPressure(sndLys)) {
                return windComp;
            }
        }
        if (!NsharpLibBasics.qc(NsharpLibBasics.i_wdir(sndLys, lower))) {
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
                foundLow = true;
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
                foundHi = true;
                break;
            }
        }

        /* ----- Start with interpolated bottom layer ----- */
        maxspd = NsharpLibBasics.i_wspd(sndLys, lower);
        p_maxspd = lower;

        if(foundHi && foundLow){
        	for (int i = lowerIndex; i <= upperIndex; i++) {
        		if (NsharpLibBasics.qc(sndLys.get(i).getPressure())) {
        			/* ----- Calculate every level that reports a wind speed ----- */
        			pres = sndLys.get(i).getPressure();
        			s1 = NsharpLibBasics.i_wspd(sndLys, pres);
        			if (s1 > maxspd) {
        				maxspd = s1;
        				p_maxspd = pres;
        			}
        		}
        	}
        }
        /* ----- Finish with interpolated top layer ----- */
        s1 = NsharpLibBasics.i_wspd(sndLys, upper);
        if (s1 > maxspd) {
            p_maxspd = upper;
        }
        windComp.setWspd(maxspd);
        windComp.setWdir(NsharpLibBasics.i_wdir(sndLys, p_maxspd));
        return windComp;
    }
}
