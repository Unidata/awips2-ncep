package gov.noaa.nws.ncep.edex.plugin.aww.util;

import gov.noaa.nws.ncep.edex.plugin.aww.dao.AwwVtecDao;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.raytheon.uf.common.dataquery.db.QueryResult;
import com.raytheon.uf.edex.database.DataAccessLayerException;
import com.raytheon.uf.edex.database.dao.DaoConfig;


/**
 * Utility class for AwwVtec.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * ????         ????       ????        Initial creation.
 * Jul 27, 2015 4500       rjpeter     Removed SQL Injection Concern.
 * Aug 05, 2015 4486       rjpeter     Changed Timestamp to Date.
 * </pre>
 * 
 * @author rjpeter
 * @version 1.0
 */
public class AwwVtecDataUtil {
//	private final static String zeroTime = "000000T0000";
//
//	public static boolean isZeroTime(String timeString) {
//		boolean isZeroTimeString = false; 
//		if(!StringUtil.isStringEmpty(timeString)) {
//			if(timeString.equals(zeroTime))
//				isZeroTimeString = true; 
//		}
//		return isZeroTimeString; 
//	}
	

	public static LatLonInfo retrieveAirportLatLonInfoByAirportId(String airportId) { 
		String queryString = getQueryForAirportLatLonInfo(airportId); 
		LatLonInfo latLonObj = retrieveAirportLatLonInfoFromDB(queryString); 
		return latLonObj; 
	}
	
	private static String getQueryForAirportLatLonInfo(String airportId) {
		StringBuilder query = new StringBuilder("select airport.latitude, airport.longitude "); 
		query.append(" from mapdata.airport airport where ")
			 .append("airport.arpt_id='")
			 .append(airportId)
			 .append("';");  
		return query.toString(); 
	}
	
	private static LatLonInfo retrieveAirportLatLonInfoFromDB(String nativeSQLQuery) {
		LatLonInfo latLonObj = new LatLonInfo();  
		DaoConfig mapsDbDaoConfig = DaoConfig.forDatabase("maps"); 
		AwwVtecDao awwVtecDao = new AwwVtecDao(mapsDbDaoConfig); 
		QueryResult queryResult = awwVtecDao.getQueryResultByNativeSQLQuery(nativeSQLQuery); 
		if(queryResult == null)
			return latLonObj; 
//		System.out.println("##########====================================================, finally, find a not null QueryResult!!!!"); 
		int rowNumber = 0; 
		String lat = (String)queryResult.getRowColumnValue(rowNumber, 0); 
		String lon = (String)queryResult.getRowColumnValue(rowNumber, 1); 
System.out.println("@@@@@@#########, nativeSQLQuery=" + nativeSQLQuery); 
System.out.println("@@@@@@#########, lat=" + lat); 
System.out.println("@@@@@@#########, lon=" + lon); 

		latLonObj.setLatStringInDegreeFormat(lat.trim()); 
		latLonObj.setLonStringInDegreeFormat(lon.trim()); 
		
		return latLonObj; 
	}
	
	public static AwwVtecDataInfo populateAwwVtecEventTimeInfo(AwwVtecDataInfo awwVtectDatainfo, String productClass, 
			String officeId, String phenomena, String significance, String eventTrackingNumber) {
		if(awwVtectDatainfo.getEventStartTime() != null && 
				awwVtectDatainfo.getEventEndTime() != null) {
			return awwVtectDatainfo; 
		}
		String queryString = getQueryForVtectEventTimeInfo(productClass, officeId, phenomena, significance, eventTrackingNumber);
		List<AwwVtecDataInfo> results = retrieveVtectEventStartEndTimeInfoFromDB(queryString); 
//		displayResultOfGetQueryForVtectEventTimeInfo(results); 
		
		if(awwVtectDatainfo.getEventStartTime() == null) {
			Calendar latestValidEventStartTime = getLatestValidEventStartTime(results); 
//			if(latestValidEventStartTime != null)
//				System.out.println("@@@@@@@@@@@@@@@===, find a latest valid event STARTSTART time ="+latestValidEventStartTime+"       ====@@@@@@@@@@@@@@@@@@"); 

			awwVtectDatainfo.setEventStartTime(latestValidEventStartTime); 
		}
		if(awwVtectDatainfo.getEventEndTime() == null) {
			Calendar latestValidEventEndTime = getLatestValidEventEndTime(results); 
//			if(latestValidEventEndTime != null)
//				System.out.println("@@@@@@@@@@@@@@@===, find a latest valid event ENDEND time ="+latestValidEventEndTime+"       ====@@@@@@@@@@@@@@@@@@"); 
			awwVtectDatainfo.setEventEndTime(latestValidEventEndTime); 
		}
		
		awwVtectDatainfo.setEventTrackingNumber(eventTrackingNumber); 
		
		return awwVtectDatainfo; 
	}
	
	public static void 	populateAwwVtecEventStartTimeWithValidValue(Calendar validEventStartTime, String productClass, 
			String officeId, String phenomena, String significance, String eventTrackingNumber) throws DataAccessLayerException {
        AwwVtecDao awwVtecDao = new AwwVtecDao();
        awwVtecDao.populateAwwVtecEventStartTimeWithValidValue(
                validEventStartTime, productClass, officeId, phenomena,
                significance, eventTrackingNumber);
	}
	
