package gov.noaa.nws.ncep.edex.plugin.soundingrequest.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import gov.noaa.nws.ncep.common.tools.IDecoderConstantsN;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;

/**
 * 
 * This code has been developed by the SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#     Engineer    Description
 * -------     -------     --------     -----------
 * 08/21/2010  301         T. Lee       Initial coding
 * 09/15/2010  301         C. Chen      Added DB retrieval
 * 09/22/2010  301         T. Lee       Added UAIR merging algorithm
 * 11/05/2010  301         C. Chen      Minor changes to fix index out of bound issue
 * 11/15/2010  301         C. Chen      fix a index out of bound bug
 * 12/2010     301         T. Lee/NCEP  Re-factored for BUFRUA
 * 5/10/2011   301         C. Chen      added rhToDewpoint(), tempToVapr()
 * 02/28/2012              C. Chen      modify several sounding query algorithms for better performance
 * 8/2012                  T. Lee/NCEP  Removed missing wind interpolation
 * 8/2012                  T. Lee/NCEP  Fixed max wind merging; May fix NSHARP EL calculation
 * 12/2013     1115        T. Lee/NCEP  Fixed missing height at top level before sorting
 * 3/2014      1116        T. Lee/NCEP  Added dpdToDewpoint for cmcHR (15km) data
 * 05/06/2015  RM#7783     C. Chen      add "convertDewpoint" method to consider all dew point conversions, 
 *                                      from RH, DpD, or SH to dew point at one place for better performance  
 * 05/20/2015  RM#8306     Chin Chen    eliminate NSHARP dependence on uEngine.
 *                                      Copy whole file from uEngine project
 *                                      and "refactor" and clean up unused code for this ticket.
 * 09/15/2015  RM#11676    Chin Chen    interpolated values for single level are wrong; clean up software
 * 09/22/2016  RM15953     R.Reynolds   Added capability for wind interpolation
 * 01/23/2017  RM22658     J. Beck      Changed mergeUairSounding() to return below ground levels
 * 03/02/2017  18784       wkwock       format the code use AWIPS standard.
 * </pre>
 * 
 * @author T. Lee
 * @version 1.0
 */

public class MergeSounding {

    private final float missingFloat = IDecoderConstantsN.UAIR_FLOAT_MISSING;

    private final int missingInteger = IDecoderConstantsN.INTEGER_MISSING;

    // input flag used for ConstructMissing()
    private enum MissFlag {
        MISSING_TEMP, MISSING_DEWPOINT, MISSING_WIND
    };

    private final float TMCK = 273.15f;

    // return value for isNumber()
    public final int INTEGER_NUM = 0;

    public final int FLOAT_NUM = 1;

    public final int DOUBLE_NUM = 2;

    public final int NOT_NUM = -1;

    // return value for getNumberType()
    public enum NumType {
        INTEGER_NUM, FLOAT_NUM, DOUBLE_NUM, NOT_NUM
    };

    /**
     * Default constructor
     */

    public MergeSounding() {

    }

    /*
     * Process native sounding data. Convert specific humidity to dew point
     * temperature then compute the moist height.
     */
    public List<NcSoundingLayer> nativeModelSounding(List<NcSoundingLayer> sls,
            float elevation) {
        spfhToDewpoint(sls);
        constructHeight(sls, elevation);
        return sls;
    }

    /*
     * Process upper air sounding data. Note that TTAA is the original/sorted
     * data, while MAN is the TTAA without underground data and MAN_D is the
     * TTAA for display, i.e., the first level is the surface level, any under
     * -ground levels will be above the surface level.
     */
    public List<NcSoundingLayer> mergeUairSounding(String level,
            List<NcSoundingLayer> ttaa, List<NcSoundingLayer> ttbb,
            List<NcSoundingLayer> ttcc, List<NcSoundingLayer> ttdd,
            List<NcSoundingLayer> ppaa, List<NcSoundingLayer> ppbb,
            List<NcSoundingLayer> ppcc, List<NcSoundingLayer> ppdd,
            List<NcSoundingLayer> trop_a, List<NcSoundingLayer> trop_c,
            List<NcSoundingLayer> wmax_a, List<NcSoundingLayer> wmax_c,
            float elevation, boolean windInterpolation) {
        List<NcSoundingLayer> sndata = new ArrayList<>();
        List<NcSoundingLayer> man = null;

        // Return the specific levels requested by users
        if (ttaa.size() > 0) {
            Collections.sort(ttaa, new reverseSortByPressure());

            if (level != null && level.toUpperCase().equalsIgnoreCase("MAN")) {
                return ttaa;
            }
            man = removeUnderGround(ttaa);

        } else {
            if (ppaa.size() < 1 && ttbb.size() < 1) {
                return missingSounding();
            }
            man = missingSounding();
        }

        // Sorting the data

        if (ttbb.size() > 0) {
            Collections.sort(ttbb, new reverseSortByPressure());
        }

        if (ttcc.size() > 0) {
            Collections.sort(ttcc, new reverseSortByPressure());

        }

        if (ttdd.size() > 0) {
            Collections.sort(ttdd, new reverseSortByPressure());

        }

        if (ppaa.size() > 0) {
            if (checkWindData(ppaa)) {
                Collections.sort(ppaa, new MergeSounding.sortByHeight());

            } else {
                Collections.sort(ppaa,
                        new MergeSounding.reverseSortByPressure());

            }
        }

        if (ppcc.size() > 0) {
            if (checkWindData(ppcc)) {
                Collections.sort(ppcc, new MergeSounding.sortByHeight());

            } else {
                Collections.sort(ppcc,
                        new MergeSounding.reverseSortByPressure());
            }
        }

        if (ppbb.size() > 0) {
            if (checkWindData(ppbb)) {
                Collections.sort(ppbb, new MergeSounding.sortByHeight());

            } else {
                Collections.sort(ppbb,
                        new MergeSounding.reverseSortByPressure());
            }
        }

        if (ppdd.size() > 0) {
            if (checkWindData(ppdd)) {
                Collections.sort(ppdd, new MergeSounding.sortByHeight());
            } else {
                Collections.sort(ppdd,
                        new MergeSounding.reverseSortByPressure());
            }

        }

        // Find surface data, return if users request surface data only.
        NcSoundingLayer sl = new NcSoundingLayer();
        sl = getSurfaceData(man, ttbb, ppbb, elevation);
        sndata.add(0, sl);

        if (level != null && getNumberType(level) != NumType.NOT_NUM) {
            if (equal(0.f, Float.valueOf(level.trim()).floatValue())
                    || equal(sl.getPressure(),
                            Float.valueOf(level.trim()).floatValue())) {
                return sndata;
            }
        }

        // Merge mandatory data
        mergeMandatory(man, ttcc, sndata);

        // Check if the single level is mandatory or not
        if (level != null && getNumberType(level) != NumType.NOT_NUM) {
            for (int kk = 0; kk < ttaa.size(); kk++) {
                if (equal(Float.valueOf(level.trim()).floatValue(),
                        ttaa.get(kk).getPressure())) {
                    sl.setPressure(ttaa.get(kk).getPressure());
                    sl.setTemperature(ttaa.get(kk).getTemperature());
                    sl.setDewpoint(ttaa.get(kk).getDewpoint());
                    sl.setWindDirection(ttaa.get(kk).getWindDirection());
                    sl.setWindSpeed(ttaa.get(kk).getWindSpeed());
                    sl.setGeoHeight(ttaa.get(kk).getGeoHeight());
                    sl.setOmega(ttaa.get(kk).getOmega());
                    sndata.clear();
                    sndata.add(sl);
                    return sndata;
                }
            }
        }

        // Merge mandatory winds
        mergeMandatoryWinds(ppaa, ppcc, sndata);

        // Merge tropopause
        mergeTropSigTemp(trop_a, trop_c, sndata);

        // Merge TTBB
        mergeTropSigTemp(ttbb, ttdd, sndata);

        constructTtbbHeight(sndata);

        if (!checkWindData(ppbb)) {
            mergeSigMaxWindOnPressure(ppbb, ppdd, sndata);

        }

        mergeSigMaxWindOnPressure(wmax_a, wmax_c, sndata);

        constructPpbbHeight(sndata);

        if (checkWindData(ppbb)) {
            mergeSigWindOnHeight(ppbb, ppdd, sndata);
            constructPpbbPressure(sndata);
        }

        // Interpolate missing temperature, dew point and winds.
        constructMissing(MissFlag.MISSING_TEMP, sndata);
        constructMissing(MissFlag.MISSING_DEWPOINT, sndata);

        if (windInterpolation) {
            constructMissing(MissFlag.MISSING_WIND, sndata);
        }

        // Return single level or add underground mandatory data to the sounding
        // profile

        List<NcSoundingLayer> sndout = new ArrayList<>();
        sndout = removeMissingPressure(sndata);
        if (level != null && getNumberType(level) == NumType.INTEGER_NUM) {
            float rlev = new Integer(Integer.parseInt(level.trim()))
                    .floatValue();
            return getSingLevel(rlev, sndout);
        } else if (level != null && getNumberType(level) == NumType.FLOAT_NUM) {
            float rlev = new Float(Float.parseFloat(level.trim()));
            return getSingLevel(rlev, sndout);
        } else {
            return addUnderGround(ttaa, sndout);
        }
    }

