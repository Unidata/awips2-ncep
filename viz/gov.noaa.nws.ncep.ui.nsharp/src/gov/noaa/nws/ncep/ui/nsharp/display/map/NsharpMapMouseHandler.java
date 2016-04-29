/**
 * 
 * gov.noaa.nws.ncep.ui.nsharp.display.map.NsharpMapMouseHandler
 * 
 * This java class performs the NSHARP Modal functions.
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    	Engineer    Description
 * -------		------- 	-------- 	-----------
 * 03/23/2010	229			Chin Chen	Initial coding
 * 03/11/2013   972         Greg Hull   NatlCntrsEditor
 * 09/28/2015   RM#10295    Chin Chen   Let sounding data query run in its own thread to avoid gui locked out during load
 *
 * </pre>
 * 
 * @author Chin Chen
 * @version 1.0
 */
package gov.noaa.nws.ncep.ui.nsharp.display.map;

import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;
import gov.noaa.nws.ncep.ui.nsharp.NsharpStationInfo;
import gov.noaa.nws.ncep.ui.nsharp.SurfaceStationPointData;
import gov.noaa.nws.ncep.ui.nsharp.display.NsharpEditor;
import gov.noaa.nws.ncep.ui.nsharp.view.NsharpLoadDialog;
import gov.noaa.nws.ncep.ui.nsharp.view.NsharpModelSoundingDialogContents;
import gov.noaa.nws.ncep.ui.pgen.tools.InputHandlerDefaultImpl;
import gov.noaa.nws.ncep.viz.ui.display.NatlCntrsEditor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.progress.UIJob;
import org.geotools.referencing.GeodeticCalculator;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.map.IMapDescriptor;
import com.vividsolutions.jts.geom.Coordinate;

public class NsharpMapMouseHandler extends InputHandlerDefaultImpl {
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(NsharpMapMouseHandler.class);

    public NsharpMapMouseHandler() {
        instance = this;
    }

    private static final double NctextuiPointMinDistance = 45000;

    private static NsharpMapMouseHandler instance;

    private double lat, lon;

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public static NsharpMapMouseHandler getAccess() {
        return instance;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.ui.input.IInputHandler#handleMouseDown(int, int,
     * int)
     */
    @Override
    public boolean handleMouseDown(int x, int y, int button) {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.ui.input.IInputHandler#handleMouseDownMove(int,
     * int, int) handle left button, so user be able to shift map while it is
     * down
     */
    @Override
    public boolean handleMouseDownMove(int x, int y, int button) {
        return false;

    }

