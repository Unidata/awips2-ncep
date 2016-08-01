package gov.noaa.nws.ncep.common.dataplugin.gempak.request;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.serialization.comm.IServerRequest;

/**
 * 
 * Contains information used to request grid field information from the EDEX
 * data stores. Users can specify model, reference time, and or forecast time.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * ??? ??, ????            Jacobs     Initial creation
 * Sep 14, 2015 RM8764     Gilbert    Added reftime and fcstsec fields
 * 
 * </pre>
 * 
 * @author sgilbert
 * @version 1.0
 */
@DynamicSerialize
public class GetGridInfoRequest implements IServerRequest {

    @DynamicSerializeElement
    private String pluginName;

    @DynamicSerializeElement
    private String modelId;

    @DynamicSerializeElement
    private String reftime;

    @DynamicSerializeElement
    private String fcstsec;

    public GetGridInfoRequest() {
    }

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getReftime() {
        return reftime;
    }

    public void setReftime(String reftime) {
        this.reftime = reftime;
    }

    public String getFcstsec() {
        return fcstsec;
    }

    public void setFcstsec(String fcstsec) {
        this.fcstsec = fcstsec;
    }

}
