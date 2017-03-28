package gov.noaa.nws.ncep.edex.plugin.soundingrequest.handler;

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

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.measure.converter.UnitConverter;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import com.raytheon.edex.plugin.bufrua.dao.BufrUADao;
import com.raytheon.uf.common.dataplugin.bufrua.UAObs;
import com.raytheon.uf.common.datastorage.DataStoreFactory;
import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.common.datastorage.Request;
import com.raytheon.uf.common.datastorage.StorageException;
import com.raytheon.uf.common.datastorage.records.FloatDataRecord;
import com.raytheon.uf.common.datastorage.records.IntegerDataRecord;
import com.raytheon.uf.common.pointdata.PointDataContainer;
import com.raytheon.uf.edex.database.DataAccessLayerException;
import com.raytheon.uf.edex.database.dao.CoreDao;
import com.raytheon.uf.edex.database.dao.DaoConfig;
import com.raytheon.uf.edex.pointdata.PointDataQuery;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * 
 * This java class performs the observed sounding data query functions. This
 * code has been developed by the SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket# Engineer   Description
 * ------------ ------- ---------- -----------
 * Sep 13, 2010 301     Chin Chen  Initial coding
 * 10/2010      301     T. Lee     Checked missing value
 * Nov 05, 2010 301     Chin Chen  Update to support uairSnd query to all data
 *                                 types, except ALLDATA
 * Dec 16, 2010 301     Chin Chen  add support of BUFRUA observed sounding data
 * Sep 14, 2011 457     S. Gurung  Renamed h5 to nc
 * Oct 20, 2011         S. Gurung  Added ncuair changes related to replacing
 *                                 slat/slon/selv with location of type
 *                                 SurfaceObsLocation
 * Nov 2011             Chin Chen  changed Ncuair table query algorithm for
 *                                 performance improvement
 * Jan 05, 2012         S. Gurung  Removed references to UAIR (performed
 *                                 cleanup)
 * Feb 28, 2012         Chin Chen  modify several sounding query algorithms for
 *                                 better performance
 * Jul 19, 2013 1992    bsteffen   Remove redundant time columns from bufrua.
 * Aug 30, 2013 2298    rjpeter    Make getPluginName abstract
 * June, 2014           Chin Chen  Retrieved observed sounding with reftime
 * Oct 03, 2014         B. Hebbard Performance improvement:  getObservedSndNcUairDataGeneric( )
 *                                 limits DB param set to retrieve, in cases where possible
 *                                 (mandatory level and no PW-for-full-sounding)
 ***********************************************************************************************************
 *
 *
 * 05/20/2015   RM#8306 Chin Chen  eliminate NSHARP dependence on uEngine.
 *                                 Copy whole file ObservedSoundingQuery.java from uEngine project to this serverRequestService project
 *                                 "refactor" and clean up unused code for this ticket.
 * 07/02/2015   RM#8107 Chin Chen  change lat/lon data type from double to float to reflect its data type changes starting 14.4.1 
 * 07/21/2015   RM#9173 Chin Chen  Clean up NcSoundingQuery and Obsolete NcSoundingQuery2 and MergeSounging2
 *  07/19/2016  5736    bsteffen   Handle prSigW data.
 * 03/05/2017   18784   wkwock     Handle not integer stationID.
 * 
 * </pre>
 * 
 * @author Chin Chen
 * @version 1.0
 */
public class ObsSoundingQuery {
    private static final UnitConverter metersPerSecondToKnots = SI.METERS_PER_SECOND
            .getConverterTo(NonSI.KNOT);

    private static final UnitConverter kelvinToCelsius = SI.KELVIN
            .getConverterTo(SI.CELSIUS);

    private static final String NCUAIR_TBL_NAME = "ncuair";

    private static final String BURFUA_TBL_NAME = "bufrua";

    private static String currentDBTblName = "nil";

