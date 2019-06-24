package gov.noaa.nws.ncep.viz.soundingrequest;

/**
 * 
 * 
 * 
 * This java class performs the NSHARP pfc sounding data query functions.
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#     Engineer    Description
 * -------      -------     --------    -----------
 * 11/1/2010    362         Chin Chen   Initial coding
 * 12/16/2010   362         Chin Chen   add support of BUFRUA observed sounding and PFC (NAM and GFS) model sounding data
 * 02/15/2012               Chin Chen   modify several sounding query algorithms for better performance
 * 10/15/2012   2473        bsteffen    Remove ncgrib
 * 03/04/2015   RM#6674     Chin Chen   support data interpolation configuration for model sounding query 
 * 05272015     RM#8306     Chin Chen   eliminate dependence on uEngine as message passing broker, instead, use ThriftClient 
 *                                      for sending query request message to EDEX
 * 07/14/2015   RM#9173     Chin Chen   Clean up NcSoundingQuery and Obsolete NcSoundingQuery2 and MergeSounding2
 * 09/22/2016   RM15953     R.Reynolds  Added capability for wind interpolation
 * </pre>
 * 
 * @author Chin Chen
 * @version 1.0
 */

import gov.noaa.nws.ncep.common.dataplugin.soundingrequest.SoundingServiceRequest;
import gov.noaa.nws.ncep.common.dataplugin.soundingrequest.SoundingServiceRequest.SoundingRequestType;
import gov.noaa.nws.ncep.common.dataplugin.soundingrequest.SoundingServiceRequest.SoundingType;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingCube;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingProfile.MdlSndType;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingProfile.ObsSndType;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingProfile.PfcSndType;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingStnInfoCollection;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingTimeLines;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingTools;

import java.util.Calendar;
import java.util.TimeZone;

import com.raytheon.uf.viz.core.requests.ThriftClient;
import org.locationtech.jts.geom.Coordinate;

public class NcSoundingQuery {
    public static int counter = 1;

    public static String GRIB_PLUGIN_NAME = "grid";

    public static long convertRefTimeStr(String refTimeStr) {
        int year, mon, date, hr;
        int index = refTimeStr.indexOf('-');
        if (index >= 4) {
            year = Integer.parseInt(refTimeStr.substring(index - 4, index));
            refTimeStr = refTimeStr.substring(index + 1);
            index = refTimeStr.indexOf('-');
            if (index >= 2) {
                mon = Integer.parseInt(refTimeStr.substring(index - 2, index));
                refTimeStr = refTimeStr.substring(index + 1);
                index = refTimeStr.indexOf(' ');
                if (index >= 2) {
                    date = Integer.parseInt(refTimeStr.substring(index - 2,
                            index));
                    refTimeStr = refTimeStr.substring(index + 1);
                    // index = refTimeStr.indexOf(':');
                    if (refTimeStr.length() >= 2) {
                        hr = Integer.parseInt(refTimeStr.substring(0, 2));
                        Calendar refTimeCal = Calendar.getInstance(TimeZone
                                .getTimeZone("GMT"));
                        // reset time
                        refTimeCal.setTimeInMillis(0);
                        // set new time
                        refTimeCal.set(year, mon - 1, date, hr, 0, 0);
                        // System.out.println("set time Str " + refTimeStr +
                        // " cal time in GMT " +
                        // refTimeCal.getTime().toGMTString() + " in msec = " +
                        // refTimeCal.getTimeInMillis());
                        return refTimeCal.getTimeInMillis();
                    }
                }
            }
        }
        return 0;
    }

    public static String convertSoundTimeDispStringToRangeStartTimeFormat(
            String displayStr) {
        // Note: time line display string has format of, e.x. (old
        // 111208/2130V003), 111208/21(Tue)V003, convert to 2011-12-08 21:00:00.
        // first 2 digits is year
        String rangeStartStr = "20" + displayStr.substring(0, 2);
        // 3rd and 4th digits is month
        rangeStartStr = rangeStartStr + "-" + displayStr.substring(2, 4);
        // 5th and 6th digits is day
        rangeStartStr = rangeStartStr + "-" + displayStr.substring(4, 6);
        // 8th and 9th digits is hour, 10th and 11th are min, and seconds should
        // be 0s
        rangeStartStr = rangeStartStr + " " + displayStr.substring(7, 9)
                + ":00:00";
        return rangeStartStr;
    }

