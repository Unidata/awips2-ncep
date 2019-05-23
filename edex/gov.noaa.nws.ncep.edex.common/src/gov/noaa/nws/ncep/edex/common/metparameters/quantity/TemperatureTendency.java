package gov.noaa.nws.ncep.edex.common.metparameters.quantity;

import javax.measure.Unit;

import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.NcUnits;

public interface TemperatureTendency extends javax.measure.Quantity <TemperatureTendency>{
    public static final Unit<TemperatureTendency> UNIT = NcUnits.KELVIN_PER_DAY;

}
