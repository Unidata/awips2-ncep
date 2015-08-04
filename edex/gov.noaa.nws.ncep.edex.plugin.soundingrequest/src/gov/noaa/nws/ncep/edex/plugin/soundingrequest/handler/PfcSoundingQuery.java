package  gov.noaa.nws.ncep.edex.plugin.soundingrequest.handler;

/**
 * 
 * gov.noaa.nws.ncep.edex.uengine.tasks.profile.PfcSoundingQuery
 * 
 * This java class performs the pfc model sounding data query functions.
 * This code has been developed by the SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    	Engineer    Description
 * -------		------- 	-------- 	-----------
 * 09/13/2010	301			Chin Chen	Initial coding
 * 12/16/2010   301         Chin Chen   add support of PFC (NAM and GFS) model sounding data
 * 02/28/2012               Chin Chen   modify several sounding query algorithms for better performance
 * 12/20/2013   2537        bsteffen    Update ModelSoundingPointDataTransform
 ***********************************************************************************************************
 *
 *
 * 05/20/2015	RM#8306	   Chin Chen  eliminate NSHARP dependence on uEngine.
 *                                    Copy whole file PfcSoundingQuery.java from uEngine project to this serverRequestService project.
 *                                    "refactor" and clean up unused code for this ticket.
 * 07/02/2015   RM#8107    Chin Chen   change lat/lon data type from double to float to reflect its data type changes starting 14.4.1 
 * 07/21/2015   RM#9173     Chin Chen   Clean up NcSoundingQuery and Obsolete NcSoundingQuery2 and MergeSounging2
 *  *
 * </pre>
 * 
 * @author Chin Chen
 * @version 1.0
 */

import gov.noaa.nws.ncep.common.dataplugin.soundingrequest.SoundingServiceRequest;
import gov.noaa.nws.ncep.common.dataplugin.soundingrequest.SoundingServiceRequest.SoundingType;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingCube;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingProfile;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingStnInfo;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingStnInfoCollection;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingTimeLines;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.measure.converter.UnitConverter;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import com.raytheon.uf.common.dataplugin.modelsounding.ModelSoundingParameters;
import com.raytheon.uf.common.dataplugin.modelsounding.ModelSoundingPointDataTransform;
import com.raytheon.uf.common.dataplugin.modelsounding.SoundingLevel;
import com.raytheon.uf.common.dataplugin.modelsounding.SoundingSite;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint.ConstraintType;
import com.raytheon.uf.edex.database.dao.CoreDao;
import com.raytheon.uf.edex.database.dao.DaoConfig;
import com.vividsolutions.jts.geom.Coordinate;

public class PfcSoundingQuery {
	private static final String PFC_TBL_NAME = "modelsounding";
	private static String currentDBTblName = "nil";
	private static String reportType;
	private static UnitConverter kelvinToCelsius = SI.KELVIN.getConverterTo(SI.CELSIUS);
	private static UnitConverter metersPerSecondToKnots = SI.METERS_PER_SECOND.getConverterTo(NonSI.KNOT);
	public static NcSoundingStnInfoCollection getPfcSndStnInfoCol(SoundingType pfcType, String selectedSndTime, String refTimeStr) {
		NcSoundingStnInfoCollection stnInfoCol = new NcSoundingStnInfoCollection();
		List<NcSoundingStnInfo> stationInfoList= new ArrayList<NcSoundingStnInfo>();
		String queryStr="";
		Object [] rtnobjArray;
		if(pfcType == SoundingType.PFC_NAM_SND){
			currentDBTblName = PFC_TBL_NAME;
			reportType = "ETA"; 
		} else if(pfcType== SoundingType.PFC_GFS_SND){
			currentDBTblName = PFC_TBL_NAME;
			reportType = "GFS";
		} else {
			return stnInfoCol;
		}
		queryStr = new String("Select Distinct latitude, longitude, stationid, elevation, reftime, rangestart FROM "+ currentDBTblName + " where rangestart='" +
				selectedSndTime+ "' AND reftime ='"+ refTimeStr+ "' AND reporttype ='" + reportType + "' AND latitude BETWEEN -89.9 AND 89.9 AND longitude BETWEEN -179.9 AND 179.9");

		//System.out.println(queryStr);
		CoreDao dao = new CoreDao(DaoConfig.forClass(SoundingSite.class));
		rtnobjArray = dao.executeSQLQuery(queryStr);
		//System.out.println("size of rtnobjArray " + rtnobjArray.length);
		if(rtnobjArray.length > 0){
			float lat, lon, elv;
			//System.out.println("queryAndMarkStn called mapresource = "+ nsharpMapResource.toString());
			//Note: A same station may have many reports
			for (int i =0; i <rtnobjArray.length; i++){
				
				Object[] objArray = (Object[] )rtnobjArray[i];
				lat = (Float)objArray[0];
				lon = (Float)objArray[1];
				System.out.println("lat = "+ lat +" lon= "+lon);
				elv = (Integer)objArray[3];
				
				NcSoundingStnInfo stn = stnInfoCol.getNewStnInfo();
				stn.setStnId((String)objArray[2]);
				stn.setStationLongitude(lon);
				stn.setStationLatitude(lat);
				stn.setStationElevation(elv);
				stn.setSynopTime((Timestamp)objArray[4]);
				stn.setRangeStartTime((Timestamp)objArray[5]);
				stationInfoList.add(stn);
				//System.out.println("stn id "+ stn.getStnId() + " lon "+ lon + " lat "+ lat);
			}
			NcSoundingStnInfo [] stationInfoAry = new NcSoundingStnInfo [stationInfoList.size()] ;
			stnInfoCol.setStationInfo(stationInfoList.toArray(stationInfoAry));
		}
		//*System.out.println("stn size = "+ stnInfoCol.getStationInfo().length);
		return stnInfoCol;
	}
	