    // return refTimeStr
    public static String convertSoundTimeDispStringToForecastTime(
            String displayStr) {
        // Note: time line display string has format of, e.x.
        // 111208/21(Tue)V003, convert to 2011-12-08 21:00:00 + forecast hour.
        String year, mon, day, hour, Vhour;
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        // first 2 digits is year
        year = "20" + displayStr.substring(0, 2);
        // 3rd and 4th digits is month
        mon = displayStr.substring(2, 4);
        // 5th and 6th digits is day
        day = displayStr.substring(4, 6);
        // 8th and 9th digits is hour
        hour = displayStr.substring(7, 9);
        // 10-11 is minutes, second should be 0s
        // min = displayStr.substring(9, 11);
        // Vhour starting digit 13
        Vhour = displayStr.substring(displayStr.indexOf('V') + 1,
                displayStr.indexOf('V') + 4);
        cal.set(Integer.parseInt(year), Integer.parseInt(mon) - 1,
                Integer.parseInt(day), Integer.parseInt(hour), 0);// Integer.parseInt(min));
        // from VXXX and rangeStart time get forecast Time
        long forecasttimeMs = cal.getTimeInMillis()
                - (Integer.parseInt(Vhour) * 3600000);
        cal.setTimeInMillis(forecasttimeMs);
        String ref = String.format("%1$tY-%1$tm-%1$td %1$tH:%1$tM:00", cal);

        return ref;
    }

    // NCP nsharp use this for query PFC for one ref time and multiple sounding
    // range times of a same station using lat/lon
    // 9173 obsoleting this method
    // public static NcSoundingCube pfcSoundingQueryByRangeTimeArray(long
    // refTime, long[] soundingRangeTime,
    // double lat, double lon, String sndType, NcSoundingLayer.DataType
    // dataType, boolean merge, String level) {
    // //RM#8306
    // long[] refLTimeAry = {refTime};
    // Coordinate[] coordArray = new Coordinate[1];
    // Coordinate latlon = new Coordinate(lon,lat);
    // coordArray[0]= latlon;
    // long[] soundingRangeTimeArray = new long[soundingRangeTime.length];
    // for(int i =0; i< soundingRangeTime.length; i++)
    // soundingRangeTimeArray[i] = soundingRangeTime[i];
    //
    // return(genericSoundingDataQuery( refLTimeAry, soundingRangeTimeArray,
    // null, null, coordArray, null, sndType, dataType,
    // merge, level,null,true, false, false));
    // }

    /*
     * genericSoundingDataQuery() used to query observed uair sounding, point
     * forecast (pfc) model sounding, and grid model sounding data. At least one
     * of {long[] refTime (in ms) ; String[] refTimeStr (in
     * "yyyy-MM-DD HH:mm:ss")} is required for all queries At least one of
     * {long[] rangeTime (in ms) ; String[] rangeTimeStr (in
     * "yyyy-MM-DD HH:mm:ss")} is required for pfc/grid model sounding queries
     * At least one of {Coordinate[] latLonAry ; String[] stnIdAry} is required
     * for all queries String sndType is required for all queries. One of
     * [ObsSndType.NCUAIR.toString(),
     * ObsSndType.BUFRUA.toString(),PfcSndType.NAMSND
     * .toString(),PfcSndType.GFSSND.toString(),MdlSndType.ANY.toString()]
     * NcSoundingLayer.DataType dataType is not used for now. Use
     * NcSoundingLayer.DataType.ALLDATA for now. boolean merge: true for almost
     * all queries; false for query "raw" data, currently only Nsharp use it for
     * query "raw" observed uair data. String level: "-1" for query all levels;
     * or one of { "1000", "925", "850", "700", "500", "400", "300", "250",
     * "200", "150", "100" } for query mandatory level String modelType: used
     * for grid model query only, ignored for other queries boolean
     * interpolation: used for grid model query only, ignored for other queries,
     * normally set to true boolean useNcSoundingLayer2: true for using
     * NcSoundingLayer2 at returned profile; false for using NcSoundingLayer
     * boolean pwRequired: true for query precipitation
     * 
     * ncp directly call this method to query ncuair/bufrua/pfcnam/pfcgfs/grid
     * d2d call mdlSoundingQueryByLatLon(), soundingQueryByStnId(), and
     * pfcSoundingQueryByLatLon() for grid/bufua/pfc sounding data, these 3
     * methods then call this method.
     */

