package gov.noaa.nws.ncep.viz.resources.time_match;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.lang.Validate;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.time.TimeRange;
import com.raytheon.uf.viz.core.AbstractTimeMatcher;
import com.raytheon.uf.viz.core.drawables.AbstractDescriptor;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.IDescriptor.FrameChangeMode;
import com.raytheon.uf.viz.core.drawables.IDescriptor.FrameChangeOperation;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.LoadProperties;

import gov.noaa.nws.ncep.viz.common.SelectableFrameTimeMatcher;
import gov.noaa.nws.ncep.viz.common.display.INatlCntrsDescriptor;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData.TimeMatchMethod;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData.TimelineGenMethod;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResourceData;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceName;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceName.ResourceNameAdapter;

/**
 * Time matching for Natl Cntrs is based on the dominant source. The data times
 * defined by it are the times for all of the resources. Other resources will
 * need to time match their data to this list of data times.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 08/11/09       145      Greg Hull    Initial creation
 * 10/20/09       145      Greg Hull    setFrameTimes
 * 09/06/10       307      Greg Hull    add timeRange, frameInterval
 * 01/29/11       365      Greg Hull    add latestRefTimeSentinel 
 * 02/01/11       365      Greg Hull    change generateTimes to set frameTimes 
 *                                      to allow for initialization without timelineControl
 * 03/07/11     migration  Greg Hull    rm notifyResourceAdd and change defn of initialLoad
 * 11/29/11       518      Greg Hull    add dfltFrameTimes
 * 06/17/12       713      Greg Hull    typo in copy constr for skipValue
 * 08/27/12       851      Greg Hull    ignore dataTime level when creating new frame Time
 * 02/12/13       972      Greg Hull    changed to work with INatlCntrsDescriptor
 * 04/24/13       689      Xiaochuan    Loop length in slctFrames that is set based on default
 *                                      or size of selectableDataTimes.
 * 05/14/14       1131     Quan Zhou    Added graphRange and hourSnap.  MouModified generateTimeline
 * 07/11/14       TTR1032  J. Wu        No timeline needed if no data times available.
 * 07/28/14       TTR1034+ J. Wu        Build timeline only from available datatimes..
 * 07/29/14       R4078    s. Gurung    Commented out code that sets numFrames=1 for graph/timeseries display
 * 08/25/14       R4097    kbugenhagen  Added EVENT_BEFORE_OR_AFTER time matching
 * 02/16/16       R15244   bkowal       Added {@link #update(NCTimeMatcherSettings)} to set
 *                                      timeline settings based on cached information.
 * 04/20/16       R17324   P. Moyer     Added BINNING_FOR_GRID_RESOURCES time matching.
 *                                      Eliminated redundant generic types.
 * 01/24/2017     R17975   kbugenhagen  Modified generateTimeline, updateFromDominantResource,
 *                                      adjustTimeline to support calendar selects for forecast resource.
 *                                      Cleaned up comments.
 * 
 * </pre>
 * 
 * @author ghull
 * @version 1.0
 */
