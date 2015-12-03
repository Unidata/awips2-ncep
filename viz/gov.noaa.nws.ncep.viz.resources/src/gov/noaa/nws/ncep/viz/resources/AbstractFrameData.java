package gov.noaa.nws.ncep.viz.resources;

import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData.TimeMatchMethod;

import java.util.Date;

import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.time.TimeRange;

/**
 * AbstractFrameData is part of the Resource classes redesign as specified
 * in Redmine 11819.  For more details about that redesign please see that
 * ticket and the title block comment of 
 * {@link gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource2
 * 
 * Before the redesign AbstractFrameData was an inner class of
 * AbstractNatlCntrsResource.  It was put into its own file in the same 
 * package to enable collections generically typed to AbstractFrameData to
 * be passed into classes of the new design.
 * 
 * AbstractFrameData serves as a parent and template to a number of 
 * FrameData class implementations as inner classes in the *Resource classes.
 * 
 * The AbstractFrameData class holds information about the frames computed for
 * display in CAVE.  This is also where various time matching methods are 
 * implemented and where updateFrameData() is declared.  updateFrameData()
 * starts the process of loading data to a particular frame.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 11/05/2015   R 11819    srussell     Initial creation
 * 
 * </pre>
 * 
 * @author srussell
 * @version 1.0
 */

public abstract class AbstractFrameData {

    protected DataTime frameTime;

    // for resources that need to populated the frames only when the frame
    // is first displayed.
    protected boolean populated;

    protected DataTime startTime; // valid times without a forecast hour

    protected DataTime endTime;

    protected long startTimeMillis;

    protected long endTimeMillis;

    /**
     * The resource data that was used to construct this resource, and generic
     * properties that should be used in the rendering of the data
     */
    protected AbstractNatlCntrsRequestableResourceData resourceData;

    // set the frame start and end time based on:
    // - frame time (from dominant resource),
    // - the frameInterval for this resource (may be different than the
    // frame interval used to generate the timeline) and
    // - the timeMatchMethod.
    protected AbstractFrameData(DataTime ftime, int frameInterval,
            AbstractNatlCntrsRequestableResourceData resourceData) {

        this.resourceData = resourceData;

        // if there is a validPeriod or levels, ignore them and just use the
        // valid time.
        this.frameTime = new DataTime(ftime.getRefTime(), ftime.getFcstTime());
        this.populated = false;
        long frameMillis = frameTime.getValidTime().getTimeInMillis();

        switch (resourceData.getTimeMatchMethod()) {

        case EXACT: {
            startTime = new DataTime(frameTime.getValidTime());
            endTime = new DataTime(frameTime.getValidTime());
        }
        // Note : Currently this is implemented the same as Exact. (ie the
        // frame time must be between the start/end time of an event.) A more
        // general algorithm could be implemented to use the frame span and test
        // whether any part of the event overlaps with any part of the frame
        // span. But currently, for Event resources, the frame span is taken
        // as the default frame interval for a manual timeline and so
        // this would need to be addressed first.)
        case EVENT: {
            startTime = new DataTime(frameTime.getValidTime());
            endTime = new DataTime(frameTime.getValidTime());
        }
        case EVENT_BEFORE_OR_AFTER: {
            startTime = new DataTime(new Date(frameMillis - frameInterval
                    * 1000 * 60 / 2));
            endTime = new DataTime(new Date(frameMillis + frameInterval * 1000
                    * 60 / 2 - 1000));
            break;
        }
        case CLOSEST_BEFORE_OR_AFTER:
        case BINNING_FOR_GRID_RESOURCES: {
            startTime = new DataTime(new Date(frameMillis - frameInterval
                    * 1000 * 60 / 2));
            endTime = new DataTime(new Date(frameMillis + frameInterval * 1000
                    * 60 / 2 - 1000));
            break;
        }
        case CLOSEST_BEFORE_OR_EQUAL: {
            startTime = new DataTime(new Date(frameMillis - frameInterval
                    * 1000 * 60));
            endTime = new DataTime(frameTime.getValidTime());
            break;
        }
        case CLOSEST_AFTER_OR_EQUAL: {
            startTime = new DataTime(frameTime.getValidTime());
            endTime = new DataTime(new Date(frameMillis + frameInterval * 1000
                    * 60 - 1000));
            break;
        }
        case BEFORE_OR_EQUAL: {
            startTime = new DataTime(new Date(0));
            endTime = new DataTime(frameTime.getValidTime());
            break;
        }
        // This could be implemented by setting the frame span to infinite.
        case MATCH_ALL_DATA: {
            startTime = new DataTime(new Date(0));
            endTime = new DataTime(new Date(Long.MAX_VALUE));
        }
        }

        startTimeMillis = startTime.getValidTime().getTimeInMillis();
        endTimeMillis = endTime.getValidTime().getTimeInMillis();
    }

    final public boolean isRscDataObjInFrame(
            gov.noaa.nws.ncep.viz.resources.IRscDataObject rscDataObj) {
        DataTime dataTime = rscDataObj.getDataTime();
        return (dataTime == null ? false : timeMatch(dataTime) >= 0);
    }

