package gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion;

import javax.measure.Unit;
import javax.measure.quantity.Length;
import javax.measure.quantity.Pressure;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.units.CustomUnits;

import gov.noaa.nws.ncep.edex.common.metparameters.quantity.AmountOfPrecipitation;
import gov.noaa.nws.ncep.edex.common.metparameters.quantity.HeatFlux;
import gov.noaa.nws.ncep.edex.common.metparameters.quantity.PotentialForCyclonicUpdraftRotation;
import gov.noaa.nws.ncep.edex.common.metparameters.quantity.RateOfChangeInPressureWithTime;
import gov.noaa.nws.ncep.edex.common.metparameters.quantity.RateOfChangeInTemperatureWithHeight;
import gov.noaa.nws.ncep.edex.common.metparameters.quantity.RateOfChangeInTemperatureWithPressure;
import gov.noaa.nws.ncep.edex.common.metparameters.quantity.TemperatureTendency;
import si.uom.NonSI;
import si.uom.SI;
import si.uom.quantity.Density;
import systems.uom.common.USCustomary;
import tec.uom.se.format.SimpleUnitFormat;
import tec.uom.se.unit.MetricPrefix;
import tec.uom.se.unit.ProductUnit;
import tec.uom.se.unit.Units;

/**
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 *                        archana   Initial creation
 * Apr 15, 2019  7596     lsingh    Units Framework upgrade to JSR-363
 * Apr 01, 2020  8118     randerso  Removed label for degree symbol as that is
 *                                  not a valid ASCII character.
 *
 * </pre>
 *
 * @author archana
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class NcUnits implements ISerializableObject {
    /**
     * No-arguments constructor
     */
    public NcUnits() {

    }

    @DynamicSerializeElement
    public static final Unit<AmountOfPrecipitation> KG_PER_METER_SQ = new ProductUnit<>(
            SI.KILOGRAM.divide((SI.METRE.pow(2))));

    @DynamicSerializeElement
    public static final Unit<?> GRAMS_PER_KILOGRAM = SI.GRAM
    .divide(SI.KILOGRAM);

    @DynamicSerializeElement
    public static final Unit<Length> HUNDREDS_OF_FEET = USCustomary.FOOT
    .multiply(100);

    @DynamicSerializeElement
    public static final Unit<?> INCHES_PER_THREE_HOURS = USCustomary.INCH
    .divide(Units.HOUR.multiply(3));

    @DynamicSerializeElement
    public static final Unit<?> INCHES_PER_HOUR = USCustomary.INCH.divide(Units.HOUR);

    @DynamicSerializeElement
    public static final Unit<PotentialForCyclonicUpdraftRotation> METER_SQUARE_PER_SECOND_SQUARE = new ProductUnit<>(
            (SI.METRE.pow(2)).divide((SI.SECOND.pow(2))));

    @DynamicSerializeElement
    public static final Unit<?> JOULES_PER_KILOGRAM = SI.JOULE
    .divide(SI.KILOGRAM);

    @DynamicSerializeElement
    public static final Unit<Pressure> MILLIBAR = MetricPrefix.MILLI(NonSI.BAR);

    @DynamicSerializeElement
    public static final Unit<?> FREQUENCY_SQUARED = SI.HERTZ.multiply(SI.HERTZ);

    // TODO Rename to TEMPERATURE_PER_UNIT_HEIGHT ??
    @DynamicSerializeElement
    public static final Unit<RateOfChangeInTemperatureWithHeight> KELVIN_PER_KILOMETER = new ProductUnit<>(
            SI.KELVIN.divide(MetricPrefix.KILO(SI.METRE)));

    @DynamicSerializeElement
    public static final Unit<RateOfChangeInTemperatureWithPressure> KELVIN_PER_MILLIBAR = new ProductUnit<>(
            SI.KELVIN.divide(NcUnits.MILLIBAR));

    @DynamicSerializeElement
    public static final Unit<RateOfChangeInPressureWithTime> PASCALS_PER_SEC = new ProductUnit<>(
            SI.PASCAL.divide(SI.SECOND));

    @DynamicSerializeElement
    public static final Unit<TemperatureTendency> KELVIN_PER_DAY = new ProductUnit<>(
            SI.KELVIN.divide(Units.DAY));

    @DynamicSerializeElement
    public static final Unit<HeatFlux> WATT_PER_METRE_SQUARE = new ProductUnit<>(
            SI.WATT.divide(SI.METRE.pow(2)));

    public static final Unit<Density> KILOGRAM_PER_METRE_CUBE = new ProductUnit<>(
            MetricPrefix.KILO(SI.GRAM).divide(SI.METRE.pow(3)));

    public static void register() {
        final SimpleUnitFormat unitFormat = SimpleUnitFormat
                .getInstance(SimpleUnitFormat.Flavor.ASCII);

        unitFormat.label(NonSI.INCH_OF_MERCURY.divide(100), "hecto_inHg");
        unitFormat.label(HUNDREDS_OF_FEET, "hecto_ft");

        unitFormat.label(MILLIBAR, "mb");

        unitFormat.label(USCustomary.FAHRENHEIT, "Fahrenheit");
        unitFormat.label(NonSI.INCH_OF_MERCURY.divide(100), "hecto_inHg");
        unitFormat.label(HUNDREDS_OF_FEET, "hecto_ft");
        unitFormat.label(NonSI.DEGREE_ANGLE, "degree");
        unitFormat.label(PASCALS_PER_SEC, "pascals_per_sec");
        unitFormat.label(KELVIN_PER_DAY, "kelvin_per_day");
        unitFormat.label(JOULES_PER_KILOGRAM, "joules_per_kg");
        unitFormat.label(INCHES_PER_HOUR, "inches_per_hour");
        unitFormat.label(KG_PER_METER_SQ, "kg_per_meter_squared");
        unitFormat.label(CustomUnits.DECIBEL, "dB");

    }
}