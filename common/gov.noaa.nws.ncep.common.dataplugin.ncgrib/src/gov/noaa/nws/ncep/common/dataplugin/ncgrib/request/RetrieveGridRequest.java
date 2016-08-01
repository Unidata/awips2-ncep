package gov.noaa.nws.ncep.common.dataplugin.ncgrib.request;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.serialization.comm.IServerRequest;

/**
 * 
 * Request for a grid for the given dataURI
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 03/01/2016   R6821      kbugenhagen Initial creation
 * 
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1.0
 */
@DynamicSerialize
public class RetrieveGridRequest implements IServerRequest {

    @DynamicSerializeElement
    private String dataURI;

    public RetrieveGridRequest() {
    }

    public RetrieveGridRequest(String dataURI) {
        super();
        this.dataURI = dataURI;
    }

    public String getDataURI() {
        return dataURI;
    }

    public void setDataURI(String dataURI) {
        this.dataURI = dataURI;
    }

}
