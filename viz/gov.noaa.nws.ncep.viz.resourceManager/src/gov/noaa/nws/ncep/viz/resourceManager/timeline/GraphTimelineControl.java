/*
 * Timeline
 * 
 * Date created 03 MARCH 2010
 *
 * This code has been developed by the SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.viz.resourceManager.timeline;

import gov.noaa.nws.ncep.viz.common.ui.CalendarSelectDialog;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData.TimelineGenMethod;
import gov.noaa.nws.ncep.viz.resources.time_match.GraphTimelineUtil;
import gov.noaa.nws.ncep.viz.resources.time_match.NCTimeMatcher;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

import com.raytheon.uf.common.time.DataTime;

/**
 * This class is an override on the TimelineControl for display graph time line.
 * The main difference are removing numFrames, and numSkip, and adding
 * graphRange and hourSnap.
 * 
 * For non-graph, we have many frames and each data is in a frame. But for
 * graph, we only have one frame and many data in the frame.
 * 
 * The numFrames and frameTimes are basic criteria for timeline control. we
 * change a concept here. We put data in frameTimes, so the timeline and
 * timeMatcher classes still work, but there is only one frame, not "frameTimes"
 * frames.
 * 
 * Timeline: A graphical view of available data times, that allows users to
 * select any or all data times from the available list.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 04/28/2014     #1131     qzhou       Initial (removed numFrames, numSkip, added graphRange, hourSnap)
 * 05/14/2014     #1131     qzhou       Add snapAvailtimes to append data at the end to fill the snap period.
 *                                      Change MourseUp function to handle snap.
 *                                      Add functions to get position from time and vice versa
 *                                      Modified updateTimeline, updateSelectedTimes, calculateSlider, calculateAvailableBoxes
 * 06/10/2014     #1136     qzhou       Modified getSelectedTimes and updateTimeline
 * 08/13/2014     R4079     sgurung     Added code changes from TimeLineControl (related to TTR1032)
 * 
 * </pre>
 * 
 * @author qzhou
 * @version 1
 */
public class GraphTimelineControl extends TimelineControl {

    private final String[] noResourcesList = { "                       None Available               " };

    private final String manualTimelineStr = "                       Manual Timeline              ";

    // private HashMap<String,
    // ArrayList<AbstractNatlCntrsRequestableResourceData>> availDomResourcesMap
    // = null;

    // private AbstractNatlCntrsRequestableResourceData domRscData = null;

    // private Combo dom_rsc_combo = null;

    // private NCTimeMatcher timeMatcher = null;

    // public interface IDominantResourceChangedListener {
    // public void dominantResourceChanged(
    // AbstractNatlCntrsRequestableResourceData newDomRsc);
    // }
    //
    // private Set<IDominantResourceChangedListener>
    // dominantResourceChangedListeners = new
    // HashSet<IDominantResourceChangedListener>();

    private enum MODE {
        MOVE_LEFT, MOVE_RIGHT, MOVE_ALL
    };

    private final float DATE_LINE = 0.25f;

    private final float TIME_LINE = 0.60f;

    private final int MARGIN = 15;

    private final int MARKER_WIDTH = 5;

    private final int MARKER_HEIGHT = 10;

    private final int SLIDER = 10;

    private final int TICK_SMALL = 5;

    private final int TICK_LARGE = 7;

    private final int MAX_DATES = 10;

    // if this is not null then the message is displayed in place of
    // a timeline. This should only be set when no times are available.
    private String timelineStateMessage = null;

    private Canvas canvas;

    private Combo graphRangeCombo;

    private Combo hourSnapCombo;

    private Spinner timeRangeDaysSpnr;

    private Spinner timeRangeHrsSpnr;

    private Combo frameIntervalCombo;

    private Combo refTimeCombo;

    private Label refTimeLbl;

    private int timeRangeHrs = 0; //

    private String availFrameIntervalStrings[] = { "Data", "1 min", "2 mins",
            "5 mins", "10 mins", "15 mins", "20 mins", "30 mins", "1 hr",
            "90 mins", "2 hrs", "3 hrs", "6 hrs", "12 hrs", "24 hrs" };

    private int availFrameIntervalMins[] = { -1, 1, 2, 5, 10, 15, 20, 30, 60,
            90, 120, 180, 360, 720, 1440 };

    private String availGraphRangeStrings[] = { "1 hr", "2 hrs", "3 hrs",
            "6 hrs", "12 hrs", "24 hrs", "3 days", "7 days", "30 days" };

    private int availGraphRangeHrs[] = { 1, 2, 3, 6, 12, 24, 72, 168, 720 };

    // extend refTime to next snap point
    private String availHourSnapStrings[] = { "0", "1", "2", "3", "6", "12",
            "24" };

    private int availHourSnapHrs[] = { 0, 1, 2, 3, 6, 12, 24 };

    // keep this in order since code is referencing the index into this list.
    private String refTimeSelectionOptions[] = { "Current", "Latest ",
            "Calendar ..." };

    private Font canvasFont;

    private Cursor pointerCursor;

    private Cursor resizeCursor;

    private Cursor grabCursor;

    private Rectangle slider = null;

    private int sliderMin, sliderMax;

    // private TimelineData timeData;

    private Map<Rectangle, Calendar> availableTimes;

    private Map<Calendar, Integer> timeLocations;

    private boolean hasDifferentMinutes = false;

    private List<Calendar> days;

    private List<Integer> dayLocation;

    private Color canvasColor, availableColor, selectedColor;

    private Shell shell;

    private int newGraphRange = 0;

