package gov.noaa.nws.ncep.viz.rsc.aww.utils;

import gov.noaa.nws.ncep.common.dataplugin.aww.AwwRecord.AwwReportType;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource.IRscDataObject;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.RGB;

import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.edex.decodertools.core.LatLonPoint;

/**
 * PreProcessDisplay - common class used throughout AWW viz resources
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

public class PreProcessDisplay implements IRscDataObject,Comparable<PreProcessDisplay>{
    public DataTime issueTime;
    public DataTime eventTime;
    public AwwReportType reportType;
    public List<Double> countyZoneLatList = new ArrayList<Double>();
    public List<Double> countyZoneLonList = new ArrayList<Double>();
    public double singleCountyZoneLat;
    public double singleCountyZoneLon;
    public int countyNumPoints;
    public String countyNames;
    public String stateNames;
    public String zoneName;
    public List<String> fipsCodesList;
    public String eventType;
    public String watchNumber;
    public DataTime origEndTime;
    public DataTime displayStart;
    public DataTime displayEnd;
    public String evTrack;
    public String evPhenomena;
    public String evSignificance;
    public String evOfficeId;
    public DataTime start;
    public DataTime origStartTime;
    public LatLonPoint zoneLatLon;
    public RGB color;
    public DataTime endTime;
    public String singleFipsCode;


    @Override 
    public int compareTo(PreProcessDisplay o) {
        
    return (int) (this.issueTime.getRefTime().getTime() - o.issueTime
    .getRefTime().getTime()); 
    }


    @Override
    public DataTime getDataTime() {
        return eventTime;
    }
}