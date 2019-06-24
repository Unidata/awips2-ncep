/**
 * 
 */
package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Temperature;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;
import si.uom.SI;
import systems.uom.common.USCustomary;

/**
 * Maps to the GEMPAK parameter TNAF
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class NightTempAnomaly
        extends AbstractMetParameter<Temperature>
        implements ISerializableObject {

    private static final long serialVersionUID = -8221554987555370951L;

    public NightTempAnomaly() throws Exception {
        super(SI.KELVIN);
    }

    @DeriveMethod
    public NightTempAnomaly derive(MinNightTemp minTemp,
            ClimNightTemp climNightTemp)
            throws InvalidValueException, NullPointerException {

        if (minTemp == null || climNightTemp == null || !minTemp.hasValidValue()
                || !climNightTemp.hasValidValue()) {

            setUnit(USCustomary.FAHRENHEIT);
            return this;
        }

        double tmax = minTemp.getValueAs(USCustomary.FAHRENHEIT).doubleValue();
        double tclim = climNightTemp.getValueAs(USCustomary.FAHRENHEIT).doubleValue();
        Double anomalyTemp = new Double(tmax - tclim);
        setValue(anomalyTemp, USCustomary.FAHRENHEIT);
        return this;
    }

    @DeriveMethod
    public NightTempAnomaly derive(Min24HrTemp minTemp,
            ClimNightTemp climNightTemp)
            throws InvalidValueException, NullPointerException {

        if (minTemp == null || climNightTemp == null || !minTemp.hasValidValue()
                || !climNightTemp.hasValidValue()) {

            setUnit(USCustomary.FAHRENHEIT);
            return this;
        }

        double tmax = minTemp.getValueAs("°F").doubleValue();
        double tclim = climNightTemp.getValueAs("°F").doubleValue();
        Double anomalyTemp = new Double(tmax - tclim);
        setValue(anomalyTemp, USCustomary.FAHRENHEIT);
        return this;
    }

}
