/**
 * 
 */
package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Dimensionless;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import tech.units.indriya.AbstractUnit;

/**
 * Maps to the GEMPAK parameter PTND
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class PressureTendencySymbol
        extends AbstractMetParameter<Dimensionless>
        implements ISerializableObject {

    /**
     * The parameter values are indexes into the press_change_char_lookup.txt
     * file which references symbols defined in the WxSymbols.svg file.
     */
    private static final long serialVersionUID = -4616340047592131749L;

    public PressureTendencySymbol() {
        super(AbstractUnit.ONE);
        // TTR 922-923, This is needed to display PTSY
        setValueIsString();
    }
}