	public static void displayResultOfGetQueryForVtectEventTimeInfo(List<AwwVtecDataInfo> results) {
		if(results.size() > 0)
			System.out.println("The total number of retrieved awwRecord.ctectDataInfo="+results.size());
		int arrayIndex = 1; 
		for(AwwVtecDataInfo eachAwwVtecDataInfo : results) {
			Integer recordId = eachAwwVtecDataInfo.getVtecRecordId(); 
			String action = eachAwwVtecDataInfo.getAction(); 
			Calendar eventStartTime = eachAwwVtecDataInfo.getEventStartTime(); 
			Calendar eventEndTime = eachAwwVtecDataInfo.getEventEndTime(); 
			Calendar awwRecordIssueTime = eachAwwVtecDataInfo.getAwwRecordIssueTime(); 
			System.out.println("====== Record No."+arrayIndex+" ========"); 
			System.out.println("\t recordId="+recordId); 
			System.out.println("\t action="+action); 
			System.out.println("\t eventStartTime="+eventStartTime); 
			System.out.println("\t eventEndTime="+eventEndTime); 
			System.out.println("\t awwRecordIssueTime="+awwRecordIssueTime); 
			arrayIndex++; 
		}
	}
	
	private static String getQueryForVtectEventTimeInfo(String productClass, 
			String officeId, String phenomena, String significance, String eventTrackingNumber) {
		StringBuilder query = new StringBuilder("select vtec.recordid, vtec.action, vtec.eventstarttime, vtec.eventendtime, "); 
		query.append("aww.issuetime from awips.aww_vtec vtec, awips.aww_ugc ugc, awips.aww aww where ")
			 .append("vtec.eventtrackingnumber='")
			 .append(eventTrackingNumber)
			 .append("' and vtec.officeid='")
			 .append(officeId)
			 .append("' and vtec.phenomena='")
			 .append(phenomena)
			 .append("' and vtec.productclass ='")
			 .append(productClass)
			 .append("' and vtec.significance='")
			 .append(significance)
			 .append("' and vtec.parentid=ugc.recordid and ugc.parentid=aww.id order by aww.issuetime desc;"); 
		return query.toString(); 
	}
	
	private static List<AwwVtecDataInfo> retrieveVtectEventStartEndTimeInfoFromDB(String nativeSQLQuery) {
		List<AwwVtecDataInfo> awwVtecDataInfoList = new ArrayList<AwwVtecDataInfo>(); 
		AwwVtecDao awwVtecDao = new AwwVtecDao(); 
		QueryResult queryResult = awwVtecDao.getQueryResultByNativeSQLQuery(nativeSQLQuery); 
		if(queryResult == null)
			return awwVtecDataInfoList; 
//		System.out.println("##########====================================================, finally, find a not null QueryResult!!!!"); 
		int resultRowNumber = queryResult.getResultCount(); 
		for(int i=0; i<resultRowNumber; i++) {
			AwwVtecDataInfo eachAwwVtecDataInfo = getAwwVtecDataInfo(queryResult, i); 
			if(eachAwwVtecDataInfo != null)
				awwVtecDataInfoList.add(eachAwwVtecDataInfo); 
		}
		return awwVtecDataInfoList; 
	}
	
	private static AwwVtecDataInfo getAwwVtecDataInfo(QueryResult queryResult, int rowNumber) {
		AwwVtecDataInfo awwVtecDataInfo = new AwwVtecDataInfo(); 
		Integer recordId = (Integer)queryResult.getRowColumnValue(rowNumber, 0); 
		String action = (String)queryResult.getRowColumnValue(rowNumber, 1); 
		Date eventStartTime = (Date)queryResult.getRowColumnValue(rowNumber, 2); 
		Date eventEndTime = (Date)queryResult.getRowColumnValue(rowNumber, 3); 
		Date awwRecordIssueTime = (Date)queryResult.getRowColumnValue(rowNumber, 4); 

		awwVtecDataInfo.setVtecRecordId(recordId); 
		awwVtecDataInfo.setAction(action); 
		awwVtecDataInfo.setEventStartTime(convertDateToCalendar(eventStartTime)); 
		awwVtecDataInfo.setEventEndTime(convertDateToCalendar(eventEndTime)); 
		awwVtecDataInfo.setAwwRecordIssueTime(convertDateToCalendar(awwRecordIssueTime)); 
		
		return awwVtecDataInfo; 
	}
	
	private static Calendar convertDateToCalendar(Date timestamp) {
		Calendar calendar = null; 
		if(timestamp != null) {
			calendar = Calendar.getInstance(); 
			calendar.setTimeInMillis(timestamp.getTime()); 
		}
		return calendar; 
	}
	
	private static Calendar getLatestValidEventStartTime(List<AwwVtecDataInfo> results) {
		Calendar latestValidEventStartTime = null; 
		if(results != null) {
			for(AwwVtecDataInfo eachAwwVtecDataInfo : results) {
				Calendar eventStartTime = eachAwwVtecDataInfo.getEventStartTime(); 
				if(eventStartTime != null) {
					latestValidEventStartTime = eventStartTime; 
					break; 
				}
			}
		}
		return latestValidEventStartTime; 
	}

	private static Calendar getLatestValidEventEndTime(List<AwwVtecDataInfo> results) {
		Calendar latestValidEventEndTime = null; 
		if(results != null) {
			for(AwwVtecDataInfo eachAwwVtecDataInfo : results) {
				Calendar eventEndTime = eachAwwVtecDataInfo.getEventEndTime(); 
				if(eventEndTime != null) {
					latestValidEventEndTime = eventEndTime; 
					break; 
				}
			}
		}
		return latestValidEventEndTime; 
	}
	
}
