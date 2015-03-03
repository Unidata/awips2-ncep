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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.dataplugin.annotations.DataURI;
import com.raytheon.uf.common.dataplugin.persist.PersistableDataObject;
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
 * 07/15/2014   1100        sgurung     Renamed name to instrument, added datatype
 * 
 * </pre>
 * 
 * @author sgurung
 * @version 1.0
 * 
 */
@Embeddable
@Entity
@Table(name = "ghcd_typeInfo", uniqueConstraints = { @UniqueConstraint(columnNames = {
        "source", "instrument", "datatype" }) })
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "data-field-definitions")
@DynamicSerialize
public class GenericHighCadenceDataTypeInfo extends
        PersistableDataObject<String> {
    private static final long serialVersionUID = 1L;

    @Id
    @DataURI(position = 1)
    @DynamicSerializeElement
    @XmlAttribute(name = "source", required = true)
    private String source;

    @Id
    @DataURI(position = 2)
    @DynamicSerializeElement
    @XmlAttribute(name = "instrument", required = true)
    private String instrument;

    @Id
    @DataURI(position = 3)
    @DynamicSerializeElement
    @XmlAttribute(name = "data-type", required = true)
    private String datatype;

    @DynamicSerializeElement
    @XmlElement
    private String description;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @DynamicSerializeElement
    @XmlElement(name = "field-def")
    private List<GenericHighCadenceDataFieldDefinition> fieldDefList = new ArrayList<GenericHighCadenceDataFieldDefinition>();

    public GenericHighCadenceDataTypeInfo() {
        super();
    }

    public GenericHighCadenceDataTypeInfo(String source, String instrument,
            String datatype) {
        super();
        this.source = source;
        this.instrument = instrument;
        this.datatype = datatype;
        this.description = instrument;
    }

    public GenericHighCadenceDataTypeInfo(String source, String instrument,
            String datatype, String description) {
        super();
        this.source = source;
        this.datatype = datatype;
        this.instrument = instrument;
        this.description = description;
    }

    public GenericHighCadenceDataTypeInfo(String source, String instrument,
            String datatype, String description,
            List<GenericHighCadenceDataFieldDefinition> fieldList) {
        super();
        this.source = source;
        this.datatype = datatype;
        this.instrument = instrument;
        this.description = description;
        this.fieldDefList = fieldList;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
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

    public List<GenericHighCadenceDataFieldDefinition> getFieldDefList() {
        if (fieldDefList == null) {
            fieldDefList = new ArrayList<GenericHighCadenceDataFieldDefinition>();
        }
        return this.fieldDefList;
    }

    public void setFieldDefList(
            List<GenericHighCadenceDataFieldDefinition> fieldDefList) {
        this.fieldDefList = fieldDefList;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public GenericHighCadenceDataTypeInfo clone()
            throws CloneNotSupportedException {
        GenericHighCadenceDataTypeInfo rpt = new GenericHighCadenceDataTypeInfo(
                this.source, this.instrument, this.datatype, this.description);
        rpt.getFieldDefList().clear();
        for (GenericHighCadenceDataFieldDefinition fieldDef : this.fieldDefList) {
            String name = fieldDef.getName();
            String desc = fieldDef.getDescription();
            String datatype = fieldDef.getDatatype();
            String units = fieldDef.getUnitString();
            double minInc = fieldDef.getMinInclusive();
            double maxInc = fieldDef.getMaxInclusive();
            double minExc = fieldDef.getMinExclusive();
            double maxExc = fieldDef.getMaxExclusive();
            GenericHighCadenceDataFieldDefinition newFieldDef = new GenericHighCadenceDataFieldDefinition();
            newFieldDef.setName(name);
            newFieldDef.setDescription(desc);
            newFieldDef.setDatatype(datatype);
            newFieldDef.setUnitString(units);
            newFieldDef.setMinInclusive(minInc);
            newFieldDef.setMaxInclusive(maxInc);
            newFieldDef.setMinExclusive(minExc);
            newFieldDef.setMaxExclusive(maxExc);
            rpt.getFieldDefList().add(newFieldDef);
        }
        return rpt;
    }
}
