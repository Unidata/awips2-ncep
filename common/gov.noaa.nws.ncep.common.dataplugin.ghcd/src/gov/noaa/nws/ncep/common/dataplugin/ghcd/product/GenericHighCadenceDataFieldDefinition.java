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

import gov.noaa.nws.ncep.common.dataplugin.ghcd.GenericHighCadenceDataConstants;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.dataplugin.annotations.DataURI;
import com.raytheon.uf.common.dataplugin.persist.PersistableDataObject;
import com.raytheon.uf.common.serialization.ISerializableObject;
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
@Entity
@Table(name = "ghcd_fielddefinition")
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement
public class GenericHighCadenceDataFieldDefinition extends
        PersistableDataObject implements ISerializableObject {

    private static final long serialVersionUID = 4866022583462757341L;

    @Id
    @SequenceGenerator(name = "GHCD_FIELDDEF_GENERATOR", sequenceName = "ghcd_fielddef_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GHCD_FIELDDEF_GENERATOR")
    @DynamicSerializeElement
    private Integer id;

    @DynamicSerializeElement
    @XmlAttribute(name = "field-name")
    @DataURI(position = 0)
    private String name;

    @Column
    @DynamicSerializeElement
    @XmlAttribute(name = "type")
    private String datatype = "string";

    @Column
    @DynamicSerializeElement
    @XmlElement
    private String description = "";

    @Column(name = "units")
    @DynamicSerializeElement
    @XmlElement(name = "units")
    private String unitString = "";

    @Column
    @DynamicSerializeElement
    private Double minInclusive = GenericHighCadenceDataConstants.MISSING_DATA_VALUE;

    @Column
    @DynamicSerializeElement
    private Double minExclusive = GenericHighCadenceDataConstants.MISSING_DATA_VALUE;

    @Column
    @DynamicSerializeElement
    private Double maxInclusive = GenericHighCadenceDataConstants.MISSING_DATA_VALUE;

    @Column
    @DynamicSerializeElement
    private Double maxExclusive = GenericHighCadenceDataConstants.MISSING_DATA_VALUE;

    @XmlElement(name = "data-range")
    @DynamicSerializeElement
    @Transient
    protected GenericHighCadenceDataRange dataRange;

    public GenericHighCadenceDataFieldDefinition() {

    }

    public GenericHighCadenceDataFieldDefinition(String name,
            String description, String datatype, String unitString,
            Double minInc, Double maxInc, Double minExc, Double maxExc) {
        this.name = name;
        this.description = description;
        this.datatype = datatype;
        this.unitString = unitString;
        this.minInclusive = minInc;
        this.maxInclusive = maxInc;
        this.minExclusive = minExc;
        this.maxExclusive = maxExc;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDatatype() {
        return datatype;
    }

    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUnitString() {
        return unitString;
    }

    public void setUnitString(String unitString) {
        this.unitString = unitString;
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

    public GenericHighCadenceDataRange getDataRange() {
        return dataRange;
    }

    public void setDataRange(GenericHighCadenceDataRange value) {
        this.dataRange = value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result
                + ((datatype == null) ? 0 : datatype.hashCode());
        result = prime * result
                + ((description == null) ? 0 : description.hashCode());
        result = prime * result
                + ((unitString == null) ? 0 : unitString.hashCode());
        result = prime * result
                + ((minInclusive == null) ? 0 : minInclusive.hashCode());
        result = prime * result
                + ((minExclusive == null) ? 0 : minExclusive.hashCode());
        result = prime * result
                + ((maxInclusive == null) ? 0 : maxInclusive.hashCode());
        result = prime * result
                + ((maxExclusive == null) ? 0 : maxExclusive.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        GenericHighCadenceDataFieldDefinition other = (GenericHighCadenceDataFieldDefinition) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (datatype == null) {
            if (other.datatype != null)
                return false;
        } else if (!datatype.equals(other.datatype))
            return false;
        if (description == null) {
            if (other.description != null)
                return false;
        } else if (!description.equals(other.description))
            return false;
        if (unitString == null) {
            if (other.unitString != null)
                return false;
        } else if (!unitString.equals(other.unitString))
            return false;
        if (minInclusive == null) {
            if (other.minInclusive != null)
                return false;
        } else if (!minInclusive.equals(other.minInclusive))
            return false;
        if (minExclusive == null) {
            if (other.minExclusive != null)
                return false;
        } else if (!minExclusive.equals(other.minExclusive))
            return false;
        if (maxInclusive == null) {
            if (other.maxInclusive != null)
                return false;
        } else if (!maxInclusive.equals(other.maxInclusive))
            return false;
        if (maxExclusive == null) {
            if (other.maxExclusive != null)
                return false;
        } else if (!maxExclusive.equals(other.maxExclusive))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "GenericHighCadenceDataFieldDefinition [name=" + name
                + ", datatype=" + datatype + ", description=" + description
                + ", unitString=" + unitString + ", minInclusive="
                + minInclusive + ", maxInclusive=" + maxInclusive
                + ", minExclusive=" + minExclusive + ", maxExclusive="
                + maxExclusive + "]";
    }
}
