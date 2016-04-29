package gov.noaa.nws.ncep.edex.common.sounding;

import gov.noaa.nws.ncep.edex.common.metparameters.AirTemperature;
import gov.noaa.nws.ncep.edex.common.metparameters.Amount;
import gov.noaa.nws.ncep.edex.common.metparameters.DewPointTemp;
import gov.noaa.nws.ncep.edex.common.metparameters.HeightAboveSeaLevel;
import gov.noaa.nws.ncep.edex.common.metparameters.Omega;
import gov.noaa.nws.ncep.edex.common.metparameters.PressureLevel;
import gov.noaa.nws.ncep.edex.common.metparameters.WindDirection;
import gov.noaa.nws.ncep.edex.common.metparameters.WindSpeed;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.NcUnits;

import java.util.ArrayList;
import java.util.List;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

/**
 * 
 * gov.noaa.nws.ncep.edex.common.sounding.NcSoundingTools
 * 
 * This code has been developed by the SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#     Engineer    Description
 * -------      -------     --------    -----------
 * 07/24/2014               Chin Chen   Initial coding 
 *                                      Support PW computation
 * 07/20/2015   R9173     Chin Chen   Clean up NcSoundingQuery, and Obsolete NcSoundingQuery2 and MergeSounding2
 * 10/20/2015   R12599    Chin Chen   negative PWAT value when surface layer dew point is missing
 * 
 * 
 * </pre>
 * 
 * @author Chin Chen
 * @version 1.0
 */
public class NcSoundingTools {
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(NcSoundingTools.class);

    /**
     * 
     * PRECIP_WATER Calculates the precipitable water in mm from surface level
     * of layer (mb) to highest level of layer (mb). The algorithm to compute
     * precipitable water is based on this C function
     * "float precip_water(float *param, float lower, float upper)" at
     * skparams.c of BigSharpp library source code
     * 
     * @param sndlayers
     *            - a list of NcSoundingLayer for pw computation
     * @return - precipitable water in mm, -1 indicating pw is not computable
     */
    public static float precip_water(List<NcSoundingLayer> sndlayers) {
        return (precip_water(sndlayers, -1, -1));
    }

    /**
     * 
     * PRECIP_WATER Calculates the precipitable water in mm from caller
     * specified bottom layer (in mb) to top layer (in mb). The algorithm to
     * compute precipitable water is based on this C function "float
     * precip_water(float *param, float lower, float upper)" at skparams.c of
     * BigSharpp library source code
     * 
     * @param sndlayers
     *            - a list of NcSoundingLayer for pw computation
     * @param top
     *            - the pressure of top level, -1 indicating the highest level
     * @param bot
     *            - the pressure of bottom level, -1 indicating the surface
     *            level
     * @return - precipitable water in mm, -1 indicating pw is not computable
     */
    public static float precip_water(List<NcSoundingLayer> sndlayers,
            float top, float bot) {
        float pw = 0;
        float d1, p1, d2, p2, tot, w1, w2, wbar;
        int botIndex = -1;
        int topIndex = -1;
        // find bottom layer index
        if (bot == -1) {
            // make sure surface layer dew point is NOT missing
            if (sndlayers.get(0).getDewpoint() == -9999f) {
                return -1;
            } else {
                botIndex = 0;
            }
        } else {
            // Note that lowest level has the greatest pressure
            // search from lowest level up and find bottom layer with valid
            // dew point and pressure equal or lower than the input "bot"
            for (int i = 0; i < sndlayers.size(); i++) {
                if ((sndlayers.get(i).getDewpoint() != NcSoundingLayer.MISSING)
                        && (sndlayers.get(i).getPressure() <= bot)) {
                    botIndex = i;
                    break;
                }
            }

        }

        if (botIndex == -1) {
            return -1f;
        }

        // find top layer index
        if (top == -1) {
            {
                topIndex = sndlayers.size() - 1;
            }
        } else {
            // Note that highest level has the smallest pressure
            // search from highest level down and find this first top layer with
            // valid dew point and pressure equal or greater than the input
            // "top"
            for (int i = sndlayers.size() - 1; i > 0; i--) {
                if ((sndlayers.get(i).getDewpoint() != NcSoundingLayer.MISSING)
                        && (sndlayers.get(i).getPressure() >= top)) {
                    topIndex = i;
                    break;
                }
            }
        }
        if ((topIndex == -1) || (topIndex <= botIndex)) {
            return -1f;
        }

        d1 = sndlayers.get(botIndex).getDewpoint(); // dewpoint in C
        p1 = sndlayers.get(botIndex).getPressure(); // pressure in mb

        tot = 0;
        for (int i = botIndex + 1; i <= topIndex; i++) {
            // Calculate every level that reports a dwpt
            d2 = sndlayers.get(i).getDewpoint();
            if (d2 == -9999f) {
                continue;
            }
            p2 = sndlayers.get(i).getPressure();
            w1 = mixingRatio(d1, p1);
            w2 = mixingRatio(d2, p2);
            wbar = (w1 + w2) / 2;
            tot = tot + wbar * (p1 - p2);
            d1 = d2;
            p1 = p2;
        }

        // Convert pw to mm from g*mb/kg
        pw = tot * 0.00040173f * 25.4f;

        return pw;
    }

