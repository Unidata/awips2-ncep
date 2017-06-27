package gov.noaa.nws.ncep.common.dataplugin.asdi;

import java.util.Calendar;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.serialization.comm.IServerRequest;

/**
 * 
 * RetrieveAsdiRequest
 * 
 * Request for ASDI data for the given time range. This is a DB Request.
 * 
 * for example:
 * 
 * In RetrieveAsdiHandler.java
 * 
 * public List<AsdiRecord> handleRequest(RetrieveAsdiRequest request) { . . .
 * DbQueryResponse queryResults = queryAsdiRecordsForTimeRange(request);
 * 
 * This code has been developed by the NCEP/NCO/SDB for use in the AWIPS2
 * system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#  Engineer     Description
 * ------------ -------- -----------  --------------------------
* 03/07/2017    R28579   R.Reynolds   Initial coding.
 * 
 * </pre>
 * 
 * @author RCReynolds
 * @version 1
 * 
 */
@DynamicSerialize
public class RetrieveAsdiRequest implements IServerRequest {

    @DynamicSerializeElement
    private Calendar startTime;

    @DynamicSerializeElement
    private Calendar endTime;

    public RetrieveAsdiRequest() {
    }

    public RetrieveAsdiRequest(Calendar startTime, Calendar endTime) {
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
