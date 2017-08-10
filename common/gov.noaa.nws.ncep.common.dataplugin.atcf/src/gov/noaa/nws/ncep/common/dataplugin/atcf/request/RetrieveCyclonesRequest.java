package gov.noaa.nws.ncep.common.dataplugin.atcf.request;

import java.util.Calendar;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.serialization.comm.IServerRequest;

/**
 * 
 * Request for ATCF Cyclones for the given time range.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#  Engineer     Description
 * ------------ -------  -----------  --------------------------
 * 7/14/2016    R9715    kbugenhagen  Initial creation.
 * 
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1.0
 */
@DynamicSerialize
public class RetrieveCyclonesRequest implements IServerRequest {

    @DynamicSerializeElement
    private Calendar startTime;

    @DynamicSerializeElement
    private Calendar endTime;

    public RetrieveCyclonesRequest() {
    }

    public RetrieveCyclonesRequest(Calendar startTime, Calendar endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Calendar getStartTime() {
        return startTime;
    }

    public void setStartTime(Calendar startTime) {
        this.startTime = startTime;
    }

    public Calendar getEndTime() {
        return endTime;
    }

    public void setEndTime(Calendar endTime) {
        this.endTime = endTime;
    }

}
