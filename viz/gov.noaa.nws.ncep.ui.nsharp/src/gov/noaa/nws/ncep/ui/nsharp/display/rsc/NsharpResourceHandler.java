/**
 * 
 * gov.noaa.nws.ncep.ui.nsharp.display.rsc.NsharpResourceHandler
 * 
 * This java class performs the NSHARP NsharpSkewTResource functions.
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date        Ticket#   Engineer   Description
 * -------     --------  --------   -----------
 * 04/30/2012  229       Chin Chen  Initial coding
 * 01/13/2014            Chin Chen  TTR829- when interpolation, edit graph is allowed
 * 02/03/2014  1106      Chin Chen  Need to be able to use clicking on the Src,Time, or StnId to select display
 * 08/12/2014            Chin Chen  fixed issue that "load archive file with wrong time line displayed"
 * 12/04/2014  DR16888   Chin Chen  fixed issue that "Comp(Src) button not functioning properly in NSHARP display"
 * 01/27/2015  DR#17006,
 *             Task#5929 Chin Chen  NSHARP freezes when loading a sounding from MDCRS products
 *                                  in Volume Browser
 * 02/03/2015  DR#17079  Chin Chen  Soundings listed out of order if frames go into new month
 * 02/05/2015  DR16888   CHin Chen  merge 12/11/2014 fixes at version 14.2.2 and check in again to 14.3.1
 * 08/10/2015  RM#9396   Chin Chen  implement new OPC pane configuration
 * 07/05/2016  RM#15923  Chin Chen  NSHARP - Native Code replacement
 * 09/05/2018  DCS20492  a.rivera   Resolve merge conflicts resulting from reverting 18.1.1 DCS 17377.
 * 10/02/2018   7475     bsteffen   Fix casting error when D2D resources are present.
 * 10/05/2018   7480     bsteffen   Handle remove from d2d.
 * 10/16/2018  6835      bsteffen   Extract printing logic.
 * 10/18/2018  7476      bsteffen   Do not reset parcel when data changes.
 * 11/05/2018   6800     bsteffen   Extract click indexing to time/station resource.
 * 10/18/2018  7476      bsteffen   Do not reset parcel when data changes.
 * 11/13/2018  7576      bsteffen   Unify activation dialogs.
 * 11/21/2018  7574      bsteffen   Fix comparison and overlay coloring.
 * 12/14/2018  6872      bsteffen   Track time more accurately.
 * 12/20/2018  7575      bsteffen   Do not reuse parcel dialog
 * 01/20/2019  17377     wkwock     Auto-update new arrival NSHARP display.
 *
 * </pre>
 * 
 * @author Chin Chen
 */
package gov.noaa.nws.ncep.ui.nsharp.display.rsc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.swt.graphics.RGB;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.datum.DefaultEllipsoid;

import com.raytheon.uf.common.sounding.WxMath;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.datastructure.LoopProperties;
import com.raytheon.uf.viz.core.drawables.IDescriptor.FramesInfo;
import com.raytheon.uf.viz.core.drawables.IFrameCoordinator;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.IWireframeShape;
import com.vividsolutions.jts.geom.Coordinate;

import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibBasics;
import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibSndglib;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;
import gov.noaa.nws.ncep.ui.nsharp.NsharpConfigManager;
import gov.noaa.nws.ncep.ui.nsharp.NsharpConfigStore;
import gov.noaa.nws.ncep.ui.nsharp.NsharpConstants;
import gov.noaa.nws.ncep.ui.nsharp.NsharpDataPageProperty;
import gov.noaa.nws.ncep.ui.nsharp.NsharpElementDescription;
import gov.noaa.nws.ncep.ui.nsharp.NsharpGraphProperty;
import gov.noaa.nws.ncep.ui.nsharp.NsharpLineProperty;
import gov.noaa.nws.ncep.ui.nsharp.NsharpOperationElement;
import gov.noaa.nws.ncep.ui.nsharp.NsharpSoundingElementStateProperty;
import gov.noaa.nws.ncep.ui.nsharp.NsharpStationInfo;
import gov.noaa.nws.ncep.ui.nsharp.NsharpTimeOperationElement;
import gov.noaa.nws.ncep.ui.nsharp.NsharpWGraphics;
import gov.noaa.nws.ncep.ui.nsharp.NsharpWxMath;
import gov.noaa.nws.ncep.ui.nsharp.display.NsharpEditor;
import gov.noaa.nws.ncep.ui.nsharp.display.map.NsharpMapResource;
import gov.noaa.nws.ncep.ui.nsharp.natives.NsharpDataHandling;
import gov.noaa.nws.ncep.ui.nsharp.view.NsharpPaletteWindow;
import gov.noaa.nws.ncep.ui.nsharp.view.NsharpShowTextDialog;

public class NsharpResourceHandler {
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(NsharpResourceHandler.class);

    private IRenderableDisplay[] displayArray = null;

    private NsharpPartListener.PartEvent editorPartStatus = NsharpPartListener.PartEvent.partClosed;

    private NsharpSkewTPaneResource skewtPaneRsc;

    private NsharpWitoPaneResource witoPaneRsc;

    private NsharpHodoPaneResource hodoPaneRsc;

    private NsharpTimeStnPaneResource timeStnPaneRsc;

    private NsharpInsetPaneResource insetPaneRsc;

    private NsharpDataPaneResource dataPaneRsc;

    private NsharpSpcGraphsPaneResource spcGraphsPaneRsc;

    private int displayDataPageMax;

    private static final int INSETPAGEMAX = 2;

    private int currentTextChapter = 1;

    private int currentInsetPage = 1;

    private String paneConfigurationName;

    private NsharpWeatherDataStore weatherDataStore;

    private NsharpConfigManager configMgr;

    private NsharpConfigStore configStore;

    private NsharpGraphProperty graphConfigProperty;

    private HashMap<String, NsharpLineProperty> linePropertyMap;

    private NsharpDataPageProperty dataPageProperty;

    // index is the real page defined in NsharpConstants to be shown, value is
    // the order number of this page.
    // index 0 point to a dummy.
    private int[] pageDisplayOrderNumberArray = new int[NsharpConstants.PAGE_MAX_NUMBER
            + 1];

    private boolean goodData = false;

    private boolean overlayIsOn = false;

    private boolean interpolateIsOn = false;

    private boolean compareSndIsOn = false;

    private boolean compareStnIsOn = false;

    private boolean compareTmIsOn = false;

    private boolean editGraphOn = false;

    private boolean getTimeMatcher = false;

    public int TEMP_TYPE = 1;

    public int DEWPOINT_TYPE = 2;

    private int currentTempCurveType;

    private int currentSoundingLayerIndex = 0;

    private int hodoEditingSoundingLayerIndex = 0;

    private boolean plotInteractiveTemp = false;

    private Coordinate interactiveTempPointCoordinate;

    public static final float INVALID_DATA = NcSoundingLayer.MISSING;;

    public static final double BARB_LENGTH = 3.5;

    private String soundingType = null;

    protected DataTime displayedSounding;

    private int currentGraphMode = NsharpConstants.GRAPH_SKEWT;

    protected ListenerList listenerList = new ListenerList();

    // current active sounding layer list
    private List<NcSoundingLayer> soundingLys = null;

    private List<NcSoundingLayer> previousSoundingLys = null;

    // current picked stn info with time line and sounding source,
    // e.g. "ATLH 101209/03(Thu)V003 GFS230"
    private String pickedStnInfoStr;

    private NsharpStationInfo pickedStnInfo = null;

    // next =forward
    private IFrameCoordinator.FrameChangeOperation currentOpDirection = IFrameCoordinator.FrameChangeOperation.NEXT;

    private HashMap<Integer, RGB> stormSlinkyColorMap = new HashMap<>();

    private List<List<List<NsharpSoundingElementStateProperty>>> stnTimeSndTable = new ArrayList<>();

    // stnTimeSndTable:
    // Store all sounding profiles property for GUI display control
    // 1st index refer to stnId, 2nd index refer to time line and 3rd point to
    // sndType.
    // It is same as [][][] 3d array.
    // We dynamically expand this 3D array based on newly added
    // stnid/timeline/sndType When a new sounding data is loaded,
    // All unloaded element is null. Only when user load new sounding with this
    // stnId/this time line/this sndType, then
    // the element allocated.
    //
    // stn3-> T1--->T2--->T3->...
    // ^
    // /
    // stn2-> T1--->T2--->T3->...
    // ^
    // /
    // stn1-> T1--->T2--->T3->...
    // | | |
    // V V V
    // snd1 snd1 snd1
    // | | |
    // V V V
    // snd2 snd2 snd2
    // | | |
    // V V V
    // stnTimeSndTable first dimension (station id) should be in sync with
    // stnElementList,
    // 2nd dimension (time line) should be in sync with timeElementList, and
    // 3rd dimension (sounding type) should be in sync with sndTypeElementList
    // NULL element in stnTimeSndTable indicates that sounding data is not
    // loaded yet.

    private List<NsharpOperationElement> stnElementList = new ArrayList<>();

    private List<NsharpTimeOperationElement> timeElementList = new ArrayList<>();

    private List<NsharpOperationElement> sndElementList = new ArrayList<>();

    private NsharpSoundingElementStateProperty curSndProfileProp = null;

    private NsharpSoundingElementStateProperty preSndProfileProp = null;

    // index to first dim of stnTimeSndTable and index to stnElementList
    private int currentStnElementListIndex = -1;

    // index to 2nd dim of stnTimeSndTable and index to timeElementList
    private int currentTimeElementListIndex = -1;

    // index to 3rd dim of stnTimeSndTable and index to sndElementList
    private int currentSndElementListIndex = -1;

    // index to track the last currentTimeElementListIndex selected by user
    private int lastUserSelectedTimeLineIndex = -1;

    // The last user selected time line
    private NsharpTimeOperationElement lastUserSelectedTimeLine = null;

    // use element state, NsharpConstants.LoadState or NsharpConstants.ActState,
    // as key to set color for drawing
    private HashMap<String, RGB> elementColorMap = new HashMap<>();

    private int currentParcel = NsharpLibSndglib.PARCELTYPE_MOST_UNSTABLE;

    private float currentParcelLayerPressure = NsharpLibSndglib.MU_LAYER_PRESS;
    
    public class CompSndSelectedElem {
        private int stnIndex;

        private int timeIndex;

        private int sndIndex;

        public CompSndSelectedElem(int stnIndex, int timeIndex, int sndIndex) {
            super();
            this.stnIndex = stnIndex;
            this.timeIndex = timeIndex;
            this.sndIndex = sndIndex;
        }

        public int getSndIndex() {
            return sndIndex;
        }

        public void setSndIndex(int sndIndex) {
            this.sndIndex = sndIndex;
        }

        public int getStnIndex() {
            return stnIndex;
        }

        public void setStnIndex(int stnIndex) {
            this.stnIndex = stnIndex;
        }

        public int getTimeIndex() {
            return timeIndex;
        }

        public void setTimeIndex(int timeIndex) {
            this.timeIndex = timeIndex;
        }

    }

    private List<CompSndSelectedElem> compSndSelectedElemList = new ArrayList<>();

    private int sndCompRadius = 0;

    public List<CompSndSelectedElem> getCompSndSelectedElemList() {
        return compSndSelectedElemList;
    }

    public List<List<List<NsharpSoundingElementStateProperty>>> getStnTimeSndTable() {
        return stnTimeSndTable;
    }

    public List<String> getAllLoadedSndDesciptionList() {
        List<String> strLst = new ArrayList<>();
        for (List<List<NsharpSoundingElementStateProperty>> tlListList : stnTimeSndTable) {
            // add a new element for the new sndType to each existing sndlist of
            // each existing time of each existing stnId
            for (List<NsharpSoundingElementStateProperty> sndtyList : tlListList) {
                for (NsharpSoundingElementStateProperty elem : sndtyList) {
                    if (elem != null) {
                        strLst.add(elem.getElementDescription());
                    }
                }
            }
        }
        return strLst;
    }

    public List<NsharpOperationElement> getStnElementList() {
        return stnElementList;
    }

    public List<NsharpTimeOperationElement> getTimeElementList() {
        return timeElementList;
    }

    public List<NsharpOperationElement> getSndElementList() {
        return sndElementList;
    }

    public int getCurrentStnElementListIndex() {
        return currentStnElementListIndex;
    }

    public int getCurrentTimeElementListIndex() {
        return currentTimeElementListIndex;
    }

    public int getTimeElementListSize() {
        return timeElementList.size();
    }

    public int getCurrentSndElementListIndex() {
        return currentSndElementListIndex;
    }

    // shape and color storage
    public class ShapeAndLineProperty {
        IWireframeShape shape;

        NsharpLineProperty lp;

        public ShapeAndLineProperty() {
            super();
            lp = new NsharpLineProperty();
        }

    }

    public Map<String, NsharpLineProperty> getLinePropertyMap() {
        return linePropertyMap;
    }