    /**
     * Check an alpha-numerical string is a number or characters. Returns:
     * INTEGER_NUM: integer FLOAT_NUM: float DOUBLE_NUM: double NOT_NUM: not a
     * number
     * 
     * @Deprecated It is deprecated and should use getNumberType instead
     */
    @Deprecated
    public int isNumber(String level) {
        try {
            if (Integer.parseInt(level) >= 0) {
                return INTEGER_NUM;
            } else {
                return NOT_NUM;
            }
        } catch (NumberFormatException nfe1) {
            try {
                if (Float.parseFloat(level) >= 0.f) {
                    return FLOAT_NUM;
                } else {
                    return NOT_NUM;
                }
            } catch (NumberFormatException nfe2) {
                try {
                    if (Double.parseDouble(level) >= 0.) {
                        return DOUBLE_NUM;
                    } else {
                        return NOT_NUM;
                    }
                } catch (NumberFormatException nfe3) {
                    return NOT_NUM;
                }
            }
        }
    }

    /*
     * Check an alpha-numerical string is a number or characters. Returns:
     * INTEGER_NUM: integer FLOAT_NUM: float DOUBLE_NUM: double NOT_NUM: not a
     * number
     */

    public NumType getNumberType(String level) {
        try {
            if (Integer.parseInt(level) >= 0) {
                return NumType.INTEGER_NUM;
            } else {
                return NumType.NOT_NUM;
            }
        } catch (NumberFormatException nfe1) {
            try {
                if (Float.parseFloat(level) >= 0.f) {
                    return NumType.FLOAT_NUM;
                } else {
                    return NumType.NOT_NUM;
                }
            } catch (NumberFormatException nfe2) {
                try {
                    if (Double.parseDouble(level) >= 0.) {
                        return NumType.DOUBLE_NUM;
                    } else {
                        return NumType.NOT_NUM;
                    }
                } catch (NumberFormatException nfe3) {
                    return NumType.NOT_NUM;
                }
            }
        }
    }

    /*
     * convert specific humidity to dew point temperature.
     */

    public List<NcSoundingLayer> spfhToDewpoint(List<NcSoundingLayer> sndata) {
        float spfh, pres;
        float dwpc = missingFloat;

        for (NcSoundingLayer layer : sndata) {
            if (layer.getDewpoint() == missingFloat) {
                spfh = layer.getSpecHumidity();
                pres = layer.getPressure();

                if (spfh == missingFloat || pres == missingFloat || spfh <= 0.f
                        || pres <= 0.f) {
                    continue;
                } else {
                    float rmix = spfh / (1.f - spfh);
                    float e = (pres * rmix) / (.62197f + rmix);
                    e = e / (1.001f + ((pres - 100.f) / 900.f) * .0034f);
                    dwpc = (float) (Math.log(e / 6.112) * 243.5
                            / (17.67 - Math.log((e / 6.112))));
                    layer.setDewpoint(dwpc);
                }

            }
        }
        return sndata;
    }

    /*
     * computes DWPC from TMPC and RELH Note: If DWPC is less than -190 degrees
     * C, it is treated as missing data Code is based on GEMPAK's prrhdp.f
     */
    public List<NcSoundingLayer> rhToDewpoint(List<NcSoundingLayer> sndata) {
        float rh, vapr, vaps, temp;
        float dwpc = missingFloat;

        for (NcSoundingLayer layer : sndata) {
            if (layer.getDewpoint() == missingFloat) {
                rh = layer.getRelativeHumidity();
                temp = layer.getTemperature();

                if (rh == missingFloat || temp == missingFloat) {
                    continue;
                } else {
                    vaps = tempToVapr(temp);
                    vapr = rh * vaps / 100;
                    if (vapr < Math.exp(-30))
                        continue;
                    else {
                        dwpc = (float) (243.5
                                * (Math.log(6.112) - Math.log(vapr))
                                / (Math.log(vapr) - Math.log(6.112) - 17.67));
                        layer.setDewpoint(dwpc);
                    }
                }
            }
        }
        return sndata;
    }

    /*
     * computes DWPC from TMPC and RELH Note: If DWPC is less than -190 degrees
     * C, it is treated as missing data Code is based on GEMPAK's prrhdp.f
     */
    public List<NcSoundingLayer> dpdToDewpoint(List<NcSoundingLayer> sndata) {
        float temp, dpdk;
        float dwpc = missingFloat;

        for (NcSoundingLayer layer : sndata) {
            if (layer.getDewpoint() == missingFloat) {
                temp = layer.getTemperature();
                dpdk = layer.getDpd();

                if (temp == missingFloat || dpdk == missingFloat) {
                    continue;
                } else {

                    dwpc = temp - dpdk;
                    layer.setDewpoint(dwpc);
                }
            }

        }
        return sndata;
    }

    /*
     * computes VAPR from TMPC Code is based on GEMPAK's prvapr.f
     */
    private float tempToVapr(float temp) {
        return (float) (6.112 * Math.exp((17.67 * temp) / (temp + 243.5)));
    }

    private void constructHeight(List<NcSoundingLayer> sndata, float elev) {

        /*
         * For native model sounding, using hypsometric equation to build height
         */
        int lev = sndata.size();
        float tb = missingFloat, tdb = missingFloat, pb = missingFloat;
        float tt = missingFloat, tdt = missingFloat, pt = missingFloat;
        float dwptsf, psfc, tmpcsf, scaleh, mhgt = missingFloat;

        for (int k = 0; k < lev; k++) {

            if (k == 0) {
                tmpcsf = sndata.get(k).getTemperature();
                dwptsf = sndata.get(k).getDewpoint();
                psfc = sndata.get(k).getPressure();
                tb = tmpcsf;
                tt = tmpcsf;
                tdb = dwptsf;
                tdt = dwptsf;
                pb = psfc;
                pt = psfc;

                scaleh = scaleHeight(tb, tt, tdb, tdt, pb, pt);
                mhgt = moistHeight(elev, pb, pt, scaleh);
            } else {
                tt = sndata.get(k).getTemperature();
                tdt = sndata.get(k).getDewpoint();
                pt = sndata.get(k).getPressure();
                scaleh = scaleHeight(tb, tt, tdb, tdt, pb, pt);

                mhgt = moistHeight(mhgt, pb, pt, scaleh);
                tb = tt;
                tdb = tdt;
                pb = pt;

            }
            sndata.get(k).setGeoHeight(mhgt);
        }
    }