	public static NcSoundingTimeLines getPfcSndTimeLine(SoundingType pfcType) {
		Object[] refTimeAry = null;
		NcSoundingTimeLines tl = new NcSoundingTimeLines();
		
		if(pfcType == SoundingType.PFC_NAM_SND){
			currentDBTblName = PFC_TBL_NAME;
			reportType = "ETA"; 
		} else if(pfcType== SoundingType.PFC_GFS_SND){
			currentDBTblName = PFC_TBL_NAME;
			reportType = "GFS";
		} else {
			System.out.println("pfc type is not supported: "+ pfcType);
			return tl;
		}
		//query table in metadata db
		String queryStr = new String("Select Distinct reftime FROM "+ currentDBTblName + " where reporttype='" +reportType+"' ORDER BY reftime DESC");


		CoreDao dao = new CoreDao(DaoConfig.forClass(SoundingSite.class));
		refTimeAry = (Object[]) dao.executeSQLQuery(queryStr);
		tl.setTimeLines(refTimeAry);

		return tl;
	}
	// get pfc range time(s) per one reference time
	public static NcSoundingTimeLines getPfcSndRangeTimeLine(SoundingType pfcType,  String refTimeStr) {
		Object[] rangeTimeAry = null;
		NcSoundingTimeLines tl = new NcSoundingTimeLines();
		
		if(pfcType == SoundingType.PFC_NAM_SND){
			currentDBTblName = PFC_TBL_NAME;
			reportType = "ETA"; 
		} else if(pfcType== SoundingType.PFC_GFS_SND){
			currentDBTblName = PFC_TBL_NAME;
			reportType = "GFS";
		} else {
			System.out.println("pfc type is not supported: "+ pfcType);
			return tl;
		}
		//query  table in metadata db
		String queryStr = new String("Select Distinct rangestart FROM "+ currentDBTblName +
				" where reporttype='" +reportType+ "' AND "+ "reftime='"+refTimeStr+":00:00'"+" ORDER BY rangestart");// DESC");
		System.out.println("queryStr  "+ queryStr);


		CoreDao dao = new CoreDao(DaoConfig.forClass(SoundingSite.class));
		rangeTimeAry = (Object[]) dao.executeSQLQuery(queryStr);
		tl.setTimeLines(rangeTimeAry);

		return tl;
	}

