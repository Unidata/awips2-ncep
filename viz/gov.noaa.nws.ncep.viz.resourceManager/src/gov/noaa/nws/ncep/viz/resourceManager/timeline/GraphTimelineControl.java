/*
 * Timeline
 *
 * Date created 03 MARCH 2010
 *
 * This code has been developed by the SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.viz.resourceManager.timeline;

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
import org.eclipse.swt.widgets.Spinner;

import com.raytheon.uf.common.time.DataTime;

import gov.noaa.nws.ncep.viz.common.ui.CalendarSelectDialog;
import gov.noaa.nws.ncep.viz.resources.time_match.GraphTimelineUtil;
import gov.noaa.nws.ncep.viz.resources.time_match.NCTimeMatcher;

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
 * TODO: Class hierarchy is not correct. There should have been an abstract
 * class with common elements that both TimelineControl and GraphTimelineControl
 * extend. Then TimelineControl and GraphTimelineControl would only support and
 * implement the aspects that were unique to them. Specifically: "main
 * difference are removing numFrames, and numSkip, and adding graphRange and
 * hourSnap". This would eliminate the need to make the same changes twice in
 * both classes as they are now.
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
 * 03/18/2015     R6920     sgurung     Fix NullPointerException in method snapAvailtimes() and remove commented out code
 * 03/11/2016     R15244    bkowal      Initial cleanup to fix how time line control has been extended.
 * 05/26/2016     R19195    sgurung     Fix errors introduced by Redmine Ticket 15244 (error thrown when switching to Graph RBD)
 * </pre>
 *
 * @author qzhou
 * @version 1
 */
public class GraphTimelineControl extends TimelineControl {

    private Canvas canvas;

    private Combo graphRangeCombo;

    private Combo hourSnapCombo;

    private Spinner timeRangeDaysSpnr;

    private Spinner timeRangeHrsSpnr;

    private Combo frameIntervalCombo;

    private Combo refTimeCombo;

    private Label refTimeLbl;

    private int timeRangeHrs = 0;

    private String availGraphRangeStrings[] = { "1 hr", "2 hrs", "3 hrs",
            "6 hrs", "12 hrs", "24 hrs", "3 days", "7 days", "30 days" };

    private int availGraphRangeHrs[] = { 1, 2, 3, 6, 12, 24, 72, 168, 720 };

    // extend refTime to next snap point
    private String availHourSnapStrings[] = { "0", "1", "2", "3", "6", "12",
            "24" };

    private int availHourSnapHrs[] = { 0, 1, 2, 3, 6, 12, 24 };

    private Map<Rectangle, Calendar> availableTimes;

    private Map<Calendar, Integer> timeLocations;

    private int newGraphRange = 0;

    public GraphTimelineControl(Composite parent) {
        super(parent, "Graph");

        availDomResourcesMap = new HashMap<>();

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
            @Override
            public void widgetSelected(SelectionEvent e) {

                /*
                 * the currently selected dominant resource is being replaced by
                 * another dominant resource in the combo.
                 */
                selectDominantResource(true);
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

    // End of methods from old TimelineControl

    // Methods from old Timeline class
    @Override
    public void setTimelineState(String state, boolean disable) {

        timelineStateMessage = state;

        setControlsEnabled(!disable);
        canvas.redraw();
    }

    /* (non-Javadoc)
     * @see gov.noaa.nws.ncep.viz.resourceManager.timeline.TimelineControl#updateTimeline(gov.noaa.nws.ncep.viz.resources.time_match.NCTimeMatcher)
     */
    @Override
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
                        false);
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
                availTimes = new ArrayList<>();
            } else if (availTimes.isEmpty()) {
                setTimelineState("Timeline Disabled", true);
            } else {
                timelineStateMessage = null;
                setControlsEnabled(true);
            }

