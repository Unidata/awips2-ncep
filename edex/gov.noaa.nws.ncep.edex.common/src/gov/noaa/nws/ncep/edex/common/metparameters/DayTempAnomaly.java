/**
 * 
 */
package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Temperature;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;
import si.uom.SI;
import systems.uom.common.USCustomary;

/**
 * Maps to the GEMPAK parameter TDAF
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class DayTempAnomaly extends AbstractMetParameter<Temperature>
        implements ISerializableObject {

    /**
    * 
    */
    private static final long serialVersionUID = 6489210440084721750L;

    public DayTempAnomaly() throws Exception {
        super(SI.KELVIN);
    }

    @DeriveMethod
    public DayTempAnomaly derive(MaxDayTemp maxTemp, ClimDayTemp climDayTemp)
            throws InvalidValueException, NullPointerException {

        if (maxTemp == null || climDayTemp == null || !maxTemp.hasValidValue()
                || !climDayTemp.hasValidValue()) {

            setUnit(USCustomary.FAHRENHEIT);
            return this;
        }

        double tmax = maxTemp.getValueAs("°F").doubleValue();
        double tclim = climDayTemp.getValueAs("°F").doubleValue();
        Double anomalyTemp = new Double(tmax - tclim);
        setValue(anomalyTemp, USCustomary.FAHRENHEIT);
        return this;
    }

    @DeriveMethod
    public DayTempAnomaly derive(Max24HrTemp maxTemp, ClimDayTemp climDayTemp)
            throws InvalidValueException, NullPointerException {

        if (maxTemp == null || climDayTemp == null || !maxTemp.hasValidValue()
                || !climDayTemp.hasValidValue()) {

            setUnit(USCustomary.FAHRENHEIT);
            return this;
        }

        double tmax = maxTemp.getValueAs("°F").doubleValue();
        double tclim = climDayTemp.getValueAs("°F").doubleValue();
        Double anomalyTemp = new Double(tmax - tclim);
        setValue(anomalyTemp, USCustomary.FAHRENHEIT);
        return this;
    }

}
