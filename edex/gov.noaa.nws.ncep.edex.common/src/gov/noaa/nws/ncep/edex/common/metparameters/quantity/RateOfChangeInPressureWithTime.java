package gov.noaa.nws.ncep.edex.common.metparameters.quantity;

import javax.measure.Quantity;
import javax.measure.Unit;

import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.NcUnits;

public interface RateOfChangeInPressureWithTime
        extends Quantity<RateOfChangeInPressureWithTime> {
    public final static Unit<RateOfChangeInPressureWithTime> UNIT = NcUnits.PASCALS_PER_SEC;
}
