package gov.noaa.nws.ncep.common.dataplugin.geomag.calculation;

import gov.noaa.nws.ncep.common.dataplugin.geomag.table.KsThree;
import gov.noaa.nws.ncep.common.dataplugin.geomag.util.KStationCoefficientLookup;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The calculation of Kp and related.
 * 
 * <pre>
 * SOFTWARE HISTORY
 *                   
 * Date          Ticket#     Engineer   Description
 * -----------  ----------  ---------- --------------------------
 * 05/14/2013   #989        qzhou      Initial Creation
 * 03/18/2014   #1123       qzhou      default k to 99999
 * 12/23/2014   R5412       sgurung    Change float to double 
 * 06/08/2015   R8416       sgurung    Added new methods, fixed bug in method getKs()
 * 10/07/2015   R11429      sgurung,jtravis Replaced hardcoded missing value codes
 * 
 * </pre>
 * 
 * @author qzhou
 * @version 1
 */
// @formatter:off
public class CalcKp {

    public CalcKp() {

    }

    public static double[] getKest(String station, double[] kIndex,
            double[] gamma) {
        double[] kest = new double[8];

        for (int i = 0; i < 8; i++) {
            double[] gammaLimit = CalcUtil.getKLimit(station);
            if (kIndex[i] < 9) {
                kest[i] = kIndex[i]
                        + (gamma[i] - gammaLimit[(int) kIndex[i]])
                        / (gammaLimit[(int) kIndex[i] + 1] - gammaLimit[(int) kIndex[i]]);
            } else if (kIndex[i] < 999) {
                kest[i] = 9.0;
            } else {
                kest[i] = CalcUtil.missingVal;
            }

        }

        return kest;
    }

    public static double getKest(String station, int kIndex, double gamma) {
        double kest = CalcUtil.missingVal;

        double[] kLimit = CalcUtil.getKLimit(station);
        if (kIndex < 9) {
            kest = kIndex + (gamma - kLimit[kIndex])
                    / (kLimit[kIndex + 1] - kLimit[kIndex]);
        } else if (kIndex < 999) {
            kest = 9.0;
        }

        return kest;
    }

    /*
     * list of the station coefficient values in the order of 00-03, 03-06...
     */
    public static ArrayList<KsThree> getKsThreeList(String station) {

        ArrayList<KsThree> threeKsList = CalcUtil.getStationCoeff()
                .getStationByCode(station).getKsThree();// size 24

        return threeKsList;
    }

    public static List<Integer> getKsThree(Date time, String station, int kIndex) {
        List<Integer> ks = new ArrayList<Integer>();

        ArrayList<KsThree> ksThreeList = getKsThreeList(station);

        if (ksThreeList != null && !ksThreeList.isEmpty()) {

            int hour = CalcUtil.getSPTime(time).getHours();
            int period = hour / 3;// 24 -> 8

            KsThree ksThree = ksThreeList.get(period);

            if (ksThree != null) {
                ks.add(getKsOfKsThree(kIndex, ksThree));
            }

            ksThree = ksThreeList.get(period + 8);

            if (ksThree != null) {
                ks.add(getKsOfKsThree(kIndex, ksThree));
            }

            ksThree = ksThreeList.get(period + 16);

            if (ksThree != null) {
                ks.add(getKsOfKsThree(kIndex, ksThree));
            }

        }

        return ks;
    }

    private static int getKsOfKsThree(int k, KsThree ksThree) {
        int ks = CalcUtil.missingVal.intValue();

        if (k == 0) {
            ks = ksThree.getK0();
        } else if (k == 1) {
            ks = ksThree.getK1();
        } else if (k == 2) {
            ks = ksThree.getK2();
        } else if (k == 3) {
            ks = ksThree.getK3();
        } else if (k == 4) {
            ks = ksThree.getK4();
        } else if (k == 5) {
            ks = ksThree.getK5();
        } else if (k == 6) {
            ks = ksThree.getK6();
        } else if (k == 7) {
            ks = ksThree.getK7();
        } else if (k == 8) {
            ks = ksThree.getK8();
        } else if (k == 9) {
            ks = ksThree.getK9();
        }

        return ks;
    }

    /**
     * Get Date range for Leap Years
     * 
     * @param time
     * @return Hashtable<String, Date>
     * @throws ParseException
     */
    private static Hashtable<String, Date> getDateForLeapYears(Date time)
            throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Hashtable<String, Date> leapYearDates = new Hashtable<String, Date>();

