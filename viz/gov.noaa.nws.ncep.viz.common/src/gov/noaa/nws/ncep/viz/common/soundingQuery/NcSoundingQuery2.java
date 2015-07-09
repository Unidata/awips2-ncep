//<<<<<<< .working
package gov.noaa.nws.ncep.viz.common.soundingQuery;

import gov.noaa.nws.ncep.common.dataplugin.soundingrequest.SoundingServiceRequest;
import gov.noaa.nws.ncep.common.dataplugin.soundingrequest.SoundingServiceRequest.SoundingRequestType;
import gov.noaa.nws.ncep.common.dataplugin.soundingrequest.SoundingServiceRequest.SoundingType;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingCube;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer.DataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.time.TimeRange;
import com.raytheon.uf.viz.core.requests.ThriftClient;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * A general-purpose, user-friendly interface to the NcSoundingDataRequest
 * uengine script. A different data structure is returned vs NcSoundingQuery.
 * 
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer     Description
 * ------------ ---------- -----------  --------------------------
 * 09/20/2011     #459     Greg Hull    Initial creation 
 * 02/21/2012              Chin Chen    Modified several areas for performance improvement
 * 06/25/2014              Chin Chen    support dropsonde
 * 07/23/2014              Chin Chen    Support PW
 * 08/27/2014              Chin Chen    fixed bug happened when query with empty stanid array
 * 06/11/2015     RM#8306  Chin Chen	eliminate dependence on uEngine as message passing broker, instead, use ThriftClient 
 *                                      for sending query request message to EDEX
 * </pre>
 * 
 * @author ghull
 * @version 1.0
 */

public class NcSoundingQuery2 {
    // TODO : change this to an enum or preferably a pluginName
    private String pluginName = null;

    private Boolean merge = false;

    private String level = "-1";

    private Boolean pwRequired = false; // Support PW

    // TODO : add support requested parameters
    // Cyclical dependencies when AbstractMetParameter was in viz.common.
    // private List<AbstractMetParameter> requestedMetParameters = null;

    private Date refTime = null;

    private int forecastHour = 0;

    private TimeRange timeRange = null;

    // if true then the timeRange is used for the valid time of the data.
    private Boolean applyTimeRangeToValidTime = false; // not implemented

    private List<Coordinate> latLonCoords = null;

    private List<String> stationIds = null;

    private List<String> stationNums = null;

    private List<Long> rangeTimeList = null;

    // only applies to UAIR data (true?)
    // TODO : change this to be a list of levelTypes.
    private NcSoundingLayer.DataType uairLevelType = DataType.ALLDATA;

    // only for "ncgrib", "grib" and "modelsounding" plugins
    // (for modelsounding this is the reportType in the database)
    private String modelName = null;

    private List<String> supportedPlugins = Arrays.asList(new String[] {
            "ncuair", "uair",
            // "drop", // what is the drop sounding plugin name?
            // "tamdar", // plugin name?
            "bufrua", "modelsounding", // for NAMSND and GFSSND
            "ncgrib", "grib" });

    private List<String> soundingTypesForPlugins = Arrays.asList(new String[] {
            "NCUAIR", "UAIR",
            // "DROP", // are DROP and TAMDAR really supported now?
            // "TAMDAR",
            "BUFRUA", "modelsounding", // reportTypes of "NAMSND", "GFSSND",
                                       // "RUC2SND", "RUCPTYPSND",
            "ncgrib", "grib" // no soundingType, uengine uses the pluginName
                             // itself
    });

    // A few convienence constructors for common constraints.
    public NcSoundingQuery2(String plugin) throws Exception {
        this(plugin, false, null);
    }

    public NcSoundingQuery2(String plugin, Boolean m) throws Exception {
        this(plugin, m, null);
    }

    public NcSoundingQuery2(String plgn, Boolean m, String lvl)
            throws Exception {
        if (!supportedPlugins.contains(plgn)) {
            System.out.println("NcSoundingQuery2 doesn't support plugin: "
                    + plgn);
            throw new Exception("NcSoundingQuery2 doesn't support plugin: "
                    + plgn);
        }
        pluginName = plgn;
        merge = m;
        level = lvl;
    }