    public boolean isCompareStnIsOn() {
        return compareStnIsOn;
    }

    public boolean isCompareTmIsOn() {
        return compareTmIsOn;
    }

    public boolean isCompareSndIsOn() {
        return compareSndIsOn;
    }

    public boolean isOverlayIsOn() {
        return overlayIsOn;
    }

    public void setNextTextChapter() {
        if (currentTextChapter == displayDataPageMax) {
            currentTextChapter = 1;
        } else {
            currentTextChapter += 1;
            // d2dlite or OCP if one page per chap case, skip future page
            if ((dataPageProperty.getNumberPagePerDisplay() == 1
                    || paneConfigurationName
                            .equals(NsharpConstants.PANE_LITE_D2D_CFG_STR)
                    || paneConfigurationName
                            .equals(NsharpConstants.PANE_OPC_CFG_STR))
                    && currentTextChapter == displayDataPageMax) {
                currentTextChapter = 1;
            }
        }
    }

    public void setPrevTextChapter() {
        currentTextChapter--;
        if (currentTextChapter == 0) {
            currentTextChapter = displayDataPageMax;
            // d2dlite or OPC if one page per chap case, skip future page
            if (dataPageProperty.getNumberPagePerDisplay() == 1
                    || paneConfigurationName
                            .equals(NsharpConstants.PANE_LITE_D2D_CFG_STR)
                    || paneConfigurationName
                            .equals(NsharpConstants.PANE_OPC_CFG_STR)) {
                currentTextChapter = displayDataPageMax - 1;
            }
        }
    }

    public void setPrevInsetPage() {
        currentInsetPage -= 1;
        if (currentInsetPage == 0) {
            currentInsetPage = INSETPAGEMAX;
        }
    }

    public void setNextInsetPage() {
        if (currentInsetPage == INSETPAGEMAX) {
            currentInsetPage = 1;
        } else {
            currentInsetPage += 1;
        }
    }

    public void setOverlayIsOn(boolean overlay) {
        this.overlayIsOn = overlay;
        if (overlay && soundingLys != null && previousSoundingLys == null) {
            for (int snd = 0; snd < sndElementList.size(); snd += 1) {
                for (int stn = 0; stn < stnElementList.size(); stn += 1) {
                    for (int time = 0; time < timeElementList
                            .size(); time += 1) {
                        NsharpSoundingElementStateProperty prop = stnTimeSndTable
                                .get(stn).get(time).get(snd);
                        if (prop != null && prop != curSndProfileProp) {
                            preSndProfileProp = prop;
                            previousSoundingLys = prop.getSndLyLst();
                        }
                    }
                }

            }
        }
        
        if (hodoPaneRsc != null){
            hodoPaneRsc.createRscHodoWindShapeAll();
        }
        if (skewtPaneRsc != null){
            skewtPaneRsc.handleResize();
        }
    }

    public boolean isInterpolateIsOn() {
        return interpolateIsOn;
    }

    /*
     * When compareStnIsOn is changed,
     */
    public void setCompareStnIsOn(boolean compareIsOn) {
        this.compareStnIsOn = compareIsOn;

        // This is the case when sounding data is not available at
        // currentTimeElementListIndex/currentSndElementListIndex/currentStnElementListIndex
        // and user set compare stn on.
        if (compareStnIsOn) {
            if (soundingLys == null && currentTimeElementListIndex >= 0
                    && currentSndElementListIndex >= 0) {
                // find a new available stn for current time and sndType
                boolean found = false;
                for (int i = 0; i < stnElementList.size(); i += 1) {
                    if (stnElementList.get(i)
                            .getActionState() == NsharpConstants.ActState.ACTIVE
                            && stnTimeSndTable.get(i)
                                    .get(currentTimeElementListIndex)
                                    .get(currentSndElementListIndex) != null) {
                        found = true;
                        currentStnElementListIndex = i;
                    }

                    if (found) {
                        break;
                    }
                }
                if (!found) {
                    return;
                }
            }
        }
        setCurSndProfileProp();
        setCurrentSoundingLayerInfo();
        resetData();
    }

