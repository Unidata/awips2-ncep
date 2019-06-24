package gov.noaa.nws.ncep.edex.common.metparameters.quantity;

import javax.measure.Quantity;
import javax.measure.Unit;

import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.NcUnits;

public interface AmountOfPrecipitation
        extends Quantity<AmountOfPrecipitation> {
    public static final Unit<AmountOfPrecipitation> UNIT = NcUnits.KG_PER_METER_SQ;
}
