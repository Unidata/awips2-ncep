package gov.noaa.nws.ncep.edex.plugin.soundingrequest.handler;

import gov.noaa.nws.ncep.edex.common.metparameters.AirTemperature;
import gov.noaa.nws.ncep.edex.common.metparameters.Amount;
import gov.noaa.nws.ncep.edex.common.metparameters.DewPointTemp;
import gov.noaa.nws.ncep.edex.common.metparameters.HeightAboveSeaLevel;
import gov.noaa.nws.ncep.edex.common.metparameters.Omega;
import gov.noaa.nws.ncep.edex.common.metparameters.PressureLevel;
import gov.noaa.nws.ncep.edex.common.metparameters.WindDirection;
import gov.noaa.nws.ncep.edex.common.metparameters.WindSpeed;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.NcUnits;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer2;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingProfile;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

public class QueryMiscTools {

	 /*
     * Convert sounding data saved in NcSoundingLayer list to NcSoundingLayer2
     * list remove NcSoundingLayer data to have a smaller size for sending back
     * to client
     */
	public static void convertNcSoundingLayerToNcSoundingLayer2(
            List<NcSoundingProfile> pfLst) 
	{
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
	public static String[]  convertTimeLongArrayToStrArray(long[] timeArr) {
		String[]  timeStringArray = new String[timeArr.length];
        for (int i = 0; i < timeArr.length; i++) {
            Calendar timeCal = Calendar
                    .getInstance(TimeZone.getTimeZone("GMT"));
            // reset time
            timeCal.setTimeInMillis(timeArr[i]);
            String tStr = String.format("%1$tY-%1$tm-%1$td %1$tH:00:00",
                    timeCal);
            timeStringArray[i]= tStr;
            //System.out.println("convertTimeLongArrayToStrArray: time string "+ tStr);
        }
        return timeStringArray;
    }

	public static long[] convertTimeStrArrayToLongArray(String[]  intimeStrArray) {
        int year, mon, date, hr;
        int index;
        long[] timeL = new long[intimeStrArray.length];
        for(int i=0; i<intimeStrArray.length; i++ ){
        	String timeStr = intimeStrArray[i];
        	index = timeStr.indexOf('-');
        	if (index >= 4) {
        		year = Integer.parseInt(timeStr.substring(index - 4, index));
        		timeStr = timeStr.substring(index + 1);
        		index = timeStr.indexOf('-');
        		if (index >= 2) {
        			mon = Integer.parseInt(timeStr.substring(index - 2, index));
        			timeStr = timeStr.substring(index + 1);
        			index = timeStr.indexOf(' ');
        			if (index >= 2) {
        				date = Integer
        						.parseInt(timeStr.substring(index - 2, index));
        				timeStr = timeStr.substring(index + 1);
        				// index = refTimeStr.indexOf(':');
        				if (timeStr.length() >= 2) {
        					hr = Integer.parseInt(timeStr.substring(0, 2));
        					Calendar cal;
        					cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        					// reset time
        					cal.setTimeInMillis(0);
        					// set new time
        					cal.set(year, mon - 1, date, hr, 0, 0);
        					timeL[i] = cal.getTimeInMillis();

        				}
        			}
        		}
        	}
        }
        return timeL;
	}


}
