package gov.noaa.nws.ncep.edex.plugin.soundingrequest.handler;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.measure.UnitConverter;

import org.locationtech.jts.geom.Coordinate;

import com.raytheon.edex.plugin.bufrua.dao.BufrUADao;
import com.raytheon.uf.common.dataplugin.bufrua.UAObs;
import com.raytheon.uf.common.datastorage.DataStoreFactory;
import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.common.datastorage.Request;
import com.raytheon.uf.common.datastorage.StorageException;
import com.raytheon.uf.common.datastorage.records.FloatDataRecord;
import com.raytheon.uf.common.datastorage.records.IntegerDataRecord;
import com.raytheon.uf.common.pointdata.PointDataContainer;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.database.DataAccessLayerException;
import com.raytheon.uf.edex.database.dao.CoreDao;
import com.raytheon.uf.edex.database.dao.DaoConfig;
import com.raytheon.uf.edex.pointdata.PointDataQuery;

import gov.noaa.nws.ncep.common.dataplugin.ncuair.NcUairMaxWind;
import gov.noaa.nws.ncep.common.dataplugin.ncuair.NcUairObsLevels;
import gov.noaa.nws.ncep.common.dataplugin.ncuair.NcUairRecord;
import gov.noaa.nws.ncep.common.dataplugin.ncuair.NcUairTropopause;
import gov.noaa.nws.ncep.common.dataplugin.ncuair.dao.NcUairToRecord;
import gov.noaa.nws.ncep.common.dataplugin.soundingrequest.SoundingServiceRequest;
import gov.noaa.nws.ncep.common.dataplugin.soundingrequest.SoundingServiceRequest.SoundingType;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingCube;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingProfile;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingProfile.ObsSndType;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingProfile.SndQueryKeyType;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingStnInfo;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingStnInfoCollection;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingTimeLines;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingTools;
import si.uom.SI;
import systems.uom.common.USCustomary;

/**
 * 
 * This java class performs the observed sounding data query functions. This
 * code has been developed by the SIB for use in the AWIPS2 system.
 * 
 * <pre>
 *  SOFTWARE HISTORY
 *  
 *  Date         Ticket# Engineer   Description
 *  ------------ ------- ---------- -----------
 *  Sep 13, 2010 301     Chin Chen  Initial coding
 *  10/2010      301     T. Lee     Checked missing value
 *  Nov 05, 2010 301     Chin Chen  Update to support uairSnd query to all data
 *                                  types, except ALLDATA
 *  Dec 16, 2010 301     Chin Chen  add support of BUFRUA observed sounding data
 *  Sep 14, 2011 457     S. Gurung  Renamed h5 to nc
 *  Oct 20, 2011         S. Gurung  Added ncuair changes related to replacing
 *                                  slat/slon/selv with location of type
 *                                  SurfaceObsLocation
 *  Nov 2011             Chin Chen  changed Ncuair table query algorithm for
 *                                  performance improvement
 *  Jan 05, 2012         S. Gurung  Removed references to UAIR (performed
 *                                  cleanup)
 *  Feb 28, 2012         Chin Chen  modify several sounding query algorithms for
 *                                  better performance
 *  Jul 19, 2013 1992    bsteffen   Remove redundant time columns from bufrua.
 *  Aug 30, 2013 2298    rjpeter    Make getPluginName abstract
 *  June, 2014           Chin Chen  Retrieved observed sounding with reftime
 *  Oct 03, 2014         B. Hebbard Performance improvement:  getObservedSndNcUairDataGeneric( )
 *                                  limits DB param set to retrieve, in cases where possible
 *                                  (mandatory level and no PW-for-full-sounding)
 *  05/20/201    RM#8306 Chin Chen  eliminate NSHARP dependence on uEngine.
 *                                  Copy whole file ObservedSoundingQuery.java from uEngine project to this serverRequestService project
 *                                  "refactor" and clean up unused code for this ticket.
 *  07/02/2015   RM#8107 Chin Chen   change lat/lon data type from double to float to reflect its data type changes starting 14.4.1 
 *  07/21/2015   RM#9173 Chin Chen   Clean up NcSoundingQuery and Obsolete NcSoundingQuery2 and MergeSounging2
 *  04/19/2016   #17875  Chin Chen   change TimeStamp to Date while querying sounding station info + clean up
 *  07/19/2016   5736    bsteffen   Handle prSigW data.
 *  09/29/2016   RM15953 R.Reynolds  Added capability for wind interpolation
 *  03/05/2017   18784   wkwock     Handle not integer stationID.
 *  04/15/2019   7596    lsingh     Updated units framework to JSR-363.
 *  
 *  @author Chin Chen
 */

public class ObsSoundingQuery {

    private static final UnitConverter metersPerSecondToKnots = SI.METRE_PER_SECOND
            .getConverterTo(USCustomary.KNOT);

    private static final UnitConverter kelvinToCelsius = SI.KELVIN
            .getConverterTo(SI.CELSIUS);

    private static final String NCUAIR_TBL_NAME = "ncuair";

    private static final String BURFUA_TBL_NAME = "bufrua";

    private static String currentDBTblName = "nil";

    protected static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(ObsSoundingQuery.class);

    /*
     * This method is for caller to get sounding station's info lat/lon/stn
     * id/stn num/elevation key: stn's lat/lon or stn ( either stn id or stn
     * num)
     */
    // checked used by handleBufruaDataRequest()
    @SuppressWarnings("unchecked")
    private static NcSoundingProfile getObservedSndStnInfo(Float lat,
            Float lon, String stn, String obType, long refTimeL,
            SndQueryKeyType queryType) {
        NcSoundingProfile pf = new NcSoundingProfile();
        CoreDao dao;
        List<String> fields = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        if (obType.equals(ObsSndType.BUFRUA.toString())) {
            List<UAObs> lUairRecords = null;
            if (queryType == SndQueryKeyType.STNID) {
                fields.add("stationName");// the stationName String field name
                                          // defined in UAObs, dont be confused
                                          // with UAIRRecord definition
                values.add(stn);
            } else if (queryType == SndQueryKeyType.STNNUM) {
                fields.add("location.stationId");// the location.stationId
                                                 // String field name defined in
                                                 // UAObs. dont be confused with
                                                 // UAIRRecord definition
                values.add(stn);
            } else if (queryType == SndQueryKeyType.LATLON) {
                fields.add("location.latitude");// the location.latitude field
                                                // name defined in UAObs
                values.add(lat);
                fields.add("location.longitude");// the location.longitude field
                                                 // name defined in UAObs
                values.add(lon);

            } else {
                return pf;
            }
            fields.add("dataTime.refTime");// the synoptic time field name
                                           // defined in UAObs

            values.add(new Date(refTimeL));
            dao = new CoreDao(DaoConfig.forClass(UAObs.class));
            try {
                lUairRecords = (List<UAObs>) dao
                        .queryByCriteria(fields, values);
                if (lUairRecords.size() > 0) {
                    pf.setStationLatitude((float) lUairRecords.get(0)
                            .getLatitude());
                    pf.setStationLongitude((float) lUairRecords.get(0)
                            .getLongitude());
                    pf.setStationElevation(lUairRecords.get(0).getElevation());
                    if ((lUairRecords.get(0).getStationId() != null)
                            && (lUairRecords.get(0).getStationId().length() > 0)) {
                        pf.setStationNumStr(lUairRecords.get(0).getStationId());
                    }
                    pf.setStationId(lUairRecords.get(0).getStationName());
                    pf.setFcsTime(lUairRecords.get(0).getDataTime()
                            .getRefTime().getTime());
                }
            } catch (DataAccessLayerException e) {
                statusHandler.handle(
                        Priority.PROBLEM,
                        "-----DB exception at getObservedSndStnInfo: "
                                + e.getLocalizedMessage(), e);
            }
        }
        return pf;
    }