    /**
     * Which of the two IRscDataObjects passed in is closest to the current
     * frame in time?
     * 
     * Return 1 if it is rscDataObj1. Return 2 if it is rscDataObj2. Return -1
     * if both objects are null
     * 
     * @param rscDataObj1
     * @param rscDataObj2
     * @return int
     */
    public int closestToFrame(IRscDataObject rscDataObj1,
            IRscDataObject rscDataObj2) {

        // Check for null objects
        if (rscDataObj1 != null && rscDataObj2 == null)
            return 1;
        if (rscDataObj1 == null && rscDataObj2 != null)
            return 2;
        else if (rscDataObj1 == null && rscDataObj2 == null)
            return -1;

        int iReturn = 0;
        long obj1MinusFrameT = 0;
        long obj2MinusFrameT = 0;

        long rdo1Millis = rscDataObj1.getDataTime().getValidTime()
                .getTimeInMillis();
        long rdo2Millis = rscDataObj2.getDataTime().getValidTime()
                .getTimeInMillis();
        long frameTimeMillis = frameTime.getValidTime().getTimeInMillis();

        obj1MinusFrameT = Math.abs(rdo1Millis - frameTimeMillis);
        obj2MinusFrameT = Math.abs(rdo2Millis - frameTimeMillis);

        if (obj1MinusFrameT < obj2MinusFrameT) {
            iReturn = 1;
        } else if (obj2MinusFrameT < obj1MinusFrameT) {
            iReturn = 2;
        } else if (obj1MinusFrameT == obj2MinusFrameT) {
            iReturn = 0;
        }

        return iReturn;
    }

    // return -1 if the data doesn't match. if the return value is 0 or
    // positive then this is the number of seconds from the perfect match.
    public long timeMatch(DataTime dataTime) {

        long dataTimeMillis = dataTime.getValidTime().getTimeInMillis();
        long frameTimeMillis = frameTime.getValidTime().getTimeInMillis();

        TimeRange dataTimeRange = dataTime.getValidPeriod();

        switch (resourceData.getTimeMatchMethod()) {

        case MATCH_ALL_DATA: // everything is a perfect match. (for PGEN
                             // Resource)
            return 0;
        case EXACT:
        case EVENT: {
            if (dataTimeRange.isValid()) {
                if (dataTimeRange.getStart().getTime() <= frameTimeMillis
                        && frameTimeMillis <= dataTimeRange.getEnd().getTime()) {
                    return 0;
                } else {
                    return -1;
                }
            } else {
                return (frameTimeMillis == dataTimeMillis ? 0 : -1);
            }
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
        // mainly (only?) for lightning. Might be able to remove this
        // timeMatchMethod if lighting resource is modified?
        case BEFORE_OR_EQUAL: {
            return (dataTimeMillis > endTimeMillis ? -1
                    : (endTimeMillis - dataTimeMillis) / 1000);
        }
        case CLOSEST_BEFORE_OR_AFTER:
        case CLOSEST_BEFORE_OR_EQUAL:
        case CLOSEST_AFTER_OR_EQUAL: {
            // This should be an invalid case. if this is an event type
            // resource then it should be an EXACT time match. Still, for
            // now leave this logic in here.
            if (dataTimeRange.isValid()) {
                System.out
                        .println("Timematching a dataTime with a valid interval with a non-EXACT\n "
                                + "TimeMatchMethod.");
                return -1;
            }

            // return -1 if this is not a match.
            // (since the start/end times are based on the timeMatchMethod,
            // we can just check that the datatime is not within the
            // start/end)
            //
            if (startTimeMillis >= dataTimeMillis
                    || dataTimeMillis > endTimeMillis) {
                return -1;
            }
            //
            else if (resourceData.getTimeMatchMethod() == TimeMatchMethod.CLOSEST_BEFORE_OR_EQUAL) {
                return (endTimeMillis - dataTimeMillis) / 1000;
            } else if (resourceData.getTimeMatchMethod() == TimeMatchMethod.CLOSEST_AFTER_OR_EQUAL) {
                return (dataTimeMillis - startTimeMillis) / 1000;
            } else if (resourceData.getTimeMatchMethod() == TimeMatchMethod.CLOSEST_BEFORE_OR_AFTER) {
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

        }// end switch
        return -1;
    }

    // only return true if the data was added to the frame. It is possible
    // for some resources for the data to time match but not be added because
    // there is already data in the frame that is a better match.
    public abstract boolean updateFrameData(IRscDataObject rscDataObj);

    public void dispose() {
        // override this if need to dispose of anything in the Frame.
    }

    public DataTime getFrameTime() {
        return frameTime;
    }

    public DataTime getFrameStartTime() {
        return startTime;
    }

    public DataTime getFrameEndTime() {
        return endTime;
    }

    public void setFrameTime(DataTime frameTime) {
        this.frameTime = frameTime;
    }

    public void setStartTime(DataTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(DataTime endTime) {
        this.endTime = endTime;
    }

    public void setStartTimeMillis(long startTimeMillis) {
        this.startTimeMillis = startTimeMillis;
    }

    public void setEndTimeMillis(long endTimeMillis) {
        this.endTimeMillis = endTimeMillis;
    }

    public void setResourceData(
            AbstractNatlCntrsRequestableResourceData resourceData) {
        this.resourceData = resourceData;
    }

    public boolean isPopulated() {
        return populated;
    }

    public void setPopulated(boolean p) {
        populated = p;
    }

}
