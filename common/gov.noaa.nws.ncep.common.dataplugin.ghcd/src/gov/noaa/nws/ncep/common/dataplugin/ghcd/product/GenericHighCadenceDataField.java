/**
 * This code has unlimited rights, and is provided "as is" by the National Centers 
 * for Environmental Prediction, without warranty of any kind, either expressed or implied, 
 * including but not limited to the implied warranties of merchantability and/or fitness 
 * for a particular purpose.
 * 
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 * 
 */
package gov.noaa.nws.ncep.common.dataplugin.ghcd.product;

import gov.noaa.nws.ncep.common.dataplugin.ghcd.GenericHighCadenceDataConstants;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

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
 */

@DynamicSerialize
@XmlAccessorType(XmlAccessType.NONE)
public class GenericHighCadenceDataField {

    @DynamicSerializeElement
    private String name;

    @DynamicSerializeElement
    private String value = GenericHighCadenceDataConstants.MISSING_DATA_VALUE_STR;

    public GenericHighCadenceDataField() {
        super();
    }

    public GenericHighCadenceDataField(String name, String value) {
        super();
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
