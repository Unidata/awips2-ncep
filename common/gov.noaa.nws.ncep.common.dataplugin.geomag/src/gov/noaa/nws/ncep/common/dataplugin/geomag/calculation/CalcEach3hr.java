package gov.noaa.nws.ncep.common.dataplugin.geomag.calculation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The calculation of k, 3 hour related.
 * 
 * <pre>
 * SOFTWARE HISTORY
 *                   
 * Date          Ticket#     Engineer   Description
 * -----------  ----------  ---------- --------------------------
 * 05/14/2013   #989        qzhou      Initial Creation
 * 03/18/2014   #1123       qzhou      Add getHQdcOrDQdc
 * 06/23/2014   R4152       qzhou      Touched up functions that do not affect the results
 * 12/23/2014   R5412       sgurung    Change float to double, fixed equation in getQuietLevelHourAvg()
 * 10/07/2015   R11429      sgurung,jtravis Replaced hardcoded missing value codes
 * 
 * </pre>
 * 
 * @author qzhou
 * @version 1
 */
public class CalcEach3hr {

    private static final int NIGHT_LENGTH = 90; // min

    private static final int DAWN_LENGTH = 60;

    private static final int DAY_LENGTH = 0;

    private static final int DUSK_LENGTH = 60;

    private static int DAYS = 30;

    private static int HOURS = 24;

    private static int MINUTES = 60;

    /*
     * calculate hrAvgs for this hour
     * 
     * @param bestList -- contains 1 hour data
     */
    public static double[] getSimpleHourAvg(List bestList) {
        double[] simpHrAvg = new double[2];
        double simpHrAvg1 = 0;
        double simpHrAvg2 = 0;
        double sum1 = 0;
        double sum2 = 0;
        int rec1 = 0;
        int rec2 = 0;

        for (int i = 0; i < bestList.size(); i++) {

            List<Double> list = (List<Double>) bestList.get(i);

            double comp1 = list.get(1);
            double comp2 = list.get(2);

            if (comp1 != CalcUtil.missingVal) {
                sum1 += comp1;
                rec1++;
            }
            if (comp2 != CalcUtil.missingVal) {
                sum2 += comp2;
                rec2++;
            }
        }

        if (rec1 > 30) // less than half missing value
            simpHrAvg1 = (double) sum1 / rec1;
        else
            simpHrAvg1 = CalcUtil.missingVal;

        if (rec2 > 30) // less than half missing value
            simpHrAvg2 = (double) sum2 / rec2;
        else
            simpHrAvg2 = CalcUtil.missingVal;

        simpHrAvg[0] = simpHrAvg1;
        simpHrAvg[1] = simpHrAvg2;

        return simpHrAvg;
    }

    /*
     * calculate hrAvgs for this day.
     * 
     * @param data -- data of one day, 1440
     */
    public static double[] getSimpleHourAvg(double[] data) { // data 1440

        double[] simpHrAvg = new double[HOURS];

        for (int ihr = 0; ihr < HOURS; ihr++) {
            double sum = 0;
            int missing = 0;

            for (int i = ihr * MINUTES; i < ihr * MINUTES + MINUTES; i++) {

                if (data[i] != CalcUtil.missingVal)
                    sum += data[i];
                else
                    missing++;
            }

            if (missing < 30) // less than half missing value
                simpHrAvg[ihr] = (double) sum / (MINUTES - missing);
            else
                simpHrAvg[ihr] = CalcUtil.missingVal;
        }

        return simpHrAvg;
    }

    /*
     * calculate hrAvgs for this hour in data array
     * 
     * @param data -- data of one day, 1440
     */
    public static double getSimpleHourAvg(double[] data, int hour) { // one day
                                                                     // 1440,
                                                                     // avg
                                                                     // for
                                                                     // hour-1

        double simpHrAvg = 0;
        double sum = 0;
        int rec = 0;

        if (data.length <= hour * MINUTES + MINUTES)
            for (int i = hour * MINUTES; i < data.length; i++) {
                if (data[i] != CalcUtil.missingVal) {
                    sum += data[i];
                    rec++;
                }
            }
        else
            for (int i = hour * MINUTES; i < hour * MINUTES + MINUTES; i++) {
                if (data[i] != CalcUtil.missingVal) {
                    sum += data[i];
                    rec++;
                }
            }

        if (rec > 30) // less than half missing value
            simpHrAvg = (double) sum / (rec);
        else
            simpHrAvg = CalcUtil.missingVal;

        return simpHrAvg;
    }

