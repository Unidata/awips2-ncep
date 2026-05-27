/**
 * 
 * Util
 * 
 * This java class contains edex generic utility methods for use.  It can
 * be seen by any other NCEP package, so is a good place to put generic
 * utility methods
 *  
 * T. Lee	11/2008	Creation
 * T. Lee	 3/2009	Fixed roll-over cases; added String functions
 * T. Lee	 4/2009	Added date to the base time
 * T. Lee
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    	Engineer    Description
 * ------------ ---------- 	----------- --------------------------
 * 11/2008		14			T. Lee		Creation
 *  3/2009		14			T. Lee		Fixed roll-over cases; added String functions
 *  4/2009		14			T. Lee		Added date to the base time
 *  5/2009		128			T. Lee		Used UTC in findDataTime
 * 12/12/2016   R27046      S. Russell  Added findFraction() and 
 *                                      findFractionForANumberGreaterThanOne()
 * Sep 06, 2024 2037799     njensen     Split UtilN into two classes and extend the other one
 *                                      
 * </pre>
 * 
 * @author T.Lee
 */

package gov.noaa.nws.ncep.edex.util;

import java.util.Calendar;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.raytheon.edex.util.Util;

public class UtilN extends gov.noaa.nws.ncep.edex.common.tools.decoder.UtilN {
    private static final Log logger = LogFactory.getLog(UtilN.class);

    /**
     * Constructor
     */
    public UtilN() {

    }

    /**
     * Convert a string in ddhhmm format to a standard {@link Calendar} format
     * where ddhhmm is the GMT format while the standard time is in Calendar
     * format with Year and Month information. Usage: ddhhmm is the issue time
     * whereas utcTime can be the MDN time. The former comes "after" the latter.
     * 
     * @parm ddhhmm day-hour-minute in GMT
     * @parm local Time UTC time in Calendar
     */
    public static Calendar findDataTime(String ddhhmm, Calendar utcTime) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        ;
        if (utcTime == null) {
            try {
                return Util.findCurrentTime(ddhhmm);
            } catch (Exception e) {
                if (logger.isInfoEnabled()) {
                    logger.info(
                            " Error in processing MND time; return current time ", e);
                }
                return cal;
            }

        } else {
            int iDay = Integer.parseInt(ddhhmm.substring(0, 2).trim());
            int iHour = Integer.parseInt(ddhhmm.substring(2, 4).trim());
            int iMinute = Integer.parseInt(ddhhmm.substring(4, 6).trim());
            int iMonth = utcTime.get(Calendar.MONTH);
            int iYear = utcTime.get(Calendar.YEAR);

            /*
             * adjust the month and year for roll-over situations
             */
            if (iDay < utcTime.get(Calendar.DAY_OF_MONTH)) {
                iMonth++;
                if (iMonth == 12) {
                    iMonth = Calendar.JANUARY;
                    iYear++;
                }
            }
            cal.set(iYear, iMonth, iDay, iHour, iMinute);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal;
        }
    }

}
