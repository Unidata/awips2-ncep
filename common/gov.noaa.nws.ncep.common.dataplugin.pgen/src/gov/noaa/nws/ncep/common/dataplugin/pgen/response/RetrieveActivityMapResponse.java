package gov.noaa.nws.ncep.common.dataplugin.pgen.response;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Response object to a RetrieveActivityMapRequest.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 05/06/2016    R9714       byin      Initial creation
 * 
 * </pre>
 * 
 * @author byin
 * @version 1.0
 */
@DynamicSerialize
public class RetrieveActivityMapResponse {

    @DynamicSerializeElement
    private ActivityMapData[] data;

    /**
     * Constructor.
     */
    public RetrieveActivityMapResponse() {
        super();
    }

    /**
     * @return the data
     */
    public ActivityMapData[] getData() {
        return data;
    }

    /**
     * @param data
     *            the data to set
     */
    public void setData(ActivityMapData[] data) {
        this.data = data;
    }

}