    // public void setRequestedParameters( List<AbstractMetParameter> reqParams
    // ) {
    // requestedMetParameters = reqParams; // copy list?
    // }

    public void setLatLonConstraints(List<Coordinate> coords) {
        if (stationNums != null) {
            System.out
                    .println("LatLon constraint is replacing stationNums constraint");
            stationNums = null;
        } else if (stationIds != null) {
            System.out
                    .println("Station Numbers constraint is replacing stationIds constraint");
            stationIds = null;
        }
        latLonCoords = new ArrayList<Coordinate>(coords);
    }

    public List<Long> getRangeTimeList() {
        return rangeTimeList;
    }

    public void setRangeTimeList(List<Long> rangeTimeList) {
        this.rangeTimeList = rangeTimeList;
    }

    // TODO : could allow for both stationId and stationNum constraint if
    // needed.
    public void setStationIdConstraints(List<String> stnIds) {
        if (stationNums != null) {
            System.out
                    .println("Station Ids constraint is replacing stationNums constraint");
            stationNums = null;
        } else if (latLonCoords != null) {
            System.out
                    .println("Station Numbers constraint is replacing LatLon constraint");
            latLonCoords = null;
        }
        stationIds = new ArrayList<String>(stnIds);
    }

    public void setStationNumConstraints(List<String> stnNums) {
        if (stationIds != null) {
            System.out
                    .println("Station Numbers constraint is replacing stationIds constraint");
            stationIds = null;
        } else if (latLonCoords != null) {
            System.out
                    .println("Station Numbers constraint is replacing LatLon constraint");
            latLonCoords = null;
        }
        stationNums = new ArrayList<String>(stnNums);
    }

    // public setSoundingTypeConstraint( ); // have to set in constructor

    //
    public void setRefTimeConstraint(Date rTime) {
        if (timeRange != null) {
            System.out
                    .println("Ref Time constraint is overriding timeRange constraint.");
            timeRange = null;
        }
        refTime = rTime;
    }

    //
    public void setForecastHourConstraint(int fcstHr) {
        // The refTime must be set in order for fcstHr to be used. Both are used
        // to
        // create a valid time passed to the script.
        // (this need not be a real restriction but the current implementation
        // won't allow it.)
        //
        if (refTime != null) {
            System.out
                    .println("Ref Time should be set before the forecast hour.");
        }

        forecastHour = fcstHr;
    }

    // Add additional constraints for the validTime and/or the forecast hour
    // (used with the ref time),
    // also
    // It wasn't obvious from the original NcSoundingQuery

    // TODO : add flag whether the timeRange applies to the valid time or the
    // refTime.
    //
    public void setTimeRangeConstraint(TimeRange tRange, Boolean valTime) {
        setTimeRangeConstraint(tRange);
        applyTimeRangeToValidTime = valTime;
    }

    public void setTimeRangeConstraint(TimeRange tRange) {
        // if( refTime != null ) {
        // System.out.println("TimeRange constraint is overriding refTime constraint.");
        // refTime = null;
        // }
        timeRange = tRange;
    }

    // this is used by
    public void setModelName(String mdlName) {
        modelName = mdlName;

        if (!pluginName.equals("ncgrib") && !pluginName.equals("grib")
                && !pluginName.equals("modelsounding")) {

            System.out.println("modelName is not applicable for plugin:"
                    + pluginName);
        }
    }

    //
    public void setLevelType(NcSoundingLayer.DataType uaLvl) {
        uairLevelType = uaLvl;
    }

    public void setMerge(Boolean m) {
        merge = m;
    }

    public void setLevelConstraint(String lvl) {
        // TODO : check if this is a non-standard level and print warning if
        // merge is not true
        level = lvl;
    }

    public void setPwRequired(Boolean pwRequired) {
        this.pwRequired = pwRequired;
    }

