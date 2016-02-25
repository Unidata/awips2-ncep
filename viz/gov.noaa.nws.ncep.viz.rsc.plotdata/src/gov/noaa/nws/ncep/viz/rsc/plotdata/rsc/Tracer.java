package gov.noaa.nws.ncep.viz.rsc.plotdata.rsc;

/**
 * Tracer
 * 
 * Utility class for logging selected code tracing and timing information,
 * currently used only withing the point data display (plotdata) resource.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 12/16/2013              B. Hebbard   Initial creation
 * 07/07/2014              B. Hebbard   Assorted updates
 * 09/01/2015    R7757     B. Hebbard   Assorted updates and cleanup
 * 11/17/2015    R9579     B. Hebbard   Show source line number in trace
 *
 * </pre>
 *
 * @author bhebbard
 * @version 1.0	
 */

import gov.noaa.nws.ncep.viz.common.ui.NmapCommon;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;

import com.raytheon.uf.common.time.DataTime;

public class Tracer {

    // ACTIVATE HERE ------------------v
    private static boolean enabled = false;

    // save it static to have it available on every call

    private static Method m;

    private static long startTime = -1;

    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

    Calendar cal = Calendar.getInstance();

    String logFileName = "" // optional custom directory
            + dateFormat.format(cal.getTime());

    private static PrintWriter writer = null;

    static {
        try {
            m = Throwable.class.getDeclaredMethod("getStackTraceElement",
                    int.class);
            m.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getCallerLocation(final int depth) {
        try {
            StackTraceElement element = (StackTraceElement) m.invoke(
                    new Throwable(), depth + 1);
            // Yes, following could be one line...
            // Just did line-by-line so can add/remove parts easily...
            String returnString = "";
            returnString = element.getClassName();
            returnString = returnString.replace(
                    "gov.noaa.nws.ncep.viz.rsc.plotdata.", "...");
            returnString += "." + element.getMethodName();
            returnString += ":" + element.getLineNumber();
            return returnString;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void print(String message) {
        if (enabled) {
            long elapsedTime = 0;
            if (startTime < 0) {
                DateFormat dateFormat = new SimpleDateFormat(
                        "yyyy-MM-dd-HH-mm-ss");
                Calendar cal = Calendar.getInstance();
                String logFileName = "/export/cdbsrv/bhebbard/pointdatadisplay/"
                        + dateFormat.format(cal.getTime());
                try {
                    writer = new PrintWriter(logFileName, "UTF-8");
                } catch (FileNotFoundException e) {
                    System.out.println("[" + "FileNotFoundException" + "] "
                            + message);
                } catch (UnsupportedEncodingException e) {
                    System.out.println("[" + "UnsupportedEncodingException"
                            + "] " + message);
                }
                System.out.println("[Logging to " + logFileName + "]");
                startTime = System.nanoTime();
            } else {
                elapsedTime = (System.nanoTime() - startTime) / 1000000;
            }
            System.out.println("[" + elapsedTime + getCallerLocation(1) + "] "
                    + message);
            writer.println("[" + elapsedTime + getCallerLocation(1) + "] "
                    + message);
        }
    }

    public static void printX(String message) {
        // Do nothing. This is to allow us to turn specific logging points on
        // and off by changing "print" to "printX", instead of having
        // commented-out code.
    }

    public static String shortTimeString(DataTime dt) {
        String returnString = NmapCommon.getTimeStringFromDataTime(dt, "/")
                .substring(4);
        if (dt.getFcstTime() != 0) {
            returnString += "(" + dt.getFcstTime() + ")";
        }
        return returnString;
    }

    public static String printableStationList(Collection<Station> stations) {
        String returnString = "";
        for (Station s : stations) {
            if (s != null && s.info != null) {
                returnString += s.info.stationId + " ";
            }
        }
        return returnString;
    }

    public static String printableStationList(Station[] stations) {
        return printableStationList(new HashSet<Station>(
                Arrays.asList(stations)));
    }
}
