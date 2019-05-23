package gov.noaa.nws.ncep.edex.common.metparameters.quantity;

import javax.measure.Quantity;
import javax.measure.Unit;

import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.NcUnits;

public interface PotentialForCyclonicUpdraftRotation extends Quantity<PotentialForCyclonicUpdraftRotation> {
    public static final Unit<PotentialForCyclonicUpdraftRotation> UNIT = NcUnits.METER_SQUARE_PER_SECOND_SQUARE;
}