    /*
     * Compute moist height.
     */
    private float moistHeight(float zb, float pb, float pt, float scale) {
        if (zb == missingFloat || pb == missingFloat || pt == missingFloat
                || scale == missingFloat) {
            return missingFloat;
        } else {
            return (float) (zb + scale * Math.log(pb / pt));
        }
    }

    /*
     * Compute scale height.
     */
    private float scaleHeight(float tb, float tt, float tdb, float tdt,
            float pb, float pt) {
        final float RDGAS = 287.04f, GRAVTY = 9.80616f, RKAP = RDGAS / GRAVTY;
        if (tb == missingFloat || tt == missingFloat || pb == missingFloat
                || pt == missingFloat) {
            return missingFloat;
        } else {
            float tvb = virtualTemperature(tb, tdb, pb);
            float tvt = virtualTemperature(tt, tdt, pt);
            if (tvb == missingFloat || tvt == missingFloat) {
                return missingFloat;
            } else {
                float tav = (tvb + tvt) / 2.0f;

                return (RKAP * tav);
            }
        }
    }

    /*
     * Compute virtual temperature
     */
    private float virtualTemperature(float tt, float td, float pres) {
        if (tt == missingFloat || pres == missingFloat) {
            return missingFloat;
        } else if (td == missingFloat) {
            return celciusToKevin(tt);
        } else {
            float tmpk = celciusToKevin(tt);
            float rmix = mixingRatio(td, pres);
            if (rmix == missingFloat) {
                return celciusToKevin(tt);
            } else {
                return tmpk * (1.f + .001f * rmix / .62197f)
                        / (1.f + .001f * rmix);
            }
        }
    }

    /*
     * Convert Celcius to Kelvin.
     */
    private float celciusToKevin(float tc) {
        if (tc == missingFloat) {
            return missingFloat;
        } else {
            return (tc + TMCK);
        }
    }

    /*
     * Compute mixing ratio from DWPC and PRES.
     */
    private float mixingRatio(float td, float pres) {
        if (td == missingFloat || pres == missingFloat) {
            return missingFloat;
        } else {
            float vapr = vaporPressure(td);
            if (vapr == missingFloat) {
                return missingFloat;
            }

            float corr = (1.001f + ((pres - 100.f) / 900.f) * .0034f);

            float e = corr * vapr;
            if (e > (.5f * pres)) {
                return missingFloat;
            } else {
                return .62197f * (e / (pres - e)) * 1000.f;
            }
        }
    }

    /*
     * Compute vapor pressure from DWPC.
     */
    private float vaporPressure(float td) {
        if (td == missingFloat) {
            return missingFloat;
        } else {
            return (6.112f * (float) Math.exp((17.67 * td) / (td + 243.5)));
        }
    }

    /*
     * Merge observed sounding data
     */

    /*
     * Check wind data if the data is reported on pressure or height surfaces.
     * Return TRUE if reported on height. (MR_CHKW)
     * 
     * Note that this is coded different from MR_CHKW, in that it will set zwind
     * to false only if pressure is less than 0. An odd logic.
     */
    public boolean checkWindData(List<NcSoundingLayer> sndata) {
        boolean zwind = true;
        for (int kk = 0; kk < sndata.size(); kk++) {
            if (sndata.get(kk).getPressure() != missingFloat) {
                zwind = false;
            }
        }
        return zwind;
    }

    /*
     * Find surface data. (MR_SRFC)
     */
    public NcSoundingLayer getSurfaceData(List<NcSoundingLayer> man,
            List<NcSoundingLayer> ttbb, List<NcSoundingLayer> ppbb,
            float elevation) {
        float psfc = missingFloat;
        NcSoundingLayer sl_sfc = new NcSoundingLayer();

        /*
         * Check for surface information in mandatory data.
         */
        if (man == null || man.size() < 1) {
            sl_sfc = missingSounding().get(0);
        } else {
            // If surface pressure is greater than 1080mb, set to missing.
            // Otherwise
            // surface pressure will be the first report level on TTAA.
            // Note that GEMPAK sets it to 1060mb.
            psfc = man.get(0).getPressure();
            if (psfc > 1080.) {
                sl_sfc.setPressure(missingFloat);
            } else {
                sl_sfc.setPressure(psfc);
            }
            sl_sfc.setTemperature(man.get(0).getTemperature());
            sl_sfc.setDewpoint(man.get(0).getDewpoint());
            sl_sfc.setWindDirection(man.get(0).getWindDirection());
            sl_sfc.setWindSpeed(man.get(0).getWindSpeed());
            sl_sfc.setGeoHeight(elevation);
            sl_sfc.setOmega(man.get(0).getOmega());
        }

        /*
         * Find the first reporting mandatory level above the surface.
         */
        float pman = missingFloat;
        int iman = 1;
        try {
            while (pman == missingFloat && iman < man.size()) {
                if (man.get(iman).getPressure() != missingFloat
                        && man.get(iman).getTemperature() != missingFloat
                        && man.get(iman).getGeoHeight() != missingFloat) {
                    pman = man.get(iman).getPressure();
                }
                iman++;
            }
        } catch (Exception e) {
            // do nothing
        }

        /*
         * If surface pressure is missing or is less than first reporting
         * mandatory level, set surface data to missing.
         */
        if (psfc == missingFloat || (psfc < pman && pman != missingFloat)) {
            sl_sfc = missingSounding().get(0);
        }

        /*
         * Use TTBB/PPBB to get surface data if TTAA is missing. The check for
         * significant level pressure to be less than or equal to psfc
         * eliminates erroneous data.
         */
        if (ttbb.size() > 0) {
            psfc = sl_sfc.getPressure();
            float psql = ttbb.get(0).getPressure();

            if (equal(psfc, psql) || psfc == missingFloat) {
                if (psql != missingFloat && psfc == missingFloat)
                    sl_sfc.setPressure(psql);

                if (sl_sfc.getTemperature() == missingFloat)
                    sl_sfc.setTemperature(ttbb.get(0).getTemperature());

                if (sl_sfc.getDewpoint() == missingFloat)
                    sl_sfc.setDewpoint(ttbb.get(0).getDewpoint());
            }
        }

        /*
         * If the first significant level wind data is surface information, use
         * it to replace the surface data if the pressure is at surface.
         */

        // PPBB reported in Height.
        if (checkWindData(ppbb)) {
            if (ppbb.size() > 0) {
                if (ppbb.get(0).getGeoHeight() == 0.
                        && sl_sfc.getWindDirection() == missingFloat) {
                    sl_sfc.setWindDirection(ppbb.get(0).getWindDirection());
                    sl_sfc.setWindSpeed(ppbb.get(0).getWindSpeed());
                }
            }
        } else {
            // PPBB reported in Pressure.
            if (ppbb.size() > 0) {
                if (ppbb.get(0).getPressure() != missingFloat
                        && sl_sfc.getPressure() == missingFloat) {
                    float psgl = Math.abs(ppbb.get(0).getPressure());
                    sl_sfc.setPressure(psgl);

                    if (ppbb.get(0).getWindDirection() != missingFloat
                            && sl_sfc.getWindDirection() == missingFloat) {
                        if (equal(psfc, psgl)) {
                            sl_sfc.setWindDirection(
                                    ppbb.get(0).getWindDirection());
                            sl_sfc.setWindSpeed(ppbb.get(0).getWindSpeed());
                        }
                    }
                }
            }
        }

        /*
         * Return surface data.
         */
        return sl_sfc;
    }

