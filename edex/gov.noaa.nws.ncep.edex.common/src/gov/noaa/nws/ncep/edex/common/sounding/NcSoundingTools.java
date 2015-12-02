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
 * 07/20/2015   RM#9173     Chin Chen   Clean up NcSoundingQuery, and Obsolete NcSoundingQuery2 and MergeSounding2
 * 
 * 
 * </pre>
 * 
 * @author Chin Chen
 * @version 1.0
 */
public class NcSoundingTools {

    /*************************************************************
     * PRECIP_WATER Calculates the Precipitation Water in mm from Bottom level
     * of layer (mb) to Top level of layer (mb)
     *************************************************************/
    public static float precip_water(List<NcSoundingLayer> sndlayers) {
        float pw = 0;
        float d1, p1, d2, p2, tot, w1, w2, wbar;

        // ----- Start with interpolated bottom layer -----
        // find surface layer or first layer with valid dewpoint
        int sfcIndex = 0;
        for (int i = 0; i < sndlayers.size(); i++) {
            if (sndlayers.get(i).getDewpoint() != -9999f) {
                sfcIndex = i;
                break;
            }
        }
        d1 = sndlayers.get(sfcIndex).getDewpoint(); // dewp
                                                    // in
                                                    // C
        p1 = sndlayers.get(sfcIndex).getPressure(); // pressure
                                                    // n
                                                    // mb

        tot = 0;
        for (int i = sfcIndex + 1; i < sndlayers.size(); i++) {
            /* ----- Calculate every level that reports a dwpt ----- */
            d2 = sndlayers.get(i).getDewpoint(); // dewp
                                                 // in C
            if (d2 == -9999f)
                continue;
            p2 = sndlayers.get(i).getPressure(); // pressure
                                                 // n mb
            w1 = mixingRatio(d1, p1);
            w2 = mixingRatio(d2, p2);
            wbar = (w1 + w2) / 2;
            tot = tot + wbar * (p1 - p2);
            // System.out.println("p1=" + p1 + " d1=" + d1 + " p2=" + p2 +
            // " d2="
            // + d2);
            d1 = d2;
            p1 = p2;
            // test the case when top level is 400 mb
            // if (p2 == 400)
            // break;
        }

        /* ----- Convert to mm (from g*mb/kg) ----- */
        pw = tot * 0.00040173f * 25.4f;

        return pw;
    }

//    public static float precip_water2(List<NcSoundingLayer2> sndlayers) {
//        float pw = 0;
//        float d1, p1, d2, p2, tot, w1, w2, wbar;
//        if (sndlayers == null || sndlayers.size() <= 0)
//            return 0;
//        // ----- Start with interpolated bottom layer -----
//        // find surface layer or first layer with valid dewpoint
//        int sfcIndex = 0;
//        for (int i = 0; i < sndlayers.size(); i++) {
//            if (sndlayers.get(i).getDewpoint().getValue().floatValue() != -9999f) {
//                sfcIndex = i;
//                break;
//            }
//        }
//        d1 = sndlayers.get(sfcIndex).getDewpoint().getValue().floatValue(); // dewp
//                                                                            // in
//                                                                            // C
//        p1 = sndlayers.get(sfcIndex).getPressure().getValue().floatValue(); // pressure
//                                                                            // n
//                                                                            // mb
//
//        tot = 0;
//        for (int i = sfcIndex + 1; i < sndlayers.size(); i++) {
//            /* ----- Calculate every level that reports a dwpt ----- */
//            d2 = sndlayers.get(i).getDewpoint().getValue().floatValue(); // dewp
//                                                                         // in C
//            if (d2 == -9999f)
//                continue;
//            p2 = sndlayers.get(i).getPressure().getValue().floatValue(); // pressure
//                                                                         // n mb
//            w1 = mixingRatio(d1, p1);
//            w2 = mixingRatio(d2, p2);
//            wbar = (w1 + w2) / 2;
//            tot = tot + wbar * (p1 - p2);
//            d1 = d2;
//            p1 = p2;
//            // test the case when top level is 400 mb
//            // if (p2 == 400)
//            // break;
//        }
//
//        /* ----- Convert to mm (from g*mb/kg) ----- */
//        pw = tot * 0.00040173f * 25.4f;
//
//        return pw;
//    }

    /*
     * Compute mixing ratio from DWPC and PRES. Chin: copy from
     * gov.noaa.nws.ncep.edex.uengine.tasks.profile.MergeSounding
     */
    private static float mixingRatio(float td, float pres) {
        float vapr = vaporPressure(td);

        float corr = (1.001f + ((pres - 100.f) / 900.f) * .0034f);

        float e = corr * vapr;
        if (e > (.5f * pres)) {
            return -9999f;
        } else {
            return .62197f * (e / (pres - e)) * 1000.f;
        }
    }

    /*
     * Compute vapor pressure from DWPC. Chin: copy from
     * gov.noaa.nws.ncep.edex.uengine.tasks.profile.MergeSounding
     */
    private static float vaporPressure(float td) {
        return (6.112f * (float) Math.exp((17.67 * td) / (td + 243.5)));
    }

    // The followings are converted from BigSharp, the computation results are
    // about the same as above methods.
    // private static float mixratio(float pres, float temp)
    //
    // {
    // float x, wfw, fwesw;
    //
    // x = 0.02f * (temp - 12.5f + 7500.0f / pres);
    // wfw = 1.0f + 0.0000045f * pres + 0.0014f * x * x;
    // fwesw = wfw * vappres(temp);
    // return 621.97f * (fwesw / (pres - fwesw));
    // }
    //
    // private static float vappres(float temp)
    //
    // {
    // double pol;
    // pol = temp * (1.1112018e-17 + temp * (-3.0994571e-20));
    // pol = temp * (2.1874425e-13 + temp * (-1.789232e-15 + pol));
    // pol = temp * (4.3884180e-09 + temp * (-2.988388e-11 + pol));
    // pol = temp * (7.8736169e-05 + temp * (-6.111796e-07 + pol));
    // pol = .99999683e-00 + temp * (-9.082695e-03 + pol);
    // pol = (pol * pol);
    // pol = (pol * pol);
    // return (6.1078f / (float) (pol * pol));
    // }

	 /*
     * Convert sounding data saved in NcSoundingLayer list to NcSoundingLayer2
     * list remove NcSoundingLayer data to have a smaller size for sending back
     * to client
     */
	public static void convertNcSoundingLayerToNcSoundingLayer2(
            List<NcSoundingProfile> pfLst) 
	{
		if(pfLst==null)
			return;
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
                    // soundingLy.setPressure(level.getPressure().floatValue()/100);
                    // soundingLy.setWindU(level.getUcWind().floatValue()); //
                    // HDF5 data in unit of Knots, no conversion needed
                    // soundingLy.setWindV(level.getVcWind().floatValue());
                    // soundingLy.setSpecHumidity(level.getSpecificHumidity().floatValue());
                    soundLy2List.add(soundingLy2);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
            // Collections.sort(soundLyList,reversePressureComparator());
            pf.setSoundingLyLst2(soundLy2List);
            pf.getSoundingLyLst().clear();
        }
    }

}