            timeData = new TimelineData(availTimes);
            // append data at the end to fill the snap peried
            snapAvailtimes(availTimes);

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
        if (snap != 0 && availTimes != null && availTimes.size() > 0) {

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

    /**
     * Returns a list of the selected data times.
     *
     * @return
     */
    @Override
    public List<Calendar> getSelectedTimes() {
        ArrayList<Calendar> list = new ArrayList<>();
        int selectedRange = timeMatcher.getGraphRange() * 60;

        Calendar firstSelected = timeData.getFirstSelected();
        if (firstSelected == null) {
            return list;
        }

        timeData.deselectAll();
        list.add(firstSelected);
        for (int i = 0; i < selectedRange; i++) {

            firstSelected.add(Calendar.MINUTE, +1);
            Calendar cal = (Calendar) firstSelected.clone();
            list.add(cal);
            timeData.select(cal);
        }

        return list;
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
                         * If click on an available time OUTSIDE of the slider
                         * rectangle, toggle it's status between selected and
                         * not selected.
                         */
                        if (!isInSlider(e.x, e.y) && toggleATime(e.x, e.y)) {
                            return;
                        }

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
                            toggleATime(e.x, e.y);
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
                            int lineY = Math.round(size.y * TIME_LINE);
                            Point beg = new Point(MARGIN, lineY);
                            Point end = new Point(size.x - MARGIN - 1, lineY);

                            double snapDist = (end.x - beg.x)
                                    * snapRatio;

                            double snapNum = Math.abs(xdiff)
                                    / snapDist;
                            int snapNumRound = (int) Math.round(snapNum);

                            switch (mode) {

                            case MOVE_ALL: {

                                int snapX = 0;
                                Calendar firstSelected = timeData
                                        .getFirstSelected();
                                Calendar lastSelected = timeData
                                        .getLastSelected();

                                if (firstSelected != null
                                        && lastSelected != null) {
                                    lastSelected = GraphTimelineUtil
                                            .snapTimeToNext(lastSelected, snap);

                                    int firstSelectedX = currPositionFromTime(firstSelected);//
                                    int lastSelectedX = currPositionFromTime(lastSelected);

                                    Calendar snapped = null;

                                    if (e.x <= saveX) {
                                        snapped = (Calendar) firstSelected
                                                .clone();
                                        snapped.add(Calendar.HOUR_OF_DAY, -snap
                                                * snapNumRound);
                                        snapX = currPositionFromTime(snapped);
                                        xdiff = snapX - firstSelectedX;
                                    } else if (e.x > saveX) {
                                        snapped = (Calendar) lastSelected
                                                .clone();
                                        snapped.add(Calendar.HOUR_OF_DAY, snap
                                                * snapNumRound);
                                        snapX = currPositionFromTime(snapped);
                                        xdiff = snapX - lastSelectedX;
                                    }

                                    moveSlider(sliderStart, xdiff);
                                }
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
            @Override
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
            @Override
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
            @Override
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
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (refTimeCombo.getSelectionIndex() == 0) {
                    timeMatcher.setCurrentRefTime();
                } else if (refTimeCombo.getSelectionIndex() == 1) {
                    timeMatcher.setLatestRefTime();
                } else if (refTimeCombo.getSelectionIndex() == 2) {

                    CalendarSelectDialog calSelDlg = new CalendarSelectDialog(
                            getShell());

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
    @Override
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
            timelineStateMessage = "Timeline Empty";
            return;
        }

        /*
         * draw date line that separates month/days and the hours of day
         */

        int dateY = Math.round(size.y * DATE_LINE);
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

        int lineY = Math.round(size.y * TIME_LINE);
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
        if (slider == null) {
            slider = calculateSlider(begTimeLine, endTimeLine);
        }

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

        if (time1 == null || time2 == null) {
            return new Rectangle(0, 0, 0, 0);
        }

        Calendar prev = timeData.getPreviousTime(time1);
        if (prev != null && timeLocations.get(prev) != null
                && timeLocations.get(time1) != null) {
            ulX = (timeLocations.get(prev) + timeLocations.get(time1)) / 2;
        } else {
            ulX = sliderMin + 5;
        }

        Calendar next = timeData.getNextTime(time2);
        if (next != null && timeLocations.get(next) != null) {
            lastX = (timeLocations.get(time2) + timeLocations.get(next)) / 2;
        } else {
            lastX = sliderMax - 5;
        }

        int ulY = beg.y - SLIDER;
        int width = lastX - ulX;
        int height = 2 * SLIDER;

        return new Rectangle(ulX, ulY, width, height);
    }

    private Calendar currTimeFromPosition(int posX) {
        Point size = canvas.getSize();
        int lineY = Math.round(size.y * TIME_LINE);
        Point beg = new Point(MARGIN, lineY);
        Point end = new Point(size.x - MARGIN - 1, lineY);
        int lineLength = end.x - beg.x;

        double dist = (double) (posX - beg.x) / (double) lineLength;

        Calendar first = timeData.getFirstTime();
        first.setTimeZone(TimeZone.getTimeZone("UTC"));
        Calendar last = timeData.getLastTime();
        last.setTimeZone(TimeZone.getTimeZone("UTC"));

        long timeLength = last.getTimeInMillis() - first.getTimeInMillis();

        long currMills = Math.round(dist * timeLength)
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
        int lineY = Math.round(size.y * TIME_LINE);
        Point beg = new Point(MARGIN, lineY);
        Point end = new Point(size.x - MARGIN - 1, lineY);
        int lineLength = end.x - beg.x;

        curr.setTimeZone(TimeZone.getTimeZone("UTC"));

        double dist = (double) (curr.getTimeInMillis() - first
                .getTimeInMillis()) / (double) timeLength;

        int currX = (int) Math.round(dist * lineLength) + beg.x;

        return currX;
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
        if (center) {
            locY = (beg.y - textHeight) / 2;
        }

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
            if (locX > startX) {
                gc.drawText(hour, locX, locY);
            }

            if (j != 0)
             {
                gc.drawLine(startX, locY, startX, beg.y); // separator
            }

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
            long lineDist = Math.round(dist * lineLength);
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
     * Calculate the boxes and their locations that will be used to represent
     * the available times on the timeline
     */
    private void calculateAvailableBoxes(Point beg, Point end) {

        availableTimes = new LinkedHashMap<>();
        timeLocations = new HashMap<>();

        int lineLength = end.x - beg.x;

        Calendar first = timeData.getStartTime();

        long timeLength = timeData.getTotalMillis();

        for (Calendar curr : timeData.getTimes()) {
            // the last data is next snap 00:00, so don't put it to
            // availableTimes.
            if (timeData.getNextTime(curr) != null) {
                double dist = (double) (curr.getTimeInMillis() - first
                        .getTimeInMillis()) / (double) timeLength;
                long lineDist = Math.round(dist * lineLength);
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
        if (hasDifferentMinutes) {
            sdf.applyPattern("mm");
        }

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
     * Retuns the current skip factor used
     *
     * @return
     */
    public int getHourSnapCombo() {
        return timeMatcher.getHourSnap();
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

    @Override
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

    @Override
    public int getFrameInterval() {
        return timeMatcher.getFrameInterval();
    }

    @Override
    public int getTimeRangeHrs() {
        return timeRangeHrs;
    }

    @Override
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
    @Override
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
        if (start.x + width > sliderMax) {
            width = sliderMax - start.x;
        }
        slider = new Rectangle(start.x, start.y, width, start.height);
    }

    /*
     * update the selected status of each available time that is currently in
     * the slider bar based on current behavior and skip factor
     */
    private void updateSelectedTimes(int xdiff) {

        if (availableTimes == null || slider == null)
         {
            return; // canvas not yet ready
        }

        Calendar first = null;
        Calendar last = null;

        /*
         * determine first and last available data time in slider bar
         */
        for (Rectangle rect : availableTimes.keySet()) {
            if (slider.intersects(rect)) {

                Calendar cal = availableTimes.get(rect);
                if (first == null) {
                    first = cal;
                }
                if (last == null) {
                    last = cal;
                }
                if (cal.before(first)) {
                    first = cal;
                }
                if (cal.after(last)) {
                    last = cal;
                }
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

        if (timeMatcher.isForecast()) {
            timeData.updateRange(first, last, 0);
        } else {
            timeData.updateRange(last, first, 0);
        }

        timeMatcher.setFrameTimes(toDataTimes(timeData.getSelectedTimes()));

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

    /*
     * Find if a click within slider or hits a data time rectangle in the
     * slider.
     *
     * Note that the slider box may fall in the middle of a data time rectangle,
     * while the click is outside of the slider box. This should be considered
     * still within the slider box!
     */
    private boolean isInSlider(int xpos, int ypos) {

        if (slider.contains(xpos, ypos)) {
            return true;
        }

        // Handle specical case when slider box hits in the middle of a data
        // time rectangle.
        Rectangle selected = findATime(xpos, ypos);

        if (selected == null || !slider.intersects(selected)) {
            return false;
        } else {
            return true;
        }
    }

    /*
     * Find if a click hits a data time rectangle on the timeline.
     */
    private Rectangle findATime(int xpos, int ypos) {
        Rectangle selected = null;

        for (Rectangle rect : availableTimes.keySet()) {
            if (rect.contains(xpos, ypos)) {
                selected = rect;
                break;
            }
        }

        return selected;
    }

    /*
     * Toggle a datatime's status between selected and not selected when
     * clicked.
     */
    private boolean toggleATime(int xpos, int ypos) {
        boolean selected = false;

        for (Rectangle rect : availableTimes.keySet()) {
            if (rect.contains(xpos, ypos)) {
                timeData.toggle(availableTimes.get(rect));

                /*
                 * can't turn off only selected time...turn back on
                 */
                if (timeData.numSelected() == 0) {
                    timeData.toggle(availableTimes.get(rect));
                }

                resetSlider();
                canvas.redraw();

                timeMatcher.setFrameTimes(toDataTimes(getSelectedTimes()));

                selected = true;
                break;
            }
        }

        return selected;
    }

}