    /*
     * @param simpHrAvgH -- data of 30 intervals(720 hours)
     * 
     * @return disturbance levels for 30 intervals
     */
    public static double[] getDisturbanceLevel(double[] simpHrAvgH,
            double[] simpHrAvgD) {
        double[] dB = new double[30];

        for (int j = 0; j < DAYS; j++) {
            double sum = 0;
            int count = 0;

            for (int i = 0; i < 23; i++) {
                int ii = j * HOURS + i;

                if (simpHrAvgH[ii] != CalcUtil.missingVal
                        && simpHrAvgD[ii] != CalcUtil.missingVal
                        && simpHrAvgH[ii + 1] != CalcUtil.missingVal
                        && simpHrAvgD[ii + 1] != CalcUtil.missingVal) {
                    sum += Math
                            .sqrt(Math.pow(
                                    (simpHrAvgH[ii + 1] - simpHrAvgH[ii]), 2)
                                    + Math.pow(
                                            (simpHrAvgD[ii + 1] - simpHrAvgD[ii]),
                                            2));
                    count++;
                }

            }

            if (count >= 12) // not 12 or more missing
                dB[j] = (double) sum / count;
            else
                dB[j] = CalcUtil.missingVal;

        }

        return dB;
    }

    /*
     * @param dB -- double[30 ]
     * 
     * @return --5 smallest disturbance levels
     */
    public static Map getSmallDisturbanceLevel(double[] dB) {
        // create a map that key=dBIndex and value=dBValue.
        // create a duplicate array dBDup. Sort it.
        // take 5 smallest dBDup[i]. Then find its index and value from the dB.
        // Put them to the map
        Map<Integer, Double> dBSmall = new HashMap<Integer, Double>();

        double[] dBDup = new double[dB.length];
        for (int i = 0; i < dBDup.length; i++) {
            dBDup[i] = dB[i];
        }

        Arrays.sort(dBDup);

        double dupIndex = (int) CalcUtil.missingVal.doubleValue();

        // take 5 smallest dBDup
        for (int j = 0; j < 5; j++) {
            for (int i = 0; i < dB.length; i++) {
                if (dB[i] == dBDup[j] && i != dupIndex) { // for duplicated
                                                          // values

                    dBSmall.put(i, dB[i]);
                    dupIndex = i;
                    break;
                }
            }
        }

        return dBSmall;
    }

    /*
     * @param -- dBSmall, 5 set map
     * 
     * @param -- simpHrAvg, -- double[720]
     * 
     * @rturn -- quietLevelHourAvg, double[24]
     */
    public static double[] getQuietLevelHourAvg(Map<Integer, Double> dBSmall,
            double[] simpHrAvg) {
        if (dBSmall.entrySet().size() < 5)
            return simpHrAvg;

        double[] quietHrAvg = new double[24];
        Arrays.fill(quietHrAvg, CalcUtil.missingVal);
        int[] index = new int[5];
        double[] dB = new double[5];

        int k = 0;
        Iterator<?> iter = dBSmall.entrySet().iterator();
        while (iter.hasNext()) {
            @SuppressWarnings("unchecked")
            Map.Entry<Integer, Double> mEntry = (Map.Entry<Integer, Double>) iter
                    .next(); // sorted on key

            index[k] = mEntry.getKey();
            dB[k] = mEntry.getValue();

            k++;
        }

        // construct smallHrAvg array (24*5) from simpHrAvg (24*30)
        double[] smallHrAvg = new double[24 * 5];

        for (int j = 0; j < 5; j++) { // k=5
            int endOfArray = smallHrAvg.length;
            int endTime = (endOfArray > j * HOURS + HOURS) ? j * HOURS + HOURS
                    : endOfArray;

            for (int i = j * HOURS; i < endTime; i++) {
                smallHrAvg[i] = simpHrAvg[index[j] * HOURS + i % HOURS]; // 700
            }
        }

        for (int ihr = 0; ihr < HOURS; ihr++) {
            double sumAvg = 0;
            double sumWk = 0;
            double wk = 0;

            for (int jk = 0; jk < 5; jk++) {
                int ind = jk * HOURS + ihr;
                wk = (double) Math.pow(dB[jk], 2.0); // modified to match
                                                     // IDL code
                                                     // FMIQDCRT11_3hr.pro, line
                                                     // 103
                if (wk < 1.0) {
                    wk = 1.0;
                } else {
                    wk = 1.0 / wk;
                }

                if (smallHrAvg[ind] != CalcUtil.missingVal) {
                    sumAvg += wk * smallHrAvg[ind];
                    sumWk += wk;
                }
            }

            if (sumWk > 0)
                quietHrAvg[ihr] = sumAvg / sumWk;

        }

        return quietHrAvg;
    }

