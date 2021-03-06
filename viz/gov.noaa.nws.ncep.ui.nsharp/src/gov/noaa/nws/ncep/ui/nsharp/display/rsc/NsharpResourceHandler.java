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
 * </pre>
 * 
 * @author Chin Chen
 * @version 1.0
 */
package gov.noaa.nws.ncep.ui.nsharp.display.rsc;

import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibBasics;
import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibSndglib;
import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibThermo;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.LayerParameters;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.Parcel;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;
import gov.noaa.nws.ncep.ui.nsharp.NsharpConfigManager;
import gov.noaa.nws.ncep.ui.nsharp.NsharpConfigStore;
import gov.noaa.nws.ncep.ui.nsharp.NsharpConstants;
import gov.noaa.nws.ncep.ui.nsharp.NsharpDataPageProperty;
import gov.noaa.nws.ncep.ui.nsharp.NsharpGraphProperty;
import gov.noaa.nws.ncep.ui.nsharp.NsharpLineProperty;
import gov.noaa.nws.ncep.ui.nsharp.NsharpOperationElement;
import gov.noaa.nws.ncep.ui.nsharp.NsharpSoundingElementStateProperty;
import gov.noaa.nws.ncep.ui.nsharp.NsharpStationInfo;
import gov.noaa.nws.ncep.ui.nsharp.NsharpWGraphics;
import gov.noaa.nws.ncep.ui.nsharp.NsharpWxMath;
import gov.noaa.nws.ncep.ui.nsharp.display.NsharpEditor;
import gov.noaa.nws.ncep.ui.nsharp.display.map.NsharpMapResource;
import gov.noaa.nws.ncep.ui.nsharp.natives.NsharpDataHandling;
import gov.noaa.nws.ncep.ui.nsharp.view.NsharpPaletteWindow;
import gov.noaa.nws.ncep.ui.nsharp.view.NsharpParcelDialog;
import gov.noaa.nws.ncep.ui.nsharp.view.NsharpShowTextDialog;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.swt.graphics.GC;
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
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.viz.core.graphing.LineStroke;
import com.raytheon.viz.core.graphing.WindBarbFactory;
import com.vividsolutions.jts.geom.Coordinate;

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

    private NsharpAbstractPaneResource futurePaneRsc;

    private String[] defaultDays;

    private int displayDataPageMax;

    private static final int INSETPAGEMAX = 2;

    private int currentTextChapter = 1;

    private int currentInsetPage = 1;

    private int dtNextPageEnd = NsharpConstants.DATA_TIMELINE_NEXT_PAGE_END_;

    private double charHeight = NsharpConstants.CHAR_HEIGHT_;

    private double lineHeight = charHeight;

    private int dtXOrig = NsharpConstants.DATA_TIMELINE_X_ORIG;

    private int dtYOrig = NsharpConstants.DATA_TIMELINE_Y_ORIG;

    private int dtWidth = NsharpConstants.DATA_TIMELINE_WIDTH;

    private String paneConfigurationName;

    private int numTimeLinePerPage = 1;

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

    protected static final double BARB_LENGTH = 3.5;

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
    //        stn3-> T1--->T2--->T3->...
    //        ^
    //       /
    //      stn2-> T1--->T2--->T3->...
    //      ^
    //     /
    // stn1-> T1--->T2--->T3->...
    //         |    |    |
    //         V    V    V
    //      snd1 snd1 snd1
    //         |    |    |
    //         V    V    V
    //      snd2 snd2 snd2
    //         |    |    |
    //         V    V    V
    // stnTimeSndTable first dimension (station id) should be in sync with
    // stnElementList,
    // 2nd dimension (time line) should be in sync with timeElementList, and
    // 3rd dimension (sounding type) should be in sync with sndTypeElementList
    // NULL element in stnTimeSndTable indicates that sounding data is not
    // loaded yet.

    private List<NsharpOperationElement> stnElementList = new ArrayList<>();

    private List<NsharpOperationElement> timeElementList = new ArrayList<>();

    private List<NsharpOperationElement> sndElementList = new ArrayList<>();

    private NsharpSoundingElementStateProperty curSndProfileProp = null;

    private NsharpSoundingElementStateProperty preSndProfileProp = null;

    private int curTimeLinePage = 1;

    private int totalTimeLinePage = 1;

    private int curStnIdPage = 1;

    private int totalStnIdPage = 1;

    private int curSndPage = 1;

    private int totalSndPage = 1;

    private int previousTimeLineStateListIndex;

    // index to first dim of stnTimeSndTable and index to stnElementList
    private int currentStnElementListIndex = -1;

    // index to 2nd dim of stnTimeSndTable and index to timeElementList
    private int currentTimeElementListIndex = -1;

    // index to 3rd dim of stnTimeSndTable and index to sndElementList
    private int currentSndElementListIndex = -1;

    // use element state, NsharpConstants.LoadState or NsharpConstants.ActState,
    // as key to set color for drawing
    private HashMap<String, RGB> elementColorMap = new HashMap<>();

    private int currentParcel = NsharpLibSndglib.PARCELTYPE_MOST_UNSTABLE;

    private float currentParcelLayerPressure = NsharpLibSndglib.MU_LAYER_PRESS;

    private float smWindDir, smWindSpd;

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
                    if (elem != null)
                        strLst.add(elem.getElementDescription());
                }
            }
        }
        return strLst;
    }

    public List<NsharpOperationElement> getStnElementList() {
        return stnElementList;
    }

    public List<NsharpOperationElement> getTimeElementList() {
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

    public HashMap<String, NsharpLineProperty> getLinePropertyMap() {
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
            currentTextChapter++;
            // d2dlite or OCP if one page per chap case, skip future page
            if ((dataPageProperty.getNumberPagePerDisplay() == 1
                    || paneConfigurationName
                            .equals(NsharpConstants.PANE_LITE_D2D_CFG_STR)
                    || paneConfigurationName
                            .equals(NsharpConstants.PANE_OPC_CFG_STR))
                    && currentTextChapter == displayDataPageMax)
                currentTextChapter = 1;
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
                            .equals(NsharpConstants.PANE_OPC_CFG_STR))
                currentTextChapter = displayDataPageMax - 1;
        }
    }

    public void setPrevInsetPage() {
        currentInsetPage--;
        if (currentInsetPage == 0) {
            currentInsetPage = INSETPAGEMAX;
        }
    }

    public void setNextInsetPage() {
        if (currentInsetPage == INSETPAGEMAX) {
            currentInsetPage = 1;
        } else
            currentInsetPage++;
    }

    public void setOverlayIsOn(boolean overlay) {
        previousSoundingLys = null;
        previousTimeLineStateListIndex = -1;
        preSndProfileProp = null;
        this.overlayIsOn = overlay;

        if (hodoPaneRsc != null)
            hodoPaneRsc.createRscHodoWindShapeAll();
        if (skewtPaneRsc != null)
            skewtPaneRsc.handleResize();
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
                for (int i = 0; i < stnElementList.size(); i++) {
                    if (stnElementList.get(i)
                            .getActionState() == NsharpConstants.ActState.ACTIVE
                            && stnTimeSndTable.get(i)
                                    .get(currentTimeElementListIndex)
                                    .get(currentSndElementListIndex) != null) {
                        found = true;
                        currentStnElementListIndex = i;
                    }

                    if (found)
                        break;
                }
                if (!found)
                    return;
            }

            int colorIndex = NsharpConstants.LINE_COMP1;
            for (NsharpOperationElement elm : stnElementList) {
                int stnIndex = stnElementList.indexOf(elm);
                NsharpSoundingElementStateProperty stnTmElm = stnTimeSndTable
                        .get(stnIndex).get(currentTimeElementListIndex)
                        .get(currentSndElementListIndex);
                if (stnTmElm != null) {
                    stnTmElm.setCompColorIndex(colorIndex);
                }
                colorIndex++;
                if (colorIndex > NsharpConstants.LINE_COMP10)
                    colorIndex = NsharpConstants.LINE_COMP1;
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
                for (int i = 0; i < sndElementList.size(); i++) {
                    if (sndElementList.get(i)
                            .getActionState() == NsharpConstants.ActState.ACTIVE
                            && stnTimeSndTable.get(currentStnElementListIndex)
                                    .get(currentTimeElementListIndex)
                                    .get(i) != null) {
                        found = true;
                        currentSndElementListIndex = i;
                    }
                    if (found)
                        break;
                }
                if (!found)
                    return;
            }
            int colorIndex = NsharpConstants.LINE_COMP1;

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
                    .get(currentTimeElementListIndex).getElementDescription()
                    .substring(0, timeLineLengthToComp);
            // loop through stns list to find "ACTIVE" stns which are within snd
            // comparison radius
            for (int i = 0; i < stnElementList.size(); i++) {
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
                    for (int k = 0; k < timeElementList.size(); k++) {
                        boolean goodTimeLine = false;
                        if (k != currentTimeElementListIndex) {
                            String timeToCopm1 = timeElementList.get(k)
                                    .getElementDescription()
                                    .substring(0, timeLineLengthToComp);
                            if (currentTimeLineToComp
                                    .equals(timeToCopm1) == true) {
                                goodTimeLine = true;
                            }
                        } else {
                            // currentTimeElementListIndex is sure to be a good
                            // time line to use
                            goodTimeLine = true;
                        }
                        if (goodTimeLine == true) {
                            for (int j = 0; j < sndElementList.size(); j++) {
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
            // now set colors here to keep color assignment (numbers) always
            // consistently counting from first snd source down
            for (int j = 0; j < sndElementList.size(); j++) {
                for (CompSndSelectedElem elem : compSndSelectedElemList) {
                    if (elem.getSndIndex() == j) {
                        NsharpSoundingElementStateProperty stnTmElm = stnTimeSndTable
                                .get(elem.getStnIndex())
                                .get(elem.getTimeIndex()).get(j);
                        stnTmElm.setCompColorIndex(colorIndex);
                        colorIndex++;
                        if (colorIndex > NsharpConstants.LINE_COMP10)
                            colorIndex = NsharpConstants.LINE_COMP1;
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
                for (int i = 0; i < timeElementList.size(); i++) {
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
            int colorIndex = NsharpConstants.LINE_COMP1;
            for (NsharpOperationElement elm : timeElementList) {
                int tmIndex = timeElementList.indexOf(elm);
                NsharpSoundingElementStateProperty stnTmElm = stnTimeSndTable
                        .get(currentStnElementListIndex).get(tmIndex)
                        .get(currentSndElementListIndex);
                if (stnTmElm != null) {
                    stnTmElm.setCompColorIndex(colorIndex);
                }
                colorIndex++;
                if (colorIndex > NsharpConstants.LINE_COMP10) {
                    colorIndex = NsharpConstants.LINE_COMP1;
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
            if (skewtPaneRsc != null)
                skewtPaneRsc.setCurrentGraphMode(currentGraphMode);
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

    public float getSmWindDir() {
        return smWindDir;
    }

    public float getSmWindSpd() {
        return smWindSpd;
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
        if ((currentParcel == NsharpLibSndglib.PARCELTYPE_USER_DEFINED)
                && (currentParcelLayerPressure != NsharpParcelDialog
                        .getUserDefdParcelMb())) {
            currentParcelLayerPressure = NsharpParcelDialog
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
        smWindDir = (float) c.y;
        smWindSpd = (float) c.x;
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
    public void resetInfoOnInterpolate(boolean interpolateIsOn)
            throws CloneNotSupportedException {
        // We dont want to assume previous interpolation on/off state. So, reset
        // soundingLys any how.
        this.interpolateIsOn = interpolateIsOn;
        NsharpSoundingElementStateProperty elem = getCurSoundingElementStateProperty();
        if (elem != null) {
            if (interpolateIsOn == false) {
                soundingLys = elem.getSndLyLst();
            } else {
                soundingLys = performInterpolation(soundingLys);
            }
            weatherDataStore.computeWeatherParameters(soundingLys,
                    paneConfigurationName);
            if (skewtPaneRsc != null)
                skewtPaneRsc.resetData(soundingLys, previousSoundingLys);
            if (hodoPaneRsc != null)
                hodoPaneRsc.resetData(soundingLys, previousSoundingLys);
            if (witoPaneRsc != null)
                witoPaneRsc.resetData(soundingLys, previousSoundingLys);
            if (dataPaneRsc != null)
                dataPaneRsc.resetData(soundingLys, previousSoundingLys);
            if (insetPaneRsc != null)
                insetPaneRsc.resetData(soundingLys, previousSoundingLys);

            // re-create shape
            if (skewtPaneRsc != null)
                skewtPaneRsc.handleResize();
            if (hodoPaneRsc != null)
                hodoPaneRsc.createRscHodoWindShapeAll();
            if (insetPaneRsc != null)
                insetPaneRsc.createInsetWireFrameShapes();
            if (witoPaneRsc != null)
                witoPaneRsc.createRscWireFrameShapes();

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
        if (skewtPaneRsc != null)
            skewtPaneRsc.resetData(soundingLys, previousSoundingLys);
        if (hodoPaneRsc != null)
            hodoPaneRsc.resetData(soundingLys, previousSoundingLys);
        if (insetPaneRsc != null)
            insetPaneRsc.resetData(soundingLys, previousSoundingLys);
        if (dataPaneRsc != null)
            dataPaneRsc.resetData(soundingLys, previousSoundingLys);
        if (witoPaneRsc != null)
            witoPaneRsc.resetData(soundingLys, previousSoundingLys);

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

            // reset parcel
            currentParcel = NsharpLibSndglib.PARCELTYPE_MOST_UNSTABLE;
            currentParcelLayerPressure = NsharpLibSndglib.MU_LAYER_PRESS;
            // reset parcel dialog as well
            if (NsharpParcelDialog.getAccess() != null) {
                NsharpParcelDialog.getAccess().resetUserDefParcel();
            }
            weatherDataStore.computeWeatherParameters(soundingLys,
                    paneConfigurationName);
        }

        if (skewtPaneRsc != null)
            skewtPaneRsc.createRscWireFrameShapes();
        if (hodoPaneRsc != null)
            hodoPaneRsc.createRscHodoWindShapeAll();
        if (insetPaneRsc != null)
            insetPaneRsc.createInsetWireFrameShapes();
        if (witoPaneRsc != null)
            witoPaneRsc.createAllWireFrameShapes();
        if (spcGraphsPaneRsc != null && (goodData)) {
            // Chin: SPC graphs performance concern, as it need to call get
            // info functions from bigSharo.so and cause long delay.
            // Therefore, do it once only when reset data.
            // spcGraphsPaneRsc.getSpcGraphsInfo();
        }
    }

    private class NsharpOperationElementComparator
            implements Comparator<NsharpOperationElement> {

        @Override
        public int compare(NsharpOperationElement o1,
                NsharpOperationElement o2) {

            String s1tok1 = "";
            String s2tok1 = "";
            String o1Desc = o1.getElementDescription();
            StringTokenizer st1 = new StringTokenizer(o1Desc);
            int tkCount1 = st1.countTokens();

            if (tkCount1 >= 1) {
                s1tok1 = st1.nextToken();
            } else {
                return 0;
            }

            String o2Desc = o2.getElementDescription();
            StringTokenizer st2 = new StringTokenizer(o2Desc);
            int tkCount2 = st2.countTokens();

            if (tkCount2 >= 1) {
                s2tok1 = st2.nextToken();
            } else {
                return 0;

            }

            if (s1tok1.compareTo(s2tok1) == 0) {
                return 0;
            } else if (s1tok1.compareTo(s2tok1) < 0) {
                return 1;
            } else if (s1tok1.compareTo(s2tok1) > 0) {
                return -1;
            }
            return 0;
        }
    }

    private int getIndexFromElementList(String targetDescription,
            List<NsharpOperationElement> elemLst) {
        for (NsharpOperationElement sndProp : elemLst) {
            if (sndProp.getElementDescription().equals(targetDescription))
                return elemLst.indexOf(sndProp);

        }
        return -1;
    }

    private void restoreAllSoundingData() {
        for (List<List<NsharpSoundingElementStateProperty>> tlListList : stnTimeSndTable) {
            // add a new element for the new sndType to each existing sndlist of
            // each existing time of each existing stnId
            for (List<NsharpSoundingElementStateProperty> sndtyList : tlListList) {
                for (NsharpSoundingElementStateProperty elem : sndtyList) {
                    if (elem != null)
                        elem.restoreSndLyLstFromBackup();
                }
            }
        }
    }

    private int addElemToElemList(String elemDesc,
            List<NsharpOperationElement> elemList) {
        NsharpOperationElement elem = new NsharpOperationElement(elemDesc,
                NsharpConstants.ActState.ACTIVE);
        elemList.add(elem);
        Collections.sort(elemList, new NsharpOperationElementComparator());
        return elemList.indexOf(elem);
    }

    private void addNewSndToStnTimeSndTable(int sndIndex) {
        for (List<List<NsharpSoundingElementStateProperty>> tlListList : stnTimeSndTable) {
            // add a new element for the new sndType to each existing sndlist of
            // each existing time of each existing stnId
            for (List<NsharpSoundingElementStateProperty> sndtyList : tlListList) {
                sndtyList.add(sndIndex, null);
            }
        }
    }

    private void addNewStnToStnTimeSndTable(int stnIndex) {
        // Add new stnid to outer list of stnTimeSndTable
        List<List<NsharpSoundingElementStateProperty>> listListForNewStn = new ArrayList<>();
        // based on new stn id, add list for each existing time line
        for (int i = 0; i < timeElementList.size(); i++) {
            // based on each time line, add element for each existing sndType
            List<NsharpSoundingElementStateProperty> sndListForTm = new ArrayList<>();
            for (int j = 0; j < sndElementList.size(); j++) {
                sndListForTm.add(null);
            }
            listListForNewStn.add(sndListForTm);
        }
        stnTimeSndTable.add(stnIndex, listListForNewStn);
    }

    private void addNewTimeToStnTimeSndTable(int timeIndex) {
        for (List<List<NsharpSoundingElementStateProperty>> tlListList : stnTimeSndTable) {
            // based on sndTypeElementList
            // create sndlist for the new time line for each existing stnid
            List<NsharpSoundingElementStateProperty> newSndList = new ArrayList<>();
            for (int i = 0; i < sndElementList.size(); i++) {
                newSndList.add(null);
            }
            // add sndlist for the new time line to stn list
            tlListList.add(timeIndex, newSndList);
        }
    }

    private void addElementToTableAndLists(String stnId_timeLine_sndType,
            String stnId, String tmLine, String sndType,
            NsharpStationInfo stnInfo, List<NcSoundingLayer> sndLyLst,
            boolean goodData) {

        NsharpSoundingElementStateProperty newSndPropElem = null;
        int tmIndex = getIndexFromElementList(tmLine, timeElementList);
        int stnIndex = getIndexFromElementList(stnId, stnElementList);
        int sndTpyeIndex = getIndexFromElementList(sndType, sndElementList);
        currentTimeElementListIndex = tmIndex;
        currentStnElementListIndex = stnIndex;
        currentSndElementListIndex = sndTpyeIndex;
        // based on these 3 indexes, we have 8 cases to handle.
        if (tmIndex >= 0 && stnIndex >= 0 && sndTpyeIndex >= 0) {
            // CASE1: All 3 index are good (>=0)
            if (stnTimeSndTable.get(stnIndex).get(tmIndex)
                    .get(sndTpyeIndex) != null) {
                // this sounding element is already loaded
                return;
            } else {
                // replace previously added "null" object with real
                // NsharpSoundingElementStateProperty object
                newSndPropElem = new NsharpSoundingElementStateProperty(
                        stnId_timeLine_sndType, stnId, tmLine, stnInfo,
                        sndLyLst, goodData); // #5929
                stnTimeSndTable.get(stnIndex).get(tmIndex).set(sndTpyeIndex,
                        newSndPropElem);
            }
        } else if (tmIndex >= 0) {
            if (stnIndex >= 0) {
                // CASE2 : tmIndex/stnIndex are good (>=0), sndTpyeIndex is bad
                // (<0), a new snd type
                // add new sndType to sndTypeElementList
                currentSndElementListIndex = addElemToElemList(sndType,
                        sndElementList);
                // Add new snd type to each snd type list of stnTimeSndTable
                addNewSndToStnTimeSndTable(currentSndElementListIndex);
                // replace previously added "null" object with real
                // NsharpSoundingElementStateProperty object
                newSndPropElem = new NsharpSoundingElementStateProperty(
                        stnId_timeLine_sndType, stnId, tmLine, stnInfo,
                        sndLyLst, goodData);
                stnTimeSndTable.get(currentStnElementListIndex)
                        .get(currentTimeElementListIndex)
                        .set(currentSndElementListIndex, newSndPropElem);
            } else {
                if (sndTpyeIndex >= 0) {
                    // CASE3 : tmIndex/sndTpyeIndex are good, stnIndex is bad
                    // (<0), a new stnId
                    // add new stn to stnElementList
                    currentStnElementListIndex = addElemToElemList(stnId,
                            stnElementList);
                    // Add new stnid to outer list of stnTimeSndTable
                    addNewStnToStnTimeSndTable(currentStnElementListIndex);
                    // replace previously added "null" object with real
                    // NsharpSoundingElementStateProperty object
                    newSndPropElem = new NsharpSoundingElementStateProperty(
                            stnId_timeLine_sndType, stnId, tmLine, stnInfo,
                            sndLyLst, goodData);
                    stnTimeSndTable.get(currentStnElementListIndex)
                            .get(currentTimeElementListIndex)
                            .set(currentSndElementListIndex, newSndPropElem);

                } else {
                    // CASE4 : tmIndex is good, stnIndex/sndTpyeIndex are bad
                    // (<0), new stnId and new snd type
                    // add new stn to stnElementList
                    currentStnElementListIndex = addElemToElemList(stnId,
                            stnElementList);
                    // add new sndType to sndTypeElementList
                    currentSndElementListIndex = addElemToElemList(sndType,
                            sndElementList);
                    // Add new snd type to each snd type list of stnTimeSndTable
                    addNewSndToStnTimeSndTable(currentSndElementListIndex);
                    // Add new stnid to outer list of stnTimeSndTable
                    addNewStnToStnTimeSndTable(currentStnElementListIndex);
                    // replace previously added "null" object with real
                    // NsharpSoundingElementStateProperty object
                    newSndPropElem = new NsharpSoundingElementStateProperty(
                            stnId_timeLine_sndType, stnId, tmLine, stnInfo,
                            sndLyLst, goodData);
                    stnTimeSndTable.get(currentStnElementListIndex)
                            .get(currentTimeElementListIndex)
                            .set(currentSndElementListIndex, newSndPropElem);

                }
            }
        } else {
            if (stnIndex >= 0) {
                if (sndTpyeIndex >= 0) {
                    // CASE5 : stnIndex/sndTpyeIndex are good, tmIndex is bad
                    // (<0)
                    // add new time line to timeElementList
                    currentTimeElementListIndex = addElemToElemList(tmLine,
                            timeElementList);
                    // add new time line to StnTimeSndTable
                    addNewTimeToStnTimeSndTable(currentTimeElementListIndex);
                    // replace previously added "null" object with real
                    // NsharpSoundingElementStateProperty object
                    newSndPropElem = new NsharpSoundingElementStateProperty(
                            stnId_timeLine_sndType, stnId, tmLine, stnInfo,
                            sndLyLst, goodData);
                    stnTimeSndTable.get(currentStnElementListIndex)
                            .get(currentTimeElementListIndex)
                            .set(currentSndElementListIndex, newSndPropElem);

                } else {
                    // CASE6 : stnIndex is good, tmIndex/sndTpyeIndex are bad
                    // (<0)
                    // add new time line to timeElementList
                    currentTimeElementListIndex = addElemToElemList(tmLine,
                            timeElementList);
                    // add new sndType to sndTypeElementList
                    currentSndElementListIndex = addElemToElemList(sndType,
                            sndElementList);
                    // Add new snd type to each snd type list of stnTimeSndTable
                    addNewSndToStnTimeSndTable(currentSndElementListIndex);
                    // add new time line to StnTimeSndTable
                    addNewTimeToStnTimeSndTable(currentTimeElementListIndex);
                    // replace previously added "null" object with real
                    // NsharpSoundingElementStateProperty object
                    newSndPropElem = new NsharpSoundingElementStateProperty(
                            stnId_timeLine_sndType, stnId, tmLine, stnInfo,
                            sndLyLst, goodData);
                    stnTimeSndTable.get(currentStnElementListIndex)
                            .get(currentTimeElementListIndex)
                            .set(currentSndElementListIndex, newSndPropElem);

                }

            } else {
                if (sndTpyeIndex >= 0) {
                    // CASE7 : sndTpyeIndex is good, tmIndex/stnIndex are bad
                    // (<0)
                    // add new time line to timeElementList
                    currentTimeElementListIndex = addElemToElemList(tmLine,
                            timeElementList);
                    // add new stn to stnElementList
                    currentStnElementListIndex = addElemToElemList(stnId,
                            stnElementList);
                    // add new time line to StnTimeSndTable
                    addNewTimeToStnTimeSndTable(currentTimeElementListIndex);
                    // Add new stnid to outer list of stnTimeSndTable
                    addNewStnToStnTimeSndTable(currentStnElementListIndex);
                    // replace previously added "null" object with real
                    // NsharpSoundingElementStateProperty object
                    newSndPropElem = new NsharpSoundingElementStateProperty(
                            stnId_timeLine_sndType, stnId, tmLine, stnInfo,
                            sndLyLst, goodData);
                    stnTimeSndTable.get(currentStnElementListIndex)
                            .get(currentTimeElementListIndex)
                            .set(currentSndElementListIndex, newSndPropElem);

                } else {
                    // CASE8 : All are bad (<0)
                    // an element with new time line, new stnId and new sndType
                    // add new time line to timeElementList
                    currentTimeElementListIndex = addElemToElemList(tmLine,
                            timeElementList);
                    // add new stn to stnElementList
                    currentStnElementListIndex = addElemToElemList(stnId,
                            stnElementList);
                    // add new sndType to sndTypeElementList
                    currentSndElementListIndex = addElemToElemList(sndType,
                            sndElementList);

                    // Construct stnTimeSndTable
                    if (stnTimeSndTable.size() > 0) {
                        List<List<NsharpSoundingElementStateProperty>> listListForNewStn = new ArrayList<>();
                        // based on new stn id, add list for each existing time
                        // line
                        for (NsharpOperationElement tmElem : timeElementList) {
                            // based on each time line, add element for each
                            // existing sndType
                            List<NsharpSoundingElementStateProperty> sndlistForTm = new ArrayList<>();
                            for (NsharpOperationElement sndElem : sndElementList) {
                                if (tmLine
                                        .equals(tmElem.getElementDescription())
                                        && sndType.equals(sndElem
                                                .getElementDescription())) {
                                    // only one case falls in this route as only
                                    // one new loaded sounding data
                                    newSndPropElem = new NsharpSoundingElementStateProperty(
                                            stnId_timeLine_sndType, stnId,
                                            tmLine, stnInfo, sndLyLst,
                                            goodData);

                                    sndlistForTm.add(newSndPropElem);
                                } else {
                                    // create for not avail sounding profiles
                                    sndlistForTm.add(null);
                                }
                            }
                            listListForNewStn.add(sndlistForTm);
                        }

                        // Now update stnTimeSndTable by adding "dummy"
                        // NsharpSoundingElementStateProperty to all exiting stn
                        // listList and time list
                        // Note that we have NOT added "listListForNewStn" to
                        // stnTimeSndTable yet.
                        // we have to update current table now
                        for (List<List<NsharpSoundingElementStateProperty>> tlListList : stnTimeSndTable) {
                            // add a new element for the new sndType to each
                            // existing sndlist of each existing time of each
                            // existing stnId
                            for (List<NsharpSoundingElementStateProperty> sndtyList : tlListList) {
                                sndtyList.add(currentSndElementListIndex, null);
                            }
                            // based on sndTypeElementList
                            // add a new sndlist for the new time line for each
                            // existing stnid
                            List<NsharpSoundingElementStateProperty> newSndList = new ArrayList<>();
                            for (int i = 0; i < sndElementList.size(); i++) {
                                {
                                    newSndList.add(null);
                                }
                            }
                            tlListList.add(currentTimeElementListIndex,
                                    newSndList);
                        }
                        // finally, add this new stn list to table
                        stnTimeSndTable.add(currentStnElementListIndex,
                                listListForNewStn);
                    } else {
                        // this is the case, we are adding first element to
                        // stnTimeSndTable
                        // need a new stn time line list to stnTimeSndTable
                        List<NsharpSoundingElementStateProperty> newList = new ArrayList<>();
                        List<List<NsharpSoundingElementStateProperty>> newListList = new ArrayList<>();

                        newSndPropElem = new NsharpSoundingElementStateProperty(
                                stnId_timeLine_sndType, stnId, tmLine, stnInfo,
                                sndLyLst, goodData);
                        newList.add(newSndPropElem);
                        newListList.add(newList);
                        stnTimeSndTable.add(newListList);
                        curSndProfileProp = newSndPropElem;
                        return;
                    }

                }
            }
        }
        setCurSndProfileProp();
    }

    private void setCurSndProfileProp() {
        if (currentTimeElementListIndex < 0
                || currentTimeElementListIndex >= timeElementList.size()
                || currentStnElementListIndex < 0
                || currentStnElementListIndex >= stnElementList.size()
                || currentSndElementListIndex < 0
                || currentSndElementListIndex >= sndElementList.size()) {
            curSndProfileProp = null;
            preSndProfileProp = null;
        } else {
            preSndProfileProp = curSndProfileProp;
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
            if (found)
                break;
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
            if (found)
                break;
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
            if (found)
                break;
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
                    if (setdone)
                        break;
                }
                if (setdone)
                    break;
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
                        sndi++;
                    }
                    if (found)
                        break;
                    timei++;
                }
                if (found)
                    break;
                stni++;
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
                        if (found)
                            break;
                    }
                    if (found)
                        break;
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
        if (timeElementList != null)
            timeElementList.clear();
        if (stnElementList != null)
            stnElementList.clear();
        if (sndElementList != null)
            sndElementList.clear();
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

            if (overlayIsOn) {
                previousSoundingLys = soundingLys;
            } else {
                previousSoundingLys = null;
            }

            if (interpolateIsOn == true) {
                soundingLys = performInterpolation(elem.getSndLyLst());
            } else {
                soundingLys = elem.getSndLyLst();
            }
        } else {
            previousSoundingLys = null;
            soundingLys = null;
        }

    }

    // D2D load data use this route
    public void addRsc(Map<String, List<NcSoundingLayer>> soundMap,
            NsharpStationInfo stnInfo, boolean fromNCP) {
        if (fromNCP) {
            // this is from NCP do nothing now
            this.addRsc(true, soundMap, stnInfo, false);
            // NCP case:
            // Key String format will be like this for NCUAIR
            // KGRI 100616/03(Wed)-NCUAIR
            // and for PFC/Grid sounding will be like this
            // KGRI 100616/03(Wed)V001-GFSSND
        } else {
            // D2D case::::
            // this is from D2D, edit display and time line string to add short
            // day-of-week and
            // also add sounding type to string to solve an issue that data with
            // same stn, same time line but different
            // sounding type will not be loaded.
            // D2D's key string is like this: "KGRI 2010-06-16 03:00:00"
            // will change it to "KGRI 100616/03(Wed)-GFSSND"
            Set<String> dataTimelineSet = soundMap.keySet();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            Calendar cal = Calendar.getInstance();
            Date date;
            Map<String, List<NcSoundingLayer>> newmap = new HashMap<>();
            String sndType = stnInfo.getSndType();
            for (String timeline : dataTimelineSet) {
                String dateStr = timeline.substring(timeline.indexOf(' ') + 1);
                String stnId = timeline.substring(0, timeline.indexOf(' '));
                try {
                    date = df.parse(dateStr);
                    cal.setTime(date);
                    String dayOfWeek = defaultDays[cal
                            .get(Calendar.DAY_OF_WEEK)];
                    String finalTimeStr = String.format(
                            "%4$s %1$ty%1$tm%1$td/%1$tH(%2$s) %3$s", cal,
                            dayOfWeek, sndType, stnId);
                    // put newTimeStr to new map with original value
                    newmap.put(finalTimeStr, soundMap.get(timeline));
                } catch (ParseException e) {
                    statusHandler.handle(Priority.PROBLEM,
                            "d2d addRsc exception:", e);
                    continue;
                }
            }
            // this is from D2D, and it does not want to display new data right
            // away.
            this.addRsc(false, newmap, stnInfo, false);
        }
    }

    private boolean checkDataIntegrity(List<NcSoundingLayer> sndLayers) {
        boolean gooddata = false;
        int numberOfTemp = 0;
        int numberOfGoodDewPt = 0;
        for (NcSoundingLayer layer : sndLayers) {
            if (layer.getTemperature() > -999) {
                numberOfTemp++;
            }
            if (layer.getDewpoint() > -999) {
                numberOfGoodDewPt++;
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
            NsharpStationInfo stnInfo, boolean fromArchive) {

        /*
         * testing code : this code is used frequently for development and do
         * not remove.
         * 
         * Set<String> keysettest = new HashSet<String>(soundMap.keySet()); for
         * (String key : keysettest) { List<NcSoundingLayer> sndLy =
         * soundMap.remove(key); String newkey= key.replace("NAMS", "SSS");
         * //String newkey = key.replace("130925", "150102");
         * soundMap.put(newkey, sndLy); } // //
         * stnInfo.setSndType(stnInfo.getSndType().replace("NCUAIR", // // //
         * "gpduair")); // stnInfo.setSndType(stnInfo.getSndType().replace( //
         * // "NAMS","SSS"));
         * 
         * END testing code
         */
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
        // save current timeline and stn state properties if we are NOT loading
        // new data
        NsharpOperationElement currentTL = null;
        NsharpOperationElement currentStn = null;
        NsharpOperationElement currentSnd = null;
        NsharpSoundingElementStateProperty currentPreSndProfileProp = null;
        if (!displayNewData) {
            currentTL = timeElementList.get(currentTimeElementListIndex);
            currentStn = stnElementList.get(currentStnElementListIndex);
            currentSnd = sndElementList.get(currentSndElementListIndex);
            currentPreSndProfileProp = preSndProfileProp;
        }
        // add new data to table
        Set<String> dataTimelineSet = soundMap.keySet();
        String[] tempTimeLineArr = dataTimelineSet
                .toArray(new String[dataTimelineSet.size()]);
        Arrays.sort(tempTimeLineArr);
        for (int i = 0; i < tempTimeLineArr.length; i++) {
            // based on this KEY string format "KGRI 100616/03(Wed)Vxxx GFSSND"
            String stnId, sndType, timeLine, timeLine_sndType,
                    stnId_timeLine_sndType;
            List<NcSoundingLayer> sndLyLst;

            try {
                stnId_timeLine_sndType = tempTimeLineArr[i].toString();
                if (stnId_timeLine_sndType.equals("N/A")) {
                    continue;
                }
                sndLyLst = soundMap.get(stnId_timeLine_sndType);

                stnId = stnId_timeLine_sndType.substring(0,
                        stnId_timeLine_sndType.indexOf(" "));
                timeLine_sndType = stnId_timeLine_sndType
                        .substring(stnId_timeLine_sndType.indexOf(" ") + 1);
                timeLine = timeLine_sndType.substring(0,
                        timeLine_sndType.indexOf(" "));
                sndType = timeLine_sndType
                        .substring(timeLine_sndType.indexOf(" ") + 1);
            } catch (Exception e) {
                statusHandler.handle(Priority.PROBLEM, "addRsc exception:", e);
                return;
            }
            if (!fromArchive) {
                // For those sounding report with forecast time, e.g. model/pfc
                // sounding
                if (timeLine.contains("V")) {
                    // Chin's NOTE:
                    // Can Not use reference time directly from the stnInfo,
                    // Timestamp refTime = stnInfo.getReftime()
                    // AS there is a "BUG" in Timestamp or Database. In some
                    // cases, Timestamp's "nanos" filelds contains non zero
                    // value of some nanoseconds and cause "hour" value shifted
                    // one hour, and therefore when calling refTime.getTime()
                    // will return unexpected reference time.
                    // So, use the following way to get referrence time.
                    // Based on timeline format "100616/03(Wed)Vxxx" do
                    // the following. to append reference time format of "DD.HH"
                    // at end of sndType.
                    SimpleDateFormat df = new SimpleDateFormat("yyMMdd/HH");
                    String dateStr = timeLine.substring(0, 9);
                    try {
                        Date date = df.parse(dateStr);
                        Calendar cal = Calendar.getInstance();
                        String vStr = timeLine.substring(15, 18);
                        int vNum = Integer.parseInt(vStr);
                        cal.setTimeInMillis(
                                date.getTime() - vNum * 60 * 60 * 1000);
                        String dateOfMonthStr, hourStr;

                        if (cal.get(Calendar.DAY_OF_MONTH) < 10) {
                            dateOfMonthStr = "0"
                                    + cal.get(Calendar.DAY_OF_MONTH);
                        } else {
                            dateOfMonthStr = ""
                                    + cal.get(Calendar.DAY_OF_MONTH);
                        }
                        if (cal.get(Calendar.HOUR_OF_DAY) < 10) {
                            hourStr = "0" + cal.get(Calendar.HOUR_OF_DAY);
                        } else {
                            hourStr = "" + cal.get(Calendar.HOUR_OF_DAY);
                        }
                        sndType = dateOfMonthStr + "." + hourStr + "@"
                                + sndType;
                    } catch (ParseException e) {
                        statusHandler.handle(Priority.PROBLEM,
                                "addRsc exception:", e);
                    }
                }
            }
            // recreate stnId_timeLine_sndType
            stnId_timeLine_sndType = stnId + " " + timeLine + " " + sndType;
            boolean goodData = checkDataIntegrity(sndLyLst);
            addElementToTableAndLists(stnId_timeLine_sndType, stnId, timeLine,
                    sndType, stnInfo, sndLyLst, goodData);
        }
        if (displayNewData) {
            // Set default parcel trace data
            currentParcel = NsharpLibSndglib.PARCELTYPE_MOST_UNSTABLE;
            currentParcelLayerPressure = NsharpLibSndglib.MU_LAYER_PRESS;
            setCurrentSoundingLayerInfo();
            resetData();
        } else {
            // Not display new data. Reset current "parameter"s after adding
            // data to map/lists
            currentStnElementListIndex = stnElementList.indexOf(currentStn);
            currentTimeElementListIndex = timeElementList.indexOf(currentTL);
            currentSndElementListIndex = sndElementList.indexOf(currentSnd);
            preSndProfileProp = currentPreSndProfileProp;
            curSndProfileProp = stnTimeSndTable.get(currentStnElementListIndex)
                    .get(currentTimeElementListIndex)
                    .get(currentSndElementListIndex);
        }

        // set total time line group and stn id list page number
        calculateTimeStnBoxData();

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
            for (int k = 0; k < 2; k++) {
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
        if (win != null)
            currentGraphMode = win.getCurrentGraphMode();

        refreshPane();

    }

    // NCP loads data from DB always uses this route.
    public void addRsc(Map<String, List<NcSoundingLayer>> soundMap,
            NsharpStationInfo stnInfo) {
        // by default, display new data
        this.addRsc(true, soundMap, stnInfo, false);
        return;
    }

    // NCP loads archive data uses this route.
    public void addArchiveRsc(Map<String, List<NcSoundingLayer>> soundMap,
            NsharpStationInfo stnInfo) {
        // by default, display new data
        this.addRsc(true, soundMap, stnInfo, true);
        return;
    }

    public String getPickedStnInfoStr() {
        return pickedStnInfoStr;
    }

    private void handleUserPickNewStationId(int index) {
        currentStnElementListIndex = index;
        if (compareTmIsOn) {
            {
                int colorIndex = NsharpConstants.LINE_COMP1;
                for (NsharpOperationElement elm : timeElementList) {
                    if (elm.getActionState() == NsharpConstants.ActState.INACTIVE)
                        continue;
                    int tmIndex = timeElementList.indexOf(elm);
                    NsharpSoundingElementStateProperty stnTmElm = stnTimeSndTable
                            .get(currentStnElementListIndex).get(tmIndex)
                            .get(currentSndElementListIndex);
                    if (stnTmElm != null) {
                        stnTmElm.setCompColorIndex(colorIndex);
                    }
                    colorIndex++;
                    if (colorIndex > NsharpConstants.LINE_COMP10)
                        colorIndex = NsharpConstants.LINE_COMP1;

                }
            }
        } else if (compareSndIsOn) {
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
        // first to find if it is for change to next page, or change sorting
        int index = ((int) (c.y - dtYOrig)) / (int) lineHeight;

        if (c.y < dtNextPageEnd) {// d2dlite
            // change to next/previous page
            if (totalStnIdPage == 1)
                return;
            if ((c.x - (dtXOrig + dtWidth)) < (dtWidth / 2)) {
                curStnIdPage++;
                if (curStnIdPage > totalStnIdPage)
                    curStnIdPage = 1;
            } else {
                curStnIdPage--;
                if (curStnIdPage <= 0)
                    curStnIdPage = totalStnIdPage;
            }
            return;
        }
        double dIndex;
        // recalculate index for time line
        dIndex = ((c.y - dtNextPageEnd)) / lineHeight
                + (curStnIdPage - 1) * numTimeLinePerPage;

        index = (int) dIndex;
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
                    if (index == currentStnElementListIndex)
                        return;
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
        previousTimeLineStateListIndex = currentTimeElementListIndex;
        currentTimeElementListIndex = index;
        if (compareStnIsOn) {
            {
                int colorIndex = NsharpConstants.LINE_COMP1;
                for (NsharpOperationElement elm : stnElementList) {
                    int stnIndex = stnElementList.indexOf(elm);
                    NsharpSoundingElementStateProperty stnTmElm = stnTimeSndTable
                            .get(stnIndex).get(index)
                            .get(currentSndElementListIndex);
                    if (stnTmElm != null) {
                        stnTmElm.setCompColorIndex(colorIndex);
                    }
                    colorIndex++;
                    if (colorIndex > NsharpConstants.LINE_COMP10) {
                        colorIndex = NsharpConstants.LINE_COMP1;
                    }
                }
            }
        } else if (compareSndIsOn) {
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

        // first to find if it is for change to next/prev page
        int index = ((int) (c.y - dtYOrig)) / ((int) lineHeight);

        if (c.y < dtNextPageEnd) {
            // change to next/previous page
            if (totalTimeLinePage == 1) {
                return;
            }
            if ((c.x - dtXOrig) < (dtWidth / 2)) {
                curTimeLinePage++;
                if (curTimeLinePage > totalTimeLinePage) {
                    curTimeLinePage = 1;
                }
            } else {
                curTimeLinePage--;
                if (curTimeLinePage <= 0) {
                    curTimeLinePage = totalTimeLinePage;
                }
            }
            return;
        }
        double dIndex;
        // recalculate index for time line
        dIndex = (c.y - dtNextPageEnd) / lineHeight
                + (curTimeLinePage - 1) * numTimeLinePerPage;
        index = (int) dIndex;
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
        if (compareTmIsOn) {
            {
                int colorIndex = NsharpConstants.LINE_COMP1;
                for (NsharpOperationElement elm : timeElementList) {
                    if (elm.getActionState() == NsharpConstants.ActState.INACTIVE) {
                        continue;
                    }
                    int tmIndex = timeElementList.indexOf(elm);
                    NsharpSoundingElementStateProperty stnTmElm = stnTimeSndTable
                            .get(currentStnElementListIndex).get(tmIndex)
                            .get(currentSndElementListIndex);
                    if (stnTmElm != null) {
                        stnTmElm.setCompColorIndex(colorIndex);
                    }
                    colorIndex++;
                    if (colorIndex > NsharpConstants.LINE_COMP10) {
                        colorIndex = NsharpConstants.LINE_COMP1;
                    }
                }
            }
        } else if (compareStnIsOn) {
            {
                int colorIndex = NsharpConstants.LINE_COMP1;
                for (NsharpOperationElement elm : stnElementList) {
                    int stnIndex = stnElementList.indexOf(elm);
                    NsharpSoundingElementStateProperty stnTmElm = stnTimeSndTable
                            .get(stnIndex).get(currentTimeElementListIndex)
                            .get(currentSndElementListIndex);
                    if (stnTmElm != null) {
                        stnTmElm.setCompColorIndex(colorIndex);
                    }
                    colorIndex++;
                    if (colorIndex > NsharpConstants.LINE_COMP10) {
                        colorIndex = NsharpConstants.LINE_COMP1;
                    }

                }
            }
        } else if (compareSndIsOn) {
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
        int index = ((int) (c.y - dtYOrig)) / (int) lineHeight;
        if (c.y < dtNextPageEnd) {
            // change to next/previous page
            if (totalSndPage == 1)
                return;
            if ((c.x - dtXOrig) < (dtWidth / 2)) {
                curSndPage++;
                if (curSndPage > totalSndPage) {
                    curSndPage = 1;
                }
            } else {
                curSndPage--;
                if (curSndPage <= 0) {
                    curSndPage = totalSndPage;
                }
            }
            return;
        }
        double dIndex;
        // recalculate index for time line
        dIndex = (c.y - dtNextPageEnd) / lineHeight
                + (curSndPage - 1) * numTimeLinePerPage;
        index = (int) dIndex;
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
        previousTimeLineStateListIndex = currentTimeElementListIndex;
        int counter = 0;
        while (true) {
            currentTimeElementListIndex++;
            currentTimeElementListIndex = currentTimeElementListIndex
                    % this.timeElementList.size();
            counter++;
            if (counter > timeElementList.size()) {
                break;
            }
            if (timeElementList.get(currentTimeElementListIndex)
                    .getActionState() == NsharpConstants.ActState.ACTIVE
                    && stnTimeSndTable.get(currentStnElementListIndex)
                            .get(currentTimeElementListIndex)
                            .get(currentSndElementListIndex) != null) {
                break;// out of while loop
            }

        }
    }

    private void moveTimeLineIndexForward() {
        previousTimeLineStateListIndex = currentTimeElementListIndex;
        int counter = 0;
        while (true) {
            // doing so to make sure we wont get a negative number
            currentTimeElementListIndex = currentTimeElementListIndex
                    + this.timeElementList.size();
            currentTimeElementListIndex--;
            currentTimeElementListIndex = currentTimeElementListIndex
                    % this.timeElementList.size();
            counter++;
            if (counter > timeElementList.size()) {
                break;
            }
            if (timeElementList.get(currentTimeElementListIndex)
                    .getActionState() == NsharpConstants.ActState.ACTIVE
                    && stnTimeSndTable.get(currentStnElementListIndex)
                            .get(currentTimeElementListIndex)
                            .get(currentSndElementListIndex) != null) {
                break;// out of while loop
            }
        }
    }

    private void moveTimeLineIndexCycle() {
        previousTimeLineStateListIndex = currentTimeElementListIndex;
        // Note: direction should only be NEXT or PREVIOUS
        int counter = 0;
        while (true) {
            counter++;
            if (counter > timeElementList.size()) {
                currentTimeElementListIndex = previousTimeLineStateListIndex;
                break;
            }
            if (currentOpDirection == IFrameCoordinator.FrameChangeOperation.NEXT) {
                currentTimeElementListIndex--;
                if (currentTimeElementListIndex <= 0) {
                    // the end of forward direction, change direction to
                    // backward
                    currentOpDirection = IFrameCoordinator.FrameChangeOperation.PREVIOUS;
                    currentTimeElementListIndex = 0;
                }

            } else { // direction is FrameChangeOperation.PREVIOUS
                currentTimeElementListIndex++;
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
                break;// out of while loop
            }
        }
    }

    /*
     * Note: looping only apply to curAggregateTimeLineList NOT stationIdList
     */
    public void setLoopingDataTimeLine(LoopProperties loopProperties) {
        if (this.timeElementList.size() > 0) {
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
            curTimeLinePage = currentTimeElementListIndex / numTimeLinePerPage
                    + 1;
            setCurSndProfileProp();
            setCurrentSoundingLayerInfo();
            resetData();
            refreshPane();
        }

    }

    public enum LoopMode {
        Forward, Backward, Cycle
    };

    private int getElemlistActiveNumber(List<NsharpOperationElement> elemlist) {
        int n = 0;
        for (NsharpOperationElement elem : elemlist) {
            if (elem.getActionState() == NsharpConstants.ActState.ACTIVE) {
                n++;
            }
        }
        return n;
    }

    public void setSteppingTimeLine(
            IFrameCoordinator.FrameChangeOperation operation,
            IFrameCoordinator.FrameChangeMode mode) {
        if (this.timeElementList.size() > 0
                && getElemlistActiveNumber(timeElementList) > 1) {
            int targetIndex = currentTimeElementListIndex;
            // preset index for LAST and FIRST operation
            switch (operation) {
            case LAST: // the future-est time, at top of time line shown. set to
                       // -1, so in while loop, it starts from 0
                targetIndex = -1;//
                break;
            case FIRST: // the oldest time, set to dataTimelineList.length, so
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
                case LAST: // the future-est time, at top of time line shown
                    targetIndex++;
                    break;
                case FIRST: // the oldest time
                    targetIndex--;
                    break;
                case PREVIOUS:
                    targetIndex++;
                    targetIndex = targetIndex % this.timeElementList.size();
                    break;
                case NEXT:
                    // so, we wont get a negative number
                    targetIndex = targetIndex + this.timeElementList.size();
                    targetIndex--;
                    targetIndex = targetIndex % this.timeElementList.size();
                    break;
                default:
                    break;
                }
                counter++;
                if (counter >= timeElementList.size())
                    return; // looped through whole list already, and index back
                            // to original

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
                            for (int i = 0; i < stnElementList.size(); i++) {
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
                        } else {
                            int colorIndex = NsharpConstants.LINE_COMP1;
                            for (NsharpOperationElement elm : stnElementList) {
                                int stnIndex = stnElementList.indexOf(elm);
                                NsharpSoundingElementStateProperty stnTmElm = stnTimeSndTable
                                        .get(stnIndex).get(targetIndex)
                                        .get(currentSndElementListIndex);
                                if (stnTmElm != null) {
                                    stnTmElm.setCompColorIndex(colorIndex);

                                }
                                colorIndex++;
                                if (colorIndex > NsharpConstants.LINE_COMP10)
                                    colorIndex = NsharpConstants.LINE_COMP1;
                            }
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
            previousTimeLineStateListIndex = currentTimeElementListIndex;
            currentTimeElementListIndex = targetIndex;
            curTimeLinePage = currentTimeElementListIndex / numTimeLinePerPage
                    + 1;
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
        if (this.stnElementList.size() > 0
                && getElemlistActiveNumber(stnElementList) > 1) {

            int counter = 0;
            while (true) {
                switch (operation) {
                case NEXT:
                    currentStnElementListIndex = currentStnElementListIndex
                            + this.stnElementList.size();
                    currentStnElementListIndex--;
                    currentStnElementListIndex = currentStnElementListIndex
                            % this.stnElementList.size();
                    break;
                case PREVIOUS:
                    // doing so to make sure we wont get a negative number
                    currentStnElementListIndex++;
                    currentStnElementListIndex = currentStnElementListIndex
                            % this.stnElementList.size();
                    break;
                default:
                    break;

                }
                counter++;
                if (counter >= stnElementList.size())
                    return; // looped through whole list already, and index back
                            // to original
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
                            for (int i = 0; i < timeElementList.size(); i++) {
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
                        } else {
                            int colorIndex = NsharpConstants.LINE_COMP1;
                            for (NsharpOperationElement elm : timeElementList) {
                                if (elm.getActionState() == NsharpConstants.ActState.INACTIVE)
                                    continue;
                                int tmIndex = timeElementList.indexOf(elm);
                                NsharpSoundingElementStateProperty stnTmElm = stnTimeSndTable
                                        .get(currentStnElementListIndex)
                                        .get(tmIndex)
                                        .get(currentSndElementListIndex);
                                if (stnTmElm != null) {
                                    stnTmElm.setCompColorIndex(colorIndex);
                                }
                                colorIndex++;
                                if (colorIndex > NsharpConstants.LINE_COMP10)
                                    colorIndex = NsharpConstants.LINE_COMP1;

                            }
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
                            for (int i = 0; i < sndElementList.size(); i++) {
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
                        } else {
                            int colorIndex = NsharpConstants.LINE_COMP1;
                            for (NsharpOperationElement elm : sndElementList) {
                                if (elm.getActionState() == NsharpConstants.ActState.INACTIVE)
                                    continue;
                                int sndIndex = sndElementList.indexOf(elm);
                                NsharpSoundingElementStateProperty stnTmElm = stnTimeSndTable
                                        .get(currentStnElementListIndex)
                                        .get(currentTimeElementListIndex)
                                        .get(sndIndex);
                                if (stnTmElm != null) {
                                    stnTmElm.setCompColorIndex(colorIndex);
                                }
                                colorIndex++;
                                if (colorIndex > NsharpConstants.LINE_COMP10)
                                    colorIndex = NsharpConstants.LINE_COMP1;
                            }
                        }
                        // no matter we find current snd type for this stn or
                        // not we should get out of here
                        break;
                    } else
                        break;
                }
            }
            curStnIdPage = currentStnElementListIndex / numTimeLinePerPage + 1;

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
        if (this.sndElementList.size() > 0
                && getElemlistActiveNumber(sndElementList) > 1) {

            int counter = 0;
            while (true) {
                switch (operation) {
                case NEXT:
                    currentSndElementListIndex = currentSndElementListIndex
                            + this.sndElementList.size();
                    currentSndElementListIndex--;
                    currentSndElementListIndex = currentSndElementListIndex
                            % this.sndElementList.size();
                    break;
                case PREVIOUS:
                    // doing so to mare sure we wont get a negative number
                    currentSndElementListIndex++;
                    currentSndElementListIndex = currentSndElementListIndex
                            % this.sndElementList.size();
                    break;
                default:
                    break;

                }
                counter++;
                if (counter >= sndElementList.size()) {
                    return; // looped through whole list already, and index back
                            // to original
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
                            for (int i = 0; i < timeElementList.size(); i++) {
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
                        } else {
                            int colorIndex = NsharpConstants.LINE_COMP1;
                            for (NsharpOperationElement elm : timeElementList) {
                                if (elm.getActionState() == NsharpConstants.ActState.INACTIVE)
                                    continue;
                                int tmIndex = timeElementList.indexOf(elm);
                                NsharpSoundingElementStateProperty stnTmElm = stnTimeSndTable
                                        .get(currentStnElementListIndex)
                                        .get(tmIndex)
                                        .get(currentSndElementListIndex);
                                if (stnTmElm != null) {
                                    stnTmElm.setCompColorIndex(colorIndex);
                                }
                                colorIndex++;
                                if (colorIndex > NsharpConstants.LINE_COMP10) {
                                    colorIndex = NsharpConstants.LINE_COMP1;
                                }
                            }
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
                            for (int i = 0; i < stnElementList.size(); i++) {
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
                        } else {
                            int colorIndex = NsharpConstants.LINE_COMP1;
                            for (NsharpOperationElement elm : stnElementList) {
                                int stnIndex = stnElementList.indexOf(elm);
                                NsharpSoundingElementStateProperty stnTmElm = stnTimeSndTable
                                        .get(stnIndex)
                                        .get(currentTimeElementListIndex)
                                        .get(currentSndElementListIndex);
                                if (stnTmElm != null) {
                                    stnTmElm.setCompColorIndex(colorIndex);
                                }
                                colorIndex++;
                                if (colorIndex > NsharpConstants.LINE_COMP10) {
                                    colorIndex = NsharpConstants.LINE_COMP1;
                                }

                            }
                        }
                        // no matter we find current stn or not
                        // we should get out of here
                        break;
                    } else
                        break;
                }
            }
            curStnIdPage = currentSndElementListIndex / numTimeLinePerPage + 1;

            setCurSndProfileProp();
            setCurrentSoundingLayerInfo();
            resetData();

            refreshPane();
        }

    }

    // used for sorting
    public class tempPoint implements Comparable<tempPoint> {
        double diff;

        double temp;

        double pressure;

        int type; // 1= temp, 2 = dewpoint

        tempPoint(double diff, double temp, double pressure, int type) {
            this.diff = diff;
            this.temp = temp;
            this.pressure = pressure;
            this.type = type;
        }

        @Override
        public int compareTo(tempPoint o) {
            if (this.diff >= o.diff)
                return 1;
            else
                return 0;
        }
    }

    /*
     * Return the closest point to the input point on Hodo graph
     */
    public Coordinate getClosestHodoPoint(Coordinate inputC) {
        Coordinate closeptC = new Coordinate(0, 0);
        if (hodoPaneRsc == null)
            return closeptC;
        // picked a impossible big number to start with
        double curSmallestDist = 10000;
        double distance;
        boolean ptFound = false;
        NcSoundingLayer layer;
        //
        // Note: soundingLys list sorted with highest pressure as first element
        //
        for (int i = 0; i < this.soundingLys.size(); i++) {
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
        if (ptFound == false) {
            closeptC.x = closeptC.y = 0;
        } else {
            layer = this.soundingLys.get(hodoEditingSoundingLayerIndex);
            closeptC = WxMath.uvComp((float) layer.getWindSpeed(),
                    (float) layer.getWindDirection());
            closeptC = hodoPaneRsc.getHodoBackground().getWorld().map(closeptC);
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
            if (interpolatedValue == NcSoundingLayer.MISSING)
                // this is not good layer data, usually happened when lowest
                // layer pressure is
                // more than 50, then when interpolate layer for pressure 50,
                // will return unvalid value
                continue;
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
        if (overlayIsOn) {
            previousSoundingLys = soundingLys;
        } else {
            previousSoundingLys = null;
        }

        if (interpolateIsOn == true) {
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
        futurePaneRsc = null;
        for (IRenderableDisplay disp : displayArray) {
            ResourcePair rscP = disp.getDescriptor().getResourceList().get(0);
            NsharpAbstractPaneResource absPaneRsc = (NsharpAbstractPaneResource) rscP
                    .getResource();
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
            } else if (absPaneRsc instanceof NsharpWitoPaneResource
                    && (paneConfigurationName
                            .equals(NsharpConstants.PANE_SPCWS_CFG_STR)
                            || paneConfigurationName
                                    .equals(NsharpConstants.PANE_DEF_CFG_1_STR)
                            || paneConfigurationName.equals(
                                    NsharpConstants.PANE_DEF_CFG_2_STR))) {

                witoPaneRsc = (NsharpWitoPaneResource) absPaneRsc;
                witoPaneRsc.setLinePropertyMap(linePropertyMap);
                witoPaneRsc.setGraphConfigProperty(graphConfigProperty);

            } else if (absPaneRsc instanceof NsharpInsetPaneResource
                    && (paneConfigurationName
                            .equals(NsharpConstants.PANE_SPCWS_CFG_STR)
                            || paneConfigurationName
                                    .equals(NsharpConstants.PANE_DEF_CFG_1_STR)
                            || paneConfigurationName.equals(
                                    NsharpConstants.PANE_DEF_CFG_2_STR))) {

                insetPaneRsc = (NsharpInsetPaneResource) absPaneRsc;
                insetPaneRsc.setLinePropertyMap(linePropertyMap);
                insetPaneRsc.setGraphConfigProperty(graphConfigProperty);

            } else if (absPaneRsc instanceof NsharpSpcGraphsPaneResource
                    && paneConfigurationName
                            .equals(NsharpConstants.PANE_SPCWS_CFG_STR)) {
                spcGraphsPaneRsc = (NsharpSpcGraphsPaneResource) absPaneRsc;
                spcGraphsPaneRsc.setLinePropertyMap(linePropertyMap);
                spcGraphsPaneRsc.setGraphConfigProperty(graphConfigProperty);
            } else if (absPaneRsc instanceof NsharpAbstractPaneResource
                    && paneConfigurationName
                            .equals(NsharpConstants.PANE_SIMPLE_D2D_CFG_STR)) {
                futurePaneRsc = (NsharpAbstractPaneResource) absPaneRsc;
                futurePaneRsc.setLinePropertyMap(linePropertyMap);
                futurePaneRsc.setGraphConfigProperty(graphConfigProperty);
            } else if (absPaneRsc instanceof NsharpTimeStnPaneResource
                    && (paneConfigurationName
                            .equals(NsharpConstants.PANE_SIMPLE_D2D_CFG_STR)
                            || paneConfigurationName
                                    .equals(NsharpConstants.PANE_LITE_D2D_CFG_STR)
                            || paneConfigurationName
                                    .equals(NsharpConstants.PANE_OPC_CFG_STR)
                            || paneConfigurationName
                                    .equals(NsharpConstants.PANE_DEF_CFG_1_STR)
                            || paneConfigurationName.equals(
                                    NsharpConstants.PANE_DEF_CFG_2_STR))) {
                timeStnPaneRsc = (NsharpTimeStnPaneResource) absPaneRsc;
                timeStnPaneRsc.setLinePropertyMap(linePropertyMap);
                timeStnPaneRsc.setGraphConfigProperty(graphConfigProperty);
            }
        }
        this.displayArray = displayArray;
    }

    public void resetRscSoundingData() {
        weatherDataStore.computeWeatherParameters(soundingLys,
                paneConfigurationName);
        if (skewtPaneRsc != null)
            skewtPaneRsc.resetData(soundingLys, previousSoundingLys);
        if (hodoPaneRsc != null)
            hodoPaneRsc.resetData(soundingLys, previousSoundingLys);
        if (witoPaneRsc != null)
            witoPaneRsc.resetData(soundingLys, previousSoundingLys);
        if (dataPaneRsc != null)
            dataPaneRsc.resetData(soundingLys, previousSoundingLys);
        if (insetPaneRsc != null)
            insetPaneRsc.resetData(soundingLys, previousSoundingLys);
        if (spcGraphsPaneRsc != null)
            spcGraphsPaneRsc.resetData(soundingLys, previousSoundingLys);
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
        if (win != null)
            currentGraphMode = win.getCurrentGraphMode();

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

        DateFormatSymbols dfs = new DateFormatSymbols();
        defaultDays = dfs.getShortWeekdays();
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

        if (NsharpParcelDialog.getAccess() != null) {
            NsharpParcelDialog.getAccess().reset();
        }
    }

    public void printHeightMark(NsharpWGraphics world, GC gc)
            throws VizException {
        // print feet scales...
        double vyMax = world.getViewYmax();
        double vyMin = world.getViewYmin();
        double vxMax = world.getViewXmax();
        for (int j = 0; j < NsharpConstants.HEIGHT_LEVEL_FEET.length; j++) {
            float meters = (float) NsharpConstants.feetToMeters
                    .convert(NsharpConstants.HEIGHT_LEVEL_FEET[j]);

            double pressure = NsharpLibBasics.i_pres(soundingLys, meters);
            double y = world.mapY(NsharpWxMath.getSkewTXY(pressure, -50).y);

            gc.drawString(
                    Integer.toString(
                            NsharpConstants.HEIGHT_LEVEL_FEET[j] / 1000),
                    (int) vxMax + 40, (int) y, false);

            gc.drawLine((int) vxMax + 50, (int) y, (int) vxMax + 45, (int) y);
        }
        // print meter scales...
        for (int j = 0; j < NsharpConstants.HEIGHT_LEVEL_METERS.length; j++) {
            int meters = NsharpConstants.HEIGHT_LEVEL_METERS[j];

            double pressure = NsharpLibBasics.i_pres(soundingLys, meters);
            double y = world.mapY(NsharpWxMath.getSkewTXY(pressure, -50).y);

            gc.drawString(Integer.toString(meters / 1000), (int) vxMax + 52,
                    (int) y, false);

            gc.drawLine((int) vxMax + 50, (int) y, (int) vxMax + 55, (int) y);
        }
        // print surface level mark
        double y = world.mapY(NsharpWxMath
                .getSkewTXY(soundingLys.get(0).getPressure(), -50).y);
        gc.drawString(
                "SFC(" + Integer.toString(
                        (int) (soundingLys.get(0).getGeoHeight())) + "m)",
                (int) vxMax + 50, (int) y, false);
        gc.drawLine((int) vxMax + 50, (int) y, (int) vxMax + 55, (int) y);
        // top level mark at 100 mbar
        y = world.mapY(NsharpWxMath.getSkewTXY(100, -50).y);
        float hgt = NsharpLibBasics.i_hght(soundingLys, 100);
        gc.drawString(Float.toString(hgt / 1000F), (int) vxMax + 50, (int) y,
                false);
        gc.drawString("Kft  Km", (int) vxMax + 35, (int) y - 8);
        gc.drawString("MSL", (int) vxMax + 45, (int) y - 15);
        gc.drawLine((int) vxMax + 40, (int) y, (int) vxMax + 60, (int) y);

        gc.drawLine((int) vxMax + 50, (int) vyMin, (int) vxMax + 50,
                (int) vyMax);

    }

    /**
     * Prints the pressure lines number at left side out of skewT bkgd for
     * printing job
     * 
     * @throws VizException
     */
    public void printNsharpPressureLinesNumber(NsharpWGraphics world, GC gc)
            throws VizException {
        String s = null;
        double vxMax = world.getViewXmax();
        double vxMin = world.getViewXmin();
        for (int i = 0; i < NsharpConstants.PRESSURE_MAIN_LEVELS.length; i++) {
            // we only care about pressure for this case, temp is no important
            // when calling getSkewTXY
            Coordinate coor = NsharpWxMath
                    .getSkewTXY(NsharpConstants.PRESSURE_MAIN_LEVELS[i], 0);

            gc.drawLine((int) vxMin, (int) world.mapY(coor.y), (int) vxMax,
                    (int) world.mapY(coor.y));

        }
        for (int i = 0; i < NsharpConstants.PRESSURE_MARK_LEVELS.length; i++) {
            // we only care about pressure for this case, temp is no important
            // when calling getSkewTXY
            Coordinate coor = NsharpWxMath
                    .getSkewTXY(NsharpConstants.PRESSURE_MARK_LEVELS[i], 0);

            gc.drawLine((int) vxMin, (int) world.mapY(coor.y), (int) vxMin + 10,
                    (int) world.mapY(coor.y));

        }
        for (int i = 0; i < NsharpConstants.PRESSURE_NUMBERING_LEVELS.length; i++) {
            s = NsharpConstants.pressFormat
                    .format(NsharpConstants.PRESSURE_NUMBERING_LEVELS[i]);
            // we only care about pressure for this case, temp is no important
            // when calling getSkewTXY
            Coordinate coor = NsharpWxMath.getSkewTXY(
                    NsharpConstants.PRESSURE_NUMBERING_LEVELS[i], 0);

            gc.drawString(s, (int) vxMin - 20, (int) world.mapY(coor.y), false);
        }
    }

    /**
     * Print the temp number at bottom out of skewT bkgd for printing job
     * 
     * @throws VizException
     */
    public void printNsharpTempNumber(NsharpWGraphics world, GC gc)
            throws VizException {
        for (int i = 40; i > -50; i -= 10) {
            Coordinate coorStart = NsharpWxMath.getSkewTXY(1050, i);
            double startX = world.mapX(coorStart.x);
            double startY = world.mapY(coorStart.y);

            gc.drawString(Integer.toString(i), (int) startX, (int) startY + 5,
                    false);
        }
        for (int i = -60; i > -120; i -= 10) {
            Coordinate coorEnd = NsharpWxMath.getSkewTXY(100, i);
            double endX = world.mapX(coorEnd.x);
            double endY = world.mapY(coorEnd.y);

            gc.drawString(Integer.toString(i), (int) endX, (int) endY - 10,
                    false);
        }
    }

    /**
     * 
     * Print Wind barb for printing job This function followed algorithm in
     * plot_barbs (void) at xwvid1.c to choose wind bulb for drawing around
     * every 400m
     * 
     */
    public void printNsharpWind(NsharpWGraphics world, GC gc)
            throws VizException {
        ArrayList<List<LineStroke>> windList = new ArrayList<>();

        double windX = world.getViewXmax() + 6 * BARB_LENGTH;
        float lastHeight = -999;
        double windY;
        for (NcSoundingLayer layer : soundingLys) {
            float pressure = layer.getPressure();
            float spd = layer.getWindSpeed();
            float dir = layer.getWindDirection();

            if (pressure < 100) {
                continue;
            }

            if ((layer.getGeoHeight() - lastHeight) < 400) {

                continue;
            }

            // Get the vertical ordinate.
            windY = world.mapY(NsharpWxMath.getSkewTXY(pressure, 0).y);

            List<LineStroke> barb = WindBarbFactory
                    .getWindGraphics((double) (spd), (double) dir);
            if (barb != null) {
                WindBarbFactory.scaleBarb(barb, -7);
                WindBarbFactory.translateBarb(barb, windX, windY);
                windList.add(barb);
            }

            lastHeight = layer.getGeoHeight();
        }
        Coordinate pt1 = new Coordinate(0, 0), pt2;
        for (List<LineStroke> barb : windList) {
            for (LineStroke stroke : barb) {
                // stroke render: rewrite stroke.render() for our printing
                // purpose
                if (stroke.getType() == "M") {
                    pt1 = stroke.getPoint();
                    // change X coordinate by mirroring x coordinate at windX
                    // axis. AS we scaleBarb with -5 time.
                    // It is easier to mirror at x-axis for this case.
                    pt1.x = windX - (pt1.x - windX);
                } else if (stroke.getType() == "D") {
                    pt2 = stroke.getPoint();
                    pt2.x = windX - (pt2.x - windX);
                    gc.drawLine((int) pt1.x, (int) pt1.y, (int) pt2.x,
                            (int) pt2.y);
                }
            }
        }
        gc.drawLine((int) windX,
                (int) world.mapY(NsharpWxMath.getSkewTXY(100, 0).y),
                (int) windX,
                (int) world.mapY(NsharpWxMath.getSkewTXY(1000, 0).y));
    }

    /**
     * 
     * Print the wetbulb trace curve
     * 
     * @throws VizException
     */
    public void printNsharpWetbulbTraceCurve(NsharpWGraphics world, GC gc)
            throws VizException {
        if ((soundingLys == null) || (soundingLys.size() == 0))
            return;
        float t1;

        Coordinate c2 = null;
        Coordinate c1;
        // print trace
        for (NcSoundingLayer layer : this.soundingLys) {
            if (layer.getDewpoint() > -200) {
                t1 = NsharpLibThermo.wetbulb(layer.getPressure(),
                        layer.getTemperature(), layer.getDewpoint());

                c1 = NsharpWxMath.getSkewTXY(layer.getPressure(), t1);
                c1.x = world.mapX(c1.x);
                c1.y = world.mapY(c1.y);
                if (c2 != null) {
                    gc.drawLine((int) c1.x, (int) c1.y, (int) c2.x, (int) c2.y);
                }
                c2 = c1;
            }
        }

    }

    public void printNsharpParcelTraceCurve(NsharpWGraphics world, GC gc)
            throws VizException {
        if (soundingLys.size() > 0) {

            Parcel parcel = weatherDataStore.getParcelMap().get(currentParcel);
            if (parcel == null) {
                return;
            }

            float vtemp = NsharpLibThermo.virtemp(parcel.getLplpres(),
                    parcel.getLpltemp(), parcel.getLpldwpt());
            Coordinate c1 = NsharpWxMath.getSkewTXY(parcel.getLplpres(), vtemp);
            c1.x = world.mapX(c1.x);
            c1.y = world.mapY(c1.y);
            LayerParameters dryLiftLayer = NsharpLibThermo.drylift(
                    parcel.getLplpres(), parcel.getLpltemp(),
                    parcel.getLpldwpt());
            vtemp = NsharpLibThermo.virtemp(dryLiftLayer.getPressure(),
                    dryLiftLayer.getTemperature(),
                    dryLiftLayer.getTemperature());
            Coordinate c2 = NsharpWxMath.getSkewTXY(dryLiftLayer.getPressure(),
                    vtemp);
            c2.x = world.mapX(c2.x);
            c2.y = world.mapY(c2.y);

            gc.drawLine((int) c1.x, (int) c1.y, (int) c2.x, (int) c2.y);
            c1 = c2;

            float t3;
            for (float i = dryLiftLayer.getPressure() - 50; i >= 100; i = i
                    - 50) {
                t3 = NsharpLibThermo.wetlift(dryLiftLayer.getPressure(),
                        dryLiftLayer.getTemperature(), i);
                vtemp = NsharpLibThermo.virtemp(i, t3, t3);
                c2 = NsharpWxMath.getSkewTXY(i, vtemp);
                c2.x = world.mapX(c2.x);
                c2.y = world.mapY(c2.y);

                gc.drawLine((int) c1.x, (int) c1.y, (int) c2.x, (int) c2.y);
                c1 = c2;
            }

            t3 = NsharpLibThermo.wetlift(dryLiftLayer.getPressure(),
                    dryLiftLayer.getTemperature(), 100);
            vtemp = NsharpLibThermo.virtemp(100, t3, t3);
            c2 = NsharpWxMath.getSkewTXY(100, vtemp);
            c2.x = world.mapX(c2.x);
            c2.y = world.mapY(c2.y);

            gc.drawLine((int) c1.x, (int) c1.y, (int) c2.x, (int) c2.y);

        }
    }

    /**
     * 
     * Print the temperature curve when during overlap or compare mode
     * 
     * @throws VizException
     */

    public void printNsharpPressureTempCurve(NsharpWGraphics world, int type,
            GC gc, List<NcSoundingLayer> soundingLys) throws VizException {
        if ((soundingLys == null) || (soundingLys.size() == 0))
            return;

        double maxPressure = NsharpWxMath
                .reverseSkewTXY(new Coordinate(0, world.getWorldYmax())).y;
        double minPressure = NsharpWxMath
                .reverseSkewTXY(new Coordinate(0, world.getWorldYmin())).y;
        Coordinate c0 = null;
        for (NcSoundingLayer layer : soundingLys) {
            double t;
            if (type == TEMP_TYPE)
                t = layer.getTemperature();
            else if (type == DEWPOINT_TYPE)
                t = layer.getDewpoint();
            else
                break;
            double pressure = layer.getPressure();
            if (t != INVALID_DATA && pressure >= minPressure
                    && pressure <= maxPressure) {

                Coordinate c1 = NsharpWxMath.getSkewTXY(pressure, t);

                c1.x = world.mapX(c1.x);
                c1.y = world.mapY(c1.y);
                if (c0 != null) {
                    gc.drawLine((int) c0.x, (int) c0.y, (int) c1.x, (int) c1.y);
                }
                c0 = c1;
            }
        }

    }

    /**
     * 
     * Print the HODO
     * 
     * 
     * @throws VizException
     */

    public void printNsharpHodoWind(NsharpWGraphics world, GC gc,
            List<NcSoundingLayer> soundingLays) throws VizException {
        Coordinate c0 = null;
        Coordinate c1;
        for (NcSoundingLayer layer : soundingLays) {
            if (layer.getPressure() < 100 || layer.getWindSpeed() < 0)
                continue;
            float wspd = layer.getWindSpeed();
            float wdir = layer.getWindDirection();
            c1 = WxMath.uvComp(wspd, wdir);
            if (c0 != null) {
                gc.setLineWidth(1);
                gc.drawLine((int) world.mapX(c0.x), (int) world.mapY(c0.y),
                        (int) world.mapX(c1.x), (int) world.mapY(c1.y));
            }
            c0 = c1;
        }

    }

    public boolean isPlotInteractiveTemp() {
        return plotInteractiveTemp;
    }

    public void setPlotInteractiveTemp(boolean plotInteractiveTemp) {
        this.plotInteractiveTemp = plotInteractiveTemp;
        if (skewtPaneRsc != null)
            skewtPaneRsc.setPlotInteractiveTemp(plotInteractiveTemp);
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
        if (hodoPaneRsc == null)
            return;
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
                if (witoPaneRsc != null)
                    witoPaneRsc.createAllWireFrameShapes();
                if (insetPaneRsc != null)
                    insetPaneRsc.createInsetWireFrameShapes();
                if (skewtPaneRsc != null)
                    skewtPaneRsc.createRscWireFrameShapes();
            }
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM,
                    "setInteractiveHodoPointCoordinate exception:", e);
        }
    }

    public void applyMovingTempLine() {
        if (skewtPaneRsc == null)
            return;
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
        if (skewtPaneRsc == null)
            return;
        Coordinate inC = NsharpWxMath.reverseSkewTXY(
                skewtPaneRsc.getWorld().unMap(interactiveTempPointCoordinate));
        double inTemp = inC.x;
        currentSoundingLayerIndex = skewtPaneRsc.getCurrentSoundingLayerIndex();
        NcSoundingLayer layer = this.soundingLys.get(currentSoundingLayerIndex);
        currentTempCurveType = skewtPaneRsc.getCurrentTempCurveType();
        if (currentTempCurveType == TEMP_TYPE) {
            if (inTemp < layer.getDewpoint())
                // temp can not be lower than dew point
                layer.setTemperature(layer.getDewpoint());
            else
                layer.setTemperature((float) inTemp);
        } else {
            if (inTemp > layer.getTemperature())
                // dew point can not be higher than temp
                layer.setDewpoint(layer.getTemperature());
            else
                layer.setDewpoint((float) inTemp);
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
        if (layerIndex < 0 || layerIndex >= soundingLys.size())
            return;
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
        if (dataPaneRsc != null)
            dataPaneRsc.setSoundingLys(soundingLys);

        if (spcGraphsPaneRsc != null && (goodData)) {// #5929
            // Chin: SPC graphs performance concern, as it need to call get
            // info functions from bigSharo.so and cause long delay.
            // Therefore, do it once only when reset data.
            // spcGraphsPaneRsc.getSpcGraphsInfo();
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
        if (dataPaneRsc != null)
            dataPaneRsc.setSoundingLys(soundingLys);
        if (spcGraphsPaneRsc != null && (goodData)) {
            // Chin: SPC graphs performance concern, as it need to call get
            // info functions from bigSharo.so and cause long delay.
            // Therefore, do it once only when reset data.
            // spcGraphsPaneRsc.getSpcGraphsInfo();
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
        if (timeStnPaneRsc != null)
            timeStnPaneRsc.setLinePropertyMap(linePropertyMap);
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
        if (dataPaneRsc != null)
            dataPaneRsc.setPageDisplayOrderNumberArray(
                    pageDisplayOrderNumberArray,
                    dataPageProperty.getNumberPagePerDisplay());
    }

    public void handleTimeLineActConfig(List<String> tlList,
            NsharpConstants.ActState actSt) {
        for (String tlStr : tlList) {
            for (NsharpOperationElement tl : timeElementList) {
                if (tlStr.equals(tl.getElementDescription())) {
                    tl.setActionState(actSt);
                    break;
                }
            }
        }
    }

    public void handleStationActConfig(List<String> stnList,
            NsharpConstants.ActState actSt) {
        for (String tlStr : stnList) {
            for (NsharpOperationElement stn : stnElementList) {
                if (tlStr.equals(stn.getElementDescription())) {
                    stn.setActionState(actSt);
                    break;
                }
            }
        }
    }

    public void handleSndTypeActConfig(List<String> sndTypeList,
            NsharpConstants.ActState actSt) {
        for (String tlStr : sndTypeList) {
            for (NsharpOperationElement sndType : sndElementList) {
                if (tlStr.equals(sndType.getElementDescription())) {
                    sndType.setActionState(actSt);
                    break;
                }
            }
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

    public int getCurTimeLinePage() {
        return curTimeLinePage;
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

    public int getCurStnIdPage() {
        return curStnIdPage;
    }

    public int getCurSndPage() {
        return curSndPage;
    }

    public HashMap<String, RGB> getElementColorMap() {
        return elementColorMap;
    }

    public int getTotalTimeLinePage() {
        return totalTimeLinePage;
    }

    public int getTotalStnIdPage() {
        return totalStnIdPage;
    }

    public int getTotalSndPage() {
        return totalSndPage;
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

    public void setTimeStnBoxData(int cnYOrig, int dtNextPage_end, int dtYOrig,
            int dtXOrig, int dtWidth, double charHeight, double lineHeight,
            int numTimeLinePerPage) {
        this.charHeight = charHeight;
        this.lineHeight = lineHeight;
        this.dtYOrig = dtYOrig;
        this.dtXOrig = dtXOrig;
        this.dtWidth = dtWidth;
        this.dtNextPageEnd = dtNextPage_end;
        this.numTimeLinePerPage = numTimeLinePerPage;
        calculateTimeStnBoxData();
    }

    public void refreshPane() {
        for (int i = 0; i < displayArray.length; i++) {
            displayArray[i].refresh();
        }
    }

    public String getPaneConfigurationName() {
        return paneConfigurationName;
    }

    public void setPaneConfigurationName(String paneConfigName) {
        if (this.paneConfigurationName.equals(paneConfigName))
            return;

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

    private void calculateTimeStnBoxData() {
        // set total time line group and stn id list page number
        totalTimeLinePage = timeElementList.size() / numTimeLinePerPage;
        if (timeElementList.size() % numTimeLinePerPage != 0)
            totalTimeLinePage = totalTimeLinePage + 1;
        curTimeLinePage = currentTimeElementListIndex / numTimeLinePerPage + 1;
        totalStnIdPage = stnElementList.size() / numTimeLinePerPage;
        if (stnElementList.size() % numTimeLinePerPage != 0)
            totalStnIdPage++;
        curStnIdPage = currentStnElementListIndex / numTimeLinePerPage + 1;
        totalSndPage = sndElementList.size() / numTimeLinePerPage;
        if (sndElementList.size() % numTimeLinePerPage != 0)
            totalSndPage++;
        curSndPage = currentSndElementListIndex / numTimeLinePerPage + 1;
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
        previousTimeLineStateListIndex = currentTimeElementListIndex;
        currentStnElementListIndex = tempStni;
        currentTimeElementListIndex = tempTmi;
        currentSndElementListIndex = tempSndi;
        setCurSndProfileProp();
        curTimeLinePage = currentTimeElementListIndex / numTimeLinePerPage + 1;
        curSndPage = currentSndElementListIndex / numTimeLinePerPage + 1;
        curStnIdPage = currentStnElementListIndex / numTimeLinePerPage + 1;
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

}
