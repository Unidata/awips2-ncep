package gov.noaa.nws.ncep.ui.nsharp.display.map;

/**
 * 
 * gov.noaa.nws.ncep.ui.nsharp.display.map.NsharpObservedSoundingQuery
 * 
 * This java class performs the NSHARP observed sounding data query functions.
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    	Engineer    Description
 * -------		------- 	-------- 	-----------
 * 11/1/2010	362			Chin Chen	Initial coding
 * 12/16/2010   362         Chin Chen   add support of BUFRUA observed sounding and PFC (NAM and GFS) model sounding data
 * 07142015     RM#9173     Chin Chen   use NcSoundingQuery.genericSoundingDataQuery() to query observed sounding data
 * 09/28/2015   RM#10295    Chin Chen   Let sounding data query run in its own thread to avoid gui locked out during load
 *
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.locationtech.jts.geom.Coordinate;

public class NsharpObservedSoundingQuery extends Job {
    private List<NsharpStationInfo> stnPtDataLineLst;
    private Map<String, List<NcSoundingLayer>> soundingLysLstMap;
    private boolean rawData;

    public NsharpObservedSoundingQuery(String name) {
        super(name);
    }

    public void getObservedSndData(List<NsharpStationInfo> stnPtDataLineLst,
            boolean rawData,
            Map<String, List<NcSoundingLayer>> soundingLysLstMap) {
        this.soundingLysLstMap = soundingLysLstMap;
        this.stnPtDataLineLst = stnPtDataLineLst;
        this.rawData = rawData;
        if (this.getState() != Job.RUNNING) {
            this.schedule();
        }
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        List<Coordinate> coords = new ArrayList<Coordinate>();
        List<Long> refTimeLst = new ArrayList<Long>();
        // note that Nsharp currently GUI only allow user pick one stn at
        // one time, but user can and may pick multiple refTimes.
        // create refTime array and lat/lon array
        for (NsharpStationInfo StnPt : stnPtDataLineLst) {
            // one StnPt represent one data time line
            boolean exist = false;
            for (Coordinate c : coords) {
                if (c.x == StnPt.getLongitude() && c.y == StnPt.getLatitude()) {
                    exist = true;
                    break;
                }
            }
            if (exist == false) {
                Coordinate coord = new Coordinate(StnPt.getLongitude(),
                        StnPt.getLatitude());
                coords.add(coord);
            }
            exist = false;
            for (long t : refTimeLst) {
                if (t == StnPt.getReftime().getTime()) {
                    exist = true;
                    break;
                }
            }
            if (exist == false) {
                refTimeLst.add(StnPt.getReftime().getTime());
            }

        }
        Coordinate[] latLonAry = new Coordinate[coords.size()];
        for (int i = 0; i < coords.size(); i++) {
            latLonAry[i] = coords.get(i);
        }
        Long[] refTimeArray = refTimeLst.toArray(new Long[0]);
        long[] refTimelArray = new long[refTimeArray.length];
        for (int i = 0; i < refTimeArray.length; i++)
            refTimelArray[i] = refTimeArray[i];
        NcSoundingCube cube = NcSoundingQuery.genericSoundingDataQuery(
                refTimelArray, null, null, null, latLonAry, null,
                stnPtDataLineLst.get(0).getSndType(),
                NcSoundingLayer.DataType.ALLDATA, !rawData, null, null, true,
                false, false);
        if (cube != null && cube.getSoundingProfileList().size() > 0
                && cube.getRtnStatus() == NcSoundingCube.QueryStatus.OK) {
            for (NcSoundingProfile sndPf : cube.getSoundingProfileList()) {
                List<NcSoundingLayer> rtnSndLst = sndPf.getSoundingLyLst();
                if (rtnSndLst != null && rtnSndLst.size() > 0) {
                    // update sounding data so they can be used by Skewt
                    // Resource and PalletWindow
                    if (rawData)
                        rtnSndLst = NsharpDataHandling
                                .sortObsSoundingDataForShow(rtnSndLst,
                                        sndPf.getStationElevation());
                    else
                        rtnSndLst = NsharpDataHandling
                                .organizeSoundingDataForShow(rtnSndLst,
                                        sndPf.getStationElevation());
                    // minimum rtnSndList size will be 2 (50 & 75 mb layers),
                    // but that is not enough
                    // We need at least 4 regular layers for plotting
                    if (rtnSndLst != null && rtnSndLst.size() > 4) {
                        String dispInfo = "";
                        for (NsharpStationInfo StnPt : stnPtDataLineLst) {
                            if (StnPt.getReftime().getTime() == sndPf
                                    .getFcsTime()) {
                                dispInfo = StnPt.getStnDisplayInfo();
                                break;
                            }
                        }
                        soundingLysLstMap.put(dispInfo, rtnSndLst);
                    }
                }
            }
        }

        NsharpSoundingQueryCommon.handleQueryResponse(stnPtDataLineLst.get(0),
                soundingLysLstMap);

        return Status.OK_STATUS;
    }

}
