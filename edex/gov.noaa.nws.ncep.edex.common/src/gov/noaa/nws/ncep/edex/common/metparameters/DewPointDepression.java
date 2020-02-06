package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Temperature;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary;
import si.uom.SI;

/**
 * Maps to any of the GEMPAK parameters DPDC or DPDK or DPDF depending on the
 * unit used to measure the dewpoint depression.
 * 
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 07/20/2016    R15950      J.Huber     Reverted to previous version and added conversion 
 *                                       to proper units.
 * </pre>
 * 
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class DewPointDepression extends AbstractMetParameter<Temperature> {

    private static final long serialVersionUID = -8246047973579792393L;

    private final double THRESHOLD_FROM_NMAP_THAT_WORKS = 30.0d;

    public DewPointDepression() throws Exception {
        super(SI.KELVIN);
    }

    @DeriveMethod
    public DewPointDepression derive(AirTemperature t, DewPointTemp d)
            throws Exception {
        if (t.hasValidValue() && d.hasValidValue()) {
            // For some reason T and DpT do not come in with the correct units.
            // In order for the calculation to be correct, the units have to be
            // converted prior to going to the PR library to be calculated.

            // TODO: Determine why units are not proper when they get here.
            Amount tCorrected = PRLibrary
                    .checkAndConvertInputAmountToExpectedUnits(t, getUnit());
            Amount dCorrected = PRLibrary
                    .checkAndConvertInputAmountToExpectedUnits(d, getUnit());
            Amount dwdp = PRLibrary.prDdep(tCorrected, dCorrected);
            setValue(dwdp);
        } else {
            setValueToMissing();
        }
        return this;
    }

    // TODO : could change this to pass along the threshhold instead of
    // hardcoding 30.
    @Override
    public String getFormattedString(String formatStr) {

        if (formatStr == null || formatStr.isEmpty()
                || formatStr.startsWith("%")) {
            return super.getFormattedString(formatStr);
        } else if (formatStr.equalsIgnoreCase("DPDX")) {
            if (getValueAs(SI.CELSIUS).doubleValue() >= this.THRESHOLD_FROM_NMAP_THAT_WORKS) {
                return "X";
            } else {
                return super.getFormattedString("%3.0f");
            }
        } else {
            return getValue().toString();
        }
    }
}
