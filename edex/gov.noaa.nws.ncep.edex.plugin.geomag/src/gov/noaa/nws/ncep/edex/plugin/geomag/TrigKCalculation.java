package gov.noaa.nws.ncep.edex.plugin.geomag;

import gov.noaa.nws.ncep.common.dataplugin.geomag.GeoMagAvg;
import gov.noaa.nws.ncep.common.dataplugin.geomag.GeoMagK1min;
import gov.noaa.nws.ncep.common.dataplugin.geomag.GeoMagK3hr;
import gov.noaa.nws.ncep.common.dataplugin.geomag.GeoMagRecord;
import gov.noaa.nws.ncep.common.dataplugin.geomag.calculation.CalcEach1min;
import gov.noaa.nws.ncep.common.dataplugin.geomag.calculation.CalcEach3hr;
import gov.noaa.nws.ncep.common.dataplugin.geomag.calculation.CalcKp;
import gov.noaa.nws.ncep.common.dataplugin.geomag.calculation.CalcUtil;
import gov.noaa.nws.ncep.common.dataplugin.geomag.dao.GeoMagAvgDao;
import gov.noaa.nws.ncep.common.dataplugin.geomag.dao.GeoMagDao;
import gov.noaa.nws.ncep.common.dataplugin.geomag.dao.GeoMagK1minDao;
import gov.noaa.nws.ncep.common.dataplugin.geomag.dao.GeoMagK3hrDao;
import gov.noaa.nws.ncep.common.dataplugin.geomag.dao.GeoMagK3hrStateDao;
import gov.noaa.nws.ncep.common.dataplugin.geomag.request.DatabaseUtil;
import gov.noaa.nws.ncep.common.dataplugin.geomag.util.ReportGenerator;
import gov.noaa.nws.ncep.common.dataplugin.geomag.util.TestCondition;

import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataIntegrityViolationException;

import com.raytheon.uf.common.dataplugin.PluginException;
import com.raytheon.uf.common.dataplugin.message.DataURINotificationMessage;
import com.raytheon.uf.common.dataplugin.persist.IPersistable;
import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.common.datastorage.StorageException;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.edex.database.plugin.PluginFactory;

/**
 * This java class calculates magnetometer k index and related values.
 * 
 * <pre>
 * OFTWARE HISTORY
 *                   
 * date         Ticket#     Engineer    Description
 * -----------  ----------  ----------- --------------------------
 * 06/07/2013   #989        qzhou       Initial Creation, event driven
 * 03/18/2014   #1123       qzhou       Move some functions to common. Modified FillAvgTimeGap in the moved functions
 * 06/26/2014   #1136       qzhou       Calculate hourly average when min>=55 instead of min=59
 * 07/16/2014   R4078       sgurung     Modified method calcK3hr() to add states when a k3hr record is inserted
 * 12/23/2014   R5412       sgurung     Change float to double, add code changes related to "debug mode"
 * 06/08/2015   R8416       sgurung    Changed int[] to double[] for klimit
 * 10/07/2015   R11429     sgurung,jtravis  Replaced hard-coded missing value codes, replaced deprecated code
 * 01/05/2016   R14697     sgurung,jtravis  Add fix for duplicate hourly averages issue
 * 
 * 
 * </pre>
 * 
 * @author qzhou
 * @version 1
 * */

public class TrigKCalculation {

    private static final String GeoMag = "geomag";
    private static final int HOURS = 24;
    private static final int MINUTES = 60;
    private static final int HD_DATA_RANGE = 3;
    private static final int ITERATIONS = 5;
    private GeoMagDao dao;
    private double[] defLength = new double[HOURS];

    private final Log logger = LogFactory.getLog(getClass());

    public TrigKCalculation() {

    }

    /*
     * trigger
     */
    public void trig1min(Object obj) {

        if (!(obj instanceof DataURINotificationMessage)) {

            return;
        }

        DataURINotificationMessage uriMsg = (DataURINotificationMessage) obj;
        String[] dataUris = uriMsg.getDataURIs();

        // get geomag uri
        List<String> geomagUri = new ArrayList<String>();

        for (String dataURI : dataUris) {
            if (dataURI.contains("geomag"))
                geomagUri.add(dataURI);
        }

        if (geomagUri.size() == 0)
            return;

        String[] dataURIs = geomagUri.toArray(new String[geomagUri.size()]);

        // sort
        Arrays.sort(dataURIs);

        try {
            dao = (GeoMagDao) PluginFactory.getInstance().getPluginDao(GeoMag);
        } catch (PluginException e) {
            e.printStackTrace();
        }

        calcSimpleHourAvg(dataURIs);
        calcK(dataURIs);

    }

    /*
     * For hdf5
     */
    public IDataRecord[] getDataRecords(String uri) {
        IDataRecord[] dataRec = null;
        IDataStore dataStore = null;

        GeoMagRecord record = new GeoMagRecord(uri);
        if (record != null)
            dataStore = dao.getDataStore((IPersistable) record);

        try {
            dataRec = dataStore.retrieve(uri); // obs_time, compx...//size 7
        } catch (FileNotFoundException e1) {
            // e1.printStackTrace();
            System.out.println("This uri didn't find the records.");
        } catch (StorageException e1) {
            System.out
                    .println("This uri didn't find place to store the records.");

        }

        return dataRec;
    }

