package gov.noaa.nws.ncep.edex.common.metparameters.quantity;

import javax.measure.Quantity;
import javax.measure.Unit;

import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.NcUnits;

public interface HeatFlux extends Quantity<HeatFlux> {
	public static final Unit<HeatFlux> UNIT = NcUnits.WATT_PER_METRE_SQUARE;
	}
