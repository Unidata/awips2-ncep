package gov.noaa.nws.ncep.edex.common.metparameters.quantity;

import javax.measure.Unit;

import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.NcUnits;

 public interface RateOfChangeInTemperatureWithPressure extends javax.measure.Quantity<RateOfChangeInTemperatureWithPressure> {
           public final static Unit<RateOfChangeInTemperatureWithPressure> UNIT = NcUnits.KELVIN_PER_MILLIBAR;
}