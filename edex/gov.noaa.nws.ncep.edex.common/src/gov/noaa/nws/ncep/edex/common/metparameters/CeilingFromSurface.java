/**
 * 
 */
package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Length;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.NcUnits;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;
import si.uom.SI;

/**
 * The Ceiling as measured from the station or surface (AKA CEIL).
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 08/20/2016    R18194    R.Reynolds  Creation
 * 09/12/2016    R18194    R,Reynolds  Added setUnit(NcUnits.HUNDREDS_OF_FEET)
 * 
 * </pre>
 * 
 * @author rreynolds
 * @version 1.0
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class CeilingFromSurface extends AbstractMetParameter<Length> implements 
        ISerializableObject {

    /**
	 * 
	 */
    private static final long serialVersionUID = -6058298343743033424L;

    public CeilingFromSurface() {
        super(SI.METRE);
    }

    @Override
    public String getParameterDescription() {
        return "The Ceiling as measured from the station or surface (AKA CEIL).";
    }

    @DeriveMethod
    AbstractMetParameter derive(SkyCoverage sc, CloudLayerBase clb)
            throws InvalidValueException, NullPointerException {

        String dbValsString[] = sc.getDbValsString();
        Number dbValsNumber[] = clb.getDbValsNumber();

        setValueToMissing(); // default is no ceiling

        for (int i = 0; i < dbValsString.length; i++) {

            if (dbValsString[i].startsWith("BKN")
                    || dbValsString[i].startsWith("OVC")
                    || dbValsString[i].startsWith("VV")) {
                setValue(dbValsNumber[i].doubleValue() * .01);
                setUnit(NcUnits.HUNDREDS_OF_FEET);
                break;
            }

        }

        return this;
    }
}
