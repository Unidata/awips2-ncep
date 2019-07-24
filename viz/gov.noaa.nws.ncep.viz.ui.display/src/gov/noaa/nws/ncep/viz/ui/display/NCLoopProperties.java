package gov.noaa.nws.ncep.viz.ui.display;

import com.raytheon.uf.viz.core.datastructure.LoopProperties;

/**
 *
 * NCP perspective extension of the Loop Properties
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * ???                                 Initial creation
 * Jun 05, 2019 64621      tjensen     Update to line up with performance frame times
 *
 * </pre>
 *
 * @author tjensen
 */
public class NCLoopProperties extends LoopProperties {

    /** flag indicating current loop stop position */
    private boolean loopStopCurrent = true;

    /*
     * NCEP loop properties
     */
    /** Maximum NCEP frame time in ms */
    private int maxNcepFrameTime = MAX_FRAME_TIME;

    /** maximum NCEP first frame dwell time in ms */
    private int maxNcepFirstFrameDwellTime = MAX_DWELL_TIME;

    /** maximum NCEP last frame dwell time in ms */
    private int maxNcepLastFrameDwellTime = MAX_DWELL_TIME;

    public NCLoopProperties() {
        super();
    }

    public NCLoopProperties(LoopProperties original) {
        super(original);
        if (original instanceof NCLoopProperties) {
            NCLoopProperties ncOriginal = (NCLoopProperties) original;
            loopStopCurrent = ncOriginal.loopStopCurrent;
            maxNcepFrameTime = ncOriginal.maxNcepFrameTime;
            maxNcepFirstFrameDwellTime = ncOriginal.maxNcepFirstFrameDwellTime;
            maxNcepLastFrameDwellTime = ncOriginal.maxNcepLastFrameDwellTime;
        }
    }

    /*
     * Set loop stop position
     */
    public void setLoopStopCurrent(boolean loopStop) {
        this.loopStopCurrent = loopStop;

    }

    /*
     * Get loop stop position
     */
    public boolean getLoopStopCurrent() {
        return loopStopCurrent;

    }

    /*
     * Set NCEP maximum frame time
     */
    public void setNcepMaxFrameTime(int frameTime) {
        maxNcepFrameTime = frameTime;
    }

    /*
     * Get NCEP maximum frame time
     */
    public int getNcepMaxFrameTime() {
        return maxNcepFrameTime;
    }

    /*
     * Set NCEP maximum first frame time
     */
    public void setNcepMaxFirstFrameTime(int frameTime) {
        maxNcepFirstFrameDwellTime = frameTime;
    }

    /*
     * Get NCEP minimum first frame time
     */
    public int getNcepMaxFirstFrameTime() {
        return maxNcepFirstFrameDwellTime;
    }

    /*
     * Set NCEP maximum last frame time
     */
    public void setNcepMaxLastFrameTime(int frameTime) {
        maxNcepLastFrameDwellTime = frameTime;
    }

    /*
     * Get NCEP minimum last frame time
     */
    public int getNcepMaxLastFrameTime() {
        return maxNcepLastFrameDwellTime;
    }
}
