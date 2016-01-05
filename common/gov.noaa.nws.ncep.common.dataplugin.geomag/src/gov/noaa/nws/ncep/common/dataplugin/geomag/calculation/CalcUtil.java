package gov.noaa.nws.ncep.common.dataplugin.geomag.calculation;

import gov.noaa.nws.ncep.common.dataplugin.geomag.table.KFitTime;
import gov.noaa.nws.ncep.common.dataplugin.geomag.util.KStationCoefficientLookup;
import gov.noaa.nws.ncep.common.dataplugin.geomag.util.MissingValueCodeLookup;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The k index and decoder calculation utility.
 * 
 * <pre>
 * SOFTWARE HISTORY
 *                   
 * Date          Ticket#     Engineer   Description
 * -----------  ----------  ---------- --------------------------
 * 05/14/2013   #989        qzhou      Initial Creation
 * 06/23/2014   R4152       qzhou      Touched up 3 functions
 * 12/23/2014   R5412       sgurung    Change float to double, added methods related to "debug mode"
 * 06/08/2015   R8416       sgurung    Changed int[] to double[] for klimit, fixed bug in getKfromTable()
 * 10/07/2015   R11429      sgurung,jtravis Replaced hardcoded missing value codes and fixed warnings
 * 
 * </pre>
 * 
 * @author qzhou
 * @version 1
 */

public class CalcUtil {

    public static Double missingVal = MissingValueCodeLookup.getInstance()
            .getDefaultMissingValue();

    private static final double K_EXPONENT = 3.3;

    private static KStationCoefficientLookup stationCoeff = KStationCoefficientLookup
            .getInstance();

    // Gamma limit table
    private static enum Limit {
        K0(0), K1(5), K2(10), K3(20), K4(40), K5(70), K6(120), K7(200), K8(330), K9(
                500);

        private final int kConst;

        private Limit(int kConst) {
            this.kConst = kConst;
        }
    }

    public static int getKConst(int k) {
        int kConst = 0;
        if (k == 0)
            kConst = Limit.K0.kConst;
        else if (k == 1)
            kConst = Limit.K1.kConst;
        else if (k == 2)
            kConst = Limit.K2.kConst;
        else if (k == 3)
            kConst = Limit.K3.kConst;
        else if (k == 4)
            kConst = Limit.K4.kConst;
        else if (k == 5)
            kConst = Limit.K5.kConst;
        else if (k == 6)
            kConst = Limit.K6.kConst;
        else if (k == 7)
            kConst = Limit.K7.kConst;
        else if (k == 8)
            kConst = Limit.K8.kConst;
        else if (k == 9)
            kConst = Limit.K9.kConst;

        return kConst;
    }

    // A-index table
    private static enum K2a {
        a0(0), a1(3), a2(7), a3(15), a4(27), a5(48), a6(80), a7(140), a8(240), a9(
                400);

        private final int a;

        private K2a(int a) {
            this.a = a;
        }
    }

    public static int getK2a(int k) {
        int a = 0;
        if (k == 0)
            a = K2a.a0.a;
        else if (k == 1)
            a = K2a.a1.a;
        else if (k == 2)
            a = K2a.a2.a;
        else if (k == 3)
            a = K2a.a3.a;
        else if (k == 4)
            a = K2a.a4.a;
        else if (k == 5)
            a = K2a.a5.a;
        else if (k == 6)
            a = K2a.a6.a;
        else if (k == 7)
            a = K2a.a7.a;
        else if (k == 8)
            a = K2a.a8.a;
        else if (k == 9)
            a = K2a.a9.a;

        return a;
    }

    public static KStationCoefficientLookup getStationCoeff() {
        return stationCoeff;
    }

    public static int getK9Limit(String station) throws NumberFormatException {
        int k9 = 0;

        String k9Limit = getStationCoeff().getStationByCode(station)
                .getK9Limit();
        k9 = Integer.parseInt(k9Limit);

        return k9;
    }

    public static double getLongitude(String station)
            throws NumberFormatException {
        double lon = 0;
        if (station != null && !station.equalsIgnoreCase("")) {
            String longitude = getStationCoeff().getStationByCode(station)
                    .getLongitude();
            lon = Double.parseDouble(longitude);
        }
        return lon;
    }

    /*
     * map of the A and the B values in the order of 00-03, 03-06...
     */
    public static Map<Double, Double> getCoeffAandB(String station) {
        Map<Double, Double> abCoeff = new HashMap<Double, Double>();

        List<KFitTime> fitTime = getStationCoeff().getStationByCode(station)
                .getKFitTime();
        if (fitTime.size() != 8)
            return abCoeff;

        for (int i = 0; i < 8; i++) {
            double a = fitTime.get(i).getCoeffA();
            double b = fitTime.get(i).getCoeffB();
            abCoeff.put(a, b);
        }

        return abCoeff;
    }