    /*
     * Input data of all source, output with higher priority source data
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public List<List> getBestObserv(List<?> dataList) {

        List<Double> comp1List = new ArrayList<Double>();
        List<Double> comp2List = new ArrayList<Double>();
        List<Integer> badPointList = new ArrayList<Integer>();
        List<Date> dateList = new ArrayList<Date>();
        List<Integer> sourceList = new ArrayList<Integer>();

        List<List> bestList = new ArrayList<List>();

        if (dataList != null) {
            for (int i = 0; i < dataList.size(); i++) {

                Object[] row = (Object[]) dataList.get(i);

                comp1List.add((Double) row[0]);
                comp2List.add((Double) row[1]);
                dateList.add((Date) row[2]);
                badPointList.add((Integer) row[3]);
                sourceList.add((Integer) row[4]);

            }

            DatabaseUtil.sort(dateList, sourceList, comp1List, comp2List,
                    badPointList);

            int count = 0;
            int size = dateList.size();

            /*
             * tempList combine all lists for the first 4 items. size=4 newList
             * holds tempLists ordered by source. size=3 bestList construct
             * newList with best source bestListFull filled time gaps
             */
            for (int i = 0; i < size; i = i + count) {
                count = 0;

                List tempList1 = new ArrayList();
                List tempList2 = new ArrayList();
                List tempList3 = new ArrayList();

                List<List> newList = new ArrayList<List>();
                // init 3
                newList.add(0, new ArrayList());
                newList.add(1, new ArrayList());
                newList.add(2, new ArrayList());

                tempList1.add(dateList.get(i));
                if (badPointList.get(i) != null && badPointList.get(i) != 0) {
                    tempList1.add(CalcUtil.missingVal);
                    tempList1.add(CalcUtil.missingVal);
                } else {
                    tempList1.add(comp1List.get(i));
                    tempList1.add(comp2List.get(i));
                }
                newList.set(sourceList.get(i) % 100 - 1, tempList1);
                count++;

                if (i + 1 < size
                        && dateList.get(i).compareTo(dateList.get(i + 1)) == 0) {

                    tempList2.add(dateList.get(i + 1));
                    if (badPointList.get(i + 1) != null
                            && badPointList.get(i + 1) != 0) {
                        tempList2.add(CalcUtil.missingVal);
                        tempList2.add(CalcUtil.missingVal);
                    } else {
                        tempList2.add(comp1List.get(i + 1));
                        tempList2.add(comp2List.get(i + 1));
                    }
                    newList.set(sourceList.get(i + 1) % 100 - 1, tempList2);
                    count++;
                }

                if (i + 2 < size
                        && dateList.get(i).compareTo(dateList.get(i + 2)) == 0) {

                    tempList3.add(dateList.get(i + 2));
                    if (badPointList.get(i + 2) != null
                            && badPointList.get(i + 2) != 0) {
                        tempList3.add(CalcUtil.missingVal);
                        tempList3.add(CalcUtil.missingVal);
                    } else {
                        tempList3.add(comp1List.get(i + 2));
                        tempList3.add(comp2List.get(i + 2));
                    }
                    newList.set(sourceList.get(i + 2) % 100 - 1, tempList3);
                    count++;
                }

                if (newList.get(2) == null || newList.get(2).isEmpty())
                    newList.remove(2);
                if (newList.get(1) == null || newList.get(1).isEmpty())
                    newList.remove(1);
                if (newList.get(0) == null || newList.get(0).isEmpty())
                    newList.remove(0);

                // Now only check if comp2 (...get(2)) is MISSING_VAL. Could
                // check both
                if (newList.get(0).get(2) != null
                        && (Double) newList.get(0).get(2) != CalcUtil.missingVal) {
                    bestList.add(newList.get(0));
                } else if (newList.size() > 1
                        && (Double) newList.get(0).get(2) == CalcUtil.missingVal
                        && i + 1 < size) {
                    // if date i = date(i+1) && comp1 (i+1) != missing
                    if ((Date) newList.get(0).get(1) == (Date) newList.get(1)
                            .get(1)
                            && newList.get(1).get(2) != null
                            && (Double) newList.get(1).get(2) != CalcUtil.missingVal) {
                        bestList.add(newList.get(1));
                    } else if (newList.size() > 2
                            && (Double) newList.get(1).get(2) == CalcUtil.missingVal
                            && i + 2 < size) {
                        if ((Date) newList.get(0).get(1) == (Date) newList.get(
                                2).get(1)
                                && (Double) newList.get(2).get(2) != CalcUtil.missingVal) {
                            bestList.add(newList.get(2));
                        } else {
                            bestList.add(newList.get(0));
                        }
                    }
                }
            }
        }

        return bestList;
    }

    /*
     * fill time tag gaps, return fullBestList
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public List<List> fillHDTimeGaps(List<List> bestList) {
        List<List> fullBestList = new ArrayList<List>();

        // fill missing in the beginning
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.setTimeInMillis(((Date) bestList.get(0).get(0)).getTime());

        int min0 = cal.get(Calendar.MINUTE);

        if (min0 != 0) {
            for (int k = 0; k < min0; k++) {
                List newList2 = new ArrayList(); // eq. newList

                Calendar dateNew = (Calendar) cal.clone();
                dateNew.set(Calendar.MINUTE, k);

                newList2.add(dateNew.getTime());
                newList2.add(CalcUtil.missingVal);
                newList2.add(CalcUtil.missingVal);
                fullBestList.add(newList2);

            }
        }

        // fill missing in the middle
        for (int j = 0; j < bestList.size(); j++) { // i=0 first non missing
                                                    // data

            Date date0 = (Date) bestList.get(j).get(0);// dateList.get(i);
            fullBestList.add(bestList.get(j));

            if (j + 1 < bestList.size()) {
                Date date1 = (Date) bestList.get(j + 1).get(0);// dateList.get(i+1);
                int diffMin = (int) (date1.getTime() - date0.getTime())
                        / (60 * 1000);

                if (diffMin != 1) {
                    for (int k = 0; k < diffMin - 1; k++) {
                        List newList2 = new ArrayList(); // eq. newList

                        newList2.add(new Date(date0.getTime() + 60 * 1000
                                * (k + 1)));
                        newList2.add(CalcUtil.missingVal);
                        newList2.add(CalcUtil.missingVal);
                        fullBestList.add(newList2);

                    }
                }
            }
        }

        return fullBestList;
    }

    /*
     * when uri time is 35 min past the hour, calculate the averages and write
     * to geomag_houravg
     */
    public void calcSimpleHourAvg(String[] dataURIs) {

        if (dao != null && dataURIs != null) {
            for (String dataURI : dataURIs) {
                String stationCode = CalcUtil.getStationFromUri(dataURI);

                Calendar cal = Calendar.getInstance();
                cal.clear();

                try {

                    cal.setTimeInMillis(CalcUtil.getTimeFromUri(dataURI)
                            .getTime());

                } catch (ParseException e) {
                    e.printStackTrace();
                }

                int currentMinute = cal.get(Calendar.MINUTE);

                List<?> dataList = null;

                // Ideally we want to calculate hourly average for each min=59
                // ingest. But some stations may not have data available until
                // minute 23:59.
                // Calculating hourly average when min>=35 will not miss any
                // average data.
                if (cal.get(Calendar.MINUTE) >= 35)
                    dataList = DatabaseUtil.retrieveUriForAvg(dao, dataURI,
                            cal.getTime());
                else
                    continue;

                if (dataList != null && dataList.size() != 0) {
                    List<List> bestList = getBestObserv(dataList);

                    double[] hrAvg = CalcEach3hr.getSimpleHourAvg(bestList);

                    GeoMagAvg recAvg = new GeoMagAvg();

                    // look the avg table to see if the avg already exists
                    cal.set(Calendar.MINUTE, 30);
                    GeoMagAvg geomagAvg = DatabaseUtil.retrieveHourlyAvg(
                            stationCode, cal.getTime());

                    if (geomagAvg != null) {
                        recAvg.setId(geomagAvg.getId());
                    }

                    recAvg.setAvgTime(cal.getTime());
                    recAvg.setInsertTime(Calendar.getInstance().getTime());
                    recAvg.setStationCode(stationCode);
                    recAvg.sethHrAvg(hrAvg[0]);
                    recAvg.setdHrAvg(hrAvg[1]);
                    recAvg.setLastMinuteUsed(currentMinute);

                    GeoMagAvgDao avgDao = new GeoMagAvgDao();

                    try {

                        avgDao.persist(recAvg);
                    } catch (DataIntegrityViolationException e) {
                        logger.info("GEOMAG ERROR: DataIntegrityViolationException occurred while persisting geomag hourly average record.");
                        e.printStackTrace();

                        geomagAvg = DatabaseUtil.retrieveHourlyAvg(stationCode,
                                cal.getTime());

                        if (geomagAvg != null) {
                            int lastMinuteUsed = geomagAvg.getLastMinuteUsed();

                            if (lastMinuteUsed < recAvg.getLastMinuteUsed()) {
                                recAvg.setId(geomagAvg.getId());
                                avgDao.persist(recAvg);
                            }
                        }

                    }
                }
            }
        }

    }

    /*
     * Write to geomag_k1min
     */
    public void calcK(String[] dataURIs) {

        TestCondition tc = null;
        ReportGenerator rg = null;
        Hashtable<String, String> testComponents = new Hashtable<String, String>();

        boolean debug = false;

        if (dataURIs != null) {
            for (String dataURI : dataURIs) {

                Calendar timeBy3 = Calendar.getInstance();
                timeBy3.clear();

                try {
                    timeBy3.setTimeInMillis((CalcUtil.getTimeFromUri(dataURI)
                            .getTime()));
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                int hour = timeBy3.get(Calendar.HOUR_OF_DAY);
                int min = timeBy3.get(Calendar.MINUTE);

                String stationCode = CalcUtil.getStationFromUri(dataURI);
                String source = CalcUtil.getSourceFromUri(dataURI);

                debug = CalcUtil.isDebugModeRequired(dataURI, hour, min);

                if (debug) {
                    testComponents = CalcUtil.getTestComponents(dataURI);

                    // define and initialize the test condition
                    // parameters
                    tc = new TestCondition();
                    tc.setCal(timeBy3.getTimeInMillis());
                    tc.setHourOfInterest(Integer.valueOf(
                            testComponents.get("HOUR")).intValue());
                    tc.setMinuteOfInterest(Integer.valueOf(
                            testComponents.get("MINUTE")).intValue());
                    tc.setStationCd(stationCode);
                    tc.setTestNumber(testComponents.get("TEST_NUMBER"));

                    try {
                        rg = new ReportGenerator(tc);
                    } catch (Exception e) {
                        e.printStackTrace();
                        debug = false;
                    }

                }

                /*
                 * Read average
                 */
                Date spTime = CalcUtil.getSPTime(timeBy3.getTime());

                List<GeoMagAvg> dataList = null;

                dataList = DatabaseUtil.retrieveUriBy3hr(dataURI,
                        CalcUtil.getSPTime(timeBy3.getTime()));

                if (debug) {
                    rg.generateDataListReport(0, dataList);
                }

                // dataList size (avg) < 24, can't calculate dB[j]
                if (dataList.size() <= HOURS)
                    continue;

                if (dataList != null && dataList.size() >= 5) {
                    List<Date> dateListFinal = new ArrayList<Date>();
                    List<Double> hHrAvgListFinal = new ArrayList<Double>();
                    List<Double> dHrAvgListFinal = new ArrayList<Double>();

                    DatabaseUtil.fillHrAvgTimeGaps(dataList, dateListFinal,
                            hHrAvgListFinal, dHrAvgListFinal, spTime);

                    double[] hHrAvgs = CalcUtil.toDoubleArray(hHrAvgListFinal);
                    double[] dHrAvgs = CalcUtil.toDoubleArray(dHrAvgListFinal);

                    double[] dB = CalcEach3hr.getDisturbanceLevel(hHrAvgs,
                            dHrAvgs);

                    @SuppressWarnings("unchecked")
                    Map<Integer, Double> dBsmall = CalcEach3hr
                            .getSmallDisturbanceLevel(dB);

                    if (debug) {
                        rg.generateReport(ReportGenerator.ReportType.HHRAVGS,
                                0, hHrAvgs);
                        rg.generateReport(ReportGenerator.ReportType.DHRAVGS,
                                0, dHrAvgs);
                        rg.generateReport(ReportGenerator.ReportType.DB, 0, dB);
                        Set<Integer> qDayKeys = dBsmall.keySet();
                        double[] qDays = new double[qDayKeys.size()];
                        int i = 0;
                        for (Integer val : qDayKeys) {
                            qDays[i] = val;
                            i++;
                        }
                        rg.generateReport(ReportGenerator.ReportType.QDAYS, 0,
                                qDays);
                    }

                    double[] qhravg_h = CalcEach3hr.getQuietLevelHourAvg(
                            dBsmall, hHrAvgs);
                    double[] qhravg_d = CalcEach3hr.getQuietLevelHourAvg(
                            dBsmall, dHrAvgs);

                    // added from FMIQDCRT11_3hr.pro
                    for (int k = 0; k < qhravg_h.length; k++) {

                        if (qhravg_h[k] == CalcUtil.missingVal) {
                            qhravg_h[k] = CalcUtil.getMedian(qhravg_h);
                        }
                        if (qhravg_d[k] == CalcUtil.missingVal) {
                            qhravg_d[k] = CalcUtil.getMedian(qhravg_d);
                        }
                    }

                    double[] qhavgShifted = CalcEach3hr.getQHA(qhravg_h);
                    double[] qdavgShifted = CalcEach3hr.getQHA(qhravg_d);

                    if (debug) {
                        rg.generateReport(ReportGenerator.ReportType.QHRAVGH,
                                0, qhravg_h);
                        rg.generateReport(ReportGenerator.ReportType.QHRAVGD,
                                0, qhravg_d);
                        rg.generateReport(
                                ReportGenerator.ReportType.QHAVG_SHIFTED, 0,
                                qhavgShifted);
                        rg.generateReport(
                                ReportGenerator.ReportType.QDAVG_SHIFTED, 0,
                                qdavgShifted);
                    }

                    double[] hQdc = CalcEach1min.getHarmonicFit(qhavgShifted);// [1440]
                    double[] dQdc = CalcEach1min.getHarmonicFit(qdavgShifted);

                    double[] qhaQdc = CalcEach1min.getQHAQDC(hQdc);// [1440]
                    double[] qdaQdc = CalcEach1min.getQHAQDC(dQdc);

                    if (debug) {
                        rg.generateReport(ReportGenerator.ReportType.QHAQDC, 0,
                                qhaQdc);
                        rg.generateReport(ReportGenerator.ReportType.QDAQDC, 0,
                                qdaQdc);
                    }

                    /*
                     * Read H and D
                     */
                    Map<String, List<double[]>> kIndexMap = new HashMap<String, List<double[]>>();

                    Calendar timeBy1 = Calendar.getInstance();
                    timeBy1.clear();

                    try {

                        timeBy1.setTimeInMillis(CalcUtil
                                .getTimeFromUri(dataURI).getTime());

                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    // Date epTime = CalcUtil.getEPTime(timeBy1.getTime());\
                    Calendar epTime = Calendar.getInstance();
                    epTime.clear();
                    epTime.setTimeInMillis(CalcUtil
                            .getEPTime(timeBy1.getTime()).getTime());
                    int epHour = epTime.get(Calendar.HOUR_OF_DAY);

                    /*
                     * change epTime to current time
                     */
                    List<?> hdDataList = DatabaseUtil.retrieveUriForK1min(dao,
                            dataURI, timeBy1.getTime());

                    if (hdDataList != null && hdDataList.size() != 0) {
                        // if dataList <= 1440, can't calculate k-index
                        if (hdDataList.size() <= HOURS * MINUTES)
                            continue;

                        // gest best observation data
                        List<List> bestList = getBestObserv(hdDataList);
                        if (bestList.size() <= HOURS * MINUTES)
                            continue;

                        List<List> bestListFull = fillHDTimeGaps(bestList);

                        // get hdata, ddata
                        double[] hdata = new double[HD_DATA_RANGE * HOURS
                                * MINUTES];
                        double[] ddata = new double[HD_DATA_RANGE * HOURS
                                * MINUTES];

                        Arrays.fill(hdata, CalcUtil.missingVal);
                        Arrays.fill(ddata, CalcUtil.missingVal);

                        for (int i = 0; i < bestListFull.size(); i++) {
                            List<Double> list = (List<Double>) bestListFull
                                    .get(i);
                            if (list != null && !list.isEmpty()) {
                                hdata[i] = list.get(1);
                                ddata[i] = list.get(2);
                            }
                        }

                        defLength = CalcEach3hr.getDefLength(stationCode,
                                epHour);

                        double[] hhdata = CalcEach1min.fillGaps(hdata);
                        double[] dddata = CalcEach1min.fillGaps(ddata);

                        if (debug) {
                            rg.generateReport(ReportGenerator.ReportType.HDATA,
                                    0, hdata);
                            rg.generateReport(ReportGenerator.ReportType.DDATA,
                                    0, ddata);
                            rg.generateReport(
                                    ReportGenerator.ReportType.HHDATA, 0,
                                    hhdata);
                            rg.generateReport(
                                    ReportGenerator.ReportType.DDDATA, 0,
                                    dddata);
                        }

                        int currTimeIndex = CalcEach1min.getCurrTimeIndex(hour,
                                min, epHour);

                        double[] hhdataEx = CalcEach1min.getExtrapolation(
                                hhdata, qhaQdc, currTimeIndex);
                        double[] dddataEx = CalcEach1min.getExtrapolation(
                                dddata, qdaQdc, currTimeIndex);

                        if (debug) {

                            rg.generateReport(
                                    ReportGenerator.ReportType.CURRTIMEINDEX,
                                    0, currTimeIndex);
                            rg.generateReport(ReportGenerator.ReportType.HQDC0,
                                    0, hQdc);
                            rg.generateReport(ReportGenerator.ReportType.DQDC0,
                                    0, dQdc);
                            rg.generateReport(
                                    ReportGenerator.ReportType.HDATAEX, 0,
                                    hhdataEx);
                            rg.generateReport(
                                    ReportGenerator.ReportType.DDATAEX, 0,
                                    dddataEx);
                        }

                        double[] hDev = CalcEach1min.getDev(hhdataEx, hQdc);// [1440]
                        double[] dDev = CalcEach1min.getDev(dddataEx, dQdc);

                        if (debug) {
                            rg.generateReport(ReportGenerator.ReportType.HDEV,
                                    0, hDev);
                            rg.generateReport(ReportGenerator.ReportType.DDEV,
                                    0, dDev);
                        }

                        // already considered missing in getDev

                        double[] kLimit = CalcUtil.getKLimit(stationCode);

                        int missingFlag = 0;
                        List<double[]> kList = CalcEach1min.getKIndex(hDev,
                                dDev, kLimit, missingFlag);// [8]

                        if (debug) {
                            rg.generateReport(
                                    ReportGenerator.ReportType.KINDEX, 0, kList);
                        }

                        double[] kIndex = kList.get(0);
                        double[] kGamma = kList.get(1);
                        double[] hGamma = kList.get(3);
                        double[] dGamma = kList.get(5);

                        double[] kLength = CalcUtil.geKLength();// [8]
                        double[] fitLength = CalcEach1min.getFitLength(
                                defLength, kIndex, kLength);// [24]

                        double[] hcA = CalcEach1min.getCentHourAvg(hhdataEx,
                                fitLength);// middle [24]
                        double[] dcA = CalcEach1min.getCentHourAvg(dddataEx,
                                fitLength);

                        if (debug) {
                            rg.generateReport(
                                    ReportGenerator.ReportType.FITLENGTH, 0,
                                    fitLength);
                            rg.generateReport(ReportGenerator.ReportType.HA1,
                                    0, hcA);
                            rg.generateReport(ReportGenerator.ReportType.DA1,
                                    0, dcA);
                        }

                        hcA = CalcEach1min.adjustHrCentAvg(hcA, qhavgShifted,
                                hGamma, kLimit);
                        dcA = CalcEach1min.adjustHrCentAvg(dcA, qdavgShifted,
                                dGamma, kLimit);

                        if (debug) {
                            rg.generateReport(ReportGenerator.ReportType.HA2,
                                    0, hcA);
                            rg.generateReport(ReportGenerator.ReportType.DA2,
                                    0, dcA);
                        }

                        // Harmonic Fit to derive the qdc
                        int foundMiss = 0;
                        for (int i = 0; i < hcA.length; i++) {
                            if (hcA[i] == CalcUtil.missingVal) {
                                foundMiss = 1;
                                break;
                            }
                        }
                        if (foundMiss == 0)
                            hQdc = CalcEach1min.getHarmonicFit(hcA);

                        foundMiss = 0;
                        for (int i = 0; i < dcA.length; i++) {
                            if (dcA[i] == CalcUtil.missingVal) {
                                foundMiss = 1;
                                break;
                            }
                        }
                        if (foundMiss == 0)
                            dQdc = CalcEach1min.getHarmonicFit(dcA);

                        if (debug) {
                            rg.generateReport(ReportGenerator.ReportType.HQDC,
                                    0, hQdc);
                            rg.generateReport(ReportGenerator.ReportType.DQDC,
                                    0, dQdc);
                        }

                        /*
                         * Do a few iterations. check for convergence of k_index
                         * and exit loop Done before ITERATIONS if you see two
                         * passes with identical values for k_index
                         */
                        double[] last_kindex = new double[8];
                        Arrays.fill(last_kindex, -1);

                        /*
                         * Check for convergence of k_index and exit loop before
                         * ITERATIONS are done if you see two passes with
                         * identical values for k_index
                         */
                        for (int num = 0; num < ITERATIONS; num++) {

                            double kchange = 0;
                            hDev = CalcEach1min.getDev(hhdataEx, hQdc);
                            dDev = CalcEach1min.getDev(dddataEx, dQdc);

                            if (debug) {
                                rg.generateReport(
                                        ReportGenerator.ReportType.HDEV,
                                        num + 1, hDev);
                                rg.generateReport(
                                        ReportGenerator.ReportType.DDEV,
                                        num + 1, dDev);
                            }

                            kList = CalcEach1min.getKIndex(hDev, dDev, kLimit,
                                    missingFlag);
                            kIndex = kList.get(0);
                            kGamma = kList.get(1);
                            hGamma = kList.get(3);
                            dGamma = kList.get(5);

                            if (debug) {
                                rg.generateReport(
                                        ReportGenerator.ReportType.KINDEX,
                                        num + 1, kList);
                            }

                            // Check for convergence of k_index
                            if (kIndex.length == 8 && last_kindex.length == 8)
                                for (int i = 0; i < last_kindex.length; i++) {
                                    kchange = kchange
                                            + Math.abs(kIndex[i]
                                                    - last_kindex[i]);
                                }

                            if (kchange == 0)
                                break;

                            fitLength = CalcEach1min.getFitLength(defLength,
                                    kIndex, kLength);

                            hcA = CalcEach1min.getCentHourAvg(hhdataEx,
                                    fitLength);
                            dcA = CalcEach1min.getCentHourAvg(dddataEx,
                                    fitLength);

                            if (debug) {
                                rg.generateReport(
                                        ReportGenerator.ReportType.FITLENGTH,
                                        num + 1, fitLength);
                                rg.generateReport(
                                        ReportGenerator.ReportType.HA1,
                                        num + 1, hcA);
                                rg.generateReport(
                                        ReportGenerator.ReportType.DA1,
                                        num + 1, dcA);
                            }

                            hcA = CalcEach1min.adjustHrCentAvg(hcA,
                                    qhavgShifted, hGamma, kLimit);
                            dcA = CalcEach1min.adjustHrCentAvg(dcA,
                                    qdavgShifted, dGamma, kLimit);

                            if (debug) {
                                rg.generateReport(
                                        ReportGenerator.ReportType.HA2,
                                        num + 1, hcA);
                                rg.generateReport(
                                        ReportGenerator.ReportType.DA2,
                                        num + 1, dcA);
                            }

                            // Harmonic Fit to derive the qdc
                            foundMiss = 0;
                            for (int i = 0; i < hcA.length; i++) {
                                if (hcA[i] == CalcUtil.missingVal) {
                                    foundMiss = 1;
                                    break;
                                }
                            }
                            if (foundMiss == 0)
                                hQdc = CalcEach1min.getHarmonicFit(hcA);

                            foundMiss = 0;
                            for (int i = 0; i < dcA.length; i++) {
                                if (dcA[i] == CalcUtil.missingVal) {
                                    foundMiss = 1;
                                    break;
                                }
                            }
                            if (foundMiss == 0)
                                dQdc = CalcEach1min.getHarmonicFit(dcA);

                            last_kindex = kIndex.clone();

                            if (debug) {
                                rg.generateReport(
                                        ReportGenerator.ReportType.HQDC,
                                        num + 1, hQdc);
                                rg.generateReport(
                                        ReportGenerator.ReportType.DQDC,
                                        num + 1, dQdc);
                            }

                        } // END LOOP

                        // Now do the calculation using the original data
                        // (hdata, ddata)
                        hDev = CalcEach1min.getDev(hdata, hQdc);// [1440]
                        dDev = CalcEach1min.getDev(ddata, dQdc);

                        if (debug) {
                            rg.generateReport(
                                    ReportGenerator.ReportType.HDEV_FINAL, 0,
                                    hDev);
                            rg.generateReport(
                                    ReportGenerator.ReportType.DDEV_FINAL, 0,
                                    dDev);
                        }

                        // Calculate the K-index and gamma values
                        kList = CalcEach1min.getKIndex(hDev, dDev, kLimit,
                                missingFlag);
                        kIndex = kList.get(0);
                        kGamma = kList.get(1);

                        double[] hkIndex = kList.get(2);
                        hGamma = kList.get(3);
                        double[] dkIndex = kList.get(4);
                        dGamma = kList.get(5);

                        if (debug) {
                            rg.generateReport(
                                    ReportGenerator.ReportType.KINDEX_FINAL, 0,
                                    kList);
                        }

                        int lastHCount = 0;
                        int lastDCount = 0;

                        // Count the number of non-missing points for the last 3
                        // hours (180 values)
                        for (int i = 2700; i < 2880; i++) {
                            if (hdata[i] != CalcUtil.missingVal)
                                lastHCount++;
                            if (ddata[i] != CalcUtil.missingVal)
                                lastDCount++;
                        }

                        double[] count = new double[2];
                        count[0] = lastHCount;
                        count[1] = lastDCount;
                        kList.add(6, count);
                        kIndexMap.put(stationCode + source, kList);

                        double ks = 0;
                        try {
                            ks = CalcKp.getKs(stationCode, (int) kIndex[7],
                                    timeBy1.getTime()); // 7 is last point
                            // kIndex
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        int a_est = CalcKp
                                .getAest(stationCode, (int) kIndex[7]);

                        // 1 min
                        int kest_index = (int) kIndex[7];
                        double kest_real = CalcKp.getKest(stationCode,
                                (int) kIndex[7], kGamma[7]);
                        double kest_gamma = kGamma[7];

                        double hgamma = hGamma[7];
                        double hk_real = CalcKp.getKest(stationCode,
                                (int) hkIndex[7], hgamma);

                        double dgamma = dGamma[7];
                        double dk_real = CalcKp.getKest(stationCode,
                                (int) dkIndex[7], dgamma);

                        int hkindex = (int) hkIndex[7];
                        int dkindex = (int) dkIndex[7];
                        int countH = (int) count[0];
                        int countD = (int) count[1];
                        double ksArray = ks;
                        int aestArray = a_est;

                        GeoMagK1min recK1min = new GeoMagK1min();

                        List<GeoMagK1min> k1minList = DatabaseUtil
                                .retrieveSingleK1min(dataURI, timeBy1.getTime());

                        if (k1minList != null && k1minList.size() != 0) {
                            for (int i = 0; i < k1minList.size(); i++) { // 1
                                GeoMagK1min row = k1minList.get(i);

                                int id = (Integer) row.getId();
                                if (id != 0)
                                    recK1min.setId(id);
                            }
                        }

                        recK1min.setRefTime(timeBy1.getTime());
                        recK1min.setLastUpdate(Calendar.getInstance().getTime());
                        recK1min.setStationCode(stationCode);
                        recK1min.setKestIndex(kest_index);
                        recK1min.setKestReal(kest_real);
                        recK1min.setKestGamma(kest_gamma);
                        recK1min.setHkIndex(hkindex);
                        recK1min.setHkReal(hk_real);
                        recK1min.setHkGamma(hgamma);
                        recK1min.setDkIndex(dkindex);
                        recK1min.setDkReal(dk_real);
                        recK1min.setDkGamma(dgamma);
                        recK1min.setKs(ksArray);
                        recK1min.setAest(aestArray);
                        recK1min.sethCount(countH);
                        recK1min.setdCount(countD);

                        GeoMagK1minDao k1minDao = new GeoMagK1minDao();
                        k1minDao.persist(recK1min);

                        calcK3h(dataURI, kest_index, kest_real, kest_gamma);

                    } // end of for dataURI
                }
            }
        }
    }

    /*
     * write to geomag_k3hr
     */
    public void calcK3h(String dataURI, int kest_index, double kest_real,
            double kest_gamma) {
        List<Integer> idDb = new ArrayList<Integer>();
        List<Date> dateDb = new ArrayList<Date>();
        List<Integer> kIndexDb = new ArrayList<Integer>();
        List<Double> kGammaDb = new ArrayList<Double>();
        List<Integer> kestIndexDb = new ArrayList<Integer>();

        int aRun = 0;

        String stationCode = CalcUtil.getStationFromUri(dataURI);

        Calendar currTime = Calendar.getInstance();
        currTime.clear();

        try {
            currTime.setTimeInMillis(CalcUtil.getTimeFromUri(dataURI).getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }

        int hour = currTime.get(Calendar.HOUR_OF_DAY);
        int min = currTime.get(Calendar.MINUTE);
        Date epTime = CalcUtil.getEPTime(currTime.getTime());

        GeoMagK3hr recK3hr = new GeoMagK3hr();

        List<GeoMagK3hr> k3hrList = DatabaseUtil.retrieveUriForK3hr(dataURI,
                epTime); // epTime not in the list

        if (k3hrList != null && k3hrList.size() != 0) {

            for (int i = 0; i < k3hrList.size(); i++) {

                GeoMagK3hr row = (GeoMagK3hr) k3hrList.get(i);

                dateDb.add(row.getRefTime());
                idDb.add(row.getId());
                kIndexDb.add(row.getKIndex());
                kGammaDb.add(row.getKGamma());
                kestIndexDb.add(row.getKestIndex());

            }

            DatabaseUtil.sort(dateDb, idDb, kIndexDb, kGammaDb, kestIndexDb);

        }

        List<GeoMagK3hr> k3hrAtPoint = DatabaseUtil.retrieveSingleK3hr(dataURI,
                epTime);

        if (k3hrAtPoint == null || k3hrAtPoint.size() == 0) {

            // calculate aRunning, aFinalRunning
            // only need first 7 k
            int sum = 0;
            for (int k = 0; k < kestIndexDb.size(); k++) {
                int a_est = CalcKp.getAest(stationCode, kestIndexDb.get(k));
                sum += a_est;
            }
            sum += CalcKp.getAest(stationCode, kest_index);
            aRun = (int) sum / (kestIndexDb.size() + 1);

            recK3hr.setRefTime(epTime);
            recK3hr.setLastUpdate(Calendar.getInstance().getTime());
            recK3hr.setStationCode(stationCode);
            recK3hr.setKestIndex(kest_index);
            recK3hr.setKestReal(kest_real);
            recK3hr.setKestGamma(kest_gamma);
            recK3hr.setARunning(aRun);

            GeoMagK3hrDao k3hrDao = new GeoMagK3hrDao();
            k3hrDao.persist(recK3hr);

            if (recK3hr.getId() != null) {
                GeoMagK3hrStateDao k3hrStateDao = new GeoMagK3hrStateDao();
                k3hrStateDao.updateStates(recK3hr.getId(), stationCode, epTime);
            }
        }

        else {
            GeoMagK3hr row = (GeoMagK3hr) k3hrAtPoint.get(0);
            int idCurr = row.getId();
            int kIndexCurr = row.getKIndex();
            double kGammaCurr = row.getKGamma();
            double kRealCurr = row.getKReal();
            int aFinalRunCurr = row.getAFinalRunning();
            int manualCurr = row.getIsManual();

            if ((hour + 1) % 3 == 0 && (min + 1) % 60 == 0) {

                // calculate aFinalRunning, aFinalRunning
                int sumEnd = 0;
                for (int k = 0; k < kIndexDb.size(); k++) {
                    int a_est = CalcKp.getAest(stationCode, kIndexDb.get(k));
                    sumEnd += a_est;
                }

                if (manualCurr == 0)
                    sumEnd += CalcKp.getAest(stationCode, kest_index);
                else
                    sumEnd += CalcKp.getAest(stationCode, kIndexCurr);

                int aFinalRun = (int) sumEnd / (kIndexDb.size() + 1);

                recK3hr.setAFinalRunning(aFinalRun);
                recK3hr.setIsManual(manualCurr);

                if (manualCurr == 0) {
                    recK3hr.setKIndex(kest_index);
                    recK3hr.setKReal(kest_real);
                    recK3hr.setKGamma(kest_gamma);
                } else {

                    recK3hr.setKIndex(kIndexCurr);
                    recK3hr.setKReal(kRealCurr);
                    recK3hr.setKGamma(kGammaCurr);
                }

            } else {

                recK3hr.setKIndex(kIndexCurr);
                recK3hr.setKReal(kRealCurr);
                recK3hr.setKGamma(kGammaCurr);
                recK3hr.setAFinalRunning(aFinalRunCurr);
                recK3hr.setIsManual(manualCurr);
            }

            // calculate aRunning, aFinalRunning
            // only need first 7 k
            int sum = 0;
            for (int k = 0; k < kestIndexDb.size(); k++) {
                int a_est = CalcKp.getAest(stationCode, kestIndexDb.get(k));
                sum += a_est;
            }
            sum += CalcKp.getAest(stationCode, kest_index);
            aRun = (int) sum / (kestIndexDb.size() + 1);

            if (idCurr != 0)
                recK3hr.setId(idCurr);
            recK3hr.setRefTime(epTime);
            recK3hr.setLastUpdate(Calendar.getInstance().getTime());
            recK3hr.setStationCode(stationCode);
            recK3hr.setKestIndex(kest_index);
            recK3hr.setKestReal(kest_real);
            recK3hr.setKestGamma(kest_gamma);
            recK3hr.setARunning(aRun);

            GeoMagK3hrDao k3hrDao = new GeoMagK3hrDao();
            k3hrDao.persist(recK3hr);

            if (recK3hr.getId() != null) {
                GeoMagK3hrStateDao k3hrStateDao = new GeoMagK3hrStateDao();
                k3hrStateDao.updateStates(recK3hr.getId(), stationCode, epTime);
            }
        }

    }

}