    private boolean equal(float x, float y) {
        final float RDIFFD = .0001f;
        if ((x + y) == 0.) {
            return Math.abs(x - y) < RDIFFD;
        } else {
            return Math.abs(x - y) / Math.abs((x + y) / 2.) < RDIFFD;
        }
    }

    /*
     * Merge the mandatory below 100 mb data and the mandatory above data.
     * sndata has surface observation ONLY. (MR_MAND)
     */

    public void mergeMandatory(List<NcSoundingLayer> man_a,
            List<NcSoundingLayer> man_c, List<NcSoundingLayer> sndata) {

        float plast;
        if (man_a.size() < 1 && man_c.size() < 1) {
            return;
        }

        if (sndata.get(0).getPressure() == missingFloat) {
            plast = 2000.f;
        } else {
            plast = sndata.get(0).getPressure();
        }

        /*
         * Move the mandatory data below 100mb to the output array, sndata.
         * Check that pressure is not missing and is decreasing.
         */
        float pres;
        if (man_a.size() > 0) {
            for (int kk = 1; kk < man_a.size(); kk++) {
                pres = man_a.get(kk).getPressure();
                if (pres < plast && pres != missingFloat
                        && (man_a.get(kk).getTemperature() != missingFloat
                                || man_a.get(kk)
                                        .getWindDirection() != missingFloat)) {
                    addDataToList(kk, man_a, sndata);
                    plast = pres;
                }
            }
        }

        /*
         * Move the mandatory data above 100 mb to the output array.
         */
        if (man_c.size() > 0) {
            for (int kk = 0; kk < man_c.size(); kk++) {
                pres = man_c.get(kk).getPressure();
                if (pres < plast && pres != missingFloat
                        && man_c.get(kk).getTemperature() != missingFloat) {
                    addDataToList(kk, man_c, sndata);
                    plast = man_c.get(kk).getPressure();
                }
            }
        }
    }

    /*
     * Merge the mandatory below 100 mb wind data and the mandatory above wind
     * data. (MR_MANW)
     */
    public void mergeMandatoryWinds(List<NcSoundingLayer> man_wa,
            List<NcSoundingLayer> man_wc, List<NcSoundingLayer> sndata) {

        if (man_wa.size() < 1 && man_wc.size() < 1) {
            return;
        }

        /*
         * Append data.
         */
        if (man_wc.size() > 0) {
            for (int kk = 0; kk < man_wc.size(); kk++) {
                man_wa.add(man_wc.get(kk));
            }
        }
        /*
         * Loop through mandatory wind data.
         */
        for (int lev = 0; lev < man_wa.size(); lev++) {

            /*
             * If this is the correct level, add wind data.
             */
            boolean found = false;
            float ppp = man_wa.get(lev).getPressure();
            for (int kk = 0; kk < sndata.size() && !found; kk++) {
                float pres = sndata.get(kk).getPressure();
                if (equal(ppp, pres)) {
                    if (sndata.get(kk).getWindDirection() == missingFloat) {
                        sndata.get(kk).setWindDirection(
                                man_wa.get(lev).getWindDirection());
                        sndata.get(kk)
                                .setWindSpeed(man_wa.get(lev).getWindSpeed());
                    }
                    found = true;
                }
            }

            /*
             * If not found, add to the list
             */
            if (!found) {
                float ddd = man_wa.get(lev).getWindDirection();
                if (ppp != missingFloat && ddd != missingFloat) {
                    addDataToList(lev, man_wa, sndata);
                }
            }
        }
    }

    /*
     * Merge tropopause, max wind and significant temperature data (TTBB) to the
     * station data array. The input parameter could be tropopause data or
     * significant temperature data. MR_TROP & MR_SIGT
     */
    public List<NcSoundingLayer> mergeTropSigTemp(List<NcSoundingLayer> trop_a,
            List<NcSoundingLayer> trop_c, List<NcSoundingLayer> sndata) {
        if (trop_a.size() < 1 && trop_c.size() < 1) {
            return sndata;
        }

        /*
         * Append two lists of wind data.
         */
        if (trop_c.size() > 0) {
            for (int kk = 0; kk < trop_c.size(); kk++) {
                trop_a.add(trop_c.get(kk));
            }
        }

        for (int lev = 0; lev < trop_a.size(); lev++) {
            boolean found = false;
            float ppp = trop_a.get(lev).getPressure();
            for (int kk = 0; kk < sndata.size() && !found; kk++) {
                float pres = sndata.get(kk).getPressure();
                if (equal(ppp, pres)) {

                    // add data to missing
                    if (sndata.get(kk).getTemperature() == missingFloat) {
                        sndata.get(kk).setTemperature(
                                trop_a.get(lev).getTemperature());
                        sndata.get(kk)
                                .setDewpoint(trop_a.get(lev).getDewpoint());
                    }

                    if (sndata.get(kk).getWindDirection() == missingFloat) {
                        sndata.get(kk).setWindDirection(
                                trop_a.get(lev).getWindDirection());
                        sndata.get(kk)
                                .setWindSpeed(trop_a.get(lev).getWindSpeed());

                    }
                    found = true;
                }
            }

            /*
             * if not found, add to the list
             */
            if (!found) {
                float ttt = trop_a.get(lev).getTemperature();
                if (ppp != missingFloat && ttt != missingFloat) {
                    addDataToList(lev, trop_a, sndata);
                }
            }

        }

        /*
         * Sort the sounding data in descending order.
         */
        Collections.sort(sndata, new reverseSortByPressure());
        return sndata;
    }

    /*
     * Compute height at significant temperature levels (TTBB) using a moist
     * hydrostatic computation. (MR_SCMZ)
     */
    public void constructTtbbHeight(List<NcSoundingLayer> sndata) {
        boolean mand = false;
        boolean newblock = true;
        int blev = 0, tlev = 0;
        float[] scale = new float[200];
        float pb, zb, tb, tdb, zlev, plev;
        float pt, zt = 0.f, tt, tdt, znew = 0.f;

        if (sndata.size() <= 2)
            return;

        for (int nlev = 0; nlev < sndata.size(); nlev++) {
            if (newblock) {
                if (sndata.get(nlev).getGeoHeight() != missingFloat
                        && sndata.get(nlev).getPressure() != missingFloat
                        && sndata.get(nlev).getTemperature() != missingFloat) {

                    blev = nlev;
                    newblock = false;
                }
            } else {
                if (sndata.get(nlev).getGeoHeight() != missingFloat
                        && sndata.get(nlev).getTemperature() != missingFloat) {
                    tlev = nlev;
                    mand = true;
                }
            }

            /*
             * Compute scale height to this level
             */
            if (mand) {
                pb = sndata.get(blev).getPressure();
                zb = sndata.get(blev).getGeoHeight();
                tb = sndata.get(blev).getTemperature();
                tdb = sndata.get(blev).getDewpoint();
                zlev = sndata.get(blev).getGeoHeight();
                plev = sndata.get(blev).getPressure();

                for (int kk = blev + 1; kk <= tlev; kk++) {
                    pt = sndata.get(kk).getPressure();
                    zt = sndata.get(kk).getGeoHeight();
                    tt = sndata.get(kk).getTemperature();
                    tdt = sndata.get(kk).getDewpoint();
                    scale[kk] = scaleHeight(tb, tt, tdb, tdt, pb, pt);
                    znew = moistHeight(zb, pb, pt, scale[kk]);
                    if (znew != missingFloat) {
                        pb = pt;
                        tb = tt;
                        tdb = tdt;
                        zb = znew;
                    }
                }

                /*
                 * Compute the scaling factor so the computed moist height is
                 * consistent at the mandatory level. Then recompute the height.
                 */
                float s = (zt - zlev) / (znew - zlev);
                float zbb = zlev;
                float pbb = plev;
                for (int kk = blev + 1; kk < tlev; kk++) {
                    pt = sndata.get(kk).getPressure();
                    zt = sndata.get(kk).getGeoHeight();
                    scale[kk] = scale[kk] * s;
                    znew = moistHeight(zbb, pbb, pt, scale[kk]);
                    if (znew != missingFloat) {
                        pbb = pt;
                        zbb = znew;
                        sndata.get(kk).setGeoHeight(znew);
                    }
                }
                mand = false;
                newblock = true;

                if ((tlev + 1) != sndata.size()) {
                    if (sndata.get(tlev + 1).getGeoHeight() == missingFloat
                            && sndata.get(tlev + 1)
                                    .getPressure() != missingFloat
                            && sndata.get(tlev + 1)
                                    .getTemperature() != missingFloat) {
                        nlev--;
                    }
                }
            }
        }
        // Fill missing height at the top levels
        fillMissingHeightAtTop(sndata);

        return;
    }