    /*
     * map of the time period and the W values in the order of 00-03, 03-06...
     */
    public static Map<String, Double> getCoeffW(String station) {
        Map<String, Double> wCoeff = new HashMap<String, Double>();

        List<KFitTime> fitTime = getStationCoeff().getStationByCode(station)
                .getKFitTime();
        if (fitTime.size() != 8)
            return wCoeff;

        for (int i = 0; i < 8; i++) {
            String a = fitTime.get(i).getKey();
            double b = fitTime.get(i).getCoeffW();
            wCoeff.put(a, b);
        }

        return wCoeff;
    }

    public static double[] getKLimit(String station) {
        double[] kLimit = new double[10];
        int k9Limit = getK9Limit(station);
        for (int i = 0; i < kLimit.length; i++) {
            kLimit[i] = k9Limit * getKConst(i) / 500.0;
        }
        return kLimit;
    }

    public static int getKfromTable(double[] kLimit, double gamma) {
        int kIndex;

        int i = 0;
        for (i = 0; i < 10; i++) {
            if (gamma >= kLimit[i])
                continue;
            else
                break;
        }

        // take the lower of i. this step eq. K_limit = K9limit * [5, 10, 20,
        // 40...
        if (i > 0)
            i = i - 1;

        if (i <= 9)
            kIndex = i;
        else
            kIndex = 9;

        return kIndex;
    }

    // assume db time format yyyy-mm-dd hh:mm:ss
    public static Date getSPTime(Date currTime) {
        Calendar spTime = Calendar.getInstance();
        spTime.clear();
        spTime.setTimeInMillis(currTime.getTime());

        int hour = spTime.get(Calendar.HOUR_OF_DAY);

        if (hour >= 0 && hour < 3)
            hour = 0;
        else if (hour >= 3 && hour < 6)
            hour = 3;
        else if (hour >= 6 && hour < 9)
            hour = 6;
        else if (hour >= 9 && hour < 12)
            hour = 9;
        else if (hour >= 12 && hour < 15)
            hour = 12;
        else if (hour >= 15 && hour < 18)
            hour = 15;
        else if (hour >= 18 && hour < 21)
            hour = 18;
        else if (hour >= 21 && hour < 24)
            hour = 21;

        spTime.set(Calendar.HOUR_OF_DAY, hour);

        spTime.set(Calendar.MINUTE, 0);
        spTime.set(Calendar.SECOND, 0);

        return spTime.getTime();
    }

    public static Date getEPTime(Date currTime) {

        Calendar epTime = Calendar.getInstance();
        epTime.clear();
        epTime.setTimeInMillis(currTime.getTime());

        int hour = epTime.get(Calendar.HOUR_OF_DAY);

        if (hour >= 0 && hour < 3)
            hour = 3;
        else if (hour >= 3 && hour < 6)
            hour = 6;
        else if (hour >= 6 && hour < 9)
            hour = 9;
        else if (hour >= 9 && hour < 12)
            hour = 12;
        else if (hour >= 12 && hour < 15)
            hour = 15;
        else if (hour >= 15 && hour < 18)
            hour = 18;
        else if (hour >= 18 && hour < 21)
            hour = 21;
        else if (hour >= 21 && hour < 24)
            hour = 0;

        if (hour != 0) {
            epTime.set(Calendar.HOUR_OF_DAY, hour);
        } else {

            epTime.set(Calendar.DAY_OF_MONTH,
                    epTime.get(Calendar.DAY_OF_MONTH) + 1);
            epTime.set(Calendar.HOUR_OF_DAY, hour);
        }

        epTime.set(Calendar.MINUTE, 0);
        epTime.set(Calendar.SECOND, 0);

        return epTime.getTime();
    }

    public static boolean isHalfMissing(double[] items) {
        boolean halfMissaing = false;

        int i = 0;
        for (i = 0; i < items.length; i++) {
            if (items[i] == missingVal)
                i++;
        }
        if (i > items.length / 2)
            halfMissaing = true;

        return halfMissaing;
    }

    public static double getThird(double kpEst) {
        double half = 0.333333f / 2;
        double x = kpEst - (int) kpEst; // get decimal fraction

        if (x >= 0 && x <= half)
            x = 0;
        else if (x >= half && x <= 2 * half)
            x = 0.333333f;
        else if (x >= 2 * half && x <= 3 * half)
            x = 0.333333f;
        else if (x >= 3 * half && x <= 4 * half)
            x = 0.666666f;
        else if (x >= 4 * half && x <= 5 * half)
            x = 0.666666f;
        else if (x >= 5 * half && x <= 6 * half)
            x = 1;

        return x;
    }