    public GraphTimelineControl(Composite parent) {

        super(parent, "Graph");
        shell = parent.getShell();

        // timeMatcher = new NCTimeMatcher();
        availDomResourcesMap = new HashMap<String, ArrayList<AbstractNatlCntrsRequestableResourceData>>();

        Composite top_form = this;
        GridData gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        top_form.setLayoutData(gd);

        top_form.setLayout(new FormLayout());

        dom_rsc_combo = new Combo(top_form, SWT.DROP_DOWN | SWT.READ_ONLY);
        FormData fd = new FormData();
        fd.width = 330;
        fd.top = new FormAttachment(0, 0);
        fd.left = new FormAttachment(30, 0);

        dom_rsc_combo.setLayoutData(fd);

        Label dom_rsc_lbl = new Label(top_form, SWT.NONE);
        dom_rsc_lbl.setText("Dominant Resource");
        fd = new FormData();
        fd.top = new FormAttachment(dom_rsc_combo, 2, SWT.TOP);
        fd.right = new FormAttachment(dom_rsc_combo, -10, SWT.LEFT);
        dom_rsc_lbl.setLayoutData(fd);

        // if changing the dominant resource then change the timeline
        dom_rsc_combo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {

                selectDominantResource();
            }
        });

        dom_rsc_combo.setItems(noResourcesList);
        dom_rsc_combo.select(0);

        canvasColor = new Color(getDisplay(), 255, 255, 255);
        availableColor = new Color(getDisplay(), 0, 0, 255);
        selectedColor = new Color(getDisplay(), 255, 0, 0);
        canvasFont = new Font(getDisplay(), "Times", 11, SWT.BOLD);
        pointerCursor = new Cursor(getDisplay(), SWT.CURSOR_ARROW);
        resizeCursor = new Cursor(getDisplay(), SWT.CURSOR_SIZEW);
        grabCursor = new Cursor(getDisplay(), SWT.CURSOR_HAND);

        createWidgets(top_form);

        addSpinnerListeners();

        updateTimeline(new NCTimeMatcher());
    }

    public void addDominantResourceChangedListener(
            IDominantResourceChangedListener lstnr) {
        dominantResourceChangedListeners.add(lstnr);
    }

    public boolean removeAddDominantResourceChangedListener(
            IDominantResourceChangedListener lstnr) {
        return dominantResourceChangedListeners.remove(lstnr);
    }

    // set the timeMatcher and update the widgets for it.
    public boolean setTimeMatcher(NCTimeMatcher tm) {
        // this can happen if the user brings up the RBD Manager (usually with
        // the spacebar) before there is an editor with a timeline
        if (tm == null) {
            timeMatcher = new NCTimeMatcher();
            return false;
        }

        timeMatcher = tm;

        /*
         * loadTimes will adjust time line with "false" - it updates available
         * times from DB but keeps selected frame times intact. If "true" is
         * used, the time line will be completely rebuilt - available times are
         * updated, the selected frames times are cleared and then re-built with
         * "numFrames" and "skip" factor.
         */
        timeMatcher.loadTimes(false);

        if (timeMatcher.getDominantResourceName() != null) {
            for (int i = 0; i < dom_rsc_combo.getItemCount(); i++) {
                if (dom_rsc_combo.getItem(i).equals(
                        timeMatcher.getDominantResourceName().toString())) {
                    dom_rsc_combo.select(i);
                }
            }
        } else {
            dom_rsc_combo.select(0); // 'None Available"
        }

        updateTimeline(timeMatcher);

        // set the domRscData from the combo and
        if (getDominantResource() == null) {
            return false;
        }

        return true;
    }

    public NCTimeMatcher getTimeMatcher() {
        timeMatcher.setFrameTimes(toDataTimes(getSelectedTimes()));
        return timeMatcher;
    }

    private ArrayList<DataTime> toDataTimes(List<Calendar> times) {
        ArrayList<DataTime> dlist = new ArrayList<DataTime>();
        for (Calendar cal : times) {
            DataTime dtime = new DataTime(cal);
            dlist.add(dtime);
        }
        return dlist;
    }

    // set domRscData from the combo selection
    public AbstractNatlCntrsRequestableResourceData getDominantResource() {
        // look up this resource in the map of stored avail
        // dominant resources and use the first in the list
        // as the dominant resource.
        String seldRscName = dom_rsc_combo.getText();

        if (seldRscName.equals(noResourcesList[0])) {
            domRscData = null;
            return domRscData;
        }
        // if nothing is selected then return null.
        else if (seldRscName.isEmpty()) {
            domRscData = null;
            return domRscData;
        } else {
            ArrayList<AbstractNatlCntrsRequestableResourceData> seldRscsList = availDomResourcesMap
                    .get(seldRscName.toString());

            if (seldRscsList == null || seldRscsList.isEmpty()) {
                System.out.println("Sanity Check: seld Rsc " + seldRscName
                        + " not found.");
                domRscData = null;
                return domRscData;
            }

            // TODO : There is a small hole in the design here in the case where
            // Manual Timeline is selected for the event type resources and
            // there are
            // more than one event resource available. The dominant will be the
            // first
            // in the list. But the since the user will need to manually select
            // a frame
            // interval the only real problem is that the latest data will
            // reference
            // this resource. If the user needed the latest data of the second
            // or other
            // event resource then they wouldn't be able to unless they removed
            // the
            // resources and re-selected in a new order.
            //
            domRscData = seldRscsList.get(0);
            return domRscData;
        }
    }

    public void selectManualTimeline() {

    }

    // set this dominant Resource in the combo and select it as the dominant
    //
    public boolean setDominantResource(
            AbstractNatlCntrsRequestableResourceData domRsc) {
        // loop thru the combo items
        for (String comboEntry : dom_rsc_combo.getItems()) {
            if (!comboEntry.equals(noResourcesList[0])) {
                if (comboEntry.equals(manualTimelineStr)) {
                }

                String mapKey = comboEntry;
                ArrayList<AbstractNatlCntrsRequestableResourceData> seldRscsList = availDomResourcesMap
                        .get(mapKey);

                if (seldRscsList == null || seldRscsList.isEmpty()) {
                    domRscData = null;
                    return false;
                }
                for (AbstractNatlCntrsRequestableResourceData rsc : seldRscsList) {
                    if (rsc.getResourceName().equals(domRsc.getResourceName())) {
                        domRscData = domRsc;
                        dom_rsc_combo.setText(comboEntry);
                        break;
                    }
                }
            }
        }

        selectDominantResource();

        return true;
    }

    // set the numFrames from the domRsc to the timeMatcher.
    public void selectDominantResource() {

        // get the dominant resource from the selected combo item
        // and set it in the timeMatcher.
        if (getDominantResource() == null) {
            timeMatcher.setDominantResourceData(null);
        } else {
            timeMatcher.setDominantResourceData(domRscData);
        }

        timeMatcher.updateFromDominantResource();

        updateTimeline(timeMatcher);

        // call all the listeners (used to set the auto update button)
        for (IDominantResourceChangedListener lstnr : dominantResourceChangedListeners) {
            lstnr.dominantResourceChanged(domRscData);
        }
    }

    public void addAvailDomResource(AbstractNatlCntrsRequestableResourceData rsc) {

        String mapKey; // the key for the map and the entry in the combo box

        // for 'event' resources which requires a manual timeline then
        // add "Manual" as a selection option
        //
        if (rsc.getTimelineGenMethod() == TimelineGenMethod.USE_MANUAL_TIMELINE) {
            mapKey = manualTimelineStr;
        } else {
            mapKey = rsc.getResourceName().toString();
        }

        ArrayList<AbstractNatlCntrsRequestableResourceData> rscList = availDomResourcesMap
                .get(mapKey);

        // if no resource by this name then create an
        // entry in the map.
        if (rscList == null) {
            rscList = new ArrayList<AbstractNatlCntrsRequestableResourceData>();
            availDomResourcesMap.put(mapKey, rscList);
        }

        // If there is no entry in the map then add the resource and update the
        // combo
        if (rscList.isEmpty()) {
            rscList.add(rsc);

            dom_rsc_combo.add(mapKey);

            // remove none from the list
            if (dom_rsc_combo.getItem(0).equals(noResourcesList[0])) {
                if (dom_rsc_combo.getSelectionIndex() == 0) {
                    dom_rsc_combo.remove(0);

                    dom_rsc_combo.deselectAll();
                    domRscData = null;
                    // dom_rsc_combo.select(0);
                    // selectDominantResource();
                } else {
                    dom_rsc_combo.remove(0);
                }
            }
        } else {
            // otherwise there are already other resources with the same name so
            // just
            // add this one. (We need to do this in case one of the other
            // resources is
            // removed then this one still needs to be made available as a
            // dominant resource)
            rscList.add(rsc);
        }
    }

    // TODO : implement ; this is complicated by the fact that there may be
    // another
    // resource in another pane to 'replace' this one so we will need to save
    // all
    // the possible resources and only present the unique ones to the user.
    //
    public boolean removeAvailDomResource(
            AbstractNatlCntrsRequestableResourceData rsc) {

        String mapKey;

        if (rsc.getTimelineGenMethod() == TimelineGenMethod.USE_MANUAL_TIMELINE) {
            mapKey = manualTimelineStr;
        } else {
            mapKey = rsc.getResourceName().toString();
        }

        ArrayList<AbstractNatlCntrsRequestableResourceData> rscList = availDomResourcesMap
                .get(mapKey);

        if (rscList == null || rscList.isEmpty()) {
            System.out.println("removeAvailDomResource: " + mapKey
                    + " is not in the availDomResourcesMap??");
            return false;
        }

        rscList.remove(rsc);

        // if this was the last resource with this name in the list and if it is
        // currently selected
        // then change the dominant resource.
        if (rscList.isEmpty()) {

            // if currently selected,
            if (dom_rsc_combo.getText().equals(mapKey)) {

                dom_rsc_combo.remove(mapKey);

                if (dom_rsc_combo.getItemCount() == 0) {
                    dom_rsc_combo.setItems(noResourcesList);
                }

                dom_rsc_combo.select(0);
                selectDominantResource();
            } else {
                dom_rsc_combo.remove(mapKey);
            }
        }
        // if this is an event type then force the next event type resource to
        // be the
        // dominant.
        else if (rsc.getTimelineGenMethod() == TimelineGenMethod.USE_MANUAL_TIMELINE) {
            selectDominantResource();
        }

        return true;
    }

    public void clearTimeline() {
        availDomResourcesMap.clear();
        dom_rsc_combo.setItems(noResourcesList);
        dom_rsc_combo.select(0);

        setTimeMatcher(new NCTimeMatcher());
        updateTimeline(timeMatcher);
    }

    // End of methods from old TimelineControl

    // Methods from old Timeline class
    public void setTimelineState(String state, boolean disable) {

        timelineStateMessage = state;

        setControlsEnabled(!disable);
        canvas.redraw();
    }

    public void updateTimeline(NCTimeMatcher tm) {
        timeMatcher = tm;

        if (timeMatcher.isCurrentRefTime()) {
            refTimeCombo.select(0);
        } else if (timeMatcher.isLatestRefTime()) {
            refTimeCombo.select(1);
        } else {
            refTimeCombo.select(2);
        }

        updateTimeline();
    }

    private void updateTimeline() {

        if (timeMatcher.getFrameTimes().isEmpty()) {
            if (timeMatcher.getDominantResource() == null) {
                setTimelineState("No Dominant Resource Selected", true);
            } else if (!timeMatcher.isDataAvailable()) {
                // don't disable since the user may still change to use a time
                // interval
                setTimelineState("No Data Available For "
                        + timeMatcher.getDominantResourceName().toString(),
                        false);// true );
            } else {
                setTimelineState(
                        "No Data Available Within Selected Time Range", false);
            }

            timeData = new TimelineData(new ArrayList<Calendar>());
        } else {

            List<Calendar> availTimes = toCalendar(timeMatcher
                    .getSelectableDataTimes());

            // this shouldn't happen. If there are no times then the caller
            // should set the state based on the reason there are no times.
            if (availTimes == null || availTimes.isEmpty()) {
                setTimelineState("Timeline Disabled", true);
                availTimes = new ArrayList<Calendar>();
            } else if (availTimes.isEmpty()) {
                setTimelineState("Timeline Disabled", true);
            } else {
                timelineStateMessage = null;
                setControlsEnabled(true);
            }

            // append data at the end to fill the snap peried
            snapAvailtimes(availTimes);

            timeData = new TimelineData(availTimes);

            /*
             * TTR 1034+: Force the timeline to start from a given time and
             * extend backward to with a time range, regardless of the actual
             * data availability - only for obs. For forecast data, always use
             * actually data time (cycle time).
             */
            if (!timeMatcher.isForecast()) {
                long refTimeMillisecs;
                if (timeMatcher.isCurrentRefTime()) {
                    refTimeMillisecs = Calendar.getInstance().getTimeInMillis();
                } else {
                    refTimeMillisecs = timeMatcher.getRefTime().getValidTime()
                            .getTimeInMillis();
                }

                long timeRangeMillisecs = ((long) timeMatcher.getTimeRange()) * 60 * 60 * 1000;

                DataTime endRefTime = timeMatcher
                        .getNormalizedTime(new DataTime(new Date(
                                refTimeMillisecs)));
                DataTime startRefTime = timeMatcher
                        .getNormalizedTime(new DataTime(new Date(
                                refTimeMillisecs - timeRangeMillisecs)));

                timeData.setStartTime(startRefTime.getValidTime());
                timeData.setEndTime(endRefTime.getValidTime());
            }
            // End of TTR1034+ change.

            // update widgets from default resource definition.
            updateTimelineWidgets(timeMatcher);

            removeSpinnerListeners();

            // these can trigger the modify listeners too...
            hasDifferentMinutes = checkTimeMinutes(availTimes);

            List<Calendar> seldTimes = toCalendar(timeMatcher.getFrameTimes());

            // only one frame
            if (!seldTimes.isEmpty()) {
                Calendar seldTime = seldTimes.get(seldTimes.size() - 1);

                seldTime = GraphTimelineUtil.snapTimeToNext(seldTime,
                        timeMatcher.getHourSnap());
                timeData.select(seldTime);

                int graphSize = timeMatcher.getGraphRange() * 60 + 1;
                for (int i = 0; i < graphSize; i++) {
                    seldTime.add(Calendar.MINUTE, -1);
                    timeData.select(seldTime);
                }

            }

            // update widgets from default resource definition.
            updateTimelineWidgets(timeMatcher);
        }
    }

    private void snapAvailtimes(List<Calendar> availTimes) {
        int snap = timeMatcher.getHourSnap();
        if (snap != 0) {
            // && timeMatcher.getDominantResourceName().getRscCategory()
            // .getCategoryName().equals("TIMESERIES")) {
            GraphTimelineUtil.sortAvailableCalendar(availTimes);
            Calendar lastAvail = availTimes.get(availTimes.size() - 1);

            int hour = lastAvail.get(Calendar.HOUR_OF_DAY);
            int min = lastAvail.get(Calendar.MINUTE);
            if (!(hour % snap == 0 && min == 0)) {
                int fillSize = snap * 60 - (hour % snap * 60 + min);
                for (int i = 0; i < fillSize; i++) {
                    lastAvail.add(Calendar.MINUTE, 1);
                    availTimes.add(lastAvail);
                }
            }
        }
    }

    private void setControlsEnabled(boolean enable) {

        graphRangeCombo.setEnabled(enable);
        hourSnapCombo.setEnabled(enable);
        timeRangeDaysSpnr.setEnabled(enable);
        timeRangeHrsSpnr.setEnabled(enable);
        frameIntervalCombo.setEnabled(enable);
        refTimeCombo.setEnabled(enable);
        canvas.setEnabled(enable);
    }

    private List<Calendar> toCalendar(List<DataTime> times) {
        if (times == null)
            return null;

        List<Calendar> timelist = new ArrayList<Calendar>();
        for (DataTime dt : times) {
            timelist.add(dt.getValidTime());
        }
        return timelist;
    }

    /**
     * Returns a list of the selected data times.
     * 
     * @return
     */
    public List<Calendar> getSelectedTimes() {
        ArrayList<Calendar> list = new ArrayList<Calendar>();
        int selectedRange = timeMatcher.getGraphRange() * 60;

        // Calendar lastSelected = timeData.getFirstSelected();
        // if (lastSelected == null)
        // return list;
        //
        // GraphTimelineUtil.snapTimeToNext(lastSelected,
        // timeMatcher.getHourSnap());
        //
        // lastSelected.add(Calendar.HOUR_OF_DAY, -selectedRange / 60);
        // timeData.deselectAll();
        // list.add(lastSelected);
        // for (int i = 0; i < selectedRange; i++) {
        //
        // lastSelected.add(Calendar.MINUTE, +1);
        // Calendar cal = (Calendar) lastSelected.clone();
        // list.add(cal);
        // }
        // System.out.println("**times " + list.size() + " "
        // + list.get(0).get(Calendar.HOUR_OF_DAY));
        Calendar firstSelected = timeData.getFirstSelected();
        if (firstSelected == null)
            return list;

        // firstSelected = GraphTimelineUtil.snapTimeToClosest(firstSelected,
        // timeMatcher.getHourSnap());

        // firstSelected.add(Calendar.HOUR_OF_DAY, selectedRange / 60);
        timeData.deselectAll();
        list.add(firstSelected);
        for (int i = 0; i < selectedRange; i++) {

            firstSelected.add(Calendar.MINUTE, +1);
            Calendar cal = (Calendar) firstSelected.clone();
            list.add(cal);
            timeData.select(cal);
        }

        return list;
        // return timeData.getSelectedTimes();
    }

    private void removeSpinnerListeners() {

        for (Listener each : graphRangeCombo.getListeners(SWT.Modify)) {
            graphRangeCombo.removeListener(SWT.Modify, each);
        }
        for (Listener each : hourSnapCombo.getListeners(SWT.Modify)) {
            hourSnapCombo.removeListener(SWT.Modify, each);
        }
        for (Listener each : timeRangeDaysSpnr.getListeners(SWT.Modify)) {
            timeRangeDaysSpnr.removeListener(SWT.Modify, each);
        }
        for (Listener each : timeRangeHrsSpnr.getListeners(SWT.Modify)) {
            timeRangeHrsSpnr.removeListener(SWT.Modify, each);
        }
    }

    private void addSpinnerListeners() {

        timeRangeDaysSpnr.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (timeRangeHrs == timeRangeDaysSpnr.getSelection() * 24
                        + timeRangeHrsSpnr.getSelection()) {
                    return;
                }
                timeRangeHrs = timeRangeDaysSpnr.getSelection() * 24
                        + timeRangeHrsSpnr.getSelection();

                if (timeRangeHrs == 0) {
                    timeRangeHrsSpnr.setSelection(1);
                } else {
                    timeMatcher.setTimeRange(timeRangeHrs);

                    timeMatcher.generateTimeline();

                    updateTimeline();
                }
            }
        });

        timeRangeHrsSpnr.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (timeRangeHrs == timeRangeDaysSpnr.getSelection() * 24
                        + timeRangeHrsSpnr.getSelection()) {
                    return;
                }
                timeRangeHrs = timeRangeDaysSpnr.getSelection() * 24
                        + timeRangeHrsSpnr.getSelection();

                if (timeRangeHrs == 0) {
                    timeRangeHrsSpnr.setSelection(1);
                } else {
                    timeMatcher.setTimeRange(timeRangeHrs);

                    timeMatcher.generateTimeline();

                    updateTimeline();
                }
            }
        });
    }

    /*
     * Determines whether every data time is specified at the same minute of the
     * hour.
     */
    private boolean checkTimeMinutes(List<Calendar> times) {

        if (times.isEmpty())
            return false;

        int min = times.get(0).get(Calendar.MINUTE);
        for (Calendar cal : times) {
            if (min != cal.get(Calendar.MINUTE))
                return true;
        }
        return false;
    }

    /*
     * Create all the widgets used for the timeline
     */
    private void createWidgets(Composite top_form) {

        createControlWidgets(top_form);

        /*
         * set up canvas on which time line display is drawn
         */
        canvas = new Canvas(top_form, SWT.BORDER);
        FormData fd = new FormData();
        fd.top = new FormAttachment(graphRangeCombo, 10, SWT.BOTTOM);
        fd.left = new FormAttachment(0, 10);
        fd.bottom = new FormAttachment(100, 0);
        fd.right = new FormAttachment(100, 0);

        canvas.setLayoutData(fd);

        canvas.setFont(canvasFont);
        canvas.setBackground(canvasColor);

        canvas.addPaintListener(new PaintListener() {
            @Override
            public void paintControl(PaintEvent e) {
                // displays the timeline in the canvas
                drawTimeline((Canvas) e.getSource(), e.gc);
            }
        });

        canvas.addControlListener(new ControlListener() {
            @Override
            public void controlMoved(ControlEvent e) {
            }

            @Override
            public void controlResized(ControlEvent e) {
                resetSlider();
            }
        });

        canvas.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                canvasFont.dispose();
                pointerCursor.dispose();
                resizeCursor.dispose();
                grabCursor.dispose();
            }
        });

        Listener mouse = new Listener() {
            /*
             * Contains the mouse handler events for selecting/deselecting
             * individual data times. AND for moving and resize the slider bar
             */
            MODE mode;

            int saveX = 0;

            Rectangle sliderStart;

            boolean dragging = false;

            @Override
            public void handleEvent(Event e) {

                if (timelineStateMessage != null) {
                    return;
                }

                switch (e.type) {

                case SWT.MouseDown:

                    if (e.button == 1) {

                        /*
                         * If click on an available time, toggle it's status
                         * between selected and not selected
                         */
                        // Disable now. May need in some case
                        // for (Rectangle rect : availableTimes.keySet()) {
                        // if (rect.contains(e.x, e.y)) {
                        // timeData.toggle(availableTimes.get(rect));
                        // if (timeData.numSelected() == 0) {
                        // // can't turn off only selected time...turn
                        // // back on
                        // timeData.toggle(availableTimes.get(rect));
                        // }
                        // resetSlider();
                        // canvas.redraw();
                        // timeMatcher
                        // .setFrameTimes(toDataTimes(getSelectedTimes()));
                        // return;
                        // }
                        // }

                        /*
                         * If user grabs center, top or bottom of slider bar,
                         * move it to new location on mouse up
                         */
                        Rectangle moveSlider = new Rectangle(slider.x + 2,
                                slider.y - 2, slider.width - 4,
                                slider.height + 4);
                        if (moveSlider.contains(e.x, e.y)) {
                            saveX = e.x;
                            sliderStart = new Rectangle(slider.x, slider.y,
                                    slider.width, slider.height);
                            dragging = true;
                            mode = MODE.MOVE_ALL;
                            return;
                        }

                        /*
                         * If user grabs left side of slider bar, adjust its
                         * location on mouse up
                         */
                        Rectangle leftSide = new Rectangle(slider.x - 2,
                                slider.y - 2, 4, slider.height + 4);
                        if (leftSide.contains(e.x, e.y)) {
                            saveX = e.x;
                            sliderStart = new Rectangle(slider.x, slider.y,
                                    slider.width, slider.height);
                            dragging = true;
                            mode = MODE.MOVE_LEFT;
                            return;
                        }

                        /*
                         * If user grabs right side of slider bar, adjust its
                         * location on mouse up
                         */
                        Rectangle rightSide = new Rectangle(slider.x
                                + slider.width - 2, slider.y - 2, 4,
                                slider.height + 4);
                        if (rightSide.contains(e.x, e.y)) {
                            saveX = e.x;
                            sliderStart = new Rectangle(slider.x, slider.y,
                                    slider.width, slider.height);
                            dragging = true;
                            mode = MODE.MOVE_RIGHT;
                            return;
                        }
                    }

                    break;

                case SWT.MouseMove:

                    if (saveX != 0) { // mouse drag is detected
                        int xdiff = e.x - saveX;

                        switch (mode) {

                        case MOVE_ALL:
                            moveSlider(sliderStart, xdiff);
                            break;
                        case MOVE_LEFT:
                            moveLeftSide(sliderStart, xdiff);
                            break;
                        case MOVE_RIGHT:
                            moveRightSide(sliderStart, xdiff);
                            break;
                        }

                        canvas.redraw();
                    } else if (slider != null) { // No drag detected...set
                                                 // appropriate cursor

                        Rectangle moveSlider = new Rectangle(slider.x + 2,
                                slider.y - 2, slider.width - 4,
                                slider.height + 4);
                        Rectangle leftSide = new Rectangle(slider.x - 2,
                                slider.y - 2, 4, slider.height + 4);
                        Rectangle rightSide = new Rectangle(slider.x
                                + slider.width - 2, slider.y - 2, 4,
                                slider.height + 4);

                        if (moveSlider.contains(e.x, e.y)) {
                            canvas.setCursor(grabCursor);
                        } else if (leftSide.contains(e.x, e.y)
                                || rightSide.contains(e.x, e.y)) {
                            canvas.setCursor(resizeCursor);
                        } else {
                            canvas.setCursor(pointerCursor);
                        }
                    }

                    break;

                case SWT.MouseUp:
                    if (!dragging) {
                        return;
                    }

                    dragging = false;

                    if (e.button == 1) {

                        if (saveX == e.x) { // mouse did not move. reset
                            saveX = 0;
                            return;
                        }

                        if (saveX != 0) { // mouse drag is detected
                            int xdiff = e.x - saveX;// before mouse up

                            // get snap distance ratio
                            int snap = timeMatcher.getHourSnap();

                            Calendar first = timeData.getFirstTime();
                            Calendar last = timeData.getLastTime();
                            int total = (int) (last.getTimeInMillis() - first
                                    .getTimeInMillis());

                            double snapRatio = (double) (snap * 3600 * 1000)
                                    / (double) total;

                            Point size = canvas.getSize();
                            int lineY = Math.round((float) size.y * TIME_LINE);
                            Point beg = new Point(MARGIN, lineY);
                            Point end = new Point(size.x - MARGIN - 1, lineY);

                            double snapDist = (double) (end.x - beg.x)
                                    * snapRatio;

                            double snapNum = (double) Math.abs(xdiff)
                                    / (double) snapDist;
                            int snapNumRound = (int) Math.round(snapNum);

                            switch (mode) {

                            case MOVE_ALL: {

                                int snapX = 0;
                                Calendar firstSelected = timeData
                                        .getFirstSelected();
                                Calendar lastSelected = timeData
                                        .getLastSelected();
                                lastSelected = GraphTimelineUtil
                                        .snapTimeToNext(lastSelected, snap);

                                int firstSelectedX = currPositionFromTime(firstSelected);//
                                int lastSelectedX = currPositionFromTime(lastSelected);

                                Calendar snapped = null;

                                if (e.x <= saveX) {
                                    snapped = (Calendar) firstSelected.clone();
                                    snapped.add(Calendar.HOUR_OF_DAY, -snap
                                            * snapNumRound);
                                    snapX = currPositionFromTime(snapped);
                                    xdiff = snapX - firstSelectedX;
                                } else if (e.x > saveX) {
                                    snapped = (Calendar) lastSelected.clone();
                                    snapped.add(Calendar.HOUR_OF_DAY, snap
                                            * snapNumRound);
                                    snapX = currPositionFromTime(snapped);
                                    xdiff = snapX - lastSelectedX;
                                }

                                moveSlider(sliderStart, xdiff);
                                break;
                            }
                            case MOVE_LEFT: {

                                int snapX = 0;
                                Calendar curr = currTimeFromPosition(e.x);

                                Calendar snapped = (Calendar) curr.clone();
                                if (snapNumRound == 0) {
                                    snapped = GraphTimelineUtil.snapTimeToNext(
                                            snapped, snap);
                                    xdiff = 0;
                                } else {
                                    if (e.x <= saveX) {

                                        snapped = GraphTimelineUtil
                                                .snapTimeToClosest(snapped,
                                                        snap);
                                        newGraphRange = timeMatcher
                                                .getGraphRange()
                                                + (snap * snapNumRound);
                                        setGraphRangeCombo(newGraphRange);
                                    } else {

                                        snapped = GraphTimelineUtil
                                                .snapTimeToClosest(snapped,
                                                        snap);
                                        newGraphRange = timeMatcher
                                                .getGraphRange()
                                                - (snap * snapNumRound);
                                        setGraphRangeCombo(newGraphRange);
                                    }

                                    snapX = currPositionFromTime(snapped);
                                    xdiff = snapX - saveX;
                                }

                                moveLeftSide(sliderStart, xdiff);

                                break;
                            }
                            case MOVE_RIGHT: {

                                int snapX = 0;
                                Calendar curr = currTimeFromPosition(e.x);

                                Calendar snapped = (Calendar) curr.clone();

                                if (snapNumRound == 0) {
                                    snapped = GraphTimelineUtil
                                            .snapTimeToPrevious(snapped, snap);
                                    xdiff = 0;
                                } else {
                                    if (e.x <= saveX) {

                                        snapped = GraphTimelineUtil
                                                .snapTimeToClosest(snapped,
                                                        snap);
                                        newGraphRange = timeMatcher
                                                .getGraphRange()
                                                - (snap * snapNumRound);
                                        setGraphRangeCombo(newGraphRange);
                                    } else {

                                        snapped = GraphTimelineUtil
                                                .snapTimeToClosest(snapped,
                                                        snap);
                                        newGraphRange = timeMatcher
                                                .getGraphRange()
                                                + (snap * snapNumRound);
                                        setGraphRangeCombo(newGraphRange);
                                    }

                                    snapX = currPositionFromTime(snapped);
                                    xdiff = snapX - saveX;
                                }

                                moveRightSide(sliderStart, xdiff);

                                break;
                            }
                            }

                            saveX = 0; // reset
                            updateSelectedTimes(xdiff);

                            canvas.redraw();
                        }

                    }

                    break;
                }
            }
        };

        canvas.addListener(SWT.MouseDown, mouse);
        canvas.addListener(SWT.MouseMove, mouse);
        canvas.addListener(SWT.MouseUp, mouse);

        graphRangeCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                for (int i = 0; i < availGraphRangeStrings.length; i++) {
                    if (availGraphRangeStrings[i].equals(graphRangeCombo
                            .getText())) {
                        timeMatcher.setGraphRange(availGraphRangeHrs[i]);
                        break;
                    }
                }

                timeMatcher.generateTimeline();

                updateTimeline();
            }
        });

        hourSnapCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                for (int i = 0; i < availHourSnapStrings.length; i++) {
                    if (availHourSnapStrings[i].equals(hourSnapCombo.getText())) {
                        timeMatcher.setHourSnap(availHourSnapHrs[i]);
                        break;
                    }
                }

                timeMatcher.generateTimeline();

                updateTimeline();
            }
        });

        frameIntervalCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                for (int i = 0; i < availFrameIntervalStrings.length; i++) {
                    if (availFrameIntervalStrings[i].equals(frameIntervalCombo
                            .getText())) {
                        timeMatcher.setFrameInterval(availFrameIntervalMins[i]);
                        break;
                    }
                }

                timeMatcher.generateTimeline();

                updateTimeline();
            }
        });

        refTimeCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (refTimeCombo.getSelectionIndex() == 0) {
                    timeMatcher.setCurrentRefTime();
                } else if (refTimeCombo.getSelectionIndex() == 1) {
                    timeMatcher.setLatestRefTime();
                } else if (refTimeCombo.getSelectionIndex() == 2) {

                    CalendarSelectDialog calSelDlg = new CalendarSelectDialog(
                            shell);

                    DataTime newRefTime = calSelDlg.open(timeMatcher
                            .getRefTime());
                    if (newRefTime != null) {
                        timeMatcher.setRefTime(newRefTime);
                    } else {
                        return;
                    }

                }

                timeMatcher.generateTimeline();
                updateTimeline();
            }
        });
    }

    /*
     * Creates control widgets above the timeline canvas
     */
    private void createControlWidgets(Composite top_form) {
        FormData fd = new FormData();

        graphRangeCombo = new Combo(top_form, SWT.DROP_DOWN);
        fd = new FormData();
        fd.width = 20;
        fd.top = new FormAttachment(dom_rsc_combo, 50, SWT.BOTTOM);
        fd.left = new FormAttachment(5, 0);
        fd.right = new FormAttachment(5, 80);
        graphRangeCombo.setLayoutData(fd);

        graphRangeCombo.setItems(availGraphRangeStrings);

        Label graphRangeLbl = new Label(top_form, SWT.NONE);
        graphRangeLbl.setText("Selected\nRange");
        fd = new FormData();
        fd.bottom = new FormAttachment(graphRangeCombo, -3, SWT.TOP);
        fd.left = new FormAttachment(graphRangeCombo, 0, SWT.LEFT);
        graphRangeLbl.setLayoutData(fd);

        hourSnapCombo = new Combo(top_form, SWT.DROP_DOWN | SWT.READ_ONLY);
        fd = new FormData();
        fd.top = new FormAttachment(graphRangeCombo, 0, SWT.TOP);
        fd.left = new FormAttachment(20, 20);
        fd.right = new FormAttachment(20, 70);
        hourSnapCombo.setLayoutData(fd);
        hourSnapCombo.setItems(availHourSnapStrings);

        Label hourSnapLbl = new Label(top_form, SWT.NONE);
        hourSnapLbl.setText("Snap\nHours");
        fd = new FormData();
        fd.bottom = new FormAttachment(hourSnapCombo, -3, SWT.TOP);
        fd.left = new FormAttachment(hourSnapCombo, 0, SWT.LEFT);
        hourSnapLbl.setLayoutData(fd);

        timeRangeDaysSpnr = new Spinner(top_form, SWT.BORDER);
        fd = new FormData();
        fd.top = new FormAttachment(graphRangeCombo, 0, SWT.TOP);
        fd.left = new FormAttachment(36, 0);
        timeRangeDaysSpnr.setLayoutData(fd);

        Label dfltTimeRangeLbl = new Label(top_form, SWT.NONE);
        dfltTimeRangeLbl.setText("Timeline Range\n(Days / Hours)");
        fd = new FormData();
        fd.bottom = new FormAttachment(timeRangeDaysSpnr, -3, SWT.TOP);
        fd.left = new FormAttachment(timeRangeDaysSpnr, 0, SWT.LEFT);
        dfltTimeRangeLbl.setLayoutData(fd);

        timeRangeDaysSpnr.setMinimum(0);
        timeRangeDaysSpnr.setMaximum(999);
        timeRangeDaysSpnr.setDigits(0);
        timeRangeDaysSpnr.setIncrement(1);
        timeRangeDaysSpnr.setTextLimit(4);
        timeRangeDaysSpnr.setPageIncrement(30);

        timeRangeHrsSpnr = new Spinner(top_form, SWT.BORDER);
        fd = new FormData();
        fd.top = new FormAttachment(timeRangeDaysSpnr, 0, SWT.TOP);
        fd.left = new FormAttachment(timeRangeDaysSpnr, 8, SWT.RIGHT);
        timeRangeHrsSpnr.setLayoutData(fd);

        timeRangeHrsSpnr.setMinimum(0);
        timeRangeHrsSpnr.setMaximum(23);
        timeRangeHrsSpnr.setDigits(0);
        timeRangeHrsSpnr.setIncrement(1);
        timeRangeHrsSpnr.setTextLimit(2);

        frameIntervalCombo = new Combo(top_form, SWT.DROP_DOWN | SWT.READ_ONLY);
        fd = new FormData();
        fd.top = new FormAttachment(graphRangeCombo, 0, SWT.TOP);
        fd.left = new FormAttachment(59, 0);
        frameIntervalCombo.setLayoutData(fd);

        frameIntervalCombo.setItems(availFrameIntervalStrings);

        Label frameIntLbl = new Label(top_form, SWT.NONE);
        frameIntLbl.setText("Data\nInterval");
        fd = new FormData();
        fd.bottom = new FormAttachment(frameIntervalCombo, -3, SWT.TOP);
        fd.left = new FormAttachment(frameIntervalCombo, 0, SWT.LEFT);
        frameIntLbl.setLayoutData(fd);

        refTimeCombo = new Combo(top_form, SWT.DROP_DOWN | SWT.READ_ONLY);
        fd = new FormData();
        fd.top = new FormAttachment(graphRangeCombo, 0, SWT.TOP);
        fd.left = new FormAttachment(80, 0);
        refTimeCombo.setLayoutData(fd);

        refTimeCombo.setItems(refTimeSelectionOptions);

        refTimeLbl = new Label(top_form, SWT.NONE);
        refTimeLbl.setText("Ref. Time");
        fd = new FormData();
        fd.bottom = new FormAttachment(refTimeCombo, -3, SWT.TOP);
        fd.left = new FormAttachment(refTimeCombo, 0, SWT.LEFT);
        refTimeLbl.setLayoutData(fd);
    }

    /*
     * calculates and draws all the time line info
     */
    protected void drawTimeline(Canvas canvas, GC gc) {

        Point size = canvas.getSize();

        int textHeight = gc.getFontMetrics().getHeight();

        if (timelineStateMessage != null) {
            int width = gc.getCharWidth('e') * timelineStateMessage.length();
            gc.drawText(timelineStateMessage, (size.x - width) / 2,
                    (size.y - textHeight) / 2);
            return;
        } else if (timeData.isEmpty()) { // shouldn't happen. if empty the state
                                         // should be set
            timelineStateMessage = new String("Timeline Empty");
            return;
        }

        /*
         * draw date line that separates month/days and the hours of day
         */

        int dateY = Math.round((float) size.y * DATE_LINE);
        Point begDateLine = new Point(MARGIN, dateY);
        Point endDateLine = new Point(size.x - MARGIN - 1, dateY);

        gc.drawLine(begDateLine.x, begDateLine.y, endDateLine.x, endDateLine.y);

        /*
         * display months and days of month
         */
        calculateDates(begDateLine, endDateLine);
        plotDates(gc, begDateLine, endDateLine);

        /*
         * draw time line
         */

        int lineY = Math.round((float) size.y * TIME_LINE);
        Point begTimeLine = new Point(MARGIN, lineY);
        Point endTimeLine = new Point(size.x - MARGIN - 1, lineY);
        gc.drawLine(begTimeLine.x, begTimeLine.y, endTimeLine.x, endTimeLine.y);

        plotTickMarks(gc, begTimeLine, endTimeLine, dateY);

        calculateAvailableBoxes(begTimeLine, endTimeLine);
        plotAvailableBoxes(gc);
        int hourY = (size.y + lineY) / 2;
        plotAvailableTimes(gc, hourY);

        /*
         * draw slider bar
         */
        if (slider == null)
            slider = calculateSlider(begTimeLine, endTimeLine);

        gc.setLineWidth(2);
        gc.drawRectangle(slider);
        gc.setLineWidth(1);

        // display legend
        gc.setBackground(availableColor);
        gc.fillRectangle(5, size.y - MARKER_HEIGHT - 5, MARKER_WIDTH,
                MARKER_HEIGHT);
        gc.setBackground(canvasColor);
        gc.drawText("available data", 15, size.y - textHeight, true);
        gc.setBackground(selectedColor);
        gc.fillRectangle(150, size.y - MARKER_HEIGHT - 5, MARKER_WIDTH,
                MARKER_HEIGHT);
        gc.setBackground(canvasColor);
        gc.drawText("selected data", 160, size.y - textHeight, true);

    }

    /*
     * Calculate the rectangle defining the slider bar, based on the currently
     * selected dates.
     */
    private Rectangle calculateSlider(Point beg, Point end) {

        int ulX, lastX;
        sliderMin = beg.x - 5;
        sliderMax = end.x + 5;

        Calendar time1 = timeData.getFirstSelected();
        Calendar time2 = timeData.getLastSelected();

        if (time1 == null)
            return new Rectangle(0, 0, 0, 0);

        Calendar prev = timeData.getPreviousTime(time1);
        if (prev != null && timeLocations.get(prev) != null
                && timeLocations.get(time1) != null)
            ulX = (timeLocations.get(prev) + timeLocations.get(time1)) / 2;
        else
            ulX = sliderMin + 5;

        Calendar next = timeData.getNextTime(time2);
        if (next != null && timeLocations.get(next) != null) {
            lastX = (timeLocations.get(time2) + timeLocations.get(next)) / 2;
        } else
            lastX = sliderMax - 5; // ?

        int ulY = beg.y - SLIDER;
        int width = lastX - ulX;
        int height = 2 * SLIDER;

        return new Rectangle(ulX, ulY, width, height);
    }

    private Calendar currTimeFromPosition(int posX) {
        Point size = canvas.getSize();
        int lineY = Math.round((float) size.y * TIME_LINE);
        Point beg = new Point(MARGIN, lineY);
        Point end = new Point(size.x - MARGIN - 1, lineY);
        int lineLength = end.x - beg.x;

        double dist = (double) (posX - beg.x) / (double) lineLength;

        Calendar first = timeData.getFirstTime();
        first.setTimeZone(TimeZone.getTimeZone("UTC"));
        Calendar last = timeData.getLastTime();
        last.setTimeZone(TimeZone.getTimeZone("UTC"));

        long timeLength = last.getTimeInMillis() - first.getTimeInMillis();

        long currMills = Math.round(dist * (double) timeLength)
                + first.getTimeInMillis();

        Calendar curr = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        curr.setTimeInMillis(currMills);

        curr.set(Calendar.SECOND, 0);
        curr.set(Calendar.MILLISECOND, 0);

        return curr;
    }

    private int currPositionFromTime(Calendar curr) {
        Calendar first = timeData.getFirstTime();
        first.setTimeZone(TimeZone.getTimeZone("UTC"));
        Calendar last = timeData.getLastTime();
        last.setTimeZone(TimeZone.getTimeZone("UTC"));
        long timeLength = last.getTimeInMillis() - first.getTimeInMillis();

        Point size = canvas.getSize();
        int lineY = Math.round((float) size.y * TIME_LINE);
        Point beg = new Point(MARGIN, lineY);
        Point end = new Point(size.x - MARGIN - 1, lineY);
        int lineLength = end.x - beg.x;

        curr.setTimeZone(TimeZone.getTimeZone("UTC"));

        double dist = (double) (curr.getTimeInMillis() - first
                .getTimeInMillis()) / (double) timeLength;

        int currX = (int) Math.round(dist * (double) lineLength) + beg.x;

        return currX;
    }

    /*
     * calculates a list of days between the first and last available times, and
     * calculate each day's relative position along the line defined by the two
     * points specified beg and end
     */
    private void calculateDates(Point beg, Point end) {

        days = new ArrayList<Calendar>();
        dayLocation = new ArrayList<Integer>();
        int lineLength = end.x - beg.x;

        Calendar first = timeData.getStartTime();
        Calendar last = timeData.getEndTime();
        long timeLength = timeData.getTotalMillis();

        Calendar cal = (Calendar) first.clone();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        while (cal.before(last)) {

            if (cal.before(first)) {
                days.add(cal);
                dayLocation.add(beg.x);
            } else {
                double dist = (double) (cal.getTimeInMillis() - first
                        .getTimeInMillis()) / (double) timeLength;
                long lineDist = Math.round(dist * (double) lineLength);
                days.add(cal);
                dayLocation.add(beg.x + (int) lineDist);
            }

            cal = (Calendar) cal.clone();
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

    }

    /*
     * plot days and month names
     */
    private void plotDates(GC gc, Point beg, Point end) {

        if (days.size() <= MAX_DATES) {
            plotDays(gc, beg, end, "MMMdd", true);
        } else {
            plotDays(gc, beg, end, "dd", false);
            plotMonths(gc, beg, end);
        }
    }

    /*
     * plots the label of each day in the available times range
     */
    private void plotDays(GC gc, Point beg, Point end, String fmt,
            boolean center) {

        SimpleDateFormat sdf = new SimpleDateFormat(fmt);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

        int textHeight = gc.getFontMetrics().getHeight();
        int width = gc.getCharWidth('0');
        int halfWidth = (width * fmt.length()) / 2;
        int locY = beg.y - textHeight;
        if (center)
            locY = (beg.y - textHeight) / 2;

        int numdays = days.size();

        for (int j = 0; j < numdays; j++) {

            Calendar cal = days.get(j);
            int startX = dayLocation.get(j);
            int endX = end.x;
            if (j < (numdays - 1)) {
                endX = dayLocation.get(j + 1);
            }

            String hour = sdf.format(cal.getTime());
            int locX = (endX + startX) / 2 - halfWidth;
            if (locX > startX)
                gc.drawText(hour, locX, locY);

            if (j != 0)
                gc.drawLine(startX, locY, startX, beg.y); // separator

        }

    }

    /*
     * plots the label of each month in the available times range
     */
    private void plotMonths(GC gc, Point beg, Point end) {

        String fmt = new String("MMM");
        SimpleDateFormat sdf = new SimpleDateFormat(fmt);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

        int width = gc.getCharWidth('0');
        int halfWidth = (width * fmt.length()) / 2;
        int locY = 0;

        Calendar first = days.get(0);
        Calendar last = days.get(days.size() - 1);

        if (first.get(Calendar.MONTH) == last.get(Calendar.MONTH)) {
            String month = sdf.format(first.getTime());
            int locX = (end.x + beg.x) / 2 - halfWidth;
            gc.drawText(month, locX, locY, true);
        } else {
            int locX = 0;
            for (Calendar cal : days) {
                if (cal.get(Calendar.DAY_OF_MONTH) == 1) {
                    int index = days.indexOf(cal);
                    locX = dayLocation.get(index);
                    break;
                }
            }

            gc.drawLine(locX, locY, locX, beg.y); // separator

            String month = sdf.format(first.getTime());
            int startX = (locX + beg.x) / 2 - halfWidth;
            gc.drawText(month, startX, locY, true);

            month = sdf.format(last.getTime());
            startX = (end.x + locX) / 2 - halfWidth;
            gc.drawText(month, startX, locY, true);
        }
    }

    /*
     * draws tick marks along the timeline defined by the two given endpoints
     * beg and end
     */
    private void plotTickMarks(GC gc, Point beg, Point end, int dateY) {
        int prevX = -9999;
        int lineLength = end.x - beg.x;

        SimpleDateFormat sdf = new SimpleDateFormat("HH");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        Calendar first = timeData.getStartTime();
        Calendar last = timeData.getEndTime();

        int textHeight = gc.getFontMetrics().getHeight();
        int width = gc.getCharWidth('0');
        int locY = (dateY + beg.y) / 2;
        int overlap = width * 3; // 3 characters

        long timeLength = timeData.getTotalMillis();
        int totalMinutes = timeData.getTotalMinutes();
        int timeInterval = calcTimeInterval(lineLength, totalMinutes); // in
                                                                       // minutes

        Calendar cal = (Calendar) first.clone();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        while (cal.before(last)) {
            if (cal.before(first)) {
                cal.add(Calendar.MINUTE, timeInterval);
                continue;
            }
            double dist = (double) (cal.getTimeInMillis() - first
                    .getTimeInMillis()) / (double) timeLength;
            long lineDist = Math.round(dist * (double) lineLength);
            int locX = beg.x + (int) lineDist;
            int tickSize;
            if (hasDifferentMinutes) {
                tickSize = (cal.get(Calendar.MINUTE) == 0) ? TICK_LARGE
                        : TICK_SMALL;
            } else {
                tickSize = (cal.get(Calendar.HOUR_OF_DAY) % 3 == 0) ? TICK_LARGE
                        : TICK_SMALL;
            }
            gc.drawLine(locX, beg.y - tickSize, locX, beg.y + tickSize);

            /*
             * label the tick mark with the hour of the day, if room.
             */
            String hour = sdf.format(cal.getTime());
            int x = locX - width;
            int y = locY - (textHeight / 2);
            if (x > (prevX + overlap)) {
                gc.drawText(hour, x, y);
                prevX = x;
            }

            cal.add(Calendar.MINUTE, timeInterval); // time of next tick mark
        }
    }

    /*
     * calculates an appropriate time interval (in minutes) to use for tick
     * marks along the timeline
     */
    private int calcTimeInterval(int lineLength, int minutes) {
        int interval = 15;
        int maxnum = lineLength / 4;

        if (hasDifferentMinutes) {
            if ((minutes / interval) > maxnum)
                interval = 30;
            if ((minutes / interval) > maxnum)
                interval = 60;
        } else {
            interval = 60;
        }

        if ((minutes / interval) > maxnum)
            interval = 180;
        if ((minutes / interval) > maxnum)
            interval = 360;
        if ((minutes / interval) > maxnum)
            interval = 720;
        if ((minutes / interval) > maxnum)
            interval = 1440;

        return interval;
    }

    /*
     * Calculate the boxes and their locations that will be used to represent
     * the available times on the timeline
     */
    private void calculateAvailableBoxes(Point beg, Point end) {

        availableTimes = new LinkedHashMap<Rectangle, Calendar>();
        timeLocations = new HashMap<Calendar, Integer>();

        int lineLength = end.x - beg.x;

        Calendar first = timeData.getStartTime();

        long timeLength = timeData.getTotalMillis();

        for (Calendar curr : timeData.getTimes()) {
            // the last data is next snap 00:00, so don't put it to
            // availableTimes.
            if (timeData.getNextTime(curr) != null) {
                double dist = (double) (curr.getTimeInMillis() - first
                        .getTimeInMillis()) / (double) timeLength;
                long lineDist = Math.round(dist * (double) lineLength);
                int locX = beg.x + (int) lineDist - (MARKER_WIDTH / 2);
                int locY = beg.y - (MARKER_HEIGHT / 2);

                Rectangle box = new Rectangle(locX, locY, MARKER_WIDTH,
                        MARKER_HEIGHT);
                availableTimes.put(box, curr);
                timeLocations.put(curr, beg.x + (int) lineDist);
            }
        }
    }

    /*
     * draw boxes representing availbale times on timeline. Selected times are
     * displayed in a different color
     */
    private void plotAvailableBoxes(GC gc) {

        gc.setBackground(availableColor);
        for (Rectangle rect : availableTimes.keySet()) {
            gc.setBackground(availableColor);
            if (timeData.isSelected(availableTimes.get(rect))) {
                gc.setBackground(selectedColor);

            }
            gc.fillRectangle(rect);
        }

        gc.setBackground(canvasColor);
    }

    /*
     * Label each available box with the hour or minute of the time represented
     * by the box
     */
    private void plotAvailableTimes(GC gc, int hourY) {
        int prevX = -9999;

        SimpleDateFormat sdf = new SimpleDateFormat("HH");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        if (hasDifferentMinutes)
            sdf.applyPattern("mm");

        int textHeight = gc.getFontMetrics().getHeight();
        int width = gc.getCharWidth('0');
        int overlap = width * 3; // 3 characters

        for (Rectangle rect : availableTimes.keySet()) {
            Calendar cal = availableTimes.get(rect);
            String hour = sdf.format(cal.getTime());
            int x = rect.x + (rect.width / 2) - width;
            int y = hourY - (textHeight / 2);
            if (x > (prevX + overlap)) {
                gc.drawText(hour, x, y);
                prevX = x;
            }
        }
    }

    /**
     * Sets the number of times that should be selected
     * 
     * @param num
     */
    // public void setNumberofFrames(int num) {
    // numFramesSpnr.setSelection(Math.min(num,
    // (timeData != null ? timeData.getSize() : 0)));
    // }

    /**
     * Retuns the current skip factor used
     * 
     * @return
     */
    public int getHourSnapCombo() {
        return timeMatcher.getHourSnap(); // hourSnapCombo.getSelectionIndex();
    }

    /**
     * Sets the snap factor
     * 
     * @param num
     */
    public void setHourSnapCombo(int num) {
        timeMatcher.setHourSnap(num);

        hourSnapCombo.deselectAll();

        for (int i = 0; i < availHourSnapHrs.length; i++) {
            if (availHourSnapHrs[i] == num) {
                hourSnapCombo.select(i);
                break;
            }
        }
    }

    public int getGraphRangeCombo() {
        return timeMatcher.getGraphRange();
    }

    public void setGraphRangeCombo(int num) {
        timeMatcher.setGraphRange(num);

        graphRangeCombo.deselectAll();
        boolean isInCombo = false;
        for (int i = 0; i < availGraphRangeHrs.length; i++) {
            if (availGraphRangeHrs[i] == num) {
                graphRangeCombo.select(i);
                isInCombo = true;
                break;
            }
        }

        if (!isInCombo) {
            graphRangeCombo.setText(String.valueOf(num) + " hrs");
        }
    }

    public void setFrameInterval(int fInt) {
        timeMatcher.setFrameInterval(fInt);

        frameIntervalCombo.deselectAll();

        for (int i = 0; i < availFrameIntervalMins.length; i++) {
            if (availFrameIntervalMins[i] == fInt) {
                frameIntervalCombo.select(i);
                break;
            }
        }
    }

    public int getFrameInterval() {
        return timeMatcher.getFrameInterval();
    }

    public int getTimeRangeHrs() {
        return timeRangeHrs;
    }

    public void setTimeRangeHrs(int tRangeHrs) {
        if (timeRangeHrs == tRangeHrs) {
            return;
        }

        timeRangeHrs = tRangeHrs;

        timeRangeDaysSpnr.setSelection(timeRangeHrs / 24);
        timeRangeHrsSpnr.setSelection(timeRangeHrs % 24);
    }

    /**
     * Sets the number of times that should be selected
     * 
     * @param num
     */
    public void setNumberofFrames(int num) {
        timeMatcher.setNumFrames(num);
    }

    /*
     * resets the slider bar so that it is recalculated using the selected times
     */
    private void resetSlider() {
        slider = null;
    }

    /*
     * Moves the slider bar the specified number of pixels (pos)
     */
    private void moveSlider(Rectangle start, int pos) {

        Rectangle whole = new Rectangle(sliderMin, slider.y, sliderMax
                - sliderMin, slider.height);
        start.x += pos;
        slider = whole.intersection(start);
        start.x -= pos;

    }

    /*
     * Moves the left side of the slider bar the specified number of pixels
     * (pos)
     */
    private void moveLeftSide(Rectangle start, int pos) {
        /*
         * deal with snap
         */
        int startX = start.x + pos;
        int width = start.width - pos;
        if (startX < sliderMin) {
            startX = sliderMin;
            width = start.x + start.width - sliderMin;
        }
        slider = new Rectangle(startX, start.y, width, start.height);
    }

    /*
     * Moves the right side of the slider bar the specified number of pixels
     * (pos)
     */
    private void moveRightSide(Rectangle start, int pos) {
        int width = start.width + pos;
        if (start.x + width > sliderMax)
            width = sliderMax - start.x;
        slider = new Rectangle(start.x, start.y, width, start.height);
    }

    /*
     * update the selected status of each available time that is currently in
     * the slider bar based on current behavior and skip factor
     */
    private void updateSelectedTimes(int xdiff) {

        if (availableTimes == null || slider == null)
            return; // canvas not yet ready

        Calendar first = null;
        Calendar last = null;

        /*
         * determine first and last available data time in slider bar
         */
        for (Rectangle rect : availableTimes.keySet()) {
            if (slider.intersects(rect)) {

                Calendar cal = availableTimes.get(rect);
                if (first == null)
                    first = cal;
                if (last == null)
                    last = cal;
                if (cal.before(first))
                    first = cal;
                if (cal.after(last))
                    last = cal;
            }
        }

        /*
         * If no available times in slider, must select at least one
         */
        if (first == null || last == null) {
            if (slider.x == 0) {
                timeData.deselectAll();
                if (timeMatcher.isForecast()) {
                    timeData.select(timeData.getFirstTime());
                } else {
                    timeData.select(timeData.getLastTime());
                }

                timeMatcher.setFrameTimes(toDataTimes(getSelectedTimes()));
            }

            resetSlider();
            return;
        }

        /*
         * reset status of each data time in slider bar.
         */
        // System.out.println("**first " + first.toString() + " "
        // + last.toString());
        // int snap = timeMatcher.getHourSnap();
        // first = GraphTimelineUtil.snapTimeToClosest(first, snap);
        // last = (Calendar) first.clone();
        // last.add(Calendar.HOUR_OF_DAY, newGraphRange);
        // System.out.println("first " + first.get(Calendar.HOUR_OF_DAY) + " "
        // + first.get(Calendar.MINUTE) + " " + last.get(Calendar.MINUTE));
        if (timeMatcher.isForecast()) {
            timeData.updateRange(first, last, 0);
        } else {
            timeData.updateRange(last, first, 0);
        }
        // List<Calendar> cals = new ArrayList<Calendar>();
        // cals.add(timeData.getFirstSelected());
        timeMatcher.setFrameTimes(toDataTimes(timeData.getSelectedTimes())); // getSelectedTimes()));

    }

    /*
     * Re-draws timeline-related widgets based on a given NCTimeMatcher.
     */
    private void updateTimelineWidgets(NCTimeMatcher tm) {

        if (tm.getDominantResource() != null && tm.isForecast()) {
            refTimeCombo.setVisible(false);

            refTimeLbl.setVisible(false);
        } else {
            refTimeCombo.setVisible(true);
            refTimeLbl.setVisible(true);
        }

        setNumberofFrames(tm.getNumFrames());

        setGraphRangeCombo(tm.getGraphRange());

        setHourSnapCombo(tm.getHourSnap());

        setFrameInterval(tm.getFrameInterval());

        setTimeRangeHrs(tm.getTimeRange());

        addSpinnerListeners();

        resetSlider();
        canvas.redraw();
    }

}