	/*
	 * getPfcSndDataGeneric()
	 * Chin:updated at 6/6/2015
	 * using ModelSoundingPointDataTransform for query.
	 * Using Lat/lon array OR StnId array, AND soundingTimeAry (fcst time array) as input.
	 * This function is to be generic for all cases.
	 * One and only one of latLonArray and stnIdArr should be not null and the other one should be null
	 * soundingRangeTimeAry should be not null 
	 * This function query one refTimeStr once
	 * 
	 */
	private static List<NcSoundingProfile>  getPfcSndDataGeneric(Coordinate[]  coordinateArray,String[] stnIdArr, 
			String refTimeStr,String[] soundingRangeTimeAry, SoundingType sndTypeStr, String level) {
		List<NcSoundingProfile> pfs = new ArrayList<NcSoundingProfile>();
		
		if(refTimeStr==null || soundingRangeTimeAry== null){
			System.out.println("refTimeStr or soundingRangeTimeAry is null");
			return null;
		}
		if(sndTypeStr == SoundingType.PFC_GFS_SND  || sndTypeStr==SoundingType.PFC_NAM_SND){
		    Map<String,RequestConstraint> constraints = new HashMap<String, RequestConstraint>();
            List<SoundingSite> lSndSiteRecords = null;
			MergeSounding ms = new MergeSounding();			
			
			
			if(coordinateArray != null){
				String latStr="", lonStr="";
				for ( int i=0; i < coordinateArray.length ; i++)
				{
						latStr = latStr+String.valueOf(coordinateArray[i].y)+",";
						lonStr = lonStr+String.valueOf(coordinateArray[i].x)+",";
				}
				latStr=latStr.substring(0, latStr.length()-1);//get rid of last ","
				lonStr=lonStr.substring(0, lonStr.length()-1);//get rid of last ","
				constraints.put("location.latitude", new RequestConstraint(latStr, ConstraintType.IN));
                constraints.put("location.longitude", new RequestConstraint(lonStr, ConstraintType.IN));
			}
			else if(stnIdArr != null){
				String stnIdStr="";
				for (String stnStr: stnIdArr){
					stnIdStr = stnIdStr+stnStr;
					stnIdStr= stnIdStr+",";
				}
				stnIdStr=stnIdStr.substring(0, stnIdStr.length()-1);//get rid of last ","
				// the rangeStart field name defined in SoundingSite and decoded modelsounding table
				//rangestart data type defined in SoundingSite is "Date"
                constraints.put("location.stationId", new RequestConstraint(stnIdStr, ConstraintType.IN));
			}
			else {
				return pfs;
			}

			// the refTime time field name defined in SoundingSite and decoded modelsounding table
			//refTime data type defined in SoundingSite is "Date"
            constraints.put("dataTime.refTime", new RequestConstraint(refTimeStr));

			String d="";
			for (String timeStr: soundingRangeTimeAry){
				d = d+timeStr;
				d= d+",";
			}
			d=d.substring(0, d.length()-1);//get rid of last ","
	         // the rangeStart field name defined in SoundingSite and decoded modelsounding table. It is forcast time.
			//rangestart data type defined in SoundingSite is "Date"
            constraints.put("dataTime.validPeriod.start", new RequestConstraint(d, ConstraintType.IN));
            //for (Entry<String,RequestConstraint> entry : constraints.entrySet()) {
            //  System.out.println("getPfcSndDataGeneric: field "+ entry.getKey() + " value "+ entry.getValue().getConstraintValue() + " operand= "+ entry.getValue().getConstraintType());
            //}
			List<String> parameters = new ArrayList<String>(12);
            parameters.addAll(ModelSoundingParameters.LVL_PARAMETERS);
            parameters.add(ModelSoundingParameters.LATITUDE);
            parameters.add(ModelSoundingParameters.LONGITUDE);
            parameters.add(ModelSoundingParameters.ELEVATION);
            parameters.add(ModelSoundingParameters.STATION_ID);
            parameters.add(ModelSoundingParameters.STATION_NUMBER);
            parameters.add(ModelSoundingParameters.REF_TIME);
            parameters.add(ModelSoundingParameters.FORECAST_HOUR);
			
			try {
				//long t01 = System.currentTimeMillis();
				lSndSiteRecords = ModelSoundingPointDataTransform.getSoundingSites(constraints, parameters);
				//long t02 = System.currentTimeMillis();
				//System.out.println("getPfcSndDataGeneric sounding site record size = "+ lSndSiteRecords.size()+
				//		" took "+(t02-t01)+ " ms");
                for (SoundingSite sndSite : lSndSiteRecords) {
					//set pf data
					NcSoundingProfile pf = new NcSoundingProfile();
					pf.setStationLatitude(sndSite.getLatitude());
					pf.setStationLongitude(sndSite.getLongitude());
					pf.setStationElevation((float)sndSite.getElevation());
					pf.setFcsTime((sndSite.getDataTime().getFcstTime()*1000)+ sndSite.getDataTime().getRefTime().getTime());
					if(sndSite.getSiteId()!=null && sndSite.getSiteId().length()>0)
						pf.setStationNum(Integer.parseInt(sndSite.getSiteId()));
					pf.setStationId(sndSite.getStationId());


					List<NcSoundingLayer> soundLyList = new ArrayList<NcSoundingLayer>();

					//for (int i=0; i<sizes[0]; i++)
					for (SoundingLevel sndLevel : sndSite.getLevels())
					{
						NcSoundingLayer soundingLy = new NcSoundingLayer();	
						soundingLy.setOmega(sndLevel.getOmega());
						soundingLy.setTemperature((float)kelvinToCelsius.convert(sndLevel.getTemperature()));
						soundingLy.setPressure(sndLevel.getPressure()/100);
						soundingLy.setWindU((float)metersPerSecondToKnots.convert(sndLevel.getUcWind())); // HDF5 data in unit of m/s, convert to Knots 4/12/2012 
						soundingLy.setWindV((float)metersPerSecondToKnots.convert(sndLevel.getVcWind()));
						soundingLy.setSpecHumidity(sndLevel.getSpecificHumidity());						
						soundLyList.add(soundingLy);
					}
					Collections.sort(soundLyList,reversePressureComparator());
					pf.setSoundingLyLst(soundLyList);

					ms.nativeModelSounding(pf.getSoundingLyLst(), pf.getStationElevation());
					if ( ms.isNumber (level) == 0 ) {
						//level is an integer >=0. It means user request a single level
						float rlev = new Integer(Integer.parseInt(level.trim())).floatValue();
						pf.setSoundingLyLst(ms.getSingLevel(rlev, pf.getSoundingLyLst()));
					} else if ( ms.isNumber (level) == 1 ) {      	
						//level is an float >=0. It also means user request a single level
						float rlev = new Float(Float.parseFloat(level.trim()));
						pf.setSoundingLyLst(ms.getSingLevel(rlev, pf.getSoundingLyLst()));
					} 

					pfs.add(pf);
					//System.out.println("sounding fcs time = "+ pf.getFcsTime());
					//System.out.println("sounding ref time = "+ sndSite.getDataTime().getRefTime().getTime());

				}
				
			} catch (Exception e) {
				//System.out.println("exception=" + e );
				e.printStackTrace();
				return pfs;
			}
				
		}
		
		//debug
		
		//long t02 = System.currentTimeMillis();
		//System.out.println("getPfcSndData2 PFC profile retreival took " + (t02 - t01));
		return pfs;
    }
	private static Comparator<NcSoundingLayer> reversePressureComparator() {

        return new Comparator<NcSoundingLayer>() {

            @Override
            public int compare(NcSoundingLayer layerA, NcSoundingLayer layerB) {
                int retValue = 0;
                if (layerA != layerB) {
                    // reverse sort relative to pressure!
                    retValue = Double.compare(layerB.getPressure(), layerA
                            .getPressure());
                }
                return retValue;
            }
        };
    }