    //RM#8306
    public NcSoundingCube query(){
        if (latLonCoords == null && stationIds == null && stationNums == null) {
            System.out
                    .println("The query must have either a lat/lon, or stations constraints.");
            return null;
        }
        //
        else if (refTime == null ) {
            System.out
                    .println("The query must have a refTime constraint.");
            return null;
        }
    	NcSoundingCube cube = null;
    	SoundingServiceRequest request = new SoundingServiceRequest();
		request.setReqType(SoundingRequestType.GET_SOUNDING_DATA_GENERIC);
		
        if (pluginName.equals("ncuair")) {
        	request.setSndType(SoundingType.OBS_UAIR_SND);
        } else if (pluginName.equals("bufrua")) {
        	request.setSndType(SoundingType.OBS_BUFRUA_SND);
        } else if (pluginName.equals("modelsounding")) {
            // the uengine script uses a soundingType which is the based
            // on the pluginName and the modelName/reportType
            if (modelName == null) {
                System.out
                        .println("ModelName is not set for modelsounding plugin?");
                return null;
            } else if (modelName.startsWith("NAM")
                    || modelName.startsWith("ETA")) {
            	request.setSndType(SoundingType.PFC_NAM_SND);
            } else if (modelName.startsWith("GFS")) {
            	request.setSndType(SoundingType.PFC_GFS_SND);
            } else {
                System.out
                        .println("Unrecognized ModelName for modelsounding plugin: "
                                + modelName);
                return null;
            }
        }
        else if (pluginName.equals("ncgrib") || pluginName.equals("grib")) {
        	request.setSndType(SoundingType.GRID_MODEL_SND);
            // sanity check that modelName is set?
            if (modelName != null) {
            	request.setModelType(modelName);
            } else {
                System.out
                        .println("ModelName is not set for grib or ncgrib plugin???");
                return null;
            }
        } else {
            System.out.println("NcSoundingQuery2 doesn't support plugin: "
                    + pluginName);
            return null;
        }
        // do not support DATA_TYPE now, as edex sounding query service does not support it now and
        // no any applications use it.
        
        if (refTime != null) {
        	long[] refTimeAry = {refTime.getTime()};
        	request.setRefTimeAry(refTimeAry);
            // use the forecast hour to create the valid time.
            DataTime validTime = new DataTime(refTime, forecastHour);
            long[] rangeTimeAry = {validTime.getValidTime().getTimeInMillis()};
            request.setRangeStartTimeAry(rangeTimeAry);
        } else{
        	return null;
        }

        if (rangeTimeList != null) {
        	long[] rangeTimeAry = new long[rangeTimeList.size()];
            for (int i = 0; i < rangeTimeList.size(); i++) {
            	rangeTimeAry[i]= rangeTimeList.get(i) ;
            }
            request.setRangeStartTimeAry(rangeTimeAry);
        }
        request.setMerge(merge);
        // Support PW
        request.setPwRequired(pwRequired);
        if(level == null)
        	level = "N/A";
        request.setLevel(level);
        request.setUseNcSoundingLayer2(true);

        // set either lat/lon, stationId or stationNum
        if (latLonCoords != null) {
            Coordinate[] latLonAry= new Coordinate[latLonCoords.size()];
            for (int i = 0; i < latLonCoords.size(); i++) {
                Coordinate latlon = latLonCoords.get(i);
                latLonAry[i]= latlon;
            }
            request.setLatLonAry(latLonAry);
        } else if (stationIds != null) {
        	String [] stnIdAry = new String[stationIds.size()];
            for (int i = 0; i < stationIds.size(); i++) {
            	stnIdAry[i]= stationIds.get(i) ;
            }
            request.setStnIdAry(stnIdAry);
        }
        try {
            Object rslts = ThriftClient.sendRequest(request);
            if ((rslts instanceof NcSoundingCube)) {
                //
            	cube = (NcSoundingCube) rslts;
            } else {
                System.out.println("genericSoundingDataQuery Request Failed: ");

            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    	return cube;
    }

    //end RM#8306

}