@XmlAccessorType(XmlAccessType.NONE)
public class NCTimeMatcher extends AbstractTimeMatcher
        implements ISerializableObject {

    protected transient AbstractNatlCntrsRequestableResourceData dominantRscData;

    @XmlAttribute
    @XmlJavaTypeAdapter(ResourceNameAdapter.class)
    protected ResourceName dominantResourceName;

    @XmlAttribute
    protected int numFrames;

    @XmlAttribute
    protected int graphRange;

    @XmlAttribute
    protected int hourSnap;

    @XmlAttribute
    protected int skipValue;

    @XmlAttribute
    protected int timeRange; // in hours

    @XmlAttribute
    protected int frameInterval; // if -1 then use data times

    private final static DataTime latestRefTimeSentinel = new DataTime(
            new Date(0));

    @XmlElement
    protected DataTime refTime; // if null, use current

    // all the times in the db based on the dominant resource
    private List<DataTime> allAvailDataTimes;

    // all the times in the time line based on the dominant resource
    private List<DataTime> selectableDataTimes;

    // the frame times that will be used for the RBD
    protected List<DataTime> frameTimes;

    // ie GDATTIME used to set the initial frame times
    @XmlAttribute
    protected String dfltFrameTimesStr;

    private final int dfltNumFrames = 0;

    private final int dfltGraphRange = 0;

    private final int dfltHourSnap = 0;

    private final int dfltSkipFrames = 0;

    private boolean timesLoaded = false;

    private boolean isForecast = false;

    private final ArrayList<INatlCntrsDescriptor> descriptorList;

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(NCTimeMatcher.class);

    /**
     * Default Constructor.
     */
    public NCTimeMatcher() {
        super();
        descriptorList = new ArrayList<>();
        dominantRscData = null;
        dominantResourceName = null;
        allAvailDataTimes = new ArrayList<>();
        selectableDataTimes = new ArrayList<>();
        frameTimes = new ArrayList<>();
        numFrames = dfltNumFrames;
        graphRange = dfltGraphRange;
        hourSnap = dfltHourSnap;
        skipValue = dfltSkipFrames;
        timeRange = 0; // set from dominant resource
        frameInterval = -1;
        timeRange = 0;
        refTime = null;
        timesLoaded = false;
        isForecast = false;
        setCurrentRefTime();
    }

    public NCTimeMatcher(NCTimeMatcher tm) {
        super();
        descriptorList = new ArrayList<>();
        allAvailDataTimes = new ArrayList<>(tm.allAvailDataTimes);
        selectableDataTimes = new ArrayList<>(tm.selectableDataTimes);
        frameTimes = new ArrayList<>(tm.frameTimes);
        timesLoaded = tm.timesLoaded;
        numFrames = tm.numFrames;
        graphRange = tm.graphRange;
        hourSnap = tm.hourSnap;
        skipValue = tm.skipValue;
        timeRange = tm.timeRange;
        refTime = (tm.refTime == null ? null
                : new DataTime(tm.refTime.getRefTime(),
                        tm.refTime.getFcstTime()));
        frameInterval = tm.frameInterval;
        dominantRscData = tm.dominantRscData;
        dominantResourceName = new ResourceName(tm.getDominantResourceName());
        isForecast = tm.isForecast;
    }

    public void addDescriptor(INatlCntrsDescriptor desc) {
        if (!descriptorList.contains(desc)) {
            descriptorList.add(desc);
        }
    }

    public void removeDescriptor(INatlCntrsDescriptor desc) {
        if (descriptorList.contains(desc)) {
            descriptorList.remove(desc);
        }
    }

    public ResourceName getDominantResourceName() {
        return dominantResourceName;
    }

    public boolean isForecast() {
        return isForecast;
    }

    public void setForecast(boolean isForecast) {
        this.isForecast = isForecast;
    }

    public int getNumFrames() {
        return numFrames;
    }

    public void setNumFrames(int numFrames) {
        this.numFrames = numFrames;
    }

    public int getHourSnap() {
        return hourSnap;
    }

    public void setHourSnap(int hourSnap) {
        this.hourSnap = hourSnap;
    }

    public int getGraphRange() {
        return graphRange;
    }

    public void setGraphRange(int graphRange) {
        this.graphRange = graphRange;
    }

    public int getSkipValue() {
        return skipValue;
    }

    public void setSkipValue(int skip) {
        this.skipValue = skip;
    }

    public int getTimeRange() {
        return timeRange;
    }

    public void setTimeRange(int timeRange) {
        this.timeRange = timeRange;
    }

    public int getFrameInterval() {
        return frameInterval;
    }

    public void setFrameInterval(int frameInterval) {
        this.frameInterval = frameInterval;
    }

    public List<DataTime> getFrameTimes() {
        return frameTimes;
    }

    public void setFrameTimes(ArrayList<DataTime> ft) {
        frameTimes = ft;

        if (this.getDominantResourceName() != null
                && !this.getDominantResourceName().getRscCategory()
                        .getCategoryName().equals("TIMESERIES"))
            numFrames = frameTimes.size();
        else
            numFrames = this.getGraphRange() * 60;
    }

    public List<DataTime> getSelectableDataTimes() {
        return selectableDataTimes;
    }

    void setSelectableDataTimes(ArrayList<DataTime> selDataTimes) {
        this.selectableDataTimes = selDataTimes;
    }

    public DataTime getRefTime() {
        return refTime;
    }

    public void setRefTime(DataTime refTime) {
        this.refTime = refTime;
    }

    public void setLatestRefTime() {
        refTime = latestRefTimeSentinel;
    }

    public void setCurrentRefTime() {
        refTime = null;
    }

    public boolean isLatestRefTime() {
        return (refTime == latestRefTimeSentinel);
    }

    public boolean isCurrentRefTime() {
        return (refTime == null);
    }

    public boolean isDataAvailable() {
        return !allAvailDataTimes.isEmpty();
    }

    public AbstractNatlCntrsRequestableResourceData getDominantResource() {
        return dominantRscData;
    }

    public boolean isAutoUpdateable() {
        return (dominantRscData == null ? false
                : dominantRscData.isAutoUpdateable());
    }

    public void setDominantResourceData(
            AbstractNatlCntrsRequestableResourceData domRscData) {
        dominantRscData = domRscData;
        if (dominantRscData == null) {
            dominantResourceName = null;
            isForecast = false;
        } else {
            dominantResourceName = dominantRscData.getResourceName();
            isForecast = dominantRscData.isForecastResource();
        }
    }

    // set the dominant resource and update the frameTimes
    public void updateFromDominantResource() {
        if (dominantRscData == null) {
            this.clearSettings();
            return;
        }

        numFrames = dominantRscData.getDfltNumFrames();
        graphRange = dominantRscData.getDfltGraphRange();
        hourSnap = dominantRscData.getDfltHourSnap();
        timeRange = dominantRscData.getDfltTimeRange();
        skipValue = 0; // no default but reset to 0
        isForecast = dominantRscData.isForecastResource();
        dfltFrameTimesStr = dominantRscData.getDfltFrameTimes();
        if (dominantRscData.getResourceName().getCycleTime() != null) {
            refTime = null;
        } else {
            setCurrentRefTime();
        }
        this.update();
        loadTimes(true);
    }

    public void update(NCTimeMatcherSettings timeSettings) {
        if (dominantRscData == null) {
            this.clearSettings();
            return;
        }

        numFrames = (timeSettings.getNumberFrames() == null)
                ? this.dominantRscData.getDfltNumFrames()
                : timeSettings.getNumberFrames();
        graphRange = dominantRscData.getDfltGraphRange();
        hourSnap = dominantRscData.getDfltHourSnap();
        timeRange = (timeSettings.getTimeRange() == null)
                ? this.dominantRscData.getDfltTimeRange()
                : timeSettings.getTimeRange();
        skipValue = (timeSettings.getSkipValue() == null) ? 0
                : timeSettings.getSkipValue();
        isForecast = dominantRscData.isForecastResource();
        dfltFrameTimesStr = dominantRscData.getDfltFrameTimes();
        if (timeSettings.getFrameInterval() != null) {
            frameInterval = timeSettings.getFrameInterval();
        } else {
            this.update();
        }
        if (timeSettings.getRefTimeSelection() != null) {
            switch (timeSettings.getRefTimeSelection()) {
            case CALENDAR:
                this.setRefTime(
                        new DataTime(timeSettings.getSelectedRefTime()));
                break;
            case CURRENT:
                this.setCurrentRefTime();
                break;
            case LATEST:
                this.setLatestRefTime();
                break;
            }
        } else if (timeSettings.getForecastRefTimeSelection() != null) {
            switch (timeSettings.getForecastRefTimeSelection()) {
            case CALENDAR:
                this.setRefTime(
                        new DataTime(timeSettings.getSelectedRefTime()));
                break;
            case CYCLE_TIME:
                this.setCurrentRefTime();
                break;
            }
        }
        loadTimes(true);
    }

    private void update() {
        TimelineGenMethod tLineGenMthd = dominantRscData.getTimelineGenMethod();

        // the frameInterval here is only used to generate the timeline.
        if (tLineGenMthd == TimelineGenMethod.USE_DATA_TIMES
                || tLineGenMthd == TimelineGenMethod.USE_CYCLE_TIME_FCST_HOURS) {
            frameInterval = -1;
        } else if (tLineGenMthd == TimelineGenMethod.USE_MANUAL_TIMELINE) {
            if (frameInterval <= 0) {
                frameInterval = 60; // what to use here as a default
            }
        } else if (tLineGenMthd == TimelineGenMethod.USE_FRAME_INTERVAL
                || tLineGenMthd == TimelineGenMethod.USE_FCST_FRAME_INTERVAL_FROM_REF_TIME
                || tLineGenMthd == TimelineGenMethod.DETERMINE_FROM_RSC_IMPLEMENTATION) {
            frameInterval = dominantRscData.getFrameSpan();
        } else {
            return;
        }
    }

    private void clearSettings() {
        frameTimes.clear();
        selectableDataTimes.clear();
        numFrames = 0;
        graphRange = 0;
        frameInterval = -1;
        timeRange = 0;
        isForecast = false;
        refTime = null;
    }

    /**
     * if refTime is null, then either use the most recent data as the refTime
     * or the cycle time for forecast data.
     * 
     * @return false if no data found
     */
    public boolean generateTimeline() {

        frameTimes.clear();
        selectableDataTimes.clear();

        List<DataTime> selDataTimes = new ArrayList<>();

        if (dominantRscData == null) {
            return false;
        } else if (frameInterval == 0) {
            return false;
        }

        long frameIntervalMillisecs = ((long) frameInterval) * 60 * 1000;
        long timeRangeMillisecs = ((long) timeRange) * 60 * 60 * 1000;

        /*
         * determine the reference time to use. refTime is marshalled out to the
         * bundle file and may be null or 'latest' or a time set by the user.
         */
        long refTimeMillisecs = 0;
        Calendar refTimeCal = null;

        /*
         * check cycleTime instead of isForecast since some resources may be
         * forecast w/o a cycletime
         */
        if (dominantResourceName.getCycleTime() != null) {
            if (!dominantResourceName.isLatestCycleTime()) {
                DataTime cycleTime = dominantResourceName.getCycleTime();
                refTimeMillisecs = (cycleTime == null ? 0
                        : cycleTime.getRefTime().getTime());
            }
            if (isLatestRefTime()) {
                refTimeMillisecs = 0;
            } else if (!isCurrentRefTime()) {
                // calendar selected
                refTimeCal = refTime.getRefTimeAsCalendar();
                refTimeMillisecs = refTime.getRefTime().getTime();
                // Remove seconds and milliseconds
                DataTime refTimeNoMillisecs = convertStringToDataTimeWithoutSeconds(
                        refTime.toString());
                refTimeCal = refTimeNoMillisecs.getRefTimeAsCalendar();
                refTimeMillisecs = refTimeNoMillisecs.getRefTime().getTime();
            }
        } else if (isCurrentRefTime()) {
            refTimeMillisecs = Calendar.getInstance().getTimeInMillis();
            refTimeCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        } else if (isLatestRefTime()) {
            refTimeMillisecs = 0;
        } else {
            refTimeCal = refTime.getRefTimeAsCalendar();
            refTimeMillisecs = refTime.getRefTime().getTime();
        }

        /*
         * Always check all available times. If none of the available data times
         * falls within the specified time range, then no time line should be
         * created.
         */
        allAvailDataTimes = dominantRscData.getAvailableDataTimes();

        List<DataTime> availTimes = allAvailDataTimes;
        if (availTimes == null || availTimes.isEmpty()) {
            return false;
        } else {
            long latestTime = availTimes.get(availTimes.size() - 1).getRefTime()
                    .getTime();
            if (latestTime < (refTimeMillisecs - timeRangeMillisecs)) {
                return false;
            }
        }

        /*
         * if refTime is Latest or if using the data to generate the timeline,
         * or if we need to get the cycle time for a forecast resource, then we
         * will need to query the times of the dominant resource.
         */
        if (refTimeMillisecs == 0 || frameInterval == -1) {

            if (allAvailDataTimes == null) { // no data
                allAvailDataTimes = new ArrayList<>();
                return false;
            }

            // added sort
            GraphTimelineUtil.sortAvailableData(allAvailDataTimes);

            // if refTime is not given (ie Latest) then get it from the data
            if (refTimeMillisecs == 0) {
                if (!allAvailDataTimes.isEmpty()) {
                    refTime = allAvailDataTimes.get(
                            (isForecast ? 0 : allAvailDataTimes.size() - 1));
                    refTimeCal = refTime.getRefTimeAsCalendar();
                    refTimeMillisecs = refTime.getRefTime().getTime();
                } else {
                    return false;
                }
            }

            // extend refTime to the snap point
            if (this.getDominantResourceName().getRscCategory()
                    .getCategoryName().equals("TIMESERIES")
                    && this.getHourSnap() != 0) {
                refTimeMillisecs = GraphTimelineUtil
                        .snapTimeToNext(refTimeCal, this.getHourSnap())
                        .getTimeInMillis();
            }

            /*
             * if generating times from the data then get only those times in
             * the selected range.
             */
            if (frameInterval == -1) {
                long oldestTimeMs = (isForecast ? refTimeMillisecs
                        : refTimeMillisecs - timeRangeMillisecs - 1);
                long latestTimeMs = (isForecast
                        ? refTimeMillisecs + timeRangeMillisecs + 1
                        : refTimeMillisecs);

                for (DataTime time : allAvailDataTimes) {
                    long timeMS = time.getValidTime().getTimeInMillis();
                    if (timeMS >= oldestTimeMs && timeMS <= latestTimeMs) {
                        selectableDataTimes.add(time);
                    }
                }
            }
        }

        // no data found
        if (selectableDataTimes.size() == 0) {
            return false;
        }
        /*
         * if a frameInterval is set then use it to create a list of times
         * within the defined range.
         */
        if (frameInterval > 0) {

            long frameTimeMillisecs = refTimeMillisecs;

            if (isForecast) {
                while (frameTimeMillisecs <= refTimeMillisecs
                        + timeRangeMillisecs) {
                    DataTime time = new DataTime(new Date(frameTimeMillisecs));
                    selDataTimes.add(time);
                    frameTimeMillisecs += frameIntervalMillisecs;
                }
            } else {
                while (frameTimeMillisecs >= refTimeMillisecs
                        - timeRangeMillisecs) {
                    DataTime normRefTime = getNormalizedTime(
                            new DataTime(new Date(frameTimeMillisecs)));
                    selDataTimes.add(0, normRefTime);
                    frameTimeMillisecs -= frameIntervalMillisecs;
                }
            }
        }

        /*
         * Now check which frame ACTUALLY have data --- Originally, all
         * FrameTime are added as select-able without check.
         * 
         * Note that for EVENT type resources, all frames will still be added
         * select-able as before.
         */
        if (selDataTimes.size() > 0) {
            for (DataTime dt : selDataTimes) {
                FrameDataContainer fdc = new FrameDataContainer(dt,
                        frameInterval, dominantRscData);
                if (fdc.isDataTimeInFrame(allAvailDataTimes)) {
                    selectableDataTimes.add(dt);
                }
            }
        }
        //

        SelectableFrameTimeMatcher frmTimeMatcher = null;

        /*
         * if there is a GDATTIM then use this to compute the frameList from the
         * list of available times in the DB
         */
        if (dfltFrameTimesStr != null) {
            try {
                frmTimeMatcher = new SelectableFrameTimeMatcher(
                        dfltFrameTimesStr);

                for (DataTime dt : allAvailDataTimes) {
                    if (frmTimeMatcher.matchTime(dt)) {
                        frameTimes.add(dt);
                    }
                }

            } catch (VizException e) {
                // sanity check since this should already be validated
                statusHandler.error("bad GDATTIM string:" + dfltFrameTimesStr);
                frmTimeMatcher = null;
                return false;
            }

        } else {
            int skipCount = 0;
            // set the initial frameTimes from the skip value and numFrames
            GraphTimelineUtil.sortAvailableData(selectableDataTimes);

            // For graph display, numFrames = 1

            for (skipCount = 0; skipCount < selectableDataTimes
                    .size(); skipCount++) {
                if (skipCount % (skipValue + 1) == 0) {

                    DataTime selectableTime = (isForecast
                            ? selectableDataTimes.get(skipCount)
                            : selectableDataTimes.get(selectableDataTimes.size()
                                    - skipCount - 1));

                    if (frmTimeMatcher == null
                            || frmTimeMatcher.matchTime(selectableTime)) {

                        if (isForecast) {
                            frameTimes.add(selectableTime);
                        } else {
                            frameTimes.add(0, selectableTime);
                        }
                    }
                }
                if (frameTimes.size() == numFrames) {
                    break;
                }
            }
        }

        if (frameTimes.size() > 0) {
            numFrames = frameTimes.size();
        }

        return true;
    }

    // YYYY-MM-DD HH:MM:SS -> DataTime
    public static DataTime convertStringToDataTimeWithoutSeconds(
            String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return null;
        }
        int yy = Integer.parseInt(timeStr.substring(0, 4));
        int mon = Integer.parseInt(timeStr.substring(5, 7)) - 1;
        int dd = Integer.parseInt(timeStr.substring(8, 10));
        int hh = Integer.parseInt(timeStr.substring(11, 13));
        int min = Integer.parseInt(timeStr.substring(14, 16));

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.set(yy, mon, dd, hh, min, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return new DataTime(cal);
    }

    /*
     * This is called by the dominant resource when updated data arrives. It
     * will determine if one or more frames times are needed for the new data.
     * note that these times are not actually added to the timeline until the
     * auto-update occurs.
     */
    public ArrayList<DataTime> determineNewFrameTimes(DataTime newDataTime) {
        /*
         * in somecases the dataTime from the data can have a level set which
         * can mess up the equals() method.
         */
        DataTime newFrameTime = new DataTime(newDataTime.getValidTime());

        ArrayList<DataTime> newFrameTimes = new ArrayList<>(1);

        /*
         * if the timeline was created using DATA_TIMES then the new time is the
         * new frame time unless it is already in the frameTimes.
         */
        if (frameInterval == -1) {
            if (!frameTimes.contains(newFrameTime)) {
                newFrameTimes.add(newFrameTime);
            }
        } else if (frameInterval != 0) {
            // if MANUAL or FRAME_TIMES
            if (!isForecast) {
                long lastFrameTimeMs = frameTimes.get(frameTimes.size() - 1)
                        .getValidTime().getTime().getTime();
                long nextFrameTimeMs = lastFrameTimeMs;

                // current time isn't appropriate if we are using archive data

                /*
                 * This will create frames up to the current time which usually
                 * won't occur but is possible if no updates are received for a
                 * complete frame.
                 */
                while ((nextFrameTimeMs
                        + (frameInterval * 1000 * 60)) < newFrameTime
                                .getValidTime().getTime().getTime()) {
                    nextFrameTimeMs += frameInterval * 1000 * 60;
                    newFrameTimes.add(new DataTime(new Date(nextFrameTimeMs)));
                }
            }
        }

        return newFrameTimes;
    }

    /*
     * On autoUpdate this will modify the generated timeline to add the new
     * times and drop the oldest. The descriptors are also updated with the new
     * frameTimes. newTimes will be sorted and will not be empty.
     */
    public void updateTimeline(ArrayList<DataTime> newTimes) {

        int frmCnt = frameTimes.size();
        int numFramesToStep = 0;

        // for each new frame time.
        for (DataTime newTime : newTimes) {
            /*
             * insert each time into the list in order and then remove the
             * oldest time.
             */
            if (!frameTimes.contains(newTime)) {
                // if newer than the last, add it to the end of the list.
                if (newTime.greaterThan(frameTimes.get(frmCnt - 1))) {
                    frameTimes.add(newTime);
                    frameTimes.remove(0);
                    numFramesToStep++;
                } else {
                    for (int f = 0; f < frmCnt; f++) {
                        if (frameTimes.get(f).greaterThan(newTime)) {
                            frameTimes.add(f, newTime);
                            frameTimes.remove(0);
                            numFramesToStep++;
                            break;
                        }
                    }
                }
            }
        }

        /*
         * update the times in the descriptors which will in turn update the
         * timeline for the resources in the descriptor.
         */
        for (INatlCntrsDescriptor ncDescr : descriptorList) {
            ncDescr.updateDataTimes(frameTimes.toArray(new DataTime[0]));

            /*
             * Since we want to keep the resources displaying the same frame
             * data and we have just changed the timeline, we need to change to
             * the previous frame. (Note: this will also trigger the
             * frameChanged listeners so that the frameTime on the status bar is
             * updated.)
             */
            while (numFramesToStep-- > 0) {
                ncDescr.changeFrame(FrameChangeOperation.PREVIOUS,
                        FrameChangeMode.TIME_ONLY);
            }
        }
    }

    //
    public DataTime getNormalizedTime(DataTime time) {
        if (frameInterval <= 0) {
            /*
             * just returning time here was causing problems when toString is
             * called
             */
            return new DataTime(time.getRefTime(), time.getFcstTime());
        }
        // convert minutes to milliseconds
        long frameIntervalMillis = frameInterval * 1000 * 60;
        long millis = time.getValidTime().getTimeInMillis()
                + (frameIntervalMillis / 2);
        millis = ((millis / frameIntervalMillis) * frameIntervalMillis);
        return new DataTime(new Date(millis));
    }

    /*
     * added adjustTimeline() to adjust time line with "false" - it updates
     * available times from DB but keeps selected frame times intact. If "true"
     * is used, the time line will be completely rebuilt - available times are
     * updated, the selected frames times are cleared and then re-built with
     * "numFrames" and "skip" factor.
     */
    public boolean loadTimes(boolean reloadTimes) {

        if (!timesLoaded) {
            generateTimeline();
            timesLoaded = true;
        } else {
            if (reloadTimes) {
                generateTimeline();
            } else {
                adjustTimeline();
            }
        }

        return timesLoaded;
    }

    /*
     * This is called by raytheon's NcAutoUpdater
     * 
     * Assume that the frameTimes have been changed. We will need to update the
     * descriptor's timeMatchingMap with the new frameTimes for all of the
     * existing resources.
     */
    @Override
    public void redoTimeMatching(IDescriptor descriptor) throws VizException {
        Validate.isTrue(descriptor instanceof AbstractDescriptor, ""
                + this.getClass().getName() + " depends on AbstractDescriptor");
    }

    @Override
    public void redoTimeMatching(AbstractVizResource<?, ?> resource) {

    }

    @Override
    public void handleRemove(AbstractVizResource<?, ?> resource,
            IDescriptor descriptor) {

        if (resource instanceof AbstractNatlCntrsResource<?, ?>) {

            if (dominantRscData != null && dominantRscData.getResourceName()
                    .equals(((INatlCntrsResourceData) resource
                            .getResourceData()).getResourceName())) {
            }

            dominantRscData = null;
        }
    }

    @Override
    public DataTime[] initialLoad(LoadProperties loadProps,
            DataTime[] availableTimes, IDescriptor descriptor)
                    throws VizException {
        return frameTimes.toArray(new DataTime[0]);
    }

    /*
     * A private class modeled from the inner class
     * AbstractNatlCntrsResource.AbstractFrameData to help check if a given
     * DataTime matches a given FrameTime, based on the TimeMatchMethod.
     */
    private class FrameDataContainer {

        protected DataTime frameTime;

        protected boolean populated;

        protected DataTime startTime; // valid times without a forecast hour

        protected DataTime endTime;

        protected long startTimeMillis;

        protected long endTimeMillis;

        AbstractNatlCntrsRequestableResourceData resourceData;

        /*- set the frame start and end time based on:
           - frame time (from dominant resource),
           - the frameInterval for this resource (may be different than the
             frame interval used to generate the timeline) and
           - the timeMatchMethod.
        */
        public FrameDataContainer(DataTime ftime, int frameInterval,
                AbstractNatlCntrsRequestableResourceData resourceData) {

            this.resourceData = resourceData;

            /*
             * if there is a validPeriod or levels, ignore them and just use the
             * valid time.
             */
            this.frameTime = new DataTime(ftime.getRefTime(),
                    ftime.getFcstTime());
            this.populated = false;
            long frameMillis = frameTime.getValidTime().getTimeInMillis();

            switch (resourceData.getTimeMatchMethod()) {

            case EXACT: {
                startTime = new DataTime(frameTime.getValidTime());
                endTime = new DataTime(frameTime.getValidTime());
            }

                /*
                 * Note : Currently this is implemented the same as Exact. (ie
                 * the frame time must be between the start/end time of an
                 * event.) A more general algorithm could be implemented to use
                 * the frame span and test whether any part of the event
                 * overlaps with any part of the frame span. But currently,for
                 * Event resources,the frame span is taken as the default frame
                 * interval for a manual timeline and so this would need to be
                 * addressed first.)
                 */

            case EVENT: {
                startTime = new DataTime(frameTime.getValidTime());
                endTime = new DataTime(frameTime.getValidTime());
            }
            case EVENT_BEFORE_OR_AFTER: {
                startTime = new DataTime(
                        new Date(frameMillis - frameInterval * 1000 * 60 / 2));
                endTime = new DataTime(new Date(
                        frameMillis + frameInterval * 1000 * 60 / 2 - 1000));
                break;
            }
            case CLOSEST_BEFORE_OR_AFTER: {
                startTime = new DataTime(
                        new Date(frameMillis - frameInterval * 1000 * 60 / 2));
                endTime = new DataTime(new Date(
                        frameMillis + frameInterval * 1000 * 60 / 2 - 1000));
                break;
            }
            case CLOSEST_BEFORE_OR_EQUAL: {
                startTime = new DataTime(
                        new Date(frameMillis - frameInterval * 1000 * 60));
                endTime = new DataTime(frameTime.getValidTime());
                break;
            }
            case CLOSEST_AFTER_OR_EQUAL: {
                startTime = new DataTime(frameTime.getValidTime());
                endTime = new DataTime(new Date(
                        frameMillis + frameInterval * 1000 * 60 - 1000));
                break;
            }
            case BEFORE_OR_EQUAL: {
                startTime = new DataTime(new Date(0));
                endTime = new DataTime(frameTime.getValidTime());
                break;
            }
                // This could be implemented by setting the frame span to
                // infinite.
            case MATCH_ALL_DATA: {
                startTime = new DataTime(new Date(0));
                endTime = new DataTime(new Date(Long.MAX_VALUE));
                break;
            }
            case BINNING_FOR_GRID_RESOURCES: {
                startTime = new DataTime(
                        new Date(frameMillis - frameInterval * 1000 * 60 / 2));
                endTime = new DataTime(new Date(
                        frameMillis + frameInterval * 1000 * 60 / 2 - 1000));
                break;
            }
            default: {
                String msg = resourceData.getTimeMatchMethod().toString()
                        + " is not a valid frame time match method.";
                statusHandler.error("FrameDataContainer(): ", msg);
                break;
            }
            }

            startTimeMillis = startTime.getValidTime().getTimeInMillis();
            endTimeMillis = endTime.getValidTime().getTimeInMillis();
        }

        /*
         * return -1 if the data doesn't match. if the return value is 0 or
         * positive then this is the number of seconds from the perfect match.
         */
        public long timeMatch(DataTime dataTime) {

            long dataTimeMillis = dataTime.getValidTime().getTimeInMillis();
            TimeRange dataTimeRange = dataTime.getValidPeriod();
            long frameTimeMillis = frameTime.getValidTime().getTimeInMillis();

            switch (resourceData.getTimeMatchMethod()) {

            case MATCH_ALL_DATA:
                // everything is a perfect match (E.g PGEN)
                return 0;

            case EXACT: {
                if (dataTimeRange.isValid()) {
                    if (dataTimeRange.getStart().getTime() <= frameTimeMillis
                            && frameTimeMillis <= dataTimeRange.getEnd()
                                    .getTime()) {
                        return 0;
                    } else {
                        return -1;
                    }
                } else {
                    return (frameTimeMillis == dataTimeMillis ? 0 : -1);
                }
            }

                /*
                 * For "EVENT", which is mainly used for MISC data, we cannot do
                 * a time-match here since the data actually has a valid time
                 * and we do not have that info here. SO we return 0 to match
                 * the behavior in NMAP2.
                 */
            case EVENT: {
                return 0;
            }

            case EVENT_BEFORE_OR_AFTER: {
                if (startTimeMillis >= dataTimeMillis
                        || dataTimeMillis > endTimeMillis) {
                    return -1;
                } else {
                    return Math.abs(frameTime.getValidTime().getTime().getTime()
                            - dataTimeMillis) / 1000;
                }
            }

                // mainly for lightning.
            case BEFORE_OR_EQUAL: {
                return (dataTimeMillis > endTimeMillis ? -1
                        : (endTimeMillis - dataTimeMillis) / 1000);
            }

            case CLOSEST_BEFORE_OR_AFTER:
            case CLOSEST_BEFORE_OR_EQUAL:
            case CLOSEST_AFTER_OR_EQUAL: {
                /*
                 * This should be an invalid case. if this is an event type
                 * resource then it should be an EXACT time match. Still, for
                 * now leave this logic in here.
                 */
                if (dataTimeRange.isValid()) {
                    statusHandler
                            .error("Timematching a dataTime with a valid interval with a non-EXACT\n "
                                    + "TimeMatchMethod.");
                    return -1;
                }

                /*
                 * return -1 if this is not a match (since the start/end times
                 * are based on the timeMatchMethod, we can just check that the
                 * datatime is not within the start/end)
                 */
                if (startTimeMillis >= dataTimeMillis
                        || dataTimeMillis > endTimeMillis) {
                    return -1;
                } else if (resourceData
                        .getTimeMatchMethod() == TimeMatchMethod.CLOSEST_BEFORE_OR_EQUAL) {
                    return (endTimeMillis - dataTimeMillis) / 1000;
                } else if (resourceData
                        .getTimeMatchMethod() == TimeMatchMethod.CLOSEST_AFTER_OR_EQUAL) {
                    return (dataTimeMillis - startTimeMillis) / 1000;
                } else if (resourceData
                        .getTimeMatchMethod() == TimeMatchMethod.CLOSEST_BEFORE_OR_AFTER) {
                    return Math.abs(frameTime.getValidTime().getTime().getTime()
                            - dataTimeMillis) / 1000;
                }
            }
            case BINNING_FOR_GRID_RESOURCES: {
                // If Data Time >= Frame Time, It Is A Match
                if (dataTimeMillis >= frameTimeMillis) {
                    return 1;
                } else {
                    return -1;
                }
            }

            }
            return -1;
        }

        // Check if a DataTime is within a Frame.
        final public boolean isDataTimeInFrame(DataTime dataTime) {
            return (dataTime == null ? false : timeMatch(dataTime) >= 0);
        }

        // Check if there is at least one DataTime is within a Frame.
        final public boolean isDataTimeInFrame(List<DataTime> dataTimes) {
            boolean matched = false;
            if (dataTimes == null || dataTimes.isEmpty()) {
                matched = false;
            } else {
                for (DataTime dt : dataTimes) {
                    if (timeMatch(dt) >= 0) {
                        matched = true;
                        break;
                    }
                }
            }

            return matched;
        }
    }

    /*
     * From generateTimeline() - The difference is that even the data times are
     * refreshed as well, but the frameTimes will remain unchanged and only
     * updated when ALL previous-selected frame times are gone.
     */
    public boolean adjustTimeline() {

        List<DataTime> selDataTimes = new ArrayList<>();

        selectableDataTimes.clear();

        if (dominantRscData == null) {
            return false;
        } else if (frameInterval == 0) {
            return false;
        }

        long frameIntervalMillisecs = ((long) frameInterval) * 60 * 1000;
        long timeRangeMillisecs = ((long) timeRange) * 60 * 60 * 1000;

        /*
         * determine the reference time to use. refTime is marshalled out to the
         * bundle file and may be null or 'latest' or a time set by the user.
         */
        long refTimeMillisecs = 0;
        Calendar refTimeCal = null;

        if (!dominantResourceName.isLatestCycleTime()) {
            DataTime cycleTime = dominantResourceName.getCycleTime();
            refTimeMillisecs = (cycleTime == null ? 0
                    : cycleTime.getRefTime().getTime());
            if (isLatestRefTime()) {
                refTimeMillisecs = 0;
            } else if (!isCurrentRefTime()) {
                // calendar selected
                refTimeCal = refTime.getRefTimeAsCalendar();
                refTimeMillisecs = refTime.getRefTime().getTime();

                DataTime refTimeNoMillisecs = convertStringToDataTimeWithoutSeconds(
                        refTime.toString());
                refTimeCal = refTimeNoMillisecs.getRefTimeAsCalendar();
                refTimeMillisecs = refTimeNoMillisecs.getRefTime().getTime();
            }
        } else if (isCurrentRefTime()) {
            // if (isCurrentRefTime()) {
            refTimeMillisecs = Calendar.getInstance().getTimeInMillis();
            refTimeCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        } else if (isLatestRefTime()) {
            refTimeMillisecs = 0;
        } else {
            refTimeCal = refTime.getRefTimeAsCalendar();
            refTimeMillisecs = refTime.getRefTime().getTime();
        }

        /*
         * Always check all available times. If none of the available data times
         * falls within the specified time range, then no time line should be
         * created.
         * 
         * Note: for auto-update-able data such as SAT, looks like there is a
         * delay for getAvailableDataTimes() to pick up the new data frames -
         * needs some investigation........ 08/12/2014 ....
         */
        allAvailDataTimes = dominantRscData.getAvailableDataTimes();

        List<DataTime> availTimes = allAvailDataTimes;
        if (availTimes == null || availTimes.isEmpty()) {
            return false;
        } else {
            long latestTime = availTimes.get(availTimes.size() - 1).getRefTime()
                    .getTime();
            if (latestTime < (refTimeMillisecs - timeRangeMillisecs)) {
                return false;
            }
        }

        /*
         * if refTime is Latest or if using the data to generate the timeline,
         * or if we need to get the cycle time for a forecast resource, then we
         * will need to query the times of the dominant resource.
         */
        if (refTimeMillisecs == 0 || frameInterval == -1) {
            if (allAvailDataTimes == null) { // no data
                allAvailDataTimes = new ArrayList<>(); //
                return false;
            }

            GraphTimelineUtil.sortAvailableData(allAvailDataTimes);

            // if refTime is not given (ie Latest) then get it from the data
            if (refTimeMillisecs == 0) {
                if (!allAvailDataTimes.isEmpty()) {
                    refTime = allAvailDataTimes.get(
                            (isForecast ? 0 : allAvailDataTimes.size() - 1));
                    refTimeCal = refTime.getRefTimeAsCalendar();
                    refTimeMillisecs = refTime.getRefTime().getTime();
                } else {
                    return false;
                }
            }

            // extend refTime to the snap point
            if (this.getDominantResourceName().getRscCategory()
                    .getCategoryName().equals("TIMESERIES")
                    && this.getHourSnap() != 0) {
                refTimeMillisecs = GraphTimelineUtil
                        .snapTimeToNext(refTimeCal, this.getHourSnap())
                        .getTimeInMillis();
            }

            /*
             * if generating times from the data then get only those times in
             * the selected range.
             */
            if (frameInterval == -1) {

                long oldestTimeMs = (isForecast ? refTimeMillisecs
                        : refTimeMillisecs - timeRangeMillisecs - 1);

                long latestTimeMs = (isForecast
                        ? refTimeMillisecs + timeRangeMillisecs + 1
                        : refTimeMillisecs);

                for (DataTime time : allAvailDataTimes) {
                    long timeMS = time.getValidTime().getTimeInMillis();
                    if (timeMS >= oldestTimeMs && timeMS <= latestTimeMs) {
                        selectableDataTimes.add(time);
                    }
                }
            }
        }

        /*
         * if a frameInterval is set then use it to create a list of times
         * within the defined range.
         */
        if (frameInterval > 0) {
            long frameTimeMillisecs = refTimeMillisecs;
            if (isForecast) {
                while (frameTimeMillisecs <= refTimeMillisecs
                        + timeRangeMillisecs) {
                    DataTime time = new DataTime(new Date(frameTimeMillisecs));
                    selDataTimes.add(time);
                    frameTimeMillisecs += frameIntervalMillisecs;
                }
            } else {
                while (frameTimeMillisecs >= refTimeMillisecs
                        - timeRangeMillisecs) {
                    DataTime normRefTime = getNormalizedTime(
                            new DataTime(new Date(frameTimeMillisecs)));
                    selDataTimes.add(0, normRefTime);
                    frameTimeMillisecs -= frameIntervalMillisecs;
                }
            }
        }

        /*
         * Now check which frame ACTUALLY have data --- Originally, all
         * FrameTime are added as select-able without check.
         * 
         * Note that for EVENT type resources, all frames will still be added
         * select-able as before.
         */
        if (selDataTimes.size() > 0) {
            for (DataTime dt : selDataTimes) {
                FrameDataContainer fdc = new FrameDataContainer(dt,
                        frameInterval, dominantRscData);
                if (fdc.isDataTimeInFrame(allAvailDataTimes)) {
                    selectableDataTimes.add(dt);
                }
            }
        }

        /*
         * Add a check here to see if existing frameTimes are still in the
         * selectableDataTimes - e.g, CAVE keeps running and the user comes back
         * in a few days later to open the data selection window?
         */
        List<DataTime> ftimes = new ArrayList<>();
        for (DataTime fdt : frameTimes) {
            long frameTimeMillisecs = fdt.getValidTime().getTimeInMillis();
            for (DataTime dt : selectableDataTimes) {
                long selTimeMillisecs = dt.getValidTime().getTimeInMillis();
                if (frameTimeMillisecs == selTimeMillisecs) {
                    ftimes.add(fdt);
                    break;
                }
            }
        }

        frameTimes.clear();
        frameTimes.addAll(ftimes);

        // If previously-selected frames are gone, then re-select.
        if (frameTimes.isEmpty()) {

            SelectableFrameTimeMatcher frmTimeMatcher = null;

            /*
             * if there is a GDATTIM then use this to compute the frameList from
             * the list of available times in the DB
             */
            if (dfltFrameTimesStr != null) {
                try {
                    frmTimeMatcher = new SelectableFrameTimeMatcher(
                            dfltFrameTimesStr);

                    for (DataTime dt : allAvailDataTimes) {
                        if (frmTimeMatcher.matchTime(dt)) {
                            frameTimes.add(dt);
                        }
                    }

                } catch (VizException e) {
                    // sanity check since this should already be validated
                    statusHandler
                            .error("bad GDATTIM string:" + dfltFrameTimesStr);
                    frmTimeMatcher = null;
                    return false;
                }

            } else {
                int skipCount = 0;
                // set the initial frameTimes from the skip value and numFrames
                GraphTimelineUtil.sortAvailableData(selectableDataTimes);
                for (skipCount = 0; skipCount < selectableDataTimes
                        .size(); skipCount++) {
                    if (skipCount % (skipValue + 1) == 0) {
                        DataTime selectableTime = (isForecast
                                ? selectableDataTimes.get(skipCount)
                                : selectableDataTimes
                                        .get(selectableDataTimes.size()
                                                - skipCount - 1));
                        if (frmTimeMatcher == null
                                || frmTimeMatcher.matchTime(selectableTime)) {
                            if (isForecast) {
                                frameTimes.add(selectableTime);
                            } else {
                                frameTimes.add(0, selectableTime);
                            }
                        }
                    }
                    if (frameTimes.size() == numFrames) {
                        break;
                    }
                }
            }

        }

        if (frameTimes.size() > 0) {
            numFrames = frameTimes.size();
        }

        return true;
    }

}
