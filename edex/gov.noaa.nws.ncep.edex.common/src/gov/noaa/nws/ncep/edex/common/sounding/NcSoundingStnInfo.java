package gov.noaa.nws.ncep.edex.common.sounding;
/**
 * 
 * gov.noaa.nws.ncep.edex.common.sounding.NcSoundingStnInfo
 * 
 * This java class provides sounding data data structure for used with NC sounding query.
 * Each NcSoundingStnInfo contains lat/lon/elv/stnId/synopTine for a station.
 * This code has been developed by the SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    	Engineer    Description
 * -------		------- 	-------- 	-----------
 * 11/15/2010	TBD			Chin Chen	Initial coding
 * 12/16/2010   362         Chin Chen   add support of BUFRUA observed sounding and PFC (NAM and GFS) model sounding data
 * Aug 05, 2015 4486        rjpeter     Changed Timestamp to Date.
 * 07/02/2015   RM#8107     Chin Chen   change lat/lon data type from double to float to reflect its data type changes starting 14.4.1 
 * 07/08/2015   RM#9172     Chin Chen   Fixed problem caused by Raytheon's change lat/lon data type from double to 
 *                                      float in SurfaceObsLocation class at 14.4.1
 * </pre>
 * 
 * @author Chin Chen
 * @version 1.0
 */

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class NcSoundingStnInfo {
	@DynamicSerializeElement
    private static final long serialVersionUID = 1324632468L;

	@DynamicSerializeElement
    private float stationElevation;
    @DynamicSerializeElement
    //private float	stationLatitude;
    private float	stationLatitude;
    @DynamicSerializeElement
    //private float	stationLongitude;
    private float	stationLongitude;
    @DynamicSerializeElement
    private String stnId;
    @DynamicSerializeElement
    private Date synopTime; // same as retTime 
    @DynamicSerializeElement
    private Date rangeStartTime;
    
	public Date getRangeStartTime() {
		return rangeStartTime;
	}
	public void setRangeStartTime(Date rangeStartTime) {
		this.rangeStartTime = rangeStartTime;
	}
	public NcSoundingStnInfo() {
		super();
		// TODO Auto-generated constructor stub
	}
	public float getStationElevation() {
		return stationElevation;
	}
	public void setStationElevation(float stationElevation) {
		this.stationElevation = stationElevation;
	}
	public float getStationLatitude() {
		return stationLatitude;
	}
	public void setStationLatitude(float stationLatitude) {
		this.stationLatitude = stationLatitude;
	}
	public float getStationLongitude() {
		return stationLongitude;
	}
	public void setStationLongitude(float stationLongitude) {
		this.stationLongitude = stationLongitude;
	}
	public String getStnId() {
		return stnId;
	}
	public void setStnId(String stnId) {
		this.stnId = stnId;
	}
	public Date getSynopTime() {
		return synopTime;
	}
	public void setSynopTime(Date synopTime) {
		this.synopTime = synopTime;
	}
}