    /*
     * Merge the significant and maximum wind data on pressure surfaces. MR_PWND
     */
    public void mergeSigMaxWindOnPressure(List<NcSoundingLayer> sig_wa,
            List<NcSoundingLayer> sig_wc, List<NcSoundingLayer> sndata) {

        if (sig_wa.size() < 1 && sig_wc.size() < 1) {
            return;
        }
        /*
         * Append two lists of wind data.
         */
        if (sig_wc.size() > 0 && sig_wc.get(0).getPressure() != missingFloat) {
            for (int kk = 0; kk < sig_wc.size(); kk++) {
                sig_wa.add(sig_wc.get(kk));
            }
        }

        /*
         * Merging
         */
        int nlevel = sndata.size();
        for (int kk = 0; kk < sig_wa.size(); kk++) {
            boolean found = false;
            for (int lev = 0; lev < nlevel; lev++) {
                if (equal(sndata.get(lev).getPressure(),
                        sig_wa.get(kk).getPressure())) {

                    // add data to missing
                    if (sndata.get(lev).getWindDirection() == missingFloat) {
                        sndata.get(lev).setWindDirection(
                                sig_wa.get(kk).getWindDirection());
                        sndata.get(lev)
                                .setWindSpeed(sig_wa.get(kk).getWindSpeed());
                    }
                    found = true;
                }
            }

            /*
             * if not found, add to the list.
             */
            if (!found) {
                if ((sig_wa.get(kk).getWindDirection() != missingFloat
                        && sig_wa.get(kk).getPressure() != missingFloat)) {

                    NcSoundingLayer sl = new NcSoundingLayer();
                    sl.setPressure(sig_wa.get(kk).getPressure());
                    sl.setTemperature(sig_wa.get(kk).getTemperature());
                    sl.setDewpoint(sig_wa.get(kk).getDewpoint());
                    sl.setWindDirection(sig_wa.get(kk).getWindDirection());
                    sl.setWindSpeed(sig_wa.get(kk).getWindSpeed());
                    sl.setGeoHeight(sig_wa.get(kk).getGeoHeight());
                    sl.setOmega(sig_wa.get(kk).getOmega());
                    sndata.add(sl);
                    nlevel++;
                }
            }

            Collections.sort(sndata, new reverseSortByPressure());
        }
        fillMissingHeightAtTop(sndata);
        return;
    }

    /*
     * Construct height at significant wind levels (PPBB) if reported on
     * pressure levels. Using moist hydrostatic computation for missing height
     * at top levels. MR_INTZ
     */
    public void constructPpbbHeight(List<NcSoundingLayer> sndata) {
        int tlev = 0;
        for (int kk = sndata.size() - 1; kk >= 0; kk--) {
            if (sndata.get(kk).getGeoHeight() != missingFloat) {
                tlev = kk;
                break;
            }
        }

        float pb = missingFloat, pt = missingFloat, zt = missingFloat,
                zb = missingFloat;
        int next;

        if (sndata.size() <= 2)
            return;

        for (int kk = 0; kk < tlev; kk++) {
            float pres = sndata.get(kk).getPressure();
            float hght = sndata.get(kk).getGeoHeight();
            if (pres == missingFloat) {
                // DO NOTHING
            } else if (hght != missingFloat) {
                pb = pres;
                zb = hght;
                pt = 2000.f;
            } else if (pb == missingFloat) {
                // DO NOTHING
            } else {

                /*
                 * Find next level with height and then interpolate the data.
                 */
                next = kk + 1;
                while (pres <= pt) {
                    if (sndata.get(next).getGeoHeight() != missingFloat) {
                        pt = sndata.get(next).getPressure();
                        zt = sndata.get(next).getGeoHeight();
                    } else {
                        next++;
                    }
                }
                float hhh = (float) (zb + (zt - zb)
                        * (Math.log(pres / pb) / Math.log(pt / pb)));
                sndata.get(kk).setGeoHeight(hhh);
            }
        }

        fillMissingHeightAtTop(sndata);
    }

    /*
     * Merge significant wind on height surfaces. The argument is sndata.
     */
    public void mergeSigWindOnHeight(List<NcSoundingLayer> sig_wa,
            List<NcSoundingLayer> sig_wc, List<NcSoundingLayer> sndata) {

        /*
         * The following code needs to be replaced by significant wind data from
         * database.
         */

        /*
         * Do nothing if wind report is not on height surfaces.
         */
        if (sig_wa.size() < 1 && sig_wc.size() < 1) {
            return;
        }

        /*
         * Add two lists of wind data.
         */
        if (sig_wc.size() > 0) {
            for (int kk = 0; kk < sig_wc.size(); kk++) {
                sig_wa.add(sig_wc.get(kk));
            }
        }
        int nlevel = sndata.size();
        for (int kk = 0; kk < sig_wa.size(); kk++) {
            boolean found = false;
            float zzz = sig_wa.get(kk).getGeoHeight();

            // Check surface level independently because sometimes station
            // report wind data twice at surface. We don't want the surface
            // pressure to be missing.
            if (zzz == 0) {
                if (sndata.get(0).getWindDirection() == missingFloat) {
                    sndata.get(0)
                            .setWindDirection(sig_wa.get(0).getWindDirection());
                    sndata.get(0).setWindSpeed(sig_wa.get(kk).getWindSpeed());
                }
                found = true;
            } else {
                for (int lev = 0; lev < nlevel; lev++) {
                    float hght = sndata.get(lev).getGeoHeight();
                    if (equal(zzz, hght) || (zzz == 0 && lev == 0 && kk == 0)) {
                        // add data to missing
                        if (sndata.get(lev)
                                .getWindDirection() == missingFloat) {
                            sndata.get(lev).setWindDirection(
                                    sig_wa.get(kk).getWindDirection());
                            sndata.get(lev).setWindSpeed(
                                    sig_wa.get(kk).getWindSpeed());
                        }
                        found = true;
                    }
                }
            }

            /*
             * if not found, add to the list.
             */
            if (!found) {
                if (sig_wa.get(kk).getWindDirection() != missingFloat
                        && sig_wa.get(kk).getGeoHeight() != missingFloat) {
                    NcSoundingLayer sl = new NcSoundingLayer();
                    sl.setPressure(sig_wa.get(kk).getPressure());
                    sl.setTemperature(sig_wa.get(kk).getTemperature());
                    sl.setDewpoint(sig_wa.get(kk).getDewpoint());
                    sl.setWindDirection(sig_wa.get(kk).getWindDirection());
                    sl.setWindSpeed(sig_wa.get(kk).getWindSpeed());
                    sl.setGeoHeight(sig_wa.get(kk).getGeoHeight());
                    sl.setOmega(sig_wa.get(kk).getOmega());
                    sndata.add(sl);
                }
            }
        }

        /*
         * Sorting the combined temperature and wind data based on Geopotential
         * height.
         */

        fillMissingHeightAtTop(sndata);

        Collections.sort(sndata, new sortByHeight());
        return;
    }

