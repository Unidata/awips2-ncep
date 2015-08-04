/**
 * 
 * This code has unlimited rights, and is provided "as is" by the National Centers 
 * for Environmental Prediction, without warranty of any kind, either expressed or implied, 
 * including but not limited to the implied warranties of merchantability and/or fitness 
 * for a particular purpose.
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 * @version 1.0
 */
package gov.noaa.nws.ncep.common.dataplugin.ghcd.product;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

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
 */

@DynamicSerialize
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "data-item")
public class GenericHighCadenceDataItem {

    @DynamicSerializeElement
    @XmlAttribute(name = "time-tag")
    protected Date refTime;

    @DynamicSerializeElement
    // @XmlTransient
    private List<GenericHighCadenceDataField> ghcdFields = new ArrayList<GenericHighCadenceDataField>();

    public GenericHighCadenceDataItem() {
        super();
    }

    @XmlAnyElement
    @XmlJavaTypeAdapter(GenericHighCadenceDataFieldAdapter.class)
    public List<GenericHighCadenceDataField> getGhcdFields() {
        return ghcdFields;
    }

    public void setGhcdFields(List<GenericHighCadenceDataField> ghcdFields) {
        this.ghcdFields = ghcdFields;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        GenericHighCadenceDataItem prod = new GenericHighCadenceDataItem();
        return prod;
    }

    public Date getRefTime() {
        return refTime;
    }

    public void setRefTime(Date refTime) {
        this.refTime = refTime;
    }

}
