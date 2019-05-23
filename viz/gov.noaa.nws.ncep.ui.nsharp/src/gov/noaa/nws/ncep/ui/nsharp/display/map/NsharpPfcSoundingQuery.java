package gov.noaa.nws.ncep.ui.nsharp.display.map;

/**
 * 
 * gov.noaa.nws.ncep.ui.nsharp.display.map.NsharpPfcSoundingQuery
 * 
 * This java class performs the NSHARP pfc sounding data query functions.
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    	Engineer    Description
 * -------		------- 	-------- 	-----------
 * 11/1/2010	362			Chin Chen	Initial coding
 * 12/16/2010   362         Chin Chen   add support of BUFRUA observed sounding and PFC (NAM and GFS) model sounding data
 * 02/15/2012               Chin Chen   add  PFC sounding query algorithm for better performance getPfcSndDataBySndTmRange()
 * Aug 05, 2015 4486        rjpeter     Changed Timestamp to Date.
 * 07202015     RM#9173     Chin Chen   use NcSoundingQuery.genericSoundingDataQuery() directly to query pc sounding data
 * 09/28/2015   RM#10295    Chin Chen   Let sounding data query run in its own thread to avoid gui locked out during load
 * </pre>
 * 
 * @author Chin Chen
 * @version 1.0
 */

import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingCube;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingProfile;
import gov.noaa.nws.ncep.ui.nsharp.NsharpStationInfo;
import gov.noaa.nws.ncep.ui.nsharp.natives.NsharpDataHandling;
import gov.noaa.nws.ncep.viz.soundingrequest.NcSoundingQuery;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.locationtech.jts.geom.Coordinate;

public class NsharpPfcSoundingQuery extends Job {
    private List<NsharpStationInfo> stnPtDataLineLst;

    private Map<String, List<NcSoundingLayer>> soundingLysLstMap;

    public NsharpPfcSoundingQuery(String name) {
        super(name);
    }

    public void getPfcSndDataBySndTmRange(
            List<NsharpStationInfo> stnPtDataLineLst,
            Map<String, List<NcSoundingLayer>> soundingLysLstMap) {
        this.soundingLysLstMap = soundingLysLstMap;
        this.stnPtDataLineLst = stnPtDataLineLst;
        if (this.getState() != Job.RUNNING) {
            this.schedule();
        }
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        String stnDispInfo = "";
        NcSoundingCube cube;
        // one StnPt represent one data time line
        for (NsharpStationInfo StnPt : this.stnPtDataLineLst) {
            long[] rangeTimeArray = new long[StnPt.getTimeLineSpList().size()];
            int i = 0;
            for (NsharpStationInfo.timeLineSpecific tmlinSpc : StnPt
                    .getTimeLineSpList()) {
                Date rangeTime = tmlinSpc.getTimeLine();
                rangeTimeArray[i] = rangeTime.getTime();
                i++;
            }
            long[] refLTimeAry = { StnPt.getReftime().getTime() };
            Coordinate[] coordArray = new Coordinate[1];
            Coordinate latlon = new Coordinate(StnPt.getLongitude(),
                    StnPt.getLatitude());
            coordArray[0] = latlon;
            cube = NcSoundingQuery.genericSoundingDataQuery(refLTimeAry,
                    rangeTimeArray, null, null, coordArray, null,
                    StnPt.getSndType(), NcSoundingLayer.DataType.ALLDATA,
                    false, "-1", null, true, false, false);
            if (cube != null && cube.getSoundingProfileList().size() > 0) {
                for (NcSoundingProfile sndPf : cube.getSoundingProfileList()) {
                    List<NcSoundingLayer> rtnSndLst = sndPf.getSoundingLyLst();
                    if (rtnSndLst != null && rtnSndLst.size() > 0) {
                        rtnSndLst = NsharpDataHandling
                                .organizeSoundingDataForShow(rtnSndLst,
                                        sndPf.getStationElevation());
                        // minimum rtnSndList size will be 2 (50 & 75 mb
                        // layers), but that is not enough
                        // We need at least 4 regular layers for plotting
                        if (rtnSndLst != null && rtnSndLst.size() > 4) {
                            stnDispInfo = "NA";
                            for (int j = 0; j < StnPt.getTimeLineSpList()
                                    .size(); j++) {
                                NsharpStationInfo.timeLineSpecific tmlinSpcj = StnPt
                                        .getTimeLineSpList().get(j);
                                if (tmlinSpcj.getTimeLine().getTime() == sndPf
                                        .getFcsTime()) {
                                    stnDispInfo = tmlinSpcj.getDisplayInfo();
                                    break;
                                }
                            }
                            soundingLysLstMap.put(stnDispInfo, rtnSndLst);
                        }
                    }
                }
            }
        }
        NsharpSoundingQueryCommon.handleQueryResponse(stnPtDataLineLst.get(0),
                soundingLysLstMap);
        return Status.OK_STATUS;
    }
}
