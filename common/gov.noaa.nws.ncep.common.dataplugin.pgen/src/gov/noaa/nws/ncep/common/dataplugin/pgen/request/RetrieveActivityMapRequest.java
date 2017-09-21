package gov.noaa.nws.ncep.common.dataplugin.pgen.request;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.comm.IServerRequest;

/**
 * 
 * Request for a PGEN activity map
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 04/22/2016   R9714       byin        Initial creation
 * 
 * </pre>
 * 
 * @author byin
 * @version 1.0
 */
@DynamicSerialize
public class RetrieveActivityMapRequest implements IServerRequest {

    public RetrieveActivityMapRequest() {
        super();
    }
}
