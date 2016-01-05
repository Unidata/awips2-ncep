package gov.noaa.nws.ncep.common.dataplugin.soundingrequest;
/**
 * 
 * This java class performs sounding data query service functions.
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    	Engineer    Description
 * -------		------- 	-------- 	-----------
 * 05/20/2015	RM#8306		Chin Chen	Initial coding - eliminate NSHARP dependence on uEngine
 * 07/20/2015   RM#9173     Chin Chen   Clean up NcSoundingQuery and Obsolete NcSoundingQuery2 and MergeSounding2
 *
 * </pre>
 * 
 * @author Chin Chen
 * @version 1.0
 */
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.serialization.comm.IServerRequest;
import com.vividsolutions.jts.geom.Coordinate;

@DynamicSerialize
public class SoundingServiceRequest implements IServerRequest {

	@DynamicSerializeElement
	private SoundingRequestType reqType;

	@DynamicSerializeElement
	private SoundingType sndType;

	@DynamicSerializeElement
	private long[]	refTimeAry=null;

	@DynamicSerializeElement
	private String[] refTimeStrAry=null;

	@DynamicSerializeElement
	private long[]	rangeStartTimeAry=null;

	@DynamicSerializeElement
	private String[]	rangeStartTimeStrAry=null;

	@DynamicSerializeElement
	// x:lon, y:lat
	private Coordinate[] latLonAry=null;

	@DynamicSerializeElement
	private String[] stnIdAry = null;

	@DynamicSerializeElement
	private String modelType;  // grid model type name
	
	@DynamicSerializeElement
	private boolean merge=true; // default true, except when user request "raw data" for observed data.

	@DynamicSerializeElement
	private boolean interpolation=true; // default true, for grid model use only 
	
	@DynamicSerializeElement
	private String level;
		
	@DynamicSerializeElement
	private boolean pwRequired= false;
	
	public SoundingServiceRequest() {
		super();
		reqType = SoundingRequestType.NONE;
		sndType = SoundingType.NA;
	}

	public static enum SoundingRequestType {
		GET_SOUNDING_DATA_GENERIC,
		GET_SOUNDING_REF_TIMELINE,
		GET_SOUNDING_RANGESTART_TIMELINE,
		GET_SOUNDING_STATION_INFO,
		NONE
	}

	public static enum SoundingType {
		GRID_MODEL_SND,
		OBS_UAIR_SND,
		OBS_BUFRUA_SND,
		PFC_NAM_SND,
		PFC_GFS_SND,
		NA
	}

	public SoundingRequestType getReqType() {
		return reqType;
	}

	public void setReqType(SoundingRequestType reqType) {
		this.reqType = reqType;
	}

	public SoundingType getSndType() {
		return sndType;
	}

	public void setSndType(SoundingType sndType) {
		this.sndType = sndType;
	}

	public long[] getRefTimeAry() {
		return refTimeAry;
	}

	public void setRefTimeAry(long[] refTimeAry) {
		this.refTimeAry = refTimeAry;
	}

	public long[] getRangeStartTimeAry() {
		return rangeStartTimeAry;
	}

	public void setRangeStartTimeAry(long[] rangeStartTimeAry) {
		this.rangeStartTimeAry = rangeStartTimeAry;
	}

	public Coordinate[] getLatLonAry() {
		return latLonAry;
	}

	public void setLatLonAry(Coordinate[] latLonAry) {
		this.latLonAry = latLonAry;
	}

	public String[] getStnIdAry() {
		return stnIdAry;
	}

	public void setStnIdAry(String[] stnIdAry) {
		this.stnIdAry = stnIdAry;
	}

	public boolean isMerge() {
		return merge;
	}

	public void setMerge(boolean merge) {
		this.merge = merge;
	}

	public boolean isInterpolation() {
		return interpolation;
	}

	public void setInterpolation(boolean interpolation) {
		this.interpolation = interpolation;
	}

	public String getModelType() {
		return modelType;
	}

	public void setModelType(String modelType) {
		this.modelType = modelType;
	}

	public String[] getRefTimeStrAry() {
		return refTimeStrAry;
	}

	public void setRefTimeStrAry(String[] refTimeStrAry) {
		this.refTimeStrAry = refTimeStrAry;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public boolean isPwRequired() {
		return pwRequired;
	}

	public void setPwRequired(boolean pwRequired) {
		this.pwRequired = pwRequired;
	}

	public String[] getRangeStartTimeStrAry() {
		return rangeStartTimeStrAry;
	}

	public void setRangeStartTimeStrAry(String[] rangeStartTimeStrAry) {
		this.rangeStartTimeStrAry = rangeStartTimeStrAry;
	}

	
}
