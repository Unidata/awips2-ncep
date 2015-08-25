/** 
 * This code has unlimited rights, and is provided "as is" by the National Centers 
 * for Environmental Prediction, without warranty of any kind, either expressed or implied, 
 * including but not limited to the implied warranties of merchantability and/or fitness 
 * for a particular purpose.
 *
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 */
package gov.noaa.nws.ncep.common.dataplugin.ghcd.product;

import gov.noaa.nws.ncep.common.dataplugin.ghcd.GenericHighCadenceDataConstants;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#     Engineer    Description
 * -------      -------     --------    -----------
 * 01/24/2014   1100        sgurung     Initial creation
 * 
 * </pre>
 * 
 * @author sgurung
 * @version 1.0
 * 
 */
@DynamicSerialize
@XmlAccessorType(XmlAccessType.NONE)
public class GenericHighCadenceDataRange {

    @DynamicSerializeElement
    @XmlAttribute
    protected Double minInclusive;

    @DynamicSerializeElement
    @XmlAttribute
    protected Double minExclusive;

    @DynamicSerializeElement
    @XmlAttribute
    protected Double maxInclusive;

    @DynamicSerializeElement
    @XmlAttribute
    protected Double maxExclusive;

    public GenericHighCadenceDataRange() {
        minInclusive = GenericHighCadenceDataConstants.MISSING_DATA_VALUE;
        minExclusive = GenericHighCadenceDataConstants.MISSING_DATA_VALUE;
        maxInclusive = GenericHighCadenceDataConstants.MISSING_DATA_VALUE;
        maxExclusive = GenericHighCadenceDataConstants.MISSING_DATA_VALUE;
    }

    public GenericHighCadenceDataRange(Double minInc, Double maxInc,
            Double minExc, Double maxExc) {
        this.minInclusive = minInc;
        this.maxInclusive = maxInc;
        this.minExclusive = minExc;
        this.maxExclusive = maxExc;
    }

    public Double getMinInclusive() {
        return minInclusive;
    }

    public void setMinInclusive(Double value) {
        this.minInclusive = value;
    }

    public Double getMinExclusive() {
        return minExclusive;
    }

    public void setMinExclusive(Double value) {
        this.minExclusive = value;
    }

    public Double getMaxInclusive() {
        return maxInclusive;
    }

    public void setMaxInclusive(Double value) {
        this.maxInclusive = value;
    }

    public Double getMaxExclusive() {
        return maxExclusive;
    }

    public void setMaxExclusive(Double value) {
        this.maxExclusive = value;
    }
}
