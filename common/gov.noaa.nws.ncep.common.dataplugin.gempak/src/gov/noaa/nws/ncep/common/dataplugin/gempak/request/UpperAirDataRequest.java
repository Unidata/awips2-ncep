package gov.noaa.nws.ncep.common.dataplugin.gempak.request;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.serialization.comm.IServerRequest;
import com.raytheon.uf.common.time.DataTime;

/**
 * Executes point data queries for station upper air level data under a single
 * timestamp.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 19, 2016 R17968     pmoyer      Initial creation
 * 
 * </pre>
 * 
 * @author pmoyer
 * @version 1.0
 */
@DynamicSerialize
public class UpperAirDataRequest implements IServerRequest {

    @DynamicSerializeElement
    private String pluginName;

    // This parameter is not necessary, but kept in to keep the
    // request parallel with the original StationDataRequest sent
    // by legacy GEMPAK. If it is ever removed, the pythonPackages
    // python files will need to be regenerated.
    @DynamicSerializeElement
    private String stationId;

    @DynamicSerializeElement
    private DataTime refTime;

    @DynamicSerializeElement
    private String parmList;

    @DynamicSerializeElement
    private String partNumber;

    public UpperAirDataRequest() {
    }

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    // This function is not necessary, but kept in to keep the
    // request parallel with the original StationDataRequest sent
    // by legacy GEMPAK. If it is ever removed, the pythonPackages
    // python files will need to be regenerated.
    public String getStationId() {
        return stationId;
    }

    // This function is not necessary, but kept in to keep the
    // request parallel with the original StationDataRequest sent
    // by legacy GEMPAK. If it is ever removed, the pythonPackages
    // python files will need to be regenerated.
    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    public DataTime getRefTime() {
        return refTime;
    }

    public void setRefTime(DataTime refTime) {
        this.refTime = refTime;
    }

    public String getParmList() {
        return parmList;
    }

    public void setParmList(String parmList) {
        this.parmList = parmList;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }

}
