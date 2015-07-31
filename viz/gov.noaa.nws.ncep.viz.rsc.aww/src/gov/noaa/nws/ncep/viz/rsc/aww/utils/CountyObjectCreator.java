package gov.noaa.nws.ncep.viz.rsc.aww.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import com.raytheon.uf.common.time.DataTime;

/**
 * CountyObjectCreator - common method used throughout AWW viz resources to return objects
 * with single counties as opposed to a list of counties for proper display.
 * 
 * 
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 17-Nov-2014   RM5125     jhuber    Initial creation.                                                              
 * </pre>
 * 
 * @author jhuber
 * @version 1.0
 */
 
public class CountyObjectCreator{
        
    public static List<PreProcessDisplay> PreProcessDisplay(List<PreProcessDisplay> CountyZoneList){
        HashMap<String,PreProcessDisplay> displayMap; 
        HashMap<String, DataTime> extMap;
        displayMap = new HashMap<String,PreProcessDisplay>(); 
        extMap = new HashMap<String, DataTime>();
        List<PreProcessDisplay> displayObj = new ArrayList<PreProcessDisplay>();
        for (PreProcessDisplay processCountyZone : CountyZoneList) {
            for (int i = 0; i < processCountyZone.fipsCodesList.size(); i++) {
                PreProcessDisplay singleCountyZone = new PreProcessDisplay();
                singleCountyZone.singleFipsCode = processCountyZone.fipsCodesList.get(i); 
                singleCountyZone.singleCountyZoneLat = processCountyZone.countyZoneLatList.get(i);
                singleCountyZone.singleCountyZoneLon =  processCountyZone.countyZoneLonList.get(i); 
                if(!processCountyZone.eventType.equalsIgnoreCase("NEW") ||!processCountyZone.eventType.equalsIgnoreCase("CON")) {
                    singleCountyZone.displayStart = processCountyZone.issueTime; 
                } else { 
                    singleCountyZone.displayStart = new DataTime(processCountyZone.eventTime.getValidPeriod().getStart()); 
                }
                singleCountyZone.displayEnd = processCountyZone.origEndTime; 
                singleCountyZone.eventTime = processCountyZone.eventTime; 
                singleCountyZone.eventType = processCountyZone.eventType;
                singleCountyZone.issueTime = processCountyZone.issueTime;
                singleCountyZone.origStartTime = processCountyZone.origStartTime;
                singleCountyZone.origEndTime = processCountyZone.origEndTime; 
                singleCountyZone.reportType = processCountyZone.reportType; 
                singleCountyZone.evTrack = processCountyZone.evTrack; 
                singleCountyZone.evPhenomena = processCountyZone.evPhenomena; 
                singleCountyZone.evSignificance = processCountyZone.evSignificance; 
                singleCountyZone.evOfficeId = processCountyZone.evOfficeId;          

          if (singleCountyZone.eventType.equalsIgnoreCase("EXT") || singleCountyZone.eventType.equalsIgnoreCase("EXB") || singleCountyZone.eventType.equalsIgnoreCase("EXA")) { 
              if (extMap.containsKey(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType)) { 
                  DataTime retrievePreviousEXTTime = extMap.get(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType);
                  PreProcessDisplay retrievePreviousEXT = displayMap.get(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType + " " + "EXT" + retrievePreviousEXTTime); 
                  if (retrievePreviousEXT == null) {
                      retrievePreviousEXT = displayMap.get(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType + " " + "EXT"); 
                      retrievePreviousEXT.displayEnd = singleCountyZone.issueTime; 
                      displayMap.put(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType + " " + "EXT", retrievePreviousEXT);
                      displayMap.put(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType + " " + "EXT" + singleCountyZone.issueTime, singleCountyZone);
                      extMap.put(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType, singleCountyZone.issueTime); 
                  } else { 
                      retrievePreviousEXT.displayEnd = singleCountyZone.issueTime; 
                      displayMap.put(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType + " " + "EXT" + " " + retrievePreviousEXTTime, retrievePreviousEXT);
                      displayMap.put(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType + " " + "EXT" + " " + singleCountyZone.issueTime, singleCountyZone); 
                      extMap.put(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType, singleCountyZone.issueTime); 
                      } 
                  } else if (displayMap.containsKey(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType)) { 
                      PreProcessDisplay retrieveCurrent = displayMap .get(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType);
                      retrieveCurrent.displayEnd = singleCountyZone.issueTime;
                      displayMap.put(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType, retrieveCurrent); 
                      displayMap.put(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType + " " + "EXT", singleCountyZone);
                      extMap.put(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType, singleCountyZone.issueTime); 
                  } else { 
                      displayMap.put(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType, singleCountyZone); 
                      } 
              } else if (singleCountyZone.eventType.equalsIgnoreCase("CAN") || singleCountyZone.eventType.equalsIgnoreCase("EXP")) { 
                  if (extMap.containsKey(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType)) { 
                      DataTime retrievePreviousEXTTime = extMap.get(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType);
                      PreProcessDisplay retrievePreviousEXT = displayMap.get(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType + " " + "EXT" + retrievePreviousEXTTime); 
                      if (retrievePreviousEXT == null) {
                          retrievePreviousEXT = displayMap .get(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType + " " + "EXT"); 
                          if (singleCountyZone.eventType.equalsIgnoreCase("CAN")) {
                              retrievePreviousEXT.displayEnd = singleCountyZone.issueTime; 
                          } else {
                              retrievePreviousEXT.displayEnd = singleCountyZone.origEndTime; 
                          }
                          displayMap.put(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType + " " + "EXT", retrievePreviousEXT); 
                          extMap.put(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack, singleCountyZone.origEndTime);
                      } else {
                          retrievePreviousEXT.displayEnd = singleCountyZone.origEndTime;
                          displayMap.put(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType + " " + "EXT" + " " + retrievePreviousEXTTime, retrievePreviousEXT);
                          extMap.put(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType, singleCountyZone.origEndTime); 
                          } 
                      } else if (displayMap.containsKey(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType)) { 
                          PreProcessDisplay retrieveCurrent = displayMap.get(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType); 
                          if (singleCountyZone.eventType.equalsIgnoreCase("CAN")) {
                              retrieveCurrent.displayEnd = singleCountyZone.issueTime; 
                          } else {
                              retrieveCurrent.displayEnd = singleCountyZone.origEndTime; 
                          }
                          displayMap.put(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType, retrieveCurrent);
                      }
              } else if(singleCountyZone.eventType.equalsIgnoreCase("UPG")) {
                  PreProcessDisplay retrieveCurrent = displayMap.get(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType);
                  if (extMap.containsKey(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType)) { 
                      DataTime retrievePreviousEXTTime = extMap.get(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType);
                      PreProcessDisplay retrievePreviousEXT = displayMap.get(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType + " " + "EXT" + retrievePreviousEXTTime); 
                      if (retrievePreviousEXT == null) {
                          retrievePreviousEXT = displayMap.get(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType + " " + "EXT"); 
                          retrievePreviousEXT.displayEnd = singleCountyZone.issueTime; 
                          displayMap.put(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType + " " + "EXT", retrievePreviousEXT);
                      } else { 
                          retrievePreviousEXT.displayEnd = singleCountyZone.issueTime; 
                          displayMap.put(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType + " " + "EXT" + " " + retrievePreviousEXTTime, retrievePreviousEXT);
                          } 
                  } else if (displayMap.containsKey(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType)) { 
                          retrieveCurrent.displayEnd = singleCountyZone.issueTime;
                          displayMap.put(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType, retrieveCurrent); 
                  } else { 
                          displayMap.put(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType, singleCountyZone); 
                  }             
              } else { 
                  if (!displayMap.containsKey(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType)) { 
                       displayMap.put(singleCountyZone.singleFipsCode + " " + singleCountyZone.evTrack + " " + singleCountyZone.reportType, singleCountyZone); 
                      } 
                  }
              }
          }
          for (Entry<String, PreProcessDisplay> entry : displayMap.entrySet()) {
              displayObj.add(entry.getValue());
          }
          Collections.sort(displayObj);
          return (displayObj); 
      }

}

