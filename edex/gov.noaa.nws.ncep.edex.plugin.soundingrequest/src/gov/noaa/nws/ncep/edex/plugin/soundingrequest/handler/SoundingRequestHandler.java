package gov.noaa.nws.ncep.edex.plugin.soundingrequest.handler;
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
 *
 * </pre>
 * 
 * @author Chin Chen
 * @version 1.0
 */



import gov.noaa.nws.ncep.common.dataplugin.soundingrequest.SoundingServiceRequest;
import gov.noaa.nws.ncep.common.dataplugin.soundingrequest.SoundingServiceRequest.SoundingRequestType;
import gov.noaa.nws.ncep.common.dataplugin.soundingrequest.SoundingServiceRequest.SoundingType;

import com.raytheon.uf.common.serialization.comm.IRequestHandler;

public class SoundingRequestHandler implements
		IRequestHandler<SoundingServiceRequest> {

	@Override
	public Object handleRequest(SoundingServiceRequest request) throws Exception {
		SoundingRequestType reqType = request.getReqType();
		SoundingType sndType = request.getSndType();
		String[] refTimeAry, rangeStartTimeAry;
		switch(reqType){
		case GET_SOUNDING_DATA_GENERIC:
			switch(sndType){
			case GRID_MODEL_SND:
				return((Object)ModelsSoundingQuery.handleGridModelDataRequest(request));
			case OBS_UAIR_SND:
				return((Object)(ObsSoundingQuery.handleNcuairDataRequest(request)));
			case OBS_BUFRUA_SND:
				return((Object)(ObsSoundingQuery.handleBufruaDataRequest(request)));
			case PFC_NAM_SND:
			case PFC_GFS_SND:
				return((Object)(PfcSoundingQuery.handlePfcDataRequest(request)));
			default:
				return null;
			}
		case GET_SOUNDING_REF_TIMELINE:
			switch(sndType){
			case OBS_UAIR_SND:
			case OBS_BUFRUA_SND:
				return((Object)(ObsSoundingQuery.getObservedSndTimeLine(sndType)));
			case PFC_NAM_SND:
			case PFC_GFS_SND:
				return((Object)(PfcSoundingQuery.getPfcSndTimeLine(sndType)));
			case GRID_MODEL_SND:
				//Chin Note:  we are using ncInventory currently
				//ModelsSoundingQuery.getMdlSndTimeLine(mdlType);
				//break;
			default:
				return null;
			}
		case GET_SOUNDING_RANGESTART_TIMELINE:
			switch(sndType){
			case GRID_MODEL_SND:
				refTimeAry = request.getRefTimeStrAry();
				return((Object)(ModelsSoundingQuery.getMdlSndRangeTimeLine(request.getModelType(), refTimeAry[0])));
			case PFC_NAM_SND:
			case PFC_GFS_SND:
				refTimeAry = request.getRefTimeStrAry();
				return((Object)(PfcSoundingQuery.getPfcSndRangeTimeLine(sndType, refTimeAry[0])));
			default:
				return null;
			}
		case GET_SOUNDING_STATION_INFO:
			switch(sndType){
			case OBS_UAIR_SND:
			case OBS_BUFRUA_SND:
				refTimeAry = request.getRefTimeStrAry();
				return((Object)(ObsSoundingQuery.getObservedSndStnInfoCol(sndType,refTimeAry[0])));
			case PFC_NAM_SND:
			case PFC_GFS_SND:
				refTimeAry = request.getRefTimeStrAry();
				rangeStartTimeAry = request.getRangeStartTimeStrAry();
				return((Object)(PfcSoundingQuery.getPfcSndStnInfoCol(sndType, rangeStartTimeAry[0], refTimeAry[0])));
			case GRID_MODEL_SND:
			default:
				return null;
			}
		default:
			return null;
		}
	}

}