    // Sort by height
    public static class sortByHeight implements Comparator<NcSoundingLayer> {
        public int compare(NcSoundingLayer l1, NcSoundingLayer l2) {
            return Float.compare(l1.getGeoHeight(), l2.getGeoHeight());
        }

    }

    // Reverse sort by pressure
    public static class reverseSortByPressure
            implements Comparator<NcSoundingLayer> {
        public int compare(NcSoundingLayer l1, NcSoundingLayer l2) {
            return Float.compare(l2.getPressure(), l1.getPressure());
        }

    }

    /*
     * Construct pressure at significant wind levels (PPBB) that are reported on
     * height levels. MR_INTP
     */
    public List<NcSoundingLayer> constructPpbbPressure(
            List<NcSoundingLayer> sndata) {

        if (sndata.size() <= 2)
            return sndata;

        float pb = missingFloat, pt = missingFloat, zt = missingFloat,
                zb = missingFloat;
        int blev = missingInteger, tlev = missingInteger;
        for (int lev = 0; lev < sndata.size(); lev++) {
            float pres = sndata.get(lev).getPressure();
            float hght = sndata.get(lev).getGeoHeight();
            if (pres != missingFloat && hght != missingFloat) {
                tlev = lev;
                pt = pres;
                zt = hght;
            }

            if (blev != missingInteger && tlev != missingInteger) {
                for (int kk = blev + 1; kk < tlev; kk++) {
                    float z = sndata.get(kk).getGeoHeight();
                    if (sndata.get(kk).getGeoHeight() != missingFloat) {
                        float ppp = (float) (pb * Math.exp((double) ((z - zb)
                                * Math.log(pt / pb) / (zt - zb))));
                        sndata.get(kk).setPressure(ppp);
                    }
                }
            }
            blev = tlev;
            pb = pt;
            zb = zt;
        }

        if (tlev == (sndata.size() - 1) || tlev == missingInteger) {
            return sndata;
        } else {

            /*
             * Compute missing pressure at top levels.
             */
            pb = sndata.get(tlev - 1).getPressure();
            zb = sndata.get(tlev - 1).getGeoHeight();

            for (int kk = tlev + 1; kk < sndata.size(); kk++) {
                if (sndata.get(kk).getPressure() == missingFloat) {
                    pt = sndata.get(kk - 1).getPressure();
                    zt = sndata.get(kk - 1).getGeoHeight();
                    float zz = sndata.get(kk).getGeoHeight();
                    float rmult = (float) ((zz - zb) / (zt - zb));
                    sndata.get(kk).setPressure(
                            (float) (pb * (Math.pow(pt / pb, rmult))));
                    pb = pt;
                    zb = zt;
                }
            }
        }
        return sndata;
    }

    /*
     * Reorder the sounding data so the first level is always the surface data.
     */
    public List<NcSoundingLayer> reOrderSounding(List<NcSoundingLayer> sndata) {
        List<NcSoundingLayer> outdat = new ArrayList<>();
        float tt, td, dd, ff;
        int klev = 0;
        if (sndata.size() <= 1)
            return sndata;

        /*
         * Find the surface level
         */
        for (int kk = 0; kk < sndata.size(); kk++) {
            tt = sndata.get(kk).getTemperature();
            td = sndata.get(kk).getDewpoint();
            dd = sndata.get(kk).getWindDirection();
            ff = sndata.get(kk).getWindSpeed();
            if (tt == missingFloat && td == missingFloat && dd == missingFloat
                    && ff == missingFloat) {
                // DO NOTHING
            } else {
                klev = kk;
                addDataToList(0, sndata, outdat);
            }
        }

        /*
         * Reorder the data below the surface levels.
         */
        for (int kk = 0; kk < klev; kk++) {
            addDataToList(kk, sndata, outdat);
        }

        for (int kk = klev + 1; kk < sndata.size(); kk++) {
            addDataToList(kk, sndata, outdat);
        }
        return outdat;
    }

    /*
     * Construct missing temperature (iflag = 1), dewpoint temperature (iflag=2)
     * and wind (iflag = 3). This method is called after reOrderSounding().
     * MR_MISS
     */
    private List<NcSoundingLayer> constructMissing(MissFlag iflag,
            List<NcSoundingLayer> sndata) {
        float pb = missingFloat, pt = missingFloat, data = missingFloat, pres,
                tb, tt, tdb, tdt;
        int jlev = missingInteger, tlev = missingInteger;
        boolean contin = true;
        if (sndata.size() <= 2)
            return sndata;
        for (int blev = 1; blev < sndata.size() - 1 && contin; blev++) {
            jlev = blev;

            switch (iflag) {
            case MISSING_TEMP: {
                data = sndata.get(blev).getTemperature();
                break;
            }
            case MISSING_DEWPOINT: {
                data = sndata.get(blev).getDewpoint();
                break;
            }
            case MISSING_WIND: {
                data = sndata.get(blev).getWindDirection();
                break;
            }
            default: {
                return sndata;
            }
            }

            if (data == missingFloat) {

                /*
                 * find data at level above. Data should already be at level
                 * below after reOrderSounding() call.
                 */
                boolean found = false;
                while (!found) {
                    jlev++;
                    switch (iflag) {
                    case MISSING_TEMP: {
                        data = sndata.get(jlev).getTemperature();
                        break;
                    }
                    case MISSING_DEWPOINT: {
                        data = sndata.get(jlev).getDewpoint();
                        break;
                    }
                    case MISSING_WIND: {
                        data = sndata.get(jlev).getWindDirection();
                        break;
                    }
                    }
                    int top = sndata.size();
                    if (data != missingFloat || jlev + 1 >= top) {
                        found = true;
                        tlev = jlev;
                        if (jlev >= top) {
                            tlev = missingInteger;
                            contin = false;
                        }
                    }
                }

                /*
                 * Add check to eliminate dew point layer more than 100mb.
                 */
                if (iflag == MissFlag.MISSING_DEWPOINT
                        && tlev != missingInteger) {
                    if ((sndata.get(blev).getPressure()
                            - sndata.get(tlev).getPressure()) > 100.) {
                        for (int kk = tlev; kk < sndata.size(); kk++) {
                            sndata.get(kk).setDewpoint(missingFloat);
                        }
                        tlev = missingInteger;
                        contin = false;
                    }
                }

                /*
                 * Add check to eliminate interpolation of winds from below 100
                 * mb to above 100 mb. This eliminates interpolation to very
                 * high level winds.
                 */
                /*
                 * Interpolate with respect to logP.
                 */
                float rmult = missingFloat;
                if (tlev != missingInteger) {
                    pb = sndata.get(blev - 1).getPressure();
                    pres = sndata.get(blev).getPressure();
                    pt = sndata.get(tlev).getPressure();
                    if (pt != missingFloat && pb != missingFloat
                            && pres != missingFloat) {
                        rmult = (float) (Math.log(pres / pb)
                                / Math.log(pt / pb));
                    }

                    switch (iflag) {
                    case MISSING_TEMP: {
                        tb = sndata.get(blev - 1).getTemperature();
                        tt = sndata.get(tlev).getTemperature();

                        if (tb != missingFloat && tt != missingFloat
                                && rmult != missingFloat) {
                            data = tb + (tt - tb) * rmult;

                            sndata.get(blev).setTemperature(data);
                        }

                        tdb = sndata.get(blev - 1).getDewpoint();
                        tdt = sndata.get(tlev).getDewpoint();
                        if (tdb != missingFloat && tdt != missingFloat
                                && rmult != missingFloat) {
                            data = tdb + (tdt - tdb) * rmult;
                            sndata.get(blev).setDewpoint(data);
                        }
                        break;
                    }
                    case MISSING_DEWPOINT: {
                        tdb = sndata.get(blev - 1).getDewpoint();
                        tdt = sndata.get(tlev).getDewpoint();
                        if (tdb != missingFloat && tdt != missingFloat
                                && rmult != missingFloat) {
                            data = tdb + (tdt - tdb) * rmult;
                            sndata.get(blev).setDewpoint(data);
                        }
                        break;
                    }
                    case MISSING_WIND: {
                        float drctb = sndata.get(blev - 1).getWindDirection();
                        float drctt = sndata.get(tlev).getWindDirection();

                        if (drctt != missingFloat && drctb != missingFloat
                                && rmult != missingFloat) {
                            drctb = drctb % 360.f;
                            drctt = drctt % 360.f;
                            if (Math.abs(drctb - drctt) > 180.f) {
                                if (drctb < drctt) {
                                    drctb = drctb + 360.f;
                                } else {
                                    drctt = drctt + 360.f;
                                }
                            }
                            float drct = (drctb + (drctt - drctb) * rmult)
                                    % 360.f;
                            sndata.get(blev).setWindDirection(drct);

                            // Interpolate wind speed
                            float spedb = sndata.get(blev - 1).getWindSpeed();
                            float spedt = sndata.get(tlev).getWindSpeed();
                            float sped = spedb + (spedt - spedb) * rmult;
                            sndata.get(blev).setWindSpeed(sped);
                        }
                        break;

                    }
                    }
                }
            }
        }
        return sndata;
    }