    /*
     * @param -- quietHrAvg, double[24]
     * 
     * @return -- shifted quietLevelHourAvg, double[24]
     */
    public static double[] getQHA(double[] quietHrAvg) {
        double[] QHA = new double[24];

        if (quietHrAvg.length != 24)
            return quietHrAvg;

        for (int ihr = 0; ihr < 24; ihr++) {
            QHA[ihr] = quietHrAvg[(ihr + 3) % 24];
        }

        return QHA;
    }

    /*
     * @return -- 24 element double array. Default fitting lengths.
     * (one for each hour of the 24 hour interval that ends at EPtime).
     */
    public static double[] getDefLength(String station, int epHour) {
        double[] defLength = new double[24];
        double lon = CalcUtil.getLongitude(station);
        int UTdiff = (int) Math.round(1440.0f * lon / 360.0f);
        int minute0 = epHour * MINUTES;

        for (int ihr = 0; ihr < HOURS; ihr++) {
            double sum = 0;

            for (int imin = 0; imin < MINUTES; imin++) {
                int curMin = (minute0 + ihr * MINUTES + imin) % 1440;
                int localMin = (curMin + UTdiff) % 1440;

                if (localMin >= 0 && localMin < 180)
                    sum += NIGHT_LENGTH;
                else if (localMin >= 180 && localMin < 360)
                    sum += DAWN_LENGTH;
                else if (localMin >= 360 && localMin < 1080)
                    sum += DAY_LENGTH;
                else if (localMin >= 1080 && localMin < 1260)
                    sum += DUSK_LENGTH;
                else if (localMin >= 1260 && localMin < 1440)
                    sum += NIGHT_LENGTH;
            }

            defLength[ihr] = sum / MINUTES;

        }

        return defLength;
    }

    /*
     * wraper function for a few functions in this class.
     * 
     * @param -- hHrAvgs, hourly average for H. double[720]
     * 
     * @param -- dHrAvgs, hourly average for D. double[720]
     * 
     * @return -- if hHrAvgs is first param, return hQdc; if dHrAvgs is first
     * param, return dQdc. double[1440]
     */
    public static double[] getHQdcOrDQdc(double[] hHrAvgs, double[] dHrAvgs) {
        double[] hQdc = null;

        double[] dB = CalcEach3hr.getDisturbanceLevel(hHrAvgs, dHrAvgs);

        @SuppressWarnings("unchecked")
        Map<Integer, Double> dBsmall = CalcEach3hr.getSmallDisturbanceLevel(dB);

        double[] quietHHrAvg = CalcEach3hr.getQuietLevelHourAvg(dBsmall,
                hHrAvgs);

        // added from FMIQDCRT11_3hr.pro
        for (int k = 0; k < quietHHrAvg.length; k++) {
            if (quietHHrAvg[k] == CalcUtil.missingVal) {
                quietHHrAvg[k] = CalcUtil.getMedian(quietHHrAvg);
            }
        }

        double[] qha = CalcEach3hr.getQHA(quietHHrAvg);

        hQdc = CalcEach1min.getHarmonicFit(qha);// [1440]

        return hQdc;
    }

}
