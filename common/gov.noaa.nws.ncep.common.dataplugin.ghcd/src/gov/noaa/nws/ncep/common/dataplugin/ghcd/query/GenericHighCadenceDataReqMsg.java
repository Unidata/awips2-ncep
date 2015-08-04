/**
 * This code has unlimited rights, and is provided "as is" by the National Centers 
 * for Environmental Prediction, without warranty of any kind, either expressed or implied, 
 * including but not limited to the implied warranties of merchantability and/or fitness 
 * for a particular purpose. 
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 */
package gov.noaa.nws.ncep.common.dataplugin.ghcd.query;

import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.serialization.comm.IServerRequest;

/**
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#     Engineer    Description
 * -------      -------     --------    -----------
 * 01/22/2014   1100        sgurung     Initial creation
 * 07/15/2014   1100        sgurung     Renamed typeName to instrument, added datatype
 * 
 * </pre>
 * 
 * @author sgurung
 * @version 1.0
 */
@DynamicSerialize
@XmlRootElement(name = "GenericHighCadenceDataReqMsg")
@XmlAccessorType(XmlAccessType.NONE)
public class GenericHighCadenceDataReqMsg implements IServerRequest {
    public static enum GenericHighCadenceDataReqType {

        GET_GHCD_DATA_ITEMS,

        // get GHCD data in Java Object format
        GET_GHCD_TYPE_INFO_OBJECT, GET_GHCD_TYPE_OBJECT, GET_GHCD_TYPE_ITEM_OBJECT, GET_GHCD_ALL_AVAILABLE_TYPES, GET_GHCD_ALL_AVAILABLE_TYPES_BY_DATA_RESOLUTION,

        // get/save product in XML format
        GET_GHCD_TYPE_XML, GET_GHCD_DATA_ITEM_XML, GET_GHCD_TYPE_INFO_XML, STORE_GHCD_TYPE_FROM_XML,

        // Purge GHCD DB
        PURGE_GHCD_TYPE_ONETIME, PURGE_GHCD_TYPE_ALLTIME, PURGE_GHCD_EXPIRED_TYPE, PURGE_GHCD_ALL_TYPES
    }

    public static enum GenericHighCadenceDataQueryKey {
        BY_TYPE_NAME, BY_DATA_URI, BY_SOURCE, BY_SOURCE_DATA_RESOLUTION
    }

    // required for all
    @DynamicSerializeElement
    @XmlAttribute(required = true)
    private GenericHighCadenceDataReqType reqType;

    @DynamicSerializeElement
    @XmlAttribute
    private Date refTime = null;

    @DynamicSerializeElement
    @XmlAttribute
    private String instrument = null;

    @DynamicSerializeElement
    @XmlAttribute
    private String datatype = null;

    @DynamicSerializeElement
    @XmlAttribute
    private String source = null;

    @DynamicSerializeElement
    @XmlAttribute
    private String dataResolUnits = null;

    @DynamicSerializeElement
    @XmlAttribute
    private Integer dataResolVal = null;

    @DynamicSerializeElement
    @XmlAttribute
    private GenericHighCadenceDataQueryKey queryKey;

    @DynamicSerializeElement
    private int maxNumLevel = 0;

    @DynamicSerializeElement
    private String ghcdDataString;

    @DynamicSerializeElement
    private List<Date> queryTimeList;

    @DynamicSerializeElement
    private String refTimeStr;

    @DynamicSerializeElement
    @XmlAttribute
    private Date startRefTime;

    @DynamicSerializeElement
    @XmlAttribute
    private Date endRefTime;

    public GenericHighCadenceDataReqMsg() {
        super();
    }

    public String getInstrument() {
        return instrument;
    }

    public void setInstrument(String instrument) {
        this.instrument = instrument;
    }

    public String getDatatype() {
        return datatype;
    }

    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }

    public GenericHighCadenceDataReqType getReqType() {
        return reqType;
    }

    public void setReqType(GenericHighCadenceDataReqType reqType) {
        this.reqType = reqType;
    }

    public Date getRefTime() {
        return refTime;
    }

    public void setRefTime(Date refTime) {
        this.refTime = refTime;
    }

    public String getGhcdDataString() {
        return ghcdDataString;
    }

    public void setGhcdDataString(String ghcdDataString) {
        this.ghcdDataString = ghcdDataString;
    }

    public GenericHighCadenceDataQueryKey getQueryKey() {
        return queryKey;
    }

    public void setQueryKey(GenericHighCadenceDataQueryKey queryKey) {
        this.queryKey = queryKey;
    }

    public int getMaxNumLevel() {
        return maxNumLevel;
    }

    public void setMaxNumLevel(int maxNumLevel) {
        this.maxNumLevel = maxNumLevel;
    }

    public List<Date> getQueryTimeList() {
        return queryTimeList;
    }

    public void setQueryTimeList(List<Date> queryTimeList) {
        this.queryTimeList = queryTimeList;
    }

    public String getRefTimeStr() {
        return refTimeStr;
    }

    public void setRefTimeStr(String refTimeStr) {
        this.refTimeStr = refTimeStr;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDataResolUnits() {
        return dataResolUnits;
    }

    public void setDataResolUnits(String dataResolUnits) {
        this.dataResolUnits = dataResolUnits;
    }

    public Integer getDataResolVal() {
        return dataResolVal;
    }

    public void setDataResolVal(Integer dataResolVal) {
        this.dataResolVal = dataResolVal;
    }

    public Date getStartRefTime() {
        return startRefTime;
    }

    public void setStartRefTime(Date startRefTime) {
        this.startRefTime = startRefTime;
    }

    public Date getEndRefTime() {
        return endRefTime;
    }

    public void setEndRefTime(Date endRefTime) {
        this.endRefTime = endRefTime;
    }

}