    /*
     * Re-order the data so the first level is always the ground level. MR_COND
     */
    public List<NcSoundingLayer> addUnderGround(List<NcSoundingLayer> man,
            List<NcSoundingLayer> sndata) {
        if (sndata == null || sndata.size() < 2)
            return sndata;
        if (sndata.get(0).getPressure() == missingFloat || man.size() < 1)
            return sndata;

        int blev = 0;
        boolean contin = true;
        while (blev < sndata.size() && contin) {
            if (man.get(blev).getPressure() > sndata.get(0).getPressure()) {
                blev++;
                if (blev >= man.size())
                    contin = false;
            } else {
                contin = false;
            }
        }

        if (blev >= sndata.size()) {
            return sndata;
        }

        /*
         * Added below-ground mandatory levels to sounding layers.
         */
        List<NcSoundingLayer> outdat = new ArrayList<>();

        int nlev = sndata.size();

        // write to surface data first
        addDataToList(0, sndata, outdat);

        // add below-ground mandatory data
        if (blev > 0 && blev < sndata.size()) {
            for (int kk = 0; kk < blev; kk++) {
                addDataToList(kk, man, outdat);
            }
        }

        // add the rest of the data
        for (int kk = 1; kk < nlev; kk++) {
            addDataToList(kk, sndata, outdat);
        }
        return outdat;

    }

    /*
     * Re-order the data so the first level is always the ground level. MR_COND
     */
    public List<NcSoundingLayer> removeUnderGround(
            List<NcSoundingLayer> sndata) {
        List<NcSoundingLayer> outdat = new ArrayList<>();
        /*
         * Remove below-ground mandatory levels from sounding layers. Only the
         * first 8 missing levels can be mandatory levels.
         */
        if (sndata.size() <= 1)
            return sndata;
        for (int kk = 0; kk < sndata.size(); kk++) {
            if (sndata.get(kk).getTemperature() <= missingFloat
                    && sndata.get(kk).getDewpoint() <= missingFloat
                    && sndata.get(kk).getWindDirection() <= missingFloat
                    && sndata.get(kk).getWindSpeed() <= missingFloat) {
            } else if (sndata.get(kk).getPressure() <= missingFloat) {
            } else {
                addDataToList(kk, sndata, outdat);
            }
        }
        return outdat;
    }

    /*
     * Interpolate data to a single level, including surface.
     */
    public List<NcSoundingLayer> getSingLevel(float pres,
            List<NcSoundingLayer> sndata) {
        NcSoundingLayer sl = new NcSoundingLayer();
        List<NcSoundingLayer> sls = new ArrayList<>();
        sndata = removeUnderGround(sndata);
        if (sndata.size() <= 1)
            return missingSounding(); // Chin: check size again, after remove
                                      // underground level, size changed

        for (int kk = 1; kk < sndata.size() - 1; kk++) {
            if (pres > sndata.get(0).getPressure() || pres < 0.f) {
                return missingSounding();
            } else {

                if (pres >= sndata.get(kk).getPressure()) {
                    float pt = missingFloat, pb = missingFloat,
                            zt = missingFloat, zb = missingFloat,
                            tt = missingFloat, tb = missingFloat,
                            tdt = missingFloat, tdb = missingFloat,
                            dt = missingFloat, db = missingFloat,
                            st = missingFloat, sb = missingFloat;
                    pb = sndata.get(kk - 1).getPressure();
                    pt = sndata.get(kk).getPressure();
                    tb = sndata.get(kk - 1).getTemperature();
                    tt = sndata.get(kk).getTemperature();
                    tdb = sndata.get(kk - 1).getDewpoint();
                    tdt = sndata.get(kk).getDewpoint();
                    db = sndata.get(kk - 1).getWindDirection() % 360.f;
                    dt = sndata.get(kk).getWindDirection() % 360.f;
                    sb = sndata.get(kk - 1).getWindSpeed();
                    st = sndata.get(kk).getWindSpeed();
                    zb = sndata.get(kk - 1).getGeoHeight();
                    zt = sndata.get(kk).getGeoHeight();
                    sl.setPressure(pres);

                    float rmult = (float) (Math.log(pres / pb)
                            / Math.log(pt / pb));

                    float tempVal = missingFloat;
                    if (tb != missingFloat && tt != missingFloat) {
                        tempVal = tb + (tt - tb) * rmult;
                    }
                    sl.setTemperature(tempVal);

                    float dewpointVal = missingFloat;
                    if (tdb != missingFloat && tdt != missingFloat) {
                        dewpointVal = tdb + (tdt - tdb) * rmult;
                    }
                    sl.setDewpoint(dewpointVal);

                    float windDirVal = missingFloat;
                    if (tdb != missingFloat && tdt != missingFloat) {
                        if (Math.abs(db - dt) > 180.) {
                            if (db < dt) {
                                db = db + 360.f;
                            } else {
                                dt = dt + 360.f;
                            }
                        }
                        windDirVal = db + (dt - db) * rmult;
                    }
                    sl.setWindDirection(windDirVal);

                    float windSpdVal = missingFloat;
                    if (sb != missingFloat && st != missingFloat) {
                        windSpdVal = sb + (st - sb) * rmult;
                    }
                    sl.setWindSpeed(windSpdVal);

                    float geoHtdVal = missingFloat;
                    if (zb != missingFloat && zt != missingFloat) {
                        geoHtdVal = zb + (zt - zb) * rmult;
                    }
                    sl.setGeoHeight(geoHtdVal);

                    sls.add(sl);
                    return sls;
                }
            }
        }
        return missingSounding();
    }

