/**
 * 
 * This java class performs the observed sounding data query functions. This
 * code has been developed by the SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket# Engineer   Description
 * 07/21/2015   RM#9173     Chin Chen   Clean up NcSoundingQuery and Obsolete NcSoundingQuery2
 * 
 * @author Chin Chen
 * @version 1.0
 */package gov.noaa.nws.ncep.edex.plugin.soundingrequest.handler;

import java.util.Calendar;
import java.util.TimeZone;

public class QueryMiscTools {

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