    public static double maxValue(double[] dev) {
        double max = -CalcUtil.missingVal;
        for (int i = 0; i < dev.length; i++) {
            if (dev[i] > max && dev[i] < missingVal) {
                max = dev[i];
            }
        }
        return max;
    }

    public static double minValue(double[] dev) {
        double min = CalcUtil.missingVal;
        for (int i = 0; i < dev.length; i++) {
            if (dev[i] < min && dev[i] > -missingVal) {
                min = dev[i];
            }
        }
        return min;
    }

    /*
     * 10 element double array
     */
    public static double[] geKLength() {
        double[] kLength = new double[10];

        kLength[0] = 0;
        for (int i = 1; i < 10; i++) {
            kLength[i] = Math.exp(K_EXPONENT * Math.log(i));
            if (kLength[i] > 1080)
                kLength[i] = 1080;
        }

        return kLength;
    }

    // uri: /geomag/2013-05-20_00:00:00.0/HAD/101/GEOMAG
    public static String getSourceFromUri(String uri) {
        if (uri != null && uri.length() >= 37)
            return uri.substring(34, 37);
        else
            return "";
    }

    public static String getStationFromUri(String uri) {
        if (uri != null && uri.length() >= 37)
            return uri.substring(30, 33);
        else
            return "";
    }

    public static Date getTimeFromUri(String uri) throws ParseException {
        String format = "yyyy-MM-dd'_'HH:mm:ss.s";
        SimpleDateFormat sdf = new SimpleDateFormat(format);

        if (uri != null && uri.length() >= 37) {
            String time = uri.substring(8, 29);
            Date date = sdf.parse(time);
            return date;
        } else
            return new Date();
    }

    // get the front part before the source in the uri
    public static String separateSourceFrontUri(String uri) {
        if (uri != null && uri.length() >= 37)
            return uri.substring(0, 34);
        else
            return "";
    }

    public static double[] toDoubleArray(List<Double> list) {
        double[] ret = new double[list.size()];
        int i = 0;
        for (Double e : list)
            ret[i++] = e.doubleValue();
        return ret;
    }

    public static int[] toIntArray(List<Integer> list) {
        int[] ret = new int[list.size()];
        int i = 0;
        for (Integer e : list)
            ret[i++] = e.intValue();
        return ret;
    }

    public static boolean isLeapYear(int year) {
        boolean isLeap;

        if (year % 400 == 0)
            isLeap = true;
        else if (year % 100 == 0)
            isLeap = false;
        else if (year % 4 == 0)
            isLeap = true;
        else
            isLeap = false;

        return isLeap;
    }

    public static String getTimeFromFileName(String fileName) {
        String time = "";
        String temp = "";
        String year = "";
        String month = "";
        String day = "";
        String num = "";

        Calendar cal = Calendar.getInstance();

        if (fileName.startsWith("m")) {
            temp = fileName.substring(1, 7);
            year = "20" + temp.substring(4, 6);
            month = temp.substring(2, 4);
            day = temp.substring(0, 2);
        } else if (fileName.startsWith("ha")) {
            temp = fileName.substring(2, 9);
            year = temp.substring(3, 7);
            num = temp.substring(0, 3);
            try {
                cal.set(Calendar.DAY_OF_YEAR, Integer.parseInt(num));
            } catch (NumberFormatException e) {

            }
            month = String.valueOf(cal.get(Calendar.MONTH));
            day = String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
        } else if (fileName.startsWith("BOU") || fileName.startsWith("CL2")
                || fileName.startsWith("CMO") || fileName.startsWith("OTT")
                || fileName.startsWith("MEA")) {
            temp = fileName.substring(3, 10);
            year = temp.substring(0, 4);
            num = temp.substring(4, 7);

            try {
                cal.set(Calendar.DAY_OF_YEAR, Integer.parseInt(num));
            } catch (NumberFormatException e) {

            }
            month = String.valueOf(cal.get(Calendar.MONTH) + 1);
            day = String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
        } else if (fileName.startsWith("ha") || fileName.startsWith("CNB")
                || fileName.startsWith("OTT") || fileName.startsWith("WNG")) {
            temp = fileName.substring(3, 10);
            year = temp.substring(0, 4);
            month = temp.substring(4, 6);
            day = temp.substring(6, 8);
        }

        if (month.length() == 1)
            month = "0" + month;
        if (day.length() == 1)
            day = "0" + day;
        time = year + "-" + month + "-" + day;
        return time;
    }