    /*
     * This method is for caller to get sounding station's info lat/lon/stn
     * id/stn num/elevation key: stn's lat/lon or stn ( either stn id or stn
     * num)
     */
    // checked used by handleBufruaDataRequest()
    @SuppressWarnings("unchecked")
    private static NcSoundingProfile getObservedSndStnInfo(Float lat, Float lon,
            String stn, String obType, long refTimeL,
            SndQueryKeyType queryType) {
        NcSoundingProfile pf = new NcSoundingProfile();
        CoreDao dao;
        List<String> fields = new ArrayList<String>();
        List<Object> values = new ArrayList<Object>();
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
            // values.add(refTimeCal.getTime());
            values.add(new Date(refTimeL)); // 8306
            dao = new CoreDao(DaoConfig.forClass(UAObs.class));
            try {
                lUairRecords = (List<UAObs>) dao.queryByCriteria(fields,
                        values);
                if (lUairRecords.size() > 0) {
                    pf.setStationLatitude(
                            (float) lUairRecords.get(0).getLatitude());
                    pf.setStationLongitude(
                            (float) lUairRecords.get(0).getLongitude());
                    pf.setStationElevation(lUairRecords.get(0).getElevation());
                    if ((lUairRecords.get(0).getStationId() != null)
                            && (lUairRecords.get(0).getStationId()
                                    .length() > 0)) {
                        pf.setStationNumStr(lUairRecords.get(0).getStationId());
                    }
                    pf.setStationId(lUairRecords.get(0).getStationName());
                    pf.setFcsTime(lUairRecords.get(0).getDataTime().getRefTime()
                            .getTime());
                }
            } catch (DataAccessLayerException e) {
                // *System.out.println("obs sounding query exception");
                e.printStackTrace();
            }
        }
        return pf;
    }

    // checked
    public static NcSoundingStnInfoCollection getObservedSndStnInfoCol(
            SoundingType sndType, String selectedSndTime) {
        NcSoundingStnInfoCollection stnInfoCol = new NcSoundingStnInfoCollection();
        List<NcSoundingStnInfo> stationInfoList = new ArrayList<NcSoundingStnInfo>();
        String queryStr, queryStr1;
        Object[] rtnobjArray, rtnobjArray1;
        CoreDao dao;
        if (sndType == SoundingType.OBS_BUFRUA_SND) {
            currentDBTblName = BURFUA_TBL_NAME;
            queryStr = new String(
                    "Select Distinct latitude, longitude, id, stationname, elevation, reftime FROM "
                            + currentDBTblName + " where reftime='"
                            + selectedSndTime
                            + "' AND latitude BETWEEN -89.9 AND 89.9 AND longitude BETWEEN -179.9 AND 179.9");
            queryStr1 = new String("Select Distinct latitude, longitude FROM "
                    + currentDBTblName + " where reftime='" + selectedSndTime
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
            queryStr1 = new String("Select Distinct latitude, longitude FROM "
                    + currentDBTblName + " where nil='FALSE' AND reftime='"
                    + selectedSndTime
                    + "' AND latitude BETWEEN -89.9 AND 89.9 AND longitude BETWEEN -179.9 AND 179.9");
            dao = new CoreDao(DaoConfig.forClass(NcUairRecord.class));
        } else {
            return stnInfoCol;
        }
        rtnobjArray = dao.executeSQLQuery(queryStr);

        // *System.out.println("size of rtnobjArray " + rtnobjArray.length);
        /*
         * Object[] obj; for (int i =0; i <rtnobjArray.length; i++){
         * System.out.println(" obj contents = "+ rtnobjArray[i]); obj =
         * (Object[] )rtnobjArray[i]; for (int j =0; j <rtnobjArray.length;
         * j++){ System.out.println(" obj contents = "+ obj[j].toString()); } }
         */

        rtnobjArray1 = dao.executeSQLQuery(queryStr1);

        // *System.out.println("size of rtnobjArray1 " + rtnobjArray1.length);
        if ((rtnobjArray1.length > 0) && (rtnobjArray.length > 0)) {
            float lat, lon, elv;
            String stnInfo;

            // System.out.println("queryAndMarkStn called mapresource = "+
            // nsharpMapResource.toString());
            // Note: A same station may have many reports and at some reports
            // they dont provide elv and or stnid
            // this implementation is "try" to make sure we get those info.
            // If, all reports does not have elv or stnid, then we still can not
            // report it.
            for (Object element : rtnobjArray1) {
                Object[] objArray1 = (Object[]) element;
                Timestamp synoptictime = null;
                stnInfo = "";
                elv = -999;
                /*
                 * if(obType.equals(ObsSndType.NCUAIR.toString())){ lat =
                 * (Float)objArray1[0]; lon = (Float)objArray1[1]; } else{
                 */
                lat = (Float) objArray1[0];
                lon = (Float) objArray1[1];
                // }
                // System.out.println("lat = "+ lat +" lon= "+lon+"\n\n");
                for (Object element2 : rtnobjArray) {
                    Object[] objArray = (Object[]) element2;
                    if ((sndType == SoundingType.OBS_UAIR_SND
                            && (lat == (Float) objArray[0])
                            && (lon == (Float) objArray[1]))
                            || ((sndType == SoundingType.OBS_BUFRUA_SND)
                                    && (lat == (Float) objArray[0])
                                    && (lon == (Float) objArray[1]))) {
                        // ids.add(((Integer)objArray[2]));
                        // System.out.println("id=" + (Integer)objArray[2]);
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
                        synoptictime = (Timestamp) objArray[5];
                    }

                }

                NcSoundingStnInfo stn = stnInfoCol.getNewStnInfo();
                stn.setStnId(stnInfo);
                stn.setStationLongitude(lon);
                stn.setStationLatitude(lat);
                stn.setStationElevation(elv);
                stn.setSynopTime(synoptictime);
                stationInfoList.add(stn);
                // System.out.println("stn "+ stnInfo + " lon "+ lon + " lat "+
                // lat);
            }
        }

        NcSoundingStnInfo[] stationInfoAry = new NcSoundingStnInfo[stationInfoList
                .size()];
        stnInfoCol.setStationInfo(stationInfoList.toArray(stationInfoAry));
        // *System.out.println("stn size = "+
        // stnInfoCol.getStationInfo().length);
        return stnInfoCol;
    }

    // checked
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
            queryStr = new String(
                    "Select Distinct reftime FROM " + currentDBTblName
                            + " where nil='FALSE' ORDER BY reftime DESC");
            dao = new CoreDao(DaoConfig.forClass(NcUairRecord.class));
        } else {
            return tl;
        }
        synopTimeAry = dao.executeSQLQuery(queryStr);
        // System.out.println("size of synoptictime " + synopTimeAry.length);

        // for(int i=0; i < synopTimeAry.length; i++){
        // if(synopTimeAry[i] != null)
        // System.out.println("synoptictime ="+synopTimeAry[i] );
        // }
        tl.setTimeLines(synopTimeAry);

        return tl;
    }

    /*
     * Chin: 2/21/2012 Using Lat/lon array OR StnId array, AND soundingTimeAry
     * (fcst time array) as input. This function is to be generic for all cases.
     * One and only one of latLonArray and stnIdArr should be not null and the
     * other one should be null soundingTimeAry is a list of refer time (in
     * NCUair, only use refer time for query) should be not null
     */
    // checked
    public static List<NcUairRecord[]> getObservedSndNcUairDataGeneric(
            Coordinate[] coordArray, String[] stnIdArray,
            String[] soundingTimeStrList, long[] soundTimeLongArr, String level,
            boolean pwRequired) {
        // List<NcSoundingProfile> soundingProfileList= new
        // ArrayList<NcSoundingProfile>();
        PointDataQuery request = null;
        PointDataContainer result = null;
        // NcUairRecord[] h5Records=null;
        String stnStr = "";
        Coordinate coord = new Coordinate();

        List<NcUairRecord> returnedDbRecords = new ArrayList<NcUairRecord>();
        List<NcUairRecord[]> finalRecordArrayList = new ArrayList<NcUairRecord[]>();
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
            boolean multipleStationsRequested = (coordArray != null
                    && coordArray.length > 1)
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
            d = d.substring(0, d.length() - 1);// get rid of last ","
            request.addParameter("dataTime.refTime", d, "in");
            if (coordArray != null) {
                String latStr = "", lonStr = "";
                for (Coordinate element : coordArray) {
                    latStr = latStr + String.valueOf(element.y) + ",";
                    lonStr = lonStr + String.valueOf(element.x) + ",";
                }
                latStr = latStr.substring(0, latStr.length() - 1);// get rid of
                                                                  // last ","
                lonStr = lonStr.substring(0, lonStr.length() - 1);// get rid of
                                                                  // last ","
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
            long t001 = System.currentTimeMillis();
            result = request.execute();
            long t002 = System.currentTimeMillis();
            // totalRqTime = totalRqTime + (t002 - t001);
            System.out.println(
                    "getObservedSndNcUairDataGeneric data query alone took "
                            + (t002 - t001) + "ms");

            if (result != null) {
                long t003 = System.currentTimeMillis();
                returnedDbRecords = NcUairToRecord.toNcUairRecordsList(result);

                if ((returnedDbRecords != null)
                        && (returnedDbRecords.size() > 0)) {
                    // System.out.println("getObservedSndNcUairDataGeneric
                    // Before loop: Number of records in
                    // returnedDbRecords="+returnedDbRecords.size());
                    // Chin: keep list of records for same station
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

                        List<List<NcUairRecord>> stnRecordsList = new ArrayList<List<NcUairRecord>>();
                        for (long refT : soundTimeLongArr) {
                            // create empty List<NcUairRecord> for each sounding
                            // time line
                            List<NcUairRecord> stnRecords = new ArrayList<NcUairRecord>();
                            stnRecordsList.add(stnRecords);
                        }
                        for (int j = returnedDbRecords.size()
                                - 1; j >= 0; j--) {
                            record = returnedDbRecords.get(j);
                            boolean goodRecord = false;
                            // System.out.println("requesting stn="+stnStr+"
                            // returned rd stnId="+record.getStationId()+
                            // " stnNum="+record.getStnum());
                            if (queryByStn == true) {
                                if (stnStr.equals(record.getStationId())) {
                                    // remove this record from return list and
                                    // add it to this stn list
                                    // stnRecords.add(returnedDbRecords.remove(j));
                                    goodRecord = true;
                                }
                            } else {
                                // System.out.println("coor.x="+coord.x +
                                // " coor.y="+coord.y +
                                // " record lon="+record.getLongitude()+
                                // " record lat="+ record.getLatitude());
                                // Chin: for some unknown reason that record
                                // returned from DB with lat/lon extended with
                                // many digits
                                // For example, coor.x=-114.4 coor.y=32.85
                                // record lon=-114.4000015258789 record
                                // lat=32.849998474121094
                                // Therefore, do the following.
                                if ((Math.abs(
                                        coord.x - record.getLongitude()) < 0.01)
                                        && (Math.abs(coord.y - record
                                                .getLatitude()) < 0.01)) {
                                    // remove this record from return list and
                                    // add it to this stn list
                                    // stnRecords.add(returnedDbRecords.remove(j));
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
                                // System.out.println("Before checking:stn
                                // lat="+lat
                                // +"stn record size="+stnRecords.size());
                                List<NcUairRecord> pickedUairRecords = new ArrayList<NcUairRecord>();
                                NcUairRecord orignalRd;
                                boolean addToList = true;
                                for (int ii = 0; ii < recordList.size(); ii++) {
                                    orignalRd = recordList.get(ii);
                                    addToList = true;
                                    for (NcUairRecord pickedRd : pickedUairRecords) {
                                        if (orignalRd.getDataType().equals(
                                                pickedRd.getDataType())) {
                                            // System.out.println("getObservedSndNcUairData:
                                            // at lat="+
                                            // lat+ " lon="+lon+
                                            // " find a same
                                            // datatype="+pickedRd.getDataType()+
                                            // " orignalRd
                                            // corr="+orignalRd.getCorr()+
                                            // " pickedRd
                                            // Corr="+pickedRd.getCorr());

                                            // the two records have same data
                                            // type
                                            // this records will either replace
                                            // the one in list or be dropped
                                            addToList = false;
                                            if ((pickedRd.getIssueTime()
                                                    .compareTo(orignalRd
                                                            .getIssueTime()) < 0)
                                                    || ((pickedRd.getIssueTime()
                                                            .compareTo(orignalRd
                                                                    .getIssueTime()) == 0)
                                                            && (orignalRd
                                                                    .getCorr() != null)
                                                            && (pickedRd
                                                                    .getCorr() != null)
                                                            && (pickedRd
                                                                    .getCorr()
                                                                    .compareTo(
                                                                            orignalRd
                                                                                    .getCorr()) < 0))
                                                    || ((pickedRd.getIssueTime()
                                                            .compareTo(orignalRd
                                                                    .getIssueTime()) == 0)
                                                            && (orignalRd
                                                                    .getCorr() != null)
                                                            && (pickedRd
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
                                                // System.out.println("getObservedSndNcUairData:
                                                // at lat="+
                                                // lat+ " lon="+lon+ " ori= " +
                                                // orignalRd.getDataURI()+
                                                // " picked="+
                                                // pickedRd.getDataURI());
                                                int pickedIndex = pickedUairRecords
                                                        .indexOf(pickedRd);
                                                pickedUairRecords.set(
                                                        pickedIndex, orignalRd);
                                                // System.out.println("getObservedSndNcUairData:
                                                // at lat="+
                                                // lat+ " lon="+lon+
                                                // " afterreplaced picked record
                                                // ="+pickedH5Records.get(pickedIndex).getDataURI());
                                            }
                                            break;
                                        }
                                    }
                                    if (addToList == true) {
                                        // add this original record to picked
                                        // list
                                        pickedUairRecords.add(orignalRd);
                                        // System.out.println("getObservedSndNcUairData:
                                        // at lat="+
                                        // lat+ " lon="+lon+
                                        // " add ori to picked record
                                        // ="+orignalRd.getDataURI());

                                    }
                                }
                                // pickedUairRecords.get(0).setNil(false); //
                                // set for special handling for its caller
                                finalRecordArrayList
                                        .add(pickedUairRecords.toArray(
                                                new NcUairRecord[pickedUairRecords
                                                        .size()]));
                                // System.out
                                // .println("getObservedSndNcUairDataGeneric
                                // Number of records in PF="
                                // + pickedUairRecords.size());
                            }
                        }
                    }

                }
                long t004 = System.currentTimeMillis();
                System.out.println(
                        " sorting return records took " + (t004 - t003) + "ms");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // System.out
        // .println("getObservedSndNcUairDataGeneric Number profiles (record[]s)
        // in finalRecordArrayList="
        // + finalRecordArrayList.size());
        return finalRecordArrayList;
    }

    private static boolean isMandatoryLevel(String level) {
        if (level == null) {
            return false;
        }
        // alternate: final String mandatoryLevels =
        // ".surface.1000.925.850.700.500.400.300.250.200.150.100.mb";
        // return mandatoryLevels.contains/* IgnoreCase */("." + level + ".");
        final String[] mandatoryLevels = { /* "surface", */"1000", "925", "850",
                "700", "500", "400", "300", "250", "200", "150", "100" };
        for (String s : mandatoryLevels) {
            if (s.equals/* IgnoreCase */(level)) {
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
        NcSoundingProfile pf = getObservedSndBufruaData(lat, lon, stn, refTimeL,
                "TTAA", queryType);
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
        // soundingLyLst = getObservedSndBufruaData(lat, lon, stn,refTimeCal,
        // "PPAA", queryType).getSoundingLyLst();
        // if (soundingLyLst.size() >= 0){
        // finalsoundingLyLst.addAll(soundingLyLst);
        // }
        soundingLyLst = getObservedSndBufruaData(lat, lon, stn, refTimeL,
                "PPBB", queryType).getSoundingLyLst();
        if (soundingLyLst.size() >= 0) {
            finalsoundingLyLst.addAll(soundingLyLst);
        }
        // soundingLyLst = getObservedSndBufruaData(lat, lon, stn,refTimeCal,
        // "PPCC", queryType).getSoundingLyLst();
        // if (soundingLyLst.size() >= 0){
        // finalsoundingLyLst.addAll(soundingLyLst);
        // }
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
    // checked
    @SuppressWarnings("unchecked")
    public static NcSoundingProfile getObservedSndBufruaData(Float lat,
            Float lon, String stn, long refTime, String dataType,
            SndQueryKeyType queryType) {
        // *System.out.println("getObservedSndBufruaData lat= " +
        // lat+" lon="+lon+" refTime="+refTimeCal );
        // Timestamp refTime = new Timestamp(refTimeL);
        // System.out.println("GMT ref time = "+ refTime.toGMTString());
        NcSoundingProfile pf = new NcSoundingProfile();
        List<NcSoundingLayer> soundLyList = new ArrayList<NcSoundingLayer>();
        if (dataType.equals(NcSoundingLayer.DataType.ALLDATA.toString())) {
            // *System.out.println("request all data is not supported in this
            // API");
            return pf;
        } else {
            List<String> fields = new ArrayList<String>();
            List<Object> values = new ArrayList<Object>();
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
            // fields.add("validTime");// the synoptic time field name defined
            // in UAObs
            // values.add(refTimeCal.getTime());
            values.add(new Date(refTime)); // 8306
            fields.add("reportType");// the record dataType field name defined
                                     // in UAObs
            int intDataType = NcSoundingLayer.dataTypeMap.get(dataType);
            values.add(intDataType);

            // for (int i=0; i < fields.size(); i++) {
            // System.out.println("field "+ fields.get(i) + " value "+
            // values.get(i));
            // }
            CoreDao dao = new CoreDao(DaoConfig.forClass(UAObs.class));
            try {
                lUairRecords = (List<UAObs>) dao.queryByCriteria(fields,
                        values);
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
                        // System.out.println("selected stn lon= " + lon +
                        // " lat = "+ lat + " elv = "+ pf.getStationElevation()
                        // + " h5 table Y index ="+ hdfIndex);
                        BufrUADao uadao = new BufrUADao("bufrua");
                        File hdf5loc = uadao.getFullFilePath(uairRecord);
                        // System.out.println("hdf5 path = " +
                        // hdf5loc.getAbsolutePath());
                        IDataStore dataStore = DataStoreFactory
                                .getDataStore(hdf5loc);

                        FloatDataRecord sfcPressurefloatData = (FloatDataRecord) dataStore
                                .retrieve("/", "sfcPressure",
                                        Request.buildYLineRequest(
                                                new int[] { hdfIndex }));
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
                        if (dataType
                                .equals(NcSoundingLayer.DataType.TTAA
                                        .toString())
                                || dataType.equals(NcSoundingLayer.DataType.TTCC
                                        .toString())) {
                            // get mandatory data size
                            IntegerDataRecord numManIntData = (IntegerDataRecord) dataStore
                                    .retrieve("/", "numMand",
                                            Request.buildYLineRequest(
                                                    new int[] { hdfIndex }));
                            int[] sizes = numManIntData.getIntData();
                            // sizes is a 1x1 2d table. Only first (0) element
                            // is valid.
                            if (sizes[0] > 0) {
                                FloatDataRecord pressurefloatData = (FloatDataRecord) dataStore
                                        .retrieve("/", "prMan",
                                                Request.buildYLineRequest(
                                                        new int[] {
                                                                hdfIndex }));
                                float[] pressuredata = pressurefloatData
                                        .getFloatData();
                                FloatDataRecord temperaturefloatData = (FloatDataRecord) dataStore
                                        .retrieve("/", "tpMan",
                                                Request.buildYLineRequest(
                                                        new int[] {
                                                                hdfIndex }));
                                float[] temperaturedata = temperaturefloatData
                                        .getFloatData();
                                FloatDataRecord dewptfloatData = (FloatDataRecord) dataStore
                                        .retrieve("/", "tdMan",
                                                Request.buildYLineRequest(
                                                        new int[] {
                                                                hdfIndex }));
                                float[] dewptdata = dewptfloatData
                                        .getFloatData();
                                FloatDataRecord windDfloatData = (FloatDataRecord) dataStore
                                        .retrieve("/", "wdMan",
                                                Request.buildYLineRequest(
                                                        new int[] {
                                                                hdfIndex }));
                                float[] windDdata = windDfloatData
                                        .getFloatData();
                                FloatDataRecord windSfloatData = (FloatDataRecord) dataStore
                                        .retrieve("/", "wsMan",
                                                Request.buildYLineRequest(
                                                        new int[] {
                                                                hdfIndex }));
                                float[] windSdata = windSfloatData
                                        .getFloatData();
                                FloatDataRecord htfloatData = (FloatDataRecord) dataStore
                                        .retrieve("/", "htMan",
                                                Request.buildYLineRequest(
                                                        new int[] {
                                                                hdfIndex }));
                                float[] htdata = htfloatData.getFloatData();
                                for (int i = 0; i < sizes[0]; i++) {
                                    soundingLy = new NcSoundingLayer();
                                    // if data is not available, dont convert it
                                    // and just use default setting data
                                    if (temperaturedata[i] != NcSoundingLayer.MISSING) {
                                        soundingLy.setTemperature(
                                                (float) kelvinToCelsius.convert(
                                                        temperaturedata[i]));
                                    }
                                    if (pressuredata[i] != NcSoundingLayer.MISSING) {
                                        soundingLy.setPressure(
                                                pressuredata[i] / 100F);
                                    }
                                    if (windSdata[i] != NcSoundingLayer.MISSING) {
                                        soundingLy.setWindSpeed(
                                                (float) metersPerSecondToKnots
                                                        .convert(windSdata[i]));
                                    }
                                    soundingLy.setWindDirection(windDdata[i]);
                                    if (dewptdata[i] != NcSoundingLayer.MISSING) {
                                        soundingLy.setDewpoint(
                                                (float) kelvinToCelsius
                                                        .convert(dewptdata[i]));
                                    }
                                    soundingLy.setGeoHeight(htdata[i]);
                                    soundLyList.add(soundingLy);
                                }
                                // debug
                                // for(NcSoundingLayer ly: soundLyList){
                                // System.out.println("Mandatory "+ dataType +
                                // ":: Pre= "+ly.getPressure()+ " Dew= "+
                                // ly.getDewpoint()+ " T= "+ ly.getTemperature()
                                // + " WS= " + ly.getWindSpeed() + " WD= " +
                                // ly.getWindDirection());
                                // }
                            } else {
                                System.out.println(
                                        "Mandatory data is not available! request data tye is "
                                                + dataType);
                            }
                        } else if (dataType
                                .equals(NcSoundingLayer.DataType.TTBB
                                        .toString())
                                || dataType.equals(NcSoundingLayer.DataType.TTDD
                                        .toString())) {
                            // get significantT data size
                            IntegerDataRecord numSigtIntData = (IntegerDataRecord) dataStore
                                    .retrieve("/", "numSigT",
                                            Request.buildYLineRequest(
                                                    new int[] { hdfIndex }));
                            int[] sizes = numSigtIntData.getIntData();
                            // sizes is a 1x1 2d table. Only first (0) element
                            // is valid.
                            if (sizes[0] > 0) {
                                FloatDataRecord pressurefloatData = (FloatDataRecord) dataStore
                                        .retrieve("/", "prSigT",
                                                Request.buildYLineRequest(
                                                        new int[] {
                                                                hdfIndex }));
                                float[] pressuredata = pressurefloatData
                                        .getFloatData();
                                FloatDataRecord temperaturefloatData = (FloatDataRecord) dataStore
                                        .retrieve("/", "tpSigT",
                                                Request.buildYLineRequest(
                                                        new int[] {
                                                                hdfIndex }));
                                float[] temperaturedata = temperaturefloatData
                                        .getFloatData();
                                FloatDataRecord dewptfloatData = (FloatDataRecord) dataStore
                                        .retrieve("/", "tdSigT",
                                                Request.buildYLineRequest(
                                                        new int[] {
                                                                hdfIndex }));
                                float[] dewptdata = dewptfloatData
                                        .getFloatData();
                                for (int i = 0; i < sizes[0]; i++) {
                                    soundingLy = new NcSoundingLayer();
                                    // if data is not available, dont convert it
                                    // and just use default setting data
                                    if (temperaturedata[i] != NcSoundingLayer.MISSING) {
                                        soundingLy.setTemperature(
                                                (float) kelvinToCelsius.convert(
                                                        temperaturedata[i]));
                                    }
                                    if (pressuredata[i] != NcSoundingLayer.MISSING) {
                                        soundingLy.setPressure(
                                                pressuredata[i] / 100F);
                                    }
                                    if (dewptdata[i] != NcSoundingLayer.MISSING) {
                                        soundingLy.setDewpoint(
                                                (float) kelvinToCelsius
                                                        .convert(dewptdata[i]));
                                    }
                                    soundLyList.add(soundingLy);
                                }
                                // for(NcSoundingLayer ly: soundLyList){
                                // System.out.println("SigT "+ dataType +
                                // ":: Pre= "+ly.getPressure()+ " Dew= "+
                                // ly.getDewpoint()+ " T= "+
                                // ly.getTemperature());
                                // }
                            } else {
                                System.out.println(
                                        "SigT data is not available! request data tye is "
                                                + dataType);
                            }

                        } else if (dataType
                                .equals(NcSoundingLayer.DataType.PPBB
                                        .toString())
                                || dataType.equals(NcSoundingLayer.DataType.PPDD
                                        .toString())) {
                            // get significantW data size
                            IntegerDataRecord numSigwIntData = (IntegerDataRecord) dataStore
                                    .retrieve("/", "numSigW",
                                            Request.buildYLineRequest(
                                                    new int[] { hdfIndex }));
                            int[] sizes = numSigwIntData.getIntData();
                            // sizes is a 1x1 2d table. Only first (0) element
                            // is valid.
                            if (sizes[0] > 0) {
                                FloatDataRecord htfloatData = (FloatDataRecord) dataStore
                                        .retrieve("/", "htSigW",
                                                Request.buildYLineRequest(
                                                        new int[] {
                                                                hdfIndex }));
                                float[] htdata = htfloatData.getFloatData();
                                float[] prdata = null;
                                try {
                                    FloatDataRecord prfloatData = (FloatDataRecord) dataStore
                                            .retrieve("/", "prSigW",
                                                    Request.buildYLineRequest(
                                                            new int[] {
                                                                    hdfIndex }));
                                    prdata = prfloatData.getFloatData();
                                } catch (StorageException e) {
                                    /*
                                     * This record does not exist in older data
                                     * so fill with MISSING.
                                     */
                                    prdata = new float[sizes[0]];
                                    Arrays.fill(prdata,
                                            NcSoundingLayer.MISSING);
                                }
                                FloatDataRecord windDfloatData = (FloatDataRecord) dataStore
                                        .retrieve("/", "wdSigW",
                                                Request.buildYLineRequest(
                                                        new int[] {
                                                                hdfIndex }));
                                float[] windDdata = windDfloatData
                                        .getFloatData();
                                FloatDataRecord windSfloatData = (FloatDataRecord) dataStore
                                        .retrieve("/", "wsSigW",
                                                Request.buildYLineRequest(
                                                        new int[] {
                                                                hdfIndex }));
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
                                    if (windSdata[i] != NcSoundingLayer.MISSING) {
                                        soundingLy.setWindSpeed(
                                                (float) metersPerSecondToKnots
                                                        .convert(windSdata[i]));
                                    }
                                    soundingLy.setWindDirection(windDdata[i]);
                                    soundLyList.add(soundingLy);
                                }
                                // for(NcSoundingLayer ly: soundLyList){
                                // System.out.println("SigW "+ dataType +
                                // ":: Ht= "+ly.getGeoHeight()+" WS= " +
                                // ly.getWindSpeed() + " WD= " +
                                // ly.getWindDirection());
                                // }
                            } else {
                                System.out.println(
                                        "SigW data is not available! request data tye is "
                                                + dataType);
                            }
                        } else if (dataType
                                .equals(NcSoundingLayer.DataType.MAXWIND_A
                                        .toString())
                                || dataType
                                        .equals(NcSoundingLayer.DataType.MAXWIND_C
                                                .toString())) {
                            // get max wind data size
                            IntegerDataRecord numMwndIntData = (IntegerDataRecord) dataStore
                                    .retrieve("/", "numMwnd",
                                            Request.buildYLineRequest(
                                                    new int[] { hdfIndex }));
                            int[] sizes = numMwndIntData.getIntData();
                            // sizes is a 1x1 2d table. Only first (0) element
                            // is valid.
                            if (sizes[0] > 0) {
                                FloatDataRecord pressurefloatData = (FloatDataRecord) dataStore
                                        .retrieve("/", "prMaxW",
                                                Request.buildYLineRequest(
                                                        new int[] {
                                                                hdfIndex }));
                                float[] pressuredata = pressurefloatData
                                        .getFloatData();
                                FloatDataRecord windDfloatData = (FloatDataRecord) dataStore
                                        .retrieve("/", "wdMaxW",
                                                Request.buildYLineRequest(
                                                        new int[] {
                                                                hdfIndex }));
                                float[] windDdata = windDfloatData
                                        .getFloatData();
                                FloatDataRecord windSfloatData = (FloatDataRecord) dataStore
                                        .retrieve("/", "wsMaxW",
                                                Request.buildYLineRequest(
                                                        new int[] {
                                                                hdfIndex }));
                                float[] windSdata = windSfloatData
                                        .getFloatData();
                                for (int i = 0; i < sizes[0]; i++) {
                                    soundingLy = new NcSoundingLayer();
                                    // if data is not available, dont convert it
                                    // and just use default setting data
                                    if (pressuredata[i] != NcSoundingLayer.MISSING) {
                                        soundingLy.setPressure(
                                                pressuredata[i] / 100F);
                                    }
                                    if (windSdata[i] != NcSoundingLayer.MISSING) {
                                        soundingLy.setWindSpeed(
                                                (float) metersPerSecondToKnots
                                                        .convert(windSdata[i]));
                                    }
                                    soundingLy.setWindDirection(windDdata[i]);
                                    soundLyList.add(soundingLy);
                                }
                                // for(NcSoundingLayer ly: soundLyList){
                                // System.out.println("MAXwind "+ dataType +
                                // ":: Pre= "+ly.getPressure()+ " WS= " +
                                // ly.getWindSpeed() + " WD= " +
                                // ly.getWindDirection());
                                // }
                            } else {
                                System.out.println(
                                        "max wind data is not available! request data tye is "
                                                + dataType);
                            }
                        } else if (dataType
                                .equals(NcSoundingLayer.DataType.TROPOPAUSE_A
                                        .toString())
                                || dataType
                                        .equals(NcSoundingLayer.DataType.TROPOPAUSE_C
                                                .toString())) {
                            // get troppause data size
                            IntegerDataRecord numTropIntData = (IntegerDataRecord) dataStore
                                    .retrieve("/", "numTrop",
                                            Request.buildYLineRequest(
                                                    new int[] { hdfIndex }));
                            int[] sizes = numTropIntData.getIntData();
                            // sizes is a 1x1 2d table. Only first (0) element
                            // is valid.
                            if (sizes[0] > 0) {
                                FloatDataRecord pressurefloatData = (FloatDataRecord) dataStore
                                        .retrieve("/", "prTrop",
                                                Request.buildYLineRequest(
                                                        new int[] {
                                                                hdfIndex }));
                                float[] pressuredata = pressurefloatData
                                        .getFloatData();
                                FloatDataRecord temperaturefloatData = (FloatDataRecord) dataStore
                                        .retrieve("/", "tpTrop",
                                                Request.buildYLineRequest(
                                                        new int[] {
                                                                hdfIndex }));
                                float[] temperaturedata = temperaturefloatData
                                        .getFloatData();
                                FloatDataRecord dewptfloatData = (FloatDataRecord) dataStore
                                        .retrieve("/", "tdTrop",
                                                Request.buildYLineRequest(
                                                        new int[] {
                                                                hdfIndex }));
                                float[] dewptdata = dewptfloatData
                                        .getFloatData();
                                FloatDataRecord windDfloatData = (FloatDataRecord) dataStore
                                        .retrieve("/", "wdTrop",
                                                Request.buildYLineRequest(
                                                        new int[] {
                                                                hdfIndex }));
                                float[] windDdata = windDfloatData
                                        .getFloatData();
                                FloatDataRecord windSfloatData = (FloatDataRecord) dataStore
                                        .retrieve("/", "wsTrop",
                                                Request.buildYLineRequest(
                                                        new int[] {
                                                                hdfIndex }));
                                float[] windSdata = windSfloatData
                                        .getFloatData();
                                for (int i = 0; i < sizes[0]; i++) {
                                    soundingLy = new NcSoundingLayer();
                                    // if data is not available, dont convert it
                                    // and just use default setting data
                                    if (temperaturedata[i] != NcSoundingLayer.MISSING) {
                                        soundingLy.setTemperature(
                                                (float) kelvinToCelsius.convert(
                                                        temperaturedata[i]));
                                    }
                                    if (pressuredata[i] != NcSoundingLayer.MISSING) {
                                        soundingLy.setPressure(
                                                pressuredata[i] / 100F);
                                    }
                                    if (windSdata[i] != NcSoundingLayer.MISSING) {
                                        soundingLy.setWindSpeed(
                                                (float) metersPerSecondToKnots
                                                        .convert(windSdata[i]));
                                    }
                                    soundingLy.setWindDirection(windDdata[i]);
                                    if (dewptdata[i] != NcSoundingLayer.MISSING) {
                                        soundingLy.setDewpoint(
                                                (float) kelvinToCelsius
                                                        .convert(dewptdata[i]));
                                    }
                                    soundLyList.add(soundingLy);
                                }
                                // debug
                                // for(NcSoundingLayer ly: soundLyList){
                                // System.out.println("Troppause "+ dataType +
                                // ":: Pre= "+ly.getPressure()+ " Dew= "+
                                // ly.getDewpoint()+ " T= "+ ly.getTemperature()
                                // + " WS= " + ly.getWindSpeed() + " WD= " +
                                // ly.getWindDirection());
                                // }
                            } else {
                                System.out.println(
                                        "Troppause data is not available! request data tye is "
                                                + dataType);
                            }
                        }

                    } else {
                        System.out
                                .println("hdf5 index (idx) is less than 0!!!");
                        return pf;
                    }
                } else {
                    System.out.println(
                            "buffrua (UAOb) record is not available!! request type "
                                    + dataType);
                    return pf;
                }

            } catch (Exception e) {
                // *System.out.println("exception=" + e );
                e.printStackTrace();
                return pf;
            }
            // *System.out.println("sounding layer size = "+
            // soundLyList.size());

            pf.setSoundingLyLst(soundLyList);

            return pf;

        }
    }

    private static List<NcSoundingProfile> processQueryReturnedNcUairData(
            List<NcUairRecord[]> uairRecordArrList,
            /* boolean useNcSndLayer2, */ boolean merge, String level,
            boolean pwRequired) {
        List<NcSoundingProfile> soundingProfileList = new ArrayList<NcSoundingProfile>(
                0);
        for (NcUairRecord[] recordArray : uairRecordArrList) {

            NcSoundingProfile pf;

            if (merge == false) {
                // Chin...need more coding
                pf = null;
            } else {
                pf = new NcSoundingProfile();
                // if (useNcSndLayer2 == true) {
                // use NcSoundingLayer2
                // if (recordArray != null && recordArray.length > 0) {
                // MergeSounding2 ms2 = new MergeSounding2();
                // List<NcSoundingLayer2> sls = new
                // ArrayList<NcSoundingLayer2>();
                // List<NcSoundingLayer2> ttaa = new
                // ArrayList<NcSoundingLayer2>();
                // List<NcSoundingLayer2> ttbb = new
                // ArrayList<NcSoundingLayer2>();
                // List<NcSoundingLayer2> ttcc = new
                // ArrayList<NcSoundingLayer2>();
                // List<NcSoundingLayer2> ttdd = new
                // ArrayList<NcSoundingLayer2>();
                // List<NcSoundingLayer2> ppaa = new
                // ArrayList<NcSoundingLayer2>();
                // List<NcSoundingLayer2> ppbb = new
                // ArrayList<NcSoundingLayer2>();
                // List<NcSoundingLayer2> ppcc = new
                // ArrayList<NcSoundingLayer2>();
                // List<NcSoundingLayer2> ppdd = new
                // ArrayList<NcSoundingLayer2>();
                // List<NcSoundingLayer2> trop_a = new
                // ArrayList<NcSoundingLayer2>();
                // List<NcSoundingLayer2> trop_c = new
                // ArrayList<NcSoundingLayer2>();
                // List<NcSoundingLayer2> wmax_a = new
                // ArrayList<NcSoundingLayer2>();
                // List<NcSoundingLayer2> wmax_c = new
                // ArrayList<NcSoundingLayer2>();
                //
                // for (int k = 0; k < recordArray.length; k++) {
                // NcUairRecord record = recordArray[k];
                // if (record.getDataType().equals("TTAA")
                // || record.getDataType().equals("XXAA")) {
                // ttaa = getSoundingLayer2FromNcUairRecordObsLevel(record);
                // trop_a = getSoundingLayer2FromNcUairRecordTrop(record);
                // wmax_a = getSoundingLayer2FromNcUairRecordMaxw(record);
                // } else if (record.getDataType().equals("TTBB")
                // || record.getDataType().equals("XXBB")) {
                // ttbb = getSoundingLayer2FromNcUairRecordObsLevel(record);
                // } else if (record.getDataType().equals("TTCC")
                // || record.getDataType().equals("XXCC")) {
                // ttcc = getSoundingLayer2FromNcUairRecordObsLevel(record);
                // trop_c = getSoundingLayer2FromNcUairRecordTrop(record);
                // wmax_c = getSoundingLayer2FromNcUairRecordMaxw(record);
                // } else if (record.getDataType().equals("TTDD")
                // || record.getDataType().equals("XXDD")) {
                // ttdd = getSoundingLayer2FromNcUairRecordObsLevel(record);
                // } else if (record.getDataType().equals("PPAA")) {
                // ppaa = getSoundingLayer2FromNcUairRecordObsLevel(record);
                // } else if (record.getDataType().equals("PPBB")) {
                // ppbb = getSoundingLayer2FromNcUairRecordObsLevel(record);
                // } else if (record.getDataType().equals("PPCC")) {
                // ppcc = getSoundingLayer2FromNcUairRecordObsLevel(record);
                // } else if (record.getDataType().equals("PPDD")) {
                // ppdd = getSoundingLayer2FromNcUairRecordObsLevel(record);
                // }
                // }
                // pf.setStationElevation((float) recordArray[0]
                // .getElevation());
                // pf.setStationId(recordArray[0].getStationId());
                // if (recordArray[0].getStnum() != null
                // && recordArray[0].getStnum().length() > 0)
                // pf.setStationNum(Integer.parseInt(recordArray[0]
                // .getStnum()));
                // pf.setStationLatitude(recordArray[0].getLatitude());
                // pf.setStationLongitude(recordArray[0].getLongitude());
                // pf.setFcsTime(recordArray[0].getDataTime().getRefTime()
                // .getTime());
                // // System.out.println("m2 input lat=" + lat +
                // // " pf's lat="
                // // + pf.getStationLatitude() + " elv="
                // // + pf.getStationElevation() + " stnId="
                // // + pf.getStationId());
                // if (useNcSndLayer2)
                // sls = ms2.mergeUairSounding(level, ttaa, ttbb,
                // ttcc, ttdd, ppaa, ppbb, ppcc, ppdd, trop_a,
                // trop_c, wmax_a, wmax_c,
                // pf.getStationElevation());
                //
                // if(pwRequired){
                // List<NcSoundingLayer2> sls2 = new
                // ArrayList<NcSoundingLayer2>();
                // sls2 = ms2.mergeUairSounding("-1", ttaa, ttbb,
                // ttcc, ttdd, ppaa, ppbb, ppcc, ppdd,
                // trop_a, trop_c, wmax_a, wmax_c,
                // pf.getStationElevation());
                // if (sls2 != null && sls2.size() > 0)
                // pf.setPw(NcSoundingTools
                // .precip_water2(sls2));
                // else
                // pf.setPw(-1);
                // }
                //
                // if (level.toUpperCase().equalsIgnoreCase("MAN")) {
                // pf.setSoundingLyLst2(sls);
                // // System.out.println("sls set to the sounding profile");
                // } else if (ms2.isNumber(level) >= 0) {
                // if (sls.size() == 1) {
                // // System.out.println("NcUair get one layer using level = "+
                // // level);
                // pf.setSoundingLyLst2(sls);
                // } else {
                // pf = null;
                // // System.out.println("NcUair get 0 layer using level = "+
                // // level);
                // }
                // } else {
                // if (sls.isEmpty() || sls.size() <= 1) {
                // pf = null;
                // // System.out.println("not MAN level & sls is empty or 1");
                // } else {
                // pf.setSoundingLyLst2(sls);
                // // System.out.println("sls set to the sounding profile for
                // level = "
                // // + level);
                // }
                // }
                // }
                // } else
                {
                    // use NcSoundingLayer
                    if (recordArray != null && recordArray.length > 0) {
                        MergeSounding ms = new MergeSounding();
                        List<NcSoundingLayer> sls = new ArrayList<NcSoundingLayer>();
                        List<NcSoundingLayer> ttaa = new ArrayList<NcSoundingLayer>();
                        List<NcSoundingLayer> ttbb = new ArrayList<NcSoundingLayer>();
                        List<NcSoundingLayer> ttcc = new ArrayList<NcSoundingLayer>();
                        List<NcSoundingLayer> ttdd = new ArrayList<NcSoundingLayer>();
                        List<NcSoundingLayer> ppaa = new ArrayList<NcSoundingLayer>();
                        List<NcSoundingLayer> ppbb = new ArrayList<NcSoundingLayer>();
                        List<NcSoundingLayer> ppcc = new ArrayList<NcSoundingLayer>();
                        List<NcSoundingLayer> ppdd = new ArrayList<NcSoundingLayer>();
                        List<NcSoundingLayer> trop_a = new ArrayList<NcSoundingLayer>();
                        List<NcSoundingLayer> trop_c = new ArrayList<NcSoundingLayer>();
                        List<NcSoundingLayer> wmax_a = new ArrayList<NcSoundingLayer>();
                        List<NcSoundingLayer> wmax_c = new ArrayList<NcSoundingLayer>();
                        for (int k = 0; k < recordArray.length; k++) {
                            NcUairRecord record = recordArray[k];
                            if (record.getDataType().equals("TTAA")
                                    || record.getDataType().equals("XXAA")) {
                                ttaa = getSndLayersFromNcUairRecordObsLevel(
                                        record);
                                trop_a = getSndLayersFromNcUairRecordTrop(
                                        record);
                                wmax_a = getSndLayersFromNcUairRecordMaxw(
                                        record);
                            } else if (record.getDataType().equals("TTBB")
                                    || record.getDataType().equals("XXBB")) {
                                ttbb = getSndLayersFromNcUairRecordObsLevel(
                                        record);
                            } else if (record.getDataType().equals("TTCC")
                                    || record.getDataType().equals("XXCC")) {
                                ttcc = getSndLayersFromNcUairRecordObsLevel(
                                        record);
                                trop_c = getSndLayersFromNcUairRecordTrop(
                                        record);
                                wmax_c = getSndLayersFromNcUairRecordMaxw(
                                        record);
                            } else if (record.getDataType().equals("TTDD")
                                    || record.getDataType().equals("XXDD")) {
                                ttdd = getSndLayersFromNcUairRecordObsLevel(
                                        record);
                            } else if (record.getDataType().equals("PPAA")) {
                                ppaa = getSndLayersFromNcUairRecordObsLevel(
                                        record);
                            } else if (record.getDataType().equals("PPBB")) {
                                ppbb = getSndLayersFromNcUairRecordObsLevel(
                                        record);
                            } else if (record.getDataType().equals("PPCC")) {
                                ppcc = getSndLayersFromNcUairRecordObsLevel(
                                        record);
                            } else if (record.getDataType().equals("PPDD")) {
                                ppdd = getSndLayersFromNcUairRecordObsLevel(
                                        record);
                            }
                        }
                        pf = new NcSoundingProfile();
                        pf.setStationElevation(
                                (float) recordArray[0].getElevation());
                        pf.setStationId(recordArray[0].getStationId());
                        if (recordArray[0].getStnum() != null
                                && recordArray[0].getStnum().length() > 0)
                            pf.setStationNumStr(recordArray[0].getStnum());
                        pf.setStationLatitude(recordArray[0].getLatitude());
                        pf.setStationLongitude(recordArray[0].getLongitude());
                        pf.setFcsTime(recordArray[0].getDataTime().getRefTime()
                                .getTime());
                        // System.out.println(" input lat="+lat+" pf's
                        // lat="+pf.getStationLatitude()+"
                        // elv="+pf.getStationElevation()+"
                        // stnId="+pf.getStationId());
                        sls = ms.mergeUairSounding(level, ttaa, ttbb, ttcc,
                                ttdd, ppaa, ppbb, ppcc, ppdd, trop_a, trop_c,
                                wmax_a, wmax_c, pf.getStationElevation());
                        // PW Support test
                        if (pwRequired) {
                            if (level.equals("-1")) {
                                float pw = NcSoundingTools.precip_water(sls);
                                pf.setPw(pw);
                            } else {
                                List<NcSoundingLayer> sls2 = new ArrayList<NcSoundingLayer>();
                                sls2 = ms.mergeUairSounding("-1", ttaa, ttbb,
                                        ttcc, ttdd, ppaa, ppbb, ppcc, ppdd,
                                        trop_a, trop_c, wmax_a, wmax_c,
                                        pf.getStationElevation());
                                if (sls2 != null && sls2.size() > 0)
                                    pf.setPw(
                                            NcSoundingTools.precip_water(sls2));
                            }
                        }
                        // end PW Support test
                        // System.out.println("NCUAIR Number of Layers after
                        // merge:"+sls.size()
                        // + " level="+level +
                        // " ms.isNumber(level)="+ms.isNumber(level));
                        // for(NcSoundingLayer ly: sls){
                        // System.out.println("Pre= "+ly.getPressure()+
                        // " Dew= "+ ly.getDewpoint()+ " T= "+
                        // ly.getTemperature()+" H="+ly.getGeoHeight()+"
                        // WSp="+ly.getWindSpeed());
                        // }

                        if (level.toUpperCase().equalsIgnoreCase("MAN"))
                            pf.setSoundingLyLst(sls);
                        else if (ms.isNumber(level) >= 0) {
                            if (sls.size() == 1) {
                                // System.out.println("NCUAIR get one layer
                                // using level = "+
                                // level);
                                pf.setSoundingLyLst(sls);
                            } else {
                                pf = null;
                                // System.out.println("NCUAIR get 0 layer using
                                // level = "+
                                // level);
                            }
                        } else {
                            if (sls.isEmpty() || sls.size() <= 1)
                                pf = null;
                            else
                                pf.setSoundingLyLst(sls);
                        }
                    } else
                        pf = null;
                }
                if (pf != null && (pf.getSoundingLyLst2().size() > 0
                        || pf.getSoundingLyLst().size() > 0)) {
                    // System.out.println(" pf is not null, so adding a profile
                    // to the list of NcSoundingProfiles ");
                    soundingProfileList.add(pf);
                    pf = null;
                }
            }
        }
        return soundingProfileList;
    }

    private static List<NcSoundingLayer> getSndLayersFromNcUairRecordObsLevel(
            NcUairRecord record) {
        List<NcSoundingLayer> sndLayers = new ArrayList<NcSoundingLayer>();
        Set<NcUairObsLevels> obLevels = record.getObsLevels();
        // System.out.println("The datauri for this record is: " +
        // record.getDataURI() );
        if (obLevels.size() > 0) {
            for (NcUairObsLevels obLev : obLevels) {
                NcSoundingLayer sndLayer = new NcSoundingLayer();
                sndLayer.setTemperature(obLev.getTemp());
                sndLayer.setDewpoint(obLev.getDwpt());
                sndLayer.setGeoHeight(obLev.getHght());
                // System.out.println("Sounding layer height = " +
                // sndLayer.getGeoHeight() );
                sndLayer.setPressure(obLev.getPres());
                sndLayer.setWindDirection(obLev.getDrct());
                if (obLev.getSped() >= 0)
                    sndLayer.setWindSpeed((float) metersPerSecondToKnots
                            .convert(obLev.getSped()));
                else
                    sndLayer.setWindSpeed(obLev.getSped());
                sndLayers.add(sndLayer);
            }
        }
        // System.out.println("ObsLevel="+obLevels.size()+"
        // sndLayers="+sndLayers.size());
        return sndLayers;
    }

    // private static List<NcSoundingLayer2>
    // getSoundingLayer2FromNcUairRecordObsLevel(
    // NcUairRecord record) {
    // List<NcSoundingLayer2> sndLayers = new ArrayList<NcSoundingLayer2>();
    // Set<NcUairObsLevels> obLevels = record.getObsLevels();
    //
    // // System.out.println("The datatype for this record is: " +
    // // record.getDataType() );
    // if (obLevels.size() > 0) {
    // for (NcUairObsLevels obLev : obLevels) {
    // // System.out.println("\n\nFrom NcUairObsLevel:");
    // // System.out.println("Temperature = " + obLev.getTemp());
    // // System.out.println("Pressure = " + obLev.getPres());
    // // System.out.println("Dewpoint = " + obLev.getDwpt());
    // // System.out.println("Height = " + obLev.getHght());
    // // System.out.println("Wind direction = " + obLev.getDrct());
    // // System.out.println("Wind speed in m/s= " + obLev.getSped());
    // try {
    // NcSoundingLayer2 sndLayer = new NcSoundingLayer2();
    // /*
    // * (Non-Javadoc) The units for each quantity are chosen
    // * based upon the units defined for these quantities in the
    // * pointdata description file for NcUair
    // */
    // AirTemperature airTemp = new AirTemperature();
    // airTemp.setValue(new Amount(obLev.getTemp(), SI.CELSIUS));
    // DewPointTemp dewPoint = new DewPointTemp();
    // dewPoint.setValue(new Amount(obLev.getDwpt(), SI.CELSIUS));
    // HeightAboveSeaLevel height = new HeightAboveSeaLevel();
    // height.setValue(obLev.getHght(), SI.METER);
    //
    // // System.out.println("Sounding layer height = " +
    // // sndLayer.getGeoHeight().doubleValue() );
    // PressureLevel pressure = new PressureLevel();
    // pressure.setValue(new Amount(obLev.getPres(),
    // NcUnits.MILLIBAR));
    //
    // WindDirection windDirection = new WindDirection();
    // windDirection.setValue(obLev.getDrct(), NonSI.DEGREE_ANGLE);
    // WindSpeed windSpeed = new WindSpeed();
    // float speed = obLev.getSped();
    //
    // /*
    // * ( Non-Javadoc ) There are no negative speed values
    // * decoded except for either -999 or -9999 to indicate that
    // * the speed is missing. The check for the positive speed
    // * value ensures that the unit conversion happens for
    // * non-missing speed values.
    // */
    // if (speed >= 0) {
    // double convertedSpeed = metersPerSecondToKnots
    // .convert(speed);
    // windSpeed.setValue(convertedSpeed, NonSI.KNOT);
    // } else {
    // windSpeed.setValueToMissing();
    // }
    //
    // // System.out.println("\nFrom MetParameters:");
    // // System.out.println("Temperature = " +
    // // airTemp.getValue().floatValue());
    // // System.out.println("Pressure = " +
    // // pressure.getValue().floatValue());
    // // System.out.println("Dewpoint = " +
    // // dewPoint.getValue().floatValue());
    // // System.out.println("Height = " +
    // // height.getValue().floatValue());
    // // System.out.println("Wind direction = " +
    // // windDirection.getValue().floatValue());
    // // System.out.println("Wind speed = " +
    // // windSpeed.getValue().floatValue());
    // sndLayer.setTemperature(airTemp);
    // sndLayer.setPressure(pressure);
    // sndLayer.setDewpoint(dewPoint);
    // sndLayer.setGeoHeight(height);
    // sndLayer.setWindDirection(windDirection);
    // sndLayer.setWindSpeed(windSpeed);
    // sndLayers.add(sndLayer);
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    // }
    // }
    // // System.out.println("ObsLevel="+obLevels.size()+"
    // sndLayers="+sndLayers.size());
    // return sndLayers;
    // }
    private static List<NcSoundingLayer> getSndLayersFromNcUairRecordTrop(
            NcUairRecord record) {
        List<NcSoundingLayer> sndLayers = new ArrayList<NcSoundingLayer>();
        Set<NcUairTropopause> trops = record.getTropopause();
        if (trops.size() > 0) {
            for (NcUairTropopause trop : trops) {
                NcSoundingLayer sndLayer = new NcSoundingLayer();
                sndLayer.setTemperature(trop.getTemp());
                sndLayer.setDewpoint(trop.getDwpt());
                sndLayer.setPressure(trop.getPres());
                sndLayer.setWindDirection(trop.getDrct());
                if (trop.getSped() >= 0)
                    sndLayer.setWindSpeed((float) metersPerSecondToKnots
                            .convert(trop.getSped()));
                else
                    sndLayer.setWindSpeed(trop.getSped());
                sndLayers.add(sndLayer);
            }
        }
        // System.out.println("trops="+trops.size()+"
        // sndLayers="+sndLayers.size());
        return sndLayers;
    }

    // private static List<NcSoundingLayer2>
    // getSoundingLayer2FromNcUairRecordTrop(
    // NcUairRecord record) {
    // List<NcSoundingLayer2> sndLayers = new ArrayList<NcSoundingLayer2>();
    // Set<NcUairTropopause> trops = record.getTropopause();
    // if (trops.size() > 0) {
    // for (NcUairTropopause trop : trops) {
    // try {
    // NcSoundingLayer2 sndLayer = new NcSoundingLayer2();
    // /*
    // * (Non-Javadoc) The units for each quantity are chosen
    // * based upon the units defined for these quantities in the
    // * pointdata description file for NcUair
    // */
    // AirTemperature airTemp = new AirTemperature();
    // airTemp.setValue(new Amount(trop.getTemp(), SI.CELSIUS));
    // DewPointTemp dewPoint = new DewPointTemp();
    // dewPoint.setValue(new Amount(trop.getDwpt(), SI.CELSIUS));
    // PressureLevel pressure = new PressureLevel();
    // pressure.setValue(new Amount(trop.getPres(),
    // NcUnits.MILLIBAR));
    // WindDirection windDirection = new WindDirection();
    // windDirection.setValue(trop.getDrct(), NonSI.DEGREE_ANGLE);
    // WindSpeed windSpeed = new WindSpeed();
    // float speed = trop.getSped();
    // /*
    // * ( Non-Javadoc ) There are no negative speed values
    // * decoded except for either -999 or -9999 to indicate that
    // * the speed is missing. The check for the positive speed
    // * value ensures that the unit conversion happens for
    // * non-missing speed values.
    // */
    // if (speed >= 0) {
    // double convertedSpeed = metersPerSecondToKnots
    // .convert(speed);
    // windSpeed.setValue(convertedSpeed, NonSI.KNOT);
    // } else {
    // windSpeed.setValueToMissing();
    // }
    // sndLayer.setTemperature(airTemp);
    // sndLayer.setPressure(pressure);
    // sndLayer.setDewpoint(dewPoint);
    // sndLayer.setWindDirection(windDirection);
    // sndLayer.setWindSpeed(windSpeed);
    // sndLayers.add(sndLayer);
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    // }
    // }
    // // System.out.println("trops="+trops.size()+"
    // sndLayers="+sndLayers.size());
    // return sndLayers;
    // }

    private static List<NcSoundingLayer> getSndLayersFromNcUairRecordMaxw(
            NcUairRecord record) {
        List<NcSoundingLayer> sndLayers = new ArrayList<NcSoundingLayer>();
        Set<NcUairMaxWind> maxWinds = record.getMaxWind();
        if (maxWinds.size() > 0) {
            for (NcUairMaxWind maxWind : maxWinds) {
                NcSoundingLayer sndLayer = new NcSoundingLayer();
                sndLayer.setPressure(maxWind.getPres());
                sndLayer.setWindDirection(maxWind.getDrct());
                if (maxWind.getSped() >= 0)
                    sndLayer.setWindSpeed((float) metersPerSecondToKnots
                            .convert(maxWind.getSped()));
                else
                    sndLayer.setWindSpeed(maxWind.getSped());
                sndLayers.add(sndLayer);
            }
        }
        // System.out.println("maxWinds="+maxWinds.size()+"
        // sndLayers="+sndLayers.size());
        return sndLayers;
    }

    // private static List<NcSoundingLayer2>
    // getSoundingLayer2FromNcUairRecordMaxw(
    // NcUairRecord record) {
    // List<NcSoundingLayer2> sndLayers = new ArrayList<NcSoundingLayer2>();
    // Set<NcUairMaxWind> maxWinds = record.getMaxWind();
    // if (maxWinds.size() > 0) {
    // /*
    // * (Non-Javadoc) The units for each quantity are chosen based upon
    // * the units defined for these quantities in the pointdata
    // * description file for NcUair
    // */
    // for (NcUairMaxWind maxWind : maxWinds) {
    // try {
    // NcSoundingLayer2 sndLayer = new NcSoundingLayer2();
    // PressureLevel pressure = new PressureLevel();
    // // pressure.setValueAs(maxWind.getPres(), "hPa" );
    // pressure.setValue(new Amount(maxWind.getPres(),
    // NcUnits.MILLIBAR));
    // WindDirection windDirection = new WindDirection();
    // windDirection.setValue(maxWind.getDrct(),
    // NonSI.DEGREE_ANGLE);
    // WindSpeed windSpeed = new WindSpeed();
    // float speed = maxWind.getSped();
    // /*
    // * ( Non-Javadoc ) There are no negative speed values
    // * decoded except for either -999 or -9999 to indicate that
    // * the speed is missing. The check for the positive speed
    // * value ensures that the unit conversion happens for
    // * non-missing speed values.
    // */
    // if (speed >= 0) {
    // double convertedSpeed = metersPerSecondToKnots
    // .convert(speed);
    // windSpeed.setValue(convertedSpeed, NonSI.KNOT);
    // } else {
    // windSpeed.setValueToMissing();
    // }
    // sndLayer.setPressure(pressure);
    // sndLayer.setWindDirection(windDirection);
    // sndLayer.setWindSpeed(windSpeed);
    // sndLayers.add(sndLayer);
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    // }
    // }
    // // System.out.println("maxWinds="+maxWinds.size()+"
    // sndLayers="+sndLayers.size());
    // return sndLayers;
    // }

    public static NcSoundingCube handleNcuairDataRequest(
            SoundingServiceRequest request) {
        // SoundingRequestType reqType = request.getReqType();
        // SoundingType sndType = request.getSndType();
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
                            /* request.isUseNcSoundingLayer2() */request
                                    .isMerge(),
                            request.getLevel(), request.isPwRequired());
            NcSoundingCube cube = new NcSoundingCube();
            cube.setSoundingProfileList(soundingProfileList);
            cube.setRtnStatus(NcSoundingCube.QueryStatus.OK);
            return (cube);
        } else
            return null;
    }

    public static NcSoundingCube handleBufruaDataRequest(
            SoundingServiceRequest request) {
        int arrLen = 0;
        SndQueryKeyType sndQuery;
        NcSoundingCube cube = new NcSoundingCube();
        List<NcSoundingProfile> soundingProfileList = new ArrayList<NcSoundingProfile>();
        Coordinate[] latLonArray = request.getLatLonAry();
        String[] stnIdArr = request.getStnIdAry();
        if (latLonArray != null) {
            arrLen = latLonArray.length;
            sndQuery = SndQueryKeyType.LATLON;
        } else if (stnIdArr != null) {
            arrLen = stnIdArr.length;
            sndQuery = SndQueryKeyType.STNID;
        } else
            return null;
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
            List<NcSoundingLayer> sls = new ArrayList<NcSoundingLayer>();
            List<NcSoundingLayer> ttaa = new ArrayList<NcSoundingLayer>();
            List<NcSoundingLayer> ttbb = new ArrayList<NcSoundingLayer>();
            List<NcSoundingLayer> ttcc = new ArrayList<NcSoundingLayer>();
            List<NcSoundingLayer> ttdd = new ArrayList<NcSoundingLayer>();
            List<NcSoundingLayer> ppaa = new ArrayList<NcSoundingLayer>();
            List<NcSoundingLayer> ppbb = new ArrayList<NcSoundingLayer>();
            List<NcSoundingLayer> ppcc = new ArrayList<NcSoundingLayer>();
            List<NcSoundingLayer> ppdd = new ArrayList<NcSoundingLayer>();
            List<NcSoundingLayer> trop_a = new ArrayList<NcSoundingLayer>();
            List<NcSoundingLayer> trop_c = new ArrayList<NcSoundingLayer>();
            List<NcSoundingLayer> wmax_a = new ArrayList<NcSoundingLayer>();
            List<NcSoundingLayer> wmax_c = new ArrayList<NcSoundingLayer>();
            long[] refTimeArray = request.getRefTimeAry();
            if (refTimeArray == null) {
                if (request.getRefTimeStrAry() == null)
                    return null;
                String[] refTimeStrArray = request.getRefTimeStrAry();
                refTimeArray = QueryMiscTools
                        .convertTimeStrArrayToLongArray(refTimeStrArray);
            }
            for (long refLtime : refTimeArray) {
                if (request.isMerge() == false) {
                    // *System.out.println ( " Request unmerged data");
                    pf = ObsSoundingQuery.getObservedSndBufruaAllData(lat, lon,
                            stnId, refLtime, sndQuery);

                } else {

                    // Get TTAA. If not existent, try ship data (UUAA). If
                    // level is not null or missing,
                    // the body of code will return a sounding list with MAN
                    // data or single level data.
                    // *System.out.println ( " Request merged data at lat="+
                    // lat+" lon="+lon+ " refT="+
                    // timeCal.getTime().toGMTString());

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
                    // ppaa =
                    // ObsSoundingQuery.getObservedSndBufruaData(lat,
                    // lon, stnId, timeCal, "PPAA",
                    // sndQuery).getSoundingLyLst();
                    ppbb = ObsSoundingQuery.getObservedSndBufruaData(lat, lon,
                            stnId, refLtime, "PPBB", sndQuery)
                            .getSoundingLyLst();
                    // ppcc =
                    // ObsSoundingQuery.getObservedSndBufruaData(lat,
                    // lon, stnId, timeCal, "PPCC",
                    // sndQuery).getSoundingLyLst();
                    ppdd = ObsSoundingQuery.getObservedSndBufruaData(lat, lon,
                            stnId, refLtime, "PPDD", sndQuery)
                            .getSoundingLyLst();
                    wmax_a = ObsSoundingQuery.getObservedSndBufruaData(lat, lon,
                            stnId, refLtime, "MAXWIND_A", sndQuery)
                            .getSoundingLyLst();
                    wmax_c = ObsSoundingQuery.getObservedSndBufruaData(lat, lon,
                            stnId, refLtime, "MAXWIND_C", sndQuery)
                            .getSoundingLyLst();
                    trop_a = ObsSoundingQuery
                            .getObservedSndBufruaData(lat, lon, stnId, refLtime,
                                    "TROPOPAUSE_A", sndQuery)
                            .getSoundingLyLst();
                    trop_c = ObsSoundingQuery
                            .getObservedSndBufruaData(lat, lon, stnId, refLtime,
                                    "TROPOPAUSE_C", sndQuery)
                            .getSoundingLyLst();
                    pf = ObsSoundingQuery.getObservedSndStnInfo(lat, lon, stnId,
                            ObsSndType.BUFRUA.toString(), refLtime, sndQuery);
                    sls = ms.mergeUairSounding(request.getLevel(), ttaa, ttbb,
                            ttcc, ttdd, ppaa, ppbb, ppcc, ppdd, trop_a, trop_c,
                            wmax_a, wmax_c, pf.getStationElevation());

                    // System.out.println("BUFRUA Number of Layers after
                    // merge:"+sls.size()
                    // + " level="+level +
                    // " ms.isNumber(level)="+ms.isNumber(level));
                    // for(NcSoundingLayer ly: sls){
                    // System.out.println("Pre= "+ly.getPressure()+
                    // " Dew= "+ ly.getDewpoint()+ " T= "+
                    // ly.getTemperature()+" H="+ly.getGeoHeight()+"
                    // WSp="+ly.getWindSpeed());
                    // }

                    if (request.getLevel().toUpperCase()
                            .equalsIgnoreCase("MAN"))
                        pf.setSoundingLyLst(sls);
                    else if (ms.isNumber(request.getLevel()) >= 0) {
                        if (sls.size() == 1) {
                            // System.out.println("NCUAIR get one layer using
                            // level = "+
                            // level);
                            pf.setSoundingLyLst(sls);
                        } else {
                            pf = null;
                            // System.out.println("NCUAIR get 0 layer using
                            // level = "+
                            // level);
                        }
                    } else {
                        if (sls.isEmpty() || sls.size() <= 1)
                            pf = null;
                        else
                            pf.setSoundingLyLst(sls);
                    }

                }
                if (pf != null && pf.getSoundingLyLst().size() > 0) {
                    soundingProfileList.add(pf);
                    pf = null;
                }
            }
        }
        if (soundingProfileList.size() == 0)
            cube.setRtnStatus(NcSoundingCube.QueryStatus.FAILED);
        else
            cube.setRtnStatus(NcSoundingCube.QueryStatus.OK);

        cube.setSoundingProfileList(soundingProfileList);
        return cube;
    }
}
