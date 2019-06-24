package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Length;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.util.UtilN;
import si.uom.SI;

/**
 * Maps to any of the GEMPAK parameters VSBC,VSBK,VSBY or VSBN depending on the
 * units in which the visibility is reported (kilometers, statute miles or
 * nautical miles respectively).
 *
 * TODO : this class can be enhanced to read AbstractNcParameter objects either
 * from an extension point or to read jaxb files from a directory. Till then the
 * AbstractNcParameter's are just created here.
 * 
 * NjJensen comment: If you end up deciding to read jaxb files, please make your
 * own JAXBManager instead of using SerializationUtil. It will perform faster
 * and you can remove the deprecated ISerializableObject from these classes.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 12/13/2016   R27046     SRussell &  Added special formatting for VSBF &
 *                         JHuber      VSBC
 * </pre>
 * 
 * @author ?
 * @version 1.0
 *
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class Visibility extends AbstractMetParameter<Length> {

    public Visibility() {
        super(SI.METRE);
    }

    @Override
    public String getFormattedString(String formatStr) {

        Double presValInMb = getValue().doubleValue();

        if (formatStr == null || formatStr.isEmpty()
                || formatStr.startsWith("%")) {
            return super.getFormattedString(formatStr);

        } else if ((formatStr.equals("VSBF"))) {
            return UtilN.findFraction(presValInMb);
        } else if ((formatStr.equals("VSBC"))) {
            return UtilN.findFractionForANumberGreaterThanOne(presValInMb);
        } else {
            return super.getFormattedString(formatStr);
        }
    }

}// end class Visibility