    public static NcSoundingStnInfoCollection getObservedSndStnInfoCol(
            SoundingType sndType, String selectedSndTime) {
        NcSoundingStnInfoCollection stnInfoCol = new NcSoundingStnInfoCollection();
        List<NcSoundingStnInfo> stationInfoList = new ArrayList<>();
        String queryStr, queryStr1;
        Object[] rtnobjArray, rtnobjArray1;
        CoreDao dao;
        if (sndType == SoundingType.OBS_BUFRUA_SND) {
            currentDBTblName = BURFUA_TBL_NAME;
            queryStr = new String(
                    "Select Distinct latitude, longitude, id, stationname, elevation, reftime FROM "
                            + currentDBTblName
                            + " where reftime='"
                            + selectedSndTime
                            + "' AND latitude BETWEEN -89.9 AND 89.9 AND longitude BETWEEN -179.9 AND 179.9");
            queryStr1 = new String(
                    "Select Distinct latitude, longitude FROM "
                            + currentDBTblName
                            + " where reftime='"
                            + selectedSndTime
                            + "' AND latitude BETWEEN -89.9 AND 89.9 AND longitude BETWEEN -179.9 AND 179.9");
            dao = new CoreDao(DaoConfig.forClass(UAObs.class));

        } else if (sndType == SoundingType.OBS_UAIR_SND) {
            currentDBTblName = NCUAIR_TBL_NAME;
            queryStr = new String(
                    "Select Distinct latitude, longitude, id, stationId, elevation, reftime FROM "
                            + currentDBTblName
                            + " where nil='FALSE' AND reftime='"
                            + selectedSndTime
                            + "' AND latitude BETWEEN -89.9 AND 89.9 AND longitude BETWEEN -179.9 AND 179.9");
            queryStr1 = new String(
                    "Select Distinct latitude, longitude FROM "
                            + currentDBTblName
                            + " where nil='FALSE' AND reftime='"
                            + selectedSndTime
                            + "' AND latitude BETWEEN -89.9 AND 89.9 AND longitude BETWEEN -179.9 AND 179.9");
            dao = new CoreDao(DaoConfig.forClass(NcUairRecord.class));
        } else {
            return stnInfoCol;
        }
        rtnobjArray = dao.executeSQLQuery(queryStr);

        rtnobjArray1 = dao.executeSQLQuery(queryStr1);

        if ((rtnobjArray1.length > 0) && (rtnobjArray.length > 0)) {
            float lat, lon, elv;
            String stnInfo;

            // Note: A same station may have many reports and at some reports
            // they dont provide elv and or stnid
            // this implementation is "try" to make sure we get those info.
            // If, all reports does not have elv or stnid, then we still can not
            // report it.
            for (Object element : rtnobjArray1) {
                Object[] objArray1 = (Object[]) element;
                Date synoptictime = null;
                stnInfo = "";
                elv = -999;
                lat = (Float) objArray1[0];
                lon = (Float) objArray1[1];
                for (Object element2 : rtnobjArray) {
                    Object[] objArray = (Object[]) element2;
                    if ((sndType == SoundingType.OBS_UAIR_SND
                            && (lat == (Float) objArray[0]) && (lon == (Float) objArray[1]))
                            || ((sndType == SoundingType.OBS_BUFRUA_SND)
                                    && (lat == (Float) objArray[0]) && (lon == (Float) objArray[1]))) {
                        if (stnInfo == "") {
                            stnInfo = (String) objArray[3];
                        }
                        if (elv == -999) {
                            if (sndType == SoundingType.OBS_UAIR_SND) {
                                elv = (Integer) objArray[4];
                            } else if (sndType == SoundingType.OBS_BUFRUA_SND) {
                                elv = (Integer) objArray[4];
                            }
                        }
                        synoptictime = (Date) objArray[5];
                    }

                }

                NcSoundingStnInfo stn = stnInfoCol.getNewStnInfo();
                stn.setStnId(stnInfo);
                stn.setStationLongitude(lon);
                stn.setStationLatitude(lat);
                stn.setStationElevation(elv);
                stn.setSynopTime(synoptictime);
                stationInfoList.add(stn);
            }
        }

        NcSoundingStnInfo[] stationInfoAry = new NcSoundingStnInfo[stationInfoList
                .size()];
        stnInfoCol.setStationInfo(stationInfoList.toArray(stationInfoAry));
        return stnInfoCol;
    }

    public static NcSoundingTimeLines getObservedSndTimeLine(
            SoundingType sndType) {
        Object[] synopTimeAry = null;
        NcSoundingTimeLines tl = new NcSoundingTimeLines();
        String queryStr;
        CoreDao dao;
        if (sndType == SoundingType.OBS_BUFRUA_SND) {
            currentDBTblName = BURFUA_TBL_NAME;
            queryStr = new String("Select Distinct reftime FROM "
                    + currentDBTblName + " ORDER BY reftime DESC");
            dao = new CoreDao(DaoConfig.forClass(UAObs.class));

        } else if (sndType == SoundingType.OBS_UAIR_SND) {
            currentDBTblName = NCUAIR_TBL_NAME;
            queryStr = new String("Select Distinct reftime FROM "
                    + currentDBTblName
                    + " where nil='FALSE' ORDER BY reftime DESC");
            dao = new CoreDao(DaoConfig.forClass(NcUairRecord.class));
        } else {
            return tl;
        }
        synopTimeAry = dao.executeSQLQuery(queryStr);
        tl.setTimeLines(synopTimeAry);

        return tl;
    }

    /*
     * Using Lat/lon array OR StnId array, AND soundingTimeAry (fcst time array)
     * as input. This function is to be generic for all cases. One and only one
     * of latLonArray and stnIdArr should be not null and the other one should
     * be null soundingTimeAry is a list of refer time (in NCUair, only use
     * refer time for query) should be not null
     */