    /*
     * Add data to output sounding profile.
     */
    public void addDataToList(int index, List<NcSoundingLayer> indat,
            List<NcSoundingLayer> outdat) {
        NcSoundingLayer sl = new NcSoundingLayer();
        sl.setPressure(indat.get(index).getPressure());
        sl.setTemperature(indat.get(index).getTemperature());
        sl.setDewpoint(indat.get(index).getDewpoint());
        sl.setWindDirection(indat.get(index).getWindDirection());
        sl.setWindSpeed(indat.get(index).getWindSpeed());
        sl.setGeoHeight(indat.get(index).getGeoHeight());
        sl.setOmega(indat.get(index).getOmega());
        outdat.add(sl);
    }

    /*
     * Set missing to output sounding profile.
     */
    public List<NcSoundingLayer> missingSounding() {
        List<NcSoundingLayer> outdat = new ArrayList<>();
        NcSoundingLayer sl = new NcSoundingLayer();
        sl.setPressure(missingFloat);
        sl.setTemperature(missingFloat);
        sl.setDewpoint(missingFloat);
        sl.setWindDirection(missingFloat);
        sl.setWindSpeed(missingFloat);
        sl.setGeoHeight(missingFloat);
        sl.setOmega(missingFloat);
        outdat.add(sl);
        return outdat;
    }

    /*
     * Print the sounding data out.
     */
    public void printOut(List<NcSoundingLayer> sndata) {
        for (NcSoundingLayer soundLy : sndata) {
            System.out.print(" PRES: " + soundLy.getPressure() + " HGHT: "
                    + soundLy.getGeoHeight() + " TMPC: "
                    + soundLy.getTemperature() + " DWPC: "
                    + soundLy.getDewpoint() + " DRCT: "
                    + soundLy.getWindDirection() + " SPED: "
                    + soundLy.getWindSpeed() + " OMEG " + soundLy.getOmega()
                    + "\n");
        }
    }

    /*
     * Extrapolate missing height at the top level before sorting the data by
     * height.
     */
    private void fillMissingHeightAtTop(List<NcSoundingLayer> sndata) {
        int blev, kk;
        float pb, zb, tb, tdb;
        float pt, tt, tdt;
        float znew;
        boolean contin;

        int miss = 0;
        contin = true;
        if (sndata.get(sndata.size() - 1).getGeoHeight() != missingFloat)
            return;
        for (kk = sndata.size() - 1; kk > 0 && contin; kk--) {
            if (sndata.get(kk).getGeoHeight() == missingFloat) {
                miss++;
                if (kk == 0)
                    return;

                if (sndata.get(kk - 1).getGeoHeight() != missingFloat)
                    contin = false;
            }
        }

        miss++;
        if (miss > 0) {
            blev = sndata.size() - miss;
            pb = sndata.get(blev).getPressure();
            zb = sndata.get(blev).getGeoHeight();
            tb = sndata.get(blev).getTemperature();
            tdb = sndata.get(blev).getDewpoint();

            for (kk = blev + 1; kk < sndata.size(); kk++) {
                pt = sndata.get(kk).getPressure();
                tt = sndata.get(kk).getTemperature();
                tdt = sndata.get(kk).getDewpoint();
                float xxx = scaleHeight(tb, tt, tdb, tdt, pb, pt);
                znew = moistHeight(zb, pb, pt, xxx);
                if (znew == missingFloat) {
                    xxx = scaleHeight(tb, tb, tdb, tdb, pb, pt);
                    znew = moistHeight(zb, pb, pt, xxx);
                }

                if (znew != missingFloat) {
                    sndata.get(kk).setGeoHeight(znew);
                    pb = pt;
                    tb = tt;
                    tdb = tdt;
                    zb = znew;
                }
            }
        }
    }

    /*
     * Remove missing pressure from the sounding profile.
     */
    public List<NcSoundingLayer> removeMissingPressure(
            List<NcSoundingLayer> sndin) {
        List<NcSoundingLayer> sndout = new ArrayList<>();

        for (int kk = 0; kk < sndin.size(); kk++) {
            if (sndin.get(kk).getPressure() > 0.) {
                addDataToList(kk, sndin, sndout);
            }
        }
        return sndout;
    }

    /**
     * Convert RH, SH, or DpD to dew point for grid model sounding data. For
     * better performance, the conversion order is RH first, if RH not available
     * then SH, and then DpD. This design choice is based on the observation
     * from database that RH is most common provided by most grid models. Only
     * few models use SH, e.g NAM at x25 and x75 levels, and DpD, e.g RUC130 at
     * surface level.
     * 
     * @param List
     *            <NcSoundingLayer> : list of sounding data
     * @return List<NcSoundingLayer> :list of dew point converted sounding data
     * @author cchen, created 05/01/2015
     */

    public List<NcSoundingLayer> convertDewpoint(List<NcSoundingLayer> sndata) {
        float rh, vapr, vaps, temp;
        float dwpc = missingFloat;
        boolean rhFound = false;
        for (NcSoundingLayer layer : sndata) {
            if (layer.getDewpoint() == missingFloat) {
                temp = layer.getTemperature();
                rh = layer.getRelativeHumidity();
                if (rh == missingFloat || temp == missingFloat) {
                    // RH or temp not available, can not convert RH, try SH
                    float spfh, pres;
                    spfh = layer.getSpecHumidity();
                    pres = layer.getPressure();
                    if (spfh == missingFloat || pres == missingFloat
                            || temp == missingFloat || spfh <= 0.f
                            || pres <= 0.f) {
                        // SH or pressure not available, try DpD
                        float dpdk;
                        dpdk = layer.getDpd();
                        if (temp == missingFloat || dpdk == missingFloat) {
                            // DpD or temp not available, no hope for this
                            // level.
                            continue;
                        } else {
                            // DpD and temp are available, perform dew point
                            // conversion for this level
                            dwpc = temp - dpdk;
                            layer.setDewpoint(dwpc);
                        }
                    } else {
                        // SH, temp and pressure available, perform dew point
                        // conversion for this level
                        // Chin note: since spfhToDewpoint() does not work
                        // properly,
                        // convert spfh to rh first then convert to dew point.
                        // Ues this formular,
                        // RH=100wws ≈ 0.263*p*spfh*
                        // /exp(17.67*T/(T0-273.16−29.65)).
                        // where p=pressure(pa), T=temp(C), T0=reference
                        // temp(273.16)
                        // found
                        // @http://earthscience.stackexchange.com/questions/2360/how-do-i-convert-specific-humidity-to-relative-humidity

                        rh = (float) (0.263 * pres * spfh / Math
                                .exp(17.67 * temp / (temp + 273.16 - 29.65)));
                        rhFound = true;
                    }
                } else {
                    rhFound = true;
                }
                if (rhFound) {
                    // RH and Temp are available, perform dew point conversion
                    // for this level
                    rhFound = false;
                    vaps = tempToVapr(temp);
                    vapr = rh * vaps / 100;
                    if (vapr < Math.exp(-30))
                        continue;
                    else {
                        dwpc = (float) (243.5
                                * (Math.log(6.112) - Math.log(vapr))
                                / (Math.log(vapr) - Math.log(6.112) - 17.67));
                        layer.setDewpoint(dwpc);
                    }
                }
            }
        }
        return sndata;
    }
}