    /*
     * Compute mixing ratio from DWPC and PRES. Chin: copy from
     * gov.noaa.nws.ncep.edex.uengine.tasks.profile.MergeSounding
     */
    private static float mixingRatio(float td, float pres) {
        float vapr = vaporPressure(td);

        float corr = (1.001f + ((pres - 100.f) / 900.f) * .0034f);

        float e = corr * vapr;
        if (e > (.5f * pres)) {
            {
                return -9999f;
            }
        } else {
            {
                return .62197f * (e / (pres - e)) * 1000.f;
            }
        }
    }

    /*
     * Compute vapor pressure from DWPC. Chin: copy from
     * gov.noaa.nws.ncep.edex.uengine.tasks.profile.MergeSounding
     */
    private static float vaporPressure(float td) {
        return (6.112f * (float) Math.exp((17.67 * td) / (td + 243.5)));
    }

    /*
     * Convert sounding data saved in NcSoundingLayer list to NcSoundingLayer2
     * list remove NcSoundingLayer data to have a smaller size for sending back
     * to client
     */
    public static void convertNcSoundingLayerToNcSoundingLayer2(
            List<NcSoundingProfile> pfLst) {
        if (pfLst == null) {
            return;
        }
        for (NcSoundingProfile pf : pfLst) {
            List<NcSoundingLayer2> soundLy2List = new ArrayList<NcSoundingLayer2>();
            for (NcSoundingLayer level : pf.getSoundingLyLst()) {
                NcSoundingLayer2 soundingLy2;
                try {
                    soundingLy2 = new NcSoundingLayer2();
                    AirTemperature airTemp;
                    airTemp = new AirTemperature();
                    airTemp.setValue(new Amount(level.getTemperature(),
                            SI.CELSIUS));
                    soundingLy2.setTemperature(airTemp);

                    DewPointTemp dewPoint = new DewPointTemp();
                    dewPoint.setValue(new Amount(level.getDewpoint(),
                            SI.CELSIUS));
                    soundingLy2.setDewpoint(dewPoint);

                    PressureLevel pressure = new PressureLevel();
                    pressure.setValue(new Amount(level.getPressure(),
                            NcUnits.MILLIBAR));
                    soundingLy2.setPressure(pressure);

                    WindDirection windDirection = new WindDirection();
                    windDirection.setValue(level.getWindDirection(),
                            NonSI.DEGREE_ANGLE);
                    soundingLy2.setWindDirection(windDirection);

                    WindSpeed windSpeed = new WindSpeed();
                    // HDF5 data in unit of Knots, no conversion needed
                    windSpeed.setValue(level.getWindSpeed(), NonSI.KNOT);
                    soundingLy2.setWindSpeed(windSpeed);

                    HeightAboveSeaLevel height = new HeightAboveSeaLevel();
                    height.setValue(level.getGeoHeight(), SI.METER);
                    soundingLy2.setGeoHeight(height);

                    Omega omega = new Omega();
                    omega.setValueAs(level.getOmega(), "");
                    soundingLy2.setOmega(omega);
                    soundLy2List.add(soundingLy2);
                } catch (Exception e) {
                    statusHandler
                            .handle(UFStatus.Priority.WARN,
                                    "An Error occured while convertNcSoundingLayerToNcSoundingLayer2",
                                    e);
                }
            }
            pf.setSoundingLyLst2(soundLy2List);
            pf.getSoundingLyLst().clear();
        }
    }
}