    public static List<NcUairRecord[]> getObservedSndNcUairDataGeneric(
            Coordinate[] coordArray, String[] stnIdArray,
            String[] soundingTimeStrList, long[] soundTimeLongArr,
            String level, boolean pwRequired) {
        PointDataQuery request = null;
        PointDataContainer result = null;
        String stnStr = "";
        Coordinate coord = new Coordinate();

        List<NcUairRecord> returnedDbRecords = new ArrayList<>();
        List<NcUairRecord[]> finalRecordArrayList = new ArrayList<>();
        boolean queryByStn;
        try {
            request = new PointDataQuery("ncuair");
            // If a mandatory level is being requested, then we
            // can speed things up significantly by getting only the
            // TTAA/XXAA parameters from the datastore. Exceptions
            // to this are made (a) if we need precipitable water for
            // the entire sounding whose algorithm requires all data
            // types, OR (b) we're only requesting data for a single
            // station (as for Cloud Height) in which case performance
            // isn't an issue.
            boolean multipleStationsRequested = (coordArray != null && coordArray.length > 1)
                    || (stnIdArray != null && stnIdArray.length > 1);
            if (isMandatoryLevel(level) && pwRequired == false
                    && multipleStationsRequested) {
                request.setParameters(NcUairToRecord.AA_ONLY_PARAMS_LIST);
                // ...otherwise, we'd better grab the whole set
            } else {
                request.setParameters(NcUairToRecord.MAN_PARAMS_LIST);
            }
            request.addParameter("nil", String.valueOf(false), "=");
            // The following may look wasteful if we only need one level,
            // but requesting a specific level (1) requires spelling out
            // parameters to which it applies, and more importantly
            // (2) saves NO time on the resulting IDataStore request,
            // because all levels are retrieved there anyway(!). See note
            // in PointDataPluginDao.getPointData( ): "...for now, we
            // will retrieve all levels and then post-process the result"
            request.requestAllLevels();
            String d = "";
            for (String timeStr : soundingTimeStrList) {
                d = d + timeStr;
                d = d + ",";
            }
            // get rid of last ","
            d = d.substring(0, d.length() - 1);
            request.addParameter("dataTime.refTime", d, "in");
            if (coordArray != null) {
                String latStr = "", lonStr = "";
                for (Coordinate element : coordArray) {
                    latStr = latStr + String.valueOf(element.y) + ",";
                    lonStr = lonStr + String.valueOf(element.x) + ",";
                }
                // get rid of last ","
                latStr = latStr.substring(0, latStr.length() - 1);
                lonStr = lonStr.substring(0, lonStr.length() - 1);
                request.addParameter("location.latitude", latStr, "in");
                request.addParameter("location.longitude", lonStr, "in");
                queryByStn = false;
            } else if (stnIdArray != null) {
                String stnIdListStr = "";
                StringBuilder stringOfStnIds = new StringBuilder();
                for (String thisStnId : stnIdArray) {
                    stringOfStnIds.append(thisStnId);
                    stringOfStnIds.append(",");
                }
                stnIdListStr = stringOfStnIds.toString();
                // get rid of the last comma
                stnIdListStr = stnIdListStr.substring(0,
                        stnIdListStr.length() - 1);

                request.addParameter("location.stationId", stnIdListStr, "in");
                queryByStn = true;
            } else {
                return finalRecordArrayList;
            }

            result = request.execute();

            if (result != null) {
                returnedDbRecords = NcUairToRecord.toNcUairRecordsList(result);

                if ((returnedDbRecords != null)
                        && (returnedDbRecords.size() > 0)) {
                    // keep list of records for same station
                    // search through all returned records and keep same
                    // staion's records in one list
                    int loopLen;
                    if (queryByStn == true) {
                        loopLen = stnIdArray.length;
                    } else {
                        loopLen = coordArray.length;
                    }
                    NcUairRecord record;

                    for (int i = 0; i < loopLen; i++) {
                        // for each station
                        if (queryByStn == true) {
                            stnStr = stnIdArray[i];
                        } else {
                            coord = coordArray[i];
                        }

                        List<List<NcUairRecord>> stnRecordsList = new ArrayList<>();
                        for (long refT : soundTimeLongArr) {
                            // create empty List<NcUairRecord> for each sounding
                            // time line
                            List<NcUairRecord> stnRecords = new ArrayList<>();
                            stnRecordsList.add(stnRecords);
                        }
                        for (int j = returnedDbRecords.size() - 1; j >= 0; j--) {
                            record = returnedDbRecords.get(j);
                            boolean goodRecord = false;
                            if (queryByStn == true) {
                                if (stnStr.equals(record.getStationId())) {
                                    goodRecord = true;
                                }
                            } else {
                                // for some unknown reason that record
                                // returned from DB with lat/lon extended with
                                // many digits
                                // For example, coor.x=-114.4 coor.y=32.85
                                // record lon=-114.4000015258789 record
                                // lat=32.849998474121094
                                // Therefore, do the following.
                                if ((Math.abs(coord.x - record.getLongitude()) < 0.01)
                                        && (Math.abs(coord.y
                                                - record.getLatitude()) < 0.01)) {
                                    goodRecord = true;
                                }
                            }
                            if (goodRecord == true) {
                                for (int index = 0; index < soundTimeLongArr.length; index++) {
                                    long refT = soundTimeLongArr[index];
                                    if (refT == record.getDataTime()
                                            .getRefTime().getTime()) {
                                        stnRecordsList.get(index).add(
                                                returnedDbRecords.remove(j));
                                    }
                                }
                            }
                        }
                        for (List<NcUairRecord> recordList : stnRecordsList) {
                            if (recordList.size() > 0) {
                                List<NcUairRecord> pickedUairRecords = new ArrayList<>();
                                NcUairRecord orignalRd;
                                boolean addToList = true;
                                for (int ii = 0; ii < recordList.size(); ii++) {
                                    orignalRd = recordList.get(ii);
                                    addToList = true;
                                    for (NcUairRecord pickedRd : pickedUairRecords) {
                                        if (orignalRd.getDataType().equals(
                                                pickedRd.getDataType())) {
                                            // the two records have same data
                                            // type
                                            // this records will either replace
                                            // the one in list or be dropped
                                            addToList = false;
                                            if ((pickedRd
                                                    .getIssueTime()
                                                    .compareTo(
                                                            orignalRd
                                                                    .getIssueTime()) < 0)
                                                    || ((pickedRd
                                                            .getIssueTime()
                                                            .compareTo(
                                                                    orignalRd
                                                                            .getIssueTime()) == 0)
                                                            && (orignalRd
                                                                    .getCorr() != null)
                                                            && (pickedRd
                                                                    .getCorr() != null) && (pickedRd
                                                            .getCorr()
                                                            .compareTo(
                                                                    orignalRd
                                                                            .getCorr()) < 0))
                                                    || ((pickedRd
                                                            .getIssueTime()
                                                            .compareTo(
                                                                    orignalRd
                                                                            .getIssueTime()) == 0)
                                                            && (orignalRd
                                                                    .getCorr() != null) && (pickedRd
                                                            .getCorr() == null))) {
                                                // decide to replace picked with
                                                // original record, based on the
                                                // following cases, in
                                                // (priority) order
                                                // case 1: original record has
                                                // "later" issue time than
                                                // picked record
                                                // case 2: original record has
                                                // "larger" correction "corr"
                                                // than picked record
                                                // case 3: original record has
                                                // correction "corr", picked
                                                // record does not have
                                                int pickedIndex = pickedUairRecords
                                                        .indexOf(pickedRd);
                                                pickedUairRecords.set(
                                                        pickedIndex, orignalRd);
                                            }
                                            break;
                                        }
                                    }
                                    if (addToList == true) {
                                        // add this original record to picked
                                        // list
                                        pickedUairRecords.add(orignalRd);
                                    }
                                }
                                // set for special handling for its caller
                                finalRecordArrayList
                                        .add(pickedUairRecords
                                                .toArray(new NcUairRecord[pickedUairRecords
                                                        .size()]));
                            }
                        }
                    }

                }
            }
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM,
                    "-----DB exception at getObservedSndNcUairDataGeneric: "
                            + e.getLocalizedMessage(), e);
        }
        return finalRecordArrayList;
    }

    private static boolean isMandatoryLevel(String level) {
        if (level == null) {
            return false;
        }
        // alternate: final String mandatoryLevels =
        // ".surface.1000.925.850.700.500.400.300.250.200.150.100.mb";
        // return mandatoryLevels.contains/* IgnoreCase */("." + level + ".");
        final String[] mandatoryLevels = { "1000", "925", "850", "700", "500",
                "400", "300", "250", "200", "150", "100" };
        for (String s : mandatoryLevels) {
            if (s.equals(level)) {
                return true;
            }
        }
        return false;
    }

    /*
     * this api is provided for applications and for testing to retrieve
     * observed bufruair data from PostgreSql DB & HDF5 dataType should use
     * "enum DataType" defined in NcSoundingLayer.java Support "ALLDATA" data
     * type only
     */
    public static NcSoundingProfile getObservedSndBufruaAllData(Float lat,
            Float lon, String stn, long refTimeL, SndQueryKeyType queryType) {
        NcSoundingProfile pfAll = new NcSoundingProfile();
        List<NcSoundingLayer> soundingLyLst, finalsoundingLyLst;
        NcSoundingProfile pf = getObservedSndBufruaData(lat, lon, stn,
                refTimeL, "TTAA", queryType);
        pfAll.setStationElevation(pf.getStationElevation());
        finalsoundingLyLst = pf.getSoundingLyLst();
        soundingLyLst = getObservedSndBufruaData(lat, lon, stn, refTimeL,
                "TTBB", queryType).getSoundingLyLst();
        if (soundingLyLst.size() >= 0) {
            finalsoundingLyLst.addAll(soundingLyLst);
        }
        soundingLyLst = getObservedSndBufruaData(lat, lon, stn, refTimeL,
                "TTCC", queryType).getSoundingLyLst();
        if (soundingLyLst.size() >= 0) {
            finalsoundingLyLst.addAll(soundingLyLst);
        }
        soundingLyLst = getObservedSndBufruaData(lat, lon, stn, refTimeL,
                "TTDD", queryType).getSoundingLyLst();
        if (soundingLyLst.size() >= 0) {
            finalsoundingLyLst.addAll(soundingLyLst);
        }
        soundingLyLst = getObservedSndBufruaData(lat, lon, stn, refTimeL,
                "PPBB", queryType).getSoundingLyLst();
        if (soundingLyLst.size() >= 0) {
            finalsoundingLyLst.addAll(soundingLyLst);
        }
        soundingLyLst = getObservedSndBufruaData(lat, lon, stn, refTimeL,
                "PPDD", queryType).getSoundingLyLst();
        if (soundingLyLst.size() >= 0) {
            finalsoundingLyLst.addAll(soundingLyLst);
        }
        soundingLyLst = getObservedSndBufruaData(lat, lon, stn, refTimeL,
                "MAXWIND_A", queryType).getSoundingLyLst();
        if (soundingLyLst.size() >= 0) {
            finalsoundingLyLst.addAll(soundingLyLst);
        }
        soundingLyLst = getObservedSndBufruaData(lat, lon, stn, refTimeL,
                "MAXWIND_C", queryType).getSoundingLyLst();
        if (soundingLyLst.size() >= 0) {
            finalsoundingLyLst.addAll(soundingLyLst);
        }
        soundingLyLst = getObservedSndBufruaData(lat, lon, stn, refTimeL,
                "TROPOPAUSE_A", queryType).getSoundingLyLst();
        if (soundingLyLst.size() >= 0) {
            finalsoundingLyLst.addAll(soundingLyLst);
        }
        soundingLyLst = getObservedSndBufruaData(lat, lon, stn, refTimeL,
                "TROPOPAUSE_C", queryType).getSoundingLyLst();
        if (soundingLyLst.size() >= 0) {
            finalsoundingLyLst.addAll(soundingLyLst);
        }
        pfAll.setSoundingLyLst(finalsoundingLyLst);
        return pfAll;
    }

    /*
     * This API is provided for retrieving bufrUA data from PostgresSQL and HDF5
     * dataType should use "enum DataType" defined in NcSoundingLayer.java
     * Support all dataType except "ALLDATA" data type using either lat/lon,
     * stnId or stnNum and synopticTime as key reference time is with Calendar
     * data type as input Chin's Note 2/28/2012:This API is used for bufrua
     * query now. Should be replaced by getObservedSndBufruaDataGeneric() when
     * point data query is implemented.
     */
    public static NcSoundingProfile getObservedSndBufruaData(Float lat,
            Float lon, String stn, long refTime, String dataType,
            SndQueryKeyType queryType) {
        NcSoundingProfile pf = new NcSoundingProfile();
        List<NcSoundingLayer> soundLyList = new ArrayList<>();
        if (dataType.equals(NcSoundingLayer.DataType.ALLDATA.toString())) {
            return pf;
        } else {
            List<String> fields = new ArrayList<>();
            List<Object> values = new ArrayList<>();
            List<UAObs> lUairRecords = null;
            if (queryType == SndQueryKeyType.STNID) {
                fields.add("stationName");// the stationName String field name
                                          // defined in UAObs, dont be confused
                                          // with UAIRRecord definition
                values.add(stn);
            } else if (queryType == SndQueryKeyType.STNNUM) {
                fields.add("location.stationId");// the location.stationId
                                                 // String field name defined in
                                                 // UAObs. dont be confused with
                                                 // UAIRRecord definition
                values.add(stn);
            } else if (queryType == SndQueryKeyType.LATLON) {
                fields.add("location.latitude");// the location.latitude field
                                                // name defined in UAObs
                values.add(lat);
                fields.add("location.longitude");// the location.longitude field
                                                 // name defined in UAObs
                values.add(lon);

            } else {
                System.out.println("request query type " + queryType
                        + " is not supported in this API");
                return pf;
            }
            fields.add("dataTime.refTime");// the synoptic time field name
                                           // defined in UAObs

            values.add(new Date(refTime));
            fields.add("reportType");// the record dataType field name defined
                                     // in UAObs
            int intDataType = NcSoundingLayer.dataTypeMap.get(dataType);
            values.add(intDataType);
            CoreDao dao = new CoreDao(DaoConfig.forClass(UAObs.class));
            try {
                lUairRecords = ((List<UAObs>) dao.queryByCriteria(fields,
                        values));
                if (lUairRecords.size() > 0) {
                    // set pf data
                    // System.out.println("record size = "+ lUairRecords.size()
                    // + " reportType="+dataType);

                    int lastCorrectedRecord = 0;
                    String currentCorInd = "";

                    if (lUairRecords.size() > 1) {
                        for (int i = 0; i < lUairRecords.size(); i++) {
                            // Since we are using lat/lon/refTime to query uair
                            // table. We may have several records returned for
                            // one query. It indicates there is a correction
                            // report, then we should use the newest one report.
                            // we compare corIndicator to find the latest
                            // record.

                            if ((lUairRecords.get(i).getCorIndicator() != null)
                                    && (currentCorInd.compareTo(lUairRecords
                                            .get(i).getCorIndicator()) < 0)) {
                                currentCorInd = lUairRecords.get(i)
                                        .getCorIndicator();
                                lastCorrectedRecord = i;
                            }
                        }
                    }
                    UAObs uairRecord = lUairRecords.get(lastCorrectedRecord);
                    pf.setStationLatitude((float) uairRecord.getLatitude());
                    pf.setStationLongitude((float) uairRecord.getLongitude());
                    pf.setStationElevation(uairRecord.getElevation());
                    if ((uairRecord.getStationId() != null)
                            && (uairRecord.getStationId().length() > 0)) {
                        pf.setStationNumStr(uairRecord.getStationId());
                    }
                    pf.setStationId(uairRecord.getStationName());
                    int hdfIndex = uairRecord.getIdx();
                    if (hdfIndex >= 0) {
                        BufrUADao uadao = new BufrUADao("bufrua");
                        File hdf5loc = uadao.getFullFilePath(uairRecord);
                        IDataStore dataStore = DataStoreFactory
                                .getDataStore(hdf5loc);

                        FloatDataRecord sfcPressurefloatData = (FloatDataRecord) dataStore
                                .retrieve(
                                        "/",
                                        "sfcPressure",
                                        Request.buildYLineRequest(new int[] { hdfIndex }));
                        float[] sfcPressuredata = sfcPressurefloatData
                                .getFloatData();
                        if (sfcPressuredata.length > 0) {
                            pf.setSfcPress(sfcPressuredata[0] / 100F);
                        }

                        NcSoundingLayer soundingLy;
                        // based on requested data type:
                        // get temp, dew point, pressure, wind u/v components,
                        // and height
                        // they are 2-D tables
                        if (dataType.equals(NcSoundingLayer.DataType.TTAA
                                .toString())
                                || dataType
                                        .equals(NcSoundingLayer.DataType.TTCC
                                                .toString())) {
                            // get mandatory data size
                            IntegerDataRecord numManIntData = (IntegerDataRecord) dataStore
                                    .retrieve(
                                            "/",
                                            "numMand",
                                            Request.buildYLineRequest(new int[] { hdfIndex }));
                            int[] sizes = numManIntData.getIntData();
                            // sizes is a 1x1 2d table. Only first (0) element
                            // is valid.
                            if (sizes[0] > 0) {
                                FloatDataRecord pressurefloatData = (FloatDataRecord) dataStore
                                        .retrieve(
                                                "/",
                                                "prMan",
                                                Request.buildYLineRequest(new int[] { hdfIndex }));
                                float[] pressuredata = pressurefloatData
                                        .getFloatData();
                                FloatDataRecord temperaturefloatData = (FloatDataRecord) dataStore
                                        .retrieve(
                                                "/",
                                                "tpMan",
                                                Request.buildYLineRequest(new int[] { hdfIndex }));
                                float[] temperaturedata = temperaturefloatData
                                        .getFloatData();
                                FloatDataRecord dewptfloatData = (FloatDataRecord) dataStore
                                        .retrieve(
                                                "/",
                                                "tdMan",
                                                Request.buildYLineRequest(new int[] { hdfIndex }));
                                float[] dewptdata = dewptfloatData
                                        .getFloatData();
                                FloatDataRecord windDfloatData = (FloatDataRecord) dataStore
                                        .retrieve(
                                                "/",
                                                "wdMan",
                                                Request.buildYLineRequest(new int[] { hdfIndex }));
                                float[] windDdata = windDfloatData
                                        .getFloatData();
                                FloatDataRecord windSfloatData = (FloatDataRecord) dataStore
                                        .retrieve(
                                                "/",
                                                "wsMan",
                                                Request.buildYLineRequest(new int[] { hdfIndex }));
                                float[] windSdata = windSfloatData
                                        .getFloatData();
                                FloatDataRecord htfloatData = (FloatDataRecord) dataStore
                                        .retrieve(
                                                "/",
                                                "htMan",
                                                Request.buildYLineRequest(new int[] { hdfIndex }));
                                float[] htdata = htfloatData.getFloatData();
                                for (int i = 0; i < sizes[0]; i++) {
                                    soundingLy = new NcSoundingLayer();
                                    // if data is not available, dont convert it
                                    // and just use default setting data
                                    if (temperaturedata[i] != NcSoundingLayer.MISSING) {
                                        soundingLy
                                                .setTemperature((float) kelvinToCelsius
                                                        .convert(temperaturedata[i]));
                                    }
                                    if (pressuredata[i] != NcSoundingLayer.MISSING) {
                                        soundingLy
                                                .setPressure(pressuredata[i] / 100F);
                                    }
                                    if (windSdata[i] != NcSoundingLayer.MISSING) {
                                        soundingLy
                                                .setWindSpeed((float) metersPerSecondToKnots
                                                        .convert(windSdata[i]));
                                    }
                                    soundingLy.setWindDirection(windDdata[i]);
                                    if (dewptdata[i] != NcSoundingLayer.MISSING) {
                                        soundingLy
                                                .setDewpoint((float) kelvinToCelsius
                                                        .convert(dewptdata[i]));
                                    }
                                    soundingLy.setGeoHeight(htdata[i]);
                                    soundLyList.add(soundingLy);
                                }
                            }
                        } else if (dataType
                                .equals(NcSoundingLayer.DataType.TTBB
                                        .toString())
                                || dataType
                                        .equals(NcSoundingLayer.DataType.TTDD
                                                .toString())) {
                            // get significantT data size
                            IntegerDataRecord numSigtIntData = (IntegerDataRecord) dataStore
                                    .retrieve(
                                            "/",
                                            "numSigT",
                                            Request.buildYLineRequest(new int[] { hdfIndex }));
                            int[] sizes = numSigtIntData.getIntData();
                            // sizes is a 1x1 2d table. Only first (0) element
                            // is valid.
                            if (sizes[0] > 0) {
                                FloatDataRecord pressurefloatData = (FloatDataRecord) dataStore
                                        .retrieve(
                                                "/",
                                                "prSigT",
                                                Request.buildYLineRequest(new int[] { hdfIndex }));
                                float[] pressuredata = pressurefloatData
                                        .getFloatData();
                                FloatDataRecord temperaturefloatData = (FloatDataRecord) dataStore
                                        .retrieve(
                                                "/",
                                                "tpSigT",
                                                Request.buildYLineRequest(new int[] { hdfIndex }));
                                float[] temperaturedata = temperaturefloatData
                                        .getFloatData();
                                FloatDataRecord dewptfloatData = (FloatDataRecord) dataStore
                                        .retrieve(
                                                "/",
                                                "tdSigT",
                                                Request.buildYLineRequest(new int[] { hdfIndex }));
                                float[] dewptdata = dewptfloatData
                                        .getFloatData();
                                for (int i = 0; i < sizes[0]; i++) {
                                    soundingLy = new NcSoundingLayer();
                                    // if data is not available, dont convert it
                                    // and just use default setting data
                                    if (temperaturedata[i] != NcSoundingLayer.MISSING) {
                                        soundingLy
                                                .setTemperature((float) kelvinToCelsius
                                                        .convert(temperaturedata[i]));
                                    }
                                    if (pressuredata[i] != NcSoundingLayer.MISSING) {
                                        soundingLy
                                                .setPressure(pressuredata[i] / 100F);
                                    }
                                    if (dewptdata[i] != NcSoundingLayer.MISSING) {
                                        soundingLy
                                                .setDewpoint((float) kelvinToCelsius
                                                        .convert(dewptdata[i]));
                                    }
                                    soundLyList.add(soundingLy);
                                }
                            }

                        } else if (dataType
                                .equals(NcSoundingLayer.DataType.PPBB
                                        .toString())
                                || dataType
                                        .equals(NcSoundingLayer.DataType.PPDD
                                                .toString())) {
                            // get significantW data size
                            IntegerDataRecord numSigwIntData = (IntegerDataRecord) dataStore
                                    .retrieve(
                                            "/",
                                            "numSigW",
                                            Request.buildYLineRequest(new int[] { hdfIndex }));
                            int[] sizes = numSigwIntData.getIntData();
                            // sizes is a 1x1 2d table. Only first (0) element
                            // is valid.
                            if (sizes[0] > 0) {
                                FloatDataRecord htfloatData = (FloatDataRecord) dataStore
                                        .retrieve(
                                                "/",
                                                "htSigW",
                                                Request.buildYLineRequest(new int[] { hdfIndex }));
                                float[] htdata = htfloatData.getFloatData();
                                float[] prdata = null;
                                try {
                                    FloatDataRecord prfloatData = (FloatDataRecord) dataStore
                                            .retrieve(
                                                    "/",
                                                    "prSigW",
                                                    Request.buildYLineRequest(new int[] { hdfIndex }));
                                    prdata = prfloatData.getFloatData();
                                } catch (StorageException e) {
                                    /*
                                     * This record does not exist in older data
                                     * so fill with MISSING.
                                     */
                                    prdata = new float[sizes[0]];
                                    Arrays.fill(prdata, NcSoundingLayer.MISSING);
                                }
                                FloatDataRecord windDfloatData = (FloatDataRecord) dataStore
                                        .retrieve(
                                                "/",
                                                "wdSigW",
                                                Request.buildYLineRequest(new int[] { hdfIndex }));
                                float[] windDdata = windDfloatData
                                        .getFloatData();
                                FloatDataRecord windSfloatData = (FloatDataRecord) dataStore
                                        .retrieve(
                                                "/",
                                                "wsSigW",
                                                Request.buildYLineRequest(new int[] { hdfIndex }));
                                float[] windSdata = windSfloatData
                                        .getFloatData();
                                for (int i = 0; i < sizes[0]; i++) {
                                    soundingLy = new NcSoundingLayer();
                                    // if data is not available, dont convert it
                                    // and just use default setting data
                                    soundingLy.setGeoHeight(htdata[i]);
                                    if (prdata[i] != NcSoundingLayer.MISSING) {
                                        soundingLy
                                                .setPressure(prdata[i] / 100F);
                                    }
                                    soundingLy.setPressure(prdata[i]);
                                    if (windSdata[i] != NcSoundingLayer.MISSING) {
                                        soundingLy
                                                .setWindSpeed((float) metersPerSecondToKnots
                                                        .convert(windSdata[i]));
                                    }
                                    soundingLy.setWindDirection(windDdata[i]);
                                    soundLyList.add(soundingLy);
                                }
                            }
                        } else if (dataType
                                .equals(NcSoundingLayer.DataType.MAXWIND_A
                                        .toString())
                                || dataType
                                        .equals(NcSoundingLayer.DataType.MAXWIND_C
                                                .toString())) {
                            // get max wind data size
                            IntegerDataRecord numMwndIntData = (IntegerDataRecord) dataStore
                                    .retrieve(
                                            "/",
                                            "numMwnd",
                                            Request.buildYLineRequest(new int[] { hdfIndex }));
                            int[] sizes = numMwndIntData.getIntData();
                            // sizes is a 1x1 2d table. Only first (0) element
                            // is valid.
                            if (sizes[0] > 0) {
                                FloatDataRecord pressurefloatData = (FloatDataRecord) dataStore
                                        .retrieve(
                                                "/",
                                                "prMaxW",
                                                Request.buildYLineRequest(new int[] { hdfIndex }));
                                float[] pressuredata = pressurefloatData
                                        .getFloatData();
                                FloatDataRecord windDfloatData = (FloatDataRecord) dataStore
                                        .retrieve(
                                                "/",
                                                "wdMaxW",
                                                Request.buildYLineRequest(new int[] { hdfIndex }));
                                float[] windDdata = windDfloatData
                                        .getFloatData();
                                FloatDataRecord windSfloatData = (FloatDataRecord) dataStore
                                        .retrieve(
                                                "/",
                                                "wsMaxW",
                                                Request.buildYLineRequest(new int[] { hdfIndex }));
                                float[] windSdata = windSfloatData
                                        .getFloatData();
                                for (int i = 0; i < sizes[0]; i++) {
                                    soundingLy = new NcSoundingLayer();
                                    // if data is not available, dont convert it
                                    // and just use default setting data
                                    if (pressuredata[i] != NcSoundingLayer.MISSING) {
                                        soundingLy
                                                .setPressure(pressuredata[i] / 100F);
                                    }
                                    if (windSdata[i] != NcSoundingLayer.MISSING) {
                                        soundingLy
                                                .setWindSpeed((float) metersPerSecondToKnots
                                                        .convert(windSdata[i]));
                                    }
                                    soundingLy.setWindDirection(windDdata[i]);
                                    soundLyList.add(soundingLy);
                                }
                            }
                        } else if (dataType
                                .equals(NcSoundingLayer.DataType.TROPOPAUSE_A
                                        .toString())
                                || dataType
                                        .equals(NcSoundingLayer.DataType.TROPOPAUSE_C
                                                .toString())) {
                            // get troppause data size
                            IntegerDataRecord numTropIntData = (IntegerDataRecord) dataStore
                                    .retrieve(
                                            "/",
                                            "numTrop",
                                            Request.buildYLineRequest(new int[] { hdfIndex }));
                            int[] sizes = numTropIntData.getIntData();
                            // sizes is a 1x1 2d table. Only first (0) element
                            // is valid.
                            if (sizes[0] > 0) {
                                FloatDataRecord pressurefloatData = (FloatDataRecord) dataStore
                                        .retrieve(
                                                "/",
                                                "prTrop",
                                                Request.buildYLineRequest(new int[] { hdfIndex }));
                                float[] pressuredata = pressurefloatData
                                        .getFloatData();
                                FloatDataRecord temperaturefloatData = (FloatDataRecord) dataStore
                                        .retrieve(
                                                "/",
                                                "tpTrop",
                                                Request.buildYLineRequest(new int[] { hdfIndex }));
                                float[] temperaturedata = temperaturefloatData
                                        .getFloatData();
                                FloatDataRecord dewptfloatData = (FloatDataRecord) dataStore
                                        .retrieve(
                                                "/",
                                                "tdTrop",
                                                Request.buildYLineRequest(new int[] { hdfIndex }));
                                float[] dewptdata = dewptfloatData
                                        .getFloatData();
                                FloatDataRecord windDfloatData = (FloatDataRecord) dataStore
                                        .retrieve(
                                                "/",
                                                "wdTrop",
                                                Request.buildYLineRequest(new int[] { hdfIndex }));
                                float[] windDdata = windDfloatData
                                        .getFloatData();
                                FloatDataRecord windSfloatData = (FloatDataRecord) dataStore
                                        .retrieve(
                                                "/",
                                                "wsTrop",
                                                Request.buildYLineRequest(new int[] { hdfIndex }));
                                float[] windSdata = windSfloatData
                                        .getFloatData();
                                for (int i = 0; i < sizes[0]; i++) {
                                    soundingLy = new NcSoundingLayer();
                                    // if data is not available, dont convert it
                                    // and just use default setting data
                                    if (temperaturedata[i] != NcSoundingLayer.MISSING) {
                                        soundingLy
                                                .setTemperature((float) kelvinToCelsius
                                                        .convert(temperaturedata[i]));
                                    }
                                    if (pressuredata[i] != NcSoundingLayer.MISSING) {
                                        soundingLy
                                                .setPressure(pressuredata[i] / 100F);
                                    }
                                    if (windSdata[i] != NcSoundingLayer.MISSING) {
                                        soundingLy
                                                .setWindSpeed((float) metersPerSecondToKnots
                                                        .convert(windSdata[i]));
                                    }
                                    soundingLy.setWindDirection(windDdata[i]);
                                    if (dewptdata[i] != NcSoundingLayer.MISSING) {
                                        soundingLy
                                                .setDewpoint((float) kelvinToCelsius
                                                        .convert(dewptdata[i]));
                                    }
                                    soundLyList.add(soundingLy);
                                }
                            }
                        }

                    } else {
                        return pf;
                    }
                } else {
                    return pf;
                }

            } catch (Exception e) {
                statusHandler.handle(
                        Priority.PROBLEM,
                        "-----DB exception at getObservedSndBufruaData: "
                                + e.getLocalizedMessage(), e);
                return pf;
            }

            pf.setSoundingLyLst(soundLyList);

            return pf;

        }
    }

    private static List<NcSoundingProfile> processQueryReturnedNcUairData(
            List<NcUairRecord[]> uairRecordArrList, boolean merge,
            String level, boolean pwRequired, boolean windInterpolation) {
        List<NcSoundingProfile> soundingProfileList = new ArrayList<>(0);
        for (NcUairRecord[] recordArray : uairRecordArrList) {

            NcSoundingProfile pf;

            if (merge == false) {
                pf = null;
            } else {
                pf = new NcSoundingProfile();
                // use NcSoundingLayer
                if (recordArray != null && recordArray.length > 0) {
                    MergeSounding ms = new MergeSounding();
                    List<NcSoundingLayer> sls = new ArrayList<>();
                    List<NcSoundingLayer> ttaa = new ArrayList<>();
                    List<NcSoundingLayer> ttbb = new ArrayList<>();
                    List<NcSoundingLayer> ttcc = new ArrayList<>();
                    List<NcSoundingLayer> ttdd = new ArrayList<>();
                    List<NcSoundingLayer> ppaa = new ArrayList<>();
                    List<NcSoundingLayer> ppbb = new ArrayList<>();
                    List<NcSoundingLayer> ppcc = new ArrayList<>();
                    List<NcSoundingLayer> ppdd = new ArrayList<>();
                    List<NcSoundingLayer> trop_a = new ArrayList<>();
                    List<NcSoundingLayer> trop_c = new ArrayList<>();
                    List<NcSoundingLayer> wmax_a = new ArrayList<>();
                    List<NcSoundingLayer> wmax_c = new ArrayList<>();
                    for (int k = 0; k < recordArray.length; k++) {
                        NcUairRecord record = recordArray[k];
                        if (record.getDataType().equals("TTAA")
                                || record.getDataType().equals("XXAA")) {
                            ttaa = getSndLayersFromNcUairRecordObsLevel(record);
                            trop_a = getSndLayersFromNcUairRecordTrop(record);
                            wmax_a = getSndLayersFromNcUairRecordMaxw(record);
                        } else if (record.getDataType().equals("TTBB")
                                || record.getDataType().equals("XXBB")) {
                            ttbb = getSndLayersFromNcUairRecordObsLevel(record);
                        } else if (record.getDataType().equals("TTCC")
                                || record.getDataType().equals("XXCC")) {
                            ttcc = getSndLayersFromNcUairRecordObsLevel(record);
                            trop_c = getSndLayersFromNcUairRecordTrop(record);
                            wmax_c = getSndLayersFromNcUairRecordMaxw(record);
                        } else if (record.getDataType().equals("TTDD")
                                || record.getDataType().equals("XXDD")) {
                            ttdd = getSndLayersFromNcUairRecordObsLevel(record);
                        } else if (record.getDataType().equals("PPAA")) {
                            ppaa = getSndLayersFromNcUairRecordObsLevel(record);
                        } else if (record.getDataType().equals("PPBB")) {
                            ppbb = getSndLayersFromNcUairRecordObsLevel(record);
                        } else if (record.getDataType().equals("PPCC")) {
                            ppcc = getSndLayersFromNcUairRecordObsLevel(record);
                        } else if (record.getDataType().equals("PPDD")) {
                            ppdd = getSndLayersFromNcUairRecordObsLevel(record);
                        }
                    }
                    pf = new NcSoundingProfile();
                    pf.setStationElevation((float) recordArray[0]
                            .getElevation());
                    pf.setStationId(recordArray[0].getStationId());
                    if (recordArray[0].getStnum() != null
                            && recordArray[0].getStnum().length() > 0) {
                        pf.setStationNumStr(recordArray[0].getStnum());
                    }
                    pf.setStationLatitude(recordArray[0].getLatitude());
                    pf.setStationLongitude(recordArray[0].getLongitude());
                    pf.setFcsTime(recordArray[0].getDataTime().getRefTime()
                            .getTime());

                    sls = ms.mergeUairSounding(level, ttaa, ttbb, ttcc, ttdd,
                            ppaa, ppbb, ppcc, ppdd, trop_a, trop_c, wmax_a,
                            wmax_c, pf.getStationElevation(), windInterpolation);
                    // PW Support
                    if (pwRequired) {
                        if (level.equals("-1")) {
                            float pw = NcSoundingTools.precip_water(sls);
                            pf.setPw(pw);
                        } else {
                            List<NcSoundingLayer> sls2 = new ArrayList<>();
                            sls2 = ms
                                    .mergeUairSounding("-1", ttaa, ttbb, ttcc,
                                            ttdd, ppaa, ppbb, ppcc, ppdd,
                                            trop_a, trop_c, wmax_a, wmax_c,
                                            pf.getStationElevation(),
                                            windInterpolation);
                            if (sls2 != null && sls2.size() > 0) {
                                pf.setPw(NcSoundingTools.precip_water(sls2));
                            }
                        }
                    }
                    // end PW Support
                    if (level.toUpperCase().equalsIgnoreCase("MAN")) {
                        pf.setSoundingLyLst(sls);
                    } else if (ms.isNumber(level) >= 0) {
                        if (sls.size() == 1) {
                            pf.setSoundingLyLst(sls);
                        } else {
                            pf = null;
                        }
                    } else {
                        if (sls.isEmpty() || sls.size() <= 1) {
                            pf = null;
                        } else {
                            pf.setSoundingLyLst(sls);
                        }
                    }
                } else {
                    pf = null;
                }
                if (pf != null
                        && (pf.getSoundingLyLst2().size() > 0 || pf
                                .getSoundingLyLst().size() > 0)) {
                    soundingProfileList.add(pf);
                    pf = null;
                }
            }
        }
        return soundingProfileList;
    }

    private static List<NcSoundingLayer> getSndLayersFromNcUairRecordObsLevel(
            NcUairRecord record) {
        List<NcSoundingLayer> sndLayers = new ArrayList<>();
        Set<NcUairObsLevels> obLevels = record.getObsLevels();
        if (obLevels.size() > 0) {
            for (NcUairObsLevels obLev : obLevels) {
                NcSoundingLayer sndLayer = new NcSoundingLayer();
                sndLayer.setTemperature(obLev.getTemp());
                sndLayer.setDewpoint(obLev.getDwpt());
                sndLayer.setGeoHeight(obLev.getHght());
                sndLayer.setPressure(obLev.getPres());
                sndLayer.setWindDirection(obLev.getDrct());
                if (obLev.getSped() >= 0) {
                    sndLayer.setWindSpeed((float) metersPerSecondToKnots
                            .convert(obLev.getSped()));
                } else {
                    sndLayer.setWindSpeed(obLev.getSped());
                }
                sndLayers.add(sndLayer);
            }
        }
        return sndLayers;
    }

    private static List<NcSoundingLayer> getSndLayersFromNcUairRecordTrop(
            NcUairRecord record) {
        List<NcSoundingLayer> sndLayers = new ArrayList<>();
        Set<NcUairTropopause> trops = record.getTropopause();
        if (trops.size() > 0) {
            for (NcUairTropopause trop : trops) {
                NcSoundingLayer sndLayer = new NcSoundingLayer();
                sndLayer.setTemperature(trop.getTemp());
                sndLayer.setDewpoint(trop.getDwpt());
                sndLayer.setPressure(trop.getPres());
                sndLayer.setWindDirection(trop.getDrct());
                if (trop.getSped() >= 0) {
                    sndLayer.setWindSpeed((float) metersPerSecondToKnots
                            .convert(trop.getSped()));
                } else {
                    sndLayer.setWindSpeed(trop.getSped());
                }
                sndLayers.add(sndLayer);
            }
        }
        return sndLayers;
    }

    private static List<NcSoundingLayer> getSndLayersFromNcUairRecordMaxw(
            NcUairRecord record) {
        List<NcSoundingLayer> sndLayers = new ArrayList<>();
        Set<NcUairMaxWind> maxWinds = record.getMaxWind();
        if (maxWinds.size() > 0) {
            for (NcUairMaxWind maxWind : maxWinds) {
                NcSoundingLayer sndLayer = new NcSoundingLayer();
                sndLayer.setPressure(maxWind.getPres());
                sndLayer.setWindDirection(maxWind.getDrct());
                if (maxWind.getSped() >= 0) {
                    sndLayer.setWindSpeed((float) metersPerSecondToKnots
                            .convert(maxWind.getSped()));
                } else {
                    sndLayer.setWindSpeed(maxWind.getSped());
                }
                sndLayers.add(sndLayer);
            }
        }
        return sndLayers;
    }

    public static NcSoundingCube handleNcuairDataRequest(
            SoundingServiceRequest request) {

        String[] refTimeStrLst = request.getRefTimeStrAry();
        if (refTimeStrLst == null)
            refTimeStrLst = QueryMiscTools
                    .convertTimeLongArrayToStrArray(request.getRefTimeAry());
        List<NcUairRecord[]> uairRecordArrList = ObsSoundingQuery
                .getObservedSndNcUairDataGeneric(request.getLatLonAry(),
                        request.getStnIdAry(), refTimeStrLst,
                        request.getRefTimeAry(), request.getLevel(),
                        request.isPwRequired());
        if (uairRecordArrList != null && uairRecordArrList.size() > 0) {
            List<NcSoundingProfile> soundingProfileList = ObsSoundingQuery
                    .processQueryReturnedNcUairData(uairRecordArrList,
                            request.isMerge(), request.getLevel(),
                            request.isPwRequired(),
                            request.isWindInterpolation());
            NcSoundingCube cube = new NcSoundingCube();
            cube.setSoundingProfileList(soundingProfileList);
            cube.setRtnStatus(NcSoundingCube.QueryStatus.OK);
            return (cube);
        } else {
            return null;
        }
    }

    public static NcSoundingCube handleBufruaDataRequest(
            SoundingServiceRequest request) {

        int arrLen = 0;
        SndQueryKeyType sndQuery;
        NcSoundingCube cube = new NcSoundingCube();
        List<NcSoundingProfile> soundingProfileList = new ArrayList<>();
        Coordinate[] latLonArray = request.getLatLonAry();
        String[] stnIdArr = request.getStnIdAry();
        if (latLonArray != null) {
            arrLen = latLonArray.length;
            sndQuery = SndQueryKeyType.LATLON;
        } else if (stnIdArr != null) {
            arrLen = stnIdArr.length;
            sndQuery = SndQueryKeyType.STNID;
        } else {
            return null;
        }
        MergeSounding ms = new MergeSounding();
        float lat = 0, lon = 0;
        String stnId = "";
        for (int i = 0; i < arrLen; i++) {
            if (sndQuery == SndQueryKeyType.LATLON) {
                // make sure we have right precision...
                lat = (float) latLonArray[i].y;
                lon = (float) latLonArray[i].x;
            } else {
                stnId = stnIdArr[i];
            }
            /*
             * Process sounding data.
             */
            NcSoundingProfile pf = new NcSoundingProfile();
            List<NcSoundingLayer> sls = new ArrayList<>();
            List<NcSoundingLayer> ttaa = new ArrayList<>();
            List<NcSoundingLayer> ttbb = new ArrayList<>();
            List<NcSoundingLayer> ttcc = new ArrayList<>();
            List<NcSoundingLayer> ttdd = new ArrayList<>();
            List<NcSoundingLayer> ppaa = new ArrayList<>();
            List<NcSoundingLayer> ppbb = new ArrayList<>();
            List<NcSoundingLayer> ppcc = new ArrayList<>();
            List<NcSoundingLayer> ppdd = new ArrayList<>();
            List<NcSoundingLayer> trop_a = new ArrayList<>();
            List<NcSoundingLayer> trop_c = new ArrayList<>();
            List<NcSoundingLayer> wmax_a = new ArrayList<>();
            List<NcSoundingLayer> wmax_c = new ArrayList<>();
            long[] refTimeArray = request.getRefTimeAry();
            if (refTimeArray == null) {
                if (request.getRefTimeStrAry() == null) {
                    return null;
                }
                String[] refTimeStrArray = request.getRefTimeStrAry();
                refTimeArray = QueryMiscTools
                        .convertTimeStrArrayToLongArray(refTimeStrArray);
            }
            for (long refLtime : refTimeArray) {
                if (request.isMerge() == false) {
                    pf = ObsSoundingQuery.getObservedSndBufruaAllData(lat, lon,
                            stnId, refLtime, sndQuery);

                } else {

                    // Get TTAA. If not existent, try ship data (UUAA). If
                    // level is not null or missing,
                    // the body of code will return a sounding list with MAN
                    // data or single level data.
                    ttaa = ObsSoundingQuery.getObservedSndBufruaData(lat, lon,
                            stnId, refLtime, "TTAA", sndQuery)
                            .getSoundingLyLst();
                    ttbb = ObsSoundingQuery.getObservedSndBufruaData(lat, lon,
                            stnId, refLtime, "TTBB", sndQuery)
                            .getSoundingLyLst();
                    ttcc = ObsSoundingQuery.getObservedSndBufruaData(lat, lon,
                            stnId, refLtime, "TTCC", sndQuery)
                            .getSoundingLyLst();
                    ttdd = ObsSoundingQuery.getObservedSndBufruaData(lat, lon,
                            stnId, refLtime, "TTDD", sndQuery)
                            .getSoundingLyLst();
                    ppbb = ObsSoundingQuery.getObservedSndBufruaData(lat, lon,
                            stnId, refLtime, "PPBB", sndQuery)
                            .getSoundingLyLst();
                    ppdd = ObsSoundingQuery.getObservedSndBufruaData(lat, lon,
                            stnId, refLtime, "PPDD", sndQuery)
                            .getSoundingLyLst();
                    wmax_a = ObsSoundingQuery.getObservedSndBufruaData(lat,
                            lon, stnId, refLtime, "MAXWIND_A", sndQuery)
                            .getSoundingLyLst();
                    wmax_c = ObsSoundingQuery.getObservedSndBufruaData(lat,
                            lon, stnId, refLtime, "MAXWIND_C", sndQuery)
                            .getSoundingLyLst();
                    trop_a = ObsSoundingQuery.getObservedSndBufruaData(lat,
                            lon, stnId, refLtime, "TROPOPAUSE_A", sndQuery)
                            .getSoundingLyLst();
                    trop_c = ObsSoundingQuery.getObservedSndBufruaData(lat,
                            lon, stnId, refLtime, "TROPOPAUSE_C", sndQuery)
                            .getSoundingLyLst();
                    pf = ObsSoundingQuery.getObservedSndStnInfo(lat, lon,
                            stnId, ObsSndType.BUFRUA.toString(), refLtime,
                            sndQuery);
                    sls = ms.mergeUairSounding(request.getLevel(), ttaa, ttbb,
                            ttcc, ttdd, ppaa, ppbb, ppcc, ppdd, trop_a, trop_c,
                            wmax_a, wmax_c, pf.getStationElevation(),
                            request.isWindInterpolation());

                    if (request.getLevel().toUpperCase()
                            .equalsIgnoreCase("MAN"))
                        pf.setSoundingLyLst(sls);
                    else if (ms.isNumber(request.getLevel()) >= 0) {
                        if (sls.size() == 1) {
                            pf.setSoundingLyLst(sls);
                        } else {
                            pf = null;
                        }
                    } else {
                        if (sls.isEmpty() || sls.size() <= 1) {
                            pf = null;
                        } else {
                            pf.setSoundingLyLst(sls);
                        }
                    }

                }
                if (pf != null && pf.getSoundingLyLst().size() > 0) {
                    soundingProfileList.add(pf);
                    pf = null;
                }
            }
        }
        if (soundingProfileList.size() == 0) {
            cube.setRtnStatus(NcSoundingCube.QueryStatus.FAILED);
        } else {
            cube.setRtnStatus(NcSoundingCube.QueryStatus.OK);
        }

        cube.setSoundingProfileList(soundingProfileList);
        return cube;
    }
}
