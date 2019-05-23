package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Temperature;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.NcUnits;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;
import si.uom.SI;

/**
 * Maps to the GEMPAK parameter LTMP
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 07/20/2016   R15950      J.Huber     Added derive method to use temperature,
 *                                      dewpoint, and pressure to derive 
 *                                      surface equivalent potential 
 *                                      temperature and surface potential
 *                                      temperature so that lifted surface air
 *                                      temperature at 500mb can be calculated.
 * </pre>
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class LiftedSurfaceAirTempAt500mb extends AbstractMetParameter<Temperature> {

    public LiftedSurfaceAirTempAt500mb() {
        super(SI.KELVIN);
    }

    @DeriveMethod
    public LiftedSurfaceAirTempAt500mb derive(SurfacePotentialTemp pt,
            SurfaceEquivPotentialTemp eqt) throws InvalidValueException,
            NullPointerException {
        if (pt.hasValidValue() && eqt.hasValidValue()) {
            /*
             * The pressure is hard-coded as 500 mb, since legacy hard-codes the
             * pressure value as 500 mb as well
             */
            Amount val = PRLibrary.prLtmp(pt, eqt, new Amount(
                    new Double(500.0), NcUnits.MILLIBAR)); // the pressureLevel
            this.setValue(val);
        } else {
            setValueToMissing();
        }
        return this;
    }

    @DeriveMethod
    public LiftedSurfaceAirTempAt500mb derive(AirTemperature t,
            SurfacePressure p, DewPointTemp dpt) throws InvalidValueException,
            NullPointerException {
        if (t.hasValidValue() && dpt.hasValidValue() && p.hasValidValue()) {
            /*
             * Derive surface equivalent potential temperature and surface
             * potential temperature to be able to calculate lifted surface air
             * temperature at 500mb.
             */
            Amount sfcEquivPotTempAmount = PRLibrary.prThte(p, t, dpt);
            Amount sfcPotentialTempAmount = PRLibrary.prThta(t, p);
            /*
             * The pressure is hard-coded as 500 mb, since legacy hard-codes the
             * pressure value as 500 mb as well
             */
            Amount val = PRLibrary.prLtmp(sfcPotentialTempAmount,
                    sfcEquivPotTempAmount, new Amount(new Double(500.0),
                            NcUnits.MILLIBAR));
            this.setValue(val);
        } else {
            setValueToMissing();
        }
        return this;
    }
}
