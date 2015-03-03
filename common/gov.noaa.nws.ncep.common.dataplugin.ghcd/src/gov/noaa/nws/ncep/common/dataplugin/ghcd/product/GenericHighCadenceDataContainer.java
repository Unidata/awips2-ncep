/**
 * This code has unlimited rights, and is provided "as is" by the National Centers 
 * for Environmental Prediction, without warranty of any kind, either expressed or implied, 
 * including but not limited to the implied warranties of merchantability and/or fitness 
 * for a particular purpose.
 * 
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 */
package gov.noaa.nws.ncep.common.dataplugin.ghcd.product;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#     Engineer    Description
 * -------      -------     --------    -----------
 * 01/22/2014   1100        sgurung     Initial creation
 * 
 * </pre>
 * 
 * @author sgurung
 * @version 1.0
 * 
 */
@DynamicSerialize
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "dataset")
public class GenericHighCadenceDataContainer {

    @DynamicSerializeElement
    @XmlElement(name = "data-resolution")
    private GenericHighCadenceDataResolution dataResolution;

    @DynamicSerializeElement
    @XmlElement(name = "data-field-definitions")
    private GenericHighCadenceDataTypeInfo dataTypeInfo;

    // list of data items
    @DynamicSerializeElement
    @XmlElement(name = "data-item")
    private List<GenericHighCadenceDataItem> dataItemLst = new ArrayList<GenericHighCadenceDataItem>();

    @DynamicSerializeElement
    @XmlAttribute
    protected String source;

    public GenericHighCadenceDataContainer() {
        super();
    }

    public GenericHighCadenceDataTypeInfo getDataTypeInfo() {
        return dataTypeInfo;
    }

    public void setDataTypeInfo(GenericHighCadenceDataTypeInfo dataTypeInfo) {
        this.dataTypeInfo = dataTypeInfo;
    }

    public List<GenericHighCadenceDataItem> getDataItemLst() {
        return dataItemLst;
    }

    public void setDataItemLst(List<GenericHighCadenceDataItem> dataItemLst) {
        this.dataItemLst = dataItemLst;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public GenericHighCadenceDataResolution getDataResolution() {
        return dataResolution;
    }

    public void setDataResolution(
            GenericHighCadenceDataResolution dataResolution) {
        this.dataResolution = dataResolution;
    }

}
