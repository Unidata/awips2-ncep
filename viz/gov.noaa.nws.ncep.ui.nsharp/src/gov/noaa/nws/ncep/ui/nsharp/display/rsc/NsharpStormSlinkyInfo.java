package gov.noaa.nws.ncep.ui.nsharp.display.rsc;

/**
 * 
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system. 
 * 
 * All methods developed in this class are based on the algorithm developed in BigSharp 
 * native C file,xwvid5.c, by John A. Hart/SPC.
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
import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibThermo;
import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibWinds;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.LayerParameters;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.WindComponent;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;

import java.util.ArrayList;
import java.util.List;

public class NsharpStormSlinkyInfo {
    private float tottim = 0;

    private float angl = 0;

    private float stormSlinkXBase;

    private float stormSlinkYBase;

    private List<Float> totalSu = new ArrayList<>();

    private List<Float> totalSv = new ArrayList<>();

    private List<Integer> color = new ArrayList<>();

    public NsharpStormSlinkyInfo() {
        super();
    }

    public void setTottim(float tottim) {
        this.tottim = tottim;
    }

    public void setAngl(float angl) {
        this.angl = angl;
    }

    public float getTottim() {
        return tottim;
    }

    public float getAngl() {
        return angl;
    }

    public float getStormSlinkXBase() {
        return stormSlinkXBase;
    }

    public void setStormSlinkXBase(float stormSlinkXBase) {
        this.stormSlinkXBase = stormSlinkXBase;
    }

    public float getStormSlinkYBase() {
        return stormSlinkYBase;
    }

    public void setStormSlinkYBase(float stormSlinkYBase) {
        this.stormSlinkYBase = stormSlinkYBase;
    }

    public List<Float> getTotalSu() {
        return totalSu;
    }

    public List<Float> getTotalSv() {
        return totalSv;
    }

    public List<Integer> getColor() {
        return color;
    }

    public static NsharpStormSlinkyInfo computeStormSlinky(float lower,
            float upper, float pres, float temp, float dwpt,
            List<NcSoundingLayer> sndLys, float smdir, float smspd)
    /*************************************************************
     * This function is to get an array of points for storm slinky inset
     * plotting. It is based on Bigsharp xwvid5.c, visual1()
     * 
     * Lifts specified parcel, given an initial 5 m/s push. parcel trajectory is
     * then calculated, using strict parcel theory. Updraft size is assumed 1km
     * dia.
     * 
     * All calculations use the virtual temperature correction.
     * 
     * @param lower
     *            = Bottom level of layer (mb)
     * @param upper
     *            = Top level of layer (mb)
     * @param pres
     *            = LPL pressure (mb)
     * @param temp
     *            = LPL temperature (c)
     * @param dwpt
     *            = LPL dew point (c)
     * @return instance of NsharpStormSlinkyInfo with storm slinky plotting
     *         data, or null if error
     *************************************************************/
    {
        float te1, pe1, te2, pe2, h1, h2, lyre, tdef1, tdef2, totp;
        float te3, pe3, h3, tp1, tp2, tp3, tdef3, lyrf;
        float dh, restim, uvv, tottim;
        float du, dv, tsu, tsv, tdist, angl;

        lyre = -1.0F;
        totp = 25.0F;
        NsharpStormSlinkyInfo stormSlinkyInfo = new NsharpStormSlinkyInfo();

        /* ----- Make sure this is a valid layer ----- */
        if (lower > pres) {
            lower = pres;
        }
        if (!NsharpLibBasics.qc(NsharpLibBasics.i_vtmp(sndLys, upper))) {
            return stormSlinkyInfo;
        }
        if (!NsharpLibBasics.qc(NsharpLibBasics.i_vtmp(sndLys, lower))) {
            return stormSlinkyInfo;
        }

        /* ----- Begin with Mixing Layer (LPL-LCL) ----- */
        te1 = NsharpLibBasics.i_vtmp(sndLys, pres);
        pe1 = lower;
        h1 = NsharpLibBasics.i_hght(sndLys, pe1);
        tp1 = NsharpLibThermo.virtemp(pres, temp, dwpt);

        LayerParameters dryliftLayer = NsharpLibThermo
                .drylift(pres, temp, dwpt);
        pe2 = dryliftLayer.getPressure();
        tp2 = dryliftLayer.getTemperature();
        h2 = NsharpLibBasics.i_hght(sndLys, pe2);
        te2 = NsharpLibBasics.i_vtmp(sndLys, pe2);

        if (lower > pe2) {
            lower = pe2;
        }

        int lowIndex = 0, upperIndex = 0;
        /* ----- Find lowest observation in layer ----- */
        for (int i = 0; i < sndLys.size(); i++) {
            if (NsharpLibBasics.qc(sndLys.get(i).getPressure())
                    && sndLys.get(i).getPressure() < lower) {
                lowIndex = i;
                break;
            }
        }

        /* ----- Find highest observation in layer ----- */
        for (int i = sndLys.size() - 1; i >= 0; i--) {
            if (NsharpLibBasics.qc(sndLys.get(i).getPressure())
                    && sndLys.get(i).getPressure() > upper) {
                upperIndex = i;
                break;
            }
        }

        /* ----- Start with interpolated bottom layer ----- */
        pe1 = lower;
        h1 = NsharpLibBasics.i_hght(sndLys, pe1);
        te1 = NsharpLibBasics.i_vtmp(sndLys, pe1);
        tp1 = NsharpLibThermo.wetlift(pe2, tp2, pe1);
        tsu = 0.0F;
        tsv = 0.0F;
        restim = 0.0F;
        tottim = 0.0F;
        for (int i = lowIndex; i < sndLys.size() && i <= upperIndex; i++) {
            NcSoundingLayer sndLy = sndLys.get(i);
            if (NsharpLibBasics.qc(sndLy.getTemperature())) {
                /* ----- Calculate every level that reports a temp ----- */
                pe2 = sndLy.getPressure();
                h2 = sndLy.getGeoHeight();
                te2 = NsharpLibBasics.i_vtmp(sndLys, pe2);
                tp2 = NsharpLibThermo.wetlift(pe1, tp1, pe2);
                tdef1 = (NsharpLibThermo.virtemp(pe1, tp1, tp1) - te1)
                        / (te1 + 273.15F);
                tdef2 = (NsharpLibThermo.virtemp(pe2, tp2, tp2) - te2)
                        / (te2 + 273.15F);

                lyre = 9.8F * (tdef1 + tdef2) / 2.0F * (h2 - h1);

                if (lyre > 0.0F) {
                    totp += lyre;
                }
                uvv = (float) Math.sqrt((double) (totp * 2.0F));
                dh = h2 - h1;
                restim = dh / uvv;
                tottim += restim;

                WindComponent srWindComp = NsharpLibWinds.sr_wind(sndLys, pe1,
                        pe2, smdir, smspd);
                du = NsharpLibBasics.kt_to_mps(srWindComp.getUcomp()) * restim;
                dv = NsharpLibBasics.kt_to_mps(srWindComp.getVcomp()) * restim;
                tsu -= du;
                tsv += dv;
                tdist = (float) Math.sqrt((double) (tsu * tsu)
                        + (double) (tsv * tsv));

                pe1 = pe2;
                h1 = h2;
                te1 = te2;
                tp1 = tp2;

                /* ----- Is this the top of given layer ----- */
                if (i >= upperIndex) {
                    pe3 = pe1;
                    h3 = h1;
                    te3 = te1;
                    tp3 = tp1;
                    lyrf = lyre;

                    if (lyrf > 0.0F) {
                        totp -= lyrf;
                    }

                    pe2 = upper;
                    h2 = NsharpLibBasics.i_hght(sndLys, pe2);
                    te2 = NsharpLibBasics.i_vtmp(sndLys, pe2);
                    tp2 = NsharpLibThermo.wetlift(pe3, tp3, pe2);
                    tdef3 = (NsharpLibThermo.virtemp(pe3, tp3, tp3) - te3)
                            / (te3 + 273.15F);
                    tdef2 = (NsharpLibThermo.virtemp(pe2, tp2, tp2) - te2)
                            / (te2 + 273.15F);
                    lyrf = 9.8F * (tdef3 + tdef2) / 2.0F * (h2 - h3);
                    if (lyrf > 0.0F) {
                        totp += lyrf;
                    }

                    uvv = (float) Math.sqrt((double) (totp * 2.0F));
                    dh = h2 - h1;
                    restim = dh / uvv;
                    tottim += restim;

                    srWindComp = NsharpLibWinds.sr_wind(sndLys, pe1, pe2,
                            smdir, smspd);
                    du = NsharpLibBasics.kt_to_mps(srWindComp.getUcomp())
                            * restim;
                    dv = NsharpLibBasics.kt_to_mps(srWindComp.getVcomp())
                            * restim;
                    tsu -= du;
                    tsv += dv;
                    tdist = (float) Math.sqrt((double) (tsu * tsu)
                            + (double) (tsv * tsv));
                    stormSlinkyInfo.getTotalSu().add(tsu);
                    stormSlinkyInfo.getTotalSv().add(tsv);
                    // based on BigNsharp, gempak color 7 Magenta
                    stormSlinkyInfo.getColor().add(7);
                    angl = 90.0F - NsharpLibWinds.angle(tdist,
                            NsharpLibBasics.agl(sndLys, h2));
                    stormSlinkyInfo.setTottim(tottim);
                    stormSlinkyInfo.setAngl(angl);
                    stormSlinkyInfo.setStormSlinkXBase(NsharpLibWinds.ucomp(
                            smdir, 30));
                    stormSlinkyInfo.setStormSlinkYBase(NsharpLibWinds.vcomp(
                            smdir, 30));
                    return stormSlinkyInfo;
                }
                // based on BigNsharp to set color
                int colrx = 13;
                if (h2 > NsharpLibBasics.msl(sndLys, 3000))
                    colrx = 3;
                if (h2 > NsharpLibBasics.msl(sndLys, 6000))
                    colrx = 27;
                if (h2 > NsharpLibBasics.msl(sndLys, 9000))
                    colrx = 20;
                if (h2 > NsharpLibBasics.msl(sndLys, 12000))
                    colrx = 6;
                stormSlinkyInfo.getColor().add(colrx);
                stormSlinkyInfo.getTotalSu().add(tsu);
                stormSlinkyInfo.getTotalSv().add(tsv);
            }
        }
        return stormSlinkyInfo;
    }
}