    public static double getMedian(double[] array) {
        double median = 0;
        if (array.length <= 1)
            return array[0];

        double[] arraySort = array.clone();
        Arrays.sort(arraySort);

        // remove missing data
        List<Double> newArray = new ArrayList<Double>();
        for (int k = 0; k < arraySort.length; k++) {
            if (arraySort[k] != missingVal) {
                newArray.add(arraySort[k]);
            } else {
                break;
            }
        }

        int size = newArray.size();
        if (size % 2 == 0) {
            median = (newArray.get(size / 2) + newArray.get(size / 2 - 1)) / 2;
        } else {
            median = newArray.get((size - 1) / 2);
        }

        return median;
    }

    public static Double round(int decimalPlace, Double d) {

        BigDecimal bd = new BigDecimal(Double.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_EVEN);
        return bd.doubleValue();
    }

    public static Hashtable<String, String> getTestComponents(String dataURI) {

        String regexIncludingHourAndMin = ".*GEOMAG\\.TEST\\.\\d{3}\\.[0-2][0-9]\\.[0-5][0-9]";

        Pattern p = Pattern.compile(regexIncludingHourAndMin);
        Matcher m = p.matcher(dataURI);

        Hashtable<String, String> results = new Hashtable<String, String>();

        if (m.matches()) { // we have the hour and minutes included

            int startIndex = dataURI.indexOf("GEOMAG") + 12;
            String dataURIComponent = dataURI.substring(startIndex);

            StringTokenizer st = new StringTokenizer(dataURIComponent, ".");

            String testNumber = st.nextToken();
            String hour = st.nextToken();
            String minute = st.nextToken();

            results.put("TEST_NUMBER", testNumber);
            results.put("HOUR", hour);
            results.put("MINUTE", minute);

        }

        return results;

    }

    public static String parseFileName(String fileName) {

        StringBuffer sb = new StringBuffer();
        String regexIncludingHourAndMin = ".*\\.test\\.\\d{3}\\.[0-2][0-9]\\.[0-5][0-9].*";
        String regexNoHourAndMin = ".*\\.test\\.\\d{3}.*";
        String result = null;

        Pattern p = Pattern.compile(regexIncludingHourAndMin);
        Matcher m = p.matcher(fileName);

        if (m.matches()) { // we have the hour and minutes included

            StringTokenizer st = new StringTokenizer(fileName, ".");

            while (st.hasMoreElements()) {

                if (st.countTokens() == 6) {
                    st.nextElement();
                    continue;
                }

                if (st.countTokens() == 1) {

                    st.nextElement();

                } else if (st.countTokens() == 2) {

                    sb.append(st.nextElement());

                } else {

                    sb.append(st.nextElement() + ".");

                }

            }

            result = "." + sb.toString().toUpperCase().trim();

        } else {

            p = Pattern.compile(regexNoHourAndMin);
            m = p.matcher(fileName);

            if (m.matches()) { // no hour and minutes included

                StringTokenizer st = new StringTokenizer(fileName, ".");

                while (st.hasMoreElements()) {

                    if (st.countTokens() == 4) {
                        st.nextElement();
                        continue;
                    }

                    if (st.countTokens() == 1) {

                        st.nextElement();

                    } else if (st.countTokens() == 2) {

                        sb.append(st.nextElement());

                    } else {

                        sb.append(st.nextElement() + ".");

                    }
                }

                result = "." + sb.toString().toUpperCase().trim();

            }
        }

        return result;

    }

    public static boolean isDebugModeRequired(String dataURI, int hour,
            int minute) {

        boolean debugModeRequired = false;

        String regexIncludingHourAndMin = ".*GEOMAG\\.TEST\\.\\d{3}\\.[0-2][0-9]\\.[0-5][0-9]";

        Pattern p = Pattern.compile(regexIncludingHourAndMin);
        Matcher m = p.matcher(dataURI);

        if (m.matches()) { // we have the hour and minutes included

            int startIndex = dataURI.indexOf("GEOMAG") + 12;
            String dataURIComponent = dataURI.substring(startIndex);

            StringTokenizer st = new StringTokenizer(dataURIComponent, ".");

            st.nextToken();
            int testHour = Integer.valueOf(st.nextToken()).intValue();
            int testMinute = Integer.valueOf(st.nextToken()).intValue();

            if (testHour == hour && testMinute == minute) {
                debugModeRequired = true;
            }
        }

        return debugModeRequired;

    }

}