    @Override
    public boolean handleMouseMove(int x, int y) {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.ui.input.IInputHandler#handleMouseUp(int, int, int)
     * handle right button, so user be able to pick stn and print text report
     */
    @Override
    public boolean handleMouseUp(int x, int y, int button) {
        if (!NsharpMapResource.getMapRsc().isEditable())
            return false;
        // button 1 is left mouse button
        if (button == 1) {
            NatlCntrsEditor mapEditor = NsharpMapResource.getMapEditor();
            if (mapEditor != null) {
                // Check if mouse is in geographic extent
                Coordinate loc = mapEditor.translateClick(x, y);
                if (loc == null)
                    return false;
                NsharpLoadDialog loadDia = NsharpLoadDialog.getAccess();
                if (loadDia != null) {
                    if (loadDia.getActiveLoadSoundingType() == NsharpLoadDialog.MODEL_SND
                            && loadDia.getMdlDialog() != null
                            && loadDia.getMdlDialog().getLocationText() != null) {

                        if (loadDia.getMdlDialog().getCurrentLocType() == NsharpModelSoundingDialogContents.LocationType.STATION) {
                            String stnName = SurfaceStationPointData
                                    .calculateNearestPoint(loc);
                            if (stnName == null)
                                stnName = "";
                            loadDia.getMdlDialog().getLocationText()
                                    .setText(stnName);
                        } else {
                            String latLonStr = String.format("%6.2f;%7.2f",
                                    loc.y, loc.x);
                            loadDia.getMdlDialog().getLocationText()
                                    .setText(latLonStr);
                        }

                    } else {
                        // get the stn (point) list
                        int activeLoadType = loadDia
                                .getActiveLoadSoundingType();
                        List<NsharpStationInfo> points = NsharpMapResource
                                .getOrCreateNsharpMapResource().getPoints();
                        if (points.isEmpty() == false) {

                            // get the stn close to loc "enough" and retrieve
                            // report for it
                            // Note::One stn may have more than one dataLine, if
                            // user picked multiple data time lines
                            List<NsharpStationInfo> stnPtDataLineLst = getPtWithinMinDist(
                                    points, loc);
                            if (stnPtDataLineLst != null
                                    && stnPtDataLineLst.size() > 0) {
                                // hash map, use stn display info as key
                                Map<String, List<NcSoundingLayer>> soundingLysLstMap = new HashMap<String, List<NcSoundingLayer>>();
                                // String soundingType;
                                if (activeLoadType == NsharpLoadDialog.OBSER_SND) {
                                    NsharpObservedSoundingQuery obsQry = new NsharpObservedSoundingQuery(
                                            "Querying Sounding Data...");
                                    obsQry.getObservedSndData(stnPtDataLineLst,
                                            loadDia.getObsDialog().isRawData(),
                                            soundingLysLstMap);
                                } else if (activeLoadType == NsharpLoadDialog.PFC_SND) {
                                    NsharpPfcSoundingQuery pfcQry = new NsharpPfcSoundingQuery(
                                            "Querying Sounding Data...");
                                    pfcQry.getPfcSndDataBySndTmRange(
                                            stnPtDataLineLst, soundingLysLstMap);
                                }
                                /*
                                 * Add GPD code here when we start to support
                                 * it.
                                 */
                                else
                                    return false;
                            }
                        }
                    }
                }
            }

        } else if (button == 3) {
            // button 3 is right button.
            bringSkewTEdToTop();
        }

        return false;
    }

    /*
     * Chin Note: If calling NsharpEditor.bringSkewTEditorToTop() directly in
     * mouse handler API, e.g. handleMouseUp(), then handleMouseUp() will be
     * called one more time by System. Do not know the root cause of it. To
     * avoid handling such event twice (e.g. query sounding data twice), we will
     * call NsharpEditor.bringSkewTEditorToTop() from an UiJob.
     */
    private void bringSkewTEdToTop() {
        Job uijob = new UIJob("bring skewT to top") {
            public IStatus runInUIThread(IProgressMonitor monitor) {
                NsharpEditor.bringEditorToTop();
                return Status.OK_STATUS;
            }

        };
        uijob.setSystem(true);
        uijob.schedule();
    }

    /**
     * Gets the nearest point of an selected element to the input point
     * 
     * @param el
     *            element
     * @param pt
     *            input point
     * @return
     */
    private List<NsharpStationInfo> getPtWithinMinDist(
            List<NsharpStationInfo> points, Coordinate pt) {

        NsharpStationInfo thePoint = null;
        double minDistance = NctextuiPointMinDistance;
        GeodeticCalculator gc;
        List<NsharpStationInfo> thePoints = new ArrayList<NsharpStationInfo>();
        // can't assume this is a map Editor/MapDescriptor
        NatlCntrsEditor mapEditor = NsharpMapResource.getMapEditor();
        if (mapEditor != null) {
            IMapDescriptor desc = (IMapDescriptor) mapEditor
                    .getActiveDisplayPane().getRenderableDisplay()
                    .getDescriptor();
            gc = new GeodeticCalculator(desc.getCRS());
            gc.setStartingGeographicPoint(pt.x, pt.y);
            for (NsharpStationInfo point : points) {

                gc.setDestinationGeographicPoint(point.getLongitude(),
                        point.getLatitude());
                double dist;
                try {
                    dist = gc.getOrthodromicDistance();
                    if (dist < minDistance) {

                        minDistance = dist;
                        thePoint = point;
                    }
                } catch (Exception e) {
                    statusHandler
                            .handle(Priority.ERROR,
                                    "NsharpMapMouseHandler: getOrthodromicDistance exception happened!",
                                    e);
                }
            }
            // Chin, there may be more than one point for a selected stn. As
            // user may selected more than one data time,
            // For same stn, each data time will have one point to represent it.
            // So, a stn may have more than one points
            if (thePoint != null) {
                for (NsharpStationInfo point : points) {
                    if ((thePoint.getLatitude() == point.getLatitude())
                            && (thePoint.getLongitude() == point.getLongitude())) {
                        thePoints.add(point);
                    }
                }

                // marked X on selected point
                NsharpMapResource.getOrCreateNsharpMapResource()
                        .setPickedPoint(thePoint);

            }

        }
        return thePoints;

    }
}
