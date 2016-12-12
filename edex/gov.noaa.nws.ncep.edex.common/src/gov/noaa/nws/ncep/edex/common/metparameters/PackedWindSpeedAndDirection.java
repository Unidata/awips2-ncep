package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;

/**
 * Packed Wind Speed And Direction, used for PKNT, PSPD
 * 
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#    Engineer    Description
 * ------------  ---------- ----------- --------------------------
 * 12/02/2016    R6154     S.Russell    Initial creation
 *
 * </pre>
 *
 * @author S.Russell
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class PackedWindSpeedAndDirection extends AbstractMetParameter
        implements javax.measure.quantity.Dimensionless {

    public PackedWindSpeedAndDirection() {
        super(UNIT);
    }

    @DeriveMethod
    AbstractMetParameter derive(WindDirection d, WindSpeed s)
            throws InvalidValueException, NullPointerException {

        if (d.hasValidValue() && s.hasValidValue()) {
            Amount ws = PRLibrary.checkAndConvertInputAmountToExpectedUnits(s,
                    this.getUnit());
            Amount pknt = PRLibrary.prPWSAD(d, ws);
            setValue(pknt);
        } else {
            setValueToMissing();
        }

        return this;

    }

}// end class PackedWindSpeedAndDirection