        Calendar cal = Calendar.getInstance();
        cal.setTime(time);
        int year = cal.get(Calendar.YEAR);

        if (KStationCoefficientLookup.getInstance().useDefaultKsDateRange) {
            // Start Range: Feb 15 – Feb 24
            leapYearDates.put("Range2Start", sdf.parse(year + "-02-15"));

            // Start Range: Feb 25 – Mar 05
            leapYearDates.put("Range3Start", sdf.parse(year + "-02-25"));

        } else { // Read Ks Date Ranges from the properties file

            leapYearDates
                    .put("Range2Start",
                            sdf.parse(year
                                    + "-"
                                    + KStationCoefficientLookup
                                            .getInstance()
                                            .getKsDateRange()
                                            .getProperty(
                                                    KStationCoefficientLookup.LEAP_YEAR_START_RANGE_2)));
            leapYearDates
                    .put("Range3Start",
                            sdf.parse(year
                                    + "-"
                                    + KStationCoefficientLookup
                                            .getInstance()
                                            .getKsDateRange()
                                            .getProperty(
                                                    KStationCoefficientLookup.LEAP_YEAR_START_RANGE_3)));
        }

        return leapYearDates;
    }

    /**
     * Get Date range for Non-Leap Years
     * 
     * @param time
     * @return Hastable<String, Date>
     * @throws ParseException
     */
    private static Hashtable<String, Date> getDateForNonLeapYears(Date time)
            throws ParseException {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Hashtable<String, Date> nonLeapYearDates = new Hashtable<String, Date>();

        Calendar cal = Calendar.getInstance();
        cal.setTime(time);
        int year = cal.get(Calendar.YEAR);

        if (KStationCoefficientLookup.getInstance().useDefaultKsDateRange) {

            // Start Range Jan 01 – Feb 13
            nonLeapYearDates.put("Range1Start", sdf.parse(year + "-01-01"));

            // Start Range Feb 14 – Feb 23
            nonLeapYearDates.put("Range2Start", sdf.parse(year + "-02-14"));

            // Start Range Feb 24 – Mar 05
            nonLeapYearDates.put("Range3Start", sdf.parse(year + "-02-24"));

            // Start Range Mar 06 – Mar 15
            nonLeapYearDates.put("Range4Start", sdf.parse(year + "-03-06"));

            // Start Range Mar 16 – Apr 15
            nonLeapYearDates.put("Range5Start", sdf.parse(year + "-03-16"));

            // Start Range Apr 16 – Apr 25
            nonLeapYearDates.put("Range6Start", sdf.parse(year + "-04-16"));

            // Start Range Apr 26 – May 05
            nonLeapYearDates.put("Range7Start", sdf.parse(year + "-04-26"));

            // Start Range May 06 – May 15
            nonLeapYearDates.put("Range8Start", sdf.parse(year + "-05-06"));

            // Start Range May 16 – Aug 16
            nonLeapYearDates.put("Range9Start", sdf.parse(year + "-05-16"));

            // Start Range Aug 17 – Aug 26
            nonLeapYearDates.put("Range10Start", sdf.parse(year + "-08-17"));

            // Start Range Aug 27 – Sep 05
            nonLeapYearDates.put("Range11Start", sdf.parse(year + "-08-27"));

            // Start Range Sep 06 – Sep 15
            nonLeapYearDates.put("Range12Start", sdf.parse(year + "-09-06"));

            // Start Range Sep 16 – Oct 16
            nonLeapYearDates.put("Range13Start", sdf.parse(year + "-09-16"));

            // Start Range Oct 17 – Oct 26
            nonLeapYearDates.put("Range14Start", sdf.parse(year + "-10-17"));

            // Start Range Oct 27 – Nov 05
            nonLeapYearDates.put("Range15Start", sdf.parse(year + "-10-27"));

            // Start Range Nov 06 – Nov 15
            nonLeapYearDates.put("Range16Start", sdf.parse(year + "-11-06"));

            // Start Range Nov 16 – Dec 31
            nonLeapYearDates.put("Range17Start", sdf.parse(year + "-11-16"));

            // End of Range Nov 16 – Dec 31
            nonLeapYearDates.put("Range17End", sdf.parse(year + "-12-31"));

        } else { // Read Ks Date Ranges from the properties file

            nonLeapYearDates
                    .put("Range1Start",
                            sdf.parse(year
                                    + "-"
                                    + KStationCoefficientLookup
                                            .getInstance()
                                            .getKsDateRange()
                                            .getProperty(
                                                    KStationCoefficientLookup.NON_LEAP_YEAR_START_RANGE_1)));
            nonLeapYearDates
                    .put("Range2Start",
                            sdf.parse(year
                                    + "-"
                                    + KStationCoefficientLookup
                                            .getInstance()
                                            .getKsDateRange()
                                            .getProperty(
                                                    KStationCoefficientLookup.NON_LEAP_YEAR_START_RANGE_2)));
            nonLeapYearDates
                    .put("Range3Start",
                            sdf.parse(year
                                    + "-"
                                    + KStationCoefficientLookup
                                            .getInstance()
                                            .getKsDateRange()
                                            .getProperty(
                                                    KStationCoefficientLookup.NON_LEAP_YEAR_START_RANGE_3)));
            nonLeapYearDates
                    .put("Range4Start",
                            sdf.parse(year
                                    + "-"
                                    + KStationCoefficientLookup
                                            .getInstance()
                                            .getKsDateRange()
                                            .getProperty(
                                                    KStationCoefficientLookup.NON_LEAP_YEAR_START_RANGE_4)));
            nonLeapYearDates
                    .put("Range5Start",
                            sdf.parse(year
                                    + "-"
                                    + KStationCoefficientLookup
                                            .getInstance()
                                            .getKsDateRange()
                                            .getProperty(
                                                    KStationCoefficientLookup.NON_LEAP_YEAR_START_RANGE_5)));
            nonLeapYearDates
                    .put("Range6Start",
                            sdf.parse(year
                                    + "-"
                                    + KStationCoefficientLookup
                                            .getInstance()
                                            .getKsDateRange()
                                            .getProperty(
                                                    KStationCoefficientLookup.NON_LEAP_YEAR_START_RANGE_6)));
            nonLeapYearDates
                    .put("Range7Start",
                            sdf.parse(year
                                    + "-"
                                    + KStationCoefficientLookup
                                            .getInstance()
                                            .getKsDateRange()
                                            .getProperty(
                                                    KStationCoefficientLookup.NON_LEAP_YEAR_START_RANGE_7)));
            nonLeapYearDates
                    .put("Range8Start",
                            sdf.parse(year
                                    + "-"
                                    + KStationCoefficientLookup
                                            .getInstance()
                                            .getKsDateRange()
                                            .getProperty(
                                                    KStationCoefficientLookup.NON_LEAP_YEAR_START_RANGE_8)));
            nonLeapYearDates
                    .put("Range9Start",
                            sdf.parse(year
                                    + "-"
                                    + KStationCoefficientLookup
                                            .getInstance()
                                            .getKsDateRange()
                                            .getProperty(
                                                    KStationCoefficientLookup.NON_LEAP_YEAR_START_RANGE_9)));
            nonLeapYearDates
                    .put("Range10Start",
                            sdf.parse(year
                                    + "-"
                                    + KStationCoefficientLookup
                                            .getInstance()
                                            .getKsDateRange()
                                            .getProperty(
                                                    KStationCoefficientLookup.NON_LEAP_YEAR_START_RANGE_10)));
            nonLeapYearDates
                    .put("Range11Start",
                            sdf.parse(year
                                    + "-"
                                    + KStationCoefficientLookup
                                            .getInstance()
                                            .getKsDateRange()
                                            .getProperty(
                                                    KStationCoefficientLookup.NON_LEAP_YEAR_START_RANGE_11)));
            nonLeapYearDates
                    .put("Range12Start",
                            sdf.parse(year
                                    + "-"
                                    + KStationCoefficientLookup
                                            .getInstance()
                                            .getKsDateRange()
                                            .getProperty(
                                                    KStationCoefficientLookup.NON_LEAP_YEAR_START_RANGE_12)));
            nonLeapYearDates
                    .put("Range13Start",
                            sdf.parse(year
                                    + "-"
                                    + KStationCoefficientLookup
                                            .getInstance()
                                            .getKsDateRange()
                                            .getProperty(
                                                    KStationCoefficientLookup.NON_LEAP_YEAR_START_RANGE_13)));
            nonLeapYearDates
                    .put("Range14Start",
                            sdf.parse(year
                                    + "-"
                                    + KStationCoefficientLookup
                                            .getInstance()
                                            .getKsDateRange()
                                            .getProperty(
                                                    KStationCoefficientLookup.NON_LEAP_YEAR_START_RANGE_14)));
            nonLeapYearDates
                    .put("Range15Start",
                            sdf.parse(year
                                    + "-"
                                    + KStationCoefficientLookup
                                            .getInstance()
                                            .getKsDateRange()
                                            .getProperty(
                                                    KStationCoefficientLookup.NON_LEAP_YEAR_START_RANGE_15)));
            nonLeapYearDates
                    .put("Range16Start",
                            sdf.parse(year
                                    + "-"
                                    + KStationCoefficientLookup
                                            .getInstance()
                                            .getKsDateRange()
                                            .getProperty(
                                                    KStationCoefficientLookup.NON_LEAP_YEAR_START_RANGE_16)));
            nonLeapYearDates
                    .put("Range17Start",
                            sdf.parse(year
                                    + "-"
                                    + KStationCoefficientLookup
                                            .getInstance()
                                            .getKsDateRange()
                                            .getProperty(
                                                    KStationCoefficientLookup.NON_LEAP_YEAR_START_RANGE_17)));
            nonLeapYearDates
                    .put("Range17End",
                            sdf.parse(year
                                    + "-"
                                    + KStationCoefficientLookup
                                            .getInstance()
                                            .getKsDateRange()
                                            .getProperty(
                                                    KStationCoefficientLookup.NON_LEAP_YEAR_END_RANGE_17)));
        }

        return nonLeapYearDates;

    }

    /**
     * Calculate the ks value for a given station with a known k value and known
     * time accounting for leap year vs non-leap years
     * 
     * @param station
     * @param k
     * @param time
     * @return double
     * @throws ParseException
     */
    public static double getKs(String station, int k, Date time)
            throws ParseException {

        double ks = 0;
        Calendar cal = Calendar.getInstance();
        cal.setTime(time);
        int year = cal.get(Calendar.YEAR);

        if (k == CalcUtil.missingVal) {
            return CalcUtil.missingVal;
        }

        // For non-leap years
        //
        // Range1Start - Jan 01 – Feb 13: Ks = ThreeKs(JFND)/3.0
        // Range2Start - Feb 14 – Feb 23: Ks = [0.75*ThreeKs(JFND) +
        // 0.25*ThreeKs(MASO)]/3.0
        // Range3Start - Feb 24 – Mar 05: Ks = [0.50*ThreeKs(JFND) +
        // 0.50*ThreeKs(MASO)]/3.0
        // Range4Start - Mar 06 – Mar 15: Ks = [0.25*ThreeKs(JFND) +
        // 0.75*ThreeKs(MASO)]/3.0
        // Range5Start - Mar 16 – Apr 15: Ks = ThreeKs(MASO)/3.0
        // Range6Start - Apr 16 – Apr 25: Ks = [0.75*ThreeKs(MASO) +
        // 0.25*ThreeKs(MJJA)]/3.0
        // Range7Start - Apr 26 – May 05: Ks = [0.50*ThreeKs(MASO) +
        // 0.50*ThreeKs(MJJA)]/3.0
        // Range8Start - May 06 – May 15: Ks = [0.25*ThreeKs(MASO) +
        // 0.75*ThreeKs(MJJA)]/3.0
        // Range9Start - May 16 – Aug 16: Ks = ThreeKs(MJJA)/3.0
        // Range10Start - Aug 17 – Aug 26: Ks = [0.75*ThreeKs(MJJA) +
        // 0.25*ThreeKs(MASO)]/3.0
        // Range11Start - Aug 27 – Sep 05: Ks = [0.50*ThreeKs(MJJA) +
        // 0.50*ThreeKs(MASO)]/3.0
        // Range12Start - Sep 06 – Sep 15: Ks = [0.25*ThreeKs(MJJA) +
        // 0.75*ThreeKs(MASO)]/3.0
        // Range13Start - Sep 16 – Oct 16: Ks = ThreeKs(MASO)/3.0
        // Range14Start - Oct 17 – Oct 26: Ks = [0.75*ThreeKs(MASO) +
        // 0.25*ThreeKs(JFND)]/3.0
        // Range15Start - Oct 27 – Nov 05: Ks = [0.50*ThreeKs(MASO) +
        // 0.50*ThreeKs(JFND)]/3.0
        // Range16Start - Nov 06 – Nov 15: Ks = [0.25*ThreeKs(MASO) +
        // 0.75*ThreeKs(JFND)]/3.0
        // Range17End - Nov 16 – Dec 31: Ks = ThreeKs(JFND)/3.0
        //
        Hashtable<String, Date> nonLeapYearDates = getDateForNonLeapYears(time);

        // For leap years
        //
        // Jan 01 – Feb 14: Ks = ThreeKs(JFND)/3.0
        // Range2Start - Feb 15 – Feb 24: Ks = [0.75*ThreeKs(JFND) +
        // 0.25*ThreeKs(MASO)]/3.0
        // Range3Start - Feb 25 – Mar 05: Ks = [0.50*ThreeKs(JFND) +
        // 0.50*ThreeKs(MASO)]/3.0
        //
        Hashtable<String, Date> leapYearDates = getDateForLeapYears(time);

        List<Integer> ksThree = getKsThree(time, station, k);

        if (time.compareTo(nonLeapYearDates.get("Range1Start")) >= 0
                && time.compareTo(nonLeapYearDates.get("Range2Start")) < 0) {
            ks = ksThree.get(0) / 3.0;
        } else if (time.compareTo(nonLeapYearDates.get("Range2Start")) >= 0
                && time.compareTo(nonLeapYearDates.get("Range3Start")) < 0) {
            ks = (0.75 * ksThree.get(0) + 0.25 * ksThree.get(1)) / 3.0;
        } else if (time.compareTo(nonLeapYearDates.get("Range3Start")) >= 0
                && time.compareTo(nonLeapYearDates.get("Range4Start")) < 0) {
            ks = (0.5 * ksThree.get(0) + 0.5 * ksThree.get(1)) / 3.0;
        } else if (time.compareTo(nonLeapYearDates.get("Range4Start")) >= 0
                && time.compareTo(nonLeapYearDates.get("Range5Start")) < 0) {
            ks = (0.25 * ksThree.get(0) + 0.75 * ksThree.get(1)) / 3.0;
        } else if (time.compareTo(nonLeapYearDates.get("Range5Start")) >= 0
                && time.compareTo(nonLeapYearDates.get("Range6Start")) < 0) {
            ks = ksThree.get(1) / 3.0;
        } else if (time.compareTo(nonLeapYearDates.get("Range6Start")) >= 0
                && time.compareTo(nonLeapYearDates.get("Range7Start")) < 0) {
            ks = (0.75 * ksThree.get(1) + 0.25 * ksThree.get(2)) / 3.0;
        } else if (time.compareTo(nonLeapYearDates.get("Range7Start")) >= 0
                && time.compareTo(nonLeapYearDates.get("Range8Start")) < 0) {
            ks = (0.5 * ksThree.get(1) + 0.5 * ksThree.get(2)) / 3.0;
        } else if (time.compareTo(nonLeapYearDates.get("Range8Start")) >= 0
                && time.compareTo(nonLeapYearDates.get("Range9Start")) < 0) {
            ks = (0.25 * ksThree.get(1) + 0.75 * ksThree.get(2)) / 3.0;
        } else if (time.compareTo(nonLeapYearDates.get("Range9Start")) >= 0
                && time.compareTo(nonLeapYearDates.get("Range10Start")) < 0) {
            ks = ksThree.get(2) / 3.0;
        } else if (time.compareTo(nonLeapYearDates.get("Range10Start")) >= 0
                && time.compareTo(nonLeapYearDates.get("Range11Start")) < 0) {
            ks = (0.75 * ksThree.get(2) + 0.25 * ksThree.get(1)) / 3.0;
        } else if (time.compareTo(nonLeapYearDates.get("Range11Start")) >= 0
                && time.compareTo(nonLeapYearDates.get("Range12Start")) < 0) {
            ks = (0.5 * ksThree.get(2) + 0.5 * ksThree.get(1)) / 3.0;
        } else if (time.compareTo(nonLeapYearDates.get("Range12Start")) >= 0
                && time.compareTo(nonLeapYearDates.get("Range13Start")) < 0) {
            ks = (0.25 * ksThree.get(2) + 0.75 * ksThree.get(1)) / 3.0;
        } else if (time.compareTo(nonLeapYearDates.get("Range13Start")) >= 0
                && time.compareTo(nonLeapYearDates.get("Range14Start")) < 0) {
            ks = ksThree.get(1) / 3.0;
        } else if (time.compareTo(nonLeapYearDates.get("Range14Start")) >= 0
                && time.compareTo(nonLeapYearDates.get("Range15Start")) < 0) {
            ks = (0.75 * ksThree.get(1) + 0.25 * ksThree.get(0)) / 3.0;
        } else if (time.compareTo(nonLeapYearDates.get("Range15Start")) >= 0
                && time.compareTo(nonLeapYearDates.get("Range16Start")) < 0) {
            ks = (0.5 * ksThree.get(1) + 0.5 * ksThree.get(0)) / 3.0;
        } else if (time.compareTo(nonLeapYearDates.get("Range16Start")) >= 0
                && time.compareTo(nonLeapYearDates.get("Range17Start")) < 0) {
            ks = (0.25 * ksThree.get(1) + 0.75 * ksThree.get(0)) / 3.0;
        } else if (time.compareTo(nonLeapYearDates.get("Range17Start")) >= 0
                && time.compareTo(nonLeapYearDates.get("Range17End")) < 0) {
            ks = ksThree.get(0) / 3.0;
        } else if (CalcUtil.isLeapYear(year)) {
            if (time.compareTo(leapYearDates.get("Range2Start")) >= 0
                    && time.compareTo(leapYearDates.get("Range3Start")) < 0) {
                ks = (0.75 * ksThree.get(0) + 0.25 * ksThree.get(1)) / 3.0;
            } else if (time.compareTo(leapYearDates.get("Range3Start")) >= 0
                    && time.compareTo(nonLeapYearDates.get("Range4Start")) < 0) {
                ks = (0.5 * ksThree.get(0) + 0.5 * ksThree.get(1)) / 3.0;
            }
        }

        return ks;
    }

    public static int getAest(String station, int kIndex) {
        return CalcUtil.getK2a(kIndex);
    }

    @SuppressWarnings("unchecked")
    public static double[] getKpEst(String[] station, double[] ks) {
        double kpEst[] = new double[ks.length];
        double[][] wcoeff = new double[station.length][ks.length];

        if (ks.length != 8)
            return kpEst;

        for (int i = 0; i < station.length; i++) {
            Map<String, Double> coeff = CalcUtil.getCoeffW(station[i]);
            int j = 0;

            Iterator<?> iter = coeff.entrySet().iterator();
            while (iter.hasNext()) {
                wcoeff[i][j] = ((Map.Entry<String, Double>) iter.next())
                        .getValue();
                j++;
            }
        }

        double sumW = 0;
        double sumWK = 0;

        for (int j = 0; j < ks.length; j++) {
            for (int i = 0; i < station.length; i++) {
                sumW += wcoeff[i][j];
                sumWK += wcoeff[i][j] * ks[i];
            }

            kpEst[j] = sumWK / sumW;
            kpEst[j] = (int) kpEst[j] + CalcUtil.getThird(kpEst[j]);
        }

        return kpEst;
    }

    @SuppressWarnings("unchecked")
    public static double getKpEst(String[] station, double ks, String fitTime) {
        double kpEst = 0;
        double[] wcoeff = new double[8];

        for (int i = 0; i < station.length; i++) {
            Map<String, Double> coeff = CalcUtil.getCoeffW(station[i]);
            int j = 0;
            Iterator<?> iter = coeff.entrySet().iterator();
            while (iter.hasNext()) {
                if (((Map.Entry<String, Double>) iter.next()).getKey()
                        .equalsIgnoreCase(fitTime)) {
                    wcoeff[i] = ((Map.Entry<String, Double>) iter.next())
                            .getValue();
                    break;
                }
                j++;
            }
        }

        double sumW = 0;
        double sumWK = 0;

        for (int i = 0; i < station.length; i++) {
            sumW += wcoeff[i];
            sumWK += wcoeff[i] * ks;
        }

        kpEst = sumWK / sumW;
        kpEst = (int) kpEst + CalcUtil.getThird(kpEst);

        return kpEst;
    }

    public static String[] getKp(double kpEst[], String[] kpModifier) {
        String[] kp = new String[kpEst.length];
        if (kpEst.length != kpModifier.length)
            return kp;

        for (int i = 0; i < kpEst.length; i++) {
            int k = (int) Math.round(kpEst[i]);
            kp[i] = k + kpModifier[i];
        }

        return kp;
    }

    public static String getKp(double kpEst, String kpModifier) {
        int kp = (int) Math.round(kpEst);

        return kp + kpModifier;
    }

}
// @formatter:on