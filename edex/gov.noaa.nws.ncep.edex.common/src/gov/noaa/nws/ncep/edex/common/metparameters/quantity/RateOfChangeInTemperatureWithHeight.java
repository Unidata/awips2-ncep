package gov.noaa.nws.ncep.edex.common.metparameters.quantity;

import javax.measure.Quantity;
import javax.measure.Unit;

import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.NcUnits;

public interface RateOfChangeInTemperatureWithHeight extends Quantity<RateOfChangeInTemperatureWithHeight> {
     public final static Unit<RateOfChangeInTemperatureWithHeight> UNIT = NcUnits.KELVIN_PER_KILOMETER;
 }