	public static NcSoundingCube handlePfcDataRequest(SoundingServiceRequest request){
		NcSoundingCube cube = new NcSoundingCube();
		cube.setRtnStatus(NcSoundingCube.QueryStatus.FAILED); // assume query failure
		List<NcSoundingProfile> finalSoundingProfileList = new ArrayList<NcSoundingProfile>(
				0);
		// NOTE:::since getPfcSndDataGeneric only handle one reftime once, we will have to call it x times, depends on 
		// the size of refTimeStrAry.
		String[] refTimeStrAry = request.getRefTimeStrAry();
		if(refTimeStrAry == null){
			refTimeStrAry =QueryMiscTools.convertTimeLongArrayToStrArray(request.getRefTimeAry());
		}
		String[] rangeTimeStrAry = request.getRangeStartTimeStrAry();
		if(rangeTimeStrAry == null){
			rangeTimeStrAry =QueryMiscTools.convertTimeLongArrayToStrArray(request.getRangeStartTimeAry());
		}
		for(String refTimeStr: refTimeStrAry){
			List<NcSoundingProfile> listReturned = PfcSoundingQuery
					.getPfcSndDataGeneric(request.getLatLonAry(), request.getStnIdAry(),
							refTimeStr  , rangeTimeStrAry, request.getSndType(), request.getLevel());
			if(listReturned!=null && listReturned.size()>0){
				List<NcSoundingProfile> soundingProfileList = new ArrayList<NcSoundingProfile>(
						0);
				soundingProfileList.addAll(listReturned);
//				if (request.isUseNcSoundingLayer2() == true) {
//					QueryMiscTools.convertNcSoundingLayerToNcSoundingLayer2(soundingProfileList);
//				}
				cube.setRtnStatus(NcSoundingCube.QueryStatus.OK); //as long as one query successful, set it to OK
				finalSoundingProfileList.addAll(soundingProfileList);
			}
		}
		cube.setSoundingProfileList(finalSoundingProfileList);
		return(cube);
    }

}