    public void setCompareSndIsOn(boolean compareSndIsOn) {
        this.compareSndIsOn = compareSndIsOn;
        // This is the case when sounding data is not available at
        // currentTimeElementListIndex/currentSndElementListIndex/currentStnElementListIndex
        // and user set compSnd on
        if (compareSndIsOn) {
            if (soundingLys == null && currentStnElementListIndex >= 0
                    && currentTimeElementListIndex >= 0) {
                // find a new available snd type for current time and stn
                boolean found = false;
                for (int i = 0; i < sndElementList.size(); i += 1) {
                    if (sndElementList.get(i)
                            .getActionState() == NsharpConstants.ActState.ACTIVE
                            && stnTimeSndTable.get(currentStnElementListIndex)
                                    .get(currentTimeElementListIndex)
                                    .get(i) != null) {
                        found = true;
                        currentSndElementListIndex = i;
                    }
                    if (found) {
                        break;
                    }
                }
                if (!found) {
                    return;
                }
            }
            /*
             * The following code is to create a list of stns within the range
             * of user defined radius (minimum distance) to "current" station
             * and also has data loaded with same time line as "current" time
             * line. Note that we have two time line formats, YYMMDD/HHVxxx(day)
             * and YYMMDD/HH(day) saved in NsharpSoundingElementStateProperty. A
             * same time line is compared by "YYMMDD/HH" only. All qualified
             * stations, including current station, found will be listed and
             * used for SND comparison.
             */
            String TIME_COMPARE_STRING = "YYMMDD/HH";
            compSndSelectedElemList.clear();
            // current station/time line/sounding element is always added to the
            // list
            GeodeticCalculator gc = new GeodeticCalculator(
                    DefaultEllipsoid.WGS84);
            NsharpStationInfo currentStnInfo = stnTimeSndTable
                    .get(currentStnElementListIndex)
                    .get(currentTimeElementListIndex)
                    .get(currentSndElementListIndex).getStnInfo();
            gc.setStartingGeographicPoint(currentStnInfo.getLongitude(),
                    currentStnInfo.getLatitude());

            int timeLineLengthToComp = TIME_COMPARE_STRING.length();

            String currentTimeLineToComp = timeElementList
                    .get(currentTimeElementListIndex).getDescription()
                    .substring(0, timeLineLengthToComp);
            // loop through stns list to find "ACTIVE" stns which are within snd
            // comparison radius
            for (int i = 0; i < stnElementList.size(); i += 1) {
                if (stnElementList.get(i)
                        .getActionState() == NsharpConstants.ActState.ACTIVE) {
                    // check if target station is within minimum distance
                    // (radius) from current station
                    // we have to get this station's lat/lon for distance
                    // computation by getting first available
                    // NsharpSoundingElementStateProperty of this station at
                    // current time line
                    // if distance to current station is smaller than minimum
                    // distance then add it to list
                    for (int k = 0; k < timeElementList.size(); k += 1) {
                        boolean goodTimeLine = false;
                        if (k != currentTimeElementListIndex) {
                            String timeToCopm1 = timeElementList.get(k)
                                    .getDescription()
                                    .substring(0, timeLineLengthToComp);
                            if (currentTimeLineToComp.equals(timeToCopm1)) {
                                goodTimeLine = true;
                            }
                        } else {
                            // currentTimeElementListIndex is sure to be a good
                            // time line to use
                            goodTimeLine = true;
                        }
                        if (goodTimeLine) {
                            for (int j = 0; j < sndElementList.size(); j += 1) {
                                NsharpSoundingElementStateProperty stnTmElm = stnTimeSndTable
                                        .get(i).get(k).get(j);
                                if (stnTmElm != null) {
                                    if ((currentStnElementListIndex == i)
                                            || (currentStnElementListIndex != i
                                                    && currentSndElementListIndex != j)) {
                                        NsharpStationInfo stnInfo = stnTmElm
                                                .getStnInfo();
                                        gc.setDestinationGeographicPoint(
                                                stnInfo.getLongitude(),
                                                stnInfo.getLatitude());
                                        double distance = gc
                                                .getOrthodromicDistance();
                                        // GeodeticCalculator return value in
                                        // meter. Convert it to mile.
                                        if (distance
                                                / 1609.344 <= sndCompRadius) {
                                            CompSndSelectedElem selectedElem = new CompSndSelectedElem(
                                                    i, k, j);
                                            compSndSelectedElemList
                                                    .add(selectedElem);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            compSndSelectedElemList.clear();
        }
        setCurSndProfileProp();
        setCurrentSoundingLayerInfo();
        resetData();
    }

    public void setCompareTmIsOn(boolean compareIsOn) {
        this.compareTmIsOn = compareIsOn;
        // This is the case when sounding data is not available at
        // currentTimeElementListIndex/currentSndElementListIndex/currentStnElementListIndex
        // and user set compTm on
        if (compareIsOn) {
            if (soundingLys == null && currentStnElementListIndex >= 0
                    && currentSndElementListIndex >= 0) {
                // find a new available time line for current snd type and stn
                boolean found = false;
                for (int i = 0; i < timeElementList.size(); i += 1) {
                    if (timeElementList.get(i)
                            .getActionState() == NsharpConstants.ActState.ACTIVE
                            && stnTimeSndTable.get(currentStnElementListIndex)
                                    .get(i)
                                    .get(currentSndElementListIndex) != null) {
                        found = true;
                        currentTimeElementListIndex = i;
                    }
                    if (found) {
                        break;
                    }
                }
                if (!found) {
                    return;
                }
            }
        }
        setCurSndProfileProp();
        setCurrentSoundingLayerInfo();
        resetData();
    }

    public void setEditGraphOn(boolean editGraphOn) {
        this.editGraphOn = editGraphOn;
    }

    public boolean isEditGraphOn() {
        return editGraphOn;
    }

    public int getCurrentGraphMode() {
        return currentGraphMode;
    }

    public void setCurrentGraphMode(int currentGraphMode) {
        this.currentGraphMode = currentGraphMode;
        if (!paneConfigurationName.equals(NsharpConstants.PANE_LITE_D2D_CFG_STR)
                && !paneConfigurationName
                        .equals(NsharpConstants.PANE_OPC_CFG_STR)) {
            if (skewtPaneRsc != null) {
                skewtPaneRsc.setCurrentGraphMode(currentGraphMode);
            }
            refreshPane();
        } else {
            NsharpEditor editor = NsharpEditor.getActiveNsharpEditor();
            if (editor == null) {
                return;
            }
            editor.restartEditor(paneConfigurationName);
        }
    }

    public int getCurrentParcel() {
        return currentParcel;
    }

    public NsharpStationInfo getPickedStnInfo() {
        return pickedStnInfo;
    }

    public String getSoundingType() {
        return soundingType;
    }

    public void setSoundingType(String soundingType) {
        this.soundingType = soundingType;
    }

    public void setCurrentParcel(int currentParcel) {
        this.currentParcel = currentParcel;
        if (currentParcel == NsharpLibSndglib.PARCELTYPE_USER_DEFINED){
            currentParcelLayerPressure = weatherDataStore
                    .getUserDefdParcelMb();
            // should recompute user defined parcel related weather data,
            // but instead recompute all stored weather data for now
            weatherDataStore.computeWeatherParameters(soundingLys,
                    paneConfigurationName);
        } else {
            currentParcelLayerPressure = NsharpWeatherDataStore.parcelToLayerPressMap
                    .get(currentParcel);
        }
        if (skewtPaneRsc != null) {
            skewtPaneRsc.createRscParcelTraceShapes(currentParcel,
                    currentParcelLayerPressure);
            skewtPaneRsc.createRscParcelRtTraceShapesList(currentParcel,
                    currentParcelLayerPressure);
            skewtPaneRsc.createLCLEtcLinesShape();
        }
    }

    public void setHodoStmCenter(Coordinate hodoHouseC) {
        if (hodoPaneRsc == null) {
            return;
        }

        Coordinate c = hodoPaneRsc.getHodoBackground().getWorld()
                .unMap(hodoHouseC.x, hodoHouseC.y);
        c = WxMath.speedDir((float) c.x, (float) c.y);
        float smWindDir = (float) c.y;
        float smWindSpd = (float) c.x;
        weatherDataStore.setStorm(smWindSpd, smWindDir);
        if (insetPaneRsc != null) {
            NsharpWGraphics WGc = insetPaneRsc.getPsblWatchTypeBackground()
                    .getWorld();
            insetPaneRsc.createBkgPsblWatchShape(WGc);

            // Sr wind vs Height graph shape need to recreate
            WGc = insetPaneRsc.getSrWindsBackground().getWorld();
            insetPaneRsc.createRscSrWindShape(WGc);
        }
        if (skewtPaneRsc != null) {
            skewtPaneRsc.createEffectiveLayerLinesShape();
        }
    }

    // This function is called only when interpolation "on/off" is changed by
    // user
    public void resetInfoOnInterpolate(boolean interpolateIsOn) {
        // We dont want to assume previous interpolation on/off state. So, reset
        // soundingLys any how.
        this.interpolateIsOn = interpolateIsOn;
        NsharpSoundingElementStateProperty elem = getCurSoundingElementStateProperty();
        if (elem != null) {
            if (interpolateIsOn) {
                soundingLys = performInterpolation(soundingLys);
            } else {
                soundingLys = elem.getSndLyLst();
            }
            weatherDataStore.computeWeatherParameters(soundingLys,
                    paneConfigurationName);
            if (skewtPaneRsc != null) {
                skewtPaneRsc.resetData(soundingLys);
            }
            if (hodoPaneRsc != null) {
                hodoPaneRsc.resetData(soundingLys);
            }
            if (witoPaneRsc != null) {
                witoPaneRsc.resetData(soundingLys);
            }
            if (dataPaneRsc != null) {
                dataPaneRsc.resetData(soundingLys);
            }
            if (insetPaneRsc != null) {
                insetPaneRsc.resetData(soundingLys);
            }

            // re-create shape
            if (skewtPaneRsc != null) {
                skewtPaneRsc.handleResize();
            }
            if (hodoPaneRsc != null) {
                hodoPaneRsc.createRscHodoWindShapeAll();
            }
            if (insetPaneRsc != null) {
                insetPaneRsc.createInsetWireFrameShapes();
            }
            if (witoPaneRsc != null) {
                witoPaneRsc.createRscWireFrameShapes();
            }

        }

    }

    public void handleNsharpEditorPartEvent(
            NsharpPartListener.PartEvent pStatus) {
        switch (pStatus) {
        case partActivated:
            if (editorPartStatus != NsharpPartListener.PartEvent.partDeactivated) {
                resetData();
            }

            break;
        default:
            break;
        }
        editorPartStatus = pStatus;
    }

    public void resetRsc() {
        restoreAllSoundingData();
        NsharpSoundingElementStateProperty elem = getCurSoundingElementStateProperty();
        if (elem != null) {
            this.soundingLys = elem.getSndLyLst();
            // Set default parcel trace data
            currentParcel = NsharpLibSndglib.PARCELTYPE_MOST_UNSTABLE;
            currentParcelLayerPressure = NsharpLibSndglib.MU_LAYER_PRESS;
            setSoundingInfo(this.soundingLys);
            currentTextChapter = 1;
            overlayIsOn = false;
            interpolateIsOn = false;
            compareStnIsOn = false;
            compareSndIsOn = false;
            compareTmIsOn = false;
            editGraphOn = false;
            if (skewtPaneRsc != null) {
                skewtPaneRsc.setCurrentSkewTEditMode(
                        NsharpConstants.SKEWT_EDIT_MODE_EDITPOINT);
            }
            elem.setGoodData(checkDataIntegrity(soundingLys));
            resetData();
        }

    }

    public synchronized void resetData() {
        if (skewtPaneRsc != null) {
            skewtPaneRsc.resetData(soundingLys);
        }
        if (hodoPaneRsc != null) {
            hodoPaneRsc.resetData(soundingLys);
        }
        if (insetPaneRsc != null) {
            insetPaneRsc.resetData(soundingLys);
        }
        if (dataPaneRsc != null) {
            dataPaneRsc.resetData(soundingLys);
        }
        if (witoPaneRsc != null) {
            witoPaneRsc.resetData(soundingLys);
        }

        NsharpShowTextDialog textarea = NsharpShowTextDialog.getAccess();
        if (textarea != null) {
            textarea.refreshTextData();
        }
        // if soundingLys is null, then we stop here, after reset data.
        if (soundingLys == null) {
            return;
        }
        // update active sounding layer and picked stn info
        // re-populate snd data to nsharp native code lib for later calculating
        // dont populate sounding data if data is bad
        if (getCurSoundingElementStateProperty() != null) {
            goodData = getCurSoundingElementStateProperty().isGoodData();
        }
        if (soundingLys != null && (goodData)) {

            weatherDataStore.computeWeatherParameters(soundingLys,
                    paneConfigurationName);
        }

        if (skewtPaneRsc != null) {
            skewtPaneRsc.createRscWireFrameShapes();
        }
        if (hodoPaneRsc != null) {
            hodoPaneRsc.createRscHodoWindShapeAll();
        }
        if (insetPaneRsc != null) {
            insetPaneRsc.createInsetWireFrameShapes();
        }
        if (witoPaneRsc != null) {
            witoPaneRsc.createAllWireFrameShapes();
        }
    }

    private void restoreAllSoundingData() {
        for (List<List<NsharpSoundingElementStateProperty>> tlListList : stnTimeSndTable) {
            // add a new element for the new sndType to each existing sndlist of
            // each existing time of each existing stnId
            for (List<NsharpSoundingElementStateProperty> sndtyList : tlListList) {
                for (NsharpSoundingElementStateProperty elem : sndtyList) {
                    if (elem != null) {
                        elem.restoreSndLyLstFromBackup();
                    }
                }
            }
        }
    }
    
    private void addElementToTableAndLists(NsharpElementDescription desc, NsharpStationInfo stnInfo,
            List<NcSoundingLayer> sndLyLst, boolean displayData) {
        boolean goodData = checkDataIntegrity(sndLyLst);
        NsharpSoundingElementStateProperty newSndPropElem = new NsharpSoundingElementStateProperty(
                desc.getDescription(), stnInfo, sndLyLst,
                goodData);
        NsharpTimeOperationElement tmElem = desc.getTimeElement();
        NsharpOperationElement stnElem = desc.getStationElement();
        NsharpOperationElement sndTypeElem = desc.getTypeElement();
        int tmIndex = timeElementList.indexOf(tmElem);
        int stnIndex = stnElementList.indexOf(stnElem);
        int sndTypeIndex = sndElementList.indexOf(sndTypeElem);
        if (sndTypeIndex < 0) {
            sndElementList.add(sndTypeElem);
            Collections.sort(sndElementList, Comparator.reverseOrder());
            sndTypeIndex = sndElementList.indexOf(sndTypeElem);
            for (List<List<NsharpSoundingElementStateProperty>> tlListList : stnTimeSndTable) {
                for (List<NsharpSoundingElementStateProperty> sndtyList : tlListList) {
                    sndtyList.add(sndTypeIndex, null);
                }
            }
        }
        if (tmIndex < 0) {
            timeElementList.add(tmElem);
            Collections.sort(timeElementList, Comparator.reverseOrder());
            tmIndex = timeElementList.indexOf(tmElem);
            for (List<List<NsharpSoundingElementStateProperty>> tlListList : stnTimeSndTable) {
                List<NsharpSoundingElementStateProperty> newSndList = new ArrayList<>();
                newSndList.addAll(
                        Collections.nCopies(sndElementList.size(), null));
                tlListList.add(tmIndex, newSndList);
            }
        }
        if (stnIndex < 0) {
            stnElementList.add(stnElem);
            Collections.sort(stnElementList, Comparator.reverseOrder());
            stnIndex = stnElementList.indexOf(stnElem);
            List<List<NsharpSoundingElementStateProperty>> listListForNewStn = new ArrayList<>();
            for (int i = 0; i < timeElementList.size(); i += 1) {
                List<NsharpSoundingElementStateProperty> sndListForTm = new ArrayList<>();
                sndListForTm.addAll(
                        Collections.nCopies(sndElementList.size(), null));
                listListForNewStn.add(sndListForTm);
            }
            stnTimeSndTable.add(stnIndex, listListForNewStn);
        }
        NsharpSoundingElementStateProperty tmpNewSndPropElem = stnTimeSndTable
                .get(stnIndex).get(tmIndex).get(sndTypeIndex);
        if (tmpNewSndPropElem != null) {
            if (sndLyLst.size() > tmpNewSndPropElem.getSndLyLst().size()) {
                tmpNewSndPropElem.getSndLyLst().clear();
                tmpNewSndPropElem.getSndLyLst().addAll(sndLyLst);
                tmpNewSndPropElem.setGoodData(checkDataIntegrity(sndLyLst));
                setCurrentSoundingLayerInfo();
            } else {
                return;
            }
        }
        stnTimeSndTable.get(stnIndex).get(tmIndex).set(sndTypeIndex,
                newSndPropElem);
        if(displayData){
            currentTimeElementListIndex = tmIndex;
            currentStnElementListIndex = stnIndex;
            currentSndElementListIndex = sndTypeIndex;
            setCurSndProfileProp();
        }
    }

    private void setCurSndProfileProp() {
        if(curSndProfileProp != null){
            preSndProfileProp = curSndProfileProp;
        }
        if (currentTimeElementListIndex < 0
                || currentTimeElementListIndex >= timeElementList.size()
                || currentStnElementListIndex < 0
                || currentStnElementListIndex >= stnElementList.size()
                || currentSndElementListIndex < 0
                || currentSndElementListIndex >= sndElementList.size()) {
            curSndProfileProp = null;
        } else {
            curSndProfileProp = stnTimeSndTable.get(currentStnElementListIndex)
                    .get(currentTimeElementListIndex)
                    .get(currentSndElementListIndex);
        }
    }

    private void cleanUpStnTimeSndTable(int stni, int tmi, int sndi) {
        boolean found = false;
        // find if this station is no longer in use
        List<List<NsharpSoundingElementStateProperty>> tlListList = stnTimeSndTable
                .get(stni);
        for (List<NsharpSoundingElementStateProperty> sndtyList : tlListList) {
            for (NsharpSoundingElementStateProperty elem : sndtyList) {
                if (elem != null) {
                    found = true;
                    break;
                }
            }
            if (found) {
                break;
            }
        }
        if (!found) {
            // This stn is no longer in use...delete it from stnTimeSndTable and
            // stnElementList
            stnElementList.remove(stni);
            tlListList = stnTimeSndTable.remove(stni);
            tlListList.clear();
        }
        // find if this time line is no longer in use
        found = false;
        for (List<List<NsharpSoundingElementStateProperty>> tmListListForStn : stnTimeSndTable) {
            List<NsharpSoundingElementStateProperty> sndtyListForTm = tmListListForStn
                    .get(tmi);
            for (NsharpSoundingElementStateProperty elem : sndtyListForTm) {
                if (elem != null) {
                    found = true;
                    break;
                }
            }
            if (found) {
                break;
            }
        }
        if (!found) {
            // This time line is no longer in use...delete it from
            // stnTimeSndTable and timeElementList
            timeElementList.remove(tmi);
            for (List<List<NsharpSoundingElementStateProperty>> tmListListForStn : stnTimeSndTable) {
                List<NsharpSoundingElementStateProperty> sndtyListForTm = tmListListForStn
                        .remove(tmi);
                sndtyListForTm.clear();
            }
        }
        // find if this sounding type is no longer in use
        found = false;
        for (List<List<NsharpSoundingElementStateProperty>> tmListListForStn : stnTimeSndTable) {
            for (List<NsharpSoundingElementStateProperty> sndtyListForTm : tmListListForStn) {
                NsharpSoundingElementStateProperty elem = sndtyListForTm
                        .get(sndi);
                if (elem != null) {
                    found = true;
                    break;
                }
            }
            if (found) {
                break;
            }
        }
        if (!found) {
            // This sounding type is no longer in use...delete it from
            // stnTimeSndTable and sndElementList
            sndElementList.remove(sndi);
            for (List<List<NsharpSoundingElementStateProperty>> tmListListForStn : stnTimeSndTable) {
                for (List<NsharpSoundingElementStateProperty> sndtyListForTm : tmListListForStn) {
                    sndtyListForTm.remove(sndi);
                }
            }
        }

    }

    /**
     * Delete method for d2d. This is necessary because d2d is not aware of the
     * element descriptions that are used in the other delete method, so this
     * can determine element descriptions based off the information available in
     * d2d.
     * 
     * @param deletingDisplayInfo
     *            the display infos of the stations to delete
     * @param soundingType
     *            the sounding type of the stations to delete.
     * @return true if the current sounding was deleted.
     */
    public boolean deleteRsc(List<String> deletingDisplayInfo,
            String soundingType) {
        List<String> deletingDataTimeList = new ArrayList<>(
                deletingDisplayInfo.size());
        for (List<List<NsharpSoundingElementStateProperty>> tlListList : stnTimeSndTable) {
            for (List<NsharpSoundingElementStateProperty> sndtyList : tlListList) {
                for (NsharpSoundingElementStateProperty elem : sndtyList) {
                    NsharpStationInfo stationInfo = elem.getStnInfo();
                    if (soundingType.equals(stationInfo.getSndType())) {
                        String displayInfo = stationInfo.getStnDisplayInfo();
                        if (deletingDisplayInfo.contains(displayInfo)) {
                            deletingDataTimeList
                                    .add(elem.getElementDescription());
                        }
                    }
                }
            }
        }
        if (!deletingDataTimeList.isEmpty()) {
            return deleteRsc(deletingDataTimeList);
        } else {
            return false;
        }
    }

    public boolean deleteRsc(List<String> deletingDataTimeList) {
        boolean curSndDeleted = false;
        for (String dataTmLine : deletingDataTimeList) {
            if (curSndProfileProp != null && curSndProfileProp
                    .getElementDescription().equals(dataTmLine)) {
                curSndDeleted = true;
            }
            // find deleting element from stnTimeSndTable and null it
            boolean setdone = false;
            for (List<List<NsharpSoundingElementStateProperty>> tlListList : stnTimeSndTable) {
                for (List<NsharpSoundingElementStateProperty> sndtyList : tlListList) {
                    for (NsharpSoundingElementStateProperty elem : sndtyList) {
                        if (elem != null && dataTmLine
                                .equals(elem.getElementDescription())) {
                            int sndi = sndtyList.indexOf(elem);
                            int tmi = tlListList.indexOf(sndtyList);
                            int stni = stnTimeSndTable.indexOf(tlListList);
                            sndtyList.set(sndtyList.indexOf(elem), null);
                            setdone = true;
                            // clean up stnTimeSndTable if a stn/timeline/snd is
                            // no longer in use
                            cleanUpStnTimeSndTable(stni, tmi, sndi);
                            break;
                        }
                    }
                    if (setdone) {
                        break;
                    }
                }
                if (setdone) {
                    break;
                }
            }
        }

        if (curSndDeleted || soundingLys == null) {
            // this is the case that we are deleting current snd, so, a new
            // current snd should be selected
            curSndProfileProp = null;
            // find CurrentElementIndexes After Delete current snding
            boolean found = false;
            int stni = 0;
            for (List<List<NsharpSoundingElementStateProperty>> tlListList : stnTimeSndTable) {
                int timei = 0;
                for (List<NsharpSoundingElementStateProperty> sndtyList : tlListList) {
                    int sndi = 0;
                    for (NsharpSoundingElementStateProperty elem : sndtyList) {
                        if (elem != null
                                && stnElementList.get(stni)
                                        .getActionState() == NsharpConstants.ActState.ACTIVE
                                && timeElementList.get(timei)
                                        .getActionState() == NsharpConstants.ActState.ACTIVE
                                && sndElementList.get(sndi)
                                        .getActionState() == NsharpConstants.ActState.ACTIVE) {
                            currentStnElementListIndex = stni;
                            currentSndElementListIndex = sndi;
                            currentTimeElementListIndex = timei;
                            found = true;
                            break;
                        }
                        sndi += 1;
                    }
                    if (found) {
                        break;
                    }
                    timei += 1;
                }
                if (found) {
                    break;
                }
                stni += 1;
            }
            if (!found) {
                currentStnElementListIndex = -1;
                currentSndElementListIndex = -1;
                currentTimeElementListIndex = -1;
            }
            setCurSndProfileProp();
        } else {
            // currentStnElementListIndex, currentSndElementListIndex and
            // currentTimeElementListIndex may not point to right element
            // after some elements are deleted.
            currentStnElementListIndex = -1;
            currentTimeElementListIndex = -1;
            currentSndElementListIndex = -1;
            if (curSndProfileProp != null) {
                boolean found = false;
                for (List<List<NsharpSoundingElementStateProperty>> tmListListForStn : stnTimeSndTable) {
                    for (List<NsharpSoundingElementStateProperty> sndtyListForTm : tmListListForStn) {
                        for (NsharpSoundingElementStateProperty elem : sndtyListForTm) {
                            if (elem != null && curSndProfileProp
                                    .getElementDescription()
                                    .equals(elem.getElementDescription())) {
                                currentSndElementListIndex = sndtyListForTm
                                        .indexOf(elem);
                                currentTimeElementListIndex = tmListListForStn
                                        .indexOf(sndtyListForTm);
                                currentStnElementListIndex = stnTimeSndTable
                                        .indexOf(tmListListForStn);
                                found = true;
                                break;
                            }
                        }
                        if (found) {
                            break;
                        }
                    }
                    if (found) {
                        break;
                    }
                }
            }
        }
        setCurrentSoundingLayerInfo();
        resetData();
        return curSndDeleted;
    }

    public void deleteRscAll() {
        NsharpMapResource nsharpMapResource = NsharpMapResource
                .getOrCreateNsharpMapResource();
        nsharpMapResource.setPoints(null);
        if (soundingLys != null) {
            soundingLys.clear();
            soundingLys = null;
        }
        if (previousSoundingLys != null) {
            previousSoundingLys.clear();
            previousSoundingLys = null;
        }
        if (stnTimeSndTable != null) {
            for (List<List<NsharpSoundingElementStateProperty>> stnListList : stnTimeSndTable) {
                for (List<NsharpSoundingElementStateProperty> timeList : stnListList) {
                    timeList.clear();
                }
                stnListList.clear();
            }
            stnTimeSndTable.clear();
        }
        if (timeElementList != null) {
            timeElementList.clear();
        }
        if (stnElementList != null) {
            stnElementList.clear();
        }
        if (sndElementList != null) {
            sndElementList.clear();
        }
        if (compSndSelectedElemList != null) {
            compSndSelectedElemList.clear();
        }
        curSndProfileProp = null;
        preSndProfileProp = null;
        currentTextChapter = 1;
        currentInsetPage = 1;
        currentParcel = NsharpLibSndglib.PARCELTYPE_MOST_UNSTABLE;
        currentParcelLayerPressure = NsharpLibSndglib.MU_LAYER_PRESS;
        currentTimeElementListIndex = -1;
        currentStnElementListIndex = -1;
        currentSndElementListIndex = -1;
        resetData();
    }

    private NsharpSoundingElementStateProperty getCurSoundingElementStateProperty() {
        if (currentTimeElementListIndex >= 0 && currentStnElementListIndex >= 0
                && currentSndElementListIndex >= 0
                && stnTimeSndTable.get(currentStnElementListIndex)
                        .get(currentTimeElementListIndex)
                        .get(currentSndElementListIndex) != null) {
            return stnTimeSndTable.get(currentStnElementListIndex)
                    .get(currentTimeElementListIndex)
                    .get(currentSndElementListIndex);
        }
        return null;
    }

    private void setCurrentSoundingLayerInfo() {
        NsharpSoundingElementStateProperty elem = getCurSoundingElementStateProperty();
        if (elem != null) {
            pickedStnInfoStr = elem.getElementDescription();
            pickedStnInfo = elem.getStnInfo();
            setSoundingInfo(elem.getSndLyLst());
        } else {
            if(soundingLys != null){
                previousSoundingLys = soundingLys;
            }
            soundingLys = null;
        }

    }

    private boolean checkDataIntegrity(List<NcSoundingLayer> sndLayers) {
        boolean gooddata = false;
        int numberOfTemp = 0;
        int numberOfGoodDewPt = 0;
        for (NcSoundingLayer layer : sndLayers) {
            if (layer.getTemperature() > -999) {
                numberOfTemp += 1;
            }
            if (layer.getDewpoint() > -999) {
                numberOfGoodDewPt += 1;
            }
        }
        if (numberOfGoodDewPt >= 2 && numberOfTemp >= 2) {
            gooddata = true;
        }
        return gooddata;
    }

    // This api peforms real load data function
    private void addRsc(boolean displayNewData,
            Map<String, List<NcSoundingLayer>> soundMap,
            NsharpStationInfo stnInfo) {

        if (stnInfo.getStnId() != null
                && stnInfo.getStnId().indexOf(" ") >= 0) {
            // take care stnId with SPACE case.
            String stnId = stnInfo.getStnId();
            String newStnId = stnId.replace(" ", "_");
            stnInfo.setStnId(newStnId);
            String dspInfo = stnInfo.getStnDisplayInfo();
            stnInfo.setStnDisplayInfo(dspInfo.replace(stnId, newStnId));
            Set<String> keyset = new HashSet<>(soundMap.keySet());
            for (String key : keyset) {
                List<NcSoundingLayer> sndLy = soundMap.remove(key);
                String newkey = key.replace(stnId, newStnId);
                soundMap.put(newkey, sndLy);
            }
        }
        
        Map<NsharpElementDescription, List<NcSoundingLayer>> newSoundMap = new HashMap<>();
        for(Entry<String, List<NcSoundingLayer>> entry : soundMap.entrySet()){
            if ("N/A".equals(entry.getKey())) {
                continue;
            }
            newSoundMap.put(NsharpElementDescription.parse(entry.getKey()), entry.getValue());
        }
        addRsc(stnInfo, newSoundMap, displayNewData);
    }
    
    // This api peforms real load data function
    public void addRsc(NsharpStationInfo stnInfo, 
            Map<NsharpElementDescription, List<NcSoundingLayer>> soundMap, boolean displayNewData) {
        if (soundMap.size() <= 0
                || (skewtPaneRsc == null && hodoPaneRsc == null)) {
            return;
        }
        if (timeElementList.isEmpty() || stnElementList.isEmpty()
                || currentSndElementListIndex < 0 || sndElementList.isEmpty()
                || currentTimeElementListIndex < 0
                || currentStnElementListIndex < 0) {
            // if no data was loaded since, then display this data any way
            displayNewData = true;
        }
        // add data in order.
        Map<NsharpElementDescription, List<NcSoundingLayer>> sortedSoundMap = new TreeMap<>();
        sortedSoundMap.putAll(soundMap);
        for (Entry<NsharpElementDescription, List<NcSoundingLayer>> entry : sortedSoundMap
                .entrySet()) {
            addElementToTableAndLists(entry.getKey(), stnInfo, entry.getValue(),
                    displayNewData);
        }
        setCurrentSoundingLayerInfo();
        resetData();

        // set data time to descriptor this is necessary for looping
        // starting 13.2.1, this line is changed by Raytheon
        if ((skewtPaneRsc != null) && (skewtPaneRsc.getDescriptor()
                .getFramesInfo().getFrameCount() == 0) && !getTimeMatcher) {
            // Chin Note: we just have to do this once and set dataTimes size
            // bigger than 1.
            // Nsharp handles changing frame itself. It just need system to send
            // change frame notice.
            // That is happened at NsharpSkewTDescriptor.checkDrawTime().
            DataTime[] dataTimes = new DataTime[2/* stnTimeTable.size() */];
            Date now = new Date();
            for (int k = 0; k < 2; k += 1) {
                dataTimes[k] = new DataTime(now, k);
            }
            // no need to get a descriptor from a renderableDispaly since we
            // have a descriptor
            skewtPaneRsc.getDescriptor()
                    .setFramesInfo(new FramesInfo(dataTimes));
            getTimeMatcher = true;
        }

        NsharpShowTextDialog textarea = NsharpShowTextDialog.getAccess();
        if (textarea != null) {
            textarea.refreshTextData();
        }
        NsharpPaletteWindow win = NsharpPaletteWindow.getInstance();
        if (win != null) {
            currentGraphMode = win.getCurrentGraphMode();
        }

        if (lastUserSelectedTimeLineIndex == 0 && getCurrentIndex() != 0
                && getSoundingLys() != null) {
            // stay on the top time line if user selected the top one
            setCurrentIndex(0);
        } else if (lastUserSelectedTimeLineIndex > 0
                && lastUserSelectedTimeLine != null) {
            int index = timeElementList.indexOf(lastUserSelectedTimeLine);
            if (index > 0) {
                // stay on the same time line if user didn't select the top one.
                setCurrentIndex(index);
            } else {
                if (timeElementList.size() > 1) {
                    // stay on the bottom time line
                    setCurrentIndex(timeElementList.size() - 1);
                } else {
                    refreshPane();
                }
            }
        } else {
            refreshPane();
        }

    }

    // NCP loads data from DB always uses this route.
    public void addRsc(Map<String, List<NcSoundingLayer>> soundMap,
            NsharpStationInfo stnInfo) {
        // by default, display new data
        this.addRsc(true, soundMap, stnInfo);
        return;
    }

    // NCP loads archive data uses this route.
    public void addArchiveRsc(Map<String, List<NcSoundingLayer>> soundMap,
            NsharpStationInfo stnInfo) {
        // by default, display new data
        this.addRsc(true, soundMap, stnInfo);
        return;
    }

    public String getPickedStnInfoStr() {
        return pickedStnInfoStr;
    }

    private void handleUserPickNewStationId(int index) {
        currentStnElementListIndex = index;
        if (!compareTmIsOn && compareSndIsOn) {
            if (currentTimeElementListIndex >= 0
                    && currentSndElementListIndex >= 0
                    && stnTimeSndTable.get(currentStnElementListIndex)
                            .get(currentTimeElementListIndex)
                            .get(currentSndElementListIndex) != null) {
                setCompareSndIsOn(true);
            }
        }
        setCurSndProfileProp();
        setCurrentSoundingLayerInfo();
        resetData();
        refreshPane();
    }

    public void handleUserClickOnStationId(Coordinate c, boolean shiftDown) {
        int index = timeStnPaneRsc.getUserClickedStationIdIndex(c);
        if (index < this.stnElementList.size()) {
            NsharpConstants.ActState actState = stnElementList.get(index)
                    .getActionState();
            if (!shiftDown) {
                if (actState == NsharpConstants.ActState.ACTIVE) {
                    handleUserPickNewStationId(index);
                }
            } else {

                switch (actState) {

                case INACTIVE:
                    stnElementList.get(index)
                            .setActionState(NsharpConstants.ActState.ACTIVE);
                    break;
                case ACTIVE:
                    // do not allow deactivate current stn
                    if (index == currentStnElementListIndex) {
                        return;
                    }
                    stnElementList.get(index)
                            .setActionState(NsharpConstants.ActState.INACTIVE);
                    break;
                default:
                    return;
                }
                if (skewtPaneRsc != null) {
                    skewtPaneRsc.createRscWireFrameShapes();
                }
                if (hodoPaneRsc != null) {
                    hodoPaneRsc.createRscHodoWindShapeAll();
                }
            }
        }
    }

    private void handleUserPickNewTimeLine(int index) {
        lastUserSelectedTimeLineIndex = index;
        lastUserSelectedTimeLine = timeElementList.get(index);
        currentTimeElementListIndex = index;
        if (!compareStnIsOn && compareSndIsOn) {
            if (currentStnElementListIndex >= 0
                    && currentSndElementListIndex >= 0
                    && stnTimeSndTable.get(currentStnElementListIndex)
                            .get(currentTimeElementListIndex)
                            .get(currentSndElementListIndex) != null) {
                setCompareSndIsOn(true);
            }
        }
        setCurSndProfileProp();
        setCurrentSoundingLayerInfo();
        resetData();
        refreshPane();
    }

    public void handleUserClickOnTimeLine(Coordinate c, boolean shiftDown) {
        int index = timeStnPaneRsc.getUserClickedTimeIndex(c);
        if (index < timeElementList.size() && index >= 0) {
            NsharpConstants.ActState actState = timeElementList.get(index)
                    .getActionState();
            if (!shiftDown) {
                if (actState == NsharpConstants.ActState.ACTIVE) {
                    handleUserPickNewTimeLine(index);
                }
            } else {
                switch (actState) {
                case INACTIVE:
                    timeElementList.get(index)
                            .setActionState(NsharpConstants.ActState.ACTIVE);
                    break;
                case ACTIVE:
                    if (index == currentTimeElementListIndex) {
                        // dont allow to deactive current time line
                        return;
                    }
                    timeElementList.get(index)
                            .setActionState(NsharpConstants.ActState.INACTIVE);
                    break;

                default:
                    return;

                }
                if (skewtPaneRsc != null) {
                    skewtPaneRsc.createRscWireFrameShapes();
                }
                if (hodoPaneRsc != null) {
                    hodoPaneRsc.createRscHodoWindShapeAll();
                }
            }
        }
    }

    private void handleUserPickNewSndLine(int index) {
        currentSndElementListIndex = index;
        if (!compareTmIsOn && !compareStnIsOn && compareSndIsOn) {
            // see if the new currentSndElementListIndex is used in one of the
            // element
            for (CompSndSelectedElem elem : compSndSelectedElemList) {
                if (elem.getSndIndex() == currentSndElementListIndex) {
                    currentTimeElementListIndex = elem.getTimeIndex();
                    currentStnElementListIndex = elem.getStnIndex();
                    break;
                }
            }
        }

        setCurSndProfileProp();
        setCurrentSoundingLayerInfo();
        resetData();
        refreshPane();
    }

    public void handleUserClickOnSndLine(Coordinate c, boolean shiftDown) {

        // first to find if it is for change to next/prev page
        int index = timeStnPaneRsc.getUserClickedSndIndex(c);
        if (index < sndElementList.size() && index >= 0) {
            NsharpConstants.ActState actState = sndElementList.get(index)
                    .getActionState();
            if (!shiftDown) {
                if (actState == NsharpConstants.ActState.ACTIVE) {
                    handleUserPickNewSndLine(index);
                }
            } else {
                switch (actState) {
                case INACTIVE:
                    sndElementList.get(index)
                            .setActionState(NsharpConstants.ActState.ACTIVE);
                    break;
                case ACTIVE:
                    if (index == currentSndElementListIndex) {
                        // dont allow to deactive current time line
                        return;
                    }
                    sndElementList.get(index)
                            .setActionState(NsharpConstants.ActState.INACTIVE);
                    break;

                default:
                    return;

                }
                if (skewtPaneRsc != null) {
                    skewtPaneRsc.createRscWireFrameShapes();
                }
                if (hodoPaneRsc != null) {
                    hodoPaneRsc.createRscHodoWindShapeAll();
                }
            }
        }
    }

    private void moveTimeLineIndexBackward() {
        int counter = 0;
        while (true) {
            currentTimeElementListIndex += 1;
            currentTimeElementListIndex = currentTimeElementListIndex
                    % this.timeElementList.size();
            counter += 1;
            if (counter > timeElementList.size()) {
                break;
            }
            if (timeElementList.get(currentTimeElementListIndex)
                    .getActionState() == NsharpConstants.ActState.ACTIVE
                    && stnTimeSndTable.get(currentStnElementListIndex)
                            .get(currentTimeElementListIndex)
                            .get(currentSndElementListIndex) != null) {
                break;
            }

        }
    }

    private void moveTimeLineIndexForward() {
        int counter = 0;
        while (true) {
            // doing so to make sure we wont get a negative number
            currentTimeElementListIndex = currentTimeElementListIndex
                    + this.timeElementList.size();
            currentTimeElementListIndex -= 1;
            currentTimeElementListIndex = currentTimeElementListIndex
                    % this.timeElementList.size();
            counter += 1;
            if (counter > timeElementList.size()) {
                break;
            }
            if (timeElementList.get(currentTimeElementListIndex)
                    .getActionState() == NsharpConstants.ActState.ACTIVE
                    && stnTimeSndTable.get(currentStnElementListIndex)
                            .get(currentTimeElementListIndex)
                            .get(currentSndElementListIndex) != null) {
                break;
            }
        }
    }

    private void moveTimeLineIndexCycle() {
        int previousTimeLineStateListIndex = currentTimeElementListIndex;
        // Note: direction should only be NEXT or PREVIOUS
        int counter = 0;
        while (true) {
            counter += 1;
            if (counter > timeElementList.size()) {
                currentTimeElementListIndex = previousTimeLineStateListIndex;
                break;
            }
            if (currentOpDirection == IFrameCoordinator.FrameChangeOperation.NEXT) {
                currentTimeElementListIndex -= 1;
                if (currentTimeElementListIndex <= 0) {
                    // the end of forward direction, change direction to
                    // backward
                    currentOpDirection = IFrameCoordinator.FrameChangeOperation.PREVIOUS;
                    currentTimeElementListIndex = 0;
                }

            } else {
                // direction is FrameChangeOperation.PREVIOUS
                currentTimeElementListIndex += 1;
                if (currentTimeElementListIndex >= timeElementList.size() - 1) {
                    // the end of backward direction, change direction to
                    // forward
                    currentOpDirection = IFrameCoordinator.FrameChangeOperation.NEXT;
                    currentTimeElementListIndex = timeElementList.size() - 1;
                }
            }
            if (timeElementList.get(currentTimeElementListIndex)
                    .getActionState() == NsharpConstants.ActState.ACTIVE
                    && stnTimeSndTable.get(currentStnElementListIndex)
                            .get(currentTimeElementListIndex)
                            .get(currentSndElementListIndex) != null) {
                break;
            }
        }
    }

    /*
     * Note: looping only apply to curAggregateTimeLineList NOT stationIdList
     */
    public void setLoopingDataTimeLine(LoopProperties loopProperties) {
        if (!this.timeElementList.isEmpty()) {
            switch (loopProperties.getMode()) {
            case Forward:
                moveTimeLineIndexForward();
                break;
            case Backward:
                moveTimeLineIndexBackward();
                break;
            case Cycle:
                moveTimeLineIndexCycle();
                break;
            }
            if (compareSndIsOn) {
                handleUserPickNewTimeLine(currentTimeElementListIndex);
                return;
            }
            setCurSndProfileProp();
            setCurrentSoundingLayerInfo();
            resetData();
            refreshPane();
        }

    }

    public enum LoopMode {
        Forward, Backward, Cycle
    };

    private int getElemlistActiveNumber(List<? extends NsharpOperationElement> elemlist) {
        int n = 0;
        for (NsharpOperationElement elem : elemlist) {
            if (elem.getActionState() == NsharpConstants.ActState.ACTIVE) {
                n += 1;
            }
        }
        return n;
    }

    public void setSteppingTimeLine(
            IFrameCoordinator.FrameChangeOperation operation,
            IFrameCoordinator.FrameChangeMode mode) {
        if (!this.timeElementList.isEmpty()
                && getElemlistActiveNumber(timeElementList) > 1) {
            int targetIndex = currentTimeElementListIndex;
            // preset index for LAST and FIRST operation
            switch (operation) {
            case LAST: 
                // the future-est time, at top of time line shown. set to
                // -1, so in while loop, it starts from 0
                targetIndex = -1;
                break;
            case FIRST: 
                // the oldest time, set to dataTimelineList.length, so
                // in while loop, it starts from
                // dataTimelineList.length-1
                targetIndex = timeElementList.size();
                break;
            default:
                break;

            }

            int counter = 0;
            while (true) {
                switch (operation) {
                case LAST:
                    // the future-est time, at top of time line shown
                    targetIndex += 1;
                    break;
                case FIRST:
                    // the oldest time
                    targetIndex -= 1;
                    break;
                case PREVIOUS:
                    targetIndex += 1;
                    targetIndex = targetIndex % this.timeElementList.size();
                    break;
                case NEXT:
                    // so, we wont get a negative number
                    targetIndex = targetIndex + this.timeElementList.size();
                    targetIndex -= 1;
                    targetIndex = targetIndex % this.timeElementList.size();
                    break;
                default:
                    break;
                }
                counter += 1;
                if (counter >= timeElementList.size()) {
                    // looped through whole list already, and index back
                    // to original
                    return;
                }
                    
                if (timeElementList.get(targetIndex)
                        .getActionState() == NsharpConstants.ActState.ACTIVE) {
                    if (compareTmIsOn && currentStnElementListIndex >= 0
                            && currentSndElementListIndex >= 0
                            && stnTimeSndTable.get(currentStnElementListIndex)
                                    .get(targetIndex)
                                    .get(currentSndElementListIndex) == null) {
                        continue;
                    } else if (compareStnIsOn) {
                        boolean found = false;
                        if (currentStnElementListIndex >= 0
                                && currentSndElementListIndex >= 0
                                && stnTimeSndTable
                                        .get(currentStnElementListIndex)
                                        .get(targetIndex)
                                        .get(currentSndElementListIndex) != null) {
                            found = true;
                        } else {
                            // find an active and available stn for this
                            // timeline and set is as current
                            for (int i = 0; i < stnElementList.size(); i += 1) {
                                if (stnElementList.get(i)
                                        .getActionState() == NsharpConstants.ActState.ACTIVE
                                        && stnTimeSndTable.get(i)
                                                .get(targetIndex)
                                                .get(currentSndElementListIndex) != null) {
                                    currentStnElementListIndex = i;
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (!found) {
                            currentStnElementListIndex = -1;
                        }
                        // no matter we find current stn or not
                        // we should get out of here
                        break;
                    } else if (compareSndIsOn) {
                        handleUserPickNewTimeLine(targetIndex);
                        return;
                    } else {
                        break;
                    }
                }
            }
            currentTimeElementListIndex = targetIndex;
            setCurSndProfileProp();
            setCurrentSoundingLayerInfo();
            resetData();
            refreshPane();
        }
    }

    /*
     * Stn index stepping is only controlled by up/down arrow keys, down key =
     * PREVIOUS operation, up key = NEXT operation
     */
    public void setSteppingStnIdList(
            IFrameCoordinator.FrameChangeOperation operation) {
        if (!this.stnElementList.isEmpty()
                && getElemlistActiveNumber(stnElementList) > 1) {

            int counter = 0;
            while (true) {
                switch (operation) {
                case NEXT:
                    currentStnElementListIndex = currentStnElementListIndex
                            + this.stnElementList.size();
                    currentStnElementListIndex -= 1;
                    currentStnElementListIndex = currentStnElementListIndex
                            % this.stnElementList.size();
                    break;
                case PREVIOUS:
                    // doing so to make sure we wont get a negative number
                    currentStnElementListIndex += 1;
                    currentStnElementListIndex = currentStnElementListIndex
                            % this.stnElementList.size();
                    break;
                default:
                    break;

                }
                counter += 1;
                if (counter >= stnElementList.size()){
                     // looped through whole list already, and index back
                     // to original
                    return;
                }
                if (stnElementList.get(currentStnElementListIndex)
                        .getActionState() == NsharpConstants.ActState.ACTIVE) {
                    if (compareStnIsOn && currentTimeElementListIndex >= 0
                            && currentSndElementListIndex >= 0
                            && stnTimeSndTable.get(currentStnElementListIndex)
                                    .get(currentTimeElementListIndex)
                                    .get(currentSndElementListIndex) == null) {
                        continue;
                    } else if (compareTmIsOn) {
                        boolean found = false;
                        if (currentTimeElementListIndex >= 0
                                && currentSndElementListIndex >= 0
                                && stnTimeSndTable
                                        .get(currentStnElementListIndex)
                                        .get(currentTimeElementListIndex)
                                        .get(currentSndElementListIndex) != null) {
                            // use currentTimeLineStateListIndex
                            found = true;
                        } else {
                            // find an active and available timeline for this
                            // stn and set is as current
                            for (int i = 0; i < timeElementList.size(); i += 1) {
                                if (timeElementList.get(i)
                                        .getActionState() == NsharpConstants.ActState.ACTIVE
                                        && stnTimeSndTable
                                                .get(currentStnElementListIndex)
                                                .get(i)
                                                .get(currentSndElementListIndex) != null) {
                                    currentTimeElementListIndex = i;
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (!found) {
                            currentTimeElementListIndex = -1;
                        }
                        // no matter we find current time line for this stn or
                        // not we should get out of here
                        break;
                    } else if (compareSndIsOn) {
                        boolean found = false;
                        if (currentTimeElementListIndex >= 0
                                && currentSndElementListIndex >= 0
                                && stnTimeSndTable
                                        .get(currentStnElementListIndex)
                                        .get(currentTimeElementListIndex)
                                        .get(currentSndElementListIndex) != null) {
                            found = true;
                        } else {
                            // find an active and available snd for this stn and
                            // set is as current
                            for (int i = 0; i < sndElementList.size(); i += 1) {
                                if (sndElementList.get(i)
                                        .getActionState() == NsharpConstants.ActState.ACTIVE
                                        && stnTimeSndTable
                                                .get(currentStnElementListIndex)
                                                .get(currentTimeElementListIndex)
                                                .get(i) != null) {
                                    currentSndElementListIndex = i;
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (!found) {
                            currentSndElementListIndex = -1;
                        }
                        // no matter we find current snd type for this stn or
                        // not we should get out of here
                        break;
                    } else {
                        break;
                    }
                }
            }

            setCurSndProfileProp();
            setCurrentSoundingLayerInfo();
            resetData();
            refreshPane();
        }

    }

    /*
     * Snd Type index stepping is only controlled by shift + up/down arrow keys,
     * shift+down key = PREVIOUS operation, shift+up key = NEXT operation
     */
    public void setSteppingSndTypeList(
            IFrameCoordinator.FrameChangeOperation operation) {
        if (!this.sndElementList.isEmpty()
                && getElemlistActiveNumber(sndElementList) > 1) {

            int counter = 0;
            while (true) {
                switch (operation) {
                case NEXT:
                    currentSndElementListIndex = currentSndElementListIndex
                            + this.sndElementList.size();
                    currentSndElementListIndex -= 1;
                    currentSndElementListIndex = currentSndElementListIndex
                            % this.sndElementList.size();
                    break;
                case PREVIOUS:
                    // doing so to mare sure we wont get a negative number
                    currentSndElementListIndex += 1;
                    currentSndElementListIndex = currentSndElementListIndex
                            % this.sndElementList.size();
                    break;
                default:
                    break;

                }
                counter += 1;
                if (counter >= sndElementList.size()) {
                    // looped through whole list already, and index back
                    // to original
                    return;
                }
                if (sndElementList.get(currentSndElementListIndex)
                        .getActionState() == NsharpConstants.ActState.ACTIVE) {
                    if (compareSndIsOn && currentTimeElementListIndex >= 0
                            && currentStnElementListIndex >= 0
                            && stnTimeSndTable.get(currentStnElementListIndex)
                                    .get(currentTimeElementListIndex)
                                    .get(currentSndElementListIndex) == null) {
                        continue;
                    } else if (compareTmIsOn) {
                        boolean found = false;
                        if (currentTimeElementListIndex >= 0
                                && currentStnElementListIndex >= 0
                                && stnTimeSndTable
                                        .get(currentStnElementListIndex)
                                        .get(currentTimeElementListIndex)
                                        .get(currentSndElementListIndex) != null) {
                            // use currentTimeLineStateListIndex
                            found = true;
                        } else {
                            // find an active and available timeline for this
                            // stn and set is as current
                            for (int i = 0; i < timeElementList.size(); i += 1) {
                                if (timeElementList.get(i)
                                        .getActionState() == NsharpConstants.ActState.ACTIVE
                                        && stnTimeSndTable
                                                .get(currentStnElementListIndex)
                                                .get(i)
                                                .get(currentSndElementListIndex) != null) {
                                    currentTimeElementListIndex = i;
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (!found) {
                            currentTimeElementListIndex = -1;
                        }
                        // no matter we find current time line for this stn or
                        // not
                        // we should get out of here
                        break;
                    } else if (compareStnIsOn) {
                        boolean found = false;
                        // find an active and available stn for this timeline
                        // and set is as current
                        if (currentTimeElementListIndex >= 0
                                && currentStnElementListIndex >= 0
                                && stnTimeSndTable
                                        .get(currentStnElementListIndex)
                                        .get(currentTimeElementListIndex)
                                        .get(currentSndElementListIndex) != null) {
                            found = true;
                        } else {
                            for (int i = 0; i < stnElementList.size(); i += 1) {
                                if (stnElementList.get(i)
                                        .getActionState() == NsharpConstants.ActState.ACTIVE
                                        && stnTimeSndTable.get(i)
                                                .get(currentTimeElementListIndex)
                                                .get(currentSndElementListIndex) != null) {
                                    currentStnElementListIndex = i;
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (!found) {
                            currentStnElementListIndex = -1;
                        }
                        // no matter we find current stn or not
                        // we should get out of here
                        break;
                    } else {
                        break;
                    }
                }
            }

            setCurSndProfileProp();
            setCurrentSoundingLayerInfo();
            resetData();

            refreshPane();
        }

    }

    /*
     * Return the closest point to the input point on Hodo graph
     */
    public Coordinate getClosestHodoPoint(Coordinate inputC) {
        Coordinate closeptC = new Coordinate(0, 0);
        if (hodoPaneRsc == null) {
            return closeptC;
        }
        // picked a impossible big number to start with
        double curSmallestDist = 10_000;
        double distance;
        boolean ptFound = false;
        NcSoundingLayer layer;
        //
        // Note: soundingLys list sorted with highest pressure as first element
        //
        for (int i = 0; i < this.soundingLys.size(); i += 1) {
            layer = this.soundingLys.get(i);
            double curS, curD;
            curS = layer.getWindSpeed();
            curD = layer.getWindDirection();
            closeptC = WxMath.uvComp((float) curS, (float) curD);
            closeptC = hodoPaneRsc.getHodoBackground().getWorld().map(closeptC);
            distance = inputC.distance(closeptC);
            if (distance < curSmallestDist) {
                curSmallestDist = distance;
                hodoEditingSoundingLayerIndex = i;
                ptFound = true;
            }
        }
        if (ptFound) {
            layer = this.soundingLys.get(hodoEditingSoundingLayerIndex);
            closeptC = WxMath.uvComp(layer.getWindSpeed(),
                     layer.getWindDirection());
            closeptC = hodoPaneRsc.getHodoBackground().getWorld().map(closeptC);
        } else {
            closeptC.x = closeptC.y = 0;
        }

        return closeptC;
    }

    public List<NcSoundingLayer> getSoundingLys() {
        return soundingLys;
    }

    public List<NcSoundingLayer> getPreviousSoundingLys() {
        return previousSoundingLys;
    }

    /*
     * This interpolation is to smooth data out with a pressure gap of 25 mb per
     * layer and also keep original lowest and highest layers.
     */
    private List<NcSoundingLayer> performInterpolation(
            List<NcSoundingLayer> rawSndLysLst) {
        NcSoundingLayer newLayer = new NcSoundingLayer();
        List<NcSoundingLayer> mySndLst = new ArrayList<>();
        // add top layer
        try {
            // here a shallowCopy is enough
            newLayer = (NcSoundingLayer) rawSndLysLst.get(0).clone();
            mySndLst.add(newLayer);
        } catch (CloneNotSupportedException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "performInterpolation exception:", e);
        }
        // The first layer has highest pressure get a pressure value below first
        // layer and can be divided by 25 exactly
        int p = (int) (rawSndLysLst.get(0).getPressure() / 25) * 25;
        float interpolatedValue;
        // if p is same as first layer, then this layer is added already, start
        // from 2nd layer
        if (p == (int) rawSndLysLst.get(0).getPressure()) {
            p = p - 25;
        }
        for (; p >= 50; p = p - 25) {
            newLayer = new NcSoundingLayer();
            newLayer.setPressure(p);
            interpolatedValue = NsharpLibBasics.i_temp(rawSndLysLst, p);
            if (interpolatedValue == NcSoundingLayer.MISSING) {
                // this is not good layer data, usually happened when lowest
                // layer pressure is
                // more than 50, then when interpolate layer for pressure 50,
                // will return unvalid value
                continue;
            }
            newLayer.setTemperature(interpolatedValue);
            interpolatedValue = NsharpLibBasics.i_dwpt(rawSndLysLst, p);
            newLayer.setDewpoint(interpolatedValue);
            interpolatedValue = NsharpLibBasics.i_wdir(rawSndLysLst, p);
            newLayer.setWindDirection(interpolatedValue);
            interpolatedValue = NsharpLibBasics.i_wspd(rawSndLysLst, p);
            newLayer.setWindSpeed(interpolatedValue);
            interpolatedValue = NsharpLibBasics.i_hght(rawSndLysLst, p);
            newLayer.setGeoHeight(interpolatedValue);
            interpolatedValue = NsharpLibBasics.i_omeg(rawSndLysLst, p);
            newLayer.setOmega(interpolatedValue);

            mySndLst.add(newLayer);
        }
        return mySndLst;
    }

    private void setSoundingInfo(List<NcSoundingLayer> sndLys) {
        if(soundingLys != null && soundingLys != sndLys){
            previousSoundingLys = soundingLys;
        }

        if (interpolateIsOn) {
            soundingLys = performInterpolation(sndLys);
        } else {
            soundingLys = sndLys;

        }
    }

    public void updateDisplay(IRenderableDisplay[] displayArray,
            String paneConfigurationName) {
        skewtPaneRsc = null;
        witoPaneRsc = null;
        hodoPaneRsc = null;
        timeStnPaneRsc = null;
        insetPaneRsc = null;
        dataPaneRsc = null;
        spcGraphsPaneRsc = null;
        for (IRenderableDisplay disp : displayArray) {
            List<NsharpAbstractPaneResource> paneResources = disp
                    .getDescriptor().getResourceList()
                    .getResourcesByTypeAsType(NsharpAbstractPaneResource.class);
            for (NsharpAbstractPaneResource absPaneRsc : paneResources) {
                if (absPaneRsc instanceof NsharpSkewTPaneResource) {
                    skewtPaneRsc = (NsharpSkewTPaneResource) absPaneRsc;
                    skewtPaneRsc.setLinePropertyMap(linePropertyMap);
                    skewtPaneRsc.setGraphConfigProperty(graphConfigProperty);
                } else if (absPaneRsc instanceof NsharpDataPaneResource) {
                    dataPaneRsc = (NsharpDataPaneResource) absPaneRsc;
                    dataPaneRsc.setLinePropertyMap(linePropertyMap);
                    dataPaneRsc.setGraphConfigProperty(graphConfigProperty);
                    dataPaneRsc.setPageDisplayOrderNumberArray(
                            pageDisplayOrderNumberArray,
                            dataPageProperty.getNumberPagePerDisplay());
                } else if (absPaneRsc instanceof NsharpHodoPaneResource) {
                    hodoPaneRsc = (NsharpHodoPaneResource) absPaneRsc;
                    hodoPaneRsc.setLinePropertyMap(linePropertyMap);
                    hodoPaneRsc.setGraphConfigProperty(graphConfigProperty);
                } else if (absPaneRsc instanceof NsharpWitoPaneResource) {
                    witoPaneRsc = (NsharpWitoPaneResource) absPaneRsc;
                    witoPaneRsc.setLinePropertyMap(linePropertyMap);
                    witoPaneRsc.setGraphConfigProperty(graphConfigProperty);
                } else if (absPaneRsc instanceof NsharpInsetPaneResource) {
                    insetPaneRsc = (NsharpInsetPaneResource) absPaneRsc;
                    insetPaneRsc.setLinePropertyMap(linePropertyMap);
                    insetPaneRsc.setGraphConfigProperty(graphConfigProperty);
                } else if (absPaneRsc instanceof NsharpSpcGraphsPaneResource) {
                    spcGraphsPaneRsc = (NsharpSpcGraphsPaneResource) absPaneRsc;
                    spcGraphsPaneRsc.setLinePropertyMap(linePropertyMap);
                    spcGraphsPaneRsc.setGraphConfigProperty(graphConfigProperty);
                } else if (absPaneRsc instanceof NsharpTimeStnPaneResource) {
                    timeStnPaneRsc = (NsharpTimeStnPaneResource) absPaneRsc;
                    timeStnPaneRsc.setLinePropertyMap(linePropertyMap);
                    timeStnPaneRsc.setGraphConfigProperty(graphConfigProperty);
                } else {
                    absPaneRsc.setLinePropertyMap(linePropertyMap);
                    absPaneRsc.setGraphConfigProperty(graphConfigProperty);
                }
            }
        }
        this.displayArray = displayArray;
    }

    public void resetRscSoundingData() {
        weatherDataStore.computeWeatherParameters(soundingLys,
                paneConfigurationName);
        if (skewtPaneRsc != null) {
            skewtPaneRsc.resetData(soundingLys);
        }
        if (hodoPaneRsc != null) {
            hodoPaneRsc.resetData(soundingLys);
        }
        if (witoPaneRsc != null) {
            witoPaneRsc.resetData(soundingLys);
        }
        if (dataPaneRsc != null) {
            dataPaneRsc.resetData(soundingLys);
        }
        if (insetPaneRsc != null) {
            insetPaneRsc.resetData(soundingLys);
        }
        if (spcGraphsPaneRsc != null) {
            spcGraphsPaneRsc.resetData(soundingLys);
        }
    }

    public NsharpResourceHandler(IRenderableDisplay[] displayArray,
            NsharpEditor editor) {
        elementColorMap.put(NsharpConstants.ActState.CURRENT.name(),
                NsharpConstants.color_green);
        elementColorMap.put(NsharpConstants.ActState.ACTIVE.name(),
                NsharpConstants.color_yellow);
        elementColorMap.put(NsharpConstants.ActState.INACTIVE.name(),
                NsharpConstants.color_white);

        // based on BigNsharp storm slinky color used and gempak color
        // definition
        stormSlinkyColorMap.put(new Integer(3), NsharpConstants.color_green);
        stormSlinkyColorMap.put(new Integer(7), NsharpConstants.color_magenta);
        stormSlinkyColorMap.put(new Integer(6), NsharpConstants.color_cyan);
        stormSlinkyColorMap.put(new Integer(13),
                NsharpConstants.color_violet_md);
        stormSlinkyColorMap.put(new Integer(20),
                NsharpConstants.color_yellow_DK);
        stormSlinkyColorMap.put(new Integer(27), NsharpConstants.color_cyan_md);
        NsharpPaletteWindow win = NsharpPaletteWindow.getInstance();
        if (win != null) {
            currentGraphMode = win.getCurrentGraphMode();
        }

        // new for configMgr
        configMgr = NsharpConfigManager.getInstance();
        configStore = configMgr.retrieveNsharpConfigStoreFromFs();
        graphConfigProperty = configStore.getGraphProperty();
        paneConfigurationName = graphConfigProperty.getPaneConfigurationName();

        int tempOffset = graphConfigProperty.getTempOffset();
        NsharpWxMath.setTempOffset(tempOffset);
        sndCompRadius = graphConfigProperty.getSndCompRadius();
        linePropertyMap = configStore.getLinePropertyMap();
        dataPageProperty = configStore.getDataPageProperty();
        updatePageOrderArray();
        updateDisplay(displayArray, paneConfigurationName);
        displayDataPageMax = NsharpConstants.PAGE_MAX_NUMBER
                / dataPageProperty.getNumberPagePerDisplay();

        weatherDataStore = new NsharpWeatherDataStore();
    }

    public void setDisplayDataPageMax(int displayDataPageMax) {
        this.displayDataPageMax = displayDataPageMax;
    }

    public void disposeInternal() {
        listenerList = null;
        soundingLys = null;
        previousSoundingLys = null;
        stormSlinkyColorMap = null;
        elementColorMap = null;

    }

    public boolean isPlotInteractiveTemp() {
        return plotInteractiveTemp;
    }

    public void setPlotInteractiveTemp(boolean plotInteractiveTemp) {
        this.plotInteractiveTemp = plotInteractiveTemp;
        if (skewtPaneRsc != null) {
            skewtPaneRsc.setPlotInteractiveTemp(plotInteractiveTemp);
        }
    }

    public void setInteractiveTempPointCoordinate(
            Coordinate interactiveTempPointCoordinate) {
        this.interactiveTempPointCoordinate = interactiveTempPointCoordinate;
        plotInteractiveTemp = true;
        if (skewtPaneRsc != null) {
            skewtPaneRsc.setPlotInteractiveTemp(plotInteractiveTemp);
            skewtPaneRsc.setInteractiveTempPointCoordinate(
                    interactiveTempPointCoordinate);
        }
    }

    public void setInteractiveHodoPointCoordinate(Coordinate c) {
        if (hodoPaneRsc == null) {
            return;
        }
        try {
            NcSoundingLayer hodoLayer = soundingLys
                    .get(hodoEditingSoundingLayerIndex);
            if (hodoLayer != null) {

                Coordinate c1 = hodoPaneRsc.getHodoBackground().getWorld()
                        .unMap(c.x, c.y);
                c1 = WxMath.speedDir((float) c1.x, (float) c1.y);
                hodoLayer.setWindSpeed((float) c1.x);
                hodoLayer.setWindDirection((float) c1.y);
                weatherDataStore.computeWeatherParameters(soundingLys,
                        paneConfigurationName);
                hodoPaneRsc.createRscHodoWindShapeAll();
                if (witoPaneRsc != null) {
                    witoPaneRsc.createAllWireFrameShapes();
                }
                if (insetPaneRsc != null) {
                    insetPaneRsc.createInsetWireFrameShapes();
                }
                if (skewtPaneRsc != null) {
                    skewtPaneRsc.createRscWireFrameShapes();
                }
            }
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM,
                    "setInteractiveHodoPointCoordinate exception:", e);
        }
    }

    public void applyMovingTempLine() {
        if (skewtPaneRsc == null) {
            return;
        }
        Coordinate inC = NsharpWxMath.reverseSkewTXY(
                skewtPaneRsc.getWorld().unMap(interactiveTempPointCoordinate));
        float inTemp = (float) inC.x;
        currentSoundingLayerIndex = skewtPaneRsc.getCurrentSoundingLayerIndex();
        currentTempCurveType = skewtPaneRsc.getCurrentTempCurveType();
        float currentLayerTemp, currentLayerDewP;
        float smallestGap = skewtPaneRsc.getTempDewPtSmallestGap();
        float tempShiftedDist;
        currentLayerTemp = soundingLys.get(currentSoundingLayerIndex)
                .getTemperature();
        currentLayerDewP = soundingLys.get(currentSoundingLayerIndex)
                .getDewpoint();
        if (currentTempCurveType == TEMP_TYPE) {
            if (inTemp < currentLayerTemp) {
                // shift to left, tempShiftedDist should be a negative number
                if ((currentLayerTemp - inTemp) > smallestGap) {
                    tempShiftedDist = -smallestGap;
                } else {
                    tempShiftedDist = inTemp - currentLayerTemp;
                }
            } else {
                // shift to right, tempShiftedDist should be a positive number
                tempShiftedDist = inTemp - currentLayerTemp;
            }
        } else {
            if (inTemp < currentLayerDewP) {
                // shift to left, tempShiftedDist should be a negative number
                tempShiftedDist = inTemp - currentLayerDewP;
            } else {
                // shift to right, tempShiftedDist should be a positive number
                if ((inTemp - currentLayerDewP) > smallestGap) {
                    tempShiftedDist = smallestGap;
                } else {
                    tempShiftedDist = inTemp - currentLayerDewP;
                }
            }
        }
        for (NcSoundingLayer layer : soundingLys) {
            float t;
            if (currentTempCurveType == TEMP_TYPE) {
                t = layer.getTemperature();
                if (t != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA) {
                    layer.setTemperature(t + tempShiftedDist);
                }
            } else {
                t = layer.getDewpoint();
                if (t != NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA) {
                    layer.setDewpoint(t + tempShiftedDist);
                }
            }
        }
        weatherDataStore.computeWeatherParameters(soundingLys,
                paneConfigurationName);
        // get storm motion wind data after populate sounding from NsharpLib
        skewtPaneRsc.setSoundingLys(soundingLys);
        skewtPaneRsc.createRscWireFrameShapes();
        if (hodoPaneRsc != null) {
            hodoPaneRsc.setSoundingLys(soundingLys);
            hodoPaneRsc.createRscHodoWindShapeAll();
        }
    }

    public void applyInteractiveTempPoint() {
        if (skewtPaneRsc == null) {
            return;
        }
        Coordinate inC = NsharpWxMath.reverseSkewTXY(
                skewtPaneRsc.getWorld().unMap(interactiveTempPointCoordinate));
        double inTemp = inC.x;
        currentSoundingLayerIndex = skewtPaneRsc.getCurrentSoundingLayerIndex();
        NcSoundingLayer layer = this.soundingLys.get(currentSoundingLayerIndex);
        currentTempCurveType = skewtPaneRsc.getCurrentTempCurveType();
        if (currentTempCurveType == TEMP_TYPE) {
            if (inTemp < layer.getDewpoint()) {
                // temp can not be lower than dew point
                layer.setTemperature(layer.getDewpoint());
            } else {
                layer.setTemperature((float) inTemp);
            }
        } else {
            if (inTemp > layer.getTemperature()) {
                // dew point can not be higher than temp
                layer.setDewpoint(layer.getTemperature());
            } else {
                layer.setDewpoint((float) inTemp);
            }
        }

        weatherDataStore.computeWeatherParameters(soundingLys,
                paneConfigurationName);
        // get storm motion wind data after populate sounding from NsharpLib
        skewtPaneRsc.setSoundingLys(soundingLys);
        skewtPaneRsc.createRscWireFrameShapes();

        if (hodoPaneRsc != null) {
            hodoPaneRsc.setSoundingLys(soundingLys);
            hodoPaneRsc.createRscHodoWindShapeAll();
        }
    }

    public void updateLayer(int layerIndex, float tp, float dp, float ws,
            float wd, float pressure) {
        if (layerIndex < 0 || layerIndex >= soundingLys.size()) {
            return;
        }
        currentSoundingLayerIndex = layerIndex;
        NcSoundingLayer layer = soundingLys.get(currentSoundingLayerIndex);
        layer.setGeoHeight(NsharpLibBasics.i_hght(soundingLys, pressure));
        layer.setTemperature(tp);
        layer.setDewpoint(dp);
        layer.setWindDirection(wd);
        layer.setWindSpeed(ws);
        layer.setPressure(pressure);
        // re-populate snd data to nsharp native code lib for later calculating
        Collections.sort(soundingLys,
                NsharpDataHandling.reversePressureHeightWindComparator());
        goodData = checkDataIntegrity(soundingLys);
        if (getCurSoundingElementStateProperty() != null) {
            getCurSoundingElementStateProperty().setGoodData(goodData);
        }
        if (goodData) {
            weatherDataStore.computeWeatherParameters(soundingLys,
                    paneConfigurationName);
        }
        // get storm motion wind data after populate sounding from NsharpLib
        // refresh test area if it is shown now
        NsharpShowTextDialog textarea = NsharpShowTextDialog.getAccess();
        if (textarea != null) {
            textarea.refreshTextData();
        }
        if (skewtPaneRsc != null) {
            skewtPaneRsc.setSoundingLys(soundingLys);
            // CHIN:::fix edit zoom issue skewtPaneRsc.handleResize();
            skewtPaneRsc.createRscWireFrameShapes();
        }
        if (hodoPaneRsc != null) {
            hodoPaneRsc.setSoundingLys(soundingLys);
            hodoPaneRsc.createRscHodoWindShapeAll();
        }
        if (insetPaneRsc != null) {
            insetPaneRsc.setSoundingLys(soundingLys);
            insetPaneRsc.createInsetWireFrameShapes();
        }
        if (witoPaneRsc != null) {
            witoPaneRsc.setSoundingLys(soundingLys);
            witoPaneRsc.createAllWireFrameShapes();
        }
        if (dataPaneRsc != null) {
            dataPaneRsc.setSoundingLys(soundingLys);
        }

    }

    public void addNewLayer(float tp, float dp, float ws, float wd,
            float pressure) {
        NcSoundingLayer layer = new NcSoundingLayer();
        layer.setGeoHeight(NsharpLibBasics.i_hght(soundingLys, pressure));
        layer.setTemperature(tp);
        layer.setDewpoint(dp);
        layer.setWindDirection(wd);
        layer.setWindSpeed(ws);
        layer.setPressure(pressure);
        soundingLys.add(layer);
        // re-populate snd data to nsharp native code lib for later calculating
        Collections.sort(soundingLys,
                NsharpDataHandling.reversePressureHeightWindComparator());
        goodData = checkDataIntegrity(soundingLys);
        if (getCurSoundingElementStateProperty() != null) {
            getCurSoundingElementStateProperty().setGoodData(goodData);
        }
        if (goodData) {
            weatherDataStore.computeWeatherParameters(soundingLys,
                    paneConfigurationName);
        }
        // get storm motion wind data after populate sounding from NsharpLib
        // refresh text area if it is shown now
        NsharpShowTextDialog textarea = NsharpShowTextDialog.getAccess();
        if (textarea != null) {
            textarea.refreshTextData();
        }

        if (skewtPaneRsc != null) {
            skewtPaneRsc.setSoundingLys(soundingLys);
            // CHIN:::fix edit zoom issue skewtPaneRsc.handleResize();
            skewtPaneRsc.createRscWireFrameShapes();
        }
        if (hodoPaneRsc != null) {
            hodoPaneRsc.setSoundingLys(soundingLys);
            hodoPaneRsc.createRscHodoWindShapeAll();
        }
        if (insetPaneRsc != null) {
            insetPaneRsc.setSoundingLys(soundingLys);
            insetPaneRsc.createInsetWireFrameShapes();
        }
        if (witoPaneRsc != null) {
            witoPaneRsc.setSoundingLys(soundingLys);
            witoPaneRsc.createAllWireFrameShapes();
        }
        if (dataPaneRsc != null) {
            dataPaneRsc.setSoundingLys(soundingLys);
        }
    }

    public void setGraphConfigProperty(
            NsharpGraphProperty graphConfigProperty) {
        this.graphConfigProperty = graphConfigProperty;
        int tempOffset = graphConfigProperty.getTempOffset();
        sndCompRadius = graphConfigProperty.getSndCompRadius();
        NsharpWxMath.setTempOffset(tempOffset);
        if (skewtPaneRsc != null) {
            skewtPaneRsc.setGraphConfigProperty(graphConfigProperty);
            skewtPaneRsc.createRscWireFrameShapes();// handleResize();
            skewtPaneRsc.getSkewTBackground()
                    .setGraphConfigProperty(graphConfigProperty);
        }
        if (hodoPaneRsc != null) {
            hodoPaneRsc.setGraphConfigProperty(graphConfigProperty);
            hodoPaneRsc.createRscHodoWindShapeAll();
        }
        if (witoPaneRsc != null) {
            witoPaneRsc.setGraphConfigProperty(graphConfigProperty);
            witoPaneRsc.createAllWireFrameShapes();
        }
        if (insetPaneRsc != null) {
            insetPaneRsc.setGraphConfigProperty(graphConfigProperty);
            insetPaneRsc.createInsetWireFrameShapes();
        }
    }

    public NsharpGraphProperty getGraphConfigProperty() {
        return graphConfigProperty;
    }

    public void setLinePropertyMap(
            HashMap<String, NsharpLineProperty> linePropertyMap) {
        this.linePropertyMap = linePropertyMap;
        if (skewtPaneRsc != null) {
            skewtPaneRsc.setLinePropertyMap(linePropertyMap);
            skewtPaneRsc.handleResize();
        }
        if (hodoPaneRsc != null) {
            hodoPaneRsc.setLinePropertyMap(linePropertyMap);
            hodoPaneRsc.createRscHodoWindShapeAll();
        }
        if (timeStnPaneRsc != null) {
            timeStnPaneRsc.setLinePropertyMap(linePropertyMap);
        }
    }

    private void updatePageOrderArray() {
        pageDisplayOrderNumberArray[NsharpConstants.PAGE_SUMMARY1] = dataPageProperty
                .getSummary1Page();
        pageDisplayOrderNumberArray[NsharpConstants.PAGE_SUMMARY2] = dataPageProperty
                .getSummary2Page();
        pageDisplayOrderNumberArray[NsharpConstants.PAGE_PARCEL_DATA] = dataPageProperty
                .getParcelDataPage();
        pageDisplayOrderNumberArray[NsharpConstants.PAGE_THERMODYNAMIC_DATA] = dataPageProperty
                .getThermodynamicDataPage();
        pageDisplayOrderNumberArray[NsharpConstants.PAGE_OPC_DATA] = dataPageProperty
                .getOpcDataPage();
        pageDisplayOrderNumberArray[NsharpConstants.PAGE_MIXING_HEIGHT] = dataPageProperty
                .getMixingHeightPage();
        pageDisplayOrderNumberArray[NsharpConstants.PAGE_STORM_RELATIVE] = dataPageProperty
                .getStormRelativePage();
        pageDisplayOrderNumberArray[NsharpConstants.PAGE_MEAN_WIND] = dataPageProperty
                .getMeanWindPage();
        pageDisplayOrderNumberArray[NsharpConstants.PAGE_CONVECTIVE_INITIATION] = dataPageProperty
                .getConvectiveInitiationPage();
        pageDisplayOrderNumberArray[NsharpConstants.PAGE_SEVERE_POTENTIAL] = dataPageProperty
                .getSeverePotentialPage();
        pageDisplayOrderNumberArray[NsharpConstants.PAGE_D2DLITE] = dataPageProperty
                .getD2dLitePage();
        pageDisplayOrderNumberArray[NsharpConstants.PAGE_FUTURE] = dataPageProperty
                .getFuturePage();
    }

    public void setDataPageProperty(NsharpDataPageProperty dataPageProperty) {
        this.dataPageProperty = dataPageProperty;
        updatePageOrderArray();
        if (dataPaneRsc != null) {
            dataPaneRsc.setPageDisplayOrderNumberArray(
                    pageDisplayOrderNumberArray,
                    dataPageProperty.getNumberPagePerDisplay());
        }
    }

    public int getCurrentTextChapter() {
        return currentTextChapter;
    }

    public float getCurrentParcelLayerPressure() {
        return currentParcelLayerPressure;
    }

    public NsharpSkewTPaneResource getSkewtPaneRsc() {
        return skewtPaneRsc;
    }

    public NsharpHodoPaneResource getHodoPaneRsc() {
        return hodoPaneRsc;
    }

    public NsharpSoundingElementStateProperty getPreSndProfileProp() {
        return preSndProfileProp;
    }

    public int getCurrentInsetPage() {
        return currentInsetPage;
    }

    public int getCurrentSoundingLayerIndex() {
        return currentSoundingLayerIndex;
    }

    public IFrameCoordinator.FrameChangeOperation getCurrentOpDirection() {
        return currentOpDirection;
    }

    public NsharpSoundingElementStateProperty getCurSndProfileProp() {
        return curSndProfileProp;
    }

    public Map<String, RGB> getElementColorMap() {
        return elementColorMap;
    }

    public void recomputeWeatherData() {
        weatherDataStore.computeWeatherParameters(soundingLys,
                paneConfigurationName);
    }

    public NsharpWitoPaneResource getWitoPaneRsc() {
        return witoPaneRsc;
    }

    public NsharpInsetPaneResource getInsetPaneRsc() {
        return insetPaneRsc;
    }

    public NsharpSpcGraphsPaneResource getSpcGraphsPaneRsc() {
        return spcGraphsPaneRsc;
    }

    public NsharpDataPaneResource getDataPaneRsc() {
        return dataPaneRsc;
    }

    public void refreshPane() {
        for (int i = 0; i < displayArray.length; i += 1) {
            displayArray[i].refresh();
        }
    }

    public String getPaneConfigurationName() {
        return paneConfigurationName;
    }

    public void setPaneConfigurationName(String paneConfigName) {
        if (this.paneConfigurationName.equals(paneConfigName)) {
            return;
        }

        currentGraphMode = NsharpConstants.GRAPH_SKEWT;
        this.paneConfigurationName = paneConfigName;
        if (this.paneConfigurationName
                .equals(NsharpConstants.PANE_LITE_D2D_CFG_STR)
                || this.paneConfigurationName
                        .equals(NsharpConstants.PANE_OPC_CFG_STR)) {
            int numberPagePerDisplay = 1;
            dataPageProperty.setNumberPagePerDisplay(numberPagePerDisplay);
        }
        displayDataPageMax = NsharpConstants.PAGE_MAX_NUMBER
                / dataPageProperty.getNumberPagePerDisplay();
    }



    /*
     * Return size of stnTimeSndTable Note that not all elements in this table
     * has sounding data loaded. Therefore, returned size may be much bigger
     * than the number of actual loaded sounding data frame.
     */
    public int getFrameCount() {
        return timeElementList.size() * sndElementList.size()
                * stnElementList.size();
    }

    /*
     * return "accumulated" index to stnTimeSndTable of current displayed
     * sounding data
     */
    public int getCurrentIndex() {
        int index = 0;
        if (currentSndElementListIndex >= 0 && currentStnElementListIndex >= 0
                && currentTimeElementListIndex >= 0) {
            index = currentSndElementListIndex
                    + currentTimeElementListIndex * sndElementList.size()
                    + currentStnElementListIndex * timeElementList.size()
                            * sndElementList.size();
        }
        return index;
    }

    /*
     * set current displayed sounding data using the input "accumulated" index
     * (to stnTimeSndTable) return false: If input index is not valid or
     * sounding data is not loaded for input index return true: if set
     * successfully
     */
    public boolean setCurrentIndex(int index) {
        if (index < 0 || index >= (timeElementList.size()
                * sndElementList.size() * stnElementList.size())) {
            return false;
        }
        int tempStni, tempSndi, tempTmi;
        tempStni = index / (timeElementList.size() * sndElementList.size());
        tempTmi = (index % (timeElementList.size() * sndElementList.size()))
                / sndElementList.size();
        tempSndi = (index % (timeElementList.size() * sndElementList.size()))
                % sndElementList.size();
        if (timeElementList.get(tempTmi)
                .getActionState() == NsharpConstants.ActState.INACTIVE
                || stnElementList.get(tempStni)
                        .getActionState() == NsharpConstants.ActState.INACTIVE
                || sndElementList.get(tempSndi)
                        .getActionState() == NsharpConstants.ActState.INACTIVE
                || stnTimeSndTable.get(tempStni).get(tempTmi)
                        .get(tempSndi) == null) {
            return false;
        }
        currentStnElementListIndex = tempStni;
        currentTimeElementListIndex = tempTmi;
        currentSndElementListIndex = tempSndi;
        setCurSndProfileProp();
        setCurrentSoundingLayerInfo();
        resetData();
        refreshPane();
        return true;
    }

    public boolean isGoodData() {
        return goodData;
    }

    public NsharpWeatherDataStore getWeatherDataStore() {
        return weatherDataStore;
    }

    public boolean isAnyCompareOn() {
        return compareSndIsOn || compareStnIsOn || compareTmIsOn;
    }

    public List<NsharpSoundingElementStateProperty> getComparisonProperties() {
        List<NsharpSoundingElementStateProperty> rslt = Collections.emptyList();
        if (compareStnIsOn) {
            rslt = new ArrayList<>(stnElementList.size());
            for (int stnIndex = 0; stnIndex < stnElementList
                    .size(); stnIndex += 1) {
                NsharpOperationElement elm = stnElementList.get(stnIndex);
                if (elm.getActionState() == NsharpConstants.ActState.ACTIVE) {
                    rslt.add(stnTimeSndTable.get(stnIndex)
                            .get(currentTimeElementListIndex)
                            .get(currentSndElementListIndex));
                }
            }
        } else if (compareSndIsOn) {
            rslt = new ArrayList<>(sndElementList.size());
            for (int sndIndex = 0; sndIndex < sndElementList
                    .size(); sndIndex += 1) {
                NsharpOperationElement elm = sndElementList.get(sndIndex);
                if (elm.getActionState() == NsharpConstants.ActState.ACTIVE) {
                    rslt.add(stnTimeSndTable.get(currentStnElementListIndex)
                            .get(currentTimeElementListIndex).get(sndIndex));
                }
            }
        } else if (compareTmIsOn) {
            rslt = new ArrayList<>(timeElementList.size());
            for (int timeIndex = 0; timeIndex < timeElementList
                    .size(); timeIndex += 1) {
                NsharpOperationElement elm = timeElementList.get(timeIndex);
                if (elm.getActionState() == NsharpConstants.ActState.ACTIVE) {
                    rslt.add(stnTimeSndTable.get(currentStnElementListIndex)
                            .get(timeIndex).get(currentSoundingLayerIndex));
                }
            }
        }
        int colorIndex = NsharpConstants.LINE_COMP1;
        for(NsharpSoundingElementStateProperty prop : rslt){
            if(prop != null){
                prop.setCompColorIndex(colorIndex);
            }
            colorIndex += 1;
            if (colorIndex > NsharpConstants.LINE_COMP10){
                colorIndex = NsharpConstants.LINE_COMP1;
            }
        }
        return rslt;
    }

}