    public static NcSoundingCube genericSoundingDataQuery(long[] refTime,
            long[] rangeTime, String[] refTimeStr, String[] rangeTimeStr,
            Coordinate[] latLonAry, String[] stnIdAry, String sndType,
            NcSoundingLayer.DataType dataType, boolean merge, String level,
            String modelType, boolean interpolation,
            boolean useNcSoundingLayer2, boolean pwRequired) {

        return genericSoundingDataQuery(refTime, rangeTime, refTimeStr,
                rangeTimeStr, latLonAry, stnIdAry, sndType, dataType, merge,
                level, modelType, interpolation, useNcSoundingLayer2,
                pwRequired, false);
    }

    public static NcSoundingCube genericSoundingDataQuery(long[] refTime,
            long[] rangeTime, String[] refTimeStr, String[] rangeTimeStr,
            Coordinate[] latLonAry, String[] stnIdAry, String sndType,
            NcSoundingLayer.DataType dataType, boolean merge, String level,
            String modelType, boolean interpolation,
            boolean useNcSoundingLayer2, boolean pwRequired,
            boolean windInterpolation) {
        NcSoundingCube cube = null;
        SoundingServiceRequest request = new SoundingServiceRequest();
        request.setReqType(SoundingRequestType.GET_SOUNDING_DATA_GENERIC);
        if (sndType.equals(ObsSndType.NCUAIR.toString())) {
            request.setSndType(SoundingType.OBS_UAIR_SND);

        } else if (sndType.equals(ObsSndType.BUFRUA.toString())) {
            request.setSndType(SoundingType.OBS_BUFRUA_SND);

        } else if (sndType.equals(PfcSndType.NAMSND.toString())) {
            request.setSndType(SoundingType.PFC_NAM_SND);
        } else if (sndType.equals(PfcSndType.GFSSND.toString())) {
            request.setSndType(SoundingType.PFC_GFS_SND);
        } else if (sndType.equals(MdlSndType.ANY.toString())) {
            request.setSndType(SoundingType.GRID_MODEL_SND);
        } else
            return null;
        request.setRefTimeAry(refTime);
        request.setRangeStartTimeAry(rangeTime);
        request.setRefTimeStrAry(refTimeStr);
        request.setRangeStartTimeStrAry(rangeTimeStr);
        request.setPwRequired(pwRequired);
        if (latLonAry != null) {
            request.setLatLonAry(latLonAry);
        } else if (stnIdAry != null) {
            request.setStnIdAry(stnIdAry);
        } else
            return null;
        if (level == null)
            level = "-1";
        request.setLevel(level);
        request.setMerge(merge);
        request.setInterpolation(interpolation);
        request.setModelType(modelType);
        request.setWindInterpolation(windInterpolation);

        // request.setUseNcSoundingLayer2(false);//(useNcSoundingLayer2);
        // request.setUseNcSoundingLayer2(useNcSoundingLayer2);
        // do not support DATA_TYPE now, as edex sounding query service does not
        // support it now and no any applications use it.
        try {
            Object rslts = ThriftClient.sendRequest(request);
            if ((rslts instanceof NcSoundingCube)) {
                //
                cube = (NcSoundingCube) rslts;
                if (useNcSoundingLayer2) {
                    NcSoundingTools
                            .convertNcSoundingLayerToNcSoundingLayer2(cube
                                    .getSoundingProfileList());
                }

            } else {
                System.out.println("genericSoundingDataQuery Request Failed: ");

            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return cube;
    }

    // end RM#8306

    // ncep Nsharp use this interface to query both bufrua and ncuair data
    // RM#9173: obsoleted this method
    // public static NcSoundingCube uaGenericSoundingQuery(Long[] refTime,
    // double[][] latLon, String sndType, NcSoundingLayer.DataType dataType,
    // boolean merge, String level) {
    // //RM#8306
    // if(refTime!=null && refTime.length>0){
    // long[] reflTimeAry = new long[refTime.length];
    // for(int i=0; i< refTime.length; i++)
    // reflTimeAry[i]= refTime[i];
    // Coordinate[] coordArray = convertDoubleLatLonArray(latLon);
    // return(genericSoundingDataQuery( reflTimeAry, null, null,
    // null,coordArray, null, sndType, dataType, merge, level,null,true,
    // false));
    // }
    // else
    // return null;
    //
    //
    // }

    /*
     * The following 3 methods are used by D2D Nsharp only!!!!!
     */
    // D2D use this interface for grid model data query, one ref time and one
    // range start time once
    public static NcSoundingCube mdlSoundingQueryByLatLon(String refTimeStr,
            String validTimeStr, float[][] latLon, String pluginName,
            String mdlName, boolean merge, String level, boolean interpolation) {
        String[] refLTimeStrAry = { refTimeStr };
        String[] soundingRangeTimeStrArray = { validTimeStr };
        Coordinate[] coordArray = convertFloatLatLonArray(latLon);
        return (genericSoundingDataQuery(null, null, refLTimeStrAry,
                soundingRangeTimeStrArray, coordArray, null,
                MdlSndType.ANY.toString(), NcSoundingLayer.DataType.ALLDATA,
                merge, level, mdlName, interpolation, false, false));
    }

    // D2D use this for bufrua data query. d2d does NOT query ncuair data
    public static NcSoundingCube soundingQueryByStnId(long refTime,
            String[] stnArray, String sndType,
            NcSoundingLayer.DataType dataType, boolean merge, String level) {
        long[] refLTimeAry = new long[1];
        refLTimeAry[0] = refTime;
        return (genericSoundingDataQuery(refLTimeAry, null, null, null, null,
                stnArray, sndType, dataType, merge, level, null, true, false,
                false));
    }

    // D2D use this for pfc query for one ref time and one sounding range time
    // of a same station using lat/lon
    public static NcSoundingCube pfcSoundingQueryByLatLon(long refTime,
            long validTime, double[][] latLon, String sndType,
            NcSoundingLayer.DataType dataType, boolean merge, String level) {
        // RM#8306

        long[] refLTimeAry = { refTime };
        long[] soundingRangeTimeArray = { validTime };
        Coordinate[] coordArray = convertDoubleLatLonArray(latLon);
        return (genericSoundingDataQuery(refLTimeAry, soundingRangeTimeArray,
                null, null, coordArray, null, sndType, dataType, merge, level,
                null, true, false, false));
        // end RM#8306
    }

    // RM#8306
    // USED BY NCUAIR FOR TIME LINE, BY PFC FOR AVAILABLE FILE LIST
    // Actually, we are querying reference time.
    // MODEL sounding USE NCInventory for this query.
    public static NcSoundingTimeLines soundingTimeLineQuery(String sndType) {
        NcSoundingTimeLines timeLines = null;
        SoundingServiceRequest request = new SoundingServiceRequest();
        request.setReqType(SoundingRequestType.GET_SOUNDING_REF_TIMELINE);
        if (sndType.equals(ObsSndType.NCUAIR.toString())) {
            request.setSndType(SoundingType.OBS_UAIR_SND);

        } else if (sndType.equals(ObsSndType.BUFRUA.toString())) {
            request.setSndType(SoundingType.OBS_BUFRUA_SND);

        } else if (sndType.equals(PfcSndType.NAMSND.toString())) {
            request.setSndType(SoundingType.PFC_NAM_SND);
        } else if (sndType.equals(PfcSndType.GFSSND.toString())) {
            request.setSndType(SoundingType.PFC_GFS_SND);
        } else
            return null;
        try {
            Object rslts = ThriftClient.sendRequest(request);
            if ((rslts instanceof NcSoundingTimeLines)) {
                //
                timeLines = (NcSoundingTimeLines) rslts;
            } else {
                System.out.println("soundingTimeLineQuery Request Failed: ");

            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return timeLines;
    }

    // get PFC/Grid range time line(s) per one reference time
    // USED BY pfc and grid model sounding to get time line for an AVAILABLE
    // FILE
    // Actually we querying range start time lines
    public static NcSoundingTimeLines soundingRangeTimeLineQuery(
            String sndType, String refTimeStr, String mdlType) {
        NcSoundingTimeLines timeLines = null;
        SoundingServiceRequest request = new SoundingServiceRequest();
        String[] refTimeAry = { refTimeStr };
        request.setRefTimeStrAry(refTimeAry);
        request.setReqType(SoundingRequestType.GET_SOUNDING_RANGESTART_TIMELINE);
        if (sndType.equals(PfcSndType.NAMSND.toString())) {
            request.setSndType(SoundingType.PFC_NAM_SND);
        } else if (sndType.equals(PfcSndType.GFSSND.toString())) {
            request.setSndType(SoundingType.PFC_GFS_SND);
        } else if (sndType.equals(MdlSndType.ANY.toString())) {
            request.setSndType(SoundingType.GRID_MODEL_SND);
            request.setModelType(mdlType);
        } else
            return null;
        try {
            Object rslts = ThriftClient.sendRequest(request);
            if ((rslts instanceof NcSoundingTimeLines)) {
                //
                timeLines = (NcSoundingTimeLines) rslts;
            } else {
                System.out
                        .println("soundingRangeTimeLineQuery Request Failed: ");

            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return timeLines;
    }

    // RM#8306 new
    // used to query OSB and PFC sounding station info
    // referTimeStr is required for all snd type
    // rangeStartTimeStr is required for PFC snd only, and should set to null
    // for OBS snd
    public static NcSoundingStnInfoCollection genericSoundingStnInfoQuery(
            String sndType, String rangeStartTimeStr, String referTimeStr) {
        NcSoundingStnInfoCollection stnInfos = null;
        SoundingServiceRequest request = new SoundingServiceRequest();
        request.setReqType(SoundingRequestType.GET_SOUNDING_STATION_INFO);
        if (sndType.equals(ObsSndType.NCUAIR.toString())) {
            request.setSndType(SoundingType.OBS_UAIR_SND);

        } else if (sndType.equals(ObsSndType.BUFRUA.toString())) {
            request.setSndType(SoundingType.OBS_BUFRUA_SND);

        } else if (sndType.equals(PfcSndType.NAMSND.toString())) {
            request.setSndType(SoundingType.PFC_NAM_SND);
        } else if (sndType.equals(PfcSndType.GFSSND.toString())) {
            request.setSndType(SoundingType.PFC_GFS_SND);
        }
        String[] refTimeStrAry = new String[1];
        refTimeStrAry[0] = referTimeStr;
        request.setRefTimeStrAry(refTimeStrAry);
        if (rangeStartTimeStr != null) {
            String[] rangeTimeStrAry = new String[1];
            rangeTimeStrAry[0] = rangeStartTimeStr;
            request.setRangeStartTimeStrAry(rangeTimeStrAry);
        }
        try {
            Object rslts = ThriftClient.sendRequest(request);
            if ((rslts instanceof NcSoundingStnInfoCollection)) {
                //
                stnInfos = (NcSoundingStnInfoCollection) rslts;
            } else {
                System.out
                        .println("genericSoundingStnInfoQuery Request Failed: ");

            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return stnInfos;
    }

    // end RM#8306

    public static Coordinate[] convertDoubleLatLonArray(double[][] latLon) {
        if (latLon != null) {
            Coordinate[] latLonAry = new Coordinate[latLon.length];
            for (int i = 0; i < latLon.length; i++) {
                Coordinate latlon = new Coordinate(latLon[i][1], latLon[i][0]);
                latLonAry[i] = latlon;
            }
            return latLonAry;
        } else
            return null;
    }

    public static Coordinate[] convertFloatLatLonArray(float[][] latLon) {
        if (latLon != null) {
            Coordinate[] latLonAry = new Coordinate[latLon.length];
            for (int i = 0; i < latLon.length; i++) {
                Coordinate latlon = new Coordinate(latLon[i][1], latLon[i][0]);
                latLonAry[i] = latlon;
            }
            return latLonAry;
        } else
            return null;
    }

}
